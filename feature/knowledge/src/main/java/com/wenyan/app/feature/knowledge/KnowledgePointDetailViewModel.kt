package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.KnowledgePointDetail
import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
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
@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgePointDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val knowledgeRepository: KnowledgeRepository,
) : ViewModel() {

    /** 从导航参数获取知识点 ID */
    val pointId: String = savedStateHandle["pointId"] ?: ""

    /**
     * 重试触发器（v0.8.3 新增：支持 ErrorState 的 onRetry）。
     *
     * 自增整数，每次 [retry] 时 +1，触发 [uiState] 的 FlatMapLatest 重新订阅 Flow。
     */
    private val retryTrigger = MutableStateFlow(0)

    /**
     * 详情 UI 状态。
     *
     * 观察 Repository 的合并流，数据库变更时自动刷新。
     *
     * v0.8.3 重构：用 flatMapLatest 替代直接 stateIn + catch，支持 retry。
     * 原 catch 后流终止无法重试，现通过 retryTrigger 触发重新订阅。
     */
    val uiState: StateFlow<KnowledgePointDetailUiState> = retryTrigger
        .flatMapLatest {
            knowledgeRepository
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
                .catch { e ->
                    emit(KnowledgePointDetailUiState(error = e.message ?: "加载失败"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = KnowledgePointDetailUiState(isLoading = true),
        )

    /**
     * 重试加载（v0.8.3 新增：供 ErrorState 的 onRetry 调用）。
     *
     * 触发 [retryTrigger] 自增，重新订阅数据流。
     */
    fun retry() {
        retryTrigger.value++
    }
}

/**
 * 知识点详情 UI 状态。
 *
 * P1-3 新增 [error] 字段：数据流加载失败时携带错误信息，UI 据此提示用户。
 */
data class KnowledgePointDetailUiState(
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val detail: KnowledgePointDetail? = null,
    /** 加载失败时的错误信息（P1-3 新增） */
    val error: String? = null,
) {
    /** 知识点实体（便捷访问） */
    val point: KnowledgePointEntity? get() = detail?.point

    /** 来源列表 */
    val sources: List<DataSourceEntity> get() = detail?.sources.orEmpty()
}
