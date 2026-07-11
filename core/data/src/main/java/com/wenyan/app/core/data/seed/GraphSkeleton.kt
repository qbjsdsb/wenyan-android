package com.wenyan.app.core.data.seed

import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphEdgeType
import com.wenyan.app.core.database.entity.GraphNodeEntity
import com.wenyan.app.core.database.entity.GraphNodeType

/**
 * 知识图谱骨架预置数据。
 *
 * 对应 Spec 第 307-342 行"功能性知识图谱"要求，预置两类骨架：
 *
 * 1. 南师大现当代文学考点骨架（[SOUTHERN_NORMAL_AUTHORS] + [SOUTHERN_NORMAL_RELATIONS]）
 *    - 7 位作家节点：鲁迅 / 周作人 / 茅盾 / 沈从文 / 张爱玲 / 赵树理 / 路遥
 *    - 作家间关系：同时期 / 对比 / 受影响 / 先于
 *
 * 2. 体裁×时段二维矩阵骨架（[GENRE_PERIOD_MATRIX] + [GENRE_PERIOD_RELATIONS]）
 *    - 体裁维度：小说 / 诗歌 / 散文 / 戏剧
 *    - 时段维度：现代文学 / 当代文学
 *    - 关系：体裁 BELONGS_TO 时段（构成 4×2 矩阵）
 *
 * 所有节点 ID 使用固定 UUID 字符串，保证多次导入不会重复（DAO 使用 REPLACE 策略）。
 */
object GraphSkeleton {

    // ==================== 常量定义 ====================

    /** 现当代文学科目 ID（固定 UUID，与 subjects 表对应） */
    private const val SUBJECT_ID = "subject-modern-contemporary-literature"

    /** 节点颜色：作家（粉色） */
    private val COLOR_AUTHOR = 0xFFE91E63.toInt()

    /** 节点颜色：体裁（蓝色） */
    private val COLOR_GENRE = 0xFF2196F3.toInt()

    /** 节点颜色：时段（绿色） */
    private val COLOR_PERIOD = 0xFF4CAF50.toInt()

    // 南师大作家节点 ID
    private const val ID_LUXUN = "550e8400-e29b-41d4-a716-446655440001"
    private const val ID_ZHOUZUOREN = "550e8400-e29b-41d4-a716-446655440002"
    private const val ID_MAODUN = "550e8400-e29b-41d4-a716-446655440003"
    private const val ID_SHENCONGWEN = "550e8400-e29b-41d4-a716-446655440004"
    private const val ID_ZHANGAILING = "550e8400-e29b-41d4-a716-446655440005"
    private const val ID_ZHAOSHULI = "550e8400-e29b-41d4-a716-446655440006"
    private const val ID_LUYAO = "550e8400-e29b-41d4-a716-446655440007"

    // 体裁节点 ID
    private const val ID_GENRE_XIAOSHUO = "550e8400-e29b-41d4-a716-446655440101"
    private const val ID_GENRE_SHIGE = "550e8400-e29b-41d4-a716-446655440102"
    private const val ID_GENRE_SANWEN = "550e8400-e29b-41d4-a716-446655440103"
    private const val ID_GENRE_XIJU = "550e8400-e29b-41d4-a716-446655440104"

    // 时段节点 ID
    private const val ID_PERIOD_XIANDAI = "550e8400-e29b-41d4-a716-446655440201"
    private const val ID_PERIOD_DANGDAI = "550e8400-e29b-41d4-a716-446655440202"

    // ==================== SubTask 19.3: 南师大现当代文学考点骨架 ====================

    /**
     * 南师大现当代文学考点 - 作家节点（7 位）。
     *
     * 包含：鲁迅 / 周作人 / 茅盾 / 沈从文 / 张爱玲 / 赵树理 / 路遥。
     * 每个节点含 id / type=AUTHOR / label（姓名）/ subtitle（生卒年）/ subject_id（现当代文学）。
     */
    val SOUTHERN_NORMAL_AUTHORS: List<GraphNodeEntity> = listOf(
        GraphNodeEntity(
            id = ID_LUXUN,
            type = GraphNodeType.AUTHOR.name,
            label = "鲁迅",
            subtitle = "1881-1936",
            color = COLOR_AUTHOR,
            relatedPointId = null,
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
            relatedPointId = null,
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
            relatedPointId = null,
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
            relatedPointId = null,
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
            relatedPointId = null,
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
    )

    /**
     * 南师大现当代文学考点 - 作家关系（8 条）。
     *
     * 关系类型分布：
     * - SAME_PERIOD: 鲁迅-周作人 / 茅盾-沈从文 / 周作人-茅盾
     * - COMPARED_WITH: 茅盾-沈从文（文学史常做对比）
     * - INFLUENCED_BY: 沈从文→鲁迅 / 张爱玲→鲁迅（鲁迅作为新文学开创者，不设 INFLUENCED_BY）
     * - PRECEDES: 鲁迅→茅盾 / 赵树理→路遥
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
}
