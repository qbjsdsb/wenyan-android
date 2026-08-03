package com.wenyan.app.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [UpdateRepositoryImpl] 更新下载 URL 解析逻辑单元测试。
 *
 * 覆盖（v0.9.28 P1 修复——"应用文件存在问题"根因回归测试）：
 * - resolveDownloadUrl 有有效 APK 资产 → 使用资产 browser_download_url
 * - resolveDownloadUrl 资产为空（API 降级路径）→ 构造真实 APK 下载 URL，而非 tag 页面 HTML
 * - resolveDownloadUrl 资产 content_type 不匹配 → 构造真实 APK 下载 URL
 * - buildApkDownloadUrl 带 v / 不带 v 前缀统一输出 release.yml 固定命名
 * - compareVersions 语义化版本比较
 */
class UpdateRepositoryTest {

    private val impl = UpdateRepositoryImpl()

    // ============ resolveDownloadUrl（P1 修复核心） ============

    @Test
    fun `resolveDownloadUrl_有有效APK资产_返回资产下载URL`() {
        val assets = listOf(
            UpdateRepositoryImpl.GitHubAsset(
                name = "wenyan-latest.apk",
                browser_download_url =
                "https://github.com/qbjsdsb/wenyan-android/releases/download/v0.9.27/wenyan-latest.apk",
                content_type = "application/vnd.android.package-archive",
            ),
            UpdateRepositoryImpl.GitHubAsset(
                name = "wenyan-v0.9.27.apk",
                browser_download_url =
                "https://github.com/qbjsdsb/wenyan-android/releases/download/v0.9.27/wenyan-v0.9.27.apk",
                content_type = "application/vnd.android.package-archive",
            ),
        )

        val url = impl.resolveDownloadUrl(assets, "v0.9.27")

        // 主路径：应返回第一个有效 APK 资产的下载 URL（含 releases/download，非 tag 页面）
        assertTrue(url.endsWith(".apk"))
        assertTrue(url.contains("/releases/download/"))
        assertFalse(url.contains("/releases/tag/"))
        assertEquals(
            "https://github.com/qbjsdsb/wenyan-android/releases/download/v0.9.27/wenyan-latest.apk",
            url,
        )
    }

    @Test
    fun `resolveDownloadUrl_资产为空_构造真实APK下载URL而非tag页面`() {
        // 模拟 API 降级路径：api.github.com 不可达时 fetchLatestTagFromFallback 返回 assets=emptyList
        val assets: List<UpdateRepositoryImpl.GitHubAsset> = emptyList()

        val url = impl.resolveDownloadUrl(assets, "v0.9.27")

        // P1 修复：必须指向真实 APK（releases/download），绝不能 fallback 到 tag 页面 HTML
        assertEquals(
            "https://github.com/qbjsdsb/wenyan-android/releases/download/v0.9.27/wenyan-v0.9.27.apk",
            url,
        )
        assertFalse("不得包含 /releases/tag/（HTML 页面）", url.contains("/releases/tag/"))
    }

    @Test
    fun `resolveDownloadUrl_资产content_type不匹配_构造真实APK下载URL`() {
        // 资产存在但不是 APK（如 source code zip / 错误 content_type）→ 构造真实 APK URL
        val assets = listOf(
            UpdateRepositoryImpl.GitHubAsset(
                name = "source.zip",
                browser_download_url = "https://github.com/qbjsdsb/wenyan-android/archive/refs/tags/v0.9.27.zip",
                content_type = "application/zip",
            ),
        )

        val url = impl.resolveDownloadUrl(assets, "v0.9.27")

        assertEquals(
            "https://github.com/qbjsdsb/wenyan-android/releases/download/v0.9.27/wenyan-v0.9.27.apk",
            url,
        )
    }

    @Test
    fun `resolveDownloadUrl_资产URL为空_构造真实APK下载URL`() {
        val assets = listOf(
            UpdateRepositoryImpl.GitHubAsset(
                name = "wenyan-v0.9.27.apk",
                browser_download_url = "",
                content_type = "application/vnd.android.package-archive",
            ),
        )

        val url = impl.resolveDownloadUrl(assets, "v0.9.27")

        assertEquals(
            "https://github.com/qbjsdsb/wenyan-android/releases/download/v0.9.27/wenyan-v0.9.27.apk",
            url,
        )
    }

    // ============ buildApkDownloadUrl ============

    @Test
    fun `buildApkDownloadUrl_带v前缀tag_输出固定命名APK链接`() {
        assertEquals(
            "https://github.com/qbjsdsb/wenyan-android/releases/download/v0.9.27/wenyan-v0.9.27.apk",
            impl.buildApkDownloadUrl("v0.9.27"),
        )
    }

    @Test
    fun `buildApkDownloadUrl_不带v前缀tag_同样输出带v命名APK链接`() {
        assertEquals(
            "https://github.com/qbjsdsb/wenyan-android/releases/download/v0.9.27/wenyan-v0.9.27.apk",
            impl.buildApkDownloadUrl("0.9.27"),
        )
    }

    // ============ compareVersions ============

    @Test
    fun `compareVersions_新版本大于旧版本_返回正数`() {
        assertTrue(impl.compareVersions("0.9.27", "0.9.26") > 0)
        assertTrue(impl.compareVersions("0.10.0", "0.9.27") > 0)
    }

    @Test
    fun `compareVersions_版本相等_返回0`() {
        assertEquals(0, impl.compareVersions("0.9.27", "0.9.27"))
    }

    @Test
    fun `compareVersions_旧版本小于新版本_返回负数`() {
        assertTrue(impl.compareVersions("0.9.26", "0.9.27") < 0)
        assertTrue(impl.compareVersions("0.9.27", "0.10.0") < 0)
    }

    @Test
    fun `compareVersions_不同长度版本号_按补零比较`() {
        assertTrue(impl.compareVersions("0.9", "0.9.1") < 0)
        assertTrue(impl.compareVersions("0.9.1", "0.9") > 0)
    }
}
