package com.wenyan.app.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * 文研底部导航栏（M3 Expressive 风格）。
 *
 * 参照 KernelSU 的配色策略：选中态用 secondaryContainer 提供药丸状指示器，
 * 文字与图标用 onSecondaryContainer 形成高对比；未选中态降级到 onSurfaceVariant。
 * 容器色用 surfaceContainer 与 TopAppBar / Scaffold 保持一致的层级表达。
 *
 * 注：material3 1.5.0-alpha18 起 [NavigationBarItemDefaults.colors] 的
 * `selectedIndicatorColor` 参数已重命名为 `indicatorColor`，本封装已对齐。
 *
 * @param items 导航项列表
 * @param currentRoute 当前路由
 * @param onNavigate 点击导航回调，参数为 item.route
 * @param modifier 修饰符
 */
@Composable
fun WenyanNavigationBar(
    items: List<WenyanNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                // v0.8.3 修复：原 contentDescription = item.label 与 label Text 重复，
                // TalkBack 朗读"首页首页"。NavigationBarItem 已合并子节点语义，
                // Icon 设为装饰性（null），由 label Text 提供唯一语义名称。
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
