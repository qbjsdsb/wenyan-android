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
- **JDK 17 路径**：`/root/.local/share/mise/installs/java/17.0.2`（由 [mise.toml](../mise.toml) 锁定，沙箱默认 java=25.0.2 会让 AGP 8.6.0 加载失败）
- **Android SDK 路径**：`/opt/android-sdk`（**沙箱镜像不预装**，每次新会话需重新安装，详见 [#016](03-FAILED-ATTEMPTS.md)）
- **Gradle 8.14.4**：通过 mise 安装（命令 `gradle`），仓库也含 `./gradlew` wrapper（2026-07-23 补齐）
- **JAVA_TOOL_OPTIONS**：已写入 [mise.toml](../mise.toml) `[env]` 节，自动生效。含 `-XX:-UseContainerSupport`（JDK 17.0.2 cgroup v2 bug）+ 代理 `127.0.0.1:18080`（Robolectric 测试下 android-all jar 用）
- **环境变量**：[mise.toml](../mise.toml) 已持久化 `ANDROID_HOME` / `ANDROID_SDK_ROOT` / `_.path`，运行 `mise exec -- <cmd>` 自动加载。若直接用 `./gradlew` 不走 mise，需手动 `export ANDROID_HOME=/opt/android-sdk && export ANDROID_SDK_ROOT=/opt/android-sdk && export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH`
- **CI=true 绕过**：`app/build.gradle.kts` 第 71 行 release keystore fail-fast 在配置阶段抛异常，沙箱跑 debug 任务需 `unset CI && export CI=false`
- **4GB cgroup OOM**：默认 `-Xmx2048m -XX:MaxMetaspaceSize=1g` + 多 worker 会 OOM，沙箱用 `-Xmx1536m -XX:MaxMetaspaceSize=768m --max-workers=1 -Dorg.gradle.parallel=false` 覆盖（详见 [03-FAILED-ATTEMPTS.md #015](03-FAILED-ATTEMPTS.md)）
- **沙箱不保留状态**：会话结束即清空，所有改动必须 commit + push 到 GitHub。`/root/.gradle/wrapper/dists/` 缓存也会被清空，需重新填充
- **GitHub token**：由用户提供，不写入仓库（环境变量或临时使用）

### 沙箱首次配置（每次新会话必做，约 3 分钟）

> **触发条件**：`/opt/android-sdk` 不存在 或 `./gradlew --version` SSL 握手失败
> 详细坑见 [03-FAILED-ATTEMPTS.md #016](03-FAILED-ATTEMPTS.md)

```bash
# === Step 1: 安装 Android SDK（约 2 分钟，下载 150MB）===
mkdir -p /opt/android-sdk
cd /tmp
wget -q --timeout=60 --tries=3 -O /tmp/cmdline-tools.zip \
  "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
unzip -q /tmp/cmdline-tools.zip -d /opt/android-sdk/
# 关键：必须嵌套一层 latest/，否则 sdkmanager 找不到
mv /opt/android-sdk/cmdline-tools /opt/android-sdk/cmdline-tools-tmp
mkdir -p /opt/android-sdk/cmdline-tools/latest
mv /opt/android-sdk/cmdline-tools-tmp/* /opt/android-sdk/cmdline-tools/latest/
rmdir /opt/android-sdk/cmdline-tools-tmp

# 接受 license + 安装组件
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
yes | sdkmanager --licenses > /tmp/sdk-licenses.log 2>&1
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# === Step 2: 填充 gradle wrapper 缓存（避开 services.gradle.org 不可达）===
# 用 mise 已装的 gradle-8.14.4 复制到 wrapper 期望位置
WRAPPER_HASH_DIR="/root/.gradle/wrapper/dists/gradle-8.14.4-bin/92wwslzcyst3phie3o264zltu"
mkdir -p "$WRAPPER_HASH_DIR"
# 先跑一次 --version 让 wrapper 创建 .part 文件并暴露 hash 目录名
./gradlew --version 2>&1 | tail -3 || true
# 复制 mise 的 gradle 到 wrapper 缓存
cp -r /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4 "$WRAPPER_HASH_DIR/"
touch "$WRAPPER_HASH_DIR/gradle-8.14.4-bin.zip.ok"
rm -f "$WRAPPER_HASH_DIR/gradle-8.14.4-bin.zip.part"

# === Step 3: 验证 ===
./gradlew --version  # 应显示 Gradle 8.14.4, Launcher JVM 17.0.2
```

### 沙箱构建命令模板（已验证可用，2026-07-26 实测 assembleDebug 12m36s SUCCESS）

```bash
cd /workspace && unset CI && export CI=false

# 编译验证（assembleDebug 实测 12m36s，APK 27MB）
mise exec -- ./gradlew assembleDebug --no-daemon --max-workers=1 \
  -Dorg.gradle.parallel=false -Dorg.gradle.workers.max=1 \
  -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8 -XX:+UseParallelGC -XX:-UseContainerSupport"

# 测试验证
mise exec -- ./gradlew testDebugUnitTest --no-daemon --max-workers=1 \
  -Dorg.gradle.parallel=false -Dorg.gradle.workers.max=1 \
  -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8 -XX:+UseParallelGC -XX:-UseContainerSupport"
```

### assembleDebug 卡死排查（重要）

如果 `./gradlew assembleDebug` 看起来卡住不动，**几乎可以肯定是 cgroup v2 OOM killer 静默杀 daemon**：
- 现象：Gradle launcher 无限等待 daemon socket，无报错无输出
- 验证：`cat /sys/fs/cgroup/memory.events` 看 `oom_kill` 计数是否 > 0
- 根因：未用上面"沙箱构建命令模板"中的 `-Xmx1536m -XX:MaxMetaspaceSize=768m --max-workers=1 -Dorg.gradle.parallel=false` 覆盖参数，走了 `gradle.properties` 默认值（`-Xmx2048m + MaxMetaspaceSize=1g + workers.max=3 + parallel=true`），峰值内存 5-6GB 超 4GB cgroup 限制
- 修复：严格按上面命令模板执行，不要省略任何覆盖参数
