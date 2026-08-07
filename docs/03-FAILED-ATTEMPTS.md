# 失败方案档案

> **遇到编译失败/运行错误时必读。** 记录已尝试但失败的方案，避免重复踩坑。
> **新会话遇到错误时，第一步查本文件。**

## #016 内容补充合并后的 OCR 辅助字段误读

- **日期**：2026-08-07（袁世硕第二批内容审计）
- **现象**：写入后校验脚本直接从 `seed_data.json` 读取 `ocr_file`，得到 `KeyError`。
- **根因**：`ocr_file`、`ocr_physical_pages`、`anchor_terms` 是合并脚本的审计辅助字段，故意不写入 App 的 seed schema；App 只保存经过核验的教材来源字符串。把候选审计结构误当成运行时数据结构会产生假失败。
- **修复**：写入后按候选文件保存的 OCR 证据，对照当前 seed 的标题和新 ID 复核；确认 10 条新卡的 ID 连续、来源字段正确、所有 OCR 锚点均可复现。
- **教训**：来源证据分为“候选/审计层”和“App 运行层”两种结构。验证脚本必须明确读取层级，不能要求 seed 保留不会被导入的内部字段。
- **相关文件**：`tools/content_supplement/merge_yuan_shishuo_v2_19.py`、`tools/content_supplement/yuan_shishuo_cards_second_v2_20.json`、`app/src/main/assets/seed_data.json`

## #001 materialkolor 4.1.1 + Kotlin 2.0.20 不兼容

- **日期**：2026-07-12
- **CI Run**：#29196837395, #29197275819
- **现象**：
  ```
  e: file:///.../ContentSourceBadge.kt:31:1
     java.lang.IllegalArgumentException: source must not be null
  ```
- **实际错误**（被 Fir 编译器内部异常掩盖）：
  ```
  Module was compiled with an incompatible version of Kotlin.
  The binary version of its metadata is 2.3.0, expected version is 2.0.0.
  Found in: material-color-utilities-api.jar, material-kolor-api.jar
  ```
- **根因**：materialkolor 4.1.1 及其依赖 material-color-utilities 用 Kotlin 2.3.0 编译，项目使用 Kotlin 2.0.20，Kotlin Fir 编译器无法读取 2.3.0 的 `.kotlin_module` 元数据
- **已尝试修复**：
  - ❌ 重写 `ContentSourceBadge.kt` when 表达式（`else -> return` → `else -> null` + 外部 null 检查）— commit `684e6a2`，无效
  - ❌ 升级 composeBom 到 2025.12.00 — commit `a6a97af`，解决了 MaterialExpressiveTheme API 缺失，但不解决此问题
  - ❌ 升级 AGP 到 8.6.0 — commit `77d34e7`，解决了 compileSdk 35，但不解决此问题
- **未尝试方案**：
  - ⏳ 升级 Kotlin 到 2.3.0 + KSP 2.3.x
  - ⏳ 降级 materialkolor 到 4.0.x
- **教训**：看到 `source must not be null` 不要先怀疑自己的代码，先检查依赖的 Kotlin 元数据版本
- **相关文件**：
  - `gradle/libs.versions.toml`（版本配置）
  - `core/designsystem/.../WenyanTheme.kt`（materialkolor 调用处）

---

**✅ 已解决（2026-07-12）**：通过升级 Kotlin 2.0.20 → 2.3.10 + KSP 2.3.2
+ Hilt 2.57.1 + Room 2.7.0 + material3 1.5.0-alpha18 解决。
方案 B（降级 materialkolor 4.0.x）不可行——4.0.x 也用 Kotlin 2.2.20 编译，
元数据版本 2.2 与 Kotlin 2.0.20 同样不兼容。
详见 docs/02-VERSION-MATRIX.md 的"已验证可行组合"小节。

> **注意**：计划中写的 material3 版本是 1.5.0-alpha23，实际使用 1.5.0-alpha18。
> 原因：alpha19+ 要求 AGP 9.1.0 + compileSdk 37，与当前 AGP 8.6.0 不兼容。
> alpha18 中 `LargeFlexibleTopAppBar` 仍为 `@ExperimentalMaterial3ExpressiveApi`，
> `WenyanLargeTopAppBar` 封装内需显式 `@OptIn`。

## #002 PowerShell 不支持 heredoc 语法

- **日期**：2026-07-12
- **现象**：`The '<' operator is reserved for future use`
- **根因**：PowerShell 不支持 `$(cat <<'EOF' ... EOF)` 多行语法
- **修复**：改用单行 commit 消息，或用 `git commit -F file.txt` 方式
- **教训**：PowerShell 下不使用 heredoc

## #003 PKCS12 keystore 密码不一致导致签名失败

- **日期**：2026-07-12
- **现象**：Gradle Android 签名工具失败
- **根因**：PKCS12 格式（Java 17+ 默认）不支持不同的 storepass 和 keypass
- **修复**：
  1. 统一 `generate-keystore.yml` 中的密码（storepass = keypass）
  2. 更新 GitHub Secrets：`KEY_PASSWORD` = `KEYSTORE_PASSWORD`
  3. 重新生成 keystore
- **教训**：PKCS12 keystore 必须使用相同密码
- **相关文件**：`.github/workflows/generate-keystore.yml`, `.github/workflows/release.yml`

## #004 AGP 8.5.x 不支持 compileSdk 35

- **日期**：2026-07-12
- **现象**：编译报错 AGP 版本过低
- **根因**：AGP 8.5.x 最高支持 compileSdk 34
- **修复**：升级 AGP 到 8.6.0 — commit `77d34e7`
- **教训**：compileSdk 35 需要 AGP 8.6.0+

## #005 Compose BOM 2024.06.00 缺少 MaterialExpressiveTheme

- **日期**：2026-07-12
- **现象**：编译报 `Unresolved reference: MaterialExpressiveTheme`
- **根因**：Compose BOM 2024.06.00 只含 Material3 1.2.x，没有 M3 Expressive API
- **修复**：升级 composeBom 到 2025.12.00 — commit `a6a97af`
- **教训**：MaterialExpressiveTheme 需要 Compose BOM 2025.12.00+（Material3 1.4.x）

## #006 旧 orphan tag 导致 Release 失败

- **日期**：2026-07-12
- **现象**：Release workflow 失败，找不到 commit
- **根因**：旧 tag `v0.1.0` 指向已删除的 commit（orphan tag）
- **修复**：
  1. 删除旧 tag：`git tag -d v0.1.0 && git push origin :refs/tags/v0.1.0`
  2. 创建新 tag 指向最新 main commit：`git tag v0.1.0 && git push origin v0.1.0`
- **教训**：Release 前确保 tag 指向存在的 commit

## #007 FSRS-Kotlin 依赖导致编译失败

- **日期**：2026-07-12（Phase 1）
- **现象**：编译失败，FSRS-Kotlin 库不可用
- **根因**：FSRS-Kotlin 库在 JitPack 上不可用或版本不兼容
- **修复**：移除 FSRS-Kotlin 依赖，自行实现 FSRS-6 算法
- **教训**：不依赖不可靠的第三方库，核心算法自行实现
- **相关文件**：`core/fsrs/`（自实现）

## #008 Version Catalog 引用错误

- **日期**：2026-07-12（Phase 1）
- **现象**：编译报找不到依赖
- **根因**：libs.versions.toml 中的引用格式错误
- **修复**：修正 Version Catalog 引用
- **教训**：仔细检查 toml 文件格式

## #009 nestedScroll import 路径错误

- **日期**：2026-07-12（KSU UI 升级 Phase 2）
- **现象**：`Unresolved reference 'nestedScroll'` + `Unresolved reference 'input'`
- **根因**：误写成 `import androidx.compose.input.nestedscroll.nestedScroll`，
  正确路径是 `import androidx.compose.ui.input.nestedscroll.nestedScroll`
  （少了 `.ui`）
- **修复**：6 个 Screen 文件的 import 全部改为 `androidx.compose.ui.input.nestedscroll.nestedScroll`
- **教训**：`nestedScroll` 修饰符在 `androidx.compose.ui.input.nestedscroll` 包中，
  不是 `androidx.compose.input.nestedscroll`。写 import 时注意 `ui` 层级
- **相关文件**：6 个 Screen（KnowledgeScreen / QuizScreen / AiAssistantScreen /
  ApiConfigScreen / SettingsScreen / KnowledgePointDetailScreen）

---

## #010 CI plugin 解析失败（Aliyun 镜像从美/欧不可达）

- **日期**：2026-07-12
- **CI Run**：#29208020552, #29208718911
- **现象**：
  ```
  Plugin [id: 'com.google.devtools.ksp', version: '2.3.2', apply: false] was not found
  Plugin [id: 'org.jetbrains.kotlin.plugin.compose', version: '2.3.10', apply: false] was not found
  ```
- **根因**：`settings.gradle.kts` 的 `pluginManagement.repositories` 把 Aliyun 镜像放
  在最前面。Aliyun 镜像从 GitHub Actions runner（美/欧数据中心）访问时可能不可达
  或返回错误响应，导致 plugin marker artifact 解析失败。本地（中国）访问正常。
- **排查**：
  - 验证 Maven Central + Aliyun 均有 POM 文件（HTTP 200 OK）
  - 验证 maven-metadata.xml 包含 2.3.2 / 2.3.10 版本
  - 本地 Gradle 8.14.4 构建成功，CI Gradle 8.7 / 8.14.4 均失败
  - setup-gradle@v3 cache restoration 失败（400 错误），但 Gradle 用空 home 运行
- **修复**：commit `22b1a7e` — `pluginManagement.repositories` 重排：
  gradlePluginPortal/mavenCentral/google 移到前面，Aliyun 作 fallback。
  `dependencyResolutionManagement` 保持 Aliyun 优先（依赖体积大，加速明显）。
- **教训**：
  - 镜像仓库的可达性受地理位置影响，CI runner 与开发环境位置不同时需注意
  - plugin 解析失败影响大（整个构建无法启动），故 plugin 仓库应用全局仓库优先
  - 依赖下载体积大，镜像加速明显，可保持 Aliyun 优先
- **相关文件**：`settings.gradle.kts`

---

## #011 CI Release 构建 Metaspace OOM

- **日期**：2026-07-12
- **CI Run**：#29209388461
- **现象**：
  ```
  e: java.lang.OutOfMemoryError: Metaspace
  > Task :feature:aiassistant:compileReleaseKotlin FAILED
  Caused by: OOMErrorException: Not enough memory to run compilation.
  ```
- **根因**：`gradle.properties` 中 `MaxMetaspaceSize=512m` 太小。Release 构建
  包含 R8 + Kotlin + Compose 编译，需加载大量类。Kotlin 编译器 in-process 模式下
  共享 Gradle daemon 的 metaspace，所有模块编译累积压力，512m 不足。
- **修复**：commit `dcba036` — `MaxMetaspaceSize` 512m → 1g。
  本地验证 `:feature:aiassistant:compileReleaseKotlin` 在 1g metaspace 下通过。
- **教训**：
  - Release 构建（含 R8）比 Debug 构建需更多 metaspace
  - in-process 编译模式下所有模块共享 metaspace，需预留足够空间
  - 512m 适合小型项目，多模块 + Compose + R8 需 1g+
- **相关文件**：`gradle.properties`

---

## #012 testReleaseUnitTest 缺 ComponentActivity 声明

- **日期**：2026-07-12
- **CI Run**：#29210251616
- **现象**：
  ```
  WenyanLargeTopAppBarTest > backButton_isNotDisplayed_whenOnBackIsNull FAILED
      java.lang.RuntimeException at RoboMonitoringInstrumentation.java:102
  ```
  4 个测试全挂。
- **根因**：`debugImplementation(libs.androidx.compose.ui.test.manifest)` 提供的
  ComponentActivity 声明只在 debug 变体可用。CI 跑 `gradle test` 会触发
  testReleaseUnitTest，release 变体没有 manifest，Robolectric 找不到 Activity
  声明导致 RuntimeException。
- **修复**：commit `9e1723d` — CI workflow `gradle test` → `gradle testDebugUnitTest`。
  Release 测试通常跳过（开发标准实践）。
- **教训**：
  - `debugImplementation` 依赖只在 debug 变体可用
  - Compose UI 测试需 ComponentActivity 声明（通过 compose-ui-test-manifest）
  - CI 应跑 `testDebugUnitTest` 而非 `test`（避免 release 变体测试失败）
- **相关文件**：`.github/workflows/android.yml`、`core/designsystem/build.gradle.kts`

---

## #013 library 模块 BuildConfig 不含 VERSION_NAME

- **日期**：2026-07-14
- **现象**：
  ```
  e: SettingsScreen.kt:242:52 Unresolved reference 'VERSION_NAME'.
  ```
- **根因**：`BuildConfig.VERSION_NAME` 是 `com.android.application` 插件（app 模块）的 defaultConfig 属性。`com.android.library` 插件（library 模块）即使启用 `buildFeatures { buildConfig = true }`，生成的 BuildConfig 类**不含** VERSION_NAME / versionCode 字段（library 模块没有这些概念）。在 library 模块中引用 `BuildConfig.VERSION_NAME` 会报 Unresolved reference。
- **已尝试修复**：
  - ❌ 仅在 library 模块 `buildFeatures { buildConfig = true }` — 启用后 BuildConfig 类存在但无 VERSION_NAME 字段，编译仍失败
  - ✅ 在 library 模块 `defaultConfig { buildConfigField("String", "VERSION_NAME", "\"0.3.0\"") }` — 显式注入，编译通过
- **教训**：library 模块需要版本号显示时，必须用 `buildConfigField` 显式注入，不能直接引用 `BuildConfig.VERSION_NAME`。注意需与 app 模块的 versionName 手动同步。
- **相关文件**：`feature/settings/build.gradle.kts`、`feature/settings/src/main/java/com/wenyan/app/feature/settings/SettingsScreen.kt`

---

## #014 GraphSkeleton FK 约束失败导致种子导入事务回滚（知识点全部丢失）

- **日期**：2026-07-16
- **现象**：v0.7.0 / v0.7.1 安装后知识点列表为空（`isEmpty=true`），显示"暂无知识点，等待种子数据加载"。App 正常启动无崩溃，logcat 无明显错误（异常被 CoroutineExceptionHandler 吞掉）。
- **根因**：`GraphSkeleton.kt` 硬编码 `SUBJECT_ID = "subject-modern-contemporary-literature"`，与 `seed_data.json` 中 modern 科目的实际 id `"subj_02"` 不匹配。`GraphNodeEntity` 有 FK 到 `subjects` 表（`subject_id → subjects.id`），`importGraphSkeleton()` 在 `importToDatabase` 的 `withTransaction` 内调用时，`insertNode` 触发 FK 约束失败（SQLite FOREIGN KEY constraint failed），整个事务回滚——已插入的 subjects/chapters/knowledge_points(909条)/memo_records/exam_questions/writing_materials 全部丢失。异常被 `WenyanApplication` 的 `CoroutineExceptionHandler` 吞掉（仅 `Log.e`），App 正常启动但数据库为空。`markInitialized()` 在事务外（事务抛异常后不会执行），下次启动 `isInitialized()` 仍返回 false，重新尝试导入——无限失败循环。
- **已尝试修复**（v0.7.0 / v0.7.1 未解决）：
  - ❌ v0.7.0：接入 `study_text` 字段 + 升级 seed version — 未触及根因
  - ❌ v0.7.1：精简 JSON（删除 cards/graph_nodes/graph_edges）+ withTimeout 30s→120s — 推测超时是根因，实际不是
- **最终修复**（v0.7.2，双保险）：
  - ✅ `GraphSkeleton.SUBJECT_ID` 改为 `"subj_02"`（与 seed_data.json 一致）
  - ✅ `importGraphSkeleton()` 移出主 `withTransaction`，独立 `database.withTransaction { }` + try-catch，即使图谱导入失败也不影响知识点（主事务已提交 + markInitialized 已执行）
  - ✅ seed version 2.1.0 → 2.2.0 触发重新导入
- **教训**：
  1. **预置常量必须与动态数据源对齐**——硬编码的 `SUBJECT_ID` 必须与 `seed_data.json` 中的实际 id 一致，否则 FK 约束失败
  2. **附加功能不应与核心功能共享事务**——图谱骨架是附加功能，知识点导入是核心功能，不应放在同一个 `withTransaction` 内，否则附加功能失败会拖垮核心功能
  3. **异常被吞掉时需要看 logcat**——`CoroutineExceptionHandler` 吞掉异常只 `Log.e`，App 正常启动但数据为空，容易误判为"数据加载慢"或"超时"
- **相关文件**：`core/data/src/main/java/com/wenyan/app/core/data/seed/GraphSkeleton.kt`、`core/data/src/main/java/com/wenyan/app/core/data/seed/SeedDataLoader.kt`、`app/src/main/assets/seed_data.json`

---

## #015 沙箱构建环境踩坑合集（gradlew 缺失 / CI fail-fast / OOM / 测试类型错误）

- **日期**：2026-07-23
- **现象**：在沙箱（4GB cgroup、JDK 17.0.2、Android SDK 35）执行 `./gradlew assembleDebug` 与 `testDebugUnitTest` 验证 v0.7.2 修复，连续遇到 4 个阻塞问题。
- **根因 + 修复**：

  1. **`gradlew` 与 `gradle-wrapper.jar` 从未入仓库**
     - 现象：`zsh: no such file or directory: ./gradlew`
     - 根因：仓库只提交了 `gradle/wrapper/gradle-wrapper.properties`，缺少 wrapper 启动三件套中的另两件
     - 修复：用 `gradle wrapper --gradle-version 8.14.4 --distribution-type bin` 重新生成，并补提交到 git

  2. **`CI=true` 在配置阶段触发 release keystore fail-fast**
     - 现象：跑 `assembleDebug` 抛 `GradleException: Release 签名未配置`
     - 根因：`app/build.gradle.kts` 第 71 行在 buildTypes 配置块内（配置阶段执行）就 `throw GradleException`，而不是在 release task 执行阶段。即使只跑 debug 任务也会触发
     - 修复（沙箱）：`unset CI && export CI=false` 绕过。CI 环境本身是预期行为，沙箱需要显式覆盖
     - **后续优化（P2 非阻塞）**：应改为在 `assembleRelease` task 配置时检查，或用 `gradle.startParameter.taskNames` 判断是否包含 release 任务

  3. **4GB cgroup OOM 导致 daemon 被 kill**
     - 现象：`Gradle build daemon disappeared unexpectedly`，daemon 日志显示 `429496729 physical memory requested, 21213184 free`
     - 根因：`gradle.properties` 配置 `-Xmx2048m -XX:MaxMetaspaceSize=1g` + `org.gradle.workers.max=3` + `org.gradle.parallel=true`，KSP workers 各自起 JVM，总内存超过 4GB cgroup 限制
     - 修复（沙箱）：命令行覆盖 `-Xmx1536m -XX:MaxMetaspaceSize=768m --max-workers=1 -Dorg.gradle.parallel=false`
     - **注意**：项目 `gradle.properties` 不修改，CI runner 内存更充裕（8GB+），保留原有配置

  4. **`CardsViewModelTest.kt` 类型错误**
     - 现象：`e: CardsViewModelTest.kt:37:51 Unresolved reference 'FakeStudyProgressRepository'`
     - 根因：第 37 行 `private lateinit var studyProgressRepository: FakeStudyProgressRepository` 把工厂函数 `FakeStudyProgressRepository()` 当作类型使用。Kotlin 不允许函数名作为类型
     - 修复：类型改为 `StudyProgressRepository`（工厂函数返回类型），调用 `FakeStudyProgressRepository()` 创建实例保持不变

- **教训**：
  1. **wrapper 三件套必须入仓库**——`gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar` 缺一不可，CI runner 没有 gradle 时只能靠 wrapper 启动
  2. **fail-fast 校验应在 task 执行阶段**——配置阶段抛异常会影响所有任务，即使是 debug 任务
  3. **沙箱内存配置应保守**——cgroup 限制下用 1536m heap + 768m metaspace + 单 worker 是稳定配置
  4. **工厂函数不能用作类型**——Kotlin 中 `fun FakeXxx() = Xxx()` 定义的 `FakeXxx` 是函数，不是类型；变量类型应用返回类型 `Xxx`
- **相关文件**：`gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar`、`app/build.gradle.kts`、`gradle.properties`、`feature/cards/src/test/java/com/wenyan/app/feature/cards/CardsViewModelTest.kt`

---

## #017 丁帆补充脚本默认 OCR 根目录少了一层 output

- **日期**：2026-08-07
- **现象**：丁帆 v2.23 合并脚本第一次 dry-run 报告所有 OCR 文件不存在，不能复现页码和锚点。
- **根因**：压缩包实际路径为 `tools_unpacked/output/file_131.json`、`file_132.json`，脚本默认路径误写成 `tools_unpacked/file_*.json`。
- **已尝试修复**：
  - ❌ 第一次 dry-run — 仅定位到路径层，不写入任何种子数据。
  - ✅ 将默认 OCR 根目录修正为 `tools_unpacked/output`，重新执行后 20/20 条页面与锚点检查通过。
- **教训**：OCR 证据校验必须先验证真实目录结构；路径层失败不能被解释为教材缺页。
- **相关文件**：`tools/content_supplement/merge_dingfan_v2_23.py`、`tools_unpacked/output/file_131.json`、`tools_unpacked/output/file_132.json`

---

## #018 直接 Kotlin 框架编译未显式传入标准库

- **日期**：2026-08-07
- **现象**：首次用 Gradle 分发包中的 Kotlin 编译器直接编译四科框架时，出现大量 `cannot access built-in declaration` 和 `找不到 kotlin-stdlib` 报错。
- **根因**：编译器启动时没有自动定位其 Kotlin home；标准库虽然在 Gradle 分发包中，但未作为编译 classpath 传入。
- **已尝试修复**：
  - ❌ 首次直接调用 `K2JVMCompiler` — 环境 classpath 不完整，未能判定源码结果。
  - ✅ 使用 `-no-stdlib -no-reflect` 并显式加入 `kotlin-stdlib-1.9.22.jar`，四科框架编译通过；临时 Kotlin 入口实测 `frameworks=4 modern=201 total=1013 errors=0`。
- **教训**：直接编译验证必须把编译器运行 classpath 与源码编译 classpath 分开明确配置。
- **相关文件**：四科 `*KnowledgeFramework.kt`、`KnowledgeFrameworkValidator.kt`

---

## #019 离线 Gradle 被 Kotlin DSL 插件缓存阻塞

- **日期**：2026-08-07
- **现象**：使用压缩包附带的 Gradle 8.7、离线模式和单 worker 执行 `:core:data:test`，配置阶段失败。
- **根因**：本地缓存没有 `org.gradle.kotlin.kotlin-dsl:4.3.0` 插件；项目 wrapper 声明的 Gradle 8.14.4 也无法从 `services.gradle.org` 下载。
- **已尝试修复**：
  - ❌ Gradle 8.7 `--offline` — 在 `build-logic/build.gradle.kts` 第 2 行停止，尚未进入源码编译和单元测试。
  - ✅ 使用直接 Kotlin 编译器验证四科框架 — 编译和实际 `validate` 通过，但不能替代完整 Android 测试。
- **教训**：构建环境阻塞必须与数据/源码失败分开报告；离线 Gradle 配置失败不等于测试失败。
- **相关文件**：`build-logic/build.gradle.kts`、`gradle/wrapper/gradle-wrapper.properties`

---

## #020 真题 v2.22 验证器把后续合法版本误判为错误

- **日期**：2026-08-07
- **现象**：丁帆 v2.23 写入后再次运行真题 `--verify-applied`，报告 seed 版本为 `2.23.0` 而非 `2.22.0`。
- **根因**：验证器把批次目标版本当成整个种子的永久版本，没有考虑后续内容批次会继续升级 metadata version。
- **已尝试修复**：
  - ❌ 旧验证逻辑 — 产生 1 个版本误报，但没有修改数据。
  - ✅ 改为检查 v2.22 真题补充记录是否仍存在，并重新验证 79 道真题，errors=0。
- **教训**：增量批次验证应校验自身变更记录和数据实体，不应锁死后续批次的全局版本号。

---

## #021 v2.24 内容批次的完整 Gradle 测试仍被插件缓存阻塞

- **日期**：2026-08-07
- **现象**：丁帆 v2.24 合并后重新运行 `:core:data:test`，Gradle 在配置阶段失败，未进入 Kotlin 源码编译和单元测试。
- **根因**：离线缓存仍缺少 `org.gradle.kotlin.kotlin-dsl:4.3.0`；当前环境不能访问插件仓库补齐该依赖。
- **已尝试修复**：
  - ❌ Gradle 8.7 `--offline`、单 worker、受控 JVM 内存 — `build-logic/build.gradle.kts` 第 2 行停止，退出码 1。
  - ✅ 直接 Kotlin 编译四科框架并运行实际校验 — `frameworks=4 modern=211 total=1023 errors=0`。
- **结论**：这是构建环境依赖阻塞，不是本批 JSON、框架源码或数据校验失败；完整 Android 单测仍需在插件缓存可用或网络恢复后补跑。
- **教训**：每个内容批次都要重新记录当前构建边界，不能沿用旧批次的“已构建”结论。
- **相关文件**：`build-logic/build.gradle.kts`、`core/data/src/main/java/com/wenyan/app/core/data/seed/KnowledgeFramework.kt`
- **相关文件**：`tools/content_supplement/merge_exam_2023_2026_v2_22.py`、`app/src/main/assets/seed_data.json`

---

## 模板（新失败方案按此格式记录）

```markdown
## #NNN 简短标题

- **日期**：YYYY-MM-DD
- **CI Run**：#XXX（如适用）
- **现象**：错误信息
- **根因**：根本原因
- **已尝试修复**：
  - ❌ 方案1 — commit，原因
  - ❌ 方案2 — commit，原因
  - ✅ 方案3 — commit，成功
- **未尝试方案**：如还有未尝试的
- **教训**：一句话总结
- **相关文件**：文件路径列表
```
