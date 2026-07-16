package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 聊天消息表 Entity（chat_messages）。
 *
 * NF-PP6 新增：存储 AI 对话的单条消息，FK→[ChatConversationEntity] ON DELETE CASCADE。
 *
 * 字段说明：
 * - id: 消息唯一标识（UUID）
 * - conversation_id: 所属对话 ID（外键 chat_conversations.id，CASCADE 删除）
 * - role: 角色 USER / ASSISTANT / SYSTEM
 * - content: 消息文本内容
 * - content_source: 内容来源标记（USER_INPUT / AI_REPLY / SYSTEM_PROMPT / RAG_QUOTED）
 * - stage: 苏格拉底对话阶段（enum name，可为空）
 * - references_json: RAG 引用列表 JSON 序列化（[com.wenyan.app.core.ai.model.RagReference] 列表）
 * - context_screen / context_title: 发起对话时的上下文屏幕 / 标题快照
 * - tokens_used: 消耗 token 数（AI 回复才有）
 * - created_at: 创建时间戳
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conversation_id")],
)
data class ChatMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "conversation_id")
    val conversationId: String,

    /** 角色：USER / ASSISTANT / SYSTEM */
    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "content")
    val content: String,

    /** 内容来源：USER_INPUT / AI_REPLY / SYSTEM_PROMPT / RAG_QUOTED */
    @ColumnInfo(name = "content_source")
    val contentSource: String?,

    /** 苏格拉底对话阶段（enum name） */
    @ColumnInfo(name = "stage")
    val stage: String?,

    /** RAG 引用列表 JSON */
    @ColumnInfo(name = "references_json")
    val referencesJson: String?,

    @ColumnInfo(name = "context_screen")
    val contextScreen: String?,

    @ColumnInfo(name = "context_title")
    val contextTitle: String?,

    @ColumnInfo(name = "tokens_used")
    val tokensUsed: Int?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
