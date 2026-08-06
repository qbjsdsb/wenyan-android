package com.wenyan.app.core.data.seed

/**
 * 文学理论的显式知识框架。
 *
 * 文学理论不是按年代排列的单线学科，核心关系是“学科基础 → 文学活动 → 创作 →
 * 作品 → 接受 → 批评”，理论史和重要理论家则作为贯穿性的历史路径单列。每个
 * 知识点只保留一个主要归属，互相引用的理论通过 tags/relatedIds 表达。
 *
 * 这份配置覆盖文学理论的 190 个知识点，兼顾教材的章节顺序、概念之间的依赖
 * 关系和 929—935 的中西文论补充专题，避免把补充内容遗留在科目根节点。
 */
object TheoryKnowledgeFramework {

    const val SUBJECT_CODE = "theory"
    const val SUBJECT_NAME = "文学理论"

    /** 相对于科目根章节的显式框架节点。 */
    val nodes: List<FrameworkNode> = listOf(
        FrameworkNode("theory_foundations", null, "文学理论导论与学科基础", 1),
        FrameworkNode("theory_foundations_subject", "theory_foundations", "学科定位、教材与研究方法", 1),
        FrameworkNode("theory_foundations_concepts", "theory_foundations", "文学概念、文学性与理论观念", 2),
        FrameworkNode("theory_foundations_author", "theory_foundations", "作者、意图与作者理论", 3),

        FrameworkNode("theory_activity", null, "文学本质与文学活动", 2),
        FrameworkNode("theory_activity_marxism", "theory_activity", "马克思主义文学理论基石", 1),
        FrameworkNode("theory_activity_essence", "theory_activity", "文学活动、发生与审美意识形态", 2),
        FrameworkNode("theory_activity_socialist", "theory_activity", "社会主义时期文学活动", 3),

        FrameworkNode("theory_creation", null, "文学创作论", 3),
        FrameworkNode("theory_creation_process", "theory_creation", "文学创造过程与心理机制", 1),
        FrameworkNode("theory_creation_truth", "theory_creation", "艺术真实、人文精神与形式化", 2),
        FrameworkNode("theory_creation_types", "theory_creation", "文学类型与创作方式", 3),

        FrameworkNode("theory_work", null, "文学作品论", 4),
        FrameworkNode("theory_work_genre", "theory_work", "文类、体裁与文体", 1),
        FrameworkNode("theory_work_poetry", "theory_work", "诗学、意象与比喻", 2),
        FrameworkNode("theory_work_text", "theory_work", "文本结构与作品层次", 3),
        FrameworkNode("theory_work_narrative", "theory_work", "小说叙事与人物结构", 4),
        FrameworkNode("theory_work_drama", "theory_work", "戏剧结构与悲剧理论", 5),
        FrameworkNode("theory_work_lyric", "theory_work", "抒情内容与抒情话语", 6),
        FrameworkNode("theory_work_style", "theory_work", "文学风格与创作个性", 7),

        FrameworkNode("theory_reception", null, "文学接受论", 5),
        FrameworkNode("theory_reception_theory", "theory_reception", "读者导向与接受美学", 1),
        FrameworkNode("theory_reception_process", "theory_reception", "文学消费、期待与接受过程", 2),

        FrameworkNode("theory_criticism", null, "文学批评与文化研究", 6),
        FrameworkNode("theory_criticism_standards", "theory_criticism", "文学批评的界定与标准", 1),
        FrameworkNode("theory_criticism_ideology", "theory_criticism", "意识形态、社会与文化批评", 2),
        FrameworkNode("theory_criticism_modes", "theory_criticism", "批评模式与批评方法", 3),

        FrameworkNode("theory_history", null, "文学史与理论流变", 7),
        FrameworkNode("theory_history_literary", "theory_history", "文学史的性质、范式与叙事", 1),
        FrameworkNode("theory_history_schools", "theory_history", "结构主义、后结构主义与现象学", 2),
        FrameworkNode("theory_history_development", "theory_history", "理论范式演变与本土化", 3),
        FrameworkNode("theory_history_key", "theory_history", "叙事学、对话理论与符号学", 4),
        FrameworkNode("theory_history_supplement", "theory_history", "中西文论补充专题", 5),
    )

    /**
     * 知识点 ID → 框架节点 ID。
     *
     * 数字区间仅用于减少重复书写；[validate] 会在导入前检查一对一完整性，
     * 任何新增、删除或误归类都会让导入失败而不会静默落到根章节。
     */
    private val assignmentPairs: List<Pair<String, String>> = buildList {
        addAll(mapPoints("theory_foundations_subject", 727..727, 734..735, 743..747, 830..832, 840..841))
        addAll(mapPoints("theory_foundations_concepts", 728..733, 736..736, 833..839))
        addAll(mapPoints("theory_foundations_author", 737..738, 785..790))

        addAll(mapPoints("theory_activity_marxism", 842..844))
        addAll(mapPoints("theory_activity_essence", 845..855))
        addAll(mapPoints("theory_activity_socialist", 856..856))

        addAll(mapPoints("theory_creation_process", 857..866))
        addAll(mapPoints("theory_creation_truth", 867..869))
        addAll(mapPoints("theory_creation_types", 870..873))

        addAll(mapPoints("theory_work_genre", 741..742, 761..763, 874..876))
        addAll(mapPoints("theory_work_poetry", 740..740, 764..772))
        addAll(mapPoints("theory_work_text", 749..760, 877..881))
        addAll(mapPoints("theory_work_narrative", 773..781, 882..884))
        addAll(mapPoints("theory_work_drama", 782..784))
        addAll(mapPoints("theory_work_lyric", 885..886))
        addAll(mapPoints("theory_work_style", 887..889))

        addAll(mapPoints("theory_reception_theory", 791..797))
        addAll(mapPoints("theory_reception_process", 890..900))

        addAll(mapPoints("theory_criticism_standards", 739..739, 901..903))
        addAll(mapPoints("theory_criticism_ideology", 798..808))
        addAll(mapPoints("theory_criticism_modes", 748..748, 904..909))

        addAll(mapPoints("theory_history_literary", 809..816))
        addAll(mapPoints("theory_history_schools", 817..820))
        addAll(mapPoints("theory_history_development", 821..823))
        addAll(mapPoints("theory_history_key", 824..829))
        addAll(mapPoints("theory_history_supplement", 929..935))
    }

    val assignments: Map<String, String> = assignmentPairs.toMap()

    /** 返回导入前必须解决的框架数据问题。空列表表示通过。 */
    fun validate(pointIds: Set<String>): List<String> {
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

    private fun mapPoints(nodeId: String, vararg groups: Iterable<Int>): List<Pair<String, String>> =
        groups.flatMap { group -> group.map { "kp_${it.toString().padStart(5, '0')}" to nodeId } }
}
