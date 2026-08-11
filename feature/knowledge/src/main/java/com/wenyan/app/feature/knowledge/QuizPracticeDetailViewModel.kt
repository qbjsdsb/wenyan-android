package com.wenyan.app.feature.knowledge

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.common.util.friendlyErrorMessage
import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.data.repository.PracticeAttemptStore
import com.wenyan.app.core.data.practice.PracticeAttemptStage
import com.wenyan.app.core.data.practice.PracticeAttemptWorkflow
import com.wenyan.app.core.data.practice.PracticeAttemptWorkflowState
import com.wenyan.app.core.data.practice.PracticeDraft
import com.wenyan.app.core.data.practice.PracticeTransition
import com.wenyan.app.core.data.practice.PracticeSessionSummary
import com.wenyan.app.core.data.practice.summarizePracticeSession
import com.wenyan.app.core.database.entity.PracticeAttemptEntity
import com.wenyan.app.core.database.entity.PracticeAttemptType
import com.wenyan.app.core.database.entity.PracticeErrorReason
import com.wenyan.app.core.database.entity.PracticeRepairState
import com.wenyan.app.core.database.entity.PracticeSelfRating
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

/**
 * 真题背题详情 ViewModel（v0.9.33 新增）。
 *
 * 纯背诵模式：看题 → 显示答案 → 对照记忆 → 标记"会/不会"。
 *
 * 前后题导航在同一筛选集内进行：列表页点击时通过 nav 参数携带
 * type/subject/year 筛选条件，本 ViewModel 按相同条件重建列表并定位起始题。
 *
 * 错题本接入：标记"不会"→ [WrongAnswerRepository.recordWrongAnswer]
 * （examQuestionId + correctAnswer=answerFramework + SOURCE_QUIZ_WRONG），
 * 错题本显示"真题练习"，自动进入 FSRS 复习队列。同一题重复标记"不会"
 * 由 recordWrongAnswer 内部去重（未解决记录 increment wrongCount，不新增）。
 *
 * @param savedStateHandle nav 参数：questionId（必填）+ type/subject/year（筛选，默认 ALL）
 */
@HiltViewModel
class QuizPracticeDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val knowledgeRepository: KnowledgeRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val practiceAttemptStore: PracticeAttemptStore,
) : ViewModel() {

    /**
     * 起始题目 ID（nav 路径参数）。
     * v0.9.35 审计修复：原 checkNotNull 对异常导航/未来 deep link 会在构造期崩溃；
     * 改可空，loadQuestions 内友好降级为错误态（对齐 KnowledgePointDetail 容错模式）。
     */
    private val startQuestionId: String? = savedStateHandle.get<String>(KEY_QUESTION_ID)

    /** 筛选条件（nav query 参数，"ALL" 表示不筛选） */
    private val selectedType: String? = savedStateHandle.get<String>(KEY_TYPE)?.takeIf { it != FILTER_ALL }
    private val selectedSubjectId: String? = savedStateHandle.get<String>(KEY_SUBJECT)?.takeIf { it != FILTER_ALL }
    private val selectedYear: Int? = savedStateHandle.get<String>(KEY_YEAR)?.takeIf { it != FILTER_ALL }?.toIntOrNull()
    private val selectedPaperCode: String? = savedStateHandle.get<String>(KEY_PAPER)?.takeIf { it != FILTER_ALL }
    private val sessionId: String = savedStateHandle.get<String>(KEY_SESSION_ID)
        ?: "practice-session-${java.util.UUID.randomUUID()}".also { savedStateHandle[KEY_SESSION_ID] = it }

    // ── 基础状态 ──

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 当前筛选集内的题目列表（顺序与列表页一致） */
    private val _questions = MutableStateFlow<List<ExamQuestionEntity>>(emptyList())
    val questions: StateFlow<List<ExamQuestionEntity>> = _questions.asStateFlow()

    /** 当前题目下标 */
    private val _currentIndex = savedStateHandle.getStateFlow(KEY_CURRENT_INDEX, 0)
    val currentIndex: StateFlow<Int> = _currentIndex

    /** 是否已显示答案 */
    private val _showAnswer = savedStateHandle.getStateFlow(KEY_SHOW_ANSWER, false)
    val showAnswer: StateFlow<Boolean> = _showAnswer

    /** 用户提示（进错题本等），UI 用 Snackbar 展示 */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val keywords = savedStateHandle.getStateFlow(KEY_KEYWORDS, "")
    val outline = savedStateHandle.getStateFlow(KEY_OUTLINE, "")
    val body = savedStateHandle.getStateFlow(KEY_BODY, "")
    val attemptStage = savedStateHandle.getStateFlow(KEY_ATTEMPT_STAGE, PracticeAttemptStage.ANSWERING.name)
    val selectedRating = savedStateHandle.getStateFlow<String?>(KEY_RATING, null)
    val selectedErrors = savedStateHandle.getStateFlow<ArrayList<String>>(KEY_ERRORS, arrayListOf())
    val sessionSummary: StateFlow<PracticeSessionSummary> = practiceAttemptStore.observeSession(sessionId)
        .map(::summarizePracticeSession)
        .stateIn(viewModelScope, SharingStarted.Eagerly, summarizePracticeSession(emptyList()))

    // ── 派生状态 ──

    /** 当前题实体 */
    val currentQuestion: StateFlow<ExamQuestionEntity?> =
        combine(_questions, _currentIndex) { questions, index ->
            questions.getOrNull(index)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    /** 进度（index + total），供"第 X / N 题"展示 */
    val progress: StateFlow<Progress> =
        combine(_questions, _currentIndex) { questions, index ->
            Progress(index = index, total = questions.size)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Progress(index = 0, total = 0),
            )

    init {
        loadQuestions()
    }

    /** 加载题目列表（与列表页相同筛选条件），定位起始题 */
    private var loadJob: Job? = null
    private var restoreJob: Job? = null
    private val persistMutex = Mutex()
    private var persistGeneration = 0L
    private val persistedGenerations = mutableMapOf<String, Long>()

    private fun loadQuestions() {
        loadJob?.cancel()
        val startId = startQuestionId
        if (startId == null) {
            _error.value = "缺少题目参数，请返回列表重试"
            _isLoading.value = false
            return
        }
        loadJob = viewModelScope.launch {
            knowledgeRepository.observePracticeQuestions(QuizPracticeTypes.ALL)
                .catch { e ->
                    // 取消必须 rethrow：旧 job 被 retry 取消时，不能让取消异常落入错误态
                    if (e is CancellationException) throw e
                    Timber.e(e, "QuizPracticeDetailViewModel load failed")
                    _error.value = friendlyErrorMessage(e)
                    _isLoading.value = false
                }
                .collect { all ->
                    val filtered = all.filter { q ->
                        (selectedType == null || q.questionType == selectedType) &&
                            (selectedSubjectId == null || q.subjectId == selectedSubjectId) &&
                            (selectedYear == null || q.year == selectedYear)
                            && (selectedPaperCode == null || q.examPaperCode == selectedPaperCode)
                    }
                    val startIndex = filtered.indexOfFirst { it.id == startId }
                    if (startIndex < 0) {
                        // 起始题不在筛选集（防御：列表页用相同条件导航，正常不会发生）
                        Timber.w(
                            "QuizPracticeDetail startQuestionId=%s not in filter set (type=%s subject=%s year=%s)",
                            startId, selectedType, selectedSubjectId, selectedYear,
                        )
                        _error.value = "题目不在当前筛选中，请返回列表重试"
                        _isLoading.value = false
                    } else {
                        val savedIndex = savedStateHandle.get<Int>(KEY_CURRENT_INDEX)
                        val targetIndex = (savedIndex ?: startIndex).coerceIn(filtered.indices)
                        _questions.value = filtered
                        if (savedIndex != targetIndex) setCurrentIndex(targetIndex)
                        _isLoading.value = false
                        restoreAttemptForQuestion(filtered[targetIndex].id)
                    }
                }
        }
    }

    // ── 操作 ──

    fun toggleShowAnswer() {
        if (_showAnswer.value) hideAnswer() else revealAnswer()
    }

    fun showAnswer() {
        revealAnswer()
    }

    fun hideAnswer() {
        savedStateHandle[KEY_SHOW_ANSWER] = false
    }

    fun previous() {
        if (_currentIndex.value > 0) {
            persistDraftBeforeNavigation()
            setCurrentIndex(_currentIndex.value - 1)
            resetAttemptForQuestion()
            restoreAttemptForQuestion(_questions.value[_currentIndex.value].id)
        }
    }

    fun next() {
        if (_currentIndex.value < _questions.value.size - 1) {
            persistDraftBeforeNavigation()
            setCurrentIndex(_currentIndex.value + 1)
            resetAttemptForQuestion()
            restoreAttemptForQuestion(_questions.value[_currentIndex.value].id)
        }
    }

    /** 标记"会了"→ 推进到下一题（已是最后一题则保持原位，由 UI 提示已完成） */
    fun markKnow() {
        if (!canAdvanceAfterReveal()) return
        // v0.9.35 审计修复：400ms 防连击窗，避免快速双击"会了"连跳两题
        if (!tryAcquireAdvanceLock()) return
        assess(PracticeSelfRating.GOOD, emptySet())
        completeAttempt()
        if (_currentIndex.value < _questions.value.size - 1) {
            setCurrentIndex(_currentIndex.value + 1)
            resetAttemptForQuestion()
            restoreAttemptForQuestion(_questions.value[_currentIndex.value].id)
        } else {
            hideAnswer()
            _message.value = "已经是最后一题"
        }
    }

    /**
     * 标记"不会"→ 写入错题本 + 推进到下一题。
     *
     * v0.9.35 审计修复（P1）：原实现先捕获题目再在协程内 await recordWrongAnswer，
     * 完成后才推进索引——快速连点会启动多个协程：同一题重复写入错题本
     * （wrongCount 虚增）且各协程先后 +1 静默跳题。现改为：
     * - 同步推进索引（用已捕获的 question），重复点击作用于下一题而非同题
     * - 400ms 防连击窗，误触双击只推进一次
     */
    fun markDontKnow() {
        val question = currentQuestion.value ?: return
        if (!canAdvanceAfterReveal()) return
        if (!tryAcquireAdvanceLock()) return
        val errors = selectedErrors.value
            .mapNotNull(PracticeErrorReason::fromDb)
            .toSet()
            .ifEmpty { setOf(PracticeErrorReason.MEMORY_GAP) }
        val userAnswer = currentDraftText()
        assess(PracticeSelfRating.AGAIN, errors)
        completeAttempt()
        val isLast = _currentIndex.value >= _questions.value.size - 1
        if (!isLast) {
            setCurrentIndex(_currentIndex.value + 1)
            resetAttemptForQuestion()
            restoreAttemptForQuestion(_questions.value[_currentIndex.value].id)
        }
        hideAnswer()
        viewModelScope.launch {
            try {
                wrongAnswerRepository.recordWrongAnswer(
                    pointId = null,
                    examQuestionId = question.id,
                    userAnswer = userAnswer,
                    correctAnswer = question.answerFramework,
                    source = WrongAnswerRepository.SOURCE_QUIZ_WRONG,
                )
                _message.value = if (isLast) {
                    "已加入错题本（已是最后一题）"
                } else {
                    "已加入错题本，将按 FSRS 安排复习"
                }
            } catch (e: Exception) {
                Timber.e(e, "markDontKnow recordWrongAnswer failed")
                // 推进已同步完成不回退；仅提示失败（错题本写入失败不影响背诵流程）
                _message.value = "加入错题本失败，请稍后重试"
            }
        }
    }

    /** 防连击锁：400ms 内只允许一次推进操作（"会了"/"不会"共用）。 */
    private var lastAdvanceAt: Long? = null

    /**
     * 时间源（可注入便于单测——纯 JVM 测试中 SystemClock.uptimeMillis 恒 0，
     * 防连击锁会永远锁住首次调用）。生产默认系统时钟。
     */
    internal var uptimeMillis: () -> Long = { android.os.SystemClock.uptimeMillis() }

    private fun tryAcquireAdvanceLock(): Boolean {
        val now = uptimeMillis()
        val last = lastAdvanceAt
        if (last != null && now - last < ADVANCE_LOCK_MS) return false
        lastAdvanceAt = now
        return true
    }

    /** 重试加载（错误态下 ErrorState 重试按钮回调） */
    fun retry() {
        _isLoading.value = true
        _error.value = null
        loadQuestions()
    }

    fun clearMessage() {
        _message.value = null
    }

    fun updateKeywords(value: String) { savedStateHandle[KEY_KEYWORDS] = value }
    fun updateOutline(value: String) { savedStateHandle[KEY_OUTLINE] = value }
    fun updateBody(value: String) { savedStateHandle[KEY_BODY] = value }

    /** 记录用户在核对后选择的一个或多个错因，随 SavedStateHandle 跨旋转保留。 */
    fun toggleErrorReason(reason: PracticeErrorReason) {
        val current = selectedErrors.value.mapNotNull(PracticeErrorReason::fromDb).toMutableSet()
        if (!current.add(reason)) current.remove(reason)
        savedStateHandle[KEY_ERRORS] = ArrayList(current.map { it.name }.sorted())
    }

    fun saveDraft() = applyTransition(PracticeAttemptWorkflow.save(workflowState()), persist = true)

    fun revealAnswer() {
        val question = currentQuestion.value ?: return
        val transition = PracticeAttemptWorkflow.reveal(
            workflowState(), question.contentStatus, !question.answerFramework.isNullOrBlank(),
        )
        applyTransition(transition, persist = true) {
            savedStateHandle[KEY_SHOW_ANSWER] = true
        }
    }

    fun assess(rating: PracticeSelfRating, errors: Set<PracticeErrorReason>) {
        applyTransition(PracticeAttemptWorkflow.assess(workflowState(), rating, errors), persist = true)
    }

    fun completeAttempt() = applyTransition(PracticeAttemptWorkflow.complete(workflowState()), persist = true)

    private fun workflowState() = PracticeAttemptWorkflowState(
        stage = PracticeAttemptStage.entries.firstOrNull { it.name == attemptStage.value } ?: PracticeAttemptStage.ANSWERING,
        draft = PracticeDraft(keywords.value, outline.value, body.value),
        rating = PracticeSelfRating.fromDb(selectedRating.value),
        errors = selectedErrors.value.mapNotNull(PracticeErrorReason::fromDb).toSet(),
        repairState = if (selectedErrors.value.isEmpty()) PracticeRepairState.NONE else PracticeRepairState.CANDIDATE,
    )

    /** 切题前静默保存非空草稿，避免用户尚未揭示答案时直接切题而丢失输入。 */
    private fun persistDraftBeforeNavigation() {
        if (workflowState().draft.hasAnswer) {
            applyTransition(PracticeAttemptWorkflow.save(workflowState()), persist = true)
        }
    }

    private fun canAdvanceAfterReveal(): Boolean {
        if (attemptStage.value == PracticeAttemptStage.COMPLETED.name) {
            _message.value = "本题本轮已完成，请切换题目后再练习"
            return false
        }
        if (!_showAnswer.value) {
            _message.value = "请先主动揭示并核对"
            return false
        }
        return true
    }

    private fun currentDraftText(): String = listOf(keywords.value, outline.value, body.value)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString("\n\n")

    private fun applyTransition(
        transition: PracticeTransition,
        persist: Boolean,
        accepted: () -> Unit = {},
    ) {
        when (transition) {
            is PracticeTransition.Rejected -> _message.value = transition.reason
            is PracticeTransition.Accepted -> {
                val state = transition.state
                savedStateHandle[KEY_ATTEMPT_STAGE] = state.stage.name
                savedStateHandle[KEY_RATING] = state.rating?.name
                savedStateHandle[KEY_ERRORS] = ArrayList(state.errors.map { it.name })
                accepted()
                if (persist) persist(state)
            }
        }
    }

    private fun persist(state: PracticeAttemptWorkflowState) {
        val question = currentQuestion.value ?: return
        val now = currentTimeMillis()
        val attemptId = savedStateHandle.get<String>(KEY_ATTEMPT_ID)
            ?: "attempt-${java.util.UUID.randomUUID()}".also { savedStateHandle[KEY_ATTEMPT_ID] = it }
        val requestedGeneration = ++persistGeneration
        viewModelScope.launch {
            runCatching {
                // assess() and completeAttempt() are separate transitions, but their persistence
                // jobs can overlap. Serialize the read-modify-write sequence, discard stale
                // snapshots for the same attempt, and never downgrade a persisted stage.
                persistMutex.withLock {
                    if (requestedGeneration < (persistedGenerations[attemptId] ?: Long.MIN_VALUE)) return@withLock
                    val existing = practiceAttemptStore.get(attemptId)
                    val existingStage = existing?.let(::persistedStage) ?: PracticeAttemptStage.ANSWERING
                    val effectiveStage = maxOf(state.stage, existingStage)
                    val effectiveDraft = if (existingStage > state.stage && existing != null) {
                        PracticeDraft(existing.userKeywords, existing.outline, existing.body)
                    } else {
                        state.draft
                    }
                    val effectiveRating = if (state.stage >= PracticeAttemptStage.ASSESSED) {
                        state.rating?.name ?: existing?.selfRating
                    } else {
                        existing?.selfRating
                    }
                    val effectiveErrors = if (state.stage >= PracticeAttemptStage.ASSESSED) {
                        state.errors
                    } else {
                        existing?.errorReasons.orEmpty().mapNotNull(PracticeErrorReason::fromDb).toSet()
                    }
                    val effectiveRepairState = if (state.stage >= PracticeAttemptStage.ASSESSED) {
                        state.repairState
                    } else {
                        PracticeRepairState.fromDb(existing?.repairState.orEmpty())
                    }
                    practiceAttemptStore.save(
                        PracticeAttemptEntity(
                            id = attemptId, questionId = question.id,
                            pointId = question.relatedPointIds?.singleOrNull(), learningUnitId = null,
                            sessionId = sessionId, attemptType = PracticeAttemptType.EXAM_OUTLINE.name,
                            userKeywords = effectiveDraft.keywords, outline = effectiveDraft.outline, body = effectiveDraft.body,
                            startedAt = existing?.startedAt ?: now,
                            revealedAt = if (effectiveStage >= PracticeAttemptStage.REVEALED) existing?.revealedAt ?: now else null,
                            completedAt = if (effectiveStage == PracticeAttemptStage.COMPLETED) existing?.completedAt ?: now else null,
                            elapsedMs = existing?.elapsedMs ?: 0L,
                            selfRating = effectiveRating,
                            errorReasons = effectiveErrors.map { it.name }.sorted(), repairState = effectiveRepairState.name,
                            createdAt = existing?.createdAt ?: now, updatedAt = now,
                        ),
                    )
                    persistedGenerations[attemptId] = requestedGeneration
                }
            }.onFailure { Timber.e(it, "persist practice attempt failed") }
        }
    }

    private fun setCurrentIndex(value: Int) { savedStateHandle[KEY_CURRENT_INDEX] = value }

    private fun resetAttemptForQuestion() {
        restoreJob?.cancel()
        savedStateHandle.remove<String>(KEY_ATTEMPT_ID)
        savedStateHandle.remove<String>(KEY_RATING)
        savedStateHandle[KEY_KEYWORDS] = ""; savedStateHandle[KEY_OUTLINE] = ""; savedStateHandle[KEY_BODY] = ""
        savedStateHandle[KEY_ERRORS] = arrayListOf<String>()
        savedStateHandle[KEY_ATTEMPT_STAGE] = PracticeAttemptStage.ANSWERING.name
        savedStateHandle[KEY_SHOW_ANSWER] = false
    }

    /** Restore the latest draft for this session/question after manual or process navigation. */
    private fun restoreAttemptForQuestion(questionId: String) {
        restoreJob?.cancel()
        restoreJob = viewModelScope.launch {
            val attempt = runCatching {
                // Serialize this read with pending writes in the same VM so an immediate return
                // cannot observe the previous version of the attempt.
                persistMutex.withLock {
                    practiceAttemptStore.getLatestForSessionAndQuestion(sessionId, questionId)
                }
            }.onFailure { Timber.e(it, "restore practice attempt failed") }.getOrNull() ?: return@launch

            val currentId = _questions.value.getOrNull(_currentIndex.value)?.id
            if (currentId != questionId || hasLocalAttemptState()) return@launch

            savedStateHandle[KEY_ATTEMPT_ID] = attempt.id
            savedStateHandle[KEY_KEYWORDS] = attempt.userKeywords
            savedStateHandle[KEY_OUTLINE] = attempt.outline
            savedStateHandle[KEY_BODY] = attempt.body
            savedStateHandle[KEY_RATING] = PracticeSelfRating.fromDb(attempt.selfRating)?.name
            savedStateHandle[KEY_ERRORS] = ArrayList(
                attempt.errorReasons.mapNotNull(PracticeErrorReason::fromDb).map { it.name }.distinct().sorted(),
            )
            savedStateHandle[KEY_ATTEMPT_STAGE] = restoredStage(attempt).name
            savedStateHandle[KEY_SHOW_ANSWER] = attempt.revealedAt != null
        }
    }

    private fun hasLocalAttemptState(): Boolean =
        keywords.value.isNotBlank() || outline.value.isNotBlank() || body.value.isNotBlank() ||
            attemptStage.value != PracticeAttemptStage.ANSWERING.name || _showAnswer.value ||
            selectedRating.value != null || selectedErrors.value.isNotEmpty()

    private fun persistedStage(attempt: PracticeAttemptEntity): PracticeAttemptStage = when {
        attempt.completedAt != null -> PracticeAttemptStage.COMPLETED
        attempt.selfRating != null -> PracticeAttemptStage.ASSESSED
        attempt.revealedAt != null -> PracticeAttemptStage.REVEALED
        attempt.userKeywords.isNotBlank() || attempt.outline.isNotBlank() || attempt.body.isNotBlank() ->
            PracticeAttemptStage.SAVED
        else -> PracticeAttemptStage.ANSWERING
    }

    private fun restoredStage(attempt: PracticeAttemptEntity): PracticeAttemptStage = persistedStage(attempt)

    internal var currentTimeMillis: () -> Long = System::currentTimeMillis

    private companion object {
        const val KEY_QUESTION_ID = "questionId"
        const val KEY_TYPE = "type"
        const val KEY_SUBJECT = "subject"
        const val KEY_YEAR = "year"
        const val KEY_PAPER = "paper"
        const val KEY_CURRENT_INDEX = "practice_current_index"
        const val KEY_SHOW_ANSWER = "practice_show_answer"
        const val KEY_KEYWORDS = "practice_keywords"
        const val KEY_OUTLINE = "practice_outline"
        const val KEY_BODY = "practice_body"
        const val KEY_ATTEMPT_ID = "practice_attempt_id"
        const val KEY_ATTEMPT_STAGE = "practice_attempt_stage"
        const val KEY_RATING = "practice_rating"
        const val KEY_ERRORS = "practice_errors"
        const val KEY_SESSION_ID = "practice_session_id"
        const val FILTER_ALL = "ALL"

        /** 防连击窗口：400ms 内"会了/不会"只生效一次（v0.9.35 审计修复） */
        const val ADVANCE_LOCK_MS = 400L
    }
}

/** 背题进度（index = 当前第 index+1 题；total = 筛选集总数）。 */
@Immutable
data class Progress(
    val index: Int,
    val total: Int,
)
