package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_plans",
    indices = [Index(value = ["plan_date"], unique = true), Index("status")],
)
data class DailyPlanEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plan_date") val planDate: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "exam_scheme_year") val examSchemeYear: Int?,
    @ColumnInfo(name = "settings_snapshot") val settingsSnapshot: String,
    @ColumnInfo(name = "content_version") val contentVersion: String,
    val status: String,
)
