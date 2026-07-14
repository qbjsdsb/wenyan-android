package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wenyan.app.core.designsystem.component.WenyanNavItem
import com.wenyan.app.core.designsystem.component.WenyanWideNavigationRail
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

// v0.6 大屏适配后底部 Tab 改为：知识点/真题/卡片/图谱/设置（AI 改为子路由）
private val sampleRailItems = listOf(
    WenyanNavItem("knowledge", "知识点", Icons.Default.LibraryBooks),
    WenyanNavItem("quiz", "真题", Icons.Default.BarChart),
    WenyanNavItem("cards", "卡片", Icons.Default.Style),
    WenyanNavItem("graph", "图谱", Icons.Default.AccountBox),
    WenyanNavItem("settings", "设置", Icons.Default.Settings),
)

@Preview(name = "Light - Expanded (大平板)", showBackground = true, widthDp = 100, heightDp = 600)
@Composable
private fun WenyanWideNavigationRailExpandedLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface {
            Box(modifier = Modifier.width(120.dp).fillMaxHeight()) {
                WenyanWideNavigationRail(
                    expanded = true,
                    items = sampleRailItems,
                    currentRoute = "knowledge",
                    onNavigate = {},
                )
            }
        }
    }
}

@Preview(name = "Dark - Collapsed (小平板)", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 80, heightDp = 600)
@Composable
private fun WenyanWideNavigationRailCollapsedDarkPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, dynamicColor = false)) {
        Surface {
            Box(modifier = Modifier.width(80.dp).fillMaxHeight()) {
                WenyanWideNavigationRail(
                    expanded = false,
                    items = sampleRailItems,
                    currentRoute = "cards",
                    onNavigate = {},
                )
            }
        }
    }
}

@Preview(name = "AMOLED - Expanded (大平板)", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 100, heightDp = 600)
@Composable
private fun WenyanWideNavigationRailExpandedAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true, dynamicColor = false)) {
        Surface {
            Box(modifier = Modifier.width(120.dp).fillMaxHeight()) {
                WenyanWideNavigationRail(
                    expanded = true,
                    items = sampleRailItems,
                    currentRoute = "settings",
                    onNavigate = {},
                )
            }
        }
    }
}
