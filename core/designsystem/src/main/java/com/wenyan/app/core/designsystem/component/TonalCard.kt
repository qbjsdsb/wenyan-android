package com.wenyan.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * M3 Expressive 色调卡片。
 *
 * 容器色使用 [MaterialTheme.colorScheme.surfaceBright]，
 * 形状使用 [MaterialTheme.shapes.large]（16dp），
 * 无阴影（用色调分层代替阴影表达层级）。
 */
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
    ) {
        content()
    }
}

/**
 * 低层级色调卡片。
 *
 * 容器色使用 [MaterialTheme.colorScheme.surfaceContainerLow]，
 * 用于次要信息的分组容器。
 */
@Composable
fun TonalCardLow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        content()
    }
}
