# ADR-001：移除知识图谱 UI 模块，引入章节树与关联知识点模块

- **Status**: Accepted（待实施）
- **Date**: 2026-07-27
- **Decision Owner**: 文研 App 项目（AI 协作）
- **Specialist**: migration-and-deprecation（staff-engineer-mode 2.1.0）
- **Reversibility**: 两向门（Two-way door）— 见第 7 节

> **OBSOLETE STATUS（2026-08-09）**：顶部“Accepted（待实施）”是历史快照。本 ADR 的迁移已经在 v0.9.0 及后续数据库迁移中实施；当前是否恢复图谱以 [决策 004：知识图谱 UI 暂缓恢复](../decisions/004-knowledge-graph-deferred.md) 为准。本文只保留原始决策与恢复信息，不再作为待办清单。

---

## 1. 决策问题（Decision Question）

是否应当移除 `feature:graph` 模块（知识图谱可视化 UI），将其底部导航位置让位给错题本（`WrongAnswerScreen` 提升为顶级目的地），并在知识点功能中引入"章节树 + 关联知识点模块"作为图谱关系能力的替代？

## 2. 上下文与驱动力（Forces）

| # | Force | Rationale |
|---|-------|-----------|
| F1 | 图谱 UI 实际使用价值低 | 2123+ 节点在移动端缩放平移体验差；用户考研复习场景以"线性阅读 + 关联跳转"为主，图谱全局视图非高频路径 |
| F2 | 图谱底层能力被核心算法复用 | `GraphRepository` / `InterferenceWarner` / `WeakSubgraphDetector` / `PrerequisiteChecker` 被 `SchedulingRepository` 与 FSRS 调度链路依赖，**不能移除** |
| F3 | 错题本当前为子路由，入口深 | `WrongAnswerScreen` 位于 `quiz` 之下，需经"真题 → TopBar 图标 → 错题本"两跳；错题本是高频复习入口，应扁平化 |
| F4 | 知识点详情已有关联点基础设施 | `KnowledgePointEntity` 已有 `relatedIds` / `contrastIds` / `extensionIds`；`RelatedPointsSection` 已实现，但呈现单薄，无关系类型视觉编码 |
| F5 | `ChapterEntity` 已支持树状但未被使用 | `parentId` 字段已存在，`ChapterDao.observeRoots/observeChildren` 已实现；但 `SeedDataLoader` 只生成扁平 default chapters（每科一个 parentId=null）|
| F6 | 移除 `feature:graph` 模块依赖关系清晰 | 静态搜索证实：`feature:graph` 仅被 `app` 模块通过 `WenyanNavHost` + `TopLevelDestination` 引用；`GraphRepository` 等核心类仅在注释中被 `CardRepository` / `ClockGuard` 提及，非真实依赖 |
| F7 | ProGuard 规则存在潜在 bug | `core/data/consumer-rules.pro` 第 22 行 `-keep class com.wenyan.app.core.data.graph.GraphSkeleton` 路径错误，实际类在 `core.data.seed.GraphSkeleton`，启用 R8 时会导致 keep 规则失效 |

## 3. 决策（Decision）

**采纳方案 A（移除 UI + 保留核心 + 章节树替代 + 错题本升级）**：

1. **移除** `feature:graph` Gradle 模块及其所有源码、测试、`consumer-rules.pro`
2. **保留** `core/data/repository/GraphRepository` + `core/data/graph/*` 算法服务 + `core/database/dao/GraphNodeDao` + `core/data/seed/GraphSkeleton` — 这些是 FSRS 调度与干扰项生成的底层依赖
3. **替换** 顶部"图谱"导航位为"错题本"，将 `WrongAnswerScreen` 从子路由提升为顶级目的地
4. **引入** 章节树（`ChapterTree`）作为知识点列表的层级视图，复用已存在的 `ChapterEntity.parentId` 与 `ChapterDao` 树状查询方法
5. **增强** `KnowledgePointDetailScreen.RelatedPointsSection` 为"关联知识点模块"：关系类型视觉编码（关联/对比/延伸用不同图标 + 颜色 + 连线语义）+ 可点击查看详情
6. **修复** `core/data/consumer-rules.pro` 中 `GraphSkeleton` keep 规则路径错误

## 4. 替代方案（Alternatives Considered）

| 方案 | 描述 | 拒绝原因 |
|------|------|----------|
| **B. 保留图谱 UI 但优化性能** | 继续维护 `feature:graph`，优化大图渲染 | 不解决 F1（场景错配）；维护成本高；用户调研未要求保留 |
| **C. 仅移除图谱 UI，不引入章节树** | 只做减法，关联能力交给现有 `RelatedPointsSection` | 不解决 F4（关联呈现单薄）；用户明确要求"比较常见的树状图，集成在知识点功能里面" |
| **D. 将图谱 UI 改为子路由，错题本保持子路由** | 图谱降级为知识点详情页内的"关系图"入口 | 仍维护高成本低价值模块；F3 未解决 |
| **E. 重写图谱为新树状组件** | 在 `feature:graph` 内用树状布局替换力导向 | 违背用户意图（移除图谱功能）；模块定位混乱 |

## 5. 后果（Consequences）

### Positive

- 删除 ~2,000 行低使用率 UI 代码（GraphScreen + GraphCanvas + GraphLayout + GraphConstants + 3 个测试文件）
- 底部导航信息架构与用户高频任务对齐（错题本一跳可达）
- 章节树复用既有 `ChapterEntity` schema，无需 DB 迁移
- 关联知识点模块让"关系"能力从全局图谱下沉到知识点上下文，更贴合复习场景
- 修复 GraphSkeleton ProGuard 路径 bug，为 R8 启用扫清障碍

### Negative

- 丧失图谱全局视图（用户无法再一眼看到学科全景）— 由章节树 + 关联模块组合缓解
- `GraphNodeEntity` 数据仍写入 DB（`SeedDataLoader` 自动生成），但不再有 UI 消费 — 可接受（算法服务仍消费）
- 移除 `feature:graph` 测试（3 文件）导致总测试数下降 — 无功能覆盖损失（功能本身移除）
- 用户若未来想要回图谱，需重新引入模块 — 由 VCS 历史保底（见第 7 节）

## 6. 责任与检查路径（Responsibility）

- **Responsibility**: 文研 App AI 协作会话（用户最终验收）
- **Local Check Path**:
  1. 本地 `:app:assembleDebug` + `:app:assembleRelease` 全绿
  2. `testDebugUnitTest` 全绿（移除 graph 测试后预期 ~440 tests）
  3. Emulator 实测：底部导航 5 个 Tab 顺序与跳转
  4. Emulator 实测：知识点详情页关联模块点击跳转
  5. Emulator 实测：错题本数据未丢失（Room 持久化）

## 7. 可逆性（Reversibility）

- **Cost to Undo**: 中等。`feature:graph` 模块完整保留在 VCS 历史（git revert 单次提交可恢复）；`TopLevelDestination.Graph` 与 `graphDestination` 函数恢复也是机械改动
- **Reconsideration Trigger**: 用户在两个版本内明确反馈"需要全局图谱视图"，或章节树 + 关联模块组合无法满足"看清学科全景"的需求
- **Two-way Door**: 是。无数据迁移、无 schema 破坏、无外部 API 契约变更。回滚为纯代码操作

## 8. 健身函数（Fitness Functions）

| ID | Property | Metric | Threshold | Source | Cadence | Failure Response |
|----|----------|--------|-----------|--------|---------|------------------|
| FF1 | 依赖方向 | `app` → `feature:graph` 引用数 | = 0 | 静态搜索 `implementation(project(":feature:graph"))` | 每次 build | 阻断 build |
| FF2 | 核心算法保留 | `GraphRepository` / `InterferenceWarner` / `WeakSubgraphDetector` / `PrerequisiteChecker` 编译存在 | = 4 类全部存在 | `compileDebugKotlin` | 每次 build | 阻断 build |
| FF3 | 错题本入口 | `TopLevelDestination` 含 `WrongAnswer` 项 | = 1 | 静态检查 `destinations` 列表 | 每次 build | 阻断 build |
| FF4 | 章节树数据 | `chapters` 表 `parent_id IS NOT NULL` 行数 | > 0（至少一科有子章节） | DB 查询 | seed 导入后 | 阻断 Release |
| FF5 | 关联模块呈现 | `RelatedPointsSection` 含三种关系类型视觉编码 | = 3（关联/对比/延伸） | UI 自检 | Emulator 实测 | 修复后重测 |
| FF6 | ProGuard 规则 | `GraphSkeleton` keep 规则路径正确 | 路径 = `com.wenyan.app.core.data.seed.GraphSkeleton` | grep consumer-rules.pro | 每次 build | 阻断 R8 启用 |

## 9. 引用

- 完整任务清单：[docs/plans/graph-removal-tree-migration.md](../plans/graph-removal-tree-migration.md)
- 失败方案档案：[docs/03-FAILED-ATTEMPTS.md](../03-FAILED-ATTEMPTS.md)
- 状态快照：[docs/00-STATUS.md](../00-STATUS.md)
- 上次会话日志：[docs/SESSION_LOG.md](../SESSION_LOG.md)
