package com.wenyan.app.feature.graph.ui

import androidx.compose.ui.graphics.PathEffect

/**
 * 知识图谱常量集中定义（v0.8.0 抽出，解决 magic number 散落问题）。
 *
 * 设计依据：
 * - Sweller 认知负荷理论：视觉通道有限，避免编码冲突
 * - Novak 概念图理论：边标签是 meaningful learning 的关键
 * - 调研报告 §4.5：从 14 色降到 3 层语义编码
 */
internal object GraphConstants {

    // ── 节点尺寸 ──

    /** 基础节点半径（dp，世界坐标系） */
    const val NODE_RADIUS_DP = 10f
    /** 点击命中半径（dp，比绘制半径大，提升触摸体验） */
    const val NODE_TOUCH_RADIUS_DP = 24f

    // ── 缩放范围 ──

    const val MIN_SCALE = 0.2f
    const val MAX_SCALE = 3.5f

    // ── LOD 阈值（v0.8.0 调整：边标签从 1.8 → 1.0，与节点标签同步）──

    /** scale < 此值：仅圆点（全局概览） */
    const val LOD_MINIMAL = 0.35f
    /** scale >= 此值：加边 + 节点形状 */
    const val LOD_SPARSE = 0.7f
    /** scale >= 此值：加节点标签 + 边标签（v0.8.0 修复：边标签阈值从 1.8 降到 1.0） */
    const val LOD_LABEL = 1.0f

    // ── 节点尺寸倍率（按重要性，4 档）──

    /** sourceKpIds >= 7：核心实体（如鲁迅、诗经） */
    const val SIZE_CORE = 1.6f
    /** sourceKpIds 4-6：重要实体 */
    const val SIZE_IMPORTANT = 1.3f
    /** sourceKpIds 2-3：常规实体 */
    const val SIZE_NORMAL = 1.0f
    /** sourceKpIds <= 1：边缘实体 */
    const val SIZE_MINOR = 0.7f
    /** 聚焦节点放大倍率（替代光环） */
    const val FOCUS_SCALE = 1.4f
    /** 高亮节点放大倍率 */
    const val HIGHLIGHT_SCALE = 1.2f

    // ── 掌握度阈值 ──

    /** R >= 此值：已掌握 */
    const val MASTERY_THRESHOLD = 0.8f
    /** 0 < R < 此值：薄弱 */
    const val WEAK_THRESHOLD = 0.5f

    // ── 核心节点判定（DisplayScope.CORE）──

    /** 关联知识点数 >= 此值为核心节点 */
    const val CORE_KP_THRESHOLD = 4
    /** 节点度数 >= 此值为桥接节点 */
    const val CORE_DEGREE_THRESHOLD = 3

    // ── 边绘制参数 ──

    /** 普通边线宽 */
    const val EDGE_WIDTH_NORMAL = 1.2f
    /** 薄弱边线宽 */
    const val EDGE_WIDTH_WEAK = 1.8f
    /** 高亮边线宽 */
    const val EDGE_WIDTH_HIGHLIGHT = 2f
    /** 类型描边线宽 */
    const val NODE_STROKE_WIDTH = 2f
    /** 标签距节点的间距 */
    const val LABEL_OFFSET = 4f

    // ── 视口剔除边距（节点半径倍数）──
    const val CULL_MARGIN_RATIO = 4f

    // ── 时间轴布局参数 ──

    /** 时间轴起点年份（覆盖现当代文学全周期） */
    const val TIMELINE_START_YEAR = 1915
    /** 时间轴终点年份 */
    const val TIMELINE_END_YEAR = 2030
    /** 时间轴左/右留白比例 */
    const val TIMELINE_PADDING_RATIO = 0.05f
    /** 时间轴顶部刻度线高度比例 */
    const val TIMELINE_RULER_RATIO = 0.06f
    /** 时间轴泳道数量 */
    const val TIMELINE_LANES = 5

    // ── 力导向布局参数（邻域模式）──

    /** 力导向迭代次数 */
    const val FORCE_ITERATIONS = 80
    /** 理想边长（弹簧静止长度，px） */
    const val FORCE_IDEAL_LENGTH = 120f
    /** 弹簧系数 */
    const val FORCE_SPRING_K = 0.05f
    /** 斥力系数 */
    const val FORCE_REPULSION_K = 8000f
    /** 中心引力系数（拉向画布中心） */
    const val FORCE_CENTER_K = 0.01f
    /** 邻域模式最大节点数（避免认知超载，Sweller 12-20 上限放宽到 30） */
    const val NEIGHBORHOOD_MAX_NODES = 30
}

/**
 * 节点类型顺序（用于环形布局的子扇区分配）。
 */
internal val TYPE_ORDER = listOf(
    "AUTHOR",
    "WORK",
    "CONCEPT",
    "MOVEMENT",
    "SCHOOL",
    "KNOWLEDGE_POINT",
)

/**
 * 节点类型显示名（供图例与详情使用）。
 */
val GRAPH_TYPE_DISPLAY_NAME = mapOf(
    "AUTHOR" to "作家",
    "WORK" to "作品",
    "CONCEPT" to "概念",
    "MOVEMENT" to "运动",
    "SCHOOL" to "流派",
    "KNOWLEDGE_POINT" to "知识点",
)

/**
 * 节点类型颜色（导出供图例使用；Canvas 渲染改用形状编码，此颜色仅用于图例小圆点）。
 *
 * v0.8.0 调整：移除类型描边色（与节点填充色冲突），改用形状区分类型。
 * 此映射保留供图例显示。
 */
val GRAPH_TYPE_COLORS = mapOf(
    "AUTHOR" to 0xFFE91E63.toInt(),          // 粉
    "WORK" to 0xFFFF9800.toInt(),            // 橙
    "CONCEPT" to 0xFF2196F3.toInt(),         // 蓝
    "MOVEMENT" to 0xFF9C27B0.toInt(),        // 紫
    "SCHOOL" to 0xFF9C27B0.toInt(),          // 紫
    "KNOWLEDGE_POINT" to 0xFF4CAF50.toInt(), // 绿
)

/**
 * 科目显示名（供图例使用）。
 */
val GRAPH_SUBJECT_DISPLAY_NAME = mapOf(
    "subj_01" to "古代",
    "subj_02" to "现当代",
    "subj_03" to "外国",
    "subj_04" to "理论",
)

/**
 * 科目颜色（仅供径向概览模式的扇区背景使用，时间轴模式不使用）。
 */
val GRAPH_SUBJECT_COLORS = mapOf(
    "subj_01" to 0xFFE91E63.toInt(),  // 古代 - 粉
    "subj_02" to 0xFF4CAF50.toInt(),  // 现当代 - 绿
    "subj_03" to 0xFFFF9800.toInt(),  // 外国 - 橙
    "subj_04" to 0xFF2196F3.toInt(),  // 理论 - 蓝
)

/**
 * 边类型 → 中文标签映射（v0.8.0 强化：边语义化是概念图学习价值的核心）。
 *
 * 研究依据：Nesbit & Adesope 元分析证实，有标签边的图比无标签图学习价值高 3-5 倍。
 */
val EDGE_TYPE_LABELS = mapOf(
    "INFLUENCED_BY" to "受影响",
    "SAME_PERIOD" to "同时期",
    "COMPARED_WITH" to "对比",
    "PRECEDES" to "先于",
    "BELONGS_TO" to "属于",
    "PART_OF" to "部分",
    "WRITTEN_BY" to "作者",
    "MEMBER_OF" to "成员",
    "RELATED_TO" to "相关",
    "DERIVED_FROM" to "源自",
    "CONTRASTS_WITH" to "对照",
    "PARTICIPATED_IN" to "参与",
)

/**
 * 边类型 → 线型样式（v0.8.0 新增：线型编码关系类型，正交于颜色通道）。
 *
 * 研究依据：NYU InfoVis 讲义指出，边编码 thickness/pattern/color 三通道中，
 * pattern（线型）最不易与节点色冲突，适合编码关系类型。
 *
 * - 实线：有向因果/包含关系
 * - 虚线：横向关联/时序关系
 * - 加粗：对比/张力关系（突出显示）
 */
val EDGE_TYPE_LINE_STYLES: Map<String, EdgeLineStyle> = mapOf(
    "INFLUENCED_BY" to EdgeLineStyle.SOLID_ARROW,
    "DERIVED_FROM" to EdgeLineStyle.SOLID_ARROW,
    "PRECEDES" to EdgeLineStyle.DASHED_ARROW,
    "SAME_PERIOD" to EdgeLineStyle.DASHED,
    "COMPARED_WITH" to EdgeLineStyle.THICK,
    "CONTRASTS_WITH" to EdgeLineStyle.THICK,
    "BELONGS_TO" to EdgeLineStyle.SOLID,
    "PART_OF" to EdgeLineStyle.SOLID,
    "WRITTEN_BY" to EdgeLineStyle.SOLID_ARROW,
    "MEMBER_OF" to EdgeLineStyle.SOLID,
    "PARTICIPATED_IN" to EdgeLineStyle.SOLID_ARROW,
    "RELATED_TO" to EdgeLineStyle.DASHED,
)

/**
 * 边线型样式枚举。
 */
enum class EdgeLineStyle {
    /** 实线（普通） */
    SOLID,
    /** 实线 + 箭头（有向关系：受影响、源自、先于、作者、参与） */
    SOLID_ARROW,
    /** 虚线（横向关联：同时期、相关） */
    DASHED,
    /** 虚线 + 箭头（时序有向：先于） */
    DASHED_ARROW,
    /** 加粗实线（对比/张力：对比、对照） */
    THICK,
}

/** 虚线效果（dash 10px, gap 6px） */
val DASHED_PATH_EFFECT = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
