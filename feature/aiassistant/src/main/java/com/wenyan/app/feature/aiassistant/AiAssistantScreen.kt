package com.wenyan.app.feature.aiassistant

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * v0.6 改动：
 * - 从顶级 Tab 降为子路由（Push/Pop slide），由 4 个主屏 TopBar SmartToy 图标进入
 * - 移除 onNavigateToSettings（设置已是底部 Tab，无需从 AI 助手跳转）
 * - actions 重构：CloudOff 改为可点击 IconButton（跳转 ApiConfig），
 *   MoreVert 改为 DropdownMenu 溢出菜单（包含"API 配置"）
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
    viewModel: AiAssistantViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )
    // v0.6：MoreVert 溢出菜单展开状态
    var showOverflowMenu by remember { mutableStateOf(false) }

    // 错误提示 → Snackbar
    // NF-UC4 修复：原 LaunchedEffect 在 Composable 离开时 showSnackbar 协程被取消，
    // clearError() 不执行 → 下次进入时 errorMessage 仍非空，错误消息重复展示。
    // 改为：先 clearError() 再 showSnackbar，确保 errorMessage 立即清空，
    // 即使 showSnackbar 被取消也不影响状态清理。
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            viewModel.clearError()
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short,
            )
        }
    }

    // 新消息时自动滚动到底部
    // NF-UC3 修复：原 LaunchedEffect(messages.size) 无条件滚动到底部，
    // 用户上滑阅读历史消息时被新消息强制拉回底部，打断阅读。
    // 改为：仅当用户已在底部附近（最后一个可见 item 索引 >= 总数-2）时才自动滚动。
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= uiState.messages.size - 2
        }
    }
    LaunchedEffect(uiState.messages.size, isAtBottom) {
        if (uiState.messages.isNotEmpty() && isAtBottom) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "AI助手",
                actions = {
                    // v0.6：CloudOff 改为可点击 IconButton，直接跳转 ApiConfig 修复离线状态
                    if (!uiState.isAvailable) {
                        IconButton(onClick = onNavigateToApiConfig) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "AI 服务不可用，点击配置",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
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
                    // v0.6：MoreVert 改为 DropdownMenu 溢出菜单
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多",
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("API 配置") },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToApiConfig()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
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

            Crossfade(
                targetState = uiState.messages.isEmpty(),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "ai_state",
                modifier = Modifier.fillMaxSize(),
            ) { isEmpty ->
                if (isEmpty) {
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
                        // P2-LAZY-1 修正：混合列表加 contentType，帮助 LazyList 复用 item cache
                        // 消息项 contentType="message"，加载指示器 contentType="loading"
                        items(items = uiState.messages, key = { it.id }, contentType = { "message" }) { message ->
                            MessageBubble(message, modifier = Modifier.animateItem())
                        }
                        if (uiState.isLoading) {
                            item(key = "loading", contentType = "loading") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    WenyanLoadingIndicator()
                                }
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
private fun MessageBubble(
    message: AiMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == AiRole.USER
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (isUser) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh
    val contentColor = if (isUser) colorScheme.onPrimaryContainer else colorScheme.onSurface
    Column(
        modifier = modifier.fillMaxWidth(),
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
                // NF-UA2 修复：原触控目标 ~28dp（padding sm+xs），低于 WCAG 48dp 标准。
                // defaultMinSize 强制最小 48dp 触控区域，手指粗用户也能准确点击。
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                .clickable(role = Role.Button, onClick = onDismiss)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}
