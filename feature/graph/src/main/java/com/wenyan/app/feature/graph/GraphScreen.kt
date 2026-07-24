package com.wenyan.app.feature.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.feature.graph.ui.EDGE_TYPE_LABELS
import com.wenyan.app.feature.graph.ui.GRAPH_SUBJECT_COLORS
import com.wenyan.app.feature.graph.ui.GRAPH_SUBJECT_DISPLAY_NAME
import com.wenyan.app.feature.graph.ui.GRAPH_TYPE_COLORS
import com.wenyan.app.feature.graph.ui.GRAPH_TYPE_DISPLAY_NAME
import com.wenyan.app.feature.graph.ui.GRAPH_TYPE_SHAPES
import com.wenyan.app.feature.graph.ui.GraphCanvas
import com.wenyan.app.feature.graph.ui.NodeShape
import com.wenyan.app.feature.graph.ui.drawNodeShape
import kotlinx.coroutines.launch

/**
 * 知识图谱界面（v0.8.0 重构：学习导向 + 3 档显示范围 + 边标签图例）。
 *
 * ## v0.8.0 核心改进
 *
 * - **3 档显示范围 Tab**：核心 / 重要 / 全部（替代二元开关）
 * - **简化图例**：移除薄弱光晕等冗余图例项
 * - **学习导向 BottomSheet**：增加"开始复习"和"查看真题"入口
 * - **掌握度统计**：底部显示已掌握/巩固/薄弱/未学分布
 * - **边标签图例**：显示常见关系类型
 *
 * 布局：
 * - TopAppBar：标题 + 搜索 + 筛选 + 重置 + AI 助手
 * - 显示范围 Tab：核心 / 重要 / 全部
 * - 搜索栏（可展开）
 * - 筛选面板（可展开）：科目 + 类型 + 薄弱
 * - 图例栏
 * - GraphCanvas（主内容区）
 * - 底部掌握度统计栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToReview: (String) -> Unit = {},
    viewModel: GraphViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val selectedTypes by viewModel.selectedTypes.collectAsStateWithLifecycle()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsStateWithLifecycle()
    val showWeakOnly by viewModel.showWeakOnly.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val displayScope by viewModel.displayScope.collectAsStateWithLifecycle()
    val layoutMode by viewModel.layoutMode.collectAsStateWithLifecycle()
    val knowledgePointTitles by viewModel.knowledgePointTitles.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showFilterPanel by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var resetTrigger by remember { mutableStateOf(0) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    // v0.8.1：图例默认收起，释放主内容区垂直空间
    var legendCollapsed by remember { mutableStateOf(true) }

    val selectedNode = remember(uiState.nodes, selectedNodeId) {
        if (selectedNodeId == null) null
        else uiState.nodes.find { it.id == selectedNodeId }
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "知识图谱",
                subtitle = "${uiState.nodes.size}/${uiState.totalCount} 节点 · " +
                    "${layoutMode.displayName} · ${displayScope.displayName}",
                actions = {
                    IconButton(onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = if (showSearchBar) "关闭搜索" else "搜索节点",
                            tint = if (showSearchBar) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = if (showFilterPanel) "收起筛选" else "展开筛选",
                            tint = if (showFilterPanel || selectedTypes.isNotEmpty() ||
                                selectedSubjectId != null || showWeakOnly)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { resetTrigger++ }) {
                        Icon(
                            imageVector = Icons.Default.LocationSearching,
                            contentDescription = "重置视图",
                        )
                    }
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
            // 布局模式选择器（v0.8.0 新增：三模式可切换）
            LayoutModeSelector(
                selectedMode = layoutMode,
                onModeSelected = viewModel::setLayoutMode,
            )

            // 显示范围 Tab（v0.8.0 新增：3 档）
            DisplayScopeTabRow(
                selectedScope = displayScope,
                onScopeSelected = viewModel::setDisplayScope,
            )

            // 搜索栏（可展开收起）
            AnimatedVisibility(
                visible = showSearchBar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onClear = {
                        viewModel.setSearchQuery("")
                        showSearchBar = false
                    },
                )
            }

            // 筛选面板
            AnimatedVisibility(
                visible = showFilterPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                FilterPanel(
                    subjects = subjects,
                    selectedSubjectId = selectedSubjectId,
                    onSubjectSelected = viewModel::setSubjectFilter,
                    selectedTypes = selectedTypes,
                    onTypeToggled = viewModel::toggleTypeFilter,
                    showWeakOnly = showWeakOnly,
                    onWeakToggled = viewModel::toggleWeakOnly,
                    onClearAll = viewModel::clearAllFilters,
                )
            }

            // 图例（v0.8.1：可折叠，默认收起释放主内容区）
            LegendBar(
                layoutMode = layoutMode,
                collapsed = legendCollapsed,
                onToggle = { legendCollapsed = !legendCollapsed },
            )

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
                                title = if (searchQuery.isNotBlank()) "未找到匹配的节点"
                                        else "当前范围无节点，尝试切换显示范围或调整筛选",
                            )
                        }
                        else -> {
                            GraphCanvas(
                                nodes = uiState.nodes,
                                edges = uiState.edges,
                                layoutMode = layoutMode,
                                highlightedNodeIds = uiState.highlightedNodeIds,
                                focusedNodeId = uiState.focusedNodeId,
                                resetTrigger = resetTrigger,
                                onNodeClick = { nodeId ->
                                    val node = uiState.nodes.find { it.id == nodeId }
                                    if (node != null) {
                                        selectedNodeId = nodeId
                                        // NEIGHBORHOOD 模式下点击节点即设为聚焦点
                                        if (layoutMode == LayoutMode.NEIGHBORHOOD) {
                                            viewModel.setFocusedNode(nodeId)
                                        }
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("未知节点")
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // 底部掌握度统计栏（v0.8.0 增强）
            if (!uiState.isLoading && uiState.totalCount > 0) {
                MasteryStatsBar(
                    displayedNodes = uiState.nodes.size,
                    totalNodes = uiState.totalCount,
                    masteredCount = uiState.masteredCount,
                    consolidatingCount = uiState.nodes.size - uiState.masteredCount -
                        uiState.weakCount - uiState.unlearnedCount,
                    weakCount = uiState.weakCount,
                    unlearnedCount = uiState.unlearnedCount,
                )
            }
        }
    }

    // 节点详情 BottomSheet（v0.8.0 学习导向重构）
    selectedNode?.let { node ->
        NodeDetailSheet(
            node = node,
            knowledgePointTitles = knowledgePointTitles,
            onDismiss = { selectedNodeId = null },
            onNavigateToDetail = { pointId ->
                selectedNodeId = null
                onNavigateToDetail(pointId)
            },
            onNavigateToReview = { pointId ->
                selectedNodeId = null
                onNavigateToReview(pointId)
            },
            onFocusNode = { nodeId ->
                viewModel.setFocusedNode(nodeId)
                selectedNodeId = null
                resetTrigger++
            },
        )
    }
}

// ── 布局模式选择器（v0.8.0 新增）──────────────────────────────

/**
 * 布局模式 SegmentedButton 选择器（v0.8.0 新增）。
 *
 * 三模式对应不同学习任务：
 * - TIMELINE：文学史脉络（默认）
 * - NEIGHBORHOOD：深挖某节点关系
 * - RADIAL：鸟瞰科目全局
 */
@Composable
private fun LayoutModeSelector(
    selectedMode: LayoutMode,
    onModeSelected: (LayoutMode) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            LayoutMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = LayoutMode.entries.size,
                    ),
                    label = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedMode == mode) FontWeight.SemiBold
                                    else FontWeight.Normal,
                            )
                        }
                    },
                )
            }
        }
    }
}

// ── 显示范围 Tab ──────────────────────────────────────────────

/**
 * 显示范围 Tab Row（v0.8.0 新增）。
 *
 * 3 档：核心 / 重要 / 全部
 * - 核心：40-60 节点，首次浏览与全局理解
 * - 重要：200-400 节点，科目内深入
 * - 全部：2000+ 节点，搜索特定实体
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayScopeTabRow(
    selectedScope: DisplayScope,
    onScopeSelected: (DisplayScope) -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedScope.ordinal,
    ) {
        DisplayScope.entries.forEach { scope ->
            Tab(
                selected = selectedScope == scope,
                onClick = { onScopeSelected(scope) },
                text = {
                    Text(
                        text = scope.displayName,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

// ── 搜索栏 ──────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            placeholder = { Text("搜索节点名称（作家/作品/概念）") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "清除")
                    }
                }
            },
            singleLine = true,
        )
    }
}

// ── 筛选面板 ──────────────────────────────────────────────

@Composable
private fun FilterPanel(
    subjects: List<com.wenyan.app.core.database.entity.SubjectEntity>,
    selectedSubjectId: String?,
    onSubjectSelected: (String?) -> Unit,
    selectedTypes: Set<String>,
    onTypeToggled: (String) -> Unit,
    showWeakOnly: Boolean,
    onWeakToggled: () -> Unit,
    onClearAll: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            // 科目筛选 + 清空按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "科目",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onClearAll) {
                    Text("清空筛选")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                FilterChip(
                    selected = selectedSubjectId == null,
                    onClick = { onSubjectSelected(null) },
                    label = { Text("全部") },
                )
                subjects.forEach { subject ->
                    FilterChip(
                        selected = selectedSubjectId == subject.id,
                        onClick = {
                            onSubjectSelected(
                                if (selectedSubjectId == subject.id) null else subject.id,
                            )
                        },
                        label = { Text(subject.name) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // 类型筛选
            Text(
                text = "节点类型",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                GRAPH_TYPE_DISPLAY_NAME.forEach { (type, name) ->
                    FilterChip(
                        selected = type in selectedTypes,
                        onClick = { onTypeToggled(type) },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GRAPH_TYPE_COLORS[type]
                                ?.let { Color(it).copy(alpha = 0.2f) }
                                ?: MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // 薄弱筛选
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = showWeakOnly,
                    onClick = onWeakToggled,
                    label = { Text("仅薄弱（R<0.5）") },
                    leadingIcon = if (showWeakOnly) {
                        { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                )
            }
        }
    }
}

// ── 图例（v0.8.1 可折叠 + 形状编码）──────────────────────────────

/**
 * 图例（v0.8.1 重构：可折叠 + 形状编码替代描边色）。
 *
 * v0.8.1 改进：
 * - **可折叠**：默认收起只显示一行摘要，点击展开完整图例。
 *   原实现固定 5 行占用 ~120dp 垂直空间，挤压 Canvas 主内容区。
 *   收起后仅占 ~32dp，Canvas 多出 ~88dp 可用高度。
 * - **形状图标**：类型图例用真实形状（圆/方/菱/三角/星）替代彩色圆点，
 *   与 Canvas 中的节点渲染保持一致（v0.8.1 形状编码）。
 *
 * 3 类信息（展开时）：
 * 1. 节点颜色 = 掌握度（灰/红/橙/绿）
 * 2. 节点形状 = 类型（圆/方/菱/三角/星）
 * 3. 边线型/标签 = 关系类型
 *
 * @param layoutMode 当前布局模式（影响顶部说明文案）
 * @param collapsed  是否收起（由调用方持有状态）
 * @param onToggle   收起/展开切换回调
 */
@Composable
private fun LegendBar(
    layoutMode: LayoutMode,
    collapsed: Boolean,
    onToggle: () -> Unit,
) {
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
            // 顶栏：布局说明 + 收起/展开按钮（始终可见）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = layoutMode.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = if (collapsed) Icons.Default.PlayArrow
                            else Icons.Default.Close,
                        contentDescription = if (collapsed) "展开图例" else "收起图例",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // 展开内容（v0.8.1：AnimatedVisibility 替代永远显示）
            AnimatedVisibility(
                visible = !collapsed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    // 掌握度色图例（主视觉编码）
                    Text(
                        text = "节点颜色 = 掌握度",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xs)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LegendItem(colorScheme.outline, "未学习")
                        LegendItem(colorScheme.error, "薄弱")
                        LegendItem(colorScheme.tertiary, "巩固中")
                        LegendItem(colorScheme.primary, "已掌握")
                    }

                    // 类型形状图例（v0.8.1：形状替代描边色）
                    Text(
                        text = "节点形状 = 类型（放大后可见）",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xs)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GRAPH_TYPE_DISPLAY_NAME.forEach { (type, name) ->
                            val shape = GRAPH_TYPE_SHAPES[type] ?: NodeShape.CIRCLE
                            ShapeLegendItem(shape, colorScheme.onSurfaceVariant, name)
                        }
                    }

                    // 边标签图例
                    Text(
                        text = "边线型/标签 = 关系类型（放大后可见）",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xs)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EDGE_TYPE_LABELS.values.take(6).forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = Spacing.xs, vertical = 2.dp),
                            )
                        }
                    }
                }
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

/**
 * 形状图例项（v0.8.1 新增：用 Canvas 绘制真实形状，与节点渲染一致）。
 */
@Composable
private fun ShapeLegendItem(shape: NodeShape, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(12.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension / 2f
            drawNodeShape(shape, Offset(cx, cy), radius, color)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs),
        )
    }
}

// ── 底部掌握度统计栏（v0.8.0 增强）──────────────────────────────

/**
 * 掌握度统计栏（v0.8.0 新增：4 档分布 + 进度条）。
 *
 * 显示：
 * - 显示节点数 / 总节点数
 * - 4 档掌握度分布：已掌握 / 巩固中 / 薄弱 / 未学习
 * - 掌握进度条（已掌握占比）
 */
@Composable
private fun MasteryStatsBar(
    displayedNodes: Int,
    totalNodes: Int,
    masteredCount: Int,
    consolidatingCount: Int,
    weakCount: Int,
    unlearnedCount: Int,
) {
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
            // 显示节点数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "显示 $displayedNodes / $totalNodes 节点",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                )
                val masteryRate = if (displayedNodes > 0) masteredCount.toFloat() / displayedNodes else 0f
                Text(
                    text = "掌握率 ${"%.0f%%".format(masteryRate * 100)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // 4 档掌握度分布进度条
            if (displayedNodes > 0) {
                LinearProgressIndicator(
                    progress = {
                        val mastered = masteredCount.toFloat() / displayedNodes
                        val consolidating = consolidatingCount.toFloat() / displayedNodes
                        val weak = weakCount.toFloat() / displayedNodes
                        mastered + consolidating + weak
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = colorScheme.primary,
                    trackColor = colorScheme.outline.copy(alpha = 0.2f),
                )
                // 4 档数字
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem("已掌握", "$masteredCount", colorScheme.primary)
                    StatItem("巩固中", "$consolidatingCount", colorScheme.tertiary)
                    StatItem("薄弱", "$weakCount", colorScheme.error)
                    StatItem("未学习", "$unlearnedCount", colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── 节点详情 BottomSheet（v0.8.0 学习导向重构）──────────────────────────────

/**
 * 节点详情 BottomSheet（v0.8.0 学习导向重构）。
 *
 * 增加学习入口：
 * - "开始复习"按钮（跳转到该节点关联知识点的复习界面）
 * - "查看真题"按钮（如有考频）
 * - "聚焦子图"按钮
 *
 * 关联知识点列表可直接跳转：
 * - 点击"查看详情" → 知识点详情页
 * - 点击"开始复习" → 复习界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeDetailSheet(
    node: GraphNodeItem,
    knowledgePointTitles: Map<String, String>,
    onDismiss: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToReview: (String) -> Unit,
    onFocusNode: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val typeDisplayName = GRAPH_TYPE_DISPLAY_NAME[node.type] ?: node.type
    val subjectDisplayName = GRAPH_SUBJECT_DISPLAY_NAME[node.subjectId] ?: "未分类"

    // 关联知识点列表（去重）
    val relatedPoints = remember(node) {
        buildList {
            node.relatedPointId?.let { add(it) }
            node.sourceKpIds.forEach { if (it !in this) add(it) }
        }
    }

    val masteryLevel = when {
        node.retrievability >= 0.8f -> "已掌握"
        node.retrievability >= 0.5f -> "巩固中"
        node.retrievability > 0f -> "薄弱"
        else -> "未学习"
    }
    val masteryColor = when {
        node.retrievability >= 0.8f -> MaterialTheme.colorScheme.primary
        node.retrievability >= 0.5f -> MaterialTheme.colorScheme.tertiary
        node.retrievability > 0f -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // 节点标题
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(masteryColor),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = node.label,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (!node.subtitle.isNullOrBlank()) {
                            Text(
                                text = node.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // 节点属性
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    DetailRow("类型", typeDisplayName)
                    DetailRow("科目", subjectDisplayName)
                    DetailRow("考频", node.examFrequency)
                    DetailRow("可提取性 R", "%.2f".format(node.retrievability))
                    DetailRow("掌握状态", masteryLevel)
                    if (node.sourceKpIds.isNotEmpty()) {
                        DetailRow("关联知识点数", "${node.sourceKpIds.size}")
                    }
                }
            }

            // 学习入口按钮（v0.8.0 新增）
            if (relatedPoints.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        // 开始复习
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToReview(relatedPoints.first()) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = "开始复习",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        // 聚焦子图
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            onClick = { onFocusNode(node.id) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationSearching,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = "聚焦子图",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            // 关联知识点列表
            if (relatedPoints.isNotEmpty()) {
                item {
                    Text(
                        text = "关联知识点（${relatedPoints.size}）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }
                items(relatedPoints) { pointId ->
                    val title = knowledgePointTitles[pointId] ?: pointId
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateToDetail(pointId) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = "查看详情",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "详情 →",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            // 底部安全间距
            item { Spacer(modifier = Modifier.height(Spacing.lg)) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
