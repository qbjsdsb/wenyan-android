package com.wenyan.app.feature.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ExamRepository
import com.wenyan.app.core.database.entity.ExamQuestionEntity
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
 * 真题练习模块 ViewModel。
 *
 * 注入 [ExamRepository] 加载真实真题数据。
 * 年份选择 → 加载该年题目列表。
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val examRepository: ExamRepository,
) : ViewModel() {

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    /**
     * UI 状态：合并年份列表 + 选中年份的题目列表。
     *
     * 使用 flatMapLatest 在切换年份时自动取消上一个年份的订阅。
     */
    val uiState: StateFlow<QuizUiState> = combine(
        examRepository.getAvailableYears(),
        _selectedYear,
    ) { years, selected ->
        years to selected
    }.flatMapLatest { (years, selected) ->
        if (selected != null) {
            examRepository.getExamQuestionsByYear(selected).map { questions ->
                QuizUiState(
                    isLoading = false,
                    availableYears = years,
                    selectedYear = selected,
                    questions = questions.map { it.toUiItem() },
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

    // 选择某年份，加载对应题目
    fun selectYear(year: Int) {
        _selectedYear.update { year }
    }
}

// 真题 UI 状态
data class QuizUiState(
    val isLoading: Boolean = false,
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val questions: List<QuizQuestionItem> = emptyList(),
)

// 真题列表项
data class QuizQuestionItem(
    val id: String,
    val year: Int,
    val title: String,
    val questionType: String,
    val subject: String,
)

private fun ExamQuestionEntity.toUiItem() = QuizQuestionItem(
    id = id,
    year = year,
    title = content.take(80),
    questionType = questionType,
    subject = subjectId,
)
