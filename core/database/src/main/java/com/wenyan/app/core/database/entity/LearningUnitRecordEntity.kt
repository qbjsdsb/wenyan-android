package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learning_unit_records",
    foreignKeys = [
        ForeignKey(
            entity = LearningUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["learning_unit_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("next_review_at"), Index("in_priority_queue")],
)
data class LearningUnitRecordEntity(
    @PrimaryKey @ColumnInfo(name = "learning_unit_id") val learningUnitId: String,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "stability", defaultValue = "0.0") val stability: Float = 0f,
    @ColumnInfo(name = "difficulty", defaultValue = "5.0") val difficulty: Float = 5f,
    @ColumnInfo(name = "last_review_at") val lastReviewAt: Long,
    @ColumnInfo(name = "next_review_at") val nextReviewAt: Long,
    @ColumnInfo(name = "review_count", defaultValue = "0") val reviewCount: Int = 0,
    @ColumnInfo(name = "fail_count", defaultValue = "0") val failCount: Int = 0,
    @ColumnInfo(name = "elapsed_days", defaultValue = "0") val elapsedDays: Int = 0,
    @ColumnInfo(name = "scheduled_days", defaultValue = "0") val scheduledDays: Int = 0,
    @ColumnInfo(name = "reps", defaultValue = "0") val reps: Int = 0,
    @ColumnInfo(name = "in_priority_queue", defaultValue = "0") val inPriorityQueue: Int = 0,
)
