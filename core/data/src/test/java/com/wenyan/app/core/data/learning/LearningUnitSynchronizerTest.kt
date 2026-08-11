package com.wenyan.app.core.data.learning

import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.LearningUnitRecordEntity
import com.wenyan.app.core.database.entity.MemoRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningUnitSynchronizerTest {
    @Test
    fun `no reliable structure produces core only`() {
        val units = LearningUnitGenerator.generate(point(tags = null), now = 10)
        assertEquals(listOf("kp_test:core:0"), units.map { it.id })
    }

    @Test
    fun `generation is deterministic and wording changes do not change ids`() {
        val first = LearningUnitGenerator.generate(point(title = "旧标题", conclusion = "旧结论"), 10)
        val second = LearningUnitGenerator.generate(point(title = "新标题", conclusion = "新结论"), 20)
        assertEquals(first.map { it.id }, second.map { it.id })
        assertFalse(first.map { it.prompt } == second.map { it.prompt })
    }

    @Test
    fun `appending keyword never reorders existing ids`() {
        val old = LearningUnitGenerator.generate(point(tags = listOf("甲", "乙")), 10)
        val plan = LearningUnitSynchronizer.plan(
            point(tags = listOf("甲", "乙", "丙")), old, emptyList(), null, 20,
        )
        assertEquals(listOf("kp_test:keyword:0", "kp_test:keyword:1"), plan.activeIds.filter { ":keyword:" in it }.take(2))
        assertTrue("kp_test:keyword:2" in plan.activeIds)
    }

    @Test
    fun `removed keyword is omitted for deactivation and survivor keeps position`() {
        val old = LearningUnitGenerator.generate(point(tags = listOf("甲", "乙")), 10)
        val plan = LearningUnitSynchronizer.plan(point(tags = listOf("乙")), old, emptyList(), null, 20)
        assertFalse("kp_test:keyword:0" in plan.activeIds)
        assertTrue("kp_test:keyword:1" in plan.activeIds)
    }

    @Test
    fun `legacy memo is copied only to core and other units start new`() {
        val memo = memo(state = "REVIEW", stability = 8f, reviewCount = 7)
        val plan = LearningUnitSynchronizer.plan(point(), emptyList(), emptyList(), memo, 20)
        val core = plan.recordsToInsert.single { ":core:" in it.learningUnitId }
        val keyword = plan.recordsToInsert.single { ":keyword:" in it.learningUnitId }
        assertEquals("REVIEW", core.state)
        assertEquals(8f, core.stability)
        assertEquals(7, core.reviewCount)
        assertEquals("NEW", keyword.state)
        assertEquals(0, keyword.reviewCount)
    }

    @Test
    fun `repeated synchronization preserves units and user records`() {
        val first = LearningUnitSynchronizer.plan(point(), emptyList(), emptyList(), memo(), 20)
        val userRecords = first.recordsToInsert.map {
            if (":keyword:" in it.learningUnitId) it.copy(state = "REVIEW", reps = 3) else it
        }
        val second = LearningUnitSynchronizer.plan(point(), first.unitsToUpsert, userRecords, memo(), 99)
        assertEquals(first.unitsToUpsert, second.unitsToUpsert)
        assertTrue(second.recordsToInsert.isEmpty())
        assertEquals(3, userRecords.single { ":keyword:" in it.learningUnitId }.reps)
    }

    private fun point(
        title: String = "标题",
        conclusion: String = "结论",
        tags: List<String>? = listOf("关键词"),
    ) = KnowledgePointEntity(
        id = "kp_test", chapterId = "chapter", title = title, summary = null,
        coreConclusion = conclusion, fullContent = conclusion, multiPerspectives = null,
        relatedIds = null, contrastIds = null, extensionIds = null, examRecords = null,
        examFrequency = "HIGH", termTemplate = null, tags = tags, difficulty = 3,
        createdAt = 1, updatedAt = 1, contentSource = "TEXTBOOK_NATIVE", ocrStatus = "VERIFIED",
        sourceFile = null, sourcePage = null, studyText = null,
    )

    private fun memo(
        state: String = "NEW",
        stability: Float = 0f,
        reviewCount: Int = 0,
    ) = MemoRecordEntity(
        pointId = "kp_test", state = state, stability = stability, difficulty = 5f,
        lastReviewAt = 0, nextReviewAt = 10, reviewCount = reviewCount,
    )
}
