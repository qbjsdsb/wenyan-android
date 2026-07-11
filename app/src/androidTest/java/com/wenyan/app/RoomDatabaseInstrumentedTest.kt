package com.wenyan.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.ExamCodeHistoryEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import dagger.hilt.android.HiltAndroidApp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Modifier

/**
 * Task 29 - Room 数据库 Instrumented Test。
 *
 * 验证 checklist C2.0-C2.13 项：数据库结构、Hilt 注入、19 Entity + 19 DAO、
 * ExamQuestion/KnowledgePoint 字段完整性、Spec 新增字段、ExamCodeHistory/DataSource 表、
 * 不存在 mentors 表、@Index 索引、种子数据 assets。
 *
 * 使用 Room.inMemoryDatabaseBuilder 创建内存数据库，不污染磁盘数据库文件。
 * 运行环境：androidTest（需 Android 设备/模拟器）。
 */
@RunWith(AndroidJUnit4::class)
class RoomDatabaseInstrumentedTest {

    private lateinit var db: WenyanDatabase
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            context, WenyanDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ===================== C2.0c: 数据库名 wenyan.db =====================

    /** C2.0c: 验证 DATABASE_NAME = "wenyan.db" */
    @Test
    fun databaseName_isWenyanDb() {
        assertEquals("wenyan.db", WenyanDatabase.DATABASE_NAME)
    }

    /** C2.0c: 验证内存数据库可成功创建（Schema 编译通过） */
    @Test
    fun inMemoryDatabase_buildsSuccessfully() {
        assertNotNull(db)
        // 验证可正常获取 DAO（说明数据库已创建）
        assertNotNull(db.examQuestionDao())
    }

    // ===================== C2.0b: Hilt 依赖注入 =====================

    /** C2.0b: 验证 WenyanApplication 标注 @HiltAndroidApp，可被 Hilt 注入 */
    @Test
    fun hiltApplication_isAnnotatedWithHiltAndroidApp() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertTrue("Application 应为 WenyanApplication", app is WenyanApplication)
        val annotation = WenyanApplication::class.java.getAnnotation(HiltAndroidApp::class.java)
        assertNotNull("WenyanApplication 应标注 @HiltAndroidApp", annotation)
    }

    // ===================== C2.0e: 多模块结构 19 Entity + 19 DAO =====================

    /** C2.0e: 验证 @Database 注解声明 19 个 Entity，且 WenyanDatabase 声明 19 个抽象 DAO 方法 */
    @Test
    fun database_has19EntitiesAnd19Daos() {
        val dbAnnotation = WenyanDatabase::class.java.getAnnotation(Database::class.java)
        assertNotNull("@Database 注解应存在", dbAnnotation)
        assertEquals("Entity 数量应为 19", 19, dbAnnotation!!.entities.size)

        val abstractDaoMethods = WenyanDatabase::class.java.declaredMethods.filter {
            Modifier.isAbstract(it.modifiers)
        }
        assertEquals("DAO 抽象方法数量应为 19", 19, abstractDaoMethods.size)
    }

    // ===================== C2.0i: ExamQuestion Entity 字段完整 =====================

    /** C2.0i: 验证 ExamQuestionEntity 字段完整，且不含错误的 stem/options/answer/explain 字段 */
    @Test
    fun examQuestion_hasAllRequiredFields_andNoWrongFields() {
        val fields = fieldNamesOf(ExamQuestionEntity::class.java)
        // 设计文档原有字段
        listOf(
            "id", "year", "subjectId", "questionType", "content", "score", "angle",
            "relatedPointIds", "answerFramework", "sampleEssay", "notes", "createdAt"
        ).forEach { fieldName ->
            assertTrue("ExamQuestionEntity 缺少字段: $fieldName", fieldName in fields)
        }
        // 错误字段不应存在（字段是 content 不是 stem）
        listOf("stem", "options", "answer", "explain").forEach { wrong ->
            assertFalse("ExamQuestionEntity 不应包含错误字段: $wrong", wrong in fields)
        }
    }

    // ===================== C2.1-C2.5: ExamQuestion Spec 新增字段 =====================

    /** C2.1-C2.5: 验证 ExamQuestionEntity 含 Spec 新增字段 examPaperCode/answerStatus/materialText/sourceFile/sourcePage */
    @Test
    fun examQuestion_hasSpecNewFields() {
        val fields = fieldNamesOf(ExamQuestionEntity::class.java)
        listOf(
            "examPaperCode", "answerStatus", "materialText", "sourceFile", "sourcePage"
        ).forEach { fieldName ->
            assertTrue("ExamQuestionEntity 缺少 Spec 新增字段: $fieldName", fieldName in fields)
        }
    }

    // ===================== C2.5a: KnowledgePoint Entity 字段完整 =====================

    /** C2.5a: 验证 KnowledgePointEntity 字段完整，且无 subject_id 字段（通过 chapter_id 间接关联科目） */
    @Test
    fun knowledgePoint_hasAllRequiredFields_andNoSubjectId() {
        val fields = fieldNamesOf(KnowledgePointEntity::class.java)
        listOf(
            "id", "chapterId", "title", "summary", "coreConclusion", "fullContent",
            "multiPerspectives", "relatedIds", "contrastIds", "extensionIds", "examRecords",
            "examFrequency", "termTemplate", "tags", "difficulty", "createdAt", "updatedAt"
        ).forEach { fieldName ->
            assertTrue("KnowledgePointEntity 缺少字段: $fieldName", fieldName in fields)
        }
        // 本表无 subject_id 字段
        assertFalse("KnowledgePointEntity 不应包含 subjectId", "subjectId" in fields)
    }

    // ===================== C2.6-C2.10: KnowledgePoint Spec 新增字段 =====================

    /** C2.6-C2.10: 验证 KnowledgePointEntity 含 Spec 新增字段 contentSource/ocrStatus/sourceFile/sourcePage/studyText */
    @Test
    fun knowledgePoint_hasSpecNewFields() {
        val fields = fieldNamesOf(KnowledgePointEntity::class.java)
        listOf(
            "contentSource", "ocrStatus", "sourceFile", "sourcePage", "studyText"
        ).forEach { fieldName ->
            assertTrue("KnowledgePointEntity 缺少 Spec 新增字段: $fieldName", fieldName in fields)
        }
    }

    // ===================== C2.11: ExamCodeHistory Entity =====================

    /** C2.11: 验证 exam_code_history 表正确创建 */
    @Test
    fun examCodeHistory_tableCreated() {
        val fields = fieldNamesOf(ExamCodeHistoryEntity::class.java)
        listOf("id", "examCode", "subjectName", "validFromYear", "validToYear", "direction", "createdAt")
            .forEach { assertTrue("ExamCodeHistoryEntity 缺少字段: $it", it in fields) }
        val tables = queryTableNames()
        assertTrue("exam_code_history 表应存在", "exam_code_history" in tables)
    }

    // ===================== C2.12: DataSource Entity =====================

    /** C2.12: 验证 data_sources 表正确创建 */
    @Test
    fun dataSource_tableCreated() {
        val fields = fieldNamesOf(DataSourceEntity::class.java)
        listOf(
            "id", "knowledgePointId", "examQuestionId", "sourceFile", "sourcePage",
            "contentSource", "ocrStatus", "createdAt"
        ).forEach { assertTrue("DataSourceEntity 缺少字段: $it", it in fields) }
        val tables = queryTableNames()
        assertTrue("data_sources 表应存在", "data_sources" in tables)
    }

    // ===================== C2.13: 不存在 mentors 表 =====================

    /** C2.13: 验证不存在 mentors 表（导师信息改为外链官网） */
    @Test
    fun database_hasNoMentorsTable() {
        val tables = queryTableNames()
        assertFalse("不应存在 mentors 表", "mentors" in tables)
    }

    // ===================== C2.10/C2.5: @Index("ocr_status") 和 @Index("exam_paper_code") =====================

    /** C2.10/C2.5: 验证 ocr_status 与 exam_paper_code 索引存在 */
    @Test
    fun database_hasOcrStatusAndExamPaperCodeIndices() {
        val indexNames = queryIndexNames()
        assertTrue(
            "应存在 exam_paper_code 索引",
            indexNames.any { it.contains("exam_paper_code") }
        )
        assertTrue(
            "应存在 ocr_status 索引",
            indexNames.any { it.contains("ocr_status") }
        )
    }

    // ===================== C2.0g: 种子数据加载器（assets/seed_data.json） =====================

    /** C2.0g: 验证 assets/seed_data.json 种子数据文件存在且可读 */
    @Test
    fun seedData_assetExists() {
        val content = context.assets.open("seed_data.json").bufferedReader().use { it.readText() }
        assertTrue("seed_data.json 内容不应为空", content.isNotBlank())
    }

    // ---------- 内部工具方法 ----------

    /** 获取数据类声明的字段名集合（Kotlin 属性 backing field） */
    private fun fieldNamesOf(clazz: Class<*>): Set<String> =
        clazz.declaredFields.map { it.name }.toSet()

    /** 查询数据库中所有用户表名（排除 Room 内部表） */
    private fun queryTableNames(): List<String> {
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name != 'room_master_table' AND name != 'android_metadata'"
        )
        return cursor.use { c ->
            val names = mutableListOf<String>()
            while (c.moveToNext()) names.add(c.getString(0))
            names
        }
    }

    /** 查询数据库中所有索引名 */
    private fun queryIndexNames(): List<String> {
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='index'"
        )
        return cursor.use { c ->
            val names = mutableListOf<String>()
            while (c.moveToNext()) names.add(c.getString(0))
            names
        }
    }
}
