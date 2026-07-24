package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 统一空状态组件。
 *
 * v0.8.4 修复（P3）：Column 添加 semantics(mergeDescendants = true)，
 * 屏幕阅读器一次性朗读完整状态，而非逐个聚焦 Icon/Title/Description。
 *
 * @param icon 空状态图标
 * @param title 标题
 * @param description 描述（可选）
 * @param modifier 修饰符
 * @param action 底部操作按钮（可选）
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: @Composable () -> Unit = {},
) {
    val fullDescription = if (description != null) "$title，$description" else title
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl)
            // v0.8.4：合并子节点语义，TalkBack 一次性朗读完整空状态
            .semantics(mergeDescendants = true) {
                contentDescription = fullDescription
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        action()
    }
}

/**
 * 统一错误状态组件（P0-6 新增）。
 *
 * 用于数据加载失败时展示错误信息 + 重试按钮。
 * 使用 error 配色与 [EmptyState] 视觉区分，让用户一眼识别这是异常状态。
 *
 * v0.8.4 修复（P1）：ErrorState 错误图标 contentDescription 为 null，
 * 屏幕阅读器无法识别这是"错误状态"。现合并语义为单一节点，
 * 朗读完整错误信息（"加载失败，<message>"）。
 *
 * @param icon 错误状态图标
 * @param title 标题
 * @param onRetry 重试回调
 * @param modifier 修饰符
 * @param message 错误详情（可选，展示异常消息帮助排查）
 */
@Composable
fun ErrorState(
    icon: ImageVector,
    title: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val fullDescription = if (message != null) "$title，$message" else title
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl)
            // v0.8.4：合并子节点语义，TalkBack 一次性朗读完整错误状态
            .semantics(mergeDescendants = true) {
                contentDescription = fullDescription
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        FilledTonalButton(onClick = onRetry) {
            Text("重试")
        }
    }
}
