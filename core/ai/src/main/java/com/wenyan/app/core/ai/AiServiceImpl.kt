package com.wenyan.app.core.ai

import com.wenyan.app.core.ai.network.ChatMessage
import com.wenyan.app.core.ai.network.ChatRequest
import com.wenyan.app.core.ai.network.LlmApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AiService] 的生产实现。
 *
 * 使用 OpenAI 兼容协议（`/v1/chat/completions`）调用 LLM API。
 * 支持 DeepSeek / 通义 / 智谱 / 月之暗面等兼容服务商，仅 baseUrl 不同。
 *
 * 设计要点：
 * - 动态构造 Retrofit：baseUrl 从 [LlmConfig] 读取，每次调用按当前配置构造
 * - 非流式调用：先实现非流式（stream=false），后续可扩展为流式 SSE
 * - 离线降级：[isAvailable] 检查配置是否存在，实际网络错误在 [chat] 中 try-catch
 *
 * @property llmConfigProvider LLM 配置提供者
 * @property okHttpClient      OkHttp 客户端（用于构造 Retrofit）
 */
@Singleton
class AiServiceImpl @Inject constructor(
    private val llmConfigProvider: LlmConfigProvider,
    private val okHttpClient: OkHttpClient,
) : AiService {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 发送对话消息，返回 AI 回复。
     *
     * 流程：
     * 1. 从 [llmConfigProvider] 获取当前配置
     * 2. 无配置时返回离线提示
     * 3. 动态构造 Retrofit 客户端
     * 4. 调用 LLM API（非流式）
     * 5. 返回 AI 回复内容
     *
     * @param query 用户提问
     * @return 流式 AI 回复（当前为单次 emit，后续可扩展为逐字 emit）
     */
    override fun chat(query: String): Flow<String> = flow {
        val config = llmConfigProvider.getCurrentConfig()
        if (config == null) {
            emit(OFFLINE_MESSAGE)
            return@flow
        }

        try {
            val service = createLlmApiService(config)
            val request = ChatRequest(
                model = config.model,
                messages = listOf(
                    ChatMessage(role = "system", content = SYSTEM_PROMPT),
                    ChatMessage(role = "user", content = query),
                ),
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                stream = false,
            )
            val authorization = "Bearer ${config.apiKey}"
            val response = service.chatCompletion(authorization, request)

            if (response.isSuccessful) {
                val body = response.body()
                val content = body?.choices?.firstOrNull()?.message?.content
                emit(content ?: EMPTY_RESPONSE_MESSAGE)
            } else {
                // P0-E1 修正：HTTP 错误码差异化提示。
                // 401/403 → API Key 问题；5xx → 服务端问题；其他 → 通用提示。
                val code = response.code()
                val msg = when (code) {
                    401, 403 -> "API Key 无效或已过期（HTTP $code），请检查配置"
                    in 500..599 -> "AI 服务端错误（HTTP $code），请稍后重试"
                    else -> "API 调用失败（HTTP $code）：${response.message()}"
                }
                emit(msg)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            // P0-E1 修正：网络异常差异化。原实现统一显示"网络错误"，
            // 用户无法区分是超时、断网、DNS 失败还是协议错误。
            emit("请求超时，请检查网络连接后重试")
        } catch (e: UnknownHostException) {
            emit("无法连接到 AI 服务，请检查网络或 baseUrl 配置")
        } catch (e: SerializationException) {
            emit("AI 响应解析失败：${e.message}")
        } catch (e: IOException) {
            // 其他 IO 异常（如 ConnectionResetException）归为网络问题
            emit("网络错误，请检查网络连接：${e.message}")
        } catch (e: Exception) {
            emit("AI 调用失败：${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 发送对话消息，返回 Result 包装的响应（P1-6 修复）。
     *
     * 与 [chat] 的逻辑一致，但失败时 emit `Result.failure(exception)` 而非
     * emit 错误字符串，让调用方可区分成功/失败做短路。
     *
     * 错误信息与 [chat] 保持一致（HTTP 错误码差异化 + 网络异常差异化），
     * 调用方可用 `result.exceptionOrNull()?.message` 获取提示。
     */
    override fun chatResult(query: String): Flow<Result<String>> = flow {
        val config = llmConfigProvider.getCurrentConfig()
        if (config == null) {
            emit(Result.failure(IllegalStateException(OFFLINE_MESSAGE)))
            return@flow
        }

        try {
            val service = createLlmApiService(config)
            val request = ChatRequest(
                model = config.model,
                messages = listOf(
                    ChatMessage(role = "system", content = SYSTEM_PROMPT),
                    ChatMessage(role = "user", content = query),
                ),
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                stream = false,
            )
            val authorization = "Bearer ${config.apiKey}"
            val response = service.chatCompletion(authorization, request)

            if (response.isSuccessful) {
                val body = response.body()
                val content = body?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    emit(Result.success(content))
                } else {
                    emit(Result.failure(IllegalStateException(EMPTY_RESPONSE_MESSAGE)))
                }
            } else {
                val code = response.code()
                val msg = when (code) {
                    401, 403 -> "API Key 无效或已过期（HTTP $code），请检查配置"
                    in 500..599 -> "AI 服务端错误（HTTP $code），请稍后重试"
                    else -> "API 调用失败（HTTP $code）：${response.message()}"
                }
                emit(Result.failure(IllegalStateException(msg)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            emit(Result.failure(SocketTimeoutException("请求超时，请检查网络连接后重试")))
        } catch (e: UnknownHostException) {
            emit(Result.failure(UnknownHostException("无法连接到 AI 服务，请检查网络或 baseUrl 配置")))
        } catch (e: SerializationException) {
            emit(Result.failure(SerializationException("AI 响应解析失败：${e.message}")))
        } catch (e: IOException) {
            emit(Result.failure(IOException("网络错误，请检查网络连接：${e.message}")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 检查 AI 服务是否可用。
     *
     * @return true 表示有配置的 API 服务商（不代表网络一定通）
     */
    override fun isAvailable(): Flow<Boolean> = flow {
        emit(llmConfigProvider.getCurrentConfig() != null)
    }.flowOn(Dispatchers.IO)

    /**
     * 根据配置动态构造 [LlmApiService]。
     *
     * baseUrl 必须以 `/` 结尾（Retrofit 要求），否则补全。
     */
    private fun createLlmApiService(config: LlmConfig): LlmApiService {
        val baseUrl = if (config.baseUrl.endsWith("/")) config.baseUrl else "${config.baseUrl}/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LlmApiService::class.java)
    }

    private companion object {
        /** 系统提示词：苏格拉底式导师角色 */
        private const val SYSTEM_PROMPT = """你是一位中国文学考研辅导的苏格拉底式导师。
你的职责是引导学生自己思考，而非直接给出标准答案。
回答要求：
1. 先肯定用户思考中合理的部分
2. 指出论证中的薄弱环节或遗漏
3. 提供改进方向（而非完整答案）
4. 如有相关教材资料，标注引用来源
5. 用中文回答"""

        /** 无 API 配置时的离线提示 */
        private const val OFFLINE_MESSAGE = "AI 助手未配置 API 服务商，请在设置中配置后使用。"

        /** API 返回空响应时的提示 */
        private const val EMPTY_RESPONSE_MESSAGE = "AI 返回了空回复，请重试。"
    }
}
