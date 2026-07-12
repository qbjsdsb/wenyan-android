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
