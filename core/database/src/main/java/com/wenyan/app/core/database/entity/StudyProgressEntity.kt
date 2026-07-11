package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 学习进度表 Entity（study_progress）。
 *
 * 对应设计文档 4.1 节 study_progress 表：
 * - id: 主键（固定单行记录，如 "default"）
 * - last_point_id: 上次学习的知识点 ID
 * - last_visited_at: 上次访问时间
 * - total_study_time: 累计学习时长（秒）
 * - streak_days: 连续学习天数
 * - last_check_in: 上次签到时间
 */
@Entity(tableName = "study_progress")
data class StudyProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "last_point_id")
    val lastPointId: String?,

    @ColumnInfo(name = "last_visited_at")
    val lastVisitedAt: Long?,

    @ColumnInfo(name = "total_study_time")
    val totalStudyTime: Int?,

    @ColumnInfo(name = "streak_days")
    val streakDays: Int?,

    @ColumnInfo(name = "last_check_in")
    val lastCheckIn: Long?,
)
