package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 知识点模块 ViewModel。
 *
 * v0.8.19 架构修复(对应 AGENTS.md 第 9.4 条 P4):
 * - 原注入 [com.wenyan.app.core.data.repository.ReviewRepository] 仅为调用 getVerifiedWithSubject(),
 *   但 ReviewRepository 职责是 FSRS 复习队列,知识点浏览与复习无关,职责混乱。
 * - 现改注入 [KnowledgeRepository](知识点浏览总入口),getVerifiedWithSubject() 已迁移至此。
 *
 * v0.8.19 P1-UI-1 新增搜索框:
 * - [searchQuery] 持久化到 [SavedStateHandle],进程恢复后保留搜索状态
 * - [debounce](300ms) 避免每次按键触发 DB 查询(参考 Anki 搜索防抖)
 * - 空搜索词时走 [KnowledgeRepository.getVerifiedWithSubject](全部 VERIFIED)
 * - 非空搜索词时走 [KnowledgeRepository.searchVerifiedWithSubject](LIKE 搜索)
 * - 搜索结果仍受 [selectedCategory] 分类筛选约束(搜索 + 筛选可叠加)
 *
 * 分类筛选通过 [KnowledgePointWithSubject.subjectName] 匹配 [KnowledgeCategory] 实现。
 *
 * 进程被杀恢复（NF-L1 修复）：[selectedCategory] 和 [searchQuery] 均持久化到 [SavedStateHandle]。
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val knowledgeRepository: KnowledgeRepository,
) : ViewModel() {

    // NF-L1 修复：selectedCategory 持久化到 SavedStateHandle（存 enum name 为 String）
    private val _selectedCategoryName = savedStateHandle.getStateFlow("selectedCategory", KnowledgeCategory.ALL.name)

    /** 当前选中的分类（从 SavedStateHandle 恢复，默认 ALL） */
    val selectedCategory: StateFlow<KnowledgeCategory> = _selectedCategoryName
        .map { name -> KnowledgeCategory.entries.find { it.name == name } ?: KnowledgeCategory.ALL }
        .stateIn(viewModelScope, SharingStarted.Eagerly, KnowledgeCategory.ALL)

    /**
     * 搜索关键词(v0.8.19 新增,P1-UI-1)。
     *
     * 持久化到 [SavedStateHandle],进程被杀恢复后保留搜索状态。
     * 空字符串表示"无搜索",走 [KnowledgeRepository.getVerifiedWithSubject] 全部浏览。
     * 非空时走 [KnowledgeRepository.searchVerifiedWithSubject] LIKE 搜索。
     *
     * [debounce](300ms) 后才触发查询,避免快速输入时每次按键都查 DB。
     * 300ms 参考 Anki 搜索防抖默认值,平衡响应速度与 DB 负载。
     */
    private val _searchQuery = savedStateHandle.getStateFlow("searchQuery", "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * 重试触发器（P0-6 新增）。点击重试时自增，[flatMapLatest] 会重新订阅数据流。
     */
    private val _retryTrigger = MutableStateFlow(0)

    /**
     * 知识点列表 UI 状态（P1-4 改造为 MutableStateFlow 包装）。
     *
     * v0.8.19 重构:支持搜索 + 分类筛选叠加。
     * - 搜索词为空时:加载全部 VERIFIED 知识点 → 按分类筛选
     * - 搜索词非空时:LIKE 搜索 → 按分类筛选(在搜索结果中再筛选)
     *
     * P0-6 修复：加 [catch] 捕获数据流异常（如数据库损坏），避免异常冒泡导致 app 崩溃。
     * 捕获后 emit error 状态，UI 展示错误信息 + 重试按钮。
     *
     * P1-4 修复：原实现用 [stateIn] 包裹，retry() 只增加 [_retryTrigger]，
     * 但 StateFlow 当前值仍是上次的 error 状态，UI 无立即 loading 反馈。
     * 现改为 MutableStateFlow + [collect]，retry() 可立即设置 isLoading=true，
     * 保留 selectedCategory / searchQuery 等其他字段不清空。
     */
    private val _uiState = MutableStateFlow<KnowledgeUiState>(KnowledgeUiState(isLoading = true))
    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _retryTrigger
                .flatMapLatest {
                    // v0.8.19 P1-UI-1: 搜索词 debounce 300ms 后触发查询
                    _searchQuery
                        .debounce(SEARCH_DEBOUNCE_MS)
                        .flatMapLatest { query ->
                            val pointsFlow = if (query.isBlank()) {
                                knowledgeRepository.getVerifiedWithSubject()
                            } else {
                                // 转义 LIKE 通配符,避免 % 和 _ 被当通配符
                                val escaped = knowledgeRepository.escapeLikeWildcards(query.trim())
                                knowledgeRepository.searchVerifiedWithSubject(escaped)
                            }
                            combine(pointsFlow, selectedCategory) { points, category ->
                                val filtered = filterByCategory(points, category)
                                KnowledgeUiState(
                                    isLoading = false,
                                    knowledgePoints = filtered.map { toUiItem(it) },
                                    selectedCategory = category,
                                    searchQuery = query,
                                )
                            }
                        }
                }
                .catch { e ->
                    emit(KnowledgeUiState(error = e.message ?: "加载失败"))
                }
                .collect { _uiState.value = it }
        }
    }

    // 切换分类标签（NF-L1 修复：持久化到 SavedStateHandle）
    fun selectCategory(category: KnowledgeCategory) {
        savedStateHandle["selectedCategory"] = category.name
    }

    /**
     * 更新搜索关键词(v0.8.19 新增,P1-UI-1)。
     *
     * 持久化到 [SavedStateHandle],[debounce](300ms) 后触发查询。
     * 调用方无需手动 debounce,ViewModel 内部已处理。
     *
     * 传入空字符串或纯空白时清除搜索,恢复全部浏览模式。
     *
     * @param query 搜索关键词(原始输入,无需转义,ViewModel 内部转义 LIKE 通配符)
     */
    fun updateSearchQuery(query: String) {
        savedStateHandle["searchQuery"] = query
    }

    /** 清除搜索(便捷方法,等价于 updateSearchQuery("")) */
    fun clearSearch() {
        savedStateHandle["searchQuery"] = ""
    }

    /**
     * 重试加载（P0-6 新增，P1-4 增强）。
     *
     * P1-4 修复：先立即设置 isLoading=true 并清空 error，保留 selectedCategory / searchQuery 不变，
     * 让 UI 立即显示 loading 反馈；再增加 [_retryTrigger] 触发数据流重新订阅。
     */
    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        _retryTrigger.value++
    }

    companion object {
        /** 搜索防抖间隔(毫秒),参考 Anki 搜索默认值 */
        private const val SEARCH_DEBOUNCE_MS = 300L

        /**
         * 按科目筛选知识点。
         *
         * 用 [KnowledgeCategory.keyword] 在 [KnowledgePointWithSubject.subjectName] 中做 contains 匹配。
         * seed_data.json 的科目名是全名（"中国古代文学"），枚举 label 是简称（"古代文学"），
         * contains 匹配可兼容两者。
         *
         * 注意：ALL.keyword 为空字符串，任意字符串.contains("") 返回 true，
         * 但为明确语义，ALL 分支显式返回全部。
         *
         * P1-AUDIT-5 修正：subjectName 可能为 null（LEFT JOIN 无有效科目关联的知识点）。
         * null subjectName 不匹配任何具体分类（ANCIENT/MODERN/FOREIGN/THEORY），
         * 但在 ALL 分类下会显示（fallback "未知科目"）。
         */
        internal fun filterByCategory(
            points: List<KnowledgePointWithSubject>,
            category: KnowledgeCategory,
        ): List<KnowledgePointWithSubject> {
            if (category == KnowledgeCategory.ALL) return points
            return points.filter { it.subjectName?.contains(category.keyword) == true }
        }

        /** 将关联数据映射为 UI 项（供测试调用） */
        internal fun toUiItem(pointWithSubject: KnowledgePointWithSubject): KnowledgePointItem =
            KnowledgePointItem(
                id = pointWithSubject.point.id,
                title = pointWithSubject.point.title,
                subject = pointWithSubject.subjectName ?: "未知科目",
                summary = pointWithSubject.point.summary
                    ?: pointWithSubject.point.coreConclusion.take(100),
            )
    }
}

// 知识点 UI 状态
data class KnowledgeUiState(
    val isLoading: Boolean = false,
    val knowledgePoints: List<KnowledgePointItem> = emptyList(),
    val selectedCategory: KnowledgeCategory = KnowledgeCategory.ALL,
    /** 加载失败时的错误信息（P0-6 新增） */
    val error: String? = null,
    /** 当前搜索关键词(v0.8.19 新增,P1-UI-1,用于 UI 搜索框回显) */
    val searchQuery: String = "",
)

// 知识点列表项
data class KnowledgePointItem(
    val id: String,
    val title: String,
    val subject: String,
    val summary: String,
)

// 知识点分类（四科 + 全部）
// keyword 用于 subjectName.contains(keyword) 匹配，兼容 seed_data 全名与枚举简称
enum class KnowledgeCategory(val label: String, val keyword: String) {
    ALL("全部", ""),
    ANCIENT("古代文学", "古代"),
    MODERN("现当代文学", "现当代"),
    FOREIGN("外国文学", "外国"),
    THEORY("文学理论", "理论"),
}
