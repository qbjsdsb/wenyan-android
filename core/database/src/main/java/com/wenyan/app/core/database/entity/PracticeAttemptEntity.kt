package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PracticeAttemptType { QUICK_RECALL, EXAM_OUTLINE, FULL_ANSWER, REPAIR;
    companion object { fun fromDb(value: String) = entries.firstOrNull { it.name == value } }
}

enum class PracticeSelfRating { AGAIN, HARD, GOOD, EASY;
    companion object { fun fromDb(value: String?) = entries.firstOrNull { it.name == value } }
}

enum class PracticeErrorReason {
    MEMORY_GAP, CONCEPT_CONFUSION, MISREAD_PROMPT, WEAK_STRUCTURE,
    WEAK_EVIDENCE, TIME_CONTROL, EXPRESSION;
    companion object { fun fromDb(value: String) = entries.firstOrNull { it.name == value } }
}

enum class PracticeRepairState { NONE, CANDIDATE, SCHEDULED, RESOLVED;
    companion object { fun fromDb(value: String) = entries.firstOrNull { it.name == value } ?: NONE }
}

@Entity(
    tableName = "practice_attempts",
    foreignKeys = [
        ForeignKey(
            entity = ExamQuestionEntity::class,
            parentColumns = ["id"], childColumns = ["question_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = KnowledgePointEntity::class,
            parentColumns = ["id"], childColumns = ["point_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = LearningUnitEntity::class,
            parentColumns = ["id"], childColumns = ["learning_unit_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("question_id"), Index("point_id"), Index("learning_unit_id"),
        Index("session_id"), Index("repair_state"), Index("completed_at"),
    ],
)
data class PracticeAttemptEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(name = "point_id") val pointId: String?,
    @ColumnInfo(name = "learning_unit_id") val learningUnitId: String?,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "attempt_type") val attemptType: String,
    @ColumnInfo(name = "user_keywords") val userKeywords: String,
    @ColumnInfo(name = "outline") val outline: String,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "revealed_at") val revealedAt: Long?,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
    @ColumnInfo(name = "self_rating") val selfRating: String?,
    @ColumnInfo(name = "error_reasons") val errorReasons: List<String>,
    @ColumnInfo(name = "repair_state") val repairState: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
