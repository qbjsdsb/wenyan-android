package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 文研层级列表项。
 *
 * 参照 KernelSU 模块管理页：通过左侧缩进 + 圆点节点表达父子层级。
 * 适用于知识点详情页的"前置知识 / 关联知识 / 后置知识"树形结构。
 *
 * - depth=0：根节点，8dp 大圆点 + bodyLarge 字号 + onSurface 文本色
 * - depth≥1：子节点，6dp 小圆点 + bodyMedium 字号 + onSurfaceVariant 文本色
 * - 左侧缩进 = Spacing.lg + depth * 24dp
 *
 * @param title 标题
 * @param depth 层级深度（0 = 根，1 = 一级子，2 = 二级子...）
 * @param onClick 点击回调，为 null 时不可点击且不显示右侧箭头
 * @param trailing 右侧自定义内容（如状态标签），优先级高于默认箭头
 * @param leadingColor 左侧圆点颜色，默认为 primary
 */
@Composable
fun HierarchicalListItem(
    title: String,
    depth: Int = 0,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    leadingColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(
                start = Spacing.lg + (depth * 24).dp,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 层级圆点（根节点为大圆点，子节点为小圆点）
        Icon(
            imageVector = Icons.Filled.Circle,
            contentDescription = null,
            tint = leadingColor,
            modifier = Modifier.size(if (depth == 0) 8.dp else 6.dp),
        )
        Text(
            text = title,
            style = if (depth == 0) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (depth == 0) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.md),
        )
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
