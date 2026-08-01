package com.wenyan.app.core.designsystem.component

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * 文研底部导航栏（流体玻璃风格）。
 *
 * v0.9.20 流体玻璃改造：
 *   - 全宽：无水平留边，Surface 撑满屏幕宽度
 *   - 仅顶部圆角（16dp），底部直角贴底，还原 iOS Tab Bar 风格
 *   - 高度 72dp（5 项 Tab 舒适间距）
 *   - 增强玻璃效果：半透明 surfaceContainerHigh（alpha=0.75f）+ 光泽渐变 + Android 12+ blur
 *   - 无底部留空，导航栏直接延伸到屏幕底部
 *
 * 参照 KernelSU 的配色策略：选中态用 secondaryContainer 提供药丸状指示器，
 * 文字与图标用 onSecondaryContainer 形成高对比；未选中态降级到 onSurfaceVariant。
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
    visible: Boolean = true,
) {
    // 流体玻璃导航栏：全宽 + 顶部圆角 + 增强玻璃质感
    // 形状：仅顶部圆角（16dp），底部直角贴底，还原 iOS Tab Bar 风格
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    )

    // 导航栏高度：72dp — 5 项 Tab 舒适间距，不拥挤也不过度占用空间
    val navHeight: Dp = 72.dp

    // KSU 风格滚动感知显隐：spring 动画驱动 translationY
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

    Surface(
        shape = shape,
        tonalElevation = 0.dp, // 用玻璃效果代替阴影
        // 半透明背景：alpha=0.75f 让内容微微透出，模拟毛玻璃
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
        // 全宽：无水平留边，底部无留空，直接贴底
        // offset 驱动导航栏的滚动感知显隐
        modifier = modifier.offset { IntOffset(0, translationY.roundToPx()) },
    ) {
        // Android 12+ 玻璃光泽叠加层
        // 1. 顶部边缘高光（细白线，模拟 iOS 反光）
        // 2. 水平光泽渐变（模拟玻璃表面的环境反射）
        val glassOverlayModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier
                // 顶部高光边缘：模拟 iOS Tab Bar 的顶部细线反射
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = 2f, // 仅顶部 2px 高光
                    ),
                    shape = shape,
                )
                // 水平光泽：模拟玻璃表面的环境光反射
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                    shape = shape,
                )
        } else {
            // Android 12 以下：仅用渐变模拟玻璃质感
            Modifier.background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.03f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.05f),
                    ),
                ),
                shape = shape,
            )
        }

        Box(modifier = glassOverlayModifier.fillMaxWidth()) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                // 高度 72dp：5 项 Tab 舒适间距
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navHeight),
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
    }
}