package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_12_13_STATEMENTS = listOf(
    """CREATE TABLE IF NOT EXISTS `daily_plans` (`id` TEXT NOT NULL, `plan_date` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `exam_scheme_year` INTEGER, `settings_snapshot` TEXT NOT NULL, `content_version` TEXT NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))""",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_plans_plan_date` ON `daily_plans` (`plan_date`)",
    "CREATE INDEX IF NOT EXISTS `index_daily_plans_status` ON `daily_plans` (`status`)",
    """CREATE TABLE IF NOT EXISTS `daily_tasks` (`id` TEXT NOT NULL, `plan_id` TEXT NOT NULL, `stable_id` TEXT NOT NULL, `position` INTEGER NOT NULL, `task_type` TEXT NOT NULL, `content_id` TEXT, `learning_unit_id` TEXT, `estimated_minutes` INTEGER NOT NULL, `status` TEXT NOT NULL, `carried_from_task_id` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`plan_id`) REFERENCES `daily_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`learning_unit_id`) REFERENCES `learning_units`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)""",
    "CREATE INDEX IF NOT EXISTS `index_daily_tasks_plan_id` ON `daily_tasks` (`plan_id`)",
    "CREATE INDEX IF NOT EXISTS `index_daily_tasks_learning_unit_id` ON `daily_tasks` (`learning_unit_id`)",
    "CREATE INDEX IF NOT EXISTS `index_daily_tasks_status` ON `daily_tasks` (`status`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_tasks_plan_id_position` ON `daily_tasks` (`plan_id`, `position`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_tasks_plan_id_stable_id` ON `daily_tasks` (`plan_id`, `stable_id`)",
)

val MIGRATION_12_13: Migration = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) = MIGRATION_12_13_STATEMENTS.forEach(db::execSQL)
}
