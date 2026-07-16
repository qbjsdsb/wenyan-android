package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 4 → 5（P1 大型任务：NF-PP4 + NF-PP5 + NF-PP6 合并 schema 变更）。
 *
 * 三部分变更（单次 Migration 完成）：
 *
 * **Part A — NF-PP4 废弃 history JSON 双写**
 * 删除 `memo_records.history` 列。SQLite 不支持 DROP COLUMN，需重建表（5 步）。
 * `history` 字段为全项目零读取的死数据（审计文档 NF-PP4），`review_logs` 表是唯一有效复习历史源。
 *
 * **Part B — NF-PP6 AI 对话持久化**
 * 合并 `chat_history` + `ai_conversations` 两张死代码表 → `chat_conversations`（对话元数据）+ `chat_messages`（消息内容，FK→对话）。
 * 存量数据（预期 0 行，因两表无 Repository 写入）汇总到一条 `migrated_legacy` 对话占位记录。
 *
 * **Part C — NF-PP5 错题本**
 * 新增 `wrong_answers` 表，支持 Cards AGAIN + Quiz 答错 双来源记录。
 *
 * 幂等性说明：Part A 涉及 DROP TABLE，不幂等；Part B/C 使用 IF NOT EXISTS。
 * 安全性：Part A 重建表保留全部数据（仅删 history 列）；Part B 旧表无有效数据；Part C 纯新增。
 *
 * @see com.wenyan.app.core.database.WenyanDatabase
 */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ============ Part A: NF-PP4 删除 memo_records.history 列 ============
        // SQLite 不支持 DROP COLUMN，需重建表（5 步：CREATE new → INSERT SELECT → DROP old → RENAME → 重建索引）
        // 新表结构必须与 Room 从新 MemoRecordEntity（无 history 字段）生成的 schema 完全一致。
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `memo_records_new` (
                `point_id` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `stability` REAL NOT NULL DEFAULT 0.0,
                `difficulty` REAL NOT NULL DEFAULT 5.0,
                `last_review_at` INTEGER NOT NULL,
                `next_review_at` INTEGER NOT NULL,
                `review_count` INTEGER NOT NULL DEFAULT 0,
                `fail_count` INTEGER NOT NULL DEFAULT 0,
                `elapsed_days` INTEGER NOT NULL DEFAULT 0,
                `scheduled_days` INTEGER NOT NULL DEFAULT 0,
                `reps` INTEGER NOT NULL DEFAULT 0,
                `in_priority_queue` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`point_id`),
                FOREIGN KEY(`point_id`) REFERENCES `knowledge_points`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO `memo_records_new` (
                `point_id`, `state`, `stability`, `difficulty`, `last_review_at`,
                `next_review_at`, `review_count`, `fail_count`, `elapsed_days`,
                `scheduled_days`, `reps`, `in_priority_queue`
            )
            SELECT `point_id`, `state`, `stability`, `difficulty`, `last_review_at`,
                   `next_review_at`, `review_count`, `fail_count`, `elapsed_days`,
                   `scheduled_days`, `reps`, `in_priority_queue`
            FROM `memo_records`
            """.trimIndent(),
        )
        database.execSQL("DROP TABLE `memo_records`")
        database.execSQL("ALTER TABLE `memo_records_new` RENAME TO `memo_records`")
        // 重建索引（必须与 MemoRecordEntity indices 完全一致，含名称）
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_memo_records_next_review_at` ON `memo_records`(`next_review_at`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_memo_records_in_priority_queue` ON `memo_records`(`in_priority_queue`)",
        )

        // ============ Part B: NF-PP6 合并 chat_history + ai_conversations ============
        // 1. 创建新表 chat_conversations（对话元数据）
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chat_conversations` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `api_config_id` TEXT,
                `model` TEXT,
                `message_count` INTEGER NOT NULL DEFAULT 0,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        // 2. 创建新表 chat_messages（消息内容，FK→对话 ON DELETE CASCADE）
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chat_messages` (
                `id` TEXT NOT NULL,
                `conversation_id` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `content_source` TEXT,
                `stage` TEXT,
                `references_json` TEXT,
                `context_screen` TEXT,
                `context_title` TEXT,
                `tokens_used` INTEGER,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`conversation_id`) REFERENCES `chat_conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        // 3. 索引（与 Entity indices 一致）
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chat_messages_conversation_id` ON `chat_messages`(`conversation_id`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chat_conversations_updated_at` ON `chat_conversations`(`updated_at`)",
        )
        // 4. 迁移存量数据（预期 0 行，因两表无 Repository 写入）
        //    若有历史数据，汇总到一条 migrated_legacy 占位对话
        database.execSQL(
            """
            INSERT OR IGNORE INTO `chat_conversations` (
                `id`, `title`, `api_config_id`, `model`, `message_count`, `created_at`, `updated_at`
            )
            SELECT 'migrated_legacy', '历史对话（迁移）', NULL, NULL, 0, 0, 0
            WHERE EXISTS (SELECT 1 FROM `chat_history` LIMIT 1)
               OR EXISTS (SELECT 1 FROM `ai_conversations` LIMIT 1)
            """.trimIndent(),
        )
        // 5. 删除旧表（索引随表自动删除）
        database.execSQL("DROP TABLE IF EXISTS `chat_history`")
        database.execSQL("DROP TABLE IF EXISTS `ai_conversations`")

        // ============ Part C: NF-PP5 新增 wrong_answers 表 ============
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wrong_answers` (
                `id` TEXT NOT NULL,
                `point_id` TEXT,
                `exam_question_id` TEXT,
                `user_answer` TEXT NOT NULL,
                `correct_answer` TEXT,
                `source` TEXT NOT NULL,
                `wrong_count` INTEGER NOT NULL DEFAULT 1,
                `last_wrong_at` INTEGER NOT NULL,
                `resolved_at` INTEGER,
                `ai_explanation` TEXT,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`point_id`) REFERENCES `knowledge_points`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`exam_question_id`) REFERENCES `exam_questions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_wrong_answers_point_id` ON `wrong_answers`(`point_id`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_wrong_answers_exam_question_id` ON `wrong_answers`(`exam_question_id`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_wrong_answers_resolved_at` ON `wrong_answers`(`resolved_at`)",
        )
    }
}
