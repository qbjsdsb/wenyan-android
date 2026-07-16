package com.wenyan.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import com.wenyan.app.core.fsrs.Rating
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SchedulingRepository] 单元测试(NF-PP4 Wave 2.2)。
 *
 * 用 Robolectric + in-memory RoomDatabase 做真实事务测试,验证:
 * - NF-PP4 修复:rateCard 后 review_logs 表是唯一复习历史源(history 字段已在 Wave 1 移除)
 * - 功能正确性:rateCard(AGAIN/GOOD) 后 memo_records 与 review_logs 都正确写入
 * - 边界处理:空白 pointId 早期返回 null,不写任何表
 *
 * 用 in-memory Room 而非 Fake DAO 的理由:
 * - SchedulingRepository 用 `database.withTransaction { ... }` 包裹两步写入,
 *   withTransaction 是 RoomDatabase 扩展函数,无法用 Fake 实现
 * - in-memory Room 提供真实事务行为,验证 withTransaction 包裹的正确性
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SchedulingRepositoryTest {

    private lateinit var db: WenyanDatabase
    private lateinit var repository: SchedulingRepository

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WenyanDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val memoRecordDao = db.memoRecordDao()
        val reviewLogDao = db.reviewLogDao()
        val clockGuard = ClockGuard(db.appMetaDao())
        repository = SchedulingRepository(db, memoRecordDao, reviewLogDao, clockGuard)

        // 插入 FK 依赖链:Subject → Chapter → KnowledgePoint
        // MemoRecord FK→KnowledgePoint,reviewLog FK→KnowledgePoint,必须先有 KnowledgePoint
        db.subjectDao().insert(
            SubjectEntity(
                id = "subj_1",
                name = "中国古代文学",
                shortName = "古文",
                sortOrder = 1,
            ),
        )
        db.chapterDao().insert(
            ChapterEntity(
                id = "ch_1",
                subjectId = "subj_1",
                parentId = null,
                title = "第一章 先秦文学",
                sortOrder = 1,
            ),
        )
        db.knowledgePointDao().insert(
            KnowledgePointEntity(
                id = "point_1",
                chapterId = "ch_1",
                title = "苏轼",
                summary = null,
                coreConclusion = "北宋文学家",
                fullContent = "苏轼是北宋著名文学家",
                multiPerspectives = null,
                relatedIds = null,
                contrastIds = null,
                extensionIds = null,
                examRecords = null,
                examFrequency = "HIGH",
                termTemplate = null,
                tags = null,
                difficulty = 3,
                createdAt = 1_000_000L,
                updatedAt = 1_000_000L,
                contentSource = null,
                ocrStatus = "VERIFIED",
                sourceFile = null,
                sourcePage = null,
                studyText = null,
            ),
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    /**
     * 场景 1:rateCard(AGAIN) 后 review_logs 表有 1 条 rating=AGAIN 记录。
     *
     * 验证 NF-PP4 修复:复习历史统一写入 review_logs 表(不再双写 history JSON)。
     * 同时验证 memo_records 也被 upsert(事务内两步都成功)。
     */
    @Test
    fun `rateCard AGAIN 后 review_logs 有 1 条 AGAIN 记录且 memo_records 已写入`() = runTest {
        val result = repository.rateCard("point_1", Rating.AGAIN, CardTemplateType.TERM_EXPLANATION)

        assertNotNull("rateCard 应返回更新后的 MemoRecord", result)
        assertEquals("point_1", result!!.pointId)

        val logs = db.reviewLogDao().getByPointOrderByCreatedDesc("point_1")
        assertEquals("review_logs 应有 1 条记录", 1, logs.size)
        assertEquals("rating 应为 AGAIN", "AGAIN", logs[0].rating)
        assertEquals("pointId 应为 point_1", "point_1", logs[0].pointId)

        val memo = db.memoRecordDao().getById("point_1")
        assertNotNull("memo_records 应有记录", memo)
        // FSRS-6:新卡(NEW) + AGAIN → LEARNING(进入学习阶段,非 RELEARNING)
        assertEquals("AGAIN 后 state 应为 LEARNING", "LEARNING", memo!!.state)
    }

    /**
     * 场景 2:rateCard(GOOD) 后 memo_records stability 更新且 state 非 NEW。
     *
     * 验证 NF-PP4 修复后 FSRS 调度功能正常(history 移除未破坏调度逻辑)。
     * GOOD 评分应让 stability > 0(新卡首次 GOOD 会初始化 stability)且 state 进入 REVIEW。
     */
    @Test
    fun `rateCard GOOD 后 memo_records stability 大于 0 且 state 非 NEW`() = runTest {
        val result = repository.rateCard("point_1", Rating.GOOD, CardTemplateType.TERM_EXPLANATION)

        assertNotNull(result)
        assertTrue("GOOD 后 stability 应 > 0", result!!.stability > 0f)
        assertEquals("GOOD 后 state 应为 REVIEW", "REVIEW", result.state)

        val memo = db.memoRecordDao().getById("point_1")
        assertNotNull(memo)
        assertEquals("DB 中 stability 应与返回值一致", result.stability, memo!!.stability)
        assertEquals("DB 中 state 应为 REVIEW", "REVIEW", memo.state)
    }

    /**
     * 场景 3:rateCard 空白 pointId 返回 null 且不写任何表。
     *
     * 验证 SchedulingRepository.rateCard 的早期返回逻辑(L72):
     * `if (pointId.isBlank()) return null`
     * 空白 pointId 不应触发任何 DB 写入,review_logs 和 memo_records 都为空。
     */
    @Test
    fun `rateCard 空白 pointId 返回 null 且不写任何表`() = runTest {
        val result = repository.rateCard("", Rating.GOOD, CardTemplateType.TERM_EXPLANATION)

        assertNull("空白 pointId 应返回 null", result)

        val logs = db.reviewLogDao().getByPointOrderByCreatedDesc("")
        assertTrue("review_logs 应为空", logs.isEmpty())

        val memo = db.memoRecordDao().getById("")
        assertNull("memo_records 应无记录", memo)
    }
}
