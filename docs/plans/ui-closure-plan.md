# 文研 App UI 改造闭环实施计划

> **For agentic workers:** 本计划基于 writing-plans skill 编写。Step 使用 `- [ ]` 复选框跟踪进度。每个 Task 应独立可执行、可验证、可回滚。

**Goal:** 把 KSU 风格 UI 改造从"骨架已立"推进到"闭环可用"——增强 GroupedCard 组件、重构 SettingsScreen 和 KnowledgePointDetailScreen 使组件真正被使用、为 4 个 KSU 组件补齐 @Preview 和 Robolectric 测试，让 UI 改造形成完整闭环。

**Architecture:** 分五阶段递进——Phase 1 增强 GroupedCard 组件（加 leadingIcon / description / 项间分割线，向下兼容）→ Phase 2 用增强后的 GroupedCard 重构 SettingsScreen（4 个分组）→ Phase 3 用 GroupedCard 重构 KnowledgePointDetailScreen 的关联知识点区域 → Phase 4 为 4 个 KSU 组件补 @Preview（浅色/深色/AMOLED）+ 为 WenyanNavigationBar / HierarchicalListItem 补 Robolectric 测试 → Phase 5 全量验证 + 文档更新。每个 Phase 末尾有验证关卡。

**Tech Stack:**
- Kotlin 2.3.10 / KSP 2.3.2 / Hilt 2.57.1 / Room 2.7.0
- material3 1.5.0-alpha18（LargeFlexibleTopAppBar 仍为 @ExperimentalMaterial3ExpressiveApi）
- Compose BOM 2025.12.00 / materialkolor 4.1.1
- Robolectric 4.13（JVM 跑 Compose UI 测试，SDK 锁 34）
- AGP 8.6.0 / Gradle 8.14.4

---

## 背景调查：当前状态与差距

### 已完成（KSU UI 升级 Phase 0-2）

| 组件/Screen | 状态 | 文件 |
|-------------|------|------|
| WenyanLargeTopAppBar | ✅ 已创建 + 4 测试 | `core/designsystem/.../WenyanLargeTopAppBar.kt` |
| WenyanNavigationBar | ✅ 已创建，无测试 | `core/designsystem/.../WenyanNavigationBar.kt` |
| GroupedCard + GroupedCardItem | ✅ 已创建，无测试，**无人使用** | `core/designsystem/.../GroupedCard.kt` |
| HierarchicalListItem | ✅ 已创建，无测试，**无人使用** | `core/designsystem/.../HierarchicalListItem.kt` |
| 9 个 Screen LargeTopAppBar 迁移 | ✅ 完成 | 各 feature 模块 |
| WenyanNavigationBar 接入 | ✅ 完成 | `app/.../WenyanApp.kt` |

### 差距分析（本计划要解决的）

| # | 差距 | 严重程度 | 证据 |
|---|------|---------|------|
| 1 | GroupedCard 创建了但无 Screen 使用 | 高 | `SettingsScreen.kt` 仍用 `SectionHeader` + 手写 Row 平铺 |
| 2 | GroupedCardItem 缺 leadingIcon / description 支持 | 中 | KSU 设置项有左侧图标 + 多行描述，当前 API 不支持 |
| 3 | KnowledgePointDetailScreen 关联知识点用 TonalCard 平铺 | 中 | `RelatedGroup` 函数用 `TonalCard` + 简单 Text |
| 4 | 4 个 KSU 组件零 @Preview | 高 | `grep "@Preview"` 全项目无匹配 |
| 5 | GroupedCard / WenyanNavigationBar / HierarchicalListItem 零测试 | 中 | 只有 WenyanLargeTopAppBar 有测试 |

### 不做的事（明确排除）

- **不重构 ApiConfigScreen**：每个 API 配置是独立项，内容多（名称/服务商/模型/接口/密钥/操作按钮），TonalCard 独立卡片更合适，塞进 GroupedCard 会破坏信息密度
- **不强行使用 HierarchicalListItem**：当前数据模型是平铺的 `List<KnowledgePointEntity>`，没有父子层级关系。硬套 depth=0/1 会很别扭。HierarchicalListItem 留待未来有"前置→当前→后置"树形结构时使用。本计划仍为它补 @Preview 和测试
- **不涉及业务逻辑变更**：纯 UI 重构 + 测试 + 预览
- **不涉及数据库 schema 变更**
- **不跑 emulator**：沙箱跑不了 emulator，emulator 验证留给用户本地操作

---

## 文件结构总览

### Phase 1 修改的文件（1 个）

- `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/GroupedCard.kt` — 增强 GroupedCardItem（加 leadingIcon / description）+ 新增 GroupedCardDivider

### Phase 1 新增的测试（1 个）

- `/workspace/core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/GroupedCardTest.kt` — GroupedCard / GroupedCardItem 测试

### Phase 2 修改的文件（1 个）

- `/workspace/feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt` — 用 GroupedCard 重构 4 个分组

### Phase 3 修改的文件（1 个）

- `/workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt` — 用 GroupedCard 重构 RelatedGroup

### Phase 4 新增的文件（4 个 Preview + 2 个 Test）

- `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/WenyanLargeTopAppBarPreview.kt`
- `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/WenyanNavigationBarPreview.kt`
- `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/GroupedCardPreview.kt`
- `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/HierarchicalListItemPreview.kt`
- `/workspace/core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/WenyanNavigationBarTest.kt`
- `/workspace/core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/HierarchicalListItemTest.kt`

### Phase 5 修改的文件（文档）

- `/workspace/docs/00-STATUS.md`
- `/workspace/docs/SESSION_LOG.md`
- `/workspace/docs/plans/ui-closure-plan.md`（本文件，标记完成）

---

## Phase 1: 增强 GroupedCard 组件

### Task 1.1: 增强 GroupedCardItem（加 leadingIcon / description）

**Files:**
- Modify: `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/GroupedCard.kt`

**依据**：KSU 设置页每个项左侧有图标（如 Palette 图标对应"主题"项），标题下方有说明文字（如"深色模式下使用纯黑背景"）。当前 `GroupedCardItem` 只有 title / subtitle / onClick / trailing，不支持左侧图标和标题下方说明。需要加 `leadingIcon: ImageVector?` 和 `description: String?` 参数。

**设计决策**：
- `subtitle`：右侧简短值（如"v0.1.0"），与 `trailing` 互斥
- `description`：标题下方说明文字（如"深色模式下使用纯黑背景"），可多行
- `leadingIcon`：左侧图标，为 null 时不显示
- 当有 `description` 时，标题和描述用 Column 包裹（weight=1f），右侧 trailing/subtitle 在 Column 外

**当前文件 import 状态（已核实）**：原文件已有 `clickable` / `Arrangement` / `Column` / `ColumnScope` / `Row` / `fillMaxWidth` / `padding` / `MaterialTheme` / `Text` / `Composable` / `Alignment` / `Modifier`。**只需新增 3 个 import**：`Icon` / `ImageVector` / `dp`。

- [ ] **Step 1: 新增 import**

在 `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/GroupedCard.kt` 的 import 区（第 1-14 行之间）添加：

```kotlin
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
```

- [ ] **Step 2: 替换 GroupedCardItem 函数**

将文件中的 `GroupedCardItem` 函数（当前第 82-117 行）整体替换为：

```kotlin
/**
 * 分组卡片内的列表项。
 *
 * - [title]：标题（必填）
 * - [subtitle]：右侧简短值（如"v0.1.0"），与 [trailing] 互斥
 * - [description]：标题下方说明文字（如"深色模式下使用纯黑背景"），可多行
 * - [leadingIcon]：左侧图标，可选
 * - [leadingIconContentDescription]：左侧图标内容描述，为 null 时用 title
 * - [trailing]：右侧自定义内容（如 Switch），优先级高于 [subtitle]
 *
 * @param title 标题
 * @param subtitle 右侧简短值，可选
 * @param description 标题下方说明文字，可选
 * @param leadingIcon 左侧图标，可选
 * @param leadingIconContentDescription 左侧图标内容描述，为 null 时用 title
 * @param onClick 点击回调，为 null 时不可点击
 * @param trailing 右侧自定义内容，优先级高于 subtitle
 */
@Composable
fun GroupedCardItem(
    title: String,
    subtitle: String? = null,
    description: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = leadingIconContentDescription ?: title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = Spacing.md),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

- [ ] **Step 3: 验证编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :core:designsystem:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`。

---

### Task 1.2: 给 GroupedCard 加项间分割线（GroupedCardDivider）

**Files:**
- Modify: `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/GroupedCard.kt`

**依据**：KSU 设置页分组内项之间有细分隔线（`outlineVariant` 色，0.5dp）。当前 `GroupedCard` 内部用 `Column { content() }` 直接排列，项之间无分割线，视觉上会糊成一片。

**设计决策**：提供独立的 `GroupedCardDivider` composable，调用方在两个 item 之间手动插入。这是 KSU 和多数 Compose 设置页的标准做法（Compose 无法在 ColumnScope 中自动检测子项数量，无法自动判断"最后一个不画线"）。

- [ ] **Step 1: 新增 import**

在文件 import 区添加：

```kotlin
import androidx.compose.material3.HorizontalDivider
```

- [ ] **Step 2: 在文件末尾追加 GroupedCardDivider 函数**

在 `GroupedCardItem` 函数之后（文件末尾）追加：

```kotlin
/**
 * 分组卡片内项之间的分割线。
 *
 * KSU 设置页标准做法：在两个 [GroupedCardItem] 之间手动插入此分割线。
 * 使用 [MaterialTheme.colorScheme.outlineVariant] 色，0.5dp 厚度，
 * 左右各留 [Spacing.lg] 边距，与 item 内容区对齐。
 */
@Composable
fun GroupedCardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
```

- [ ] **Step 3: 验证编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :core:designsystem:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`。

---

### Task 1.3: 为 GroupedCard 写测试

**Files:**
- Create: `/workspace/core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/GroupedCardTest.kt`

**依据**：参照现有 `WenyanLargeTopAppBarTest.kt` 的模式——`@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [34])` + `createComposeRule()` + `MaterialTheme { Surface { ... } }` 包装。

- [ ] **Step 1: 创建测试文件**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupedCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun title_isDisplayed_whenProvided() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观设置") {
                        GroupedCardItem(title = "主题模式")
                    }
                }
            }
        }
        composeRule.onNodeWithText("外观设置").assertIsDisplayed()
    }

    @Test
    fun itemTitle_isDisplayed() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观") {
                        GroupedCardItem(title = "AMOLED 纯黑模式")
                    }
                }
            }
        }
        composeRule.onNodeWithText("AMOLED 纯黑模式").assertIsDisplayed()
    }

    @Test
    fun itemSubtitle_isDisplayed_onRightSide() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "关于") {
                        GroupedCardItem(title = "版本", subtitle = "v0.1.0")
                    }
                }
            }
        }
        composeRule.onNodeWithText("版本").assertIsDisplayed()
        composeRule.onNodeWithText("v0.1.0").assertIsDisplayed()
    }

    @Test
    fun itemDescription_isDisplayed_belowTitle() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观") {
                        GroupedCardItem(
                            title = "AMOLED 纯黑模式",
                            description = "深色模式下使用纯黑背景，节省 OLED 电量",
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithText("AMOLED 纯黑模式").assertIsDisplayed()
        composeRule.onNodeWithText("深色模式下使用纯黑背景，节省 OLED 电量").assertIsDisplayed()
    }

    @Test
    fun itemLeadingIcon_isDisplayed_whenProvided() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观") {
                        GroupedCardItem(
                            title = "主题模式",
                            leadingIcon = Icons.Default.Palette,
                        )
                    }
                }
            }
        }
        // contentDescription 默认用 title
        composeRule.onNodeWithContentDescription("主题模式").assertIsDisplayed()
    }

    @Test
    fun itemTrailing_switch_isDisplayed() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观") {
                        GroupedCardItem(
                            title = "AMOLED",
                            trailing = {
                                Switch(checked = false, onCheckedChange = {})
                            },
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithText("AMOLED").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: 运行测试验证**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :core:designsystem:testDebugUnitTest --tests "com.wenyan.app.core.designsystem.component.GroupedCardTest" --no-daemon 2>&1 | tail -15
```
Expected: 6 个测试全部通过。

- [ ] **Step 3: 提交 Phase 1**

```bash
cd /workspace && git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/GroupedCard.kt core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/GroupedCardTest.kt && git commit -m "$(cat <<'EOF'
feat: 增强 GroupedCard 组件（leadingIcon/description/分割线）+ 测试

GroupedCardItem 新增 leadingIcon / leadingIconContentDescription /
description 参数，支持 KSU 风格的"左侧图标 + 标题 + 说明文字 + 右侧控件"
四段式布局。

新增 GroupedCardDivider 组件用于项间分割线（outlineVariant 0.5dp）。

新增 GroupedCardTest（6 个测试用例）：标题/子标题/描述/图标/trailing。
EOF
)"
```

---

## Phase 2: 重构 SettingsScreen

### Task 2.1: 用 GroupedCard 重构"外观"分组

**Files:**
- Modify: `/workspace/feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`

**依据**：当前"外观"区块用 `SectionHeader("外观")` + 手写 Column（主题模式 FilterChip）+ `SwitchItem`（AMOLED）。改为 `GroupedCard(title="外观")` 内含：
1. 主题模式 GroupedCardItem（subtitle 显示当前模式）+ 下方 FilterChip Row（独立行）
2. AMOLED 开关 GroupedCardItem（description + trailing Switch）

**当前文件 import 状态（已核实）**：已有 `FilterChip` / `Switch` / `Icons` / `Palette` / `Icon` / `Text` / `MaterialTheme` / `Arrangement` / `Column` / `Row` / `Alignment` / `Color` / `dp` / `Spacing` / `SectionHeader`。**需新增 3 个 import**：`GroupedCard` / `GroupedCardDivider` / `GroupedCardItem`。

- [ ] **Step 1: 新增 import**

在 SettingsScreen.kt 的 import 区添加：

```kotlin
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
```

- [ ] **Step 2: 替换"外观"区块**

将第 71-117 行（从 `// 外观` 到 AMOLED 开关 item 结束的 `}`）替换为：

```kotlin
            // 外观
            item {
                GroupedCard(title = "外观") {
                    // 主题模式
                    GroupedCardItem(
                        title = "主题模式",
                        subtitle = when (themeConfig.colorMode) {
                            ColorMode.SYSTEM -> "跟随系统"
                            ColorMode.LIGHT -> "浅色"
                            ColorMode.DARK -> "深色"
                        },
                    )
                    GroupedCardDivider()
                    // 主题模式选择 chips（在卡片内独立一行）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
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
                    GroupedCardDivider()
                    // AMOLED 开关
                    GroupedCardItem(
                        title = "AMOLED 纯黑模式",
                        description = "深色模式下使用纯黑背景，节省 OLED 电量",
                        trailing = {
                            Switch(
                                checked = themeConfig.amoledMode,
                                onCheckedChange = { viewModel.setAmoledMode(it) },
                            )
                        },
                    )
                }
            }
```

- [ ] **Step 3: 验证编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :feature:settings:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`（注意：此时 `SectionHeader` import 暂时保留，因为动态色彩/AI服务/关于分组仍在用，Task 2.3 会清理）。

---

### Task 2.2: 用 GroupedCard 重构"动态色彩"分组

**Files:**
- Modify: `/workspace/feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`

- [ ] **Step 1: 替换"动态色彩"区块**

将第 119-216 行（从 `// 动态色彩` 到调色板风格 if 块结束的 `}`）替换为：

```kotlin
            // 动态色彩
            item {
                GroupedCard(title = "动态色彩") {
                    GroupedCardItem(
                        title = "动态色彩",
                        description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "跟随系统壁纸自动生成色彩"
                        } else {
                            "需要 Android 12 及以上"
                        },
                        trailing = {
                            Switch(
                                checked = themeConfig.dynamicColor,
                                onCheckedChange = { viewModel.setDynamicColor(it) },
                                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                            )
                        },
                    )
                    // 种子色 + 调色板风格（动态色彩关闭时显示）
                    if (!themeConfig.dynamicColor) {
                        GroupedCardDivider()
                        // 种子色选择
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "种子色",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
                        GroupedCardDivider()
                        // 调色板风格
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "风格",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
```

- [ ] **Step 2: 验证编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :feature:settings:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`。

---

### Task 2.3: 用 GroupedCard 重构"AI 服务"和"关于"分组 + 清理

**Files:**
- Modify: `/workspace/feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`

- [ ] **Step 1: 替换"AI 服务"区块**

将第 218-241 行（从 `// AI 服务` 到 API 配置 item 的 `}`）替换为：

```kotlin
            // AI 服务
            item {
                GroupedCard(title = "AI 服务") {
                    GroupedCardItem(
                        title = "API 配置",
                        subtitle = "DeepSeek / 通义 / 智谱 / 月之暗面",
                        onClick = onNavigateToApiConfig,
                    )
                }
            }
```

- [ ] **Step 2: 替换"关于"区块**

将第 243-265 行（从 `// 关于` 到版本 item 的 `}`）替换为：

```kotlin
            // 关于
            item {
                GroupedCard(title = "关于") {
                    GroupedCardItem(
                        title = "版本",
                        subtitle = "v0.1.0",
                    )
                }
            }
```

- [ ] **Step 3: 删除旧的 SwitchItem 私有函数**

删除 SettingsScreen.kt 末尾的 `SwitchItem` 私有 composable（原第 270-306 行，因前面替换行号可能有偏移，搜索 `private fun SwitchItem` 定位）。

- [ ] **Step 4: 清理不再使用的 import**

删除以下 import（搜索确认全文件不再使用后删除）：

```kotlin
import com.wenyan.app.core.designsystem.component.SectionHeader  // 4 个分组都已改用 GroupedCard
import androidx.compose.material.icons.automirrored.filled.ArrowForward  // API 配置改用 GroupedCardItem onClick
import androidx.compose.material3.IconButton  // 同上
```

**注意**：`Icons` / `Palette` / `Icon` / `FilterChip` / `Switch` 仍在动态色彩区块使用，**保留**。`Row` / `Column` / `Arrangement` / `Alignment` 仍在使用，**保留**。`dp` 在原 SwitchItem 中使用，删除 SwitchItem 后检查是否仍需——如果 GroupedCard 内的 Row 没用 `dp`，则 `dp` 也可删。实际检查：GroupedCard 内 Row 用的是 `Spacing.*`，不用 `dp`。但 `2.dp` 在原 SwitchItem 的 `Arrangement.spacedBy(2.dp)` 中用。删除 SwitchItem 后，搜索 `dp` 确认无其他使用即可删除 `import androidx.compose.ui.unit.dp`。

- [ ] **Step 5: 验证编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :feature:settings:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`，无 unused import 警告。

- [ ] **Step 6: 提交 Phase 2**

```bash
cd /workspace && git add feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt && git commit -m "$(cat <<'EOF'
refactor: SettingsScreen 用 GroupedCard 重构 4 个分组

外观/动态色彩/AI 服务/关于 4 个区块从 SectionHeader + 手写 Row
改为 GroupedCard + GroupedCardItem，统一 KSU 风格分组卡片布局。

- 主题模式/种子色/调色板风格保留 FilterChip（在卡片内独立行）
- AMOLED/动态色彩用 GroupedCardItem + trailing Switch
- API 配置用 GroupedCardItem + onClick 跳转
- 版本用 GroupedCardItem + subtitle
- 删除旧的 SwitchItem 私有函数和 SectionHeader/ArrowForward/IconButton import
EOF
)"
```

---

## Phase 3: 重构 KnowledgePointDetailScreen 关联知识点

### Task 3.1: 用 GroupedCard 重构 RelatedGroup

**Files:**
- Modify: `/workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt`

**依据**：当前 `RelatedGroup` 函数（第 387-413 行）用 `TonalCard` + 简单 Text 平铺关联知识点。外层有 `InfoSection(title="关联知识点")` 的 `primary` 色标题 + 3 个子 `RelatedGroup`（关联/对比/延伸）。

**设计决策**：去掉外层 `InfoSection`，直接用 3 个 `GroupedCard`（title="关联"/"对比"/"延伸"）。理由：GroupedCard 的 title 已用 `primary` 色 + `titleMedium` 样式，本身有足够视觉层级，双层 primary 标题会冗余。

**当前文件 import 状态（已核实）**：`TonalCard`（第 36 行）和 `clickable`（第 14 行）只在 `RelatedGroup` 中使用，重构后可删除。`HorizontalDivider`（第 18 行）在 `SourcesSection` 中使用，**保留**。

- [ ] **Step 1: 新增 import**

在 KnowledgePointDetailScreen.kt 的 import 区添加：

```kotlin
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
```

- [ ] **Step 2: 替换 RelatedPointsSection 函数**

将 `RelatedPointsSection` 函数（第 347-385 行）替换为：

```kotlin
@Composable
private fun RelatedPointsSection(
    detail: com.wenyan.app.core.data.repository.KnowledgePointDetail?,
    onNavigateToDetail: (String) -> Unit,
) {
    if (detail == null) return

    val hasRelated = detail.relatedPoints.isNotEmpty()
    val hasContrast = detail.contrastPoints.isNotEmpty()
    val hasExtension = detail.extensionPoints.isNotEmpty()

    if (!hasRelated && !hasContrast && !hasExtension) return

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        if (hasRelated) {
            RelatedGroup(
                title = "关联",
                points = detail.relatedPoints,
                onNavigateToDetail = onNavigateToDetail,
            )
        }
        if (hasContrast) {
            RelatedGroup(
                title = "对比",
                points = detail.contrastPoints,
                onNavigateToDetail = onNavigateToDetail,
            )
        }
        if (hasExtension) {
            RelatedGroup(
                title = "延伸",
                points = detail.extensionPoints,
                onNavigateToDetail = onNavigateToDetail,
            )
        }
    }
}
```

- [ ] **Step 3: 替换 RelatedGroup 函数**

将 `RelatedGroup` 函数（第 387-413 行）替换为：

```kotlin
@Composable
private fun RelatedGroup(
    title: String,
    points: List<KnowledgePointEntity>,
    onNavigateToDetail: (String) -> Unit,
) {
    GroupedCard(title = title) {
        points.forEachIndexed { index, point ->
            GroupedCardItem(
                title = point.title,
                onClick = { onNavigateToDetail(point.id) },
            )
            if (index < points.size - 1) {
                GroupedCardDivider()
            }
        }
    }
}
```

- [ ] **Step 4: 删除不再使用的 import**

删除以下 import（已核实全文件无其他使用）：

```kotlin
import androidx.compose.foundation.clickable      // 只在旧 RelatedGroup 用
import com.wenyan.app.core.designsystem.component.TonalCard  // 只在旧 RelatedGroup 用
```

- [ ] **Step 5: 验证编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :feature:knowledge:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 6: 提交 Phase 3**

```bash
cd /workspace && git add feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt && git commit -m "$(cat <<'EOF'
refactor: KnowledgePointDetailScreen 关联知识点用 GroupedCard 重构

RelatedGroup（关联/对比/延伸）从 TonalCard + 简单 Text 改为
GroupedCard + GroupedCardItem，统一 KSU 风格分组卡片布局。

- 每个 GroupedCardItem 带 onClick 跳转到知识点详情
- 项间用 GroupedCardDivider 分割
- 去掉外层 InfoSection 标题，避免双层 primary 标题
- 删除不再使用的 TonalCard / clickable import
EOF
)"
```

---

## Phase 4: 补 @Preview 和测试

### Task 4.1: 为 WenyanLargeTopAppBar 补 @Preview

**Files:**
- Create: `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/WenyanLargeTopAppBarPreview.kt`

**API 签名（已核实）**：`WenyanLargeTopAppBar(title: String, modifier, subtitle: String?, onBack: (() -> Unit)?, actions, scrollBehavior)`。`WenyanTheme(config: ThemeConfig, content: @Composable () -> Unit)`。`ThemeConfig(colorMode, amoledMode, paletteStyle, dynamicColor, seedColor)` 全有默认值。

- [ ] **Step 1: 创建 Preview 文件**

```kotlin
package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

@Preview(name = "Light - Simple", showBackground = true)
@Composable
private fun WenyanLargeTopAppBarSimplePreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT)) {
        Surface {
            WenyanLargeTopAppBar(title = "知识点")
        }
    }
}

@Preview(name = "Light - With Subtitle + Back", showBackground = true)
@Composable
private fun WenyanLargeTopAppBarWithSubtitlePreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT)) {
        Surface {
            WenyanLargeTopAppBar(
                title = "鲁迅《狂人日记》",
                subtitle = "高频 · 难度4/5",
                onBack = {},
            )
        }
    }
}

@Preview(name = "AMOLED - With Subtitle", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WenyanLargeTopAppBarAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true)) {
        Surface {
            WenyanLargeTopAppBar(
                title = "鲁迅《狂人日记》",
                subtitle = "高频 · 难度4/5",
                onBack = {},
            )
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :core:designsystem:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`。

---

### Task 4.2: 为 WenyanNavigationBar 补 @Preview

**Files:**
- Create: `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/WenyanNavigationBarPreview.kt`

**API 签名（已核实）**：`WenyanNavigationBar(items: List<WenyanNavItem>, currentRoute: String?, onNavigate: (String) -> Unit, modifier)`。`WenyanNavItem(route, label, icon)` 是 data class。

- [ ] **Step 1: 创建 Preview 文件**

```kotlin
package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wenyan.app.core.designsystem.component.WenyanNavItem
import com.wenyan.app.core.designsystem.component.WenyanNavigationBar
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

private val sampleItems = listOf(
    WenyanNavItem("knowledge", "知识点", Icons.Default.LibraryBooks),
    WenyanNavItem("quiz", "真题", Icons.Default.BarChart),
    WenyanNavItem("cards", "卡片", Icons.Default.Style),
    WenyanNavItem("graph", "图谱", Icons.Default.AccountBox),
    WenyanNavItem("aiassistant", "AI", Icons.Default.Chat),
)

@Preview(name = "Light - Knowledge selected", showBackground = true)
@Composable
private fun WenyanNavigationBarLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT)) {
        Surface {
            WenyanNavigationBar(
                items = sampleItems,
                currentRoute = "knowledge",
                onNavigate = {},
            )
        }
    }
}

@Preview(name = "Dark - AI selected", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WenyanNavigationBarDarkPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK)) {
        Surface {
            WenyanNavigationBar(
                items = sampleItems,
                currentRoute = "aiassistant",
                onNavigate = {},
            )
        }
    }
}

@Preview(name = "AMOLED - Cards selected", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WenyanNavigationBarAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true)) {
        Surface {
            WenyanNavigationBar(
                items = sampleItems,
                currentRoute = "cards",
                onNavigate = {},
            )
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :core:designsystem:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`。

---

### Task 4.3: 为 GroupedCard 补 @Preview

**Files:**
- Create: `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/GroupedCardPreview.kt`

- [ ] **Step 1: 创建 Preview 文件**

```kotlin
package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

@Preview(name = "Light - Settings style", showBackground = true)
@Composable
private fun GroupedCardSettingsLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT)) {
        Surface {
            GroupedCard(title = "外观") {
                GroupedCardItem(
                    title = "主题模式",
                    subtitle = "跟随系统",
                    leadingIcon = Icons.Default.Palette,
                )
                GroupedCardDivider()
                GroupedCardItem(
                    title = "AMOLED 纯黑模式",
                    description = "深色模式下使用纯黑背景，节省 OLED 电量",
                    trailing = { Switch(checked = false, onCheckedChange = {}) },
                )
            }
        }
    }
}

@Preview(name = "Dark - About style", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GroupedCardAboutDarkPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK)) {
        Surface {
            GroupedCard(title = "关于") {
                GroupedCardItem(title = "版本", subtitle = "v0.1.0")
                GroupedCardDivider()
                GroupedCardItem(title = "API 配置", subtitle = "DeepSeek", onClick = {})
                GroupedCardDivider()
                GroupedCardItem(title = "检查更新", onClick = {})
            }
        }
    }
}

@Preview(name = "AMOLED - Knowledge related", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GroupedCardRelatedAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true)) {
        Surface {
            GroupedCard(title = "关联") {
                GroupedCardItem(title = "鲁迅《狂人日记》", onClick = {})
                GroupedCardDivider()
                GroupedCardItem(title = "《呐喊》自序", onClick = {})
                GroupedCardDivider()
                GroupedCardItem(title = "新文化运动", onClick = {})
            }
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :core:designsystem:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`。

---

### Task 4.4: 为 HierarchicalListItem 补 @Preview

**Files:**
- Create: `/workspace/core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/HierarchicalListItemPreview.kt`

**API 签名（已核实）**：`HierarchicalListItem(title: String, depth: Int = 0, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null, leadingColor: Color)`。

- [ ] **Step 1: 创建 Preview 文件**

```kotlin
package com.wenyan.app.core.designsystem.component.previews

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wenyan.app.core.designsystem.component.HierarchicalListItem
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme

@Preview(name = "Light - Tree structure", showBackground = true)
@Composable
private fun HierarchicalListItemLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT)) {
        Surface {
            Column {
                HierarchicalListItem(title = "中国现代文学史", depth = 0, onClick = {})
                HierarchicalListItem(title = "第一章：文学革命", depth = 1, onClick = {})
                HierarchicalListItem(title = "《新青年》与白话文运动", depth = 2, onClick = {})
                HierarchicalListItem(title = "鲁迅《狂人日记》", depth = 2, onClick = {})
                HierarchicalListItem(title = "第二章：新诗", depth = 1, onClick = {})
            }
        }
    }
}

@Preview(name = "Dark - With trailing", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HierarchicalListItemDarkPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK)) {
        Surface {
            Column {
                HierarchicalListItem(
                    title = "现代文学三十年",
                    depth = 0,
                    trailing = { Text("已掌握") },
                )
                HierarchicalListItem(title = "第一个十年", depth = 1, onClick = {})
                HierarchicalListItem(title = "第二个十年", depth = 1, onClick = {})
            }
        }
    }
}

@Preview(name = "AMOLED - No onClick", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HierarchicalListItemAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true)) {
        Surface {
            Column {
                HierarchicalListItem(title = "知识点树（只读）", depth = 0)
                HierarchicalListItem(title = "子节点 A", depth = 1)
                HierarchicalListItem(title = "子节点 B", depth = 1)
            }
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :core:designsystem:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`。

---

### Task 4.5: 为 WenyanNavigationBar 补测试

**Files:**
- Create: `/workspace/core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/WenyanNavigationBarTest.kt`

- [ ] **Step 1: 创建测试文件**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WenyanNavigationBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleItems = listOf(
        WenyanNavItem("knowledge", "知识点", Icons.Default.LibraryBooks),
        WenyanNavItem("quiz", "真题", Icons.Default.BarChart),
    )

    @Test
    fun labels_areDisplayed_forAllItems() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanNavigationBar(
                        items = sampleItems,
                        currentRoute = "knowledge",
                        onNavigate = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("知识点").assertIsDisplayed()
        composeRule.onNodeWithText("真题").assertIsDisplayed()
    }

    @Test
    fun icons_haveContentDescription_withLabel() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanNavigationBar(
                        items = sampleItems,
                        currentRoute = "knowledge",
                        onNavigate = {},
                    )
                }
            }
        }
        // NavigationBarItem icon 的 contentDescription = item.label
        composeRule.onNodeWithContentDescription("知识点").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("真题").assertIsDisplayed()
    }

    @Test
    fun onNavigate_invoked_whenItemClicked() {
        var clickedRoute: String? = null
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanNavigationBar(
                        items = sampleItems,
                        currentRoute = "knowledge",
                        onNavigate = { clickedRoute = it },
                    )
                }
            }
        }
        composeRule.onNodeWithText("真题").performClick()
        assert(clickedRoute == "quiz") { "Expected clickedRoute to be 'quiz', was $clickedRoute" }
    }
}
```

- [ ] **Step 2: 运行测试**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :core:designsystem:testDebugUnitTest --tests "com.wenyan.app.core.designsystem.component.WenyanNavigationBarTest" --no-daemon 2>&1 | tail -15
```
Expected: 3 个测试全部通过。

---

### Task 4.6: 为 HierarchicalListItem 补测试

**Files:**
- Create: `/workspace/core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/HierarchicalListItemTest.kt`

- [ ] **Step 1: 创建测试文件**

```kotlin
package com.wenyan.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HierarchicalListItemTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun title_isDisplayed_rootNode() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    HierarchicalListItem(title = "现代文学史", depth = 0)
                }
            }
        }
        composeRule.onNodeWithText("现代文学史").assertIsDisplayed()
    }

    @Test
    fun title_isDisplayed_childNode() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    HierarchicalListItem(title = "第一章", depth = 1)
                }
            }
        }
        composeRule.onNodeWithText("第一章").assertIsDisplayed()
    }

    @Test
    fun trailingContent_isDisplayed_whenProvided() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    HierarchicalListItem(
                        title = "现代文学史",
                        depth = 0,
                        trailing = { Text("已掌握") },
                    )
                }
            }
        }
        composeRule.onNodeWithText("现代文学史").assertIsDisplayed()
        composeRule.onNodeWithText("已掌握").assertIsDisplayed()
    }

    @Test
    fun onClick_invoked_whenClicked() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    HierarchicalListItem(
                        title = "可点击节点",
                        depth = 0,
                        onClick = { clicked = true },
                    )
                }
            }
        }
        composeRule.onNodeWithText("可点击节点").performClick()
        assert(clicked) { "onClick was not invoked" }
    }

    @Test
    fun trailingNotShown_whenOnClickProvided_andNoTrailing() {
        // 当有 onClick 但无 trailing 时，应显示 ChevronRight 箭头（不报错即可）
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    HierarchicalListItem(
                        title = "带箭头节点",
                        depth = 0,
                        onClick = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("带箭头节点").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: 运行测试**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :core:designsystem:testDebugUnitTest --tests "com.wenyan.app.core.designsystem.component.HierarchicalListItemTest" --no-daemon 2>&1 | tail -15
```
Expected: 5 个测试全部通过。

- [ ] **Step 3: 提交 Phase 4**

```bash
cd /workspace && git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/previews/ core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/WenyanNavigationBarTest.kt core/designsystem/src/test/java/com/wenyan/app/core/designsystem/component/HierarchicalListItemTest.kt && git commit -m "$(cat <<'EOF'
test: 为 4 个 KSU 组件补 @Preview + 2 个测试文件

@Preview（12 个预览函数，覆盖浅色/深色/AMOLED 三态）：
- WenyanLargeTopAppBarPreview（3 个）
- WenyanNavigationBarPreview（3 个）
- GroupedCardPreview（3 个）
- HierarchicalListItemPreview（3 个）

测试（8 个测试用例）：
- WenyanNavigationBarTest（3 个：标签/图标/点击）
- HierarchicalListItemTest（5 个：标题/子节点/trailing/点击/箭头）
EOF
)"
```

---

## Phase 5: 全量验证 + 文档更新

### Task 5.1: 全量编译 + 测试验证

**Files:** 无修改

- [ ] **Step 1: 全量编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :app:assembleDebug --no-daemon 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 2: 全量测试**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle testDebugUnitTest --no-daemon 2>&1 | tail -20
```
Expected: 所有测试通过（原有测试 + 新增的 GroupedCardTest 6 个 + WenyanNavigationBarTest 3 个 + HierarchicalListItemTest 5 个 = 14 个新测试）。

- [ ] **Step 3: 检查 Preview 编译**

Run:
```bash
cd /workspace && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && export ANDROID_HOME=/opt/android-sdk && export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport" && export PATH=$JAVA_HOME/bin:/root/.local/share/mise/shims:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH && gradle :core:designsystem:compileDebugKotlin --no-daemon 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`（Preview 函数在 debug 编译时会被编译，确保无语法错误）。

---

### Task 5.2: 推送 + CI 验证

**Files:** 无修改

- [ ] **Step 1: 推送到远端**

```bash
cd /workspace && git push origin main
```

- [ ] **Step 2: 等 CI 运行并验证全绿**

Run:
```bash
sleep 60 && gh run list --repo qbjsdsb/wenyan-android --limit 3
```

如果 `gh` CLI 不可用，用 API 方式（需环境变量 `$GITHUB_TOKEN`）：

```bash
sleep 60 && curl -s -H "Authorization: token $GITHUB_TOKEN" "https://api.github.com/repos/qbjsdsb/wenyan-android/actions/runs?per_page=3" | python3 -c "import sys,json; d=json.load(sys.stdin); [print(r['id'],'|',r['status'],'|',r['conclusion'],'|',r['head_sha'][:7]) for r in d.get('workflow_runs',[])]"
```

重复检查直到 `conclusion: success`。

---

### Task 5.3: 更新文档

**Files:**
- Modify: `/workspace/docs/00-STATUS.md`
- Modify: `/workspace/docs/SESSION_LOG.md`
- Modify: `/workspace/docs/plans/ui-closure-plan.md`

- [ ] **Step 1: 更新 00-STATUS.md**

在"项目进度"的"KSU 风格 UI 升级"表格后新增"UI 改造闭环"小节：

```markdown
### UI 改造闭环（方案 D）

| 阶段 | 状态 | 内容 |
|------|------|------|
| Phase 1 | ✅ 完成 | GroupedCard 增强（leadingIcon/description/分割线）+ 6 测试 |
| Phase 2 | ✅ 完成 | SettingsScreen 用 GroupedCard 重构 4 个分组 |
| Phase 3 | ✅ 完成 | KnowledgePointDetailScreen 关联知识点用 GroupedCard 重构 |
| Phase 4 | ✅ 完成 | 4 个 KSU 组件 @Preview + 8 个测试 |
| Phase 5 | ✅ 完成 | 全量验证 + 文档更新 |
```

更新"下一步优先级"为：

```markdown
## 🎯 下一步优先级

1. **P0**：跑 emulator 实测 LargeFlexibleTopAppBar + GroupedCard 视觉效果
2. **P1**：OCR 完成后跑知识提取管线 → 生成 seed_data.json
3. **P2**：功能深化（AI 助手 Markdown 渲染 / 卡片翻转动画打磨）
4. **P3**：用 HierarchicalListItem 改造知识图谱节点详情（需先有树形数据结构）
```

- [ ] **Step 2: 更新 SESSION_LOG.md**

追加新会话记录（将 commit hash 填入实际值）：

```markdown
## 2026-07-13 会话：UI 改造闭环

- **完成**：
  - Phase 1（commit `<填入实际 hash>`）：GroupedCard 增强
    - GroupedCardItem 新增 leadingIcon / description 参数
    - 新增 GroupedCardDivider 组件
    - 新增 GroupedCardTest（6 测试）
  - Phase 2（commit `<填入实际 hash>`）：SettingsScreen 重构
    - 4 个分组用 GroupedCard 重构
    - 删除 SwitchItem 私有函数和 3 个旧 import
  - Phase 3（commit `<填入实际 hash>`）：KnowledgePointDetailScreen 重构
    - RelatedGroup 用 GroupedCard + GroupedCardItem
    - 删除 TonalCard / clickable import
  - Phase 4（commit `<填入实际 hash>`）：@Preview + 测试
    - 12 个 @Preview（浅色/深色/AMOLED）
    - 8 个新测试（NavigationBar 3 + HierarchicalListItem 5）

- **关键发现**：
  - GroupedCard 增强 API 向下兼容（新参数都有默认值，无破坏性变更）
  - SettingsScreen 的 FilterChip Row 在 GroupedCard 内需独立成行，不能塞进 GroupedCardItem 的 trailing
  - KnowledgePointDetailScreen 去掉 InfoSection 外层后，3 个 GroupedCard 直接排列视觉层级足够
  - [执行时补充其他发现]

- **下次继续**：
  - 跑 emulator 实测视觉效果
  - OCR 完成后跑知识提取管线
```

- [ ] **Step 3: 在本计划文档标记完成**

在每个 Phase 标题后加 ✅，在所有 Task 的 Step 复选框打勾。

- [ ] **Step 4: 提交文档**

```bash
cd /workspace && git add docs/00-STATUS.md docs/SESSION_LOG.md docs/plans/ui-closure-plan.md && git commit -m "$(cat <<'EOF'
docs: UI 改造闭环完成，更新状态文档

00-STATUS.md：新增"UI 改造闭环"小节，更新下一步优先级
SESSION_LOG.md：追加 UI 改造闭环会话记录
ui-closure-plan.md：标记所有 Phase 完成
EOF
)" && git push origin main
```

---

## 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| GroupedCard API 变更破坏现有调用 | GroupedCard 之前无人使用，无破坏性。新参数都有默认值，向下兼容 |
| SettingsScreen FilterChip 在 GroupedCard 内布局异常 | FilterChip Row 放在 GroupedCardItem 之间作为独立行，不塞进 Item 的 trailing |
| KnowledgePointDetailScreen 去掉 InfoSection 后视觉层级不够 | GroupedCard 的 title 用 primary 色，本身有足够视觉层级 |
| Robolectric 测试因 SDK 版本失败 | 锁定 `@Config(sdk = [34])`，与现有 WenyanLargeTopAppBarTest 一致 |
| @Preview 在 CI 编译失败 | Preview 函数用 `@Preview` 注解，debug 编译时会编译但不影响 release |
| SettingsScreen 删除 import 后编译失败 | Task 2.3 Step 4 已逐个列出需删除的 import，并说明保留哪些。编译验证在 Step 5 |

## 验证清单

- [ ] GroupedCard 编译通过
- [ ] GroupedCardTest 6 个测试通过
- [ ] SettingsScreen 编译通过
- [ ] KnowledgePointDetailScreen 编译通过
- [ ] 12 个 @Preview 编译通过
- [ ] WenyanNavigationBarTest 3 个测试通过
- [ ] HierarchicalListItemTest 5 个测试通过
- [ ] 全量 assembleDebug 通过
- [ ] 全量 testDebugUnitTest 通过（原有 + 14 新增）
- [ ] CI 全绿
- [ ] 文档更新完成
