package com.wenyan.app.core.data

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ThemeRepository
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主题状态 ViewModel。
 *
 * 将 [ThemeRepository] 的 [ThemeConfig] Flow 转换为 [StateFlow] 供 Compose 消费。
 *
 * 放在 core:data 模块中以避免 :app → :feature:settings → :app 循环依赖。
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val themeConfig: StateFlow<ThemeConfig> = themeRepository.themeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig())

    fun setColorMode(mode: ColorMode) = launchSafely {
        themeRepository.setColorMode(mode)
    }

    fun setAmoledMode(enabled: Boolean) = launchSafely {
        themeRepository.setAmoledMode(enabled)
    }

    fun setPaletteStyle(style: WenyanPaletteStyle) = launchSafely {
        themeRepository.setPaletteStyle(style)
    }

    fun setDynamicColor(enabled: Boolean) = launchSafely {
        themeRepository.setDynamicColor(enabled)
    }

    fun setSeedColor(color: Color) = launchSafely {
        themeRepository.setSeedColor(color)
    }

    /**
     * 安全启动协程：捕获 DataStore IOException 等异常，避免崩溃全局 ThemeViewModel。
     *
     * P0-V1 修正：原 5 处 viewModelScope.launch 全无 try/catch，
     * DataStore 抛 IOException 时 App 崩溃（ThemeViewModel 是全局单例）。
     * CancellationException 必须重新抛出，不吞协程取消。
     */
    private fun launchSafely(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 静默处理，避免 DataStore IO 异常崩溃全局主题 ViewModel
        }
    }
}
