package com.wenyan.app.feature.graph.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wenyan.app.feature.graph.GraphEdgeItem
import com.wenyan.app.feature.graph.GraphNodeItem
import com.wenyan.app.feature.graph.LayoutMode
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.PI
import kotlin.math.sin

/**
 * 知识图谱 Canvas 可视化组件（v0.8.0 重构：三模式布局 + 学习导向视觉编码）。
 *
 * ## v0.8.0 设计原则
 *
 * 基于认知负荷理论与概念图研究（Sweller, Novak, Nesbit & Adesope）：
 *
 * 1. **三模式布局**（[layoutMode]）：
 *    - TIMELINE：文学史时间轴泳道布局，横轴年份 + 纵轴 5 体裁泳道
 *    - NEIGHBORHOOD：邻域力导向布局，Obsidian Local Graph 范式
 *    - RADIAL：径向科目概览，按 subjectId 分扇区
 *
 * 2. **简化视觉编码**（3 层，正交无冲突）：
 *    - 颜色 = 掌握度（R 值：灰=未学/红=薄弱/橙=巩固/绿=已掌握）—— 主视觉
 *    - 尺寸 = 重要性（sourceKpIds.size，4 档）—— 固定不变
 *    - 描边 = 类型色（scale >= 0.7 时显示）—— 次要编码
 *
 * 3. **边语义化**（v0.8.0 强化）：
 *    - 边类型映射到中文标签："受影响"、"同时期"、"对比"等
 *    - 边类型映射到线型：实线/虚线/加粗/箭头（[EDGE_TYPE_LINE_STYLES]）
 *    - 研究表明：标注边的语义比单纯连线学习价值高 3-5 倍
 *
 * 4. **3 档 LOD**（Level of Detail）：
 *    - scale < 0.35：仅圆点（全局概览）
 *    - 0.35 ≤ scale < 0.7：圆点 + 边
 *    - 0.7 ≤ scale < 1.0：+ 类型描边
 *    - scale ≥ 1.0：+ 节点标签 + 边标签（v0.8.0 修复：边标签从 1.8 降到 1.0）
 *
 * 5. **交互态用透明度**（替代光环，减少视觉噪音）：
 *    - 搜索时未匹配节点 alpha 0.25
 *    - 聚焦节点尺寸 ×1.4（[GraphConstants.FOCUS_SCALE]）
 *
 * 6. **性能**：批量绘制（同色边合并为单一 Path）、视口剔除、LOD 跳过
 */
@Composable
fun GraphCanvas(
    nodes: List<GraphNodeItem>,
    edges: List<GraphEdgeItem>,
    layoutMode: LayoutMode,
    highlightedNodeIds: Set<String> = emptySet(),
    focusedNodeId: String? = null,
    onNodeClick: (String) -> Unit,
    resetTrigger: Int = 0,
    modifier: Modifier = Modifier,
) {
    if (nodes.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val colorScheme = MaterialTheme.colorScheme
    val masteredColor = colorScheme.primary
    val consolidatingColor = colorScheme.tertiary
    val weakColor = colorScheme.error
    val unlearnedColor = colorScheme.outline
    val labelColor = colorScheme.onSurface
    val edgeLabelColor = colorScheme.onSurfaceVariant
    val edgeColor = colorScheme.outlineVariant.copy(alpha = 0.5f)
    val weakEdgeColor = colorScheme.error.copy(alpha = 0.5f)
    val highlightEdgeColor = colorScheme.tertiary.copy(alpha = 0.7f)
    val rulerColor = colorScheme.outline.copy(alpha = 0.4f)

    // 缩放与平移状态
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 复位触发器
    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures(panZoomLock = true) { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(
                        GraphConstants.MIN_SCALE,
                        GraphConstants.MAX_SCALE,
                    )
                    val scaleFactor = newScale / scale
                    offset = (offset + centroid) * scaleFactor - centroid + pan
                    scale = newScale
                }
            }
            .semantics {
                contentDescription = "知识图谱，${nodes.size} 个节点，${layoutMode.displayName}布局。" +
                    "双指缩放，单指拖动，点击节点查看详情"
            },
    ) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        if (canvasWidth <= 0f || canvasHeight <= 0f) return@BoxWithConstraints

        val nodeRadiusPx = with(density) { GraphConstants.NODE_RADIUS_DP.dp.toPx() }
        val touchRadiusPx = with(density) { GraphConstants.NODE_TOUCH_RADIUS_DP.dp.toPx() }

        // 调用统一布局入口（v0.8.0：三模式可切换）
        val layoutResult = remember(nodes, edges, canvasWidth, canvasHeight, layoutMode, focusedNodeId) {
            GraphLayout.calculate(
                mode = layoutMode,
                nodes = nodes,
                edges = edges,
                width = canvasWidth,
                height = canvasHeight,
                focusedNodeId = focusedNodeId,
            )
        }

        // LOD 计算（v0.8.0：边标签阈值从 1.8 降到 1.0）
        val showEdges = scale >= GraphConstants.LOD_MINIMAL
        val showTypeStroke = scale >= GraphConstants.LOD_SPARSE
        val showLabels = scale >= GraphConstants.LOD_LABEL
        val showEdgeLabels = scale >= GraphConstants.LOD_LABEL
        val showTimelineRuler = layoutMode == LayoutMode.TIMELINE &&
            scale >= GraphConstants.LOD_MINIMAL
        val showSubjectSectors = layoutMode == LayoutMode.RADIAL &&
            scale >= GraphConstants.LOD_MINIMAL

        // 预测量节点标签
        val textLayouts = remember(nodes, labelColor, showLabels) {
            if (showLabels) {
                nodes.associate { node ->
                    node.id to textMeasurer.measure(
                        AnnotatedString(node.label),
                        TextStyle(fontSize = 11.sp, color = labelColor),
                    )
                }
            } else {
                emptyMap()
            }
        }

        // 预测量边标签（v0.8.0 修复 O(n²) bug：直接以 edge 为 key 缓存，而非按 label 文本查找）
        val edgeLabelLayouts = remember(edges, edgeLabelColor, showEdgeLabels) {
            if (showEdgeLabels) {
                edges.associateWith { edge ->
                    val label = EDGE_TYPE_LABELS[edge.relation] ?: ""
                    textMeasurer.measure(
                        AnnotatedString(label),
                        TextStyle(fontSize = 9.sp, color = edgeLabelColor),
                    )
                }
            } else {
                emptyMap()
            }
        }

        // v0.8.3 修复：科目标签预缓存，避免在 Canvas draw 循环内每帧调用 textMeasurer.measure
        // 原实现在 drawPath 后即时 measure 科目名称，每帧 × 扇区数 次测量，大量 GC 压力
        val subjectLabelLayouts = remember(layoutResult.subjectSectors, showSubjectSectors, scale) {
            if (showSubjectSectors && scale >= GraphConstants.LOD_SPARSE) {
                layoutResult.subjectSectors.mapNotNull { sector ->
                    val sectorColor = GRAPH_SUBJECT_COLORS[sector.subjectId] ?: return@mapNotNull null
                    val label = GRAPH_SUBJECT_DISPLAY_NAME[sector.subjectId] ?: ""
                    if (label.isEmpty()) return@mapNotNull null
                    sector to textMeasurer.measure(
                        AnnotatedString(label),
                        TextStyle(fontSize = 14.sp, color = Color(sectorColor).copy(alpha = 0.5f)),
                    )
                }
            } else {
                emptyList()
            }
        }

        // 弱节点 ID 集合
        val weakNodeIds = remember(nodes) {
            nodes.filter { it.retrievability > 0f && it.retrievability < GraphConstants.WEAK_THRESHOLD }
                .map { it.id }
                .toSet()
        }

        // 视口边界（世界坐标系，用于剔除）
        val viewportLeft = -offset.x / scale
        val viewportTop = -offset.y / scale
        val viewportRight = (canvasWidth - offset.x) / scale
        val viewportBottom = (canvasHeight - offset.y) / scale
        val cullMargin = nodeRadiusPx * GraphConstants.CULL_MARGIN_RATIO

        // Canvas 渲染层（统一 graphicsLayer 变换）
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offset.x
                    translationY = offset.y
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(nodes, layoutResult) {
                    // v0.8.3 修复：原 key 含 (nodes, scale, offset)，每次缩放/平移都重启
                    // detectTapGestures 协程，导致快速操作时手势检测被中断（漏点击）。
                    // 改为 (nodes, layoutResult) 稳定 key，scale/offset 在 lambda 内通过
                    // 状态委托读取最新值，手势检测器生命周期与节点数据一致。
                    detectTapGestures { tapOffset ->
                        // 读取当前 scale/offset（通过 MutableState 委托，获取最新值）
                        val currentScale = scale
                        val currentOffset = offset

                        // 屏幕坐标 → 世界坐标反向变换
                        val worldX = (tapOffset.x - currentOffset.x) / currentScale
                        val worldY = (tapOffset.y - currentOffset.y) / currentScale
                        val tapWorld = Offset(worldX, worldY)

                        // 视口边界（从当前 scale/offset 计算，用于剔除远端节点）
                        val vLeft = -currentOffset.x / currentScale
                        val vTop = -currentOffset.y / currentScale
                        val vRight = (canvasWidth - currentOffset.x) / currentScale
                        val vBottom = (canvasHeight - currentOffset.y) / currentScale

                        var nearestId: String? = null
                        var nearestDist = Float.MAX_VALUE
                        val maxDist = touchRadiusPx / currentScale
                        for (node in nodes) {
                            val pos = layoutResult.positions[node.id] ?: continue
                            if (pos.x < vLeft - cullMargin || pos.x > vRight + cullMargin) continue
                            if (pos.y < vTop - cullMargin || pos.y > vBottom + cullMargin) continue
                            val d = hypot(pos.x - tapWorld.x, pos.y - tapWorld.y)
                            if (d < nearestDist && d < maxDist) {
                                nearestDist = d
                                nearestId = node.id
                            }
                        }
                        nearestId?.let { onNodeClick(it) }
                    }
                },
        ) {
            // ── 1. 时间轴刻度线 + 泳道标签（仅 TIMELINE 模式）──
            if (showTimelineRuler) {
                // 时间刻度竖线
                for (tick in layoutResult.timelineTicks) {
                    drawLine(
                        color = rulerColor,
                        start = Offset(tick.x, 0f),
                        end = Offset(tick.x, size.height),
                        strokeWidth = 0.8f,
                    )
                }
                // 泳道横线 + 标签
                for (lane in layoutResult.timelineLanes) {
                    drawLine(
                        color = rulerColor,
                        start = Offset(0f, lane.y),
                        end = Offset(size.width, lane.y),
                        strokeWidth = 0.5f,
                    )
                }
            }

            // ── 2. 科目扇区背景（仅 RADIAL 模式）──
            if (showSubjectSectors && layoutResult.subjectSectors.isNotEmpty()) {
                for (sector in layoutResult.subjectSectors) {
                    val sectorColor = GRAPH_SUBJECT_COLORS[sector.subjectId] ?: continue
                    val path = Path().apply {
                        val steps = 24
                        for (i in 0..steps) {
                            val angle = sector.startAngle + (sector.endAngle - sector.startAngle) * i / steps
                            val x = (layoutResult.centerX + sector.outerRadius * cos(angle)).toFloat()
                            val y = (layoutResult.centerY + sector.outerRadius * sin(angle)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        for (i in steps downTo 0) {
                            val angle = sector.startAngle + (sector.endAngle - sector.startAngle) * i / steps
                            val x = (layoutResult.centerX + sector.innerRadius * cos(angle)).toFloat()
                            val y = (layoutResult.centerY + sector.innerRadius * sin(angle)).toFloat()
                            lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color(sectorColor).copy(alpha = 0.06f),
                    )
                }

                // v0.8.3 优化：科目标签批量绘制，使用预缓存 layout，避免每帧 measure
                if (scale >= GraphConstants.LOD_SPARSE) {
                    for ((sector, textLayout) in subjectLabelLayouts) {
                        val midAngle = (sector.startAngle + sector.endAngle) / 2
                        val labelRadius = (sector.innerRadius + sector.outerRadius) / 2
                        val labelX = (layoutResult.centerX + labelRadius * cos(midAngle)).toFloat()
                        val labelY = (layoutResult.centerY + labelRadius * sin(midAngle)).toFloat()
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                labelX - textLayout.size.width / 2f,
                                labelY - textLayout.size.height / 2f,
                            ),
                        )
                    }
                }
            }

            // ── 3. 边（LOD 控制 + 视口剔除 + 按线型分组批量绘制 + 边标签）──
            if (showEdges) {
                // 按线型 + 状态分组（6 组：solid/weak/highlight × normal/dashed/thick）
                // 简化：按 lineStyle 分 3 组，状态用颜色覆盖
                val solidPath = Path()
                val dashedPath = Path()
                val thickPath = Path()
                val weakSolidPath = Path()
                val weakDashedPath = Path()
                val highlightSolidPath = Path()
                val highlightDashedPath = Path()

                // 边标签位置收集
                data class EdgeLabelDraw(
                    val pos: Offset,
                    val edge: GraphEdgeItem,
                )
                val labeledEdges = mutableListOf<EdgeLabelDraw>()

                for (edge in edges) {
                    val from = layoutResult.positions[edge.fromId] ?: continue
                    val to = layoutResult.positions[edge.toId] ?: continue
                    // 视口剔除
                    if (from.x < viewportLeft - cullMargin && to.x < viewportLeft - cullMargin) continue
                    if (from.x > viewportRight + cullMargin && to.x > viewportRight + cullMargin) continue
                    if (from.y < viewportTop - cullMargin && to.y < viewportTop - cullMargin) continue
                    if (from.y > viewportBottom + cullMargin && to.y > viewportBottom + cullMargin) continue

                    val isWeak = edge.fromId in weakNodeIds || edge.toId in weakNodeIds
                    val isHighlighted = highlightedNodeIds.isNotEmpty() &&
                        (edge.fromId in highlightedNodeIds || edge.toId in highlightedNodeIds)
                    val lineStyle = EDGE_TYPE_LINE_STYLES[edge.relation] ?: EdgeLineStyle.SOLID
                    val isDashed = lineStyle == EdgeLineStyle.DASHED || lineStyle == EdgeLineStyle.DASHED_ARROW
                    val isThick = lineStyle == EdgeLineStyle.THICK

                    // 选择目标 Path
                    val targetPath = when {
                        isHighlighted && isDashed -> highlightDashedPath
                        isHighlighted -> highlightSolidPath
                        isWeak && isDashed -> weakDashedPath
                        isWeak -> weakSolidPath
                        isThick -> thickPath
                        isDashed -> dashedPath
                        else -> solidPath
                    }

                    val fromSubject = layoutResult.nodeSubject[edge.fromId]
                    val toSubject = layoutResult.nodeSubject[edge.toId]
                    val isCrossSubject = fromSubject != null && toSubject != null && fromSubject != toSubject

                    if (isCrossSubject) {
                        // 跨科目边用贝塞尔曲线（控制点向圆心偏移）
                        val midX = (from.x + to.x) / 2f
                        val midY = (from.y + to.y) / 2f
                        val ctrlX = midX + (layoutResult.centerX - midX) * 0.25f
                        val ctrlY = midY + (layoutResult.centerY - midY) * 0.25f
                        targetPath.moveTo(from.x, from.y)
                        targetPath.quadraticTo(ctrlX, ctrlY, to.x, to.y)

                        // 边标签位置：二次贝塞尔曲线中点
                        if (showEdgeLabels && EDGE_TYPE_LABELS.containsKey(edge.relation)) {
                            val labelX = 0.25f * from.x + 0.5f * ctrlX + 0.25f * to.x
                            val labelY = 0.25f * from.y + 0.5f * ctrlY + 0.25f * to.y
                            labeledEdges.add(EdgeLabelDraw(Offset(labelX, labelY), edge))
                        }
                    } else {
                        targetPath.moveTo(from.x, from.y)
                        targetPath.lineTo(to.x, to.y)

                        if (showEdgeLabels && EDGE_TYPE_LABELS.containsKey(edge.relation)) {
                            val midX = (from.x + to.x) / 2f
                            val midY = (from.y + to.y) / 2f
                            labeledEdges.add(EdgeLabelDraw(Offset(midX, midY), edge))
                        }
                    }
                }

                // 批量绘制各组（v0.8.0：线型由 pathEffect 控制，颜色由状态决定）
                val normalStroke = Stroke(
                    width = GraphConstants.EDGE_WIDTH_NORMAL,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
                val dashedStroke = Stroke(
                    width = GraphConstants.EDGE_WIDTH_NORMAL,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = DASHED_PATH_EFFECT,
                )
                val thickStroke = Stroke(
                    width = GraphConstants.EDGE_WIDTH_HIGHLIGHT,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
                val weakStroke = Stroke(
                    width = GraphConstants.EDGE_WIDTH_WEAK,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
                val weakDashedStroke = Stroke(
                    width = GraphConstants.EDGE_WIDTH_WEAK,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = DASHED_PATH_EFFECT,
                )
                val highlightStroke = Stroke(
                    width = GraphConstants.EDGE_WIDTH_HIGHLIGHT,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )

                if (!solidPath.isEmpty) drawPath(solidPath, edgeColor, style = normalStroke)
                if (!dashedPath.isEmpty) drawPath(dashedPath, edgeColor, style = dashedStroke)
                if (!thickPath.isEmpty) drawPath(thickPath, edgeColor, style = thickStroke)
                if (!weakSolidPath.isEmpty) drawPath(weakSolidPath, weakEdgeColor, style = weakStroke)
                if (!weakDashedPath.isEmpty) drawPath(weakDashedPath, weakEdgeColor, style = weakDashedStroke)
                if (!highlightSolidPath.isEmpty) drawPath(highlightSolidPath, highlightEdgeColor, style = highlightStroke)
                if (!highlightDashedPath.isEmpty) {
                    drawPath(
                        highlightDashedPath,
                        highlightEdgeColor,
                        style = Stroke(
                            width = GraphConstants.EDGE_WIDTH_HIGHLIGHT,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = DASHED_PATH_EFFECT,
                        ),
                    )
                }

                // 绘制边标签（v0.8.0 修复：用预缓存 layout，O(n) 而非 O(n²)）
                if (showEdgeLabels) {
                    for (draw in labeledEdges) {
                        val layout = edgeLabelLayouts[draw.edge] ?: continue
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                draw.pos.x - layout.size.width / 2f,
                                draw.pos.y - layout.size.height / 2f,
                            ),
                        )
                    }
                }
            }

            // ── 4. 节点（v0.8.1 重构视觉编码：形状替代描边色）──
            // 视觉编码（3 层，正交无冲突）：
            //   - 颜色 = 掌握度（R 值：灰=未学/红=薄弱/橙=巩固/绿=已掌握）
            //   - 尺寸 = 重要性（sourceKpIds.size，4 档）
            //   - 形状 = 节点类型（圆=作家/方=作品/菱=概念/三角=流派/星=知识点）
            //     v0.8.1：替代类型描边色，避免与掌握度填充色冲突
            for (node in nodes) {
                val pos = layoutResult.positions[node.id] ?: continue
                if (pos.x < viewportLeft - cullMargin) continue
                if (pos.x > viewportRight + cullMargin) continue
                if (pos.y < viewportTop - cullMargin) continue
                if (pos.y > viewportBottom + cullMargin) continue

                val isHighlighted = node.id in highlightedNodeIds
                val isFocused = node.id == focusedNodeId
                val isSearching = highlightedNodeIds.isNotEmpty() && !isHighlighted

                // 节点尺寸：基于关联知识点数（重要性），4 档
                val sizeMultiplier = nodeSizeMultiplier(node.sourceKpIds.size)
                var nodeRadius = nodeRadiusPx * sizeMultiplier
                // 聚焦节点放大（替代光环）
                if (isFocused) nodeRadius *= GraphConstants.FOCUS_SCALE
                else if (isHighlighted) nodeRadius *= GraphConstants.HIGHLIGHT_SCALE

                // 节点颜色：掌握度（R 值映射）
                val baseColor = masteryColor(
                    retrievability = node.retrievability,
                    masteredColor = masteredColor,
                    consolidatingColor = consolidatingColor,
                    weakColor = weakColor,
                    unlearnedColor = unlearnedColor,
                )
                val finalColor = if (isSearching) baseColor.copy(alpha = 0.25f) else baseColor

                // 节点形状：按类型映射（v0.8.1 新增）
                // scale >= 0.7 时显示真实形状；scale < 0.7 时统一用圆点（远观降低视觉噪音）
                val shape = if (showTypeStroke) {
                    GRAPH_TYPE_SHAPES[node.type] ?: NodeShape.CIRCLE
                } else {
                    NodeShape.CIRCLE
                }
                drawNodeShape(shape, pos, nodeRadius, finalColor)

                // 标签
                if (showLabels) {
                    val textLayout = textLayouts[node.id] ?: continue
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(
                            pos.x - textLayout.size.width / 2f,
                            pos.y + nodeRadius + GraphConstants.LABEL_OFFSET,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * 绘制节点形状（v0.8.1 新增：替代 drawCircle，支持 5 种形状编码类型）。
 *
 * 作为 [DrawScope] 扩展函数，在 Canvas 渲染块内通过 `this: DrawScope` 隐式接收。
 * 同时供图例 [com.wenyan.app.feature.graph.GraphScreen.ShapeLegendItem] 复用，
 * 确保图例形状与 Canvas 节点形状视觉一致。
 *
 * @param shape  节点形状
 * @param center 中心坐标
 * @param radius 半径（外接圆半径）
 * @param color  填充色（掌握度色）
 */
internal fun DrawScope.drawNodeShape(
    shape: NodeShape,
    center: Offset,
    radius: Float,
    color: Color,
) {
    when (shape) {
        NodeShape.CIRCLE -> drawCircle(color = color, radius = radius, center = center)
        NodeShape.SQUARE -> {
            val half = radius * 0.9f
            drawRect(
                color = color,
                topLeft = Offset(center.x - half, center.y - half),
                size = androidx.compose.ui.geometry.Size(half * 2, half * 2),
            )
        }
        NodeShape.DIAMOND -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius, center.y)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius, center.y)
                close()
            }
            drawPath(path = path, color = color)
        }
        NodeShape.TRIANGLE -> {
            // 等边三角形，中心对齐
            val r = radius * 1.1f
            val path = Path().apply {
                moveTo(center.x, center.y - r)
                lineTo(center.x + r * 0.866f, center.y + r * 0.5f)
                lineTo(center.x - r * 0.866f, center.y + r * 0.5f)
                close()
            }
            drawPath(path = path, color = color)
        }
        NodeShape.STAR -> {
            // 五角星：外半径 radius，内半径 radius*0.4
            val path = Path().apply {
                val outerR = radius * 1.1f
                val innerR = outerR * 0.4f
                for (i in 0 until 10) {
                    val angle = -PI / 2 + i * PI / 5
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = (center.x + r * cos(angle)).toFloat()
                    val y = (center.y + r * sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path = path, color = color)
        }
    }
}

/**
 * 按掌握度（R 值）映射节点填充色。
 *
 * 映射：
 * - R ≥ 0.8：已掌握（masteredColor，主色/绿）
 * - 0.5 ≤ R < 0.8：巩固中（consolidatingColor，三色/橙）
 * - 0 < R < 0.5：薄弱（weakColor，错误色/红）
 * - R = 0：未学习（unlearnedColor，轮廓色/灰）
 */
private fun masteryColor(
    retrievability: Float,
    masteredColor: Color,
    consolidatingColor: Color,
    weakColor: Color,
    unlearnedColor: Color,
): Color = when {
    retrievability >= GraphConstants.MASTERY_THRESHOLD -> masteredColor
    retrievability >= GraphConstants.WEAK_THRESHOLD -> consolidatingColor
    retrievability > 0f -> weakColor
    else -> unlearnedColor
}

/**
 * 按关联知识点数计算节点尺寸倍率。
 *
 * 节点尺寸体现"重要性"：关联知识点越多，该实体在考研知识体系中越核心。
 *
 * 4 档（v0.8.0 抽到 [GraphConstants]）：
 * - sourceKpCount >= 7：1.6x（核心实体，如鲁迅/诗经等横跨多个知识点）
 * - sourceKpCount 4-6：1.3x（重要实体）
 * - sourceKpCount 2-3：1.0x（常规实体）
 * - sourceKpCount <= 1：0.7x（边缘实体，只出现在 1 个知识点）
 */
private fun nodeSizeMultiplier(sourceKpCount: Int): Float = when {
    sourceKpCount >= 7 -> GraphConstants.SIZE_CORE
    sourceKpCount >= 4 -> GraphConstants.SIZE_IMPORTANT
    sourceKpCount >= 2 -> GraphConstants.SIZE_NORMAL
    else -> GraphConstants.SIZE_MINOR
}
