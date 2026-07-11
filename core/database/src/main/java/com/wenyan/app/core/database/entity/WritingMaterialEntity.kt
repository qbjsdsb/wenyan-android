package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 写作素材表 Entity（writing_materials）。
 *
 * 对应设计文档 4.1 节 writing_materials 表：
 * - id: 唯一标识
 * - category: 素材类别 QUOTE / THEORY / EVIDENCE / TEMPLATE / ESSAY
 * - sub_category: 子类别
 * - content: 素材正文
 * - source: 来源
 * - tags: 标签（普通字符串，非 JSON 数组）
 * - created_at: 创建时间
 */
@Entity(
    tableName = "writing_materials",
    indices = [
        Index("category"),
    ],
)
data class WritingMaterialEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** 素材类别：QUOTE / THEORY / EVIDENCE / TEMPLATE / ESSAY */
    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "sub_category")
    val subCategory: String?,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "source")
    val source: String?,

    @ColumnInfo(name = "tags")
    val tags: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
