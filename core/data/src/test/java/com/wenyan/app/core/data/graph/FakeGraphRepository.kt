package com.wenyan.app.core.data.graph

import com.wenyan.app.core.data.repository.GraphRepository
import com.wenyan.app.core.data.repository.NodeWithRetrievability
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphNodeEntity
import com.wenyan.app.core.database.entity.GraphNodeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * GraphRepository 的 Fake 实现，供图谱算法单元测试使用。
 *
 * 通过内存 List/Map 模拟 DAO 行为，所有查询方法返回 [flowOf] 以便 `.first()` 直接取值。
 * 写入方法（insertNode/insertEdge）为空实现，测试中不需要持久化。
 *
 * @param nodes 全部节点列表
 * @param edges 全部边列表
 * @param retrievabilityMap 节点 ID → 可提取性 R 值的映射
 * @param examFrequencyMap 节点 ID → 考频（HIGH/MEDIUM/LOW/NEVER）的映射
 */
class FakeGraphRepository(
    private val nodes: List<GraphNodeEntity> = emptyList(),
    private val edges: List<GraphEdgeEntity> = emptyList(),
    private val retrievabilityMap: Map<String, Float> = emptyMap(),
    private val examFrequencyMap: Map<String, String> = emptyMap(),
) : GraphRepository {

    override fun getAllNodes(): Flow<List<GraphNodeEntity>> = flowOf(nodes)

    override fun getAllEdges(): Flow<List<GraphEdgeEntity>> = flowOf(edges)

    override fun getNodesByType(type: GraphNodeType): Flow<List<GraphNodeEntity>> =
        flowOf(nodes.filter { it.type == type.name })

    override fun getPrerequisites(nodeId: String): Flow<List<GraphNodeEntity>> = flowOf(
        nodes.find { it.id == nodeId }?.prerequisites?.mapNotNull { id ->
            nodes.find { it.id == id }
        } ?: emptyList(),
    )

    override fun getRelatedNodes(nodeId: String): Flow<List<GraphNodeEntity>> {
        val relatedIds = edges.flatMap { edge ->
            listOf(edge.sourceId, edge.targetId).filter { it != nodeId }
        }.distinct()
        return flowOf(relatedIds.mapNotNull { id -> nodes.find { it.id == id } })
    }

    override fun getAdjacentNodes(nodeId: String): Flow<List<GraphNodeEntity>> =
        getRelatedNodes(nodeId)

    override fun getExamFrequency(nodeId: String): Flow<String> =
        flowOf(examFrequencyMap[nodeId] ?: "NEVER")

    override suspend fun insertNode(node: GraphNodeEntity) {
        // 测试中不需要实现
    }

    override suspend fun insertEdge(edge: GraphEdgeEntity) {
        // 测试中不需要实现
    }

    override fun getRetrievability(nodeId: String): Flow<Float> =
        flowOf(retrievabilityMap[nodeId] ?: 0f)

    /**
     * 批量返回节点 + R 值（阶段3新增，测试用 Fake 实现）。
     *
     * 使用 [retrievabilityMap] 中的预设 R 值；未预设的节点返回 0f。
     */
    override fun getNodesWithRetrievability(): Flow<List<NodeWithRetrievability>> =
        flowOf(
            nodes.map { node ->
                NodeWithRetrievability(
                    node = node,
                    retrievability = retrievabilityMap[node.id] ?: 0f,
                )
            },
        )
}

// ── 测试辅助函数 ──────────────────────────────────────────────────

/**
 * 创建测试用图谱节点（简化构造参数，仅设置测试关心的字段）。
 */
fun testNode(
    id: String,
    label: String,
    prerequisites: List<String>? = null,
    relatedPointId: String? = null,
    metadata: Map<String, String>? = null,
    subtitle: String? = null,
    type: String = "KNOWLEDGE_POINT",
) = GraphNodeEntity(
    id = id,
    type = type,
    label = label,
    subtitle = subtitle,
    color = 0,
    relatedPointId = relatedPointId,
    subjectId = null,
    metadata = metadata,
    prerequisites = prerequisites,
)

/**
 * 创建测试用图谱边（简化构造参数）。
 */
fun testEdge(
    from: String,
    to: String,
    type: String = "RELATED_CONCEPT",
    label: String? = null,
) = GraphEdgeEntity(
    id = "${from}_$to",
    sourceId = from,
    targetId = to,
    type = type,
    label = label,
)
