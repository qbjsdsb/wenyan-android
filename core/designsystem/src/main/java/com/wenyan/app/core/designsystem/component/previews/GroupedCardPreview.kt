package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

@Preview(name = "Light - Settings style", showBackground = true)
@Composable
private fun GroupedCardSettingsLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface {
            GroupedCard(title = "外观") {
                GroupedCardItem(
                    title = "主题模式",
                    subtitle = "跟随系统",
                    leadingIcon = Icons.Default.Palette,
                )
                GroupedCardDivider()
                GroupedCardItem(
                    title = "AMOLED 纯黑模式",
                    description = "深色模式下使用纯黑背景，节省 OLED 电量",
                    trailing = { Switch(checked = false, onCheckedChange = {}) },
                )
            }
        }
    }
}

@Preview(name = "Dark - About style", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GroupedCardAboutDarkPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, dynamicColor = false)) {
        Surface {
            GroupedCard(title = "关于") {
                GroupedCardItem(title = "版本", subtitle = "v0.1.0")
                GroupedCardDivider()
                GroupedCardItem(title = "API 配置", subtitle = "DeepSeek", onClick = {})
                GroupedCardDivider()
                GroupedCardItem(title = "检查更新", onClick = {})
            }
        }
    }
}

@Preview(name = "AMOLED - Knowledge related", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GroupedCardRelatedAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true, dynamicColor = false)) {
        Surface {
            GroupedCard(title = "关联") {
                GroupedCardItem(title = "鲁迅《狂人日记》", onClick = {})
                GroupedCardDivider()
                GroupedCardItem(title = "《呐喊》自序", onClick = {})
                GroupedCardDivider()
                GroupedCardItem(title = "新文化运动", onClick = {})
            }
        }
    }
}
