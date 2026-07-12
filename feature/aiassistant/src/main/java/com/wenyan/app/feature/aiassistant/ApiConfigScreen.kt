package com.wenyan.app.feature.aiassistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.database.entity.ApiConfigEntity

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

    // 错误提示 → Snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddForm) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "添加配置")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.configs.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    ConfigList(
                        configs = uiState.configs,
                        currentConfigId = uiState.currentConfigId,
                        onSetCurrent = viewModel::setCurrent,
                        onEdit = viewModel::showEditForm,
                        onDelete = { config -> deletingConfig = config },
                        contentPadding = PaddingValues(16.dp),
                    )
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
            title = { Text("删除配置") },
            text = { Text("确定删除「${config.displayName}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConfig(config.id)
                        deletingConfig = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingConfig = null }) {
                    Text("取消")
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(configs, key = { it.id }) { config ->
            ConfigCard(
                config = config,
                isCurrent = config.id == currentConfigId,
                onSetCurrent = { onSetCurrent(config.id) },
                onEdit = { onEdit(config) },
                onDelete = { onDelete(config) },
            )
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSetCurrent, // 点击卡片设为当前
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = config.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (isCurrent) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "当前使用",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
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
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text("编辑")
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text("删除", color = MaterialTheme.colorScheme.error)
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

// ── 空状态 ────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "暂无 API 配置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "点击右下角 + 添加服务商配置\n支持 DeepSeek / 通义 / 智谱 / 月之暗面",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ── 表单弹窗 ──────────────────────────────────────────────────

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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API 配置") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 服务商预设选择
                Text(
                    text = "服务商",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(LlmProvider.entries.toList()) { provider ->
                        ProviderChip(
                            label = provider.displayName,
                            selected = formState.provider == provider.key,
                            onClick = { onProviderChange(provider.key) },
                        )
                    }
                }

                FormTextField(
                    label = "显示名称",
                    value = formState.displayName,
                    onValueChange = onDisplayNameChange,
                    placeholder = "如：我的 DeepSeek",
                )
                FormTextField(
                    label = "接口地址",
                    value = formState.baseUrl,
                    onValueChange = onBaseUrlChange,
                    placeholder = "https://api.deepseek.com",
                )
                FormTextField(
                    label = "API 密钥",
                    value = formState.apiKey,
                    onValueChange = onApiKeyChange,
                    placeholder = "sk-...",
                    isPassword = true,
                )
                FormTextField(
                    label = "模型名称",
                    value = formState.model,
                    onValueChange = onModelChange,
                    placeholder = "deepseek-chat",
                )
                FormTextField(
                    label = "温度（0-2）",
                    value = formState.temperature.toString(),
                    onValueChange = { v ->
                        v.toDoubleOrNull()?.let { onTemperatureChange(it.coerceIn(0.0, 2.0)) }
                    },
                    placeholder = "0.7",
                    keyboardType = KeyboardType.Decimal,
                )
                FormTextField(
                    label = "最大 Token 数",
                    value = formState.maxTokens.toString(),
                    onValueChange = { v ->
                        v.toIntOrNull()?.let { onMaxTokensChange(it.coerceIn(1, 32000)) }
                    },
                    placeholder = "2000",
                    keyboardType = KeyboardType.Number,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun ProviderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}
