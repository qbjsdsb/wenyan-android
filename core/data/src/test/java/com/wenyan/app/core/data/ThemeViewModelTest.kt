package com.wenyan.app.core.data

import app.cash.turbine.test
import com.wenyan.app.core.data.repository.ThemeRepository
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 启动一个后台收集器以激活 stateIn(WhileSubscribed) 的上游收集。
     * 没有订阅者时，stateIn 不会从 repository 收集数据，.value 始终返回初始值。
     * backgroundScope 由 TestScope 提供，测试结束自动取消。
     */

    @Test
    fun `initial state is default ThemeConfig`() = runTest(testDispatcher) {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        assertEquals(ThemeConfig(), viewModel.themeConfig.value)
    }

    @Test
    fun `setColorMode updates state to DARK`() = runTest(testDispatcher) {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        backgroundScope.launch { viewModel.themeConfig.collect { } }
        advanceUntilIdle()
        viewModel.setColorMode(ColorMode.DARK)
        advanceUntilIdle()
        assertEquals(ColorMode.DARK, viewModel.themeConfig.value.colorMode)
    }

    @Test
    fun `setAmoledMode updates state to true`() = runTest(testDispatcher) {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        backgroundScope.launch { viewModel.themeConfig.collect { } }
        advanceUntilIdle()
        viewModel.setAmoledMode(true)
        advanceUntilIdle()
        assertEquals(true, viewModel.themeConfig.value.amoledMode)
    }

    @Test
    fun `setPaletteStyle updates state to EXPRESSIVE`() = runTest(testDispatcher) {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        backgroundScope.launch { viewModel.themeConfig.collect { } }
        advanceUntilIdle()
        viewModel.setPaletteStyle(WenyanPaletteStyle.EXPRESSIVE)
        advanceUntilIdle()
        assertEquals(WenyanPaletteStyle.EXPRESSIVE, viewModel.themeConfig.value.paletteStyle)
    }

    @Test
    fun `themeConfig emits updates`() = runTest(testDispatcher) {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        viewModel.themeConfig.test {
            assertEquals(ThemeConfig(), awaitItem())
            viewModel.setDynamicColor(false)
            advanceUntilIdle()
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
