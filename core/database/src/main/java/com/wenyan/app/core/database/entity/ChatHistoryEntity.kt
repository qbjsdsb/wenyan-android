package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 聊天历史表 Entity（chat_history）。
 *
 * 对应设计文档 4.1 节 chat_history 表：
 * - id: 唯一标识
 * - role: 角色 USER / ASSISTANT / SYSTEM
 * - content: 消息内容
 * - context_screen: 上下文屏幕
 * - context_title: 上下文标题
 * - context_content: 上下文内容
 * - api_config_id: 使用的 API 配置 ID（外键 api_configs.id）
 * - tokens_used: 消耗 token 数
 * - created_at: 创建时间
 */
@Entity(
    tableName = "chat_history",
    foreignKeys = [
        ForeignKey(
            entity = ApiConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["api_config_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("api_config_id"),
        Index("created_at"),
    ],
)
data class ChatHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** 角色：USER / ASSISTANT / SYSTEM */
    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "context_screen")
    val contextScreen: String?,

    @ColumnInfo(name = "context_title")
    val contextTitle: String?,

    @ColumnInfo(name = "context_content")
    val contextContent: String?,

    @ColumnInfo(name = "api_config_id")
    val apiConfigId: String?,

    @ColumnInfo(name = "tokens_used")
    val tokensUsed: Int?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
