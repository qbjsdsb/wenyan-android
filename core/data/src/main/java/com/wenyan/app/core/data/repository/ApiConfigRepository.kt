package com.wenyan.app.core.data.repository

import com.wenyan.app.core.data.crypto.ApiKeyCrypto
import com.wenyan.app.core.database.dao.ApiConfigDao
import com.wenyan.app.core.database.entity.ApiConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API 配置仓库。
 *
 * 桥接 [ApiConfigDao] 与 [ApiKeyCrypto]：
 * - 写入时加密 apiKey（明文 → 密文存入数据库）
 * - 读取时解密 apiKey（密文 → 明文返回给上层）
 *
 * 上层（core:ai 的 AiServiceImpl）拿到的是**已解密**的 [ApiConfigEntity]，
 * 可直接用 apiKey 作为 Bearer token 调用 LLM API。
 *
 * @param apiConfigDao API 配置 DAO
 * @param apiKeyCrypto API Key 加解密器
 */
@Singleton
class ApiConfigRepository @Inject constructor(
    private val apiConfigDao: ApiConfigDao,
    private val apiKeyCrypto: ApiKeyCrypto,
) {

    /**
     * 观察当前选中的 API 配置（apiKey 已解密）。
     *
     * 无当前配置时返回 null。
     */
    fun observeCurrentConfig(): Flow<ApiConfigEntity?> =
        apiConfigDao.observeCurrent().map { it?.decrypted() }

    /**
     * 获取当前选中的 API 配置（apiKey 已解密）。
     *
     * @return 当前配置，无则 null
     */
    suspend fun getCurrentConfig(): ApiConfigEntity? =
        apiConfigDao.getCurrent()?.decrypted()

    /**
     * 观察所有已启用的 API 配置（apiKey 已解密）。
     */
    fun observeEnabledConfigs(): Flow<List<ApiConfigEntity>> =
        apiConfigDao.observeEnabled().map { list -> list.map { it.decrypted() } }

    /**
     * 观察所有 API 配置（apiKey 已解密）。
     */
    fun observeAllConfigs(): Flow<List<ApiConfigEntity>> =
        apiConfigDao.observeAll().map { list -> list.map { it.decrypted() } }

    /**
     * 保存 API 配置（apiKey 加密后存入数据库）。
     *
     * @param config 待保存的配置（apiKey 为明文，方法内部加密）
     */
    suspend fun saveConfig(config: ApiConfigEntity) {
        val encrypted = config.copy(apiKey = apiKeyCrypto.encrypt(config.apiKey))
        apiConfigDao.insert(encrypted)
    }

    /**
     * 按 ID 获取配置（apiKey 已解密）。
     */
    suspend fun getById(id: String): ApiConfigEntity? =
        apiConfigDao.getById(id)?.decrypted()

    /**
     * 设置当前使用的 API 配置。
     *
     * @param id 配置 ID
     */
    suspend fun setCurrent(id: String) {
        apiConfigDao.setCurrent(id)
    }

    /**
     * 删除指定 API 配置。
     *
     * @param id 配置 ID
     */
    suspend fun deleteConfig(id: String) {
        apiConfigDao.deleteById(id)
    }

    /** 将 Entity 中的 apiKey 从密文解密为明文 */
    private fun ApiConfigEntity.decrypted(): ApiConfigEntity =
        copy(apiKey = apiKeyCrypto.decrypt(apiKey))
}
