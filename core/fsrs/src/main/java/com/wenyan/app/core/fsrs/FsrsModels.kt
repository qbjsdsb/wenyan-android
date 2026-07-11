package com.wenyan.app.core.fsrs

import java.time.LocalDateTime

/**
 * FSRS评分枚举
 *
 * 对应FSRS-Kotlin库的Rating枚举和设计文档第3470行。
 * 用户复习时的4档评分反馈，FSRS算法据此调整记忆稳定性和难度。
 *
 * @property value 评分值（1=Again, 2=Hard, 3=Good, 4=Easy）
 */
enum class Rating(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4);

    companion object {
        fun fromValue(v: Int): Rating = entries.first { it.value == v }
    }
}

/**
 * 卡片状态枚举
 *
 * 对应设计文档第3475行和memo_records表的state字段（NEW/LEARNING/REVIEW/RELEARNING）。
 * FSRS-Kotlin库使用CardPhase(Added/ReLearning/Review)，此处采用设计文档的4状态模型。
 *
 * @property NEW         新卡，尚未开始复习
 * @property LEARNING    学习中，初次接触正在熟悉
 * @property REVIEW      复习中，已毕业进入长期记忆维护
 * @property RELEARNING  重学中，遗忘后重新学习
 */
enum class State {
    NEW,
    LEARNING,
    REVIEW,
    RELEARNING
}

/**
 * FSRS记忆卡片 —— 核心数据类
 *
 * 字段名遵循FSRS-Kotlin库的camelCase命名（dueDate/reviewCount/lastReview），
 * 同时包含设计文档5.4.3节Card所需的完整字段（state/elapsedDays/scheduledDays/reps/lapses）。
 *
 * @property dueDate       下次到期复习时间（对应memo_records.next_review_at）
 * @property stability     记忆稳定性S（FSRS核心变量，单位天）
 * @property difficulty    难度D（1-10，FSRS核心变量，动态调整）
 * @property interval      当前调度间隔（天）
 * @property reviewCount   复习次数（对应memo_records.review_count）
 * @property lastReview    上次复习时间（对应memo_records.last_review_at）
 * @property state         卡片状态（对应memo_records.state）
 * @property elapsedDays   距上次复习天数
 * @property scheduledDays 上次调度的间隔天数
 * @property reps          总复习次数（与reviewCount同步）
 * @property lapses        遗忘次数（Review→Again，对应memo_records.fail_count）
 */
data class FlashCard(
    val dueDate: LocalDateTime = LocalDateTime.now(),
    val stability: Float = 0f,
    val difficulty: Float = 0f,
    val interval: Int = 0,
    val reviewCount: Int = 0,
    val lastReview: LocalDateTime? = null,
    val state: State = State.NEW,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0
)

/**
 * 复习日志记录
 *
 * 对应设计文档第3529行ReviewLog和review_logs表。
 * 每次复习后记录评分前后的状态变化，用于参数优化。
 */
data class ReviewLog(
    val rating: Rating,
    val state: State,
    val dueDate: LocalDateTime,
    val stability: Float,
    val difficulty: Float,
    val elapsedDays: Int,
    val lastElapsedDays: Int,
    val scheduledDays: Int,
    val reviewTime: LocalDateTime
)

/**
 * 调度结果（卡片+日志）
 */
data class SchedulingCard(
    val card: FlashCard,
    val reviewLog: ReviewLog
)
