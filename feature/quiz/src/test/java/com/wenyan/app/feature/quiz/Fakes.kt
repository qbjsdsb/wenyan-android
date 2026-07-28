package com.wenyan.app.feature.quiz

import com.wenyan.app.core.data.repository.ExamQuestionWithSubject
import com.wenyan.app.core.data.repository.ExamRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.data.util.SubjectResolution
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.database.entity.WrongAnswerWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * [ExamRepository] 的 Fake 实现,供 [QuizViewModelTest] / [WrongAnswerViewModelTest]
 * 使用(NF-PP5 Wave 3.2)。
 *
 * - [years]:getAvailableYears 返回的年份列表
 * - [questionsByYear]:getExamQuestionsWithSubjectInfo 按年份返回的题目列表
 * - [relatedPoints]:getRelatedKnowledgePoints 返回的知识点列表
 *
 * v0.8.21 新增 [yearsException] / [questionsException] 字段,
 * 供 retry-after-error 回归测试模拟数据流抛异常。
 */
class FakeExamRepository(
    var years: List<Int> = emptyList(),
    var questionsByYear: Map<Int, List<ExamQuestionWithSubject>> = emptyMap(),
    var relatedPoints: List<KnowledgePointEntity> = emptyList(),
    /** v0.8.21 新增:非 null 时 getAvailableYears() 抛此异常,用于测试 retry-after-error */
    var yearsException: Throwable? = null,
    /** v0.8.21 新增:非 null 时 getExamQuestionsWithSubjectInfo() 抛此异常 */
    var questionsException: Throwable? = null,
) : ExamRepository {

    override fun getExamQuestionsWithSubjectInfo(year: Int): Flow<List<ExamQuestionWithSubject>> {
        questionsException?.let { throw it }
        val list = questionsByYear[year] ?: emptyList()
        return flowOf(list)
    }

    override fun getExamQuestionsByYear(year: Int): Flow<List<ExamQuestionEntity>> {
        val list = questionsByYear[year]?.map { it.question } ?: emptyList()
        return flowOf(list)
    }

    override fun getAvailableYears(): Flow<List<Int>> {
        yearsException?.let { throw it }
        return flowOf(years)
    }

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
 *
 * v0.8.21 新增 [unresolvedException] / [allException] 字段,
 * 供 retry-after-error 回归测试模拟数据流抛异常。
 */
class FakeWrongAnswerRepository(
    initialAll: List<WrongAnswerWithDetails> = emptyList(),
    initialUnresolved: List<WrongAnswerWithDetails> = emptyList(),
) : WrongAnswerRepository {

    private val _all = MutableStateFlow(initialAll)
    private val _unresolved = MutableStateFlow(initialUnresolved)

    /** v0.8.21 新增:非 null 时 observeUnresolved() 抛此异常,用于测试 retry-after-error */
    var unresolvedException: Throwable? = null

    /** v0.8.21 新增:非 null 时 observeAll() 抛此异常,用于测试 retry-after-error */
    var allException: Throwable? = null

    val recordedWrongAnswers: MutableList<RecordedWrongAnswer> = mutableListOf()
    val resolvedIds: MutableList<String> = mutableListOf()
    val deletedIds: MutableList<String> = mutableListOf()

    /** v0.8.21 新增:非 null 时 recordWrongAnswer 抛此异常,用于测试 selfEvaluate 错题反馈 */
    var recordException: Throwable? = null

    /** 用于测试切换列表内容(模拟 markResolved 后流重发) */
    fun setAll(newList: List<WrongAnswerWithDetails>) {
        _all.value = newList
    }

    fun setUnresolved(newList: List<WrongAnswerWithDetails>) {
        _unresolved.value = newList
    }

    override fun observeAll(): Flow<List<WrongAnswerWithDetails>> {
        // v0.8.21:exception 非 null 时抛异常,模拟数据库异常
        // (用 flow { throw } 包装,使异常在 collect 时抛出,与真实 Room Flow 行为一致)
        allException?.let { return flow { throw it } }
        return _all.asStateFlow()
    }

    override fun observeUnresolved(): Flow<List<WrongAnswerWithDetails>> {
        unresolvedException?.let { return flow { throw it } }
        return _unresolved.asStateFlow()
    }

    override fun observeByPoint(pointId: String): Flow<List<WrongAnswerEntity>> = flowOf(emptyList())

    override fun observeByExamQuestion(examQuestionId: String): Flow<List<WrongAnswerEntity>> = flowOf(emptyList())

    override suspend fun recordWrongAnswer(
        pointId: String?,
        examQuestionId: String?,
        userAnswer: String,
        correctAnswer: String?,
        source: String,
    ): String {
        // v0.8.21:exception 非 null 时抛异常,模拟错题记录失败
        recordException?.let { throw it }
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
