package com.wenyan.app.core.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wenyan.app.core.data.util.catchAndLog
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [ThemeRepository] 的 DataStore Preferences 实现。
 *
 * 使用以下键存储 [ThemeConfig] 各字段：
 * - COLOR_MODE_KEY: String (枚举 name)
 * - AMOLED_KEY: Boolean
 * - PALETTE_STYLE_KEY: String (枚举 name)
 * - DYNAMIC_COLOR_KEY: Boolean
 * - SEED_COLOR_KEY: Int (ARGB)
 *
 * P1-NEW-7 修正：枚举 valueOf 改为 runCatching 容错。
 * 原实现直接 valueOf，若未来版本删除了某枚举值（或 DataStore 被外部写入非法值），
 * valueOf 抛 IllegalArgumentException → themeConfig Flow 崩溃 → 整个 App 主题系统瘫痪。
 * 改为 runCatching { valueOf(...) }.getOrNull() ?: DEFAULT，遇到非法值降级为默认值。
 *
 * P1 审计修复：themeConfig Flow 加 .catchAndLog，DataStore IO 异常（磁盘满/文件损坏）
 * 时降级为默认 ThemeConfig，避免 App 启动时主题系统崩溃导致白屏。
 */
class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemeRepository {

    override val themeConfig: Flow<ThemeConfig> = dataStore.data.map { prefs ->
        ThemeConfig(
            colorMode = prefs[COLOR_MODE_KEY]?.let { parseColorMode(it) } ?: ColorMode.SYSTEM,
            amoledMode = prefs[AMOLED_KEY] ?: false,
            paletteStyle = prefs[PALETTE_STYLE_KEY]?.let { parsePaletteStyle(it) }
                ?: WenyanPaletteStyle.TONAL_SPOT,
            dynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: true,
            seedColor = Color(prefs[SEED_COLOR_KEY] ?: 0xFF6750A4.toInt()),
        )
    }.catchAndLog(TAG, "themeConfig") { ThemeConfig() }

    /** 解析 ColorMode，非法值降级为 SYSTEM */
    private fun parseColorMode(name: String): ColorMode? =
        runCatching { ColorMode.valueOf(name) }.getOrNull()

    /** 解析 WenyanPaletteStyle，非法值降级为 TONAL_SPOT */
    private fun parsePaletteStyle(name: String): WenyanPaletteStyle? =
        runCatching { WenyanPaletteStyle.valueOf(name) }.getOrNull()

    override suspend fun setColorMode(mode: ColorMode) {
        dataStore.edit { it[COLOR_MODE_KEY] = mode.name }
    }

    override suspend fun setAmoledMode(enabled: Boolean) {
        dataStore.edit { it[AMOLED_KEY] = enabled }
    }

    override suspend fun setPaletteStyle(style: WenyanPaletteStyle) {
        dataStore.edit { it[PALETTE_STYLE_KEY] = style.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    override suspend fun setSeedColor(color: Color) {
        dataStore.edit { it[SEED_COLOR_KEY] = color.toArgb() }
    }

    private companion object {
        private const val TAG = "ThemeRepositoryImpl"
        val COLOR_MODE_KEY = stringPreferencesKey("color_mode")
        val AMOLED_KEY = booleanPreferencesKey("amoled_mode")
        val PALETTE_STYLE_KEY = stringPreferencesKey("palette_style")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val SEED_COLOR_KEY = intPreferencesKey("seed_color")
    }
}
