package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 记忆记录表 Entity（memo_records）。
 *
 * 对应设计文档 4.1 节 memo_records 表（FSRS 算法调度数据）：
 * - point_id: 知识点 ID（主键 + 外键 knowledge_points.id）
 * - state: 记忆状态 NEW / LEARNING / REVIEW / RELEARNING
 * - stability: 稳定性，默认 0.0
 * - difficulty: 难度，默认 5.0
 * - last_review_at: 上次复习时间
 * - next_review_at: 下次复习时间
 * - review_count / fail_count: 复习/失败次数
 * - elapsed_days: 距上次复习天数（阶段3新增，FSRS调度状态）
 * - scheduled_days: 上次调度的间隔天数（阶段3新增）
 * - reps: 总复习次数（阶段3新增，与 review_count 同步）
 * - history: 复习历史，JSON 字符串（结构复杂，业务层解析）
 * - in_priority_queue: 是否在优先队列，默认 0
 */
@Entity(
    tableName = "memo_records",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgePointEntity::class,
            parentColumns = ["id"],
            childColumns = ["point_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("next_review_at"),
        Index("in_priority_queue"),
    ],
)
data class MemoRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "point_id")
    val pointId: String,

    /** 记忆状态：NEW / LEARNING / REVIEW / RELEARNING */
    @ColumnInfo(name = "state")
    val state: String,

    @ColumnInfo(name = "stability", defaultValue = "0.0")
    val stability: Double = 0.0,

    @ColumnInfo(name = "difficulty", defaultValue = "5.0")
    val difficulty: Double = 5.0,

    @ColumnInfo(name = "last_review_at")
    val lastReviewAt: Long,

    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: Long,

    @ColumnInfo(name = "review_count", defaultValue = "0")
    val reviewCount: Int = 0,

    @ColumnInfo(name = "fail_count", defaultValue = "0")
    val failCount: Int = 0,

    /** 距上次复习天数（FSRS调度状态，阶段3新增） */
    @ColumnInfo(name = "elapsed_days", defaultValue = "0")
    val elapsedDays: Int = 0,

    /** 上次调度的间隔天数（FSRS调度状态，阶段3新增） */
    @ColumnInfo(name = "scheduled_days", defaultValue = "0")
    val scheduledDays: Int = 0,

    /** 总复习次数（FSRS调度状态，阶段3新增，与 review_count 同步） */
    @ColumnInfo(name = "reps", defaultValue = "0")
    val reps: Int = 0,

    /** 复习历史，JSON 字符串（结构复杂，业务层解析） */
    @ColumnInfo(name = "history")
    val history: String?,

    @ColumnInfo(name = "in_priority_queue", defaultValue = "0")
    val inPriorityQueue: Int = 0,
)
