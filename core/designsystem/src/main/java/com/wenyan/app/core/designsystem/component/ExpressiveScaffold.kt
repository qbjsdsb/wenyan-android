package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * M3 Expressive 版 Scaffold。
 *
 * 默认容器色使用 [androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer]，
 * 以色调表面代替纯色背景，实现 M3 Expressive 的层级表达。
 *
 * @param contentWindowInsets 内容区域的窗口 insets 消费策略。
 *   默认使用 [ScaffoldDefaults.contentWindowInsets]（顶栏 + 水平 + IME），
 *   传入空值可实现内容延伸至屏幕边缘的沉浸式效果。
 */
@Composable
fun ExpressiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}
