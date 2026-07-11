package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 科目表 Entity（subjects）。
 *
 * 对应设计文档 4.1 节 subjects 表：
 * - id: 科目唯一标识（UUID 字符串）
 * - name: 科目名称（如"中国古代文学"）
 * - short_name: 科目简称（如"古文"）
 * - sort_order: 排序序号
 */
@Entity(
    tableName = "subjects",
    indices = [Index("sort_order")],
)
data class SubjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "short_name")
    val shortName: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)
