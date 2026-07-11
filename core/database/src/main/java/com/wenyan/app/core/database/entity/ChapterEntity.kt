package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 章节表 Entity（chapters）。
 *
 * 对应设计文档 4.1 节 chapters 表：
 * - id: 章节唯一标识
 * - subject_id: 所属科目（外键 subjects.id）
 * - parent_id: 父章节 ID（可空，支持多级章节树）
 * - title: 章节标题
 * - sort_order: 排序序号
 */
@Entity(
    tableName = "chapters",
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
        Index("parent_id"),
        Index("sort_order"),
    ],
)
data class ChapterEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "subject_id")
    val subjectId: String,

    @ColumnInfo(name = "parent_id")
    val parentId: String?,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)
