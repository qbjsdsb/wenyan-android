# 文研 Android 应用 Material 3 Expressive 改造设计

**日期**：2026-07-12
**状态**：已批准（待写实现计划）
**作者**：assistant + user

## 1. 背景与目标

### 1.1 问题陈述

当前文研 Android 应用 UI 存在以下问题：

1. **主题系统单薄**：仅 18 个颜色角色（M3 完整需 40+），缺失 `tertiaryContainer`、`errorContainer`、`surfaceContainer*` 等关键角色，导致 `CardRenderer`、`QuizScreen`、`AiAssistantScreen` 等多处回退 M3 默认值，与品牌色不一致
2. **Typography 不完整**：仅 8 个样式（M3 需 15 个），`headlineSmall`/`bodySmall`/`labelSmall` 等被广泛使用却未定义，回退默认值与自定义字号体系不一致
3. **designsystem 模块极薄**：仅 1 个共享组件（`ContentSourceBadge`），`InfoChip`/`EmptyState`/`TopAppBar` 等 4+ 处重复实现且样式不一致（`KnowledgePointDetail` 的 `InfoChip` 用 `surfaceVariant`，`Quiz` 的用 `secondaryContainer`）
4. **Shapes 未定义**：使用 M3 默认，但多处硬编码圆角（`RoundedCornerShape(4.dp)`/`RoundedCornerShape(16.dp)` 混用）
5. **大量硬编码颜色**：`GraphCanvas` 的 `COLOR_GREEN/YELLOW/RED/GRAY`、`ContentSourceBadge` 的 `Color.White`、`GraphScreen` 的 `LEGEND_*` 常量等，未走主题
6. **深色主题质量差**：`primary`/`onPrimary` 简单互换可能导致对比度不达标，`background`/`surface` 硬编码无命名常量
7. **无 `@Preview`**：所有 Composable 均未提供预览，不利于设计与开发协作

### 1.2 目标

将文研 Android 应用 UI 全面升级为 **Material 3 Expressive（2025 版）**，达到 KernelSU（KSU）级别的"谷歌味道"：

- 采用 `MaterialExpressiveTheme` + `MotionScheme.expressive()`
- 引入 `com.materialkolor:library` 实现 Android 5+ 动态色彩
- 使用 `surfaceContainer` 色调分层代替阴影
- 构建 Expressive 组件族（`ExpressiveScaffold`、`TonalCard`、`WenyanTopAppBar` 等 9 个共享组件）
- 首页使用 `LargeFlexibleTopAppBar`（M3 Expressive 大标题顶栏）
- 移除"墨色+宣纸"硬编码色，改用纯动态色彩
- 支持跟随系统深色模式 + AMOLED 纯黑选项
- 新建 `feature/settings` 模块，提供主题模式/AMOLED/调色板风格设置
- 全量改造 7 个 feature Screen

### 1.3 非目标

- 不涉及业务逻辑变更（FSRS 调度、AI 服务、RAG 等保持不变）
- 不涉及数据库 schema 变更
- 不涉及网络层变更
- 不做国际化（strings.xml 提取不在本次范围）
- 不引入 Navigation 3.0（保持现有 Navigation Compose）

### 1.4 成功标准

1. CI 编译通过（`assembleDebug` + `assembleRelease`）
2. 全部 61+ 现有单元测试通过
3. 新增主题相关单元测试通过
4. 各 Screen 在浅色/深色/AMOLED 三种模式下视觉正确
5. 动态色彩在 Android 12+ 正确跟随壁纸
6. Android 11 及以下设备用预设种子色生成色板
7. 签名 APK 构建成功（Release workflow）
8. 视觉效果接近 KSU 水平——"非常谷歌味道"

## 2. 架构总览

### 2.1 模块结构

```
wenyan-android/
├── core/designsystem/          # 设计系统（核心改造）
│   ├── theme/
│   │   ├── Color.kt            # → 移除硬编码色，改用 materialkolor 生成
│   │   ├── Type.kt             # → 补全 15 个 M3 Typography 样式
│   │   ├── Shapes.kt           # → 新建：M3 Shapes（extraSmall=4, small=8, medium=12, large=16, extraLarge=28）
│   │   ├── WenyanTheme.kt      # → 重写：MaterialExpressiveTheme + MotionScheme.expressive()
│   │   └── ThemeConfig.kt      # → 新建：主题配置状态管理（ColorMode/PaletteStyle/Amoled）
│   └── component/              # Expressive 组件族
│       ├── ContentSourceBadge.kt  # → 重构：走主题色
│       ├── ExpressiveScaffold.kt  # → 新建：containerColor=surfaceContainer
│       ├── TonalCard.kt           # → 新建：containerColor=surfaceBright, shape=large
│       ├── WenyanTopAppBar.kt     # → 新建：统一 TopAppBar + LargeFlexibleTopAppBar 封装
│       ├── WenyanInfoChip.kt      # → 新建：统一 InfoChip（消除 KnowledgePointDetail/Quiz 不一致）
│       ├── EmptyState.kt          # → 新建：统一空状态
│       ├── LoadingState.kt        # → 新建：统一加载态
│       ├── SectionHeader.kt       # → 新建：区块标题
│       └── Spacing.kt             # → 新建：间距 tokens（4/8/12/16/24/32dp）
├── core/data/                  # 新增 ThemeRepository
│   └── di/ThemeModule.kt       # → Hilt DI
├── feature/settings/           # 新建模块：设置页
│   └── SettingsScreen.kt       # 主题模式/AMOLED/调色板风格/关于
├── feature/knowledge/          # 改造：使用新组件
├── feature/quiz/               # 改造：使用新组件
├── feature/cards/              # 改造：使用新组件
├── feature/graph/              # 改造：GraphCanvas 走主题色
├── feature/aiassistant/        # 改造：AiAssistant/ApiConfig/MentorInfo
└── app/                        # 改造：WenyanApp/WenyanNavHost/MainActivity
```

### 2.2 新增依赖

```kotlin
// core/designsystem/build.gradle.kts
api("com.materialkolor:library:1.7.0")  // 动态色彩生成（KSU 同款）
api("androidx.compose.material3:material3:1.4.0-alpha15")  // MaterialExpressiveTheme
// MotionScheme.expressive() 在 material3 1.4.0+ 提供
```

### 2.3 导航结构变更

```
顶级路由（5个，不变）：
  knowledge → quiz → cards → graph → aiassistant

子路由（4个）：
  knowledge_detail/{pointId}
  mentor
  api_config
  settings  ← 新增
```

`settings` 路由入口固定在 `AiAssistantScreen` 的 TopAppBar action（设置图标），与现有 API 配置入口并列。`MentorInfoScreen` 不添加设置入口（保持单一入口避免混淆）。

## 3. 主题系统设计

### 3.1 颜色系统（materialkolor 动态生成）

**移除**：`Color.kt` 中所有硬编码颜色（`WenyanPrimary`、`WenyanSecondary` 等"墨色+宣纸"色板）。

**新方案**：颜色完全由 `materialkolor` 的 `createColorScheme()` 生成，不手动定义任何颜色常量。

```kotlin
// WenyanTheme.kt
@Composable
fun WenyanTheme(
    config: ThemeConfig,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (config.colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }

    val baseScheme = remember(config) {
        if (config.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+：使用系统壁纸提取的动态色彩
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            // Android 11- 或手动种子色：用 materialkolor 生成
            createColorScheme(
                seed = config.seedColor,
                style = config.paletteStyle.toMaterialKolorStyle(),
                isDark = isDark,
                specVersion = ColorSpec.SpecVersion.SPEC_2025  // M3 Expressive 2025 规范
            )
        }
    }

    // AMOLED 模式：将底层表面替换为纯黑
    val finalScheme = if (isDark && config.amoledMode) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color.Black,
        )
    } else baseScheme

    // 颜色切换动画
    val animatedScheme by animateColorAsState(
        targetValue = finalScheme,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "colorScheme"
    )

    MaterialExpressiveTheme(
        colorScheme = animatedScheme,
        motionScheme = MotionScheme.expressive(),
        typography = WenyanTypography,
        shapes = WenyanShapes,
        content = content
    )
}
```

### 3.2 完整 ColorScheme 角色

`materialkolor` 的 `createColorScheme(specVersion = SPEC_2025)` 自动生成全部 M3 Expressive 角色：

- **主色系**：`primary`/`onPrimary`/`primaryContainer`/`onPrimaryContainer`/`inversePrimary`
- **次色系**：`secondary`/`onSecondary`/`secondaryContainer`/`onSecondaryContainer`
- **三色系**：`tertiary`/`onTertiary`/`tertiaryContainer`/`onTertiaryContainer`
- **错误系**：`error`/`onError`/`errorContainer`/`onErrorContainer`
- **表面系**（M3 Expressive 核心）：`surface`/`onSurface`/`surfaceVariant`/`onSurfaceVariant`/`surfaceTint`/`surfaceBright`/`surfaceDim`/`surfaceContainer`/`surfaceContainerLow`/`surfaceContainerLowest`/`surfaceContainerHigh`/`surfaceContainerHighest`
- **固定色系**：`primaryFixed`/`primaryFixedDim`/`onPrimaryFixed`/`onPrimaryFixedVariant` + secondary/tertiary 对应
- **其他**：`outline`/`outlineVariant`/`scrim`/`inverseSurface`/`inverseOnSurface`

### 3.3 Typography（15 样式）

```kotlin
val WenyanTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Normal),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Normal),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)
```

### 3.4 Shapes（M3 Expressive 圆角规范）

```kotlin
val WenyanShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // InfoChip、小标签
    small = RoundedCornerShape(8.dp),          // FAB、小按钮
    medium = RoundedCornerShape(12.dp),        // Card、Dialog（M3 标准）
    large = RoundedCornerShape(16.dp),         // TonalCard、大卡片
    extraLarge = RoundedCornerShape(28.dp),    // BottomSheet、大型 Dialog
)
```

### 3.5 间距 Tokens

```kotlin
object Spacing {
    val xs = 4.dp    // 图标与文字间距
    val sm = 8.dp    // 卡片内元素间距
    val md = 12.dp   // 卡片间距、列表项间距
    val lg = 16.dp   // 屏幕边距、卡片内 padding
    val xl = 24.dp   // 区块间距
    val xxl = 32.dp  // 页面级间距
}
```

### 3.6 主题配置状态

```kotlin
// ThemeConfig.kt
data class ThemeConfig(
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val amoledMode: Boolean = false,
    val paletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT,
    val dynamicColor: Boolean = true,
    val seedColor: Color = Color(0xFF6750A4),
)

enum class ColorMode { SYSTEM, LIGHT, DARK }
enum class PaletteStyle { TONAL_SPOT, NEUTRAL, VIBRANT, EXPRESSIVE }
```

## 4. Expressive 组件库

### 4.1 组件清单（9 个共享组件）

#### 4.1.1 `ExpressiveScaffold`

M3 Expressive 版 Scaffold，默认 `containerColor = surfaceContainer`（色调表面）。

```kotlin
@Composable
fun ExpressiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable PaddingValues -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        content = content
    )
}
```

#### 4.1.2 `TonalCard`

M3 Expressive 色调卡片，`containerColor = surfaceBright`，`shape = large`，无阴影（用色调代替）。

#### 4.1.3 `WenyanTopAppBar`

统一 TopAppBar 封装，支持返回按钮，容器色 `surfaceContainer`。首页可选用 `LargeFlexibleTopAppBar` 变体（滚动时折叠）。

#### 4.1.4 `WenyanInfoChip`

统一 InfoChip，4 种 variant（NEUTRAL/PRIMARY/SECONDARY/ERROR），消除 `KnowledgePointDetail` 和 `Quiz` 的不一致实现。圆角 `shapes.extraSmall`。

#### 4.1.5 `EmptyState`

统一空状态组件，支持图标、标题、描述、可选操作按钮。

#### 4.1.6 `LoadingState`

统一加载态，`CircularProgressIndicator` + 可选标签。

#### 4.1.7 `SectionHeader`

区块标题，`titleMedium` 字体，右侧可选操作按钮。

#### 4.1.8 `ContentSourceBadge`（重构）

移除硬编码 `Color.White`，改用主题角色色。

#### 4.1.9 `Spacing`

间距 tokens 对象，6 级（xs/sm/md/lg/xl/xxl）。

### 4.2 内容来源五级标注颜色映射

| 来源 | 旧方案（硬编码） | 新方案（主题角色） |
|------|------------------|-------------------|
| TEXTBOOK_NATIVE | `#4CAF50` 绿 | `secondaryContainer` / `onSecondaryContainer` |
| TEXTBOOK_OCR | `#4CAF50` 绿 + OCR 文字 | 同上 |
| AI_GENERATED | `#2196F3` 蓝 | `tertiaryContainer` / `onTertiaryContainer` |
| HYBRID | `#FFC107` 黄 | `surfaceContainerHighest` / `onSurfaceVariant` |
| USER_CREATED | `#9E9E9E` 灰 | `surfaceContainerHigh` / `onSurfaceVariant` |
| MISSING | `#F44336` 红 + 警告图标 | `errorContainer` / `onErrorContainer` |

## 5. 各 Screen 改造方案

### 5.1 WenyanApp（主 Scaffold + 导航）

- `Scaffold` → `ExpressiveScaffold`（背景 `surfaceContainer`）
- `NavigationBar` 容器色 → `surfaceContainer`，选中指示器 → `secondaryContainer`
- 导航图标选中色 → `onSecondaryContainer`，未选中 → `onSurfaceVariant`
- 新增 `settings` 子路由

### 5.2 KnowledgeScreen（知识点列表）

- `TopAppBar` → `LargeFlexibleTopAppBar`（M3 Expressive 大标题，滚动折叠）
- 标题"知识点"用 `headlineMedium`（28sp）
- `CategoryChips` 的 `AssistChip` → `FilterChip`
- `KnowledgePointCard` → `TonalCard`
- 卡片内 padding 统一 `Spacing.lg`（16dp）
- 移除私有 `EmptyState`，用共享组件

> 注：仅 `KnowledgeScreen`（首页）使用 `LargeFlexibleTopAppBar`。其他列表页（`QuizScreen` 等）保持标准 `TopAppBar`（通过 `WenyanTopAppBar` 封装），避免大标题过度占用空间。

### 5.3 KnowledgePointDetailScreen（知识点详情）

- `Scaffold` → `ExpressiveScaffold` + `WenyanTopAppBar`
- 私有 `InfoChip` → 共享 `WenyanInfoChip`
- `PerspectiveCard`：isOfficial 用 `primaryContainer`，其他用 `surfaceContainerLow`
- `HorizontalDivider` 颜色 → `outlineVariant`
- 关联卡片 → `TonalCard`

### 5.4 QuizScreen（真题练习）

- `YearSelector` 的 `AssistChip` → `FilterChip`
- 移除私有 `InfoChip`，用共享 `WenyanInfoChip`（`ChipVariant.SECONDARY`）
- `QuestionCard` → `TonalCard`
- `AnswerSection` 的 `errorContainer` 背景现在有定义
- `AnimatedVisibility` 动画用 `MotionScheme.expressive()`

### 5.5 CardsScreen（记忆卡片）

- 翻转卡片正面 → `surfaceContainerHigh`，背面 → `secondaryContainer`
- `CardRenderer` 的 `tertiaryContainer` 现在有定义
- `SchoolComparisonCard` 的 `HorizontalDivider` → `outlineVariant`

### 5.6 GraphScreen（知识图谱）

- `LegendBar` 和 `StatsBar` 的 `Surface(surfaceVariant)` → `surfaceContainerLow`
- **GraphCanvas 颜色重构**：
  - 移除 `COLOR_GREEN/YELLOW/RED/GRAY` 硬编码
  - R≥0.8 → `primary`（已掌握）
  - 0.5≤R<0.8 → `tertiary`（需巩固）
  - 0<R<0.5 → `error`（薄弱）
  - R≤0 → `outline`（未学习）
  - 节点标签文字色 → `onSurface`
  - 边线色 → `outlineVariant`
  - 薄弱节点光晕 → `error.copy(alpha = 0.2f)`
  - 薄弱节点边框 → `error.copy(alpha = 0.6f)`
- 移除 `GraphScreen` 顶部 `LEGEND_*` 常量

### 5.7 AiAssistantScreen（AI 助手）

- TopAppBar actions：新增 settings 入口
- `MessageBubble` 用户消息背景 → `primaryContainer`，文字 → `onPrimaryContainer`（原 primary/onPrimary 对比度过高）
- AI 消息背景 → `surfaceContainerHigh`，文字 → `onSurface`
- 气泡圆角 → `shapes.large`
- `ReferencesList` 引用项 → `surfaceContainerLow` 背景
- 输入栏 `OutlinedTextField` 容器色 → `surfaceContainerLow`

### 5.8 ApiConfigScreen（API 配置）

- FAB 圆角 → `shapes.small`，色 → `primaryContainer`
- `ConfigCard` → `TonalCard`
- `ProviderChip` 改用 `FilterChip`（M3 规范组件）
- `AlertDialog` 圆角 → `shapes.extraLarge`（28dp），背景 → `surfaceContainerHigh`

### 5.9 MentorInfoScreen（导师信息）

- 内容 `Column` padding → `Spacing.xl`
- "前往官网"按钮 → `FilledTonalButton`

### 5.10 SettingsScreen（新建）

```
ExpressiveScaffold
  WenyanTopAppBar("设置")
  LazyColumn
    ├─ SectionHeader("外观")
    ├─ SegmentedList（主题模式：跟随系统/浅色/深色）
    ├─ SwitchItem（AMOLED 纯黑模式）
    ├─ SectionHeader("动态色彩")
    ├─ SwitchItem（动态色彩，Android 12+ 可用）
    ├─ ColorPickerItem（种子色选择，动态色彩关闭时可用）
    ├─ SegmentedList（调色板风格：TonalSpot/Neutral/Vibrant/Expressive）
    ├─ SectionHeader("AI 服务")
    ├─ ListItem（API 配置，点击跳转 api_config）
    ├─ SectionHeader("关于")
    └─ ListItem（版本号 v0.1.0）
```

## 6. 数据流与状态管理

### 6.1 主题状态流

```
DataStore<ThemeConfig> (持久化)
    ↓
ThemeRepository (core/data)
    ↓ StateFlow<ThemeConfig>
ThemeViewModel (app)
    ↓ StateFlow<ThemeConfig>
WenyanApp (collectAsState)
    ↓ ThemeConfig
WenyanTheme(config) { ... }
```

### 6.2 ThemeRepository

```kotlin
class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ThemeRepository {
    override val themeConfig: Flow<ThemeConfig> = dataStore.data.map { prefs ->
        ThemeConfig(
            colorMode = ColorMode.valueOf(prefs[COLOR_MODE_KEY] ?: ColorMode.SYSTEM.name),
            amoledMode = prefs[AMOLED_KEY] ?: false,
            paletteStyle = PaletteStyle.valueOf(prefs[PALETTE_STYLE_KEY] ?: PaletteStyle.TONAL_SPOT.name),
            dynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: true,
            seedColor = Color(prefs[SEED_COLOR_KEY] ?: 0xFF6750A4.toInt())
        )
    }

    override suspend fun setColorMode(mode: ColorMode) { ... }
    override suspend fun setAmoledMode(enabled: Boolean) { ... }
    override suspend fun setPaletteStyle(style: PaletteStyle) { ... }
    override suspend fun setDynamicColor(enabled: Boolean) { ... }
    override suspend fun setSeedColor(color: Color) { ... }
}
```

### 6.3 ThemeViewModel

```kotlin
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {
    val themeConfig: StateFlow<ThemeConfig> = themeRepository.themeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig())

    fun setColorMode(mode: ColorMode) = viewModelScope.launch { themeRepository.setColorMode(mode) }
    fun setAmoledMode(enabled: Boolean) = viewModelScope.launch { themeRepository.setAmoledMode(enabled) }
    fun setPaletteStyle(style: PaletteStyle) = viewModelScope.launch { themeRepository.setPaletteStyle(style) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { themeRepository.setDynamicColor(enabled) }
    fun setSeedColor(color: Color) = viewModelScope.launch { themeRepository.setSeedColor(color) }
}
```

### 6.4 WenyanApp 接入

```kotlin
@Composable
fun WenyanApp(viewModel: ThemeViewModel = hiltViewModel()) {
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()

    WenyanTheme(config = themeConfig) {
        val navController = rememberNavController()
        ExpressiveScaffold(
            bottomBar = { WenyanBottomBar(navController) }
        ) { padding ->
            WenyanNavHost(navController, Modifier.padding(padding))
        }
    }
}
```

### 6.5 GraphCanvas 颜色数据流

```kotlin
@Composable
fun GraphCanvas(...) {
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(colorScheme) {
        GraphColors(
            mastered = colorScheme.primary,
            consolidating = colorScheme.tertiary,
            weak = colorScheme.error,
            unlearned = colorScheme.outline,
            nodeLabel = colorScheme.onSurface,
            edge = colorScheme.outlineVariant,
            weakHalo = colorScheme.error.copy(alpha = 0.2f),
            weakBorder = colorScheme.error.copy(alpha = 0.6f)
        )
    }
    Canvas(...) {
        // 使用 colors.xxx 代替硬编码
    }
}
```

## 7. 错误处理与测试

### 7.1 错误处理

**主题相关**：
- `materialkolor` 的 `createColorScheme()` 不会抛异常，任何种子色都能生成合法色板
- `dynamicColorScheme` 在 Android 12+ 可用，低于 12 自动降级到 materialkolor 生成
- AMOLED 模式仅在 `isDark = true` 时生效，浅色模式忽略该开关
- DataStore 读取失败时 fallback 到 `ThemeConfig()` 默认值

**UI 相关**：
- 各 Screen 的 `UiState` 保持现有 Loading/Error/Success 三态模式
- 网络错误用 `errorContainer` 背景 + `onErrorContainer` 文字提示
- 空数据用共享 `EmptyState` 组件
- 加载中用共享 `LoadingState` 组件

### 7.2 测试策略

**单元测试**（保持现有 61+ 测试通过）：
- `ThemeRepositoryImpl`：测试 DataStore 读写、默认值、枚举序列化
- `ThemeViewModel`：测试 StateFlow 发射、状态转换
- 现有业务逻辑测试不受影响

**UI 测试**（新增）：
```kotlin
@HiltAndroidTest
class SettingsScreenTest {
    @Test fun themeModeSwitch_updatesDataStore() { ... }
    @Test fun amoledToggle_updatesDataStore() { ... }
    @Test fun dynamicColorToggle_updatesDataStore() { ... }
}

@HiltAndroidTest
class ThemeTest {
    @Test fun themeConfig_appliedToComposition() { ... }
    @Test fun amoledMode_setsBlackBackground() { ... }
}
```

**视觉回归测试**（通过 `@Preview`）：
- 为每个新组件提供 `@Preview`（浅色/深色/AMOLED 三种）
- 为每个改造后的 Screen 提供 `@Preview`

### 7.3 兼容性保证

- `minSdk = 26`（Android 8.0）保持不变
- `materialkolor` 支持 Android 5+，无兼容问题
- `MaterialExpressiveTheme` 需要 `material3:1.4.0-alpha15+`，需确认与 `compose-bom` 兼容
- `MotionScheme.expressive()` 同上
- 现有 Hilt DI、Room DB、Repository 架构不变，仅新增 ThemeRepository

### 7.4 验证清单

- [ ] CI 编译通过（assembleDebug + assembleRelease）
- [ ] 全部 61+ 单元测试通过
- [ ] 新增主题相关单元测试通过
- [ ] 各 Screen 在浅色/深色/AMOLED 下视觉正确
- [ ] 动态色彩在 Android 12+ 正确跟随壁纸
- [ ] 非 Android 12 设备用预设种子色生成色板
- [ ] NavigationBar 选中/未选中态正确
- [ ] GraphCanvas 颜色随主题变化
- [ ] ContentSourceBadge 颜色随主题变化
- [ ] 签名 APK 构建成功（Release workflow）

## 8. 参考资源

- [KernelSU GitHub 仓库](https://github.com/tiann/KernelSU) — 主题架构和 Expressive 组件封装参考
- [Material Design 3 官方文档](https://m3.material.io/)
- [materialkolor 库](https://github.com/jordond/materialkolor) — 动态色彩生成
- [Material 3 Expressive 2025 发布](https://material.io/blog/material-3-expressive-2025)

## 9. 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| `MaterialExpressiveTheme` API 不稳定（alpha 版） | 锁定 `1.4.0-alpha15` 版本，CI 验证编译 |
| `materialkolor` 与 `compose-bom` 版本冲突 | 单独引入，不通过 BOM 管理 |
| 改动量大导致回归 | 分批改造，每批 CI 验证；保持现有 61+ 测试全通过 |
| 动态色彩在低端设备性能问题 | `remember(config)` 缓存 ColorScheme，避免重复计算 |
| AMOLED 模式对比度不足 | 仅替换底层表面，保留 `surfaceContainerHigh` 等高层级表面 |
