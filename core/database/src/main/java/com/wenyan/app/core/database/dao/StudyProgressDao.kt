package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.StudyProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * 学习进度表 DAO。
 */
@Dao
interface StudyProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StudyProgressEntity)

    @Update
    suspend fun update(entity: StudyProgressEntity)

    @Query("DELETE FROM study_progress WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM study_progress WHERE id = :id")
    suspend fun getById(id: String): StudyProgressEntity?

    @Query("SELECT * FROM study_progress WHERE id = :id")
    fun observeById(id: String): Flow<StudyProgressEntity?>
}
