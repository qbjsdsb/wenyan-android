package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            //   2. 导航栏：Surface 悬浮容器（圆角 24dp + 半透明玻璃质感 + 水平间距 8dp）
            //
            // v0.9.19 紧凑玻璃风格：
            //   - 移除 BottomGradientScrim：导航栏自身半透明，不再需要渐变遮罩过渡
            //   - 底部 padding 从 80dp 降至 56dp+4dp，减少遮挡面积 ~30%
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

                if (showNavigation) {
                    // 有导航栏：沉浸式布局
                    //
                    // 布局结构（Box 叠加）：
                    //   1. 内容区：surfaceContainer 背景，底部显式 padding 避开导航栏
                    //   2. 导航栏：半透明玻璃质感，浮在最上层
                    //
                    // 底部间距 = 导航栏高度(56dp) + 底部留边(4dp) + 系统导航栏手势区
                    // v0.9.19 紧凑玻璃风格：导航栏高度从 80dp 降至 56dp，底部留边 4dp
                    val bottomPadding = 56.dp + 4.dp + systemNavBarBottomDp

                    // 1. 内容区：surfaceContainer 背景 + 显式 padding
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(top = topInsetDp, bottom = bottomPadding),
                    ) {
                        content(PaddingValues(0.dp))
                    }

                    // 2. 底部导航栏（透明背景，浮在最上层）
                    // v0.9.19 移除 BottomGradientScrim：紧凑玻璃风格导航栏自身半透明，不再需要渐变遮罩过渡
                    WenyanNavigationBar(
                        items = items,
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                } else {
                    // 无导航栏（子路由）：内容全屏，不添加额外底部色块。
                    //
                    // 不设置 background 也不添加 bottomPadding，
                    // 子页面自己的 ExpressiveScaffold 会通过 contentWindowInsets
                    // 处理系统栏 insets(状态栏/导航栏/IME)。
                    // 外层不需要额外 padding，避免与 Scaffold 的 inset 消费重复。
                    Box(modifier = Modifier.fillMaxSize()) {
                        content(PaddingValues(0.dp))
                    }
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
