package com.wenyan.app.feature.quiz

import androidx.lifecycle.SavedStateHandle
import com.wenyan.app.core.data.repository.ExamQuestionWithSubject
import com.wenyan.app.core.data.repository.WrongAnswerRepository
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
 * [QuizViewModel] 单元测试(NF-PP5 Wave 3.2)。
 *
 * 验证答题交互三方法:
 * - [updateAnswer]:仅未提交时允许编辑
 * - [submitAnswer]:标记已提交 + 自动展开参考答案
 * - [selfEvaluate]:记录对错,答错时记录错题(SOURCE_QUIZ_WRONG)
 *
 * 用 StandardTestDispatcher + advanceUntilIdle 控制协程执行时序。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var examRepository: FakeExamRepository
    private lateinit var wrongAnswerRepository: FakeWrongAnswerRepository
    private lateinit var viewModel: QuizViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        // 预设 1 个年份 + 1 道有答题框架的真题
        examRepository = FakeExamRepository(
            years = listOf(2024),
            questionsByYear = mapOf(
                2024 to listOf(
                    ExamQuestionWithSubject(testExamQuestion(), TEST_SUBJECT_RESOLUTION),
                ),
            ),
        )
        wrongAnswerRepository = FakeWrongAnswerRepository()

        // 预选 2024 年,init 时直接加载题目
        viewModel = QuizViewModel(
            savedStateHandle = SavedStateHandle(mapOf("selectedYear" to 2024)),
            examRepository = examRepository,
            wrongAnswerRepository = wrongAnswerRepository,
        )
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 场景 1:updateAnswer 在未提交时更新 userAnswer。
     */
    @Test
    fun `updateAnswer 未提交时更新用户答案`() = runTest(testDispatcher) {
        viewModel.updateAnswer("q_1", "我的初版答案")
        advanceUntilIdle()

        val answer = viewModel.answers.value["q_1"]
        assertNotNull("answers 中应有 q_1 条目", answer)
        assertEquals("我的初版答案", answer!!.userAnswer)
        assertFalse("未提交时 isSubmitted 应为 false", answer.isSubmitted)
    }

    /**
     * 场景 2:submitAnswer 标记 isSubmitted=true 并自动展开参考答案区。
     *
     * 验证:
     * - 答案已提交后,updateAnswer 不再修改 userAnswer(锁定)
     * - expandedQuestionIds 包含 q_1(自动展开)
     */
    @Test
    fun `submitAnswer 标记已提交并展开参考答案`() = runTest(testDispatcher) {
        viewModel.updateAnswer("q_1", "我的答案")
        viewModel.submitAnswer("q_1")
        advanceUntilIdle()

        val answer = viewModel.answers.value["q_1"]
        assertNotNull(answer)
        assertTrue("提交后 isSubmitted 应为 true", answer!!.isSubmitted)

        // 自动展开
        assertTrue(
            "expandedQuestionIds 应包含 q_1",
            "q_1" in viewModel.expandedQuestionIds.value,
        )

        // 锁定:提交后 updateAnswer 不应修改 userAnswer
        viewModel.updateAnswer("q_1", "尝试修改")
        advanceUntilIdle()
        assertEquals(
            "提交后答案应锁定",
            "我的答案",
            viewModel.answers.value["q_1"]!!.userAnswer,
        )
    }

    /**
     * 场景 3:selfEvaluate(isCorrect=true) 不记录错题。
     */
    @Test
    fun `selfEvaluate 答对不记录错题`() = runTest(testDispatcher) {
        viewModel.updateAnswer("q_1", "我的答案")
        viewModel.submitAnswer("q_1")
        advanceUntilIdle()

        viewModel.selfEvaluate("q_1", isCorrect = true)
        advanceUntilIdle()

        val answer = viewModel.answers.value["q_1"]!!
        assertTrue("应标记 isSelfEvaluated=true", answer.isSelfEvaluated)
        assertTrue("应标记 isCorrect=true", answer.isCorrect)
        assertTrue("答对不应记录错题", wrongAnswerRepository.recordedWrongAnswers.isEmpty())
    }

    /**
     * 场景 4:selfEvaluate(isCorrect=false) 记录错题,
     * source = SOURCE_QUIZ_WRONG, examQuestionId = q_1,
     * correctAnswer = answerFramework（v0.7.6 起范文字段已删除）。
     */
    @Test
    fun `selfEvaluate 答错记录错题且 source 为 QUIZ_WRONG`() = runTest(testDispatcher) {
        viewModel.updateAnswer("q_1", "我的错误答案")
        viewModel.submitAnswer("q_1")
        advanceUntilIdle()

        viewModel.selfEvaluate("q_1", isCorrect = false)
        advanceUntilIdle()

        val answer = viewModel.answers.value["q_1"]!!
        assertTrue("应标记 isSelfEvaluated=true", answer.isSelfEvaluated)
        assertFalse("应标记 isCorrect=false", answer.isCorrect)

        // 验证错题记录
        assertEquals("应记录一次错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
        val record = wrongAnswerRepository.recordedWrongAnswers[0]
        assertEquals(
            "source 应为 QUIZ_WRONG",
            WrongAnswerRepository.SOURCE_QUIZ_WRONG,
            record.source,
        )
        assertEquals("examQuestionId 应为 q_1", "q_1", record.examQuestionId)
        assertEquals("userAnswer 应为我的错误答案", "我的错误答案", record.userAnswer)
        // testExamQuestion 默认 answerFramework 非空,correctAnswer 应为 answerFramework
        assertEquals(
            "correctAnswer 应为 answerFramework",
            "1. 词坛贡献 2. 诗歌成就 3. 散文影响",
            record.correctAnswer,
        )
    }

    // ── v0.8.21 修复回归测试(对照 feature/knowledge + feature/cards) ───────

    /**
     * 场景 5(v0.8.21 新增):加载失败后 retry() 真正重新加载,验证 B2 修复。
     *
     * 原实现 catch 在 flatMapLatest 外层,异常触发后整流终止,
     * retry() 通过 _retryTrigger++ 触发的重订阅无法被任何 collector 接收,
     * UI 永远停留在 error 态(必须杀进程重启 App 才能恢复)。
     *
     * 现修复后 catch 移入 flatMapLatest 内部,仅终止本次 inner Flow,
     * 外层仍由 _retryTrigger 驱动,retry() 真正生效。
     *
     * 步骤:
     * 1. 让 getExamQuestionsWithSubjectInfo 抛 RuntimeException
     * 2. 验证 uiState.error 非 null,isLoading=false
     * 3. 清除异常(模拟数据库恢复)
     * 4. 调用 retry()
     * 5. 验证 uiState.error == null,questions 正常加载
     */
    @Test
    fun `加载失败后 retry 真正重新加载`() = runTest(testDispatcher) {
        // 1. 注入异常:questionsException 会在 getExamQuestionsWithSubjectInfo 抛出
        examRepository.questionsException = RuntimeException("DB corrupted")
        viewModel.retry()
        advanceUntilIdle()

        // 2. 验证 error 状态
        val errorState = viewModel.uiState.value
        assertFalse("error 态下 isLoading 应为 false", errorState.isLoading)
        assertNotNull("error 应非 null", errorState.error)

        // 3. 清除异常(数据库恢复)
        examRepository.questionsException = null

        // 4. 调用 retry()
        viewModel.retry()
        advanceUntilIdle()

        // 5. 验证恢复正常加载
        val recoveredState = viewModel.uiState.value
        assertNull("retry 后 error 应清空", recoveredState.error)
        assertFalse("retry 后 isLoading 应为 false", recoveredState.isLoading)
        assertEquals("应加载 1 道真题", 1, recoveredState.questions.size)
        assertEquals("题目 id 应为 q_1", "q_1", recoveredState.questions[0].id)
    }

    /**
     * 场景 6(v0.8.21 新增):catch 分支验证错误友好提示(M2 修复)。
     *
     * 原实现用 raw `e.message` 展示英文堆栈给用户,
     * 现改用 [com.wenyan.app.core.common.util.friendlyErrorMessage] 映射为中文友好提示。
     *
     * 注:unit test 环境无 Robolectric,SQLiteException 是 stub 无法实例化,
     * 故用 message 包含 "no such table" 的 RuntimeException 验证兜底映射路径
     * (friendlyErrorMessage 检测 message.contains("no such table") → "数据库版本异常,请重启 App")。
     */
    @Test
    fun `catch 分支将异常映射为友好提示`() = runTest(testDispatcher) {
        examRepository.questionsException =
            RuntimeException("no such table: exam_questions")
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

    /**
     * 场景 7(v0.8.21 新增):selfEvaluate 错题记录失败时反馈 errorMessage,验证 M3 修复。
     *
     * 原实现 `catch (e: Exception) {}` 静默吞异常,与 CardsViewModel 不一致,
     * 生产排查困难,用户错题记录失败后无任何感知。
     *
     * 现修复后加 Log.w + 设置 errorMessage 反馈用户(通过 Snackbar 展示)。
     * 错题记录失败不阻塞主流程(自评状态已更新),用户可查看错题本或重试。
     *
     * 验证点:
     * - recordWrongAnswer 抛异常时,errorMessage 应非 null 且包含中文提示
     * - 自评状态仍正确更新(isSelfEvaluated=true, isCorrect=false)
     * - 错题记录未被写入(recordedWrongAnswers 为空)
     */
    @Test
    fun `selfEvaluate 错题记录失败时反馈 errorMessage`() = runTest(testDispatcher) {
        // 注入 recordWrongAnswer 异常
        wrongAnswerRepository.recordException = RuntimeException("DB write failed")

        viewModel.updateAnswer("q_1", "我的错误答案")
        viewModel.submitAnswer("q_1")
        advanceUntilIdle()

        viewModel.selfEvaluate("q_1", isCorrect = false)
        advanceUntilIdle()

        // 自评状态仍正确更新(错题记录失败不阻塞主流程)
        val answer = viewModel.answers.value["q_1"]!!
        assertTrue("自评状态应更新为 isSelfEvaluated=true", answer.isSelfEvaluated)
        assertFalse("自评结果应为 isCorrect=false", answer.isCorrect)

        // 错题记录失败反馈
        val errorMessage = viewModel.errorMessage.value
        assertNotNull("recordWrongAnswer 失败应设置 errorMessage", errorMessage)
        assertTrue(
            "errorMessage 应包含中文提示'错题记录失败'",
            errorMessage!!.contains("错题记录失败"),
        )

        // 错题记录未被写入
        assertTrue(
            "recordWrongAnswer 抛异常时不应写入错题",
            wrongAnswerRepository.recordedWrongAnswers.isEmpty(),
        )

        // clearError 可清空错误提示(供 UI Snackbar 展示后调用)
        viewModel.clearError()
        assertNull("clearError 后 errorMessage 应为 null", viewModel.errorMessage.value)
    }

    /**
     * 场景 8(v0.8.21 新增):updateAnswer 限制最大长度,验证 M4 修复。
     *
     * 原实现无长度限制,粘贴整本教材等异常输入会导致:
     * - StateFlow 持有超大字符串导致内存压力
     * - SavedStateHandle(Bundle)序列化超大字符串导致 TransactionTooLargeException
     * - 错题本记录超长答案导致 wrong_answers 表膨胀
     *
     * 现限制 MAX_ANSWER_LENGTH=5000 字符,超出截断(不影响正常使用,
     * 考研论述题答案典型 1500-3000 字)。
     */
    @Test
    fun `updateAnswer 限制最大长度并截断超长输入`() = runTest(testDispatcher) {
        // 构造 6000 字符的超长答案
        val longAnswer = "A".repeat(6000)
        viewModel.updateAnswer("q_1", longAnswer)
        advanceUntilIdle()

        val answer = viewModel.answers.value["q_1"]
        assertNotNull("answers 中应有 q_1 条目", answer)
        assertEquals(
            "超长输入应截断为 5000 字符",
            5000,
            answer!!.userAnswer.length,
        )
    }

    /**
     * 场景 9(v0.8.21 新增):selfEvaluate 记录错题时超长答案省略,验证 M5 修复。
     *
     * 原实现将完整 userAnswer 持久化到错题本,超长答案导致:
     * - wrong_answers 表存储超长 userAnswer 导致查询变慢
     * - 错题本 UI 渲染超长文本导致列表卡顿
     *
     * 现限制 MAX_USER_ANSWER_FOR_WRONG=500 字符,超出部分用 "…" 省略
     * (500 字符足够展示用户答案核心内容,错题本目的是定位错点不是完整保留答案)。
     */
    @Test
    fun `selfEvaluate 记录错题时超长答案省略到 500 字符`() = runTest(testDispatcher) {
        // 构造 600 字符的答案(超过 MAX_USER_ANSWER_FOR_WRONG=500)
        val longAnswer = "B".repeat(600)
        viewModel.updateAnswer("q_1", longAnswer)
        viewModel.submitAnswer("q_1")
        advanceUntilIdle()

        viewModel.selfEvaluate("q_1", isCorrect = false)
        advanceUntilIdle()

        assertEquals("应记录一次错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
        val record = wrongAnswerRepository.recordedWrongAnswers[0]
        assertEquals(
            "错题本记录的 userAnswer 应省略到 500 字符 + 省略号",
            501, // 500 字符 + 1 个 "…"
            record.userAnswer.length,
        )
        assertTrue(
            "省略后的 userAnswer 应以 … 结尾",
            record.userAnswer.endsWith("…"),
        )
    }
}
