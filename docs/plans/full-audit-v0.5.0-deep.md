# 文研 App 第四轮深度审计计划 v0.5.0

> **基准**:main @ `7ae628f`(v0.4.2 修复完成后)
>
> **本计划性质**:深度审计**计划文档**(audit plan),不是审计报告。先制定检查清单 → 逐项执行 → 输出报告 → 修复 → 验证。
>
> **审计目标**:在 v0.4.2 修复基础上,完成"零未修 P0 + 全代码无死角复审 + 业务逻辑正确性验证 + 实测运行时行为",达到可发 v0.3.0 Release 的状态。
>
> **审计方法**:每个维度配 ① 检查项清单(可勾选) ② 必查文件列表 ③ 验证方法(grep/test/emulator) ④ 输出格式 ⑤ 通过标准。
>
> **总规模**:160 个 .kt 文件 / 19,721 行代码 / 13 个模块 / 27 测试文件 / 1 instrumented test / 13 .pro 文件 / 3 workflow / 154 行 libs.versions.toml。
>
> **v2 修订**(基于 5 个深度 subagent 预扫描):新增 **5 个未识别 P0** + **23 个 P1** + 修正 Phase 1 修复列表 + 新增 Phase 1.E/F/G/H/I。
>
> **v3 修订**(基于第二轮 5 个深度 subagent 扫描,覆盖 Navigation/Lifecycle/Manifest/Hilt/测试质量/资源泄漏/线程安全/类型安全/错误处理/模块边界/Compose 副作用/Accessibility/M3 Expressive/业务边界/DataStore/进度持久化 15 个新维度):新增 **~25 个未识别 P0** + **~80 个 P1** + **6 个新维度汇总小节(§2.8-2.13)** + **Phase 1 扩展为 18 项(新增 1.J-1.S)** + **Phase 2 扩展为 15 维度(新增 2.I-2.O 七个审计细节,§2.10 拆为 2.K 代码级 + 2.L 架构级)** + **Phase 5 扩展为 7 Batch** + **emulator 测试矩阵扩展为 25 项**。累计 **P0 ~34 项 / P1 ~100 项 / Checklist ~85 项**。

---

## 0. 总览:本轮审计要解决的 18 个核心问题

| # | 问题 | 当前状态 | 本轮目标 |
|---|------|---------|---------|
| **Q1** | v0.4.2 修复 24 文件是否引入回归? | 未验证 | 全量回归测试 + 24 文件逐行复审 |
| **Q2** | 4 个未修 P0(E1/E2/E3/E4)何时修? | 仍开 | 出详细修复方案 + 至少修 2 个 |
| **Q3** | 前三轮未覆盖的维度有哪些? | v2 补 8 维度,v3 再发现 7 维度 | 补齐 15 个新维度(2.I-2.O) |
| **Q4** | 运行时实际行为是否符合预期? | 0 emulator 实测 | 至少跑 1 次完整 emulator 实测 |
| **Q5** ⚠ v2 | FSRS 实现到底是 FSRS-4/5 还是 FSRS-6? | 公式与版本声明不符 | 出定位决策 + 修或文档化 |
| **Q6** ⚠ v2 | ThemeViewModel 为何 5 处全裸 launch? | App 崩溃风险 | 立即修复 |
| **Q7** ⚠ v2 | RecallChecker L3 LLM 为何永不触发? | 三层退化为两层 | 修 if/else bug |
| **Q8** ⚠ v2 | Migration_1_2 是否回填 reps? | 未回填,已发版用户数据错乱 | 升 v3 + 回填 SQL |
| **Q9** ⚠ v3 | 7/8 ViewModel 为何不用 SavedStateHandle? | 旋转/进程被杀丢所有交互状态 | 全部 ViewModel 加 SavedStateHandle |
| **Q10** ⚠ v3 | 种子数据加载链路为何无防御? | 双 DataStore / 无 IO 兜底 / REPLACE 覆盖用户数据 / 失败重复导入 | 重构种子加载链路 |
| **Q11** ⚠ v3 | core:data 为何反向依赖 core:designsystem? | 架构污染,UI 层被数据层依赖 | Color → Long 转换,移除反向依赖 |
| **Q12** ⚠ v3 | res/xml 目录为何全缺? | Android 12+ 备份规则/网络安全/per-app 语言全无配置 | 创建 5 个 xml 配置文件 |
| **Q13** ⚠ v3 | debug 构建为何无三大基础工具? | 无 StrictMode / 无 LeakCanary / 无 Splash Screen API | 三大工具一次性补齐 |
| **Q14** ⚠ v3 | 错误处理为何全靠 catch(Exception)+字符串? | 无 sealed 错误类型 / Repository 零 try/catch / 几乎无日志 | 引入 Timber + sealed AppError |
| **Q15** ⚠ v3 | 线程安全为何全靠"单线程巧合"? | 0 处 @Volatile / Atomic / Mutex / synchronized | 建立项目级规范 + editingId 改 StateFlow |
| **Q16** ⚠ v3 | 解密失败为何让整个 Flow 永久 failed? | AndroidKeyStore 失效后 AI 配置全死 | runCatching + 标记损坏配置 |
| **Q17** ⚠ v3 | 复习日志为何双写(JSON + 表)? | memo_records.history JSON 与 review_logs 表易不一致 | 废弃 JSON 字段,统一用表 |
| **Q18** ⚠ v3 | 错题本为何未实现? | spec 提到但无 wrong_answers 表 | 新增表 + Dao + ViewModel 集成 |

---

## 1. 执行顺序与里程碑(v3 修订)

```
Phase 0  回归性复审(确认 v0.4.2 修复无回归)            [必做,P0]
  ├─ 0.1 28 文件逐行 diff 复审
  ├─ 0.2 全量测试重跑(207 tests 必须 0 失败)
  └─ 0.3 assembleDebug + lint 全跑

Phase 1  P0 修复方案设计 + 实施(扩展为 18 项)            [必做,P0]
  ├─ 1.A P0-E1 网络异常差异化(中等)
  ├─ 1.B P0-E4 FSRS 时钟回拨防护(中等)
  ├─ 1.C P0-E2 AI 对话持久化(大,出方案)
  ├─ 1.D P0-E3 进程被杀状态恢复(大,出方案)
  ├─ 1.E ⚠ v2 P0-F1 FSRS 公式版本定位决策(最高优先级)
  ├─ 1.F ⚠ v2 P0-V1 ThemeViewModel 5 处 launch 加 try/catch(立即可修)
  ├─ 1.G ⚠ v2 P0-A1 RecallChecker L3 触发逻辑修复(立即可修)
  ├─ 1.H ⚠ v2 P0-D1 Migration_2_3 回填 reps + schema 升级
  ├─ 1.I ⚠ v2 P0-DB MemoRecordDao upsert 改 @Update + INSERT IGNORE(防 history 丢失)
  ├─ 1.J ⚠ v3 P0-DS1 种子加载链路重构(双 DataStore 合并 + IO 兜底 + IGNORE 策略)
  ├─ 1.K ⚠ v3 P0-N1 BottomBar 顶级路由过滤(立即可修,30 分钟)
  ├─ 1.L ⚠ v3 P0-L1+L2+L3 SavedStateHandle 全 ViewModel 注入(7 个 ViewModel)
  ├─ 1.M ⚠ v3 P0-S3+S2+S1 三大基础工具补齐(Splash + LeakCanary + StrictMode)
  ├─ 1.N ⚠ v3 P0-M8 res/xml 目录创建(backup_rules + data_extraction + network_security + locales_config)
  ├─ 1.O ⚠ v3 P0-E1+E2 解密异常 runCatching 包装(防 Flow 永久 failed)
  ├─ 1.P ⚠ v3 P0-T1 studyText!! 改 orEmpty()(立即可修,5 分钟)
  ├─ 1.Q ⚠ v3 P0-C1 ApiConfigViewModel.editingId 改 MutableStateFlow(立即可修)
  ├─ 1.R ⚠ v3 P0-BB3 PrerequisiteChecker 加环检测(防无限递归)
  └─ 1.S ⚠ v3 P0-EE1 MemoRecordMapper JSON 异常不再静默重置(立即可修)

Phase 2  新维度深度审计(扩展为 15 维度)                  [必做,P0]
  ├─ 2.A 业务逻辑正确性(扩展:含考研日期 + AntiRote + 边界值 + SocraticTutor 状态共享)
  ├─ 2.B Room SQL + 数据模型(扩展:含 1.json + 死依赖 + 双写一致 + LIKE 转义)
  ├─ 2.C 协程/Flow 深度(扩展:含 stateIn .catch + Repository combine .catch + first 超时)
  ├─ 2.D Compose 重组性能实测(扩展:含 @Immutable + derivedStateOf + contentType + textLayouts)
  ├─ 2.E 资源与本地化(扩展:含 themes.xml M3 + values-night + dimens + adaptive icon monochrome)
  ├─ 2.F 构建系统与 CI/CD(扩展:含 consumer-rules + Release minify + convention plugin + 反向依赖)
  ├─ 2.G 安全深度(扩展:含 Certificate Pinning + 密码硬编码 + networkSecurityConfig)
  ├─ 2.H 测试质量提升(扩展:含 6 Repository + Migration + TypeConverter + ApiKeyCrypto + SeedDataLoader)
  ├─ 2.I ⚠ v3 Navigation 图 + Lifecycle 边界(BackStack + BottomBar + SavedStateHandle + repeatOnLifecycle + BackHandler)
  ├─ 2.J ⚠ v3 Hilt DI 图 + Application/启动流程(@HiltAndroidTest + Splash + StrictMode + LeakCanary + 启动同步)
  ├─ 2.K ⚠ v3 资源泄漏 + 线程安全 + 类型安全 + 模块边界(Retrofit 单例 + Atomic + !! 清理 + 反向依赖 + internal)
  ├─ 2.L ⚠ v3 错误处理一致性 + 日志规范(sealed AppError + Timber + Snackbar 统一 + CancellationException)
  ├─ 2.M ⚠ v3 Compose 副作用 + Accessibility + M3 Expressive(LaunchedEffect + role + 触控目标 + TalkBack + MotionScheme + WideNavigationRail)
  ├─ 2.N ⚠ v3 业务边界 + DataStore 持久化 + 进度持久化(种子链路 + 双写统一 + 错题本 + 消息持久化 + FSRS 配置 + Key 治理)
  └─ 2.O ⚠ v3 Manifest + Android 配置(res/xml 目录 + backup_rules + network_security + locales_config + monochrome + dimens)

Phase 3  依赖升级路径与短期升级(扩展:含 retrofit 2.11 + LeakCanary + Splash + Timber) [必做,P1]
Phase 4  emulator 实测与运行时验证(扩展:25 项矩阵)        [必做,P0]
Phase 5  出审计报告 v0.5.0 + 修复 + 验证(7 Batch)         [必做,P0]
```

---

## 2. 深度预扫描发现汇总(v2 + v3)

> 在制定本计划时,已用 5 个 subagent 对代码做了深度预扫描。以下是已确认的、原 v0.5.0 计划未覆盖的新发现。完整报告见各维度 Phase。

### 2.1 FSRS 算法维度(5 个新发现,3 个 P0)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-F1** | 🔴 P0 | `nextRecallStability` 用 `exp(w[8])` 而非 `w[8]`(FSRS-4 公式) | `FsrsWrapper.kt:337` | 增长项放大约 3.02 倍,稳定性增长过快、间隔膨胀 |
| **NF-F2** | 🔴 P0 | `retrievability` 用 FSRS-4/5 公式 `(1+t/(9S))^(-1)`(decay=-1),非 FSRS-6 `(1+19t/(81S))^(-0.5)`(decay=-0.5) | `FsrsWrapper.kt:295` | R 计算值偏差,传导到 stability 更新 |
| **NF-F3** | 🔴 P0 | `nextInterval` 公式基于 FSRS-4/5 `9*S*(1/R-1)`,非 FSRS-6 公式 | `FsrsWrapper.kt:363` | R_target ≠ 0.9 时偏差 |
| **NF-F4** | 🟠 P1 | `nextForgetStability` 用 FSRS-4/5 权重槽位 w[11]-w[14],FSRS-6 应用 w[15]-w[18] | `FsrsWrapper.kt:348-349` | 需对照官方 fsrs4anki 确认 |
| **NF-F5** | 🟠 P1 | `TierFsrsConfig.minInterval=1` 三档死字段,FsrsWrapper 无对应参数 | `TierFsrsConfig.kt:22` | 死代码,误导维护者 |
| **NF-F6** | 🟠 P1 | `stabilityGrowthFactor/easyBonus/againPenalty` 三档参数零测试覆盖 | `FsrsWrapperTest.kt` | 三档差异化能力无回归保护 |
| **NF-F7** | 🟠 P1 | 考研日期规则"12月倒数第二个周六"与实际"最后一个完整周末"可能有 1 周偏差 | `ExamCountdownManager.kt:66-72` | 倒计时 + 阶段切换可能错误 |
| **NF-F8** | 🟠 P1 | `getTransitionFactor` 注释"每天调整10%卡片",实际是全局保持率线性插值 | `ExamCountdownManager.kt:139` | 语义与注释不符 |

### 2.2 数据库维度(7 个新发现,3 个 P0/P1)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-D1** | 🔴 P0 | `Migration_1_2` 仅 `ALTER TABLE ADD COLUMN reps DEFAULT 0`,**未回填 `reps = review_count`** | `Migration_1_2.kt:17-28` | v0.2.0 用户升级后所有卡片 `reps=0`,FSRS 把老卡误判为新卡,复习间隔重置 |
| **NF-D2** | 🟠 P1 | `MemoRecordDao.upsert` 用 `OnConflictStrategy.REPLACE`,DELETE+INSERT 触发 FK CASCADE + 丢 history 字段 | `MemoRecordDao.kt:17-18` | FSRS 复习历史丢失 |
| **NF-D3** | 🟠 P1 | `MemoRecordDao.observeDue` 用 `strftime('%s','now')` 但 Flow 仅表数据变化才触发,**不会因时间推移自动刷新** | `MemoRecordDao.kt:36` | 用户挂起 App 后到期卡片数不刷新 |
| **NF-D4** | 🟠 P1 | `chat_history` 与 `ai_conversations` 表字段 90% 重叠,均未废弃 | `ChatHistoryEntity.kt` + `AiConversationEntity.kt` | AI 历史分散,字段维护翻倍 |
| **NF-D5** | 🟠 P1 | `ApiConfigEntity.api_key` Room 默认明文存储(业务层加密未验证) | `ApiConfigEntity.kt:48` | root 设备可 `adb pull` 直读 |
| **NF-D6** | 🟡 P2 | `schemas/` 目录只有 `2.json`,**无 `1.json`** | `core/database/schemas/` | MigrationTestHelper 无法自动验证 Migration_1_2 |
| **NF-D7** | 🟡 P2 | `WenyanTypeConverters` 空字符串与空集合不可逆 | `WenyanTypeConverters.kt:33,47` | 业务依赖 emptyList vs null 区分会出错 |

### 2.3 ViewModel/Repository 维度(3 个新发现,2 个 P0)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-V1** | 🔴 P0 | **ThemeViewModel 5 个 viewModelScope.launch 全无 try/catch** | `ThemeViewModel.kt:32/36/40/44/48` | DataStore 抛 IOException 时 App 崩溃(全局单例) |
| **NF-V2** | 🔴 P0 | **7/8 ViewModel stateIn 上游 Flow 链无 `.catch{}` 兜底** | CardsViewModel/ApiConfigViewModel/KnowledgeViewModel/KnowledgePointDetailViewModel/QuizViewModel/GraphViewModel/ThemeViewModel | 异常时 StateFlow 进入 failed,UI 永远 stale |
| **NF-V3** | 🔴 P0 | **6 个 Repository 完全无单元测试**,含 SchedulingRepository(FSRS 调度核心入口) | CardRepository/ExamRepository/KnowledgeRepository/ReviewRepository/SchedulingRepository/LlmConfigProviderImpl | FSRS 4 个 bug 修复后无回归保护 |

### 2.4 AI 服务维度(2 个新发现,1 个 P0)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-A1** | 🔴 P0 | **RecallChecker L3 LLM 评估永不触发**:`if (HARD && coverage in range)` 两分支都 `emit(l2Result)`,从未调 `checkL3Llm()` | `RecallChecker.kt:57-63` | 三层渐进式回忆检测退化为两层,Spec 设计完全失效 |
| **NF-A2** | 🟠 P1 | RecallChecker L2 评分只输出 HARD/EASY,**缺少 GOOD 档** | `RecallChecker.kt:137-141` | FSRS 调度缺失 GOOD 评级 |

### 2.5 Compose UI/资源维度(4 个新发现,4 个 P0)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-U1** | 🔴 P0 | 全项目零 `rememberSaveable`,旋转 + 进程被杀丢所有 UI 状态 | 所有 feature 模块 | UX 严重退化 |
| **NF-U2** | 🔴 P0 | 全项目零 `stringResource`,strings.xml 仅 `app_name` | 9 个 Screen + CardRenderer | 无法 i18n,50+ 字符串散布 |
| **NF-U3** | 🔴 P0 | `themes.xml` 用 `android:Theme.Material.Light.NoActionBar`(Android 5.0 框架主题,非 M3) | `app/src/main/res/values/themes.xml:4` | 不支持 M3 色彩 token + 不支持 DayNight |
| **NF-U4** | 🔴 P0 | 无 `values-night` 目录,深色模式启动闪米白屏 ~200ms | `app/src/main/res/` | 深色用户体验割裂 |
| **NF-U5** | 🟠 P1 | 全项目零 `@Stable`/`@Immutable`,所有 UiState 含 List 字段不稳定 | 所有 ViewModel UiState data class | 父重组强制子重组 |
| **NF-U6** | 🟠 P1 | 全项目零 `derivedStateOf`,多处每次重组重复计算 | WenyanApp L43-47, GraphScreen L148-149, KnowledgePointDetailScreen L67-75 | O(n) 重复计算 |
| **NF-U7** | 🟠 P1 | 4 处 `AnimatedVisibility` 未用 WenyanMotion spec | CardsScreen L136/L143, QuizScreen L402, SettingsScreen L147 | 动画风格不统一 |
| **NF-U8** | 🟠 P1 | `knowledge_detail → knowledge_detail` 无界 back stack | `WenyanNavHost.kt:94-99` | 用户点关联知识点无限堆栈 |

### 2.6 构建/CI/安全维度(4 个新发现,1 个 P0)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-B1** | 🔴 P0 | `release.yml` "Verify keystore" 步骤无条件执行,`KEYSTORE_BASE64` 未配置时 keytool 必崩 | `release.yml:63-70` | 首次配置 Release 时 workflow 必崩 |
| **NF-B2** | 🟠 P1 | 13 个 `.pro` 文件全空,Release `isMinifyEnabled=false` | 13 个 .pro + `app/build.gradle.kts:55` | APK 体积大 + 无代码混淆保护 + 启用即崩 |
| **NF-B3** | 🟠 P1 | `generate-keystore.yml` 密码硬编码 `Wenyan2026Release` | `generate-keystore.yml:37-38` | 任何能查看仓库的人都知道 keystore 密码 |
| **NF-B4** | 🟠 P1 | 无 convention plugin,7+ 模块配置高度重复 | 13 个 build.gradle.kts | 改一次配置需同步 7+ 文件 |
| **NF-B5** | 🟠 P1 | `core:designsystem → core:database` 反向依赖 | `core/designsystem/build.gradle.kts:36` | UI 层依赖数据层,分层异常 |
| **NF-B6** | 🟠 P1 | `core:ai` 直接依赖 `core:database` DAO(RagEngine 用 KnowledgePointDao) | `core/ai/build.gradle.kts:35` | 应通过 Repository 接口反转 |
| **NF-B7** | 🟡 P2 | `security-crypto 1.1.0-alpha06` 是死依赖,ApiKeyCryptoImpl 用 AndroidKeyStore + javax.crypto,未 import `androidx.security.crypto.*` | `libs.versions.toml:50` + `core:ai/build.gradle.kts:55` | alpha 死依赖,应移除 |
| **NF-B8** | 🟡 P2 | `libs.versions.toml` 5 个 `wenyan-feature-*` library 声明从未被引用(模块用 `project(":feature:xxx")`) | `libs.versions.toml:139-143` | 死声明 |
| **NF-B9** | 🟠 P1 | `retrofit 2.9.0` + `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0`(已停维) | `libs.versions.toml:37, 42` | 应升 retrofit 2.11+ 用官方 converter |

### 2.7 v0.4.2 修复回归性确认(subagent 已核对)

| 修复项 | 状态 | 证据 |
|--------|------|------|
| F-01 nextDifficulty w[6]/w[7] | ✅ 已修 | FsrsWrapper.kt:320-324 |
| F-02 easyBonus = 1 + w[16] | ✅ 已修 | FsrsWrapper.kt:333 |
| F-03 EASY interval/stability 一致 | ✅ 已修 | FsrsWrapper.kt:237 |
| F-05 nextInterval roundToInt | ✅ 已修 | FsrsWrapper.kt:364 |
| P0-M1 allowBackup=false | ✅ 已修 | AndroidManifest.xml:11 |
| P0-M2 versionCode=3 + versionName=0.3.0 | ✅ 已修 | app/build.gradle.kts:21-23 |
| P0-D1 fallbackToDestructiveMigrationOnDowngrade | ✅ 已修 | DatabaseModule.kt:70 |
| P0-D2 SeedDataLoader withTransaction | ✅ 已修 | SeedDataLoader.kt:109 |
| P0-T1c WenyanNavigationBarTest assert→assertEquals | ✅ 已修 | WenyanNavigationBarTest.kt |
| P1-NEW-7 ThemeRepositoryImpl 枚举 valueOf runCatching | ✅ 已修 | ThemeRepositoryImpl.kt:49-54 |
| P1-D1/D2 GraphRepositoryImpl 3 处 N+1 修复 | ✅ 已修 | GraphRepositoryImpl.kt:57-59/67-73/81-87 |
| P1-NEW-4 CardsViewModel rateCard try/catch + isFinished | ✅ 已修 | CardsViewModel.kt:123-139 |
| P1-NEW-5 ApiConfigViewModel editingId 局部量捕获 | ✅ 已修 | ApiConfigViewModel.kt:142-143 |
| P0-3 HttpLoggingInterceptor Release NONE | ✅ 已修 | AiModule.kt:42-49 + core:ai/buildConfig=true |
| **18 处 DAO ORDER BY** | ✅ 全部到位 | 全 19 DAO 已复审 |
| **3 处 N+1 修复(GraphRepository)** | ✅ 已修 | getByIds + associateBy |

### 2.8 Navigation 图 + Lifecycle 边界维度(v3 新增,10 个 P0/P1)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-N1** | 🔴 P0 | BottomBar 在所有子路由(knowledge_detail/settings/api_config)上仍显示,违反 M3 准则 + 与 AiAssistantScreen InputBar 叠加冲突 | `WenyanApp.kt:49-63` | UX 退化 + 误触 Tab 弹栈 |
| **NF-N2** | 🟠 P1 | 路由字符串硬编码(`"knowledge_detail/{pointId}"`),无类型安全,IDE 无补全,拼写错误编译期不报 | `WenyanNavHost.kt:211-213` | 重构困难,运行时崩溃 |
| **NF-N8-deep** | 🔴 P0 | knowledge_detail → knowledge_detail 无界 back stack(深入核对 NF-U8),无 popUpTo/launchSingleTop | `WenyanNavHost.kt:94-99` + `KnowledgePointDetailScreen.kt:426` | back 键需逐层按 N 次,内存膨胀 |
| **NF-L1** | 🔴 P0 | ApiConfigViewModel.editingId 是 var,未持久化到 SavedStateHandle,进程被杀即丢 | `ApiConfigViewModel.kt:48` | 编辑模式变新建模式,误转"设为当前" |
| **NF-L2** | 🔴 P0 | CardsViewModel._isFlipped 和 _currentIndex 未持久化,进程被杀丢卡片位置 | `CardsViewModel.kt:42-45` | 学习连续性中断 |
| **NF-L3** | 🔴 P0 | QuizViewModel._selectedYear 和 _expandedQuestionIds 未持久化 | `QuizViewModel.kt:37-41` | 真题浏览位置丢失 |
| **NF-L5** | 🔴 P0 | applicationScope 启动 seed 加载无超时,seed 文件损坏永久挂起 | `WenyanApplication.kt:39-48` | UI 永远显示空列表 |
| **NF-L6** | 🔴 P0 | 种子加载未与 Splash 同步,首次冷启动 UI 空白数秒 | `WenyanApplication.kt:43-48` | 用户误以为 App 无内容 |
| **NF-L7** | 🟠 P1 | KnowledgePointDetailViewModel.pointId 一次性读取,后续不观察 SavedStateHandle 变化 | `KnowledgePointDetailViewModel.kt:35` | 同路由实例下 pointId 变化不更新 |
| **NF-M6** | 🟠 P1 | MainActivity 缺少 android:configChanges,旋转屏 Activity recreate | `AndroidManifest.xml:18-27` | 与 NF-U1 叠加丢 UI 状态 |

### 2.9 Hilt DI 图 + 启动流程维度(v3 新增,8 个 P0/P1)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-H2** | 🔴 P0 | 27 个测试文件零 @HiltAndroidTest,DI 图无自动化验证 | 全测试目录 | 绑定完整性仅运行时暴露 |
| **NF-H8** | 🟠 P1 | 两个独立 DataStore 实例(wenyan_preferences + wenyan_seed_prefs),管理分散 | `DataStoreModule.kt:24-30` + `SeedDataLoader.kt:63-65` | Key 命名空间分散 |
| **NF-S1** | 🟠 P1 | 无 StrictMode 配置(debug 构建),主线程 IO/网络违规无检测 | `WenyanApplication.kt:43-48` | 开发期难快速发现主线程阻塞 |
| **NF-S2** | 🟠 P1 | 无 LeakCanary 配置(debug 构建),Compose+Hilt+ViewModel 链路泄漏无自动检测 | `app/build.gradle.kts:82-138` | v0.5.0 计划 Phase 4.3 提到的 LeakCanary 根本未配置 |
| **NF-S3** | 🔴 P0 | 无 Splash Screen API(Theme.SplashScreen),冷启动白屏 ~200ms | `themes.xml:4` + 全项目零 installSplashScreen | UX 严重退化 |
| **NF-S7** | 🟠 P1 | CoroutineExceptionHandler 仅 Log.e,无上报机制,seed 加载失败用户无感知 | `WenyanApplication.kt:35-37` | 永久失败时用户看不到种子数据 |
| **NF-H1** | 🟡 P2 | WenyanApplication 未实现 Configuration.Provider,未来 WorkManager 与 Hilt 冲突 | `WenyanApplication.kt:30` | 预留,当前无影响 |
| **NF-H4** | 🟠 P1 | 6 个 Repository 无接口,直接 @Inject constructor,无法 Mock 测试 | `DataModule.kt:35-41` | 测试困难,更换实现需改 ViewModel |

### 2.10 资源泄漏 + 线程安全 + 类型安全 + 错误处理维度(v3 新增,29 个 P0/P1)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-R1** | 🔴 P0 | Retrofit 实例每次 chat() 调用都重建,丢失 ServiceMethod 缓存,GC 压力 | `AiServiceImpl.kt:101-108` | 高频对话卡顿 |
| **NF-C1** | 🔴 P0 | ApiConfigViewModel.editingId 共享可变 var 非线程安全 | `ApiConfigViewModel.kt:48` | 跨调度器时 lost-update |
| **NF-C2** | 🔴 P0 | 全项目零线程同步原语(@Volatile/Atomic/Mutex/synchronized 全 0 处) | 全仓库 | 任何 IO 调度引入立即数据竞争 |
| **NF-C4** | 🟠 P1 | Repository 层 7+ 个 combine 链无 .catch 兜底 | GraphRepositoryImpl/ReviewRepository/ExamRepository | DB 异常致页面永久白屏 |
| **NF-C5** | 🟠 P1 | SocraticTutor/RecallChecker/InterferenceWarner 6 处 first() 无超时 | `SocraticTutor.kt:174,184,193,203,213` + `RecallChecker.kt:79` + `InterferenceWarner.kt:46` | LLM 阻塞时协程永久挂起 |
| **NF-T1** | 🔴 P0 | KnowledgePointDetailScreen 用 `point.studyText!!` 双叹号 | `KnowledgePointDetailScreen.kt:259` | 数据不一致时 NPE 崩溃 |
| **NF-T2** | 🟠 P1 | ExamCodeResolver 同一逻辑两种写法(?-let vs !!-短路) | `ExamCodeResolver.kt:41 vs 121` | 重构时易引入 NPE |
| **NF-T3** | 🟠 P1 | SchedulingRepository `?: TIER_CONFIGS[TIER_FRAMEWORK]!!` 双重兜底反模式 | `SchedulingRepository.kt:56` | 掩盖不变量假设 |
| **NF-T4** | 🟠 P1 | MemoRecordMapper Float↔Double 转换精度损失(DB Double → FSRS Float → DB Double) | `MemoRecordMapper.kt:63-64, 113-114` | FSRS 长期累积偏差 |
| **NF-T5** | 🟠 P1 | MemoRecordMapper 手动 StringBuilder 拼 JSON,无字符转义 | `MemoRecordMapper.kt:130-145, 150-166` | enum name 安全但扩展性差 |
| **NF-T6** | 🟠 P1 | AntiRoteMemorization log.rating.uppercase() 未防空,DB rating 列 null 时 NPE | `AntiRoteMemorization.kt:91, 111` | 脏数据下崩溃 |
| **NF-E1** | 🔴 P0 | ApiConfigRepository 4 处 `map { it.decrypted() }` 未 try/catch,KeyStore 失效 Flow 永久 failed | `ApiConfigRepository.kt:36, 50, 56, 72` | AndroidKeyStore 失效后 AI 配置全死 |
| **NF-E2** | 🔴 P0 | ApiConfigRepository `list.map { it.decrypted() }` 单项解密失败致全列表不可用 | `ApiConfigRepository.kt:50, 56` | 一条损坏数据连累全部 |
| **NF-E3** | 🟠 P1 | AiServiceImpl catch(Exception) 过宽 + 错误用 emit(String) 混入正常输出 + 无日志 | `AiServiceImpl.kt:60-85` | 错误归类不准,401 显示"网络错误" |
| **NF-E4** | 🟠 P1 | 5 个 ViewModel 9 处 catch(Exception) 过宽,可能误吞 CancellationException | AiAssistantViewModel/CardsViewModel/ApiConfigViewModel | 破坏结构化并发 |
| **NF-E5** | 🔴 P0 | 全项目几乎无日志(仅 1 处 Log.e),无 Timber,无统一 TAG | 全项目 | 线上排障几乎无日志可查 |
| **NF-E6** | 🟠 P1 | ThemeRepositoryImpl DataStore Flow 无 .catch,IOException 时主题流死亡 | `ThemeRepositoryImpl.kt:37` | DataStore 文件损坏后主题无法切换 |
| **NF-E7** | 🟠 P1 | MemoRecordMapper JSON 异常时重置丢历史数据,无日志无告警 | `MemoRecordMapper.kt:143` | 数百次复习历史一次损坏全丢 |
| **NF-E8** | 🟠 P1 | ApiKeyCryptoImpl 解密失败返回空字符串 "",合法空 apiKey 与失败无法区分 | `ApiKeyCryptoImpl.kt:61` | 错误标志反模式 |
| **NF-M1** | 🔴 P0 | core:designsystem 反向依赖 core:database(ContentSource 枚举错位) | `core/designsystem/build.gradle.kts:36` | UI 层依赖数据层,分层异常 |
| **NF-M2** | 🔴 P0 | core:ai 反向依赖 core:database(RagEngine 用 KnowledgePointDao,AntiRote 用 ReviewLogDao) | `core/ai/build.gradle.kts:35` | 应通过 Repository 接口反转 |
| **NF-MM1** | 🔴 P0 | core:data 反向依赖 core:designsystem(ThemeRepositoryImpl 用 Compose Color) | `core/data/build.gradle.kts:29` | 数据层依赖 UI 层,架构污染 |
| **NF-MM2** | 🟠 P1 | 全项目几乎不用 internal 修饰符(仅 3 处),90%+ 类全 public | 全项目 | 模块边界形同虚设 |
| **NF-MM3** | 🟠 P1 | 6 个 Repository 是 class 而非 interface,无接口/实现分离 | `core/data/.../repository/*.kt` | 无法 Mock,更换实现需改 ViewModel |
| **NF-MM4** | 🟠 P1 | Entity 跨模块暴露,ViewModel 直接用 Entity 而非 Domain Model | `core/data/build.gradle.kts:30` api(project(":core:database")) | DB schema 改动直接破坏 ViewModel |
| **NF-MM5** | 🟠 P1 | security-crypto 依赖在错误模块(core:ai 而非 core:data) | `core/ai/build.gradle.kts:55` | 依赖关系混乱 |
| **NF-C7** | 🟠 P1 | 多处用 System.currentTimeMillis() 而非 SystemClock.elapsedRealtime(),时钟回拨影响 FSRS | `GraphRepositoryImpl.kt:133,148` + `SchedulingRepository.kt:67,121` | 与 P0-E4 同源,补充细节 |
| **NF-T7** | 🟠 P1 | FsrsWrapper `w[rating.value - 1]` 数组下标依赖枚举顺序 | `FsrsWrapper.kt:303` | 重构脆弱,可能越界 |
| **NF-T8** | 🟠 P1 | FsrsWrapper applyFuzz 用全局 Random,不可注入,测试不可重复 | `FsrsWrapper.kt:374-382` | FSRS fuzz 测试只能验证范围 |

### 2.11 Compose 副作用 + Accessibility + M3 实际使用度维度(v3 新增,24 个 P0/P1)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-UC1** | 🟠 P1 | KnowledgePointDetailScreen scrollState 在 Crossfade content lambda 内,转场时滚动位置丢失 | `KnowledgePointDetailScreen.kt:122` | 后台刷新触发 isLoading 时滚动重置顶部 |
| **NF-UC2** | 🟠 P1 | WenyanTheme dynamicLightColorScheme/dynamicDarkColorScheme 未 remember,每次重组重建 ColorScheme | `WenyanTheme.kt:41-43` | Android 12+ 用户 GC 压力 |
| **NF-UC3** | 🟠 P1 | AiAssistantScreen LaunchedEffect(messages.size) 强制滚动到底部,打断用户上滑阅读 | `AiAssistantScreen.kt:93-97` | 新消息到达打断历史阅读 |
| **NF-UC4** | 🟠 P1 | AiAssistantScreen LaunchedEffect(errorMessage) 内 clearError 在 Composable 离开时不执行 | `AiAssistantScreen.kt:85-90` | 错误消息重复展示 |
| **NF-UC5** | 🟠 P1 | GraphCanvas pointerInput(nodes) 在 nodes 列表变化时重启手势检测,tap 可能丢失 | `GraphCanvas.kt:103-110` | R 值刷新瞬间点击无响应 |
| **NF-UA1** | 🔴 P0 | GraphCanvas Canvas 完全无障碍缺失,TalkBack 无法访问整个知识图谱 | `GraphCanvas.kt:100-111` | 视障用户无法使用图谱,违反 WCAG 2.1 Level A |
| **NF-UA2** | 🟠 P1 | AiAssistantScreen "知道了" Text clickable 触控目标 ~28dp,低于 48dp WCAG 标准 | `AiAssistantScreen.kt:365-375` | 手指粗用户难点击 |
| **NF-UA3** | 🟠 P1 | GraphCanvas 节点标签 fontSize=9.sp 硬编码,低于 WCAG 推荐最小 12.sp | `GraphCanvas.kt:88` | 视力不佳用户难阅读 |
| **NF-UA4** | 🟠 P1 | TonalCard .clickable 无 role=Role.Button 语义,TalkBack 不朗读"按钮" | `KnowledgeScreen.kt:174-177` + `ApiConfigScreen.kt:241-244` | 视障用户不知卡片可点击 |
| **NF-UA5** | 🟠 P1 | 全项目零 Modifier.semantics,无 liveRegion,状态变化不朗读 | 全项目 | TalkBack 用户在动态状态变化时无感知 |
| **NF-UM1** | 🟠 P1 | MotionScheme.expressive() 已设但被自定义 WenyanMotion 部分架空,三套动画规范并存 | `WenyanTheme.kt:75` + `WenyanMotion.kt:28-78` | 动画风格不统一,M3 Expressive 特性未生效 |
| **NF-UM2** | 🟠 P1 | 仅用 NavigationBar,未用 WideNavigationRail(M3 Expressive),平板/横屏体验差 | `WenyanApp.kt:52-62` | 平板底部导航不便 |
| **NF-UM3** | 🟠 P1 | 仅用 AlertDialog,未用 FlexibleBottomSheet(M3 Expressive),长表单体验差 | `ApiConfigScreen.kt:351-435` | 小屏 7 字段表单可能截断 |
| **NF-UM4** | 🟠 P1 | Typography 字重全 Normal/Medium,未用 M3 Expressive 的字重对比 | `Type.kt:14-94` | 标题层级不突出,缺乏 Expressive 表现力 |
| **NF-UP1** | 🟠 P1 | WenyanTheme AMOLED baseScheme.copy(...) 未 remember,每次重组创建新 ColorScheme | `WenyanTheme.kt:60-71` | AMOLED 用户 GC 压力翻倍 |
| **NF-UP2** | 🟠 P1 | SettingsScreen seedColors listOf 每次重组创建新 List | `SettingsScreen.kt:167-173` | 轻微 GC 压力 |
| **NF-UP3** | 🟠 P1 | KnowledgePointDetailScreen verticalScroll + forEachIndexed,长列表非 Lazy 渲染 | `KnowledgePointDetailScreen.kt:123-162, 337-343, 423-431` | 来源/关联多时首帧慢 |
| **NF-UP4** | 🟠 P1 | 7 处 LazyColumn/LazyRow items 缺 contentType,影响 item 复用 | KnowledgeScreen/QuizScreen/ApiConfigScreen/AiAssistantScreen | 混合 item 列表滚动性能略差 |
| **NF-UP5** | 🟠 P1 | GraphCanvas textLayouts remember 以 labelColor 为 key,主题切换全量重测量 | `GraphCanvas.kt:84-91` | 大图谱主题切换掉帧 |
| **NF-UT1** | 🔴 P0 | 9 个 Screen 零 smoke test,仅 3 个 designsystem 组件有 Compose 测试 | 全 Screen | Screen 层 UI 回归无保护 |
| **NF-UT2** | 🟠 P1 | GraphCanvas/CardRenderer/WenyanTheme 零测试,核心渲染与主题逻辑无回归保护 | 3 个文件 | 渲染/主题逻辑改动无保护 |
| **NF-UC6** | 🟡 P2 | 全项目零 DisposableEffect,无生命周期资源清理机制 | 全项目 | 当前无影响,扩展时技术债 |
| **NF-UC7** | 🟡 P2 | 全项目零 BackHandler,内部状态无 back 拦截(CardsScreen 翻转/QuizScreen 展开/AiAssistant 横幅) | 全项目 | back 键直接退出而非消费内部状态 |
| **NF-UM5** | 🟡 P2 | 7 处 Crossfade 全部缺 contentKey 参数(当前 Pair 稳定无 bug,但非最佳实践) | 7 个 Screen | 未来 targetState 改不稳定类型会闪烁 |

### 2.12 业务边界 + DataStore 持久化 + 进度持久化维度(v3 新增,38 个 P0/P1)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-DS1** | 🔴 P0 | 双 DataStore 实例(wenyan_preferences + wenyan_seed_prefs)违反单例 delegate 原则 | `DataStoreModule.kt:28` + `SeedDataLoader.kt:63` | Key 命名空间分散,扩展难统一治理 |
| **NF-DS2** | 🔴 P0 | SeedDataLoader.isInitialized() 无 IOException 兜底,异常冒泡到 Application | `SeedDataLoader.kt:80` | 磁盘 IO 故障被误判永久失败 |
| **NF-DS3** | 🔴 P0 | SeedDataLoader.markInitialized() 写失败冒泡,种子可能重复导入 | `SeedDataLoader.kt:85` | 用户校对后种子数据被覆盖 |
| **NF-DS6** | 🔴 P0 | 种子导入用 REPLACE 策略,可能与用户已建数据冲突覆盖 | `SeedDataLoader.kt:113-198` | 用户辛苦整理的数据被覆盖 |
| **NF-DS7** | 🟠 P1 | DataStore Key 分散在两文件(ThemeRepositoryImpl + SeedDataLoader),无集中定义 | 两文件 | Key 命名冲突风险 |
| **NF-DS8** | 🟠 P1 | DataStore Key 无版本前缀(v1_xxx),字段重命名无法 Migration | `ThemeRepositoryImpl.kt:77-81` | 字段重构致用户配置丢失 |
| **NF-DS10** | 🟡 P2 | seed_color 默认值 0xFF6750A4 硬编码在 Repository,未集中到 ThemeDefaults | `ThemeRepositoryImpl.kt:44` | 默认值散落 |
| **NF-EE1** | 🔴 P0 | MemoRecordMapper JSON 格式异常静默重置,丢失全部历史复习日志,无日志 | `MemoRecordMapper.kt:130-145` | 长期复习记录一次损坏全丢 |
| **NF-EE2** | 🟠 P1 | 全项目无自定义 Either/Try/sealed class 错误类型,全靠 catch+字符串 | 全项目 | UI 无法据错误类型显示不同按钮 |
| **NF-EE3** | 🔴 P0 | Repository 层零 try/catch,SQLiteConstraintException 直接抛到 ViewModel | `core/data/.../repository/*.kt` | 错误信息泄露 SQLite 表名/列名 |
| **NF-EE4** | 🟠 P1 | Snackbar 错误消息格式不统一(14 处,5 种格式) | 14 处 Screen | 国际化困难,UX 不一致 |
| **NF-EE5** | 🟠 P1 | CardsViewModel 评分失败无重试按钮,仅设置 errorMessage | `CardsViewModel.kt:137-138` | FSRS 数据可能缺失 |
| **NF-EE6** | 🟡 P2 | WenyanApplication Log.e tag 未用 companion TAG 模式 | `WenyanApplication.kt:36` | 不规范 |
| **NF-DS11** | 🔴 P0 | 全项目仅 1 处 Log,无 Timber,无统一 TAG 体系 | 全项目 | 线上排障几乎无日志可查(与 NF-E5 同源,从 DataStore 角度) |
| **NF-BB1** | 🟠 P1 | KnowledgePointDao.searchByKeyword LIKE 未转义 %/_,100% 误匹配 1000 | `KnowledgePointDao.kt:84-87` | RAG 检索精度下降 |
| **NF-BB2** | 🟠 P1 | SocraticTutor 三阶段串行调 LLM,前阶段输出未作后阶段 context | `SocraticTutor.kt:43, 116, 137` | 三段输出可能矛盾 |
| **NF-BB3** | 🔴 P0 | PrerequisiteChecker 无环检测,循环依赖 A→B→A 会无限递归 | `PrerequisiteChecker.kt:41-52` | 误建循环依赖时 App 卡死/StackOverflow |
| **NF-BB5** | 🟠 P1 | ExamRepository.getRelatedKnowledgePoints 用 List 而非 Set,O(n*m) 复杂度 | `ExamRepository.kt:91-96` | 数据量大时卡顿 |
| **NF-BB6** | 🟠 P1 | AntiRoteMemorization 空关联卡片返回 0f(被判 false),应判 unknown | `AntiRoteMemorization.kt:107-108` | 新卡永不标记死记硬背 |
| **NF-BB8** | 🟠 P1 | FsrsWrapper.schedule 的 now 参数无校验,可被外部传入未来/过去时间 | `FsrsWrapper.kt:77, 142` | 误传未来时间致调度错乱 |
| **NF-BB9** | 🔴 P0 | Rating.fromValue 越界抛 NoSuchElementException,无防御 | `FsrsModels.kt` | DB rating 非法值致崩溃 |
| **NF-BB10** | 🟠 P1 | RagEngine.search 未处理 query 超长,LIKE 性能退化 + LLM 超 token | `RagEngine.kt:42` | 超长输入致 RAG 卡顿 + LLM 失败 |
| **NF-PP1** | 🟠 P1 | StudyProgressDao 仅 5 方法,无按日期/科目查询,学习时长未聚合 | `StudyProgressDao.kt:15-30` | 学习统计页无数据支撑 |
| **NF-PP2** | 🟠 P1 | FSRS 配置(targetRetention/maximumInterval)未持久化,用户无法调整 | `TierFsrsConfig.kt` | 用户无法根据自身情况调参 |
| **NF-PP4** | 🔴 P0 | 复习日志双写(review_logs 表 + memo_records.history JSON),易不一致 | `MemoRecordEntity.kt` + `ReviewLogDao.kt` + `MemoRecordMapper.kt:130` | 数据冗余 + 一致性风险 |
| **NF-PP5** | 🔴 P0 | 错题本未实现,无 wrong_answers 表,spec 提到但无实现 | `core/database` 全无 | 用户答错题无法独立收集复习 |
| **NF-PP6** | 🔴 P0 | AiAssistantViewModel 消息在内存 StateFlow,进程被杀即丢 | `AiAssistantViewModel.kt:53` | 长对话切应用回来消息消失 |
| **NF-BB4** | 🟡 P2 | CardSplitter.indexToChinese index>10 返回数字字符串,中文数字与阿拉伯数字混排 | `CardSplitter.kt:295` | 卡片标题风格不统一 |
| **NF-BB11** | 🟡 P2 | CardSplitter 未处理 100+ 标题场景,可能 O(n²) | `CardSplitter.kt:73-80` | 极端数据下性能退化 |
| **NF-BB12** | 🟡 P2 | WeakSubgraphDetector 孤儿边静默丢弃,无日志 | `WeakSubgraphDetector.kt:137-139` | 数据不一致问题难发现 |
| **NF-BB13** | 🟡 P2 | PrerequisiteChecker.retrievabilityThreshold 硬编码 0.7f | `PrerequisiteChecker.kt:52` | 阈值不可配置 |
| **NF-BB14** | 🟡 P2 | AntiRoteMemorization 阈值硬编码(STREAK=5, ERROR_RATE=0.4f) | `AntiRoteMemorization.kt:74` | 灵敏度不可调 |
| **NF-DS12** | 🟡 P2 | Migration 1→2 无 schema 1.json(与 NF-D6 同源,从 DataStore 角度补充) | `Migration_1_2.kt:19-27` | Migration 逻辑错误无自动测试 |
| **NF-MM3-deep** | 🟠 P1 | ReviewRepository.getAllVerifiedKnowledgePoints() 事实死代码职责越界(AGENTS.md §9 P4 已记录) | `ReviewRepository.kt:54` | Repository 职责越界 |
| **NF-DS9** | 🟡 P2 | Key 命名 color_mode 等无文档,无 KeyRegistry 集中索引 | 全 DataStore Key | 维护成本高 |
| **NF-PP3** | 🟡 P2 | 设置项持久化范围未审计(主题已持久化,AI 配置已,FSRS 配置未,学习历史部分) | 全设置项 | 部分设置项可能丢 |
| **NF-DS13** | 🟡 P2 | DataStore 写入是否在 IO 线程未统一审计(部分 Repository 用 .edit 默认 IO,部分可能未) | 全 DataStore 调用 | 主线程写入风险 |
| **NF-PP7** | 🟡 P2 | 学习历史(评分历史 AGAIN/GOOD/EASY)持久化范围未审计(ReviewLog 已,但与 memo_records.history 双写矛盾,见 NF-PP4) | 全历史持久化 | 与 NF-PP4 关联 |
| **NF-BB15** | 🟡 P2 | InterferenceWarner 相似度计算 > 1.0 浮点误差未 clamp | `InterferenceWarner.kt`(未读) | 极端情况相似度异常 |

### 2.13 Manifest + Android 配置维度(v3 新增,9 个 P0/P1)

| # | 严重度 | 问题 | 文件:行 | 影响 |
|---|--------|------|---------|------|
| **NF-M1** | 🟠 P1 | AndroidManifest 缺 android:fullBackupContent / android:dataExtractionRules(Android 12+) | `AndroidManifest.xml:9-16` | Android 12+ 备份行为不可预测 |
| **NF-M2** | 🟠 P1 | AndroidManifest 缺 android:networkSecurityConfig,无集中网络安全配置 | `AndroidManifest.xml:9-16` | 无 Certificate Pinning 配置位置 |
| **NF-M4** | 🟠 P1 | AndroidManifest 缺 android:enableOnBackInvokedCallback="true"(Android 13+ 预测返回) | `AndroidManifest.xml:9-16` | 预测返回手势不显示 App 内动画 |
| **NF-M8** | 🔴 P0 | res/xml 目录完全不存在(backup_rules/data_extraction/network_security/locales_config 全缺) | `app/src/main/res/xml/` | Android 12+ 配置基础设施缺失 |
| **NF-C3** | 🟠 P1 | 缺少 locales_config.xml(Android 13+ per-app language) | `app/src/main/res/xml/` | 未来加英文版无法系统切换 |
| **NF-C5** | 🟠 P1 | ic_launcher.xml adaptive-icon 缺 `<monochrome>` 元素(Android 13+ themed icon) | `mipmap-anydpi-v26/ic_launcher.xml:2-5` | Android 13+ themed icon 不生效 |
| **NF-C10** | 🟠 P1 | 缺少 dimens.xml,CardRenderer 等硬编码 dp 重灾区 ~20+ 处 | `app/src/main/res/values/` | 间距/字号调整需逐文件改 |
| **NF-M3** | 🟡 P2 | AndroidManifest 缺 android:usesCleartextTraffic="false" 显式声明(默认正确但维护者难判断) | `AndroidManifest.xml:9-16` | 与 NF-M2 重复,networkSecurityConfig 可替代 |
| **NF-M7** | 🟡 P2 | `<application>` 缺少 android:label,仅 `<activity android:label="@string/app_name">` 兜底 | `AndroidManifest.xml:9-16` | 应用列表显示用 activity label,不规范 |

---

## Phase 0 — 回归性复审(必做,P0)

> **目的**:确认 v0.4.2 commit `7ae628f` 28 文件改动无回归。subagent 已基本核对(见 §2.7),Phase 0 补充未审项。

### 0.1 28 文件逐行 diff 复审(subagent 已覆盖 90%,补 10%)

**已审 ✓**(subagent 核对完整):
- Batch 1 FSRS:`FsrsWrapper.kt` + `FsrsWrapperTest.kt` — F-01/F-02/F-03/F-05 全部到位
- Batch 2 数据安全:`AndroidManifest.xml` / `build.gradle.kts` / `DatabaseModule.kt` / `AiModule.kt` / `SeedDataLoader.kt`
- Batch 3 测试:`AntiRoteMemorizationTest.kt` / `WenyanNavigationBarTest.kt` / `AiAssistantViewModel.kt`
- Batch 4 UX/契约:`ThemeRepositoryImpl.kt` / `SettingsScreen.kt` / 5 DAO / `GraphRepositoryImpl.kt` / `CardsViewModel.kt` / `ApiConfigViewModel.kt`

**待补审**:
- [ ] 文档同步:AGENTS.md / 00-STATUS.md / 03-FAILED-ATTEMPTS.md / SESSION_LOG.md / full-audit-v0.4.2-deep.md
- [ ] `feature/settings/build.gradle.kts`:`buildConfigField("String", "VERSION_NAME", "\"0.3.0\"")` 是否硬编码(应自动从 app versionName 同步,见 NF-B4 相关)
- [ ] `core/ai/build.gradle.kts`:`buildConfig = true` 是否影响其他模块

### 0.2 全量测试重跑

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false \
  -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar \
  org.gradle.launcher.GradleMain :app:testDebugUnitTest --no-daemon 2>&1 | tail -30
```

**通过标准**:207 tests 0 failures(数量不减),无新警告。

### 0.3 assembleDebug + lint 全跑

```bash
$JAVA_HOME/bin/java ... :app:assembleDebug :app:lintDebug --no-daemon 2>&1 | tail -50
```

**通过标准**:`assembleDebug` SUCCESSFUL,`lintDebug` 无新增 Error 级问题。

### 0.4 ⚠ 新 v0.4.2 修复副作用检查

- [ ] `versionCode` 从 1 跳到 3,已安装 v0.2.0(versionCode=1)用户升级会触发数据迁移路径 → 复审 Migration_1_2 是否真的在升级路径上(见 NF-D1)
- [ ] `withTransaction` 包裹 7 步导入,中途失败回滚后 DataStore `KEY_SEED_INITIALIZED` 标志是否真的不会写(已在事务外,核对 SeedDataLoader.kt:76)
- [ ] `themeRepositoryImpl` runCatching 兜底是否真的不影响正常枚举值

---

## Phase 1 — P0 修复方案设计 + 实施(扩展为 18 项,必做,P0)

> **修订**:
> - v2:基于深度预扫描发现,Phase 1 从 4 项扩展为 9 项。1.F/1.G/1.H 立即可修;1.E 是最高优先级决策;1.A-1.D 沿用原计划。
> - v3:基于第二轮深度扫描,Phase 1 从 9 项扩展为 18 项。新增 1.J-1.S 共 10 项(种子链路 / BottomBar / SavedStateHandle / 基础工具 / res/xml / 解密容错 / !!清理 / editingId / 环检测 / JSON 不重置),其中 1.P / 1.Q / 1.S 立即可修(< 30 分钟)。

### 1.A P0-E1 网络异常差异化(中等,优先修)

**问题**:`AiServiceImpl.kt:79-84` 把 401/超时/断网/JSON 错误统一显示"网络错误"。

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

2. `AiServiceImpl.request` 内 catch 链分层捕获:
   - `IOException` → 检查 `ConnectivityManager` 区分 timeout vs no connectivity
   - `HttpException(code=401)` → `Unauthorized`
   - `HttpException(code>=500)` → `ServerError`
   - `JsonDecodeException` → `JsonParse`
   - 其他 → `Unknown`

3. ViewModel 把 `AiError` 类型透传到 UI,UI 显示对应文案 + 图标。

4. **额外修复**:解析 Retrofit `errorBody`(很多 OpenAI 兼容服务在 errorBody 中返回 `error.message`)。

**验证**:`AiServiceImplErrorMappingTest`,用 MockWebServer 模拟 5 种错误响应。

### 1.B P0-E4 FSRS 时钟回拨防护(中等,优先修)

**问题**:`SchedulingRepository.kt:67` 用 `System.currentTimeMillis()` 计算到期,用户改系统时间会导致卡片"永久消失"或"无限到期"。

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

**验证**:`SchedulingRepositoryClockTest`:模拟时钟前移 + 回拨 5 分钟 + 回拨 1 小时,断言 `dueCards` 行为正确。

### 1.C P0-E2 AI 对话持久化(大,出方案 — 完整实现级设计)

**问题**:`AiAssistantViewModel` 把消息放在内存 `StateFlow<List<AiMessage>>`,进程被杀即丢。

**现状调研**(2026-07-15):
- `chat_history` 表 + `ChatHistoryDao`:已注册,**无任何 Repository 引用**(死代码)
- `ai_conversations` 表 + `AiConversationDao`:已注册,**无任何 Repository 引用**(死代码)
- 两表字段 90% 重叠(NF-D4),`ai_conversations` 多 `is_bookmarked` + `context_screen_type` 改名
- `AiAssistantViewModel` 用 `MutableStateFlow<List<AiMessage>>` 纯内存,`clearMessages()` 直接清空
- `AiMessage` 含 `references: List<RagReference>` 和 `stage: SocraticStage?`,需序列化存储

**修复方案**(实现级设计):

#### 1.C.1 数据模型重整(结合 NF-D4)

合并 `chat_history` + `ai_conversations` → 规范化两表模型(对话 + 消息):

```kotlin
// 新表:chat_conversations(对话元数据)
@Entity(
    tableName = "chat_conversations",
    foreignKeys = [ForeignKey(
        entity = ApiConfigEntity::class, parentColumns = ["id"],
        childColumns = ["api_config_id"], onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("api_config_id"), Index("updated_at")]
)
data class ChatConversationEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,           // 首条用户消息前 30 字
    @ColumnInfo(name = "api_config_id") val apiConfigId: String?,
    @ColumnInfo(name = "model") val model: String?,           // 使用的模型名
    @ColumnInfo(name = "message_count") val messageCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,     // 最后消息时间
)

// 新表:chat_messages(消息内容,FK→对话)
@Entity(
    tableName = "chat_messages",
    foreignKeys = [ForeignKey(
        entity = ChatConversationEntity::class, parentColumns = ["id"],
        childColumns = ["conversation_id"], onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("conversation_id"), Index("created_at")]
)
data class ChatMessageEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "role") val role: String,              // USER / ASSISTANT
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "content_source") val contentSource: String?,  // AI_GENERATED 等
    @ColumnInfo(name = "stage") val stage: String?,           // SocraticStage 序列化
    @ColumnInfo(name = "references_json") val referencesJson: String?, // RagReference 列表 JSON
    @ColumnInfo(name = "context_screen") val contextScreen: String?,
    @ColumnInfo(name = "context_title") val contextTitle: String?,
    @ColumnInfo(name = "tokens_used") val tokensUsed: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
```

#### 1.C.2 Migration v4→v5

```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 创建新表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS chat_conversations (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                api_config_id TEXT,
                model TEXT,
                message_count INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(api_config_id) REFERENCES api_configs(id) ON DELETE SET NULL
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_conversations_api_config_id ON chat_conversations(api_config_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_conversations_updated_at ON chat_conversations(updated_at)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS chat_messages (
                id TEXT NOT NULL PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                content_source TEXT,
                stage TEXT,
                references_json TEXT,
                context_screen TEXT,
                context_title TEXT,
                tokens_used INTEGER,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_conversation_id ON chat_messages(conversation_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_created_at ON chat_messages(created_at)")

        // 2. 迁移存量数据:chat_history + ai_conversations → 单一默认对话 + messages
        //    (两表当前无数据,但保留迁移逻辑防用户已有历史)
        database.execSQL("""
            INSERT INTO chat_conversations (id, title, api_config_id, model, message_count, created_at, updated_at)
            SELECT 'migrated_legacy', '历史对话', NULL, NULL, COUNT(*), 
                   COALESCE(MIN(created_at), 0), COALESCE(MAX(created_at), 0)
            FROM (SELECT created_at FROM chat_history UNION ALL SELECT created_at FROM ai_conversations)
            HAVING COUNT(*) > 0
        """.trimIndent())

        // chat_history → chat_messages
        database.execSQL("""
            INSERT INTO chat_messages (id, conversation_id, role, content, content_source, stage, references_json, context_screen, context_title, tokens_used, created_at)
            SELECT id, 'migrated_legacy', role, content, NULL, NULL, NULL, context_screen, context_title, tokens_used, created_at
            FROM chat_history
        """.trimIndent())
        // ai_conversations → chat_messages(context_screen_type → context_screen)
        database.execSQL("""
            INSERT INTO chat_messages (id, conversation_id, role, content, content_source, stage, references_json, context_screen, context_title, tokens_used, created_at)
            SELECT id, 'migrated_legacy', role, content, NULL, NULL, NULL, context_screen_type, context_title, tokens_used, created_at
            FROM ai_conversations
        """.trimIndent())

        // 3. 删除旧表
        database.execSQL("DROP TABLE IF EXISTS chat_history")
        database.execSQL("DROP TABLE IF EXISTS ai_conversations")
    }
}
```

**注意**:当前两表无数据(死代码),迁移主要保证 schema 正确。exportSchema=true 需更新 schema JSON。

#### 1.C.3 DAO

```kotlin
@Dao
interface ChatConversationDao {
    @Upsert suspend fun upsert(entity: ChatConversationEntity)
    @Query("SELECT * FROM chat_conversations ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<ChatConversationEntity>>
    @Query("SELECT * FROM chat_conversations WHERE id = :id")
    suspend fun getById(id: String): ChatConversationEntity?
    @Query("DELETE FROM chat_conversations WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("UPDATE chat_conversations SET message_count = :count, updated_at = :updatedAt WHERE id = :id")
    suspend fun touch(id: String, count: Int, updatedAt: Long)
}

@Dao
interface ChatMessageDao {
    @Upsert suspend fun upsert(entity: ChatMessageEntity)
    @Query("SELECT * FROM chat_messages WHERE conversation_id = :convId ORDER BY created_at ASC")
    fun observeByConversation(convId: String): Flow<List<ChatMessageEntity>>
    @Query("DELETE FROM chat_messages WHERE conversation_id = :convId")
    suspend fun deleteByConversation(convId: String)
    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversation_id = :convId")
    suspend fun countByConversation(convId: String): Int
}
```

#### 1.C.4 Repository

```kotlin
interface ChatRepository {
    fun observeConversations(): Flow<List<ChatConversation>>
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    val currentConversationId: Flow<String?>
    suspend fun createConversation(title: String, apiConfigId: String?, model: String?): String
    suspend fun appendMessage(conversationId: String, message: ChatMessage)
    suspend fun deleteConversation(id: String)
    suspend fun setCurrentConversation(id: String?)
    suspend fun loadOrInitCurrent(): String?  // 启动时恢复
}
```

实现要点:
- 注入 `ChatConversationDao` + `ChatMessageDao` + `DataStore<Preferences>`(持久化 currentConversationId)
- `appendMessage` 内部:upsert message + `conversationDao.touch()` 更新 count/updatedAt
- `ChatMessage` ↔ `ChatMessageEntity` 映射:`references` 用 `kotlinx.serialization` JSON 序列化,`stage` 用 enum.name

#### 1.C.5 ViewModel 改造

```kotlin
@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    // ... 其他依赖不变
) : ViewModel() {

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

    init {
        viewModelScope.launch { chatRepository.loadOrInitCurrent() }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val convId = ensureConversation()  // 无 current 则创建
            // ... 保存 user message
            chatRepository.appendMessage(convId, userMsg)
            // ... AI 调用后保存 assistant message
            chatRepository.appendMessage(convId, assistantMsg)
        }
    }

    fun clearMessages() {
        // 改为:删除当前对话 + 清空 currentConversationId
        viewModelScope.launch {
            chatRepository.currentConversationId.first()?.let {
                chatRepository.deleteConversation(it)
            }
            chatRepository.setCurrentConversation(null)
        }
    }
}
```

#### 1.C.6 UI 变更(最小化)

- AiAssistantScreen 顶部加"历史对话"入口(Drawer 或 Sheet)
- 对话列表项:title + updatedAt + messageCount,点击切换
- "新建对话"按钮:调 `setCurrentConversation(null)` + 清空 UI
- **不做**复杂 UI(搜索/重命名/收藏),留后续迭代

#### 1.C.7 验证

- `ChatRepositoryImplTest`:创建/追加/删除/恢复
- `MigrationTest`(Room schema):v4→v5 数据迁移正确性
- 集成测试:进程被杀重启,历史完整恢复
- 现有 `AiAssistantViewModelTest` 适配(注入 FakeChatRepository)

#### 1.C.8 风险与决策

| 风险 | 决策 |
|------|------|
| 旧表有数据但无 Repository 引用,可能从未写入 | Migration 保留迁移逻辑但预期 0 行 |
| `references: List<RagReference>` 序列化 | 用 kotlinx.serialization JSON,字段 nullable |
| 存量用户升级后 currentConversationId=null | `loadOrInitCurrent()` 返回 null,UI 空白,发首条消息时自动创建 |
| DB version 4→5 | 与 1.B 的 v3→v4 链接,Migration_4_5 纯 DDL+DML,幂等 |

**工作量预估**:8-12 文件改动(Entity×2 + DAO×2 + Migration + Repository + ViewModel + DI + Test×3),建议单独一个 commit。

### 1.D P0-E3 进程被杀状态恢复(大,出方案 — 实际范围远小于预估)

**问题**:全项目 0 处 `rememberSaveable` / 0 处 `onSaveInstanceState`(NF-U1 已确认)。

**现状调研**(2026-07-15,1.L 修复后重新评估):

#### 1.D.1 ViewModel SavedStateHandle — ✅ 已完成(1.L 第六批)

| ViewModel | SavedStateHandle | 持久化字段 | 状态 |
|-----------|-----------------|-----------|------|
| CardsViewModel | ✅ | isFlipped, currentIndex | 1.L 已修 |
| QuizViewModel | ✅ | selectedYear, expandedQuestionIds | 1.L 已修 |
| KnowledgeViewModel | ✅ | selectedCategory | 1.L 已修 |
| KnowledgePointDetailViewModel | ✅ | pointId(导航参数) | 原有 |
| ThemeViewModel | ❌ 不需要 | 主题配置从 DataStore 读取,已持久化 | - |
| GraphViewModel | ❌ 不需要 | 纯 combine repository Flow,无可变 UI 状态 | - |
| AiAssistantViewModel | ❌ 由 1.C 覆盖 | 消息将持久化到 DB | 1.C 处理 |
| ApiConfigViewModel | ❌ 不需要 | editingId 已用 MutableStateFlow(1.Q),表单状态瞬时 | - |

**结论**:ViewModel 层 SavedStateHandle 已全部覆盖,无需额外工作。

#### 1.D.2 Composable `rememberSaveable` — 仅 3 处(非预估的 30+)

实际 `grep` 结果:`feature/` 下仅 3 处 `remember { mutableStateOf }` / `rememberScrollState` / `rememberLazyListState`:

| 文件 | 行 | 当前代码 | 改造 | 优先级 |
|------|----|---------|------|--------|
| ApiConfigScreen.kt | 84 | `var deletingConfig by remember { mutableStateOf<ApiConfigEntity?>(null) }` | 改 `rememberSaveable` + 自定义 Saver(只存 id:String?) | P2(删除确认弹窗,丢失影响小) |
| AiAssistantScreen.kt | 78 | `val listState = rememberLazyListState()` | 改 `rememberSaveable(saver = LazyListState.Saver)` | P3(1.C 持久化消息后,滚动位置次要) |
| KnowledgePointDetailScreen.kt | 122 | `val scrollState = rememberScrollState()`(在 Crossfade 内) | 提到 Crossfade 外 + `rememberSaveable` | P2(详情页滚动位置) |

`SnackbarHostState` × 3 处(GraphScreen/ApiConfigScreen/AiAssistantScreen):瞬时反馈,无需持久化。

#### 1.D.3 Manifest configChanges — ✅ 已完成(1.L)

`AndroidManifest.xml:27` 已设 `android:configChanges="orientation|screenSize|keyboardHidden|screenLayout"`。

#### 1.D.4 实施方案

```kotlin
// ApiConfigScreen.kt — 删除确认状态(只存 id,重建时从 uiState 查找)
val saver = remember {
    Saver<ApiConfigEntity?, String?>(
        save = { it?.id },
        restore = { id -> id } // 简化:恢复后需从 uiState 重新匹配,或直接关闭弹窗
    )
}
var deletingConfigId by rememberSaveable(saver = Saver(...)) { mutableStateOf<String?>(null) }

// AiAssistantScreen.kt — 滚动位置
val listState = rememberLazyListState()
// LazyListState 已自带 Saver,但 rememberLazyListState 不用 rememberSaveable
// 改为:val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

// KnowledgePointDetailScreen.kt — 滚动位置提到 Crossfade 外
val scrollState = rememberSaveable { ScrollState(0) }
// Crossfade { ... scrollState ... }  ← scrollState 定义在 Crossfade 外层
```

#### 1.D.5 风险与决策

| 风险 | 决策 |
|------|------|
| `ApiConfigEntity` 不可直接 rememberSaveable(非 Bundle 友好) | 只存 id:String?,恢复后从 uiState 查找或关闭弹窗 |
| `LazyListState.Saver` API 版本兼容 | Compose 1.5+ 已稳定,当前 BOM 2025.12.00 支持 |
| 滚动位置恢复在 1.C 消息加载后可能跳变 | 1.C 实现后统一验证 |

**工作量预估**:3 文件小改(~30 行),可与 1.C 实现一起 commit,或单独小 commit。

**结论**:1.D 实际范围远小于原预估(30+ → 3 处),因 1.L 已覆盖 ViewModel 层。剩余 Composable 层 3 处 `rememberSaveable` 为 P2-P3 优先级,非阻塞。

### 1.E ⚠ 新 P0-F1 FSRS 公式版本定位决策(最高优先级,出决策)

**问题**(NF-F1/F2/F3):

| 公式 | 当前代码 | FSRS-4/5 | FSRS-6 |
|------|---------|----------|--------|
| `nextRecallStability` 基础项 | `exp(w[8])` | ✓ 一致 | ❌ 应为 `w[8]`(直接用) |
| `retrievability` | `(1+t/(9S))^(-1)` decay=-1 | ✓ 一致 | ❌ 应为 `(1+19t/(81S))^(-0.5)` decay=-0.5 |
| `nextInterval` | `9*S*(1/R-1)` | ✓ 一致 | ❌ 应为 `S*(1/R^(1/decay) - 1)/factor` |
| `nextForgetStability` 权重槽位 | w[11]-w[14] | ✓ 一致 | ❌ 应为 w[15]-w[18](NF-F4) |
| `w[17]-w[20]` | 全 0,占位未用 | N/A | ❌ 应在 LEARNING/RELEARNING 短期记忆公式中使用 |

**影响**:
- `exp(w[8])` 让稳定性增长放大约 3.02 倍 → 间隔膨胀,用户复习频率过低
- `retrievability decay=-1` 衰减曲线形状不同 → R 值偏差传导到 stability 更新
- 当 `R_target=0.9` 时,nextInterval 数值恰好接近,但其他 retention 值下偏差明显

**决策选项**(用户决策):

**选项 A**:升级到完整 FSRS-6(改 4 处公式)
- 修 `nextRecallStability`:`exp(w[8])` → `w[8]`
- 修 `retrievability`:decay=-1 → decay=-0.5
- 修 `nextInterval`:配套 decay=-0.5
- 修 `nextForgetStability`:w[11-14] → w[15-18]
- 实现 w[17] 短期记忆项
- 风险:已发版用户的复习间隔会立即变化(可能变长或变短,需评估用户体验)

**选项 B**:文档化为"FSRS-5 公式 + FSRS-6 权重适配版"
- 不改代码,改类注释 + 设计文档明确标注
- 风险:与项目宣称"FSRS-6 自实现"矛盾,误导用户

**选项 C**:先实测对比两版本数值差异,再决策
- 跑 5 组典型用例(D=5, S=10, R=0.9/0.85/0.95, rating=AGAIN/GOOD/EASY)
- 手算 FSRS-4/5 vs FSRS-6 数值,看差异幅度
- 若差异 < 5%,选 B;若 > 10%,选 A

**推荐**:选项 C(先对比,再决策),3 个工作单元。

**验证**:`FsrsFormulaComparisonTest`,断言 5 组用例的数值差异。

### 1.F ⚠ 新 P0-V1 ThemeViewModel 紧急修复(立即可修,30 分钟)

**问题**(NF-V1):`ThemeViewModel.kt` L32/L36/L40/L44/L48 — 5 个 setter 全裸 `viewModelScope.launch`,无 try/catch。

**风险**:DataStore 写入抛 IOException → 异常冒泡到 Thread.uncaughtExceptionHandler → **App 崩溃**。ThemeViewModel 是 Hilt 全局单例,崩溃影响整个 App。

**修复**:

```kotlin
// 修改前(L32)
fun setColorMode(mode: ColorMode) {
    viewModelScope.launch {
        themeRepository.setColorMode(mode)
    }
}

// 修改后
fun setColorMode(mode: ColorMode) {
    viewModelScope.launch {
        try {
            themeRepository.setColorMode(mode)
        } catch (e: Exception) {
            // DataStore 写入失败,记录但不崩溃
            android.util.Log.e("ThemeViewModel", "setColorMode failed", e)
            // 可选:_errorMessage.value = "主题保存失败"
        }
    }
}
```

**5 处全改**:`setColorMode` / `setPaletteStyle` / `setDynamicColor` / `setAmoledMode` / `setHighContrast`(具体方法名核对 ThemeViewModel.kt)

**验证**:`ThemeViewModelTest` 加 5 个测试,模拟 DataStore 抛 IOException,断言 ViewModel 不崩溃。

### 1.G ⚠ 新 P0-A1 RecallChecker L3 触发逻辑修复(立即可修,15 分钟)

**问题**(NF-A1):`RecallChecker.kt:57-63` — `if/else` 两分支都 `emit(l2Result)`,从未调用 `checkL3Llm()`。

```kotlin
// 当前代码(bug)
QuestionType.ESSAY -> {
    val l2Result = checkL2Semantic(userAnswer, correctAnswer)
    if (l2Result.rating == RecallRating.HARD && l2Result.coverage in PARTIAL_CORRECT_RANGE) {
        emit(l2Result)  // ❌ 应改为 emit(checkL3Llm(userAnswer, correctAnswer))
    } else {
        emit(l2Result)
    }
}
```

**修复**:

```kotlin
QuestionType.ESSAY -> {
    val l2Result = checkL2Semantic(userAnswer, correctAnswer)
    if (l2Result.rating == RecallRating.HARD && l2Result.coverage in PARTIAL_CORRECT_RANGE) {
        // L2 判定"部分正确",触发 L3 LLM 评估
        emit(checkL3Llm(userAnswer, correctAnswer))
    } else {
        emit(l2Result)
    }
}
```

**额外修复 NF-A2**:L2 评分增加 GOOD 档:
```kotlin
// 修改前
when {
    similarity < 0.6f -> RecallRating.HARD
    similarity < 0.85f -> RecallRating.HARD  // 部分正确,触发 L3
    else -> RecallRating.EASY
}
// 修改后
when {
    similarity < 0.6f -> RecallRating.HARD
    similarity < 0.75f -> RecallRating.HARD  // 部分正确,触发 L3
    similarity < 0.85f -> RecallRating.GOOD   // 较好但不完美
    else -> RecallRating.EASY
}
```

**验证**:`RecallCheckerTest` 加 5 个测试覆盖 L3 触发路径。

### 1.H ⚠ 新 P0-D1 Migration_2_3 回填 reps + schema 升级(中等,1 个工作单元)

**问题**(NF-D1):`Migration_1_2.kt:17-28` 仅 `ALTER TABLE ADD COLUMN reps INTEGER NOT NULL DEFAULT 0`,未执行 `UPDATE memo_records SET reps = review_count WHERE reps = 0 AND review_count > 0`。

**影响**:v0.2.0 已发版用户升级到 v2 后,所有已有 `memo_records` 的 `reps = 0`,但 `review_count` 可能 > 0。FSRS 调度若依赖 `reps` 判断复习阶段(如新卡 vs 复习卡),会把老卡片误判为新卡,复习间隔重置。

**修复方案**:

1. 升级 `WenyanDatabase` version 从 2 到 3
2. 创建 `Migration_2_3`:
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 回填 reps 字段:用 review_count 初始化
        db.execSQL("UPDATE memo_records SET reps = review_count WHERE reps = 0 AND review_count > 0")
    }
}
```
3. `DatabaseModule.kt`:`addMigrations(MIGRATION_1_2, MIGRATION_2_3)`
4. **补全 schema 1.json**(NF-D6):从 v0.2.0 git tag 检出代码,运行 `:core:database:assembleDebug` 生成 1.json,提交到仓库
5. 生成 schema 3.json:`exportSchema = true` 已开启,升级后自动生成

**验证**:`MigrationTestHelper` 自动验证 1→2→3 路径(需 1.json + 2.json + 3.json 三个 schema)。

### 1.I ⚠ 新 P0-DB MemoRecordDao upsert 改 @Update + INSERT IGNORE(立即可修,1 小时)

**问题**(NF-D2):`MemoRecordDao.kt:17-18` 用 `@Insert(onConflict = REPLACE)`,REPLACE 在 SQLite 是 DELETE + INSERT,会触发 FK CASCADE + 丢 `history` 字段。

**修复方案**:

```kotlin
// 修改前
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsert(record: MemoRecordEntity)

// 修改后
@Update
suspend fun update(record: MemoRecordEntity)

@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertIgnore(record: MemoRecordEntity): Long  // 返回 rowId,-1 表示冲突

// Repository 层组合:
suspend fun upsert(record: MemoRecordEntity) {
    val rowId = dao.insertIgnore(record)
    if (rowId == -1L) {
        dao.update(record)  // 已存在,更新(保留 history 字段需先 read merge)
    }
}
```

**或者更优**:用 Room 2.5+ 的 `@Upsert`(但需评估是否真的原子)。**最稳**:写 `@Query("UPDATE memo_records SET state=:state, stability=:stability, ... WHERE point_id=:pointId")` 按字段更新,保留 history。

**验证**:`MemoRecordDaoTest`(instrumented),验证 upsert 不丢 history。

### 1.J ⚠ v3 P0-DS1 种子加载链路重构(中等,2 个工作单元)

**问题**(NF-DS1/DS2/DS3/DS6):种子数据加载链路有 4 个 P0 缺陷叠加:
1. 双 DataStore 实例(`wenyan_preferences` + `wenyan_seed_prefs`)违反单例原则
2. `isInitialized()` 无 IOException 兜底,异常冒泡到 Application
3. `markInitialized()` 写失败冒泡,种子可能重复导入
4. 种子导入用 REPLACE 策略,覆盖用户已建数据

**修复方案**:

1. **合并 DataStore**(NF-DS1):
   ```kotlin
   // 删除 SeedDataLoader.kt:63 的 Context.seedDataStore delegate
   // 把 KEY_SEED_INITIALIZED 移到主 DataStore
   // SeedDataLoader 注入 DataStore<Preferences> 而非 Context
   ```

2. **IO 兜底**(NF-DS2/DS3):
   ```kotlin
   private suspend fun isInitialized(): Boolean = try {
       dataStore.data
           .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
           .map { it[KEY_SEED_INITIALIZED] ?: false }
           .first()
   } catch (e: IOException) {
       Log.w(TAG, "isInitialized failed, assume false", e)
       false
   }

   private suspend fun markInitialized() {
       try {
           dataStore.edit { it[KEY_SEED_INITIALIZED] = true }
       } catch (e: IOException) {
           Log.w(TAG, "markInitialized failed, will retry next launch", e)
       }
   }
   ```

3. **IGNORE 策略**(NF-DS6):所有种子导入 DAO 从 `OnConflictStrategy.REPLACE` 改 `OnConflictStrategy.IGNORE`,已存在数据不覆盖。或导入前 `SELECT COUNT(*) WHERE source='seed'` 判断是否首次导入。

4. **超时兜底**(NF-L5):`applicationScope.launch { withTimeoutOrNull(30_000) { seedDataLoader.ensureSeedDataLoaded() } }`

5. **暴露状态**(NF-L6/S7):`SeedDataLoader` 暴露 `StateFlow<SeedState>`,UI 观察后显示 Loading 占位。

**验证**:`SeedDataLoaderTest`(instrumented 或 Robolectric),覆盖:首次加载、重复加载 idempotent、IO 异常降级、REPLACE→IGNORE 不覆盖。

### 1.K ⚠ v3 P0-N1 BottomBar 顶级路由过滤(立即可修,30 分钟)

**问题**(NF-N1):`WenyanApp.kt:49-63` 的 `ExpressiveScaffold` bottomBar 无条件填充 `WenyanNavigationBar`,push 子路由(knowledge_detail/settings/api_config)时底部导航栏不消失。AiAssistantScreen 已自带 InputBar 作为 bottomBar,与外层 NavigationBar 叠加冲突。

**修复**:

```kotlin
// WenyanApp.kt
val currentDestination = navController.currentBackStackEntryAsState().value?.destination
val showBottomBar = currentDestination?.route in TopLevelDestination.destinations.map { it.route }
// 或更精确:currentDestination?.hierarchy?.any { it.route in topLevelRoutes } == true

ExpressiveScaffold(
    bottomBar = {
        // AiAssistantScreen 自带 InputBar,不显示外层 NavigationBar
        if (showBottomBar && currentDestination?.route != ROUTE_AI_ASSISTANT) {
            WenyanNavigationBar(...)
        }
    }
) { ... }
```

**验证**:emulator 实测 push knowledge_detail 后底部导航栏消失;AiAssistantScreen 显示 InputBar 不与 NavigationBar 叠加。

### 1.L ⚠ v3 P0-L1+L2+L3 SavedStateHandle 全 ViewModel 注入(中等,3 个工作单元)

**问题**(NF-L1/L2/L3):7 个 ViewModel 中 6 个未用 SavedStateHandle 持久化交互状态(ApiConfigViewModel/CardsViewModel/QuizViewModel/AiAssistantViewModel/GraphViewModel/KnowledgeViewModel),进程被杀即丢。仅 KnowledgePointDetailViewModel 使用但仅一次性读取(NF-L7)。

**修复方案**:

1. **所有 ViewModel 构造函数加 `savedStateHandle: SavedStateHandle`**(Hilt 自动注入):
   ```kotlin
   @HiltViewModel
   class CardsViewModel @Inject constructor(
       private val savedStateHandle: SavedStateHandle,  // 新增
       private val cardRepository: CardRepository,
       // ...
   ) : ViewModel() { ... }
   ```

2. **关键状态用 `getStateFlow`**:
   ```kotlin
   // CardsViewModel
   private val _isFlipped = MutableStateFlow(false)
   val isFlipped = _isFlipped.asStateFlow()
   init {
       _isFlipped.value = savedStateHandle["isFlipped"] ?: false
       viewModelScope.launch {
           isFlipped.collect { savedStateHandle["isFlipped"] = it }
       }
   }
   ```
   或更简洁:`val isFlipped = savedStateHandle.getStateFlow("isFlipped", false)`

3. **改造清单**(逐 ViewModel):
   - ApiConfigViewModel:`editingId` → `savedStateHandle.getStateFlow("editingId", null)`
   - CardsViewModel:`isFlipped` + `currentIndex` → 两个 StateFlow
   - QuizViewModel:`selectedYear` + `expandedQuestionIds`(用 `ArrayList<String>.toSet()` Saver)→ 两个 StateFlow
   - AiAssistantViewModel:`roteWarningDismissed` → StateFlow(消息持久化见 P0-E2)
   - GraphViewModel:`selectedNode` → StateFlow
   - KnowledgeViewModel:`selectedCategory` → StateFlow
   - KnowledgePointDetailViewModel:`pointId` 从 val 改 `getStateFlow`(NF-L7)

4. **MainActivity 加 `android:configChanges="orientation|screenSize|keyboardHidden|screenLayout"`**(NF-M6),避免旋转 recreate。

**验证**:`SavedStateHandleTest`,模拟进程被杀重启,断言 UI 状态完整恢复。

### 1.M ⚠ v3 P0-S3+S2+S1 三大基础工具补齐(中等,1 个工作单元)

**问题**(NF-S3/S2/S1):debug 构建缺三大基础调试工具:
- 无 Splash Screen API,冷启动白屏 ~200ms
- 无 LeakCanary,Compose+Hilt+ViewModel 链路泄漏无自动检测
- 无 StrictMode,主线程 IO/网络违规无检测

**修复方案**:

1. **Splash Screen API**(NF-S3):
   ```toml
   # libs.versions.toml
   androidx-core-splashscreen = "1.0.1"
   ```
   ```kotlin
   // app/build.gradle.kts
   implementation(libs.androidx.core.splashscreen)
   ```
   ```xml
   <!-- themes.xml 加 Theme.Wenyan.Splash -->
   <style name="Theme.Wenyan.Splash" parent="Theme.SplashScreen">
       <item name="windowSplashScreenBackground">@color/wenyan_window_background</item>
       <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
       <item name="postSplashScreenTheme">@style/Theme.Wenyan</item>
   </style>
   ```
   ```kotlin
   // MainActivity.onCreate
   installSplashScreen()
   // 配合 1.J 的 SeedState:installSplashScreen().setKeepOnScreenCondition { seedState == Loading }
   ```

2. **LeakCanary**(NF-S2):
   ```kotlin
   // app/build.gradle.kts
   debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
   ```
   自动初始化,无需代码改动。

3. **StrictMode**(NF-S1):
   ```kotlin
   // WenyanApplication.onCreate
   if (BuildConfig.DEBUG) {
       StrictMode.setThreadPolicy(
           StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyFlashScreen().build()
       )
       StrictMode.setVmPolicy(
           StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
       )
   }
   super.onCreate()
   ```

**验证**:emulator 冷启动无白屏;Logcat 可见 LeakCanary 日志;StrictMode 触发主线程 IO 警告(若有)。

### 1.N ⚠ v3 P0-M8 res/xml 目录创建(立即可修,1 小时)

**问题**(NF-M8/M1/M2/M4/C3/C5):`app/src/main/res/xml/` 目录完全不存在,Android 12+ 备份规则、网络安全、per-app language 全无配置。

**修复方案**:

1. 创建 `app/src/main/res/xml/` 目录
2. 创建 4 个 xml 文件:

   **`backup_rules.xml`**(Android 11 及以下):
   ```xml
   <full-backup-content>
       <exclude domain="sharedpref" path="wenyan_preferences.preferences_pb" />
       <exclude domain="database" path="wenyan.db" />
   </full-backup-content>
   ```

   **`data_extraction_rules.xml`**(Android 12+):
   ```xml
   <data-extraction-rules>
       <cloud-backup disableIfNoEncryptionCapabilities="true" />
       <device-transfer>
           <include domain="sharedpref" path="." />
       </device-transfer>
   </data-extraction-rules>
   ```

   **`network_security_config.xml`**:
   ```xml
   <network-security-config>
       <base-config cleartextTrafficPermitted="false">
           <trust-anchors>
               <certificates src="system" />
           </trust-anchors>
       </base-config>
       <!-- 预留:未来加 <pin-set> for DeepSeek/通义/智谱/月之暗面 -->
   </network-security-config>
   ```

   **`locales_config.xml`**:
   ```xml
   <locale-config xmlns:android="http://schemas.android.com/apk/res/android">
       <locale android:name="zh-CN" />
       <!-- 预留 <locale android:name="en-US" /> -->
   </locale-config>
   ```

3. **AndroidManifest 加属性**:
   ```xml
   <application
       android:fullBackupContent="@xml/backup_rules"
       android:dataExtractionRules="@xml/data_extraction_rules"
       android:networkSecurityConfig="@xml/network_security_config"
       android:enableOnBackInvokedCallback="true"
       android:localeConfig="@xml/locales_config"
       android:label="@string/app_name"
       ...>
   ```

4. **ic_launcher.xml 加 monochrome**(NF-C5):
   ```xml
   <adaptive-icon>
       <background android:drawable="@drawable/ic_launcher_background" />
       <foreground android:drawable="@drawable/ic_launcher_foreground" />
       <monochrome android:drawable="@drawable/ic_launcher_foreground" />
   </adaptive-icon>
   ```

**验证**:emulator Android 13+ 设备 themed icon 生效;备份规则生效;预测返回手势动画显示。

### 1.O ⚠ v3 P0-E1+E2 解密异常 runCatching 包装(立即可修,1 小时)

**问题**(NF-E1/E2):`ApiConfigRepository.kt` 4 处 `map { it.decrypted() }` 未 try/catch,AndroidKeyStore 失效或单项密文损坏时整个 Flow 永久 failed,所有 AI 配置不可用。

**修复方案**:

```kotlin
// ApiConfigRepository.kt

// 修改前(L36/50/56/72)
fun observeAll(): Flow<List<ApiConfig>> = apiConfigDao.observeAll().map { list ->
    list.map { it.decrypted() }
}

// 修改后
fun observeAll(): Flow<List<ApiConfig>> = apiConfigDao.observeAll().map { list ->
    list.mapNotNull { entity ->
        runCatching { entity.decrypted() }
            .onFailure { e -> Log.w(TAG, "decrypt failed for config ${entity.id}", e) }
            .getOrNull()
    }
}.catch { e ->
    Log.e(TAG, "observeAll failed", e)
    emit(emptyList())
}

// 单条查询同理(L72):
suspend fun getCurrent(): ApiConfig? = runCatching {
    apiConfigDao.getCurrent()?.decrypted()
}.onFailure { e -> Log.w(TAG, "getCurrent decrypt failed", e) }.getOrNull()
```

**额外修复 NF-E8**:`ApiKeyCryptoImpl.decrypt` 解密失败返回 `""` 改为返回 `null` 或抛 `DecryptionException`:
```kotlin
fun decrypt(data: ByteArray): String? {
    if (data.size < GCM_IV_SIZE + 1) return null
    // ...
    return try { String(cipher.doFinal(...)) } catch (e: GeneralSecurityException) { null }
}
```

**验证**:`ApiConfigRepositoryDecryptTest`,模拟 KeyStore 失效 + 单项密文损坏,断言 Flow 不死、其他配置仍可用。

### 1.P ⚠ v3 P0-T1 studyText!! 改 orEmpty()(立即可修,5 分钟)

**问题**(NF-T1):`KnowledgePointDetailScreen.kt:259` `content = point.studyText!!` 双叹号,数据不一致时 NPE 崩溃。

**修复**:

```kotlin
// 修改前(L259)
content = point.studyText!!,

// 修改后
content = point.studyText.orEmpty(),
```

或更安全:
```kotlin
content = point.studyText ?: return@PerspectiveCard,
```

**额外修复 NF-T2/T3/T6**(全项目 !! 清理):
- `ExamCodeResolver.kt:121` `(record.validToYear == null || year <= record.validToYear!!)` → `record.validToYear?.let { year <= it } ?: true`
- `SchedulingRepository.kt:56` `?: TIER_CONFIGS[TIER_FRAMEWORK]!!` → `?: TIER_CONFIGS.getValue(MemoryTier.TIER_FRAMEWORK)`
- `AntiRoteMemorization.kt:91, 111` `log.rating.uppercase()` → `log.rating?.uppercase().orEmpty()`

**验证**:`grep "!!" feature/ core/ --include="*.kt" | grep -v test`,生产代码 !! 数量从 ~5 降至 0。

### 1.Q ⚠ v3 P0-C1 ApiConfigViewModel.editingId 改 MutableStateFlow(立即可修,30 分钟)

**问题**(NF-C1):`ApiConfigViewModel.kt:48` `private var editingId: String? = null` 是裸 var,被 UI 线程方法与 viewModelScope.launch 协程并发读写,跨调度器时 lost-update。

**修复**:

```kotlin
// 修改前(L48)
private var editingId: String? = null

// 修改后
private val _editingId = MutableStateFlow<String?>(null)
private val editingId: String? get() = _editingId.value

fun showAddForm() {
    _editingId.value = null
    // ...
}

fun showEditForm(config: ApiConfig) {
    _editingId.value = config.id
    // ...
}
```

**额外修复 NF-C3**(全项目 MutableStateFlow.value 复合操作改 .update):
- `ApiConfigViewModel` L67/74/83/88/90/118/122/126/130/145/197 全部 `.value = _xxx.value.copy(...)` 改 `.update { it.copy(...) }`

**验证**:`grep "var " feature/aiassistant/ --include="*.kt"`,ViewModel 内 var 数量降至 0。

### 1.R ⚠ v3 P0-BB3 PrerequisiteChecker 加环检测(中等,1 个工作单元)

**问题**(NF-BB3):`PrerequisiteChecker.kt:41-52` `checkPrerequisites` 调 `getPrerequisites`,若图存在环 A→B→A,可能无限递归或返回重复节点,无环检测、无 visited set 保护。App 卡死或 StackOverflow。

**修复方案**:

```kotlin
// PrerequisiteChecker.kt

// 修改前
suspend fun checkPrerequisites(nodeId: String): PrerequisiteResult {
    val prerequisites = getPrerequisites(nodeId)
    // ...
}

// 修改后
suspend fun checkPrerequisites(nodeId: String): PrerequisiteResult {
    val visited = mutableSetOf<String>()
    val prerequisites = getPrerequisitesSafe(nodeId, visited)
    if (visited.size > MAX_GRAPH_SIZE) {
        return PrerequisiteResult.CycleDetected
    }
    // ...
}

private suspend fun getPrerequisitesSafe(
    nodeId: String,
    visited: MutableSet<String>
): List<String> {
    if (nodeId in visited) return emptyList()  // 环检测
    visited.add(nodeId)
    val direct = graphRepository.getDirectPrerequisites(nodeId).first()
    return direct + direct.flatMap { getPrerequisitesSafe(it, visited) }
}

companion object {
    private const val MAX_GRAPH_SIZE = 1000  // 防御性上限
}
```

**额外修复 NF-BB9**:`Rating.fromValue` 越界抛 NoSuchElementException:
```kotlin
// FsrsModels.kt
fun fromValue(v: Int): Rating = entries.firstOrNull { it.value == v } ?: Rating.GOOD
```

**验证**:`PrerequisiteCheckerCycleTest`,构造 A→B→A 环,断言返回 `CycleDetected` 而非 StackOverflow。

### 1.S ⚠ v3 P0-EE1 MemoRecordMapper JSON 异常不再静默重置(立即可修,30 分钟)

**问题**(NF-EE1):`MemoRecordMapper.kt:130-145` `appendReviewLog` 当 `existingJson` 既非空也非合法 JSON 数组时,L143 直接用 `"[$entry]"` 重置,**丢弃全部历史复习日志**,且无日志输出。

**修复方案**:

```kotlin
// MemoRecordMapper.kt

// 修改前(L143)
else -> "[$entry]"  // 格式异常时重置

// 修改后
else -> {
    Log.w("MemoRecordMapper", "history JSON corrupted, preserving new entry only. original: $trimmed")
    // 保留原 JSON 作为备份(可选):存到 history_corrupted 字段
    "[$entry]"
}
```

**长期方案**(NF-PP4):废弃 `memo_records.history` JSON 字段,统一用 `review_logs` 表:
1. Migration_3_4:`ALTER TABLE memo_records DROP COLUMN history`(SQLite 不支持 DROP,需重建表)
2. 所有 `appendReviewLog` 调用改为 `reviewLogDao.insert(ReviewLogEntity(...))`
3. 查询历史改 `reviewLogDao.observeByPoint(pointId)`

**额外修复 NF-T5**:用 kotlinx.serialization 替代手动 StringBuilder:
```kotlin
@Serializable
data class ReviewLogDto(val state: String, val rating: String, val timestamp: Long, val elapsedDays: Int)

fun appendReviewLog(existingJson: String, log: ReviewLog): String {
    val list = runCatching { Json.decodeFromString<List<ReviewLogDto>>(existingJson) }
        .getOrDefault(emptyList())
        .toMutableList()
    list.add(log.toDto())
    return Json.encodeToString(list)
}
```

**验证**:`MemoRecordMapperTest`,模拟 JSON 损坏,断言不丢失新 entry + 日志输出。

---

## Phase 2 — 新维度深度审计(扩展为 15 维度,必做,P0)

> **修订**:
> - v2:基于预扫描发现,2.A-2.H 每个维度补充扩展检查项。
> - v3:新增 2.I-2.O 七个维度(Navigation/Lifecycle + Hilt/Startup + 资源/线程/类型/错误 + Compose/A11y/M3 + 业务/DataStore/持久化 + Manifest),与 §2.8-2.13 汇总表对应。其中 §2.10 拆为 2.K(代码级:资源/线程/类型/模块)+ 2.L(架构级:错误类型/日志/Snackbar)。

### 2.A 业务逻辑正确性深度审计(扩展)

#### 2.A.1 FSRS-6 完整公式对比(扩展)

**必查文件**:`core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt`

**对照基准**:[open-spaced-repetition/fsrs4anki](https://github.com/open-spaced-repetition/fsrs4anki) fsrs-6.x.py

**检查项**(对照官方):

- [ ] `initDifficulty` 4 档映射:AGAIN=w[0]+1, HARD=w[1]+1, GOOD=w[2]+1, EASY=w[3]+1(注意 +1)— subagent 已核对 ✓
- [ ] `initStability` 4 档映射:AGAIN=w[4], HARD=w[5], GOOD=w[6], EASY=w[7] — subagent 已核对 ✓
- [ ] `nextDifficulty`:`D' = w[6]*(D - w[7]*(r-3)) + (1-w[6])*w[7-1]` — subagent 已核对 ✓(F-01 修后)
- [ ] **NF-F1**:`nextRecallStability` GOOD 分支:`exp(w[8]) * (11-D) * S^(-w[9]) * (exp((1-R)*w[10]) - 1)` — **应为 `w[8]` 非 `exp(w[8])`**
- [ ] **NF-F1 扩展**:`nextRecallStability` 是否含 FSRS-6 的 `* (1 - w[17]*(1-R))` 短期记忆项 — **缺失**
- [ ] `nextRecallStability` AGAIN 分支:`w[11] * D^(-w[12]) * ((S+1)^w[13] - 1) * exp(w[14]*(1-R))` — 与 FSRS-4/5 一致,与 FSRS-6 是否一致需对照
- [ ] **NF-F4**:`nextForgetStability`:`w[15] * D^(-w[16]) * ((S+1)^w[17] - 1) * exp(w[18]*(1-R))` — 当前用 w[11-14],FSRS-6 应用 w[15-18]
- [ ] **NF-F2**:`retrievability`:`(1 + 19*t/(81*S))^(-0.5)` — 当前 decay=-1
- [ ] **NF-F3**:`nextInterval`:`min(max(round(interval * fuzz), 1), maximumInterval)` — 配套 decay
- [ ] `applyFuzz`:`interval ± max(1, 0.05*interval)` — 15-20 天区间偏小(NF-F6 已知)
- [ ] w[17]-w[20] 是否真的未使用(F-04)— subagent 确认未使用

**输出**:`fsrs-formula-diff.md`,每个公式 ① 代码 ② 官方 ③ 一致/不一致 ④ 数值影响 ⑤ 修复方案(对应 Phase 1.E)。

#### 2.A.2 知识图谱业务逻辑(扩展)

**必查文件**:
- `core/data/src/main/java/com/wenyan/app/core/data/graph/WeakSubgraphDetector.kt`
- `core/data/src/main/java/com/wenyan/app/core/data/graph/PrerequisiteChecker.kt`
- `core/data/src/main/java/com/wenyan/app/core/data/graph/InterferenceWarner.kt`

**检查项**:
- [ ] `WeakSubgraphDetector`:弱子图检测算法是否正确?对比图论标准定义
- [ ] `PrerequisiteChecker`:拓扑排序 + 环检测是否处理了 DAG 中的环?
- [ ] `InterferenceWarner`:相似度计算(余弦/Jaccard?)是否正确,阈值是否合理
- [ ] 3 个组件的测试是否真的覆盖业务正确性(不只是不抛异常)
- [ ] **新**:3 个测试全用 `runBlocking` 而非 `runTest`(NF-V 已记录),改 runTest

#### 2.A.3 AI 服务业务逻辑(扩展)

**必查文件**:
- `core/ai/src/main/java/com/wenyan/app/core/ai/RagEngine.kt`
- `core/ai/src/main/java/com/wenyan/app/core/ai/SocraticTutor.kt`
- `core/ai/src/main/java/com/wenyan/app/core/ai/recall/RecallChecker.kt`
- `core/ai/src/main/java/com/wenyan/app/core/ai/recall/AntiRoteMemorization.kt`

**检查项**:
- [ ] `RagEngine`:检索 → rerank → 拼接 prompt 流程是否正确,topK 参数是否生效
- [ ] **新**:`RagEngine` 无相似度排序,仅 SQLite LIKE + ORDER BY updated_at(NF-A1 相关)
- [ ] **新**:LIKE 通配符未转义(P1-D6 未修)
- [ ] `SocraticTutor`:苏格拉底提问生成逻辑,是否真的避免直接给答案
- [ ] **新**:`SocraticTutor` 三阶段串行无并发(P1-NEW-4 相关)
- [ ] `RecallChecker`:L3 触发逻辑(NF-A1 已修后,核对)
- [ ] **新**:`RecallChecker` L2 缺 GOOD 档(NF-A2 已修后,核对)
- [ ] `AntiRoteMemorization`:反死记硬背策略
- [ ] **新**:`AntiRoteMemorization` 仅检测不干预,Spec 要求"安排变体出题/反向提问"未实现
- [ ] **新**:`AntiRoteMemorization.getByPointOrderByCreatedDesc` 参数名 `cardId` 实际查 `point_id`,命名误导

#### 2.A.4 真题/知识点关联业务逻辑(扩展)

**必查文件**:
- `core/data/src/main/java/com/wenyan/app/core/data/cards/CardSplitter.kt`
- `core/fsrs/src/main/java/com/wenyan/app/core/fsrs/ContentTierMapper.kt`
- `core/fsrs/src/main/java/com/wenyan/app/core/fsrs/ExamCountdownManager.kt`

**检查项**:
- [ ] `CardSplitter`:把知识点拆成卡片的策略是否合理(每知识点 1 张?N 张?)
- [ ] `ContentTierMapper`:三层记忆(粗/中/细)映射规则是否符合教学法 — subagent 已核对 ✓
- [ ] **新 NF-F7**:`ExamCountdownManager.getExamDate` 考研日期规则"12月倒数第二个周六"与实际"最后一个完整周末"可能有 1 周偏差
  - 2026 年为例:代码返回 12-19,实际可能应为 12-26
  - **需用户确认南师大现当代文学考研(050106)的实际日期规则**
- [ ] **新 NF-F8**:`getTransitionFactor` 注释"每天调整10%卡片",实际是全局保持率线性插值,语义不符
- [ ] **新**:`TierFsrsConfig.minInterval=1` 死字段(NF-F5),FsrsWrapper 无对应参数

---

### 2.B Room SQL + 数据模型完整性审计(扩展)

#### 2.B.1 Entity 字段与关系图(扩展)

**必查文件**:`core/database/src/main/java/com/wenyan/app/core/database/entity/*.kt`(全部 19 个 Entity)

**检查项**:
- [ ] 列出所有 Entity,画 ER 图(mermaid)— subagent 已绘制 ✓
- [ ] **新**:核对 subagent 报告中的 ER 图完整性
- [ ] **新 NF-D5**:`ApiConfigEntity.api_key` 明文存储,核对业务层 `ApiConfigRepository.decrypted()` 是否真的解密
- [ ] ForeignKey 关系是否完整
- [ ] Index 是否覆盖所有查询字段
- [ ] **新**:`graph_edges` 无 `(source_id, target_id, type)` UNIQUE 约束(subagent P2-2)
- [ ] **新**:`api_configs.is_current` 无 UNIQUE 约束(subagent P2-3)
- [ ] TypeConverters 是否覆盖所有非基本类型字段
- [ ] **新 NF-D7**:`WenyanTypeConverters` 空字符串与空集合不可逆
- [ ] **新**:`writing_materials.tags` 是 String,其他表同类字段是 `List<String>`(subagent P2-5)

#### 2.B.2 DAO SQL 正确性(扩展)

**必查文件**:`core/database/src/main/java/com/wenyan/app/core/database/dao/*.kt`(全部 19 个 DAO)

**检查项**:
- [ ] 每个 `@Query` 的 SQL 是否符合 SQLite 方言 — subagent 已核对 ✓
- [ ] `ORDER BY` 子句(v0.4.2 已修 18 处,subagent 已核对完整 ✓)
- [ ] **新**:`LIMIT` 防护:`MemoRecordDao.observeDue` / `KnowledgePointDao.observeAll` / `ReviewLogDao.observeAll` 等无 LIMIT(subagent §3.3)
- [ ] `@Transaction` 标注(全项目 0 处,subagent 确认无单语句需事务)
- [ ] `@Insert(onConflict = REPLACE)` 的影响 — subagent 已识别 MemoRecordDao 风险(NF-D2)
- [ ] **新 NF-D2**:`MemoRecordDao.upsert` 改 @Update + INSERT IGNORE(Phase 1.I 修后核对)
- [ ] **新**:`WritingMaterialDao.observeByTag` LIKE 子串误匹配(subagent P2-4)
- [ ] **新**:`ReviewLogDao.getByPointIds` 无 ORDER BY(subagent P3-4)

#### 2.B.3 Migration 完整性(扩展)

**必查文件**:`Migration_1_2.kt` + `DatabaseModule.kt` + `WenyanDatabase.kt`

**检查项**:
- [ ] `version` 与 schema 一致 — subagent 已核对 ✓ (v2)
- [ ] **新 NF-D1**:Migration_1_2 未回填 reps(Phase 1.H 修后核对)
- [ ] **新 NF-D6**:`schemas/` 缺 1.json,无法跑 MigrationTestHelper(Phase 1.H 修后核对)
- [ ] `fallbackToDestructiveMigrationOnDowngrade` 是否真的只在 Downgrade 触发 — subagent 已核对 ✓
- [ ] **新**:`fallbackToDestructiveMigrationOnDowngrade` 在生产环境回滚也会丢数据(subagent P3-11),评估是否移除
- [ ] Export schema 选项 — subagent 已核对 ✓

#### 2.B.4 实际生成 SQL 验证

```bash
$JAVA_HOME/bin/java ... :app:connectedDebugAndroidTest
```

- [ ] `RoomDatabaseInstrumentedTest` 已存在,扩展它验证所有 DAO 实际 SQL
- [ ] **新**:用 `EXPLAIN QUERY PLAN` 检查索引使用

---

### 2.C 协程/Flow 深度审计(扩展)

**必查文件**:所有 Repository Impl + ViewModel(13 + 8 个,约 21 个文件)

**检查项**:

- [ ] **Flow 操作符链**:
  - `.map { }` 内是否调用 suspend(应该用 `.flatMapLatest` 或 `.map { transform() }` 后 `.flattenConcat`)
  - **新 NF-V2**:`.stateIn` 是否都有 `.catch {}` 兜底(7/8 无,Phase 1.F 修后核对)
  - `.combine` 两个源是否都在变化时触发
  - `.flowOn(IO)` 是否放在正确位置
  - `.distinctUntilChanged()` 是否缺失

- [ ] **协程作用域**:
  - 全项目 grep `GlobalScope`(必须 0)— subagent 已核对 ✓
  - 全项目 grep `CoroutineScope(`(只允许 DI 注入的)
  - 全项目 grep `viewModelScope.launch { }` 是否有 try/catch
  - **新 NF-V1**:ThemeViewModel 5 处全无 try/catch(Phase 1.F 修后核对)
  - 全项目 grep `liveData { }`(应该 0)

- [ ] **并发安全**:
  - `MutableStateFlow` 的 `update {}` vs `value =` 原子性
  - `Mutex` 是否用对
  - **新**:`AiAssistantViewModel` 无 Mutex 串行化(P2-1)

- [ ] **取消语义**:
  - `withTimeout` 是否有 fallback
  - `CancellationException` 是否被错误 catch
  - `Flow.first()` vs `Flow.single()`

- [ ] **stateIn 配置**:
  - `SharingStarted.WhileSubscribed(5000)` 是否统一
  - `initialValue` 是否合理

**验证**:
```bash
grep -rn "GlobalScope" --include="*.kt" core/ feature/ app/ | grep -v test
grep -rn "viewModelScope.launch" --include="*.kt" feature/ | grep -v "try\s*{"
grep -rn "\.stateIn(" --include="*.kt" feature/ core/
grep -rn "\.catch\s*{" --include="*.kt" feature/ core/
```

---

### 2.D Compose 重组性能实测(扩展,Compiler Metrics)

**步骤**:

1. **启用 Compose Compiler Metrics**:
   ```properties
   # gradle.properties 加(临时,验证后关闭)
   org.gradle.jvmargs=-Xmx4g
   ```
   ```bash
   -P plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.buildDir}/compose_reports
   -P plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${project.buildDir}/compose_metrics
   ```

2. **生成报告后审查**:
   - [ ] `*-classes.txt`:标记 `restartable skippable` 的 Composable
   - [ ] `*-composables.txt`:每个 Composable 的重组次数
   - [ ] `*-sets.txt`:不稳定参数集合
   - [ ] **新 NF-U5**:全项目零 `@Stable`/`@Immutable`,标记为 `unstable` 的参数 → 改 `@Stable` 或 `@Immutable`
   - [ ] **新 NF-U6**:全项目零 `derivedStateOf`,补关键位置

3. **重点审查 Composable**(基于 subagent 发现):
   - [ ] `WenyanApp.selectedTopLevelRoute`(线性扫描)— 应加 `derivedStateOf`
   - [ ] `GraphScreen.weakNodes` / `avgRetrievability`(O(n) 每次重组)— 应加 `derivedStateOf`
   - [ ] `KnowledgePointDetailScreen.subtitle`(字符串拼接)— 应加 `derivedStateOf`
   - [ ] `SettingsScreen.seedColors`(每次重组新建 List)— 提取为 top-level val
   - [ ] **新**:`KnowledgePointDetailScreen.scrollState` 在 Crossfade 内(NF-U6 相关,P1-6)
   - [ ] **新**:`KnowledgePointDetailScreen` 用 Column+verticalScroll(P1-5)— 改 LazyColumn
   - [ ] **新 NF-U7**:4 处 `AnimatedVisibility` 未用 WenyanMotion spec
   - [ ] **新 NF-U8**:`knowledge_detail → knowledge_detail` 无界 back stack

4. **emulator 实测重组**:
   - Layout Inspector → Compose 重组计数
   - 滚动列表观察是否卡顿

**输出**:`compose-recomposition-report.md`,列出所有不稳定 Composable + 修复方案。

---

### 2.E 资源与本地化审计(扩展)

**必查文件**:`app/src/main/res/**` 全部 + 9 个 Screen 的硬编码字符串

**检查项**:

- [ ] **strings.xml 完整性**:
  - **新 NF-U2**:当前只有 `app_name`,9 个 Screen 全硬编码(预估 50+ 字符串)
  - 列出所有硬编码字符串
  - 输出迁移清单(按优先级:用户可见 > 装饰性 > 内部)

- [ ] **values-night 缺失**:
  - **新 NF-U4**:加 `values-night/colors.xml` + `values-night/themes.xml`
  - 验证 dark mode 启动无白屏闪烁

- [ ] **themes.xml**:
  - **新 NF-U3**:当前是 `android:Theme.Material.Light.NoActionBar`(legacy)
  - 改 M3 `Theme.Material3.DayNight.NoActionBar`(需 `com.google.android.material:material` 依赖,已有)
  - splash 配置(API 23+ 用 `Theme.SplashScreen`)

- [ ] **drawables**:
  - `ic_launcher_foreground.xml` + `ic_launcher_background.xml` 是否合理
  - **新**:是否缺 adaptive icon 的 monochrome 属性(Android 13+ themed icon)
  - 是否有 vector drawable 而非 PNG

- [ ] **多语言支持**:
  - 留 i18n 钩子(`values-en/strings.xml` 占位)
  - `localeConfig`(Android 13+ per-app language)

- [ ] **dimens.xml 缺失**:
  - **新**:`CardRenderer.kt` 是硬编码 dp 重灾区(~20+ 处)
  - 创建 `dimens.xml` + `Spacing` token 完整迁移

- [ ] **colors.xml**:
  - 当前只有 launcher 颜色
  - 应清理(主题色用 M3 dynamic)

- [ ] **font 配置**:
  - 是否用系统字体 / 自定义字体

---

### 2.F 构建系统与 CI/CD 审计(扩展)

**必查文件**:
- `build.gradle.kts`(root)
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts` + 12 个模块 `build.gradle.kts`
- `.github/workflows/android.yml` / `release.yml` / `generate-keystore.yml`
- 13 个 .pro 文件

**检查项**:

- [ ] **gradle/libs.versions.toml**:
  - 所有依赖是否都在 [versions] / [libraries] / [bundles] / [plugins] 完整定义
  - **新 NF-B9**:`retrofit 2.9.0` + `jakewharton retrofit2-kotlinx-serialization-converter 1.0.0`(已停维),应升 retrofit 2.11+ 用官方 converter
  - **新 NF-B7**:`security-crypto 1.1.0-alpha06` 死依赖,应移除
  - **新 NF-B8**:`libs.versions.toml` 5 个 `wenyan-feature-*` 死声明,应删除

- [ ] **build.gradle.kts(root)**:
  - plugin alias 是否正确
  - repository 顺序(pluginManagement + dependencyResolutionManagement,AGENTS.md 已规定 Aliyun fallback)— subagent 已核对 ✓
  - **新 NF-B4**:`commonSettings` 或 convention plugin 是否值得引入(13 个模块有重复配置)
  - **新 NF-B5**:`core:designsystem → core:database` 反向依赖,应将 `ContentSource` 枚举下沉到 `:core:common`
  - **新 NF-B6**:`core:ai → core:database` DAO 直接依赖,应通过 Repository 接口反转

- [ ] **app/build.gradle.kts**:
  - `signingConfigs` 是否正确(release 用 secrets,debug 用默认)— subagent 已核对 ✓
  - **新 NF-B2**:`buildTypes.release.isMinifyEnabled=false` + `isShrinkResources` 未设,Release APK 无代码混淆保护
  - `compileSdk` / `targetSdk` / `minSdk` 合理性 — subagent 已核对 ✓ (35/35/26)
  - `testOptions` 配置(unit tests 用 Robolectric?)

- [ ] **CI workflow**:
  - `android.yml`:CI 跑 `testDebugUnitTest`(已修) — subagent 已核对 ✓
  - **新 NF-B1**:`release.yml` Line 63-70 "Verify keystore" 隐藏 bug(P0-4 未修)— 加 `if: ${{ secrets.KEYSTORE_BASE64 != '' }}`
  - **新 NF-B3**:`generate-keystore.yml` 密码硬编码 `Wenyan2026Release`,应改 `openssl rand -base64 24`
  - **新**:`android.yml` 无 `lint` 步骤,无 `timeout-minutes`
  - **新**:`release.yml` `testDebugUnitTest` 在 `assembleRelease` 之后,应先测再发
  - 所有 workflow 的 `runs-on` / `timeout-minutes` / `cache` 配置

- [ ] **ProGuard / R8**:
  - **新 NF-B2**:13 个 .pro 文件全空,Release 前必须预置规则:
    - Retrofit:keep interface + Generic
    - kotlinx.serialization:keep @Serializable
    - Hilt:keep @HiltAndroidApp + @Inject
    - Room:keep @Entity + @Dao
    - Compose:自动 keep
  - 输出每个模块的 `consumer-rules.pro` 模板

- [ ] **Gradle Wrapper**:
  - `gradle-wrapper.properties` 版本 — subagent 已核对 ✓ (8.14.4)
  - `gradlew` 可执行权限
  - `gradle/wrapper/gradle-wrapper.jar` 存在

- [ ] **local.properties**:
  - 不入仓库(检查 .gitignore)
  - `sdk.dir` 配置

- [ ] **build cache**:
  - `org.gradle.caching=true` — subagent 已核对 ✓
  - `org.gradle.configuration-cache=false` — subagent 已核对 ✓

---

### 2.G 安全深度审计(扩展)

**检查项**:

- [ ] **AndroidManifest**:
  - `allowBackup="false"`(已修)— subagent 已核对 ✓
  - **新**:`android:fullBackupContent` / `android:dataExtractionRules`(Android 12+ 备份规则)未声明
  - **新**:`android:networkSecurityConfig` 未声明
  - **新**:`android:usesCleartextTraffic="false"` 未显式声明
  - `android:exported` 对所有 component 标注
  - 权限声明是否最小化(`grep "uses-permission" AndroidManifest.xml`)— subagent 已核对 ✓ (INTERNET + ACCESS_NETWORK_STATE)

- [ ] **API Key 存储**:
  - **新 NF-D5**:`ApiConfigEntity.api_key` Room 默认明文,核对 `ApiConfigRepository.decrypted()` 是否真加密
  - `ApiKeyCryptoImpl` subagent 已核对 ✓ (AES-256-GCM + AndroidKeyStore)
  - **新 NF-B7**:`security-crypto alpha` 死依赖,移除
  - **新**:加密强度评估(AES-256-GCM ✓)
  - **新**:`ApiKeyCryptoImpl.context` 参数未使用(subagent P2-5),清理
  - **新**:`ApiKeyCryptoImpl` 解密校验过松 `+1` 应 `+16`(subagent P2-6)
  - key alias 命名(subagent 已核对 ✓ `wenyan_api_key_master`)

- [ ] **网络通信**:
  - HTTPS 强制(`usesCleartextTraffic="false"`)
  - **新**:Certificate Pinning 未配置(subagent P2-2),建议至少 pin DeepSeek/通义/智谱/月之暗面证书
  - OkHttp `Interceptor` 链:Auth / Logging / Retry — subagent 已核对 ✓
  - **新**:日志拦截器在 Release 必须关闭 BODY 级(P1-H2 已修,核对)— subagent 已核对 ✓
  - 超时配置 — subagent 已核对 ✓ (connect 30s / read 60s / write 30s)
  - **新**:无重试机制

- [ ] **Room 数据库**:
  - 是否加密(预估没,用户 FSRS 数据敏感性评估)
  - SQLCipher 评估(性能 vs 安全 trade-off)

- [ ] **Compose UI**:
  - 密码字段 `KeyboardType.Password` + `visualTransformation`
  - API Key 输入框是否掩码

- [ ] **第三方 SDK**:
  - 全依赖列表 `./gradlew dependencies` → 排查已知 tracker / ad SDK
  - Material3 / AndroidX 是否官方源

- [ ] **隐私政策**:
  - App 收集什么数据(API key / FSRS 记录 / 知识点)
  - 是否需要隐私政策界面

---

### 2.H 测试质量提升计划(扩展)

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

- [ ] **NF-V3 6 个 Repository 零测试**:
  - [ ] `CardRepositoryTest`(新)
  - [ ] `ExamRepositoryTest`(新)
  - [ ] `KnowledgeRepositoryTest`(新)
  - [ ] `ReviewRepositoryTest`(新)
  - [ ] **`SchedulingRepositoryTest`**(新,FSRS 调度核心入口,最高优先级)
  - [ ] `LlmConfigProviderImplTest`(新)

- [ ] **FSRS 测试补强**(P0-T1e/f):
  - 4 档评分 × 3 状态 = 12 组合全覆盖(subagent 已核对 ✓ 16 组合全覆盖)
  - **新 NF-F6**:三档参数(stabilityGrowthFactor/easyBonus/againPenalty)零测试,补完整
  - 边界值:S=0 / D=1 / D=10 / rating=4 / R=1.0 / R=0.0
  - **新**:`initStability` 仅测 AGAIN,补 HARD/GOOD/EASY + 单调递增断言
  - **新**:`initDifficulty` 仅测 GOOD,补 AGAIN/HARD/EASY + 单调递减断言
  - **新**:`nextForgetStability` 完全未测,补独立测试
  - **新**:`applyFuzz` 完全未测,改 Random 可注入后补测试
  - 数值精度:对比官方 fsrs-rs 计算结果(取 5 组典型用例手算)

- [ ] **Fake 实现质量**:
  - `FakeKnowledgePointDao` / `FakeReviewLogDao` / `FakeApiConfigDao` 是否实现真实
  - **新**:`FakeGraphRepository.insertNode/insertEdge` 空实现(subagent P2-8),补真实实现
  - 用 `MockK` 或 `Mockito-Kotlin` 替代部分手写 Fake

- [ ] **测试反模式**:
  - **新**:全项目 grep `runBlocking` → 改 `runTest`(3 个 graph 测试文件全用 runBlocking,subagent 已识别)
  - 全项目 grep `Thread.sleep` → 改 `advanceTimeBy`
  - 全项目 grep `assert(`(Kotlin assert)→ 改 `assertEquals`(第三轮 P0-T1c 修了 1 个,核对完整)

- [ ] **instrumented test**:
  - 当前只有 `RoomDatabaseInstrumentedTest` 1 个
  - **新 NF-D6**:补 `MigrationTestHelper` 自动验证 1→2→3 路径
  - 补 Hilt DI 图测试(`@HiltAndroidTest`)
  - 补 Compose UI 测试(`createAndroidComposeRule`)
  - 至少 9 个 Screen 各 1 个 smoke test

- [ ] **测试命名与组织**:
  - `MethodName_State_ExpectedBehavior` 风格
  - 每个 test class 有 `@Before` setup
  - 测试间无状态依赖

---

### 2.I Navigation 图 + Lifecycle 边界审计(v3 新增)

> **对应汇总表**:§2.8(10 项:6 P0 + 4 P1)。修复方案见 1.K / 1.L / 1.J。

**必查文件**(7 个):
- `app/src/main/java/com/wenyan/app/WenyanApp.kt`(BottomBar 显隐)
- `app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`(路由定义 + back stack)
- `app/src/main/java/com/wenyan/app/MainActivity.kt`(configChanges + repeatOnLifecycle)
- `app/src/main/AndroidManifest.xml`(activity configChanges)
- 8 个 ViewModel(全核对 SavedStateHandle 注入)
- `feature/knowledge/.../KnowledgePointDetailScreen.kt`(无界 back stack 触发点 L426)
- `feature/settings/.../ApiConfigScreen.kt`(editingId 使用处)

**检查项**:

- [ ] **NF-N1 BottomBar 顶级路由过滤**(P0):
  - grep `WenyanApp.kt` 中 `currentBackStackEntryAsState` + `NavHost.currentDestination?.route`
  - 核对 BottomBar 显示条件:仅 `Route.CARDS / Route.QUIZ / Route.KNOWLEDGE / Route.GRAPH / Route.SETTINGS` 5 个顶级路由显示
  - 子路由(knowledge_detail/{pointId}、api_config、ai_assistant)**必须隐藏** BottomBar
  - 特别核对 AiAssistantScreen InputBar 与 BottomBar 不重叠(底部 padding 计算)
  - **通过标准**:9 个 Screen 中,仅 5 个顶级显示 BottomBar,其余 4 个隐藏

- [ ] **NF-N2 路由字符串硬编码**(P1):
  - grep `WenyanNavHost.kt` 中 `composable("` 统计硬编码路由数
  - 评估迁移到 Navigation Compose Type-Safe(需 Kotlin Serialization,@Serializable Route sealed class)
  - **通过标准**:所有路由通过 @Serializable 对象定义,零字符串字面量

- [ ] **NF-N8-deep knowledge_detail 无界 back stack**(P0):
  - 读 `WenyanNavHost.kt:94-99` composable("knowledge_detail/{pointId}") 定义
  - 读 `KnowledgePointDetailScreen.kt:426` onRelatedClick 导航调用
  - 核对是否缺 `popUpTo` / `launchSingleTop` / `popUpTo(startDestination){saveState=true}`
  - **通过标准**:点关联知识点 N 次后,back 键只需 1 次回到列表(或单实例替换)

- [ ] **NF-L1/L2/L3 SavedStateHandle 全 ViewModel 注入**(P0,3 项合并):
  - grep 全 ViewModel `class.*ViewModel.*constructor` 检查 SavedStateHandle 参数
  - 7 个 ViewModel 必须注入:ApiConfigViewModel(editingId)、CardsViewModel(_isFlipped/_currentIndex)、QuizViewModel(_selectedYear/_expandedQuestionIds)、KnowledgePointDetailViewModel(pointId)、KnowledgeViewModel(筛选状态)、GraphViewModel(节点筛选)、ThemeViewModel(无状态可豁免)
  - 每个状态字段用 `savedStateHandle.getStateFlow<String?>("key", null)` 或 `savedStateHandle["key"] = value`
  - **通过标准**:旋转屏 + 进程被杀后,7 个 ViewModel 的交互状态全部恢复

- [ ] **NF-L5/L6 种子加载超时 + Splash 同步**(P0,2 项合并):
  - 读 `WenyanApplication.kt:39-48` applicationScope.launch
  - 核对是否用 `withTimeoutOrNull(30_000)` 包裹 seed 加载
  - 核对是否用 `installSplashScreen` + `setKeepOnScreenCondition { !seedLoaded }` 同步
  - **通过标准**:seed 文件损坏时 30 秒后 UI 正常显示(空列表 + 错误提示),不永久挂起;首次冷启动不显示空白 UI

- [ ] **NF-L7 KnowledgePointDetailViewModel pointId 观察**(P1):
  - 读 `KnowledgePointDetailViewModel.kt:35` pointId 读取方式
  - 改为 `savedStateHandle.getStateFlow<String>("pointId", "")` 然后 `flatMapLatest { repository.observe(it) }`
  - **通过标准**:同一 ViewModel 实例下 pointId 变化时 UI 自动更新

- [ ] **NF-M6 MainActivity configChanges**(P1):
  - 读 `AndroidManifest.xml:18-27` activity 声明
  - 加 `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|uiMode"`
  - **通过标准**:旋转屏不触发 Activity recreate(与 SavedStateHandle 双保险)

- [ ] **repeatOnLifecycle 使用审计**(P1,新):
  - grep 全 Screen `collectAsState` / `collectAsStateWithLifecycle`
  - 核对是否用 `lifecycle-runtime-compose` 的 `collectAsStateWithLifecycle`(默认 STARTED)
  - **通过标准**:零裸 `collectAsState`,全部用 `collectAsStateWithLifecycle`

- [ ] **BackHandler 拦截审计**(P2,新,对应 NF-UC7):
  - grep 全 Screen `BackHandler`
  - 核对 CardsScreen(翻转态)、QuizScreen(展开态)、AiAssistant(横幅态)是否拦截 back
  - **通过标准**:内部状态(翻转/展开/横幅)下按 back 先消费状态,再退出

**验证命令**:
```bash
# BottomBar 显示路由
grep -n "currentDestination\|route ==" app/src/main/java/com/wenyan/app/WenyanApp.kt
# 路由硬编码
grep -n 'composable("' app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt
# SavedStateHandle 注入
grep -rn "SavedStateHandle" --include="*.kt" feature/ app/
# configChanges
grep -n "configChanges" app/src/main/AndroidManifest.xml
# collectAsState 裸用
grep -rn "collectAsState()" --include="*.kt" feature/ app/
# BackHandler
grep -rn "BackHandler" --include="*.kt" feature/
```

**输出格式**:`audit/2I-navigation-lifecycle.md`,表格列:文件 / 行号 / 问题编号 / 严重度 / 当前代码 / 修复建议 / 状态。

**通过标准**:10 个检查项全部 ✅,9 个 Screen 的 BottomBar 显隐符合 M3 准则,7 个 ViewModel 状态可恢复,旋转屏不丢状态。

---

### 2.J Hilt DI 图 + 启动流程审计(v3 新增)

> **对应汇总表**:§2.9(8 项:2 P0 + 6 P1)。修复方案见 1.M / 1.J。

**必查文件**(8 个):
- `app/src/main/java/com/wenyan/app/WenyanApplication.kt`(@HiltAndroidApp + 启动)
- `app/src/main/java/com/wenyan/app/MainActivity.kt`(@AndroidEntryPoint)
- `core/data/src/main/java/.../di/DataStoreModule.kt`(DataStore 单例)
- `core/data/src/main/java/.../di/DataModule.kt`(Repository 绑定)
- `core/data/src/main/java/.../di/DatabaseModule.kt`(Room 绑定)
- `core/ai/src/main/java/.../di/AiModule.kt`(Retrofit + OkHttp 绑定)
- `app/build.gradle.kts`(LeakCanary / Splash 依赖)
- `app/src/main/res/values/themes.xml`(SplashScreen 主题)

**检查项**:

- [ ] **NF-H2 @HiltAndroidTest DI 图自动化验证**(P0):
  - grep 测试目录 `@HiltAndroidTest` 统计数量(当前应为 0)
  - 配置 `app/src/androidTest/.../HiltAndroidTestRule.kt`
  - 至少补 5 个 DI smoke test:Repository 注入 / Dao 注入 / DataStore 注入 / Retrofit 注入 / ViewModel 注入
  - **通过标准**:`./gradlew :app:connectedDebugAndroidTest` 至少 5 个 @HiltAndroidTest 全绿

- [ ] **NF-S3 Splash Screen API**(P0):
  - grep `installSplashScreen` 全项目(当前应为 0)
  - 读 `themes.xml` 核对是否用 `Theme.SplashScreen` 父主题
  - 加 `androidx.core:core-splashscreen:1.0.1` 依赖
  - 在 MainActivity `installSplashScreen()` + `setKeepOnScreenCondition { !seedLoaded }`
  - **通过标准**:冷启动无白屏,Splash 期间加载 seed,seed 完成后显示主 UI

- [ ] **NF-H8 双 DataStore 合并**(P1):
  - 读 `DataStoreModule.kt:24-30` + `SeedDataLoader.kt:63-65`
  - 合并为单一 `wenyan_preferences` DataStore,Key 加 `seed_` 前缀
  - **通过标准**:grep `preferencesDataStore` 全项目仅 1 处声明

- [ ] **NF-S1 StrictMode 配置**(P1):
  - 读 `WenyanApplication.kt` onCreate
  - 在 `BuildConfig.DEBUG` 分支加 `StrictMode.setThreadPolicy(...)` + `setVmPolicy(...)`
  - 检测主线程 IO / 网络 / SQL泄漏
  - **通过标准**:debug 构建主线程 IO 时 Logcat 输出 StrictMode 警告

- [ ] **NF-S2 LeakCanary 配置**(P1):
  - 读 `app/build.gradle.kts:82-138` 依赖
  - 加 `debugImplementation "com.squareup.leakcanary:leakcanary-android:2.14"`
  - **通过标准**:debug 构建自动启用 LeakCanary,泄漏时显示通知

- [ ] **NF-S7 CoroutineExceptionHandler 上报**(P1):
  - 读 `WenyanApplication.kt:35-37` CoroutineExceptionHandler
  - 加 Timber.e + 用户可见错误状态(如 DataStore errorStateFlow)
  - **通过标准**:seed 加载失败时 UI 显示"种子加载失败,请重试"按钮

- [ ] **NF-H1 Configuration.Provider 预留**(P2):
  - 评估是否实现 `Configuration.Provider` 为 WorkManager 预留
  - 当前无 WorkManager,可暂缓,文档化决策
  - **通过标准**:决策记录在审计报告,若实现则核对 HiltWorkerFactory

- [ ] **NF-H4 Repository 接口/实现分离**(P1):
  - 读 `DataModule.kt:35-41` 6 个 @Binds / @Provides
  - 核对 6 个 Repository 是否有 interface(CardRepository / ExamRepository / KnowledgeRepository / ReviewRepository / SchedulingRepository / LlmConfigProvider)
  - 为无接口的 Repository 抽取 interface,ViewModel 依赖 interface
  - **通过标准**:6 个 Repository 均有 interface,ViewModel @Inject interface 而非 class

- [ ] **启动同步审计**(新):
  - 读 `WenyanApplication.onCreate` 全部同步代码
  - 核对是否有主线程 IO(DataStore.readSync / Room 初始化)
  - **通过标准**:onCreate 仅同步初始化 Hilt + Timber + StrictMode,其余异步

**验证命令**:
```bash
grep -rn "@HiltAndroidTest" --include="*.kt" app/src/androidTest/
grep -rn "installSplashScreen" --include="*.kt" app/
grep -rn "preferencesDataStore" --include="*.kt" core/data/
grep -n "StrictMode" app/src/main/java/com/wenyan/app/WenyanApplication.kt
grep -n "leakcanary" app/build.gradle.kts
grep -rn "@Binds\|@Provides" --include="*.kt" core/data/src/main/java/.../di/
```

**输出格式**:`audit/2J-hilt-startup.md`,DI 图 Mermaid + 启动时序图 + 问题表格。

**通过标准**:8 项全 ✅,@HiltAndroidTest ≥ 5 个,Splash 无白屏,StrictMode + LeakCanary 启用,单 DataStore。

---

### 2.K 资源泄漏 + 线程安全 + 类型安全 + 错误处理审计(v3 新增)

> **对应汇总表**:§2.10(29 项:11 P0 + 18 P1)。修复方案见 1.O / 1.P / 1.Q / 1.R / 1.S。
> **注**:本维度与 2.L 错误处理有交叉(NF-E1-E8 / NF-E5 同时归属两维度),2.L 侧重 sealed AppError + Timber 规范,本维度侧重线程安全 + 类型安全 + 资源泄漏。

**必查文件**(15 个):
- `core/ai/src/main/java/.../AiServiceImpl.kt`(Retrofit 重建 + catch 过宽)
- `feature/settings/.../ApiConfigViewModel.kt`(editingId 线程安全)
- `core/data/src/main/java/.../repository/GraphRepositoryImpl.kt`(combine .catch + currentTimeMillis)
- `core/data/src/main/java/.../repository/ReviewRepository.kt`(combine .catch)
- `core/data/src/main/java/.../repository/ExamRepository.kt`(combine .catch)
- `core/ai/src/main/java/.../SocraticTutor.kt`(first 超时)
- `core/ai/src/main/java/.../RecallChecker.kt`(first 超时)
- `core/ai/src/main/java/.../InterferenceWarner.kt`(first 超时)
- `feature/knowledge/.../KnowledgePointDetailScreen.kt`(studyText!!)
- `core/common/src/main/java/.../ExamCodeResolver.kt`(双写法)
- `core/data/src/main/java/.../repository/SchedulingRepository.kt`(! 兜底 + currentTimeMillis)
- `core/data/src/main/java/.../MemoRecordMapper.kt`(Float↔Double + JSON + 异常重置)
- `core/ai/src/main/java/.../AntiRoteMemorization.kt`(uppercase NPE)
- `core/fsrs/src/main/java/.../FsrsWrapper.kt`(数组下标 + Random)
- `core/data/src/main/java/.../ApiKeyCryptoImpl.kt`(解密空字符串)

**检查项**:

- [ ] **NF-R1 Retrofit 单例化**(P0):
  - 读 `AiServiceImpl.kt:101-108` chat() 内是否每次 `Retrofit.Builder().build()`
  - 改为 @Inject 单例 Retrofit,Service 缓存 `by lazy { retrofit.create() }`
  - **通过标准**:grep `Retrofit.Builder` 在 AiServiceImpl 仅 0 处(移到 AiModule)

- [ ] **NF-C1/C2 线程安全规范建立**(P0,2 项合并):
  - 全项目 grep `@Volatile` / `AtomicReference` / `AtomicInt` / `Mutex` / `synchronized`(当前应为 0)
  - ApiConfigViewModel.editingId 改 `MutableStateFlow<Long?>(null)` + `.update {}`
  - 评估其他 var 字段(全 ViewModel grep `var ` 字段)是否需改 StateFlow
  - 建立《线程安全规范》文档:ViewModel 状态用 StateFlow,Repository 缓存用 AtomicReference,共享可变用 Mutex
  - **通过标准**:0 处裸 var 共享可变状态,规范文档存在

- [ ] **NF-C4 Repository combine .catch**(P1):
  - grep `combine` 在 Repository 层统计数量
  - 每个 combine 链末尾加 `.catch { Timber.e(it); emit(emptyList()) }`
  - **通过标准**:0 处 combine 无 .catch

- [ ] **NF-C5 first 超时**(P1):
  - grep `\.first()` 在 AI 模块统计(应 ≥ 7 处)
  - 每个 first 改 `firstOrNull()` + `withTimeoutOrNull(30_000)`
  - **通过标准**:0 处裸 first 在 AI 模块

- [ ] **NF-T1 studyText!! 清理**(P0):
  - 全项目 grep `!!` 统计数量
  - KnowledgePointDetailScreen L259 `point.studyText!!` 改 `point.studyText.orEmpty()`
  - 评估其他 `!!` 是否可改 `?: ""` / `requireNotNull` / `checkNotNull`
  - **通过标准**:`!!` 数量降至必要场景(< 5 处,如 FsrsWrapper 内部不变量)

- [ ] **NF-T2/T3/T7/T8 类型安全**(P1,4 项合并):
  - ExamCodeResolver 统一 ?-let 写法
  - SchedulingRepository `TIER_CONFIGS[TIER_FRAMEWORK]!!` 改 `requireNotNull(TIER_CONFIGS[TIER_FRAMEWORK]) { "..." }`
  - FsrsWrapper `w[rating.value - 1]` 改 `rating.index`(enum 加 val index)
  - FsrsWrapper applyFuzz 改可注入 `Random`(构造参数默认 `Random.Default`)
  - **通过标准**:4 项全改完,单测可注入 Random 验证精确值

- [ ] **NF-T4/T5/T6 数据精度 + JSON + NPE**(P1,3 项合并):
  - MemoRecordMapper 评估 Float→Double 是否可统一 Float(DB 改 REAL→Float)
  - MemoRecordMapper JSON 改 `kotlinx.serialization`(@Serializable data class)
  - AntiRoteMemorization `log.rating?.uppercase()` 加 `?.` 防空
  - **通过标准**:3 项全改完,JSON 异常有日志不丢数据,rating null 不崩

- [ ] **NF-E1/E2 解密异常 runCatching**(P0,2 项合并,修复方案见 1.O):
  - ApiConfigRepository 4 处 `map { it.decrypted() }` 改 `mapNotNull { runCatching { it.decrypted() }.onFailure { Timber.w(it) }.getOrNull() }`
  - **通过标准**:单条解密失败不影响其他,Flow 不永久 failed

- [ ] **NF-E3/E4 catch(Exception) 过宽**(P1,2 项合并):
  - AiServiceImpl + 5 个 ViewModel 9 处 catch(Exception) 改 `catch(e: CancellationException) { throw e } catch(e: Exception) { ... }`
  - 或用 `runCatching` + `onFailure`
  - **通过标准**:0 处 catch(Exception) 不先 rethrow CancellationException

- [ ] **NF-E5/E11 日志体系建立**(P0,与 2.L 共担):
  - 加 `com.jakewharton.timber:timber:5.0.1` 依赖
  - WenyanApplication `Timber.plant(DebugTree())`(debug)/ `CrashReportingTree`(release)
  - **通过标准**:grep `Timber.` 全项目 ≥ 50 处,Log.x 降至 0

- [ ] **NF-E6/E7/E8 错误兜底**(P1,3 项合并):
  - ThemeRepositoryImpl DataStore Flow 加 `.catch { emit(defaultTheme) }`
  - MemoRecordMapper JSON 异常不重置(见 1.S)
  - ApiKeyCryptoImpl 解密失败返回 `Result.failure<ByteArray>(KeyStoreException)` 或抛特定异常
  - **通过标准**:3 项全改完

- [ ] **NF-M1/M2/MM1 模块反向依赖**(P0,3 项合并):
  - 读 3 个 build.gradle.kts:`core/designsystem/`、`core/ai/`、`core/data/`
  - core:designsystem → core:database:移除,ContentSource 枚举移到 core:database 或 core:common
  - core:ai → core:database:改通过 Repository interface 注入(RagEngine 注入 KnowledgePointRepository)
  - core:data → core:designsystem:ThemeRepositoryImpl 的 Compose Color 改 Long(Argb),UI 层做 Color(Long) 转换
  - **通过标准**:grep `project(":core:` 在 build.gradle.kts 中无反向依赖

- [ ] **NF-MM2/MM3/MM4/MM5 模块边界**(P1,4 项合并):
  - 全项目 grep `internal` 修饰符统计(当前 ~3 处),目标 ≥ 50 处
  - 6 个 Repository 抽 interface(与 NF-H4 共担)
  - core:data `api(project(":core:database"))` 评估改 `implementation` + Domain Model 映射
  - security-crypto 依赖从 core:ai 移到 core:data(ApiKeyCryptoImpl 应在 core:data)
  - **通过标准**:internal 数量 ≥ 50,Repository 全 interface,Entity 不跨模块

- [ ] **NF-C7 SystemClock.elapsedRealtime**(P1):
  - grep `System.currentTimeMillis` 在 FSRS 相关文件统计
  - 改 `SystemClock.elapsedRealtime()`(Android)或 `System.nanoTime()`(纯 Kotlin 测试)
  - **通过标准**:FSRS 调度相关 0 处 currentTimeMillis

**验证命令**:
```bash
grep -rn "Retrofit.Builder" --include="*.kt" core/ai/
grep -rn "@Volatile\|AtomicReference\|Mutex\|synchronized" --include="*.kt" --include="*.kt" feature/ core/
grep -rn "combine" --include="*.kt" core/data/src/main/java/.../repository/
grep -rn "\.first()" --include="*.kt" core/ai/
grep -rn "!!" --include="*.kt" feature/ core/ | wc -l
grep -rn "catch(Exception" --include="*.kt" feature/ core/
grep -rn "Timber\." --include="*.kt" feature/ core/ app/ | wc -l
grep -rn "project(\":core:" --include="*.kts" core/
grep -rn "^internal\|^\s*internal" --include="*.kt" core/ feature/ | wc -l
grep -rn "System.currentTimeMillis" --include="*.kt" core/fsrs/ core/data/
```

**输出格式**:`audit/2K-thread-type-resource.md`,线程安全矩阵表 + 类型安全清理清单 + 模块依赖图(修正后)。

**通过标准**:29 项全 ✅(11 P0 必修 + 18 P1),`!!` < 5,catch(Exception) 全加 CancellationException rethrow,3 处反向依赖消除,internal ≥ 50。

---

### 2.L 错误处理一致性 + 日志规范审计(v3 新增)

> **对应汇总表**:§2.10 中 NF-E1-E8 + §2.12 中 NF-EE1-EE6 + NF-DS11(共 ~15 项,7 P0 + 8 P1)。
> **与 2.K 分工**:2.K 侧重"线程安全 + 类型安全 + 资源泄漏"(代码级),2.L 侧重"错误类型模型 + 日志体系 + 错误 UX"(架构级)。

**必查文件**(20+):
- 全 14 处 Snackbar 错误消息处(9 Screen + 5 ViewModel)
- `core/ai/src/main/java/.../AiServiceImpl.kt`(错误 emit)
- `core/data/src/main/java/.../repository/*.kt`(6 Repository 零 try/catch)
- `core/data/src/main/java/.../ApiKeyCryptoImpl.kt`(解密错误)
- `core/data/src/main/java/.../MemoRecordMapper.kt`(JSON 错误)
- 全项目 grep `Log\.` / `catch(` / `errorMessage`

**检查项**:

- [ ] **NF-EE2/EE3 sealed AppError 引入**(P0):
  - 设计 `core/common/src/main/java/.../AppError.kt`:
    ```kotlin
    sealed class AppError(open val message: String) {
        data class Network(override val message: String, val code: Int? = null) : AppError(message)
        data class Auth(override val message: String) : AppError(message)
        data class Database(override val message: String, val cause: Throwable? = null) : AppError(message)
        data class Crypto(override val message: String) : AppError(message)
        data class Parse(override val message: String, val raw: String? = null) : AppError(message)
        data class Timeout(override val message: String) : AppError(message)
        data class Cancelled(override val message: String = "Cancelled") : AppError(message)
        data class Unknown(override val message: String, val cause: Throwable? = null) : AppError(message)
    }
    ```
  - Repository 返回 `Result<T>` 或 `Flow<Result<T>>`,ViewModel `map { it.getOrElse { AppError.X } }`
  - **通过标准**:6 Repository 全返回 Result,0 处裸 throw 到 ViewModel

- [ ] **NF-E5/E11/DS11 Timber 日志体系**(P0):
  - 加 Timber 依赖(见 2.K NF-E5)
  - WenyanApplication 配置 Timber.plant
  - 建《日志规范》:每模块 companion `private val TAG = "WenyanXxx"`,Timber 自动 TAG;敏感字段(apiKey / token)日志脱敏
  - **通过标准**:全项目 Log.x 0 处,Timber. ≥ 50 处,无 apiKey 明文日志

- [ ] **NF-E3 AiServiceImpl 错误归类**(P1):
  - 读 `AiServiceImpl.kt:60-85` catch(Exception)
  - 改为 catch(e: HttpException) / catch(e: IOException) / catch(e: CancellationException) 分别处理
  - emit 用 sealed `AiStreamItem.Error(AppError.Network(...))` 而非 `emit("网络错误")`
  - **通过标准**:401 显示"认证失败,请检查 API Key",超时显示"请求超时",网络显示"网络不可用"

- [ ] **NF-E4 ViewModel catch CancellationException**(P1,与 2.K 共担):
  - 5 个 ViewModel 9 处 catch(Exception) 加 `if (e is CancellationException) throw e`
  - **通过标准**:0 处误吞 CancellationException

- [ ] **NF-E1/E2 解密错误标记损坏配置**(P0,与 2.K 共担,见 1.O):
  - **通过标准**:解密失败配置标记 `isCorrupted = true`,UI 显示"此配置密钥已损坏,请重新输入"

- [ ] **NF-E6/E7/E8 数据层错误兜底**(P1,与 2.K 共担):
  - ThemeRepositoryImpl .catch
  - MemoRecordMapper 不静默重置(见 1.S)
  - ApiKeyCryptoImpl 返回 Result
  - **通过标准**:3 项全改完

- [ ] **NF-EE1 JSON 异常不丢历史**(P0,见 1.S):
  - **通过标准**:JSON 损坏时保留新 entry,日志告警

- [ ] **NF-EE4 Snackbar 统一格式**(P1):
  - grep `snackbarHostState.showSnackbar` 统计 14 处
  - 抽 `commonUi/showAppError(snackbar, appError: AppError)` 扩展
  - 5 种格式 → 1 种:`"操作失败:${appError.message}"`
  - **通过标准**:0 处直接 showSnackbar(String),全走 showAppError 扩展

- [ ] **NF-EE5 CardsViewModel 重试按钮**(P1):
  - 读 `CardsViewModel.kt:137-138` 评分失败
  - UiState 加 `val retryAction: (() -> Unit)? `,UI 显示"重试"按钮
  - **通过标准**:评分失败时 UI 有重试按钮,点击重新评分

- [ ] **NF-EE6 WenyanApplication TAG 规范**(P2):
  - `companion object { private val TAG = "WenyanApplication" }`
  - Log.e → Timber.e
  - **通过标准**:0 处裸 Log,全 Timber

- [ ] **NF-S7 CoroutineExceptionHandler 上报**(P1,与 2.J 共担):
  - **通过标准**:seed 加载失败 UI 显示错误 + 重试

- [ ] **错误 UX 一致性审计**(新):
  - 9 个 Screen 错误显示方式核对:Snackbar / Dialog / Inline / 全屏错误页
  - 统一规范:可恢复错误 Snackbar,不可恢复错误全屏页,确认操作 Dialog
  - **通过标准**:9 Screen 错误 UX 一致,符合规范

**验证命令**:
```bash
grep -rn "showSnackbar" --include="*.kt" feature/ | wc -l
grep -rn "catch(Exception" --include="*.kt" feature/ core/
grep -rn "Log\." --include="*.kt" --include="*.kt" feature/ core/ app/ | wc -l
grep -rn "Timber\." --include="*.kt" feature/ core/ app/ | wc -l
grep -rn "throw " --include="*.kt" core/data/src/main/java/.../repository/
grep -rn "Result<" --include="*.kt" core/data/src/main/java/.../repository/ | wc -l
```

**输出格式**:`audit/2L-error-logging.md`,AppError 类图 + 错误流转图 + Snackbar 统一规范文档 + 日志规范文档。

**通过标准**:15 项全 ✅,sealed AppError 引入,6 Repository 返回 Result,Log.x = 0,Timber. ≥ 50,Snackbar 1 种格式。

---

### 2.M Compose 副作用 + Accessibility + M3 实际使用度审计(v3 新增)

> **对应汇总表**:§2.11(24 项:2 P0 + 15 P1 + 7 P2)。

**必查文件**(15+):
- 9 个 Screen(全核对副作用 + A11y)
- `core/designsystem/src/main/java/.../theme/WenyanTheme.kt`(ColorScheme remember + MotionScheme)
- `core/designsystem/src/main/java/.../theme/WenyanMotion.kt`(与 MotionScheme.expressive 冲突)
- `core/designsystem/src/main/java/.../theme/Type.kt`(字重对比)
- `feature/graph/src/main/java/.../GraphCanvas.kt`(A11y + pointerInput + textLayouts)
- `core/designsystem/src/main/java/.../CardRenderer.kt`(硬编码 dp)
- `app/src/main/java/com/wenyan/app/WenyanApp.kt`(NavigationBar → WideNavigationRail)

**检查项**:

- [ ] **NF-UA1 GraphCanvas 无障碍**(P0):
  - 读 `GraphCanvas.kt:100-111` Canvas 绘制
  - 加 `Modifier.semantics { contentDescription = "知识图谱,${nodes.size} 个节点" }`
  - 节点点击改为单独 `Box(Modifier.semantics { role = Role.Button; contentDescription = node.name })` 叠加在 Canvas 上
  - **通过标准**:TalkBack 能朗读节点名,双击触发节点点击

- [ ] **NF-UT1 9 Screen smoke test**(P0):
  - 配置 `app/src/androidTest/.../screens/` 目录
  - 9 个 Screen 各 1 个 `createAndroidComposeRule` smoke test:断言关键元素存在
  - **通过标准**:`./gradlew :app:connectedDebugAndroidTest` 9 个 smoke test 全绿

- [ ] **NF-UC1/UC2/UC3/UC4/UC5 副作用修正**(P1,5 项合并):
  - KnowledgePointDetailScreen scrollState 移出 Crossfade lambda
  - WenyanTheme dynamicLightColorScheme / dynamicDarkColorScheme `remember(context)`
  - AiAssistantScreen LaunchedEffect(messages.size) 改 `derivedStateOf { messages.lastOrNull()?.id }` + 仅新消息滚动
  - AiAssistantScreen errorMessage LaunchedEffect 改 `LaunchedEffect(errorMessage) { errorMessage?.let { ...; clearError() } }`
  - GraphCanvas pointerInput(nodes.size) 改 `pointerInput(Unit) { ... }`(手势检测不依赖 nodes)
  - **通过标准**:5 项全改完,旋转 / 主题切换 / 新消息场景行为符合预期

- [ ] **NF-UA2/UA3/UA4/UA5 Accessibility**(P1,4 项合并):
  - AiAssistantScreen "知道了" Text 加 `Modifier.size(48.dp)` 最小触控
  - GraphCanvas 节点标签 fontSize 改 `12.sp`(或 dimen)
  - TonalCard .clickable 加 `role = Role.Button`
  - 关键状态变化处加 `Modifier.semantics { liveRegion = LiveRegionMode.Polite }`
  - **通过标准**:TalkBack 朗读"按钮",触控目标 ≥ 48dp,字号 ≥ 12sp,状态变化朗读

- [ ] **NF-UM1 MotionScheme 架空修正**(P1):
  - 读 `WenyanTheme.kt:75` + `WenyanMotion.kt:28-78`
  - 评估 WenyanMotion 是否可移除(改用 MotionScheme.expressive 默认)
  - 或显式 `motionScheme = MotionScheme.expressive()` 并废弃 WenyanMotion 自定义
  - **通过标准**:单一动画规范,MotionScheme.expressive 生效

- [ ] **NF-UM2/UM3/UM4 M3 Expressive 特性采用**(P1,3 项合并):
  - WenyanApp NavigationBar 评估 `WideNavigationRail`(WindowSizeClass.Expanded 时)
  - ApiConfigScreen AlertDialog 评估 `FlexibleBottomSheet`(7 字段表单)
  - Type.kt 加字重对比(标题 Bold / 正文 Normal / 辅助 Medium)
  - **通过标准**:平板用 WideNavigationRail,长表单用 BottomSheet,标题字重突出

- [ ] **NF-UP1/UP2/UP3/UP4/UP5 性能优化**(P1,5 项合并):
  - WenyanTheme AMOLED baseScheme.copy `remember`
  - SettingsScreen seedColors `remember { listOf(...) }`
  - KnowledgePointDetailScreen verticalScroll + forEach 改 LazyColumn(评估)
  - 7 处 LazyColumn items 加 `contentType`
  - GraphCanvas textLayouts remember key 改 `remember(labelColor, nodes)`
  - **通过标准**:5 项全改完,重组性能 profiling 无冗余计算

- [ ] **NF-UT2 渲染核心测试**(P1):
  - GraphCanvas / CardRenderer / WenyanTheme 各补 1 个 Compose 测试
  - **通过标准**:3 文件有回归保护

- [ ] **NF-UC6/UC7/UM5 P2 项**(P2,3 项合并):
  - 评估 DisposableEffect 使用场景(传感器 / 相机)
  - BackHandler(与 2.I 共担)
  - Crossfade contentKey(7 处加 `contentKey = { it }`)
  - **通过标准**:P2 项决策记录,若修则核对测试

- [ ] **Compiler Metrics 实测**(新,与 2.D 共担):
  - `./gradlew assembleDebug -P enableComposeCompilerReports=true`
  - 分析 `app/build/compose_reports/` 重组次数 + 不稳定参数
  - **通过标准**:关键 Composable(列表 item)重组次数 < 2

**验证命令**:
```bash
grep -rn "LaunchedEffect" --include="*.kt" feature/
grep -rn "Modifier.semantics\|role = Role\." --include="*.kt" feature/
grep -rn "fontSize\s*=\s*[0-9]" --include="*.kt" feature/ core/designsystem/ | grep -v "dimen"
grep -rn "collectAsState" --include="*.kt" feature/ | grep -v "WithLifecycle"
grep -rn "MotionScheme\|WenyanMotion" --include="*.kt" core/designsystem/
grep -rn "WideNavigationRail\|FlexibleBottomSheet" --include="*.kt" app/ feature/
grep -rn "contentType" --include="*.kt" feature/
```

**输出格式**:`audit/2M-compose-a11y-m3.md`,副作用矩阵表 + A11y 清单 + M3 Expressive 采用度报告 + Compiler Metrics 分析。

**通过标准**:24 项全 ✅(2 P0 + 15 P1 + 7 P2),TalkBack 可用,9 Screen smoke test 全绿,重组性能优化,M3 Expressive 特性 ≥ 3 项采用。

---

### 2.N 业务边界 + DataStore 持久化 + 进度持久化审计(v3 新增)

> **对应汇总表**:§2.12(38 项:11 P0 + 19 P1 + 8 P2)。修复方案见 1.J / 1.S / 1.R。
> **与 2.K 分工**:2.K 侧重"代码级错误处理",2.N 侧重"业务逻辑正确性 + 持久化范围"。

**必查文件**(20+):
- `core/data/src/main/java/.../SeedDataLoader.kt`(种子加载全链路)
- `core/data/src/main/java/.../repository/ApiConfigRepository.kt`(解密)
- `core/data/src/main/java/.../ApiKeyCryptoImpl.kt`(加密)
- `core/data/src/main/java/.../MemoRecordMapper.kt`(JSON 双写)
- `core/database/src/main/java/.../dao/KnowledgePointDao.kt`(LIKE 转义)
- `core/database/src/main/java/.../dao/StudyProgressDao.kt`(方法少)
- `core/database/src/main/java/.../dao/ReviewLogDao.kt`(双写一方)
- `core/database/src/main/java/.../entity/MemoRecordEntity.kt`(history JSON)
- `core/ai/src/main/java/.../SocraticTutor.kt`(三阶段串行)
- `core/ai/src/main/java/.../RecallChecker.kt`(L3 触发,与 1.G 共担)
- `core/ai/src/main/java/.../InterferenceWarner.kt`(相似度 clamp)
- `core/ai/src/main/java/.../PrerequisiteChecker.kt`(环检测)
- `core/ai/src/main/java/.../AntiRoteMemorization.kt`(阈值硬编码)
- `core/ai/src/main/java/.../RagEngine.kt`(query 超长)
- `core/ai/src/main/java/.../CardSplitter.kt`(中文数字)
- `core/ai/src/main/java/.../WeakSubgraphDetector.kt`(孤儿边)
- `core/fsrs/src/main/java/.../FsrsModels.kt`(Rating.fromValue)
- `core/fsrs/src/main/java/.../TierFsrsConfig.kt`(FSRS 配置未持久化)
- `feature/aiassistant/.../AiAssistantViewModel.kt`(消息内存)
- 全 Repository(业务边界 + 职责越界)

**检查项**:

- [ ] **NF-DS1/DS2/DS3/DS6 种子加载链路重构**(P0,4 项合并,见 1.J):
  - 双 DataStore 合并(与 2.J NF-H8 共担)
  - isInitialized / markInitialized IOException 兜底
  - REPLACE → IGNORE 策略
  - **通过标准**:种子加载全链路有防御,用户数据不被覆盖

- [ ] **NF-EE3 Repository 零 try/catch**(P0,与 2.L 共担):
  - 6 Repository grep `try` 统计(应 0)
  - SQLiteConstraintException / IOException 包装为 AppError.Database
  - **通过标准**:6 Repository 全 try/catch,返回 Result

- [ ] **NF-EE1 MemoRecordMapper JSON 不重置**(P0,见 1.S):
  - **通过标准**:JSON 损坏保留新 entry,日志告警

- [ ] **NF-BB3 PrerequisiteChecker 环检测**(P0,见 1.R):
  - **通过标准**:A→B→A 不无限递归,抛 CycleDetected

- [ ] **NF-BB9 Rating.fromValue 越界**(P0):
  - 读 `FsrsModels.kt` Rating.fromValue
  - 改返回 `Rating?` 或 `Rating.fromValueOrThrow` + `fromValueOrNull`
  - **通过标准**:DB rating=5 时返回 null 或默认 GOOD,不崩溃

- [ ] **NF-PP4 复习日志双写统一**(P0):
  - 读 `MemoRecordEntity.history` JSON + `ReviewLogDao` 表
  - 决策:废弃 history JSON 字段,统一用 review_logs 表(Migration_3_4 删除 history 列)
  - 或:废弃 review_logs 表,统一用 history JSON(不推荐,查询难)
  - **通过标准**:单一数据源,无双写

- [ ] **NF-PP5 错题本实现**(P0):
  - 新增 `WrongAnswerEntity` + `WrongAnswerDao`(entityId / wrongCount / lastWrongAt / userAnswer / correctAnswer)
  - ViewModel 集成:QuizViewModel 答错时插入,CardsViewModel 评分 AGAIN 时插入
  - 新 Screen:WrongAnswerScreen(错题列表 + 重做)
  - **通过标准**:错题可独立收集 + 复习,有 smoke test

- [ ] **NF-PP6 AiAssistantViewModel 消息持久化**(P0):
  - 评估:存 Room(chat_history 表已存在)+ Flow 观察
  - 或:存 DataStore(序列化 List<Message>,≤ 50 条)
  - **通过标准**:进程被杀后对话历史恢复

- [ ] **NF-DS11/EE2 错误类型 + 日志**(P0,与 2.L 共担):
  - **通过标准**:sealed AppError + Timber 体系

- [ ] **NF-BB1 LIKE 转义**(P1):
  - 读 `KnowledgePointDao.kt:84-87` searchByKeyword
  - 改 `query.replace("%", "\\%").replace("_", "\\_")` + `ESCAPE '\\'`
  - **通过标准**:"100%" 不匹配 "1000"

- [ ] **NF-BB2 SocraticTutor 三阶段 context 传递**(P1):
  - 读 `SocraticTutor.kt:43, 116, 137`
  - 第 2/3 阶段 prompt 加第 1 阶段输出作为 context
  - **通过标准**:三段输出逻辑一致

- [ ] **NF-BB5 ExamRepository List → Set**(P1):
  - 读 `ExamRepository.kt:91-96` getRelatedKnowledgePoints
  - 改 `relatedPoints.toSet()` 去重
  - **通过标准**:O(n) 而非 O(n*m)

- [ ] **NF-BB6 AntiRoteMemorization 空关联**(P1):
  - 读 `AntiRoteMemorization.kt:107-108`
  - 空关联返回 `null`(unknown)而非 `0f`(false)
  - **通过标准**:新卡不被标记死记硬背

- [ ] **NF-BB8 FsrsWrapper.schedule now 校验**(P1):
  - 读 `FsrsWrapper.kt:77, 142`
  - 加 `require(now > 0)` 或 `now.coerceAtLeast(0)`
  - **通过标准**:未来/过去时间被防御

- [ ] **NF-BB10 RagEngine query 长度限制**(P1):
  - 读 `RagEngine.kt:42` search
  - 加 `query.take(500)` + 日志告警
  - **通过标准**:超长输入不卡顿

- [ ] **NF-PP1 StudyProgressDao 扩展**(P1):
  - 读 `StudyProgressDao.kt:15-30`
  - 加 `getByDateRange / getBySubject / getTotalDuration` 方法
  - **通过标准**:学习统计页有数据支撑

- [ ] **NF-PP2 FSRS 配置持久化**(P1):
  - 读 `TierFsrsConfig.kt`
  - targetRetention / maximumInterval 持久化到 DataStore
  - Settings 加 FSRS 配置页
  - **通过标准**:用户可调整 FSRS 参数

- [ ] **NF-DS7/DS8/DS9/DS10/DS13 DataStore Key 治理**(P1,5 项合并):
  - 建 `core/data/src/main/java/.../prefs/PreferenceKeys.kt` 集中定义
  - Key 加 `v1_` 版本前缀
  - 默认值集中到 `ThemeDefaults` / `FsrsDefaults`
  - 写入统一在 IO 线程(DataStore .edit 默认 IO,核对)
  - **通过标准**:Key 单文件定义,有版本前缀,默认值集中

- [ ] **NF-EE4 Snackbar 统一**(P1,与 2.L 共担):
  - **通过标准**:1 种格式

- [ ] **NF-EE5 重试按钮**(P1,与 2.L 共担):
  - **通过标准**:评分失败可重试

- [ ] **NF-MM3-deep ReviewRepository 死代码**(P1):
  - 读 `ReviewRepository.kt:54` getAllVerifiedKnowledgePoints
  - 评估:删除(职责应在 KnowledgeRepository)或迁移
  - **通过标准**:死代码删除,职责归位

- [ ] **NF-BB4/BB11/BB12/BB13/BB14/BB15 P2 项**(P2,6 项合并):
  - CardSplitter indexToChinese 扩展 > 10
  - CardSplitter 100+ 标题 O(n²) 优化
  - WeakSubgraphDetector 孤儿边日志
  - PrerequisiteChecker 阈值可配置
  - AntiRoteMemorization 阈值可配置
  - InterferenceWarner 相似度 clamp
  - **通过标准**:P2 项决策记录,若修则核对测试

- [ ] **NF-DS12/PP3/PP7/DS9 持久化范围审计**(P2,4 项合并):
  - 审计全部设置项持久化状态(主题 / AI / FSRS / 学习历史)
  - 列出"未持久化"清单
  - **通过标准**:持久化范围文档化,无遗漏

- [ ] **考研业务规则审计**(新):
  - 核对考研日期规则(12 月倒数第二个周六,与 NF-F7 共担)
  - 核对三档 FSRS 配置(框架 / 强化 / 冲刺)切换逻辑
  - 核对 AntiRote / SocraticTutor / InterferenceWarner 业务正确性
  - **通过标准**:业务规则符合 spec,无逻辑漏洞

**验证命令**:
```bash
grep -rn "REPLACE\|IGNORE" --include="*.kt" core/data/src/main/java/.../SeedDataLoader.kt
grep -rn "LIKE" --include="*.kt" core/database/src/main/java/.../dao/
grep -rn "Rating.fromValue\|fromValue" --include="*.kt" core/fsrs/
grep -rn "history" --include="*.kt" core/database/src/main/java/.../entity/MemoRecordEntity.kt
grep -rn "try" --include="*.kt" core/data/src/main/java/.../repository/ | wc -l
grep -rn "WrongAnswer" --include="*.kt" core/database/
grep -rn "isInitialized\|markInitialized" --include="*.kt" core/data/
```

**输出格式**:`audit/2N-business-persistence.md`,业务规则矩阵 + 持久化范围表 + 错题本设计文档 + DataStore Key Registry。

**通过标准**:38 项全 ✅(11 P0 + 19 P1 + 8 P2),种子加载有防御,复习日志单写,错题本实现,消息持久化,FSRS 配置可调,业务规则正确。

---

### 2.O Manifest + Android 配置审计(v3 新增)

> **对应汇总表**:§2.13(9 项:1 P0 + 7 P1 + 1 P2)。修复方案见 1.N。

**必查文件**(6 个):
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-night/themes.xml`(核对是否存在)
- `app/src/main/res/xml/`(核对目录是否存在)
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/values/dimens.xml`(核对是否存在)

**检查项**:

- [ ] **NF-M8 res/xml 目录创建**(P0,见 1.N):
  - 创建 4 个 xml:backup_rules.xml / data_extraction_rules.xml / network_security_config.xml / locales_config.xml
  - Manifest 加对应属性引用
  - **通过标准**:res/xml/ 4 文件存在,Manifest 引用正确

- [ ] **NF-M1 fullBackupContent / dataExtractionRules**(P1):
  - Manifest `<application>` 加 `android:fullBackupContent="@xml/backup_rules"` + `android:dataExtractionRules="@xml/data_extraction_rules"`
  - **通过标准**:Android 12+ 备份行为可预测

- [ ] **NF-M2 networkSecurityConfig**(P1):
  - Manifest 加 `android:networkSecurityConfig="@xml/network_security_config"`
  - 配置 cleartextTrafficPermitted="false" + 可选 Certificate Pinning
  - **通过标准**:网络安全集中配置

- [ ] **NF-M4 enableOnBackInvokedCallback**(P1):
  - Manifest `<application>` 加 `android:enableOnBackInvokedCallback="true"`
  - **通过标准**:Android 13+ 预测返回手势显示 App 内动画

- [ ] **NF-C3 locales_config.xml**(P1):
  - res/xml/locales_config.xml 列出 zh-CN
  - Manifest 加 `android:localeConfig="@xml/locales_config"`
  - **通过标准**:未来加英文版可系统切换

- [ ] **NF-C5 ic_launcher monochrome**(P1):
  - 读 `mipmap-anydpi-v26/ic_launcher.xml`
  - 加 `<monochrome android:drawable="@drawable/ic_launcher_monochrome" />`
  - 创建 `ic_launcher_monochrome.xml`(vector)
  - **通过标准**:Android 13+ themed icon 生效

- [ ] **NF-C10 dimens.xml**(P1):
  - 创建 `values/dimens.xml` 集中 dp / sp
  - CardRenderer 等 20+ 硬编码改 `@dimen/xxx`
  - **通过标准**:0 处硬编码 dp / sp 在 CardRenderer

- [ ] **NF-M3 usesCleartextTraffic**(P2):
  - Manifest 加 `android:usesCleartextTraffic="false"`(与 networkSecurityConfig 重复但显式)
  - **通过标准**:显式声明

- [ ] **NF-M7 application label**(P2):
  - `<application>` 加 `android:label="@string/app_name"`
  - **通过标准**:应用列表显示 App 名

**验证命令**:
```bash
ls app/src/main/res/xml/ 2>/dev/null || echo "xml 目录不存在"
grep -n "fullBackupContent\|dataExtractionRules\|networkSecurityConfig\|localeConfig\|enableOnBackInvokedCallback" app/src/main/AndroidManifest.xml
grep -n "monochrome" app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
ls app/src/main/res/values/dimens.xml 2>/dev/null || echo "dimens.xml 不存在"
grep -rn "dp\|\.sp" --include="*.kt" core/designsystem/src/main/java/.../CardRenderer.kt | wc -l
```

**输出格式**:`audit/2O-manifest-config.md`,Manifest 属性核对表 + res/xml 文件清单 + dimens 迁移清单。

**通过标准**:9 项全 ✅,res/xml/ 4 文件存在,Manifest 5 属性齐全,ic_launcher monochrome,dimens.xml 存在。

---

## Phase 3 — 依赖升级路径规划(扩展,必做,P1)

> **核心问题**:AGP 8.6.0 钉死链(第二轮 3.1 节)如何解开?

### 3.1 短期(不碰 AGP,可立即升)

| 依赖 | 当前 | 目标 | 风险 | 备注 |
|------|------|------|------|------|
| coroutines | 1.8.1 | 1.9.x | 低 | API 兼容 |
| kotlinx-serialization | 1.6.3 | 1.7.x | 低 | - |
| activity-compose | 1.9.1 | +1 minor | 低 | - |
| core-ktx | 1.13.1 | +1 minor | 低 | - |
| lifecycle-* | 2.8.4 | +1 minor | 低 | - |
| materialKolor | 4.1.1 | 5.0.0 | 中 | API 变更 |
| **新** retrofit | 2.9.0 | 2.11+ | 中 | 改用官方 kotlinx-serialization-converter,删 jakewharton |

### 3.2 中期(需升 AGP 到 8.8+)

| 依赖 | 当前 | 目标 | 阻塞 |
|------|------|------|------|
| AGP | 8.6.0 | 8.8.x | 需测 KSP 2.3.x 兼容 |
| KSP | 2.3.2 | 2.3.10 | AGP 8.8+ |
| Hilt | 2.57.1 | 2.59+ | AGP 8.8+ |
| compileSdk | 35 | 35(保持) | - |

### 3.3 长期(大爆炸)

| 依赖 | 当前 | 目标 | 阻塞 |
|------|------|------|------|
| AGP | 8.6.0 | 9.x | compileSdk 36+ |
| material3 | 1.5.0-alpha18 | stable | AGP 9.1 + compileSdk 37 |
| Kotlin | 2.3.10 | 2.4.x | 待发布 |
| navigation-compose | 2.7.7 | 2.8.x(类型安全) | 独立可升 |

### 3.4 安全替换

- [ ] **新 NF-B7**:`security-crypto 1.1.0-alpha06` 死依赖直接移除(ApiKeyCryptoImpl 不用它)
- [ ] 评估 `androidx.crypto` 是否有 stable 替代(预估无)
- [ ] Android Keystore 直接封装已实现 ✓

### 3.5 CVE 联网复核

用 [OSV.dev](https://osv.dev/) 或 [GitHub Advisories](https://github.com/advisories) 复核:
- [ ] okhttp 4.12.0
- [ ] retrofit 2.9.0
- [ ] coroutines 1.8.1
- [ ] kotlinx-serialization 1.6.3
- [ ] Hilt 2.57.1
- [ ] Room 2.7.0
- [ ] Compose BOM 2025.12.00

---

## Phase 4 — emulator 实测与运行时验证(扩展,必做,P0)

> **核心问题**:代码"能编译"和"能跑对"是两回事。

### 4.1 环境准备

```bash
avdmanager create avd -n wenyan_test -k "system-images;android-35;google_apis;x86_64" -d pixel_7
emulator -avd wenyan_test -no-window -no-audio -no-boot-anim &
adb wait-for-device
```

### 4.2 测试矩阵(扩展为 25 项)

| # | 场景 | 验证项 | 通过标准 |
|---|------|-------|---------|
| 1 | App 启动 | 冷启动 < 2s,无崩溃 | 主页可交互 |
| 2 | 种子数据导入 | SeedDataLoader 跑完无异常 | 知识点列表非空 |
| 3 | FSRS 调度 | 评分 AGAIN/GOOD/EASY 后下次到期日 | EASY 间隔 > GOOD > AGAIN |
| 4 | **新 NF-F1** FSRS 数值对比 | 实测 EASY 间隔是否过大(exp(w[8]) 膨胀) | 与 fsrs4anki 对比 |
| 5 | 卡片翻转 | 翻转动画无镜像(v0.3 修复) | 正反面正确显示 |
| 6 | AI 入口 | TopBar AI 图标点击跳转 | 进入 AiAssistantScreen |
| 7 | Tab 切换 | 5 个 Tab 切换动画 | fade transition 流畅 |
| 8 | 暗色模式 | 切换无白屏闪烁(NF-U4 修后) | values-night 生效 |
| 9 | 旋转屏 | 旋转后 UI 状态(当前会丢,P0-E3) | 记录现象 |
| 10 | 进程被杀 | "不保留活动"后重启 | 记录现象 |
| 11 | 网络断开 | AI 请求报错文案 | 显示差异化错误(P0-E1 修后) |
| 12 | **新 NF-A1** RecallChecker | 论述题评分"部分正确"是否触发 L3 LLM | 日志可见 |
| 13 | 时钟回拨 | 改系统时间后到期卡片 | 不消失(P0-E4 修后) |
| 14 | AI 对话 | 发消息 → 收回复 → 切后台 → 杀进程 → 重启 | 历史恢复(P0-E2 修后) |
| 15 | 大量数据 | 用脚本插 1000 知识点 | 列表滚动流畅 60fps |
| 16 | 内存 | 跑 30 分钟 + 看 Profiler | 无内存泄漏 |
| 17 | Compose 重组 | Layout Inspector | 列表 item 重组 < 5 次/秒 |
| 18 | **新 NF-F7** 考研日期 | ExamCountdownManager 显示日期 | 与南师大官方对比 |
| 19 | ⚠ v3 **BottomBar 显隐**(NF-N1) | 进入 knowledge_detail/api_config/ai_assistant | BottomBar 隐藏,InputBar 不重叠 |
| 20 | ⚠ v3 **SavedStateHandle 恢复**(NF-L1/L2/L3) | 旋转屏 + 进程被杀后:ApiConfig editingId / Cards 翻转+位置 / Quiz 年份+展开 | 7 ViewModel 状态全恢复 |
| 21 | ⚠ v3 **Splash Screen**(NF-S3) | 冷启动观察 | 无白屏,seed 加载期间显示 Splash |
| 22 | ⚠ v3 **LeakCanary + StrictMode**(NF-S1/S2) | 跑 30 分钟 + Logcat | LeakCanary 无泄漏通知,StrictMode 无主线程 IO 警告 |
| 23 | ⚠ v3 **res/xml 配置**(NF-M8) | adb shell backup / 系统语言切换 / themed icon | 备份规则生效,语言可切换,themed icon 显示 |
| 24 | ⚠ v3 **解密失败容错**(NF-E1/E2) | 清除 KeyStore(adb shell)或注入损坏密钥 | 单条配置失败不影响其他,UI 标记"已损坏" |
| 25 | ⚠ v3 **环检测 + JSON 损坏**(NF-BB3/EE1) | 建循环依赖 A→B→A / 注入损坏 history JSON | 环检测抛 CycleDetected 不崩,JSON 损坏保留新 entry |

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

### 4.5 **新** FSRS 数值对比专项

跑 5 组典型用例,手算 + 实测对比:

| 用例 | D | S | R | rating | 当前代码 nextInterval | fsrs4anki 官方 | 差异 |
|------|---|---|---|--------|---------------------|---------------|------|
| 1 | 5 | 10 | 0.9 | GOOD | ? | ? | ? |
| 2 | 5 | 10 | 0.9 | EASY | ? | ? | ? |
| 3 | 5 | 10 | 0.9 | AGAIN | ? | ? | ? |
| 4 | 8 | 20 | 0.85 | GOOD | ? | ? | ? |
| 5 | 3 | 5 | 0.95 | EASY | ? | ? | ? |

**通过标准**:5 组差异 < 5% 选 B(文档化);> 10% 选 A(升级 FSRS-6)。

---

## Phase 5 — 出审计报告 + 修复 + 验证(必做,P0)

### 5.1 报告输出

执行完 Phase 0-4 后,输出 `docs/plans/full-audit-v0.5.0-report.md`(报告,非计划):
- 第 0 节:回归性复审结果(0.1-0.4 通过/失败)
- 第 1 节:18 个 P0 修复状态(已修/方案待实施)
  - 1.A P0-E1 网络异常差异化
  - 1.B P0-E4 FSRS 时钟回拨
  - 1.C P0-E2 AI 对话持久化(方案)
  - 1.D P0-E3 状态恢复(方案)
  - 1.E P0-F1 FSRS 公式版本决策(选项 A/B/C)
  - 1.F P0-V1 ThemeViewModel try/catch(已修)
  - 1.G P0-A1 RecallChecker L3 触发(已修)
  - 1.H P0-D1 Migration_2_3 回填 reps(已修)
  - 1.I P0-DB MemoRecordDao upsert 改造(已修)
  - 1.J ⚠ v3 P0-DS1 种子加载链路重构
  - 1.K ⚠ v3 P0-N1 BottomBar 顶级路由过滤
  - 1.L ⚠ v3 P0-L1/L2/L3 SavedStateHandle 全 ViewModel 注入
  - 1.M ⚠ v3 P0-S3/S2/S1 三大基础工具补齐
  - 1.N ⚠ v3 P0-M8 res/xml 目录创建
  - 1.O ⚠ v3 P0-E1/E2 解密异常 runCatching 包装
  - 1.P ⚠ v3 P0-T1 studyText!! 改 orEmpty()
  - 1.Q ⚠ v3 P0-C1 editingId 改 MutableStateFlow
  - 1.R ⚠ v3 P0-BB3 PrerequisiteChecker 加环检测
  - 1.S ⚠ v3 P0-EE1 MemoRecordMapper JSON 异常不再静默重置
- 第 2 节:15 个维度发现(P0/P1/P2 清单,2.A-2.O)
- 第 3 节:依赖升级路径
- 第 4 节:emulator 实测结果(25 项测试矩阵)
- 第 5 节:本轮新增 vs 前三轮累计统计
- 第 6 节:审计质量自评

### 5.2 修复 Batch 计划(扩展为 7 Batch)

根据报告的 P0/P1 数量,分 Batch 修复:

- **Batch 1**(P0 立即修,1 个工作单元):
  - 1.F ThemeViewModel try/catch
  - 1.G RecallChecker L3 触发
  - 1.I MemoRecordDao upsert 改造
  - NF-B1 release.yml Verify keystore 加 `if`

- **Batch 2**(P0 数据库迁移,1 个工作单元):
  - 1.H Migration_2_3 回填 reps
  - 补全 schema 1.json
  - 升级 WenyanDatabase version=3

- **Batch 3**(P0 网络与时钟,2 个工作单元):
  - 1.A P0-E1 网络异常差异化
  - 1.B P0-E4 FSRS 时钟回拨防护

- **Batch 4**(P0 FSRS 决策执行,2-3 个工作单元,依赖 Phase 1.E 决策):
  - 若选 A:升级 FSRS-6 公式
  - 若选 B:文档化 + 改类注释
  - 若选 C:先实测对比再决策

- **Batch 5**(P1 关键,3-4 个工作单元):
  - NF-V2 stateIn .catch 兜底全补
  - NF-V3 6 Repository 单元测试补全
  - NF-U1 rememberSaveable 全补(CardsScreen/QuizScreen 优先)
  - NF-U2 strings.xml 全量迁移
  - NF-U3 themes.xml M3 改造
  - NF-U4 values-night
  - NF-B2 consumer-rules.pro 全套 + Release minify 启用

- **Batch 6**(⚠ v3 P0 架构与基础设施,4-5 个工作单元):
  - 1.K P0-N1 BottomBar 顶级路由过滤
  - 1.L P0-L1/L2/L3 SavedStateHandle 全 ViewModel 注入(7 个 ViewModel)
  - 1.M P0-S3/S2/S1 三大基础工具补齐(Splash + LeakCanary + StrictMode)
  - 1.N P0-M8 res/xml 目录创建(4 个 xml + Manifest 属性 + ic_launcher monochrome + dimens.xml)
  - NF-M1/MM1 模块反向依赖消除(designsystem↔database / ai→database / data→designsystem)
  - 1.P P0-T1 studyText!! 改 orEmpty()(全项目 !! 清理)
  - 1.Q P0-C1 editingId 改 MutableStateFlow
  - 配套测试:7 ViewModel SavedStateHandle 测试 + 3 个基础设施 smoke test

- **Batch 7**(⚠ v3 P0 数据安全与持久化,4-5 个工作单元):
  - 1.J P0-DS1 种子加载链路重构(双 DataStore 合并 + IO 兜底 + IGNORE 策略 + 超时 + Splash 同步)
  - 1.O P0-E1/E2 解密异常 runCatching 包装(防 Flow 永久 failed)
  - 1.R P0-BB3 PrerequisiteChecker 加环检测(防无限递归)
  - 1.S P0-EE1 MemoRecordMapper JSON 异常不再静默重置
  - NF-BB9 Rating.fromValue 越界防御
  - NF-PP4 复习日志双写统一(废弃 history JSON,Migration_3_4)
  - NF-PP5 错题本实现(WrongAnswerEntity + Dao + Screen)
  - NF-PP6 AiAssistantViewModel 消息持久化
  - NF-EE3 Repository 零 try/catch(sealed AppError + Result 返回)
  - NF-E5 Timber 日志体系建立
  - 配套测试:种子加载链路测试 + 解密容错测试 + 环检测测试 + JSON 损坏测试 + 错题本测试 + Repository Result 测试

### 5.3 验证标准(扩展)

修复完成后必须满足:
- [ ] `assembleDebug` SUCCESSFUL
- [ ] `testDebugUnitTest` 0 failures(数量应 ≥ 280,v3 新增 50+ 测试:7 ViewModel SavedStateHandle + 6 Repository Result + 错题本 + 环检测 + JSON 损坏 + 解密容错 + 基础设施)
- [ ] `lintDebug` 0 Errors
- [ ] emulator 25 项测试矩阵全部通过
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

## 附录 A:执行 Checklist(v3 完整版,可勾选)

### Phase 0 回归复审
- [ ] 0.1 28 文件逐行 diff 复审(subagent 已覆盖 90%,补 10%)
- [ ] 0.2 207 tests 重跑通过
- [ ] 0.3 assembleDebug + lint 通过
- [ ] 0.4 v0.4.2 修复副作用检查

### Phase 1 P0 修复(扩展为 18 项)
- [ ] 1.A P0-E1 网络异常差异化(实施)
- [ ] 1.B P0-E4 FSRS 时钟回拨(实施)
- [ ] 1.C P0-E2 AI 对话持久化(方案)
- [ ] 1.D P0-E3 状态恢复(方案)
- [ ] 1.E P0-F1 FSRS 公式版本决策(选项 A/B/C)
- [ ] 1.F P0-V1 ThemeViewModel try/catch(立即修)
- [ ] 1.G P0-A1 RecallChecker L3 触发(立即修)
- [ ] 1.H P0-D1 Migration_2_3 回填 reps(实施)
- [ ] 1.I P0-DB MemoRecordDao upsert 改造(立即修)
- [ ] 1.J ⚠ v3 P0-DS1 种子加载链路重构(双 DataStore 合并 + IO 兜底 + IGNORE)
- [ ] 1.K ⚠ v3 P0-N1 BottomBar 顶级路由过滤(30 分钟)
- [ ] 1.L ⚠ v3 P0-L1/L2/L3 SavedStateHandle 全 ViewModel 注入(7 个 ViewModel)
- [ ] 1.M ⚠ v3 P0-S3/S2/S1 三大基础工具补齐(Splash + LeakCanary + StrictMode)
- [ ] 1.N ⚠ v3 P0-M8 res/xml 目录创建(4 xml + Manifest + monochrome + dimens)
- [ ] 1.O ⚠ v3 P0-E1/E2 解密异常 runCatching 包装(防 Flow 永久 failed)
- [ ] 1.P ⚠ v3 P0-T1 studyText!! 改 orEmpty()(5 分钟)
- [ ] 1.Q ⚠ v3 P0-C1 editingId 改 MutableStateFlow(30 分钟)
- [ ] 1.R ⚠ v3 P0-BB3 PrerequisiteChecker 加环检测(防无限递归)
- [ ] 1.S ⚠ v3 P0-EE1 MemoRecordMapper JSON 异常不再静默重置

### Phase 2 新维度(扩展为 15 维度)
- [ ] 2.A 业务逻辑正确性(FSRS + 图谱 + AI + 真题 + 考研日期 + AntiRote 干预)
- [ ] 2.B Room SQL + 数据模型(含 1.json 补全 + 死依赖清理 + 死表核对)
- [ ] 2.C 协程/Flow(含 stateIn .catch 兜底全审)
- [ ] 2.D Compose 重组性能(含 @Immutable 全审 + derivedStateOf)
- [ ] 2.E 资源与本地化(含 themes.xml M3 + values-night + CardRenderer 硬编码 dp)
- [ ] 2.F 构建系统与 CI/CD(含 consumer-rules + Release minify + convention plugin + 反向依赖)
- [ ] 2.G 安全深度(含 Certificate Pinning + 密码硬编码清理 + networkSecurityConfig)
- [ ] 2.H 测试质量提升(含 6 Repository 零测试补全 + Migration 测试 + Fake 真实化)
- [ ] 2.I ⚠ v3 Navigation 图 + Lifecycle 边界(BottomBar + SavedStateHandle + BackHandler)
- [ ] 2.J ⚠ v3 Hilt DI 图 + 启动流程(@HiltAndroidTest + Splash + StrictMode + LeakCanary)
- [ ] 2.K ⚠ v3 资源/线程/类型/模块边界(Retrofit 单例 + Atomic + !! 清理 + 反向依赖 + internal)
- [ ] 2.L ⚠ v3 错误处理 + 日志规范(sealed AppError + Timber + Snackbar 统一)
- [ ] 2.M ⚠ v3 Compose 副作用 + Accessibility + M3 Expressive(LaunchedEffect + role + TalkBack + MotionScheme)
- [ ] 2.N ⚠ v3 业务边界 + DataStore 持久化(种子链路 + 双写统一 + 错题本 + 消息持久化)
- [ ] 2.O ⚠ v3 Manifest + Android 配置(res/xml + backup_rules + monochrome + dimens)

### Phase 3 依赖升级
- [ ] 3.1 短期升级(含 retrofit 2.11 + 删 jakewharton + Timber + Splash + LeakCanary)
- [ ] 3.4 security-crypto 死依赖移除
- [ ] 3.5 CVE 复核

### Phase 4 emulator 实测(扩展为 25 项)
- [ ] 4.2 25 项测试矩阵(含 FSRS 数值对比 + RecallChecker L3 + 考研日期 + BottomBar + SavedStateHandle + Splash + LeakCanary + res/xml + 解密容错 + 环检测)
- [ ] 4.3 性能 profiling
- [ ] 4.4 可访问性实测
- [ ] 4.5 FSRS 数值对比专项

### Phase 5 报告与修复(扩展为 7 Batch)
- [ ] 5.1 出 v0.5.0 报告
- [ ] 5.2 Batch 1(P0 立即修)
- [ ] 5.2 Batch 2(P0 数据库迁移)
- [ ] 5.2 Batch 3(P0 网络与时钟)
- [ ] 5.2 Batch 4(P0 FSRS 决策执行)
- [ ] 5.2 Batch 5(P1 关键)
- [ ] 5.2 Batch 6 ⚠ v3(P0 架构与基础设施)
- [ ] 5.2 Batch 7 ⚠ v3(P0 数据安全与持久化)
- [ ] 5.3 验证标准全部通过(280+ tests + 25 矩阵)
- [ ] 5.4 (可选)发 v0.3.0 Release

---

## 附录 B:并行执行建议(v3 修订)

以下维度**无依赖**可并行(用 Task subagent):

| 并行组 | 维度 | 估时 | 备注 |
|-------|------|------|------|
| Group 1 | 2.A 业务逻辑 + 2.B Room SQL + 2.C 协程 | 3 个 subagent 并行 | 已预扫描,补审 10% |
| Group 2 | 2.D Compose 性能 + 2.E 资源 + 2.F 构建 | 3 个 subagent 并行 | 已预扫描,补审 10% |
| Group 3 | 2.G 安全 + 2.H 测试质量 | 2 个 subagent 并行 | 已预扫描,补审 10% |
| Group 4 | 2.I Navigation + 2.J Hilt/Startup | 2 个 subagent 并行 | ⚠ v3 新维度 |
| Group 5 | 2.K 资源/线程/类型 + 2.L 错误/日志 | 2 个 subagent 并行 | ⚠ v3 新维度(2.K 与 2.L 有交叉,需协调 AppError 设计) |
| Group 6 | 2.M Compose/A11y/M3 + 2.O Manifest | 2 个 subagent 并行 | ⚠ v3 新维度 |
| Group 7 | 2.N 业务/DataStore/持久化 | 1 个 subagent(独立) | ⚠ v3 新维度,内容多(38 项) |
| Group 8 | Phase 0 回归复审 + Phase 1 P0 修复 | 串行(修复方案依赖复审结果) | - |
| Group 9 | Phase 1.F + 1.G + 1.I + 1.P + 1.Q + 1.S(立即可修) | 并行(6 个无依赖) | ⚠ v3 新增 3 个立即可修 |
| Group 10 | Phase 1.H Migration | 独立(需生成 schema) | 1 个工作单元 |
| Group 11 | Phase 1.E FSRS 决策 | 独立(需手算对比) | 3 个工作单元 |
| Group 12 | Phase 1.J 种子链路 + 1.O 解密 + 1.R 环检测 | 并行(3 个无依赖) | ⚠ v3 新增 |
| Group 13 | Phase 1.L SavedStateHandle + 1.M 基础工具 + 1.N res/xml | 并行(3 个无依赖) | ⚠ v3 新增 |
| Group 14 | Phase 4 emulator 实测 | 独立(需 emulator 环境) | 2 个工作单元 |

**总并行度**:Phase 2 的 15 个维度可分组并行,理论 7 个 subagent 同时跑(Group 1-7)。考虑上下文成本,建议 3-4 个并行。

**串行依赖**:
- Phase 1.F/G/I/P/Q/S → Phase 1.H(数据库迁移可能影响测试)
- Phase 1.E 决策 → Phase 5.2 Batch 4(FSRS 修复)
- Phase 1.J 种子链路 → Phase 1.M Splash 同步(SeedDataLoader 重构后才能接 Splash)
- Phase 1.L SavedStateHandle → Phase 4 测试矩阵第 20 项(验证恢复)
- 2.K 与 2.L 需协调 AppError 设计(2.K 先出代码级清理,2.L 再出架构级 sealed 类)
- 2.N 错题本 + 双写统一 → 2.B Room SQL(Migration_3_4 依赖双写决策)
- Phase 0 → Phase 1/2(回归通过才开新工作)

---

## 附录 C:输出文件清单(v3 修订)

执行完本计划后,将产出以下文档:

**计划与报告**(5 个):
1. `docs/plans/full-audit-v0.5.0-deep.md` — 本计划(已存在,v3)
2. `docs/plans/full-audit-v0.5.0-report.md` — 审计报告(Phase 5.1)
3. `docs/plans/phase0-regression-review.md` — 回归复审结果(Phase 0.1)
4. `docs/plans/fsrs-version-decision.md` — FSRS 版本决策记录(Phase 1.E)
5. `docs/plans/dependency-upgrade-path.md` — 依赖升级路径(Phase 3)

**FSRS 专项**(2 个):
6. `docs/plans/fsrs-formula-diff.md` — FSRS 公式对比(Phase 2.A.1)
7. `docs/plans/fsrs-formula-comparison-test.md` — FSRS 数值对比测试(Phase 4.5)

**性能与实测**(2 个):
8. `docs/plans/compose-recomposition-report.md` — Compose 重组报告(Phase 2.D)
9. `docs/plans/emulator-test-matrix.md` — 实测结果(Phase 4,25 项矩阵)

**⚠ v3 审计细节文档**(7 个,对应 2.I-2.O):
10. `audit/2I-navigation-lifecycle.md` — Navigation/Lifecycle 审计(Phase 2.I)
11. `audit/2J-hilt-startup.md` — Hilt/启动流程审计(Phase 2.J)
12. `audit/2K-thread-type-resource.md` — 资源/线程/类型/模块审计(Phase 2.K)
13. `audit/2L-error-logging.md` — 错误处理/日志审计(Phase 2.L)
14. `audit/2M-compose-a11y-m3.md` — Compose/A11y/M3 审计(Phase 2.M)
15. `audit/2N-business-persistence.md` — 业务/DataStore/持久化审计(Phase 2.N)
16. `audit/2O-manifest-config.md` — Manifest/Android 配置审计(Phase 2.O)

**⚠ v3 设计与规范文档**(5 个):
17. `docs/design/app-error-model.md` — sealed AppError 类设计(Phase 2.L)
18. `docs/design/wrong-answer-book.md` — 错题本设计(Phase 2.N,NF-PP5)
19. `docs/design/datastore-key-registry.md` — DataStore Key 集中索引(Phase 2.N,NF-DS7)
20. `docs/reference/logging-convention.md` — 日志规范(Phase 2.L,NF-E5)
21. `docs/reference/thread-safety-convention.md` — 线程安全规范(Phase 2.K,NF-C2)

**代码与 Schema**(3 个):
22. 代码修复 commit(Phase 5.2,7 个 Batch)
23. `core/database/schemas/.../1.json` — 补全 schema 1.json(Phase 1.H)
24. `core/database/schemas/.../3.json` — 新增 schema 3.json(Phase 1.H)
25. `core/database/schemas/.../4.json` — ⚠ v3 新增 schema 4.json(Phase 2.N,NF-PP4 双写统一 Migration_3_4)

**状态与日志**(4 个):
26. `docs/SESSION_LOG.md` 新增本轮会话记录
27. `docs/00-STATUS.md` 状态更新
28. `docs/03-FAILED-ATTEMPTS.md` 新增本轮失败方案(如有)
29. `docs/02-VERSION-MATRIX.md` 新增版本兼容信息(如 Timber/Splash/LeakCanary)

---

## 附录 D:v2 修订记录(2026-07-14)

基于 5 个深度 subagent 预扫描(覆盖 FSRS / 数据库 / ViewModel / Compose UI / AI+构建)的发现,对 v1 计划做了以下修订:

### 新增 P0(5 项)

1. **NF-F1/F2/F3**:FSRS 公式版本定位决策(`exp(w[8])` / decay=-1 / nextInterval)— Phase 1.E
2. **NF-V1**:ThemeViewModel 5 处全裸 launch — Phase 1.F
3. **NF-A1**:RecallChecker L3 永不触发 — Phase 1.G
4. **NF-D1**:Migration_1_2 未回填 reps — Phase 1.H
5. **NF-D2**:MemoRecordDao upsert 丢 history — Phase 1.I

### 新增 P1(23 项)

- FSRS:NF-F4(权重槽位)/ NF-F5(死字段)/ NF-F6(三档零测试)/ NF-F7(考研日期)/ NF-F8(注释语义)
- 数据库:NF-D3(observeDue 不刷新)/ NF-D4(双表)/ NF-D5(api_key 明文)
- ViewModel:NF-V2(stateIn 无 .catch)/ NF-V3(6 Repository 零测试)
- AI:NF-A2(L2 缺 GOOD)
- UI:NF-U5(零 @Immutable)/ NF-U6(零 derivedStateOf)/ NF-U7(AnimatedVisibility spec)/ NF-U8(无界 back stack)
- 构建:NF-B2(.pro 全空 + minify 未开)/ NF-B3(密码硬编码)/ NF-B4(无 convention plugin)/ NF-B5(反向依赖 designsystem)/ NF-B6(反向依赖 ai)/ NF-B9(retrofit 过时)

### 新增 P2(7 项)

- NF-D6(schema 1.json 缺失)/ NF-D7(TypeConverter 不可逆)
- NF-B7(security-crypto 死依赖)/ NF-B8(5 个 wenyan-feature-* 死声明)
- 其他细节项

### 扩展检查项

- Phase 0.4:v0.4.2 修复副作用检查
- Phase 2.A.4:考研日期规则 + AntiRote 干预策略
- Phase 2.B.3:1.json 补全 + 死依赖清理
- Phase 2.C:stateIn .catch 兜底全审
- Phase 2.D:@Immutable 全审 + derivedStateOf
- Phase 2.E:themes.xml M3 + values-night + CardRenderer 硬编码 dp
- Phase 2.F:consumer-rules + Release minify + convention plugin + 反向依赖
- Phase 2.G:Certificate Pinning + 密码硬编码清理
- Phase 2.H:6 Repository 零测试 + Migration 测试 + Fake 真实化
- Phase 4.2:18 项测试矩阵(含 FSRS 数值对比 + RecallChecker L3 + 考研日期)
- Phase 4.5:FSRS 数值对比专项
- Phase 5.2:5 Batch 修复计划

### 量化指标对比

| 项 | v1 计划 | v2 计划 | 增量 |
|----|--------|--------|------|
| P0 项 | 4(E1/E2/E3/E4) | 9(+F1/V1/A1/D1/DB) | +5 |
| P1 项 | 估 30 | 估 53 | +23 |
| Checklist 项 | ~30 | ~50 | +20 |
| emulator 测试矩阵 | 15 项 | 18 项 | +3 |
| 修复 Batch | 4 个 | 5 个 | +1 |
| 输出文档 | 11 个 | 15 个 | +4 |

---

**v2 部分完成。** 详见附录 E 的 v3 修订,在 v2 基础上新增 ~25 个 P0 + ~80 个 P1。

---

## 附录 E:v3 修订记录(2026-07-14)

基于第二轮 5 个深度 subagent 扫描(覆盖 Navigation/Lifecycle/Manifest/Hilt/测试质量/资源泄漏/线程安全/类型安全/错误处理/模块边界/Compose 副作用/Accessibility/M3 Expressive/业务边界/DataStore/进度持久化 **15 个新维度**),对 v2 计划做了以下修订。

### 扫描方法

启动 5 个并行 Task subagent(search 类型),每个覆盖 3 个维度,对 160 个 .kt 文件 + 13 个 build.gradle.kts + AndroidManifest + res/ 目录做逐行扫描:

| Subagent | 覆盖维度 | 发现数 |
|----------|---------|--------|
| Task 1 | Navigation 图 + Lifecycle 边界 + Manifest + Hilt DI | 38 个(1 P0 + 17 P1 + 20 P2) |
| Task 2 | 测试代码质量 + Fake 实现 + 测试反模式 | 15 个(NF-T1 至 NF-T15) |
| Task 3 | 资源泄漏 + 线程安全 + 类型安全 + 错误处理 + 模块边界 | 29 个(6 P0 + 16 P1 + 7 P2) |
| Task 4 | Compose 副作用 + Accessibility + M3 Expressive 实际使用度 | 24 个(2 P0 + 14 P1 + 10 P2,注:含 2 P0 与其他维度重复) |
| Task 5 | 业务边界 + DataStore 持久化 + 进度持久化 | 38 个(11 P0 + 19 P1 + 8 P2) |

去重后(部分 P0 在多维度交叉出现):**~25 个未识别 P0 + ~80 个 P1 + ~25 个 P2**。

### 新增 P0(~25 项,去重后)

**架构与基础设施(7 项)**:
1. **NF-N1**:BottomBar 在所有子路由仍显示 — Phase 1.K
2. **NF-L1/L2/L3**:7 个 ViewModel 无 SavedStateHandle — Phase 1.L
3. **NF-L5/L6**:种子加载无超时 + 未与 Splash 同步 — Phase 1.J / 1.M
4. **NF-S3**:无 Splash Screen API — Phase 1.M
5. **NF-M8**:res/xml 目录全缺 — Phase 1.N
6. **NF-N8-deep**:knowledge_detail 无界 back stack — Phase 1.K(关联)
7. **NF-H2**:27 测试零 @HiltAndroidTest — Phase 2.J

**数据安全与持久化(8 项)**:
8. **NF-DS1**:双 DataStore 违反单例 — Phase 1.J
9. **NF-DS2/DS3**:isInitialized/markInitialized 无 IO 兜底 — Phase 1.J
10. **NF-DS6**:种子 REPLACE 覆盖用户数据 — Phase 1.J
11. **NF-E1/E2**:解密失败 Flow 永久 failed — Phase 1.O
12. **NF-EE1**:MemoRecordMapper JSON 异常静默重置丢历史 — Phase 1.S
13. **NF-EE3**:Repository 零 try/catch — Phase 2.L / Batch 7
14. **NF-PP4**:复习日志双写不一致 — Phase 2.N / Batch 7
15. **NF-PP5**:错题本未实现 — Phase 2.N / Batch 7
16. **NF-PP6**:AiAssistantViewModel 消息内存态 — Phase 2.N / Batch 7
17. **NF-DS11/E5**:全项目几乎无日志 — Phase 2.L / Batch 7
18. **NF-EE2**:无 sealed 错误类型 — Phase 2.L / Batch 7

**线程安全与类型安全(5 项)**:
19. **NF-R1**:Retrofit 每次重建 — Phase 2.K
20. **NF-C1/C2**:editingId 非线程安全 + 全项目零同步原语 — Phase 1.Q / 2.K
21. **NF-T1**:studyText!! NPE — Phase 1.P
22. **NF-BB3**:PrerequisiteChecker 无环检测 — Phase 1.R
23. **NF-BB9**:Rating.fromValue 越界崩溃 — Phase 2.N / Batch 7

**模块边界(3 项)**:
24. **NF-M1**:core:designsystem → core:database 反向依赖 — Phase 2.K
25. **NF-M2**:core:ai → core:database 反向依赖 — Phase 2.K
26. **NF-MM1**:core:data → core:designsystem 反向依赖 — Phase 2.K

**Accessibility(1 项)**:
27. **NF-UA1**:GraphCanvas 完全无障碍缺失 — Phase 2.M
28. **NF-UT1**:9 Screen 零 smoke test — Phase 2.M

### 新增 P1(~80 项,按维度分组)

- **Navigation/Lifecycle(4 项)**:NF-N2(路由硬编码)/ NF-L7(pointId 不观察)/ NF-M6(configChanges)/ repeatOnLifecycle + BackHandler
- **Hilt/Startup(6 项)**:NF-H8(双 DataStore)/ NF-S1(StrictMode)/ NF-S2(LeakCanary)/ NF-S7(错误上报)/ NF-H1(Configuration.Provider)/ NF-H4(Repository 无接口)
- **资源/线程/类型/模块(18 项)**:NF-C4(combine 无 .catch)/ NF-C5(first 无超时)/ NF-T2-T8(类型安全 7 项)/ NF-E3/E4/E6/E7/E8(错误兜底)/ NF-MM2-MM5(模块边界 4 项)/ NF-C7(currentTimeMillis)
- **错误/日志(8 项)**:NF-E3/E4/AiServiceImpl 归类 / NF-EE4(Snackbar 5 种格式)/ NF-EE5(重试按钮)/ NF-EE6(TAG)/ 错误 UX 一致性
- **Compose/A11y/M3(15 项)**:NF-UC1-UC5(副作用 5 项)/ NF-UA2-UA5(A11y 4 项)/ NF-UM1-UM4(M3 Expressive 4 项)/ NF-UP1-UP5(性能 5 项)/ NF-UT2(渲染测试)
- **业务/DataStore/持久化(19 项)**:NF-DS7-DS10(Key 治理 4 项)/ NF-BB1-BB2-BB5-BB6-BB8-BB10(业务 6 项)/ NF-PP1-PP2(持久化 2 项)/ NF-MM3-deep(死代码)/ DataStore Key 治理 + 持久化范围审计
- **Manifest(7 项)**:NF-M1-M2-M4(配置 3 项)/ NF-C3-C5-C10(资源 3 项)/ NF-M7(label)

### 新增 P2(~25 项)

NF-UC6/UC7/UM5(副作用 3 项)+ NF-BB4/BB11-BB15(业务 6 项)+ NF-DS9/DS12/DS13/PP3/PP7(DataStore 5 项)+ NF-M3/M7(Manifest 2 项)+ 其他细节项

### 结构性扩展

| 部分 | v2 | v3 | 变化 |
|------|----|----|------|
| §0 核心问题 | 8 个 | 18 个 | +10(Q9-Q18) |
| §1 Phase 1 | 9 项 | 18 项 | +9(1.J-1.S) |
| §1 Phase 2 | 8 维度 | 15 维度 | +7(2.I-2.O) |
| §2 汇总表 | 7 个(2.1-2.7) | 13 个(2.1-2.13) | +6(2.8-2.13) |
| Phase 2 审计细节 | 8 个(2.A-2.H) | 15 个(2.A-2.O) | +7(2.I-2.O) |
| Phase 4 测试矩阵 | 18 项 | 25 项 | +7 |
| Phase 5 Batch | 5 个 | 7 个 | +2(Batch 6 架构 + Batch 7 数据安全) |
| 附录 A Checklist | ~50 项 | ~85 项 | +35 |
| 附录 B 并行组 | 8 个 | 14 个 | +6 |
| 附录 C 输出文档 | 15 个 | 29 个 | +14 |
| 测试目标 | ≥ 230 | ≥ 280 | +50 |

### 量化指标对比

| 项 | v1 计划 | v2 计划 | v3 计划 | v2→v3 增量 |
|----|--------|--------|--------|-----------|
| P0 项 | 4 | 9 | ~34 | +25 |
| P1 项 | ~30 | ~53 | ~100 | +47(含交叉) |
| Checklist 项 | ~30 | ~50 | ~85 | +35 |
| emulator 测试矩阵 | 15 | 18 | 25 | +7 |
| 修复 Batch | 4 | 5 | 7 | +2 |
| 输出文档 | 11 | 15 | 29 | +14 |
| 审计维度 | 8 | 8 | 15 | +7 |
| 测试目标 | ~207 | ~230 | ~280 | +50 |

### v3 修订的核心理念

1. **从"能编译"到"能跑对"**:v2 侧重代码级正确性,v3 补齐运行时行为(SavedStateHandle 恢复 / Splash / LeakCanary / 25 项 emulator 矩阵)
2. **从"单点修复"到"体系建立"**:v2 是逐 bug 修,v3 引入 sealed AppError + Timber 日志体系 + 线程安全规范 + DataStore Key Registry 等架构级治理
3. **从"功能完整"到"架构健康"**:v3 补齐模块边界(3 处反向依赖)+ internal 修饰符 + Repository 接口分离
4. **从"能访问"到"无障碍"**:v3 补齐 WCAG 2.1 Accessibility(TalkBack / 触控目标 / 字号 / 语义)
5. **从"能用"到"可维护"**:v3 补齐 res/xml 配置 + dimens.xml + locales_config + themed icon 等基础设施

---

**本计划 v3 制定完成。** 共 5 Phase / **18 P0 修复项** / **15 审计维度** / **25 项实测矩阵** / **7 修复 Batch** / **~85 项 Checklist** / **29 输出文档** / **~280 测试目标**。

下一步建议(按优先级):
1. **立即执行 Phase 1.F + 1.G + 1.I + 1.P + 1.Q + 1.S**(6 个立即可修的 P0,均 < 1 小时)
2. **并行启动 Phase 1.E FSRS 决策**(需手算对比,3 个工作单元)
3. **并行启动 Phase 1.J 种子链路 + 1.O 解密 + 1.R 环检测**(3 个无依赖 P0)
4. **并行启动 Phase 1.L SavedStateHandle + 1.M 基础工具 + 1.N res/xml**(3 个无依赖 P0)
5. **Phase 0 回归复审 + Phase 2 十五维度分组并行**(Group 1-7,已预扫描 90%)
6. **Phase 4 emulator 实测**(独立,需 emulator 环境,25 项矩阵)

是否开始执行?推荐从 Phase 1.F(G/V1 ThemeViewModel 紧急修复)或 Phase 1.P(studyText!! 5 分钟速修)开始,这是最高优先级且立即可修。
