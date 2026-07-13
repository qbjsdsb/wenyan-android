package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wenyan.app.core.designsystem.component.HierarchicalListItem
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

@Preview(name = "Light - Tree structure", showBackground = true)
@Composable
private fun HierarchicalListItemLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface {
            Column {
                HierarchicalListItem(title = "中国现代文学史", depth = 0, onClick = {})
                HierarchicalListItem(title = "第一章：文学革命", depth = 1, onClick = {})
                HierarchicalListItem(title = "《新青年》与白话文运动", depth = 2, onClick = {})
                HierarchicalListItem(title = "鲁迅《狂人日记》", depth = 2, onClick = {})
                HierarchicalListItem(title = "第二章：新诗", depth = 1, onClick = {})
            }
        }
    }
}

@Preview(name = "Dark - With trailing", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HierarchicalListItemDarkPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, dynamicColor = false)) {
        Surface {
            Column {
                HierarchicalListItem(
                    title = "现代文学三十年",
                    depth = 0,
                    trailing = { Text("已掌握") },
                )
                HierarchicalListItem(title = "第一个十年", depth = 1, onClick = {})
                HierarchicalListItem(title = "第二个十年", depth = 1, onClick = {})
            }
        }
    }
}

@Preview(name = "AMOLED - No onClick", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HierarchicalListItemAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true, dynamicColor = false)) {
        Surface {
            Column {
                HierarchicalListItem(title = "知识点树（只读）", depth = 0)
                HierarchicalListItem(title = "子节点 A", depth = 1)
                HierarchicalListItem(title = "子节点 B", depth = 1)
            }
        }
    }
}
