package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_13_14_STATEMENTS = listOf(
    """CREATE TABLE IF NOT EXISTS `practice_attempts` (`id` TEXT NOT NULL, `question_id` TEXT NOT NULL, `point_id` TEXT, `learning_unit_id` TEXT, `session_id` TEXT, `attempt_type` TEXT NOT NULL, `user_keywords` TEXT NOT NULL, `outline` TEXT NOT NULL, `body` TEXT NOT NULL, `started_at` INTEGER NOT NULL, `revealed_at` INTEGER, `completed_at` INTEGER, `elapsed_ms` INTEGER NOT NULL, `self_rating` TEXT, `error_reasons` TEXT NOT NULL, `repair_state` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`question_id`) REFERENCES `exam_questions`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`point_id`) REFERENCES `knowledge_points`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`learning_unit_id`) REFERENCES `learning_units`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)""",
    "CREATE INDEX IF NOT EXISTS `index_practice_attempts_question_id` ON `practice_attempts` (`question_id`)",
    "CREATE INDEX IF NOT EXISTS `index_practice_attempts_point_id` ON `practice_attempts` (`point_id`)",
    "CREATE INDEX IF NOT EXISTS `index_practice_attempts_learning_unit_id` ON `practice_attempts` (`learning_unit_id`)",
    "CREATE INDEX IF NOT EXISTS `index_practice_attempts_session_id` ON `practice_attempts` (`session_id`)",
    "CREATE INDEX IF NOT EXISTS `index_practice_attempts_repair_state` ON `practice_attempts` (`repair_state`)",
    "CREATE INDEX IF NOT EXISTS `index_practice_attempts_completed_at` ON `practice_attempts` (`completed_at`)",
)

val MIGRATION_13_14: Migration = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) = MIGRATION_13_14_STATEMENTS.forEach(db::execSQL)
}
