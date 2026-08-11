package com.wenyan.app.core.data.planner

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Priority buckets are deliberately ordered; persisted task types must not rely on ordinals. */
enum class DailyBucket { DUE, REPAIR, NEW, OUTPUT, WRITING }

data class DailyCandidate(
    val stableId: String,
    val bucket: DailyBucket,
    val subject: String,
    val overdueDays: Int = 0,
    val retrievability: Double = 1.0,
    val examFrequencyRank: Int = Int.MAX_VALUE,
    val recentWeakness: Int = 0,
    val trusted: Boolean = true,
)

data class DailyPlannerSettings(
    val taskQuota: Int,
    val subjectRotation: List<String> = emptyList(),
    /** Whether today's explicit study schedule calls for a 610 writing task. */
    val writingScheduled: Boolean = false,
)

data class DailyPlannerInput(
    val candidates: List<DailyCandidate>,
    val settings: DailyPlannerSettings,
    val examDate: LocalDate? = null,
)

enum class PlanIssue {
    ZERO_QUOTA,
    EXAM_PASSED,
    NO_TRUSTED_NEW_CONTENT,
    OVERDUE_BACKLOG,
    OUTPUT_UNAVAILABLE,
    WRITING_UNAVAILABLE,
}

data class DailyPlannerResult(
    val date: LocalDate,
    val tasks: List<DailyCandidate>,
    val issues: List<PlanIssue>,
)

/**
 * Deterministic, side-effect-free daily selection. It does not access Room or UI and never
 * expands [DailyPlannerSettings.taskQuota] to satisfy a lower-priority requirement.
 */
class DailyPlanner(
    private val clock: Clock,
    private val zoneId: ZoneId = ZoneId.of("Asia/Taipei"),
) {
    fun plan(input: DailyPlannerInput): DailyPlannerResult {
        val today = LocalDate.now(clock.withZone(zoneId))
        if (input.settings.taskQuota <= 0) {
            return DailyPlannerResult(today, emptyList(), listOf(PlanIssue.ZERO_QUOTA))
        }
        if (input.examDate?.isBefore(today) == true) {
            return DailyPlannerResult(today, emptyList(), listOf(PlanIssue.EXAM_PASSED))
        }

        val issues = linkedSetOf<PlanIssue>()
        val hadUntrustedNew = input.candidates.any { it.bucket == DailyBucket.NEW && !it.trusted }
        val eligible = input.candidates.filterNot { it.bucket == DailyBucket.NEW && !it.trusted }
        if (hadUntrustedNew && eligible.none { it.bucket == DailyBucket.NEW }) {
            issues += PlanIssue.NO_TRUSTED_NEW_CONTENT
        }

        val subjectRanks = input.settings.subjectRotation.withIndex().associate { it.value to it.index }
        val ordered = eligible.sortedWith(
            compareBy<DailyCandidate> { it.bucket.rank }
                .thenByDescending { it.overdueDays }
                .thenBy { it.retrievability }
                .thenBy { it.examFrequencyRank }
                .thenByDescending { it.recentWeakness }
                .thenBy { subjectRanks[it.subject] ?: Int.MAX_VALUE }
                .thenBy { it.stableId },
        )
        val selected = ordered.take(input.settings.taskQuota)

        if (ordered.count { it.bucket == DailyBucket.DUE } > selected.count { it.bucket == DailyBucket.DUE }) {
            issues += PlanIssue.OVERDUE_BACKLOG
        }
        if (selected.none { it.bucket == DailyBucket.OUTPUT }) issues += PlanIssue.OUTPUT_UNAVAILABLE
        if (input.settings.writingScheduled && selected.none { it.bucket == DailyBucket.WRITING }) {
            issues += PlanIssue.WRITING_UNAVAILABLE
        }
        return DailyPlannerResult(today, selected, issues.toList())
    }

    private val DailyBucket.rank: Int
        get() = when (this) {
            DailyBucket.DUE -> 0
            DailyBucket.REPAIR -> 1
            DailyBucket.NEW -> 2
            DailyBucket.OUTPUT -> 3
            DailyBucket.WRITING -> 4
        }
}
