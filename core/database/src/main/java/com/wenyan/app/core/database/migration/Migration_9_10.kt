package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 9 → 10（v0.9.24：为筛选查询补索引）。
 *
 * **背景**：以下查询随数据量增长会全表扫描（DAO 已存在，但列无索引）：
 * - [com.wenyan.app.core.database.dao.ExamQuestionDao.observeByQuestionType]
 *   / [com.wenyan.app.core.database.dao.ExamQuestionDao.observeAllEssays]：WHERE question_type = ?
 * - [com.wenyan.app.core.database.dao.ExamQuestionDao.observeByAnswerStatus]：WHERE answer_status = ?
 * - [com.wenyan.app.core.database.dao.KnowledgePointDao.observeByContentSource]：WHERE content_source = ?
 *
 * **变更**：CREATE INDEX IF NOT EXISTS 3 个索引（不修改任何数据，完全安全）：
 * - index_exam_questions_question_type ON exam_questions(question_type)
 * - index_exam_questions_answer_status ON exam_questions(answer_status)
 * - index_knowledge_points_content_source ON knowledge_points(content_source)
 *
 * **幂等性**：幂等（CREATE INDEX IF NOT EXISTS 重复执行无害）。
 *
 * @see com.wenyan.app.core.database.entity.ExamQuestionEntity
 * @see com.wenyan.app.core.database.entity.KnowledgePointEntity
 * @see com.wenyan.app.core.database.WenyanDatabase
 */
val MIGRATION_9_10: Migration = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_exam_questions_question_type ON exam_questions(question_type)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_exam_questions_answer_status ON exam_questions(answer_status)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_knowledge_points_content_source ON knowledge_points(content_source)",
        )
    }
}
