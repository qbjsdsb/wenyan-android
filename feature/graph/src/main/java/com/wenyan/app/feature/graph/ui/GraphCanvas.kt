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
import com.wenyan.app.feature.graph.GraphEdgeItem
import com.wenyan.app.feature.graph.GraphNodeItem
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// 节点半径
private val NODE_RADIUS_DP = 12f
private val NODE_TOUCH_RADIUS_DP = 24f

/**
 * 缩放范围：0.5x（看全局）~ 3.0x（看清单个节点标签）。
 */
private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 3.0f

/**
 * 知识图谱 Canvas 可视化组件（Spec C4.12）。
 *
 * v0.7.4 重构（修复用户反馈"知识图谱过于杂乱，看不清"）：
 *
 * 1. **分组径向布局** 替代单圆周布局：
 *    - 按 [GraphNodeItem.color] 分组（作家粉/体裁蓝/时段绿/流派紫/作品橙）
 *    - 每个色组占据一个扇区，扇区中心位于外环
 *    - 同组节点围绕扇区中心组成小圆环（"花瓣"布局）
 *    - 同色节点聚集 → 组内边短、组间边少交叉
 *
 * 2. **启用分类色**：节点颜色优先用 [GraphNodeItem.color]（实体预设色），
 *    仅当 color=0 时退化为按 R 值着色。原实现完全丢弃分类色，33 节点全按 R 值
 *    4 色映射，视觉同质化严重。
 *
 * 3. **双指缩放 + 单指平移**：
 *    - detectTransformGestures 同时处理 zoom（双指）和 pan（单指/双指）
 *    - scale 限制在 [MIN_SCALE]..[MAX_SCALE]，避免过度缩放失真或丢失上下文
 *    - 应用 graphicsLayer { translation = offset; scaleX = scaleY = scale }
 *    - 节点点击触控区同样变换，确保缩放后点击位置精准
 *
 * 4. **节点标签智能定位**：标签放在节点远离组中心的一侧（径向外），
 *    减少与节点本身和其他节点标签的重叠。
 *
 * **NF-UA1 无障碍**（保留）：Canvas 只负责绘制，节点上方叠加透明 Box 承接点击 + semantics。
 *
 * @param nodes 图谱节点列表（含 R 值与分类色）
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

    // 缩放与平移状态（v0.7.4 新增）
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

        // 计算节点位置（分组径向布局）
        val positions = remember(nodes, canvasWidth, canvasHeight) {
            calculateGroupedRadialLayout(nodes, canvasWidth, canvasHeight)
        }

        // 每个节点相对于其所属组中心的单位向量（用于标签径向外定位）
        val nodeOutwardDirections = remember(nodes, positions) {
            calculateOutwardDirections(nodes, positions)
        }

        // 预测量标签文本（避免每帧重复测量）
        val textLayouts = remember(nodes, labelColor) {
            nodes.associate { node ->
                node.id to textMeasurer.measure(
                    AnnotatedString(node.label),
                    // NF-UA3 修复 + v0.7.4：12.sp 是 Android 无障碍最小可读字号。
                    TextStyle(fontSize = 12.sp, color = labelColor),
                )
            }
        }

        // 弱节点 ID 集合（R < 0.5 且 R > 0）
        val weakNodeIds = remember(nodes) {
            nodes.filter { it.retrievability > 0f && it.retrievability < 0.5f }
                .map { it.id }
                .toSet()
        }

        // ── Canvas 只负责绘制（边、节点、标签），不再处理点击 ──
        // 应用缩放/平移变换：translation + scale
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offset.x
                    translationY = offset.y
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            // 绘制边
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

            // 绘制节点
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

                // 节点标签：径向外定位，减少与节点本身和其他节点标签的重叠
                val textLayout = textLayouts[node.id] ?: return@forEach
                val outward = nodeOutwardDirections[node.id] ?: Offset(0f, 1f)
                // 标签锚点：节点中心 + (节点半径 + 4px) 沿径向外方向
                // 然后按文本宽高修正对齐到标签左上角
                val labelAnchor = Offset(
                    pos.x + (nodeRadiusPx + 4f) * outward.x,
                    pos.y + (nodeRadiusPx + 4f) * outward.y,
                )
                val labelTopLeft = when {
                    // 向右下方：标签左上角 = anchor
                    outward.x >= 0f && outward.y >= 0f ->
                        Offset(labelAnchor.x, labelAnchor.y - textLayout.size.height / 2f)
                    // 向右上方
                    outward.x >= 0f && outward.y < 0f ->
                        Offset(labelAnchor.x, labelAnchor.y - textLayout.size.height)
                    // 向左下方
                    outward.x < 0f && outward.y >= 0f ->
                        Offset(labelAnchor.x - textLayout.size.width, labelAnchor.y - textLayout.size.height / 2f)
                    // 向左上方
                    else ->
                        Offset(labelAnchor.x - textLayout.size.width, labelAnchor.y - textLayout.size.height)
                }
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = labelTopLeft,
                )
            }
        }

        // ── NF-UA1 修复：节点点击改为 Box 叠加 ──
        // 注意：点击 Box 必须应用相同的 graphicsLayer 变换，否则缩放后点击位置错位。
        // 触控区域直径 = 2 * touchRadiusPx（48dp），符合 WCAG 最小触控目标。
        nodes.forEach { node ->
            val pos = positions[node.id] ?: return@forEach
            // 应用 scale/offset 变换得到屏幕坐标
            val screenX = (pos.x + offset.x) * scale
            val screenY = (pos.y + offset.y) * scale
            // 触控区域也按 scale 缩放，保证缩放后点击区域与视觉节点一致
            val touchRadiusScaled = touchRadiusPx * scale
            val xDp = with(density) { (screenX - touchRadiusScaled).toDp() }
            val yDp = with(density) { (screenY - touchRadiusScaled).toDp() }
            val touchSizeScaledDp = with(density) { (touchRadiusScaled * 2).toDp() }
            Box(
                modifier = Modifier
                    .size(touchSizeScaledDp.coerceAtLeast(24.dp))
                    .semantics {
                        role = Role.Button
                        contentDescription = buildString {
                            append(node.label)
                            if (!node.subtitle.isNullOrBlank()) append("（").append(node.subtitle).append("）")
                        }
                    }
                    .align(Alignment.TopStart)
                    .absoluteOffset(x = xDp, y = yDp)
                    .clip(CircleShape)
                    .clickable { onNodeClick(node.id) },
            )
        }
    }
}

/**
 * 分组径向布局（v0.7.4 重构）。
 *
 * 算法：
 * 1. 按 [GraphNodeItem.color] 分组（视觉分类：作家粉/体裁蓝/时段绿/流派紫/作品橙）
 * 2. 色值为 0（未指定）的节点单独归为"未分类"组
 * 3. 每个组分配一个扇区中心角（均分 2π）
 * 4. 组中心放置在以画布中心为原点、外环半径 R1 的圆上
 * 5. 同组节点围绕组中心组成小圆环（半径 R2，由组内节点数决定）
 *
 * 结果：同色节点聚集形成"花瓣"，组内边短、组间边少交叉。
 *
 * 特殊情况：
 * - 1 个节点：居中
 * - 2 个节点：水平对称
 * - 仅 1 个组：组内按圆周布局（退化情形，等价原算法）
 */
private fun calculateGroupedRadialLayout(
    nodes: List<GraphNodeItem>,
    width: Float,
    height: Float,
): Map<String, Offset> {
    val centerX = width / 2f
    val centerY = height / 2f

    if (nodes.size == 1) return mapOf(nodes[0].id to Offset(centerX, centerY))
    if (nodes.size == 2) {
        val radius = min(width, height) * 0.3f
        return mapOf(
            nodes[0].id to Offset(centerX - radius, centerY),
            nodes[1].id to Offset(centerX + radius, centerY),
        )
    }

    // 按 color 分组（color=0 视为"未分类"单独一组）
    val groups = nodes.groupBy { it.color }
        .toList()
        .sortedWith(compareByDescending<Pair<Int, List<GraphNodeItem>>> { it.second.size }.thenBy { it.first })

    // 仅一组时退化为单圆周布局（原算法），保持兼容
    if (groups.size == 1) {
        return calculateSingleCircleLayout(nodes, centerX, centerY, min(width, height) * 0.4f)
    }

    // 外环半径：组中心距画布中心的距离
    // 留出 0.15 边距给节点+标签，避免贴边
    val outerRadius = min(width, height) * 0.34f
    // 内环半径基数：同组节点围绕组中心的小圆环半径
    // 按组大小开根号缩放，避免大组内节点过挤
    val innerRadiusBase = min(width, height) * 0.10f

    val positions = mutableMapOf<String, Offset>()
    groups.forEachIndexed { groupIdx, (_, groupNodes) ->
        val groupAngle = (2.0 * Math.PI * groupIdx / groups.size).toFloat()
        val groupCenterX = centerX + outerRadius * cos(groupAngle)
        val groupCenterY = centerY + outerRadius * sin(groupAngle)

        if (groupNodes.size == 1) {
            positions[groupNodes[0].id] = Offset(groupCenterX, groupCenterY)
        } else {
            // 组内小圆环：半径按组大小开根号缩放，避免大组内节点过挤
            // 13 个节点时半径 ~ innerRadiusBase * 1.6（sqrt(13/5)）
            val innerRadius = innerRadiusBase * sqrt(groupNodes.size.toFloat() / 5f).coerceIn(0.6f, 1.8f)
            groupNodes.forEachIndexed { idx, node ->
                val angleInGroup = (2.0 * Math.PI * idx / groupNodes.size).toFloat()
                positions[node.id] = Offset(
                    x = groupCenterX + innerRadius * cos(angleInGroup),
                    y = groupCenterY + innerRadius * sin(angleInGroup),
                )
            }
        }
    }
    return positions
}

/**
 * 单圆周布局（v0.7.4 兼容保留，仅当所有节点同色时退化使用）。
 */
private fun calculateSingleCircleLayout(
    nodes: List<GraphNodeItem>,
    centerX: Float,
    centerY: Float,
    radius: Float,
): Map<String, Offset> {
    return nodes.mapIndexed { index, node ->
        val angle = (2 * Math.PI * index / nodes.size).toFloat()
        node.id to Offset(
            x = centerX + radius * cos(angle),
            y = centerY + radius * sin(angle),
        )
    }.toMap()
}

/**
 * 计算每个节点相对于其所属组中心的"径向外"单位向量（v0.7.4 新增）。
 *
 * 用于标签定位：标签放在节点远离组中心的一侧，避免与节点和其他节点标签重叠。
 *
 * 算法：
 * 1. 按 color 重新分组，计算每组中心（所有节点位置平均值）
 * 2. 对每个节点，方向 = (节点位置 - 组中心) 归一化
 * 3. 退化情形（节点恰好位于组中心）默认向下 (0, 1)
 */
private fun calculateOutwardDirections(
    nodes: List<GraphNodeItem>,
    positions: Map<String, Offset>,
): Map<String, Offset> {
    val groups = nodes.groupBy { it.color }
    val result = mutableMapOf<String, Offset>()
    groups.forEach { (_, groupNodes) ->
        if (groupNodes.isEmpty()) return@forEach
        // 组中心 = 所有节点位置平均值
        val groupCenter = groupNodes
            .mapNotNull { positions[it.id] }
            .fold(Offset.Zero) { acc, pos -> acc + pos } / groupNodes.size.toFloat()

        groupNodes.forEach { node ->
            val pos = positions[node.id] ?: return@forEach
            val delta = pos - groupCenter
            val length = sqrt(delta.x * delta.x + delta.y * delta.y)
            // 退化情形：节点恰在组中心，默认向下
            result[node.id] = if (length < 1f) {
                Offset(0f, 1f)
            } else {
                Offset(delta.x / length, delta.y / length)
            }
        }
    }
    return result
}

/**
 * 解析节点显示颜色（v0.7.4 重构）。
 *
 * 优先级：
 * 1. **节点实体预设色**（[GraphNodeItem.color] != 0）：直接使用，保留分类色
 *    （作家粉 / 体裁蓝 / 时段绿 / 流派紫 / 作品橙）
 * 2. **退化为 R 值映射色**（color == 0）：按 R 值四档映射
 *    （已掌握 primary / 需巩固 tertiary / 薄弱 error / 未学习 outline）
 *
 * 原实现完全丢弃实体色，33 节点全部按 R 值 4 色映射，视觉同质化严重。
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
