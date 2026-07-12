package com.wenyan.app

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ThemeRepository
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主题状态 ViewModel。
 *
 * 将 [ThemeRepository] 的 [ThemeConfig] Flow 转换为 [StateFlow] 供 Compose 消费。
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val themeConfig: StateFlow<ThemeConfig> = themeRepository.themeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig())

    fun setColorMode(mode: ColorMode) = viewModelScope.launch {
        themeRepository.setColorMode(mode)
    }

    fun setAmoledMode(enabled: Boolean) = viewModelScope.launch {
        themeRepository.setAmoledMode(enabled)
    }

    fun setPaletteStyle(style: WenyanPaletteStyle) = viewModelScope.launch {
        themeRepository.setPaletteStyle(style)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        themeRepository.setDynamicColor(enabled)
    }

    fun setSeedColor(color: Color) = viewModelScope.launch {
        themeRepository.setSeedColor(color)
    }
}
