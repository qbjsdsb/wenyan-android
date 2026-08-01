# 真题 → 论述题 Tab 迁移计划

> 版本：v2.0（精修版）
> 状态：计划阶段（不执行）
> 设计依据：docs/design/essay-module-design.md
> 审查框架：staff-engineer-mode migration-and-deprecation specialist

---

## 0. 迁移决策记录

| 字段 | 值 |
|------|-----|
| 被替换项 | "真题" Tab（`TopLevelDestination.Quiz`，route=`"quiz"`） |
| 替换项 | "论述题" Tab（`TopLevelDestination.Essay`，route=`"essay"`） |
| 替换理由 | 论述题板块已完整开发（v0.9.8 Phase 0-2），功能覆盖并超越真题模块 |
| 保留设施 | `feature:quiz` 模块不删除（WrongAnswerScreen 代码在此模块中） |
| 迁移模式 | **Compulsory（强制性）** — 替换后无新 usage 走旧路由 |
| 截止日期 | 本迁移执行完毕即生效 |

---

## 1. 现状分析

### 1.1 当前底部导航结构（5 Tab）

```
知识点 | 真题 | 卡片 | 错题本 | 设置
```

### 1.2 目标底部导航结构（5 Tab）

```
知识点 | 论述题 | 卡片 | 错题本 | 设置
```

### 1.3 完整 Usage Inventory（代码引用全面盘点）

#### 1.3.1 静态引用 — `TopLevelDestination.Quiz` / `ROUTE_QUIZ`

| 文件 | 行号 | 引用类型 | 迁移处理 |
|------|------|---------|---------|
| `TopLevelDestination.kt` | L39-43 | `data object Quiz` 定义 | **删除**，替换为 Essay |
| `TopLevelDestination.kt` | L66 | `const val ROUTE_QUIZ = "quiz"` | **删除**，替换为 ROUTE_ESSAY |
| `TopLevelDestination.kt` | L6 | `import Icons.Filled.Quiz` | **删除**（不再需要） |
| `TopLevelDestination.kt` | L42 | `icon = Icons.Filled.Quiz` | **删除**（随 Quiz data object 删除） |
| `TopLevelDestination.kt` | L73 | `Quiz` 在 `destinations` 列表中 | **替换**为 `Essay` |
| `WenyanNavHost.kt` | L233 | `composable(TopLevelDestination.ROUTE_QUIZ)` | **删除**（整个 quizDestination 函数） |
| `WenyanNavHost.kt` | L17 | `import ...QuizScreen` | **删除**（死 import） |
| `WenyanNavHost.kt` | L89-103 | `quizDestination(...)` 调用 | **删除** |
| `AboutTutorialScreen.kt` | L28 | `import Icons.Filled.Quiz` | **保留**（仍用于描述性图标） |
| `AboutTutorialScreen.kt` | L229 | `leadingIcon = Icons.Filled.Quiz` | **更新**为 `Icons.AutoMirrored.Filled.MenuBook`，文字同步改"论述题" |

#### 1.3.2 静态引用 — `essayListDestination` / `ROUTE_ESSAY_LIST`

| 文件 | 行号 | 引用类型 | 迁移处理 |
|------|------|---------|---------|
| `WenyanNavHost.kt` | L82-87 | `onNavigateToEssays` lambda 调用 `navController.navigate(ROUTE_ESSAY_LIST)` | **删除**（随 knowledgeDestination 参数删除） |
| `WenyanNavHost.kt` | L202-210 | `essayListDestination(...)` 调用 | **删除** |
| `WenyanNavHost.kt` | L390-406 | `essayListDestination()` 函数定义 | **删除** |
| `WenyanNavHost.kt` | L414 | `private const val ROUTE_ESSAY_LIST = "essay_list"` | **删除**（所有引用已移除） |

#### 1.3.3 静态引用 — `onNavigateToEssays`

| 文件 | 行号 | 引用类型 | 迁移处理 |
|------|------|---------|---------|
| `WenyanNavHost.kt` | L218 | `knowledgeDestination` 签名参数 | **删除** |
| `WenyanNavHost.kt` | L224 | `knowledgeDestination` 内传入 | **删除** |
| `KnowledgeScreen.kt` | L85 | `KnowledgeScreen` 签名参数 | **删除** |
| `KnowledgeScreen.kt` | L192 | `KnowledgeList` 调用传参 | **删除** |
| `KnowledgeScreen.kt` | L281 | `KnowledgeList` 签名参数 | **删除** |
| `KnowledgeScreen.kt` | L296 | `EssayEntryCard(onClick = onNavigateToEssays)` | **删除**（EssayEntryCard 整个删除） |

#### 1.3.4 动态引用（运行时确认）

| 组件 | 引用方式 | 迁移处理 |
|------|---------|---------|
| `WenyanApp.kt` L45 | `TopLevelDestination.destinations.firstOrNull` 高亮计算 | **无需改动**（动态映射） |
| `WenyanApp.kt` L54 | `TopLevelDestination.destinations.map { it.route }` 路由集合 | **无需改动**（动态映射） |
| `WenyanApp.kt` L67 | `TopLevelDestination.destinations.map` → `WenyanNavItem` | **无需改动**（动态映射） |

#### 1.3.5 测试引用

| 文件 | 引用 | 迁移处理 |
|------|------|---------|
| `QuizViewModelTest.kt` | QuizViewModel 单元测试 | **保留**（quiz 模块不删除，测试仍可运行） |
| `EssayListViewModelTest.kt` | EssayListViewModel 单元测试 | **无需改动** |

### 1.4 数据流分析

**论述题数据流（现状，良好，无需改动）：**
```
EssayListViewModel
  ├── knowledgeRepository.observeAllEssays()  → ExamQuestionDao (core:database)
  ├── chapterRepository.observeSubjects()     → SubjectDao (core:database)
  └── 内存筛选（年份/科目/onlyWithAngle）

EssayDetailViewModel
  ├── knowledgeRepository.observeEssayById()  → ExamQuestionDao
  ├── parseEssayAngle() / parseEssayNotes()   → JSON 解析
  ├── knowledgeRepository.getKnowledgePointsByIds() → KnowledgePointDao
  ├── socraticTutor.guideEssayAnswer()        → core:ai
  ├── wrongAnswerRepository.recordWrongAnswer() → WrongAnswerDao
  └── schedulingRepository.rateWrongAnswer()   → FSRS 调度
```

**关键结论：** 论述题数据层完全独立于 Quiz 模块，迁移后无需修改任何数据层代码。

### 1.5 依赖关系分析

```
app/build.gradle.kts:
  ├── implementation(project(":feature:knowledge"))  ← Essay 在此模块
  ├── implementation(project(":feature:quiz"))       ← 含 QuizScreen + WrongAnswerScreen
  └── 其他模块...

settings.gradle.kts:
  ├── include(":feature:knowledge")
  ├── include(":feature:quiz")
  └── 其他模块...
```

**关键问题：** WrongAnswerScreen 的代码在 `feature:quiz` 模块中。直接删除 `feature:quiz` 会破坏错题本 Tab。

---

## 2. 方案对比

### 方案 A：最小改动（推荐）

只改导航层，不移动/删除模块。

**改动范围：** 5 个文件
**风险：** 最低
**代价：** Quiz 模块保留（死代码），WrongAnswer 仍在 quiz 模块中

### 方案 B：完整清理

方案 A + 将 WrongAnswer 移出 quiz 模块，然后删除 quiz 模块。

**改动范围：** ~10 个文件
**风险：** 中等（模块迁移需改 module 声明、依赖、包名）
**代价：** 代码干净，无死代码

### 方案 C：渐进式

先做方案 A，后续再决定是否做方案 B。

**推荐理由：** 优先交付用户价值（论述题 Tab），模块清理可后续进行。

---

## 3. 方案 A 详细步骤（推荐，最小改动）

### 迁移批次划分

将改动分为 **3 个批次**，每批次可独立编译验证：

| 批次 | 步骤 | 验证点 | 回滚范围 |
|------|------|--------|---------|
| **Batch 1：Tab 替换** | Step 1 | Destinations 编译通过 | 1 文件 |
| **Batch 2：导航层** | Step 2 → Step 3 → Step 5 | NavHost 编译通过 | 2 文件 |
| **Batch 3：UI 清理** | Step 4 → Step 6 | 全模块编译通过 | 2 文件 |

---

### Batch 1: Tab 替换

#### Step 1: 修改 `TopLevelDestination.kt`

**改动：**
- 删除 `data object Quiz` 
- 新增 `data object Essay`（route = `"essay"`, label = `"论述题"`, icon = `Icons.AutoMirrored.Filled.MenuBook`）
- `companion object` 中：删除 `ROUTE_QUIZ`，新增 `ROUTE_ESSAY = "essay"`
- `destinations` 列表：将 `Quiz` 替换为 `Essay`（保持顺序：Knowledge → Essay → Cards → WrongAnswer → Settings）
- 清理死 import：`import Icons.Filled.Quiz`

**注意：** 图标选择 `Icons.AutoMirrored.Filled.MenuBook`（与当前 EssayEntryCard 一致，用户已熟悉）。

**验证：** `:app:compileDebugKotlin` BUILD SUCCESSFUL

---

### Batch 2: 导航层

#### Step 2: 修改 `EssayListScreen.kt` — 使 `onBack` 可选

**当前位置：** `feature/knowledge/src/main/java/.../EssayListScreen.kt`

**改动：** `onBack: () -> Unit = {}` → `onBack: (() -> Unit)? = null`

**代码变更：**
```kotlin
// 改前
fun EssayListScreen(
    onBack: () -> Unit = {},
    ...

// 改后
fun EssayListScreen(
    onBack: (() -> Unit)? = null,
    ...
```

**设计决策：**
- `onBack = null` → 顶级 Tab 模式，WenyanLargeTopAppBar 不显示返回箭头
- `onBack = { navController.popBackStack() }` → 子路由模式（当前 essayListDestination 调用方式）
- **WenyanLargeTopAppBar 已支持 nullable onBack**（L63: `onBack: (() -> Unit)? = null`），无需额外适配

**验证：** `:feature:knowledge:compileDebugKotlin` BUILD SUCCESSFUL

#### Step 3: 修改 `WenyanNavHost.kt`

**3a. 新增 `essayTabDestination()` — 顶级 Tab 版本**

```kotlin
private fun NavGraphBuilder.essayTabDestination(
    onNavigateToEssayDetail: (String) -> Unit,
) {
    // ⚠️ 使用 Tab fade transition（NavHost 默认），而非 Push/Pop slide
    // 与 quizDestination/cardsDestination/wrongAnswerDestination 一致
    composable(TopLevelDestination.ROUTE_ESSAY) {
        EssayListScreen(
            onBack = null,  // 顶级模式，无返回箭头
            onNavigateToEssayDetail = onNavigateToEssayDetail,
        )
    }
}
```

**3b. 替换 `quizDestination()` 调用**

```kotlin
// 删除：
// quizDestination(
//     onNavigateToAiAssistant = {...},
//     onNavigateToDetail = {...},
// )
// 改为：
essayTabDestination(
    onNavigateToEssayDetail = { essayId ->
        navController.navigate("$ROUTE_ESSAY_DETAIL/$essayId") {
            launchSingleTop = true
        }
    },
)
```

**3c. 删除 `essayListDestination()` 调用和函数定义**

不再需要，因为：
- 知识点 Tab 不再有 EssayEntryCard（Step 4）
- 顶级 Tab 直接走 `essayTabDestination`
- 唯一的子路由入口是 `essayDetailDestination`（论述题详情）

**3d. 删除 `ROUTE_ESSAY_LIST` 常量**

```kotlin
// 删除：
private const val ROUTE_ESSAY_LIST = "essay_list"
```

**3e. 清理死 import**

```kotlin
// 删除：
import com.wenyan.app.feature.quiz.QuizScreen
```

**验证：** `:app:compileDebugKotlin` BUILD SUCCESSFUL

#### Step 5: 更新 `WenyanNavHost` 中 `knowledgeDestination` 调用

**改动：**
- `knowledgeDestination(` 调用中删除 `onNavigateToEssays` lambda
- `knowledgeDestination(` 函数签名中删除 `onNavigateToEssays: () -> Unit` 参数

```kotlin
// 改前调用：
knowledgeDestination(
    onNavigateToAiAssistant = {...},
    onNavigateToDetail = {...},
    onNavigateToEssays = {...},  // ← 删除
)

// 改后调用：
knowledgeDestination(
    onNavigateToAiAssistant = {...},
    onNavigateToDetail = {...},
)
```

```kotlin
// 改前函数签名：
private fun NavGraphBuilder.knowledgeDestination(
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEssays: () -> Unit,  // ← 删除
)

// 改后函数签名：
private fun NavGraphBuilder.knowledgeDestination(
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
)
```

**验证：** `:app:compileDebugKotlin` BUILD SUCCESSFUL

---

### Batch 3: UI 清理

#### Step 4: 修改 `KnowledgeScreen.kt` — 移除 EssayEntryCard

**改动：**
- `KnowledgeScreen` 函数签名删除 `onNavigateToEssays: () -> Unit = {}` 参数
- `KnowledgeList` 调用删除 `onNavigateToEssays = onNavigateToEssays` 传参
- `KnowledgeList` 函数签名删除 `onNavigateToEssays: () -> Unit` 参数
- KnowledgeList 的 `LazyColumn` 中删除 `EssayEntryCard` item（L295-297）
- 删除 `EssayEntryCard` Composable 函数（L319-361）
- 清理死 import：`Icons.AutoMirrored.Filled.MenuBook`（如果仅 EssayEntryCard 使用）

**验证：** `:feature:knowledge:compileDebugKotlin` BUILD SUCCESSFUL

#### Step 6: 更新 `AboutTutorialScreen.kt` — 同步功能描述

**当前位置：** `feature/settings/.../AboutTutorialScreen.kt`

**改动：**
- 将"真题"条目（L226-231）更新为"论述题"条目
- `leadingIcon = Icons.Filled.Quiz` → `leadingIcon = Icons.AutoMirrored.Filled.MenuBook`
- `title = "真题"` → `title = "论述题"`
- `subtitle = "历年真题"` → `subtitle = "真题论述题"`
- `description = "按年份分组，答题后自评，答错自动入错题本。"` → `description = "历年真题论述题 · 审题思路 + 依据 + 知识点串联"`

**理由：** 教程页描述应与实际界面一致，避免用户困惑。

**验证：** `:feature:settings:compileDebugKotlin` BUILD SUCCESSFUL

---

## 4. 方案 A 完整文件改动清单

| # | 文件 | 改动类型 | 所属批次 | 说明 |
|---|------|---------|---------|------|
| 1 | `app/.../navigation/TopLevelDestination.kt` | 修改 | Batch 1 | Quiz → Essay 替换，清理 Quiz import |
| 2 | `feature/knowledge/.../EssayListScreen.kt` | 修改 | Batch 2 | `onBack: () -> Unit` → `(() -> Unit)? = null` |
| 3 | `app/.../navigation/WenyanNavHost.kt` | 修改 | Batch 2 | 新增 essayTabDestination + 删除 quizDestination + 删除 essayListDestination + 删除 ROUTE_ESSAY_LIST + 清理 QuizScreen import + 更新 knowledgeDestination 签名 |
| 4 | `feature/knowledge/.../KnowledgeScreen.kt` | 修改 | Batch 3 | 删除 EssayEntryCard + onNavigateToEssays 参数 |
| 5 | `feature/settings/.../AboutTutorialScreen.kt` | 修改 | Batch 3 | 同步功能描述（真题 → 论述题） |

**总计：** 5 个文件修改，零新增文件，零删除文件。

---

## 5. 不修改的文件（已验证无影响）

| 文件 | 不修改的理由 |
|------|-------------|
| `WenyanApp.kt` | 使用 `TopLevelDestination.destinations` 动态映射，无需改动 |
| `EssayDetailScreen.kt` | 子路由，不受 Tab 变更影响 |
| `EssayDetailViewModel.kt` | 数据层和导航逻辑完全独立 |
| `EssayListViewModel.kt` | 数据层逻辑完全独立 |
| `EssayDetailModels.kt` | 纯数据模型 |
| `QuizScreen.kt` | 保留（不删除，避免影响现有测试） |
| `QuizViewModel.kt` | 保留（不删除，避免影响测试） |
| `WrongAnswerScreen.kt` | 保留（错题本 Tab 仍需） |
| `WrongAnswerViewModel.kt` | 保留 |
| `feature/quiz/build.gradle.kts` | 保留（模块不删除） |
| `settings.gradle.kts` | 保留（`:feature:quiz` include 不删除） |
| `app/build.gradle.kts` | 保留（`:feature:quiz` 依赖不删除，因 WrongAnswer 依赖） |
| `core/data/.../KnowledgeRepository.kt` | 数据层完全独立 |
| `core/database/.../ExamQuestionDao.kt` | 数据层完全独立 |

---

## 6. 风险评估

### 6.1 风险矩阵

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| EssayListScreen 的 onBack 改为 nullable 后 WenyanLargeTopAppBar 不兼容 | ~~中~~ **低** | 高 | ✅ **已确认** WenyanLargeTopAppBar 的 onBack 已是 `(() -> Unit)? = null`，天然兼容 |
| 知识点 Tab → 论述题详情子路由导航中断 | 低 | 高 | 验证 KnowledgePointDetailScreen 的 onNavigateToEssay 仍正常（走 essayDetailDestination，不受影响） |
| 错题本 Tab 因 quiz 模块保留而正常 | 低 | 高 | 不删除 quiz 模块，错题本完全不受影响 |
| 测试因 Quiz 路由删除而失败 | 低 | 中 | QuizViewModelTest 引用 quiz 模块内部类，模块不删除则测试不受影响；EssayListScreen 签名变更可能影响 EssayListViewModelTest — 检查测试中是否构造 EssayListScreen |
| **G5: 未来代码误引用 ROUTE_QUIZ** | 低 | 低 | `ROUTE_QUIZ` 从 TopLevelDestination 的 companion object 中删除后，编译器直接报错，无法误用 |
| **G7: 导航栏选中高亮中断** | 低 | 高 | 验证 essay 路由在 `WenyanApp.kt` 的 `selectedTopLevelRoute` 计算中被正确匹配 |

### 6.2 回滚方案

如果迁移后出现问题，按批次回滚：

| 批次 | 回滚操作 |
|------|---------|
| Batch 3 | 恢复 `KnowledgeScreen.kt` 的 EssayEntryCard + `AboutTutorialScreen.kt` 描述 |
| Batch 2 | 恢复 `WenyanNavHost.kt` 的 `quizDestination` 调用 + `essayListDestination` + 恢复 `EssayListScreen.kt` 的 `onBack` 签名 |
| Batch 1 | 恢复 `TopLevelDestination.kt` 的 `Quiz` data object |

**关键：** 回滚 Batch 1 即可完全恢复，Batch 2 和 Batch 3 是 UI 清理，不回滚也不影响功能。

---

## 7. 验证清单（执行前逐项检查）

### 7.1 编译检查
- [ ] `:app:assembleDebug` BUILD SUCCESSFUL
- [ ] 全模块 `testDebugUnitTest` 全绿

### 7.2 静态检查（代码审查）
- [ ] `TopLevelDestination.kt` 无 `ROUTE_QUIZ` / `Icons.Filled.Quiz` 引用
- [ ] `WenyanNavHost.kt` 无 `quizDestination` 调用 / `QuizScreen` import / `ROUTE_ESSAY_LIST` 常量
- [ ] `KnowledgeScreen.kt` 无 `onNavigateToEssays` 参数 / `EssayEntryCard` 引用
- [ ] `EssayListScreen.kt` 的 `onBack` 为 `(() -> Unit)? = null`
- [ ] `AboutTutorialScreen.kt` 的"真题"条目已更新为"论述题"

### 7.3 功能验证（需 emulator 实测）
- [ ] 底部导航栏显示"论述题"而非"真题"
- [ ] 点击"论述题"Tab → 展示论述题列表（无返回箭头）
- [ ] 论述题列表页三维筛选正常工作（年份/科目/onlyWithAngle）
- [ ] 点击论述题 → 详情页渲染（11 区块结构）
- [ ] 详情页 AI 审题助手正常工作
- [ ] 知识点 Tab → 知识点详情 → 相关论述题 → 跳转正常
- [ ] 知识点 Tab 列表顶部不再显示 EssayEntryCard
- [ ] 错题本 Tab 完全正常
- [ ] "真题"相关数据不丢失（数据库中有分类，只是 UI 入口移除）
- [ ] 导航栏选中高亮在 5 个 Tab 间切换正确

### 7.4 Completion Evidence（迁移完成证据）
- [ ] `ROUTE_QUIZ` 在项目中无任何引用（`Grep "ROUTE_QUIZ"` 0 结果）
- [ ] `onNavigateToEssays` 在项目中无任何引用（`Grep "onNavigateToEssays"` 0 结果）
- [ ] `EssayEntryCard` 在项目中无任何引用（`Grep "EssayEntryCard"` 0 结果）
- [ ] `ROUTE_ESSAY_LIST` 在项目中无任何引用（`Grep "ROUTE_ESSAY_LIST"` 0 结果）

---

## 8. Backsliding Prevention（防止回退）

| 机制 | 说明 |
|------|------|
| 编译器强制 | `ROUTE_QUIZ` 从 companion object 中删除后，任何引用 `TopLevelDestination.ROUTE_QUIZ` 的代码直接编译失败 |
| 死代码审查 | 在 Code Review 中检查 `ROUTE_QUIZ` / `Icons.Filled.Quiz` 是否重新出现 |
| 测试覆盖 | EssayListViewModelTest 确保 Essay 功能持续正常 |

---

## 9. 后续优化（方案 B — 暂不执行）

### Phase 2: 模块清理
- 将 `WrongAnswerScreen.kt` + `WrongAnswerViewModel.kt` 迁移到独立模块（如 `feature:wronganswer`）或 `feature:knowledge` 模块
- 删除 `feature:quiz` 整个模块目录
- 修改 `settings.gradle.kts` 移除 `:feature:quiz`
- 修改 `app/build.gradle.kts` 移除 `:feature:quiz` 依赖
- 删除 `feature/quiz/src/test/` 测试文件

### Phase 3: 论述题 Tab 增强（可选）
- Essay Tab 添加默认排序/筛选状态
- 添加"最近浏览"或"高频考点"入口
- 添加论述题统计信息（已练习/未练习数量）

---

## 10. 关键设计决策记录

### ADR-20260801-001: 为什么保留 Quiz 模块不删除

**决策：** 保留 `feature:quiz` 模块及其所有文件。

**理由：**
1. WrongAnswerScreen 代码在此模块中，迁移需要额外工作
2. 删除模块可能导致 CI 或测试引用问题
3. 优先保证功能正确性，模块清理可后续进行
4. 死代码（QuizScreen/QuizViewModel）不占用运行内存，仅增加 APK 体积（可忽略）

**替代方案：** 立即将 WrongAnswerScreen 移出 + 删除 quiz 模块 → 风险高，收益小

### ADR-20260801-002: Essay 图标选择

**决策：** 使用 `Icons.AutoMirrored.Filled.MenuBook`

**理由：**
1. 与当前 EssayEntryCard 的图标一致，用户已熟悉
2. MenuBook 语义为"学习/教材"，契合论述题练习场景
3. 与"知识点"(AutoStories) 形成视觉区分
4. AutoMirrored 兼容 RTL 布局

### ADR-20260801-003: EssayListScreen onBack 改为 nullable 而非默认空 lambda

**决策：** `onBack: (() -> Unit)? = null`

**理由：**
1. `WenyanLargeTopAppBar` 的 onBack 已是 `(() -> Unit)? = null`，天然兼容
2. nullable 语义清晰：null = 顶级模式，非 null = 子路由模式
3. 避免空 lambda 导致 TopAppBar 显示不可点击的返回箭头

### ADR-20260801-004: 迁移批次策略

**决策：** 分 3 批独立验证，而非一次性全部改动。

**理由：**
1. 每批可独立编译验证，快速定位问题批次
2. 回滚粒度更细，避免全军覆没
3. Batch 1（Tab 替换）是核心改动，Batch 2/3 是连带清理

---

## 11. 执行计划（迁移脚本）

当准备执行时，按以下顺序操作：

```
Batch 1: Tab 替换
  Step 1: TopLevelDestination.kt — 替换 Quiz → Essay（2 分钟）
  验证: :app:compileDebugKotlin

Batch 2: 导航层
  Step 2: EssayListScreen.kt — onBack nullable（1 分钟）
  Step 3: WenyanNavHost.kt — 替换导航注册（5 分钟）
  Step 5: WenyanNavHost.kt — knowledgeDestination 参数清理（1 分钟）
  验证: :app:assembleDebug

Batch 3: UI 清理
  Step 4: KnowledgeScreen.kt — 删除 EssayEntryCard（3 分钟）
  Step 6: AboutTutorialScreen.kt — 同步功能描述（2 分钟）
  验证: :app:assembleDebug

Final:
  Step 7: Completion Evidence 检查 — Grep 4 项 0 结果（1 分钟）
  Step 8: testDebugUnitTest 全模块验证（3 分钟）
  Step 9: emulator 实测 10 项功能（12 分钟）
-------------------------------------------
总计：约 30 分钟
```