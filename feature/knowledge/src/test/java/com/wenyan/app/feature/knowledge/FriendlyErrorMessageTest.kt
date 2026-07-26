package com.wenyan.app.feature.knowledge

import android.database.sqlite.SQLiteException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * [friendlyErrorMessage] 单元测试(v0.8.13 P1-5 新增)。
 *
 * 覆盖所有异常分支映射,确保用户始终看到中文友好提示而非英文堆栈:
 * - 网络超时([SocketTimeoutException] / [UnknownHostException])
 * - 本地数据异常([SQLiteException])
 * - 加载超时([TimeoutCancellationException])
 * - 数据库版本异常(message 含 "no such table")
 * - 其他未知异常(fallback "加载失败,请重试")
 *
 * 这是 P1-5 修复的回归保护:原实现直接展示 `e.message ?: "加载失败"`,
 * 可能展示英文堆栈(SQLiteException: no such table...)。
 */
class FriendlyErrorMessageTest {

    @Test
    fun socketTimeout_returnsNetworkMessage() {
        val e = SocketTimeoutException("connect timed out")
        val msg = friendlyErrorMessage(e)
        assertEquals("网络超时,请检查网络后重试", msg)
        // 确保不泄露英文堆栈
        assertTrue("不应包含英文堆栈", !msg.contains("connect timed out", ignoreCase = true))
    }

    @Test
    fun unknownHost_returnsNetworkMessage() {
        val e = UnknownHostException("api.example.com")
        val msg = friendlyErrorMessage(e)
        assertEquals("网络超时,请检查网络后重试", msg)
        assertTrue("不应包含主机名", !msg.contains("api.example.com", ignoreCase = true))
    }

    @Test
    fun sqliteException_returnsLocalDataMessage() {
        // isReturnDefaultValues=true 允许实例化 Android SQLiteException
        val e = SQLiteException("UNIQUE constraint failed: knowledge_points.id")
        val msg = friendlyErrorMessage(e)
        assertEquals("本地数据异常,请重启 App", msg)
        assertTrue("不应包含 SQL 错误", !msg.contains("UNIQUE constraint", ignoreCase = true))
    }

    @Test
    fun timeoutCancellation_returnsLoadTimeoutMessage() = runBlocking {
        // TimeoutCancellationException 构造函数是 internal,无法直接实例化。
        // 用 withTimeout 真实产生异常实例来测试。
        var exception: TimeoutCancellationException? = null
        try {
            withTimeout(1) { delay(1000) }
        } catch (e: TimeoutCancellationException) {
            exception = e
        }
        assertNotNull("withTimeout should produce TimeoutCancellationException", exception)
        val msg = friendlyErrorMessage(exception!!)
        assertEquals("加载超时,请重试", msg)
        assertTrue("不应包含英文", !msg.contains("Timed out", ignoreCase = true))
    }

    @Test
    fun noSuchTableMessage_returnsDbVersionMessage() {
        // 非 SQLiteException 但 message 含 "no such table"(如包装后的运行时异常)
        val e = RuntimeException("no such table: knowledge_points (code 1)")
        val msg = friendlyErrorMessage(e)
        assertEquals("数据库版本异常,请重启 App", msg)
        assertTrue("不应包含表名", !msg.contains("knowledge_points", ignoreCase = true))
    }

    @Test
    fun noSuchTableMessage_caseInsensitive_returnsDbVersionMessage() {
        // 大小写不敏感匹配 "no such table"
        val e = RuntimeException("NO SUCH TABLE: exam_questions")
        val msg = friendlyErrorMessage(e)
        assertEquals("数据库版本异常,请重启 App", msg)
    }

    @Test
    fun genericException_returnsDefaultMessage() {
        val e = RuntimeException("unexpected null pointer")
        val msg = friendlyErrorMessage(e)
        assertEquals("加载失败,请重试", msg)
        assertTrue("不应包含英文", !msg.contains("unexpected", ignoreCase = true))
    }

    @Test
    fun exceptionWithNullMessage_returnsDefaultMessage() {
        val e = RuntimeException() // message == null
        val msg = friendlyErrorMessage(e)
        assertEquals("加载失败,请重试", msg)
    }
}
