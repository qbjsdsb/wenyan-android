package com.wenyan.app.feature.aiassistant

import androidx.compose.ui.res.stringResource
import com.wenyan.app.feature.aiassistant.R

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.foundation.layout.Spacer
import com.wenyan.app.core.ai.recall.QuestionType
import com.wenyan.app.core.ai.recall.RecallRating
import com.wenyan.app.core.ai.recall.RecallResult
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
import com.wenyan.app.core.designsystem.component.MaxContentWidth
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
    onBack: () -> Unit = {},
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
    // P0-3 修复：清空对话确认弹窗状态。原实现点击清空按钮直接清空，
    // 误触即丢失全部对话不可恢复。现加二次确认 Dialog。
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    // P0 v0.7.2: 学习工具 Dialog 模式(null=不显示)
    var showLearningToolDialog by remember { mutableStateOf<LearningToolMode?>(null) }

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
                // v0.9.30 打磨：长错误信息用 Long 时长，Short 会被截断
                duration = SnackbarDuration.Long,
            )
        }
    }

    // 新消息时自动滚动到底部
    // NF-UC3 修复：原 LaunchedEffect(messages.size) 无条件滚动到底部，
    // 用户上滑阅读历史消息时被新消息强制拉回底部，打断阅读。
    // 改为：仅当用户已在底部附近（最后一个可见 item 索引 >= 总数-2）时才自动滚动。
    // v0.9.25 修复：流式输出时 streamingContent 逐字更新但 messages.size 不变，
    // 原 LaunchedEffect 不会触发滚动，AI 长回复在可视区外增长看不到。
    // 加入 streamingContent 作为 key，并在流式时滚动到 streaming 项。
    val isAtBottom by remember {
        derivedStateOf {
            val hasStreaming = uiState.isLoading && uiState.streamingContent != null
            val lastItemIndex = uiState.messages.size - 1 + if (hasStreaming) 1 else 0
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= lastItemIndex - 1
        }
    }
    LaunchedEffect(uiState.messages.size, isAtBottom, uiState.streamingContent) {
        if (uiState.messages.isNotEmpty() && isAtBottom) {
            val hasStreaming = uiState.isLoading && uiState.streamingContent != null
            val target = uiState.messages.size - 1 + if (hasStreaming) 1 else 0
            if (hasStreaming) {
                // 流式高频更新：瞬时滚动避免动画频繁重启抖动
                listState.scrollToItem(target)
            } else {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = stringResource(R.string.ai_title),
                // P0-4 修复：子路由加 onBack，与其他子路由（ApiConfig/KnowledgePointDetail）契约一致
                onBack = onBack,
                actions = {
                    // v0.6：CloudOff 改为可点击 IconButton，直接跳转 ApiConfig 修复离线状态
                    if (!uiState.isAvailable) {
                        IconButton(onClick = onNavigateToApiConfig) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = stringResource(R.string.ai_unavailable),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    IconButton(
                        // P0-3 修复：点击清空按钮弹确认框，而非直接清空
                        onClick = { showClearConfirmDialog = true },
                        enabled = uiState.messages.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.ai_clear_chat),
                        )
                    }
                    // NF-PP6 Wave 3.1: 新建对话按钮(切换到新对话,保留历史)
                    // v0.8.3 修复：messages 为空时已是新对话，按钮 disable 避免无效点击
                    // 与"清空对话"按钮的 enabled 逻辑一致
                    IconButton(
                        onClick = viewModel::startNewConversation,
                        enabled = uiState.messages.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.ai_new_chat),
                        )
                    }
                    // v0.6：MoreVert 改为 DropdownMenu 溢出菜单
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.ai_more),
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.text_01)) },
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
                            HorizontalDivider()
                            // P0 v0.7.2: 4 个学习工具入口,接通 ViewModel 已有但未接 UI 的方法
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.text_02)) },
                                onClick = {
                                    showOverflowMenu = false
                                    showLearningToolDialog = LearningToolMode.ESSAY_GUIDE
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.text_03)) },
                                onClick = {
                                    showOverflowMenu = false
                                    showLearningToolDialog = LearningToolMode.WRONG_ANSWER
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.text_04)) },
                                onClick = {
                                    showOverflowMenu = false
                                    showLearningToolDialog = LearningToolMode.RECALL_CHECK
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.text_05)) },
                                onClick = {
                                    showOverflowMenu = false
                                    showLearningToolDialog = LearningToolMode.ROTE_CHECK
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
                // v0.9.24 停止生成
                onStop = viewModel::stopGeneration,
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
                    // v0.8.15 Stage 1: 横屏/平板下限制消息列表最大宽度并居中，避免对话气泡行宽过宽阅读疲劳。
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        LazyColumn(
                            modifier = Modifier.widthIn(max = MaxContentWidth.compact),
                            state = listState,
                            contentPadding = PaddingValues(Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                        // P2-LAZY-1 修正：混合列表加 contentType，帮助 LazyList 复用 item cache
                        // 消息项 contentType="message"，加载指示器 contentType="loading"
                        items(items = uiState.messages, key = { it.id }, contentType = { "message" }) { message ->
                            MessageBubble(message, modifier = Modifier.animateItem())
                        }
                        // v0.9.24 流式输出气泡：AI 逐字回复中显示增量文本 + 光标
                        if (uiState.isLoading && uiState.streamingContent != null) {
                            item(key = "streaming", contentType = "streaming") {
                                StreamingBubble(content = uiState.streamingContent ?: "")
                            }
                        }
                        // v0.9.25 修复：流式时不再同时显示 loading 转圈（原条件 isLoading 与上面重叠）
                        if (uiState.isLoading && uiState.streamingContent == null) {
                            item(key = "loading", contentType = "loading") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    WenyanLoadingIndicator()
                                }
                            }
                            } // LazyColumn end
                        } // Box end
                    }
                }
            }
        }
    }

    // P0-3 修复：清空对话确认 Dialog，避免误触丢失全部消息
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.text_06)) },
            text = { Text(stringResource(R.string.text_07)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearMessages()
                        showClearConfirmDialog = false
                    },
                ) {
                    Text(stringResource(R.string.text_08), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.text_09))
                }
            },
        )
    }

    // P0 v0.7.2: 学习工具 Dialog
    showLearningToolDialog?.let { mode ->
        LearningToolDialog(
            mode = mode,
            onDismiss = { showLearningToolDialog = null },
            onEssayGuide = { question, answer ->
                viewModel.guideEssayAnswer(question, answer)
                showLearningToolDialog = null
            },
            onWrongAnswer = { question, userAnswer, correctAnswer ->
                viewModel.explainWrongAnswer(question, userAnswer, correctAnswer)
                showLearningToolDialog = null
            },
            onRecallCheck = { userAnswer, correctAnswer, questionType ->
                viewModel.launchCheckRecall(userAnswer, correctAnswer, questionType)
                showLearningToolDialog = null
            },
            onRoteCheck = { pointId, relatedIds ->
                viewModel.checkRoteMemorization(pointId, relatedIds)
                showLearningToolDialog = null
            },
        )
    }

    // P0 v0.7.2: 回忆检测结果展示
    uiState.recallResult?.let { result ->
        RecallResultDialog(
            result = result,
            onDismiss = viewModel::clearRecallResult,
        )
    }
}

// ── 学习工具 Dialog ────────────────────────────────────────────

/** 学习工具模式(P0 v0.7.2) */
private enum class LearningToolMode {
    ESSAY_GUIDE,
    WRONG_ANSWER,
    RECALL_CHECK,
    ROTE_CHECK,
}

@Composable
private fun LearningToolDialog(
    mode: LearningToolMode,
    onDismiss: () -> Unit,
    onEssayGuide: (question: String, userAnswer: String) -> Unit,
    onWrongAnswer: (question: String, userAnswer: String, correctAnswer: String) -> Unit,
    onRecallCheck: (userAnswer: String, correctAnswer: String, questionType: QuestionType) -> Unit,
    onRoteCheck: (pointId: String, relatedIds: List<String>) -> Unit,
) {
    val title = when (mode) {
        LearningToolMode.ESSAY_GUIDE -> "论述题引导"
        LearningToolMode.WRONG_ANSWER -> "错题解释"
        LearningToolMode.RECALL_CHECK -> "回忆检测"
        LearningToolMode.ROTE_CHECK -> "死记硬背检测"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = {
            // v0.8.3 修复：原 Column 无 verticalArrangement，说明文字/输入框/按钮紧贴一起
            // 视觉拥挤且误触率高。加 spacedBy(Spacing.sm) 保证 8dp 间距。
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                when (mode) {
                    LearningToolMode.ESSAY_GUIDE -> EssayGuideFields(onEssayGuide)
                    LearningToolMode.WRONG_ANSWER -> WrongAnswerFields(onWrongAnswer)
                    LearningToolMode.RECALL_CHECK -> RecallCheckFields(onRecallCheck)
                    LearningToolMode.ROTE_CHECK -> RoteCheckFields(onRoteCheck)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.text_09)) }
        },
    )
}

@Composable
private fun EssayGuideFields(onSubmit: (String, String) -> Unit) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    Text(stringResource(R.string.text_10), style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text(stringResource(R.string.text_11)) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text(stringResource(R.string.text_12)) }, modifier = Modifier.fillMaxWidth())
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = { onSubmit(question, answer) },
            enabled = question.isNotBlank() && answer.isNotBlank(),
        ) { Text(stringResource(R.string.text_13)) }
    }
}

@Composable
private fun WrongAnswerFields(onSubmit: (String, String, String) -> Unit) {
    var question by remember { mutableStateOf("") }
    var userAnswer by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf("") }
    Text(stringResource(R.string.text_14), style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text(stringResource(R.string.text_15)) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = userAnswer, onValueChange = { userAnswer = it }, label = { Text(stringResource(R.string.text_16)) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = correctAnswer, onValueChange = { correctAnswer = it }, label = { Text(stringResource(R.string.text_17)) }, modifier = Modifier.fillMaxWidth())
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = { onSubmit(question, userAnswer, correctAnswer) },
            enabled = question.isNotBlank() && userAnswer.isNotBlank() && correctAnswer.isNotBlank(),
        ) { Text(stringResource(R.string.text_18)) }
    }
}

@Composable
private fun RecallCheckFields(onSubmit: (String, String, QuestionType) -> Unit) {
    var userAnswer by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf("") }
    var questionType by remember { mutableStateOf(QuestionType.TERM_EXPLANATION) }
    Text(stringResource(R.string.text_19), style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(value = userAnswer, onValueChange = { userAnswer = it }, label = { Text(stringResource(R.string.text_12)) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = correctAnswer, onValueChange = { correctAnswer = it }, label = { Text(stringResource(R.string.text_20)) }, modifier = Modifier.fillMaxWidth())
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(selected = questionType == QuestionType.TERM_EXPLANATION, onClick = { questionType = QuestionType.TERM_EXPLANATION }, label = { Text(stringResource(R.string.text_21)) })
        Spacer(modifier = Modifier.size(Spacing.sm))
        FilterChip(selected = questionType == QuestionType.ESSAY, onClick = { questionType = QuestionType.ESSAY }, label = { Text(stringResource(R.string.text_11)) })
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = { onSubmit(userAnswer, correctAnswer, questionType) },
            enabled = userAnswer.isNotBlank() && correctAnswer.isNotBlank(),
        ) { Text(stringResource(R.string.text_22)) }
    }
}

@Composable
private fun RoteCheckFields(onSubmit: (String, List<String>) -> Unit) {
    var pointId by remember { mutableStateOf("") }
    var relatedIdsText by remember { mutableStateOf("") }
    Text(stringResource(R.string.text_23), style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(value = pointId, onValueChange = { pointId = it }, label = { Text(stringResource(R.string.text_24)) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = relatedIdsText, onValueChange = { relatedIdsText = it }, label = { Text(stringResource(R.string.text_25)) }, modifier = Modifier.fillMaxWidth())
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = {
                val ids = relatedIdsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                onSubmit(pointId, ids)
            },
            enabled = pointId.isNotBlank(),
        ) { Text(stringResource(R.string.text_22)) }
    }
}

@Composable
private fun RecallResultDialog(result: RecallResult, onDismiss: () -> Unit) {
    val ratingText = when (result.rating) {
        RecallRating.AGAIN -> "需要重学"
        RecallRating.HARD -> "勉强记住"
        RecallRating.GOOD -> "记得不错"
        RecallRating.EASY -> "记忆牢固"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.text_26)) },
        text = {
            Column {
                Text(stringResource(R.string.ai_score_level, result.level))
                Text(stringResource(R.string.ai_score_coverage, "%.0f".format(result.coverage * 100)))
                Text(stringResource(R.string.ai_score_rating, ratingText))
                result.score?.let { Text(stringResource(R.string.ai_score_llm, it)) }
                result.reason?.let { Text(stringResource(R.string.ai_score_reason, it)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.text_27)) }
        },
    )
}

// ── 输入栏 ──────────────────────────────────────────────────────

@Composable
private fun InputBar(
    text: String,
    isLoading: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // P0-1 修复：imePadding 让输入栏随 IME 上推，避免键盘遮挡。
            // navigationBarsPadding 确保手势导航条不遮挡输入栏。
            .imePadding()
            .navigationBarsPadding()
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.text_28)) },
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )
        IconButton(
            // v0.9.24 停止生成：isLoading 时点击停止（取消 AI 流式任务）
            onClick = if (isLoading) onStop else onSend,
            enabled = if (isLoading) true else text.isNotBlank(),
        ) {
            if (isLoading) {
                // v0.9.30 打磨：停止用方块图标（Close X 语义是"关闭"，误导；Stop 图标库缺失，用自定义方块）
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.ai_send),
                )
            }
        }
    }
}

// ── 消息气泡 ────────────────────────────────────────────────────

/**
 * 流式输出气泡（v0.9.24 新增）。
 *
 * AI 逐字回复中实时显示增量文本，末尾加闪烁光标提示"生成中"。
 */
@Composable
private fun StreamingBubble(
    content: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    // v0.9.30 打磨：光标闪烁动画（此前为静态"▍"，注释称闪烁实为不闪）
    val infiniteTransition = rememberInfiniteTransition(label = "streaming_cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursor_alpha",
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                // v0.9.30 打磨：移除幽灵头像 48dp 留白（无头像渲染，留白致气泡不对称）
                .clip(MaterialTheme.shapes.large)
                .background(colorScheme.surfaceContainerHigh)
                .padding(Spacing.md),
        ) {
            Text(
                text = if (content.isEmpty()) "……" else content,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface,
            )
            // 光标（闪烁竖线提示生成中）
            Text(
                text = "▍",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.primary.copy(alpha = cursorAlpha),
            )
        }
    }
}

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
                    // v0.9.30 打磨：移除幽灵头像 48dp 留白（无头像渲染）
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
            // v0.9.24 token 用量小字（仅 AI 消息、非空时显示）
            if (message.tokensUsed != null) {
                Text(
                    text = "本次回复 ${message.tokensUsed} tokens",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs),
                )
            }
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
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                .clickable(role = Role.Button, onClick = onDismiss)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}
