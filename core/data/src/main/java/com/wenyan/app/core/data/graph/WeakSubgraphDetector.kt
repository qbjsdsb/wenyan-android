package com.wenyan.app.core.data.graph

import com.wenyan.app.core.data.repository.GraphRepository
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphNodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 薄弱子图识别器（Task 21）。
 *
 * 对应 Spec 第 292-297 行"薄弱子图识别"功能：
 * - 每日复习开始时识别知识图谱中 R 值最低的连通子图
 * - 优先推送该子图的卡片
 * - 在图谱可视化中用红色标记该薄弱区域
 * - 薄弱子图过大（>20节点）时按考频排序，优先推送高频考点子集（前10个）
 *
 * 通过 Hilt @Inject 注入 [GraphRepository]。
 *
 * @property graphRepository 图谱仓库，提供节点/边查询、R 值计算与考频查询
 */
@Singleton
class WeakSubgraphDetector @Inject constructor(
    private val graphRepository: GraphRepository,
) {

    /**
     * 识别薄弱子图（SubTask 21.1）。
     *
     * 逻辑：
     * 1. 获取所有节点和边
     * 2. 计算每个节点的 R 值
     * 3. 用 BFS 找出所有连通分量
     * 4. 找出平均 R 值最低的连通子图
     * 5. 若子图 > [MAX_SUBGRAPH_SIZE] 节点，按 exam_frequency 排序取前 [PRIORITY_SUBSET_SIZE] 个
     *
     * @return 薄弱子图识别结果
     */
    fun detectWeakSubgraph(): Flow<WeakSubgraph> = flow {
        // 1. 获取所有节点和边
        val nodes = graphRepository.getAllNodes().first()
        val edges = graphRepository.getAllEdges().first()

        // 空图谱直接返回空结果
        if (nodes.isEmpty()) {
            emit(
                WeakSubgraph(
                    nodeIds = emptyList(),
                    averageR = 0f,
                    isTooLarge = false,
                    prioritizedNodeIds = emptyList(),
                ),
            )
            return@flow
        }

        // 2. 计算每个节点的 R 值
        val nodeRMap = mutableMapOf<String, Float>()
        for (node in nodes) {
            val r = graphRepository.getRetrievability(node.id).first()
            nodeRMap[node.id] = r
        }

        // 3. 用 BFS 找连通分量
        val adjacency = buildAdjacencyList(nodes, edges)
        val visited = mutableSetOf<String>()
        val connectedComponents = mutableListOf<List<String>>()

        for (node in nodes) {
            if (node.id !in visited) {
                val component = bfsComponent(node.id, adjacency, visited)
                connectedComponents.add(component)
            }
        }

        // 4. 找出平均 R 值最低的连通子图
        var weakestComponent: List<String> = emptyList()
        var weakestAverageR = Float.MAX_VALUE

        for (component in connectedComponents) {
            val avgR = component.mapNotNull { nodeRMap[it] }.let { rValues ->
                if (rValues.isEmpty()) 0f else rValues.sum() / rValues.size
            }
            if (avgR < weakestAverageR) {
                weakestAverageR = avgR
                weakestComponent = component
            }
        }

        // 5. 若子图 > 20 节点，按考频排序取前 10 个
        val isTooLarge = weakestComponent.size > MAX_SUBGRAPH_SIZE
        val prioritizedNodeIds = if (isTooLarge) {
            getPrioritizedNodeIds(weakestComponent)
        } else {
            weakestComponent
        }

        emit(
            WeakSubgraph(
                nodeIds = weakestComponent,
                averageR = if (weakestAverageR == Float.MAX_VALUE) 0f else weakestAverageR,
                isTooLarge = isTooLarge,
                prioritizedNodeIds = prioritizedNodeIds,
            ),
        )
    }

    /**
     * 获取每日优先推送卡片（SubTask 21.2）。
     *
     * 返回薄弱子图中优先推送的卡片对应知识点 ID。
     * 子图未超限时返回全部节点，超限时返回按考频排序的前 10 个。
     *
     * @return 优先推送的知识点 ID 列表
     */
    fun getDailyPriorityCards(): Flow<List<String>> = flow {
        val weakSubgraph = detectWeakSubgraph().first()
        emit(weakSubgraph.prioritizedNodeIds)
    }

    /**
     * 构建邻接表（无向图）。
     */
    private fun buildAdjacencyList(
        nodes: List<GraphNodeEntity>,
        edges: List<GraphEdgeEntity>,
    ): Map<String, MutableList<String>> {
        val adjacency = mutableMapOf<String, MutableList<String>>()
        // 初始化每个节点的邻接列表
        for (node in nodes) {
            adjacency[node.id] = mutableListOf()
        }
        // 添加边（无向图，双向添加）
        // NF-BB12 修复：孤儿边（sourceId 或 targetId 不在 nodes 列表中）静默丢弃，
        // 数据一致性问题难发现。现加 Log.w 告警，便于排查图谱数据异常。
        for (edge in edges) {
            val sourceAdj = adjacency[edge.sourceId]
            val targetAdj = adjacency[edge.targetId]
            if (sourceAdj == null || targetAdj == null) {
                // v0.8.21: Log.w → Timber.w（tag 自动推断为 "WeakSubgraphDetector"）
                Timber.w(
                    "Orphan edge dropped: sourceId=${edge.sourceId}, targetId=${edge.targetId}, " +
                        "type=${edge.type}. Node(s) not in current graph nodes list.",
                )
                continue
            }
            sourceAdj.add(edge.targetId)
            targetAdj.add(edge.sourceId)
        }
        return adjacency
    }

    /**
     * BFS 遍历一个连通分量。
     */
    private fun bfsComponent(
        startNodeId: String,
        adjacency: Map<String, MutableList<String>>,
        visited: MutableSet<String>,
    ): List<String> {
        val component = mutableListOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(startNodeId)
        visited.add(startNodeId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            component.add(current)
            val neighbors = adjacency[current] ?: emptyList()
            for (neighbor in neighbors) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        return component
    }

    /**
     * 按考频排序获取优先节点子集。
     *
     * 考频排序优先级：HIGH > MEDIUM > LOW > NEVER
     * 取前 [PRIORITY_SUBSET_SIZE] 个。
     */
    private suspend fun getPrioritizedNodeIds(nodeIds: List<String>): List<String> {
        // 收集每个节点的考频
        val nodeFrequencyPairs = nodeIds.map { nodeId ->
            val frequency = graphRepository.getExamFrequency(nodeId).first()
            nodeId to frequency
        }

        // 按考频优先级排序，取前 10 个
        return nodeFrequencyPairs
            .sortedByDescending { it.second.toPriorityWeight() }
            .take(PRIORITY_SUBSET_SIZE)
            .map { it.first }
    }

    /**
     * 将考频字符串转为排序权重。
     */
    private fun String.toPriorityWeight(): Int = when (this) {
        EXAM_FREQUENCY_HIGH -> 4
        EXAM_FREQUENCY_MEDIUM -> 3
        EXAM_FREQUENCY_LOW -> 2
        else -> 1 // NEVER 或未知
    }

    companion object {
        /** 薄弱子图节点数上限（Spec 要求 20） */
        const val MAX_SUBGRAPH_SIZE = 20

        /** 超限时优先推送的子集大小（Spec 要求前 10） */
        const val PRIORITY_SUBSET_SIZE = 10

        private const val EXAM_FREQUENCY_HIGH = "HIGH"
        private const val EXAM_FREQUENCY_MEDIUM = "MEDIUM"
        private const val EXAM_FREQUENCY_LOW = "LOW"
    }
}

/**
 * 薄弱子图识别结果。
 *
 * @property nodeIds 薄弱子图节点 ID 列表
 * @property averageR 平均 R 值
 * @property isTooLarge 是否超过 20 节点
 * @property prioritizedNodeIds 超过 20 节点时按考频排序的前 10 个；未超限时为全部节点
 */
data class WeakSubgraph(
    val nodeIds: List<String>,
    val averageR: Float,
    val isTooLarge: Boolean,
    val prioritizedNodeIds: List<String>,
)
