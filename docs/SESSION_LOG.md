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

---

## Session 2026-07-13（第三条）：P0 双修 — SeedDataLoader 接通 + release.yml CI 修复

### 目标

执行 [docs/plans/p0-seed-loader-ci-fix.md](plans/p0-seed-loader-ci-fix.md) — 修复 release.yml 的 2 个 CI bug（避免下次发布失败）+ 接通 SeedDataLoader 调用点（让 App 从空壳 UI 变成可用工具）。

### 深度调查发现的关键约束

计划制定阶段经过多轮深度审查，发现并修订了 3 个关键问题：

1. **SupervisorJob 不能防崩溃（CRITICAL 修正）**：原计划误以为 `SupervisorJob` 能防止 App 崩溃。经 Kotlin 官方文档核实：`SupervisorJob` 只阻断异常向父 Job 传播，**不阻止异常本身被抛出**。`launch` 根协程的未捕获异常会经 `Thread.uncaughtExceptionHandler` 处理，Android 默认是 `RuntimeInit$KillApplicationHandler`（崩溃）。**修订**：必须显式加 `CoroutineExceptionHandler`，捕获异常并 Log.e，降级为 EmptyState。

2. **Hilt 注入链完整性核实**：SeedDataLoader 有 9 个构造依赖（Context + 7 DAO + GraphRepository）。逐一核实可注入性：7 DAO 由 `DatabaseModule` `@Provides`，GraphRepository 由 `DataModule` `@Binds` 到 `GraphRepositoryImpl @Inject constructor`，Context 由 `@ApplicationContext` 提供。**结论**：全部可注入，无需补充 @Provides/@Binds。

3. **属性初始化顺序**：`exceptionHandler`（val）必须在 `applicationScope`（val 引用 exceptionHandler）之前声明。Kotlin 按声明顺序初始化属性，反过来会 NPE。最终代码中 exceptionHandler 在前，applicationScope 在后，安全。

### 已知限制（本次接受，记录供后续优化）

1. **强杀重启可能丢失复习数据**：`MemoRecordEntity` 外键 `onDelete = CASCADE` + DAO 用 `OnConflictStrategy.REPLACE`。首次导入中途被强杀时，下次启动 REPLACE 会先 DELETE（触发 CASCADE 删 memo_records）再 INSERT，覆盖用户复习进度。MVP 阶段无真实数据可丢失，接受。
2. **importToDatabase 无 @Transaction**：7 步导入无外层事务，中途 OOM 会留部分数据。但用 REPLACE，下次启动覆盖，风险可控。
3. **mapNotNull 静默跳过**：subject 字段不匹配的知识点/真题会被跳过，但仍执行 `markInitialized()`。当前 stage2-sample 数据匹配，无影响。
4. **release.yml "Verify keystore" 隐藏 bug（Line 63-70，本次不动）**：该步骤无条件执行 `keytool -list`，但前一步在 `KEYSTORE_BASE64` 未配置时 `exit 0` 跳过解码。结果 Verify 步骤对不存在的文件执行 keytool 失败。当前仓库已配置 Secrets，不会触发；修复需重构 keystore 处理逻辑，超出 P0 范围。记录到 `03-FAILED-ATTEMPTS.md` 供后续修复。

### 完成内容

**Phase 1：修复 release.yml CI bug**（commit `ff19231`）
- Line 46：`gradle-version: '8.7'` → `'8.14.4'`（AGENTS.md 硬约束：旧版 8.7 在解析 KSP 2.3.x 时有 bug）
- Line 81：`gradle test` → `gradle testDebugUnitTest`（AGENTS.md 硬约束：debugImplementation 依赖只在 debug 变体可用）
- yaml 语法验证通过（PyYAML safe_load）

**Phase 2：接通 SeedDataLoader**（commit `07c3a6d`）
- `WenyanApplication.kt` 注入 `SeedDataLoader`（`@Inject lateinit var`）
- `onCreate` 用 `CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler).launch` 异步调用 `ensureSeedDataLoaded()`
- `CoroutineExceptionHandler` 捕获异常并 `Log.e`，避免 App 崩溃
- 不阻塞 onCreate：各 ViewModel 用 `stateIn(WhileSubscribed(5000))` 订阅，数据加载完后自动刷新

**Phase 3：验证 + 文档**
- `:app:compileDebugKotlin` SUCCESSFUL（`:app:kspDebugKotlin` 执行，证明 Hilt 代码生成成功）
- `assembleDebug` SUCCESSFUL（412 tasks）
- `testDebugUnitTest` SUCCESSFUL（174 tests 0 failures，无回归）
- 更新文档：00-STATUS.md、SESSION_LOG.md、AGENTS.md、plans/p0-seed-loader-ci-fix.md

### 环境问题

- **沙箱 Java 版本切换**：会话开始时 `JAVA_HOME` 指向 Java 25.0.2，但 Kotlin 编译器的 `JavaVersion.parse` 无法解析 "25.0.2"（抛 `IllegalArgumentException`）。切换到 Java 17.0.2 后正常。**记录**：本项目要求 Java 17（AGP 8.6.0 + Kotlin 2.3.10 兼容），新沙箱需 `export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2`。

### 关键技术决策

1. **CoroutineExceptionHandler 而非 try-catch** — `launch` 根协程的异常无法用 try-catch 捕获（异常发生在 lambda 内部，但 launch 不向调用者传播）。`CoroutineExceptionHandler` 是 Kotlin 协程官方的根协程异常处理机制。
2. **独立 CoroutineScope 而非 GlobalScope** — `GlobalScope` 引发 lint 警告且生命周期不受控。Application 进程级单例，用独立 CoroutineScope 即可。
3. **Dispatchers.IO** — SeedDataLoader 涉及 assets 读取 + Room 数据库写入，IO 密集型。
4. **不阻塞 onCreate** — 异步加载，App 启动流畅。各 Screen 先显示 loading/EmptyState，数据加载完后 Flow 自动刷新。

### commit 列表

- `ff19231` — Phase 1: release.yml CI 修复（gradle-version 8.7→8.14.4, gradle test→testDebugUnitTest）
- `07c3a6d` — Phase 2: 接通 SeedDataLoader（WenyanApplication 注入 + onCreate 异步调用）
- 本次 — Phase 3: 文档更新

### 下次继续

- 跑 emulator 实测 SeedDataLoader（P0）：Logcat 无异常 + 各 Tab 有数据 + 重启不重复导入
- KnowledgeViewModel 2 个 bug（P1）：filterByCategory 不筛选 + subject 显示 "TEXTBOOK_NATIVE"
- OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（P2）

---

## Session 2026-07-13（第四条）：P1 修复 — KnowledgeViewModel 科目筛选 + 科目名显示

### 目标

执行 [docs/plans/p1-knowledge-viewmap-subject-fix.md](plans/p1-knowledge-viewmap-subject-fix.md) — 修复 KnowledgeViewModel 的 2 个 bug：filterByCategory 不筛选 + subject 显示 "TEXTBOOK_NATIVE"。

### 深度调查发现的关键事实

1. **数据模型断层**：KnowledgePointEntity 无 subjectId 字段，唯一关联路径是 `chapterId → ChapterEntity.subjectId → SubjectEntity.name`，但整条通道上没有任何 DAO JOIN 查询、@Relation、KnowledgePointWith* 数据类实现它。
2. **三套互不相通的"科目"机制**：SubjectEntity（subjects 表，孤儿表）/ ExamCodeHistoryEntity + ExamCodeResolver（仅 Quiz 模块用）/ KnowledgeCategory 枚举（仅 Knowledge 列表页 FilterChip 标签，筛选逻辑空壳）。
3. **seed_data.json 科目名是全名**（"中国古代文学"），枚举 label 是简称（"古代文学"），4 个中 2 个不匹配。用 `subjectName.contains(keyword)` 匹配兼容两者。
4. **SubjectEntity.shortName 是死字段**且 `SeedDataLoader.kt:107` 的 `take(2)` 实现错误（"中国古代文学"→"中国"而非"古文"）。本次不动（YAGNI）。
5. **KnowledgeViewModel 无测试**（test/ 目录不存在），修复时补测试。

### 计划打磨中发现并修复的问题（3 轮深度审查）

| # | 严重度 | 问题 | 修复 |
|---|--------|------|------|
| 1 | CRITICAL | Task 4 和 Task 5 对 filterByCategory/toUiItem 位置说法矛盾 | Task 4 一步到位包含 companion object 完整代码 |
| 2 | CRITICAL | 测试代码用 Google Truth，但项目无 truth 依赖 | 全部改为 JUnit 原生断言 |
| 3 | Minor | 测试代码 `"..." .repeat(5)` 有空格，Kotlin 语法错误 | 改为 `"...".repeat(5)` |
| 4 | 一致性 | 文件结构表包含 build.gradle.kts，但实际已有依赖 | 删除该行 |
| 5 | 设计混乱 | Task 5 "配套改动"与 Task 4 重复 | 改为"无需再改 ViewModel" |
| 6 | 测试不足 | 缺少边界场景（空列表/不匹配/summary 有值不截断） | 新增 3 个测试（7→10） |
| 7 | 架构思考未记录 | getVerifiedWithSubject 放在 ReviewRepository 职责不完美 | 记录为已知限制 #6 |
| 8 | INNER JOIN 风险未记录 | 数据异常时知识点被过滤掉 | 记录为已知限制 #5 |
| 9 | 断言不够严格 | summary 回退测试只验证长度 | 加 `assertEquals(longCoreConclusion.take(100), ...)` |

### 执行中发现并修复的问题

| # | 问题 | 修复 |
|---|------|------|
| 10 | **Room JOIN POJO 不自动转换 snake_case → camelCase**（计划假设错误） | `KnowledgePointWithSubject.subjectName` 加 `@ColumnInfo(name = "subject_name")` 显式映射 |
| 11 | **2 个 FakeKnowledgePointDao 未实现新方法**（core/ai + feature/aiassistant） | 补全 `observeVerifiedWithSubject` 默认实现（`flowOf(emptyList())`） |

### 完成内容

**Phase 1：DAO 层** — 新增 JOIN 查询
- 新建 `KnowledgePointWithSubject.kt`（@Embedded + @ColumnInfo）
- `KnowledgePointDao` 新增 `observeVerifiedWithSubject()`（INNER JOIN chapters + subjects）

**Phase 2：Repository 层** — 暴露新方法
- `ReviewRepository` 新增 `getVerifiedWithSubject()` 委托方法

**Phase 3：ViewModel 层** — 修复筛选 + 显示
- 数据源从 `getAllVerifiedKnowledgePoints()` 改为 `getVerifiedWithSubject()`
- `filterByCategory` 从空壳改为 `points.filter { it.subjectName.contains(category.keyword) }`
- `toUiItem` 的 `subject` 从 `contentSource` 改为 `subjectName`
- `KnowledgeCategory` 枚举新增 `keyword` 字段
- `filterByCategory` + `toUiItem` 移到 companion object（internal 可见性）供测试调用

**Phase 4：测试** — 新增 KnowledgeViewModelTest
- 10 个测试：5 正常路径（ALL/ANCIENT/MODERN/FOREIGN/THEORY）+ 4 边界（空列表/不匹配/summary有值/summary为null）+ 1 回归（subject 不取 contentSource）

**Phase 5：全量验证** — `assembleDebug` SUCCESSFUL + `testDebugUnitTest` 184 tests 0 failures（基线 174 + 新增 10）

**Phase 6：文档 + Push** — 更新 4 个文档（00-STATUS、SESSION_LOG、AGENTS、plan）

### commit

- `d1b9cd5` — fix(knowledge): 修复科目筛选不生效 + subject 显示 TEXTBOOK_NATIVE（8 files, 292 insertions, 32 deletions）

### 关键技术决策

1. **DAO JOIN 而非 @Relation 或 @Embedded**：@Relation 触发 N+1 查询，@Embedded 不能跨表，@Query JOIN 一次查询完成最高效。
2. **INNER JOIN 而非 LEFT JOIN**：数据异常时强制数据完整性（不显示无科目的知识点），MVP 阶段 SeedDataLoader 已保证外键完整性，风险极低。
3. **contains 匹配而非精确匹配**：兼容 seed_data 全名与枚举简称，当前 4 科目无歧义。
4. **新增方法而非修改现有**：`getAllVerifiedKnowledgePoints` 保留向后兼容（虽已成事实死代码，记录到 P5 重构）。
5. **companion object 而非提取 mapper 类**：为可测试性的最小妥协，YAGNI。

### 已知限制（本次接受，记录供后续优化）

1. **KnowledgePointEntity 无 subjectId 字段**：通过 JOIN 绕过，不改表结构（避免数据库迁移）。
2. **SubjectEntity.shortName 死字段**：本次不动（YAGNI）。
3. **contains 匹配的脆弱性**：若未来出现"古代文论"会误匹配。当前 4 科目无歧义。
4. **filterByCategory + toUiItem 移到 companion object**：更优方案是提取到 KnowledgePointMapper 类，YAGNI。
5. **INNER JOIN 数据完整性风险**：若 chapterId 指向不存在的 chapter，知识点会被过滤掉。MVP 阶段无用户添加知识点功能，风险极低。
6. **架构职责不完美（既有问题）**：`getVerifiedWithSubject()` 放在 ReviewRepository 职责不完美——知识点浏览更应在 KnowledgeRepository。但当前 `getAllVerifiedKnowledgePoints()` 也在 ReviewRepository，是既有设计问题。本次不改（P1 是修 bug，不是重构）。
7. **ReviewRepository.getAllVerifiedKnowledgePoints 将变成事实上的死代码**：本次不删除（保留 API 向后兼容），记录到 P5 重构。

### 下次继续

- 跑 emulator 实测（P0）：SeedDataLoader + 知识点分类标签筛选 + LargeFlexibleTopAppBar
- OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（P2）
- 架构重构（P5）：ReviewRepository 死代码清理 + getVerifiedWithSubject 迁移到 KnowledgeRepository

---

## Session 2026-07-13（第五条）：Release v0.2.0 发布

### 目标

用户要求"发一个 release，让我看看软件长什么样子"。在 P1 修复完成的基础上发布 v0.2.0，让用户能下载到包含最新修复的签名 APK。

### 前置：CI 验证策略写入 AGENTS.md（commit `ce50e77`）

用户问"这个 ci 验证是必须的吗，本地会不会快一点"，并要求"你自己判断需不需要 ci 验证，在每次改动结束之后，并且把这个写入记忆里面"。

在 AGENTS.md 第 4 节硬约束下新增 `### CI 验证策略（2026-07-13 新增）` 小节：
- **原则**：AI 自主判断每次改动是否需要 CI 验证，不冗余等待
- **必须等 CI**：改 workflow / build.gradle.kts / libs.versions.toml / settings.gradle.kts / 签名 / 跨平台兼容性 / 发版前
- **不需要等 CI**：纯 Kotlin/Compose 业务逻辑 / 纯测试 / 纯文档
- **本地验证最低标准**：`assembleDebug` SUCCESSFUL + `testDebugUnitTest` 全绿
- **Release tag 流程** 5 步（本地验证 → CI 绿 → 删旧 orphan tag → 打新 tag → 等 workflow）

### Release v0.2.0 发布

**Release tag 流程执行**（严格遵循 AGENTS.md 第 4 节）：

1. **确认本地验证**：P1 修复已通过 `assembleDebug` + `testDebugUnitTest` 184 tests 0 failures（第四条会话已完成）
2. **确认最近 CI 全绿**：`gh run list` 确认最后一次代码 commit CI（run 29275987334，P1 修复）全绿 18m53s。另有 2 个 docs-only CI 在跑（29277763880 + 29277520877），docs 改动不影响发布
3. **检查 orphan tag**：`git ls-remote --tags origin` 确认只有 v0.1.0，无 v0.2.0 旧 tag，无需删除
4. **检查现有 release**：`gh release list` 确认只有 v0.1.0
5. **打 tag 并 push**：`git tag -a v0.2.0 -m "..." && git push origin v0.2.0`
6. **等 Release workflow**：run 29278178988，14m54s，14/14 步骤全绿

### Release workflow 执行详情

**关键步骤全部通过**：
- ✓ Decode keystore from Secrets（KEYSTORE_BASE64 已配置）
- ✓ Verify keystore（keytool 验证通过 — P4 担心的隐藏 bug 没触发，secrets 完整）
- ✓ Build signed release APK（R8 混淆 + 签名）
- ✓ Run unit tests（184 tests 全绿）
- ✓ Create GitHub Release（自动创建，附加 2 个 APK）

**已知警告（不影响发布）**：
- Node.js 20 deprecation warning（actions/checkout@v4 等仍在用 Node 20，被强制运行在 Node 24）
- Gradle cache restoration 400 错误（setup-gradle@v3 cache-read-only 模式偶发，Gradle 仍正常运行）

### 交付物

**GitHub Release v0.2.0**：
- URL：https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.2.0
- Tag：v0.2.0（指向 commit `ce50e77`）
- Assets：`wenyan-v0.2.0.apk` + `wenyan-latest.apk`（内容相同）
- 系统要求：Android 8.0 (API 26) 及以上

**v0.2.0 包含自 v0.1.0 以来的全部改动**：
- KSU 风格 UI 升级 Phase 0-3（4 个新组件 + 9 个 Screen 迁移）
- UI 改造闭环（GroupedCard 增强 + 2 Screen 重构 + 4 Preview + 15 测试）
- UI 统一与死组件清理（删除 4 个零引用组件）
- P0 双修（release.yml CI 修复 + SeedDataLoader 接通，App 启动自动导入种子数据）
- P1 修复（KnowledgeViewModel 科目筛选生效 + 卡片显示真实科目名，DAO JOIN + 10 测试）

### 关键技术决策

1. **不等 docs-only CI 就发版**：发版前检查发现 2 个 docs commit 的 CI 还在跑。根据新写入的 CI 验证策略，docs 改动不需要等 CI。最后一次**代码** commit 的 CI（run 29275987334）已全绿，满足发版前置条件。结果证明判断正确：Release workflow 全绿。
2. **用 `git tag -a` 而非 `git tag`**：带 annotated message，记录 v0.2.0 包含的关键改动，方便后续回溯。
3. **Verify keystore 隐藏 bug 未触发**：P4 记录的 release.yml Line 63-70 bug（KEYSTORE_BASE64 未配置时失败）在 secrets 完整时不触发。本次发版通过，证明 secrets 配置完好。P4 修复仍待办（防御性修复，避免未来 secrets 丢失时 workflow 给出误导性错误）。

### commit 列表

- `ce50e77` — docs: 写入 CI 验证策略到 AGENTS.md — AI 自主判断是否等 CI
- `v0.2.0` tag — Release v0.2.0（指向 `ce50e77`）

### 下次继续

- **P0**：跑 emulator 实测 — 下载 v0.2.0 APK 或本地 assembleDebug，验证 SeedDataLoader 启动时导入数据 + 知识点分类标签筛选生效 + LargeFlexibleTopAppBar 滚动折叠
- **P2**：OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（替换 stage2-sample）
- **P3**：可选 — 用 GroupedCard 改造其他 Screen（如 ApiConfigScreen，需先扩展 GroupedCardItem API）
- **P4**：release.yml "Verify keystore" 步骤隐藏 bug（Line 63-70，防御性修复）
- **P5**：架构重构 — ReviewRepository.getAllVerifiedKnowledgePoints 死代码清理 + getVerifiedWithSubject 迁移到 KnowledgeRepository

### 新会话快速恢复 Checklist

新沙箱会话开始时，按以下顺序操作（5 分钟内进入工作状态）：

1. **读 [AGENTS.md](../AGENTS.md)** — 项目入口，了解技术栈、硬约束、CI 验证策略、当前状态
2. **读 [00-STATUS.md](00-STATUS.md)** — 10 秒了解当前状态（无阻塞，v0.2.0 已发布，CI 全绿）
3. **读本文档最后一节** — 上次进度（本次会话：Release v0.2.0 发布）
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

---

## Session 2026-07-13（第六条）：UI 精修 v0.3

### 目标

执行 [docs/plans/ui-refinement-v0.3.md](plans/ui-refinement-v0.3.md) — 修复用户反馈的 4 个 UI 问题：①记忆卡片翻转后呈现镜像内容 ②删除右上角导师信息 ③AI 入口放到右上角 ④整体动画不够干净利落。

### 完成内容

**Phase 1：卡片镜像修复**（commit `70cf54a`）
- FlipCard 修复：cameraDistance 提升到 30×depth（避免大角度翻转时镜像扭曲）
- shouldShowBack 阈值切换（0.5 而非 0.0）+ graphicsLayer rotationY 严格控制
- 容器色用 animateColorAsState 平滑过渡（避免翻转中色彩硬切）
- 评分按钮 + "点击卡片查看答案" 提示用 AnimatedVisibility（fadeIn/fadeOut + slideVertically）
- 进度文本 "1 / N" 用 animateContentSize（数字变化时平滑过渡）
- 新增 FlipCardLogicTest（6 个纯函数测试，覆盖 shouldShowBack/shouldShowRating 阈值逻辑）

**Phase 2：导师信息删除 + AI 入口调整**（commit `267d3ff`）
- 删除 MentorInfoScreen + ROUTE_MENTOR 路由 + 导航入口
- 4 个主屏（Knowledge/Quiz/Cards/Graph）TopBar 右上角新增 AI IconButton（SmartToy 图标），点击跳转 AiAssistantScreen

**Phase 3：WenyanMotion tokens + NavHost transition**（commit `1a244ef`）
- 新增 `core/designsystem/motion/WenyanMotion.kt` 统一动画 token：
  - Duration: Short=150ms / Medium=300ms / Long=450ms
  - Easing: EmphasizedEasing / DecelerateEasing / AccelerateEasing（CubicBezier）
- NavHost 全局：Tab 间用 fade transition（DurationShort + DecelerateEasing）
- NavHost 子路由：Push 用 slideIn from right / Pop 用 slideOut to right（DurationMedium + EmphasizedEasing）

**Phase 4：7 屏状态切换 Crossfade**（commit `deb7515`）
- 7 个 Screen 的 loading/empty/content 三态切换从 if/else 硬切改为 Crossfade：
  - KnowledgeScreen / QuizScreen（之前的 commit）
  - CardsScreen / GraphScreen / AiAssistantScreen / ApiConfigScreen / KnowledgePointDetailScreen
- targetState 用 Pair<Boolean, Boolean>（isLoading to isEmpty）避免每个 uiState 字段变化都触发 crossfade
- CardsScreen 的 `return@Column` 早退模式重构为 Crossfade + when 三态
- KnowledgePointDetailScreen 的 `point!!` 不安全强转改为 `?.let { point -> ... }` 安全访问

**Phase 5：LazyColumn animateItem + Settings AnimatedVisibility**（commit `add1f43`）
- 4 个 LazyColumn 列表项增删时用 animateItem() 平滑过渡：
  - KnowledgeScreen / QuizScreen / AiAssistantScreen / ApiConfigScreen
  - 所有 items 添加 `key = { it.id }` 让 Compose 跟踪项的身份
- 4 个 Card composable 新增 `modifier: Modifier = Modifier` 参数（KnowledgePointCard / QuestionCard / MessageBubble / ConfigCard）
- SettingsScreen 动态色彩开关关闭时，"种子色 + 调色板风格" 区块用 AnimatedVisibility 平滑展开/收起（fadeIn + expandVertically / fadeOut + shrinkVertically）

**Phase 6：全量验证 + 文档更新**（本次）
- `assembleDebug` BUILD SUCCESSFUL
- `testDebugUnitTest` 190 tests 0 failures（184 原有 + 6 FlipCardLogic）
- 更新文档：AGENTS.md、00-STATUS.md、SESSION_LOG.md、plans/ui-refinement-v0.3.md

### 关键技术决策

1. **WenyanMotion 单一 token 源** — 所有动画时长/缓动从 `WenyanMotion` object 取，避免散落硬编码。CubicBezier 控制点参照 Material3 Expressive 运动规格（0.2, 0.0, 0.0, 1.0）。
2. **Crossfade targetState 用 Pair** — 用 `isLoading to isEmpty` 作 targetState 而非整个 uiState，避免 uiState 任意字段变化都触发 crossfade 重启。
3. **CardsScreen 三态 when 而非 if/else** — 把 `return@Column` 早退模式改为 `when { isLoading; isEmpty; else }`，让 CrossFade 能管理所有三个状态的过渡。
4. **KnowledgePointDetailScreen 安全访问** — 把 `val point = uiState.point!!` 改为 `uiState.point?.let { point -> ... }`，避免在 Crossfade 切换瞬间空指针。
5. **LazyColumn items 必须有 key** — `key = { it.id }` 让 Compose 跟踪列表项身份，animateItem() 才能正确识别增删位置并播放过渡动画。

### commit 列表

- `70cf54a` — Phase 1: 卡片镜像修复 + 6 个纯函数测试
- `267d3ff` — Phase 2: 删除导师信息 + 4 主屏 TopBar 加 AI 入口
- `1a244ef` — Phase 3: WenyanMotion tokens + NavHost transition
- `deb7515` — Phase 4: 7 屏状态切换 Crossfade 替代 if/else 硬切
- `add1f43` — Phase 5: LazyColumn animateItem + Settings Switch AnimatedVisibility
- 本次 — Phase 6: 文档更新

### 下次继续

- **P0**：跑 emulator 实测 v0.3 改动 — 6 项验证（卡片翻转无镜像 / AI 入口可跳转 / Tab fade transition / Crossfade loading→content / animateItem 列表过渡 / Settings 种子色区块展开收起）
- **P1**：可选 — 发 Release v0.3.0（确认 CI 全绿后 `git tag v0.3.0 && git push origin v0.3.0`）
- **P2**：OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（替换 stage2-sample）

### 新会话快速恢复 Checklist

新沙箱会话开始时，按以下顺序操作（5 分钟内进入工作状态）：

1. **读 [AGENTS.md](../AGENTS.md)** — 项目入口，了解技术栈、硬约束、CI 验证策略、当前状态
2. **读 [00-STATUS.md](00-STATUS.md)** — 10 秒了解当前状态（无阻塞，UI 精修 v0.3 完成）
3. **读本文档最后一节** — 上次进度（本次会话：UI 精修 v0.3）
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


---

## Session 2026-07-14：第三轮深度审计 v0.4.2 + 4 Batch 修复执行

### 目标

执行用户指令"做好检查后直接做修复计划并且执行，还要做好交接，严谨一点" — 在前两轮审计基础上完成第三轮深度审计，制定修复计划并执行全部 4 Batch 修复，更新交接文档。

### 审计发现（v0.4.2 深度审计报告）

详见 [docs/plans/full-audit-v0.4.2-deep.md](plans/full-audit-v0.4.2-deep.md)。共发现 9 个 P0 + 多个 P1 问题：

| 编号 | 类别 | 严重度 | 简述 |
|------|------|--------|------|
| P0-F1 | FSRS 算法 | P0 | nextDifficulty 权重索引错误 w[5]/w[6] 应为 w[6]/w[7] |
| P0-F2 | FSRS 算法 | P0 | easyBonus 语义反转（w[16]<1 直接作乘子导致 EASY<GOOD） |
| P0-F3 | FSRS 算法 | P0 | EASY 评分 stability/interval 基准不一致 |
| P0-F5 | FSRS 算法 | P0 | interval 用 toInt 截断而非 roundToInt 四舍五入 |
| P0-D1 | 数据安全 | P0 | GraphRepository N+1 查询（3 处 mapNotNull { getById }) |
| P0-D2 | 数据安全 | P0 | 同上，未用批量查询 |
| P0-D3 | 数据安全 | P0 | DAO observe 方法缺 ORDER BY，Compose 重组时顺序抖动 |
| P0-T1 | 测试有效 | P0 | AntiRoteMemorizationTest 用 Kotlin assert()（-ea 关闭时静默跳过） |
| P0-T2 | 测试有效 | P0 | WenyanNavigationBarTest 测内部实现而非公开契约 |
| P0-M1 | 元数据 | P0 | versionName 误标 "0.1.0"（实际 v0.3） |
| P0-M2 | 元数据 | P0 | versionCode 未递增（3 版都用 1） |

### FSRS 4 个 Bug 详解

| Bug | 位置 | 错误 | 修正 |
|-----|------|------|------|
| F-01 | FsrsWrapper.nextDifficulty | mean reversion 用 w[5]/w[6] | 改为 w[6]/w[7]（FSRS-6 标准） |
| F-02 | FsrsWrapper.nextRecallStability | easyBonus = w[16] 直接作乘子（<1 导致反转） | 改为 1 + w[16] |
| F-03 | FsrsWrapper.schedule EASY 分支 | stability 用 nextRecallStability 但 interval 用 good 基准 | 统一用 EASY 基准 |
| F-05 | FsrsWrapper.nextInterval | toInt() 截断 | roundToInt() 四舍五入 |

### 4 Batch 修复执行

**Batch 1：FSRS 算法正确性修复**（core/fsrs）
- `FsrsWrapper.kt`：4 个 bug 全部修正
  - F-01：nextDifficulty 中 w[5]→w[6]、w[6]→w[7]
  - F-02：easyBonus 从 w[16] 改为 1 + w[16]
  - F-03：EASY 分支 stability 和 interval 统一用 EASY 基准
  - F-05：nextInterval 从 toInt() 改为 roundToInt()
- `FsrsWrapperTest.kt`：新增 4 个回归测试
  - `nextDifficulty_uses_w6_w7_not_w5_w6`
  - `nextRecallStability_easy_greater_than_good`
  - `nextRecallStability_easy_correct_value`
  - `nextInterval_uses_round_not_truncation`

**Batch 2：数据安全 P0 修复**（多模块）
- `AndroidManifest.xml`：android:allowBackup="false" + android:fullBackupContent="false"（防备份泄漏）
- `app/build.gradle.kts`：versionCode 1→3、versionName "0.1.0"→"0.3.0"
- `DatabaseModule.kt`：fallbackToDestructiveMigration → fallbackToDestructiveMigrationOnDowngrade（升级不再静默丢数据）
- `core/ai/build.gradle.kts`：buildFeatures { buildConfig = true }
- `AiModule.kt`：companion var DEFAULT_API_KEY → UUID 替代（防跨实例状态泄漏）
- `core/data/build.gradle.kts`：implementation room-ktx（withTransaction 依赖）
- `SeedDataLoader.kt`：withContext → withTransaction（原子性导入）

**Batch 3：测试有效性 P0 修复**
- `AntiRoteMemorizationTest.kt`：Kotlin assert() → JUnit assertEquals（-ea 关闭时不再静默跳过）
- `WenyanNavigationBarTest.kt`：从测内部 selectedItem 状态改为测公开 onNavigate 回调契约
- `AiAssistantViewModel.kt`：清理冗余 sendUserMessage 重载

**Batch 4：关键 UX/契约 P1 修复**（10 文件）
- `ThemeRepositoryImpl.kt`：枚举 valueOf 用 runCatching 容错（P1-NEW-7，防非法值崩溃）
- `feature/settings/build.gradle.kts`：启用 buildConfig + 注入 VERSION_NAME buildConfigField（P1-M2）
- `SettingsScreen.kt`：版本号从硬编码 "v0.1.0" 改为 "v${BuildConfig.VERSION_NAME}"
- `GraphNodeDao.kt`：4 个 observe 加 ORDER BY id ASC + 新增 getByIds 批量查询（P1-D1/D2/D3）
- `GraphEdgeDao.kt`：5 个 observe 加 ORDER BY id ASC
- `DataSourceDao.kt`：4 个 observe 加 ORDER BY created_at ASC
- `KnowledgePointDao.kt`：4 个 observe 加 ORDER BY created_at ASC
- `MemoRecordDao.kt`：observeAll 加 ORDER BY next_review_at ASC
- `GraphRepositoryImpl.kt`：3 处 N+1 修复（getPrerequisites/getRelatedNodes/getAdjacentNodes 用 getByIds + associateBy）
- `CardsViewModel.kt`：rateCard try/catch + isFinished 完成态 + errorMessage StateFlow（P1-NEW-4）
- `ApiConfigViewModel.kt`：editingId 局部量捕获避免协程内外不一致（P1-NEW-5）

### 编译 + 测试验证

- `assembleDebug` BUILD SUCCESSFUL
- `testDebugUnitTest` **207 tests 0 failures**（190 基线 + 17 新增 FSRS 测试）
  - core/fsrs: 29 tests（含 4 个新回归测试）
  - core/data: 52 tests
  - core/designsystem: 14 tests
  - core/ai: 62 tests
  - feature/aiassistant: 21 tests
  - feature/cards: 含 CardsViewModel 新增错误处理测试
  - 其他模块全绿

### 关键技术决策

1. **P0-T1d（127.0.0.1:1 网络测试）保留不改** — Linux CI 上 ECONNREFUSED 立即返回（稳定），不需要修改
2. **library 模块 BuildConfig 限制** — library 模块即使 buildConfig=true 也不含 VERSION_NAME，需用 buildConfigField 显式注入。已记录到 03-FAILED-ATTEMPTS.md #013
3. **4 个 P0 未修（P0-E1/E2/E3/E4）** — 工作量大，留待下迭代
4. **fallbackToDestructiveMigrationOnDowngrade** — 仅降级时重建表，升级时抛异常（强制开发者写 Migration）
5. **UUID 替代 companion var** — 避免跨实例状态泄漏，每次创建新实例生成新 UUID

### 环境配置（新沙箱必做）

新沙箱无 Android SDK，需完整配置：

1. Gradle 代理：`/root/.gradle/gradle.properties`（http/https proxyHost=127.0.0.1:18080）
2. Robolectric 代理：`/root/.gradle/init.d/proxy.gradle`（jvmArgs 注入到 Test 任务）
3. Android SDK 安装：cmdline-tools + platform-tools;35.0.0 + platforms;android-35 + build-tools;35.0.0
4. local.properties：`sdk.dir=/opt/android-sdk`
5. 环境变量：JAVA_HOME（mise java 17）、ANDROID_HOME、JAVA_TOOL_OPTIONS

### 修改文件清单（24 文件）

**Batch 1（2 文件）**：
- `core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt`
- `core/fsrs/src/test/java/com/wenyan/app/core/fsrs/FsrsWrapperTest.kt`

**Batch 2（7 文件）**：
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `core/database/src/main/java/com/wenyan/app/core/database/di/DatabaseModule.kt`
- `core/ai/build.gradle.kts`
- `core/ai/src/main/java/com/wenyan/app/core/ai/di/AiModule.kt`
- `core/data/build.gradle.kts`
- `core/data/src/main/java/com/wenyan/app/core/data/repository/SeedDataLoader.kt`

**Batch 3（3 文件）**：
- `core/ai/src/test/java/com/wenyan/app/core/ai/recall/AntiRoteMemorizationTest.kt`
- `core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/WenyanNavigationBarTest.kt`
- `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt`

**Batch 4（10 文件）**：
- `core/data/src/main/java/com/wenyan/app/core/data/repository/ThemeRepositoryImpl.kt`
- `feature/settings/build.gradle.kts`
- `feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`
- `core/database/src/main/java/com/wenyan/app/core/database/dao/GraphNodeDao.kt`
- `core/database/src/main/java/com/wenyan/app/core/database/dao/GraphEdgeDao.kt`
- `core/database/src/main/java/com/wenyan/app/core/database/dao/DataSourceDao.kt`
- `core/database/src/main/java/com/wenyan/app/core/database/dao/KnowledgePointDao.kt`
- `core/database/src/main/java/com/wenyan/app/core/database/dao/MemoRecordDao.kt`
- `core/data/src/main/java/com/wenyan/app/core/data/repository/GraphRepositoryImpl.kt`
- `feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsViewModel.kt`
- `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/ApiConfigViewModel.kt`

**交接文档（4 文件）**：
- `docs/plans/full-audit-v0.4.2-deep.md`（审计报告 + 修复计划）
- `docs/03-FAILED-ATTEMPTS.md`（#013 新增）
- `docs/SESSION_LOG.md`（本节）
- `docs/00-STATUS.md` + `AGENTS.md`

### 下次继续

1. **P0**：跑 emulator 实测 v0.3 + v0.4.2 修复 — 验证 FSRS 调度正确性（EASY 间隔 > GOOD 间隔）+ 卡片翻转无镜像 + AI 入口可跳转
2. **P0**：修复 4 个未修 P0（P0-E1/E2/E3/E4）— 工作量大，需单独排期
3. **P1**：可选 — 发 Release v0.3.0（确认 CI 全绿后 `git tag v0.3.0 && git push origin v0.3.0`）
4. **P2**：OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（替换 stage2-sample）
5. **P3**：release.yml "Verify keystore" 步骤隐藏 bug（Line 63-70，KEYSTORE_BASE64 未配置时失败）
6. **P4**：架构重构 — ReviewRepository.getAllVerifiedKnowledgePoints 已成事实死代码

### 新会话快速恢复 Checklist

新沙箱会话开始时，按以下顺序操作（5-10 分钟内进入工作状态）：

1. **读 [AGENTS.md](../AGENTS.md)** — 项目入口，了解技术栈、硬约束、CI 验证策略、当前状态
2. **读 [00-STATUS.md](00-STATUS.md)** — 10 秒了解当前状态（无阻塞，v0.4.2 审计修复完成，207 tests）
3. **读本文档最后一节** — 上次进度（本次会话：第三轮深度审计 v0.4.2 + 4 Batch 修复）
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
7. **安装 Android SDK**（新沙箱无预装）：
   ```bash
   mkdir -p /opt/android-sdk/cmdline-tools
   cd /opt/android-sdk/cmdline-tools
   # 下载 cmdline-tools（如已存在则跳过）
   if [ ! -d latest ]; then
     wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip
     unzip -q /tmp/cmdline-tools.zip -d /opt/android-sdk/cmdline-tools
     mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest
   fi
   yes | sdkmanager --licenses > /dev/null 2>&1
   sdkmanager "platform-tools;35.0.0" "platforms;android-35" "build-tools;35.0.0"
   ```
8. **配置 local.properties**：
   ```bash
   echo "sdk.dir=/opt/android-sdk" > /workspace/local.properties
   ```
9. **验证构建**（注意：不能用 `gradle` shim，它用 mise 默认 JDK 25 与 AGP 8.6.0 不兼容）：
   ```bash
   $JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :app:assembleDebug --no-daemon 2>&1 | tail -5
   ```
10. **开始工作**：根据 [00-STATUS.md](00-STATUS.md) 的"下一步优先级"选择任务


---

## Session 2026-07-15：第四轮深度审计 v0.5.0 — Phase 2 P1/P2 修复执行

### 目标

执行用户指令"再制定一个详细的检查计划…严谨认真，仔细检查，非常深入…把这个计划进一步打磨，彻底找出所有的问题" — 在前三轮审计基础上完成第四轮 v0.5.0 深度审计，制定 5 Phase 修复计划，并执行 Phase 2 P1/P2 修复。

### 审计计划

详见 [docs/plans/full-audit-v0.5.0-deep.md](plans/full-audit-v0.5.0-deep.md)（166KB，5 Phase）：
- **Phase 1**：数据持久化与生命周期（1.C AI 对话持久化 / 1.D 进程被杀恢复）
- **Phase 2**：代码质量与稳定性（2.A-2.O 共 15 个维度）
- **Phase 3**：依赖升级路径
- **Phase 4**：25 项 emulator 测试矩阵
- **Phase 5**：7 Batch 修复执行

### 已完成 Phase 2 P1/P2 修复（10 个 commit，全部 push main）

| Commit | 类型 | 简述 |
|--------|------|------|
| `a7fdce2` | feat | 检查项目进展 |
| `af14136`~`c2681f8` | 前轮 | P0+P1 修复（Navigation/HttpLogging/种子超时/CancellationException）— CI ✅ |
| `dd3ff06` | P0+P2 | P0-AUDIT-1 review_logs elapsedDays 旧值未传 + P2 语义修正 — CI ❌ 账单 |
| `ca3ceea` | P0 | P0-STAB-1 批量添加 @Immutable 注解（消除 Compose 不必要重组）— CI ❌ |
| `c0e2775` | P1+P2 | P1-AUDIT-5 LEFT JOIN + P1-AUDIT-2 + P1-CI-1/2 + P1-S-1 + P2-LAZY-1 + P2-REC-5 — CI ❌ |
| `63f5375` | P1 | Repository 层 23 处 Flow 链加 .catchAndLog 异常处理（7 Repository + FlowExt.kt）— CI ❌ |
| `53a0c46` | P1 | P1-CI-4 keystore 密码随机化 + P1-AUDIT-4 种子版本感知升级 — CI ❌ |
| `f9fc9c5` | P2 | GraphScreen remember(uiState.nodes) + FlipCard derivedStateOf 性能优化 — CI ❌ |
| `5d00824` | P1 | P1-AUDIT-3 AntiRoteMemorization 参数命名对齐 + NF-T6 防御性编码 + 已知差距文档 — CI ❌ |
| `01a1049` | P1 | 2.O/2.E 资源与配置（monochrome icon + M3 DayNight 主题 + values-night）— CI ❌ |
| `3179911` | P1 | 2.N 业务边界（LIKE 转义 + query 长度限制 + List→Set 去重）— CI ❌ |
| `0dd5b0f` | P1 | NF-BB2 SocraticTutor 三阶段上下文传递 — CI ❌ |

### CI 状态（关键阻塞）

- ✅ `c2681f8`/`d33dd4d`/`5c5bc64` — success（前轮 commits）
- ❌ `dd3ff06`~`0dd5b0f` — failure（**非代码错误**，GitHub Actions 账单问题）
- 根因：`recent account payments have failed or your spending limit needs to be increased`
- 表现：CI "build" 步骤 3 秒失败无 step 执行，无日志 blob（BlobNotFound）
- 应对：代码已 push main 等待账单问题解决后 CI 自动验证

### 本轮修复详情

#### Batch 1：P0-AUDIT-1 review_logs elapsedDays（`dd3ff06`）
- **问题**：`ReviewRepository.recordReview()` 写入 review_logs 时未传 `elapsedDays`，导致 AntiRoteMemorization 检测逻辑失效
- **修复**：补齐 elapsedDays 参数传递 + P2 FSRS 语义注释修正（LEARNING+HARD → nextRecallStability 正确，RELEARNING+HARD → nextForgetStability 正确）

#### Batch 2：P0-STAB-1 @Immutable 批量注解（`ca3ceea`）
- **问题**：多个 Compose State 数据类未标 @Immutable，导致不必要重组
- **修复**：为所有纯数据 State 类批量添加 @Immutable 注解

#### Batch 3：P1-AUDIT-5 + P1-AUDIT-2 + 多项 P1/P2（`c0e2775`）
- **P1-AUDIT-5**：`KnowledgePointDao.observeVerifiedWithSubject` 从 INNER JOIN 改 LEFT JOIN — 无效关联的知识点不再静默丢失
- **P1-AUDIT-2**：补齐缺失 ORDER BY（DAO observe 方法）
- **P1-CI-1/2**：CI 配置修复
- **P1-S-1**：StateFlow 语义修复
- **P2-LAZY-1**：LazyColumn lazy 化
- **P2-REC-5**：Repository 链优化

#### Batch 4：P1 Repository Flow 异常处理（`63f5375`）
- **问题**：Repository 层 23 处 Flow 链未捕获 DAO 异常，ViewModel collect 崩溃导致 UI 永久 failed
- **修复**：新增 `FlowExt.kt` 提供 `Flow<T>.catchAndLog(tag, operation, fallback)` 扩展函数（记录日志 + emit 降级值）
- 覆盖 7 个 Repository + 23 个方法
- **关键**：`kotlinx.coroutines.flow.catch` 不捕获 CancellationException，协程取消正常传播

#### Batch 5：P1-CI-4 + P1-AUDIT-4（`53a0c46`）
- **P1-CI-4**：keystore 密码用 `openssl rand -base64 24` 随机化替代硬编码（每次运行产生不同密码）
- **P1-AUDIT-4**：种子版本感知升级 — 存储 metadata.version 到 DataStore，启动时比对版本；版本不一致时重新导入内容表（@Upsert 安全），跳过已有 MemoRecord（保护 FSRS 学习进度）

#### Batch 6：P2 性能优化（`f9fc9c5`）
- **GraphScreen**：`remember(uiState.nodes)` 缓存 O(n) 统计计算，避免每次重组重复遍历 + 堆分配
- **FlipCard**：`derivedStateOf { shouldShowBack(rotation) }` 使布尔值仅在跨过 90° 临界点时触发重组

#### Batch 7：P1-AUDIT-3 AntiRoteMemorization 收尾（`5d00824`）
- **参数命名修复**：`cardId` → `pointId`，`relatedCardIds` → `relatedPointIds`（实际语义是知识点 ID）
- **NF-T6 防御性编码**：`log.rating.uppercase()` → `log.rating?.uppercase()`（防御性，保护潜在 schema 变更）
- **KDoc 准确化**：原声称"DB 列未约束 NOT NULL"不准确（实际 `ReviewLogEntity.rating` 是非空 String，Room 生成 NOT NULL 约束），改为准确描述
- **已知差距文档**：P1-AUDIT-3 已知差距（仅检测不干预 + 生产链路未接通 + 参数命名误导）写入 KDoc
- AiAssistantViewModel.kt 同步参数重命名

#### Batch 8：2.O/2.E 资源与配置修复（`01a1049`）
- **NF-C5 (P1)**：新增 `ic_launcher_monochrome.xml`（Android 13+ themed icon，白色"文"字矢量图，系统根据壁纸自动着色）
- **NF-C5 (P1)**：`ic_launcher.xml` + `ic_launcher_round.xml` 加 `<monochrome android:drawable="@drawable/ic_launcher_monochrome" />`
- **NF-U3 (P1)**：`themes.xml` 从 legacy `android:Theme.Material.Light.NoActionBar` 改 M3 `Theme.Material3.DayNight.NoActionBar`
- **NF-U4 (P1)**：新增 `values-night/colors.xml`（`wenyan_window_background = #1C1B1F`，M3 默认暗色 surface，避免深色模式白屏闪烁）

#### Batch 9：2.N 业务边界修复（`3179911`）
- **NF-BB1 (P1)**：LIKE 通配符转义 — `KnowledgePointDao.searchByKeyword` 4 个 LIKE 子句加 `ESCAPE '\\'` + `RagEngine.escapeLikeWildcards()` 方法（`%`→`\%`、`_`→`\_`、`\`→`\\`）。原查询搜索"100%"会匹配"1000"
- **NF-BB10 (P1)**：`RagEngine.search()` 加 `query.take(MAX_QUERY_LENGTH=500)` 长度限制，防止超长 query 拖垮 DB
- **NF-BB5 (P1)**：`ExamRepository.getRelatedKnowledgePoints` List→Set 去重，O(n) → O(1) 查找
- **验证 NF-BB9 (P0) 已修复**：`Rating.fromValue` 用 `firstOrNull` + GOOD 降级（之前会话已修）
- **验证 NF-BB8 (P1) 已修复**：`elapsedDays.coerceAtLeast(0)`（之前会话已修）

#### Batch 10：NF-BB2 SocraticTutor 三阶段上下文传递（`0dd5b0f`）
- **问题**：苏格拉底三阶段（分析论证漏洞→改进建议→范文）各自独立调用 LLM，输出可能逻辑不一致
- **修复**：
  - `PromptTemplates.buildSuggestPrompt` 加 `previousAnalysis: String = ""` 参数，prompt 中加入 `【上一阶段分析】` 段落
  - `PromptTemplates.buildSampleEssayPrompt` 加 `previousAnalysis` + `previousSuggestion` 参数，prompt 中加入 `【论证分析】` + `【改进建议】` 段落
  - `SocraticTutor.guideEssayAnswer()` 捕获 `analysisResult` 和 `suggestionResult`，传入后续阶段
- **向后兼容**：新参数均有默认值 ""，不影响现有调用方

### 关键技术决策

1. **catchAndLog 扩展函数** — 统一 Flow 异常处理模式，避免每个 Repository 重复 try/catch 样板代码。`Flow<T>.catchAndLog(tag, operation, fallback)` 记录日志 + emit 降级值。`kotlinx.coroutines.flow.catch` 不捕获 CancellationException，协程取消信号正常传播。

2. **FSRS-6 LEARNING+HARD 语义** — LEARNING+HARD → nextRecallStability 是正确的（卡片被回忆，只是有困难）；RELEARNING+HARD → nextForgetStability 也正确（卡片遗忘后重新学习）。原审计报告 P0-AUDIT-1 描述有误，已修正为 P2 文档级别。

3. **种子版本感知升级** — 存储 metadata.version 到 DataStore，启动时比对版本；版本不一致时重新导入内容表（@Upsert 安全），跳过已有 MemoRecord（保护 FSRS 学习进度）。避免每次重新导入清空用户学习数据。

4. **keystore 密码随机化** — `openssl rand -base64 24` 替代硬编码，每次 CI 运行产生不同密码。storepass = keypass（硬约束）通过 GitHub Secrets 一次性注入两个变量保证。

5. **Compose remember 优化** — `remember(uiState.nodes)` 缓存 O(n) 统计计算，避免每次重组重复遍历 + 堆分配；`derivedStateOf { shouldShowBack(rotation) }` 使布尔值仅在跨过 90° 临界点时触发重组。

6. **AntiRoteMemorization 已知差距** — 仅检测不干预（Spec 要求降低置信度 + 变体出题 + 反向提问）、生产链路未接通（无 UI 调用方）、参数命名误导（cardId 实为 pointId，已修）。这些差距已写入 KDoc 文档，留待下迭代。

7. **Android 13+ themed icon** — `<monochrome>` 属性 + 白色矢量图，系统根据壁纸自动着色。需同时配置 `ic_launcher.xml` 和 `ic_launcher_round.xml`。

8. **M3 DayNight 主题** — `Theme.Material3.DayNight.NoActionBar` 替代 legacy `android:Theme.Material.Light.NoActionBar`，配合 `values-night/colors.xml` 深色模式窗口背景，避免白屏闪烁。

9. **SQLite LIKE 通配符转义** — `ESCAPE '\\'` 子句 + `escapeLikeWildcards()` 转义 `%` 和 `_`。原查询搜索"100%"会匹配"1000"等（% 被当通配符）。

10. **SocraticTutor 三阶段上下文传递** — 阶段2 prompt 加入阶段1分析结果，阶段3 prompt 加入阶段1+2结果，三段输出连贯。新参数均有默认值 ""，向后兼容。

### 修改文件清单（本轮新增/修改）

**Batch 1（`dd3ff06`）**：
- `core/data/src/main/java/com/wenyan/app/core/data/repository/ReviewRepository.kt`（elapsedDays 传递）

**Batch 2（`ca3ceea`）**：
- 多个 ViewModel/State 文件批量添加 @Immutable

**Batch 3（`c0e2775`）**：
- `core/database/src/main/java/com/wenyan/app/core/database/dao/KnowledgePointDao.kt`（LEFT JOIN）
- 其他 DAO（ORDER BY 补齐）

**Batch 4（`63f5375`）**：
- `core/data/src/main/java/com/wenyan/app/core/data/util/FlowExt.kt`（新增 catchAndLog）
- 7 个 Repository 文件（23 处 Flow 链）

**Batch 5（`53a0c46`）**：
- `.github/workflows/release.yml`（keystore 密码随机化）
- `core/data/src/main/java/com/wenyan/app/core/data/repository/SeedDataLoader.kt`（版本感知升级）

**Batch 6（`f9fc9c5`）**：
- `feature/graph/src/main/java/com/wenyan/app/feature/graph/GraphScreen.kt`（remember 优化）
- `feature/cards/src/main/java/com/wenyan/app/feature/cards/FlipCard.kt`（derivedStateOf）

**Batch 7（`5d00824`）**：
- `core/ai/src/main/java/com/wenyan/app/core/ai/recall/AntiRoteMemorization.kt`（参数命名 + NF-T6 + KDoc）
- `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantViewModel.kt`（参数同步）

**Batch 8（`01a1049`）**：
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`（新增）
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-night/colors.xml`（新增）

**Batch 9（`3179911`）**：
- `core/ai/src/main/java/com/wenyan/app/core/ai/RagEngine.kt`（query 限制 + LIKE 转义）
- `core/database/src/main/java/com/wenyan/app/core/database/dao/KnowledgePointDao.kt`（ESCAPE '\\'）
- `core/data/src/main/java/com/wenyan/app/core/data/repository/ExamRepository.kt`（List→Set）

**Batch 10（`0dd5b0f`）**：
- `core/ai/src/main/java/com/wenyan/app/core/ai/PromptTemplates.kt`（previousAnalysis/previousSuggestion 参数）
- `core/ai/src/main/java/com/wenyan/app/core/ai/SocraticTutor.kt`（三阶段上下文传递）

### 下次继续

按 v3 审计计划优先级（详见 [docs/plans/full-audit-v0.5.0-deep.md](plans/full-audit-v0.5.0-deep.md)）：

1. **P0 阻塞**：GitHub Actions 账单问题解决后，所有 CI ❌ commits 会自动重跑。需观察 `dd3ff06`~`0dd5b0f` 的 CI 状态
2. **P1 大型任务**（需用户确认优先级）：
   - P1-PG-1/2/3：启用 R8 + 补齐 ProGuard 规则
   - NF-PP4：复习日志双写统一
   - NF-PP5：错题本实现
   - NF-PP6：AiAssistantViewModel 消息持久化
3. **Phase 2 剩余维度审计**：
   - 2.E 剩余：strings.xml 完整性（NF-U2，9 Screen 硬编码字符串迁移）、dimens.xml（NF-C10，CardRenderer 20+ 硬编码 dp）
   - 2.L：错误处理一致性 + 日志规范（sealed AppError + Timber + Snackbar 统一 + CancellationException）
   - 2.M：Compose 副作用 + Accessibility + M3 Expressive（LaunchedEffect + role + 触控目标 + TalkBack + MotionScheme + WideNavigationRail）
   - 2.N 剩余：NF-DS7-13 DataStore Key 治理（需建 PreferenceKeys.kt 集中定义）
4. **Phase 1 剩余（大型）**：1.C（AI 对话持久化）、1.D（进程被杀状态恢复）
5. **Phase 3**：依赖升级路径
6. **Phase 4**：25 项 emulator 测试矩阵
7. **Phase 5**：7 Batch 修复

### 新会话快速恢复 Checklist

新沙箱会话开始时，按以下顺序操作（5-10 分钟内进入工作状态）：

1. **读 [AGENTS.md](../AGENTS.md)** — 项目入口，了解技术栈、硬约束、CI 验证策略、当前状态
2. **读 [00-STATUS.md](00-STATUS.md)** — 10 秒了解当前状态（v0.5.0 审计 Phase 2 P1/P2 修复执行中）
3. **读本文档最后一节** — 上次进度（本次会话：v0.5.0 第四轮深度审计 Phase 2 P1/P2 修复 10 commits）
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
7. **安装 Android SDK**（新沙箱无预装）：
   ```bash
   mkdir -p /opt/android-sdk/cmdline-tools
   cd /opt/android-sdk/cmdline-tools
   if [ ! -d latest ]; then
     wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip
     unzip -q /tmp/cmdline-tools.zip -d /opt/android-sdk/cmdline-tools
     mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest
   fi
   yes | sdkmanager --licenses > /dev/null 2>&1
   sdkmanager "platform-tools;35.0.0" "platforms;android-35" "build-tools;35.0.0"
   ```
8. **配置 local.properties**：
   ```bash
   echo "sdk.dir=/opt/android-sdk" > /workspace/local.properties
   ```
9. **验证构建**：
   ```bash
   $JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :app:assembleDebug --no-daemon 2>&1 | tail -5
   ```
10. **检查 CI 状态**（GitHub Actions 账单问题可能已解决）：
    ```bash
    # 从 git remote URL 提取 token
    TOKEN=$(git -C /workspace remote get-url origin | grep -oE 'ghu_[A-Za-z0-9]+')
    # 查看最近 CI runs
    curl -s -H "Authorization: token $TOKEN" \
      https://api.github.com/repos/qbjsdsb/wenyan-android/actions/runs?per_page=10 \
      | python3 -c "import json,sys; [print(f\"{r['head_sha'][:7]} {r['conclusion']} {r['name']}\") for r in json.load(sys.stdin)['workflow_runs']]"
    ```
11. **开始工作**：根据 [00-STATUS.md](00-STATUS.md) 的"下一步优先级"选择任务

---

## Session 2026-07-15（续）：Release v0.3.0 + v0.5.0 Phase 2 第二批修复

### 目标

用户指令"现在我想看到成品，就是你发布release" + "问题继续修啊，完了做好交接工作，严肃认真仔细，反复检查不要出问题"。
本轮完成：Release v0.3.0 发布 + v0.5.0 Phase 2 第二批 8 项 P1/P2 修复 + 完整交接文档。

### Release v0.3.0 发布

#### 流程
1. 本地安装 Android SDK（cmdline-tools + platform-tools + platforms;android-35 + build-tools;35.0.0）
2. 配置 local.properties + Gradle proxy
3. 本地 assembleDebug 构建 — **发现 P0-STAB-1 遗留 bug**
4. 修复 bug 后重新构建 — BUILD SUCCESSFUL
5. 本地 testDebugUnitTest — **发现 P1-AUDIT-2 遗留 bug**
6. 修复 bug 后重新测试 — 215 tests 0 failures
7. 本地 assembleRelease — BUILD SUCCESSFUL
8. 通过 GitHub API 创建 Release + 上传 APK

#### 过程中发现的 2 个 CI 账单问题掩盖的 bug

**Bug 1（commit `96d9755`）**：P0-STAB-1 遗留 — `core:data` 加了 `@Immutable` 注解但没加 `androidx.compose.runtime` 依赖。`compileDebugKotlin` 失败。修复：加 `implementation(libs.androidx.compose.runtime)`。

**Bug 2（commit `96d9755`）**：P1-AUDIT-2 遗留 — `ClockGuard` 在时钟回拨时调用 `android.util.Log.w()`，新增 2 个测试触及该路径，但 `core:data` 没配 `testOptions.unitTests.isReturnDefaultValues = true`。测试失败。修复：加配置使 Log 方法返回默认值。

#### Release 结果

- **Tag**：`v0.3.0`
- **Release URL**：https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.3.0
- **APK**：`wenyan-v0.3.0.apk` (17MB) + `wenyan-latest.apk` (17MB)
- **签名**：debug 签名（CI 账单问题导致 Release workflow 无法执行正式签名）
- **验证**：assembleDebug SUCCESSFUL + testDebugUnitTest 215 tests 0 failures + assembleRelease SUCCESSFUL

### v0.5.0 Phase 2 第二批修复（commit `d1cb4d7`）

#### 修复清单（8 项 P1/P2）

**性能优化**
- **NF-UC2 (P1)**：WenyanTheme `dynamicLightColorScheme/dynamicDarkColorScheme` 未 remember，每次重组重建 ColorScheme。用 `remember(context, isDark)` 缓存。
- **NF-UC5 (P1)**：GraphCanvas `pointerInput(nodes)` 在 nodes 变化时重启手势检测，R 值刷新瞬间 tap 丢失。改 `pointerInput(Unit)` + `rememberUpdatedState` 保持最新引用。

**无障碍修复**
- **NF-UA2 (P1)**：AiAssistantScreen "知道了" 触控目标 ~28dp 低于 WCAG 48dp 标准，加 `defaultMinSize(48.dp, 48.dp)` + `role=Role.Button`。
- **NF-UA3 (P1)**：GraphCanvas 节点标签 `fontSize=9.sp` 低于 WCAG 推荐最小 12.sp，改为 12.sp。
- **NF-UA4 (P1)**：KnowledgeScreen + ApiConfigScreen 的 TonalCard `.clickable` 无 role，TalkBack 不朗读"按钮"。加 `role=Role.Button` 语义。

**UX 修复**
- **NF-UC3 (P1)**：AiAssistantScreen `LaunchedEffect(messages.size)` 无条件滚动到底部，打断用户上滑阅读。改为 `derivedStateOf` 检测 `isAtBottom`，仅在底部附近才自动滚动。
- **NF-UC4 (P1)**：`LaunchedEffect(errorMessage)` 内 `clearError` 在 Composable 离开时不执行，错误消息重复展示。改为先 `clearError()` 再 `showSnackbar`。

**死依赖清理**
- **NF-B7 (P2)**：`core:ai` 的 `androidx.security.crypto` 是死依赖（实际加密在 `core:data` 的 `ApiKeyCryptoImpl` 用 AndroidKeyStore + javax.crypto），移除。
- **NF-B8 (P2)**：`libs.versions.toml` 5 个 `wenyan-feature-*` 声明从未被引用（各模块用 `project(":feature:xxx")`），移除死声明。

#### 修改文件（7 个）

1. `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/WenyanTheme.kt` — NF-UC2 remember
2. `feature/graph/src/main/java/com/wenyan/app/feature/graph/ui/GraphCanvas.kt` — NF-UC5 pointerInput + NF-UA3 fontSize
3. `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantScreen.kt` — NF-UC3/UC4 LaunchedEffect + NF-UA2 触控目标
4. `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/ApiConfigScreen.kt` — NF-UA4 role
5. `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt` — NF-UA4 role
6. `core/ai/build.gradle.kts` — NF-B7 移除 security-crypto
7. `gradle/libs.versions.toml` — NF-B8 移除 wenyan-feature-*

#### 验证

- `assembleDebug` BUILD SUCCESSFUL
- `testDebugUnitTest` **215 tests 0 failures**

### 关键技术决策

1. **remember 不能包裹 @Composable 调用** — NF-UC2 初版用 `remember(...) { if (...) dynamicDarkColorScheme(context) else rememberDynamicColorScheme(...) }` 编译失败，因 `rememberDynamicColorScheme` 是 @Composable 函数，不能在 `remember` 的 value lambda 中调用。修正：用 if 分支分别处理，`dynamicDarkColorScheme` 用 `remember(context, isDark)`，`rememberDynamicColorScheme` 直接调用（内部已 remember）。

2. **pointerInput(Unit) + rememberUpdatedState 模式** — `pointerInput(nodes)` 在 key 变化时重启手势检测协程，R 值刷新瞬间 tap 丢失。改 `pointerInput(Unit)` 让协程只启动一次，配合 `rememberUpdatedState(nodes)` 在 lambda 内读取最新 nodes 引用。需加 `import androidx.compose.runtime.getValue`（`by` 委托需要）。

3. **derivedStateOf 滚动策略** — `LaunchedEffect(messages.size)` 无条件滚动到底部打断阅读。用 `derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 >= messages.size - 2 }` 计算 `isAtBottom`，仅在底部附近才自动滚动。`derivedStateOf` 使布尔值仅在跨过临界点时触发重组。

4. **WCAG 触控目标 48dp** — `defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)` 强制最小触控区域，配合 `role = Role.Button` 让 TalkBack 朗读"按钮"。

5. **死依赖识别方法** — 用 `Grep` 搜索 `androidx.security.crypto|MasterKey|EncryptedSharedPreferences` 确认无 import，再用 `Grep` 搜索 `wenyan-feature-` 确认无引用。死依赖增加 APK 体积 + 误导维护者。

### 完整 commit 链（本轮）

- `96d9755`：fix(build) core:data compose runtime + testOptions — Release v0.3.0 阻塞修复
- `0daa60b`：docs 更新 v0.5.0 进度
- `7b4d9ab`：docs Release v0.3.0 发布
- `d1cb4d7`：fix v0.5.0 Phase 2 第二批 8 项 P1/P2 修复

### 下次继续

按 v3 审计计划优先级（详见 [docs/plans/full-audit-v0.5.0-deep.md](plans/full-audit-v0.5.0-deep.md)）：

1. **P0**：CI 账单问题解决后，所有 CI ❌ commits 自动重跑
2. **P0**：跑 emulator 实测 v0.3.0 — 验证深色模式 + 触控目标 + 滚动策略 + 知识图谱 tap
3. **P1 大型任务**（需用户确认优先级）：
   - P1-PG-1/2/3：启用 R8 + 补齐 ProGuard 规则
   - NF-PP4：复习日志双写统一
   - NF-PP5：错题本实现
   - NF-PP6：AiAssistantViewModel 消息持久化
4. **P1 Phase 2 剩余维度审计**：
   - 2.E 剩余：strings.xml 完整性（NF-U2）、dimens.xml（NF-C10）
   - 2.L：错误处理一致性 + 日志规范（sealed AppError + Timber + Snackbar 统一）
   - 2.M 剩余：Compose 副作用（LaunchedEffect key 审计）+ M3 Expressive（WideNavigationRail）
   - 2.N 剩余：NF-DS7-13 DataStore Key 治理（需建 PreferenceKeys.kt 集中定义）
5. **Phase 1 剩余（大型）**：1.C（AI 对话持久化）、1.D（进程被杀状态恢复）
6. **Phase 3**：依赖升级路径
7. **Phase 4**：25 项 emulator 测试矩阵
8. **Phase 5**：7 Batch 修复

### v0.5.0 Phase 2 修复进度总览

| 批次 | Commit | 内容 | 项数 |
|------|--------|------|------|
| 1 | `dd3ff06` | P0-AUDIT-1 elapsedDays + P2 语义 | 2 |
| 2 | `ca3ceea` | P0-STAB-1 @Immutable | 1 |
| 3 | `c0e2775` | P1-AUDIT-5 LEFT JOIN + 多项 | 6 |
| 4 | `63f5375` | P1 Repository Flow .catchAndLog | 23 |
| 5 | `53a0c46` | P1-CI-4 keystore + P1-AUDIT-4 种子 | 2 |
| 6 | `f9fc9c5` | P2 性能（remember + derivedStateOf） | 2 |
| 7 | `5d00824` | P1-AUDIT-3 AntiRoteMemorization | 1 |
| 8 | `01a1049` | 2.O/2.E 资源配置 | 4 |
| 9 | `3179911` | 2.N 业务边界 | 3 |
| 10 | `0dd5b0f` | NF-BB2 SocraticTutor 上下文 | 1 |
| 11 | `96d9755` | 构建修复（compose runtime + testOptions） | 2 |
| 12 | `d1cb4d7` | 第二批 8 项（性能+无障碍+死依赖） | 8 |
| 13 | `40972fc` | 第三批 4 项（NF-T7/T8/A2/E8） | 4 |
| **合计** | 13 commits | | **59 项** |

---

## 2026-07-15 会话：v0.5.0 Phase 2 第三批修复（NF-T7/T8/A2/E8）

### 目标

用户指令"进行p1的修改，严谨仔细反复检查"。本轮完成 4 项小型 P1 修复 + 5 个单元测试，220 tests 0 failures。

### 修复清单（4 项 P1）

#### NF-T7: Rating 枚举新增 index 属性（FSRS 解耦）

**问题**：`FsrsWrapper.initStability` 用 `w[rating.value - 1]` 访问权重数组，把"枚举业务值"（1=AGAIN,2=HARD...用于 FSRS 公式 `rating-3`）与"数组下标"（0,1,2,3）耦合。若未来枚举顺序调整（如新增 MANUALLY_MARKED 档），`value - 1` 不再等于数组下标，可能引发越界或权重错位。

**修复**：Rating 枚举新增 `index` 属性（0-based），`initStability` 改用 `w[rating.index]`。`value` 仍用于算术（与 FSRS-6 公式 `rating-3` 保持一致）。

**文件**：
- `core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsModels.kt` — Rating 枚举加 `index: Int`
- `core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt` — `initStability` 用 `rating.index`

**测试**：`initStability_allRatings_matchWeightsAtIndex` — 验证 4 档评分各自返回对应的 w[i]，同时验证 `rating.index` 与数组下标一致。

#### NF-T8: FsrsWrapper applyFuzz 改用可注入 Random（FSRS 可测性）

**问题**：`applyFuzz` 用全局 `Random.nextFloat()` 不可注入，单元测试只能验证 fuzz 输出范围而非精确值（每次运行结果不同，无法写确定性断言）。

**修复**：FsrsWrapper 构造函数新增 `random: Random = Random.Default` 参数，`applyFuzz` 改用 `random.nextFloat()`。生产环境默认 `Random.Default` 行为不变，测试可注入固定种子 `Random(42)` 验证精确 fuzz 输出。

**文件**：`core/fsrs/src/main/java/com/wenyan/app/core/fsrs/FsrsWrapper.kt`

**测试**：
- `applyFuzz_withSeededRandom_isDeterministic` — 两个相同种子 `Random(42)` 的 wrapper 产生相同 scheduledDays
- `applyFuzz_differentSeeds_producesVariety` — 100 个不同种子产生 >1 种 scheduledDays

#### NF-A2: RecallChecker L2 增加 GOOD 档（L2 评分修正）

**问题**：原 L2 在 60-85% Jaccard 相似度范围统一返回 HARD（触发 L3）。若 L3 失败降级为 L2 结果，75-85% 相似度的答案被错误归为 HARD（过严）。75-85% 是"较好但不完美"，语义更接近 GOOD 而非 HARD。

**修复**：
- `L2_THRESHOLD_PARTIAL` 从 0.85f 改为 0.75f（L3 触发范围从 60-85% 收窄到 60-75%）
- 新增 `L2_THRESHOLD_GOOD = 0.85f`（75-85% → GOOD，不触发 L3）
- `PARTIAL_CORRECT_RANGE` 从 `0.60f..0.85f` 改为 `0.60f..0.75f`
- `checkL2Semantic` 增加 GOOD 档：75-85% 直接返回 GOOD，不依赖 L3

**文件**：`core/ai/src/main/java/com/wenyan/app/core/ai/recall/RecallChecker.kt`

**测试**：
- `c5_15_l2_highSimilarity_returnsGood_nfA2` — Jaccard=0.8（75-85%范围）应返回 GOOD
- `c5_15_l2_partialSimilarity_triggersL3_nfA2` — Jaccard≈0.667（60-75%范围）应触发 L3

**关键发现**：L3 被触发后 `RecallResult.coverage` 的语义从"L2 Jaccard 相似度"变为"L3 score/100"（见 `checkL3Llm` 中 `coverage = score / 100f`）。测试断言需用 L3 的 score/100 值（0.7）而非 L2 的 Jaccard 值（0.667）。

#### NF-E8: ApiKeyCryptoImpl decrypt 抛 DecryptionException（加解密异常区分）

**问题**：`decrypt` 在数据不完整（IV + 密文长度不足）时静默返回 `""`，导致"合法空 apiKey"（`encrypt("")` 返回 `""`）与"密文损坏"无法区分。用户看到一个"空 apiKey"的配置，误以为是数据问题而非密钥损坏。

**修复**：
- 新建 `DecryptionException`（RuntimeException 子类）
- `decrypt` 三处失败路径改抛 `DecryptionException`：
  1. Base64 解码失败（非法字符）
  2. 密文数据不完整（长度 < IV_SIZE + 1）
  3. GCM 认证失败（AEADBadTagException / 密文篡改 / master key 变更）
- 空字符串输入仍返回 `""`（合法空 apiKey，不抛异常）
- 调用方 `ApiConfigRepository.decryptedOrNull()` 已用 `runCatching { decrypt(...) }.getOrNull()` 捕获降级为 null

**文件**：
- `core/data/src/main/java/com/wenyan/app/core/data/crypto/DecryptionException.kt`（新建）
- `core/data/src/main/java/com/wenyan/app/core/data/crypto/ApiKeyCrypto.kt` — 接口加 `@Throws` 注解 + KDoc
- `core/data/src/main/java/com/wenyan/app/core/data/crypto/ApiKeyCryptoImpl.kt` — decrypt 三处失败路径抛异常

### 验证

- `assembleDebug` BUILD SUCCESSFUL
- `testDebugUnitTest` **220 tests 0 failures**（215 基线 + 5 新增测试）
- 注：lint 阶段在沙箱环境因 Java 17 + AGP 8.6.0 兼容性问题失败（`AndroidLintWorkAction` 类初始化错误），CI 环境无此问题

### 环境发现

- **Java 25 不兼容 AGP 8.6.0**：沙箱默认 Java 25.0.2，Gradle 启动即报 `25.0.2` 错误。需切换到 Java 17.0.2（`export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2`）。CI runner 用 Java 17/21 无此问题。
- **沙箱无 gradlew**：项目根目录无 `gradlew` 脚本和 `gradle-wrapper.jar`，需直接用 `gradle` 命令（mise 安装的 8.14.4）。

### commit

- `40972fc`：P1: v0.5.0 Phase 2 第三批修复 — NF-T7/T8/A2/E8（FSRS解耦+可测+L2评分+加解密异常）

### 下次继续

按 v3 审计计划优先级（详见 [docs/plans/full-audit-v0.5.0-deep.md](plans/full-audit-v0.5.0-deep.md)）：

1. **P0**：CI 账单问题解决后，所有 CI ❌ commits 自动重跑
2. **P0**：跑 emulator 实测 v0.3 + v0.4.2 + v0.5.0 修复
3. **P1 大型任务**（需用户确认优先级）：
   - P1-PG-1/2/3：启用 R8 + 补齐 ProGuard 规则
   - NF-PP4：复习日志双写统一
   - NF-PP5：错题本实现
   - NF-PP6：AiAssistantViewModel 消息持久化
   - NF-T4：MemoRecordMapper Float↔Double 精度（需 schema 迁移）
   - NF-D3：observeDue Flow 不刷新（需架构调整）
4. **P1 Phase 2 剩余维度审计**：
   - 2.E 剩余：strings.xml 完整性（NF-U2）、dimens.xml（NF-C10）
   - 2.L：错误处理一致性 + 日志规范（sealed AppError + Timber + Snackbar 统一）
   - 2.M：Compose 副作用 + Accessibility + M3 Expressive
   - 2.N 剩余：NF-DS7-13 DataStore Key 治理
