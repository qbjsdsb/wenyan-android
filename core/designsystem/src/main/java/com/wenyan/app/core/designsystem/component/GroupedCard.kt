package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 文研分组卡片。
 *
 * 参照 KernelSU 设置页的分组风格：带标题的容器，内部垂直排列多个列表项，
 * 项之间用 padding 区分（不画分割线，保持 M3 Expressive 的"色调分层"理念）。
 * 容器色用 [TonalCard] 的 surfaceBright 突出层级。
 *
 * 用法：
 * ```
 * GroupedCard(title = "复习设置") {
 *     GroupedCardItem(title = "每日新卡数", subtitle = "20")
 *     GroupedCardItem(title = "最大复习数", subtitle = "200")
 * }
 * ```
 *
 * @param title 分组标题
 * @param modifier 修饰符
 * @param titleTrailing 标题右侧的辅助内容（如"全部展开"按钮），可选
 * @param content 分组内容，通常是一组 [GroupedCardItem]
 */
@Composable
fun GroupedCard(
    title: String,
    modifier: Modifier = Modifier,
    titleTrailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    bottom = Spacing.sm,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (titleTrailing != null) {
                titleTrailing()
            }
        }
        TonalCard {
            Column {
                content()
            }
        }
    }
}

/**
 * 分组卡片内的列表项。
 *
 * 简单的标题 + 可选副标题行，左侧标题、右侧副标题或自定义 trailing，点击有回调。
 *
 * @param title 标题
 * @param subtitle 副标题，可选（如设置值）
 * @param onClick 点击回调，为 null 时不可点击
 * @param trailing 右侧自定义内容（如 Switch），优先级高于 subtitle
 */
@Composable
fun GroupedCardItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        } else if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
