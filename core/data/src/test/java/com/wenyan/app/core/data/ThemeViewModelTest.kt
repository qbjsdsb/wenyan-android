package com.wenyan.app.core.data

import app.cash.turbine.test
import com.wenyan.app.core.data.repository.ThemeRepository
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeViewModelTest {

    @Test
    fun `initial state is default ThemeConfig`() = runTest {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        assertEquals(ThemeConfig(), viewModel.themeConfig.value)
    }

    @Test
    fun `setColorMode updates state to DARK`() = runTest {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        viewModel.setColorMode(ColorMode.DARK)
        assertEquals(ColorMode.DARK, viewModel.themeConfig.value.colorMode)
    }

    @Test
    fun `setAmoledMode updates state to true`() = runTest {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        viewModel.setAmoledMode(true)
        assertEquals(true, viewModel.themeConfig.value.amoledMode)
    }

    @Test
    fun `setPaletteStyle updates state to EXPRESSIVE`() = runTest {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        viewModel.setPaletteStyle(WenyanPaletteStyle.EXPRESSIVE)
        assertEquals(WenyanPaletteStyle.EXPRESSIVE, viewModel.themeConfig.value.paletteStyle)
    }

    @Test
    fun `themeConfig emits updates`() = runTest {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        viewModel.themeConfig.test {
            assertEquals(ThemeConfig(), awaitItem())
            viewModel.setDynamicColor(false)
            assertEquals(false, awaitItem().dynamicColor)
        }
    }
}

private class FakeThemeRepository : ThemeRepository {
    private val config = MutableStateFlow(ThemeConfig())
    override val themeConfig: Flow<ThemeConfig> = config

    override suspend fun setColorMode(mode: ColorMode) {
        config.value = config.value.copy(colorMode = mode)
    }

    override suspend fun setAmoledMode(enabled: Boolean) {
        config.value = config.value.copy(amoledMode = enabled)
    }

    override suspend fun setPaletteStyle(style: WenyanPaletteStyle) {
        config.value = config.value.copy(paletteStyle = style)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        config.value = config.value.copy(dynamicColor = enabled)
    }

    override suspend fun setSeedColor(color: Color) {
        config.value = config.value.copy(seedColor = color)
    }
}
