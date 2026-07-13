# 当前状态快照

> **AI 新会话第一份要读的文件。10 秒了解项目当前状态。**
> 最后更新：2026-07-13

## ✅ 当前状态

**P0 双修完成** — release.yml CI bug 修复 + SeedDataLoader 接通（App 从"能编译"变成"能用"）

| 项 | 值 |
|----|-----|
| 最新 commit | `07c3a6d`（main，接通 SeedDataLoader） |
| P0 双修 | 2 个 commit（`ff19231` CI 修复 + `07c3a6d` SeedDataLoader 接通） |
| CI 状态 | ✅ run 29272102909 全绿（12/12 步骤，20m13s） |
| PR | [#1](https://github.com/qbjsdsb/wenyan-android/pull/1) 已合并（KSU UI 升级 Phase 0-3） |
| 阻塞 | 无 |
| 详情 | [02-VERSION-MATRIX.md](02-VERSION-MATRIX.md) |

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

### UI 改造闭环计划（2026-07-13）

| 阶段 | 状态 | 内容 | Commit |
|------|------|------|--------|
| Phase 1 | ✅ 完成 | GroupedCard 增强（leadingIcon/description/分割线）+ 7 测试 | `da3f369` |
| Phase 2 | ✅ 完成 | SettingsScreen 4 个分组用 GroupedCard 重构 | `68e5946` |
| Phase 3 | ✅ 完成 | KnowledgePointDetailScreen 关联知识点用 GroupedCard 重构 | `c918411` |
| Phase 4 | ✅ 完成 | 4 个 @Preview（三态覆盖）+ 2 个组件测试（8 tests） | `f311a31` |
| Phase 5 | ✅ 完成 | assembleDebug + testDebugUnitTest（117 tests 0 failures）+ 文档更新 | 本次 |

### UI 统一与死组件清理（2026-07-13）

| 阶段 | 状态 | 内容 | Commit |
|------|------|------|--------|
| Phase 1 | ✅ 完成 | KnowledgePointDetailScreen 摘要+资料来源→GroupedCard，PerspectiveCard→TonalCardLow | `ebad848` |
| Phase 2 | ✅ 完成 | 删除 4 个死组件（WenyanTopAppBar/SectionHeader/LoadingState/HierarchicalListItem）| `2f83ac3` |
| Phase 3 | ✅ 完成 | assembleDebug（412 tasks）+ testDebugUnitTest（174 tests 0 failures）+ 文档更新 | 本次 |

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

1. **P0**：跑 emulator 实测 — 验证 SeedDataLoader 启动时导入数据（Logcat 无异常 + 各 Tab 有数据显示 + 重启不重复导入）+ LargeFlexibleTopAppBar 滚动折叠效果
2. **P1**：KnowledgeViewModel 2 个 bug（filterByCategory 不筛选 + subject 显示 "TEXTBOOK_NATIVE" 而非"古代文学"）
3. **P2**：OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（替换 stage2-sample）
4. **P3**：可选 — 用 GroupedCard 改造其他 Screen（如 ApiConfigScreen）保持视觉一致性
5. **P4**：release.yml "Verify keystore" 步骤隐藏 bug（Line 63-70，KEYSTORE_BASE64 未配置时失败）

## 📦 已交付

- GitHub Release v0.1.0（2026-07-12）
- 签名 APK：`wenyan-v0.1.0.apk` + `wenyan-latest.apk`（14.7 MB）
- KSU 风格 UI 升级 Phase 0-3（4 个组件 + 9 个 Screen 迁移，已合并 main + CI 全绿）
- UI 改造闭环 Phase 1-5（GroupedCard 增强 + 2 Screen 重构 + 4 Preview + 15 个组件测试）
- UI 统一与死组件清理（KnowledgePointDetailScreen 统一 + 删除 4 个死组件，174 tests 0 failures）
- P0 双修（release.yml CI 修复 + SeedDataLoader 接通，App 启动自动导入 stage2-sample 数据）
