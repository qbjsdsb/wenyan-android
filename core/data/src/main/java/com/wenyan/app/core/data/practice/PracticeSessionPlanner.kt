package com.wenyan.app.core.data.practice

import com.wenyan.app.core.database.entity.PracticeErrorReason
import com.wenyan.app.core.database.entity.PracticeAttemptEntity

data class PracticeSessionCandidate(
    val questionId: String,
    val year: Int,
    val paperCode: String,
    val questionType: String,
    val subjectId: String,
    val examFrequencyRank: Int,
    val recentWeakness: Int,
    val errorReasons: Set<PracticeErrorReason> = emptySet(),
)

data class PracticeSessionFilter(
    val years: Set<Int> = emptySet(),
    val paperCodes: Set<String> = emptySet(),
    val questionTypes: Set<String> = emptySet(),
    val subjectIds: Set<String> = emptySet(),
    val maxExamFrequencyRank: Int? = null,
    val minimumWeakness: Int? = null,
    val errorReasons: Set<PracticeErrorReason> = emptySet(),
)

data class PracticeSessionPlan(
    val sessionId: String,
    val questionIds: List<String>,
    val emptyReason: String?,
)

object PracticeSessionPlanner {
    private val historicalCodes = setOf("610", "801", "805", "806", "807")

    fun plan(sessionId: String, candidates: List<PracticeSessionCandidate>, filter: PracticeSessionFilter): PracticeSessionPlan {
        require(sessionId.isNotBlank())
        require(filter.paperCodes.all { it in historicalCodes }) { "unknown paper code filter" }
        val selected = candidates.asSequence()
            .filter { filter.years.isEmpty() || it.year in filter.years }
            .filter { filter.paperCodes.isEmpty() || it.paperCode in filter.paperCodes }
            .filter { filter.questionTypes.isEmpty() || it.questionType in filter.questionTypes }
            .filter { filter.subjectIds.isEmpty() || it.subjectId in filter.subjectIds }
            .filter { filter.maxExamFrequencyRank == null || it.examFrequencyRank <= filter.maxExamFrequencyRank }
            .filter { filter.minimumWeakness == null || it.recentWeakness >= filter.minimumWeakness }
            .filter { filter.errorReasons.isEmpty() || it.errorReasons.any(filter.errorReasons::contains) }
            .distinctBy { it.questionId }
            .sortedWith(
                compareByDescending<PracticeSessionCandidate> { it.recentWeakness }
                    .thenBy { it.examFrequencyRank }
                    .thenByDescending { it.year }
                    .thenBy { historicalCodes.indexOf(it.paperCode) }
                    .thenBy { it.questionType }
                    .thenBy { it.subjectId }
                    .thenBy { it.questionId },
            )
            .map { it.questionId }
            .toList()
        return PracticeSessionPlan(
            sessionId,
            selected,
            if (selected.isEmpty()) "当前筛选没有匹配题目；请放宽年份、科目、题型、考频、薄弱点或错因。" else null,
        )
    }
}

data class PracticeSessionSummary(
    val completed: Int,
    val missingKeywords: Int,
    val missingOutlines: Int,
    val missingBodies: Int,
    val errorCounts: Map<PracticeErrorReason, Int>,
    val suggestedRepairAttemptIds: List<String>,
)

fun summarizePracticeSession(attempts: List<PracticeAttemptEntity>): PracticeSessionSummary {
    val ordered = attempts.distinctBy { it.id }.sortedWith(compareBy<PracticeAttemptEntity> { it.createdAt }.thenBy { it.id })
    val errors = PracticeErrorReason.entries.associateWith { reason ->
        ordered.count { reason.name in it.errorReasons }
    }.filterValues { it > 0 }
    return PracticeSessionSummary(
        completed = ordered.count { it.completedAt != null },
        missingKeywords = ordered.count { it.userKeywords.isBlank() },
        missingOutlines = ordered.count { it.outline.isBlank() },
        missingBodies = ordered.count { it.body.isBlank() },
        errorCounts = errors,
        suggestedRepairAttemptIds = ordered.filter {
            it.repairState == "CANDIDATE" || it.errorReasons.isNotEmpty()
        }.map { it.id },
    )
}
