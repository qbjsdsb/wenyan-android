# CI/CD 工作流说明

> GitHub Actions 工作流配置说明。

## 1. 工作流文件

### `.github/workflows/android.yml` — 主 CI

- **触发**：push 到 main / PR 到 main
- **作用**：编译 Debug APK + Release APK + 单元测试
- **步骤**：
  1. Checkout 代码
  2. 配置 JDK 17（temurin）
  3. 配置 Gradle 8.7
  4. 配置 Android SDK
  5. `gradle assembleDebug`
  6. `gradle assembleRelease`
  7. `gradle test`
  8. 上传 APK artifacts

### `.github/workflows/release.yml` — 发布

- **触发**：push tag `v*.*.*`
- **作用**：构建签名 Release APK + 创建 GitHub Release
- **步骤**（16 步）：
  1. Checkout 代码
  2. 配置 JDK 17
  3. 配置 Gradle 8.7
  4. 解码 keystore（base64 → .jks）
  5. 验证 keystore 文件
  6. 验证 storepass
  7. 配置签名
  8. `gradle assembleRelease`（签名）
  9. 验证 APK 签名
  10. 运行单元测试
  11. 重命名 APK（wenyan-vX.Y.Z.apk + wenyan-latest.apk）
  12. 创建 GitHub Release
  13. 上传 APK assets
  14. ... 其他验证步骤

### `.github/workflows/generate-keystore.yml` — keystore 生成

- **触发**：手动（workflow_dispatch）
- **作用**：生成 PKCS12 keystore 并更新 GitHub Secrets
- **注意**：storepass 和 keypass 必须相同（PKCS12 要求）

## 2. CI 命令

CI 使用系统 Gradle（非 wrapper）：

```bash
gradle assembleDebug        # 编译 Debug APK
gradle assembleRelease      # 编译 Release APK（需签名配置）
gradle test                 # 运行单元测试
```

本地可用 wrapper：

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test
```

## 3. 查询 CI 状态

### GitHub API（推荐云端 AI 使用）

```bash
# 最新 Run
curl -H "Authorization: token <GITHUB_TOKEN>" \
  https://api.github.com/repos/qbjsdsb/wenyan-android/actions/runs?per_page=1

# 特定 Run 详情
curl -H "Authorization: token <GITHUB_TOKEN>" \
  https://api.github.com/repos/qbjsdsb/wenyan-android/actions/runs/<RUN_ID>

# Run 日志（需先获取 logs URL）
curl -L -H "Authorization: token <GITHUB_TOKEN>" \
  https://api.github.com/repos/qbjsdsb/wenyan-android/actions/runs/<RUN_ID>/logs -o logs.zip
```

### GitHub Web UI

- https://github.com/qbjsdsb/wenyan-android/actions

## 4. 常见 CI 失败原因

| 原因 | 解决方案 | 详情 |
|------|----------|------|
| Kotlin 版本不兼容 | 升级 Kotlin 或降级依赖 | [03-FAILED-ATTEMPTS.md #001](../03-FAILED-ATTEMPTS.md) |
| AGP 不支持 compileSdk | 升级 AGP | [03-FAILED-ATTEMPTS.md #004](../03-FAILED-ATTEMPTS.md) |
| API 不存在 | 升级 Compose BOM | [03-FAILED-ATTEMPTS.md #005](../03-FAILED-ATTEMPTS.md) |
| 签名失败 | 检查 Secrets 和 keystore | [03-FAILED-ATTEMPTS.md #003](../03-FAILED-ATTEMPTS.md) |
| 测试失败 | 查测试日志，修复测试代码 | — |

## 5. CI 环境 vs 本地环境

| 项 | CI | 本地 |
|----|-----|------|
| OS | ubuntu-latest | Windows |
| Gradle | 8.7（系统） | 8.7（wrapper） |
| JDK | 17（temurin） | 17（conda openjdk） |
| 命令 | `gradle` | `./gradlew` |
| Android SDK | 自动配置 | 需手动配置 |

## 6. artifacts

CI 会产出以下 artifacts：

- `app-debug.apk` — Debug APK
- `app-release-unsigned.apk` — Release APK（未签名）
- 测试报告

Release 工作流额外产出：

- `wenyan-vX.Y.Z.apk` — 签名 Release APK（版本号）
- `wenyan-latest.apk` — 签名 Release APK（latest 标签）

## 7. 注意事项

- CI 失败时先查 [03-FAILED-ATTEMPTS.md](../03-FAILED-ATTEMPTS.md) 是否已有解决方案
- 修改 `gradle/libs.versions.toml` 后必查 [02-VERSION-MATRIX.md](../02-VERSION-MATRIX.md)
- Release 前确保 tag 指向存在的 commit（[03-FAILED-ATTEMPTS.md #006](../03-FAILED-ATTEMPTS.md)）
- PKCS12 keystore 要求 storepass = keypass
