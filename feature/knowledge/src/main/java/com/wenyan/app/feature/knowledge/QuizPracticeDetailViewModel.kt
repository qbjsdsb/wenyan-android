package com.wenyan.app.feature.knowledge

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.common.util.friendlyErrorMessage
import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
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
import kotlinx.coroutines.launch
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
    savedStateHandle: SavedStateHandle,
    private val knowledgeRepository: KnowledgeRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
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

    // ── 基础状态 ──

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 当前筛选集内的题目列表（顺序与列表页一致） */
    private val _questions = MutableStateFlow<List<ExamQuestionEntity>>(emptyList())
    val questions: StateFlow<List<ExamQuestionEntity>> = _questions.asStateFlow()

    /** 当前题目下标 */
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    /** 是否已显示答案 */
    private val _showAnswer = MutableStateFlow(false)
    val showAnswer: StateFlow<Boolean> = _showAnswer.asStateFlow()

    /** 用户提示（进错题本等），UI 用 Snackbar 展示 */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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
                        _questions.value = filtered
                        _currentIndex.value = startIndex
                        _isLoading.value = false
                    }
                }
        }
    }

    // ── 操作 ──

    fun toggleShowAnswer() {
        _showAnswer.value = !_showAnswer.value
    }

    fun showAnswer() {
        _showAnswer.value = true
    }

    fun hideAnswer() {
        _showAnswer.value = false
    }

    fun previous() {
        if (_currentIndex.value > 0) {
            _currentIndex.value -= 1
            hideAnswer()
        }
    }

    fun next() {
        if (_currentIndex.value < _questions.value.size - 1) {
            _currentIndex.value += 1
            hideAnswer()
        }
    }

    /** 标记"会了"→ 推进到下一题（已是最后一题则保持原位，由 UI 提示已完成） */
    fun markKnow() {
        // v0.9.35 审计修复：400ms 防连击窗，避免快速双击"会了"连跳两题
        if (!tryAcquireAdvanceLock()) return
        hideAnswer()
        if (_currentIndex.value < _questions.value.size - 1) {
            _currentIndex.value += 1
        } else {
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
        if (!tryAcquireAdvanceLock()) return
        val isLast = _currentIndex.value >= _questions.value.size - 1
        if (!isLast) {
            _currentIndex.value += 1
        }
        hideAnswer()
        viewModelScope.launch {
            try {
                wrongAnswerRepository.recordWrongAnswer(
                    pointId = null,
                    examQuestionId = question.id,
                    userAnswer = "",
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
    private var lastAdvanceAt = 0L

    /**
     * 时间源（可注入便于单测——纯 JVM 测试中 SystemClock.uptimeMillis 恒 0，
     * 防连击锁会永远锁住首次调用）。生产默认系统时钟。
     */
    internal var uptimeMillis: () -> Long = { android.os.SystemClock.uptimeMillis() }

    private fun tryAcquireAdvanceLock(): Boolean {
        val now = uptimeMillis()
        if (now - lastAdvanceAt < ADVANCE_LOCK_MS) return false
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

    private companion object {
        const val KEY_QUESTION_ID = "questionId"
        const val KEY_TYPE = "type"
        const val KEY_SUBJECT = "subject"
        const val KEY_YEAR = "year"
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
