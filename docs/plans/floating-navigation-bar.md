# 悬浮底部导航栏改造方案

> 响应 "我看 ksunext 等等这种用 M3 Expressive 的软件底部是悬浮的，你研究一下可不可以做到这样"

## 1. 调研结论

### 1.1 KSUNext 的做法

从 KSUNext 源码分析（[deepwiki.com](https://deepwiki.com/KernelSU-Next/KernelSU-Next/4.1-application-structure-and-navigation)）：

- 底部导航栏是**悬浮的动画组件**，包裹在 `Surface` 中，带有 `tonalElevation` 投影
- 支持**滚动感知显隐**（scroll-aware visibility）：下滑时隐藏，上滑时显示，spring 动画过渡
- 每个 Tab 有 filled/outlined 双图标，选中态切换

### 1.2 M3 Expressive 官方柔性导航栏（2025-05）

- 新增 **Flexible Navigation Bar**：高度更短，支持 MEDIUM 窗口水平排列
- 官方推荐替代 Baseline Navigation Bar
- 颜色：选中标签从 `on-surface-variant` 改为 `secondary`
- **默认无阴影**（Elevation: No shadow）—— 悬浮效果需要我们自己加

### 1.3 当前文研导航栏的问题

| 问题 | 说明 |
|------|------|
| 遮挡面积大 | 80dp 透明导航栏 + 120dp 渐变遮罩，底部共 200dp 非内容区 |
| 小横条不沉浸 | 系统导航条（gesture bar）在透明导航栏下方，视觉上有割裂感 |
| 非悬浮 | 导航栏紧贴底部，无间距/圆角/投影，缺少 M3E 的"漂浮"质感 |

## 2. 改造方案

### 2.1 核心思路

用一个**带圆角 + 高度 + 投影的 Surface 包裹 NavigationBar**，水平两侧留间距，底部留出系统手势区，视觉上形成"悬浮"效果。

```
┌──────────────────────────────────┐
│                                  │
│            Content               │
│                                  │
│    ┌──────────────────────┐      │
│    │  Gradient Scrim (80dp)│      │  ← 缩短的渐变遮罩
│    └──────────────────────┘      │
│  ┌────────────────────────────────┐│
│  │  padding 16.dp ←→ 16.dp       ││
│  │ ┌──────────────────────────┐   ││
│  │ │◉ 知识点  ◉ 论述题  ◉ 卡片│   ││  ← Surface(表面容器色)
│  │ │◉ 错题本  ◉ 设置          │   ││    tonalElevation=3.dp
│  │ └──────────────────────────┘   ││    shape=RoundedCorner(16.dp)
│  │        padding 8.dp + sysNav   ││
│  └────────────────────────────────┘│
└──────────────────────────────────┘
```

### 2.2 改动文件

| 文件 | 改动内容 |
|------|----------|
| `WenyanNavigationBar.kt` | 添加 `shape`、`tonalElevation`、`containerColor` 改为 `surfaceContainer`；modifier 添加水平/底部 padding |
| `WenyanAdaptiveNavigation.kt` | 调整 `BottomGradientScrim` 高度（120dp → 80dp）+ 透明度微调；内容底部 padding 保持不变 |

### 2.3 详细参数

#### WenyanNavigationBar.kt 改动

```kotlin
// 新增参数
shape = RoundedCornerShape(16.dp),
tonalElevation = 3.dp,
containerColor = MaterialTheme.colorScheme.surfaceContainer,

// modifier 新增 padding
modifier = modifier
    .padding(horizontal = 16.dp)
    .padding(bottom = 8.dp),
```

**参数选择理由**：
- `16.dp` 圆角 — 对齐 M3E 按钮/卡片圆角规范（buttons 12→16dp, cards 4→20dp）
- `3.dp` tonalElevation — 产生阴影悬浮感但不夸张（KSUNext 使用 3.dp）
- `surfaceContainer` — 与内容区背景色一致，视觉上统一
- 水平 `16.dp` padding — 与屏幕边缘留出呼吸感
- 底部 `8.dp` padding — 在系统手势区之上留出间距，导航栏不贴底

#### WenyanAdaptiveNavigation.kt 改动

```kotlin
// BottomGradientScrim: 120dp → 80dp
// 透明度微调：0.85f → 0.70f
// 因为悬浮导航栏自带 surfaceContainer 背景，不再需要大片渐变过渡
```

### 2.4 不实施的特性

**滚动感知显隐（scroll-aware visibility）** — 本次不实施，原因：
1. 文研 App 不是 Feed 流应用，大部分页面是列表/详情页，滚动场景不多
2. 加入显隐动画会增加复杂度（需传递 LazyListState 到 WenyanAdaptiveNavigation）
3. 用户未要求此功能，可后续迭代

## 3. 视觉效果对比

| 方面 | 当前（透明沉浸式） | 改造后（悬浮式） |
|------|-------------------|-----------------|
| 导航栏背景 | 透明 | `surfaceContainer` |
| 底部间距 | 无（紧贴屏幕底边） | 水平 16dp + 底部 8dp |
| 圆角 | 无 | 16dp |
| 投影 | 无 | 3dp tonalElevation |
| 渐变遮罩 | 120dp | 80dp（缩短） |
| 视觉感受 | 内容"延伸到"导航栏区域 | 导航栏"漂浮"在内容之上 |

## 4. 实施步骤

1. **修改 `WenyanNavigationBar.kt`** — 添加 shape、tonalElevation、containerColor、padding
2. **修改 `WenyanAdaptiveNavigation.kt`** — 缩短 BottomGradientScrim 到 80dp，降低透明度到 0.70f
3. **本地验证** — `assembleDebug` + `testDebugUnitTest` 全绿
4. **emulator 实测** — 验证 5 个 Tab 切换、子路由返回、沉浸式效果

## 5. 风险与注意事项

- **Lint 警告**：RoundedCornerShape 需要 import `androidx.compose.foundation.shape.RoundedCornerShape`
- **MEDIUM/EXPANDED 布局**：不受影响，NavigationRail 不变
- **子路由（showNavigation=false）**：不受影响，导航栏完全隐藏时不参与布局
- **无障碍**：surfaceContainer 背景不影响 TalkBack 对 NavigationBarItem 的朗读