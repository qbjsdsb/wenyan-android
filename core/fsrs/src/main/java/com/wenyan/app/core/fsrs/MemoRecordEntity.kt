package com.wenyan.app.core.fsrs

/**
 * 背诵记录实体 —— 对应数据库 memo_records 表
 *
 * 对应设计文档第2734-2746行（memo_records表schema）和第3788-3793行（5.4.7节新增字段）。
 * 存储FSRS算法的核心状态变量（stability/difficulty/state）和复习历史。
 *
 * 字段映射关系（snake_case ↔ FlashCard camelCase）：
 * - state           ↔ FlashCard.state (State枚举名)
 * - stability       ↔ FlashCard.stability
 * - difficulty      ↔ FlashCard.difficulty
 * - last_review_at  ↔ FlashCard.lastReview (LocalDateTime → epoch millis)
 * - next_review_at  ↔ FlashCard.dueDate (LocalDateTime → epoch millis)
 * - review_count    ↔ FlashCard.reviewCount
 * - fail_count      ↔ FlashCard.lapses
 * - elapsed_days    ↔ FlashCard.elapsedDays
 * - scheduled_days  ↔ FlashCard.scheduledDays
 * - reps            ↔ FlashCard.reps
 * - history         ↔ JSON序列化的复习日志列表
 * - in_priority_queue ↔ 是否在优先攻坚队列
 *
 * @property pointId         知识点ID（主键，外键关联knowledge_points.id）
 * @property state           卡片状态（NEW/LEARNING/REVIEW/RELEARNING）
 * @property stability       记忆稳定性S（FSRS核心变量）
 * @property difficulty      难度D（1-10，FSRS核心变量）
 * @property lastReviewAt    上次复习时间（epoch millis）
 * @property nextReviewAt    下次复习时间（epoch millis）
 * @property reviewCount     复习次数
 * @property failCount       遗忘次数（Review→Again）
 * @property elapsedDays     距上次复习天数
 * @property scheduledDays   上次调度的间隔天数
 * @property reps            总复习次数
 * @property historyJson     复习历史JSON字符串
 * @property inPriorityQueue 是否在优先攻坚队列
 */
data class MemoRecordEntity(
    val pointId: String,
    val state: String,
    val stability: Float,
    val difficulty: Float,
    val lastReviewAt: Long,
    val nextReviewAt: Long,
    val reviewCount: Int = 0,
    val failCount: Int = 0,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
    val reps: Int = 0,
    val historyJson: String = "[]",
    val inPriorityQueue: Boolean = false
)
