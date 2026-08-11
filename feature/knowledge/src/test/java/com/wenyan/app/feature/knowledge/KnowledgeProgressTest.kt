package com.wenyan.app.feature.knowledge

import com.wenyan.app.core.database.entity.LearningUnitEntity
import com.wenyan.app.core.database.entity.LearningUnitRecordEntity
import com.wenyan.app.core.database.entity.LearningUnitWithRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KnowledgeProgressTest {
    @Test
    fun noAttemptsNeverClaimsRememberedOrWritable() {
        val result = calculateKnowledgeProgress(listOf(unit("a", null)), nowMillis = 100)
        assertEquals("尚未学习", result.seen)
        assertEquals("尚无回忆记录", result.remembered)
        assertEquals("尚未练习", result.writable)
    }

    @Test
    fun dueStateUsesCountsWithoutFalsePrecision() {
        val result = calculateKnowledgeProgress(
            listOf(unit("a", record("a", 200)), unit("b", record("b", 50))),
            nowMillis = 100,
        )
        assertEquals("已学习全部 2 个单元", result.seen)
        assertEquals("1/2 个已练单元未到期", result.remembered)
        assertFalse(result.remembered.contains('.'))
        assertEquals("尚未练习", result.writable)
    }

    private fun unit(id: String, record: LearningUnitRecordEntity?) = LearningUnitWithRecord(
        unit = LearningUnitEntity(id, "kp", "CORE", 0, id, id, true, 0, 0),
        records = listOfNotNull(record),
    )

    private fun record(id: String, next: Long) = LearningUnitRecordEntity(
        learningUnitId = id,
        state = "REVIEW",
        lastReviewAt = 1,
        nextReviewAt = next,
        reviewCount = 1,
    )
}
