package com.wenyan.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity
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
        val wrongAnswerDao = db.wrongAnswerDao()
        repository = SchedulingRepositoryImpl(db, memoRecordDao, reviewLogDao, clockGuard, wrongAnswerDao)

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

    // ── v0.9.4 新增:rateWrongAnswer FSRS 调度测试 ──────────────────

    /**
     * 场景 4(v0.9.4 新增):rateWrongAnswer(GOOD) 后错题 sched_* 字段正确更新。
     *
     * 验证 [SchedulingRepository.rateWrongAnswer] 的完整流程:
     * 1. 读取 WrongAnswerEntity(默认 sched_state=NEW)
     * 2. 转 FlashCard → FSRS 调度(TIER_FRAMEWORK)
     * 3. updateScheduling 写回 DB
     * 4. 返回更新后的 Entity
     *
     * GOOD 评分预期:
     * - sched_state: NEW → REVIEW(新卡首次 GOOD 进入复习阶段)
     * - sched_reps: 0 → 1(复习次数 +1)
     * - sched_stability: > 0(GOOD 初始化稳定性)
     * - sched_next_review_at: > 0(未来时间戳,非立即到期)
     * - sched_lapses: 0(新卡首次 GOOD 无遗忘)
     *
     * 用 in-memory Room 验证真实 DB 写入,而非 Fake DAO,理由同 rateCard 测试:
     * - updateScheduling 是多参数 SQL UPDATE,需验证字段映射正确
     * - Fake DAO 易遗漏字段,真实 DB 能捕获列名/类型错误
     */
    @Test
    fun `rateWrongAnswer GOOD 后 sched 字段正确更新为 REVIEW`() = runTest {
        // 1. 插入一条新错题(sched_state=NEW, sched_reps=0)
        val wrongAnswerId = "wa_test_1"
        db.wrongAnswerDao().upsert(
            WrongAnswerEntity(
                id = wrongAnswerId,
                pointId = "point_1",
                examQuestionId = null,
                userAnswer = "苏轼是南宋人",
                correctAnswer = "苏轼是北宋文学家",
                source = "CARD_AGAIN",
                wrongCount = 1,
                lastWrongAt = 1_000_000L,
                resolvedAt = null,
                aiExplanation = null,
                createdAt = 500_000L,
                // sched_* 默认值:NEW / 0 / 5f / 0L / 0L / 0 / 0 / 0 / 0 / 0
            ),
        )

        // 2. 调用 rateWrongAnswer(GOOD)
        val result = repository.rateWrongAnswer(wrongAnswerId, Rating.GOOD)

        // 3. 验证返回值
        assertNotNull("rateWrongAnswer 应返回更新后的 Entity", result)
        assertEquals("id 应一致", wrongAnswerId, result!!.id)
        assertEquals("GOOD 后 state 应为 REVIEW", "REVIEW", result.schedState)
        assertEquals("sched_reps 应为 1", 1, result.schedReps)
        assertEquals("sched_review_count 应为 1", 1, result.schedReviewCount)
        assertTrue("GOOD 后 stability 应 > 0", result.schedStability > 0f)
        assertEquals("新卡首次 GOOD 无遗忘", 0, result.schedLapses)
        assertTrue(
            "sched_next_review_at 应为未来时间戳(>0)",
            result.schedNextReviewAt > 0L,
        )
        assertTrue(
            "sched_last_review_at 应非 0(已复习)",
            result.schedLastReviewAt > 0L,
        )

        // 4. 验证 DB 持久化(重新读取确认字段一致)
        val fromDb = db.wrongAnswerDao().getById(wrongAnswerId)
        assertNotNull("DB 中应有此错题", fromDb)
        assertEquals("DB state 应为 REVIEW", "REVIEW", fromDb!!.schedState)
        assertEquals("DB sched_reps 应为 1", 1, fromDb.schedReps)
        assertEquals("DB stability 应与返回值一致", result.schedStability, fromDb.schedStability)
    }

    /**
     * 场景 5(v0.9.4 新增):rateWrongAnswer(AGAIN) 后进入 LEARNING 状态。
     *
     * AGAIN 评分预期(新卡):
     * - sched_state: NEW → LEARNING(新卡 AGAIN 进入学习阶段,非 RELEARNING)
     * - sched_reps: 0 → 1(复习次数 +1)
     * - sched_lapses: 0(新卡 AGAIN 不算遗忘,只有 REVIEW→AGAIN 才算 lapse)
     * - sched_next_review_at: 短间隔(学习阶段,分钟级,但仍 > 0)
     *
     * 注:FSRS-6 新卡(NEW)AGAIN 进入 LEARNING,已掌握卡(REVIEW)AGAIN
     * 才进入 RELEARNING 且 lapses++. 本测试用新卡验证 LEARNING 分支。
     */
    @Test
    fun `rateWrongAnswer AGAIN 新卡进入 LEARNING 且无 lapse`() = runTest {
        val wrongAnswerId = "wa_test_2"
        db.wrongAnswerDao().upsert(
            WrongAnswerEntity(
                id = wrongAnswerId,
                pointId = "point_1",
                examQuestionId = null,
                userAnswer = "答错",
                correctAnswer = "正确",
                source = "CARD_AGAIN",
                wrongCount = 1,
                lastWrongAt = 1_000_000L,
                resolvedAt = null,
                aiExplanation = null,
                createdAt = 500_000L,
            ),
        )

        val result = repository.rateWrongAnswer(wrongAnswerId, Rating.AGAIN)

        assertNotNull(result)
        assertEquals(
            "新卡 AGAIN 应进入 LEARNING(非 RELEARNING)",
            "LEARNING",
            result!!.schedState,
        )
        assertEquals("sched_reps 应为 1", 1, result.schedReps)
        assertEquals("新卡 AGAIN 不算遗忘,lapses 应为 0", 0, result.schedLapses)
    }

    /**
     * 场景 6(v0.9.4 新增):rateWrongAnswer 空白 id 返回 null。
     *
     * 验证早期返回逻辑:`if (wrongAnswerId.isBlank()) return null`
     * 空白 id 不应触发任何 DB 读取或写入。
     */
    @Test
    fun `rateWrongAnswer 空白 id 返回 null`() = runTest {
        val result = repository.rateWrongAnswer("", Rating.GOOD)
        assertNull("空白 id 应返回 null", result)
    }

    /**
     * 场景 7(v0.9.4 新增):rateWrongAnswer 不存在的 id 返回 null。
     *
     * 验证 `wrongAnswerDao.getById(id) ?: return null` 分支:
     * DB 中无此 id 时返回 null,不抛异常,不写 DB。
     */
    @Test
    fun `rateWrongAnswer 不存在的 id 返回 null`() = runTest {
        val result = repository.rateWrongAnswer("non_existent_id", Rating.GOOD)
        assertNull("不存在的 id 应返回 null", result)
    }

    /**
     * 场景 8(v0.9.4 新增):rateWrongAnswer 不影响 wrongCount / resolvedAt 等非调度字段。
     *
     * 验证 [WrongAnswerDao.updateScheduling] 仅更新 sched_* 字段,
     * 不覆盖 wrongCount / resolvedAt / userAnswer / correctAnswer 等。
     *
     * 这是关键数据安全测试:若 updateScheduling 误用 upsert(整行覆盖),
     * 评分后 wrongCount 会丢失(重置为 Entity 默认值 1)。
     */
    @Test
    fun `rateWrongAnswer 不影响 wrongCount 和 resolvedAt 等非调度字段`() = runTest {
        val wrongAnswerId = "wa_test_3"
        db.wrongAnswerDao().upsert(
            WrongAnswerEntity(
                id = wrongAnswerId,
                pointId = "point_1",
                examQuestionId = null,
                userAnswer = "原始错误答案",
                correctAnswer = "原始正确答案",
                source = "QUIZ_WRONG",
                wrongCount = 5, // 非默认值,验证不被重置
                lastWrongAt = 2_000_000L,
                resolvedAt = null,
                aiExplanation = "AI 解释文本",
                createdAt = 500_000L,
            ),
        )

        repository.rateWrongAnswer(wrongAnswerId, Rating.GOOD)

        val fromDb = db.wrongAnswerDao().getById(wrongAnswerId)
        assertNotNull(fromDb)
        // 验证非调度字段保持不变
        assertEquals("wrongCount 应保持 5(不被重置)", 5, fromDb!!.wrongCount)
        assertEquals("source 应保持 QUIZ_WRONG", "QUIZ_WRONG", fromDb.source)
        assertEquals("userAnswer 应保持原值", "原始错误答案", fromDb.userAnswer)
        assertEquals("correctAnswer 应保持原值", "原始正确答案", fromDb.correctAnswer)
        assertEquals("aiExplanation 应保持原值", "AI 解释文本", fromDb.aiExplanation)
        assertNull("resolvedAt 应保持 null(未解决)", fromDb.resolvedAt)
        assertEquals("lastWrongAt 应保持原值", 2_000_000L, fromDb.lastWrongAt)
        // 验证调度字段已更新
        assertEquals("sched_state 应为 REVIEW", "REVIEW", fromDb.schedState)
        assertEquals("sched_reps 应为 1", 1, fromDb.schedReps)
    }
}
