package com.wenyan.app.feature.aiassistant

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.ai.AiService
import com.wenyan.app.core.ai.PromptTemplates
import com.wenyan.app.core.ai.RagEngine
import com.wenyan.app.core.ai.RagReference
import com.wenyan.app.core.ai.SocraticGuide
import com.wenyan.app.core.ai.SocraticStage
import com.wenyan.app.core.ai.SocraticTutor
import com.wenyan.app.core.ai.WrongAnswerExplanation
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // NF-PP6: 确保当前对话存在,持久化用户消息
                ensureConversation()
                chatRepository.appendMessage(
                    conversationId = currentConversationId!!,
                    role = AiRole.USER.name,
                    content = text,
                    contentSource = CONTENT_SOURCE_USER_INPUT,
                    stage = null,
                    references = null,
                    contextScreen = null,
                    contextTitle = null,
                    tokensUsed = null,
                )

                // 1. RAG 检索
                val ragResult = ragEngine.search(text).first()

                // 2. 检查 AI 可用性
                val available = aiService.isAvailable().first()
                if (!available) {
                    addOfflineMessage(ragResult.references)
                    return@launch
                }

                // 3. 构建 prompt 并调用 AI（P1-5 改用 chatResult 区分成功/失败）
                val prompt = PromptTemplates.buildChatPrompt(text, ragResult.references)
                val result = aiService.chatResult(prompt).first()

                if (result.isFailure) {
                    _uiState.update {
                        it.copy(errorMessage = "请求失败：${result.exceptionOrNull()?.message ?: "未知错误"}")
                    }
                    return@launch
                }

                // 4. 添加 AI 回复（标注引用来源）
                addAssistantMessage(
                    content = result.getOrThrow(),
                    contentSource = CONTENT_SOURCE_AI,
                    references = if (ragResult.hasResults) ragResult.references else emptyList(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "请求失败：${e.message ?: "未知错误"}")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
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

        val userMsg = AiMessage(
            id = nextId(),
            role = AiRole.USER,
            content = "【论述题】$question\n\n我的答案：$userAnswer",
        )
        _uiState.update {
            it.copy(messages = it.messages + userMsg, errorMessage = null)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // P1-AUDIT-5 修复：持久化用户消息（原实现只更新 UI 未入库，重启后上下文丢失）
                ensureConversation()
                chatRepository.appendMessage(
                    conversationId = currentConversationId!!,
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
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── "解释我的答案"机制（错题解释） ────────────────────────────

    /**
     * 用户答错后，AI 分析错误思路并解释正确思路。
     */
    fun explainWrongAnswer(question: String, userAnswer: String, correctAnswer: String) {
        if (question.isBlank() || userAnswer.isBlank() || correctAnswer.isBlank()) return

        val userMsg = AiMessage(
            id = nextId(),
            role = AiRole.USER,
            content = "【错题解释】$question\n\n我的答案：$userAnswer\n正确答案：$correctAnswer",
        )
        _uiState.update {
            it.copy(messages = it.messages + userMsg, errorMessage = null)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // P1-AUDIT-5 修复：持久化用户消息（原实现只更新 UI 未入库，重启后上下文丢失）
                ensureConversation()
                chatRepository.appendMessage(
                    conversationId = currentConversationId!!,
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
            } finally {
                _uiState.update { it.copy(isLoading = false) }
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = checkRecall(userAnswer, correctAnswer, questionType)
                _uiState.update { it.copy(recallResult = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "回忆检测失败：${e.message ?: "未知错误"}")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
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
        viewModelScope.launch {
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

    // ── UI 辅助方法 ───────────────────────────────────────────────

    /** 更新输入框文本 */
    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /** 清空对话消息(NF-PP6: 同时删除当前对话 + 清空 DataStore currentId) */
    fun clearMessages() {
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
                currentConversationId = convId
                val messages = chatRepository.observeMessages(convId).first()
                if (messages.isNotEmpty()) {
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
    ) {
        val msg = AiMessage(
            id = nextId(),
            role = AiRole.ASSISTANT,
            content = content,
            contentSource = contentSource,
            references = references,
            stage = stage,
        )
        _uiState.update { it.copy(messages = it.messages + msg) }

        // NF-PP6: 持久化 AI 消息(currentConversationId 应已由 sendMessage 的 ensureConversation 设置)
        val convId = currentConversationId
        if (convId != null) {
            chatRepository.appendMessage(
                conversationId = convId,
                role = AiRole.ASSISTANT.name,
                content = content,
                contentSource = contentSource,
                stage = stage?.name,
                references = references,
                contextScreen = null,
                contextTitle = null,
                tokensUsed = null,
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
 */
data class AiAssistantUiState(
    val messages: List<AiMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isAvailable: Boolean = true,
    val errorMessage: String? = null,
    val roteWarning: String? = null,
    val recallResult: RecallResult? = null,
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
 */
@Immutable
data class AiMessage(
    val id: String,
    val role: AiRole,
    val content: String,
    val contentSource: String? = null,
    val references: List<RagReference> = emptyList(),
    val stage: SocraticStage? = null,
)

/** 消息角色 */
enum class AiRole {
    USER,
    ASSISTANT,
}
