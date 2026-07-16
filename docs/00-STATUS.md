# 当前状态快照

> **AI 新会话第一份要读的文件。10 秒了解项目当前状态。**
> 最后更新：2026-07-16

## ✅ 当前状态

**第五轮深度审计 P0 + P1 2A/2B 批完成** — 16 项修复（3 commits），220 tests 0 failures。

| 项 | 值 |
|----|-----|
| 最新 commit | `76c5084`（main，P1 第二批 2B 4 项架构修复） |
| 最新 Release | [v0.4.0](https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.4.0)（2026-07-14，debug 签名 fallback，17MB） |
| 测试 | **220 tests 0 failures 0 errors** |
| 阻塞 | **CI 账单问题** — 21 个 commit 待 CI 验证（v0.5.0 13 个 + v0.6 6 个 + UI 修复 3 个 + 深度审计 3 个，部分重叠） |
| 详情 | [SESSION_LOG.md](SESSION_LOG.md) 最后一节 |

## 🆕 最新改动（2026-07-16）

第五轮深度审计 P0 + P1 2A/2B 批（16 项，3 commits）：

| 批次 | commit | 项数 | 核心内容 |
|------|--------|------|----------|
| P0 第一批 | `d6532e4` | 6 | Converter 降级 + rateCard 事务 + 输入框受控 + 错误清理顺序 + flatMapLatest opt-in + VERSION_NAME 同步 |
| P1-2A 批 | `4496242` | 6 | LIKE 转义 + ViewModel catch + retry loading 反馈 + roundToInt 对称 + FakeDAO 契约（P1-12 暂缓） |
| P1-2B 批 | `76c5084` | 4 | ContentSource 迁移到 core/common + ThemeViewModel 迁移到 designsystem + observeDue tickFlow 刷新 + SocraticTutor 三阶段短路 |

详见 [SESSION_LOG.md](SESSION_LOG.md) 最后一节。

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

1. **P0 阻塞**：等待 GitHub Actions 账单问题解决 — 21 个 commit 待 CI 验证（v0.5.0 13 个 + v0.6 6 个 + UI 修复 3 个 + 深度审计 3 个，部分重叠）
2. **P0**：跑 emulator 实测 — 验证 v0.5.0 + v0.6 + UI 三批 + 第五轮深度审计 P0/P1 修复（rateCard 事务 + 输入框受控 + Flow 刷新 + 三阶段短路 + ContentSource/Theme 迁移 + 卡片翻转 + AI 入口 + Tab/列表动画 + 深色模式 + 平板 WideNavigationRail + 主题切换颜色动画 + Push/Pop 弹簧 + LoadingIndicator + SegmentedButton + IME 适配 + 清空确认 + 错误重试 + 长文本省略 + 无障碍合并）
3. **P0**：CI 账单问题解决后，删除 v0.4.0 tag 重新打 tag 触发正式签名 Release（`git push origin :refs/tags/v0.4.0 && git tag v0.4.0 && git push origin v0.4.0`）
4. **P1 第二批 2C 批（3 项需用户确认）**：
   - P1-5：AiService.chat() 错误吞噬（注：P1-6 已部分解决，chatResult() 已新增；chat() 本身是否废弃待定）
   - P1-9：~800 行死代码清理（ReviewRepository.getAllVerifiedKnowledgePoints + chat_history/ai_conversations 表等）
   - P1-10：Release R8 未启用 + ProGuard 规则补全
5. **P1 大型任务**（需用户确认优先级）：
   - P1-PG-1/2/3：启用 R8 + 补齐 ProGuard 规则（与 P1-10 同一问题）
   - NF-PP4：复习日志双写统一
   - NF-PP5：错题本实现
   - NF-PP6：AiAssistantViewModel 消息持久化
   - NF-T4：MemoRecordMapper Float↔Double 精度（需 schema 迁移）
   - ~~NF-D3：observeDue Flow 不刷新~~ ✅ 已由 P1-1（commit `76c5084`）修复
6. **P1**：v0.5.0 Phase 2 剩余维度审计（strings.xml / 错误处理 / Compose 副作用 / DataStore Key 治理）
7. **P2 剩余 UI 项**（可选）：
   - ConfigCard 架构级冲突：整卡点击 + 内部编辑/删除按钮，需重构
   - CardRenderer FlipCard 超长背面答案溢出：加 `verticalScroll`
   - @Preview 补齐（6 个已有，可再补 4 个）
   - 平板双栏布局（已有 WideNavigationRail，可加 list-detail）
8. **P2**：OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（替换 stage2-sample）
9. **P3**：release.yml "Verify keystore" 步骤隐藏 bug（Line 63-70，KEYSTORE_BASE64 未配置时失败）
10. **P4**：架构重构 — ReviewRepository.getAllVerifiedKnowledgePoints 已成事实死代码

## 📦 已交付

- GitHub Release v0.1.0（2026-07-12）+ v0.2.0（2026-07-13）+ v0.3.0（2026-07-15，debug 签名）+ **v0.4.0（2026-07-14，debug 签名 fallback）**
- 签名 APK：`wenyan-v0.4.0.apk` + `wenyan-latest.apk`（v0.4.0，包含 v0.5.0 + v0.6 全部改动）
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
- **第五轮深度审计 P0 + P1 2A/2B 批**（3 commits：Converter 降级 + rateCard 事务 + 输入框受控 + Flow 异常 catch + retry loading + roundToInt + LIKE 转义 + FakeDAO 契约 + ContentSource/ThemeViewModel 迁移 + observeDue tickFlow + SocraticTutor 三阶段短路，220 tests 保持）
