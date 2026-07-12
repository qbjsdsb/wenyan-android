package com.wenyan.app.feature.aiassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ApiConfigRepository
import com.wenyan.app.core.database.entity.ApiConfigEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * API 配置界面 ViewModel（Spec C5.7a 设计文档 3.6.4 API 多服务商配置）。
 *
 * 管理：
 * - 配置列表（观察 [ApiConfigRepository.observeAllConfigs]）
 * - 当前选中配置
 * - 添加/编辑表单状态
 * - 保存/删除/设为当前操作
 *
 * 支持服务商：DeepSeek / 通义 / 智谱 / 月之暗面 / 自定义（OpenAI 兼容协议）。
 */
@HiltViewModel
class ApiConfigViewModel @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
) : ViewModel() {

    /** 表单状态（添加/编辑时使用） */
    private val _formState = MutableStateFlow(ApiConfigFormState())
    val formState: StateFlow<ApiConfigFormState> = _formState.asStateFlow()

    /** 是否显示表单弹窗 */
    private val _isFormVisible = MutableStateFlow(false)
    val isFormVisible: StateFlow<Boolean> = _isFormVisible.asStateFlow()

    /** 错误提示 */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 编辑中的配置 ID（null = 新建模式） */
    private var editingId: String? = null

    /**
     * UI 状态：合并配置列表 + 当前配置标记。
     *
     * 配置列表中 apiKey 已由 ApiConfigRepository 解密，
     * 但 UI 层不展示完整 apiKey（仅显示掩码），避免泄露。
     */
    val uiState: StateFlow<ApiConfigUiState> = apiConfigRepository.observeAllConfigs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ApiConfigUiState(isLoading = true),
        )

    /** 打开新建表单 */
    fun showAddForm() {
        editingId = null
        _formState.value = ApiConfigFormState()
        _isFormVisible.value = true
    }

    /** 打开编辑表单，预填已有配置 */
    fun showEditForm(config: ApiConfigEntity) {
        editingId = config.id
        _formState.value = ApiConfigFormState(
            provider = config.provider,
            displayName = config.displayName,
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = config.model,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
        )
        _isFormVisible.value = true
    }

    /** 关闭表单 */
    fun dismissForm() {
        _isFormVisible.value = false
        editingId = null
        _formState.value = ApiConfigFormState()
    }

    /** 表单字段更新 */
    fun updateProvider(provider: String) {
        val preset = LlmProvider.fromKey(provider)
        _formState.update {
            it.copy(
                provider = provider,
                displayName = if (editingId == null) preset.displayName else it.displayName,
                baseUrl = if (editingId == null) preset.defaultBaseUrl else it.baseUrl,
                model = if (editingId == null) preset.defaultModel else it.model,
            )
        }
    }

    fun updateDisplayName(value: String) = _formState.update { it.copy(displayName = value) }
    fun updateBaseUrl(value: String) = _formState.update { it.copy(baseUrl = value) }
    fun updateApiKey(value: String) = _formState.update { it.copy(apiKey = value) }
    fun updateModel(value: String) = _formState.update { it.copy(model = value) }
    fun updateTemperature(value: Double) = _formState.update { it.copy(temperature = value) }
    fun updateMaxTokens(value: Int) = _formState.update { it.copy(maxTokens = value) }

    /** 保存配置（新建或更新） */
    fun saveConfig() {
        val form = _formState.value
        // 基本校验
        if (form.displayName.isBlank()) {
            _errorMessage.value = "请填写显示名称"
            return
        }
        if (form.baseUrl.isBlank()) {
            _errorMessage.value = "请填写接口地址"
            return
        }
        if (form.apiKey.isBlank()) {
            _errorMessage.value = "请填写 API 密钥"
            return
        }
        if (form.model.isBlank()) {
            _errorMessage.value = "请填写模型名称"
            return
        }

        val id = editingId ?: UUID.randomUUID().toString()
        val existing = uiState.value.configs.find { it.id == id }
        val entity = ApiConfigEntity(
            id = id,
            provider = form.provider,
            displayName = form.displayName.trim(),
            baseUrl = form.baseUrl.trim(),
            apiKey = form.apiKey.trim(),
            model = form.model.trim(),
            temperature = form.temperature,
            maxTokens = form.maxTokens,
            isEnabled = existing?.isEnabled ?: 1,
            isCurrent = existing?.isCurrent ?: 0, // 新建默认不设为当前
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )

        viewModelScope.launch {
            try {
                apiConfigRepository.saveConfig(entity)
                // 如果是新建且列表中无当前配置，自动设为当前
                if (editingId == null && uiState.value.currentConfigId == null) {
                    apiConfigRepository.setCurrent(id)
                }
                dismissForm()
            } catch (e: Exception) {
                _errorMessage.value = "保存失败：${e.message}"
            }
        }
    }

    /** 设为当前使用 */
    fun setCurrent(id: String) {
        viewModelScope.launch {
            try {
                apiConfigRepository.setCurrent(id)
            } catch (e: Exception) {
                _errorMessage.value = "切换失败：${e.message}"
            }
        }
    }

    /** 删除配置 */
    fun deleteConfig(id: String) {
        viewModelScope.launch {
            try {
                apiConfigRepository.deleteConfig(id)
            } catch (e: Exception) {
                _errorMessage.value = "删除失败：${e.message}"
            }
        }
    }

    /** 清除错误提示 */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * API 配置 UI 状态。
 *
 * @param isLoading 加载中标记
 * @param configs 所有配置列表（apiKey 已解密，但 UI 层应掩码展示）
 * @param currentConfigId 当前选中配置 ID（null 表示无）
 */
data class ApiConfigUiState(
    val isLoading: Boolean = false,
    val configs: List<ApiConfigEntity> = emptyList(),
) {
    /** 当前选中的配置 ID */
    val currentConfigId: String? get() = configs.firstOrNull { it.isCurrent == 1 }?.id
}

/** 表单状态 */
data class ApiConfigFormState(
    val provider: String = LlmProvider.DEEPSEEK.key,
    val displayName: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 2000,
)

/**
 * 预定义 LLM 服务商（OpenAI 兼容协议）。
 *
 * 选择预设后自动填充 baseUrl 和 model，用户可手动修改。
 */
enum class LlmProvider(
    val key: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
) {
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com", "deepseek-chat"),
    QWEN("qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-turbo"),
    ZHIPU("zhipu", "智谱清言", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash"),
    MOONSHOT("moonshot", "月之暗面", "https://api.moonshot.cn/v1", "moonshot-v1-8k"),
    CUSTOM("custom", "自定义", "", ""),
    ;

    companion object {
        fun fromKey(key: String): LlmProvider = entries.find { it.key == key } ?: CUSTOM
    }
}
