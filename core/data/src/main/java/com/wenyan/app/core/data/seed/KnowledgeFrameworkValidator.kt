package com.wenyan.app.core.data.seed

/**
 * 所有显式知识框架共享的导入前校验器。
 *
 * 内容映射仍由各科目独立维护；这里只集中处理章节树结构、知识点覆盖范围和
 * 重复归属检查，避免四个科目各自复制一套容易漂移的校验逻辑。
 */
internal object KnowledgeFrameworkValidator {

    fun validate(
        nodes: List<FrameworkNode>,
        assignmentPairs: List<Pair<String, String>>,
        assignments: Map<String, String>,
        pointIds: Set<String>,
    ): List<String> {
        val errors = mutableListOf<String>()
        val nodeIds = nodes.map { it.id }.toSet()
        if (nodeIds.size != nodes.size) errors += "章节节点 ID 重复"
        if (nodes.any { it.id.isBlank() || it.title.isBlank() }) errors += "章节节点 ID 或标题为空"
        val danglingParents = nodes
            .filter { it.parentId != null && it.parentId !in nodeIds }
            .map { "${it.id}→${it.parentId}" }
        if (danglingParents.isNotEmpty()) {
            errors += "章节父节点不存在: ${danglingParents.joinToString()}"
        }
        val duplicateAssignmentIds = assignmentPairs
            .groupingBy { it.first }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateAssignmentIds.isNotEmpty()) {
            errors += "知识点重复归属: ${duplicateAssignmentIds.sorted().joinToString()}"
        }

        val missing = pointIds.filterNot(assignments::containsKey).sorted()
        if (missing.isNotEmpty()) errors += "知识点未归类: ${missing.joinToString()}"

        val stale = assignments.keys.filterNot(pointIds::contains).sorted()
        if (stale.isNotEmpty()) errors += "框架包含不存在的知识点: ${stale.joinToString()}"

        val danglingNodes = assignments.values.filterNot(nodeIds::contains).distinct().sorted()
        if (danglingNodes.isNotEmpty()) errors += "归属节点不存在: ${danglingNodes.joinToString()}"

        val nodeById = nodes.associateBy { it.id }
        for (node in nodes) {
            val seen = mutableSetOf<String>()
            var current: String? = node.id
            while (current != null) {
                if (!seen.add(current)) {
                    errors += "章节树存在循环: ${node.id}"
                    break
                }
                current = nodeById[current]?.parentId
            }
        }
        return errors.distinct()
    }
}
