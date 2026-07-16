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
 * 知识图谱界面（阶段5增强）。
 *
 * 功能：
 * - Canvas 可视化节点 + 边
 * - R 值颜色映射（绿/黄/红/灰）
 * - 薄弱子图高亮（红色光晕 + 红色边）
 * - 图例说明
 * - 底部统计栏（总节点 / 薄弱节点 / 平均 R）
 * - 节点点击 → Snackbar 提示（后续接通知识点详情页跳转）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    onNavigateToAiAssistant: () -> Unit = {},
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
            // 图例
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
                                    // 阶段5：Snackbar 提示；后续 5.3 接通详情页跳转
                                    val node = uiState.nodes.find { it.id == nodeId }
                                    val message = if (node != null) {
                                        "${node.label}（R=%.2f）".format(node.retrievability)
                                    } else {
                                        "未知节点"
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(message)
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

@Composable
private fun LegendBar() {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(colorScheme.primary, "已掌握")
            LegendItem(colorScheme.tertiary, "需巩固")
            LegendItem(colorScheme.error, "薄弱")
            LegendItem(colorScheme.outline, "未学习")
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
