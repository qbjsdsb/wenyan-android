# P0 双修：SeedDataLoader 接通 + release.yml CI 修复 实施计划

> **状态：✅ 已完成（2026-07-13）**
>
> - Phase 1 commit `ff19231`：release.yml Line 46 + Line 81 修复
> - Phase 2 commit `07c3a6d`：WenyanApplication 注入 SeedDataLoader + onCreate 异步调用
> - 验证：`assembleDebug` SUCCESSFUL（412 tasks）+ `testDebugUnitTest` 174 tests 0 failures
> - 详见 [SESSION_LOG.md](../SESSION_LOG.md) Session 2026-07-13（第三条）

> **For agentic workers:** 本计划基于 writing-plans skill 编写。Step 使用 `- [ ]` 复选框跟踪进度。每个 Task 应独立可执行、可验证、可回滚。

**Goal:** 修复 release.yml 的 2 个 CI bug（避免下次发布失败）+ 接通 SeedDataLoader 调用点（让 App 从空壳 UI 变成可用工具，可用 stage2-sample 数据立即验证数据流）。

**Architecture:** 分两个独立的 Phase——Phase 1 修 release.yml（2 行改动，零代码风险）→ Phase 2 接通 SeedDataLoader（在 WenyanApplication.onCreate 用 Hilt 注入 + 协程异步调用）。两个 Phase 互不依赖，可独立提交和回滚。

**Tech Stack:**
- Kotlin 2.3.10 / Hilt 2.57.1（`@HiltAndroidApp` + `@Inject lateinit var`）
- Coroutines（`CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler).launch`）
- Gradle 8.14.4 / AGP 8.6.0
- GitHub Actions（`gradle/actions/setup-gradle@v3`）

---

## 背景调查：当前状态与差距

### 调查项 1：release.yml 的 2 个 bug

**Bug 1（Line 46）**：`gradle-version: '8.7'`
- 违反 AGENTS.md 第 4 节硬约束："CI Gradle 版本与本地对齐 — 用 8.14.4"
- 详见 [docs/03-FAILED-ATTEMPTS.md](file:///workspace/docs/03-FAILED-ATTEMPTS.md) — 旧版 8.7 在解析 KSP 2.3.x 时有 bug
- 后果：下次 `git tag vX.Y.Z` 触发 release 时，Gradle 8.7 + KSP 2.3.2 不兼容，构建失败

**Bug 2（Line 81）**：`gradle test`
- 违反 AGENTS.md 第 4 节硬约束："CI 跑 testDebugUnitTest 而非 test"
- 详见 [03-FAILED-ATTEMPTS.md #012](file:///workspace/docs/03-FAILED-ATTEMPTS.md) — `debugImplementation` 依赖只在 debug 变体可用，release 测试会因缺 ComponentActivity 声明失败
- 后果：即使 Gradle 版本修对了，`gradle test` 会跑 release 测试变体，因缺 ComponentActivity 声明失败

**参考**：android.yml 已正确用 Gradle 8.14.4 + `testDebugUnitTest`（已核实 `.github/workflows/android.yml:25,37`），release.yml 应对齐。`generate-keystore.yml` 是 `workflow_dispatch` 手动触发，不用 Gradle，无需改动。

### 调查项 2：SeedDataLoader 从未被调用

**事实**：
- `/workspace/app/src/main/java/com/wenyan/app/WenyanApplication.kt` 是空壳（只有 `@HiltAndroidApp class WenyanApplication : Application()`）
- Grep 验证：`SeedDataLoader` 和 `ensureSeedDataLoaded` 在整个代码库中只有自身定义（2 处匹配），**0 个调用点**
- `SeedDataLoader` 实现完整（6 步导入：subjects → chapters → knowledge_points → memo_records → exam_questions → writing_materials + graph skeleton）
- `seed_data.json` 已存在于 `/workspace/app/src/main/assets/seed_data.json`（stage2-sample：4 科目 + 12 知识点 + 4 真题 + 4 写作素材）
- app 模块已依赖 `core:data`（`app/build.gradle.kts` line 83），Hilt 可注入 SeedDataLoader

**后果**：
- App 启动时数据库为空，所有 feature 模块的 ViewModel 观察空数据库
- GraphScreen / CardsScreen / QuizScreen / KnowledgeScreen 全部显示 EmptyState
- App 实际是空壳 UI，无法验证真实数据流

### Hilt 注入 Application 的标准模式

Hilt 支持用 `@Inject lateinit var` 注入到 `@HiltAndroidApp` 标注的 Application：

```kotlin
@HiltAndroidApp
class WenyanApplication : Application() {
    @Inject lateinit var seedDataLoader: SeedDataLoader

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e("WenyanApplication", "Seed data load failed", e)
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seedDataLoader.ensureSeedDataLoaded()
        }
    }
}
```

**为什么用 `CoroutineScope(Dispatchers.IO).launch` 而不是 `GlobalScope`：**
- `GlobalScope` 会引发 lint 警告，且生命周期不受控
- Application 进程级单例，用独立 CoroutineScope 即可（Application 不会被销毁）
- `Dispatchers.IO` 适合磁盘/数据库操作

**为什么必须加 CoroutineExceptionHandler（深度审查修订）：**

原计划误以为 `SupervisorJob` 能防止 App 崩溃，这是事实性错误。经 Kotlin 官方文档和多方技术资料核实：
- `SupervisorJob` 只阻断异常向父 Job 传播（不让父协程被取消），**但不阻止异常本身被抛出**
- `launch` 创建的根协程，未捕获异常会经 `Thread.uncaughtExceptionHandler` 处理
- Android 默认的 `UncaughtExceptionHandler` 是 `RuntimeInit$KillApplicationHandler`，**会导致 App 崩溃**
- 因此 seed_data.json 解析失败（`SerializationException`）或 assets 读取失败（`IOException`）会让 App 直接崩溃

加 `CoroutineExceptionHandler` 后：异常被捕获并记录到 Logcat，App 不崩溃，用户看到 EmptyState（可接受降级）。

**为什么不阻塞 onCreate：**
- 阻塞 onCreate 会导致 App 启动卡顿
- 异步加载时各 Screen 的 ViewModel 会先收到空数据（显示 loading 或 EmptyState），数据加载完后 Flow 自动刷新

### 已知限制（深度审查发现，本次计划接受）

1. **强杀重启可能丢失复习数据**：`MemoRecordEntity` 外键 `onDelete = CASCADE` + DAO 用 `OnConflictStrategy.REPLACE`。如果 App 在首次导入中途被强杀（DataStore 未标记已初始化），下次启动重新导入时，`REPLACE` 会先 DELETE 旧行（触发 CASCADE 删除 memo_records），再 INSERT 新行。用户已复习的进度会被初始值覆盖。
   - **发生概率**：低（仅在首次启动 + 中途被杀时）
   - **本次接受理由**：MVP 阶段，用户尚未有真实复习数据可丢失；后续可用 `INSERT OR IGNORE` 或 `@Transaction` 优化
   - **后续优化**：将 `importToDatabase` 改为先检查是否存在再决定插入，或用 `@Insert(onConflict = ABORT)` + try-catch

2. **importToDatabase 无 @Transaction**：7 步导入之间无外层事务包裹，如果中途 OOM 会被杀，可能留下部分数据。但所有 insertAll 用 `REPLACE`，下次启动会覆盖，风险可控。

3. **mapNotNull 静默跳过**：seed_data.json 中 subject 字段不匹配 subjects.name 的知识点会被静默跳过，且 `markInitialized()` 仍执行。当前 stage2-sample 数据匹配，无影响。

4. **release.yml "Verify keystore" 步骤隐藏 bug（Line 63-70，本次不动）**：该步骤无条件执行 `keytool -list`，但前一步 "Decode keystore" 在 `KEYSTORE_BASE64` 未配置时 `exit 0` 跳过解码（不生成 .jks 文件）。结果 Verify 步骤会对不存在的文件执行 keytool 而失败。**触发条件**：仓库未配置 Secrets 时打 tag 触发 release。**本次不动理由**：当前仓库已配置 Secrets，不会触发；修复需重构 keystore 处理逻辑，超出 P0 范围。记录到 `docs/03-FAILED-ATTEMPTS.md` 供后续修复。

---

## 文件结构

### 修改的文件

| 文件 | 改动 | 风险 |
|------|------|------|
| `.github/workflows/release.yml` | Line 46: `'8.7'` → `'8.14.4'`；Line 81: `gradle test` → `gradle testDebugUnitTest` | 极低（2 行改动） |
| `app/src/main/java/com/wenyan/app/WenyanApplication.kt` | 注入 SeedDataLoader + onCreate 异步调用 | 低（标准 Hilt 模式） |

### 不修改的文件

- `SeedDataLoader.kt` — 实现完整，无需改动
- `seed_data.json` — stage2-sample 已存在
- `app/build.gradle.kts` — 已依赖 `core:data`（line 83）+ `kotlinx.coroutines.android`（line 125），无需改
- 其他 Screen / ViewModel — 数据加载后 Flow 自动刷新

---

## Phase 1：修复 release.yml CI bug

### Task 1: 修复 Gradle 版本 + test 命令

**Files:**
- Modify: `.github/workflows/release.yml:46,81`

**改动说明：**

两处独立改动，一次提交。

**改动 1（Line 46）：**
```yaml
# 改前
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.7'

# 改后
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.14.4'
```

**改动 2（Line 81）：**
```yaml
# 改前
      - name: Run unit tests
        run: gradle test --no-daemon --stacktrace

# 改后
      - name: Run unit tests
        run: gradle testDebugUnitTest --no-daemon --stacktrace
```

- [ ] **Step 1: 修改 release.yml Line 46 — gradle-version '8.7' → '8.14.4'**
- [ ] **Step 2: 修改 release.yml Line 81 — gradle test → gradle testDebugUnitTest**
- [ ] **Step 3: 验证 yaml 语法** `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))"`
- [ ] **Step 4: Commit Phase 1**
  ```bash
  git add .github/workflows/release.yml
  git commit -m "fix(ci): release.yml 对齐 AGENTS.md 硬约束

  - gradle-version 8.7 → 8.14.4（旧版 8.7 在解析 KSP 2.3.x 时有 bug，详见 03-FAILED-ATTEMPTS.md）
  - gradle test → testDebugUnitTest（debugImplementation 依赖只在 debug 变体可用，
    release 测试会因缺 ComponentActivity 声明失败，详见 03-FAILED-ATTEMPTS.md #012）

  为什么：下次 git tag vX.Y.Z 触发 release 时会因这两个 bug 失败。
  android.yml 已正确用 8.14.4 + testDebugUnitTest，release.yml 应对齐。"
  ```

---

## Phase 2：接通 SeedDataLoader 调用点

### Task 2: WenyanApplication 注入 SeedDataLoader + onCreate 异步调用

**Files:**
- Modify: `app/src/main/java/com/wenyan/app/WenyanApplication.kt`

**改动说明：**

用 Hilt 的 `@Inject lateinit var` 注入 SeedDataLoader，在 onCreate 中用 `CoroutineScope(Dispatchers.IO).launch` 异步调用 `ensureSeedDataLoaded()`。

**改动：**

```kotlin
// 改前
package com.wenyan.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 文研App Application 入口。
 *
 * 标注 @HiltAndroidApp 触发 Hilt 代码生成，创建依赖注入容器。
 * 整个应用的依赖图以此为根。
 */
@HiltAndroidApp
class WenyanApplication : Application()

// 改后
package com.wenyan.app

import android.app.Application
import android.util.Log
import com.wenyan.app.core.data.seed.SeedDataLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 文研App Application 入口。
 *
 * 标注 @HiltAndroidApp 触发 Hilt 代码生成，创建依赖注入容器。
 * 整个应用的依赖图以此为根。
 *
 * 启动时异步加载种子数据（首次启动从 assets/seed_data.json 导入到 Room）。
 * 加载在 IO 调度器执行，不阻塞 onCreate；各 ViewModel 通过 Flow 观察数据库，
 * 数据导入完成后自动刷新 UI。
 *
 * 异常处理：用 CoroutineExceptionHandler 捕获种子加载异常，避免 App 崩溃。
 * SupervisorJob 只阻断异常向父 Job 传播，但不阻止异常本身被抛出；
 * launch 根协程未捕获异常会经 Thread.uncaughtExceptionHandler 处理，
 * Android 默认会导致 App 崩溃，因此必须显式加异常处理器。
 */
@HiltAndroidApp
class WenyanApplication : Application() {

    @Inject
    lateinit var seedDataLoader: SeedDataLoader

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e("WenyanApplication", "Seed data load failed", e)
    }

    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + exceptionHandler,
    )

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seedDataLoader.ensureSeedDataLoaded()
        }
    }
}
```

**关键设计决策：**

1. **`SupervisorJob()` 而非默认 Job**：阻断异常向父 Job 传播（不让父协程被取消）。
2. **`Dispatchers.IO`**：SeedDataLoader 涉及 assets 读取 + Room 数据库写入，IO 密集型。
3. **`CoroutineExceptionHandler`（深度审查新增）**：捕获种子加载异常，记录到 Logcat，避免 App 崩溃。SupervisorJob 不阻止异常本身被抛出，未捕获异常会经 `Thread.uncaughtExceptionHandler` 导致 Android App 崩溃。加异常处理器后，用户看到 EmptyState（可接受降级），比崩溃好。
4. **不阻塞 onCreate**：异步加载，App 启动流畅。各 ViewModel 用 `stateIn(WhileSubscribed(5000))` 订阅，数据加载完后自动刷新。

- [ ] **Step 1: 修改 WenyanApplication.kt — 注入 SeedDataLoader + onCreate 异步调用**
- [ ] **Step 2: 编译验证** `:app:compileDebugKotlin`
- [ ] **Step 3: 全量编译验证** `assembleDebug`
- [ ] **Step 4: 运行全量测试确保无回归** `testDebugUnitTest`
- [ ] **Step 5: Commit Phase 2**
  ```bash
  git add app/src/main/java/com/wenyan/app/WenyanApplication.kt
  git commit -m "feat: 接通 SeedDataLoader — App 启动时异步导入种子数据

  - WenyanApplication 注入 SeedDataLoader（@Inject lateinit var）
  - onCreate 用 CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler).launch
    异步调用 ensureSeedDataLoaded()，不阻塞启动
  - CoroutineExceptionHandler 捕获种子加载异常（如 JSON 解析失败、assets 读取失败），
    记录到 Logcat，避免 App 崩溃（降级为 EmptyState）
  - 注意：SupervisorJob 只阻断异常向父 Job 传播，不阻止异常本身被抛出；
    真正防崩溃的是 CoroutineExceptionHandler，不是 SupervisorJob

  为什么：SeedDataLoader 实现完整（6 步导入）但从未被调用，App 启动时数据库为空，
  所有 Screen 显示 EmptyState。接通后可用 stage2-sample seed_data.json 立即验证数据流。
  从'能编译'变成'能用'的关键一步。"
  ```

---

## Phase 3：验证 + 推送 + CI

### Task 3: 推送 + CI 验证

- [ ] **Step 1: git push origin main**
- [ ] **Step 2: 等待 CI 运行完成（约 20 分钟）**
- [ ] **Step 3: 验证 CI 全绿** `gh run view --repo qbjsdsb/wenyan-android`

### Task 4: 更新文档

**Files:**
- Modify: `docs/00-STATUS.md`（更新当前状态 + 下一步优先级）
- Modify: `docs/SESSION_LOG.md`（新增 Session 2026-07-13 第三条记录）
- Modify: `AGENTS.md`（更新第 7 节当前状态 + 第 9 节下一步优先级）
- Modify: `docs/plans/p0-seed-loader-ci-fix.md`（顶部标记完成）

- [ ] **Step 1: 更新 00-STATUS.md** — 新增"P0 双修"章节 + 更新下一步优先级
- [ ] **Step 2: 更新 SESSION_LOG.md** — 新增 Session 2026-07-13（第三条）记录
- [ ] **Step 3: 更新 AGENTS.md** — 第 7 节状态 + 第 9 节优先级（P0 完成后，P1 提升为新 P0）
- [ ] **Step 4: 更新 plan 文件** — 顶部标记为已完成
- [ ] **Step 5: Commit 文档**
- [ ] **Step 6: Push 文档 commit**

---

## 自检清单

### 计划完整性
- [x] 每个 Task 有明确的文件路径和行号
- [x] 每个 Task 有具体的代码改动（非占位符）
- [x] 每个 Phase 末尾有验证关卡
- [x] Commit message 说明"为什么改"

### 深度调查发现的关键约束（已纳入计划）
- [x] release.yml 2 个 bug 的具体行号（Line 46 + Line 81）已核实
- [x] SeedDataLoader 实现完整（6 步导入）已读取确认
- [x] seed_data.json 已存在（stage2-sample）已 Glob 确认
- [x] app 模块已依赖 core:data（build.gradle.kts line 83）+ kotlinx.coroutines.android（line 125）已读取确认
- [x] Hilt 注入 Application 的标准模式（@Inject lateinit var）已确认
- [x] SeedDataLoader 的 9 个构造依赖全部可注入（7 DAO 由 DatabaseModule @Provides，GraphRepository 由 DataModule @Binds 到 GraphRepositoryImpl @Inject constructor，Context 由 @ApplicationContext 提供）已核实
- [x] android.yml 已用 8.14.4 + testDebugUnitTest（line 25,37），generate-keystore.yml 不用 Gradle，均无需改动

### 风险评估
- **Phase 1 风险：极低** — 2 行 yaml 改动，不涉及代码逻辑，yaml 语法验证后即安全
- **Phase 2 风险：低** — 标准 Hilt 模式（@Inject lateinit var 在 Application 中是官方推荐用法），CoroutineExceptionHandler 捕获异常防崩溃，不阻塞 onCreate
- **回滚方案** — 每个 Phase 独立 commit，可 `git revert` 单个 Phase

### 预期结果
- 修改 2 个文件（release.yml + WenyanApplication.kt）
- CI 全绿（android.yml 14/14 步骤）
- App 启动后自动导入 stage2-sample 数据（4 科目 + 12 知识点 + 4 真题 + 4 写作素材）
- GraphScreen / CardsScreen / QuizScreen / KnowledgeScreen 从 EmptyState 变为有数据

### 后续验证（需真机/模拟器，本沙箱无法做）
- 启动 App 后查看 Logcat 无 SeedDataLoader 异常
- 进入各 Tab 确认有数据显示
- 重启 App 确认不重复导入（DataStore 的 KEY_SEED_INITIALIZED 标志）
