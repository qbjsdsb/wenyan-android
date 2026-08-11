package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class WritingSessionMode { OUTLINE_10_MIN, MICRO_30_MIN, FULL_TIMED;
    companion object { fun fromDb(value: String) = entries.firstOrNull { it.name == value } }
}
enum class WritingSessionState { DRAFT, RUNNING, PAUSED, COMPLETED, DISCARDED;
    companion object { fun fromDb(value: String) = entries.firstOrNull { it.name == value } }
}

@Entity(
    tableName = "writing_sessions",
    foreignKeys = [
        ForeignKey(entity = ExamQuestionEntity::class, parentColumns = ["id"], childColumns = ["exam_question_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = AnswerTemplateEntity::class, parentColumns = ["id"], childColumns = ["template_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = PracticeAttemptEntity::class, parentColumns = ["id"], childColumns = ["practice_attempt_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("exam_question_id"), Index("template_id"), Index("practice_attempt_id"), Index("state"), Index("updated_at")],
)
data class WritingSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "exam_question_id") val examQuestionId: String?,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "practice_attempt_id") val practiceAttemptId: String?,
    val mode: String,
    @ColumnInfo(name = "prompt_snapshot") val promptSnapshot: String,
    @ColumnInfo(name = "prompt_analysis") val promptAnalysis: String,
    val thesis: String,
    @ColumnInfo(name = "outline_json") val outlineJson: String,
    @ColumnInfo(name = "evidence_refs_json") val evidenceRefsJson: String,
    val body: String,
    val state: String,
    @ColumnInfo(name = "target_duration_ms") val targetDurationMs: Long,
    @ColumnInfo(name = "started_at") val startedAt: Long?,
    @ColumnInfo(name = "elapsed_before_pause_ms") val elapsedBeforePauseMs: Long,
    @ColumnInfo(name = "paused_at") val pausedAt: Long?,
    @ColumnInfo(name = "last_saved_at") val lastSavedAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "self_assessment_json") val selfAssessmentJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
