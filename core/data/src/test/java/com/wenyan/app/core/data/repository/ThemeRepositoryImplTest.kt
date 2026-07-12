package com.wenyan.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeRepositoryImplTest {

    @Test
    fun `default config returns system mode and tonal spot style`() = runTest {
        val repo = ThemeRepositoryImpl(FakeDataStore())
        val config = repo.themeConfig.first()
        assertEquals(ColorMode.SYSTEM, config.colorMode)
        assertEquals(false, config.amoledMode)
        assertEquals(WenyanPaletteStyle.TONAL_SPOT, config.paletteStyle)
        assertEquals(true, config.dynamicColor)
    }

    @Test
    fun `setColorMode persists dark mode`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = ThemeRepositoryImpl(fakeStore)
        repo.setColorMode(ColorMode.DARK)
        val config = repo.themeConfig.first()
        assertEquals(ColorMode.DARK, config.colorMode)
    }

    @Test
    fun `setAmoledMode persists enabled`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = ThemeRepositoryImpl(fakeStore)
        repo.setAmoledMode(true)
        val config = repo.themeConfig.first()
        assertEquals(true, config.amoledMode)
    }

    @Test
    fun `setPaletteStyle persists expressive`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = ThemeRepositoryImpl(fakeStore)
        repo.setPaletteStyle(WenyanPaletteStyle.EXPRESSIVE)
        val config = repo.themeConfig.first()
        assertEquals(WenyanPaletteStyle.EXPRESSIVE, config.paletteStyle)
    }

    @Test
    fun `setDynamicColor persists disabled`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = ThemeRepositoryImpl(fakeStore)
        repo.setDynamicColor(false)
        val config = repo.themeConfig.first()
        assertEquals(false, config.dynamicColor)
    }

    @Test
    fun `setSeedColor persists custom color`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = ThemeRepositoryImpl(fakeStore)
        repo.setSeedColor(Color.Red)
        val config = repo.themeConfig.first()
        assertEquals(Color.Red, config.seedColor)
    }

    @Test
    fun `themeConfig emits updates on change`() = runTest {
        val repo = ThemeRepositoryImpl(FakeDataStore())
        repo.themeConfig.test {
            // 初始值
            assertEquals(ColorMode.SYSTEM, awaitItem().colorMode)
            // 设置后发射新值
            repo.setColorMode(ColorMode.DARK)
            assertEquals(ColorMode.DARK, awaitItem().colorMode)
        }
    }
}

/**
 * Fake DataStore for testing.
 */
private class FakeDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        val newValue = transform(state.value)
        state.value = newValue
        return newValue
    }
}
