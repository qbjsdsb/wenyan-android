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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.data.repository.WrongAnswerRepository
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 错题本界面。
 *
 * v0.9.0 起 WrongAnswer 从 quiz 子路由提升为顶级 Tab，支持两种形态：
 * - **顶级 Tab 模式**（默认）：onBack = null，顶栏无返回箭头，由底部 NavigationBar 切换
 * - **子路由模式**（保留兼容）：onBack 非 null，顶栏左侧显示返回箭头，popBackStack 返回上层
 *
 * 功能:
 * - 顶栏 "错题本" 标题（按需显示返回按钮）
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
    onBack: (() -> Unit)? = null,
    viewModel: WrongAnswerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    // v0.8.4 修复：collect errorMessage，删除/标记失败时通过 Snackbar 反馈
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    // v0.8.4 修复：errorMessage 非 null 时弹 Snackbar，展示后立即 clearError 避免重组重复弹
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "错题本",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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

            // v0.8.4 修复：Crossfade key 加 error 字段，加载失败时走 ErrorState 分支（原伪装为空状态）
            Crossfade(
                targetState = Triple(uiState.isLoading, uiState.error, uiState.items.isEmpty()),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "wrong_answer_state",
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
                            // v0.8.4 修复：Inbox 语义弱，改用 ErrorOutline 更贴切"错题"语义
                            icon = Icons.Default.ErrorOutline,
                            title = when (filter) {
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
    // v0.8.4 修复：仅 2 项过滤用 LazyRow 过度设计，改用普通 Row 减少开销
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // v0.8.4 修复：values() 已 deprecated，改用 entries
        WrongAnswerFilter.entries.forEach { filterOption ->
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
                    { Icon(Icons.Default.Check, contentDescription = null) }
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
    // v0.8.3 新增：删除二次确认状态，防止误触丢失学习数据
    var deletingItem by remember { mutableStateOf<WrongAnswerItem?>(null) }

    // v0.8.15 Stage 1: 横屏/平板下限制内容最大宽度并居中，避免错题卡片行宽过宽阅读疲劳。
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = MaxContentWidth.comfortable),
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
                    onDelete = { deletingItem = item },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    // v0.8.3 新增：删除确认 Dialog（与 ApiConfigScreen/AiAssistantScreen 行为一致）
    // v0.8.4 修复：Dialog 展示错题内容预览，多条时用户可确认删哪条
    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("删除错题") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("确定删除此错题吗？此操作不可撤销。")
                    Text(
                        text = "你的答案：${item.userAnswer.take(60)}${if (item.userAnswer.length > 60) "…" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(item.id)
                        deletingItem = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text("取消")
                }
            },
        )
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
                // v0.8.4 修复：surfaceVariant 配 onSurfaceVariant 而非默认 onSurface
                Text(
                    text = item.userAnswer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            // v0.8.4 修复：SimpleDateFormat 未 remember，每次重组创建新实例
            val timeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
            Text(
                text = "最后答错：${timeFormat.format(Date(item.lastWrongAt))}",
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
                // v0.8.4 修复：删除是破坏性操作，按钮用 error 色传达危险语义
                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = Spacing.xs),
                    )
                    Text("删除", color = MaterialTheme.colorScheme.error)
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
