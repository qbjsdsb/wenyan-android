package com.wenyan.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow

/**
 * 主题配置仓库接口。
 *
 * 提供 [ThemeConfig] 的读取和持久化能力，
 * 底层使用 DataStore Preferences 存储。
 *
 * P1-8 修复：从 core/data 迁入 core/designsystem。
 * 原因：ThemeRepository 操作 [ColorMode] / [ThemeConfig] / [WenyanPaletteStyle]
 * 等设计系统类型，放在 core/data 导致 core/data → core/designsystem 反向依赖。
 * 迁入 designsystem 后类型与行为同处一模块，依赖方向正确。
 */
interface ThemeRepository {
    val themeConfig: Flow<ThemeConfig>

    suspend fun setColorMode(mode: ColorMode)
    suspend fun setAmoledMode(enabled: Boolean)
    suspend fun setPaletteStyle(style: WenyanPaletteStyle)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setSeedColor(color: Color)
}
