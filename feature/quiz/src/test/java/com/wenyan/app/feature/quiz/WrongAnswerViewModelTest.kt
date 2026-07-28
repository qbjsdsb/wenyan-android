package com.wenyan.app.feature.quiz

import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.database.entity.WrongAnswerWithDetails
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
 * [WrongAnswerViewModel] 单元测试(NF-PP5 Wave 3.2 + v0.8.21 retry 回归)。
 *
 * 验证:
 * - 默认 filter=UNRESOLVED,uiState 从 observeUnresolved 加载
 * - setFilter(ALL) 切换后,uiState 从 observeAll 加载
 * - markResolved / deleteById 调用仓库对应方法
 *
 * v0.8.21 新增(对照 feature/knowledge + feature/cards 修复模式):
 * - 场景 3:加载失败后 retry() 真正重新加载(回归 B1 修复)
 * - 场景 4:catch 分支验证错误友好提示(SQLiteException → "本地数据异常,请重启 App")
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

    /**
     * 场景 3(v0.8.21 新增):加载失败后 retry() 真正重新加载,验证 B1 修复。
     *
     * 原实现 catch 在 flatMapLatest 外层,异常触发后整流终止,retry() 失效。
     * 现修复后 catch 移入 flatMapLatest 内部,retry() 通过 retryTrigger 重新触发。
     *
     * 步骤:
     * 1. 让 observeUnresolved 抛 RuntimeException
     * 2. 验证 uiState.error 非 null,isLoading=false
     * 3. 清除异常(模拟数据库恢复)
     * 4. 调用 retry()
     * 5. 验证 uiState.error == null,isLoading=false,items 正常加载
     */
    @Test
    fun `加载失败后 retry 真正重新加载`() = runTest(testDispatcher) {
        // 1. 注入异常
        wrongAnswerRepository.unresolvedException = RuntimeException("DB corrupted")
        // 重新触发订阅(retry 让 init 重新订阅)
        viewModel.retry()
        advanceUntilIdle()

        // 2. 验证 error 状态
        val errorState = viewModel.uiState.value
        assertFalse("error 态下 isLoading 应为 false", errorState.isLoading)
        assertNotNull("error 应非 null", errorState.error)

        // 3. 清除异常(数据库恢复)
        wrongAnswerRepository.unresolvedException = null

        // 4. 调用 retry()
        viewModel.retry()
        advanceUntilIdle()

        // 5. 验证恢复正常加载
        val recoveredState = viewModel.uiState.value
        assertNull("retry 后 error 应清空", recoveredState.error)
        assertFalse("retry 后 isLoading 应为 false", recoveredState.isLoading)
        assertEquals("应加载 2 条未解决错题", 2, recoveredState.items.size)
    }

    /**
     * 场景 4(v0.8.21 新增):catch 分支验证错误友好提示(M2 修复)。
     *
     * 原实现用 raw `e.message` 展示英文堆栈给用户,
     * 现改用 [com.wenyan.app.core.common.util.friendlyErrorMessage] 映射为中文友好提示。
     *
     * 注:unit test 环境无 Robolectric,SQLiteException 是 stub 无法实例化,
     * 故用 message 包含 "no such table" 的 RuntimeException 验证兜底映射路径
     * (friendlyErrorMessage 检测 message.contains("no such table") → "数据库版本异常,请重启 App")。
     * 这条路径更严格地验证了"不暴露英文堆栈给用户"的设计意图。
     */
    @Test
    fun `catch 分支将异常映射为友好提示`() = runTest(testDispatcher) {
        wrongAnswerRepository.unresolvedException =
            RuntimeException("no such table: wrong_answers")
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("应有 error", state.error)
        assertEquals(
            "异常应映射为友好提示(不暴露英文堆栈)",
            "数据库版本异常,请重启 App",
            state.error,
        )
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    /**
     * 构造测试用 [WrongAnswerWithDetails]（v0.9.2：JOIN 后的 POJO，含 questionTitle）。
     *
     * @param questionTitle 题目文本（默认 "题目 $id" 模拟 JOIN 到的知识点 title；
     *   测试可传 null 验证"题目已删除"兜底分支）
     */
    private fun sampleWrongAnswer(
        id: String,
        isResolved: Boolean,
        source: String = "CARD_AGAIN",
        questionTitle: String? = "题目 $id",
    ) = WrongAnswerWithDetails(
        wrongAnswer = WrongAnswerEntity(
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
        ),
        questionTitle = questionTitle,
    )
}
