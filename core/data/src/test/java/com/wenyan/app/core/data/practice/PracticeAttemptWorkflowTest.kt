package com.wenyan.app.core.data.practice

import com.wenyan.app.core.database.entity.ContentReviewStatus
import com.wenyan.app.core.database.entity.PracticeErrorReason
import com.wenyan.app.core.database.entity.PracticeRepairState
import com.wenyan.app.core.database.entity.PracticeSelfRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeAttemptWorkflowTest {
    @Test fun blankCannotBeSavedOrRevealed() {
        val state = PracticeAttemptWorkflowState()
        assertTrue(PracticeAttemptWorkflow.save(state) is PracticeTransition.Rejected)
        assertTrue(PracticeAttemptWorkflow.reveal(state, ContentReviewStatus.REVIEWED.name, true) is PracticeTransition.Rejected)
    }

    @Test fun unreviewedFrameworkNeverReveals() {
        val state = PracticeAttemptWorkflowState(draft = PracticeDraft(outline = "我的提纲"))
        assertTrue(
            PracticeAttemptWorkflow.reveal(state, ContentReviewStatus.LEGACY_UNVERIFIED.name, true) is PracticeTransition.Rejected,
        )
    }

    @Test fun orderedWorkflowCreatesRepairCandidateAndIsIdempotent() {
        var state = PracticeAttemptWorkflowState(draft = PracticeDraft(keywords = "关键词"))
        state = (PracticeAttemptWorkflow.save(state) as PracticeTransition.Accepted).state
        state = (PracticeAttemptWorkflow.save(state) as PracticeTransition.Accepted).state
        assertEquals(PracticeAttemptStage.SAVED, state.stage)
        state = (PracticeAttemptWorkflow.reveal(state, ContentReviewStatus.REVIEWED.name, true) as PracticeTransition.Accepted).state
        state = (PracticeAttemptWorkflow.assess(
            state,
            PracticeSelfRating.HARD,
            setOf(PracticeErrorReason.WEAK_STRUCTURE),
        ) as PracticeTransition.Accepted).state
        assertEquals(PracticeRepairState.CANDIDATE, state.repairState)
        state = (PracticeAttemptWorkflow.complete(state) as PracticeTransition.Accepted).state
        assertEquals(PracticeAttemptStage.COMPLETED, state.stage)
    }

    @Test fun cannotAssessBeforeRevealOrCompleteBeforeAssess() {
        val state = PracticeAttemptWorkflowState(draft = PracticeDraft(body = "正文"))
        assertTrue(PracticeAttemptWorkflow.assess(state, PracticeSelfRating.GOOD, emptySet()) is PracticeTransition.Rejected)
        assertTrue(PracticeAttemptWorkflow.complete(state) is PracticeTransition.Rejected)
    }
}
