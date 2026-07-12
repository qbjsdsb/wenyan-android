# 当前状态快照

> **AI 新会话第一份要读的文件。10 秒了解项目当前状态。**
> 最后更新：2026-07-12

## 🔴 当前阻塞

**CI 编译失败** — materialkolor 4.1.1 与 Kotlin 2.0.20 不兼容

| 项 | 值 |
|----|-----|
| 最新 commit | `684e6a2` |
| 最新 CI Run | #29197275819（failure） |
| 错误 | `source must not be null`（实际是 Kotlin 元数据版本不匹配） |
| 根因 | materialkolor 4.1.1 用 Kotlin 2.3.0 编译，项目用 Kotlin 2.0.20 |
| 修复方案 | 升级 Kotlin 到 2.3.0 + KSP 2.3.x，或降级 materialkolor |
| 详情 | [02-VERSION-MATRIX.md](02-VERSION-MATRIX.md) + [03-FAILED-ATTEMPTS.md](03-FAILED-ATTEMPTS.md) |

## 📊 项目进度

### Android App 开发

| 阶段 | 状态 | 详情 |
|------|------|------|
| Phase 2 Android 骨架 | ✅ 完成 | 多模块架构 + Room 数据库 + Hilt DI |
| Phase 3 FSRS 调度 | ✅ 完成 | FSRS-6 自实现 + 三层记忆调度 |
| Phase 4 AI 服务 | ✅ 完成 | OpenAI 兼容协议适配 |
| Phase 5 UI 增强 | ✅ 完成 | 9 个 Screen + M3 基础组件 |
| Release 配置 | ✅ 完成 | 签名 APK + GitHub Release v0.1.0 |
| M3 Expressive 改造 | 🟡 Phase 0 阻塞 | 27 个 commit 已推，CI 失败 |

### M3 Expressive 改造计划（方案 C）

| 阶段 | 状态 | 内容 |
|------|------|------|
| Phase 0 | 🟡 进行中 | 修复 CI（materialkolor/Kotlin 版本兼容） |
| Phase 1 | ⏳ 待开始 | 设计令牌 + 4 个关键组件（药丸导航栏/LargeTopAppBar/分组卡片/层级列表项） |
| Phase 2 | ⏳ 待开始 | 应用到 5 主屏（知识点/真题/卡片/图谱/设置） |
| Phase 3 | ⏳ 待开始 | 打磨 4 次屏（AI助手/API配置/导师信息/知识点详情） |

### OCR 资料数字化

| 项 | 值 |
|----|-----|
| 进度 | 约 60%（125/208 文件） |
| PID | 20432（运行中） |
| 已完成 | P1 核心教材（郑克鲁/聂珍钊等） |
| 剩余 | P4 文学理论 + P5 其他（约 47 个 PDF） |
| 预计完成 | 明日上午 |
| 详情 | [reference/OCR_PIPELINE.md](reference/OCR_PIPELINE.md) |

## 🎯 下一步优先级

1. **P0**：修复 CI 编译错误（升级 Kotlin 或降级 materialkolor）
2. **P1**：M3 改造 Phase 1 — 设计令牌 + 关键组件
3. **P2**：M3 改造 Phase 2 — 5 主屏应用
4. **P3**：M3 改造 Phase 3 — 4 次屏打磨
5. **P4**：OCR 完成后跑知识提取管线 → 生成 seed_data.json

## 📦 已交付

- GitHub Release v0.1.0（2026-07-12）
- 签名 APK：`wenyan-v0.1.0.apk` + `wenyan-latest.apk`（14.7 MB）
- 27 个 M3 改造 commit（已推送，待 CI 通过验证）
