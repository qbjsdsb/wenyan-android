package com.wenyan.app.feature.quiz

import com.wenyan.app.core.data.repository.ExamQuestionWithSubject
import com.wenyan.app.core.data.repository.ExamRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.data.util.SubjectResolution
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * [ExamRepository] 的 Fake 实现,供 [QuizViewModelTest] / [WrongAnswerViewModelTest]
 * 使用(NF-PP5 Wave 3.2)。
 *
 * - [years]:getAvailableYears 返回的年份列表
 * - [questionsByYear]:getExamQuestionsWithSubjectInfo 按年份返回的题目列表
 * - [relatedPoints]:getRelatedKnowledgePoints 返回的知识点列表
 */
class FakeExamRepository(
    var years: List<Int> = emptyList(),
    var questionsByYear: Map<Int, List<ExamQuestionWithSubject>> = emptyMap(),
    var relatedPoints: List<KnowledgePointEntity> = emptyList(),
) : ExamRepository {

    override fun getExamQuestionsWithSubjectInfo(year: Int): Flow<List<ExamQuestionWithSubject>> {
        val list = questionsByYear[year] ?: emptyList()
        return flowOf(list)
    }

    override fun getExamQuestionsByYear(year: Int): Flow<List<ExamQuestionEntity>> {
        val list = questionsByYear[year]?.map { it.question } ?: emptyList()
        return flowOf(list)
    }

    override fun getAvailableYears(): Flow<List<Int>> = flowOf(years)

    override fun getRelatedKnowledgePoints(questionId: String): Flow<List<KnowledgePointEntity>> =
        flowOf(relatedPoints)
}

/**
 * [WrongAnswerRepository] 的 Fake 实现(feature/quiz 测试用,NF-PP5 Wave 3.2)。
 *
 * 与 feature/cards 测试中的 FakeWrongAnswerRepository 结构一致,
 * 但放在 feature/quiz 测试包内,保持模块隔离(测试代码不跨模块共享)。
 *
 * - [initialAll]:observeAll 初始数据
 * - [initialUnresolved]:observeUnresolved 初始数据
 * - [recordedWrongAnswers]:记录所有 recordWrongAnswer 调用
 * - [resolvedIds]:记录所有 markResolved 调用的 id
 * - [deletedIds]:记录所有 deleteById 调用的 id
 */
class FakeWrongAnswerRepository(
    initialAll: List<WrongAnswerEntity> = emptyList(),
    initialUnresolved: List<WrongAnswerEntity> = emptyList(),
) : WrongAnswerRepository {

    private val _all = MutableStateFlow(initialAll)
    private val _unresolved = MutableStateFlow(initialUnresolved)

    val recordedWrongAnswers: MutableList<RecordedWrongAnswer> = mutableListOf()
    val resolvedIds: MutableList<String> = mutableListOf()
    val deletedIds: MutableList<String> = mutableListOf()

    /** 用于测试切换列表内容(模拟 markResolved 后流重发) */
    fun setAll(newList: List<WrongAnswerEntity>) {
        _all.value = newList
    }

    fun setUnresolved(newList: List<WrongAnswerEntity>) {
        _unresolved.value = newList
    }

    override fun observeAll(): Flow<List<WrongAnswerEntity>> = _all.asStateFlow()

    override fun observeUnresolved(): Flow<List<WrongAnswerEntity>> = _unresolved.asStateFlow()

    override fun observeByPoint(pointId: String): Flow<List<WrongAnswerEntity>> = flowOf(emptyList())

    override fun observeByExamQuestion(examQuestionId: String): Flow<List<WrongAnswerEntity>> = flowOf(emptyList())

    override suspend fun recordWrongAnswer(
        pointId: String?,
        examQuestionId: String?,
        userAnswer: String,
        correctAnswer: String?,
        source: String,
    ): String {
        recordedWrongAnswers.add(
            RecordedWrongAnswer(pointId, examQuestionId, userAnswer, correctAnswer, source),
        )
        return "wa_${recordedWrongAnswers.size}"
    }

    override suspend fun markResolved(id: String) {
        resolvedIds.add(id)
    }

    override suspend fun deleteById(id: String) {
        deletedIds.add(id)
    }

    override suspend fun countUnresolved(): Int = _unresolved.value.size
}

/**
 * 记录一次 recordWrongAnswer 调用(供断言用)。
 */
data class RecordedWrongAnswer(
    val pointId: String?,
    val examQuestionId: String?,
    val userAnswer: String,
    val correctAnswer: String?,
    val source: String,
)

/**
 * 创建测试用 ExamQuestionEntity(简化构造)。
 *
 * 默认 id="q_1", year=2024, answerStatus="HAS_ANSWER", answerFramework 非空。
 */
fun testExamQuestion(
    id: String = "q_1",
    year: Int = 2024,
    content: String = "试论述苏轼的文学成就",
    questionType: String = "ESSAY",
    score: Int = 25,
    answerFramework: String? = "1. 词坛贡献 2. 诗歌成就 3. 散文影响",
    answerStatus: String? = "HAS_ANSWER",
    examPaperCode: String? = "805",
): ExamQuestionEntity = ExamQuestionEntity(
    id = id,
    year = year,
    subjectId = "subj_1",
    questionType = questionType,
    content = content,
    score = score,
    angle = null,
    relatedPointIds = null,
    answerFramework = answerFramework,
    notes = null,
    createdAt = 1000L,
    examPaperCode = examPaperCode,
    answerStatus = answerStatus,
    materialText = null,
    sourceFile = null,
    sourcePage = null,
)

/**
 * 默认 SubjectResolution(用于测试,显示名"610 文学基础")。
 */
val TEST_SUBJECT_RESOLUTION = SubjectResolution(
    displayName = "610 文学基础",
    subjectName = "文学基础",
    direction = null,
    isVerified = true,
    warningMessage = null,
)
