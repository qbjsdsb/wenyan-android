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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
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
            // 手机：沉浸式底部导航栏（v0.9.13 沉浸式改造，v0.9.14 修复底栏遮盖）
            //
            // 布局结构（Box 叠加）：
            //   1. 内容区：surfaceContainer 背景，底部显式 padding 避开导航栏
            //   2. 渐变遮罩：底部 120dp 渐变，让内容平滑过渡
            //   3. 导航栏：透明背景，浮在最上层
            //
            // 关键修复：不再依赖 ExpressiveScaffold 的 contentWindowInsets 消费策略，
            // 直接用 Modifier.padding 为内容添加底部间距，确保可点击区域不被导航栏遮挡。
            Box(modifier = modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val topInsetDp = with(density) {
                    WindowInsets.statusBars.only(WindowInsetsSides.Top).getTop(density).toDp()
                }
                val systemNavBarBottomDp = with(density) {
                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).getBottom(density).toDp()
                }
                // 底部间距 = 导航栏高度(80dp) + 系统导航栏手势区
                // 注意：IME 键盘不由底部间距处理（Scaffold 默认行为），
                // 顶部内容区由 verticalScroll + imePadding 处理。
                val bottomPadding = 80.dp + systemNavBarBottomDp

                // 1. 内容区：surfaceContainer 背景 + 显式 padding
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(top = topInsetDp, bottom = bottomPadding),
                ) {
                    content(PaddingValues(0.dp))
                }

                // 2. 底部渐变遮罩（覆盖在内容之上，导航栏之下）
                if (showNavigation) {
                    Box(Modifier.align(Alignment.BottomCenter)) {
                        BottomGradientScrim()
                    }
                }

                // 3. 底部导航栏（透明背景，浮在最上层）
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
