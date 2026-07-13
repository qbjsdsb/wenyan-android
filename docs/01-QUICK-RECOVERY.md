# 快速恢复协议

> **本文件定义 AI 会话的标准开始/结束流程，确保失忆后无缝衔接。**

## 会话开始（目标：5 分钟内进入工作状态）

### Step 1：读 3 个核心文件（30 秒）

1. [../AGENTS.md](../AGENTS.md) — 项目入口（30 秒）
2. [00-STATUS.md](00-STATUS.md) — 当前状态（10 秒）
3. [SESSION_LOG.md](SESSION_LOG.md) 最后一节 — 上次进度（30 秒）

### Step 2：检查 CI 状态（如有阻塞）

```bash
# 用 GitHub API 查最新 Run 状态（替换 <GITHUB_TOKEN>）
curl -s -H "Authorization: token <GITHUB_TOKEN>" \
  "https://api.github.com/repos/qbjsdsb/wenyan-android/actions/runs?per_page=3" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); [print(r['id'],'|',r['status'],'|',r['conclusion'],'|',r['head_branch'],'|',r['head_sha'][:7]) for r in d.get('workflow_runs',[])]"
```

- 如 `conclusion: failure` → 读 [02-VERSION-MATRIX.md](02-VERSION-MATRIX.md) + [03-FAILED-ATTEMPTS.md](03-FAILED-ATTEMPTS.md)
- 如 `conclusion: success` → 阻塞已解除，继续下一步

### Step 3：确认工作内容

根据 [00-STATUS.md](00-STATUS.md) 的"下一步优先级"或 [../AGENTS.md](../AGENTS.md) 第 9 节确定本次会话目标。

### Step 4：拉取最新代码

```bash
# 本地开发（Windows D 盘）
cd D:\wenyan\wenyan-android
git pull origin main

# 或在 Trae 沙箱
cd /workspace
git pull origin main
```

## 会话结束（必须完成，3 分钟）

### Step 1：更新 SESSION_LOG.md（追加一节）

```markdown
## YYYY-MM-DD HH:MM 会话

- **完成**：
  - 具体做了什么
- **进行中**：
  - 哪些任务还没完成
- **阻塞**：
  - 当前遇到的问题
- **下次继续**：
  - 下次会话应该做什么
- **关键发现**：
  - 学到的新信息、踩到的坑
- **commit**：
  - 本次会话的 commit hash 列表
```

### Step 2：更新 00-STATUS.md（如状态变化）

如阻塞解除/新增、阶段进度变化、CI 状态变化，更新对应字段。

### Step 3：补充避坑文档（如发现新坑）

- 版本兼容问题 → [02-VERSION-MATRIX.md](02-VERSION-MATRIX.md)
- 失败方案 → [03-FAILED-ATTEMPTS.md](03-FAILED-ATTEMPTS.md)

### Step 4：commit + push

```bash
# 本地开发（Windows D 盘）
cd D:\wenyan\wenyan-android
git add docs/SESSION_LOG.md docs/00-STATUS.md
# 如有其他避坑文档更新也一起 add
git commit -m "docs: update session log and status after session"
git push origin main

# 或在 Trae 沙箱
cd /workspace
git add docs/SESSION_LOG.md docs/00-STATUS.md AGENTS.md
git commit -m "docs: update session log and status after session"
git push origin main
```

## 常见场景

### 场景 1：修复 CI 编译错误

1. 读 [03-FAILED-ATTEMPTS.md](03-FAILED-ATTEMPTS.md) 看已尝试过的方案（特别注意 #010-#012 CI 相关坑）
2. 读 [02-VERSION-MATRIX.md](02-VERSION-MATRIX.md) 看版本兼容性
3. 下载 CI 失败日志：
   ```bash
   curl -sL -H "Authorization: token <GITHUB_TOKEN>" \
     "https://api.github.com/repos/qbjsdsb/wenyan-android/actions/runs/<RUN_ID>/logs" \
     -o /tmp/ci-logs.zip
   unzip -p /tmp/ci-logs.zip "build/<N>_<step name>.txt" | grep -E "(FAILURE|FAILED|error|Exception|What went wrong)" | head -30
   ```
4. 选择未尝试的方案
5. 修改 `gradle/libs.versions.toml` 或对应配置
6. commit + push（建议开 feature 分支 + PR 触发 CI，避免污染 main）
7. 等 CI 运行（约 15 分钟完整构建）
8. 如成功 → 更新 00-STATUS.md 解除阻塞 + 合并 PR
9. 如失败 → 记录到 03-FAILED-ATTEMPTS.md，换方案

**CI 常见失败原因**：
- plugin marker artifact 解析失败 → 检查 `settings.gradle.kts` 的 pluginManagement 仓库顺序（[#010](03-FAILED-ATTEMPTS.md)）
- Metaspace OOM → 检查 `gradle.properties` 的 MaxMetaspaceSize ≥ 1g（[#011](03-FAILED-ATTEMPTS.md)）
- testReleaseUnitTest 失败 → CI 跑 `testDebugUnitTest` 而非 `test`（[#012](03-FAILED-ATTEMPTS.md)）

### 场景 2：KSU 风格 UI 升级后续（UI 开发）

1. 读 [plans/ksu-ui-upgrade.md](plans/ksu-ui-upgrade.md) 了解已完成的 Phase 0-3 与剩余工作
2. 读 [design/m3-expressive-redesign.md](design/m3-expressive-redesign.md) 了解设计规格
3. KSU 组件位置：`core/designsystem/src/main/java/com/wenyan/app/core/designsystem/component/`
   - `WenyanLargeTopAppBar.kt` — LargeFlexibleTopAppBar 封装
   - `WenyanNavigationBar.kt` — 药丸风格导航栏
   - `GroupedCard.kt` — 分组卡片（已应用到 SettingsScreen + KnowledgePointDetailScreen）
   - `TonalCard.kt` / `TonalCardLow.kt` — 色调卡片组件
4. 9 个 Screen 已迁移到 WenyanLargeTopAppBar（6 个滚动屏 + 3 个固定内容屏）
5. UI 改造已完成：SettingsScreen + KnowledgePointDetailScreen 均用 GroupedCard，4 个死组件已删除

### 场景 3：OCR 管线（本地运行）

1. 读 [reference/OCR_PIPELINE.md](reference/OCR_PIPELINE.md)
2. 确认 conda 环境 `ocr` 可用：`conda activate ocr`
3. 确认 OCR 进度：查 `tools/manifest.json`
4. 按顺序执行管线步骤
5. 完成后将 `seed_data.json` 复制到 `app/src/main/assets/`
6. commit + push

## 注意事项

- **不修改 route 文件**（中间件重构时）
- **所有中间件使用 async/await**
- **使用 Koa 2.x**
- **PowerShell 不支持 heredoc** — commit 消息用单行（沙箱 bash 支持 heredoc）
- **PowerShell profile.ps1 不含 conda 初始化**
- **OCR 运行时不跑 CPU 密集 Python 任务**
- **Android 开发不影响 OCR**，可并行

### Trae 沙箱环境（Linux）

- **沙箱路径**：`/workspace`（不是 `D:\wenyan`）
- **JDK 17 路径**：`/root/.local/share/mise/installs/java/17.0.2`
- **Android SDK 路径**：`/opt/android-sdk`
- **Gradle 8.14.4**：通过 mise 安装，命令 `gradle`（无 gradlew wrapper）
- **JAVA_TOOL_OPTIONS**：必须设置 `-XX:-UseContainerSupport`（JDK 17.0.2 cgroup v2 bug）
- **沙箱不保留状态**：会话结束即清空，所有改动必须 commit + push 到 GitHub
- **GitHub token**：由用户提供，不写入仓库（环境变量或临时使用）
