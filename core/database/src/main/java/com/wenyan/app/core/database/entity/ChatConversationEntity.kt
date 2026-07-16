package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 聊天对话表 Entity（chat_conversations）。
 *
 * NF-PP6 新增：替代原 chat_history + ai_conversations 两张死代码表。
 * 对话元数据（标题 / API 配置快照 / 消息计数 / 时间戳），消息内容见 [ChatMessageEntity]。
 *
 * 字段说明：
 * - id: 对话唯一标识（UUID）
 * - title: 对话标题（用户可见，首条消息摘要或自定义）
 * - api_config_id: 使用的 API 配置 ID 快照（不设外键，配置删除后保留历史）
 * - model: 模型名称快照
 * - message_count: 消息总数（由 [com.wenyan.app.core.database.dao.ChatConversationDao.touch] 维护）
 * - created_at / updated_at: 创建 / 最后更新时间戳
 */
@Entity(
    tableName = "chat_conversations",
    indices = [Index("updated_at")],
)
data class ChatConversationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    /** API 配置 ID 快照（不设外键，配置删除后保留历史） */
    @ColumnInfo(name = "api_config_id")
    val apiConfigId: String?,

    @ColumnInfo(name = "model")
    val model: String?,

    @ColumnInfo(name = "message_count", defaultValue = "0")
    val messageCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
