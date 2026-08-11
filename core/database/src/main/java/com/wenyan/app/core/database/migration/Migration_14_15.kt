package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_14_15_STATEMENTS = listOf(
    """CREATE TABLE IF NOT EXISTS `writing_sessions` (`id` TEXT NOT NULL, `exam_question_id` TEXT, `template_id` TEXT, `practice_attempt_id` TEXT, `mode` TEXT NOT NULL, `prompt_snapshot` TEXT NOT NULL, `prompt_analysis` TEXT NOT NULL, `thesis` TEXT NOT NULL, `outline_json` TEXT NOT NULL, `evidence_refs_json` TEXT NOT NULL, `body` TEXT NOT NULL, `state` TEXT NOT NULL, `target_duration_ms` INTEGER NOT NULL, `started_at` INTEGER, `elapsed_before_pause_ms` INTEGER NOT NULL, `paused_at` INTEGER, `last_saved_at` INTEGER NOT NULL, `completed_at` INTEGER, `self_assessment_json` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`exam_question_id`) REFERENCES `exam_questions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`template_id`) REFERENCES `answer_templates`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`practice_attempt_id`) REFERENCES `practice_attempts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)""",
    "CREATE INDEX IF NOT EXISTS `index_writing_sessions_exam_question_id` ON `writing_sessions` (`exam_question_id`)",
    "CREATE INDEX IF NOT EXISTS `index_writing_sessions_template_id` ON `writing_sessions` (`template_id`)",
    "CREATE INDEX IF NOT EXISTS `index_writing_sessions_practice_attempt_id` ON `writing_sessions` (`practice_attempt_id`)",
    "CREATE INDEX IF NOT EXISTS `index_writing_sessions_state` ON `writing_sessions` (`state`)",
    "CREATE INDEX IF NOT EXISTS `index_writing_sessions_updated_at` ON `writing_sessions` (`updated_at`)",
)
val MIGRATION_14_15: Migration = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) = MIGRATION_14_15_STATEMENTS.forEach(db::execSQL)
}
