package com.wenyan.app.core.data.seed

import com.wenyan.app.core.database.entity.ContentReviewStatus
import com.wenyan.app.core.database.entity.SourceEvidenceType

/** Fail-closed mapping from optional seed metadata to persisted provenance values. */
internal object SeedProvenanceMapper {
    fun contentStatus(vararg declaredValues: String?): String {
        val declared = declaredValues.firstOrNull { !it.isNullOrBlank() }
        return ContentReviewStatus.fromStorage(declared).name
    }

    fun sourceStatus(declaredValue: String?, sourceTitle: String?): String {
        if (sourceTitle.isNullOrBlank()) return SourceEvidenceType.UNKNOWN.name
        return SourceEvidenceType.fromStorage(declaredValue).name
    }

    fun isFormalLearningContent(storedValue: String?): Boolean =
        ContentReviewStatus.fromStorage(storedValue) !in setOf(
            ContentReviewStatus.AI_DRAFT,
            ContentReviewStatus.REJECTED,
        )
}
