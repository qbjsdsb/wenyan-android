package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 科目代码变动历史表 Entity（exam_code_history）。
 *
 * Spec 新增表（spec 第 24 行），设计文档无此表：
 * - id: 唯一标识
 * - exam_code: 试卷代码（如 610 / 801 / 805 / 806）
 * - subject_name: 科目名称
 * - valid_from_year: 生效起始年份
 * - valid_to_year: 生效结束年份（可空，表示至今有效）
 * - direction: 方向（专一 / 专二）
 * - created_at: 创建时间
 */
@Entity(
    tableName = "exam_code_history",
    indices = [
        Index("exam_code"),
        Index("valid_from_year"),
    ],
)
data class ExamCodeHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** 试卷代码：610 / 801 / 805 / 806 等 */
    @ColumnInfo(name = "exam_code")
    val examCode: String,

    @ColumnInfo(name = "subject_name")
    val subjectName: String,

    @ColumnInfo(name = "valid_from_year")
    val validFromYear: Int,

    @ColumnInfo(name = "valid_to_year")
    val validToYear: Int?,

    /** 方向：专一 / 专二 */
    @ColumnInfo(name = "direction")
    val direction: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
