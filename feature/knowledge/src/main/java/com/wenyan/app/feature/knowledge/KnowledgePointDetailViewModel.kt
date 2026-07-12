package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.KnowledgePointDetail
import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 知识点详情 ViewModel（阶段5新增，Spec C1.27 多教材对照 + C7.2 来源溯源）。
 *
 * 通过 [SavedStateHandle] 从导航参数获取 pointId，
 * 观察 [KnowledgeRepository.observeKnowledgePointDetail] 获取详情。
 *
 * UI 状态含：
 * - 知识点主信息（title/summary/coreConclusion/studyText/multiPerspectives）
 * - 来源溯源列表（data_sources 表）
 * - 关联/对比/延伸知识点标题
 */
@HiltViewModel
class KnowledgePointDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val knowledgeRepository: KnowledgeRepository,
) : ViewModel() {

    /** 从导航参数获取知识点 ID */
    val pointId: String = savedStateHandle["pointId"] ?: ""

    /**
     * 详情 UI 状态。
     *
     * 观察 Repository 的合并流，数据库变更时自动刷新。
     */
    val uiState: StateFlow<KnowledgePointDetailUiState> = knowledgeRepository
        .observeKnowledgePointDetail(pointId)
        .map { detail ->
            if (detail == null) {
                KnowledgePointDetailUiState(isLoading = false, notFound = true)
            } else {
                KnowledgePointDetailUiState(
                    isLoading = false,
                    detail = detail,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = KnowledgePointDetailUiState(isLoading = true),
        )
}

/**
 * 知识点详情 UI 状态。
 */
data class KnowledgePointDetailUiState(
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val detail: KnowledgePointDetail? = null,
) {
    /** 知识点实体（便捷访问） */
    val point: KnowledgePointEntity? get() = detail?.point

    /** 来源列表 */
    val sources: List<DataSourceEntity> get() = detail?.sources.orEmpty()
}
