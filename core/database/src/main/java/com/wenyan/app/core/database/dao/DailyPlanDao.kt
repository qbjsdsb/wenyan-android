package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.DailyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyPlanDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(plan: DailyPlanEntity): Long

    @Query("SELECT * FROM daily_plans WHERE plan_date = :date LIMIT 1")
    suspend fun getEntityByDate(date: String): DailyPlanEntity?

    @Query("SELECT * FROM daily_plans WHERE id = :id LIMIT 1")
    suspend fun getEntityById(id: String): DailyPlanEntity?

    @Query("SELECT * FROM daily_plans WHERE plan_date = :date LIMIT 1")
    fun observeEntityByDate(date: String): Flow<DailyPlanEntity?>

    @Update
    suspend fun update(plan: DailyPlanEntity): Int
}
