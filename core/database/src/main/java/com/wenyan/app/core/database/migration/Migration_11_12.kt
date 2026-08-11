package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_11_12_STATEMENTS = listOf(
    """CREATE TABLE IF NOT EXISTS `learning_units` (`id` TEXT NOT NULL, `point_id` TEXT NOT NULL, `unit_type` TEXT NOT NULL, `position` INTEGER NOT NULL, `prompt` TEXT NOT NULL, `answer` TEXT NOT NULL, `active` INTEGER NOT NULL DEFAULT 1, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`point_id`) REFERENCES `knowledge_points`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""",
    "CREATE INDEX IF NOT EXISTS `index_learning_units_point_id` ON `learning_units` (`point_id`)",
    "CREATE INDEX IF NOT EXISTS `index_learning_units_active` ON `learning_units` (`active`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_learning_units_point_id_unit_type_position` ON `learning_units` (`point_id`, `unit_type`, `position`)",
    """CREATE TABLE IF NOT EXISTS `learning_unit_records` (`learning_unit_id` TEXT NOT NULL, `state` TEXT NOT NULL, `stability` REAL NOT NULL DEFAULT 0.0, `difficulty` REAL NOT NULL DEFAULT 5.0, `last_review_at` INTEGER NOT NULL, `next_review_at` INTEGER NOT NULL, `review_count` INTEGER NOT NULL DEFAULT 0, `fail_count` INTEGER NOT NULL DEFAULT 0, `elapsed_days` INTEGER NOT NULL DEFAULT 0, `scheduled_days` INTEGER NOT NULL DEFAULT 0, `reps` INTEGER NOT NULL DEFAULT 0, `in_priority_queue` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`learning_unit_id`), FOREIGN KEY(`learning_unit_id`) REFERENCES `learning_units`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""",
    "CREATE INDEX IF NOT EXISTS `index_learning_unit_records_next_review_at` ON `learning_unit_records` (`next_review_at`)",
    "CREATE INDEX IF NOT EXISTS `index_learning_unit_records_in_priority_queue` ON `learning_unit_records` (`in_priority_queue`)",
    "ALTER TABLE `review_logs` ADD COLUMN `learning_unit_id` TEXT DEFAULT NULL REFERENCES `learning_units`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL",
    "CREATE INDEX IF NOT EXISTS `index_review_logs_learning_unit_id` ON `review_logs` (`learning_unit_id`)",
)

val MIGRATION_11_12: Migration = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_11_12_STATEMENTS.forEach(db::execSQL)
    }
}
