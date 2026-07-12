package com.wenyan.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/**
 * 主题配置状态。
 *
 * @param colorMode 颜色模式（跟随系统/浅色/深色）
 * @param amoledMode AMOLED 纯黑模式（仅深色模式生效）
 * @param paletteStyle 调色板风格
 * @param dynamicColor 是否使用动态色彩（Android 12+ 自动跟随壁纸）
 * @param seedColor 种子色（动态色彩关闭时使用）
 */
data class ThemeConfig(
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val amoledMode: Boolean = false,
    val paletteStyle: WenyanPaletteStyle = WenyanPaletteStyle.TONAL_SPOT,
    val dynamicColor: Boolean = true,
    val seedColor: Color = Color(0xFF6750A4),
)

/**
 * 颜色模式。
 */
enum class ColorMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * 调色板风格（映射到 materialkolor 的 PaletteStyle）。
 */
enum class WenyanPaletteStyle {
    TONAL_SPOT,
    NEUTRAL,
    VIBRANT,
    EXPRESSIVE,
}

/**
 * 将 [WenyanPaletteStyle] 转换为 materialkolor 的 [PaletteStyle]。
 */
fun WenyanPaletteStyle.toMaterialKolorStyle(): PaletteStyle = when (this) {
    WenyanPaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
    WenyanPaletteStyle.NEUTRAL -> PaletteStyle.Neutral
    WenyanPaletteStyle.VIBRANT -> PaletteStyle.Vibrant
    WenyanPaletteStyle.EXPRESSIVE -> PaletteStyle.Expressive
}
