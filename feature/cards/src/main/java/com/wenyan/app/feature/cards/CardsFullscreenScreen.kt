package com.wenyan.app.feature.cards

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.wenyan.app.core.designsystem.component.AdaptiveWindowLayout
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.ImmersiveSystemBars
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import kotlinx.coroutines.withTimeout

/**
 * 全屏沉浸式复习页（v0.9.36 全屏模式）。
 *
 * 从卡片页顶栏全屏按钮进入，共享 [CardsViewModel]（通过导航层
 * `hiltViewModel(navController.getBackStackEntry(ROUTE_CARDS))` 传入），
 * 因此与卡片页是**同一个复习会话**：当前卡片、翻转状态、会话统计实时同步。
 *
 * 全屏要点：
 * - [ImmersiveSystemBars] 隐藏状态栏 + 导航栏（滑动边缘可临时唤出），
 *   离开组合（返回卡片页）自动恢复系统栏
 * - [ExpressiveScaffold] 去掉顶栏、`contentWindowInsets = WindowInsets(0,0,0,0)`，
 *   让内容占满全屏（系统栏 insets 已归零）
 * - 左上角半透明圆形浮动按钮退出全屏（全屏无返回箭头，需明确出口）
 * - 横屏走双栏变体：卡片放宽到 560dp + 右操作栏 280dp 单列竖排评分按钮
 *   （[CardReviewContent] `fullscreenLandscape=true`）
 * - 竖屏走单栏变体：放宽最大宽度上限（comfortable）让卡片更大
 *
 * 状态机与卡片页一致（Crossfade 五态：加载/错误/完成/空/正常复习），
 * Snackbar 与 Leech 警告对话框逻辑镜像 [CardsScreen]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsFullscreenScreen(
    onBack: () -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToKnowledge: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: CardsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val sessionReviewed by viewModel.sessionReviewedCount.collectAsStateWithLifecycle()
    val sessionAgain by viewModel.sessionAgainCount.collectAsStateWithLifecycle()
    // 评分预期间隔预览 + Leech 警告（与卡片页一致）
    val currentPreviews by viewModel.currentPreviews.collectAsStateWithLifecycle()
    val leechWarning by viewModel.leechWarning.collectAsStateWithLifecycle()
    // sibling 卡已评分状态：隐藏误导性预期间隔
    val isSiblingAlreadyRated by viewModel.isSiblingAlreadyRated.collectAsStateWithLifecycle()
    // 会话时长
    val sessionDurationMinutes by viewModel.sessionDurationMinutes.collectAsStateWithLifecycle()
    // 手动加入错题本状态
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val isAddingBookmark by viewModel.isAddingBookmark.collectAsStateWithLifecycle()
    val manualAddedPointIds by viewModel.manualAddedPointIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // v0.9.36 全屏：进入即隐藏系统栏，离开组合自动恢复（DisposableEffect onDispose）
    // 注意：仅全屏页启用，卡片页正常保留系统栏
    ImmersiveSystemBars(enabled = true)

    // errorMessage 弹 Snackbar（先 clear 再 show + withTimeout 兜底，见 CardsScreen 注释）
    LaunchedEffect(errorMessage, leechWarning) {
        val error = errorMessage
        if (error != null && leechWarning == null) {
            viewModel.clearError()
            withTimeout(SNACKBAR_TIMEOUT_MS) {
                snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short)
            }
        }
    }
    // successMessage（加入错题本）弹 Snackbar
    LaunchedEffect(successMessage) {
        val msg = successMessage
        if (msg != null) {
            viewModel.clearSuccessMessage()
            withTimeout(SNACKBAR_TIMEOUT_MS) {
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            }
        }
    }

    // Leech 警告 AlertDialog（与卡片页一致：问 AI / 查看知识点 / 知道了）
    // 全屏页共享 ViewModel，leechWarning 状态同步，需自行处理对话框
    leechWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = viewModel::clearLeechWarning,
            title = { Text(stringResource(R.string.text_01)) },
            text = { Text(warning.message) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TextButton(onClick = {
                        viewModel.clearLeechWarning()
                        onNavigateToAiAssistant()
                    }) {
                        Text(stringResource(R.string.text_02))
                    }
                    TextButton(onClick = {
                        val pid = warning.pointId
                        viewModel.clearLeechWarning()
                        if (pid.isNotBlank()) onNavigateToDetail(pid)
                    }) {
                        Text(stringResource(R.string.text_03))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearLeechWarning) {
                    Text(stringResource(R.string.text_04))
                }
            },
        )
    }

    ExpressiveScaffold(
        // 全屏模式：无顶栏 + 零系统栏 insets（ImmersiveSystemBars 已隐藏系统栏）
        topBar = {},
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 五态状态键（与 CardsScreen 优先级一致：isLoading > error > isFinished > hasCards）
            val stateKey = FullscreenStateKey(
                isLoading = uiState.isLoading,
                error = uiState.error,
                isFinished = uiState.isFinished,
                hasCards = uiState.currentCard != null,
            )
            // 内容区顶部预留浮动退出按钮空间（全屏无顶栏，按钮悬浮左上角）
            AdaptiveWindowLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = FULLSCREEN_EXIT_TOP_PADDING),
            ) { layout ->
                // 双栏判定与卡片页一致：横屏 + 内容区 ≥600dp（或窗口非 COMPACT）
                val windowWidthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
                val useDualPane = layout.maxWidth > layout.maxHeight &&
                    (windowWidthClass != WindowWidthSizeClass.COMPACT || layout.maxWidth >= 600.dp)
                Crossfade(
                    targetState = stateKey,
                    animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                    label = "cards_fullscreen_state",
                    modifier = Modifier.fillMaxSize(),
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
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = MaxContentWidth.comfortable)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ErrorState(
                                        icon = Icons.Default.CloudOff,
                                        title = stringResource(R.string.card_load_failed),
                                        message = key.error,
                                        onRetry = viewModel::retry,
                                    )
                                }
                            }
                        }
                        key.isFinished -> {
                            // 会话完成态：全屏下"返回知识点列表"直接切知识 Tab
                            //（浮动 X 按钮仍可退出全屏回到卡片页完成态）
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
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = MaxContentWidth.comfortable)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    EmptyState(
                                        icon = Icons.Default.CheckCircle,
                                        title = stringResource(R.string.card_empty_today),
                                        description = "今日新卡与复习任务见上方，可去知识点列表预习新内容",
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
                                                Text(stringResource(R.string.text_05))
                                            }
                                        },
                                    )
                                }
                            }
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
                                    useDualPane = useDualPane,
                                    // 全屏模式：横屏卡片 560dp + 右栏 280dp 单列竖排；
                                    // 竖屏放宽最大宽度（comfortable）
                                    fullscreenLandscape = true,
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
            // 浮动退出按钮：左上角半透明圆形，始终可见（全屏无系统栏返回，需明确出口）
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.md),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shadowElevation = 4.dp,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.card_fullscreen_exit),
                    )
                }
            }
        }
    }
}

/**
 * 全屏页 Crossfade 状态键（与 CardsScreen 的 CardsStateKey 对称，独立定义避免跨文件耦合）。
 *
 * 优先级：isLoading > error > isFinished > hasCards
 */
private data class FullscreenStateKey(
    val isLoading: Boolean,
    val error: String?,
    val isFinished: Boolean,
    val hasCards: Boolean,
)

/** Snackbar 显示超时兜底（与 CardsScreen 的 SNACKBAR_TIMEOUT_MS 对称）。 */
private const val SNACKBAR_TIMEOUT_MS = 5_000L

/** 浮动退出按钮占位高度（IconButton 48dp + Spacing.md 12dp 外边距）。 */
private val FULLSCREEN_EXIT_TOP_PADDING = 60.dp
