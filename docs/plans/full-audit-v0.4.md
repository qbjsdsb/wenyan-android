# 文研 App 全面深入检查计划 v0.4

> **目标**：对整个项目做 12 个维度的深度审计，识别 P0/P1/P2 问题，按优先级逐项修复，确保项目达到生产可发版（v1.0）标准。
>
> **基准**：main @ `d81e135`（UI 精修 v0.3 完成，190 tests 0 failures）
>
> **执行原则**：
> - 每个 Phase 独立 commit，可单独验证
> - 每个 Phase 结束必须 `assembleDebug` + `testDebugUnitTest` 全绿
> - P0/P1 全部修复，P2 按时间允许择优修复
> - 改动遵循 AGENTS.md CI 验证策略

---

## 0. 检查结论速览（执行前必读）

### P0 — 必须立即修复（3 项）

| # | 问题 | 文件 | 影响 |
|---|------|------|------|
| P0-1 | OkHttp 日志拦截器在生产环境暴露 API Key | [AiModule.kt#L39-L41](file:///workspace/core/ai/src/main/java/com/wenyan/app/core/ai/di/AiModule.kt#L39-L41) | 安全：Authorization 头泄露 |
| P0-2 | release.yml "Verify keystore" 步骤隐藏 Bug | [release.yml#L55-L70](file:///workspace/.github/workflows/release.yml#L55-L70) | CI：secrets 缺失时误报错 |
| P0-3 | `ReviewRepository.getAllVerifiedKnowledgePoints` 死代码 | [ReviewRepository.kt#L54](file:///workspace/core/data/src/main/java/com/wenyan/app/core/data/repository/ReviewRepository.kt#L54) | 可维护性：事实死代码 |

### P1 — 应尽快修复（12 项）

| # | 问题 | 文件 |
|---|------|------|
| P1-1 | core:designsystem 反向依赖 core:database | [designsystem/build.gradle.kts#L36](file:///workspace/core/designsystem/build.gradle.kts#L36) |
| P1-2 | core:data 依赖 core:designsystem | [core/data/build.gradle.kts#L29](file:///workspace/core/data/build.gradle.kts#L29) |
| P1-3 | feature:cards 直接依赖 core:fsrs | [feature/cards/build.gradle.kts#L34](file:///workspace/feature/cards/build.gradle.kts#L34) |
| P1-4 | feature:aiassistant 直接依赖 core:ai | [feature/aiassistant/build.gradle.kts#L34](file:///workspace/feature/aiassistant/build.gradle.kts#L34) |
| P1-5 | fallbackToDestructiveMigration 在生产环境危险 | [DatabaseModule.kt#L67](file:///workspace/core/database/src/main/java/com/wenyan/app/core/database/di/DatabaseModule.kt#L67) |
| P1-6 | AiConversationDao 未使用，对话历史不持久化 | [AiAssistantViewModel.kt](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt) |
| P1-7 | 无网络重试机制 | [AiServiceImpl.kt](file:///workspace/core/ai/src/main/java/com/wenyan/app/core/ai/AiServiceImpl.kt) |
| P1-8 | AMOLED 模式不完整（surfaceBright 未覆盖） | [WenyanTheme.kt#L60-L68](file:///workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/WenyanTheme.kt#L60-L68) |
| P1-9 | feature 模块 Screen 全部缺 @Preview | 9 个 Screen 文件 |
| P1-10 | allowBackup 默认 true | [AndroidManifest.xml#L11](file:///workspace/app/src/main/AndroidManifest.xml#L11) |
| P1-11 | Release 签名 fallback 到 debug 不安全 | [app/build.gradle.kts#L44-L46](file:///workspace/app/build.gradle.kts#L44-L46) |
| P1-12 | ProGuard 规则缺失 | [proguard-rules.pro](file:///workspace/app/proguard-rules.pro) |

### P2 — 可改进（10 项）

| # | 问题 | 文件 |
|---|------|------|
| P2-1 | 多数 ViewModel 缺 error 三态 | 5 个 ViewModel |
| P2-2 | QuizViewModel.expandedQuestionIds 未合并进 uiState | [QuizViewModel.kt#L41-L42](file:///workspace/feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizViewModel.kt#L41-L42) |
| P2-3 | AiAssistantViewModel.messageCounter 是静态可变变量 | [AiAssistantViewModel.kt#L338](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt#L338) |
| P2-4 | SettingsScreen 硬编码种子色 | [SettingsScreen.kt#L167-L171](file:///workspace/feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt#L167-L171) |
| P2-5 | ThemeConfig 默认色与 Color.kt 重复定义 | [ThemeConfig.kt#L20](file:///workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/ThemeConfig.kt#L20) |
| P2-6 | FSRS 核心公式缺直接数值测试 | FsrsWrapperTest.kt |
| P2-7 | feature:quiz / graph / settings 无单元测试 | - |
| P2-8 | NavHost 的 4 个 onNavigateToAiAssistant lambda 重复 | [WenyanNavHost.kt#L51-L93](file:///workspace/app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt#L51-L93) |
| P2-9 | 无深链接支持 | WenyanNavHost.kt |
| P2-10 | KnowledgeScreen.kt 遗留空注释 | [KnowledgeScreen.kt#L198](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt#L198) |

---

## 1. 修复阶段规划（8 个 Phase）

### Phase 1：P0 安全与 CI 修复（3 项，最高优先级）

**目标**：堵住 API Key 泄露 + 修 release.yml bug + 删死代码

#### Task 1.1：HttpLoggingInterceptor 区分 Debug/Release

- 修改 [AiModule.kt#L39-L41](file:///workspace/core/ai/src/main/java/com/wenyan/app/core/ai/di/AiModule.kt#L39-L41)
- 方案：用 `BuildConfig.DEBUG` 条件设置
  - Debug：`Level.BODY`（开发期需要看完整请求）
  - Release：`Level.NONE`（完全静默）
- 同时用 `redactHeader("Authorization")` 在 HEADERS 级别也保护 Authorization
- 在 core:ai 的 build.gradle.kts 启用 `buildConfigField` 或使用 `android.buildFeatures.buildConfig = true`

#### Task 1.2：release.yml "Verify keystore" 步骤防御性修复

- 修改 [release.yml#L55-L70](file:///workspace/.github/workflows/release.yml#L55-L70)
- 方案：把 Verify 步骤加 `if: ${{ env.KEYSTORE_BASE64 != '' }}` 条件，或在步骤内 `[ -f "$KEYSTORE_PATH" ] && keytool ... || echo "keystore not configured, skipping verify"`
- 在 AGENTS.md 第 9 节把 P4 标记为完成

#### Task 1.3：删除 ReviewRepository.getAllVerifiedKnowledgePoints 死代码

- 修改 [ReviewRepository.kt#L54](file:///workspace/core/data/src/main/java/com/wenyan/app/core/data/repository/ReviewRepository.kt#L54)
- 用 `grep -r "getAllVerifiedKnowledgePoints" /workspace` 确认零引用
- 删除函数 + 接口声明 + 实现类 override
- 同步删除对应测试（如有）

#### Task 1.4：验证 + commit

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :app:assembleDebug testDebugUnitTest --no-daemon 2>&1 | tail -10
```

commit message:
```
fix(security+ci): P0 修复 — API Key 日志泄露 + release.yml 防御 + 死代码清理

- AiModule: HttpLoggingInterceptor 按 BuildConfig.DEBUG 分级（BODY/NONE）
- release.yml: Verify keystore 步骤加 if 条件避免 secrets 缺失时误报
- ReviewRepository: 删除 getAllVerifiedKnowledgePoints 死代码（已被 getVerifiedWithSubject 取代）
```

---

### Phase 2：模块依赖解耦（4 项 P1）

**目标**：纠正"feature→core 子模块直连"违反分层原则的问题

#### Task 2.1：core:designsystem 解除对 core:database 的依赖

- 修改 [designsystem/build.gradle.kts#L36](file:///workspace/core/designsystem/build.gradle.kts#L36)，删除 `implementation(project(":core:database"))`
- ContentSourceBadge 改为接受 `String` 或在 designsystem 内重新定义枚举常量（与 database 的 ContentSource 解耦）
- 文件：[ContentSourceBadge.kt](file:///workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/ContentSourceBadge.kt)

#### Task 2.2：core:data 解除对 core:designsystem 的依赖

- 修改 [core/data/build.gradle.kts#L29](file:///workspace/core/data/build.gradle.kts#L29)，删除 `implementation(project(":core:designsystem"))`
- ThemeViewModel 用的 `Color` 类来自 `androidx.compose.ui.graphics.Color`，已在 build.gradle.kts 显式声明 `androidx-compose-ui-graphics`（行 56-57）
- 检查 core:data 是否真的不引用 designsystem 的任何组件

#### Task 2.3：feature:cards 解除对 core:fsrs 的依赖

- 修改 [feature/cards/build.gradle.kts#L34](file:///workspace/feature/cards/build.gradle.kts#L34)，删除 `implementation(project(":core:fsrs"))`
- CardsViewModel 用的 `Rating` 枚举 — 方案：在 feature:cards 内定义 `UiRating` 枚举，在 ViewModel 内映射到 data 层的 `Rating`；或把 `Rating` 提到 core:common
- 推荐：提到 core:common（更简单，Rating 是值类型无依赖）

#### Task 2.4：feature:aiassistant 解除对 core:ai 的依赖

- 修改 [feature/aiassistant/build.gradle.kts#L34](file:///workspace/feature/aiassistant/build.gradle.kts#L34)，删除 `implementation(project(":core:ai"))`
- AiAssistantViewModel 用 `AiMessage` / `ChatRole` 类型 — 方案：在 core:data 暴露 `AiMessage` 作为 DTO（或在 core:common 定义），core:ai 的 AiServiceImpl 内部做映射

#### Task 2.5：验证 + commit

```bash
$JAVA_HOME/bin/java ... assembleDebug testDebugUnitTest --no-daemon
```

commit message:
```
refactor(architecture): P1 模块依赖解耦 — 4 项

- core:designsystem 解除对 core:database 的反向依赖（ContentSourceBadge 改用 String）
- core:data 解除对 core:designsystem 的依赖（Color 来自 androidx.compose.ui.graphics）
- feature:cards 解除对 core:fsrs 的依赖（Rating 提到 core:common）
- feature:aiassistant 解除对 core:ai 的依赖（AiMessage 经 core:data 暴露）
```

---

### Phase 3：数据层健壮性（2 项 P1）

**目标**：修 destructive migration 风险 + 接通 AiConversationDao 持久化

#### Task 3.1：fallbackToDestructiveMigration 改为仅降级时触发

- 修改 [DatabaseModule.kt#L67](file:///workspace/core/database/src/main/java/com/wenyan/app/core/database/di/DatabaseModule.kt#L67)
- 改为 `.fallbackToDestructiveMigrationOnDowngrade()`（仅版本号回退时清空，升级时强制要求 Migration）
- 评估：当前只有 1→2 一个 Migration，未来若加字段必须补 Migration，否则升级时会抛异常 — 这是正确的"严格模式"

#### Task 3.2：AiAssistantViewModel 接通 AiConversationDao 持久化

- 修改 [AiAssistantViewModel.kt](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt)
- 方案：
  - `sendMessage` 成功后异步 `aiConversationDao.insertConversation(...)` + `insertMessage(...)`
  - `init` 块内 `observeAll()` 收集历史 → 合并到 `_uiState.messages`
  - 加 `clearHistory()` 方法调用 `aiConversationDao.clearAll()`
- 新增 AiAssistantViewModelTest 测试：对话持久化 + 清空

#### Task 3.3：验证 + commit

```bash
$JAVA_HOME/bin/java ... assembleDebug testDebugUnitTest --no-daemon
```

commit message:
```
feat(data): P1 数据层健壮性 — Migration 严格化 + AI 对话持久化

- DatabaseModule: fallbackToDestructiveMigration → fallbackToDestructiveMigrationOnDowngrade
- AiAssistantViewModel: 接通 AiConversationDao，对话历史持久化 + 启动时恢复
```

---

### Phase 4：网络层健壮性（1 项 P1）

**目标**：加网络重试机制

#### Task 4.1：AiServiceImpl 加指数退避重试

- 修改 [AiServiceImpl.kt](file:///workspace/core/ai/src/main/java/com/wenyan/app/core/ai/AiServiceImpl.kt)
- 方案：在 `chat()` 内加 retry 循环
  - 最多 3 次重试
  - 指数退避：500ms → 1000ms → 2000ms
  - 仅对 IOException（网络错误）重试，HttpException 4xx 不重试
  - 5xx 重试
- 或在 OkHttp 加 `RetryAndFollowUpInterceptor`（推荐前者，业务层控制更清晰）

#### Task 4.2：Retrofit 实例缓存

- 修改 [AiServiceImpl.kt#L101-L108](file:///workspace/core/ai/src/main/java/com/wenyan/app/core/ai/AiServiceImpl.kt#L101-L108)
- 用 `LruCache<String, LlmApiService>(4)` 按 baseUrl 缓存

#### Task 4.3：验证 + commit

```bash
$JAVA_HOME/bin/java ... assembleDebug testDebugUnitTest --no-daemon
```

commit message:
```
feat(ai): P1 网络健壮性 — 指数退避重试 + Retrofit 缓存

- AiServiceImpl: chat() 加 3 次指数退避重试（IOException/5xx 重试，4xx 不重试）
- AiServiceImpl: LlmApiService 用 LruCache 按 baseUrl 缓存，避免每次反射创建
```

---

### Phase 5：主题与安全加固（3 项 P1）

**目标**：补全 AMOLED + 关 allowBackup + 修 Release 签名 fallback + ProGuard 规则

#### Task 5.1：AMOLED 模式补全 surface 层级

- 修改 [WenyanTheme.kt#L60-L68](file:///workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/WenyanTheme.kt#L60-L68)
- 补全 `surfaceContainerHigh` / `surfaceContainerHighest` / `surfaceBright = Color.Black`
- 评估：补成纯黑会让 AMOLED 模式下所有卡片视觉消失 — 需要权衡
  - 方案 A：全部纯黑（极致省电，但 TonalCard 不可见）
  - 方案 B：surfaceBright 保持深灰（`Color(0xFF111111)`），surfaceContainerHigh 同（卡片仍可见）
- 推荐**方案 B**：surfaceBright = `Color(0xFF111111)`（比 default 更深但可辨识）

#### Task 5.2：AndroidManifest allowBackup 改为 false

- 修改 [AndroidManifest.xml#L11](file:///workspace/app/src/main/AndroidManifest.xml#L11)
- `android:allowBackup="false"` — 防止 adb backup 导出加密数据库

#### Task 5.3：Release 签名 fallback 抛异常

- 修改 [app/build.gradle.kts#L44-L46](file:///workspace/app/build.gradle.kts#L44-L46)
- 方案：release buildType 时若 `storeFile == null` 抛 `GradleException`，仅 debug 允许 fallback

#### Task 5.4：ProGuard 规则补全

- 修改 [proguard-rules.pro](file:///workspace/app/proguard-rules.pro)
- 加 Hilt / Retrofit / kotlinx-serialization / Room 保留规则
- 注意：当前 `isMinifyEnabled = false`，规则补全为未来启用做准备

#### Task 5.5：验证 + commit

```bash
$JAVA_HOME/bin/java ... assembleDebug testDebugUnitTest --no-daemon
```

commit message:
```
fix(theme+security): P1 主题与安全加固 — AMOLED 补全 + allowBackup + 签名 fallback

- WenyanTheme: AMOLED 模式补全 surfaceContainerHigh/High/Bright（深灰 #111111，保留卡片辨识度）
- AndroidManifest: allowBackup=false（防 adb backup 导出）
- app/build.gradle.kts: Release 签名 fallback 抛异常（仅 debug 允许 fallback）
- proguard-rules.pro: 补 Hilt/Retrofit/serialization/Room 保留规则
```

---

### Phase 6：Compose UI 补 Preview（1 项 P1，工作量最大）

**目标**：9 个 Screen 各补 1-2 个 @Preview

#### Task 6.1-6.9：每个 Screen 补 @Preview

每个 Screen 补：
- 1 个 `@Preview(showBackground = true)` 默认态（有数据）
- 1 个空态 Preview
- 1 个加载态 Preview（仅 CardsScreen / KnowledgePointDetailScreen 需要）

需要修改的文件：
- [KnowledgeScreen.kt](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt)
- [QuizScreen.kt](file:///workspace/feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizScreen.kt)
- [CardsScreen.kt](file:///workspace/feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt)
- [GraphScreen.kt](file:///workspace/feature/graph/src/main/java/com/wenyan/app/feature/graph/GraphScreen.kt)
- [AiAssistantScreen.kt](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantScreen.kt)
- [ApiConfigScreen.kt](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/ApiConfigScreen.kt)
- [SettingsScreen.kt](file:///workspace/feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt)
- [KnowledgePointDetailScreen.kt](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt)
- + WenyanApp.kt（主 Scaffold Preview）

#### Task 6.10：验证 + commit

```bash
$JAVA_HOME/bin/java ... assembleDebug testDebugUnitTest --no-daemon
```

commit message:
```
feat(ui): P1 9 Screen 补 @Preview — 三态覆盖（default/empty/loading）

- KnowledgeScreen / QuizScreen / CardsScreen / GraphScreen
- AiAssistantScreen / ApiConfigScreen / SettingsScreen
- KnowledgePointDetailScreen + WenyanApp
- 每个 @Preview 用 dynamicColor=false 避免壁纸依赖
```

---

### Phase 7：P2 可改进项（按时间允许择优）

**目标**：清理小问题，提升代码质量

#### Task 7.1：ViewModel 补 error 三态（5 个 ViewModel）

- KnowledgeViewModel / QuizViewModel / CardsViewModel / GraphViewModel / KnowledgePointDetailViewModel
- 在 UiState data class 加 `val error: String? = null`
- Repository 的 catch 块 emit error

#### Task 7.2：QuizViewModel 合并 expandedQuestionIds 到 uiState

- 修改 [QuizViewModel.kt#L41-L42](file:///workspace/feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizViewModel.kt#L41-L42)
- 把 `_expandedQuestionIds` 合并进 `QuizUiState.expandedQuestionIds: Set<Long>`

#### Task 7.3：AiAssistantViewModel.messageCounter 改为实例字段

- 修改 [AiAssistantViewModel.kt#L338](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt#L338)
- 从 companion object 挪到实例字段

#### Task 7.4：种子色统一到 designsystem

- 新建 [WenyanSeedColors.kt](file:///workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/WenyanSeedColors.kt)
- SettingsScreen 引用 `WenyanSeedColors` 而非硬编码
- ThemeConfig 引用 `DefaultSeedColor` 而非重复定义

#### Task 7.5：FSRS 核心公式直接数值测试

- 修改 FsrsWrapperTest.kt，加：
  - `nextRecallStability` 已知 S/D/R 输入 → 固定 S' 值断言
  - `nextForgetStability` 同上
  - `State.RELEARNING` 4 个分支独立断言

#### Task 7.6：feature:quiz / graph / settings 补 ViewModel 测试

- 至少补 QuizViewModel 年份切换 + GraphViewModel 节点映射测试

#### Task 7.7：NavHost onNavigateToAiAssistant 抽取扩展函数

- 修改 [WenyanNavHost.kt#L51-L93](file:///workspace/app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt#L51-L93)
- 抽取 `NavHostController.navigateToAiAssistant()` 扩展函数

#### Task 7.8：删除 KnowledgeScreen 遗留空注释

- 修改 [KnowledgeScreen.kt#L198](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt#L198)

#### Task 7.9：验证 + commit

按改动量分多个 commit，或合并为一个 P2 commit。

---

### Phase 8：全量验证 + 文档更新 + 交接

#### Task 8.1：全量构建 + 测试

```bash
$JAVA_HOME/bin/java ... :app:assembleDebug testDebugUnitTest --no-daemon
```

预期：190+ tests 0 failures（新增测试可能让总数到 210+）

#### Task 8.2：更新文档

- AGENTS.md 第 7-9 节：v0.4 完成
- 00-STATUS.md：最新 commit + v0.4 改动表
- SESSION_LOG.md：追加第七条
- 03-FAILED-ATTEMPTS.md：补充本次发现的新坑（如有）
- 02-VERSION-MATRIX.md：补充新版本兼容信息（如有）

#### Task 8.3：commit + push

#### Task 8.4：可选 — 发 Release v0.4.0

```bash
gh run list --limit 3
git tag -a v0.4.0 -m "Release v0.4.0 — 全面深入检查与修复"
git push origin v0.4.0
```

---

## 2. 验收标准

### 每个 Phase 必须满足

- [ ] `assembleDebug` BUILD SUCCESSFUL
- [ ] `testDebugUnitTest` 全绿（不破坏现有 190 tests）
- [ ] commit message 说清"为什么改"
- [ ] 改动符合 AGENTS.md 硬约束

### 全部完成的验收

- [ ] P0 3 项全部修复
- [ ] P1 12 项全部修复
- [ ] P2 至少修复 5 项（按时间允许）
- [ ] 测试总数 ≥ 200（Phase 3/4/7 新增测试）
- [ ] AMOLED 模式视觉无断层
- [ ] AI 对话历史可持久化
- [ ] 模块依赖图无反向/越级（用 `./gradlew :app:dependencies` 验证）
- [ ] release.yml 在 secrets 缺失时不报错（dry-run 验证）

---

## 3. 执行顺序与依赖

```
Phase 1 (P0)  ──────┐
                     ├─→ Phase 8 (验证 + 文档)
Phase 2 (架构)  ─────┤
                     │
Phase 3 (数据)  ─────┤
                     │
Phase 4 (网络)  ─────┤
                     │
Phase 5 (主题+安全) ─┤
                     │
Phase 6 (Preview) ───┤
                     │
Phase 7 (P2)  ───────┘
```

- Phase 1 必须最先（P0 安全）
- Phase 2-7 可按需调整顺序
- Phase 8 必须最后

---

## 4. Self-Review

### 4.1 覆盖完整性检查

- [x] 12 个维度全部覆盖（架构/数据/FSRS/ViewModel/UI/导航/网络/主题/测试/CI/安全/文档）
- [x] 每个 P0/P1/P2 都有对应 Phase 任务
- [x] 每个 Task 都有文件引用 + 方案 + 验证步骤
- [x] 验收标准明确（测试数、模块依赖图、AMOLED 视觉）

### 4.2 风险评估

- **Phase 2 模块解耦风险**：可能引入编译错误（如 Rating 提到 core:common 后所有引用点需更新）。缓解：先 grep 所有引用点，逐步迁移。
- **Phase 3 AiConversationDao 接通风险**：可能改变现有 AiAssistantViewModelTest 行为（消息列表从内存变为持久化）。缓解：测试用 FakeAiConversationDao，行为兼容。
- **Phase 5 AMOLED 风险**：方案 B（surfaceBright=#111111）需要在 emulator 实测视觉效果。缓解：先在 Preview 里看，再 emulator 实测。
- **Phase 6 Preview 风险**：补 Preview 可能暴露现有 Screen 的可预览性问题（如硬依赖 ViewModel）。缓解：Preview 用 fake UiState，不依赖 ViewModel。

### 4.3 时间预估（不强制）

- Phase 1：短（3 个独立小修复）
- Phase 2：中（模块解耦需谨慎）
- Phase 3-4：中（新增测试 + 业务逻辑改动）
- Phase 5：短
- Phase 6：长（9 个 Screen × 3 Preview = 27 个 Preview）
- Phase 7：中（按时间允许择优）
- Phase 8：短

---

## 5. 后续规划（v0.4 之后）

### v0.5 候选

- 启用 `isMinifyEnabled = true` + APK 体积优化
- 深链接支持（P2-9）
- Compose 性能 profiling（recomposition 跟踪）
- KMP 跨平台评估（桌面/web）

### v1.0 候选

- OCR 知识提取管线完成 → 替换 stage2-sample → 完整 seed_data.json
- emulator 实测全功能
- Google Play 上架准备
