package com.wenyan.app.feature.cards

import androidx.compose.ui.res.stringResource
import com.wenyan.app.feature.cards.R

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Fullscreen

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
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.data.cards.ClozeQuoteCard
import com.wenyan.app.core.data.repository.IntervalPreview
import com.wenyan.app.core.designsystem.component.AdaptiveWindowLayout
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.component.WenyanRatingButton
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import kotlinx.coroutines.withTimeout
import kotlin.math.PI
import kotlin.math.sin
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme
import com.wenyan.app.core.fsrs.Rating

/**
 * Snackbar 显示超时兜底（v0.9.23 新增）。
 *
 * 用于 [SnackbarHostState.showSnackbar] 的 withTimeout 保护：
 * material3 1.5.0-alpha18 的 duration 计时若异常导致 showSnackbar 挂起不返回，
 * 5 秒后协程超时取消、Snackbar 被 dismiss，保证提示最多显示 5 秒不常驻。
 * 正常流程下 SnackbarDuration.Short（约 4 秒）先于超时结束。
 */
private const val SNACKBAR_TIMEOUT_MS = 5_000L

/** 翻转稍慢于普通状态切换，给正反面交接留出可感知的空间运动。 */
private const val CARD_FLIP_DURATION_MS = 420

/** 正反面操作区使用短促的 fade-through，避免两个不同高度面板同时占位造成跳动。 */
private const val CARD_ACTIONS_DURATION_MS = 240

/**
 * 横屏双栏卡片最大宽度（v0.9.35 协调优化）。
 *
 * 实测 800dp 内容区下左栏宽 584dp（比例 1.73:1，行文过长似横幅）；
 * 限宽 480dp 后比例 ~1.42:1，阅读舒适、更像"卡片"。
 * 竖屏列宽 < 480dp 时 widthIn 不生效，保持 fillMaxWidth 原行为。
 */
private val CARD_MAX_WIDTH_LANDSCAPE = 480.dp

/**
 * 全屏横屏卡片最大宽度（v0.9.36 全屏模式）。
 *
 * 全屏释放顶栏/横幅后高度更大，卡片可放宽到 560dp 仍保持协调比例
 * （900dp 内容区 → 左栏 ~604dp 限 560 → 比例 ~1.45:1）；
 * 比普通双栏 480dp 更突出，配合右侧单列竖排操作栏形成沉浸式阅读体验。
 */
private val CARD_MAX_WIDTH_FULLSCREEN = 560.dp

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
 * 4. 回看按钮：currentIndex > 0 时可见，回看上一张（不回滚 FSRS）
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
    // v0.9.36 全屏模式：顶栏全屏按钮入口（仅当前有卡可复习时显示）
    onNavigateToFullscreen: () -> Unit = {},
    onDailyTaskFinished: () -> Unit = {},
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
    // v0.9.29: 今日任务（新卡/复习/距考试天数/进度）
    val todayPlan by viewModel.todayPlan.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onDailyTaskFinished()
    }

    // errorMessage 非 null 时弹 Snackbar，展示后立即 clearError 避免重组重复弹
    // v0.8.14 P0-8 修复:Leech 警告(AlertDialog)显示时不弹 Snackbar,避免两者同时
    // 弹出造成用户注意力分散 + AlertDialog 导航走后 Snackbar 未消费被销毁。
    // 修复策略:leechWarning 非空时暂存 errorMessage 不弹,leechWarning 清除后
    // errorMessage 仍存在(未 clearError),LaunchedEffect 重新触发弹 Snackbar。
    //
    // v0.9.23 修复:先 clear 再 show + withTimeout 超时兜底。
    // 原实现 clearXxx 在 showSnackbar 之后,若 material3 1.5.0-alpha18 的 duration
    // 计时异常导致 showSnackbar 挂起不返回,状态永远清不掉、Snackbar 永远显示。
    // 修复后:1) 先 clear 立即清状态不残留; 2) withTimeout(5s) 兜底,即使挂起
    // 5 秒后协程取消、Snackbar 被 dismiss,最多显示 5 秒。
    LaunchedEffect(errorMessage, leechWarning) {
        val error = errorMessage
        if (error != null && leechWarning == null) {
            viewModel.clearError()
            withTimeout(SNACKBAR_TIMEOUT_MS) {
                snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short)
            }
        }
    }

    // v0.9.18: 成功消息（加入错题本）弹 Snackbar
    // v0.9.23 修复:同上（先 clear 再 show + withTimeout 兜底）,见 errorMessage 注释。
    LaunchedEffect(successMessage) {
        val msg = successMessage
        if (msg != null) {
            viewModel.clearSuccessMessage()
            withTimeout(SNACKBAR_TIMEOUT_MS) {
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            }
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
        topBar = {
            WenyanLargeTopAppBar(
                title = stringResource(R.string.card_title),
                actions = {
                    IconButton(onClick = onNavigateToAiAssistant) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = stringResource(R.string.card_ai_assistant),
                        )
                    }
                    // v0.9.36 全屏模式：仅当前有卡可复习时显示全屏入口
                    //（加载/错误/空/完成态隐藏，避免在无卡场景进入无意义的全屏）
                    if (uiState.currentCard != null) {
                        IconButton(onClick = onNavigateToFullscreen) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.card_fullscreen),
                            )
                        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
        // v0.9.34 横屏：外层感知内容区尺寸——横屏时解除 widthIn 限制，
        // 让 CardReviewContent 双栏布局用满宽度；TodayPlanBanner 限宽居中防拉伸。
        // v0.9.35 审计修复（H1）：双断点不一致——MEDIUM 窗口（600-840dp）渲染
        // 左侧 80dp 折叠 rail 后内容区仅 520-599dp，shouldUseDualPane 内容区
        // ≥600dp 判据永不激活。补窗口宽度类：窗口非 COMPACT（MEDIUM/EXPANDED）
        // 且内容区宽 > 高即双栏，覆盖 600-679dp 横屏手机。
        AdaptiveWindowLayout(modifier = Modifier.fillMaxSize()) { layout ->
            val windowWidthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
            val useDualPane = layout.maxWidth > layout.maxHeight &&
                (windowWidthClass != WindowWidthSizeClass.COMPACT || layout.maxWidth >= 600.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (useDualPane) {
                            Modifier
                        } else {
                            Modifier.widthIn(max = MaxContentWidth.compact)
                        },
                    )
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // v0.9.29: 今日任务横幅（加载/错误时隐藏，避免数据未就绪闪烁）
                // v0.9.34 横屏：compact 单行版释放垂直空间给复习卡片
                if (!stateKey.isLoading && stateKey.error == null) {
                    TodayPlanBanner(
                        plan = todayPlan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (useDualPane) {
                                    // 横屏横幅限宽居中（comfortable），避免全宽拉伸
                                    Modifier.widthIn(max = MaxContentWidth.comfortable)
                                } else {
                                    Modifier
                                },
                            )
                            .padding(bottom = Spacing.md),
                        compact = useDualPane,
                    )
                }
                Crossfade(
                    targetState = stateKey,
                    animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                    label = "cards_state",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                            // v0.9.35 审计修复（M3）：横屏外层不限宽时错误态全宽拉伸，
                            // 限宽 comfortable 居中（竖屏 <720dp 不生效零回归）
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
                            // 会话完成态：展示本次复习统计 + 会话时长 + 鼓励继续 / 返回
                            // v0.8.17 P1:sessionDurationMinutes 改为 collect StateFlow,
                            // 避免在 Composable 函数体中直接调用 viewModel.getSessionDurationMinutes()
                            // 破坏重组稳定性(每次重组返回不同值,SessionCompleteState 无谓重组)
                            // 完成后仍可回看最后一张卡（仅回到 CardReviewContent，不回滚评分）
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
                            // v0.9.29：今日任务横幅已在上方展示（新卡/复习/距考试/进度）
                            // v0.9.35 审计修复（M3）：横屏空态限宽居中
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
        } // Box end
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
internal fun CardReviewContent(
    card: CardItem,
    uiState: CardsUiState,
    previews: Map<Rating, IntervalPreview>,
    isSiblingAlreadyRated: Boolean,
    isInWrongBook: Boolean,
    isAddingBookmark: Boolean,
    useDualPane: Boolean = false,
    // v0.9.36 全屏模式：横屏全屏布局变体——卡片更大(560dp) + 右操作栏 280dp 单列竖排
    fullscreenLandscape: Boolean = false,
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
    //
    // v0.9.34 横屏:useDualPane=true 时走"左卡片 / 右操作面板"双栏布局——
    // 卡片占全部高度 + 大部分宽度（突出、大、方便阅读），评分按钮收敛到
    // 右侧 200dp 窄列（2×2 网格，按钮更小）。竖屏 useDualPane=false 保持原单栏。
    // v0.9.36 全屏:fullscreenLandscape=true 时横屏左卡片放宽到 560dp、
    // 右操作栏 280dp 单列竖排（用户"一个个竖着排列"偏好）。

    // ── 进度 + 新卡徽章 + 可翻转卡片（竖屏与横屏左栏共用）──
    @Composable
    fun ColumnScope.CardArea(cardMaxWidth: Dp) {
        // 进度区：文字 + 进度条
        // v0.9.35 横屏协调：进度条与卡片同宽限宽居中（widthIn 在 fillMaxWidth
        // 前，避免 fillMaxWidth 强制全宽后 widthIn 约束冲突；左栏 Column
        // horizontalAlignment=Center 居中；竖屏列宽 < maxWidth 不受影响）
        ProgressSection(
            currentIndex = uiState.currentIndex,
            total = uiState.cards.size,
            modifier = Modifier
                .widthIn(max = cardMaxWidth)
                .fillMaxWidth(),
        )

        // v0.9.31：新卡标识（未学过的知识点首次进入学习循环，帮助理解学习循环）
        if (card.isNew) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .widthIn(max = cardMaxWidth)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "新卡 · 首次学习（10 分钟后强化一次）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                )
            }
        }

        // 可翻转卡片
        // v0.9.34 横屏：widthIn 防止大平板横屏左栏过宽
        // v0.9.35 协调优化：限宽收窄到 480dp（实测 800dp 内容区卡片达 584dp 宽、
        // 比例 1.73:1 行文过长似横幅；480dp 下比例 ~1.42:1 更舒适协调），
        // 左栏 horizontalAlignment=Center 居中，两侧留白与右栏形成平衡。
        // v0.9.36 全屏横屏：放宽到 560dp（全屏高度更大，比例 ~1.45:1 仍协调）
        // 竖屏列宽 < maxWidth 不受影响（fillMaxWidth 优先）
        FlipCard(
            card = card,
            isFlipped = uiState.isFlipped,
            onClick = onFlip,
            modifier = Modifier
                .widthIn(max = cardMaxWidth)
                .fillMaxWidth()
                // v0.9.35 审计修复（M1）：极端矮屏（内容区 <300dp）卡片 height 保底，
                // 避免 weight 挤压到不可读；正常高度不受影响
                .heightIn(min = 140.dp)
                .weight(1f),
        )
    }

    // ── 翻转后操作组：已调度提示 + 评分 + 加入错题本 + 回看/跳过 ──
    @Composable
    fun ColumnScope.FlippedActions(compact: Boolean, vertical: Boolean = false) {
        // 同一知识点已完成调度时仍保留评分按钮，但隐藏不会生效的预期间隔。
        // 原实现 isSiblingAlreadyRated=true 时完全隐藏 RatingButtons,导致用户
        // 无法评分推进(只能跳过),也无法记录错题(sibling AGAIN 仍会调用
        // wrongAnswerRepository.recordWrongAnswer,但 UI 隐藏按钮后用户无法触发)。
        //
        // 修复策略:
        // - AlreadyScheduledHint 作为信息提示放在评分按钮上方(非替换)
        // - RatingButtons 始终渲染,sibling 时传空 previews(不显示预期间隔)
        //   避免误导用户以为评分会改变调度间隔
        // - 用户可正常评分推进,AGAIN 评分仍记录错题(不影响 FSRS 调度)
        if (isSiblingAlreadyRated) {
            AlreadyScheduledHint(compact = compact || vertical)
        }
        RatingButtons(
            onRate = onRate,
            previews = if (isSiblingAlreadyRated) emptyMap() else previews,
            // v0.9.34 横屏窄面板 2×2 网格（按钮更小、更省高度），竖屏 4 横排
            // v0.9.36 全屏横屏：单列竖排（用户"一个个竖着排列"）
            columns = when {
                vertical -> 1
                compact -> 2
                else -> 4
            },
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
        // v0.9.36 全屏横屏：vertical 时单列竖排（每按钮全栏宽）
        if (vertical) {
            UndoButton(
                onUndo = onUndo,
                enabled = uiState.currentIndex > 0,
                modifier = Modifier.fillMaxWidth(),
            )
            SkipButton(onSkip = onSkip, modifier = Modifier.fillMaxWidth())
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // 回看按钮恒占位（索引 0 时透明禁用），避免布局跳动
                UndoButton(
                    onUndo = onUndo,
                    enabled = uiState.currentIndex > 0,
                    modifier = Modifier.weight(1f),
                )
                SkipButton(onSkip = onSkip, modifier = Modifier.weight(1f))
            }
        }
    }

    // ── 翻转前操作组：提示 + 加入错题本 + 撤销/跳过 ──
    @Composable
    fun ColumnScope.UnflippedActions(vertical: Boolean = false) {
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
        // 原实现未翻转只显示跳过，用户想回看上一张必须先翻转，操作迂回
        // v0.9.36 全屏横屏：vertical 时单列竖排（每按钮全栏宽）
        if (vertical) {
            UndoButton(
                onUndo = onUndo,
                enabled = uiState.currentIndex > 0,
                modifier = Modifier.fillMaxWidth(),
            )
            SkipButton(onSkip = onSkip, modifier = Modifier.fillMaxWidth())
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // 回看按钮恒占位（索引 0 时透明禁用），避免布局跳动
                UndoButton(
                    onUndo = onUndo,
                    enabled = uiState.currentIndex > 0,
                    modifier = Modifier.weight(1f),
                )
                SkipButton(onSkip = onSkip, modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    fun ColumnScope.AnimatedCardActions(
        compact: Boolean,
        vertical: Boolean,
    ) {
        AnimatedContent(
            targetState = uiState.isFlipped,
            transitionSpec = {
                val direction = if (targetState) 1 else -1
                (
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = CARD_ACTIONS_DURATION_MS,
                            delayMillis = CARD_ACTIONS_DURATION_MS / 4,
                            easing = WenyanMotion.DecelerateEasing,
                        ),
                    ) + slideInVertically(
                        animationSpec = tween(
                            CARD_ACTIONS_DURATION_MS,
                            easing = WenyanMotion.DecelerateEasing,
                        ),
                        initialOffsetY = { direction * it / 10 },
                    )
                    ).togetherWith(
                    fadeOut(
                        animationSpec = tween(
                            CARD_ACTIONS_DURATION_MS / 2,
                            easing = WenyanMotion.AccelerateEasing,
                        ),
                    ) + slideOutVertically(
                        animationSpec = tween(
                            CARD_ACTIONS_DURATION_MS,
                            easing = WenyanMotion.AccelerateEasing,
                        ),
                        targetOffsetY = { -direction * it / 12 },
                    ),
                )
            },
            label = "card_actions",
            modifier = Modifier.fillMaxWidth(),
        ) { flipped ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (flipped) {
                    FlippedActions(compact = compact, vertical = vertical)
                } else {
                    UnflippedActions(vertical = vertical)
                }
            }
        }
    }

    if (useDualPane) {
        // ── 横屏双栏：左卡片突出 / 右操作面板窄列 ──
        // v0.9.36 全屏：fullscreenLandscape=true 时右操作栏加宽到 280dp、
        // 按钮单列竖排、卡片放宽到 560dp（全屏高度大，比例仍协调）
        val actionPanelWidth = if (fullscreenLandscape) 280.dp else 200.dp
        val cardMaxWidth = if (fullscreenLandscape) CARD_MAX_WIDTH_FULLSCREEN else CARD_MAX_WIDTH_LANDSCAPE
        Column(modifier = modifier) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                // 左栏：进度 + 卡片（占全部高度 + 大部分宽度）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    CardArea(cardMaxWidth = cardMaxWidth)
                }
                // 右栏：操作面板（窄列，按钮更小）
                // v0.9.34 打磨：矮横屏（~360dp 高）翻转后操作组可能超出右栏可用高度，
                // verticalScroll 兜底保证所有操作可访问（评分与撤销/跳过优先可见，
                // 次要"加入错题本"可滚动查看）
                // v0.9.35 协调优化：Arrangement.Center 垂直居中——实测右栏内容
                // 仅占顶部 155~230dp，下方 160~240dp 空白（视觉悬空失衡）；
                // Center 使操作面板与左侧卡片垂直平衡；内容超出时自动可滚
                val actionScrollState = rememberScrollState()
                // 翻转/切卡时重置滚动到顶部：评分按钮始终优先可见
                // （与左栏 FlipCard 的 scrollTo(0) 对称）
                LaunchedEffect(uiState.isFlipped, card.id) {
                    actionScrollState.scrollTo(0)
                }
                Column(
                    modifier = Modifier
                        .width(actionPanelWidth)
                        .fillMaxHeight()
                        .verticalScroll(actionScrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AnimatedCardActions(
                        compact = !fullscreenLandscape,
                        vertical = fullscreenLandscape,
                    )
                }
            }
        }
    } else {
        // ── 竖屏单栏（原布局）──
        // v0.9.36 全屏竖屏：无顶栏/横幅释放空间后放宽上限（compact→comfortable）
        // 让卡片更大；普通竖屏保持 compact
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(
                        max = if (fullscreenLandscape) {
                            MaxContentWidth.comfortable
                        } else {
                            MaxContentWidth.compact
                        },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                CardArea(
                    cardMaxWidth = if (fullscreenLandscape) {
                        CARD_MAX_WIDTH_FULLSCREEN
                    } else {
                        CARD_MAX_WIDTH_LANDSCAPE
                    },
                )

                // 正反面操作区共用同一布局槽位，避免旧实现两个 AnimatedVisibility
                // 在交接期同时占高导致卡片与按钮上下跳动。
                AnimatedCardActions(compact = false, vertical = false)
            }
        }
    }
}

/**
 * 当前知识点已完成调度的提示。
 *
 * 适用于同知识点的后续 sibling 卡，也适用于“回看上一张”后的首张卡。
 * 此时评分不会再次触发 FSRS，避免重复调度导致 stability 虚高。
 *
 * v0.8.10 P1-B3 修复:
 * - 原实现用提示 **替换** RatingButtons,导致用户无法评分推进
 * - 现将提示放在 RatingButtons **上方**
 * - 评分按钮始终渲染(不显示预期间隔),用户可正常评分推进
 * - AGAIN 评分仍记录错题(不影响 FSRS 调度,但保留错题本更新)
 */
@Composable
private fun AlreadyScheduledHint(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    // v0.8.12 P0-7:文案去术语化,图标从 CheckCircle 改为 Info
    // v0.8.13 P1-1:文案补全"评分仍会记入错题本和会话统计",
    // 解决用户"既然不改变计划,为什么还要评分"的困惑。
    // 实际:sibling 卡评分不触发 FSRS 调度(避免 stability 虚高),
    // 但 AGAIN 仍记录错题,所有评分仍累加 sessionReviewedCount(用于完成态统计)。
    //
    // v0.8.14 P2-7 修复:原文案"仍会记入错题本和会话统计"对所有评分都显示,
    // 但只有 AGAIN 评分才记入错题本,GOOD/HARD/EASY 不记。用户可能误以为评 GOOD
    // 也会记错题,造成困惑。现明确区分:AGAIN 记错题,所有评分计入会话统计。
    //
    // v0.9.34 横屏:compact=true 时图标上置 + 文字居中（窄操作面板 200dp 内
    // 避免 icon 横排挤压文字）。
    val hintText = "这个知识点本轮已安排复习，本卡评分不会再次改变间隔；" +
        "评“不会”仍会加入错题本"
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = hintText
            },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (compact) {
            Column(
                modifier = Modifier.padding(Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
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
 *
 * v0.9.36：从 private 提升为 internal，供全屏沉浸页 [CardsFullscreenScreen] 复用。
 */
@Composable
internal fun SessionCompleteState(
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
            // v0.9.34 横屏：外层不限宽后完成态需自限宽（竖屏宽度 < 720dp 不生效）
            .widthIn(max = MaxContentWidth.comfortable)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // v0.9.37 P0-3：仅统计信息区合并为单一语义节点（TalkBack 一次朗读
        // fullDescription），下方 3 个操作按钮各自独立可聚焦/可触发。
        // 原实现 mergeDescendants 作用在整个 Column，把"再复习/撤销/返回"
        // 3 个按钮并入单一节点——读屏用户无法分别操作，核心完成流程受损。
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                        label = stringResource(R.string.card_reviewed),
                        value = reviewedCount.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = stringResource(R.string.card_need_retry),
                        value = againCount.toString(),
                        color = if (againCount > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = stringResource(R.string.card_mastery),
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
            Text(stringResource(R.string.text_06))
        }
        // 完成态允许回看最后一张；数据库评分与复习计划保持不变。
        // 适用场景：完成后想重新查看最后一张的题面与答案。
        // 回看后 currentIndex 回退、isFinished 变 false，Crossfade 切回复习态；
        // 已落库的评分、错题与复习计划均不改变。仅在 reviewedCount > 0 时显示。
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
                Text(stringResource(R.string.text_07))
            }
        }
        // v0.8.7：退出按钮,让用户明确离开复习(导航到知识点列表)
        // v0.8.13 P1-2:文案从"返回学习"改为"返回知识点列表"
        // 原文案"返回学习"语义模糊(cards tab 本身就是学习),实际行为是去知识点列表浏览
        TextButton(
            onClick = onExit,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.text_08))
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
 * 回看按钮：仅 UI 回退，不回滚 FSRS 调度。
 *
 * 历史方法名为 undo；界面明确称为“回看”，避免用户误以为数据库评分已撤销。
 * 触控目标 ≥48dp，左对齐 Undo 图标 + 文字。
 *
 * v0.8.8：新增 [modifier] 参数，支持在 Row 中 weight 分配宽度。
 */
@Composable
private fun UndoButton(
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // v0.9.30 i18n：semantics lambda 非 Composable，先在函数体取资源
    val undoContentDesc = stringResource(R.string.card_undo)
    TextButton(
        onClick = onUndo,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 48.dp)
            // v0.9.30 打磨：不可用时透明但恒占位（避免 1↔2 按钮切换布局跳动）
            .alpha(if (enabled) 1f else 0f)
            .semantics { contentDescription = undoContentDesc },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = null,
            modifier = Modifier.padding(end = Spacing.xs),
        )
        Text(stringResource(R.string.text_09))
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
    // v0.9.30 i18n：semantics lambda 非 Composable，先在函数体取资源
    val skipContentDesc = stringResource(R.string.card_skip)
    TextButton(
        onClick = onSkip,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = skipContentDesc },
    ) {
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = null,
            modifier = Modifier.padding(end = Spacing.xs),
        )
        Text(stringResource(R.string.text_10))
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
    // 420ms 空间运动 + 中点轻微退远，比原 300ms 匀速观感更柔和、正反面交接更清楚。
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(CARD_FLIP_DURATION_MS, easing = WenyanMotion.EmphasizedEasing),
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
        animationSpec = tween(CARD_FLIP_DURATION_MS, easing = WenyanMotion.EmphasizedEasing),
        label = "card_color",
    )

    val showBack by remember { derivedStateOf { shouldShowBack(rotation) } }

    // v0.9.7 M4 修复:翻转时重置滚动位置,避免背面继承正面滚动状态。
    // 原实现用 rememberScrollState() 内联,翻转前后共享同一 scrollState,
    // 用户在正面滚到底部后翻转,背面也滚到底部(但背面内容不同,应从顶部开始)。
    // 现提升 scrollState 到变量,LaunchedEffect 监听 isFlipped 变化时 scrollTo(0)。
    // v0.9.25 修复:加入 card.id——点"跳过"切新卡时 isFlipped 不变(仍 false),
    // 原实现不会重置滚动,新卡会从上一张滚过的中段开始显示。
    val scrollState = rememberScrollState()
    LaunchedEffect(isFlipped, card.id) {
        scrollState.scrollTo(0)
    }

    Card(
        modifier = modifier
            // v0.9.30 打磨：正反面内容高度差异平滑过渡（此前翻转时高度突变）
            .animateContentSize(
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.EmphasizedEasing),
            )
            .graphicsLayer {
                rotationY = rotation
                val scaleAtAngle = flipScale(rotation)
                scaleX = scaleAtAngle
                scaleY = scaleAtAngle
                cameraDistance = 18 * density
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
 * v0.9.31 统一为设计系统公共组件 [WenyanRatingButton]，
 * 与错题本（WrongAnswerScreen）共用同一实现，消除三处重复。
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
    columns: Int = 4,
) {
    // v0.9.34 横屏：columns=2 时 2×2 网格（每个按钮更窄、总高更矮），
    // 竖屏默认 columns=4 保持一行横排不变。
    @Composable
    fun SpecButton(
        label: String,
        rating: CardRating,
        intervalKey: Rating,
        container: Color,
        content: Color,
        primary: Boolean,
        modifier: Modifier,
    ) {
        RatingButton(
            label = label,
            intervalText = previews[intervalKey]?.displayText,
            onClick = { onRate(rating) },
            containerColor = container,
            contentColor = content,
            isPrimary = primary,
            modifier = modifier,
        )
    }

    if (columns >= 4) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // AGAIN：红色警示（"完全不会"）
            SpecButton(
                label = stringResource(R.string.card_rating_again),
                rating = CardRating.AGAIN,
                intervalKey = Rating.AGAIN,
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
                primary = false,
                modifier = Modifier.weight(1f),
            )
            // HARD：黄/橙色（"有难度"）
            SpecButton(
                label = stringResource(R.string.card_rating_hard),
                rating = CardRating.HARD,
                intervalKey = Rating.HARD,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
                primary = false,
                modifier = Modifier.weight(1f),
            )
            // GOOD：绿色（"掌握了"，FSRS 标准间隔，Anki 惯例绿=成功）
            // v0.8.9:从 primary(蓝) 改为 secondaryContainer(绿),与 Anki Mobile 对齐
            SpecButton(
                label = stringResource(R.string.card_rating_good),
                rating = CardRating.GOOD,
                intervalKey = Rating.GOOD,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                primary = true,
                modifier = Modifier.weight(1f),
            )
            // EASY：蓝色（"很简单"，加成间隔，Anki 惯例蓝=超预期）
            // v0.8.9:从 secondaryContainer(绿) 改为 primary(蓝),与 Anki Mobile 对齐
            // v0.8.12 P2-2:改用 primaryContainer 而非 primary,保持 FilledTonalButton 视觉层级
            // 弱于 GOOD 的 Button(filled),避免 EASY 比 GOOD 更醒目颠倒视觉强调
            SpecButton(
                label = stringResource(R.string.card_rating_easy),
                rating = CardRating.EASY,
                intervalKey = Rating.EASY,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                primary = false,
                modifier = Modifier.weight(1f),
            )
        }
    } else if (columns == 2) {
        // 2×2 网格：横屏窄操作面板内按钮更小（~90dp）、总高 ~120dp（远小于 4 横排 ~184dp）
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SpecButton(
                    label = stringResource(R.string.card_rating_again),
                    rating = CardRating.AGAIN,
                    intervalKey = Rating.AGAIN,
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                    primary = false,
                    modifier = Modifier.weight(1f),
                )
                SpecButton(
                    label = stringResource(R.string.card_rating_hard),
                    rating = CardRating.HARD,
                    intervalKey = Rating.HARD,
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                    primary = false,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SpecButton(
                    label = stringResource(R.string.card_rating_good),
                    rating = CardRating.GOOD,
                    intervalKey = Rating.GOOD,
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    primary = true,
                    modifier = Modifier.weight(1f),
                )
                SpecButton(
                    label = stringResource(R.string.card_rating_easy),
                    rating = CardRating.EASY,
                    intervalKey = Rating.EASY,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    primary = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        // v0.9.36 全屏横屏单列：4 评分按钮竖排全栏宽（用户"一个个竖着排列"偏好）
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SpecButton(
                label = stringResource(R.string.card_rating_again),
                rating = CardRating.AGAIN,
                intervalKey = Rating.AGAIN,
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
                primary = false,
                modifier = Modifier.fillMaxWidth(),
            )
            SpecButton(
                label = stringResource(R.string.card_rating_hard),
                rating = CardRating.HARD,
                intervalKey = Rating.HARD,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
                primary = false,
                modifier = Modifier.fillMaxWidth(),
            )
            SpecButton(
                label = stringResource(R.string.card_rating_good),
                rating = CardRating.GOOD,
                intervalKey = Rating.GOOD,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SpecButton(
                label = stringResource(R.string.card_rating_easy),
                rating = CardRating.EASY,
                intervalKey = Rating.EASY,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                primary = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 单个评分按钮：评分标签 + 预期间隔（v0.8.7 抽取；v0.9.31 委托公共组件）。
 *
 * v0.9.31 起实现统一为设计系统公共组件 [WenyanRatingButton]，
 * 与错题本/论述题自评共用同一实现，消除三处重复。
 * 本适配器仅补充 Cards 特有的无障碍描述（"评分后 X 后重看"）。
 *
 * - [label]：评分文字（"不会"/"困难"/"良好"/"简单"）
 * - [intervalText]：预期间隔（"1分钟"/"6天"/"12天"），null 时不显示
 * - [isPrimary]：true 用 filled [Button]，false 用 [FilledTonalButton]
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
    WenyanRatingButton(
        label = label,
        intervalText = intervalText,
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor,
        isPrimary = isPrimary,
        contentDescription = semanticsDesc,
        modifier = modifier,
    )
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

/** 翻到侧面时轻微缩小，起止位置保持 1f，避免生硬的平面原地旋转。 */
internal fun flipScale(rotation: Float): Float {
    val normalized = rotation.coerceIn(0f, 180f) / 180f
    return 1f - 0.025f * sin(normalized * PI).toFloat()
}

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
internal fun previewCardItem(): CardItem = CardItem(
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
internal fun previewUiState(isFlipped: Boolean = true): CardsUiState = CardsUiState(
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
                isInWrongBook = false,
                isAddingBookmark = false,
                onFlip = {},
                onRate = {},
                onUndo = {},
                onSkip = {},
                onAddToWrongAnswerBook = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 横屏双栏 Preview（v0.9.34 新增，翻转前）。
 *
 * widthDp=800 / heightDp=400 模拟横屏手机内容区：左卡片大区域 + 右操作面板窄列。
 * useDualPane=true 走双栏布局（卡片突出大、按钮收敛到右侧窄列）。
 */
@Preview(name = "Cards - Landscape (Front)", widthDp = 800, heightDp = 400, showBackground = true)
@Composable
private fun CardsLandscapeFrontPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            CardReviewContent(
                card = previewCardItem(),
                uiState = previewUiState(isFlipped = false),
                previews = emptyMap(),
                isSiblingAlreadyRated = false,
                isInWrongBook = false,
                isAddingBookmark = false,
                useDualPane = true,
                onFlip = {},
                onRate = {},
                onUndo = {},
                onSkip = {},
                onAddToWrongAnswerBook = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 横屏双栏 Preview（v0.9.34 新增，翻转后）。
 *
 * 展示 2×2 评分按钮网格（横屏"按钮更小"）+ 加入错题本 + 撤销/跳过，全部收敛在右侧 200dp 窄列。
 */
@Preview(name = "Cards - Landscape (Back)", widthDp = 800, heightDp = 400, showBackground = true)
@Composable
private fun CardsLandscapeBackPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            CardReviewContent(
                card = previewCardItem(),
                uiState = previewUiState(isFlipped = true),
                previews = emptyMap(),
                isSiblingAlreadyRated = false,
                isInWrongBook = false,
                isAddingBookmark = false,
                useDualPane = true,
                onFlip = {},
                onRate = {},
                onUndo = {},
                onSkip = {},
                onAddToWrongAnswerBook = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 今日任务横幅（v0.9.29 卡片备考系统）。
 *
 * 展示：
 * - 距考试天数（未设置考试日期显示提示）
 * - 今日：新卡 X 张 · 复习 Y 张（估算，每知识点约 6 张卡）
 * - 学习进度条（已学知识点 / 总 VERIFIED 知识点）
 *
 * v0.9.34 横屏：[compact]=true 时降级为单行核心信息（今日任务 + 距考试），
 * 隐藏进度条/上限行，高度 ~110dp → ~44dp，把垂直空间让给复习卡片。
 */
@Composable
private fun TodayPlanBanner(
    plan: TodayPlanUi,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val examLabel = plan.daysUntilExam?.let { "距考试 $it 天" } ?: "未设置考试日期"
    val progressPercent = (plan.progress * 100).toInt()

    if (compact) {
        // ── 横屏紧凑版：单行核心任务信息 ──
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildString {
                        append("今日：")
                        if (plan.newPointCount > 0) append("新学 ${plan.newPointCount} 个知识点")
                        if (plan.newPointCount > 0 && plan.duePointCount > 0) append(" · ")
                        if (plan.duePointCount > 0) append("复习 ${plan.duePointCount} 个知识点")
                        if (plan.newPointCount == 0 && plan.duePointCount == 0) append("暂无学习任务")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = examLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        return
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = examLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "已学 $progressPercent%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.padding(top = Spacing.xs))
            // v0.9.30 修复：改按知识点显示（FSRS 按知识点调度，sibling 6 张一起排期；
            // 此前"复习约 X 张"= 点数×6，用户过 1 张卡减少 6 张，与实际推进粒度不符）
            Text(
                text = buildString {
                    append("今日：")
                    if (plan.newPointCount > 0) append("新学 ${plan.newPointCount} 个知识点")
                    if (plan.newPointCount > 0 && plan.duePointCount > 0) append(" · ")
                    if (plan.duePointCount > 0) append("复习 ${plan.duePointCount} 个知识点")
                    if (plan.newPointCount == 0 && plan.duePointCount == 0) append("暂无学习任务")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.padding(top = Spacing.xs))
            // 每日新卡限额提示（新卡 = 未学过的知识点；默认 60 张 ≈ 10 个知识点）
            Text(
                text = "每日新卡上限 ${plan.newCardLimit} 张（约 ${plan.newCardLimit / CARDS_PER_POINT_UI} 个知识点）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.padding(top = Spacing.sm))
            LinearProgressIndicator(
                progress = { plan.progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 今日任务横幅用的每知识点卡片数估算（与数据层 CARDS_PER_POINT_ESTIMATE 一致）。 */
private const val CARDS_PER_POINT_UI = 6

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
 * + 鼓励文案 + 再复习一轮/回看最后一张/返回知识点列表 三个按钮。
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
