package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            // 手机：底部 NavigationBar（现有行为）
            ExpressiveScaffold(
                modifier = modifier.fillMaxSize(),
                bottomBar = {
                    if (showNavigation) {
                        WenyanNavigationBar(
                            items = items,
                            currentRoute = currentRoute,
                            onNavigate = onNavigate,
                        )
                    }
                },
            ) { padding ->
                content(padding)
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
