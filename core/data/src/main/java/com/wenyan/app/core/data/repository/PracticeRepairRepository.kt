package com.wenyan.app.core.data.repository

import androidx.room.withTransaction
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.DailyTaskEntity
import com.wenyan.app.core.database.entity.PracticeRepairState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeRepairRepository @Inject constructor(
    private val database: WenyanDatabase,
) {
    suspend fun scheduleForLaterDate(
        attemptId: String,
        targetDate: LocalDate,
        now: Long,
        zoneId: ZoneId = ZoneId.of("Asia/Taipei"),
    ): DailyTaskEntity = database.withTransaction {
        val attemptDao = database.practiceAttemptDao()
        val attempt = requireNotNull(attemptDao.getById(attemptId))
        require(attempt.repairState == PracticeRepairState.CANDIDATE.name || attempt.repairState == PracticeRepairState.SCHEDULED.name)
        val sourceDate = Instant.ofEpochMilli(attempt.completedAt ?: attempt.updatedAt).atZone(zoneId).toLocalDate()
        requireRepairDate(sourceDate, targetDate)
        val plan = requireNotNull(database.dailyPlanDao().getEntityByDate(targetDate.toString()))
        val stableId = repairStableId(attempt.id)
        database.dailyTaskDao().getByStableId(plan.id, stableId)?.let { return@withTransaction it }
        val existing = database.dailyTaskDao().getByPlan(plan.id)
        val task = DailyTaskEntity(
            id = "${plan.id}:$stableId", planId = plan.id, stableId = stableId,
            position = (existing.maxOfOrNull { it.position } ?: -1) + 1,
            taskType = "REPAIR", contentId = attempt.questionId, learningUnitId = attempt.learningUnitId,
            estimatedMinutes = 10, status = "PENDING", carriedFromTaskId = null,
            createdAt = now, updatedAt = now,
        )
        database.dailyTaskDao().insert(task)
        check(attemptDao.update(attempt.copy(repairState = PracticeRepairState.SCHEDULED.name, updatedAt = now)) == 1)
        task
    }
}

internal fun requireRepairDate(sourceDate: LocalDate, targetDate: LocalDate) {
    require(targetDate > sourceDate) { "repair must enter a later day, never reorder today" }
}

internal fun repairStableId(attemptId: String): String {
    require(attemptId.isNotBlank())
    return "repair:practice:$attemptId"
}
