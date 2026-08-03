# 文研App 全面检查报告与改进计划（v0.9.26 后）

> 日期：2026-08-03
> 方式：3 路并行审查（代码质量 / 测试构建 / 仓库合规 UI）+ 人工逐项核实
> 状态：检查完成，改进计划待执行（用户确认后分批实施）

## 一、总体结论

- **无 P0 崩溃/数据丢失级问题**，工程质量整体良好（518 单测、R8、迁移测试、i18n、convention plugin 均落地）
- 发现 **P1 × 8、P2 × 11、P3 × 5**，主要问题集中在：AI 并发竞态、新增功能测试缺口、CI 覆盖不足、仓库卫生、合规缺失

---

## 二、问题清单

### 🔴 P1（明显缺陷，建议优先修）

| # | 位置 | 问题 | 修复建议 |
|---|------|------|----------|
| 1 | `AiAssistantViewModel.kt:120` | **aiJob 竞态**：`finally { aiJob = null }` 无条件清空。任务 A 停止中（NonCancellable DB 写入延迟），用户快速发新消息 → B 启动 → A finally 抹掉 B 引用 → 停止失效/并发启动/半截回复写错会话 | `if (coroutineContext[Job] == aiJob) aiJob = null` |
| 2 | `AiModule.kt:129-136` | **Retry-After 无上限 + Thread.sleep 不可中断**：服务商返回大值（60s+）长阻塞 IO 线程 + 占 Semaphore 槽位，停止生成无效 | clamp 到 `MAX_BACKOFF_MS`（5s）；评估改挂起 `delay` |
| 3 | 新增功能零测试 | commit `ad86909` 5 项（Semaphore 并发 / Retry-After / LazyColumn / RAG VERIFIED）全无测试 | 补单测：`AiServiceImplTest` 并发、`RetryInterceptorTest` header、DAO 过滤、LazyColumn 可省 |
| 4 | `android.yml` | **CI 只跑 debug，不跑 assembleRelease(R8)** → R8 问题只在发版 tag 暴露 | 加 `assembleRelease` 步骤（或 `check`） |
| 5 | `android.yml` | **androidTest（MigrationTest）CI 不跑**（无 emulator 步骤） | 加 emulator 任务（可先只跑迁移测试） |
| 6 | `release-assets/` | **4 个旧 APK 共 76MB 入库**（git 历史已涨） | `git rm --cached` + .gitignore（GitHub Release 分发） |
| 7 | `AGENTS.md` | **过期**：状态停在 v0.9.18；第 81-89 行混入他项目内容（Koa/Express/OCR D盘） | 更新到 v0.9.26 + 清理他项目内容 |
| 8 | 全项目 | **无隐私政策/用户协议**（App 有网络/安装/AI 数据外发权限） | 补隐私政策 + 用户协议（docs + App 内入口） |

### 🟡 P2（小问题/改进）

| # | 位置 | 问题 | 建议 |
|---|------|------|------|
| 9 | `AiModule.kt:76` | callTimeout(90s) 可能掐断超长合法流式回复 | 权衡可接受；记录/可调 |
| 10 | `AiAssistantScreen.kt:330` | 流结束闪烁 loading（streamingContent=null 但 isLoading 未置 false） | 流完成先置 isLoading=false |
| 11 | `AiAssistantViewModel.kt:240-256` | 流中途失败 partial 内容被存为正式消息 | 失败时不存 partial（或标记截断） |
| 12 | `KnowledgePointDetailScreen.kt` | i18n 不彻底：78 处硬编码中文；text_01..32 无意义命名 | 补全 + 语义化命名 |
| 13 | `KnowledgePointDetailScreen.kt:121` | rememberLazyListState 未用 rememberSaveable（旋转丢位置） | 换 rememberSaveable（非回归） |
| 14 | `KnowledgePointDetailScreen.kt:202` | item("multi_perspective") 无条件加入空 item | 空时跳过 |
| 15 | `AiServiceImplTest.kt:89` 等 | 弱断言 `assertTrue(contains(A)||contains(B))` | 加强断言 |
| 16 | `release.yml:82-109` | 不校验 versionCode 递增（同码发版无法覆盖安装） | 加 versionCode 校验 |
| 17 | `feature/quiz/QuizScreen.kt` | 死代码：QuizScreen/QuizViewModel 685 行不再被导航引用 | 删除 |
| 18 | 根 `build.gradle.kts:6` | `kotlin.jvm` 插件死声明 | 删除 |
| 19 | `.tmp_essay_analysis.py` `.tmp-preview/` | 临时文件入库 | 删除 |
| 20 | `docs/plans/` + `SESSION_LOG.md` | 21+ 过期 plan 未归档；SESSION_LOG 6440 行/388K | 归档 + 截断 |

### 🟢 P3（建议/低优先）

| # | 位置 | 问题 | 建议 |
|---|------|------|------|
| 21 | `tools/` 73 文件 | 一次性脚本未归档（essay_fill 18 个、exam_answers JSON） | 归档/清理 |
| 22 | `libs.versions.toml` | retrofit 2.9.0（2020）等依赖过旧 | 升级会破坏（3.x 需换 converter），评估后定 |
| 23 | `CardsViewModel.kt:521` `Logging.kt:60` | TODO 遗留（review schema、Crashlytics） | 处理或记录 |
| 24 | 5 个超大文件 | CardsViewModelTest 2154 行 / CardsScreen 1309 等 | 拆分为可选 |
| 25 | `ApiConfigViewModel.kt:290` | validateBaseUrl 允许 http://（明文敞口） | 强制 https |

---

## 三、改进计划（分批执行，每批独立 commit + 验证）

### 批次 A：工程质量加固（P1-1~5，高优先）
1. **aiJob 竞态修复**（条件清空）+ 相关回归测试
2. **Retry-After 上限**（clamp 5s）+ 测试
3. **补 5 项新增功能测试**（Semaphore/Retry-After/LazyColumn/RAG VERIFIED）
4. **CI 增强**：android.yml 加 assembleRelease + emulator 迁移测试
5. **release.yml 加 versionCode 递增校验**

### 批次 B：仓库卫生（P1-6~7 + P2-17~20）
6. release-assets 76MB `git rm --cached` + .gitignore
7. AGENTS.md 清理过期 + 他项目内容
8. 删临时文件（.tmp_essay_analysis.py / .tmp-preview/）
9. 归档过期 docs + SESSION_LOG 截断
10. 删死代码（QuizScreen/QuizViewModel + kotlin.jvm 死声明）

### 批次 C：UI/体验打磨（P2-9~16）
11. 流结束 loading 闪烁修复
12. 流失败 partial 处理优化
13. i18n 补全（KnowledgePointDetailScreen）+ 语义化命名
14. rememberSaveable 保持旋转位置
15. LazyColumn 空 item 跳过
16. 弱断言测试加强

### 批次 D：合规与长期（P1-8 + P2/P3）
17. 隐私政策 + 用户协议（docs + App 入口）
18. validateBaseUrl 强制 https
19. TODO 清理 + 崩溃上报评估（Logging.kt:60 关联）
20. 依赖健康评估（retrofit 升级决策）

---

## 四、建议执行顺序

- **先做批次 A**（工程质量，直接消除竞态/安全/测试/CI 风险）
- **再批次 B**（仓库卫生，减仓库体积、清理文档）
- **批次 C/D 可并行/穿插**（体验与合规）

> 每次批次独立 commit + 全量单测 + assembleDebug/Release 验证，不引入回归。
