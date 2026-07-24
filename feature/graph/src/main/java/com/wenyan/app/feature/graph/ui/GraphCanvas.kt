package com.wenyan.app.feature.graph.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wenyan.app.core.data.seed.GraphSkeleton
import com.wenyan.app.feature.graph.GraphEdgeItem
import com.wenyan.app.feature.graph.GraphNodeItem
import kotlin.math.abs

// 节点半径
private val NODE_RADIUS_DP = 12f
private val NODE_TOUCH_RADIUS_DP = 24f

/**
 * 缩放范围：0.5x（看全局）~ 3.0x（看清单个节点标签）。
 */
private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 3.0f

// 文学史时间轴范围（覆盖现当代文学全周期）
private const val TIMELINE_MIN_YEAR = 1915
private const val TIMELINE_MAX_YEAR = 2030

// 泳道 Y 轴占比（基于 Canvas 高度）
private const val LANE_Y_PERIOD = 0.16f   // 时段泳道（顶部，作为时间标尺）
private const val LANE_Y_SCHOOL = 0.38f   // 流派泳道
private const val LANE_Y_AUTHOR = 0.62f   // 作家泳道（主体）
private const val LANE_Y_GENRE = 0.86f    // 体裁泳道（底部）

// 同泳道节点重叠避让：相邻节点 X 距离 < 此值时进行 Y 偏移
private const val LANE_COLLISION_THRESHOLD_PX = 80f
private const val LANE_COLLISION_OFFSET_PX = 22f

// 时间轴刻度年份（与 7 个文学史分期对齐）
private val TIMELINE_TICK_YEARS = intArrayOf(1917, 1927, 1937, 1949, 1966, 1976, 1989, 2000)

/**
 * 知识图谱 Canvas 可视化组件（Spec C4.12）。
 *
 * v0.7.6 重构（基于用户反馈"知识图谱还是不够有逻辑，不够美丽，也不够能帮助学习"）：
 *
 * 1. **文学史时间轴布局** 替代 v0.7.4 分组径向布局：
 *    - **横轴 = 时间**（1915~2030，覆盖现当代文学全周期）
 *      节点按时间字段（生卒年/年代/起止年）在 X 轴定位
 *    - **纵轴 = 泳道**（4 条）：
 *      - Lane 0 时段（顶部，作为时间标尺）
 *      - Lane 1 流派
 *      - Lane 2 作家（主体）
 *      - Lane 3 体裁（底部）
 *    - 跨类边纵向连接泳道，形成"作家↔流派↔体裁↔时段"知识链路
 *
 * 2. **顶部时间刻度线**：在 Canvas 顶部绘制 8 个关键年份刻度
 *    （1917/1927/1937/1949/1966/1976/1989/2000），对应 7 个文学史分期边界
 *
 * 3. **同泳道重叠避让**：相邻节点 X 距离 < [LANE_COLLISION_THRESHOLD_PX] 时，
 *    对后放置节点进行 Y 偏移（[LANE_COLLISION_OFFSET_PX]），避免标签重叠
 *
 * 4. **保留 v0.7.4 交互**：双指缩放 + 单指平移、节点点击 Box 叠加（NF-UA1 无障碍）、
 *    分类色优先 + R 值退化、薄弱节点光晕
 *
 * 5. **保留 v0.7.4 标签智能定位**：标签放在节点下方（统一向下，避免与时间刻度重叠）
 *
 * v0.7.6 流畅性优化：
 * - **修复变换 bug**：v0.7.4 遗留——Canvas 用 graphicsLayer（先缩放再平移），
 *   节点点击 Box 手动算 (pos+offset)*scale（先平移再缩放），变换公式不一致，
 *   缩放+平移时点击位置错位（偏差 = offset * (scale - 1)）。
 *   现统一到外层 Box 的 graphicsLayer，Canvas 和点击层共享同一变换。
 * - **手势性能提升**：原实现每次手势触发 BoxWithConstraints 重组，40+ 节点 Box
 *   重新计算屏幕坐标 + px→Dp 转换。现节点 Box 用未变换坐标定位，Dp 偏移预缓存，
 *   手势变化只触发 graphicsLayer 重新应用（GPU 层），不触发 Compose 重组布局。
 *
 * **优势对比**（v0.7.4 分组径向 → v0.7.6 时间轴）：
 * - 逻辑性：径向布局按"颜色分组"无内在逻辑；时间轴按"历史时间"组织，符合文学史认知
 * - 学习价值：径向布局难以看出作家/流派/时段的时序关系；时间轴直观展示代际传承
 * - 美观性：径向布局花瓣形状虽美但节点密集；时间轴泳道分明，跨类边形成纵向网络
 *
 * @param nodes 图谱节点列表（含 R 值、分类色、metadata 时间字段）
 * @param edges 图谱边列表
 * @param onNodeClick 节点点击回调（参数为节点 ID）
 * @param modifier 修饰符
 */
@Composable
fun GraphCanvas(
    nodes: List<GraphNodeItem>,
    edges: List<GraphEdgeItem>,
    onNodeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (nodes.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 从主题获取颜色角色（避免在 DrawScope 中访问 MaterialTheme）
    val colorScheme = MaterialTheme.colorScheme
    val masteredColor = colorScheme.primary
    val consolidatingColor = colorScheme.tertiary
    val weakColor = colorScheme.error
    val unlearnedColor = colorScheme.outline
    val labelColor = colorScheme.onSurface
    val edgeColor = colorScheme.outlineVariant
    val weakHaloColor = colorScheme.error.copy(alpha = 0.2f)
    val weakEdgeColor = colorScheme.error.copy(alpha = 0.6f)
    val timelineTickColor = colorScheme.outline
    val timelineLabelColor = colorScheme.onSurfaceVariant
    val laneDividerColor = colorScheme.outlineVariant.copy(alpha = 0.3f)

    // 缩放与平移状态（v0.7.4 保留）
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // 双指缩放 + 单指/双指平移。centroid 会自动取双指中点，
                // 单指时 centroid = 触点位置。zoom > 1 表示双指张开，pan 是位移增量。
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // 以手势中心为锚点缩放：先平移到原点缩放再平移回去，避免节点"飞走"
                    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    val scaleFactor = newScale / scale
                    offset = (offset + centroid) * scaleFactor - centroid
                    scale = newScale
                    offset += pan
                }
            }
            // NF-UA1 修复：整个图谱加 semantics，让 TalkBack 朗读"知识图谱，N 个节点"。
            .semantics { contentDescription = "知识图谱，${nodes.size} 个节点。双指缩放，单指拖动" },
    ) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        if (canvasWidth <= 0f || canvasHeight <= 0f) return@BoxWithConstraints

        val nodeRadiusPx = with(density) { NODE_RADIUS_DP.dp.toPx() }
        val touchRadiusPx = with(density) { NODE_TOUCH_RADIUS_DP.dp.toPx() }

        // v0.7.6 重构：计算节点位置（文学史时间轴泳道布局）
        val positions = remember(nodes, canvasWidth, canvasHeight) {
            calculateTimelineLayout(nodes, canvasWidth, canvasHeight)
        }

        // 预测量标签文本（避免每帧重复测量）
        val textLayouts = remember(nodes, labelColor) {
            nodes.associate { node ->
                node.id to textMeasurer.measure(
                    AnnotatedString(node.label),
                    // NF-UA3 修复 + v0.7.4：12.sp 是 Android 无障碍最小可读字号。
                    TextStyle(fontSize = 11.sp, color = labelColor),
                )
            }
        }

        // 预测量时间刻度标签
        val tickLabelLayouts = remember(timelineLabelColor) {
            TIMELINE_TICK_YEARS.associateWith { year ->
                textMeasurer.measure(
                    AnnotatedString(year.toString()),
                    TextStyle(fontSize = 10.sp, color = timelineLabelColor),
                )
            }
        }

        // 弱节点 ID 集合（R < 0.5 且 R > 0）
        val weakNodeIds = remember(nodes) {
            nodes.filter { it.retrievability > 0f && it.retrievability < 0.5f }
                .map { it.id }
                .toSet()
        }

        // v0.7.6 流畅性优化：预计算节点点击区域的 Dp 偏移（未变换坐标）。
        // 点击 Box 用 absoluteOffset 定位在 graphicsLayer 变换前的坐标系中，
        // 经外层 graphicsLayer 变换后与 Canvas 渲染位置完全对齐。
        // 只在 positions 变化时重算，手势变化不触发重算。
        val touchSizeDp = with(density) { (touchRadiusPx * 2f).toDp() }
        val touchOffsetsDp = remember(positions, touchRadiusPx, density) {
            positions.mapValues { (_, pos) ->
                with(density) {
                    Pair(
                        (pos.x - touchRadiusPx).toDp(),
                        (pos.y - touchRadiusPx).toDp(),
                    )
                }
            }
        }

        // ── v0.7.6 流畅性优化：Canvas + 点击层共享 graphicsLayer ──
        // 修复 v0.7.4 遗留 bug：原 Canvas 用 graphicsLayer（先缩放再平移），
        // 节点 Box 手动算 (pos+offset)*scale（先平移再缩放），变换公式不一致，
        // 缩放+平移时点击位置错位。现统一到外层 Box 的 graphicsLayer，
        // Canvas 和点击层共享同一变换，位置完全一致。
        //
        // 性能提升：手势变化只触发 graphicsLayer 重新应用（GPU 层合成），
        // 不触发 Compose 重组布局，40+ 节点点击 Box 无需重新计算坐标。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offset.x
                    translationY = offset.y
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            // ── 1. Canvas 绘制（时间轴、边、节点、标签）──
            Canvas(modifier = Modifier.fillMaxSize()) {
                // ── 1.1 时间轴刻度线（顶部）──
                val timelineY = canvasHeight * 0.06f
                TIMELINE_TICK_YEARS.forEach { year ->
                    val x = yearToX(year.toFloat(), canvasWidth)
                    drawLine(
                        color = timelineTickColor,
                        start = Offset(x, timelineY),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 0.8f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f)),
                    )
                    val tickLabel = tickLabelLayouts[year]
                    if (tickLabel != null) {
                        drawText(
                            textLayoutResult = tickLabel,
                            topLeft = Offset(
                                x - tickLabel.size.width / 2f,
                                timelineY - tickLabel.size.height - 2f,
                            ),
                        )
                    }
                }

                // ── 1.2 泳道分割线（淡）──
                val laneYs = floatArrayOf(
                    canvasHeight * LANE_Y_PERIOD,
                    canvasHeight * LANE_Y_SCHOOL,
                    canvasHeight * LANE_Y_AUTHOR,
                    canvasHeight * LANE_Y_GENRE,
                )
                laneYs.forEach { laneY ->
                    drawLine(
                        color = laneDividerColor,
                        start = Offset(0f, laneY),
                        end = Offset(canvasWidth, laneY),
                        strokeWidth = 0.5f,
                    )
                }

                // ── 1.3 边（含薄弱边高亮）──
                edges.forEach { edge ->
                    val from = positions[edge.fromId]
                    val to = positions[edge.toId]
                    if (from != null && to != null) {
                        val isWeak = edge.fromId in weakNodeIds || edge.toId in weakNodeIds
                        drawLine(
                            color = if (isWeak) weakEdgeColor else edgeColor,
                            start = from,
                            end = to,
                            strokeWidth = if (isWeak) 3f else 1.5f,
                        )
                    }
                }

                // ── 1.4 节点 + 标签 ──
                nodes.forEach { node ->
                    val pos = positions[node.id] ?: return@forEach
                    val color = resolveNodeColor(
                        node = node,
                        masteredColor = masteredColor,
                        consolidatingColor = consolidatingColor,
                        weakColor = weakColor,
                        unlearnedColor = unlearnedColor,
                    )

                    // 外圈光晕（薄弱节点）
                    if (node.id in weakNodeIds) {
                        drawCircle(
                            color = weakHaloColor,
                            radius = nodeRadiusPx + 6f,
                            center = pos,
                        )
                    }

                    // 节点圆
                    drawCircle(
                        color = color,
                        radius = nodeRadiusPx,
                        center = pos,
                    )

                    // 节点标签：v0.7.6 统一向下放置，避免与顶部时间刻度重叠
                    // 标签锚点：节点中心下方 (nodeRadiusPx + 4f) px
                    val textLayout = textLayouts[node.id] ?: return@forEach
                    val labelTopLeft = Offset(
                        pos.x - textLayout.size.width / 2f,
                        pos.y + nodeRadiusPx + 4f,
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = labelTopLeft,
                    )
                }
            }

            // ── 2. 节点点击层（位置与 Canvas 内部坐标一致）──
            // NF-UA1：点击 Box 用 absoluteOffset 定位在未变换坐标系中，
            // 经外层 graphicsLayer 变换后与 Canvas 渲染位置完全对齐。
            // 触控区域固定 48dp（WCAG 最小标准），不随 scale 缩放。
            nodes.forEach { node ->
                val offsets = touchOffsetsDp[node.id] ?: return@forEach
                Box(
                    modifier = Modifier
                        .size(touchSizeDp)
                        .align(Alignment.TopStart)
                        .absoluteOffset(x = offsets.first, y = offsets.second)
                        .clip(CircleShape)
                        .semantics {
                            role = Role.Button
                            contentDescription = buildString {
                                append(node.label)
                                if (!node.subtitle.isNullOrBlank()) {
                                    append("（").append(node.subtitle).append("）")
                                }
                            }
                        }
                        .clickable { onNodeClick(node.id) },
                )
            }
        }
    }
}

// ==================== v0.7.6 时间轴布局算法 ====================

/**
 * 文学史时间轴泳道布局（v0.7.6 重构）。
 *
 * 算法：
 * 1. 按节点类型/分类分泳道：
 *    - Lane 0 (Y=0.16): 时段节点（type=CONCEPT 且 metadata["dimension"]="period"）
 *    - Lane 1 (Y=0.38): 流派节点（type=CONCEPT 且 metadata["type"] 为 "society"/"school"/"movement"）
 *    - Lane 2 (Y=0.62): 作家节点（type=AUTHOR）
 *    - Lane 3 (Y=0.86): 体裁节点（type=CONCEPT 且 metadata["dimension"]="genre"）
 *    - 其他节点：归入作家泳道（最宽）
 * 2. X 轴定位：
 *    - 作家：(birthYear + deathYear) / 2
 *    - 流派：metadata["year"]（如 "1930s" → 1930）
 *    - 时段：(startYear + endYear) / 2
 *    - 体裁：无时间字段，沿 X 轴均匀分布
 * 3. 同泳道重叠避让：相邻节点 X 距离过近时，对后放置节点进行 Y 偏移
 *
 * 结果：节点在二维平面形成"时间×类型"矩阵，跨类边纵向连接形成知识网络。
 */
private fun calculateTimelineLayout(
    nodes: List<GraphNodeItem>,
    width: Float,
    height: Float,
): Map<String, Offset> {
    val positions = mutableMapOf<String, Offset>()
    if (nodes.isEmpty()) return positions

    // 按节点类型/分类分泳道，并按时间排序
    val periodNodes = mutableListOf<GraphNodeItem>()
    val schoolNodes = mutableListOf<GraphNodeItem>()
    val authorNodes = mutableListOf<GraphNodeItem>()
    val genreNodes = mutableListOf<GraphNodeItem>()
    val otherNodes = mutableListOf<GraphNodeItem>()

    nodes.forEach { node ->
        val dimension = node.metadata?.get("dimension")
        val typeMeta = node.metadata?.get("type")
        when {
            // 时段节点：dimension=period
            dimension == "period" -> periodNodes.add(node)
            // 流派/社团节点：metadata.type = society/school/movement
            typeMeta == "society" || typeMeta == "school" || typeMeta == "movement" -> schoolNodes.add(node)
            // 作家节点
            node.type == "AUTHOR" -> authorNodes.add(node)
            // 体裁节点：dimension=genre
            dimension == "genre" -> genreNodes.add(node)
            else -> otherNodes.add(node)
        }
    }

    // 时段泳道：按时段起止年中位数排序，X 轴对应时间
    periodNodes.sortBy { extractPeriodMidYear(it) }
    placeNodesInLane(
        nodes = periodNodes,
        laneY = height * LANE_Y_PERIOD,
        width = width,
        positions = positions,
        yearExtractor = ::extractPeriodMidYear,
    )

    // 流派泳道：按年代排序
    val sortedSchoolNodes = schoolNodes.sortedBy { extractSchoolYear(it) }
    placeNodesInLane(
        nodes = sortedSchoolNodes,
        laneY = height * LANE_Y_SCHOOL,
        width = width,
        positions = positions,
        yearExtractor = ::extractSchoolYear,
    )

    // 作家泳道：按生卒年中位数排序
    val sortedAuthorNodes = authorNodes.sortedBy { extractAuthorMidYear(it) }
    placeNodesInLane(
        nodes = sortedAuthorNodes,
        laneY = height * LANE_Y_AUTHOR,
        width = width,
        positions = positions,
        yearExtractor = ::extractAuthorMidYear,
    )

    // 体裁泳道：均匀分布（无时间字段）
    genreNodes.forEachIndexed { idx, node ->
        val x = if (genreNodes.size <= 1) width / 2f
        else width * (idx + 1f) / (genreNodes.size + 1f)
        positions[node.id] = Offset(x, height * LANE_Y_GENRE)
    }

    // 其他节点（无明确分类）：放到作家泳道，X 轴均匀分布
    if (otherNodes.isNotEmpty()) {
        otherNodes.forEachIndexed { idx, node ->
            val x = width * (idx + 1f) / (otherNodes.size + 1f)
            positions[node.id] = Offset(x, height * LANE_Y_AUTHOR)
        }
    }

    return positions
}

/**
 * 在指定泳道内放置节点，应用时间→X 轴映射 + 同泳道重叠避让。
 *
 * 避让算法：
 * - 节点按时间排序后依次放置
 * - 若与前一个节点 X 距离 < [LANE_COLLISION_THRESHOLD_PX]，向后偏移 Y 轴
 *   （偏移量随连续碰撞次数递增，避免堆叠）
 */
private fun placeNodesInLane(
    nodes: List<GraphNodeItem>,
    laneY: Float,
    width: Float,
    positions: MutableMap<String, Offset>,
    yearExtractor: (GraphNodeItem) -> Float,
) {
    var lastX = -Float.MAX_VALUE
    var collisionStreak = 0
    nodes.forEach { node ->
        val year = yearExtractor(node)
        val x = yearToX(year, width)
        // 同泳道重叠避让
        val yOffset = if (abs(x - lastX) < LANE_COLLISION_THRESHOLD_PX) {
            collisionStreak++
            // 交替向上下偏移，连续碰撞递增偏移量
            val sign = if (collisionStreak % 2 == 0) -1 else 1
            sign * LANE_COLLISION_OFFSET_PX * ((collisionStreak + 1) / 2)
        } else {
            collisionStreak = 0
            0f
        }
        positions[node.id] = Offset(x, laneY + yOffset)
        lastX = x
    }
}

/**
 * 提取作家节点的中位年份（生卒年中位数）。
 *
 * 退化策略：
 * 1. metadata["birthYear"] + metadata["deathYear"] 中位数
 * 2. 仅 birthYear：birthYear + 30（假设创作活跃期 30 年后）
 * 3. 无时间字段：返回 [TIMELINE_MIN_YEAR]（最左侧）
 */
private fun extractAuthorMidYear(node: GraphNodeItem): Float {
    val meta = node.metadata ?: return TIMELINE_MIN_YEAR.toFloat()
    val birth = meta[GraphSkeleton.META_KEY_BIRTH_YEAR]?.toFloatOrNull()
    val death = meta[GraphSkeleton.META_KEY_DEATH_YEAR]?.toFloatOrNull()
    return when {
        birth != null && death != null -> (birth + death) / 2f
        birth != null -> birth + 30f
        death != null -> death - 30f
        else -> TIMELINE_MIN_YEAR.toFloat()
    }
}

/**
 * 提取流派/社团节点的年代。
 *
 * 退化策略：
 * 1. metadata["year"] 解析（如 "1930s" → 1930, "1985" → 1985）
 * 2. 无 year 字段：返回 [TIMELINE_MIN_YEAR]
 */
private fun extractSchoolYear(node: GraphNodeItem): Float {
    val raw = node.metadata?.get("year") ?: return TIMELINE_MIN_YEAR.toFloat()
    // 解析 "1930s" / "1985" / "1930s" 等格式
    val digits = raw.takeWhile { it.isDigit() }
    return digits.toFloatOrNull() ?: TIMELINE_MIN_YEAR.toFloat()
}

/**
 * 提取时段节点的中位年份。
 *
 * 退化策略：
 * 1. metadata["startYear"] + metadata["endYear"] 中位数
 * 2. 仅 startYear：startYear
 * 3. 无时间字段：返回 [TIMELINE_MIN_YEAR]
 */
private fun extractPeriodMidYear(node: GraphNodeItem): Float {
    val meta = node.metadata ?: return TIMELINE_MIN_YEAR.toFloat()
    val start = meta[GraphSkeleton.META_KEY_START_YEAR]?.toFloatOrNull()
    val end = meta[GraphSkeleton.META_KEY_END_YEAR]?.toFloatOrNull()
    return when {
        start != null && end != null -> (start + end) / 2f
        start != null -> start
        end != null -> end
        else -> TIMELINE_MIN_YEAR.toFloat()
    }
}

/**
 * 年份 → X 轴像素坐标转换。
 *
 * 线性映射：x = (year - MIN_YEAR) / (MAX_YEAR - MIN_YEAR) * width
 * 留 5% 边距避免贴边
 */
private fun yearToX(year: Float, width: Float): Float {
    val padding = width * 0.05f
    val usableWidth = width - 2 * padding
    val ratio = (year - TIMELINE_MIN_YEAR) / (TIMELINE_MAX_YEAR - TIMELINE_MIN_YEAR)
    return padding + ratio.coerceIn(0f, 1f) * usableWidth
}

/**
 * 解析节点显示颜色（v0.7.4 实现，v0.7.6 保留）。
 *
 * 优先级：
 * 1. **节点实体预设色**（[GraphNodeItem.color] != 0）：直接使用，保留分类色
 *    （作家粉 / 体裁蓝 / 时段绿 / 流派紫 / 作品橙）
 * 2. **退化为 R 值映射色**（color == 0）：按 R 值四档映射
 *    （已掌握 primary / 需巩固 tertiary / 薄弱 error / 未学习 outline）
 *
 * 注意：薄弱节点（R < 0.5 且 R > 0）仍会绘制 error 色光晕，与节点填充色无关。
 * 这样既能看到分类（节点填充色），又能看到薄弱状态（外圈光晕）。
 */
private fun resolveNodeColor(
    node: GraphNodeItem,
    masteredColor: Color,
    consolidatingColor: Color,
    weakColor: Color,
    unlearnedColor: Color,
): Color {
    // 实体预设色优先（保留分类视觉）
    if (node.color != 0) return Color(node.color)
    // 退化：按 R 值映射
    return when {
        node.retrievability >= 0.8f -> masteredColor
        node.retrievability >= 0.5f -> consolidatingColor
        node.retrievability > 0f -> weakColor
        else -> unlearnedColor
    }
}
