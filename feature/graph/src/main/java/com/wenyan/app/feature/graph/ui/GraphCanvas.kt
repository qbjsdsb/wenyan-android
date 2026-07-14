package com.wenyan.app.feature.graph.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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

// 节点半径
private val NODE_RADIUS_DP = 12f
private val NODE_TOUCH_RADIUS_DP = 24f

/**
 * 知识图谱 Canvas 可视化组件（Spec C4.12）。
 *
 * 功能：
 * - 圆形布局排列节点
 * - R 值颜色映射（已掌握 primary / 需巩固 tertiary / 薄弱 error / 未学习 outline）
 * - 边连线（薄弱子图 error 加粗）
 * - 节点标签（onSurface）
 * - 点击节点触发回调
 *
 * @param nodes 图谱节点列表（含 R 值）
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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        if (canvasWidth <= 0f || canvasHeight <= 0f) return@BoxWithConstraints

        val nodeRadiusPx = with(density) { NODE_RADIUS_DP.dp.toPx() }
        val touchRadiusPx = with(density) { NODE_TOUCH_RADIUS_DP.dp.toPx() }

        // 计算节点位置（圆形布局）
        val positions = remember(nodes, canvasWidth, canvasHeight) {
            calculateCircularLayout(nodes, canvasWidth, canvasHeight)
        }

        // 预测量标签文本（避免每帧重复测量）
        val textLayouts = remember(nodes, labelColor) {
            nodes.associate { node ->
                node.id to textMeasurer.measure(
                    AnnotatedString(node.label),
                    // NF-UA3 修复：9.sp 低于 WCAG 推荐最小 12.sp，视力不佳用户难阅读。
                    // 12.sp 是 Android 无障碍最小可读字号。
                    TextStyle(fontSize = 12.sp, color = labelColor),
                )
            }
        }

        // 弱节点 ID 集合（R < 0.5）
        val weakNodeIds = remember(nodes) {
            nodes.filter { it.retrievability > 0f && it.retrievability < 0.5f }
                .map { it.id }
                .toSet()
        }

        // NF-UC5 修复：rememberUpdatedState 保持最新 nodes 引用，
        // 让 pointerInput(Unit) 内的 lambda 总能读到最新 nodes，无需重启手势检测。
        val currentNodes by rememberUpdatedState(nodes)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // NF-UC5 修复：pointerInput(nodes) 在 nodes 列表变化时重启手势检测协程，
                // R 值刷新瞬间（nodes 引用变化）tap 事件可能丢失。
                // 改为 pointerInput(Unit) 让手势检测协程只启动一次，配合 rememberUpdatedState
                // 在 lambda 内读取 currentNodes 而非闭包捕获的 nodes。
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        currentNodes.find { node ->
                            val pos = positions[node.id]
                            pos != null && (tapOffset - pos).getDistance() <= touchRadiusPx
                        }?.let { onNodeClick(it.id) }
                    }
                },
        ) {
            // ── 绘制边 ──
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

            // ── 绘制节点 ──
            nodes.forEach { node ->
                val pos = positions[node.id] ?: return@forEach
                val color = colorForRetrievability(
                    r = node.retrievability,
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

                // 节点标签
                val textLayout = textLayouts[node.id] ?: return@forEach
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        pos.x - textLayout.size.width / 2f,
                        pos.y + nodeRadiusPx + 4f,
                    ),
                )
            }
        }
    }
}

/**
 * 圆形布局：节点均匀分布在圆周上。
 *
 * - 1 个节点：居中
 * - 2 个节点：水平对称
 * - 3+ 个节点：圆周等分
 */
private fun calculateCircularLayout(
    nodes: List<GraphNodeItem>,
    width: Float,
    height: Float,
): Map<String, Offset> {
    val centerX = width / 2f
    val centerY = height / 2f
    val radius = min(width, height) * 0.35f

    return when {
        nodes.size == 1 -> mapOf(nodes[0].id to Offset(centerX, centerY))
        nodes.size == 2 -> mapOf(
            nodes[0].id to Offset(centerX - radius, centerY),
            nodes[1].id to Offset(centerX + radius, centerY),
        )
        else -> {
            nodes.mapIndexed { index, node ->
                val angle = (2 * Math.PI * index / nodes.size).toFloat()
                node.id to Offset(
                    x = centerX + radius * cos(angle),
                    y = centerY + radius * sin(angle),
                )
            }.toMap()
        }
    }
}

/** R 值 → 颜色映射（基于主题角色色） */
private fun colorForRetrievability(
    r: Float,
    masteredColor: Color,
    consolidatingColor: Color,
    weakColor: Color,
    unlearnedColor: Color,
): Color = when {
    r >= 0.8f -> masteredColor
    r >= 0.5f -> consolidatingColor
    r > 0f -> weakColor
    else -> unlearnedColor
}
