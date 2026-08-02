# 文研 App 完美改进计划（v0.9.24+ 路线图）

> 生成时间：2026-08-02
> 状态：批一 ✅ 完成、批二 ✅ 完成（commit `178658b`）；批三/批四待执行
> 方法：多轮深度审计（全仓库/AI/构建CI/商业）+ 专项技术调查（AI 流式方案/对话管理/数据安全）+ 关键问题人工复验

---

## 一、已完成项（不进计划）

| 批次 | 内容 | 版本 |
|------|------|------|
| 代码清理 + release 防呆 | 死代码删除 / tag↔versionName 校验 / update_release / schema 同步 | v0.9.22 |
| 批 A+B | 底栏 double inset / 主题错误提示 / 版本提升 / FSRS 除零 / 迁移补索引 / 错题并发 / 时间源 / 评分防重入 | v0.9.22 |
| 论述题删年份 | 列表/详情/筛选/知识点关联全部移除年份 | v0.9.23 |
| Snackbar 常驻修复 | 先 clear 再 show + withTimeout 兜底 | v0.9.23 |
| AI 审计修复 | 服务商 URL / 竞态 / 并发防重入 / RAG 降级 / 注入封堵 | v0.9.23 |
| 更新日志机制 | CHANGELOG.md + release.yml 动态读取 | v0.9.23 |

---

## 二、改进计划（4 批，按优先级）

### 🥇 批一：AI 体验（用户感知最强，卖点核心）—— ✅ 已完成（commit `b737f9f`）

> **设计原则**：流式/多轮/停止/token 统计**一次接口升级一起做**（新增 `chatResultStream`，保留 `chatResult` 兼容 SocraticTutor/RecallChecker 的 5 处 `.first()` 调用），避免重复改动。

| # | 项目 | 状态 |
|---|------|------|
| 1 | **AI 真·流式输出** | ✅ OkHttp 原生 SSE 逐行解析（零新依赖），逐 chunk emit Delta；UiState.streamingContent 逐字显示 |
| 2 | **停止生成按钮** | ✅ stopGeneration() + job.invokeOnCompletion { call.cancel() }，已生成内容保留 |
| 3 | **多轮对话上下文** | ✅ getRecentByConversation + 最近 20 条注入 + token 裁剪 |
| 4 | **Token 用量统计** | ✅ AiChatReply.usage → tokensUsed 透传 + 气泡小字 |

**验证**：core:ai + feature:aiassistant 单测（新增流式 collect / history 注入 / usage 透传测试）+ 全量 + assembleDebug + emulator 实测流式体验

---

### 🥈 批二：工程质量与商业化基础 —— ✅ 已完成（commit `178658b`）

| # | 项目 | 状态 |
|---|------|------|
| 5 | **R8/ProGuard 混淆** | ✅ isMinifyEnabled=true，APK -79%，mapping 验证；⚠️ 需 emulator 实测后发布 |
| 6 | **崩溃上报 + 分析埋点** | ⏳ 待做（需 Firebase/Sentry 账号配置） |
| 7 | **对话列表/历史管理** | ⏳ 待做（中-大工作量） |
| 8 | **聊天数据加密** | ⏳ 待做（SQLCipher 风险大，谨慎评估） |
| 9 | **数据库迁移测试** | ✅ MigrationTestHelper 覆盖 8→9、9→10（androidTest） |
| 10 | **Tab 返回闪烁** | ✅ 3 处 stateIn 改 Eagerly |
| 11 | **DAO 查询列补索引** | ✅ 数据库 9→10（question_type/answer_status/content_source） |
| 12 | **多步写事务化** | ✅ ChatRepositoryImpl.appendMessage；StudyProgress 评估后保留 |

**验证**：相关模块单测 + R8 后 emulator 冒烟（重点：Room/Hilt/序列化/网络）+ 全量 + assembleRelease 签名验证

---

### 🥉 批三：性能与工程整洁

| # | 项目 | 方案 | 工作量 |
|---|------|------|--------|
| 13 | **详情页懒加载** | KnowledgePointDetailScreen 5 处 `forEachIndexed` → 嵌套 LazyColumn（heightIn 包裹），错题/关联多时不卡 | 中 |
| 14 | **convention plugin** | 抽取 11 模块重复 android{} 块（~200 行）到 buildSrc/composite build；改依赖版本不漏改 | 中 |
| 15 | **导航类型安全/深链** | type-safe routes 替代字符串拼接（ID 未编码风险） | 中 |
| 16 | **i18n 资源化** | 硬编码字符串 → strings.xml（单中文可后置） | 中 |
| 17 | **依赖健康** | material3 alpha 关注升级、retrofit 2.9.0 评估升级、kotlin-jvm 冗余删除、CI 加依赖漏洞扫描（OSV-Scanner） | 小 |
| 18 | **RAG 检索质量** | 关键词提取增强（复合问句分词）+ 过滤 VERIFIED（复用现有 ocr_status 索引）+ 可选向量检索（阶段 5） | 中 |
| 19 | **AI 成本控制** | 429 重试读 Retry-After 头；单次引导 3 阶段 API 调用加总量熔断 | 小 |

**验证**：各模块单测 + 全量 + assembleDebug + 性能抽样

---

### 📦 批四：仓库卫生与合规（随时可做）

| # | 项目 | 方案 | 工作量 |
|---|------|------|--------|
| 20 | **release-assets 清理** | `git rm --cached` 4 个旧 APK（77MB）→ .gitignore（release.yml CI 每次重建，不入库） | 小 |
| 21 | **AGENTS.md/docs 过期更新** | 修正"当前状态"（仍写 v0.9.18）、删除 OCR 项目遗留约束（Koa/D:\wenyan/conda）、00-STATUS.md/02-VERSION-MATRIX.md/RELEASE_PROCESS.md 同步 | 小-中 |
| 22 | **隐私政策/用户协议** | 明确"用户输入发送第三方 LLM"；数据收集说明 | 小 |
| 23 | **上架资质** | 软著登记 / 国内应用商店备案（商业化前置） | 小 |

---

## 三、推荐执行顺序（投入产出比）

1. **批一（AI 体验）**：用户感知最强，AI 卖点核心，改动集中（1 次接口升级）
2. **批二**：R8 + 崩溃上报 + 对话管理 + 迁移测试（工程质量与商业化基础）
3. **批三**：性能与整洁
4. **批四**：仓库卫生与合规（随时可插队做）

每批完成后：相关模块单测 → 全量 511+ → assembleDebug → 提交推送（CHANGELOG 更新日志）。

---

## 四、风险与注意事项

| 风险 | 应对 |
|------|------|
| 流式改造牵连 SocraticTutor/RecallChecker | 新增 `chatResultStream` 保留旧接口，5 处 `.first()` 调用零改动 |
| R8 混淆引发运行时问题 | 需 emulator 实测；保留 Room/Hilt/serialization/OkHttp 规则；可先灰度 |
| 数据库 9→10 加索引 | 与补 1/3.json + 迁移测试一起做，MigrationTestHelper 覆盖 |
| 对话列表 UI 改动大 | 可拆两步：先数据层+ViewModel（切换能力），再 UI 列表 |
| AGENTS.md 混合行尾 | 用 Python 二进制精确替换（按段落实际行尾），避免大 diff |
