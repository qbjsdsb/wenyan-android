package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 2 → 3（NF-D1 修复：回填 reps 字段）。
 *
 * 背景：[MIGRATION_1_2] 为 memo_records 补了 reps 字段（DEFAULT 0），但未回填，
 * 导致 v0.2.0 已发版用户升级后，所有已有 memo_records 的 reps = 0，
 * 而 review_count 可能 > 0。FSRS 调度若依赖 reps 判断复习阶段
 * （新卡 vs 复习卡），会把老卡片误判为新卡，复习间隔重置。
 *
 * 本迁移用 review_count 回填 reps，仅更新 reps = 0 且 review_count > 0 的行，
 * 避免覆盖用户在 v2 期间已正常累积的 reps 值。
 *
 * 幂等性：多次执行结果一致（reps 已 = review_count 后条件不再满足）。
 * 安全性：纯 UPDATE，无 schema 变更，不触发 FK CASCADE。
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 回填 reps：仅更新 reps = 0 且 review_count > 0 的行
        // 避免覆盖 v2 期间已正常累积的 reps 值
        database.execSQL(
            """
            UPDATE memo_records
            SET reps = review_count
            WHERE reps = 0 AND review_count > 0
            """.trimIndent(),
        )
    }
}
