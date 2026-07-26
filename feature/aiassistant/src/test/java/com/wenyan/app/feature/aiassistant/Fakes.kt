package com.wenyan.app.feature.aiassistant

import com.wenyan.app.core.ai.AiService
import com.wenyan.app.core.ai.RagReference
import com.wenyan.app.core.data.repository.ChatRepository
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.ReviewLogDao
import com.wenyan.app.core.database.entity.ChatConversationEntity
import com.wenyan.app.core.database.entity.ChatMessageEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import com.wenyan.app.core.database.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * [AiService] 的 Fake 实现，供 [AiAssistantViewModelTest] 使用。
 *
 * - [response]：chat() 返回的内容
 * - [available]：isAvailable() 返回的值
 * - [throwException]：非 null 时 chat() 抛异常（测试异常处理）
 */
class FakeAiService(
    var response: String = "默认 AI 回复",
    var available: Boolean = true,
    var throwException: Throwable? = null,
) : AiService {

    override fun chat(query: String): Flow<String> = flow {
        throwException?.let { throw it }
        emit(response)
    }

    override fun chatResult(query: String): Flow<Result<String>> = flow {
        throwException?.let { emit(Result.failure(it)); return@flow }
        emit(Result.success(response))
    }

    override fun isAvailable(): Flow<Boolean> = flowOf(available)
}

/**
 * [KnowledgePointDao] 的 Fake 实现，供 [AiAssistantViewModelTest] 使用。
 *
 * 只实现 [searchByKeyword]，其余方法返回默认值。
 */
class FakeKnowledgePointDao(
    private val searchResults: List<KnowledgePointEntity> = emptyList(),
) : KnowledgePointDao {

    override suspend fun insert(entity: KnowledgePointEntity) {}
    override suspend fun insertAll(entities: List<KnowledgePointEntity>) {}
    override suspend fun update(entity: KnowledgePointEntity) {}
    override suspend fun deleteById(id: String) {}
    override suspend fun getById(id: String): KnowledgePointEntity? = null
    override suspend fun getByIds(ids: List<String>): List<KnowledgePointEntity> = emptyList()
    override fun observeById(id: String): Flow<KnowledgePointEntity?> = flowOf(null)
    override fun observeByChapter(chapterId: String): Flow<List<KnowledgePointEntity>> = flowOf(emptyList())
    override fun observeByExamFrequency(frequency: String): Flow<List<KnowledgePointEntity>> = flowOf(emptyList())
    override fun observeByOcrStatus(status: String): Flow<List<KnowledgePointEntity>> = flowOf(emptyList())
    override fun observeByContentSource(source: String): Flow<List<KnowledgePointEntity>> = flowOf(emptyList())
    override suspend fun countByChapter(chapterId: String): Int = 0
    override fun observeAll(): Flow<List<KnowledgePointEntity>> = flowOf(searchResults)
    override fun observeVerifiedForReview(): Flow<List<KnowledgePointEntity>> = flowOf(emptyList())
    override fun observeVerifiedWithSubject(): Flow<List<KnowledgePointWithSubject>> = flowOf(emptyList())
    override fun observeSearchWithSubject(keyword: String): Flow<List<KnowledgePointWithSubject>> = flowOf(emptyList())
    override suspend fun updateOcrStatus(id: String, status: String) {}

    override suspend fun searchByKeyword(keyword: String, limit: Int): List<KnowledgePointEntity> {
        return searchResults.filter { entity ->
            entity.title.contains(keyword, ignoreCase = true) ||
                entity.coreConclusion.contains(keyword, ignoreCase = true) ||
                entity.fullContent.contains(keyword, ignoreCase = true) ||
                (entity.studyText?.contains(keyword, ignoreCase = true) ?: false)
        }.take(limit)
    }
}

/**
 * [ReviewLogDao] 的 Fake 实现，供 [AiAssistantViewModelTest] 使用。
 */
class FakeReviewLogDao(
    initialLogs: List<ReviewLogEntity> = emptyList(),
) : ReviewLogDao {

    private val store = mutableMapOf<String, MutableList<ReviewLogEntity>>()

    init {
        for (log in initialLogs) {
            store.getOrPut(log.pointId) { mutableListOf() }.add(log)
        }
    }

    override suspend fun insert(entity: ReviewLogEntity) {
        store.getOrPut(entity.pointId) { mutableListOf() }.add(entity)
    }

    override suspend fun insertAll(entities: List<ReviewLogEntity>) {
        for (entity in entities) {
            insert(entity)
        }
    }

    override suspend fun deleteById(id: String) {
        store.values.forEach { list -> list.removeAll { it.id == id } }
    }

    override suspend fun getById(id: String): ReviewLogEntity? {
        return store.values.flatten().firstOrNull { it.id == id }
    }

    override fun observeByPoint(pointId: String): Flow<List<ReviewLogEntity>> {
        return flowOf(store[pointId]?.sortedByDescending { it.createdAt }?.toList() ?: emptyList())
    }

    override fun observeAll(): Flow<List<ReviewLogEntity>> {
        return flowOf(store.values.flatten().sortedByDescending { it.createdAt })
    }

    override suspend fun countByPoint(pointId: String): Int {
        return store[pointId]?.size ?: 0
    }

    override suspend fun getByPointOrderByCreatedDesc(pointId: String): List<ReviewLogEntity> {
        return store[pointId]?.sortedByDescending { it.createdAt }?.toList() ?: emptyList()
    }

    override suspend fun getByPointIds(pointIds: List<String>): List<ReviewLogEntity> {
        return store.filterKeys { it in pointIds }.values.flatten()
    }
}

/**
 * [ChatRepository] 的 Fake 实现,供 [AiAssistantViewModelTest] 使用(NF-PP6 Wave 3.1)。
 *
 * 内存模拟 chat_conversations + chat_messages 两表 + DataStore currentId。
 *
 * - [initialConversations] / [initialMessages]:构造时预设历史(测试进程重启恢复场景)
 * - [appendedMessages]:记录所有 appendMessage 调用(按顺序)
 * - [deletedConversationIds]:记录所有 deleteConversation 调用
 * - [currentId]:当前选中对话 ID(可读可写)
 */
class FakeChatRepository(
    initialConversations: List<ChatConversationEntity> = emptyList(),
    initialMessages: List<ChatMessageEntity> = emptyList(),
) : ChatRepository {

    private val conversations = mutableMapOf<String, ChatConversationEntity>()
    private val messagesByConv = mutableMapOf<String, MutableList<ChatMessageEntity>>()
    private val _currentId = MutableStateFlow<String?>(null)

    val appendedMessages: MutableList<ChatMessageEntity> = mutableListOf()
    val deletedConversationIds: MutableList<String> = mutableListOf()
    val setCurrentCalls: MutableList<String?> = mutableListOf()
    val currentId: String? get() = _currentId.value

    init {
        for (conv in initialConversations) {
            conversations[conv.id] = conv
        }
        for (msg in initialMessages) {
            messagesByConv.getOrPut(msg.conversationId) { mutableListOf() }.add(msg)
        }
        // 如果有初始对话,默认选中第一个(模拟 loadOrInitCurrent 行为)
        if (initialConversations.isNotEmpty()) {
            _currentId.value = initialConversations.first().id
        }
    }

    override fun observeConversations(): Flow<List<ChatConversationEntity>> =
        flowOf(conversations.values.sortedByDescending { it.updatedAt })

    override fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>> =
        flowOf(messagesByConv[conversationId]?.sortedBy { it.createdAt } ?: emptyList())

    override val currentConversationId: Flow<String?> = _currentId.asStateFlow()

    override suspend fun createConversation(
        title: String,
        apiConfigId: String?,
        model: String?,
    ): String {
        val id = "conv_${conversations.size + 1}_${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()
        conversations[id] = ChatConversationEntity(
            id = id,
            title = title,
            apiConfigId = apiConfigId,
            model = model,
            messageCount = 0,
            createdAt = now,
            updatedAt = now,
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
        val id = "msg_${messagesByConv.values.sumOf { it.size } + 1}_${System.currentTimeMillis()}"
        val msg = ChatMessageEntity(
            id = id,
            conversationId = conversationId,
            role = role,
            content = content,
            contentSource = contentSource,
            stage = stage,
            referencesJson = null, // Fake 不序列化,测试不验证 JSON
            contextScreen = contextScreen,
            contextTitle = contextTitle,
            tokensUsed = tokensUsed,
            createdAt = System.currentTimeMillis(),
        )
        messagesByConv.getOrPut(conversationId) { mutableListOf() }.add(msg)
        appendedMessages.add(msg)
        conversations[conversationId]?.let { conv ->
            conversations[conversationId] = conv.copy(
                messageCount = conv.messageCount + 1,
                updatedAt = msg.createdAt,
            )
        }
        return id
    }

    override suspend fun deleteConversation(id: String) {
        deletedConversationIds.add(id)
        conversations.remove(id)
        messagesByConv.remove(id)
        if (_currentId.value == id) {
            _currentId.value = null
        }
    }

    override suspend fun setCurrentConversation(id: String?) {
        setCurrentCalls.add(id)
        _currentId.value = id
    }

    override suspend fun loadOrInitCurrent(): String? {
        // 如果已有 currentId 且存在,返回
        val cur = _currentId.value
        if (cur != null && conversations.containsKey(cur)) return cur
        // 否则查最近对话
        val mostRecent = conversations.values.maxByOrNull { it.updatedAt }
        if (mostRecent != null) {
            _currentId.value = mostRecent.id
            return mostRecent.id
        }
        return null
    }
}
