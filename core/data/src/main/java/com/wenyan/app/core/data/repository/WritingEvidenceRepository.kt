package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.WritingMaterialDao
import com.wenyan.app.core.database.entity.ContentReviewStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class WritingEvidenceItem(
    val id: String,
    val title: String?,
    val preview: String,
    val reviewStatus: ContentReviewStatus,
    val sourceLabels: List<String>,
) {
    val isCitable: Boolean get() = reviewStatus == ContentReviewStatus.REVIEWED
}

/** Full text for the one material explicitly selected to seed a writing session. */
data class WritingEvidenceDetail(
    val id: String,
    val title: String?,
    val content: String,
    val reviewStatus: ContentReviewStatus,
) {
    val isCitable: Boolean get() = reviewStatus == ContentReviewStatus.REVIEWED
}

interface WritingEvidenceSource {
    val evidence: Flow<List<WritingEvidenceItem>>

    /** Fetch full content lazily; list flows intentionally retain only previews. */
    suspend fun get(id: String): WritingEvidenceDetail? = evidence.first()
        .firstOrNull { it.id == id }
        ?.let { item -> WritingEvidenceDetail(item.id, item.title, item.preview, item.reviewStatus) }
}

@Singleton
class WritingEvidenceRepository @Inject constructor(
    private val writingMaterialDao: WritingMaterialDao,
) : WritingEvidenceSource {
    override val evidence = writingMaterialDao.observeAllWithSources().map { materials ->
        materials.map { item ->
            WritingEvidenceItem(
                id = item.material.id,
                title = item.material.title,
                preview = item.material.content.take(160),
                reviewStatus = ContentReviewStatus.fromStorage(item.material.contentStatus),
                sourceLabels = item.sources.mapNotNull { it.sourceTitle ?: it.sourceFile.takeIf(String::isNotBlank) },
            )
        }
    }

    override suspend fun get(id: String): WritingEvidenceDetail? = writingMaterialDao.getById(id)?.let { material ->
        WritingEvidenceDetail(
            id = material.id,
            title = material.title,
            content = material.content,
            reviewStatus = ContentReviewStatus.fromStorage(material.contentStatus),
        )
    }
}
