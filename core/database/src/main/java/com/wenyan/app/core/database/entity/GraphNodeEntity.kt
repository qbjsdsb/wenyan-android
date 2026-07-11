package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 图谱节点表 Entity（graph_nodes）。
 *
 * 对应设计文档 4.1 节 graph_nodes 表，并合并 Spec 新增 prerequisites 字段：
 * - id: 唯一标识
 * - type: 节点类型 AUTHOR / WORK / SCHOOL / MOVEMENT / CONCEPT / KNOWLEDGE_POINT
 * - label: 节点标签
 * - subtitle: 副标题
 * - size: 节点大小，默认 1.0
 * - color: 节点颜色（颜色整数）
 * - related_point_id: 关联知识点 ID（外键 knowledge_points.id）
 * - subject_id: 所属科目 ID（外键 subjects.id）
 * - metadata: 元数据，JSON 对象 Map<String, String>
 * - prerequisites: 前置依赖节点 ID 列表，JSON 数组（Spec 新增字段）
 */
@Entity(
    tableName = "graph_nodes",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgePointEntity::class,
            parentColumns = ["id"],
            childColumns = ["related_point_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("type"),
        Index("related_point_id"),
        Index("subject_id"),
    ],
)
data class GraphNodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** 节点类型：AUTHOR / WORK / SCHOOL / MOVEMENT / CONCEPT / KNOWLEDGE_POINT */
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "subtitle")
    val subtitle: String?,

    @ColumnInfo(name = "size", defaultValue = "1.0")
    val size: Double = 1.0,

    @ColumnInfo(name = "color")
    val color: Int,

    @ColumnInfo(name = "related_point_id")
    val relatedPointId: String?,

    @ColumnInfo(name = "subject_id")
    val subjectId: String?,

    /** 元数据，JSON 对象 */
    @ColumnInfo(name = "metadata")
    val metadata: Map<String, String>?,

    /** 前置依赖节点 ID 列表，JSON 数组（Spec 新增字段） */
    @ColumnInfo(name = "prerequisites")
    val prerequisites: List<String>?,
)
