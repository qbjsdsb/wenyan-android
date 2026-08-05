package com.wenyan.app.feature.aiassistant

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.ai.AiService
import com.wenyan.app.core.ai.AiStreamEvent
import com.wenyan.app.core.ai.PromptTemplates
import com.wenyan.app.core.ai.RagEngine
import com.wenyan.app.core.ai.RagReference
import com.wenyan.app.core.ai.SocraticGuide
import com.wenyan.app.core.ai.SocraticStage
import com.wenyan.app.core.ai.SocraticTutor
import com.wenyan.app.core.ai.WrongAnswerExplanation
import com.wenyan.app.core.ai.network.ChatMessage
import com.wenyan.app.core.ai.recall.AntiRoteMemorization
import com.wenyan.app.core.ai.recall.QuestionType
import com.wenyan.app.core.ai.recall.RecallChecker
import com.wenyan.app.core.ai.recall.RecallResult
import com.wenyan.app.core.ai.recall.RoteCheckResult
import com.wenyan.app.core.data.mapper.ChatMessageMapper
import com.wenyan.app.core.data.repository.ChatRepository
import com.wenyan.app.core.database.entity.ChatMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * AI 助手模块 ViewModel（阶段4完整接入）。
 *
 * 接入 core:ai 的 5 个组件：
 * - [AiService]：LLM API 调用 + 离线降级
 * - [SocraticTutor]：苏格拉底式引导 + "解释我的答案"机制
 * - [RagEngine]：关键词检索（引用可溯源）
 * - [RecallChecker]：三层主动回忆检测
 * - [AntiRoteMemorization]：防死记硬背检测
 *
 * 核心场景：
 * 1. 普通问答：[sendMessage] → RAG 检索 → AI 回答（标注引用来源）
 * 2. 论述题引导：[guideEssayAnswer] → 三阶段苏格拉底式引导
 * 3. 错题解释：[explainWrongAnswer] → "解释我的答案"机制
 * 4. 回忆检测：[checkRecall] → 三层渐进式检测
 * 5. 死记硬背检测：[checkRoteMemorization] → 关联卡片错误率分析
 *
 * NF-PP6 Wave 3.1 持久化改造：
 * - [chatRepository] 持久化对话历史到 chat_conversations + chat_messages 表
 * - 进程被杀重启后,init 调用 loadOrInitCurrent + 加载历史消息到 _uiState
 * - sendMessage/clearMessages 双写(_uiState + chatRepository),保持 UI 响应同时持久化
 * - 双写是过渡方案,后续可统一为 chatRepository.observeMessages 单源(需重构测试时序)
 *
 * @property chatRepository AI 对话仓库(NF-PP6 新增注入)
 */
@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val aiService: AiService,
    private val socraticTutor: SocraticTutor,
    private val ragEngine: RagEngine,
    private val recallChecker: RecallChecker,
    private val antiRoteMemorization: AntiRoteMemorization,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    /**
     * 当前对话 ID(内存缓存,与 chatRepository.currentConversationId DataStore 同步)。
     *
     * null 表示尚未加载或已清空。sendMessage 时如果为 null,会先 createConversation。
     */
    private var currentConversationId: String? = null

    /** v0.9.35 审计修复：对话代次——clearMessages/startNewConversation 时递增，
     * 过期 AI 任务的写入（幽灵回复）据此丢弃 */
    private var conversationGeneration = 0

    /**
     * 当前 AI 任务 Job（v0.9.23 P0-1/P1-3 修复）。
     *
     * - **防重入（P1-3）**：AI 回复中用户又触发学习工具（论述题引导/错题解释/回忆检测）
     *   时，[launchAiTask] 会拒绝新任务，避免多个 AI 协程并发写 _uiState / 重复计费。
     * - **可取消（P0-1）**：清空/新建对话时 [clearMessages]/[startNewConversation]
     *   调用 [Job.cancel]，取消在途任务，避免"发送中清空"竞态导致
     *   `currentConversationId!!` NPE / 用户消息丢失。
     */
    private var aiJob: Job? = null

    /**
     * 统一启动 AI 任务（v0.9.23 新增）。
     *
     * @param showLoading 是否在任务期间置 isLoading=true（false 用于静默检测类任务）
     * @param block 任务体（内部自行 catch 并设置具体 errorMessage；本方法兜底）
     */
    private fun launchAiTask(
        showLoading: Boolean = true,
        block: suspend () -> Unit,
    ) {
        // P1-3 防重入：已有 AI 任务在跑时忽略新任务
        if (aiJob?.isActive == true) {
            Timber.d("launchAiTask skipped: aiJob is active")
            return
        }
        aiJob = viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true) }
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 兜底：block 内部已 catch 的不会到这里；这里是双保险
                Timber.w(e, "AI 任务异常: ${e.message}")
            } finally {
                if (showLoading) _uiState.update { it.copy(isLoading = false) }
                // v0.9.27 修复（并发竞态）：仅当自己是当前 aiJob 时才清空引用。
                // 原实现无条件 aiJob = null：任务 A 停止中（CancellationException 分支的
                // withContext(NonCancellable) DB 写入延迟）isActive 已 false，用户快速发新消息
                // 会启动任务 B 赋给 aiJob，随后 A 的 finally 执行 aiJob = null 抹掉 B 的引用，
                // 导致：停止按钮对新任务失效、可并发启动多个 AI 任务（重复计费）、
                // 取消任务的半截回复写进新会话（数据污染）。
                if (coroutineContext[Job] == aiJob) {
                    aiJob = null
                }
            }
        }
    }

    init {
        checkAvailability()
        // NF-PP6: 加载或初始化当前对话,恢复历史消息
        restoreConversationIfNeeded()
    }

    // ── 普通问答（RAG + AI） ──────────────────────────────────────

    /**
     * 发送用户消息，获取 AI 回答。
     *
     * 流程：
     * 1. 添加用户消息到列表(同时持久化到 chatRepository)
     * 2. RAG 检索相关资料
     * 3. 检查 AI 可用性（离线降级）
     * 4. 构建 RAG prompt → 调用 AiService
     * 5. 添加 AI 回复（标注引用来源,同时持久化）
     *
     * NF-PP6: 双写 — _uiState 更新(UI 响应) + chatRepository.appendMessage(持久化)。
     * 如果 currentConversationId 为 null,先 createConversation。
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        // v0.9.23 P1-3 修复：AI 回复中拒绝新消息。
        // 必须在添加用户消息到 UI 之前检查，否则会出现"消息显示了但 AI 不处理"的错乱。
        if (aiJob?.isActive == true) return
        // v0.8.16 P1-3 修复：限制输入长度，防止用户粘贴超长文本导致：
        // - LLM prompt token 超限（多数模型 context window 8k-32k tokens）
        // - LLM API 报 400/413 错误
        // - 浪费 token 配额（denial-of-wallet）
        // - RAG LIKE 查询超长 SQL 性能下降（RagEngine 已 limit 500，但 prompt 仍会超长）
        // 2000 字约等于 3000-4000 tokens，足以承载完整的考研知识点提问。
        if (text.length > MAX_INPUT_LENGTH) {
            _uiState.update {
                it.copy(errorMessage = "输入过长（${text.length} 字），请控制在 $MAX_INPUT_LENGTH 字以内")
            }
            return
        }

        val userMessage = AiMessage(
            id = nextId(),
            role = AiRole.USER,
            content = text,
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                errorMessage = null,
            )
        }

        // v0.9.23 P0-1/P1-3：统一走 launchAiTask（防重入 + 可取消）
        launchAiTask {
            // v0.9.25 修复：sb 提升到 try 外——停止生成（CancellationException）时需要保留已生成的部分内容
            val sb = StringBuilder()
            try {
                // NF-PP6: 确保当前对话存在,持久化用户消息
                ensureConversation()
                val convId = currentConversationId
                // P0-1 安全空判断：清空/新建对话取消了任务后，这里不应 NPE
                if (convId == null) {
                    _uiState.update {
                        it.copy(errorMessage = "对话创建失败，请重试")
                    }
                    return@launchAiTask
                }
                // v0.9.35 审计修复：多轮上下文须在 appendMessage **之前**提取——
                // 原实现先持久化当前用户消息再 getRecentMessages，本次提问内容
                // 在 history 与 user query 中重复注入（浪费 token 且可能误导模型）。
                val history = chatRepository
                    .getRecentMessages(convId, MAX_HISTORY_MESSAGES)
                    .mapNotNull { entity ->
                        when (entity.role.uppercase()) {
                            "USER" -> ChatMessage(role = "user", content = entity.content)
                            "ASSISTANT" -> ChatMessage(role = "assistant", content = entity.content)
                            else -> null // 跳过 SYSTEM / 其他
                        }
                    }
                    // v0.9.35 审计修复：token 预算截断——20 条长回复（AI 范文 500-800 字）
                    // 可超 8k 上下文模型（如 moonshot-v1-8k）限制触发 400 错误；
                    // 按总字符预算从最早的对话丢弃，保留最近上下文
                    .let { trimHistoryByBudget(it) }
                chatRepository.appendMessage(
                    conversationId = convId,
                    role = AiRole.USER.name,
                    content = text,
                    contentSource = CONTENT_SOURCE_USER_INPUT,
                    stage = null,
                    references = null,
                    contextScreen = null,
                    contextTitle = null,
                    tokensUsed = null,
                )

                // 1. RAG 检索（v0.9.23 P2-1：RagEngine 内部已降级，不会抛异常阻断主流程）
                val ragResult = ragEngine.search(text).first()

                // 2. 检查 AI 可用性
                val available = aiService.isAvailable().first()
                if (!available) {
                    addOfflineMessage(ragResult.references)
                    return@launchAiTask
                }

                // 3. 构建 prompt 并调用 AI（v0.9.24 改流式）
                //    v0.9.24 多轮上下文：取最近 N 条历史注入 LLM（仅 USER/ASSISTANT 消息）
                val prompt = PromptTemplates.buildChatPrompt(text, ragResult.references)
                var tokensUsed: Int? = null

                aiService.chatResultStream(prompt, history).collect { result ->
                    result.onSuccess { event ->
                        when (event) {
                            is AiStreamEvent.Delta -> {
                                sb.append(event.content)
                                // 流式增量：更新 streamingContent 供 UI 逐字显示
                                _uiState.update { it.copy(streamingContent = sb.toString()) }
                            }
                            is AiStreamEvent.Complete -> {
                                tokensUsed = event.usage?.totalTokens
                            }
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(errorMessage = "请求失败：${e.message ?: "未知错误"}")
                        }
                    }
                }

                // 4. 添加 AI 回复（标注引用来源 + token 用量）
                val finalContent = sb.toString().trim()
                if (finalContent.isNotBlank()) {
                    addAssistantMessage(
                        content = finalContent,
                        contentSource = CONTENT_SOURCE_AI,
                        references = if (ragResult.hasResults) ragResult.references else emptyList(),
                        tokensUsed = tokensUsed,
                    )
                }
                // 流式结束，清空 streamingContent
                _uiState.update { it.copy(streamingContent = null) }
            } catch (e: CancellationException) {
                // v0.9.24 停止生成：用户取消时保留已生成的部分内容
                // v0.9.25 修复：此前只清 streamingContent，未保存部分内容到消息
                val partial = sb.toString().trim()
                if (partial.isNotBlank()) {
                    // 协程已取消，suspend 调用需在 NonCancellable 上下文执行才能完成（UI 更新 + 持久化）
                    withContext(NonCancellable) {
                        addAssistantMessage(
                            content = partial,
                            contentSource = CONTENT_SOURCE_AI,
                            references = emptyList(), // 取消时引用可能未就绪，保持简单
                            tokensUsed = null,
                        )
                    }
                }
                _uiState.update { it.copy(streamingContent = null) }
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "请求失败：${e.message ?: "未知错误"}", streamingContent = null)
                }
            }
        }
    }

    // ── 苏格拉底式引导（论述题） ──────────────────────────────────

    /**
     * 苏格拉底式引导论述题作答。
     *
     * 三阶段输出（ANALYZE → SUGGEST → SHOW_SAMPLE），每阶段作为独立消息添加。
     */
    fun guideEssayAnswer(question: String, userAnswer: String) {
        if (question.isBlank() || userAnswer.isBlank()) return
        // v0.9.23 P1-3：AI 回复中拒绝新任务（在添加用户消息前检查）
        if (aiJob?.isActive == true) return

        val userMsg = AiMessage(
            id = nextId(),
            role = AiRole.USER,
            content = "【论述题】$question\n\n我的答案：$userAnswer",
        )
        _uiState.update {
            it.copy(messages = it.messages + userMsg, errorMessage = null)
        }

        // v0.9.23 P0-1/P1-3：统一走 launchAiTask（防重入 + 可取消）
        launchAiTask {
            try {
                // P1-AUDIT-5 修复：持久化用户消息（原实现只更新 UI 未入库，重启后上下文丢失）
                ensureConversation()
                val convId = currentConversationId
                if (convId == null) {
                    _uiState.update { it.copy(errorMessage = "对话创建失败，请重试") }
                    return@launchAiTask
                }
                chatRepository.appendMessage(
                    conversationId = convId,
                    role = AiRole.USER.name,
                    content = "【论述题】$question\n\n我的答案：$userAnswer",
                    contentSource = CONTENT_SOURCE_USER_INPUT,
                    stage = null,
                    references = null,
                    contextScreen = null,
                    contextTitle = null,
                    tokensUsed = null,
                )
                socraticTutor.guideEssayAnswer(question, userAnswer).collect { guide ->
                    addSocraticGuideMessage(guide)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "引导失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    // ── "解释我的答案"机制（错题解释） ────────────────────────────

    /**
     * 用户答错后，AI 分析错误思路并解释正确思路。
     */
    fun explainWrongAnswer(question: String, userAnswer: String, correctAnswer: String) {
        if (question.isBlank() || userAnswer.isBlank() || correctAnswer.isBlank()) return
        // v0.9.23 P1-3：AI 回复中拒绝新任务（在添加用户消息前检查）
        if (aiJob?.isActive == true) return

        val userMsg = AiMessage(
            id = nextId(),
            role = AiRole.USER,
            content = "【错题解释】$question\n\n我的答案：$userAnswer\n正确答案：$correctAnswer",
        )
        _uiState.update {
            it.copy(messages = it.messages + userMsg, errorMessage = null)
        }

        // v0.9.23 P0-1/P1-3：统一走 launchAiTask（防重入 + 可取消）
        launchAiTask {
            try {
                // P1-AUDIT-5 修复：持久化用户消息（原实现只更新 UI 未入库，重启后上下文丢失）
                ensureConversation()
                val convId = currentConversationId
                if (convId == null) {
                    _uiState.update { it.copy(errorMessage = "对话创建失败，请重试") }
                    return@launchAiTask
                }
                chatRepository.appendMessage(
                    conversationId = convId,
                    role = AiRole.USER.name,
                    content = "【错题解释】$question\n\n我的答案：$userAnswer\n正确答案：$correctAnswer",
                    contentSource = CONTENT_SOURCE_USER_INPUT,
                    stage = null,
                    references = null,
                    contextScreen = null,
                    contextTitle = null,
                    tokensUsed = null,
                )
                val explanation: WrongAnswerExplanation = socraticTutor
                    .explainWrongAnswer(question, userAnswer, correctAnswer)
                    .first()

                val content = buildString {
                    append("【错误思路分析】\n").append(explanation.errorAnalysis).append("\n\n")
                    append("【正确思路】\n").append(explanation.correctApproach)
                }
                addAssistantMessage(
                    content = content,
                    contentSource = CONTENT_SOURCE_AI,
                    references = explanation.references,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "解释失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    // ── 主动回忆检测 ──────────────────────────────────────────────

    /**
     * 检测用户主动回忆质量（三层渐进式）。
     *
     * @return 检测结果，UI 可据此展示覆盖率/评分/理由
     */
    suspend fun checkRecall(
        userAnswer: String,
        correctAnswer: String,
        questionType: QuestionType,
    ): RecallResult {
        return recallChecker.checkRecall(userAnswer, correctAnswer, questionType).first()
    }

    /**
     * 主动回忆检测的 UI 入口(P0 v0.7.2 新增)。
     *
     * [checkRecall] 是 suspend 返回 RecallResult,UI 无法直接调用。
     * 此方法内部 launch 协程,结果写入 [AiAssistantUiState.recallResult],
     * Screen 观察状态展示结果。保留 [checkRecall] 原签名以兼容测试。
     */
    fun launchCheckRecall(
        userAnswer: String,
        correctAnswer: String,
        questionType: QuestionType,
    ) {
        if (userAnswer.isBlank() || correctAnswer.isBlank()) return
        // v0.9.23 P1-3：AI 任务进行中拒绝新任务
        if (aiJob?.isActive == true) return
        // v0.9.23 P1-3：统一走 launchAiTask（防重入 + 可取消）
        launchAiTask {
            try {
                val result = checkRecall(userAnswer, correctAnswer, questionType)
                _uiState.update { it.copy(recallResult = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "回忆检测失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    /** 清除回忆检测结果(P0 v0.7.2 新增) */
    fun clearRecallResult() {
        _uiState.update { it.copy(recallResult = null) }
    }

    // ── 防死记硬背检测 ────────────────────────────────────────────

    /**
     * 检测某知识点是否疑似死记硬背。
     *
     * 检测结果通过 [AiAssistantUiState.roteWarning] 暴露给 UI。
     *
     * P1-AUDIT-3 修复：参数名 `cardId` → `pointId`，`relatedCardIds` → `relatedPointIds`，
     * 与 AntiRoteMemorization 和 DAO 命名对齐。
     */
    fun checkRoteMemorization(pointId: String, relatedPointIds: List<String>) {
        // v0.9.23 P1-3：AI 任务进行中拒绝新任务
        if (aiJob?.isActive == true) return
        // v0.9.23 P1-3：统一走 launchAiTask（防重入 + 可取消）。
        // 检测不置 isLoading（静默检测，不干扰对话 UI）。
        launchAiTask(showLoading = false) {
            try {
                val result: RoteCheckResult = antiRoteMemorization
                    .checkRoteMemorization(pointId, relatedPointIds)
                    .first()
                _uiState.update {
                    if (result.isSuspected) {
                        it.copy(roteWarning = result.suggestion)
                    } else {
                        it.copy(roteWarning = null)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 死记硬背检测失败不阻塞主流程，静默忽略
                _uiState.update { it.copy(roteWarning = null) }
            }
        }
    }

    /**
     * v0.9.35 审计修复：多轮上下文 token 预算截断。
     *
     * getRecentMessages 返回旧→新；中文字符 ≈ 1 token，预算 6000 字符 ≈ 6k token，
     * 给 system prompt + RAG 引用 + 本次提问留出安全余量。从最早的消息开始丢弃，
     * 保留最近连续上下文（越近越相关）。
     */
    private fun trimHistoryByBudget(history: List<ChatMessage>): List<ChatMessage> {
        var total = 0
        val kept = ArrayDeque<ChatMessage>()
        for (msg in history.asReversed()) {
            if (total + msg.content.length > HISTORY_TOKEN_BUDGET_CHARS) break
            total += msg.content.length
            kept.addFirst(msg)
        }
        return kept.toList()
    }

    // ── UI 辅助方法 ───────────────────────────────────────────────

    /** 更新输入框文本 */
    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /** 清空对话消息(NF-PP6: 同时删除当前对话 + 清空 DataStore currentId) */
    fun clearMessages() {
        // v0.9.23 P0-1 修复：先取消在途 AI 任务，避免 sendMessage 协程在
        // currentConversationId 置 null 后读到 null（NPE）或把消息写入已删除会话。
        // v0.9.35 审计修复：代次递增，阻止被取消协程的 CancellationException 分支
        // 在 NonCancellable 内把"半截回复"写回已清空的消息列表（幽灵回复）
        aiJob?.cancel()
        conversationGeneration++
        val convId = currentConversationId
        if (convId != null) {
            viewModelScope.launch {
                chatRepository.deleteConversation(convId)
                chatRepository.setCurrentConversation(null)
            }
            currentConversationId = null
        }
        _uiState.update {
            it.copy(
                messages = emptyList(),
                errorMessage = null,
                roteWarning = null,
            )
        }
    }

    /**
     * 开始新对话(NF-PP6 新增,供 Screen "新建对话"按钮调用)。
     *
     * 与 [clearMessages] 区别:
     * - clearMessages:删除当前对话(历史不保留)
     * - startNewConversation:保留当前对话历史,仅切换到新对话(下一次 sendMessage 时创建)
     */
    fun startNewConversation() {
        // v0.9.23 P0-1 修复：先取消在途 AI 任务（同 clearMessages）
        // v0.9.35 审计修复：代次递增（同 clearMessages）
        aiJob?.cancel()
        conversationGeneration++
        currentConversationId = null
        viewModelScope.launch {
            chatRepository.setCurrentConversation(null)
        }
        _uiState.update {
            it.copy(
                messages = emptyList(),
                errorMessage = null,
                roteWarning = null,
            )
        }
    }

    /** 清除错误提示 */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** 清除死记硬背提示 */
    fun clearRoteWarning() {
        _uiState.update { it.copy(roteWarning = null) }
    }

    /** 检查 AI 服务可用性（离线降级支持） */
    fun checkAvailability() {
        viewModelScope.launch {
            try {
                val available = aiService.isAvailable().first()
                _uiState.update { it.copy(isAvailable = available) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isAvailable = false) }
            }
        }
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────

    /**
     * 确保当前对话存在(NF-PP6)。
     *
     * 如果 currentConversationId 为 null,创建新对话并设为当前。
     * 调用方需在协程内调用(内部调用 suspend 方法)。
     */
    private suspend fun ensureConversation() {
        if (currentConversationId != null) return
        val newId = chatRepository.createConversation(
            title = "AI 对话",
            apiConfigId = null,
            model = null,
        )
        currentConversationId = newId
        chatRepository.setCurrentConversation(newId)
    }

    /**
     * 进程重启后恢复对话历史(NF-PP6)。
     *
     * - loadOrInitCurrent 返回非 null → 加载该对话消息到 _uiState
     * - 返回 null → 无历史,保持 _uiState 空(等用户首次 sendMessage)
     */
    private fun restoreConversationIfNeeded() {
        viewModelScope.launch {
            try {
                val convId = chatRepository.loadOrInitCurrent() ?: return@launch
                // v0.9.23 P0-2 修复：若用户已通过 sendMessage 创建了新会话
                // （currentConversationId 非 null），不覆盖用户当前会话，
                // 避免恢复完成时把旧历史灌入并让用户刚发的消息从 UI 消失。
                if (currentConversationId != null) return@launch
                currentConversationId = convId
                val messages = chatRepository.observeMessages(convId).first()
                if (messages.isNotEmpty() && currentConversationId == convId) {
                    _uiState.update { it.copy(messages = messages.map { it.toAiMessage() }) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 恢复失败不阻塞主流程,用户可正常发起新对话
            }
        }
    }

    /** 添加 AI 助手消息(NF-PP6: 同时持久化到 chatRepository) */
    private suspend fun addAssistantMessage(
        content: String,
        contentSource: String,
        references: List<RagReference> = emptyList(),
        stage: SocraticStage? = null,
        tokensUsed: Int? = null,
    ) {
        // v0.9.35 审计修复：代次快照——若清空/新建对话发生（代次递增），
        // 本次写入视为过期丢弃（幽灵回复防护）；update 内比较保证与
        // clearMessages 的递增在主线程串行一致
        val generation = conversationGeneration
        val msg = AiMessage(
            id = nextId(),
            role = AiRole.ASSISTANT,
            content = content,
            contentSource = contentSource,
            references = references,
            stage = stage,
            tokensUsed = tokensUsed,
        )
        _uiState.update { state ->
            if (generation != conversationGeneration) state
            else state.copy(messages = state.messages + msg)
        }

        // NF-PP6: 持久化 AI 消息(currentConversationId 应已由 sendMessage 的 ensureConversation 设置)
        val convId = currentConversationId
        if (convId != null && generation == conversationGeneration) {
            chatRepository.appendMessage(
                conversationId = convId,
                role = AiRole.ASSISTANT.name,
                content = content,
                contentSource = contentSource,
                stage = stage?.name,
                references = references,
                contextScreen = null,
                contextTitle = null,
                tokensUsed = tokensUsed,
            )
        }
    }

    /** 添加苏格拉底引导消息（按阶段标注,NF-PP6: 持久化通过 addAssistantMessage） */
    private suspend fun addSocraticGuideMessage(guide: SocraticGuide) {
        val prefix = when (guide.stage) {
            SocraticStage.ANALYZE -> "【论证分析】"
            SocraticStage.SUGGEST -> "【改进建议】"
            SocraticStage.SHOW_SAMPLE -> if (guide.isSampleEssay) "【参考范文，非标准答案】" else "【范文】"
        }
        addAssistantMessage(
            content = prefix + "\n" + guide.content,
            contentSource = guide.contentSource,
            stage = guide.stage,
        )
    }

    /** 离线降级：AI 不可用时显示友好提示（附 RAG 引用供参考,NF-PP6: 持久化通过 addAssistantMessage） */
    private suspend fun addOfflineMessage(references: List<RagReference>) {
        val content = buildString {
            append("AI 服务当前不可用，请检查网络连接或 API 配置。\n")
            if (references.isNotEmpty()) {
                append("\n以下是从资料库检索到的相关内容，供参考：\n")
                references.forEachIndexed { index, ref ->
                    append("${index + 1}. 《${ref.sourceFile}》P${ref.sourcePage}\n")
                    append("   ${ref.excerpt.take(100)}\n")
                }
            }
        }
        addAssistantMessage(
            content = content,
            contentSource = CONTENT_SOURCE_AI,
            references = references,
        )
    }

    /**
     * 停止生成（v0.9.24 新增）。
     *
     * 取消当前 AI 流式任务：launchAiTask 的 Job.cancel() 会中断
     * chatResultStream 的 collect（底层 OkHttp call 被取消），
     * 已生成的部分内容在 sendMessage 的 CancellationException 分支保留为消息。
     */
    fun stopGeneration() {
        aiJob?.cancel()
    }

    /**
     * 生成唯一消息 ID。
     *
     * P0-T1b 修正：原用 companion object var messageCounter 生成 "timestamp-counter" ID，
     * 该 counter 跨 ViewModel 实例共享，导致：
     * 1) 单测中多个 VM 实例 counter 持续累加，断言特定 ID 时抖动
     * 2) 进程被杀重启后 counter 归零，可能与持久化消息 ID 冲突
     * 改用 UUID 保证全局唯一性，无需可变状态。
     */
    private fun nextId(): String = java.util.UUID.randomUUID().toString()

    companion object {
        /** AI 生成内容来源标识 */
        private const val CONTENT_SOURCE_AI = "AI_GENERATED"

        /** 用户输入内容来源标识(NF-PP6) */
        private const val CONTENT_SOURCE_USER_INPUT = "USER_INPUT"

        /**
         * 多轮上下文最大注入条数（v0.9.24 新增）。
         *
         * 取最近 N 条历史消息注入 LLM messages 数组（system + history + 当前 query）。
         * 20 条约等于 10 轮对话，足以承载连续追问场景；配合
         * 单条 MAX_INPUT_LENGTH=2000 字约束，总 token 在可控范围。
         */
        private const val MAX_HISTORY_MESSAGES = 20
        /** 多轮上下文总字符预算（≈6k token，中文 1 字≈1 token；v0.9.35 审计新增） */
        private const val HISTORY_TOKEN_BUDGET_CHARS = 6000

        /**
         * 用户输入最大长度（v0.8.16 P1-3）。
         *
         * 限制原因：
         * - 多数 LLM context window 8k-32k tokens，超长输入触发 400/413
         * - 防止 denial-of-wallet（用户粘贴大文本耗尽 token 配额）
         * - RagEngine 已限制 LIKE 查询 500 字，但 prompt 仍会拼接超长 user 内容
         *
         * 2000 中文字约等于 3000-4000 tokens，足够承载完整的考研知识点提问。
         */
        private const val MAX_INPUT_LENGTH = 2000
    }
}

/**
 * 将 [ChatMessageEntity] 转换为 [AiMessage](NF-PP6 Wave 3.1)。
 *
 * 用于进程重启后从 chatRepository 恢复历史消息到 _uiState。
 * referencesJson 通过 [ChatMessageMapper.deserializeReferences] 反序列化。
 */
private fun ChatMessageEntity.toAiMessage(): AiMessage {
    val role = when (role.uppercase()) {
        "USER" -> AiRole.USER
        else -> AiRole.ASSISTANT // ASSISTANT / SYSTEM 都映射为 ASSISTANT(UI 只区分两类)
    }
    val stage = stage?.let { runCatching { SocraticStage.valueOf(it) }.getOrNull() }
    return AiMessage(
        id = id,
        role = role,
        content = content,
        contentSource = contentSource,
        references = ChatMessageMapper.deserializeReferences(referencesJson),
        stage = stage,
        tokensUsed = tokensUsed,
    )
}

/**
 * AI 助手 UI 状态。
 *
 * @param messages 对话消息列表
 * @param inputText 输入框文本
 * @param isLoading 是否正在加载
 * @param isAvailable AI 服务是否可用（离线降级）
 * @param errorMessage 错误提示（可清除）
 * @param roteWarning 死记硬背提示（可清除）
 * @param recallResult 主动回忆检测结果(P0 v0.7.2 新增,可清除)
 * @param streamingContent 流式输出中的增量文本（v0.9.24 新增，非 null 表示 AI 正在逐字回复）
 */
data class AiAssistantUiState(
    val messages: List<AiMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isAvailable: Boolean = true,
    val errorMessage: String? = null,
    val roteWarning: String? = null,
    val recallResult: RecallResult? = null,
    val streamingContent: String? = null,
)

/**
 * 对话消息。
 *
 * @param id 唯一标识
 * @param role 消息角色（用户/助手）
 * @param content 消息内容
 * @param contentSource 内容来源（AI_GENERATED / TEXTBOOK_NATIVE / TEXTBOOK_OCR），仅助手消息有
 * @param references RAG 引用来源列表（可溯源），仅助手消息有
 * @param stage 苏格拉底引导阶段（仅苏格拉底引导消息有）
 * @param tokensUsed AI 回复消耗 token 数（v0.9.24 新增，仅助手消息有，可空）
 */
@Immutable
data class AiMessage(
    val id: String,
    val role: AiRole,
    val content: String,
    val contentSource: String? = null,
    val references: List<RagReference> = emptyList(),
    val stage: SocraticStage? = null,
    val tokensUsed: Int? = null,
)

/** 消息角色 */
enum class AiRole {
    USER,
    ASSISTANT,
}
