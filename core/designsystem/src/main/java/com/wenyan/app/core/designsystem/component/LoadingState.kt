package com.wenyan.app.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 文研加载指示器（M3 Expressive LoadingIndicator）。
 *
 * v0.6：用 M3 Expressive 的 [LoadingIndicator] 替代标准 CircularProgressIndicator。
 * Expressive LoadingIndicator 有更丰富的动效（多弧线 + 变速），是 M3 Expressive
 * 的标志性组件之一。
 *
 * 封装在此以集中管理 @OptIn(ExperimentalMaterial3ExpressiveApi)，各 feature 模块
 * 直接使用本组件无需重复 opt-in。
 *
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WenyanLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    LoadingIndicator(modifier = modifier)
}
