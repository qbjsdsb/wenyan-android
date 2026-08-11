package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.LearningUnitDao
import com.wenyan.app.core.database.entity.LearningUnitWithRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface KnowledgeProgressSource {
    fun observe(pointId: String): Flow<List<LearningUnitWithRecord>>
}

/** Read-only evidence used to explain progress on one knowledge point. */
@Singleton
class KnowledgeProgressRepository @Inject constructor(
    private val learningUnitDao: LearningUnitDao,
) : KnowledgeProgressSource {
    override fun observe(pointId: String): Flow<List<LearningUnitWithRecord>> =
        learningUnitDao.observeActiveWithRecordsByPoint(pointId)
}
