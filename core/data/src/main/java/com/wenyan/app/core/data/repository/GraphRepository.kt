package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphNodeEntity
import com.wenyan.app.core.database.entity.GraphNodeType
import kotlinx.coroutines.flow.Flow

/**
 * 带可提取性 R 值的图谱节点数据对（阶段3新增）。
 *
 * @property node 图谱节点实体
 * @property retrievability 可提取性 R（0-1），-1f 表示节点无关联知识点
 */
data class NodeWithRetrievability(
    val node: GraphNodeEntity,
    val retrievability: Float,
)

/**
 * 知识图谱仓库接口。
 *
 * 协调图谱节点、边与记忆记录的查询与写入能力。
 * 抽象为接口便于测试替换（Fake 实现），生产环境由 [GraphRepositoryImpl] 实现。
 *
 * 功能性图谱核心方法：
 * - [getPrerequisites]: 获取节点的前置依赖节点（Spec 新增，用于前置依赖检测）
 * - [getRelatedNodes]: 获取与某节点关联的所有节点
 * - [getRetrievability]: 计算节点的可提取性 R（基于 FSRS stability + last_review_at）
 *
 * @see GraphRepositoryImpl
 */
interface GraphRepository {

    /**
     * 观察所有图谱节点。
     */
    fun getAllNodes(): Flow<List<GraphNodeEntity>>

    /**
     * 观察所有图谱边。
     */
    fun getAllEdges(): Flow<List<GraphEdgeEntity>>

    /**
     * 按节点类型观察节点。
     *
     * @param type 节点类型枚举
     */
    fun getNodesByType(type: GraphNodeType): Flow<List<GraphNodeEntity>>

    /**
     * 获取节点的前置依赖节点。
     *
     * 使用节点上的 prerequisites 字段（Spec 新增），存储前置依赖节点 ID 列表。
     * 用于 Spec 第 311-315 行"前置依赖检测"场景：
     * 用户学习某知识点前，先检查其前置依赖节点的可提取性 R。
     *
     * @param nodeId 目标节点 ID
     * @return 前置依赖节点列表，若节点不存在或无前置依赖则返回空列表
     */
    fun getPrerequisites(nodeId: String): Flow<List<GraphNodeEntity>>

    /**
     * 获取与某节点关联的所有节点（通过边连接）。
     *
     * 查询所有以该节点为起点或终点的边，返回对端节点列表。
     *
     * @param nodeId 目标节点 ID
     */
    fun getRelatedNodes(nodeId: String): Flow<List<GraphNodeEntity>>

    /**
     * 获取节点的相邻节点（通过边直接连接的节点）。
     *
     * Task 22.3 新增，用于干扰预警（Task 22）检测连续复习的相邻节点。
     * 与 [getRelatedNodes] 语义一致，返回所有通过边连接的对端节点。
     *
     * @param nodeId 目标节点 ID
     * @return 相邻节点列表
     */
    fun getAdjacentNodes(nodeId: String): Flow<List<GraphNodeEntity>>

    /**
     * 获取节点的考频（exam_frequency）。
     *
     * Task 22.3 新增，用于薄弱子图识别（Task 21）中按考频排序。
     * 通过节点的 related_point_id 关联到知识点，读取 knowledge_points.exam_frequency。
     *
     * @param nodeId 节点 ID
     * @return 考频值：HIGH / MEDIUM / LOW / NEVER；节点无关联知识点时返回 NEVER
     */
    fun getExamFrequency(nodeId: String): Flow<String>

    /**
     * 插入一个图谱节点。
     */
    suspend fun insertNode(node: GraphNodeEntity)

    /**
     * 插入一条图谱边。
     */
    suspend fun insertEdge(edge: GraphEdgeEntity)

    /**
     * 计算节点的可提取性 R（Retrievability）。
     *
     * 基于 FSRS-6 算法的保持率公式（幂律）：R = (1 + t/(9*S))^(-1)
     *
     * Spec 第 313-315 行：若前置节点 R < 0.7，先插入该前置节点的复习卡片。
     *
     * @param nodeId 节点 ID
     * @return 可提取性 R，取值范围 [0.0, 1.0]
     */
    fun getRetrievability(nodeId: String): Flow<Float>

    /**
     * 批量获取所有节点及其可提取性 R（阶段3新增）。
     *
     * 一次性 combine 节点流与记忆记录流，批量计算 R 值，避免 N+1 查询。
     * 记忆记录变更时（如评分后 upsert），R 值自动刷新。
     *
     * @return 节点 + R 值列表的 Flow
     */
    fun getNodesWithRetrievability(): Flow<List<NodeWithRetrievability>>

    /**
     * 批量查询知识点标题（v0.8.1 新增：替代 NodeDetailSheet 显示 UUID 的问题）。
     *
     * 用于图谱节点详情 BottomSheet：节点的 `sourceKpIds` 存储知识点 ID 列表，
     * 原实现直接显示 ID（如 "kp_00613"），用户体验差。
     * 现通过此方法批量查询标题，显示如"《呐喊》与新小说的奠基"。
     *
     * @param ids 知识点 ID 列表
     * @return id → title 映射；未找到的 ID 不出现在 map 中
     */
    suspend fun getKnowledgePointTitles(ids: List<String>): Map<String, String>
}
