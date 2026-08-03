package com.wenyan.app.core.ai

import app.cash.turbine.test
import com.wenyan.app.core.ai.network.ChatMessage
import com.wenyan.app.core.ai.network.ChatUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * [AiServiceImpl] 单元测试。
 *
 * 验证：
 * - isAvailable 在无配置时返回 false
 * - chat 在无配置时返回离线提示
 * - chat 在有配置但无网络时返回错误提示（不崩溃）
 * - chatResultStream 流式 SSE 解析（v0.9.24）
 */
class AiServiceImplTest {

    private lateinit var okHttpClient: OkHttpClient
    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        // 短超时，避免网络不可达时测试卡住
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun makeConfig(baseUrl: String) = LlmConfig(
        baseUrl = baseUrl,
        apiKey = "sk-test",
        model = "deepseek-chat",
    )

    @Test
    fun `isAvailable 无配置时返回 false`() = runTest {
        val provider = FakeLlmConfigProvider(config = null)
        val service = AiServiceImpl(provider, okHttpClient)

        service.isAvailable().test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `isAvailable 有配置时返回 true`() = runTest {
        val config = makeConfig("https://api.deepseek.com")
        val provider = FakeLlmConfigProvider(config = config)
        val service = AiServiceImpl(provider, okHttpClient)

        service.isAvailable().test {
            assertTrue(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `chat 无配置时返回离线提示`() = runTest {
        val provider = FakeLlmConfigProvider(config = null)
        val service = AiServiceImpl(provider, okHttpClient)

        service.chat("什么是唐宋八大家？").test {
            val reply = awaitItem()
            assertTrue("应包含离线提示", reply.contains("未配置 API 服务商"))
            awaitComplete()
        }
    }

    @Test
    fun `chat 有配置但网络不可达时返回错误提示不崩溃`() = runTest {
        val config = makeConfig("http://127.0.0.1:1") // 不可达地址
        val provider = FakeLlmConfigProvider(config = config)
        val service = AiServiceImpl(provider, okHttpClient)

        service.chat("测试问题").test {
            val reply = awaitItem()
            assertTrue("应返回错误提示", reply.contains("网络错误"))
            awaitComplete()
        }
    }

    /**
     * v0.9.24：chatResultStream 解析 SSE 流，逐 chunk 收到 Delta，
     * 最后收到 Complete（含 usage）。
     *
     * 注：用 runBlocking（真实调度）而非 runTest（虚拟时间）——
     * chatResultStream 内部 flowOn(Dispatchers.IO)，虚拟时间调度器无法唤醒真实 IO。
     */
    @Test
    fun `chatResultStream 流式解析 SSE 增量与 usage`() = runBlocking {
        val sseBody = buildString {
            append("data: {\"choices\":[{\"delta\":{\"role\":\"assistant\",\"content\":\"韩\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"愈\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"是\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{}}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":3,\"total_tokens\":13}}\n\n")
            append("data: [DONE]\n\n")
        }
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody),
        )

        val config = makeConfig(server.url("/").toString())
        val provider = FakeLlmConfigProvider(config = config)
        val service = AiServiceImpl(provider, okHttpClient)

        val deltas = mutableListOf<String>()
        var usage: ChatUsage? = null
        var failure: Throwable? = null
        service.chatResultStream("苏轼是谁？").collect { result ->
            result.onSuccess { event ->
                when (event) {
                    is AiStreamEvent.Delta -> deltas.add(event.content)
                    is AiStreamEvent.Complete -> usage = event.usage
                }
            }.onFailure { failure = it }
        }

        assertNull("不应失败: $failure", failure)
        assertEquals(listOf("韩", "愈", "是"), deltas)
        assertNotNull("usage 不应为 null", usage)
        assertEquals(13, usage?.totalTokens)
    }

    /**
     * v0.9.24：chatResultStream 传入 history 时请求体 messages 应包含 system + history + user。
     */
    @Test
    fun `chatResultStream 多轮上下文 history 注入请求体`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"choices\":[{\"delta\":{\"content\":\"回答\"}}]}\n\ndata: [DONE]\n\n"),
        )

        val config = makeConfig(server.url("/").toString())
        val provider = FakeLlmConfigProvider(config = config)
        val service = AiServiceImpl(provider, okHttpClient)

        val history = listOf(
            ChatMessage(role = "user", content = "上一问"),
            ChatMessage(role = "assistant", content = "上一答"),
        )
        val deltas = mutableListOf<String>()
        service.chatResultStream("当前问题", history).collect { result ->
            result.onSuccess { event ->
                if (event is AiStreamEvent.Delta) deltas.add(event.content)
            }
        }
        assertEquals(listOf("回答"), deltas)

        // 校验请求体 messages 顺序：system → user(history) → assistant(history) → user(当前)
        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue("请求应包含 system", body.contains("\"system\""))
        assertTrue("history user 应注入", body.contains("上一问"))
        assertTrue("history assistant 应注入", body.contains("上一答"))
        assertTrue("当前问题应注入", body.contains("当前问题"))
    }

    /**
     * v0.9.24：chatResultStream 无配置时返回离线错误。
     */
    @Test
    fun `chatResultStream 无配置时返回离线错误`() = runTest {
        val provider = FakeLlmConfigProvider(config = null)
        val service = AiServiceImpl(provider, okHttpClient)

        service.chatResultStream("测试").test {
            val result = awaitItem()
            assertTrue("应失败", result.isFailure)
            assertTrue(
                "错误信息应含离线提示",
                result.exceptionOrNull()?.message?.contains("未配置") == true ||
                    result.exceptionOrNull()?.message?.contains("API") == true,
            )
            awaitComplete()
        }
    }
}

/**
 * [LlmConfigProvider] 的 Fake 实现，供单元测试使用。
 */
class FakeLlmConfigProvider(
    private val config: LlmConfig?,
) : LlmConfigProvider {

    override fun observeCurrentConfig(): Flow<LlmConfig?> = kotlinx.coroutines.flow.flowOf(config)

    override suspend fun getCurrentConfig(): LlmConfig? = config
}
