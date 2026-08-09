# 文研 Android：Cloud MVP 执行进度

> 本文件由 docs/plans/CLOUD-MVP-EXECUTION.md 管理。
> 只记录可复算的断点与证据，不粘贴大段构建日志。
> 状态枚举：PENDING / IN_PROGRESS / PASS / FAIL / BLOCKED / SKIPPED。

## 1. 启动基线

| 项目 | 值 |
| --- | --- |
| 记录日期 | 2026-08-09 |
| 仓库 | qbjsdsb/wenyan-android |
| 已完成阶段 | PR-00、PR-01A |
| PR-01A merge commit | 205eb5c2ded5451e461167c7462f2e6348f76bd1 |
| 第一个未完成阶段 | C01 / PR-01B |
| App | v0.9.43 / versionCode 68 |
| Room | v10 |
| seed | v2.26.0 |
| 知识点 | 1101 |
| 真题 | 564 |
| 论述题 | 142 |
| 写作材料 | 909 |
| seed SHA-256 | d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446 |
| tools.zip | 本任务禁止读取 |

这些值是启动准备时的事实。真正执行 C00 时必须从最新 main 重新复算，不得机械复制为 PASS。

## 2. 检查点总览

| 检查点 | 阶段 | 状态 | Commit | 核心证据 |
| --- | --- | --- | --- | --- |
| C00 | 云端预检与基线冻结 | PENDING | — | — |
| C01 | PR-01B：审计接入 CI | PENDING | — | — |
| C02 | PR-02A：内容溯源数据库 v11 | PENDING | — | — |
| C03 | PR-02B：loader 可信度语义 | PENDING | — | — |
| C04 | PR-02C：来源与可信度 UI | PENDING | — | — |
| C05 | PR-03A：LearningUnit 数据库 v12 | PENDING | — | — |
| C06 | PR-03B：确定性单元生成 | PENDING | — | — |
| C07 | PR-03C：按 unit 独立 FSRS | PENDING | — | — |
| C08 | PR-04A：DailyPlanner 纯函数 | PENDING | — | — |
| C09 | PR-04B：DailyPlan 数据库 v13 | PENDING | — | — |
| C10 | PR-04C：跨日与显式重建 | PENDING | — | — |
| C11 | PR-05A：Today 内容页 | PENDING | — | — |
| C12 | PR-05B：四段顶层导航 | PENDING | — | — |
| C13 | PR-06A：知识详情纯拆分 | PENDING | — | — |
| C14 | PR-06B：主动回忆与分层学习 | PENDING | — | — |
| C15 | PR-06C：显式关系与 fallback | PENDING | — | — |
| C16 | PR-06D：三维进度 | PENDING | — | — |
| C17 | PR-07A：PracticeAttempt 数据库 v14 | PENDING | — | — |
| C18 | PR-07B：Training 薄容器 | PENDING | — | — |
| C19 | PR-07C：真题先作答后核对 | PENDING | — | — |
| C20 | PR-07D：专项 session 与错题修复 | PENDING | — | — |
| C21 | PR-08A：WritingSession 数据库 v15 | PENDING | — | — |
| C22 | PR-08B：离线写作编辑器 | PENDING | — | — |
| C23 | PR-08C：量规、自评与历史对比 | PENDING | — | — |
| C24 | MVP 闭环与最终自审 | PENDING | — | — |

## 3. 当前断点

- 当前检查点：C00
- 当前状态：PENDING
- 当前分支：执行时填写
- 当前 HEAD：执行时填写
- 最新 origin/main：执行时填写
- 上一个通过检查点：PR-01A（远端已合并）
- 下一个动作：按 CLOUD-MVP-EXECUTION.md 完成云端环境、Git、Python、Gradle、seed 和 migration 能力预检
- 停止条件：未触发

## 4. 每个检查点的追加模板

完成或停止一个检查点时，在本节顶部追加一段：

### CXX / 阶段名 — 状态

- 开始 HEAD：
- 结束 commit：
- 开始时间：
- 结束时间：
- 拟修改文件：
- 实际修改文件：
- 行为或数据模型变化：
- 定向测试：
  - 命令：
  - 结果：
- 全量测试：
  - 命令：
  - 结果：
- Migration：
  - 起止版本：
  - schema：
  - 升级 fixture：
  - 结果：
- Seed / ID：
  - seed SHA：
  - ID 集合：
  - 结果：
- 用户数据不变量：
- 未运行项与原因：
- 风险：
- 回滚：
- 下一个检查点：
- 是否触发停止条件：

## 5. 架构决定记录

| 决定 | 当前结论 | 依据 |
| --- | --- | --- |
| 连续执行边界 | C01 → C24；不进入 PR-09 | 用户希望一次云端授权完成 MVP |
| PR/分支 | 一个独立分支、一个 Draft PR、内部原子 commits | 降低人工消息次数，同时保留审阅和回滚边界 |
| Room 版本计划 | v11 provenance；v12 learning unit；v13 daily plan；v14 practice attempt；v15 writing session | 每个语义边界独立迁移，禁止占用同一版本 |
| PracticeAttempt | 独立表，不复用 wrong_answers/review_logs/template_fills/ai_grading_records | 旧表语义不足以表达一次完整输出尝试 |
| WritingSession | 独立表，旧 TemplateFill/材料语义保留 | 长期草稿、计时和自评需要独立生命周期 |
| tools.zip | 禁止读取 | 与 PR-01B → PR-08C 产品重构无关，且含历史资料与环境产物 |
| 最终远端动作 | Draft PR；不 Ready、不合并、不发布 | 保留最终人工闸门 |

若实际主线已经占用计划中的 Room 版本，必须顺延并在此表记录；不得覆盖已有 migration。

## 6. 最终验收摘要

执行完成前保持为空。C24 后填写：

- 最终 branch：
- 最终 HEAD：
- Draft PR：
- Room 迁移链：
- Python tests：
- Gradle tests：
- Instrumentation / migration tests：
- Debug build：
- Seed audit：
- Seed / ID 不变量：
- 用户数据不变量：
- 真机待验：
- PENDING_CI：
- 已知风险：
- 回滚入口：
