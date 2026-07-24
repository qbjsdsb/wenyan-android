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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.motion.WenyanMotion

/**
 * 记忆卡片界面骨架。
 *
 * 实现卡片正反面翻转交互：
 * - 点击卡片翻转（正面问题 / 背面答案）
 * - 翻转后展示 FSRS 四档评分按钮（Again/Hard/Good/Easy）
 *
 * 翻转动画通过 graphicsLayer rotationY 实现。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    onNavigateToAiAssistant: () -> Unit = {},
    viewModel: CardsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // v0.8.4 修复：collect errorMessage，评分/错题记录失败时通过 Snackbar 反馈
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // v0.8.3 修复：接入 scrollBehavior，长答案滚动时 TopAppBar 可折叠
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
        Crossfade(
            targetState = Triple(uiState.isLoading, uiState.error, uiState.currentCard == null),
            animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
            label = "cards_state",
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .padding(Spacing.lg),
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
                    // v0.8.3 优化：用 EmptyState 组件替代裸 Text，与全 App 一致 + 鼓励文案
                    EmptyState(
                        icon = Icons.Default.CheckCircle,
                        title = "今日复习已完成",
                        description = "暂无待复习卡片，可去知识点列表预习新内容",
                    )
                }
                else -> {
                    uiState.currentCard?.let { card ->
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                        ) {
                            // 进度提示（animateContentSize 让数字变化平滑）
                            Text(
                                text = "${uiState.currentIndex + 1} / ${uiState.cards.size}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.animateContentSize(),
                            )

                            // 可翻转卡片
                            FlipCard(
                                card = card,
                                isFlipped = uiState.isFlipped,
                                onClick = viewModel::flipCard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )

                            // 评分按钮（翻转后显示）+ 提示文案（翻转前显示）
                            // 两者用 AnimatedVisibility 替代 if/else 硬切
                            AnimatedVisibility(
                                visible = uiState.isFlipped,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
                            ) {
                                RatingButtons(onRate = viewModel::rateCard)
                            }
                            AnimatedVisibility(
                                visible = !uiState.isFlipped,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Text(
                                    text = "点击卡片查看答案",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
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
    // 翻转角度动画：用 tween 让动画更干净利落（spring 默认有轻微过冲）
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "card_flip",
    )

    // 容器色平滑过渡（避免硬切）
    val containerColor by animateColorAsState(
        targetValue = if (isFlipped) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 300),
        label = "card_color",
    )

    // P2 性能修复：用 derivedStateOf 包裹 shouldShowBack(rotation)，
    // 使 showBack 仅在 rotation 跨过 90° 临界点（布尔值翻转）时触发重组，
    // 而非 400ms 翻转动画的每一帧都重组 CardContent。
    // 同时，containerColor 动画（300ms）导致父组件重组时，
    // showBack 不变 → CardContent 参数不变 → Compose 编译器跳过 CardContent 调用。
    val showBack by remember { derivedStateOf { shouldShowBack(rotation) } }

    Card(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                // 修正 3D 透视失真：cameraDistance 越大透视效果越弱（边缘拉伸越小）
                // 默认值偏小导致 180° 翻转时边缘严重拉伸
                cameraDistance = 12 * density
            }
            // v0.8.4 修复：FlipCard 无障碍语义，TalkBack 用户可感知"可翻转/已翻转"
            .semantics {
                role = Role.Button
                contentDescription = if (isFlipped) "答案面，双击返回问题" else "问题面，双击查看答案"
                stateDescription = if (isFlipped) "已翻转" else "未翻转"
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick,
    ) {
        // v0.7.4 修复：长答案（如论述题范文、名词解释）超出卡片可视区时被截断，
        // 用户无法看到完整答案。给内容容器加 verticalScroll，长内容可在卡片内滚动阅读。
        // 滚动放在外层 Box（未受 graphicsLayer rotationY 影响），
        // 避免背面 180° 旋转后滚动方向反向（ swipe up 反而向下）的体验问题。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            // 阶段5：优先使用 CardContent 结构化渲染（6种模板专属样式）
            // template 为 null 时降级为 front/back 纯文本（向后兼容）
            //
            // 关键：用 showBack（derivedStateOf）而非 isFlipped 决定显示正/反面，
            // 确保内容切换与动画同步在 rotation>90° 那一帧发生（卡侧消失瞬间）。
            //
            // v0.7.3 P0 修复（镜像文字）：父 Card 容器 rotationY=0→180° 翻转，
            // 背面内容（rotation=180° 时）会被父层镜像投影成左右翻转的文字。
            // 原注释声称"背面内容本身是正向的,所以用户看到的是正常的背面"是错误推断——
            // graphicsLayer 的 rotationY 会镜像所有子内容,无论子内容本身是否正向。
            // 修复：给内容层加反向 rotationY=180° 抵消父层镜像（180+180=360=0）。
            // 仅在 showBack=true 时应用反向旋转,正面内容不受影响。
            val template = card.template
            Box(
                modifier = Modifier.graphicsLayer {
                    rotationY = if (showBack) 180f else 0f
                },
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

// FSRS 评分按钮组
@Composable
private fun RatingButtons(onRate: (CardRating) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // v0.8.4 修复：评分按钮默认 40dp < 48dp 触控下限，加 heightIn 保底
        FilledTonalButton(
            onClick = { onRate(CardRating.AGAIN) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) { Text("不会") }

        OutlinedButton(
            onClick = { onRate(CardRating.HARD) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) { Text("困难") }

        OutlinedButton(
            onClick = { onRate(CardRating.GOOD) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) { Text("良好") }

        Button(
            onClick = { onRate(CardRating.EASY) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) { Text("简单") }
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
 *
 * v0.7.3 修正注释：原注释声称"背面内容立即以正向方向渲染（虽然外层 graphicsLayer
 * 仍在 rotation，但背面内容本身是正向的）"——这是错误推断。graphicsLayer 的 rotationY
 * 会镜像所有子内容。真正的镜像修复在 FlipCard 的内容层加了反向 rotationY=180° 抵消。
 * 本函数仅决定何时切换正/反面内容，不负责解除镜像。
 */
internal fun shouldShowBack(rotation: Float): Boolean = rotation > 90f
