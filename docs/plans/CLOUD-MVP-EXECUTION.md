# 文研 Android：Codex Cloud 连续执行合同（PR-01B → PR-08C）

> 状态：云端 MVP 实施的唯一连续执行合同
> 维护日期：2026-08-09
> 仓库：qbjsdsb/wenyan-android
> 起始事实：PR-00 已完成；PR-01A 已由 PR #13 合并，merge commit 为 205eb5c2ded5451e461167c7462f2e6348f76bd1
> 第一个未完成工单：PR-01B
> 目标终点：PR-08C 完成后，通过一次 PR-00 → PR-08 的 MVP 闭环审计

本文件把原来需要用户反复发送“只读核对—允许实施—自审—提交”的流程，改为一次明确授权后的云端连续执行。它只改变人工等待点，不降低任何数据、测试、迁移、来源和发布门槛。

## 1. 启用条件与规则优先级

### 1.1 何时启用

只有用户在 Codex Cloud 中明确发送“执行 docs/plans/CLOUD-MVP-EXECUTION.md”或同义的连续实施授权时，本模式才启用。

普通任务、单独 PR、内容批次和故障修复仍按 AGENTS.md 与单工单文档中的常规流程执行。不能因为本文件存在，就默认跳过用户授权。

### 1.2 本模式允许什么

启用后，代理可以在一个独立云端分支中按本文件的检查点连续执行 PR-01B 到 PR-08C。每个普通检查点完成并通过自动验收后，不必再次等待用户发送“允许实施”。

这项连续授权包括：

- 只读核对当前检查点；
- 在允许范围内实施；
- 补测试、运行测试、修复本检查点造成的失败；
- 严格自审；
- 形成原子 commit；
- 更新 docs/plans/CLOUD-MVP-PROGRESS.md；
- 自动进入下一个检查点。

这项连续授权不包括：

- 直接写入 main；
- 合并任何 PR；
- 将 Draft 改为 Ready；
- 打 tag、发布 APK 或修改 Release；
- 修改签名、Secrets 或用户凭据；
- 猜写教材、页码、引文、真题、分值或答案；
- 读取、解压、复制或提交 tools.zip；
- 扩展到 PR-09 及以后；
- 并行启动多个写代理修改同一工作树。

### 1.3 冲突时的优先级

本任务的事实与规则按以下顺序解释：

1. 用户在当前云端任务中的最新明确指令；
2. AGENTS.md 的永久安全边界；
3. 本文件的连续执行协议；
4. docs/00-STATUS.md 的当前断点；
5. docs/plans/WENYAN-MASTER-PLAN.md 的阶段顺序；
6. 当前检查点对应的 PR 合同；
7. 实际代码、Room schema、seed、测试和 Git 历史。

本文件只覆盖“每个检查点都等待人工再次授权”和“每个检查点单独创建 PR”两项常规流程。稳定 ID、用户数据、显式 migration、来源真实性、测试、禁止发布等永久约束不被覆盖。

若实际代码与文档冲突，先以命令复算并记录。若冲突会改变产品语义、数据迁移或内容事实，必须停止；不得自行选一个版本继续。

## 2. 可验证的最终结果

本次连续任务完成时，必须同时满足：

1. PR-01B 的确定性 seed 审计已接入普通 Android CI；
2. 内容“可学习”与“来源已审校”在数据库、loader 和 UI 中分离；
3. 学习单位从整个知识点细化为稳定 LearningUnit，并按 unit 独立调度；
4. 每日计划是纯函数生成、Room 持久化、同日重启不重排；
5. 冷启动进入“今日”，顶层导航为“今日 / 知识 / 训练 / 我的”；
6. 知识详情支持主动回忆、答题骨架、理解辨析、证据来源和显式关系；
7. 真题支持先作答、后核对、错因记录和修复任务；
8. 610 写作支持离线审题、提纲、正文、计时、草稿恢复、自评和历史对比；
9. 从 v10 到最终 Room 版本的全部显式 migration 有导出 schema 和真实升级测试；
10. seed 的既有知识点、真题、章节 ID 集合不变，正式 seed 未因产品重构漂移；
11. PR-00 → PR-08 的最终闭环审计通过；
12. 最终只创建一个 Draft PR，不合并、不发布。

若执行时间或平台预算不足，只能在一个已通过验收的原子检查点结束，并把状态写入进度文件；不得把部分完成报告成最终完成。

## 3. Codex Cloud 环境准备

### 3.1 建议设置

在 Codex Cloud 中选择：

- Repository：qbjsdsb/wenyan-android
- Base branch：最新 main
- Model：由 Codex Cloud 自动选择；当前云端聊天不支持手动更改默认模型，不要把 Luna 或其他模型作为启动前置条件
- Reasoning：使用云端提供的默认设置；若界面不提供调节项，不因此停工
- Agent internet：关闭；本 MVP 不需要运行时联网检索
- Secrets：不配置发布签名、GitHub PAT 或 LLM API key
- 同一时间只运行一个写任务

Setup 阶段允许联网安装依赖。建议在 Cloud Environment 的 setup script 中配置：

~~~bash
set -euo pipefail

command -v mise >/dev/null
mise install
mise exec -- bash scripts/setup-env.sh

SDK_DIR="$(sed -n 's/^sdk.dir=//p' local.properties | tail -n 1)"
test -n "$SDK_DIR"
yes | mise exec -- "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
mise exec -- "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "emulator" "system-images;android-35;google_apis;x86_64"

if ! mise exec -- "$SDK_DIR/cmdline-tools/latest/bin/avdmanager" list avd | grep -q "Name: wenyan-api35"; then
  printf 'no\n' | mise exec -- "$SDK_DIR/cmdline-tools/latest/bin/avdmanager" create avd --force --name wenyan-api35 --package "system-images;android-35;google_apis;x86_64" --device pixel_5
fi

# Setup 阶段预热 Gradle、Robolectric 和 androidTest 依赖；
# agent 阶段关闭外网后仍应能运行现有测试。
mise exec -- gradle testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace
~~~

如果 setup script 无法安装 emulator，必须在 C00 报告具体失败。数据库检查点不得在没有任何可执行 migration 验证路径时继续。

建议 maintenance script：

~~~bash
set -euo pipefail
mise install
mise exec -- bash scripts/setup-env.sh --check
~~~

setup script 中的 export 不会自动延续到 agent 阶段。所有 Gradle 命令优先显式使用：

~~~bash
mise exec -- gradle <tasks> --no-daemon --stacktrace
~~~

Android SDK 路径从 local.properties 读取，不硬编码 /opt、HOME 或旧 CodeBuddy 路径。

### 3.2 Emulator 启动模板

需要 instrumentation migration test 时：

~~~bash
set -euo pipefail
SDK_DIR="$(sed -n 's/^sdk.dir=//p' local.properties | tail -n 1)"
"$SDK_DIR/emulator/emulator" -avd wenyan-api35 -no-window -noaudio -no-boot-anim -gpu swiftshader_indirect -no-snapshot > /tmp/wenyan-emulator.log 2>&1 &
EMULATOR_PID=$!

"$SDK_DIR/platform-tools/adb" wait-for-device
until [[ "$("$SDK_DIR/platform-tools/adb" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
  sleep 2
done

mise exec -- gradle connectedDebugAndroidTest --no-daemon --stacktrace
kill "$EMULATOR_PID" || true
~~~

若容器不支持 emulator，允许为 migration 测试建立不依赖破坏性降级的 JVM/SQLite 验证路径，但必须实际运行并验证导出 schema 与用户数据。只做静态阅读不能算 migration 通过。

## 4. Git、分支与提交协议

### 4.1 开始

C00 必须执行：

~~~bash
git status --short
git branch --show-current
git rev-parse HEAD
git remote -v
git rev-parse origin/main
git log -1 --oneline origin/main
~~~

要求：

- 工作树干净；
- 从最新 origin/main 新建唯一分支；
- 推荐分支名：agent/cloud-mvp-pr01b-pr08c；
- 不复用 PR-00、PR-01A、PR-01B 的旧分支；
- 不 force-push；
- 不改写 main 历史。

Codex Cloud 会在执行 setup 前检出所选分支或 commit。C00 不依赖 agent 阶段联网 fetch；用户应在启动任务时选择最新 main。若平台允许并且确有必要，可以只读 fetch 更新远端引用。若 origin/main 已在云端启动包合并后继续前进，必须重新核对这些新增提交是否与本计划冲突。无冲突时从最新 main 开始；有数据库版本、导航或 seed 冲突时停止。

### 4.2 原子检查点

每个 C01–C23 至少形成一个原子 commit。若手写 diff 预计超过约 800 行或 8 个核心文件，应在同一检查点内部进一步拆成“数据/领域/接线/测试”原子 commit，但不能混入下一个检查点。

每个 commit 必须：

- 只包含当前检查点；
- 同时更新 CLOUD-MVP-PROGRESS.md 的证据；
- commit message 说明为什么；
- 不包含构建产物、local.properties、数据库文件、APK、日志或原始资料；
- 保留可独立 revert 的边界。

不得为了保持“一个 commit”而压缩多个 migration、多个独立功能或巨型 UI 重构。

### 4.3 最终远端动作

完成 C24 后：

1. 推送当前独立分支；
2. 创建一个 Draft PR，base 为 main；
3. 不改 Ready；
4. 不合并；
5. 不打 tag；
6. 不发布；
7. 尽可能等待并读取真实 GitHub Actions；
8. CI 失败只修当前分支，不通过降低门槛变绿。

## 5. 永久数据与内容不变量

整个任务必须持续证明：

- knowledge point、exam question、chapter 的既有 ID 不删除、不重排、不复用；
- seed 正文、版本、顺序和 SHA 不因产品重构改变；
- memo_records、FSRS、review_logs、wrong_answers、study_progress、template_fills、AI 配置、聊天、设置和草稿不被清空；
- 每个 Room 版本都有显式 migration，不使用 destructive migration；
- 每个新 schema 都提交导出 JSON；
- v10 → 当前版本链式升级后旧用户 fixture 完整；
- 历史内容默认 LEGACY_UNVERIFIED/UNKNOWN，不能批量冒充 REVIEWED/OFFICIAL；
- AI_DRAFT、REJECTED 不进入正式学习队列；
- 2024/2025 年 805 缺证据内容保持未知，不猜写；
- 丁帆、聂珍钊、袁世硕的既定内容边界不改变；
- 909 条旧 writing_materials 不批量升级为可引用证据；
- eq_0038 的精确合题跨科关联不得被误删；
- 旧自动推导关系必须标为低可信 fallback，不能回写成已审校显式关系；
- 所有用户可见“教材原文、官方真题、页码、学者原话”都必须有真实证据；
- tools.zip 永不作为本任务输入。

## 6. 测试与自审协议

### 6.1 每个检查点

顺序固定为：

1. 读当前实现和测试；
2. 记录拟修改文件与风险到 progress；
3. 先补失败测试或确定性断言；
4. 做最小实现；
5. 跑定向测试；
6. 跑 git diff --check；
7. 逐文件自审；
8. 核对当前检查点验收清单；
9. 更新 progress；
10. commit；
11. 再进入下一个检查点。

### 6.2 阶段边界

C01、C04、C07、C10、C12、C16、C20、C23 后至少运行：

~~~bash
mise exec -- gradle testDebugUnitTest assembleDebug --no-daemon --stacktrace
git diff --check
~~~

C01 还必须运行 Python 审计与报告字节比较。每个数据库版本检查点必须运行对应 migration 测试，不得只依赖 assembleDebug。

### 6.3 报告口径

progress 和最终报告必须区分：

- PASS：命令实际执行并成功；
- FAIL：命令实际执行并失败；
- BLOCKED：环境或权限阻塞，未进入目标测试；
- NOT_RUN：有明确理由未运行；
- PENDING_CI：本地通过，等待远端 Actions。

“编译成功”不能代替测试通过，“测试代码已写”不能代替 migration 实际运行。

## 7. 停止条件

遇到以下任一情况，停止在最后一个已通过检查点，更新 progress 并报告，不得继续：

1. 需求、AGENTS、计划或实际架构存在会改变数据语义的冲突；
2. 无法实际证明 migration 与旧用户数据安全；
3. 为通过测试必须修改正式 seed、baseline、旧 ID 或放宽审计；
4. 需要猜写教材、真题、页码、引文、来源或答案；
5. 需要签名 Secrets、发布凭据或 LLM API key；
6. 云端构建环境、权限或磁盘无法修复；
7. 当前检查点的失败在允许范围内无法安全修复；
8. main 出现并发的 Room 版本、导航或 seed 变更；
9. 单个检查点无法进一步拆分且 diff 已大到不可审阅；
10. 发现真实用户数据可能被 CASCADE、REPLACE、重建或默认值覆盖；
11. 必须启用 destructive migration；
12. 已达到平台时间/资源限制。

以下情况不应停：

- 普通编译错误；
- 当前检查点新增测试暴露的本范围 bug；
- 格式、lint、导入、可空性或小型接线问题；
- 可在当前范围内修复的 flaky test；
- 需要在同一检查点内部再拆一个原子 commit。

## 8. 检查点路线

## C00：云端预检与基线冻结

必读：

- AGENTS.md
- docs/00-STATUS.md
- docs/architecture/current-system.md
- docs/plans/WENYAN-MASTER-PLAN.md
- docs/plans/PR-01A.md
- docs/plans/PR-01B.md
- 本文件与 CLOUD-MVP-PROGRESS.md
- .github/workflows/android.yml
- .github/workflows/release.yml
- Room v10 schema、MigrationTest、WenyanDatabase
- seed audit、schema、baseline 和测试

必须复算：

- App v0.9.43 / versionCode 68；
- Room v10；
- seed v2.26.0；
- 1101 知识点、564 真题、142 论述题、909 写作材料；
- seed SHA-256；
- PR-01A 审计结果；
- main、HEAD、工作树与分支；
- baseline Python 测试；
- baseline Gradle 单测与 assembleDebug；
- emulator/migration 测试能力。

成功条件：所有事实一致，或差异已证明是 main 的合法后续变更且不冲突。只更新 progress，不修改产品。

## C01 / PR-01B：审计接入普通 CI

合同：docs/plans/PR-01B.md。

目标：

- 在 Android 单测和构建前执行 seed audit；
- 固定 Python 版本；
- 同一正式 seed 连续审计两次并 cmp；
- 失败时上传不含正文的精简报告；
- CI 只检查，不写 baseline；
- 保留 testDebugUnitTest → assembleDebug 顺序。

禁止：

- 修改 seed、schema、baseline、release workflow、签名、Gradle 版本、Room 或产品代码；
- 使用 --write-baseline、|| true 或跳过失败。

退出证据：

- Python 单测全绿；
- seed 两次审计与 cmp 通过；
- seed/schema/baseline 字节不变；
- workflow 静态顺序测试；
- 阶段边界 Gradle 全绿。

建议 commit：ci(content): gate Android builds on deterministic seed audit

## C02 / PR-02A：内容溯源数据库 v11

目标：将“文本可用”与“来源已审校”分离，只做数据库和 migration。

模型至少表达：

- REVIEWED / LEGACY_UNVERIFIED / AI_DRAFT / REJECTED；
- OFFICIAL_ORIGINAL / USER_CONFIRMED / SECONDARY_RECOLLECTION / UNKNOWN；
- 来源书名、版本、页码范围、校验值、审校备注；
- 写作素材标题与关联知识点；
- 未知枚举安全降级。

迁移语义：

- v10 → v11；
- 历史条目默认 LEGACY_UNVERIFIED/UNKNOWN；
- 不因旧 loader 的 VERIFIED 文案升级为 REVIEWED；
- 旧用户表与全部既有内容 ID 不变。

退出证据：

- v11 schema 已导出；
- v10 → v11 与可用最早发布版本 → v11 的 migration test 实际运行；
- 用户 fixture 逐表保留；
- 1101/564 既有 ID 集合不变；
- 不改 loader、UI 或 seed。

## C03 / PR-02B：loader 可信度语义

目标：

- 有真实、有效、明确审校字段的内容才映射 REVIEWED；
- 仅“其他”、无来源或旧历史内容映射 LEGACY_UNVERIFIED；
- AI_DRAFT/REJECTED 不进入正式学习队列；
- 未知状态安全降级；
- seed 重复升级幂等；
- 用户进度不重置。

要求把可信度 mapper 从过大的 SeedDataLoader 中提取，不批量修改 seed。

退出证据：真实来源、占位来源、无来源、AI_DRAFT、未知值、重复升级和用户数据保留测试全绿；PR-01 audit 证明 ID/数量不漂移。

## C04 / PR-02C：来源与可信度 UI

目标：知识点、真题/论述题、写作素材入口显示真实可信度和来源。

要求：

- 可复用 ProvenanceBadge 与 SourceSection；
- LEGACY_UNVERIFIED 使用克制提示，不使用“错误内容”式危险红；
- 无来源不显示伪书名或页码；
- 多来源、长书名、未知枚举、大字体、横屏可用；
- A → B → 返回 A 导航不回归。

退出证据：mapper 测试、Compose/preview 证据、导航回归与阶段边界全量测试通过。

## C05 / PR-03A：LearningUnit 数据库 v12

目标：新增 LearningUnit、LearningUnitRecord，并让 review_logs 可选记录 learning_unit_id；暂不生成单元或切换调度。

稳定 ID 格式使用 pointId:type:position，不使用内容 hash。至少支持：

- CORE
- KEYWORD
- SEQUENCE
- COMPARE
- EVIDENCE
- EXAM_OUTLINE

要求：

- v11 → v12 显式 migration；
- 新表初始为空；
- unit 可 active=false，不物理删除历史；
- 旧 memo/review 仍可读；
- review log 同时保留 point_id；
- v12 schema 与链式 migration test。

## C06 / PR-03B：确定性单元生成与旧进度映射

目标：

- 以纯函数从现有字段确定性生成 unit；
- 不在运行时使用 LLM；
- 无可靠结构时只生成 CORE；
- 首次启用只把旧知识点级状态复制到 :core:0；
- 其他 unit 为 NEW；
- 移除 unit 只 active=false；
- 文案变化不改变 ID；
- 重复执行幂等。

退出证据：重复生成一致、末尾新增不重排、移除停用、旧进度只到 core、重复升级不重置调度、seed/ID 不漂移。

## C07 / PR-03C：按 unit 独立 FSRS 调度

目标：

- 每个 LearningUnitRecord 独立评分、到期和记录 review log；
- sibling 在同场分散/暂缓，但不会只显示第一张；
- :core:0 保留旧进度，其他 unit 从 NEW；
- 旧 memo_records 只作兼容，不形成双写真相源；
- 不重写 FSRS-6 公式。

必须覆盖：参考向量、一次评分只改当前 unit、撤销、Again/leech、跨日、进程恢复、review log point_id + unit_id、sibling 公平性。

若手写 diff 超预算，在 C07 内拆 repository、ViewModel 接线和测试 commit，不大改 CardsScreen。

## C08 / PR-04A：DailyPlanner 纯函数

目标：不访问 Room/UI 的确定性 planner。

优先级：

1. 到期复习；
2. 遗忘修复、Again、错题、leech；
3. 新内容覆盖；
4. 至少一个输出训练；
5. 按计划出现的 610 写作。

稳定排序键：

bucketRank → overdueDays DESC → retrievability ASC → examFrequencyRank → recentWeakness DESC → subjectRotationRank → stableId ASC。

不可行计划要返回可解释状态，不能无限加量。使用注入 Clock/LocalDate 和固定时区。

退出证据：相同输入相同输出、到期永远优先、单科轮换、无可信新内容、配额零、考试已过、大量逾期、同分 stableId、不可行状态测试。

## C09 / PR-04B：DailyPlan/DailyTask 数据库 v13

目标：

- v12 → v13；
- daily_plans 与 daily_tasks；
- 原子“读取今日已有，否则生成并保存”；
- 同日一个有效计划；
- 顺序与完成状态跨重启保持；
- 不使用 REPLACE 误删任务。

DailyPlan 至少保存日期、创建时间、考试方案年、设置快照、内容版本和状态。DailyTask 至少保存稳定 ID、顺序、类型、内容/unit、预计时间、状态、遗留来源和时间戳。

退出证据：v13 schema、链式 migration、并发只生成一份、事务回滚、旧用户数据完整。

## C10 / PR-04C：跨日、遗留与显式重建

规则：

- 同日重启只读已有计划；
- 23:59 生成的计划不在 00:01 静默重排；
- 昨日未完成进入显式遗留集合；
- 设置、考试日期、内容版本变化默认次日生效；
- 只有用户明确动作才重建今天未完成任务；
- 已完成任务永不复活；
- carriedFromTaskId 防循环和重复；
- carry / skip / special-session 三种决定幂等；
- 时区固定 Asia/Taipei，时间源可注入。

退出证据：日期边界、进程重启、版本/设置变化、显式重建、完成保护、遗留幂等和事务失败测试；阶段边界全量通过。

## C11 / PR-05A：Today 内容页

目标：

- 新增职责单一的 feature:today；
- 只消费持久化 DailyPlan；
- 显示倒计时、预计时间、到期/修复/新学/输出/写作分组、一键继续、空态和完成总结；
- 暂不替换顶层导航；
- ViewModel 不复制 planner、不直接访问 DAO；
- 小文件、可测试 mapper/use case。

退出证据：loading、empty、partial、finished、infeasible、error、大字体、横屏 preview/测试；任务 callback 到旧入口正确。

## C12 / PR-05B：四段顶层导航

目标：今日 / 知识 / 训练 / 我的；Today 为冷启动目的地。

要求：

- 旧卡片、真题/论述、错题、设置作为子路由保留；
- 不删除旧 feature；
- 顶层状态 save/restore；
- 动态知识详情 A → B → C，返回 C → B → A；
- 卡片全屏共享 ViewModel 与返回不回归；
- 深链无历史 fallback；
- 旋转/进程恢复；
- 系统返回正确。

退出证据：导航策略测试、全部旧入口映射、阶段边界全量测试和不超过 10 分钟真机清单。

## C13 / PR-06A：拆分知识详情，行为不变

目标：将大详情页提取为 Recall、Outline、Explanation、Evidence、Relations 等小组件。

要求：

- 行为、顺序、文案事实、路由和查询不变；
- 先用 preview/测试锁定 before；
- 若预计超过 800 行，C13 内分两个纯重构 commit；
- 不趁机改视觉或数据。

退出证据：before/after 区块映射；行为变化明确为“无”；preview 与导航回归通过。

## C14 / PR-06B：主动回忆与分层学习

目标：

- 30 秒回忆；
- 2 分钟答题骨架；
- 考试表达；
- 理解与辨析；
- 证据与来源。

规则：

- 默认先回忆，再主动揭示；
- 缺内容诚实为空或降级；
- 不由 UI/AI 编造；
- reveal 状态旋转和进程恢复；
- 揭示答案不等于掌握；
- 数据来自现有内容、LearningUnit 和 provenance。

退出证据：reveal 前后、状态恢复、空层、真题跳转、大字体、长文本测试。

## C15 / PR-06C：显式关系与低可信 fallback

关系类型至少包括：

- COMPARE_WITH
- INFLUENCES
- INFLUENCED_BY
- PART_OF
- EVIDENCE_FOR
- EXAM_VARIANT

要求：

- 显式关系与 tag/字符串 fallback 分开；
- fallback 标“自动关联”，不回写 REVIEWED；
- 方向正确、去重、无悬空、可追踪原因；
- eq_0038 精确合题关系保留；
- 不恢复图谱画布；
- 无法确定方向时保持 UNKNOWN。

退出证据：重复、悬空、方向、跨科、eq_0038、A → B → C 返回链和 audit 全绿。

## C16 / PR-06D：三维进度

目标：显示“见过 / 记得 / 写得出”，取代含混单百分比。

证据源：

- 见过：明确学习/浏览记录；
- 记得：LearningUnit 到期与真实回忆表现；
- 写得出：真实 PracticeAttempt 或写作记录。

C16 时若 PracticeAttempt 尚未建立，“写得出”只能显示“尚未练习”或使用已有明确输出记录；不得偷建表或用浏览/卡片分数冒充。

退出证据：纯计算可复算、缺数据语义、解释文案、无虚假精确小数、大字体/读屏；阶段边界通过。

## C17 / PR-07A：PracticeAttempt 数据库 v14

架构决定：新增独立 PracticeAttempt 模型，不把一次作答硬塞进 wrong_answers、review_logs、TemplateFill 或 AiGradingRecord。

原因：这些旧表分别表达错题、记忆日志、模板填写和 AI 批改，无法完整、诚实地表达一次通用输出尝试。

至少保存：

- id、questionId、可选 pointId、可选 learningUnitId；
- sessionId、attemptType；
- 用户关键词、提纲、正文；
- startedAt、revealedAt、completedAt、elapsedMs；
- selfRating、errorReasons；
- repairState；
- createdAt、updatedAt。

错因固定：

- MEMORY_GAP
- CONCEPT_CONFUSION
- MISREAD_PROMPT
- WEAK_STRUCTURE
- WEAK_EVIDENCE
- TIME_CONTROL
- EXPRESSION

要求：v13 → v14、未知枚举安全、隐私与导出边界、旧用户数据链式迁移、仅数据层与测试。

## C18 / PR-07B：Training 薄容器

目标：聚合四种入口：

- 快速回忆；
- 真题作答；
- 610 写作；
- 错题修复。

只复用路由和已有业务，不复制卡片队列、错题查询、真题筛选或 DailyPlan 逻辑。

退出证据：四入口与返回、空态、横屏、大字体、深链 fallback、共享 ViewModel 风险测试。

## C19 / PR-07C：真题先作答后核对

状态机至少覆盖：

作答中 → 已保存未揭示 → 已揭示 → 已自评/错因 → 已完成。

要求：

- 空白答案不能标掌握；
- 用户主动揭示人工审校框架；
- 来源状态始终可见；
- 无可靠框架只保存作答并标待核；
- 旋转/杀进程恢复；
- 重复提交幂等；
- 错因形成后续修复候选；
- 修复任务默认次日进入，不打乱今天；
- 不改正式题干、答案或来源。

## C20 / PR-07D：专项 session 与错题修复

筛选维度：

- 年份；
- 历史科目代码；
- 题型；
- 学科；
- 考频；
- 薄弱点；
- 错因。

要求：

- 保留各年度 610/801/805/806/807 真实代码；
- 稳定排序、去重、空结果解释；
- session 进度可恢复；
- 专项 session 不重写正常 FSRS 到期；
- 完成后输出漏项/错因总结和建议修复。

若需要新持久表，优先在 v14 的 PracticeAttempt/session 设计中预留；不得在 C20 无计划占用新 migration。阶段边界全量通过。

## C21 / PR-08A：WritingSession 数据库 v15

架构决定：新增独立 WritingSession 模型；保留 TemplateFill、AnswerTemplate、WritingMaterial 和 AiGradingRecord 的原语义，不用 JSON 强塞一个长期写作状态机。

原因：

- TemplateFill 依赖必填 template_id 且表达一次模板填写；
- PracticeAttempt 适合一般输出尝试，不适合长期自动保存、计时和多阶段 610 写作；
- WritingSession 可以通过可选外键引用题目、模板和 PracticeAttempt，同时使用 ON DELETE SET_NULL 保护用户草稿。

至少保存：

- id、可选 examQuestionId、可选 templateId、可选 practiceAttemptId；
- mode、promptSnapshot；
- promptAnalysis、thesis、outlineJson、evidenceRefsJson、body；
- state；
- targetDurationMs、startedAt、elapsedBeforePauseMs、pausedAt；
- lastSavedAt、completedAt；
- selfAssessmentJson；
- createdAt、updatedAt。

要求：

- v14 → v15；
- 不批量修改 909 条旧材料；
- 旧材料保持 legacy note；
- REVIEWED 才能进入“可引用证据”；
- 链式 migration 与草稿用户数据保护。

## C22 / PR-08B：离线写作编辑器

目标：

- 审题、中心论点、分论点、证据卡、正文；
- 10 分钟提纲、30 分钟微写作、完整限时；
- debounce 自动保存；
- 保存失败提示与重试；
- 杀进程恢复；
- 暂停/恢复；
- 离开/放弃确认；
- 完全离线。

计时使用持久开始时间与单调推算，不每秒写数据库。Composable 不直接访问 DAO，不启动不可控全局 timer。

退出证据：自动保存、失败重试、进程恢复、三模式、暂停、系统时间变化、超长正文、旋转、大字体和离线测试。

## C23 / PR-08C：量规、自评、证据和历史对比

自评维度固定：

- 立意；
- 结构；
- 理论；
- 文本证据；
- 分析；
- 语言；
- 时间。

要求：

- 每维可解释等级与备注；
- 总分若显示，由维度透明计算，不冒充官方评分；
- REVIEWED 内容可作证据；
- LEGACY_UNVERIFIED 只作待核线索；
- 规则反馈只检查结构性事实，不宣称学术正确；
- 显示本次弱项、同维度历史趋势和后续任务；
- 自评先于任何未来 AI；
- 不接 API、不改 seed。

退出证据：无来源/混合来源、空证据、首次/多次历史、弱项任务、离线全流程和阶段边界通过。

## C24：MVP 闭环与最终自审

不新增功能。完成以下端到端审计：

1. 冷启动 Today；
2. 完成一个到期 LearningUnit；
3. 完成一个真题提纲；
4. 主动揭示并记录错因；
5. 完成一篇 610 微写作；
6. 完成本地量规自评；
7. 生成次日修复任务；
8. 同日重启计划不重排；
9. 杀进程恢复写作草稿；
10. 离线运行；
11. 来源 UNKNOWN 正确降级；
12. v10 旧用户链式升级到最终 schema；
13. seed/ID 集合无漂移。

最终命令至少包括：

~~~bash
python -m unittest discover -s tools/tests -p 'test*.py'

python -m tools.content_pipeline.audit_seed   --seed app/src/main/assets/seed_data.json   --schema content/schema/seed.schema.json   --baseline content/baselines/seed-baseline.json   --report /tmp/wenyan-seed-audit-1.json   --as-of-year 2026   --check

python -m tools.content_pipeline.audit_seed   --seed app/src/main/assets/seed_data.json   --schema content/schema/seed.schema.json   --baseline content/baselines/seed-baseline.json   --report /tmp/wenyan-seed-audit-2.json   --as-of-year 2026   --check

cmp /tmp/wenyan-seed-audit-1.json /tmp/wenyan-seed-audit-2.json
mise exec -- gradle testDebugUnitTest assembleDebug --no-daemon --stacktrace
git diff --check
git status --short
~~~

还必须运行：

- 每个 migration 的增量测试；
- v10 → 最终版本链式 migration test；
- 导航策略回归；
- DailyPlanner 稳定性测试；
- 写作进程恢复测试；
- 不超过 10 分钟的真机验收清单生成。

最后逐文件审查相对 main 的完整 diff，特别查：

- destructive migration；
- REPLACE/CASCADE 误删；
- seed、baseline、版本、签名、release workflow；
- 超大文件继续膨胀；
- 未使用代码和重复业务；
- 硬编码教材事实；
- 被标成 PASS 但未实际运行的测试。

## 9. 进度日志格式

docs/plans/CLOUD-MVP-PROGRESS.md 是本任务跨时段的唯一续跑日志。

每个检查点开始时记录：

- 当前 main 与 branch HEAD；
- 当前检查点；
- 拟修改文件；
- 风险；
- baseline 测试。

每个检查点结束时记录：

- commit SHA；
- 实际修改文件；
- 定向测试与真实结果；
- 全量测试状态；
- migration/ID/seed 不变量；
- 未运行项；
- 下一个检查点；
- 是否触发停止条件。

不要把大段编译日志写入仓库，只保留命令、退出状态、测试数量和关键摘要。

## 10. Draft PR 合同

标题建议：

CLOUD MVP: PR-01B through PR-08C study loop

正文必须包含：

- 目标与明确不做；
- 起始 main 与最终 HEAD；
- 按 C01–C23 的 commit/checkpoint 表；
- 数据库 v10 → 最终版本迁移图；
- 每个 schema 和 migration 证据；
- seed SHA、版本、数量和 ID 集合前后对比；
- 用户表保护结果；
- UI/导航/进程恢复证据；
- Python 审计、Gradle 测试、debug 构建和 instrumentation 的真实结果；
- 未运行项及原因；
- CI 状态；
- 真机验收清单；
- 风险与逐检查点回滚方式。

Draft PR 不得写“全部通过”，除非所有对应命令真实运行。CI 尚未完成时写 PENDING_CI。

## 11. 可直接发送的云端启动语

合并云端启动准备 PR 后，在 Codex Cloud 选择 qbjsdsb/wenyan-android 和最新 main；模型由 Codex Cloud 自动选择，不要尝试把 Luna 作为云端模型，也不要因无法手动更改默认模型而停工。发送：

~~~text
请在 Codex Cloud 中连续执行 docs/plans/CLOUD-MVP-EXECUTION.md。

从最新 origin/main 开始，先完成 C00 预检，识别并保留已经完成的 PR-00 和 PR-01A，从第一个未完成检查点 C01 / PR-01B 接续，持续执行到 C24 的 MVP 闭环验收。

这是一次连续实施授权。C01–C23 的普通检查点无需等待我再次发送“允许实施”；请自动依次完成只读核对、测试先行、最小实现、自审、原子提交、进度日志更新，然后进入下一个检查点。

只能在一个新的独立云端分支工作。不得直接写 main，不得合并、打 tag、发布、修改签名或 Secrets，不得使用 destructive migration，不得删除或重排既有 ID，不得清空用户数据，不得猜写教材/页码/真题/来源/答案，不得读取或提交 tools.zip，不得开始 PR-09。

严格执行文件中的停止条件。若平台时间不足，在最后一个已通过的原子检查点停下，更新 CLOUD-MVP-PROGRESS.md，并把未完成项明确标为 pending；不要把部分完成说成最终完成。

完成后创建一个 Draft PR，报告完整测试证据、migration 链、seed/ID 不变量、风险、回滚方式和真机验收清单。不要自行 Ready、合并或发布。
~~~

若平台中断后从同一分支续跑，发送：

~~~text
继续执行 docs/plans/CLOUD-MVP-EXECUTION.md。

先读取 docs/plans/CLOUD-MVP-PROGRESS.md、当前分支提交和完整 diff，验证最后一个 PASS 检查点仍然成立；不要重做已完成检查点。从第一个 PENDING 或 BLOCKED 检查点继续，仍遵守全部停止、数据、测试和发布边界。
~~~

## 12. 依据

本合同采用以下当前原则：

- Codex Cloud 在独立环境检出仓库、运行 setup、执行任务并在结束时提供 diff；
- setup 阶段可联网，agent 阶段默认关闭联网；
- 长任务必须明确结果、约束和验收，并在仓库中保留进度日志；
- AGENTS.md 保存永久规则，详细长计划放在独立执行文档；
- Room migration 必须依赖导出 schema 和实际升级测试，不使用 destructive fallback。
