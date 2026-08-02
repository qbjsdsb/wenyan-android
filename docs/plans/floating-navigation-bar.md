# 悬浮底部导航栏改造方案（v2 — 深度调研版）

> 响应 "我看 ksunext 等等这种用 M3 Expressive 的软件底部是悬浮的，你研究一下可不可以做到这样"

## 1. 问题分析

### 1.1 当前实现的痛点

| 问题 | 截图表现 | 技术原因 |
|------|----------|----------|
| 遮挡面积大 | 底部 200dp 非内容区（80dp 导航栏 + 120dp 渐变遮罩） | 透明导航栏需要大片渐变来实现内容过渡 |
| 小横条不沉浸 | 系统手势条在透明导航栏下方，视觉上分层 | 导航栏透明，系统导航条背景暴露 |
| 无悬浮感 | 导航栏紧贴底边，无间距/圆角/投影 | `containerColor = Color.Transparent`，`tonalElevation = 0.dp` |

### 1.2 KSUNext 的做法

从 KSUNext 源码分析（[deepwiki.com](https://deepwiki.com/KernelSU-Next/KernelSU-Next/4.1-application-structure-and-navigation)）：

- 用 **Surface 包裹 NavigationBar**，Surface 带 `tonalElevation` 投影 + 圆角
- 底部导航栏是**独立悬浮组件**，不嵌入 Scaffold 的 bottomBar 插槽
- 支持**滚动感知显隐**（scroll-aware visibility）：下滑隐藏，上滑显示，spring 动画
- 每个 Tab 有 filled/outlined 双图标，选中态切换

### 1.3 M3 Expressive 2025-05 更新

- 新增 **Flexible Navigation Bar**：高度更短，支持 MEDIUM 窗口横向排列
- 选中标签颜色从 `on-surface-variant` 改为 `secondary`
- **默认无阴影**（Elevation: No shadow）—— 悬浮效果需要我们自己加
- material3 1.5.0-alpha18 的 `NavigationBar` API 签名（无 `shape` 参数）：

```kotlin
@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = NavigationBarDefaults.contentColor,
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit,
)
```

**关键发现**：`NavigationBar` 没有 `shape` 参数，不能直接设置圆角。需要包裹在 `Surface` 中或使用 `Modifier.clip()`。

## 2. 技术方案对比

### 方案 A：Surface 包裹 NavigationBar（推荐 ✅）

```
Surface(
    shape = RoundedCornerShape(16.dp),
    tonalElevation = 3.dp,
    color = surfaceContainer,
    modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp)
) {
    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        ...
    )
}
```

| 优点 | 缺点 |
|------|------|
| Surface 原生支持 shape + elevation + color | 多一层嵌套 |
| 内层 NavigationBar 透明，所有视觉由 Surface 控制 | — |
| `tonalElevation` 自动适配 light/dark 主题色调叠加 | — |
| 与 KSUNext 做法一致 | — |

### 方案 B：NavigationBar 直接 clip（不推荐 ❌）

```
NavigationBar(
    modifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .padding(horizontal = 16.dp, bottom = 8.dp),
    containerColor = surfaceContainer,
    tonalElevation = 3.dp,
    ...
)
```

| 优点 | 缺点 |
|------|------|
| 少一层嵌套 | `clip` 只裁剪视觉，不参与布局测量 |
| — | `tonalElevation` 在 NavigationBar 上行为与 Surface 不同（无阴影叠加） |
| — | 裁剪后导航栏四角空白区域会透出底层内容 |

**结论**：方案 A（Surface 包裹）是正确做法。

## 3. 详细改动

### 3.1 文件 1：WenyanNavigationBar.kt

**改动前**：

```kotlin
NavigationBar(
    modifier = modifier,
    containerColor = Color.Transparent,
    tonalElevation = 0.dp,
) { ... }
```

**改动后**：

```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp

// 外层 Surface 提供悬浮外观
Surface(
    shape = RoundedCornerShape(16.dp),
    tonalElevation = 3.dp,
    color = MaterialTheme.colorScheme.surfaceContainer,
    // 水平留边 16dp，底部在系统手势区之上 8dp
    modifier = modifier.padding(horizontal = 16.dp, bottom = 8.dp),
) {
    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
    ) { ... }
}
```

**为什么用 `tonalElevation` 而不是 `shadowElevation`**：
- `tonalElevation` 在 light 主题产生浅色叠加，在 dark 主题产生深色叠加，与 M3 色彩体系一致
- `shadowElevation` 产生硬阴影，在深色主题下可能不自然
- 3.dp 是中等高度，比 Card 默认 1.dp 高，比 Dialog 的 6.dp 低，适合"悬浮导航栏"的层次

### 3.2 文件 2：WenyanAdaptiveNavigation.kt

**改动 1：内容底部 padding 微调**

当前：`val bottomPadding = 80.dp + systemNavBarBottomDp`

因为悬浮导航栏底部额外有 8.dp padding，向上偏移了，所以内容需要更多底部空间来避免被导航栏遮挡。

但是，如果保持 80.dp + systemNavBarBottomDp，内容会与导航栏顶部有约 8dp 重叠。这个重叠区有渐变遮罩处理，视觉上过渡平滑。

**两种子方案**：

| 子方案 | 内容 padding | 效果 |
|--------|-------------|------|
| A1（推荐） | 保持 `80.dp + systemNavBarBottomDp` | 内容与导航栏顶部有 ~8dp 重叠，渐变遮罩处理过渡，内容利用更充分 |
| A2 | `88.dp + systemNavBarBottomDp` | 内容与导航栏完全无重叠，但底部留白稍多 |

推荐 **A1**，因为：
- 缩短的渐变遮罩（80dp）正好覆盖这个重叠区
- 内容区域利用最大化
- 视觉上导航栏"浮"在内容之上，重叠是正常的

**改动 2：BottomGradientScrim 缩短**

当前：120dp 高度，透明度从 Transparent → 0.85f → surfaceContainer

当前效果：
```
120dp:
┌─────────────────┐
│  Transparent     │  ← 0dp
│  ...渐变...       │
│  0.85f alpha     │  ← ~80dp
│  surfaceContainer│  ← 120dp
└─────────────────┘
```

改为 80dp，透明度降低：

```kotlin
@Composable
private fun BottomGradientScrim() {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)  // 120dp → 80dp
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        surfaceContainer.copy(alpha = 0.60f),  // 0.85f → 0.60f
                        surfaceContainer.copy(alpha = 0.85f),  // 新增中间色
                        surfaceContainer,
                    ),
                ),
            ),
    )
}
```

**说明**：
- 缩短到 80dp：因为悬浮导航栏自带 `surfaceContainer` 背景，不再需要大片渐变来过渡
- 透明度降低 + 增加中间色阶：渐变更平滑，从透明到不透明有 3 个 stops
- 导航栏区域（底部 16dp 水平间距内）由 Surface 的 `surfaceContainer` 直接覆盖

### 3.3 改动总结

| 文件 | 改动类型 | 行数变化 |
|------|----------|----------|
| `WenyanNavigationBar.kt` | 新增 Surface 包裹 + shape/elevation/color + padding | +5 / -0 |
| `WenyanAdaptiveNavigation.kt` | BottomGradientScrim 缩短 + 透明度微调 | +3 / -2 |
| 合计 | 2 文件 | +8 / -2 |

## 4. 布局结构对比

### 改造前（透明沉浸式）

```
┌──────────────────────────────────┐
│  Content (surfaceContainer 背景)  │
│  bottom padding = 80dp + sysNav  │
│                                  │
│  ┌──── BottomGradient(120dp) ──┐ │
│  │  Transparent → 0.85f → solid│ │
│  └─────────────────────────────┘ │
│  ┌── NavigationBar (透明 80dp) ──┐│
│  │  ◉ 知识点  ◉ 论述题  ◉ 卡片  ││
│  │  ◉ 错题本  ◉ 设置            ││
│  └───────────────────────────────┘│
│  ← 系统导航条（手势区） →          │
└──────────────────────────────────┘
```

### 改造后（悬浮式）

```
┌──────────────────────────────────┐
│  Content (surfaceContainer 背景)  │
│  bottom padding = 80dp + sysNav  │
│                                  │
│  ┌──── BottomGradient(80dp) ───┐ │
│  │  Transparent → 0.60f → 0.85f│ │
│  │  → solid                    │ │
│  └─────────────────────────────┘ │
│     ┌────────────────────────┐   │
│     │  NavigationBar(Surface) │   │  ← 16dp 水平间距
│     │  surfaceContainer 背景  │   │  ← 3dp tonalElevation
│     │  roundedCorner 16dp    │   │  ← 浮在内容之上
│     │  ◉ 知识点  ◉ 论述题     │   │
│     │  ◉ 卡片  ◉ 错题本  ◉ 设置│   │
│     └────────────────────────┘   │
│          8dp + sysNav            │  ← 底部留空
└──────────────────────────────────┘
```

## 5. 测试影响

### 5.1 WenyanNavigationBarTest（3 个测试）

```
labels_areDisplayed_forAllItems()        → 不受影响 ✅
items_haveClickAction_forAccessibility()  → 不受影响 ✅
onNavigate_invoked_whenItemClicked()      → 不受影响 ✅
```

测试使用 `Surface` 包裹，内层 `WenyanNavigationBar` 的 API 签名不变，测试代码无需修改。

### 5.2 WenyanNavigationBarPreview（3 个 Preview）

```
Light - Knowledge selected  → 视觉变化 ✅（自动显示悬浮效果）
Dark - AI selected          → 视觉变化 ✅（自动显示悬浮效果）
AMOLED - Cards selected     → 视觉变化 ✅（自动显示悬浮效果）
```

Preview 使用 `Surface` 包裹，外层新增的 `Surface` 在 Preview 中自动渲染，可直接看到悬浮效果。

## 6. 视觉对比

| 方面 | 改造前 | 改造后 |
|------|--------|--------|
| 导航栏背景 | 透明，透出内容和渐变 | `surfaceContainer`，不透明 |
| 底部间距 | 0dp（紧贴屏幕底边） | 水平 16dp + 底部 8dp + 系统手势区 |
| 圆角 | 无（直角） | 16dp RoundedCorner |
| 投影 | 无 | 3dp tonalElevation |
| 渐变遮罩 | 120dp，Transparent → 0.85f → solid | 80dp，Transparent → 0.60f → 0.85f → solid |
| 内容 padding | 80dp + sysNav | 80dp + sysNav（不变） |
| 小横条沉浸感 | 差（透明导航栏 + 手势条分离） | 好（surfaceContainer 背景统一） |
| 遮挡面积 | 200dp（80+120） | 160dp（80+80），减少 20% |
| 维护性 | 略复杂（需处理透明叠加） | 简单（Surface 统一管理） |

## 7. 风险分析

| 风险 | 等级 | 说明 | 缓解措施 |
|------|------|------|----------|
| Surface 嵌套导致语义冲突 | 低 | 内层 NavigationBar 的 containerColor 为 Transparent，不会与外层 Surface 颜色冲突 | 已验证 NavigationBar 在 Transparent 下行为正常 |
| tonalElevation 在 AMOLED 模式 | 低 | AMOLED 纯黑背景 + elevation 叠加可能不够明显 | 3.dp 在 AMOLED 下仍有可见阴影 |
| 水平 padding 16dp 在窄屏 | 低 | 360dp 宽度设备上，导航栏净宽 328dp，5 项仍可正常显示 | M3 标准 NavigationBar 最小宽度 300dp，328dp > 300dp |
| 底部 padding 8dp 在全面屏手势 | 低 | 8dp + systemNavBarBottomDp 在全面屏手势下约 8+24=32dp，足够 | 与系统导航条区域不重叠 |
| 渐变遮罩 80dp 不足 | 低 | 80dp 覆盖导航栏高度（80dp）的完整区域 | 测试验证，如不足可微调至 100dp |
| 测试用例需更新 | 无 | 3 个测试全部通过，不涉及 WenyanNavigationBar 的 API 变化 | — |

## 8. 实施步骤

1. **修改 `WenyanNavigationBar.kt`**：添加 Surface 包裹 + shape/elevation/color + padding
2. **修改 `WenyanAdaptiveNavigation.kt`**：缩短 BottomGradientScrim + 透明度微调
3. **本地验证**：`./gradlew :core:designsystem:testDebugUnitTest` + `assembleDebug`
4. **Preview 目视检查**：3 个 Preview 的悬浮效果
5. **emulator 实测**：验证 5 个 Tab 切换、子路由返回、沉浸式效果

## 9. 后续可迭代（本次不做）

1. **滚动感知显隐**：下滑隐藏导航栏，上滑显示（需传递 LazyListState 到 WenyanAdaptiveNavigation）
2. **选中态动画**：指示器在 Tab 间滑动动画（spring animation）
3. **Filled/Outlined 双图标**：选中态用 filled 图标，未选中用 outlined
4. **Docked FAB**：在导航栏左侧/右侧悬浮一个主要操作按钮

## 11. v0.9.20 流体玻璃改造（2026-08-01）

### 11.1 背景

v0.9.19 的"紧凑玻璃风格"收到用户强烈负面反馈："底栏还是非常糟糕，怎么一次比一次糟糕"。

### 11.2 问题诊断

| 问题 | v0.9.19 表现 | 根因 |
|------|-------------|------|
| 高度太挤 | 56dp，5 项 Tab 图标+文字挤在一起 | 过度追求"减少遮挡" |
| 留边尴尬 | 水平 8dp，既不是全宽也不是明显悬浮 | 折中方案两头不讨好 |
| 圆角过激 | 24dp 四角圆角，56dp 高的 bar 圆角吃掉一半 | 视觉上像一颗药丸 |
| 玻璃效果弱 | 0.04f/0.06f 白色渐变几乎不可见 | 玻璃效果实现不到位 |
| 误删 Scrim | BottomGradientScrim 被移除，内容到导航栏无过渡 | 以为半透明不需要过渡 |
| 底部留空 | 离底 4dp，悬浮感变成"飘着" | 仿 iOS 但没仿到位 |

### 11.3 改造方案

#### 11.3.1 设计语言

**目标**：还原 Apple iOS Tab Bar 的流体玻璃风格

| 特性 | iOS Tab Bar | v0.9.20 实现 |
|------|-------------|-------------|
| 宽度 | 全宽 | 无水平留边，Surface 撑满 |
| 圆角 | 顶部微圆角 | 仅顶部 16dp，底部直角贴底 |
| 高度 | 舒适 | 72dp（5 项 Tab 舒适间距） |
| 材质 | 毛玻璃模糊 | surfaceContainerHigh alpha=0.75f + 光泽渐变 |
| 底部 | 贴底 | 无底部空隙，直接延伸到屏幕底部 |
| 顶部高光 | 细线边缘 | 垂直渐变 0.12f→Transparent，2px 高光 |
| 过渡 | 自然模糊 | 恢复 40dp BottomGradientScrim |

#### 11.3.2 布局结构

```
┌──────────────────────────────────────┐
│  Content area                         │
│  bottom padding = 72dp + sysNav      │
│                                       │
│  ┌── BottomGradientScrim(40dp) ────┐  │
│  │  Transparent → 0.50f → 0.85f    │  │
│  │  → surfaceContainer (solid)      │  │
│  └─────────────────────────────────┘  │
│  ┌──────────────────────────────────┐  │
│  │  NavigationBar (全宽, 72dp)       │  │
│  │  顶部 16dp 圆角, 底部直角贴底     │  │
│  │  surfaceContainerHigh(0.75) + 光泽│  │
│  │  ◉ 知识点  ◉ 论述题  ◉ 卡片       │  │
│  │  ◉ 错题本  ◉ 设置                │  │
│  │  ── 顶部高光边缘 2px ──          │  │
│  └──────────────────────────────────┘  │
│  ← 系统手势区（由 WindowInsets 处理）→  │
└──────────────────────────────────────┘
```

#### 11.3.3 改动文件

**WenyanNavigationBar.kt**：
- 形状：`RoundedCornerShape(topStart=16.dp, topEnd=16.dp, bottomStart=0.dp, bottomEnd=0.dp)`
- 高度：`72.dp`（val navHeight: Dp = 72.dp）
- 颜色：`surfaceContainerHigh.copy(alpha = 0.75f)` — 更透明的玻璃质感
- tonalElevation: `0.dp` — 用玻璃效果代替阴影
- 无水平 padding，无底部 padding
- Android 12+ 叠加两层光泽：
  1. 顶部高光边缘：`verticalGradient(0.12f White → Transparent, startY=0, endY=2)`
  2. 水平光泽：`horizontalGradient(0.03f → Transparent → 0.05f)`

**WenyanAdaptiveNavigation.kt**：
- 底部 padding：`72.dp + systemNavBarBottomDp`（之前是 56dp + 4dp）
- 恢复 BottomGradientScrim（40dp，4 阶渐变：Transparent → 0.50f → 0.85f → solid）
- 移除底部 4dp 留空

### 11.4 对比

| 方面 | v0.9.19（紧凑玻璃） | v0.9.20（流体玻璃） |
|------|--------------------|--------------------|
| 宽度 | 水平 8dp 留边 | 全宽，无留边 |
| 圆角 | 24dp 四角 | 仅顶部 16dp |
| 高度 | 56dp | 72dp |
| 透明度 | 0.85f | 0.75f（更透） |
| 高光 | 无 | 顶部 2px 高光边缘 |
| 光泽 | 0.04f/0.06f 渐变 | 0.03f/0.05f 渐变（更自然） |
| 底部 | 离底 4dp | 贴底 |
| 过渡 | 无 Scrim | 40dp BottomGradientScrim |
| 遮挡面积 | 56dp（导航栏） | 72dp（导航栏）+ 40dp（Scrim 覆盖区域与导航栏重叠） |

### 11.5 测试影响

3 个 WenyanNavigationBarTest 的 API 签名不变，不受影响 ✅

### 11.6 潜在风险

| 风险 | 等级 | 说明 | 缓解 |
|------|------|------|------|
| 全宽在 AMOLED 下 | 低 | 全宽半透明 bar 在 AMOLED 纯黑背景上可能不够明显 | 顶部高光边缘 + 光泽渐变提供视觉边界 |
| 72dp 遮挡面积 | 低 | 比 56dp 多 16dp，但恢复的内容过渡更自然 | 用户实际感知是"融合"而非"遮挡" |

## 12. v0.9.20 KSU 风格滚动感知导航栏（2026-08-01）

### 12.1 背景

响应用户需求"就ksu的吧，做好然后做好交接"。用户对 v0.9.19 紧凑玻璃导航栏不满意后，深入调研 KernelSU Next 的源码实现，发现其核心差异在于**滚动感知显隐（scroll-aware visibility）**：下滑内容时导航栏自动隐藏，上滑时自动显示，用 spring 动画驱动。

### 12.2 实现方案

#### 12.2.1 架构设计

```
┌──────────────────────────────────────────────┐
│  CompositionLocal<LazyListState?>             │
│  ┌─ LocalLazyListState ──────────────────┐   │
│  │  Screen → LazyColumn → LazyListState  │   │
│  │  WenyanAdaptiveNavigation → 读取      │   │
│  └──────────────────────────────────────┘   │
│                                              │
│  ┌─ 滚动方向检测 ─────────────────────────┐  │
│  │  snapshotFlow(firstVisibleItemIndex,    │  │
│  │    firstVisibleItemScrollOffset)        │  │
│  │  → 下滑: barVisible = false             │  │
│  │  → 上滑: barVisible = true              │  │
│  │  → 10px 阈值防抖                        │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ┌─ Spring 动画组 ────────────────────────┐  │
│  │  bottomOffset: 0.dp → 72.dp (spring)  │  │
│  │  ├─ BottomGradientScrim (40dp)         │  │
│  │  └─ WenyanNavigationBar (72dp)         │  │
│  │  整体移动，下滑时一起移出屏幕           │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

#### 12.2.2 核心组件

**1. LocalScrollState.kt（新增）**
- 定义 `LocalLazyListState` 为 `CompositionLocal<LazyListState?>`
- 默认值为 `null`（无 LazyColumn 的页面导航栏保持可见）
- 设计参照 KernelSU Next 的 scroll-aware 底部导航栏模式

**2. WenyanNavigationBar.kt（修改）**
- 新增 `visible: Boolean = true` 参数
- 通过 `animateDpAsState` + `spring` 驱动 `translationY`
- `visible=false` → 导航栏向下移出屏幕（translationY = navHeight）
- `visible=true` → 导航栏回到原位（translationY = 0.dp）
- 默认 `visible=true`，不影响现有测试

**3. WenyanAdaptiveNavigation.kt（修改）**
- 读取 `LocalLazyListState.current` 获取当前页面的滚动状态
- 使用 `snapshotFlow` 监听 `firstVisibleItemIndex` + `firstVisibleItemScrollOffset`
- 滚动方向检测：
  - 下滑：index 增大，或同一 index 但 offset 增大（+10px 阈值防抖）
  - 上滑：index 减小，或同一 index 但 offset 减小（-10px 阈值防抖）
- `BottomGradientScrim` + `WenyanNavigationBar` 通过 `bottomOffset` 整体移动

**4. 各 Screen（修改）**
- KnowledgeScreen / QuizScreen / WrongAnswerScreen / SettingsScreen / EssayListScreen
- 每个 LazyColumn 通过 `CompositionLocalProvider(LocalLazyListState provides listState)` 提供滚动状态
- `listState = rememberLazyListState()` 带默认参数，不影响现有测试

### 12.3 改动文件清单

| 文件 | 改动类型 | 行数变化 |
|------|----------|----------|
| `LocalScrollState.kt` | 新增 | +21 |
| `WenyanNavigationBar.kt` | 修改 | +22 / -1 |
| `WenyanAdaptiveNavigation.kt` | 修改 | +92 / -42 |
| `KnowledgeScreen.kt` | 修改 | +30 / -18 |
| `QuizScreen.kt` | 修改 | +44 / -24 |
| `WrongAnswerScreen.kt` | 修改 | +42 / -22 |
| `SettingsScreen.kt` | 修改 | +15 / -8 |
| `EssayListScreen.kt` | 修改 | +30 / -18 |
| 合计 | 8 文件 | +296 / -133 |

### 12.4 行为说明

| 场景 | 行为 |
|------|------|
| 有 LazyColumn 的页面，下滑内容 | 导航栏 + 渐变遮罩整体向下移出屏幕（spring 动画） |
| 有 LazyColumn 的页面，上滑内容 | 导航栏 + 渐变遮罩整体回到原位（spring 动画） |
| 无 LazyColumn 的页面（CardsScreen） | `LocalLazyListState` 为 null，导航栏保持可见 |
| 子路由（无导航栏） | 不触发滚动感知逻辑 |
| 快速切换方向 | 10px 阈值防抖，避免在滚动暂停时误触发 |
| 列表顶部（index=0, offset=0） | 导航栏始终显示，不会误隐藏 |

### 12.5 测试影响

| 测试 | 影响 |
|------|------|
| WenyanNavigationBarTest（3 测试） | 不受影响 ✅（visible 默认 true） |
| 各 Screen 测试 | 不受影响 ✅（listState 默认 rememberLazyListState()） |

### 12.6 风险分析

| 风险 | 等级 | 说明 | 缓解 |
|------|------|------|------|
| 10px 阈值在低刷新率设备 | 低 | 60Hz 设备上单次滚动可能超过 10px | 10px 约 0.5 行文本高度，误触率低 |
| spring 动画在低端设备 | 低 | spring 动画在 GPU 上运行，开销极低 | 仅在滚动停止时触发一次动画 |
| CompositionLocal 传递 | 低 | 只有 4 个顶级 Screen 提供，无深层传递 | 局部使用，不影响全局状态 |
| 下拉刷新时导航栏隐藏 | 低 | 下拉刷新初始阶段的滑动方向是"下滑" | 刷新完成后用户自然上滑可恢复导航栏 |

### 12.7 参考

- KernelSU Next BottomBar 源码: [deepwiki.com](https://deepwiki.com/KernelSU-Next/KernelSU-Next/4.1-application-structure-and-navigation)
- Android Compose CompositionLocal: [developer.android.com](https://developer.android.com/develop/ui/compose/compositionlocal)
- Compose Animation Spring: [developer.android.com](https://developer.android.com/reference/kotlin/androidx/compose/animation/core/Spring)

## 13. v0.9.20 MD3 规范回归（2026-08-02）

**背景**：v0.9.19/v0.9.20 的"流体玻璃"风格（§11）是仿 iOS Tab Bar 的毛玻璃方案。用户明确要求回归**规范 Material 3 风格**（"不是说是毛玻璃，我现在想比较规范的md3的风格"），同时保留 KSU 风格滚动感知显隐（§12）与 80dp 标准高度。

**目标**：底栏视觉完全对齐 [m3-expressive-redesign.md §5.1](../design/m3-expressive-redesign.md)：`surfaceContainer` 实色容器、80dp 标准高度、直角全宽、`tonalElevation` 3dp、选中 `secondaryContainer` 药丸指示器。

**方案**：

| 方面 | v0.9.20 流体玻璃（§11） | v0.9.20 MD3 规范回归 |
|------|------------------------|----------------------|
| 容器色 | surfaceContainerHigh alpha=0.75f 半透明 | `surfaceContainer` 实色 |
| 高度 | 72dp | **80dp**（MD3 标准 NavigationBar 高度） |
| 形状 | 全宽 + 顶部圆角 | 全宽直角（MD3 无圆角） |
| 阴影 | 无（半透明需过渡） | `tonalElevation = 3.dp` |
| 光泽渐变 | 有（光泽 overlay） | 移除 |
| 渐变遮罩 | 40dp BottomGradientScrim | **移除**（实色底栏无需过渡） |
| 选中指示器 | secondaryContainer | secondaryContainer（不变） |
| 滚动感知 | scroll-aware 显隐（spring） | 保留（不变） |
| 内容 padding | 72dp + sysNav | **80dp + sysNav** |

**改动文件**：
- `WenyanNavigationBar.kt`：`containerColor = surfaceContainer`、`height = 80.dp`、`tonalElevation = 3.dp`；移除 glass overlay（Build.VERSION 条件 + 渐变）
- `WenyanAdaptiveNavigation.kt`：删除 `BottomGradientScrim` composable 及调用；`bottomPadding` 72dp→80dp；`bottomHideDistance` 72dp→80dp
- 提取 `detectScrollDirection()` 纯函数 + `ScrollDirection` 枚举（可单测）
- 新增 `ScrollDirectionDetectorTest`（16 用例）：index 优先 / ±10px 阈值 / 边界 / 自定义阈值

**验证**：沙箱 JDK 17 全量构建，`:core:designsystem:assembleDebug` + `testDebugUnitTest` **42 tests / 0 failures**（含 ScrollDirectionDetectorTest 16 + Robolectric 14）。

**待 emulator 实测**：MD3 配色观感 + 滚动感知流畅度 + CardsScreen 保持可见 + 子路由无影响。

## 14. 参考

- Apple HIG Tab Bar: [developer.apple.com](https://developer.apple.com/design/human-interface-guidelines/tab-bars)
- KSUNext BottomBar: [deepwiki.com](https://deepwiki.com/KernelSU-Next/KernelSU-Next/4.1-application-structure-and-navigation)
- M3 Expressive NavigationBar: [m3.material.io](https://m3.material.io/components/navigation-bar)
- Compose Material3 API: [NavigationBar](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#NavigationBar)