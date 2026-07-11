package com.wenyan.app.core.data.mapper

import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.fsrs.FlashCard
import com.wenyan.app.core.fsrs.Rating
import com.wenyan.app.core.fsrs.ReviewLog
import com.wenyan.app.core.fsrs.State
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * FlashCard ↔ Room MemoRecordEntity 双向映射器（阶段3迁移自 core:fsrs）。
 *
 * 原先 core:fsrs 包内有一个同名 data class MemoRecordEntity（Float/Boolean 类型），
 * 与 Room 实体（Double/Int 类型）不兼容。阶段3消除这一双重实体：
 * - 删除 core:fsrs/MemoRecordEntity.kt
 * - 本映射器迁移到 core:data，直接映射 FlashCard ↔ Room MemoRecordEntity
 * - 内部做 Float↔Double / Boolean↔Int 转换
 *
 * 字段映射关系：
 * - state           ↔ FlashCard.state (State枚举名)
 * - stability       ↔ FlashCard.stability (Double ↔ Float)
 * - difficulty      ↔ FlashCard.difficulty (Double ↔ Float)
 * - last_review_at  ↔ FlashCard.lastReview (LocalDateTime → epoch millis)
 * - next_review_at  ↔ FlashCard.dueDate (LocalDateTime → epoch millis)
 * - review_count    ↔ FlashCard.reviewCount
 * - fail_count      ↔ FlashCard.lapses
 * - elapsed_days    ↔ FlashCard.elapsedDays
 * - scheduled_days  ↔ FlashCard.scheduledDays
 * - reps            ↔ FlashCard.reps
 * - history         ↔ JSON 序列化的复习日志列表
 * - in_priority_queue ↔ 是否在优先攻坚队列 (Int 0/1 ↔ Boolean)
 */
object MemoRecordMapper {

    private const val DAY_MS = 86_400_000L

    /**
     * Room MemoRecordEntity → FlashCard
     *
     * 将数据库记录转换为 FSRS 算法可操作的卡片对象。
     * 内部做 Double→Float 转换以适配 FSRS 算法的 Float 类型。
     *
     * @param memoRecord Room 数据库背诵记录
     * @return FSRS FlashCard 对象
     */
    fun toFlashCard(memoRecord: MemoRecordEntity): FlashCard {
        val lastReview = if (memoRecord.lastReviewAt > 0) {
            millisToLocalDateTime(memoRecord.lastReviewAt)
        } else null

        val dueDate = millisToLocalDateTime(memoRecord.nextReviewAt)

        val state = try {
            State.valueOf(memoRecord.state)
        } catch (e: IllegalArgumentException) {
            State.NEW
        }

        return FlashCard(
            dueDate = dueDate,
            stability = memoRecord.stability.toFloat(),
            difficulty = memoRecord.difficulty.toFloat(),
            interval = if (memoRecord.lastReviewAt > 0) {
                ((memoRecord.nextReviewAt - memoRecord.lastReviewAt) / DAY_MS).toInt()
            } else 0,
            reviewCount = memoRecord.reviewCount,
            lastReview = lastReview,
            state = state,
            elapsedDays = memoRecord.elapsedDays,
            scheduledDays = memoRecord.scheduledDays,
            reps = memoRecord.reps,
            lapses = memoRecord.failCount,
        )
    }

    /**
     * FlashCard → Room MemoRecordEntity
     *
     * 将 FSRS 调度后的卡片对象转换回数据库记录。
     * 内部做 Float→Double / Boolean→Int 转换以适配 Room 实体类型。
     *
     * @param flashCard           FSRS FlashCard 对象（调度后）
     * @param pointId             知识点 ID（主键）
     * @param inPriorityQueue     是否在优先攻坚队列
     * @param reviewLog           本次复习日志（可选，传入则追加到 history）
     * @param existingHistoryJson 已有的复习历史 JSON（默认 "[]"，传入新日志时追加到其中）
     * @return Room 数据库背诵记录
     */
    fun toMemoRecord(
        flashCard: FlashCard,
        pointId: String,
        inPriorityQueue: Boolean = false,
        reviewLog: ReviewLog? = null,
        existingHistoryJson: String = "[]",
    ): MemoRecordEntity {
        val lastReviewAt = flashCard.lastReview?.let {
            localDateTimeToMillis(it)
        } ?: 0L

        val nextReviewAt = localDateTimeToMillis(flashCard.dueDate)

        val historyJson = if (reviewLog != null) {
            appendReviewLog(existingHistoryJson, reviewLog)
        } else {
            existingHistoryJson
        }

        return MemoRecordEntity(
            pointId = pointId,
            state = flashCard.state.name,
            stability = flashCard.stability.toDouble(),
            difficulty = flashCard.difficulty.toDouble(),
            lastReviewAt = lastReviewAt,
            nextReviewAt = nextReviewAt,
            reviewCount = flashCard.reviewCount,
            failCount = flashCard.lapses,
            elapsedDays = flashCard.elapsedDays,
            scheduledDays = flashCard.scheduledDays,
            reps = flashCard.reps,
            history = historyJson,
            inPriorityQueue = if (inPriorityQueue) 1 else 0,
        )
    }

    /**
     * 将一条复习日志追加到已有的历史 JSON 数组中
     */
    private fun appendReviewLog(existingJson: String, log: ReviewLog): String {
        val entry = formatReviewLogJson(log)
        val trimmed = existingJson.trim()
        return when {
            trimmed.isEmpty() || trimmed == "[]" -> "[$entry]"
            trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                val inner = trimmed.dropLast(1).trimEnd()
                if (inner == "[") {
                    "[$entry]"
                } else {
                    "$inner,$entry]"
                }
            }
            else -> "[$entry]"  // 格式异常时重置
        }
    }

    /**
     * 将单条 ReviewLog 序列化为 JSON 对象字符串
     */
    private fun formatReviewLogJson(log: ReviewLog): String {
        val dueMillis = localDateTimeToMillis(log.dueDate)
        val rtMillis = localDateTimeToMillis(log.reviewTime)
        return StringBuilder(128)
            .append('{')
            .append("\"rating\":").append(log.rating.value).append(',')
            .append("\"state\":\"").append(log.state.name).append("\",")
            .append("\"dueDate\":").append(dueMillis).append(',')
            .append("\"stability\":").append(log.stability).append(',')
            .append("\"difficulty\":").append(log.difficulty).append(',')
            .append("\"elapsedDays\":").append(log.elapsedDays).append(',')
            .append("\"lastElapsedDays\":").append(log.lastElapsedDays).append(',')
            .append("\"scheduledDays\":").append(log.scheduledDays).append(',')
            .append("\"reviewTime\":").append(rtMillis)
            .append('}')
            .toString()
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
