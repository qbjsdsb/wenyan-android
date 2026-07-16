package com.wenyan.app.feature.quiz

import com.wenyan.app.core.database.entity.WrongAnswerEntity
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
 * [WrongAnswerViewModel] 单元测试(NF-PP5 Wave 3.2)。
 *
 * 验证:
 * - 默认 filter=UNRESOLVED,uiState 从 observeUnresolved 加载
 * - setFilter(ALL) 切换后,uiState 从 observeAll 加载
 * - markResolved / deleteById 调用仓库对应方法
 *
 * 用 StandardTestDispatcher + advanceUntilIdle 控制 stateIn 协程。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WrongAnswerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var wrongAnswerRepository: FakeWrongAnswerRepository
    private lateinit var viewModel: WrongAnswerViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        // 预设:未解决 2 条,全部 3 条(含 1 条已解决)
        val unresolved = listOf(
            sampleWrongAnswer("wa_1", isResolved = false),
            sampleWrongAnswer("wa_2", isResolved = false),
        )
        val all = unresolved + sampleWrongAnswer("wa_3", isResolved = true)

        wrongAnswerRepository = FakeWrongAnswerRepository(
            initialAll = all,
            initialUnresolved = unresolved,
        )

        viewModel = WrongAnswerViewModel(wrongAnswerRepository)
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 场景 1:默认 filter=UNRESOLVED,uiState.items 从 observeUnresolved 加载。
     */
    @Test
    fun `默认 filter 为 UNRESOLVED 且加载未解决错题`() = runTest(testDispatcher) {
        assertEquals(WrongAnswerFilter.UNRESOLVED, viewModel.filter.value)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertEquals("应加载 2 条未解决错题", 2, items.size)
        assertTrue("所有项 isResolved 应为 false", items.all { !it.isResolved })
    }

    /**
     * 场景 2:setFilter(ALL) 切换后,uiState.items 从 observeAll 加载(3 条,含已解决)。
     *
     * 同时验证 markResolved / deleteById 调用仓库对应方法。
     */
    @Test
    fun `setFilter ALL 切换到全部错题并验证 markResolved 和 deleteById`() = runTest(testDispatcher) {
        viewModel.setFilter(WrongAnswerFilter.ALL)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertEquals("应加载 3 条全部错题", 3, items.size)
        assertTrue("应包含 1 条已解决", items.count { it.isResolved } == 1)

        // markResolved
        viewModel.markResolved("wa_1")
        advanceUntilIdle()
        assertTrue("markResolved 应调用仓库", "wa_1" in wrongAnswerRepository.resolvedIds)

        // deleteById
        viewModel.deleteById("wa_2")
        advanceUntilIdle()
        assertTrue("deleteById 应调用仓库", "wa_2" in wrongAnswerRepository.deletedIds)
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    private fun sampleWrongAnswer(
        id: String,
        isResolved: Boolean,
        source: String = "CARD_AGAIN",
    ) = WrongAnswerEntity(
        id = id,
        pointId = "point_$id",
        examQuestionId = null,
        userAnswer = "用户答案 $id",
        correctAnswer = "正确答案 $id",
        source = source,
        wrongCount = 1,
        lastWrongAt = 1000L,
        resolvedAt = if (isResolved) 2000L else null,
        aiExplanation = null,
        createdAt = 500L,
    )
}
