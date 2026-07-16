package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 错题本表 Entity（wrong_answers）。
 *
 * NF-PP5 新增：记录用户答错的题目，支持两个来源：
 * 1. 卡片复习答错（source = CARD_AGAIN）—— [com.wenyan.app.feature.cards.CardsViewModel] rateCard(AGAIN) 时记录
 * 2. 真题练习答错（source = QUIZ_WRONG）—— [com.wenyan.app.feature.quiz.QuizViewModel] submitAnswer() 判定错误时记录
 *
 * 字段说明：
 * - id: 错题唯一标识（UUID）
 * - point_id: 关联知识点 ID（卡片来源时非空，外键 knowledge_points.id CASCADE）
 * - exam_question_id: 关联真题 ID（真题来源时非空，外键 exam_questions.id CASCADE）
 * - user_answer: 用户错误答案
 * - correct_answer: 正确答案（可为空，待 AI 批改时填入）
 * - source: 来源 CARD_AGAIN / QUIZ_WRONG
 * - wrong_count: 答错次数（同一题重复答错递增）
 * - last_wrong_at: 最后答错时间戳
 * - resolved_at: 解决时间戳（null = 未解决）
 * - ai_explanation: AI 解释（可选，后续扩展）
 * - created_at: 首次记录时间戳
 */
@Entity(
    tableName = "wrong_answers",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgePointEntity::class,
            parentColumns = ["id"],
            childColumns = ["point_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExamQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["exam_question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("point_id"),
        Index("exam_question_id"),
        Index("resolved_at"),
    ],
)
data class WrongAnswerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** 关联知识点 ID（卡片来源时非空） */
    @ColumnInfo(name = "point_id")
    val pointId: String?,

    /** 关联真题 ID（真题来源时非空） */
    @ColumnInfo(name = "exam_question_id")
    val examQuestionId: String?,

    @ColumnInfo(name = "user_answer")
    val userAnswer: String,

    @ColumnInfo(name = "correct_answer")
    val correctAnswer: String?,

    /** 来源：CARD_AGAIN / QUIZ_WRONG */
    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "wrong_count", defaultValue = "1")
    val wrongCount: Int = 1,

    @ColumnInfo(name = "last_wrong_at")
    val lastWrongAt: Long,

    /** 解决时间戳（null = 未解决） */
    @ColumnInfo(name = "resolved_at")
    val resolvedAt: Long?,

    @ColumnInfo(name = "ai_explanation")
    val aiExplanation: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
