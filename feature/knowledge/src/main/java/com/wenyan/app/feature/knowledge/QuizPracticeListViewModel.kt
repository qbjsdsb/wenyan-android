package com.wenyan.app.feature.knowledge

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.common.util.ExamContentCleaner
import com.wenyan.app.core.common.util.friendlyErrorMessage
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
 * 真题背题题型常量（v0.9.33 新增）。
 *
 * 与 seed_data.json / [com.wenyan.app.core.database.dao.ExamQuestionDao] 的
 * question_type 字符串一致。背题专项只收录名词解释 + 简答，
 * ESSAY 由论述题板块独立承载（见 [QuizPracticeListViewModel] 注释）。
 */
object QuizPracticeTypes {
    const val TERM_EXPLANATION = "TERM_EXPLANATION"
    const val SHORT_ANSWER = "SHORT_ANSWER"

    /** 背题专项收录的全部题型（严格排除 ESSAY，避免与论述题重复） */
    val ALL: List<String> = listOf(TERM_EXPLANATION, SHORT_ANSWER)
}

/**
 * 真题背题列表 ViewModel（v0.9.33 新增）。
 *
 * 数据源：[KnowledgeRepository.observePracticeQuestions]（名词解释 + 简答，346 题）。
 *
 * 三维筛选（内存完成，346 条 < 5ms）：
 * - 题型：[selectedType]（null=全部 / TERM_EXPLANATION / SHORT_ANSWER）
 * - 科目：[selectedSubjectId]（null=全部）
 * - 年份：[selectedYear]（null=全部）
 *
 * 筛选状态独立 StateFlow（与 EssayListViewModel 解耦策略一致），
 * error/loading 态下也可即时切换，combine 重算不依赖数据流重启。
 *
 * 数据流（复用 retryTrigger + flatMapLatest 模式）：
 * ```
 * retryTrigger → flatMapLatest → flow { emit(loading); emitAll(combine(题目, 科目, 3筛选)) }
 * ```
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class QuizPracticeListViewModel @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
    private val chapterRepository: ChapterRepository,
) : ViewModel() {

    // ── 筛选状态（独立 StateFlow）──

    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    val selectedSubjectId: StateFlow<String?> = _selectedSubjectId.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    /** 重试触发器（与 EssayListViewModel 一致，驱动 flatMapLatest 重建内层流） */
    private val retryTrigger = MutableStateFlow(0)

    // ── UI 状态 ──

    val uiState: StateFlow<QuizPracticeListUiState> =
        retryTrigger
            .flatMapLatest {
                flow {
                    emit(QuizPracticeListUiState(isLoading = true))
                    emitAll(
                        combine(
                            knowledgeRepository.observePracticeQuestions(QuizPracticeTypes.ALL),
                            chapterRepository.observeSubjects(),
                            _selectedType,
                            _selectedSubjectId,
                            _selectedYear,
                        ) { questions, subjects, type, subjectId, year ->
                            val subjectMap = subjects.associate { it.id to it.name }
                            val years = questions.map { it.year }.distinct().sortedDescending()
                            val filtered = questions.filter { q ->
                                (type == null || q.questionType == type) &&
                                    (subjectId == null || q.subjectId == subjectId) &&
                                    (year == null || q.year == year)
                            }
                            val items = filtered.map { q ->
                                QuizPracticeListItem(
                                    id = q.id,
                                    questionType = q.questionType,
                                    subjectName = subjectMap[q.subjectId] ?: "未知科目",
                                    year = q.year,
                                    contentPreview = ExamContentCleaner.stripQuestionNumber(q.content).take(MAX_PREVIEW_LENGTH),
                                    answerLength = (q.answerFramework ?: "").length,
                                )
                            }
                            QuizPracticeListUiState(
                                isLoading = false,
                                questions = items,
                                totalCount = questions.size,
                                filteredCount = filtered.size,
                                subjects = subjects,
                                years = years,
                            )
                        }
                            .catch { e ->
                                Timber.e(e, "QuizPracticeListViewModel combine failed")
                                emit(
                                    QuizPracticeListUiState(
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
                // Eagerly：与 EssayListViewModel 一致，Tab/路由切换不中断 Room 流
                started = SharingStarted.Eagerly,
                initialValue = QuizPracticeListUiState(isLoading = true),
            )

    // ── 筛选操作 ──

    fun selectType(type: String?) {
        _selectedType.value = type
    }

    fun selectSubject(subjectId: String?) {
        _selectedSubjectId.value = subjectId
    }

    fun selectYear(year: Int?) {
        _selectedYear.value = year
    }

    fun clearFilters() {
        _selectedType.value = null
        _selectedSubjectId.value = null
        _selectedYear.value = null
    }

    fun retry() {
        retryTrigger.value++
    }

    private companion object {
        const val MAX_PREVIEW_LENGTH = 60
        const val TAG = "QuizPracticeListViewModel"
    }
}

/** 背题列表 UI 状态（v0.9.33）。 */
@Immutable
data class QuizPracticeListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val questions: List<QuizPracticeListItem> = emptyList(),
    val totalCount: Int = 0,
    val filteredCount: Int = 0,
    val subjects: List<SubjectEntity> = emptyList(),
    val years: List<Int> = emptyList(),
)

/** 背题列表项（v0.9.33）。 */
@Immutable
data class QuizPracticeListItem(
    val id: String,
    val questionType: String,
    val subjectName: String,
    val year: Int,
    val contentPreview: String,
    val answerLength: Int,
)
