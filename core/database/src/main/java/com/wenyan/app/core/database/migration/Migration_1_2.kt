package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 1 → 2（阶段3：FSRS调度接通）。
 *
 * 为 memo_records 表补充3个 FSRS 调度状态字段：
 * - elapsed_days:   距上次复习天数
 * - scheduled_days: 上次调度的间隔天数
 * - reps:           总复习次数（与 review_count 同步）
 *
 * 三列均带 NOT NULL DEFAULT 0，确保已有数据迁移后取默认值，
 * 与 [com.wenyan.app.core.database.entity.MemoRecordEntity] 中的 defaultValue 一致。
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE memo_records ADD COLUMN elapsed_days INTEGER NOT NULL DEFAULT 0",
        )
        database.execSQL(
            "ALTER TABLE memo_records ADD COLUMN scheduled_days INTEGER NOT NULL DEFAULT 0",
        )
        database.execSQL(
            "ALTER TABLE memo_records ADD COLUMN reps INTEGER NOT NULL DEFAULT 0",
        )
    }
}
