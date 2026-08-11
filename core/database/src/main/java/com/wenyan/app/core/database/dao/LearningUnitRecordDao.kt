package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.LearningUnitRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningUnitRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<LearningUnitRecordEntity>): List<Long>

    @Upsert
    suspend fun upsert(record: LearningUnitRecordEntity)

    @Query("SELECT * FROM learning_unit_records WHERE learning_unit_id = :unitId")
    suspend fun getById(unitId: String): LearningUnitRecordEntity?

    @Query(
        "SELECT records.* FROM learning_unit_records records " +
            "INNER JOIN learning_units units ON units.id = records.learning_unit_id " +
            "WHERE units.point_id = :pointId",
    )
    suspend fun getByPoint(pointId: String): List<LearningUnitRecordEntity>

    @Query("SELECT * FROM learning_unit_records WHERE learning_unit_id = :unitId")
    fun observeById(unitId: String): Flow<LearningUnitRecordEntity?>

    @Query("SELECT COUNT(*) FROM learning_unit_records")
    suspend fun countAll(): Int
}
