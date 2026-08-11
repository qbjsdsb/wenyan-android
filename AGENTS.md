# 文研 App — AI 协作入口

> **本文件是 AI 接手项目的第一入口。** Trae 云端模式不保留会话记忆，所有上下文依赖本文件与 `docs/` 目录。新会话开始时，请按"快速恢复"三步操作。

## 1. 项目概述

**文研 App** 是面向南京师范大学文学院现当代文学考研（050106）的深度专业课学习与背诵工具。核心理念：以真题为纲、以知识网络为本、以深度背诵为用。

**技术栈**：Kotlin 2.3.10 / Jetpack Compose（BOM 2025.12.00）/ Material 3 Expressive（material3 1.5.0-alpha18）/ Hilt 2.57.1 / Room 2.7.0 / FSRS-6 自实现 / 多模块架构（参考 Google Now in Android）

**仓库**：`qbjsdsb/wenyan-android`（public）

> **当前事实来源（2026-08-12）**：PR #21 已合并，当前 `main` 为 `3010107c`；本轮 follow-up 分支从该基线建立，用于修复 PR #21 自动审查指出的两个 P2 边界问题并补强 APK 文件识别。新任务以 [docs/00-STATUS.md](docs/00-STATUS.md)、[当前系统基线](docs/architecture/current-system.md)、仓库实际代码和 `.github/workflows/` 为准。当前代码为 v0.9.45 / Room 15 / 4 个顶层入口 / 25 张表。本文件后半部保留的大段旧版本、D 盘、CodeBuddy、OCR 和图谱记录仅用于历史恢复，不能覆盖这些当前来源。

> **Codex Cloud 连续模式**：只有用户明确要求执行 [CLOUD-MVP-EXECUTION.md](docs/plans/CLOUD-MVP-EXECUTION.md) 时，才允许按其中 C00–C24 在一个独立分支连续推进。该模式只免除普通检查点重复等待用户授权；稳定 ID、用户数据、显式 migration、真实测试、来源证据以及禁止 Ready/合并/tag/发布等规则全部继续生效。续跑前先读 [CLOUD-MVP-PROGRESS.md](docs/plans/CLOUD-MVP-PROGRESS.md)。

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
├── architecture/
│   └── current-system.md        # PR-00 可复算系统基线
├── decisions/                   # 当前架构与数据保护决定
│
├── design/                      # 设计文档
│   ├── app-design.md            # App 主设计（定位/功能/架构）
│   ├── m3-expressive-redesign.md # M3 改造设计规格
│   └── code-fix-history.md      # 代码修复历史
│
├── plans/                       # 实现计划
│   ├── WENYAN-MASTER-PLAN.md          # 当前总体实施顺序与永久不变量
│   ├── PR-01A.md                       # 已完成的只读 seed 审计合同
│   ├── PR-01B.md                       # 当前第一个未完成工单：CI 门禁
│   ├── CLOUD-MVP-EXECUTION.md          # 显式授权后使用的云端连续执行合同
│   ├── CLOUD-MVP-PROGRESS.md           # 云端任务原子检查点与续跑日志
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

### 4.1 当前仓库与 Android 约束

- **所有代码和修改必须存储在 GitHub** — Trae 云端不保留记忆，仓库即记忆
- **PKCS12 keystore 要求 storepass = keypass** — 否则 Gradle Android 签名失败
- **Release 由 push tag 触发** — `git tag vX.Y.Z && git push origin vX.Y.Z`
- **只处理已确认的同名失败 tag** — 删除或移动 tag 前必须解析准确 tag 和 commit；禁止批量删除或凭旧记录删除 tag

### 4.2 历史 Windows/OCR/Node 工作站约束

> 以下规则只在任务明确维护旧 Windows OCR 或历史 Node 中间件时适用。普通 Android、文档、CI 和内容审计任务不得假定存在 `D:\wenyan`、`ocr` conda 环境、Koa 服务或旧沙箱配置。

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

**本地验证最低标准**（push 前必须如实报告）：
- Android 源码、依赖或构建配置改动：`assembleDebug` SUCCESSFUL
- 业务逻辑或测试改动：相关测试及 `testDebugUnitTest` 全绿
- 纯文档改动：运行文档/链接/复算门禁与 `git diff --check`；Android 构建可以不重复运行，但必须说明未运行原因并以远程 CI 为最终补充证据

**Release tag 流程**：
1. 确认本地 `assembleDebug` + `testDebugUnitTest` 全绿
2. 确认最近一次 CI 全绿（gh run list 查看）
3. **在 `CHANGELOG.md` 顶部写本版本更新日志**（`## [vX.Y.Z] - 日期`，格式见文件头部说明）并提交推送——release.yml 会自动读取该段作为 Release 正文，App 内"检查更新"界面同步展示。**务必写好，不要遗漏**
4. 提升 `app/build.gradle.kts` 的 `versionCode` / `versionName`（与 CHANGELOG 版本一致）并提交推送
5. 删除旧 orphan tag（如有）：`git push origin :refs/tags/vX.Y.Z`
6. 打新 tag：`git tag vX.Y.Z && git push origin vX.Y.Z`
7. 等 Release workflow 完成，下载 APK 验证版本号与更新日志

## 5. 敏感信息（不入仓库）

| 信息 | 获取方式 |
|------|----------|
| GitHub token | 由用户提供，不写入仓库 |
| keystore 密码 | GitHub Secrets: `KEYSTORE_PASSWORD` |
| keystore 文件 | GitHub Secrets: `KEYSTORE_BASE64`（base64 编码） |
| key alias | GitHub Secrets: `KEY_ALIAS`（值为 `wenyan-release`） |
| key password | GitHub Secrets: `KEY_PASSWORD`（与 KEYSTORE_PASSWORD 相同） |
| LLM API key | 本地环境变量配置 |

### 历史沙箱推送通道（2026-08-02，CodeBuddy 环境）

> **ARCHIVED**：下述主机、凭据位置和有效期只描述当时的 CodeBuddy 沙箱。新环境不得据此假定凭据存在、读取旧凭据文件或改写 remote；应先检查当前 Git remote 与已授权的 GitHub 连接。

**背景**：沙箱网络无法直连 github.com（TLS 握手被中间设备掐断），api.github.com / SSH 亦不可达。经排查，**ghfast.top 镜像可透传 git 协议（含 git-receive-pack 写操作）**，配合 GitHub PAT 可正常 clone / push / 打 tag。

**用法**（已配置到本地仓库，新会话直接用）：

```bash
git pull origin main
git push origin main
git tag vX.Y.Z && git push origin vX.Y.Z   # 打 tag 触发 Release
```

**认证**：GitHub PAT（ghp_ 开头，classic）由用户提供，存于沙箱 ~/.git-credentials + 环境变量 GITHUB_PAT（~/.zshrc）。git 已配置 credential.helper store，自动填充认证，无需在命令中携带 token。

**有效期**：90 天（用户 2026-08-02 设置，预计 2026-10-31 到期）。到期后需用户重新提供 PAT。若 GitHub 返回 Invalid username or token → 检查 ~/.git-credentials 是否有效。

**安全**：PAT 只存沙箱本地（~/.git-credentials 权限 600），绝不写入仓库。

### 历史沙箱构建环境（2026-08-02，CodeBuddy 环境）

**背景**：沙箱无法直连任何 Google 官方源（services.gradle.org / dl.google.com / repo1 / maven.google.com，TLS 全部被中间设备掐断）。构建必须全链路走国内镜像。已配置完成，新会话直接用：

```bash
# JDK 17（项目 compileOptions 17，环境 JDK 20 会导致 JVM target 不一致）
export JAVA_HOME=/opt/jdk17          # Temurin 17.0.20（清华 TUNA Adoptium 镜像安装）
export PATH=/opt/jdk17/bin:$PATH

# 构建（镜像已由 ~/.gradle/init.gradle 全局配置）
./gradlew :app:assembleDebug
./gradlew :core:designsystem:testDebugUnitTest
```

**关键配置（沙箱本地，不入仓库）**：
- `~/.gradle/init.gradle`：`pluginManagement` + `dependencyResolutionManagement` 全部 `clear()` 后替换为腾讯 maven-public（聚合 google+central）→ Aliyun。**不要保留官方仓库**，回退直连会挂起（fake-ip 连接 Recv-Q=0 无数据）
- `~/.gradle/gradle.properties`：`org.gradle.internal.http.connectionTimeout/socketTimeout=30000` 防挂起
- Android SDK：`/opt/android-sdk`（local.properties 指向它）。注意 zip 内部带 `android-14/`/`android-35/` 前缀目录，解压时要上移一层；组件必须有 `source.properties` 否则 AGP 判定无效
- Gradle 发行版：腾讯 `mirrors.cloud.tencent.com/gradle/gradle-8.14.4-bin.zip`
- Robolectric android-all jar：预下载到 `~/.m2/repository/org/robolectric/android-all-instrumented/<ver>/`（腾讯 maven-public 有），否则测试时 MavenArtifactFetcher 联网下载被 TLS 拦截

**已验证**（2026-08-02）：`:core:designsystem:assembleDebug` + `testDebugUnitTest` **42 tests / 0 failures** 全绿。


## 6. AI 协作规则

- **每次会话结束前**更新 [docs/SESSION_LOG.md](docs/SESSION_LOG.md) 并 commit
- 遇到版本兼容问题 → 先查 [docs/02-VERSION-MATRIX.md](docs/02-VERSION-MATRIX.md)
- 遇到编译失败 → 先查 [docs/03-FAILED-ATTEMPTS.md](docs/03-FAILED-ATTEMPTS.md)，不重复已失败方案
- 发现新坑 → 补充到 `docs/03-FAILED-ATTEMPTS.md`
- 发现版本兼容信息 → 补充到 `docs/02-VERSION-MATRIX.md`
- commit message 说清"为什么改"，不只是"改了什么"
- 用户偏好：中文交流、严谨验证、反复检查、有趣的教学风格、M3 谷歌味道 UI

### 6.1 当前 Luna/Codex 工单协议

- 普通任务：一次只做一个工单；先只读报告，收到用户明确授权后才实施；独立分支和 Draft PR；合并后再开始下一工单。
- Cloud MVP：只有启动语明确引用 `docs/plans/CLOUD-MVP-EXECUTION.md` 时，才按 C00–C24 连续执行；每个 C 检查点仍必须测试先行、自审、原子 commit 并更新 progress。
- Cloud MVP 的当前起点是 PR-01B，终点是 PR-08C 后的 C24 闭环审计；不得提前进入 PR-09。
- Cloud MVP 最终只创建 Draft PR，不得自动 Ready、合并、打 tag 或发布。
- 触发执行合同的停止条件时，停在最后一个 PASS 检查点，不得靠改 seed、baseline、旧 ID、用户数据或降低测试门槛继续。
- 不允许多个写代理并行修改同一工作树；不要使用 tools.zip 作为 PR-01B → PR-08C 的输入。

## 7. 历史状态快照（2026-08-01）

> **ARCHIVED**：本节及后续阶段/优先级记录是 2026-08-01 的恢复快照，不是当前待办。当前版本、阻塞和下一工单只看 [docs/00-STATUS.md](docs/00-STATUS.md) 与已批准的实施计划。

**✅ v0.9.18 悬浮底部导航栏 + 知识卡片手动加入错题本（已发布）** — 双功能发布。**悬浮导航栏**：响应用户需求"ksunext 底部悬浮"，Surface 包裹 NavigationBar（圆角 16dp + tonalElevation 3dp + 水平间距 16dp + 底部 8dp 留空），BottomGradientScrim 缩短至 80dp（原 120dp），减少遮挡面积 20%。**手动加入错题本**：响应用户需求"在知识卡片里面加一个按钮，可以把卡片手动加入错题本"，5 层实现（SOURCE_CARD_MANUAL 常量 / `addToWrongAnswerBook()` 防重入+防重复+NonCancellable 原子写入 / `AddToWrongAnswerButton` 三态 UI / `isCurrentCardInWrongBook` sibling 感知 / 10+ 新测试）。**CI 修复**：3 轮编译错误修复 + 14 个测试失败修复（移除 Dispatchers.IO，与 rateCard() 模式一致）。CI 全绿后正确打 tag v0.9.18 → commit `7ec209da`。**Release 2026-08-01T18:46:10Z 成功发布**，APK 19,475,344 bytes SHA-256 `3d968ad5...0561f5`。Exception E1（debug 签名 fallback）。设计文档：[docs/plans/floating-navigation-bar.md](docs/plans/floating-navigation-bar.md) + [docs/plans/cards-add-to-wrong-answer-book.md](docs/plans/cards-add-to-wrong-answer-book.md)。**待 emulator 实测**：验证悬浮导航栏 + 手动加入错题本 + 启动图标 v4 三项功能。

**✅ v4 启动图标设计重构（书+文负空间，已实施，待发布）** — 响应用户需求"把这个app的图标重新设计一下"。用户选择方案 B（书+文负空间），经精修后实施。从 v3 "印章文"（5 个独立矩形 path）改为 v4 "展开的书 + 文负空间"（单 path + evenOdd fillType 镂空）。设计语言：Google Play Books（书形）+ Google Docs（字母负空间）混合。精修要点：去 serif 平底收笔 + "文"字垂直居中于书页。Safe Zone 检查全部通过。本地验证：`:app:assembleDebug` BUILD SUCCESSFUL + 全模块 `testDebugUnitTest` UP-TO-DATE。**待 emulator 实测**：验证新图标启动屏/桌面/通知栏显示效果。设计文档：[docs/design/icon-redesign.md](docs/design/icon-redesign.md)，精修预览：`.tmp-preview/icon-preview.html`。

**✅ v2.16.0 知识点补充（论述题 knowledgeGaps 完整化，已 commit c951b2e）** — 响应用户需求"可以的，你帮我补充一下知识点，然后整体严谨检查一下，一定要仔细严谨，不要出问题，包括我的考研要学习的内容"。补充论述题 knowledgeGaps 字段明确建议的 25 个核心知识点（kp_00911-kp_00935），对齐袁行霈/钱理群/朱维之/童庆炳四教材，并清理 eq_0100 OCR 错误条目。学科分布：古代4（王勃/江淹/唐传奇/清初才子佳人小说）/现当代8（戴望舒/穆时英/萧红/路遥/钱钟书围城/陈忠实/宋晓贤/陆蠡）/外国6（乔伊斯/伍尔夫/劳伦斯/王尔德/简·奥斯汀/陀思妥耶夫斯基罪与罚）/文论7（列宁论托尔斯泰/刘勰文心雕龙/姚斯接受美学/布洛心理距离/康德美学/罗兰·巴特/莱辛拉奥孔）。新增 `tools/essay_fill/fill_missing_knowledge_points.py`（542 行生成脚本）。seed_data.json 2.15.0→2.16.0，知识点库 910→935 完整化。严谨检查：85 个 knowledgeGaps 关键词全部匹配到知识点（0 真正缺失），OCR 错误条目已清理，新增知识点结构规范（study_text 平均 622 字符），关联派生模拟 16/134 论述题关联新增知识点。本地验证：`:app:assembleDebug` SUCCESSFUL + `:core:data:testDebugUnitTest` SeedDataLoaderTest 21 tests 0 failures（--rerun-tasks 强制重跑）。**agent-pr-review ✅ READY TO MERGE**（0 blocker, 0 must-fix, 1 follow-up — 9 个新增知识点通过 knowledgeGaps 标注补充，未被论述题直接关联）。Receipt：[docs/release-receipts/v2.16.0-knowledge-supplement-pr-review.md](docs/release-receipts/v2.16.0-knowledge-supplement-pr-review.md)。**待 emulator 实测**：验证 seed 2.16.0 触发重导后 935 知识点正确导入 + 25 个新增知识点可浏览/搜索。

**✅ v0.9.9 论述题板块完整版 + AI 审题助手 + 134 题全覆盖填充（已发布）** — 响应需求"因为我要考研嘛，你帮我把所有论述题都整理，然后放在软件里面，反复研究调查"。**v0.9.8 论述题板块（Phase 0-2）**：知识点详情页"相关论述题"区块 + 论述题详情页 10 区块结构（题目/审题/论证/框架/依据/交叉验证/参考链接/知识盲点/关联知识点）+ EssayDetailViewModel + EssayDetailModels（kotlinx.serialization 优雅降级）+ ROUTE_ESSAY_DETAIL 双向导航 + 独立论述题列表页（EssayListScreen + EssayListViewModel，三维筛选：年份/科目/仅显示有审题思路）+ 知识点列表顶部 EssayEntryCard 入口 + ROUTE_ESSAY_LIST 子路由。**v0.9.9 Phase 3 AI 审题助手**：苏格拉底三阶段引导（论证分析→改进建议→参考范文，流式输出）+ 自评三档（AGAIN/GOOD/EASY，AGAIN 时回写错题本 + FSRS 调度）+ MAX_USER_ANSWER_LENGTH=5000 防超限 + 防重入 + EssayAiGuideSection UI 区块（论述题详情页第 11 区）+ EssaySelfRating 枚举。**论述题全覆盖填充（134/134 题）**：2007-2022 年 610/614/615/616 卷全部 134 道论述题 angle+notes 完整填充，对齐 3 道示例题标准（angle: questionType/coreKeywords/limitKeywords/task/breakthroughAngles/angleRationale/argumentPath；notes: evidences[作品原文+学者观点+教材定论]/crossValidation[教材对比+学者对比]/referenceLinks/knowledgeGaps）。学术严谨：作品原文如实引用+标注出处、学者观点标注原作者与文献（王富仁/汪晖/钱理群/陈寅恪/朱光潜/王季思/袁行霈/洪子诚/陈思和等）、教材定论以袁行霈《中国文学史》/钱理群《三十年》/朱维之《外国文学史》/童庆炳《文学理论教程》为基准、未覆盖知识点如实记入 knowledgeGaps 不臆造。tools/essay_fill/ 新增 11 个 Python 脚本按年份批量填充。seed 2.14.0→2.15.0。本地验证：`:app:assembleDebug` + `:app:assembleRelease` + 全模块 `testDebugUnitTest` 全绿（469 tests, 0 failures）。**agent-pr-review ✅ READY TO MERGE + PRR ✅ READY TO RELEASE + RBR ✅ PASS**（per staff-engineer-mode Agent Event Policy）。Release v0.9.9 已发布（debug 签名 fallback — Exception E1）。Debug APK 29,074,437 bytes SHA-256 `2a4b38fb1889010dcbb8623c5adbb29392e57f18d61e69e68d8c5fe66fd7c6ea` / Release APK 19,412,892 bytes SHA-256 `0327eb562fc2be9696adee057e924b3cbe4e3efa54834997bdc80fcd2750c563`。**已知限制**：Phase 3 AI 审题助手需用户配置 LLM API key；~~部分题目的 knowledgeGaps 知识点（王勃/江淹/高适/岑参/陶渊明/孔尚任等）尚未在 910 知识点库中，后续可补充~~ **已于 v2.16.0 补充完成**（25 个知识点 kp_00911-kp_00935，知识点库 910→935）。

**✅ v0.9.7 知识卡片功能完善 + 界面审查修复（已发布）** — 响应用户反馈"知识卡片部分功能还是不够完善，整体界面好好审查一下"。深度审查知识卡片模块，修复 3 项数据一致性 Bug + 5 项体验优化 + 1 项 UI 工程化 + 2 新测试。**数据一致性（B1/B2/B3）**：B1 sibling 去重 FSRS 调度漏洞（templateType 解析提前，无效 cardType 不污染 ratedPointIds）/ B2 Leech 误报修复（RELEARNING+AGAIN 时 failCount 不变，oldFailCount 反推根据 updated.state 区分）/ B3 无 pointId 卡评分加 Timber 警告日志。**体验优化（M2/M4/M5/M9/M11）**：M2 sibling 卡打散（interleaveSiblingCards round-robin，避免连续 5-6 张同知识点）/ M4 翻转时重置滚动位置（LaunchedEffect scrollTo(0)）/ M5 完成态新增"撤销最后一张"按钮（评错可回退）/ M9 无效 cardTypeStr 加 Timber 警告（原静默失败）/ M11 collectLatest 进入时清空预览（避免快速切卡时旧预览闪烁）。**UI 工程化（M10）**：CardsScreen 添加 3 个 @Preview（Normal/Empty/Finished）+ 辅助函数 previewCardItem/previewUiState。**代码卫生**：templateType!! → templateType（smart cast，消除编译器警告）。本地验证：`:app:assembleDebug` + `:app:assembleRelease` + 全模块 `testDebugUnitTest` 全绿（405 tests, 0 failures）。**PRR ✅ READY TO RELEASE + RBR ✅ PASS**（per staff-engineer-mode Agent Event Policy）。Debug APK SHA-256 `c318237a5b866b64a45acaf615f638368c11d805e2dc5adc3cff9da9ebed156e` / 27,522,631 bytes / Release APK SHA-256 `7dde7e3fe81037232d5d4b0bb41199c631890dcfef50e4222ce6f7f931a7d587` / 19,186,172 bytes。

**✅ v0.9.5 关于与教程子路由（已发布）** — 设置页"关于"分组新增"关于与教程"入口，注册 ROUTE_ABOUT 子路由（Push/Pop slide + launchSingleTop 防双击压栈），加载 AboutTutorialScreen（7 章深度教程，430 行新文件）。教程覆盖：1) 软件定位与核心理念 2) 功能模块导览（5 个顶级 Tab）3) FSRS-6 间隔重复算法（4 大公式 + 4 状态调度 + 4 档评分 + ClockGuard）4) 三档记忆机制（EXACT 0.95 / FRAMEWORK 0.90 / UNDERSTAND 0.85）5) AI 助手与 RAG 架构（RAG + 苏格拉底三阶段 + 多服务商 + Prompt Injection 防护）6) 使用指南与学习路径（6 步入门 + 三阶段节奏）7) 技术信息与致谢。本地验证：`:app:assembleDebug` + `:app:assembleRelease` + 全模块 `testDebugUnitTest` 全绿（236 tests, 0 failures）。agent-pr-review ✅ Ready to merge（0 blocker, 0 must-fix）。**PRR ✅ READY TO RELEASE + RBR ✅ PASS**（per staff-engineer-mode Agent Event Policy）。Release v0.9.5 已发布（debug 签名 fallback — Exception E1）。Debug APK SHA-256 `0045a82d1ae318d2d504b73e8bb71bc13ee117d4354bdba60a914e968093eb58` / 27,522,631 bytes。

**✅ v0.9.4 错题本接入 FSRS 间隔重复调度（已发布）** — 为 wrong_answers 表添加 10 个 sched_* FSRS 调度字段，复用 FSRS-6 算法 + TIER_FRAMEWORK 档位（R_target=0.90），实现错题的间隔重复复习。5 层实现：数据层（Migration 7→8 + 10 字段 + 索引）+ 映射层（WrongAnswerSchedulingMapper）+ 仓库层（SchedulingRepository.rateWrongAnswer）+ ViewModel 层（DUE 过滤 + 评分委托 + ClockGuard 注入）+ UI 层（四档评分按钮 + 调度信息展示）。Follow-up #1 ClockGuard 注入 + #2 interval coerceAtLeast(0) 已修复。+10 单测。agent-pr-review ✅ Approved（0 blocker, 0 must-fix）。Release v0.9.4 已发布（debug 签名 fallback — Exception E1）。

- 最新 commit：**v0.9.18** `7ec209da` fix: 移除 addToWrongAnswerBook 中 Dispatchers.IO（2026-08-01）
- 最新 Release：**v0.9.18**（2026-08-01 发布，debug 签名 fallback — Exception E1）— https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.18
- 本地验证（2026-08-01 v0.9.18）：CI 全绿（60 tests, 0 failures）
- versionCode / versionName：**43 / "0.9.18"**
- v0.9.18 APK 校验：wenyan-v0.9.18.apk 19,475,344 bytes SHA-256 `3d968ad5e1e2eee8c96cab214541f086ed1a8b699b734a5f72945c725d0561f5`（debug 签名 Exception E1）
- v0.9.18 Rollback target：[v0.9.17](https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.17)（uninstall v0.9.18 + install v0.9.17，versionCode 42 < 43 需卸载后安装）
- v0.9.18 receipt：待生成（optionally：docs/release-receipts/v0.9.18-release-receipt.md）
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

## 8. 历史项目阶段总览

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
| v0.9.8 论述题板块 | ✅ 完成（2026-07-31，待发布） | 响应用户需求增加论述题板块串联知识点。**Phase 0 数据层**：ExamQuestionDao.observeAllEssays + KnowledgeRepository.observeRelatedEssays/observeEssayById/getKnowledgePointsByIds + SeedDataLoader.computeExamQuestionRelatedPoints + seed 2.13.1→2.14.0 + 3 道示例题。**Phase 1 UI 层**：知识点详情页"相关论述题"区块 + 论述题详情页 10 区块结构 + EssayDetailViewModel（JSON 解析 + 关联知识点聚合）+ EssayDetailModels（kotlinx.serialization 优雅降级）+ ROUTE_ESSAY_DETAIL 双向导航。**Phase 2 列表页 + 入口**：EssayListScreen + EssayListViewModel（三维筛选：年份/科目/审题思路 + retryTrigger 重试）+ 知识点列表 EssayEntryCard 入口 + ROUTE_ESSAY_LIST 子路由 + FakeChapterRepository。本地验证全绿（469 tests, 0 failures） |

## 9. 历史下一步优先级

1. **P0**：跑 emulator 实测 v2.16.0（已 commit c951b2e，待发布）— 验证知识点补充：①seed 2.16.0 触发重导后 935 知识点正确导入 ②25 个新增知识点（kp_00911-kp_00935）可浏览/搜索 ③新增知识点按学科正确分类（古代4/现当代8/外国6/文论7）④eq_0100 knowledgeGaps 空数组正常渲染（无 OCR 错误条目）⑤论述题详情页"关联知识点"区块正确派生（16/134 题关联新增知识点）
2. **P0**：跑 emulator 实测 v0.9.9（已发布，最新版）— 验证论述题板块完整功能：①知识点列表顶部 EssayEntryCard 入口可点击 → 论述题列表页 ②列表页三维筛选（年份/科目/仅显示有审题思路）正常 ③论述题详情页 11 区块结构渲染（题目/审题/论证/框架/依据/交叉验证/参考链接/知识盲点/关联知识点/AI 审题助手）④关联知识点点击跳转 ⑤AI 审题助手苏格拉底三阶段引导（需配置 LLM API key）+ 自评三档（AGAIN 回写错题本）⑥seed 2.15.0 触发重导后 134 道论述题 angle+notes 正确填充 ⑦知识点详情页"相关论述题"区块渲染
3. **P0**：跑 emulator 实测 v0.9.7 — 验证知识卡片功能：sibling 卡打散 + 翻转重置滚动 + 完成态"撤销最后一张" + Leech 警告 + 3 个 @Preview
4. **P0**：跑 emulator 实测 v0.9.6 — 验证设置 → 关于 → 关于与教程入口 + 5 节结构渲染（HeroCard / QuickStart / Modules / Principles 可折叠 / About）+ ExpandableInfoItem 展开/收起 + 竖屏友好
5. **P0**：跑 emulator 实测 v0.9.4 — 验证错题本 DUE 过滤模式 + 四档评分按钮 + 调度信息展示 + Migration 7→8 升级 + ClockGuard 时间源对齐
6. **P0**：CI 已修复（2026-07-31）— keystore fail-fast 从配置阶段移到执行阶段，`testDebugUnitTest` / `assembleDebug` 可正常在 CI 运行。push 到 main 后自动触发 CI 验证。
7. **P0**：GitHub Secrets 配置后，重新用正式 keystore 构建 release APK 并替换 v0.9.4 + v0.9.5 + v0.9.6 + v0.9.7 + v0.9.9 asset（消除 Exception E1）。需设置 `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` 四个 Secrets。
8. **P1**：启用 R8（P1-PG 规则已就绪 + B5.1 GraphSkeleton 路径已修正，需 emulator 实测验证无崩溃后切换 isMinifyEnabled=true）
9. **P2 优化项（非阻塞）**：`app/build.gradle.kts` release keystore fail-fast 已修复（2026-07-31）：CI 检查从配置阶段移到执行阶段（`assembleRelease.doFirst`），`testDebugUnitTest` / `assembleDebug` 在 CI 中不再被阻断。`android.yml` 已移除 `assembleRelease` 步骤（release 构建由 `release.yml` 独立处理）。
10. **P1 Phase 2 剩余维度审计**：
   - 2.E 剩余：strings.xml 完整性（NF-U2）、dimens.xml（NF-C10）
   - 2.L：错误处理一致性 + 日志规范（sealed AppError + Timber + Snackbar 统一） — v0.8.18 已完成 Timber 引入，剩 sealed AppError + Snackbar 统一
   - 2.M：Compose 副作用 + Accessibility + M3 Expressive
   - 2.N 剩余：NF-DS7-13 DataStore Key 治理
11. **P2**：OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（替换 stage2-sample）
12. **P3**：release.yml "Verify keystore" 步骤隐藏 bug（Line 63-70，KEYSTORE_BASE64 未配置时失败）— **已修复**：P1-S-1 修正已在 "Decode keystore" 步骤添加 fail-fast（exit 1 if KEYSTORE_BASE64 empty），见 [.github/workflows/release.yml](.github/workflows/release.yml) L60-65
13. **P4**：架构重构 — getVerifiedWithSubject 职责应在 KnowledgeRepository — **已完成**：方法已迁移至 [KnowledgeRepository.kt:116](core/data/src/main/java/com/wenyan/app/core/data/repository/KnowledgeRepository.kt#L116)，ReviewRepository 仅保留 doc 引用
