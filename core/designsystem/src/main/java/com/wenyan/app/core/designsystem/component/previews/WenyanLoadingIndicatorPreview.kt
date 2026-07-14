package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

@Preview(name = "Light - LoadingIndicator", showBackground = true, widthDp = 200, heightDp = 200)
@Composable
private fun WenyanLoadingIndicatorLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                WenyanLoadingIndicator(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Preview(name = "Dark - LoadingIndicator", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 200, heightDp = 200)
@Composable
private fun WenyanLoadingIndicatorDarkPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, dynamicColor = false)) {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                WenyanLoadingIndicator(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Preview(name = "AMOLED - LoadingIndicator", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 200, heightDp = 200)
@Composable
private fun WenyanLoadingIndicatorAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true, dynamicColor = false)) {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                WenyanLoadingIndicator(modifier = Modifier.size(48.dp))
            }
        }
    }
}
