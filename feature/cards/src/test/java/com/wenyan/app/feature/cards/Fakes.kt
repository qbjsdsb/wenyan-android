package com.wenyan.app.feature.cards

import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.cards.ClozeQuoteCard
import com.wenyan.app.core.data.repository.CardRepository
import com.wenyan.app.core.data.repository.IntervalPreview
import com.wenyan.app.core.data.repository.SchedulingRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.fsrs.Rating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * [CardRepository] 的 Fake 实现,供 [CardsViewModelTest] 使用(NF-PP5 Wave 3.2)。
 *
 * 通过 [cards] 可控地注入测试卡片流。
 */
class FakeCardRepository(
    initialCards: List<CardTemplate> = emptyList(),
) : CardRepository {

    private val _cards = MutableStateFlow(initialCards)

    override fun getCardsForReview(): Flow<List<CardTemplate>> = _cards.asStateFlow()
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
    initialAll: List<WrongAnswerEntity> = emptyList(),
    initialUnresolved: List<WrongAnswerEntity> = emptyList(),
    var unresolvedCount: Int = 0,
) : WrongAnswerRepository {

    private val _all = MutableStateFlow(initialAll)
    private val _unresolved = MutableStateFlow(initialUnresolved)

    val recordedWrongAnswers: MutableList<RecordedWrongAnswer> = mutableListOf()
    val resolvedIds: MutableList<String> = mutableListOf()
    val deletedIds: MutableList<String> = mutableListOf()

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
