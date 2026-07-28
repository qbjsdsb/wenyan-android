package com.wenyan.app.core.data.mapper

import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.fsrs.FlashCard
import com.wenyan.app.core.fsrs.State
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * WrongAnswerEntity sched_* 字段 ↔ FlashCard 双向映射器（v0.9.4 新增）。
 *
 * 错题本独立维护 FSRS 调度状态（sched_* 前缀字段），与知识点的 memo_records 互不干扰。
 * 本映射器负责 WrongAnswerEntity 的 sched_* 字段与 FSRS FlashCard 之间的类型转换，
 * 模式与 [MemoRecordMapper] 完全一致，仅字段名前缀不同（sched_ vs 无前缀）。
 *
 * 字段映射关系：
 * - sched_state          ↔ FlashCard.state (State 枚举名)
 * - sched_stability      ↔ FlashCard.stability
 * - sched_difficulty     ↔ FlashCard.difficulty
 * - sched_last_review_at ↔ FlashCard.lastReview (LocalDateTime → epoch millis)
 * - sched_next_review_at ↔ FlashCard.dueDate (LocalDateTime → epoch millis)
 * - sched_review_count   ↔ FlashCard.reviewCount
 * - sched_lapses         ↔ FlashCard.lapses
 * - sched_elapsed_days   ↔ FlashCard.elapsedDays
 * - sched_scheduled_days ↔ FlashCard.scheduledDays
 * - sched_reps           ↔ FlashCard.reps
 *
 * @see MemoRecordMapper 同构映射器，用于 memo_records 表
 */
object WrongAnswerSchedulingMapper {

    private const val DAY_MS = 86_400_000L

    /**
     * WrongAnswerEntity → FlashCard
     *
     * 将错题的 sched_* 调度字段转换为 FSRS 算法可操作的卡片对象。
     *
     * @param wrongAnswer 错题记录（含 sched_* 字段）
     * @return FSRS FlashCard 对象
     */
    fun toFlashCard(wrongAnswer: WrongAnswerEntity): FlashCard {
        val lastReview = if (wrongAnswer.schedLastReviewAt > 0) {
            millisToLocalDateTime(wrongAnswer.schedLastReviewAt)
        } else null

        val dueDate = millisToLocalDateTime(wrongAnswer.schedNextReviewAt)

        val state = try {
            State.valueOf(wrongAnswer.schedState)
        } catch (e: IllegalArgumentException) {
            State.NEW
        }

        return FlashCard(
            dueDate = dueDate,
            stability = wrongAnswer.schedStability,
            difficulty = wrongAnswer.schedDifficulty,
            interval = if (wrongAnswer.schedLastReviewAt > 0) {
                ((wrongAnswer.schedNextReviewAt - wrongAnswer.schedLastReviewAt) / DAY_MS).toInt()
            } else 0,
            reviewCount = wrongAnswer.schedReviewCount,
            lastReview = lastReview,
            state = state,
            elapsedDays = wrongAnswer.schedElapsedDays,
            scheduledDays = wrongAnswer.schedScheduledDays,
            reps = wrongAnswer.schedReps,
            lapses = wrongAnswer.schedLapses,
        )
    }

    /**
     * FlashCard 调度结果 → WrongAnswerEntity sched_* 字段更新参数。
     *
     * 返回一个 lambda，调用方用 [WrongAnswerDao.updateScheduling] 写入。
     * 不直接返回 Entity 以避免覆盖 wrong_answers 表的非调度字段
     * （如 wrongCount、resolvedAt、userAnswer 等）。
     *
     * @param flashCard FSRS 调度后的 FlashCard
     * @return sched_* 字段值的数据类
     */
    fun toSchedulingUpdate(flashCard: FlashCard): SchedulingUpdate {
        val lastReviewAt = flashCard.lastReview?.let {
            localDateTimeToMillis(it)
        } ?: 0L

        val nextReviewAt = localDateTimeToMillis(flashCard.dueDate)

        return SchedulingUpdate(
            state = flashCard.state.name,
            stability = flashCard.stability,
            difficulty = flashCard.difficulty,
            lastReviewAt = lastReviewAt,
            nextReviewAt = nextReviewAt,
            reviewCount = flashCard.reviewCount,
            lapses = flashCard.lapses,
            elapsedDays = flashCard.elapsedDays,
            scheduledDays = flashCard.scheduledDays,
            reps = flashCard.reps,
        )
    }

    // ===================== 时间转换工具 =====================

    private fun millisToLocalDateTime(millis: Long): LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneId.systemDefault(),
        )
    }

    private fun localDateTimeToMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

/**
 * FSRS 调度字段更新参数（v0.9.4 新增）。
 *
 * 由 [WrongAnswerSchedulingMapper.toSchedulingUpdate] 生成，
 * 传递给 [com.wenyan.app.core.database.dao.WrongAnswerDao.updateScheduling]。
 */
data class SchedulingUpdate(
    val state: String,
    val stability: Float,
    val difficulty: Float,
    val lastReviewAt: Long,
    val nextReviewAt: Long,
    val reviewCount: Int,
    val lapses: Int,
    val elapsedDays: Int,
    val scheduledDays: Int,
    val reps: Int,
)
