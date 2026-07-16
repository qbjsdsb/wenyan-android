# P1 大型任务规划

> **基于代码调研的严谨规划。5 个 P1 任务(NF-T4 / NF-PP4 / NF-PP5 / NF-PP6 / P1-PG-1/2/3)的完整执行方案。**
>
> 创建:2026-07-16
> 前置:Release v0.5.0 已发布(commit `70a474d`,2026-07-16 16:43 UTC)
> 数据库版本:当前 v4 → 目标 v5(单次合并 Migration)
> 测试基线:220 tests 0 failures(目标:Wave 完成后 240+ tests)

## 0. 用户决策(已确认)

| 决策点 | 选择 | 影响 |
|--------|------|------|
| **schema 策略** | 合并单次 Migration 4→5 | 所有 schema 变化一次性完成,版本号简单 |
| **NF-PP5 范围** | 完整版:Quiz 答题判定 + Cards AGAIN 双来源 | 需扩展 QuizScreen 答题交互 UI |
| **NF-PP6 UI** | 完整版:加历史对话 Drawer/Sheet | AiAssistantScreen 加多会话切换 |
| **R8 时机** | 先写规则文件但暂不启用 | 13 个 .pro 补齐规则,`isMinifyEnabled=false` 保持 |

## 1. 任务依赖关系图

```
Wave 1: 数据库 schema 准备(统一 Migration_4_5)
   ├─ NF-T4 类型统一(可能无 schema 变化)
   ├─ NF-PP4 删 history 列(重建 memo_records 表)
   ├─ NF-PP6 合并 chat_history + ai_conversations → chat_conversations + chat_messages
   └─ NF-PP5 新增 wrong_answers 表
                ↓
Wave 2: 数据层实现(并行,4 个任务独立)
   ├─ NF-T4: Entity 类型 + Mapper 简化 + 测试
   ├─ NF-PP4: Mapper/SchedulingRepository 简化 + 测试
   ├─ NF-PP6: ChatRepository + ChatMessageMapper + DI
   └─ NF-PP5: WrongAnswerRepository + DI
                ↓
Wave 3: 业务层集成(部分并行)
   ├─ NF-PP6: AiAssistantViewModel 改造 + Screen 加 Drawer(独立)
   └─ NF-PP5: CardsViewModel 集成 + QuizScreen 扩展答题 + WrongAnswerScreen(独立)
                ↓
Wave 4: R8 规则准备(独立)
   └─ 13 个 .pro 文件补齐规则,不启用 minify
                ↓
Wave 5: 全量验证 + 文档
   ├─ assembleDebug + testDebugUnitTest(目标 240+ tests)
   ├─ Migration 测试(MigrationTestHelper)
   ├─ 文档:00-STATUS + SESSION_LOG + 设计文档
   └─ 准备 Release v0.6.0
```

## 2. Wave 1:数据库 schema 统一迁移

### 2.1 schema 变化清单(单次 Migration_4_5)

| 任务 | 变化 | SQL 操作 |
|------|------|---------|
| **NF-T4** | Entity 类型 Double→Float | **可能无 schema 变化**(SQLite REAL 亲和相同),实测验证 |
| **NF-PP4** | 删 `memo_records.history` 列 | SQLite 不支持 DROP COLUMN → 重建表(5 步) |
| **NF-PP6** | 合并 chat_history + ai_conversations → chat_conversations + chat_messages | DROP 2 旧表 + CREATE 2 新表 + 索引 |
| **NF-PP5** | 新增 wrong_answers 表 | CREATE TABLE + 索引 |

### 2.2 Migration_4_5.kt 实施步骤

**文件**:`/workspace/core/database/src/main/java/com/wenyan/app/core/database/migration/Migration_4_5.kt`(新建)

```kotlin
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ============ Part A: NF-PP4 删除 memo_records.history 列 ============
        // SQLite 不支持 DROP COLUMN,需重建表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS memo_records_new (
                `point_id` TEXT NOT NULL PRIMARY KEY,
                `state` TEXT NOT NULL,
                `stability` REAL NOT NULL DEFAULT 0.0,
                `difficulty` REAL NOT NULL DEFAULT 5.0,
                `last_review_at` INTEGER NOT NULL,
                `next_review_at` INTEGER NOT NULL,
                `review_count` INTEGER NOT NULL DEFAULT 0,
                `fail_count` INTEGER NOT NULL DEFAULT 0,
                `elapsed_days` INTEGER NOT NULL DEFAULT 0,
                `scheduled_days` INTEGER NOT NULL DEFAULT 0,
                `reps` INTEGER NOT NULL DEFAULT 0,
                `in_priority_queue` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`point_id`) REFERENCES `knowledge_points`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("""
            INSERT INTO memo_records_new (
                point_id, state, stability, difficulty, last_review_at,
                next_review_at, review_count, fail_count, elapsed_days,
                scheduled_days, reps, in_priority_queue
            )
            SELECT point_id, state, stability, difficulty, last_review_at,
                   next_review_at, review_count, fail_count, elapsed_days,
                   scheduled_days, reps, in_priority_queue
            FROM memo_records
        """.trimIndent())
        database.execSQL("DROP TABLE memo_records")
        database.execSQL("ALTER TABLE memo_records_new RENAME TO memo_records")
        // 重建索引(原表如有)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_memo_records_next_review_at ON memo_records(next_review_at)")

        // ============ Part B: NF-PP6 合并 chat_history + ai_conversations ============
        // 1. 创建新表 chat_conversations(对话元数据)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS chat_conversations (
                `id` TEXT NOT NULL PRIMARY KEY,
                `title` TEXT NOT NULL,
                `api_config_id` TEXT,
                `model` TEXT,
                `message_count` INTEGER NOT NULL DEFAULT 0,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
        """.trimIndent())
        // 2. 创建新表 chat_messages(消息内容,FK→对话)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS chat_messages (
                `id` TEXT NOT NULL PRIMARY KEY,
                `conversation_id` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `content_source` TEXT,
                `stage` TEXT,
                `references_json` TEXT,
                `context_screen` TEXT,
                `context_title` TEXT,
                `tokens_used` INTEGER,
                `created_at` INTEGER NOT NULL,
                FOREIGN KEY(`conversation_id`) REFERENCES `chat_conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_conversation_id ON chat_messages(conversation_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_conversations_updated_at ON chat_conversations(updated_at)")
        // 3. 迁移存量数据(预期 0 行,因两表无 Repository 写入)
        //    用 chat_history + ai_conversations 的并集填充 migrated_legacy 对话
        database.execSQL("""
            INSERT OR IGNORE INTO chat_conversations (id, title, api_config_id, model, message_count, created_at, updated_at)
            SELECT 'migrated_legacy', '历史对话(迁移)', NULL, NULL, 0, 0, 0
            WHERE EXISTS (SELECT 1 FROM chat_history LIMIT 1)
               OR EXISTS (SELECT 1 FROM ai_conversations LIMIT 1)
        """.trimIndent())
        // 4. 删除旧表
        database.execSQL("DROP TABLE IF EXISTS chat_history")
        database.execSQL("DROP TABLE IF EXISTS ai_conversations")

        // ============ Part C: NF-PP5 新增 wrong_answers 表 ============
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS wrong_answers (
                `id` TEXT NOT NULL PRIMARY KEY,
                `point_id` TEXT,
                `exam_question_id` TEXT,
                `user_answer` TEXT NOT NULL,
                `correct_answer` TEXT,
                `source` TEXT NOT NULL,
                `wrong_count` INTEGER NOT NULL DEFAULT 1,
                `last_wrong_at` INTEGER NOT NULL,
                `resolved_at` INTEGER,
                `ai_explanation` TEXT,
                `created_at` INTEGER NOT NULL,
                FOREIGN KEY(`point_id`) REFERENCES `knowledge_points`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`exam_question_id`) REFERENCES `exam_questions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_wrong_answers_point_id ON wrong_answers(point_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_wrong_answers_exam_question_id ON wrong_answers(exam_question_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_wrong_answers_resolved_at ON wrong_answers(resolved_at)")
    }
}
```

### 2.3 WenyanDatabase.kt 改动

**文件**:`/workspace/core/database/src/main/java/com/wenyan/app/core/database/WenyanDatabase.kt`

```kotlin
@Database(
    entities = [
        // ... 既有 18 张表(不含 chat_history / ai_conversations)...
        ChatConversationEntity::class,   // 新增(替代 ChatHistoryEntity + AiConversationEntity)
        ChatMessageEntity::class,        // 新增
        WrongAnswerEntity::class,        // 新增
        // 移除:ChatHistoryEntity::class, AiConversationEntity::class
    ],
    version = 5,  // 4 → 5
    exportSchema = true,
)
abstract class WenyanDatabase : RoomDatabase() {
    // 移除:abstract fun chatHistoryDao(): ChatHistoryDao
    // 移除:abstract fun aiConversationDao(): AiConversationDao
    abstract fun chatConversationDao(): ChatConversationDao   // 新增
    abstract fun chatMessageDao(): ChatMessageDao             // 新增
    abstract fun wrongAnswerDao(): WrongAnswerDao             // 新增
}
```

### 2.4 DatabaseModule.kt 改动

```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)  // 追加 MIGRATION_4_5

// 移除:provideChatHistoryDao, provideAiConversationDao
// 新增:
@Provides
fun provideChatConversationDao(database: WenyanDatabase): ChatConversationDao = database.chatConversationDao()

@Provides
fun provideChatMessageDao(database: WenyanDatabase): ChatMessageDao = database.chatMessageDao()

@Provides
fun provideWrongAnswerDao(database: WenyanDatabase): WrongAnswerDao = database.wrongAnswerDao()
```

### 2.5 schema 导出

运行 `assembleDebug` 后,Room 会自动生成 `/workspace/core/database/schemas/com.wenyan.app.core.database.WenyanDatabase/5.json`,需提交到 git。

## 3. Wave 2:数据层实现(4 个任务并行)

### 3.1 NF-T4:Float 类型统一

**目标**:Entity 字段 Double → Float,Mapper 转换变恒等,与 FSRS-6 官方 Float 实现对齐。

**修改文件**(4 个):

| 文件 | 改动 |
|------|------|
| `core/database/.../entity/MemoRecordEntity.kt` | `stability: Double = 0.0` → `Float = 0f`;`difficulty: Double = 5.0` → `Float = 5f` |
| `core/database/.../entity/ReviewLogEntity.kt` | `stability: Double?` → `Float?`;`difficulty: Double?` → `Float?` |
| `core/data/.../mapper/MemoRecordMapper.kt` | 删 `.toFloat()`(L65-66)+ `.toDouble()`(L115-116) |
| `core/data/.../repository/SchedulingRepository.kt` | 删 `reviewLog.stability.toDouble()`(L142-143)+ `createDefaultMemoRecord` 默认值 `0.0` → `0f` / `5.0` → `5f` |

**新增测试**:`core/data/src/test/java/com/wenyan/app/core/data/mapper/MemoRecordMapperTest.kt`(当前 0 测试覆盖)
- round-trip 一致性(`toMemoRecord(toFlashCard(e))` 对 stability/difficulty 字段)
- 边界值(stability=0、stability=720、difficulty=1、difficulty=10)
- 默认值正确性

**风险**:🟢 低(SQLite REAL 不变,无数据损失)

### 3.2 NF-PP4:复习日志双写统一

**目标**:废弃 `memo_records.history` JSON 字段,统一用 `review_logs` 表。

**修改文件**(3 个):

| 文件 | 改动 |
|------|------|
| `core/database/.../entity/MemoRecordEntity.kt` | 删 `history: String?` 字段(L80-82) |
| `core/data/.../mapper/MemoRecordMapper.kt` | 删 `appendReviewLog`(L132-153)+ `formatReviewLogJson`(L158-174);从 `toMemoRecord` 签名删 `reviewLog` / `existingHistoryJson` 参数 |
| `core/data/.../repository/SchedulingRepository.kt` | `toMemoRecord` 调用不传 reviewLog / existingHistoryJson(L120-126);`reviewLogDao.insert(...)` 保留(L134-147) |

**新增测试**:`core/data/src/test/java/com/wenyan/app/core/data/repository/SchedulingRepositoryTest.kt`(当前 0 测试覆盖)
- `rateCard(AGAIN)` 后,review_logs 表有 1 条 rating=AGAIN 记录
- `rateCard(GOOD)` 后,memo_records 表 stability 更新,history 字段不存在
- 事务一致性:模拟 insert 失败,memo_records 与 review_logs 都不写入

**风险**:🟡 中(改动核心调度逻辑,需保证 AntiRoteMemorization 消费方正常)

### 3.3 NF-PP6:AI 消息持久化数据层

**目标**:激活已存在的死代码表(替换为合并后的 chat_conversations + chat_messages),实现 ChatRepository。

**新增文件**(7 个):

1. `core/database/.../entity/ChatConversationEntity.kt`(新,替代 ChatHistoryEntity + AiConversationEntity)
2. `core/database/.../entity/ChatMessageEntity.kt`(新)
3. `core/database/.../dao/ChatConversationDao.kt`(新)
4. `core/database/.../dao/ChatMessageDao.kt`(新)
5. `core/data/.../repository/ChatRepository.kt`(新接口)
6. `core/data/.../repository/ChatRepositoryImpl.kt`(新实现)
7. `core/data/.../mapper/ChatMessageMapper.kt`(新,AiMessage ↔ Entity + references JSON 序列化)

**删除文件**(4 个):
- `core/database/.../entity/ChatHistoryEntity.kt`
- `core/database/.../entity/AiConversationEntity.kt`
- `core/database/.../dao/ChatHistoryDao.kt`
- `core/database/.../dao/AiConversationDao.kt`

**ChatRepository 接口设计**(按审计文档 1.C.4 节):

```kotlin
interface ChatRepository {
    fun observeConversations(): Flow<List<ChatConversation>>
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    val currentConversationId: Flow<String?>
    suspend fun createConversation(title: String, apiConfigId: String?, model: String?): String
    suspend fun appendMessage(conversationId: String, message: ChatMessage)
    suspend fun deleteConversation(id: String)
    suspend fun setCurrentConversation(id: String?)
    suspend fun loadOrInitCurrent(): String?
}
```

**关键实现要点**:
- 注入 `ChatConversationDao` + `ChatMessageDao` + `DataStore<Preferences>`(持久化 `currentConversationId`)
- `appendMessage` 内部:upsert message + `conversationDao.touch()` 更新 count/updatedAt
- `references: List<RagReference>` 用 `kotlinx.serialization` JSON 序列化(需检查 `RagReference` 是否 `@Serializable`,若否则补注解)
- `stage: SocraticStage?` 用 `enum.name` 序列化

**新增 Hilt 绑定**:在 `core/data/.../di/DataModule.kt` 加 `@Binds fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository`

**新增测试**:`core/data/src/test/java/com/wenyan/app/core/data/repository/ChatRepositoryImplTest.kt`
- `createConversation` 返回非空 id
- `appendMessage` 后 `observeMessages` 反映新消息
- `deleteConversation` 级联删除消息(FK CASCADE)
- `loadOrInitCurrent` 无历史时返回 null,有历史时返回 last conversation id
- `setCurrentConversation(null)` 后 `currentConversationId` 为 null

**风险**:🟡 中(改造面大,但有审计文档指引)

### 3.4 NF-PP5:错题本数据层

**目标**:新增 wrong_answers 表 + DAO + Repository。

**新增文件**(4 个):

1. `core/database/.../entity/WrongAnswerEntity.kt`(新)
2. `core/database/.../dao/WrongAnswerDao.kt`(新)
3. `core/data/.../repository/WrongAnswerRepository.kt`(新接口)
4. `core/data/.../repository/WrongAnswerRepositoryImpl.kt`(新实现)

**WrongAnswerEntity 设计**:

```kotlin
@Entity(
    tableName = "wrong_answers",
    foreignKeys = [
        ForeignKey(entity = KnowledgePointEntity::class, parentColumns = ["id"], childColumns = ["point_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExamQuestionEntity::class, parentColumns = ["id"], childColumns = ["exam_question_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("point_id"), Index("exam_question_id"), Index("resolved_at")],
)
data class WrongAnswerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "point_id") val pointId: String?,              // 卡片来源(可为空)
    @ColumnInfo(name = "exam_question_id") val examQuestionId: String?,  // 真题来源(可为空)
    @ColumnInfo(name = "user_answer") val userAnswer: String,
    @ColumnInfo(name = "correct_answer") val correctAnswer: String?,
    @ColumnInfo(name = "source") val source: String,                  // CARD_AGAIN / QUIZ_WRONG
    @ColumnInfo(name = "wrong_count") val wrongCount: Int = 1,
    @ColumnInfo(name = "last_wrong_at") val lastWrongAt: Long,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long?,          // null = 未解决
    @ColumnInfo(name = "ai_explanation") val aiExplanation: String?,  // AI 解释(可选)
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
```

**WrongAnswerRepository 接口**:

```kotlin
interface WrongAnswerRepository {
    fun observeAll(): Flow<List<WrongAnswer>>
    fun observeUnresolved(): Flow<List<WrongAnswer>>
    fun observeByPoint(pointId: String): Flow<List<WrongAnswer>>
    fun observeByExamQuestion(examQuestionId: String): Flow<List<WrongAnswer>>
    suspend fun recordWrongAnswer(
        pointId: String?, examQuestionId: String?,
        userAnswer: String, correctAnswer: String?,
        source: WrongAnswerSource,
    ): String  // 返回 id(新插入或已有记录递增 wrong_count)
    suspend fun markResolved(id: String)
    suspend fun deleteById(id: String)
    suspend fun countUnresolved(): Int
}
```

**新增 Hilt 绑定**:在 `core/data/.../di/DataModule.kt` 加 `@Binds fun bindWrongAnswerRepository(impl: WrongAnswerRepositoryImpl): WrongAnswerRepository`

**新增测试**:`core/data/src/test/java/com/wenyan/app/core/data/repository/WrongAnswerRepositoryImplTest.kt`
- `recordWrongAnswer` 新插入返回非空 id
- 同一 pointId 重复记录,`wrong_count` 递增
- `markResolved` 后 `observeUnresolved` 不返回该项
- `deleteById` 后 observeAll 不返回该项
- `observeByPoint` 按 pointId 筛选正确

**风险**:🟢 低(新功能,不影响现有逻辑)

## 4. Wave 3:业务层集成

### 4.1 NF-PP6:AiAssistantViewModel 改造 + Screen 加 Drawer

**修改文件**(3 个):

| 文件 | 改动 |
|------|------|
| `feature/aiassistant/.../AiAssistantViewModel.kt` | `@Inject constructor` 加 `chatRepository: ChatRepository`;`uiState` 改为 `chatRepository.currentConversationId.flatMapLatest { observeMessages }.stateIn(...)`;`sendMessage` 先 appendMessage(userMsg),AI 返回后 appendMessage(assistantMsg);`clearMessages()` 改为 `deleteConversation(currentId) + setCurrentConversation(null)`;`init` 加 `loadOrInitCurrent()` |
| `feature/aiassistant/.../AiAssistantScreen.kt` | 顶部加历史对话 Drawer/Sheet 入口;对话列表项(title + updatedAt + messageCount);"新建对话"按钮 |
| `feature/aiassistant/src/test/.../Fakes.kt` | 加 `FakeChatRepository`(实现 ChatRepository 接口,内存模拟) |

**ViewModel 改造核心代码**(按审计文档 1.C.5):

```kotlin
val uiState: StateFlow<AiAssistantUiState> = chatRepository.currentConversationId
    .flatMapLatest { convId ->
        if (convId != null) {
            chatRepository.observeMessages(convId).map { msgs ->
                AiAssistantUiState(messages = msgs.map { it.toAiMessage() })
            }
        } else {
            flowOf(AiAssistantUiState())
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiAssistantUiState())

init { viewModelScope.launch { chatRepository.loadOrInitCurrent() } }
```

**AiAssistantViewModelTest 适配**:
- 现有 18 个测试需注入 `FakeChatRepository`
- 验证消息状态的断言改为通过 `FakeChatRepository.appendedMessages` 验证

**风险**:🟡 中(`flatMapLatest` + `stateIn` 5 秒订阅延迟,需 `testScope.runTest` 配合)

### 4.2 NF-PP5:CardsViewModel 集成 + QuizScreen 扩展答题 + WrongAnswerScreen

**修改文件**(5 个):

| 文件 | 改动 |
|------|------|
| `feature/cards/.../CardsViewModel.kt` | `@Inject constructor` 加 `wrongAnswerRepository`;`rateCard(AGAIN)` 时调 `wrongAnswerRepository.recordWrongAnswer(pointId=..., userAnswer=卡片背面, correctAnswer=卡片正面, source=CARD_AGAIN)` |
| `feature/quiz/.../QuizScreen.kt` | `AnswerSection` 扩展:加用户答题输入框 + 提交按钮 + 判定对错逻辑(对照参考答案或 AI 批改) |
| `feature/quiz/.../QuizViewModel.kt` | `QuizUiState` 加 `userAnswer: String` / `isAnswered: Boolean` / `isCorrect: Boolean`;`submitAnswer()` 方法判定对错;答错时调 `wrongAnswerRepository.recordWrongAnswer(examQuestionId=..., source=QUIZ_WRONG)` |
| `feature/quiz/src/test/.../QuizViewModelTest.kt` | 新增测试:答错时插入错题本,答对时不插入 |

**新增文件**(3 个):

1. `feature/wronganswer/src/main/java/com/wenyan/app/feature/wronganswer/WrongAnswerScreen.kt`(新模块,或放 feature/quiz 下)
2. `feature/wronganswer/src/main/java/com/wenyan/app/feature/wronganswer/WrongAnswerViewModel.kt`
3. `feature/wronganswer/src/main/java/com/wenyan/app/feature/wronganswer/WrongAnswerUiState.kt`

**WrongAnswerScreen UI**:
- 顶部 WenyanLargeTopAppBar("错题本")
- LazyColumn 列表项:卡片来源 vs 真题来源用 icon 区分;显示 userAnswer(截断) + wrongCount + lastWrongAt(相对时间)
- 点击列表项:展开详情(userAnswer + correctAnswer + aiExplanation)
- "重做"按钮:跳转回 CardsScreen 或 QuizScreen
- "标记已解决"按钮:调 `markResolved(id)`

**导航入口**:
- **方案**:QuizScreen TopBar 加"错题本"图标(Similar to AI 助手 SmartToy 模式),Push 进入 `WrongAnswerScreen`
- **不新增 Tab**(保持 5 Tab 布局)
- 在 `WenyanNavHost.kt` 注册子路由(参考 L191-207 `aiAssistantDestination` 模板,用 `WenyanMotion.PushEnterTransition`)

**新增测试**:
- `feature/wronganswer/src/test/.../WrongAnswerViewModelTest.kt`(新)
- `feature/quiz/src/test/.../QuizViewModelTest.kt` 扩展(答错时插入错题本)
- `feature/cards/src/test/.../CardsViewModelTest.kt` 扩展(rateCard(AGAIN) 时插入错题本)

**风险**:🟢 低(新功能,扩展而非重构)

## 5. Wave 4:R8 规则准备

**目标**:补齐 13 个 .pro 文件规则,**不启用** `isMinifyEnabled`。等 emulator 实测后再启用。

**修改文件**(13 个 .pro + 0 个 build.gradle.kts):

| 文件 | 规则内容 |
|------|---------|
| `app/proguard-rules.pro` | 通用 keep 规则 + Hilt Application/Activity keep + Compose 通用 |
| `core/ai/consumer-rules.pro` | Retrofit interface keep + 6 个 LlmDtos @Serializable keep + OkHttp 通用 |
| `core/data/consumer-rules.pro` | 6 个 SeedDataLoader @Serializable 类 keep + ApiConfigRepository keep |
| `core/database/consumer-rules.pro` | Room @Entity/@Dao/@Database keep + WenyanTypeConverters keep + 20 个 Entity 通用规则 |
| `core/fsrs/consumer-rules.pro` | FSRS 数据类 keep(FlashCard / ReviewLog / Rating enum) |
| `core/common/consumer-rules.pro` | (无特殊规则,保持占位) |
| `core/designsystem/consumer-rules.pro` | (无特殊规则,保持占位) |
| `feature/aiassistant/consumer-rules.pro` | AiAssistantViewModel @HiltViewModel keep |
| `feature/cards/consumer-rules.pro` | CardsViewModel @HiltViewModel keep |
| `feature/graph/consumer-rules.pro` | GraphViewModel @HiltViewModel keep |
| `feature/knowledge/consumer-rules.pro` | KnowledgeViewModel @HiltViewModel keep |
| `feature/quiz/consumer-rules.pro` | QuizViewModel @HiltViewModel keep |
| `feature/settings/consumer-rules.pro` | SettingsViewModel + ThemeViewModel @HiltViewModel keep |

**通用 keep 规则模板**:

```proguard
# ============ kotlinx.serialization ============
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class **.$$serializer {
    *;
}

# ============ Hilt ============
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# ============ Room ============
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class *
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.TypeConverter class *

# ============ Retrofit ============
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepattributes Signature, Exceptions
-keep @retrofit2.http.* interface * { *; }
```

**风险**:🟢 低(仅写规则,不启用,不影响构建)

## 6. Wave 5:全量验证 + 文档

### 6.1 验证步骤

1. **本地编译验证**:`CI=false gradle assembleDebug --no-daemon`(必须 SUCCESSFUL)
2. **单元测试**:`CI=false gradle testDebugUnitTest --no-daemon`(目标 240+ tests 0 failures)
3. **Migration 测试**:用 Room `MigrationTestHelper` 验证 v4→v5 数据迁移正确性
4. **lint 验证**:`CI=false gradle lintDebug`(沙箱可能因 Java 17 兼容性失败,CI 环境无此问题)

### 6.2 文档更新

| 文件 | 更新内容 |
|------|---------|
| `docs/00-STATUS.md` | 当前状态改为"v0.6.0 P1 大型任务完成";已交付加上 NF-T4/PP4/PP5/PP6/R8 规则 |
| `docs/SESSION_LOG.md` | 新增会话节,记录 P1 大型任务完整实施过程 |
| `docs/design/wrong-answer-book.md`(新) | 错题本设计文档(审计 L2909 要求) |
| `docs/design/chat-persistence.md`(新) | AI 对话持久化设计文档 |
| `docs/plans/full-audit-v0.5.0-deep.md` | 标记 NF-T4/PP4/PP5/PP6/P1-PG 为已完成 |

### 6.3 Release 准备

- versionCode 5 → 6
- versionName 0.5.0 → 0.6.0
- 本地构建 release APK + GitHub API 上传(沿用 v0.5.0 流程)

## 7. 实施顺序与 commit 规划

| Wave | commit | 内容 | 测试增量 |
|------|--------|------|---------|
| Wave 1 | `feat: Migration_4_5 + WenyanDatabase v5 schema 升级` | 数据库 schema 统一迁移 | +0(仅 schema) |
| Wave 2.1 | `refactor: NF-T4 Float 类型统一 + MemoRecordMapperTest` | Entity Double→Float | +5 测试 |
| Wave 2.2 | `refactor: NF-PP4 废弃 history JSON 双写 + SchedulingRepositoryTest` | 删 history 字段 | +3 测试 |
| Wave 2.3 | `feat: NF-PP6 ChatRepository + ChatMessageMapper + 测试` | AI 持久化数据层 | +5 测试 |
| Wave 2.4 | `feat: NF-PP5 WrongAnswerRepository + 测试` | 错题本数据层 | +5 测试 |
| Wave 3.1 | `feat: NF-PP6 AiAssistantViewModel 持久化 + Screen Drawer` | AI 持久化业务层 | +3 测试(适配现有 18) |
| Wave 3.2 | `feat: NF-PP5 QuizScreen 答题交互 + WrongAnswerScreen` | 错题本业务层 | +8 测试 |
| Wave 4 | `feat: P1-PG ProGuard 规则补齐(不启用 minify)` | 13 个 .pro 文件 | +0(规则准备) |
| Wave 5 | `docs: P1 大型任务完成 + Release v0.6.0 准备` | 文档 + 版本号 | - |

**总测试增量**:目标 +29 测试(220 → 249 tests)

## 8. 风险与缓解

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| Migration_4_5 复杂度高(SQLite 重建表) | 🟡 中 | 严格按 5 步流程(CREATE new → INSERT SELECT → DROP old → RENAME → 重建索引);写 MigrationTest |
| NF-PP6 flatMapLatest + stateIn 测试复杂 | 🟡 中 | 用 `testScope.runTest` + `UnconfinedTestDispatcher`;FakeChatRepository 内存模拟 |
| NF-PP5 Quiz 答题判定逻辑设计 | 🟢 低 | 阶段 1:用户提交后显示参考答案,用户自评对错(简化判定);阶段 2:接 AI 批改 |
| R8 规则遗漏(后续启用时崩) | 🟢 低 | Wave 4 仅写规则不启用;后续 emulator 实测三个关键路径(首启种子/AI/知识点列表) |
| 数据丢失(Migration 失败) | 🔴 高 | `fallbackToDestructiveMigrationOnDowngrade()` 已配置(只降级清空,升级抛异常);Migration 严格测试 |

## 9. 通过标准(Definition of Done)

- [ ] `assembleDebug` BUILD SUCCESSFUL
- [ ] `testDebugUnitTest` 240+ tests 0 failures 0 errors
- [ ] Migration_4_5 测试通过(数据完整性 + schema 一致性)
- [ ] NF-T4:MemoRecordMapper 双向转换无精度损失
- [ ] NF-PP4:review_logs 表是唯一复习历史源,无双写
- [ ] NF-PP6:进程被杀重启后 AI 对话历史恢复;多会话切换正常
- [ ] NF-PP5:Cards AGAIN + Quiz 答错 都能记录到错题本;错题本可重做
- [ ] P1-PG:13 个 .pro 文件规则齐全(`isMinifyEnabled=false` 保持)
- [ ] 文档更新:00-STATUS + SESSION_LOG + 2 个设计文档
- [ ] Release v0.6.0 发布(本地构建 + API 上传)

## 10. 调研附录(关键文件路径)

### NF-T4
- `core/data/src/main/java/com/wenyan/app/core/data/mapper/MemoRecordMapper.kt`
- `core/database/src/main/java/com/wenyan/app/core/database/entity/MemoRecordEntity.kt`(L50-54)
- `core/database/src/main/java/com/wenyan/app/core/database/entity/ReviewLogEntity.kt`(L60-64)
- `core/data/src/main/java/com/wenyan/app/core/data/repository/SchedulingRepository.kt`(L142-143, L186-187)

### NF-PP4
- `core/data/src/main/java/com/wenyan/app/core/data/repository/SchedulingRepository.kt`(L132-148 双写点)
- `core/data/src/main/java/com/wenyan/app/core/data/mapper/MemoRecordMapper.kt`(L106-153 appendReviewLog)
- `core/database/src/main/java/com/wenyan/app/core/database/entity/MemoRecordEntity.kt`(L80-82 history 字段)
- `core/ai/src/main/java/com/wenyan/app/core/ai/recall/AntiRoteMemorization.kt`(L107, L129 唯一消费方)

### NF-PP6
- `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt`(L47-53 注入,L55 StateFlow)
- `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantScreen.kt`(L220-261 渲染)
- `core/database/src/main/java/com/wenyan/app/core/database/entity/ChatHistoryEntity.kt`(死代码)
- `core/database/src/main/java/com/wenyan/app/core/database/entity/AiConversationEntity.kt`(死代码)
- `core/ai/src/main/java/com/wenyan/app/core/ai/AiServiceImpl.kt`(L67-76 SYSTEM_PROMPT,L190-198 Retrofit 构造)
- 审计文档 `docs/plans/full-audit-v0.5.0-deep.md`(L445-696 完整设计方案)

### NF-PP5
- `feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizScreen.kt`(L370-477 当前无答题判定)
- `feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizViewModel.kt`(L151-158 UiState)
- `feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsViewModel.kt`(L141-174 rateCard)
- `core/data/src/main/java/com/wenyan/app/core/data/repository/SchedulingRepository.kt`(L65-151)
- `core/ai/src/main/java/com/wenyan/app/core/ai/SocraticTutor.kt`(L156-190 explainWrongAnswer)
- `app/src/main/java/com/wenyan/app/navigation/TopLevelDestination.kt`(5 Tab 定义)
- `app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`(L191-207 子路由模板)

### P1-PG
- `app/build.gradle.kts`(L59 isMinifyEnabled=false)
- `app/proguard-rules.pro`(1 行注释)
- `core/ai/src/main/java/com/wenyan/app/core/ai/network/LlmDtos.kt`(6 个 @Serializable)
- `core/data/src/main/java/com/wenyan/app/core/data/seed/SeedDataLoader.kt`(L404-491 6 个 @Serializable)
- `core/database/src/main/java/com/wenyan/app/core/database/converter/WenyanTypeConverters.kt`(TypeConverter)
- `core/ai/src/main/java/com/wenyan/app/core/ai/network/LlmApiService.kt`(Retrofit 接口)

---

**规划完成,待用户确认后开始执行 Wave 1。**
