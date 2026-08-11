package com.wenyan.app.feature.today

import com.wenyan.app.core.data.planner.DailyBucket
import com.wenyan.app.core.data.planner.DailyCandidate
import com.wenyan.app.core.data.planner.DailyPlanner
import com.wenyan.app.core.data.planner.DailyPlannerInput
import com.wenyan.app.core.data.planner.DailyPlannerSettings
import com.wenyan.app.core.data.planner.PlanIssue
import com.wenyan.app.core.data.repository.CardSettingsRepository
import com.wenyan.app.core.data.repository.CardRepository
import com.wenyan.app.core.data.repository.DailyPlanDraft
import com.wenyan.app.core.data.repository.ReviewRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.entity.DailyPlanEntity
import com.wenyan.app.core.database.entity.DailyTaskEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

private val TODAY_ZONE: ZoneId = ZoneId.of("Asia/Taipei")

/** A source item with the stable content ID kept separate from the planner's stable ID. */
internal data class TodayPlanCandidate(
    val pointId: String,
    val bucket: DailyBucket,
    val examFrequencyRank: Int = Int.MAX_VALUE,
    val recentWeakness: Int = 0,
)

/**
 * Deterministic conversion from today's source snapshot to the persisted daily-plan schema.
 * The source lists are expected to be point-disjoint; [TodayPlanFactory] enforces that before
 * calling this function.
 */
internal fun buildTodayPlanDraft(
    date: LocalDate,
    nowMillis: Long,
    candidates: List<TodayPlanCandidate>,
    examDate: LocalDate?,
    taskQuota: Int,
): DailyPlanDraft {
    val plannerClock = Clock.fixed(date.atStartOfDay(TODAY_ZONE).toInstant(), TODAY_ZONE)
    val planner = DailyPlanner(plannerClock, TODAY_ZONE)
    val plannerCandidates = candidates.map { candidate ->
        DailyCandidate(
            stableId = candidate.stableId,
            bucket = candidate.bucket,
            subject = "",
            examFrequencyRank = candidate.examFrequencyRank,
            recentWeakness = candidate.recentWeakness,
        )
    }
    val result = planner.plan(
        DailyPlannerInput(
            candidates = plannerCandidates,
            settings = DailyPlannerSettings(taskQuota = taskQuota.coerceAtLeast(1)),
            examDate = examDate,
        ),
    )
    val candidateByStableId = candidates.associateBy { it.stableId }
    val planId = "daily:$date"
    val tasks = result.tasks.mapIndexed { position, selected ->
        val source = requireNotNull(candidateByStableId[selected.stableId])
        DailyTaskEntity(
            id = "$planId:task:${selected.stableId}",
            planId = planId,
            stableId = selected.stableId,
            position = position,
            taskType = selected.bucket.name,
            contentId = source.pointId,
            learningUnitId = null,
            estimatedMinutes = when (selected.bucket) {
                DailyBucket.DUE, DailyBucket.REPAIR -> 5
                DailyBucket.NEW -> 8
                DailyBucket.OUTPUT, DailyBucket.WRITING -> 15
            },
            status = "PENDING",
            carriedFromTaskId = null,
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )
    }
    val blockingIssues = result.issues.filterNot { issue ->
        issue == PlanIssue.OUTPUT_UNAVAILABLE || issue == PlanIssue.WRITING_UNAVAILABLE
    }
    val status = when {
        tasks.isNotEmpty() -> "ACTIVE"
        blockingIssues.isNotEmpty() -> "INFEASIBLE:${blockingIssues.joinToString(",") { it.name }}"
        else -> "EMPTY"
    }
    val settingsSnapshot = buildString {
        append('{')
        examDate?.let { append("\"examDate\":\"$it\",") }
        append("\"taskQuota\":${taskQuota.coerceAtLeast(1)}}")
    }
    return DailyPlanDraft(
        plan = DailyPlanEntity(
            id = planId,
            planDate = date.toString(),
            createdAt = nowMillis,
            examSchemeYear = examDate?.year,
            settingsSnapshot = settingsSnapshot,
            contentVersion = "runtime-card-queue-v1",
            status = status,
        ),
        tasks = tasks,
    )
}

private val TodayPlanCandidate.stableId: String
    get() = "card:$pointId"

private fun examFrequencyRank(value: String): Int = when (value) {
    "HIGH" -> 0
    "MEDIUM" -> 1
    "LOW" -> 2
    else -> Int.MAX_VALUE
}

@Singleton
class TodayPlanFactory @Inject constructor(
    private val cardRepository: CardRepository,
    private val reviewRepository: ReviewRepository,
    private val cardSettingsRepository: CardSettingsRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
) {
    suspend fun create(date: String): DailyPlanDraft {
        val localDate = LocalDate.parse(date)
        val now = System.currentTimeMillis()
        val settings = cardSettingsRepository.cardSettings.first()
        val queue = reviewRepository.getTodayStudyQueue().first()
        // A queue point is only actionable if the card repository can materialize at least one
        // card for it. This prevents a persisted Today task from opening an unrelated full
        // session or an empty target session when source tables are temporarily inconsistent.
        val availablePointIds = cardRepository.getCardsForReview().first().mapTo(mutableSetOf()) { it.pointId }
        val dueIds = queue.duePoints.mapTo(mutableSetOf()) { it.id }
        val repairCandidates = try {
            wrongAnswerRepository.observeDueWrongAnswers(now).first()
                .mapNotNull { detail ->
                    val pointId = detail.wrongAnswer.pointId?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    if (pointId in dueIds || pointId !in availablePointIds) return@mapNotNull null
                    TodayPlanCandidate(
                        pointId = pointId,
                        bucket = DailyBucket.REPAIR,
                        recentWeakness = detail.wrongAnswer.wrongCount,
                    )
                }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Repair is an enhancement; a malformed/stale wrong-answer join must not prevent
            // the core due/new card plan from opening.
            emptyList()
        }
        val candidates = distinctByPoint(
            buildList {
                queue.duePoints.filter { it.id in availablePointIds }.forEach { point ->
                    add(TodayPlanCandidate(point.id, DailyBucket.DUE, examFrequencyRank(point.examFrequency)))
                }
                addAll(repairCandidates)
                queue.newPoints.filter { it.id in availablePointIds }.forEach { point ->
                    add(TodayPlanCandidate(point.id, DailyBucket.NEW, examFrequencyRank(point.examFrequency)))
                }
            },
        )
        val examDate = settings.examDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(TODAY_ZONE).toLocalDate()
        }
        // dailyNewLimit already limits the NEW queue upstream. Do not reuse it as a total-task
        // cap: silently dropping an overdue point would make the persisted Today plan lie about
        // the review backlog. A future explicit daily-task quota can be added to settings with
        // its own migration and UI contract.
        val taskQuota = candidates.size.coerceAtLeast(1)
        return buildTodayPlanDraft(localDate, now, candidates, examDate, taskQuota)
    }

    private fun distinctByPoint(items: List<TodayPlanCandidate>): List<TodayPlanCandidate> {
        val seen = mutableSetOf<String>()
        return items.filter { seen.add(it.pointId) }
    }
}
