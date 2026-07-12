package com.wenyan.app.core.data.repository

import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow

/**
 * 主题配置仓库接口。
 *
 * 提供 [ThemeConfig] 的读取和持久化能力，
 * 底层使用 DataStore Preferences 存储。
 */
interface ThemeRepository {
    val themeConfig: Flow<ThemeConfig>

    suspend fun setColorMode(mode: ColorMode)
    suspend fun setAmoledMode(enabled: Boolean)
    suspend fun setPaletteStyle(style: WenyanPaletteStyle)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setSeedColor(color: Color)
}
