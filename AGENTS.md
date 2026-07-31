# 文研 App — AI 协作入口

> **本文件是 AI 接手项目的第一入口。** Trae 云端模式不保留会话记忆，所有上下文依赖本文件与 `docs/` 目录。新会话开始时，请按"快速恢复"三步操作。

## 1. 项目概述

**文研 App** 是面向南京师范大学文学院现当代文学考研（050106）的深度专业课学习与背诵工具。核心理念：以真题为纲、以知识网络为本、以深度背诵为用。

**技术栈**：Kotlin 2.3.10 / Jetpack Compose（BOM 2025.12.00）/ Material 3 Expressive（material3 1.5.0-alpha18）/ Hilt 2.57.1 / Room 2.7.0 / FSRS-6 自实现 / 多模块架构（参考 Google Now in Android）

**仓库**：`qbjsdsb/wenyan-android`（private）

## 2. 快速恢复（新会话必读，3 步 5 分钟）

1. **读 [docs/00-STATUS.md](docs/00-STATUS.md)** — 10 秒知道当前状态与阻塞点
2. **读 [docs/01-QUICK-RECOVERY.md](docs/01-QUICK-RECOVERY.md)** — 了解会话开始/结束标准流程
3. **按需查阅**：
   - 有 CI 编译失败 → [docs/02-VERSION-MATRIX.md](docs/02-VERSION-MATRIX.md) + [docs/03-FAILED-ATTEMPTS.md](docs/03-FAILED-ATTEMPTS.md)
   - 要改 UI → [docs/design/m3-expressive-redesign.md](docs/design/m3-expressive-redesign.md) + [docs/plans/ksu-ui-upgrade.md](docs/plans/ksu-ui-upgrade.md)
   - 要跑 OCR 管线 → [docs/reference/OCR_PIPELINE.md](docs/reference/OCR_PIPELINE.md)
   - 不懂术语 → [docs/reference/GLOSSARY.md](docs/reference/GLOSSARY.md)
   - 上次进度 → [docs/SESSION_LOG.md](docs/SESSION_LOG.md) 最后一节
   - **看最新巡检报告** → [docs/auto-check/README.md](docs/auto-check/README.md) → 按时间倒序读最新报告

## 3. 文档地图

```
docs/
├── 00-STATUS.md                 # 当前状态快照（必读）
├── 01-QUICK-RECOVERY.md         # 快速恢复协议（必读）
├── 02-VERSION-MATRIX.md         # 版本兼容性矩阵（避坑）
├── 03-FAILED-ATTEMPTS.md        # 失败方案档案（不重复踩坑）
├── SESSION_LOG.md               # 会话日志（持续更新）
│
├── design/                      # 设计文档
│   ├── app-design.md            # App 主设计（定位/功能/架构）
│   ├── m3-expressive-redesign.md # M3 改造设计规格
│   └── code-fix-history.md      # 代码修复历史
│
├── plans/                       # 实现计划
│   ├── ksu-ui-upgrade.md              # KSU 风格 UI 升级计划（已完成 Phase 0-3）
│   ├── m3-expressive-implementation.md # M3 改造 26 Task（旧版）
│   ├── code-fix-implementation.md      # 代码修复计划
│   └── early-phase-plan.md             # 早期 Phase 1-7 计划
│
├── auto-check/                  # 自动巡检报告（每小时生成，AI 接手必读）
│   └── README.md                # 巡检机制说明 + 历史问题索引
│
├── research/                    # 调研文档
│   ├── exam-deep-research.md    # 考研深度调研
│   └── exam-second-round.md     # 考研二轮调研
│
└── reference/                   # 参考文档
    ├── ENVIRONMENT_SETUP.md     # 环境配置
    ├── OCR_PIPELINE.md          # OCR 管线运行手册
    ├── CI_CD_GUIDE.md           # CI/CD 工作流
    ├── RELEASE_PROCESS.md       # 发布流程
    ├── GLOSSARY.md              # 术语表
    └── PROJECT_REVIEW.md        # 项目全面复盘报告

tools/                           # Python 管线脚本
├── README.md                    # 管线使用说明
├── pipeline_runner.py           # OCR 批处理
├── rapidocr_pipeline.py         # RapidOCR
├── post_correct.py              # OCR 校对
├── extract_knowledge.py         # LLM 知识提取
├── cross_validate.py            # 交叉验证
├── generate_seed.py             # 生成 seed_data.json
├── scan_files.py                # 文件扫描
├── d_drive_env.py               # D 盘环境配置
├── requirements.txt             # Python 依赖
└── environment.yml              # conda 环境导出
```

## 4. 硬约束（不可违反）

- **所有代码和修改必须存储在 GitHub** — Trae 云端不保留记忆，仓库即记忆
- **PKCS12 keystore 要求 storepass = keypass** — 否则 Gradle Android 签名失败
- **Release 由 push tag 触发** — `git tag vX.Y.Z && git push origin vX.Y.Z`
- **Release 前删除旧 orphan tag** — 旧 tag 指向的 commit 不存在会导致失败
- **不修改 route 文件**（中间件重构时）
- **所有中间件使用 async/await**，不用 callback
- **使用 Koa 2.x**（Express 已弃用）
- **OCR 处理优先精度而非速度** — 核心教材 DPI=200，参考书 DPI=150
- **所有操作在 D 盘工作文件夹内**（`D:\wenyan`）
- **conda 环境 'ocr'**（Python 3.11.15）
- **PowerShell profile.ps1 不含 conda 初始化** — 防止终端执行失败
- **OCR 运行时不要跑 CPU 密集型 Python 任务** — 会拖慢 OCR
- **Android 开发是纯静态代码工作** — 不影响 OCR，可并行

### CI 相关硬约束（2026-07-12 新增）

- **pluginManagement 仓库顺序**：gradlePluginPortal / mavenCentral / google 优先，Aliyun 作 fallback — CI runner（美/欧）从 Aliyun 解析 plugin marker artifact 会失败（详见 [03-FAILED-ATTEMPTS.md #010](docs/03-FAILED-ATTEMPTS.md)）
- **MaxMetaspaceSize ≥ 1g** — Release 构建（R8 + Kotlin + Compose）需加载大量类，512m 会 OOM（详见 [#011](docs/03-FAILED-ATTEMPTS.md)）
- **CI 跑 testDebugUnitTest 而非 test** — `debugImplementation` 依赖只在 debug 变体可用，release 测试会因缺 ComponentActivity 声明失败（详见 [#012](docs/03-FAILED-ATTEMPTS.md)）
- **CI Gradle 版本与本地对齐** — 用 8.14.4（旧版 8.7 在解析 KSP 2.3.x 时有 bug）
- **KSP 2.3.2 而非 2.3.10** — KSP 2.3.10 调用 `AndroidComponentsExtension.addKspConfigurations`（AGP 8.8+ 才有），与 AGP 8.6.0 不兼容

### CI 验证策略（2026-07-13 新增）

**原则**：AI 自主判断每次改动是否需要 CI 验证，不冗余等待。

**必须等 CI 验证的场景**（push 后主动等结果）：
- 改了 `.github/workflows/*.yml` / `build.gradle.kts` / `gradle/libs.versions.toml` / `settings.gradle.kts`
- 改了签名配置或 keystore 相关
- 跨平台/跨 SDK 版本兼容性改动
- 准备发 Release tag 前（必须 CI 全绿才能 tag）

**不需要等 CI 的场景**（本地验证通过即可 push，CI 异步跑）：
- 纯 Kotlin/Compose 业务逻辑改动（ViewModel / Repository / Screen / Entity）
- 纯测试代码改动（新增/修改测试）
- 纯文档改动（docs/ 或 AGENTS.md）

**本地验证最低标准**（push 前必须通过）：
- `assembleDebug` SUCCESSFUL
- `testDebugUnitTest` 全绿（若有测试改动）

**Release tag 流程**：
1. 确认本地 `assembleDebug` + `testDebugUnitTest` 全绿
2. 确认最近一次 CI 全绿（gh run list 查看）
3. 删除旧 orphan tag（如有）：`git push origin :refs/tags/vX.Y.Z`
4. 打新 tag：`git tag vX.Y.Z && git push origin vX.Y.Z`
5. 等 Release workflow 完成，下载 APK 验证

## 5. 敏感信息（不入仓库）

| 信息 | 获取方式 |
|------|----------|
| GitHub token | 由用户提供，不写入仓库 |
| keystore 密码 | GitHub Secrets: `KEYSTORE_PASSWORD` |
| keystore 文件 | GitHub Secrets: `KEYSTORE_BASE64`（base64 编码） |
| key alias | GitHub Secrets: `KEY_ALIAS`（值为 `wenyan-release`） |
| key password | GitHub Secrets: `KEY_PASSWORD`（与 KEYSTORE_PASSWORD 相同） |
| LLM API key | 本地环境变量配置 |

## 6. AI 协作规则

- **每次会话结束前**更新 [docs/SESSION_LOG.md](docs/SESSION_LOG.md) 并 commit
- 遇到版本兼容问题 → 先查 [docs/02-VERSION-MATRIX.md](docs/02-VERSION-MATRIX.md)
- 遇到编译失败 → 先查 [docs/03-FAILED-ATTEMPTS.md](docs/03-FAILED-ATTEMPTS.md)，不重复已失败方案
- 发现新坑 → 补充到 `docs/03-FAILED-ATTEMPTS.md`
- 发现版本兼容信息 → 补充到 `docs/02-VERSION-MATRIX.md`
- commit message 说清"为什么改"，不只是"改了什么"
- 用户偏好：中文交流、严谨验证、反复检查、有趣的教学风格、M3 谷歌味道 UI

## 7. 当前状态（2026-07-31）

**✅ v0.9.6 关于与教程精简重构 + 代码卫生审计（已发布）** — 响应用户反馈"关于与教程界面做的太复杂了，排版也很难看，内容也太多了，竖屏的时候更是非常糟糕"，重构 AboutTutorialScreen.kt：7 章 430 行 → 5 节 ~384 行。新结构：1) HeroCard（欢迎卡：定位 + 三大理念）2) SectionQuickStart（快速上手：3 步入门）3) SectionModules（功能模块：5 个 Tab 简介）4) SectionPrinciples（学习原理：FSRS + 三档记忆，用 ExpandableInfoItem + AnimatedVisibility 可折叠）5) SectionAbout（关于：技术栈 + 致谢 + 免责声明）。默认视图简洁，深度原理按需展开。竖屏友好：MaxContentWidth.compact 限宽 + LazyColumn spacedBy(Spacing.xl)。同时修复 4 项代码卫生问题：CardsScreen.kt 弃用图标、FriendlyErrorMessage.kt 冗余 !!、CardsViewModel.kt 2 处 !!（改局部变量 currentError）、导航 Preview 移除已删除 graph 模块引用。本地验证：`:app:assembleDebug` + `:app:assembleRelease` + 全模块 `testDebugUnitTest` 全绿（403 tests, 0 failures）。**PRR ✅ READY TO RELEASE + RBR ✅ PASS**（per staff-engineer-mode Agent Event Policy）。Release v0.9.6 已发布（debug 签名 fallback — Exception E1）。Debug APK SHA-256 `36237a66d911d06cf21e45aba9b3c5394db7cbaf22a101417aea12ff712ff100` / 27,522,631 bytes / Release APK SHA-256 `8661d97bba625d837aa535df32ae6ab644906e12866b332d4a1144fb5b8d356c` / 19,169,788 bytes。

**✅ v0.9.5 关于与教程子路由（已发布）** — 设置页"关于"分组新增"关于与教程"入口，注册 ROUTE_ABOUT 子路由（Push/Pop slide + launchSingleTop 防双击压栈），加载 AboutTutorialScreen（7 章深度教程，430 行新文件）。教程覆盖：1) 软件定位与核心理念 2) 功能模块导览（5 个顶级 Tab）3) FSRS-6 间隔重复算法（4 大公式 + 4 状态调度 + 4 档评分 + ClockGuard）4) 三档记忆机制（EXACT 0.95 / FRAMEWORK 0.90 / UNDERSTAND 0.85）5) AI 助手与 RAG 架构（RAG + 苏格拉底三阶段 + 多服务商 + Prompt Injection 防护）6) 使用指南与学习路径（6 步入门 + 三阶段节奏）7) 技术信息与致谢。本地验证：`:app:assembleDebug` + `:app:assembleRelease` + 全模块 `testDebugUnitTest` 全绿（236 tests, 0 failures）。agent-pr-review ✅ Ready to merge（0 blocker, 0 must-fix）。**PRR ✅ READY TO RELEASE + RBR ✅ PASS**（per staff-engineer-mode Agent Event Policy）。Release v0.9.5 已发布（debug 签名 fallback — Exception E1）。Debug APK SHA-256 `0045a82d1ae318d2d504b73e8bb71bc13ee117d4354bdba60a914e968093eb58` / 27,522,631 bytes。

**✅ v0.9.4 错题本接入 FSRS 间隔重复调度（已发布）** — 为 wrong_answers 表添加 10 个 sched_* FSRS 调度字段，复用 FSRS-6 算法 + TIER_FRAMEWORK 档位（R_target=0.90），实现错题的间隔重复复习。5 层实现：数据层（Migration 7→8 + 10 字段 + 索引）+ 映射层（WrongAnswerSchedulingMapper）+ 仓库层（SchedulingRepository.rateWrongAnswer）+ ViewModel 层（DUE 过滤 + 评分委托 + ClockGuard 注入）+ UI 层（四档评分按钮 + 调度信息展示）。Follow-up #1 ClockGuard 注入 + #2 interval coerceAtLeast(0) 已修复。+10 单测。agent-pr-review ✅ Approved（0 blocker, 0 must-fix）。Release v0.9.4 已发布（debug 签名 fallback — Exception E1）。

- 最新 commit：（待 commit 后填入）
- 最新 Release：**v0.9.6**（2026-07-31 发布，debug 签名 fallback — Exception E1）— https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.6
- 本地验证（2026-07-31 v0.9.6 release）：`:app:assembleDebug` + `:app:assembleRelease` + 全模块 `testDebugUnitTest` 全绿（403 tests, 0 failures）
- versionCode / versionName：**31 / "0.9.6"**
- v0.9.6 APK 校验：Debug APK 27,522,631 bytes SHA-256 `36237a66d911d06cf21e45aba9b3c5394db7cbaf22a101417aea12ff712ff100`（debug 签名 Exception E1）/ Release APK 19,169,788 bytes SHA-256 `8661d97bba625d837aa535df32ae6ab644906e12866b332d4a1144fb5b8d356c`
- v0.9.6 Rollback target：[v0.9.5](https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.5)（uninstall v0.9.6 + install v0.9.5，versionCode 30 < 31 需卸载后安装）
- v0.9.6 receipt：[docs/release-receipts/v0.9.6-release-receipt.md](docs/release-receipts/v0.9.6-release-receipt.md)（含 PRR + RBR + agent-pr-review）
- v0.9.6 核心改动：
  - **AboutTutorialScreen.kt 精简重构**：7 章 430 行 → 5 节 ~384 行
    - HeroCard（欢迎卡：定位 + 三大理念，TonalCard + PrincipleRow）
    - SectionQuickStart（快速上手：3 步 GroupedCardItem）
    - SectionModules（功能模块：5 个 Tab GroupedCardItem）
    - SectionPrinciples（学习原理：FSRS + 三档记忆，ExpandableInfoItem + AnimatedVisibility 可折叠）
    - SectionAbout（关于：技术栈 + 致谢 + 免责声明）
    - 默认视图简洁，深度原理按需展开（rememberSaveable 持久化展开状态）
    - 竖屏友好：MaxContentWidth.compact 限宽 + LazyColumn spacedBy(Spacing.xl)
  - **代码卫生审计修复（4 项）**：
    - CardsScreen.kt: Icons.Filled.MenuBook → Icons.AutoMirrored.Filled.MenuBook（v0.9.5 漏修）
    - FriendlyErrorMessage.kt: e.message!!.contains → e.message?.contains == true（去冗余 !!）
    - CardsViewModel.kt: 2 处 _errorMessage.value!!.startsWith → 局部变量 currentError（去 !!）
    - 导航 Preview：移除已删除的 graph 模块引用，更新为错题本 Tab
- v0.9.5 核心改动：设置页"关于与教程"子路由 — ROUTE_ABOUT 常量 + aboutDestination（Push/Pop slide）+ SettingsScreen 入口项 + AboutTutorialScreen（7 章深度教程 430 行：定位/模块/FSRS-6/三档记忆/RAG/使用指南/致谢）+ Icons.AutoMirrored.Filled.MenuBook 弃用修复
- v0.9.5 receipt：[docs/release-receipts/v0.9.5-about-tutorial-pr-review.md](docs/release-receipts/v0.9.5-about-tutorial-pr-review.md)（agent-pr-review 结构化审查，0 blocker / 0 must-fix）
- v0.9.0 核心改动（5 Batch）：
  - **B1 章节树数据层**：ChapterDao.observeTree（WITH RECURSIVE CTE）+ countNonRootChapters + ChapterRepository/Impl + SeedDataLoader 基于文学时段生成二级章节树（subject → default_chapter → chapter_<tag>），seed 2.11.0→2.12.0
  - **B2 关联知识点模块增强**：RelationshipType 枚举（RELATED/CONTRAST/EXTENSION）+ 视觉编码（图标 + 颜色）+ RelatedPointItem（标题 + 摘要预览 + 考频/难度 chip + 右箭头）+ 3 主题 Preview
  - **B3 错题本升级为顶级 Tab**：TopLevelDestination.WrongAnswer（ROUTE_WRONG_ANSWER）替换原 Graph + WrongAnswerScreen onBack 可选（顶级模式无返回箭头）+ QuizScreen 移除 Inbox IconButton
  - **B4 移除 feature:graph 模块**：删除 feature/graph/ 整个目录（GraphScreen/GraphViewModel/GraphCanvas/GraphConstants/GraphLayout + 3 测试文件，~5000 行）+ app/build.gradle.kts 移除依赖 + settings.gradle.kts 移除 include
  - **B5 ProGuard 修复 + 文档**：core/data/consumer-rules.pro GraphSkeleton keep 路径修正（.graph. → .seed.）+ AGENTS.md/STATUS.md/SESSION_LOG.md 同步
- v0.9.0 保留设施（按 ADR-001 0.1 节"保留"清单，**已于 v0.9.3 优化 4 全部移除**）：原保留 core/database 图谱 DAO/Entity + core/data GraphRepository/Impl + GraphSkeleton + 算法服务（InterferenceWarner/WeakSubgraphDetector/PrerequisiteChecker），2026-07-28 验证全项目无 FSRS/ViewModel/UI 层调用后删除（28 文件，+1836/-4063 行，净减 2227 行）。详见 [docs/release-receipts/v0.9.3-opt4-graph-removal-receipt.md](docs/release-receipts/v0.9.3-opt4-graph-removal-receipt.md)
- v0.9.0 设计依据：[docs/design/adr-001-graph-removal.md](docs/design/adr-001-graph-removal.md) + [docs/plans/graph-removal-tree-migration.md](docs/plans/graph-removal-tree-migration.md)
- v0.9.1 核心改动：关联知识点模块不渲染 Hotfix — SeedDataLoader.computeRelatedIdsByTags（同 subject + 共享 tag → RELATED，按共享数降序取前 5）+ seed 2.12.0→2.13.0 + 8 单测
- v0.9.4 核心改动：错题本接入 FSRS 间隔重复调度 — 5 层实现（数据层 Migration 7→8 + 10 sched_* 字段 + 索引 / 映射层 WrongAnswerSchedulingMapper / 仓库层 SchedulingRepository.rateWrongAnswer / ViewModel 层 DUE 过滤 + 评分委托 + ClockGuard 注入 / UI 层四档评分按钮 + 调度信息展示）+ TIER_FRAMEWORK 档位 + 10 单测。Follow-up #1 ClockGuard 注入（DUE 过滤与评分调度时间源对齐）+ #2 interval coerceAtLeast(0) 下界保护已修复。agent-pr-review ✅ Approved（0 blocker, 0 must-fix, 1 pre-existing follow-up）
- v0.9.4 设计依据：[docs/design/adr-002-wrong-answer-fsrs.md](docs/design/adr-002-wrong-answer-fsrs.md)（方案 B：wrong_answers 表添加 sched_* 字段，不复用 memo_records 不新建表）
- v0.9.4 receipt：[docs/release-receipts/v0.9.4-receipt.md](docs/release-receipts/v0.9.4-receipt.md)（含 PRR + RBR + agent-pr-review + Post-Release Verification）
- v0.9.5 核心改动：设置页"关于与教程"子路由 — ROUTE_ABOUT 常量 + aboutDestination（Push/Pop slide）+ SettingsScreen 入口项 + AboutTutorialScreen（7 章深度教程 430 行：定位/模块/FSRS-6/三档记忆/RAG/使用指南/致谢）+ Icons.AutoMirrored.Filled.MenuBook 弃用修复
- v0.9.5 receipt：[docs/release-receipts/v0.9.5-about-tutorial-pr-review.md](docs/release-receipts/v0.9.5-about-tutorial-pr-review.md)（agent-pr-review 结构化审查，0 blocker / 0 must-fix）
- v0.9.4 APK 校验：Debug APK 27,489,863 bytes SHA-256 `b48d4f68...3a3364` / Release APK 19,169,788 bytes SHA-256 `02294bc7...62d955`（debug 签名 Exception E1）
- v0.9.4 Rollback target：[v0.9.1](https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.1)（uninstall v0.9.4 + install v0.9.1，versionCode 28 < 29 需卸载后安装）
- v0.8.18 核心改动：ic_launcher_foreground.xml v3 印章文（米色印面 + 墨黑"文"字 + 圆角 12dp）+ ic_launcher_monochrome.xml v3（Android 13+ themed icons）+ Logging.kt（Timber 封装）+ 20+ 文件 Log.d/.e → Logging.kt 迁移 + scripts/setup-env.sh（沙箱/云端/CI 通用）+ mise.toml（JDK 17.0.2 + Gradle 8.14.4）+ timber 5.0.1 依赖 + versionCode 25→26 + versionName "0.8.17"→"0.8.18"
- v0.8.18 工程化审查（per staff-engineer-mode Iron Law）：PRR ✅ + RBR ✅ + agent-pr-review ✅，详见 [docs/release-receipts/v0.8.18-receipt.md](docs/release-receipts/v0.8.18-receipt.md)
- v0.8.18 RBR Exception E1：CI 账单问题，本地构建 + gh 上传（与 v0.8.14-v0.8.17 一致，用户已接受）；CI 恢复后用正式 keystore 重新构建并替换 v0.8.18 asset
- v0.8.17 核心改动：staff-engineer-mode 三功能审计（知识点 + 错题本 + 知识卡片 retry-after-error Blocker 修复）+ 错误处理一致性 + 5 项 Must-Fix + 9 新测试，455 tests 全绿
- v0.8.1 核心改动：SeedDataLoader.importKnowledgeEntities() 自动入图（2123+ 节点）+ GraphLayout.kt 三模式布局 + GraphConstants.kt 常量集中 + NodeShape 形状编码 + EDGE_TYPE_LABELS 边语义化 + GraphViewModel LayoutMode 切换 + GraphCanvas drawNodeShape + GraphScreen 可折叠 LegendBar + seed 2.9.0→2.11.0
- CI 阻塞：GitHub Actions 账单问题，38+ commit 待 CI 验证（不影响 Release，已通过本地构建 + gh 上传绕过）
- 详见 [docs/00-STATUS.md](docs/00-STATUS.md) + [docs/SESSION_LOG.md](docs/SESSION_LOG.md) 最后一节

## 8. 项目阶段总览

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 1 资料数字化 | 约 60% | OCR 处理 125/208 文件 |
| Phase 2 Android 骨架 | ✅ 完成 | 多模块架构 + 数据库 |
| Phase 3 FSRS 调度 | ✅ 完成 | FSRS-6 自实现 + 三层记忆 |
| Phase 4 AI 服务 | ✅ 完成 | OpenAI 兼容协议 |
| Phase 5 UI 增强 | ✅ 完成 | 9 个 Screen + M3 组件 |
| Release 配置 | ✅ 完成 | 签名 + GitHub Release v0.1.0 + v0.2.0 |
| KSU 风格 UI 升级 | ✅ 完成 | Phase 0-3 + CI 修复，已合并 main |
| UI 改造闭环 | ✅ 完成 | GroupedCard 增强 + 2 Screen 重构 + 4 Preview + 15 组件测试 |
| UI 统一与死组件清理 | ✅ 完成 | KnowledgePointDetailScreen 统一 + 删除 4 个死组件（174 tests） |
| P0 双修 | ✅ 完成 | release.yml CI 修复 + SeedDataLoader 接通（App 启动自动导入种子数据） |
| P1 修复 | ✅ 完成 | KnowledgeViewModel 科目筛选 + 科目名显示修复（DAO JOIN + 10 测试，184 tests） |
| Release v0.2.0 | ✅ 完成 | 签名 APK 发布，包含自 v0.1.0 以来所有改动 |
| UI 精修 v0.3 | ✅ 完成 | 卡片镜像修复 + 导师信息删除 + AI 入口调整 + 全面动画优化（190 tests） |
| 第三轮深度审计 v0.4.2 | ✅ 完成 | 4 Batch 修复：FSRS 算法 4 bug + 数据安全 7 项 + 测试有效 3 项 + UX/契约 10+ 文件（207 tests） |
| 第四轮深度审计 v0.5.0 | ✅ Phase 2 P1/P2 修复完成 | 13 commits，59 项修复，Release v0.3.0 已发布 |
| v0.6 M3 Expressive 精修 | ✅ 完成 | 5 commits：导航重构 + 动效字体 + 大屏自适应 + 组件升级 + 视觉精修 |
| 第五轮深度审计 P0/P1/P2 | ✅ 完成 | 5 commits：Converter 降级 + rateCard 事务 + Flow 异常 + LIKE 转义 + ContentSource 迁移 + 死代码清理 |
| P1 大型任务 5 Wave | ✅ 完成 | 7 commits：schema v5 + Float 精度 + 复习日志 + AI 对话持久化 + 错题本 + ProGuard 规则（258 tests） |
| Release v0.7.0 | ✅ 完成 | 接入 909 知识点 + study_text + seed v2.1.0 |
| Release v0.7.2 | ✅ 完成 | 修复 GraphSkeleton FK 回滚导致知识点全部丢失，seed v2.2.0 触发重新导入 |
| 沙箱编译验证 v0.7.2 | ✅ 完成（2026-07-23） | 补齐 gradlew wrapper + 修复 CardsViewModelTest + assembleDebug + 258 tests 全绿 |
| v0.7.4 用户体验深度修复 | ✅ 完成 | 4 道合并题拆分 + 2022年806答案错位修复 + OCR 清洗 + GraphCanvas 重写 + UI 修复，seed 2.4.0→2.7.0 |
| v0.7.5 610 综合卷科目深度修复 | ✅ 完成（2026-07-23） | 610 综合卷 127 题科目重新分类（古代36/现当代32/外国26/理论33），seed 2.7.0→2.8.0 |
| v0.7.6 数据瘦身 + 知识图谱时间轴布局 | ✅ 完成（2026-07-24） | 删除 multi_perspectives/sample_essay 冗余字段 + DB v5→v6 迁移 + GraphCanvas 重构为文学史时间轴泳道布局（4 泳道 + 35 跨类边 + 7 时段节点），seed 2.8.0→2.9.0 |
| v0.8.1 知识图谱三模式重构 + 形状编码 | ✅ 完成（2026-07-24） | 图谱覆盖率 4.4%→100%（910 知识点自动入图，2123+ 节点 968+ 边）+ 三模式布局（时间轴/邻域力导向/径向）+ 形状编码（圆/方/菱/三角/星）+ 边语义化（12 中文标签 + 线型）+ LegendBar 可折叠，seed 2.9.0→2.11.0 |
| v0.8.2-v0.8.17 多轮深度审计 + UI 打磨 | ✅ 完成（2026-07-24 → 2026-07-27） | 图谱闪退修复 + UI/UX 深度打磨（AMOLED + 无障碍 + 动画）+ 知识卡片功能深度修复（FSRS 调度 + sibling 卡 + Leech 警告）+ stark UI 审计 + retry-after-error Blocker 修复，455 tests 全绿 |
| v0.8.18 启动图标 v3 + Logging.kt + 发布 | ✅ 完成（2026-07-27） | App 启动图标 v3 "印章文"（米色印面 + 墨黑"文"字 + M3E 圆角 12dp + monochrome 适配 Android 13+）+ Logging.kt 统一日志门面（Timber 封装 + 20+ 文件迁移）+ scripts/setup-env.sh + mise.toml 工具链锁定，versionCode 25→26，450 tests 全绿，本地构建 + gh 上传（Exception E1 debug 签名） |
| v0.9.0 知识图谱移除 + 章节树 + 错题本升级 | ✅ 完成（2026-07-27） | 按 ADR-001 5 Batch 迁移：B1 章节树数据层 + B2 关联知识点模块增强 + B3 错题本升级为顶级 Tab + B4 移除 feature:graph 模块（~5000 行删除）+ B5 ProGuard 修复 + 文档。保留 core 层图谱基础设施（算法服务消费）。403 tests 全绿，待 Release |
| v0.9.1 关联知识点模块不渲染 Hotfix | ✅ 完成（2026-07-28） | SeedDataLoader.computeRelatedIdsByTags（同 subject + 共享 tag → RELATED）+ seed 2.12.0→2.13.0 + 8 单测。Release v0.9.1 已发布（debug 签名 fallback） |
| v0.9.4 错题本接入 FSRS 间隔重复调度 | ✅ 完成 + 已发布（2026-07-28） | 5 层实现：数据层 Migration 7→8 + 10 sched_* 字段 + 索引 / 映射层 WrongAnswerSchedulingMapper / 仓库层 SchedulingRepository.rateWrongAnswer / ViewModel 层 DUE 过滤 + 评分委托 + ClockGuard 注入 / UI 层四档评分按钮 + 调度信息展示。TIER_FRAMEWORK 档位。10 单测。Follow-up #1 ClockGuard 注入 + #2 interval coerceAtLeast(0) 已修复。agent-pr-review ✅ Approved（0 blocker, 0 must-fix）。Release v0.9.4 已发布（debug 签名 Exception E1） |
| v0.9.5 关于与教程子路由 | ✅ 完成 + 已发布（2026-07-28） | 设置页"关于"分组新增"关于与教程"入口，注册 ROUTE_ABOUT 子路由（Push/Pop slide + launchSingleTop 防双击压栈），加载 AboutTutorialScreen（7 章深度教程 430 行：定位/模块/FSRS-6/三档记忆/RAG/使用指南/致谢）。Icons.AutoMirrored.Filled.MenuBook 弃用修复。本地验证全绿（236 tests）。agent-pr-review ✅ Ready to merge。**PRR ✅ READY TO RELEASE + RBR ✅ PASS**（per staff-engineer-mode Agent Event Policy）。Release v0.9.5 已发布（debug 签名 Exception E1）。APK SHA-256 `0045a82d...93eb58` / 27,522,631 bytes |
| v0.9.6 关于与教程精简重构 + 代码卫生审计 | ✅ 完成 + 已发布（2026-07-31） | 响应用户反馈重构 AboutTutorialScreen.kt：7 章 430 行 → 5 节 ~384 行（HeroCard / QuickStart / Modules / Principles 可折叠 / About）。默认视图简洁，深度原理用 ExpandableInfoItem + AnimatedVisibility 按需展开。竖屏友好：MaxContentWidth.compact 限宽。同时修复 4 项代码卫生问题：CardsScreen.kt 弃用图标、FriendlyErrorMessage.kt 冗余 !!、CardsViewModel.kt 2 处 !!、导航 Preview 移除已删除 graph 模块引用。本地验证全绿（403 tests）。**PRR ✅ READY TO RELEASE + RBR ✅ PASS**。Release v0.9.6 已发布（debug 签名 Exception E1）。Debug APK SHA-256 `36237a66...2ff100` / 27,522,631 bytes / Release APK SHA-256 `8661d97b...8d356c` / 19,169,788 bytes |

## 9. 下一步优先级

1. **P0**：跑 emulator 实测 v0.9.6（已发布）— 验证设置 → 关于 → 关于与教程入口可见可点击 + 5 节结构渲染（HeroCard / QuickStart / Modules / Principles 可折叠 / About）+ ExpandableInfoItem 点击展开/收起动画流畅 + 默认视图简洁（深度原理折叠态）+ 竖屏友好（MaxContentWidth.compact 限宽）+ 横屏/平板内容居中 + 返回箭头返回设置页
2. **P0**：跑 emulator 实测 v0.9.4 — 验证错题本 DUE 过滤模式 + 四档评分按钮（不会/困难/良好/简单）+ 调度信息展示（下次复习/复习次数/遗忘次数）+ Migration 7→8 升级（已有错题 sched_* 字段默认值正确）+ ClockGuard 时间源对齐（评分后错题不会立即重新出现在 DUE 列表）
3. **P0**：跑 emulator 实测 v0.9.1（若未测）— 验证关联知识点模块渲染（RelatedPointsSection 应有关联知识点列表）+ 关联知识点点击跳转 + seed 2.13.0 触发重导后 relatedIds 正确填充
4. **P0 阻塞**：等待 GitHub Actions 账单问题解决 — 40+ commit 待 CI 验证（不影响 Release，已通过本地构建 + gh 上传绕过）
5. **P0**：CI 账单问题解决后，重新用正式 keystore 构建 release APK 并替换 v0.9.4 + v0.9.5 + v0.9.6 asset（消除 Exception E1）
6. **P1**：启用 R8（P1-PG 规则已就绪 + B5.1 GraphSkeleton 路径已修正，需 emulator 实测验证无崩溃后切换 isMinifyEnabled=true）
7. **P2 优化项（非阻塞）**：`app/build.gradle.kts` 第 71 行 release keystore fail-fast 应移到 task 执行阶段（当前在配置阶段抛异常，沙箱跑 debug 任务也触发，需 `unset CI` 绕过）
8. **P1 Phase 2 剩余维度审计**：
   - 2.E 剩余：strings.xml 完整性（NF-U2）、dimens.xml（NF-C10）
   - 2.L：错误处理一致性 + 日志规范（sealed AppError + Timber + Snackbar 统一） — v0.8.18 已完成 Timber 引入，剩 sealed AppError + Snackbar 统一
   - 2.M：Compose 副作用 + Accessibility + M3 Expressive
   - 2.N 剩余：NF-DS7-13 DataStore Key 治理
9. **P2**：OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（替换 stage2-sample）
10. **P3**：release.yml "Verify keystore" 步骤隐藏 bug（Line 63-70，KEYSTORE_BASE64 未配置时失败）— **已修复**：P1-S-1 修正已在 "Decode keystore" 步骤添加 fail-fast（exit 1 if KEYSTORE_BASE64 empty），见 [.github/workflows/release.yml](.github/workflows/release.yml) L60-65
11. **P4**：架构重构 — getVerifiedWithSubject 职责应在 KnowledgeRepository — **已完成**：方法已迁移至 [KnowledgeRepository.kt:116](core/data/src/main/java/com/wenyan/app/core/data/repository/KnowledgeRepository.kt#L116)，ReviewRepository 仅保留 doc 引用
