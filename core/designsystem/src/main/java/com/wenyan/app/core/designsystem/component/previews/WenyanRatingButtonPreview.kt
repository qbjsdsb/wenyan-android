package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanRatingButton
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

/**
 * 动作模式 Preview：四档 FSRS 评分（红/黄/绿/蓝 + 预期间隔）。
 */
@Preview(name = "Rating - Action 4-level", showBackground = true)
@Composable
private fun WenyanRatingButtonActionPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    WenyanRatingButton(
                        label = "不会",
                        intervalText = "1分钟",
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    WenyanRatingButton(
                        label = "困难",
                        intervalText = "6天",
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    WenyanRatingButton(
                        label = "良好",
                        intervalText = "12天",
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                    )
                    WenyanRatingButton(
                        label = "简单",
                        intervalText = "24天",
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                }

                // 无预期间隔降级（错题本场景）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    WenyanRatingButton(
                        label = "不会",
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    WenyanRatingButton(
                        label = "困难",
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    WenyanRatingButton(
                        label = "良好",
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                    )
                    WenyanRatingButton(
                        label = "简单",
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 选择模式 Preview：论述题自评三档（图标 + 选中态评分色）。
 */
@Preview(name = "Rating - Selection 3-level", showBackground = true)
@Composable
private fun WenyanRatingButtonSelectionPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                // 未选中态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    WenyanRatingButton(
                        label = "不会",
                        icon = Icons.Default.SentimentDissatisfied,
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        isSelected = false,
                        modifier = Modifier.weight(1f),
                    )
                    WenyanRatingButton(
                        label = "尚可",
                        icon = Icons.Default.SentimentNeutral,
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        isSelected = false,
                        modifier = Modifier.weight(1f),
                    )
                    WenyanRatingButton(
                        label = "轻松",
                        icon = Icons.Default.SentimentVerySatisfied,
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        isSelected = false,
                        modifier = Modifier.weight(1f),
                    )
                }

                // 选中态（"尚可"被选中 → 绿色 FilledTonal）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    WenyanRatingButton(
                        label = "不会",
                        icon = Icons.Default.SentimentDissatisfied,
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        isSelected = false,
                        modifier = Modifier.weight(1f),
                    )
                    WenyanRatingButton(
                        label = "尚可",
                        icon = Icons.Default.SentimentNeutral,
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        isSelected = true,
                        modifier = Modifier.weight(1f),
                    )
                    WenyanRatingButton(
                        label = "轻松",
                        icon = Icons.Default.SentimentVerySatisfied,
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        isSelected = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
