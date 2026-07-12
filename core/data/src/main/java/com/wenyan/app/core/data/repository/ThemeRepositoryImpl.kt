package com.wenyan.app.core.data.repository

import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
 */
class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemeRepository {

    override val themeConfig: Flow<ThemeConfig> = dataStore.data.map { prefs ->
        ThemeConfig(
            colorMode = ColorMode.valueOf(
                prefs[COLOR_MODE_KEY] ?: ColorMode.SYSTEM.name,
            ),
            amoledMode = prefs[AMOLED_KEY] ?: false,
            paletteStyle = WenyanPaletteStyle.valueOf(
                prefs[PALETTE_STYLE_KEY] ?: WenyanPaletteStyle.TONAL_SPOT.name,
            ),
            dynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: true,
            seedColor = Color(prefs[SEED_COLOR_KEY] ?: 0xFF6750A4.toInt()),
        )
    }

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
        dataStore.edit { it[SEED_COLOR_KEY] = color.value.toInt() }
    }

    private companion object {
        val COLOR_MODE_KEY = stringPreferencesKey("color_mode")
        val AMOLED_KEY = booleanPreferencesKey("amoled_mode")
        val PALETTE_STYLE_KEY = stringPreferencesKey("palette_style")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val SEED_COLOR_KEY = intPreferencesKey("seed_color")
    }
}
