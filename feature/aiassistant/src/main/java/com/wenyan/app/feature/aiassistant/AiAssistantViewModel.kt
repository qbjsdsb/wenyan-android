package com.wenyan.app.feature.aiassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI 助手模块 ViewModel。
 *
 * 管理 UI 状态：对话消息列表 + 输入框 + 加载状态。
 * 后续接入 core:ai 模块实现苏格拉底式引导 + RAG 检索。
 */
@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    // TODO: 注入 AiRepository（core:ai 封装）
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    // 发送用户消息
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = AiMessage(
            id = System.currentTimeMillis().toString(),
            role = AiRole.USER,
            content = text,
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // TODO: aiRepository.chat(messages) —— 苏格拉底式引导 + RAG
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // 更新输入框文本
    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }
}

// AI 助手 UI 状态
data class AiAssistantUiState(
    val messages: List<AiMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
)

// 对话消息
data class AiMessage(
    val id: String,
    val role: AiRole,
    val content: String,
)

// 消息角色
enum class AiRole {
    USER,
    ASSISTANT,
}
