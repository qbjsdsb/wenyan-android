package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.PracticeAttemptEntity
import com.wenyan.app.core.data.repository.PracticeAttemptStore
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private fun entity(id: String, year: Int = 2020, reviewed: Boolean = false) = ExamQuestionEntity(
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
        contentStatus = if (reviewed) "REVIEWED" else "LEGACY_UNVERIFIED",
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
        attemptStore: FakePracticeAttemptStore = FakePracticeAttemptStore(),
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
            practiceAttemptStore = attemptStore,
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
            practiceAttemptStore = FakePracticeAttemptStore(),
        )

        advanceUntilIdle()
        assertEquals(false, vm.isLoading.value)
        assertEquals("缺少题目参数，请返回列表重试", vm.error.value)
    }

    @Test
    fun `空白作答不能揭示且未审校框架始终不可见`() = runTest(dispatcher) {
        val vm = createViewModel("q1", listOf(entity("q1")), FakeKnowledgeWrongAnswerRepository())
        subscribeCurrentQuestion(vm)
        vm.revealAnswer()
        assertFalse(vm.showAnswer.value)
        vm.updateOutline("我的提纲")
        vm.revealAnswer()
        assertFalse(vm.showAnswer.value)
        assertTrue(vm.message.value.orEmpty().contains("尚未人工审校"))
    }

    @Test
    fun `已审校题先作答再揭示并持久化进程恢复字段`() = runTest(dispatcher) {
        val store = FakePracticeAttemptStore()
        val vm = createViewModel("q1", listOf(entity("q1", reviewed = true)), FakeKnowledgeWrongAnswerRepository(), store)
        subscribeCurrentQuestion(vm)
        vm.updateKeywords("关键词")
        vm.updateOutline("我的提纲")
        vm.revealAnswer()
        advanceUntilIdle()
        assertTrue(vm.showAnswer.value)
        assertEquals(1, store.values.size)
        assertEquals("我的提纲", store.values.values.single().outline)
        assertTrue(store.values.values.single().revealedAt != null)
    }

    @Test
    fun `SavedStateHandle恢复未完成作答而不自动揭示`() = runTest(dispatcher) {
        val examDao = FakeExamQuestionDao(initialEssays = listOf(entity("q1", reviewed = true)))
        val handle = SavedStateHandle(
            mapOf(
                "questionId" to "q1",
                "practice_keywords" to "已保存关键词",
                "practice_outline" to "已保存提纲",
                "practice_attempt_stage" to "SAVED",
                "practice_show_answer" to false,
            ),
        )
        val vm = QuizPracticeDetailViewModel(
            savedStateHandle = handle,
            knowledgeRepository = buildKnowledgeRepository(FakeKnowledgePointDao(), FakeDataSourceDao(), examDao),
            wrongAnswerRepository = FakeKnowledgeWrongAnswerRepository(),
            practiceAttemptStore = FakePracticeAttemptStore(),
        )
        subscribeCurrentQuestion(vm)
        assertEquals("已保存关键词", vm.keywords.value)
        assertEquals("已保存提纲", vm.outline.value)
        assertEquals("SAVED", vm.attemptStage.value)
        assertFalse(vm.showAnswer.value)
    }
}

private class FakePracticeAttemptStore : PracticeAttemptStore {
    val values = linkedMapOf<String, PracticeAttemptEntity>()
    private val flow = kotlinx.coroutines.flow.MutableStateFlow<List<PracticeAttemptEntity>>(emptyList())
    override suspend fun save(attempt: PracticeAttemptEntity) {
        values[attempt.id] = attempt
        flow.value = values.values.toList()
    }
    override suspend fun get(id: String): PracticeAttemptEntity? = values[id]
    override fun observeSession(sessionId: String) = flow
}
