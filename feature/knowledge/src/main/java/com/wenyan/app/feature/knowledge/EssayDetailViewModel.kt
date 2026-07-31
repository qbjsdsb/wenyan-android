package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 论述题详情 ViewModel（v0.9.8 新增，对应 docs/design/essay-module-design.md）。
 *
 * 职责：
 * - 从 [SavedStateHandle] 获取 examQuestionId
 * - 观察 [KnowledgeRepository.observeEssayById] 获取论述题实体
 * - 解析 [ExamQuestionEntity.angle] JSON → [EssayAngle]（审题思路）
 * - 解析 [ExamQuestionEntity.notes] JSON → [EssayNotes]（依据与交叉验证）
 * - 批量查询关联知识点（relatedPointIds + evidences.linkedKnowledgePointId）
 *
 * UI 状态含：
 * - 论述题主信息（年份/科目/分值/正文/答题框架）
 * - 审题思路（题型/关键词/任务/突破角度/论证路径）
 * - 依据与交叉验证（evidences/crossValidation/referenceLinks/knowledgeGaps）
 * - 关联知识点列表（点击跳转知识点详情，实现双向串联）
 *
 * 优雅降级：
 * - angle/notes 为 null（131/134 道论述题未填充）→ 隐藏对应区块
 * - JSON 解析失败 → 隐藏对应区块 + Timber.w 日志
 * - 关联知识点查询失败 → 空列表
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class EssayDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val knowledgeRepository: KnowledgeRepository,
) : ViewModel() {

    private val examQuestionIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("examQuestionId", "")

    /** 当前论述题 ID（便捷访问） */
    val examQuestionId: String get() = examQuestionIdFlow.value

    private val retryTrigger = MutableStateFlow(0)

    private val _uiState = MutableStateFlow<EssayDetailUiState>(EssayDetailUiState(isLoading = true))
    val uiState: StateFlow<EssayDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(retryTrigger, examQuestionIdFlow) { _, id -> id }
                .flatMapLatest { examQuestionId ->
                    if (examQuestionId.isBlank()) {
                        flowOf(EssayDetailUiState(isLoading = false, notFound = true))
                    } else {
                        knowledgeRepository.observeEssayById(examQuestionId)
                            .mapLatest { essay ->
                                if (essay == null) {
                                    EssayDetailUiState(isLoading = false, notFound = true)
                                } else {
                                    // 解析 angle/notes JSON（失败返回 null，UI 优雅降级）
                                    val angle = parseEssayAngle(essay.angle)
                                    val notes = parseEssayNotes(essay.notes)

                                    // 收集关联知识点 ID：relatedPointIds + evidences.linkedKnowledgePointId
                                    val pointIds = buildSet {
                                        essay.relatedPointIds?.let { addAll(it) }
                                        notes?.evidences?.forEach { ev ->
                                            ev.linkedKnowledgePointId?.let { add(it) }
                                        }
                                    }.toList()

                                    val relatedPoints = if (pointIds.isEmpty()) {
                                        emptyList()
                                    } else {
                                        knowledgeRepository.getKnowledgePointsByIds(pointIds)
                                    }

                                    EssayDetailUiState(
                                        isLoading = false,
                                        essay = essay,
                                        angle = angle,
                                        notes = notes,
                                        relatedPoints = relatedPoints,
                                    )
                                }
                            }
                            .catch { e ->
                                Timber.e(e, "loadEssayDetail failed: examQuestionId=$examQuestionId")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = friendlyErrorMessage(e),
                                )
                            }
                    }
                }
                .collect { _uiState.value = it }
        }
    }

    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        retryTrigger.value++
    }
}

/**
 * 论述题详情 UI 状态（v0.9.8 新增）。
 *
 * @param isLoading 加载中
 * @param notFound 论述题不存在（ID 错误或已删除）
 * @param error 加载失败错误信息
 * @param essay 论述题实体（年份/科目/分值/正文/答题框架）
 * @param angle 审题思路（解析自 essay.angle JSON，null 时 UI 隐藏审题区块）
 * @param notes 依据与交叉验证（解析自 essay.notes JSON，null 时 UI 隐藏依据区块）
 * @param relatedPoints 关联知识点列表（点击跳转知识点详情）
 */
data class EssayDetailUiState(
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val error: String? = null,
    val essay: ExamQuestionEntity? = null,
    val angle: EssayAngle? = null,
    val notes: EssayNotes? = null,
    val relatedPoints: List<KnowledgePointEntity> = emptyList(),
)
