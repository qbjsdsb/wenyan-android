# 文研 App 第三轮深度审计报告 v0.4.2

> **基准**：main @ `6ea0290`（UI 精修 v0.3 完成后未发版）
>
> **审计范围**：6 个全新维度（数据库层 / Hilt DI / Manifest+资源+ProGuard / 测试有效性 / 边缘情况与错误链路 / CVE 联网核实+FSRS 复审）
>
> **审计方法**：5 路并行 search subagent 深度代码审读 + 全局 grep 验证 + WebSearch CVE 联网核实 + FSRS 公式手算复审
>
> **本轮新增发现**：**9 项 P0** + **45 项 P1** + **70 项 P2**

---

## 0. 关键发现速览

### 三轮审计累计统计

| 严重度 | 第一轮 | 第二轮新增 | 第三轮新增 | 累计 |
|--------|--------|----------|----------|------|
| P0 | 3 | 2 | **9** | 14 |
| P1 | 12 | 14 | **45** | 71 |
| P2 | 10 | 15+ | **70** | 95+ |

### 第三轮 9 项 P0（必须立即修复）

| # | 维度 | 问题 | 文件 |
|---|------|------|------|
| **P0-D1** | 数据库 | `fallbackToDestructiveMigration` 启用，生产环境会丢用户数据 | [DatabaseModule.kt:67](file:///workspace/core/database/src/main/java/com/wenyan/app/core/database/di/DatabaseModule.kt) |
| **P0-D2** | 数据库 | SeedDataLoader 7 步导入无 @Transaction 包裹 | [SeedDataLoader.kt:99-238](file:///workspace/core/data/src/main/java/com/wenyan/app/core/data/seed/SeedDataLoader.kt) |
| **P0-M1** | Manifest | `allowBackup="true"` 泄露用户数据 | [AndroidManifest.xml:11](file:///workspace/app/src/main/AndroidManifest.xml) |
| **P0-M2** | Manifest | `versionCode=1` 从未递增，三个版本共用同一 versionCode | [app/build.gradle.kts:19](file:///workspace/app/build.gradle.kts) |
| **P0-E1** | 边缘 | 网络异常完全无差异化（401/超时/断网统一显示"请求失败"） | [AiServiceImpl.kt:79-84](file:///workspace/core/ai/src/main/java/com/wenyan/app/core/ai/AiServiceImpl.kt) |
| **P0-E2** | 边缘 | AI 对话完全无持久化，进程被杀即丢失全部历史 | [AiAssistantViewModel.kt:53](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt) |
| **P0-E3** | 边缘 | 全项目零 `rememberSaveable` / 零 `onSaveInstanceState`，进程被杀丢所有 UI 状态 | 全 feature 模块 |
| **P0-E4** | 边缘 | FSRS 时钟回拨导致卡片"永久消失"或"无限到期" | [SchedulingRepository.kt:67](file:///workspace/core/data/src/main/java/com/wenyan/app/core/data/repository/SchedulingRepository.kt) |
| **P0-T1** | 测试 | 测试有效性 7 项 P0（假断言 / companion var 泄漏 / assert() 静默跳过 / 真实网络测试 / FSRS 边界零覆盖） | 详见测试维度 |

> **注**：P0-T1 包含 7 项测试质量问题，合并为 1 个 P0 项跟踪。

---

## 1. 维度 A：数据库层深度审计

**关键发现**：2 P0 + 8 P1 + 12 P2

### P0 问题
- **P0-D1**：`fallbackToDestructiveMigration()` 在生产环境会静默清空整个数据库（含用户 FSRS 复习记录）。v0.2.0 已发布，用户有真实数据。**修正**：改用 `fallbackToDestructiveMigrationOnDowngrade()`。
- **P0-D2**：SeedDataLoader.importToDatabase 7 步导入无事务包裹，中途失败留下半成品数据 + DataStore 标志已写"initialized"→ 永久半成品。**修正**：注入 WenyanDatabase，用 `db.withTransaction { }` 包裹。

### P1 问题（8 项）
- **P1-D1**：GraphRepositoryImpl.getRelatedNodes / getAdjacentNodes N+1 查询（对每个相邻节点 ID 串行调 getById）
- **P1-D2**：GraphRepositoryImpl.getPrerequisites N+1 查询（同型问题）
- **P1-D3**：17 处 DAO observeXxx 缺 ORDER BY，结果顺序未定义 → Compose 重组时 item 顺序抖动
- **P1-D4**：StudyProgressEntity.lastPointId 无 ForeignKey + 无 Index → 悬挂引用 + 全表扫描
- **P1-D5**：chat_history 与 ai_conversations 表语义重复
- **P1-D6**：KnowledgePointDao.searchByKeyword LIKE 通配符风险（用户输入 % 全表扫描）
- **P1-D7**：MemoRecordDao.observeDue Flow 不按时间主动触发（用户长时间挂起 App，到期卡片数不刷新）
- **P1-D8**：CardRepository.getCardsForReview Flow.map 内对每知识点调用 suspend（N+1）

---

## 2. 维度 B：Hilt DI 配置审计

**关键发现**：0 P0 + 3 P1 + 6 P2

### 阴性结论 ✓
- DI 图完整，无 MissingBinding 运行时崩溃风险
- 跨模块 DI 绑定链完整，无循环依赖
- 所有 @Module 安装到 SingletonComponent 合理

### P1 问题（3 项）
- **P1-H1**：Dispatcher 全局硬编码（WenyanApplication + AiServiceImpl × 2），无 @Qualifier 注入，测试不可替换
- **P1-H2**：OkHttpClient 在生产构建中 BODY 级日志泄漏 API Key（前两轮 P0-3 确认未修复）
- **P1-H3**：无 @ViewModelScoped 分层（当前可接受，记录为架构注意事项）

### P2 问题（6 项）
- ApiKeyCrypto 用 @Provides 而非 @Binds（风格不一致）
- ThemeRepositoryImpl 缺 @Singleton 类注解
- ThemeViewModel 放置在 core:data 模块（分层异常）
- SeedDataLoader 绕过 DI 自建 DataStore（双重 DataStore 实例）
- 无 @HiltAndroidTest，DI 图未被 instrumented 测试验证
- 7 个 DAO @Provides 无消费者（死绑定）

---

## 3. 维度 C：Manifest + 资源 + ProGuard 审计

**关键发现**：2 P0 + 7 P1 + 8 P2

### P0 问题
- **P0-M1**：`allowBackup="true"` 泄露用户数据到 adb backup
- **P0-M2**：`versionCode=1` 从未递增，v0.1.0/v0.2.0/v0.3 三个版本共用同一 versionCode

### P1 问题（7 项）
- **P1-M1**：versionName="0.1.0" 与实际 v0.3 不匹配
- **P1-M2**：SettingsScreen 版本号硬编码 "v0.1.0"
- **P1-M3**：13 个 .pro 规则文件全空（启用 minify 即崩）
- **P1-M4**：strings.xml 仅含 app_name，全项目 0 处 stringResource() 调用
- **P1-M5**：Release 签名 fallback 到 debug 签名
- **P1-M6**：themes.xml 使用框架 legacy 主题 `android:Theme.Material.Light.NoActionBar`，未用 Material3 DayNight
- **P1-M7**：无 values-night 目录，dark mode 启动时白屏闪烁 ~200ms

### P2 问题（8 项）
jvmTarget 未显式设置 / colors.xml 无 dark 变体 / 无 isShrinkResources / 无 backup rules / launcher foreground 硬编码色 / settings consumer-rules 完全空白 / 无 localeConfig / 无 dimens.xml

---

## 4. 维度 D：测试有效性深度审计

**关键发现**：7 P0 + 12 P1 + 12 P2

### P0 问题（7 项，合并跟踪为 P0-T1）
- **P0-T1a**：AntiRoteMemorizationTest 弱断言（在 Boolean/String 非空字段上做 assertNotNull，恒真）
- **P0-T1b**：AiAssistantViewModel.messageCounter companion var 跨测试实例状态泄漏
- **P0-T1c**：WenyanNavigationBarTest 用 Kotlin `assert()` 而非 JUnit `assertEquals`（-ea 关闭时静默跳过）
- **P0-T1d**：AiServiceImplTest 使用真实网络（http://127.0.0.1:1），CI 易抖
- **P0-T1e**：FSRS coerceIn(1f, 10f) 边界完全未覆盖（核心算法）
- **P0-T1f**：FSRS 4 档评分状态机只测了 2 档（AGAIN/GOOD），HARD/EASY + LEARNING/REVIEW/RELEARNING 12 个组合零覆盖
- **P0-T1g**：3 个图谱测试全用 runBlocking 而非 runTest（反模式）

### 测试有效性评分
| 模块 | 评分 | 关键缺陷 |
|------|------|----------|
| core:fsrs | B | coerceIn 边界 0 覆盖、4 档评分只测 2 档 |
| core:data (graph) | B- | runBlocking 滥用、Fake 写方法空实现 |
| core:data (Theme/ApiConfig) | B+ | FakeApiConfigDao 实现真实、断言较精确 |
| core:ai (Rag/Socratic) | B- | AiServiceImplTest 真实网络抖、Fake 状态不真实 |
| core:ai (recall) | B | AntiRoteMemorizationTest 假断言 |
| core:designsystem | B | assert() 危险用法 |
| feature:cards | A- | 边界覆盖良好 |
| feature:aiassistant | B- | companion var 状态泄漏 |
| feature:knowledge | C+ | ViewModel 状态机零覆盖 |
| feature:settings/quiz/graph | D | 完全无测试 |

**整体评级：B-**（存在 P0 级测试可信度问题）

---

## 5. 维度 E：边缘情况与错误链路审计

**关键发现**：5 P0 + 17 P1 + 30 P2

### P0 问题（5 项，已在速览列出）

### P1 问题（17 项，重点）
- 无 ConnectivityManager 网络可用性检测
- 超时无重试机制
- JSON 解析失败未友好提示
- Retrofit errorBody 未解析
- FSRS 并发评分竞态（同一卡片快速评分两次，FSRS stability 推进错乱）
- AI 请求响应错位（快速发两条消息，响应顺序错乱）
- CardsViewModel 无"牌组完成"显式状态（末尾重复评分）
- TypeConverters 无错误处理
- MemoRecordMapper.appendReviewLog 脆弱 JSON 拼接
- ApiConfigScreen 表单 baseUrl 无格式校验
- temperature/maxTokens 输入静默丢弃
- apiKey 无格式校验
- AiAssistant 输入无长度限制
- KnowledgePointDetailScreen scrollState 在 Crossfade 内
- WenyanApplication 启动时未等待种子加载完成
- ExamCountdownManager.lastPhaseChangeDate 未持久化
- GraphScreen avgRetrievability 计算含未学习节点

---

## 6. 维度 F：CVE 联网核实 + FSRS 修复方案复审

### 6.1 CVE 联网核实结果

| 依赖 | 当前版本 | CVE 状态 | 建议 |
|------|---------|---------|------|
| okhttp | 4.12.0 | **无已知 CVE**（4.x 系列干净） | 可保留，择期升 5.x |
| retrofit | 2.9.0 | CVE-2018-1000850 影响 2.0-2.5.0，**2.9.0 不受影响** | 可保留，择期升 2.11+ |
| coroutines | 1.8.1 | 未发现 CVE | 可保留 |
| security-crypto | 1.1.0-alpha06 | **无 CVE，但 alpha 状态持续 3 年+**（2023-04 发布，至今无 stable） | **建议评估替代方案**（Android Keystore 直接封装） |

**security-crypto 特别说明**：
- 1.1.0-alpha06 发布于 2023-04-19
- 1.1.0-alpha07 发布于 2025-05-02（仅小版本迭代）
- 至 2026-07 仍未有 stable 版本
- alpha 状态持续 3 年+，说明该库维护缓慢，API 可能不稳定
- 用于存储 LLM API key（高敏感），建议评估替代方案

### 6.2 FSRS 修复方案复审

经手算复审，前两轮 v0.4.1 报告的 FSRS 修复方案**全部正确**：

- **F-01**（nextDifficulty 权重索引）：`w[5]→w[6]`, `w[6]→w[7]` ✓
  - 验证：w[5]=1.0526（难度变化系数），w[6]=0.5699（均值回归系数，代码误用），w[7]=0.2197（正确的均值回归系数）
  - 修正后：均值回归从 57% 降到 22%，符合 FSRS-6 spec

- **F-02**（easyBonus 语义反转）：`w[16]` → `1f + w[16]` ✓
  - 验证：w[16]=0.2316 < 1，直接用作乘子导致 EASY < GOOD
  - 修正后：EASY stability = 32.64 > GOOD stability = 28.38 ✓

- **F-03**（EASY interval/stability 不一致）：把 `recallS` 改为含 easyBonus ✓
  - 当前：stability=recallS*easyBonus 但 interval=nextInterval(recallS)（不含 bonus）
  - 修正后：stability=recallS*easyBonus，interval=nextInterval(recallS*easyBonus) ✓

- **F-05**（nextInterval 截断）：`toInt()` → `roundToInt()` ✓

- **F-04**（w[17]-w[20] 未使用）：标注为"已知限制"，非 bug

---

## 7. 综合修复计划（4 个 Batch）

### Batch 1：FSRS 算法正确性（最高优先级）
- F-01: nextDifficulty 权重索引修正
- F-02: easyBonus 改 1+w[16]
- F-03: EASY 评分 interval/stability 一致性
- F-05: nextInterval round 替代 toInt
- 补测试：coerceIn 边界、4 档评分状态机、EASY > GOOD

### Batch 2：数据安全 P0
- P0-M1: allowBackup=false
- P0-M2: versionCode=3 + versionName="0.3.0"
- P0-D1: fallbackToDestructiveMigration → OnDowngrade
- P0-D2: SeedDataLoader 加 withTransaction
- P1-H2/P0-E1: HttpLoggingInterceptor 区分 Debug/Release

### Batch 3：测试有效性 P0
- P0-T1a: 删除 AntiRoteMemorizationTest 假断言
- P0-T1c: WenyanNavigationBarTest assert() → assertEquals
- P0-T1b: messageCounter 改 UUID
- P0-T1e/f: 补 FSRS coerceIn + 4 档评分测试（与 Batch 1 合并）

### Batch 4：关键 UX/契约 P1
- P1-M2: SettingsScreen 版本号改 BuildConfig.VERSION_NAME
- P1-NEW-7: ThemeRepositoryImpl 枚举 valueOf 容错
- P1-D3: 17 处 DAO 加 ORDER BY
- P1-D1/D2: GraphNodeDao.getByIds 批量查询（修 N+1）
- P1-NEW-4: CardsViewModel.rateCard 加 try/catch + isFinished
- P1-NEW-5: ApiConfigViewModel.editingId 局部量捕获

---

## 8. 审计质量自评

### 覆盖维度
- ✅ 第一轮 12 维度
- ✅ 第二轮 4 维度深度
- ✅ 第三轮 6 维度深度（数据库 / DI / Manifest / 测试 / 边缘 / CVE）
- ✅ CVE 联网核实（okhttp / retrofit / coroutines / security-crypto）
- ✅ FSRS 修复方案手算复审

### 局限性
- ⚠️ 仍需 emulator 实测验证运行时行为
- ⚠️ Compose Compiler Metrics 未生成（recomposition 跟踪需实测）
- ⚠️ security-crypto 替代方案需进一步调研
- ⚠️ AI 对话持久化（P0-E2）和进程被杀状态恢复（P0-E3）工作量大，本轮修复计划未包含，留待下个迭代

---

**第三轮深度审计完成。** 累计三轮共发现 14 P0 + 71 P1 + 95+ P2。最紧急的是 FSRS 算法正确性（F-01/F-02/F-03/F-05）和数据安全 P0（allowBackup / versionCode / fallbackToDestructive / SeedLoader 事务 / OkHttp 日志）。修复计划分 4 个 Batch，按优先级顺序执行。
