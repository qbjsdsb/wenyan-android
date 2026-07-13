# 当前状态快照

> **AI 新会话第一份要读的文件。10 秒了解项目当前状态。**
> 最后更新：2026-07-13

## ✅ 当前状态

**UI 精修 v0.3 完成** — 卡片镜像修复 + 导师信息删除 + AI 入口调整 + 全面动画优化。尚未发版。

| 项 | 值 |
|----|-----|
| 最新 commit | `add1f43`（main，LazyColumn animateItem + Settings Switch AnimatedVisibility） |
| 最新 Release | [v0.2.0](https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.2.0)（2026-07-13，v0.3 改动尚未发版） |
| 测试 | 190 tests 0 failures（184 原有 + 6 FlipCardLogic） |
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

### UI 精修 v0.3（2026-07-13）

> 用户反馈 4 个问题：①记忆卡片翻转镜像 ②删除右上角导师信息 ③AI 放右上角 ④动画不够干净利落

| Phase | 状态 | 内容 | Commit |
|-------|------|------|--------|
| Phase 1 | ✅ 完成 | 卡片镜像修复：cameraDistance + shouldShowBack 阈值切换 + 容器色 animateColorAsState + 评分按钮 AnimatedVisibility + 进度文本 animateContentSize + 6 个纯函数测试 | `70cf54a` |
| Phase 2 | ✅ 完成 | 删除 MentorInfoScreen + ROUTE_MENTOR + 4 主屏 TopBar 加 AI IconButton（SmartToy）| `267d3ff` |
| Phase 3 | ✅ 完成 | WenyanMotion 统一动画 tokens（Duration/Easing）+ NavHost 全局 Tab fade transition + 3 子路由 Push/Pop slide transition | `1a244ef` |
| Phase 4 | ✅ 完成 | 7 屏状态切换 Crossfade 替代 if/else 硬切（KnowledgeScreen/QuizScreen/CardsScreen/GraphScreen/AiAssistantScreen/ApiConfigScreen/KnowledgePointDetailScreen）| `deb7515` |
| Phase 5 | ✅ 完成 | 4 LazyColumn animateItem（KnowledgeScreen/QuizScreen/AiAssistantScreen/ApiConfigScreen）+ SettingsScreen 动态色彩开关 AnimatedVisibility | `add1f43` |
| Phase 6 | ✅ 完成 | 全量验证（assembleDebug + 190 tests）+ 文档更新 | 本次 |

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

1. **P0**：跑 emulator 实测 v0.3 改动 — 本地 `assembleDebug`，验证：①记忆卡片翻转无镜像 ②4 主屏 TopBar 右上角 AI 入口可跳转 ③Tab 切换有 fade transition ④loading→content 平滑 Crossfade ⑤列表项增删有 animateItem 过渡 ⑥Settings 动态色彩开关时种子色区块平滑展开/收起
2. **P1**：可选 — 发 Release v0.3.0（确认 CI 全绿后 `git tag v0.3.0 && git push origin v0.3.0`）
3. **P2**：OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（替换 stage2-sample）
4. **P3**：可选 — 用 GroupedCard 改造其他 Screen（如 ApiConfigScreen）保持视觉一致性
5. **P4**：release.yml "Verify keystore" 步骤隐藏 bug（Line 63-70，KEYSTORE_BASE64 未配置时失败）
6. **P5**：架构重构 — ReviewRepository.getAllVerifiedKnowledgePoints 已成事实死代码；getVerifiedWithSubject 职责应在 KnowledgeRepository（详见 SESSION_LOG 第四条）

## 📦 已交付

- GitHub Release v0.1.0（2026-07-12）+ v0.2.0（2026-07-13）
- 签名 APK：`wenyan-v0.2.0.apk` + `wenyan-latest.apk`（v0.2.0，包含 P1 修复）
- KSU 风格 UI 升级 Phase 0-3（4 个组件 + 9 个 Screen 迁移，已合并 main + CI 全绿）
- UI 改造闭环 Phase 1-5（GroupedCard 增强 + 2 Screen 重构 + 4 Preview + 15 个组件测试）
- UI 统一与死组件清理（KnowledgePointDetailScreen 统一 + 删除 4 个死组件，174 tests 0 failures）
- P0 双修（release.yml CI 修复 + SeedDataLoader 接通，App 启动自动导入 stage2-sample 数据）
- P1 修复（KnowledgeViewModel 科目筛选 + 科目名显示修复，DAO JOIN + 10 测试，184 tests 0 failures）
- UI 精修 v0.3（卡片镜像修复 + 导师信息删除 + AI 入口调整 + 全面动画优化，190 tests 0 failures）
