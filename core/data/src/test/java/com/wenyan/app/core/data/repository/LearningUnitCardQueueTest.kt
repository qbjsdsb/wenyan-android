package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.entity.LearningUnitEntity
import com.wenyan.app.core.database.entity.LearningUnitRecordEntity
import com.wenyan.app.core.database.entity.LearningUnitWithRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningUnitCardQueueTest {
    @Test
    fun `siblings are interleaved without suppressing any unit`() {
        val actual = interleaveUnitSiblings(
            listOf(
                listOf("a0", "a1", "a2"),
                listOf("b0", "b1"),
                listOf("c0"),
            ),
        )

        assertEquals(listOf("a0", "b0", "c0", "a1", "b1", "a2"), actual)
    }

    @Test
    fun `empty groups do not change stable group order`() {
        assertEquals(
            listOf("a0", "b0", "a1"),
            interleaveUnitSiblings(listOf(emptyList(), listOf("a0", "a1"), listOf("b0"))),
        )
    }

    @Test
    fun `persisted future record becomes due after clock crosses boundary`() {
        val future = item("future", state = "REVIEW", nextReviewAt = 2_000, reps = 1)
        val fresh = item("fresh", state = "NEW", nextReviewAt = Long.MAX_VALUE)

        assertEquals(listOf("fresh"), selectDueLearningUnits(listOf(future, fresh), 1_999).map { it.unit.id })
        assertEquals(listOf("future", "fresh"), selectDueLearningUnits(listOf(future, fresh), 2_000).map { it.unit.id })
    }

    private fun item(id: String, state: String, nextReviewAt: Long, reps: Int = 0) =
        LearningUnitWithRecord(
            unit = LearningUnitEntity(id, "point", "CORE", 0, "Q", "A", true, 1, 1),
            records = listOf(
                LearningUnitRecordEntity(
                    learningUnitId = id,
                    state = state,
                    lastReviewAt = 0,
                    nextReviewAt = nextReviewAt,
                    reviewCount = reps,
                    reps = reps,
                ),
            ),
        )
}
