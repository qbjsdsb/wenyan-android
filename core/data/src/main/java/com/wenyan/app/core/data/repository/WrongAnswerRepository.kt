package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.database.entity.WrongAnswerWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * 错题本仓库接口(NF-PP5 Wave 2.4)。
 *
 * 记录用户答错的题目,支持三个来源:
 * - [SOURCE_CARD_AGAIN]:卡片复习答 CardsViewModel.rateCard(AGAIN) 时记录
 * - [SOURCE_QUIZ_WRONG]:真题练习 QuizViewModel.submitAnswer() 判定错误时记录
 * - [SOURCE_ESSAY_PRACTICE]:论述题自评答不好 EssayDetailViewModel.rateSelf(AGAIN) 时记录（v0.9.9 Phase 3 新增）
 *
 * 同一知识点/真题的未解决错题,重复答错时递增 wrongCount(不重复插入),
 * markResolved 后该错题不再出现在 observeUnresolved 中。
 *
 * 设计说明:
 * - 读 API observeAll/observeUnresolved 返回 [WrongAnswerWithDetails](v0.9.2 改造,
 *   JOIN knowledge_points/exam_questions 获取题目文本,供 UI 渲染题目区)
 * - observeByPoint/observeByExamQuestion 仍返回 [WrongAnswerEntity](内部按 ID 筛选,
 *   不需要题目文本,调用方为内部逻辑)
 * - 写 API recordWrongAnswer 接收字段参数(非 Entity),内部判断是新插入还是递增
 * - source 参数用 String + companion const,避免新增 enum 但仍约束取值
 */
interface WrongAnswerRepository {

    /** 观察所有错题(JOIN 关联表获取题目文本,按 lastWrongAt DESC) */
    fun observeAll(): Flow<List<WrongAnswerWithDetails>>

    /** 观察未解决错题(JOIN 关联表获取题目文本,resolvedAt IS NULL,按 lastWrongAt DESC) */
    fun observeUnresolved(): Flow<List<WrongAnswerWithDetails>>

    /**
     * 观察待复习的未解决错题（v0.9.4 新增）。
     *
     * FSRS 调度：sched_next_review_at <= now AND resolved_at IS NULL。
     * 新建错题 sched_next_review_at=0（立即到期），首次进入即出现。
     *
     * @param now 当前时间戳（由调用方传入，便于测试控制时间）
     */
    fun observeDueWrongAnswers(now: Long): Flow<List<WrongAnswerWithDetails>>

    /** 观察指定知识点的错题(按 lastWrongAt DESC) */
    fun observeByPoint(pointId: String): Flow<List<WrongAnswerEntity>>

    /** 观察指定真题的错题(按 lastWrongAt DESC) */
    fun observeByExamQuestion(examQuestionId: String): Flow<List<WrongAnswerEntity>>

    /**
     * 记录一次答错。
     *
     * 逻辑:
     * - 同一 pointId + source(且 resolvedAt IS NULL)已有记录 → incrementWrongCount(wrongCount++)
     * - 同一 examQuestionId + source(且 resolvedAt IS NULL)已有记录 → incrementWrongCount
     * - 否则 → upsert 新 WrongAnswerEntity
     *
     * pointId 与 examQuestionId 至少一个非空(卡片来源用 pointId,真题来源用 examQuestionId)。
     *
     * @param pointId        关联知识点 ID(卡片来源非空)
     * @param examQuestionId 关联真题 ID(真题来源非空)
     * @param userAnswer     用户错误答案
     * @param correctAnswer  正确答案(可为空,待 AI 批改填入)
     * @param source         来源:[SOURCE_CARD_AGAIN] / [SOURCE_QUIZ_WRONG] / [SOURCE_ESSAY_PRACTICE]
     * @return 错题记录 ID(新插入或已有)
     */
    suspend fun recordWrongAnswer(
        pointId: String?,
        examQuestionId: String?,
        userAnswer: String,
        correctAnswer: String?,
        source: String,
    ): String

    /**
     * 标记错题为已解决(写 resolvedAt)。
     *
     * @param id 错题 ID
     */
    suspend fun markResolved(id: String)

    /**
     * 删除错题记录。
     *
     * @param id 错题 ID
     */
    suspend fun deleteById(id: String)

    /**
     * 统计未解决错题数量。
     *
     * @return 未解决数量
     */
    suspend fun countUnresolved(): Int

    companion object {
        /** 来源:卡片复习答 AGAIN */
        const val SOURCE_CARD_AGAIN = "CARD_AGAIN"

        /** 来源:真题练习答错 */
        const val SOURCE_QUIZ_WRONG = "QUIZ_WRONG"

        /** 来源:论述题自评答不好（v0.9.9 Phase 3 新增） */
        const val SOURCE_ESSAY_PRACTICE = "ESSAY_PRACTICE"
    }
}
