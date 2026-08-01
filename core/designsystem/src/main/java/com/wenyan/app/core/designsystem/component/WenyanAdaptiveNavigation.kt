package com.wenyan.app.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
            //   2. BottomGradientScrim：40dp 渐变遮罩，实现内容到导航栏的平滑过渡
            //   3. 导航栏：全宽流体玻璃风格，贴底
            //
            // v0.9.20 流体玻璃改造：
            //   - 导航栏全宽，仅顶部圆角，底部贴底
            //   - 高度 72dp（5 项 Tab 舒适间距）
            //   - 恢复 BottomGradientScrim（40dp），内容到导航栏平滑过渡
            //   - 底部 padding = 72dp + 系统导航栏手势区
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
                    // 有导航栏：沉浸式布局 + KSU 风格滚动感知显隐
                    //
                    // 布局结构（Box 叠加）：
                    //   1. 内容区：surfaceContainer 背景，底部显式 padding 避开导航栏
                    //   2. 底部容器（scroll-aware 动画组）：渐变遮罩 + 导航栏
                    //      - 下滑内容 → 整体向下移出屏幕（spring 动画）
                    //      - 上滑内容 → 整体回到原位（spring 动画）
                    //
                    // 底部间距 = 导航栏高度(72dp) + 系统导航栏手势区
                    // v0.9.20 流体玻璃：导航栏全宽贴底，高度 72dp，无需底部留边
                    val bottomPadding = 72.dp + systemNavBarBottomDp

                    // KSU 风格滚动感知显隐：监听 LocalLazyListState 的滚动方向
                    val scrollState = LocalLazyListState.current
                    var barVisible by remember { mutableStateOf(true) }

                    if (scrollState != null) {
                        // 记录上一次滚动位置，用于检测滚动方向
                        var previousIndex by remember { mutableStateOf(0) }
                        var previousOffset by remember { mutableStateOf(0) }

                        LaunchedEffect(scrollState) {
                            snapshotFlow {
                                scrollState.firstVisibleItemIndex to
                                    scrollState.firstVisibleItemScrollOffset
                            }.collect { (index, offset) ->
                                // 下滑：index 增大，或同一 index 但 offset 增大（+10px 阈值防抖）
                                val scrollingDown = index > previousIndex ||
                                    (index == previousIndex && offset > previousOffset + 10)
                                // 上滑：index 减小，或同一 index 但 offset 减小（-10px 阈值防抖）
                                val scrollingUp = index < previousIndex ||
                                    (index == previousIndex && offset < previousOffset - 10)

                                if (scrollingDown) {
                                    barVisible = false
                                } else if (scrollingUp) {
                                    barVisible = true
                                }

                                previousIndex = index
                                previousOffset = offset
                            }
                        }
                    }

                    // 底部容器的总隐藏距离 = 导航栏高度(72dp)
                    // 渐变遮罩(40dp)跟随导航栏一起移动
                    val bottomHideDistance = 72.dp
                    val bottomOffset by animateDpAsState(
                        targetValue = if (barVisible) 0.dp else bottomHideDistance,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "bottomNavGroupOffset",
                    )

                    // 1. 内容区：surfaceContainer 背景 + 显式 padding
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(top = topInsetDp, bottom = bottomPadding),
                    ) {
                        content(PaddingValues(0.dp))
                    }

                    // 2. 底部容器（scroll-aware 动画组）：渐变遮罩 + 导航栏
                    // 两者整体移动，下滑时一起移出屏幕，上滑时一起回到原位
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset { IntOffset(0, bottomOffset.roundToPx()) },
                    ) {
                        // 2a. 渐变遮罩：内容到底栏的平滑过渡
                        // v0.9.20 恢复 BottomGradientScrim（40dp），导航栏半透明时需要过渡
                        BottomGradientScrim()

                        // 2b. 底部导航栏（全宽流体玻璃，贴底）
                        WenyanNavigationBar(
                            items = items,
                            currentRoute = currentRoute,
                            onNavigate = onNavigate,
                            visible = true, // 父容器已控制整体偏移，导航栏自身不再额外动画
                            modifier = Modifier,
                        )
                    }
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

/**
 * 底部渐变遮罩。
 *
 * 在内容区与半透明导航栏之间提供平滑过渡，避免内容直接截断在导航栏顶部。
 * v0.9.20：40dp 高度，与 72dp 导航栏配合，过渡区域约为导航栏高度的一半。
 *
 * 颜色从 transparent 渐变到 surfaceContainer（与内容区背景一致），
 * 视觉上"内容逐渐沉入底部"的效果。
 */
@Composable
private fun BottomGradientScrim() {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        surfaceContainer.copy(alpha = 0.50f),
                        surfaceContainer.copy(alpha = 0.85f),
                        surfaceContainer,
                    ),
                ),
            ),
    )
}
