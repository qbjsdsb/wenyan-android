package com.wenyan.app.feature.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    viewModel: CardsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("记忆卡片") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val card = uiState.currentCard
            if (card == null) {
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
                return@Column
            }

            // 进度提示
            Text(
                text = "${uiState.currentIndex + 1} / ${uiState.cards.size}",
                style = MaterialTheme.typography.labelLarge,
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

            // 评分按钮（仅翻转后显示）
            if (uiState.isFlipped) {
                RatingButtons(onRate = viewModel::rateCard)
            } else {
                Text(
                    text = "点击卡片查看答案",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    // 翻转角度动画
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        label = "card_flip",
    )

    Card(
        modifier = modifier
            .graphicsLayer { rotationY = rotation },
        colors = CardDefaults.cardColors(
            containerColor = if (isFlipped) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // 简化处理：根据翻转状态显示正/反面
            Text(
                text = if (isFlipped) card.back else card.front,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

// FSRS 评分按钮组
@Composable
private fun RatingButtons(onRate: (CardRating) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
