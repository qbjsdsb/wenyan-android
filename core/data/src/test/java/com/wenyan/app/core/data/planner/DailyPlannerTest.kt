package com.wenyan.app.core.data.planner

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPlannerTest {
    private val zone = ZoneId.of("Asia/Taipei")
    private val clock = Clock.fixed(Instant.parse("2026-08-09T04:00:00Z"), zone)
    private val planner = DailyPlanner(clock, zone)

    @Test fun `same input produces same plan`() {
        val input = input(listOf(candidate("b", DailyBucket.NEW), candidate("a", DailyBucket.NEW)))
        assertEquals(planner.plan(input), planner.plan(input))
    }

    @Test fun `due is always ahead of repair new output and writing`() {
        val result = planner.plan(input(listOf(
            candidate("write", DailyBucket.WRITING), candidate("out", DailyBucket.OUTPUT),
            candidate("new", DailyBucket.NEW), candidate("repair", DailyBucket.REPAIR),
            candidate("due", DailyBucket.DUE),
        )))
        assertEquals(listOf("due", "repair", "new", "out", "write"), result.tasks.map { it.stableId })
    }

    @Test fun `subject rotation breaks otherwise equal candidates`() {
        val result = planner.plan(input(listOf(
            candidate("ancient", DailyBucket.NEW, subject = "ancient"),
            candidate("modern", DailyBucket.NEW, subject = "modern"),
        ), subjects = listOf("modern", "ancient")))
        assertEquals(listOf("modern", "ancient"), result.tasks.map { it.stableId })
    }

    @Test fun `untrusted new content is excluded and explained`() {
        val result = planner.plan(input(listOf(candidate("draft", DailyBucket.NEW, trusted = false))))
        assertTrue(result.tasks.isEmpty())
        assertTrue(PlanIssue.NO_TRUSTED_NEW_CONTENT in result.issues)
    }

    @Test fun `zero quota is finite and explained`() {
        val result = planner.plan(input(List(100) { candidate("d$it", DailyBucket.DUE) }, quota = 0))
        assertTrue(result.tasks.isEmpty())
        assertEquals(listOf(PlanIssue.ZERO_QUOTA), result.issues)
    }

    @Test fun `exam passed is explained`() {
        val result = planner.plan(input(emptyList(), examDate = java.time.LocalDate.of(2026, 8, 8)))
        assertEquals(listOf(PlanIssue.EXAM_PASSED), result.issues)
    }

    @Test fun `large overdue backlog stays capped and explained`() {
        val result = planner.plan(input(List(100) { candidate("d$it", DailyBucket.DUE, overdue = it) }, quota = 10))
        assertEquals(10, result.tasks.size)
        assertTrue(PlanIssue.OVERDUE_BACKLOG in result.issues)
    }

    @Test fun `stable id is final tie breaker`() {
        val result = planner.plan(input(listOf(candidate("z", DailyBucket.DUE), candidate("a", DailyBucket.DUE))))
        assertEquals(listOf("a", "z"), result.tasks.map { it.stableId })
    }

    @Test fun `priority comparator follows the documented key chain`() {
        val result = planner.plan(input(listOf(
            DailyCandidate("less-overdue", DailyBucket.DUE, "modern", 2, .1, 0, 9),
            DailyCandidate("weak-recall", DailyBucket.DUE, "modern", 3, .2, 0, 0),
            DailyCandidate("strong-recall", DailyBucket.DUE, "modern", 3, .8, 0, 9),
            DailyCandidate("frequent", DailyBucket.DUE, "modern", 3, .2, 1, 9),
            DailyCandidate("weakness", DailyBucket.DUE, "modern", 3, .2, 2, 8),
            DailyCandidate("not-weak", DailyBucket.DUE, "modern", 3, .2, 2, 1),
        )))
        assertEquals(
            listOf("weak-recall", "frequent", "weakness", "not-weak", "strong-recall", "less-overdue"),
            result.tasks.map { it.stableId },
        )
    }

    @Test fun `missing output or scheduled writing is reported rather than exceeding quota`() {
        val result = planner.plan(input(listOf(candidate("due", DailyBucket.DUE)), quota = 1, writing = true))
        assertEquals(listOf("due"), result.tasks.map { it.stableId })
        assertTrue(PlanIssue.OUTPUT_UNAVAILABLE in result.issues)
        assertTrue(PlanIssue.WRITING_UNAVAILABLE in result.issues)
    }

    @Test fun `Taipei midnight is the only day boundary`() {
        val before = DailyPlanner(Clock.fixed(Instant.parse("2026-08-09T15:59:00Z"), zone), zone)
            .plan(input(emptyList())).date
        val after = DailyPlanner(Clock.fixed(Instant.parse("2026-08-09T16:01:00Z"), zone), zone)
            .plan(input(emptyList())).date
        assertEquals(java.time.LocalDate.of(2026, 8, 9), before)
        assertEquals(java.time.LocalDate.of(2026, 8, 10), after)
    }

    private fun input(
        candidates: List<DailyCandidate>, quota: Int = 20,
        subjects: List<String> = emptyList(), examDate: java.time.LocalDate? = null,
        writing: Boolean = false,
    ) = DailyPlannerInput(candidates, DailyPlannerSettings(quota, subjects, writing), examDate)

    private fun candidate(
        id: String, bucket: DailyBucket, subject: String = "modern", trusted: Boolean = true,
        overdue: Int = 0,
    ) = DailyCandidate(id, bucket, subject, overdue, .5, 1, 0, trusted)
}
