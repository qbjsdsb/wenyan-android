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

// ── 响应 DTO（流式） ──────────────────────────────────────────────

/**
 * OpenAI 兼容协议的 chat/completions 流式响应 chunk（v0.9.24 新增）。
 *
 * SSE 协议：`data: {json}\n\n`，结束标记 `data: [DONE]`。
 * 流式响应用 `choices[].delta.content`（而非非流式的 `message.content`）。
 * 部分服务商（DeepSeek 默认 / OpenAI 需 stream_options.include_usage）在
 * 最后一个 chunk 携带 `usage` 字段。
 */
@Serializable
data class ChatStreamChunk(
    val id: String? = null,
    val choices: List<ChatStreamChoice> = emptyList(),
    val usage: ChatUsage? = null,
)

/**
 * 流式响应中的选项。
 */
@Serializable
data class ChatStreamChoice(
    val index: Int = 0,
    val delta: ChatStreamDelta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

/**
 * 流式响应中的增量消息。
 */
@Serializable
data class ChatStreamDelta(
    val role: String? = null,
    val content: String? = null,
)
