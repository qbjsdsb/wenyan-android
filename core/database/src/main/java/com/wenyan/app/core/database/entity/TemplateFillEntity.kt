package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 模板填写记录表 Entity（template_fills）。
 *
 * 对应设计文档 4.1 节 template_fills 表：
 * - id: 唯一标识
 * - template_id: 使用的模板 ID（外键 answer_templates.id）
 * - exam_question_id: 关联真题 ID（外键 exam_questions.id）
 * - filled_sections: 已填写的分节，JSON 字符串（结构复杂，业务层解析）
 * - filled_fields: 已填写的字段，JSON 字符串（结构复杂，业务层解析）
 * - total_text: 完整答题文本
 * - word_count: 字数
 * - created_at / updated_at: 时间戳
 */
@Entity(
    tableName = "template_fills",
    foreignKeys = [
        ForeignKey(
            entity = AnswerTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExamQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["exam_question_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("template_id"),
        Index("exam_question_id"),
    ],
)
data class TemplateFillEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "template_id")
    val templateId: String,

    @ColumnInfo(name = "exam_question_id")
    val examQuestionId: String?,

    /** 已填写的分节，JSON 字符串（结构复杂，业务层解析） */
    @ColumnInfo(name = "filled_sections")
    val filledSections: String?,

    /** 已填写的字段，JSON 字符串（结构复杂，业务层解析） */
    @ColumnInfo(name = "filled_fields")
    val filledFields: String?,

    @ColumnInfo(name = "total_text")
    val totalText: String,

    @ColumnInfo(name = "word_count")
    val wordCount: Int?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
