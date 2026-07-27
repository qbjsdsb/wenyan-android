package com.wenyan.app.feature.graph.ui

import androidx.compose.ui.geometry.Offset
import com.wenyan.app.feature.graph.GraphEdgeItem
import com.wenyan.app.feature.graph.GraphNodeItem
import com.wenyan.app.feature.graph.LayoutMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * 知识图谱布局算法（v0.8.0 抽出，支持三模式可切换）。
 *
 * 三种布局对应三种学习任务（详见调研报告 §4）：
 * - [calculateTimelineLayout]：建立文学史脉络（默认模式）
 * - [calculateNeighborhoodLayout]：深挖某节点关系（Obsidian Local Graph 范式）
 * - [calculateRadialLayout]：鸟瞰科目全局（远观模式）
 *
 * 理论依据：
 * - NYU InfoVis 2025：节点有语义属性时 fixed layout 优于 force-directed
 * - Sweller 认知负荷理论：邻域模式节点数控制在 12-30
 * - Obsidian Local Graph：以焦点节点为中心的邻域探索比全局图更有用
 */
internal object GraphLayout {

    /**
     * 布局结果。
     *
     * @property positions 节点 ID → 世界坐标
     * @property nodeSubject 节点 ID → 科目 ID（用于跨科目边判定）
     * @property centerX 画布中心 X（用于跨科目边贝塞尔曲线控制点）
     * @property centerY 画布中心 Y
     * @property subjectSectors 径向布局的科目扇区（仅 RADIAL 模式有值）
     * @property timelineTicks 时间轴刻度（仅 TIMELINE 模式有值）
     * @property timelineLanes 时间轴泳道定义（仅 TIMELINE 模式有值）
     */
    data class LayoutResult(
        val positions: Map<String, Offset>,
        val nodeSubject: Map<String, String>,
        val centerX: Float,
        val centerY: Float,
        val subjectSectors: List<SubjectSector> = emptyList(),
        val timelineTicks: List<TimelineTick> = emptyList(),
        val timelineLanes: List<TimelineLane> = emptyList(),
    )

    /** 径向布局的科目扇区 */
    data class SubjectSector(
        val subjectId: String,
        val startAngle: Double,
        val endAngle: Double,
        val innerRadius: Float,
        val outerRadius: Float,
    )

    /** 时间轴刻度线 */
    data class TimelineTick(
        val year: Int,
        val x: Float,
        val label: String,
    )

    /** 时间轴泳道定义 */
    data class TimelineLane(
        val name: String,
        val y: Float,
        val color: Int,
    )

    /**
     * 根据布局模式选择布局算法。
     */
    fun calculate(
        mode: LayoutMode,
        nodes: List<GraphNodeItem>,
        edges: List<GraphEdgeItem>,
        width: Float,
        height: Float,
        focusedNodeId: String? = null,
    ): LayoutResult = when (mode) {
        LayoutMode.TIMELINE -> calculateTimelineLayout(nodes, edges, width, height)
        LayoutMode.NEIGHBORHOOD -> calculateNeighborhoodLayout(nodes, edges, width, height, focusedNodeId)
        LayoutMode.RADIAL -> calculateRadialLayout(nodes, width, height)
    }

    // ==================== 模式一：时间轴泳道布局（默认）====================

    /**
     * 文学史时间轴泳道布局（v0.8.0 重构，复用 v0.7.6 数据资产）。
     *
     * ## 布局结构
     *
     * ```
     * 1917  1927  1937  1949  1966  1976  1989  1990s   ← 时间刻度线
     * │     │     │     │     │     │     │     │
     * [五四]──[左翼]──[抗战]──[十七年]──[文革]──[新时期]──[后新时期]  ← 时段节点带（顶部）
     * ═══════════════════════════════════════════════════
     * 流派/社团泳道  ◇文学革命  ◇创造社  ◇左联  ◇京派  ◇伤痕  ◇寻根
     * ═══════════════════════════════════════════════════
     * 小说泳道  ●鲁迅─────●茅─────●沈─────●赵─────●路遥
     * ═══════════════════════════════════════════════════
     * 诗歌泳道  ●郭───────●艾──────●─────●──────●
     * ═══════════════════════════════════════════════════
     * 散文泳道  ●周作人─────●巴──────●─────●──────●
     * ═══════════════════════════════════════════════════
     * 戏剧泳道  ●曹禺─────────────────●─────────●
     * ═══════════════════════════════════════════════════
     * ```
     *
     * ## 设计依据
     *
     * - Map of the Literature 范式：时间是主轴，体裁是次轴，流派是分支
     * - NYU InfoVis 2025：fixed layout 适合语义属性
     * - 北大《论语》KG：多维知识网络
     *
     * ## 节点定位规则
     *
     * - 时段节点（type=CONCEPT, dimension=period）：顶部时间刻度带
     * - 流派/社团（type=SCHOOL/MOVEMENT 或 metadata.type=society/movement）：流派泳道
     * - 作家（type=AUTHOR）：按主要创作体裁分泳道，x = (birth+death)/2
     * - 作品（type=WORK）：x = 创作年份，y = 体裁泳道
     * - 概念/知识点（type=KNOWLEDGE_POINT）：底部概念层
     * - 无年份节点：x 随机散布在所属时段范围内
     */
    fun calculateTimelineLayout(
        nodes: List<GraphNodeItem>,
        edges: List<GraphEdgeItem>,
        width: Float,
        height: Float,
    ): LayoutResult {
        val positions = mutableMapOf<String, Offset>()
        val nodeSubject = mutableMapOf<String, String>()

        if (nodes.isEmpty()) {
            return LayoutResult(positions, nodeSubject, width / 2f, height / 2f)
        }

        val padX = width * GraphConstants.TIMELINE_PADDING_RATIO
        val padY = height * GraphConstants.TIMELINE_PADDING_RATIO
        val drawableWidth = width - 2 * padX
        val drawableHeight = height - 2 * padY

        // 时间轴横轴映射：年份 → x 坐标
        val yearRange = (GraphConstants.TIMELINE_END_YEAR - GraphConstants.TIMELINE_START_YEAR).toFloat()
        fun yearToX(year: Int): Float {
            val clamped = year.coerceIn(
                GraphConstants.TIMELINE_START_YEAR,
                GraphConstants.TIMELINE_END_YEAR,
            )
            return padX + (clamped - GraphConstants.TIMELINE_START_YEAR) / yearRange * drawableWidth
        }

        // 泳道定义（6 条：流派/小说/诗歌/散文/戏剧/知识点）
        // v0.8.2 修复：原 5 条泳道导致 genre 节点（laneIdx=-2）和 KNOWLEDGE_POINT（laneIdx=5）越界崩溃
        val rulerHeight = drawableHeight * GraphConstants.TIMELINE_RULER_RATIO
        val laneAreaTop = padY + rulerHeight
        val laneAreaHeight = drawableHeight - rulerHeight
        val laneCount = 6
        val laneSpacing = laneAreaHeight / laneCount

        val lanes = listOf(
            TimelineLane("流派/社团", laneAreaTop + laneSpacing * 0.5f, 0xFF9C27B0.toInt()),
            TimelineLane("小说", laneAreaTop + laneSpacing * 1.5f, 0xFFE91E63.toInt()),
            TimelineLane("诗歌", laneAreaTop + laneSpacing * 2.5f, 0xFFFF9800.toInt()),
            TimelineLane("散文", laneAreaTop + laneSpacing * 3.5f, 0xFF2196F3.toInt()),
            TimelineLane("戏剧", laneAreaTop + laneSpacing * 4.5f, 0xFF4CAF50.toInt()),
            TimelineLane("知识点", laneAreaTop + laneSpacing * 5.5f, 0xFF607D8B.toInt()),
        )

        // 关键年份刻度
        val keyYears = listOf(
            1917 to "1917",
            1927 to "1927",
            1937 to "1937",
            1949 to "1949",
            1966 to "1966",
            1976 to "1976",
            1989 to "1989",
            2000 to "2000",
        )
        val ticks = keyYears.map { (year, label) ->
            TimelineTick(year, yearToX(year), label)
        }

        // 邻接表 + 节点索引（v0.8.1：体裁判定改为基于边语义，移除硬编码 UUID）
        val adjacency = mutableMapOf<String, MutableList<GraphEdgeItem>>()
        for (edge in edges) {
            adjacency.getOrPut(edge.fromId) { mutableListOf() }.add(edge)
            adjacency.getOrPut(edge.toId) { mutableListOf() }.add(edge)
        }
        val nodeMap = nodes.associateBy { it.id }

        fun isGenreNode(node: GraphNodeItem): Boolean =
            node.type == "CONCEPT" && node.metadata?.get("dimension") == "genre"

        fun isPeriodNode(node: GraphNodeItem): Boolean =
            node.type == "CONCEPT" && node.metadata?.get("dimension") == "period"

        /** 通过 BELONGS_TO 边查找节点所属体裁泳道（移除硬编码 UUID，改用 label 匹配） */
        fun findGenreLaneIndex(nodeId: String): Int {
            val connected = adjacency[nodeId] ?: return 1
            for (edge in connected) {
                if (edge.relation != "BELONGS_TO") continue
                val otherId = if (edge.fromId == nodeId) edge.toId else edge.fromId
                val other = nodeMap[otherId] ?: continue
                if (!isGenreNode(other)) continue
                return when (other.label) {
                    "小说" -> 1
                    "诗歌" -> 2
                    "散文" -> 3
                    "戏剧" -> 4
                    else -> 1
                }
            }
            return 1
        }

        // 节点 → 泳道映射（v0.8.2 修复：genre 节点返回有效索引，不再返回 -2 越界）
        fun nodeLaneIndex(node: GraphNodeItem): Int = when {
            isPeriodNode(node) -> 0 // 时段节点放流派泳道（不再用 -1 特殊值，避免越界）
            isGenreNode(node) -> when (node.label) {
                "小说" -> 1
                "诗歌" -> 2
                "散文" -> 3
                "戏剧" -> 4
                else -> 0
            }
            node.type in listOf("SCHOOL", "MOVEMENT") -> 0
            node.metadata?.get("type") in listOf("society", "movement", "school") -> 0
            node.type == "AUTHOR" -> findGenreLaneIndex(node.id)
            node.type == "WORK" -> findGenreLaneIndex(node.id)
            node.type == "KNOWLEDGE_POINT" -> 5 // 知识点层（v0.8.2：lanes 已扩展到 6 条）
            node.type == "CHARACTER" -> 1
            else -> 0 // 概念节点放流派泳道
        }

        // 计算节点 x 坐标（v0.8.1：无年份节点按类型+科目分配默认年份，不再纯随机）
        fun nodeX(node: GraphNodeItem): Float {
            val meta = node.metadata
            if (meta != null) {
                meta["year"]?.toIntOrNull()?.let { return yearToX(it) }
                val startYear = meta["startYear"]?.toIntOrNull()
                val endYear = meta["endYear"]?.toIntOrNull()
                if (startYear != null && endYear != null) return yearToX((startYear + endYear) / 2)
                if (startYear != null) return yearToX(startYear)
                val birthYear = meta["birthYear"]?.toIntOrNull()
                val deathYear = meta["deathYear"]?.toIntOrNull()
                if (birthYear != null && deathYear != null) return yearToX((birthYear + deathYear) / 2)
                if (birthYear != null) return yearToX(birthYear + 30)
            }
            // 无年份：按类型 + 科目分配默认年份（确定性，基于 id 哈希）
            val seed = node.id.hashCode()
            val baseYear = when {
                node.type == "AUTHOR" -> 1910 + (seed and 0x7F) % 80
                node.type == "WORK" -> 1930 + (seed and 0x7F) % 70
                node.type in listOf("SCHOOL", "MOVEMENT") ||
                    node.metadata?.get("type") in listOf("society", "movement") -> 1920 + (seed and 0x7F) % 70
                node.type == "KNOWLEDGE_POINT" && node.subjectId == "subj_02" -> 1930 + (seed and 0x7F) % 70
                else -> 1920 + (seed and 0x7F) % 80
            }
            return yearToX(baseYear)
        }

        // 同泳道节点防重叠：x 坐标最小间距
        // v0.8.3：提取到 GraphConstants.TIMELINE_MIN_SPACING / TIMELINE_OVERLAP_OFFSET
        // v0.8.2 性能修复：原 O(n²) count 改为 TreeSet subSet O(log n)，
        // 2123 节点 ALL 模式从 ~450 万次比较降到 ~2.3 万次比较（n log n）。
        val laneNodeXs = mutableMapOf<Int, java.util.TreeSet<Float>>()

        for (node in nodes) {
            val laneIdx = nodeLaneIndex(node).coerceIn(0, lanes.lastIndex)
            val x = nodeX(node)
            val y = lanes[laneIdx].y

            // 防重叠：统计同泳道 x ∈ (x - MIN_SPACING, x + MIN_SPACING) 的已添加节点数
            // TreeSet.subSet(from, fromInclusive, to, toInclusive) 是 O(log n)
            val xs = laneNodeXs.getOrPut(laneIdx) { java.util.TreeSet() }
            val low = x - GraphConstants.TIMELINE_MIN_SPACING
            val high = x + GraphConstants.TIMELINE_MIN_SPACING
            // subSet(low, false, high, false) 排除两端，等价于 abs(it - x) < MIN_SPACING
            val overlapCount = xs.subSet(low, false, high, false).size
            xs.add(x)
            val finalY = y + overlapCount * GraphConstants.TIMELINE_OVERLAP_OFFSET * (if (overlapCount % 2 == 0) 1 else -1)

            positions[node.id] = Offset(x, finalY)
            nodeSubject[node.id] = node.subjectId ?: "unknown"
        }

        return LayoutResult(
            positions = positions,
            nodeSubject = nodeSubject,
            centerX = width / 2f,
            centerY = height / 2f,
            timelineTicks = ticks,
            timelineLanes = lanes,
        )
    }

    // v0.8.3 清理：targetIsGenre 函数已废弃（v0.8.1 改用 findGenreLaneIndex 基于 label 匹配），删除

    // ==================== 模式二：邻域力导向布局 ====================

    /**
     * 邻域力导向布局（Obsidian Local Graph 范式）。
     *
     * 以 [focusedNodeId] 为中心，1-3 跳邻居用力导向布局。
     *
     * ## 算法
     *
     * 简化 spring-electric 模型：
     * - 弹簧力（连接的节点互相吸引）
     * - 斥力（所有节点对互相排斥）
     * - 中心引力（拉向画布中心，防止节点飞散）
     *
     * 迭代 [GraphConstants.FORCE_ITERATIONS] 次收敛，无需 Barnes-Hut（n<30 时 O(n²) 可接受）。
     *
     * ## 节点数量控制
     *
     * 遵循 Sweller 认知负荷理论，邻域模式上限 [GraphConstants.NEIGHBORHOOD_MAX_NODES]（30）。
     * 超出时按 edge weight 优先保留近邻。
     */
    fun calculateNeighborhoodLayout(
        nodes: List<GraphNodeItem>,
        edges: List<GraphEdgeItem>,
        width: Float,
        height: Float,
        focusedNodeId: String?,
    ): LayoutResult {
        val positions = mutableMapOf<String, Offset>()
        val nodeSubject = mutableMapOf<String, String>()

        if (nodes.isEmpty()) {
            return LayoutResult(positions, nodeSubject, width / 2f, height / 2f)
        }

        val centerNodeId = focusedNodeId ?: nodes.first().id
        val centerNode = nodes.find { it.id == centerNodeId } ?: nodes.first()

        // 找邻居（无向图，2跳）
        val neighbors = mutableSetOf(centerNodeId)
        val directNeighbors = mutableSetOf<String>()
        for (edge in edges) {
            if (edge.fromId == centerNodeId) {
                directNeighbors.add(edge.toId)
                neighbors.add(edge.toId)
            } else if (edge.toId == centerNodeId) {
                directNeighbors.add(edge.fromId)
                neighbors.add(edge.fromId)
            }
        }
        // 二跳邻居
        val twoHopCandidates = mutableSetOf<String>()
        for (edge in edges) {
            if (edge.fromId in directNeighbors && edge.toId !in neighbors) {
                twoHopCandidates.add(edge.toId)
            } else if (edge.toId in directNeighbors && edge.fromId !in neighbors) {
                twoHopCandidates.add(edge.fromId)
            }
        }
        // 限制总数到 NEIGHBORHOOD_MAX_NODES
        // v0.8.2 修复：原实现对 1 跳邻居未限制，导致 50+ 直连场景超出 MAX_NODES。
        // 现 1 跳与 2 跳合并后统一裁剪，优先保留 1 跳（LinkedHashSet 保留插入顺序，
        // 直连邻居先加入，2 跳后加入，take(N) 自然优先保留近邻）。
        val remaining = GraphConstants.NEIGHBORHOOD_MAX_NODES - neighbors.size
        if (remaining > 0) {
            neighbors.addAll(twoHopCandidates.take(remaining))
        }
        // v0.8.2 修复：若 1 跳邻居已超出 MAX_NODES，截断到 MAX_NODES
        if (neighbors.size > GraphConstants.NEIGHBORHOOD_MAX_NODES) {
            val limited = neighbors.take(GraphConstants.NEIGHBORHOOD_MAX_NODES).toMutableSet()
            neighbors.clear()
            neighbors.addAll(limited)
        }

        val subNodes = nodes.filter { it.id in neighbors }
        for (node in subNodes) {
            nodeSubject[node.id] = node.subjectId ?: "unknown"
        }

        // 初始位置：中心节点放画布中心，邻居环绕
        val cx = width / 2f
        val cy = height / 2f
        val initialRadius = minOf(width, height) * 0.25f
        val rng = Random(centerNodeId.hashCode())

        val currentPositions = mutableMapOf<String, Offset>()
        for ((index, node) in subNodes.withIndex()) {
            currentPositions[node.id] = if (node.id == centerNodeId) {
                Offset(cx, cy)
            } else {
                val angle = 2 * PI * index / subNodes.size
                Offset(
                    (cx + initialRadius * cos(angle)).toFloat(),
                    (cy + initialRadius * sin(angle)).toFloat(),
                )
            }
        }

        // 力导向迭代
        val subEdges = edges.filter { it.fromId in neighbors && it.toId in neighbors }
        val maxIterations = GraphConstants.FORCE_ITERATIONS

        repeat(maxIterations) {
            val forces = mutableMapOf<String, Offset>()

            // 弹簧力（连接的节点互相吸引）
            for (edge in subEdges) {
                val p1 = currentPositions[edge.fromId] ?: continue
                val p2 = currentPositions[edge.toId] ?: continue
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                val idealLength = GraphConstants.FORCE_IDEAL_LENGTH
                val force = GraphConstants.FORCE_SPRING_K * (dist - idealLength) / dist
                val fx = force * dx
                val fy = force * dy
                forces[edge.fromId] = (forces[edge.fromId] ?: Offset.Zero) + Offset(fx, fy)
                forces[edge.toId] = (forces[edge.toId] ?: Offset.Zero) - Offset(fx, fy)
            }

            // 斥力（所有节点对）
            // v0.8.2 性能修复：复用 distSq 计算，避免 hypot 重复 sqrt(dx*dx+dy*dy)
            for (i in subNodes.indices) {
                for (j in (i + 1) until subNodes.size) {
                    val p1 = currentPositions[subNodes[i].id] ?: continue
                    val p2 = currentPositions[subNodes[j].id] ?: continue
                    val dx = p2.x - p1.x
                    val dy = p2.y - p1.y
                    val distSq = (dx * dx + dy * dy).coerceAtLeast(1f)
                    val dist = kotlin.math.sqrt(distSq)
                    val force = GraphConstants.FORCE_REPULSION_K / distSq
                    val fx = force * dx / dist
                    val fy = force * dy / dist
                    val id1 = subNodes[i].id
                    val id2 = subNodes[j].id
                    forces[id1] = (forces[id1] ?: Offset.Zero) - Offset(fx, fy)
                    forces[id2] = (forces[id2] ?: Offset.Zero) + Offset(fx, fy)
                }
            }

            // 中心引力（拉向画布中心，中心节点更强）
            for (node in subNodes) {
                val p = currentPositions[node.id] ?: continue
                val dx = cx - p.x
                val dy = cy - p.y
                val centerK = if (node.id == centerNodeId) 0.5f else GraphConstants.FORCE_CENTER_K
                val fx = dx * centerK
                val fy = dy * centerK
                forces[node.id] = (forces[node.id] ?: Offset.Zero) + Offset(fx, fy)
            }

            // 应用力（衰减步长）
            val damping = 0.6f
            for (node in subNodes) {
                val force = forces[node.id] ?: continue
                val p = currentPositions[node.id] ?: continue
                // 中心节点固定
                if (node.id == centerNodeId) continue
                currentPositions[node.id] = Offset(
                    p.x + force.x * damping,
                    p.y + force.y * damping,
                )
            }
        }

        positions.putAll(currentPositions)

        return LayoutResult(
            positions = positions,
            nodeSubject = nodeSubject,
            centerX = cx,
            centerY = cy,
        )
    }

    // ==================== 模式三：径向科目概览（保留原环形布局）====================

    /**
     * 径向科目概览布局（v0.7.4 原实现，v0.8.0 保留为远观模式）。
     *
     * 算法：
     * 1. 按 subjectId 分组节点（顶级分组）
     * 2. 每个科目分配一个扇区，扇区角度按节点数比例分配
     * 3. 每个科目扇区内，按 type 分子扇区
     * 4. 子扇区内节点沿圆弧均匀分布
     * 5. 半径按节点总数自适应
     */
    fun calculateRadialLayout(
        nodes: List<GraphNodeItem>,
        width: Float,
        height: Float,
    ): LayoutResult {
        val positions = mutableMapOf<String, Offset>()
        val nodeSubject = mutableMapOf<String, String>()
        val subjectSectors = mutableListOf<SubjectSector>()

        if (nodes.isEmpty()) {
            return LayoutResult(positions, nodeSubject, width / 2f, height / 2f)
        }

        val centerX = width / 2f
        val centerY = height / 2f

        // 半径自适应节点数
        val baseRadius = minOf(width, height) * when {
            nodes.size <= 50 -> 0.32f
            nodes.size <= 200 -> 0.40f
            nodes.size <= 1000 -> 0.50f
            else -> 0.65f
        }
        val innerRadius = baseRadius * 0.15f
        val outerRadius = baseRadius * 1.05f

        // 按 subjectId 分组
        val subjectGroups = mutableMapOf<String, MutableList<GraphNodeItem>>()
        for (node in nodes) {
            val subjectId = node.subjectId ?: "unknown"
            subjectGroups.getOrPut(subjectId) { mutableListOf() }.add(node)
            nodeSubject[node.id] = subjectId
        }

        val sortedSubjects = subjectGroups.entries.sortedByDescending { it.value.size }
        val totalNodes = nodes.size
        var currentAngle = -PI / 2
        val sectorGap = 0.04

        for ((subjectId, subjectNodes) in sortedSubjects) {
            val subjectSize = subjectNodes.size
            val sectorAngle = (2.0 * PI * subjectSize / totalNodes) - sectorGap
            if (sectorAngle <= 0 || subjectSize == 0) continue

            if (subjectId != "unknown") {
                subjectSectors.add(
                    SubjectSector(
                        subjectId = subjectId,
                        startAngle = currentAngle,
                        endAngle = currentAngle + sectorAngle,
                        innerRadius = innerRadius,
                        outerRadius = outerRadius,
                    ),
                )
            }

            // 科目内按 type 分子扇区
            val typeGroups = TYPE_ORDER.associateWith { type ->
                subjectNodes.filter { it.type == type }
            }.filter { it.value.isNotEmpty() }

            if (typeGroups.isEmpty()) {
                placeNodesOnArc(subjectNodes, centerX, centerY, baseRadius, currentAngle, sectorAngle, positions)
            } else {
                val typedTotal = typeGroups.values.sumOf { it.size }
                var typeCurrentAngle = currentAngle
                val typeGap = 0.02

                for ((_, typeNodes) in typeGroups) {
                    val typeSize = typeNodes.size
                    val typeSectorAngle = (sectorAngle * typeSize / typedTotal) - typeGap
                    if (typeSectorAngle <= 0 || typeSize == 0) continue

                    placeNodesOnArc(typeNodes, centerX, centerY, baseRadius, typeCurrentAngle, typeSectorAngle, positions)
                    typeCurrentAngle += typeSectorAngle + typeGap
                }
            }

            currentAngle += sectorAngle + sectorGap
        }

        return LayoutResult(
            positions = positions,
            nodeSubject = nodeSubject,
            subjectSectors = subjectSectors,
            centerX = centerX,
            centerY = centerY,
        )
    }

    /**
     * 沿圆弧均匀分布节点（v0.8.0 抽出，消除原代码 L624-636 / L647-658 重复）。
     */
    private fun placeNodesOnArc(
        nodes: List<GraphNodeItem>,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Double,
        angleSpan: Double,
        positions: MutableMap<String, Offset>,
    ) {
        val n = nodes.size
        if (n == 0) return
        for (i in 0 until n) {
            val nodeAngle = if (n == 1) {
                startAngle + angleSpan / 2
            } else {
                startAngle + angleSpan * i / (n - 1)
            }
            val node = nodes[i]
            positions[node.id] = Offset(
                (centerX + radius * cos(nodeAngle)).toFloat(),
                (centerY + radius * sin(nodeAngle)).toFloat(),
            )
        }
    }
}
