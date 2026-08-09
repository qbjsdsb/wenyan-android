# Python OCR 管线脚本

> 本目录包含文研 App 的 OCR 数据处理管线脚本。
> 详细运行说明见 [docs/reference/OCR_PIPELINE.md](../docs/reference/OCR_PIPELINE.md)。

## 文件清单

| 文件 | 用途 | 输入 | 输出 |
|------|------|------|------|
| `scan_files.py` | 文件扫描 | `wenyanziliao/` | `manifest.json` |
| `pipeline_runner.py` | OCR 批处理 | `manifest.json` | `output/file_XXX.json` |
| `rapidocr_pipeline.py` | RapidOCR 处理 | 单个文件 | OCR JSON |
| `post_correct.py` | OCR 校对 | `output/*.json` | `output/corrected/*.json` |
| `extract_knowledge.py` | LLM 知识提取 | `output/corrected/` | `output/knowledge/*.json` |
| `cross_validate.py` | 交叉验证 | `output/knowledge/` | `output/cross_validated/` |
| `generate_seed.py` | 生成种子数据 | `output/cross_validated/` | `app/src/main/assets/seed_data.json` |
| `d_drive_env.py` | D 盘环境配置 | — | 环境变量 |
| `requirements.txt` | Python 依赖 | — | — |
| `environment.yml` | conda 环境导出 | — | — |

## 管线流程

```
scan_files.py → pipeline_runner.py → post_correct.py
  → extract_knowledge.py → cross_validate.py → generate_seed.py
```

最终产出 `seed_data.json`，复制到 `app/src/main/assets/` 并 push 到 GitHub。

## 快速运行

```bash
# 前提：conda 环境 ocr 已激活
cd D:\wenyan\wenyan-android\tools

# 1. 扫描文件
python scan_files.py --input ../../wenyanziliao --output manifest.json

# 2. OCR 批处理（支持断点续传）
python pipeline_runner.py --manifest manifest.json --output output/

# 3. 校对
python post_correct.py --input output/ --output output/corrected/

# 4. 知识点提取
python extract_knowledge.py --input output/corrected/ --output output/knowledge/

# 5. 交叉验证
python cross_validate.py --input output/knowledge/ --output output/cross_validated/

# 6. 生成种子数据
python generate_seed.py --input output/cross_validated/ --output ../app/src/main/assets/
```

## 注意事项

- **OCR 运行时不跑 CPU 密集 Python 任务**
- **需要 LLM API key**（post_correct 和 extract_knowledge）
- **原始教材是版权材料** — 不上传到 GitHub
- **OCR 输出 JSON 体积大** — 不上传到 GitHub
- **只有 seed_data.json 上传到仓库**
- **核心教材 DPI=200，参考书 DPI=150**
- **4 进程并行**
- **断点续传**：pipeline_runner.py 自动跳过已完成文件

## PR-01A：只读 seed 审计

PR-01A 的审计器只读取 seed、schema 和 baseline，不会修改
`app/src/main/assets/seed_data.json`，也不会自动更新 baseline。它使用 Python
标准库，输出不含运行时间、机器绝对路径或教材正文的确定性 JSON 报告。

```bash
python -m tools.content_pipeline.audit_seed \
  --seed app/src/main/assets/seed_data.json \
  --schema content/schema/seed.schema.json \
  --baseline content/baselines/seed-baseline.json \
  --report /tmp/wenyan-seed-audit.json \
  --as-of-year 2026
```

首次建立或经人工审阅后更新 baseline 时，必须显式使用
`--write-baseline PATH`；普通审计不会写入 baseline。退出码为 0 表示 schema、
引用、ID、噪声和 ratchet 检查均通过；已记录的 legacy debt 会保留在报告中，
新增债务、旧 ID 删除和未批准的跨科关系会返回非 0。

```bash
python -m unittest discover -s tools/tests -p 'test*.py'
```
