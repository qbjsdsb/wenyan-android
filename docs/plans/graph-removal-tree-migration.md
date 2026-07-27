# 任务清单：移除知识图谱 UI + 引入章节树与关联知识点模块

- **关联 ADR**: [docs/design/adr-001-graph-removal.md](../design/adr-001-graph-removal.md)
- **Specialist**: migration-and-deprecation（staff-engineer-mode 2.1.0）
- **创建日期**: 2026-07-27
- **执行原则**: expand/contract（并行变更）— 先添加新路径，验证等价，再移除旧路径
- **回滚策略**: 每个 Batch 独立 commit，`git revert <commit>` 即可回滚该批次

---

## 0. 使用清单（Usage Inventory）

### 0.1 静态使用（已测量）

| 资源 | 类型 | 引用方 | 处置 |
|------|------|--------|------|
| `feature:graph` Gradle 模块 | 模块 | `settings.gradle.kts:50` `include(":feature:graph")` | **删除** |
| `:feature:graph` 依赖 | 依赖 | `app/build.gradle.kts:121` `implementation(project(":feature:graph"))` | **删除** |
| `TopLevelDestination.Graph` | sealed object | `app/navigation/TopLevelDestination.kt:49-53` + `destinations` 列表 `:74` | **删除** |
| `TopLevelDestination.ROUTE_GRAPH` | 常量 | `app/navigation/TopLevelDestination.kt:66` | **删除** |
| `NavGraphBuilder.graphDestination()` | 扩展函数 | `app/navigation/WenyanNavHost.kt:219-235` | **删除** |
| `graphDestination(...)` 调用 | 调用点 | `app/navigation/WenyanNavHost.kt:124` | **删除** |
| `feature/graph/consumer-rules.pro` | ProGuard 规则 | Hilt ViewModel keep | **删除** |
| `feature/graph/src/main/java/.../GraphScreen.kt` | UI | 仅被 `graphDestination` 引用 | **删除** |
| `feature/graph/src/main/java/.../GraphViewModel.kt` | ViewModel | 仅被 GraphScreen 引用 | **删除** |
| `feature/graph/src/main/java/.../ui/GraphCanvas.kt` | UI | 仅被 GraphScreen 引用 | **删除** |
| `feature/graph/src/main/java/.../ui/GraphConstants.kt` | 常量 | 仅被 GraphCanvas/GraphLayout 引用 | **删除** |
| `feature/graph/src/main/java/.../ui/GraphLayout.kt` | 算法 | 仅被 GraphViewModel 引用 | **删除** |
| `feature/graph/src/test/java/.../*` | 测试 | 3 文件（Fakes, GraphViewModelTest, GraphLayoutTest） | **删除** |
| `core/data/repository/GraphRepository.kt` | 接口 | `DataModule` + 算法服务 | **保留** |
| `core/data/repository/GraphRepositoryImpl.kt` | 实现 | `DataModule` 绑定 | **保留** |
| `core/data/graph/InterferenceWarner.kt` | 算法 | `SchedulingRepository` 链路 | **保留** |
| `core/data/graph/WeakSubgraphDetector.kt` | 算法 | `SchedulingRepository` 链路 | **保留** |
| `core/data/graph/PrerequisiteChecker.kt` | 算法 | `SchedulingRepository` 链路 | **保留** |
| `core/data/seed/GraphSkeleton.kt` | 数据 | `SeedDataLoader` | **保留** |
| `core/database/dao/GraphNodeDao.kt` | DAO | `GraphRepositoryImpl` | **保留** |
| `core/database/entity/GraphNodeEntity.kt` | Entity | `GraphNodeDao` + `SeedDataLoader` | **保留** |
| `core/data/consumer-rules.pro:22` | ProGuard | `-keep class com.wenyan.app.core.data.graph.GraphSkeleton` | **修正路径** → `core.data.seed.GraphSkeleton` |

### 0.2 运行时使用（已测量）

- **DB 写入**: `SeedDataLoader.importGraphNodeEntities()` 仍会向 `graph_nodes` 表写入节点（每个去重实体一个）— **保留**，算法服务消费
- **DB 读取**: `GraphRepositoryImpl` 通过 `GraphNodeDao` 读取 — **保留**
- **UI 入口**: 底部导航第 4 个 Tab "图谱" — **移除**（替换为"错题本"）
- **导航调用**: `WenyanNavHost` 启动时 `graphDestination(...)` 注册 — **移除**

### 0.3 盲点（Blind Spots）

- **无运行时遥测**：App 未接入 analytics，无法量化"图谱 Tab 实际点击率"。假设低使用率基于 F1（场景错配）+ 用户明确要求移除
- **无外部客户端**：App 是单仓库单 App，无外部 SDK 消费者

---

## 1. 依赖分类与迁移批次（Dependent Classification & Migration Batches）

| 批次 | 名称 | 类型 | 风险 | 可回滚 | 关联 commit |
|------|------|------|------|--------|-------------|
| **B1** | 章节树数据层（expand） | 新增 | 低 | 是（删除新文件） | 1 个 commit |
| **B2** | 关联知识点模块增强（expand） | 新增 | 低 | 是（revert UI 改动） | 1 个 commit |
| **B3** | 错题本升级为顶级目的地（expand） | 新增 | 中（导航改动） | 是（恢复子路由） | 1 个 commit |
| **B4** | 移除 feature:graph 模块（contract） | 删除 | 中（构建配置） | 是（git revert） | 1 个 commit |
| **B5** | ProGuard 规则修复 + 文档更新 | 修复 | 低 | 是 | 1 个 commit |

**执行顺序**：B1 → B2 → B3 → **验证门 G1**（emulator 实测新功能） → B4 → **验证门 G2**（构建全绿） → B5 → **验证门 G3**（Release 准入）

---

## 2. 详细任务（Tasks）

### Batch B1：章节树数据层（expand）

> 目标：让 `ChapterEntity` 真正支持树状层级，为 UI 提供数据基础。

#### B1.1 扩展 `ChapterDao` 树状查询方法

- **文件**: `core/database/src/main/java/com/wenyan/app/core/database/dao/ChapterDao.kt`
- **新增方法**:
  ```kotlin
  @Query("WITH RECURSIVE tree AS (SELECT * FROM chapters WHERE id = :rootId UNION ALL SELECT c.* FROM chapters c JOIN tree t ON c.parent_id = t.id) SELECT * FROM tree ORDER BY sort_order ASC")
  fun observeTree(rootId: String): Flow<List<ChapterEntity>>

  @Query("SELECT COUNT(*) FROM chapters WHERE parent_id IS NOT NULL")
  suspend fun countNonRootChapters(): Int
  ```
- **验收**: 编译通过 + 新增单元测试 `ChapterDaoTest`（用 Room in-memory DB 验证树状查询）

#### B1.2 新增 `ChapterRepository` 接口与实现

- **文件**: `core/data/src/main/java/com/wenyan/app/core/data/repository/ChapterRepository.kt`（新增）
- **文件**: `core/data/src/main/java/com/wenyan/app/core/data/repository/ChapterRepositoryImpl.kt`（新增）
- **职责**:
  - `observeSubjects(): Flow<List<SubjectEntity>>`
  - `observeRootChapters(subjectId: String): Flow<List<ChapterEntity>>`
  - `observeChildren(parentId: String): Flow<List<ChapterEntity>>`
  - `observeTree(rootId: String): Flow<List<ChapterEntity>>` — 递归 CTE
  - `observeKnowledgePointsByChapter(chapterId: String): Flow<List<KnowledgePointEntity>>`
- **DI**: 在 `DataModule.kt` 注册 `@Binds`
- **验收**: `ChapterRepositoryTest` 全绿

#### B1.3 扩展 `SeedDataLoader` 生成章节树

- **文件**: `core/data/src/main/java/com/wenyan/app/core/data/seed/SeedDataLoader.kt`
- **改动**:
  - 当前 `defaultChapters` 只为每科创建一个 `parentId=null` 的扁平章节
  - 新增逻辑：基于 `KnowledgePointEntity.tags`（如 "先秦/秦汉/魏晋..."）自动生成二级章节树
  - 章节树结构：`subject` → `default_chapter`（保留兼容）→ `chapter_<tag>`（新增子章节，parentId 指向 default_chapter）
  - 将 `KnowledgePointEntity.chapterId` 重新指向最具体的子章节
- **seed 版本**: `seedVersion` 2.11.0 → 2.12.0（触发重新导入）
- **验收**: `SeedDataLoaderTest` 验证章节树生成 + 至少一科有 `parent_id IS NOT NULL` 子章节

#### B1.4 验证门 G0（B1 内部）

- **命令**: `gradle :core:data:testDebugUnitTest :core:database:testDebugUnitTest --no-daemon`
- **通过条件**: 全绿 + `countNonRootChapters() > 0` 测试通过
- **回滚**: `git revert <B1 commit>`

---

### Batch B2：关联知识点模块增强（expand）

> 目标：让 `RelatedPointsSection` 从"列表"升级为"关联模块"，提供关系类型视觉编码。

#### B2.1 设计关系类型视觉编码

- **关系类型**:
  - **关联**（related）— 图标 `Icons.Filled.Link`，颜色 `primary`
  - **对比**（contrast）— 图标 `Icons.Filled.CompareArrows`，颜色 `tertiary`
  - **延伸**（extension）— 图标 `Icons.Filled.CallMade`，颜色 `secondary`
- **文件**: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt`
- **改动位置**: `RelatedPointsSection` + `RelatedGroup` 私有 Composable

#### B2.2 重构 `RelatedGroup` 增加关系类型 header

- **当前**: `RelatedGroup(title: String, points: List<KnowledgePointEntity>, onNavigateToDetail: (String) -> Unit)`
- **新增参数**: `relationType: RelationType`（枚举：RELATED / CONTRAST / EXTENSION）
- **UI 变化**:
  - Header 行：关系图标 + 关系名称 + 计数 chip
  - 每个知识点项：标题 + 考频 chip + 难度 chip + 右箭头
  - 点击整行 → `onNavigateToDetail(point.id)`
- **无障碍**: `semantics { contentDescription = "关联知识点：${point.title}，考频${freq}，难度${diff}" }`

#### B2.3 添加关系类型预览

- **文件**: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt`
- **新增**: `@Preview` 三种关系类型的 Composable Preview（light/dark 主题）
- **验收**: Preview 可渲染

#### B2.4 验证门 G0（B2 内部）

- **命令**: `gradle :feature:knowledge:testDebugUnitTest :feature:knowledge:compileDebugKotlin --no-daemon`
- **通过条件**: 编译全绿 + 现有 `KnowledgePointDetailViewModelTest` 不退化
- **回滚**: `git revert <B2 commit>`

---

### Batch B3：错题本升级为顶级目的地（expand）

> 目标：将 `WrongAnswerScreen` 从 `quiz` 子路由提升为底部导航第 4 个 Tab。

#### B3.1 修改 `TopLevelDestination`

- **文件**: `app/src/main/java/com/wenyan/app/navigation/TopLevelDestination.kt`
- **改动**:
  - 删除 `Graph` data object（保留为注释或直接删，由 B4 处理模块删除）
  - 新增 `WrongAnswer` data object:
    ```kotlin
    data object WrongAnswer : TopLevelDestination(
        route = ROUTE_WRONG_ANSWER,
        label = "错题本",
        icon = Icons.Filled.CancelPresentation, // 或 Icons.Filled.ErrorOutline
    )
    ```
  - `destinations` 列表：`[Knowledge, Quiz, Cards, WrongAnswer, Settings]`
  - 新增常量 `const val ROUTE_WRONG_ANSWER_TOP = "wrong_answer_top"`（与子路由 `ROUTE_WRONG_ANSWER = "wrong_answer"` 区分，避免冲突）

#### B3.2 修改 `WenyanNavHost`

- **文件**: `app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`
- **改动**:
  - 删除 `graphDestination(...)` 调用（第 124 行）— B4 会删除函数定义
  - 将 `wrongAnswerDestination` 改为顶级目的地 composable（无 onBack，因为顶级 Tab 不需要返回）
  - 保留原 `ROUTE_WRONG_ANSWER` 子路由（从 QuizScreen 入口仍可进入，双入口兼容）
  - 或：移除 QuizScreen TopBar 的错题本 IconButton（避免双入口混淆）— **推荐移除**，统一为顶级入口

#### B3.3 修改 `WrongAnswerScreen` 支持顶级模式

- **文件**: `feature/quiz/src/main/java/com/wenyan/app/feature/quiz/WrongAnswerScreen.kt`
- **改动**:
  - `onBack` 参数改为可选 `onBack: (() -> Unit)? = null`
  - TopBar：当 `onBack == null` 时，不显示返回箭头（顶级 Tab 模式）
  - 标题保持"错题本"

#### B3.4 修改 `QuizScreen` 移除错题本入口

- **文件**: `feature/quiz/src/main/java/com/wenyan/app/feature/quiz/QuizScreen.kt`
- **改动**:
  - 删除 `onNavigateToWrongAnswer: () -> Unit` 参数
  - 删除第 116 行 `IconButton(onClick = onNavigateToWrongAnswer)` 
  - 保留 `onNavigateToAiAssistant` 入口

#### B3.5 验证门 G1（emulator 实测）

- **实测项**:
  1. App 启动后底部导航显示 5 个 Tab：知识点 / 真题 / 卡片 / 错题本 / 设置
  2. 点击"错题本" Tab → 直接显示 `WrongAnswerScreen`（无返回箭头）
  3. 错题本数据未丢失（之前答错的题目仍在）
  4. 真题 Tab TopBar 不再有错题本图标
  5. AI 助手入口在真题 Tab TopBar 仍可用
- **回滚**: `git revert <B3 commit>`

---

### Batch B4：移除 feature:graph 模块（contract）

> 目标：删除 `feature:graph` Gradle 模块及其所有引用。
> **前置条件**: G1 通过（错题本已替代图谱位置）

#### B4.1 删除 `app` 模块对 `:feature:graph` 的依赖

- **文件**: `app/build.gradle.kts`
- **改动**: 删除第 121 行 `implementation(project(":feature:graph"))`

#### B4.2 删除 `settings.gradle.kts` 的 include

- **文件**: `settings.gradle.kts`
- **改动**: 删除第 50 行 `include(":feature:graph")`

#### B4.3 删除 `WenyanNavHost.graphDestination` 函数定义

- **文件**: `app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt`
- **改动**: 删除第 219-235 行 `private fun NavGraphBuilder.graphDestination(...)` 整个函数
- **改动**: 删除第 124 行 `graphDestination(...)` 调用（若 B3.2 未删）

#### B4.4 删除 `TopLevelDestination.Graph` 与 `ROUTE_GRAPH`

- **文件**: `app/src/main/java/com/wenyan/app/navigation/TopLevelDestination.kt`
- **改动**:
  - 删除 `data object Graph : TopLevelDestination(...)`（第 49-53 行）
  - 删除 `const val ROUTE_GRAPH = "graph"`（第 66 行）
  - 删除 `destinations` 列表中的 `Graph` 项（第 74 行）
  - 删除 `import androidx.compose.material.icons.filled.Hub`（第 5 行）

#### B4.5 删除 `feature/graph/` 整个目录

- **命令**: `rm -rf feature/graph/`
- **包含**:
  - `build.gradle.kts`
  - `consumer-rules.pro`
  - `src/main/java/com/wenyan/app/feature/graph/GraphScreen.kt`
  - `src/main/java/com/wenyan/app/feature/graph/GraphViewModel.kt`
  - `src/main/java/com/wenyan/app/feature/graph/ui/GraphCanvas.kt`
  - `src/main/java/com/wenyan/app/feature/graph/ui/GraphConstants.kt`
  - `src/main/java/com/wenyan/app/feature/graph/ui/GraphLayout.kt`
  - `src/test/java/com/wenyan/app/feature/graph/Fakes.kt`
  - `src/test/java/com/wenyan/app/feature/graph/GraphViewModelTest.kt`
  - `src/test/java/com/wenyan/app/feature/graph/ui/GraphLayoutTest.kt`

#### B4.6 验证门 G2（构建全绿）

- **命令**:
  ```bash
  unset CI
  gradle :app:assembleDebug :app:assembleRelease testDebugUnitTest --no-daemon
  ```
- **通过条件**:
  - `assembleDebug` SUCCESSFUL
  - `assembleRelease` SUCCESSFUL
  - `testDebugUnitTest` 全绿（预期 ~440 tests，相比 v0.8.18 的 450 减少 ~10 个 graph 测试）
  - 静态搜索 `grep -r "feature:graph\|ROUTE_GRAPH\|TopLevelDestination.Graph" --include="*.kt" --include="*.kts"` 返回空
- **回滚**: `git revert <B4 commit>`

---

### Batch B5：ProGuard 规则修复 + 文档更新

#### B5.1 修复 `GraphSkeleton` keep 规则路径

- **文件**: `core/data/consumer-rules.pro`
- **改动**:
  ```diff
  --keep class com.wenyan.app.core.data.graph.GraphSkeleton { *; }
  --keep class com.wenyan.app.core.data.graph.GraphSkeleton$* { *; }
  +-keep class com.wenyan.app.core.data.seed.GraphSkeleton { *; }
  +-keep class com.wenyan.app.core.data.seed.GraphSkeleton$* { *; }
  ```
- **验收**: grep 验证路径正确

#### B5.2 更新 `AGENTS.md` 第 7 节当前状态

- **文件**: `AGENTS.md`
- **改动**: 新增 v0.8.19 条目（或下一个版本号），记录"移除知识图谱 UI + 引入章节树 + 错题本升级"

#### B5.3 更新 `docs/00-STATUS.md`

- **文件**: `docs/00-STATUS.md`
- **改动**: 同步状态快照

#### B5.4 更新 `docs/SESSION_LOG.md`

- **文件**: `docs/SESSION_LOG.md`
- **改动**: 追加本次会话日志（含 ADR-001 链接 + 任务清单链接 + 执行结果）

#### B5.5 验证门 G3（Release 准入）

- **通过条件**:
  - G0 + G1 + G2 全部通过
  - `git status` 干净（所有改动已 commit）
  - `git log --oneline -6` 显示 5 个 batch commit + 1 个 release commit
  - Emulator 实测三模式（章节树 + 关联模块 + 错题本）无回归
- **Release 流程**（按 AGENTS.md 第 4 节硬约束）:
  1. `assembleDebug` + `testDebugUnitTest` 本地全绿
  2. 删除旧 orphan tag（如有）
  3. `git tag vX.Y.Z && git push origin vX.Y.Z`
  4. 等 Release workflow 完成（CI 账单问题则本地构建 + gh 上传）

---

## 3. 迁移完成证据（Migration Completion Evidence）

### 3.1 旧/新混合状态检查

| 检查项 | 粒度 | 验证方法 | 期望结果 |
|--------|------|----------|----------|
| `feature:graph` 模块引用 | 仓库级 | `grep -r "feature:graph" --include="*.kts"` | 空 |
| `TopLevelDestination.Graph` 引用 | 仓库级 | `grep -r "TopLevelDestination.Graph\|ROUTE_GRAPH" --include="*.kt"` | 空 |
| `GraphScreen` / `GraphViewModel` 引用 | 仓库级 | `grep -r "GraphScreen\|GraphViewModel" --include="*.kt"` | 空（除注释） |
| `WrongAnswer` 顶级目的地 | 仓库级 | `grep -r "TopLevelDestination.WrongAnswer" --include="*.kt"` | ≥ 1 |
| 章节树数据 | DB 级 | `SELECT COUNT(*) FROM chapters WHERE parent_id IS NOT NULL` | > 0 |
| 关联模块视觉编码 | UI 级 | Emulator 截图 | 3 种关系类型可见 |
| GraphRepository 保留 | 编译级 | `compileDebugKotlin` | 成功 |
| GraphSkeleton ProGuard 路径 | 配置级 | `grep "GraphSkeleton" core/data/consumer-rules.pro` | 路径 = `seed.GraphSkeleton` |

### 3.2 per-consumer 完成证据

- **app 模块**: 不再依赖 `:feature:graph`（`app/build.gradle.kts` grep 空）
- **导航**: `WenyanNavHost` 不再注册 `graphDestination`，已注册 `wrongAnswerTopDestination`
- **数据层**: `core/data` 的 `GraphRepository` 及算法服务编译存在且测试全绿
- **DB**: `graph_nodes` 表 schema 不变（算法服务仍消费），`chapters` 表有子章节

---

## 4. 迁移指南与逃生通道（Migration Guide & Escape Hatch）

### 4.1 用户侧迁移指南

- **图谱 Tab 消失**: 用户原底部导航第 4 个 Tab "图谱" 已替换为"错题本"。图谱的全局视图能力由两处替代：
  1. **章节树**（知识点 Tab 内）：按科目 → 章节 → 子章节层级浏览
  2. **关联知识点模块**（知识点详情页）：在具体知识点上下文中查看关联/对比/延伸
- **错题本入口**: 从"真题 → TopBar 图标"两跳，改为底部导航一跳直达
- **数据无丢失**: 错题本数据由 Room 持久化，导航改动不影响 DB

### 4.2 开发者侧逃生通道

- **回滚单批次**: `git revert <batch-commit>`
- **完整回滚**: `git revert <B5> <B4> <B3> <B2> <B1>`（逆序回滚）
- **应急回滚**（若 Release 后发现 Blocker）:
  1. `git revert <release-commit>` 撤销 Release tag 指向
  2. 重新发布旧版本 APK
  3. 在 GitHub Release 说明中标注"vX.Y.Z 已撤回，请使用 vX.Y.Z-1"

### 4.3 兼容性垫片（Compatibility Shim）

- **DB schema 不变**: `graph_nodes` 表保留，无需迁移
- **seed 版本升级**: 2.11.0 → 2.12.0 触发章节树重新生成（旧 default_chapter 保留，新增子章节）
- **导航路由**: `ROUTE_WRONG_ANSWER = "wrong_answer"` 子路由保留（向后兼容深链接），新增 `ROUTE_WRONG_ANSWER_TOP` 顶级路由

---

## 5. 防止回潮（Backsliding Prevention）

| 控制项 | 实现方式 |
|--------|----------|
| **构建阻断** | `settings.gradle.kts` 不再 `include(":feature:graph")`，任何尝试恢复都会触发 Gradle 解析失败 |
| **静态检查** | CI 可选添加 grep 检查：`grep -r "feature:graph" --include="*.kts"` 失败 build |
| **代码审查** | PR 模板 checklist 新增："未引入对已移除模块的引用" |
| **文档** | ADR-001 + 本任务清单 + AGENTS.md 第 7 节均记录移除决策 |
| **ProGuard** | `feature/graph/consumer-rules.pro` 已删除，无法被 R8 引用 |

---

## 6. 禁用前删除（Disable Before Delete）— Watch Window

### 6.1 B4 删除前观察窗口

- **前置条件**: B1-B3 已合并，G1 emulator 实测通过
- **观察期**: 1 个本地构建周期（assembleDebug + assembleRelease + testDebugUnitTest）
- **检查项**:
  - 无编译错误（特别是 Hilt DI 图，因为 `GraphViewModel` 是 `@HiltViewModel`）
  - 无运行时崩溃（特别是导航到原"图谱" Tab 的位置现在是"错题本"）
  - 测试通过率不下降（除移除的 graph 测试外）
- **逃生通道**: 若观察期内发现问题，`git revert <B3>` 回滚错题本升级，图谱 Tab 恢复

### 6.2 B4 删除后观察窗口

- **观察期**: 1 个 emulator 实测周期
- **检查项**:
  - App 启动正常（无 Hilt DI 图崩溃）
  - 底部导航 5 Tab 正常
  - 错题本功能正常
  - 知识点详情页关联模块正常
  - 章节树数据正确
- **隐藏依赖检查**: 静态搜索 `grep -r "GraphScreen\|GraphViewModel\|feature.graph" --include="*.kt" --include="*.kts"` 返回空

---

## 7. 最终退役清单（Final Retirement Checklist）

| # | 项 | 状态 | 处置 |
|---|----|------|------|
| 1 | `feature:graph/` 目录 | 待删 | `rm -rf` |
| 2 | `settings.gradle.kts` include | 待删 | 编辑 |
| 3 | `app/build.gradle.kts` 依赖 | 待删 | 编辑 |
| 4 | `TopLevelDestination.Graph` | 待删 | 编辑 |
| 5 | `WenyanNavHost.graphDestination` | 待删 | 编辑 |
| 6 | `feature/graph/consumer-rules.pro` | 待删 | 随目录删除 |
| 7 | `core/data/consumer-rules.pro` GraphSkeleton 路径 | 待修 | 编辑 |
| 8 | `core/data/repository/GraphRepository*` | **保留** | 不动 |
| 9 | `core/data/graph/*` 算法服务 | **保留** | 不动 |
| 10 | `core/data/seed/GraphSkeleton.kt` | **保留** | 不动 |
| 11 | `core/database/dao/GraphNodeDao.kt` | **保留** | 不动 |
| 12 | `core/database/entity/GraphNodeEntity.kt` | **保留** | 不动 |
| 13 | `DataModule.kt` GraphRepository 绑定 | **保留** | 不动 |
| 14 | `AGENTS.md` 状态更新 | 待更 | 编辑 |
| 15 | `docs/00-STATUS.md` | 待更 | 编辑 |
| 16 | `docs/SESSION_LOG.md` | 待更 | 编辑 |
| 17 | GitHub Release vX.Y.Z | 待发 | tag + push |
| 18 | 旧图谱相关文档（plans/design） | **保留** | 历史归档，不删 |
| 19 | 旧图谱测试快照 | 待删 | 随目录删除 |
| 20 | Emulator 实测报告 | 待做 | 手动验证 |

---

## 8. 风险登记（Risk Register）

| ID | 风险 | 可能性 | 影响 | 缓解 | 触发条件 | 应急 |
|----|------|--------|------|------|----------|------|
| R1 | Hilt DI 图崩溃（GraphViewModel 移除后） | 低 | 高（App 启动失败） | B4 前已无 `@HiltViewModel` 引用 GraphViewModel | assembleDebug 启动崩溃 | revert B4 |
| R2 | 章节树生成逻辑错误（tags 为空） | 中 | 中（章节树无子节点） | B1.3 保留 default_chapter 兜底 | `countNonRootChapters() == 0` | 修复 SeedDataLoader |
| R3 | 错题本路由冲突（顶级 vs 子路由） | 中 | 中（导航异常） | B3.1 使用 `ROUTE_WRONG_ANSWER_TOP` 区分 | 导航到错题本崩溃 | revert B3 |
| R4 | GraphSkeleton ProGuard 路径修复后 R8 仍报错 | 低 | 中（Release 构建失败） | B5.1 路径已验证正确 | assembleRelease 失败 | 暂时禁用 R8 |
| R5 | seed 版本升级触发全量重新导入耗时 | 中 | 低（首次启动慢） | 用户已接受（v0.7.2 先例） | 首次启动 > 5s | 优化导入逻辑 |
| R6 | 用户反馈需要图谱全局视图 | 中 | 低（功能缺失） | ADR-001 第 7 节回滚路径 | 用户反馈 | 评估是否恢复 feature:graph |
| R7 | CI 账单问题导致无法验证 | 已知 | 低 | 本地构建 + gh 上传（v0.8.14-v0.8.18 先例） | CI 不可用 | 本地验证 |

---

## 9. 执行检查表（Execution Checklist）

执行时按顺序勾选：

- [ ] **B1.1** ChapterDao 树状查询方法 + 测试
- [ ] **B1.2** ChapterRepository 接口 + 实现 + DI + 测试
- [ ] **B1.3** SeedDataLoader 章节树生成 + seed 2.12.0
- [ ] **G0-B1** `:core:data:testDebugUnitTest :core:database:testDebugUnitTest` 全绿
- [ ] **B2.1** 关系类型视觉编码设计
- [ ] **B2.2** RelatedGroup 重构
- [ ] **B2.3** Preview 添加
- [ ] **G0-B2** `:feature:knowledge:testDebugUnitTest` 全绿
- [ ] **B3.1** TopLevelDestination 修改
- [ ] **B3.2** WenyanNavHost 修改
- [ ] **B3.3** WrongAnswerScreen 顶级模式
- [ ] **B3.4** QuizScreen 移除错题本入口
- [ ] **G1** Emulator 实测 5 项通过
- [ ] **B4.1** app/build.gradle.kts 删除依赖
- [ ] **B4.2** settings.gradle.kts 删除 include
- [ ] **B4.3** WenyanNavHost 删除 graphDestination
- [ ] **B4.4** TopLevelDestination 删除 Graph
- [ ] **B4.5** `rm -rf feature/graph/`
- [ ] **G2** `assembleDebug + assembleRelease + testDebugUnitTest` 全绿
- [ ] **B5.1** consumer-rules.pro 路径修复
- [ ] **B5.2** AGENTS.md 更新
- [ ] **B5.3** 00-STATUS.md 更新
- [ ] **B5.4** SESSION_LOG.md 更新
- [ ] **G3** Release 准入检查通过
- [ ] **Release** tag + push + APK 上传

---

## 10. 引用

- ADR: [docs/design/adr-001-graph-removal.md](../design/adr-001-graph-removal.md)
- 失败方案档案: [docs/03-FAILED-ATTEMPTS.md](../03-FAILED-ATTEMPTS.md)
- 版本兼容矩阵: [docs/02-VERSION-MATRIX.md](../02-VERSION-MATRIX.md)
- 状态快照: [docs/00-STATUS.md](../00-STATUS.md)
- 会话日志: [docs/SESSION_LOG.md](../SESSION_LOG.md)
- staff-engineer-mode specialist: `migration-and-deprecation@2.1.0`
