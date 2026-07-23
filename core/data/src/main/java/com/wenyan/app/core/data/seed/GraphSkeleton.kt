package com.wenyan.app.core.data.seed

import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphEdgeType
import com.wenyan.app.core.database.entity.GraphNodeEntity
import com.wenyan.app.core.database.entity.GraphNodeType

/**
 * 知识图谱骨架预置数据。
 *
 * 对应 Spec 第 307-342 行"功能性知识图谱"要求，预置三类骨架：
 *
 * 1. 南师大现当代文学考点骨架（[SOUTHERN_NORMAL_AUTHORS] + [SOUTHERN_NORMAL_RELATIONS]）
 *    - 13 位作家节点：鲁迅 / 周作人 / 茅盾 / 沈从文 / 张爱玲 / 赵树理 / 路遥
 *      + 巴金 / 老舍 / 曹禺 / 郭沫若 / 钱钟书 / 艾青
 *    - 作家间关系：同时期 / 对比 / 受影响 / 先于
 *
 * 2. 体裁×时段二维矩阵骨架（[GENRE_PERIOD_MATRIX] + [GENRE_PERIOD_RELATIONS]）
 *    - 体裁维度：小说 / 诗歌 / 散文 / 戏剧
 *    - 时段维度：现代文学 / 当代文学
 *    - 关系：体裁 BELONGS_TO 时段（构成 4×2 矩阵）
 *
 * 3. 文学流派骨架（[LITERARY_SCHOOLS] + [LITERARY_SCHOOL_RELATIONS]）
 *    - v0.7.3 新增：覆盖现当代文学核心流派与文学社团
 *    - 14 个流派/社团节点，关联真实知识点 ID
 *
 * 所有节点 ID 使用固定 UUID 字符串，保证多次导入不会重复（DAO 使用 REPLACE 策略）。
 *
 * v0.7.3 更新：节点数从 13 扩充至 40+，大部分节点关联真实知识点 ID（relatedPointId），
 * 用户点击图谱节点可跳转到对应知识点详情页。
 */
object GraphSkeleton {

    // ==================== 常量定义 ====================

    /**
     * 现当代文学科目 ID（必须与 seed_data.json 中 subjects 表 modern 科目的 id 一致）。
     *
     * v0.7.2 修复：原值 "subject-modern-contemporary-literature" 与 seed_data.json 的
     * "subj_02" 不匹配，导致 GraphNodeEntity 的 subject_id FK 约束失败，
     * 整个 withTransaction 回滚，909 条知识点全部丢失（详见 SESSION_LOG v0.7.2）。
     */
    private const val SUBJECT_ID = "subj_02"

    /** 节点颜色：作家（粉色） */
    private val COLOR_AUTHOR = 0xFFE91E63.toInt()

    /** 节点颜色：体裁（蓝色） */
    private val COLOR_GENRE = 0xFF2196F3.toInt()

    /** 节点颜色：时段（绿色） */
    private val COLOR_PERIOD = 0xFF4CAF50.toInt()

    /** 节点颜色：流派/社团（紫色，v0.7.3 新增） */
    private val COLOR_SCHOOL = 0xFF9C27B0.toInt()

    /** 节点颜色：作品（橙色，v0.7.3 新增） */
    private val COLOR_WORK = 0xFFFF9800.toInt()

    // 南师大作家节点 ID（原 7 位 + v0.7.3 新增 6 位 = 13 位）
    private const val ID_LUXUN = "550e8400-e29b-41d4-a716-446655440001"
    private const val ID_ZHOUZUOREN = "550e8400-e29b-41d4-a716-446655440002"
    private const val ID_MAODUN = "550e8400-e29b-41d4-a716-446655440003"
    private const val ID_SHENCONGWEN = "550e8400-e29b-41d4-a716-446655440004"
    private const val ID_ZHANGAILING = "550e8400-e29b-41d4-a716-446655440005"
    private const val ID_ZHAOSHULI = "550e8400-e29b-41d4-a716-446655440006"
    private const val ID_LUYAO = "550e8400-e29b-41d4-a716-446655440007"
    // v0.7.3 新增作家
    private const val ID_BAJIN = "550e8400-e29b-41d4-a716-446655440008"
    private const val ID_LAOSHE = "550e8400-e29b-41d4-a716-446655440009"
    private const val ID_CAOYU = "550e8400-e29b-41d4-a716-446655440010"
    private const val ID_GUOMORUO = "550e8400-e29b-41d4-a716-446655440011"
    private const val ID_QIANZHONGSHU = "550e8400-e29b-41d4-a716-446655440012"
    private const val ID_AIQING = "550e8400-e29b-41d4-a716-446655440013"

    // 体裁节点 ID
    private const val ID_GENRE_XIAOSHUO = "550e8400-e29b-41d4-a716-446655440101"
    private const val ID_GENRE_SHIGE = "550e8400-e29b-41d4-a716-446655440102"
    private const val ID_GENRE_SANWEN = "550e8400-e29b-41d4-a716-446655440103"
    private const val ID_GENRE_XIJU = "550e8400-e29b-41d4-a716-446655440104"

    // 时段节点 ID
    private const val ID_PERIOD_XIANDAI = "550e8400-e29b-41d4-a716-446655440201"
    private const val ID_PERIOD_DANGDAI = "550e8400-e29b-41d4-a716-446655440202"

    // v0.7.3 新增：文学流派/社团节点 ID
    private const val ID_SCHOOL_WENXUEGE = "550e8400-e29b-41d4-a716-446655440301"
    private const val ID_SCHOOL_CHUANGZAO = "550e8400-e29b-41d4-a716-446655440302"
    private const val ID_SCHOOL_XINYUE = "550e8400-e29b-41d4-a716-446655440303"
    private const val ID_SCHOOL_ZUOLIAN = "550e8400-e29b-41d4-a716-446655440304"
    private const val ID_SCHOOL_JINGPAI = "550e8400-e29b-41d4-a716-446655440305"
    private const val ID_SCHOOL_HAIPAI = "550e8400-e29b-41d4-a716-446655440306"
    private const val ID_SCHOOL_SHANGHEN = "550e8400-e29b-41d4-a716-446655440307"
    private const val ID_SCHOOL_FANSI = "550e8400-e29b-41d4-a716-446655440308"
    private const val ID_SCHOOL_GAIGE = "550e8400-e29b-41d4-a716-446655440309"
    private const val ID_SCHOOL_XUNGEN = "550e8400-e29b-41d4-a716-446655440310"
    private const val ID_SCHOOL_XIANFENG = "550e8400-e29b-41d4-a716-446655440311"
    private const val ID_SCHOOL_MENLONG = "550e8400-e29b-41d4-a716-446655440312"
    private const val ID_SCHOOL_XINSHIXIE = "550e8400-e29b-41d4-a716-446655440313"
    private const val ID_SCHOOL_WENYAN = "550e8400-e29b-41d4-a716-446655440314"

    // ==================== SubTask 19.3: 南师大现当代文学考点骨架 ====================

    /**
     * 南师大现当代文学考点 - 作家节点（13 位）。
     *
     * 原 7 位：鲁迅 / 周作人 / 茅盾 / 沈从文 / 张爱玲 / 赵树理 / 路遥
     * v0.7.3 新增 6 位：巴金 / 老舍 / 曹禺 / 郭沫若 / 钱钟书 / 艾青
     *
     * 每个节点含 id / type=AUTHOR / label（姓名）/ subtitle（生卒年）/ subject_id（现当代文学）。
     *
     * v0.7.3 修复：为有对应知识点的作家填充 relatedPointId，让节点可点击跳转。
     * - 鲁迅 → kp_00613（《呐喊》与新小说的奠基）
     * - 茅盾 → kp_00636（《子夜》与吴荪甫形象）
     * - 沈从文 → kp_00624（沈从文与湘西小说）
     * - 张爱玲 → kp_00625（张爱玲与'苍凉'美学）
     * - 赵树理 → kp_00641（《锻炼锻炼》与双重叙事视点）
     * - 巴金 → kp_00694（《随想录》与知识分子的自我反省）
     * - 老舍 → kp_00627（老舍与京味小说）
     * - 曹禺 → kp_00630（《雷雨》与中国现代戏剧）
     * - 郭沫若 → kp_00601（《女神》与新诗精神）
     * - 钱钟书 / 艾青 / 路遥 / 周作人：暂无直接对应知识点（relatedPointId=null），
     *   后续补充知识点时可回填。
     */
    val SOUTHERN_NORMAL_AUTHORS: List<GraphNodeEntity> = listOf(
        GraphNodeEntity(
            id = ID_LUXUN,
            type = GraphNodeType.AUTHOR.name,
            label = "鲁迅",
            subtitle = "1881-1936",
            color = COLOR_AUTHOR,
            relatedPointId = "kp_00613",
            subjectId = SUBJECT_ID,
            metadata = mapOf("originalName" to "周树人"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_ZHOUZUOREN,
            type = GraphNodeType.AUTHOR.name,
            label = "周作人",
            subtitle = "1885-1967",
            color = COLOR_AUTHOR,
            relatedPointId = null,
            subjectId = SUBJECT_ID,
            metadata = null,
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_MAODUN,
            type = GraphNodeType.AUTHOR.name,
            label = "茅盾",
            subtitle = "1896-1981",
            color = COLOR_AUTHOR,
            relatedPointId = "kp_00636",
            subjectId = SUBJECT_ID,
            metadata = mapOf("originalName" to "沈德鸿"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SHENCONGWEN,
            type = GraphNodeType.AUTHOR.name,
            label = "沈从文",
            subtitle = "1902-1988",
            color = COLOR_AUTHOR,
            relatedPointId = "kp_00624",
            subjectId = SUBJECT_ID,
            metadata = null,
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_ZHANGAILING,
            type = GraphNodeType.AUTHOR.name,
            label = "张爱玲",
            subtitle = "1920-1995",
            color = COLOR_AUTHOR,
            relatedPointId = "kp_00625",
            subjectId = SUBJECT_ID,
            metadata = mapOf("originalName" to "张瑛"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_ZHAOSHULI,
            type = GraphNodeType.AUTHOR.name,
            label = "赵树理",
            subtitle = "1906-1970",
            color = COLOR_AUTHOR,
            relatedPointId = "kp_00641",
            subjectId = SUBJECT_ID,
            metadata = null,
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_LUYAO,
            type = GraphNodeType.AUTHOR.name,
            label = "路遥",
            subtitle = "1949-1992",
            color = COLOR_AUTHOR,
            relatedPointId = null,
            subjectId = SUBJECT_ID,
            metadata = mapOf("originalName" to "王卫国"),
            prerequisites = null,
        ),
        // v0.7.3 新增作家节点
        GraphNodeEntity(
            id = ID_BAJIN,
            type = GraphNodeType.AUTHOR.name,
            label = "巴金",
            subtitle = "1904-2005",
            color = COLOR_AUTHOR,
            relatedPointId = "kp_00694",
            subjectId = SUBJECT_ID,
            metadata = mapOf("originalName" to "李尧棠"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_LAOSHE,
            type = GraphNodeType.AUTHOR.name,
            label = "老舍",
            subtitle = "1899-1966",
            color = COLOR_AUTHOR,
            relatedPointId = "kp_00627",
            subjectId = SUBJECT_ID,
            metadata = mapOf("originalName" to "舒庆春"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_CAOYU,
            type = GraphNodeType.AUTHOR.name,
            label = "曹禺",
            subtitle = "1910-1996",
            color = COLOR_AUTHOR,
            relatedPointId = "kp_00630",
            subjectId = SUBJECT_ID,
            metadata = mapOf("originalName" to "万家宝"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_GUOMORUO,
            type = GraphNodeType.AUTHOR.name,
            label = "郭沫若",
            subtitle = "1892-1978",
            color = COLOR_AUTHOR,
            relatedPointId = "kp_00601",
            subjectId = SUBJECT_ID,
            metadata = null,
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_QIANZHONGSHU,
            type = GraphNodeType.AUTHOR.name,
            label = "钱钟书",
            subtitle = "1910-1998",
            color = COLOR_AUTHOR,
            relatedPointId = null,
            subjectId = SUBJECT_ID,
            metadata = null,
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_AIQING,
            type = GraphNodeType.AUTHOR.name,
            label = "艾青",
            subtitle = "1910-1996",
            color = COLOR_AUTHOR,
            relatedPointId = null,
            subjectId = SUBJECT_ID,
            metadata = mapOf("originalName" to "蒋正涵"),
            prerequisites = null,
        ),
    )

    /**
     * 南师大现当代文学考点 - 作家关系（12 条，v0.7.3 扩充）。
     *
     * 关系类型分布：
     * - SAME_PERIOD: 鲁迅-周作人 / 茅盾-沈从文 / 周作人-茅盾 / 巴金-老舍 / 曹禺-老舍
     * - COMPARED_WITH: 茅盾-沈从文 / 巴金-茅盾 / 钱钟书-张爱玲
     * - INFLUENCED_BY: 沈从文→鲁迅 / 张爱玲→鲁迅 / 艾青→郭沫若
     * - PRECEDES: 鲁迅→茅盾 / 赵树理→路遥 / 郭沫若→艾青
     */
    val SOUTHERN_NORMAL_RELATIONS: List<GraphEdgeEntity> = listOf(
        // 鲁迅与周作人：兄弟，同为新文学运动核心人物
        GraphEdgeEntity(
            id = "edge-sn-001",
            sourceId = ID_LUXUN,
            targetId = ID_ZHOUZUOREN,
            type = GraphEdgeType.SAME_PERIOD.name,
            label = "兄弟·同时期",
        ),
        // 茅盾与沈从文：同为小说家，文学史常做对比
        GraphEdgeEntity(
            id = "edge-sn-002",
            sourceId = ID_MAODUN,
            targetId = ID_SHENCONGWEN,
            type = GraphEdgeType.COMPARED_WITH.name,
            label = "小说流派对比",
        ),
        // 茅盾与沈从文：同时期活跃
        GraphEdgeEntity(
            id = "edge-sn-003",
            sourceId = ID_MAODUN,
            targetId = ID_SHENCONGWEN,
            type = GraphEdgeType.SAME_PERIOD.name,
            label = "同时期",
        ),
        // 沈从文受鲁迅影响
        GraphEdgeEntity(
            id = "edge-sn-004",
            sourceId = ID_SHENCONGWEN,
            targetId = ID_LUXUN,
            type = GraphEdgeType.INFLUENCED_BY.name,
            label = "新文学先驱影响",
        ),
        // 张爱玲受鲁迅影响（鲁迅对国民性的批判影响后世作家）
        GraphEdgeEntity(
            id = "edge-sn-005",
            sourceId = ID_ZHANGAILING,
            targetId = ID_LUXUN,
            type = GraphEdgeType.INFLUENCED_BY.name,
            label = "文学精神传承",
        ),
        // 周作人与茅盾：同时期
        GraphEdgeEntity(
            id = "edge-sn-006",
            sourceId = ID_ZHOUZUOREN,
            targetId = ID_MAODUN,
            type = GraphEdgeType.SAME_PERIOD.name,
            label = "同时期",
        ),
        // 鲁迅先于茅盾（文学活动时间）
        GraphEdgeEntity(
            id = "edge-sn-007",
            sourceId = ID_LUXUN,
            targetId = ID_MAODUN,
            type = GraphEdgeType.PRECEDES.name,
            label = "文学活动先于",
        ),
        // 赵树理先于路遥（赵树理代表解放区文学，路遥代表新时期文学）
        GraphEdgeEntity(
            id = "edge-sn-008",
            sourceId = ID_ZHAOSHULI,
            targetId = ID_LUYAO,
            type = GraphEdgeType.PRECEDES.name,
            label = "文学分期先后",
        ),
        // v0.7.3 新增关系
        // 巴金与老舍：同时期，同为现代小说大家
        GraphEdgeEntity(
            id = "edge-sn-009",
            sourceId = ID_BAJIN,
            targetId = ID_LAOSHE,
            type = GraphEdgeType.SAME_PERIOD.name,
            label = "同时期",
        ),
        // 巴金与茅盾：常做对比（同为长篇小说大家）
        GraphEdgeEntity(
            id = "edge-sn-010",
            sourceId = ID_BAJIN,
            targetId = ID_MAODUN,
            type = GraphEdgeType.COMPARED_WITH.name,
            label = "长篇小说对比",
        ),
        // 曹禺与老舍：同时期，戏剧与小说互参
        GraphEdgeEntity(
            id = "edge-sn-011",
            sourceId = ID_CAOYU,
            targetId = ID_LAOSHE,
            type = GraphEdgeType.SAME_PERIOD.name,
            label = "同时期",
        ),
        // 钱钟书与张爱玲：同时期，文人小说对比
        GraphEdgeEntity(
            id = "edge-sn-012",
            sourceId = ID_QIANZHONGSHU,
            targetId = ID_ZHANGAILING,
            type = GraphEdgeType.COMPARED_WITH.name,
            label = "文人小说对比",
        ),
        // 郭沫若先于艾青（诗歌代际传承）
        GraphEdgeEntity(
            id = "edge-sn-013",
            sourceId = ID_GUOMORUO,
            targetId = ID_AIQING,
            type = GraphEdgeType.PRECEDES.name,
            label = "诗歌代际传承",
        ),
        // 艾青受郭沫若影响（从浪漫主义到现实主义）
        GraphEdgeEntity(
            id = "edge-sn-014",
            sourceId = ID_AIQING,
            targetId = ID_GUOMORUO,
            type = GraphEdgeType.INFLUENCED_BY.name,
            label = "新诗精神影响",
        ),
    )

    // ==================== SubTask 19.4: 体裁×时段二维矩阵骨架 ====================

    /**
     * 体裁×时段二维矩阵 - 节点（6 个）。
     *
     * 体裁维度（4 个，type=CONCEPT）：小说 / 诗歌 / 散文 / 戏剧
     * 时段维度（2 个，type=CONCEPT）：现代文学 / 当代文学
     *
     * 构成 4×2 矩阵骨架，通过 [GENRE_PERIOD_RELATIONS] 中的 BELONGS_TO 边连接。
     */
    val GENRE_PERIOD_MATRIX: List<GraphNodeEntity> = listOf(
        // ---- 体裁节点 ----
        GraphNodeEntity(
            id = ID_GENRE_XIAOSHUO,
            type = GraphNodeType.CONCEPT.name,
            label = "小说",
            subtitle = "体裁维度",
            color = COLOR_GENRE,
            relatedPointId = null,
            subjectId = SUBJECT_ID,
            metadata = mapOf("dimension" to "genre"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_GENRE_SHIGE,
            type = GraphNodeType.CONCEPT.name,
            label = "诗歌",
            subtitle = "体裁维度",
            color = COLOR_GENRE,
            relatedPointId = null,
            subjectId = SUBJECT_ID,
            metadata = mapOf("dimension" to "genre"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_GENRE_SANWEN,
            type = GraphNodeType.CONCEPT.name,
            label = "散文",
            subtitle = "体裁维度",
            color = COLOR_GENRE,
            relatedPointId = null,
            subjectId = SUBJECT_ID,
            metadata = mapOf("dimension" to "genre"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_GENRE_XIJU,
            type = GraphNodeType.CONCEPT.name,
            label = "戏剧",
            subtitle = "体裁维度",
            color = COLOR_GENRE,
            relatedPointId = null,
            subjectId = SUBJECT_ID,
            metadata = mapOf("dimension" to "genre"),
            prerequisites = null,
        ),
        // ---- 时段节点 ----
        GraphNodeEntity(
            id = ID_PERIOD_XIANDAI,
            type = GraphNodeType.CONCEPT.name,
            label = "现代文学",
            subtitle = "时段维度",
            color = COLOR_PERIOD,
            relatedPointId = null,
            subjectId = SUBJECT_ID,
            metadata = mapOf("dimension" to "period"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_PERIOD_DANGDAI,
            type = GraphNodeType.CONCEPT.name,
            label = "当代文学",
            subtitle = "时段维度",
            color = COLOR_PERIOD,
            relatedPointId = null,
            subjectId = SUBJECT_ID,
            metadata = mapOf("dimension" to "period"),
            prerequisites = null,
        ),
    )

    /**
     * 体裁×时段二维矩阵 - 关系（8 条）。
     *
     * 每条边表示一个体裁 BELONGS_TO 一个时段，构成 4×2 矩阵：
     * - 小说 BELONGS_TO 现代文学 / 当代文学
     * - 诗歌 BELONGS_TO 现代文学 / 当代文学
     * - 散文 BELONGS_TO 现代文学 / 当代文学
     * - 戏剧 BELONGS_TO 现代文学 / 当代文学
     *
     * 例如"小说 BELONGS_TO 现代文学"代表矩阵中的"现代小说"单元格。
     */
    val GENRE_PERIOD_RELATIONS: List<GraphEdgeEntity> = listOf(
        // 小说 → 现代文学 / 当代文学
        GraphEdgeEntity(
            id = "edge-gp-001",
            sourceId = ID_GENRE_XIAOSHUO,
            targetId = ID_PERIOD_XIANDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "现代小说",
        ),
        GraphEdgeEntity(
            id = "edge-gp-002",
            sourceId = ID_GENRE_XIAOSHUO,
            targetId = ID_PERIOD_DANGDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "当代小说",
        ),
        // 诗歌 → 现代文学 / 当代文学
        GraphEdgeEntity(
            id = "edge-gp-003",
            sourceId = ID_GENRE_SHIGE,
            targetId = ID_PERIOD_XIANDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "现代诗歌",
        ),
        GraphEdgeEntity(
            id = "edge-gp-004",
            sourceId = ID_GENRE_SHIGE,
            targetId = ID_PERIOD_DANGDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "当代诗歌",
        ),
        // 散文 → 现代文学 / 当代文学
        GraphEdgeEntity(
            id = "edge-gp-005",
            sourceId = ID_GENRE_SANWEN,
            targetId = ID_PERIOD_XIANDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "现代散文",
        ),
        GraphEdgeEntity(
            id = "edge-gp-006",
            sourceId = ID_GENRE_SANWEN,
            targetId = ID_PERIOD_DANGDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "当代散文",
        ),
        // 戏剧 → 现代文学 / 当代文学
        GraphEdgeEntity(
            id = "edge-gp-007",
            sourceId = ID_GENRE_XIJU,
            targetId = ID_PERIOD_XIANDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "现代戏剧",
        ),
        GraphEdgeEntity(
            id = "edge-gp-008",
            sourceId = ID_GENRE_XIJU,
            targetId = ID_PERIOD_DANGDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "当代戏剧",
        ),
    )

    // ==================== v0.7.3 新增:文学流派/社团骨架 ====================

    /**
     * 文学流派与社团节点（14 个，v0.7.3 新增）。
     *
     * 覆盖现当代文学核心流派与社团，每个节点关联真实知识点 ID（relatedPointId）：
     *
     * 现代文学社团（5 个）：
     * - 文学研究会 → kp_00589
     * - 创造社 → kp_00590
     * - 新月社 → kp_00592
     * - 左联 → kp_00634
     * - 京派 → kp_00622
     *
     * 现代文学流派（2 个）：
     * - 海派 → kp_00622（与京派合论）
     * - 文学革命 → kp_00581
     *
     * 当代文学流派（7 个）：
     * - 伤痕文学 → kp_00672
     * - 反思文学 → kp_00673
     * - 改革文学 → kp_00675
     * - 寻根文学 → kp_00682
     * - 先锋小说 → kp_00678
     * - 朦胧诗 → kp_00667
     * - 新写实小说 → kp_00910（v0.7.3 新增知识点）
     */
    val LITERARY_SCHOOLS: List<GraphNodeEntity> = listOf(
        // ---- 现代文学社团 ----
        GraphNodeEntity(
            id = ID_SCHOOL_WENXUEGE,
            type = GraphNodeType.CONCEPT.name,
            label = "文学研究会",
            subtitle = "1921·现实主义",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00589",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "society", "year" to "1921"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_CHUANGZAO,
            type = GraphNodeType.CONCEPT.name,
            label = "创造社",
            subtitle = "1921·浪漫主义",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00590",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "society", "year" to "1921"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_XINYUE,
            type = GraphNodeType.CONCEPT.name,
            label = "新月社",
            subtitle = "1923·新格律诗",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00592",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "society", "year" to "1923"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_ZUOLIAN,
            type = GraphNodeType.CONCEPT.name,
            label = "左联",
            subtitle = "1930·左翼文学",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00634",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "society", "year" to "1930"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_JINGPAI,
            type = GraphNodeType.CONCEPT.name,
            label = "京派",
            subtitle = "1930s·田园牧歌",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00622",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "school", "year" to "1930s"),
            prerequisites = null,
        ),
        // ---- 现代文学流派 ----
        GraphNodeEntity(
            id = ID_SCHOOL_HAIPAI,
            type = GraphNodeType.CONCEPT.name,
            label = "海派",
            subtitle = "1930s·都市现代主义",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00622",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "school", "year" to "1930s"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_WENYAN,
            type = GraphNodeType.CONCEPT.name,
            label = "文学革命",
            subtitle = "1917·新文学开端",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00581",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "movement", "year" to "1917"),
            prerequisites = null,
        ),
        // ---- 当代文学流派 ----
        GraphNodeEntity(
            id = ID_SCHOOL_SHANGHEN,
            type = GraphNodeType.CONCEPT.name,
            label = "伤痕文学",
            subtitle = "1978·新时期开端",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00672",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "movement", "year" to "1978"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_FANSI,
            type = GraphNodeType.CONCEPT.name,
            label = "反思文学",
            subtitle = "1979·历史追问",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00673",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "movement", "year" to "1979"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_GAIGE,
            type = GraphNodeType.CONCEPT.name,
            label = "改革文学",
            subtitle = "1981·改革叙事",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00675",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "movement", "year" to "1981"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_XUNGEN,
            type = GraphNodeType.CONCEPT.name,
            label = "寻根文学",
            subtitle = "1985·文化寻根",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00682",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "movement", "year" to "1985"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_XIANFENG,
            type = GraphNodeType.CONCEPT.name,
            label = "先锋小说",
            subtitle = "1985·现代主义实验",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00678",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "movement", "year" to "1985"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_MENLONG,
            type = GraphNodeType.CONCEPT.name,
            label = "朦胧诗",
            subtitle = "1980·诗歌变革",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00667",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "movement", "year" to "1980"),
            prerequisites = null,
        ),
        GraphNodeEntity(
            id = ID_SCHOOL_XINSHIXIE,
            type = GraphNodeType.CONCEPT.name,
            label = "新写实小说",
            subtitle = "1989·原生态写实",
            color = COLOR_SCHOOL,
            relatedPointId = "kp_00910",
            subjectId = SUBJECT_ID,
            metadata = mapOf("type" to "movement", "year" to "1989"),
            prerequisites = null,
        ),
    )

    /**
     * 文学流派/社团关系（18 条，v0.7.3 新增）。
     *
     * 关系类型分布：
     * - PRECEDES: 文学革命→文学研究会 / 文学革命→创造社 / ... (流派时序)
     * - BELONGS_TO: 各流派 BELONGS_TO 现代文学/当代文学时段
     * - COMPARED_WITH: 京派-海派 / 寻根文学-先锋小说 / 伤痕文学-反思文学
     * - INFLUENCED_BY: 寻根文学←反思文学 / 先锋小说←朦胧诗 / 新写实←寻根文学
     */
    val LITERARY_SCHOOL_RELATIONS: List<GraphEdgeEntity> = listOf(
        // 时序关系：文学革命先于各社团
        GraphEdgeEntity(
            id = "edge-ls-001",
            sourceId = ID_SCHOOL_WENYAN,
            targetId = ID_SCHOOL_WENXUEGE,
            type = GraphEdgeType.PRECEDES.name,
            label = "催生",
        ),
        GraphEdgeEntity(
            id = "edge-ls-002",
            sourceId = ID_SCHOOL_WENYAN,
            targetId = ID_SCHOOL_CHUANGZAO,
            type = GraphEdgeType.PRECEDES.name,
            label = "催生",
        ),
        // 流派归属：现代文学流派 BELONGS_TO 现代文学时段
        GraphEdgeEntity(
            id = "edge-ls-003",
            sourceId = ID_SCHOOL_WENXUEGE,
            targetId = ID_PERIOD_XIANDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "现代文学社团",
        ),
        GraphEdgeEntity(
            id = "edge-ls-004",
            sourceId = ID_SCHOOL_CHUANGZAO,
            targetId = ID_PERIOD_XIANDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "现代文学社团",
        ),
        GraphEdgeEntity(
            id = "edge-ls-005",
            sourceId = ID_SCHOOL_XINYUE,
            targetId = ID_PERIOD_XIANDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "现代文学社团",
        ),
        GraphEdgeEntity(
            id = "edge-ls-006",
            sourceId = ID_SCHOOL_ZUOLIAN,
            targetId = ID_PERIOD_XIANDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "现代文学社团",
        ),
        // 京派与海派对比
        GraphEdgeEntity(
            id = "edge-ls-007",
            sourceId = ID_SCHOOL_JINGPAI,
            targetId = ID_SCHOOL_HAIPAI,
            type = GraphEdgeType.COMPARED_WITH.name,
            label = "京派海派之争",
        ),
        // 当代文学流派 BELONGS_TO 当代文学时段
        GraphEdgeEntity(
            id = "edge-ls-008",
            sourceId = ID_SCHOOL_SHANGHEN,
            targetId = ID_PERIOD_DANGDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "新时期文学",
        ),
        GraphEdgeEntity(
            id = "edge-ls-009",
            sourceId = ID_SCHOOL_FANSI,
            targetId = ID_PERIOD_DANGDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "新时期文学",
        ),
        GraphEdgeEntity(
            id = "edge-ls-010",
            sourceId = ID_SCHOOL_GAIGE,
            targetId = ID_PERIOD_DANGDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "新时期文学",
        ),
        GraphEdgeEntity(
            id = "edge-ls-011",
            sourceId = ID_SCHOOL_XUNGEN,
            targetId = ID_PERIOD_DANGDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "85新潮",
        ),
        GraphEdgeEntity(
            id = "edge-ls-012",
            sourceId = ID_SCHOOL_XIANFENG,
            targetId = ID_PERIOD_DANGDAI,
            type = GraphEdgeType.BELONGS_TO.name,
            label = "85新潮",
        ),
        // 流派时序：伤痕→反思→改革→寻根→先锋→新写实
        GraphEdgeEntity(
            id = "edge-ls-013",
            sourceId = ID_SCHOOL_SHANGHEN,
            targetId = ID_SCHOOL_FANSI,
            type = GraphEdgeType.PRECEDES.name,
            label = "深化",
        ),
        GraphEdgeEntity(
            id = "edge-ls-014",
            sourceId = ID_SCHOOL_FANSI,
            targetId = ID_SCHOOL_GAIGE,
            type = GraphEdgeType.PRECEDES.name,
            label = "转向",
        ),
        GraphEdgeEntity(
            id = "edge-ls-015",
            sourceId = ID_SCHOOL_GAIGE,
            targetId = ID_SCHOOL_XUNGEN,
            type = GraphEdgeType.PRECEDES.name,
            label = "文化转向",
        ),
        GraphEdgeEntity(
            id = "edge-ls-016",
            sourceId = ID_SCHOOL_XUNGEN,
            targetId = ID_SCHOOL_XIANFENG,
            type = GraphEdgeType.PRECEDES.name,
            label = "形式实验",
        ),
        // 流派对比与影响
        GraphEdgeEntity(
            id = "edge-ls-017",
            sourceId = ID_SCHOOL_XUNGEN,
            targetId = ID_SCHOOL_XIANFENG,
            type = GraphEdgeType.COMPARED_WITH.name,
            label = "85新潮双流",
        ),
        GraphEdgeEntity(
            id = "edge-ls-018",
            sourceId = ID_SCHOOL_XIANFENG,
            targetId = ID_SCHOOL_XINSHIXIE,
            type = GraphEdgeType.PRECEDES.name,
            label = "回归现实",
        ),
    )
}
