package com.wenyan.app.core.fsrs

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * FlashCard ↔ MemoRecordEntity 双向映射器
 *
 * 对应设计文档第3696-3743行 FSRSAdapter和spec.md第268行（TypeConverter映射说明）。
 *
 * FSRS-Kotlin库FlashCard字段（camelCase）：dueDate/reviewCount/lastReview/stability/difficulty/state
 * 设计文档memo_records表字段（snake_case）：state/stability/difficulty/last_review_at/next_review_at/
 *   review_count/fail_count/elapsed_days/scheduled_days/reps/history/in_priority_queue
 *
 * 本映射器负责在两者之间做TypeConverter转换：
 * - LocalDateTime ↔ epoch millis (Long)
 * - State枚举 ↔ String
 * - Boolean ↔ Int (in_priority_queue)
 */
object MemoRecordMapper {

    private const val DAY_MS = 86_400_000L

    /**
     * MemoRecordEntity → FlashCard
     *
     * 将数据库记录转换为FSRS算法可操作的卡片对象。
     *
     * @param memoRecord 数据库背诵记录
     * @return FSRS FlashCard对象
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
            stability = memoRecord.stability,
            difficulty = memoRecord.difficulty,
            interval = if (memoRecord.lastReviewAt > 0) {
                ((memoRecord.nextReviewAt - memoRecord.lastReviewAt) / DAY_MS).toInt()
            } else 0,
            reviewCount = memoRecord.reviewCount,
            lastReview = lastReview,
            state = state,
            elapsedDays = memoRecord.elapsedDays,
            scheduledDays = memoRecord.scheduledDays,
            reps = memoRecord.reps,
            lapses = memoRecord.failCount
        )
    }

    /**
     * FlashCard → MemoRecordEntity
     *
     * 将FSRS调度后的卡片对象转换回数据库记录。
     *
     * @param flashCard           FSRS FlashCard对象（调度后）
     * @param pointId             知识点ID（主键）
     * @param inPriorityQueue     是否在优先攻坚队列
     * @param reviewLog           本次复习日志（可选，传入则追加到historyJson）
     * @param existingHistoryJson 已有的复习历史JSON（默认"[]"，传入新日志时追加到其中）
     * @return 数据库背诵记录
     */
    fun toMemoRecord(
        flashCard: FlashCard,
        pointId: String,
        inPriorityQueue: Boolean = false,
        reviewLog: ReviewLog? = null,
        existingHistoryJson: String = "[]"
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
            stability = flashCard.stability,
            difficulty = flashCard.difficulty,
            lastReviewAt = lastReviewAt,
            nextReviewAt = nextReviewAt,
            reviewCount = flashCard.reviewCount,
            failCount = flashCard.lapses,
            elapsedDays = flashCard.elapsedDays,
            scheduledDays = flashCard.scheduledDays,
            reps = flashCard.reps,
            historyJson = historyJson,
            inPriorityQueue = inPriorityQueue
        )
    }

    /**
     * 将一条复习日志追加到已有的历史JSON数组中
     *
     * @param existingJson 已有的JSON数组字符串（如"[{...},{...}]"）
     * @param log          待追加的复习日志
     * @return 追加后的JSON数组字符串
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
     * 将单条ReviewLog序列化为JSON对象字符串
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

    /**
     * epoch millis → LocalDateTime（使用系统默认时区）
     * FSRS算法用本地时区算"今天"（设计文档5.4.5节：不要用UTC）
     */
    private fun millisToLocalDateTime(millis: Long): LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        )
    }

    /**
     * LocalDateTime → epoch millis（使用系统默认时区）
     */
    private fun localDateTimeToMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
