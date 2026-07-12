# 失败方案档案

> **遇到编译失败/运行错误时必读。** 记录已尝试但失败的方案，避免重复踩坑。
> **新会话遇到错误时，第一步查本文件。**

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
