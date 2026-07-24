package com.wenyan.app.core.designsystem.theme

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主题状态 ViewModel。
 *
 * 将 [ThemeRepository] 的 [ThemeConfig] Flow 转换为 [StateFlow] 供 Compose 消费。
 *
 * P1-8 修复：从 core/data 迁入 core/designsystem。
 * 原注释"放在 core/data 模块中以避免 :app → :feature:settings → :app 循环依赖"
 * 已不成立——迁入 designsystem 后，:app / :feature:settings 通过 :core:designsystem
 * 共享 ThemeViewModel，仍无循环依赖，且消除了 core/data → core/designsystem 反向依赖。
 *
 * v0.8.4 修复（P2）：原 launchSafely 静默吞所有非 CancellationException 异常，
 * 无日志、无 UI 反馈。生产环境主题保存失败用户无感知且难以排查。
 * 现添加 Log.w 日志 + errorEvents SharedFlow，UI 可订阅展示 Snackbar。
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val themeConfig: StateFlow<ThemeConfig> = themeRepository.themeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig())

    // v0.8.4：主题操作错误事件流，UI 可订阅展示 Snackbar
    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

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
     *
     * v0.8.4：不再静默吞异常，添加日志 + 错误事件流。
     */
    private fun launchSafely(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "主题操作失败: ${e.message}", e)
            _errorEvents.tryEmit("设置未保存：${e.message ?: "存储异常"}")
        }
    }

    private companion object {
        private const val TAG = "ThemeViewModel"
    }
}
