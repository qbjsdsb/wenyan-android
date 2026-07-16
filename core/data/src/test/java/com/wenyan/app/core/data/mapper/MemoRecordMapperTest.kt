package com.wenyan.app.core.data.mapper

import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.fsrs.FlashCard
import com.wenyan.app.core.fsrs.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * [MemoRecordMapper] 单元测试。
 *
 * NF-T4 修复后，MemoRecordEntity 的 stability/difficulty 改为 Float，
 * 与 FSRS FlashCard 类型一致，Mapper 转换变为恒等（无精度损失）。
 *
 * 覆盖：
 * - Entity→FlashCard→Entity round-trip 一致性
 * - FlashCard→Entity→FlashCard round-trip 一致性
 * - 边界值（stability=0 / 720, difficulty=1 / 10）
 * - 默认值正确性
 * - state 枚举映射
 */
class MemoRecordMapperTest {

    @Test
    fun `entity to flashCard to entity round-trip preserves stability and difficulty`() {
        val entity = MemoRecordEntity(
            pointId = "point_001",
            state = "REVIEW",
            stability = 42.5f,
            difficulty = 6.3f,
            lastReviewAt = 1_000_000L,
            nextReviewAt = 2_000_000L,
            reviewCount = 10,
            failCount = 2,
            elapsedDays = 5,
            scheduledDays = 7,
            reps = 10,
            inPriorityQueue = 1,
        )

        val flashCard = MemoRecordMapper.toFlashCard(entity)
        val roundTripped = MemoRecordMapper.toMemoRecord(
            flashCard = flashCard,
            pointId = entity.pointId,
            inPriorityQueue = entity.inPriorityQueue != 0,
        )

        assertEquals(entity.pointId, roundTripped.pointId)
        assertEquals(entity.stability, roundTripped.stability)
        assertEquals(entity.difficulty, roundTripped.difficulty)
        assertEquals(entity.lastReviewAt, roundTripped.lastReviewAt)
        assertEquals(entity.nextReviewAt, roundTripped.nextReviewAt)
        assertEquals(entity.reviewCount, roundTripped.reviewCount)
        assertEquals(entity.failCount, roundTripped.failCount)
        assertEquals(entity.reps, roundTripped.reps)
        assertEquals(entity.inPriorityQueue, roundTripped.inPriorityQueue)
    }

    @Test
    fun `flashCard to entity to flashCard round-trip preserves stability and difficulty`() {
        val flashCard = FlashCard(
            dueDate = LocalDateTime.of(2026, 7, 16, 10, 0),
            stability = 15.7f,
            difficulty = 4.2f,
            interval = 3,
            reviewCount = 8,
            lastReview = LocalDateTime.of(2026, 7, 13, 10, 0),
            state = State.REVIEW,
            elapsedDays = 3,
            scheduledDays = 3,
            reps = 8,
            lapses = 1,
        )

        val entity = MemoRecordMapper.toMemoRecord(
            flashCard = flashCard,
            pointId = "point_002",
            inPriorityQueue = false,
        )
        val roundTripped = MemoRecordMapper.toFlashCard(entity)

        assertEquals(flashCard.stability, roundTripped.stability)
        assertEquals(flashCard.difficulty, roundTripped.difficulty)
        assertEquals(flashCard.state, roundTripped.state)
        assertEquals(flashCard.reviewCount, roundTripped.reviewCount)
        assertEquals(flashCard.lapses, roundTripped.lapses)
        assertEquals(flashCard.reps, roundTripped.reps)
    }

    @Test
    fun `edge value stability zero is preserved`() {
        val entity = createEntity(stability = 0f, difficulty = 5f)
        val flashCard = MemoRecordMapper.toFlashCard(entity)
        assertEquals(0f, flashCard.stability)
        val roundTripped = MemoRecordMapper.toMemoRecord(flashCard, "point_003")
        assertEquals(0f, roundTripped.stability)
    }

    @Test
    fun `edge value stability 720 is preserved`() {
        val entity = createEntity(stability = 720f, difficulty = 5f)
        val flashCard = MemoRecordMapper.toFlashCard(entity)
        assertEquals(720f, flashCard.stability)
        val roundTripped = MemoRecordMapper.toMemoRecord(flashCard, "point_004")
        assertEquals(720f, roundTripped.stability)
    }

    @Test
    fun `edge value difficulty 1 is preserved`() {
        val entity = createEntity(stability = 10f, difficulty = 1f)
        val flashCard = MemoRecordMapper.toFlashCard(entity)
        assertEquals(1f, flashCard.difficulty)
        val roundTripped = MemoRecordMapper.toMemoRecord(flashCard, "point_005")
        assertEquals(1f, roundTripped.difficulty)
    }

    @Test
    fun `edge value difficulty 10 is preserved`() {
        val entity = createEntity(stability = 10f, difficulty = 10f)
        val flashCard = MemoRecordMapper.toFlashCard(entity)
        assertEquals(10f, flashCard.difficulty)
        val roundTripped = MemoRecordMapper.toMemoRecord(flashCard, "point_006")
        assertEquals(10f, roundTripped.difficulty)
    }

    @Test
    fun `default inPriorityQueue is false (0)`() {
        val flashCard = FlashCard(
            stability = 5f,
            difficulty = 5f,
            state = State.NEW,
        )
        val entity = MemoRecordMapper.toMemoRecord(flashCard, "point_007")
        assertEquals(0, entity.inPriorityQueue)
    }

    @Test
    fun `inPriorityQueue true maps to 1`() {
        val flashCard = FlashCard(
            stability = 5f,
            difficulty = 5f,
            state = State.NEW,
        )
        val entity = MemoRecordMapper.toMemoRecord(flashCard, "point_008", inPriorityQueue = true)
        assertEquals(1, entity.inPriorityQueue)
    }

    @Test
    fun `invalid state string falls back to NEW`() {
        val entity = createEntity(state = "INVALID_STATE")
        val flashCard = MemoRecordMapper.toFlashCard(entity)
        assertEquals(State.NEW, flashCard.state)
    }

    @Test
    fun `lastReviewAt zero produces null lastReview in FlashCard`() {
        val entity = createEntity(lastReviewAt = 0L)
        val flashCard = MemoRecordMapper.toFlashCard(entity)
        assertTrue(flashCard.lastReview == null)
    }

    @Test
    fun `NF-T4 no precision loss - Float type is identity conversion`() {
        // NF-T4 核心验证：Entity 和 FlashCard 都是 Float，转换是恒等的
        val testValues = listOf(0f, 0.001f, 1.5f, 42.123f, 720f, Float.MAX_VALUE, Float.MIN_VALUE)
        for (value in testValues) {
            val entity = createEntity(stability = value, difficulty = value)
            val flashCard = MemoRecordMapper.toFlashCard(entity)
            assertEquals("stability=$value should be preserved", value, flashCard.stability)
            assertEquals("difficulty=$value should be preserved", value, flashCard.difficulty)
        }
    }

    private fun createEntity(
        pointId: String = "point_test",
        state: String = "REVIEW",
        stability: Float = 10f,
        difficulty: Float = 5f,
        lastReviewAt: Long = 1_000_000L,
        nextReviewAt: Long = 2_000_000L,
    ): MemoRecordEntity {
        return MemoRecordEntity(
            pointId = pointId,
            state = state,
            stability = stability,
            difficulty = difficulty,
            lastReviewAt = lastReviewAt,
            nextReviewAt = nextReviewAt,
            reviewCount = 5,
            failCount = 1,
            elapsedDays = 3,
            scheduledDays = 3,
            reps = 5,
            inPriorityQueue = 0,
        )
    }
}
