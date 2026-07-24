package com.wenyan.app.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

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
 * v0.8.4 修复（P1）：添加无障碍语义。
 * 原实现无 semantics，屏幕阅读器无法识别这是"加载中"状态。
 * 现添加 contentDescription + LiveRegionMode.Polite，
 * TalkBack 朗读"加载中"并在加载完成时自动通知。
 *
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WenyanLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    LoadingIndicator(
        modifier = modifier.semantics {
            contentDescription = "加载中"
            // Polite 模式：不打断用户当前操作，等待合适时机再朗读
            liveRegion = LiveRegionMode.Polite
        },
    )
}
