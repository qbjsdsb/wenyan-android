package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 3 → 4（NF-B / P0-E4 修复：FSRS 时钟回拨防护）。
 *
 * 背景：[com.wenyan.app.core.data.repository.SchedulingRepository] 与
 * [com.wenyan.app.core.data.repository.GraphRepositoryImpl] 用
 * `System.currentTimeMillis()` / `LocalDateTime.now()` 计算卡片到期时间与可提取性 R，
 * 用户改系统时间（手动调时 / 时区切换 / NTP 异常）会导致卡片"永久消失"或"无限到期"。
 *
 * 本迁移新增 `app_meta` 表（通用 key-value），存储 `last_known_timestamp_ms` 等
 * 应用级元数据，供 [com.wenyan.app.core.data.repository.ClockGuard] 检测时钟回拨。
 *
 * 表结构：
 * - `key` TEXT PRIMARY KEY NOT NULL
 * - `long_value` INTEGER NULL（时间戳类元数据）
 * - `string_value` TEXT NULL（文本类元数据）
 *
 * 幂等性：纯 DDL，多次执行结果一致（CREATE TABLE IF NOT EXISTS）。
 * 安全性：新增表，不影响现有数据，不触发 FK CASCADE。
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_meta (
                `key` TEXT NOT NULL PRIMARY KEY,
                long_value INTEGER,
                string_value TEXT
            )
            """.trimIndent(),
        )
    }
}
