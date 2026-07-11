package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wenyan.app.core.database.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * 复习日志表 DAO。
 */
@Dao
interface ReviewLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReviewLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ReviewLogEntity>)

    @Query("DELETE FROM review_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM review_logs WHERE id = :id")
    suspend fun getById(id: String): ReviewLogEntity?

    @Query("SELECT * FROM review_logs WHERE point_id = :pointId ORDER BY created_at DESC")
    fun observeByPoint(pointId: String): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ReviewLogEntity>>

    @Query("SELECT COUNT(*) FROM review_logs WHERE point_id = :pointId")
    suspend fun countByPoint(pointId: String): Int
}
