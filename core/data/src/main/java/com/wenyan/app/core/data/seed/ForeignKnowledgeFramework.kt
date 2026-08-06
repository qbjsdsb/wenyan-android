package com.wenyan.app.core.data.seed

/**
 * 外国文学的显式知识框架。
 *
 * 这一科同时包含文学史分期、文学思潮和作家作品专题，不能简单按标题的首个
 * 关键词分组。框架先按历史阶段建立主路径，再在阶段下按文学思潮、地域和作家
 * 作品细分；每个知识点只保留一个主要归属，交叉关系继续由 tags/relatedIds 表达。
 *
 * 这份配置覆盖外国文学的 124 个知识点，并特别处理了古希腊悲剧、自然主义、
 * 象征主义、英语现代主义以及补充知识点等容易被顺序规则误判的边界。
 */
object ForeignKnowledgeFramework {

    const val SUBJECT_CODE = "foreign"
    const val SUBJECT_NAME = "外国文学"

    /** 相对于科目根章节的显式框架节点。 */
    val nodes: List<FrameworkNode> = listOf(
        FrameworkNode("foreign_overview", null, "欧美文学总体论与比较视野", 1),

        FrameworkNode("foreign_ancient", null, "古希腊罗马文学", 2),
        FrameworkNode("foreign_ancient_myth", "foreign_ancient", "神话与人本意识", 1),
        FrameworkNode("foreign_ancient_epic", "foreign_ancient", "荷马史诗", 2),
        FrameworkNode("foreign_ancient_tragedy", "foreign_ancient", "希腊悲剧", 3),
        FrameworkNode("foreign_ancient_comedy_rome", "foreign_ancient", "希腊喜剧与古罗马文学", 4),

        FrameworkNode("foreign_medieval", null, "欧洲中世纪文学", 3),
        FrameworkNode("foreign_medieval_dante", "foreign_medieval", "但丁与中世纪文学转型", 1),
        FrameworkNode("foreign_medieval_overview", "foreign_medieval", "中世纪文学总体概况", 2),

        FrameworkNode("foreign_renaissance", null, "文艺复兴与人文主义文学", 4),
        FrameworkNode("foreign_renaissance_overview", "foreign_renaissance", "人文主义文学概况", 1),
        FrameworkNode("foreign_renaissance_boccaccio", "foreign_renaissance", "薄伽丘与《十日谈》", 2),
        FrameworkNode("foreign_renaissance_cervantes", "foreign_renaissance", "塞万提斯与《堂吉诃德》", 3),
        FrameworkNode("foreign_renaissance_shakespeare", "foreign_renaissance", "莎士比亚戏剧", 4),

        FrameworkNode("foreign_classicism", null, "17世纪古典主义文学", 5),
        FrameworkNode("foreign_classicism_moliere", "foreign_classicism", "莫里哀与古典主义喜剧", 1),

        FrameworkNode("foreign_enlightenment", null, "18世纪启蒙文学", 6),
        FrameworkNode("foreign_enlightenment_overview", "foreign_enlightenment", "启蒙文学基本观念", 1),
        FrameworkNode("foreign_enlightenment_rousseau", "foreign_enlightenment", "卢梭与启蒙感伤主义", 2),
        FrameworkNode("foreign_enlightenment_goethe", "foreign_enlightenment", "歌德与德国文学", 3),

        FrameworkNode("foreign_romanticism", null, "19世纪浪漫主义文学", 7),
        FrameworkNode("foreign_romanticism_overview", "foreign_romanticism", "浪漫主义文学概况", 1),
        FrameworkNode("foreign_romanticism_wordsworth", "foreign_romanticism", "华兹华斯与湖畔派", 2),
        FrameworkNode("foreign_romanticism_byron", "foreign_romanticism", "拜伦与拜伦式英雄", 3),
        FrameworkNode("foreign_romanticism_hugo_dumas", "foreign_romanticism", "雨果、大仲马与通俗小说", 4),

        FrameworkNode("foreign_realism", null, "19世纪现实主义与自然主义", 8),
        FrameworkNode("foreign_realism_overview", "foreign_realism", "现实主义文学基本特征", 1),
        FrameworkNode("foreign_realism_france", "foreign_realism", "法国现实主义小说", 2),
        FrameworkNode("foreign_realism_britain", "foreign_realism", "英国现实主义小说", 3),
        FrameworkNode("foreign_realism_russia", "foreign_realism", "俄国现实主义小说", 4),
        FrameworkNode("foreign_realism_drama", "foreign_realism", "现实主义戏剧", 5),
        FrameworkNode("foreign_realism_naturalism", "foreign_realism", "自然主义文学", 6),
        FrameworkNode("foreign_realism_america", "foreign_realism", "美国现实主义文学", 7),

        FrameworkNode("foreign_late_nineteenth", null, "19世纪后期文学与现代转型", 9),
        FrameworkNode("foreign_late19_context", "foreign_late_nineteenth", "社会背景与文学转向", 1),
        FrameworkNode("foreign_late19_hardy", "foreign_late_nineteenth", "哈代与悲剧小说", 2),
        FrameworkNode("foreign_late19_tolstoy", "foreign_late_nineteenth", "托尔斯泰与道德现实主义", 3),
        FrameworkNode("foreign_late19_ibsen", "foreign_late_nineteenth", "易卜生与社会问题剧", 4),
        FrameworkNode("foreign_late19_zola", "foreign_late_nineteenth", "左拉与自然主义实验", 5),
        FrameworkNode("foreign_late19_twain", "foreign_late_nineteenth", "马克·吐温与讽刺现实主义", 6),
        FrameworkNode("foreign_late19_natsume", "foreign_late_nineteenth", "夏目漱石与日本近代文学", 7),
        FrameworkNode("foreign_late19_aesthetic_symbolist", "foreign_late_nineteenth", "唯美主义与象征主义", 8),

        FrameworkNode("foreign_modernism", null, "20世纪现代主义文学", 10),
        FrameworkNode("foreign_modern_context", "foreign_modernism", "现代主义的历史背景与思潮", 1),
        FrameworkNode("foreign_modern_soviet", "foreign_modernism", "苏联文学与高尔基、肖洛霍夫", 2),
        FrameworkNode("foreign_modern_french", "foreign_modernism", "法国意识流小说", 3),
        FrameworkNode("foreign_modern_english", "foreign_modernism", "英语现代主义诗歌与小说", 4),
        FrameworkNode("foreign_modern_german", "foreign_modernism", "卡夫卡与表现主义小说", 5),
        FrameworkNode("foreign_modern_american_drama", "foreign_modernism", "美国表现主义戏剧", 6),
        FrameworkNode("foreign_modern_faulkner", "foreign_modernism", "福克纳与南方文学", 7),
        FrameworkNode("foreign_modern_hemingway", "foreign_modernism", "海明威与迷惘的一代", 8),
        FrameworkNode("foreign_modern_asia", "foreign_modernism", "亚洲现代文学", 9),

        FrameworkNode("foreign_late_twentieth", null, "20世纪下半期多元文学", 11),
        FrameworkNode("foreign_late20_trends", "foreign_late_twentieth", "后现代主义与多元思潮", 1),
        FrameworkNode("foreign_late20_region", "foreign_late_twentieth", "欧美与拉美文学新变", 2),
        FrameworkNode("foreign_late20_sartre", "foreign_late_twentieth", "萨特与存在主义文学", 3),
        FrameworkNode("foreign_late20_beckett", "foreign_late_twentieth", "贝克特与荒诞派戏剧", 4),
        FrameworkNode("foreign_late20_naipaul", "foreign_late_twentieth", "奈保尔与后殖民文学", 5),
        FrameworkNode("foreign_late20_aitmatov", "foreign_late_twentieth", "艾特玛托夫的跨文化写作", 6),
        FrameworkNode("foreign_late20_bellow", "foreign_late_twentieth", "索尔·贝娄与犹太文学", 7),
        FrameworkNode("foreign_late20_morrison", "foreign_late_twentieth", "莫里森与黑人文学", 8),
        FrameworkNode("foreign_late20_marquez", "foreign_late_twentieth", "马尔克斯与魔幻现实主义", 9),
        FrameworkNode("foreign_late20_borges", "foreign_late_twentieth", "博尔赫斯与迷宫叙事", 10),
        FrameworkNode("foreign_late20_oe", "foreign_late_twentieth", "大江健三郎与边缘文化", 11),
        FrameworkNode("foreign_late20_mahfouz", "foreign_late_twentieth", "马哈福兹与阿拉伯文学", 12),
        FrameworkNode("foreign_late20_coetzee", "foreign_late_twentieth", "库切与后殖民书写", 13),
    )

    /**
     * 知识点 ID → 框架节点 ID。
     *
     * 数字区间仅用于减少重复书写；[validate] 会在导入前检查一对一完整性，
     * 任何新增、删除或误归类都会让导入失败而不会静默落到根章节。
     */
    private val assignmentPairs: List<Pair<String, String>> = buildList {
        addAll(mapPoints("foreign_late19_context", 461..462))
        addAll(mapPoints("foreign_late19_hardy", 463..465))
        addAll(mapPoints("foreign_late19_tolstoy", 466..468))
        addAll(mapPoints("foreign_late19_ibsen", 469..470))
        addAll(mapPoints("foreign_late19_zola", 471..472))
        addAll(mapPoints("foreign_late19_twain", 473..474))
        addAll(mapPoints("foreign_late19_natsume", 475..476))
        addAll(mapPoints("foreign_late19_aesthetic_symbolist", 575..575, 926..926))

        addAll(mapPoints("foreign_modern_context", 477..477))
        addAll(mapPoints("foreign_modern_soviet", 478..480))
        addAll(mapPoints("foreign_modern_french", 481..481))
        addAll(mapPoints("foreign_modern_english", 482..482, 923..925))
        addAll(mapPoints("foreign_modern_german", 483..484))
        addAll(mapPoints("foreign_modern_american_drama", 485..485))
        addAll(mapPoints("foreign_modern_faulkner", 486..487))
        addAll(mapPoints("foreign_modern_hemingway", 488..490))
        addAll(mapPoints("foreign_modern_asia", 491..492))

        addAll(mapPoints("foreign_late20_trends", 493..498))
        addAll(mapPoints("foreign_late20_region", 499..502))
        addAll(mapPoints("foreign_late20_sartre", 503..505))
        addAll(mapPoints("foreign_late20_beckett", 506..507))
        addAll(mapPoints("foreign_late20_naipaul", 508..509))
        addAll(mapPoints("foreign_late20_aitmatov", 510..511))
        addAll(mapPoints("foreign_late20_bellow", 512..513))
        addAll(mapPoints("foreign_late20_morrison", 514..515))
        addAll(mapPoints("foreign_late20_marquez", 516..517))
        addAll(mapPoints("foreign_late20_borges", 518..518))
        addAll(mapPoints("foreign_late20_oe", 519..520))
        addAll(mapPoints("foreign_late20_mahfouz", 521..522))
        addAll(mapPoints("foreign_late20_coetzee", 523..524))

        addAll(mapPoints("foreign_ancient_myth", 525..525))
        addAll(mapPoints("foreign_ancient_epic", 526..527))
        addAll(mapPoints("foreign_ancient_tragedy", 528..530, 577..577))
        addAll(mapPoints("foreign_ancient_comedy_rome", 531..533))

        addAll(mapPoints("foreign_medieval_dante", 534..535))
        addAll(mapPoints("foreign_medieval_overview", 536..536))

        addAll(mapPoints("foreign_renaissance_overview", 537..537))
        addAll(mapPoints("foreign_renaissance_boccaccio", 538..538))
        addAll(mapPoints("foreign_renaissance_cervantes", 539..539))
        addAll(mapPoints("foreign_renaissance_shakespeare", 540..541))

        addAll(mapPoints("foreign_classicism_moliere", 542..543))

        addAll(mapPoints("foreign_enlightenment_overview", 544..544))
        addAll(mapPoints("foreign_enlightenment_rousseau", 545..545))
        addAll(mapPoints("foreign_enlightenment_goethe", 546..547))

        addAll(mapPoints("foreign_romanticism_overview", 548..548))
        addAll(mapPoints("foreign_romanticism_wordsworth", 549..549))
        addAll(mapPoints("foreign_romanticism_byron", 550..550))
        addAll(mapPoints("foreign_romanticism_hugo_dumas", 551..552))

        addAll(mapPoints("foreign_realism_overview", 553..553))
        addAll(mapPoints("foreign_realism_france", 554..557))
        addAll(mapPoints("foreign_realism_britain", 558..560, 927..927))
        addAll(mapPoints("foreign_realism_russia", 561..568, 928..928))
        addAll(mapPoints("foreign_realism_drama", 569..570))
        addAll(mapPoints("foreign_realism_naturalism", 571..574))
        addAll(mapPoints("foreign_realism_america", 576..576))

        addAll(mapPoints("foreign_overview", 938..938))
    }

    val assignments: Map<String, String> = assignmentPairs.toMap()

    /** 返回导入前必须解决的框架数据问题。空列表表示通过。 */
    fun validate(pointIds: Set<String>): List<String> =
        KnowledgeFrameworkValidator.validate(nodes, assignmentPairs, assignments, pointIds)

    private fun mapPoints(nodeId: String, vararg groups: Iterable<Int>): List<Pair<String, String>> =
        groups.flatMap { group -> group.map { "kp_${it.toString().padStart(5, '0')}" to nodeId } }
}
