package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 5 → 6（v0.7.6：删除 exam_questions.sample_essay 列）。
 *
 * **背景**：seed_data.json v2.9.0 已删除所有 485 道真题的 `sample_essay` 字段
 * （范文冗余，answer_framework 答题框架已足够；总字符 21.6 万）。
 * 现删除数据库列完成代码层清理。
 *
 * **变更**：仅删除 `exam_questions.sample_essay` 一列。
 *
 * **方法**：SQLite 不支持 DROP COLUMN（旧版本限制），需重建表（5 步）：
 * 1. CREATE new（无 sample_essay 列）
 * 2. INSERT SELECT（迁移数据，排除 sample_essay）
 * 3. DROP old
 * 4. RENAME new → old
 * 5. 重建索引（随表 DROP 自动删除）
 *
 * 新表结构必须与 Room 从新 ExamQuestionEntity（无 sampleEssay 字段）生成的 schema 完全一致。
 * 参见 schema 5.json 中 exam_questions 的 createSql（去掉 `, `sample_essay` TEXT` 部分）。
 *
 * **安全性**：重建表保留全部数据（仅删 sample_essay 列）。
 * **幂等性**：不幂等（涉及 DROP TABLE），但 Room 仅在 5→6 时调用一次。
 *
 * @see com.wenyan.app.core.database.WenyanDatabase
 * @see com.wenyan.app.core.database.entity.ExamQuestionEntity
 */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 创建新表（无 sample_essay 列，结构与新 ExamQuestionEntity 一致）
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exam_questions_new` (
                `id` TEXT NOT NULL,
                `year` INTEGER NOT NULL,
                `subject_id` TEXT NOT NULL,
                `question_type` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `score` INTEGER NOT NULL,
                `angle` TEXT,
                `related_point_ids` TEXT,
                `answer_framework` TEXT,
                `notes` TEXT,
                `created_at` INTEGER NOT NULL,
                `exam_paper_code` TEXT,
                `answer_status` TEXT,
                `material_text` TEXT,
                `source_file` TEXT,
                `source_page` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`subject_id`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        // 2. 迁移数据（排除 sample_essay 列）
        database.execSQL(
            """
            INSERT INTO `exam_questions_new` (
                `id`, `year`, `subject_id`, `question_type`, `content`, `score`,
                `angle`, `related_point_ids`, `answer_framework`, `notes`, `created_at`,
                `exam_paper_code`, `answer_status`, `material_text`, `source_file`, `source_page`
            )
            SELECT
                `id`, `year`, `subject_id`, `question_type`, `content`, `score`,
                `angle`, `related_point_ids`, `answer_framework`, `notes`, `created_at`,
                `exam_paper_code`, `answer_status`, `material_text`, `source_file`, `source_page`
            FROM `exam_questions`
            """.trimIndent(),
        )

        // 3. 替换表
        database.execSQL("DROP TABLE `exam_questions`")
        database.execSQL("ALTER TABLE `exam_questions_new` RENAME TO `exam_questions`")

        // 4. 重建索引（必须与 ExamQuestionEntity indices 完全一致，含名称）
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exam_questions_subject_id` ON `exam_questions`(`subject_id`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exam_questions_year` ON `exam_questions`(`year`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exam_questions_exam_paper_code` ON `exam_questions`(`exam_paper_code`)",
        )
    }
}
