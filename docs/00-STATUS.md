# 当前状态快照

> **AI 新会话第一份要读的文件。10 秒了解项目当前状态。**
> 最后更新：2026-07-12

## ✅ 当前状态

**CI 编译阻塞已解除** — KSU 风格 UI 升级 Phase 0-2 已完成

| 项 | 值 |
|----|-----|
| 最新 commit | `a85cc68` |
| 编译状态 | ✅ assembleDebug + testDebugUnitTest 全通过 |
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
| KSU 风格 UI 升级 | ✅ Phase 0-2 完成 | 4 个 KSU 组件 + 9 个 Screen 迁移 |

### KSU 风格 UI 升级（方案 C）

| 阶段 | 状态 | 内容 |
|------|------|------|
| Phase 0 | ✅ 完成 | 升级 Kotlin 2.3.10 + KSP 2.3.2 + Hilt 2.57.1 + Room 2.7.0 + material3 1.5.0-alpha18 |
| Phase 1 | ✅ 完成 | 4 个 KSU 组件（WenyanLargeTopAppBar/WenyanNavigationBar/GroupedCard/HierarchicalListItem）+ 首个 Compose UI 测试 |
| Phase 2 | ✅ 完成 | WenyanApp.kt + 9 个 Screen 全部迁移到 WenyanLargeTopAppBar |
| Phase 3 | ✅ 完成 | 文档更新（SESSION_LOG/VERSION-MATRIX/FAILED-ATTEMPTS/STATUS） |

### OCR 资料数字化

| 项 | 值 |
|----|-----|
| 进度 | 约 60%（125/208 文件） |
| 已完成 | P1 核心教材（郑克鲁/聂珍钊等） |
| 剩余 | P4 文学理论 + P5 其他（约 47 个 PDF） |
| 详情 | [reference/OCR_PIPELINE.md](reference/OCR_PIPELINE.md) |

## 🎯 下一步优先级

1. **P0**：推送到远端 GitHub，等 CI 全绿
2. **P1**：跑 emulator 实测 LargeFlexibleTopAppBar 滚动折叠效果
3. **P2**：用 GroupedCard 改造 SettingsScreen（当前仍是 TonalCard 平铺）
4. **P3**：用 HierarchicalListItem 改造 KnowledgePointDetailScreen 关联知识点区域
5. **P4**：OCR 完成后跑知识提取管线 → 生成 seed_data.json

## 📦 已交付

- GitHub Release v0.1.0（2026-07-12）
- 签名 APK：`wenyan-v0.1.0.apk` + `wenyan-latest.apk`（14.7 MB）
- KSU 风格 UI 升级 Phase 0-2（4 个组件 + 9 个 Screen 迁移）
