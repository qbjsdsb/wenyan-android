package com.wenyan.app.core.data.practice

import com.wenyan.app.core.database.entity.PracticeErrorReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import com.wenyan.app.core.database.entity.PracticeAttemptEntity
import com.wenyan.app.core.data.repository.repairStableId
import com.wenyan.app.core.data.repository.requireRepairDate
import java.time.LocalDate

class PracticeSessionPlannerTest {
    @Test fun allSevenDimensionsFilterWithoutRewritingPaperCodes() {
        val candidates = listOf(
            candidate("q1", 2025, "805", "SHORT", "foreign", 1, 5, PracticeErrorReason.WEAK_EVIDENCE),
            candidate("q2", 2024, "610", "ESSAY", "theory", 2, 2, PracticeErrorReason.WEAK_STRUCTURE),
        )
        val plan = PracticeSessionPlanner.plan(
            "session-1", candidates,
            PracticeSessionFilter(
                years = setOf(2025), paperCodes = setOf("805"), questionTypes = setOf("SHORT"),
                subjectIds = setOf("foreign"), maxExamFrequencyRank = 1, minimumWeakness = 4,
                errorReasons = setOf(PracticeErrorReason.WEAK_EVIDENCE),
            ),
        )
        assertEquals(listOf("q1"), plan.questionIds)
    }

    @Test fun stableOrderAndDedupPreferWeaknessThenFrequency() {
        val input = listOf(
            candidate("b", 2024, "801", "SHORT", "s", 2, 5),
            candidate("a", 2025, "610", "TERM", "s", 1, 5),
            candidate("a", 2025, "610", "TERM", "s", 1, 5),
        )
        assertEquals(listOf("a", "b"), PracticeSessionPlanner.plan("s", input, PracticeSessionFilter()).questionIds)
    }

    @Test fun emptyResultExplainsHowToRecover() {
        val plan = PracticeSessionPlanner.plan("s", emptyList(), PracticeSessionFilter())
        assertEquals(emptyList<String>(), plan.questionIds)
        assertNotNull(plan.emptyReason)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInventedHistoricalPaperCode() {
        PracticeSessionPlanner.plan("s", emptyList(), PracticeSessionFilter(paperCodes = setOf("999")))
    }

    @Test fun summaryReportsOmissionsErrorsAndStableRepairSuggestions() {
        val attempt = PracticeAttemptEntity(
            id = "a", questionId = "q", pointId = null, learningUnitId = null, sessionId = "s",
            attemptType = "EXAM_OUTLINE", userKeywords = "", outline = "提纲", body = "",
            startedAt = 1, revealedAt = 2, completedAt = 3, elapsedMs = 2,
            selfRating = "HARD", errorReasons = listOf("WEAK_EVIDENCE"), repairState = "CANDIDATE",
            createdAt = 1, updatedAt = 3,
        )
        val summary = summarizePracticeSession(listOf(attempt, attempt))
        assertEquals(1, summary.completed)
        assertEquals(1, summary.missingKeywords)
        assertEquals(1, summary.missingBodies)
        assertEquals(mapOf(PracticeErrorReason.WEAK_EVIDENCE to 1), summary.errorCounts)
        assertEquals(listOf("a"), summary.suggestedRepairAttemptIds)
    }

    @Test fun repairIdentityIsStableAndOnlyLaterDatesAreAccepted() {
        assertEquals("repair:practice:a", repairStableId("a"))
        requireRepairDate(LocalDate.parse("2026-08-11"), LocalDate.parse("2026-08-12"))
        assertNotNull(
            runCatching {
                requireRepairDate(LocalDate.parse("2026-08-11"), LocalDate.parse("2026-08-11"))
            }.exceptionOrNull(),
        )
    }

    private fun candidate(
        id: String, year: Int, code: String, type: String, subject: String,
        frequency: Int, weakness: Int, error: PracticeErrorReason? = null,
    ) = PracticeSessionCandidate(id, year, code, type, subject, frequency, weakness, setOfNotNull(error))
}
