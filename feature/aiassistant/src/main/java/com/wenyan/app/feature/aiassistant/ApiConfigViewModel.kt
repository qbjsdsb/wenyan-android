package com.wenyan.app.feature.aiassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.common.util.friendlyErrorMessage
import com.wenyan.app.core.data.repository.ApiConfigRepository
import com.wenyan.app.core.database.entity.ApiConfigEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
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
    // P0-C1 修正：原为 var，非线程安全。改用 MutableStateFlow（内部 AtomicReference）。
    private val editingId = MutableStateFlow<String?>(null)

    /**
     * 重试触发器：用户点击"重试"按钮时递增，驱动 [flatMapLatest] 重新订阅数据流。
     *
     * v0.8.13 修复（P0-3）：原 uiState 直接订阅 [apiConfigRepository.observeAllConfigs]，
     * 加载失败后进入 [catch] 分支 emit 终态，但无法重试 —— StateFlow 终态后无新数据。
     * 现以 _retryTrigger 作为上游触发器，retry() 递增后 flatMapLatest 重新订阅，实现"重试"语义。
     */
    private val _retryTrigger = MutableStateFlow(0)

    /** 用户点击重试 */
    fun retry() {
        _retryTrigger.value++
    }

    /**
     * UI 状态：合并配置列表 + 当前配置标记。
     *
     * 配置列表中 apiKey 已由 ApiConfigRepository 解密，
     * 但 UI 层不展示完整 apiKey（仅显示掩码），避免泄露。
     *
     * v0.8.13 修复（P0-3 + P0-3b）：
     * - 原直接订阅 [apiConfigRepository.observeAllConfigs]，加载失败后 StateFlow 终态无法重试。
     * - 现以 [_retryTrigger] 为上游 flatMapLatest 触发重试，retry() 递增后重新订阅数据流。
     * - 错误信息用 [friendlyErrorMessage] 统一映射，避免裸 e.message 暴露英文堆栈给用户。
     * - Log.e 输出原始异常便于排查。
     *
     * **关键实现细节（P0-3b 修复）**：`.catch` 必须放在 `flatMapLatest` 内部，
     * 作用于内部 flow，**不能**放在外层。
     *
     * 原因：catch 操作符执行后 flow **正常完成**。若放在外层：
     * ```
     * _retryTrigger.flatMapLatest { ... }.catch { emit(error) }
     * ```
     * 内部 observeAllConfigs 抛异常 → 冒泡到外层 catch → emit 错误态 → 外层 flow 完成。
     * 此时即使 _retryTrigger（StateFlow 不完成）继续发射新值，已完成的下游 chain 不会再被激活，
     * **retry() 永久失效**，用户无法从加载失败状态恢复（Kotlin issue #3594）。
     *
     * 正确模式：catch 放在 flatMapLatest 内部，仅作用于内部 flow。
     * 内部 flow 异常被 catch 后 emit 错误态并完成，但外层 _retryTrigger 仍活跃，
     * retry() 递增 → flatMapLatest 取消上一个（已完成的）内部 flow → 重新订阅新的内部 flow。
     *
     * 注意：本 [error] 与 [_errorMessage] 不同维度——后者是用户操作（save/delete）反馈，
     * 前者是流加载错误。两者不应混用。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ApiConfigUiState> = _retryTrigger
        .flatMapLatest {
            apiConfigRepository.observeAllConfigs()
                .map { configs -> ApiConfigUiState(isLoading = false, configs = configs) }
                .catch { e ->
                    Timber.e(e, "load ApiConfigs failed")
                    emit(ApiConfigUiState(error = friendlyErrorMessage(e)))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ApiConfigUiState(isLoading = true),
        )

    /** 打开新建表单 */
    fun showAddForm() {
        editingId.value = null
        _formState.value = ApiConfigFormState()
        _isFormVisible.value = true
    }

    /** 打开编辑表单，预填已有配置 */
    fun showEditForm(config: ApiConfigEntity) {
        editingId.value = config.id
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
        editingId.value = null
        _formState.value = ApiConfigFormState()
    }

    /** 表单字段更新 */
    fun updateProvider(provider: String) {
        val preset = LlmProvider.fromKey(provider)
        _formState.update {
            it.copy(
                provider = provider,
                displayName = if (editingId.value == null) preset.displayName else it.displayName,
                baseUrl = if (editingId.value == null) preset.defaultBaseUrl else it.baseUrl,
                model = if (editingId.value == null) preset.defaultModel else it.model,
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
        // v0.8.16 P1-5 修复：baseUrl 格式校验，防止 Retrofit 构造时 IllegalArgumentException
        // 常见错误：用户复制 "api.deepseek.com" 而非 "https://api.deepseek.com"
        // Retrofit.Builder.baseUrl() 要求：
        // - 必须以 http:// 或 https:// 开头
        // - 必须以 / 结尾（AiServiceImpl 已补全，但协议缺失无法补全）
        val baseUrlError = validateBaseUrl(form.baseUrl)
        if (baseUrlError != null) {
            _errorMessage.value = baseUrlError
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

        // P1-NEW-5 修正：捕获 editingId 的瞬时值到局部量。
        // 原实现第 134 行 `val id = editingId ?: UUID...` 读取 editingId，
        // 第 154 行在 launch 协程内又读 `editingId.value == null`。
        // editingId 是可变 var，若用户在 launch 调度前点了 showAddForm（把 editingId 置 null），
        // launch 内读到的 editingId 已不是 saveConfig 调用时的值，导致：
        //   - 原本是编辑场景（id=已存在），但 launch 内 editingId==null 误判为新建，
        //     可能错误调用 setCurrent 把刚编辑的配置设为当前。
        // 现用局部 isNew 在调用时锁定语义，避免协程内外读取不一致。
        val isNew = editingId.value == null
        val id = editingId.value ?: UUID.randomUUID().toString()
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
                // 如果是新建且列表中无当前配置，自动设为当前（用局部 isNew 而非 editingId）
                if (isNew && uiState.value.currentConfigId == null) {
                    apiConfigRepository.setCurrent(id)
                }
                dismissForm()
            } catch (e: CancellationException) {
                throw e
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
            } catch (e: CancellationException) {
                throw e
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "删除失败：${e.message}"
            }
        }
    }

    /** 清除错误提示 */
    fun clearError() {
        _errorMessage.value = null
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────

    /**
     * 校验 baseUrl 格式（v0.8.16 P1-5 新增）。
     *
     * Retrofit.Builder.baseUrl() 在协议缺失时抛 IllegalArgumentException：
     * ```
     * java.lang.IllegalArgumentException: Illegal URL: api.deepseek.com
     *   at retrofit2.Retrofit$Builder.baseUrl(Retrofit.java:450)
     * ```
     * 此时 AiServiceImpl.createLlmApiService() 抛异常，被 chat() 的 catch 块吞掉
     * 显示 "AI 调用失败：Illegal URL: ..."，用户难以理解。
     *
     * 提前在保存配置时校验，给出友好错误提示。
     *
     * @return null 表示通过，非 null 为错误提示
     */
    private fun validateBaseUrl(baseUrl: String): String? {
        val trimmed = baseUrl.trim()
        return when {
            !trimmed.startsWith("http://") && !trimmed.startsWith("https://") -> {
                "接口地址必须以 http:// 或 https:// 开头（当前为 \"$trimmed\"）"
            }
            // 检查是否包含 host 部分（"http://" 后至少有一个字符）
            // 例如 "http://" 本身非法，"https:///" 也非法
            trimmed.removePrefix("http://").removePrefix("https://").isBlank() -> {
                "接口地址缺少域名（如 api.deepseek.com）"
            }
            else -> null
        }
    }
}

/**
 * API 配置 UI 状态。
 *
 * P1-3 新增 [error] 字段：数据流加载失败时携带错误信息，UI 据此提示用户。
 *
 * @param isLoading 加载中标记
 * @param configs 所有配置列表（apiKey 已解密，但 UI 层应掩码展示）
 * @param currentConfigId 当前选中配置 ID（null 表示无）
 * @param error 加载失败时的错误信息（P1-3 新增，与 [ApiConfigViewModel._errorMessage] 不同维度）
 */
data class ApiConfigUiState(
    val isLoading: Boolean = false,
    val configs: List<ApiConfigEntity> = emptyList(),
    /** 加载失败时的错误信息（P1-3 新增） */
    val error: String? = null,
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
