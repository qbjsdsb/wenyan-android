package com.wenyan.app.feature.quiz

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.designsystem.component.ChipVariant
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanInfoChip
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 错题本界面(NF-PP5 Wave 3.2)。
 *
 * 功能:
 * - 顶栏返回按钮 + "错题本" 标题
 * - 过滤行:"未解决"(默认) / "全部"
 * - 错题列表:每张卡片展示来源 / 答错次数 / 用户答案 / 正确答案 / 时间
 * - 卡片操作行:
 *   - 未解决 → "标记已解决" OutlinedButton
 *   - 所有 → "删除" TextButton
 *
 * 数据来源:[WrongAnswerViewModel.uiState] 订阅 [WrongAnswerRepository.observeUnresolved] /
 * [WrongAnswerRepository.observeAll]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongAnswerScreen(
    onBack: () -> Unit = {},
    viewModel: WrongAnswerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "错题本",
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
            // 过滤行:未解决 / 全部
            WrongAnswerFilterRow(
                currentFilter = filter,
                onFilterSelected = viewModel::setFilter,
            )

            Crossfade(
                targetState = Triple(uiState.isLoading, uiState.items.isEmpty(), filter),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "wrong_answer_state",
                modifier = Modifier.fillMaxSize(),
            ) { (isLoading, isEmpty, currentFilter) ->
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            WenyanLoadingIndicator()
                        }
                    }
                    isEmpty -> {
                        EmptyState(
                            icon = Icons.Filled.Inbox,
                            title = when (currentFilter) {
                                WrongAnswerFilter.UNRESOLVED -> "暂无未解决错题"
                                WrongAnswerFilter.ALL -> "错题本为空"
                            },
                        )
                    }
                    else -> {
                        WrongAnswerList(
                            items = uiState.items,
                            onMarkResolved = viewModel::markResolved,
                            onDelete = viewModel::deleteById,
                        )
                    }
                }
            }
        }
    }
}

// 过滤行
@Composable
private fun WrongAnswerFilterRow(
    currentFilter: WrongAnswerFilter,
    onFilterSelected: (WrongAnswerFilter) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = Spacing.lg,
            vertical = Spacing.sm,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(
            items = WrongAnswerFilter.values(),
            key = { it.name },
            contentType = { "filter" },
        ) { filterOption ->
            FilterChip(
                selected = currentFilter == filterOption,
                onClick = { onFilterSelected(filterOption) },
                label = {
                    Text(
                        when (filterOption) {
                            WrongAnswerFilter.UNRESOLVED -> "未解决"
                            WrongAnswerFilter.ALL -> "全部"
                        },
                    )
                },
                leadingIcon = if (currentFilter == filterOption) {
                    { Text("✓") }
                } else {
                    null
                },
            )
        }
    }
}

// 错题列表
@Composable
private fun WrongAnswerList(
    items: List<WrongAnswerItem>,
    onMarkResolved: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = Spacing.lg,
            vertical = Spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(items = items, key = { it.id }, contentType = { "wrong_answer" }) { item ->
            WrongAnswerCard(
                item = item,
                onMarkResolved = { onMarkResolved(item.id) },
                onDelete = { onDelete(item.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/**
 * 单个错题卡片。
 *
 * 结构:
 * 1. 顶部信息行:来源标签 + 答错次数 + 解决状态
 * 2. 用户答案区
 * 3. 正确答案区(如有)
 * 4. 时间行:最后答错时间 + 首次记录时间
 * 5. 操作行:标记已解决(未解决时) / 删除
 */
@Composable
private fun WrongAnswerCard(
    item: WrongAnswerItem,
    onMarkResolved: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TonalCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // 1. 顶部信息行:来源 + 答错次数 + 解决状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WenyanInfoChip(
                    text = formatSource(item.source),
                    variant = ChipVariant.SECONDARY,
                )
                if (item.wrongCount > 1) {
                    WenyanInfoChip(
                        text = "错 ${item.wrongCount} 次",
                        variant = ChipVariant.ERROR,
                    )
                }
                if (item.isResolved) {
                    WenyanInfoChip(
                        text = "已解决",
                        variant = ChipVariant.PRIMARY,
                    )
                }
            }

            // 2. 用户答案区
            Text(
                text = "你的答案：",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs),
            ) {
                Text(
                    text = item.userAnswer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(Spacing.sm),
                )
            }

            // 3. 正确答案区(如有)
            item.correctAnswer?.takeIf { it.isNotBlank() }?.let { correct ->
                Text(
                    text = "正确答案：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                Text(
                    text = correct,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            // 4. 时间行
            Text(
                text = "最后答错：${formatTime(item.lastWrongAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm),
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.sm),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // 5. 操作行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!item.isResolved) {
                    OutlinedButton(onClick = onMarkResolved) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.padding(end = Spacing.xs),
                        )
                        Text("标记已解决")
                    }
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = Spacing.xs),
                    )
                    Text("删除")
                }
            }
        }
    }
}

// ── 辅助工具 ────────────────────────────────────────────────────

/** 将来源代码映射为中文显示 */
private fun formatSource(source: String): String = when (source) {
    WrongAnswerRepository.SOURCE_CARD_AGAIN -> "卡片复习"
    WrongAnswerRepository.SOURCE_QUIZ_WRONG -> "真题练习"
    else -> source
}

/** 时间戳格式化为 "yyyy-MM-dd HH:mm" */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
