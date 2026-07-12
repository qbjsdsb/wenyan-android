package com.wenyan.app.core.data.repository

import app.cash.turbine.test
import com.wenyan.app.core.data.crypto.FakeApiKeyCrypto
import com.wenyan.app.core.database.entity.ApiConfigEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [ApiConfigRepository] 单元测试。
 *
 * 验证：
 * - 加解密往返一致性（saveConfig → getCurrentConfig 返回明文 apiKey）
 * - setCurrent 正确切换 isCurrent
 * - deleteConfig 正确删除
 * - observeAllConfigs 返回已解密的列表
 */
class ApiConfigRepositoryTest {

    private lateinit var dao: FakeApiConfigDao
    private lateinit var crypto: FakeApiKeyCrypto
    private lateinit var repository: ApiConfigRepository

    @Before
    fun setup() {
        dao = FakeApiConfigDao()
        crypto = FakeApiKeyCrypto()
        repository = ApiConfigRepository(dao, crypto)
    }

    @Test
    fun `saveConfig 加密 apiKey，getCurrentConfig 解密返回明文`() = runTest {
        val config = sampleConfig(id = "cfg1", apiKey = "sk-deepseek-123456")
        repository.saveConfig(config)

        // 数据库中存储的是密文
        val stored = dao.getById("cfg1")
        assertNotNull(stored)
        assertTrue("数据库中 apiKey 应为密文", stored!!.apiKey != "sk-deepseek-123456")
        assertTrue("密文应以 ENC: 开头", stored.apiKey.startsWith("ENC:"))

        // Repository 返回的是明文
        val retrieved = repository.getCurrentConfig()
        assertNull("无当前配置时应返回 null", retrieved)

        // 设为当前后获取
        repository.setCurrent("cfg1")
        val current = repository.getCurrentConfig()
        assertNotNull(current)
        assertEquals("返回的 apiKey 应为明文", "sk-deepseek-123456", current!!.apiKey)
    }

    @Test
    fun `加解密往返一致性`() = runTest {
        val plainKeys = listOf("sk-abc123", "Bearer xyz789", "", "中文密钥测试")

        for (plain in plainKeys) {
            val encrypted = crypto.encrypt(plain)
            val decrypted = crypto.decrypt(encrypted)
            assertEquals("加解密往返应一致: '$plain'", plain, decrypted)
        }
    }

    @Test
    fun `setCurrent 正确切换 isCurrent`() = runTest {
        val cfg1 = sampleConfig(id = "cfg1", apiKey = "key1")
        val cfg2 = sampleConfig(id = "cfg2", apiKey = "key2")
        repository.saveConfig(cfg1)
        repository.saveConfig(cfg2)

        repository.setCurrent("cfg1")
        assertEquals("cfg1", repository.getCurrentConfig()?.id)

        repository.setCurrent("cfg2")
        assertEquals("cfg2", repository.getCurrentConfig()?.id)
    }

    @Test
    fun `deleteConfig 正确删除`() = runTest {
        val config = sampleConfig(id = "cfg1", apiKey = "key1")
        repository.saveConfig(config)

        repository.deleteConfig("cfg1")
        assertNull(repository.getById("cfg1"))
    }

    @Test
    fun `observeAllConfigs 返回已解密的列表`() = runTest {
        repository.saveConfig(sampleConfig(id = "cfg1", apiKey = "key1"))
        repository.saveConfig(sampleConfig(id = "cfg2", apiKey = "key2"))

        repository.observeAllConfigs().test {
            val list = awaitItem()
            assertEquals(2, list.size)
            assertTrue(list.any { it.apiKey == "key1" })
            assertTrue(list.any { it.apiKey == "key2" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeCurrentConfig 返回已解密的当前配置`() = runTest {
        repository.saveConfig(sampleConfig(id = "cfg1", apiKey = "key1").copy(isCurrent = 1))

        repository.observeCurrentConfig().test {
            val current = awaitItem()
            assertNotNull(current)
            assertEquals("cfg1", current!!.id)
            assertEquals("返回的 apiKey 应为明文", "key1", current.apiKey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `空 apiKey 加密后解密仍为空`() {
        val encrypted = crypto.encrypt("")
        assertEquals("", encrypted)
        assertEquals("", crypto.decrypt(encrypted))
    }

    private fun sampleConfig(
        id: String,
        apiKey: String,
        isCurrent: Int = 0,
    ) = ApiConfigEntity(
        id = id,
        provider = "deepseek",
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com",
        apiKey = apiKey,
        model = "deepseek-chat",
        temperature = 0.7,
        maxTokens = 2000,
        isEnabled = 1,
        isCurrent = isCurrent,
        createdAt = System.currentTimeMillis(),
    )
}
