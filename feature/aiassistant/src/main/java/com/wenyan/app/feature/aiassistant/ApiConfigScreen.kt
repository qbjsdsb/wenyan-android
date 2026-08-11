package com.wenyan.app.feature.aiassistant

import androidx.compose.ui.res.stringResource
import com.wenyan.app.feature.aiassistant.R

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.AlertDialog
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.database.entity.ApiConfigEntity
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar

/**
 * API 配置界面（Spec C5.7a 设计文档 3.6.4 API 多服务商配置）。
 *
 * 功能：
 * - 展示已保存的 API 配置列表
 * - 添加/编辑配置（服务商预设 + 自定义字段）
 * - 设为当前使用
 * - 删除配置（需确认）
 *
 * 支持 OpenAI 兼容协议服务商：DeepSeek / 通义 / 智谱 / 月之暗面 / 自定义。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigScreen(
    onBack: () -> Unit = {},
    viewModel: ApiConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isFormVisible by viewModel.isFormVisible.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var deletingConfig by remember { mutableStateOf<ApiConfigEntity?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    // 错误提示 → Snackbar
    // P0-4 修复：先 clearError() 再 showSnackbar()，与 AiAssistantScreen NF-UC4 修复一致。
    // 原顺序（showSnackbar → clearError）在用户退出 ApiConfig 时协程被取消，
    // clearError() 不执行 → 下次进入时 errorMessage 仍非空 → snackbar 重复展示。
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            viewModel.clearError()
            snackbarHostState.showSnackbar(msg)
        }
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = stringResource(R.string.api_title),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            // v0.8.3 修复：表单弹出时隐藏 FAB，避免被 scrim 遮挡但仍可点击的歧义
            if (!isFormVisible) {
                FloatingActionButton(onClick = viewModel::showAddForm) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.api_add_config))
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            Crossfade(
                // v0.8.13 修复（P0-3）：原仅 (isLoading, isEmpty) 两元组，
                // 加载失败时 configs 仍为空 → fallthrough 到 isEmpty 分支误显示"暂无 API 配置"，
                // 用户无法区分"真的没配置"还是"加载失败"。
                // 现改为三元组 (isLoading, error, isEmpty)，error 优先级最高，
                // 进入 ErrorState 分支并提供"重试"按钮调用 viewModel::retry() 重新拉取数据流。
                targetState = Triple(uiState.isLoading, uiState.error, uiState.configs.isEmpty()),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "api_config_state",
                modifier = Modifier.fillMaxSize(),
            ) { (isLoading, error, isEmpty) ->
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            WenyanLoadingIndicator()
                        }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            ErrorState(
                                icon = Icons.Default.CloudOff,
                                title = stringResource(R.string.api_load_failed),
                                message = error,
                                onRetry = viewModel::retry,
                            )
                        }
                    }
                    isEmpty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyState(
                                icon = Icons.Default.Inbox,
                                title = stringResource(R.string.api_empty),
                                description = "点击右下角 + 添加服务商配置\n支持 DeepSeek / 通义 / 智谱 / 月之暗面",
                            )
                        }
                    }
                    else -> {
                        ConfigList(
                            configs = uiState.configs,
                            currentConfigId = uiState.currentConfigId,
                            onSetCurrent = viewModel::setCurrent,
                            onEdit = viewModel::showEditForm,
                            onDelete = { config -> deletingConfig = config },
                            contentPadding = PaddingValues(Spacing.lg),
                        )
                    }
                }
            }
        }
    }

    // 添加/编辑表单弹窗
    if (isFormVisible) {
        ApiConfigFormDialog(
            formState = formState,
            onProviderChange = viewModel::updateProvider,
            onDisplayNameChange = viewModel::updateDisplayName,
            onBaseUrlChange = viewModel::updateBaseUrl,
            onApiKeyChange = viewModel::updateApiKey,
            onModelChange = viewModel::updateModel,
            onTemperatureChange = viewModel::updateTemperature,
            onMaxTokensChange = viewModel::updateMaxTokens,
            onSave = viewModel::saveConfig,
            onDismiss = viewModel::dismissForm,
        )
    }

    // 删除确认对话框
    deletingConfig?.let { config ->
        AlertDialog(
            onDismissRequest = { deletingConfig = null },
            title = { Text(stringResource(R.string.text_29)) },
            text = { Text(stringResource(R.string.api_delete_confirm, config.displayName)) },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConfig(config.id)
                        deletingConfig = null
                    },
                ) {
                    Text(stringResource(R.string.text_30), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingConfig = null }) {
                    Text(stringResource(R.string.text_09))
                }
            },
        )
    }
}

// ── 配置列表 ──────────────────────────────────────────────────

@Composable
private fun ConfigList(
    configs: List<ApiConfigEntity>,
    currentConfigId: String?,
    onSetCurrent: (String) -> Unit,
    onEdit: (ApiConfigEntity) -> Unit,
    onDelete: (ApiConfigEntity) -> Unit,
    contentPadding: PaddingValues,
) {
    // v0.8.15 Stage 1: 横屏/平板下限制内容最大宽度并居中，避免配置卡片行宽过宽阅读疲劳。
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = MaxContentWidth.comfortable),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(configs, key = { it.id }, contentType = { "config" }) { config ->
                ConfigCard(
                    config = config,
                    isCurrent = config.id == currentConfigId,
                    onSetCurrent = { onSetCurrent(config.id) },
                    onEdit = { onEdit(config) },
                    onDelete = { onDelete(config) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun ConfigCard(
    config: ApiConfigEntity,
    isCurrent: Boolean,
    onSetCurrent: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TonalCard(
        modifier = modifier
            .fillMaxWidth()
            // v0.8.3 修复（P2-A-1）：整卡可点击设为"当前使用"（Android 设置惯用模式），
            // 但将原 CheckCircle 图标改为 RadioButton，使"单选"语义更明确，
            // 避免用户误以为点击卡片是"查看详情"。
            .clickable(role = Role.Button, onClick = onSetCurrent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            // v0.8.3 修复（P3-A-1）：Spacing.xs + Spacing.xs 等价于 Spacing.sm，直接用 token
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = config.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    // P1-6 修复：Bold(700) 过重，M3 Expressive 推荐 SemiBold(600)
                    fontWeight = FontWeight.SemiBold,
                    // P1-2 修复：长显示名限 1 行 + 省略号，避免与右侧 RadioButton 错位
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // v0.8.3（P2-A-1）：RadioButton 替代 CheckCircle，单选语义更明确
                RadioButton(
                    selected = isCurrent,
                    onClick = onSetCurrent,
                )
            }

            // 服务商标签
            val providerLabel = LlmProvider.fromKey(config.provider).displayName
            Text(
                text = "服务商：$providerLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "模型：${config.model}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "接口：${config.baseUrl}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // P1-2 修复：长 URL 限 1 行 + 省略号，避免换行撑高卡片
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // API Key 掩码展示（避免泄露完整密钥）
            Text(
                text = "密钥：${maskApiKey(config.apiKey)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs),
                // v0.8.3 修复（P2-A-2）：加 spacedBy 避免编辑/删除按钮紧贴
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.End),
            ) {
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.padding(end = Spacing.xs),
                    )
                    Text(stringResource(R.string.text_31))
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = Spacing.xs),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(stringResource(R.string.text_30), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** API Key 掩码：仅显示前4位和后4位，中间用 * 代替 */
private fun maskApiKey(key: String): String {
    if (key.length <= 8) return "****"
    val prefix = key.take(4)
    val suffix = key.takeLast(4)
    return "$prefix****$suffix"
}

// ── 表单弹窗 ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiConfigFormDialog(
    formState: ApiConfigFormState,
    onProviderChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTemperatureChange: (Double) -> Unit,
    onMaxTokensChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    // P0-2 修复：AlertDialog 改为 ModalBottomSheet。
    // 原因：AlertDialog 内含 7 个 OutlinedTextField，IME 弹出时不会上推，
    // 底部字段（温度/Token）被键盘遮挡无法访问。
    // ModalBottomSheet 天然支持 IME 上推（contentWindowInsets 包含 ime），
    // 且 BottomSheet 是 M3 Expressive 推荐的长表单容器形态。
    //
    // P0-3 修复：温度和 Token 输入框改为本地 String state 缓冲。
    // 原因：原实现是受控的 `value = formState.temperature.toString()`，用户输入
    // "0." / "-0" / "" 时 toDoubleOrNull() 返回 null，let {} 不执行，
    // formState.temperature 不变，输入被立即丢弃，用户无法清空重输或输入小数点。
    // 现改为本地 String state 自由输入，onSave 时统一解析与 coerceIn。
    // 本地文本在保存前不回写 ViewModel，避免每次输入合法字符都改变
    // rememberSaveable 的 key，把用户正在编辑的 "0." / "1" 重置成 Double 格式。
    //
    // v0.8.3 修复（P1-A-2）：remember → rememberSaveable，屏幕旋转不丢失输入。
    // v0.8.3 修复（P1-A-1）：添加输入校验，非法值时显示错误提示。
    var temperatureText by rememberSaveable(formState.temperature) {
        mutableStateOf(formState.temperature.toString())
    }
    var maxTokensText by rememberSaveable(formState.maxTokens) {
        mutableStateOf(formState.maxTokens.toString())
    }

    // 输入校验状态
    val temperatureError = remember(temperatureText) {
        val parsed = temperatureText.toDoubleOrNull()
        when {
            temperatureText.isBlank() -> null // 空值允许，保存时用默认值
            // v0.9.30 打磨：输入"0."、"."、"-"等中间态不报错（合法数字格式，可继续输入）
            parsed == null ->
                if (temperatureText.matches(NUMERIC_PATTERN)) null else "请输入有效数字"
            parsed < 0.0 || parsed > 2.0 -> "范围 0-2"
            else -> null
        }
    }
    val maxTokensError = remember(maxTokensText) {
        val parsed = maxTokensText.toIntOrNull()
        when {
            maxTokensText.isBlank() -> null
            parsed == null ->
                if (maxTokensText.matches(INTEGER_PATTERN)) null else "请输入有效整数"
            parsed < 1 || parsed > 32000 -> "范围 1-32000"
            else -> null
        }
    }
    // 中间态（如 "-"、"0."）允许继续输入，但不能作为有效配置保存。
    val temperatureCanSave = temperatureText.isBlank() || temperatureText.toDoubleOrNull() != null
    val maxTokensCanSave = maxTokensText.isBlank() || maxTokensText.toIntOrNull() != null

    // v0.9.30 打磨：必填校验（此前空名称/URL/key 也可保存）
    val displayNameError = remember(formState.displayName) {
        if (formState.displayName.isBlank()) "请输入显示名称" else null
    }
    // v0.9.35 审计修复：UI 直接复用 validateBaseUrl，避免表单实时校验和保存校验
    // 在协议大小写、首尾空白、无域名等边界上出现两套规则。
    val baseUrlError = remember(formState.baseUrl) {
        when {
            formState.baseUrl.isBlank() -> "请输入接口地址"
            else -> validateBaseUrl(formState.baseUrl)
        }
    }
    val apiKeyError = remember(formState.apiKey) {
        if (formState.apiKey.isBlank()) "请输入 API 密钥" else null
    }
    val modelError = remember(formState.model) {
        if (formState.model.isBlank()) "请输入模型名称" else null
    }

    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = "API 配置",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            // 服务商预设选择
            Text(
                text = "服务商",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                // v0.8.3 修复（P2-A-6）：加 contentPadding 提供滚动起始余量
                contentPadding = PaddingValues(horizontal = Spacing.lg),
            ) {
                // P2-LAZY-1 修正：LazyRow items 加 key（用 provider.key 唯一标识），避免重组时丢失选中状态
                // NF-UP4 修正：加 contentType 让 LazyRow 复用同一类型 item 的 slot，提升滚动性能
                items(items = LlmProvider.entries.toList(), key = { it.key }, contentType = { "provider" }) { provider ->
                    ProviderChip(
                        label = provider.displayName,
                        selected = formState.provider == provider.key,
                        onClick = { onProviderChange(provider.key) },
                    )
                }
            }

            FormTextField(
                label = stringResource(R.string.api_display_name),
                value = formState.displayName,
                onValueChange = onDisplayNameChange,
                placeholder = stringResource(R.string.api_display_name_placeholder),
                // v0.9.30 打磨：必填校验
                isError = displayNameError != null,
                supportingText = displayNameError,
            )
            FormTextField(
                label = stringResource(R.string.api_base_url),
                value = formState.baseUrl,
                onValueChange = onBaseUrlChange,
                placeholder = "https://api.deepseek.com",
                // v0.9.30 打磨：必填 + URL 格式校验
                isError = baseUrlError != null,
                supportingText = baseUrlError,
            )
            FormTextField(
                label = stringResource(R.string.api_key),
                value = formState.apiKey,
                onValueChange = onApiKeyChange,
                placeholder = "sk-...",
                isPassword = true,
                // v0.9.30 打磨：必填校验
                isError = apiKeyError != null,
                supportingText = apiKeyError,
            )
            FormTextField(
                label = stringResource(R.string.api_model),
                value = formState.model,
                onValueChange = onModelChange,
                placeholder = "deepseek-chat",
                // v0.9.30 打磨：必填校验
                isError = modelError != null,
                supportingText = modelError,
            )
            FormTextField(
                label = stringResource(R.string.api_temperature),
                value = temperatureText,
                onValueChange = { v ->
                    // P0-3 修复：本地 state 自由接收输入，不立即解析
                    temperatureText = v
                },
                placeholder = "0.7",
                keyboardType = KeyboardType.Decimal,
                // v0.8.3（P1-A-1）：输入校验错误反馈
                isError = temperatureError != null,
                supportingText = temperatureError,
            )
            FormTextField(
                label = stringResource(R.string.api_max_tokens),
                value = maxTokensText,
                onValueChange = { v ->
                    // P0-3 修复：本地 state 自由接收输入，不立即解析
                    maxTokensText = v
                },
                placeholder = "2000",
                keyboardType = KeyboardType.Number,
                // v0.8.3（P1-A-1）：输入校验错误反馈
                isError = maxTokensError != null,
                supportingText = maxTokensError,
            )

            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.text_09))
                }
                // v0.8.3 修复（P2-A-3）：保存是主要操作，改用 FilledTonalButton 提升视觉权重
                // 有输入错误时禁用保存，防止用户保存非法值
                FilledTonalButton(
                    onClick = {
                        // P0-3 修复：保存时统一解析本地 state，非法值降级为默认值
                        val parsedTemp = temperatureText.toDoubleOrNull()
                            ?.coerceIn(0.0, 2.0)
                            ?: formState.temperature
                        if (parsedTemp != formState.temperature) {
                            onTemperatureChange(parsedTemp)
                        }
                        val parsedTokens = maxTokensText.toIntOrNull()
                            ?.coerceIn(1, 32000)
                            ?: formState.maxTokens
                        if (parsedTokens != formState.maxTokens) {
                            onMaxTokensChange(parsedTokens)
                        }
                        onSave()
                    },
                    // v0.9.30 打磨：保存按钮禁用条件加入必填校验（此前只查温度/Token）
                    enabled = temperatureCanSave &&
                        maxTokensCanSave &&
                        temperatureError == null &&
                        maxTokensError == null &&
                        displayNameError == null &&
                        baseUrlError == null &&
                        apiKeyError == null &&
                        modelError == null,
                ) {
                    Text(stringResource(R.string.text_32))
                }
            }
        }
    }
}

@Composable
private fun ProviderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    // v0.8.3 新增（P1-A-1）：支持输入校验错误状态与提示文本
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        // v0.8.3 修复（P3-A-2）：使用 import 的 VisualTransformation 替代全限定名
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 合法十进制数中间态（允许 "0."、"."、"-" 等继续输入，v0.9.30）。 */
private val NUMERIC_PATTERN = Regex("-?[0-9]*\\.?[0-9]*")

/** 合法整数中间态（允许 "-" 等继续输入，v0.9.30）。 */
private val INTEGER_PATTERN = Regex("-?[0-9]*")
