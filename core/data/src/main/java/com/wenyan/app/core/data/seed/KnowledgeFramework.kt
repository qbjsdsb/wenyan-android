package com.wenyan.app.core.data.seed

/**
 * 中国现当代文学的显式知识框架。
 *
 * 这份配置故意与标题关键词匹配分开：标题只适合做兜底判断，不能承担教材章节归属。
 * 节点 ID 一旦发布就不再复用，重新命名只改变 [FrameworkNode.title]，从而保护用户的
 * 章节浏览状态以及知识点与章节之间的长期引用。
 */
object KnowledgeFramework {

    const val SUBJECT_CODE = "modern"
    const val SUBJECT_NAME = "中国现当代文学"

    /** 相对于科目根章节的显式框架节点。 */
    val nodes: List<FrameworkNode> = listOf(
        FrameworkNode("modern_history", null, "文学史观与新文学发生", 1),
        FrameworkNode("modern_history_late_qing", "modern_history", "晚清文学与小说界革命", 1),

        FrameworkNode("modern_modern", null, "中国现代文学（1917—1949）", 2),
        FrameworkNode("modern_first", "modern_modern", "第一个十年：文学革命与新诗（1917—1927）", 1),
        FrameworkNode("modern_first_movements", "modern_first", "文学革命、社团与小说", 1),
        FrameworkNode("modern_first_poetry", "modern_first", "新诗发展与诗歌流派", 2),
        FrameworkNode("modern_first_lu", "modern_first", "鲁迅与启蒙小说", 3),
        FrameworkNode("modern_first_prose", "modern_first", "现代散文与抒情写作", 4),
        FrameworkNode("modern_first_publication", "modern_first", "新文学出版与传播", 5),

        FrameworkNode("modern_second", "modern_modern", "第二个十年：左翼、京派与海派（1928—1937）", 2),
        FrameworkNode("modern_second_left", "modern_second", "革命文学与左翼文学", 1),
        FrameworkNode("modern_second_styles", "modern_second", "京派、海派与现代散文", 2),
        FrameworkNode("modern_second_fiction", "modern_second", "小说与现代戏剧", 3),

        FrameworkNode("modern_third", "modern_modern", "第三个十年：战争与社会转型（1937—1949）", 3),
        FrameworkNode("modern_third_war", "modern_third", "战争、解放区与人性书写", 1),

        FrameworkNode("modern_contemporary", null, "中国当代文学（1949年至今）", 3),
        FrameworkNode("modern_seventeen", "modern_contemporary", "十七年文学（1949—1966）", 1),
        FrameworkNode("modern_seventeen_system", "modern_seventeen", "文学体制与创作规范", 1),
        FrameworkNode("modern_seventeen_red", "modern_seventeen", "红色经典与革命历史叙事", 2),
        FrameworkNode("modern_seventeen_rural", "modern_seventeen", "农村题材与人性书写", 3),
        FrameworkNode("modern_seventeen_poetry", "modern_seventeen", "政治抒情诗与散文", 4),
        FrameworkNode("modern_seventeen_other", "modern_seventeen", "城市、战争与知识分子书写", 5),

        FrameworkNode("modern_cultural", "modern_contemporary", "“文革”时期文学（1966—1976）", 2),
        FrameworkNode("modern_cultural_main", "modern_cultural", "主流文学、三突出与样板戏", 1),
        FrameworkNode("modern_cultural_underground", "modern_cultural", "地下写作与文学转折", 2),

        FrameworkNode("modern_new", "modern_contemporary", "新时期文学（1976—1989）", 3),
        FrameworkNode("modern_new_restore", "modern_new", "伤痕、反思与改革文学", 1),
        FrameworkNode("modern_new_poetry", "modern_new", "朦胧诗与新诗转型", 2),
        FrameworkNode("modern_new_root", "modern_new", "寻根文学与文化反思", 3),
        FrameworkNode("modern_new_avant", "modern_new", "先锋文学与现代主义实验", 4),

        FrameworkNode("modern_since90", "modern_contemporary", "九十年代以来：多元化写作", 4),
        FrameworkNode("modern_since_realism", "modern_since90", "新写实与现实主义转型", 1),
        FrameworkNode("modern_since_history", "modern_since90", "新历史、地域与少数民族叙事", 2),
        FrameworkNode("modern_since_women", "modern_since90", "女性、私人化与身体写作", 3),
        FrameworkNode("modern_since_popular", "modern_since90", "通俗文学、儿童文学与戏剧", 4),
        FrameworkNode("modern_since_poetry", "modern_since90", "诗歌、散文与大众文化", 5),
        FrameworkNode("modern_since_new_century", "modern_since90", "新世纪文学与文学生态", 6),

        FrameworkNode("modern_taiwan_hk", null, "台港文学与跨地域书写", 4),
        FrameworkNode("modern_taiwan", "modern_taiwan_hk", "台湾文学", 1),
        FrameworkNode("modern_hongkong", "modern_taiwan_hk", "香港文学", 2),
        FrameworkNode("modern_diaspora", "modern_taiwan_hk", "海外华文与离散写作", 3),
    )

    /**
     * 知识点 ID → 框架节点 ID。
     *
     * 数字区间只用于减少重复书写；最终仍会在导入前验证为一对一的完整映射。
     */
    private val assignmentPairs: List<Pair<String, String>> = buildList {
        addAll(mapPoints("modern_history", 578..581, listOf(951, 955)))
        addAll(mapPoints("modern_history_late_qing", 582..586, listOf(946)))

        addAll(mapPoints("modern_first_movements", 587..593, 602..604, listOf(939, 953, 954)))
        addAll(mapPoints("modern_first_poetry", 594..601))
        addAll(mapPoints("modern_first_prose", listOf(605, 606, 612, 621)))
        addAll(mapPoints("modern_first_publication", listOf(607, 608)))
        addAll(mapPoints("modern_first_lu", 610..611, 613..620))

        addAll(mapPoints("modern_second_styles", listOf(609), 622..626, listOf(915, 916, 945)))
        addAll(mapPoints("modern_second_fiction", 627..633, listOf(917)))
        addAll(mapPoints("modern_second_left", 634..636, listOf(941)))
        addAll(mapPoints("modern_third_war", listOf(919, 922, 940, 947, 948, 949, 950)))

        addAll(mapPoints("modern_seventeen_system", listOf(637)))
        addAll(mapPoints("modern_seventeen_red", listOf(638, 640), 648..656))
        addAll(mapPoints("modern_seventeen_rural", 641..642))
        addAll(mapPoints("modern_seventeen_poetry", 657..659, listOf(937)))
        addAll(mapPoints("modern_seventeen_other", listOf(639), 643..647))

        addAll(mapPoints("modern_cultural_main", 660..662))
        addAll(mapPoints("modern_cultural_underground", 663..666))

        addAll(mapPoints("modern_new_poetry", 667..671))
        addAll(mapPoints("modern_new_restore", 672..676, listOf(679, 680, 681, 689, 691, 694, 695, 714, 715, 918, 936, 942, 943, 944)))
        addAll(mapPoints("modern_new_root", listOf(677, 682, 683, 684, 685)))
        addAll(mapPoints("modern_new_avant", listOf(678, 692), 696..701, listOf(952)))

        addAll(mapPoints("modern_since_realism", listOf(910, 706, 711)))
        addAll(mapPoints("modern_since_history", listOf(686, 687, 688, 690, 707, 708, 709, 716, 719, 720, 920)))
        addAll(mapPoints("modern_since_women", listOf(717, 718, 710)))
        addAll(mapPoints("modern_since_popular", listOf(693, 702, 703, 704, 705, 712, 713, 721, 723)))
        addAll(mapPoints("modern_since_poetry", listOf(722, 724, 725, 726, 921)))

        addAll(mapPoints("modern_taiwan", listOf(956, 957)))
        addAll(mapPoints("modern_hongkong", listOf(958, 959)))

        addAll(mapPoints("modern_first_prose", listOf(994)))
        addAll(mapPoints("modern_first_poetry", listOf(995)))
        addAll(mapPoints("modern_third_war", listOf(996, 997, 998, 999)))
        addAll(mapPoints("modern_seventeen_rural", listOf(1000)))
        addAll(mapPoints("modern_diaspora", listOf(1001)))
        addAll(mapPoints("modern_since_poetry", listOf(1002, 1003, 1004, 1009, 1010)))
        addAll(mapPoints("modern_since_history", listOf(1005, 1006, 1011, 1012, 1013)))
        addAll(mapPoints("modern_since_women", listOf(1007)))
        addAll(mapPoints("modern_since_new_century", listOf(1008)))
        addAll(mapPoints("modern_since_history", listOf(1014, 1015, 1016, 1018, 1019, 1021)))
        addAll(mapPoints("modern_since_realism", listOf(1017)))
        addAll(mapPoints("modern_new_root", listOf(1020)))
        addAll(mapPoints("modern_new_avant", listOf(1022, 1023)))

        addAll(mapPoints("modern_history_late_qing", listOf(1024)))
        addAll(mapPoints("modern_first_poetry", listOf(1025, 1026, 1027, 1036, 1037)))
        addAll(mapPoints("modern_first_movements", listOf(1028, 1029, 1030)))
        addAll(mapPoints("modern_first_prose", listOf(1031)))
        addAll(mapPoints("modern_second_styles", listOf(1032, 1033, 1034)))
        addAll(mapPoints("modern_third_war", listOf(1035, 1038, 1039, 1040, 1041)))
        addAll(mapPoints("modern_taiwan", listOf(1042, 1043)))
        addAll(mapPoints("modern_diaspora", listOf(1044)))

        addAll(mapPoints("modern_seventeen_system", listOf(1045, 1048)))
        addAll(mapPoints("modern_seventeen_other", listOf(1046, 1049, 1051)))
        addAll(mapPoints("modern_seventeen_poetry", listOf(1047)))
        addAll(mapPoints("modern_new_restore", listOf(1050)))
        addAll(mapPoints("modern_cultural_underground", listOf(1052)))
        addAll(mapPoints("modern_new_poetry", listOf(1053, 1054, 1055, 1056, 1057)))
        addAll(mapPoints("modern_since_history", listOf(1058, 1059, 1063)))
        addAll(mapPoints("modern_since_popular", listOf(1060, 1061, 1064)))
        addAll(mapPoints("modern_since_realism", listOf(1062)))
        addAll(mapPoints("modern_since_women", listOf(1065, 1066)))
        addAll(mapPoints("modern_since_new_century", listOf(1067, 1068)))
    }

    val assignments: Map<String, String> = assignmentPairs.toMap()

    /** 返回导入前必须解决的框架数据问题。空列表表示通过。 */
    fun validate(pointIds: Set<String>): List<String> =
        KnowledgeFrameworkValidator.validate(nodes, assignmentPairs, assignments, pointIds)

    private fun mapPoints(nodeId: String, vararg groups: Iterable<Int>): List<Pair<String, String>> =
        groups.flatMap { group -> group.map { "kp_${it.toString().padStart(5, '0')}" to nodeId } }
}

/**
 * 已注册的科目框架入口。
 *
 * 每个已完成审核的科目都在这里注册。导入器不再依赖某一个具体 object 的特殊判断，
 * 章节导入、校验、旧节点清理和浏览链路由同一套逻辑复用。
 */
data class RegisteredKnowledgeFramework(
    val subjectCode: String,
    val subjectName: String,
    val nodes: List<FrameworkNode>,
    val assignments: Map<String, String>,
    val validate: (Set<String>) -> List<String>,
    /** 旧版按时段生成的节点；只清理没有任何知识点引用的节点。 */
    val legacyChapterIds: List<String> = emptyList(),
)

object KnowledgeFrameworkRegistry {

    val definitions: List<RegisteredKnowledgeFramework> = listOf(
        RegisteredKnowledgeFramework(
            subjectCode = KnowledgeFramework.SUBJECT_CODE,
            subjectName = KnowledgeFramework.SUBJECT_NAME,
            nodes = KnowledgeFramework.nodes,
            assignments = KnowledgeFramework.assignments,
            validate = KnowledgeFramework::validate,
            legacyChapterIds = (0..6).map { "chapter_modern_$it" },
        ),
        RegisteredKnowledgeFramework(
            subjectCode = AncientKnowledgeFramework.SUBJECT_CODE,
            subjectName = AncientKnowledgeFramework.SUBJECT_NAME,
            nodes = AncientKnowledgeFramework.nodes,
            assignments = AncientKnowledgeFramework.assignments,
            validate = AncientKnowledgeFramework::validate,
            legacyChapterIds = (0..7).map { "chapter_ancient_$it" },
        ),
        RegisteredKnowledgeFramework(
            subjectCode = ForeignKnowledgeFramework.SUBJECT_CODE,
            subjectName = ForeignKnowledgeFramework.SUBJECT_NAME,
            nodes = ForeignKnowledgeFramework.nodes,
            assignments = ForeignKnowledgeFramework.assignments,
            validate = ForeignKnowledgeFramework::validate,
            legacyChapterIds = (0..7).map { "chapter_foreign_$it" },
        ),
        RegisteredKnowledgeFramework(
            subjectCode = TheoryKnowledgeFramework.SUBJECT_CODE,
            subjectName = TheoryKnowledgeFramework.SUBJECT_NAME,
            nodes = TheoryKnowledgeFramework.nodes,
            assignments = TheoryKnowledgeFramework.assignments,
            validate = TheoryKnowledgeFramework::validate,
            legacyChapterIds = (0..5).map { "chapter_theory_$it" },
        ),
    )

    private val byCode = definitions.associateBy { it.subjectCode }
    private val byName = definitions.associateBy { it.subjectName }

    fun find(subjectCode: String, subjectName: String): RegisteredKnowledgeFramework? =
        byCode[subjectCode]?.takeIf { it.subjectName == subjectName }
            ?: byName[subjectName]?.takeIf { it.subjectCode == subjectCode }
}

data class FrameworkNode(
    val id: String,
    val parentId: String?,
    val title: String,
    val sortOrder: Int,
)
