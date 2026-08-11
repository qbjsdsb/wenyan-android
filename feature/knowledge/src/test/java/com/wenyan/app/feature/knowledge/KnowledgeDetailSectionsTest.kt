package com.wenyan.app.feature.knowledge

import org.junit.Assert.assertEquals
import org.junit.Test

class KnowledgeDetailSectionsTest {
    @Test
    fun `all sections retain the legacy detail order`() {
        val sections = visibleKnowledgeDetailSections(
            KnowledgeDetailSectionVisibility(
                recall = true,
                outlineAndExplanation = true,
                evidence = true,
                relatedPoints = true,
                relatedEssays = true,
                wrongAnswers = true,
            ),
        )

        assertEquals(
            listOf(
                KnowledgeDetailSection.RECALL,
                KnowledgeDetailSection.OUTLINE_AND_EXPLANATION,
                KnowledgeDetailSection.EVIDENCE,
                KnowledgeDetailSection.RELATED_POINTS,
                KnowledgeDetailSection.RELATED_ESSAYS,
                KnowledgeDetailSection.WRONG_ANSWERS,
            ),
            sections,
        )
    }

    @Test
    fun `empty sections are omitted without moving remaining sections`() {
        val sections = visibleKnowledgeDetailSections(
            KnowledgeDetailSectionVisibility(
                recall = false,
                outlineAndExplanation = true,
                evidence = false,
                relatedPoints = false,
                relatedEssays = true,
                wrongAnswers = false,
            ),
        )

        assertEquals(
            listOf(
                KnowledgeDetailSection.OUTLINE_AND_EXPLANATION,
                KnowledgeDetailSection.RELATED_ESSAYS,
            ),
            sections,
        )
    }
}
