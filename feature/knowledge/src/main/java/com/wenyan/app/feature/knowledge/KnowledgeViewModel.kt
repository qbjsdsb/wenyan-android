package com.wenyan.app.feature.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ReviewRepository
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 知识点模块 ViewModel。
 *
 * 注入 [ReviewRepository] 加载真实知识点数据。
 * 分类筛选在内存中执行（通过 [KnowledgePointEntity.contentSource] 间接分类）。
 */
@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(KnowledgeCategory.ALL)
    val selectedCategory: StateFlow<KnowledgeCategory> = _selectedCategory.asStateFlow()

    /**
     * 知识点列表：合并 Repository 数据流与分类筛选流。
     *
     * 使用 [ReviewRepository.getAllVerifiedKnowledgePoints] 获取全部已验证知识点
     * （含未到期，供浏览）。复习队列请使用 [ReviewRepository.getReviewQueue]。
     *
     * 使用 stateIn 订阅，ViewModelScope 销毁时自动取消。
     */
    val uiState: StateFlow<KnowledgeUiState> = combine(
        reviewRepository.getAllVerifiedKnowledgePoints(),
        _selectedCategory,
    ) { points, category ->
        val filtered = filterByCategory(points, category)
        KnowledgeUiState(
            isLoading = false,
            knowledgePoints = filtered.map { it.toUiItem() },
            selectedCategory = category,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KnowledgeUiState(isLoading = true),
    )

    // 切换分类标签
    fun selectCategory(category: KnowledgeCategory) {
        _selectedCategory.update { category }
    }

    // 按科目筛选（当前通过 contentSource 粗略分类，后续接入 chapter→subject 关联后精确筛选）
    private fun filterByCategory(
        points: List<KnowledgePointEntity>,
        category: KnowledgeCategory,
    ): List<KnowledgePointEntity> {
        if (category == KnowledgeCategory.ALL) return points
        // 暂时返回全部，后续通过 chapter_id → subject 关联实现精确筛选
        return points
    }

    private fun KnowledgePointEntity.toUiItem() = KnowledgePointItem(
        id = id,
        title = title,
        subject = contentSource ?: "未知",
        summary = summary ?: coreConclusion.take(100),
    )
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
enum class KnowledgeCategory(val label: String) {
    ALL("全部"),
    ANCIENT("古代文学"),
    MODERN("现当代文学"),
    FOREIGN("外国文学"),
    THEORY("文学理论"),
}
