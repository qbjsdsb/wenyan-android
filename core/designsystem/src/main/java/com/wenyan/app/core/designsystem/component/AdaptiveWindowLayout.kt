package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 横屏双栏判定纯函数（v0.9.34 新增，可单测）。
 *
 * 判据：`maxWidth > maxHeight && maxWidth >= 600.dp`。
 *
 * - 精确捕获"横屏手机"（宽 > 高）：横屏手机宽 ~640-900dp、高仅 ~400dp，
 *   此时内容区高度紧张，需要双栏布局给卡片让出垂直空间。
 * - **不会误触发平板竖屏**（高 > 宽仍走旧布局）：平板竖屏高度充足，
 *   单栏上下排布本就合理，无需双栏。
 * - `maxWidth >= 600.dp` 对齐 Material3 MEDIUM 断点：避免过窄窗口
 *   （如横屏小折叠屏 500dp 宽）双栏后每栏过窄不可用。
 *
 * @param maxWidth 内容区最大可用宽度（BoxWithConstraints 的 maxWidth）
 * @param maxHeight 内容区最大可用高度
 */
fun shouldUseDualPane(maxWidth: Dp, maxHeight: Dp): Boolean =
    maxWidth > maxHeight && maxWidth >= 600.dp

/**
 * 窗口布局感知容器（v0.9.34 新增，横屏适配统一入口）。
 *
 * 内部用 [BoxWithConstraints] 暴露内容区实际可用尺寸，通过 [isLandscape]
 * 判定是否需要双栏布局。所有 Screen 的横屏特化都应优先经本组件，
 * 避免各自用 `LocalConfiguration.orientation`（@Preview 恒竖屏、测试不可注入）
 * 或重复写断点判断。
 *
 * 与导航/顶栏的 `currentWindowAdaptiveInfo`（WindowWidthSizeClass）互补：
 * - 本组件：基于**内容区**约束（Scaffold innerPadding 之后），精确且可测试
 * - 导航/顶栏：基于**窗口**宽度类，保持现状不动（已测试稳定）
 *
 * 用法：
 * ```kotlin
 * AdaptiveWindowLayout { layout ->
 *     if (layout.isLandscape) {
 *         // 横屏双栏布局
 *     } else {
 *         // 竖屏单栏布局
 *     }
 * }
 * ```
 *
 * @param modifier 外部修饰符（默认 fillMaxSize 由调用方约束）
 * @param content 接收 [AdaptiveWindowLayoutInfo] 信息的 content lambda
 */
@Composable
fun AdaptiveWindowLayout(
    modifier: Modifier = Modifier,
    content: @Composable (AdaptiveWindowLayoutInfo) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        content(
            AdaptiveWindowLayoutInfo(
                isLandscape = shouldUseDualPane(maxWidth = maxWidth, maxHeight = maxHeight),
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            ),
        )
    }
}

/**
 * 窗口布局信息（v0.9.34）。
 *
 * @param isLandscape 是否需要双栏布局（[shouldUseDualPane] 判定）
 * @param maxWidth 内容区最大可用宽度
 * @param maxHeight 内容区最大可用高度
 */
data class AdaptiveWindowLayoutInfo(
    val isLandscape: Boolean,
    val maxWidth: Dp,
    val maxHeight: Dp,
)
