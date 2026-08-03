package com.wenyan.app.core.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        /** 期望 APK sha256（GitHub API 资产 digest；降级路径为 null 时跳过校验） */
        val expectedSha256: String? = null,
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
        /** GitHub API（首选）— 国内可能访问不稳定，有备用方案 */
        private const val GITHUB_API_URL =
            "https://api.github.com/repos/qbjsdsb/wenyan-android/releases/latest"
        /** GitHub Releases 页面（备用）— 国内通常可访问，通过重定向获取版本号 */
        private const val GITHUB_RELEASES_URL =
            "https://github.com/qbjsdsb/wenyan-android/releases/latest"
        /** Release tag 页面（旧 fallback，仅作兜底） */
        private const val GITHUB_DOWNLOAD_BASE =
            "https://github.com/qbjsdsb/wenyan-android/releases/tag"
        /** Release 资产下载根路径（releases/download/{tag}/wenyan-{tag}.apk） */
        private const val GITHUB_DOWNLOAD_URL_BASE =
            "https://github.com/qbjsdsb/wenyan-android/releases/download"
        /** 请求超时（毫秒） */
        private const val CONNECT_TIMEOUT = 8_000
        private const val READ_TIMEOUT = 8_000
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
    internal data class GitHubAsset(
        val name: String = "",
        val browser_download_url: String = "",
        val content_type: String = "",
        /** GitHub API 资产摘要，形如 "sha256:1843e1a9..."（v0.9.27 起可用） */
        val digest: String? = null,
    )

    override suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult {
        val current = currentVersion.removePrefix("v")
        return try {
            // 优先尝试 GitHub API
            val release = withContext(Dispatchers.IO) {
                try {
                    fetchLatestReleaseFromApi()
                } catch (e: Exception) {
                    // API 失败（国内可能无法访问 api.github.com），
                    // 降级到 github.com Releases 页面，通过重定向获取版本号
                    fetchLatestTagFromFallback()
                }
            }

            val latestTag = release.tag_name.removePrefix("v")
            val comparison = compareVersions(latestTag, current)

            when {
                comparison > 0 -> {
                    // P1 修复（v0.9.28）：降级路径（api.github.com 不可达）assets 为空，
                    // 旧逻辑 fallback 到 release tag 页面（HTML），App 下载网页当 APK 导致
                    // "应用文件存在问题"。统一走 resolveDownloadUrl：有资产用资产 URL，
                    // 无资产按 release.yml 固定命名规则构造真实 APK 下载 URL。
                    val downloadUrl = resolveDownloadUrl(release.assets, release.tag_name)
                    val expectedSha256 = release.assets
                        .firstOrNull { asset ->
                            asset.name.endsWith(".apk", ignoreCase = true) &&
                                asset.content_type == "application/vnd.android.package-archive"
                        }
                        ?.digest
                        ?.removePrefix("sha256:")
                        ?.takeIf { it.isNotBlank() && it.length == 64 }

                    UpdateCheckResult.UpdateAvailable(
                        latestVersion = release.tag_name,
                        downloadUrl = downloadUrl,
                        releaseNotes = release.body ?: "暂无更新说明",
                        expectedSha256 = expectedSha256,
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
     * 通过 GitHub API 获取最新 release 信息。
     *
     * @throws Exception API 请求或解析失败
     */
    @Throws(Exception::class)
    private fun fetchLatestReleaseFromApi(): GitHubRelease {
        val url = URL(GITHUB_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        connection.setRequestProperty("User-Agent", "Wenyan-Android-App")
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
     * 备用方案：通过 GitHub Releases 页面重定向获取最新版本号。
     *
     * 国内网络环境下 `api.github.com` 可能不可达，但 `github.com` 通常可访问。
     * 访问 `/releases/latest` 会 302 重定向到 `/releases/tag/vX.Y.Z`，
     * 从重定向 URL 中提取版本号。
     *
     * @throws Exception 请求或解析失败
     */
    @Throws(Exception::class)
    private fun fetchLatestTagFromFallback(): GitHubRelease {
        val url = URL(GITHUB_RELEASES_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        connection.setRequestProperty("User-Agent", "Wenyan-Android-App")
        // 不自动跟进重定向，手动读取 Location 头
        connection.instanceFollowRedirects = false

        return try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_MOVED_TEMP &&
                responseCode != HttpURLConnection.HTTP_MOVED_PERM &&
                responseCode != HttpURLConnection.HTTP_OK
            ) {
                throw RuntimeException("HTTP $responseCode")
            }

            // 从 Location 头提取版本号
            val location = connection.getHeaderField("Location") ?: ""
            val tagName = if (location.isNotBlank()) {
                location.substringAfterLast("/")
            } else {
                throw RuntimeException("无法获取最新版本信息")
            }

            if (tagName.isBlank()) {
                throw RuntimeException("无法获取最新版本信息")
            }

            GitHubRelease(
                tag_name = tagName,
                body = null,
                html_url = "$GITHUB_DOWNLOAD_BASE/$tagName",
                assets = emptyList(),
            )
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 从 release 资产中解析 APK 下载 URL。
     *
     * - 资产列表含有效 APK（.apk 且 content_type 正确）→ 返回其 browser_download_url；
     * - 否则按 release.yml 固定命名规则构造真实 APK 下载 URL
     *   （P1 修复：旧逻辑 fallback 到 tag 页面 HTML，App 下载网页当 APK 导致安装失败）。
     *
     * @param assets release 资产列表（降级路径为空）
     * @param tagName release tag（如 "v0.9.27"）
     */
    internal fun resolveDownloadUrl(assets: List<GitHubAsset>, tagName: String): String {
        val apkAsset = assets.firstOrNull { asset ->
            asset.name.endsWith(".apk", ignoreCase = true) &&
                asset.content_type == "application/vnd.android.package-archive"
        }
        return apkAsset?.browser_download_url
            .takeIf { !it.isNullOrBlank() }
            ?: buildApkDownloadUrl(tagName)
    }

    /**
     * 构造 Release APK 资产下载 URL。
     *
     * 与 release.yml 固定命名规则一致：`releases/download/vX.Y.Z/wenyan-vX.Y.Z.apk`。
     * 在 API 降级路径（assets 为空）时使用，避免 fallback 到 HTML 页面导致下载损坏。
     *
     * @param tagName release tag（如 "v0.9.27" 或 "0.9.27"）
     */
    internal fun buildApkDownloadUrl(tagName: String): String {
        val tag = tagName.removePrefix("v")
        return "$GITHUB_DOWNLOAD_URL_BASE/v$tag/wenyan-v$tag.apk"
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