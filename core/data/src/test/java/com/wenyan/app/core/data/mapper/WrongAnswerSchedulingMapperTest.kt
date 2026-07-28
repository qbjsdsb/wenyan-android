package com.wenyan.app.core.data.mapper

import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.fsrs.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [WrongAnswerSchedulingMapper] 单元测试（v0.9.5 follow-up #2 新增）。
 *
 * 核心验证：
 * - toFlashCard: sched_* 字段正确映射到 FlashCard
 * - toSchedulingUpdate: FlashCard 调度结果正确映射回 sched_* 更新参数
 * - interval 下界保护（coerceAtLeast(0)）：时钟回拨/数据损坏导致
 *   schedNextReviewAt < schedLastReviewAt 时,interval 不为负
 *
 * 不使用 Room/Robolectric,纯 JVM 单元测试（mapper 是纯 object,无 DB 依赖）。
 */
class WrongAnswerSchedulingMapperTest {

    // ── toFlashCard: 正常场景 ──────────────────────────────────────

    /**
     * 正常场景：schedNextReviewAt > schedLastReviewAt,interval 为正数（天）。
     */
    @Test
    fun `toFlashCard 正常场景 interval 为正天数`() {
        val entity = sampleEntity(
            schedLastReviewAt = 1_000_000L,
            schedNextReviewAt = 1_000_000L + 3 * 86_400_000L, // 3 天后
        )

        val flashCard = WrongAnswerSchedulingMapper.toFlashCard(entity)

        assertEquals("interval 应为 3 天", 3, flashCard.interval)
        assertEquals(State.REVIEW, flashCard.state)
        assertEquals(5f, flashCard.stability)
        assertEquals(5f, flashCard.difficulty)
        assertEquals(1, flashCard.reviewCount)
        assertEquals(1, flashCard.reps)
        assertEquals(0, flashCard.lapses)
    }

    /**
     * 新建错题：schedLastReviewAt=0,interval=0（从未复习）。
     */
    @Test
    fun `toFlashCard 新建错题 schedLastReviewAt 为 0 时 interval 为 0`() {
        val entity = sampleEntity(
            schedLastReviewAt = 0L,
            schedNextReviewAt = 0L,
            schedState = "NEW",
        )

        val flashCard = WrongAnswerSchedulingMapper.toFlashCard(entity)

        assertEquals("新建错题 interval 应为 0", 0, flashCard.interval)
        assertEquals(State.NEW, flashCard.state)
    }

    /**
     * 边界：schedNextReviewAt == schedLastReviewAt,interval=0（同一天复习）。
     */
    @Test
    fun `toFlashCard schedNextReviewAt 等于 schedLastReviewAt 时 interval 为 0`() {
        val sameTime = 5_000_000L
        val entity = sampleEntity(
            schedLastReviewAt = sameTime,
            schedNextReviewAt = sameTime,
        )

        val flashCard = WrongAnswerSchedulingMapper.toFlashCard(entity)

        assertEquals("相同时间戳 interval 应为 0", 0, flashCard.interval)
    }

    // ── toFlashCard: 防御性场景（follow-up #2 核心）──────────────────

    /**
     * v0.9.5 follow-up #2 核心测试：schedNextReviewAt < schedLastReviewAt 时 interval 强制为 0。
     *
     * 场景：时钟回拨或数据损坏导致 nextReviewAt < lastReviewAt。
     * 原实现（无 coerceAtLeast）会计算负 interval,FSRS 算法假设 interval >= 0,
     * 负值会导致 stability 计算异常（stability 公式含 interval 作为除数/乘数）。
     *
     * 修复后 coerceAtLeast(0) 确保下界,interval=0 表示"刚复习过"（FSRS 安全值）。
     */
    @Test
    fun `toFlashCard schedNextReviewAt 小于 schedLastReviewAt 时 interval 强制为 0`() {
        val entity = sampleEntity(
            schedLastReviewAt = 10_000_000L,
            schedNextReviewAt = 5_000_000L, // 比 lastReview 早 5_000_000ms
        )

        val flashCard = WrongAnswerSchedulingMapper.toFlashCard(entity)

        assertTrue(
            "interval 应 >= 0 (coerceAtLeast 保护),实际: ${flashCard.interval}",
            flashCard.interval >= 0,
        )
        assertEquals(
            "时钟回拨场景 interval 应强制为 0,而非负数",
            0,
            flashCard.interval,
        )
    }

    /**
     * 极端回拨：schedNextReviewAt 远小于 schedLastReviewAt（如回拨 365 天）。
     *
     * 确保即使回拨量很大,interval 也不会变成大负数（-365），而是 0。
     */
    @Test
    fun `toFlashCard 极端回拨 365 天 interval 仍为 0`() {
        val entity = sampleEntity(
            schedLastReviewAt = 10_000_000L,
            schedNextReviewAt = 10_000_000L - 365L * 86_400_000L, // 回拨 365 天
        )

        val flashCard = WrongAnswerSchedulingMapper.toFlashCard(entity)

        assertEquals("极端回拨 interval 应为 0", 0, flashCard.interval)
    }

    /**
     * 无效 schedState 字符串时回退到 State.NEW。
     */
    @Test
    fun `toFlashCard 无效 schedState 回退到 NEW`() {
        val entity = sampleEntity(schedState = "INVALID_STATE")

        val flashCard = WrongAnswerSchedulingMapper.toFlashCard(entity)

        assertEquals("无效 state 应回退到 NEW", State.NEW, flashCard.state)
    }

    // ── toSchedulingUpdate ────────────────────────────────────────

    /**
     * toSchedulingUpdate: FlashCard 调度结果正确映射到 SchedulingUpdate。
     */
    @Test
    fun `toSchedulingUpdate 正确映射所有字段`() {
        val flashCard = com.wenyan.app.core.fsrs.FlashCard(
            dueDate = java.time.LocalDateTime.of(2026, 7, 28, 12, 0),
            stability = 10.5f,
            difficulty = 4.2f,
            interval = 7,
            reviewCount = 3,
            lastReview = java.time.LocalDateTime.of(2026, 7, 21, 12, 0),
            state = State.REVIEW,
            elapsedDays = 7,
            scheduledDays = 7,
            reps = 3,
            lapses = 1,
        )

        val update = WrongAnswerSchedulingMapper.toSchedulingUpdate(flashCard)

        assertEquals("REVIEW", update.state)
        assertEquals(10.5f, update.stability)
        assertEquals(4.2f, update.difficulty)
        assertEquals(3, update.reviewCount)
        assertEquals(1, update.lapses)
        assertEquals(7, update.elapsedDays)
        assertEquals(7, update.scheduledDays)
        assertEquals(3, update.reps)
        assertTrue("nextReviewAt 应 > 0", update.nextReviewAt > 0)
        assertTrue("lastReviewAt 应 > 0", update.lastReviewAt > 0)
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    private fun sampleEntity(
        schedState: String = "REVIEW",
        schedStability: Float = 5f,
        schedDifficulty: Float = 5f,
        schedLastReviewAt: Long = 1_000_000L,
        schedNextReviewAt: Long = 2_000_000L,
        schedReviewCount: Int = 1,
        schedLapses: Int = 0,
        schedElapsedDays: Int = 0,
        schedScheduledDays: Int = 1,
        schedReps: Int = 1,
    ) = WrongAnswerEntity(
        id = "wa_test",
        pointId = "point_1",
        examQuestionId = null,
        userAnswer = "test answer",
        correctAnswer = "test correct",
        source = "CARD_AGAIN",
        wrongCount = 1,
        lastWrongAt = 500_000L,
        resolvedAt = null,
        aiExplanation = null,
        createdAt = 100_000L,
        schedState = schedState,
        schedStability = schedStability,
        schedDifficulty = schedDifficulty,
        schedLastReviewAt = schedLastReviewAt,
        schedNextReviewAt = schedNextReviewAt,
        schedReviewCount = schedReviewCount,
        schedLapses = schedLapses,
        schedElapsedDays = schedElapsedDays,
        schedScheduledDays = schedScheduledDays,
        schedReps = schedReps,
    )
}
