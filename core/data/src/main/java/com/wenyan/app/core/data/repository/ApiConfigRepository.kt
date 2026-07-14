package com.wenyan.app.core.data.repository

import android.util.Log
import com.wenyan.app.core.data.crypto.ApiKeyCrypto
import com.wenyan.app.core.database.dao.ApiConfigDao
import com.wenyan.app.core.database.entity.ApiConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
     * 无当前配置时返回 null。单项解密失败时也返回 null（Flow 不死）。
     */
    fun observeCurrentConfig(): Flow<ApiConfigEntity?> =
        apiConfigDao.observeCurrent()
            .map { it?.decryptedOrNull() }
            .catch { e ->
                Log.e(TAG, "observeCurrentConfig failed", e)
                emit(null)
            }

    /**
     * 获取当前选中的 API 配置（apiKey 已解密）。
     *
     * @return 当前配置，无则 null；解密失败也返回 null
     */
    suspend fun getCurrentConfig(): ApiConfigEntity? =
        apiConfigDao.getCurrent()?.decryptedOrNull()

    /**
     * 观察所有已启用的 API 配置（apiKey 已解密）。
     *
     * 单项解密失败的配置被过滤（mapNotNull），其他配置仍可用。
     */
    fun observeEnabledConfigs(): Flow<List<ApiConfigEntity>> =
        apiConfigDao.observeEnabled()
            .map { list -> list.mapNotNull { it.decryptedOrNull() } }
            .catch { e ->
                Log.e(TAG, "observeEnabledConfigs failed", e)
                emit(emptyList())
            }

    /**
     * 观察所有 API 配置（apiKey 已解密）。
     *
     * 单项解密失败的配置被过滤（mapNotNull），其他配置仍可用。
     * Flow 整体异常（如 DAO 错误）降级为空列表，避免 UI 永久 failed。
     */
    fun observeAllConfigs(): Flow<List<ApiConfigEntity>> =
        apiConfigDao.observeAll()
            .map { list -> list.mapNotNull { it.decryptedOrNull() } }
            .catch { e ->
                Log.e(TAG, "observeAllConfigs failed", e)
                emit(emptyList())
            }

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
     *
     * @return 配置；不存在或解密失败均返回 null
     */
    suspend fun getById(id: String): ApiConfigEntity? =
        apiConfigDao.getById(id)?.decryptedOrNull()

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

    /**
     * 将 Entity 中的 apiKey 从密文解密为明文（容错版）。
     *
     * P0-E1/E2 修正：原 [decrypted] 在 AndroidKeyStore 失效或单项密文损坏时抛
     * `GeneralSecurityException` / `KeyStoreException`，导致整个 Flow 永久 failed，
     * 所有 AI 配置不可用。现用 `runCatching` 包装：
     * - 单项解密失败返回 null，被上层 `mapNotNull` 过滤，其他配置仍可用
     * - 失败时记录警告日志（含 configId），便于线上排查
     */
    private fun ApiConfigEntity.decryptedOrNull(): ApiConfigEntity? =
        runCatching { copy(apiKey = apiKeyCrypto.decrypt(apiKey)) }
            .onFailure { e ->
                Log.w(TAG, "decrypt failed for config id=$id: ${e.message}", e)
            }
            .getOrNull()

    private companion object {
        private const val TAG = "ApiConfigRepository"
    }
}
