package com.wenyan.app.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenyan.app.core.ai.RagReference
import com.wenyan.app.core.database.WenyanDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * [ChatRepositoryImpl] 单元测试(NF-PP6 Wave 2.3)。
 *
 * 用 Robolectric + in-memory Room + 临时 DataStore 做真实持久化测试,验证:
 * - createConversation 返回非空 UUID,upsert 到 chat_conversations
 * - appendMessage 写入 chat_messages + touch 对话(updatedAt 推进 / messageCount++)
 * - deleteConversation 触发 FK CASCADE,级联删除消息
 * - loadOrInitCurrent:无历史返回 null;有历史返回最近对话 ID 并持久化到 DataStore
 * - setCurrentConversation(null) 后 currentConversationId 为 null
 *
 * 用 in-memory Room + 真实 DataStore(临时文件)的理由:
 * - ChatRepositoryImpl 内部组合 DAO 与 DataStore,真实组件能验证完整持久化链路
 * - 临时 DataStore 文件在 teardown 中删除,测试隔离不污染
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatRepositoryImplTest {

    private lateinit var db: WenyanDatabase
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: ChatRepositoryImpl
    private lateinit var dataStoreFile: File

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WenyanDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // 临时 DataStore 文件,与生产 DataStoreModule 的 wenyan_preferences.preferences_pb 区隔
        dataStoreFile = File(context.cacheDir, "test_chat_prefs.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { dataStoreFile },
        )

        repository = ChatRepositoryImpl(
            conversationDao = db.chatConversationDao(),
            messageDao = db.chatMessageDao(),
            preferencesDataStore = dataStore,
        )
    }

    @After
    fun teardown() {
        db.close()
        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }
    }

    /**
     * 场景 1:createConversation 返回非空 id,且能通过 getById 读回。
     *
     * 验证 createConversation 内部:
     * - UUID.randomUUID().toString() 生成非空 id
     * - conversationDao.upsert 将 ChatConversationEntity 写入 DB
     * - createdAt == updatedAt(新创建)
     */
    @Test
    fun `createConversation 返回非空 id 且能读回`() = runTest {
        val id = repository.createConversation(
            title = "苏轼研究",
            apiConfigId = "config_1",
            model = "gpt-4",
        )

        assertTrue("createConversation 应返回非空 id", id.isNotBlank())

        val conv = db.chatConversationDao().getById(id)
        assertNotNull("DB 应能读回对话", conv)
        assertEquals("苏轼研究", conv!!.title)
        assertEquals("config_1", conv.apiConfigId)
        assertEquals("gpt-4", conv.model)
        assertEquals("新对话 messageCount 应为 0", 0, conv.messageCount)
        assertEquals("新对话 createdAt == updatedAt", conv.createdAt, conv.updatedAt)
    }

    /**
     * 场景 2:appendMessage 后 observeMessages 反映新消息,对话 touch 更新计数与时间戳。
     *
     * 验证 appendMessage 内部:
     * - messageDao.insert 写入 chat_messages
     * - ChatMessageMapper.serializeReferences 序列化 references 为 JSON
     * - conversationDao.touch 让 messageCount++ 且 updatedAt 推进
     */
    @Test
    fun `appendMessage 后 observeMessages 反映新消息且对话计数更新`() = runTest {
        val convId = repository.createConversation("对话1", null, null)
        val convBefore = db.chatConversationDao().getById(convId)!!
        val tsBefore = convBefore.updatedAt

        // 等待 1ms 确保时间戳推进
        Thread.sleep(2)

        val references = listOf(
            RagReference(
                sourceFile = "袁行霈《中国文学史》",
                sourcePage = 156,
                contentSource = "TEXTBOOK_NATIVE",
                excerpt = "苏轼是北宋文学家",
            ),
        )
        val msgId = repository.appendMessage(
            conversationId = convId,
            role = "ASSISTANT",
            content = "苏轼(1037-1101)是北宋著名文学家",
            contentSource = "AI_REPLY",
            stage = "ANALYZE",
            references = references,
            contextScreen = "KnowledgePointDetail",
            contextTitle = "苏轼",
            tokensUsed = 100,
        )

        assertTrue("appendMessage 应返回非空 msgId", msgId.isNotBlank())

        val messages = db.chatMessageDao().getByConversation(convId)
        assertEquals("应有 1 条消息", 1, messages.size)
        assertEquals("msgId 一致", msgId, messages[0].id)
        assertEquals("ASSISTANT", messages[0].role)
        assertEquals("AI_REPLY", messages[0].contentSource)
        assertEquals("ANALYZE", messages[0].stage)
        assertEquals("KnowledgePointDetail", messages[0].contextScreen)
        assertEquals("苏轼", messages[0].contextTitle)
        assertEquals(100, messages[0].tokensUsed)
        assertNotNull("referencesJson 应非空", messages[0].referencesJson)
        assertTrue("referencesJson 应包含 sourceFile", messages[0].referencesJson!!.contains("袁行霈"))

        val convAfter = db.chatConversationDao().getById(convId)!!
        assertEquals("messageCount 应 +1", 1, convAfter.messageCount)
        assertTrue("updatedAt 应推进", convAfter.updatedAt > tsBefore)
    }

    /**
     * 场景 3:deleteConversation 级联删除消息(FK CASCADE)。
     *
     * 验证 deleteConversation 内部:
     * - conversationDao.deleteById 删除 chat_conversations 行
     * - FK ON DELETE CASCADE 自动删除 chat_messages 中 conversation_id 匹配的行
     * - 删除后 getById 返回 null,getByConversation 返回空列表
     */
    @Test
    fun `deleteConversation 级联删除消息`() = runTest {
        val convId = repository.createConversation("对话2", null, null)
        repository.appendMessage(convId, "USER", "问题1", "USER_INPUT", null, null, null, null, null)
        repository.appendMessage(convId, "ASSISTANT", "回答1", "AI_REPLY", null, null, null, null, 50)

        assertEquals("删除前应有 2 条消息", 2, db.chatMessageDao().countByConversation(convId))

        repository.deleteConversation(convId)

        assertNull("删除后对话应不存在", db.chatConversationDao().getById(convId))
        assertEquals("级联删除后消息应为 0", 0, db.chatMessageDao().countByConversation(convId))
        assertTrue("消息列表应为空", db.chatMessageDao().getByConversation(convId).isEmpty())
    }

    /**
     * 场景 4:loadOrInitCurrent 无历史对话时返回 null。
     *
     * 验证 loadOrInitCurrent 内部:
     * - DataStore 无记录 → conversationDao.getMostRecent() 返回 null → 返回 null
     */
    @Test
    fun `loadOrInitCurrent 无历史对话时返回 null`() = runTest {
        val result = repository.loadOrInitCurrent()
        assertNull("无历史对话时 loadOrInitCurrent 应返回 null", result)
        // DataStore 也应无记录
        assertNull(dataStore.data.first()[stringPreferencesKey("current_chat_conversation_id")])
    }

    /**
     * 场景 5:loadOrInitCurrent 有历史对话时返回最近对话 ID 并持久化到 DataStore。
     *
     * 验证 loadOrInitCurrent 内部:
     * - DataStore 无记录 + conversationDao.getMostRecent() 返回最近对话
     * - 自动选中最近对话 ID 并写入 DataStore
     */
    @Test
    fun `loadOrInitCurrent 有历史对话时返回最近对话 id 并持久化`() = runTest {
        val id1 = repository.createConversation("对话A", null, null)
        Thread.sleep(2)
        val id2 = repository.createConversation("对话B", null, null) // id2 创建更晚,应被选中

        val result = repository.loadOrInitCurrent()
        assertEquals("应返回最近创建的对话 id2", id2, result)

        // DataStore 应持久化 id2
        val stored = dataStore.data.first()[stringPreferencesKey("current_chat_conversation_id")]
        assertEquals("DataStore 应持久化 id2", id2, stored)

        // 再次 loadOrInitCurrent 应直接从 DataStore 读取(返回 id2,不重新推断)
        val result2 = repository.loadOrInitCurrent()
        assertEquals("第二次 loadOrInitCurrent 应直接读 DataStore 返回 id2", id2, result2)
    }

    /**
     * 场景 6:setCurrentConversation(null) 后 currentConversationId 为 null。
     *
     * 验证 setCurrentConversation(null) 内部:
     * - preferencesDataStore.edit 移除 KEY_CURRENT_CONVERSATION_ID
     * - currentConversationId Flow 随后发射 null
     *
     * 额外验证:先 set 一个 id,再 set null,验证从有到无的转换。
     */
    @Test
    fun `setCurrentConversation null 后 currentConversationId 为 null`() = runTest {
        val convId = repository.createConversation("对话C", null, null)

        // 先 set 一个有效 id
        repository.setCurrentConversation(convId)
        assertEquals(
            "currentConversationId 应为 convId",
            convId,
            repository.currentConversationId.first(),
        )

        // 再 set null
        repository.setCurrentConversation(null)
        assertNull(
            "set null 后 currentConversationId 应为 null",
            repository.currentConversationId.first(),
        )
    }
}
