package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager

/**
 * KVM-independent migration verification for Cloud runners.
 *
 * Android's MigrationTestHelper remains the authoritative second gate. These tests execute the
 * exact statement lists used by the production Migration objects against SQLite JDBC, build the
 * starting database from Room's exported schema, validate the resulting columns and indexes
 * against the target export, and prove representative user rows survive byte-for-byte.
 */
class JvmMigrationTest {

    @Test
    fun migrate8To9_restoresMissingWrongAnswerIndexesAndPreservesUserRows() = withDatabase { db ->
        val source = schema(8)
        val historicallyMissing = setOf(
            "index_wrong_answers_point_id_source",
            "index_wrong_answers_exam_question_id_source",
        )
        createFromSchema(db, source, excludedIndexes = historicallyMissing)
        insertLegacyFixture(db)
        val before = userFixtureSnapshot(db)

        executeAtomically(db, MIGRATION_8_9_STATEMENTS)

        validateAgainstSchema(db, schema(9))
        assertEquals(before, userFixtureSnapshot(db))
    }

    @Test
    fun migrate9To10_addsFilterIndexesAndPreservesUserRows() = withDatabase { db ->
        createFromSchema(db, schema(9))
        insertLegacyFixture(db)
        val before = userFixtureSnapshot(db)

        executeAtomically(db, MIGRATION_9_10_STATEMENTS)

        validateAgainstSchema(db, schema(10))
        assertEquals(before, userFixtureSnapshot(db))
    }

    @Test
    fun migrate8To10_chainMatchesV10SchemaAndPreservesUserRows() = withDatabase { db ->
        val historicallyMissing = setOf(
            "index_wrong_answers_point_id_source",
            "index_wrong_answers_exam_question_id_source",
        )
        createFromSchema(db, schema(8), excludedIndexes = historicallyMissing)
        insertLegacyFixture(db)
        val before = userFixtureSnapshot(db)

        executeAtomically(db, MIGRATION_8_9_STATEMENTS)
        executeAtomically(db, MIGRATION_9_10_STATEMENTS)

        validateAgainstSchema(db, schema(10))
        assertEquals(before, userFixtureSnapshot(db))
    }

    @Test
    fun migrate10To11_defaultsLegacyContentToUnverifiedAndPreservesUserRows() = withDatabase { db ->
        createFromSchema(db, schema(10))
        insertLegacyFixture(db)
        val before = userFixtureSnapshot(db)

        executeAtomically(db, MIGRATION_10_11_STATEMENTS)

        validateAgainstSchema(db, schema(11))
        assertEquals(before, userFixtureSnapshot(db))
        assertColumnValue(db, "knowledge_points", "content_status", "LEGACY_UNVERIFIED")
        assertColumnValue(db, "exam_questions", "content_status", "LEGACY_UNVERIFIED")
        assertColumnValue(db, "writing_materials", "content_status", "LEGACY_UNVERIFIED")
        assertColumnValue(db, "data_sources", "source_status", "UNKNOWN")
    }

    @Test
    fun migrate11To12_addsEmptyLearningTablesAndPreservesMemoAndReviewRows() = withDatabase { db ->
        createFromSchema(db, schema(11))
        insertLegacyFixture(db)
        val before = userFixtureSnapshot(db)

        executeAtomically(db, MIGRATION_11_12_STATEMENTS)

        validateAgainstSchema(db, schema(12))
        assertEquals(before, userFixtureSnapshot(db))
        assertEquals(0, countRows(db, "learning_units"))
        assertEquals(0, countRows(db, "learning_unit_records"))
        assertColumnValue(db, "review_logs", "learning_unit_id", null)
    }

    @Test
    fun migrate12To13_addsEmptyDailyPlanTablesAndPreservesUserRows() = withDatabase { db ->
        createFromSchema(db, schema(12))
        insertLegacyFixture(db)
        val before = userFixtureSnapshot(db)

        executeAtomically(db, MIGRATION_12_13_STATEMENTS)

        validateAgainstSchema(db, schema(13))
        assertEquals(before, userFixtureSnapshot(db))
        assertEquals(0, countRows(db, "daily_plans"))
        assertEquals(0, countRows(db, "daily_tasks"))
    }

    @Test
    fun migrate13To14_addsEmptyPracticeAttemptsAndPreservesUserRows() = withDatabase { db ->
        createFromSchema(db, schema(13))
        insertLegacyFixture(db)
        val before = userFixtureSnapshot(db)

        executeAtomically(db, MIGRATION_13_14_STATEMENTS)

        validateAgainstSchema(db, schema(14))
        assertEquals(before, userFixtureSnapshot(db))
        assertEquals(0, countRows(db, "practice_attempts"))
    }

    @Test
    fun migrate14To15_addsEmptyWritingSessionsAndPreservesUserRows() = withDatabase { db ->
        createFromSchema(db, schema(14))
        insertLegacyFixture(db)
        val before = userFixtureSnapshot(db)
        executeAtomically(db, MIGRATION_14_15_STATEMENTS)
        validateAgainstSchema(db, schema(15))
        assertEquals(before, userFixtureSnapshot(db))
        assertEquals(0, countRows(db, "writing_sessions"))
    }

    @Test
    fun migrate10To15_chainPreservesUserRowsAndValidatesFinalSchema() = withDatabase { db ->
        createFromSchema(db, schema(10))
        insertLegacyFixture(db)
        val before = userFixtureSnapshot(db)
        listOf(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
            .forEach { executeProductionMigration(db, it) }
        validateAgainstSchema(db, schema(15))
        assertEquals(before, userFixtureSnapshot(db))
        assertEquals(0, countRows(db, "writing_sessions"))
    }

    @Test
    fun migrate2To15_chainUsesProductionMigrationsAndPreservesPublishedUserData() = withDatabase { db ->
        createFromSchema(db, schema(2))
        insertCoreFixture(db)
        val before = coreFixtureSnapshot(db)

        listOf(
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
        ).forEach { executeProductionMigration(db, it) }

        validateAgainstSchema(db, schema(15))
        assertEquals(before, coreFixtureSnapshot(db))
        assertColumnValue(db, "knowledge_points", "content_status", "LEGACY_UNVERIFIED")
        assertColumnValue(db, "exam_questions", "content_status", "LEGACY_UNVERIFIED")
    }

    private fun withDatabase(block: (Connection) -> Unit) {
        val file = Files.createTempFile("wenyan-migration-", ".db")
        try {
            DriverManager.getConnection("jdbc:sqlite:$file").use(block)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun schema(version: Int): JsonObject {
        val path = Path.of("schemas/com.wenyan.app.core.database.WenyanDatabase/$version.json")
        return Json.parseToJsonElement(String(Files.readAllBytes(path))).jsonObject["database"]!!.jsonObject
    }

    private fun createFromSchema(
        db: Connection,
        schema: JsonObject,
        excludedIndexes: Set<String> = emptySet(),
    ) {
        entities(schema).forEach { entity ->
            val table = entity.string("tableName")
            db.createStatement().use { statement ->
                statement.execute(entity.string("createSql").replace("\${TABLE_NAME}", table))
            }
        }
        entities(schema).forEach { entity ->
            val table = entity.string("tableName")
            indexes(entity).filterNot { it.string("name") in excludedIndexes }.forEach { index ->
                val unique = if (index["unique"]!!.jsonPrimitive.boolean) "UNIQUE " else ""
                val columns = index["columnNames"]!!.jsonArray.joinToString { "`${it.jsonPrimitive.content}`" }
                db.createStatement().use { statement ->
                    statement.execute("CREATE ${unique}INDEX `${index.string("name")}` ON `$table` ($columns)")
                }
            }
        }
        db.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
    }

    private fun insertLegacyFixture(db: Connection) {
        insertCoreFixture(db)
        db.createStatement().use { statement ->
            statement.executeUpdate(
                """INSERT INTO wrong_answers
                    (id, point_id, exam_question_id, user_answer, correct_answer, source, wrong_count, last_wrong_at, created_at)
                    VALUES ('fixture-wrong', 'fixture-point', 'fixture-question', '用户原答案', '原参考答案', 'QUIZ', 2, 14, 15)""",
            )
            statement.executeUpdate(
                """INSERT INTO writing_materials
                    (id, category, sub_category, content, source, tags, created_at)
                    VALUES ('fixture-material', 'EVIDENCE', 'legacy', '用户写作素材', '旧来源', 'tag', 22)""",
            )
            statement.executeUpdate(
                """INSERT INTO data_sources
                    (id, knowledge_point_id, exam_question_id, source_file, source_page, content_source, ocr_status, created_at)
                    VALUES ('fixture-source', 'fixture-point', NULL, 'legacy.pdf', 23, 'TEXTBOOK_OCR', 'VERIFIED', 24)""",
            )
            statement.executeUpdate(
                """INSERT INTO review_logs
                    (id, point_id, rating, elapsed_days, scheduled_days, state, stability, difficulty, reps, created_at)
                    VALUES ('fixture-review', 'fixture-point', 'Good', 3, 4, 'REVIEW', 3.25, 6.5, 4, 25)""",
            )
        }
    }

    private fun insertCoreFixture(db: Connection) {
        db.createStatement().use { statement ->
            statement.executeUpdate(
                "INSERT INTO subjects (id, name, short_name, sort_order) VALUES ('fixture-subject', '保留科目', '科目', 1)",
            )
            statement.executeUpdate(
                "INSERT INTO chapters (id, subject_id, title, sort_order) VALUES ('fixture-chapter', 'fixture-subject', '保留章节', 1)",
            )
            statement.executeUpdate(
                """INSERT INTO knowledge_points
                    (id, chapter_id, title, core_conclusion, full_content, exam_frequency, created_at, updated_at, content_source)
                    VALUES ('fixture-point', 'fixture-chapter', '保留标题', '保留结论', '保留正文', 'HIGH', 11, 12, 'fixture-source')""",
            )
            statement.executeUpdate(
                """INSERT INTO exam_questions
                    (id, year, subject_id, question_type, content, score, created_at, answer_status)
                    VALUES ('fixture-question', 2025, 'fixture-subject', 'ESSAY', '保留题干', 30, 13, 'LEGACY')""",
            )
            statement.executeUpdate(
                """INSERT INTO memo_records
                    (point_id, state, stability, difficulty, last_review_at, next_review_at, review_count, fail_count, reps)
                    VALUES ('fixture-point', 'REVIEW', 3.25, 6.5, 16, 17, 4, 1, 4)""",
            )
            statement.executeUpdate(
                """INSERT INTO study_progress
                    (id, last_point_id, last_visited_at, total_study_time, streak_days, last_check_in)
                    VALUES ('fixture-progress', 'fixture-point', 18, 19, 20, 21)""",
            )
        }
    }

    private fun userFixtureSnapshot(
        db: Connection,
        coreOnly: Boolean = false,
    ): Map<String, List<List<String?>>> {
        val columnsByTable = linkedMapOf(
            "subjects" to "id, name, short_name, sort_order",
            "chapters" to "id, subject_id, parent_id, title, sort_order",
            "knowledge_points" to "id, chapter_id, title, summary, core_conclusion, full_content, multi_perspectives, related_ids, contrast_ids, extension_ids, exam_records, exam_frequency, term_template, tags, difficulty, created_at, updated_at, content_source, ocr_status, source_file, source_page, study_text",
            "exam_questions" to "id, year, subject_id, question_type, content, score, angle, related_point_ids, answer_framework, notes, created_at, exam_paper_code, answer_status, material_text, source_file, source_page",
            "wrong_answers" to "id, point_id, exam_question_id, user_answer, correct_answer, source, wrong_count, last_wrong_at, resolved_at, ai_explanation, created_at, sched_state, sched_stability, sched_difficulty, sched_last_review_at, sched_next_review_at, sched_review_count, sched_lapses, sched_elapsed_days, sched_scheduled_days, sched_reps",
            "memo_records" to "point_id, state, stability, difficulty, last_review_at, next_review_at, review_count, fail_count, elapsed_days, scheduled_days, reps, in_priority_queue",
            "study_progress" to "id, last_point_id, last_visited_at, total_study_time, streak_days, last_check_in",
            "writing_materials" to "id, category, sub_category, content, source, tags, created_at",
            "data_sources" to "id, knowledge_point_id, exam_question_id, source_file, source_page, content_source, ocr_status, created_at",
            "review_logs" to "id, point_id, rating, elapsed_days, scheduled_days, state, stability, difficulty, reps, created_at",
        )
        val selected = if (coreOnly) columnsByTable.filterKeys {
            it in setOf("subjects", "chapters", "knowledge_points", "exam_questions", "memo_records", "study_progress")
        } else {
            columnsByTable
        }
        return selected.mapValues { (table, columns) ->
                db.createStatement().use { statement ->
                    statement.executeQuery("SELECT $columns FROM `$table` ORDER BY 1").use { rows ->
                        buildList {
                            val columnCount = rows.metaData.columnCount
                            while (rows.next()) {
                                add((1..columnCount).map { rows.getObject(it)?.toString() })
                            }
                        }
                    }
                }
            }
    }

    private fun coreFixtureSnapshot(db: Connection): Map<String, List<List<String?>>> =
        userFixtureSnapshot(db, coreOnly = true)

    private fun executeProductionMigration(db: Connection, migration: Migration) {
        val supportDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    val sql = args!![0] as String
                    db.createStatement().use { it.execute(sql) }
                    Unit
                }
                "toString" -> "JdbcSupportSQLiteDatabase"
                "hashCode" -> System.identityHashCode(db)
                "equals" -> false
                else -> error("Migration unexpectedly called SupportSQLiteDatabase.${method.name}")
            }
        } as SupportSQLiteDatabase
        db.autoCommit = false
        try {
            migration.migrate(supportDb)
            db.commit()
        } catch (failure: Throwable) {
            db.rollback()
            throw failure
        } finally {
            db.autoCommit = true
        }
    }

    private fun assertColumnValue(db: Connection, table: String, column: String, expected: String?) {
        db.createStatement().use { statement ->
            statement.executeQuery("SELECT `$column` FROM `$table` LIMIT 1").use { rows ->
                assertTrue("missing fixture row in $table", rows.next())
                assertEquals(expected, rows.getString(1))
            }
        }
    }

    private fun countRows(db: Connection, table: String): Int = db.createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM `$table`").use { rows ->
            check(rows.next())
            rows.getInt(1)
        }
    }

    private fun executeAtomically(db: Connection, statements: List<String>) {
        db.autoCommit = false
        try {
            db.createStatement().use { statement -> statements.forEach(statement::execute) }
            db.commit()
        } catch (failure: Throwable) {
            db.rollback()
            throw failure
        } finally {
            db.autoCommit = true
        }
    }

    private fun validateAgainstSchema(db: Connection, target: JsonObject) {
        val expectedTables = entities(target).map { it.string("tableName") }.toSet()
        val actualTables = db.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
            ).use { rows -> buildSet { while (rows.next()) add(rows.getString("name")) } }
        }
        assertEquals("database tables", expectedTables, actualTables)

        entities(target).forEach { entity ->
            val table = entity.string("tableName")
            val expectedColumns = entity["fields"]!!.jsonArray.map { it.jsonObject.string("columnName") }
            val actualColumns = db.createStatement().use { statement ->
                statement.executeQuery("PRAGMA table_info(`$table`)").use { rows ->
                    buildList { while (rows.next()) add(rows.getString("name")) }
                }
            }
            assertEquals("columns for $table", expectedColumns, actualColumns)

            val expectedIndexes = indexes(entity).associate { index ->
                index.string("name") to ExpectedIndex(
                    columns = index["columnNames"]!!.jsonArray.map { it.jsonPrimitive.content },
                    unique = index["unique"]!!.jsonPrimitive.boolean,
                )
            }
            val actualIndexes = db.createStatement().use { statement ->
                statement.executeQuery("PRAGMA index_list(`$table`)").use { rows ->
                    buildMap {
                        while (rows.next()) {
                            if (rows.getString("origin") == "c") {
                                put(rows.getString("name"), rows.getInt("unique") == 1)
                            }
                        }
                    }
                }
            }
            assertEquals("indexes for $table", expectedIndexes.keys, actualIndexes.keys)
            expectedIndexes.forEach { (name, expected) ->
                assertEquals("uniqueness for index $name", expected.unique, actualIndexes[name])
                val actualColumns = db.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA index_info(`$name`)").use { rows ->
                        buildList { while (rows.next()) add(rows.getString("name")) }
                    }
                }
                assertEquals("columns for index $name", expected.columns, actualColumns)
            }
        }
        db.createStatement().use { statement ->
            statement.executeQuery("PRAGMA foreign_key_check").use { violations ->
                assertTrue("foreign-key violation after migration", !violations.next())
            }
        }
    }

    private fun entities(schema: JsonObject): List<JsonObject> =
        schema["entities"]!!.jsonArray.map { it.jsonObject }
    private fun indexes(entity: JsonObject): List<JsonObject> =
        entity["indices"]?.jsonArray?.map { it.jsonObject }.orEmpty()

    private fun JsonObject.string(name: String): String = this[name]!!.jsonPrimitive.contentOrNull!!

    private data class ExpectedIndex(val columns: List<String>, val unique: Boolean)
}
