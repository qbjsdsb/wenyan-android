package com.wenyan.app.feature.aiassistant

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
 */
@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val aiService: AiService,
    private val socraticTutor: SocraticTutor,
    private val ragEngine: RagEngine,
    private val recallChecker: RecallChecker,
    private val antiRoteMemorization: AntiRoteMemorization,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    init {
        checkAvailability()
    }

    // ── 普通问答（RAG + AI） ──────────────────────────────────────

    /**
     * 发送用户消息，获取 AI 回答。
     *
     * 流程：
     * 1. 添加用户消息到列表
     * 2. RAG 检索相关资料
     * 3. 检查 AI 可用性（离线降级）
     * 4. 构建 RAG prompt → 调用 AiService
     * 5. 添加 AI 回复（标注引用来源）
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
                // 1. RAG 检索
                val ragResult = ragEngine.search(text).first()

                // 2. 检查 AI 可用性
                val available = aiService.isAvailable().first()
                if (!available) {
                    addOfflineMessage(ragResult.references)
                    return@launch
                }

                // 3. 构建 prompt 并调用 AI
                val prompt = PromptTemplates.buildChatPrompt(text, ragResult.references)
                val response = aiService.chat(prompt).first()

                // 4. 添加 AI 回复（标注引用来源）
                addAssistantMessage(
                    content = response,
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

    // ── 防死记硬背检测 ────────────────────────────────────────────

    /**
     * 检测某卡片是否疑似死记硬背。
     *
     * 检测结果通过 [AiAssistantUiState.roteWarning] 暴露给 UI。
     */
    fun checkRoteMemorization(cardId: String, relatedCardIds: List<String>) {
        viewModelScope.launch {
            try {
                val result: RoteCheckResult = antiRoteMemorization
                    .checkRoteMemorization(cardId, relatedCardIds)
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

    /** 清空对话消息 */
    fun clearMessages() {
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

    /** 添加 AI 助手消息 */
    private fun addAssistantMessage(
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
    }

    /** 添加苏格拉底引导消息（按阶段标注） */
    private fun addSocraticGuideMessage(guide: SocraticGuide) {
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

    /** 离线降级：AI 不可用时显示友好提示（附 RAG 引用供参考） */
    private fun addOfflineMessage(references: List<RagReference>) {
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
    }
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
 */
data class AiAssistantUiState(
    val messages: List<AiMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isAvailable: Boolean = true,
    val errorMessage: String? = null,
    val roteWarning: String? = null,
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
