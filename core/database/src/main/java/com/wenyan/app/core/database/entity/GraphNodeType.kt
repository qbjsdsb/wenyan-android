package com.wenyan.app.core.database.entity

/**
 * 图谱节点类型枚举（6 种）。
 *
 * 对应 Spec 4.1 节 graph_nodes.type 字段：
 * - [AUTHOR]: 作家
 * - [WORK]: 作品
 * - [SCHOOL]: 流派/社团
 * - [MOVEMENT]: 文学运动
 * - [CONCEPT]: 概念/术语
 * - [KNOWLEDGE_POINT]: 知识点
 */
enum class GraphNodeType {
    AUTHOR,
    WORK,
    SCHOOL,
    MOVEMENT,
    CONCEPT,
    KNOWLEDGE_POINT,
}
