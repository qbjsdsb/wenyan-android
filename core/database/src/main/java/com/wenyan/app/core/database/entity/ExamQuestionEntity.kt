package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 真题表 Entity（exam_questions）。
 *
 * 对应设计文档 4.1 节 exam_questions 表，并合并 Spec 新增字段：
 * - 设计文档原有：id/year/subject_id/question_type/content/score/angle/
 *   related_point_ids/answer_framework/notes/created_at
 *   （注意：字段是 content 不是 stem；v2.9.0 已删除 sample_essay，范文冗余）
 * - Spec 新增：exam_paper_code / answer_status / material_text / source_file / source_page
 *
 * JSON 字段说明：
 * - related_point_ids: List<String>
 */
@Entity(
    tableName = "exam_questions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("subject_id"),
        Index("year"),
        Index("exam_paper_code"),
        // v0.9.24 新增：question_type（observeByQuestionType/observeAllEssays 筛选）、
        // answer_status（observeByAnswerStatus 筛选）——数据量增长后避免全表扫描
        Index("question_type"),
        Index("answer_status"),
        Index("content_status"),
    ],
)
data class ExamQuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "year")
    val year: Int,

    @ColumnInfo(name = "subject_id")
    val subjectId: String,

    /** 题型：TERM_EXPLANATION / SHORT_ANSWER / ESSAY / WRITING 等 */
    @ColumnInfo(name = "question_type")
    val questionType: String,

    /** 题目正文（注意：字段是 content 不是 stem） */
    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "score")
    val score: Int,

    /** 考查角度 */
    @ColumnInfo(name = "angle")
    val angle: String?,

    /** 关联知识点 ID 列表，JSON 数组 */
    @ColumnInfo(name = "related_point_ids")
    val relatedPointIds: List<String>?,

    /** 答题框架 */
    @ColumnInfo(name = "answer_framework")
    val answerFramework: String?,

    @ColumnInfo(name = "notes")
    val notes: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    // ---------- Spec 新增字段 ----------

    /** 当年试卷代码：610 / 801 / 805 / 806 等 */
    @ColumnInfo(name = "exam_paper_code")
    val examPaperCode: String?,

    /** 答案状态：HAS_ANSWER / NO_ANSWER / AI_GENERATED */
    @ColumnInfo(name = "answer_status")
    val answerStatus: String?,

    /** 材料题原文 */
    @ColumnInfo(name = "material_text")
    val materialText: String?,

    /** 来源文件路径 */
    @ColumnInfo(name = "source_file")
    val sourceFile: String?,

    /** 来源页码 */
    @ColumnInfo(name = "source_page")
    val sourcePage: Int?,

    /** Human review state, separate from answer availability and source evidence. */
    @ColumnInfo(name = "content_status", defaultValue = "LEGACY_UNVERIFIED")
    val contentStatus: String = ContentReviewStatus.LEGACY_UNVERIFIED.name,
)
