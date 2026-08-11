package com.wenyan.app.core.ai.di

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * [RetryInterceptor] 单元测试（v0.8.16 P1-4b 新增）。
 *
 * 验证维度：
 * - 成功响应（200）直接返回，无重试
 * - 可重试状态码（429/500/502/503/504）触发重试
 * - 不可重试状态码（400/401/403/404）直接返回，无重试
 * - IOException 触发重试
 * - 协程取消（IOException("Canceled")）不重试（P1-4b 回归）
 * - SocketTimeoutException 重试（不应被误判为取消）
 * - 重试耗尽后抛出最后异常
 *
 * 注意：为避免真实 Thread.sleep 拖慢测试，使用 maxRetries=0 或 1，
 * 退避仍会 sleep 但只 1 次（约 500ms ± 200ms），可接受。
 */
class RetryInterceptorTest {

    private val dummyRequest = Request.Builder()
        .url("https://api.example.com/v1/chat/completions")
        .build()

    private fun buildResponse(code: Int): Response = Response.Builder()
        .request(dummyRequest)
        .protocol(Protocol.HTTP_1_1)
        .message("ok")
        .code(code)
        .body("{}".toResponseBody(null))
        .build()

    /**
     * Fake Chain：可控制 proceed() 的行为。
     *
     * - [responses]：按顺序返回的响应或异常队列
     * - [proceedCount]：记录 chain.proceed() 被调用次数
     */
    private class FakeChain(
        private val responses: MutableList<Any>,
    ) : Interceptor.Chain {
        var proceedCount = 0
            private set

        override fun request(): Request = Request.Builder().url("https://example.com/").build()

        override fun proceed(request: Request): Response {
            proceedCount++
            val item = responses.removeAt(0)
            return when (item) {
                is Response -> item
                is IOException -> throw item
                is Exception -> throw item
                else -> throw IllegalStateException("unknown response type: $item")
            }
        }

        // ── 其他 Chain 方法（未使用，提供空实现以满足接口） ──
        override fun connectTimeoutMillis(): Int = 0
        override fun readTimeoutMillis(): Int = 0
        override fun writeTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun call(): okhttp3.Call = throw UnsupportedOperationException()
        override fun connection(): okhttp3.Connection? = null
    }

    // ── 成功路径 ──────────────────────────────────────────────

    @Test
    fun `成功响应不重试`() {
        val chain = FakeChain(arrayListOf(buildResponse(200)))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(1, chain.proceedCount)
    }

    // ── 可重试状态码 ─────────────────────────────────────────

    @Test
    fun `429 限流重试后成功`() {
        val chain = FakeChain(arrayListOf(buildResponse(429), buildResponse(200)))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.proceedCount)
    }

    @Test
    fun `503 服务端错误重试后成功`() {
        val chain = FakeChain(arrayListOf(buildResponse(503), buildResponse(200)))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.proceedCount)
    }

    @Test
    fun `可重试状态码耗尽重试后返回最后响应`() {
        // 3 次重试 + 1 次初始 = 4 次，全部 503
        val chain = FakeChain(arrayListOf(
            buildResponse(503), buildResponse(503), buildResponse(503), buildResponse(503),
        ))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val response = interceptor.intercept(chain)

        assertEquals(503, response.code)
        assertEquals(4, chain.proceedCount)
    }

    // ── 不可重试状态码 ───────────────────────────────────────

    @Test
    fun `400 客户端错误不重试`() {
        val chain = FakeChain(arrayListOf(buildResponse(400)))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val response = interceptor.intercept(chain)

        assertEquals(400, response.code)
        assertEquals(1, chain.proceedCount)
    }

    @Test
    fun `401 鉴权失败不重试`() {
        val chain = FakeChain(arrayListOf(buildResponse(401)))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val response = interceptor.intercept(chain)

        assertEquals(401, response.code)
        assertEquals(1, chain.proceedCount)
    }

    @Test
    fun `403 禁止访问不重试`() {
        val chain = FakeChain(arrayListOf(buildResponse(403)))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val response = interceptor.intercept(chain)

        assertEquals(403, response.code)
        assertEquals(1, chain.proceedCount)
    }

    // ── IO 异常 ──────────────────────────────────────────────

    @Test
    fun `IOException 重试后成功`() {
        val chain = FakeChain(arrayListOf(
            IOException("connection reset"),
            buildResponse(200),
        ))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.proceedCount)
    }

    @Test
    fun `SocketTimeoutException 重试后成功`() {
        val chain = FakeChain(arrayListOf(
            SocketTimeoutException("read timeout"),
            buildResponse(200),
        ))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.proceedCount)
    }

    @Test
    fun `IOException 耗尽重试后抛出最后异常`() {
        val chain = FakeChain(arrayListOf(
            IOException("error 1"),
            IOException("error 2"),
            IOException("error 3"),
            IOException("final error"),
        ))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val ex = assertThrows(IOException::class.java) {
            interceptor.intercept(chain)
        }

        assertEquals("final error", ex.message)
        assertEquals(4, chain.proceedCount)
    }

    // ── P1-4b 回归：协程取消不重试 ───────────────────────────

    /**
     * P1-4b 回归测试：OkHttp Call.cancel() 抛 IOException("Canceled")，
     * 必须立即抛出，不重试。
     *
     * 原实现 `e is InterruptedException` 恒为 false（InterruptedException 不继承自 IOException），
     * 导致取消时浪费 3-6 秒退避重试。
     */
    @Test
    fun `Canceled 异常不重试_P1_4b回归`() {
        val chain = FakeChain(arrayListOf(
            IOException("Canceled"),
        ))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val ex = assertThrows(IOException::class.java) {
            interceptor.intercept(chain)
        }

        assertEquals("Canceled", ex.message)
        assertEquals("取消应只调用 proceed 1 次（不重试）", 1, chain.proceedCount)
    }

    @Test
    fun `cancelled 英式拼写也能识别`() {
        val chain = FakeChain(arrayListOf(
            IOException("Call cancelled by client"),
        ))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val ex = assertThrows(IOException::class.java) {
            interceptor.intercept(chain)
        }

        assertEquals(1, chain.proceedCount)
    }

    @Test
    fun `null message 的 IOException 仍重试`() {
        // 构造 message=null 的 IOException，不应误判为取消
        val nullMessageException = object : IOException() {
            override val message: String? get() = null
        }
        val chain = FakeChain(arrayListOf(
            nullMessageException,
            buildResponse(200),
        ))
        val interceptor = RetryInterceptor(maxRetries = 3)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals("null message 不应被误判为取消，应重试", 2, chain.proceedCount)
    }

    // ── maxRetries=0 边界 ────────────────────────────────────

    @Test
    fun `maxRetries=0 时可重试状态码直接返回不重试`() {
        val chain = FakeChain(arrayListOf(buildResponse(503)))
        val interceptor = RetryInterceptor(maxRetries = 0)

        val response = interceptor.intercept(chain)

        assertEquals(503, response.code)
        assertEquals(1, chain.proceedCount)
    }

    @Test
    fun `maxRetries=0 时 IOException 直接抛出不重试`() {
        val chain = FakeChain(arrayListOf(IOException("error")))
        val interceptor = RetryInterceptor(maxRetries = 0)

        assertThrows(IOException::class.java) {
            interceptor.intercept(chain)
        }

        assertEquals(1, chain.proceedCount)
    }

    // ── Retry-After 解析边界 ──────────────────────────────────

    @Test
    fun `Retry-After 超大秒数在乘毫秒前截断不溢出`() {
        val interceptor = RetryInterceptor(maxRetries = 0)

        assertEquals(5_000L, interceptor.retryAfterMillis(Long.MAX_VALUE.toString()))
        assertEquals(5_000L, interceptor.retryAfterMillis("6"))
        assertEquals(1_000L, interceptor.retryAfterMillis("1"))
    }

    @Test
    fun `Retry-After 非正数或非数字回退指数退避`() {
        val interceptor = RetryInterceptor(maxRetries = 0)

        assertNull(interceptor.retryAfterMillis(null))
        assertNull(interceptor.retryAfterMillis("0"))
        assertNull(interceptor.retryAfterMillis("-1"))
        assertNull(interceptor.retryAfterMillis("Wed, 21 Oct 2015 07:28:00 GMT"))
    }
}
