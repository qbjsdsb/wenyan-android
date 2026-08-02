# 文研 App 完美改进计划（v0.9.24+ 路线图）

> 生成时间：2026-08-02
> 状态：研究调查完成，待用户确认执行
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

### 🥇 批一：AI 体验（用户感知最强，卖点核心）

> **设计原则**：流式/多轮/停止/token 统计**一次接口升级一起做**（新增 `chatResultStream`，保留 `chatResult` 兼容 SocraticTutor/RecallChecker 的 5 处 `.first()` 调用），避免重复改动。

| # | 项目 | 技术方案（已调查） | 工作量 |
|---|------|-------------------|--------|
| 1 | **AI 真·流式输出** | **OkHttp 原生流式（零新依赖）**：不走 Retrofit，`okHttpClient.newCall().execute()` + `source.readUtf8Line()` 逐行解析 SSE（`data: {json}\n\n`，`data: [DONE]` 结束）。新增 `ChatStreamChunk/ChatStreamDelta` DTO（`delta.content`）。UiState 加 `streamingContent: String?` + ViewModel 持 StringBuilder 定时 flush（每 100ms），避免每 chunk 复制整 List | 小-中 |
| 2 | **停止生成按钮** | `suspendCancellableCoroutine` + `invokeOnCancellation { call.cancel() }`；现有 `RetryInterceptor.isCancellation` 已保证取消不被重试。UI：isLoading 时输入栏变"停止"按钮 | 小 |
| 3 | **多轮对话上下文** | DAO 加 `getRecentByConversation(id, limit)` → Repository 加方法 → `chatResult(query, history = [])` 默认参数向后兼容 → ViewModel 取最近 20 条注入。裁剪：按条数（20）优先 + 按 token 粗估（中文 1 token/字，上限 4000） | 小-中 |
| 4 | **Token 用量统计** | `ChatUsage` 已解析（`LlmDtos.kt` L67-75）但从未读取；`appendMessage.tokensUsed` 全传 null。流式接口返回 `AiChatReply(content, usage)` → ViewModel 透传 → `AiMessage.tokensUsed` → 气泡小字展示 | 小-中 |

**验证**：core:ai + feature:aiassistant 单测（新增流式 collect / history 注入 / usage 透传测试）+ 全量 + assembleDebug + emulator 实测流式体验

---

### 🥈 批二：工程质量与商业化基础

| # | 项目 | 方案 | 工作量 |
|---|------|------|--------|
| 5 | **R8/ProGuard 混淆** | `isMinifyEnabled = true` + 完善 consumer-rules（保留 Room/Hilt/serialization 规则）；**需 emulator 实测无崩溃后启用**（AGENTS.md 已知 P1 待办） | 中 |
| 6 | **崩溃上报 + 分析埋点** | Firebase Crashlytics + Analytics（或 Sentry 轻量替代），线上崩溃/卡顿可视化 | 中 |
| 7 | **对话列表/历史管理** | `observeConversations`（零消费者）接通：DAO 加 `rename` + Repository + ViewModel `conversations` 状态/切换/重命名/删除 + TopBar"历史对话"入口 + BottomSheet 或子路由列表 + 首条消息自动生成标题（取 query 前 15 字替代写死"AI 对话"） | 中-大 |
| 8 | **聊天数据加密** | SQLCipher 加密（需评估 Room 集成成本）或至少补隐私说明；与 API key 的 Keystore 加密对齐 | 中 |
| 9 | **数据库迁移测试** | MigrationTestHelper + 补 1.json/3.json 历史 schema；覆盖 8→9 补索引场景（上次靠人工发现） | 中 |
| 10 | **Tab 返回闪烁** | 3 处 `stateIn(WhileSubscribed)`（ApiConfig/EssayList/StudyProgress）→ 改 collect 模式或 Eagerly | 小 |
| 11 | **DAO 查询列补索引** | `exam_questions.question_type/answer_status`、`knowledge_points.content_source` 等（数据库版本 9→10 需迁移，与 8 一并做） | 小 |
| 12 | **多步写事务化** | `ChatRepositoryImpl.appendMessage`（插入+更新计数）、`StudyProgressRepository.recordStudySession`（读-算-写）包 `@Transaction` | 小 |

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
