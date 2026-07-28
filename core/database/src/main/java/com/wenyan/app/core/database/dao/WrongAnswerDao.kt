package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.database.entity.WrongAnswerWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * 错题本表 DAO（wrong_answers）。
 *
 * NF-PP5 新增：支持 Cards AGAIN + Quiz 答错 双来源记录。
 * v0.9.2：observeAll / observeUnresolved 改为 JOIN 查询返回 [WrongAnswerWithDetails]，
 * 补充题目文本（知识点 title 或真题 content）供 UI 渲染。
 */
@Dao
interface WrongAnswerDao {

    @Upsert
    suspend fun upsert(entity: WrongAnswerEntity)

    @Query("DELETE FROM wrong_answers WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 观察所有错题（JOIN knowledge_points + exam_questions 获取题目文本）。
     *
     * v0.9.2：原 `SELECT * FROM wrong_answers` 无 JOIN，UI 拿不到题目文本，
     * 导致错题本只显示答案不显示题目。现 LEFT JOIN 两张关联表，用 COALESCE
     * 优先取知识点 title（卡片来源），真题来源取 exam_questions.content。
     */
    @Query(
        """
        SELECT w.*, COALESCE(k.title, e.content) AS question_title
        FROM wrong_answers w
        LEFT JOIN knowledge_points k ON w.point_id = k.id
        LEFT JOIN exam_questions e ON w.exam_question_id = e.id
        ORDER BY w.last_wrong_at DESC
        """,
    )
    fun observeAll(): Flow<List<WrongAnswerWithDetails>>

    /**
     * 观察未解决错题（JOIN 同上，仅过滤 resolved_at IS NULL）。
     */
    @Query(
        """
        SELECT w.*, COALESCE(k.title, e.content) AS question_title
        FROM wrong_answers w
        LEFT JOIN knowledge_points k ON w.point_id = k.id
        LEFT JOIN exam_questions e ON w.exam_question_id = e.id
        WHERE w.resolved_at IS NULL
        ORDER BY w.last_wrong_at DESC
        """,
    )
    fun observeUnresolved(): Flow<List<WrongAnswerWithDetails>>

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

    /**
     * 观察待复习的未解决错题（v0.9.4 新增）。
     *
     * FSRS 调度：sched_next_review_at <= now AND resolved_at IS NULL。
     * 新建错题 sched_next_review_at=0（立即到期），首次进入即出现在待复习列表。
     *
     * JOIN knowledge_points + exam_questions 获取题目文本（同 observeUnresolved）。
     */
    @Query(
        """
        SELECT w.*, COALESCE(k.title, e.content) AS question_title
        FROM wrong_answers w
        LEFT JOIN knowledge_points k ON w.point_id = k.id
        LEFT JOIN exam_questions e ON w.exam_question_id = e.id
        WHERE w.resolved_at IS NULL AND w.sched_next_review_at <= :now
        ORDER BY w.sched_next_review_at ASC
        """,
    )
    fun observeDueWrongAnswers(now: Long): Flow<List<WrongAnswerWithDetails>>

    /**
     * 按 ID 查询单条错题（v0.9.4 新增，用于 FSRS 调度时读取当前状态）。
     */
    @Query("SELECT * FROM wrong_answers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WrongAnswerEntity?

    /**
     * 更新错题的 FSRS 调度字段（v0.9.4 新增）。
     *
     * 由 [com.wenyan.app.core.data.repository.SchedulingRepositoryImpl.rateWrongAnswer] 调用，
     * FSRS 调度后一次性写入全部 sched_* 字段。
     */
    @Query(
        """
        UPDATE wrong_answers
        SET sched_state = :state,
            sched_stability = :stability,
            sched_difficulty = :difficulty,
            sched_last_review_at = :lastReviewAt,
            sched_next_review_at = :nextReviewAt,
            sched_review_count = :reviewCount,
            sched_lapses = :lapses,
            sched_elapsed_days = :elapsedDays,
            sched_scheduled_days = :scheduledDays,
            sched_reps = :reps
        WHERE id = :id
        """,
    )
    suspend fun updateScheduling(
        id: String,
        state: String,
        stability: Float,
        difficulty: Float,
        lastReviewAt: Long,
        nextReviewAt: Long,
        reviewCount: Int,
        lapses: Int,
        elapsedDays: Int,
        scheduledDays: Int,
        reps: Int,
    )
}
