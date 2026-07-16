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
     * correctAnswer = sampleEssay(优先) 或 answerFramework。
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
        // testExamQuestion 默认 sampleEssay=null,correctAnswer 应为 answerFramework
        assertEquals(
            "correctAnswer 应为 answerFramework",
            "1. 词坛贡献 2. 诗歌成就 3. 散文影响",
            record.correctAnswer,
        )
    }
}
