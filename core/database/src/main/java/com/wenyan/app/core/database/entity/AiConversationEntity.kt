package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 对话记录表 Entity（ai_conversations）。
 *
 * 对应设计文档 4.1 节 ai_conversations 表：
 * - id: 唯一标识
 * - role: 角色 USER / ASSISTANT / SYSTEM
 * - content: 消息内容
 * - context_screen_type: 上下文屏幕类型
 * - context_title: 上下文标题
 * - context_content: 上下文内容
 * - api_config_id: 使用的 API 配置 ID（外键 api_configs.id）
 * - tokens_used: 消耗 token 数
 * - is_bookmarked: 是否收藏，默认 0
 * - created_at: 创建时间
 *
 * 注意：本表与 chat_history 表字段相近但独立，context_screen 字段名为
 * context_screen_type，且新增 is_bookmarked 字段。
 */
@Entity(
    tableName = "ai_conversations",
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
        Index("is_bookmarked"),
        Index("created_at"),
    ],
)
data class AiConversationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** 角色：USER / ASSISTANT / SYSTEM */
    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "context_screen_type")
    val contextScreenType: String?,

    @ColumnInfo(name = "context_title")
    val contextTitle: String?,

    @ColumnInfo(name = "context_content")
    val contextContent: String?,

    @ColumnInfo(name = "api_config_id")
    val apiConfigId: String?,

    @ColumnInfo(name = "tokens_used")
    val tokensUsed: Int?,

    @ColumnInfo(name = "is_bookmarked", defaultValue = "0")
    val isBookmarked: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
