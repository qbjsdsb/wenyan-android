package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * [QuizPracticeDetailViewModel] 背题详情 ViewModel 测试（v0.9.35 新增）。
 *
 * 覆盖审计修复：
 * - markDontKnow 快速连点只推进一次（400ms 防连击锁）
 * - markDontKnow 只写入一次错题本（原竞态会重复写入 wrongCount 虚增 + 静默跳题）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizPracticeDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun entity(id: String, year: Int = 2020) = ExamQuestionEntity(
        id = id,
        year = year,
        subjectId = "sub1",
        questionType = "TERM_EXPLANATION",
        content = "题目$id",
        score = 5,
        angle = null,
        relatedPointIds = null,
        answerFramework = "参考答案$id",
        notes = null,
        createdAt = 1000L,
        examPaperCode = "610",
        answerStatus = "HAS_ANSWER",
        materialText = null,
        sourceFile = null,
        sourcePage = null,
    )

    /**
     * currentQuestion 是 WhileSubscribed StateFlow——无订阅者时返回 initialValue(null)。
     * 测试需先启动订阅（UnconfinedTestDispatcher 下 launch 立即执行）再读值。
     */
    private fun kotlinx.coroutines.test.TestScope.subscribeCurrentQuestion(
        vm: QuizPracticeDetailViewModel,
    ) {
        // backgroundScope：runTest 结束自动取消，避免无限收集阻塞测试完成
        backgroundScope.launch { vm.currentQuestion.collect {} }
        runCurrent()
        advanceUntilIdle()
    }

    private fun createViewModel(
        questionId: String,
        essays: List<ExamQuestionEntity>,
        wrongRepo: FakeKnowledgeWrongAnswerRepository,
    ): QuizPracticeDetailViewModel {
        val examDao = FakeExamQuestionDao(initialEssays = essays)
        val repo = buildKnowledgeRepository(
            knowledgePointDao = FakeKnowledgePointDao(),
            dataSourceDao = FakeDataSourceDao(),
            examQuestionDao = examDao,
        )
        return QuizPracticeDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("questionId" to questionId)),
            knowledgeRepository = repo,
            wrongAnswerRepository = wrongRepo,
        )
    }

    @Test
    fun `markDontKnow快速连点只推进一次且只写一次错题本`() = runTest(dispatcher) {
        val wrongRepo = FakeKnowledgeWrongAnswerRepository()
        val vm = createViewModel("q1", listOf(entity("q1"), entity("q2")), wrongRepo)

        // 等待题目加载完成，当前第 1 题（订阅 WhileSubscribed StateFlow）
        subscribeCurrentQuestion(vm)
        assertEquals("q1", vm.currentQuestion.value?.id)
        assertEquals(0, vm.currentIndex.value)

        // 注入可控时间源：恒定 1000ms → 首次调用通过防连击锁（1000-0≥400），
        // 第二次调用 now 不变（1000-1000<400）被锁，模拟 400ms 内快速连点
        vm.uptimeMillis = { 1000L }
        // 400ms 内连点两次"不会"——第二次被防连击锁拦截
        vm.markDontKnow()
        vm.markDontKnow()
        advanceUntilIdle()

        // 只推进一次：第 1 题 → 第 2 题
        assertEquals(1, vm.currentIndex.value)
        // 只写一次错题本，且写入的是第 1 题（同步推进语义）
        assertEquals(1, wrongRepo.recordedWrongAnswers.size)
        assertEquals("q1", wrongRepo.recordedWrongAnswers[0].examQuestionId)
    }

    @Test
    fun `markKnow快速连点只推进一次`() = runTest(dispatcher) {
        val wrongRepo = FakeKnowledgeWrongAnswerRepository()
        val vm = createViewModel("q1", listOf(entity("q1"), entity("q2"), entity("q3")), wrongRepo)

        subscribeCurrentQuestion(vm)
        assertEquals("q1", vm.currentQuestion.value?.id)

        vm.uptimeMillis = { 1000L }
        vm.markKnow()
        vm.markKnow() // 400ms 内第二次被锁
        advanceUntilIdle()

        // 只推进一次：q1 → q2（而非 q3）
        assertEquals(1, vm.currentIndex.value)
        assertEquals("q2", vm.currentQuestion.value?.id)
    }

    @Test
    fun `缺少questionId参数时友好降级为错误态而非崩溃`() = runTest(dispatcher) {
        val wrongRepo = FakeKnowledgeWrongAnswerRepository()
        val examDao = FakeExamQuestionDao(initialEssays = listOf(entity("q1")))
        val repo = buildKnowledgeRepository(FakeKnowledgePointDao(), FakeDataSourceDao(), examDao)
        val vm = QuizPracticeDetailViewModel(
            savedStateHandle = SavedStateHandle(), // 无 questionId
            knowledgeRepository = repo,
            wrongAnswerRepository = wrongRepo,
        )

        advanceUntilIdle()
        assertEquals(false, vm.isLoading.value)
        assertEquals("缺少题目参数，请返回列表重试", vm.error.value)
    }
}
