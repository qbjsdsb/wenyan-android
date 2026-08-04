package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * 统一评分按钮（v0.9.31 设计系统公共组件）。
 *
 * 整合此前三处重复实现，消除颜色编码不一致：
 * - feature/cards `RatingButton`：动作评分 + 预期间隔 + 四档颜色
 * - feature/quiz `WrongAnswerRatingButton`：动作评分 + 四档颜色（无预期间隔）
 * - feature/knowledge `SelfRatingButton`：自评选择 + 图标 + 选中态
 *
 * ## 两种模式（由 [isSelected] 区分）
 *
 * **动作模式**（[isSelected] = null，Cards/WrongAnswer 使用）：
 * - [isPrimary] = true → [Button]（filled 主操作，如 GOOD 默认推荐）
 * - [isPrimary] = false → [FilledTonalButton]（次级操作）
 * - [containerColor]/[contentColor] 控制四档颜色编码：
 *   AGAIN=error 容器（红）、HARD=tertiary 容器（黄/橙）、
 *   GOOD=secondary 容器（绿）、EASY=primary 容器（蓝）
 *
 * **选择模式**（[isSelected] 非 null，Essay 自评使用）：
 * - [isSelected] = true → [FilledTonalButton]（选中强调，叠加评分色）
 * - [isSelected] = false → [OutlinedButton]（未选中中性态）
 * - 同样支持 [containerColor]/[contentColor]，选中后显示对应评分色
 *
 * ## 内容区
 *
 * - [icon] 非空时图标位于标签左侧（选择模式常用）
 * - [intervalText] 非空时标签下方显示预期间隔小字（动作模式预期间隔）
 * - 两者可同时为空，退化为纯文字按钮
 *
 * 所有模式统一 `heightIn(min = 48.dp)`，满足 M3 触控目标规范。
 *
 * @param label 评分文字（如"不会"/"困难"/"良好"/"简单"）
 * @param onClick 评分回调
 * @param modifier 外部修饰符（如 Row 内 weight(1f)）
 * @param intervalText 预期间隔文字（如"1分钟"/"6天"/"12天"），null 不显示
 * @param icon 可选图标（选择模式用情感图标）
 * @param containerColor 容器色；null 时动作模式回退 secondaryContainer
 * @param contentColor 内容色；null 时动作模式回退 onSecondaryContainer
 * @param isPrimary 动作模式：true 用 filled [Button]，false 用 [FilledTonalButton]
 * @param isSelected 选择模式开关：null=动作模式，true=选中，false=未选中
 * @param contentDescription 无障碍描述覆盖；null 时用按钮默认语义
 */
@Composable
fun WenyanRatingButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    intervalText: String? = null,
    icon: ImageVector? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    isPrimary: Boolean = false,
    isSelected: Boolean? = null,
    contentDescription: String? = null,
) {
    val minHeightModifier = modifier.heightIn(min = 48.dp)
    val finalModifier = if (contentDescription != null) {
        minHeightModifier.semantics { this.contentDescription = contentDescription }
    } else {
        minHeightModifier
    }

    // 按钮内容：可选图标 + 标签（+ 预期间隔小字）
    val content: @Composable () -> Unit = {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        }
        if (intervalText != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(intervalText, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }

    when {
        // ── 选择模式 ──────────────────────────────────────────────
        isSelected != null -> {
            if (isSelected) {
                FilledTonalButton(
                    onClick = onClick,
                    modifier = finalModifier,
                    colors = if (containerColor != null && contentColor != null) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = containerColor,
                            contentColor = contentColor,
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                ) {
                    content()
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    modifier = finalModifier,
                ) {
                    content()
                }
            }
        }

        // ── 动作模式：主操作（filled） ─────────────────────────────
        isPrimary -> {
            Button(
                onClick = onClick,
                modifier = finalModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor ?: MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = contentColor ?: MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                content()
            }
        }

        // ── 动作模式：次级操作（tonal） ────────────────────────────
        else -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = finalModifier,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = containerColor ?: MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = contentColor ?: MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                content()
            }
        }
    }
}
