package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 答题模板表 Entity（answer_templates）。
 *
 * 对应设计文档 4.1 节 answer_templates 表：
 * - id: 唯一标识
 * - question_type: 适用题型 TERM_EXPLANATION / SHORT_ANSWER / ESSAY / WRITING
 * - name: 模板名称
 * - structure: 模板结构，JSON 字符串（结构复杂，业务层解析）
 * - applicable_tags: 适用标签，JSON 数组 List<String>
 * - example_usage: 示例用法
 * - score_range_min / score_range_max: 分数范围
 * - word_limit_min / word_limit_max: 字数范围
 * - is_builtin: 是否内置，默认 1
 * - created_at / updated_at: 时间戳
 */
@Entity(
    tableName = "answer_templates",
    indices = [
        Index("question_type"),
    ],
)
data class AnswerTemplateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** 适用题型：TERM_EXPLANATION / SHORT_ANSWER / ESSAY / WRITING */
    @ColumnInfo(name = "question_type")
    val questionType: String,

    @ColumnInfo(name = "name")
    val name: String,

    /** 模板结构，JSON 字符串（结构复杂，业务层解析） */
    @ColumnInfo(name = "structure")
    val structure: String,

    /** 适用标签列表，JSON 数组 */
    @ColumnInfo(name = "applicable_tags")
    val applicableTags: List<String>?,

    @ColumnInfo(name = "example_usage")
    val exampleUsage: String?,

    @ColumnInfo(name = "score_range_min")
    val scoreRangeMin: Int?,

    @ColumnInfo(name = "score_range_max")
    val scoreRangeMax: Int?,

    @ColumnInfo(name = "word_limit_min")
    val wordLimitMin: Int?,

    @ColumnInfo(name = "word_limit_max")
    val wordLimitMax: Int?,

    @ColumnInfo(name = "is_builtin", defaultValue = "1")
    val isBuiltin: Int = 1,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
