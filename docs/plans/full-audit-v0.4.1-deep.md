# 文研 App 第二轮深度审计报告 v0.4.1

> **基准**：main @ `d81e135`（UI 精修 v0.3 完成）
>
> **审计范围**：第一轮 12 维度 + 第二轮 4 维度深度（FSRS 算法数值正确性 / ViewModel-Repository 契约 / 依赖版本与安全 / Compose 性能与可访问性）
>
> **审计方法**：4 路并行 search subagent 深度代码审读 + 全局 grep 验证 + 数值手算对比 FSRS-6 官方 spec
>
> **本轮新增发现**：**2 个严重 bug**（FSRS 算法错误）+ **6 项 P1** + **10+ 项 P2**

---

## 0. 关键发现速览（按严重度排序）

### 🔴 P0 严重（必须立即修复）— 第二轮新发现 2 项 + 第一轮 3 项 = 共 5 项

| # | 问题 | 来源 | 文件 | 影响 |
|---|------|------|------|------|
| **P0-NEW-1** | **FSRS-6 nextDifficulty 用错权重索引**（w[5]/w[6] 应为 w[6]/w[7]） | 第二轮 FSRS 审计 | [FsrsWrapper.kt#L318-L322](file:///workspace/core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt#L318-L322) | **算法正确性**：均值回归系数从 0.2197 变 0.5699，过度回归，难度衰减异常 |
| **P0-NEW-2** | **FSRS-6 w[16]=0.2316 作为 easyBonus 直接乘子** | 第二轮 FSRS 审计 | [FsrsWrapper.kt#L57](file:///workspace/core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt#L57) + [#L332](file:///workspace/core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt#L332) | **算法正确性**：EASY 评分稳定性（14.26）低于 GOOD（28.38），语义完全反转 |
| P0-3 | OkHttp 日志拦截器在生产环境暴露 API Key | 第一轮 | [AiModule.kt#L39-L41](file:///workspace/core/ai/src/main/java/com/wenyan/app/core/ai/di/AiModule.kt#L39-L41) | 安全：Authorization 头泄露 |
| P0-4 | release.yml "Verify keystore" 步骤隐藏 Bug | 第一轮 | [release.yml#L55-L70](file:///workspace/.github/workflows/release.yml#L55-L70) | CI：secrets 缺失时误报错 |
| P0-5 | `ReviewRepository.getAllVerifiedKnowledgePoints` 死代码 | 第一轮 | [ReviewRepository.kt#L54](file:///workspace/core/data/src/main/java/com/wenyan/app/core/data/repository/ReviewRepository.kt#L54) | 可维护性 |

### 🟠 P1 高优先级 — 第二轮新增 14 项 + 第一轮 12 项 = 共 26 项

#### 第二轮新增（FSRS 算法）

| # | 问题 | 文件 |
|---|------|------|
| **P1-NEW-1** | EASY 评分 interval 与 stability 不一致（recallS vs recallS*easyBonus） | [FsrsWrapper.kt#L237,L259,L281](file:///workspace/core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt#L237) |
| **P1-NEW-2** | w[17]-w[20]（FSRS-6 短期记忆权重）从未使用 | FsrsWrapper.kt 全文件 |
| **P1-NEW-3** | nextInterval 用 toInt()（截断）而非 round() | [FsrsWrapper.kt#L357](file:///workspace/core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt#L357) |

#### 第二轮新增（ViewModel-Repository 契约）

| # | 问题 | 文件 |
|---|------|------|
| **P1-NEW-4** | CardsViewModel.rateCard 无 try/catch，DB 异常会崩 + 无"牌组完成"状态导致末尾重复评分 | [CardsViewModel.kt#L106-L119](file:///workspace/feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsViewModel.kt#L106-L119) |
| **P1-NEW-5** | ApiConfigViewModel.editingId 裸 var 竞态 → 新建配置不自动 setCurrent + 用户表单被清空 | [ApiConfigViewModel.kt#L48,L134,L154,L157](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/ApiConfigViewModel.kt#L48) |
| **P1-NEW-6** | AiAssistantViewModel 并发请求 isLoading 错乱 | [AiAssistantViewModel.kt#L89,L115-L117](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt#L89) |
| **P1-NEW-7** | ThemeRepositoryImpl 枚举 valueOf 无容错 → 历史/损坏数据击穿 Flow | [ThemeRepositoryImpl.kt#L34-L40](file:///workspace/core/data/src/main/java/com/wenyan/app/core/data/repository/ThemeRepositoryImpl.kt#L34-L40) |
| **P1-NEW-8** | KnowledgeViewModelTest 未实例化 VM → 核心管线零覆盖 | [KnowledgeViewModelTest.kt](file:///workspace/feature/knowledge/src/test/java/com/wenyan/app/feature/knowledge/KnowledgeViewModelTest.kt) |
| **P1-NEW-9** | 5/8 ViewModel 零测试（Cards/ApiConfig/KnowledgePointDetail/Graph/Quiz） | - |

#### 第二轮新增（依赖与安全）

| # | 问题 | 文件 |
|---|------|------|
| **P1-NEW-10** | security-crypto 用 alpha 版本（1.1.0-alpha06）存 API key | [libs.versions.toml#L50](file:///workspace/gradle/libs.versions.toml#L50) |
| **P1-NEW-11** | versionName 未随发版 bump（仍是 "0.1.0"，实际已到 v0.3） | [app/build.gradle.kts#L20](file:///workspace/app/build.gradle.kts#L20) |
| **P1-NEW-12** | retrofit 2.9.0 过旧（2021 年版本） | [libs.versions.toml#L37](file:///workspace/gradle/libs.versions.toml#L37) |
| **P1-NEW-13** | navigation-compose 2.7.7 过旧（2.8.x 有类型安全导航） | [libs.versions.toml#L18](file:///workspace/gradle/libs.versions.toml#L18) |
| **P1-NEW-14** | 全项目 12 个 .pro 规则文件全空（启用 minify 即崩） | app/proguard-rules.pro + 各 consumer-rules.pro |

#### 第二轮新增（Compose 性能与可访问性）

| # | 问题 | 文件 |
|---|------|------|
| **P1-NEW-15** | strings.xml 几乎为空，所有用户可见文本硬编码（i18n + TalkBack 语言切换失效） | 9 个 Screen |
| **P1-NEW-16** | SettingsScreen 版本号硬编码 "v0.1.0"（实际 v0.3） | [SettingsScreen.kt#L240](file:///workspace/feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt#L240) |
| **P1-NEW-17** | Text 普遍缺 maxLines + overflow，长文本撑破布局 | 所有列表项 Screen |
| **P1-NEW-18** | AnimatedVisibility 普遍未指定 WenyanMotion spec | CardsScreen L138/L145, QuizScreen L402, SettingsScreen L146 |
| **P1-NEW-19** | CardsScreen 翻转动画硬编码时长（400ms/300ms）未用 WenyanMotion token | [CardsScreen.kt#L173,L184](file:///workspace/feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt#L173) |
| **P1-NEW-20** | LazyRow/LazyColumn items 缺 key | KnowledgeScreen L135, QuizScreen L165, ApiConfigScreen L370 |
| **P1-NEW-21** | derivedStateOf/remember 缺失导致重复计算 | WenyanApp L43, GraphScreen L148-149, KnowledgePointDetailScreen L67 |
| **P1-NEW-22** | KnowledgePointDetailScreen 用 Column+verticalScroll 而非 LazyColumn | [KnowledgePointDetailScreen.kt#L122-L129](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt#L122) |
| **P1-NEW-23** | LaunchedEffect(errorMessage) 连续相同错误不触发 | AiAssistantScreen L85, ApiConfigScreen L90 |
| **P1-NEW-24** | KnowledgePointDetailScreen scrollState 在 Crossfade 内（状态切换丢位置） | [KnowledgePointDetailScreen.kt#L122](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt#L122) |

#### 第一轮原有 P1（12 项，见 v0.4 计划文件）

P1-1 ~ P1-12（模块依赖解耦 4 项 + destructive migration + AiConversationDao 未接通 + 网络重试 + AMOLED 不完整 + Screen 缺 Preview + allowBackup + 签名 fallback + ProGuard）

### 🟡 P2 可改进 — 第二轮新增 15+ 项

详见下文各维度报告。重点包括：
- FSRS applyFuzz 阈值 15 vs 20 / retrievability 公式 decay=-1 vs -0.5
- 多数 ViewModel stateIn 无 .catch{} 兜底
- messageCounter companion 共享 var
- QuizViewModel._expandedQuestionIds 状态分裂
- 硬编码 dp 散落
- Crossfade targetState 用 Pair 语义弱
- jvmTarget 隐式推断
- 装饰性 Icon contentDescription = "离线"（应 null）
- Text("✓") 应改 Icon

---

## 1. FSRS-6 算法数值正确性深度审计

### 1.1 权重数组 ✓

- 21 个权重（w[0]-w[20]）数量完整
- 数值与 open-spaced-repetition FSRS-6 默认权重一致
- 4 档初始稳定性映射正确（AGAIN→w[0], HARD→w[1], GOOD→w[2], EASY→w[3]）

### 1.2 🔴 严重 bug：nextDifficulty 权重索引错误（F-01）

**文件**：[FsrsWrapper.kt#L318-L322](file:///workspace/core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt#L318-L322)

```kotlin
fun nextDifficulty(d: Float, rating: Rating): Float {
    val dNext = d - w[5] * (rating.value - 3)       // ❌ 应为 w[6]
    val meanReverted = w[6] * dNext + (1f - w[6]) * w[4]  // ❌ 应为 w[7]
    return meanReverted.coerceIn(1f, 10f)
}
```

**官方 FSRS-6 spec**：
- `D' = D - w[6] * (rating - 3)`（难度变化系数）
- `D_next = w[7] * D' + (1 - w[7]) * w[4]`（均值回归）

**数值影响**（以 GOOD 评分后 D=6 为例）：
- 代码：均值回归系数 w[6]=0.5699 → D_next = 0.5699*6 + 0.4301*4.7284 = **5.453**
- 官方：均值回归系数 w[7]=0.2197 → D_next = 0.2197*6 + 0.7803*4.7284 = **5.008**

均值回归从应有的 22% 变成 57%，过度回归。

**修正**：
```kotlin
val dNext = d - w[6] * (rating.value - 3)
val meanReverted = w[7] * dNext + (1f - w[7]) * w[4]
```

### 1.3 🔴 严重 bug：w[16] easyBonus 语义反转（F-02）

**文件**：[FsrsWrapper.kt#L57](file:///workspace/core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt#L57) + [#L332](file:///workspace/core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt#L332)

```kotlin
0.2316f,  // w[16] Easy奖励因子  ← 值 < 1
...
val easyBonusVal = if (rating == Rating.EASY) w[16] else 1f  // 直接用作乘子
```

**数值验证**（D=5, S=10, R=0.9）：
| 评分 | easyBonusVal | growth | S' = S*(1+growth) |
|------|-------------|--------|-------------------|
| GOOD | 1 | 1.838 | **28.38** |
| EASY（代码） | 0.2316 | 0.426 | **14.26** ❌ |
| EASY（修正后 1+w[16]） | 1.2316 | 2.264 | **32.64** ✓ |

EASY 评分的稳定性反而低于 GOOD，"Easy 奖励"变成"Easy 惩罚"。

**修正方案**（二选一）：
```kotlin
// 方案 A：改公式（推荐，保持权重值不变）
val easyBonusVal = if (rating == Rating.EASY) 1f + w[16] else 1f

// 方案 B：改权重值
0.2316f → 1.2316f
```

### 1.4 ⚠️ EASY 评分 interval/stability 不一致（F-03）

**文件**：[FsrsWrapper.kt#L237,L259,L281](file:///workspace/core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt#L237)

```kotlin
Rating.EASY -> {
    val recallS = nextRecallStability(...)
    ScheduleResult(recallS * easyBonus, ..., nextInterval(recallS).toFloat(), ...)
    //         ^^^^^^^^^^^^^^^^^^                  ^^^^^^^^^^^^^^^^^^
    //         存储 stability = recallS*easyBonus  interval 基于 recallS（不含 easyBonus）
}
```

存储 stability=18.54 但只调度 14 天后复习（应 18 天）。

**修正**：
```kotlin
Rating.EASY -> {
    val recallS = nextRecallStability(...) * easyBonus
    ScheduleResult(recallS, ..., nextInterval(recallS).toFloat(), ...)
}
```

### 1.5 ⚠️ w[17]-w[20] FSRS-6 短期记忆权重未使用（F-04）

**文件**：FsrsWrapper.kt 全文件

LEARNING/RELEARNING 状态应使用 w[17]-w[20] 计算短期稳定性，但代码用了 nextForgetStability/nextRecallStability。FSRS-6 核心新特性未实现。

**影响**：当前实现本质是"FSRS-5 公式 + FSRS-6 默认权重值 + 未使用的 w[17-20] 占位"。

**修正**：需实现短期记忆公式，或显式标注"基于 FSRS-5 公式的 FSRS-6 权重适配版"。

### 1.6 ⚠️ nextInterval 截断而非四舍五入（F-05）

**文件**：[FsrsWrapper.kt#L357](file:///workspace/core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt#L357)

```kotlin
return minOf(maxOf(interval.toInt(), 1), maximumInterval)  // toInt() 截断
```

官方用 `round()`。stability=5.5 时，代码得 5 天，官方得 6 天。

**修正**：`interval.roundToInt()`

### 1.7 轻微：applyFuzz 阈值（F-06）

代码在 interval=15 处从 ±1 切换到 ±5%，官方用 `max(1, 0.05*interval)` 在 interval=20 处交叉。15-20 天区间扰动范围偏小。

### 1.8 轻微：retrievability 公式（F-07）

代码用 `(1+t/(9S))^(-1)`（decay=-1），FSRS-6 官方可能用 `(1+19t/(81S))^(-0.5)`（decay=-0.5）。两种公式都满足 R(t=S)=0.9，但曲线形状不同。需与上游确认。

### 1.9 测试覆盖严重不足

| 未覆盖项 | 对应 bug | 风险 |
|---------|---------|------|
| `nextDifficulty` | **F-01** | w[5]/w[6] vs w[6]/w[7] 索引错误不会被捕获 |
| `nextRecallStability` EASY 分支 | **F-02** | EASY < GOOD 不会被捕获 |
| EASY 评分 interval/stability 一致性 | **F-03** | 不会被捕获 |
| 边界值 S=0, D=1, D=10, rating=4 | - | - |
| LEARNING/RELEARNING 状态 4 档 | - | - |

---

## 2. ViewModel-Repository 契约深度审计

### 2.1 全局阴性结论（排除常见问题）✓

- 无 GlobalScope 使用
- 无自建 CoroutineScope
- 无 callbackFlow/channelFlow 未 complete
- 无空 catch 块
- Repository 无可变实例状态
- ViewModel 不持有 View/Context
- 所有 viewModelScope.launch 自动取消

### 2.2 🟠 P1：CardsViewModel.rateCard 无错误处理 + 无牌组完成态

**文件**：[CardsViewModel.kt#L106-L119](file:///workspace/feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsViewModel.kt#L106-L119)

两个问题：
1. `viewModelScope.launch { schedulingRepository.rateCard(...) }` 无 try/catch，Room 抛 SQLiteException 会崩
2. 评完最后一张卡时 `_currentIndex` 变为 `size`，combine 内 `coerceIn(0, size-1)` 钳回 `size-1`，用户再次评分会**重复调度同一知识点**（stability/difficulty 被错误推进）

**修正**：
- 加 try/catch + errorMessage 字段
- 加 `isFinished` 状态，currentIndex >= size 时禁用评分

### 2.3 🟠 P1：ApiConfigViewModel.editingId 裸 var 竞态

**文件**：[ApiConfigViewModel.kt#L48,L134,L154,L157](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/ApiConfigViewModel.kt#L48)

复现场景：
1. 用户点"新建" → editingId=null → 填表 → 点保存
2. saveConfig 在 line 134 读 editingId=null 生成 UUID，进入 viewModelScope.launch（IO 挂起）
3. **挂起期间**用户点另一配置的"编辑" → editingId 改为 "cfgX"
4. 协程恢复，line 154 读 editingId 现为 "cfgX" → 跳过 setCurrent（新建配置未被自动设为当前）
5. line 157 dismissForm() 把 editingId 置 null → **用户刚打开的编辑表单被清空**

**修正**：launch 前捕获局部量 `val wasNew = editingId == null` + `val savedId = id`，launch 内只用局部量。

### 2.4 🟠 P1：AiAssistantViewModel 并发请求 isLoading 错乱

**文件**：[AiAssistantViewModel.kt#L89,L115-L117](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt#L89)

用户快速连发两条消息：
- 请求 A 置 isLoading=true
- 请求 B 置 isLoading=true
- 请求 A 完成在 finally 置 isLoading=false
- 此时请求 B 仍在跑但 UI 显示非加载态

**修正**：用 Mutex/Job 引用串行化，或维护 inFlightCount。

### 2.5 🟠 P1：ThemeRepositoryImpl 枚举 valueOf 无容错

**文件**：[ThemeRepositoryImpl.kt#L34-L40](file:///workspace/core/data/src/main/java/com/wenyan/app/core/data/repository/ThemeRepositoryImpl.kt#L34-L40)

`ColorMode.valueOf(...)` / `WenyanPaletteStyle.valueOf(...)` 在存储值是已废弃枚举名时抛 IllegalArgumentException，沿 Flow 传播导致 Composable 崩溃。

**修正**：`runCatching { valueOf(name) }.getOrDefault(default)`

### 2.6 🟠 P1：测试覆盖严重不足

| ViewModel | 有测试？ | 问题 |
|-----------|---------|------|
| ThemeViewModel | ✓ | 但未覆盖 error 路径 |
| AiAssistantViewModel | ✓ | 较全，但未覆盖并发竞态 |
| KnowledgeViewModel | ⚠️ | **仅测 companion 静态函数，未实例化 VM** |
| CardsViewModel | ❌ | 零测试（仅有 FlipCardLogicTest） |
| ApiConfigViewModel | ❌ | 零测试 |
| KnowledgePointDetailViewModel | ❌ | 零测试 |
| GraphViewModel | ❌ | 零测试（有 FakeGraphRepository 可复用） |
| QuizViewModel | ❌ | 零测试 |

**5/8 ViewModel 零测试**，核心业务逻辑（卡片调度、配置管理、知识点详情、图谱、真题）无单元测试保护。

### 2.7 P2 共性问题

- 多数 ViewModel stateIn Flow 无 `.catch{}` 兜底
- Repository 全部无 catch（"let it bubble"策略，但 ViewModel 也无 catch = 崩溃链）
- 6/8 Repository 未接口化（测试替身困难）
- QuizViewModel._expandedQuestionIds 状态分裂（独立流未合并进 uiState）
- AiAssistantViewModel.messageCounter companion 共享 var
- GraphRepositoryImpl.getRelatedNodes 与 getAdjacentNodes 实现完全相同

---

## 3. 依赖版本与安全深度审计

### 3.1 🔴 核心结构性约束：AGP 8.6.0 反向钉死链

```
AGP 8.6.0 ──┬──► KSP 必须 ≤ 2.3.2（不能用 2.3.10）
            ├──► Hilt 必须 ≤ 2.57.x（2.59+ 需 AGP 9）
            ├──► material3 必须 ≤ alpha18（alpha19+ 需 AGP 9.1.0 + compileSdk 37）
            └──► compileSdk ≤ 35（36 需 AGP 8.9+）
```

**只要保持 AGP 8.6.0，上述 4 个依赖都无法单独升级。** 任何升级必须先把 AGP 升到 8.8+/9.x，连带 compileSdk/KSP/Hilt/material3 一起升。这是"大爆炸"式升级，需整体规划。

### 3.2 🟠 P1：security-crypto 用 alpha 版本存 API key

**文件**：[libs.versions.toml#L50](file:///workspace/gradle/libs.versions.toml#L50)

`androidx.security:security-crypto:1.1.0-alpha06` 是 alpha 版本，用于存储 LLM API key（高敏感）。alpha 软件未经完整安全审计，且 API 可能在后续版本变更。

**建议**：联网核实是否有 1.1.0 stable；评估改用 Android Keystore 直接封装。

### 3.3 🟠 P1：versionName 未随发版 bump

**文件**：[app/build.gradle.kts#L20](file:///workspace/app/build.gradle.kts#L20)

`versionName = "0.1.0"`，但已发 v0.2.0，v0.3 改动待发。**v0.2.0 的 APK 内部仍自报 0.1.0**。

### 3.4 🟠 P1：retrofit 2.9.0 过旧（2021 年版本）

**文件**：[libs.versions.toml#L37](file:///workspace/gradle/libs.versions.toml#L37)

2.11+ 已内置官方 kotlinx.serialization converter，jakewharton 版废弃。

### 3.5 🟠 P1：navigation-compose 2.7.7 过旧

**文件**：[libs.versions.toml#L18](file:///workspace/gradle/libs.versions.toml#L18)

2.8.x 引入类型安全导航（@Serializable 路由）、predictive back 改进。

### 3.6 🟠 P1：全项目 12 个 .pro 规则文件全空

**文件**：app/proguard-rules.pro + 11 个 consumer-rules.pro

每个文件仅 1 行注释，零条 -keep 规则。当前 isMinifyEnabled=false 无碍，但启用即崩（Retrofit/serialization/Hilt/Room 全部需要 keep 规则）。

### 3.7 P2 可择期升级（不碰 AGP 钉死链）

- coroutines 1.8.1 → 1.9.x
- okhttp 4.12.0 → 5.x
- kotlinx-serialization 1.6.3 → 1.7.x
- activity-compose / core-ktx / lifecycle 各升一小版本
- materialKolor 4.1.1 → 5.0.0
- 显式锁定 jvmTarget（当前隐式推断）

---

## 4. Compose 性能与可访问性深度审计

### 4.1 🔴 P0：strings.xml 几乎为空

**文件**：[strings.xml](file:///workspace/app/src/main/res/values/strings.xml) 仅含 `app_name`

9 个 Screen 中**所有**用户可见文本均硬编码在 Kotlin 代码中。TalkBack 可读但无法 i18n（系统语言切换无效）。

### 4.2 🔴 P0：SettingsScreen 版本号硬编码 "v0.1.0"

**文件**：[SettingsScreen.kt#L240](file:///workspace/feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt#L240)

实际项目已到 v0.3，stale 数据 bug。应改 `BuildConfig.VERSION_NAME`。

### 4.3 🟠 P1：Text 普遍缺 maxLines + overflow

所有列表项标题/摘要/消息正文/题目正文等长文本字段都未限制，会撑破布局。重点关注：
- KnowledgeScreen KnowledgePointCard（title/subject/summary）
- KnowledgePointDetailScreen（point.title / content / sourceFile）
- QuizScreen QuestionCard（question.content / material）
- AiAssistantScreen MessageBubble（message.content）
- ApiConfigScreen ConfigCard（displayName / baseUrl / apiKey）

### 4.4 🟠 P1：AnimatedVisibility 普遍未指定 WenyanMotion spec

4 处使用默认 spring：
- CardsScreen L138-139, L145-146
- QuizScreen L402
- SettingsScreen L146-150

应统一 `tween(WenyanMotion.DurationShort/Medium, easing = WenyanMotion.DecelerateEasing/EmphasizedEasing)`。

### 4.5 🟠 P1：CardsScreen 翻转动画硬编码时长

**文件**：[CardsScreen.kt#L173,L184](file:///workspace/feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt#L173)

```kotlin
animateFloatAsState(... animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing))  // 硬编码
animateColorAsState(... animationSpec = tween(durationMillis = 300))  // 硬编码
```

应改 `WenyanMotion.DurationLong` / `DurationMedium` + `WenyanMotion.EmphasizedEasing` / `DecelerateEasing`。

### 4.6 🟠 P1：LazyRow/LazyColumn items 缺 key

- KnowledgeScreen L135 `items(KnowledgeCategory.entries.toList())` 无 key
- QuizScreen L165 `items(years)` 无 key
- ApiConfigScreen L370 `items(LlmProvider.entries.toList())` 无 key
- SettingsScreen LazyColumn 4 个 `item { }` 无 key

### 4.7 🟠 P1：derivedStateOf/remember 缺失

- WenyanApp L43-47 `selectedTopLevelRoute` 线性扫描每次重组
- GraphScreen L148-149 `weakNodes` / `avgRetrievability` 每次重组重算
- KnowledgePointDetailScreen L67-75 `subtitle` 每次重组重算
- SettingsScreen L166-172 `seedColors` 每次重组创建新 List

### 4.8 🟠 P1：KnowledgePointDetailScreen 用 Column+verticalScroll

**文件**：[KnowledgePointDetailScreen.kt#L122-L129](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt#L122)

长内容（多教材对照 + 来源溯源 + 关联/对比/延伸）总 item 数 ≥ 20，全部一次性渲染拖慢首帧。应改 LazyColumn。

### 4.9 🟠 P1：LaunchedEffect(errorMessage) 连续相同错误不触发

**文件**：AiAssistantScreen L85, ApiConfigScreen L90

key 是值，相同字符串不重启 effect。第二次相同错误不弹 Snackbar。

### 4.10 🟠 P1：KnowledgePointDetailScreen scrollState 在 Crossfade 内

**文件**：[KnowledgePointDetailScreen.kt#L122](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt#L122)

状态切换（loading→content）后回到 content 分支会重置 scrollState，用户滚动位置丢失。应提到 Crossfade 外层。

### 4.11 P2 其他发现

- ApiConfigScreen deletingConfig 用 remember 而非 rememberSaveable（旋转丢状态）
- 装饰性 Icon contentDescription = "离线"（应 null）
- Text("✓") 应改 Icons.Default.Check
- 硬编码 dp 散落（48.dp / 20.dp / 4.dp / 0.5.dp / 12.dp / 8.dp / 10.dp）
- Crossfade targetState 用 Pair 语义弱（建议 data class）
- Arrangement.spacedBy(Spacing.xs + Spacing.xs) 应改 Spacing.sm

### 4.12 正向发现 ✓

- WenyanMotion token 在 Crossfade 处统一使用
- Icon contentDescription 装饰性 vs 描述性区分基本正确
- 触控目标默认满足 48dp
- 文字颜色对比度由 M3 主题保证 WCAG AA
- 无 LazyColumn 垂直嵌套垂直
- Spacing token 使用良好

---

## 5. 综合优先级与修复建议

### 5.1 必须立即修复（P0，5 项）

1. **FSRS nextDifficulty 权重索引**（F-01）— 算法正确性
2. **FSRS w[16] easyBonus 语义反转**（F-02）— 算法正确性
3. **HttpLoggingInterceptor 暴露 API Key** — 安全
4. **release.yml Verify keystore bug** — CI
5. **ReviewRepository 死代码** — 可维护性

### 5.2 应尽快修复（P1，26 项）

按类别分组：
- **FSRS 算法**（3 项）：interval/stability 一致性 / w[17-20] 未使用 / nextInterval 截断
- **ViewModel 契约**（6 项）：CardsViewModel 错误处理+完成态 / ApiConfigViewModel 竞态 / AiAssistantViewModel 并发 / ThemeRepositoryImpl 枚举容错 / KnowledgeViewModelTest / 5 VM 零测试
- **依赖安全**（5 项）：security-crypto alpha / versionName / retrofit / navigation / ProGuard 全空
- **Compose UI**（12 项）：strings.xml / 版本号硬编码 / Text maxLines / AnimatedVisibility spec / 翻转动画硬编码 / items 缺 key / derivedStateOf / Column→LazyColumn / LaunchedEffect key / scrollState 位置 + 第一轮 12 项

### 5.3 可改进（P2，25+ 项）

详见各维度报告。

### 5.4 修复路径建议

**第一优先级**（FSRS 算法正确性）：
- 修 F-01 + F-02 + F-03 + F-05
- 补 nextDifficulty / nextRecallStability EASY / interval-stability 一致性测试
- 这是项目核心算法，bug 影响所有用户的复习调度

**第二优先级**（安全与 CI）：
- HttpLoggingInterceptor 区分 Debug/Release
- release.yml keystore 守卫
- 删除死代码

**第三优先级**（ViewModel 契约）：
- CardsViewModel 错误处理 + 完成态
- ApiConfigViewModel 竞态修复
- AiAssistantViewModel 并发串行化
- ThemeRepositoryImpl 枚举容错

**第四优先级**（Compose UI）：
- strings.xml 全量迁移
- Text maxLines + overflow
- AnimatedVisibility spec 统一
- items 加 key + derivedStateOf

**第五优先级**（依赖升级）：
- 短期：versionName bump + security-crypto 评估 + ProGuard 规则预置
- 中期：retrofit 2.11 + navigation 2.8
- 长期：AGP 大爆炸升级

---

## 6. 审计质量自评

### 6.1 覆盖维度

- ✅ 第一轮 12 维度（架构/数据/FSRS/ViewModel/UI/导航/网络/主题/测试/CI/安全/文档）
- ✅ 第二轮 4 维度深度（FSRS 数值正确性 / VM-Repo 契约 / 依赖安全 / Compose 性能可访问性）
- ✅ 数值手算对比 FSRS-6 官方 spec
- ✅ 全局 grep 验证阴性结论（GlobalScope / 空 catch / Repository var）
- ✅ 测试覆盖缺口识别

### 6.2 局限性

- ⚠️ 依赖版本最新与 CVE 查询受限于无 WebSearch 实时能力，所有标 [需联网核实] 的条目需用 Maven Central / OSV / GitHub Advisories 复核
- ⚠️ GraphCanvas / 自定义 Canvas 绘制未深度审计（性能 profiling 需 emulator 实测）
- ⚠️ 资源文件（drawable / dimen）未审计
- ⚠️ 实际运行时性能（recomposition 跟踪）需 Layout Inspector / Compose Compiler Metrics 实测

### 6.3 建议补充审计

1. **emulator 实测**：跑 v0.3 APK，验证 FSRS 调度 + AMOLED 视觉 + 动画流畅度
2. **Compose Compiler Metrics**：`-P plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=...` 生成稳定性报告
3. **CVE 联网复核**：用 OSV 查 okhttp/retrofit/coroutines/security-crypto
4. **Layout Inspector**：检查实际重组次数
5. **TalkBack 实测**：验证可访问性

---

**第二轮深度审计完成。** 共发现 5 个 P0 + 26 个 P1 + 25+ 个 P2 问题。最紧急的是 FSRS-6 算法的 2 个严重数值 bug（F-01 + F-02），直接影响所有用户的复习调度正确性，应立即修复。
