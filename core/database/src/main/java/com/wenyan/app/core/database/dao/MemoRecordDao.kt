package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.MemoRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 记忆记录表 DAO（FSRS 调度数据）。
 */
@Dao
interface MemoRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MemoRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MemoRecordEntity>)

    @Update
    suspend fun update(entity: MemoRecordEntity)

    @Query("DELETE FROM memo_records WHERE point_id = :pointId")
    suspend fun deleteById(pointId: String)

    @Query("SELECT * FROM memo_records WHERE point_id = :pointId")
    suspend fun getById(pointId: String): MemoRecordEntity?

    @Query("SELECT * FROM memo_records WHERE point_id = :pointId")
    fun observeById(pointId: String): Flow<MemoRecordEntity?>

    /** 查询到期需要复习的知识点（next_review_at <= 当前时间，使用 SQLite 内置时间避免 Flow 构建时时间戳固定） */
    @Query("SELECT * FROM memo_records WHERE next_review_at <= (CAST(strftime('%s', 'now') AS INTEGER) * 1000) ORDER BY next_review_at ASC")
    fun observeDue(): Flow<List<MemoRecordEntity>>

    /** 查询优先队列中的记忆记录 */
    @Query("SELECT * FROM memo_records WHERE in_priority_queue = 1 ORDER BY next_review_at ASC")
    fun observePriorityQueue(): Flow<List<MemoRecordEntity>>

    @Query("SELECT COUNT(*) FROM memo_records WHERE state = :state")
    suspend fun countByState(state: String): Int
}
