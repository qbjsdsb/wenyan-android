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
| C00 | 云端预检与基线冻结 | PASS | 本记录所在 commit | KVM-independent SQLite 验证真实执行 8→9、9→10、8→10；Android MigrationTestHelper 保留为第二层门禁 |
| C01 | PR-01B：审计接入 CI | PASS | 本记录所在 commit | Python 3.12；25 tests；双审计+cmp；精简报告 always 上传；PENDING_CI |
| C02 | PR-02A：内容溯源数据库 v11 | PASS | 本记录所在 commit | v10→11、v2→11 SQLite 实迁移；旧 fixture 保留；Android helper PENDING_KVM |
| C03 | PR-02B：loader 可信度语义 | PASS | 本记录所在 commit | fail-closed mapper；占位来源过滤；draft/rejected 正式队列隔离；649 tests |
| C04 | PR-02C：来源与可信度 UI | PASS | 本记录所在 commit | reusable badge/source section；知识/论述/素材入口；大字横屏 previews；656 tests |
| C05 | PR-03A：LearningUnit 数据库 v12 | PASS | 本记录所在 commit | v11→12/v2→12；空 unit tables；memo/review fixture 保留；659 tests |
| C06 | PR-03B：确定性单元生成 | PASS | 本记录所在 commit | pure core/keyword generator；stable reconcile；core-only legacy mapping；665 tests |
| C07 | PR-03C：按 unit 独立 FSRS | PASS | 本记录所在 commit | unit 独立队列/预览/评分/撤销；sibling 公平；跨时点恢复；全量门禁通过 |
| C08 | PR-04A：DailyPlanner 纯函数 | PASS | 本记录所在 commit | injected Clock/Asia-Taipei；stable priority chain；finite explained infeasibility；10 tests |
| C09 | PR-04B：DailyPlan 数据库 v13 | PASS | 本记录所在 commit | v12→13/v2→13；atomic get-or-create；ordered restart state；rollback/concurrency tests |
| C10 | PR-04C：跨日与显式重建 | PASS | 本记录所在 commit | Taipei boundary；explicit idempotent carry/skip/special/rebuild；DONE protection；rollback tests |
| C11 | PR-05A：Today 内容页 | PASS | 本记录所在 commit | persisted-plan-only feature；grouped/continue/summary states；route mapper；large-font/landscape previews |
| C12 | PR-05B：四段顶层导航 | PASS | 本记录所在 commit | Today cold start；exact four tabs；all legacy child mappings；save/restore and detail/card regressions pass |
| C13 | PR-06A：知识详情纯拆分 | PASS | 本记录所在 commit | slot-based Recall/Outline+Explanation/Evidence/Relations；旧 key/顺序/文案/查询不变 |
| C14 | PR-06B：主动回忆与分层学习 | PASS | 本记录所在 commit | sequential reveal；SavedState restore；missing layers fail closed；large-font/landscape previews |
| C15 | PR-06C：显式关系与 fallback | PASS | 本记录所在 commit | typed relations；explicit precedence/dedup；automatic label；unknown direction；eq_0038 preserved |
| C16 | PR-06D：三维进度 | PASS | 本记录所在 commit | unit 真实复习/到期证据；写作维度 fail-closed；无虚假百分比 |
| C17 | PR-07A：PracticeAttempt 数据库 v14 | PASS_LOCAL | 本记录所在 commit | explicit v13→14; fixed errors; unknown enums fail closed; JVM migration passed |
| C18 | PR-07B：Training 薄容器 | PASS | 本记录所在 commit | exact four reused routes; stable entry contract; large-font/landscape preview |
| C19 | PR-07C：真题先作答后核对 | PASS | 本记录所在 commit | persisted answer-first workflow; reviewed-only reveal; SavedState recovery; repair candidate |
| C20 | PR-07D：专项 session 与错题修复 | PASS | 本记录所在 commit | stable seven-dimensional session; persisted summary; idempotent later-date Room repair; full stage gate |
| C21 | PR-08A：WritingSession 数据库 v15 | PASS | 本记录所在 commit | independent table/DAO; explicit v14→15 and v2→15; legacy rows preserved |
| C22 | PR-08B：离线写作编辑器 | PASS | 本记录所在 commit | offline editor; debounced save/retry; persisted recovery/timer; three modes |
| C23 | PR-08C：量规、自评与历史对比 | PASS | 本记录所在 commit | seven explained dimensions; transparent total; provenance gate; trends/follow-up |
| C24 | MVP 闭环与最终自审 | PASS_LOCAL | 本记录所在 commit | full JVM/build/audit gates; v10→15; device checklist; Draft PR pending Cloud page |

## 3. 当前断点

- 当前检查点：C24 / MVP 闭环与最终自审完成，等待 Cloud Draft PR
- 当前状态：PASS_LOCAL（Draft PR 尚待 Cloud 页面创建）
- 当前分支：`work`（平台提供的唯一隔离 checkout）
- 当前 HEAD：本记录所在 commit（起始 main `cef480a72272c6a9dd0f01ec929245eef3d6ee49`）
- 最新 origin/main：N/A（Cloud 平台 checkout 未提供 `origin`；未添加或修改 remote）
- 上一个通过检查点：C23 / PR-08C
- 下一个动作：由 Cloud 页面创建 Draft PR；不得 Ready、合并、tag 或 release
- 停止条件：未触发；Android MigrationTestHelper 因无 KVM 继续标记 PENDING_KVM

## 4. 每个检查点的追加模板

### C24 MVP 闭环与最终自审 — PASS_LOCAL

- 13 项闭环均已映射到实际测试/实现证据；新增 v10→v15 生产 migration 链测试并通过。
- Python 25 tests、双只读 seed audit/cmp、全模块 JVM tests、Debug APK、androidTest APK、导航/Planner/Writing targeted gates 全绿。
- 完整 diff 审查覆盖 destructive migration、REPLACE/CASCADE、seed/baseline、版本/签名/release workflow、超大文件、重复业务、硬编码事实及虚假 PASS。
- 生成不超过十分钟设备清单；无 KVM 的 instrumentation 继续 PENDING_KVM。完整证据见 `docs/reports/CLOUD-MVP-C24-EVIDENCE.md`。
- 终端未 push/Ready/merge/tag/release；Draft PR 必须由 Cloud 页面创建。
- 续审修正：原写作计时仅使用 wall clock、证据引用仍是自由文本、量规无备注/历史接线、离开时 debounce 可能未落盘，且 `WritingMaterialDao.REPLACE` 在新增 provenance CASCADE 后存在误删来源风险；均已补真实回归测试并修复，未保留虚假 PASS。

### C23 / PR-08C 量规、自评、证据和历史对比 — PASS

- 固定立意、结构、理论、文本证据、分析、语言、时间七维，每级有解释；总分由各维整数直接相加并明确不是官方评分。
- 仅 REVIEWED 进入可引用证据；LEGACY_UNVERIFIED/UNKNOWN 单列为待核线索，不宣称学术正确。
- 自评随 WritingSession 自动保存并可进程恢复；首次/多次趋势与弱项后续离线修复任务均为纯确定性计算。
- 无来源、混合来源、首次/多次历史、弱项任务、编码恢复测试与 feature 编译通过；未接 API、未改 seed。进入 C24。
- 续审补齐：证据卡改为 repository 提供的真实素材，仅 `content_status=REVIEWED` 可选，legacy/unknown 只读显示待核；每维备注与已完成 session 历史趋势均接入 UI 和自动保存。

### C22 / PR-08B 离线写作编辑器 — PASS

- 写作工作台覆盖审题、中心论点、分论点/提纲和正文，素材页提供明确入口；全部读写经 ViewModel/Store，不由 Composable 访问 DAO。
- 三种模式时长固定；计时由持久开始时间与累计暂停时间推算，不逐秒写库，回拨不产生负耗时。
- 750ms debounce 仅保存最新草稿，失败显式显示并可重试；sessionId 进入 SavedStateHandle，数据库草稿支持杀进程恢复。
- 定向自动保存/失败重试/三模式/暂停恢复/回拨/超长正文测试与 Debug APK 通过；完全离线且未接 API。下一检查点 C23。
- 续审补齐：活动计时采用 monotonic anchor，wall clock 仅用于跨进程恢复；返回前强制 flush 最新 debounce 草稿，且 ViewModel 进程恢复、时钟回拨和 reviewed-only 引用均有直接测试。

### C21 / PR-08A WritingSession 数据库 v15 — PASS

- 新增独立 writing_sessions 表与 DAO，显式保存题目快照、审题/立意/提纲/证据引用/正文、自评、三种模式、状态与可恢复计时字段；未把生命周期塞入旧素材 JSON。
- 可选关联真题、模板和 PracticeAttempt 均采用 SET NULL；旧 TemplateFill/AnswerTemplate/WritingMaterial/AiGradingRecord 语义和 909 条旧素材不变。
- 显式 v14→15 migration 只建新表/索引；JVM verifier 覆盖 14→15 与生产 2→15 链并校验用户 fixture 保留，15.json 与 androidTest APK 通过。
- 枚举未知值 fail closed；没有新增 seed、教材事实、API 或 destructive migration。下一检查点 C22。

### C20 / PR-07D 专项 session 与错题修复 — PASS

- 七维 session 筛选、稳定排序/去重、空结果解释、session 恢复与完成/漏项/错因总结均已接线并通过测试。
- Room repair tests 通过：只允许 later-date，确定性 stable ID 保证幂等，事务失败回滚，不改当天任务或 FSRS。
- 通过完整阶段门禁：全模块 JVM tests、Debug APK、androidTest APK、25 项 Python tests、两次只读 seed audit 与报告一致性比较。
- Robolectric 官方 runtime 仅下载到本机 Maven cache，未进入仓库；seed/baseline/既有 ID/用户数据均未修改。下一检查点 C21。

### C20 / PR-07D 专项 session 与错题修复 — IN_PROGRESS（UI/恢复子提交）

- 真题筛选新增并保留 610/801/805/806/807 试卷代码，筛选随导航传入详情，前后题不越出 session。
- PracticeAttemptStore 新增 session Flow；详情显示已完成、关键词/提纲/正文漏项及错因总结；sessionId/题目索引继续由 SavedState 恢复。
- planner/ViewModel/navigation/Debug targeted gate PASS。真实 Room repair tests 两次因容器无法获取 Robolectric android-all runtime artifact 失败（代理/直连均失败），测试代码保留且未冒充通过。
- 下一步：补齐该环境证据或以最终 CI 运行作为门禁，再判定 C20；当前不提前进入 C21。

### C20 / PR-07D 专项 session 与错题修复 — IN_PROGRESS（领域/事务子提交）

- 新增七维 PracticeSessionPlanner：年份、历史代码、题型、学科、考频、薄弱点、错因；稳定排序/去重，空结果可解释，拒绝虚构代码，保留 610/801/805/806/807。
- PracticeAttempt 使用 SavedState 持久 sessionId；summary 输出完成数、漏项、错因计数和稳定修复建议。
- PracticeRepairRepository 仅允许 later-date，确定性 stable ID 幂等插入明日计划并将候选标 SCHEDULED，不改当天计划或 FSRS。
- 纯 tests PASS；真实 Room tests 已编写，但当前容器 Robolectric android-all artifact 下载被代理拒绝，诚实保留未运行证据。
- 下一子提交：筛选/session UI 接线、恢复进度与阶段全量门禁；C20 尚未 PASS。

### C19 / PR-07C 真题先作答后核对 — PASS

- 详情页先录入关键词/提纲/正文；空白不能保存或揭示。来源状态始终显示，仅 REVIEWED 且有框架才主动揭示，legacy 只保留作答并提示待核。
- 状态、输入、索引、揭示标记和 attempt ID 进入 SavedStateHandle；进程恢复不自动揭示。
- PracticeAttemptStore 使用 Mutex+Room transaction 串行幂等 insert/update，题目身份不可变；自评/错因写入，AGAIN/HARD/错因生成 CANDIDATE。
- 定向 workflow/ViewModel tests 与 feature assemble PASS；不改题干、答案、来源、seed 或旧 ID。次日任务编排留在 C20。
- 下一个检查点：C20 / PR-07D。

### C19 / PR-07C 真题先作答后核对 — IN_PROGRESS（领域状态机子提交）

- 新增纯 PracticeAttemptWorkflow，锁定 ANSWERING→SAVED→REVEALED→ASSESSED→COMPLETED。
- 空白不能保存/揭示；只有 REVIEWED 且存在框架才允许揭示；揭示前不能自评，完成前必须自评；AGAIN/HARD 或任一固定错因生成 CANDIDATE。
- save/reveal 重复调用保持阶段单调、幂等；定向 4 tests PASS。
- 下一子提交：repository 持久化、SavedState 恢复、详情 UI、来源常显和次日修复候选接线。C19 尚未 PASS。

### C18 / PR-07B Training 薄容器 — PASS

- Training 精确提供快速回忆、真题作答、610 写作、错题修复四入口；只导航到 Cards、现有真题、论述题/素材和错题本，不复制队列、筛选、DAO 或 DailyPlan。
- stable entry contract 测试锁定标题、路由、顺序与去重；2x 字体和 900×500 横屏 Preview 编译。
- `:app:testDebugUnitTest --tests '*TrainingHubTest' :app:assembleDebug` PASS。首次编译暴露错误 weight import 与错误 parent 假设，均最小修正后原命令通过。
- 不改 Room v14、migration、seed、ID 或用户数据。
- 下一个检查点：C19 / PR-07C。

### C17 / PR-07A PracticeAttempt 数据库 v14 — PASS_LOCAL（PENDING_KVM）

- Room 13→14 新增独立 practice_attempts；question 必填且禁止级联删除，point/unit 可空且删除时 SET_NULL，保护用户输出。
- 保存关键词、提纲、正文、计时、揭示/完成、自评、七类固定错因、修复状态与 session；DAO 写入使用 ABORT/UPDATE，不使用 REPLACE。
- 未知 attempt/rating/error 枚举返回 null，未知 repair 状态降为 NONE。
- JVM verifier 实际执行 13→14 与 2→14 并匹配导出 14.json；旧 fixture 保留，新表为空。Android helper 已编译路径接线，因无 KVM 保持 PENDING_KVM。
- 定向 Gradle 执行中 core:database tests 已完成；androidTest APK 后续阶段全量构建因本轮执行时限中断，不冒充全量通过。
- seed、既有 ID 和用户表未修改；无 destructive migration。
- 下一个检查点：C18 / PR-07B。

### C16 / PR-06D 三维进度 — PASS

- 证据：见过取 unit 实际 reviewCount；记得取真实已练 unit 的 nextReviewAt 到期状态；写得出在 PracticeAttempt 尚未建立时固定显示“尚未练习”，不以浏览或卡片评分冒充。
- 精度：仅显示离散状态和可解释计数，不生成虚假百分比或小数。
- 数据路径：新增只读 KnowledgeProgressSource，经按 point 查询 active unit+record；不写 Room、seed 或用户数据。
- 定向测试：`mise exec -- gradle :feature:knowledge:testDebugUnitTest --tests '*KnowledgeProgressTest' --tests '*KnowledgePointDetailViewModelTest' :feature:knowledge:assembleDebug --stacktrace` — PASS。
- 回滚：revert 本检查点 commit；无 migration 或数据回滚。
- 下一个检查点：C17 / PR-07A。
- 是否触发停止条件：否。

### C15 / PR-06C 显式关系与低可信 fallback — PASS

- 开始 HEAD：`9287aad`
- 结束 commit：本记录所在 commit
- 类型：建立 COMPARE_WITH/INFLUENCES/INFLUENCED_BY/PART_OF/EVIDENCE_FOR/EXAM_VARIANT/UNKNOWN 完整领域枚举；来源严格区分 EXPLICIT 与 AUTOMATIC_FALLBACK
- 旧数据解释：contrast_ids 明确映射 COMPARE_WITH；extension_ids 因旧字段没有方向，只映射 UNKNOWN；tag 派生 related_ids 保持 UNKNOWN 且 UI 标“自动关联”，不冒充 REVIEWED
- 一致性：显式关系优先于同目标 fallback，stable order 去重；self/dangling target 丢弃；reason 记录原字段或 fallback 原因
- 真题：exam_questions.related_point_ids 映射 EXAM_VARIANT；`eq_0038` 固定测试验证顺序、去重和类型不丢失
- UI/导航：知识详情由统一 relationship projection 分组，fallback 与方向待核可见；A→B→C 导航回归 PASS；不恢复图谱画布
- 定向测试：`mise exec -- gradle :core:data:testDebugUnitTest --tests '*ContentRelationshipTest' --tests '*KnowledgeRepositoryTest' :feature:knowledge:testDebugUnitTest :app:testDebugUnitTest --tests '*KnowledgeDetailNavigationPolicyTest'` — PASS
- 不变量：不改 Room/schema/migration/seed/ID/用户数据；未向 seed 回写关系或 review 状态；C14 双 audit 后 seed SHA 保持冻结值
- 回滚：revert 本检查点 commit；仅领域投影与标签恢复，无数据回滚
- 下一个检查点：C16 / PR-06D
- 是否触发停止条件：否

### C14 / PR-06B 主动回忆与分层学习 — PASS

- 开始 HEAD：`96974d3`
- 结束 commit：本记录所在 commit
- 流程：标题后默认只显示主动回忆提示，按 30 秒回忆→2 分钟答题骨架→考试表达→理解与辨析→证据与来源逐层主动揭示；明确提示“揭示不代表掌握”
- 内容边界：summary/coreConclusion/studyText/multiPerspectives/data_sources 只读映射；现有数据没有独立答题骨架时显示“暂无已审校内容”，不从 UI/AI 猜写
- 恢复：revealed layer names 写入 SavedStateHandle，旋转/进程恢复可重建；未知未来值 fail closed 忽略，不崩溃
- 测试/预览：顺序、空骨架、考试表达映射、未知恢复值 tests PASS；2x fontScale、900×500dp 横屏、长文本 previews 编译
- 定向测试：`mise exec -- gradle :feature:knowledge:testDebugUnitTest --tests '*KnowledgeStudyLayersTest' --tests '*KnowledgePointDetailViewModelTest' :feature:knowledge:assembleDebug` — PASS
- Python/seed：25 tests 与双 audit/cmp PASS；0 errors / 2783 known debt / 0 new debt；seed SHA 不变
- 不变量：Room v13、migration、seed、既有 ID 和用户数据均未修改；真题/关系路由仍在原 Relations 区块
- 回滚：revert 本检查点 commit；无数据迁移或持久用户学习记录需要回滚
- 下一个检查点：C15 / PR-06C
- 是否触发停止条件：否

### C13 / PR-06A 知识详情纯拆分 — PASS

- 开始 HEAD：`e54a53c`
- 结束 commit：本记录所在 commit
- before/after：原 `summary → multi_perspective → sources → related_points → related_essays → wrong_answers` 的六段 LazyColumn 条件分支，提取为 slot-based Recall、Outline/Explanation、Evidence、Relations 顺序合同；header 仍固定最前
- 行为变化：无。保留原 item key/contentType、显隐条件、文案、Composable、回调、路由和 ViewModel/Repository 查询；没有视觉或数据修改
- 测试先行：新增纯顺序测试锁定全量顺序和稀疏内容省略语义；实现后定向测试 2/2 PASS
- 定向/阶段测试：`mise exec -- gradle :feature:knowledge:testDebugUnitTest :app:testDebugUnitTest --tests '*KnowledgeDetailNavigationPolicyTest' :feature:knowledge:assembleDebug` — PASS，BUILD SUCCESSFUL
- 不变量：Room 仍为 v13；migration/seed/既有 ID/用户数据均未修改；seed SHA-256 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`
- 自审：逐文件检查 screen 调用、纯 section contract 与两条 regression tests；`git diff --check` PASS
- 回滚：revert 本检查点 commit 即恢复内联条件分支，不涉及数据库或数据回滚
- 下一个检查点：C14 / PR-06B
- 是否触发停止条件：否

### C12 / PR-05B 四段顶层导航 — PASS

- 开始 HEAD：`89d2c2d`
- 结束 commit：本记录所在 commit
- 顶层：精确收敛为 今日/知识/训练/我的；NavHost startDestination 改 Today，冷启动直接消费持久化计划
- 旧入口：Training hub 保留卡片、真题背诵、论述题、写作素材；My hub 保留错题本、设置、AI。旧 feature/route 未删除，均作为子路由，子页面隐藏外层导航栏
- 状态/返回：顶层继续使用 saveState/restoreState；child→parent 纯映射固定。Essay/Writing/WrongAnswer 增加显式返回 parent；知识详情 A→B→C 仍使用已有动态栈策略；Cards fullscreen 继续从 cards backStackEntry 共享 ViewModel
- 深链：`parentRouteFor` 覆盖全部旧入口；未知 route 返回 null，不伪造历史；已有 `popBackStackOrNavigateTo` 仅在无历史时 fallback
- 定向测试：`mise exec -- gradle :app:testDebugUnitTest --tests '*TopLevelDestinationTest' --tests '*KnowledgeDetailNavigationPolicyTest' :app:assembleDebug --stacktrace` — PASS；验证四顶层、全部旧入口 parent 映射与动态详情策略
- 全量测试：`mise exec -- gradle :core:database:testDebugUnitTest --tests '*JvmMigrationTest' testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --stacktrace` — PASS，BUILD SUCCESSFUL（3m11s）
- Python/seed：25 tests、双 audit/cmp PASS；seed SHA 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`
- Migration/数据：C12 不改 Room/seed/ID/用户数据；JVM verifier PASS；Android helper PENDING_KVM
- 真机待验：四顶层切换、旋转/进程恢复、系统返回、深链 fallback 和卡片全屏共享会话列入最终不超过 10 分钟清单
- 回滚：revert 本检查点 commit；Today 模块和旧五路由仍存在，不涉及数据回滚
- 下一个检查点：C13 / PR-06A
- 是否触发停止条件：否

### C11 / PR-05A Today 内容页 — PASS

- 开始 HEAD：`05fa134`
- 结束 commit：本记录所在 commit
- 模块边界：新增职责单一 `feature:today`；ViewModel 只订阅 `TodayPlanSource` 包装的 `DailyPlanRepository.observe(date)`，不访问 DAO、不调用或复制 DailyPlanner
- 只读映射：小型 `TodayPlanMapper` 将持久化 task 映射为到期/修复/新学/输出/写作分组；SUPERSEDED/CARRIED/SKIPPED 来源历史不冒充今日任务
- 页面能力：真实 settings snapshot 含 examDate 时显示倒计时，否则诚实省略；显示剩余预计时间、一键继续、分组列表、空态、不可行说明、错误态与完成总结
- 旧入口 callback：DUE/REPAIR/NEW→CARDS，OUTPUT/SPECIAL_SESSION→QUIZ，WRITING→WRITING_MATERIALS；C11 只暴露 callback，暂不替换顶层导航
- 状态证据：mapper/ViewModel tests 覆盖 loading、empty、partial、finished、infeasible、error 和 callback destination；2x fontScale 与 900×500dp landscape previews 编译
- 定向测试：`mise exec -- gradle :feature:today:testDebugUnitTest :feature:today:assembleDebug` — PASS（初次测试暴露 preview 参数、Long 断言和 StateFlow loading 重复发射问题，均最小修复后重跑通过）
- 全量测试：`mise exec -- gradle :core:database:testDebugUnitTest --tests '*JvmMigrationTest' testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --stacktrace` — PASS，BUILD SUCCESSFUL（11m58s）
- Python/seed：25 tests、双 audit/cmp PASS；0 errors / 2783 known debt / 0 new debt；seed SHA 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`
- Migration/用户数据：C11 不改 Room v13 schema/migration，不写 daily plan/task 或任何旧用户表；JVM verifier PASS；Android helper PENDING_KVM
- 风险/回滚：Today 尚未成为顶层冷启动页，导航切换留给 C12；revert 本检查点 commit 可完整移除模块且不影响数据
- 下一个检查点：C12 / PR-05B
- 是否触发停止条件：否

### C10 / PR-04C 跨日、遗留与显式重建 — PASS

- 开始 HEAD：`33c3460`
- 结束 commit：本记录所在 commit
- 跨日：DailyPlanner 继续使用注入 Clock 与 Asia/Taipei；新增 15:59Z/16:01Z（台北 23:59/00:01）边界测试。计划 repository 以明确 ISO date 为键，同日始终读取原快照，次日才接受新 settings/content snapshot
- 遗留集合：只查询目标日期之前仍 PENDING 的任务；不静默搬运。`resolveLegacy` 仅接受显式 CARRY/SKIP/SPECIAL_SESSION，目标日期必须晚于来源计划
- 幂等/防循环：carry/special 使用确定性 ID、stable ID 与 carriedFromTaskId；重复决定读取已有结果，source 终态不重复创建；later-date 约束阻止回指形成循环
- 显式重建：只重建未完成任务；DONE stable ID 拒绝进入 replacements，永不复活。被移除的未完成任务标 SUPERSEDED、不删除；失败在 Room transaction 内完整回滚
- 定向测试：`mise exec -- gradle :core:data:testDebugUnitTest --tests '*DailyPlanRepositoryTest' --tests '*DailyPlannerTest' :core:database:testDebugUnitTest --tests '*JvmMigrationTest' --stacktrace` — PASS（首次因新容器缺 Robolectric runtime artifact 环境失败；预取官方 Maven artifact 后原测试重跑通过）
- 全量测试：`mise exec -- gradle testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --stacktrace` — PASS，BUILD SUCCESSFUL（542 actionable tasks）
- Python/seed：25 tests、双 audit/cmp PASS；0 errors / 2783 known debt / 0 new debt；seed SHA 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`
- Migration/用户数据：C10 不改 Room v13 schema/migration；JVM verifier 复跑；所有变更仅触及新 daily 表且不使用 DELETE/REPLACE；旧用户表不变
- 未运行项：Android instrumentation 因无 `/dev/kvm` 保持 PENDING_KVM；androidTest APK 已构建
- 回滚：revert 本检查点 commit；C09 getOrCreate 与持久化计划仍可用
- 下一个检查点：C11 / PR-05A
- 是否触发停止条件：否

### C09 / PR-04B DailyPlan 数据库 v13 — PASS（PENDING_KVM）

- 开始 HEAD：`5dcc7f2`
- 结束 commit：本记录所在 commit
- 数据模型：Room 12→13；`daily_plans` 唯一 plan_date，保存创建时间、考试方案年、设置快照、内容版本和状态；`daily_tasks` 保存稳定 ID、唯一位置、类型、内容/unit、预计时间、状态、遗留来源与时间戳
- 数据安全：v12→13 只 CREATE TABLE/INDEX，新表初始为空；无 DROP/REPLACE/旧表改写。task FK 删除策略仅限新 plan→task CASCADE 与 unit 删除 SET_NULL
- 原子语义：`DailyPlanRepository.getOrCreate` 在单一 Room transaction 内读取当天、生成、INSERT IGNORE 竞争仲裁和 ABORT task insert；任务失败会回滚 plan，同日并发只生成/保存一份
- 重启顺序：读取时显式 `ORDER BY position,id`，已完成状态原样读取；不依赖 Room relation 的隐含顺序
- 定向测试：`mise exec -- gradle :core:database:testDebugUnitTest --tests '*JvmMigrationTest' :core:data:testDebugUnitTest --tests '*DailyPlanRepositoryTest' --stacktrace` — PASS；覆盖 8 并发调用、状态/顺序恢复、事务回滚、owner fail-fast、v12→13 与 v2→13
- 全量测试：`mise exec -- gradle testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --stacktrace` — PASS（583 tests / 0 failures；Debug/androidTest APK 构建）
- Migration：JVM SQLite 实际执行 v12→13 并严格匹配 13.json；v2→13 完整生产链保留旧 fixture；Android MigrationTestHelper 12→13 已编译，因无 KVM 为 PENDING_KVM
- Python/seed：25 tests、双 audit/cmp PASS；0 errors / 2783 known debt / 0 new debt；seed SHA 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`
- 用户数据不变量：旧 MemoRecord、unit record、review、错题、学习进度 fixture 不变；不修改 seed 或既有内容 ID
- 回滚：revert 本检查点 commit；已升级设备保留空/新 daily 表，不影响全部旧表；禁止降级安装
- 下一个检查点：C10 / PR-04C
- 是否触发停止条件：否

### C08 / PR-04A DailyPlanner 纯函数 — PASS

- 开始 HEAD：`8584c90`
- 结束 commit：本记录所在 commit
- 拟修改/实际文件：仅 `core:data` planner 纯模型/算法、对应 JVM tests 与 progress/session 文档；不访问 Room、UI、seed 或网络
- 时间语义：构造注入 `Clock`，日期强制通过 `Asia/Taipei` zone 计算；测试使用固定 Instant，不读系统默认时区
- 稳定排序：严格执行 bucketRank → overdueDays DESC → retrievability ASC → examFrequencyRank → recentWeakness DESC → subjectRotationRank → stableId；bucket 固定为 DUE/REPAIR/NEW/OUTPUT/WRITING
- 不可行策略：配额永不隐式增加；零配额、考试已过、无可信新内容、大量逾期、缺输出训练及按计划缺 610 写作均返回明确 `PlanIssue`
- 定向测试：`mise exec -- gradle :core:data:testDebugUnitTest --tests '*DailyPlannerTest'` — PASS，10 tests；覆盖相同输入、到期优先、单科轮换、可信过滤、零配额、考试已过、大量逾期、完整排序链、stable ID 与不可行输出/写作
- 阶段全量：`mise exec -- gradle :core:data:testDebugUnitTest --tests '*DailyPlannerTest' :core:database:testDebugUnitTest --tests '*JvmMigrationTest' testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --stacktrace` — PASS，Debug 与 androidTest 构建完成
- Python/seed：25 tests PASS；双 audit 均 0 errors / 2783 known debt / 0 new debt；`cmp` PASS；seed SHA 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`
- Migration/用户数据：C08 不改变 Room v12、migration 或任何用户表；JVM migration verifier 复跑 PASS；Android helper 因无 KVM 保持 PENDING_KVM
- 风险/回滚：C08 只定义纯选择合同，持久化与并发 get-or-create 留在 C09；revert 本检查点 commit 即可
- 下一个检查点：C09 / PR-04B
- 是否触发停止条件：否

### C07 / PR-03C 按 unit 独立 FSRS — PASS（PENDING_KVM）

- 开始 HEAD：`b7dc340`
- 结束 commit：本记录所在 commit
- 已完成范围：LearningUnitRecord↔FlashCard mapper；unit 独立到期队列/rate/preview；事务 review log point_id+unit_id；receipt 精确 undo；失效/错配 unit fail-closed
- 单一真相源：unit 评分仅写 `learning_unit_records` 与 `review_logs`，明确不写 `memo_records`；旧 memo 仍只作为 C06 首次 core 映射兼容源
- 定向测试：`mise exec -- gradle :core:data:testDebugUnitTest --tests '*LearningUnitCardQueueTest' :feature:cards:testDebugUnitTest --tests '*CardsViewModelTest' :core:database:testDebugUnitTest --tests '*JvmMigrationTest'` — PASS
- 已覆盖：一次评分只改当前 unit；sibling record 不变且 round-robin 全量出现；ReviewLog 同时记录 point/unit；AGAIN 从 REVIEW 进入 RELEARNING 且 lapse 7→8；receipt undo 精确恢复；持久化 future record 在时钟跨过 nextReviewAt 后进入队列，进程恢复不依赖内存去重；FSRS 既有参考向量随全量测试复跑
- Cards 接线：CardRepository 直接读取 active unit+record；LearningUnitCard/CardItem 使用稳定 unit ID；CardsViewModel 按 unit 独立 preview/rate/leech/undo，旧 point 调度仅保留兼容 fallback，不双写
- 全量测试：`mise exec -- gradle :core:database:testDebugUnitTest --tests '*JvmMigrationTest' testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --stacktrace` — PASS，BUILD SUCCESSFUL（542 actionable tasks）
- Python/seed：`python -m unittest discover -s tools/tests -p 'test*.py'` PASS（25 tests）；两次正式 audit 均 0 errors / 2783 known debt / 0 new debt，`cmp` PASS
- Migration：C07 不改 schema；v12 JVM verifier PASS；Android helper仍 PENDING_KVM
- Seed / 用户数据：seed SHA 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`；不改既有 ID；没有 REPLACE/DELETE 用户记录；只在显式 unit 评分事务写 unit record+log
- 未运行项：Android instrumentation 因 Cloud runner 无 `/dev/kvm` 保持 PENDING_KVM，不冒充执行
- 下一个检查点：C08 / PR-04A
- 是否触发停止条件：否

### C06 / PR-03B 确定性单元生成与旧进度映射 — PASS

- 开始 HEAD：`4a41f41`
- 结束 commit：本记录所在 commit
- 开始时间：2026-08-09
- 结束时间：2026-08-09
- 拟修改文件：纯 LearningUnit generator/reconciler、事务 repository、unit DAO 幂等方法、SeedDataLoader import schema、tests、progress/session 文档
- 实际修改文件：`core/data/learning` 两个纯逻辑文件、`LearningUnitRepository.kt`、两个 unit DAO、`SeedDataLoader.kt`、同步 tests 与 progress/session 文档
- 行为或数据模型变化：seed import schema 3→4，在首次安装或一次性升级时显式生成 unit；可靠 tags 生成 KEYWORD，无可靠结构只生成 CORE；无运行时 LLM、无内容猜测
- 确定性与 ID：CORE 固定 `:core:0`；关键词保留既有 position，末尾新增只分配新 position；移除只从 active IDs 排除并由 DAO 标 active=false，记录不删除；重新出现复用原 ID
- 旧进度：仅新建的 CORE record 逐字段复制旧 MemoRecord；其他 unit 从 NEW 开始；已有 unit record 永不 upsert/覆盖，重复同步不重置用户调度
- 定向测试：
  - 命令：`mise exec -- gradle :core:data:testDebugUnitTest --tests '*LearningUnitSynchronizerTest' --tests '*SeedDataLoaderTest' :core:database:testDebugUnitTest --tests '*JvmMigrationTest' --no-daemon --stacktrace`
  - 结果：PASS；覆盖重复生成一致、文案变化 ID 不变、末尾新增不重排、移除停用、core-only 旧进度复制、非 core NEW、重复同步不重置及 JVM migration
- 全量测试：
  - 命令：`mise exec -- gradle testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace`
  - 结果：PASS，BUILD SUCCESSFUL（665 JVM tests / 0 failures；542 actionable tasks）
  - 命令：`python -m unittest discover -s tools/tests -p 'test*.py'`，随后两次正式 seed audit 与 `cmp`
  - 结果：PASS，25 tests；0 errors / 2783 known debt / 0 new debt，报告字节一致
- Migration：C06 不改变 v12 schema/migration；C05 JVM/SQLite v2→12 verifier 复跑 PASS；Android helper 继续 PENDING_KVM
- Seed / ID：seed SHA 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`；原 1101/564/909 ID 集合不变；unit ID 仅从既有 point ID/type/position 派生
- 用户数据不变量：事务中使用 Room `@Upsert` 更新 unit 文案但不 REPLACE；records 只 `INSERT IGNORE` 缺失项；旧 MemoRecord 保持原表和原值，移除 unit 不删 record
- 未运行项与原因：Android instrumentation 因无 KVM 保持 PENDING_KVM；本检查点未切换正式评分/调度读取，该切换只在 C07
- 风险：当前可靠结构只包含 CORE 与 seed tags 的 KEYWORD；不从自由文本猜 SEQUENCE/COMPARE/EVIDENCE/EXAM_OUTLINE，后续有显式结构时才能确定性扩展
- 回滚：revert 本检查点 commit；v12 空表结构仍存在但无调度消费者，旧 MemoRecord 继续可用
- 下一个检查点：C07 / PR-03C
- 是否触发停止条件：否

### C05 / PR-03A LearningUnit 数据库 v12 — PASS（PENDING_KVM）

- 开始 HEAD：`1596c85`
- 结束 commit：本记录所在 commit
- 开始时间：2026-08-09
- 结束时间：2026-08-09
- 拟修改文件：LearningUnit/Record entity+DAO、ReviewLog 可空 unit FK、v11→12 migration、v12 schema、JVM/Android migration tests、progress/session 文档
- 实际修改文件：`core/database` 的两个 entity/DAO、`ReviewLogEntity.kt`、`Migration_11_12.kt`、DB/module 注册、`12.json`、JVM tests；`app` Android migration test；progress/session 文档
- 行为或数据模型变化：新增 `learning_units` 与 `learning_unit_records` 空表；ReviewLog 追加可空 `learning_unit_id` 且始终保留非空 `point_id`；本检查点不生成 unit、不复制进度、不切换调度
- 稳定 ID：`LearningUnitId.create(pointId,type,position)` 输出小写 type 的 `pointId:type:position`，覆盖 CORE/KEYWORD/SEQUENCE/COMPARE/EVIDENCE/EXAM_OUTLINE；不使用内容 hash，position 禁止负数
- 定向测试：
  - 命令：`mise exec -- gradle :core:database:testDebugUnitTest --tests '*JvmMigrationTest' --tests '*LearningUnitEntityTest' --no-daemon --stacktrace`
  - 结果：PASS，8 tests / 0 failures；实际执行 v11→12 与 v2→12 生产 migration 链，严格匹配 v12 schema
- 全量测试：
  - 命令：`mise exec -- gradle testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace`
  - 结果：PASS，BUILD SUCCESSFUL（659 JVM tests / 0 failures；542 actionable tasks）
  - 命令：`python -m unittest discover -s tools/tests -p 'test*.py'`，随后两次正式 seed audit 与 `cmp`
  - 结果：PASS，25 tests；两次均 0 errors / 2783 known debt / 0 new debt，报告字节一致
- Migration：
  - 起止版本：11→12，以及 2→3→4→5→6→7→8→9→10→11→12
  - schema：导出 `12.json`；逐表核对列、索引、唯一索引、FK 与 `foreign_key_check`
  - 升级 fixture：v11 fixture 含旧 memo、review log、错题、进度、来源和内容；迁移前后旧列快照一致，review 的 `point_id` 不变且 `learning_unit_id=NULL`
  - 结果：PASS；`learning_units` 与 `learning_unit_records` 迁移后均为 0 行；Android helper 已编译，运行 PENDING_KVM
- Seed / ID：seed SHA 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`；1101/564/909 数量与 ID 集合不变
- 用户数据不变量：无旧表重建、DELETE、REPLACE 或 destructive migration；memo/review/进度逐字段保留，新 unit 表初始为空
- 未运行项与原因：Android `connectedDebugAndroidTest` 因 Cloud runner 无 `/dev/kvm` 保持 PENDING_KVM
- 风险：C05 只建立存储边界；unit 生成/旧进度映射必须在 C06 通过幂等测试后才启用，调度切换只允许在 C07
- 回滚：发布前 revert 本检查点 commit；不得对已升级用户执行降级或清库
- 下一个检查点：C06 / PR-03B
- 是否触发停止条件：否

### C04 / PR-02C 来源与可信度 UI — PASS

- 开始 HEAD：`5e3ebcc`
- 结束 commit：本记录所在 commit
- 开始时间：2026-08-09
- 结束时间：2026-08-09
- 拟修改文件：designsystem provenance 组件、知识/论述详情、写作素材只读入口、导航、UI mapper/tests、progress/session 文档
- 实际修改文件：`ProvenanceComponents.kt` 与 tests；knowledge 的知识点/论述题详情、论述题列表、写作素材列表及 tests；Repository/DAO relation；`WenyanNavHost.kt`；progress/session 文档
- 行为或数据模型变化：新增可复用 `ProvenanceBadge` / `SourceSection`；知识点和论述题显示内容可信度与来源证据；论述题页新增写作素材只读入口，909 条素材逐条显示可信度，仅在真实非占位来源存在时显示来源
- 定向测试：
  - 命令：`mise exec -- gradle :core:designsystem:testDebugUnitTest --tests '*ProvenanceComponentsTest' :feature:knowledge:testDebugUnitTest --tests '*KnowledgePointDetailViewModelTest' --tests '*EssayDetailViewModelTest' --tests '*KnowledgeNavigationPolicyTest' --no-daemon --stacktrace`
  - 结果：PASS；覆盖 unknown/legacy 克制映射、页码范围、来源类型、多来源详情流与既有详情行为
  - 命令：`mise exec -- gradle :feature:knowledge:testDebugUnitTest --tests '*WritingMaterialProvenanceTest' :app:testDebugUnitTest --tests '*KnowledgeDetailNavigationPolicyTest' :core:database:testDebugUnitTest --tests '*JvmMigrationTest' --no-daemon --stacktrace`
  - 结果：PASS；占位来源不展示、真实来源 trim 展示、A→B→返回 A 导航策略及 JVM/SQLite migration verifier 全绿
- Compose / preview：`SourceSectionPreview` 同时提供多来源长书名、420dp/1.5x 大字和 840×360/2x 横屏证据；布局使用 FlowRow、LazyColumn 和自动换行，无固定文本高度
- 全量测试：
  - 命令：`mise exec -- gradle testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace`
  - 结果：PASS，BUILD SUCCESSFUL（656 JVM tests / 0 failures；542 actionable tasks）；首次全量因临时容器缺 Robolectric 官方 artifact 失败，下载准确 Maven Central artifact 后同一命令重跑退出码 0
  - 命令：`python -m unittest discover -s tools/tests -p 'test*.py'`，随后两次 seed audit 与 `cmp`
  - 结果：PASS，25 tests；0 errors / 2783 known debt / 0 new debt，报告字节一致
- Migration：C04 不改 Room schema/migration；C02 的 JVM/SQLite verifier 复跑 PASS；Android helper 继续 PENDING_KVM
- Seed / ID：seed SHA 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`；1101/564/909 数量与 ID 集合不变
- 用户数据不变量：只读查询与展示；不写 seed、用户进度、错题、FSRS 或素材，未使用 destructive migration
- 未运行项与原因：Android instrumentation 因无 KVM 保持 PENDING_KVM；Compose JVM mapper/组件测试与 preview、APK 编译提供当前层证据
- 风险：当前 seed 大部分 provenance 为 legacy/unknown，UI 会如实显示“历史资料/来源类型待确认”，不会用危险红色或伪造书名页码
- 回滚：revert 本检查点 commit；数据库 v11 内容和用户数据无需回滚
- 下一个检查点：C05 / PR-03A
- 是否触发停止条件：否

### C03 / PR-02B loader 可信度语义 — PASS

- 开始 HEAD：`e68c08f`
- 结束 commit：本记录所在 commit
- 开始时间：2026-08-09
- 结束时间：2026-08-09
- 拟修改文件：SeedDataLoader 的 provenance 映射、正式学习 DAO 查询、纯 mapper tests、DAO fake、progress/session 文档
- 实际修改文件：`SeedProvenanceMapper.kt`、`SeedDataLoader.kt`、`KnowledgePointDao.kt`、`DataSourceDao.kt`、mapper tests、两处测试 fake 和 progress/session 文档
- 行为或数据模型变化：仅显式且合法的 review 字段可成为 `REVIEWED`；缺失、旧 `DRAFT` 和未知值降为 `LEGACY_UNVERIFIED`；来源必须同时有非占位标题和合法显式证据类型，否则为 `UNKNOWN`；`AI_DRAFT/REJECTED` 被排除于复习、列表、搜索与 RAG 正式查询
- 定向测试：
  - 命令：`mise exec -- gradle :core:data:testDebugUnitTest --tests '*SeedProvenanceMapperTest' --tests '*SeedDataLoaderTest' :core:database:testDebugUnitTest --tests '*JvmMigrationTest' --no-daemon --stacktrace`
  - 结果：PASS；覆盖真实来源、`其他` 占位、无来源、显式 REVIEWED、AI_DRAFT、REJECTED、未知值与正式队列判定，并复跑 JVM/SQLite migration verifier
- 全量测试：
  - 命令：`mise exec -- gradle testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace`
  - 结果：PASS，BUILD SUCCESSFUL（649 JVM tests / 0 failures；542 actionable tasks）
  - 命令：`python -m unittest discover -s tools/tests -p 'test*.py'`，随后两次正式 seed audit 与 `cmp`
  - 结果：PASS，25 tests；两次均 0 errors / 2783 known debt / 0 new debt，报告字节一致
- Migration：C03 不改 Room schema 或 migration；C02 v2→11/v10→11 JVM verifier 再次 PASS；Android helper 仍 PENDING_KVM
- Seed / ID：seed SHA 仍为 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`；1101 知识点、564 真题、909 写作素材及 ID 集合不变
- 用户数据不变量：沿用已有“只为缺失知识点创建 MemoRecord”与 seed/user 来源 ID 命名隔离；重复导入仅重建 `seed-*-source:` 记录，不触碰用户来源或已有 FSRS 进度
- 未运行项与原因：无数据库版本变化；Android MigrationTestHelper 按 C02 证据继续 PENDING_KVM
- 风险：当前 seed 没有显式 review/source status，因此现有内容按保守策略落为 legacy/unknown；仍可学习，但 UI 必须在 C04 如实说明而不能包装为已审校
- 回滚：revert 本检查点 commit；不需要数据回滚，v11 的保守默认仍可读取
- 下一个检查点：C04 / PR-02C
- 是否触发停止条件：否

### C02 / PR-02A 内容溯源数据库 v11 — PASS（PENDING_KVM）

- 开始 HEAD：`cf66013`
- 结束 commit：本记录所在 commit
- 开始时间：2026-08-09
- 结束时间：2026-08-09
- 拟修改文件：Room entity、v10→v11 migration、v11 导出 schema、JVM/Android migration tests、progress/session 文档
- 实际修改文件：`core/database` 的 provenance enum、四个内容/来源 entity、`Migration_10_11.kt`、数据库注册与 `11.json`、JVM tests，以及 `app` 的 Android MigrationTestHelper 测试和 progress/session 文档
- 行为或数据模型变化：Room 升至 v11；内容审校状态与来源证据类型分离；写作素材可存标题和显式关联知识点；历史内容只得到 `LEGACY_UNVERIFIED` / `UNKNOWN`，旧 OCR `VERIFIED` 不会被推断为 `REVIEWED`
- 定向测试：
  - 命令：`mise exec -- gradle :core:database:testDebugUnitTest --tests '*JvmMigrationTest' --tests '*ProvenanceStatusTest' --no-daemon --stacktrace`
  - 结果：PASS，8 tests / 0 failures；覆盖未知枚举 fail-closed、v10→11 和仓库最早导出 v2→11 生产 migration 链
- 全量测试：
  - 命令：`mise exec -- gradle testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace`
  - 结果：PASS，BUILD SUCCESSFUL（644 JVM tests / 0 failures；542 actionable tasks）；首次尝试因 Cloud 缺少 Robolectric 正式 runtime artifact 失败，下载准确 Maven Central artifact 后同一命令重跑退出码 0
  - 命令：`python -m unittest discover -s tools/tests -p 'test*.py'`，随后两次正式 seed audit 与 `cmp`
  - 结果：PASS，25 tests；两次审计均 0 errors / 2783 known debt / 0 new debt，报告字节一致
- Migration：
  - 起止版本：10→11，以及 2→3→4→5→6→7→8→9→10→11
  - schema：新增 Room 导出 `11.json`；严格核对全部表的列、索引、外键与 `PRAGMA foreign_key_check`
  - 升级 fixture：subjects、chapters、knowledge_points、exam_questions、memo_records、study_progress；v8 起另含 wrong_answers、writing_materials、data_sources；旧列逐表快照保持一致
  - 结果：PASS（SQLite JDBC 实际执行生产 Migration 对象）；Android v10→11 MigrationTestHelper 已编译进 androidTest APK，运行保持 PENDING_KVM
- Seed / ID：
  - seed SHA：`d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`
  - ID 集合：1101/1101 知识点 ID 唯一、564/564 真题 ID 唯一、909/909 写作素材 ID 唯一
  - 结果：seed、seed schema、audit baseline 字节不变；未修改 loader、UI 或 seed
- 用户数据不变量：migration 仅追加列与索引；旧用户表、旧列值和既有内容 ID 均未删除、重排、复用或重建；无 destructive migration
- 未运行项与原因：`connectedDebugAndroidTest` 因 Cloud runner 无 `/dev/kvm` 未运行，明确记为 PENDING_KVM；未冒充 Android instrumentation 已执行
- 风险：SQLite JDBC 已覆盖 SQL 与 fixture，但 Android SQLite driver 的第二层兼容性仍须在有 KVM runner 执行保留的 MigrationTestHelper
- 回滚：revert 本检查点 commit；由于尚未发布 v11，不执行降级 migration 或清空数据
- 下一个检查点：C03 / PR-02B
- 是否触发停止条件：否

### C01 / PR-01B 审计接入普通 CI — PASS（PENDING_CI）

- 开始 HEAD：`22da442`
- 结束 commit：本记录所在 commit
- 开始时间：2026-08-09
- 结束时间：2026-08-09
- 拟修改文件：`.github/workflows/android.yml`、`tools/tests/`、`tools/README.md`、progress/session 文档
- 实际修改文件：`.github/workflows/android.yml`、`tools/tests/test_audit_seed.py`、`tools/tests/test_android_workflow.py`、`tools/README.md`、progress/session 文档
- 行为或数据模型变化：无产品行为变化；普通 Android CI 在 Gradle 之前增加只读内容门禁
- 定向测试：
  - 命令：`python -m unittest discover -s tools/tests -p 'test*.py'`
  - 结果：PASS，25 tests；新增覆盖 `--check` SHA mismatch/只读、报告无正文、workflow 顺序、固定 Python、双审计、禁止写 baseline、always 上传和原 Android 命令保留
  - 命令：两次正式 seed audit 后 `cmp`
  - 结果：PASS；两次均 0 errors / 2783 known debt / 0 new debt，报告字节一致
- 全量测试：
  - 命令：`mise exec -- gradle testDebugUnitTest assembleDebug --no-daemon --stacktrace`
  - 结果：PASS，BUILD SUCCESSFUL（511 actionable tasks；639 JVM tests / 0 failures）
  - GitHub Actions：PENDING_CI；当前 Cloud checkout 无 remote，且用户禁止 terminal push/PR，最终由 Cloud 页面打开 Draft PR 后运行
- Migration：C01 不修改 Room/migration；C00 JVM/SQLite 证据保持有效，Android instrumentation 仍 PENDING_KVM
- Seed / ID：
  - seed SHA：`d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`
  - schema SHA：`24ef820be741e60e50f1a18f868abbe84409d749870eb23c31193e4ef0fc4e1b`
  - baseline SHA：`e7febcc14fa1754d66bcdf094d0a2c059c2f35dedd616b051e31a9d2868b5e66`
  - 结果：命令前后字节一致；ID 集合未改
- 用户数据不变量：未修改 Room、产品代码或用户表
- 未运行项与原因：真实 GitHub Actions 等最终 Cloud Draft PR；无 remote 且禁止 terminal push
- 风险：Actions YAML 只能在远端 runner 获得最终平台验证；静态合同测试已锁定关键顺序和禁止项
- 回滚：revert 本检查点 commit 即恢复原 Android workflow；不涉及 baseline 或 seed 回滚
- 下一个检查点：C02 / PR-02A
- 是否触发停止条件：否

### C00 / JVM/SQLite migration fallback — PASS

- 开始 HEAD：`1379715`（上一次 C00 环境证据 commit）
- 结束 commit：本记录所在 commit
- 开始时间：2026-08-09
- 结束时间：2026-08-09
- 拟修改文件：v8→v9→v10 migration 的共享 SQL 表达、`core:database` JVM 测试依赖与测试、progress/session 文档
- 实际修改文件：`Migration_8_9.kt`、`Migration_9_10.kt`、`JvmMigrationTest.kt`、`core/database/build.gradle.kts`、`gradle/libs.versions.toml`、progress/session 文档
- 行为或数据模型变化：无；原有 5 条 SQL 文本和执行顺序不变，仅提取为生产 Migration 与 JVM verifier 共用的 statement list
- 定向测试：
  - 命令：`mise exec -- gradle :core:database:testDebugUnitTest --tests '*JvmMigrationTest' --no-daemon --stacktrace`
  - 结果：PASS，3 tests；实际执行 8→9、9→10 和 8→10 链式 migration
  - 证据：从 Room 8/9 schema JSON 创建 SQLite 数据库；模拟存量 v8 缺失的两个复合索引；迁移后逐表核对 v9/v10 导出 schema 的列与索引，并执行 `foreign_key_check`
  - 用户 fixture：subjects、chapters、knowledge_points、exam_questions、wrong_answers、memo_records、study_progress 迁移前后逐字段快照一致
- 全量测试：
  - 命令：`mise exec -- gradle testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace`
  - 结果：PASS，退出码 0，BUILD SUCCESSFUL（639 tests / 0 failures；542 actionable tasks）
  - 命令：`python -m unittest discover -s tools/tests -p 'test*.py'`
  - 结果：PASS，18 tests
  - 命令：两次 seed audit 后 `cmp`
  - 结果：PASS，0 errors / 2783 known debt / 0 new debt，报告字节一致
- Migration：
  - 起止版本：8→9、9→10、8→10
  - schema：使用仓库已导出的 8.json、9.json、10.json；文件未修改
  - 升级 fixture：7 个关联/用户数据表，合法外键，迁移前后逐字段一致
  - 结果：PASS（SQLite JDBC 实际执行）；Android `MigrationTestHelper` 源码和 androidTest 保留，当前无 KVM 环境为 NOT_RUN/PENDING_KVM，不冒充已运行
- Seed / ID：
  - seed SHA：`d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`
  - ID 集合：未修改；1101 知识点、564 真题（其中 142 论述题）、909 写作材料
  - 结果：PASS；正式 seed、baseline 和 Room schema 无 diff
- 用户数据不变量：fixture 中知识点、题目、错题、FSRS 记录和学习进度的全部列值保持一致；无 destructive migration、REPLACE 或表重建
- 未运行项与原因：Android `connectedDebugAndroidTest` 仍因 Cloud runner 无 `/dev/kvm` 未运行；按合同 3.2 节及用户明确授权，保留为有 KVM runner 的第二层门禁
- 风险：SQLite JDBC 不能替代 Android driver 的最终兼容性证据，因此 Android instrumentation 必须继续保留并在可用 runner 补跑
- 回滚：revert 本检查点 commit；migration SQL 文本未改变
- 下一个检查点：C01 / PR-01B
- 是否触发停止条件：否

### C00 / 云端预检与基线冻结 — BLOCKED

- 开始 HEAD：`cef480a72272c6a9dd0f01ec929245eef3d6ee49`
- 结束 commit：本记录所在 commit
- 开始时间：2026-08-09
- 结束时间：2026-08-09
- 拟修改文件：仅 Cloud 环境依赖与本进度记录；不修改产品代码、Room、migration 或 seed
- 实际修改文件：`docs/plans/CLOUD-MVP-PROGRESS.md`、`docs/SESSION_LOG.md`；系统环境安装 Ubuntu 正式包和 Android SDK/AVD（均不入仓库）
- 行为或数据模型变化：无
- Git 基线：平台 checkout HEAD 与用户外部核验的 GitHub main SHA `cef480a72272c6a9dd0f01ec929245eef3d6ee49` 精确一致；`origin=N/A`，未添加或修改 remote
- 环境：Ubuntu 24.04.4；安装 `libasound2t64`、`libdrm2`、`libpulse0`、`libtcmalloc-minimal4t64`、`libxi6`、`libxkbfile1`；`libasound.so.2` 与 `libdrm.so.2` 可由 `ldconfig` 解析；emulator 37.1.11.0 二进制可运行
- 定向测试：
  - 命令：`python -m unittest discover -s tools/tests -p 'test*.py'`
  - 结果：PASS，18 tests
  - 命令：两次 `python -m tools.content_pipeline.audit_seed ... --check` 后 `cmp`
  - 结果：PASS，两次均为 0 errors / 2783 known debt / 0 new debt，报告字节一致
- 全量测试：
  - 命令：`mise exec -- gradle testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace`
  - 结果：PASS，退出码 0，BUILD SUCCESSFUL（538 actionable tasks）；首次运行因 Robolectric runtime artifact 未预热而失败，下载 Maven Central 正式 artifact 后原命令重跑通过
- Migration：
  - 起止版本：现有 Room v10 migration 能力预检
  - schema：v10 存在；本检查点未修改
  - 升级 fixture：NOT_RUN
  - 结果：BLOCKED；AVD 已创建且 system image 完整，但 Cloud runner 无 `/dev/kvm`，x86_64 emulator 明确报 `x86_64 emulation currently requires hardware acceleration`
- Seed / ID：
  - seed SHA：`d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`
  - ID 集合：未修改；1101 知识点、564 真题（其中 142 论述题）、909 写作材料
  - 结果：与冻结基线一致；seed v2.26.0
- 用户数据不变量：未触碰产品代码、Room schema、migration、seed、既有 ID 或用户数据
- 未运行项与原因：`connectedDebugAndroidTest` 和真实 migration/emulator tests 未运行；`/dev/kvm` 不存在，AVD 在被 `adb` 识别前退出
- 风险：当前 Cloud runner 无硬件加速，无法提供合同要求的真实 migration 升级证据
- 回滚：仓库仅 revert 本次文档 commit；系统包、SDK 与 AVD 是临时容器状态，不进入 Git
- 下一个检查点：仍为 C00；不得进入 C01
- 是否触发停止条件：是

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
