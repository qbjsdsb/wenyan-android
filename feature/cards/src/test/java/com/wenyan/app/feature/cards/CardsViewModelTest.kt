package com.wenyan.app.feature.cards

import androidx.lifecycle.SavedStateHandle
import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.cards.ClozeQuoteCard
import com.wenyan.app.core.data.repository.IntervalPreview
import com.wenyan.app.core.data.repository.StudyProgressRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.fsrs.Rating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [CardsViewModel] 单元测试。
 *
 * 覆盖范围：
 * - NF-PP5 Wave 3.2:AGAIN 记录错题行为
 * - v0.8.5 P0：sibling 去重（同 pointId 多卡仅首次触发 FSRS 调度）
 * - v0.8.5 P1：会话统计（sessionReviewedCount / sessionAgainCount）
 * - v0.8.5 P1：撤销 undo()（仅回退 UI 索引，不回滚 FSRS）
 *
 * 用 StandardTestDispatcher + advanceUntilIdle 控制协程执行时序。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var cardRepository: FakeCardRepository
    private lateinit var schedulingRepository: FakeSchedulingRepository
    private lateinit var wrongAnswerRepository: FakeWrongAnswerRepository
    private lateinit var studyProgressRepository: StudyProgressRepository
    private lateinit var viewModel: CardsViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        // 预设 1 张测试卡片(pointId="point_1")
        cardRepository = FakeCardRepository(listOf(testClozeCard()))
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()

        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )

        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 场景 1:rateCard(AGAIN) 后 wrongAnswerRepository.recordWrongAnswer 被调用,
     * source = SOURCE_CARD_AGAIN,pointId 与当前卡片一致,correctAnswer = 卡片背面。
     */
    @Test
    fun `rateCard AGAIN 后记录错题且 source 为 CARD_AGAIN`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val currentCard = viewModel.uiState.value.currentCard
        assertTrue("应有当前卡片", currentCard != null)
        assertEquals("point_1", currentCard!!.pointId)

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        assertEquals("应调用一次 rateCard", 1, schedulingRepository.rateCardCalls.size)
        val (_, rating, _) = schedulingRepository.rateCardCalls[0]
        assertEquals(Rating.AGAIN, rating)

        assertEquals("应记录一次错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
        val record = wrongAnswerRepository.recordedWrongAnswers[0]
        assertEquals("pointId 应为 point_1", "point_1", record.pointId)
        assertEquals("source 应为 CARD_AGAIN", WrongAnswerRepository.SOURCE_CARD_AGAIN, record.source)
        assertEquals("correctAnswer 应为卡片背面", "北宋文学家", record.correctAnswer)
    }

    /**
     * 场景 2:rateCard(GOOD) 后 wrongAnswerRepository.recordWrongAnswer 不被调用。
     */
    @Test
    fun `rateCard GOOD 不记录错题`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.currentCard != null)

        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertEquals(1, schedulingRepository.rateCardCalls.size)
        assertEquals(Rating.GOOD, schedulingRepository.rateCardCalls[0].second)
        assertTrue(
            "GOOD 评分不应记录错题",
            wrongAnswerRepository.recordedWrongAnswers.isEmpty(),
        )
    }

    // ---------- v0.8.5 新增：sibling 去重测试 ----------

    /**
     * 场景 3（P0）：同一 pointId 的多张 sibling 卡，仅首次评分触发 FSRS 调度。
     *
     * 模拟：CardRepository 返回 3 张卡，全部 pointId="point_sibling"。
     * 用户依次评分 GOOD/GOOD/GOOD，调度应只被调用 1 次（第一次），
     * 后续 2 张仅推进 UI + 累加 sessionReviewedCount。
     */
    @Test
    fun `同 pointId 多张卡仅首次评分触发 FSRS 调度`() = runTest(testDispatcher) {
        // 用 3 张同 pointId 的卡重新构造 ViewModel
        val siblingCards = listOf(
            testClozeCard(front = "建安风骨 — 时代", back = "汉末建安年间", pointId = "point_sibling"),
            testClozeCard(front = "建安风骨 — 代表作家", back = "三曹七子", pointId = "point_sibling"),
            testClozeCard(front = "建安风骨 — 风格", back = "慷慨悲凉", pointId = "point_sibling"),
        )
        cardRepository = FakeCardRepository(siblingCards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        // 3 张卡全部 GOOD
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        // 关键断言：FSRS 调度只调用 1 次（sibling 去重生效）
        assertEquals(
            "同 pointId 多卡应仅首次触发调度（sibling 去重）",
            1,
            schedulingRepository.rateCardCalls.size,
        )
        // 但会话统计应累加 3 次（每张卡都算"复习过"）
        assertEquals(
            "sessionReviewedCount 应累加 3 次",
            3,
            viewModel.sessionReviewedCount.value,
        )
        // AGAIN 计数应为 0
        assertEquals(0, viewModel.sessionAgainCount.value)
    }

    /**
     * 场景 4（P0）：不同 pointId 的卡各自独立触发 FSRS 调度。
     */
    @Test
    fun `不同 pointId 各自触发调度`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(front = "卡 A", back = "答案 A", pointId = "point_a"),
            testClozeCard(front = "卡 B", back = "答案 B", pointId = "point_b"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertEquals("不同 pointId 应各自触发调度", 2, schedulingRepository.rateCardCalls.size)
        assertEquals("sessionReviewedCount 应为 2", 2, viewModel.sessionReviewedCount.value)
    }

    /**
     * 场景 5（P1）：AGAIN 评分累加 sessionAgainCount。
     */
    @Test
    fun `AGAIN 评分累加 sessionAgainCount`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(pointId = "p1"),
            testClozeCard(pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertEquals(2, viewModel.sessionReviewedCount.value)
        assertEquals("仅 AGAIN 应累加 sessionAgainCount", 1, viewModel.sessionAgainCount.value)
    }

    // ---------- v0.8.5 新增：undo 测试 ----------

    /**
     * 场景 6（P1）：undo() 回退 currentIndex，但不回滚 FSRS 调度。
     *
     * 用户评分 GOOD 后，currentIndex 从 0 → 1。
     * 调 undo() 后，currentIndex 回到 0，但 FSRS 调度记录仍为 1 次。
     */
    @Test
    fun `undo 回退 currentIndex 但不回滚 FSRS`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(front = "卡 A", back = "答案 A", pointId = "p1"),
            testClozeCard(front = "卡 B", back = "答案 B", pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        assertEquals("初始 currentIndex 应为 0", 0, viewModel.uiState.value.currentIndex)
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        assertEquals("评分后 currentIndex 应为 1", 1, viewModel.uiState.value.currentIndex)
        assertEquals("FSRS 应被调用一次", 1, schedulingRepository.rateCardCalls.size)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals("undo 后 currentIndex 应回到 0", 0, viewModel.uiState.value.currentIndex)
        assertEquals(
            "undo 不应回滚 FSRS 调度",
            1,
            schedulingRepository.rateCardCalls.size,
        )
    }

    /**
     * 场景 7（P1）：currentIndex=0 时 undo 不操作。
     */
    @Test
    fun `currentIndex 为 0 时 undo 不操作`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.currentIndex)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals("currentIndex 仍为 0", 0, viewModel.uiState.value.currentIndex)
    }

    // ---------- v0.8.5 新增：会话完成状态测试 ----------

    /**
     * 场景 8（P0）：评完所有卡后 isFinished=true。
     */
    @Test
    fun `评完所有卡后 isFinished 为 true`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(pointId = "p1"),
            testClozeCard(pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        assertFalse("未评分时 isFinished 应为 false", viewModel.uiState.value.isFinished)

        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertTrue(
            "评完所有卡后 isFinished 应为 true",
            viewModel.uiState.value.isFinished,
        )
    }

    /**
     * 场景 9（P1）：retry() 重置会话状态（统计清零 + currentIndex=0 + ratedPointIds 清空）。
     */
    @Test
    fun `retry 重置会话状态`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(pointId = "p1"),
            testClozeCard(pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()
        assertEquals(1, viewModel.sessionReviewedCount.value)
        assertEquals(1, viewModel.sessionAgainCount.value)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals("retry 后 sessionReviewedCount 应清零", 0, viewModel.sessionReviewedCount.value)
        assertEquals("retry 后 sessionAgainCount 应清零", 0, viewModel.sessionAgainCount.value)
        assertEquals("retry 后 currentIndex 应为 0", 0, viewModel.uiState.value.currentIndex)

        // retry 后 ratedPointIds 已清空，再次评分应触发调度
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        assertEquals(
            "retry 后再次评分应触发调度（ratedPointIds 已清空）",
            2, // 之前 1 次 + 这次 1 次
            schedulingRepository.rateCardCalls.size,
        )
    }

    /**
     * 场景 10（P0）：无 pointId 的卡仅推进 UI，不触发调度。
     */
    @Test
    fun `无 pointId 的卡仅推进 UI 不触发调度`() = runTest(testDispatcher) {
        val cards: List<CardTemplate> = listOf(
            ClozeQuoteCard(
                front = "无 pointId 卡",
                back = "答案",
                pointId = "",
                quote = "____",
                blank = "答案",
                hint = "提示",
            ),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertEquals(
            "无 pointId 卡不应触发 FSRS 调度",
            0,
            schedulingRepository.rateCardCalls.size,
        )
        assertEquals(
            "无 pointId 卡仍应推进 sessionReviewedCount",
            1,
            viewModel.sessionReviewedCount.value,
        )
    }

    // ---------- v0.8.7 新增：预期间隔预览测试 ----------

    /**
     * 场景 11（P0）：进入新卡片时异步调用 previewIntervals 加载预期间隔。
     *
     * 验证 ViewModel 的 currentPreviews StateFlow 被正确填充，
     * UI 据此在评分按钮上显示"1分钟 / 6天 / 12天"。
     */
    @Test
    fun `进入新卡片时异步加载预期间隔预览`() = runTest(testDispatcher) {
        advanceUntilIdle()
        // ViewModel init 后应已为当前卡片调用 previewIntervals
        assertTrue(
            "应调用 previewIntervals 加载预览",
            schedulingRepository.previewCalls.isNotEmpty(),
        )
        val (pointId, cardType) = schedulingRepository.previewCalls.last()
        assertEquals("pointId 应为当前卡片", "point_1", pointId)
        assertEquals(
            "cardType 应为 CLOZE_QUOTE",
            CardTemplateType.CLOZE_QUOTE,
            cardType,
        )
        // currentPreviews 应被填充（4 档）
        assertEquals(
            "currentPreviews 应有 4 档预览",
            4,
            viewModel.currentPreviews.value.size,
        )
        // 验证 AGAIN 档预览内容
        val againPreview = viewModel.currentPreviews.value[Rating.AGAIN]
        assertNotNull("AGAIN 预览不应为 null", againPreview)
        assertEquals("AGAIN 预期间隔应为 1 分钟", "1分钟", againPreview!!.displayText)
    }

    /**
     * 场景 12（P0）：previewIntervals 加载失败时 currentPreviews 为空，UI 降级。
     *
     * 模拟 FakeSchedulingRepository.previewIntervals 抛异常，
     * 验证 currentPreviews 为空 Map（不阻塞主流程）。
     */
    @Test
    fun `预览加载失败时 currentPreviews 为空不阻塞主流程`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(listOf(testClozeCard()))
        schedulingRepository = FakeSchedulingRepository().apply {
            // 让 previewIntervals 抛异常
            previewResults = emptyMap()
        }
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        // 即使预览为空，rateCard 仍应正常工作
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        assertEquals("预览失败不应阻塞评分", 1, schedulingRepository.rateCardCalls.size)
    }

    // ---------- v0.8.7 新增：Leech 检测测试 ----------

    /**
     * 场景 13（P0）：rateCard 后 failCount 达阈值(8)时弹出 leechWarning。
     *
     * 模拟 FakeSchedulingRepository 返回 failCount=8 的 MemoRecordEntity，
     * 验证 viewModel.leechWarning 被设置为非 null 警告文案。
     */
    @Test
    fun `Leech 检测 failCount 达阈值时弹出 leechWarning`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(listOf(testClozeCard()))
        schedulingRepository = FakeSchedulingRepository(
            rateCardResult = MemoRecordEntity(
                pointId = "point_1",
                state = "REVIEW",
                stability = 1f,
                difficulty = 9f,
                lastReviewAt = 1000L,
                nextReviewAt = 2000L,
                reviewCount = 8,
                failCount = 8, // 达到 LEECH_THRESHOLD=8
                elapsedDays = 0,
                scheduledDays = 0,
                reps = 8,
                inPriorityQueue = 0,
            ),
        )
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        assertNull("评分前 leechWarning 应为 null", viewModel.leechWarning.value)

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        val warning = viewModel.leechWarning.value
        assertNotNull("failCount=8 时应弹出 leechWarning", warning)
        // v0.8.8: leechWarning 类型从 String? 改为 LeechWarning?,
        // 警告文案在 warning.message 字段,pointId 在 warning.pointId 字段
        assertTrue(
            "警告文案应包含复习次数",
            warning!!.message.contains("8"),
        )
        assertTrue(
            "警告应建议拆分卡片",
            warning.message.contains("拆分"),
        )
        assertEquals(
            "LeechWarning 应携带 pointId 供 UI 跳转知识点详情",
            "point_1",
            warning.pointId,
        )
    }

    /**
     * 场景 14：failCount 未达阈值时不弹 leechWarning。
     */
    @Test
    fun `failCount 未达阈值时不弹 leechWarning`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(listOf(testClozeCard()))
        schedulingRepository = FakeSchedulingRepository(
            rateCardResult = MemoRecordEntity(
                pointId = "point_1",
                state = "REVIEW",
                stability = 10f,
                difficulty = 5f,
                lastReviewAt = 1000L,
                nextReviewAt = 2000L,
                reviewCount = 3,
                failCount = 3, // 未达 LEECH_THRESHOLD=8
                elapsedDays = 0,
                scheduledDays = 1,
                reps = 3,
                inPriorityQueue = 0,
            ),
        )
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        assertNull(
            "failCount=3 未达阈值，不应弹 leechWarning",
            viewModel.leechWarning.value,
        )
    }

    /**
     * 场景 15：clearLeechWarning 清除警告。
     */
    @Test
    fun `clearLeechWarning 清除警告`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(listOf(testClozeCard()))
        schedulingRepository = FakeSchedulingRepository(
            rateCardResult = MemoRecordEntity(
                pointId = "point_1",
                state = "REVIEW",
                stability = 1f,
                difficulty = 9f,
                lastReviewAt = 1000L,
                nextReviewAt = 2000L,
                reviewCount = 8,
                failCount = 8,
                elapsedDays = 0,
                scheduledDays = 0,
                reps = 8,
                inPriorityQueue = 0,
            ),
        )
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()
        assertNotNull("应弹出 leechWarning", viewModel.leechWarning.value)

        viewModel.clearLeechWarning()
        assertNull("clearLeechWarning 后应为 null", viewModel.leechWarning.value)
    }

    // ---------- v0.8.7 新增：undo 回退 sessionAgainCount 测试 ----------

    /**
     * 场景 16（P1）：undo 撤销 AGAIN 卡时回退 sessionAgainCount。
     *
     * 用户评 AGAIN 后 sessionAgainCount=1，undo 后应回退到 0，
     * 保证完成态"掌握率"统计准确。
     */
    @Test
    fun `undo 撤销 AGAIN 卡时回退 sessionAgainCount`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(front = "卡 A", back = "答案 A", pointId = "p1"),
            testClozeCard(front = "卡 B", back = "答案 B", pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()
        assertEquals("AGAIN 后 sessionAgainCount 应为 1", 1, viewModel.sessionAgainCount.value)
        assertEquals("sessionReviewedCount 应为 1", 1, viewModel.sessionReviewedCount.value)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(
            "undo 后 sessionAgainCount 应回退到 0",
            0,
            viewModel.sessionAgainCount.value,
        )
        assertEquals(
            "undo 后 sessionReviewedCount 应回退到 0",
            0,
            viewModel.sessionReviewedCount.value,
        )
    }

    /**
     * 场景 17（P1）：undo 撤销 GOOD 卡时不影响 sessionAgainCount。
     */
    @Test
    fun `undo 撤销 GOOD 卡时不影响 sessionAgainCount`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(front = "卡 A", back = "答案 A", pointId = "p1"),
            testClozeCard(front = "卡 B", back = "答案 B", pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        // 先评 AGAIN，再评 GOOD，然后撤销 GOOD
        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        assertEquals("AGAIN+GOOD 后 sessionAgainCount 应为 1", 1, viewModel.sessionAgainCount.value)
        assertEquals("sessionReviewedCount 应为 2", 2, viewModel.sessionReviewedCount.value)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(
            "撤销 GOOD 不应影响 sessionAgainCount",
            1,
            viewModel.sessionAgainCount.value,
        )
        assertEquals(
            "sessionReviewedCount 应回退到 1",
            1,
            viewModel.sessionReviewedCount.value,
        )
    }

    // ---------- v0.8.10 新增：skip + 多步 undo + ratedPointIds 回退测试 ----------

    /**
     * 场景 18（P1）：skipCard 推进索引但不影响统计。
     *
     * skip 不评分推进到下一张,sessionReviewedCount/sessionAgainCount 不变。
     */
    @Test
    fun `skipCard 推进索引但不影响统计`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(front = "卡 A", back = "答案 A", pointId = "p1"),
            testClozeCard(front = "卡 B", back = "答案 B", pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        assertEquals("初始 currentIndex=0", 0, viewModel.uiState.value.currentIndex)
        assertEquals("初始 sessionReviewedCount=0", 0, viewModel.sessionReviewedCount.value)

        viewModel.skipCard()
        advanceUntilIdle()

        assertEquals("skip 后 currentIndex=1", 1, viewModel.uiState.value.currentIndex)
        assertEquals(
            "skip 不影响 sessionReviewedCount",
            0,
            viewModel.sessionReviewedCount.value,
        )
        assertEquals(
            "skip 不触发 FSRS 调度",
            0,
            schedulingRepository.rateCardCalls.size,
        )
    }

    /**
     * 场景 19（P1）：skip 后 undo 回退到被跳过的卡。
     *
     * skip 入栈 rating=null,undo 时仅回退索引,不回退统计。
     */
    @Test
    fun `skip 后 undo 回退到被跳过的卡`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(front = "卡 A", back = "答案 A", pointId = "p1"),
            testClozeCard(front = "卡 B", back = "答案 B", pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        viewModel.skipCard()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentIndex)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals("undo 后 currentIndex 回到 0", 0, viewModel.uiState.value.currentIndex)
        assertEquals(
            "undo skip 不影响 sessionReviewedCount",
            0,
            viewModel.sessionReviewedCount.value,
        )
    }

    /**
     * 场景 20（P0）：多步 undo 精确回退（AGAIN→GOOD→undo→undo）。
     *
     * 验证 v0.8.8 ratingHistory 栈的精确回退:
     * - 评 AGAIN:sessionReviewedCount=1, sessionAgainCount=1
     * - 评 GOOD:sessionReviewedCount=2, sessionAgainCount=1
     * - undo GOOD:sessionReviewedCount=1, sessionAgainCount=1
     * - undo AGAIN:sessionReviewedCount=0, sessionAgainCount=0
     *
     * 原实现 lastRatingWasAgain:Boolean 在第二次 undo 丢失 AGAIN 回退。
     */
    @Test
    fun `多步 undo 精确回退 AGAIN GOOD undo undo`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(front = "卡 A", back = "答案 A", pointId = "p1"),
            testClozeCard(front = "卡 B", back = "答案 B", pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        // AGAIN → GOOD
        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertEquals("AGAIN+GOOD 后 reviewedCount=2", 2, viewModel.sessionReviewedCount.value)
        assertEquals("AGAIN+GOOD 后 againCount=1", 1, viewModel.sessionAgainCount.value)
        // 注意:_currentIndex(SavedStateHandle)=2,但 UI state 的 currentIndex 被 coerceIn
        // 到 safeIndex=1(cards.size=2,safeIndex∈[0,1])。此时 isFinished=true。
        assertTrue("两卡都评完后应 isFinished=true", viewModel.uiState.value.isFinished)

        // undo GOOD
        viewModel.undo()
        advanceUntilIdle()
        assertEquals("undo GOOD 后 reviewedCount=1", 1, viewModel.sessionReviewedCount.value)
        assertEquals("undo GOOD 后 againCount 仍为 1", 1, viewModel.sessionAgainCount.value)
        assertEquals("undo GOOD 后 currentIndex=1", 1, viewModel.uiState.value.currentIndex)

        // undo AGAIN
        viewModel.undo()
        advanceUntilIdle()
        assertEquals(
            "undo AGAIN 后 reviewedCount=0",
            0,
            viewModel.sessionReviewedCount.value,
        )
        assertEquals(
            "undo AGAIN 后 againCount=0",
            0,
            viewModel.sessionAgainCount.value,
        )
        assertEquals("undo AGAIN 后 currentIndex=0", 0, viewModel.uiState.value.currentIndex)
    }

    /**
     * 场景 21（P0）：undo 后 ratedPointIds 不回退，重新评分不重复触发 FSRS。
     *
     * v0.8.12 P0-1 修复:原实现 undo 回退 ratedPointIds 导致重新评分重复调度 FSRS,
     * stability 异常增长。现 undo 不回退 ratedPointIds,重新评分时 shouldSchedule=false。
     *
     * 用户评 GOOD(触发调度) → undo(UI 回退,FSRS 不回滚) →
     * 重新评 GOOD(shouldSchedule=false,不重复调度)。
     */
    @Test
    fun `undo 后重新评分不重复触发 FSRS`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(front = "卡 A", back = "答案 A", pointId = "p1"),
            testClozeCard(front = "卡 B", back = "答案 B", pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        // 评 GOOD 触发调度
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        assertEquals("首次评分应触发调度", 1, schedulingRepository.rateCardCalls.size)

        // undo 仅回退 UI,不回退 ratedPointIds
        viewModel.undo()
        advanceUntilIdle()
        assertEquals("undo 后 currentIndex 回到 0", 0, viewModel.uiState.value.currentIndex)

        // 重新评 GOOD 不应再次触发调度(ratedPointIds 未回退)
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        assertEquals(
            "undo 后重新评分不应重复触发 FSRS(避免 stability 异常增长)",
            1,
            schedulingRepository.rateCardCalls.size,
        )
    }

    /**
     * 场景 22（P2）：无 pointId 卡评 AGAIN 记录错题。
     *
     * v0.8.10 P2-C3 修复:原实现无 pointId 卡评分时直接 return,跳过错题记录。
     * 现改为 AGAIN 时仍记录错题(pointId=null)。
     */
    @Test
    fun `无 pointId 卡评 AGAIN 记录错题`() = runTest(testDispatcher) {
        val cards: List<CardTemplate> = listOf(
            ClozeQuoteCard(
                front = "无 pointId 卡",
                back = "答案",
                pointId = "",
                quote = "____",
                blank = "答案",
                hint = "提示",
            ),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        assertEquals(
            "无 pointId 卡不应触发 FSRS 调度",
            0,
            schedulingRepository.rateCardCalls.size,
        )
        assertEquals(
            "无 pointId 卡评 AGAIN 应记录错题",
            1,
            wrongAnswerRepository.recordedWrongAnswers.size,
        )
        val record = wrongAnswerRepository.recordedWrongAnswers[0]
        assertNull("无 pointId 卡的错题记录 pointId 应为 null", record.pointId)
        assertEquals(
            "source 应为 CARD_AGAIN",
            WrongAnswerRepository.SOURCE_CARD_AGAIN,
            record.source,
        )
    }

    /**
     * 场景 23（P1）：无 pointId 卡评 GOOD 不记录错题。
     *
     * 只有 AGAIN 记录错题,GOOD/HARD/EASY 不记录。
     */
    @Test
    fun `无 pointId 卡评 GOOD 不记录错题`() = runTest(testDispatcher) {
        val cards: List<CardTemplate> = listOf(
            ClozeQuoteCard(
                front = "无 pointId 卡",
                back = "答案",
                pointId = "",
                quote = "____",
                blank = "答案",
                hint = "提示",
            ),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertTrue(
            "无 pointId 卡评 GOOD 不应记录错题",
            wrongAnswerRepository.recordedWrongAnswers.isEmpty(),
        )
    }
}

/** 测试用 StudyProgressRepository(P0 v0.7.2 新增,直接实例化 + Fake DAO) */
private fun FakeStudyProgressRepository() = StudyProgressRepository(FakeStudyProgressDao())

/** 测试用空 DAO,不实际读写数据库 */
private class FakeStudyProgressDao : com.wenyan.app.core.database.dao.StudyProgressDao {
    override suspend fun upsert(entity: com.wenyan.app.core.database.entity.StudyProgressEntity) {}
    override suspend fun update(entity: com.wenyan.app.core.database.entity.StudyProgressEntity) {}
    override suspend fun deleteById(id: String) {}
    override suspend fun getById(id: String): com.wenyan.app.core.database.entity.StudyProgressEntity? = null
    override fun observeById(id: String): kotlinx.coroutines.flow.Flow<com.wenyan.app.core.database.entity.StudyProgressEntity?> =
        kotlinx.coroutines.flow.flowOf(null)
}
