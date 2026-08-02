# 文研 App AI 功能深度审计报告

> 生成时间：2026-08-02
> 审计范围：core/ai（11 文件）+ feature/aiassistant（6 文件）+ 论述题 SocraticTutor/EssayDetailViewModel + core/data 的 ChatRepository/ApiConfigRepository + core/database 的 ChatMessage/AiGradingRecord
> 方法：2 路并行深度审查 + 关键问题人工复验（URL 拼接、非流式、竞态、RAG 异常、注入链）

---

## 一、总体评价

**做得好的地方**（已核实）：
- **密钥安全扎实**：API key 用 AndroidKeyStore AES/GCM 加密入库（`ApiKeyCryptoImpl.kt`），无硬编码，Debug 日志 redact Authorization 头
- **重试有界且带指数退避**：429/5xx/IOException 重试最多 3 次，500ms→1s→2s + 抖动（`RetryInterceptor`，有完整单测）
- **输入长度有界**：sendMessage 限 2000 字、答案限 5000 字、RAG 查询限 500 字、maxTokens 默认 2000——**无"天价账单"风险**
- **RAG 体量可控**：最多 5 条 × 200 字摘录注入 prompt，不会把整库发给 LLM
- **论述题三阶段引导质量高**：validateUserAnswer → RAG 降级 → 阶段失败短路均已实现
- **离线降级**：无 API 配置时明确提示"请配置 API 服务商"

**未发现 P0 安全漏洞**（无硬编码密钥、无明文流量、无日志泄漏、无无限重试）

---

## 二、问题清单（按优先级，全部经人工复验）

### P0 — 数据丢失/状态撕裂

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| P0-1 | **发送中清空/新建对话竞态**：sendMessage 无 job 句柄，clearMessages/startNewConversation 不取消 in-flight 协程，直接把 `currentConversationId = null`。in-flight 协程 L132 `currentConversationId!!` 读到 null → NPE（被 catch 吞成"请求失败：null"），**用户消息既不落库也无 AI 回复** | `AiAssistantViewModel.kt:98-180, 380-417` | 用户快速"发送→清空"或"发送→新建"时消息丢失 |
| P0-2 | **init 恢复竞态**：`restoreConversationIfNeeded` 异步恢复与用户首条消息竞争。恢复完成时用旧会话覆盖 `currentConversationId`，**用户刚发的消息从 UI 消失**（残留在孤儿会话） | `AiAssistantViewModel.kt:468-483` | 进程重启后高频场景 |

### P1 — 功能缺陷

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| P1-1 | **预设服务商 URL 拼接错误（3/4 不可用）**：接口固定 `@POST("v1/chat/completions")`，而 Qwen 预设 `.../compatible-mode/v1`、Zhipu 预设 `.../api/paas/v4`、Moonshot 预设 `.../v1` 已含版本段 → 拼出 `.../v1/v1/chat/completions` 等错误 URL，**开箱即用必 404**（仅 DeepSeek 恰好正确） | `LlmApiService.kt:25` + `ApiConfigViewModel.kt:344-347` | 通义/智谱/月之暗面预设配置全部不可用 |
| P1-2 | **名为流式实为非流式**：两处 `stream = false`，返回 Flow 但只 emit 一次完整响应；`consumer-rules.pro` 引用的 `StreamChunk` DTO 实际不存在。UI 等待期间只有转圈，长回答 10-60s 无反馈 | `AiServiceImpl.kt:75,139` | 体验与"流式"设计不符 |
| P1-3 | **并发竞态**：sendMessage 无 in-flight 保护 + 溢出菜单学习工具在 isLoading 时可点 → 多个 AI 协程并发写 `_uiState`，先完成的把 isLoading 置 false，后完成的还在后台输出，消息顺序乱插 | `AiAssistantViewModel.kt:189-333` + `AiAssistantScreen.kt:213-240` | 响应错乱、重复计费 |
| P1-4 | **无停止生成**：发送后全屏无取消按钮；结合非流式 + 60s readTimeout + 最多 3 次重试，单条消息最坏阻塞数分钟 | `AiAssistantScreen.kt:563-571` | 用户只能干等 |
| P1-5 | **历史对话无限增长**：observeMessages 无 LIMIT，UI 全量渲染；长对话（上百轮）内存与渲染退化 | `AiAssistantViewModel.kt:473` + `ChatMessageDao.kt:24-25` | 性能退化 |
| P1-6 | **对话管理不完整**：observeConversations 全项目无消费者；无对话列表/历史切换/重命名/消息删除；旧会话永久堆积 | `ChatRepository.kt:27` | 功能承诺未达成 |
| P1-7 | **AI 批改整表死代码**：AiGradingRecordEntity + DAO + ai_grading_records 表全部存在但**零引用**；表无 status 字段，无法追踪"进行中/已完成" | `AiGradingRecordEntity.kt` + `AiGradingRecordDao.kt` | 批改闭环缺失 |

### P2 — 缺陷/风险

| # | 问题 | 位置 |
|---|------|------|
| P2-1 | **RAG 检索失败阻断主流程**：`ragEngine.search()` 无 runCatching，DAO 异常时整个 AI 调用失败（即使无 RAG 也能回答） | `RagEngine.kt:57` + `AiAssistantViewModel.kt:144` |
| P2-2 | **RAG 关键词提取简陋**：仅去前后缀，复合问句（"论述 A 与 B 的关系"）常提取失败 → 误报"不在资料库范围"或匹配无关内容 | `RagEngine.kt:83-116` |
| P2-3 | **RAG 未过滤 VERIFIED**：searchByKeyword 不过滤 ocr_status，可能命中未校对/OCR 质量差的知识点作为"可溯源引用" | `KnowledgePointDao.kt:88-96` |
| P2-4 | **无多轮上下文**：AI 每次只发 `system + 当前 query`，历史消息存 DB 但从不回传 LLM，连续追问每轮像新对话 | `AiServiceImpl.kt:70-72,134-136` |
| P2-5 | **无 token 用量统计**：ChatUsage 已解析但从未使用，appendMessage 的 tokensUsed 恒传 null，无法监控成本 | `LlmDtos.kt:68-75` + `AiAssistantViewModel.kt:140` |
| P2-6 | **提示词注入放大链**：苏格拉底阶段 2/3 把上一阶段 AI 输出直接拼入 prompt（无边界标记、无注入警告）→ 阶段 1 被注入后放大到后续阶段 | `PromptTemplates.kt:107-111,156-163` |
| P2-7 | **Thread.sleep 不可取消**：RetryInterceptor 在 OkHttp 线程池 sleep（最长 2s），协程取消无法打断，且占用线程 | `AiModule.kt:125,138` |
| P2-8 | **429 重试忽略 Retry-After 头** | `AiModule.kt:122-128` |
| P2-9 | **聊天明文存储**：ChatMessageEntity.content 无加密（DB 无 SQLCipher），root/备份可读；与 API key 加密不对称 | `ChatMessageEntity.kt:50-51` |
| P2-10 | **编辑表单明文回填 key**：showEditForm 把解密 key 写入 formState，内存持有完整明文；observeAllConfigs 也会把所有解密 key 放进 UI 状态 | `ApiConfigViewModel.kt:125-137` + `ApiConfigRepository.kt:72-78` |
| P2-11 | **appendMessage 非事务**：messageDao.insert 与 conversationDao.touch 两个独立事务，touch 失败导致 message_count 偏差 | `ChatRepositoryImpl.kt:83-99` |
| P2-12 | **普通问答失败无重试**：失败只弹 Snackbar，用户消息已落库留下"悬挂"消息，无重试/删除入口（论述题有 retryAiGuide 但问答没有） | `AiAssistantViewModel.kt:157-162` |
| P2-13 | **弱网无监控**：无 ConnectivityManager，网络中断仅差异化提示，恢复后 isAvailable 不自动刷新 | 全局 |
| P2-14 | **rateSelf 错题回写误导**：recordWrongAnswer 成功但 rateWrongAnswer 异常时，UI 显示"未成功"但错题实际已写入 | `EssayDetailViewModel.kt:297-326` |

### P3 — 优化建议

| # | 问题 | 位置 |
|---|------|------|
| P3-1 | 每次调用新建 Retrofit 实例（应缓存 per-config） | `AiServiceImpl.kt:192-197` |
| P3-2 | validateBaseUrl 允许 http://（network_security_config 会拦截） | `ApiConfigViewModel.kt:289` |
| P3-3 | chat() 死代码（错误字符串当回复 emit，无调用方） | `AiServiceImpl.kt:58-111` |
| P3-4 | 通用 catch 泄漏原始异常 message 到 UI | `AiServiceImpl.kt:171-172` |
| P3-5 | 学习工具"死记硬背检测"要求手填知识点 ID，对真实用户不可用 | `AiAssistantScreen.kt:486-502` |
| P3-6 | 对话标题硬编码"AI 对话" | `AiAssistantViewModel.kt:454` |

---

## 三、最值得优先修复的 Top 5

1. **P1-1 URL 拼接 bug**：改接口路径为 `@POST("chat/completions")` + 预设 baseUrl 统一为版本前缀（DeepSeek 改 `https://api.deepseek.com/v1`）——**否则通义/智谱/月之暗面预设配置全部不可用**
2. **P0-1/P0-2 竞态**：sendMessage 保存 Job 引用，清空/新建前 cancel 在途任务；`currentConversationId!!` 改安全空判断——数据丢失根源
3. **P1-3 并发控制**：sendMessage/学习工具加 in-flight 串行化（参考 EssayDetailViewModel.aiGuideJob 模式）
4. **P2-1 RAG 降级**：ragEngine.search 包 runCatching，失败降级为无上下文直接调用 LLM
5. **P2-6 注入链封堵**：阶段 2/3 的 previousAnalysis/previousSuggestion 加边界标记 + 注入警告

## 四、可后续考虑（功能增强）

- **真流式 SSE**：`stream=true` + 解析 SSE 分块，UI 逐字渲染 + 停止生成按钮
- **多轮上下文**：把最近 N 轮历史回传 LLM（需裁剪策略）
- **对话列表 UI**：接通 observeConversations，支持历史切换/重命名/删除
- **AI 批改闭环**：给 ai_grading_records 加 status 列 + Repository，接入论述题批改
- **token 用量统计**：接通 ChatUsage，记录 tokensUsed，提供成本面板
- **SQLCipher 加密**：聊天内容加密（工作量大，需评估必要性）
