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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
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
            // 手机：Material 3 标准底部导航栏（v0.9.20 MD3 回归）
            //
            // 布局结构（Box 叠加）：
            //   1. 内容区：surfaceContainer 背景，底部显式 padding 避开导航栏
            //   2. 导航栏：全宽 80dp，surfaceContainer 底色，secondaryContainer 指示器，贴底
            //
            // v0.9.20 MD3 回归：
            //   - 导航栏全宽，直角（无圆角），底部贴底
            //   - 高度 80dp（MD3 NavigationBar 标准高度）
            //   - containerColor = surfaceContainer，tonalElevation = 3dp
            //   - 选中指示器 = secondaryContainer，选中色 = onSecondaryContainer
            //   - 移除流体玻璃效果和 BottomGradientScrim（MD3 不透明底栏无需过渡）
            //   - 底部 padding = 80dp + 系统导航栏手势区
            //
            // 关键修复：不再依赖 ExpressiveScaffold 的 contentWindowInsets 消费策略，
            // 直接用 Modifier.padding 为内容添加底部间距，确保可点击区域不被导航栏遮挡。
            Box(modifier = modifier.fillMaxSize()) {
                val density = LocalDensity.current
                // 顶部 insets 由内层 ExpressiveScaffold 的 contentWindowInsets 消费，
                // 外层不再加 top padding，避免双倍状态栏空白。
                val systemNavBarBottomDp = with(density) {
                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).getBottom(density).toDp()
                }

                if (showNavigation) {
                    // 有导航栏：MD3 标准布局 + KSU 风格滚动感知显隐
                    //
                    // 布局结构（Box 叠加）：
                    //   1. 内容区：surfaceContainer 背景，底部显式 padding 避开导航栏
                    //   2. 底部容器（scroll-aware 动画组）：导航栏
                    //      - 下滑内容 → 整体向下移出屏幕（spring 动画）
                    //      - 上滑内容 → 整体回到原位（spring 动画）
                    //
                    // 底部间距 = MD3 导航栏内容高度(80dp)。
                    // v0.9.22 修复 double inset：系统手势条区域由内层 ExpressiveScaffold
                    // 的 contentWindowInsets 消费（与顶部 statusBars 处理对称），外层只避让
                    // 导航栏 80dp。此前外层加 80dp + 手势区、内层 Scaffold 又消费一次手势区，
                    // 导致底部多出一个手势条高度的空白（用户反馈"底栏上方大面积空白"根因）。
                    // 导航栏自身通过 windowInsets 吃手势条（总高 = 80dp + 手势条）。
                    val bottomPadding = 80.dp

                    // KSU 风格滚动感知显隐：监听 LocalLazyListState 的滚动方向
                    val scrollState = LocalLazyListState.current
                    var barVisible by remember { mutableStateOf(true) }

                    if (scrollState != null) {
                        // 记录上一次滚动位置，用于检测滚动方向
                        var previousIndex by remember { mutableStateOf(0) }
                        var previousOffset by remember { mutableStateOf(0) }

                        LaunchedEffect(scrollState) {
                            // v0.9.25 修复：Tab 切换时 scrollState 变化（LocalLazyListState
                            // 提供新 Tab 的列表），重置导航栏可见与滚动方向基线。
                            // 原实现 previousIndex/previousOffset/barVisible 跨 Tab 保留：
                            // A Tab 下滑隐藏导航栏后切到 B Tab，B 列表在顶部但导航栏仍隐藏；
                            // 且首帧滚动方向会用 A 的旧位置误判。
                            previousIndex = 0
                            previousOffset = 0
                            barVisible = true
                            snapshotFlow {
                                scrollState.firstVisibleItemIndex to
                                    scrollState.firstVisibleItemScrollOffset
                            }.collect { (index, offset) ->
                                val direction = detectScrollDirection(
                                    index = index,
                                    offset = offset,
                                    previousIndex = previousIndex,
                                    previousOffset = previousOffset,
                                )
                                when (direction) {
                                    ScrollDirection.DOWN -> barVisible = false
                                    ScrollDirection.UP -> barVisible = true
                                    ScrollDirection.IDLE -> { /* no change */ }
                                }

                                previousIndex = index
                                previousOffset = offset
                            }
                        }
                    }

                    // 底部容器的总隐藏距离 = 导航栏总高（内容 80dp + 手势条区域）
                    val bottomHideDistance = 80.dp + systemNavBarBottomDp
                    val bottomOffset by animateDpAsState(
                        targetValue = if (barVisible) 0.dp else bottomHideDistance,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "bottomNavGroupOffset",
                    )

                    // 1. 内容区：surfaceContainer 背景 + 底部显式 padding
                    // 顶部不加 padding（内层 ExpressiveScaffold 消费 statusBars）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(bottom = bottomPadding),
                    ) {
                        content(PaddingValues(0.dp))
                    }

                    // 2. 底部容器（scroll-aware 动画组）：导航栏
                    // 下滑时一起移出屏幕，上滑时一起回到原位
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset { IntOffset(0, bottomOffset.roundToPx()) },
                    ) {
                        // 底部导航栏（MD3 标准，全宽贴底）
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
 * 滚动方向枚举。
 *
 * 用于 [detectScrollDirection] 的返回值，表示本次滚动帧的方向。
 */
enum class ScrollDirection {
    /** 向下滚动（内容向屏幕上方移动） */
    DOWN,
    /** 向上滚动（内容向屏幕下方移动） */
    UP,
    /** 无明显方向变化（偏移量在防抖阈值内） */
    IDLE,
}

/**
 * 检测滚动方向（纯函数，可独立测试）。
 *
 * 通过比较当前帧与上一帧的 [LazyListState] 快照来判断滚动方向。
 *
 * 判定逻辑：
 * - [ScrollDirection.DOWN]：firstVisibleItemIndex 增大，或同一 index 但 offset 增量 > [threshold]
 * - [ScrollDirection.UP]：firstVisibleItemIndex 减小，或同一 index 但 offset 减量 > [threshold]
 * - [ScrollDirection.IDLE]：未满足上述条件（变化在防抖阈值内，或到达列表边界）
 *
 * @param index 当前帧的 firstVisibleItemIndex
 * @param offset 当前帧的 firstVisibleItemScrollOffset
 * @param previousIndex 上一帧的 firstVisibleItemIndex
 * @param previousOffset 上一帧的 firstVisibleItemScrollOffset
 * @param threshold 防抖阈值（像素），默认 10px。偏移变化小于此值时判定为 IDLE。
 * @return 滚动方向
 */
fun detectScrollDirection(
    index: Int,
    offset: Int,
    previousIndex: Int,
    previousOffset: Int,
    threshold: Int = 10,
): ScrollDirection {
    // index 优先：跨 item 的滚动方向由 index 决定
    if (index > previousIndex) return ScrollDirection.DOWN
    if (index < previousIndex) return ScrollDirection.UP

    // 同一 index 内：由 offset 变化量决定，带防抖阈值
    val delta = offset - previousOffset
    return when {
        delta > threshold -> ScrollDirection.DOWN
        delta < -threshold -> ScrollDirection.UP
        else -> ScrollDirection.IDLE
    }
}
