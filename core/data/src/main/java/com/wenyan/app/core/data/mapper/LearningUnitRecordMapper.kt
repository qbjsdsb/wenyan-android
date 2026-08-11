package com.wenyan.app.core.data.mapper

import com.wenyan.app.core.database.entity.LearningUnitRecordEntity
import com.wenyan.app.core.fsrs.FlashCard
import com.wenyan.app.core.fsrs.State
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object LearningUnitRecordMapper {
    private const val DAY_MS = 86_400_000L

    fun toFlashCard(record: LearningUnitRecordEntity): FlashCard = FlashCard(
        dueDate = record.nextReviewAt.toDateTime(),
        stability = record.stability,
        difficulty = record.difficulty,
        interval = if (record.lastReviewAt > 0) ((record.nextReviewAt - record.lastReviewAt) / DAY_MS).toInt() else 0,
        reviewCount = record.reviewCount,
        lastReview = record.lastReviewAt.takeIf { it > 0 }?.toDateTime(),
        state = runCatching { State.valueOf(record.state) }.getOrDefault(State.NEW),
        elapsedDays = record.elapsedDays,
        scheduledDays = record.scheduledDays,
        reps = record.reps,
        lapses = record.failCount,
    )

    fun fromFlashCard(card: FlashCard, unitId: String, inPriorityQueue: Boolean) = LearningUnitRecordEntity(
        learningUnitId = unitId,
        state = card.state.name,
        stability = card.stability,
        difficulty = card.difficulty,
        lastReviewAt = card.lastReview?.toMillis() ?: 0,
        nextReviewAt = card.dueDate.toMillis(),
        reviewCount = card.reviewCount,
        failCount = card.lapses,
        elapsedDays = card.elapsedDays,
        scheduledDays = card.scheduledDays,
        reps = card.reps,
        inPriorityQueue = if (inPriorityQueue) 1 else 0,
    )

    private fun Long.toDateTime() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
    private fun LocalDateTime.toMillis() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
