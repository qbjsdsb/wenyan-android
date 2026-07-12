package com.wenyan.app.feature.aiassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.ai.SocraticStage
import com.wenyan.app.core.designsystem.component.ContentSourceBadge
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar

/**
 * AI 助手界面（阶段4增强）。
 *
 * 增强点：
 * - 内容来源标注（AI生成 / 教材原文 / OCR识别）
 * - 引用来源可溯源展示（sourceFile + sourcePage）
 * - 错误提示（Snackbar）
 * - 死记硬背提示（顶部横幅）
 * - 离线状态提示
 * - 清空对话按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onNavigateToApiConfig: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: AiAssistantViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    // 错误提示 → Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // 新消息时自动滚动到底部
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "AI助手",
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "设置",
                        )
                    }
                    IconButton(onClick = onNavigateToApiConfig) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "API 配置",
                        )
                    }
                    if (!uiState.isAvailable) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "离线",
                            modifier = Modifier.padding(end = Spacing.xs),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    IconButton(
                        onClick = viewModel::clearMessages,
                        enabled = uiState.messages.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "清空对话",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            InputBar(
                text = uiState.inputText,
                isLoading = uiState.isLoading,
                onTextChanged = viewModel::updateInput,
                onSend = { viewModel.sendMessage(uiState.inputText) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            // 死记硬背提示横幅
            uiState.roteWarning?.let { warning ->
                RoteWarningBanner(
                    warning = warning,
                    onDismiss = viewModel::clearRoteWarning,
                )
            }

            if (uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "向AI助手提问，它会引导你思考而非直接给答案",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(uiState.messages) { message ->
                        MessageBubble(message)
                    }
                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 输入栏 ──────────────────────────────────────────────────────

@Composable
private fun InputBar(
    text: String,
    isLoading: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier.weight(1f),
            placeholder = { Text("输入你的问题……") },
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isLoading,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "发送",
            )
        }
    }
}

// ── 消息气泡 ────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: AiMessage) {
    val isUser = message.role == AiRole.USER
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (isUser) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh
    val contentColor = if (isUser) colorScheme.onPrimaryContainer else colorScheme.onSurface
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        start = if (isUser) 48.dp else 0.dp,
                        end = if (isUser) 0.dp else 48.dp,
                    )
                    .clip(MaterialTheme.shapes.large)
                    .background(containerColor)
                    .padding(Spacing.md),
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
            }
        }

        // 内容来源标注 + 引用列表（仅 AI 消息）
        if (!isUser) {
            ContentSourceBadge(
                contentSource = message.contentSource,
                stageLabel = formatStageLabel(message.stage),
                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs),
            )
            ReferencesList(message)
        }
    }
}

// ── 内容来源标注 ────────────────────────────────────────────────

/** 将苏格拉底阶段映射为标签文本（阶段优先于 contentSource） */
private fun formatStageLabel(stage: SocraticStage?): String? = when (stage) {
    SocraticStage.ANALYZE -> "论证分析 · AI引导"
    SocraticStage.SUGGEST -> "改进建议 · AI引导"
    SocraticStage.SHOW_SAMPLE -> "参考范文 · AI生成"
    null -> null
}

// ── 引用来源列表 ────────────────────────────────────────────────

@Composable
private fun ReferencesList(message: AiMessage) {
    if (message.references.isEmpty()) return

    Column(
        modifier = Modifier
            .padding(start = Spacing.xs, top = Spacing.xs)
            .fillMaxWidth(0.8f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(Spacing.sm),
    ) {
        Text(
            text = "引用来源：",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        message.references.forEachIndexed { index, ref ->
            Text(
                text = "${index + 1}. 《${ref.sourceFile}》P${ref.sourcePage}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = Spacing.xs),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

// ── 死记硬背提示横幅 ────────────────────────────────────────────

@Composable
private fun RoteWarningBanner(
    warning: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = warning,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "知道了",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                .clickable(onClick = onDismiss)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}
