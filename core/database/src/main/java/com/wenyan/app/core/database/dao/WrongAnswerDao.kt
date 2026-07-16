package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import kotlinx.coroutines.flow.Flow

/**
 * 错题本表 DAO（wrong_answers）。
 *
 * NF-PP5 新增：支持 Cards AGAIN + Quiz 答错 双来源记录。
 */
@Dao
interface WrongAnswerDao {

    @Upsert
    suspend fun upsert(entity: WrongAnswerEntity)

    @Query("DELETE FROM wrong_answers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM wrong_answers ORDER BY last_wrong_at DESC")
    fun observeAll(): Flow<List<WrongAnswerEntity>>

    @Query("SELECT * FROM wrong_answers WHERE resolved_at IS NULL ORDER BY last_wrong_at DESC")
    fun observeUnresolved(): Flow<List<WrongAnswerEntity>>

    @Query("SELECT * FROM wrong_answers WHERE point_id = :pointId ORDER BY last_wrong_at DESC")
    fun observeByPoint(pointId: String): Flow<List<WrongAnswerEntity>>

    @Query("SELECT * FROM wrong_answers WHERE exam_question_id = :examQuestionId ORDER BY last_wrong_at DESC")
    fun observeByExamQuestion(examQuestionId: String): Flow<List<WrongAnswerEntity>>

    /**
     * 查找同一知识点 + 同一来源的未解决错题（用于重复答错时递增 wrong_count）。
     */
    @Query(
        "SELECT * FROM wrong_answers WHERE point_id = :pointId AND source = :source AND resolved_at IS NULL LIMIT 1",
    )
    suspend fun findUnresolvedByPointAndSource(
        pointId: String,
        source: String,
    ): WrongAnswerEntity?

    /**
     * 查找同一真题 + 同一来源的未解决错题（用于重复答错时递增 wrong_count）。
     */
    @Query(
        "SELECT * FROM wrong_answers WHERE exam_question_id = :examQuestionId AND source = :source AND resolved_at IS NULL LIMIT 1",
    )
    suspend fun findUnresolvedByExamQuestionAndSource(
        examQuestionId: String,
        source: String,
    ): WrongAnswerEntity?

    /**
     * 递增答错次数并重置为未解决状态。
     */
    @Query(
        "UPDATE wrong_answers SET wrong_count = wrong_count + 1, last_wrong_at = :lastWrongAt, resolved_at = NULL WHERE id = :id",
    )
    suspend fun incrementWrongCount(id: String, lastWrongAt: Long)

    @Query("UPDATE wrong_answers SET resolved_at = :resolvedAt WHERE id = :id")
    suspend fun markResolved(id: String, resolvedAt: Long)

    @Query("SELECT COUNT(*) FROM wrong_answers WHERE resolved_at IS NULL")
    suspend fun countUnresolved(): Int
}
