# 发布流程

> 发布新版本 APK 的完整流程。

> **ARCHIVED / OBSOLETE（2026-08-09）**：本页保留旧 Windows/D 盘操作路径和历史流程说明；实际触发顺序、Gradle 命令和签名闸门以仓库的 [release.yml](../../.github/workflows/release.yml) 为准。不要据下文旧版本号或命令判断当前发布状态。

## 1. 前置条件

- 代码已 push 到 `main` 分支
- CI（android.yml）通过
- 要发布的功能已开发完成并测试
- keystore 已配置在 GitHub Secrets

## 2. 发布步骤（7 步）

### Step 1：确保 main 分支最新

```bash
cd D:\wenyan\wenyan-android
& "C:\Program Files\Git\cmd\git.exe" pull origin main
```

### Step 2：确认 CI 通过

查 GitHub Actions 最新 Run 状态为 success。

### Step 3：选择版本号

遵循语义化版本：

- `v0.X.0` — 功能更新
- `v0.0.X` — Bug 修复
- `v1.0.0` — 正式发布

### Step 4：删除旧 orphan tag（如存在）

```bash
# 查看现有 tag
& "C:\Program Files\Git\cmd\git.exe" tag -l

# 如有旧 tag 指向不存在的 commit，删除
& "C:\Program Files\Git\cmd\git.exe" tag -d v0.1.0
& "C:\Program Files\Git\cmd\git.exe" push origin :refs/tags/v0.1.0
```

### Step 5：创建新 tag

```bash
# 在 main 分支最新 commit 上创建 tag
& "C:\Program Files\Git\cmd\git.exe" tag v0.X.Y
& "C:\Program Files\Git\cmd\git.exe" push origin v0.X.Y
```

### Step 6：等待 Release workflow 完成

- push tag 后自动触发 `release.yml`
- 约 10-15 分钟完成
- 在 https://github.com/qbjsdsb/wenyan-android/actions 查看进度

### Step 7：验证 Release

1. 检查 GitHub Release 页面：https://github.com/qbjsdsb/wenyan-android/releases
2. 确认有两个 APK asset：
   - `wenyan-vX.Y.Z.apk`
   - `wenyan-latest.apk`
3. 下载 APK 验证签名

## 3. Release workflow 16 步详解

`release.yml` 包含以下 16 步（必须全部通过）：

1. Checkout 代码
2. 配置 JDK 17（temurin）
3. 配置 Gradle 8.7
4. 解码 keystore（base64 → .jks 文件）
5. 验证 keystore 文件存在
6. 验证 storepass 正确（keytool -list）
7. 配置 Gradle 签名（signingConfigs）
8. `gradle assembleRelease`（签名 APK）
9. 验证 APK 签名（apksigner verify）
10. 运行单元测试
11. 重命名 APK 为 `wenyan-vX.Y.Z.apk`
12. 复制 APK 为 `wenyan-latest.apk`
13. 创建 GitHub Release
14. 上传 `wenyan-vX.Y.Z.apk` 到 Release
15. 上传 `wenyan-latest.apk` 到 Release
16. 输出 Release URL

## 4. 已发布的版本

### v0.1.0（2026-07-12）

- 首个签名 Release
- APK 大小：14.7 MB
- 包含：M3 基础组件 + 9 个 Screen + FSRS-6 调度 + AI 服务
- commit：`3a50c2f`
- Release URL：https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.1.0

## 5. 故障排除

### Release workflow 失败

1. 查看失败步骤
2. 如 Step 4-6（keystore）失败 → 检查 GitHub Secrets
3. 如 Step 8（编译）失败 → 查 [03-FAILED-ATTEMPTS.md](../03-FAILED-ATTEMPTS.md)
4. 如 Step 9（签名验证）失败 → keystore 问题，重新生成
5. 如 Step 13（GitHub Release）失败 → 网络/API 问题，重试

### tag 指向不存在的 commit

- 现象：Release workflow 失败，找不到 commit
- 原因：旧 tag 是 orphan
- 修复：删除旧 tag，创建新 tag 指向 main 最新 commit
- 详见 [03-FAILED-ATTEMPTS.md #006](../03-FAILED-ATTEMPTS.md)

### PKCS12 签名失败

- 现象：keystore 验证失败
- 原因：storepass 和 keypass 不一致
- 修复：确保 `KEY_PASSWORD` = `KEYSTORE_PASSWORD`
- 详见 [03-FAILED-ATTEMPTS.md #003](../03-FAILED-ATTEMPTS.md)

## 6. 注意事项

- **Release 由 push tag 触发** — 不要手动运行 release.yml
- **PKCS12 keystore 要求 storepass = keypass**
- **Release 前删除旧 orphan tag**
- **Release 前确保 CI（android.yml）通过**
- **Release 前确保 main 分支最新**
- **Release workflow 必须全部 16 步成功**
- **Release 产物：两个 APK（版本号 + latest）**
- **Release 后更新 [00-STATUS.md](../00-STATUS.md) 的"已交付"部分**
