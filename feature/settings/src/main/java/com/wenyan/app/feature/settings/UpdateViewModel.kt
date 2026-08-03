package com.wenyan.app.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.UpdateCheckResult
import com.wenyan.app.core.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 检查更新 ViewModel。
 *
 * 管理 7 种 UI 状态：
 * - Idle：初始状态，用户尚未触发检查
 * - Checking：检查中，防重复点击
 * - Latest：已是最新版本
 * - UpdateAvailable：新版本可用，携带版本号/下载链接/更新说明
 * - Downloading：下载中，携带进度（0-100）
 * - DownloadComplete：下载完成，准备安装
 * - Error：检查/下载失败，携带错误信息
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

    /** 下载中，progress 为 0-100 整数百分比 */
    data class Downloading(val progress: Int) : UpdateUiState()

    /** 下载完成，准备安装 */
    data class DownloadComplete(val apkFile: File) : UpdateUiState()

    data class Error(val message: String) : UpdateUiState()
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /** 检查更新防重入锁 */
    private var isChecking = false

    /** 下载防重入锁 */
    private var isDownloading = false

    /** 下载 URL（在 UpdateAvailable 时保存，供下载用） */
    private var pendingDownloadUrl: String = ""

    /** 期望 APK sha256（来自 GitHub API 资产 digest；null 时跳过哈希校验） */
    private var pendingSha256: String? = null

    /** OkHttp 客户端（超时 30s，流式下载） */
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

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
                val currentVersion = try {
                    val pkgInfo = context.packageManager.getPackageInfo(
                        context.packageName, 0
                    )
                    pkgInfo.versionName ?: "0.0.0"
                } catch (e: Exception) {
                    "0.0.0"
                }
                val result = updateRepository.checkForUpdate(currentVersion)
                _uiState.value = when (result) {
                    is UpdateCheckResult.Latest ->
                        UpdateUiState.Latest(currentVersion = result.currentVersion)
                    is UpdateCheckResult.UpdateAvailable -> {
                        pendingDownloadUrl = result.downloadUrl
                        pendingSha256 = result.expectedSha256
                        UpdateUiState.UpdateAvailable(
                            latestVersion = result.latestVersion,
                            downloadUrl = result.downloadUrl,
                            releaseNotes = result.releaseNotes,
                        )
                    }
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
     * 软件内下载并安装 APK。
     *
     * 使用 OkHttp 流式下载到 cache 目录，下载完成后自动弹出安装界面。
     * 防重入：下载中再次调用直接返回。
     */
    fun downloadAndInstallApk() {
        if (isDownloading) return
        if (pendingDownloadUrl.isBlank()) return

        isDownloading = true
        _uiState.value = UpdateUiState.Downloading(progress = 0)

        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    // P1 修复（v0.9.28）：下载失败自动重试 1 次，
                    // 避免国内网络单次中断直接报错（重试前 downloadApk 会清空旧文件）。
                    try {
                        downloadApk(pendingDownloadUrl, pendingSha256)
                    } catch (first: Exception) {
                        downloadApk(pendingDownloadUrl, pendingSha256)
                    }
                }
                _uiState.value = UpdateUiState.DownloadComplete(apkFile = file)
                // 下载完成后自动触发安装
                installApk(file)
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("Unable to resolve host") == true ||
                        e.message?.contains("Failed to connect") == true ||
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                        "下载失败，请检查网络后重试"

                    e.message?.contains("403") == true ->
                        "下载被拒绝，请稍后再试"

                    e.message?.contains("No space left") == true ->
                        "存储空间不足，请清理后重试"

                    e.message?.contains("下载不完整") == true ||
                        e.message?.contains("校验失败") == true ->
                        "下载文件不完整，请重试"

                    else -> "下载失败：${e.message ?: "未知错误"}"
                }
                _uiState.value = UpdateUiState.Error(message = message)
            } finally {
                isDownloading = false
            }
        }
    }

    /**
     * 安装已下载完成的 APK（DownloadComplete 态）。
     *
     * v0.9.25 修复：此前"安装更新"按钮调用 downloadAndInstallApk() 会重新联网下载，
     * 已下载的包无法直接安装（离线时点击直接进 Error）。
     */
    fun installDownloadedApk() {
        val state = _uiState.value
        if (state is UpdateUiState.DownloadComplete) {
            installApk(state.apkFile)
        }
    }

    /**
     * 使用 OkHttp 下载 APK 到缓存目录。
     *
     * @param url APK 下载 URL
     * @return 下载完成的 File 对象
     */
    @Throws(Exception::class)
    private fun downloadApk(url: String, expectedSha256: String?): File {
        // 确保缓存目录存在
        val cacheDir = File(context.cacheDir, "apk")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        // 清理旧文件（含上次重试可能残留的不完整文件）
        cacheDir.listFiles()?.forEach { it.delete() }

        val file = File(cacheDir, "wenyan-update.apk")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Wenyan-Android-App")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("HTTP ${response.code}")
        }

        val body = response.body ?: throw RuntimeException("响应体为空")
        val contentLength = body.contentLength()
        val inputStream = body.byteStream()

        var totalBytesRead = 0L

        FileOutputStream(file).use { outputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var lastProgress = -1

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                // 更新进度（仅在有 Content-Length 时）
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt()
                    if (progress != lastProgress) {
                        lastProgress = progress
                        _uiState.value = UpdateUiState.Downloading(progress = progress)
                    }
                }
            }
            outputStream.flush()
        }

        // P1 修复（v0.9.28）：下载完整性校验。
        // 此前无任何校验，下载中断/串内容时把损坏文件直接交给安装器 → "应用文件存在问题"。
        // 校验 1：Content-Length 字节数对比（服务端声明了长度时必须一致）。
        if (contentLength > 0 && totalBytesRead != contentLength) {
            file.delete()
            throw RuntimeException(
                "下载不完整：预期 $contentLength 字节，实际 $totalBytesRead 字节",
            )
        }

        // 校验 2：sha256 摘要（GitHub API 资产 digest；降级路径无 digest 时跳过）。
        if (expectedSha256 != null) {
            val actual = file.sha256Hex()
            if (!actual.equals(expectedSha256, ignoreCase = true)) {
                file.delete()
                throw RuntimeException("下载校验失败：文件哈希不匹配")
            }
        }

        return file
    }

    /** 计算文件 SHA-256 十六进制摘要（用于 APK 完整性校验）。 */
    private fun File.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(readBytes())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 通过系统安装器安装 APK。
     *
     * 使用 FileProvider 提供文件 URI，避免 file:// URI 导出限制（Android 7+）。
     */
    private fun installApk(file: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    /**
     * 在浏览器中打开下载页面（备用方案）。
     */
    fun openDownloadPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * 重置到初始状态（用于错误后重试）。
     */
    fun resetState() {
        _uiState.value = UpdateUiState.Idle
        pendingDownloadUrl = ""
        isChecking = false
        isDownloading = false
    }

    override fun onCleared() {
        super.onCleared()
        // 清理下载的 APK 文件
        val cacheDir = File(context.cacheDir, "apk")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }
}