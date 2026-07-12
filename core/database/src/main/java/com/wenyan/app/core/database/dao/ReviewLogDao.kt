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

    /**
     * 获取某卡片的复习日志（按创建时间倒序）（阶段4新增）。
     *
     * 用于 [com.wenyan.app.core.ai.recall.AntiRoteMemorization] 计算连续正确次数：
     * 从最新记录开始往前遍历，遇到 GOOD/EASY 计数+1，遇到 AGAIN/HARD 停止。
     *
     * @param pointId 知识点 ID
     * @return 复习日志列表，最新记录在前
     */
    @Query("SELECT * FROM review_logs WHERE point_id = :pointId ORDER BY created_at DESC")
    suspend fun getByPointOrderByCreatedDesc(pointId: String): List<ReviewLogEntity>

    /**
     * 批量查询多个卡片的复习日志（阶段4新增）。
     *
     * 用于 [com.wenyan.app.core.ai.recall.AntiRoteMemorization] 计算关联卡片错误率：
     * 统计 AGAIN 评级占比，错误率 ≥ 0.4 视为"频繁出错"。
     *
     * @param pointIds 关联卡片 ID 列表
     * @return 匹配的复习日志列表
     */
    @Query("SELECT * FROM review_logs WHERE point_id IN (:pointIds)")
    suspend fun getByPointIds(pointIds: List<String>): List<ReviewLogEntity>
}
