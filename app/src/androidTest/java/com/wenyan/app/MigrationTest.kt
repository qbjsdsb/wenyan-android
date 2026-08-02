package com.wenyan.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.migration.MIGRATION_8_9
import com.wenyan.app.core.database.migration.MIGRATION_9_10
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

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
