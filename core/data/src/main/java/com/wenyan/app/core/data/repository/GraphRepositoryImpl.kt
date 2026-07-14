package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.GraphEdgeDao
import com.wenyan.app.core.database.dao.GraphNodeDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphNodeEntity
import com.wenyan.app.core.database.entity.GraphNodeType
import com.wenyan.app.core.database.entity.MemoRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * 知识图谱仓库实现（[GraphRepository] 接口的生产实现）。
 *
 * 协调 [GraphNodeDao]、[GraphEdgeDao] 与 [MemoRecordDao]，提供图谱的查询与写入能力。
 *
 * @property graphNodeDao 图谱节点 DAO
 * @property graphEdgeDao 图谱边 DAO
 * @property memoRecordDao 记忆记录 DAO（用于计算可提取性 R）
 * @property knowledgePointDao 知识点 DAO（用于查询考频 exam_frequency）
 */
@Singleton
class GraphRepositoryImpl @Inject constructor(
    private val graphNodeDao: GraphNodeDao,
    private val graphEdgeDao: GraphEdgeDao,
    private val memoRecordDao: MemoRecordDao,
    private val knowledgePointDao: KnowledgePointDao,
) : GraphRepository {

    override fun getAllNodes(): Flow<List<GraphNodeEntity>> =
        graphNodeDao.observeAll()

    override fun getAllEdges(): Flow<List<GraphEdgeEntity>> =
        graphEdgeDao.observeAll()

    override fun getNodesByType(type: GraphNodeType): Flow<List<GraphNodeEntity>> =
        graphNodeDao.observeByType(type.name)

    override fun getPrerequisites(nodeId: String): Flow<List<GraphNodeEntity>> = flow {
        val node = graphNodeDao.getById(nodeId)
        if (node == null) {
            emit(emptyList())
            return@flow
        }
        val prerequisiteIds = node.prerequisites
        if (prerequisiteIds.isNullOrEmpty()) {
            emit(emptyList())
            return@flow
        }
        // P1-D2 修正：原 mapNotNull { getById(it) } 串行 N 次查询，改为一次 IN 批量查询
        val nodesById = graphNodeDao.getByIds(prerequisiteIds).associateBy { it.id }
        emit(prerequisiteIds.mapNotNull { nodesById[it] })
    }

    override fun getRelatedNodes(nodeId: String): Flow<List<GraphNodeEntity>> =
        graphEdgeDao.observeRelatedTo(nodeId).map { edges ->
            val relatedIds = edges.flatMap { edge ->
                listOf(edge.sourceId, edge.targetId).filter { it != nodeId }
            }.distinct()
            // P1-D1 修正：原 mapNotNull { getById(it) } 串行 N 次查询，改为一次 IN 批量查询
            if (relatedIds.isEmpty()) {
                emptyList()
            } else {
                val nodesById = graphNodeDao.getByIds(relatedIds).associateBy { it.id }
                relatedIds.mapNotNull { nodesById[it] }
            }
        }

    override fun getAdjacentNodes(nodeId: String): Flow<List<GraphNodeEntity>> =
        graphEdgeDao.observeRelatedTo(nodeId).map { edges ->
            val adjacentIds = edges.flatMap { edge ->
                listOf(edge.sourceId, edge.targetId).filter { it != nodeId }
            }.distinct()
            // P1-D1 修正：同 getRelatedNodes，改为一次 IN 批量查询
            if (adjacentIds.isEmpty()) {
                emptyList()
            } else {
                val nodesById = graphNodeDao.getByIds(adjacentIds).associateBy { it.id }
                adjacentIds.mapNotNull { nodesById[it] }
            }
        }

    override fun getExamFrequency(nodeId: String): Flow<String> = flow {
        val node = graphNodeDao.getById(nodeId)
        if (node == null) {
            emit(EXAM_FREQUENCY_NEVER)
            return@flow
        }
        val pointId = node.relatedPointId
        if (pointId == null) {
            emit(EXAM_FREQUENCY_NEVER)
            return@flow
        }
        val point = knowledgePointDao.getById(pointId)
        if (point == null) {
            emit(EXAM_FREQUENCY_NEVER)
            return@flow
        }
        emit(point.examFrequency)
    }

    override suspend fun insertNode(node: GraphNodeEntity) {
        graphNodeDao.insert(node)
    }

    override suspend fun insertEdge(edge: GraphEdgeEntity) {
        graphEdgeDao.insert(edge)
    }

    override fun getRetrievability(nodeId: String): Flow<Float> = flow {
        val node = graphNodeDao.getById(nodeId)
        if (node == null) {
            emit(0f)
            return@flow
        }
        val pointId = node.relatedPointId
        if (pointId == null) {
            emit(0f)
            return@flow
        }
        val memo = memoRecordDao.getById(pointId)
        if (memo == null) {
            emit(0f)
            return@flow
        }
        emit(calculateRetrievability(memo, System.currentTimeMillis()))
    }

    /**
     * 批量获取所有节点及其可提取性 R（阶段3新增）。
     *
     * combine 节点流与记忆记录流，一次性批量计算 R 值。
     * 记忆记录变更时（如评分后 upsert），R 值自动刷新。
     */
    override fun getNodesWithRetrievability(): Flow<List<NodeWithRetrievability>> =
        combine(
            graphNodeDao.observeAll(),
            memoRecordDao.observeAll(),
        ) { nodes, memos ->
            val memoMap = memos.associateBy { it.pointId }
            val now = System.currentTimeMillis()
            nodes.map { node ->
                val r = calculateRetrievabilityForNode(node, memoMap, now)
                NodeWithRetrievability(node = node, retrievability = r)
            }
        }

    /**
     * 计算单个节点的可提取性 R（批量场景，从 memoMap 取值）。
     *
     * @param node    图谱节点
     * @param memoMap pointId → MemoRecordEntity 映射
     * @param now     当前时间戳（毫秒）
     * @return 可提取性 R（0-1），无关联知识点或无记录返回 0f
     */
    private fun calculateRetrievabilityForNode(
        node: GraphNodeEntity,
        memoMap: Map<String, MemoRecordEntity>,
        now: Long,
    ): Float {
        val pointId = node.relatedPointId ?: return 0f
        val memo = memoMap[pointId] ?: return 0f
        return calculateRetrievability(memo, now)
    }

    /**
     * 计算可提取性 R（FSRS-6 幂律公式）。
     *
     * 公式：R = (1 + t / (9 * S))^(-1)
     * - t：距上次复习天数
     * - S：记忆稳定性
     *
     * 阶段3统一公式：原先用 exp(-t/S)（指数衰减），现改为 FSRS-6 幂律，
     * 与 [com.wenyan.app.core.fsrs.FsrsWrapper.getRetrievability] 一致。
     *
     * @param memo 记忆记录
     * @param now  当前时间戳（毫秒）
     * @return 可提取性 R（0-1），stability ≤ 0 返回 0
     */
    private fun calculateRetrievability(memo: MemoRecordEntity, now: Long): Float {
        // 稳定性 ≤ 0 表示新卡片，尚未形成记忆，R = 0
        if (memo.stability <= 0.0) return 0f

        val elapsedMillis = now - memo.lastReviewAt
        // 未复习或时间异常，R = 1（记忆完全保持）
        if (elapsedMillis <= 0L) return 1f

        val elapsedDays = (elapsedMillis.toDouble() / MILLIS_PER_DAY).toFloat().coerceAtLeast(0f)
        val s = memo.stability.toFloat()
        // FSRS-6 幂律公式：R = (1 + t/(9*S))^(-1)
        val r = (1f + elapsedDays / (9f * s)).pow(-1f)
        return r.coerceIn(0f, 1f)
    }

    private companion object {
        private const val MILLIS_PER_DAY = 24.0 * 60 * 60 * 1000

        /** 默认考频（节点无关联知识点时返回） */
        private const val EXAM_FREQUENCY_NEVER = "NEVER"
    }
}
