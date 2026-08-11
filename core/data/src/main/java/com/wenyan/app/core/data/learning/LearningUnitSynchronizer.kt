package com.wenyan.app.core.data.learning

import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.LearningUnitEntity
import com.wenyan.app.core.database.entity.LearningUnitId
import com.wenyan.app.core.database.entity.LearningUnitRecordEntity
import com.wenyan.app.core.database.entity.LearningUnitType
import com.wenyan.app.core.database.entity.MemoRecordEntity

data class LearningUnitSyncPlan(
    val unitsToUpsert: List<LearningUnitEntity>,
    val activeIds: List<String>,
    val recordsToInsert: List<LearningUnitRecordEntity>,
)

/** Pure reconciliation keeps surviving keyword positions and never overwrites existing records. */
object LearningUnitSynchronizer {
    fun plan(
        point: KnowledgePointEntity,
        existingUnits: List<LearningUnitEntity>,
        existingRecords: List<LearningUnitRecordEntity>,
        memoRecord: MemoRecordEntity?,
        now: Long,
    ): LearningUnitSyncPlan {
        val drafts = LearningUnitGenerator.generate(point, now)
        val existingById = existingUnits.associateBy(LearningUnitEntity::id)
        val existingKeywordByAnswer = existingUnits
            .filter { it.unitType == LearningUnitType.KEYWORD.name }
            .associateBy(LearningUnitEntity::answer)
        var nextKeywordPosition = existingUnits
            .filter { it.unitType == LearningUnitType.KEYWORD.name }
            .maxOfOrNull(LearningUnitEntity::position)
            ?.plus(1) ?: 0

        val desired = drafts.map { draft ->
            val stableDraft = if (draft.unitType == LearningUnitType.KEYWORD.name) {
                existingKeywordByAnswer[draft.answer]?.let { existing ->
                    draft.copy(id = existing.id, position = existing.position)
                } ?: draft.copy(
                    id = LearningUnitId.create(point.id, LearningUnitType.KEYWORD, nextKeywordPosition),
                    position = nextKeywordPosition++,
                )
            } else {
                draft
            }
            existingById[stableDraft.id]?.let { existing ->
                val updated = stableDraft.copy(createdAt = existing.createdAt)
                if (existing.copy(active = true) == updated.copy(updatedAt = existing.updatedAt)) {
                    existing.copy(active = true)
                } else {
                    updated
                }
            } ?: stableDraft
        }

        val recordIds = existingRecords.mapTo(hashSetOf(), LearningUnitRecordEntity::learningUnitId)
        val records = desired.filterNot { it.id in recordIds }.map { unit ->
            if (unit.unitType == LearningUnitType.CORE.name && memoRecord != null) {
                memoRecord.toUnitRecord(unit.id)
            } else {
                LearningUnitRecordEntity(
                    learningUnitId = unit.id,
                    state = "NEW",
                    lastReviewAt = 0,
                    nextReviewAt = now,
                )
            }
        }
        return LearningUnitSyncPlan(desired, desired.map(LearningUnitEntity::id), records)
    }

    private fun MemoRecordEntity.toUnitRecord(unitId: String) = LearningUnitRecordEntity(
        learningUnitId = unitId,
        state = state,
        stability = stability,
        difficulty = difficulty,
        lastReviewAt = lastReviewAt,
        nextReviewAt = nextReviewAt,
        reviewCount = reviewCount,
        failCount = failCount,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reps = reps,
        inPriorityQueue = inPriorityQueue,
    )
}
