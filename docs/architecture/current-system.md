# 当前系统基线

> **状态：** PR-00 文档基线（2026-08-09）。
>
> **基线 commit：** `c1df65e860bc1e9f9deb046d63f4a05ac14f2883`（`main` 与 `origin/main` 一致）。
>
> 本文只描述该 commit 中已经存在的系统事实，不引入产品行为。数字是快照；仓库变化后必须按“复算命令”重新计算，不能把本文手填数字当成新的数据源。

## 1. 复算环境与统一口径

以下命令均从仓库根目录执行：

~~~bash
# Git 基线
git rev-parse HEAD
git branch --show-current

# App 版本（统计 versionName/versionCode 所在的 defaultConfig）
rg -n 'versionCode =|versionName =' app/build.gradle.kts

# Room 版本（schema 版本来自 @Database；依赖版本来自 version catalog）
rg -n 'version = [0-9]+' core/database/src/main/java/com/wenyan/app/core/database/WenyanDatabase.kt
rg -n '^room = ' gradle/libs.versions.toml

# seed 内容统计：统计 app/src/main/assets/seed_data.json 的数组，不是运行后 Room 行数
jq -r '"seed=\(.metadata.version)", "subjects=\(.subjects|length)", "knowledge_points=\(.knowledge_points|length)", "exam_questions=\(.exam_questions|length)", "essay=\([.exam_questions[] | select(.question_type == "ESSAY")] | length)", "writing_materials=\(.writing_materials|length)"' \
  app/src/main/assets/seed_data.json

# 知识点按 seed.subject 分组；这是内容数组口径，不是章节树节点口径
jq -r '.knowledge_points | group_by(.subject)[] | "\((.[0].subject))=\(length)"' \
  app/src/main/assets/seed_data.json

# 论述题 angle/notes：非 null 字段计数；两者都具备的题目另计
jq -r '[.exam_questions[] | select(.question_type == "ESSAY")] |
  {total: length,
   angle: (map(select(.angle != null)) | length),
   notes: (map(select(.notes != null)) | length),
   both: (map(select(.angle != null and .notes != null)) | length)}' \
  app/src/main/assets/seed_data.json

# JVM unit-test 静态基线：只统计 app/src/test、core、feature 中的 @Test，
# 明确排除 app/src/androidTest；这是注解数量，不替代实际测试执行结果。
rg -o '@Test\b' --glob '*.kt' app/src/test core feature | wc -l

# 模块与路由声明数量；下面的表格按这些声明逐项列出
rg '^include\(' settings.gradle.kts | wc -l
rg '^    data object ' app/src/main/java/com/wenyan/app/navigation/TopLevelDestination.kt | wc -l
rg '^\s*route = ' app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt | wc -l

# Room 实体数量：WenyanDatabase.entities 列表是最终口径
rg -n '^\s*[A-Za-z]*Entity::class,' \
  core/database/src/main/java/com/wenyan/app/core/database/WenyanDatabase.kt | wc -l
~~~

当前复算结果：

| 项目 | 当前值 | 统计口径 |
| --- | ---: | --- |
| App | v0.9.43 / versionCode 68 | `app/build.gradle.kts` 的 `defaultConfig` |
| Room schema | v10 | `WenyanDatabase.kt` 的 `@Database(version = 10)` |
| Room 依赖 | 2.7.0 | `gradle/libs.versions.toml` 的 `room` 版本 |
| seed | v2.26.0 | `seed_data.json.metadata.version` |
| 科目 | 4 | `seed_data.json.subjects` 数组长度 |
| 知识点 | 1101 | `seed_data.json.knowledge_points` 数组长度 |
| 真题 | 564 | `seed_data.json.exam_questions` 数组长度 |
| 其中 ESSAY | 142 | `question_type == "ESSAY"` 的真题数量 |
| 写作材料 | 909 | `seed_data.json.writing_materials` 数组长度 |
| JVM unit-test 静态计数 | 636 | `app/src/test`、`core`、`feature` 中的 `@Test` 注解；不含 instrumentation test |
| Room 实体表 | 19 | `WenyanDatabase.entities` 列表 |

知识点按 `seed.subject` 的复算结果为：中国古代文学 498、中国现当代文学 256、外国文学 157、文学理论 190。论述题的 `angle`、`notes` 均为 134/142，二者同时存在的题目为 134/142。上述每个数字均以本节命令的数组筛选口径为准。

## 2. Gradle 子项目与职责

声明来源：[settings.gradle.kts](../../settings.gradle.kts)；`build-logic` 是 included build，不计入下列 Gradle 子项目。数量以本节 `rg '^include\(' settings.gradle.kts | wc -l` 复算。

| 子项目 | 职责 |
| --- | --- |
| `:app` | Android 应用壳、Hilt 组装、导航与 seed asset |
| `:core:common` | 共享基础类型与工具 |
| `:core:database` | Room 数据库、实体、DAO、迁移和 schema |
| `:core:data` | Repository、seed 导入、内容映射和业务数据流 |
| `:core:designsystem` | Compose Material 3 设计系统与通用组件 |
| `:core:fsrs` | FSRS-6 调度与考试倒计时 |
| `:core:ai` | AI/RAG/苏格拉底引导等可选能力 |
| `:feature:knowledge` | 知识点、章节、论述题和真题背题界面 |
| `:feature:quiz` | 错题本界面 |
| `:feature:cards` | 卡片复习、评分和全屏复习 |
| `:feature:aiassistant` | AI 助手与 API 配置入口 |
| `:feature:settings` | 设置、关于教程和更新检查 |

当前不存在 `:feature:graph`；它不应被当作现有模块或当前构建依赖。

## 3. 顶层导航与子路由

顶层目的地来源：[TopLevelDestination.kt](../../app/src/main/java/com/wenyan/app/navigation/TopLevelDestination.kt)：

| 顺序 | route | 用户入口 |
| ---: | --- | --- |
| 1 | `knowledge` | 知识点 |
| 2 | `essay` | 论述题 |
| 3 | `cards` | 卡片 |
| 4 | `wrong_answer` | 错题本 |
| 5 | `settings` | 设置 |

当前注册的子路由来源：[WenyanNavHost.kt](../../app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt)：

| route | 用途 |
| --- | --- |
| `quiz_practice` | 名词解释/简答真题背题列表 |
| `quiz_practice_detail/{questionId}?type={type}&subject={subject}&year={year}` | 真题背题详情 |
| `knowledge_detail/{pointId}` | 知识点详情；详情 A→B→C 保留返回路径 |
| `essay_detail/{examQuestionId}` | 论述题详情 |
| `cards_fullscreen` | 卡片沉浸式全屏复习 |
| `aiassistant` | AI 助手 |
| `api_config` | AI API 配置 |
| `about` | 关于与教程 |
| `update_check` | 更新检查 |

关键导航不变量：

- 顶层 Tab 只有上述 5 个；错题本占据原图谱位置。
- 知识点详情内部跳转到不同 `pointId` 必须正常入栈；同一点重复点击才可跳过。
- `feature:graph`、Graph 顶级 route 和当前图谱 UI 入口均不存在。

## 4. Room 表与数据边界

数据库声明来源：[WenyanDatabase.kt](../../core/database/src/main/java/com/wenyan/app/core/database/WenyanDatabase.kt)；实体表名来源：[entity 目录](../../core/database/src/main/java/com/wenyan/app/core/database/entity/)。

当前 19 个实体表如下：

~~~text
subjects
chapters
knowledge_points
exam_questions
memo_records
study_progress
writing_materials
api_configs
ai_grading_records
answer_templates
template_fills
writing_patterns
review_logs
exam_code_history
data_sources
app_meta
chat_conversations
chat_messages
wrong_answers
~~~

表的边界：

- 内容与来源：`subjects`、`chapters`、`knowledge_points`、`exam_questions`、`writing_materials`、`data_sources`。
- 复习与用户记录：`memo_records`、`study_progress`、`review_logs`、`wrong_answers`。
- 写作：`answer_templates`、`template_fills`、`writing_patterns`。
- 应用、AI 与兼容元数据：`api_configs`、`ai_grading_records`、`exam_code_history`、`app_meta`、`chat_conversations`、`chat_messages`。

图谱表 `graph_nodes`、`graph_edges` 已在历史迁移中移除，不属于 v10 当前 schema。

## 5. Seed 导入与不可破坏不变量

实现来源：[SeedDataLoader.kt](../../core/data/src/main/java/com/wenyan/app/core/data/seed/SeedDataLoader.kt) 及相关 DAO。

当前导入器的可验证约束：

1. `metadata.version` 与导入器 schema version 分开保存；当前导入器 schema 常量为 3。
2. 导入步骤置于 `WenyanDatabase.withTransaction` 内；任一步失败时不应留下已标记完成的半成品导入。
3. seed 管理的科目、章节、知识点和真题使用稳定 ID；这些 DAO 当前使用 `@Upsert`。写作材料 DAO 仍使用带替换冲突策略的插入，后续若增加其子表必须另加数据保护测试。
4. seed 升级时，已有 `MemoRecord` 按 `point_id` 跳过，只为新增知识点建立初始记录；既有 FSRS 字段不由 seed 初始值覆盖。
5. 导入器不写入用户的 `study_progress`、`review_logs`、`wrong_answers`、`template_fills`、`api_configs` 和聊天记录；seed 来源清理仅针对受管理的 `seed-kp-source:` 前缀。
6. 当前 PR-00 不改 seed、Room、Kotlin 或用户数据；任何后续 schema/seed 改动必须保留旧 ID、补迁移/升级测试，并证明用户记录不丢失。

可复核实现细节：

~~~bash
rg -n 'CURRENT_SEED_IMPORT_SCHEMA_VERSION|withTransaction|missingMemoRecords' \
  core/data/src/main/java/com/wenyan/app/core/data/seed/SeedDataLoader.kt
rg -n '@Upsert|insertAll' \
  core/database/src/main/java/com/wenyan/app/core/database/dao/{SubjectDao,ChapterDao,KnowledgePointDao,ExamQuestionDao,MemoRecordDao}.kt
~~~

## 6. CI 与本地验证入口

当前 CI 来源：[android.yml](../../.github/workflows/android.yml) 与 [release.yml](../../.github/workflows/release.yml)：

~~~bash
# 主 CI：先测试，再构建 Debug
gradle testDebugUnitTest --no-daemon --stacktrace
gradle assembleDebug --no-daemon --stacktrace

# Release workflow：先测试，再构建签名 Release
gradle testDebugUnitTest --no-daemon --stacktrace
gradle assembleRelease --no-daemon --stacktrace
~~~

本地 PR 验收可使用：

~~~bash
./gradlew testDebugUnitTest assembleDebug
git diff --check
~~~

本地 wrapper 的网络、缓存或权限失败不能写成“代码测试失败”；报告中必须区分“未进入构建”“构建失败”和“测试失败”。

## 7. 当前决策索引

- [离线优先](../decisions/001-offline-first.md)：本地 Room/seed 是内容与复习的默认真相源，网络能力可选。
- [稳定内容 ID](../decisions/002-stable-content-ids.md)：不重排、不复用既有内容 ID，升级保护用户记录。
- [AI/OCR 审核闸门](../decisions/003-ai-ocr-review-gate.md)：AI/OCR 草稿不得直接进入正式 seed。
- [知识图谱暂缓](../decisions/004-knowledge-graph-deferred.md)：当前使用章节树和关联知识点，不恢复图谱 UI。
