package com.wenyan.app.feature.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.data.cards.ClozeQuoteCard
import com.wenyan.app.core.data.repository.IntervalPreview
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme
import com.wenyan.app.core.fsrs.Rating

/**
 * 记忆卡片界面。
 *
 * 实现卡片正反面翻转交互：
 * - 点击卡片翻转（正面问题 / 背面答案）
 * - 翻转后展示 FSRS 四档评分按钮（Again/Hard/Good/Easy）
 *
 * v0.8.5 UI 改造（与 ViewModel 状态对齐）：
 * 1. 区分四种状态：加载中 / 错误 / 会话完成 / 无到期卡 / 正常复习
 *    （原实现只能识别"空"，无法区分"今日没卡"vs"刚刚完成一轮"）
 * 2. 评分按钮颜色编码：AGAIN=error 红 / HARD=tertiary 黄 / GOOD=primary 蓝 / EASY=secondary 绿
 *    （原实现四个按钮全是中性色，用户无法一眼识别评分语义）
 * 3. 完成态展示会话统计：已复习 N 张 / AGAIN M 张 / 完成率
 * 4. 撤销按钮：currentIndex > 0 时可见，回退上一张（不回滚 FSRS）
 * 5. 进度条：用 LinearProgressIndicator 替代纯文字 "3 / 12"
 *
 * 翻转动画通过 graphicsLayer rotationY 实现。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToKnowledge: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: CardsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val sessionReviewed by viewModel.sessionReviewedCount.collectAsStateWithLifecycle()
    val sessionAgain by viewModel.sessionAgainCount.collectAsStateWithLifecycle()
    // v0.8.7 P0：接通评分预期间隔预览 + Leech 警告（原 ViewModel 已实现但 UI 未接通）
    val currentPreviews by viewModel.currentPreviews.collectAsStateWithLifecycle()
    val leechWarning by viewModel.leechWarning.collectAsStateWithLifecycle()
    // v0.8.9 P1-2:接通 sibling 已评分状态,UI 据此隐藏误导性预期间隔
    val isSiblingAlreadyRated by viewModel.isSiblingAlreadyRated.collectAsStateWithLifecycle()
    // v0.8.17 P1:会话时长改为 StateFlow,避免 Composable 中直接调用函数破坏重组稳定性
    val sessionDurationMinutes by viewModel.sessionDurationMinutes.collectAsStateWithLifecycle()
    // v0.9.18: 手动加入错题本状态
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val isAddingBookmark by viewModel.isAddingBookmark.collectAsStateWithLifecycle()
    val manualAddedPointIds by viewModel.manualAddedPointIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    // errorMessage 非 null 时弹 Snackbar，展示后立即 clearError 避免重组重复弹
    // v0.8.14 P0-8 修复:Leech 警告(AlertDialog)显示时不弹 Snackbar,避免两者同时
    // 弹出造成用户注意力分散 + AlertDialog 导航走后 Snackbar 未消费被销毁。
    // 修复策略:leechWarning 非空时暂存 errorMessage 不弹,leechWarning 清除后
    // errorMessage 仍存在(未 clearError),LaunchedEffect 重新触发弹 Snackbar。
    LaunchedEffect(errorMessage, leechWarning) {
        val error = errorMessage
        if (error != null && leechWarning == null) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // v0.9.18: 成功消息（加入错题本）弹 Snackbar
    LaunchedEffect(successMessage) {
        val msg = successMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSuccessMessage()
        }
    }

    // v0.8.7 P0：Leech 警告用 AlertDialog 展示（比 Snackbar 更醒目，需用户主动确认）
    // v0.8.8：携带 pointId，新增"查看知识点"按钮直接跳转 detail 页处理
    // v0.8.12 P0-8：新增"问 AI 助手"按钮，补全操作路径；文案移除"拆分卡片"（App 不支持）
    leechWarning?.let { warning ->
        // v0.8.13 P1-3:两个 confirmButton 之间加 Spacing.sm,
        // 原实现 Row 内两个 TextButton 紧贴,视觉拥挤且触控目标易误触
        AlertDialog(
            onDismissRequest = viewModel::clearLeechWarning,
            title = { Text("需要重点关注") },
            text = { Text(warning.message) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TextButton(onClick = {
                        viewModel.clearLeechWarning()
                        onNavigateToAiAssistant()
                    }) {
                        Text("问 AI 助手")
                    }
                    TextButton(onClick = {
                        val pid = warning.pointId
                        viewModel.clearLeechWarning()
                        if (pid.isNotBlank()) onNavigateToDetail(pid)
                    }) {
                        Text("查看知识点")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearLeechWarning) {
                    Text("知道了")
                }
            },
        )
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "记忆卡片",
                actions = {
                    IconButton(onClick = onNavigateToAiAssistant) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI助手",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        // 用四元组键控 Crossfade：覆盖加载/错误/空/完成/正常卡片五种状态
        // isFinished=true 表示用户已经评完一轮（区分于"今日无到期卡"）
        val stateKey = CardsStateKey(
            isLoading = uiState.isLoading,
            error = uiState.error,
            isFinished = uiState.isFinished,
            hasCards = uiState.currentCard != null,
        )
        Crossfade(
            targetState = stateKey,
            animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
            label = "cards_state",
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .padding(Spacing.lg),
        ) { key ->
            when {
                key.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        WenyanLoadingIndicator()
                    }
                }
                key.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ErrorState(
                            icon = Icons.Default.CloudOff,
                            title = "加载失败",
                            message = key.error,
                            onRetry = viewModel::retry,
                        )
                    }
                }
                key.isFinished -> {
                    // 会话完成态：展示本次复习统计 + 会话时长 + 鼓励继续 / 返回
                    // v0.8.17 P1:sessionDurationMinutes 改为 collect StateFlow,
                    // 避免在 Composable 函数体中直接调用 viewModel.getSessionDurationMinutes()
                    // 破坏重组稳定性(每次重组返回不同值,SessionCompleteState 无谓重组)
                    // v0.9.7 M5:新增 onUndo 参数,允许用户撤销最后一张卡的评分(回退到 CardReviewContent)
                    SessionCompleteState(
                        reviewedCount = sessionReviewed,
                        againCount = sessionAgain,
                        sessionDurationMinutes = sessionDurationMinutes,
                        onRetry = viewModel::retry,
                        onUndo = viewModel::undo,
                        onExit = onNavigateToKnowledge,
                    )
                }
                !key.hasCards -> {
                    // 今日无到期卡（首次进入就没卡，与"刚完成一轮"区分）
                    // v0.8.8：加"去学习"按钮引导用户到知识点列表
                    EmptyState(
                        icon = Icons.Default.CheckCircle,
                        title = "今天没有到期卡片",
                        description = "已全部复习完毕，可去知识点列表预习新内容",
                        action = {
                            FilledTonalButton(
                                onClick = onNavigateToKnowledge,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = Spacing.xs),
                                )
                                Text("去学习")
                            }
                        },
                    )
                }
                else -> {
                    uiState.currentCard?.let { card ->
                        CardReviewContent(
                            card = card,
                            uiState = uiState,
                            previews = currentPreviews,
                            isSiblingAlreadyRated = isSiblingAlreadyRated,
                            isInWrongBook = card.pointId in manualAddedPointIds,
                            isAddingBookmark = isAddingBookmark,
                            onFlip = viewModel::flipCard,
                            onRate = viewModel::rateCard,
                            onUndo = viewModel::undo,
                            onSkip = viewModel::skipCard,
                            onAddToWrongAnswerBook = viewModel::addToWrongAnswerBook,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Crossfade 状态键（避免 Triple 语义不清）。
 *
 * - [isLoading]：首次加载中
 * - [error]：加载失败
 * - [isFinished]：本次会话已完成（currentIndex 推到末尾）
 * - [hasCards]：当前有可复习卡片
 *
 * 优先级：isLoading > error > isFinished > hasCards
 */
private data class CardsStateKey(
    val isLoading: Boolean,
    val error: String?,
    val isFinished: Boolean,
    val hasCards: Boolean,
)

/**
 * 正常卡片复习态：进度 + 翻转卡 + 评分按钮。
 *
 * v0.8.7：新增 [previews] 参数，传入当前卡片 4 档评分的预期间隔，
 * 评分按钮下方显示"1分钟 / 6天 / 12天"等预期间隔（参考 Anki）。
 *
 * v0.8.9 P1-2:新增 [isSiblingAlreadyRated] 参数,sibling 卡(同 pointId 已评分过)
 * 隐藏误导性预期间隔,改为显示"已调度(同知识点首卡已评分)"提示。
 * 原实现 sibling 卡也显示"GOOD→6天",但评分不会触发 FSRS 调度,误导用户。
 */
@Composable
private fun CardReviewContent(
    card: CardItem,
    uiState: CardsUiState,
    previews: Map<Rating, IntervalPreview>,
    isSiblingAlreadyRated: Boolean,
    isInWrongBook: Boolean,
    isAddingBookmark: Boolean,
    onFlip: () -> Unit,
    onRate: (CardRating) -> Unit,
    onUndo: () -> Unit,
    onSkip: () -> Unit,
    onAddToWrongAnswerBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // v0.8.13 P1-4:大屏适配,限制内容最大宽度避免卡片和按钮在平板/折叠屏上拉伸过宽
    // 600dp 对应 Material3 中型窗口断点,超过此宽度居中显示留白
    // 用 Box 包裹实现居中:fillMaxSize 占满父容器,widthIn 限制 Column 最大宽度,
    // contentAlignment=CenterHorizontally 让 Column 在大屏上水平居中
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
        // 进度区：文字 + 进度条
        ProgressSection(
            currentIndex = uiState.currentIndex,
            total = uiState.cards.size,
            modifier = Modifier.fillMaxWidth(),
        )

        // 可翻转卡片
        FlipCard(
            card = card,
            isFlipped = uiState.isFlipped,
            onClick = onFlip,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        // 评分按钮（翻转后）+ 撤销/跳过按钮 / 翻转前提示文案
        // 用 AnimatedVisibility 替代 if/else 硬切
        AnimatedVisibility(
            visible = uiState.isFlipped,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                // v0.8.10 P1-B3 修复:sibling 已评分时仍保留评分按钮,仅隐藏预期间隔。
                // 原实现 isSiblingAlreadyRated=true 时完全隐藏 RatingButtons,导致用户
                // 无法评分推进(只能跳过),也无法记录错题(sibling AGAIN 仍会调用
                // wrongAnswerRepository.recordWrongAnswer,但 UI 隐藏按钮后用户无法触发)。
                //
                // 修复策略:
                // - SiblingRatedHint 作为信息提示放在评分按钮上方(非替换)
                // - RatingButtons 始终渲染,sibling 时传空 previews(不显示预期间隔)
                //   避免误导用户以为评分会改变调度间隔
                // - 用户可正常评分推进,AGAIN 评分仍记录错题(不影响 FSRS 调度)
                if (isSiblingAlreadyRated) {
                    SiblingRatedHint()
                }
                RatingButtons(
                    onRate = onRate,
                    previews = if (isSiblingAlreadyRated) emptyMap() else previews,
                )
                // v0.9.18: 手动加入错题本按钮
                AddToWrongAnswerButton(
                    isInWrongBook = isInWrongBook,
                    isLoading = isAddingBookmark,
                    pointId = card.pointId,
                    onClick = onAddToWrongAnswerBook,
                )
                // v0.8.8：撤销 + 跳过按钮横排
                // 撤销：回退上一张（currentIndex > 0 才显示）
                // 跳过：不评分推进到下一张（避免乱评污染 FSRS）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (uiState.currentIndex > 0) {
                        UndoButton(onUndo = onUndo, modifier = Modifier.weight(1f))
                    }
                    SkipButton(onSkip = onSkip, modifier = Modifier.weight(1f))
                }
            }
        }
        AnimatedVisibility(
            visible = !uiState.isFlipped,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            // 翻转前的辅助提示 + 撤销/跳过按钮
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = "点击卡片查看答案",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // v0.9.18: 手动加入错题本按钮（翻转前也可加入）
                AddToWrongAnswerButton(
                    isInWrongBook = isInWrongBook,
                    isLoading = isAddingBookmark,
                    pointId = card.pointId,
                    onClick = onAddToWrongAnswerBook,
                )
                // v0.8.12 P2-14：未翻转也显示撤销/跳过按钮（与翻转后一致）
                // 原实现未翻转只显示跳过，用户跳过后想撤销必须先翻转才能看到撤销按钮，操作迂回
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (uiState.currentIndex > 0) {
                        UndoButton(onUndo = onUndo, modifier = Modifier.weight(1f))
                    }
                    SkipButton(onSkip = onSkip, modifier = Modifier.weight(1f))
                }
            }
        }
        }
    }
}

/**
 * sibling 卡已评分提示(v0.8.9 P1-2 新增,v0.8.10 P1-B3 重构)。
 *
 * 当前卡片与已评分的首张 sibling 卡共享同一知识点,评分不会触发 FSRS 调度
 * (调度已在首张卡完成,参考 Anki sibling burying 避免重复评分导致 stability 虚高)。
 *
 * v0.8.10 P1-B3 修复:
 * - 原实现用 SiblingRatedHint **替换** RatingButtons,导致用户无法评分推进
 * - 现改为 SiblingRatedHint 作为信息提示放在 RatingButtons **上方**
 * - 评分按钮始终渲染(不显示预期间隔),用户可正常评分推进
 * - AGAIN 评分仍记录错题(不影响 FSRS 调度,但保留错题本更新)
 */
@Composable
private fun SiblingRatedHint(modifier: Modifier = Modifier) {
    // v0.8.12 P0-7:文案去术语化,图标从 CheckCircle 改为 Info
    // v0.8.13 P1-1:文案补全"评分仍会记入错题本和会话统计",
    // 解决用户"既然不改变计划,为什么还要评分"的困惑。
    // 实际:sibling 卡评分不触发 FSRS 调度(避免 stability 虚高),
    // 但 AGAIN 仍记录错题,所有评分仍累加 sessionReviewedCount(用于完成态统计)。
    //
    // v0.8.14 P2-7 修复:原文案"仍会记入错题本和会话统计"对所有评分都显示,
    // 但只有 AGAIN 评分才记入错题本,GOOD/HARD/EASY 不记。用户可能误以为评 GOOD
    // 也会记错题,造成困惑。现明确区分:AGAIN 记错题,所有评分计入会话统计。
    val hintText = "这张卡和刚复习的卡同属一个知识点，评分不会改变复习计划，" +
        "评 AGAIN 仍会记入错题本"
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = hintText
            },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = Spacing.xs),
            )
            Text(
                text = hintText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.xs),
            )
        }
    }
}

/**
 * 进度区：当前 N / 总数 + 进度条。
 *
 * 用 LinearProgressIndicator 替代纯文字，视觉更直观。
 */
@Composable
private fun ProgressSection(
    currentIndex: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val safeTotal = total.coerceAtLeast(1)
    val progress by remember(currentIndex, total) {
        derivedStateOf { currentIndex.toFloat() / safeTotal.toFloat() }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${currentIndex + 1} / $total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.animateContentSize(),
        )
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 4.dp)
                .semantics {
                    contentDescription = "复习进度：第 ${currentIndex + 1} 张，共 $total 张"
                },
        )
    }
}

/**
 * 会话完成态：展示本次复习统计 + 会话时长 + 重开/返回按钮。
 *
 * v0.8.5 新增：让用户看到本轮复习的实际效果（N 张 / M 张不会），
 * 替代原"今日复习已完成"的笼统提示。区分"刚完成"与"今日无卡"两种场景。
 *
 * v0.8.7 新增：
 * - [sessionDurationMinutes]：本次会话用时（分钟），展示"本次用时 X 分钟"提升成就感
 * - [onExit]：退出按钮，导航到知识点列表（原仅有"再复习一轮"，用户无法明确离开）
 *
 * - [reviewedCount]：本次会话已评分卡片数
 * - [againCount]：本次会话评 AGAIN 的卡片数（含 sibling）
 * - [onRetry]：重开一轮（重新加载 due 卡）
 */
@Composable
private fun SessionCompleteState(
    reviewedCount: Int,
    againCount: Int,
    sessionDurationMinutes: Int,
    onRetry: () -> Unit,
    onUndo: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 计算掌握率：((reviewedCount - againCount) / reviewedCount).coerceIn(0, 1)
    val masteryRate = if (reviewedCount > 0) {
        ((reviewedCount - againCount).toFloat() / reviewedCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    // 鼓励文案：根据掌握率选择
    // v0.8.12 P2：reviewedCount==0 不应进入完成态，但防御性处理
    val encouragement = when {
        reviewedCount == 0 -> "本次没有需要复习的卡片"
        masteryRate >= 0.85f -> "掌握得很好，继续保持"
        masteryRate >= 0.6f -> "稳步进步，下次再战"
        else -> "需要重点巩固，加油"
    }
    val fullDescription = buildString {
        append("本次复习完成，用时 $sessionDurationMinutes 分钟，共 $reviewedCount 张")
        if (againCount > 0) append("，其中 $againCount 张需要重新记忆")
        append("，$encouragement")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xxl)
            .semantics(mergeDescendants = true) {
                contentDescription = fullDescription
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.padding(top = Spacing.xl),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "本次复习完成",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        // v0.8.7：会话用时（提升学习成就感）
        // v0.8.13 P0-2:reviewedCount=0 时隐藏会话时长和统计卡片,只显示空状态文案
        // (此场景为防御性兜底,正常流程 reviewedCount=0 应进入"今日无到期卡"分支)
        if (reviewedCount > 0) {
            Text(
                text = "用时 $sessionDurationMinutes 分钟",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 统计卡：复习张数 / AGAIN 张数 / 掌握率
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                StatCard(
                    label = "已复习",
                    value = reviewedCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "需重练",
                    value = againCount.toString(),
                    color = if (againCount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "掌握率",
                    value = "${(masteryRate * 100).toInt()}%",
                    color = when {
                        masteryRate >= 0.85f -> MaterialTheme.colorScheme.primary
                        masteryRate >= 0.6f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = encouragement,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            // reviewedCount=0 兜底:仅显示空状态文案,不展示统计卡片
            Text(
                text = encouragement,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = null,
                modifier = Modifier.padding(end = Spacing.sm),
            )
            Text("再复习一轮")
        }
        // v0.9.7 M5:撤销最后一张卡评分,回退到 CardReviewContent 重新评分。
        // 适用场景:用户评完最后一张才发现评错了(如本想评 GOOD 却点了 AGAIN),
        // 此时 Crossfade 已切到完成态,原实现无法撤销。现提供撤销入口,
        // undo 后 currentIndex 回退,isFinished 变 false,Crossfade 切回复习态。
        // 仅在 reviewedCount > 0 时显示(无评分无可撤销)。
        if (reviewedCount > 0) {
            FilledTonalButton(
                onClick = onUndo,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = null,
                    modifier = Modifier.padding(end = Spacing.sm),
                )
                Text("撤销最后一张")
            }
        }
        // v0.8.7：退出按钮,让用户明确离开复习(导航到知识点列表)
        // v0.8.13 P1-2:文案从"返回学习"改为"返回知识点列表"
        // 原文案"返回学习"语义模糊(cards tab 本身就是学习),实际行为是去知识点列表浏览
        TextButton(
            onClick = onExit,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("返回知识点列表")
        }
    }
}

/**
 * 单个统计数字卡（用于 SessionCompleteState）。
 */
@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 撤销按钮：仅 UI 回退，不回滚 FSRS 调度。
 *
 * v0.8.5 新增：参考 Anki Z 键撤销，简化为 TextButton 风格，
 * 触控目标 ≥48dp，左对齐 Undo 图标 + 文字。
 *
 * v0.8.8：新增 [modifier] 参数，支持在 Row 中 weight 分配宽度。
 */
@Composable
private fun UndoButton(
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onUndo,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "撤销上一张" },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = null,
            modifier = Modifier.padding(end = Spacing.xs),
        )
        Text("撤销")
    }
}

/**
 * 跳过按钮(v0.8.8 新增)。
 *
 * 不评分推进到下一张,避免乱评污染 FSRS 数据。
 * 适用:卡片内容有误/临时不想答。
 *
 * 翻转前后均可用(未翻转时也可跳过,避免卡在不懂的卡上)。
 */
@Composable
private fun SkipButton(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onSkip,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "跳过当前卡片，不评分" },
    ) {
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = null,
            modifier = Modifier.padding(end = Spacing.xs),
        )
        Text("跳过")
    }
}

// ---------- v0.9.18 新增：加入错题本按钮 ----------

/**
 * 手动加入错题本按钮（v0.9.18 新增）。
 *
 * 在知识卡片正面（翻转前）和背面（翻转后评分按钮下方）均显示。
 * 用户点击后将该卡片关联的知识点加入错题本，作为"手动加入"来源。
 *
 * 状态：
 * - 未加入：显示 BookmarkBorder 图标 + "加入错题本"
 * - 已加入：显示 CheckCircle 图标 + "已加入错题本"（禁用）
 * - 加载中：显示 CircularProgressIndicator + "加入中..."（禁用）
 * - pointId 为空：显示 "无法加入错题本"（禁用）
 *
 * 无障碍：contentDescription 区分四种状态，Snackbar 设为 liveRegion=Assertive
 */
@Composable
private fun AddToWrongAnswerButton(
    isInWrongBook: Boolean,
    isLoading: Boolean,
    pointId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when {
        isLoading -> "加入中..."
        isInWrongBook -> "已加入错题本"
        pointId.isBlank() -> "无法加入错题本"
        else -> "加入错题本"
    }
    val icon = when {
        isInWrongBook -> Icons.Default.CheckCircle
        else -> Icons.Default.BookmarkBorder
    }
    val enabled = !isLoading && !isInWrongBook && pointId.isNotBlank()

    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription = when {
                    isInWrongBook -> "当前卡片已加入错题本"
                    isLoading -> "正在加入错题本"
                    pointId.isBlank() -> "无法加入错题本：知识点关联缺失"
                    else -> "加入错题本"
                }
            },
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isInWrongBook) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (isInWrongBook) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.padding(start = Spacing.xs))
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = Spacing.xs),
            )
        }
        Text(label)
    }
}

// 翻转卡片
@Composable
private fun FlipCard(
    card: CardItem,
    isFlipped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 翻转角度动画：v0.8.12 P1-3 对齐 WenyanMotion.DurationMedium(300ms)
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.EmphasizedEasing),
        label = "card_flip",
    )

    // 容器色平滑过渡（v0.8.12 P1-3：与翻转同步 300ms）
    val containerColor by animateColorAsState(
        targetValue = if (isFlipped) {
            // v0.8.12 P2：翻转后用 surfaceContainerHighest 而非 secondaryContainer(绿)，
            // 避免"查看答案"被潜意识误读为"已答对"
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.EmphasizedEasing),
        label = "card_color",
    )

    val showBack by remember { derivedStateOf { shouldShowBack(rotation) } }

    // v0.9.7 M4 修复:翻转时重置滚动位置,避免背面继承正面滚动状态。
    // 原实现用 rememberScrollState() 内联,翻转前后共享同一 scrollState,
    // 用户在正面滚到底部后翻转,背面也滚到底部(但背面内容不同,应从顶部开始)。
    // 现提升 scrollState 到变量,LaunchedEffect 监听 isFlipped 变化时 scrollTo(0)。
    val scrollState = rememberScrollState()
    LaunchedEffect(isFlipped) {
        scrollState.scrollTo(0)
    }

    Card(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12 * density
            }
            .semantics {
                role = Role.Button
                contentDescription = if (isFlipped) "答案面，单击返回问题" else "问题面，单击查看答案"
                stateDescription = if (isFlipped) "已翻转" else "未翻转"
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick,
    ) {
        // v0.8.12 P0-5 修复：verticalScroll 移到内层旋转抵消的 Box 上。
        // 原实现把 verticalScroll 放在外层 Box(受 graphicsLayer rotationY 影响),
        // 背面 180° 翻转后滚动容器坐标空间也被翻转,导致滚动方向与手势相反。
        // 现外层 Box 仅做居中,内层 Box(已用 rotationY=180 抵消翻转)负责滚动,
        // 滚动方向始终与手势一致。
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val template = card.template
            // v0.9.7 N1:简化 null 分支。
            // CardItem.template 在 toUiItem 中总是赋值(template = this),
            // 但类型仍为 CardTemplate?(data class 默认值 null),需保留 null 安全。
            // 兜底显示 card.back/card.front 纯文本(理论上不会触发)。
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        rotationY = if (showBack) 180f else 0f
                    }
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                contentAlignment = Alignment.Center,
            ) {
                if (template != null) {
                    CardContent(card = template, isFlipped = showBack)
                } else {
                    Text(
                        text = if (showBack) card.back else card.front,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(Spacing.xl),
                    )
                }
            }
        }
    }
}

/**
 * FSRS 评分按钮组。
 *
 * v0.8.5 颜色编码：
 * - AGAIN：error 容器（红，警告）
 * - HARD：tertiary 容器（黄/橙，注意）
 * - GOOD：secondary 容器（绿，成功）
 * - EASY：primary（蓝，加成）
 *
 * v0.8.7 新增预期间隔显示（参考 Anki "10m / 4d / 8d"）：
 * - 每个按钮下方显示该评分档位的预期间隔（如"1分钟""6天""12天"）
 * - [previews] 为空时（加载失败/新卡未加载完）降级为纯文字按钮
 * - 让用户在评分前理解每个评分的后果，建立 FSRS 心智模型
 *
 * v0.8.9 P2-3 修复:GOOD/EASY 颜色对齐 Anki 惯例。
 * - 原实现 GOOD=primary(蓝)/EASY=secondary(绿),与 Anki Mobile/AnkiDroid 颠倒
 * - Anki 惯例:GOOD=绿(掌握/通行),EASY=蓝(超预期加成)
 * - 调整后 GOOD=secondaryContainer(绿,主操作),EASY=primary(蓝,加成操作)
 * - isPrimary 标记同步调整:GOOD 作为默认推荐评分用 filled Button,
 *   EASY 用 FilledTonalButton 视觉权重稍弱
 *
 * 颜色语义参考 Anki Mobile / Duolingo 的"红黄绿"配色直觉。
 * 所有按钮 heightIn(min=48dp) 满足 M3 触控目标规范。
 *
 * @param onRate 评分回调
 * @param previews 4 档评分的预期间隔（由 ViewModel 异步加载）
 */
@Composable
private fun RatingButtons(
    onRate: (CardRating) -> Unit,
    previews: Map<Rating, IntervalPreview>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // AGAIN：红色警示（"完全不会"）
        RatingButton(
            label = "不会",
            intervalText = previews[Rating.AGAIN]?.displayText,
            onClick = { onRate(CardRating.AGAIN) },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            isPrimary = false,
            modifier = Modifier.weight(1f),
        )

        // HARD：黄/橙色（"有难度"）
        RatingButton(
            label = "困难",
            intervalText = previews[Rating.HARD]?.displayText,
            onClick = { onRate(CardRating.HARD) },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            isPrimary = false,
            modifier = Modifier.weight(1f),
        )

        // GOOD：绿色（"掌握了"，FSRS 标准间隔，Anki 惯例绿=成功）
        // v0.8.9:从 primary(蓝) 改为 secondaryContainer(绿),与 Anki Mobile 对齐
        RatingButton(
            label = "良好",
            intervalText = previews[Rating.GOOD]?.displayText,
            onClick = { onRate(CardRating.GOOD) },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            isPrimary = true,
            modifier = Modifier.weight(1f),
        )

        // EASY：蓝色（"很简单"，加成间隔，Anki 惯例蓝=超预期）
        // v0.8.9:从 secondaryContainer(绿) 改为 primary(蓝),与 Anki Mobile 对齐
        // v0.8.12 P2-2:改用 primaryContainer 而非 primary,保持 FilledTonalButton 视觉层级
        // 弱于 GOOD 的 Button(filled),避免 EASY 比 GOOD 更醒目颠倒视觉强调
        RatingButton(
            label = "简单",
            intervalText = previews[Rating.EASY]?.displayText,
            onClick = { onRate(CardRating.EASY) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            isPrimary = false,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 单个评分按钮：评分标签 + 预期间隔（v0.8.7 抽取）。
 *
 * - [label]：评分文字（"不会"/"困难"/"良好"/"简单"）
 * - [intervalText]：预期间隔（"1分钟"/"6天"/"12天"），null 时不显示
 * - [isPrimary]：true 用 [Button]（filled），false 用 [FilledTonalButton]
 */
@Composable
private fun RatingButton(
    label: String,
    intervalText: String?,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    val semanticsDesc = if (intervalText != null) {
        "$label：$intervalText 后重看"
    } else {
        label
    }
    // v0.8.17 P0 修复:isPrimary=true 时 Button 也必须传 colors,否则用默认 primary(蓝)。
    // 原实现只为 FilledTonalButton 传 colors,Button 用默认 colors,导致 GOOD 按钮显示
    // 默认 primary(蓝)而非设计的 secondaryContainer(绿),与 EASY(primaryContainer 蓝)
    // 颜色重复,4 档按钮实际显示为红/黄/蓝/蓝,破坏"红黄绿蓝"渐进视觉直觉。
    // 现为 Button 传入 buttonColors(containerColor, contentColor),与 FilledTonalButton
    // 保持一致的容器配色,实现 v0.8.9 P2-3 注释中"GOOD=secondaryContainer(绿)"的设计意图。
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = semanticsDesc },
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            RatingButtonContent(label = label, intervalText = intervalText)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = semanticsDesc },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            RatingButtonContent(label = label, intervalText = intervalText)
        }
    }
}

/**
 * 评分按钮内容：标签 + 预期间隔（v0.8.7 抽取）。
 *
 * 标签用 labelLarge，间隔用 labelSmall（视觉层次）。
 * 间隔为 null 时只显示标签（降级模式）。
 */
@Composable
private fun RatingButtonContent(label: String, intervalText: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (intervalText != null) {
            Text(
                text = intervalText,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * 判断卡片翻转动画当前应显示正面还是背面。
 *
 * 90° 是"卡侧消失"的临界点：
 * - rotation ≤ 90°：正面朝向用户，显示正面内容
 * - rotation > 90°：背面朝向用户，显示背面内容
 *
 * 提取为纯函数便于测试。注意 [androidx.compose.animation.core.animateFloatAsState]
 * 会在每帧更新 rotation，本函数在每帧被调用以决定内容切换时机。
 */
internal fun shouldShowBack(rotation: Float): Boolean = rotation > 90f

// ---------- v0.9.7 M10: @Preview ----------
//
// CardsScreen 顶层使用 hiltViewModel() 注入,无法直接 @Preview(Preview 不支持 Hilt)。
// 现对 Crossfade 内三种关键状态分支的私有组件添加 @Preview,
// 便于 IDE 中实时查看 UI 效果 + 回归视觉检查(颜色/间距/无障碍文案):
// 1. Normal Review — 正常复习态(已翻转,展示四档评分按钮 + 撤销/跳过)
// 2. Empty — 今日无到期卡空状态
// 3. Finished — 会话完成统计态(已复习/需重练/掌握率 + 撤销/再复习/返回)
//
// 注:Preview 用静态数据,不触发 FSRS 调度/错题记录等副作用,onXxx 回调均为空 lambda。

/**
 * Preview 用的测试 [CardItem](ClozeQuoteCard 模板)。
 *
 * 构造一张"苏轼名句填空"卡,内容真实可读,便于在 Preview 中验证
 * CardContent 渲染(正面挖空 / 背面答案) + FlipCard 翻转 + 评分按钮布局。
 */
private fun previewCardItem(): CardItem = CardItem(
    id = "preview_card_1",
    front = "苏轼____，号东坡居士",
    back = "轼",
    cardType = "CLOZE_QUOTE",
    pointId = "preview_point_1",
    template = ClozeQuoteCard(
        front = "苏轼____，号东坡居士",
        back = "轼",
        pointId = "preview_point_1",
        quote = "苏轼____，号东坡居士",
        blank = "轼",
        hint = "北宋文学家，唐宋八大家之一",
    ),
)

/**
 * Preview 用的测试 [CardsUiState]。
 *
 * @param isFlipped 是否已翻转(true=展示背面+评分按钮,false=展示正面问题)
 */
private fun previewUiState(isFlipped: Boolean = true): CardsUiState = CardsUiState(
    cards = listOf(previewCardItem()),
    currentIndex = 0,
    isFlipped = isFlipped,
)

/**
 * 正常复习态 Preview(已翻转)。
 *
 * 展示:进度条(1/1) + 翻转卡(背面答案) + 四档评分按钮 + 撤销/跳过按钮。
 * previews=emptyMap() 降级为无预期间隔的纯文字按钮(模拟新卡预览未加载完)。
 */
@Preview(name = "Cards - Normal Review (Light)", showBackground = true)
@Composable
private fun CardsNormalReviewPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            CardReviewContent(
                card = previewCardItem(),
                uiState = previewUiState(isFlipped = true),
                previews = emptyMap(),
                isSiblingAlreadyRated = false,
                onFlip = {},
                onRate = {},
                onUndo = {},
                onSkip = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 今日无到期卡空状态 Preview。
 *
 * 展示:CheckCircle 图标 + "今天没有到期卡片" + 描述文案。
 * 对应 CardsScreen Crossfade 中 `!key.hasCards` 分支。
 */
@Preview(name = "Cards - Empty (Light)", showBackground = true)
@Composable
private fun CardsEmptyPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            EmptyState(
                icon = Icons.Default.CheckCircle,
                title = "今天没有到期卡片",
                description = "已全部复习完毕，可去知识点列表预习新内容",
            )
        }
    }
}

/**
 * 会话完成态 Preview。
 *
 * 展示:AutoAwesome 图标 + "本次复习完成" + 用时 + 统计卡(已复习12/需重练3/掌握率75%)
 * + 鼓励文案 + 再复习一轮/撤销最后一张/返回知识点列表 三个按钮。
 * 对应 CardsScreen Crossfade 中 `key.isFinished` 分支。
 */
@Preview(name = "Cards - Finished (Light)", showBackground = true)
@Composable
private fun CardsFinishedPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            SessionCompleteState(
                reviewedCount = 12,
                againCount = 3,
                sessionDurationMinutes = 8,
                onRetry = {},
                onUndo = {},
                onExit = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
