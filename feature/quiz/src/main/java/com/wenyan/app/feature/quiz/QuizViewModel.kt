package com.wenyan.app.feature.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ExamQuestionWithSubject
import com.wenyan.app.core.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
 * - 完整保留 answerFramework / sampleEssay / answerStatus 等字段供 UI 展示
 * - 维护展开状态 [expandedQuestionIds]，控制答题框架/范文的折叠展开
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val examRepository: ExamRepository,
) : ViewModel() {

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    /** 展开状态：记录当前展开答题区的题目ID集合 */
    private val _expandedQuestionIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedQuestionIds: StateFlow<Set<String>> = _expandedQuestionIds.asStateFlow()

    /**
     * UI 状态：合并年份列表 + 选中年份的题目列表（含科目判定信息）。
     *
     * 使用 flatMapLatest 在切换年份时自动取消上一个年份的订阅。
     * 使用 [ExamRepository.getExamQuestionsWithSubjectInfo] 获取科目判定信息，
     * 解决 610/801 代码语义翻转问题（Spec Task 26）。
     */
    val uiState: StateFlow<QuizUiState> = combine(
        examRepository.getAvailableYears(),
        _selectedYear,
    ) { years, selected ->
        years to selected
    }.flatMapLatest { (years, selected) ->
        if (selected != null) {
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuizUiState(isLoading = true),
    )

    /** 选择某年份，加载对应题目 */
    fun selectYear(year: Int) {
        _selectedYear.update { year }
    }

    /** 切换某题目的展开状态（折叠 ↔ 展开） */
    fun toggleExpanded(questionId: String) {
        _expandedQuestionIds.update { ids ->
            if (questionId in ids) ids - questionId else ids + questionId
        }
    }
}

/** 真题 UI 状态 */
data class QuizUiState(
    val isLoading: Boolean = false,
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val questions: List<QuizQuestionItem> = emptyList(),
)

/**
 * 真题列表项（阶段5增强）。
 *
 * 完整保留真题字段供 UI 展示：
 * - [content]：题目正文（完整，非截断）
 * - [score]：分值
 * - [angle]：考查角度
 * - [answerFramework]：答题框架（Spec：HAS_ANSWER 时录入，NO_ANSWER 时留空）
 * - [sampleEssay]：范文
 * - [answerStatus]：答案状态（HAS_ANSWER / NO_ANSWER / AI_GENERATED）
 * - [examPaperCode]：当年试卷代码（610 / 801 / 805 / 806）
 * - [materialText]：材料题原文
 * - [relatedPointIds]：关联知识点ID列表
 * - [subjectDisplayName]：科目显示名称（含试卷代码与年份标注，来自 SubjectResolution）
 * - [subjectWarning]：科目警告信息（如"年份待核实"）
 */
data class QuizQuestionItem(
    val id: String,
    val year: Int,
    val content: String,
    val questionType: String,
    val score: Int,
    val angle: String?,
    val answerFramework: String?,
    val sampleEssay: String?,
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
    sampleEssay = question.sampleEssay,
    answerStatus = question.answerStatus,
    examPaperCode = question.examPaperCode,
    materialText = question.materialText,
    relatedPointIds = question.relatedPointIds,
    subjectDisplayName = subjectResolution.displayName,
    subjectWarning = subjectResolution.warningMessage,
)
