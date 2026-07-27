package com.wenyan.app.feature.graph

import com.wenyan.app.core.data.repository.GraphRepository
import com.wenyan.app.core.data.repository.NodeWithRetrievability
import com.wenyan.app.core.database.dao.SubjectDao
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphNodeEntity
import com.wenyan.app.core.database.entity.GraphNodeType
import com.wenyan.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * [GraphRepository] 的 Fake 实现（feature:graph 测试用，v0.8.2 新增）。
 *
 * 与 core:data 测试包内的 FakeGraphRepository 结构一致，但放在 feature:graph 测试包内，
 * 保持模块隔离（测试代码不跨模块共享）。
 *
 * 通过 [MutableStateFlow] 暴露节点/边数据，便于测试中动态修改后验证 ViewModel 响应。
 *
 * @param initialNodes 初始节点列表
 * @param initialEdges 初始边列表
 * @param retrievabilityMap 节点 ID → 可提取性 R 值的映射（缺省 0f）
 * @param kpTitles 知识点 ID → 标题映射（v0.8.1 getKnowledgePointTitles 用）
 */
open class FakeGraphRepository(
    initialNodes: List<GraphNodeEntity> = emptyList(),
    initialEdges: List<GraphEdgeEntity> = emptyList(),
    private val retrievabilityMap: Map<String, Float> = emptyMap(),
    private val kpTitles: Map<String, String> = emptyMap(),
) : GraphRepository {

    private val _nodesFlow = MutableStateFlow(initialNodes)
    private val _edgesFlow = MutableStateFlow(initialEdges)

    /** 测试中可修改此值触发 ViewModel 重新订阅 */
    fun setNodes(nodes: List<GraphNodeEntity>) {
        _nodesFlow.value = nodes
    }

    /** 测试中可修改此值触发 ViewModel 重新订阅 */
    fun setEdges(edges: List<GraphEdgeEntity>) {
        _edgesFlow.value = edges
    }

    override fun getAllNodes(): Flow<List<GraphNodeEntity>> = _nodesFlow.asStateFlow()

    override fun getAllEdges(): Flow<List<GraphEdgeEntity>> = _edgesFlow.asStateFlow()

    override fun getNodesByType(type: GraphNodeType): Flow<List<GraphNodeEntity>> =
        flowOf(_nodesFlow.value.filter { it.type == type.name })

    override fun getPrerequisites(nodeId: String): Flow<List<GraphNodeEntity>> = flowOf(
        _nodesFlow.value.find { it.id == nodeId }?.prerequisites?.mapNotNull { id ->
            _nodesFlow.value.find { it.id == id }
        } ?: emptyList(),
    )

    override fun getRelatedNodes(nodeId: String): Flow<List<GraphNodeEntity>> {
        val relatedIds = _edgesFlow.value.flatMap { edge ->
            listOf(edge.sourceId, edge.targetId).filter { it != nodeId }
        }.distinct()
        return flowOf(relatedIds.mapNotNull { id -> _nodesFlow.value.find { it.id == id } })
    }

    override fun getAdjacentNodes(nodeId: String): Flow<List<GraphNodeEntity>> =
        getRelatedNodes(nodeId)

    override fun getExamFrequency(nodeId: String): Flow<String> {
        val node = _nodesFlow.value.find { it.id == nodeId }
        val freq = node?.metadata?.get("examFrequency") ?: "NEVER"
        return flowOf(freq)
    }

    override suspend fun insertNode(node: GraphNodeEntity) {
        // 测试中不需要实现
    }

    override suspend fun insertEdge(edge: GraphEdgeEntity) {
        // 测试中不需要实现
    }

    override fun getRetrievability(nodeId: String): Flow<Float> =
        flowOf(retrievabilityMap[nodeId] ?: 0f)

    open override fun getNodesWithRetrievability(): Flow<List<NodeWithRetrievability>> =
        _nodesFlow.map { nodes ->
            nodes.map { node ->
                NodeWithRetrievability(
                    node = node,
                    retrievability = retrievabilityMap[node.id] ?: 0f,
                )
            }
        }

    override suspend fun getKnowledgePointTitles(ids: List<String>): Map<String, String> =
        if (ids.isEmpty()) emptyMap() else kpTitles.filterKeys { it in ids }
}

/**
 * [SubjectDao] 的 Fake 实现（feature:graph 测试用，v0.8.2 新增）。
 *
 * 仅实现 [observeAll]（ViewModel 中唯一调用的方法），其余方法抛 [NotImplementedError]。
 */
class FakeSubjectDao(
    initialSubjects: List<SubjectEntity> = emptyList(),
) : SubjectDao {

    private val _subjectsFlow = MutableStateFlow(initialSubjects)

    fun setSubjects(subjects: List<SubjectEntity>) {
        _subjectsFlow.value = subjects
    }

    override fun observeAll(): Flow<List<SubjectEntity>> = _subjectsFlow.asStateFlow()

    override suspend fun insert(entity: SubjectEntity) {
        throw NotImplementedError("测试中不需要")
    }

    override suspend fun insertAll(entities: List<SubjectEntity>) {
        throw NotImplementedError("测试中不需要")
    }

    override suspend fun update(entity: SubjectEntity) {
        throw NotImplementedError("测试中不需要")
    }

    override suspend fun deleteById(id: String) {
        throw NotImplementedError("测试中不需要")
    }

    override suspend fun getById(id: String): SubjectEntity? = _subjectsFlow.value.find { it.id == id }

    override suspend fun count(): Int = _subjectsFlow.value.size
}

// ── 测试辅助构造函数 ──────────────────────────────────────────────────

/**
 * 创建测试用 [GraphNodeEntity]（简化必填参数，仅设置测试关心的字段）。
 */
fun testNode(
    id: String,
    label: String = id,
    type: String = "AUTHOR",
    subjectId: String? = "subj_02",
    relatedPointId: String? = null,
    metadata: Map<String, String>? = null,
    subtitle: String? = null,
    prerequisites: List<String>? = null,
) = GraphNodeEntity(
    id = id,
    type = type,
    label = label,
    subtitle = subtitle,
    color = 0,
    relatedPointId = relatedPointId,
    subjectId = subjectId,
    metadata = metadata,
    prerequisites = prerequisites,
)

/**
 * 创建测试用 [GraphEdgeEntity]（简化构造参数）。
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
