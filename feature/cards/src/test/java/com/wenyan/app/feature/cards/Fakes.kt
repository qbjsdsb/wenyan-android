package com.wenyan.app.feature.cards

import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.cards.ClozeQuoteCard
import com.wenyan.app.core.data.cards.DistinctionCard
import com.wenyan.app.core.data.cards.EssayPointsCard
import com.wenyan.app.core.data.cards.SocietyTermFields
import com.wenyan.app.core.data.cards.TermCategory
import com.wenyan.app.core.data.cards.TermExplanationCard
import com.wenyan.app.core.data.repository.CardRepository
import com.wenyan.app.core.data.repository.CardFrequencyFilter
import com.wenyan.app.core.data.repository.CardSettings
import com.wenyan.app.core.data.repository.CardSettingsRepository
import com.wenyan.app.core.data.repository.IntervalPreview
import com.wenyan.app.core.data.repository.SchedulingRepository
import com.wenyan.app.core.data.repository.StudyProgress
import com.wenyan.app.core.data.repository.TodayStudyQueue
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.database.entity.WrongAnswerWithDetails
import com.wenyan.app.core.fsrs.Rating
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * [CardRepository] 的 Fake 实现,供 [CardsViewModelTest] 使用(NF-PP5 Wave 3.2)。
 *
 * 通过 [initialCards] 可控地注入测试卡片流。
 *
 * v0.8.20 P1-2 新增 [throwOnGetCards] 参数,用于测试 CardsViewModel 的加载失败分支:
 * - 非 null 时 [getCardsForReview] 返回一个在 collect 时抛出指定异常的 Flow,
 *   触发 CardsViewModel init 块的 .catch 分支,验证错误提示文案。
 * - 默认 null,与历史行为兼容(返回 [initialCards] 对应的 StateFlow)。
 *
 * 用 flow { throw ... } 而非直接在 getCardsForReview() 抛异常,确保异常在 collect
 * 时触发(模拟真实场景:DB 查询异常发生在 Flow collect 阶段,而非构造阶段)。
 */
class FakeCardRepository(
    initialCards: List<CardTemplate> = emptyList(),
    var throwOnGetCards: Throwable? = null,
) : CardRepository {

    private val _cards = MutableStateFlow(initialCards)

    fun emitCards(cards: List<CardTemplate>) {
        _cards.value = cards
    }

    override fun getCardsForReview(): Flow<List<CardTemplate>> =
        throwOnGetCards?.let { e ->
            flow { throw e }
        } ?: _cards.asStateFlow()

    // v0.9.29：今日任务数据（默认空，测试可按需覆写 todayQueue/progress）
    private val _todayQueue = MutableStateFlow(TodayStudyQueue(emptyList(), emptyList()))
    private val _progress = MutableStateFlow(StudyProgress(0, 0))
    val todayQueue: MutableStateFlow<TodayStudyQueue> = _todayQueue
    val progress: MutableStateFlow<StudyProgress> = _progress

    override fun getTodayStudyQueue(): Flow<TodayStudyQueue> = _todayQueue

    override fun getStudyProgress(): Flow<StudyProgress> = _progress
}

/**
 * [CardSettingsRepository] 的 Fake 实现（v0.9.29）。
 *
 * 内存 StateFlow，测试可注入初始 [CardSettings] 或调用 setter 更新。
 */
class FakeCardSettingsRepository(
    initial: CardSettings = CardSettings(),
) : CardSettingsRepository {
    private val _settings = MutableStateFlow(initial)
    override val cardSettings = _settings

    override suspend fun setDailyNewLimit(limit: Int) {
        _settings.value = _settings.value.copy(dailyNewLimit = limit)
    }

    override suspend fun setFrequencyFilter(filter: CardFrequencyFilter) {
        _settings.value = _settings.value.copy(frequencyFilter = filter)
    }

    override suspend fun setSubjectFilters(subjects: Set<String>) {
        _settings.value = _settings.value.copy(subjectFilters = subjects)
    }

    override suspend fun setExamDate(millis: Long?) {
        _settings.value = _settings.value.copy(examDateMillis = millis)
    }
}

/**
 * [SchedulingRepository] 的 Fake 实现,供 [CardsViewModelTest] 使用(NF-PP5 Wave 3.2)。
 *
 * - [rateCardResult]:rateCard 返回的 MemoRecordEntity(默认非 null)
 * - [throwException]:非 null 时 rateCard 抛异常
 * - [rateCardCalls]:记录所有 rateCard 调用参数
 * - [previewResults]:previewIntervals 返回的预设结果(v0.8.6 新增)
 *
 * v0.8.6 新增 previewIntervals 支持:测试可注入 [previewResults] 控制预览输出。
 * 默认返回 4 档预览(Again=1分钟/Hard=5分钟/Good=6天/Easy=12天),
 * 模拟真实 FSRS 输出便于 UI 测试。
 */
class FakeSchedulingRepository(
    var rateCardResult: MemoRecordEntity? = MemoRecordEntity(
        pointId = "fake",
        state = "REVIEW",
        stability = 10f,
        difficulty = 5f,
        lastReviewAt = 1000L,
        nextReviewAt = 2000L,
        reviewCount = 1,
        failCount = 0,
        elapsedDays = 0,
        scheduledDays = 1,
        reps = 1,
        inPriorityQueue = 0,
    ),
    var throwException: Throwable? = null,
    var previewResults: Map<Rating, IntervalPreview> = mapOf(
        Rating.AGAIN to IntervalPreview(Rating.AGAIN, 0, 60_000L, "1分钟"),
        Rating.HARD to IntervalPreview(Rating.HARD, 0, 300_000L, "5分钟"),
        Rating.GOOD to IntervalPreview(Rating.GOOD, 6, 6L * 86_400_000L, "6天"),
        Rating.EASY to IntervalPreview(Rating.EASY, 12, 12L * 86_400_000L, "12天"),
    ),
) : SchedulingRepository {

    val rateCardCalls: MutableList<Triple<String, Rating, CardTemplateType>> = mutableListOf()
    val previewCalls: MutableList<Pair<String, CardTemplateType>> = mutableListOf()
    /** v0.9.4 新增:记录 rateWrongAnswer 调用(cards 模块测试不调用,但接口需实现) */
    val rateWrongAnswerCalls: MutableList<Pair<String, Rating>> = mutableListOf()

    override suspend fun rateCard(
        pointId: String,
        rating: Rating,
        cardType: CardTemplateType,
    ): MemoRecordEntity? {
        throwException?.let { throw it }
        rateCardCalls.add(Triple(pointId, rating, cardType))
        return rateCardResult
    }

    override suspend fun previewIntervals(
        pointId: String,
        cardType: CardTemplateType,
    ): Map<Rating, IntervalPreview> {
        previewCalls.add(pointId to cardType)
        return previewResults
    }

    /**
     * v0.9.4 新增:错题 FSRS 评分调度 Fake 实现。
     *
     * cards 模块测试不调用此方法(错题调度由 WrongAnswerViewModel 触发),
     * 但 SchedulingRepository 接口扩展后必须实现,返回 null 安全兜底。
     * 若未来 cards 模块需要测试错题调度,可扩展 [rateWrongAnswerResult] 字段。
     */
    override suspend fun rateWrongAnswer(
        wrongAnswerId: String,
        rating: Rating,
    ): WrongAnswerEntity? {
        rateWrongAnswerCalls.add(wrongAnswerId to rating)
        return null
    }
}

/**
 * [WrongAnswerRepository] 的 Fake 实现,供 [CardsViewModelTest] /
 * [QuizViewModelTest] / [WrongAnswerViewModelTest] 使用(NF-PP5 Wave 3.2)。
 *
 * - [initialAll]:observeAll 初始数据(默认空)
 * - [initialUnresolved]:observeUnresolved 初始数据(默认空)
 * - [recordedWrongAnswers]:记录所有 recordWrongAnswer 调用(参数五元组)
 * - [resolvedIds]:记录所有 markResolved 调用的 id
 * - [deletedIds]:记录所有 deleteById 调用的 id
 * - [unresolvedCount]:countUnresolved 返回值
 */
class FakeWrongAnswerRepository(
    initialAll: List<WrongAnswerWithDetails> = emptyList(),
    initialUnresolved: List<WrongAnswerWithDetails> = emptyList(),
    var unresolvedCount: Int = 0,
    /** 模拟异步操作的延迟（毫秒，v0.9.18 新增，用于测试 isAddingBookmark 加载中状态） */
    var delayMs: Long = 0,
    /** 非 null 时 recordWrongAnswer 抛出指定异常（用于测试错误分支） */
    var throwOnRecord: Throwable? = null,
) : WrongAnswerRepository {

    private val _all = MutableStateFlow(initialAll)
    private val _unresolved = MutableStateFlow(initialUnresolved)

    val recordedWrongAnswers: MutableList<RecordedWrongAnswer> = mutableListOf()
    val resolvedIds: MutableList<String> = mutableListOf()
    val deletedIds: MutableList<String> = mutableListOf()

    override fun observeAll(): Flow<List<WrongAnswerWithDetails>> = _all.asStateFlow()

    override fun observeUnresolved(): Flow<List<WrongAnswerWithDetails>> = _unresolved.asStateFlow()

    /**
     * v0.9.4 新增:观察待复习错题(FSRS 调度)。
     *
     * cards 模块测试不依赖此方法(由 WrongAnswerViewModel 使用),
     * 但接口扩展后必须实现,返回空流安全兜底。
     */
    override fun observeDueWrongAnswers(now: Long): Flow<List<WrongAnswerWithDetails>> =
        flowOf(emptyList())

    override fun observeByPoint(pointId: String): Flow<List<WrongAnswerEntity>> = flowOf(emptyList())

    override fun observeByExamQuestion(examQuestionId: String): Flow<List<WrongAnswerEntity>> = flowOf(emptyList())

    override suspend fun recordWrongAnswer(
        pointId: String?,
        examQuestionId: String?,
        userAnswer: String,
        correctAnswer: String?,
        source: String,
    ): String {
        // 模拟异步操作延迟，用于测试 isAddingBookmark 加载中状态
        if (delayMs > 0) delay(delayMs)
        // 模拟异常，用于测试错误分支
        throwOnRecord?.let { throw it }
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

    override suspend fun countUnresolved(): Int = unresolvedCount
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
 * 创建测试用 ClozeQuoteCard(简化构造,最简单的 CardTemplate 子类)。
 *
 * 默认 pointId="point_1",front="苏轼",back="北宋文学家"。
 */
fun testClozeCard(
    front: String = "苏轼",
    back: String = "北宋文学家",
    pointId: String = "point_1",
): ClozeQuoteCard = ClozeQuoteCard(
    front = front,
    back = back,
    pointId = pointId,
    quote = "${front}____",
    blank = back,
    hint = "提示",
)

/**
 * 创建测试用 TermExplanationCard(v0.8.13 P1-5 新增)。
 *
 * 生产环境中 CardRepository.generateCardsFromKnowledgePoint 实际生成的是
 * TermExplanationCard(不是 ClozeQuoteCard),但原测试全部用 ClozeQuoteCard,
 * 导致生产卡片类型在 ViewModel 层无测试覆盖。
 *
 * 默认构造一个社团类名词解释卡(文学研究会),含结构化字段,
 * 用于验证 ViewModel 对生产卡片类型的处理(评分/调度/错题记录)。
 */
fun testTermCard(
    front: String = "文学研究会 — 时代",
    back: String = "1921年",
    pointId: String = "point_1",
    category: TermCategory = TermCategory.SOCIETY,
): TermExplanationCard = TermExplanationCard(
    front = front,
    back = back,
    pointId = pointId,
    category = category,
    society = if (category == TermCategory.SOCIETY) {
        SocietyTermFields(time = back, place = "北京", members = "郑振铎、沈雁冰")
    } else {
        null
    },
    work = null,
    fullExplanation = "文学研究会是1921年成立于北京的文学团体",
    studyText = null,
)

/**
 * 创建测试用 DistinctionCard(v0.8.13 P0-2 新增)。
 *
 * 用于验证 AGAIN 评分时 correctAnswer 从 differences 列表提取,
 * 而非从 back(占位文本"$item1 与 $item2 的区别见要点")提取。
 *
 * 默认对比"建安风骨"与"正始风骨",含 3 条结构化区别要点。
 */
fun testDistinctionCard(
    front: String = "区分：建安风骨 与 正始风骨",
    back: String = "建安风骨 与 正始风骨 的区别见要点",
    pointId: String = "point_1",
    item1: String = "建安风骨",
    item2: String = "正始风骨",
    differences: List<String> = listOf(
        "建安风骨产生于汉末建安年间,正始风骨产生于魏晋正始年间",
        "建安风骨代表作家为三曹七子,正始风骨代表作家为竹林七贤",
        "建安风骨风格慷慨悲凉,正始风骨风格玄远幽邃",
    ),
): DistinctionCard = DistinctionCard(
    front = front,
    back = back,
    pointId = pointId,
    item1 = item1,
    item2 = item2,
    differences = differences,
)

/**
 * 创建测试用 EssayPointsCard(v0.8.13 P0-2 新增)。
 *
 * 用于验证 AGAIN 评分时 correctAnswer 从 keyPoints 列表提取,
 * 而非从 back(summary 散文)提取。
 *
 * 默认论述题"建安风骨的文学史意义",含 3 个关键词要点。
 */
fun testEssayPointsCard(
    front: String = "建安风骨的文学史意义",
    back: String = "建安风骨是中国文学史上的重要转折点,标志着文人诗的成熟",
    pointId: String = "point_1",
    question: String = "建安风骨的文学史意义",
    keyPoints: List<String> = listOf(
        "标志着文人诗的成熟",
        "奠定五言诗基础",
        "影响后世边塞诗派",
    ),
): EssayPointsCard = EssayPointsCard(
    front = front,
    back = back,
    pointId = pointId,
    question = question,
    keyPoints = keyPoints,
)
