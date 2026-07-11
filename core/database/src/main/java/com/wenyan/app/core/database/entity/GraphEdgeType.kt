package com.wenyan.app.core.database.entity

/**
 * 图谱关系类型枚举（9 种，含 Spec 新增 PREREQUISITE）。
 *
 * 对应 Spec 4.1 节 graph_edges.type 字段：
 * - [AUTHORED]: 作者-作品
 * - [BELONGS_TO]: 作品-流派 / 作家-流派
 * - [PARTICIPATED_IN]: 作家-运动
 * - [INFLUENCED_BY]: 受影响
 * - [COMPARED_WITH]: 对比
 * - [SAME_PERIOD]: 同时期
 * - [PRECEDES]: 先于
 * - [RELATED_CONCEPT]: 相关概念
 * - [PREREQUISITE]: 前置依赖（Spec 新增）
 */
enum class GraphEdgeType {
    AUTHORED,
    BELONGS_TO,
    PARTICIPATED_IN,
    INFLUENCED_BY,
    COMPARED_WITH,
    SAME_PERIOD,
    PRECEDES,
    RELATED_CONCEPT,
    PREREQUISITE,
}
