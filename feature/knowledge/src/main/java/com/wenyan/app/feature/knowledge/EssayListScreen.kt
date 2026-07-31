package com.wenyan.app.feature.knowledge

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.ChipVariant
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanInfoChip
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

/**
 * 论述题列表界面（v0.9.8 Phase 2 新增，对应 docs/design/essay-module-design.md Phase 2）。
 *
 * 入口：知识点列表页顶部"论述题练习"入口卡片 → 本页。
 *
 * 功能：
 * - 三维筛选：年份（LazyRow FilterChip）/ 科目（LazyRow FilterChip）/ 仅显示有审题思路（toggle chip）
 * - 列表卡片：年份 + 科目 + 分值 chip + 题目预览 + 审题思路/依据/关联知识点标记
 * - 点击进入论述题详情页（10 区块结构）
 *
 * 数据流：[EssayListViewModel] combine(observeAllEssays, observeSubjects, 3个筛选StateFlow)
 * 筛选在内存完成（134 题规模 < 5ms）。
 *
 * 设计依据：用户需求"增加论述题板块融合在知识点板块"，入口放在知识点 Tab，
 * 与知识点详情页的"相关论述题"区块形成双入口（列表浏览 + 知识点关联跳转）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EssayListScreen(
    onBack: () -> Unit = {},
    onNavigateToEssayDetail: (String) -> Unit = {},
    viewModel: EssayListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsStateWithLifecycle()
    val onlyWithAngle by viewModel.onlyWithAngle.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "论述题",
                subtitle = if (uiState.totalCount > 0) {
                    if (uiState.filteredCount != uiState.totalCount) {
                        "${uiState.filteredCount} / ${uiState.totalCount} 题"
                    } else {
                        "共 ${uiState.totalCount} 题"
                    }
                } else {
                    null
                },
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            // ── 筛选区 ──
            EssayFilterBar(
                availableYears = uiState.availableYears,
                subjects = uiState.subjects,
                selectedYear = selectedYear,
                selectedSubjectId = selectedSubjectId,
                onlyWithAngle = onlyWithAngle,
                onYearSelected = viewModel::selectYear,
                onSubjectSelected = viewModel::selectSubject,
                onToggleOnlyWithAngle = viewModel::toggleOnlyWithAngle,
            )

            // ── 列表区 ──
            Crossfade(
                targetState = Triple(uiState.isLoading, uiState.error, uiState.essays.isEmpty()),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "essay_list_state",
                modifier = Modifier.fillMaxSize(),
            ) { (isLoading, error, isEmpty) ->
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { WenyanLoadingIndicator() }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            ErrorState(
                                icon = Icons.Default.CloudOff,
                                title = "加载失败",
                                onRetry = viewModel::retry,
                                message = error,
                            )
                        }
                    }
                    isEmpty -> {
                        val hasFilter = selectedYear != null || selectedSubjectId != null || onlyWithAngle
                        EmptyState(
                            icon = Icons.Default.Inbox,
                            title = if (hasFilter) "当前筛选无匹配论述题" else "暂无论述题",
                            description = if (hasFilter) "尝试调整筛选条件" else "等待种子数据加载",
                        )
                    }
                    else -> {
                        EssayList(
                            items = uiState.essays,
                            onNavigateToEssayDetail = onNavigateToEssayDetail,
                            contentPadding = PaddingValues(Spacing.lg),
                        )
                    }
                }
            }
        }
    }
}

// ── 筛选栏 ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EssayFilterBar(
    availableYears: List<Int>,
    subjects: List<com.wenyan.app.core.database.entity.SubjectEntity>,
    selectedYear: Int?,
    selectedSubjectId: String?,
    onlyWithAngle: Boolean,
    onYearSelected: (Int?) -> Unit,
    onSubjectSelected: (String?) -> Unit,
    onToggleOnlyWithAngle: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // 年份筛选
        if (availableYears.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                item(key = "year_all", contentType = "yearChip") {
                    FilterChip(
                        selected = selectedYear == null,
                        onClick = { onYearSelected(null) },
                        label = { Text("全部年份") },
                    )
                }
                items(items = availableYears, key = { it }, contentType = { "yearChip" }) { year ->
                    FilterChip(
                        selected = selectedYear == year,
                        onClick = { onYearSelected(year) },
                        label = { Text("${year}年") },
                    )
                }
            }
        }

        // 科目筛选 + 审题思路开关（FlowRow 自动换行）
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            FilterChip(
                selected = selectedSubjectId == null,
                onClick = { onSubjectSelected(null) },
                label = { Text("全部科目") },
            )
            subjects.forEach { subject ->
                FilterChip(
                    selected = selectedSubjectId == subject.id,
                    onClick = { onSubjectSelected(subject.id) },
                    label = { Text(subject.name) },
                )
            }
            // 审题思路开关（与科目 chip 同行，空间不足自动换行）
            FilterChip(
                selected = onlyWithAngle,
                onClick = onToggleOnlyWithAngle,
                label = { Text("仅有审题思路") },
                leadingIcon = if (onlyWithAngle) {
                    {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

// ── 列表 ────────────────────────────────────────────────────

@Composable
private fun EssayList(
    items: List<EssayListItem>,
    onNavigateToEssayDetail: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = MaxContentWidth.comfortable),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(items = items, key = { it.id }, contentType = { "essayItem" }) { item ->
                EssayListItemCard(
                    item = item,
                    onClick = { onNavigateToEssayDetail(item.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

// ── 列表项卡片 ──────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EssayListItemCard(
    item: EssayListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TonalCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            // 第一行：年份 + 科目 + 分值 chip
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                WenyanInfoChip(text = "${item.year}年", variant = ChipVariant.PRIMARY)
                WenyanInfoChip(text = item.subjectName, variant = ChipVariant.SECONDARY)
                if (item.score > 0) {
                    WenyanInfoChip(text = "${item.score}分", variant = ChipVariant.NEUTRAL)
                }
                if (item.hasAngle) {
                    WenyanInfoChip(text = "审题思路", variant = ChipVariant.TERTIARY)
                }
            }

            // 第二行：题目预览
            Text(
                text = item.contentPreview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // 第三行：底部标记（依据 + 关联知识点数）
            if (item.hasNotes || item.relatedPointCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    if (item.hasNotes) {
                        Text(
                            text = "✓ 有依据",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (item.relatedPointCount > 0) {
                        Text(
                            text = "关联 ${item.relatedPointCount} 个知识点",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Previews ───────────────────────────────────────────────

@Preview(name = "Essay List - Light", showBackground = true)
@Composable
private fun EssayListLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        androidx.compose.material3.Surface {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                EssayListItemCard(
                    item = EssayListItem(
                        id = "eq_0038",
                        year = 2008,
                        subjectName = "中国现当代文学",
                        score = 30,
                        contentPreview = "试述冰心，丁玲，萧红，张爱玲，王安忆五位女作家创作的异同，并梳理她们在不同时期的创作演变。",
                        hasAngle = true,
                        hasNotes = true,
                        relatedPointCount = 5,
                    ),
                    onClick = {},
                )
                EssayListItemCard(
                    item = EssayListItem(
                        id = "eq_0182",
                        year = 2015,
                        subjectName = "中国现当代文学",
                        score = 25,
                        contentPreview = "结合具体作品，论述寻根文学的代表作家及其文学史意义。",
                        hasAngle = false,
                        hasNotes = false,
                        relatedPointCount = 3,
                    ),
                    onClick = {},
                )
            }
        }
    }
}
