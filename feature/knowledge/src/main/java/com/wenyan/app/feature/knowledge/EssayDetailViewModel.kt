package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.ai.SocraticGuide
import com.wenyan.app.core.ai.SocraticTutor
import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.data.repository.SchedulingRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.fsrs.Rating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
 * 论述题详情 ViewModel（v0.9.8 新增，v0.9.9 Phase 3 接入 AI 审题助手）。
 *
 * 职责：
 * - 从 [SavedStateHandle] 获取 examQuestionId
 * - 观察 [KnowledgeRepository.observeEssayById] 获取论述题实体
 * - 解析 [ExamQuestionEntity.angle] JSON → [EssayAngle]（审题思路）
 * - 解析 [ExamQuestionEntity.notes] JSON → [EssayNotes]（依据与交叉验证）
 * - 批量查询关联知识点（relatedPointIds + evidences.linkedKnowledgePointId）
 *
 * v0.9.9 Phase 3 新增职责（AI 审题助手集成）：
 * - 用户答题区状态管理（[userAnswer] / [isAnswering]）
 * - 调用 [SocraticTutor.guideEssayAnswer] 流式输出三阶段引导（ANALYZE / SUGGEST / SHOW_SAMPLE）
 * - 用户自评（[EssaySelfRating]）→ AGAIN 时回写错题本 + FSRS 调度
 *
 * UI 状态含：
 * - 论述题主信息（年份/科目/分值/正文/答题框架）
 * - 审题思路（题型/关键词/任务/突破角度/论证路径）
 * - 依据与交叉验证（evidences/crossValidation/referenceLinks/knowledgeGaps）
 * - 关联知识点列表（点击跳转知识点详情，实现双向串联）
 * - 用户答题文本 + AI 三阶段引导结果 + 自评状态（Phase 3）
 *
 * 优雅降级：
 * - angle/notes 为 null（131/134 道论述题未填充）→ 隐藏对应区块
 * - JSON 解析失败 → 隐藏对应区块 + Timber.w 日志
 * - 关联知识点查询失败 → 空列表
 * - AI 引导失败 → aiGuideError 展示 + 重试按钮，已 emit 的阶段仍展示
 * - AI 服务不可用 → submitAnswerAndGuide 仍调用，由 SocraticTutor 内部降级处理
 *   （RAG 无结果时 emit 友好提示）
 *
 * 错题回写策略（v0.9.9 Phase 3）：
 * - 用户自评 AGAIN → recordWrongAnswer(examQuestionId, userAnswer, source=SOURCE_ESSAY_PRACTICE)
 *   + rateWrongAnswer(newWrongAnswerId, Rating.AGAIN) 触发 FSRS 调度
 * - 自评 GOOD/EASY → 不写错题本，仅 UI 展示完成
 * - 复用 v0.9.4 错题本 FSRS 调度基础设施（SchedulingRepository.rateWrongAnswer）
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class EssayDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val knowledgeRepository: KnowledgeRepository,
    private val socraticTutor: SocraticTutor,
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val schedulingRepository: SchedulingRepository,
) : ViewModel() {

    private val examQuestionIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("examQuestionId", "")

    /** 当前论述题 ID（便捷访问） */
    val examQuestionId: String get() = examQuestionIdFlow.value

    private val retryTrigger = MutableStateFlow(0)

    private val _uiState = MutableStateFlow<EssayDetailUiState>(EssayDetailUiState(isLoading = true))
    val uiState: StateFlow<EssayDetailUiState> = _uiState.asStateFlow()

    /**
     * 当前 AI 引导协程句柄（v0.9.9 Phase 3）。
     *
     * 用于 [cancelAiGuide] 取消正在进行的 AI 引导，
     * 以及防止 [submitAnswerAndGuide] 重复触发（同时只能有一个引导任务）。
     */
    private var aiGuideJob: Job? = null

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

    // ── Phase 3: AI 审题助手 ──────────────────────────────────

    /**
     * 更新用户答题文本（v0.9.9 Phase 3）。
     *
     * UI 答题区 TextField onValueChange 调用，超过 [MAX_USER_ANSWER_LENGTH] 字符截断。
     */
    fun updateUserAnswer(text: String) {
        val truncated = if (text.length > MAX_USER_ANSWER_LENGTH) {
            text.take(MAX_USER_ANSWER_LENGTH)
        } else {
            text
        }
        _uiState.value = _uiState.value.copy(userAnswer = truncated)
    }

    /**
     * 展开/进入答题模式（v0.9.9 Phase 3）。
     *
     * UI 展示 TextField + 提交按钮，隐藏"开始练习"入口。
     */
    fun startAnswering() {
        _uiState.value = _uiState.value.copy(isAnswering = true)
    }

    /**
     * 收起/退出答题模式（v0.9.9 Phase 3）。
     *
     * 清空答题文本 + AI 引导结果 + 自评状态，回到初始查看态。
     * 若有进行中的 AI 引导协程，先取消。
     */
    fun cancelAnswering() {
        cancelAiGuideJob()
        _uiState.value = _uiState.value.copy(
            isAnswering = false,
            userAnswer = "",
            aiGuides = emptyList(),
            isAiGuiding = false,
            aiGuideError = null,
            selfRating = null,
        )
    }

    /**
     * 提交答案并触发 AI 苏格拉底三阶段引导（v0.9.9 Phase 3）。
     *
     * 流程：
     * 1. 防重入：[aiGuideJob] 非空且 active → 直接返回
     * 2. 空答案保护：[userAnswer] blank → 不触发，由 UI 禁用提交按钮
     * 3. 清空旧引导结果 + 设置 isAiGuiding=true
     * 4. 调用 [socraticTutor.guideEssayAnswer] 流式 collect，每阶段 emit 追加到 [aiGuides]
     * 5. 异常 → aiGuideError（已 emit 阶段保留）
     * 6. finally → isAiGuiding=false，[aiGuideJob] = null
     *
     * SocraticTutor 内部已处理：
     * - 答案过短（< 50 字）→ emit 引导提示并 return
     * - RAG 无结果 → emit 友好提示并 return
     * - 三阶段失败短路（P1-6 修复）：阶段N 失败不执行阶段N+1
     */
    fun submitAnswerAndGuide() {
        val essay = _uiState.value.essay ?: return
        val userAnswer = _uiState.value.userAnswer
        if (userAnswer.isBlank()) {
            Timber.w("submitAnswerAndGuide called with blank answer, ignored")
            return
        }
        if (aiGuideJob?.isActive == true) {
            Timber.w("submitAnswerAndGuide called while AI guide in progress, ignored")
            return
        }

        _uiState.value = _uiState.value.copy(
            aiGuides = emptyList(),
            isAiGuiding = true,
            aiGuideError = null,
            selfRating = null,
        )

        aiGuideJob = viewModelScope.launch {
            try {
                socraticTutor.guideEssayAnswer(essay.content, userAnswer).collect { guide ->
                    _uiState.value = _uiState.value.copy(
                        aiGuides = _uiState.value.aiGuides + guide,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "submitAnswerAndGuide failed: examQuestionId=${essay.id}")
                _uiState.value = _uiState.value.copy(
                    aiGuideError = friendlyErrorMessage(e),
                )
            } finally {
                _uiState.value = _uiState.value.copy(isAiGuiding = false)
                aiGuideJob = null
            }
        }
    }

    /**
     * 重试 AI 引导（v0.9.9 Phase 3）。
     *
     * AI 引导失败后，UI 用相同答案重试。
     * 等价于 [submitAnswerAndGuide]，但语义独立便于日志追踪。
     */
    fun retryAiGuide() {
        submitAnswerAndGuide()
    }

    /**
     * 清空 AI 引导结果（v0.9.9 Phase 3）。
     *
     * 保留 [userAnswer]，仅清空 [aiGuides] / [aiGuideError] / [selfRating]，
     * 让用户可基于原答案重新提交或修改。
     */
    fun clearAiGuides() {
        cancelAiGuideJob()
        _uiState.value = _uiState.value.copy(
            aiGuides = emptyList(),
            aiGuideError = null,
            selfRating = null,
            isAiGuiding = false,
        )
    }

    /**
     * 用户自评（v0.9.9 Phase 3）。
     *
     * 三档评分语义对齐 FSRS：
     * - [EssaySelfRating.AGAIN]：答得不好，记录到错题本 + FSRS 调度（Rating.AGAIN）
     * - [EssaySelfRating.GOOD]：答得尚可，不写错题本
     * - [EssaySelfRating.EASY]：答得轻松，不写错题本
     *
     * 错题回写流程（AGAIN 时）：
     * 1. recordWrongAnswer(examQuestionId, userAnswer, source=SOURCE_ESSAY_PRACTICE)
     *    → 返回 wrongAnswerId（新插入或已有记录）
     * 2. schedulingRepository.rateWrongAnswer(wrongAnswerId, Rating.AGAIN)
     *    → 触发 FSRS 调度，更新 sched_* 字段
     * 3. selfRating = AGAIN（UI 展示自评结果）
     *
     * 失败处理：
     * - recordWrongAnswer / rateWrongAnswer 异常 → Timber.e + selfRating 仍设置
     *   （自评是用户意图，不应因持久化失败丢失；下次进入页面错题本可能未记录，
     *   但用户可重新自评或手动加入错题本）
     */
    fun rateSelf(rating: EssaySelfRating) {
        val essay = _uiState.value.essay ?: return
        val userAnswer = _uiState.value.userAnswer

        viewModelScope.launch {
            var recordedWrongAnswerId: String? = null
            if (rating == EssaySelfRating.AGAIN) {
                try {
                    val wrongAnswerId = wrongAnswerRepository.recordWrongAnswer(
                        pointId = null,
                        examQuestionId = essay.id,
                        userAnswer = userAnswer,
                        correctAnswer = null,
                        source = WrongAnswerRepository.SOURCE_ESSAY_PRACTICE,
                    )
                    schedulingRepository.rateWrongAnswer(wrongAnswerId, Rating.AGAIN)
                    recordedWrongAnswerId = wrongAnswerId
                    Timber.i("Essay self-rated AGAIN, recorded to wrong_answers: id=$wrongAnswerId, examQuestionId=${essay.id}")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "rateSelf AGAIN failed to record wrong answer: examQuestionId=${essay.id}")
                }
            }
            _uiState.value = _uiState.value.copy(
                selfRating = rating,
                lastWrongAnswerId = recordedWrongAnswerId,
            )
        }
    }

    /**
     * 取消正在进行的 AI 引导协程（v0.9.9 Phase 3）。
     *
     * 内部辅助方法，[cancelAnswering] / [clearAiGuides] 调用。
     * 不抛 CancellationException 到调用方（cancel 是协作式，Job.cancel 后 collect 会抛出，
     * 但 try-catch 已在 [submitAnswerAndGuide] 处理）。
     */
    private fun cancelAiGuideJob() {
        aiGuideJob?.let { job ->
            if (job.isActive) {
                job.cancel()
                Timber.d("AI guide job cancelled")
            }
        }
        aiGuideJob = null
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel 销毁时取消 AI 引导协程，避免泄漏
        cancelAiGuideJob()
    }

    companion object {
        /**
         * 用户答题文本最大长度（v0.9.9 Phase 3）。
         *
         * 5000 字，对齐设计文档 4.4 节约束。
         * 防止用户粘贴整本教材导致 LLM token 超限。
         */
        const val MAX_USER_ANSWER_LENGTH = 5000
    }
}

/**
 * 论述题详情 UI 状态（v0.9.8 新增，v0.9.9 Phase 3 扩展 AI 引导字段）。
 *
 * @param isLoading 加载中
 * @param notFound 论述题不存在（ID 错误或已删除）
 * @param error 加载失败错误信息
 * @param essay 论述题实体（年份/科目/分值/正文/答题框架）
 * @param angle 审题思路（解析自 essay.angle JSON，null 时 UI 隐藏审题区块）
 * @param notes 依据与交叉验证（解析自 essay.notes JSON，null 时 UI 隐藏依据区块）
 * @param relatedPoints 关联知识点列表（点击跳转知识点详情）
 *
 * Phase 3 新增字段：
 * @param userAnswer 用户答题文本（TextField 双向绑定）
 * @param isAnswering 是否展开答题区（true 时显示 TextField + 提交按钮）
 * @param aiGuides AI 苏格拉底三阶段引导结果（流式追加，按 stage 顺序）
 * @param isAiGuiding AI 引导进行中（UI 显示 Loading 指示器）
 * @param aiGuideError AI 引导失败错误信息（null 表示无错误）
 * @param selfRating 用户自评结果（null 表示未自评）
 * @param lastWrongAnswerId 最后一次错题回写的 ID（用于测试断言，UI 不展示）
 */
data class EssayDetailUiState(
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val error: String? = null,
    val essay: ExamQuestionEntity? = null,
    val angle: EssayAngle? = null,
    val notes: EssayNotes? = null,
    val relatedPoints: List<KnowledgePointEntity> = emptyList(),
    // Phase 3 新增字段
    val userAnswer: String = "",
    val isAnswering: Boolean = false,
    val aiGuides: List<SocraticGuide> = emptyList(),
    val isAiGuiding: Boolean = false,
    val aiGuideError: String? = null,
    val selfRating: EssaySelfRating? = null,
    val lastWrongAnswerId: String? = null,
)

/**
 * 论述题自评档位（v0.9.9 Phase 3 新增）。
 *
 * 三档评分语义对齐 FSRS [Rating]，但仅取三档（无 HARD）：
 * - [AGAIN]：答得不好 → 写入错题本 + FSRS 调度（Rating.AGAIN）
 * - [GOOD]：答得尚可 → 不写错题本
 * - [EASY]：答得轻松 → 不写错题本
 *
 * 不设 HARD 档的原因：论述题自评主观性强，4 档区分度过细；
 * 3 档"不好/尚可/轻松"已足够驱动错题回写决策。
 */
enum class EssaySelfRating {
    AGAIN,
    GOOD,
    EASY,
}
