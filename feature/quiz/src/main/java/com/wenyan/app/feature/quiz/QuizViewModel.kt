package com.wenyan.app.feature.quiz

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ExamQuestionWithSubject
import com.wenyan.app.core.data.repository.ExamRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 真题练习模块 ViewModel（阶段5增强）。
 *
 * 注入 [ExamRepository] 加载真实真题数据。
 * 年份选择 → 加载该年题目列表（含科目判定信息）。
 *
 * 增强点：
 * - 使用 [ExamRepository.getExamQuestionsWithSubjectInfo] 获取科目判定信息，
 *   正确展示"610 文学基础（2022年代码）"等历史代码语义
 * - 完整保留 answerFramework / answerStatus 等字段供 UI 展示
 * - 维护展开状态 [expandedQuestionIds]，控制答题框架的折叠展开
 *
 * 进程被杀恢复（NF-L3 修复）：
 * - [selectedYear] + [expandedQuestionIds] 持久化到 [SavedStateHandle]，
 *   进程被杀后恢复真题浏览位置与展开状态。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val examRepository: ExamRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
) : ViewModel() {

    // NF-L3 修复：selectedYear 持久化到 SavedStateHandle（用 -1 表示 null，避免可空类型序列化问题）
    private val _selectedYear = savedStateHandle.getStateFlow("selectedYear", -1)
    val selectedYear: StateFlow<Int?> = _selectedYear.map { if (it == -1) null else it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** 展开状态：记录当前展开答题区的题目ID集合（NF-L3 修复：持久化到 SavedStateHandle） */
    private val _expandedQuestionIds = MutableStateFlow<Set<String>>(
        (savedStateHandle.get<ArrayList<String>>("expandedQuestionIds") ?: emptyList()).toSet(),
    )
    val expandedQuestionIds: StateFlow<Set<String>> = _expandedQuestionIds.asStateFlow()

    /**
     * 答题状态(NF-PP5 Wave 3.2):每道题的答题/自评状态。
     *
     * 独立于 [uiState](从 examRepository 流重建)存放,避免流重发时丢失用户输入。
     * key = questionId,value = [QuizAnswerState]。
     */
    private val _answers = MutableStateFlow<Map<String, QuizAnswerState>>(emptyMap())
    val answers: StateFlow<Map<String, QuizAnswerState>> = _answers.asStateFlow()

    /**
     * 重试触发器（P0-6 新增）。点击重试时自增，[flatMapLatest] 会重新订阅数据流。
     */
    private val _retryTrigger = MutableStateFlow(0)

    /**
     * UI 状态（P1-4 改造为 MutableStateFlow 包装）。
     *
     * 合并年份列表 + 选中年份的题目列表（含科目判定信息）。
     *
     * 使用 flatMapLatest 在切换年份时自动取消上一个年份的订阅。
     * 使用 [ExamRepository.getExamQuestionsWithSubjectInfo] 获取科目判定信息，
     * 解决 610/801 代码语义翻转问题（Spec Task 26）。
     *
     * NF-L3 修复：_selectedYear 用 -1 表示 null（SavedStateHandle 持久化），
     * 此处映射回 nullable 供 UI 使用。
     *
     * P0-6 修复：加 [catch] 捕获数据流异常，避免异常冒泡导致 app 崩溃。
     *
     * P1-4 修复：原 [stateIn] 模式 retry() 后 UI 无立即 loading 反馈，
     * 现改为 MutableStateFlow + [collect]，retry() 可立即设置 isLoading=true，
     * 保留 selectedYear 等其他字段不清空。
     */
    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState(isLoading = true))
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _retryTrigger
                .flatMapLatest {
                    combine(
                        examRepository.getAvailableYears(),
                        _selectedYear,
                    ) { years, selected ->
                        years to selected
                    }.flatMapLatest { (years, selected) ->
                        if (selected != -1) {
                            examRepository.getExamQuestionsWithSubjectInfo(selected).map { questionsWithSubject ->
                                QuizUiState(
                                    isLoading = false,
                                    availableYears = years,
                                    selectedYear = selected,
                                    questions = questionsWithSubject.map { it.toUiItem() },
                                )
                            }
                        } else {
                            flowOf(
                                QuizUiState(
                                    isLoading = false,
                                    availableYears = years,
                                    selectedYear = null,
                                    questions = emptyList(),
                                ),
                            )
                        }
                    }
                }
                .catch { e ->
                    emit(QuizUiState(error = e.message ?: "加载失败"))
                }
                .collect { _uiState.value = it }
        }
    }

    /** 选择某年份，加载对应题目（NF-L3 修复：持久化到 SavedStateHandle） */
    fun selectYear(year: Int) {
        savedStateHandle["selectedYear"] = year
    }

    /**
     * 重试加载（P0-6 新增，P1-4 增强）。
     *
     * P1-4 修复：先立即设置 isLoading=true 并清空 error，保留 selectedYear 不变，
     * 让 UI 立即显示 loading 反馈；再增加 [_retryTrigger] 触发数据流重新订阅。
     */
    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        _retryTrigger.value++
    }

    /** 切换某题目的展开状态（折叠 ↔ 展开）（NF-L3 修复：持久化到 SavedStateHandle） */
    fun toggleExpanded(questionId: String) {
        _expandedQuestionIds.update { ids ->
            val newIds = if (questionId in ids) ids - questionId else ids + questionId
            // 同步到 SavedStateHandle（用 ArrayList 以兼容 Bundle 序列化）
            savedStateHandle["expandedQuestionIds"] = ArrayList(newIds)
            newIds
        }
    }

    /**
     * 更新答题输入文本(NF-PP5 Wave 3.2)。
     *
     * 仅在未提交时允许编辑。提交后答案锁定,需通过自评判定对错。
     */
    fun updateAnswer(questionId: String, text: String) {
        _answers.update { current ->
            val existing = current[questionId] ?: QuizAnswerState()
            if (existing.isSubmitted) return@update current  // 已提交,不允许编辑
            current + (questionId to existing.copy(userAnswer = text))
        }
    }

    /**
     * 提交答案(NF-PP5 Wave 3.2 + v0.7.3 P0 修复)。
     *
     * 标记答案为已提交,UI 随后展示参考答案 + 自评按钮。
     * 空白答案不允许提交。
     *
     * v0.7.3 P0 修复:移除 `if (!hasReference) return` 阻断逻辑。
     * 原实现要求题目必须有参考答案才允许提交,导致 481 道无答案真题
     * (answerFramework 为 null)无法进入自评流程,
     * 错题本(SOURCE_QUIZ_WRONG)永不写入。
     * 现允许无答案题目也提交:用户输入答案后可自评,
     * 自评错误时 correctAnswer 字段降级为"暂无参考答案"占位文本,
     * 错题本仍会记录用户答案与题目关联,后续可通过 AI 助手补全。
     */
    fun submitAnswer(questionId: String) {
        val question = _uiState.value.questions.find { it.id == questionId } ?: return

        val currentAnswer = _answers.value[questionId]?.userAnswer ?: ""
        if (currentAnswer.isBlank()) return

        _answers.update { current ->
            val existing = current[questionId] ?: QuizAnswerState()
            if (existing.isSubmitted) return@update current  // 防重复提交
            current + (questionId to existing.copy(isSubmitted = true))
        }

        // 自动展开参考答案区(让用户对照参考答案自评;无答案时展示自评引导)
        _expandedQuestionIds.update { it + questionId }
        savedStateHandle["expandedQuestionIds"] = ArrayList(_expandedQuestionIds.value)
    }

    /**
     * 自评对错(NF-PP5 Wave 3.2)。
     *
     * 用户提交答案后对照参考答案自评。判定为"错"时记录到错题本。
     * 自评是一次性的:已自评后不允许更改(避免重复记录/删除逻辑复杂化)。
     *
     * @param questionId 题目 ID
     * @param isCorrect  用户自评是否正确
     */
    fun selfEvaluate(questionId: String, isCorrect: Boolean) {
        val question = _uiState.value.questions.find { it.id == questionId } ?: return
        val answer = _answers.value[questionId] ?: return
        if (!answer.isSubmitted || answer.isSelfEvaluated) return  // 未提交或已自评,忽略

        _answers.update { current ->
            current + (questionId to answer.copy(isCorrect = isCorrect, isSelfEvaluated = true))
        }

        // 答错时记录到错题本
        if (!isCorrect) {
            viewModelScope.launch {
                try {
                    // v0.7.3 P0:无参考答案时 correctAnswer 降级为占位文本,
                    // 避免传 null 到错题本导致 UI 显示异常,后续可通过 AI 助手补全
                    val correctAnswer = question.answerFramework
                        ?: "（暂无参考答案，可使用 AI 助手生成）"
                    wrongAnswerRepository.recordWrongAnswer(
                        pointId = null,
                        examQuestionId = questionId,
                        userAnswer = answer.userAnswer,
                        correctAnswer = correctAnswer,
                        source = WrongAnswerRepository.SOURCE_QUIZ_WRONG,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // 错题记录失败不阻塞主流程(自评状态已更新)
                }
            }
        }
    }
}

/** 真题 UI 状态 */
data class QuizUiState(
    val isLoading: Boolean = false,
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val questions: List<QuizQuestionItem> = emptyList(),
    /** 加载失败时的错误信息（P0-6 新增） */
    val error: String? = null,
)

/**
 * 真题列表项（阶段5增强）。
 *
 * 完整保留真题字段供 UI 展示：
 * - [content]：题目正文（完整，非截断）
 * - [score]：分值
 * - [angle]：考查角度
 * - [answerFramework]：答题框架（Spec：HAS_ANSWER 时录入，NO_ANSWER 时留空）
 * - [answerStatus]：答案状态（HAS_ANSWER / NO_ANSWER / AI_GENERATED）
 * - [examPaperCode]：当年试卷代码（610 / 801 / 805 / 806）
 * - [materialText]：材料题原文
 * - [relatedPointIds]：关联知识点ID列表
 * - [subjectDisplayName]：科目显示名称（含试卷代码与年份标注，来自 SubjectResolution）
 * - [subjectWarning]：科目警告信息（如"年份待核实"）
 */
@Immutable
data class QuizQuestionItem(
    val id: String,
    val year: Int,
    val content: String,
    val questionType: String,
    val score: Int,
    val angle: String?,
    val answerFramework: String?,
    val answerStatus: String?,
    val examPaperCode: String?,
    val materialText: String?,
    val relatedPointIds: List<String>?,
    val subjectDisplayName: String,
    val subjectWarning: String?,
)

/** 将 [ExamQuestionWithSubject] 转换为 UI 列表项 */
private fun ExamQuestionWithSubject.toUiItem() = QuizQuestionItem(
    id = question.id,
    year = question.year,
    content = question.content,
    questionType = question.questionType,
    score = question.score,
    angle = question.angle,
    answerFramework = question.answerFramework,
    answerStatus = question.answerStatus,
    examPaperCode = question.examPaperCode,
    materialText = question.materialText,
    relatedPointIds = question.relatedPointIds,
    subjectDisplayName = subjectResolution.displayName,
    subjectWarning = subjectResolution.warningMessage,
)

/**
 * 答题状态(NF-PP5 Wave 3.2)。
 *
 * 生命周期:输入中 → [isSubmitted]=true(提交,展示参考答案) → [isSelfEvaluated]=true(自评完成)。
 *
 * @property userAnswer       用户输入的答案
 * @property isSubmitted      是否已提交(提交后锁定编辑,展示参考答案 + 自评按钮)
 * @property isCorrect        自评结果(仅在 [isSelfEvaluated]=true 时有效)
 * @property isSelfEvaluated  是否已完成自评(自评后不可更改)
 */
@Immutable
data class QuizAnswerState(
    val userAnswer: String = "",
    val isSubmitted: Boolean = false,
    val isCorrect: Boolean = false,
    val isSelfEvaluated: Boolean = false,
)
