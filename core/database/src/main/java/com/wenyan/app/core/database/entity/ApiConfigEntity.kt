package com.wenyan.app.core.database.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * API 配置表 Entity（api_configs）。
 *
 * 对应设计文档 4.1 节 api_configs 表：
 * - id: 唯一标识
 * - provider: 服务商 deepseek / qwen / zhipu / moonshot / custom
 * - display_name: 显示名称
 * - base_url: 接口地址
 * - api_key: API 密钥（加密存储，业务层负责加解密）
 * - model: 模型名称
 * - temperature: 温度参数，默认 0.7
 * - max_tokens: 最大 token 数，默认 2000
 * - is_enabled: 是否启用，默认 1
 * - is_current: 是否当前使用，默认 0
 * - created_at: 创建时间
 */
@Immutable
@Entity(
    tableName = "api_configs",
    indices = [
        Index("provider"),
        Index("is_current"),
    ],
)
data class ApiConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** 服务商：deepseek / qwen / zhipu / moonshot / custom */
    @ColumnInfo(name = "provider")
    val provider: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "base_url")
    val baseUrl: String,

    /** API 密钥（加密存储，业务层负责加解密） */
    @ColumnInfo(name = "api_key")
    val apiKey: String,

    @ColumnInfo(name = "model")
    val model: String,

    @ColumnInfo(name = "temperature", defaultValue = "0.7")
    val temperature: Double = 0.7,

    @ColumnInfo(name = "max_tokens", defaultValue = "2000")
    val maxTokens: Int = 2000,

    @ColumnInfo(name = "is_enabled", defaultValue = "1")
    val isEnabled: Int = 1,

    @ColumnInfo(name = "is_current", defaultValue = "0")
    val isCurrent: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
