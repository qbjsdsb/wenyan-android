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
        Index("content_status"),
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

    /** Optional display title; legacy material remains untitled rather than being guessed. */
    @ColumnInfo(name = "title")
    val title: String? = null,

    /** Explicit related knowledge-point IDs encoded by the existing list converter. */
    @ColumnInfo(name = "related_point_ids")
    val relatedPointIds: List<String>? = null,

    @ColumnInfo(name = "content_status", defaultValue = "LEGACY_UNVERIFIED")
    val contentStatus: String = ContentReviewStatus.LEGACY_UNVERIFIED.name,
)
