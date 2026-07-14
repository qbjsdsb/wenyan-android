package com.wenyan.app.core.data.graph

import com.wenyan.app.core.data.repository.GraphRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 前置依赖检测器（Task 20）。
 *
 * 对应 Spec 第 286-291 行"前置依赖检测"功能：
 * 学某知识点前先检查其依赖节点的可提取性 R，
 * 若任一前置节点 R < 0.7，先插入该前置节点的复习卡片，
 * 前置节点复习完成后，再进行新卡学习。
 *
 * 验证案例（Spec）："江西诗派"→先检查黄庭坚/杜甫/宋诗特点的 R 值。
 *
 * 通过 Hilt @Inject 注入 [GraphRepository]。
 *
 * 防御性保护（NF-BB3 修复）：
 * - [GraphRepository.getPrerequisites] 当前仅返回直接前置（一层），非递归，无 StackOverflow 风险。
 * - 但若数据异常（重复 ID）或未来改为递归遍历传递闭包，可能出现重复节点或超大列表。
 * - 故对返回结果去重（按 nodeId）并设上限 [MAX_PREREQUISITES]，超限截断并告警。
 *
 * @property graphRepository 图谱仓库，提供前置依赖查询与可提取性 R 计算
 */
@Singleton
class PrerequisiteChecker @Inject constructor(
    private val graphRepository: GraphRepository,
) {

    /**
     * 检查目标节点的前置依赖可提取性。
     *
     * 逻辑（SubTask 20.1）：
     * 1. 获取节点的 prerequisites 字段（前置依赖节点 ID 列表）
     * 2. 对每个前置节点计算可提取性 R
     * 3. R < [RETRIEVABILITY_THRESHOLD] 标记 needsReview = true
     * 4. 若任一 needsReview = true，canStartLearning = false
     *
     * 防御（NF-BB3）：对返回节点按 nodeId 去重 + 上限截断，防御异常数据与未来递归扩展。
     *
     * @param nodeId 目标节点 ID
     * @return 前置依赖检测结果
     */
    fun checkPrerequisites(nodeId: String): Flow<PrerequisiteCheckResult> = flow {
        // 1. 获取前置依赖节点列表（当前非递归，仅直接前置）
        val rawPrerequisiteNodes = graphRepository.getPrerequisites(nodeId).first()

        // NF-BB3 防御：去重（按 nodeId）+ 上限截断。
        // 当前 getPrerequisites 非递归，重复仅可能由异常数据引起；
        // 上限防御未来递归遍历传递闭包时遭遇环或超大图。
        val prerequisiteNodes = rawPrerequisiteNodes
            .distinctBy { it.id }
            .take(MAX_PREREQUISITES)
        if (rawPrerequisiteNodes.size > MAX_PREREQUISITES) {
            // 截断告警：数据异常或图过大，避免下游 R 值计算 N+1 查询拖垮性能
            android.util.Log.w(
                TAG,
                "Prerequisites truncated for nodeId=$nodeId: ${rawPrerequisiteNodes.size} > $MAX_PREREQUISITES",
            )
        }

        // 2. 对每个前置节点计算 R 值，构建 PrerequisiteNode 列表
        val prerequisites = prerequisiteNodes.map { node ->
            val r = graphRepository.getRetrievability(node.id).first()
            PrerequisiteNode(
                nodeId = node.id,
                label = node.label,
                retrievability = r,
                needsReview = r < RETRIEVABILITY_THRESHOLD,
            )
        }

        // 3. 找出 R < 阈值的前置节点 ID
        val blockedBy = prerequisites.filter { it.needsReview }.map { it.nodeId }

        // 4. 若存在需复习的前置节点，则不可开始学习
        val canStartLearning = blockedBy.isEmpty()

        emit(
            PrerequisiteCheckResult(
                nodeId = nodeId,
                prerequisites = prerequisites,
                canStartLearning = canStartLearning,
                blockedBy = blockedBy,
            ),
        )
    }

    /**
     * 生成前置复习队列（SubTask 20.2）。
     *
     * 返回需要先复习的前置节点 ID 列表（R < [RETRIEVABILITY_THRESHOLD] 的）。
     * 前置复习完成后才进行新卡学习。
     *
     * @param nodeId 目标节点 ID
     * @return 需要先复习的前置节点 ID 列表
     */
    fun generateReviewQueueForPrerequisites(nodeId: String): Flow<List<String>> = flow {
        val checkResult = checkPrerequisites(nodeId).first()
        // 返回被阻塞的前置节点 ID（R < 阈值的）
        emit(checkResult.blockedBy)
    }

    companion object {
        /** 可提取性 R 阈值，低于此值需先复习（Spec 要求 0.7） */
        const val RETRIEVABILITY_THRESHOLD = 0.7f

        /**
         * 前置节点列表防御性上限（NF-BB3）。
         * 当前非递归实现下，单节点直接前置通常 < 10；上限 100 容纳异常数据，
         * 并防御未来递归遍历传递闭包时遭遇环或超大图导致 N+1 R 值查询。
         */
        private const val MAX_PREREQUISITES = 100

        private const val TAG = "PrerequisiteChecker"
    }
}

/**
 * 前置依赖检测结果。
 *
 * @property nodeId 目标节点 ID
 * @property prerequisites 前置依赖节点列表（含 R 值与 needsReview 标记）
 * @property canStartLearning 是否可以开始学习（所有前置 R >= 阈值时 true）
 * @property blockedBy 被阻塞的前置节点 ID 列表（R < 阈值的）
 */
data class PrerequisiteCheckResult(
    val nodeId: String,
    val prerequisites: List<PrerequisiteNode>,
    val canStartLearning: Boolean,
    val blockedBy: List<String>,
)

/**
 * 前置依赖节点信息。
 *
 * @property nodeId 节点 ID
 * @property label 节点标签
 * @property retrievability 可提取性 R 值
 * @property needsReview 是否需要复习（R < 阈值时 true）
 */
data class PrerequisiteNode(
    val nodeId: String,
    val label: String,
    val retrievability: Float,
    val needsReview: Boolean,
)
