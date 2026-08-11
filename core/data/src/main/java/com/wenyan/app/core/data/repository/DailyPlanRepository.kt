package com.wenyan.app.core.data.repository

import androidx.room.withTransaction
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.DailyPlanEntity
import com.wenyan.app.core.database.entity.DailyPlanWithTasks
import com.wenyan.app.core.database.entity.DailyTaskEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

enum class LegacyDecision { CARRY, SKIP, SPECIAL_SESSION }

data class DailyPlanDraft(
    val plan: DailyPlanEntity,
    val tasks: List<DailyTaskEntity>,
)

@Singleton
class DailyPlanRepository @Inject constructor(
    private val database: WenyanDatabase,
) {
    fun observe(date: String): Flow<DailyPlanWithTasks?> =
        database.dailyPlanDao().observeEntityByDate(date).flatMapLatest { plan ->
            if (plan == null) flowOf(null) else database.dailyTaskDao().observeByPlan(plan.id)
                .map { DailyPlanWithTasks(plan, it) }
        }
    /** Atomic read-today-or-generate. ABORT task inserts roll the entire new plan back. */
    suspend fun getOrCreate(
        date: String,
        generate: suspend () -> DailyPlanDraft,
    ): DailyPlanWithTasks = database.withTransaction {
        load(date)?.let { return@withTransaction it }
        val draft = generate()
        require(draft.plan.planDate == date) { "generated plan date mismatch" }
        require(draft.tasks.all { it.planId == draft.plan.id }) { "task planId mismatch" }
        require(draft.tasks.map { it.position }.distinct().size == draft.tasks.size) {
            "task positions must be unique"
        }
        val inserted = database.dailyPlanDao().insertIfAbsent(draft.plan)
        if (inserted == -1L) {
            return@withTransaction requireNotNull(load(date))
        }
        database.dailyTaskDao().insertAll(draft.tasks)
        requireNotNull(load(date))
    }

    /**
     * Fill an empty placeholder plan after an earlier source snapshot had no materialized
     * cards. This is deliberately narrower than rebuild: it refuses to touch any plan that
     * already has tasks, preserving the same-day immutability contract.
     */
    suspend fun fillEmpty(
        date: String,
        draft: DailyPlanDraft,
    ): DailyPlanWithTasks = database.withTransaction {
        val existing = requireNotNull(load(date))
        if (existing.plan.status != "EMPTY" || existing.tasks.isNotEmpty() || draft.tasks.isEmpty()) {
            return@withTransaction existing
        }
        require(draft.plan.planDate == date) { "generated plan date mismatch" }
        require(draft.tasks.all { it.planId == draft.plan.id }) { "task planId mismatch" }
        require(draft.tasks.map { it.position }.distinct().size == draft.tasks.size) {
            "task positions must be unique"
        }
        val plan = draft.plan.copy(
            id = existing.plan.id,
            planDate = existing.plan.planDate,
            createdAt = existing.plan.createdAt,
        )
        database.dailyTaskDao().insertAll(draft.tasks.map { it.copy(planId = existing.plan.id) })
        database.dailyPlanDao().update(plan)
        requireNotNull(load(date))
    }

    /**
     * Mark one persisted task complete without reviving or mutating legacy states.
     *
     * The read and conditional write are one transaction so duplicate completion callbacks
     * (for example, the normal and fullscreen card destinations both observing the same
     * session) remain harmless.
     */
    suspend fun markDone(
        taskId: String,
        now: Long = System.currentTimeMillis(),
    ): Boolean = database.withTransaction {
        val taskDao = database.dailyTaskDao()
        val task = taskDao.getById(taskId) ?: return@withTransaction false
        when (task.status) {
            "DONE" -> true
            "PENDING" -> taskDao.updateStatus(taskId, "DONE", now) == 1
            else -> false
        }
    }

    private suspend fun load(date: String): DailyPlanWithTasks? {
        val plan = database.dailyPlanDao().getEntityByDate(date) ?: return null
        return DailyPlanWithTasks(plan, database.dailyTaskDao().getByPlan(plan.id))
    }

    suspend fun legacyBefore(date: String): List<DailyTaskEntity> =
        database.dailyTaskDao().getLegacyBefore(date)

    /** Explicit and idempotent legacy resolution; target plan must already exist. */
    suspend fun resolveLegacy(
        sourceTaskId: String,
        targetDate: String,
        decision: LegacyDecision,
        now: Long,
    ): DailyTaskEntity? = database.withTransaction {
        val taskDao = database.dailyTaskDao()
        val source = requireNotNull(taskDao.getById(sourceTaskId))
        if (source.status != "PENDING") return@withTransaction taskDao.getCarriedFrom(sourceTaskId)
        val sourcePlan = requireNotNull(database.dailyPlanDao().getEntityById(source.planId))
        val target = requireNotNull(database.dailyPlanDao().getEntityByDate(targetDate))
        require(targetDate > sourcePlan.planDate) { "legacy target must be a later date" }
        if (decision == LegacyDecision.SKIP) {
            taskDao.update(source.copy(status = "SKIPPED", updatedAt = now))
            return@withTransaction null
        }
        taskDao.getCarriedFrom(sourceTaskId)?.let { return@withTransaction it }
        val nextPosition = (taskDao.getByPlan(target.id).maxOfOrNull { it.position } ?: -1) + 1
        val prefix = if (decision == LegacyDecision.CARRY) "carry" else "special"
        val created = source.copy(
            id = "${target.id}:$prefix:${source.id}",
            planId = target.id,
            stableId = "$prefix:${source.stableId}:${source.id}",
            position = nextPosition,
            taskType = if (decision == LegacyDecision.CARRY) source.taskType else "SPECIAL_SESSION",
            status = "PENDING",
            carriedFromTaskId = source.id,
            createdAt = now,
            updatedAt = now,
        )
        taskDao.insert(created)
        taskDao.update(source.copy(status = if (decision == LegacyDecision.CARRY) "CARRIED" else "SPECIAL_SESSION", updatedAt = now))
        created
    }

    /** Explicit same-day rebuild. DONE tasks are immutable and never revived. */
    suspend fun rebuildUnfinished(
        date: String,
        replacements: List<DailyTaskEntity>,
        now: Long,
    ): DailyPlanWithTasks = database.withTransaction {
        val plan = requireNotNull(database.dailyPlanDao().getEntityByDate(date))
        require(replacements.all { it.planId == plan.id }) { "replacement planId mismatch" }
        val taskDao = database.dailyTaskDao()
        val existing = taskDao.getByPlan(plan.id)
        val doneStableIds = existing.filter { it.status == "DONE" }.mapTo(mutableSetOf()) { it.stableId }
        require(replacements.none { it.stableId in doneStableIds }) { "completed task cannot be rebuilt" }
        val shiftBase = Math.addExact(
            maxOf(existing.maxOfOrNull { it.position } ?: -1, replacements.maxOfOrNull { it.position } ?: -1),
            1,
        )
        existing.filter { it.status != "DONE" }.forEachIndexed { index, task ->
            taskDao.update(
                task.copy(position = Math.addExact(shiftBase, index), status = "SUPERSEDED", updatedAt = now),
            )
        }
        val byStableId = existing.associateBy { it.stableId }
        replacements.forEach { replacement ->
            val old = byStableId[replacement.stableId]
            if (old == null) {
                taskDao.insert(replacement.copy(status = "PENDING", updatedAt = now))
            } else {
                taskDao.update(replacement.copy(id = old.id, createdAt = old.createdAt, status = "PENDING", updatedAt = now))
            }
        }
        requireNotNull(load(date))
    }
}
