package com.wenyan.app.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.UpdateCheckResult
import com.wenyan.app.core.data.repository.UpdateRepository
import com.wenyan.app.feature.settings.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 检查更新 ViewModel。
 *
 * 管理 5 种 UI 状态：
 * - Idle：初始状态，用户尚未触发检查
 * - Checking：检查中，防重复点击
 * - Latest：已是最新版本
 * - UpdateAvailable：新版本可用，携带版本号/下载链接/更新说明
 * - Error：检查失败，携带错误信息
 */
sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data class Latest(val currentVersion: String) : UpdateUiState()
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String,
    ) : UpdateUiState()

    data class Error(val message: String) : UpdateUiState()
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /** 防重入锁 */
    private var isChecking = false

    /**
     * 检查更新。
     *
     * 防重复：检查中再次调用直接返回。
     * 防重入：用 isChecking 锁 + viewModelScope 单协程，避免并发请求。
     */
    fun checkForUpdate() {
        if (isChecking) return
        if (_uiState.value is UpdateUiState.Checking) return

        isChecking = true
        _uiState.value = UpdateUiState.Checking

        viewModelScope.launch {
            try {
                val currentVersion = BuildConfig.VERSION_NAME
                val result = updateRepository.checkForUpdate(currentVersion)
                _uiState.value = when (result) {
                    is UpdateCheckResult.Latest ->
                        UpdateUiState.Latest(currentVersion = result.currentVersion)
                    is UpdateCheckResult.UpdateAvailable ->
                        UpdateUiState.UpdateAvailable(
                            latestVersion = result.latestVersion,
                            downloadUrl = result.downloadUrl,
                            releaseNotes = result.releaseNotes,
                        )
                    is UpdateCheckResult.Error ->
                        UpdateUiState.Error(message = result.message)
                }
            } catch (e: Exception) {
                _uiState.value = UpdateUiState.Error(
                    message = "检查更新失败：${e.message ?: "未知错误"}",
                )
            } finally {
                isChecking = false
            }
        }
    }

    /**
     * 在浏览器中打开下载页面。
     */
    fun openDownloadPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}