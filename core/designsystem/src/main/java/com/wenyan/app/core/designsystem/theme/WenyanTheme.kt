package com.wenyan.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 文研App 浅色配色方案
private val WenyanLightColorScheme = lightColorScheme(
    primary = WenyanPrimary,
    onPrimary = WenyanOnPrimary,
    primaryContainer = WenyanPrimaryContainer,
    onPrimaryContainer = WenyanOnPrimaryContainer,
    secondary = WenyanSecondary,
    onSecondary = WenyanOnSecondary,
    secondaryContainer = WenyanSecondaryContainer,
    onSecondaryContainer = WenyanOnSecondaryContainer,
    tertiary = WenyanTertiary,
    onTertiary = WenyanOnTertiary,
    background = WenyanBackground,
    onBackground = WenyanOnBackground,
    surface = WenyanSurface,
    onSurface = WenyanOnSurface,
    surfaceVariant = WenyanSurfaceVariant,
    onSurfaceVariant = WenyanOnSurfaceVariant,
    error = WenyanError,
    onError = WenyanOnError,
)

// 文研App 深色配色方案
private val WenyanDarkColorScheme = darkColorScheme(
    primary = WenyanOnPrimary,
    onPrimary = WenyanPrimary,
    primaryContainer = WenyanOnPrimaryContainer,
    onPrimaryContainer = WenyanPrimaryContainer,
    secondary = WenyanSecondary,
    onSecondary = WenyanOnSecondary,
    secondaryContainer = WenyanSecondaryContainer,
    onSecondaryContainer = WenyanOnSecondaryContainer,
    tertiary = WenyanTertiary,
    onTertiary = WenyanOnTertiary,
    background = Color(0xFF1A1A1A),
    onBackground = WenyanOnPrimary,
    surface = Color(0xFF242424),
    onSurface = WenyanOnPrimary,
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = Color(0xFFD0D0D0),
    error = WenyanError,
    onError = WenyanOnError,
)

// 文研App 主题入口
@Composable
fun WenyanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) WenyanDarkColorScheme else WenyanLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WenyanTypography,
        content = content,
    )
}
