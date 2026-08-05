package com.wenyan.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.wenyan.app.core.ai.RagReference
import com.wenyan.app.core.data.mapper.ChatMessageMapper
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.dao.ChatConversationDao
import com.wenyan.app.core.database.dao.ChatMessageDao
import com.wenyan.app.core.database.entity.ChatConversationEntity
import com.wenyan.app.core.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 对话仓库实现(NF-PP6 Wave 2.3)。
 *
 * 持久化 AI 对话历史到 chat_conversations + chat_messages 表,
 * 当前对话 ID 持久化到 DataStore。
 *
 * @property conversationDao 对话元数据 DAO
 * @property messageDao      消息内容 DAO
 * @property preferencesDataStore DataStore(持久化 currentConversationId)
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val database: WenyanDatabase,
    private val conversationDao: ChatConversationDao,
    private val messageDao: ChatMessageDao,
    private val preferencesDataStore: DataStore<Preferences>,
) : ChatRepository {

    override fun observeConversations(): Flow<List<ChatConversationEntity>> =
        conversationDao.observeAll()

    override fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>> =
        messageDao.observeByConversation(conversationId)

    override suspend fun getRecentMessages(
        conversationId: String,
        limit: Int,
    ): List<ChatMessageEntity> =
        // DAO 返回倒序（最新在前），这里 reverse 成时间正序供 LLM 注入
        messageDao.getRecentByConversation(conversationId, limit).reversed()

    override val currentConversationId: Flow<String?> =
        preferencesDataStore.data.map { it[KEY_CURRENT_CONVERSATION_ID] }

    override suspend fun createConversation(
        title: String,
        apiConfigId: String?,
        model: String?,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        conversationDao.upsert(
            ChatConversationEntity(
                id = id,
                title = title,
                apiConfigId = apiConfigId,
                model = model,
                messageCount = 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    override suspend fun appendMessage(
        conversationId: String,
        role: String,
        content: String,
        contentSource: String?,
        stage: String?,
        references: List<RagReference>?,
        contextScreen: String?,
        contextTitle: String?,
        tokensUsed: Int?,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val referencesJson = ChatMessageMapper.serializeReferences(references)

        // v0.9.24：insert + touch 合并为单个事务。
        // 原实现两个独立事务，touch 失败/进程中断会导致 message_count 与实际消息数不一致。
        database.withTransaction {
            messageDao.insert(
                ChatMessageEntity(
                    id = id,
                    conversationId = conversationId,
                    role = role,
                    content = content,
                    contentSource = contentSource,
                    stage = stage,
                    referencesJson = referencesJson,
                    contextScreen = contextScreen,
                    contextTitle = contextTitle,
                    tokensUsed = tokensUsed,
                    createdAt = now,
                ),
            )
            // 更新对话计数与时间戳(touch 内部 message_count + 1, updated_at = now)
            conversationDao.touch(conversationId, now)
            // v0.9.37 P1-7：会话消息保留上限——长对话超过 MAX_MESSAGES_PER_CONVERSATION
            // 时删除最旧超限消息并同步修正 message_count，防止 DB 无限膨胀
            // （历史消息仅在 UI 展示与 LLM 上下文注入时使用，最旧消息截断可接受）。
            enforceMessageCap(conversationId, now)
        }
        return id
    }

    /**
     * 会话消息保留上限（v0.9.37 P1-7）。
     *
     * 200 条 ≈ 100 轮问答，远超正常复习答疑会话长度；配合 LLM 上下文注入
     * 仅取最近 [ChatRepository.getRecentMessages] 的 limit（多轮 20 条），
     * 截断最旧消息不影响 AI 质量，仅限制本地 DB 体积。
     */
    private suspend fun enforceMessageCap(conversationId: String, now: Long) {
        val count = messageDao.countByConversation(conversationId)
        if (count <= MAX_MESSAGES_PER_CONVERSATION) return
        val overflow = count - MAX_MESSAGES_PER_CONVERSATION
        messageDao.deleteOldestByConversation(conversationId, overflow)
        conversationDao.setMessageCount(conversationId, MAX_MESSAGES_PER_CONVERSATION, now)
    }

    override suspend fun deleteConversation(id: String) {
        // FK CASCADE 自动删除 chat_messages 中 conversation_id = id 的所有消息
        conversationDao.deleteById(id)
    }

    override suspend fun setCurrentConversation(id: String?) {
        try {
            preferencesDataStore.edit { prefs ->
                if (id == null) {
                    prefs.remove(KEY_CURRENT_CONVERSATION_ID)
                } else {
                    prefs[KEY_CURRENT_CONVERSATION_ID] = id
                }
            }
        } catch (e: Exception) {
            // DataStore 写失败不冒泡（与 SeedDataLoader 的种子状态写入策略一致），
            // 下次启动 loadOrInitCurrent 会重新从 DB 推断
            // v0.8.21: Log.w → Timber.w（tag 自动推断）
            Timber.w(e, "setCurrentConversation failed for id=$id")
        }
    }

    override suspend fun loadOrInitCurrent(): String? {
        // 1. 先读 DataStore
        val stored = preferencesDataStore.data.first()[KEY_CURRENT_CONVERSATION_ID]
        if (stored != null) {
            // 验证存储的 ID 是否仍存在(对话可能已被删除)
            val exists = conversationDao.getById(stored) != null
            if (exists) {
                return stored
            }
            // 对话已删除,清除 DataStore 中的失效引用
            // v0.8.21: Log.i → Timber.i（tag 自动推断）
            Timber.i("Stored conversation id=$stored no longer exists, clearing")
            setCurrentConversation(null)
        }

        // 2. DataStore 无记录或失效 → 查最近对话
        val mostRecent = conversationDao.getMostRecent() ?: return null
        // 自动选中最近对话,持久化到 DataStore
        setCurrentConversation(mostRecent.id)
        return mostRecent.id
    }

    private companion object {
        private val KEY_CURRENT_CONVERSATION_ID = stringPreferencesKey("current_chat_conversation_id")

        /** 会话消息保留上限（v0.9.37 P1-7，200 条 ≈ 100 轮问答）。 */
        private const val MAX_MESSAGES_PER_CONVERSATION = 200
    }
}
