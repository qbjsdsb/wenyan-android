package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 批改记录表 Entity（ai_grading_records）。
 *
 * 对应设计文档 4.1 节 ai_grading_records 表：
 * - id: 唯一标识
 * - exam_question_id: 真题 ID（外键 exam_questions.id）
 * - user_answer: 用户作答
 * - grading_result: 批改结果，JSON 字符串（结构复杂，业务层解析）
 * - api_config_id: 使用的 API 配置 ID（外键 api_configs.id）
 * - tokens_used: 消耗 token 数
 * - created_at: 创建时间
 */
@Entity(
    tableName = "ai_grading_records",
    foreignKeys = [
        ForeignKey(
            entity = ExamQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["exam_question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ApiConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["api_config_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("exam_question_id"),
        Index("api_config_id"),
    ],
)
data class AiGradingRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "exam_question_id")
    val examQuestionId: String?,

    @ColumnInfo(name = "user_answer")
    val userAnswer: String,

    /** 批改结果，JSON 字符串（结构复杂，业务层解析） */
    @ColumnInfo(name = "grading_result")
    val gradingResult: String,

    @ColumnInfo(name = "api_config_id")
    val apiConfigId: String?,

    @ColumnInfo(name = "tokens_used")
    val tokensUsed: Int?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
