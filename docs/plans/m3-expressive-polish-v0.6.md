# M3 Expressive 精修计划 v0.6（导航重构 + 味道提升 + 大屏适配）

> **创建时间**：2026-07-15
> **状态**：待用户审阅（已确认大屏适配纳入实施）
> **前置文档**：[m3-expressive-redesign.md](../design/m3-expressive-redesign.md) / [ksu-ui-upgrade.md](ksu-ui-upgrade.md)
> **调研依据**：两份调研报告 + M3 Adaptive 1.2.0 / WideNavigationRail API Web 调研

---

## 1. 背景与目标

### 1.1 用户反馈

> "这个软件整体的 ui 我感觉还是不够有 m3express 的味道，还有就是右上角已经有了 ai 助手，最底部右边的 ai 就可以砍掉，做成设置界面了。"
> "都按照你的建议做，不过大屏我希望可以做，因为我还要在平板上面用，继续打磨"

三个明确诉求：
1. **M3 Expressive 味道不足** — 提升动效/字体/组件的"谷歌味道"
2. **导航重构** — 底部第 5 个 Tab 从"AI助手"改为"设置"，AI 统一走右上角入口
3. **大屏适配** — 用户要在平板上用，需要自适应布局

### 1.2 现状诊断（调研结论）

#### 已实现（做得好的部分）
- `MaterialExpressiveTheme` + `MotionScheme.expressive()` 已注入主题
- 动态色彩（Android 12+）+ materialkolor SPEC_2025 + AMOLED 纯黑模式
- 完整 15 样式 Typography + 5 级 Shapes + 6 级 Spacing
- 8 个 Screen 统一用 `WenyanLargeTopAppBar`（LargeFlexibleTopAppBar）
- GroupedCard / TonalCard / WenyanInfoChip / ContentSourceBadge 组件化
- GraphCanvas 颜色已全面主题化（无硬编码 COLOR_GREEN/RED 等）

#### 主要差距（影响"味道"与"大屏体验"的关键项）
| 差距 | 现状 | 理想状态 | 影响 |
|------|------|----------|------|
| 颜色切换无动画 | 主题/AMOLED/种子色切换瞬间跳变 | `animateColorAsState(spring)` 平滑过渡 | 高 |
| 导航过渡全用 tween | Tab/Push/Pop 用 CubicBezier，无弹簧 | 关键交互用弹簧物理 | 高 |
| 字重对比不足 | 15 样式全用 Normal/Medium | Display/Headline 用 Bold/SemiBold | 高 |
| **无大屏自适应** | **全设备统一底部 NavigationBar** | **WindowSizeClass 驱动 NavigationBar ↔ WideNavigationRail** | **高** |
| **无 ListDetail 双栏** | **知识点列表→详情是 Push 跳转** | **大屏 ListDetailPaneScaffold 双栏并排** | **高** |
| CircularProgressIndicator | 普通 M3 加载器 | Expressive `LoadingIndicator` | 中 |
| FilterChip 选主题模式 | 3 个 FilterChip 横排 | `SingleChoiceSegmentedButtonRow` | 中 |
| 形状全等圆角 | 5 级全 RoundedCornerShape | 增加不对称圆角变体 | 中 |
| 无共享元素过渡 | 列表→详情是 push/slide | SharedTransitionLayout 飞行动画 | 中 |
| 缺失组件 | HierarchicalListItem/LoadingState/SectionHeader 未实现 | 补齐统一组件 | 中 |
| Preview 覆盖不全 | 仅 3 个 Preview | 浅色/深色/AMOLED 三态覆盖 | 低 |

#### 导航结构问题
- 底部 5 Tab：`知识点/真题/卡片/图谱/AI助手` — AI 助手是工具性质，不应占顶级 Tab
- 4 个 Tab 右上角已有 SmartToy 入口 → 底部 AI Tab 冗余
- SettingsScreen 是子路由，需从 AiAssistantScreen 内部 MoreVert 进入 → 链路深
- AiAssistantScreen 4 个 actions（MoreVert/Settings/CloudOff/Delete）过度拥挤，MoreVert 语义错位

### 1.3 目标

1. **导航重构**：底部 Tab 改为 `知识点/真题/卡片/图谱/设置`，AI 统一走右上角
2. **味道提升**：颜色过渡动画 + 字重对比 + 弹簧动效，让 UI "活起来"
3. **大屏适配**：WindowSizeClass 驱动自适应布局，平板用 WideNavigationRail + ListDetailPaneScaffold
4. **组件升级**：用 M3 Expressive 原生组件替代手写/普通 M3 组件
5. **一致性**：补齐缺失组件，统一加载/标题/层级表达

---

## 2. 改造范围与优先级

| Phase | 主题 | 优先级 | 预期效果 | 风险 |
|-------|------|--------|----------|------|
| Phase 1 | 导航重构 | P0（用户明确要求） | 底部 Tab 砍 AI 改设置，AI 入口统一 | 低 |
| Phase 2 | 动效 + 字体 | P0（最能提升味道） | 颜色过渡 + 弹簧 + 字重张力 | 低 |
| Phase 3 | 大屏适配 | P0（用户明确要求） | WindowSizeClass 自适应导航 + 双栏布局 | 中 |
| Phase 4 | 组件升级 | P1 | LoadingIndicator/SegmentedButton/统一组件 | 中 |
| Phase 5 | 视觉精修 | P2 | 形状变体/共享元素/Preview | 中 |

---

## 3. Phase 1：导航重构（P0）

### 3.1 目标

- 底部 Tab 从 `知识点/真题/卡片/图谱/AI助手` 改为 `知识点/真题/卡片/图谱/设置`
- AI 助手不再占顶级 Tab，统一从 4 个 Tab 右上角 SmartToy 进入
- SettingsScreen 从子路由提升为顶级 Tab
- AiAssistantScreen actions 重构（移除 MoreVert→Settings，整理 CloudOff/Delete）

### 3.2 任务清单

#### 任务 1.1：TopLevelDestination 调整
**文件**：`app/src/main/java/com/wenyan/app/navigation/TopLevelDestination.kt`

- 删除 `AiAssistant` data object（不再是顶级目的地）
- 新增 `Settings` data object：
  ```kotlin
  data object Settings : TopLevelDestination(
      route = ROUTE_SETTINGS,
      label = "设置",
      icon = Icons.Filled.Settings,
  )
  ```
- `destinations` 列表改为 `[Knowledge, Quiz, Cards, Graph, Settings]`
- 保留 `ROUTE_AI_ASSISTANT` 常量（AI 助手仍作为可导航目的地，只是不在底部栏）

#### 任务 1.2：WenyanApp 底部栏显示逻辑
**文件**：`app/src/main/java/com/wenyan/app/WenyanApp.kt`

- `showBottomBar` 逻辑调整：
  - 移除 `currentRoute != TopLevelDestination.ROUTE_AI_ASSISTANT` 排除条件（AI 助手不再是顶级路由，自动不显示底部栏）
  - 设置页 `ROUTE_SETTINGS` 在 `topLevelRoutes` 中，会显示底部栏 ✓
- 进入 AI 助手（非顶级路由）时不显示底部栏（`ROUTE_AI_ASSISTANT` 不在 `topLevelRoutes` 中，自动满足）

#### 任务 1.3：WenyanNavHost 路由调整
**文件**：`app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`

- `settingsDestination` 从子路由（Push/Pop slide）改为顶级路由（Tab fade）
  - 移除 `enterTransition/exitTransition/popEnterTransition/popExitTransition = WenyanMotion.Push*`
  - 改用 NavHost 默认的 Tab fade（与 knowledge/quiz/cards/graph 一致）
  - 移除 `onBack` 参数（顶级 Tab 无需返回箭头）
- `aiAssistantDestination` 从顶级 Tab 改为子路由（Push/Pop slide）
  - 添加 `enterTransition = WenyanMotion.PushEnterTransition` 等
- 4 个 Tab 的 `onNavigateToAiAssistant` 改为子路由跳转（Push/Pop slide + launchSingleTop）
- `aiAssistantDestination` 的 `onNavigateToSettings` 移除（设置是底部 Tab）
  - 保留 `onNavigateToApiConfig`（API 配置仍是子路由）

#### 任务 1.4：SettingsScreen 适配顶级 Tab
**文件**：`feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`

- 移除 `onBack` 参数（顶级 Tab 无返回箭头）
- TopBar 的 `navigationIcon` 移除
- 保留 `onNavigateToApiConfig`（API 配置仍是子路由）

#### 任务 1.5：AiAssistantScreen actions 重构
**文件**：`feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantScreen.kt`

当前 4 个 actions：
1. MoreVert → onNavigateToSettings ❌（设置变顶级 Tab，移除）
2. Settings 图标 → onNavigateToApiConfig（保留，改图标）
3. CloudOff → 不可点击（改为可点击 IconButton → onNavigateToApiConfig）
4. Delete → clearMessages（保留）

重构后 actions：
1. `CloudOff`（条件 `!isAvailable`）→ IconButton，onClick = onNavigateToApiConfig，tint = error
2. `Delete` → IconButton，onClick = clearMessages，enabled = messages.isNotEmpty()
3. `MoreVert` → DropdownMenu（溢出菜单），包含"API 配置"项

移除的参数：
- `onNavigateToSettings`（设置已是顶级 Tab）

保留的参数：
- `onNavigateToApiConfig`

#### 任务 1.6：AiAssistantScreen 路由参数清理
**文件**：`app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`

- `aiAssistantDestination` 签名从 `(onNavigateToApiConfig, onNavigateToSettings)` 改为 `(onNavigateToApiConfig)`
- AiAssistantScreen 调用处移除 `onNavigateToSettings`

### 3.3 预期效果

- 底部导航 5 Tab：`知识点 / 真题 / 卡片 / 图谱 / 设置`，最右是设置（符合用户习惯）
- AI 助手从任意 Tab 右上角 SmartToy 进入，push 动画
- AiAssistantScreen 顶部 actions 简洁：CloudOff（条件）/ Delete / MoreVert（溢出）
- 设置页直接从底部进入，无需绕道 AI 助手

### 3.4 风险

- **低**：导航重构是纯结构性改动，有现有测试覆盖（220 tests）
- **注意**：AiAssistantScreen 从顶级路由改为子路由后，`WenyanApp.showBottomBar` 逻辑需确认不会误显示底部栏

---

## 4. Phase 2：动效 + 字体（P0）

### 4.1 目标

让 UI "活起来"：颜色切换有过渡、关键交互有弹簧、字体有张力。

### 4.2 任务清单

#### 任务 2.1：颜色切换动画
**文件**：`core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/WenyanTheme.kt`

- 当前 `finalScheme` 直接传给 `MaterialExpressiveTheme`，切换主题/AMOLED/种子色时色彩瞬间跳变
- 改用 `animateColorAsState` 对 colorScheme 的每个颜色角色做过渡：
  ```kotlin
  val animatedScheme = finalScheme.copy(
      primary = animateColorAsState(finalScheme.primary, spring(dampingRatio = 0.8f, stiffness = StiffnessLow)).value,
      // ... 对所有颜色角色做动画
  )
  ```
- **简化方案**：用 `materialkolor` 的 `animateColorSchemeAsState`（如可用）或手写扩展函数批量动画
- 弹簧参数：`dampingRatio = 0.8f`（轻微过冲）+ `stiffness = StiffnessLow`（缓慢）

#### 任务 2.2：导航过渡引入弹簧
**文件**：`core/designsystem/src/main/java/com/wenyan/app/core/designsystem/motion/WenyanMotion.kt`

- 当前 Tab/Push/Pop 过渡全用 `tween` + CubicBezier
- 评估方案：
  - **方案 A（推荐）**：Push/Pop 改用 `spring(dampingRatio = 0.8f, stiffness = StiffnessMediumLow)`，保留 Tab fade（避免与 NavigationBar indicator 冲突）
  - **方案 B**：全部改弹簧（Tab 也用 spring fade）
- 保留 `tween` 用于精确时序场景（如 SharedTransition）

#### 任务 2.3：Typography 字重对比
**文件**：`core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Type.kt`

- 当前 15 样式全用 Normal/Medium
- 调整：
  - `displayLarge/Medium/Small`：Normal → **SemiBold**（或 Bold）
  - `headlineLarge/Medium/Small`：Normal → **SemiBold**
  - `titleLarge`：Medium（保持）
  - `bodyLarge/Medium/Small`：Normal（保持）
  - `labelLarge/Medium/Small`：Medium（保持）
- **注意**：中文字重渲染依赖系统字体，部分设备可能无 SemiBold，需测试回退效果

#### 任务 2.4：NavigationBar 指示器弹簧动效
**文件**：`core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanNavigationBar.kt`

- 当前药丸指示器用默认动画
- 确认 `MotionScheme.expressive()` 是否已自动作用于 NavigationBar indicator
- 若未生效，手动为 indicator 的 offset/width 加 `spring(dampingRatio = 0.7f, stiffness = StiffnessMedium)`

### 4.3 预期效果

- 切换主题/AMOLED/种子色时，颜色平滑过渡（约 300-500ms），不再瞬间跳变
- 页面 Push/Pop 有轻微过冲回弹，更"有生命感"
- 大标题（Display/Headline）字重更重，与小字（Body）形成视觉张力
- 底部导航 Tab 切换时药丸指示器有弹性滑动

### 4.4 风险

- **颜色动画性能**：对 40+ 颜色角色同时 animateColorAsState 可能掉帧，需用 `derivedStateOf` 或批量动画优化
- **中文字重回退**：部分 Android 设备无 SemiBold 字重，会回退到 Normal，效果打折（可接受）
- **弹簧参数调校**：过冲过大可能让用户晕眩，需在 emulator 实测

---

## 5. Phase 3：大屏适配（P0）

### 5.1 目标

让 App 在平板上有原生体验：导航栏自适应、列表详情双栏、内容布局充分利用大屏空间。

### 5.2 技术方案

#### 5.2.1 WindowSizeClass 分级策略

| 宽度 | 等级 | 导航形态 | 布局策略 |
|------|------|----------|----------|
| < 600dp | Compact | 底部 NavigationBar（现有） | 单栏，Push 跳转详情 |
| 600-840dp | Medium | WideNavigationRail（折叠态） | 单栏或紧凑双栏 |
| ≥ 840dp | Expanded | WideNavigationRail（展开态） | ListDetailPaneScaffold 双栏 |

#### 5.2.2 依赖新增

**文件**：`gradle/libs.versions.toml`

```toml
[versions]
material3Adaptive = "1.2.0"  # 2025-10 稳定版
windowManager = "1.5.0"      # adaptive 1.2.0 依赖

[libraries]
androidx-compose-material3-adaptive = { group = "androidx.compose.material3.adaptive", name = "adaptive", version.ref = "material3Adaptive" }
androidx-compose-material3-adaptive-layout = { group = "androidx.compose.material3.adaptive", name = "adaptive-layout", version.ref = "material3Adaptive" }
androidx-compose-material3-adaptive-navigation = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation", version.ref = "material3Adaptive" }
androidx-window = { group = "androidx.window", name = "window", version.ref = "windowManager" }
```

**说明**：
- `WideNavigationRail` / `WideNavigationRailItem` 已在 material3 1.5.0-alpha18 中（`@ExperimentalMaterial3ExpressiveApi`），无需额外依赖
- `material3-adaptive` 1.2.0 提供 `ListDetailPaneScaffold` / `WindowSizeClass` / `rememberPaneExpansionState`
- `adaptive-navigation` 提供 `NavigableListDetailPaneScaffold`（与 NavHost 集成）

#### 5.2.3 版本兼容性说明

- material3-adaptive 1.2.0 是独立库，与 material3 1.5.0-alpha18 兼容（adaptive 不依赖 material3 的实验性 API）
- `WideNavigationRail` 来自 material3 1.5.0-alpha18，是 `@ExperimentalMaterial3ExpressiveApi`，需 `@OptIn`
- WindowManager 1.5.0 是 adaptive 1.2.0 的硬依赖

### 5.3 任务清单

#### 任务 3.1：新增 adaptive 依赖
**文件**：`gradle/libs.versions.toml` + 各模块 `build.gradle.kts`

- 在 `libs.versions.toml` 添加上述依赖声明
- `app/build.gradle.kts` 添加 `implementation(libs.androidx.compose.material3.adaptive)` 等
- `core/designsystem/build.gradle.kts` 添加 adaptive 依赖（WideNavigationRail 封装在此）

#### 任务 3.2：WenyanAdaptiveNavigation 组件封装
**文件**：新建 `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanAdaptiveNavigation.kt`

封装 WindowSizeClass 驱动的自适应导航容器：

```kotlin
@Composable
fun WenyanAdaptiveNavigation(
    items: List<WenyanNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    when (windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            // 手机：底部 NavigationBar（现有 WenyanNavigationBar）
            ExpressiveScaffold(bottomBar = { WenyanNavigationBar(...) }) { content(it) }
        }
        WindowWidthSizeClass.MEDIUM -> {
            // 小平板/折叠屏：WideNavigationRail 折叠态
            Row {
                WenyanWideNavigationRail(expanded = false, items, currentRoute, onNavigate)
                ExpressiveScaffold { content(it) }
            }
        }
        else -> {
            // 大平板/桌面：WideNavigationRail 展开态
            Row {
                WenyanWideNavigationRail(expanded = true, items, currentRoute, onNavigate)
                ExpressiveScaffold { content(it) }
            }
        }
    }
}
```

#### 任务 3.3：WenyanWideNavigationRail 组件封装
**文件**：新建 `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanWideNavigationRail.kt`

封装 M3 Expressive 的 `WideNavigationRail`：

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WenyanWideNavigationRail(
    expanded: Boolean,
    items: List<WenyanNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val railState = rememberWideNavigationRailState(
        initialValue = if (expanded) Expanded else Collapsed,
    )
    WideNavigationRail(
        state = railState,
        modifier = modifier.fillMaxHeight(),
        header = { /* 可选：FAB 或 Logo */ },
    ) {
        items.forEach { item ->
            WideNavigationRailItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                railExpanded = railState.targetValue == Expanded,
            )
        }
    }
}
```

#### 任务 3.4：WenyanApp 接入自适应导航
**文件**：`app/src/main/java/com/wenyan/app/WenyanApp.kt`

- 将现有的 `ExpressiveScaffold + WenyanNavigationBar` 替换为 `WenyanAdaptiveNavigation`
- `WenyanAdaptiveNavigation` 内部根据 WindowSizeClass 决定用底部栏还是侧边栏
- `showBottomBar` 逻辑移到 `WenyanAdaptiveNavigation` 内部（仅 Compact 模式显示底部栏）

#### 任务 3.5：KnowledgeScreen ListDetailPaneScaffold 双栏
**文件**：
- `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt`
- `app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`

大屏（Expanded）下，知识点列表与详情并排显示：

```kotlin
@Composable
fun KnowledgeScreen(
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    if (windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) {
        // 大屏：双栏并排
        KnowledgeListDetailPaneScaffold(
            onNavigateToAiAssistant = onNavigateToAiAssistant,
            onNavigateToDetail = onNavigateToDetail,
        )
    } else {
        // 手机：单栏 + Push 跳转（现有逻辑）
        KnowledgeSinglePaneScreen(...)
    }
}
```

**实现要点**：
- 用 `NavigableListDetailPaneScaffold`（adaptive-navigation）与 NavHost 集成
- `listPane`：知识点列表
- `detailPane`：知识点详情（选中项）
- `rememberPaneExpansionState()` 设置初始比例（如列表 40% + 详情 60%）
- 手机模式下退化为单栏 + Push 跳转（现有行为）

#### 任务 3.6：SettingsScreen 大屏适配
**文件**：`feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`

- 大屏下设置项可用两列 Grid 布局（外观/动态色彩一列，AI 服务/关于一列）
- 或保持单列但宽度限制（maxWidth = 600dp 居中），避免大屏拉伸过宽

#### 任务 3.7：GraphScreen 大屏 Canvas 优化
**文件**：`feature/graph/src/main/java/com/wenyan/app/feature/graph/GraphScreen.kt` + `GraphCanvas.kt`

- 大屏下 Canvas 区域更宽，节点圆形布局半径可增大（当前硬编码 0.35f 比例）
- 图例可从底部移到右侧侧边栏
- 评估：大屏是否显示更多节点（当前可能有限制）

#### 任务 3.8：AiAssistantScreen 大屏分栏
**文件**：`feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantScreen.kt`

- 大屏下：对话区（左 70%）+ 引用列表区（右 30%）并排
- `ReferencesList` 从消息气泡内提取到独立右侧面板
- 手机模式下：引用列表仍在消息气泡内（现有行为）

### 5.4 预期效果

**手机（Compact < 600dp）**：
- 底部 NavigationBar（不变）
- 单栏布局（不变）
- 知识点详情 Push 跳转（不变）

**小平板/折叠屏（Medium 600-840dp）**：
- 左侧 WideNavigationRail（折叠态，仅图标）
- 单栏布局
- 知识点详情 Push 跳转

**大平板/桌面（Expanded ≥ 840dp）**：
- 左侧 WideNavigationRail（展开态，图标+标签）
- 知识点列表+详情双栏并排（ListDetailPaneScaffold）
- AI 助手对话+引用分栏
- 图谱 Canvas 充分利用空间

### 5.5 风险

- **依赖兼容性**：material3-adaptive 1.2.0 与 material3 1.5.0-alpha18 需验证兼容性（adaptive 相对独立，风险低）
- **NavigableListDetailPaneScaffold 与 NavHost 集成**：需要重构导航逻辑，复杂度较高
- **状态管理**：双栏模式下选中项的状态同步（列表选中 → 详情更新）
- **测试覆盖**：需在 emulator 多种屏幕尺寸下测试（手机/小平板/大平板/横竖屏）

---

## 6. Phase 4：组件升级（P1）

### 6.1 目标

用 M3 Expressive 原生组件替代手写/普通 M3 组件，提升一致性与"味道"。

### 6.2 任务清单

#### 任务 4.1：LoadingIndicator 替代 CircularProgressIndicator
**文件**：
- 新建 `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/LoadingState.kt`
- 修改各 Screen 的加载态（AiAssistantScreen / KnowledgeScreen / QuizScreen / CardsScreen / GraphScreen）

- M3 Expressive 的 `LoadingIndicator`（实验性）替代标准 `CircularProgressIndicator`
- 封装为 `LoadingState` 组件，统一加载态表达
- 各 Screen 用 `LoadingState` 替代散落的 `CircularProgressIndicator`

#### 任务 4.2：SegmentedButton 替代 FilterChip（主题模式选择）
**文件**：`feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`

- 当前主题模式（系统/浅色/深色）用 3 个 FilterChip 横排
- 改用 `SingleChoiceSegmentedButtonRow` + `SegmentedButton`
- 视觉更紧凑，符合 M3 Expressive 互斥选择惯例

#### 任务 4.3：HierarchicalListItem 实现
**文件**：新建 `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/HierarchicalListItem.kt`

- ksu-ui-upgrade.md Task 1.4 规划但未实现
- 用于 KnowledgePointDetailScreen 的"前置/关联/后置知识"树形结构
- 左侧缩进 + 圆点节点表达父子层级
- 支持 leadingIcon / title / subtitle / trailing / onClick

#### 任务 4.4：SectionHeader 实现
**文件**：新建 `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/SectionHeader.kt`

- m3-expressive-redesign.md §4.1.7 规划但未实现
- 统一区块标题表达（title + optional action）
- 用于 SettingsScreen / KnowledgePointDetailScreen 等需要分区的页面

#### 任务 4.5：SettingsScreen 用 GroupedCard 重构
**文件**：`feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`

- 当前已用 GroupedCard，但内部结构混乱（主题模式 item + divider + chips Row 分裂）
- 重构：
  - 主题模式：GroupedCardItem（subtitle 显示当前模式）→ 点击展开 SegmentedButton
  - 种子色：FlowRow 替代 Row（避免窄屏溢出）+ 加 contentDescription 修复无障碍
  - AMOLED：根据 colorMode 禁用（浅色模式下无意义）

#### 任务 4.6：AiAssistantScreen 消息气泡用 Surface
**文件**：`feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantScreen.kt`

- 当前 MessageBubble 用 `clip + background + padding` 手写
- 改用 M3 `Surface(color = ..., contentColor = ..., shape = ...)` 获得 tonal elevation + ripple
- RoteWarningBanner 同理改用 Surface 或 M3 Alert

### 6.3 预期效果

- 加载态统一用 Expressive LoadingIndicator，动效更丰富
- 主题模式选择用 SegmentedButton，视觉更紧凑专业
- 知识点详情的关联知识用 HierarchicalListItem，层级感清晰
- 消息气泡用 Surface，获得标准 tonal elevation 与触控反馈

### 6.4 风险

- **LoadingIndicator 实验性**：alpha18 仍 `@ExperimentalMaterial3ExpressiveApi`，需 `@OptIn`
- **SegmentedButton 中文的宽度**：3 个选项"跟随系统/浅色/深色"可能需要测宽
- **HierarchicalListItem 设计**：需参考 KSU 实现，避免过度设计

---

## 7. Phase 5：视觉精修（P2）

### 7.1 目标

锦上添花，提升形态多样性与过渡连续性。

### 7.2 任务清单

#### 任务 5.1：形状变体
**文件**：`core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Shapes.kt`

- 当前 5 级全 RoundedCornerShape（四角等圆）
- 增加形状变体：
  - `Shapes.bottomSheetShape`：顶部 extraLarge + 底部 0dp（从底部弹出的 Sheet）
  - `Shapes.cardExpandedShape`：顶部 large + 底部 0dp（展开的卡片）
- 评估 Squircle（超椭圆）用于 FAB（可选，需自定义 Shape）

#### 任务 5.2：共享元素过渡（列表→详情）
**文件**：
- `app/src/main/java/com/wenyan/app/WenyanApp.kt`（SharedTransitionLayout 包裹）
- `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt`（列表项 SharedElement）
- `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt`（详情标题 SharedElement）

- 用 `SharedTransitionLayout` + `SharedElement` 让列表卡片标题"飞行"到详情页
- M3 Expressive 标志性体验，但实现复杂度高
- **决策点**：是否值得投入？建议作为可选任务

#### 任务 5.3：GraphCanvas alpha 主题化
**文件**：`feature/graph/src/main/java/com/wenyan/app/feature/graph/ui/GraphCanvas.kt`

- `weakHaloColor` alpha = 0.2f / `weakEdgeColor` alpha = 0.6f 硬编码
- 改用 `colorScheme.errorContainer`（已含 alpha 处理）或定义主题 token
- 验证 AMOLED 纯黑模式下薄弱光晕可见性

#### 任务 5.4：Preview 补齐
**文件**：`core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/`

- 当前仅 3 个 Preview（GroupedCard / WenyanNavigationBar / WenyanLargeTopAppBar）
- 补齐：EmptyState / WenyanInfoChip / ContentSourceBadge / TonalCard / LoadingState / HierarchicalListItem / SectionHeader / WenyanWideNavigationRail
- 每个 Preview 提供浅色/深色/AMOLED 三态
- 新增大屏布局 Preview（WideNavigationRail 展开态）

### 7.3 预期效果

- 底部弹出的 Sheet 有不对称圆角，更"Expressive"
- 列表→详情有共享元素飞行动画（如实施）
- 图谱薄弱光晕在 AMOLED 模式下可见
- 组件 Preview 覆盖完整，支持设计协作

### 7.4 风险

- **共享元素过渡复杂**：SharedTransition API 仍实验性，与 NavHost 集成有坑
- **形状变体一致性**：增加变体后需确保各组件用对形状

---

## 8. 实施顺序与依赖

```
Phase 1（导航重构）          ← 用户明确要求，先做
  ├─ 1.1 TopLevelDestination
  ├─ 1.2 WenyanApp
  ├─ 1.3 WenyanNavHost
  ├─ 1.4 SettingsScreen
  ├─ 1.5 AiAssistantScreen actions
  └─ 1.6 路由参数清理
       ↓
Phase 2（动效 + 字体）        ← 最能提升味道
  ├─ 2.1 颜色切换动画
  ├─ 2.2 导航过渡弹簧
  ├─ 2.3 Typography 字重
  └─ 2.4 NavigationBar 指示器
       ↓
Phase 3（大屏适配）           ← 用户明确要求
  ├─ 3.1 新增 adaptive 依赖
  ├─ 3.2 WenyanAdaptiveNavigation 封装
  ├─ 3.3 WenyanWideNavigationRail 封装
  ├─ 3.4 WenyanApp 接入自适应导航
  ├─ 3.5 KnowledgeScreen 双栏
  ├─ 3.6 SettingsScreen 大屏适配
  ├─ 3.7 GraphScreen Canvas 优化
  └─ 3.8 AiAssistantScreen 分栏
       ↓
Phase 4（组件升级）           ← 一致性
  ├─ 4.1 LoadingIndicator
  ├─ 4.2 SegmentedButton
  ├─ 4.3 HierarchicalListItem
  ├─ 4.4 SectionHeader
  ├─ 4.5 SettingsScreen 重构
  └─ 4.6 消息气泡 Surface
       ↓
Phase 5（视觉精修）           ← 锦上添花
  ├─ 5.1 形状变体
  ├─ 5.2 共享元素过渡（可选）
  ├─ 5.3 GraphCanvas alpha
  └─ 5.4 Preview 补齐
```

**依赖关系**：
- Phase 2 不依赖 Phase 1（可并行，但建议先做 Phase 1 稳定导航）
- **Phase 3 依赖 Phase 1**：自适应导航需要新的 TopLevelDestination（Settings 替代 AiAssistant）
- Phase 3.5（KnowledgeScreen 双栏）依赖 Phase 1.3（路由调整）
- Phase 4.5（SettingsScreen 重构）依赖 Phase 1.4（SettingsScreen 适配顶级 Tab）+ Phase 3.6（大屏适配）
- Phase 5.2（共享元素）依赖 Phase 2.2（弹簧动效）

---

## 9. 验收标准

### 9.1 Phase 1 验收
- [ ] 底部导航显示 `知识点 / 真题 / 卡片 / 图谱 / 设置` 5 个 Tab
- [ ] 点击底部"设置"Tab 直接进入 SettingsScreen，无返回箭头
- [ ] 4 个 Tab 右上角 SmartToy 点击进入 AiAssistantScreen，Push 动画
- [ ] AiAssistantScreen 顶部 actions ≤ 3 个，无 MoreVert→Settings 语义错位
- [ ] AiAssistantScreen 的 CloudOff 可点击跳转 ApiConfig
- [ ] `assembleDebug` + `testDebugUnitTest` 全绿

### 9.2 Phase 2 验收
- [ ] 切换主题模式（系统/浅色/深色）时，颜色平滑过渡（非瞬间跳变）
- [ ] 切换 AMOLED 模式时，黑色过渡平滑
- [ ] 切换种子色时，色板平滑过渡
- [ ] 页面 Push/Pop 有轻微弹簧感（非生硬 slide）
- [ ] Display/Headline 标题字重明显加重
- [ ] NavigationBar Tab 切换时药丸指示器有弹性
- [ ] emulator 实测无明显掉帧

### 9.3 Phase 3 验收（大屏适配）
**手机（Compact < 600dp）**：
- [ ] 底部 NavigationBar 显示（与现有行为一致）
- [ ] 单栏布局，知识点详情 Push 跳转
- [ ] 无 WideNavigationRail

**小平板/折叠屏（Medium 600-840dp）**：
- [ ] 左侧 WideNavigationRail 折叠态（仅图标）
- [ ] 单栏布局
- [ ] NavigationBar 不显示

**大平板/桌面（Expanded ≥ 840dp）**：
- [ ] 左侧 WideNavigationRail 展开态（图标+标签）
- [ ] 知识点列表+详情双栏并排（ListDetailPaneScaffold）
- [ ] AI 助手对话+引用分栏
- [ ] 图谱 Canvas 充分利用空间
- [ ] 可拖拽调整双栏比例（PaneExpansionState）

**通用**：
- [ ] 横竖屏切换时布局正确自适应
- [ ] `assembleDebug` + `testDebugUnitTest` 全绿

### 9.4 Phase 4 验收
- [ ] 各 Screen 加载态用 LoadingIndicator（非 CircularProgressIndicator）
- [ ] SettingsScreen 主题模式用 SegmentedButton
- [ ] KnowledgePointDetailScreen 关联知识用 HierarchicalListItem
- [ ] 消息气泡用 Surface（有 tonal elevation）
- [ ] 新增组件有 Preview

### 9.5 Phase 5 验收
- [ ] 底部 Sheet 有不对称圆角
- [ ] 列表→详情有共享元素飞行动画（如实施）
- [ ] GraphCanvas 薄弱光晕在 AMOLED 模式下可见
- [ ] 组件 Preview 覆盖浅色/深色/AMOLED 三态
- [ ] WideNavigationRail 有 Preview

---

## 10. 风险与回滚

### 10.1 整体风险
- **低**：Phase 1/2 是改动范围可控的精修，不涉及架构重构
- **中**：Phase 3 大屏适配涉及导航架构调整，需充分测试
- **中**：Phase 4/5 涉及新组件与实验性 API，需充分测试
- **实验性 API**：`WideNavigationRail` / `LoadingIndicator` / `SharedTransition` 仍 `@ExperimentalMaterial3ExpressiveApi`，需 `@OptIn`，未来 alpha 升级可能 break

### 10.2 回滚策略
- 每个 Phase 独立 commit，可单独 revert
- Phase 1 导航重构若出问题，revert 后恢复底部 AI Tab
- Phase 2 动效若掉帧严重，可降级回 tween
- **Phase 3 大屏适配若不稳定，可降级为"全设备用底部 NavigationBar"**（WideNavigationRail 是可选增强）
- Phase 4/5 新组件若不稳定，可保留旧实现

### 10.3 Phase 3 特别风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| material3-adaptive 1.2.0 与 alpha18 不兼容 | 低 | 高 | 先在 spike 分支验证，确认无冲突再合入 |
| NavigableListDetailPaneScaffold 与 NavHost 集成困难 | 中 | 高 | 可退化为手写双栏（Row + AnimatedVisibility），不强制用 adaptive-navigation |
| 双栏状态同步 bug（选中项未更新详情） | 中 | 中 | 用 SharedFlow 或 stateIn 暴露选中项，详情页 collect |
| 平板 emulator 测试覆盖不足 | 中 | 中 | 用 AVD 创建 600dp/840dp/1200dp 三种模拟器测试 |

---

## 11. 已确认决策点

| # | 决策点 | 用户选择 |
|---|--------|----------|
| 1 | 底部第 5 个 Tab | 纯"设置"（✅ 按建议） |
| 2 | AiAssistantScreen 路由类型 | 子路由，Push/Pop slide（✅ 按建议） |
| 3 | 共享元素过渡（Phase 5.2） | 暂缓，先做 Phase 1-4（✅ 按建议） |
| 4 | **WideNavigationRail 大屏适配** | **实施（用户明确要求在平板上用）** |
| 5 | 可变字体 | 暂不引入（✅ 按建议） |
| 6 | Phase 实施顺序 | Phase 1→2→3→4→5 串行（✅ 按建议） |

---

## 附录 A：技术参考

### A.1 Material 3 Adaptive 1.2.0
- **发布**：2025-10 稳定版
- **核心 API**：
  - `currentWindowAdaptiveInfo()` — 获取 WindowSizeClass
  - `ListDetailPaneScaffold` — 列表详情双栏布局
  - `NavigableListDetailPaneScaffold` — 与导航集成的双栏（adaptive-navigation）
  - `rememberPaneExpansionState()` — 可拖拽调整双栏比例
  - `AdaptStrategy.Reflow` / `AdaptStrategy.Levitate` — 1.2.0 新增布局策略
- **断点**：Compact(<600dp) / Medium(600-840dp) / Expanded(≥840dp) / Large(≥1200dp) / ExtraLarge(≥1600dp)
- **依赖**：WindowManager 1.5.0

### A.2 WideNavigationRail（material3 1.5.0-alpha18）
- **来源**：material3 库（非 adaptive），`@ExperimentalMaterial3ExpressiveApi`
- **核心 API**：
  - `WideNavigationRail` — 容器，支持 `header` / `arrangement` / `colors`
  - `WideNavigationRailItem` — 导航项，`selected` / `onClick` / `icon` / `label` / `railExpanded`
  - `rememberWideNavigationRailState()` — 状态，`Expanded` / `Collapsed`
  - `NavigationItemIconPosition.Top`（折叠时）/ `Start`（展开时）
- **特性**：折叠态仅图标，展开态图标+标签，支持动画切换

### A.3 官方文档
- [Material 3 Adaptive 1.2.0 发布博客](https://developer.android.com/blog/posts/material-3-adaptive-1-2-0-is-stable)
- [Compose Material 3 Adaptive 版本说明](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive)
- [WideNavigationRail API 文档](https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-wide-navigation-rail.html)
- [WindowSizeClass 使用指南](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes)

---

## 附录 B：关键文件路径

**导航相关**：
- `app/src/main/java/com/wenyan/app/navigation/TopLevelDestination.kt`
- `app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`
- `app/src/main/java/com/wenyan/app/WenyanApp.kt`

**主题与动效**：
- `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/WenyanTheme.kt`
- `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Type.kt`
- `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Shapes.kt`
- `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/motion/WenyanMotion.kt`

**组件（现有 + 新建）**：
- `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanNavigationBar.kt`
- `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanLargeTopAppBar.kt`
- 新建：`WenyanAdaptiveNavigation.kt` / `WenyanWideNavigationRail.kt` / `LoadingState.kt` / `HierarchicalListItem.kt` / `SectionHeader.kt`

**Screen**：
- `feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`
- `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantScreen.kt`
- `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt`
- `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt`
- `feature/graph/src/main/java/com/wenyan/app/feature/graph/GraphScreen.kt`
- `feature/graph/src/main/java/com/wenyan/app/feature/graph/ui/GraphCanvas.kt`

**依赖配置**：
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `core/designsystem/build.gradle.kts`
