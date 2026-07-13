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

## 7. 当前状态（2026-07-13）

**✅ 无阻塞** — UI 统一与死组件清理完成，KSU 风格 UI 升级 + UI 闭环 + 死组件清理均已合并到 main。

- 最新 commit：`2f83ac3`（main，删除 4 个死组件）
- UI 统一与清理：2 个 commit（`ebad848` Phase 1 + `2f83ac3` Phase 2）
- 验证：`assembleDebug` SUCCESSFUL（412 tasks）+ `testDebugUnitTest` 174 tests 0 failures
- 详见 [docs/00-STATUS.md](docs/00-STATUS.md)

## 8. 项目阶段总览

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 1 资料数字化 | 约 60% | OCR 处理 125/208 文件 |
| Phase 2 Android 骨架 | ✅ 完成 | 多模块架构 + 数据库 |
| Phase 3 FSRS 调度 | ✅ 完成 | FSRS-6 自实现 + 三层记忆 |
| Phase 4 AI 服务 | ✅ 完成 | OpenAI 兼容协议 |
| Phase 5 UI 增强 | ✅ 完成 | 9 个 Screen + M3 组件 |
| Release 配置 | ✅ 完成 | 签名 + GitHub Release v0.1.0 |
| KSU 风格 UI 升级 | ✅ 完成 | Phase 0-3 + CI 修复，已合并 main |
| UI 改造闭环 | ✅ 完成 | GroupedCard 增强 + 2 Screen 重构 + 4 Preview + 15 组件测试 |
| UI 统一与死组件清理 | ✅ 完成 | KnowledgePointDetailScreen 统一 + 删除 4 个死组件（174 tests） |

## 9. 下一步优先级

1. **P0**：跑 emulator 实测 LargeFlexibleTopAppBar 滚动折叠效果（组件已就绪，缺真机视觉验证）
2. **P1**：OCR 完成后跑知识提取管线 → 生成 seed_data.json
3. **P2**：可选 — 用 GroupedCard 改造其他 Screen（如 ApiConfigScreen）保持视觉一致性（注意：ApiConfigScreen 的 ConfigCard 有 4 行元信息 + 2 操作按钮，GroupedCardItem API 无法承载，需先扩展组件或保留现状）
