package com.wenyan.app.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

/**
 * 文研App 主题入口（Material 3 Expressive）。
 *
 * 使用 [MaterialExpressiveTheme] + [MotionScheme.expressive] 实现 M3 Expressive 设计语言。
 * 颜色方案由以下优先级生成：
 * 1. Android 12+ 且 [ThemeConfig.dynamicColor] 开启 → 系统壁纸动态色彩
 * 2. 其他情况 → materialkolor 从种子色生成（SPEC_2025 规范）
 *
 * AMOLED 模式在深色模式下将底层表面替换为纯黑，节省 OLED 电量。
 *
 * @param config 主题配置
 * @param content 可组合内容
 */
@Composable
fun WenyanTheme(
    config: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = when (config.colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }

    // 生成基础 ColorScheme
    val baseScheme = if (config.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+：使用系统壁纸提取的动态色彩
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Android 11- 或手动种子色：用 materialkolor 生成
        val paletteStyle = config.paletteStyle.toMaterialKolorStyle()
        rememberDynamicColorScheme(
            seedColor = config.seedColor,
            isDark = isDark,
            style = paletteStyle,
            specVersion = if (paletteStyle.supportsSpec2025) {
                ColorSpec.SpecVersion.SPEC_2025
            } else {
                ColorSpec.SpecVersion.SPEC_2021
            },
        )
    }

    // AMOLED 模式：将底层表面替换为纯黑
    val finalScheme = if (isDark && config.amoledMode) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color.Black,
        )
    } else {
        baseScheme
    }

    MaterialExpressiveTheme(
        colorScheme = finalScheme,
        motionScheme = MotionScheme.expressive(),
        typography = WenyanTypography,
        shapes = WenyanShapes,
        content = content,
    )
}
