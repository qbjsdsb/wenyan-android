package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.MemoRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 记忆记录表 DAO（FSRS 调度数据）。
 */
@Dao
interface MemoRecordDao {

    // P0-DB 修正：原用 @Insert(REPLACE)，会 DELETE+INSERT 触发 FK CASCADE + 丢 history。
    // 改用 @Upsert，内部用 INSERT ... ON CONFLICT DO UPDATE，不触发 DELETE，安全更新。
    @Upsert
    suspend fun upsert(entity: MemoRecordEntity)

    @Upsert
    suspend fun insertAll(entities: List<MemoRecordEntity>)

    @Update
    suspend fun update(entity: MemoRecordEntity)

    @Query("DELETE FROM memo_records WHERE point_id = :pointId")
    suspend fun deleteById(pointId: String)

    @Query("SELECT * FROM memo_records WHERE point_id = :pointId")
    suspend fun getById(pointId: String): MemoRecordEntity?

    @Query("SELECT * FROM memo_records WHERE point_id = :pointId")
    fun observeById(pointId: String): Flow<MemoRecordEntity?>

    /**
     * 查询真正到期的已学习记录。
     *
     * SeedDataLoader 会为全部知识点预建一条 pristine NEW 记录，便于首次评分时直接进入
     * FSRS；这类记录虽然 next_review_at 为当前时间，但语义仍是“每日新卡”，必须经过
     * 新卡限额与筛选，不能混入到期复习队列。
     *
     * 同时兼容两类旧数据：真正复习过但 reps 未回填的行可由 review_count 识别；旧版
     * 未学习行可能把 last_review_at 写成安装时间，因此不能用该字段判断是否学过。
     */
    @Query(
        """
        SELECT * FROM memo_records
        WHERE next_review_at <= (CAST(strftime('%s', 'now') AS INTEGER) * 1000)
          AND NOT (
              state = 'NEW'
              AND reps = 0
              AND review_count = 0
          )
        ORDER BY next_review_at ASC
        """,
    )
    fun observeDue(): Flow<List<MemoRecordEntity>>

    /** 查询优先队列中的记忆记录 */
    @Query("SELECT * FROM memo_records WHERE in_priority_queue = 1 ORDER BY next_review_at ASC")
    fun observePriorityQueue(): Flow<List<MemoRecordEntity>>

    /**
     * 观察全部记忆记录（阶段3新增，用于批量计算 R 值）。
     *
     * 一次性计算所有知识点的可提取性，避免 N+1 查询。
     *
     * P1-D3 修正：加 ORDER BY next_review_at ASC 保证 Compose 重组时顺序稳定。
     */
    @Query("SELECT * FROM memo_records ORDER BY next_review_at ASC")
    fun observeAll(): Flow<List<MemoRecordEntity>>

    @Query("SELECT COUNT(*) FROM memo_records WHERE state = :state")
    suspend fun countByState(state: String): Int

    /**
     * 查询所有已存在的 point_id（P1-AUDIT-4 种子版本升级用）。
     *
     * 种子升级时需跳过已有 MemoRecord 的知识点（保留用户 FSRS 学习进度），
     * 仅为新知识点创建初始 MemoRecord。用轻量 point_id 查询替代全量 entity 加载。
     */
    @Query("SELECT point_id FROM memo_records")
    suspend fun getExistingPointIds(): List<String>
}
