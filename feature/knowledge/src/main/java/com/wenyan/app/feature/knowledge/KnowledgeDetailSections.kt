package com.wenyan.app.feature.knowledge

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

/** Stable presentation order for the knowledge-detail page. */
internal enum class KnowledgeDetailSection(
    val key: String,
    val contentType: String = key,
) {
    RECALL("summary"),
    OUTLINE_AND_EXPLANATION("multi_perspective"),
    EVIDENCE("sources"),
    RELATED_POINTS("related_points"),
    RELATED_ESSAYS("related_essays"),
    WRONG_ANSWERS("wrong_answers"),
}

internal data class KnowledgeDetailSectionVisibility(
    val recall: Boolean,
    val outlineAndExplanation: Boolean,
    val evidence: Boolean,
    val relatedPoints: Boolean,
    val relatedEssays: Boolean,
    val wrongAnswers: Boolean,
)

/**
 * Pure order contract used by the screen and its regression test.
 *
 * Keeping this independent of Compose also makes it explicit that the C13 extraction does not
 * reorder, synthesize, or query any content.
 */
internal fun visibleKnowledgeDetailSections(
    visibility: KnowledgeDetailSectionVisibility,
): List<KnowledgeDetailSection> = buildList {
    if (visibility.recall) add(KnowledgeDetailSection.RECALL)
    if (visibility.outlineAndExplanation) add(KnowledgeDetailSection.OUTLINE_AND_EXPLANATION)
    if (visibility.evidence) add(KnowledgeDetailSection.EVIDENCE)
    if (visibility.relatedPoints) add(KnowledgeDetailSection.RELATED_POINTS)
    if (visibility.relatedEssays) add(KnowledgeDetailSection.RELATED_ESSAYS)
    if (visibility.wrongAnswers) add(KnowledgeDetailSection.WRONG_ANSWERS)
}

/** Small slot-based components: Recall, Outline/Explanation, Evidence, and Relations. */
internal fun LazyListScope.knowledgeDetailSections(
    visibility: KnowledgeDetailSectionVisibility,
    recall: @Composable () -> Unit,
    outlineAndExplanation: @Composable () -> Unit,
    evidence: @Composable () -> Unit,
    relatedPoints: @Composable () -> Unit,
    relatedEssays: @Composable () -> Unit,
    wrongAnswers: @Composable () -> Unit,
) {
    visibleKnowledgeDetailSections(visibility).forEach { section ->
        item(key = section.key, contentType = section.contentType) {
            when (section) {
                KnowledgeDetailSection.RECALL -> recall()
                KnowledgeDetailSection.OUTLINE_AND_EXPLANATION -> outlineAndExplanation()
                KnowledgeDetailSection.EVIDENCE -> evidence()
                KnowledgeDetailSection.RELATED_POINTS -> relatedPoints()
                KnowledgeDetailSection.RELATED_ESSAYS -> relatedEssays()
                KnowledgeDetailSection.WRONG_ANSWERS -> wrongAnswers()
            }
        }
    }
}
