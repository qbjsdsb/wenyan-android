package com.wenyan.app.core.ai

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
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
 */
class AiServiceImplTest {

    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setup() {
        // 短超时，避免网络不可达时测试卡住
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()
    }

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
        val config = LlmConfig(
            baseUrl = "https://api.deepseek.com",
            apiKey = "sk-test",
            model = "deepseek-chat",
        )
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
            assertTrue("应包含离线提示", reply.contains("未配置") || reply.contains("API"))
            awaitComplete()
        }
    }

    @Test
    fun `chat 有配置但网络不可达时返回错误提示不崩溃`() = runTest {
        val config = LlmConfig(
            baseUrl = "http://127.0.0.1:1", // 不可达地址
            apiKey = "sk-test",
            model = "deepseek-chat",
        )
        val provider = FakeLlmConfigProvider(config = config)
        val service = AiServiceImpl(provider, okHttpClient)

        service.chat("测试问题").test {
            val reply = awaitItem()
            assertTrue("应返回错误提示", reply.contains("网络错误") || reply.contains("失败") || reply.contains("错误"))
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
