package com.wenyan.app.core.data.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 检查更新结果。
 */
sealed class UpdateCheckResult {
    /** 已是最新版本 */
    data class Latest(val currentVersion: String) : UpdateCheckResult()

    /** 有新版本可用 */
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String,
    ) : UpdateCheckResult()

    /** 检查失败 */
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * 更新检查仓库 — 从 GitHub Releases API 获取最新版本信息。
 *
 * 使用 JDK 内置 HttpURLConnection（无需额外网络依赖），
 * 配合 kotlinx.serialization 解析 JSON 响应。
 *
 * 注意：GitHub API 未认证时每小时限 60 次请求，个人使用绰绰有余。
 */
interface UpdateRepository {

    /**
     * 检查是否有新版本可用。
     *
     * @param currentVersion 当前版本号（如 "0.9.10"）
     * @return [UpdateCheckResult] 检查结果
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult
}

@Singleton
class UpdateRepositoryImpl @Inject constructor() : UpdateRepository {

    private companion object {
        private const val TAG = "UpdateRepository"
        private const val GITHUB_API_URL =
            "https://api.github.com/repos/qbjsdsb/wenyan-android/releases/latest"
        private const val GITHUB_RELEASES_URL =
            "https://github.com/qbjsdsb/wenyan-android/releases/latest"
        /** 请求超时（毫秒） */
        private const val CONNECT_TIMEOUT = 10_000
        private const val READ_TIMEOUT = 10_000
    }

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class GitHubRelease(
        val tag_name: String,
        val body: String? = null,
        val html_url: String = "",
        val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    private data class GitHubAsset(
        val name: String = "",
        val browser_download_url: String = "",
        val content_type: String = "",
    )

    override suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult {
        return try {
            val release = fetchLatestRelease()
            val latestTag = release.tag_name.removePrefix("v")
            val current = currentVersion.removePrefix("v")

            val comparison = compareVersions(latestTag, current)
            when {
                comparison > 0 -> {
                    // 找 APK 下载链接，找不到则用 releases 页面
                    val apkAsset = release.assets.firstOrNull { asset ->
                        asset.name.endsWith(".apk", ignoreCase = true) &&
                            asset.content_type == "application/vnd.android.package-archive"
                    }
                    val downloadUrl = apkAsset?.browser_download_url
                        .takeIf { !it.isNullOrBlank() }
                        ?: release.html_url.ifBlank { GITHUB_RELEASES_URL }

                    UpdateCheckResult.UpdateAvailable(
                        latestVersion = release.tag_name,
                        downloadUrl = downloadUrl,
                        releaseNotes = release.body ?: "暂无更新说明",
                    )
                }

                comparison == 0 -> UpdateCheckResult.Latest(currentVersion = current)
                else -> UpdateCheckResult.Error(
                    message = "当前版本 ($currentVersion) 高于 GitHub 最新版 ($latestTag)，请确认",
                )
            }
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("Unable to resolve host") == true ||
                    e.message?.contains("Failed to connect") == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                    "网络连接失败，请检查网络后重试"

                e.message?.contains("403") == true ->
                    "GitHub API 请求频率超限，请稍后再试"

                e.message?.contains("404") == true ->
                    "未找到版本信息，请确认仓库地址正确"

                e.message?.contains("JSON") == true ||
                    e.message?.contains("kotlinx.serialization") == true ->
                    "解析版本信息失败，请稍后再试"

                else -> "检查更新失败：${e.message ?: "未知错误"}"
            }
            UpdateCheckResult.Error(message)
        }
    }

    /**
     * 调用 GitHub Releases API 获取最新 release 信息。
     */
    @Throws(Exception::class)
    private fun fetchLatestRelease(): GitHubRelease {
        val url = URL(GITHUB_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        // GitHub API 需要 User-Agent
        connection.setRequestProperty("User-Agent", "Wenyan-Android-App")
        // 接受 JSON 响应
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        return try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("$responseCode")
            }
            val body = connection.inputStream.bufferedReader().readText()
            json.decodeFromString<GitHubRelease>(body)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 语义化版本比较。
     *
     * @return 正数：version1 > version2；0：相等；负数：version1 < version2
     */
    internal fun compareVersions(version1: String, version2: String): Int {
        val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val v1 = parts1.getOrElse(i) { 0 }
            val v2 = parts2.getOrElse(i) { 0 }
            if (v1 != v2) return v1 - v2
        }
        return 0
    }
}