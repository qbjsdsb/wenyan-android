package com.wenyan.app.feature.graph

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.SmartToy
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.feature.graph.ui.GraphCanvas
import kotlinx.coroutines.launch

// 图例颜色（从主题获取，与 GraphCanvas 一致）

/**
 * 知识图谱界面（v0.7.6 时间轴布局）。
 *
 * 功能：
 * - Canvas 可视化节点 + 边（v0.7.6 文学史时间轴泳道布局）
 * - 横轴 = 时间（1915-2030，覆盖现当代文学全周期）
 * - 纵轴 = 4 条泳道（时段/流派/作家/体裁）
 * - 跨类边纵向连接泳道，形成"作家↔流派↔体裁↔时段"知识链路
 * - 顶部时间刻度线（8 个关键年份）+ 泳道分割线
 * - 分类色映射（作家粉/体裁蓝/时段绿/流派紫/作品橙）
 * - 薄弱子图高亮（红色光晕 + 红色边）
 * - 图例说明（v0.7.6 新增布局提示）
 * - 底部统计栏（总节点 / 薄弱节点 / 平均 R）
 * - 双指缩放 + 单指平移（v0.7.4 保留）
 * - 节点点击 → 有关联知识点则跳转详情,否则 Snackbar 提示(P0 v0.7.2 修复)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: GraphViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ExpressiveScaffold(
        topBar = {
            // 图谱 Canvas 固定布局不滚动，仅享受 Large 标题样式
            WenyanLargeTopAppBar(
                title = "知识图谱",
                // v0.7.6 新增：副标题提示时间轴范围
                subtitle = "文学史时间轴 · 1915-2030",
                actions = {
                    IconButton(onClick = onNavigateToAiAssistant) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI助手",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 图例（v0.7.6 紧凑化：单行分类色 + 泳道说明）
            LegendBar()

            // 主内容区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Crossfade(
                    targetState = Triple(uiState.isLoading, uiState.error, uiState.nodes.isEmpty()),
                    animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                    label = "graph_state",
                    modifier = Modifier.fillMaxSize(),
                ) { (isLoading, error, isEmpty) ->
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                WenyanLoadingIndicator()
                            }
                        }
                        // P0-6 修复：加 error 分支，数据加载失败时展示错误信息 + 重试按钮
                        error != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                ErrorState(
                                    icon = Icons.Default.CloudOff,
                                    title = "加载失败",
                                    message = error,
                                    onRetry = viewModel::retry,
                                )
                            }
                        }
                        isEmpty -> {
                            EmptyState(
                                icon = Icons.Filled.Inbox,
                                title = "暂无图谱数据，请先导入知识点",
                            )
                        }
                        else -> {
                            GraphCanvas(
                                nodes = uiState.nodes,
                                edges = uiState.edges,
                                onNodeClick = { nodeId ->
                                    // P0 修复(v0.7.2):有关联知识点 → 跳转详情;无关联 → Snackbar 提示
                                    val node = uiState.nodes.find { it.id == nodeId }
                                    if (node != null && node.relatedPointId != null) {
                                        onNavigateToDetail(node.relatedPointId)
                                    } else {
                                        val message = if (node != null) {
                                            "${node.label}（R=%.2f，导航性节点）".format(node.retrievability)
                                        } else {
                                            "未知节点"
                                        }
                                        scope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // 底部统计栏
            // P2 性能修复：统计计算包裹 remember(uiState.nodes)，避免每次重组重复 O(n) 遍历 + 堆分配。
            // 原 map{}.average() 每次重组分配新 List<Float> + 两次 O(n) 遍历，
            // 节点列表未变时完全无需重算。
            val stats = remember(uiState.nodes) {
                val nodes = uiState.nodes
                if (nodes.isEmpty()) null
                else Triple(
                    nodes.size,
                    nodes.count { it.retrievability > 0f && it.retrievability < 0.5f },
                    nodes.map { it.retrievability }.average().toFloat(),
                )
            }
            if (!uiState.isLoading && stats != null) {
                StatsBar(
                    totalNodes = stats.first,
                    weakNodes = stats.second,
                    avgRetrievability = stats.third,
                )
            }
        }
    }
}

// ── 图例 ──────────────────────────────────────────────────────

/**
 * 图例（v0.7.6 重构：时间轴泳道布局说明）。
 *
 * 原图例仅显示 5 类分类色（v0.7.4 实现），未说明布局含义。
 * v0.7.6 改为两层信息：
 * - 上层：横向时间轴 / 纵向泳道 提示
 * - 下层：5 类分类色 + 薄弱光晕
 *
 * 配合 [GraphCanvas] 的文学史时间轴布局，让用户立刻理解图谱组织方式：
 * - 横轴 = 时间（1915-2030，覆盖现当代文学全周期）
 * - 纵轴 = 4 条泳道（时段/流派/作家/体裁）
 * - 跨类边纵向连接泳道，形成"作家↔流派↔体裁↔时段"知识链路
 */
@Composable
private fun LegendBar() {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            // 上层：布局说明
            Text(
                text = "横轴：时间  ·  纵轴：泳道（时段 / 流派 / 作家 / 体裁）",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
            // 下层：分类色图例
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendItem(Color(0xFFE91E63), "作者")
                LegendItem(Color(0xFF2196F3), "体裁")
                LegendItem(Color(0xFF4CAF50), "时段")
                LegendItem(Color(0xFF9C27B0), "流派")
                LegendItem(Color(0xFFFF9800), "作品")
                LegendItem(colorScheme.error.copy(alpha = 0.4f), "薄弱光晕")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs),
        )
    }
}

// ── 底部统计栏 ────────────────────────────────────────────────

@Composable
private fun StatsBar(
    totalNodes: Int,
    weakNodes: Int,
    avgRetrievability: Float,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem("节点", "$totalNodes")
            StatItem("薄弱", "$weakNodes")
            StatItem("平均R", "%.2f".format(avgRetrievability))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            // P2-2 修复：Bold(700) 过重，M3 Expressive 推荐 SemiBold(600)
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── 空状态 ────────────────────────────────────────────────────

// EmptyState 已迁移至共享 EmptyState 组件
