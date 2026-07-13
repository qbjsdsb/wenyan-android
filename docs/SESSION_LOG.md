# 会话日志

> **每次会话结束前追加一节。** 新会话开始时读最后一节恢复进度。

---

## 2026-07-12 完整工作日会话

- **完成**：
  - Phase 1-5 Android 开发全部完成（骨架/FSRS/AI/UI/Release）
  - GitHub Release v0.1.0 发布（签名 APK 14.7 MB）
  - M3 Expressive 改造：27 个 commit 推送，设计规格 + 实现计划（26 Task）完成
  - CI 修复：升级 composeBom 到 2025.12.00、AGP 到 8.6.0
  - 交接方案：创建完整 docs/ 文档体系 + AGENTS.md + tools/ 脚本迁移
- **进行中**：
  - M3 改造 Phase 0（CI 修复）阻塞中
  - OCR 处理约 60%（125/208 文件，PID 20432 运行中）
- **阻塞**：
  - CI 编译失败：materialkolor 4.1.1 与 Kotlin 2.0.20 不兼容
  - 根因：materialkolor 4.1.1 用 Kotlin 2.3.0 编译，元数据版本不匹配
  - 详见 [03-FAILED-ATTEMPTS.md #001](03-FAILED-ATTEMPTS.md)
- **下次继续**：
  - 方案 C Phase 0：修复 CI（升级 Kotlin 到 2.3.0 或降级 materialkolor）
  - 方案 C Phase 1：设计令牌 + 4 个关键组件（药丸导航栏/LargeTopAppBar/分组卡片/层级列表项）
  - 方案 C Phase 2：5 主屏应用
  - 方案 C Phase 3：4 次屏打磨
  - OCR 完成后跑知识提取管线
- **关键发现**：
  - materialkolor 4.1.1 用 Kotlin 2.3.0 编译，与项目 Kotlin 2.0.20 不兼容
  - `source must not be null` 错误实际是 Kotlin 元数据版本不匹配，不是代码问题
  - PKCS12 keystore 要求 storepass = keypass
  - PowerShell 不支持 heredoc
  - Trae 云端模式不保留 AI 记忆，依赖 AGENTS.md + docs/ 恢复上下文
- **commit**：
  - `a6a97af` — 升级 composeBom
  - `77d34e7` — 升级 AGP
  - `684e6a2` — 重写 ContentSourceBadge when 表达式
  - 本次会话：AGENTS.md + docs/ + tools/ 迁移（待 commit）

---

## 2026-07-12 会话：KSU 风格 UI 升级 Phase 0-3

- **完成**：
  - **Phase 0**（commit `0e086ba`）：解除 materialkolor 4.1.1 + Kotlin 2.0.20 元数据阻塞
    - Kotlin 2.0.20 → 2.3.10
    - KSP 2.0.20-1.0.25 → 2.3.2（新版本号格式）
    - Hilt 2.51.1 → 2.57.1（Kotlin 2.3 元数据兼容）
    - Room 2.6.1 → 2.7.0（KSP2 支持）
    - material3 显式锁定 1.5.0-alpha18（覆盖 BOM 1.4.0）
    - 修复 WenyanTheme.kt ColorSpec import 路径 + PaletteStyle.supportsSpec2025 校验
  - **Phase 1**（commit `6bbbb29`）：新增 4 个 KSU 风格组件
    - WenyanLargeTopAppBar（LargeFlexibleTopAppBar 封装，含 @OptIn）
    - WenyanNavigationBar（药丸风格底部导航，用 indicatorColor 参数）
    - GroupedCard + GroupedCardItem（分组卡片）
    - HierarchicalListItem（层级列表项）
    - 为 core:designsystem 模块添加首个 Compose UI 测试（Robolectric + createComposeRule）
    - 搭建 Robolectric 测试基础设施（m2 settings.xml 阿里云镜像 + 预下载 SDK jar）
  - **Phase 2**（commit `a85cc68`）：9 个 Screen 迁移到 WenyanLargeTopAppBar
    - WenyanApp.kt 替换为 WenyanNavigationBar（保留 hierarchy 高亮逻辑）
    - 6 个滚动屏接入 exitUntilCollapsedScrollBehavior + nestedScroll
    - 3 个固定内容屏仅享受 Large 标题样式
    - KnowledgePointDetailScreen 动态 title + subtitle（考频+难度）
    - 修复 6 个文件的 nestedScroll import 路径错误

- **关键发现**：
  - material3 1.5.0-alpha19+ 要求 AGP 9.1.0 + compileSdk 37，与 AGP 8.6.0 不兼容
  - alpha18 中 LargeFlexibleTopAppBar 仍为 @ExperimentalMaterial3ExpressiveApi（非 Stable）
  - MaterialExpressiveTheme 标记为 Material3ExpressiveApi（非 @RequiresOptIn），WenyanTheme 编译无需 OptIn
  - NavigationBarItemDefaults.colors() 参数名从 selectedIndicatorColor 改为 indicatorColor（alpha18）
  - nestedScroll 正确 import 路径：androidx.compose.ui.input.nestedscroll（不是 androidx.compose.input.nestedscroll）
  - Robolectric Maven Resolver 不读 Gradle 配置，需单独 ~/.m2/settings.xml
  - createComposeRule() 需 ComponentActivity 声明（debugImplementation compose-ui-test-manifest）
  - assertIsDisplayed 是顶层扩展函数需 import；assertDoesNotExist 是成员函数不需 import
  - onNodeWithText 只匹配 Text 组件；onNodeWithContentDescription 匹配 Icon contentDescription
  - releaseUnitTest 不含 debugImplementation 依赖，需运行 testDebugUnitTest

- **commit**：
  - `0e086ba` — Phase 0：解除 M3 Expressive 改造阻塞
  - `6bbbb29` — Phase 1：4 个 KSU 组件 + 首个 Compose UI 测试
  - `a85cc68` — Phase 2：9 个 Screen 迁移到 WenyanLargeTopAppBar
  - `c0e2cf1` — Phase 3：文档更新

- **下次继续**：
  - 跑 emulator 实测滚动折叠效果
  - 用 GroupedCard 改造 SettingsScreen
  - 用 HierarchicalListItem 改造 KnowledgePointDetailScreen 关联知识点区域
  - 为 GroupedCard / HierarchicalListItem 写测试
  - OCR 完成后跑知识提取管线

---

## 2026-07-12 会话：CI 修复 + PR 合并

- **完成**：
  - 推送 10 个 commit 到 `trae/agent-cKcjcc` 分支
  - 创建 PR #1 触发 CI
  - 修复 3 个 CI 失败问题，最终 CI run 29211066998 全绿（11/11 步骤成功）
  - 合并 PR #1 到 main（squash merge → `3efe678`）

- **CI 失败修复过程**：
  - **失败 1**：`Plugin [id: 'com.google.devtools.ksp', version: '2.3.2'] was not found`
    - 排查：Aliyun 镜像 metadata 显示 2.3.2 存在，POM HTTP 200 OK，但 CI 找不到
    - 修复 `22b1a7e`：pluginManagement 仓库顺序调整，gradlePluginPortal/mavenCentral/google 移到前面，Aliyun 作 fallback
  - **失败 2**：`Plugin [id: 'org.jetbrains.kotlin.plugin.compose', version: '2.3.10'] was not found`
    - 同上，仓库顺序修复后解决
  - **失败 3**：`java.lang.OutOfMemoryError: Metaspace` 在 `:feature:aiassistant:compileReleaseKotlin`
    - 修复 `dcba036`：MaxMetaspaceSize 512m → 1g（Release 构建 R8 + Kotlin + Compose 需加载大量类）
  - **失败 4**：`java.lang.RuntimeException at RoboMonitoringInstrumentation.java:102` 4 个测试全挂
    - 根因：testReleaseUnitTest 不含 debugImplementation 依赖（ComponentActivity manifest 缺失）
    - 修复 `9e1723d`：CI `gradle test` → `gradle testDebugUnitTest`（release 测试通常跳过）
  - 另有 `64b8894`：CI Gradle 8.7 → 8.14.4 与本地环境对齐

- **关键发现**：
  - Aliyun 镜像从 GitHub Actions runner（美/欧）访问时可能不可达或返回错误响应，plugin marker artifact 解析失败
  - dependencyResolutionManagement（依赖）保持 Aliyun 优先（体积大，加速明显），pluginManagement（插件）改为全局仓库优先
  - Kotlin 编译器 in-process 模式下共享 Gradle daemon 的 metaspace，所有模块编译累积压力，512m 对 Release 构建不足
  - `debugImplementation(libs.androidx.compose.ui.test.manifest)` 只在 debug 变体可用，release 变体测试时 Robolectric 找不到 Activity 声明
  - setup-gradle@v3 的 cache-read-only 模式下 cache restoration 可能失败（400 错误），但 Gradle 仍能正常运行

- **commit**：
  - `22b1a7e` — pluginManagement 仓库顺序调整
  - `64b8894` — CI Gradle 8.7 → 8.14.4
  - `dcba036` — MaxMetaspaceSize 512m → 1g
  - `9e1723d` — test → testDebugUnitTest
  - `3efe678` — PR #1 squash merge 到 main

- **下次继续**：
  - 跑 emulator 实测滚动折叠效果
  - 用 GroupedCard 改造 SettingsScreen
  - 用 HierarchicalListItem 改造 KnowledgePointDetailScreen 关联知识点区域
  - 为 GroupedCard / HierarchicalListItem 写测试
  - OCR 完成后跑知识提取管线

---

## 2026-07-12 会话：交接文档完善

- **完成**：
  - 推送文档更新到 main（commit `4461eba`）
  - 清理已合并的远端 feature 分支 `trae/agent-cKcjcc`
  - 系统性更新交接文档，确保沙箱清空后 AI 可无缝接手

- **文档更新内容**：
  - **AGENTS.md**：
    - 技术栈更新为实际版本（Kotlin 2.3.10 / material3 1.5.0-alpha18 / Hilt 2.57.1 / Room 2.7.0）
    - 第 7 节"当前阻塞"改为"当前状态"（无阻塞）
    - 第 8 节"项目阶段总览"更新 KSU UI 升级为已完成
    - 新增第 9 节"下一步优先级"
    - 新增"CI 相关硬约束"小节（5 条 CI 相关规则）
    - 文档地图新增 ksu-ui-upgrade.md
  - **01-QUICK-RECOVERY.md**：
    - CI 检查命令更新为 python3 解析 JSON 格式
    - 新增"下载 CI 失败日志"命令模板
    - 新增"CI 常见失败原因"快速诊断列表
    - 场景 2 从"M3 改造"改为"KSU 风格 UI 升级后续"
    - 新增"Trae 沙箱环境"小节（路径/JDK/Android SDK/Gradle/JAVA_TOOL_OPTIONS）
    - 会话结束 Step 4 同时给出本地和沙箱两条命令
  - **00-STATUS.md**：已在 `4461eba` 中更新
  - **03-FAILED-ATTEMPTS.md**：已在 `4461eba` 中新增 #010-#012

- **关键交接信息**（新会话必读）：
  - **main 最新 commit**：`4461eba`（文档更新，PR #1 后）
  - **PR #1 squash merge**：`3efe678`（KSU UI 升级 Phase 0-3 全部代码）
  - **CI 状态**：run 29211066998 全绿（PR 分支），main 上 2 个 run 运行中
  - **无阻塞**：可直接开始下一步工作
  - **下一步**：跑 emulator 实测 / GroupedCard 改造 / HierarchicalListItem 改造

- **commit**：
  - `4461eba` — 文档更新（00-STATUS + SESSION_LOG + 03-FAILED-ATTEMPTS）
  - 本次交接：AGENTS.md + 01-QUICK-RECOVERY.md + SESSION_LOG.md（待 commit）

- **下次继续**：
  - 跑 emulator 实测 LargeFlexibleTopAppBar 滚动折叠效果
  - 用 GroupedCard 改造 SettingsScreen
  - 用 HierarchicalListItem 改造 KnowledgePointDetailScreen 关联知识点区域
  - 为 GroupedCard / HierarchicalListItem 写测试
  - OCR 完成后跑知识提取管线

---

## Session 2026-07-13：UI 改造闭环计划（Phase 1-5 全部完成）

### 目标

执行 [docs/plans/ui-closure-plan.md](plans/ui-closure-plan.md) — 把 KSU 风格 UI 改造从"骨架已立"推进到"闭环可用"。

### 完成内容

**Phase 1：GroupedCard 组件增强**（commit `da3f369`）
- 增强 `GroupedCardItem`：新增 `leadingIcon` / `leadingIconContentDescription` / `description` 参数
- 新增 `GroupedCardDivider` 函数（`HorizontalDivider` + outlineVariant + 0.5dp）
- 新增 7 个 Robolectric 测试（GroupedCardTest.kt）覆盖 title/subtitle/description/leadingIcon/trailing

**Phase 2：SettingsScreen 重构**（commit `68e5946`）
- 4 个分组（外观/动态色彩/AI服务/关于）全部从 `SectionHeader` + 手写 Row 迁移到 `GroupedCard` + `GroupedCardItem`
- LazyColumn 添加 `verticalArrangement = Arrangement.spacedBy(Spacing.xl)` 避免卡片粘连
- 删除私有 `SwitchItem` 函数（GroupedCardItem.trailing 已覆盖）

**Phase 3：KnowledgePointDetailScreen 重构**（commit `c918411`）
- `RelatedGroup`（关联/对比/延伸知识点）从 `TonalCard` + 简单 `Text` 重构为 `GroupedCard` + `GroupedCardItem` + `GroupedCardDivider`
- `forEachIndexed` 在项间插入分割线（除最后一项）

**Phase 4：@Preview + 组件测试**（commit `f311a31`）
- 4 个 @Preview 文件（全部 `dynamicColor=false`，三态覆盖 light/dark/AMOLED）：
  - `WenyanLargeTopAppBarPreview`：Light-Simple / Light-WithSubtitle / AMOLED-WithSubtitle
  - `WenyanNavigationBarPreview`：Light / Dark / AMOLED（5 个示例导航项）
  - `GroupedCardPreview`：settings-style / about-style / knowledge-related-style
  - `HierarchicalListItemPreview`：Light-Tree / Dark-WithTrailing / AMOLED-NoOnClick
- 2 个组件测试文件（8 tests 全绿）：
  - `WenyanNavigationBarTest`（3 tests）：labels 显示 / items 有点击行为 / onNavigate 回调
  - `HierarchicalListItemTest`（5 tests）：root/child title / trailing / onClick / 无 trailing 时不显示箭头

**Phase 5：全量验证 + 文档更新**（本次）
- `assembleDebug` BUILD SUCCESSFUL（3m 59s，412 tasks）
- `testDebugUnitTest` BUILD SUCCESSFUL（1m 4s，117 tests 0 failures：designsystem 19 + fsrs 25 + data 52 + aiassistant 21）
- 更新文档：00-STATUS.md、SESSION_LOG.md、plans/ui-closure-plan.md（标记完成）

### 关键技术决策

1. **leadingIconContentDescription 默认 null**（装饰性图标）— 避免 TalkBack 重复朗读 title。仅在图标含义与 title 不同时才需显式设置。
2. **@Preview 全部 `dynamicColor=false`** — 动态色彩依赖系统壁纸，Preview 环境无壁纸会导致渲染异常。
3. **`icons_haveContentDescription_withLabel` 测试失败 → 改为 `items_haveClickAction_forAccessibility`** — Material3 NavigationBarItem 在 `label != null` 时对 icon 应用 `clearAndSetSemantics`，icon 的 contentDescription 节点不可见。正确做法是验证合并语义后 label 节点有 `ClickAction`（供 TalkBack 触发）。
4. **`GroupedCardDivider` 用 `outlineVariant` + 0.5dp** — 与 KSU 视觉规格一致，比 `outline` 更柔和。

### 环境问题与解决（沙箱特有）

- **Gradle 代理**：沙箱有 HTTP 代理 `127.0.0.1:18080`，但 Gradle 不读 `http_proxy` 环境变量。需在 `/root/.gradle/gradle.properties` 配置 `systemProp.http.proxyHost` 等。
- **Robolectric 代理**：Robolectric 的 `MavenArtifactFetcher` 不读 Gradle 的 `systemProp.*`。需在 `/root/.gradle/init.d/proxy.gradle` 用 `jvmArgs('-Dhttp.proxyHost=...')` 注入到 Test 任务。
- **JDK 版本**：mise 默认 `java=25`，但 `gradle` shim 用 mise 默认 JDK。需用 `$JAVA_HOME/bin/java -cp .../gradle-launcher.jar org.gradle.launcher.GradleMain` 直接调用强制 JDK 17。
- **Android SDK**：新沙箱未预装，需用 cmdline-tools 安装 `platform-tools;35.0.0` + `platforms;android-35` + `build-tools;35.0.0`。

### commit 列表

- `da3f369` — Phase 1: GroupedCard 增强 + 7 tests
- `68e5946` — Phase 2: SettingsScreen GroupedCard 重构（4 分组）
- `c918411` — Phase 3: KnowledgePointDetailScreen RelatedGroup 重构
- `f311a31` — Phase 4: 4 @Preview + 2 组件测试（8 tests）
- 本次 — Phase 5: 文档更新（00-STATUS + SESSION_LOG + plan 标记完成）

### 下次继续

- 跑 emulator 实测 LargeFlexibleTopAppBar 滚动折叠效果（P0）
- 可选：用 HierarchicalListItem 改造 KnowledgePointDetailScreen 多教材对照区域
- OCR 完成后跑知识提取管线 → 生成 seed_data.json

---

## Session 2026-07-13（第二条）：UI 统一与死组件清理

### 目标

执行 [docs/plans/ui-consolidation-cleanup.md](plans/ui-consolidation-cleanup.md) — 把 KnowledgePointDetailScreen 的 InfoSection/PerspectiveCard/SourcesSection 统一到 designsystem 组件，并清理 4 个零引用死组件。

### 深度调查发现的关键约束

在制定计划阶段，通过两轮深度调查发现 3 个关键问题，修订了原计划：

1. **AMOLED 嵌套卡片视觉反转**：调查 `WenyanTheme.kt` line 60-68 发现，AMOLED 模式覆盖了 `surfaceContainerLow = Color.Black`，但**未覆盖 `surfaceBright`**。若在 GroupedCard（surfaceBright）内嵌套 TonalCardLow（surfaceContainerLow），会形成"深灰卡套纯黑卡"的视觉反转。**结论**：MultiPerspectiveSection 保留 InfoSection 无容器模式，避免嵌套。

2. **padding 一致性**：GroupedCardItem 的水平 padding 是 `Spacing.lg`（16dp）。GroupedCard 内的所有内容必须用 `horizontal = Spacing.lg` 保持左边缘对齐。原计划摘要 Text 用 `Spacing.md`（12dp）会导致 4dp 不对齐。**结论**：统一为 `horizontal=lg, vertical=md`。

3. **HierarchicalListItem API 不匹配**：原 AGENTS.md P1 计划"用 HierarchicalListItem 改造多教材对照"——经源码核实，该组件 API 只有 `title + trailing`，无法承载教材正文段落（多行长文本），且多教材对照是扁平列表非树形层级。**结论**：删除该死组件，修订 P1 计划。

### 完成内容

**Phase 1：KnowledgePointDetailScreen 统一**（commit `ebad848`）
- 摘要 `InfoSection` → `GroupedCard`（纯文本，无嵌套风险，padding `horizontal=lg, vertical=md`）
- 资料来源 `InfoSection` → `GroupedCard` + `HorizontalDivider` → `GroupedCardDivider`
- `SourceRow` 加 `padding(horizontal=lg, vertical=md)` 与 GroupedCardItem 对齐
- `PerspectiveCard` 非 official 分支 → `TonalCardLow`（走 designsystem，独立卡片不嵌套）
- 多教材对照**保留 InfoSection**（避免 AMOLED 嵌套卡片视觉反转），加 KDoc 注释说明原因
- 清理不再使用的 imports（`HorizontalDivider`、`dp`）

**Phase 2：删除 4 个死组件**（commit `2f83ac3`）
- 删除 `WenyanTopAppBar`（KSU 升级后 9/9 Screen 用 WenyanLargeTopAppBar，0 引用）
- 删除 `SectionHeader`（GroupedCard 标题区已覆盖，0 引用）
- 删除 `LoadingState`（9 个 Screen 都手写 Box{CircularProgressIndicator()}，0 引用）
- 删除 `HierarchicalListItem`（API 只有 title+trailing，不匹配任何现有列表，0 生产引用）
  + 同步删除 `HierarchicalListItemPreview`（3 个 @Preview）
  + 同步删除 `HierarchicalListItemTest`（5 个测试）
- 更新 `WenyanLargeTopAppBar.kt` 注释：删除对 WenyanTopAppBar 的 2 处引用

**Phase 3：全量验证 + 文档更新**（本次）
- `assembleDebug` BUILD SUCCESSFUL（3m 59s，412 tasks）
- `testDebugUnitTest` BUILD SUCCESSFUL（174 tests 0 failures：designsystem 14 + data 52 + fsrs 25 + ai 62 + aiassistant 21）
- 更新文档：00-STATUS.md、SESSION_LOG.md、AGENTS.md、01-QUICK-RECOVERY.md、plans/ui-consolidation-cleanup.md

### 关键技术决策

1. **MultiPerspectiveSection 保留 InfoSection** — AMOLED 模式下 `surfaceContainerLow` 被覆盖为 Black 而 `surfaceBright` 未覆盖，GroupedCard 套 TonalCardLow 会形成视觉反转。加 KDoc 注释说明保留原因，避免后续误删。
2. **PerspectiveCard 分 isOfficial 两分支** — official 保留 `Surface(primaryContainer)`（designsystem 无 primaryContainer 变体），非 official 用 `TonalCardLow`（color/shape 完全一致）。
3. **删除 HierarchicalListItem 而非扩展 API** — 经调查证实无任何现有列表适合用该组件（所有列表都有多字段元信息，title+trailing 无法承载）。扩展 API 会增加复杂度但无实际收益，YAGNI。

### 环境问题

- **沙箱重置导致环境丢失**：会话中途沙箱被重置，`/root/.gradle/gradle.properties`、`/root/.gradle/init.d/proxy.gradle`、`/opt/android-sdk`、`/workspace/local.properties` 全部丢失。重新创建代理配置 + 重装 Android SDK（cmdline-tools + platform-tools + platforms;android-35 + build-tools;35.0.0）后恢复。

### commit 列表

- `ebad848` — Phase 1: KnowledgePointDetailScreen 摘要+资料来源统一到 GroupedCard
- `2f83ac3` — Phase 2: 删除 4 个零引用死组件
- 本次 — Phase 3: 文档更新

### 下次继续

- 跑 emulator 实测 LargeFlexibleTopAppBar 滚动折叠效果（P0）
- OCR 完成后跑知识提取管线 → 生成 seed_data.json（P1）
- 可选：用 GroupedCard 改造其他 Screen（如 ApiConfigScreen，但需先扩展 GroupedCardItem API）

### 新会话快速恢复 Checklist

新沙箱会话开始时，按以下顺序操作（5 分钟内进入工作状态）：

1. **读 [AGENTS.md](../AGENTS.md)** — 项目入口，了解技术栈、硬约束、当前状态
2. **读 [00-STATUS.md](00-STATUS.md)** — 10 秒了解当前状态（无阻塞，CI 全绿）
3. **读本文档最后一节** — 上次进度（本次会话）
4. **拉取最新代码**：
   ```bash
   cd /workspace && git pull origin main
   ```
5. **配置 Gradle 代理**（沙箱特有，新沙箱必做）：
   ```bash
   # /root/.gradle/gradle.properties
   cat > /root/.gradle/gradle.properties <<'EOF'
   systemProp.http.proxyHost=127.0.0.1
   systemProp.http.proxyPort=18080
   systemProp.https.proxyHost=127.0.0.1
   systemProp.https.proxyPort=18080
   systemProp.http.nonProxyHosts=localhost|127.0.0.1
   EOF

   # /root/.gradle/init.d/proxy.gradle（Robolectric 测试需要）
   mkdir -p /root/.gradle/init.d
   cat > /root/.gradle/init.d/proxy.gradle <<'EOF'
   allprojects {
       tasks.withType(Test).configureEach {
           jvmArgs('-Dhttp.proxyHost=127.0.0.1','-Dhttp.proxyPort=18080',
                   '-Dhttps.proxyHost=127.0.0.1','-Dhttps.proxyPort=18080',
                   '-Dhttp.nonProxyHosts=localhost|127.0.0.1')
       }
   }
   EOF
   ```
6. **配置环境变量**：
   ```bash
   export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2
   export ANDROID_HOME=/opt/android-sdk
   export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport"
   export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
   ```
7. **验证构建**（注意：不能用 `gradle` shim，它用 mise 默认 JDK 25 与 AGP 8.6.0 不兼容）：
   ```bash
   $JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :app:assembleDebug --no-daemon 2>&1 | tail -5
   ```
8. **开始工作**：根据 [00-STATUS.md](00-STATUS.md) 的"下一步优先级"选择任务
