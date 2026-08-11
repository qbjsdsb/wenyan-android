package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.ChatConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * 聊天对话表 DAO（chat_conversations）。
 *
 * NF-PP6 新增：配合 [ChatMessageDao] 实现 AI 对话持久化。
 */
@Dao
interface ChatConversationDao {

    @Upsert
    suspend fun upsert(entity: ChatConversationEntity)

    @Query("DELETE FROM chat_conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM chat_conversations ORDER BY updated_at DESC, id ASC")
    fun observeAll(): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_conversations WHERE id = :id")
    suspend fun getById(id: String): ChatConversationEntity?

    @Query("SELECT * FROM chat_conversations ORDER BY updated_at DESC, id ASC LIMIT 1")
    suspend fun getMostRecent(): ChatConversationEntity?

    /**
     * 更新对话的消息计数与时间戳（追加消息时调用）。
     */
    @Query(
        "UPDATE chat_conversations SET message_count = message_count + 1, updated_at = :updatedAt WHERE id = :id",
    )
    suspend fun touch(id: String, updatedAt: Long)

    /**
     * 直接设定消息计数（v0.9.37 P1-7：消息保留上限清理后同步计数，防漂移）。
     */
    @Query(
        "UPDATE chat_conversations SET message_count = :count, updated_at = :updatedAt WHERE id = :id",
    )
    suspend fun setMessageCount(id: String, count: Int, updatedAt: Long)
}
