package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.WritingMaterialDao
import com.wenyan.app.core.database.entity.ContentReviewStatus
import kotlinx.coroutines.flow.Flow
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

interface WritingEvidenceSource { val evidence: Flow<List<WritingEvidenceItem>> }

@Singleton
class WritingEvidenceRepository @Inject constructor(
    writingMaterialDao: WritingMaterialDao,
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
}
