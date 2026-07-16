package com.wenyan.app.feature.quiz

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ExamQuestionWithSubject
import com.wenyan.app.core.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
 *
 * 进程被杀恢复（NF-L3 修复）：
 * - [selectedYear] + [expandedQuestionIds] 持久化到 [SavedStateHandle]，
 *   进程被杀后恢复真题浏览位置与展开状态。
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val examRepository: ExamRepository,
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
     * 重试触发器（P0-6 新增）。点击重试时自增，[flatMapLatest] 会重新订阅数据流。
     */
    private val _retryTrigger = MutableStateFlow(0)

    /**
     * UI 状态：合并年份列表 + 选中年份的题目列表（含科目判定信息）。
     *
     * 使用 flatMapLatest 在切换年份时自动取消上一个年份的订阅。
     * 使用 [ExamRepository.getExamQuestionsWithSubjectInfo] 获取科目判定信息，
     * 解决 610/801 代码语义翻转问题（Spec Task 26）。
     *
     * NF-L3 修复：_selectedYear 用 -1 表示 null（SavedStateHandle 持久化），
     * 此处映射回 nullable 供 UI 使用。
     *
     * P0-6 修复：加 [catch] 捕获数据流异常，避免异常冒泡导致 app 崩溃。
     */
    val uiState: StateFlow<QuizUiState> = _retryTrigger
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = QuizUiState(isLoading = true),
        )

    /** 选择某年份，加载对应题目（NF-L3 修复：持久化到 SavedStateHandle） */
    fun selectYear(year: Int) {
        savedStateHandle["selectedYear"] = year
    }

    /** 重试加载（P0-6 新增） */
    fun retry() {
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
 * - [sampleEssay]：范文
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
