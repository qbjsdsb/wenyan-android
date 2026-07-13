# 文研 App UI 统一与死组件清理实施计划

> **状态：✅ 已完成（2026-07-13）** — Phase 1-3 全部完成，3 个 commit 已推送 main。
> Commits: `ebad848`（Phase 1）→ `2f83ac3`（Phase 2）+ 文档更新 commit。
> 验证：`assembleDebug` SUCCESSFUL（412 tasks）、`testDebugUnitTest` 174 tests 0 failures。
> 深度调查发现 3 个关键问题并修订计划：AMOLED 嵌套卡片视觉反转、padding 一致性、HierarchicalListItem API 不匹配。

> **For agentic workers:** 本计划基于 writing-plans skill 编写。Step 使用 `- [ ]` 复选框跟踪进度。每个 Task 应独立可执行、可验证、可回滚。

**Goal:** 把 KnowledgePointDetailScreen 的 InfoSection/PerspectiveCard/SourcesSection 统一到 GroupedCard/TonalCard/GroupedCardDivider 设计系统组件，并清理 4 个零引用的死组件（WenyanTopAppBar/SectionHeader/LoadingState/HierarchicalListItem），让 UI 层无重复造轮子、无死代码。

**Architecture:** 分三阶段递进——Phase 1 统一 KnowledgePointDetailScreen 的三个区块（InfoSection→GroupedCard、PerspectiveCard→TonalCardLow、SourcesSection→GroupedCardDivider）→ Phase 2 删除 4 个死组件 + 同步清理 Preview/Test → Phase 3 全量验证 + 文档更新。每个 Phase 末尾有验证关卡。

**Tech Stack:**
- Kotlin 2.3.10 / KSP 2.3.2 / Hilt 2.57.1 / Room 2.7.0
- material3 1.5.0-alpha18
- Compose BOM 2025.12.00
- Robolectric 4.13（JVM 跑 Compose UI 测试，SDK 锁 34）
- AGP 8.6.0 / Gradle 8.14.4

---

## 背景调查：当前状态与差距

### 已完成

| 组件/Screen | 状态 |
|-------------|------|
| WenyanLargeTopAppBar | ✅ 9/9 Screen 使用 |
| GroupedCard + GroupedCardItem + GroupedCardDivider | ✅ SettingsScreen（4 组）+ KnowledgePointDetailScreen（RelatedGroup）使用 |
| TonalCard / TonalCardLow | ✅ designsystem 组件就绪 |
| 4 个 @Preview（三态覆盖） | ✅ 已创建 |
| 15 个组件测试 | ✅ 全绿 |

### 差距分析（本计划要解决的）

| # | 差距 | 严重程度 | 证据 |
|---|------|---------|------|
| 1 | KnowledgePointDetailScreen 的 InfoSection 与 GroupedCard 标题区重复造轮子 | 高 | 两者都用 `titleMedium + primary` 色标题，InfoSection 是手写 Column，GroupedCard 已有标准实现 |
| 2 | PerspectiveCard 用裸 Surface 绕过 designsystem | 中 | 非 official 的 PerspectiveCard 与 TonalCardLow 的 color/shape 完全一致，却手写 Surface |
| 3 | SourcesSection 手写 HorizontalDivider 而非 GroupedCardDivider | 低 | 实现完全一致（outlineVariant + 0.5dp），但未走 designsystem 组件 |
| 4 | WenyanTopAppBar 死组件 | 中 | 0 生产引用，KSU 升级后 9/9 Screen 用 WenyanLargeTopAppBar |
| 5 | SectionHeader 死组件 | 中 | 0 引用，GroupedCard 的标题区已覆盖其场景 |
| 6 | LoadingState 死组件 | 中 | 0 引用，9 个 Screen 都手写 `Box { CircularProgressIndicator() }` |
| 7 | HierarchicalListItem 死组件 | 中 | 仅 Preview/Test 引用，0 生产引用；API 不匹配任何现有列表（无 body 内容槽） |

### 不做的事（经调查证实不合适）

- **用 HierarchicalListItem 改造多教材对照**：API 只有 title+trailing，无法承载教材正文段落（多行长文本）；多教材对照是扁平列表非树形层级。强行改造会丢失正文。
- **用 GroupedCardItem 改造 ApiConfigScreen 的 ConfigCard**：GroupedCardItem API 无法承载 4 行元信息 + 2 个操作按钮。
- **推广 LoadingState 到 9 个 Screen**：当前手写 `Box { CircularProgressIndicator() }` 虽重复但简单清晰，YAGNI 原则不强求统一。

---

## 文件结构

### 修改的文件

| 文件 | 改动 |
|------|------|
| `feature/knowledge/.../KnowledgePointDetailScreen.kt` | InfoSection→GroupedCard、PerspectiveCard→TonalCardLow、SourcesSection→GroupedCardDivider |
| `core/designsystem/.../WenyanLargeTopAppBar.kt` | 删除注释中对 WenyanTopAppBar 的引用 |

### 删除的文件

| 文件 | 原因 |
|------|------|
| `core/designsystem/.../WenyanTopAppBar.kt` | 0 生产引用 |
| `core/designsystem/.../SectionHeader.kt` | 0 引用，GroupedCard 已覆盖 |
| `core/designsystem/.../LoadingState.kt` | 0 引用 |
| `core/designsystem/.../HierarchicalListItem.kt` | 0 生产引用，API 不匹配任何现有列表 |
| `core/designsystem/.../previews/HierarchicalListItemPreview.kt` | 随组件删除 |
| `core/designsystem/src/test/.../HierarchicalListItemTest.kt` | 随组件删除 |

### 不修改的文件

- `GroupedCard.kt` / `TonalCard.kt` / `GroupedCardTest.kt` — 已就绪，无需改动
- 其他 8 个 Screen — 不在本次范围

---

## Phase 1：KnowledgePointDetailScreen 统一

### 深度调查发现的关键约束（必读）

**AMOLED 嵌套卡片视觉反转问题：**

调查 `WenyanTheme.kt` line 60-68 发现，AMOLED 模式覆盖了 `surfaceContainerLow = Color.Black`，但**未覆盖 `surfaceBright`**。

- `GroupedCard` 内部用 `TonalCard`（`surfaceBright`）→ AMOLED 下为深灰色
- `TonalCardLow` 用 `surfaceContainerLow` → AMOLED 下为纯黑

若在 `GroupedCard` 内嵌套 `TonalCardLow`（如 MultiPerspectiveSection 的 PerspectiveCard），会形成"深灰卡套纯黑卡"的视觉反转——外层比内层更亮，违反 M3 Expressive 的 elevation 层级语义。

**结论：** `MultiPerspectiveSection` 不能用 `GroupedCard` 包裹 `PerspectiveCard`（会导致嵌套卡片）。保留 `InfoSection` 无容器模式。

**padding 一致性约束：**

`GroupedCardItem` 的水平 padding 是 `Spacing.lg`（16dp）。GroupedCard 内的所有内容必须用 `horizontal = Spacing.lg` 保持左边缘对齐。

### Task 1: 摘要 + 资料来源 → GroupedCard（2 处，无嵌套风险）

**Files:**
- Modify: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt`

**改动说明：**

摘要（纯文本）和资料来源（SourceRow 无容器）改为 GroupedCard，不形成嵌套卡片。多教材对照保留 InfoSection（因其内部 PerspectiveCard 有容器，套 GroupedCard 会嵌套）。

**改动点：**

1. **摘要**（line 126-133）：
```kotlin
// 改前
InfoSection(title = "摘要") {
    Text(text = summary, style = MaterialTheme.typography.bodyMedium)
}

// 改后
GroupedCard(title = "摘要") {
    Text(
        text = summary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.md,
            bottom = Spacing.md,
        ),
    )
}
```

2. **资料来源**（line 305-316）：
```kotlin
// 改前
InfoSection(title = "资料来源（${sources.size}）") {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        sources.forEach { source ->
            SourceRow(source)
            HorizontalDivider(thickness = 0.5.dp, color = outlineVariant)
        }
    }
}

// 改后（同时做 Task 3 的 GroupedCardDivider 替换）
GroupedCard(title = "资料来源（${sources.size}）") {
    sources.forEachIndexed { index, source ->
        SourceRow(source)
        if (index < sources.size - 1) {
            GroupedCardDivider()
        }
    }
}
```

3. **保留 InfoSection 函数**（line 195-209）— 仅多教材对照 1 处使用，加注释说明保留原因

**InfoSection 加注释：**
```kotlin
/**
 * 无容器的标题区块（仅用于内部有容器的场景，避免嵌套卡片）。
 *
 * 当前仅 MultiPerspectiveSection 使用——其内部 PerspectiveCard 已有 Surface/TonalCardLow 容器，
 * 若再套 GroupedCard 的 TonalCard 会导致 AMOLED 模式下色调层级反转
 *（surfaceBright 未被 AMOLED 覆盖为 Black，而 surfaceContainerLow 被覆盖）。
 */
@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit,
) { ... }
```

- [ ] **Step 1: 摘要 InfoSection → GroupedCard（Text 加 horizontal=lg, vertical=md padding）**
- [ ] **Step 2: 资料来源 InfoSection → GroupedCard + HorizontalDivider → GroupedCardDivider**
- [ ] **Step 3: InfoSection 函数加 KDoc 注释说明保留原因**
- [ ] **Step 4: 编译验证** `:feature:knowledge:compileDebugKotlin`

### Task 2: PerspectiveCard → TonalCardLow（非 official，独立卡片不嵌套）

**Files:**
- Modify: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt`

**改动说明：**

`PerspectiveCard` 非 official 分支改用 `TonalCardLow`。由于 MultiPerspectiveSection 保留 InfoSection（无容器），PerspectiveCard 作为独立卡片呈现在页面背景上，不形成嵌套。

**改动：**

```kotlin
// 改后
@Composable
private fun PerspectiveCard(
    label: String,
    content: String,
    isOfficial: Boolean,
) {
    if (isOfficial) {
        // 答题基准（马工程）：用 primaryContainer 突出官方权威性
        // designsystem 的 TonalCard/TonalCardLow 无 primaryContainer 变体，此处保留自定义 Surface
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    } else {
        // 学习理解/多视角：用 TonalCardLow（surfaceContainerLow + shapes.medium）
        TonalCardLow(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
```

**需要新增的 import：** `TonalCardLow`

- [ ] **Step 1: 重构 PerspectiveCard — 分 isOfficial 两分支，非 official 用 TonalCardLow**
- [ ] **Step 2: 新增 TonalCardLow import**
- [ ] **Step 3: 编译验证** `:feature:knowledge:compileDebugKotlin`

### Task 3: SourceRow 加 padding

**Files:**
- Modify: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt`

**改动说明：**

SourceRow 被 GroupedCard 的 TonalCard 包裹后，需加 `horizontal=lg, vertical=md` padding 与 GroupedCardItem 对齐（Task 1 已完成 GroupedCard 包裹和 GroupedCardDivider 替换）。

**改动：**

```kotlin
// 改前
@Composable
private fun SourceRow(source: DataSourceEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // ...
    }
}

// 改后
@Composable
private fun SourceRow(source: DataSourceEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // ...
    }
}
```

**需要清理的 import：** `HorizontalDivider`（如果不再使用）、`dp`（如果不再使用）

- [ ] **Step 1: SourceRow 加 padding（start=lg, end=lg, top=md, bottom=md）**
- [ ] **Step 2: 清理 imports（HorizontalDivider、dp 如果不再使用）**
- [ ] **Step 3: 编译验证** `:feature:knowledge:compileDebugKotlin`

### Task 4: Phase 1 验证关卡

- [ ] **Step 1: 编译 feature:knowledge**
  Run: `gradle :feature:knowledge:compileDebugKotlin --no-daemon`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行现有测试确保无回归**
  Run: `gradle :core:designsystem:testDebugUnitTest --no-daemon`
  Expected: 19 tests passed, 0 failed（GroupedCard 7 + LargeTopAppBar 4 + NavigationBar 3 + HierarchicalListItem 5）

- [ ] **Step 3: Commit Phase 1**
  ```bash
  git add feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt
  git commit -m "refactor: KnowledgePointDetailScreen 摘要+资料来源统一到 GroupedCard

  - 摘要 InfoSection → GroupedCard（纯文本，无嵌套风险）
  - 资料来源 InfoSection → GroupedCard + HorizontalDivider → GroupedCardDivider
  - SourceRow 加 padding 与 GroupedCardItem 对齐（horizontal=lg, vertical=md）
  - PerspectiveCard 非 official → TonalCardLow（走 designsystem，独立卡片不嵌套）
  - 多教材对照保留 InfoSection（避免 AMOLED 嵌套卡片视觉反转）

  为什么：InfoSection 与 GroupedCard 标题区重复造轮子（都用 titleMedium+primary）。
  但 MultiPerspectiveSection 不能套 GroupedCard——AMOLED 模式下 surfaceContainerLow
  被覆盖为 Black 而 surfaceBright 未覆盖，GroupedCard 套 TonalCardLow 会形成
  '深灰卡套纯黑卡'的视觉反转。保留 InfoSection 无容器模式，加注释说明原因。"
  ```

---

## Phase 2：删除 4 个死组件

### Task 5: 删除 WenyanTopAppBar + 更新 LargeTopAppBar 注释

**Files:**
- Delete: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanTopAppBar.kt`
- Modify: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanLargeTopAppBar.kt`（删除注释中对 WenyanTopAppBar 的引用）

**WenyanLargeTopAppBar.kt 注释改动：**

```kotlin
// 改前（line 30-35）
 * - onBack 为 null 时不显示返回按钮（与 [WenyanTopAppBar] 行为一致）
 * ...
 * 与 [WenyanTopAppBar] 的关键差异：
 * - 支持滚动折叠（透传 [scrollBehavior]）
 * - 支持副标题（[subtitle]），展开时显示在标题下方
 * - 展开时为大标题样式（headlineMedium），收起时为标准标题样式

// 改后（删除两处对 WenyanTopAppBar 的引用，改为自描述）
 * - onBack 为 null 时不显示返回按钮
 * ...
 * 特性：
 * - 支持滚动折叠（透传 [scrollBehavior]）
 * - 支持副标题（[subtitle]），展开时显示在标题下方
 * - 展开时为大标题样式（headlineMedium），收起时为标准标题样式
```

- [ ] **Step 1: 删除 WenyanTopAppBar.kt**
- [ ] **Step 2: 更新 WenyanLargeTopAppBar.kt 注释（删除 2 处 WenyanTopAppBar 引用）**
- [ ] **Step 3: 编译验证** `:core:designsystem:compileDebugKotlin`

### Task 6: 删除 SectionHeader

**Files:**
- Delete: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/SectionHeader.kt`

- [ ] **Step 1: 删除 SectionHeader.kt**
- [ ] **Step 2: 编译验证** `:core:designsystem:compileDebugKotlin`

### Task 7: 删除 LoadingState

**Files:**
- Delete: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/LoadingState.kt`

- [ ] **Step 1: 删除 LoadingState.kt**
- [ ] **Step 2: 编译验证** `:core:designsystem:compileDebugKotlin`

### Task 8: 删除 HierarchicalListItem + Preview + Test

**Files:**
- Delete: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/HierarchicalListItem.kt`
- Delete: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/HierarchicalListItemPreview.kt`
- Delete: `core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/HierarchicalListItemTest.kt`

- [ ] **Step 1: 删除 HierarchicalListItem.kt**
- [ ] **Step 2: 删除 HierarchicalListItemPreview.kt**
- [ ] **Step 3: 删除 HierarchicalListItemTest.kt**
- [ ] **Step 4: 编译验证** `:core:designsystem:compileDebugKotlin`

### Task 9: Phase 2 验证关卡

- [ ] **Step 1: 编译 core:designsystem**
  Run: `gradle :core:designsystem:compileDebugKotlin --no-daemon`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行 designsystem 测试**
  Run: `gradle :core:designsystem:testDebugUnitTest --no-daemon`
  Expected: 14 tests passed, 0 failed（删除了 HierarchicalListItem 的 5 个测试，19-5=14）

- [ ] **Step 3: Commit Phase 2**
  ```bash
  git add -A core/designsystem/
  git commit -m "refactor: 删除 4 个零引用死组件

  删除：
  - WenyanTopAppBar（KSU 升级后 9/9 Screen 用 WenyanLargeTopAppBar，0 引用）
  - SectionHeader（GroupedCard 标题区已覆盖，0 引用）
  - LoadingState（9 个 Screen 都手写 Box{CircularProgressIndicator()}，0 引用）
  - HierarchicalListItem（API 只有 title+trailing，不匹配任何现有列表，0 生产引用）
    + 同步删除 HierarchicalListItemPreview（3 个 @Preview）
    + 同步删除 HierarchicalListItemTest（5 个测试）

  为什么：死代码增加 designsystem 表面积和认知负担。HierarchicalListItem 经调查
  证实 API 与所有现有列表不匹配（无 body 内容槽，数据非树形），保留无意义。
  WenyanLargeTopAppBar 注释中对 WenyanTopAppBar 的引用已清理。"
  ```

---

## Phase 3：全量验证 + 文档更新

### Task 10: 全量编译 + 测试

- [ ] **Step 1: assembleDebug**
  Run: `gradle assembleDebug --no-daemon`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 2: testDebugUnitTest（先确认基线再验证）**
  Run: `gradle testDebugUnitTest --no-daemon`
  Expected: BUILD SUCCESSFUL，0 failures
  注意：实施前先跑一次 testDebugUnitTest 记录基线测试数（上次为 117，但 core/ai 可能有新增测试），删除 5 个 HierarchicalListItem 测试后预期 = 基线 - 5

### Task 11: 推送 + CI 验证

- [ ] **Step 1: git push origin main**
- [ ] **Step 2: 等待 CI 运行完成（约 20 分钟）**
- [ ] **Step 3: 验证 CI 全绿** `gh run view --repo qbjsdsb/wenyan-android`

### Task 12: 更新文档

**Files:**
- Modify: `docs/00-STATUS.md`（新增"UI 统一与死组件清理"章节 + 删除 line 66 的 HierarchicalListItem P1 条目）
- Modify: `docs/SESSION_LOG.md`（新增 Session 2026-07-13 第二条记录）
- Modify: `AGENTS.md`（修订第 9 节下一步优先级，删除已完成的 P1）
- Modify: `docs/01-QUICK-RECOVERY.md`（更新 line 121-123 的剩余工作描述，删除 HierarchicalListItem 引用）
- Modify: `docs/plans/ui-consolidation-cleanup.md`（顶部标记完成）

- [ ] **Step 1: 更新 00-STATUS.md** — 新增"UI 统一与死组件清理"章节 + 删除 HierarchicalListItem P1 条目
- [ ] **Step 2: 更新 SESSION_LOG.md** — 新增 Session 2026-07-13（第二条）记录
- [ ] **Step 3: 更新 AGENTS.md** — 修订第 9 节下一步优先级（删除已完成的 P1，修正原 P1 描述）
- [ ] **Step 4: 更新 01-QUICK-RECOVERY.md** — 更新剩余工作描述，删除 HierarchicalListItem 引用
- [ ] **Step 5: 更新 plan 文件** — 顶部标记为已完成
- [ ] **Step 6: Commit 文档**
- [ ] **Step 7: Push 文档 commit**

---

## 自检清单

### 计划完整性
- [x] 每个 Task 有明确的文件路径
- [x] 每个 Task 有具体的代码改动（非占位符）
- [x] 每个 Phase 末尾有验证关卡
- [x] 每个 Task 末尾有编译验证
- [x] Commit message 说明"为什么改"

### 深度调查发现的关键约束（已纳入计划）
- [x] AMOLED 嵌套卡片视觉反转（surfaceBright 未被覆盖为 Black，surfaceContainerLow 被覆盖）→ MultiPerspectiveSection 保留 InfoSection
- [x] padding 一致性（GroupedCardItem 水平 padding = lg=16dp）→ 摘要 Text 和 SourceRow 都用 horizontal=lg
- [x] 测试数基线不确定（core/ai 可能有新增测试）→ 实施前先跑 testDebugUnitTest 确认基线
- [x] 文档次生影响（00-STATUS.md + 01-QUICK-RECOVERY.md 引用 HierarchicalListItem）→ Task 12 补充更新

### 风险评估
- **Phase 1 风险：低** — 摘要/资料来源→GroupedCard 是等价替换（标题样式一致），PerspectiveCard→TonalCardLow 是等价替换（color/shape 一致），SourcesSection→GroupedCardDivider 是等价替换（实现完全一致）；多教材对照保留 InfoSection 避免嵌套风险
- **Phase 2 风险：极低** — 4 个组件均 0 生产引用，删除不影响任何 Screen
- **回滚方案** — 每个 Phase 独立 commit，可 `git revert` 单个 Phase

### 预期结果
- 删除 4 个文件（死组件）+ 2 个关联文件（Preview/Test）
- 修改 2 个文件（KnowledgePointDetailScreen + WenyanLargeTopAppBar 注释）
- 测试数：基线 - 5（删除 5 个 HierarchicalListItem 测试，实施前先跑 testDebugUnitTest 确认基线）
- CI 全绿
