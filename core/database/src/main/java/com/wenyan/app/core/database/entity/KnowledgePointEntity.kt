package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 知识点表 Entity（knowledge_points）。
 *
 * 对应设计文档 4.1 节 knowledge_points 表，并合并 Spec 新增字段：
 * - 设计文档原有：id/chapter_id/title/summary/core_conclusion/full_content/
 *   multi_perspectives/related_ids/contrast_ids/extension_ids/exam_records/
 *   exam_frequency/term_template/tags/difficulty/created_at/updated_at
 * - Spec 新增：content_source / ocr_status / source_file / source_page / study_text
 *
 * 注意：本表无 subject_id 字段，通过 chapter_id 间接关联科目。
 *
 * JSON 字段说明：
 * - multi_perspectives: Map<String, String>（多维视角分析）
 * - related_ids / contrast_ids / extension_ids / tags: List<String>
 * - exam_records / term_template: 结构复杂，使用 String 存储，业务层解析
 */
@Entity(
    tableName = "knowledge_points",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("chapter_id"),
        Index("exam_frequency"),
        Index("ocr_status"),
    ],
)
data class KnowledgePointEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "chapter_id")
    val chapterId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "summary")
    val summary: String?,

    @ColumnInfo(name = "core_conclusion")
    val coreConclusion: String,

    @ColumnInfo(name = "full_content")
    val fullContent: String,

    /** 多维视角分析，JSON 对象，如 {"文学史": "...", "理论": "..."} */
    @ColumnInfo(name = "multi_perspectives")
    val multiPerspectives: Map<String, String>?,

    /** 关联知识点 ID 列表，JSON 数组 */
    @ColumnInfo(name = "related_ids")
    val relatedIds: List<String>?,

    /** 对比知识点 ID 列表，JSON 数组 */
    @ColumnInfo(name = "contrast_ids")
    val contrastIds: List<String>?,

    /** 延伸知识点 ID 列表，JSON 数组 */
    @ColumnInfo(name = "extension_ids")
    val extensionIds: List<String>?,

    /** 历年真题记录，JSON 字符串（结构复杂，业务层解析） */
    @ColumnInfo(name = "exam_records")
    val examRecords: String?,

    /** 考频：HIGH / MEDIUM / LOW / NEVER */
    @ColumnInfo(name = "exam_frequency")
    val examFrequency: String,

    /** 术语解释模板，JSON 字符串（结构复杂，业务层解析） */
    @ColumnInfo(name = "term_template")
    val termTemplate: String?,

    /** 标签列表，JSON 数组 */
    @ColumnInfo(name = "tags")
    val tags: List<String>?,

    /** 难度等级 1-5，默认 3 */
    @ColumnInfo(name = "difficulty", defaultValue = "3")
    val difficulty: Int = 3,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    // ---------- Spec 新增字段 ----------

    /** 内容来源：TEXTBOOK_NATIVE / TEXTBOOK_OCR / AI_GENERATED / HYBRID / USER_CREATED / MISSING */
    @ColumnInfo(name = "content_source")
    val contentSource: String?,

    /** OCR 状态：VERIFIED / PENDING，默认 VERIFIED */
    @ColumnInfo(name = "ocr_status", defaultValue = "VERIFIED")
    val ocrStatus: String = "VERIFIED",

    /** 来源文件路径 */
    @ColumnInfo(name = "source_file")
    val sourceFile: String?,

    /** 来源页码 */
    @ColumnInfo(name = "source_page")
    val sourcePage: Int?,

    /** 学习理解文本（袁行霈版为主） */
    @ColumnInfo(name = "study_text")
    val studyText: String?,
)
