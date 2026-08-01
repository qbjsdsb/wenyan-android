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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.common.util.ExamContentCleaner
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
import com.wenyan.app.core.fsrs.Rating
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
 * v0.9.4 新增 FSRS 间隔重复调度：
 * - 过滤行新增 "待复习" chip（DUE 模式），仅显示 sched_next_review_at <= now 的错题
 * - DUE 模式下每张卡片展示 FSRS 四档评分按钮（不会/困难/良好/简单）
 * - 评分后错题的 sched_next_review_at 更新，从待复习列表移除
 * - 所有模式都展示调度状态信息（下次复习时间 / 复习次数 / 遗忘次数）
 *
 * 功能:
 * - 顶栏 "错题本" 标题（按需显示返回按钮）
 * - 过滤行:"未解决"(默认) / "全部" / "待复习"
 * - 错题列表:每张卡片展示来源 / 答错次数 / 题目 / 用户答案 / 正确答案 / 调度信息 / 时间
 * - 卡片操作行:
 *   - DUE 模式 → FSRS 四档评分按钮（评分后自动调度）
 *   - 未解决 → "标记已解决" OutlinedButton
 *   - 所有 → "删除" TextButton
 *
 * 数据来源:[WrongAnswerViewModel.uiState] 订阅 [WrongAnswerRepository.observeUnresolved] /
 * [WrongAnswerRepository.observeAll] / [WrongAnswerRepository.observeDueWrongAnswers]。
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
            // 过滤行:未解决 / 全部 / 待复习（v0.9.4 新增 DUE）
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
                            // v0.9.4：DUE 模式用 Schedule 图标，传达"按计划复习"语义
                            icon = when (filter) {
                                WrongAnswerFilter.DUE -> Icons.Default.Schedule
                                else -> Icons.Default.ErrorOutline
                            },
                            title = when (filter) {
                                WrongAnswerFilter.UNRESOLVED -> "暂无未解决错题"
                                WrongAnswerFilter.ALL -> "错题本为空"
                                WrongAnswerFilter.DUE -> "暂无待复习错题"
                            },
                        )
                    }
                    else -> {
                        WrongAnswerList(
                            items = uiState.items,
                            filter = filter,
                            onMarkResolved = viewModel::markResolved,
                            onDelete = viewModel::deleteById,
                            onRate = viewModel::rateWrongAnswer,
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
    // v0.9.4：3 项过滤（含 DUE）仍用 Row，宽度足够
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
                            WrongAnswerFilter.DUE -> "待复习"
                        },
                    )
                },
                // v0.9.4：DUE chip 用 Schedule 图标传达"按计划复习"语义
                leadingIcon = if (currentFilter == filterOption) {
                    {
                        Icon(
                            imageVector = if (filterOption == WrongAnswerFilter.DUE) {
                                Icons.Default.Schedule
                            } else {
                                Icons.Default.Check
                            },
                            contentDescription = null,
                        )
                    }
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
    filter: WrongAnswerFilter,
    onMarkResolved: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRate: (String, Rating) -> Unit,
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
                    filter = filter,
                    onMarkResolved = { onMarkResolved(item.id) },
                    onDelete = { deletingItem = item },
                    onRate = { rating -> onRate(item.id, rating) },
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
                    // v0.9.2：Dialog 也显示题目，多条时用户可确认删哪条
                    item.questionTitle?.takeIf { it.isNotBlank() }?.let { title ->
                        Text(
                            text = "题目：${title.take(60)}${if (title.length > 60) "…" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
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
 * 1. 顶部信息行:来源标签 + 答错次数 + 解决状态 + FSRS 调度状态(v0.9.4)
 * 2. 题目区(v0.9.2 新增):知识点 title 或真题 content
 * 3. 用户答案区
 * 4. 正确答案区(如有)
 * 5. FSRS 调度信息区(v0.9.4 新增):下次复习时间 + 复习次数 + 遗忘次数
 * 6. 时间行:最后答错时间 + 首次记录时间
 * 7. 操作行:
 *    - DUE 模式 → FSRS 四档评分按钮（不会/困难/良好/简单）
 *    - 未解决 → "标记已解决" OutlinedButton
 *    - 所有 → "删除" TextButton
 *
 * v0.9.4 新增:
 * - DUE 模式下展示 FSRS 四档评分按钮（颜色编码与 CardsScreen 一致）
 * - 所有模式展示调度状态信息（下次复习时间 / 复习次数 / 遗忘次数）
 * - 调度状态 chip：NEW=未学习 / LEARNING=学习中 / REVIEW=复习中 / RELEARNING=重学中
 */
@Composable
private fun WrongAnswerCard(
    item: WrongAnswerItem,
    filter: WrongAnswerFilter,
    onMarkResolved: () -> Unit,
    onDelete: () -> Unit,
    onRate: (Rating) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    TonalCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // 1. 顶部信息行:来源 + 答错次数 + 解决状态 + FSRS 调度状态
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
                // v0.9.4：FSRS 调度状态 chip（仅复习次数 > 0 时显示，避免新错题噪音）
                if (item.schedReps > 0) {
                    WenyanInfoChip(
                        text = formatSchedState(item.schedState),
                        variant = when (item.schedState) {
                            "REVIEW" -> ChipVariant.PRIMARY
                            "LEARNING", "RELEARNING" -> ChipVariant.TERTIARY
                            else -> ChipVariant.SECONDARY
                        },
                    )
                }
            }

            // 2. 题目区(v0.9.2 新增):展示关联的题目文本
            //    卡片来源=知识点 title,真题来源=真题 content
            //    questionTitle 理论不应为 null（FK 保证关联记录存在），
            //    但 LEFT JOIN 仍可能返回 null（如 FK 记录被删除），兜底显示"题目已删除"
            val title = item.questionTitle?.takeIf { it.isNotBlank() }
            Text(
                text = "题目：",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            Text(
                text = title?.let { ExamContentCleaner.stripQuestionNumber(it) } ?: "（题目已删除）",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (title != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = Spacing.xs),
            )

            // 3. 用户答案区
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

            // 4. 正确答案区(如有)
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

            // 5. FSRS 调度信息区（v0.9.4 新增）
            //    展示下次复习时间 + 复习次数 + 遗忘次数
            //    新建错题（schedReps=0）仅显示"待学习"，不显示复习统计
            WrongAnswerSchedulingInfo(
                item = item,
                timeFormat = timeFormat,
                modifier = Modifier.padding(top = Spacing.sm),
            )

            // 6. 时间行
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

            // 7. 操作行
            //    v0.9.4：DUE 模式优先展示 FSRS 评分按钮（替代"标记已解决"）
            if (filter == WrongAnswerFilter.DUE) {
                WrongAnswerRatingButtons(onRate = onRate)
            } else {
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
}

/**
 * FSRS 调度信息展示（v0.9.4 新增）。
 *
 * 展示错题的 FSRS 调度状态:
 * - 新建错题（schedReps=0）→ "待学习"提示
 * - 已调度错题（schedReps>0）→ 下次复习时间 + 复习次数 + 遗忘次数
 *
 * 遗忘次数 > 0 时用 error 色高亮，提示用户该错题曾多次遗忘需重点记忆。
 */
@Composable
private fun WrongAnswerSchedulingInfo(
    item: WrongAnswerItem,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.sm)) {
            if (item.schedReps == 0) {
                // 新建错题，从未复习
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = Spacing.xs),
                    )
                    Text(
                        text = "待学习：切换到「待复习」标签开始 FSRS 调度",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // 已调度，展示下次复习时间 + 统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    val nextReviewText = if (item.schedNextReviewAt > 0) {
                        timeFormat.format(Date(item.schedNextReviewAt))
                    } else {
                        "立即"
                    }
                    Text(
                        text = "下次复习：$nextReviewText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        text = "复习 ${item.schedReps} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.schedLapses > 0) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        // v0.9.4：遗忘次数 > 0 用 error 色高亮，提示重点记忆
                        Text(
                            text = "遗忘 ${item.schedLapses} 次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * FSRS 四档评分按钮（v0.9.4 新增）。
 *
 * 颜色编码与 feature/cards/CardsScreen.kt 的 RatingButtons 完全一致:
 * - AGAIN：error 容器（红，警告）— "完全不会"
 * - HARD：tertiary 容器（黄/橙，注意）— "有难度"
 * - GOOD：secondary 容器（绿，成功）— "掌握了"（FSRS 标准间隔）
 * - EASY：primary 容器（蓝，加成）— "很简单"
 *
 * 与 CardsScreen 的差异:
 * - 不显示预期间隔（错题调度不调用 previewIntervals，简化 UI）
 * - 仅 DUE 模式显示，评分后错题自动从待复习列表移除
 *
 * 所有按钮 heightIn(min=48dp) 满足 M3 触控目标规范。
 *
 * @param onRate 评分回调，参数为 FSRS Rating
 */
@Composable
private fun WrongAnswerRatingButtons(
    onRate: (Rating) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // AGAIN：红色警示（"完全不会"）— 重置到学习阶段
        WrongAnswerRatingButton(
            label = "不会",
            onClick = { onRate(Rating.AGAIN) },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            isPrimary = false,
            modifier = Modifier.weight(1f),
        )

        // HARD：黄/橙色（"有难度"）— 短间隔复习
        WrongAnswerRatingButton(
            label = "困难",
            onClick = { onRate(Rating.HARD) },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            isPrimary = false,
            modifier = Modifier.weight(1f),
        )

        // GOOD：绿色（"掌握了"，FSRS 标准间隔）— 默认推荐评分
        WrongAnswerRatingButton(
            label = "良好",
            onClick = { onRate(Rating.GOOD) },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            isPrimary = true,
            modifier = Modifier.weight(1f),
        )

        // EASY：蓝色（"很简单"，加成间隔）
        WrongAnswerRatingButton(
            label = "简单",
            onClick = { onRate(Rating.EASY) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            isPrimary = false,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 单个错题评分按钮（v0.9.4 新增）。
 *
 * 与 CardsScreen 的 RatingButton 结构一致，仅 label 无 intervalText（错题不显示预期间隔）。
 * isPrimary=true 用 [Button]（filled），false 用 [FilledTonalButton]。
 */
@Composable
private fun WrongAnswerRatingButton(
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "$label：评分后调度下次复习" },
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "$label：评分后调度下次复习" },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── 辅助工具 ────────────────────────────────────────────────────

/** 将来源代码映射为中文显示 */
private fun formatSource(source: String): String = when (source) {
    WrongAnswerRepository.SOURCE_CARD_AGAIN -> "卡片复习"
    WrongAnswerRepository.SOURCE_QUIZ_WRONG -> "真题练习"
    WrongAnswerRepository.SOURCE_ESSAY_PRACTICE -> "论述题自评"
    else -> source
}

/**
 * 将 FSRS 调度状态代码映射为中文显示（v0.9.4 新增）。
 *
 * 状态语义对照 FSRS-6:
 * - NEW → 新建（未学习，sched_reps=0，由 [WrongAnswerSchedulingInfo] 单独提示）
 * - LEARNING → 学习中（首次学习阶段，短间隔分钟/小时级）
 * - REVIEW → 复习中（已掌握进入长期间隔复习，天/周级）
 * - RELEARNING → 重学中（REVIEW 状态评 AGAIN 后重置，遗忘一次 lapses++）
 *
 * 未知状态兜底返回原字符串，便于排查 DB 脏数据。
 */
private fun formatSchedState(state: String): String = when (state) {
    "NEW" -> "新建"
    "LEARNING" -> "学习中"
    "REVIEW" -> "复习中"
    "RELEARNING" -> "重学中"
    else -> state
}
