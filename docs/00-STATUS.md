# 当前状态快照

> **AI 新会话第一份要读的文件。10 秒了解项目当前状态。**
> 最后更新：2026-08-03（v0.9.24 已发布，Release #53）

## ✅ 当前状态

**v0.9.24 已发布**（2026-08-03，Release #53）：批一（AI 体验 4 项）+ 批二（工程质量 5 项）。

**v0.9.23 已发布**（2026-08-02，Release #52）：论述题删年份 + Snackbar 常驻修复 + AI 审计修复 + 更新日志机制。

### v0.9.24 发布内容（tag `9183ecc`，versionCode 49 / versionName "0.9.24"）
- **Release**：https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.24
- **receipt**：`docs/release-receipts/v0.9.24-release-receipt.md`

### 批一：AI 体验（commit `b737f9f`）
- **AI 真·流式输出**：OkHttp 原生 SSE 逐行解析（零新依赖），逐字显示
- **停止生成**：AI 回复中可停止，已生成内容保留
- **多轮对话上下文**：最近 20 条历史注入 LLM
- **Token 用量统计**：AI 回复下方显示 token 数
- 新增 `chatResultStream` 接口（保留旧 `chatResult` 兼容苏格拉底引导/回忆检测）

### 批二：工程质量（commit `178658b`）
- **R8 混淆**：release APK 26.7MB→5.6MB（-79%），mapping 验证混淆生效
- **数据库迁移测试**：MigrationTestHelper 覆盖 8→9、9→10（androidTest）
- **Tab 返回闪烁修复**：3 处 stateIn 改 Eagerly
- **DAO 补索引**：exam_questions.question_type/answer_status、knowledge_points.content_source（数据库 9→10）
- **appendMessage 事务化**：消息插入+计数合并 withTransaction

## 📊 版本矩阵

| 项 | 值 |
|----|-----|
| 最新 commit | **9183ecc**（v0.9.24 版本号提升，2026-08-03） |
| 最新 Release | **v0.9.24**（2026-08-03，正式签名 + R8 混淆）— https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.24 |
| versionCode / versionName | **49 / "0.9.24"** |
| 知识点 | **935 个** |
| 真题 | **485 道** |
| 论述题 | **134 道**（v0.9.23 起删年份显示，数据层 year 保留） |
| seed 版本 | **2.16.0** |
| 数据库版本 | **10**（v0.9.24 补 3 个筛选索引） |
| 底部导航 | **5 Tab**（知识点/论述题/卡片/错题本/设置），MD3 规范 + scroll-aware 显隐 |
| 图谱 | **已移除**（v0.9.3） |
| AI 服务商 | DeepSeek/通义/智谱/月之暗面/自定义（v0.9.23 修复 URL 拼接） |
| 更新日志机制 | **CHANGELOG.md** + release.yml 动态读取（v0.9.23 起） |
| 单测 | **518 个 0 失败**（批一+批二后） |
| R8 | **已启用并随 v0.9.24 发布**（APK 5.9MB，需 emulator 冒烟实测） |
| 崩溃上报 | **未接入**（需 Firebase/Sentry 配置） |
| 阻塞 | **无** |

## ✅ v0.9.24 发布验证记录（2026-08-03，Release #53）

> 发布核心验证已全部通过（详见 `docs/release-receipts/v0.9.24-release-receipt.md`）。

1. **tag 与版本**：✅ v0.9.24 → 9183ecc = HEAD；versionCode 49 / versionName "0.9.24"
2. **Release 页面**：✅ 文研App v0.9.24 已发布（2026-08-02T17:33:33Z UTC），正文"更新内容"来自 CHANGELOG v0.9.24（动态日志机制持续生效）
3. **APK 资产**：✅ wenyan-v0.9.24.apk + wenyan-latest.apk 均 200（5,909,874 字节），sha256 完全一致
4. **APK 版本**：✅ aapt2 校验 versionCode 49 / versionName "0.9.24" / targetSdk 35
5. **APK 签名**：✅ apksigner v2 scheme 通过，CN=Wenyan App（RSA 2048）
6. **单测/构建**：✅ 518 单测 0 失败 + assembleDebug + assembleRelease(R8) 全绿（本地已验）

> ⚠️ **唯一待人工验证**：emulator 安装 release 混淆 APK 冒烟（App 启动 / 列表加载 / AI 流式 / 主题切换）+ 数据库 9→10 覆盖安装升级。

## 📋 剩余待办（按优先级）

### 批三：性能与整洁
- 详情页懒加载（KnowledgePointDetailScreen 5 处 forEach → LazyColumn）
- convention plugin 抽取（11 模块 ~200 行重复）
- 导航类型安全/深链
- i18n 资源化
- 依赖健康（material3 alpha 关注、retrofit 升级、CI 漏洞扫描）
- RAG 检索质量（关键词提取增强 + 过滤 VERIFIED）
- AI 成本控制（429 Retry-After、并发熔断）

### 批四：仓库卫生与合规
- release-assets 4 个旧 APK（77MB）git rm --cached
- AGENTS.md/docs 过期更新（00-STATUS 已更新，AGENTS.md 混入 OCR 项目约束待清）
- 隐私政策/用户协议（商业化前置）
- 上架资质（软著/备案）

### 已评估未做
- 崩溃上报（需 Firebase/Sentry 账号配置）
- 对话列表 UI（中-大工作量）
- SQLCipher 聊天加密（风险大，谨慎评估）
- 内容版权自查/原创化（商业化生死关）

## 🔑 交接要点
- **发布流程**：CHANGELOG.md 写日志 → 提升 versionCode/versionName → 打 tag（AGENTS.md 已更新流程）
- **AGENTS.md 是混合行尾**：编辑需用 Python 二进制精确替换（按段落实际行尾），勿用 Edit 工具（会整文件转 LF 导致大 diff）
- **app/build.gradle.kts / SESSION_LOG.md / 00-STATUS.md 是纯 LF**：可直接 Edit/Write
- **数据库 8→9、9→10 是补索引迁移**（幂等），10.json 已生成
- **AI 流式接口**：`chatResultStream(query, history)` 返回 Flow<Result<AiStreamEvent>>（Delta/Complete），保留 `chatResult` 兼容
- **R8 已启用**：consumer-rules.pro（各模块）+ proguard-rules.pro（app）规则齐全，release 构建已验证
