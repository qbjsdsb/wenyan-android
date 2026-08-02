package com.wenyan.app.feature.settings

import androidx.compose.ui.res.stringResource
import com.wenyan.app.feature.settings.R

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCard

/**
 * 检查更新页面。
 *
 * 7 种状态：
 * - Idle：初始状态，显示"检查更新"按钮
 * - Checking：检查中，显示加载动画
 * - Latest：已是最新版本，显示绿色勾
 * - UpdateAvailable：新版本可用，显示版本信息和下载按钮
 * - Downloading：下载中，显示进度条
 * - DownloadComplete：下载完成，显示安装按钮
 * - Error：检查/下载失败，显示错误信息和重试按钮
 *
 * 进入页面时自动触发检查（LaunchedEffect 仅执行一次）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCheckScreen(
    onBack: () -> Unit,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    // 进入页面自动检查更新
    LaunchedEffect(Unit) {
        viewModel.checkForUpdate()
    }

    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.update_check_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth.compact)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Spacer(modifier = Modifier.height(Spacing.xxl))

                // ── 当前版本展示 ──
                TonalCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Text(
                            text = "文研 App",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "当前版本 v$currentVersionName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // 显示构建类型（debug/release），帮助用户理解签名差异
                        Text(
                            text = if (BuildConfig.DEBUG) "Debug 构建（仅开发测试）" else "Release 构建",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (BuildConfig.DEBUG)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // ── 状态区域（AnimatedContent 过渡动画） ──
                // 使用基于状态类型的稳定 key，避免进度更新时触发过渡动画（闪烁）
                val stateKey = when (uiState) {
                    is UpdateUiState.Idle -> "idle"
                    is UpdateUiState.Checking -> "checking"
                    is UpdateUiState.Latest -> "latest"
                    is UpdateUiState.UpdateAvailable -> "update_available"
                    is UpdateUiState.Downloading -> "downloading"
                    is UpdateUiState.DownloadComplete -> "download_complete"
                    is UpdateUiState.Error -> "error"
                }
                AnimatedContent(
                    targetState = stateKey,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "update_state",
                ) { key ->
                    // v0.9.25 修复：content lambda 使用参数 key 分发，而非闭包直接读 uiState。
                    // 原实现两帧过渡时都渲染"最新状态"（crossfade 失效），
                    // 且 Downloading→DownloadComplete 等状态转换时旧帧会读错状态。
                    // 用 as? 安全 cast，过渡期间旧帧数据不可用时渲染空（300ms 内可接受）。
                    when (key) {
                        "idle" -> IdleContent(onCheck = viewModel::checkForUpdate)
                        "checking" -> CheckingContent()
                        "latest" -> (uiState as? UpdateUiState.Latest)?.let {
                            LatestContent(currentVersion = it.currentVersion)
                        }
                        "update_available" -> (uiState as? UpdateUiState.UpdateAvailable)?.let {
                            UpdateAvailableContent(
                                latestVersion = it.latestVersion,
                                releaseNotes = it.releaseNotes,
                                onDownload = viewModel::downloadAndInstallApk,
                                onOpenInBrowser = { viewModel.openDownloadPage(it.downloadUrl) },
                            )
                        }
                        "downloading" -> (uiState as? UpdateUiState.Downloading)?.let {
                            DownloadingContent(progress = it.progress)
                        }
                        "download_complete" -> (uiState as? UpdateUiState.DownloadComplete)?.let {
                            DownloadCompleteContent(onInstall = viewModel::installDownloadedApk)
                        }
                        "error" -> (uiState as? UpdateUiState.Error)?.let {
                            ErrorContent(
                                message = it.message,
                                onRetry = {
                                    viewModel.resetState()
                                    viewModel.checkForUpdate()
                                },
                            )
                        }
                        else -> IdleContent(onCheck = viewModel::checkForUpdate)
                    }
                }
            }
        }
    }
}

// ── 各状态的内容组件 ──

@Composable
private fun IdleContent(onCheck: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = "点击下方按钮检查是否有新版本可用",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Button(
            onClick = onCheck,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.update_check_button))
        }
    }
}

@Composable
private fun CheckingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
        )
        Text(
            text = "正在检查更新…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LatestContent(currentVersion: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "已是最新版本",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "v$currentVersion",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "当前版本已是最新，无需更新",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UpdateAvailableContent(
    latestVersion: String,
    releaseNotes: String,
    onDownload: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // 版本信息卡片
        TonalCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(40.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "新版本可用",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = latestVersion,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        // 更新说明
        if (releaseNotes.isNotBlank() && releaseNotes != "暂无更新说明") {
            TonalCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = "更新说明",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // 软件内更新按钮（主操作）
        Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.update_download_in_app))
        }

        // 浏览器下载（备用链接）
        OutlinedButton(
            onClick = onOpenInBrowser,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.update_download_browser))
        }
    }
}

@Composable
private fun DownloadingContent(progress: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        TonalCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "正在下载更新…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // 进度条
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )

                // 进度百分比
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = "下载完成后将自动安装，请勿离开此页面",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DownloadCompleteContent(onInstall: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        TonalCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "下载完成！",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "点击下方按钮开始安装",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Button(
            onClick = onInstall,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.InstallMobile,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.update_install))
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = "检查更新失败",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.update_retry))
        }
    }
}