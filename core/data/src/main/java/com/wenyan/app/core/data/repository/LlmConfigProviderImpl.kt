package com.wenyan.app.core.data.repository

import com.wenyan.app.core.ai.LlmConfig
import com.wenyan.app.core.ai.LlmConfigProvider
import com.wenyan.app.core.data.util.catchAndLog
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
 *
 * P1 审计修复：observeCurrentConfig 加 .catchAndLog。
 * 虽然上游 [ApiConfigRepository.observeCurrentConfig] 已有 .catch，
 * 但本层 .map 内 toLlmConfig 异常不受上游保护（.catch 在 .map 之前），
 * 若 toLlmConfig 未来加入校验逻辑抛异常，会传播到 AiService collect 导致 AI 入口崩溃。
 * 降级为 null（匹配 LlmConfig? 返回类型），AI 服务会提示"未配置"。
 */
@Singleton
class LlmConfigProviderImpl @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
) : LlmConfigProvider {

    override fun observeCurrentConfig(): Flow<LlmConfig?> =
        apiConfigRepository.observeCurrentConfig()
            .map { it?.toLlmConfig() }
            .catchAndLog(TAG, "observeCurrentConfig") { null }

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

    private companion object {
        private const val TAG = "LlmConfigProviderImpl"
    }
}
