package com.wenyan.app.feature.aiassistant

import com.wenyan.app.core.ai.RagEngine
import com.wenyan.app.core.ai.SocraticTutor
import com.wenyan.app.core.ai.SocraticTutorImpl
import com.wenyan.app.core.ai.recall.AntiRoteMemorization
import com.wenyan.app.core.ai.recall.QuestionType
import com.wenyan.app.core.ai.recall.RecallChecker
import com.wenyan.app.core.ai.recall.RecallLevel
import com.wenyan.app.core.ai.recall.RecallRating
import com.wenyan.app.core.database.entity.ChatConversationEntity
import com.wenyan.app.core.database.entity.ChatMessageEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.ReviewLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
 * [AiAssistantViewModel] 单元测试。
 *
 * 验证：
 * - sendMessage 正常流程 / 离线降级 / RAG 引用 / 异常处理
 * - guideEssayAnswer 三阶段苏格拉底式引导
 * - explainWrongAnswer 错题解释
 * - checkRecall 主动回忆检测
 * - checkRoteMemorization 死记硬背检测
 * - UI 辅助方法（updateInput / clearMessages / clearError / clearRoteWarning）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiAssistantViewModelTest {

    private lateinit var aiService: FakeAiService
    private lateinit var ragEngine: RagEngine
    private lateinit var socraticTutor: SocraticTutor
    private lateinit var recallChecker: RecallChecker
    private lateinit var antiRoteMemorization: AntiRoteMemorization
    private lateinit var chatRepository: FakeChatRepository
    private lateinit var viewModel: AiAssistantViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        aiService = FakeAiService()
        val dao = FakeKnowledgePointDao(listOf(sampleEntity()))
        ragEngine = RagEngine(dao)
        socraticTutor = SocraticTutorImpl(ragEngine, aiService)
        recallChecker = RecallChecker(aiService)
        antiRoteMemorization = AntiRoteMemorization(FakeReviewLogDao(emptyList()))
        // NF-PP6: 默认无历史,init 的 loadOrInitCurrent 返回 null,不影响现有测试
        chatRepository = FakeChatRepository()

        viewModel = AiAssistantViewModel(
            aiService = aiService,
            socraticTutor = socraticTutor,
            ragEngine = ragEngine,
            recallChecker = recallChecker,
            antiRoteMemorization = antiRoteMemorization,
            chatRepository = chatRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── sendMessage 测试 ──────────────────────────────────────────

    @Test
    fun `sendMessage 空白文本不发送`() = runTest {
        val initialSize = viewModel.uiState.value.messages.size
        viewModel.sendMessage("   ")
        // 空白文本不应添加任何消息
        assertEquals(initialSize, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `sendMessage 正常流程添加用户消息和AI回复`() = runTest {
        aiService.response = "AI 回复内容"
        viewModel.sendMessage("苏轼")

        // UnconfinedTestDispatcher 同步执行协程，直接验证最终状态
        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(AiRole.USER, state.messages[0].role)
        assertEquals("苏轼", state.messages[0].content)
        assertEquals(AiRole.ASSISTANT, state.messages[1].role)
        assertEquals("AI 回复内容", state.messages[1].content)
        assertEquals("AI_GENERATED", state.messages[1].contentSource)
        assertFalse(state.isLoading)
    }

    @Test
    fun `sendMessage RAG有结果时AI回复包含引用`() = runTest {
        aiService.response = "AI 回复"
        viewModel.sendMessage("苏轼")

        val state = viewModel.uiState.value
        val assistantMsg = state.messages.last { it.role == AiRole.ASSISTANT }
        assertTrue("RAG 有结果时引用列表不应为空", assistantMsg.references.isNotEmpty())
        assertEquals("中国文学史", assistantMsg.references[0].sourceFile)
    }

    @Test
    fun `sendMessage RAG无结果时AI回复无引用`() = runTest {
        // 用空搜索结果的 DAO 重新构造
        val emptyDao = FakeKnowledgePointDao(emptyList())
        val emptyRagEngine = RagEngine(emptyDao)
        val vm = AiAssistantViewModel(
            aiService = aiService,
            socraticTutor = SocraticTutorImpl(emptyRagEngine, aiService),
            ragEngine = emptyRagEngine,
            recallChecker = recallChecker,
            antiRoteMemorization = antiRoteMemorization,
            chatRepository = FakeChatRepository(),
        )

        aiService.response = "AI 回复"
        vm.sendMessage("量子力学")

        val state = vm.uiState.value
        val assistantMsg = state.messages.last { it.role == AiRole.ASSISTANT }
        assertTrue("RAG 无结果时引用列表应为空", assistantMsg.references.isEmpty())
    }

    @Test
    fun `sendMessage 离线降级显示友好提示`() = runTest {
        aiService.available = false
        viewModel.sendMessage("苏轼")

        val state = viewModel.uiState.value
        val assistantMsg = state.messages.last { it.role == AiRole.ASSISTANT }
        assertTrue("离线提示应包含不可用信息", assistantMsg.content.contains("不可用"))
    }

    @Test
    fun `sendMessage 离线降级RAG有结果时提示包含引用内容`() = runTest {
        aiService.available = false
        viewModel.sendMessage("苏轼")

        val state = viewModel.uiState.value
        val assistantMsg = state.messages.last { it.role == AiRole.ASSISTANT }
        assertTrue("离线提示应包含资料库检索内容", assistantMsg.content.contains("资料库"))
        assertTrue("离线时引用列表不应为空", assistantMsg.references.isNotEmpty())
    }

    @Test
    fun `sendMessage 异常时设置errorMessage`() = runTest {
        aiService.throwException = RuntimeException("网络错误")
        viewModel.sendMessage("苏轼")

        val state = viewModel.uiState.value
        assertNotNull("异常时应设置 errorMessage", state.errorMessage)
        assertTrue("errorMessage 应包含失败信息", state.errorMessage!!.contains("请求失败"))
        assertFalse("异常时 isLoading 应为 false", state.isLoading)
    }

    @Test
    fun `sendMessage 后输入框清空`() = runTest {
        viewModel.updateInput("测试内容")
        assertEquals("测试内容", viewModel.uiState.value.inputText)

        viewModel.sendMessage("测试内容")
        assertEquals("发送后输入框应清空", "", viewModel.uiState.value.inputText)
    }

    // ── guideEssayAnswer 测试 ─────────────────────────────────────

    @Test
    fun `guideEssayAnswer 空白输入不发送`() = runTest {
        val initialSize = viewModel.uiState.value.messages.size
        viewModel.guideEssayAnswer("", "答案")
        assertEquals(initialSize, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `guideEssayAnswer 正常答案输出三阶段消息`() = runTest {
        aiService.response = "AI 分析结果"
        val longAnswer = "苏轼是北宋著名的文学家，他在诗、词、文、书、画等方面都有很高的成就。" +
            "他的词开创了豪放派，对后世文学产生了深远的影响。"

        viewModel.guideEssayAnswer("苏轼", longAnswer)

        val state = viewModel.uiState.value
        // 1 条用户消息 + 3 条 AI 引导消息（ANALYZE/SUGGEST/SHOW_SAMPLE）
        val assistantMsgs = state.messages.filter { it.role == AiRole.ASSISTANT }
        assertEquals("应输出 3 个阶段的引导消息", 3, assistantMsgs.size)
        assertTrue("第一条应包含【论证分析】", assistantMsgs[0].content.contains("【论证分析】"))
        assertTrue("第二条应包含【改进建议】", assistantMsgs[1].content.contains("【改进建议】"))
        assertTrue("第三条应包含【参考范文】", assistantMsgs[2].content.contains("【参考范文"))
        assertFalse("完成后 isLoading 应为 false", state.isLoading)
    }

    @Test
    fun `guideEssayAnswer 答案过短时只输出一条提示`() = runTest {
        viewModel.guideEssayAnswer("苏轼", "太短")

        val state = viewModel.uiState.value
        val assistantMsgs = state.messages.filter { it.role == AiRole.ASSISTANT }
        assertEquals("答案过短时应只输出 1 条提示", 1, assistantMsgs.size)
        assertTrue("提示应包含不足或回顾", assistantMsgs[0].content.contains("不足") || assistantMsgs[0].content.contains("回顾"))
    }

    // ── explainWrongAnswer 测试 ───────────────────────────────────

    @Test
    fun `explainWrongAnswer 空白输入不发送`() = runTest {
        val initialSize = viewModel.uiState.value.messages.size
        viewModel.explainWrongAnswer("题目", "", "正确答案")
        assertEquals(initialSize, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `explainWrongAnswer 输出错题解释消息`() = runTest {
        aiService.response = "AI 分析结果"
        viewModel.explainWrongAnswer("苏轼", "错误答案", "正确答案")

        val state = viewModel.uiState.value
        val assistantMsg = state.messages.last { it.role == AiRole.ASSISTANT }
        assertTrue("应包含错误思路分析", assistantMsg.content.contains("【错误思路分析】"))
        assertTrue("应包含正确思路", assistantMsg.content.contains("【正确思路】"))
        assertFalse("完成后 isLoading 应为 false", state.isLoading)
    }

    // ── checkRecall 测试 ──────────────────────────────────────────

    @Test
    fun `checkRecall 返回L1关键词检测结果`() = runTest {
        val result = viewModel.checkRecall(
            userAnswer = "苏轼是北宋文学家",
            correctAnswer = "苏轼，北宋文学家，豪放派词人",
            questionType = QuestionType.TERM_EXPLANATION,
        )
        assertEquals(RecallLevel.L1, result.level)
        assertTrue("覆盖率应 > 0", result.coverage > 0f)
    }

    @Test
    fun `checkRecall 返回L2语义相似度结果`() = runTest {
        val result = viewModel.checkRecall(
            userAnswer = "苏轼是北宋著名的文学家豪放派词人",
            correctAnswer = "苏轼是北宋著名的文学家豪放派词人",
            questionType = QuestionType.ESSAY,
        )
        assertEquals(RecallLevel.L2, result.level)
        assertEquals("完全相同时相似度应为 1", 1f, result.coverage, 0.001f)
        assertEquals(RecallRating.EASY, result.rating)
    }

    // ── checkRoteMemorization 测试 ────────────────────────────────

    @Test
    fun `checkRoteMemorization 疑似死记硬背时设置roteWarning`() = runTest {
        // 构造疑似死记硬背的复习历史
        val dao = FakeReviewLogDao(
            listOf(
                ReviewLogEntity(
                    id = "log1", pointId = "card_001", rating = "GOOD",
                    elapsedDays = 1, scheduledDays = 1, state = "REVIEW",
                    stability = 10f, difficulty = 5f, reps = 5, createdAt = 1000L,
                ),
                ReviewLogEntity(
                    id = "log2", pointId = "card_001", rating = "GOOD",
                    elapsedDays = 1, scheduledDays = 1, state = "REVIEW",
                    stability = 10f, difficulty = 5f, reps = 5, createdAt = 2000L,
                ),
                ReviewLogEntity(
                    id = "log3", pointId = "card_001", rating = "GOOD",
                    elapsedDays = 1, scheduledDays = 1, state = "REVIEW",
                    stability = 10f, difficulty = 5f, reps = 5, createdAt = 3000L,
                ),
                ReviewLogEntity(
                    id = "log4", pointId = "card_001", rating = "GOOD",
                    elapsedDays = 1, scheduledDays = 1, state = "REVIEW",
                    stability = 10f, difficulty = 5f, reps = 5, createdAt = 4000L,
                ),
                ReviewLogEntity(
                    id = "log5", pointId = "card_001", rating = "GOOD",
                    elapsedDays = 1, scheduledDays = 1, state = "REVIEW",
                    stability = 10f, difficulty = 5f, reps = 5, createdAt = 5000L,
                ),
                // 关联卡片频繁出错
                ReviewLogEntity(
                    id = "log6", pointId = "card_002", rating = "AGAIN",
                    elapsedDays = 1, scheduledDays = 1, state = "REVIEW",
                    stability = 1f, difficulty = 8f, reps = 1, createdAt = 6000L,
                ),
                ReviewLogEntity(
                    id = "log7", pointId = "card_002", rating = "AGAIN",
                    elapsedDays = 1, scheduledDays = 1, state = "REVIEW",
                    stability = 1f, difficulty = 8f, reps = 1, createdAt = 7000L,
                ),
                ReviewLogEntity(
                    id = "log8", pointId = "card_002", rating = "AGAIN",
                    elapsedDays = 1, scheduledDays = 1, state = "REVIEW",
                    stability = 1f, difficulty = 8f, reps = 1, createdAt = 8000L,
                ),
            ),
        )
        val roteAnti = AntiRoteMemorization(dao)
        val vm = AiAssistantViewModel(
            aiService = aiService,
            socraticTutor = socraticTutor,
            ragEngine = ragEngine,
            recallChecker = recallChecker,
            antiRoteMemorization = roteAnti,
            chatRepository = FakeChatRepository(),
        )

        vm.checkRoteMemorization("card_001", listOf("card_002"))

        // UnconfinedTestDispatcher 同步执行协程，直接验证最终状态
        assertNotNull("疑似死记硬背时应设置 roteWarning", vm.uiState.value.roteWarning)
        assertTrue("roteWarning 应包含建议", vm.uiState.value.roteWarning!!.isNotEmpty())
    }

    @Test
    fun `checkRoteMemorization 正常时roteWarning为null`() = runTest {
        // 空复习历史，不会判定为死记硬背
        viewModel.checkRoteMemorization("card_001", listOf("card_002"))

        // UnconfinedTestDispatcher 同步执行协程，直接验证最终状态
        assertNull("正常时 roteWarning 应为 null", viewModel.uiState.value.roteWarning)
    }

    // ── UI 辅助方法测试 ───────────────────────────────────────────

    @Test
    fun `updateInput 更新输入框文本`() {
        viewModel.updateInput("新内容")
        assertEquals("新内容", viewModel.uiState.value.inputText)
    }

    @Test
    fun `clearMessages 清空消息和提示`() = runTest {
        aiService.response = "AI 回复"
        viewModel.sendMessage("测试")
        assertTrue("发送后应有消息", viewModel.uiState.value.messages.isNotEmpty())

        viewModel.clearMessages()
        assertTrue("清空后消息应为空", viewModel.uiState.value.messages.isEmpty())
        assertNull("清空后 errorMessage 应为 null", viewModel.uiState.value.errorMessage)
        assertNull("清空后 roteWarning 应为 null", viewModel.uiState.value.roteWarning)
    }

    @Test
    fun `clearError 清除错误提示`() = runTest {
        // 通过异常设置 errorMessage
        aiService.throwException = RuntimeException("错误")
        viewModel.sendMessage("测试")
        assertNotNull("异常后应有 errorMessage", viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull("clearError 后 errorMessage 应为 null", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearRoteWarning 清除死记硬背提示`() {
        viewModel.clearRoteWarning()
        assertNull("clearRoteWarning 后 roteWarning 应为 null", viewModel.uiState.value.roteWarning)
    }

    // ── NF-PP6 持久化测试(Wave 3.1 新增) ─────────────────────────

    /**
     * 场景 1:sendMessage 后 chatRepository 持久化了用户消息 + AI 消息。
     *
     * 验证双写:_uiState 更新(UI 响应) + chatRepository.appendMessage(持久化)。
     * appendedMessages 应有 2 条:USER + ASSISTANT,顺序与内容正确。
     */
    @Test
    fun `sendMessage 后 chatRepository 持久化用户和AI消息`() = runTest {
        aiService.response = "AI 持久化回复"
        viewModel.sendMessage("测试持久化")

        // _uiState 应有 2 条(双写的 UI 侧)
        assertEquals(2, viewModel.uiState.value.messages.size)

        // chatRepository 应持久化 2 条(USER + ASSISTANT)
        assertEquals("应持久化 2 条消息", 2, chatRepository.appendedMessages.size)
        assertEquals("USER", chatRepository.appendedMessages[0].role)
        assertEquals("测试持久化", chatRepository.appendedMessages[0].content)
        assertEquals("USER_INPUT", chatRepository.appendedMessages[0].contentSource)
        assertEquals("ASSISTANT", chatRepository.appendedMessages[1].role)
        assertEquals("AI 持久化回复", chatRepository.appendedMessages[1].content)
        assertEquals("AI_GENERATED", chatRepository.appendedMessages[1].contentSource)
    }

    /**
     * 场景 2:clearMessages 后 chatRepository.deleteConversation 被调用。
     *
     * 验证 clearMessages 双写:_uiState 清空 + chatRepository.deleteConversation + setCurrentConversation(null)。
     */
    @Test
    fun `clearMessages 后 chatRepository 删除当前对话`() = runTest {
        aiService.response = "AI 回复"
        viewModel.sendMessage("先发一条")
        // sendMessage 后 currentConversationId 应非 null(ensureConversation 已创建)
        assertTrue("应有 currentId", chatRepository.currentId != null)
        val convId = chatRepository.currentId!!

        viewModel.clearMessages()

        // _uiState 清空
        assertTrue("清空后消息应为空", viewModel.uiState.value.messages.isEmpty())
        // chatRepository.deleteConversation 被调用
        assertTrue("deleteConversation 应被调用", convId in chatRepository.deletedConversationIds)
        // setCurrentConversation(null) 被调用
        assertTrue("setCurrentConversation(null) 应被调用", null in chatRepository.setCurrentCalls)
    }

    /**
     * 场景 3:进程重启(新 ViewModel 实例)后 init 加载历史消息到 _uiState。
     *
     * 验证 restoreConversationIfNeeded:
     * - FakeChatRepository 预设 1 个对话 + 2 条历史消息
     * - 新建 ViewModel 时 init 触发 loadOrInitCurrent + observeMessages
     * - _uiState.messages 应恢复 2 条历史消息
     */
    @Test
    fun `进程重启后 init 恢复历史消息到 uiState`() = runTest {
        val convId = "conv_history_1"
        val initialConv = ChatConversationEntity(
            id = convId,
            title = "历史对话",
            apiConfigId = null,
            model = null,
            messageCount = 2,
            createdAt = 1000L,
            updatedAt = 2000L,
        )
        val initialMsgs = listOf(
            ChatMessageEntity(
                id = "msg_1", conversationId = convId, role = "USER",
                content = "历史问题", contentSource = "USER_INPUT", stage = null,
                referencesJson = null, contextScreen = null, contextTitle = null,
                tokensUsed = null, createdAt = 1000L,
            ),
            ChatMessageEntity(
                id = "msg_2", conversationId = convId, role = "ASSISTANT",
                content = "历史回答", contentSource = "AI_GENERATED", stage = null,
                referencesJson = null, contextScreen = null, contextTitle = null,
                tokensUsed = null, createdAt = 1500L,
            ),
        )
        val historyRepo = FakeChatRepository(
            initialConversations = listOf(initialConv),
            initialMessages = initialMsgs,
        )

        // 模拟进程重启:新建 ViewModel,init 应恢复历史
        val restartedVm = AiAssistantViewModel(
            aiService = aiService,
            socraticTutor = socraticTutor,
            ragEngine = ragEngine,
            recallChecker = recallChecker,
            antiRoteMemorization = antiRoteMemorization,
            chatRepository = historyRepo,
        )

        // UnconfinedTestDispatcher 同步执行 init 协程,直接验证
        val state = restartedVm.uiState.value
        assertEquals("应恢复 2 条历史消息", 2, state.messages.size)
        assertEquals(AiRole.USER, state.messages[0].role)
        assertEquals("历史问题", state.messages[0].content)
        assertEquals(AiRole.ASSISTANT, state.messages[1].role)
        assertEquals("历史回答", state.messages[1].content)
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    private fun sampleEntity(
        id: String = "kp1",
        title: String = "苏轼",
        coreConclusion: String = "北宋文学家",
    ) = KnowledgePointEntity(
        id = id,
        chapterId = "ch1",
        title = title,
        summary = null,
        coreConclusion = coreConclusion,
        fullContent = "",
        multiPerspectives = null,
        relatedIds = null,
        contrastIds = null,
        extensionIds = null,
        examRecords = null,
        examFrequency = "MEDIUM",
        termTemplate = null,
        tags = null,
        difficulty = 3,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        contentSource = "TEXTBOOK_NATIVE",
        ocrStatus = "VERIFIED",
        sourceFile = "中国文学史",
        sourcePage = 100,
        studyText = null,
    )
}
