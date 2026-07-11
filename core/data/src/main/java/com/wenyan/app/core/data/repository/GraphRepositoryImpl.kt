package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.GraphEdgeDao
import com.wenyan.app.core.database.dao.GraphNodeDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphNodeEntity
import com.wenyan.app.core.database.entity.GraphNodeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

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
        val prerequisiteNodes = prerequisiteIds.mapNotNull { graphNodeDao.getById(it) }
        emit(prerequisiteNodes)
    }

    override fun getRelatedNodes(nodeId: String): Flow<List<GraphNodeEntity>> =
        graphEdgeDao.observeRelatedTo(nodeId).map { edges ->
            val relatedIds = edges.flatMap { edge ->
                listOf(edge.sourceId, edge.targetId).filter { it != nodeId }
            }.distinct()
            relatedIds.mapNotNull { graphNodeDao.getById(it) }
        }

    override fun getAdjacentNodes(nodeId: String): Flow<List<GraphNodeEntity>> =
        graphEdgeDao.observeRelatedTo(nodeId).map { edges ->
            val adjacentIds = edges.flatMap { edge ->
                listOf(edge.sourceId, edge.targetId).filter { it != nodeId }
            }.distinct()
            adjacentIds.mapNotNull { graphNodeDao.getById(it) }
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
        // 稳定性 ≤ 0 表示新卡片，尚未形成记忆，R = 0
        if (memo.stability <= 0.0) {
            emit(0f)
            return@flow
        }
        val now = System.currentTimeMillis()
        val elapsedMillis = now - memo.lastReviewAt
        // 未复习或时间异常，R = 1（记忆完全保持）
        if (elapsedMillis <= 0L) {
            emit(1f)
            return@flow
        }
        val elapsedDays = elapsedMillis.toDouble() / MILLIS_PER_DAY
        val r = exp(-elapsedDays / memo.stability)
        // 钳制到 [0.0, 1.0] 范围
        emit(max(0.0, min(1.0, r)).toFloat())
    }

    private companion object {
        private const val MILLIS_PER_DAY = 24.0 * 60 * 60 * 1000

        /** 默认考频（节点无关联知识点时返回） */
        private const val EXAM_FREQUENCY_NEVER = "NEVER"
    }
}
