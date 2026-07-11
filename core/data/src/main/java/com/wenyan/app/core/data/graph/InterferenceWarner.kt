package com.wenyan.app.core.data.graph

import com.wenyan.app.core.data.repository.GraphRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 干扰预警检测器（Task 22）。
 *
 * 对应 Spec 第 298-305 行"干扰预警"功能：
 * - 连续复习图谱中相邻节点（如同一流派的两个作家）的卡片时
 * - 系统主动插入"区分卡"（对比两个易混淆节点）
 * - 提示用户注意区分
 *
 * 通过 Hilt @Inject 注入 [GraphRepository]。
 *
 * @property graphRepository 图谱仓库，提供相邻节点查询与节点元数据访问
 */
@Singleton
class InterferenceWarner @Inject constructor(
    private val graphRepository: GraphRepository,
) {

    /**
     * 检查干扰预警（SubTask 22.1）。
     *
     * 逻辑：
     * 1. 检查最近复习的节点中是否有图谱中相邻的（通过边连接）
     * 2. 若有相邻节点连续复习，返回 [InterferenceWarning]
     * 3. 建议插入区分卡
     *
     * @param reviewedNodeIds 按复习顺序排列的节点 ID 列表
     * @return 干扰预警信息；无干扰时返回 null
     */
    fun checkInterference(reviewedNodeIds: List<String>): Flow<InterferenceWarning?> = flow {
        // 至少需要两个节点才能检测连续复习干扰
        if (reviewedNodeIds.size < 2) {
            emit(null)
            return@flow
        }

        // 获取所有边，用于判断相邻关系和关系类型
        val allEdges = graphRepository.getAllEdges().first()

        // 遍历连续的节点对，检查是否相邻
        for (i in 0 until reviewedNodeIds.size - 1) {
            val node1Id = reviewedNodeIds[i]
            val node2Id = reviewedNodeIds[i + 1]

            // 查找连接这两个节点的边（双向）
            val connectingEdge = allEdges.find { edge ->
                (edge.sourceId == node1Id && edge.targetId == node2Id) ||
                    (edge.sourceId == node2Id && edge.targetId == node1Id)
            }

            if (connectingEdge != null) {
                // 找到相邻节点连续复习，返回预警
                emit(
                    InterferenceWarning(
                        node1Id = node1Id,
                        node2Id = node2Id,
                        relationType = connectingEdge.type,
                        suggestion = SUGGESTION_DISTINCTION_CARD,
                    ),
                )
                return@flow
            }
        }

        // 无相邻节点连续复习
        emit(null)
    }

    /**
     * 生成区分卡（SubTask 22.2）。
     *
     * 从两个节点的 metadata 中提取对比信息生成区分卡。
     * 对比信息包括：
     * - 节点标签（姓名/名称）
     * - subtitle（生卒年/时期）
     * - metadata 中的所有差异字段
     *
     * @param node1Id 节点 1 ID
     * @param node2Id 节点 2 ID
     * @return 区分卡数据
     */
    fun generateDistinctionCard(
        node1Id: String,
        node2Id: String,
    ): Flow<DistinctionCardData> = flow {
        // 获取两个节点
        val allNodes = graphRepository.getAllNodes().first()
        val node1 = allNodes.find { it.id == node1Id }
        val node2 = allNodes.find { it.id == node2Id }

        val label1 = node1?.label ?: node1Id
        val label2 = node2?.label ?: node2Id

        // 从 metadata 提取对比差异
        val differences = mutableListOf<String>()

        // 对比 subtitle（生卒年/时期等）
        val subtitle1 = node1?.subtitle
        val subtitle2 = node2?.subtitle
        if (!subtitle1.isNullOrEmpty() && !subtitle2.isNullOrEmpty() && subtitle1 != subtitle2) {
            differences.add("$label1（$subtitle1）vs $label2（$subtitle2）")
        }

        // 对比 metadata 中的字段
        val metadata1 = node1?.metadata ?: emptyMap()
        val metadata2 = node2?.metadata ?: emptyMap()
        val allKeys = (metadata1.keys + metadata2.keys).distinct()
        for (key in allKeys) {
            val value1 = metadata1[key]
            val value2 = metadata2[key]
            if (!value1.isNullOrEmpty() && !value2.isNullOrEmpty() && value1 != value2) {
                differences.add("$key：$label1 为「$value1」，$label2 为「$value2」")
            }
        }

        // 若无差异信息，添加默认提示
        if (differences.isEmpty()) {
            differences.add("请注意区分 $label1 与 $label2 的不同特征")
        }

        emit(
            DistinctionCardData(
                item1 = label1,
                item2 = label2,
                differences = differences,
            ),
        )
    }

    companion object {
        /** 干扰预警建议文案 */
        private const val SUGGESTION_DISTINCTION_CARD = "建议复习区分卡"
    }
}

/**
 * 干扰预警信息。
 *
 * @property node1Id 节点 1 ID
 * @property node2Id 节点 2 ID
 * @property relationType 关系类型（如 SAME_PERIOD / COMPARED_WITH）
 * @property suggestion 建议文案（"建议复习区分卡"）
 */
data class InterferenceWarning(
    val node1Id: String,
    val node2Id: String,
    val relationType: String,
    val suggestion: String,
)

/**
 * 区分卡数据。
 *
 * @property item1 对比项 1（节点标签）
 * @property item2 对比项 2（节点标签）
 * @property differences 差异列表
 */
data class DistinctionCardData(
    val item1: String,
    val item2: String,
    val differences: List<String>,
)
