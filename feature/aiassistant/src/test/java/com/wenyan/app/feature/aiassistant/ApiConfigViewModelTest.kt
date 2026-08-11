package com.wenyan.app.feature.aiassistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [validateBaseUrl] 纯函数单测（v0.9.31 批次 D 合规：强制 https）。
 *
 * 覆盖：
 * - 合法 https:// 通过（含带路径/尾斜杠）
 * - http:// 明文被拒绝（network_security_config 全局禁明文，fail-fast）
 * - 无协议 / 大写协议 被拒绝
 * - https:// 无域名被拒绝
 * - 首尾空白 trim
 */
class ApiConfigViewModelTest {

    @Test
    fun `合法的 https 地址通过`() {
        assertNull(validateBaseUrl("https://api.deepseek.com"))
        assertNull(validateBaseUrl("https://api.deepseek.com/v1"))
        assertNull(validateBaseUrl("https://api.deepseek.com/"))
        assertNull(validateBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1"))
        assertNull(validateBaseUrl("https://open.bigmodel.cn/api/paas/v4"))
    }

    @Test
    fun `首尾空白会被 trim 后校验`() {
        assertNull(validateBaseUrl("  https://api.deepseek.com  "))
    }

    @Test
    fun `http 明文地址被拒绝并提示改用 https`() {
        val error = validateBaseUrl("http://api.deepseek.com")
        assertNotNull(error)
        assertTrue(error!!.contains("http://"))
        assertTrue(error.contains("https://"))
        assertTrue(error.contains("明文"))
    }

    @Test
    fun `无协议地址被拒绝并提示以 https 开头`() {
        val error = validateBaseUrl("api.deepseek.com")
        assertNotNull(error)
        assertTrue(error!!.contains("https:// 开头"))
    }

    @Test
    fun `大写协议被拒绝`() {
        // Retrofit 要求 scheme 小写；大写 HTTPS:// 会被拒绝，校验保持大小写敏感
        val error = validateBaseUrl("HTTPS://api.deepseek.com")
        assertNotNull(error)
        assertTrue(error!!.contains("https:// 开头"))
    }

    @Test
    fun `https 无域名被拒绝`() {
        assertNotNull(validateBaseUrl("https://"))
        assertNotNull(validateBaseUrl("https:///"))
        assertNotNull(validateBaseUrl("https://?model=chat"))
        assertNotNull(validateBaseUrl("https://api.deepseek.com:bad"))
    }

    @Test
    fun `baseUrl 不允许携带凭据查询参数或片段`() {
        assertNotNull(validateBaseUrl("https://user:password@api.deepseek.com"))
        assertNotNull(validateBaseUrl("https://api.deepseek.com?model=chat"))
        assertNotNull(validateBaseUrl("https://api.deepseek.com/v1#chat"))
    }

    @Test
    fun `空字符串或纯空白被拒绝`() {
        assertNotNull(validateBaseUrl(""))
        assertNotNull(validateBaseUrl("   "))
    }

    @Test
    fun `错误信息不回显接口地址原文`() {
        val error = validateBaseUrl("api.deepseek.com")
        assertEquals(
            "接口地址必须以 https:// 开头",
            error,
        )
    }
}
