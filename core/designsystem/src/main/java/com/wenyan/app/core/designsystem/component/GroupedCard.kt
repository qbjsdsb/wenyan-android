package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

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
 * - [title]：标题（必填）
 * - [subtitle]：右侧简短值（如"v0.1.0"），与 [trailing] 互斥
 * - [description]：标题下方说明文字（如"深色模式下使用纯黑背景"），可多行
 * - [leadingIcon]：左侧图标，可选
 * - [leadingIconContentDescription]：左侧图标内容描述，为 null 时图标为装饰性（不读屏），
 *   避免 title 被读屏重复；仅当图标有额外语义时才显式设置
 * - [trailing]：右侧自定义内容（如 Switch），优先级高于 [subtitle]
 *
 * @param title 标题
 * @param subtitle 右侧简短值，可选
 * @param description 标题下方说明文字，可选
 * @param leadingIcon 左侧图标，可选
 * @param leadingIconContentDescription 左侧图标内容描述，为 null 时图标为装饰性（不读屏）
 * @param onClick 点击回调，为 null 时不可点击
 * @param trailing 右侧自定义内容，优先级高于 subtitle
 */
@Composable
fun GroupedCardItem(
    title: String,
    subtitle: String? = null,
    description: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // NF-UA4 修复：加 role=Role.Button 语义，TalkBack 朗读"按钮"，
            // 视障用户才能识别该项可点击。原 .clickable 无 role，TalkBack 不朗读"按钮"。
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = leadingIconContentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = Spacing.md),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

/**
 * 分组卡片内项之间的分割线。
 *
 * KSU 设置页标准做法：在两个 [GroupedCardItem] 之间手动插入此分割线。
 * 使用 [MaterialTheme.colorScheme.outlineVariant] 色，0.5dp 厚度，
 * 左右各留 [Spacing.lg] 边距，与 item 内容区对齐。
 */
@Composable
fun GroupedCardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
