package com.wenyan.app.core.ai

import kotlinx.coroutines.flow.Flow

/**
 * LLM API 配置（core:ai 层面的轻量数据类，不依赖数据库 Entity）。
 *
 * 由 [LlmConfigProvider] 提供，[AiServiceImpl] 用于构造 Retrofit 客户端。
 *
 * @param baseUrl    接口地址（如 "https://api.deepseek.com"）
 * @param apiKey     API 密钥（明文，已由 ApiConfigRepository 解密）
 * @param model      模型名称（如 "deepseek-chat"）
 * @param temperature 温度参数
 * @param maxTokens  最大 token 数
 */
data class LlmConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2000,
)

/**
 * LLM 配置提供者接口。
 *
 * core:ai 定义此接口，core:data 的 [com.wenyan.app.core.data.repository.ApiConfigRepository]
 * 实现此接口（通过 [com.wenyan.app.core.data.repository.LlmConfigProviderImpl]）。
 *
 * 这样 core:ai 不需要直接依赖 core:data，避免循环依赖。
 */
interface LlmConfigProvider {

    /** 观察当前选中的 LLM 配置 */
    fun observeCurrentConfig(): Flow<LlmConfig?>

    /** 获取当前选中的 LLM 配置（suspend） */
    suspend fun getCurrentConfig(): LlmConfig?
}
