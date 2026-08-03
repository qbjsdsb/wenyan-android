# 当前状态快照

> **AI 新会话第一份要读的文件。10 秒了解项目当前状态。**
> 最后更新：2026-08-04（v0.9.27 已发布，Release #56）

## ✅ 当前状态

**v0.9.27 已发布**（2026-08-04，Release #56）：启动图标 v7.5 精进 + 全面检查 P1-1/2 修复 + 四科内容补齐 25 个知识点。

**v0.9.26 已发布**（2026-08-03，Release #55）：新启动图标 v7.4（Google Play Books 风格）+ 批三（性能与整洁 5 项）。

### v0.9.27 发布内容（tag `baa178a`，versionCode 52 / versionName "0.9.27"）
- **Release**：https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.27
- **receipt**：`docs/release-receipts/v0.9.27-release-receipt.md`

### 启动图标 v7.5 精进（commit `6935b5f`）
- v7.4 基础上精进：右页米色 #F2E9D8 双色页、页脚双色厚度、右页首行缩进/末行短收
- Android 13+ 主题图标改 evenOdd 镂空文字线，纯色单层也清晰

### 全面检查 P1-1/2 修复（commit `5b7267f`）
- aiJob 竞态：finally 条件清空 `if (coroutineContext[Job] == aiJob)`（旧任务不抹新任务引用）
- Retry-After 上限：`coerceAtMost(5000L)` clamp 服务商限流秒数

### 内容补齐（commit `ba3fc68` + `ef3d932`，seed 2.16.0→2.18.0，935→960）
- 真题硬缺口 10 + 杨朔模式（第一批 11 个）/ 教材缺口 9 + 台港澳 4 + 敦煌变文（第二批 14 个）
- 四科 25 个缺口全部补齐，三路交叉验证（教材/真题/结构）

### v0.9.26 发布内容（tag `5d7f3d9`，versionCode 51 / versionName "0.9.26"）
- **Release**：https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.26
- **receipt**：`docs/release-receipts/v0.9.26-release-receipt.md`

### 新图标 v7.4（commit `38b9ddf`）
- Google Play Books 风格「黑底白书」：手工矢量白书 + 文字线，墨黑 #1A1A1A
- 纯 VectorDrawable（84KB → 9.8KB），adaptive icon 三层 + Splash 同步

### 批三（commit `ad86909` / `ace2e64` / `13631da` / `ba0a53f`）
- 详情页懒加载（Column → LazyColumn）/ RAG VERIFIED 过滤 / AI 成本控制（Retry-After + callTimeout + Semaphore3）
- i18n 资源化（5 模块 74 处）/ convention plugin 抽取（11 库模块共用配置）
- RAG 停用词剔除回退（LIKE 匹配语义，多词 OR 留待后续）

### v0.9.25 发布内容（tag `760be63`，versionCode 50 / versionName "0.9.25"）
- **Release**：https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.25
- **receipt**：`docs/release-receipts/v0.9.25-release-receipt.md`

### 新图标（commit `d5b9695`）
- AI 生成「书堆 + 文」插画图标（ImageGen → PIL 处理），5 密度 webp 共 84KB
- adaptive icon 三层 + Splash 同步；旧 v4 矢量备份 `.icon-gen/archive/v4-svg/`

### UI 修复 14 项（commit `769455b`，3 路并行审查）
- P1：AI 停止保留内容 / 流式自动滚动 / 流式转圈重叠 / 状态栏图标色 / 更新安装已下载 APK
- P2：更新页过渡 / retry loading / 错误态禁用筛选 / 长标题截断 / 种子色暗色亮化 / 卡片滚动重置 / 错题本 Snackbar / 日期行省略 / 底栏跨 Tab 重置

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
| 最新 commit | **baa178a**（v0.9.27 版本号提升，2026-08-04） |
| 最新 Release | **v0.9.27**（2026-08-04，正式签名 + R8 + 图标 v7.5 + 内容补齐 25 个）— https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.9.27 |
| versionCode / versionName | **52 / "0.9.27"** |
| 知识点 | **960 个** |
| 真题 | **485 道** |
| 论述题 | **134 道**（v0.9.23 起删年份显示，数据层 year 保留） |
| seed 版本 | **2.18.0** |
| 数据库版本 | **10**（v0.9.24 补 3 个筛选索引） |
| 底部导航 | **5 Tab**（知识点/论述题/卡片/错题本/设置），MD3 规范 + scroll-aware 显隐 |
| 图谱 | **已移除**（v0.9.3） |
| AI 服务商 | DeepSeek/通义/智谱/月之暗面/自定义（v0.9.23 修复 URL 拼接） |
| 更新日志机制 | **CHANGELOG.md** + release.yml 动态读取（v0.9.23 起） |
| 单测 | **518 个 0 失败**（批一+批二后，v0.9.27 验证仍 0 失败） |
| R8 | **已启用并随 v0.9.27 发布**（APK 5.96MB，需 emulator 冒烟实测） |
| 启动图标 | **v7.5 双色页精进**（v0.9.27 起：米色右页+页脚厚度+缩进+主题镂空，纯矢量） |
| 崩溃上报 | **未接入**（需 Firebase/Sentry 配置） |
| 阻塞 | **无** |

## ✅ v0.9.27 发布验证记录（2026-08-04，Release #56）

> 发布核心验证已全部通过（详见 `docs/release-receipts/v0.9.27-release-receipt.md`）。

1. **tag 与版本**：✅ v0.9.27 → baa178a = HEAD；versionCode 52 / versionName "0.9.27"
2. **Release 页面**：✅ 文研App v0.9.27 已发布（2026-08-03T16:49:48Z UTC），正文"更新内容"来自 CHANGELOG v0.9.27（动态日志机制持续生效）
3. **APK 资产**：✅ wenyan-v0.9.27.apk + wenyan-latest.apk 均 200（5,959,265 字节），sha256 完全一致（1843e1a9…，与 GitHub API digest 一致）
4. **APK 版本**：✅ aapt2 校验 versionCode 52 / versionName "0.9.27" / targetSdk 35
5. **APK 签名**：✅ apksigner v2 scheme 通过
6. **单测/构建**：✅ 518 单测 0 失败 + assembleDebug + assembleRelease(R8) 全绿（本地已验）
3. **APK 资产**：✅ wenyan-v0.9.26.apk + wenyan-latest.apk 均 200（5,937,261 字节），sha256 完全一致（8a291432…）
4. **APK 版本**：✅ aapt2 校验 versionCode 51 / versionName "0.9.26" / targetSdk 35
5. **APK 签名**：✅ apksigner v2 scheme 通过
6. **单测/构建**：✅ 518 单测 0 失败 + assembleDebug + assembleRelease(R8) 全绿（本地已验）

> ⚠️ **唯一待人工验证**：emulator 安装 release 混淆 APK 冒烟（App 启动 / 新图标桌面效果 / 详情页滚动 / AI 流式+成本控制 / 主题切换）。

## 📋 剩余待办（按优先级）

### 批四：仓库卫生与合规
- release-assets 4 个旧 APK（77MB）git rm --cached
- AGENTS.md/docs 过期更新（00-STATUS 已更新，AGENTS.md 混入 OCR 项目约束待清）
- 隐私政策/用户协议（商业化前置）
- 上架资质（软著/备案）

### 批三余项（低优先）
- 详情页懒加载已做；convention plugin 已做（仅 app 模块未抽）
- 导航类型安全/深链
- i18n 全量（contentDescription/错误提示/模板串暂留硬编码）
- RAG 多关键词 OR（LIKE 语义改进，需 DAO 改造）
- 依赖健康（material3 alpha/retrofit 升级会破坏，保持锁定）

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
