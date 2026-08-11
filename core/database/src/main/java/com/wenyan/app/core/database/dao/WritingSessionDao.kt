package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.WritingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WritingSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(session: WritingSessionEntity)
    @Update suspend fun update(session: WritingSessionEntity): Int
    @Query("SELECT * FROM writing_sessions WHERE id = :id") suspend fun getById(id: String): WritingSessionEntity?
    @Query("SELECT * FROM writing_sessions WHERE id = :id") fun observeById(id: String): Flow<WritingSessionEntity?>
    @Query("SELECT * FROM writing_sessions WHERE state = 'COMPLETED' ORDER BY completed_at, id") fun observeCompleted(): Flow<List<WritingSessionEntity>>
    @Query("SELECT * FROM writing_sessions WHERE state NOT IN ('COMPLETED', 'DISCARDED') ORDER BY updated_at DESC, id") fun observeRecoverable(): Flow<List<WritingSessionEntity>>
}
