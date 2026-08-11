package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 复习日志表 Entity（review_logs）。
 *
 * 对应设计文档 4.1 节 review_logs 表（约第 3796 行）：
 * - id: 唯一标识
 * - point_id: 知识点 ID（外键 knowledge_points.id）
 * - rating: 评分 Again / Hard / Good / Easy
 * - elapsed_days: 实际间隔天数
 * - scheduled_days: 计划间隔天数
 * - state: 复习时记忆状态
 * - stability: 复习时稳定性
 * - difficulty: 复习时难度
 * - reps: 复习次数
 * - created_at: 创建时间
 */
@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgePointEntity::class,
            parentColumns = ["id"],
            childColumns = ["point_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LearningUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["learning_unit_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("point_id"),
        Index("created_at"),
        Index("learning_unit_id"),
    ],
)
data class ReviewLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "point_id")
    val pointId: String,

    /** 评分：Again / Hard / Good / Easy */
    @ColumnInfo(name = "rating")
    val rating: String,

    @ColumnInfo(name = "elapsed_days")
    val elapsedDays: Int?,

    @ColumnInfo(name = "scheduled_days")
    val scheduledDays: Int?,

    @ColumnInfo(name = "state")
    val state: String?,

    @ColumnInfo(name = "stability")
    val stability: Float?,

    @ColumnInfo(name = "difficulty")
    val difficulty: Float?,

    @ColumnInfo(name = "reps")
    val reps: Int?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "learning_unit_id")
    val learningUnitId: String? = null,
)
