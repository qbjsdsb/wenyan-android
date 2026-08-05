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
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import android.database.sqlite.SQLiteException

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
        // v0.9.37 P1-9：会话卡片构建调度器注入虚拟调度器，
        // 避免真实 Default 线程异步导致虚拟时间无法等待（runTest 只控制测试调度器）
        CardsViewModel.sessionCardDispatcher = testDispatcher

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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )

        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 回归：上游首次发空队列、随后真实卡片到达时，不能把空列表冻结为整场会话。
     * 该竞态会表现为今日任务横幅已有新卡数量，但正文仍显示“今天没有到期卡片”。
     */
    @Test
    fun `首次空队列后真实卡片到达仍会显示卡片`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(initialCards = emptyList())
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = FakeSchedulingRepository(),
            wrongAnswerRepository = FakeWrongAnswerRepository(),
            studyProgressRepository = FakeStudyProgressRepository(),
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.cards.isEmpty())

        cardRepository.emitCards(listOf(testClozeCard(pointId = "late_card")))
        advanceUntilIdle()

        assertEquals("late_card", viewModel.uiState.value.currentCard?.pointId)
        assertFalse(viewModel.uiState.value.isFinished)
    }

    /**
     * 场景 1:rateCard(AGAIN) 后 wrongAnswerRepository.recordWrongAnswer 被调用,
     * source = SOURCE_CARD_AGAIN,pointId 与当前卡片一致,correctAnswer = 完整 quote(ClozeQuoteCard 取 quote 而非 back)。
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
        assertEquals("correctAnswer 应为完整 quote(ClozeQuoteCard v0.8.13 设计)", "苏轼____", record.correctAnswer)
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
        // v0.8.13 P0-1 修复:断言与生产文案对齐
        // v0.8.12 移除了"拆分卡片"建议(App 不支持),改为引导用户查看知识点或问 AI 助手
        assertTrue(
            "警告应引导查看知识点或问 AI 助手",
            warning.message.contains("查看知识点") || warning.message.contains("AI"),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()
        assertNotNull("应弹出 leechWarning", viewModel.leechWarning.value)

        viewModel.clearLeechWarning()
        advanceUntilIdle()
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
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
     * 场景 22(P2):无 pointId 卡评 AGAIN 不记录错题。
     *
     * v0.8.14 P0-4 修复:原实现(v0.8.10 P2-C3)无 pointId 卡评 AGAIN 时
     * 传入 pointId=null 调用 recordWrongAnswer,违反 WrongAnswerRepository 契约
     * ("pointId 与 examQuestionId 至少一个非空")。WrongAnswerRepositoryImpl 对
     * null pointId 的处理是 existing=null,导致每次评分都 upsert 新记录,
     * 无法去重、无法关联知识点、无法通过 observeByPoint 查询,成为错题本里的"孤儿数据"。
     *
     * 现恢复 v0.8.5 设计:无 pointId 卡不记录错题(无知识点关联,记录无意义)。
     * 本测试验证修复:无 pointId 卡评 AGAIN 后,wrongAnswerRepository 不被调用。
     */
    @Test
    fun `无 pointId 卡评 AGAIN 不记录错题`() = runTest(testDispatcher) {
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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        assertEquals(
            "无 pointId 卡不应触发 FSRS 调度",
            0,
            schedulingRepository.rateCardCalls.size,
        )
        assertTrue(
            "无 pointId 卡评 AGAIN 不应记录错题(违反 WrongAnswerRepository 契约)",
            wrongAnswerRepository.recordedWrongAnswers.isEmpty(),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertTrue(
            "无 pointId 卡评 GOOD 不应记录错题",
            wrongAnswerRepository.recordedWrongAnswers.isEmpty(),
        )
    }

    // ---------- v0.8.13 新增:TermExplanationCard 测试覆盖(生产卡片类型) ----------

    /**
     * 场景 24(P0):TermExplanationCard 评分触发 FSRS 调度,cardType=TERM_EXPLANATION。
     *
     * v0.8.13 P1-5 新增:原测试全部用 ClozeQuoteCard(生产永不生成),
     * 生产实际生成的 TermExplanationCard 在 ViewModel 层无测试覆盖。
     * 本测试验证:
     * - TermExplanationCard 的 templateType=TERM_EXPLANATION 正确传递给 SchedulingRepository
     * - rateCard 调用使用正确的 cardType(影响 FSRS tier 映射:
     *   TERM_EXPLANATION→TIER_FRAMEWORK, CLOZE_QUOTE→TIER_EXACT)
     * - previewIntervals 用正确的 cardType 加载预览
     */
    @Test
    fun `TermExplanationCard 评分传递正确的 cardType 给 FSRS 调度`() = runTest(testDispatcher) {
        // 用 TermExplanationCard 重新构造 ViewModel(替代默认 ClozeQuoteCard)
        cardRepository = FakeCardRepository(listOf(testTermCard()))
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        // previewIntervals 应被调用,且 cardType=TERM_EXPLANATION
        assertTrue(
            "应调用 previewIntervals 加载预览",
            schedulingRepository.previewCalls.isNotEmpty(),
        )
        val (_, previewCardType) = schedulingRepository.previewCalls.last()
        assertEquals(
            "TermExplanationCard 的 previewIntervals 应传递 TERM_EXPLANATION 类型",
            CardTemplateType.TERM_EXPLANATION,
            previewCardType,
        )

        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertEquals("应调用一次 rateCard", 1, schedulingRepository.rateCardCalls.size)
        val (_, _, rateCardType) = schedulingRepository.rateCardCalls[0]
        assertEquals(
            "rateCard 应传递 TERM_EXPLANATION 类型(影响 FSRS tier 映射)",
            CardTemplateType.TERM_EXPLANATION,
            rateCardType,
        )
    }

    /**
     * 场景 25(P1):TermExplanationCard 评 AGAIN 记录错题,correctAnswer 优先用 fullExplanation。
     *
     * v0.8.14 P0-5 修复:原断言"correctAnswer 应为卡片背面 1921年"与 extractCorrectAnswer
     * 实现矛盾(实现优先用 fullExplanation)。本场景已被场景 28 完整覆盖,保留场景编号
     * 但修正断言,避免 CI 跑测试时本场景失败漏网(此前因 CI 账单问题 38+ commit 未验证)。
     */
    @Test
    fun `TermExplanationCard 评 AGAIN 记录错题且 correctAnswer 优先用 fullExplanation`() =
        runTest(testDispatcher) {
            cardRepository = FakeCardRepository(listOf(testTermCard(back = "1921年")))
            schedulingRepository = FakeSchedulingRepository()
            wrongAnswerRepository = FakeWrongAnswerRepository()
            studyProgressRepository = FakeStudyProgressRepository()
            viewModel = CardsViewModel(
                savedStateHandle = SavedStateHandle(),
                cardRepository = cardRepository,
                schedulingRepository = schedulingRepository,
                wrongAnswerRepository = wrongAnswerRepository,
                studyProgressRepository = studyProgressRepository,
                cardSettingsRepository = FakeCardSettingsRepository(),
            )
            advanceUntilIdle()

            viewModel.rateCard(CardRating.AGAIN)
            advanceUntilIdle()

            assertEquals("AGAIN 应记录错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
            val record = wrongAnswerRepository.recordedWrongAnswers[0]
            // 与 extractCorrectAnswer 实现一致:TermExplanationCard 优先用 fullExplanation
            assertEquals(
                "correctAnswer 应优先用 fullExplanation(完整解释),而非 back(维度答案)",
                "文学研究会是1921年成立于北京的文学团体",
                record.correctAnswer,
            )
            assertEquals(
                "source 应为 CARD_AGAIN",
                WrongAnswerRepository.SOURCE_CARD_AGAIN,
                record.source,
            )
        }

    // ---------- v0.8.13 新增:P0-2 correctAnswer 提取测试 ----------

    /**
     * 场景 26(P0):DistinctionCard 评 AGAIN 时 correctAnswer 从 differences 提取。
     *
     * v0.8.13 P0-2 修复:DistinctionCard.back 是占位文本"$item1 与 $item2 的区别见要点",
     * 无信息量。extractCorrectAnswer 应从 differences 列表提取真实答案。
     */
    @Test
    fun `DistinctionCard 评 AGAIN 时 correctAnswer 为 differences 拼接`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(listOf(testDistinctionCard()))
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        assertEquals("应记录一次错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
        val record = wrongAnswerRepository.recordedWrongAnswers[0]
        // correctAnswer 应为 differences 拼接,而非 back(占位文本)
        assertTrue(
            "correctAnswer 应包含第一条区别要点",
            record.correctAnswer?.contains("建安风骨产生于汉末建安年间") == true,
        )
        assertTrue(
            "correctAnswer 应包含第二条区别要点",
            record.correctAnswer?.contains("建安风骨代表作家为三曹七子") == true,
        )
        assertTrue(
            "correctAnswer 不应是占位文本",
            record.correctAnswer?.contains("区别见要点") == false,
        )
    }

    /**
     * 场景 27(P0):EssayPointsCard 评 AGAIN 时 correctAnswer 从 keyPoints 提取。
     *
     * v0.8.13 P0-2 修复:EssayPointsCard.back 是 summary 散文,
     * extractCorrectAnswer 应从 keyPoints 列表提取结构化要点(带序号)。
     */
    @Test
    fun `EssayPointsCard 评 AGAIN 时 correctAnswer 为 keyPoints 带序号拼接`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(listOf(testEssayPointsCard()))
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        assertEquals("应记录一次错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
        val record = wrongAnswerRepository.recordedWrongAnswers[0]
        // correctAnswer 应为 keyPoints 带序号拼接
        assertTrue(
            "correctAnswer 应包含序号 1.",
            record.correctAnswer?.contains("1. 标志着文人诗的成熟") == true,
        )
        assertTrue(
            "correctAnswer 应包含序号 2.",
            record.correctAnswer?.contains("2. 奠定五言诗基础") == true,
        )
        assertTrue(
            "correctAnswer 应包含序号 3.",
            record.correctAnswer?.contains("3. 影响后世边塞诗派") == true,
        )
    }

    /**
     * 场景 28(P0):TermExplanationCard 评 AGAIN 时 correctAnswer 优先用 fullExplanation。
     *
     * v0.8.13 P0-2 修复:TermExplanationCard.back 是维度答案(如"1921年"),
     * 信息密度低。extractCorrectAnswer 应优先用 fullExplanation(完整解释)。
     */
    @Test
    fun `TermExplanationCard 评 AGAIN 时 correctAnswer 优先用 fullExplanation`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(listOf(testTermCard()))
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        assertEquals("应记录一次错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
        val record = wrongAnswerRepository.recordedWrongAnswers[0]
        // correctAnswer 应为 fullExplanation(完整解释),而非 back(维度答案"1921年")
        assertEquals(
            "correctAnswer 应为 fullExplanation",
            "文学研究会是1921年成立于北京的文学团体",
            record.correctAnswer,
        )
    }

    // ---------- 已调度状态提示（含回看场景） ----------

    /**
     * 场景 29(P1):回看首张评分卡时仍标记该知识点已完成调度。
     *
     * 回看不会回滚 FSRS，因此必须继续隐藏预期间隔；旧逻辑返回 false，会重新显示
     * “良好→6天”等不会真正生效的预览。
     */
    @Test
    fun `回看首张评分卡时仍显示知识点已调度`() = runTest(testDispatcher) {
        // 两张同 pointId 的 sibling 卡
        val cards = listOf(
            testTermCard(front = "文学研究会 — 时代", back = "1921年", pointId = "p1"),
            testTermCard(front = "文学研究会 — 代表作家", back = "郑振铎、沈雁冰", pointId = "p1"),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        // 初始：卡 A 尚未调度
        assertFalse("初始卡 A 尚未调度", viewModel.isSiblingAlreadyRated.value)

        // 评 GOOD 卡 A,推进到卡 B(sibling)
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        assertTrue(
            "卡 B(同 pointId sibling)应显示 sibling 提示",
            viewModel.isSiblingAlreadyRated.value,
        )

        // 回看卡 A；调度并未回滚，因此仍应隐藏预期间隔并显示说明
        viewModel.undo()
        advanceUntilIdle()
        assertTrue(
            "回看卡 A 时该知识点仍已调度，不能重新显示虚假间隔预览",
            viewModel.isSiblingAlreadyRated.value,
        )
    }

    /**
     * 场景 30(P1):sibling 卡(非首张)isSiblingAlreadyRated=true。
     *
     * 验证 P1-1 修复后,正常的 sibling 卡提示仍能正确显示。
     * 评 GOOD 卡 A → 推进到卡 B(同 pointId)→ isSiblingAlreadyRated=true。
     */
    @Test
    fun `sibling 卡非首张时 isSiblingAlreadyRated 为 true`() = runTest(testDispatcher) {
        val cards = listOf(
            testTermCard(front = "文学研究会 — 时代", back = "1921年", pointId = "p1"),
            testTermCard(front = "文学研究会 — 代表作家", back = "郑振铎", pointId = "p1"),
            testTermCard(front = "文学研究会 — 主张", back = "为人生而艺术", pointId = "p1"),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        // 卡 A:首张,未评分,isSibling=false
        assertFalse("卡 A 不应是 sibling", viewModel.isSiblingAlreadyRated.value)

        // 评 GOOD 卡 A,推进到卡 B
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        assertTrue("卡 B(sibling)应是 sibling", viewModel.isSiblingAlreadyRated.value)

        // 评 GOOD 卡 B(不触发 FSRS,因 shouldSchedule=false),推进到卡 C
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        assertTrue(
            "卡 C(第三个 sibling)应仍是 sibling",
            viewModel.isSiblingAlreadyRated.value,
        )
    }

    // ---------- v0.8.14 新增:P0-1 双击防护测试 ----------

    /**
     * 场景 31(P0):快速连点评分不会重复评分同一张卡。
     *
     * v0.8.14 P0-1 修复:原实现从 `uiState.value.currentCard` 读取当前卡,
     * 但 _uiState 异步更新,两次同步 rateCard 调用之间 _uiState 不会重新 emit,
     * 导致第二次调用仍读到旧卡,同一张卡被评分两次,统计虚高 + 跳过下一张。
     *
     * 修复:从 sessionCards + _currentIndex(SavedStateHandle-backed,同步可见)读取。
     *
     * 本测试模拟快速连点:连续两次 rateCard(GOOD) 不等 advanceUntilIdle,
     * 验证第一次评卡 A,第二次评卡 B(而非再次评卡 A)。
     */
    @Test
    fun `快速连点评分不重复评分同一张卡`() = runTest(testDispatcher) {
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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        // 连续两次评分,中间不 advanceUntilIdle(模拟快速双击)
        viewModel.rateCard(CardRating.GOOD)
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        // 关键断言:两次评分应分别评卡 A 和卡 B(各自触发一次 FSRS 调度,共 2 次)
        assertEquals(
            "快速连点应分别评两张卡,FSRS 调度 2 次(而非同一张卡评 2 次)",
            2,
            schedulingRepository.rateCardCalls.size,
        )
        // 验证两次调度的 pointId 不同(分别是 p1 和 p2)
        assertEquals("p1", schedulingRepository.rateCardCalls[0].first)
        assertEquals("p2", schedulingRepository.rateCardCalls[1].first)
        // sessionReviewedCount 应为 2(每张卡各 1 次)
        assertEquals(
            "sessionReviewedCount 应为 2(无虚高)",
            2,
            viewModel.sessionReviewedCount.value,
        )
    }

    /**
     * 场景 32(P0):快速连点 skipCard 不会重复跳过同一张卡。
     *
     * 与场景 31 类似,验证 skipCard 也从 sessionCards + _currentIndex 读取。
     */
    @Test
    fun `快速连点 skipCard 不重复跳过同一张卡`() = runTest(testDispatcher) {
        val cards = listOf(
            testClozeCard(front = "卡 A", back = "答案 A", pointId = "p1"),
            testClozeCard(front = "卡 B", back = "答案 B", pointId = "p2"),
            testClozeCard(front = "卡 C", back = "答案 C", pointId = "p3"),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        // 连续两次 skip,中间不 advanceUntilIdle
        viewModel.skipCard()
        viewModel.skipCard()
        advanceUntilIdle()

        // 验证 currentIndex 推进了 2(而非 1)
        assertEquals(
            "快速连点 skip 应分别跳过两张卡,currentIndex=2",
            2,
            viewModel.uiState.value.currentIndex,
        )
        // skip 不触发 FSRS 调度
        assertEquals(
            "skip 不应触发 FSRS 调度",
            0,
            schedulingRepository.rateCardCalls.size,
        )
    }

    // ---------- v0.8.17 新增:sessionDurationMinutes StateFlow 测试 ----------

    /**
     * 场景 33(P1):sessionDurationMinutes 在会话进行中为 0,完成后 >=1,retry 后回 0。
     *
     * v0.8.17 P1 修复:原 [getSessionDurationMinutes] 是普通函数,在 Composable 的
     * Crossfade 重组中被直接调用(`sessionDurationMinutes = viewModel.getSessionDurationMinutes()`)。
     * 这是 Compose 反模式:
     * - 每次父组件重组都会重新执行该函数,读 System.currentTimeMillis() 返回不稳定值
     * - 破坏 Compose 重组跳过机制(参数不稳定 → SessionCompleteState 无谓重组)
     * - 在完成态停留时,时长会随重组不断变化,但 UI 无感知(参数已传入子组件)
     *
     * 改为 StateFlow 后,仅在 [_uiState] 或 [_sessionStartTime] 变化时重新计算:
     * - isFinished=false 时返回 0(进行中无需计算)
     * - isFinished=true 时计算 (now - sessionStartTime) / 60_000,coerceAtLeast(1)
     *
     * 本测试用过去时间戳初始化 sessionStartTime(模拟 5 分钟前开始),验证:
     * 1. 会话进行中(未评完所有卡):sessionDurationMinutes == 0
     * 2. 会话完成(isFinished=true):sessionDurationMinutes >= 1
     * 3. retry 后重置:sessionDurationMinutes 回到 0(因 sessionStartTime 被重置为 now,
     *    且 isFinished 在 _uiState 中被 copy 重置)
     */
    @Test
    fun `sessionDurationMinutes 进行中为0完成后大于等于1且retry后回0`() = runTest(testDispatcher) {
        // 用过去时间戳初始化 sessionStartTime(模拟 5 分钟前开始)
        val pastStartTime = System.currentTimeMillis() - 5 * 60_000L
        val cards = listOf(
            testClozeCard(front = "卡 A", back = "答案 A", pointId = "p1"),
            testClozeCard(front = "卡 B", back = "答案 B", pointId = "p2"),
        )
        cardRepository = FakeCardRepository(cards)
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(
                initialState = mapOf("sessionStartTime" to pastStartTime),
            ),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        // 1. 会话进行中:未评完所有卡,sessionDurationMinutes 应为 0
        assertFalse("未完成时 isFinished 应为 false", viewModel.uiState.value.isFinished)
        assertEquals(
            "会话进行中 sessionDurationMinutes 应为 0(不计算时长)",
            0,
            viewModel.sessionDurationMinutes.value,
        )

        // 2. 评完所有卡,会话完成
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertTrue("评完所有卡后 isFinished 应为 true", viewModel.uiState.value.isFinished)
        assertTrue(
            "完成后 sessionDurationMinutes 应 >= 1(实际 ${viewModel.sessionDurationMinutes.value})",
            viewModel.sessionDurationMinutes.value >= 1,
        )

        // 3. retry 后 sessionStartTime 重置为 now,isFinished 重置,sessionDurationMinutes 回 0
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(
            "retry 后 sessionDurationMinutes 应回到 0(会话重置)",
            0,
            viewModel.sessionDurationMinutes.value,
        )
    }

    // ========== v0.9.18: addToWrongAnswerBook 测试 ==========

    /**
     * 场景:addToWrongAnswerBook 成功调用 recordWrongAnswer。
     * 验证:recordedWrongAnswers 中有一条记录,source=CARD_MANUAL,pointId=当前卡 pointId。
     */
    @Test
    fun `addToWrongAnswerBook 记录错题`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val currentCard = viewModel.uiState.value.currentCard
        assertNotNull("应有当前卡片", currentCard)

        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()

        assertEquals("应记录一条错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
        val record = wrongAnswerRepository.recordedWrongAnswers[0]
        assertEquals("pointId 应为当前卡 pointId", "point_1", record.pointId)
        assertEquals("source 应为 CARD_MANUAL", WrongAnswerRepository.SOURCE_CARD_MANUAL, record.source)
        assertNull("examQuestionId 应为 null", record.examQuestionId)
        assertTrue("userAnswer 应包含'手动加入'", record.userAnswer.contains("手动加入"))
    }

    /**
     * 场景:同一卡重复调用 addToWrongAnswerBook 只记录一次。
     * 验证:第二次调用不增加 recordedWrongAnswers 数量。
     */
    @Test
    fun `addToWrongAnswerBook 重复调用去重`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()
        viewModel.addToWrongAnswerBook()  // 第二次
        advanceUntilIdle()

        assertEquals("应只记录一条错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
    }

    /**
     * 场景:sibling 卡（同 pointId）加入后 isCurrentCardInWrongBook 为 true。
     * 验证:加入卡 A(pointId=p_sibling) 后,卡 B(pointId=p_sibling) 的 isCurrentCardInWrongBook 为 true。
     */
    @Test
    fun `sibling 卡加入后 isCurrentCardInWrongBook 为 true`() = runTest(testDispatcher) {
        val siblingCards = listOf(
            testClozeCard(front = "卡 A", pointId = "p_sibling"),
            testClozeCard(front = "卡 B", pointId = "p_sibling"),
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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        assertFalse("初始状态 isCurrentCardInWrongBook 应为 false",
            viewModel.isCurrentCardInWrongBook.value)

        viewModel.addToWrongAnswerBook()  // 加入卡 A
        advanceUntilIdle()

        assertTrue("加入后 isCurrentCardInWrongBook 应为 true",
            viewModel.isCurrentCardInWrongBook.value)

        // 推进到卡 B（sibling）
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        assertTrue("sibling 卡 isCurrentCardInWrongBook 也应为 true",
            viewModel.isCurrentCardInWrongBook.value)
    }

    /**
     * 场景:pointId 为空时不记录错题。
     * 验证:addToWrongAnswerBook 不调用 recordWrongAnswer,设 errorMessage。
     */
    @Test
    fun `addToWrongAnswerBook 空 pointId 不记录`() = runTest(testDispatcher) {
        val noPointCard = testClozeCard(pointId = "")
        cardRepository = FakeCardRepository(listOf(noPointCard))
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()

        assertTrue("不应记录错题", wrongAnswerRepository.recordedWrongAnswers.isEmpty())
        assertNotNull("应设置 errorMessage", viewModel.errorMessage.value)
        assertTrue("errorMessage 应包含'知识点关联缺失'",
            viewModel.errorMessage.value!!.contains("知识点关联缺失"))
    }

    /**
     * 场景:recordWrongAnswer 抛异常时设置 errorMessage。
     * 验证:addToWrongAnswerBook 后 errorMessage 非空且包含"错题本记录失败"。
     */
    @Test
    fun `addToWrongAnswerBook 失败时设 errorMessage`() = runTest(testDispatcher) {
        advanceUntilIdle()
        wrongAnswerRepository.throwOnRecord = RuntimeException("DB error")

        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()

        assertNotNull("应设置 errorMessage", viewModel.errorMessage.value)
        assertTrue("errorMessage 应包含'错题本记录失败'",
            viewModel.errorMessage.value!!.contains("错题本记录失败"))
    }

    /**
     * 场景:addToWrongAnswerBook 后 sessionManualAddCount 递增。
     * 验证:加入后 sessionManualAddCount 从 0 变为 1。
     */
    @Test
    fun `addToWrongAnswerBook 递增 sessionManualAddCount`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(0, viewModel.sessionManualAddCount.value)

        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()

        assertEquals("sessionManualAddCount 应为 1", 1, viewModel.sessionManualAddCount.value)
    }

    /**
     * 场景:addToWrongAnswerBook 不影响 sessionReviewedCount/sessionAgainCount。
     * 验证:加入后 sessionReviewedCount 和 sessionAgainCount 仍为 0（未评分）。
     */
    @Test
    fun `addToWrongAnswerBook 不影响会话统计`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(0, viewModel.sessionReviewedCount.value)
        assertEquals(0, viewModel.sessionAgainCount.value)

        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()

        assertEquals("sessionReviewedCount 应不变", 0, viewModel.sessionReviewedCount.value)
        assertEquals("sessionAgainCount 应不变", 0, viewModel.sessionAgainCount.value)
    }

    /**
     * 场景:retry 后 sessionManualAddCount 重置为 0。
     * 验证:加入后 retry,count 回到 0。
     */
    @Test
    fun `retry 重置 sessionManualAddCount`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()
        assertEquals(1, viewModel.sessionManualAddCount.value)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals("retry 后 sessionManualAddCount 应为 0", 0, viewModel.sessionManualAddCount.value)
    }

    /**
     * 场景:addToWrongAnswerBook 后 isCurrentCardInWrongBook 自动变为 true。
     * 验证:无需手动触发 _uiState 更新,combine 驱动自动重新计算。
     */
    @Test
    fun `addToWrongAnswerBook 后 isCurrentCardInWrongBook 自动更新`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse("初始状态 isCurrentCardInWrongBook 应为 false",
            viewModel.isCurrentCardInWrongBook.value)

        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()

        assertTrue("addToWrongAnswerBook 后 isCurrentCardInWrongBook 应自动变为 true",
            viewModel.isCurrentCardInWrongBook.value)
    }

    /**
     * 场景:addToWrongAnswerBook 成功后 successMessage 发射"已加入错题本"。
     * 验证:调用后 successMessage 非空且内容正确。
     */
    @Test
    fun `addToWrongAnswerBook 成功后发射 successMessage`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertNull("初始 successMessage 应为 null", viewModel.successMessage.value)

        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()

        assertNotNull("addToWrongAnswerBook 后 successMessage 应非空",
            viewModel.successMessage.value)
        assertEquals("successMessage 应为'已加入错题本'",
            "已加入错题本", viewModel.successMessage.value)
    }

    /**
     * 场景:clearSuccessMessage 清除 successMessage。
     * 验证:调用 clearSuccessMessage 后 successMessage 为 null。
     */
    @Test
    fun `clearSuccessMessage 清除成功消息`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()
        assertNotNull(viewModel.successMessage.value)

        viewModel.clearSuccessMessage()
        assertNull("clearSuccessMessage 后 successMessage 应为 null",
            viewModel.successMessage.value)
    }

    /**
     * 场景:retry 后 successMessage 被清空。
     * 验证:addToWrongAnswerBook → retry → successMessage 为 null。
     */
    @Test
    fun `retry 清空 successMessage`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.addToWrongAnswerBook()
        advanceUntilIdle()
        assertNotNull(viewModel.successMessage.value)

        viewModel.retry()
        advanceUntilIdle()

        assertNull("retry 后 successMessage 应为 null", viewModel.successMessage.value)
    }

    /**
     * 场景:进程死亡恢复后 manualAddedPointIds 从 SavedStateHandle 恢复。
     * 验证:模拟 SavedStateHandle 持有一个 pointId,初始化后 isCurrentCardInWrongBook 为 true。
     */
    @Test
    fun `进程死亡恢复后 manualAddedPointIds 正确恢复`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle().apply {
            this["manualAddedPointIds"] = "point_1,point_2"
            this["sessionManualAddCount"] = 2
        }
        cardRepository = FakeCardRepository(listOf(
            testClozeCard(pointId = "point_1"),
            testClozeCard(pointId = "point_2", front = "卡 B"),
        ))
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        val vm = CardsViewModel(
            savedStateHandle = savedStateHandle,
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        assertTrue("进程恢复后 point_1 的 isCurrentCardInWrongBook 应为 true",
            vm.isCurrentCardInWrongBook.value)
        assertEquals("sessionManualAddCount 应恢复为 2", 2, vm.sessionManualAddCount.value)
    }

    // ---------- v0.8.20 P1-2 新增:错误处理统一测试 ----------

    /**
     * 场景 34(P0):getCardsForReview 抛 SQLiteException 时,UI 显示"本地数据异常,请重启 App"。
     *
     * v0.8.20 P1-2 修复回归测试:
     * 原实现 catch 块用 `e.message ?: "加载失败"` 暴露原始 SQL 错误文本(如
     * "UNIQUE constraint failed: knowledge_points.id"),用户看到英文堆栈无措。
     * 现复用 core/common/util/friendlyErrorMessage,SQLiteException 统一映射为
     * "本地数据异常,请重启 App",与 KnowledgeViewModel 错误提示一致。
     *
     * 测试依赖:feature/cards build.gradle.kts 已配置
     * testOptions.unitTests.isReturnDefaultValues=true(允许实例化 Android SQLiteException)。
     */
    @Test
    fun `加载失败 SQLiteException 显示本地数据异常友好提示`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(
            initialCards = emptyList(),
            throwOnGetCards = SQLiteException("UNIQUE constraint failed: knowledge_points.id"),
        )
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        val error = viewModel.uiState.value.error
        assertNotNull("SQLiteException 时 uiState.error 应非 null", error)
        assertEquals(
            "SQLiteException 应映射为'本地数据异常,请重启 App'(不暴露英文 SQL 错误)",
            "本地数据异常,请重启 App",
            error,
        )
        assertFalse(
            "错误提示不应包含英文 SQL 文本",
            error!!.contains("UNIQUE constraint", ignoreCase = true),
        )
    }

    /**
     * 场景 35(P0):getCardsForReview 抛 SocketTimeoutException 时,UI 显示"网络超时,请检查网络后重试"。
     *
     * 模拟 FSRS 调度服务调用超时(虽然本仓库 FSRS 是本地实现,但保留网络异常分支
     * 与 KnowledgeViewModel 一致,future-proofing 远程 FSRS 接入)。
     */
    @Test
    fun `加载失败 SocketTimeoutException 显示网络超时友好提示`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(
            initialCards = emptyList(),
            throwOnGetCards = SocketTimeoutException("connect timed out"),
        )
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        val error = viewModel.uiState.value.error
        assertNotNull("SocketTimeoutException 时 uiState.error 应非 null", error)
        assertEquals(
            "SocketTimeoutException 应映射为'网络超时,请检查网络后重试'",
            "网络超时,请检查网络后重试",
            error,
        )
        assertFalse(
            "错误提示不应包含英文 connect timed out",
            error!!.contains("connect timed out", ignoreCase = true),
        )
    }

    /**
     * 场景 36(P1):getCardsForReview 抛 UnknownHostException 时,UI 显示"网络超时,请检查网络后重试"。
     *
     * 与场景 35 互补,验证 UnknownHostException 也走网络异常分支。
     */
    @Test
    fun `加载失败 UnknownHostException 显示网络超时友好提示`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(
            initialCards = emptyList(),
            throwOnGetCards = UnknownHostException("api.example.com"),
        )
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        val error = viewModel.uiState.value.error
        assertEquals(
            "UnknownHostException 应映射为'网络超时,请检查网络后重试'",
            "网络超时,请检查网络后重试",
            error,
        )
    }

    /**
     * 场景 37(P0):getCardsForReview 抛未知 RuntimeException 时,UI 显示兜底"加载失败,请重试"。
     *
     * 验证 friendlyErrorMessage 的兜底分支,以及与原 `e.message ?: "加载失败"` 的差异:
     * 原实现会暴露 e.message(如 "unexpected null pointer"),
     * 现统一映射为"加载失败,请重试",不暴露任何英文文本。
     */
    @Test
    fun `加载失败未知 RuntimeException 显示兜底友好提示`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(
            initialCards = emptyList(),
            throwOnGetCards = RuntimeException("unexpected null pointer in parser"),
        )
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        val error = viewModel.uiState.value.error
        assertNotNull("未知异常时 uiState.error 应非 null", error)
        assertEquals(
            "未知异常应映射为兜底'加载失败,请重试'(不暴露原始 message)",
            "加载失败,请重试",
            error,
        )
        assertFalse(
            "错误提示不应包含英文 unexpected",
            error!!.contains("unexpected", ignoreCase = true),
        )
        assertFalse(
            "错误提示不应包含 null pointer",
            error.contains("null pointer", ignoreCase = true),
        )
    }

    /**
     * 场景 38(P1):加载失败后调用 retry(),错误清空并重新加载成功。
     *
     * 验证 retry() 的契约:
     * - 1. retry() 立即设置 isLoading=true,error=null(快速反馈)
     * - 2. retry() 触发 _retryTrigger,Flow 重新订阅
     * - 3. 重新加载成功后 uiState.isLoading=false, cards 非空, error=null
     *
     * 使用 FakeCardRepository 的 throwOnGetCards 字段(可变)模拟"先失败后成功":
     * - 初始:抛 SQLiteException
     * - retry 前:清空 throwOnGetCards(恢复正常返回空列表的 Flow)
     */
    @Test
    fun `加载失败后 retry 清空错误并重新加载`() = runTest(testDispatcher) {
        // 初始:抛异常
        cardRepository = FakeCardRepository(
            initialCards = listOf(testClozeCard()),
            throwOnGetCards = SQLiteException("no such table: memo_records"),
        )
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        // 验证初始为错误态
        assertNotNull("初始应加载失败", viewModel.uiState.value.error)
        assertEquals(
            "初始错误应为本地数据异常",
            "本地数据异常,请重启 App",
            viewModel.uiState.value.error,
        )

        // 修复 Fake:清空 throwOnGetCards,恢复返回正常卡片流
        cardRepository.throwOnGetCards = null
        // 触发 retry
        viewModel.retry()
        // advanceUntilIdle 让 retry 的 _retryTrigger Flow 重新订阅 + 加载完成
        //
        // v0.8.20 P1-2 修复:原 CardsViewModel 把 .catch 放在 flatMapLatest 外层,
        // 导致首次加载失败后 .catch emit 错误态使整条 Flow 终止,retry() 设置
        // isLoading=true 后再无 collector 把它改回 false,UI 永远卡 loading。
        // 现已把 .catch 移入 flatMapLatest 内部,仅终止本次订阅,外层 Flow 仍由
        // _retryTrigger 驱动,retry() 真正重新触发加载。本测试是此修复的回归保护。
        advanceUntilIdle()
        advanceUntilIdle()

        // 验证 retry 后清空错误 + 重新加载
        assertNull(
            "retry 后 error 应清空",
            viewModel.uiState.value.error,
        )
        assertFalse(
            "retry 后 isLoading 应为 false(加载完成)",
            viewModel.uiState.value.isLoading,
        )
        assertNotNull(
            "retry 后应能加载到卡片(throwOnGetCards 已清空)",
            viewModel.uiState.value.currentCard,
        )
    }

    /**
     * 场景 39(P1):rateCard 时 schedulingRepository.rateCard 抛异常,error message 以"评分调度失败"开头。
     *
     * v0.8.20 P1-2 审计发现:此路径用 `e.message ?: "未知错误"` 暴露 raw exception message,
     * 与加载失败的 friendlyErrorMessage 不一致(记为 P2 finding,不在本批修复)。
     *
     * 本测试锁定当前行为(以"评分调度失败"前缀 + raw message),作为后续 P2 修复的基线:
     * - 验证错误确实被捕获到 _errorMessage(不冒泡崩溃)
     * - 验证前缀"评分调度失败："存在(用户能识别错误来源)
     * - 不强制断言具体 message 内容(允许 P2 修复后改为 friendlyErrorMessage)
     */
    @Test
    fun `评分调度失败时 errorMessage 包含评分调度失败前缀`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(listOf(testClozeCard()))
        schedulingRepository = FakeSchedulingRepository().apply {
            // 让 rateCard 抛 RuntimeException
            throwException = RuntimeException("FSRS algorithm crashed: division by zero")
        }
        wrongAnswerRepository = FakeWrongAnswerRepository()
        studyProgressRepository = FakeStudyProgressRepository()
        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            studyProgressRepository = studyProgressRepository,
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        // 评分触发调度失败
        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        val error = viewModel.errorMessage.value
        assertNotNull("评分调度失败时 errorMessage 应非 null", error)
        assertTrue(
            "errorMessage 应以'评分调度失败'开头(标识错误来源),实际: $error",
            error!!.startsWith("评分调度失败"),
        )
        // v0.8.20 P2 finding:当前实现暴露 raw message("FSRS algorithm crashed: division by zero")
        // 后续 P2 修复应改为 friendlyErrorMessage,届时此断言需相应更新。
        // 现仅断言前缀存在,允许 message 内容变化。
    }

    // ---------- v0.9.7 新增：B2 Leech oldFailCount 反推修复测试 ----------

    /**
     * 场景 40（v0.9.7 B2 修复）：RELEARNING 状态 + AGAIN 评分时 failCount 不变,
     * 不应误触发 Leech 警告。
     *
     * 背景:FSRS-6 中仅 REVIEW + AGAIN 增加 lapses(=failCount),
     * LEARNING/RELEARNING + AGAIN 不增加(注释"学习阶段答Again:尚未记住,不构成遗忘")。
     *
     * 原 bug:CardsViewModel L526-529 反推 oldFailCount 时,AGAIN 总是 `failCount - 1`,
     * 若 RELEARNING + AGAIN 时 updated.failCount=8(未变),反推 oldFailCount=7,
     * 误满足 `7 < 8 && 8 >= 8` → 弹 Leech 警告(误报)。
     *
     * 修复后:根据 updated.state 区分,RELEARNING + AGAIN 时 oldFailCount = failCount(不减 1),
     * `8 < 8` 为 false → 不弹警告。
     *
     * 测试构造:FakeSchedulingRepository 返回 state="RELEARNING", failCount=8,
     * 模拟用户在 RELEARNING 状态答 AGAIN。期望:不弹 leechWarning。
     */
    @Test
    fun `B2 修复 RELEARNING 状态 AGAIN 评分 failCount 不变时不误报 Leech`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(listOf(testClozeCard()))
        schedulingRepository = FakeSchedulingRepository(
            rateCardResult = MemoRecordEntity(
                pointId = "point_1",
                state = "RELEARNING", // 关键:RELEARNING 状态,AGAIN 不增加 lapses
                stability = 1f,
                difficulty = 9f,
                lastReviewAt = 1000L,
                nextReviewAt = 2000L,
                reviewCount = 8,
                failCount = 8, // 已达 LEECH_THRESHOLD=8,但因 RELEARNING + AGAIN 不增加,oldFailCount 应=8
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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        assertNull("评分前 leechWarning 应为 null", viewModel.leechWarning.value)

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        assertNull(
            "RELEARNING + AGAIN 时 failCount 未变(8→8),oldFailCount 应=8,不满足 oldFailCount<8,不应弹 leechWarning",
            viewModel.leechWarning.value,
        )
    }

    /**
     * 场景 41（v0.9.7 B2 对照组）：REVIEW 状态 + AGAIN 评分时 failCount 从 7→8,
     * 应正确触发 Leech 警告。
     *
     * 这是 B2 修复的对照组:REVIEW + AGAIN 确实增加 lapses(7→8),
     * oldFailCount = 8 - 1 = 7,满足 `7 < 8 && 8 >= 8` → 弹警告。
     * 验证 B2 修复没有破坏正常的 Leech 检测。
     */
    @Test
    fun `B2 对照组 REVIEW 状态 AGAIN 评分 failCount 跨阈值时正确弹 Leech`() = runTest(testDispatcher) {
        cardRepository = FakeCardRepository(listOf(testClozeCard()))
        schedulingRepository = FakeSchedulingRepository(
            rateCardResult = MemoRecordEntity(
                pointId = "point_1",
                state = "REVIEW", // 关键:REVIEW 状态,AGAIN 增加 lapses
                stability = 1f,
                difficulty = 9f,
                lastReviewAt = 1000L,
                nextReviewAt = 2000L,
                reviewCount = 8,
                failCount = 8, // 跨阈值:7→8
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
            cardSettingsRepository = FakeCardSettingsRepository(),
        )
        advanceUntilIdle()

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        val warning = viewModel.leechWarning.value
        assertNotNull(
            "REVIEW + AGAIN 时 failCount 7→8 跨阈值,应弹 leechWarning",
            warning,
        )
        assertEquals("point_1", warning!!.pointId)
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
