# 真题 → 论述题 Tab 迁移计划

> 版本：v1.0
> 状态：计划阶段（不执行）
> 设计依据：docs/design/essay-module-design.md

## 0. 现状分析

### 0.1 当前底部导航结构（5 Tab）

```
知识点 | 真题 | 卡片 | 错题本 | 设置
```

### 0.2 目标底部导航结构（5 Tab）

```
知识点 | 论述题 | 卡片 | 错题本 | 设置
```

### 0.3 相关模块文件索引

| 文件 | 当前角色 | 迁移后的角色 |
|------|----------|-------------|
| `app/.../navigation/TopLevelDestination.kt` | 定义 5 Tab 路由/标签/图标 | 替换 Quiz → Essay |
| `app/.../navigation/WenyanNavHost.kt` | 注册所有 destinations | 替换 quizDestination → essayTabDestination |
| `feature/quiz/.../QuizScreen.kt` | 真题列表（年份筛选+答题+自评） | 删除（不再需要） |
| `feature/quiz/.../QuizViewModel.kt` | 真题状态管理 | 删除 |
| `feature/quiz/.../WrongAnswerScreen.kt` | 错题本 Tab（代码在 quiz 模块中） | 保留，需迁移出 quiz 模块 |
| `feature/quiz/.../WrongAnswerViewModel.kt` | 错题本 ViewModel | 保留，需迁移 |
| `feature/knowledge/.../EssayListScreen.kt` | 论述题列表（子路由，有 onBack） | 提升为顶级 Tab，onBack 改为可选 |
| `feature/knowledge/.../EssayListViewModel.kt` | 论述题列表状态管理 | 不变（良好） |
| `feature/knowledge/.../EssayDetailScreen.kt` | 论述题详情（子路由） | 不变（良好） |
| `feature/knowledge/.../EssayDetailViewModel.kt` | 论述题详情状态管理 | 不变（良好） |
| `feature/knowledge/.../EssayDetailModels.kt` | 数据模型 + JSON 解析 | 不变（良好） |
| `feature/knowledge/.../KnowledgeScreen.kt` | 知识点列表（含 EssayEntryCard） | 移除 EssayEntryCard + onNavigateToEssays |

### 0.4 数据流分析

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

### 0.5 依赖关系分析

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

## 1. 方案对比

### 方案 A：最小改动（推荐）

只改导航层，不移动/删除模块。

**改动范围：** 4 个文件
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

## 2. 方案 A 详细步骤（推荐，最小改动）

### Step 1: 修改 `TopLevelDestination.kt`

**改动：**
- 删除 `data object Quiz` 
- 新增 `data object Essay`（route = "essay", label = "论述题", icon = Icons.AutoMirrored.Filled.MenuBook）
- `companion object` 中：删除 `ROUTE_QUIZ`，新增 `ROUTE_ESSAY = "essay"`
- `destinations` 列表：将 `Quiz` 替换为 `Essay`（保持顺序：Knowledge → Essay → Cards → WrongAnswer → Settings）

**注意：** 图标选择 `Icons.AutoMirrored.Filled.MenuBook`（与当前 EssayEntryCard 一致，用户已熟悉）。

### Step 2: 修改 `EssayListScreen.kt` — 使 `onBack` 可选

**当前位置：** `feature/knowledge/src/main/java/.../EssayListScreen.kt`

**改动：**
- `onBack: () -> Unit = {}`（默认空实现，顶级 Tab 无返回按钮）
- 在 TopAppBar 中：仅在 `onBack` 非空时显示返回箭头（或检查调用者是否传了 onBack）

**设计决策：** 顶级 Tab 应有"返回"按钮吗？
- 当前错题本 Tab（WrongAnswerScreen）在顶级模式下无返回箭头
- 论述题作为顶级 Tab 也不应有返回箭头
- 但知识点详情跳转论述题时，仍需要返回按钮 → 这种情况走的是 `essayDetailDestination` 子路由，不受影响
- 顶级 Tab 的 EssayListScreen 不应显示返回箭头

**具体实现：**
```kotlin
// EssayListScreen 签名
@Composable
fun EssayListScreen(
    onBack: () -> Unit = {},
    onNavigateToEssayDetail: (String) -> Unit = {},
    viewModel: EssayListViewModel = hiltViewModel(),
)
```

TopAppBar 中：
```kotlin
WenyanLargeTopAppBar(
    title = "论述题",
    // 顶级 Tab 模式：不传 onBack 或传 {}
    // 此时 WenyanLargeTopAppBar 不显示返回箭头
    ...
)
```

**检查 WenyanLargeTopAppBar 的 onBack 逻辑：** 如果 onBack 默认值为 `{}`（非空 lambda），TopAppBar 仍会显示返回箭头。需要确认 `WenyanLargeTopAppBar` 是否检查 `onBack` 是否为 `{}` 或 null。

**解决方案：** 将 `onBack` 改为 nullable：
```kotlin
@Composable
fun EssayListScreen(
    onBack: (() -> Unit)? = null,  // null = 顶级模式，无返回箭头
    onNavigateToEssayDetail: (String) -> Unit = {},
    viewModel: EssayListViewModel = hiltViewModel(),
)
```

**检查 WenyanLargeTopAppBar 的签名：** 确认它是否支持 nullable onBack。如果不支持，要么修改它，要么在 EssayListScreen 中根据 onBack != null 条件式渲染返回箭头。

### Step 3: 修改 `WenyanNavHost.kt`

**改动：**
1. 删除 `quizDestination()` 调用
2. 新增 `essayTabDestination()` — 顶级 Tab 版本的 essay 列表
3. 删除 `essayListDestination()`（子路由版本不再需要，因为顶级 Tab 直接走 essay 路由）
4. 更新注释

**新增的 `essayTabDestination()`：**
```kotlin
private fun NavGraphBuilder.essayTabDestination(
    onNavigateToEssayDetail: (String) -> Unit,
) {
    composable(TopLevelDestination.ROUTE_ESSAY) {
        EssayListScreen(
            onBack = null,  // 顶级模式，无返回箭头
            onNavigateToEssayDetail = onNavigateToEssayDetail,
        )
    }
}
```

**WenyanNavHost 中调用：**
```kotlin
// 替换：
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

**删除 `essayListDestination()`：** 不再需要，因为：
- 知识点 Tab 不再有 EssayEntryCard（见 Step 4）
- 顶级 Tab 直接走 `essayTabDestination`
- 唯一的子路由入口是 `essayDetailDestination`（论述题详情）

### Step 4: 修改 `KnowledgeScreen.kt` — 移除 EssayEntryCard

**改动：**
- 删除 `onNavigateToEssays: () -> Unit` 参数
- 删除 `KnowledgeList` 中的 `EssayEntryCard` item
- 删除 `EssayEntryCard` Composable 函数
- 更新 `WenyanNavHost` 中 `knowledgeDestination` 的调用（移除 `onNavigateToEssays` 参数）

**理由：** 论述题已有独立 Tab，不再需要从知识点列表顶部进入。

### Step 5: 更新 `WenyanNavHost` 中 `knowledgeDestination` 调用

**改动：**
- 删除 `onNavigateToEssays` lambda
- 更新 `knowledgeDestination` 注册函数签名（删除 `onNavigateToEssays` 参数）

---

## 3. 方案 A 文件改动清单

| # | 文件 | 改动类型 | 说明 |
|---|------|---------|------|
| 1 | `app/.../navigation/TopLevelDestination.kt` | 修改 | Quiz → Essay 替换 |
| 2 | `app/.../navigation/WenyanNavHost.kt` | 修改 | 替换 quizDestination + 删除 essayListDestination + 新增 essayTabDestination |
| 3 | `feature/knowledge/.../EssayListScreen.kt` | 修改 | onBack 改为 nullable |
| 4 | `feature/knowledge/.../KnowledgeScreen.kt` | 修改 | 删除 EssayEntryCard + onNavigateToEssays |
| 5 | `feature/knowledge/.../KnowledgeScreen.kt` | 删除 | EssayEntryCard Composable 函数 |

**总计：** 4 个文件修改，零新增文件，零删除文件。

---

## 4. 不修改的文件（已验证无影响）

| 文件 | 不修改的理由 |
|------|-------------|
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
| `app/build.gradle.kts` | 保留（`:feature:quiz` 依赖不删除） |
| `core/data/.../KnowledgeRepository.kt` | 数据层完全独立 |
| `core/database/.../ExamQuestionDao.kt` | 数据层完全独立 |

---

## 5. 风险评估

### 5.1 风险矩阵

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| EssayListScreen 的 onBack 改为 nullable 后 WenyanLargeTopAppBar 不兼容 | 中 | 高 | 检查 WenyanLargeTopAppBar 签名，如不支持 nullable 则需适配 |
| 知识点 Tab → 论述题详情子路由导航中断 | 低 | 高 | 验证 KnowledgePointDetailScreen 的 onNavigateToEssay 仍正常 |
| 错题本 Tab 因 quiz 模块保留而正常 | 低 | 高 | 不删除 quiz 模块，错题本完全不受影响 |
| 测试因 Quiz 路由删除而失败 | 中 | 中 | 更新/删除相关测试 |

### 5.2 回滚方案

如果迁移后出现问题，只需：
1. 恢复 `TopLevelDestination.kt` 的 `Quiz` data object
2. 恢复 `WenyanNavHost.kt` 的 `quizDestination()` 调用
3. 恢复 `KnowledgeScreen.kt` 的 `EssayEntryCard`
4. 恢复 `EssayListScreen.kt` 的 `onBack` 签名

---

## 6. 验证清单（执行前逐项检查）

### 编译检查
- [ ] `:app:assembleDebug` BUILD SUCCESSFUL
- [ ] 全模块 `testDebugUnitTest` 全绿

### 功能验证（需 emulator 实测）
- [ ] 底部导航栏显示"论述题"而非"真题"
- [ ] 点击"论述题"Tab → 展示论述题列表（无返回箭头）
- [ ] 论述题列表页三维筛选正常工作（年份/科目/onlyWithAngle）
- [ ] 点击论述题 → 详情页渲染（11 区块结构）
- [ ] 详情页 AI 审题助手正常工作
- [ ] 知识点 Tab → 知识点详情 → 相关论述题 → 跳转正常
- [ ] 知识点 Tab 列表顶部不再显示 EssayEntryCard
- [ ] 错题本 Tab 完全正常
- [ ] "真题"相关数据不丢失（数据库中有分类，只是 UI 入口移除）

---

## 7. 后续优化（方案 B — 暂不执行）

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

## 8. 关键设计决策记录

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
1. `WenyanLargeTopAppBar` 的 onBack 逻辑通常检查 null 而非空 lambda
2. nullable 语义清晰：null = 顶级模式，非 null = 子路由模式
3. 避免空 lambda 导致 TopAppBar 显示不可点击的返回箭头

---

## 9. 执行计划（迁移脚本）

当准备执行时，按以下顺序操作：

```
Step 1: TopLevelDestination.kt — 替换 Quiz → Essay（2 分钟）
Step 2: EssayListScreen.kt — onBack nullable（1 分钟）
Step 3: WenyanNavHost.kt — 替换导航注册（5 分钟）
Step 4: KnowledgeScreen.kt — 删除 EssayEntryCard（3 分钟）
Step 5: assembleDebug 验证（2 分钟）
Step 6: testDebugUnitTest 验证（3 分钟）
Step 7: emulator 实测 8 项功能（10 分钟）
-------------------------------------------
总计：约 26 分钟
```