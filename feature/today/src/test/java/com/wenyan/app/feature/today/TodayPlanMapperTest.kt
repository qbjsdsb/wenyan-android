package com.wenyan.app.feature.today

import com.wenyan.app.core.database.entity.DailyPlanEntity
import com.wenyan.app.core.database.entity.DailyPlanWithTasks
import com.wenyan.app.core.database.entity.DailyTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TodayPlanMapperTest {
    @Test fun `partial plan maps groups countdown duration and old destinations`() {
        val state = TodayPlanMapper.map(
            plan(
                status = "ACTIVE", snapshot = "{\"examDate\":\"2026-12-20\"}",
                tasks = listOf(task("due", "DUE", "PENDING", 5), task("output", "OUTPUT", "DONE", 15), task("write", "WRITING", "PENDING", 20)),
            ),
            LocalDate.of(2026, 8, 11),
        )
        assertEquals(131L, state.countdownDays)
        assertEquals(25, state.estimatedMinutes)
        assertEquals(listOf(TodayGroup.DUE, TodayGroup.OUTPUT, TodayGroup.WRITING), state.tasks.map { it.group })
        assertEquals(listOf(TodayDestination.CARDS, TodayDestination.QUIZ, TodayDestination.WRITING_MATERIALS), state.tasks.map { it.destination })
        assertFalse(state.isFinished)
    }

    @Test fun `finished plan is honest and excludes superseded history`() {
        val state = TodayPlanMapper.map(plan(tasks = listOf(task("done", "DUE", "DONE", 5), task("old", "NEW", "SUPERSEDED", 9))), LocalDate.of(2026, 8, 11))
        assertTrue(state.isFinished)
        assertEquals(0, state.estimatedMinutes)
        assertEquals(listOf("done"), state.tasks.map { it.id })
    }

    @Test fun `empty infeasible and missing exam date do not invent values`() {
        val state = TodayPlanMapper.map(plan(status = "INFEASIBLE:没有可信的新内容"), LocalDate.of(2026, 8, 11))
        assertTrue(state.tasks.isEmpty())
        assertEquals("没有可信的新内容", state.infeasibleMessage)
        assertNull(state.countdownDays)
    }

    @Test
    fun `unknown task type stays visible but is never routed to writing`() {
        val state = TodayPlanMapper.map(
            plan(tasks = listOf(task("future", "FUTURE_TASK", "PENDING", 10))),
            LocalDate.of(2026, 8, 11),
        )

        assertEquals(TodayGroup.OTHER, state.tasks.single().group)
        assertEquals(TodayDestination.UNSUPPORTED, state.tasks.single().destination)
        assertEquals("future", state.tasks.single().title)
    }

    private fun plan(status: String = "ACTIVE", snapshot: String = "{}", tasks: List<DailyTaskEntity> = emptyList()) = DailyPlanWithTasks(
        DailyPlanEntity("plan", "2026-08-11", 1, 2027, snapshot, "2.26.0", status), tasks,
    )

    private fun task(id: String, type: String, status: String, minutes: Int) = DailyTaskEntity(
        id, "plan", id, 0, type, id, null, minutes, status, null, 1, 1,
    )
}
