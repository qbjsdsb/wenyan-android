package com.wenyan.app.core.ai.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI 兼容协议的 Retrofit 接口。
 *
 * DeepSeek / 通义 / 智谱 / 月之暗面均兼容 chat/completions 端点。
 *
 * v0.9.23 修复（P1-1）：路径从 `v1/chat/completions` 改为 `chat/completions`。
 * 预设 baseUrl 统一为"版本前缀"（如 `https://api.deepseek.com/v1` /
 * `https://dashscope.aliyuncs.com/compatible-mode/v1` /
 * `https://open.bigmodel.cn/api/paas/v4` / `https://api.moonshot.cn/v1`），
 * 由 baseUrl 携带版本段，接口层不再重复拼接 v1，避免 3/4 服务商 404。
 *
 * 注意：baseUrl 在 [com.wenyan.app.core.ai.AiServiceImpl] 中根据
 * [com.wenyan.app.core.ai.LlmConfig] 动态构造，不在此接口硬编码。
 */
interface LlmApiService {

    /**
     * 非流式 chat/completions 请求。
     *
     * @param authorization Bearer token，格式 "Bearer <apiKey>"
     * @param request       请求体
     * @return 响应体
     */
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest,
    ): Response<ChatResponse>
}
