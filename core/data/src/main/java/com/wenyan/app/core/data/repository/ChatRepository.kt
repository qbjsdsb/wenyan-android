package com.wenyan.app.core.data.repository

import com.wenyan.app.core.ai.RagReference
import com.wenyan.app.core.database.entity.ChatConversationEntity
import com.wenyan.app.core.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI 对话仓库接口(NF-PP6 Wave 2.3)。
 *
 * 持久化 AI 对话历史,支持进程被杀后恢复 + 多会话切换。
 * 数据存储在 chat_conversations + chat_messages 两张表(Wave 1 合并自
 * 原 chat_history + ai_conversations 死代码表)。
 *
 * 当前会话 ID 持久化在 DataStore,跨进程恢复。
 *
 * 设计说明:
 * - 读 API(observe*)返回 [ChatConversationEntity] / [ChatMessageEntity](与
 *   [ApiConfigRepository] 一致,直接暴露 Entity)
 * - 写 API appendMessage 接收 domain 参数(role/references),内部构造 Entity
 *   并用 [com.wenyan.app.core.data.mapper.ChatMessageMapper] 序列化 references
 * - references 反序列化由调用方按需调用 ChatMessageMapper.deserializeReferences
 */
interface ChatRepository {

    /** 观察所有对话(按 updated_at DESC 排序) */
    fun observeConversations(): Flow<List<ChatConversationEntity>>

    /** 观察指定对话的消息列表(按 created_at ASC 排序) */
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    /** 当前选中的对话 ID(持久化在 DataStore,跨进程恢复) */
    val currentConversationId: Flow<String?>

    /**
     * 创建新对话。
     *
     * @param title       对话标题(用户可见)
     * @param apiConfigId API 配置 ID 快照(可为空)
     * @param model       模型名称快照(可为空)
     * @return 新对话的 ID(UUID)
     */
    suspend fun createConversation(
        title: String,
        apiConfigId: String?,
        model: String?,
    ): String

    /**
     * 追加消息到指定对话。
     *
     * 内部:序列化 references 为 JSON → 构造 [ChatMessageEntity] → insert →
     * 调用 [com.wenyan.app.core.database.dao.ChatConversationDao.touch] 更新计数与时间戳。
     *
     * @param conversationId 对话 ID
     * @param role           角色:USER / ASSISTANT / SYSTEM
     * @param content        消息文本内容
     * @param contentSource  内容来源:USER_INPUT / AI_REPLY / SYSTEM_PROMPT / RAG_QUOTED
     * @param stage          苏格拉底对话阶段(ANALYZE/SUGGEST/SHOW_SAMPLE,可为空)
     * @param references     RAG 引用列表(可为空)
     * @param contextScreen  发起对话时的屏幕快照(可为空)
     * @param contextTitle   发起对话时的标题快照(可为空)
     * @param tokensUsed     AI 回复消耗 token 数(可为空,仅 AI 回复有)
     * @return 新消息的 ID(UUID)
     */
    suspend fun appendMessage(
        conversationId: String,
        role: String,
        content: String,
        contentSource: String?,
        stage: String?,
        references: List<RagReference>?,
        contextScreen: String?,
        contextTitle: String?,
        tokensUsed: Int?,
    ): String

    /**
     * 删除对话(FK CASCADE 自动删除其所有消息)。
     *
     * @param id 对话 ID
     */
    suspend fun deleteConversation(id: String)

    /**
     * 设置当前对话 ID(持久化到 DataStore)。
     *
     * @param id 对话 ID,传 null 清除当前选择
     */
    suspend fun setCurrentConversation(id: String?)

    /**
     * 加载或初始化当前对话 ID。
     *
     * - DataStore 有记录:返回存储的 ID
     * - DataStore 无记录但有历史对话:返回最近对话 ID,并写入 DataStore
     * - DataStore 无记录且无历史对话:返回 null(由调用方决定是否创建新对话)
     *
     * @return 当前对话 ID,或 null(无历史对话)
     */
    suspend fun loadOrInitCurrent(): String?
}
