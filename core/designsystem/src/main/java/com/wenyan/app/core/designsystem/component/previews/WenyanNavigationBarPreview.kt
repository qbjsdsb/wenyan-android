package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wenyan.app.core.designsystem.component.WenyanNavItem
import com.wenyan.app.core.designsystem.component.WenyanNavigationBar
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

// v0.9.0 底部 Tab：知识点/真题/卡片/错题本/设置（图谱已移除，AI 改子路由）
private val sampleItems = listOf(
    WenyanNavItem("knowledge", "知识点", Icons.Default.LibraryBooks),
    WenyanNavItem("quiz", "真题", Icons.Default.BarChart),
    WenyanNavItem("cards", "卡片", Icons.Default.Style),
    WenyanNavItem("wrong_answer", "错题本", Icons.Default.ErrorOutline),
    WenyanNavItem("settings", "设置", Icons.Default.Settings),
)

@Preview(name = "Light - Knowledge selected", showBackground = true)
@Composable
private fun WenyanNavigationBarLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface {
            WenyanNavigationBar(
                items = sampleItems,
                currentRoute = "knowledge",
                onNavigate = {},
            )
        }
    }
}

@Preview(name = "Dark - AI selected", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WenyanNavigationBarDarkPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, dynamicColor = false)) {
        Surface {
            WenyanNavigationBar(
                items = sampleItems,
                currentRoute = "aiassistant",
                onNavigate = {},
            )
        }
    }
}

@Preview(name = "AMOLED - Cards selected", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WenyanNavigationBarAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true, dynamicColor = false)) {
        Surface {
            WenyanNavigationBar(
                items = sampleItems,
                currentRoute = "cards",
                onNavigate = {},
            )
        }
    }
}
