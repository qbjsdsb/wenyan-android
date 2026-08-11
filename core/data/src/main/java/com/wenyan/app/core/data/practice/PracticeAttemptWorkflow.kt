package com.wenyan.app.core.data.practice

import com.wenyan.app.core.database.entity.ContentReviewStatus
import com.wenyan.app.core.database.entity.PracticeErrorReason
import com.wenyan.app.core.database.entity.PracticeRepairState
import com.wenyan.app.core.database.entity.PracticeSelfRating

enum class PracticeAttemptStage { ANSWERING, SAVED, REVEALED, ASSESSED, COMPLETED }

data class PracticeDraft(
    val keywords: String = "",
    val outline: String = "",
    val body: String = "",
) {
    val hasAnswer: Boolean get() = keywords.isNotBlank() || outline.isNotBlank() || body.isNotBlank()
}

data class PracticeAttemptWorkflowState(
    val stage: PracticeAttemptStage = PracticeAttemptStage.ANSWERING,
    val draft: PracticeDraft = PracticeDraft(),
    val rating: PracticeSelfRating? = null,
    val errors: Set<PracticeErrorReason> = emptySet(),
    val repairState: PracticeRepairState = PracticeRepairState.NONE,
)

sealed interface PracticeTransition {
    data class Accepted(val state: PracticeAttemptWorkflowState) : PracticeTransition
    data class Rejected(val reason: String) : PracticeTransition
}

object PracticeAttemptWorkflow {
    fun save(state: PracticeAttemptWorkflowState): PracticeTransition =
        if (!state.draft.hasAnswer) PracticeTransition.Rejected("请先写下关键词、提纲或正文")
        else PracticeTransition.Accepted(state.copy(stage = maxOf(state.stage, PracticeAttemptStage.SAVED)))

    fun reveal(state: PracticeAttemptWorkflowState, contentStatus: String, hasFramework: Boolean): PracticeTransition {
        if (!state.draft.hasAnswer) return PracticeTransition.Rejected("空白作答不能揭示后标记掌握")
        if (contentStatus != ContentReviewStatus.REVIEWED.name || !hasFramework) {
            return PracticeTransition.Rejected("参考框架尚未人工审校；作答已保留，暂不展示答案")
        }
        return PracticeTransition.Accepted(state.copy(stage = maxOf(state.stage, PracticeAttemptStage.REVEALED)))
    }

    fun assess(
        state: PracticeAttemptWorkflowState,
        rating: PracticeSelfRating,
        errors: Set<PracticeErrorReason>,
    ): PracticeTransition {
        if (state.stage < PracticeAttemptStage.REVEALED) return PracticeTransition.Rejected("请先主动揭示并核对")
        val repair = if (rating == PracticeSelfRating.AGAIN || rating == PracticeSelfRating.HARD || errors.isNotEmpty()) {
            PracticeRepairState.CANDIDATE
        } else PracticeRepairState.NONE
        return PracticeTransition.Accepted(
            state.copy(stage = PracticeAttemptStage.ASSESSED, rating = rating, errors = errors, repairState = repair),
        )
    }

    fun complete(state: PracticeAttemptWorkflowState): PracticeTransition =
        if (state.stage < PracticeAttemptStage.ASSESSED) PracticeTransition.Rejected("完成前需要自评并记录错因")
        else PracticeTransition.Accepted(state.copy(stage = PracticeAttemptStage.COMPLETED))
}
