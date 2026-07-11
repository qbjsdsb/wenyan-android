package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.AiConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI 对话记录表 DAO。
 */
@Dao
interface AiConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AiConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AiConversationEntity>)

    @Update
    suspend fun update(entity: AiConversationEntity)

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM ai_conversations WHERE id = :id")
    suspend fun getById(id: String): AiConversationEntity?

    @Query("SELECT * FROM ai_conversations ORDER BY created_at ASC")
    fun observeAll(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE is_bookmarked = 1 ORDER BY created_at DESC")
    fun observeBookmarked(): Flow<List<AiConversationEntity>>

    @Query("UPDATE ai_conversations SET is_bookmarked = :bookmarked WHERE id = :id")
    suspend fun setBookmarked(id: String, bookmarked: Int)

    @Query("DELETE FROM ai_conversations")
    suspend fun clearAll()
}
