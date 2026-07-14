package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ReviewRepository
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 知识点模块 ViewModel。
 *
 * 注入 [ReviewRepository] 加载真实知识点数据。
 * 分类筛选通过 [KnowledgePointWithSubject.subjectName] 匹配 [KnowledgeCategory] 实现。
 *
 * 进程被杀恢复（NF-L1 修复）：[selectedCategory] 持久化到 [SavedStateHandle]，
 * 进程被杀后恢复分类筛选状态。
 */
@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    // NF-L1 修复：selectedCategory 持久化到 SavedStateHandle（存 enum name 为 String）
    private val _selectedCategoryName = savedStateHandle.getStateFlow("selectedCategory", KnowledgeCategory.ALL.name)

    /** 当前选中的分类（从 SavedStateHandle 恢复，默认 ALL） */
    val selectedCategory: StateFlow<KnowledgeCategory> = _selectedCategoryName
        .map { name -> KnowledgeCategory.entries.find { it.name == name } ?: KnowledgeCategory.ALL }
        .stateIn(viewModelScope, SharingStarted.Eagerly, KnowledgeCategory.ALL)

    /**
     * 知识点列表：合并 Repository 数据流与分类筛选流。
     *
     * 使用 [ReviewRepository.getVerifiedWithSubject] 获取知识点 + 科目名，
     * 按 [KnowledgeCategory] 筛选后映射为 UI 项。
     */
    val uiState: StateFlow<KnowledgeUiState> = combine(
        reviewRepository.getVerifiedWithSubject(),
        selectedCategory,
    ) { pointsWithSubject, category ->
        val filtered = filterByCategory(pointsWithSubject, category)
        KnowledgeUiState(
            isLoading = false,
            knowledgePoints = filtered.map { toUiItem(it) },
            selectedCategory = category,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KnowledgeUiState(isLoading = true),
    )

    // 切换分类标签（NF-L1 修复：持久化到 SavedStateHandle）
    fun selectCategory(category: KnowledgeCategory) {
        savedStateHandle["selectedCategory"] = category.name
    }

    companion object {
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
