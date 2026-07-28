package com.wenyan.app.feature.quiz

import com.wenyan.app.core.data.repository.ExamQuestionWithSubject
import com.wenyan.app.core.data.repository.ExamRepository
import com.wenyan.app.core.data.repository.IntervalPreview
import com.wenyan.app.core.data.repository.SchedulingRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.data.util.SubjectResolution
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.database.entity.WrongAnswerWithDetails
import com.wenyan.app.core.fsrs.Rating
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
 * - [initialDue]:observeDueWrongAnswers 初始数据(v0.9.4 新增,FSRS 待复习错题)
 * - [recordedWrongAnswers]:记录所有 recordWrongAnswer 调用
 * - [resolvedIds]:记录所有 markResolved 调用的 id
 * - [deletedIds]:记录所有 deleteById 调用的 id
 *
 * v0.8.21 新增 [unresolvedException] / [allException] 字段,
 * 供 retry-after-error 回归测试模拟数据流抛异常。
 *
 * v0.9.4 新增 [dueException] 字段,供 DUE 过滤模式 retry-after-error 测试。
 */
class FakeWrongAnswerRepository(
    initialAll: List<WrongAnswerWithDetails> = emptyList(),
    initialUnresolved: List<WrongAnswerWithDetails> = emptyList(),
    initialDue: List<WrongAnswerWithDetails> = emptyList(),
) : WrongAnswerRepository {

    private val _all = MutableStateFlow(initialAll)
    private val _unresolved = MutableStateFlow(initialUnresolved)
    private val _due = MutableStateFlow(initialDue)

    /** v0.8.21 新增:非 null 时 observeUnresolved() 抛此异常,用于测试 retry-after-error */
    var unresolvedException: Throwable? = null

    /** v0.8.21 新增:非 null 时 observeAll() 抛此异常,用于测试 retry-after-error */
    var allException: Throwable? = null

    /** v0.9.4 新增:非 null 时 observeDueWrongAnswers() 抛此异常 */
    var dueException: Throwable? = null

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

    /** v0.9.4 新增:用于测试切换 DUE 列表内容(模拟评分后流重发) */
    fun setDue(newList: List<WrongAnswerWithDetails>) {
        _due.value = newList
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

    /**
     * v0.9.4 新增:观察待复习错题(FSRS 调度)。
     *
     * 测试中忽略 now 参数,直接返回 [_due] StateFlow,由测试通过 [setDue] 控制内容。
     * 真实实现按 sched_next_review_at <= now 过滤,测试不需要验证 SQL 语义
     * (DAO 层 SQL 由 SchedulingRepositoryTest 用 in-memory Room 验证)。
     */
    override fun observeDueWrongAnswers(now: Long): Flow<List<WrongAnswerWithDetails>> {
        dueException?.let { return flow { throw it } }
        return _due.asStateFlow()
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
 * [SchedulingRepository] 的 Fake 实现(feature/quiz 测试用,v0.9.4 新增)。
 *
 * 专门为 [WrongAnswerViewModelTest] 的 `rateWrongAnswer` 测试设计:
 * - [rateWrongAnswerResult]:rateWrongAnswer 返回的 WrongAnswerEntity(默认非 null)
 * - [rateWrongAnswerException]:非 null 时 rateWrongAnswer 抛异常
 * - [rateWrongAnswerCalls]:记录所有 rateWrongAnswer 调用参数(id + rating)
 *
 * 与 feature/cards 的 FakeSchedulingRepository 区别:
 * - feature/cards 版本侧重 rateCard(知识点卡片评分)
 * - feature/quiz 版本侧重 rateWrongAnswer(错题 FSRS 调度)
 * - 两者都需实现完整 SchedulingRepository 接口,但默认值侧重不同
 *
 * rateCard / previewIntervals 在本测试中不会被调用(错题本不涉及知识点卡片评分),
 * 但仍需实现以满足接口契约,默认返回安全值(null/emptyMap)。
 */
class FakeSchedulingRepository(
    var rateWrongAnswerResult: WrongAnswerEntity? = WrongAnswerEntity(
        id = "fake_wa",
        pointId = "fake_point",
        examQuestionId = null,
        userAnswer = "fake answer",
        correctAnswer = "fake correct",
        source = "CARD_AGAIN",
        wrongCount = 1,
        lastWrongAt = 1000L,
        resolvedAt = null,
        aiExplanation = null,
        createdAt = 500L,
        // FSRS 调度字段(模拟 GOOD 评分后的状态)
        schedState = "REVIEW",
        schedStability = 5f,
        schedDifficulty = 5f,
        schedLastReviewAt = 1000L,
        schedNextReviewAt = 2000L,
        schedReviewCount = 1,
        schedLapses = 0,
        schedElapsedDays = 0,
        schedScheduledDays = 1,
        schedReps = 1,
    ),
    var rateWrongAnswerException: Throwable? = null,
) : SchedulingRepository {

    val rateWrongAnswerCalls: MutableList<Pair<String, Rating>> = mutableListOf()

    /** 错题本测试不涉及知识点卡片评分,返回 null 即可 */
    override suspend fun rateCard(
        pointId: String,
        rating: Rating,
        cardType: CardTemplateType,
    ): MemoRecordEntity? = null

    /** 错题本测试不涉及预览,返回空 Map */
    override suspend fun previewIntervals(
        pointId: String,
        cardType: CardTemplateType,
    ): Map<Rating, IntervalPreview> = emptyMap()

    /**
     * 错题 FSRS 评分调度 Fake 实现。
     *
     * 异常注入:[rateWrongAnswerException] 非 null 时抛出,用于测试 ViewModel 错误处理。
     * 调用记录:[rateWrongAnswerCalls] 记录所有调用,用于断言 ViewModel 是否正确调用。
     * 返回值:[rateWrongAnswerResult] 默认非 null,测试可自定义。
     */
    override suspend fun rateWrongAnswer(
        wrongAnswerId: String,
        rating: Rating,
    ): WrongAnswerEntity? {
        rateWrongAnswerException?.let { throw it }
        rateWrongAnswerCalls.add(wrongAnswerId to rating)
        return rateWrongAnswerResult
    }
}

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
