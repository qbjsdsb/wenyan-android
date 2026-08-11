package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_tasks",
    foreignKeys = [
        ForeignKey(
            entity = DailyPlanEntity::class,
            parentColumns = ["id"], childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LearningUnitEntity::class,
            parentColumns = ["id"], childColumns = ["learning_unit_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("plan_id"), Index("learning_unit_id"), Index("status"),
        Index(value = ["plan_id", "position"], unique = true),
        Index(value = ["plan_id", "stable_id"], unique = true),
    ],
)
data class DailyTaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plan_id") val planId: String,
    @ColumnInfo(name = "stable_id") val stableId: String,
    val position: Int,
    @ColumnInfo(name = "task_type") val taskType: String,
    @ColumnInfo(name = "content_id") val contentId: String?,
    @ColumnInfo(name = "learning_unit_id") val learningUnitId: String?,
    @ColumnInfo(name = "estimated_minutes") val estimatedMinutes: Int,
    val status: String,
    @ColumnInfo(name = "carried_from_task_id") val carriedFromTaskId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
