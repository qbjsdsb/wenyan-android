package com.wenyan.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.DailyPlanEntity
import com.wenyan.app.core.database.entity.DailyTaskEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyPlanRepositoryTest {
    private lateinit var db: WenyanDatabase
    private lateinit var repository: DailyPlanRepository

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WenyanDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = DailyPlanRepository(db)
    }

    @After fun tearDown() = db.close()

    @Test fun `same day concurrent callers persist exactly one ordered plan`() = runTest {
        var generations = 0
        val results = List(8) { index ->
            async {
                repository.getOrCreate(DATE) {
                    generations++
                    draft("plan-$index", listOf(2, 0, 1))
                }
            }
        }.awaitAll()

        assertEquals(1, generations)
        assertEquals(1, results.map { it.plan.id }.distinct().size)
        assertEquals(listOf(0, 1, 2), results.first().tasks.map { it.position })
        assertEquals(1, count("daily_plans"))
        assertEquals(3, count("daily_tasks"))
    }

    @Test fun `existing plan is returned without regeneration and statuses survive`() = runTest {
        val first = repository.getOrCreate(DATE) { draft("plan", listOf(0, 1)) }
        db.dailyTaskDao().updateStatus(first.tasks.first().id, "DONE", 99)
        var regenerated = false

        val restored = repository.getOrCreate(DATE) {
            regenerated = true
            draft("replacement", listOf(0))
        }

        assertEquals(false, regenerated)
        assertEquals(listOf("DONE", "PENDING"), restored.tasks.map { it.status })
    }

    @Test fun `markDone is idempotent and does not revive non-pending tasks`() = runTest {
        val plan = repository.getOrCreate(DATE) { draft("plan", listOf(0, 1, 2)) }

        assertEquals(true, repository.markDone(plan.tasks[0].id, now = 20))
        assertEquals(true, repository.markDone(plan.tasks[0].id, now = 21))
        db.dailyTaskDao().updateStatus(plan.tasks[1].id, "SUPERSEDED", 21)
        assertEquals(false, repository.markDone(plan.tasks[1].id, now = 22))
        assertEquals(false, repository.markDone("missing", now = 23))
        assertEquals(listOf("DONE", "SUPERSEDED", "PENDING"), db.dailyTaskDao().getByPlan(plan.plan.id).map { it.status })
        assertEquals(20L, db.dailyTaskDao().getById(plan.tasks[0].id)?.updatedAt)
    }

    @Test fun `empty placeholder can be filled without changing its plan identity`() = runTest {
        val emptyDraft = draft("empty", emptyList())
        val empty = repository.getOrCreate(DATE) {
            emptyDraft.copy(plan = emptyDraft.plan.copy(status = "EMPTY"))
        }
        val replacement = draft("replacement", listOf(0))

        val filled = repository.fillEmpty(DATE, replacement)

        assertEquals(empty.plan.id, filled.plan.id)
        assertEquals(empty.plan.createdAt, filled.plan.createdAt)
        assertEquals("ACTIVE", filled.plan.status)
        assertEquals(listOf("PENDING"), filled.tasks.map { it.status })
        assertEquals(empty.plan.id, filled.tasks.single().planId)
    }

    @Test fun `task insert failure rolls back new plan`() = runTest {
        val bad = draft("bad", listOf(0, 1)).let { draft ->
            draft.copy(tasks = draft.tasks.map { it.copy(stableId = "duplicate") })
        }
        runCatching { repository.getOrCreate(DATE) { bad } }

        assertNull(db.dailyPlanDao().getEntityByDate(DATE))
        assertEquals(0, count("daily_tasks"))
    }

    @Test fun `mismatched task ownership fails before writing`() = runTest {
        val bad = draft("bad", listOf(0)).let { draft ->
            draft.copy(tasks = draft.tasks.map { it.copy(planId = "other") })
        }
        val failure = runCatching { repository.getOrCreate(DATE) { bad } }.exceptionOrNull()
        assertNotNull(failure)
        assertEquals(0, count("daily_plans"))
    }

    @Test fun `same date ignores changed settings while next date gets a new snapshot`() = runTest {
        repository.getOrCreate(DATE) { draft("today", listOf(0)) }
        val same = repository.getOrCreate(DATE) {
            draft("changed", listOf(0)).let { it.copy(plan = it.plan.copy(settingsSnapshot = "changed")) }
        }
        val tomorrow = repository.getOrCreate("2026-08-10") {
            draft("tomorrow", listOf(0), "2026-08-10").let {
                it.copy(plan = it.plan.copy(settingsSnapshot = "changed"))
            }
        }
        assertEquals("{}", same.plan.settingsSnapshot)
        assertEquals("changed", tomorrow.plan.settingsSnapshot)
    }

    @Test fun `carry skip and special decisions are explicit and idempotent`() = runTest {
        val yesterday = repository.getOrCreate("2026-08-08") { draft("yesterday", listOf(0, 1, 2), "2026-08-08") }
        repository.getOrCreate(DATE) { draft("today", emptyList()) }

        val carried = repository.resolveLegacy(yesterday.tasks[0].id, DATE, LegacyDecision.CARRY, 10)
        val carriedAgain = repository.resolveLegacy(yesterday.tasks[0].id, DATE, LegacyDecision.CARRY, 11)
        repository.resolveLegacy(yesterday.tasks[1].id, DATE, LegacyDecision.SKIP, 10)
        repository.resolveLegacy(yesterday.tasks[1].id, DATE, LegacyDecision.SKIP, 11)
        val special = repository.resolveLegacy(yesterday.tasks[2].id, DATE, LegacyDecision.SPECIAL_SESSION, 10)

        assertEquals(carried?.id, carriedAgain?.id)
        assertEquals("SPECIAL_SESSION", special?.taskType)
        assertEquals(2, db.dailyTaskDao().getByPlan("today").size)
        assertEquals(emptyList<DailyTaskEntity>(), repository.legacyBefore(DATE))
    }

    @Test fun `rebuild preserves completed tasks and supersedes missing unfinished tasks`() = runTest {
        val original = repository.getOrCreate(DATE) { draft("plan", listOf(0, 1, 2)) }
        db.dailyTaskDao().updateStatus(original.tasks[0].id, "DONE", 5)
        val replacement = original.tasks[1].copy(position = 3, estimatedMinutes = 9)

        val rebuilt = repository.rebuildUnfinished(DATE, listOf(replacement), 10)

        assertEquals("DONE", rebuilt.tasks.single { it.id == original.tasks[0].id }.status)
        assertEquals("PENDING", rebuilt.tasks.single { it.stableId == replacement.stableId }.status)
        assertEquals("SUPERSEDED", rebuilt.tasks.single { it.stableId == original.tasks[2].stableId }.status)
    }

    @Test fun `completed task cannot be revived and failed rebuild rolls back`() = runTest {
        val original = repository.getOrCreate(DATE) { draft("plan", listOf(0, 1)) }
        db.dailyTaskDao().updateStatus(original.tasks[0].id, "DONE", 5)
        val before = db.dailyTaskDao().getByPlan("plan")

        runCatching { repository.rebuildUnfinished(DATE, listOf(original.tasks[0]), 10) }

        assertEquals(before, db.dailyTaskDao().getByPlan("plan"))
    }

    private fun draft(id: String, positions: List<Int>, date: String = DATE): DailyPlanDraft = DailyPlanDraft(
        DailyPlanEntity(id, date, 1, 2027, "{}", "2.26.0", "ACTIVE"),
        positions.map { position ->
            DailyTaskEntity(
                id = "$id-task-$position", planId = id, stableId = "unit-$position",
                position = position, taskType = "DUE", contentId = "point-$position",
                learningUnitId = null, estimatedMinutes = 5, status = "PENDING",
                carriedFromTaskId = null, createdAt = 1, updatedAt = 1,
            )
        },
    )

    private fun count(table: String): Int = db.openHelper.readableDatabase
        .query("SELECT COUNT(*) FROM $table").use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }

    private companion object { const val DATE = "2026-08-09" }
}
