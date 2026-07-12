# 环境配置指南

> 本地与 CI 环境的配置说明。云端 AI 主要用 CI 环境；本地 AI 用本地环境。

## 1. Android 开发环境（云端 AI 适用）

### JDK 17
- 发行版：temurin
- CI 自动配置（见 `.github/workflows/android.yml`）
- 本地需手动安装：`conda install -c conda-forge openjdk=17`

### Gradle 8.7
- CI 使用系统 Gradle（非 wrapper）
- 命令：`gradle assembleDebug` / `assembleRelease` / `test`
- 本地可使用 `./gradlew`（wrapper）

### Android SDK
- compileSdk = 35
- targetSdk = 35
- minSdk = 26
- CI 自动配置 SDK

### Kotlin / AGP / Compose
- Kotlin 2.0.20（待升级到 2.3.0，见 [02-VERSION-MATRIX.md](../02-VERSION-MATRIX.md)）
- AGP 8.6.0
- Compose BOM 2025.12.00
- Compose Compiler（与 Kotlin 同版本）

## 2. Python OCR 环境（本地 AI 适用）

### Miniconda3
- 安装路径：`C:\Users\33425\miniconda3`
- conda 可执行文件：`C:\Users\33425\miniconda3\Scripts\conda.exe`
- ⚠️ conda 不在 PATH，需用完整路径调用

### conda 环境 'ocr'
- Python 3.11.15
- 创建命令：`conda create -n ocr python=3.11.15`
- 激活：`conda activate ocr`（需先初始化）或用完整路径的 python.exe
- Python 路径：`C:\Users\33425\miniconda3\envs\ocr\python.exe`

### 依赖
- 安装：`pip install -r tools/requirements.txt`
- 环境导出：`conda env export > tools/environment.yml`

### MinerU 3.x
- CLI：`mineru -p <input> -o <output> -m auto -b pipeline`
- 配置：`.config/mineru.json`
- 模型缓存：`.cache/modelscope/`

### D 盘环境配置
- `d_drive_env.py` 将所有缓存重定向到 `D:\wenyan\.cache\`
- 避免沙箱限制
- `MODELSCOPE_CACHE` 环境变量指向项目目录

## 3. CI 环境（GitHub Actions）

### Runner
- ubuntu-latest

### 自动配置
- JDK 17（temurin）
- Gradle 8.7
- Android SDK

### 命令
```bash
gradle assembleDebug
gradle assembleRelease
gradle test
```

### 工作流文件
- `.github/workflows/android.yml` — 主 CI（push/PR 触发）
- `.github/workflows/release.yml` — 发布（tag 触发）
- `.github/workflows/generate-keystore.yml` — keystore 生成（手动触发）

## 4. Git 配置

### 本地 Git
- 路径：`C:\Program Files\Git\cmd\git.exe`（不在 PATH）
- 调用方式：`& "C:\Program Files\Git\cmd\git.exe" -C "D:\wenyan\wenyan-android" <command>`
- 版本：2.55.0.2

### 用户配置
- user.name：已配置
- user.email：已配置
- credential.helper：已配置

### PowerShell 注意事项
- PowerShell 5.x，不支持 heredoc（`<<'EOF'`）
- commit 消息用单行，或用 `git commit -F file.txt`
- profile.ps1 不含 conda 初始化（防止终端执行失败）

## 5. 敏感信息配置

### GitHub Secrets（仓库设置中配置）

| Secret 名 | 用途 | 值 |
|-----------|------|-----|
| `KEYSTORE_BASE64` | keystore 文件（base64 编码） | base64 of wenyan-release.jks |
| `KEYSTORE_PASSWORD` | keystore 密码 | （敏感） |
| `KEY_ALIAS` | key 别名 | `wenyan-release` |
| `KEY_PASSWORD` | key 密码 | 同 KEYSTORE_PASSWORD（PKCS12 要求） |

### 本地敏感文件（不入仓库）

| 文件 | 位置 | 说明 |
|------|------|------|
| GitHub token | 用户提供 | 不写入仓库 |
| keystore 文件 | `D:\wenyan\wenyan-keystore\wenyan-release.jks` | 已 base64 存入 Secrets |
| keystore 密码 | `D:\wenyan\wenyan-keystore\keystore-password.txt` | 已存入 Secrets |
| LLM API key | 环境变量 | 本地配置 |
| MinerU 配置 | `.config/mineru.json` | 本地配置 |

## 6. 目录结构

### 本地工作目录（D:\wenyan）

```
D:\wenyan\
├── wenyan-android\          # Android 工程（git 仓库）
├── android\                 # 旧 Android 工程副本（已废弃）
├── wenyanziliao\            # 原始教材（208 PDF/DOCX/XLS，版权材料）
├── tools\                   # OCR 管线脚本（本地副本）
│   ├── output\              # OCR 输出 JSON（~500MB）
│   ├── temp\                # 临时文件
│   └── manifest.json        # OCR 文件清单
├── docs\                    # 设计文档原始位置（已迁移到仓库）
├── .trae\                   # Trae IDE 文件
├── .cache\                  # 模型缓存
├── .config\                 # MinerU 等配置
├── miniconda3\              # Miniconda 安装
├── ocr_env\                 # 旧 OCR 环境
├── wenyan-keystore\         # keystore 文件（敏感）
├── wenyan-keystore-v2\      # 第二版 keystore
├── upload_to_github.ps1     # GitHub 上传脚本（含 token，敏感）
└── ci_log*.txt              # CI 日志
```
