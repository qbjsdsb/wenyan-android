package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

@Preview(name = "Light - Simple", showBackground = true)
@Composable
private fun WenyanLargeTopAppBarSimplePreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface {
            WenyanLargeTopAppBar(title = "知识点")
        }
    }
}

@Preview(name = "Light - With Subtitle + Back", showBackground = true)
@Composable
private fun WenyanLargeTopAppBarWithSubtitlePreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface {
            WenyanLargeTopAppBar(
                title = "鲁迅《狂人日记》",
                subtitle = "高频 · 难度4/5",
                onBack = {},
            )
        }
    }
}

@Preview(name = "AMOLED - With Subtitle", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WenyanLargeTopAppBarAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true, dynamicColor = false)) {
        Surface {
            WenyanLargeTopAppBar(
                title = "鲁迅《狂人日记》",
                subtitle = "高频 · 难度4/5",
                onBack = {},
            )
        }
    }
}
