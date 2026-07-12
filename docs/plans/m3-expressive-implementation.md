# 文研 M3 Expressive 改造实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将文研 Android 应用 UI 全面升级为 Material 3 Expressive（2025版），达到 KernelSU 级别的"谷歌味道"，采用动态色彩、AMOLED 支持、完整 M3 组件族。

**Architecture:** 以 `core:designsystem` 为核心，引入 materialkolor 4.1.1 动态色彩生成 + material3 1.4 `MaterialExpressiveTheme`。主题配置通过 DataStore 持久化，经 ThemeRepository → ThemeViewModel → WenyanApp → WenyanTheme 流转。新建 9 个共享 Expressive 组件，全量改造 7 个 Screen，新建独立 Settings 页面。

**Tech Stack:** Kotlin 2.0.20, Compose BOM 2025.12.00, material3 1.4.x (MaterialExpressiveTheme + MotionScheme.expressive()), materialkolor 4.1.1, DataStore Preferences, Hilt DI

---

## File Structure

### 新建文件
```
core/designsystem/src/main/java/com/wenyan/app/core/designsystem/
├── theme/
│   ├── ThemeConfig.kt          # 主题配置数据模型 + 枚举 + 映射
│   └── Shapes.kt               # M3 Shapes 5 级圆角
├── component/
│   ├── Spacing.kt              # 间距 tokens (xs/sm/md/lg/xl/xxl)
│   ├── ExpressiveScaffold.kt   # M3E Scaffold 封装
│   ├── TonalCard.kt            # 色调卡片
│   ├── WenyanTopAppBar.kt      # 统一 TopAppBar
│   ├── WenyanInfoChip.kt       # 统一 InfoChip
│   ├── EmptyState.kt           # 空状态
│   ├── LoadingState.kt         # 加载态
│   └── SectionHeader.kt        # 区块标题

core/data/src/main/java/com/wenyan/app/core/data/
├── repository/
│   ├── ThemeRepository.kt      # 接口
│   └── ThemeRepositoryImpl.kt  # DataStore 实现
└── di/
    └── ThemeModule.kt          # Hilt DI

app/src/main/java/com/wenyan/app/
└── ThemeViewModel.kt           # 主题状态管理

feature/settings/               # 新建模块
├── build.gradle.kts
└── src/main/java/com/wenyan/app/feature/settings/
    └── SettingsScreen.kt       # 设置页面
```

### 修改文件
```
gradle/libs.versions.toml                              # 升级 composeBom + 添加 materialkolor
settings.gradle.kts                                    # 添加 :feature:settings
core/designsystem/build.gradle.kts                     # 添加 materialkolor 依赖
core/designsystem/.../theme/Color.kt                   # 移除硬编码，保留内容来源映射
core/designsystem/.../theme/Type.kt                    # 补全 15 样式
core/designsystem/.../theme/WenyanTheme.kt             # 重写为 MaterialExpressiveTheme
core/designsystem/.../component/ContentSourceBadge.kt  # 走主题色
app/build.gradle.kts                                   # compileSdk=35 + :feature:settings 依赖
app/.../MainActivity.kt                                # 移除 WenyanTheme 包裹（移入 WenyanApp）
app/.../WenyanApp.kt                                   # 接入 ThemeViewModel + ExpressiveScaffold
app/.../navigation/WenyanNavHost.kt                    # 新增 settings 路由
feature/knowledge/.../KnowledgeScreen.kt               # LargeFlexibleTopAppBar + TonalCard
feature/knowledge/.../KnowledgePointDetailScreen.kt    # WenyanInfoChip + TonalCard
feature/quiz/.../QuizScreen.kt                         # WenyanInfoChip + TonalCard
feature/cards/.../CardsScreen.kt                       # 主题色卡片
feature/graph/.../GraphScreen.kt                       # GraphCanvas 走主题色
feature/aiassistant/.../AiAssistantScreen.kt           # settings 入口 + 主题色气泡
feature/aiassistant/.../ApiConfigScreen.kt             # TonalCard + FilterChip
feature/aiassistant/.../MentorInfoScreen.kt            # Spacing + FilledTonalButton
所有 build.gradle.kts                                   # compileSdk 34→35
```

---

## Phase 1: 依赖升级与基础准备

### Task 1: 升级依赖（composeBom + materialkolor + compileSdk）

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `core/designsystem/build.gradle.kts`
- Modify: `app/build.gradle.kts` (compileSdk)
- Modify: 所有 `build.gradle.kts` 中的 `compileSdk = 34` → `compileSdk = 35`

- [ ] **Step 1: 升级 libs.versions.toml**

将 `composeBom` 从 `2024.06.00` 升级到 `2025.12.00`，添加 materialkolor 版本和库定义。

在 `gradle/libs.versions.toml` 的 `[versions]` 部分，找到 `composeBom = "2024.06.00"` 替换为：

```toml
composeBom = "2025.12.00"
```

在 `[versions]` 部分末尾（`securityCrypto` 之后）添加：

```toml
# MaterialKolor 动态色彩生成库（KSU 同款）
materialKolor = "4.1.1"
```

在 `[libraries]` 部分，找到 `androidx-compose-material3` 行，在其后添加：

```toml
# MaterialKolor 动态色彩
materialkolor = { group = "com.materialkolor", name = "material-kolor", version.ref = "materialKolor" }
```

- [ ] **Step 2: 在 core:designsystem 添加 materialkolor 依赖**

在 `core/designsystem/build.gradle.kts` 的 `dependencies` 块中，找到 `implementation(libs.androidx.compose.material3)` 行，在其后添加：

```kotlin
    // MaterialKolor 动态色彩生成（KSU 同款）
    api(libs.materialkolor)
```

注意：使用 `api` 而非 `implementation`，因为 `WenyanTheme.kt` 中的 `rememberDynamicColorScheme` 需要被依赖模块访问。

- [ ] **Step 3: 升级所有模块的 compileSdk 从 34 到 35**

在以下所有文件中，将 `compileSdk = 34` 替换为 `compileSdk = 35`：

- `app/build.gradle.kts`
- `core/common/build.gradle.kts`
- `core/database/build.gradle.kts`
- `core/data/build.gradle.kts`
- `core/designsystem/build.gradle.kts`
- `core/fsrs/build.gradle.kts`
- `core/ai/build.gradle.kts`
- `feature/knowledge/build.gradle.kts`
- `feature/quiz/build.gradle.kts`
- `feature/cards/build.gradle.kts`
- `feature/graph/build.gradle.kts`
- `feature/aiassistant/build.gradle.kts`

同时将 `app/build.gradle.kts` 中的 `targetSdk = 34` 替换为 `targetSdk = 35`。

- [ ] **Step 4: 验证编译**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL（如果 composeBom 2025.12.00 不存在，改为 `2025.10.00` 或 `2025.11.00` 重试）

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml core/designsystem/build.gradle.kts app/build.gradle.kts
git add core/common/build.gradle.kts core/database/build.gradle.kts core/data/build.gradle.kts
git add core/fsrs/build.gradle.kts core/ai/build.gradle.kts
git add feature/knowledge/build.gradle.kts feature/quiz/build.gradle.kts feature/cards/build.gradle.kts
git add feature/graph/build.gradle.kts feature/aiassistant/build.gradle.kts
git commit -m "build: upgrade composeBom to 2025.12.00 + add materialkolor 4.1.1 + compileSdk 35"
```

---

### Task 2: 创建 ThemeConfig 数据模型

**Files:**
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/ThemeConfig.kt`

- [ ] **Step 1: 创建 ThemeConfig.kt**

```kotlin
package com.wenyan.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/**
 * 主题配置状态。
 *
 * @param colorMode 颜色模式（跟随系统/浅色/深色）
 * @param amoledMode AMOLED 纯黑模式（仅深色模式生效）
 * @param paletteStyle 调色板风格
 * @param dynamicColor 是否使用动态色彩（Android 12+ 自动跟随壁纸）
 * @param seedColor 种子色（动态色彩关闭时使用）
 */
data class ThemeConfig(
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val amoledMode: Boolean = false,
    val paletteStyle: WenyanPaletteStyle = WenyanPaletteStyle.TONAL_SPOT,
    val dynamicColor: Boolean = true,
    val seedColor: Color = Color(0xFF6750A4),
)

/**
 * 颜色模式。
 */
enum class ColorMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * 调色板风格（映射到 materialkolor 的 PaletteStyle）。
 */
enum class WenyanPaletteStyle {
    TONAL_SPOT,
    NEUTRAL,
    VIBRANT,
    EXPRESSIVE,
}

/**
 * 将 [WenyanPaletteStyle] 转换为 materialkolor 的 [PaletteStyle]。
 */
fun WenyanPaletteStyle.toMaterialKolorStyle(): PaletteStyle = when (this) {
    WenyanPaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
    WenyanPaletteStyle.NEUTRAL -> PaletteStyle.Neutral
    WenyanPaletteStyle.VIBRANT -> PaletteStyle.Vibrant
    WenyanPaletteStyle.EXPRESSIVE -> PaletteStyle.Expressive
}
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/ThemeConfig.kt
git commit -m "feat: add ThemeConfig data model with ColorMode and PaletteStyle enums"
```

---

### Task 3: 创建 Spacing 间距 Tokens

**Files:**
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/Spacing.kt`

- [ ] **Step 1: 创建 Spacing.kt**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.ui.unit.dp

/**
 * 间距 tokens（6 级），统一全 App 间距体系。
 *
 * - [xs]：4dp — 图标与文字间距
 * - [sm]：8dp — 卡片内元素间距
 * - [md]：12dp — 卡片间距、列表项间距
 * - [lg]：16dp — 屏幕边距、卡片内 padding
 * - [xl]：24dp — 区块间距
 * - [xxl]：32dp — 页面级间距
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}
```

- [ ] **Step 2: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/Spacing.kt
git commit -m "feat: add Spacing tokens (xs/sm/md/lg/xl/xxl)"
```

---

## Phase 2: 主题系统重写

### Task 4: 重写 Color.kt（移除硬编码色）

**Files:**
- Modify: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Color.kt`

- [ ] **Step 1: 检查硬编码色的引用**

搜索所有引用 `WenyanPrimary`、`WenyanSecondary`、`SourceTextbook`、`SourceAi`、`SourceHybrid`、`SourceUser`、`SourceMissing` 等旧颜色常量的文件。

Run: `grep -rn "WenyanPrimary\|WenyanSecondary\|WenyanTertiary\|WenyanBackground\|WenyanSurface\|WenyanError\|SourceTextbook\|SourceAi\|SourceHybrid\|SourceUser\|SourceMissing" --include="*.kt" core/ feature/ app/`

记录引用位置，后续 Task 改造各 Screen 时逐一替换。

- [ ] **Step 2: 重写 Color.kt**

将 `Color.kt` 的完整内容替换为：

```kotlin
package com.wenyan.app.core.designsystem.theme

// 文研App 颜色系统
//
// 所有颜色角色由 materialkolor 的 dynamicColorScheme() 在运行时生成，
// 不再使用硬编码颜色常量。
//
// 内容来源五级标注的颜色映射在 ContentSourceBadge.kt 中通过
// MaterialTheme.colorScheme 角色色实现，不在此处定义常量。

/** 默认种子色（Material 3 经典紫色） */
val DefaultSeedColor = androidx.compose.ui.graphics.Color(0xFF6750A4)
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL（如果有其他模块引用了被删除的颜色常量，暂时注释掉引用处，后续 Task 会修复）

注意：如果编译失败因为其他模块引用了 `SourceTextbook` 等常量，暂时在 `Color.kt` 中保留这些常量定义，在 Task 16（重构 ContentSourceBadge）时再删除。

- [ ] **Step 4: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Color.kt
git commit -m "refactor: remove hardcoded colors, switch to materialkolor dynamic color scheme"
```

---

### Task 5: 补全 Type.kt（15 样式）

**Files:**
- Modify: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Type.kt`

- [ ] **Step 1: 重写 Type.kt**

将 `Type.kt` 的完整内容替换为：

```kotlin
package com.wenyan.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 文研App 字体排版（Material 3 Expressive 完整 15 样式）。
 *
 * 参考 M3 设计规范：
 * https://m3.material.io/styles/typography/type-scale-tokens
 */
val WenyanTypography = Typography(
    // Display — 超大标题（页面级，极少使用）
    displayLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    // Headline — 大标题
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    // Title — 标题
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Body — 正文
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Label — 标签
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Type.kt
git commit -m "feat: complete M3 Typography with all 15 styles"
```

---

### Task 6: 创建 Shapes.kt

**Files:**
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Shapes.kt`

- [ ] **Step 1: 创建 Shapes.kt**

```kotlin
package com.wenyan.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 文研App 形状系统（Material 3 Expressive 圆角规范）。
 *
 * - [extraSmall]：4dp — InfoChip、小标签
 * - [small]：8dp — FAB、小按钮
 * - [medium]：12dp — Card、Dialog（M3 标准）
 * - [large]：16dp — TonalCard、大卡片
 * - [extraLarge]：28dp — BottomSheet、大型 Dialog
 */
val WenyanShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
```

- [ ] **Step 2: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Shapes.kt
git commit -m "feat: add M3 Shapes with 5-level corner radius"
```

---

### Task 7: 重写 WenyanTheme.kt

**Files:**
- Modify: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/WenyanTheme.kt`

- [ ] **Step 1: 重写 WenyanTheme.kt**

将 `WenyanTheme.kt` 的完整内容替换为：

```kotlin
package com.wenyan.app.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

/**
 * 文研App 主题入口（Material 3 Expressive）。
 *
 * 使用 [MaterialExpressiveTheme] + [MotionScheme.expressive] 实现 M3 Expressive 设计语言。
 * 颜色方案由以下优先级生成：
 * 1. Android 12+ 且 [ThemeConfig.dynamicColor] 开启 → 系统壁纸动态色彩
 * 2. 其他情况 → materialkolor 从种子色生成（SPEC_2025 规范）
 *
 * AMOLED 模式在深色模式下将底层表面替换为纯黑，节省 OLED 电量。
 *
 * @param config 主题配置
 * @param content 可组合内容
 */
@Composable
fun WenyanTheme(
    config: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = when (config.colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }

    // 生成基础 ColorScheme
    val baseScheme = if (config.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+：使用系统壁纸提取的动态色彩
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Android 11- 或手动种子色：用 materialkolor 生成
        rememberDynamicColorScheme(
            seedColor = config.seedColor,
            isDark = isDark,
            style = config.paletteStyle.toMaterialKolorStyle(),
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
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
    } else {
        baseScheme
    }

    MaterialExpressiveTheme(
        colorScheme = finalScheme,
        motionScheme = MotionScheme.expressive(),
        typography = WenyanTypography,
        shapes = WenyanShapes,
        content = content,
    )
}
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL

注意：如果 `MaterialExpressiveTheme` 或 `MotionScheme.expressive()` 无法解析，确认 composeBom 已升级到 `2025.12.00`。如果 BOM 版本对应的 material3 仍为 1.3.x，则需要在 `core/designsystem/build.gradle.kts` 中单独指定 material3 版本：

```kotlin
implementation("androidx.compose.material3:material3:1.4.0")
```

- [ ] **Step 3: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/WenyanTheme.kt
git commit -m "feat: rewrite WenyanTheme with MaterialExpressiveTheme + materialkolor dynamic colors"
```

---

## Phase 3: 数据层

### Task 8: 创建 ThemeRepository 接口

**Files:**
- Create: `core/data/src/main/java/com/wenyan/app/core/data/repository/ThemeRepository.kt`

- [ ] **Step 1: 创建 ThemeRepository.kt**

```kotlin
package com.wenyan.app.core.data.repository

import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow

/**
 * 主题配置仓库接口。
 *
 * 提供 [ThemeConfig] 的读取和持久化能力，
 * 底层使用 DataStore Preferences 存储。
 */
interface ThemeRepository {
    val themeConfig: Flow<ThemeConfig>

    suspend fun setColorMode(mode: ColorMode)
    suspend fun setAmoledMode(enabled: Boolean)
    suspend fun setPaletteStyle(style: WenyanPaletteStyle)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setSeedColor(color: Color)
}
```

注意：`core:data` 模块需要依赖 `core:designsystem`。检查 `core/data/build.gradle.kts`，如果没有 `implementation(project(":core:designsystem"))` 则添加。

- [ ] **Step 2: 添加 core:designsystem 依赖到 core:data**

在 `core/data/build.gradle.kts` 的 `dependencies` 块中，找到 `implementation(project(":core:common"))` 行，在其后添加：

```kotlin
    implementation(project(":core:designsystem"))
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :core:data:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add core/data/src/main/java/com/wenyan/app/core/data/repository/ThemeRepository.kt
git add core/data/build.gradle.kts
git commit -m "feat: add ThemeRepository interface"
```

---

### Task 9: 实现 ThemeRepositoryImpl（TDD）

**Files:**
- Create: `core/data/src/main/java/com/wenyan/app/core/data/repository/ThemeRepositoryImpl.kt`
- Test: `core/data/src/test/java/com/wenyan/app/core/data/repository/ThemeRepositoryImplTest.kt`

- [ ] **Step 1: 写失败测试**

创建测试文件 `core/data/src/test/java/com/wenyan/app/core/data/repository/ThemeRepositoryImplTest.kt`：

```kotlin
package com.wenyan.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeRepositoryImplTest {

    @Test
    fun `default config returns system mode and tonal spot style`() = runTest {
        val repo = ThemeRepositoryImpl(FakeDataStore())
        val config = repo.themeConfig.first()
        assertEquals(ColorMode.SYSTEM, config.colorMode)
        assertEquals(false, config.amoledMode)
        assertEquals(WenyanPaletteStyle.TONAL_SPOT, config.paletteStyle)
        assertEquals(true, config.dynamicColor)
    }

    @Test
    fun `setColorMode persists dark mode`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = ThemeRepositoryImpl(fakeStore)
        repo.setColorMode(ColorMode.DARK)
        val config = repo.themeConfig.first()
        assertEquals(ColorMode.DARK, config.colorMode)
    }

    @Test
    fun `setAmoledMode persists enabled`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = ThemeRepositoryImpl(fakeStore)
        repo.setAmoledMode(true)
        val config = repo.themeConfig.first()
        assertEquals(true, config.amoledMode)
    }

    @Test
    fun `setPaletteStyle persists expressive`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = ThemeRepositoryImpl(fakeStore)
        repo.setPaletteStyle(WenyanPaletteStyle.EXPRESSIVE)
        val config = repo.themeConfig.first()
        assertEquals(WenyanPaletteStyle.EXPRESSIVE, config.paletteStyle)
    }

    @Test
    fun `setDynamicColor persists disabled`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = ThemeRepositoryImpl(fakeStore)
        repo.setDynamicColor(false)
        val config = repo.themeConfig.first()
        assertEquals(false, config.dynamicColor)
    }

    @Test
    fun `setSeedColor persists custom color`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = ThemeRepositoryImpl(fakeStore)
        repo.setSeedColor(Color.Red)
        val config = repo.themeConfig.first()
        assertEquals(Color.Red, config.seedColor)
    }

    @Test
    fun `themeConfig emits updates on change`() = runTest {
        val repo = ThemeRepositoryImpl(FakeDataStore())
        repo.themeConfig.test {
            // 初始值
            assertEquals(ColorMode.SYSTEM, awaitItem().colorMode)
            // 设置后发射新值
            repo.setColorMode(ColorMode.DARK)
            assertEquals(ColorMode.DARK, awaitItem().colorMode)
        }
    }
}

/**
 * Fake DataStore for testing.
 */
private class FakeDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        val newValue = transform(state.value)
        state.value = newValue
        return newValue
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "com.wenyan.app.core.data.repository.ThemeRepositoryImplTest"`
Expected: FAIL — `ThemeRepositoryImpl` 未定义

- [ ] **Step 3: 实现 ThemeRepositoryImpl**

创建 `core/data/src/main/java/com/wenyan/app/core/data/repository/ThemeRepositoryImpl.kt`：

```kotlin
package com.wenyan.app.core.data.repository

import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [ThemeRepository] 的 DataStore Preferences 实现。
 *
 * 使用以下键存储 [ThemeConfig] 各字段：
 * - COLOR_MODE_KEY: String (枚举 name)
 * - AMOLED_KEY: Boolean
 * - PALETTE_STYLE_KEY: String (枚举 name)
 * - DYNAMIC_COLOR_KEY: Boolean
 * - SEED_COLOR_KEY: Int (ARGB)
 */
class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemeRepository {

    override val themeConfig: Flow<ThemeConfig> = dataStore.data.map { prefs ->
        ThemeConfig(
            colorMode = ColorMode.valueOf(
                prefs[COLOR_MODE_KEY] ?: ColorMode.SYSTEM.name,
            ),
            amoledMode = prefs[AMOLED_KEY] ?: false,
            paletteStyle = WenyanPaletteStyle.valueOf(
                prefs[PALETTE_STYLE_KEY] ?: WenyanPaletteStyle.TONAL_SPOT.name,
            ),
            dynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: true,
            seedColor = Color(prefs[SEED_COLOR_KEY] ?: 0xFF6750A4.toInt()),
        )
    }

    override suspend fun setColorMode(mode: ColorMode) {
        dataStore.edit { it[COLOR_MODE_KEY] = mode.name }
    }

    override suspend fun setAmoledMode(enabled: Boolean) {
        dataStore.edit { it[AMOLED_KEY] = enabled }
    }

    override suspend fun setPaletteStyle(style: WenyanPaletteStyle) {
        dataStore.edit { it[PALETTE_STYLE_KEY] = style.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    override suspend fun setSeedColor(color: Color) {
        dataStore.edit { it[SEED_COLOR_KEY] = color.value.toInt() }
    }

    private companion object {
        val COLOR_MODE_KEY = stringPreferencesKey("color_mode")
        val AMOLED_KEY = booleanPreferencesKey("amoled_mode")
        val PALETTE_STYLE_KEY = stringPreferencesKey("palette_style")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val SEED_COLOR_KEY = intPreferencesKey("seed_color")
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "com.wenyan.app.core.data.repository.ThemeRepositoryImplTest"`
Expected: 7 tests PASS

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/java/com/wenyan/app/core/data/repository/ThemeRepositoryImpl.kt
git add core/data/src/test/java/com/wenyan/app/core/data/repository/ThemeRepositoryImplTest.kt
git commit -m "feat: implement ThemeRepositoryImpl with DataStore (TDD, 7 tests)"
```

---

### Task 10: 创建 ThemeModule（Hilt DI）

**Files:**
- Create: `core/data/src/main/java/com/wenyan/app/core/data/di/ThemeModule.kt`
- Modify: `core/data/src/main/java/com/wenyan/app/core/data/di/DataModule.kt` (添加 DataStore 提供)

- [ ] **Step 1: 创建 ThemeModule.kt**

```kotlin
package com.wenyan.app.core.data.di

import com.wenyan.app.core.data.repository.ThemeRepository
import com.wenyan.app.core.data.repository.ThemeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 主题仓库 Hilt 模块。
 *
 * 将 [ThemeRepositoryImpl] 绑定到 [ThemeRepository] 接口。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository
}
```

- [ ] **Step 2: 在 DataModule 中添加 DataStore 提供**

在 `core/data/src/main/java/com/wenyan/app/core/data/di/DataModule.kt` 中，将 `abstract class DataModule` 拆分为 abstract + object 两个部分，或新建一个 `DataStoreModule`。

为保持简洁，新建 `DataStoreModule`：

创建 `core/data/src/main/java/com/wenyan/app/core/data/di/DataStoreModule.kt`：

```kotlin
package com.wenyan.app.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DataStore Preferences Hilt 模块。
 *
 * 提供全局唯一的 [DataStore]<[Preferences]> 实例。
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.dataStoreFile("wenyan_preferences.preferences_pb") },
    )
}
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :core:data:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add core/data/src/main/java/com/wenyan/app/core/data/di/ThemeModule.kt
git add core/data/src/main/java/com/wenyan/app/core/data/di/DataStoreModule.kt
git commit -m "feat: add ThemeModule and DataStoreModule for Hilt DI"
```

---

### Task 11: 创建 ThemeViewModel（TDD）

**Files:**
- Create: `app/src/main/java/com/wenyan/app/ThemeViewModel.kt`
- Test: `app/src/test/java/com/wenyan/app/ThemeViewModelTest.kt`

- [ ] **Step 1: 写失败测试**

创建 `app/src/test/java/com/wenyan/app/ThemeViewModelTest.kt`：

```kotlin
package com.wenyan.app

import app.cash.turbine.test
import com.wenyan.app.core.data.repository.ThemeRepository
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeViewModelTest {

    @Test
    fun `initial state is default ThemeConfig`() = runTest {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        assertEquals(ThemeConfig(), viewModel.themeConfig.value)
    }

    @Test
    fun `setColorMode updates state to DARK`() = runTest {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        viewModel.setColorMode(ColorMode.DARK)
        assertEquals(ColorMode.DARK, viewModel.themeConfig.value.colorMode)
    }

    @Test
    fun `setAmoledMode updates state to true`() = runTest {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        viewModel.setAmoledMode(true)
        assertEquals(true, viewModel.themeConfig.value.amoledMode)
    }

    @Test
    fun `setPaletteStyle updates state to EXPRESSIVE`() = runTest {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        viewModel.setPaletteStyle(WenyanPaletteStyle.EXPRESSIVE)
        assertEquals(WenyanPaletteStyle.EXPRESSIVE, viewModel.themeConfig.value.paletteStyle)
    }

    @Test
    fun `themeConfig emits updates`() = runTest {
        val repo = FakeThemeRepository()
        val viewModel = ThemeViewModel(repo)
        viewModel.themeConfig.test {
            assertEquals(ThemeConfig(), awaitItem())
            viewModel.setDynamicColor(false)
            assertEquals(false, awaitItem().dynamicColor)
        }
    }
}

private class FakeThemeRepository : ThemeRepository {
    private val config = MutableStateFlow(ThemeConfig())
    override val themeConfig: Flow<ThemeConfig> = config

    override suspend fun setColorMode(mode: ColorMode) {
        config.value = config.value.copy(colorMode = mode)
    }

    override suspend fun setAmoledMode(enabled: Boolean) {
        config.value = config.value.copy(amoledMode = enabled)
    }

    override suspend fun setPaletteStyle(style: WenyanPaletteStyle) {
        config.value = config.value.copy(paletteStyle = style)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        config.value = config.value.copy(dynamicColor = enabled)
    }

    override suspend fun setSeedColor(color: Color) {
        config.value = config.value.copy(seedColor = color)
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.wenyan.app.ThemeViewModelTest"`
Expected: FAIL — `ThemeViewModel` 未定义

- [ ] **Step 3: 实现 ThemeViewModel**

创建 `app/src/main/java/com/wenyan/app/ThemeViewModel.kt`：

```kotlin
package com.wenyan.app

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ThemeRepository
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主题状态 ViewModel。
 *
 * 将 [ThemeRepository] 的 [ThemeConfig] Flow 转换为 [StateFlow] 供 Compose 消费。
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val themeConfig: StateFlow<ThemeConfig> = themeRepository.themeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig())

    fun setColorMode(mode: ColorMode) = viewModelScope.launch {
        themeRepository.setColorMode(mode)
    }

    fun setAmoledMode(enabled: Boolean) = viewModelScope.launch {
        themeRepository.setAmoledMode(enabled)
    }

    fun setPaletteStyle(style: WenyanPaletteStyle) = viewModelScope.launch {
        themeRepository.setPaletteStyle(style)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        themeRepository.setDynamicColor(enabled)
    }

    fun setSeedColor(color: Color) = viewModelScope.launch {
        themeRepository.setSeedColor(color)
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.wenyan.app.ThemeViewModelTest"`
Expected: 5 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wenyan/app/ThemeViewModel.kt
git add app/src/test/java/com/wenyan/app/ThemeViewModelTest.kt
git commit -m "feat: add ThemeViewModel with StateFlow (TDD, 5 tests)"
```

---

## Phase 4: 共享组件库

### Task 12: 创建 ExpressiveScaffold + TonalCard

**Files:**
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/ExpressiveScaffold.kt`
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/TonalCard.kt`

- [ ] **Step 1: 创建 ExpressiveScaffold.kt**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * M3 Expressive 版 Scaffold。
 *
 * 默认容器色使用 [androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer]，
 * 以色调表面代替纯色背景，实现 M3 Expressive 的层级表达。
 */
@Composable
fun ExpressiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}
```

- [ ] **Step 2: 创建 TonalCard.kt**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * M3 Expressive 色调卡片。
 *
 * 容器色使用 [MaterialTheme.colorScheme.surfaceBright]，
 * 形状使用 [MaterialTheme.shapes.large]（16dp），
 * 无阴影（用色调分层代替阴影表达层级）。
 */
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
    ) {
        content()
    }
}

/**
 * 低层级色调卡片。
 *
 * 容器色使用 [MaterialTheme.colorScheme.surfaceContainerLow]，
 * 用于次要信息的分组容器。
 */
@Composable
fun TonalCardLow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        content()
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/ExpressiveScaffold.kt
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/TonalCard.kt
git commit -m "feat: add ExpressiveScaffold and TonalCard components"
```

---

### Task 13: 创建 WenyanTopAppBar

**Files:**
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanTopAppBar.kt`

- [ ] **Step 1: 创建 WenyanTopAppBar.kt**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 文研统一 TopAppBar。
 *
 * 容器色使用 [MaterialTheme.colorScheme.surfaceContainer]，
 * 标题使用 [MaterialTheme.typography.titleLarge]。
 *
 * @param title 标题文本
 * @param modifier 修饰符
 * @param onBack 返回按钮回调，为 null 时不显示返回按钮
 * @param actions 右侧操作区
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WenyanTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = title) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanTopAppBar.kt
git commit -m "feat: add WenyanTopAppBar with surfaceContainer color"
```

---

### Task 14: 创建 WenyanInfoChip + SectionHeader

**Files:**
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanInfoChip.kt`
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/SectionHeader.kt`

- [ ] **Step 1: 创建 WenyanInfoChip.kt**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * InfoChip 变体。
 */
enum class ChipVariant {
    NEUTRAL,
    PRIMARY,
    SECONDARY,
    TERTIARY,
    ERROR,
}

/**
 * 文研统一 InfoChip。
 *
 * 消除 KnowledgePointDetail 和 Quiz 中不一致的 InfoChip 实现。
 * 根据 [variant] 使用不同的主题角色色。
 *
 * @param text 标签文本
 * @param variant 颜色变体
 * @param modifier 修饰符
 */
@Composable
fun WenyanInfoChip(
    text: String,
    modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.NEUTRAL,
) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (variant) {
        ChipVariant.NEUTRAL -> colorScheme.surfaceContainerHigh to colorScheme.onSurfaceVariant
        ChipVariant.PRIMARY -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        ChipVariant.SECONDARY -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        ChipVariant.TERTIARY -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        ChipVariant.ERROR -> colorScheme.errorContainer to colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.then(
                androidx.compose.foundation.layout.padding(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                ),
            ),
        )
    }
}
```

注意：需要添加 `import androidx.compose.ui.unit.dp` 和 `import androidx.compose.foundation.layout.padding`。

修正后的完整 import 和实现：

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ChipVariant {
    NEUTRAL,
    PRIMARY,
    SECONDARY,
    TERTIARY,
    ERROR,
}

@Composable
fun WenyanInfoChip(
    text: String,
    modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.NEUTRAL,
) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (variant) {
        ChipVariant.NEUTRAL -> colorScheme.surfaceContainerHigh to colorScheme.onSurfaceVariant
        ChipVariant.PRIMARY -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        ChipVariant.SECONDARY -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        ChipVariant.TERTIARY -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        ChipVariant.ERROR -> colorScheme.errorContainer to colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
```

- [ ] **Step 2: 创建 SectionHeader.kt**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 区块标题。
 *
 * 左侧标题使用 [MaterialTheme.typography.titleMedium]，
 * 右侧可选操作按钮（如"查看全部"）。
 *
 * @param title 标题文本
 * @param modifier 修饰符
 * @param action 右侧操作内容
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        action()
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/WenyanInfoChip.kt
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/SectionHeader.kt
git commit -m "feat: add WenyanInfoChip (5 variants) and SectionHeader components"
```

---

### Task 15: 创建 EmptyState + LoadingState

**Files:**
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/EmptyState.kt`
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/LoadingState.kt`

- [ ] **Step 1: 创建 EmptyState.kt**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 统一空状态组件。
 *
 * @param icon 空状态图标
 * @param title 标题
 * @param description 描述（可选）
 * @param modifier 修饰符
 * @param action 底部操作按钮（可选）
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        action()
    }
}
```

- [ ] **Step 2: 创建 LoadingState.kt**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 统一加载态组件。
 *
 * @param modifier 修饰符
 * @param label 加载提示文本（可选）
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/EmptyState.kt
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/LoadingState.kt
git commit -m "feat: add EmptyState and LoadingState shared components"
```

---

### Task 16: 重构 ContentSourceBadge

**Files:**
- Modify: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/ContentSourceBadge.kt`

- [ ] **Step 1: 重写 ContentSourceBadge.kt**

将 `ContentSourceBadge.kt` 的完整内容替换为：

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 内容来源标注类型（Spec Task 24 五级标注 + 1 特殊状态）。
 */
object ContentSource {
    const val TEXTBOOK_NATIVE = "TEXTBOOK_NATIVE"
    const val TEXTBOOK_OCR = "TEXTBOOK_OCR"
    const val AI_GENERATED = "AI_GENERATED"
    const val HYBRID = "HYBRID"
    const val USER_CREATED = "USER_CREATED"
    const val MISSING = "MISSING"
}

/**
 * 内容来源五级颜色标签组件（M3 Expressive 主题角色色版）。
 *
 * 颜色映射：
 * - TEXTBOOK_NATIVE / TEXTBOOK_OCR → secondaryContainer / onSecondaryContainer
 * - AI_GENERATED → tertiaryContainer / onTertiaryContainer
 * - HYBRID → surfaceContainerHighest / onSurfaceVariant
 * - USER_CREATED → surfaceContainerHigh / onSurfaceVariant
 * - MISSING → errorContainer / onErrorContainer
 *
 * 若 [stageLabel] 非空，则显示苏格拉底引导阶段标签（tertiaryContainer），
 * 优先级高于 [contentSource]。
 */
@Composable
fun ContentSourceBadge(
    contentSource: String?,
    modifier: Modifier = Modifier,
    stageLabel: String? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val config = when {
        stageLabel != null -> BadgeConfig(
            text = stageLabel,
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer,
            showWarning = false,
        )
        contentSource == ContentSource.TEXTBOOK_NATIVE -> BadgeConfig(
            text = "资料",
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer,
            showWarning = false,
        )
        contentSource == ContentSource.TEXTBOOK_OCR -> BadgeConfig(
            text = "资料·OCR",
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer,
            showWarning = false,
        )
        contentSource == ContentSource.AI_GENERATED -> BadgeConfig(
            text = "AI",
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer,
            showWarning = false,
        )
        contentSource == ContentSource.HYBRID -> BadgeConfig(
            text = "资料+AI",
            containerColor = colorScheme.surfaceContainerHighest,
            contentColor = colorScheme.onSurfaceVariant,
            showWarning = false,
        )
        contentSource == ContentSource.USER_CREATED -> BadgeConfig(
            text = "我的",
            containerColor = colorScheme.surfaceContainerHigh,
            contentColor = colorScheme.onSurfaceVariant,
            showWarning = false,
        )
        contentSource == ContentSource.MISSING -> BadgeConfig(
            text = "缺失",
            containerColor = colorScheme.errorContainer,
            contentColor = colorScheme.onErrorContainer,
            showWarning = true,
        )
        else -> return
    }

    Surface(
        color = config.containerColor,
        contentColor = config.contentColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (config.showWarning) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = config.text,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private data class BadgeConfig(
    val text: String,
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val showWarning: Boolean,
)
```

- [ ] **Step 2: 删除 Color.kt 中的旧颜色常量**

现在 `ContentSourceBadge` 不再引用 `SourceTextbook` 等旧颜色常量。回到 `Color.kt`，删除所有保留的旧颜色常量（`SourceTextbook`、`SourceAi`、`SourceHybrid`、`SourceUser`、`SourceMissing`）。

确保 `Color.kt` 最终只保留：

```kotlin
package com.wenyan.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** 默认种子色（Material 3 经典紫色） */
val DefaultSeedColor = Color(0xFF6750A4)
```

- [ ] **Step 3: 搜索并修复所有引用旧颜色的文件**

Run: `grep -rn "SourceTextbook\|SourceAi\|SourceHybrid\|SourceUser\|SourceMissing\|WenyanPrimary\|WenyanSecondary\|WenyanTertiary\|WenyanBackground\|WenyanSurface\|WenyanError" --include="*.kt" core/ feature/ app/`

对于每个引用：
- 将硬编码颜色引用替换为 `MaterialTheme.colorScheme.xxx` 对应角色
- 例如 `SourceTextbook` → `MaterialTheme.colorScheme.secondaryContainer`
- `WenyanPrimary` → `MaterialTheme.colorScheme.primary`

- [ ] **Step 4: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/ContentSourceBadge.kt
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/theme/Color.kt
git add -u  # 其他修改的文件
git commit -m "refactor: ContentSourceBadge uses theme color roles, remove all hardcoded colors"
```

---

## Phase 5: App 层接入

### Task 17: 改造 WenyanApp + MainActivity

**Files:**
- Modify: `app/src/main/java/com/wenyan/app/MainActivity.kt`
- Modify: `app/src/main/java/com/wenyan/app/WenyanApp.kt`

- [ ] **Step 1: 修改 MainActivity.kt**

将 `MainActivity.kt` 中的 `setContent` 块替换为：

```kotlin
        setContent {
            WenyanApp()
        }
```

移除 `import com.wenyan.app.core.designsystem.theme.WenyanTheme`（不再需要）。

完整文件：

```kotlin
package com.wenyan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WenyanApp()
        }
    }
}
```

- [ ] **Step 2: 重写 WenyanApp.kt**

```kotlin
package com.wenyan.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.theme.WenyanTheme
import com.wenyan.app.navigation.TopLevelDestination
import com.wenyan.app.navigation.WenyanNavHost

/**
 * 文研App 顶层 Composable。
 *
 * 接入 [ThemeViewModel] 获取主题配置，包裹 [WenyanTheme]。
 * 使用 [ExpressiveScaffold] 提供色调表面背景。
 */
@Composable
fun WenyanApp(
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

    WenyanTheme(config = themeConfig) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination

        ExpressiveScaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    TopLevelDestination.destinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTopLevelDestination(navController, destination.route) },
                            icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
                            label = { Text(text = destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIndicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
        ) { innerPadding ->
            WenyanNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

private fun navigateToTopLevelDestination(
    navController: androidx.navigation.NavHostController,
    route: String,
) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/wenyan/app/MainActivity.kt app/src/main/java/com/wenyan/app/WenyanApp.kt
git commit -m "feat: WenyanApp integrates ThemeViewModel + ExpressiveScaffold + WenyanTheme"
```

---

### Task 18: 改造 WenyanNavHost（新增 settings 路由）

**Files:**
- Modify: `app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`
- Modify: `app/build.gradle.kts` (添加 :feature:settings 依赖 — 在 Task 19 创建模块后)

- [ ] **Step 1: 修改 WenyanNavHost.kt**

在 `WenyanNavHost.kt` 中：

1. 在文件顶部添加 import：
```kotlin
import com.wenyan.app.feature.settings.SettingsScreen
```

2. 在 `aiAssistantDestination` 函数中添加 `onNavigateToSettings` 参数：

找到：
```kotlin
private fun NavGraphBuilder.aiAssistantDestination(
    onNavigateToApiConfig: () -> Unit,
) {
    composable(TopLevelDestination.ROUTE_AI_ASSISTANT) {
        AiAssistantScreen(onNavigateToApiConfig = onNavigateToApiConfig)
    }
}
```

替换为：
```kotlin
private fun NavGraphBuilder.aiAssistantDestination(
    onNavigateToApiConfig: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    composable(TopLevelDestination.ROUTE_AI_ASSISTANT) {
        AiAssistantScreen(
            onNavigateToApiConfig = onNavigateToApiConfig,
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}
```

3. 在 `WenyanNavHost` 函数体中，找到 `aiAssistantDestination(...)` 调用，替换为：

```kotlin
        aiAssistantDestination(
            onNavigateToApiConfig = {
                navController.navigate(ROUTE_API_CONFIG)
            },
            onNavigateToSettings = {
                navController.navigate(ROUTE_SETTINGS)
            },
        )
```

4. 添加 settingsDestination 和路由常量：

在 `apiConfigDestination` 函数之后添加：

```kotlin
private fun NavGraphBuilder.settingsDestination(
    onBack: () -> Unit,
    onNavigateToApiConfig: () -> Unit,
) {
    composable(ROUTE_SETTINGS) {
        SettingsScreen(
            onBack = onBack,
            onNavigateToApiConfig = onNavigateToApiConfig,
        )
    }
}
```

5. 在文件底部的路由常量中添加：

```kotlin
private const val ROUTE_SETTINGS = "settings"
```

6. 在 `WenyanNavHost` 函数体中，在 `apiConfigDestination(...)` 之后添加：

```kotlin
        settingsDestination(
            onBack = { navController.popBackStack() },
            onNavigateToApiConfig = {
                navController.navigate(ROUTE_API_CONFIG)
            },
        )
```

- [ ] **Step 2: 添加 :feature:settings 依赖到 app**

在 `app/build.gradle.kts` 的 `dependencies` 块中，找到 `implementation(project(":feature:aiassistant"))` 行，在其后添加：

```kotlin
    implementation(project(":feature:settings"))
```

注意：此步骤依赖 Task 19 先创建 `:feature:settings` 模块。如果 Task 19 未完成，先注释掉此行和 navHost 中的 settings 相关代码，等 Task 19 完成后取消注释。

- [ ] **Step 3: Commit（暂不编译，等 Task 19 完成后统一验证）**

```bash
git add app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt app/build.gradle.kts
git commit -m "feat: add settings route to WenyanNavHost"
```

---

## Phase 6: 新建 Settings 模块

### Task 19: 创建 feature:settings 模块

**Files:**
- Create: `feature/settings/build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: 在 settings.gradle.kts 注册模块**

在 `settings.gradle.kts` 中，找到 `include(":feature:aiassistant")` 行，在其后添加：

```kotlin
include(":feature:settings")
```

- [ ] **Step 2: 创建 feature/settings/build.gradle.kts**

```kotlin
// feature:settings 模块 —— 设置页面（主题模式/AMOLED/调色板风格/关于）
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.wenyan.app.feature.settings"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
```

- [ ] **Step 3: 创建 consumer-rules.pro（空文件）**

创建空文件 `feature/settings/consumer-rules.pro`。

- [ ] **Step 4: 创建 AndroidManifest.xml**

创建 `feature/settings/src/main/AndroidManifest.xml`：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 5: 验证模块可编译**

Run: `./gradlew :feature:settings:assembleDebug`
Expected: BUILD SUCCESSFUL（模块为空但可编译）

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts feature/settings/
git commit -m "build: create feature:settings module structure"
```

---

### Task 20: 实现 SettingsScreen

**Files:**
- Create: `feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`

- [ ] **Step 1: 创建 SettingsScreen.kt**

```kotlin
package com.wenyan.app.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.ThemeViewModel
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.SectionHeader
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanTopAppBar
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle

/**
 * 设置页面。
 *
 * 包含：外观（主题模式/AMOLED）、动态色彩（开关/种子色/调色板风格）、AI 服务、关于。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToApiConfig: () -> Unit,
    viewModel: ThemeViewModel = hiltViewModel(),
) {
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()

    ExpressiveScaffold(
        topBar = {
            WenyanTopAppBar(
                title = "设置",
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
        ) {
            // 外观
            item { SectionHeader(title = "外观") }

            // 主题模式选择
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = "主题模式",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        ColorMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeConfig.colorMode == mode,
                                onClick = { viewModel.setColorMode(mode) },
                                label = {
                                    Text(
                                        text = when (mode) {
                                            ColorMode.SYSTEM -> "跟随系统"
                                            ColorMode.LIGHT -> "浅色"
                                            ColorMode.DARK -> "深色"
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }

            // AMOLED 开关
            item {
                SwitchItem(
                    title = "AMOLED 纯黑模式",
                    description = "深色模式下使用纯黑背景，节省 OLED 电量",
                    checked = themeConfig.amoledMode,
                    onCheckedChange = { viewModel.setAmoledMode(it) },
                )
            }

            // 动态色彩
            item { SectionHeader(title = "动态色彩") }

            // 动态色彩开关
            item {
                SwitchItem(
                    title = "动态色彩",
                    description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "跟随系统壁纸自动生成色彩"
                    } else {
                        "需要 Android 12 及以上"
                    },
                    checked = themeConfig.dynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) },
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                )
            }

            // 种子色选择（动态色彩关闭时可用）
            if (!themeConfig.dynamicColor) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            text = "种子色",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            val seedColors = listOf(
                                Color(0xFF6750A4), // 紫
                                Color(0xFF0061A4), // 蓝
                                Color(0xFF006C4C), // 绿
                                Color(0xFF9C4146), // 红
                                Color(0xFF7C5800), // 棕
                            )
                            seedColors.forEach { color ->
                                FilterChip(
                                    selected = themeConfig.seedColor == color,
                                    onClick = { viewModel.setSeedColor(color) },
                                    label = {},
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = color,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 调色板风格
            if (!themeConfig.dynamicColor) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            text = "调色板风格",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            WenyanPaletteStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = themeConfig.paletteStyle == style,
                                    onClick = { viewModel.setPaletteStyle(style) },
                                    label = {
                                        Text(
                                            text = when (style) {
                                                WenyanPaletteStyle.TONAL_SPOT -> "Tonal Spot"
                                                WenyanPaletteStyle.NEUTRAL -> "Neutral"
                                                WenyanPaletteStyle.VIBRANT -> "Vibrant"
                                                WenyanPaletteStyle.EXPRESSIVE -> "Expressive"
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // AI 服务
            item { SectionHeader(title = "AI 服务") }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "API 配置",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = onNavigateToApiConfig) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "API 配置",
                        )
                    }
                }
            }

            // 关于
            item { SectionHeader(title = "关于") }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "版本",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "v0.1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew :feature:settings:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt
git commit -m "feat: implement SettingsScreen with theme/dynamic color/about sections"
```

---

## Phase 7: Screen 改造

### Task 21: 改造 KnowledgeScreen + KnowledgePointDetailScreen

**Files:**
- Modify: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt`
- Modify: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt`

- [ ] **Step 1: 读取现有 KnowledgeScreen.kt**

Run: `cat feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt`

理解现有结构后进行以下修改：
- 将 `Scaffold` 替换为 `ExpressiveScaffold`
- 将 `TopAppBar` 替换为 `LargeFlexibleTopAppBar`（M3 Expressive 大标题顶栏）
- 将 `AssistChip` 替换为 `FilterChip`
- 将知识点卡片替换为 `TonalCard`
- 移除私有 `EmptyState`，用共享 `EmptyState` 组件
- 卡片内 padding 统一使用 `Spacing.lg`

- [ ] **Step 2: 修改 KnowledgeScreen.kt**

在 import 区添加：

```kotlin
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCard
```

将 `Scaffold` 替换为 `ExpressiveScaffold`。
将 `TopAppBar` 替换为 `LargeFlexibleTopAppBar`（如果 API 可用；如果不可用，用 `WenyanTopAppBar`）。
将卡片 `Surface` 或 `Card` 替换为 `TonalCard`。
将私有 `EmptyState` 替换为共享 `EmptyState`。

注意：`LargeFlexibleTopAppBar` 是 M3 Expressive 组件，签名：

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFlexibleTopAppBar(
    title: { Text("知识点") },
    modifier = modifier,
)
```

如果 `LargeFlexibleTopAppBar` 不可用，退回使用 `WenyanTopAppBar`。

- [ ] **Step 3: 修改 KnowledgePointDetailScreen.kt**

在 import 区添加：

```kotlin
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanInfoChip
import com.wenyan.app.core.designsystem.component.WenyanTopAppBar
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.ChipVariant
```

- 将私有 `InfoChip` 替换为共享 `WenyanInfoChip`
- 将 `Scaffold` 替换为 `ExpressiveScaffold` + `WenyanTopAppBar`
- `PerspectiveCard` 中 `isOfficial` 用 `primaryContainer`，其他用 `surfaceContainerLow`
- `HorizontalDivider` 颜色改为 `MaterialTheme.colorScheme.outlineVariant`
- 关联卡片替换为 `TonalCard`

- [ ] **Step 4: 验证编译**

Run: `./gradlew :feature:knowledge:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add feature/knowledge/
git commit -m "feat: KnowledgeScreen uses LargeFlexibleTopAppBar + TonalCard; KnowledgePointDetail uses WenyanInfoChip"
```

---

### Task 22: 改造 QuizScreen

**Files:**
- Modify: `feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizScreen.kt`

- [ ] **Step 1: 修改 QuizScreen.kt**

在 import 区添加：

```kotlin
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanInfoChip
import com.wenyan.app.core.designsystem.component.WenyanTopAppBar
import com.wenyan.app.core.designsystem.component.ChipVariant
```

改造内容：
- `YearSelector` 的 `AssistChip` → `FilterChip`
- 移除私有 `InfoChip`，用共享 `WenyanInfoChip`（`ChipVariant.SECONDARY`）
- `QuestionCard` → `TonalCard`
- `AnswerSection` 的错误背景用 `MaterialTheme.colorScheme.errorContainer`
- `Scaffold` → `ExpressiveScaffold` + `WenyanTopAppBar`

- [ ] **Step 2: 验证编译**

Run: `./gradlew :feature:quiz:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add feature/quiz/
git commit -m "feat: QuizScreen uses WenyanInfoChip + TonalCard + FilterChip"
```

---

### Task 23: 改造 CardsScreen

**Files:**
- Modify: `feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt`

- [ ] **Step 1: 修改 CardsScreen.kt**

在 import 区添加：

```kotlin
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanTopAppBar
import com.wenyan.app.core.designsystem.component.Spacing
```

改造内容：
- 翻转卡片正面背景 → `MaterialTheme.colorScheme.surfaceContainerHigh`
- 翻转卡片背面背景 → `MaterialTheme.colorScheme.secondaryContainer`
- `CardRenderer` 的 `tertiaryContainer` 现在有定义（之前回退默认值）
- `SchoolComparisonCard` 的 `HorizontalDivider` → `outlineVariant`
- `Scaffold` → `ExpressiveScaffold` + `WenyanTopAppBar`

- [ ] **Step 2: 验证编译**

Run: `./gradlew :feature:cards:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add feature/cards/
git commit -m "feat: CardsScreen uses surfaceContainerHigh/secondaryContainer for card flip"
```

---

### Task 24: 改造 GraphScreen（GraphCanvas 走主题色）

**Files:**
- Modify: `feature/graph/src/main/java/com/wenyan/app/feature/graph/GraphScreen.kt`

- [ ] **Step 1: 修改 GraphScreen.kt**

在 import 区添加：

```kotlin
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.WenyanTopAppBar
import com.wenyan.app.core.designsystem.component.Spacing
```

改造内容：
- `LegendBar` 和 `StatsBar` 的 `Surface(surfaceVariant)` → `surfaceContainerLow`
- 移除 `LEGEND_*` 硬编码颜色常量
- `Scaffold` → `ExpressiveScaffold` + `WenyanTopAppBar`

- [ ] **Step 2: 重构 GraphCanvas 颜色**

在 `GraphCanvas` Composable 中，移除 `COLOR_GREEN/YELLOW/RED/GRAY` 硬编码常量，改为从主题获取：

```kotlin
@Composable
fun GraphCanvas(
    // ... 现有参数
) {
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(colorScheme) {
        GraphColors(
            mastered = colorScheme.primary,           // R≥0.8 已掌握
            consolidating = colorScheme.tertiary,      // 0.5≤R<0.8 需巩固
            weak = colorScheme.error,                  // 0<R<0.5 薄弱
            unlearned = colorScheme.outline,           // R≤0 未学习
            nodeLabel = colorScheme.onSurface,
            edge = colorScheme.outlineVariant,
            weakHalo = colorScheme.error.copy(alpha = 0.2f),
            weakBorder = colorScheme.error.copy(alpha = 0.6f),
        )
    }
    Canvas(...) {
        // 使用 colors.xxx 代替硬编码
    }
}

data class GraphColors(
    val mastered: Color,
    val consolidating: Color,
    val weak: Color,
    val unlearned: Color,
    val nodeLabel: Color,
    val edge: Color,
    val weakHalo: Color,
    val weakBorder: Color,
)
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :feature:graph:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add feature/graph/
git commit -m "feat: GraphCanvas uses theme color roles, remove all hardcoded colors"
```

---

### Task 25: 改造 AiAssistantScreen + ApiConfigScreen + MentorInfoScreen

**Files:**
- Modify: `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantScreen.kt`
- Modify: `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/ApiConfigScreen.kt`
- Modify: `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/MentorInfoScreen.kt`

- [ ] **Step 1: 修改 AiAssistantScreen.kt**

在 import 区添加：

```kotlin
import androidx.compose.material.icons.filled.Settings
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.WenyanTopAppBar
import com.wenyan.app.core.designsystem.component.Spacing
```

改造内容：
- TopAppBar actions：新增 settings 入口（设置图标，调用 `onNavigateToSettings`）
- 添加 `onNavigateToSettings: () -> Unit` 参数到 `AiAssistantScreen` 函数签名
- `MessageBubble` 用户消息背景 → `primaryContainer`，文字 → `onPrimaryContainer`
- AI 消息背景 → `surfaceContainerHigh`，文字 → `onSurface`
- 气泡圆角 → `MaterialTheme.shapes.large`
- `ReferencesList` 引用项 → `surfaceContainerLow` 背景
- 输入栏 `OutlinedTextField` 容器色 → `surfaceContainerLow`
- `Scaffold` → `ExpressiveScaffold` + `WenyanTopAppBar`

AiAssistantScreen 签名修改：

```kotlin
@Composable
fun AiAssistantScreen(
    onNavigateToApiConfig: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    // ...
    ExpressiveScaffold(
        topBar = {
            WenyanTopAppBar(
                title = "AI 助手",
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                        )
                    }
                },
            )
        },
    ) { padding ->
        // ...
    }
}
```

- [ ] **Step 2: 修改 ApiConfigScreen.kt**

改造内容：
- FAB 圆角 → `MaterialTheme.shapes.small`，色 → `primaryContainer`
- `ConfigCard` → `TonalCard`
- `ProviderChip` 改用 `FilterChip`
- `AlertDialog` 圆角 → `MaterialTheme.shapes.extraLarge`，背景 → `surfaceContainerHigh`
- `Scaffold` → `ExpressiveScaffold` + `WenyanTopAppBar`

- [ ] **Step 3: 修改 MentorInfoScreen.kt**

改造内容：
- 内容 `Column` padding → `Spacing.xl`
- "前往官网"按钮 → `FilledTonalButton`
- `Scaffold` → `ExpressiveScaffold` + `WenyanTopAppBar`

- [ ] **Step 4: 验证编译**

Run: `./gradlew :feature:aiassistant:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add feature/aiassistant/
git commit -m "feat: AiAssistantScreen adds settings entry; ApiConfig uses TonalCard; MentorInfo uses FilledTonalButton"
```

---

## Phase 8: 验证

### Task 26: 编译验证 + GitHub Actions 推送

**Files:**
- None (verification only)

- [ ] **Step 1: 全量编译验证**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行所有单元测试**

Run: `./gradlew testDebugUnitTest`
Expected: 全部通过（原有 61+ + 新增 12 = 73+ 测试）

- [ ] **Step 3: 推送到 GitHub**

```bash
git push origin main
```

- [ ] **Step 4: 验证 GitHub Actions CI 通过**

检查 GitHub Actions Run 是否全部步骤成功（assembleDebug + testDebugUnitTest）。

- [ ] **Step 5: 最终 Commit**

```bash
git add -A
git commit -m "chore: M3 Expressive redesign complete — all screens migrated"
git push origin main
```

---

## Self-Review Notes

### Spec Coverage Checklist

| Spec Section | Task | Status |
|---|---|---|
| 3.1 颜色系统（materialkolor 动态生成） | Task 7 | ✅ |
| 3.2 完整 ColorScheme 角色 | Task 7 (materialkolor 自动生成) | ✅ |
| 3.3 Typography 15 样式 | Task 5 | ✅ |
| 3.4 Shapes 5 级 | Task 6 | ✅ |
| 3.5 间距 Tokens | Task 3 | ✅ |
| 3.6 主题配置状态 | Task 2 | ✅ |
| 4.1.1 ExpressiveScaffold | Task 12 | ✅ |
| 4.1.2 TonalCard | Task 12 | ✅ |
| 4.1.3 WenyanTopAppBar | Task 13 | ✅ |
| 4.1.4 WenyanInfoChip | Task 14 | ✅ |
| 4.1.5 EmptyState | Task 15 | ✅ |
| 4.1.6 LoadingState | Task 15 | ✅ |
| 4.1.7 SectionHeader | Task 14 | ✅ |
| 4.1.8 ContentSourceBadge 重构 | Task 16 | ✅ |
| 4.1.9 Spacing | Task 3 | ✅ |
| 4.2 内容来源五级标注颜色映射 | Task 16 | ✅ |
| 5.1 WenyanApp | Task 17 | ✅ |
| 5.2 KnowledgeScreen | Task 21 | ✅ |
| 5.3 KnowledgePointDetailScreen | Task 21 | ✅ |
| 5.4 QuizScreen | Task 22 | ✅ |
| 5.5 CardsScreen | Task 23 | ✅ |
| 5.6 GraphScreen | Task 24 | ✅ |
| 5.7 AiAssistantScreen | Task 25 | ✅ |
| 5.8 ApiConfigScreen | Task 25 | ✅ |
| 5.9 MentorInfoScreen | Task 25 | ✅ |
| 5.10 SettingsScreen | Task 20 | ✅ |
| 6.1 主题状态流 | Task 17 | ✅ |
| 6.2 ThemeRepository | Task 8-9 | ✅ |
| 6.3 ThemeViewModel | Task 11 | ✅ |
| 6.4 WenyanApp 接入 | Task 17 | ✅ |
| 6.5 GraphCanvas 颜色数据流 | Task 24 | ✅ |
| 7.2 测试策略（单元测试） | Task 9, 11 | ✅ |
| 导航结构变更（settings 路由） | Task 18 | ✅ |

### Known Risks

1. **composeBom 2025.12.00 版本可能不存在**：如果该版本不存在，尝试 `2025.10.00` 或 `2025.11.00`。如果都不行，保持 BOM 不变，单独在 `core/designsystem/build.gradle.kts` 中指定 `implementation("androidx.compose.material3:material3:1.4.0")`。

2. **MaterialExpressiveTheme API 可能与计划中不同**：如果 `MaterialExpressiveTheme` 或 `MotionScheme.expressive()` 的签名不同，参考 AndroidX 官方文档调整 `WenyanTheme.kt`。

3. **LargeFlexibleTopAppBar 可能不可用**：如果该组件不在 material3 1.4.x 中，退回使用 `WenyanTopAppBar`。

4. **compileSdk 35 可能需要升级 AGP**：如果 AGP 8.5.2 不支持 compileSdk 35，升级 AGP 到 8.6+。

5. **Color.value 返回 ULong**：在 `ThemeRepositoryImpl` 中，`Color.value.toInt()` 可能在某些 Compose 版本中需要使用 `Color(color.value).toArgb()` 或 `color.toArgb()`。如果编译失败，改用 `androidx.compose.ui.graphics.toArgb()`。

### Type Consistency Check

- `ThemeConfig` 字段名在所有任务中一致：`colorMode`/`amoledMode`/`paletteStyle`/`dynamicColor`/`seedColor` ✅
- `ColorMode` 枚举值：`SYSTEM`/`LIGHT`/`DARK` ✅
- `WenyanPaletteStyle` 枚举值：`TONAL_SPOT`/`NEUTRAL`/`VIBRANT`/`EXPRESSIVE` ✅
- `ChipVariant` 枚举值：`NEUTRAL`/`PRIMARY`/`SECONDARY`/`TERTIARY`/`ERROR` ✅
- `ThemeRepository` 方法名：`setColorMode`/`setAmoledMode`/`setPaletteStyle`/`setDynamicColor`/`setSeedColor` ✅
- `ThemeViewModel` 方法名与 Repository 一致 ✅
- `ExpressiveScaffold` 参数名：`topBar`/`bottomBar`/`snackbarHost`/`floatingActionButton`/`content` ✅
- `WenyanTopAppBar` 参数名：`title`/`onBack`/`actions` ✅
- `ROUTE_SETTINGS = "settings"` 在 Task 18 和 Task 19 中一致 ✅
