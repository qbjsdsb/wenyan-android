package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * 文研自适应导航容器（v0.6 大屏适配）。
 *
 * 根据 [WindowWidthSizeClass] 自动选择导航形态：
 *
 * | 宽度等级 | 导航形态 | 布局 |
 * |----------|----------|------|
 * | COMPACT（< 600dp） | 底部 [WenyanNavigationBar] | 单栏 |
 * | MEDIUM（600-840dp） | 左侧 [WenyanWideNavigationRail]（折叠） | 单栏 |
 * | EXPANDED（≥ 840dp） | 左侧 [WenyanWideNavigationRail]（展开） | 单栏 |
 *
 * @param items 导航项列表
 * @param currentRoute 当前路由（用于高亮选中项）
 * @param onNavigate 点击导航回调
 * @param showNavigation 是否显示导航栏（子路由时隐藏）
 * @param modifier 修饰符
 * @param content 主内容区域
 */
@Composable
fun WenyanAdaptiveNavigation(
    items: List<WenyanNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    showNavigation: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    when (windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            // 手机：沉浸式底部导航栏（v0.11 沉浸式改造）
            // 使用 Box 叠加布局：内容全屏延伸至底部，NavigationBar 透明叠加在内容之上。
            // 底部渐变遮罩让内容在导航栏区域平滑过渡到背景色，避免被截断感。
            Box(modifier = modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val topInset = WindowInsets.statusBars.only(WindowInsetsSides.Top).getTop(density)
                val bottomInset = WindowInsets.ime.getBottom(density)
                ExpressiveScaffold(
                    modifier = Modifier.fillMaxSize(),
                    // 只保留状态栏顶部 + IME 底部间距，不消费底部导航栏 insets，
                    // 让内容延伸到导航栏后面，实现沉浸效果。
                    // 使用 WindowInsets(top, bottom) 构造避免 + 操作符（部分 Compose 版本不可用）。
                    contentWindowInsets = WindowInsets(top = topInset, bottom = bottomInset),
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        content(padding)
                        // 底部渐变遮罩（带对齐，作用在父 Box 内）
                        if (showNavigation) {
                            Box(Modifier.align(Alignment.BottomCenter)) {
                                BottomGradientScrim()
                            }
                        }
                    }
                }
                // 底部导航栏叠加层：透明背景，浮在内容之上
                if (showNavigation) {
                    WenyanNavigationBar(
                        items = items,
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }

        WindowWidthSizeClass.MEDIUM -> {
            // 小平板/折叠屏：左侧 WideNavigationRail 折叠态（仅图标）
            AdaptiveRailScaffold(
                showNavigation = showNavigation,
                expanded = false,
                items = items,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                modifier = modifier,
                content = content,
            )
        }

        else -> {
            // 大平板/桌面：左侧 WideNavigationRail 展开态（图标+标签）
            AdaptiveRailScaffold(
                showNavigation = showNavigation,
                expanded = true,
                items = items,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                modifier = modifier,
                content = content,
            )
        }
    }
}

/**
 * 侧边栏 + 内容区的 Row 布局（Medium/Expanded 共用）。
 *
 * WideNavigationRail 在左，ExpressiveScaffold 在右（weight=1f 填充剩余空间）。
 */
@Composable
private fun AdaptiveRailScaffold(
    showNavigation: Boolean,
    expanded: Boolean,
    items: List<WenyanNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Row(modifier = modifier.fillMaxSize()) {
        if (showNavigation) {
            WenyanWideNavigationRail(
                expanded = expanded,
                items = items,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
            )
        }
        ExpressiveScaffold(modifier = Modifier.weight(1f)) { padding ->
            content(padding)
        }
    }
}

/**
 * 底部渐变遮罩层（沉浸式导航栏专用）。
 *
 * 从透明渐变到 [MaterialTheme.colorScheme.surfaceContainer]，
 * 让内容在导航栏区域平滑过渡，避免被导航栏截断的突兀感。
 * 高度 120dp 覆盖导航栏 + 手势条区域。
 */
@Composable
private fun BottomGradientScrim() {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        surfaceContainer.copy(alpha = 0.85f),
                        surfaceContainer,
                    ),
                ),
            ),
    )
}
