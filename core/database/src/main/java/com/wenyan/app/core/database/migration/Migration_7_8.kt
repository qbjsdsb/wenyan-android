package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 7 → 8（v0.9.4：错题本接入 FSRS 调度）。
 *
 * **背景**：错题本原仅展示列表+手动"标记解决"，无间隔重复调度。
 * 用户可能遗忘错题、重复犯错。v0.9.4 为 wrong_answers 表添加 10 个
 * `sched_*` 前缀的 FSRS 调度字段，复用 FSRS-6 算法 + TIER_FRAMEWORK 档位，
 * 实现错题的间隔重复复习。
 *
 * **设计决策**（ADR 详见 docs/design/adr-002-wrong-answer-fsrs.md）：
 * - 方案 B：在 wrong_answers 表添加 FSRS 字段（而非复用 memo_records 或新建表）
 * - 原因：memo_records PK 是 point_id FK→knowledge_points，真题来源错题无 pointId
 *   会破坏 FK；新建表对 1:1 关系过度规范化
 *
 * **变更**：ALTER TABLE wrong_answers ADD COLUMN 10 个 sched_* 字段 + 3 个索引。
 *
 * **安全性**：
 * - 所有新列有 defaultValue，已有错题记录自动获得默认值（NEW 状态，立即到期）
 * - 不删除任何现有列，不修改现有数据
 * - 已有用户的错题记录完整保留，新增调度字段从 NEW 状态开始
 *
 * **v0.9.22 补充（P2-2）**：补建两个复合索引。
 * WrongAnswerEntity 的 @Entity(indices=...) 自创建起就声明了
 * [point_id, source] 与 [exam_question_id, source] 两个复合索引，但本迁移遗漏创建，
 * 导致升级用户的 wrong_answers 表比新装用户少两个索引：
 * WrongAnswerDao.findUnresolvedByPointAndSource / findUnresolvedByExamQuestionAndSource
 * （每次答错都执行）在升级用户上只能走单列索引再逐行过滤 source，数据增长后性能退化。
 * 新装用户通过 8.json 全量建表有 6 个索引，此处补齐后升级用户与之对齐。
 *
 * **幂等性**：不幂等（ALTER TABLE ADD COLUMN 重复执行会报"duplicate column"）。
 * Room 仅在 7→8 时调用一次。
 *
 * @see com.wenyan.app.core.database.entity.WrongAnswerEntity
 * @see com.wenyan.app.core.database.WenyanDatabase
 */
val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 添加 10 个 FSRS 调度字段（全部有 defaultValue，已有记录自动填充）
        database.execSQL(
            "ALTER TABLE wrong_answers ADD COLUMN sched_state TEXT NOT NULL DEFAULT 'NEW'",
        )
        database.execSQL(
            "ALTER TABLE wrong_answers ADD COLUMN sched_stability REAL NOT NULL DEFAULT 0.0",
        )
        database.execSQL(
            "ALTER TABLE wrong_answers ADD COLUMN sched_difficulty REAL NOT NULL DEFAULT 5.0",
        )
        database.execSQL(
            "ALTER TABLE wrong_answers ADD COLUMN sched_last_review_at INTEGER NOT NULL DEFAULT 0",
        )
        database.execSQL(
            "ALTER TABLE wrong_answers ADD COLUMN sched_next_review_at INTEGER NOT NULL DEFAULT 0",
        )
        database.execSQL(
            "ALTER TABLE wrong_answers ADD COLUMN sched_review_count INTEGER NOT NULL DEFAULT 0",
        )
        database.execSQL(
            "ALTER TABLE wrong_answers ADD COLUMN sched_lapses INTEGER NOT NULL DEFAULT 0",
        )
        database.execSQL(
            "ALTER TABLE wrong_answers ADD COLUMN sched_elapsed_days INTEGER NOT NULL DEFAULT 0",
        )
        database.execSQL(
            "ALTER TABLE wrong_answers ADD COLUMN sched_scheduled_days INTEGER NOT NULL DEFAULT 0",
        )
        database.execSQL(
            "ALTER TABLE wrong_answers ADD COLUMN sched_reps INTEGER NOT NULL DEFAULT 0",
        )

        // 2. 添加 sched_next_review_at 索引（用于"待复习"查询：WHERE sched_next_review_at <= now）
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_wrong_answers_sched_next_review_at ON wrong_answers(sched_next_review_at)",
        )

        // 3. v0.9.22 补充：两个复合索引（与 WrongAnswerEntity @Entity(indices=...) 对齐）。
        // 用于 WrongAnswerDao.findUnresolvedByPointAndSource / findUnresolvedByExamQuestionAndSource
        // 的精确匹配（point_id + source / exam_question_id + source），避免单列索引后逐行过滤。
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_wrong_answers_point_id_source ON wrong_answers(point_id, source)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_wrong_answers_exam_question_id_source ON wrong_answers(exam_question_id, source)",
        )
    }
}
