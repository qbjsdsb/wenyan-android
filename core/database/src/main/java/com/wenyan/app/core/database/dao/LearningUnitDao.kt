package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import com.wenyan.app.core.database.entity.LearningUnitEntity
import com.wenyan.app.core.database.entity.LearningUnitWithRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningUnitDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(units: List<LearningUnitEntity>): List<Long>

    @Upsert
    suspend fun upsertAll(units: List<LearningUnitEntity>)

    @Query("SELECT * FROM learning_units WHERE point_id = :pointId ORDER BY position, id")
    suspend fun getByPoint(pointId: String): List<LearningUnitEntity>

    @Query("SELECT * FROM learning_units WHERE id = :unitId")
    suspend fun getById(unitId: String): LearningUnitEntity?

    @Query("UPDATE learning_units SET active = 0, updated_at = :updatedAt WHERE point_id = :pointId AND id NOT IN (:activeIds) AND active = 1")
    suspend fun deactivateMissing(pointId: String, activeIds: List<String>, updatedAt: Long)

    @Query("SELECT * FROM learning_units WHERE point_id = :pointId ORDER BY position, id")
    fun observeByPoint(pointId: String): Flow<List<LearningUnitEntity>>

    @Transaction
    @Query("SELECT * FROM learning_units WHERE active = 1 ORDER BY point_id, position, id")
    fun observeActiveWithRecords(): Flow<List<LearningUnitWithRecord>>

    @Transaction
    @Query("SELECT * FROM learning_units WHERE point_id = :pointId AND active = 1 ORDER BY position, id")
    fun observeActiveWithRecordsByPoint(pointId: String): Flow<List<LearningUnitWithRecord>>

    @Query("SELECT COUNT(*) FROM learning_units")
    suspend fun countAll(): Int
}
