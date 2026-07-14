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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    ExpressiveScaffold(
        topBar = {
            // 卡片翻转界面内容固定不滚动，仅享受 Large 标题样式（不传 scrollBehavior）
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
            )
        },
    ) { innerPadding ->
        Crossfade(
            targetState = uiState.isLoading to (uiState.currentCard == null),
            animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
            label = "cards_state",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Spacing.lg),
        ) { (isLoading, isEmpty) ->
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                isEmpty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "今日复习已完成，暂无待复习卡片",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // 阶段5：优先使用 CardContent 结构化渲染（6种模板专属样式）
            // template 为 null 时降级为 front/back 纯文本（向后兼容）
            //
            // 关键：用 showBack（derivedStateOf）而非 isFlipped 决定显示正/反面，
            // 确保内容切换与动画同步在 rotation>90° 那一帧发生（卡侧消失瞬间），
            // 用户视觉上感知不到内容切换，也不会看到镜像文字。
            val template = card.template
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

// FSRS 评分按钮组
@Composable
private fun RatingButtons(onRate: (CardRating) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        FilledTonalButton(
            onClick = { onRate(CardRating.AGAIN) },
            modifier = Modifier.weight(1f),
        ) { Text("不会") }

        OutlinedButton(
            onClick = { onRate(CardRating.HARD) },
            modifier = Modifier.weight(1f),
        ) { Text("困难") }

        OutlinedButton(
            onClick = { onRate(CardRating.GOOD) },
            modifier = Modifier.weight(1f),
        ) { Text("良好") }

        Button(
            onClick = { onRate(CardRating.EASY) },
            modifier = Modifier.weight(1f),
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
 * 修复镜像 bug 的核心：原实现用 [if (isFlipped)] 硬切内容，与 rotation 动画时序错位，
 * 导致用户在 rotation 0→90° 区间看到背面内容（被正向投影渲染），
 * 在 rotation 90°→180° 区间看到镜像文字（被 3D 投影左右镜像）。
 * 用本函数后，内容切换发生在 rotation>90° 那一帧，背面内容立即以正向方向渲染（虽然外层
 * graphicsLayer 仍在 rotation，但背面内容本身是正向的，所以用户看到的是正常的背面）。
 */
internal fun shouldShowBack(rotation: Float): Boolean = rotation > 90f
