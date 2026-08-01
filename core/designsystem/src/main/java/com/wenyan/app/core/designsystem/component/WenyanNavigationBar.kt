package com.wenyan.app.core.designsystem.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    // v0.9.18 悬浮导航栏改造：Surface 包裹 NavigationBar 提供圆角/投影/间距
    // v0.9.19 紧凑玻璃风格改造：
    //   - 圆角 16dp → 24dp，更圆润
    //   - tonalElevation 3dp → 2dp，更轻
    //   - 颜色 surfaceContainer → surfaceContainerHigh.copy(alpha=0.85f)，半透明玻璃质感
    //   - 水平留边 16dp → 8dp，底部 8dp → 4dp，更紧凑
    //   - NavigationBar 高度 80dp → 56dp，减少遮挡
    //   - Android 12+ 叠加渐变光泽 overlay，模拟流体玻璃
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        // 水平留边 8dp，底部在系统手势区之上 4dp
        // 注：padding(horizontal, bottom) 无此重载，需链式调用
        modifier = modifier.padding(horizontal = 8.dp).padding(bottom = 4.dp),
    ) {
        // Android 12+ 叠加渐变光泽，模拟玻璃反光效果
        val glassOverlayModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
                shape = RoundedCornerShape(24.dp),
            )
        } else {
            Modifier
        }
        Box(modifier = glassOverlayModifier.fillMaxWidth()) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                // 紧凑：高度从 80dp 降至 56dp
                modifier = Modifier.height(56.dp),
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
    }
}
