package com.wenyan.app.feature.knowledge

import com.wenyan.app.core.database.entity.KnowledgePointEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KnowledgeStudyLayersTest {
    @Test
    fun `layers reveal in the contracted order`() {
        assertEquals(KnowledgeStudyLayer.RECALL_30_SECONDS, nextStudyLayer(emptySet()))
        assertEquals(
            KnowledgeStudyLayer.OUTLINE_2_MINUTES,
            nextStudyLayer(setOf(KnowledgeStudyLayer.RECALL_30_SECONDS)),
        )
        assertNull(nextStudyLayer(KnowledgeStudyLayer.entries.toSet()))
    }

    @Test
    fun `missing outline stays empty instead of being invented`() {
        val point = KnowledgePointEntity(
            id = "kp",
            chapterId = "ch",
            title = "题目",
            summary = null,
            coreConclusion = "结论",
            fullContent = "",
            multiPerspectives = null,
            relatedIds = null,
            contrastIds = null,
            extensionIds = null,
            examRecords = null,
            examFrequency = "NEVER",
            termTemplate = null,
            tags = null,
            createdAt = 0,
            updatedAt = 0,
            contentSource = null,
            sourceFile = null,
            sourcePage = null,
            studyText = null,
        )
        val layers = point.studyLayerContents().associateBy { it.layer }

        assertNull(layers.getValue(KnowledgeStudyLayer.OUTLINE_2_MINUTES).content)
        assertEquals("结论", layers.getValue(KnowledgeStudyLayer.EXAM_EXPRESSION).content)
    }

    @Test
    fun `saved reveal names restore safely and ignore future unknown values`() {
        assertEquals(
            setOf(KnowledgeStudyLayer.RECALL_30_SECONDS, KnowledgeStudyLayer.EXAM_EXPRESSION),
            decodeStudyLayers(listOf("RECALL_30_SECONDS", "FUTURE_LAYER", "EXAM_EXPRESSION")),
        )
    }
}
