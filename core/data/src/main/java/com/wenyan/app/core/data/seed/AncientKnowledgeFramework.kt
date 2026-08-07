package com.wenyan.app.core.data.seed

/**
 * 中国古代文学的显式知识框架。
 *
 * 归类依据是知识点的标题、内容语义和古代文学史的教材编排，而不是把旧的
 * 关键词命中结果直接搬进章节树。每个知识点只保留一个主要归属，跨时期或
 * 跨体裁的联系交给 tags/relatedIds 表达，避免在框架中重复出现。
 *
 * 这份配置覆盖中国古代文学的 498 个知识点。节点 ID 是稳定的数据标识，发布
 * 后不因标题润色而复用；这样重新整理章节时不会破坏用户的浏览状态或学习记录。
 */
object AncientKnowledgeFramework {

    const val SUBJECT_CODE = "ancient"
    const val SUBJECT_NAME = "中国古代文学"

    /** 相对于科目根章节的显式框架节点。 */
    val nodes: List<FrameworkNode> = listOf(
        FrameworkNode("ancient_research", null, "文学史通论与研究方法", 1),

        FrameworkNode("ancient_pre_qin", null, "先秦文学", 2),
        FrameworkNode("ancient_pre_qin_myth", "ancient_pre_qin", "神话与文学起源", 1),
        FrameworkNode("ancient_pre_qin_shijing", "ancient_pre_qin", "《诗经》与周代诗歌", 2),
        FrameworkNode("ancient_pre_qin_history", "ancient_pre_qin", "先秦历史散文", 3),
        FrameworkNode("ancient_pre_qin_thinkers", "ancient_pre_qin", "诸子散文", 4),
        FrameworkNode("ancient_pre_qin_chuci", "ancient_pre_qin", "楚辞与屈原", 5),

        FrameworkNode("ancient_qin_han", null, "秦汉文学", 3),
        FrameworkNode("ancient_qin_han_overview", "ancient_qin_han", "秦汉文学概况与作家群", 1),
        FrameworkNode("ancient_qin_han_fu", "ancient_qin_han", "汉赋与辞赋传统", 2),
        FrameworkNode("ancient_qin_han_history", "ancient_qin_han", "史传与汉代散文", 3),
        FrameworkNode("ancient_qin_han_yuefu", "ancient_qin_han", "乐府诗与叙事传统", 4),
        FrameworkNode("ancient_qin_han_wuyan", "ancient_qin_han", "东汉文人诗与五言诗", 5),

        FrameworkNode("ancient_wei_jin", null, "魏晋南北朝文学", 4),
        FrameworkNode("ancient_wei_jin_social", "ancient_wei_jin", "时代思潮与文学转型", 1),
        FrameworkNode("ancient_wei_jin_jianan", "ancient_wei_jin", "建安与正始文学", 2),
        FrameworkNode("ancient_wei_jin_poetry", "ancient_wei_jin", "魏晋南北朝诗歌", 3),
        FrameworkNode("ancient_wei_jin_fiction", "ancient_wei_jin", "志怪与志人小说", 4),
        FrameworkNode("ancient_wei_jin_theory", "ancient_wei_jin", "文学理论与批评", 5),
        FrameworkNode("ancient_wei_jin_buddhism", "ancient_wei_jin", "佛教传播与文学影响", 6),

        FrameworkNode("ancient_sui_tang", null, "隋唐五代文学", 5),
        FrameworkNode("ancient_sui_tang_initial", "ancient_sui_tang", "初唐与盛唐诗歌", 1),
        FrameworkNode("ancient_sui_tang_middle_late", "ancient_sui_tang", "中晚唐诗歌与古文运动", 2),
        FrameworkNode("ancient_sui_tang_fiction", "ancient_sui_tang", "唐传奇与敦煌变文", 3),
        FrameworkNode("ancient_sui_tang_ci", "ancient_sui_tang", "晚唐五代词", 4),

        FrameworkNode("ancient_song_liao_jin", null, "宋辽金文学", 6),
        FrameworkNode("ancient_song_overview", "ancient_song_liao_jin", "宋代文学的文化背景与总体成就", 1),
        FrameworkNode("ancient_song_early_poetry", "ancient_song_liao_jin", "北宋诗文革新与诗歌", 2),
        FrameworkNode("ancient_song_ci", "ancient_song_liao_jin", "宋词的演进与词人", 3),
        FrameworkNode("ancient_song_sushi", "ancient_song_liao_jin", "苏轼的文学成就", 4),
        FrameworkNode("ancient_song_southern", "ancient_song_liao_jin", "南宋诗词与江西诗派", 5),
        FrameworkNode("ancient_song_fiction", "ancient_song_liao_jin", "话本与说话艺术", 6),
        FrameworkNode("ancient_liao_jin", "ancient_song_liao_jin", "辽金文学", 7),

        FrameworkNode("ancient_yuan", null, "元代文学", 7),
        FrameworkNode("ancient_yuan_overview", "ancient_yuan", "元代社会与文学概况", 1),
        FrameworkNode("ancient_yuan_huaben", "ancient_yuan", "说话、话本与诸宫调", 2),
        FrameworkNode("ancient_yuan_zaju", "ancient_yuan", "元杂剧与关汉卿", 3),
        FrameworkNode("ancient_yuan_xixiang", "ancient_yuan", "《西厢记》与王实甫", 4),
        FrameworkNode("ancient_yuan_other_zaju", "ancient_yuan", "其他杂剧作家与作品", 5),
        FrameworkNode("ancient_yuan_nanxi", "ancient_yuan", "南戏与《琵琶记》", 6),
        FrameworkNode("ancient_yuan_sanqu", "ancient_yuan", "散曲", 7),
        FrameworkNode("ancient_yuan_poetry", "ancient_yuan", "元代诗文", 8),

        FrameworkNode("ancient_ming", null, "明代文学", 8),
        FrameworkNode("ancient_ming_overview", "ancient_ming", "明代文学概况", 1),
        FrameworkNode("ancient_ming_fiction", "ancient_ming", "历史演义与世情小说", 2),
        FrameworkNode("ancient_ming_drama", "ancient_ming", "明代戏曲与汤显祖", 3),
        FrameworkNode("ancient_ming_movements", "ancient_ming", "文学复古与晚明思潮", 4),
        FrameworkNode("ancient_ming_poetry", "ancient_ming", "明代诗文、散曲与民歌", 5),

        FrameworkNode("ancient_qing", null, "清代文学", 9),
        FrameworkNode("ancient_qing_poetry_prose", "ancient_qing", "清代诗文与诗学流派", 1),
        FrameworkNode("ancient_qing_ci", "ancient_qing", "清词与词学", 2),
        FrameworkNode("ancient_qing_drama", "ancient_qing", "清代戏曲", 3),
        FrameworkNode("ancient_qing_fiction", "ancient_qing", "清代小说", 4),

        FrameworkNode("ancient_near_modern", null, "近代文学", 10),
        FrameworkNode("ancient_near_modern_overview", "ancient_near_modern", "近代文学转型概况", 1),
        FrameworkNode("ancient_near_modern_gong", "ancient_near_modern", "龚自珍与近代思想转型", 2),
        FrameworkNode("ancient_near_modern_poetry_prose", "ancient_near_modern", "诗界革命与近代散文", 3),
        FrameworkNode("ancient_near_modern_fiction", "ancient_near_modern", "近代小说", 4),
        FrameworkNode("ancient_near_modern_drama", "ancient_near_modern", "近代戏曲与戏剧改良", 5),
    )

    /**
     * 知识点 ID → 框架节点 ID。
     *
     * 数字区间仅用于减少重复书写；[validate] 会在导入前检查一对一完整性，
     * 任何新增、删除或误归类都会让导入失败而不会静默落到根章节。
     */
    private val assignmentPairs: List<Pair<String, String>> = buildList {
        addAll(mapPoints("ancient_research", 139..148, 219..220, 230..232))

        addAll(mapPoints("ancient_pre_qin_myth", 1..5, 85..86, 149..152))
        addAll(mapPoints("ancient_pre_qin_shijing", 6..9, 87..87, 153..158, 221..222))
        addAll(mapPoints("ancient_pre_qin_history", 10..12, 88..88, 97..98, 159..161, 223..223))
        addAll(mapPoints("ancient_pre_qin_history", 963..964))
        addAll(mapPoints("ancient_pre_qin_history", 984..984))
        addAll(mapPoints("ancient_pre_qin_thinkers", 13..13, 99..100, 162..166, 224..224, 961..962))
        addAll(mapPoints("ancient_pre_qin_thinkers", 985..985))
        addAll(mapPoints("ancient_pre_qin_chuci", 14..17, 101..101, 167..170, 225..225))

        addAll(mapPoints("ancient_qin_han_overview", 89..89, 171..177))
        addAll(mapPoints("ancient_qin_han_fu", 20..20, 91..91, 178..184, 195..202, 226..226))
        addAll(mapPoints("ancient_qin_han_history", 18..19, 90..90, 103..103, 185..188, 203..207, 227..227))
        addAll(mapPoints("ancient_qin_han_yuefu", 21..23, 102..102, 189..194, 208..209, 228..229))
        addAll(mapPoints("ancient_qin_han_wuyan", 210..218))

        addAll(mapPoints("ancient_wei_jin_social", 233..233, 237..247, 269..269))
        addAll(mapPoints("ancient_wei_jin_jianan", 24..25, 92..93, 104..106, 263..268, 270..282))
        addAll(mapPoints("ancient_wei_jin_poetry", 26..31, 107..108, 912..912, 965..965))
        addAll(mapPoints("ancient_wei_jin_social", 966..966))
        addAll(mapPoints("ancient_wei_jin_fiction", 94..95))
        addAll(mapPoints("ancient_wei_jin_theory", 32..32, 109..109, 234..236, 248..253))
        addAll(mapPoints("ancient_wei_jin_buddhism", 254..262))

        addAll(mapPoints("ancient_sui_tang_initial", 33..37, 96..96, 110..112, 911..911, 967..968))
        addAll(mapPoints("ancient_sui_tang_middle_late", 38..41, 113..115, 969..971))
        addAll(mapPoints("ancient_sui_tang_middle_late", 986..987))
        addAll(mapPoints("ancient_sui_tang_fiction", 972..972))
        addAll(mapPoints("ancient_sui_tang_fiction", 42..42, 913..913, 960..960))
        addAll(mapPoints("ancient_sui_tang_ci", 43..43, 116..117))

        addAll(mapPoints("ancient_song_overview", 283..287))
        addAll(mapPoints("ancient_song_early_poetry", 44..44, 119..120, 288..292, 296..298))
        addAll(mapPoints("ancient_song_ci", 45..45, 48..48, 118..118, 123..124, 126..127, 293..295))
        addAll(mapPoints("ancient_song_sushi", 46..46, 121..122, 299..303))
        addAll(mapPoints("ancient_song_southern", 47..47, 49..50, 125..125, 128..128, 304..310, 973..976, 988..989))
        addAll(mapPoints("ancient_song_fiction", 51..51))
        addAll(mapPoints("ancient_liao_jin", 52..52, 130..130, 311..311, 990..990))

        addAll(mapPoints("ancient_yuan_overview", 312..313, 315..315))
        addAll(mapPoints("ancient_yuan_huaben", 316..318))
        addAll(mapPoints("ancient_yuan_zaju", 53..53, 59..59, 319..322))
        addAll(mapPoints("ancient_yuan_xixiang", 54..54, 323..325))
        addAll(mapPoints("ancient_yuan_other_zaju", 55..55, 129..129, 326..338))
        addAll(mapPoints("ancient_yuan_nanxi", 57..58, 339..343))
        addAll(mapPoints("ancient_yuan_sanqu", 56..56, 314..314, 344..354))
        addAll(mapPoints("ancient_yuan_poetry", 355..363, 991..991))

        addAll(mapPoints("ancient_ming_overview", 60..60))
        addAll(mapPoints("ancient_ming_fiction", 61..63, 65..66, 364..368, 377..379, 977..977))
        addAll(mapPoints("ancient_ming_drama", 64..64, 131..131, 374..376, 978..978))
        addAll(mapPoints("ancient_ming_movements", 67..70, 380..380))
        addAll(mapPoints("ancient_ming_poetry", 71..71, 132..132, 369..373, 381..381, 992..992))

        addAll(mapPoints("ancient_qing_poetry_prose", 72..72, 74..74, 82..82, 133..135, 382..388, 394..396, 418..423, 980..980))
        addAll(mapPoints("ancient_qing_ci", 73..73, 389..393, 424..426))
        addAll(mapPoints("ancient_qing_drama", 77..78, 397..403, 428..430))
        addAll(mapPoints("ancient_qing_fiction", 75..76, 79..81, 83..83, 404..417, 427..427, 914..914, 979..979))

        addAll(mapPoints("ancient_near_modern_overview", 84..84, 136..136, 431..431, 983..983))
        addAll(mapPoints("ancient_near_modern_gong", 432..434))
        addAll(mapPoints("ancient_near_modern_poetry_prose", 137..138, 435..438, 444..454, 981..981))
        addAll(mapPoints("ancient_near_modern_fiction", 439..441, 455..459, 982..982, 993..993))
        addAll(mapPoints("ancient_near_modern_drama", 442..443, 460..460))
    }

    val assignments: Map<String, String> = assignmentPairs.toMap()

    /** 返回导入前必须解决的框架数据问题。空列表表示通过。 */
    fun validate(pointIds: Set<String>): List<String> =
        KnowledgeFrameworkValidator.validate(nodes, assignmentPairs, assignments, pointIds)

    private fun mapPoints(nodeId: String, vararg groups: Iterable<Int>): List<Pair<String, String>> =
        groups.flatMap { group -> group.map { "kp_${it.toString().padStart(5, '0')}" to nodeId } }
}
