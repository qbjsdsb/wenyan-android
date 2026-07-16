package com.wenyan.app.feature.cards

import androidx.lifecycle.SavedStateHandle
import com.wenyan.app.core.data.repository.WrongAnswerRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [CardsViewModel] 单元测试(NF-PP5 Wave 3.2)。
 *
 * 仅验证 NF-PP5 新增的 AGAIN 记录错题行为,其他 CardsViewModel 行为
 * (翻面/索引推进/FSRS 调度)已由 SchedulingRepositoryTest + FlipCardLogicTest 覆盖。
 *
 * 用 StandardTestDispatcher + advanceUntilIdle 控制协程执行时序,
 * 确保 rateCard 内的 viewModelScope.launch(AGAIN 记录错题)在断言前完成。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var cardRepository: FakeCardRepository
    private lateinit var schedulingRepository: FakeSchedulingRepository
    private lateinit var wrongAnswerRepository: FakeWrongAnswerRepository
    private lateinit var viewModel: CardsViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        // 预设 1 张测试卡片(pointId="point_1")
        cardRepository = FakeCardRepository(listOf(testClozeCard()))
        schedulingRepository = FakeSchedulingRepository()
        wrongAnswerRepository = FakeWrongAnswerRepository()

        viewModel = CardsViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = cardRepository,
            schedulingRepository = schedulingRepository,
            wrongAnswerRepository = wrongAnswerRepository,
        )

        // 等待 init 块的 combine + collect 完成,加载卡片
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
        // 等待卡片加载,确保 currentCard 非 null
        advanceUntilIdle()
        val currentCard = viewModel.uiState.value.currentCard
        assertTrue("应有当前卡片", currentCard != null)
        assertEquals("point_1", currentCard!!.pointId)

        viewModel.rateCard(CardRating.AGAIN)
        advanceUntilIdle()

        // 验证调度被调用
        assertEquals("应调用一次 rateCard", 1, schedulingRepository.rateCardCalls.size)
        val (_, rating, _) = schedulingRepository.rateCardCalls[0]
        assertEquals(Rating.AGAIN, rating)

        // 验证错题记录被调用
        assertEquals("应记录一次错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
        val record = wrongAnswerRepository.recordedWrongAnswers[0]
        assertEquals("pointId 应为 point_1", "point_1", record.pointId)
        assertEquals("source 应为 CARD_AGAIN", WrongAnswerRepository.SOURCE_CARD_AGAIN, record.source)
        assertEquals("correctAnswer 应为卡片背面", "北宋文学家", record.correctAnswer)
    }

    /**
     * 场景 2:rateCard(GOOD) 后 wrongAnswerRepository.recordWrongAnswer 不被调用。
     *
     * 仅 AGAIN 评分记录错题,GOOD/HARD/EASY 不记录。
     */
    @Test
    fun `rateCard GOOD 不记录错题`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.currentCard != null)

        viewModel.rateCard(CardRating.GOOD)
        advanceUntilIdle()

        // 调度被调用
        assertEquals(1, schedulingRepository.rateCardCalls.size)
        assertEquals(Rating.GOOD, schedulingRepository.rateCardCalls[0].second)

        // 错题不应被记录
        assertTrue(
            "GOOD 评分不应记录错题",
            wrongAnswerRepository.recordedWrongAnswers.isEmpty(),
        )
    }
}
