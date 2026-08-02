package com.wenyan.app.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * 文研底部导航栏项数据。
 *
 * @param route 导航路由
 * @param label 显示标签
 * @param icon 图标
 */
data class WenyanNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * 文研底部导航栏（规范 Material 3 风格）。
 *
 * v0.9.20 MD3 规范回归（2026-08-02）：
 *   - 容器色：`surfaceContainer` 实色（对齐 docs/design/m3-expressive-redesign.md §5.1）
 *   - 高度：80dp（Material 3 标准 NavigationBar 高度）
 *   - 形状：全宽直角（MD3 标准 NavigationBar 无圆角）
 *   - 阴影：tonalElevation 3dp（MD3 默认浅投影，提供底栏层次）
 *   - 移除 v0.9.19/v0.9.20 的半透明假玻璃 + 光泽叠加层（非 MD3 元素）
 *
 * 配色对齐 MD3 规范（docs/design/m3-expressive-redesign.md §5.1）：
 * 选中态用 secondaryContainer 药丸指示器 + onSecondaryContainer，
 * 未选中态用 onSurfaceVariant。
 *
 * 注：material3 1.5.0-alpha18 起 [NavigationBarItemDefaults.colors] 的
 * `selectedIndicatorColor` 参数已重命名为 `indicatorColor`，本封装已对齐。
 *
 * @param items 导航项列表
 * @param currentRoute 当前路由
 * @param onNavigate 点击导航回调，参数为 item.route
 * @param modifier 修饰符
 * @param visible 是否可见（滚动感知显隐，下滑隐藏/上滑显示）
 */
@Composable
fun WenyanNavigationBar(
    items: List<WenyanNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    // MD3 标准 NavigationBar 高度：80dp（图标 + 文字舒适间距）
    val navHeight: Dp = 80.dp

    // 滚动感知显隐（保留 v0.9.20 行为）：spring 动画驱动 translationY
    // 下滑隐藏（visible=false）→ 导航栏向下移出屏幕
    // 上滑显示（visible=true）→ 导航栏回到原位
    val translationY by animateDpAsState(
        targetValue = if (visible) 0.dp else navHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "navBarTranslationY",
    )

    NavigationBar(
        // MD3 规范：容器色 surfaceContainer（实色，对齐 §5.1）
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        // MD3 默认浅投影，提供底栏层次
        tonalElevation = 3.dp,
        // windowInsets：底栏吃底部系统手势条 inset。
        // 总高 = 内容 80dp（icon+label 居中）+ 底部手势条区域，
        // 与内容区 bottomPadding(80dp + 手势条) 对齐，避免底栏上方多余空白。
        windowInsets = NavigationBarDefaults.windowInsets,
        // 全宽直角 + 滚动感知 offset
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, translationY.roundToPx()) },
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                // v0.8.3 修复：Icon 设为装饰性（null），由 label Text 提供唯一语义名称
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                label = { Text(text = item.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}