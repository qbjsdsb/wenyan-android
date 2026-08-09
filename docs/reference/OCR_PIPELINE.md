# OCR 管线运行手册

> 本地运行 OCR 管线的完整指南。云端 AI 不直接运行此管线，但需理解流程以理解 seed_data.json 的来源。

> **ARCHIVED / OBSOLETE（2026-08-09）**：下文的 `D:\wenyan`、Windows 路径、OCR 中间产物和旧脚本流程属于历史工作站恢复资料，不是当前 Android 构建或正式 seed 发布的唯一依据。保留原命令以便追溯；当前 seed 统计与发布边界以 [当前系统基线](../architecture/current-system.md) 和 [AI/OCR 内容审核闸门](../decisions/003-ai-ocr-review-gate.md) 为准。

## 1. 管线流程

```
scan_files.py       → manifest.json（文件清单）
      ↓
pipeline_runner.py  → output/file_XXX.json（OCR 结果）
      ↓
post_correct.py     → output/corrected/（校对后）
      ↓
extract_knowledge.py→ output/knowledge/（知识点）
      ↓
cross_validate.py   → output/cross_validated/（交叉验证）
      ↓
generate_seed.py    → app/src/main/assets/seed_data.json（App 种子数据）
```

## 2. 前置条件

- conda 环境 `ocr`（Python 3.11.15）
- `D:\wenyan\wenyanziliao\` 目录有原始教材
- D 盘环境配置（`d_drive_env.py`）
- MinerU 3.x 已安装
- LLM API key（用于 post_correct 和 extract_knowledge）

## 3. 各步骤说明

### Step 1：scan_files.py — 文件扫描

- **输入**：`wenyanziliao/` 目录
- **输出**：`tools/manifest.json`
- **作用**：扫描所有 PDF/DOCX/XLS，生成清单，含优先级分类
- **命令**：
  ```bash
  cd D:\wenyan\tools
  C:\Users\33425\miniconda3\envs\ocr\python.exe scan_files.py --input ../../wenyanziliao --output manifest.json
  ```
- **优先级**：
  - P1：核心教材（郑克鲁、聂珍钊等关键词）
  - P2：笔记/辅导
  - P3：真题
  - P4：文学理论
  - P5：其他
  - 跳过：32 个预定义名著关键词
  - 跳过：重复文件（"(1)" 后缀）

### Step 2：pipeline_runner.py — OCR 批处理

- **输入**：`manifest.json`
- **输出**：`tools/output/file_XXX.json`
- **作用**：对每个文件执行 OCR，输出结构化 JSON
- **命令**：
  ```bash
  cd D:\wenyan\tools
  C:\Users\33425\miniconda3\envs\ocr\python.exe pipeline_runner.py --manifest manifest.json --output output/
  ```
- **特性**：
  - 断点续传（自动跳过已完成文件）
  - 4 进程并行
  - 核心教材 DPI=200（98%+ 精度）
  - 参考书 DPI=150（95%+ 精度）
  - 输出 JSON 含 `id / relative_path / category / content_source / ocr_status / data`
  - data 内含 pages/sheets/lines，带置信度分数

### Step 3：post_correct.py — OCR 校对

- **输入**：`output/*.json`
- **输出**：`output/corrected/*.json`
- **作用**：高置信度部分自动校对，低置信度调 LLM
- **命令**：
  ```bash
  cd D:\wenyan\tools
  C:\Users\33425\miniconda3\envs\ocr\python.exe post_correct.py --input output/ --output output/corrected/
  ```
- **注意**：高置信度部分 CPU 消耗不大，可谨慎尝试；低置信度部分需 LLM API

### Step 4：extract_knowledge.py — 知识点提取

- **输入**：`output/corrected/`
- **输出**：`output/knowledge/*.json`
- **作用**：调 LLM API 提取知识点、关系、属性
- **命令**：
  ```bash
  cd D:\wenyan\tools
  C:\Users\33425\miniconda3\envs\ocr\python.exe extract_knowledge.py --input output/corrected/ --output output/knowledge/
  ```
- **注意**：需要 LLM API key

### Step 5：cross_validate.py — 交叉验证

- **输入**：`output/knowledge/`
- **输出**：`output/cross_validated/`
- **作用**：交叉验证知识点，去除重复和矛盾
- **命令**：
  ```bash
  cd D:\wenyan\tools
  C:\Users\33425\miniconda3\envs\ocr\python.exe cross_validate.py --input output/knowledge/ --output output/cross_validated/
  ```

### Step 6：generate_seed.py — 生成种子数据

- **输入**：`output/cross_validated/`
- **输出**：`app/src/main/assets/seed_data.json`
- **作用**：生成 App 种子数据，含知识点/真题/卡片
- **命令**：
  ```bash
  cd D:\wenyan\tools
  C:\Users\33425\miniconda3\envs\ocr\python.exe generate_seed.py --input output/cross_validated/ --output ../../wenyan-android/app/src/main/assets/
  ```

## 4. 交接物

**只有 `seed_data.json` 需要上传到 GitHub 仓库。**

- 位置：`app/src/main/assets/seed_data.json`
- 大小：几百 KB（结构化知识点）
- 云端 AI 只读最终产物，不碰 OCR 原始数据

```bash
# 复制到仓库并 push
Copy-Item "D:\wenyan\tools\output\seed_data.json" "D:\wenyan\wenyan-android\app\src\main\assets\seed_data.json"
cd D:\wenyan\wenyan-android
& "C:\Program Files\Git\cmd\git.exe" add app/src/main/assets/seed_data.json
& "C:\Program Files\Git\cmd\git.exe" commit -m "data: update seed_data.json with extracted knowledge"
& "C:\Program Files\Git\cmd\git.exe" push origin main
```

## 5. 进度查询

### 查 OCR 进度
- 读 `tools/manifest.json`，统计各文件 `status` 字段
- `completed` / `pending` / `failed` / `skipped`

### 查当前运行的 OCR 进程
```powershell
Get-Process -Name python* | Select-Object Id, StartTime, CPU
```

## 6. 故障恢复

### 单个文件失败
1. 查 `manifest.json` 中该文件的 `status` 和 `error`
2. 修复后重跑 `pipeline_runner.py`（自动跳过 completed）
3. 或单独跑：`python pipeline_runner.py --file-id 128`

### OCR 进程中断
- `pipeline_runner.py` 支持断点续传
- 重新运行同一命令即可

### 输出 JSON 损坏
- 删除 `output/file_XXX.json`
- 重跑该文件

## 7. 当前进度（2026-07-12）

- 总文件数：208
- 已完成：125（约 60%）
- 跳过：36（名著/重复）
- 待处理：47
- PID：20432
- 已完成 P1 核心教材（郑克鲁/聂珍钊等）
- 剩余主要为 P4 文学理论 + P5 其他
- 预计完成时间：明日上午

## 8. 注意事项

- **OCR 运行时不跑 CPU 密集 Python 任务** — 会拖慢 OCR
- **Android 开发不影响 OCR** — 可并行
- **post_correct 高置信度部分 CPU 消耗不大** — 可谨慎尝试
- **extract_knowledge 需要 LLM API** — 有网络和成本消耗
- **原始教材是版权材料** — 不上传到 GitHub
- **OCR 输出 JSON 体积大** — 不上传到 GitHub（约 500MB）
- **只有 seed_data.json 上传到仓库** — 云端 AI 只读最终产物
