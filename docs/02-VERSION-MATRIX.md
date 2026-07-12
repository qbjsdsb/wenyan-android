# 版本兼容性矩阵

> **修改依赖版本前必读。** 记录所有依赖的版本、兼容性和已知问题。

## 当前版本（gradle/libs.versions.toml）

| 依赖 | 当前版本 | 状态 | 说明 |
|------|----------|------|------|
| AGP | 8.6.0 | ✅ | 支持 compileSdk 35 |
| Kotlin | 2.3.10 | ✅ | 升级后兼容 materialkolor 4.1.1 |
| KSP | 2.3.2 | ✅ | 新版本号格式（不再 `<kotlin>-<ksp>`） |
| Compose BOM | 2025.12.00 | ✅ | 含 Material3 1.4.x stable |
| material3 | 1.5.0-alpha18 | ✅ | 显式锁定，覆盖 BOM 的 1.4.0，含 M3 Expressive API |
| materialkolor | 4.1.1 | ✅ | 用 Kotlin 2.3.0 编译，与 Kotlin 2.3.10 兼容 |
| Hilt | 2.57.1 | ✅ | kotlin-metadata-jvm unshaded，支持 Kotlin 2.3 |
| Room | 2.7.0 | ✅ | 首个支持 KSP2 的稳定版 |
| Coroutines | 1.8.1 | ✅ | |
| Retrofit | 2.9.0 | ✅ | |
| OkHttp | 4.12.0 | ✅ | |
| kotlinx-serialization | 1.6.3 | ✅ | |
| DataStore | 1.1.1 | ✅ | |
| navigation-compose | 2.7.7 | ✅ | |
| activity-compose | 1.9.1 | ✅ | |
| lifecycle | 2.8.4 | ✅ | |
| core-ktx | 1.13.1 | ✅ | |
| material-icons-extended | (BOM) | ✅ | |
| security-crypto | 1.1.0-alpha06 | ✅ | |

## 已知不兼容组合（避免使用）

| 组合 | 问题 | 详情 |
|------|------|------|
| Kotlin 2.0.20 + materialkolor 4.1.1 | 元数据版本 2.3.0 vs 期望 2.0.0 | [03-FAILED-ATTEMPTS.md #001](03-FAILED-ATTEMPTS.md) |
| AGP 8.5.x + compileSdk 35 | AGP 不支持 compileSdk 35 | 需 AGP 8.6.0+ |
| Compose BOM 2024.06.00 + MaterialExpressiveTheme | API 不存在 | 需 BOM 2025.12.00+ |
| PKCS12 keystore + 不同 storepass/keypass | 签名失败 | 必须相同密码 |

## 版本对应关系

### Compose BOM ↔ Material3

| Compose BOM | Material3 版本 | 关键 API |
|-------------|----------------|----------|
| 2024.06.00 | 1.2.x | 基础 M3 |
| 2025.12.00 | 1.4.x（stable） | `MaterialExpressiveTheme` + `MotionScheme.expressive()`（通过 materialkolor 传递依赖拉入 alpha） |
| 2025.12.00 + 显式锁定 | 1.5.0-alpha18 | ✅ 当前使用。`LargeFlexibleTopAppBar`（仍 @ExperimentalMaterial3ExpressiveApi） |

> **重要**：material3 1.5.0-alpha19+ 要求 AGP 9.1.0 + compileSdk 37，与当前
> AGP 8.6.0 不兼容。alpha18 是兼容 AGP 8.6.0 的最后一个 alpha 版本。
> `LargeFlexibleTopAppBar` 在 alpha18 中仍为 `@ExperimentalMaterial3ExpressiveApi`，
> 需在封装组件中显式 `@OptIn`。计划中提到的 "alpha23 graduated Stable" 未使用。

### Kotlin ↔ KSP

| Kotlin 版本 | KSP 版本 | 说明 |
|-------------|----------|------|
| 2.0.20 | 2.0.20-1.0.25 | 旧格式 `<kotlin>-<ksp>` |
| 2.3.10 | 2.3.2 | ✅ 当前使用，新格式（单一版本号） |

KSP 2.3.x 起放弃旧的 `<kotlin-version>-<ksp-version>` 格式，改用与 Kotlin 对齐的
单一版本号（如 `2.3.2`）。注意 KSP 版本号不需要和 Kotlin 完全一致（Kotlin 2.3.10
配 KSP 2.3.2 即可）。

### Kotlin ↔ Compose Compiler

Kotlin 2.0+ 使用 Compose Compiler Gradle 插件（`org.jetbrains.kotlin.plugin.compose`），版本与 Kotlin 版本同步。

如升级 Kotlin 到 2.3.0，Compose Compiler 插件也自动用 2.3.0。

### Kotlin ↔ AGP

| AGP 版本 | 最低 JDK | 支持 compileSdk |
|----------|----------|----------------|
| 8.5.x | 17 | 34 |
| 8.6.0+ | 17 | 35 |
| 8.7.0+ | 17 | 35 |

## 待验证方案

### 方案 A：升级 Kotlin 到 2.3.0（推荐）

**修改**：
```toml
kotlin = "2.3.0"
ksp = "2.3.0-2.0.0"  # 待确认具体版本
```

**风险**：
- KSP 版本需对应
- 其他依赖可能不兼容 Kotlin 2.3.0
- 需重新验证全部编译

**验证步骤**：
1. 修改 libs.versions.toml
2. commit + push
3. 等 CI 运行
4. 查看错误日志
5. 如成功 → 更新本文件为"已验证可行"
6. 如失败 → 记录到 03-FAILED-ATTEMPTS.md

### 方案 B：降级 materialkolor 到 4.0.x

**修改**：
```toml
materialKolor = "4.0.0"  # 待确认具体版本
```

**风险**：
- API 可能不同（`rememberDynamicColorScheme` 签名可能变化）
- 需检查 materialkolor 4.0.x 的文档

**materialkolor 版本历史**：
- 5.0.0（2026-07-11）：最新
- 4.1.1：需 Kotlin 2.3.0
- 4.0.x：可能兼容 Kotlin 2.0.x（待验证）

**验证步骤**：
1. 查 materialkolor 4.0.x 的 release notes
2. 确认 API 兼容性
3. 修改 libs.versions.toml
4. commit + push
5. 等 CI 运行
6. 如 API 不兼容 → 修改调用代码
7. 如成功 → 更新本文件

## 已验证可行组合

经实际编译验证（gradle assembleDebug + gradle testDebugUnitTest 全通过），以下组合可用：

| 依赖 | 版本 | 备注 |
|------|------|------|
| Kotlin | 2.3.10 | 最新稳定 bug fix |
| KSP | 2.3.2 | 新版本号格式（不再 `<kotlin>-<ksp>`） |
| AGP | 8.6.0 | 保持不变，在 Kotlin 2.3.0 兼容范围 |
| Hilt | 2.57.1 | 必须 ≥ 2.57（kotlin-metadata-jvm unshaded），不可用 2.59+（需 AGP 9） |
| Room | 2.7.0 | 必须 ≥ 2.7（KSP2 支持），不可用 3.0.0（包名 breaking change） |
| Compose BOM | 2025.12.00 | 保持不变 |
| material3 | 1.5.0-alpha18 | 显式锁定，覆盖 BOM 的 1.4.0。alpha19+ 需 AGP 9 不可用 |
| materialkolor | 4.1.1 | 保持不变，用 Kotlin 2.3.0 编译，与 Kotlin 2.3.10 兼容 |
| Gradle | 8.14.4 | 系统安装（mise），兼容 AGP 8.6.0 |

## CI 环境版本

| 项 | 版本 |
|----|------|
| Runner | ubuntu-latest |
| JDK | 17（temurin） |
| Gradle | 8.7 |
| 命令 | `gradle assembleDebug` / `assembleRelease` / `test` |

## 本地环境版本

| 项 | 版本 |
|----|------|
| OS | Windows |
| conda | Miniconda3 |
| Python | 3.11.15（ocr 环境） |
| Git | 2.55.0.2 |
| PowerShell | 5.x（不支持 heredoc） |
