package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.DailyTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(tasks: List<DailyTaskEntity>)

    @Query("SELECT * FROM daily_tasks WHERE plan_id = :planId ORDER BY position, id")
    suspend fun getByPlan(planId: String): List<DailyTaskEntity>

    @Query("SELECT * FROM daily_tasks WHERE plan_id = :planId ORDER BY position, id")
    fun observeByPlan(planId: String): Flow<List<DailyTaskEntity>>

    @Query("SELECT * FROM daily_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DailyTaskEntity?

    @Query("SELECT * FROM daily_tasks WHERE plan_id = :planId AND stable_id = :stableId LIMIT 1")
    suspend fun getByStableId(planId: String, stableId: String): DailyTaskEntity?

    @Query("SELECT * FROM daily_tasks WHERE carried_from_task_id = :sourceId LIMIT 1")
    suspend fun getCarriedFrom(sourceId: String): DailyTaskEntity?

    @Query("SELECT daily_tasks.* FROM daily_tasks INNER JOIN daily_plans ON daily_plans.id = daily_tasks.plan_id WHERE daily_plans.plan_date < :date AND daily_tasks.status = 'PENDING' ORDER BY daily_plans.plan_date, daily_tasks.position, daily_tasks.id")
    suspend fun getLegacyBefore(date: String): List<DailyTaskEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: DailyTaskEntity)

    @Update
    suspend fun update(task: DailyTaskEntity): Int

    @Query("UPDATE daily_tasks SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long): Int
}
