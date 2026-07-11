package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 写作句式表 Entity（writing_patterns）。
 *
 * 对应设计文档 4.1 节 writing_patterns 表：
 * - id: 唯一标识
 * - name: 句式名称
 * - category: 类别 OPENING / ARGUMENT / ENDING（开头/论证/结尾）
 * - description: 描述
 * - template: 句式模板
 * - example: 示例
 * - applicable_genres: 适用文体，JSON 数组 List<String>
 * - is_builtin: 是否内置，默认 1
 * - created_at: 创建时间
 */
@Entity(
    tableName = "writing_patterns",
    indices = [
        Index("category"),
    ],
)
data class WritingPatternEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    /** 类别：OPENING / ARGUMENT / ENDING */
    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "template")
    val template: String,

    @ColumnInfo(name = "example")
    val example: String?,

    /** 适用文体列表，JSON 数组 */
    @ColumnInfo(name = "applicable_genres")
    val applicableGenres: List<String>?,

    @ColumnInfo(name = "is_builtin", defaultValue = "1")
    val isBuiltin: Int = 1,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
