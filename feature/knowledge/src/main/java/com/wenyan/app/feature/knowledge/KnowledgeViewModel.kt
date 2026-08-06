package com.wenyan.app.feature.knowledge

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ChapterRepository
import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.KnowledgePointListItem
import com.wenyan.app.core.database.entity.SubjectEntity
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val chapterRepository: ChapterRepository,
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
    val searchQuery: StateFlow<String> = _searchQuery

    /**
     * 重试触发器（P0-6 新增）。点击重试时自增，[flatMapLatest] 会重新订阅数据流。
     */
    private val _retryTrigger = MutableStateFlow(0)

    /** 框架与列表模式共享同一份 lean 查询，避免进入知识点页后重复扫描知识点表。 */
    private val verifiedListItems = knowledgeRepository.getVerifiedListItems()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

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

    /** 框架浏览状态。列表模式仍由 [uiState] 提供，两个模式共享同一份轻量知识点流。 */
    private val _frameworkUiState = MutableStateFlow<FrameworkUiState>(FrameworkUiState(isLoading = true))
    val frameworkUiState: StateFlow<FrameworkUiState> = _frameworkUiState.asStateFlow()

    init {
        viewModelScope.launch {
            // v0.8.17 修复 B1:catch 必须在 flatMapLatest 内部,仅终止本次 inner Flow,
            // 外层 Flow 仍由 _retryTrigger 驱动,支持 retry() 重新触发加载。
            // 原实现 catch 在 flatMapLatest 外层,异常触发后整流终止,retry() 永久失效
            // (用户必须杀进程重启 App 才能恢复)。同 feature/cards v0.8.20 P1-2 修复模式。
            _retryTrigger
                .flatMapLatest {
                    // v0.8.19 P1-UI-1: 搜索词 debounce 300ms 后触发查询
                    // v0.8.13 P0-1 修复: retry 时跳过 debounce 立即重试。
                    // 原 _searchQuery.debounce(300ms) 在 retry 后仍要等 300ms 才重新查询,
                    // 违反 retry 的"立即重试"语义。用 onStart 在 debounce 之后立即 emit 当前值,
                    // 跳过首次 debounce 等待;distinctUntilChanged 过滤掉 debounce 后相同值的
                    // 重复 emit,避免首次加载/retry 触发两次相同 DB 查询。
                    _searchQuery
                        .debounce(SEARCH_DEBOUNCE_MS)
                        .onStart { emit(_searchQuery.value) }
                        .distinctUntilChanged()
                        .flatMapLatest { query ->
                            // v0.9.37 P1-2：列表展示走 lean 投影（只查展示列，
                            // 不加载 full_content/study_text 大文本列）
                            val pointsFlow = if (query.isBlank()) {
                                verifiedListItems
                            } else {
                                // 转义 LIKE 通配符,避免 % 和 _ 被当通配符
                                val escaped = knowledgeRepository.escapeLikeWildcards(query.trim())
                                knowledgeRepository.searchVerifiedListItems(escaped)
                            }
                            combine(pointsFlow, selectedCategory) { points, category ->
                                val filtered = filterByCategory(points, category)
                                KnowledgeUiState(
                                    isLoading = false,
                                    knowledgePoints = filtered.map { toUiItem(it) },
                                    selectedCategory = category,
                                )
                            }
                        }
                        // v0.8.17 修复 B1 + M4:catch 移入 flatMapLatest 内部,
                        // 仅终止本次 inner Flow,外层仍由 _retryTrigger 驱动。
                        // 同时加 Log.e 输出完整堆栈,生产排查不丢上下文
                        // (对照 feature/cards v0.8.14 P1-7 修复)。
                        // v0.8.20 P1-4:catch 时保留已有 knowledgePoints,
                        // 避免数据库偶发异常导致列表瞬间清空,用户丢失正在浏览的上下文。
                        .catch { e ->
                            Timber.e(e, "loadKnowledgePoints failed")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = friendlyErrorMessage(e),
                            )
                        }
                }
                .collect { _uiState.value = it }
        }

        viewModelScope.launch {
            _retryTrigger
                .flatMapLatest { observeFramework() }
                .catch { e ->
                    Timber.e(e, "loadKnowledgeFramework failed")
                    _frameworkUiState.value = _frameworkUiState.value.copy(
                        isLoading = false,
                        error = friendlyErrorMessage(e),
                    )
                }
                .collect { _frameworkUiState.value = it }
        }
    }

    private fun observeFramework(): kotlinx.coroutines.flow.Flow<FrameworkUiState> =
        chapterRepository.observeSubjects()
            .flatMapLatest { subjects ->
                observeChaptersForSubjects(subjects)
                    .combine(verifiedListItems) { chapters, points ->
                        buildFrameworkState(subjects, chapters, points)
                    }
            }

    private fun observeChaptersForSubjects(subjects: List<SubjectEntity>): kotlinx.coroutines.flow.Flow<List<ChapterEntity>> =
        if (subjects.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(subjects.map { chapterRepository.observeChapters(it.id) }) { chapterLists ->
                chapterLists.flatMap { it }
            }
        }

    private fun buildFrameworkState(
        subjects: List<SubjectEntity>,
        chapters: List<ChapterEntity>,
        points: List<KnowledgePointListItem>,
    ): FrameworkUiState {
        val pointsByChapter = points
            .filter { it.chapterId.isNotBlank() }
            .groupBy { it.chapterId }
            .mapValues { (_, items) -> items.map(::toUiItem) }
        val chaptersBySubject = chapters.groupBy { it.subjectId }

        val subjectItems = subjects.map { subject ->
            val subjectChapters = chaptersBySubject[subject.id].orEmpty()
            val childrenByParent = subjectChapters.groupBy { it.parentId }
            val directPoints = subjectChapters.associate { chapter ->
                chapter.id to pointsByChapter[chapter.id].orEmpty()
            }
            val aggregateCache = mutableMapOf<String, ChapterAggregate>()

            fun aggregate(chapterId: String, visiting: Set<String> = emptySet()): ChapterAggregate {
                aggregateCache[chapterId]?.let { return it }
                if (chapterId in visiting) return ChapterAggregate()
                val direct = directPoints[chapterId].orEmpty()
                val children = childrenByParent[chapterId].orEmpty()
                    .map { aggregate(it.id, visiting + chapterId) }
                return ChapterAggregate(
                    totalPointCount = direct.size + children.sumOf { it.totalPointCount },
                    highFrequencyCount = direct.count { it.examFrequency == "HIGH" } +
                        children.sumOf { it.highFrequencyCount },
                ).also { aggregateCache[chapterId] = it }
            }

            val chapterItems = subjectChapters.map { chapter ->
                val direct = directPoints[chapter.id].orEmpty()
                val aggregate = aggregate(chapter.id)
                FrameworkChapterItem(
                    id = chapter.id,
                    title = chapter.title,
                    parentId = chapter.parentId,
                    sortOrder = chapter.sortOrder,
                    childCount = childrenByParent[chapter.id].orEmpty().size,
                    directPointCount = direct.size,
                    totalPointCount = aggregate.totalPointCount,
                    highFrequencyCount = aggregate.highFrequencyCount,
                )
            }
            FrameworkSubjectItem(
                id = subject.id,
                name = subject.name,
                rootChapterId = subjectChapters.firstOrNull { it.parentId == null }?.id,
                chapters = chapterItems,
                pointsByChapter = pointsByChapter.filterKeys { key -> subjectChapters.any { it.id == key } },
            )
        }
        return FrameworkUiState(isLoading = false, subjects = subjectItems)
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
     * v0.8.20 P1-7 修复:限制最大长度 50 字符,避免用户粘贴超长字符串
     * 触发 O(n) escapeLikeWildcards + SQLite LIKE 全表扫描性能问题。
     * 50 字符足够覆盖考研关键词(如"鲁迅《呐喊》狂人日记象征手法")。
     *
     * v0.8.17 修复 M3:原实现只判断 length 截断,不 trim 不清空白,
     * UI 显示原始值(含尾空格)但搜索用 trim 后值,UI 与搜索语义不一致。
     * 现统一 trim 后存储:空白 → 空字符串(走全部浏览分支),
     * 非空白 → trim 后字符串(UI 与搜索一致)。
     *
     * @param query 搜索关键词(原始输入,无需转义,ViewModel 内部 trim + 转义 LIKE 通配符)
     */
    fun updateSearchQuery(query: String) {
        // v0.8.17 修复 M3:统一 trim,纯空白视为空搜索
        val trimmed = query.trim()
        // v0.8.20 P1-7: 限制最大 50 字符,超出截断
        val bounded = if (trimmed.length > MAX_SEARCH_QUERY_LENGTH) trimmed.take(MAX_SEARCH_QUERY_LENGTH) else trimmed
        savedStateHandle["searchQuery"] = bounded
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
        _frameworkUiState.value = _frameworkUiState.value.copy(isLoading = true, error = null)
        _retryTrigger.value++
    }

    companion object {
        /** 搜索防抖间隔(毫秒),参考 Anki 搜索默认值 */
        private const val SEARCH_DEBOUNCE_MS = 300L

        /**
         * 搜索关键词最大长度(v0.8.20 P1-7 新增)。
         *
         * 限制 50 字符避免:
         * - O(n) escapeLikeWildcards 转义超长字符串
         * - SQLite LIKE 全表扫描(910 知识点 × 10000 字符 ≈ 920 万次字符比较)
         * - SavedStateHandle 持久化超长字符串到 Bundle
         * - EmptyState 渲染超长文本
         *
         * 50 字符足够覆盖考研关键词(如"鲁迅《呐喊》狂人日记象征手法")。
         */
        private const val MAX_SEARCH_QUERY_LENGTH = 50


        /**
         * 按科目筛选知识点。
         *
         * 用 [KnowledgeCategory.keyword] 在 [KnowledgePointListItem.subjectName] 中做 contains 匹配。
         * seed_data.json 的科目名是全名（"中国古代文学"），枚举 label 是简称（"古代文学"），
         * contains 匹配可兼容两者。
         *
         * 注意：ALL.keyword 为空字符串，任意字符串.contains("") 返回 true，
         * 但为明确语义，ALL 分支显式返回全部。
         *
         * P1-AUDIT-5 修正：subjectName 可能为 null（LEFT JOIN 无有效科目关联的知识点）。
         * null subjectName 不匹配任何具体分类（ANCIENT/MODERN/FOREIGN/THEORY），
         * 但在 ALL 分类下会显示（fallback "未知科目"）。
         *
         * v0.9.37 P1-2：参数从 [KnowledgePointWithSubject] 改为 lean 投影
         * [KnowledgePointListItem]（列表流不再加载大文本列）。
         */
        internal fun filterByCategory(
            points: List<KnowledgePointListItem>,
            category: KnowledgeCategory,
        ): List<KnowledgePointListItem> {
            if (category == KnowledgeCategory.ALL) return points
            return points.filter { it.subjectName?.contains(category.keyword) == true }
        }

        /** 将 lean 投影数据映射为 UI 项（v0.9.37 P1-2，供测试调用） */
        internal fun toUiItem(item: KnowledgePointListItem): KnowledgePointItem =
            KnowledgePointItem(
                id = item.id,
                title = item.title,
                subject = item.subjectName ?: "未知科目",
                summary = item.summary ?: item.coreConclusion.take(100),
                // v0.8.20 P1-2 新增:透传考频,列表卡片展示高频/中频/低频标签,
                // 用户浏览时快速识别高频考点(无需点进详情页查看)
                examFrequency = item.examFrequency,
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
    // v0.8.20 P0-2 修复:删除死字段 searchQuery。
    // 原字段从未被 UI 读取(UI 用 viewModel.searchQuery StateFlow 实时值),
    // 且 debounce 300ms 后才更新,与 viewModel.searchQuery 双源不同步,易引入 bug。
)

data class FrameworkUiState(
    val isLoading: Boolean = false,
    val subjects: List<FrameworkSubjectItem> = emptyList(),
    val error: String? = null,
)

data class FrameworkSubjectItem(
    val id: String,
    val name: String,
    val rootChapterId: String?,
    val chapters: List<FrameworkChapterItem>,
    val pointsByChapter: Map<String, List<KnowledgePointItem>>,
)

data class FrameworkChapterItem(
    val id: String,
    val title: String,
    val parentId: String?,
    val sortOrder: Int,
    val childCount: Int,
    val directPointCount: Int,
    val totalPointCount: Int,
    val highFrequencyCount: Int,
)

private data class ChapterAggregate(
    val totalPointCount: Int = 0,
    val highFrequencyCount: Int = 0,
)

/**
 * 知识点列表项。
 *
 * v0.8.20 P1-2 新增 [examFrequency]:透传原始考频值(HIGH/MEDIUM/LOW/NEVER),
 * UI 层根据值显示对应 chip(高频/中频/低频/未考),
 * 与详情页 HeaderSection 的考频映射逻辑一致(避免在 ViewModel 层做 string 翻译)。
 *
 * v0.9.37 P2：补 @Immutable（全字段不可变，Compose 列表项稳定性统一）。
 */
@Immutable
data class KnowledgePointItem(
    val id: String,
    val title: String,
    val subject: String,
    val summary: String,
    /** 考频原始值(v0.8.20 P1-2 新增,UI 层映射为中文标签) */
    val examFrequency: String = "NEVER",
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

/**
 * 将异常映射为用户友好的中文错误提示。
 *
 * v0.8.20 P1-5 新增,v0.8.20 P1-2 重构:
 * 实现已抽取到 core/common 模块 [com.wenyan.app.core.common.util.friendlyErrorMessage]
 * 作为公共 API,供 feature/knowledge、feature/cards 等模块共享。
 *
 * 本 internal 包装仅为保持旧 API 兼容(feat/knowledge 测试文件仍引用
 * `friendlyErrorMessage`),实际委托到 core/common 实现。
 *
 * 历史背景详见 [com.wenyan.app.core.common.util.friendlyErrorMessage] 的 KDoc。
 */
internal fun friendlyErrorMessage(e: Throwable): String =
    com.wenyan.app.core.common.util.friendlyErrorMessage(e)
