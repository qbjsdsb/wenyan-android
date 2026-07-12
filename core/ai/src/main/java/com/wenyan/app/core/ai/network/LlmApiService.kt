package com.wenyan.app.core.ai.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI 兼容协议的 Retrofit 接口。
 *
 * DeepSeek / 通义 / 智谱 / 月之暗面均兼容 `/v1/chat/completions` 端点。
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
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest,
    ): Response<ChatResponse>
}
