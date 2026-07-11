package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.ChatHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 聊天历史表 DAO。
 */
@Dao
interface ChatHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ChatHistoryEntity>)

    @Update
    suspend fun update(entity: ChatHistoryEntity)

    @Query("DELETE FROM chat_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM chat_history WHERE id = :id")
    suspend fun getById(id: String): ChatHistoryEntity?

    @Query("SELECT * FROM chat_history ORDER BY created_at ASC")
    fun observeAll(): Flow<List<ChatHistoryEntity>>

    @Query("SELECT * FROM chat_history WHERE api_config_id = :configId ORDER BY created_at ASC")
    fun observeByApiConfig(configId: String): Flow<List<ChatHistoryEntity>>

    @Query("DELETE FROM chat_history")
    suspend fun clearAll()
}
