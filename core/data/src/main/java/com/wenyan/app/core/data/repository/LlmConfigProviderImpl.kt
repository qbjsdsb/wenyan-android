package com.wenyan.app.core.data.repository

import com.wenyan.app.core.ai.LlmConfig
import com.wenyan.app.core.ai.LlmConfigProvider
import com.wenyan.app.core.database.entity.ApiConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [LlmConfigProvider] 的实现，桥接 [ApiConfigRepository] 与 core:ai。
 *
 * 将 [ApiConfigEntity]（数据库层）转换为 [LlmConfig]（AI 层轻量数据类），
 * 使 core:ai 的 [com.wenyan.app.core.ai.AiServiceImpl] 无需直接依赖 core:data。
 */
@Singleton
class LlmConfigProviderImpl @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
) : LlmConfigProvider {

    override fun observeCurrentConfig(): Flow<LlmConfig?> =
        apiConfigRepository.observeCurrentConfig().map { it?.toLlmConfig() }

    override suspend fun getCurrentConfig(): LlmConfig? =
        apiConfigRepository.getCurrentConfig()?.toLlmConfig()

    /** 将 [ApiConfigEntity] 转换为 [LlmConfig] */
    private fun ApiConfigEntity.toLlmConfig(): LlmConfig = LlmConfig(
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        temperature = temperature,
        maxTokens = maxTokens,
    )
}
