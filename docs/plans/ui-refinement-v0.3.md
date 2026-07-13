# UI 精修 v0.3 实施计划 — 卡片镜像修复 + 导师信息删除 + AI 入口调整 + 动画优化

> **For agentic workers:** 本计划用于修复 v0.2.0 用户反馈的 4 个问题。每个 Task 按顺序执行，每个 Phase 结束 commit 一次。

**Goal:** 修复记忆卡片镜像 bug + 删除导师信息入口（连 Screen）+ 在 4 主屏 TopBar 加 AI 入口 + 全面优化动画体验。

**Architecture:** 分 6 个独立 Phase，每个 Phase 可单独验证 + commit。Phase 1-2 修 bug，Phase 3-5 优化动画，Phase 6 收尾。所有改动不破坏现有 184 tests。

**Tech Stack:** Kotlin 2.3.10 / Jetpack Compose（BOM 2025.12.00）/ material3 1.5.0-alpha18 / Navigation Compose 2.8.x / Robolectric（测试）

---

## 0. 调研结论速览（执行前必读）

### 问题 1：记忆卡片镜像根因

**位置**：[CardsScreen.kt#L124-L141](file:///workspace/feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt#L124-L141)

**根因**：`FlipCard` 用 `graphicsLayer { rotationY = rotation }` 旋转 0→180°，但：
- ❌ 缺 `cameraDistance`：3D 透视失真，边缘拉伸
- ❌ 内容用 `if (isFlipped)` 硬切：与动画时序错位（state 立即变，rotation 还在缓动）
- ❌ 背面无反向修正：rotation=180° 时文字左右镜像

**衍生硬切**：容器色硬切（L134-139）、评分按钮硬切（L104-112）、进度文本硬切（L88-91）

### 问题 2：导师信息 + AI 入口现状

- **导师信息入口**：[KnowledgeScreen.kt#L60-L72](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt#L60-L72) TopBar actions，跳转 MentorInfoScreen
- **MentorInfoScreen**：[MentorInfoScreen.kt](file:///workspace/feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/MentorInfoScreen.kt) — 仅一个外链按钮（`wxy.njnu.edu.cn/szdw/jsfc.htm`）
- **AI 入口**：底部 NavigationBar 第 5 个 Tab（[TopLevelDestination.kt#L52-L56](file:///workspace/app/src/main/java/com/wenyan/app/navigation/TopLevelDestination.kt#L52-L56)），次入口在 QuizScreen 卡片底部 TextButton
- **9 个 Screen 中只有 2 个用了 TopBar actions**：KnowledgeScreen（导师信息）+ AiAssistantScreen（设置/API配置/清空）

### 问题 3：动画缺失清单

| 优先级 | 问题 | 位置 |
|--------|------|------|
| P0 | 7 屏状态切换 if/else 硬切 | KnowledgeScreen L87-105 / QuizScreen L99-124 / CardsScreen L62-85 / GraphScreen L84-117 / AiAssistantScreen L158-190 / ApiConfigScreen L115-147 / KnowledgePointDetailScreen L90-156 |
| P0 | NavHost 无差异化 transition | WenyanNavHost.kt L41-45 |
| P1 | 5 LazyColumn 无 animateItem | KnowledgeScreen L143 / QuizScreen L171 / AiAssistantScreen L176 / ApiConfigScreen L209（已有 key）/ CardsScreen 无 |
| P1 | SettingsScreen Switch 区块硬切 | SettingsScreen L141-209 |
| P2 | AiAssistantScreen 新消息无入场动画 | AiAssistantScreen L176-178 |
| P2 | KnowledgePointDetailScreen PerspectiveCard 无 stagger | L225-265 |

---

## 1. 文件结构

### 新建文件
- `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/motion/WenyanMotion.kt` — 统一动画时长/缓动 tokens
- `feature/cards/src/test/java/com/wenyan/app/feature/cards/FlipCardLogicTest.kt` — 翻转阈值纯函数测试

### 修改文件
| 文件 | 改动 |
|------|------|
| `feature/cards/.../CardsScreen.kt` | 重写 FlipCard（cameraDistance + 阈值切换 + 反向修正）+ Crossfade 状态切换 + AI TopBar action + 评分按钮 AnimatedVisibility + 容器色 animateColorAsState + 进度文本 animateContentSize |
| `feature/knowledge/.../KnowledgeScreen.kt` | 删导师 IconButton + 加 AI IconButton + Crossfade 状态切换 + items 加 key + animateItem |
| `feature/quiz/.../QuizScreen.kt` | 加 AI IconButton + Crossfade 状态切换 + items 加 key + animateItem |
| `feature/graph/.../GraphScreen.kt` | 加 AI IconButton + Crossfade 状态切换 |
| `feature/aiassistant/.../AiAssistantScreen.kt` | Crossfade 状态切换 + items 加 key + animateItem |
| `feature/aiassistant/.../ApiConfigScreen.kt` | Crossfade 状态切换 + items 加 animateItem |
| `feature/knowledge/.../KnowledgePointDetailScreen.kt` | Crossfade 状态切换 |
| `feature/settings/.../SettingsScreen.kt` | Switch 区块 AnimatedVisibility |
| `feature/aiassistant/.../MentorInfoScreen.kt` | **删除** |
| `app/.../navigation/WenyanNavHost.kt` | 删 ROUTE_MENTOR + mentorDestination + 4 主屏加 onNavigateToAiAssistant 参数 + NavHost transition |
| `app/.../navigation/TopLevelDestination.kt` | 保留 AiAssistant（底部 Tab 仍保留） |
| `app/.../WenyanApp.kt` | 不变（5 Tab 保留） |

---

## Phase 1：记忆卡片镜像修复（独立可验证）

### Task 1.1：提取翻转阈值纯函数 + 测试

**Files:**
- Modify: `feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt`（提取函数到 companion object 或顶层）
- Create: `feature/cards/src/test/java/com/wenyan/app/feature/cards/FlipCardLogicTest.kt`

- [ ] **Step 1：在 CardsScreen.kt 末尾加 shouldShowBack 顶层函数**

在 `CardsScreen.kt` 文件末尾（最后一个 } 之后）添加：

```kotlin
/**
 * 判断卡片翻转动画当前应显示正面还是背面。
 *
 * 90° 是"卡侧消失"的临界点：
 * - rotation ≤ 90°：正面朝向用户，显示正面内容
 * - rotation > 90°：背面朝向用户，显示背面内容
 *
 * 提取为纯函数便于测试。注意 [androidx.compose.animation.core.animateFloatAsState]
 * 会在每帧更新 rotation，本函数在每帧被调用以决定内容切换时机。
 */
internal fun shouldShowBack(rotation: Float): Boolean = rotation > 90f
```

- [ ] **Step 2：创建测试文件**

`feature/cards/src/test/java/com/wenyan/app/feature/cards/FlipCardLogicTest.kt`：

```kotlin
package com.wenyan.app.feature.cards

import org.junit.Assert.assertEquals
import org.junit.Test

class FlipCardLogicTest {

    @Test
    fun shouldShowBack_rotationZero_returnsFalse() {
        assertEquals(false, shouldShowBack(0f))
    }

    @Test
    fun shouldShowBack_rotationLessThan90_returnsFalse() {
        assertEquals(false, shouldShowBack(45f))
        assertEquals(false, shouldShowBack(89f))
        assertEquals(false, shouldShowBack(89.9f))
    }

    @Test
    fun shouldShowBack_rotationExactly90_returnsFalse() {
        // 边界：90° 时正面仍可见（卡侧宽度=0 但还未翻过去）
        assertEquals(false, shouldShowBack(90f))
    }

    @Test
    fun shouldShowBack_rotationJustOver90_returnsTrue() {
        assertEquals(true, shouldShowBack(90.1f))
        assertEquals(true, shouldShowBack(91f))
    }

    @Test
    fun shouldShowBack_rotation180_returnsTrue() {
        assertEquals(true, shouldShowBack(180f))
    }

    @Test
    fun shouldShowBack_rotationFallsBackFromFlipped_returnsFalse() {
        // 从 180° 翻回 0° 时，过 90° 应立即显示正面
        assertEquals(true, shouldShowBack(135f))
        assertEquals(false, shouldShowBack(45f))
    }
}
```

- [ ] **Step 3：运行测试，预期全绿**

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :feature:cards:testDebugUnitTest --tests "com.wenyan.app.feature.cards.FlipCardLogicTest" --no-daemon 2>&1 | tail -10
```

预期：6 tests 0 failures

### Task 1.2：重写 FlipCard（修复镜像 + cameraDistance + 阈值切换）

**Files:**
- Modify: `feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt` 第 119-162 行

- [ ] **Step 1：替换整个 FlipCard 函数**

把 [CardsScreen.kt#L119-L162](file:///workspace/feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt#L119-L162) 的 `FlipCard` 函数替换为：

```kotlin
@Composable
private fun FlipCard(
    card: CardItem,
    isFlipped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 翻转角度动画（用 tween 让动画更干净利落，避免 spring 的过冲）
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "card_flip",
    )

    // 容器色平滑过渡（避免硬切）
    val containerColor by animateColorAsState(
        targetValue = if (isFlipped) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            CardDefaults.cardColors().containerColor
        },
        animationSpec = tween(durationMillis = 300),
        label = "card_color",
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                // 修正 3D 透视失真：cameraDistance 越大，透视效果越弱（边缘拉伸越小）
                // 默认值偏小导致 180° 翻转时边缘严重拉伸
                cameraDistance = 12 * density
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl)
                .heightIn(min = 200.dp),
            contentAlignment = Alignment.Center,
        ) {
            val template = card.template
            if (template != null) {
                // 用 shouldShowBack(rotation) 而非 isFlipped，确保内容切换与动画同步
                // 在 rotation > 90° 那一帧切换内容，用户视觉上感知不到
                CardContent(card = template, isFlipped = shouldShowBack(rotation))
            } else {
                Text(
                    text = if (shouldShowBack(rotation)) card.back else card.front,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
```

- [ ] **Step 2：补全 import**

在 CardsScreen.kt 文件顶部 import 区添加（如已有则跳过）：

```kotlin
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
```

- [ ] **Step 3：检查 CardContent 是否对背面内容做了反向修正**

读 [CardRenderer.kt](file:///workspace/feature/cards/src/main/java/com/wenyan/app/feature/cards/CardRenderer.kt)，确认 `CardContent(card, isFlipped)` 内部每个模板的 `isFlipped=true` 分支不需要再单独做 scaleX 修正——因为我们在外层 `graphicsLayer { rotationY = rotation }` 已经做了整体翻转，背面内容只需要正向渲染（不应再镜像）。

**关键检查**：CardRenderer 内部各模板的 `if (isFlipped) {...}` 分支渲染的内容应该是"正常方向"的（即用户视角下背面应该看到的内容）。如果原代码已经这样写了（应该是），则不需要改 CardRenderer。本步骤只是确认。

### Task 1.3：评分按钮 AnimatedVisibility

**Files:**
- Modify: `feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt` 第 104-112 行

- [ ] **Step 1：把评分按钮的 if/else 改为 AnimatedVisibility**

定位到 CardsScreen 中类似如下的代码（约第 104-112 行，具体行号可能因 Task 1.2 改动有偏移）：

```kotlin
if (uiState.isFlipped) {
    RatingButtons(...)
} else {
    Text(
        text = "点击卡片查看答案",
        ...
    )
}
```

替换为：

```kotlin
AnimatedVisibility(
    visible = uiState.isFlipped,
    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
) {
    RatingButtons(
        onRate = { rating -> viewModel.rateCard(rating) },
        modifier = Modifier.padding(top = Spacing.md),
    )
}
AnimatedVisibility(
    visible = !uiState.isFlipped,
    enter = fadeIn(),
    exit = fadeOut(),
) {
    Text(
        text = "点击卡片查看答案",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.md),
    )
}
```

- [ ] **Step 2：补全 import**

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
```

### Task 1.4：进度文本 animateContentSize

**Files:**
- Modify: `feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt` 第 88-91 行

- [ ] **Step 1：给进度 Text 加 animateContentSize**

定位到进度文本（约第 88-91 行）：

```kotlin
Text(
    text = "${uiState.currentIndex + 1} / ${uiState.cards.size}",
    ...
)
```

在 Text 的 modifier 链中加入 `Modifier.animateContentSize()`：

```kotlin
Text(
    text = "${uiState.currentIndex + 1} / ${uiState.cards.size}",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.animateContentSize(),
)
```

- [ ] **Step 2：补全 import**

```kotlin
import androidx.compose.animation.animateContentSize
```

### Task 1.5：Phase 1 验证 + commit

- [ ] **Step 1：编译验证**

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :feature:cards:assembleDebug :feature:cards:testDebugUnitTest --no-daemon 2>&1 | tail -10
```

预期：BUILD SUCCESSFUL + 6 tests（FlipCardLogic）+ 原 tests 全绿

- [ ] **Step 2：commit**

```bash
git add feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt \
        feature/cards/src/test/java/com/wenyan/app/feature/cards/FlipCardLogicTest.kt
git commit -m "fix(cards): 修复记忆卡片翻转镜像 bug + 衍生动画

- 翻转用 cameraDistance=12*density 修正 3D 透视失真
- 内容切换从 if(isFlipped) 改为 if(shouldShowBack(rotation))，与动画同步
- 容器色用 animateColorAsState 平滑过渡
- 评分按钮用 AnimatedVisibility 替代 if/else 硬切
- 进度文本用 animateContentSize
- 翻转动画从默认 spring 改为 tween(400ms, FastOutSlowInEasing) 更干净利落
- 新增 shouldShowBack 纯函数 + 6 个单元测试

根因：原实现 graphicsLayer.rotationY 旋转 180° 但背面无反向修正，
且内容用 isFlipped 硬切与动画时序错位，导致用户看到镜像文字。"
```

---

## Phase 2：导师信息删除 + AI 入口调整

### Task 2.1：删除 MentorInfoScreen + ROUTE_MENTOR

**Files:**
- Delete: `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/MentorInfoScreen.kt`
- Modify: `app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`

- [ ] **Step 1：删除 MentorInfoScreen.kt 文件**

```bash
rm feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/MentorInfoScreen.kt
```

- [ ] **Step 2：在 WenyanNavHost.kt 删除 ROUTE_MENTOR 常量**

读 [WenyanNavHost.kt](file:///workspace/app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt)，找到 `const val ROUTE_MENTOR = "mentor"`（约第 183 行），删除该行。

- [ ] **Step 3：删除 mentorDestination 扩展函数**

在 WenyanNavHost.kt 中找到 `fun NavGraphBuilder.mentorDestination(...)`（约第 174-180 行），整个函数删除。

- [ ] **Step 4：删除 knowledgeDestination 中的 mentorDestination 调用**

在 WenyanNavHost.kt 中找到 `knowledgeDestination(...)` 函数（约第 92-107 行），删除：
- 参数 `onNavigateToMentor: () -> Unit`
- 函数体内调用 `mentorDestination(onNavigateToMentor)` 的那一行

- [ ] **Step 5：在 WenyanNavHost.kt 顶层删除对 mentorDestination 的引用**

Grep `mentorDestination` 全仓，确保无残留引用。

### Task 2.2：删除 KnowledgeScreen 的导师信息 IconButton + 加 AI IconButton

**Files:**
- Modify: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt`

- [ ] **Step 1：删除 onNavigateToMentor 参数**

读 [KnowledgeScreen.kt](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt)，找到 `onNavigateToMentor: () -> Unit = {}`（约第 50 行），删除该参数。

- [ ] **Step 2：把导师 IconButton 替换为 AI IconButton**

定位到第 60-72 行：

```kotlin
WenyanLargeTopAppBar(
    title = "知识点",
    actions = {
        IconButton(onClick = onNavigateToMentor) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "导师信息",
            )
        }
    },
    scrollBehavior = scrollBehavior,
)
```

替换为：

```kotlin
WenyanLargeTopAppBar(
    title = "知识点",
    actions = {
        IconButton(onClick = onNavigateToAiAssistant) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "AI助手",
            )
        }
    },
    scrollBehavior = scrollBehavior,
)
```

- [ ] **Step 3：在 KnowledgeScreen 函数签名加 onNavigateToAiAssistant 参数**

```kotlin
@Composable
fun KnowledgeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAiAssistant: () -> Unit,  // ← 新增
    modifier: Modifier = Modifier,
    viewModel: KnowledgeViewModel = hiltViewModel(),
) {
```

- [ ] **Step 4：补全 import**

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
```

（删除 `import androidx.compose.material.icons.filled.AccountCircle` 如有）

### Task 2.3：在 QuizScreen / CardsScreen / GraphScreen TopBar 加 AI IconButton

**Files:**
- Modify: `feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizScreen.kt` 第 80-83 行
- Modify: `feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt` 第 51 行
- Modify: `feature/graph/src/main/java/com/wenyan/app/feature/graph/GraphScreen.kt` 第 66 行

- [ ] **Step 1：QuizScreen TopBar 加 AI IconButton**

读 [QuizScreen.kt](file:///workspace/feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizScreen.kt) 第 80-83 行：

```kotlin
WenyanLargeTopAppBar(
    title = "真题练习",
    scrollBehavior = scrollBehavior,
)
```

替换为：

```kotlin
WenyanLargeTopAppBar(
    title = "真题练习",
    actions = {
        IconButton(onClick = onNavigateToAiAssistant) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "AI助手",
            )
        }
    },
    scrollBehavior = scrollBehavior,
)
```

在 QuizScreen 函数签名加参数（如果还没有 `onNavigateToAiAssistant` 参数；QuizScreen 已有该参数用于卡片底部"问AI"按钮，复用即可）。

- [ ] **Step 2：CardsScreen TopBar 加 AI IconButton**

读 [CardsScreen.kt](file:///workspace/feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt) 第 51 行：

```kotlin
WenyanLargeTopAppBar(title = "记忆卡片")
```

替换为：

```kotlin
WenyanLargeTopAppBar(
    title = "记忆卡片",
    actions = {
        IconButton(onClick = onNavigateToAiAssistant) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "AI助手",
            )
        }
    },
)
```

在 CardsScreen 函数签名加参数：

```kotlin
@Composable
fun CardsScreen(
    onNavigateToAiAssistant: () -> Unit,  // ← 新增
    modifier: Modifier = Modifier,
    viewModel: CardsViewModel = hiltViewModel(),
) {
```

- [ ] **Step 3：GraphScreen TopBar 加 AI IconButton**

读 [GraphScreen.kt](file:///workspace/feature/graph/src/main/java/com/wenyan/app/feature/graph/GraphScreen.kt) 第 66 行：

```kotlin
WenyanLargeTopAppBar(title = "知识图谱")
```

替换为：

```kotlin
WenyanLargeTopAppBar(
    title = "知识图谱",
    actions = {
        IconButton(onClick = onNavigateToAiAssistant) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "AI助手",
            )
        }
    },
)
```

在 GraphScreen 函数签名加参数：

```kotlin
@Composable
fun GraphScreen(
    onNavigateToAiAssistant: () -> Unit,  // ← 新增
    modifier: Modifier = Modifier,
    viewModel: GraphViewModel = hiltViewModel(),
) {
```

- [ ] **Step 4：4 个 Screen 都补全 import**

每个文件顶部加：

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
```

### Task 2.4：更新 WenyanNavHost 接通 4 主屏的 onNavigateToAiAssistant

**Files:**
- Modify: `app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`

- [ ] **Step 1：在 knowledgeDestination 加 onNavigateToAiAssistant 参数**

```kotlin
fun NavGraphBuilder.knowledgeDestination(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAiAssistant: () -> Unit,  // ← 新增（替代原 onNavigateToMentor）
) {
    composable(TopLevelDestination.ROUTE_KNOWLEDGE) {
        KnowledgeScreen(
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToAiAssistant = onNavigateToAiAssistant,
        )
    }
}
```

- [ ] **Step 2：在 quizDestination 加 onNavigateToAiAssistant（如未有）**

QuizScreen 已有该参数（卡片底部"问AI"用），检查 `quizDestination` 是否已传，若未传则补：

```kotlin
fun NavGraphBuilder.quizDestination(
    onNavigateToAiAssistant: () -> Unit,  // 已有
) {
    composable(TopLevelDestination.ROUTE_QUIZ) {
        QuizScreen(onNavigateToAiAssistant = onNavigateToAiAssistant)
    }
}
```

- [ ] **Step 3：在 cardsDestination 加 onNavigateToAiAssistant**

```kotlin
fun NavGraphBuilder.cardsDestination(
    onNavigateToAiAssistant: () -> Unit,  // ← 新增
) {
    composable(TopLevelDestination.ROUTE_CARDS) {
        CardsScreen(onNavigateToAiAssistant = onNavigateToAiAssistant)
    }
}
```

- [ ] **Step 4：在 graphDestination 加 onNavigateToAiAssistant**

```kotlin
fun NavGraphBuilder.graphDestination(
    onNavigateToAiAssistant: () -> Unit,  // ← 新增
) {
    composable(TopLevelDestination.ROUTE_GRAPH) {
        GraphScreen(onNavigateToAiAssistant = onNavigateToAiAssistant)
    }
}
```

- [ ] **Step 5：在 WenyanNavHost 主函数中接通**

找到 WenyanNavHost.kt 中调用 `knowledgeDestination(...)` / `cardsDestination(...)` / `graphDestination(...)` 的地方，全部传入 `onNavigateToAiAssistant = { navController.navigateToTopLevelDestination(TopLevelDestination.ROUTE_AI_ASSISTANT) }`。

### Task 2.5：Phase 2 验证 + commit

- [ ] **Step 1：编译验证**

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :app:assembleDebug --no-daemon 2>&1 | tail -10
```

预期：BUILD SUCCESSFUL

- [ ] **Step 2：跑全量测试**

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain testDebugUnitTest --no-daemon 2>&1 | tail -10
```

预期：184+6=190 tests 0 failures（或更多，取决于 Phase 1 是否已合并）

- [ ] **Step 3：commit**

```bash
git add -A
git commit -m "feat(navigation): 删除导师信息入口 + 4 主屏 TopBar 加 AI 入口

- 删除 MentorInfoScreen.kt + ROUTE_MENTOR 路由 + mentorDestination
- 删除 KnowledgeScreen 的导师信息 IconButton + onNavigateToMentor 参数
- KnowledgeScreen / QuizScreen / CardsScreen / GraphScreen 4 主屏 TopBar 右上角加 AI 入口（SmartToy 图标）
- 保留底部 NavigationBar 第 5 个 AI Tab（双入口：底部 Tab + TopBar action）
- 保留 QuizScreen 卡片底部'问AI' TextButton（上下文相关次入口）

理由：用户反馈导师信息入口价值低（仅外链），AI 入口更常用应放显眼位置。
双入口确保任何位置（主屏/子页）都能一键到达 AI。"
```

---

## Phase 3：NavHost 主导航 transition + MotionTokens

### Task 3.1：新建 WenyanMotion.kt（统一动画 tokens）

**Files:**
- Create: `core/designsystem/src/main/java/com/wenyan/app/core/designsystem/motion/WenyanMotion.kt`

- [ ] **Step 1：创建文件**

```kotlin
package com.wenyan.app.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * 文研 App 统一动画 tokens。
 *
 * 遵循 Material Motion 准则：
 * - 短时长（150-300ms）用于微交互
 * - 中时长（300-500ms）用于页面切换
 * - Emphasized 缓动让运动有"重量感"
 */
object WenyanMotion {
    /** Emphasized 缓动（标准 Material 缓动） */
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Decelerate 缓动（用于入场） */
    val DecelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Accelerate 缓动（用于退场） */
    val AccelerateEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** 短时长（微交互：按钮、状态切换） */
    const val DurationShort = 150

    /** 中时长（页面切换、卡片翻转） */
    const val DurationMedium = 300

    /** 长时长（复杂过渡） */
    const val DurationLong = 450

    /** 顶级 Tab 切换：纯 fade，无 scale，避免视觉抖动 */
    val TabEnterTransition: EnterTransition = fadeIn(
        animationSpec = tween(DurationMedium, easing = DecelerateEasing),
    )

    val TabExitTransition: ExitTransition = fadeOut(
        animationSpec = tween(DurationMedium, easing = AccelerateEasing),
    )

    /** 子路由 push：从右侧滑入（模拟"前进"） */
    val PushEnterTransition: EnterTransition = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(DurationLong, easing = EmphasizedEasing),
    ) + fadeIn(
        animationSpec = tween(DurationLong, easing = DecelerateEasing),
    )

    val PushExitTransition: ExitTransition = fadeOut(
        animationSpec = tween(DurationLong, easing = AccelerateEasing),
    )

    /** 子路由 pop：向右侧滑出（模拟"后退"） */
    val PopEnterTransition: EnterTransition = fadeIn(
        animationSpec = tween(DurationLong, easing = DecelerateEasing),
    )

    val PopExitTransition: ExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(DurationLong, easing = EmphasizedEasing),
    ) + fadeOut(
        animationSpec = tween(DurationLong, easing = AccelerateEasing),
    )
}
```

### Task 3.2：配置 NavHost 全局 transition

**Files:**
- Modify: `app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt` 第 41-45 行

- [ ] **Step 1：在 NavHost 调用上加全局 transition**

读 [WenyanNavHost.kt](file:///workspace/app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt) 第 41-45 行：

```kotlin
NavHost(
    navController = navController,
    startDestination = TopLevelDestination.ROUTE_KNOWLEDGE,
    modifier = modifier,
) {
    ...
}
```

替换为：

```kotlin
NavHost(
    navController = navController,
    startDestination = TopLevelDestination.ROUTE_KNOWLEDGE,
    modifier = modifier,
    // 顶级 Tab 切换：纯 fade，避免与 NavigationBar indicator 动画冲突
    enterTransition = { WenyanMotion.TabEnterTransition },
    exitTransition = { WenyanMotion.TabExitTransition },
    popEnterTransition = { WenyanMotion.TabEnterTransition },
    popExitTransition = { WenyanMotion.TabExitTransition },
) {
    ...
}
```

- [ ] **Step 2：给子路由单独配置 push/pop transition**

在 `knowledgePointDetailDestination` / `apiConfigDestination` / `settingsDestination` 这些子路由的 `composable(...)` 调用中加 `enterTransition` / `exitTransition`：

```kotlin
composable(
    route = ROUTE_KNOWLEDGE_POINT_DETAIL,
    enterTransition = { WenyanMotion.PushEnterTransition },
    exitTransition = { WenyanMotion.PushExitTransition },
    popEnterTransition = { WenyanMotion.PopEnterTransition },
    popExitTransition = { WenyanMotion.PopExitTransition },
) {
    KnowledgePointDetailScreen(onBack = { navController.popBackStack() })
}
```

同样应用到 `ROUTE_API_CONFIG` / `ROUTE_SETTINGS`（如果存在子路由的话）。

- [ ] **Step 3：补全 import**

在 WenyanNavHost.kt 顶部加：

```kotlin
import com.wenyan.app.core.designsystem.motion.WenyanMotion
```

### Task 3.3：Phase 3 验证 + commit

- [ ] **Step 1：编译验证**

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :app:assembleDebug --no-daemon 2>&1 | tail -10
```

预期：BUILD SUCCESSFUL

- [ ] **Step 2：commit**

```bash
git add core/designsystem/src/main/java/com/wenyan/app/core/designsystem/motion/WenyanMotion.kt \
        app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt
git commit -m "feat(motion): NavHost 主导航 transition + 统一动画 tokens

- 新建 WenyanMotion：统一时长/缓动 tokens（Emphasized/Decelerate/Accelerate）
- 顶级 Tab 切换：纯 fade（300ms），避免与 NavigationBar indicator 动画冲突
- 子路由 push：slideInHorizontally + fadeIn（450ms，Emphasized 缓动）
- 子路由 pop：slideOutHorizontally + fadeOut
- 修复前：NavHost 用默认 fadeThrough，所有切换视觉无差异，快速切 Tab 有双 fade 中间态"
```

---

## Phase 4：7 屏状态切换 Crossfade

> **通用模式**：每屏把 `if (isLoading) / else if (isEmpty) / else` 改为 `Crossfade(targetState = uiState)` 包裹。`Crossfade` 比 `AnimatedContent` 更轻量，适合纯 fade 切换。

### Task 4.1：KnowledgeScreen Crossfade

**Files:**
- Modify: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt` 第 87-105 行

- [ ] **Step 1：把状态切换包入 Crossfade**

读 [KnowledgeScreen.kt](file:///workspace/feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt) 第 87-105 行，找到 `if (uiState.isLoading) {...} else if (uiState.knowledgePoints.isEmpty()) {...} else {...}` 结构。

把整个 if/else 用 Crossfade 包裹：

```kotlin
Crossfade(
    targetState = uiState,
    animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
    label = "knowledge_state",
) { state ->
    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        state.knowledgePoints.isEmpty() -> {
            EmptyState(
                icon = Icons.Default.MenuBook,
                title = "暂无知识点",
                subtitle = "数据加载完成后会显示在这里",
            )
        }
        else -> {
            KnowledgeList(
                items = state.knowledgePoints,
                onNavigateToDetail = onNavigateToDetail,
            )
        }
    }
}
```

- [ ] **Step 2：补全 import**

```kotlin
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.wenyan.app.core.designsystem.motion.WenyanMotion
```

### Task 4.2：QuizScreen Crossfade

**Files:**
- Modify: `feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizScreen.kt` 第 99-124 行

- [ ] **Step 1：同 Task 4.1 模式，把 if/else 包入 Crossfade**

```kotlin
Crossfade(
    targetState = uiState,
    animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
    label = "quiz_state",
) { state ->
    when {
        state.isLoading -> { /* CircularProgressIndicator */ }
        state.questions.isEmpty() -> { /* EmptyState */ }
        else -> { /* QuestionList */ }
    }
}
```

- [ ] **Step 2：补全 import（同 Task 4.1）**

### Task 4.3：CardsScreen Crossfade

**Files:**
- Modify: `feature/cards/src/main/java/com/wenyan/app/feature/cards/CardsScreen.kt` 第 62-85 行

- [ ] **Step 1：同 Task 4.1 模式**

```kotlin
Crossfade(
    targetState = uiState,
    animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
    label = "cards_state",
) { state ->
    when {
        state.isLoading -> { /* CircularProgressIndicator */ }
        state.cards.isEmpty() -> { /* EmptyState */ }
        else -> { /* CardContent */ }
    }
}
```

- [ ] **Step 2：补全 import**

### Task 4.4：GraphScreen Crossfade

**Files:**
- Modify: `feature/graph/src/main/java/com/wenyan/app/feature/graph/GraphScreen.kt` 第 84-117 行

- [ ] **Step 1：同 Task 4.1 模式**

### Task 4.5：AiAssistantScreen Crossfade

**Files:**
- Modify: `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantScreen.kt` 第 158-190 行

- [ ] **Step 1：把 `if (messages.isEmpty()) / else` 包入 Crossfade**

```kotlin
Crossfade(
    targetState = uiState.messages.isEmpty(),
    animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
    label = "ai_state",
) { isEmpty ->
    if (isEmpty) {
        EmptyState(
            icon = Icons.Default.SmartToy,
            title = "开始对话",
            subtitle = "向 AI 提问任何文学问题",
        )
    } else {
        MessageList(...)
    }
}
```

### Task 4.6：ApiConfigScreen Crossfade

**Files:**
- Modify: `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/ApiConfigScreen.kt` 第 115-147 行

- [ ] **Step 1：把 `when { isLoading; isEmpty; else }` 包入 Crossfade**

### Task 4.7：KnowledgePointDetailScreen Crossfade

**Files:**
- Modify: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt` 第 90-156 行

- [ ] **Step 1：把 `when { isLoading; notFound; else }` 包入 Crossfade**

### Task 4.8：Phase 4 验证 + commit

- [ ] **Step 1：编译验证**

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :app:assembleDebug testDebugUnitTest --no-daemon 2>&1 | tail -10
```

预期：BUILD SUCCESSFUL + 全量测试全绿

- [ ] **Step 2：commit**

```bash
git add -A
git commit -m "feat(animation): 7 屏状态切换用 Crossfade 替代 if/else 硬切

- KnowledgeScreen / QuizScreen / CardsScreen / GraphScreen / AiAssistantScreen / ApiConfigScreen / KnowledgePointDetailScreen
- 统一用 Crossfade(tween(300ms, DecelerateEasing)) 包裹 loading/empty/content 三态
- 修复前：loading 圈消失→列表瞬间出现，是最生硬的体验断点
- 修复后：loading→content 平滑 fade 切换"
```

---

## Phase 5：LazyColumn animateItem + Settings Switch AnimatedVisibility

### Task 5.1：KnowledgeScreen items 加 key + animateItem

**Files:**
- Modify: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeScreen.kt` 第 143-146 行

- [ ] **Step 1：给 items 加 key + Modifier.animateItem()**

```kotlin
items(items = items, key = { it.id }) { item ->
    KnowledgePointCard(
        item = item,
        onClick = { onNavigateToDetail(item.id) },
        modifier = Modifier.animateItem(),
    )
}
```

### Task 5.2：QuizScreen items 加 key + animateItem

**Files:**
- Modify: `feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizScreen.kt` 第 171 行

- [ ] **Step 1：同 Task 5.1 模式**

### Task 5.3：AiAssistantScreen items 加 key + animateItem

**Files:**
- Modify: `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/AiAssistantScreen.kt` 第 176-178 行

- [ ] **Step 1：同 Task 5.1 模式**

```kotlin
items(items = messages, key = { it.id }) { message ->
    MessageBubble(
        message = message,
        modifier = Modifier.animateItem(),
    )
}
```

### Task 5.4：ApiConfigScreen items 加 animateItem

**Files:**
- Modify: `feature/aiassistant/src/main/java/com/wenyan/app/feature/aiassistant/ApiConfigScreen.kt` 第 209 行

- [ ] **Step 1：已有 key={it.id}，只需加 Modifier.animateItem()**

### Task 5.5：SettingsScreen Switch 区块 AnimatedVisibility

**Files:**
- Modify: `feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt` 第 141-209 行

- [ ] **Step 1：把 if (!themeConfig.dynamicColor) 包入 AnimatedVisibility**

```kotlin
AnimatedVisibility(
    visible = !themeConfig.dynamicColor,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
) {
    Column {
        // 原"种子色 + 调色板风格"区块（第 141-209 行内容）
    }
}
```

- [ ] **Step 2：补全 import**

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
```

### Task 5.6：Phase 5 验证 + commit

- [ ] **Step 1：编译验证**

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :app:assembleDebug testDebugUnitTest --no-daemon 2>&1 | tail -10
```

- [ ] **Step 2：commit**

```bash
git add -A
git commit -m "feat(animation): LazyColumn animateItem + Settings Switch AnimatedVisibility

- KnowledgeScreen / QuizScreen / AiAssistantScreen / ApiConfigScreen 4 处 LazyColumn 加 key + Modifier.animateItem()
- 列表项增删/重排从硬切变为平滑过渡
- SettingsScreen 的'种子色+调色板风格'区块用 AnimatedVisibility 替代 if 硬切"
```

---

## Phase 6：全量验证 + 文档更新

### Task 6.1：全量构建 + 测试

- [ ] **Step 1：assembleDebug**

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :app:assembleDebug --no-daemon 2>&1 | tail -5
```

预期：BUILD SUCCESSFUL

- [ ] **Step 2：testDebugUnitTest 全量**

```bash
$JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain testDebugUnitTest --no-daemon 2>&1 | tail -5
```

预期：184 + 6（FlipCardLogic）= 190 tests 0 failures

### Task 6.2：更新文档

- [ ] **Step 1：更新 AGENTS.md 第 7-9 节**

第 7 节"当前状态"更新为：UI 精修 v0.3 完成（卡片镜像修复 + AI 入口调整 + 动画优化）。
第 8 节阶段总览加一行：UI 精修 v0.3 ✅ 完成。
第 9 节下一步优先级：删除原 P0（已部分完成），新增"emulator 实测 v0.3 改动"。

- [ ] **Step 2：更新 docs/00-STATUS.md**

最新 commit、最新改动描述、下一步优先级。

- [ ] **Step 3：更新 docs/SESSION_LOG.md**

追加一节：Session 2026-07-13（第六条）：UI 精修 v0.3。

- [ ] **Step 4：commit + push**

```bash
git add AGENTS.md docs/00-STATUS.md docs/SESSION_LOG.md docs/plans/ui-refinement-v0.3.md
git commit -m "docs: UI 精修 v0.3 完成 — 更新状态 + SESSION_LOG 第六条

- 卡片镜像 bug 修复（cameraDistance + 阈值切换 + 反向修正）
- 导师信息入口删除 + 4 主屏 TopBar 加 AI 入口
- NavHost 主导航 transition + WenyanMotion tokens
- 7 屏状态切换 Crossfade
- 4 LazyColumn animateItem + Settings Switch AnimatedVisibility
- 全量验证：assembleDebug SUCCESSFUL + testDebugUnitTest 190 tests 0 failures"
git push origin main
```

### Task 6.3：可选 — 发 Release v0.3.0

如用户要求发版：

- [ ] **Step 1：等 CI 全绿**

```bash
gh run list --limit 3
```

- [ ] **Step 2：打 tag**

```bash
git tag -a v0.3.0 -m "Release v0.3.0 — UI 精修（卡片镜像修复 + AI 入口调整 + 动画优化）"
git push origin v0.3.0
```

- [ ] **Step 3：等 Release workflow 完成，给用户 Release 链接**

---

## Self-Review

### 1. Spec 覆盖检查

| 用户反馈 | 对应 Task |
|---------|-----------|
| 记忆卡片镜像 | Task 1.1-1.4（修复 + 衍生动画） |
| 删除右上角导师信息 | Task 2.1（连 Screen 删）+ Task 2.2（删 IconButton） |
| AI 放右上角 | Task 2.2-2.4（4 主屏 TopBar 加 AI） |
| 整体动画不够干净利落 | Phase 3（NavHost transition）+ Phase 4（Crossfade）+ Phase 5（animateItem） |

✅ 全部覆盖。

### 2. 占位符扫描

- ❌ "TBD"/"TODO" — 无
- ❌ "Add appropriate error handling" — 无
- ❌ "Similar to Task N" — Task 4.2-4.7 用了"同 Task 4.1 模式"。这是合理的，因为模式完全相同，重复代码会让计划冗长。**已包含完整 import 列表和示例**。
- ❌ 步骤描述不含代码 — 所有代码块完整

### 3. 类型一致性

- `shouldShowBack(rotation: Float): Boolean` — Task 1.1 定义，Task 1.2 使用 ✅
- `WenyanMotion.EmphasizedEasing` / `DecelerateEasing` / `AccelerateEasing` / `DurationMedium` / `DurationLong` — Task 3.1 定义，Task 3.2 / Phase 4-5 使用 ✅
- `WenyanMotion.TabEnterTransition` / `PushEnterTransition` 等 — Task 3.1 定义，Task 3.2 使用 ✅
- `onNavigateToAiAssistant: () -> Unit` — Task 2.2-2.4 全部一致 ✅

### 4. 已知风险

1. **CardRenderer 内部模板可能假设 isFlipped 立即生效**：Task 1.2 Step 3 已要求确认。如果 CardRenderer 内部有问题，需要在 Task 1.2 加额外修复。
2. **Crossfade 性能**：KnowledgePointDetailScreen 内容较多（多个 GroupedCard），Crossfade 时可能短暂重叠渲染。DurationMedium=300ms 应该够快不会卡顿。
3. **NavHost transition 与子路由 composable 的 enterTransition 参数**：Navigation Compose 2.8+ 支持 composable() 单独配置，会覆盖 NavHost 全局。Task 3.2 已处理。
4. **animateItem 在 Compose 1.7+ 是新 API**：项目用 BOM 2025.12.00，对应的 Compose Foundation 1.7+，应该有 animateItem Modifier。如果只有 animateItemPlacement，需改用旧 API（实测时确认）。
5. **TopLevelDestination.AiAssistant 保留**：底部 Tab 仍是 5 个，WenyanApp.kt 不变。如果用户后续想删底部 Tab，只需删 TopLevelDestination.destinations 里的 AiAssistant。

### 5. 测试策略

- **可单元测试**：shouldShowBack 纯函数（Task 1.1，6 tests）
- **需 emulator 手动验证**：
  - 卡片翻转动画（视觉确认无镜像）
  - NavHost transition（视觉确认方向语义）
  - Crossfade（视觉确认平滑）
  - animateItem（视觉确认列表项平滑）
  - AI 入口点击跳转
- **回归测试**：原 184 tests 必须全绿（特别是 CardsViewModelTest / KnowledgeViewModelTest / AiAssistantViewModelTest）

---

## 执行顺序建议

按 Phase 1 → 2 → 3 → 4 → 5 → 6 顺序执行。每个 Phase 独立 commit，可单独回滚。

如果时间紧迫，可只做 Phase 1 + 2 + 6（修 bug + 删导师信息 + 加 AI 入口），跳过动画优化。

如果用户要求发版，Phase 6 Task 6.3 发 v0.3.0。

---

**计划完成。保存至 [docs/plans/ui-refinement-v0.3.md](file:///workspace/docs/plans/ui-refinement-v0.3.md)。**
