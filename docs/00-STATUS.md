# 当前状态快照

> **AI 新会话第一份要读的文件。10 秒了解项目当前状态。**
> 最后更新：2026-08-01（悬浮底部导航栏 v0.9.18）

## ✅ 当前状态

**v0.9.18 悬浮底部导航栏（改造中，本会话）** — 响应用户需求"我看 ksunext 等等这种用 M3 Expressive 的软件底部是悬浮的，你研究一下可不可以做到这样"。基于深度调研，采用 Surface 包裹 NavigationBar 方案实现悬浮效果：WenyanNavigationBar.kt 外层 Surface 容器（圆角 16dp + tonalElevation 3dp + 水平间距 16dp + 底部 8dp 留空），内层 NavigationBar 透明。WenyanAdaptiveNavigation.kt BottomGradientScrim 缩短至 80dp（原 120dp），渐变从 3 色改为 4 色（Transparent → 0.60f → 0.85f → solid），减少遮挡面积 20%（200dp→160dp）。设计文档：[docs/plans/floating-navigation-bar.md](plans/floating-navigation-bar.md)。**待 emulator 实测**：验证 5 个 Tab 悬浮效果、渐变遮罩过渡、子路由返回、沉浸式体验。

**v0.9.17 题号前缀剥离（已发布）** — 响应用户需求"去掉题号前缀"。创建 ExamContentCleaner 集中清洗工具，剥离所有题目内容中的阿拉伯数字前缀（"1. " "2. "）和中文数字前缀（"一、" "二、" "三、论述题" 等），包括试卷标题。6 个 UI 展示点统一清洗：论述题列表预览（EssayListViewModel）、论述题详情正文（EssayDetailScreen）、知识点关联预览（KnowledgePointDetailScreen）、真题练习题目（QuizScreen）、错题本题目标题（WrongAnswerScreen）、AI 审题助手输入（EssayDetailViewModel）。不修改 seed_data.json，仅运行时清洗。versionCode 41→42，versionName "0.9.16"→"0.9.17"。**Exception E1**：CI 账单问题，release APK 使用 debug 签名 fallback（与 v0.9.4-v0.9.16 一致）。**待 emulator 实测**：验证 6 个展示点题号前缀全部剥离、AI 审题助手接收清洗后内容。

**v0.9.16 真题→论述题迁移（底部导航 Tab 替换，已发布）** — 响应用户需求"真题这个部分删除，因为已经有论述题部分了，然后论述题部分放到原来真题的位置"。底部导航第 2 个 Tab 从"真题"(Quiz) 替换为"论述题"(Essay)。死代码审查：无残留。versionCode 40→41，versionName "0.9.15"→"0.9.16"。**Exception E1**：CI 账单问题，release APK 使用 debug 签名 fallback。

**v0.9.14 修复底栏遮盖 + 软件内更新（已发布 2026-07-31）** — 响应用户反馈"底栏遮盖了可以点击的地方，更新为什么不能软件内更新，还要去浏览器，而且下的是debug版本，而且只能卸载重装，为什么有这么多问题"。修复 v0.9.13 沉浸式导航栏导致的底栏遮盖可点击区域问题：COMPACT 布局改用 Box + 显式 padding（80dp 导航栏高度 + 系统手势区），不再依赖 Scaffold contentWindowInsets 消费策略。实现软件内 APK 下载+安装：UpdateViewModel 新增 OkHttp 流式下载 + FileProvider 安装 + 2 种新 UI 状态（Downloading/DownloadComplete），UpdateCheckScreen 新增进度条和安装按钮。AndroidManifest 新增 REQUEST_INSTALL_PACKAGES + FileProvider。新增 file_paths.xml。OkHttp 依赖。**重要更正**：CI Release workflow 运行正常，keystore 已配置，本次 Release 为正式签名 APK（19.5 MB），可直接覆盖安装旧版，无需卸载。本地验证：`:app:assembleDebug` SUCCESSFUL + `testDebugUnitTest` SUCCESSFUL（317 tasks, 0 failures）。

**v2.16.0 知识点补充（论述题 knowledgeGaps 完整化，已 commit c951b2e）** — 响应用户需求"可以的，你帮我补充一下知识点，然后整体严谨检查一下，一定要仔细严谨，不要出问题，包括我的考研要学习的内容"。补充论述题 knowledgeGaps 字段明确建议的 25 个核心知识点（kp_00911-kp_00935），对齐袁行霈/钱理群/朱维之/童庆炳四教材，并清理 eq_0100 OCR 错误条目。学科分布：古代4（王勃/江淹/唐传奇/清初才子佳人小说）/现当代8（戴望舒/穆时英/萧红/路遥/钱钟书围城/陈忠实/宋晓贤/陆蠡）/外国6（乔伊斯/伍尔夫/劳伦斯/王尔德/简·奥斯汀/陀思妥耶夫斯基罪与罚）/文论7（列宁论托尔斯泰/刘勰文心雕龙/姚斯接受美学/布洛心理距离/康德美学/罗兰·巴特/莱辛拉奥孔）。新增 `tools/essay_fill/fill_missing_knowledge_points.py`（542 行生成脚本）。seed_data.json 2.15.0→2.16.0，知识点库 910→935 完整化。严谨检查：85 个 knowledgeGaps 关键词全部匹配到知识点（0 真正缺失），OCR 错误条目已清理，新增知识点结构规范（study_text 平均 622 字符），关联派生模拟 16/134 论述题关联新增知识点。本地验证：`:app:assembleDebug` SUCCESSFUL + `:core:data:testDebugUnitTest` SeedDataLoaderTest 21 tests 0 failures（--rerun-tasks 强制重跑）。**agent-pr-review ✅ READY TO MERGE**（0 blocker, 0 must-fix, 1 follow-up）。Receipt：[docs/release-receipts/v2.16.0-knowledge-supplement-pr-review.md](release-receipts/v2.16.0-knowledge-supplement-pr-review.md)。

**v0.9.9 论述题板块完整版 + AI 审题助手 + 134 题全覆盖填充（已发布）** — v0.9.8 论述题板块（Phase 0-2）+ v0.9.9 Phase 3 AI 审题助手（苏格拉底三阶段引导 + 自评三档 + 错题回写 FSRS）+ 论述题全覆盖填充（134/134 题 angle+notes 完整填充）。Release v0.9.9 已发布（debug 签名 fallback — Exception E1）。Debug APK 29,074,437 bytes SHA-256 `2a4b38fb...7c6ea` / Release APK 19,412,892 bytes SHA-256 `0327eb56...50c563`。

**v0.9.6 关于与教程精简重构 + 代码卫生审计（已发布）** — 响应用户反馈"关于与教程界面做的太复杂了，排版也很难看，内容也太多了，竖屏的时候更是非常糟糕"，重构 AboutTutorialScreen.kt：7 章 430 行 → 5 节 ~384 行（HeroCard / QuickStart / Modules / Principles 可折叠 / About）。默认视图简洁，深度原理用 ExpandableInfoItem + AnimatedVisibility 按需展开。竖屏友好：MaxContentWidth.compact 限宽。同时修复 4 项代码卫生问题：CardsScreen.kt 弃用图标、FriendlyErrorMessage.kt 冗余 !!、CardsViewModel.kt 2 处 !!、导航 Preview 移除已删除 graph 模块引用。本地验证全绿（403 tests）。**PRR ✅ READY TO RELEASE + RBR ✅ PASS**（per staff-engineer-mode Agent Event Policy）。Release v0.9.6 已发布（debug 签名 Exception E1）。Debug APK SHA-256 `36237a66...2ff100` / 27,522,631 bytes / Release APK SHA-256 `8661d97b...8d356c` / 19,169,788 bytes。

**v0.9.5 关于与教程子路由（已发布）** — 设置页"关于"分组新增"关于与教程"入口，注册 ROUTE_ABOUT 子路由（Push/Pop slide + launchSingleTop 防双击压栈），加载 AboutTutorialScreen（7 章深度教程 430 行：定位/模块/FSRS-6/三档记忆/RAG/使用指南/致谢）。Icons.AutoMirrored.Filled.MenuBook 弃用修复。本地验证全绿（236 tests）。agent-pr-review ✅ Ready to merge。**PRR ✅ READY TO RELEASE + RBR ✅ PASS**（per staff-engineer-mode Agent Event Policy）。Release v0.9.5 已发布（debug 签名 Exception E1）。APK SHA-256 `0045a82d...93eb58` / 27,522,631 bytes。

**v0.9.4 错题本接入 FSRS 间隔重复调度（已发布）** — 为 wrong_answers 表添加 10 个 sched_* FSRS 调度字段，复用 FSRS-6 算法 + TIER_FRAMEWORK 档位（R_target=0.90），实现错题的间隔重复复习。5 层实现：数据层（Migration 7→8 + 10 字段 + 索引）+ 映射层（WrongAnswerSchedulingMapper）+ 仓库层（SchedulingRepository.rateWrongAnswer）+ ViewModel 层（DUE 过滤 + 评分委托 + ClockGuard 注入）+ UI 层（四档评分按钮 + 调度信息展示）。Follow-up #1 ClockGuard 注入 + #2 interval coerceAtLeast(0) 已修复。+10 单测。agent-pr-review ✅ Approved（0 blocker, 0 must-fix）。Release v0.9.4 已发布（debug 签名 fallback — Exception E1）。

| 项 | 值 |
|----|-----|
| 最新 commit | **本会话** 悬浮底部导航栏 v0.9.18（2026-08-01） |
| 最新 Release | **v0.9.17**（2026-08-01 发布，debug 签名 Exception E1）— https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.17 |
| 编译验证 | **沙箱无 Android SDK，跳过本地编译**（纯 Compose 视觉参数调整，无 API/逻辑变更） |
| versionCode / versionName | **42 / "0.9.17"**（v0.9.18 尚未升级版本号） |
| 知识点 | **935 个**（v2.16.0 补充 25 个核心知识点 kp_00911-kp_00935） |
| 真题 | **485 道**（v0.7.6 已删除 sample_essay 冗余字段） |
| 论述题 | **134 道 ESSAY 题**（v0.9.9 全覆盖填充 angle+notes，v0.9.10 全面审计 0 问题） |
| seed 版本 | **2.16.0**（v2.16.0 补充 25 个知识点 + 清理 OCR 错误条目，触发重新导入） |
| 数据库版本 | **8**（v0.9.4 Migration_7_8 wrong_answers 添加 10 个 sched_* FSRS 调度字段 + 索引） |
| 章节树 | **二级层级**（subject → default_chapter → chapter_<tag>，基于文学时段自动生成） |
| 关联模块 | **3 关系类型**（RELATED/CONTRAST/EXTENSION）+ 视觉编码 + **v0.9.1: relatedIds 基于 tags 派生**（同 subject + 共享 tag，按共享数降序取前 5） |
| 错题本 FSRS | **v0.9.4 已发布**：DUE 过滤模式 + 四档评分（不会/困难/良好/简单）+ 调度信息展示（下次复习/复习次数/遗忘次数）+ TIER_FRAMEWORK 档位 + ClockGuard 时间源对齐 + interval 下界保护 |
| 论述题板块 | **v0.9.8 + v0.9.9 已发布**：知识点详情页"相关论述题"区块 + 论述题详情页 11 区块结构（含 AI 审题助手）+ JSON 优雅降级 + 双向导航 + 独立列表页（三维筛选）+ EssayEntryCard 入口 + 134/134 题 angle+notes 完整填充 |
| 关于与教程 | **v0.9.6 精简重构**：5 节简洁版（HeroCard / QuickStart / Modules / Principles 可折叠 / About），默认视图简洁，深度原理按需展开 |
| 底部导航 | **5 Tab**（知识点 / 论述题 / 卡片 / 错题本 / 设置），**v0.9.18 悬浮式**：Surface 圆角 16dp + tonalElevation 3dp + 水平留边 16dp + 底部留空 8dp |
| 图谱 UI | **已移除**（v0.9.0 feature:graph 模块删除） |
| 图谱数据层 | **已移除**（v0.9.3 优化 4 全部移除，详见 [docs/release-receipts/v0.9.3-opt4-graph-removal-receipt.md](release-receipts/v0.9.3-opt4-graph-removal-receipt.md)） |
| 启动图标 | **v4 "书+文负空间"**（展开的书 + "文"字镂空 negative space，单 path + evenOdd fillType，书形占 safe zone 70%+） |
| 日志门面 | **Logging.kt**（Timber 封装，Debug=Logcat / Release=WARN+ERROR） |
| 工具链锁定 | **mise.toml**（JDK 17.0.2 + Gradle 8.14.4） |
| 阻塞 | **无** — CI Release workflow 正常运行，keystore 已配置 |
| 详情 | [SESSION_LOG.md](SESSION_LOG.md) 最后一节（2026-07-31 启动图标 v4 设计重构 — 书+文负空间） |

## 🚨 新会话首要任务

**v0.9.18 悬浮底部导航栏（改造中，本会话）** — 响应需求"ksunext 底部悬浮"。WenyanNavigationBar.kt Surface 包裹（圆角 16dp + tonalElevation 3dp + 水平间距 16dp + 底部 8dp）。WenyanAdaptiveNavigation.kt BottomGradientScrim 缩短至 80dp（原 120dp），渐变 4 色（Transparent→0.60f→0.85f→solid）。减少遮挡面积 20%（200dp→160dp）。设计文档：[docs/plans/floating-navigation-bar.md](plans/floating-navigation-bar.md)。**待 emulator 实测**：验证悬浮效果、渐变过渡、沉浸式体验。

**启动图标 v4 设计重构（书+文负空间，已实施，待 push）** — 响应用户需求"把这个app的图标重新设计一下"。用户选择方案 B（书+文负空间），经精修后实施。从 v3 "印章文"（5 个独立矩形 path）改为 v4 "展开的书 + 文负空间"（单 path + evenOdd fillType 镂空）。设计语言：Google Play Books（书形）+ Google Docs（字母负空间）混合。精修要点：去 serif 平底收笔 + "文"字垂直居中于书页。Safe Zone 检查全部通过。本地验证：`:app:assembleDebug` BUILD SUCCESSFUL（279 tasks）+ `testDebugUnitTest`（317 tasks, 0 failures）。**待 emulator 实测**：验证新图标启动屏/桌面/通知栏显示效果。

**v0.9.14 已发布**（2026-07-31，debug 签名 fallback — Exception E1）。本地构建全绿：`:app:assembleDebug` + `testDebugUnitTest`（317 tasks 0 failures）。修复底栏遮盖 + 实现软件内 APK 下载+安装。

下一步优先级（按顺序）：

1. **P0**：emulator 实测 v0.9.18 悬浮导航栏 — 验证：①导航栏悬浮效果（圆角/投影/间距）②渐变遮罩过渡（80dp 平滑过渡）③5 个 Tab 切换正常 ④子路由返回后导航栏恢复 ⑤AMOLED 模式下投影可见
2. **P0**：emulator 实测启动图标 v4 — 验证：①启动屏图标显示正确（书+文负空间）②桌面图标（方形+圆形遮罩）③最近任务栏小尺寸图标清晰 ④Android 13+ themed icon 模式下"文"字负空间保留
3. **P0**：emulator 实测 v0.9.14 — 验证：①底部导航栏不遮挡可点击区域 ②软件内更新下载进度条显示正常 ③下载完成后自动弹出安装界面 ④各 Tab 页面正常显示
3. **P1**：启用 R8（P1-PG 规则已就绪，需 emulator 实测验证无崩溃后切换 isMinifyEnabled=true）
4. **P1 Phase 2 剩余维度审计**：
   - 2.E 剩余：strings.xml 完整性（NF-U2）、dimens.xml（NF-C10）
   - 2.L：错误处理一致性 + 日志规范（sealed AppError + Timber + Snackbar 统一） — v0.8.18 已完成 Timber 引入，剩 sealed AppError + Snackbar 统一
   - 2.M：Compose 副作用 + Accessibility + M3 Expressive
   - 2.N 剩余：NF-DS7-13 DataStore Key 治理
7. **P2**：CONTRAST/EXTENSION 关联需语义分析，可由 AI 管线（LLM 从 full_content 派生）或手动标注补充
8. **P2**：OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（替换 stage2-sample）

### v0.8.18 工程化审查（per staff-engineer-mode Iron Law）

按 Iron Law "Before tags, versions, hosted releases, packages, artifacts, or promotions, read `release-build-reproducibility` and `production-readiness-review`, show the structured review artifacts to the user, record the receipt in its own shell command, then run the release command in a separate shell command" 完成三项审查：

| 审查 | 结果 | 关键点 |
|------|------|--------|
| PRR（production-readiness-review） | ✅ READY TO RELEASE | External Artifact / Blocker B1 → Exception E1 |
| RBR（release-build-reproducibility） | ✅ PASS | Pinned inputs（JDK 17.0.2 / Gradle 8.14.4 / AGP 8.6.0 / Kotlin 2.3.10 / KSP 2.3.2 / Compose BOM 2025.12.00 / Material3 1.5.0-alpha18 / compileSdk 35 / versionCode 26 / versionName "0.8.18"）+ APK SHA-256 `933c915015d18af27d59fc9b156d97c6ad81efc629c3a70d404d2036145431b8`（19266156 bytes）+ signer `CN=Android Debug` |
| agent-pr-review | ✅ SAFE TO COMMIT | Intent matches diff / no failure-mode / assembleDebug + assembleRelease + testDebugUnitTest 全绿 |

详见 [docs/release-receipts/v0.8.18-receipt.md](release-receipts/v0.8.18-receipt.md) + [SESSION_LOG.md](SESSION_LOG.md) 最后一节。

### v0.8.18 RBR Exception E1（debug 签名 fallback）

- **Exception**: GitHub Actions 账单问题导致 Release workflow 无法运行，正式 keystore 存储在 GitHub Secrets 本地不可访问
- **Compensating control**: 本地构建 + gh 上传（与 v0.8.14/v0.8.15/v0.8.16/v0.8.17 一致，用户已接受）+ Release notes 明示 debug 签名 + GitHub Release 历史保留所有旧版 APK 可回滚
- **Expiry**: GitHub Actions 账单问题解决后，重新用正式 keystore 构建并替换 v0.8.18 release APK

## 🆕 最新改动（2026-07-23 v0.7.5 610综合卷科目深度修复）

**5 轮迭代修复**（详见 [SESSION_LOG.md](SESSION_LOG.md) 2026-07-23 v0.7.5 条目）：

| 轮次 | 内容 | seed 版本 |
|------|------|-----------|
| 第1轮 | UI 修复：Type.kt 字号 + CardsScreen 滚动 + GraphCanvas 重写 | — |
| 第2轮 | 综合卷 604/605 科目标签重新分类（57 题） | 2.4.0 → 2.5.0 |
| 第3轮 | 2022年806答案错位修复 + 合并题拆分(苏轼+姚鼐/鲁迅+婉约词) + OCR清洗 | 2.5.0 → 2.6.0 |
| 第4轮 | 深度复查 + 合并题拆分(历史散文+诗经/杨朔+20世纪) + OCR残留清理 | 2.6.0 → 2.7.0 |
| 第5轮 | 610 综合卷 127 题科目重新分类（古代36/现当代32/外国26/理论33） | 2.7.0 → 2.8.0 |

**累计修复统计**：拆分合并题 4 道 / 修复答案错位 5 道 / 清理 OCR 噪音 50+ 处 / 科目重新分类 156 道 / 分值提取 36 道 / 题型推断 9 道

**v0.7.0 → v0.7.2 → v0.7.4 → v0.7.5 演进**：

| 版本 | 状态 | 内容 |
|------|------|------|
| v0.7.0 | ❌ 知识点不显示 | 接入 909 知识点 + seed v2.1.0 |
| v0.7.2 | ✅ 修复导入 | GraphSkeleton.SUBJECT_ID 改 `subj_02` + seed v2.2.0 |
| v0.7.4 | ✅ 修复体验 | 答案错位+合并题+图谱+UI + seed v2.7.0 |
| v0.7.5 | ✅ 修复科目 | 610 综合卷 127 题科目重新分类 + seed v2.8.0 |

---

**P1 大型任务 5 Wave 全部完成（7 commits，38 测试新增，258 tests 0 failures）**：

| Wave | commit | 内容 | 测试增量 |
|------|--------|------|---------|
| Wave 1 | `148dad6` | 数据库 schema v4→v5 统一迁移（NF-PP4/PP5/PP6 合并单次 Migration） | +0 |
| Wave 2.1 | `302165e` | NF-T4 Float 类型统一消除 DB↔FSRS 精度损失 | +5 |
| Wave 2.2 | `6adeb40` | NF-PP4 SchedulingRepositoryTest 真实事务验证 | +3 |
| Wave 2.3 | `55001c0` | NF-PP6 ChatRepository Hilt 绑定 + ChatRepositoryImplTest | +6 |
| Wave 2.4 | `eb944a5` | NF-PP5 WrongAnswerRepository + Hilt 绑定 + 7 测试 | +7 |
| Wave 3.1 | `26ae190` | NF-PP6 AiAssistantViewModel 持久化 + Screen 新建对话按钮 | +3 |
| Wave 3.2 | `c829e4f` | NF-PP5 错题本完整闭环（接口提取 + 业务层 + UI 层） | +8 |
| Wave 4 | `f297344` | P1-PG ProGuard 规则补齐（13 个 .pro，不启用 minify） | +0 |

详见 [plans/p1-large-tasks-plan.md](plans/p1-large-tasks-plan.md) + [SESSION_LOG.md](SESSION_LOG.md) 最后两节。

---

**启动图标重设计 + 第五轮深度审计 P0 + P1 + P2 第一批（21 项 + 图标，6 commits）**：

| 批次 | commit | 项数 | 核心内容 |
|------|--------|------|----------|
| P0 第一批 | `d6532e4` | 6 | Converter 降级 + rateCard 事务 + 输入框受控 + 错误清理顺序 + flatMapLatest opt-in + VERSION_NAME 同步 |
| P1-2A 批 | `4496242` | 6 | LIKE 转义 + ViewModel catch + retry loading 反馈 + roundToInt 对称 + FakeDAO 契约（P1-12 暂缓） |
| P1-2B 批 | `76c5084` | 4 | ContentSource 迁移到 core/common + ThemeViewModel 迁移到 designsystem + observeDue tickFlow 刷新 + SocraticTutor 三阶段短路 |
| P1-2C 批 | `8ba2973` | 2+1 暂缓 | RecallChecker/AiAssistantViewModel 迁移到 chatResult + 删除 getAllVerifiedKnowledgePoints 死代码（P1-10 R8 暂缓待 emulator 实测） |
| P2 第一批 | `a0bd1cf` | 3 | securityCrypto 死声明清理 + CardSplitter.indexToChinese 扩展到 1-99 + WeakSubgraphDetector 孤儿边日志 |
| 图标重设计 | `6a1175c` | - | ic_launcher_foreground + monochrome 替换为"展开的书 + 文字负空间 evenOdd 镂空"，保留墨黑/米色品牌色，版本 v0.5.0 |

详见 [SESSION_LOG.md](SESSION_LOG.md) 最后一节 + [docs/design/icon-redesign.md](design/icon-redesign.md)。

## 📊 项目进度

### Android App 开发

| 阶段 | 状态 | 详情 |
|------|------|------|
| Phase 2 Android 骨架 | ✅ 完成 | 多模块架构 + Room 数据库 + Hilt DI |
| Phase 3 FSRS 调度 | ✅ 完成 | FSRS-6 自实现 + 三层记忆调度 |
| Phase 4 AI 服务 | ✅ 完成 | OpenAI 兼容协议适配 |
| Phase 5 UI 增强 | ✅ 完成 | 9 个 Screen + M3 基础组件 |
| Release 配置 | ✅ 完成 | 签名 APK + GitHub Release v0.1.0 |
| KSU 风格 UI 升级 | ✅ Phase 0-3 完成 + 已合并 main | 4 个 KSU 组件 + 9 个 Screen 迁移 + CI 全绿 |
| UI 改造闭环 | ✅ Phase 1-5 完成 | GroupedCard 增强 + 2 Screen 重构 + 4 Preview + 8 测试 |
| UI 精修 v0.3 | ✅ 完成 | 卡片镜像修复 + AI 入口调整 + 全面动画优化 |
| 第三轮深度审计 v0.4.2 | ✅ 4 Batch 修复完成 | FSRS 算法 + 数据安全 + 测试有效 + UX/契约（24 文件，207 tests） |
| 第四轮深度审计 v0.5.0 | ✅ Phase 2 P1/P2 修复完成 | 13 commits，59 项修复，Release v0.3.0 已发布 |
| v0.6 M3 Expressive 精修 | ✅ Phase 1-5 完成 | 5 commits：导航重构 + 动效字体 + 大屏自适应 + 组件升级 + 视觉精修（220 tests 保持） |
| Release v0.4.0 | ✅ 已发布 | 包含 v0.5.0 Phase 2 第三批 + v0.6 全部改动，debug 签名 fallback |

### 第四轮深度审计 v0.5.0（2026-07-15）

> 详见 [plans/full-audit-v0.5.0-deep.md](plans/full-audit-v0.5.0-deep.md)（5 Phase）

| Phase | 状态 | 内容 |
|-------|------|------|
| Phase 1 数据持久化 | ⏳ 待执行 | 1.C AI 对话持久化 / 1.D 进程被杀恢复 |
| Phase 2 代码质量 | 🔄 进行中 | 15 个维度（2.A-2.O），已完成 10 个 commit |
| Phase 3 依赖升级 | ⏳ 待执行 | 依赖升级路径 |
| Phase 4 测试矩阵 | ⏳ 待执行 | 25 项 emulator 测试 |
| Phase 5 修复执行 | ⏳ 待执行 | 7 Batch 修复 |

#### Phase 2 已完成 commits（13 个）

| Commit | 内容 | CI |
|--------|------|-----|
| `dd3ff06` | P0-AUDIT-1 review_logs elapsedDays + P2 语义修正 | ❌ 账单 |
| `ca3ceea` | P0-STAB-1 批量 @Immutable 注解 | ❌ 账单 |
| `c0e2775` | P1-AUDIT-5 LEFT JOIN + P1-AUDIT-2 ORDER BY + P1-CI-1/2 + P1-S-1 + P2-LAZY-1 + P2-REC-5 | ❌ 账单 |
| `63f5375` | P1 Repository Flow 异常处理（23 处 .catchAndLog） | ❌ 账单 |
| `53a0c46` | P1-CI-4 keystore 密码随机化 + P1-AUDIT-4 种子版本感知升级 | ❌ 账单 |
| `f9fc9c5` | P2 性能（GraphScreen remember + FlipCard derivedStateOf） | ❌ 账单 |
| `5d00824` | P1-AUDIT-3 AntiRoteMemorization 参数命名 + NF-T6 防御性编码 | ❌ 账单 |
| `01a1049` | 2.O/2.E 资源（monochrome icon + M3 DayNight + values-night） | ❌ 账单 |
| `3179911` | 2.N 业务边界（LIKE 转义 + query 限制 + List→Set） | ❌ 账单 |
| `0dd5b0f` | NF-BB2 SocraticTutor 三阶段上下文传递 | ❌ 账单 |
| `96d9755` | 构建修复（compose runtime + testOptions） | ❌ 账单 |
| `d1cb4d7` | 第二批 8 项（性能+无障碍+死依赖） | ❌ 账单 |
| `40972fc` | 第三批 4 项（NF-T7 Rating.index + NF-T8 Random 注入 + NF-A2 L2 GOOD 档 + NF-E8 DecryptionException） | ❌ 账单 |

### v0.6 M3 Expressive 精修（2026-07-14）

> 详见 [plans/m3-expressive-polish-v0.6.md](plans/m3-expressive-polish-v0.6.md)（5 Phase）

| Phase | 状态 | 内容 | Commit |
|-------|------|------|--------|
| Phase 1 导航重构 | ✅ 完成 | 底部第 5 Tab 砍 AI 改"设置"；AiAssistant 改子路由 Push/Pop | `eb146ef` |
| Phase 2 动效 + 字体 | ✅ 完成 | animateColorScheme（35 颜色角色 spring 过渡）+ Push/Pop 弹簧 + Display/Headline SemiBold | `8bf8d98` |
| Phase 3 大屏自适应 | ✅ 完成 | material3-adaptive 1.2.0 + WenyanWideNavigationRail + WindowSizeClass 三档 | `0b5d4e6` |
| Phase 4 组件升级 | ✅ 完成 | WenyanLoadingIndicator 替代 7 处 CircularProgressIndicator + SegmentedButton 替代 FilterChip | `cc509d0` |
| Phase 5 视觉精修 | ✅ 完成 | Preview 补全（6 个）+ SettingsScreen 调色板风格 SegmentedButton 统一 + Shapes extraLarge 28→32dp | `e09ff81` |
| 验证 | ✅ 完成 | assembleDebug SUCCESSFUL + assembleRelease SUCCESSFUL（沙箱绕过 lint/signing）+ 220 tests 0 failures | - |
| Release | ✅ 已发布 | v0.4.0（含 v0.5.0 + v0.6 全部改动，debug 签名 fallback） | `9ada352` |

### 第三轮深度审计 v0.4.2（2026-07-14）

> 详见 [plans/full-audit-v0.4.2-deep.md](plans/full-audit-v0.4.2-deep.md)

| Batch | 状态 | 内容 | 文件数 |
|-------|------|------|--------|
| Batch 1 | ✅ 完成 | FSRS 算法正确性（F-01 权重索引 + F-02 easyBonus + F-03 一致性 + F-05 roundToInt）+ 4 回归测试 | 2 |
| Batch 2 | ✅ 完成 | 数据安全 P0（allowBackup=false + versionCode/Name 修正 + fallbackToDestructiveMigrationOnDowngrade + UUID 替代 + withTransaction） | 7 |
| Batch 3 | ✅ 完成 | 测试有效性 P0（assert→assertEquals + NavigationBar 契约测试 + 清理冗余） | 3 |
| Batch 4 | ✅ 完成 | UX/契约 P1（枚举容错 + 版本号显示 + 18 处 ORDER BY + 3 处 N+1 修复 + rateCard 错误处理 + editingId 捕获） | 10+ |
| 验证 | ✅ 完成 | assembleDebug SUCCESSFUL + testDebugUnitTest 207 tests 0 failures | - |

### UI 精修 v0.3（2026-07-13）

> 用户反馈 4 个问题：①记忆卡片翻转镜像 ②删除右上角导师信息 ③AI 放右上角 ④动画不够干净利落

| Phase | 状态 | 内容 | Commit |
|-------|------|------|--------|
| Phase 1 | ✅ 完成 | 卡片镜像修复：cameraDistance + shouldShowBack 阈值切换 + 容器色 animateColorAsState + 评分按钮 AnimatedVisibility + 进度文本 animateContentSize + 6 个纯函数测试 | `70cf54a` |
| Phase 2 | ✅ 完成 | 删除 MentorInfoScreen + ROUTE_MENTOR + 4 主屏 TopBar 加 AI IconButton（SmartToy）| `267d3ff` |
| Phase 3 | ✅ 完成 | WenyanMotion 统一动画 tokens（Duration/Easing）+ NavHost 全局 Tab fade transition + 3 子路由 Push/Pop slide transition | `1a244ef` |
| Phase 4 | ✅ 完成 | 7 屏状态切换 Crossfade 替代 if/else 硬切（KnowledgeScreen/QuizScreen/CardsScreen/GraphScreen/AiAssistantScreen/ApiConfigScreen/KnowledgePointDetailScreen）| `deb7515` |
| Phase 5 | ✅ 完成 | 4 LazyColumn animateItem（KnowledgeScreen/QuizScreen/AiAssistantScreen/ApiConfigScreen）+ SettingsScreen 动态色彩开关 AnimatedVisibility | `add1f43` |
| Phase 6 | ✅ 完成 | 全量验证（assembleDebug + 190 tests）+ 文档更新 | - |

### KSU 风格 UI 升级（方案 C）

| 阶段 | 状态 | 内容 |
|------|------|------|
| Phase 0 | ✅ 完成 | 升级 Kotlin 2.3.10 + KSP 2.3.2 + Hilt 2.57.1 + Room 2.7.0 + material3 1.5.0-alpha18 |
| Phase 1 | ✅ 完成 | 4 个 KSU 组件（WenyanLargeTopAppBar/WenyanNavigationBar/GroupedCard/HierarchicalListItem）+ 首个 Compose UI 测试 |
| Phase 2 | ✅ 完成 | WenyanApp.kt + 9 个 Screen 全部迁移到 WenyanLargeTopAppBar |
| Phase 3 | ✅ 完成 | 文档更新（SESSION_LOG/VERSION-MATRIX/FAILED-ATTEMPTS/STATUS） |
| CI 修复 | ✅ 完成 | 仓库顺序 + Gradle 8.14.4 + Metaspace 1g + testDebugUnitTest |
| 合并 main | ✅ 完成 | PR #1 squash merge → `3efe678`，CI run 29211066998 全绿 |

### OCR 资料数字化

| 项 | 值 |
|----|-----|
| 进度 | 约 60%（125/208 文件） |
| 已完成 | P1 核心教材（郑克鲁/聂珍钊等） |
| 剩余 | P4 文学理论 + P5 其他（约 47 个 PDF） |
| 详情 | [reference/OCR_PIPELINE.md](reference/OCR_PIPELINE.md) |

## 🎯 下一步优先级

1. **P0**：跑 emulator 实测 v0.5.0 — 验证图标显示 + P0/P1/P2 修复（rateCard 事务 + 输入框受控 + Flow 刷新 + 三阶段短路 + ContentSource/Theme 迁移 + RecallChecker/AiAssistantViewModel 错误传播 + indexToChinese 扩展 + 孤儿边日志 + 卡片翻转 + AI 入口 + Tab/列表动画 + 深色模式 + 平板 WideNavigationRail + 主题切换颜色动画 + Push/Pop 弹簧 + LoadingIndicator + SegmentedButton + IME 适配 + 清空确认 + 错误重试 + 长文本省略 + 无障碍合并）
2. **P0 阻塞**：等待 GitHub Actions 账单问题解决 — 27 个 commit 待 CI 验证（v0.5.0 13 个 + v0.6 6 个 + UI 修复 3 个 + 深度审计 5 个 + 图标 1 个，部分重叠）— 不影响 Release，已通过本地构建 + API 上传绕过
3. **P0**：CI 账单问题解决后，可删除 v0.5.0 tag 重新打 tag 触发正式签名 Release（可选 — debug 签名 APK 已可用，正式签名仅供完整性校验）
4. **P1-10 待 emulator 实测后启用**：Release R8 + ProGuard 规则补全（反射/序列化/规则遗漏风险，沙箱无 emulator 暂缓）
5. **P2 剩余项**（需 emulator 实测或 schema 迁移）：
   - NF-UC7（BackHandler）：需 emulator 实测验证 UX
   - NF-D6/NF-DS12（schema 1.json）：需从 git 历史考古或反推
   - graph_edges / api_configs.is_current UNIQUE 约束：需 schema 迁移
   - Certificate Pinning：需 emulator 实测
   - NF-PP3/NF-PP7/NF-DS13：审计/调研任务（无代码改动）
6. **P1 大型任务**（需用户确认优先级）：
   - P1-PG-1/2/3：启用 R8 + 补齐 ProGuard 规则（与 P1-10 同一问题）
   - NF-PP4：复习日志双写统一
   - NF-PP5：错题本实现
   - NF-PP6：AiAssistantViewModel 消息持久化（将启用 chat_history/ai_conversations 表）
   - NF-T4：MemoRecordMapper Float↔Double 精度（需 schema 迁移）
   - ~~NF-D3：observeDue Flow 不刷新~~ ✅ 已由 P1-1（commit `76c5084`）修复
7. **P1**：v0.5.0 Phase 2 剩余维度审计（strings.xml / 错误处理 / Compose 副作用 / DataStore Key 治理）
8. **P2 剩余 UI 项**（可选）：
   - ConfigCard 架构级冲突：整卡点击 + 内部编辑/删除按钮，需重构
   - CardRenderer FlipCard 超长背面答案溢出：加 `verticalScroll`
   - @Preview 补齐（6 个已有，可再补 4 个）
   - 平板双栏布局（已有 WideNavigationRail，可加 list-detail）
9. **P2**：OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（替换 stage2-sample）
10. **P3**：release.yml "Verify keystore" 步骤隐藏 bug（Line 63-70，KEYSTORE_BASE64 未配置时失败）
11. **P4**：架构重构 — ~~ReviewRepository.getAllVerifiedKnowledgePoints 已成事实死代码~~ ✅ 已由 P1-9（commit `8ba2973`）删除

## 📦 已交付

- GitHub Release v0.1.0（2026-07-12）+ v0.2.0（2026-07-13）+ v0.3.0（2026-07-15，debug 签名）+ v0.4.0（2026-07-14，debug 签名 fallback）+ v0.5.0（2026-07-16，本地构建 + GitHub API 上传）+ **v0.7.0（2026-07-16，909 知识点）+ v0.7.2（2026-07-16，修复 FK 回滚）+ v0.7.4（2026-07-23，UX 修复 + GraphCanvas 重写）+ v0.7.5（2026-07-23，610 综合卷科目重新分类）+ v0.7.6（2026-07-24，数据瘦身 + 图谱时间轴）+ v0.8.1（2026-07-24，图谱三模式 + 形状编码 100% 覆盖）+ v0.8.2-v0.8.17（多轮深度审计 + 修复 + UI 打磨）+ v0.8.18（2026-07-27，启动图标 v3 + Logging.kt）**
- 签名 APK：`wenyan-v0.8.18.apk`（v0.8.18，debug 签名 fallback — Exception E1，启动图标 v3 印章文 + Logging.kt 统一日志门面）
- **v0.8.18 工程化审查**（per staff-engineer-mode Iron Law）：PRR ✅ + RBR ✅ + agent-pr-review ✅，详见 [docs/release-receipts/v0.8.18-receipt.md](release-receipts/v0.8.18-receipt.md)
- **v0.8.18 RBR Exception E1**：CI 账单问题导致 release workflow 无法运行，本地构建 + gh 上传（与 v0.8.14-v0.8.17 一致，用户已接受）；CI 恢复后用正式 keystore 重新构建并替换 v0.8.18 asset
- **沙箱编译验证 v0.7.2 通过**（2026-07-23）：assembleDebug + 258 tests 0 failures，详见 [03-FAILED-ATTEMPTS.md #015](03-FAILED-ATTEMPTS.md)
- **gradlew wrapper 补齐**（2026-07-23）：`gradlew` / `gradlew.bat` / `gradle-wrapper.jar` 三件套此前从未入仓库，CI runner 无法用 wrapper 启动，现已修复
- KSU 风格 UI 升级 Phase 0-3（4 个组件 + 9 个 Screen 迁移，已合并 main + CI 全绿）
- UI 改造闭环 Phase 1-5（GroupedCard 增强 + 2 Screen 重构 + 4 Preview + 15 个组件测试）
- UI 统一与死组件清理（KnowledgePointDetailScreen 统一 + 删除 4 个死组件，174 tests 0 failures）
- P0 双修（release.yml CI 修复 + SeedDataLoader 接通，App 启动自动导入 stage2-sample 数据）
- P1 修复（KnowledgeViewModel 科目筛选 + 科目名显示修复，DAO JOIN + 10 测试，184 tests 0 failures）
- UI 精修 v0.3（卡片镜像修复 + 导师信息删除 + AI 入口调整 + 全面动画优化，190 tests 0 failures）
- 第三轮深度审计 v0.4.2 修复（4 Batch：FSRS 算法 + 数据安全 + 测试有效 + UX/契约，24 文件，207 tests 0 failures）
- 第四轮深度审计 v0.5.0 Phase 2 P1/P2 修复（13 commits：Flow 异常处理 + LEFT JOIN + keystore 随机化 + 种子版本升级 + 性能优化 + AntiRoteMemorization 收尾 + 资源配置 + 业务边界 + SocraticTutor 上下文传递 + 第二批性能/无障碍/死依赖 + 第三批 FSRS 解耦/可测/L2 评分/加解密异常）
- **v0.6 M3 Expressive 精修 Phase 1-5**（5 commits：导航重构 + 动效字体 + 大屏自适应 + 组件升级 + 视觉精修，220 tests 保持）
- **Release v0.4.0**（含 v0.5.0 + v0.6 全部改动，220 tests 0 failures 0 errors）
- **UI 全面审查 + P0/P1/P2 三批修复**（3 commits：IME 适配 + 清空确认 + 错误处理 + 长文本溢出 + 无障碍 mergeDescendants + 文案/字重统一，220 tests 保持）
- **第五轮深度审计 P0 + P1 + P2 第一批**（5 commits：Converter 降级 + rateCard 事务 + 输入框受控 + Flow 异常 catch + retry loading + roundToInt + LIKE 转义 + FakeDAO 契约 + ContentSource/ThemeViewModel 迁移 + observeDue tickFlow + SocraticTutor 三阶段短路 + RecallChecker/AiAssistantViewModel chatResult 迁移 + getAllVerifiedKnowledgePoints 死代码删除 + securityCrypto 死声明清理 + indexToChinese 扩展 + 孤儿边日志，220 tests 保持）
