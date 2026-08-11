package com.wenyan.app.feature.today

import com.wenyan.app.core.data.planner.DailyBucket
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayPlanFactoryTest {
    private val date = LocalDate.of(2026, 8, 11)

    @Test fun `draft is deterministic and preserves exact point ids in task navigation fields`() {
        val candidates = listOf(
            TodayPlanCandidate("new-point", DailyBucket.NEW),
            TodayPlanCandidate("repair-point", DailyBucket.REPAIR, recentWeakness = 3),
            TodayPlanCandidate("due-point", DailyBucket.DUE),
        )

        val first = buildTodayPlanDraft(date, 99, candidates, null, taskQuota = 10)
        val second = buildTodayPlanDraft(date, 99, candidates, null, taskQuota = 10)

        assertEquals(first, second)
        assertEquals(listOf("DUE", "REPAIR", "NEW"), first.tasks.map { it.taskType })
        assertEquals(listOf("due-point", "repair-point", "new-point"), first.tasks.map { it.contentId })
        assertEquals(listOf("card:due-point", "card:repair-point", "card:new-point"), first.tasks.map { it.stableId })
        assertEquals(listOf(0, 1, 2), first.tasks.map { it.position })
        assertTrue(first.tasks.all { it.status == "PENDING" })
    }

    @Test fun `passed exam produces an explicit infeasible plan without tasks`() {
        val draft = buildTodayPlanDraft(
            date = date,
            nowMillis = 99,
            candidates = listOf(TodayPlanCandidate("due-point", DailyBucket.DUE)),
            examDate = date.minusDays(1),
            taskQuota = 10,
        )

        assertTrue(draft.tasks.isEmpty())
        assertEquals("INFEASIBLE:EXAM_PASSED", draft.plan.status)
    }
}
