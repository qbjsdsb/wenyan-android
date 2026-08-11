package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 8 → 9（v0.9.22：为存量 v8 用户补建错题本复合索引）。
 *
 * **背景**：WrongAnswerEntity 的 @Entity(indices=...) 自创建（v0.9.2）起就声明了
 * [point_id, source] 与 [exam_question_id, source] 两个复合索引，但 MIGRATION_7_8
 * （v0.9.4，7→8）遗漏创建，且 8.json schema 也长期未同步（v0.9.22 才修正 schema 文件）。
 *
 * **影响范围**：
 * - 新装用户：通过最新 schema 全量建表，wrong_answers 有全部 6 个索引，无此问题
 * - v7 直升 v8 用户：若在 v0.9.22 之后升级，会走修正后的 MIGRATION_7_8（已补索引）
 * - **v8 存量用户（v0.9.4~v0.9.21 已升级到 v8 的用户）：数据库版本不变不会重跑 7→8，
 *   wrong_answers 表永远缺少这两个复合索引** —— 本迁移正是为这批用户补建
 *
 * **变更**：CREATE INDEX IF NOT EXISTS 两个复合索引（不修改任何数据，完全安全）：
 * - index_wrong_answers_point_id_source ON wrong_answers(point_id, source)
 * - index_wrong_answers_exam_question_id_source ON wrong_answers(exam_question_id, source)
 *
 * 用于 WrongAnswerDao.findUnresolvedByPointAndSource / findUnresolvedByExamQuestionAndSource
 * （每次答错都会执行的精确匹配查询）在存量用户上走复合索引，避免单列索引后逐行过滤 source，
 * 数据量增长后性能退化。
 *
 * **幂等性**：幂等（CREATE INDEX IF NOT EXISTS 重复执行无害）。
 *
 * @see com.wenyan.app.core.database.entity.WrongAnswerEntity
 * @see com.wenyan.app.core.database.WenyanDatabase
 */
internal val MIGRATION_8_9_STATEMENTS = listOf(
    "CREATE INDEX IF NOT EXISTS index_wrong_answers_point_id_source ON wrong_answers(point_id, source)",
    "CREATE INDEX IF NOT EXISTS index_wrong_answers_exam_question_id_source ON wrong_answers(exam_question_id, source)",
)

val MIGRATION_8_9: Migration = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        MIGRATION_8_9_STATEMENTS.forEach(database::execSQL)
    }
}
