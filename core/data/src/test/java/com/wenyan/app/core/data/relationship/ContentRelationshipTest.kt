package com.wenyan.app.core.data.relationship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentRelationshipTest {
    @Test
    fun `explicit relation wins fallback duplicate and dangling targets are removed`() {
        val result = resolveKnowledgeRelationships(
            sourceId = "a",
            automaticRelatedIds = listOf("b", "ghost", "a"),
            explicitCompareIds = listOf("b"),
            explicitDirectionUnknownIds = listOf("c"),
            existingIds = setOf("a", "b", "c"),
        )

        assertEquals(listOf("b", "c"), result.map { it.targetId })
        assertEquals(ContentRelationshipType.COMPARE_WITH, result.first().type)
        assertEquals(ContentRelationshipOrigin.EXPLICIT, result.first().origin)
        assertEquals(ContentRelationshipType.UNKNOWN, result.last().type)
    }

    @Test
    fun `fallback remains visibly automatic and never claims direction`() {
        val relation = resolveKnowledgeRelationships("a", listOf("b"), emptyList(), emptyList(), setOf("b")).single()

        assertEquals(ContentRelationshipOrigin.AUTOMATIC_FALLBACK, relation.origin)
        assertEquals(ContentRelationshipType.UNKNOWN, relation.type)
        assertTrue(relation.reason.contains("自动关联"))
    }

    @Test
    fun `eq 0038 explicit links remain exam variants with stable order and deduplication`() {
        val result = resolveExamVariantRelationships(
            questionId = "eq_0038",
            relatedPointIds = listOf("kp_1", "kp_2", "kp_1", "ghost"),
            existingPointIds = setOf("kp_1", "kp_2"),
        )

        assertEquals(listOf("kp_1", "kp_2"), result.map { it.targetId })
        assertTrue(result.all { it.type == ContentRelationshipType.EXAM_VARIANT })
        assertTrue(result.all { it.origin == ContentRelationshipOrigin.EXPLICIT })
    }

    @Test
    fun `all contracted relation types remain representable`() {
        assertEquals(
            setOf("COMPARE_WITH", "INFLUENCES", "INFLUENCED_BY", "PART_OF", "EVIDENCE_FOR", "EXAM_VARIANT", "UNKNOWN"),
            ContentRelationshipType.entries.map { it.name }.toSet(),
        )
    }
}
