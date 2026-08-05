package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wenyan.app.core.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 聊天消息表 DAO（chat_messages）。
 *
 * NF-PP6 新增：存储 AI 对话消息内容，FK→chat_conversations ON DELETE CASCADE。
 */
@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun deleteByConversation(conversationId: String)

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    fun observeByConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    suspend fun getByConversation(conversationId: String): List<ChatMessageEntity>

    /**
     * 取指定对话最近 N 条消息（v0.9.24 新增，多轮上下文用）。
     *
     * 返回按 created_at **倒序**（最新在前），调用方需 reverse 成时间正序再注入 LLM。
     * LIMIT :limit 限制条数，避免长对话全量加载。
     */
    @Query(
        "SELECT * FROM chat_messages WHERE conversation_id = :conversationId " +
            "ORDER BY created_at DESC LIMIT :limit",
    )
    suspend fun getRecentByConversation(
        conversationId: String,
        limit: Int,
    ): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun countByConversation(conversationId: String): Int

    /**
     * 删除会话最旧的 N 条消息（v0.9.37 P1-7：消息保留上限）。
     *
     * 按 (created_at, rowid) 升序取最旧 limit 条的 rowid 删除——rowid 兜底
     * 同毫秒多条消息的场景（created_at 可能重复，仅按 created_at 会误删更多）。
     */
    @Query(
        "DELETE FROM chat_messages WHERE rowid IN (" +
            "SELECT rowid FROM chat_messages WHERE conversation_id = :conversationId " +
            "ORDER BY created_at ASC, rowid ASC LIMIT :limit)",
    )
    suspend fun deleteOldestByConversation(conversationId: String, limit: Int)
}
