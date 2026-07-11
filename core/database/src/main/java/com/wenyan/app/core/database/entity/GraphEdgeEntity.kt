package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 图谱边表 Entity（graph_edges）。
 *
 * 对应设计文档 4.1 节 graph_edges 表，关系类型含 Spec 新增的 PREREQUISITE：
 * - id: 唯一标识
 * - source_id: 起点节点 ID（外键 graph_nodes.id）
 * - target_id: 终点节点 ID（外键 graph_nodes.id）
 * - type: 关系类型 AUTHORED / BELONGS_TO / PARTICIPATED_IN / INFLUENCED_BY /
 *         COMPARED_WITH / SAME_PERIOD / PRECEDES / RELATED_CONCEPT / PREREQUISITE
 *         （最后一种 PREREQUISITE 为 Spec 新增）
 * - weight: 权重，默认 1.0
 * - label: 边标签
 */
@Entity(
    tableName = "graph_edges",
    foreignKeys = [
        ForeignKey(
            entity = GraphNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GraphNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["target_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("source_id"),
        Index("target_id"),
        Index("type"),
    ],
)
data class GraphEdgeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "source_id")
    val sourceId: String,

    @ColumnInfo(name = "target_id")
    val targetId: String,

    /**
     * 关系类型：AUTHORED / BELONGS_TO / PARTICIPATED_IN / INFLUENCED_BY /
     * COMPARED_WITH / SAME_PERIOD / PRECEDES / RELATED_CONCEPT / PREREQUISITE
     */
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "weight", defaultValue = "1.0")
    val weight: Double = 1.0,

    @ColumnInfo(name = "label")
    val label: String?,
)
