package com.wenyan.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ThemeRepositoryImpl 单元测试（P1-8 修复：从 core/data 迁入 core/designsystem）。
 *
 * 注意：原 testOptions 未设置 isReturnDefaultValues=true，
 * ThemeRepositoryImpl 用了 android.util.Log.e，JVM 单元测试默认会抛 RuntimeException。
 * designsystem 模块的 build.gradle.kts 也未设置该选项。
 * 现迁入 designsystem 后需在 designsystem 的 android.testOptions.unitTests 加
 * isReturnDefaultValues = true，否则 .catch 块的 Log.e 会抛异常导致测试失败。
 */
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
