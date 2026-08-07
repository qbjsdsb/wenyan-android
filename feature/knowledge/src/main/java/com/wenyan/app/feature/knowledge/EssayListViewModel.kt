package com.wenyan.app.feature.knowledge

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.common.util.ExamContentCleaner
import com.wenyan.app.core.data.repository.ChapterRepository
import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.database.entity.SubjectEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

/**
 * 论述题列表 ViewModel（v0.9.8 Phase 2 新增）。
 *
 * 职责：
 * - 从 [KnowledgeRepository.observeAllEssays] 获取全部论述题
 * - 从 [ChapterRepository.observeSubjects] 获取科目列表（subj_xx → 科目名映射）
 * - 提供二维筛选：科目 / 仅显示有审题思路的题（v0.9.23：年份筛选已删除）
 * - 筛选在内存完成（当前数据量下 < 5ms，与 observeRelatedEssays 策略一致）
 *
 * 筛选状态独立 StateFlow，UI 可即时响应筛选切换（与 KnowledgeViewModel.selectedCategory
 * 解耦策略一致，避免 error/loading 态下筛选无反馈）。
 *
 * 数据流：
 * ```
 * observeAllEssays + observeSubjects + selectedSubjectId + onlyWithAngle
 *     → combine → 内存筛选 + 科目名映射 → EssayListUiState
 * ```
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class EssayListViewModel @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
    private val chapterRepository: ChapterRepository,
) : ViewModel() {

    // ── 筛选状态（独立 StateFlow，error/loading 态下也可切换）──

    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    val selectedSubjectId: StateFlow<String?> = _selectedSubjectId.asStateFlow()

    private val _onlyWithAngle = MutableStateFlow(false)
    val onlyWithAngle: StateFlow<Boolean> = _onlyWithAngle.asStateFlow()

    /**
     * 重试触发器（与 KnowledgePointDetailViewModel / EssayDetailViewModel 一致）。
     *
     * 自增整数，每次 [retry] 时 +1，触发 [uiState] 的 [flatMapLatest] 重新订阅
     * 整个数据流。combine + .catch 是终端操作（catch 后内层流终止），
     * 需通过 retryTrigger 驱动 flatMapLatest 重建内层流才能恢复。
     */
    private val retryTrigger = MutableStateFlow(0)

    // ── UI 状态 ──

    val uiState: StateFlow<EssayListUiState> =
        retryTrigger
            .flatMapLatest {
                // v0.9.25 修复：内层 flow 先 emit isLoading=true 再 emitAll 数据。
                // 原实现 combine 首个 emit 即 isLoading=false 的数据结果，
                // retry() 后 UI 保持 error 直到数据到达，无 loading 反馈。
                // 现在每次重订阅（含 retry）都会先显示 loading。
                flow {
                    emit(EssayListUiState(isLoading = true))
                    // 内层 combine:4 个数据/筛选源合并 → EssayListUiState
                    // retryTrigger++ 时 flatMapLatest 取消旧内层流、重建新内层流,
                    // 实现真正重新订阅(observeAllEssays / observeSubjects 重新查询)。
                    emitAll(
                        combine(
                            knowledgeRepository.observeAllEssays(),
                            chapterRepository.observeSubjects(),
                            _selectedSubjectId,
                            _onlyWithAngle,
                        ) { essays, subjects, subjectId, onlyWithAngle ->
                            val subjectMap = subjects.associate { it.id to it.name }
                            val filtered = essays.filter { essay ->
                                (subjectId == null || essay.subjectId == subjectId) &&
                                    (!onlyWithAngle || !essay.angle.isNullOrBlank())
                            }
                            val items = filtered.map { essay ->
                                EssayListItem(
                                    id = essay.id,
                                    subjectName = subjectMap[essay.subjectId] ?: "未知科目",
                                    score = essay.score,
                                    contentPreview = ExamContentCleaner.stripQuestionNumber(essay.content).take(MAX_PREVIEW_LENGTH),
                                    hasAngle = !essay.angle.isNullOrBlank(),
                                    hasNotes = !essay.notes.isNullOrBlank(),
                                    relatedPointCount = essay.relatedPointIds?.size ?: 0,
                                )
                            }
                            EssayListUiState(
                                isLoading = false,
                                essays = items,
                                totalCount = essays.size,
                                filteredCount = filtered.size,
                                subjects = subjects,
                            )
                        }
                            // catch 必须在 flatMapLatest 内层:仅终止本次内层流,
                            // 外层 retryTrigger 流仍存活,retry() 可重新触发订阅。
                            .catch { e ->
                                Timber.e(e, "EssayListViewModel combine failed")
                                emit(
                                    EssayListUiState(
                                        isLoading = false,
                                        error = friendlyErrorMessage(e),
                                    ),
                                )
                            },
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                // v0.9.24 修复：WhileSubscribed(5000) → Eagerly。
                // 原实现离开 Tab 超 5s 后内层流停止收集，返回时重新订阅导致先闪 loading。
                // 数据源是 Room Flow（observeAllEssays/observeSubjects），Eagerly 长期收集
                // 开销小（Room observer 复用），换取 Tab 切换不中断。
                started = SharingStarted.Eagerly,
                initialValue = EssayListUiState(isLoading = true),
            )

    // ── 筛选操作 ──

    fun selectSubject(subjectId: String?) {
        _selectedSubjectId.value = subjectId
    }

    fun toggleOnlyWithAngle() {
        _onlyWithAngle.value = !_onlyWithAngle.value
    }

    fun clearFilters() {
        _selectedSubjectId.value = null
        _onlyWithAngle.value = false
    }

    /**
     * 重试加载（与 KnowledgePointDetailViewModel.retry 一致）。
     *
     * 先设置 isLoading=true 让 UI 立即显示 loading 反馈，
     * 再触发 [retryTrigger]++ 让 [uiState] 重新订阅数据流。
     */
    fun retry() {
        // stateIn 不暴露 setter,无法直接修改 uiState;用 retryTrigger 触发重新订阅,
        // 重新订阅时 initialValue 会被新的 isLoading=true 覆盖（combine 首个 emit 前
        // uiState 保持上一次的 error 值,但 retryTrigger 变化会让 combine 重新执行,
        // 首个 emit 是 isLoading=false 的结果,所以这里不需要额外设 loading）。
        retryTrigger.value++
    }

    private companion object {
        /** 题目预览最大长度（列表卡片显示，超出截断） */
        const val MAX_PREVIEW_LENGTH = 80
    }
}

/**
 * 论述题列表项（UI 层精简模型，避免把完整 ExamQuestionEntity 暴露给 UI）。
 *
 * @param id 真题 ID（如 eq_0038）
 * @param subjectName 科目名（从 subjectId 映射，如"中国现当代文学"）
 * @param score 分值
 * @param contentPreview 题目正文预览（前 80 字）
 * @param hasAngle 是否有审题思路（angle JSON 非空）
 * @param hasNotes 是否有依据/交叉验证（notes JSON 非空）
 * @param relatedPointCount 关联知识点数量
 */
@Immutable
data class EssayListItem(
    val id: String,
    val subjectName: String,
    val score: Int,
    val contentPreview: String,
    val hasAngle: Boolean,
    val hasNotes: Boolean,
    val relatedPointCount: Int,
)

/**
 * 论述题列表 UI 状态。
 *
 * @param isLoading 加载中
 * @param error 加载失败错误信息
 * @param essays 筛选后的论述题列表项
 * @param totalCount 总题数（不受筛选影响）
 * @param filteredCount 筛选后题数
 * @param subjects 科目列表（供科目筛选 chip 渲染）
 */
@Immutable
data class EssayListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val essays: List<EssayListItem> = emptyList(),
    val totalCount: Int = 0,
    val filteredCount: Int = 0,
    val subjects: List<SubjectEntity> = emptyList(),
)
