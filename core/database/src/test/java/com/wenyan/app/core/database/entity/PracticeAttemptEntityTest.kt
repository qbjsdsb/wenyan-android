package com.wenyan.app.core.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PracticeAttemptEntityTest {
    @Test
    fun unknownEnumsFailClosed() {
        assertNull(PracticeAttemptType.fromDb("FUTURE_TYPE"))
        assertNull(PracticeSelfRating.fromDb("MASTERED"))
        assertNull(PracticeErrorReason.fromDb("OTHER"))
        assertEquals(PracticeRepairState.NONE, PracticeRepairState.fromDb("FUTURE_STATE"))
    }

    @Test
    fun fixedErrorReasonsRemainStable() {
        assertEquals(
            listOf(
                "MEMORY_GAP", "CONCEPT_CONFUSION", "MISREAD_PROMPT", "WEAK_STRUCTURE",
                "WEAK_EVIDENCE", "TIME_CONTROL", "EXPRESSION",
            ),
            PracticeErrorReason.entries.map { it.name },
        )
    }
}
