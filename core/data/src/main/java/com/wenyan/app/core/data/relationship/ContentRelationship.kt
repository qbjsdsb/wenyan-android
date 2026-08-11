package com.wenyan.app.core.data.relationship

enum class ContentRelationshipType {
    COMPARE_WITH,
    INFLUENCES,
    INFLUENCED_BY,
    PART_OF,
    EVIDENCE_FOR,
    EXAM_VARIANT,
    UNKNOWN,
}

enum class ContentRelationshipOrigin { EXPLICIT, AUTOMATIC_FALLBACK }

data class ContentRelationship(
    val sourceId: String,
    val targetId: String,
    val type: ContentRelationshipType,
    val origin: ContentRelationshipOrigin,
    val reason: String,
)

/**
 * Resolves legacy relation fields without pretending tag-derived links are reviewed facts.
 * Explicit fields win duplicate targets; missing/self targets are discarded.
 */
fun resolveKnowledgeRelationships(
    sourceId: String,
    automaticRelatedIds: List<String>,
    explicitCompareIds: List<String>,
    explicitDirectionUnknownIds: List<String>,
    existingIds: Set<String>,
): List<ContentRelationship> {
    val resolved = linkedMapOf<String, ContentRelationship>()

    explicitCompareIds.forEach { targetId ->
        if (targetId != sourceId && targetId in existingIds) {
            resolved[targetId] = ContentRelationship(
                sourceId,
                targetId,
                ContentRelationshipType.COMPARE_WITH,
                ContentRelationshipOrigin.EXPLICIT,
                "contrast_ids",
            )
        }
    }
    explicitDirectionUnknownIds.forEach { targetId ->
        if (targetId != sourceId && targetId in existingIds && targetId !in resolved) {
            resolved[targetId] = ContentRelationship(
                sourceId,
                targetId,
                ContentRelationshipType.UNKNOWN,
                ContentRelationshipOrigin.EXPLICIT,
                "extension_ids（旧字段未保存方向）",
            )
        }
    }
    automaticRelatedIds.forEach { targetId ->
        if (targetId != sourceId && targetId in existingIds && targetId !in resolved) {
            resolved[targetId] = ContentRelationship(
                sourceId,
                targetId,
                ContentRelationshipType.UNKNOWN,
                ContentRelationshipOrigin.AUTOMATIC_FALLBACK,
                "同科目标签相交的旧版自动关联",
            )
        }
    }
    return resolved.values.toList()
}

fun resolveExamVariantRelationships(
    questionId: String,
    relatedPointIds: List<String>,
    existingPointIds: Set<String>,
): List<ContentRelationship> = relatedPointIds.distinct().mapNotNull { pointId ->
    pointId.takeIf(existingPointIds::contains)?.let {
        ContentRelationship(
            sourceId = questionId,
            targetId = it,
            type = ContentRelationshipType.EXAM_VARIANT,
            origin = ContentRelationshipOrigin.EXPLICIT,
            reason = "exam_questions.related_point_ids",
        )
    }
}
