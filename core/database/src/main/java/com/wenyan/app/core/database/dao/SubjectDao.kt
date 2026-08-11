package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * 科目表 DAO。
 */
@Dao
interface SubjectDao {

    // P0 修正:原用 @Insert(REPLACE),DELETE+INSERT 会触发子表 CASCADE
    // (chapters → knowledge_points → memo_records/review_logs/data_sources),
    // 静默清空用户 FSRS 调度数据。改用 @Upsert(INSERT ... ON CONFLICT DO UPDATE)。
    @Upsert
    suspend fun insert(entity: SubjectEntity)

    @Upsert
    suspend fun insertAll(entities: List<SubjectEntity>)

    @Update
    suspend fun update(entity: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int
}
