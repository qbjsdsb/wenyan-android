package com.wenyan.app.core.ai.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── 请求 DTO ──────────────────────────────────────────────────────

/**
 * OpenAI 兼容协议的 chat/completions 请求体。
 *
 * DeepSeek / 通义 / 智谱 / 月之暗面均兼容此协议，仅 baseUrl 不同。
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens")
    val maxTokens: Int = 2000,
    val stream: Boolean = false,
)

/**
 * 对话消息（请求中的单条消息）。
 */
@Serializable
data class ChatMessage(
    val role: String,    // system / user / assistant
    val content: String,
)

// ── 响应 DTO（非流式） ────────────────────────────────────────────

/**
 * OpenAI 兼容协议的 chat/completions 响应体（非流式）。
 */
@Serializable
data class ChatResponse(
    val id: String? = null,
    val choices: List<ChatChoice> = emptyList(),
    val usage: ChatUsage? = null,
)

/**
 * 响应中的选项。
 */
@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatChoiceMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

/**
 * 响应中的消息。
 */
@Serializable
data class ChatChoiceMessage(
    val role: String? = null,
    val content: String? = null,
)

/**
 * Token 用量统计。
 */
@Serializable
data class ChatUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0,
)
