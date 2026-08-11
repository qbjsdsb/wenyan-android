package com.wenyan.app.core.ai.di

import com.wenyan.app.core.ai.AiService
import com.wenyan.app.core.ai.AiServiceImpl
import com.wenyan.app.core.ai.BuildConfig
import com.wenyan.app.core.ai.SocraticTutor
import com.wenyan.app.core.ai.SocraticTutorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.math.min

/**
 * core:ai 的 Hilt 模块。
 *
 * 提供：
 * - [AiService] → [AiServiceImpl] 绑定（@Binds）
 * - [OkHttpClient] 单例（@Provides，用于 Retrofit 构造）
 *
 * OkHttpClient 配置：
 * - 30 秒超时（LLM API 响应较慢）
 * - 日志拦截器：
 *   - Debug 构建用 BODY 级别（含 Authorization 头），便于联调
 *   - Release 构建用 NONE 级别，避免 API Key 泄漏到 logcat
 *   - P1-H2 修正：原实现始终 BODY 级别，Release 包中 API Key 会写入 logcat
 * - v0.8.16 P1-4 新增 [RetryInterceptor]：处理 LLM API 瞬时错误
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiService(impl: AiServiceImpl): AiService

    /**
     * 绑定 [SocraticTutor] 接口到 [SocraticTutorImpl]（v0.9.9 Phase 3 提取接口）。
     *
     * 消费方（[EssayDetailViewModel] / [AiAssistantViewModel]）依赖接口，
     * 单测可注入 FakeSocraticTutor 不依赖真实 LLM API。
     */
    @Binds
    @Singleton
    abstract fun bindSocraticTutor(impl: SocraticTutorImpl): SocraticTutor

    companion object {

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
                // P1 修正：即使 Debug BODY 级别也 redact Authorization 头，
                // 避免 API Key 明文写入 logcat（bug report / 旧版 Android logcat 读取）。
                redactHeader("Authorization")
            }
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // v0.9.26 成本控制：callTimeout 覆盖整个调用（含 SSE 长连接读取），
                // 防止流式响应卡死导致 token/电量空耗；上限 90s 足够长回答（逐字流式）。
                .callTimeout(90, TimeUnit.SECONDS)
                .addInterceptor(RetryInterceptor())
                .addInterceptor(loggingInterceptor)
                .build()
        }
    }
}

/**
 * LLM API 瞬时错误重试拦截器（v0.8.16 P1-4 新增）。
 *
 * 触发重试的条件（仅重试"明显瞬时"的错误，避免对永久性错误重试浪费时间）：
 * - HTTP 429 Too Many Requests（限流，常见于 DeepSeek/Qwen 高峰期）
 * - HTTP 500/502/503/504（服务端临时不可用）
 * - [IOException]（网络抖动、连接重置等，非用户取消）
 *
 * 不重试的条件：
 * - HTTP 4xx（除 429）：客户端错误，重试无意义（如 401 API Key 无效 / 400 请求格式错误）
 * - HTTP 200/3xx：成功响应，无需重试
 * - 请求体不可重放（如 multipart/streaming）：跳过重试（LLM API 请求均为 JSON，可重放）
 *
 * 退避策略：指数退避 + 抖动（jitter），避免 thundering herd
 * - 第1次重试：500ms ± 200ms
 * - 第2次重试：1000ms ± 400ms
 * - 第3次重试：2000ms ± 800ms
 * - 最多重试 3 次（共 4 次请求）
 *
 * 设计权衡：
 * - 不使用 OkHttp Authenticator（语义不符，那是 401 重试）
 * - 不重试非幂等方法：LLM /v1/chat/completions 是 POST，但 LLM 推理是幂等的
 *   （相同 prompt 多次调用结果不同但等价，重试不会破坏一致性）
 * - 总耗时上界：3 次重试 × (2s 退避 + 60s read timeout) ≈ 186s，远超用户容忍
 *   → 但实际只在网络错误时触发，正常请求零开销
 *
 * @see <a href="https://developers.deepseek.com/api-reference/zh-cn/error-codes">DeepSeek 错误码</a>
 * @see <a href="https://platform.openai.com/docs/guides/error-codes/api-errors">OpenAI 错误码</a>
 */
class RetryInterceptor(
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(chain.request())
                // 检查是否为可重试的 HTTP 状态码
                if (response.code in RETRYABLE_STATUS_CODES && attempt < maxRetries) {
                    // v0.9.26 成本控制：429/5xx 时优先读取 Retry-After 头（服务商明确告知
                    // 限流秒数），有则按其等待，避免请求过频再次撞限流；无则回退指数退避。
                    // 注：Retry-After 可能为 HTTP-date 格式，此处仅解析纯秒数，解析失败回退。
                    // v0.9.27 修复：clamp 到 MAX_BACKOFF_MS(5s)——服务商若返回大值（60s+），
                    // 不设上限会长时间阻塞 IO 线程且占住全局 Semaphore 槽位（3 槽可能被睡死），
                    // 用户"停止生成"也无法中断 Thread.sleep。上限后最长阻塞 5s，可接受。
                    val retryAfterMs = retryAfterMillis(response.header("Retry-After"))
                    response.close()
                    val backoffMs = retryAfterMs ?: computeBackoff(attempt)
                    Thread.sleep(backoffMs)
                    attempt++
                    continue
                }
                return response
            } catch (e: IOException) {
                // IO 异常可重试（连接重置、DNS 失败、socket 超时等）
                // 但 cancellation 是协程主动取消，不应重试
                if (isCancellation(e) || attempt >= maxRetries) {
                    throw e
                }
                lastException = e
                val backoffMs = computeBackoff(attempt)
                Thread.sleep(backoffMs)
                attempt++
            }
        }

        // 所有重试都失败，抛出最后一次异常
        throw lastException ?: IOException("LLM 请求重试 $maxRetries 次后仍失败")
    }

    /**
     * 计算指数退避 + 抖动延迟。
     *
     * @param attempt 当前已重试次数（0 表示第一次失败后准备重试）
     * @return 延迟毫秒数
     */
    private fun computeBackoff(attempt: Int): Long {
        val baseDelay = BASE_DELAY_MS * (1L shl attempt)  // 500, 1000, 2000
        val jitter = (Math.random() * (baseDelay * 0.4)) - (baseDelay * 0.2)  // ±20% 抖动
        return min(baseDelay + jitter.toLong(), MAX_BACKOFF_MS).coerceAtLeast(0)
    }

    /**
     * Parse a numeric Retry-After value without overflowing while converting seconds to millis.
     * Values above the backoff cap are clamped before multiplication.
     */
    internal fun retryAfterMillis(value: String?): Long? {
        val seconds = value
            ?.trim()
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?: return null
        return seconds.coerceAtMost(MAX_BACKOFF_MS / 1000) * 1000
    }

    /**
     * 判断异常是否为协程取消（不可重试）。
     *
     * v0.8.16 P1-4b 修复：原实现 `e is InterruptedException` 恒为 false ——
     * `InterruptedException` 继承自 `Exception` 而非 `IOException`，Kotlin
     * 编译器告警 "Check for instance is always 'false'"，运行时永不命中。
     * 后果：协程取消（用户离开 AI 助手页面）时，OkHttp 抛出 `IOException("Canceled")`
     * 被当作普通网络错误重试 3 次（≈3-6 秒退避 + 重复请求），浪费电量和 token。
     *
     * 正确检测方式：OkHttp 在 `Call.cancel()` 时抛 `IOException("Canceled")`
     * （见 RealCall.kt 的 `cancel()` 实现，message 固定为 "Canceled"）。
     * 协程取消时 Retrofit 调用 `Call.cancel()`，触发此异常。
     *
     * 注：SocketTimeoutException 也继承自 InterruptedIOException，但它是真实的
     * 网络超时（应重试），与 `IOException("Canceled")` 不同。
     * 这里通过 message 区分，不会误判 SocketTimeoutException 为取消。
     */
    private fun isCancellation(e: IOException): Boolean {
        // OkHttp Call.cancel() 抛 IOException("Canceled")（不区分大小写兼容 ClosedIOException 等）
        val message = e.message?.lowercase() ?: return false
        return message.contains("canceled") || message.contains("cancelled")
    }

    companion object {
        /** 默认最大重试次数（共 4 次请求：1 次初始 + 3 次重试） */
        private const val DEFAULT_MAX_RETRIES = 3

        /** 基础退避延迟（毫秒） */
        private const val BASE_DELAY_MS = 500L

        /** 最大退避延迟（毫秒），避免单次等待过长 */
        private const val MAX_BACKOFF_MS = 5000L

        /** 可重试的 HTTP 状态码：429（限流）、5xx（服务端错误） */
        private val RETRYABLE_STATUS_CODES = setOf(
            429, 500, 502, 503, 504,
        )
    }
}
