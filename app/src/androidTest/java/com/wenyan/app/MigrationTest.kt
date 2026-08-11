package com.wenyan.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.migration.MIGRATION_8_9
import com.wenyan.app.core.database.migration.MIGRATION_9_10
import com.wenyan.app.core.database.migration.MIGRATION_10_11
import com.wenyan.app.core.database.migration.MIGRATION_11_12
import com.wenyan.app.core.database.migration.MIGRATION_12_13
import com.wenyan.app.core.database.migration.MIGRATION_13_14
import com.wenyan.app.core.database.migration.MIGRATION_14_15
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room 数据库迁移测试（v0.9.24 新增）。
 *
 * 用 [MigrationTestHelper] 从历史 schema JSON 建库，执行迁移后
 * [MigrationTestHelper.runMigrationsAndValidate] 会校验迁移结果与最新 schema
 * 一致（含索引/外键/列，缺索引会在测试中暴露——此前 8→9 补复合索引
 * 就是靠人工发现，这里自动化兜底）。
 *
 * 需要 Android 模拟器/设备运行（androidTest）。
 *
 * 覆盖：
 * - 8→9：补建 wrong_answers 两个复合索引（MIGRATION_7_8 遗漏的历史修复）
 * - 9→10：补建 exam_questions/knowledge_points 三个筛选索引
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WenyanDatabase::class.java,
    )

    /**
     * 8→9：存量 v8 用户升级后，wrong_answers 应有 6 个索引（含两个复合索引）。
     */
    @Test
    fun migrate8To9_wrongAnswersHasAllIndices() {
        // 从 8.json 建 v8 库
        helper.createDatabase(TEST_DB, 8).use { db ->
            // 建库后立即关闭，交给迁移
        }
        // 执行 8→9 并校验与 9.json 一致（索引/列/外键）
        helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)
    }

    /**
     * 9→10：存量 v9 用户升级后，exam_questions/knowledge_points 应有新筛选索引。
     */
    @Test
    fun migrate9To10_filterIndicesCreated() {
        helper.createDatabase(TEST_DB, 9).use { }
        helper.runMigrationsAndValidate(TEST_DB, 10, true, MIGRATION_9_10)
    }

    @Test
    fun migrate10To11_provenanceSchemaCreated() {
        helper.createDatabase(TEST_DB, 10).use { db ->
            db.execSQL("INSERT INTO subjects (id, name, short_name, sort_order) VALUES ('migration-subject', 'subject', 's', 1)")
            db.execSQL("INSERT INTO chapters (id, subject_id, title, sort_order) VALUES ('migration-chapter', 'migration-subject', 'chapter', 1)")
            db.execSQL(
                """INSERT INTO knowledge_points
                    (id, chapter_id, title, core_conclusion, full_content, exam_frequency, created_at, updated_at)
                    VALUES ('migration-point', 'migration-chapter', 'title', 'conclusion', 'content', 'HIGH', 1, 1)""",
            )
        }
        helper.runMigrationsAndValidate(TEST_DB, 11, true, MIGRATION_10_11).use { db ->
            db.query("SELECT content_status FROM knowledge_points WHERE id = 'migration-point'").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getString(0) == "LEGACY_UNVERIFIED")
            }
        }
    }

    @Test
    fun migrate11To12_learningTablesStartEmptyAndReviewPointIdSurvives() {
        helper.createDatabase(TEST_DB, 11).use { db ->
            db.execSQL("INSERT INTO subjects (id, name, short_name, sort_order) VALUES ('unit-subject', 'subject', 's', 1)")
            db.execSQL("INSERT INTO chapters (id, subject_id, title, sort_order) VALUES ('unit-chapter', 'unit-subject', 'chapter', 1)")
            db.execSQL(
                """INSERT INTO knowledge_points
                    (id, chapter_id, title, core_conclusion, full_content, exam_frequency, created_at, updated_at)
                    VALUES ('unit-point', 'unit-chapter', 'title', 'conclusion', 'content', 'HIGH', 1, 1)""",
            )
            db.execSQL(
                """INSERT INTO review_logs (id, point_id, rating, created_at)
                    VALUES ('unit-review', 'unit-point', 'Good', 2)""",
            )
        }
        helper.runMigrationsAndValidate(TEST_DB, 12, true, MIGRATION_11_12).use { db ->
            db.query("SELECT point_id, learning_unit_id FROM review_logs WHERE id = 'unit-review'").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getString(0) == "unit-point")
                check(cursor.isNull(1))
            }
            db.query("SELECT COUNT(*) FROM learning_units").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getInt(0) == 0)
            }
        }
    }

    @Test
    fun migrate12To13_dailyPlanTablesStartEmpty() {
        helper.createDatabase(TEST_DB, 12).use { }
        helper.runMigrationsAndValidate(TEST_DB, 13, true, MIGRATION_12_13).use { db ->
            db.query("SELECT COUNT(*) FROM daily_plans").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getInt(0) == 0)
            }
            db.query("SELECT COUNT(*) FROM daily_tasks").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getInt(0) == 0)
            }
        }
    }

    @Test
    fun migrate13To14_practiceAttemptsStartEmpty() {
        helper.createDatabase(TEST_DB, 13).use { }
        helper.runMigrationsAndValidate(TEST_DB, 14, true, MIGRATION_13_14).use { db ->
            db.query("SELECT COUNT(*) FROM practice_attempts").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getInt(0) == 0)
            }
        }
    }

    @Test
    fun migrate14To15_writingSessionsStartEmpty() {
        helper.createDatabase(TEST_DB, 14).use { }
        helper.runMigrationsAndValidate(TEST_DB, 15, true, MIGRATION_14_15).use { db ->
            db.query("SELECT COUNT(*) FROM writing_sessions").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getInt(0) == 0)
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
