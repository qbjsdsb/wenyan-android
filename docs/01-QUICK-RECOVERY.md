# 快速恢复协议

> **本文件定义 AI 会话的标准开始/结束流程，确保失忆后无缝衔接。**

## 会话开始（目标：5 分钟内进入工作状态）

### Step 1：读 3 个核心文件（30 秒）

1. [../AGENTS.md](../AGENTS.md) — 项目入口（30 秒）
2. [00-STATUS.md](00-STATUS.md) — 当前状态（10 秒）
3. [SESSION_LOG.md](SESSION_LOG.md) 最后一节 — 上次进度（30 秒）

### Step 2：检查 CI 状态（如有阻塞）

```bash
# 用 GitHub API 查最新 Run 状态
curl -H "Authorization: token <GITHUB_TOKEN>" \
  https://api.github.com/repos/qbjsdsb/wenyan-android/actions/runs?per_page=1
```

- 如 `conclusion: failure` → 读 [02-VERSION-MATRIX.md](02-VERSION-MATRIX.md) + [03-FAILED-ATTEMPTS.md](03-FAILED-ATTEMPTS.md)
- 如 `conclusion: success` → 阻塞已解除，继续下一步

### Step 3：确认工作内容

根据 [00-STATUS.md](00-STATUS.md) 的"下一步优先级"确定本次会话目标。

### Step 4：拉取最新代码

```bash
cd D:\wenyan\wenyan-android
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
cd D:\wenyan\wenyan-android
git add docs/SESSION_LOG.md docs/00-STATUS.md
# 如有其他避坑文档更新也一起 add
git commit -m "docs: update session log and status after session"
git push origin main
```

## 常见场景

### 场景 1：修复 CI 编译错误

1. 读 [03-FAILED-ATTEMPTS.md](03-FAILED-ATTEMPTS.md) 看已尝试过的方案
2. 读 [02-VERSION-MATRIX.md](02-VERSION-MATRIX.md) 看版本兼容性
3. 选择未尝试的方案
4. 修改 `gradle/libs.versions.toml`
5. commit + push
6. 等 CI 运行（约 5-10 分钟）
7. 如成功 → 更新 00-STATUS.md 解除阻塞
8. 如失败 → 记录到 03-FAILED-ATTEMPTS.md，换方案

### 场景 2：M3 改造（UI 开发）

1. 读 [design/m3-expressive-redesign.md](design/m3-expressive-redesign.md) 了解设计规格
2. 读 [plans/m3-expressive-implementation.md](plans/m3-expressive-implementation.md) 了解 Task 列表
3. 按 Phase 顺序执行 Task
4. 每个 Task 完成后 commit + push
5. 等待 CI 验证
6. 全部 Phase 完成后更新 00-STATUS.md

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
- **PowerShell 不支持 heredoc** — commit 消息用单行
- **PowerShell profile.ps1 不含 conda 初始化**
- **OCR 运行时不跑 CPU 密集 Python 任务**
- **Android 开发不影响 OCR**，可并行
