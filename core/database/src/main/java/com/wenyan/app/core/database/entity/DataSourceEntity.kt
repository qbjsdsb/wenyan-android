package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 资料来源溯源表 Entity（data_sources）。
 *
 * Spec 新增表（spec 第 25 行），设计文档无此表：
 * - id: 唯一标识
 * - knowledge_point_id: 知识点 ID（外键 knowledge_points.id）
 * - exam_question_id: 真题 ID（外键 exam_questions.id）
 * - source_file: 来源文件路径
 * - source_page: 来源页码
 * - content_source: 内容来源类型
 *     TEXTBOOK_NATIVE / TEXTBOOK_OCR / AI_GENERATED / HYBRID / USER_CREATED / MISSING
 * - ocr_status: OCR 状态，默认 VERIFIED（VERIFIED / PENDING）
 * - created_at: 创建时间
 */
@Entity(
    tableName = "data_sources",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgePointEntity::class,
            parentColumns = ["id"],
            childColumns = ["knowledge_point_id"],
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
        Index("knowledge_point_id"),
        Index("exam_question_id"),
        Index("content_source"),
    ],
)
data class DataSourceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "knowledge_point_id")
    val knowledgePointId: String?,

    @ColumnInfo(name = "exam_question_id")
    val examQuestionId: String?,

    @ColumnInfo(name = "source_file")
    val sourceFile: String,

    @ColumnInfo(name = "source_page")
    val sourcePage: Int?,

    /** 内容来源：TEXTBOOK_NATIVE / TEXTBOOK_OCR / AI_GENERATED / HYBRID / USER_CREATED / MISSING */
    @ColumnInfo(name = "content_source")
    val contentSource: String,

    @ColumnInfo(name = "ocr_status", defaultValue = "VERIFIED")
    val ocrStatus: String = "VERIFIED",

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
