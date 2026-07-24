package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailItemDefaults
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

/**
 * 文研宽导航栏（M3 Expressive WideNavigationRail）。
 *
 * v0.6 大屏适配：在 Medium（600-840dp）和 Expanded（≥840dp）窗口宽度下
 * 替代底部 [WenyanNavigationBar]，提供左侧侧边导航。
 *
 * - 折叠态（Medium）：仅显示图标，节省横向空间
 * - 展开态（Expanded）：图标 + 标签，完整导航信息
 *
 * 配色策略与 [WenyanNavigationBar] 一致：
 * - 容器：surfaceContainer（与 Scaffold 层级一致）
 * - 选中指示器：secondaryContainer（药丸风格）
 * - 选中文字/图标：onSecondaryContainer
 * - 未选中：onSurfaceVariant
 *
 * @param expanded 是否展开（true=图标+标签，false=仅图标）
 * @param items 导航项列表
 * @param currentRoute 当前路由
 * @param onNavigate 点击导航回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WenyanWideNavigationRail(
    expanded: Boolean,
    items: List<WenyanNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val railState = rememberWideNavigationRailState(
        initialValue = if (expanded) {
            WideNavigationRailValue.Expanded
        } else {
            WideNavigationRailValue.Collapsed
        },
    )
    // v0.8.3 修复：原实现仅用 expanded 作 initialValue，参数变化时 railState 不会同步，
    // 导致外部"展开/折叠"按钮点击后视觉无响应。添加 LaunchedEffect 强制同步。
    LaunchedEffect(expanded) {
        if (expanded) railState.expand() else railState.collapse()
    }
    val colorScheme = MaterialTheme.colorScheme
    WideNavigationRail(
        modifier = modifier.fillMaxHeight(),
        state = railState,
        colors = WideNavigationRailDefaults.colors(
            containerColor = colorScheme.surfaceContainer,
        ),
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            WideNavigationRailItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                // v0.8.3 修复：同 WenyanNavigationBar，Icon 设为装饰性避免与 label 重复读屏
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                label = { Text(text = item.label) },
                railExpanded = expanded,
                colors = WideNavigationRailItemDefaults.colors(
                    selectedIndicatorColor = colorScheme.secondaryContainer,
                    selectedIconColor = colorScheme.onSecondaryContainer,
                    selectedTextColor = colorScheme.onSecondaryContainer,
                    unselectedIconColor = colorScheme.onSurfaceVariant,
                    unselectedTextColor = colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
