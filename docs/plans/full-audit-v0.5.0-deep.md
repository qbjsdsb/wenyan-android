# 文研 App 第四轮深度审计计划 v0.5.0

> **基准**：main @ `7ae628f`(v0.4.2 修复完成后)
>
> **本计划性质**:深度审计**计划文档**(audit plan),不是审计报告。先制定检查清单 → 逐项执行 → 输出报告 → 修复 → 验证。
>
> **审计目标**:在 v0.4.2 修复基础上,完成"零未修 P0 + 全代码无死角复审 + 业务逻辑正确性验证 + 实测运行时行为",达到可发 v0.3.0 Release 的状态。
>
> **审计方法**:每个维度配 ① 检查项清单(可勾选) ② 必查文件列表 ③ 验证方法(grep/test/emulator) ④ 输出格式 ⑤ 通过标准。
>
> **总规模**:160 个 .kt 文件 / 19,721 行代码 / 13 个模块 / 27 测试文件 / 1 instrumented test / 13 .pro 文件 / 3 workflow / 154 行 libs.versions.toml。

---

## 0. 总览:本轮审计要解决的 4 个核心问题

| # | 问题 | 当前状态 | 本轮目标 |
|---|------|---------|---------|
| **Q1** | v0.4.2 修复 24 文件是否引入回归? | 未验证 | 全量回归测试 + 24 文件逐行复审 |
| **Q2** | 4 个未修 P0 何时修? | P0-E1/E2/E3/E4 仍开 | 出详细修复方案 + 至少修 2 个 |
| **Q3** | 前三轮未覆盖的维度有哪些? | 22 维度已审,8 维度未审 | 补齐 8 个新维度 |
| **Q4** | 运行时实际行为是否符合预期? | 0 emulator 实测 | 至少跑 1 次完整 emulator 实测 |

---

## 1. 执行顺序与里程碑

```
Phase 0  回归性复审(确认 v0.4.2 修复无回归)         [必做,P0]
  ├─ 0.1 24 文件逐行 diff 复审
  ├─ 0.2 全量测试重跑(207 tests 必须 0 失败)
  └─ 0.3 assembleDebug + lint 全跑
Phase 1  未修 P0 修复方案设计 + 实施                  [必做,P0]
  ├─ 1.A P0-E1 网络异常差异化(中等)
  ├─ 1.B P0-E4 FSRS 时钟回拨防护(中等)
  ├─ 1.C P0-E2 AI 对话持久化(大,需新表 + Mapper + Repository)
  └─ 1.D P0-E3 进程被杀状态恢复(大,需全屏 rememberSaveable)
Phase 2  新维度深度审计(8 维度)                       [必做,P0]
  ├─ 2.A 业务逻辑正确性
  ├─ 2.B Room SQL + 数据模型完整性
  ├─ 2.C 协程/Flow 深度
  ├─ 2.D Compose 重组性能实测(Compiler Metrics)
  ├─ 2.E 资源与本地化
  ├─ 2.F 构建系统与 CI/CD
  ├─ 2.G 安全深度
  └─ 2.H 测试质量提升
Phase 3  依赖升级路径与短期升级                       [必做,P1]
Phase 4  emulator 实测与运行时验证                    [必做,P0]
Phase 5  出审计报告 v0.5.0 + 修复 + 验证              [必做,P0]
```

**估时分配**(仅用于排期,不作为承诺):
- Phase 0:1 个工作单元
- Phase 1:4 个工作单元(每 P0 一个)
- Phase 2:8 个工作单元(每维度一个,可并行)
- Phase 3:1 个工作单元
- Phase 4:2 个工作单元
- Phase 5:2 个工作单元
- 合计:18 个工作单元

---

## Phase 0 — 回归性复审(必做,P0)

> **目的**:确认 v0.4.2 commit `7ae628f` 24 文件改动没有引入新 bug,且修复确实解决问题。

### 0.1 24 文件逐行 diff 复审

**方法**:`git show 7ae628f -- <file>` 逐文件 review,核对每个 hunk 是否完整修复了对应 P0/P1。

#### 必查文件清单(28 个,按 Batch 分组)

**Batch 1 — FSRS 算法正确性(2 文件)**
- [ ] `core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt`
  - F-01:`nextDifficulty` 是否真的用 `w[6]/w[7]` 而非 `w[5]/w[6]`
  - F-02:`easyBonus` 是否为 `1f + w[16]` 而非 `w[16]`
  - F-03:EASY 分支 `interval` 是否基于 `recallS * easyBonus` 而非裸 `recallS`
  - F-05:`nextInterval` 是否用 `roundToInt()` 而非 `toInt()`
- [ ] `core/fsrs/src/test/java/com/wenyan/app/core/fsrs/FsrsWrapperTest.kt`
  - 4 个回归测试是否真的覆盖 F-01/F-02/F-03/F-05
  - 断言强度是否够(不能只是 assertNotNull)
  - 是否覆盖 4 档评分 × 3 状态 = 12 组合

**Batch 2 — 数据安全 P0(7 文件)**
- [ ] `app/src/main/AndroidManifest.xml` — `allowBackup="false"` 是否生效
- [ ] `app/build.gradle.kts` — `versionCode=3` + `versionName="0.3.0"` 是否正确
- [ ] `core/database/src/main/java/com/wenyan/app/core/database/di/DatabaseModule.kt` — `fallbackToDestructiveMigrationOnDowngrade` 是否替换正确,Upgrade 路径未破坏
- [ ] `core/ai/src/main/java/com/wenyan/app/core/ai/di/AiModule.kt` — `DEFAULT_API_KEY` 是否改为 `UUID.randomUUID().toString()`,OkHttp 日志是否区分 Debug/Release
- [ ] `core/data/src/main/java/com/wenyan/app/core/data/seed/SeedDataLoader.kt` — `withTransaction { }` 是否包裹了全部 7 步导入
- [ ] `core/data/build.gradle.kts` — `room-ktx` 依赖是否添加
- [ ] `core/ai/build.gradle.kts` — 改动核对

**Batch 3 — 测试有效性 P0(3 文件)**
- [ ] `core/ai/src/test/java/com/wenyan/app/core/ai/recall/AntiRoteMemorizationTest.kt` — `assert` 是否真的换成了 `assertEquals`
- [ ] `core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/WenyanNavigationBarTest.kt` — 是否测公开 `onNavigate` 回调而非内部状态
- [ ] `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt` — 冗余 `sendUserMessage` 重载是否清理,`messageCounter` 是否改 UUID

**Batch 4 — UX/契约 P1(10+ 文件)**
- [ ] `core/data/src/main/java/com/wenyan/app/core/data/repository/ThemeRepositoryImpl.kt` — 枚举 `valueOf` 是否用 `runCatching` 包裹
- [ ] `feature/settings/build.gradle.kts` — 是否生成 BuildConfig
- [ ] `feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt` — 版本号是否来自 `BuildConfig.VERSION_NAME`
- [ ] `core/database/src/main/java/com/wenyan/app/core/database/dao/GraphNodeDao.kt` — 18 处 `ORDER BY` 是否到位 + `getByIds` 批量查询
- [ ] `core/database/src/main/java/com/wenyan/app/core/database/dao/GraphEdgeDao.kt`
- [ ] `core/database/src/main/java/com/wenyan/app/core/database/dao/DataSourceDao.kt`
- [ ] `core/database/src/main/java/com/wenyan/app/core/database/dao/KnowledgePointDao.kt`
- [ ] `core/database/src/main/java/com/wenyan/app/core/database/dao/MemoRecordDao.kt`
- [ ] `core/data/src/main/java/com/wenyan/app/core/data/repository/GraphRepositoryImpl.kt` — 3 处 N+1 修复是否真的用 `getByIds + associateBy`
- [ ] `feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsViewModel.kt` — `rateCard` 是否有 try/catch + `isFinished` 完成态 + `errorMessage` StateFlow
- [ ] `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/ApiConfigViewModel.kt` — `editingId` 是否用局部量捕获

**文档(4 文件)**
- [ ] `AGENTS.md` / `docs/00-STATUS.md` / `docs/03-FAILED-ATTEMPTS.md` / `docs/SESSION_LOG.md` — 是否同步更新

**输出**:`phase0-regression-review.md`,每个文件 ① 修复点是否到位 ② 是否引入新问题 ③ 通过/不通过判定。

### 0.2 全量测试重跑

```bash
# 配置好环境后
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false \
  -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar \
  org.gradle.launcher.GradleMain :app:testDebugUnitTest --no-daemon 2>&1 | tail -30
```

**通过标准**:207 tests 0 failures(数量不减),无新警告。

### 0.3 assembleDebug + lint 全跑

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false \
  -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar \
  org.gradle.launcher.GradleMain :app:assembleDebug :app:lintDebug --no-daemon 2>&1 | tail -50
```

**通过标准**:`assembleDebug` SUCCESSFUL,`lintDebug` 无新增 Error 级问题。

---

## Phase 1 — 未修 P0 修复方案设计 + 实施(必做,P0)

> **目的**:为 4 个未修 P0 出详细修复方案,优先修中等规模的 2 个,大改造的 2 个出完整方案待用户决定。

### 1.A P0-E1 网络异常差异化(中等,优先修)

**问题**:`AiServiceImpl.kt` 把 401/超时/断网/JSON 错误统一显示"请求失败"。

**当前代码位置**:`core/ai/src/main/java/com/wenyan/app/core/ai/AiServiceImpl.kt:79-84`

**修复方案**:

1. 定义 `sealed class AiError`:
```kotlin
sealed class AiError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    object Unauthorized : AiError("API Key 无效或已过期")
    object NetworkTimeout : AiError("请求超时,请检查网络")
    object NoConnectivity : AiError("无网络连接")
    class JsonParse(cause: Throwable) : AiError("响应解析失败", cause)
    class ServerError(code: Int, body: String) : AiError("服务端错误 $code")
    class Unknown(cause: Throwable) : AiError("未知错误", cause)
}
```

2. 在 `AiServiceImpl.request` 内 catch 链分层捕获:
   - `IOException` → 检查 `ConnectivityManager` 区分 timeout vs no connectivity
   - `HttpException(code=401)` → `Unauthorized`
   - `HttpException(code>=500)` → `ServerError`
   - `JsonDecodeException` → `JsonParse`
   - 其他 → `Unknown`

3. ViewModel 把 `AiError` 类型透传到 UI,UI 显示对应文案 + 图标。

**验证**:新增 `AiServiceImplErrorMappingTest`,用 MockWebServer 模拟 4 种错误响应,断言映射正确。

**通过标准**:`AiServiceImpl` 不再有"请求失败"统一文案,测试覆盖 5 种错误分支。

### 1.B P0-E4 FSRS 时钟回拨防护(中等,优先修)

**问题**:`SchedulingRepository.kt` 用 `System.currentTimeMillis()` 计算到期,用户改系统时间会导致卡片"永久消失"或"无限到期"。

**当前代码位置**:`core/data/src/main/java/com/wenyan/app/core/data/repository/SchedulingRepository.kt:67`

**修复方案**:

1. 在 `WenyanDatabase` 加 `app_meta` 表(单行),记录 `lastKnownTimestamp` + `lastKnownBootCount`:
```kotlin
@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val key: String,
    val longValue: Long?,
    val stringValue: String?
)
```

2. `SchedulingRepository` 注入 `AppMetaDao`,每次计算到期前:
   - 读 `lastKnownTimestamp` + 当前 `System.currentTimeMillis()`
   - 若 `current < lastKnown - 60_000`(回拨超 1 分钟),用 `lastKnown` 作为基准 + 记录告警
   - 否则正常 + 更新 `lastKnown = current`

3. 启动时 + 每次评分后更新 `lastKnownTimestamp`。

**验证**:`SchedulingRepositoryClockTest`:
- 模拟时钟前移(正常)+ 回拨 5 分钟 + 回拨 1 小时,断言 `dueCards` 行为正确。

**通过标准**:时钟回拨不再导致卡片消失/无限到期,有日志可查。

### 1.C P0-E2 AI 对话持久化(大,出方案)

**问题**:`AiAssistantViewModel` 把消息放在内存 `StateFlow<List<ChatMessage>>`,进程被杀即丢。

**修复方案**(完整设计,留待 Phase 1.C 执行):

1. **数据模型**(已有 `chat_history` + `ai_conversations` 表但未接通):
   - 重新审视 `ChatConversation` vs `AiConversation` 语义重复(P1-D5 第三轮发现)
   - 合并为单一 `ChatConversationEntity`(id/title/createdAt/lastMessageAt/model)
   - `ChatMessageEntity`(id/conversationId/role/content/timestamp/tokens)

2. **Repository**:
   ```kotlin
   interface ChatRepository {
       fun observeConversations(): Flow<List<ChatConversation>>
       fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
       suspend fun createConversation(): String  // 返回 id
       suspend fun appendMessage(conversationId: String, msg: ChatMessage)
       suspend fun deleteConversation(id: String)
   }
   ```

3. **ViewModel 改造**:
   - `currentConversationId: StateFlow<String?>` 持久化到 DataStore
   - App 启动时若 `currentConversationId != null` → 从 DB 加载历史
   - 发送/接收消息 → `appendMessage` 落库

4. **UI**:加"新建对话"/"历史对话列表"入口(可放 drawer 或顶部菜单)。

**验证**:
- 单元测试:`ChatRepositoryImplTest` 覆盖 CRUD + Flow
- 集成测试:进程被杀重启,历史消息完整恢复

**估时**:大,需 1-2 个工作单元。建议先出设计文档单独评估。

### 1.D P0-E3 进程被杀状态恢复(大,出方案)

**问题**:全项目 0 处 `rememberSaveable` / 0 处 `onSaveInstanceState`,旋转 + 进程被杀全丢 UI 状态。

**修复方案**(完整设计,留待 Phase 1.D 执行):

1. **审计所有 Composable 的可变状态**:
   ```bash
   grep -rn "remember\s*{" feature/ core/designsystem/ | wc -l
   grep -rn "rememberSaveable" feature/ core/designsystem/
   ```

2. **改造清单**(预估 30+ 处):
   - 所有 `var x by remember { mutableStateOf(...) }` 改 `rememberSaveable`
   - 复杂状态用 `Saver` 自定义保存
   - `LazyListState` / `ScrollState` 用 `rememberSaveable`

3. **ViewModel 已 `SavedStateHandle` 注入**(检查 Hilt ViewModelFactory 配置):
   - 所有 ViewModel 构造函数加 `@Inject constructor(... savedStateHandle: SavedStateHandle)`
   - 关键状态(key UI state)写 `savedStateHandle["key"] = value` + 读 `savedStateHandle.getStateFlow("key", default)`

4. **Manifest**:`android:configChanges` 评估是否要指定(避免重建),`android:saveEnabled="true"`(默认)。

**验证**:
- 开发者选项"不保留活动"开启,旋转 + 切后台 + 杀进程,UI 状态完整恢复
- 9 个 Screen 各跑一遍

**估时**:最大,需 2-3 个工作单元。建议先做高优先级 Screen(Cards/Quiz/AiAssistant/Knowledge)。

---

## Phase 2 — 新维度深度审计(8 维度,必做,P0)

> **目的**:补齐前三轮未深度覆盖的维度,确保"所有代码都不能放过"。

### 2.A 业务逻辑正确性深度审计

> **核心问题**:代码"能跑"和"逻辑对"是两回事。前三轮关注 bug,本轮关注**业务正确性**。

#### 2.A.1 FSRS-6 完整公式对比(深度复审)

**必查文件**:`core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt`

**检查项**(对照 [open-spaced-repetition/fsrs4anki](https://github.com/open-spaced-repetition/fsrs4anki) 的 fsrs-6.x.py):

- [ ] `initDifficulty` 4 档映射:AGAIN=w[0]+1, HARD=w[1]+1, GOOD=w[2]+1, EASY=w[3]+1(注意 +1)
- [ ] `initStability` 4 档映射:AGAIN=w[4], HARD=w[5], GOOD=w[6], EASY=w[7]
- [ ] `nextDifficulty`:`D' = w[6]*(D - w[7]*(r-3)) + (1-w[6])*w[7-1]`(注意 FSRS-6 公式变种)
- [ ] `nextRecallStability` GOOD 分支:`S' * (1 + w[8]*exp(-w[9]*S) * (11-D) * R - w[10] * (1-R) * w[17])`(FSRS-6 含 w[17])
- [ ] `nextRecallStability` AGAIN 分支:`w[11] * D^(-w[12]) * ((S+1)^w[13] - 1) * exp(w[14]*(1-R))`(FSRS-6)
- [ ] `nextForgetStability`:`w[15] * D^(-w[16]) * ((S+1)^w[17] - 1) * exp(w[18]*(1-R))`(FSRS-6)
- [ ] `applyFuzz`:`interval ± max(1, 0.05*interval)`(不是 ±1/±5%)
- [ ] `retrievability`:`(1 + 19*t/(81*S))^(-0.5)`(FSRS-6 decay=-0.5,不是 -1)
- [ ] `nextInterval`:`min(max(round(interval * fuzz), 1), maximumInterval)`
- [ ] w[17]-w[20] 是否真的未使用(F-04),FSRS-6 短期记忆权重应影响 LEARNING/RELEARNING

**输出**:`fsrs-formula-diff.md`,每个公式 ① 代码 ② 官方 ③ 一致/不一致 ④ 数值影响。

#### 2.A.2 知识图谱业务逻辑

**必查文件**:
- `core/data/src/main/java/com/wenyan/app/core/data/graph/WeakSubgraphDetector.kt`
- `core/data/src/main/java/com/wenyan/app/core/data/graph/PrerequisiteChecker.kt`
- `core/data/src/main/java/com/wenyan/app/core/data/graph/InterferenceWarner.kt`

**检查项**:
- [ ] `WeakSubgraphDetector`:弱子图检测算法是否正确?对比图论标准定义
- [ ] `PrerequisiteChecker`:拓扑排序 + 环检测是否处理了 DAG 中的环?
- [ ] `InterferenceWarner`:相似度计算(余弦/Jaccard?)是否正确,阈值是否合理
- [ ] 3 个组件的测试是否真的覆盖业务正确性(不只是不抛异常)

#### 2.A.3 AI 服务业务逻辑

**必查文件**:
- `core/ai/src/main/java/com/wenyan/app/core/ai/RagEngine.kt`
- `core/ai/src/main/java/com/wenyan/app/core/ai/SocraticTutor.kt`
- `core/ai/src/main/java/com/wenyan/app/core/ai/recall/RecallChecker.kt`
- `core/ai/src/main/java/com/wenyan/app/core/ai/recall/AntiRoteMemorization.kt`

**检查项**:
- [ ] `RagEngine`:检索 → rerank → 拼接 prompt 流程是否正确,topK 参数是否生效
- [ ] `SocraticTutor`:苏格拉底提问生成逻辑,是否真的避免直接给答案
- [ ] `RecallChecker`:回忆检测算法,基于 FSRS retrievability 还是独立模型
- [ ] `AntiRoteMemorization`:反死记硬背策略,题目变形/换角度是否有效

#### 2.A.4 真题/知识点关联业务逻辑

**必查文件**:
- `core/data/src/main/java/com/wenyan/app/core/data/cards/CardSplitter.kt`
- `core/fsrs/src/main/java/com/wenyan/app/core/fsrs/ContentTierMapper.kt`
- `core/fsrs/src/main/java/com/wenyan/app/core/fsrs/ExamCountdownManager.kt`

**检查项**:
- [ ] `CardSplitter`:把知识点拆成卡片的策略是否合理(每知识点 1 张?N 张?)
- [ ] `ContentTierMapper`:三层记忆(粗/中/细)映射规则是否符合教学法
- [ ] `ExamCountdownManager`:考研倒计时 + 阶段切换(基础/强化/冲刺)逻辑

---

### 2.B Room SQL + 数据模型完整性审计

> **核心问题**:DAO 写对了吗?Entity 关系正确吗?Migration 完整吗?

#### 2.B.1 Entity 字段与关系图

**必查文件**:`core/database/src/main/java/com/wenyan/app/core/database/entity/*.kt`(全部)

**检查项**:
- [ ] 列出所有 Entity(预估 8-10 个),画 ER 图(在 markdown 里用 mermaid)
- [ ] ForeignKey 关系是否完整(预估缺 StudyProgressEntity.lastPointId 等)
- [ ] Index 是否覆盖所有查询字段(@Index)
- [ ] PrimaryKey 是否合理(UUID vs 自增)
- [ ] 嵌套对象用 `@Embedded` 是否正确
- [ ] TypeConverters 是否覆盖所有非基本类型字段

#### 2.B.2 DAO SQL 正确性

**必查文件**:`core/database/src/main/java/com/wenyan/app/core/database/dao/*.kt`(全部)

**检查项**:
- [ ] 每个 `@Query` 的 SQL 是否符合 SQLite 方言(JOIN 语法/COALESCE/IN/LIKE 转义)
- [ ] `ORDER BY` 子句(第三轮已修 18 处,本轮复审)
- [ ] `LIMIT` 是否防止过大结果集
- [ ] `@Transaction` 标注的多步查询是否真有事务保护
- [ ] `Flow` 返回的查询是否在表变更时正确触发(所有写入表都对应查询表)
- [ ] 参数绑定是否防 SQL 注入(用 `:param` 而非字符串拼接)
- [ ] `@Insert(onConflict = REPLACE)` 的影响是否考虑(会替换整行)

#### 2.B.3 Migration 完整性

**必查文件**:`core/database/src/main/java/com/wenyan/app/core/database/WenyanDatabase.kt` + di/DatabaseModule.kt

**检查项**:
- [ ] `version` 与 schema 一致
- [ ] 是否有 `Migration` 实例(预估当前是 1,改 schema 必须 bump version + 加 Migration)
- [ ] `fallbackToDestructiveMigrationOnDowngrade` 是否真的只在 Downgrade 触发
- [ ] Export schema 选项(`exportSchema=true`)是否生成 schema JSON
- [ ] Room 验证:`room.schemaLocation` 配置

#### 2.B.4 实际生成 SQL 验证

```bash
# 跑 instrumented test
$JAVA_HOME/bin/java ... :app:connectedDebugAndroidTest
```

- [ ] `RoomDatabaseInstrumentedTest` 已存在,扩展它验证所有 DAO 实际 SQL
- [ ] 用 `EXPLAIN QUERY PLAN` 检查索引使用(在测试里)

---

### 2.C 协程/Flow 深度审计

> **核心问题**:Flow 链是否正确?协程是否泄漏?并发是否安全?

**必查文件**:所有 Repository Impl + ViewModel(13 + 8 个,约 21 个文件)

**检查项**:

- [ ] **Flow 操作符链**:
  - `.map { }` 内是否调用 suspend(应该用 `.flatMapLatest` 或 `.map { transform() }` 后 `.flattenConcat`)
  - `.stateIn` 是否都有 `.catch {}` 兜底
  - `.combine` 两个源是否都在变化时触发
  - `.flowOn(IO)` 是否放在正确位置(在 Flow 构建后,操作符前)
  - `.distinctUntilChanged()` 是否缺失导致重复触发

- [ ] **协程作用域**:
  - 全项目 grep `GlobalScope`(必须 0)
  - 全项目 grep `CoroutineScope(`(只允许 DI 注入的)
  - 全项目 grep `viewModelScope.launch { }` 是否有 try/catch
  - 全项目 grep `liveData { }`(应该 0,用 Flow)

- [ ] **并发安全**:
  - `MutableStateFlow` 的 `update {}` vs `value =` 原子性
  - `Mutex` 是否用对(tryLock vs lock)
  - 同一 ViewModel 的多个 `launch` 是否串行化(用 `Mutex` 或 `channel`)

- [ ] **取消语义**:
  - `withTimeout` 是否有 fallback
  - `CancellationException` 是否被错误 catch(必须 rethrow)
  - `Flow.first()` vs `Flow.single()` 的区别

- [ ] **StateFlow vs SharedFlow 使用**:
  - UI state 用 StateFlow(去重)
  - 事件用 SharedFlow(不丢)
  - `extraBufferCapacity` / `onBufferOverflow` 配置

**验证**:
```bash
grep -rn "GlobalScope" --include="*.kt" core/ feature/ app/ | grep -v test
grep -rn "viewModelScope.launch" --include="*.kt" feature/ | grep -v "try\s*{" | head -20
```

---

### 2.D Compose 重组性能实测(Compiler Metrics)

> **核心问题**:第二/三轮发现的 derivedStateOf 缺失/LazyColumn items 缺 key/不稳定参数是否真的导致重组?

**步骤**:

1. **启用 Compose Compiler Metrics**(临时,验证后关闭):
   ```kotlin
   // app/build.gradle.kts
   composeOptions {
       kotlinCompilerExtensionVersion = "x.x.x"
   }
   // 在 gradle.properties 加:
   org.gradle.jvmargs=-Xmx4g
   ```
   ```bash
   # 跑 build 时加参数
   -P plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.buildDir}/compose_reports
   -P plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${project.buildDir}/compose_metrics
   ```

2. **生成报告后审查**:
   - [ ] `*-classes.txt`:标记 `restartable skippable` 的 Composable 是否真的 skippable
   - [ ] `*-composables.txt`:每个 Composable 的重组次数
   - [ ] `*-sets.txt`:不稳定参数集合(List/Map/普通 data class)
   - [ ] 标记为 `unstable` 的参数 → 改 `@Stable` 或 `@Immutable`

3. **重点审查 Composable**(第二/三轮已发现):
   - [ ] `WenyanApp.selectedTopLevelRoute`(线性扫描)
   - [ ] `GraphScreen.weakNodes` / `avgRetrievability`
   - [ ] `KnowledgePointDetailScreen.subtitle`
   - [ ] `SettingsScreen.seedColors`
   - [ ] `KnowledgeScreen.items(KnowledgeCategory.entries.toList())` 无 key
   - [ ] `QuizScreen.items(years)` 无 key
   - [ ] `ApiConfigScreen.items(LlmProvider.entries.toList())` 无 key
   - [ ] `KnowledgePointDetailScreen` Column+verticalScroll → LazyColumn

4. **emulator 实测重组**:
   - Layout Inspector → Compose 重组计数
   - 滚动列表观察是否卡顿

**输出**:`compose-recomposition-report.md`,列出所有不稳定 Composable + 修复方案。

---

### 2.E 资源与本地化审计

**必查文件**:`app/src/main/res/**` 全部 + 9 个 Screen 的硬编码字符串

**检查项**:

- [ ] **strings.xml 完整性**:
  - 当前只有 `app_name`,9 个 Screen 全硬编码(第二轮 P0 未修)
  - 列出所有硬编码字符串(`grep -rn "Text(\"" feature/ core/designsystem/ | wc -l`)
  - 输出迁移清单(预估 100+ 字符串)
  - 优先级:用户可见 > 装饰性 > 内部

- [ ] **values-night 缺失**(第二轮 P1-M7):
  - 加 `values-night/colors.xml` + `values-night/themes.xml`
  - 验证 dark mode 启动无白屏闪烁

- [ ] **drawables**:
  - `ic_launcher_foreground.xml` + `ic_launcher_background.xml` 是否合理
  - 是否缺 adaptive icon 的 monochrome 层(Android 13+ themed icon)
  - 是否有 vector drawable 而非 PNG

- [ ] **themes.xml**:
  - 当前是 legacy `android:Theme.Material.Light.NoActionBar`(P1-M6)
  - 改 `<style name="Theme.Wenyan" parent="android:Theme.Material.Light.NoActionBar">` → M3 `Theme.Material3.DayNight.NoActionBar`
  - splash 配置(API 23+ 用 `Theme.SplashScreen`)

- [ ] **多语言支持**:
  - 当前只中文,但留 i18n 钩子(`values-en/strings.xml` 占位)
  - `localeConfig`(Android 13+ per-app language)

- [ ] **dimens.xml 缺失**:
  - 硬编码 dp 散落(8.dp / 12.dp / 20.dp / 48.dp)
  - 创建 `dimens.xml` + `Spacing` token 完整迁移

- [ ] **colors.xml**:
  - 当前只有 launcher 颜色
  - 应清理(主题色用 M3 dynamic)

- [ ] **font 配置**:
  - 是否用系统字体 / 自定义字体
  - `fontFamily` 配置

---

### 2.F 构建系统与 CI/CD 审计

**必查文件**:
- `build.gradle.kts`(root)
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts` + 12 个模块 `build.gradle.kts`
- `.github/workflows/android.yml` / `release.yml` / `generate-keystore.yml`

**检查项**:

- [ ] **gradle/libs.versions.toml**:
  - 所有依赖是否都在 [versions] / [libraries] / [bundles] / [plugins] 完整定义
  - 是否有硬编码版本号(`grep -rn "implementation \"" --include="*.kts"` 应该 0)
  - 版本冲突检查(`./gradlew dependencies`)

- [ ] **build.gradle.kts(root)**:
  - plugin alias 是否正确
  - repository 顺序(pluginManagement + dependencyResolutionManagement,AGENTS.md 已规定 Aliyun fallback)
  - `commonSettings` 或 convention plugin 是否值得引入(13 个模块有重复配置)

- [ ] **app/build.gradle.kts**:
  - `signingConfigs` 是否正确(release 用 secrets,debug 用默认)
  - `buildTypes`:`release` 是否 `isMinifyEnabled=true`(当前 false,P1-M3 .pro 全空,启用即崩)
  - `flavors` 是否需要(预估不需要)
  - `compileSdk` / `targetSdk` / `minSdk` 合理性
  - `testOptions` 配置(unit tests 用 Robolectric?)

- [ ] **CI workflow**:
  - `android.yml`:CI 跑 `testDebugUnitTest`(已修),`assembleDebug` + `lintDebug`
  - `release.yml`:Line 63-70 "Verify keystore" 隐藏 bug(P0-4 未修)— 详查
  - `generate-keystore.yml`:用途是啥(预估本地生成 keystore 用),是否在 CI 跑
  - 所有 workflow 的 `runs-on` / `timeout-minutes` / `cache` 配置
  - secrets 使用是否完整(KEYSTORE_BASE64 / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD)

- [ ] **ProGuard / R8**:
  - 13 个 .pro 文件全空(P1-NEW-14)
  - 即使 `isMinifyEnabled=false`,Release 前必须预置规则:
    - Retrofit:keep interface + Generic
    - kotlinx.serialization:keep @Serializable
    - Hilt:keep @HiltAndroidApp + @Inject
    - Room:keep @Entity + @Dao
    - Compose:自动 keep
  - 输出每个模块的 `consumer-rules.pro` 模板

- [ ] **Gradle Wrapper**:
  - `gradle-wrapper.properties` 版本(应该是 8.14.4,AGP 8.6.0 兼容)
  - `gradlew` 可执行权限
  - `gradle/wrapper/gradle-wrapper.jar` 存在

- [ ] **local.properties**:
  - 不入仓库(检查 .gitignore)
  - `sdk.dir` 配置

- [ ] **build cache**:
  - `org.gradle.caching=true`
  - `org.gradle.configuration-cache`(AGP 8.x 支持,可试)

---

### 2.G 安全深度审计

> **核心问题**:用户数据 / API key / 网络通信是否真的安全?

**检查项**:

- [ ] **AndroidManifest**:
  - `allowBackup="false"`(已修)
  - `android:fullBackupContent` / `android:dataExtractionRules`(Android 12+ 备份规则)
  - `android:networkSecurityConfig`(明文流量限制)
  - `android:exported` 对所有 component 标注
  - 权限声明是否最小化(`grep "uses-permission" AndroidManifest.xml`)

- [ ] **API Key 存储**:
  - `security-crypto` alpha 版本(P1-NEW-10)
  - 评估替代:Android Keystore 直接封装(无 alpha 依赖)
  - 加密强度(AES-GCM vs CBC)
  - key alias 命名(防止冲突)

- [ ] **网络通信**:
  - HTTPS 强制(`usesCleartextTraffic="false"`)
  - Certificate Pinning(预估没做,要评估是否需要)
  - OkHttp `Interceptor` 链:Auth / Logging / Retry
  - 日志拦截器在 Release 必须关闭(BODY 级,P1-H2 已识别,核对修复)
  - 超时配置(connect/read/write 各 30s?)

- [ ] **Room 数据库**:
  - 是否加密(预估没,用户 FSRS 数据敏感性评估)
  - SQLCipher 评估(性能 vs 安全 trade-off)

- [ ] **Compose UI**:
  - 密码字段 `KeyboardType.Password` + `visualTransformation`
  - API Key 输入框是否掩码(预估 ApiConfigScreen 已做)

- [ ] **第三方 SDK**:
  - 全依赖列表 `./gradlew dependencies` → 排查已知 tracker / ad SDK(预估 0)
  - Material3 / AndroidX 是否官方源

- [ ] **隐私政策**:
  - App 收集什么数据(API key / FSRS 记录 / 知识点)
  - 是否需要隐私政策界面(预估学生用工具,暂不需要)

---

### 2.H 测试质量提升计划

> **当前评级 B-(第三轮)**,目标提升到 A-。

**必查文件**:27 个测试文件 + 1 instrumented test

**检查项**:

- [ ] **覆盖率**:
  - 跑 `./gradlew jacocoTestReport`(需先加 jacoco 插件)
  - 行覆盖率目标:核心模块(fsrs/data/ai)≥ 70%,feature 模块 ≥ 50%
  - 分支覆盖率 ≥ 50%

- [ ] **5/8 ViewModel 零测试**(第二轮 P1-NEW-9):
  - [ ] `CardsViewModelTest`(新)
  - [ ] `ApiConfigViewModelTest`(新)
  - [ ] `KnowledgePointDetailViewModelTest`(新)
  - [ ] `GraphViewModelTest`(新,有 FakeGraphRepository 可复用)
  - [ ] `QuizViewModelTest`(新)

- [ ] **FSRS 测试补强**(P0-T1e/f):
  - 4 档评分 × 3 状态 = 12 组合全覆盖(第三轮已部分补,核对完整)
  - 边界值:S=0 / D=1 / D=10 / rating=4 / R=1.0 / R=0.0
  - 数值精度:对比官方 fsrs-rs 计算结果(取 5 组典型用例手算)

- [ ] **Repository 测试**:
  - 6/8 Repository 未接口化(第二轮 P2)— 接口化 + Fake + 单测
  - 8 个 RepositoryImpl 各补关键路径测试

- [ ] **Fake 实现质量**:
  - `FakeKnowledgePointDao` / `FakeReviewLogDao` / `FakeApiConfigDao` 是否实现真实(第三轮发现写方法空实现)
  - 用 `MockK` 或 `Mockito-Kotlin` 替代部分手写 Fake

- [ ] **测试反模式**:
  - 全项目 grep `runBlocking` → 改 `runTest`(第三轮 P0-T1g 修了 3 个,核对完整)
  - 全项目 grep `Thread.sleep` → 改 `advanceTimeBy`
  - 全项目 grep `assert(`(Kotlin assert,不是 JUnit)→ 改 `assertEquals`(第三轮 P0-T1c 修了 1 个)
  - 测试是否依赖外部状态(网络/时间/文件系统)

- [ ] **instrumented test**:
  - 当前只有 `RoomDatabaseInstrumentedTest` 1 个
  - 补 Hilt DI 图测试(`@HiltAndroidTest`)
  - 补 Compose UI 测试(`createAndroidComposeRule`)
  - 至少 9 个 Screen 各 1 个 smoke test

- [ ] **测试命名与组织**:
  - `MethodName_State_ExpectedBehavior` 风格
  - 每个 test class 有 `@Before` setup
  - 测试间无状态依赖(每个测试独立)

---

## Phase 3 — 依赖升级路径规划(必做,P1)

> **核心问题**:AGP 8.6.0 钉死链(第二轮 3.1 节)如何解开?

### 3.1 短期(不碰 AGP,可立即升)

| 依赖 | 当前 | 目标 | 风险 |
|------|------|------|------|
| coroutines | 1.8.1 | 1.9.x | 低(API 兼容) |
| kotlinx-serialization | 1.6.3 | 1.7.x | 低 |
| activity-compose | 当前 | +1 minor | 低 |
| core-ktx | 当前 | +1 minor | 低 |
| lifecycle-* | 当前 | +1 minor | 低 |
| materialKolor | 4.1.1 | 5.0.0 | 中(可能 API 变更) |

### 3.2 中期(需升 AGP 到 8.8+)

| 依赖 | 当前 | 目标 | 阻塞 |
|------|------|------|------|
| AGP | 8.6.0 | 8.8.x | 需测 KSP 2.3.x 兼容 |
| KSP | 2.3.2 | 2.3.10 | AGP 8.8+ 才支持 |
| Hilt | 2.57.1 | 2.59+ | AGP 8.8+ |
| compileSdk | 35 | 35(保持) | - |

### 3.3 长期(大爆炸)

| 依赖 | 当前 | 目标 | 阻塞 |
|------|------|------|------|
| AGP | 8.6.0 | 9.x | compileSdk 36+ |
| material3 | 1.5.0-alpha18 | stable | AGP 9.1 + compileSdk 37 |
| Kotlin | 2.3.10 | 2.4.x | 待发布 |
| retrofit | 2.9.0 | 2.11+ | 独立可升(内含 serialization converter) |
| navigation-compose | 2.7.7 | 2.8.x(类型安全) | 独立可升 |

### 3.4 安全替换

- [ ] `security-crypto 1.1.0-alpha06` → Android Keystore 直接封装
  - 设计 `ApiKeyCrypto` 接口不变,Impl 改用 `KeyStore.getInstance("AndroidKeyStore")`
  - 测试:加密 → 解密往返 / key alias 冲突 / Android Keystore 不可用 fallback
- [ ] 评估 `androidx.crypto` 是否有 stable 替代

### 3.5 CVE 联网复核

用 [OSV.dev](https://osv.dev/) 或 [GitHub Advisories](https://github.com/advisories) 复核:
- [ ] okhttp 4.12.0
- [ ] retrofit 2.9.0
- [ ] coroutines 1.8.1
- [ ] kotlinx-serialization 1.6.3
- [ ] security-crypto 1.1.0-alpha06(第三轮已查,复核)
- [ ] Hilt 2.57.1
- [ ] Room 2.7.0
- [ ] Compose BOM 2025.12.00

---

## Phase 4 — emulator 实测与运行时验证(必做,P0)

> **核心问题**:代码"能编译"和"能跑对"是两回事。

### 4.1 环境准备

```bash
# AVD 创建(API 35,x86_64)
avdmanager create avd -n wenyan_test -k "system-images;android-35;google_apis;x86_64" -d pixel_7
emulator -avd wenyan_test -no-window -no-audio -no-boot-anim &
adb wait-for-device
```

### 4.2 测试矩阵

| # | 场景 | 验证项 | 通过标准 |
|---|------|-------|---------|
| 1 | App 启动 | 冷启动 < 2s,无崩溃 | 主页可交互 |
| 2 | 种子数据导入 | SeedDataLoader 跑完无异常 | 知识点列表非空 |
| 3 | FSRS 调度 | 评分 AGAIN/GOOD/EASY 后下次到期日 | EASY 间隔 > GOOD > AGAIN |
| 4 | 卡片翻转 | 翻转动画无镜像(v0.3 修复) | 正反面正确显示 |
| 5 | AI 入口 | TopBar AI 图标点击跳转 | 进入 AiAssistantScreen |
| 6 | Tab 切换 | 5 个 Tab 切换动画 | fade transition 流畅 |
| 7 | 暗色模式 | 切换无白屏闪烁 | values-night 生效 |
| 8 | 旋转屏 | 旋转后 UI 状态(当前会丢,P0-E3) | 记录现象 |
| 9 | 进程被杀 | "不保留活动"后重启 | 记录现象 |
| 10 | 网络断开 | AI 请求报错文案 | 显示差异化错误(修后) |
| 11 | 时钟回拨 | 改系统时间后到期卡片 | 不消失(修后) |
| 12 | AI 对话 | 发消息 → 收回复 → 切后台 → 杀进程 → 重启 | 历史恢复(修后) |
| 13 | 大量数据 | 用脚本插 1000 知识点 | 列表滚动流畅 60fps |
| 14 | 内存 | 跑 30 分钟 + 看 Profiler | 无内存泄漏 |
| 15 | Compose 重组 | Layout Inspector | 列表 item 重组 < 5 次/秒 |

### 4.3 性能 profiling

- [ ] CPU Profiler:启动 + 滚动 + 卡片翻转
- [ ] Memory Profiler:跑 30 分钟,看 GC + LeakCanary
- [ ] Network Profiler:AI 请求
- [ ] Battery Historian:30 分钟使用
- [ ] StrictMode:开启 detectAll → 检查主线程 IO/网络

### 4.4 可访问性实测

- [ ] TalkBack:9 个 Screen 各跑一遍
- [ ] Switch Access:验证焦点顺序
- [ ] 字体大小:最大字体下布局不破
- [ ] 对比度:WCAG AA 通过

---

## Phase 5 — 出审计报告 + 修复 + 验证(必做,P0)

### 5.1 报告输出

执行完 Phase 0-4 后,输出 `docs/plans/full-audit-v0.5.0-report.md`(报告,非计划):
- 第 0 节:回归性复审结果(0.1-0.3 通过/失败)
- 第 1 节:4 个未修 P0 修复状态(已修/方案待实施)
- 第 2 节:8 个新维度发现(P0/P1/P2 清单)
- 第 3 节:依赖升级路径
- 第 4 节:emulator 实测结果(15 项测试矩阵)
- 第 5 节:本轮新增 vs 前三轮累计统计
- 第 6 节:审计质量自评

### 5.2 修复 Batch 计划

根据报告的 P0/P1 数量,分 Batch 修复:
- **Batch 1**(P0 必修):FSRS 算法残余问题 + 4 个未修 P0 已实施部分 + 数据安全新发现
- **Batch 2**(P1 关键):ViewModel 契约 + 测试补全 + Compose 性能
- **Batch 3**(P1 次要):资源本地化 + 依赖升级短期
- **Batch 4**(P2 可选):代码风格 + 命名 + 文档

### 5.3 验证标准

修复完成后必须满足:
- [ ] `assembleDebug` SUCCESSFUL
- [ ] `testDebugUnitTest` 0 failures(数量应 ≥ 220,新增 13+ 测试)
- [ ] `lintDebug` 0 Errors
- [ ] emulator 15 项测试矩阵全部通过
- [ ] `00-STATUS.md` 同步更新
- [ ] commit message 清晰描述本轮修复
- [ ] CI 全绿后才可发 Release v0.3.0

### 5.4 发版流程(可选,需用户确认)

```bash
# 1. 本地全验证通过
# 2. CI 全绿(gh run list 核对)
# 3. 删除旧 orphan tag(如有)
git push origin :refs/tags/v0.3.0  # 如有旧 tag
# 4. 打新 tag
git tag v0.3.0
git push origin v0.3.0
# 5. 等 Release workflow 完成
# 6. 下载 APK 实测验证
```

---

## 附录 A:执行 Checklist(可勾选)

### Phase 0 回归复审
- [ ] 0.1 28 文件逐行 diff 复审
- [ ] 0.2 207 tests 重跑通过
- [ ] 0.3 assembleDebug + lint 通过

### Phase 1 未修 P0
- [ ] 1.A P0-E1 网络异常差异化(实施)
- [ ] 1.B P0-E4 FSRS 时钟回拨(实施)
- [ ] 1.C P0-E2 AI 对话持久化(方案)
- [ ] 1.D P0-E3 状态恢复(方案)

### Phase 2 新维度
- [ ] 2.A 业务逻辑正确性(FSRS + 图谱 + AI + 真题)
- [ ] 2.B Room SQL + 数据模型
- [ ] 2.C 协程/Flow
- [ ] 2.D Compose 重组性能
- [ ] 2.E 资源与本地化
- [ ] 2.F 构建系统与 CI/CD
- [ ] 2.G 安全深度
- [ ] 2.H 测试质量提升

### Phase 3 依赖升级
- [ ] 3.1 短期升级
- [ ] 3.4 security-crypto 替换
- [ ] 3.5 CVE 复核

### Phase 4 emulator 实测
- [ ] 4.2 15 项测试矩阵
- [ ] 4.3 性能 profiling
- [ ] 4.4 可访问性实测

### Phase 5 报告与修复
- [ ] 5.1 出 v0.5.0 报告
- [ ] 5.2 修复 Batch 1-4
- [ ] 5.3 验证标准全部通过
- [ ] 5.4 (可选)发 v0.3.0 Release

---

## 附录 B:并行执行建议

以下维度**无依赖**可并行(用 Task subagent):

| 并行组 | 维度 | 估时 |
|-------|------|------|
| Group 1 | 2.A 业务逻辑 + 2.B Room SQL + 2.C 协程 | 3 个 subagent 并行 |
| Group 2 | 2.D Compose 性能 + 2.E 资源 + 2.F 构建 | 3 个 subagent 并行 |
| Group 3 | 2.G 安全 + 2.H 测试质量 | 2 个 subagent 并行 |
| Group 4 | Phase 0 回归复审 + Phase 1 P0 修复方案 | 串行(修复方案依赖复审结果) |
| Group 5 | Phase 4 emulator 实测 | 独立(需 emulator 环境) |

**总并行度**:Phase 2 的 8 个维度可全部并行,理论 8 个 subagent 同时跑。考虑上下文成本,建议 3-4 个并行。

---

## 附录 C:输出文件清单

执行完本计划后,将产出以下文档:

1. `docs/plans/full-audit-v0.5.0-deep.md` — 本计划(已存在)
2. `docs/plans/full-audit-v0.5.0-report.md` — 审计报告(Phase 5.1)
3. `docs/plans/phase0-regression-review.md` — 回归复审结果(Phase 0.1)
4. `docs/plans/fsrs-formula-diff.md` — FSRS 公式对比(Phase 2.A.1)
5. `docs/plans/compose-recomposition-report.md` — Compose 重组报告(Phase 2.D)
6. `docs/plans/dependency-upgrade-path.md` — 依赖升级路径(Phase 3)
7. `docs/plans/emulator-test-matrix.md` — 实测结果(Phase 4)
8. 代码修复 commit(Phase 5.2,若干个)
9. `docs/SESSION_LOG.md` 新增本轮会话记录
10. `docs/00-STATUS.md` 状态更新
11. `docs/03-FAILED-ATTEMPTS.md` 新增本轮失败方案(如有)

---

**本计划制定完成。** 共 5 Phase / 8 新维度 / 4 未修 P0 / 15 项实测矩阵 / ~30 项 Checklist。下一步:用户确认计划 → 执行 Phase 0(回归复审)→ Phase 1/2 并行 → Phase 4 实测 → Phase 5 出报告。
