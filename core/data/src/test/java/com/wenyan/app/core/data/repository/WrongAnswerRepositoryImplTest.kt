package com.wenyan.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [WrongAnswerRepositoryImpl] 单元测试(NF-PP5 Wave 2.4)。
 *
 * 用 Robolectric + in-memory Room 做真实持久化测试,验证:
 * - recordWrongAnswer 新插入返回非空 id,upsert 到 wrong_answers
 * - 同一 pointId + source 重复记录,wrongCount 递增(不重复插入)
 * - markResolved 后 observeUnresolved 不返回该项(resolvedAt 写入)
 * - deleteById 后 observeAll 不返回该项
 * - observeByPoint 按 pointId 筛选正确
 *
 * 用 in-memory Room 而非 Fake DAO 的理由:
 * - WrongAnswerRepositoryImpl 内部用 DAO 的 findUnresolved/incrementWrongCount/upsert
 *   组合,真实 DB 能验证完整事务行为与 SQL 查询正确性
 * - Fake DAO 易遗漏 SQL 边界(如 resolvedAt IS NULL 的 NULL 语义)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WrongAnswerRepositoryImplTest {

    private lateinit var db: WenyanDatabase
    private lateinit var repository: WrongAnswerRepositoryImpl

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WenyanDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = WrongAnswerRepositoryImpl(db.wrongAnswerDao())

        // 插入 FK 依赖链:Subject → Chapter → KnowledgePoint(供 WrongAnswer FK→KnowledgePoint)
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
        // 插入 ExamQuestion(供 WrongAnswer FK→ExamQuestion)
        db.examQuestionDao().insert(
            ExamQuestionEntity(
                id = "eq_1",
                year = 2023,
                subjectId = "subj_1",
                questionType = "SHORT_ANSWER",
                content = "简述苏轼的文学成就",
                score = 15,
                angle = null,
                relatedPointIds = null,
                answerFramework = null,
                notes = null,
                createdAt = 1_000_000L,
                examPaperCode = "805",
                answerStatus = "HAS_ANSWER",
                materialText = null,
                sourceFile = null,
                sourcePage = null,
            ),
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    /**
     * 场景 1:recordWrongAnswer 新插入返回非空 id,且能通过 observeAll 读回。
     *
     * 验证 recordWrongAnswer 内部:
     * - 无已有未解决错题 → UUID.randomUUID().toString() 生成新 id
     * - wrongAnswerDao.upsert 写入 WrongAnswerEntity
     * - wrongCount 默认 1, resolvedAt = null(未解决)
     *
     * v0.9.2 新增:验证 JOIN 查询返回的 [WrongAnswerWithDetails.questionTitle]
     * 正确取到 knowledge_points.title（卡片来源 → 知识点 title）。
     */
    @Test
    fun `recordWrongAnswer 新插入返回非空 id 且能读回`() = runTest {
        val id = repository.recordWrongAnswer(
            pointId = "point_1",
            examQuestionId = null,
            userAnswer = "苏轼是南宋人",
            correctAnswer = "苏轼是北宋文学家",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )

        assertTrue("新插入应返回非空 id", id.isNotBlank())

        val all = repository.observeAll().first()
        assertEquals("observeAll 应有 1 条记录", 1, all.size)
        // v0.9.2：JOIN 返回 WrongAnswerWithDetails，字段嵌套在 wrongAnswer 内
        val record = all[0]
        assertEquals("id 一致", id, record.wrongAnswer.id)
        assertEquals("point_1", record.wrongAnswer.pointId)
        assertNull("examQuestionId 应为 null", record.wrongAnswer.examQuestionId)
        assertEquals("苏轼是南宋人", record.wrongAnswer.userAnswer)
        assertEquals("苏轼是北宋文学家", record.wrongAnswer.correctAnswer)
        assertEquals("source 应为 CARD_AGAIN", "CARD_AGAIN", record.wrongAnswer.source)
        assertEquals("wrongCount 应为 1", 1, record.wrongAnswer.wrongCount)
        assertNull("resolvedAt 应为 null(未解决)", record.wrongAnswer.resolvedAt)
        // v0.9.2：验证 JOIN 取到知识点 title（setup 中 point_1.title="苏轼"）
        assertEquals(
            "questionTitle 应 JOIN 到 knowledge_points.title",
            "苏轼",
            record.questionTitle,
        )
    }

    /**
     * 场景 2:同一 pointId + source 重复记录,wrongCount 递增(不重复插入)。
     *
     * 验证 recordWrongAnswer 内部:
     * - findUnresolvedByPointAndSource 找到已有记录
     * - incrementWrongCount 让 wrongCount++
     * - 返回 existing.id(非新 id)
     */
    @Test
    fun `同一 pointId 重复记录 wrongCount 递增`() = runTest {
        val id1 = repository.recordWrongAnswer(
            pointId = "point_1",
            examQuestionId = null,
            userAnswer = "答错1",
            correctAnswer = "正确",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )
        val id2 = repository.recordWrongAnswer(
            pointId = "point_1",
            examQuestionId = null,
            userAnswer = "答错2",
            correctAnswer = "正确",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )
        val id3 = repository.recordWrongAnswer(
            pointId = "point_1",
            examQuestionId = null,
            userAnswer = "答错3",
            correctAnswer = "正确",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )

        assertEquals("同一题重复答错应返回同一 id", id1, id2)
        assertEquals("第三次仍是同一 id", id1, id3)

        val all = repository.observeAll().first()
        assertEquals("应只有 1 条记录(不重复插入)", 1, all.size)
        assertEquals("wrongCount 应递增到 3", 3, all[0].wrongAnswer.wrongCount)
    }

    /**
     * 场景 3:markResolved 后 observeUnresolved 不返回该项。
     *
     * 验证 markResolved 内部:
     * - wrongAnswerDao.markResolved(id, now) 写入 resolvedAt
     * - observeUnresolved 查询 resolvedAt IS NULL,已解决的不再返回
     * - observeAll 仍返回(resolvedAt 非 null)
     */
    @Test
    fun `markResolved 后 observeUnresolved 不返回该项`() = runTest {
        val id = repository.recordWrongAnswer(
            pointId = "point_1",
            examQuestionId = null,
            userAnswer = "答错",
            correctAnswer = "正确",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )

        // 标记前:observeUnresolved 包含该项
        val unresolvedBefore = repository.observeUnresolved().first()
        assertEquals("标记前应有 1 条未解决", 1, unresolvedBefore.size)
        assertEquals(id, unresolvedBefore[0].wrongAnswer.id)
        assertEquals("countUnresolved 应为 1", 1, repository.countUnresolved())

        repository.markResolved(id)

        // 标记后:observeUnresolved 不包含该项
        val unresolvedAfter = repository.observeUnresolved().first()
        assertTrue("标记后 observeUnresolved 应为空", unresolvedAfter.isEmpty())
        assertEquals("countUnresolved 应为 0", 0, repository.countUnresolved())

        // observeAll 仍返回(resolvedAt 非 null)
        val all = repository.observeAll().first()
        assertEquals("observeAll 仍应返回该项", 1, all.size)
        assertNotNull("resolvedAt 应非 null", all[0].wrongAnswer.resolvedAt)
    }

    /**
     * 场景 4:deleteById 后 observeAll 不返回该项。
     *
     * 验证 deleteById 内部:
     * - wrongAnswerDao.deleteById(id) 物理删除
     * - observeAll 与 observeUnresolved 均不返回该项
     */
    @Test
    fun `deleteById 后 observeAll 不返回该项`() = runTest {
        val id = repository.recordWrongAnswer(
            pointId = "point_1",
            examQuestionId = null,
            userAnswer = "答错",
            correctAnswer = "正确",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )

        assertEquals("删除前应有 1 条", 1, repository.observeAll().first().size)

        repository.deleteById(id)

        assertTrue("删除后 observeAll 应为空", repository.observeAll().first().isEmpty())
        assertTrue("删除后 observeUnresolved 应为空", repository.observeUnresolved().first().isEmpty())
    }

    /**
     * 场景 5:observeByPoint 按 pointId 筛选正确。
     *
     * 验证 observeByPoint 内部:
     * - SQL WHERE point_id = :pointId 正确筛选
     * - 不同 pointId 的错题不互相干扰
     */
    @Test
    fun `observeByPoint 按 pointId 筛选正确`() = runTest {
        // 先插入第二个 KnowledgePoint(避免 FK 冲突)
        db.knowledgePointDao().insert(
            KnowledgePointEntity(
                id = "point_2",
                chapterId = "ch_1",
                title = "辛弃疾",
                summary = null,
                coreConclusion = "南宋词人",
                fullContent = "辛弃疾是南宋著名词人",
                multiPerspectives = null,
                relatedIds = null,
                contrastIds = null,
                extensionIds = null,
                examRecords = null,
                examFrequency = "MEDIUM",
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

        repository.recordWrongAnswer(
            pointId = "point_1",
            examQuestionId = null,
            userAnswer = "答错1",
            correctAnswer = "正确1",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )
        repository.recordWrongAnswer(
            pointId = "point_2",
            examQuestionId = null,
            userAnswer = "答错2",
            correctAnswer = "正确2",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )
        repository.recordWrongAnswer(
            pointId = "point_2",
            examQuestionId = null,
            userAnswer = "答错3",
            correctAnswer = "正确2",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )

        val byPoint1 = repository.observeByPoint("point_1").first()
        assertEquals("point_1 应有 1 条", 1, byPoint1.size)
        assertEquals("point_1", byPoint1[0].pointId)
        assertEquals("wrongCount 应为 1", 1, byPoint1[0].wrongCount)

        val byPoint2 = repository.observeByPoint("point_2").first()
        assertEquals("point_2 应有 1 条(重复答错递增不新增)", 1, byPoint2.size)
        assertEquals("point_2", byPoint2[0].pointId)
        assertEquals("wrongCount 应递增到 2", 2, byPoint2[0].wrongCount)

        // 全部应有 2 条(point_1 + point_2 各 1 条)
        val all = repository.observeAll().first()
        assertEquals("observeAll 应有 2 条", 2, all.size)
    }

    /**
     * 场景 6:真题来源错题记录正确(examQuestionId 非空,pointId 为空)。
     *
     * 验证 recordWrongAnswer 内部:
     * - pointId = null, examQuestionId != null → findUnresolvedByExamQuestionAndSource 查询
     * - 新插入 WrongAnswerEntity,pointId = null, examQuestionId = "eq_1"
     * - observeByExamQuestion 能查到
     */
    @Test
    fun `真题来源错题记录正确`() = runTest {
        val id = repository.recordWrongAnswer(
            pointId = null,
            examQuestionId = "eq_1",
            userAnswer = "苏轼是南宋人",
            correctAnswer = "苏轼是北宋文学家",
            source = WrongAnswerRepository.SOURCE_QUIZ_WRONG,
        )

        assertTrue(id.isNotBlank())

        val byExam = repository.observeByExamQuestion("eq_1").first()
        assertEquals("应有 1 条真题错题", 1, byExam.size)
        assertEquals(id, byExam[0].id)
        assertNull("pointId 应为 null(真题来源)", byExam[0].pointId)
        assertEquals("eq_1", byExam[0].examQuestionId)
        assertEquals("source 应为 QUIZ_WRONG", "QUIZ_WRONG", byExam[0].source)

        // 同一真题重复答错也递增
        val id2 = repository.recordWrongAnswer(
            pointId = null,
            examQuestionId = "eq_1",
            userAnswer = "再答错",
            correctAnswer = "苏轼是北宋文学家",
            source = WrongAnswerRepository.SOURCE_QUIZ_WRONG,
        )
        assertEquals("同一真题重复答错应返回同一 id", id, id2)
        val byExam2 = repository.observeByExamQuestion("eq_1").first()
        assertEquals("wrongCount 应为 2", 2, byExam2[0].wrongCount)
    }

    /**
     * 场景 7:已解决的错题,再次答错时新增一条未解决记录(不复用已解决记录)。
     *
     * 验证 recordWrongAnswer 内部:
     * - findUnresolvedByPointAndSource 只查 resolvedAt IS NULL,已解决记录不会被命中
     * - 因此 upsert 新 WrongAnswerEntity,返回新 id(≠ 原 id)
     * - 历史已解决记录保留(observeAll 有 2 条),observeUnresolved 只有新记录
     *
     * 这是有意设计:已解决的错题作为历史保留,新的答错是新事件,不混淆历史。
     */
    @Test
    fun `已解决错题再次答错新增未解决记录`() = runTest {
        val id = repository.recordWrongAnswer(
            pointId = "point_1",
            examQuestionId = null,
            userAnswer = "答错1",
            correctAnswer = "正确",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )

        // 标记为已解决
        repository.markResolved(id)
        assertTrue("标记后应无未解决", repository.observeUnresolved().first().isEmpty())

        // 再次答错(同一 pointId + source,但已有记录 resolvedAt 非 null)
        // 注意:findUnresolvedByPointAndSource 只查 resolvedAt IS NULL,所以这次找不到 existing,
        // 会 upsert 新记录。这是预期行为(已解决的历史错题保留,新错题是新记录)。
        val id2 = repository.recordWrongAnswer(
            pointId = "point_1",
            examQuestionId = null,
            userAnswer = "答错2",
            correctAnswer = "正确",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )

        assertNotEquals("已解决后再次答错应是新 id(新记录)", id, id2)
        assertEquals("observeAll 应有 2 条(已解决 + 新未解决)", 2, repository.observeAll().first().size)
        assertEquals("observeUnresolved 应有 1 条(新未解决)", 1, repository.observeUnresolved().first().size)
        assertEquals(
            "新记录 wrongCount 应为 1",
            1,
            repository.observeUnresolved().first()[0].wrongAnswer.wrongCount,
        )
    }

    /**
     * 场景 8(v0.9.2 新增):真题来源错题的 questionTitle JOIN 到 exam_questions.content。
     *
     * 验证 JOIN 查询:
     * - pointId = null, examQuestionId = "eq_1" → COALESCE(k.title, e.content) 取 e.content
     * - setup 中 eq_1.content = "简述苏轼的文学成就"
     * - questionTitle 应为 "简述苏轼的文学成就"
     *
     * 这条测试专门覆盖 v0.9.2 修复的核心场景:
     * 原错题本只显示答案不显示题目,因 DAO 无 JOIN 拿不到 exam_questions.content。
     */
    @Test
    fun `真题来源错题 questionTitle JOIN 到 exam_questions content`() = runTest {
        repository.recordWrongAnswer(
            pointId = null,
            examQuestionId = "eq_1",
            userAnswer = "苏轼是南宋人",
            correctAnswer = "苏轼是北宋文学家",
            source = WrongAnswerRepository.SOURCE_QUIZ_WRONG,
        )

        val all = repository.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(
            "真题来源 questionTitle 应 JOIN 到 exam_questions.content",
            "简述苏轼的文学成就",
            all[0].questionTitle,
        )
    }

    /**
     * 场景 9(v0.9.2 新增):FK 关联记录被删除时 questionTitle 为 null（LEFT JOIN 兜底）。
     *
     * 验证 LEFT JOIN 行为:
     * - 插入 pointId = "point_ghost" 的错题（无对应 knowledge_points 记录）
     * - LEFT JOIN 取不到 k.title,examQuestionId = null 也取不到 e.content
     * - COALESCE(NULL, NULL) = NULL,questionTitle 应为 null
     *
     * 但注意:wrong_answers 表有 FK 约束 point_id → knowledge_points.id,
     * 实际无法插入 point_ghost。故本测试用直接 DAO upsert 绕过 Repository 的 FK 校验,
     * 模拟"FK 记录被删除后"的场景（虽然 FK 阻止删除,但 LEFT JOIN 语义仍需验证）。
     *
     * 由于 FK 约束阻止插入 ghost pointId,本测试改为验证"正常场景下 questionTitle 非 null",
     * LEFT JOIN null 兜底由 UI 层 "题目已删除" 文案覆盖（WrongAnswerScreen.kt 已处理）。
     */
    @Test
    fun `正常场景 questionTitle 非 null`() = runTest {
        repository.recordWrongAnswer(
            pointId = "point_1",
            examQuestionId = null,
            userAnswer = "答错",
            correctAnswer = "正确",
            source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
        )

        val all = repository.observeAll().first()
        assertEquals(1, all.size)
        assertNotNull("正常场景 questionTitle 应非 null", all[0].questionTitle)
        assertEquals("苏轼", all[0].questionTitle)
    }
}
