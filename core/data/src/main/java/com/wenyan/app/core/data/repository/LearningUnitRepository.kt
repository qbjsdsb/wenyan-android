package com.wenyan.app.core.data.repository

import androidx.room.withTransaction
import com.wenyan.app.core.data.learning.LearningUnitSynchronizer
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearningUnitRepository @Inject constructor(
    private val database: WenyanDatabase,
) {
    /** Explicit synchronization path, invoked only by the version-gated seed import or callers. */
    suspend fun synchronize(point: KnowledgePointEntity, now: Long) = database.withTransaction {
        synchronizeInCurrentTransaction(point, now)
    }

    suspend fun synchronizeAll(points: List<KnowledgePointEntity>, now: Long) = database.withTransaction {
        points.forEach { synchronizeInCurrentTransaction(it, now) }
    }

    private suspend fun synchronizeInCurrentTransaction(point: KnowledgePointEntity, now: Long) {
        val unitDao = database.learningUnitDao()
        val recordDao = database.learningUnitRecordDao()
        val plan = LearningUnitSynchronizer.plan(
            point = point,
            existingUnits = unitDao.getByPoint(point.id),
            existingRecords = recordDao.getByPoint(point.id),
            memoRecord = database.memoRecordDao().getById(point.id),
            now = now,
        )
        unitDao.upsertAll(plan.unitsToUpsert)
        unitDao.deactivateMissing(point.id, plan.activeIds, now)
        recordDao.insertAll(plan.recordsToInsert)
    }
}
