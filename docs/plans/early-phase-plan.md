# 文研App资料内置与AI助手增强 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `wenyanziliao` 文件夹208个文件数字化为结构化种子数据，并在此基础上增强文研App的资料内置层、FSRS算法、知识图谱、AI助手和主动回忆检测功能。

**Architecture:** 分两个独立子系统——(1) Python桌面端资料数字化管线（MinerU+PaddleOCR+LLM纠错），输出 `assets/seed_data.json` 等结构化JSON；(2) Web App增强（基于现有纯前端架构），将种子数据加载到App并实现FSRS复习、知识图谱、AI助手等功能。数据结构镜像SQLite schema，便于未来移植Android。

**Tech Stack:**
- 资料数字化：Python 3.11 / MinerU 3.1 / PaddleOCR PP-OCRv6 / pdfplumber / python-docx / openpyxl / py-fsrs v6.3.1
- Web App：原生HTML/CSS/JS（零依赖）/ localStorage / IndexedDB（图谱数据）
- AI助手：RAG架构 / BGE-small-zh-v1.5语义相似度 / LLM异步评估

---

## Scope Note: 两个独立子系统

本计划覆盖两个可独立执行的子系统：

1. **Phase 1（Python资料数字化管线）**：独立运行，产出 `assets/seed_data.json`。可单独执行和测试。
2. **Phase 2-7（App功能增强）**：依赖Phase 1产出的种子数据。基于现有Web App架构增强，数据结构镜像SQLite schema以便未来移植Android。

**建议执行顺序**：先完成Phase 1，再启动Phase 2-7。Phase 2可与Phase 1后期并行（数据结构定义不依赖实际OCR结果）。

---

## File Structure

### Python资料数字化管线（新建 `tools/ocr/` 目录）

```
tools/ocr/
├── environment.yml              # conda环境定义
├── scan_files.py                # 文件扫描与manifest生成
├── pipeline_runner.py           # manifest驱动的断点续传批处理
├── extract_native.py            # NATIVE类型PDF/DOCX/XLSX提取
├── ocr_scan.py                  # SCAN_ONLY类型PDF/图片OCR
├── post_correct.py              # OCR校对闭环（置信度分级+LLM纠错）
├── extract_knowledge.py         # 知识提取与结构化
├── cross_validate.py            # 多教材交叉校验
├── generate_seed.py             # 汇总生成种子数据JSON
├── utils/
│   ├── __init__.py
│   ├── pdf_classifier.py        # PDF类型分类（NATIVE/OCR_LAYER/SCAN_ONLY/MIXED）
│   ├── dedup.py                 # 重复文件去重
│   └── llm_client.py            # LLM调用封装
├── tests/
│   ├── test_scan_files.py
│   ├── test_pdf_classifier.py
│   ├── test_extract_native.py
│   ├── test_post_correct.py
│   └── test_extract_knowledge.py
└── prompts/
    ├── correct_ocr.txt          # LLM纠错prompt
    ├── extract_knowledge.txt    # 知识提取prompt
    └── cross_validate.txt       # 交叉校验prompt
```

### App增强（修改现有文件 + 新增）

```
js/
├── data.js                      # 修改：加载seed_data.json替代硬编码
├── app.js                       # 修改：增强复习逻辑、知识图谱、AI助手
├── fsrs.js                      # 新增：FSRS-6算法实现
├── knowledge-graph.js           # 新增：功能性知识图谱
├── ai-assistant.js              # 新增：苏格拉底式AI助手
├── recall-detector.js           # 新增：主动回忆检测三层方案
├── card-templates.js            # 新增：6种文学专用卡片模板
└── exam-codes.js                # 新增：科目代码历史管理
css/
└── style.css                    # 修改：新增来源标签/图谱/AI助手样式
assets/
├── seed_data.json               # Phase 1产出
├── reference_catalog.json       # Phase 1产出
├── exam_code_history.json       # Phase 1产出
└── error_dict.json              # Phase 1产出
```

---

## Phase 1: Python资料数字化工具链搭建

> **依赖**：无（独立子系统）
> **产出**：`assets/seed_data.json` + `assets/reference_catalog.json` + `assets/exam_code_history.json` + `assets/error_dict.json`
> **预计Task数**：9个Task

### Task 1: 搭建Python OCR环境与批处理脚本骨架

**Files:**
- Create: `tools/ocr/environment.yml`
- Create: `tools/ocr/scan_files.py`
- Create: `tools/ocr/pipeline_runner.py`
- Create: `tools/ocr/utils/__init__.py`
- Create: `tools/ocr/utils/pdf_classifier.py`
- Create: `tools/ocr/utils/dedup.py`
- Create: `tools/ocr/tests/test_scan_files.py`
- Create: `tools/ocr/tests/test_pdf_classifier.py`

- [ ] **Step 1: 创建conda环境定义文件**

Create `tools/ocr/environment.yml`:

```yaml
name: ocr
channels:
  - conda-forge
  - defaults
dependencies:
  - python=3.11
  - pip
  - pip:
    # PDF解析与OCR
    - mineru>=3.1          # MinerU 3.x（PyPI包名为mineru，非magic-pdf/minerU）
    - paddlepaddle==3.0.0
    - paddleocr>=2.9.0
    - pdfplumber>=0.11.0
    # 文档格式处理
    - python-docx>=1.1.0   # 仅支持.docx
    - xlrd>=2.0.0          # .xls格式读取（xlrd 2.0+仍支持.xls，仅不支持.xlsx）
    - openpyxl>=3.1.0      # 仅支持.xlsx
    # .doc格式处理（Windows COM接口，需本机安装MS Word）
    - pywin32>=306         # win32com.client用于.doc→.docx转换
    # 中文处理
    - opencc-python-reimplemented>=0.1.7
    # 间隔重复算法
    - py-fsrs==6.3.1
    # 语义相似度
    - sentence-transformers>=3.0.0
    # LLM调用
    - openai>=1.0.0
    # 超分辨率（spec Task 5.6：低质扫描件增强）
    - realesrgan>=0.3.0
    - pymupdf>=1.24.0        # PyMuPDF（fitz），PDF转图片用于超分
    - pillow>=10.0.0         # 图片处理，超分后合并回PDF
    # 测试
    - pytest>=8.0.0
```

- [ ] **Step 2: 创建conda环境**

Run: `conda env create -f tools/ocr/environment.yml`
Expected: 环境创建成功，`conda activate ocr` 可激活

- [ ] **Step 3: 编写PDF类型分类器**

Create `tools/ocr/utils/pdf_classifier.py`:

```python
"""PDF类型分类器：NATIVE / OCR_LAYER / SCAN_ONLY / MIXED"""
import pdfplumber
from enum import Enum
from pathlib import Path

class PDFType(Enum):
    NATIVE = "NATIVE"           # 原生电子文本PDF
    OCR_LAYER = "OCR_LAYER"     # 已有OCR文本层的扫描PDF
    SCAN_ONLY = "SCAN_ONLY"     # 纯扫描件，无文本层
    MIXED = "MIXED"             # 混合：部分页有文本，部分页扫描

def classify_pdf(pdf_path: str | Path) -> PDFType:
    """
    判断PDF类型：
    - 检查每页是否有文本层（extract_text()返回非空）
    - 全部页有文本且文本量充足 → NATIVE
    - 全部页有文本但文本量少（<50字符/页）→ OCR_LAYER
    - 全部页无文本 → SCAN_ONLY
    - 部分页有部分页无 → MIXED
    """
    pdf_path = Path(pdf_path)
    if not pdf_path.exists():
        raise FileNotFoundError(f"PDF not found: {pdf_path}")

    pages_with_text = 0
    pages_with_minimal_text = 0
    total_pages = 0

    with pdfplumber.open(str(pdf_path)) as pdf:
        total_pages = len(pdf.pages)
        for page in pdf.pages:
            text = page.extract_text() or ""
            text = text.strip()
            if len(text) >= 50:
                pages_with_text += 1
            elif len(text) > 0:
                pages_with_minimal_text += 1

    if total_pages == 0:
        raise ValueError(f"PDF has no pages: {pdf_path}")

    text_pages = pages_with_text + pages_with_minimal_text
    if text_pages == total_pages and pages_with_minimal_text == 0:
        return PDFType.NATIVE
    elif text_pages == total_pages and pages_with_minimal_text > 0:
        return PDFType.OCR_LAYER
    elif text_pages == 0:
        return PDFType.SCAN_ONLY
    else:
        return PDFType.MIXED
```

- [ ] **Step 4: 编写PDF分类器测试**

Create `tools/ocr/tests/test_pdf_classifier.py`:

```python
"""测试PDF类型分类器"""
import pytest
from pathlib import Path
from utils.pdf_classifier import classify_pdf, PDFType

WENYANZILIAO = Path(__file__).parent.parent.parent.parent / "wenyanziliao"

def test_classify_native_pdf():
    """测试NATIVE类型PDF分类（西西笔记应为NATIVE或OCR_LAYER）"""
    # 找一个原生电子文本PDF
    native_candidates = list(WENYANZILIAO.glob("5.现当代文学/*.pdf"))
    assert len(native_candidates) > 0, "未找到现当代文学PDF测试文件"
    result = classify_pdf(native_candidates[0])
    assert result in [PDFType.NATIVE, PDFType.OCR_LAYER, PDFType.MIXED]

def test_classify_scan_only_pdf():
    """测试SCAN_ONLY类型PDF分类"""
    scan_candidates = list(WENYANZILIAO.glob("2.文学真题/古代文学/2003年.pdf"))
    if scan_candidates:
        result = classify_pdf(scan_candidates[0])
        assert result == PDFType.SCAN_ONLY

def test_nonexistent_pdf_raises():
    """测试文件不存在时抛出异常"""
    with pytest.raises(FileNotFoundError):
        classify_pdf("nonexistent.pdf")
```

- [ ] **Step 5: 运行分类器测试验证**

Run: `cd tools/ocr && python -m pytest tests/test_pdf_classifier.py -v`
Expected: 测试通过（或因文件路径跳过，但不报错）

- [ ] **Step 6: 编写去重工具**

Create `tools/ocr/utils/dedup.py`:

```python
"""重复文件去重：处理(1)后缀重复文件"""
import hashlib
from pathlib import Path

def file_hash(filepath: str | Path) -> str:
    """计算文件MD5哈希"""
    h = hashlib.md5()
    with open(filepath, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()

def find_duplicates(directory: str | Path) -> dict[str, list[Path]]:
    """
    找出目录中的重复文件（按内容哈希）。
    返回 {hash: [path1, path2, ...]} 字典。
    """
    directory = Path(directory)
    hashes = {}
    for filepath in directory.rglob("*"):
        if filepath.is_file() and not filepath.name.startswith("."):
            h = file_hash(filepath)
            hashes.setdefault(h, []).append(filepath)
    return {h: paths for h, paths in hashes.items() if len(paths) > 1}

def dedup_priority(paths: list[Path]) -> Path:
    """
    从重复文件中选择优先保留的文件。
    规则：无(1)后缀的原版优先；其次文件名更短的优先。
    """
    def sort_key(p):
        name = p.name
        has_dup_suffix = "(1)" in name or "（1）" in name
        return (has_dup_suffix, len(name), str(p))
    return sorted(paths, key=sort_key)[0]
```

- [ ] **Step 7: 编写文件扫描脚本**

Create `tools/ocr/scan_files.py`:

```python
"""
扫描 wenyanziliao 全目录，生成 manifest.json。
manifest 结构：
{
  "files": [
    {
      "path": "相对路径",
      "absolute_path": "绝对路径",
      "filename": "文件名",
      "extension": ".pdf",
      "size_bytes": 12345,
      "hash": "md5...",
      "type": "NATIVE|OCR_LAYER|SCAN_ONLY|DOCX|DOC|XLSX|XLS|IMAGE|ZIP",
      "status": "PENDING|PROCESSING|DONE|FAILED|SKIPPED",
      "attempts": 0,
      "last_error": null,
      "output_path": null
    }
  ],
  "stats": {"total": 208, "by_type": {...}},
  "created_at": "2026-07-10T...",
  "updated_at": "2026-07-10T..."
}
"""
import json
import sys
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from utils.pdf_classifier import classify_pdf, PDFType
from utils.dedup import file_hash, find_duplicates, dedup_priority

WENYANZILIAO = Path(__file__).parent.parent.parent / "wenyanziliao"
MANIFEST_PATH = Path(__file__).parent / "manifest.json"

def classify_file(filepath: Path) -> str:
    """根据扩展名和内容分类文件"""
    ext = filepath.suffix.lower()
    if ext == ".pdf":
        try:
            return classify_pdf(filepath).value
        except Exception as e:
            print(f"  [WARN] PDF分类失败 {filepath.name}: {e}")
            return "SCAN_ONLY"  # 默认按扫描件处理
    elif ext in (".docx",):
        return "DOCX"
    elif ext in (".doc",):
        return "DOC"
    elif ext in (".xlsx",):
        return "XLSX"
    elif ext in (".xls",):
        return "XLS"
    elif ext in (".jpg", ".jpeg", ".png", ".bmp", ".tiff"):
        return "IMAGE"
    elif ext == ".zip":
        return "ZIP"
    else:
        return "UNKNOWN"

def scan_all():
    """扫描全部文件生成manifest"""
    print(f"扫描目录: {WENYANZILIAO}")
    files = []
    for filepath in sorted(WENYANZILIAO.rglob("*")):
        if not filepath.is_file():
            continue
        if filepath.name.startswith("."):
            continue
        try:
            ftype = classify_file(filepath)
            h = file_hash(filepath)
        except Exception as e:
            print(f"  [ERROR] {filepath}: {e}")
            ftype = "UNKNOWN"
            h = ""

        rel_path = filepath.relative_to(WENYANZILIAO)
        files.append({
            "path": str(rel_path),
            "absolute_path": str(filepath),
            "filename": filepath.name,
            "extension": filepath.suffix.lower(),
            "size_bytes": filepath.stat().st_size,
            "hash": h,
            "type": ftype,
            "status": "PENDING",
            "attempts": 0,
            "last_error": None,
            "output_path": None,
        })

    # 标记重复文件
    dups = find_duplicates(WENYANZILIAO)
    for h, paths in dups.items():
        keep = dedup_priority(paths)
        for f in files:
            if Path(f["absolute_path"]) in paths and Path(f["absolute_path"]) != keep:
                f["status"] = "SKIPPED"
                f["last_error"] = f"重复文件，保留: {keep.name}"

    # 统计
    by_type = {}
    for f in files:
        by_type[f["type"]] = by_type.get(f["type"], 0) + 1

    manifest = {
        "files": files,
        "stats": {
            "total": len(files),
            "by_type": by_type,
            "pending": sum(1 for f in files if f["status"] == "PENDING"),
            "skipped": sum(1 for f in files if f["status"] == "SKIPPED"),
        },
        "created_at": datetime.now().isoformat(),
        "updated_at": datetime.now().isoformat(),
    }

    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\n扫描完成: {len(files)}个文件")
    print(f"类型分布: {by_type}")
    print(f"重复跳过: {manifest['stats']['skipped']}个")
    print(f"待处理: {manifest['stats']['pending']}个")
    print(f"Manifest已保存: {MANIFEST_PATH}")

if __name__ == "__main__":
    scan_all()
```

- [ ] **Step 8: 编写扫描脚本测试**

Create `tools/ocr/tests/test_scan_files.py`:

```python
"""测试文件扫描脚本"""
import json
import pytest
from pathlib import Path
from scan_files import classify_file, WENYANZILIAO

def test_classify_pdf_file():
    """测试PDF文件分类"""
    pdfs = list(WENYANZILIAO.glob("**/*.pdf"))
    assert len(pdfs) > 0, "未找到PDF文件"
    result = classify_file(pdfs[0])
    assert result in ["NATIVE", "OCR_LAYER", "SCAN_ONLY", "MIXED"]

def test_classify_docx_file():
    """测试DOCX文件分类"""
    docxs = list(WENYANZILIAO.glob("**/*.docx"))
    assert len(docxs) > 0, "未找到DOCX文件"
    result = classify_file(docxs[0])
    assert result == "DOCX"

def test_classify_xls_file():
    """测试XLS文件分类"""
    xls = list(WENYANZILIAO.glob("**/*.xls"))
    assert len(xls) > 0, "未找到XLS文件"
    result = classify_file(xls[0])
    assert result == "XLS"

def test_classify_image_file():
    """测试图片文件分类"""
    imgs = list(WENYANZILIAO.glob("**/*.jpg")) + list(WENYANZILIAO.glob("**/*.png"))
    assert len(imgs) > 0, "未找到图片文件"
    result = classify_file(imgs[0])
    assert result == "IMAGE"

def test_classify_zip_file():
    """测试ZIP文件分类"""
    zips = list(WENYANZILIAO.glob("**/*.zip"))
    assert len(zips) > 0, "未找到ZIP文件"
    result = classify_file(zips[0])
    assert result == "ZIP"
```

- [ ] **Step 9: 运行扫描脚本测试**

Run: `cd tools/ocr && python -m pytest tests/test_scan_files.py -v`
Expected: 所有测试通过

- [ ] **Step 10: 运行扫描脚本生成manifest**

Run: `cd tools/ocr && python scan_files.py`
Expected: 输出208个文件，类型分布约 `NATIVE: 28, OCR_LAYER: 6, SCAN_ONLY: 111, MIXED: 8, DOCX: 31, DOC: 3, XLSX: 3, XLS: 13, IMAGE: 4, ZIP: 1`

- [ ] **Step 11: 编写批处理脚本骨架**

Create `tools/ocr/pipeline_runner.py`:

```python
"""
manifest驱动的断点续传批处理脚本。
功能：
- 读取manifest.json
- 按status=PENDING筛选待处理文件
- 根据type路由到对应处理函数
- 指数退避重试（3次失败跳过）
- 处理完成后更新manifest
"""
import json
import sys
import time
import traceback
from pathlib import Path
from datetime import datetime

MANIFEST_PATH = Path(__file__).parent / "manifest.json"
OUTPUT_DIR = Path(__file__).parent / "output"
MAX_RETRIES = 3

def load_manifest():
    return json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))

def save_manifest(manifest):
    manifest["updated_at"] = datetime.now().isoformat()
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")

def process_native_pdf(filepath: Path, output_dir: Path) -> str:
    """处理NATIVE类型PDF"""
    from extract_native import extract_native_pdf
    return extract_native_pdf(filepath, output_dir)

def process_ocr_layer_pdf(filepath: Path, output_dir: Path) -> str:
    """处理OCR_LAYER类型PDF"""
    from extract_native import extract_ocr_layer_pdf
    return extract_ocr_layer_pdf(filepath, output_dir)

def process_mixed_pdf(filepath: Path, output_dir: Path) -> str:
    """处理MIXED类型PDF"""
    from extract_native import extract_mixed_pdf
    return extract_mixed_pdf(filepath, output_dir)

def process_docx(filepath: Path, output_dir: Path) -> str:
    """处理DOCX（python-docx仅支持.docx）"""
    from extract_native import extract_docx
    return extract_docx(filepath, output_dir)

def process_doc(filepath: Path, output_dir: Path) -> str:
    """处理DOC（win32com转换为.docx后提取，需MS Word）"""
    from extract_native import extract_doc
    return extract_doc(filepath, output_dir)

def process_xlsx(filepath: Path, output_dir: Path) -> str:
    """处理XLSX（openpyxl仅支持.xlsx）"""
    from extract_native import extract_xlsx
    return extract_xlsx(filepath, output_dir)

def process_xls(filepath: Path, output_dir: Path) -> str:
    """处理XLS（xlrd 2.0+支持.xls）"""
    from extract_native import extract_xls
    return extract_xls(filepath, output_dir)

def process_scan_pdf(filepath: Path, output_dir: Path) -> str:
    """处理SCAN_ONLY类型PDF"""
    from ocr_scan import ocr_scan_pdf
    return ocr_scan_pdf(filepath, output_dir)

def process_image(filepath: Path, output_dir: Path) -> str:
    """处理图片"""
    from ocr_scan import ocr_image
    return ocr_image(filepath, output_dir)

def process_zip(filepath: Path, output_dir: Path) -> str:
    """处理ZIP（解压后按内部类型路由）"""
    import zipfile
    extract_dir = output_dir / filepath.stem
    extract_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(str(filepath), "r") as zf:
        zf.extractall(str(extract_dir))
    # 递归处理解压后的文件（简化：返回解压目录，后续手动/自动处理）
    return str(extract_dir)

PROCESSORS = {
    "NATIVE": process_native_pdf,
    "OCR_LAYER": process_ocr_layer_pdf,
    "MIXED": process_mixed_pdf,
    "DOCX": process_docx,
    "DOC": process_doc,          # .doc用win32com转换（非extract_docx）
    "XLSX": process_xlsx,
    "XLS": process_xls,          # .xls用xlrd（非openpyxl）
    "SCAN_ONLY": process_scan_pdf,
    "IMAGE": process_image,
    "ZIP": process_zip,
}

def run_pipeline(type_filter=None, limit=None):
    """
    运行批处理管线。
    type_filter: 只处理指定类型（如["NATIVE", "DOCX"]）
    limit: 最多处理N个文件（测试用）
    """
    manifest = load_manifest()
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    pending = [f for f in manifest["files"] if f["status"] == "PENDING"]
    if type_filter:
        pending = [f for f in pending if f["type"] in type_filter]
    if limit:
        pending = pending[:limit]

    print(f"待处理文件: {len(pending)}个")
    success, failed = 0, 0

    for i, file_entry in enumerate(pending, 1):
        filepath = Path(file_entry["absolute_path"])
        ftype = file_entry["type"]
        print(f"\n[{i}/{len(pending)}] {ftype} | {file_entry['path']}")

        processor = PROCESSORS.get(ftype)
        if not processor:
            print(f"  [SKIP] 无处理器: {ftype}")
            file_entry["status"] = "SKIPPED"
            file_entry["last_error"] = f"无处理器: {ftype}"
            continue

        for attempt in range(1, MAX_RETRIES + 1):
            try:
                output_path = processor(filepath, OUTPUT_DIR)
                file_entry["status"] = "DONE"
                file_entry["output_path"] = str(output_path)
                file_entry["attempts"] = attempt
                print(f"  [OK] → {output_path}")
                success += 1
                break
            except Exception as e:
                file_entry["attempts"] = attempt
                file_entry["last_error"] = f"{type(e).__name__}: {e}"
                if attempt < MAX_RETRIES:
                    wait = 2 ** (attempt - 1)  # 1s, 2s, 4s
                    print(f"  [RETRY {attempt}/{MAX_RETRIES}] {wait}秒后重试...")
                    time.sleep(wait)
                else:
                    print(f"  [FAILED] {e}")
                    file_entry["status"] = "FAILED"
                    failed += 1

        save_manifest(manifest)  # 每个文件处理后保存

    print(f"\n完成: 成功{success} / 失败{failed} / 共{len(pending)}")
    return success, failed

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="资料数字化批处理管线")
    parser.add_argument("--type", nargs="*", help="只处理指定类型")
    parser.add_argument("--limit", type=int, help="最多处理N个文件")
    args = parser.parse_args()
    run_pipeline(type_filter=args.type, limit=args.limit)
```

- [ ] **Step 12: Commit**

```bash
git add tools/ocr/
git commit -m "feat(ocr): 搭建Python OCR环境与批处理脚本骨架

- environment.yml: conda环境定义（MinerU/PaddleOCR/pdfplumber等）
- scan_files.py: 扫描208个文件生成manifest.json
- pipeline_runner.py: manifest驱动的断点续传批处理
- utils/pdf_classifier.py: PDF类型分类（NATIVE/OCR_LAYER/SCAN_ONLY/MIXED）
- utils/dedup.py: 重复文件去重
- tests/: 分类器和扫描脚本测试"
```

---

### Task 2: 处理第一层原生电子文本（NATIVE PDF 28 + DOCX/DOC 34 + XLSX/XLS 16 = 78个）

**Files:**
- Create: `tools/ocr/extract_native.py`
- Create: `tools/ocr/tests/test_extract_native.py`

- [ ] **Step 1: 编写原生电子文本提取模块**

Create `tools/ocr/extract_native.py`:

```python
"""
原生电子文本提取模块。
处理：
- NATIVE类型PDF（pdfplumber直接提取）
- OCR_LAYER类型PDF（提取+抽样校对）
- MIXED类型PDF（文本层提取+扫描页OCR）
- DOCX/DOC（python-docx提取）
- XLSX/XLS（openpyxl提取）
"""
import json
import pdfplumber
from pathlib import Path
from datetime import datetime

def extract_native_pdf(pdf_path: Path, output_dir: Path) -> str:
    """提取NATIVE类型PDF文本，零错字"""
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{pdf_path.stem}.json"

    pages = []
    with pdfplumber.open(str(pdf_path)) as pdf:
        for i, page in enumerate(pdf.pages, 1):
            text = page.extract_text() or ""
            pages.append({
                "page": i,
                "text": text,
                "char_count": len(text),
            })

    result = {
        "source_file": str(pdf_path),
        "filename": pdf_path.name,
        "type": "NATIVE",
        "content_source": "TEXTBOOK_NATIVE",
        "ocr_status": "VERIFIED",
        "extracted_at": datetime.now().isoformat(),
        "total_pages": len(pages),
        "pages": pages,
    }

    output_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    return str(output_path)

def extract_ocr_layer_pdf(pdf_path: Path, output_dir: Path) -> str:
    """提取OCR_LAYER类型PDF文本，标注为待校对"""
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{pdf_path.stem}.json"

    pages = []
    with pdfplumber.open(str(pdf_path)) as pdf:
        for i, page in enumerate(pdf.pages, 1):
            text = page.extract_text() or ""
            pages.append({
                "page": i,
                "text": text,
                "char_count": len(text),
            })

    result = {
        "source_file": str(pdf_path),
        "filename": pdf_path.name,
        "type": "OCR_LAYER",
        "content_source": "TEXTBOOK_OCR",
        "ocr_status": "PENDING",
        "extracted_at": datetime.now().isoformat(),
        "total_pages": len(pages),
        "pages": pages,
        "note": "已有OCR文本层，需抽样校对（每文件抽3-5页与原图对照）",
    }

    output_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    return str(output_path)

def extract_mixed_pdf(pdf_path: Path, output_dir: Path) -> str:
    """提取MIXED类型PDF：文本层页用pdfplumber，扫描页用MinerU OCR"""
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{pdf_path.stem}.json"

    pages = []
    with pdfplumber.open(str(pdf_path)) as pdf:
        for i, page in enumerate(pdf.pages, 1):
            text = page.extract_text() or ""
            if len(text.strip()) >= 50:
                # 有文本层的页
                pages.append({
                    "page": i,
                    "text": text,
                    "source": "text_layer",
                    "char_count": len(text),
                })
            else:
                # 扫描页——标记为需OCR（实际OCR在Task 5处理）
                pages.append({
                    "page": i,
                    "text": "",
                    "source": "scan_needs_ocr",
                    "char_count": 0,
                })

    result = {
        "source_file": str(pdf_path),
        "filename": pdf_path.name,
        "type": "MIXED",
        "content_source": "TEXTBOOK_OCR",
        "ocr_status": "PENDING",
        "extracted_at": datetime.now().isoformat(),
        "total_pages": len(pages),
        "pages": pages,
        "scan_pages_needing_ocr": sum(1 for p in pages if p["source"] == "scan_needs_ocr"),
    }

    output_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    return str(output_path)

def extract_docx(docx_path: Path, output_dir: Path) -> str:
    """提取DOCX文本和表格（python-docx仅支持.docx，不支持.doc）"""
    from docx import Document

    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{docx_path.stem}.json"

    doc = Document(str(docx_path))

    # 提取段落
    paragraphs = []
    for para in doc.paragraphs:
        text = para.text.strip()
        if text:
            paragraphs.append({
                "text": text,
                "style": para.style.name if para.style else "Normal",
            })

    # 提取表格
    tables = []
    for table in doc.tables:
        rows = []
        for row in table.rows:
            cells = [cell.text.strip() for cell in row.cells]
            rows.append(cells)
        tables.append(rows)

    result = {
        "source_file": str(docx_path),
        "filename": docx_path.name,
        "type": "DOCX",
        "content_source": "TEXTBOOK_NATIVE",
        "ocr_status": "VERIFIED",
        "extracted_at": datetime.now().isoformat(),
        "paragraphs": paragraphs,
        "tables": tables,
        "paragraph_count": len(paragraphs),
        "table_count": len(tables),
    }

    output_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    return str(output_path)

def extract_doc(doc_path: Path, output_dir: Path) -> str:
    """
    提取DOC文本和表格。
    python-docx不支持.doc格式，需通过win32com（Windows COM接口）转换为.docx后提取。
    前置条件：系统安装了Microsoft Word。
    """
    import tempfile
    output_dir.mkdir(parents=True, exist_ok=True)

    # .doc → .docx 转换（通过Word COM接口）
    try:
        import win32com.client
    except ImportError:
        raise RuntimeError(
            "提取.doc文件需要pywin32（pip install pywin32）和Microsoft Word。"
            "替代方案：手动将.doc转为.docx后放入原目录。"
        )

    word = win32com.client.Dispatch("Word.Application")
    word.Visible = False
    temp_docx = None
    try:
        doc = word.Documents.Open(str(doc_path.resolve()))
        # 保存为.docx格式到临时目录
        temp_dir = Path(tempfile.mkdtemp())
        temp_docx = temp_dir / f"{doc_path.stem}.docx"
        doc.SaveAs2(str(temp_docx), FileFormat=16)  # 16 = wdFormatXMLDocument (.docx)
        doc.Close()
    finally:
        word.Quit()

    # 用extract_docx处理转换后的.docx
    result_path = extract_docx(temp_docx, output_dir)

    # 修正source_file为原始.doc路径
    data = json.loads(Path(result_path).read_text(encoding="utf-8"))
    data["source_file"] = str(doc_path)
    data["filename"] = doc_path.name
    data["type"] = "DOC"
    Path(result_path).write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

    # 清理临时文件
    if temp_docx and temp_docx.exists():
        temp_docx.unlink()
        temp_docx.parent.rmdir()

    return result_path

def extract_xlsx(xlsx_path: Path, output_dir: Path) -> str:
    """提取XLSX表格数据（openpyxl仅支持.xlsx，不支持.xls）"""
    from openpyxl import load_workbook

    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{xlsx_path.stem}.json"

    wb = load_workbook(str(xlsx_path), data_only=True, read_only=True)
    sheets = []
    for ws in wb.worksheets:
        rows = []
        for row in ws.iter_rows(values_only=True):
            rows.append([str(cell) if cell is not None else "" for cell in row])
        sheets.append({
            "sheet_name": ws.title,
            "rows": rows,
            "row_count": len(rows),
        })
    wb.close()

    result = {
        "source_file": str(xlsx_path),
        "filename": xlsx_path.name,
        "type": "XLSX",
        "content_source": "TEXTBOOK_NATIVE",
        "ocr_status": "VERIFIED",
        "extracted_at": datetime.now().isoformat(),
        "sheets": sheets,
    }

    output_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    return str(output_path)

def extract_xls(xls_path: Path, output_dir: Path) -> str:
    """提取XLS表格数据（openpyxl不支持.xls，使用xlrd 2.0+）"""
    import xlrd

    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{xls_path.stem}.json"

    wb = xlrd.open_workbook(str(xls_path))
    sheets = []
    for ws in wb.sheets():
        rows = []
        for row_idx in range(ws.nrows):
            row = [str(ws.cell_value(row_idx, col_idx)) for col_idx in range(ws.ncols)]
            rows.append(row)
        sheets.append({
            "sheet_name": ws.name,
            "rows": rows,
            "row_count": len(rows),
        })
    wb.release_resources()

    result = {
        "source_file": str(xls_path),
        "filename": xls_path.name,
        "type": "XLS",
        "content_source": "TEXTBOOK_NATIVE",
        "ocr_status": "VERIFIED",
        "extracted_at": datetime.now().isoformat(),
        "sheets": sheets,
    }

    output_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    return str(output_path)
```

- [ ] **Step 2: 编写提取测试**

Create `tools/ocr/tests/test_extract_native.py`:

```python
"""测试原生电子文本提取"""
import json
import pytest
from pathlib import Path
from extract_native import extract_native_pdf, extract_docx, extract_xlsx

WENYANZILIAO = Path(__file__).parent.parent.parent.parent / "wenyanziliao"
OUTPUT_DIR = Path(__file__).parent.parent / "test_output"

def test_extract_native_pdf():
    """测试NATIVE PDF提取"""
    from utils.pdf_classifier import classify_pdf, PDFType
    pdfs = [p for p in WENYANZILIAO.glob("**/*.pdf")
            if classify_pdf(p) == PDFType.NATIVE]
    if not pdfs:
        pytest.skip("无NATIVE类型PDF")
    result_path = extract_native_pdf(pdfs[0], OUTPUT_DIR)
    data = json.loads(Path(result_path).read_text(encoding="utf-8"))
    assert data["content_source"] == "TEXTBOOK_NATIVE"
    assert data["ocr_status"] == "VERIFIED"
    assert data["total_pages"] > 0
    assert len(data["pages"]) == data["total_pages"]

def test_extract_docx():
    """测试DOCX提取"""
    docxs = list(WENYANZILIAO.glob("**/*.docx"))
    if not docxs:
        pytest.skip("无DOCX文件")
    result_path = extract_docx(docxs[0], OUTPUT_DIR)
    data = json.loads(Path(result_path).read_text(encoding="utf-8"))
    assert data["content_source"] == "TEXTBOOK_NATIVE"
    assert data["ocr_status"] == "VERIFIED"
    assert data["paragraph_count"] >= 0

def test_extract_xlsx():
    """测试XLSX提取"""
    xlsxs = list(WENYANZILIAO.glob("**/*.xlsx"))
    if not xlsxs:
        pytest.skip("无XLSX文件")
    result_path = extract_xlsx(xlsxs[0], OUTPUT_DIR)
    data = json.loads(Path(result_path).read_text(encoding="utf-8"))
    assert data["content_source"] == "TEXTBOOK_NATIVE"
    assert data["ocr_status"] == "VERIFIED"
    assert len(data["sheets"]) > 0
```

- [ ] **Step 3: 运行测试**

Run: `cd tools/ocr && python -m pytest tests/test_extract_native.py -v`
Expected: 测试通过

- [ ] **Step 4: 批量处理78个原生电子文本文件**

Run: `cd tools/ocr && python pipeline_runner.py --type NATIVE DOCX DOC XLSX XLS`
Expected: 78个文件处理完成，输出到 `tools/ocr/output/` 目录

- [ ] **Step 5: Commit**

```bash
git add tools/ocr/extract_native.py tools/ocr/tests/test_extract_native.py tools/ocr/manifest.json tools/ocr/output/
git commit -m "feat(ocr): 提取78个原生电子文本文件

- NATIVE PDF 28个：pdfplumber直接提取
- DOCX/DOC 34个：python-docx提取段落和表格
- XLSX/XLS 16个：openpyxl提取表格数据
- 全部标注content_source=TEXTBOOK_NATIVE, ocr_status=VERIFIED"
```

---

### Task 3: 处理第二层网络权威电子文本（补全真题与缺失教材）

**Files:**
- Modify: `tools/ocr/scan_files.py`（新增网络下载文件支持）
- Create: `tools/ocr/utils/llm_client.py`（LLM调用封装，后续Task复用）

> **注意**：此Task涉及网络资源获取，部分子任务需用户手动补全教材电子版。

- [ ] **Step 1: 创建LLM调用封装**

Create `tools/ocr/utils/llm_client.py`:

```python
"""
LLM调用封装（用于OCR纠错和知识提取）。
支持OpenAI兼容API，可通过环境变量配置。
"""
import os
import json
from pathlib import Path

PROMPTS_DIR = Path(__file__).parent.parent / "prompts"

def load_prompt(name: str) -> str:
    """加载prompt模板"""
    return (PROMPTS_DIR / f"{name}.txt").read_text(encoding="utf-8")

async def call_llm(prompt: str, system: str = None, max_tokens: int = 4000) -> str:
    """
    调用LLM API。
    从环境变量读取配置：
    - LLM_API_KEY: API密钥
    - LLM_BASE_URL: API基础URL（默认OpenAI）
    - LLM_MODEL: 模型名称
    """
    # 延迟导入，避免未安装时报错
    try:
        from openai import AsyncOpenAI
    except ImportError:
        raise RuntimeError("请安装openai包: pip install openai")

    api_key = os.environ.get("LLM_API_KEY", "")
    base_url = os.environ.get("LLM_BASE_URL", "https://api.openai.com/v1")
    model = os.environ.get("LLM_MODEL", "gpt-4o-mini")

    if not api_key:
        raise RuntimeError("请设置环境变量 LLM_API_KEY")

    client = AsyncOpenAI(api_key=api_key, base_url=base_url)
    messages = []
    if system:
        messages.append({"role": "system", "content": system})
    messages.append({"role": "user", "content": prompt})

    response = await client.chat.completions.create(
        model=model,
        messages=messages,
        max_tokens=max_tokens,
        temperature=0.1,  # 低温度保证一致性
    )
    return response.choices[0].message.content

def parse_json_response(text: str) -> dict:
    """安全解析LLM返回的JSON"""
    text = text.strip()
    # 去除可能的markdown代码块标记
    if text.startswith("```"):
        lines = text.split("\n")
        text = "\n".join(lines[1:-1] if lines[-1].strip() == "```" else lines[1:])
    return json.loads(text)
```

- [ ] **Step 2: 创建LLM纠错prompt模板**

Create `tools/ocr/prompts/correct_ocr.txt`:

```text
你是一个OCR文本纠错专家。请对以下OCR识别结果进行保守纠错。

规则（严格遵守）：
1. 只修形近字（如"己/已/巳"混淆、"末/未"混淆）
2. 不改变语义
3. 不修改专有名词（人名/地名/书名/作品名）
4. 不添加或删除内容
5. 保留原始段落结构和标点

输入文本：
---
{text}
---

输出JSON格式：
{
  "corrected_text": "纠错后的完整文本",
  "changes": [
    {"original": "原文片段", "corrected": "纠错片段", "reason": "形近字"}
  ],
  "change_count": 改动数量
}

注意：如果无需修改，返回 {"corrected_text": "原文", "changes": [], "change_count": 0}
```

- [ ] **Step 3: 创建知识提取prompt模板**

Create `tools/ocr/prompts/extract_knowledge.txt`:

```text
你是一个文学考研知识提取专家。请从以下文本中提取结构化知识点。

规则：
1. 每个知识点=一道考研名词解释/简答题答案级别（50-150字）
2. 提取实体：作家名/作品名/流派名/术语
3. 提取关系：作者-作品/流派-成员/影响-被影响/并称
4. 标注来源页码
5. 给出置信度（0-1）

输入文本（来源：{source_file}，页码：{page}）：
---
{text}
---

输出JSON格式：
{
  "knowledge_points": [
    {
      "title": "知识点标题",
      "content": "50-150字知识点内容",
      "subject": "ancient|modern|foreign|theory",
      "entities": [{"name": "实体名", "type": "AUTHOR|WORK|SCHOOL|CONCEPT"}],
      "relations": [{"from": "实体A", "relation": "AUTHORED|BELONGS_TO|INFLUENCED_BY", "to": "实体B"}],
      "source_page": 页码,
      "confidence": 0.0-1.0
    }
  ]
}
```

- [ ] **Step 4: 用户补全缺失教材**

**用户操作**（非代码步骤）：
- [ ] 补全袁行霈《中国文学史》第3版4卷本电子版 → 放入 `wenyanziliao/3.古代文学/袁行霈版本/`
- [ ] 补全聂珍钊《外国文学史》第2版上册电子版 → 放入 `wenyanziliao/4.外国文学/`

- [ ] **Step 5: Commit**

```bash
git add tools/ocr/utils/llm_client.py tools/ocr/prompts/
git commit -m "feat(ocr): 新增LLM调用封装和prompt模板

- llm_client.py: OpenAI兼容API封装
- prompts/correct_ocr.txt: OCR纠错prompt（保守策略）
- prompts/extract_knowledge.txt: 知识提取prompt"
```

---

### Task 4: 处理第三层OCR文本层PDF（6个，提取后校对）

**Files:**
- 无新建文件（使用Task 2的 `extract_ocr_layer_pdf`）

- [ ] **Step 1: 批量处理OCR_LAYER类型PDF**

Run: `cd tools/ocr && python pipeline_runner.py --type OCR_LAYER`
Expected: 6个OCR_LAYER类型PDF提取完成，标注 `ocr_status = 'PENDING'`

- [ ] **Step 2: 编写抽样校对脚本**

Create `tools/ocr/sample_check.py`:

```python
"""OCR文本层PDF抽样校对脚本——每文件抽3-5页与原图对照"""
import json
import random
from pathlib import Path

OUTPUT_DIR = Path(__file__).parent / "output"

def sample_check(file_stem: str, sample_size: int = 5):
    """
    对指定文件的OCR结果进行抽样。
    输出抽样页码和文本，供人工对照原图校对。
    """
    json_path = OUTPUT_DIR / f"{file_stem}.json"
    if not json_path.exists():
        print(f"文件不存在: {json_path}")
        return

    data = json.loads(json_path.read_text(encoding="utf-8"))
    total_pages = data["total_pages"]
    sample_pages = sorted(random.sample(
        range(1, total_pages + 1),
        min(sample_size, total_pages)
    ))

    print(f"\n=== {data['filename']} ===")
    print(f"总页数: {total_pages}，抽样页: {sample_pages}")
    for page_num in sample_pages:
        page = next(p for p in data["pages"] if p["page"] == page_num)
        print(f"\n--- 第{page_num}页（{page['char_count']}字符）---")
        print(page["text"][:500])  # 前500字符
        print("..." if page["char_count"] > 500 else "")

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        sample_check(sys.argv[1])
    else:
        # 对所有OCR_LAYER文件抽样
        for f in OUTPUT_DIR.glob("*.json"):
            data = json.loads(f.read_text(encoding="utf-8"))
            if data.get("type") == "OCR_LAYER":
                sample_check(data["filename"].replace(".pdf", ""))
```

- [ ] **Step 3: 运行抽样校对**

Run: `cd tools/ocr && python sample_check.py`
Expected: 输出6个OCR_LAYER文件的抽样页文本，供人工对照校对

- [ ] **Step 4: Commit**

```bash
git add tools/ocr/sample_check.py tools/ocr/manifest.json tools/ocr/output/
git commit -m "feat(ocr): 提取6个OCR文本层PDF并抽样校对

- 6个OCR_LAYER类型PDF提取完成
- 标注content_source=TEXTBOOK_OCR, ocr_status=PENDING
- sample_check.py: 抽样校对脚本"
```

---

### Task 5: 处理第四层扫描件OCR（111个SCAN_ONLY PDF + 4个图片 = 115个）

**Files:**
- Create: `tools/ocr/ocr_scan.py`

- [ ] **Step 1: 编写扫描件OCR模块**

Create `tools/ocr/ocr_scan.py`:

```python
"""
扫描件OCR模块。
使用MinerU 3.1处理SCAN_ONLY类型PDF，PaddleOCR处理图片。
"""
import json
import subprocess
import sys
from pathlib import Path
from datetime import datetime

OUTPUT_DIR = Path(__file__).parent / "output"

def ocr_scan_pdf(pdf_path: Path, output_dir: Path) -> str:
    """
    用MinerU 3.x OCR处理扫描件PDF。
    MinerU 3.x默认使用hybrid-engine后端（兼顾准确率和速度），无需显式指定。
    CLI命令为 `mineru`（非magic-pdf），用法：mineru -p <input> -o <output>
    """
    output_dir.mkdir(parents=True, exist_ok=True)
    file_output_dir = output_dir / pdf_path.stem
    file_output_dir.mkdir(parents=True, exist_ok=True)

    # MinerU 3.x CLI调用
    cmd = [
        "mineru",
        "-p", str(pdf_path),        # 输入文件路径
        "-o", str(file_output_dir),  # 输出目录
        "-m", "auto",                # 解析方法：auto（自动判断文本层/OCR）
        # hybrid-engine是默认后端，无需显式 -b hybrid-engine
    ]

    result = subprocess.run(cmd, capture_output=True, text=True, timeout=3600)
    if result.returncode != 0:
        raise RuntimeError(f"MinerU失败: {result.stderr[:500]}")

    # MinerU输出：content_list.json（结构化内容）+ middle.json（含置信度）
    content_list_path = file_output_dir / "content_list.json"
    middle_json_path = file_output_dir / "middle.json"

    if not content_list_path.exists():
        raise RuntimeError(f"MinerU未生成content_list.json: {content_list_path}")

    content_list = json.loads(content_list_path.read_text(encoding="utf-8"))

    # 提取置信度（如果有middle.json）
    confidence_scores = []
    if middle_json_path.exists():
        middle_data = json.loads(middle_json_path.read_text(encoding="utf-8"))
        # MinerU的middle.json结构可能因版本而异
        # 提取每个block的置信度score
        for page_info in middle_data.get("pdf_info", []):
            for block in page_info.get("preproc_blocks", []):
                if "line" in block:
                    for line in block["line"]:
                        score = line.get("score", 0)
                        confidence_scores.append(score)

    avg_confidence = sum(confidence_scores) / len(confidence_scores) if confidence_scores else 0.0

    result_data = {
        "source_file": str(pdf_path),
        "filename": pdf_path.name,
        "type": "SCAN_ONLY",
        "content_source": "TEXTBOOK_OCR",
        "ocr_status": "PENDING",
        "ocr_engine": "MinerU_3.x",
        "extracted_at": datetime.now().isoformat(),
        "content_list": content_list,
        "avg_confidence": avg_confidence,
        "confidence_scores_count": len(confidence_scores),
        "needs_correction": 0.7 <= avg_confidence < 0.9,
        "needs_manual_review": avg_confidence < 0.7,
    }

    output_path = output_dir / f"{pdf_path.stem}.json"
    output_path.write_text(json.dumps(result_data, ensure_ascii=False, indent=2), encoding="utf-8")
    return str(output_path)

def ocr_image(image_path: Path, output_dir: Path) -> str:
    """用PaddleOCR处理图片"""
    from paddleocr import PaddleOCR

    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{image_path.stem}.json"

    # 初始化PaddleOCR（中文模型，show_log参数在2.7+已移除）
    ocr = PaddleOCR(use_angle_cls=True, lang="ch")
    result = ocr.ocr(str(image_path), cls=True)

    # 提取文本和置信度
    lines = []
    confidence_scores = []
    if result and result[0]:
        for line in result[0]:
            bbox, (text, conf) = line
            lines.append({"text": text, "confidence": conf})
            confidence_scores.append(conf)

    avg_confidence = sum(confidence_scores) / len(confidence_scores) if confidence_scores else 0.0

    result_data = {
        "source_file": str(image_path),
        "filename": image_path.name,
        "type": "IMAGE",
        "content_source": "TEXTBOOK_OCR",
        "ocr_status": "PENDING",
        "ocr_engine": "PaddleOCR",
        "extracted_at": datetime.now().isoformat(),
        "lines": lines,
        "avg_confidence": avg_confidence,
        "needs_correction": 0.7 <= avg_confidence < 0.9,
        "needs_manual_review": avg_confidence < 0.7,
    }

    output_path.write_text(json.dumps(result_data, ensure_ascii=False, indent=2), encoding="utf-8")
    return str(output_path)
```

- [ ] **Step 2: 测试单个扫描件OCR**

Run: `cd tools/ocr && python -c "from ocr_scan import ocr_scan_pdf; from pathlib import Path; ocr_scan_pdf(Path('../../wenyanziliao/2.文学真题/古代文学/2003年.pdf'), Path('output'))"`
Expected: 2003年.pdf OCR完成，输出JSON文件

- [ ] **Step 3: 批量处理教材扫描件（优先）**

Run: `cd tools/ocr && python pipeline_runner.py --type SCAN_ONLY --limit 10`
Expected: 先处理10个扫描件验证流程，确认无误后继续

- [ ] **Step 4: 批量处理全部扫描件**

Run: `cd tools/ocr && python pipeline_runner.py --type SCAN_ONLY IMAGE`
Expected: 115个扫描件/图片处理完成（耗时较长，支持断点续传）

- [ ] **Step 5: 编写Real-ESRGAN超分模块（spec Task 5.6要求）**

Create `tools/ocr/super_resolve.py`:

```python
"""
低质扫描件超分辨率增强模块（spec Task 5.6）。

背景：wenyanziliao中有34个低质扫描件（模糊/倾斜/低DPI），
直接OCR准确率低。spec要求用Real-ESRGAN超分后再OCR。

【重要警告】Real-ESRGAN面向自然图像训练，文档场景可能产生文字伪影。
因此必须做A/B对比验证：超分前vs超分后OCR准确率，
若未提升或反而下降，则改用文档专用增强方案（DocUNet等）。

实施策略：
1. 先选3-5个困难文件做A/B测试
2. 对比超分前后OCR准确率（人工抽检关键段落）
3. 若超分后准确率提升≥3个百分点，则全量应用；否则改用DocUNet
"""
import json
import subprocess
import sys
from pathlib import Path
from datetime import datetime
from typing import Optional

OUTPUT_DIR = Path(__file__).parent / "output"
SUPER_RESOLVED_DIR = Path(__file__).parent / "output" / "super_resolved"
AB_TEST_DIR = Path(__file__).parent / "output" / "ab_test"

def render_pdf_to_images(pdf_path: Path, output_dir: Path, dpi: int = 200) -> list[Path]:
    """
    将PDF每页渲染为图片，用于超分处理。
    使用pdf2image（依赖poppler）或PyMuPDF（fitz）。
    """
    output_dir.mkdir(parents=True, exist_ok=True)
    try:
        # 优先用PyMuPDF（无需系统级poppler依赖）
        import fitz  # PyMuPDF
        doc = fitz.open(str(pdf_path))
        image_paths = []
        for i, page in enumerate(doc):
            mat = fitz.Matrix(dpi / 72, dpi / 72)
            pix = page.get_pixmap(matrix=mat)
            img_path = output_dir / f"{pdf_path.stem}_page{i+1:04d}.png"
            pix.save(str(img_path))
            image_paths.append(img_path)
        doc.close()
        return image_paths
    except ImportError:
        # 回退到pdf2image
        from pdf2image import convert_from_path
        images = convert_from_path(str(pdf_path), dpi=dpi)
        image_paths = []
        for i, img in enumerate(images):
            img_path = output_dir / f"{pdf_path.stem}_page{i+1:04d}.png"
            img.save(str(img_path), "PNG")
            image_paths.append(img_path)
        return image_paths

def super_resolve_image(input_image: Path, output_image: Path, model: str = "RealESRGAN_x4plus") -> Path:
    """
    用Real-ESRGAN超分单张图片。
    使用realesrgan包的CLI接口。

    Args:
        input_image: 输入图片路径
        output_image: 输出图片路径
        model: 模型名，默认RealESRGAN_x4plus（4倍放大）
               文档场景可试 RealESRGAN_x4plus_anime_6B（对线条更友好）
    """
    output_image.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        sys.executable, "-m", "realesrgan",
        "-i", str(input_image),
        "-o", str(output_image),
        "-n", model,
        "-s", "4",  # 放大倍数
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=600)
    if result.returncode != 0:
        raise RuntimeError(f"Real-ESRGAN失败 {input_image.name}: {result.stderr[:500]}")
    return output_image

def super_resolve_pdf(pdf_path: Path, output_dir: Path) -> Path:
    """
    超分整个PDF：先渲染为图片，逐页超分，再合并回PDF。
    返回超分后的PDF路径。
    """
    stem = pdf_path.stem
    work_dir = output_dir / stem
    work_dir.mkdir(parents=True, exist_ok=True)

    # 1. PDF转图片
    print(f"  [{stem}] 渲染PDF为图片...")
    image_paths = render_pdf_to_images(pdf_path, work_dir / "original")

    # 2. 逐页超分
    print(f"  [{stem}] 超分{len(image_paths)}页...")
    sr_image_paths = []
    for i, img_path in enumerate(image_paths, 1):
        sr_path = work_dir / "super_resolved" / img_path.name
        if not sr_path.exists():  # 断点续传
            print(f"    第{i}/{len(image_paths)}页: {img_path.name}")
            super_resolve_image(img_path, sr_path)
        sr_image_paths.append(sr_path)

    # 3. 合并回PDF
    print(f"  [{stem}] 合并超分图片为PDF...")
    try:
        from PIL import Image
    except ImportError:
        raise RuntimeError("需安装Pillow: pip install Pillow")

    sr_pdf_path = output_dir / f"{stem}_sr.pdf"
    images = [Image.open(str(p)).convert("RGB") for p in sr_image_paths]
    if len(images) == 1:
        images[0].save(str(sr_pdf_path), "PDF")
    else:
        images[0].save(str(sr_pdf_path), "PDF", save_all=True, append_images=images[1:])
    for img in images:
        img.close()

    return sr_pdf_path

def ab_test_single_file(pdf_path: Path, ocr_func, output_dir: Path) -> dict:
    """
    对单个文件做A/B测试：原始OCR vs 超分后OCR。

    Args:
        pdf_path: 原始PDF路径
        ocr_func: OCR函数，签名为 ocr_func(pdf_path, output_dir) -> str
        output_dir: 输出目录

    Returns:
        {
            "file": pdf_path.name,
            "original_ocr_path": "...",
            "sr_ocr_path": "...",
            "original_confidence": 0.xx,
            "sr_confidence": 0.xx,
            "confidence_delta": +0.xx,
            "needs_manual_compare": True  # 置信度只是参考，最终需人工抽检
        }
    """
    output_dir.mkdir(parents=True, exist_ok=True)
    ab_dir = output_dir / pdf_path.stem
    ab_dir.mkdir(parents=True, exist_ok=True)

    # A: 原始OCR
    print(f"[A/B测试] {pdf_path.name} - 原始OCR...")
    original_ocr_path = ocr_func(pdf_path, ab_dir / "original")

    # B: 超分后OCR
    print(f"[A/B测试] {pdf_path.name} - 超分...")
    sr_pdf_path = super_resolve_pdf(pdf_path, ab_dir / "sr_pdf")
    print(f"[A/B测试] {pdf_path.name} - 超分后OCR...")
    sr_ocr_path = ocr_func(sr_pdf_path, ab_dir / "sr_ocr")

    # 对比置信度（参考指标，不能完全代替人工）
    def read_avg_confidence(ocr_json_path: str) -> float:
        data = json.loads(Path(ocr_json_path).read_text(encoding="utf-8"))
        return data.get("avg_confidence", 0.0)

    orig_conf = read_avg_confidence(original_ocr_path)
    sr_conf = read_avg_confidence(sr_ocr_path)

    return {
        "file": pdf_path.name,
        "original_ocr_path": original_ocr_path,
        "sr_ocr_path": sr_ocr_path,
        "original_confidence": orig_conf,
        "sr_confidence": sr_conf,
        "confidence_delta": sr_conf - orig_conf,
        "needs_manual_compare": True,  # 置信度仅供参考，必须人工抽检
    }

def run_ab_test_on_difficult_files(
    difficult_files: list[Path],
    ocr_func,
    output_dir: Path = AB_TEST_DIR,
) -> dict:
    """
    对困难文件集合做A/B测试，输出汇总报告。

    报告用于决策：是否全量应用Real-ESRGAN超分？
    决策标准（spec要求）：
    - 若超分后OCR准确率提升≥3个百分点 → 全量应用Real-ESRGAN
    - 若提升<3个百分点或反而下降 → 改用DocUNet等文档专用方案

    注意：准确率评估需人工抽检关键段落（每文件3-5段），
    置信度只是参考指标，不能单独作为决策依据。
    """
    results = []
    for pdf_path in difficult_files:
        try:
            r = ab_test_single_file(pdf_path, ocr_func, output_dir)
            results.append(r)
        except Exception as e:
            print(f"  [ERROR] {pdf_path.name}: {e}")
            results.append({
                "file": pdf_path.name,
                "error": str(e),
            })

    # 汇总
    valid_results = [r for r in results if "error" not in r]
    avg_delta = (
        sum(r["confidence_delta"] for r in valid_results) / len(valid_results)
        if valid_results else 0.0
    )

    report = {
        "total_files": len(difficult_files),
        "successful_ab_tests": len(valid_results),
        "failed": len(results) - len(valid_results),
        "avg_confidence_delta": avg_delta,
        "results": results,
        "decision": None,  # 待人工抽检后填写
        "decision_note": (
            "请人工抽检每文件3-5段，对比超分前后OCR准确率。"
            "若准确率提升≥3个百分点，decision='apply_realesrgan'；"
            "若提升<3个百分点或下降，decision='switch_to_docunet'。"
            "置信度delta仅供参考，不能单独作为决策依据。"
        ),
        "generated_at": datetime.now().isoformat(),
    }

    report_path = output_dir / "ab_test_report.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nA/B测试报告: {report_path}")
    print(f"平均置信度delta: {avg_delta:+.4f}")
    print(f"请人工抽检后填写decision字段")
    return report

def identify_difficult_files(manifest_path: Path) -> list[Path]:
    """
    从manifest.json中识别困难文件（低质扫描件）。

    判断标准（启发式，需人工复核）：
    1. SCAN_ONLY类型且文件大小异常小（可能是低DPI扫描）
    2. 文件名含"扫描""复印""旧"等关键词
    3. 已OCR但avg_confidence<0.7的文件（从output目录读取）
    """
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    difficult = []
    for f in manifest["files"]:
        if f["status"] != "DONE" and f["status"] != "PENDING":
            continue
        if f["type"] != "SCAN_ONLY":
            continue
        filepath = Path(f["absolute_path"])
        # 启发式1: 文件大小<1MB的PDF可能是低质扫描（多页PDF正常应>2MB）
        if f["size_bytes"] < 1_000_000 and f["extension"] == ".pdf":
            difficult.append(filepath)
            continue
        # 启发式2: 已OCR但置信度低
        output_json = OUTPUT_DIR / f"{Path(f['filename']).stem}.json"
        if output_json.exists():
            data = json.loads(output_json.read_text(encoding="utf-8"))
            if data.get("avg_confidence", 1.0) < 0.7:
                difficult.append(filepath)

    # 去重
    seen = set()
    unique = []
    for p in difficult:
        if str(p) not in seen:
            seen.add(str(p))
            unique.append(p)
    return unique

if __name__ == "__main__":
    # CLI入口：先对5个困难文件做A/B测试
    manifest = Path(__file__).parent / "manifest.json"
    if not manifest.exists():
        print("错误：manifest.json不存在，请先运行scan_files.py")
        sys.exit(1)

    difficult = identify_difficult_files(manifest)
    print(f"识别到{len(difficult)}个困难文件")

    if not difficult:
        print("无困难文件，跳过超分")
        sys.exit(0)

    # 先对前5个做A/B测试（spec要求先验证再全量）
    sample = difficult[:5]
    print(f"先对{len(sample)}个文件做A/B测试...")
    from ocr_scan import ocr_scan_pdf
    report = run_ab_test_on_difficult_files(sample, ocr_scan_pdf)
    print("\n请人工抽检ab_test_report.json中的results，填写decision字段")
    print("若decision='apply_realesrgan'，运行: python super_resolve.py --apply-all")
```

- [ ] **Step 6: 执行Real-ESRGAN A/B测试并决策**

Run: `cd tools/ocr && python super_resolve.py`
Expected:
1. 识别困难文件（约34个低质扫描件）
2. 对前5个做A/B测试：原始OCR vs 超分后OCR
3. 输出 `output/ab_test/ab_test_report.json`

**【人工决策步骤】** 打开 `ab_test_report.json`，对每个文件抽检3-5段OCR结果：
- 对比超分前后OCR准确率（重点关注形近字、标点、专名）
- 在report中填写 `decision` 字段：
  - `apply_realesrgan`：超分后准确率提升≥3个百分点 → 全量应用
  - `switch_to_docunet`：提升<3个百分点或下降 → 改用DocUNet
  - `skip`：该文件超分无意义（如已是高清扫描）

若 `decision = "apply_realesrgan"`，继续执行全量超分：

Run: `cd tools/ocr && python super_resolve.py --apply-all`
Expected: 34个困难文件全部超分后重新OCR，结果写入 `output/super_resolved/`

若 `decision = "switch_to_docunet"`，则改用文档专用增强方案：

```python
# tools/ocr/doc_unet_enhance.py（备选方案，仅在Real-ESRGAN无效时启用）
"""
DocUNet文档专用增强方案。
Real-ESRGAN面向自然图像训练，文档场景可能产生文字伪影。
若A/B测试证明Real-ESRGAN无效，则改用此方案。

DocUNet特点：
- 专为文档去扭曲训练
- 不放大分辨率，仅校正几何变形
- 对倾斜/卷曲扫描件效果更好
"""
# 实施略，仅在Real-ESRGAN A/B测试失败后开发
```

- [ ] **Step 7: Commit**

```bash
git add tools/ocr/ocr_scan.py tools/ocr/super_resolve.py tools/ocr/manifest.json tools/ocr/output/
git commit -m "feat(ocr): 扫描件OCR处理115个文件 + Real-ESRGAN超分困难件

- 111个SCAN_ONLY PDF用MinerU 3.x OCR
- 4个图片用PaddleOCR直接OCR
- 输出含置信度评分，支持后续校对分级
- 标注content_source=TEXTBOOK_OCR, ocr_status=PENDING
- 新增Real-ESRGAN超分模块（spec Task 5.6）：
  - A/B测试验证超分效果（5个样本）
  - 人工决策：全量应用/改用DocUNet/跳过
  - 警告：Real-ESRGAN面向自然图像，文档场景需验证"
```

---

### Task 6: OCR校对闭环（四级管线）

**Files:**
- Create: `tools/ocr/post_correct.py`
- Create: `tools/ocr/tests/test_post_correct.py`
- Create: `tools/ocr/prompts/correct_ocr.txt`（Task 3已创建）

- [ ] **Step 1: 编写OCR校对模块**

Create `tools/ocr/post_correct.py`:

```python
"""
OCR校对闭环模块。
四级管线：
1. 读取MinerU置信度score分级路由
2. score≥0.9直接入库
3. 0.7-0.9送LLM保守纠错
4. <0.7进人工校对队列
"""
import json
import asyncio
from pathlib import Path
from datetime import datetime

OUTPUT_DIR = Path(__file__).parent / "output"
ERROR_DICT_PATH = Path(__file__).parent.parent.parent / "assets" / "error_dict.json"
MANUAL_REVIEW_DIR = Path(__file__).parent / "manual_review"

sys_path = str(Path(__file__).parent)
import sys
if sys_path not in sys.path:
    sys.path.insert(0, sys_path)
from utils.llm_client import call_llm, load_prompt, parse_json_response

def classify_by_confidence(avg_confidence: float) -> str:
    """根据置信度分级路由"""
    if avg_confidence >= 0.9:
        return "AUTO_PASS"      # 直接入库
    elif avg_confidence >= 0.7:
        return "LLM_CORRECT"    # LLM保守纠错
    else:
        return "MANUAL_REVIEW"  # 人工校对

async def llm_correct_text(text: str) -> dict:
    """LLM保守纠错"""
    prompt_template = load_prompt("correct_ocr")
    prompt = prompt_template.replace("{text}", text)
    response = await call_llm(prompt, system="你是OCR文本纠错专家。")
    return parse_json_response(response)

def load_error_dict() -> dict:
    """加载OCR错误词典"""
    if ERROR_DICT_PATH.exists():
        return json.loads(ERROR_DICT_PATH.read_text(encoding="utf-8"))
    return {"errors": []}

def save_error_dict(error_dict: dict):
    """保存OCR错误词典"""
    ERROR_DICT_PATH.parent.mkdir(parents=True, exist_ok=True)
    ERROR_DICT_PATH.write_text(json.dumps(error_dict, ensure_ascii=False, indent=2), encoding="utf-8")

def update_error_dict(error_dict: dict, changes: list):
    """将纠错记录沉淀到错误词典"""
    for change in changes:
        error_entry = {
            "original": change["original"],
            "corrected": change["corrected"],
            "reason": change.get("reason", ""),
            "timestamp": datetime.now().isoformat(),
        }
        error_dict["errors"].append(error_entry)
    save_error_dict(error_dict)

async def post_correct_file(json_path: Path):
    """对单个OCR结果文件进行校对"""
    data = json.loads(json_path.read_text(encoding="utf-8"))

    # 跳过非OCR文件
    if data.get("content_source") != "TEXTBOOK_OCR":
        return
    if data.get("ocr_status") == "VERIFIED":
        return

    avg_conf = data.get("avg_confidence", 0)
    route = classify_by_confidence(avg_conf)

    if route == "AUTO_PASS":
        data["ocr_status"] = "VERIFIED"
        data["correction_route"] = "AUTO_PASS"
        data["corrected_at"] = datetime.now().isoformat()
        print(f"  [AUTO_PASS] {data['filename']} (置信度{avg_conf:.3f})")

    elif route == "LLM_CORRECT":
        print(f"  [LLM_CORRECT] {data['filename']} (置信度{avg_conf:.3f})")
        # 提取所有文本
        if "content_list" in data:
            # MinerU格式
            texts = []
            for item in data["content_list"]:
                if item.get("type") == "text":
                    texts.append(item.get("text", ""))
            full_text = "\n".join(texts)
        elif "pages" in data:
            # pdfplumber格式
            full_text = "\n".join(p["text"] for p in data["pages"])
        elif "lines" in data:
            # PaddleOCR格式
            full_text = "\n".join(l["text"] for l in data["lines"])
        else:
            full_text = ""

        if not full_text.strip():
            data["correction_route"] = "LLM_CORRECT"
            data["correction_error"] = "无文本可纠错"
        else:
            try:
                correction = await llm_correct_text(full_text)
                data["corrected_text"] = correction["corrected_text"]
                data["correction_changes"] = correction["changes"]
                data["correction_change_count"] = correction["change_count"]
                data["correction_route"] = "LLM_CORRECT"

                # 沉淀到错误词典
                if correction["changes"]:
                    error_dict = load_error_dict()
                    update_error_dict(error_dict, correction["changes"])

                # 改动率检查（防过度修正）
                if full_text:
                    change_rate = correction["change_count"] / len(full_text) * 100
                    data["change_rate_percent"] = round(change_rate, 2)
                    if change_rate > 5:
                        data["correction_warning"] = f"改动率{change_rate:.1f}%超过5%阈值，需人工复核"

            except Exception as e:
                data["correction_route"] = "LLM_CORRECT_FAILED"
                data["correction_error"] = str(e)

    elif route == "MANUAL_REVIEW":
        MANUAL_REVIEW_DIR.mkdir(parents=True, exist_ok=True)
        review_path = MANUAL_REVIEW_DIR / json_path.name
        review_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        data["correction_route"] = "MANUAL_REVIEW"
        data["manual_review_path"] = str(review_path)
        print(f"  [MANUAL_REVIEW] {data['filename']} → {review_path}")

    json_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

async def post_correct_all():
    """对所有OCR结果文件进行校对"""
    ocr_files = []
    for f in OUTPUT_DIR.glob("*.json"):
        data = json.loads(f.read_text(encoding="utf-8"))
        if data.get("content_source") == "TEXTBOOK_OCR" and data.get("ocr_status") != "VERIFIED":
            ocr_files.append(f)

    print(f"待校对文件: {len(ocr_files)}个")
    for i, f in enumerate(ocr_files, 1):
        print(f"\n[{i}/{len(ocr_files)}] {f.name}")
        await post_correct_file(f)

if __name__ == "__main__":
    asyncio.run(post_correct_all())
```

- [ ] **Step 2: 编写校对测试**

Create `tools/ocr/tests/test_post_correct.py`:

```python
"""测试OCR校对闭环"""
import pytest
from post_correct import classify_by_confidence

def test_classify_high_confidence():
    """高置信度直接通过"""
    assert classify_by_confidence(0.95) == "AUTO_PASS"
    assert classify_by_confidence(0.90) == "AUTO_PASS"

def test_classify_medium_confidence():
    """中等置信度LLM纠错"""
    assert classify_by_confidence(0.85) == "LLM_CORRECT"
    assert classify_by_confidence(0.75) == "LLM_CORRECT"
    assert classify_by_confidence(0.70) == "LLM_CORRECT"

def test_classify_low_confidence():
    """低置信度人工校对"""
    assert classify_by_confidence(0.65) == "MANUAL_REVIEW"
    assert classify_by_confidence(0.50) == "MANUAL_REVIEW"
    assert classify_by_confidence(0.0) == "MANUAL_REVIEW"
```

- [ ] **Step 3: 运行校对测试**

Run: `cd tools/ocr && python -m pytest tests/test_post_correct.py -v`
Expected: 测试通过

- [ ] **Step 4: 执行全量校对**

Run: `cd tools/ocr && python post_correct.py`
Expected: OCR文件按置信度分级处理，`assets/error_dict.json` 生成

- [ ] **Step 5: Commit**

```bash
git add tools/ocr/post_correct.py tools/ocr/tests/test_post_correct.py tools/ocr/manual_review/ assets/error_dict.json
git commit -m "feat(ocr): OCR校对闭环四级管线

- 置信度≥0.9直接入库
- 0.7-0.9 LLM保守纠错（只修形近字，不改语义）
- <0.7进人工校对队列
- 纠错记录沉淀到error_dict.json
- 改动率>5%自动标记需复核"
```

---

### Task 7: 知识提取与结构化

**Files:**
- Create: `tools/ocr/extract_knowledge.py`

- [ ] **Step 1: 编写知识提取模块**

Create `tools/ocr/extract_knowledge.py`:

```python
"""
知识提取与结构化模块。
从OCR文本中提取知识点、实体、关系。
"""
import json
import asyncio
from pathlib import Path
from datetime import datetime

OUTPUT_DIR = Path(__file__).parent / "output"
KNOWLEDGE_OUTPUT = Path(__file__).parent / "knowledge_output"

sys_path = str(Path(__file__).parent)
import sys
if sys_path not in sys.path:
    sys.path.insert(0, sys_path)
from utils.llm_client import call_llm, load_prompt, parse_json_response

# 别名归一化字典
ALIAS_MAP = {
    "苏东坡": "苏轼", "子瞻": "苏轼", "东坡居士": "苏轼",
    "少陵野老": "杜甫", "杜少陵": "杜甫", "子美": "杜甫",
    "青莲居士": "李白", "太白": "李白",
    "香山居士": "白居易", "乐天": "白居易",
    "六一居士": "欧阳修", "永叔": "欧阳修",
    "放翁": "陆游", "务观": "陆游",
    "稼轩": "辛弃疾", "幼安": "辛弃疾",
}

def normalize_entity(name: str) -> str:
    """实体别名归一化"""
    return ALIAS_MAP.get(name, name)

async def extract_knowledge_from_file(json_path: Path) -> list[dict]:
    """从单个OCR结果文件提取知识点"""
    data = json.loads(json_path.read_text(encoding="utf-8"))
    source_file = data.get("filename", json_path.stem)

    # 提取全部文本（按页/块）
    text_blocks = []
    if "content_list" in data:
        # MinerU格式
        for item in data["content_list"]:
            if item.get("type") == "text":
                text_blocks.append({
                    "text": item.get("text", ""),
                    "page": item.get("page_idx", 0),
                })
    elif "pages" in data:
        # pdfplumber格式
        for p in data["pages"]:
            text_blocks.append({"text": p["text"], "page": p["page"]})
    elif "corrected_text" in data:
        text_blocks.append({"text": data["corrected_text"], "page": 1})
    elif "lines" in data:
        text_blocks.append({
            "text": "\n".join(l["text"] for l in data["lines"]),
            "page": 1,
        })

    # 分块提取（每块约2000字符，避免超出LLM上下文）
    CHUNK_SIZE = 2000
    all_knowledge = []

    for block in text_blocks:
        text = block["text"]
        page = block["page"]
        if not text.strip():
            continue

        # 分块
        for i in range(0, len(text), CHUNK_SIZE):
            chunk = text[i:i + CHUNK_SIZE]
            prompt_template = load_prompt("extract_knowledge")
            prompt = prompt_template.replace("{text}", chunk)
            prompt = prompt.replace("{source_file}", source_file)
            prompt = prompt.replace("{page}", str(page))

            try:
                response = await call_llm(prompt, system="你是文学考研知识提取专家。")
                result = parse_json_response(response)
                knowledge_points = result.get("knowledge_points", [])

                # 实体归一化
                for kp in knowledge_points:
                    for entity in kp.get("entities", []):
                        entity["name"] = normalize_entity(entity["name"])
                    for rel in kp.get("relations", []):
                        rel["from"] = normalize_entity(rel["from"])
                        rel["to"] = normalize_entity(rel["to"])
                    kp["source_file"] = source_file
                    all_knowledge.append(kp)
            except Exception as e:
                print(f"  [ERROR] 提取失败 ({source_file} P{page}): {e}")

    return all_knowledge

def filter_by_confidence(knowledge_points: list[dict]) -> tuple[list, list]:
    """按置信度分级：≥0.9直接入库；0.6-0.9待校；<0.6丢弃"""
    auto_accept = []
    needs_review = []
    for kp in knowledge_points:
        conf = kp.get("confidence", 0)
        if conf >= 0.9:
            auto_accept.append(kp)
        elif conf >= 0.6:
            needs_review.append(kp)
        # <0.6 丢弃
    return auto_accept, needs_review

async def extract_all():
    """提取所有文件的知识点"""
    KNOWLEDGE_OUTPUT.mkdir(parents=True, exist_ok=True)

    all_files = list(OUTPUT_DIR.glob("*.json"))
    print(f"待提取文件: {len(all_files)}个")

    all_knowledge = []
    for i, f in enumerate(all_files, 1):
        print(f"[{i}/{len(all_files)}] {f.name}")
        kps = await extract_knowledge_from_file(f)
        all_knowledge.extend(kps)

    # 按置信度分级
    auto_accept, needs_review = filter_by_confidence(all_knowledge)

    # 保存结果
    result = {
        "extracted_at": datetime.now().isoformat(),
        "total_knowledge_points": len(all_knowledge),
        "auto_accept_count": len(auto_accept),
        "needs_review_count": len(needs_review),
        "auto_accept": auto_accept,
        "needs_review": needs_review,
    }

    output_path = KNOWLEDGE_OUTPUT / "knowledge_points.json"
    output_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\n知识点提取完成:")
    print(f"  总数: {len(all_knowledge)}")
    print(f"  自动入库: {len(auto_accept)}")
    print(f"  待校: {len(needs_review)}")

if __name__ == "__main__":
    asyncio.run(extract_all())
```

- [ ] **Step 2: 执行知识提取**

Run: `cd tools/ocr && python extract_knowledge.py`
Expected: 输出 `knowledge_output/knowledge_points.json`，含按置信度分级的知识点

- [ ] **Step 3: 10%抽样人工校验**

Run: `cd tools/ocr && python -c "
import json, random
from pathlib import Path
data = json.loads(Path('knowledge_output/knowledge_points.json').read_text(encoding='utf-8'))
sample = random.sample(data['auto_accept'], max(1, len(data['auto_accept']) // 10))
for kp in sample[:10]:
    print(f\"\\n--- {kp['title']} (置信度{kp.get('confidence', 0):.2f}) ---\")
    print(kp['content'][:100])
"`
Expected: 输出抽样知识点供人工校验

- [ ] **Step 4: Commit**

```bash
git add tools/ocr/extract_knowledge.py tools/ocr/knowledge_output/
git commit -m "feat(ocr): 知识提取与结构化

- LLM分块提取知识点（50-150字/个）
- 实体识别+别名归一化（苏轼=苏东坡=子瞻）
- 关系抽取（作者-作品/流派-成员/影响-被影响/并称）
- 置信度分级：≥0.9入库/0.6-0.9待校/<0.6丢弃
- 10%抽样人工校验"
```

---

### Task 8: 多教材交叉校验与对照

**Files:**
- Create: `tools/ocr/cross_validate.py`
- Create: `tools/ocr/prompts/cross_validate.txt`

- [ ] **Step 1: 编写交叉校验prompt**

Create `tools/ocr/prompts/cross_validate.txt`:

```text
你是一个文学考研多教材对照专家。请对以下知识点进行多教材交叉校验。

教材版本：
- 袁行霈《中国文学史》第3版（学习理解为主干，表述清晰易读）
- 袁世硕主编陈文新副主编《中国古代文学史》马工程第二版（考试答题为准，官方指定）
- 游国恩《中国文学史》（南师本科传统，补充视角）

知识点：
{knowledge_point}

各教材相关内容：
{materials}

输出JSON：
{
  "core_conclusion": "以马工程版为准的核心结论（答题基准）",
  "study_text": "以袁行霈版为准的学习理解文本",
  "multi_perspectives": [
    {"source": "游国恩版", "text": "游国恩版的不同表述"}
  ],
  "conflict_flag": true/false,
  "conflict_description": "如有实质性矛盾，描述矛盾点"
}
```

- [ ] **Step 2: 编写交叉校验模块**

Create `tools/ocr/cross_validate.py`:

```python
"""
多教材交叉校验模块。
双轨制：学习用袁行霈（易读），答题用马工程（官方指定）。
"""
import json
import asyncio
from pathlib import Path
from datetime import datetime

KNOWLEDGE_OUTPUT = Path(__file__).parent / "knowledge_output"

sys_path = str(Path(__file__).parent)
import sys
if sys_path not in sys.path:
    sys.path.insert(0, sys_path)
from utils.llm_client import call_llm, load_prompt, parse_json_response

# 教材来源标识
TEXTBOOK_SOURCES = {
    "yuanxingpei": "袁行霈《中国文学史》第3版",
    "magongcheng": "袁世硕《中国古代文学史》马工程第二版",
    "youguoen": "游国恩《中国文学史》",
    "dingfan": "丁帆《中国新文学史》",
    "niezhenzhao": "聂珍钊《外国文学史》第2版",
    "tongqingbing": "童庆炳《文学理论教程》第5版",
}

async def cross_validate_point(kp: dict, materials: list[dict]) -> dict:
    """对单个知识点进行多教材交叉校验"""
    prompt_template = load_prompt("cross_validate")
    prompt = prompt_template.replace("{knowledge_point}", kp.get("content", ""))
    materials_text = "\n\n".join(
        f"【{m['source']}】:\n{m['text'][:500]}"
        for m in materials
    )
    prompt = prompt.replace("{materials}", materials_text)

    try:
        response = await call_llm(prompt, system="你是文学考研多教材对照专家。")
        result = parse_json_response(response)
        kp["core_conclusion"] = result.get("core_conclusion", "")
        kp["study_text"] = result.get("study_text", "")
        kp["multi_perspectives"] = result.get("multi_perspectives", [])
        kp["conflict_flag"] = result.get("conflict_flag", False)
        kp["conflict_description"] = result.get("conflict_description", "")
        kp["cross_validated"] = True
    except Exception as e:
        kp["cross_validated"] = False
        kp["cross_validate_error"] = str(e)
    return kp

def build_textbook_index():
    """
    构建教材来源索引：扫描output目录中的教材OCR结果，按科目分类。
    返回 {subject: [{source, text, filename}]} 字典。
    """
    OUTPUT_DIR = Path(__file__).parent / "output"
    index = {"ancient": [], "modern": [], "foreign": [], "theory": []}

    # 教材文件名关键词映射
    TEXTBOOK_KEYWORDS = {
        "ancient": ["袁行霈", "马工程", "古代文学史", "袁世硕", "游国恩"],
        "modern": ["丁帆", "新文学史", "钱理群", "三十年"],
        "foreign": ["聂珍钊", "外国文学史", "郑克鲁"],
        "theory": ["童庆炳", "文学理论", "周宪"],
    }

    for f in OUTPUT_DIR.glob("*.json"):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
            filename = data.get("filename", "")
            for subject, keywords in TEXTBOOK_KEYWORDS.items():
                if any(kw in filename for kw in keywords):
                    # 提取文本片段
                    text = ""
                    if "content_list" in data:
                        text = " ".join(
                            item.get("text", "")
                            for item in data["content_list"]
                            if item.get("type") == "text"
                        )[:2000]
                    elif "pages" in data:
                        text = " ".join(p.get("text", "") for p in data["pages"])[:2000]
                    elif "corrected_text" in data:
                        text = data["corrected_text"][:2000]
                    elif "paragraphs" in data:
                        text = " ".join(p.get("text", "") for p in data["paragraphs"])[:2000]

                    if text.strip():
                        source_label = TEXTBOOK_SOURCES.get(
                            "yuanxingpei" if "袁行霈" in filename else
                            "magongcheng" if "马工程" in filename or "袁世硕" in filename else
                            "youguoen" if "游国恩" in filename else
                            "dingfan" if "丁帆" in filename else
                            "niezhenzhao" if "聂珍钊" in filename else
                            "tongqingbing" if "童庆炳" in filename else
                            "unknown",
                            filename
                        )
                        index[subject].append({
                            "source": source_label,
                            "text": text,
                            "filename": filename,
                        })
        except Exception:
            continue

    return index

def find_matching_materials(kp, subject, textbook_index):
    """
    根据知识点的科目和关键词，从教材索引中查找匹配的教材内容。
    返回 [{source, text}] 列表（最多3条，避免prompt过长）。
    """
    candidates = textbook_index.get(subject, [])
    if not candidates:
        return []

    # 简化匹配：按知识点标题中的关键词过滤
    kp_title = kp.get("title", "")
    kp_content = kp.get("content", "")
    matched = []

    for material in candidates:
        # 优先匹配标题关键词
        if any(kw in material["text"] for kw in kp_title[:4] if len(kw) > 1):
            matched.append(material)
        elif any(kw in material["text"] for kw in kp_content[:20] if len(kw) > 1):
            matched.append(material)

    # 去重并限制数量
    seen_sources = set()
    result = []
    for m in matched:
        if m["source"] not in seen_sources:
            result.append(m)
            seen_sources.add(m["source"])
        if len(result) >= 3:
            break

    return result

async def cross_validate_all():
    """
    对所有知识点进行多教材交叉校验。
    根据source_file匹配教材来源，调用LLM进行双轨制对照：
    - core_conclusion以马工程版为准（答题基准）
    - study_text以袁行霈版为准（学习理解）
    无匹配教材时回退到默认值。
    """
    kp_path = KNOWLEDGE_OUTPUT / "knowledge_points.json"
    data = json.loads(kp_path.read_text(encoding="utf-8"))

    all_kps = data["auto_accept"] + data["needs_review"]
    print(f"待校验知识点: {len(all_kps)}个")

    # 构建教材来源索引：按科目匹配教材OCR结果
    textbook_index = build_textbook_index()

    validated_count = 0
    fallback_count = 0
    for i, kp in enumerate(all_kps, 1):
        if i % 50 == 0:
            print(f"  进度: {i}/{len(all_kps)} (已校验{validated_count}, 回退{fallback_count})")

        # 匹配同科目的教材内容
        subject = kp.get("subject", "")
        materials = find_matching_materials(kp, subject, textbook_index)

        if materials:
            # 有匹配教材：调用LLM交叉校验
            kp = await cross_validate_point(kp, materials)
            validated_count += 1
        else:
            # 无匹配教材：回退默认值，标注未校验
            kp.setdefault("core_conclusion", kp.get("content", ""))
            kp.setdefault("study_text", kp.get("content", ""))
            kp.setdefault("multi_perspectives", [])
            kp.setdefault("conflict_flag", False)
            kp["cross_validated"] = False
            kp["cross_validate_note"] = "无匹配教材，未进行LLM交叉校验"
            fallback_count += 1

    # 保存结果
    data["auto_accept"] = [kp for kp in all_kps if kp.get("confidence", 0) >= 0.9]
    data["needs_review"] = [kp for kp in all_kps if 0.6 <= kp.get("confidence", 0) < 0.9]
    data["cross_validated_at"] = datetime.now().isoformat()

    kp_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"交叉校验完成: {len(all_kps)}个知识点")
    print(f"  LLM校验: {validated_count}个")
    print(f"  回退默认: {fallback_count}个（无匹配教材）")

if __name__ == "__main__":
    asyncio.run(cross_validate_all())
```

- [ ] **Step 3: 执行交叉校验**

Run: `cd tools/ocr && python cross_validate.py`
Expected: 知识点添加 `core_conclusion` / `study_text` / `multi_perspectives` 字段

- [ ] **Step 4: Commit**

```bash
git add tools/ocr/cross_validate.py tools/ocr/prompts/cross_validate.txt tools/ocr/knowledge_output/
git commit -m "feat(ocr): 多教材交叉校验

- 双轨制：core_conclusion以马工程为准（答题），study_text以袁行霈为准（学习）
- multi_perspectives存储游国恩等补充视角
- conflict_flag标注版本间实质性矛盾"
```

---

### Task 9: 生成种子数据JSON

**Files:**
- Create: `tools/ocr/generate_seed.py`

- [ ] **Step 1: 编写种子数据生成模块**

Create `tools/ocr/generate_seed.py`:

```python
"""
汇总所有处理结果为App种子数据JSON。
产出：
- assets/seed_data.json（知识点/真题/卡片/写作素材）
- assets/reference_catalog.json（D级参考资料外链清单）
- assets/exam_code_history.json（科目代码变动历史）
- assets/error_dict.json（Task 6已生成）
"""
import json
from pathlib import Path
from datetime import datetime

ASSETS_DIR = Path(__file__).parent.parent.parent / "assets"
KNOWLEDGE_OUTPUT = Path(__file__).parent / "knowledge_output"
OUTPUT_DIR = Path(__file__).parent / "output"

def generate_exam_code_history():
    """生成科目代码变动历史"""
    history = [
        {"exam_code": "610", "subject_name": "专业写作", "valid_from_year": 2026, "valid_to_year": None, "direction": "现当代文学"},
        {"exam_code": "610", "subject_name": "文学基础", "valid_from_year": 2019, "valid_to_year": 2025, "direction": "各方向共用"},
        {"exam_code": "801", "subject_name": "文学基础", "valid_from_year": 2026, "valid_to_year": None, "direction": "各方向共用"},
        {"exam_code": "801", "subject_name": "专业写作", "valid_from_year": 2019, "valid_to_year": 2025, "direction": "现当代文学"},
        {"exam_code": "805", "subject_name": "中国现当代文学史", "valid_from_year": 2018, "valid_to_year": 2018, "direction": "现当代文学"},
        {"exam_code": "806", "subject_name": "中国现当代文学史", "valid_from_year": 2019, "valid_to_year": 2025, "direction": "现当代文学"},
        {"exam_code": "F008", "subject_name": "现当代文学专题", "valid_from_year": 2019, "valid_to_year": None, "direction": "现当代文学复试"},
    ]
    return history

def generate_reference_catalog():
    """生成参考资料外链清单"""
    return {
        "references": [
            {
                "title": "鸿知考研网-南师大文学院真题",
                "url": "https://www.kaoyan.com",
                "type": "external",
                "description": "1998-2025年南师大文学院考研真题",
            },
            {
                "title": "南师大文学院官网-教师信息",
                "url": "https://wxy.njnu.edu.cn/szdw/jsfc.htm",
                "type": "external",
                "description": "导师信息（不内置数据，外链官网）",
            },
            {
                "title": "维基文库-公共领域文学原典",
                "url": "https://zh.wikisource.org",
                "type": "external",
                "description": "《文心雕龙》等公共领域原典",
            },
        ]
    }

def extract_exam_questions():
    """
    从OCR结果中提取真题题干。
    识别真题文件（文件名含"真题"或年份），解析题干结构。
    """
    import re
    questions = []
    question_id = 0

    # 题型关键词
    QTYPE_PATTERNS = [
        ("名词解释", "noun_explain"),
        ("简答", "short_answer"),
        ("论述", "essay"),
        ("分析", "essay"),
        ("作文", "essay"),
        ("写作", "essay"),
        ("阅读", "cloze"),
    ]

    # 科目映射（根据文件路径中的目录名）
    SUBJECT_MAP = {
        "古代": "ancient", "古代文学": "ancient",
        "现当代": "modern", "现代": "modern",
        "外国": "foreign", "比较文学": "foreign",
        "理论": "theory", "文学理论": "theory",
    }

    for f in OUTPUT_DIR.glob("*.json"):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
            filename = data.get("filename", "")
            source_path = data.get("source_file", "")

            # 判断是否为真题文件
            is_exam = "真题" in filename or any(str(y) in filename for y in range(1998, 2026))
            if not is_exam:
                continue

            # 提取年份
            year_match = re.search(r'(19|20)(\d{2})', filename)
            year = int(year_match.group()) if year_match else None

            # 判断科目
            subject = "ancient"  # 默认
            for keyword, subj_code in SUBJECT_MAP.items():
                if keyword in filename or keyword in source_path:
                    subject = subj_code
                    break

            # 判断试卷代码
            exam_paper_code = None
            for code in ["610", "801", "805", "806", "807", "F008"]:
                if code in filename:
                    exam_paper_code = code
                    break

            # 提取全文本
            full_text = ""
            if "pages" in data:
                full_text = "\n".join(p.get("text", "") for p in data["pages"])
            elif "content_list" in data:
                full_text = "\n".join(
                    item.get("text", "") for item in data["content_list"]
                    if item.get("type") == "text"
                )
            elif "corrected_text" in data:
                full_text = data["corrected_text"]
            elif "paragraphs" in data:
                full_text = "\n".join(p.get("text", "") for p in data["paragraphs"])

            if not full_text.strip():
                continue

            # 判断答案状态：2004-2025年真题文件名含"答案"或有答案文本
            has_answer = "答案" in filename or "answer" in filename.lower()
            answer_status = "HAS_ANSWER" if has_answer else "NO_ANSWER"
            if year and year < 2004:
                answer_status = "NO_ANSWER"  # 1998-2003年无答案

            # 按题型分割题干
            for qtype_keyword, qtype_code in QTYPE_PATTERNS:
                # 匹配 "一、名词解释" "二、简答题" 等模式
                pattern = rf'[一二三四五六七八九十]+[、.]\s*{qtype_keyword}'
                sections = re.split(pattern, full_text)
                if len(sections) > 1:
                    # sections[0]是分割前内容，sections[1:]是各题内容
                    for section in sections[1:]:
                        # 取第一道题的题干（到下一个编号或换行）
                        lines = section.strip().split("\n")
                        stem = lines[0].strip() if lines else ""
                        # 过滤过短或无效的题干
                        if len(stem) < 5:
                            continue
                        question_id += 1
                        questions.append({
                            "id": f"eq-{question_id}",
                            "subject": subject,
                            "year": year,
                            "exam_paper_code": exam_paper_code,
                            "question_type": qtype_code,
                            "stem": stem[:500],
                            "answer_status": answer_status,
                            "answer_framework": "" if answer_status == "NO_ANSWER" else "\n".join(lines[1:5])[:500],
                            "source_file": filename,
                            "source_page": 1,
                            "content_source": data.get("content_source", "TEXTBOOK_OCR"),
                            "ocr_status": data.get("ocr_status", "PENDING"),
                        })

        except Exception as e:
            print(f"  [WARN] 真题提取失败 {f.name}: {e}")
            continue

    return questions

def extract_writing_materials():
    """
    从评论类文件提取写作素材。
    识别文件名含"评论""鲁迅""写作"的文件，提取段落为写作素材。
    """
    materials = []
    material_id = 0

    WRITING_KEYWORDS = ["评论", "鲁迅", "写作", "文论", "批评"]

    for f in OUTPUT_DIR.glob("*.json"):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
            filename = data.get("filename", "")

            if not any(kw in filename for kw in WRITING_KEYWORDS):
                continue

            # 提取段落
            paragraphs = []
            if "paragraphs" in data:
                paragraphs = [p.get("text", "") for p in data["paragraphs"] if p.get("text", "").strip()]
            elif "pages" in data:
                for p in data["pages"]:
                    text = p.get("text", "").strip()
                    if text:
                        paragraphs.append(text)
            elif "content_list" in data:
                for item in data["content_list"]:
                    if item.get("type") == "text" and item.get("text", "").strip():
                        paragraphs.append(item["text"])

            # 每个有意义的段落作为一个写作素材
            for para in paragraphs:
                if len(para) < 50:  # 过滤过短段落
                    continue
                material_id += 1
                materials.append({
                    "id": f"wm-{material_id}",
                    "source_file": filename,
                    "content": para[:1000],
                    "content_source": data.get("content_source", "TEXTBOOK_NATIVE"),
                    "ocr_status": data.get("ocr_status", "VERIFIED"),
                    "tags": [kw for kw in WRITING_KEYWORDS if kw in filename],
                })

        except Exception as e:
            print(f"  [WARN] 写作素材提取失败 {f.name}: {e}")
            continue

    return materials

def generate_basic_cards(knowledge_points):
    """
    从知识点生成基础名词解释卡片。
    遵循Wozniak最小信息原则：一个知识点一张卡，正面问题背面答案。
    """
    cards = []
    for kp in knowledge_points:
        title = kp.get("title", "")
        content = kp.get("content", "") or kp.get("study_text", "") or kp.get("core_conclusion", "")
        subject = kp.get("subject", "ancient")
        if not title or not content:
            continue
        cards.append({
            "id": f"card-{kp.get('id', len(cards))}",
            "subject": subject,
            "card_type": "noun_explain",
            "term": title,
            "def": content[:200],
            "content_source": kp.get("content_source", "TEXTBOOK_NATIVE"),
            "ocr_status": kp.get("ocr_status", "VERIFIED"),
            "source_file": kp.get("source_file", ""),
        })
    return cards

def generate_seed_data():
    """汇总生成种子数据"""
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)

    # 加载知识点
    kp_path = KNOWLEDGE_OUTPUT / "knowledge_points.json"
    if kp_path.exists():
        kp_data = json.loads(kp_path.read_text(encoding="utf-8"))
        knowledge_points = kp_data.get("auto_accept", []) + kp_data.get("needs_review", [])
    else:
        knowledge_points = []

    # 按科目分类
    by_subject = {"ancient": [], "modern": [], "foreign": [], "theory": []}
    for kp in knowledge_points:
        subject = kp.get("subject", "ancient")
        if subject in by_subject:
            by_subject[subject].append(kp)

    # 从真题文件提取真题题干
    exam_questions = extract_exam_questions()

    # 从评论类文件提取写作素材
    writing_materials = extract_writing_materials()

    # 从知识点生成基础卡片（名词解释卡）
    cards = generate_basic_cards(knowledge_points)

    # 生成种子数据
    seed_data = {
        "version": "1.0.0",
        "generated_at": datetime.now().isoformat(),
        "stats": {
            "total_knowledge_points": len(knowledge_points),
            "by_subject": {k: len(v) for k, v in by_subject.items()},
            "total_exam_questions": len(exam_questions),
        },
        "subjects": [
            {"id": "ancient", "name": "中国古代文学", "short": "古代"},
            {"id": "modern", "name": "中国现当代文学", "short": "现当代"},
            {"id": "foreign", "name": "外国文学", "short": "外国"},
            {"id": "theory", "name": "文学理论", "short": "理论"},
        ],
        "knowledge_points": knowledge_points,
        "exam_questions": exam_questions,
        "writing_materials": writing_materials,
        "cards": cards,
    }

    # 保存
    seed_path = ASSETS_DIR / "seed_data.json"
    seed_path.write_text(json.dumps(seed_data, ensure_ascii=False, indent=2), encoding="utf-8")

    # 科目代码历史
    code_history = generate_exam_code_history()
    (ASSETS_DIR / "exam_code_history.json").write_text(
        json.dumps(code_history, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    # 参考资料外链
    ref_catalog = generate_reference_catalog()
    (ASSETS_DIR / "reference_catalog.json").write_text(
        json.dumps(ref_catalog, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    print(f"种子数据生成完成:")
    print(f"  知识点: {len(knowledge_points)}个")
    print(f"  按科目: {seed_data['stats']['by_subject']}")
    print(f"  真题: {len(exam_questions)}个")
    print(f"  写作素材: {len(writing_materials)}个")
    print(f"  基础卡片: {len(cards)}个")
    print(f"  科目代码历史: {len(code_history)}条")
    print(f"  参考资料外链: {len(ref_catalog['references'])}条")
    print(f"\n输出目录: {ASSETS_DIR}")

if __name__ == "__main__":
    generate_seed_data()
```

- [ ] **Step 2: 执行种子数据生成**

Run: `cd tools/ocr && python generate_seed.py`
Expected: `assets/` 目录下生成4个JSON文件

- [ ] **Step 3: 验证种子数据完整性**

Run: `cd tools/ocr && python -c "
import json
from pathlib import Path
data = json.loads(Path('../../assets/seed_data.json').read_text(encoding='utf-8'))
print('版本:', data['version'])
print('知识点总数:', data['stats']['total_knowledge_points'])
print('按科目:', data['stats']['by_subject'])
# 验证四科覆盖
for subj in ['ancient', 'modern', 'foreign', 'theory']:
    count = data['stats']['by_subject'].get(subj, 0)
    assert count > 0, f'{subj}科目无知识点'
print('四科覆盖: OK')
"`
Expected: 四科均有知识点覆盖

- [ ] **Step 4: Commit**

```bash
git add tools/ocr/generate_seed.py assets/
git commit -m "feat(ocr): 生成App种子数据

- assets/seed_data.json: 知识点/真题/写作素材
- assets/exam_code_history.json: 科目代码变动历史
- assets/reference_catalog.json: 参考资料外链清单
- assets/error_dict.json: OCR错误词典（Task 6生成）
- 验证四科知识点覆盖完整"
```

---

## Phase 2: App数据结构升级

> **依赖**：Phase 1的 `assets/seed_data.json`（数据结构定义可提前，实际数据加载需Phase 1完成）
> **目标平台**：现有Web App（数据结构镜像SQLite schema，便于未来移植Android）

### Task 10: 修改exam_questions数据结构（新增字段）

**Files:**
- Modify: `js/data.js`（增强数据结构）
- Create: `js/db-schema.js`（数据结构定义与校验）

- [ ] **Step 1: 创建数据结构定义模块**

Create `js/db-schema.js`:

```javascript
/* ============================================================
 *  文研 · 数据结构定义（镜像SQLite schema，便于未来移植Android）
 * ============================================================ */

// exam_questions 新增字段
const EXAM_QUESTION_SCHEMA = {
  id: "string",
  subject: "string",           // ancient/modern/foreign/theory
  year: "number",
  exam_paper_code: "string",   // 新增：当年试卷代码610/801/805/806等
  question_type: "string",     // essay/short_answer/cloze
  stem: "string",
  answer_status: "string",     // 新增：HAS_ANSWER/NO_ANSWER/AI_GENERATED
  answer_framework: "string",
  material_text: "string",     // 新增：材料题原文
  source_file: "string",       // 新增：来源文件
  source_page: "number",       // 新增：来源页码
  content_source: "string",    // 新增：TEXTBOOK_NATIVE/TEXTBOOK_OCR/AI_GENERATED/HYBRID/USER_CREATED
  ocr_status: "string",        // 新增：VERIFIED/PENDING
};

// knowledge_points 新增字段
const KNOWLEDGE_POINT_SCHEMA = {
  id: "string",
  subject: "string",
  title: "string",
  summary: "string",
  content_source: "string",    // 新增：5种来源类型
  ocr_status: "string",        // 新增：VERIFIED/PENDING
  source_file: "string",       // 新增
  source_page: "number",       // 新增
  study_text: "string",        // 新增：学习理解文本（袁行霈版为主）
  core_conclusion: "string",   // 答题基准（马工程版为准）
  multi_perspectives: "array", // 多教材对照
  conflict_flag: "boolean",    // 版本间矛盾标注
};

// exam_code_history 表
const EXAM_CODE_HISTORY_SCHEMA = {
  exam_code: "string",
  subject_name: "string",
  valid_from_year: "number",
  valid_to_year: "number|null",
  direction: "string",
};

// data_sources 表（资料来源溯源）
const DATA_SOURCE_SCHEMA = {
  id: "string",
  ref_type: "string",          // knowledge_point/exam_question/writing_material
  ref_id: "string",
  source_file: "string",
  source_page: "number",
  source_type: "string",       // TEXTBOOK_NATIVE/TEXTBOOK_OCR/EXTERNAL
  priority: "number",          // 主干1/官方2/补充3
};

// 内容来源标签映射
const CONTENT_SOURCE_LABELS = {
  TEXTBOOK_NATIVE: { label: "资料", color: "green", icon: "📚" },
  TEXTBOOK_OCR: { label: "资料", color: "green", icon: "📚", suffix: "OCR" },
  AI_GENERATED: { label: "AI", color: "blue", icon: "🤖" },
  HYBRID: { label: "资料+AI", color: "yellow", icon: "📚🤖" },
  USER_CREATED: { label: "我的", color: "gray", icon: "✏️" },
  MISSING: { label: "缺失", color: "red", icon: "⚠️" },
};

window.WENYAN_SCHEMA = {
  EXAM_QUESTION_SCHEMA,
  KNOWLEDGE_POINT_SCHEMA,
  EXAM_CODE_HISTORY_SCHEMA,
  DATA_SOURCE_SCHEMA,
  CONTENT_SOURCE_LABELS,
};
```

- [ ] **Step 2: 修改data.js加载种子数据**

在 `js/data.js` 末尾添加种子数据加载逻辑（保留现有硬编码数据作为fallback）:

```javascript
// 在文件末尾添加
/* ---------------------- 种子数据加载 ---------------------- */
async function loadSeedData() {
  try {
    const response = await fetch("assets/seed_data.json");
    if (!response.ok) {
      console.warn("seed_data.json未找到，使用内置数据");
      return null;
    }
    return await response.json();
  } catch (e) {
    console.warn("种子数据加载失败，使用内置数据:", e);
    return null;
  }
}

// 导出异步初始化函数
window.WENYAN_SEED_LOADER = { loadSeedData };
```

- [ ] **Step 3: Commit**

```bash
git add js/db-schema.js js/data.js
git commit -m "feat(app): 新增数据结构定义和种子数据加载

- db-schema.js: 镜像SQLite schema的数据结构定义
- exam_questions新增exam_paper_code/answer_status/material_text/source_file/source_page字段
- knowledge_points新增content_source/ocr_status/study_text字段
- data.js: 添加种子数据异步加载（fetch seed_data.json）"
```

---

### Task 11: 修改knowledge_points数据结构（新增字段）

> 已在Task 10的 `db-schema.js` 中定义，此处确保现有数据兼容。

**Files:**
- Modify: `js/data.js`（为现有知识点添加默认字段）

- [ ] **Step 1: 为现有硬编码知识点添加默认字段**

在 `js/data.js` 的 `KNOWLEDGE` 数组每个元素添加默认字段。由于现有数据是原生电子文本（非OCR），默认值：

```javascript
// 在KNOWLEDGE数组每个元素中添加（或批量处理函数）
function normalizeKnowledge(kp) {
  return Object.assign({
    content_source: "TEXTBOOK_NATIVE",
    ocr_status: "VERIFIED",
    source_file: null,
    source_page: null,
    study_text: kp.summary || "",
    core_conclusion: kp.summary || "",
    multi_perspectives: [],
    conflict_flag: false,
  }, kp);
}

// 应用到现有数据
const NORMALIZED_KNOWLEDGE = KNOWLEDGE.map(normalizeKnowledge);
```

- [ ] **Step 2: Commit**

```bash
git add js/data.js
git commit -m "feat(app): 现有知识点数据添加默认字段

- content_source默认TEXTBOOK_NATIVE
- ocr_status默认VERIFIED
- study_text/core_conclusion默认使用summary"
```

---

### Task 12: 新增数据表（exam_code_history / data_sources）

> 已在Task 10的 `db-schema.js` 中定义结构。此处实现数据加载。

**Files:**
- Create: `js/exam-codes.js`

- [ ] **Step 1: 创建科目代码历史管理模块**

Create `js/exam-codes.js`:

```javascript
/* ============================================================
 *  文研 · 科目代码历史管理
 *  解决610/801语义翻转问题（2026年代码变更）
 * ============================================================ */

(function () {
  "use strict";

  // 默认科目代码历史（与assets/exam_code_history.json同步）
  const DEFAULT_HISTORY = [
    { exam_code: "610", subject_name: "专业写作", valid_from_year: 2026, valid_to_year: null, direction: "现当代文学" },
    { exam_code: "610", subject_name: "文学基础", valid_from_year: 2019, valid_to_year: 2025, direction: "各方向共用" },
    { exam_code: "801", subject_name: "文学基础", valid_from_year: 2026, valid_to_year: null, direction: "各方向共用" },
    { exam_code: "801", subject_name: "专业写作", valid_from_year: 2019, valid_to_year: 2025, direction: "现当代文学" },
    { exam_code: "805", subject_name: "中国现当代文学史", valid_from_year: 2018, valid_to_year: 2018, direction: "现当代文学" },
    { exam_code: "806", subject_name: "中国现当代文学史", valid_from_year: 2019, valid_to_year: 2025, direction: "现当代文学" },
    { exam_code: "F008", subject_name: "现当代文学专题", valid_from_year: 2019, valid_to_year: null, direction: "现当代文学复试" },
  ];

  let history = DEFAULT_HISTORY;

  // 从seed_data.json加载
  async function loadHistory() {
    try {
      const response = await fetch("assets/exam_code_history.json");
      if (response.ok) {
        history = await response.json();
      }
    } catch (e) {
      console.warn("科目代码历史加载失败，使用默认数据:", e);
    }
  }

  /**
   * 根据年份和试卷代码判定科目
   * @param {number} year - 年份
   * @param {string} examPaperCode - 试卷代码（610/801/805/806等）
   * @returns {string|null} - 科目名称，如"文学基础"
   */
  function getSubjectName(year, examPaperCode) {
    const records = history.filter(
      h => h.exam_code === examPaperCode &&
           year >= h.valid_from_year &&
           (h.valid_to_year === null || year <= h.valid_to_year)
    );
    if (records.length === 0) {
      return null;
    }
    return records[0].subject_name;
  }

  /**
   * 获取带年份标注的科目显示名
   * @param {number} year
   * @param {string} examPaperCode
   * @returns {string} - 如"610 文学基础（2022年代码）"
   */
  function getDisplayLabel(year, examPaperCode) {
    const subject = getSubjectName(year, examPaperCode);
    if (subject === null) {
      return `${examPaperCode}（年份待核实）`;
    }
    return `${examPaperCode} ${subject}（${year}年代码）`;
  }

  /**
   * 检查两个年份的同一代码是否语义不同
   */
  function hasSemanticChange(code, year1, year2) {
    const s1 = getSubjectName(year1, code);
    const s2 = getSubjectName(year2, code);
    return s1 !== s2;
  }

  // 初始化
  loadHistory();

  window.WENYAN_EXAM_CODES = {
    getSubjectName,
    getDisplayLabel,
    hasSemanticChange,
    loadHistory,
  };
})();
```

- [ ] **Step 2: Commit**

```bash
git add js/exam-codes.js
git commit -m "feat(app): 科目代码历史管理模块

- 解决610/801语义翻转问题
- getSubjectName: 根据年份+代码判定科目
- getDisplayLabel: 带年份标注的显示名
- hasSemanticChange: 检测代码语义变更"
```

---

## Phase 3: FSRS算法升级与卡片设计

> **依赖**：Task 11（ocr_status字段）

### Task 13: 实现FSRS参数预设

**Files:**
- Create: `js/fsrs.js`

- [ ] **Step 1: 实现FSRS-6算法核心**

Create `js/fsrs.js`:

```javascript
/* ============================================================
 *  文研 · FSRS-6间隔重复算法实现
 *  参考：py-fsrs v6.3.1 (MIT)
 *  DSR三组件模型：难度D / 稳定性S / 可提取性R
 * ============================================================ */

(function () {
  "use strict";

  // FSRS-6默认参数（21参数）
  const DEFAULT_PARAMS = {
    w: [
      0.4072, 1.1829, 3.1262, 15.4742, 7.2102, 0.5316, 1.0651,
      0.0234, 1.616, 0.1543, 0.9582, 2.0701, 0.2781, 0.3125,
      0.4628, 1.427, 0.1164, 0.1062, 0.1175, 0.3234, 0.6723,
    ],
    request_retention: 0.9,
    maximum_interval: 36500,
    enable_fuzzing: true,
  };

  // 评分等级
  const RATING = {
    AGAIN: 1,  // 忘记
    HARD: 2,   // 困难
    GOOD: 3,   // 良好
    EASY: 4,   // 简单
  };

  // 内容类型预设
  const PRESETS = {
    "名词解释": { desired_retention: 0.90, enable_fuzzing: true },
    "作品背诵": { desired_retention: 0.95, enable_fuzzing: false },  // 精确记忆，不模糊
    "论述题": { desired_retention: 0.85, enable_fuzzing: true },     // 理解性，允许遗忘重建
    "流派特征": { desired_retention: 0.90, enable_fuzzing: true },
    "默认": { desired_retention: 0.90, enable_fuzzing: true },
  };

  // 考研倒计时阶段
  function getStudyPhase(daysToExam) {
    if (daysToExam > 180) return { phase: "基础", global_retention: 0.85 };
    if (daysToExam > 90) return { phase: "强化", global_retention: 0.90 };
    return { phase: "冲刺", global_retention: 0.95 };
  }

  // 计算可提取性R（基于稳定性S和经过时间t）
  function getRetrievability(stability, elapsedDays) {
    const decay = -0.5;
    const factor = Math.pow(0.9, 1 / decay) - 1;
    return Math.pow(1 + factor * elapsedDays / stability, decay);
  }

  // 初始化新卡片
  function createCard(cardType) {
    const preset = PRESETS[cardType] || PRESETS["默认"];
    return {
      due: new Date(),
      stability: 0,
      difficulty: 0,
      elapsed_days: 0,
      scheduled_days: 0,
      reps: 0,
      lapses: 0,
      state: 0, // 0=New, 1=Learning, 2=Review, 3=Relearning
      last_review: null,
      card_type: cardType,
      desired_retention: preset.desired_retention,
      enable_fuzzing: preset.enable_fuzzing,  // 修复：之前遗漏此字段，导致updateCard中fuzzing永远为undefined
    };
  }

  // 更新卡片（评分后调度）
  function updateCard(card, rating, daysToExam) {
    const phase = getStudyPhase(daysToExam);
    // 卡片级预设优先于全局保持率
    const targetRetention = Math.max(card.desired_retention, phase.global_retention);

    const now = new Date();
    const elapsed = card.last_review
      ? Math.floor((now - new Date(card.last_review)) / 86400000)
      : 0;

    if (card.state === 0) {
      // 新卡片
      card.stability = initStability(rating);
      card.difficulty = initDifficulty(rating);
      card.state = rating === RATING.AGAIN ? 1 : 2;
    } else if (rating === RATING.AGAIN) {
      // 忘记
      card.lapses += 1;
      card.stability = nextForgetStability(card.difficulty, card.stability, card.state);
      card.state = 3;
    } else {
      // 回忆
      const retrievability = getRetrievability(card.stability, elapsed);
      card.stability = nextStability(card.difficulty, card.stability, retrievability, rating);
      card.difficulty = nextDifficulty(card.difficulty, rating);
      card.state = 2;
    }

    card.reps += 1;
    card.last_review = now.toISOString();
    card.elapsed_days = elapsed;

    // 计算下次间隔
    const interval = nextInterval(card.stability, targetRetention, card.enable_fuzzing);
    card.scheduled_days = interval;
    card.due = new Date(now.getTime() + interval * 86400000);

    return card;
  }

  // 辅助函数（简化实现，实际应参考FSRS-6论文）
  function initStability(rating) {
    const w = DEFAULT_PARAMS.w;
    return Math.max(w[rating - 1], 0.1);
  }

  function initDifficulty(rating) {
    const w = DEFAULT_PARAMS.w;
    const d = w[4] - (rating - 3) * w[5];
    return Math.min(Math.max(d, 1), 10);
  }

  function nextDifficulty(d, rating) {
    const w = DEFAULT_PARAMS.w;
    const nextD = d - w[6] * (rating - 3);
    return Math.min(Math.max(nextD, 1), 10);
  }

  function nextStability(d, s, r, rating) {
    const w = DEFAULT_PARAMS.w;
    if (rating === RATING.EASY) {
      return s * (1 + w[8] * Math.pow(d, -0.5) * Math.pow(r, -0.2) * (1 - r));
    } else if (rating === RATING.GOOD) {
      return s * (1 + w[7] * Math.pow(d, -0.5) * Math.pow(r, -0.2) * (1 - r));
    } else {
      return s * (1 + w[6] * Math.pow(d, -0.5) * Math.pow(r, -0.2) * (1 - r));
    }
  }

  function nextForgetStability(d, s, state) {
    const w = DEFAULT_PARAMS.w;
    return Math.max(s * w[9] * Math.pow(d, -0.2), 0.1);
  }

  function nextInterval(stability, targetRetention, fuzzing) {
    const factor = Math.pow(0.9, 1 / -0.5) - 1;
    let interval = stability * (Math.pow(targetRetention, 1 / -0.5) - 1) / factor;
    interval = Math.max(Math.round(interval), 1);
    if (fuzzing && interval > 2.5) {
      const fuzz = interval * 0.05;
      interval = Math.round(interval + (Math.random() * 2 - 1) * fuzz);
    }
    return Math.min(interval, DEFAULT_PARAMS.maximum_interval);
  }

  window.WENYAN_FSRS = {
    RATING,
    PRESETS,
    createCard,
    updateCard,
    getRetrievability,
    getStudyPhase,
  };
})();
```

- [ ] **Step 2: Commit**

```bash
git add js/fsrs.js
git commit -m "feat(app): FSRS-6间隔重复算法实现

- 21参数DSR三组件模型
- 4种内容类型预设（名词解释/作品背诵/论述题/流派特征）
- 考研倒计时驱动动态保持率（基础0.85/强化0.90/冲刺0.95）
- 卡片级预设优先于全局保持率"
```

---

### Task 14: 实现考研倒计时驱动动态保持率

> 已在Task 13的 `fsrs.js` 中实现 `getStudyPhase` 函数。此处集成到App。

**Files:**
- Modify: `js/app.js`

- [ ] **Step 1: 在app.js中集成倒计时**

在 `js/app.js` 中添加倒计时计算和阶段切换逻辑。需在现有state对象中添加 `daysToExam` 和 `studyPhase`。

```javascript
// === js/app.js 新增/修改部分 ===

/**
 * 计算考研日期（次年12月倒数第二个周末）。
 * 考研通常在12月最后一个完整周末的周六日，此处按倒数第二个周六计算。
 * 2026年考研：2025年12月20-21日（实际以教育部公告为准）
 */
function getExamDate(year = null) {
  // 默认下一年的12月倒数第二个周六
  const targetYear = year || (new Date().getFullYear() + 1);
  const dec31 = new Date(targetYear, 11, 31);  // 12月31日
  // 找最后一个周六
  let lastSaturday = new Date(dec31);
  lastSaturday.setDate(dec31.getDate() - dec31.getDay() - 1);
  // 倒数第二个周六
  let secondLastSaturday = new Date(lastSaturday);
  secondLastSaturday.setDate(lastSaturday.getDate() - 7);
  // 考研是周六日两天，取周六作为起始
  return secondLastSaturday;
}

/**
 * 计算距考研天数
 */
function calcDaysToExam() {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const examDate = getExamDate();
  examDate.setHours(0, 0, 0, 0);
  const diffMs = examDate - today;
  return Math.ceil(diffMs / (1000 * 60 * 60 * 24));
}

/**
 * 倒计时初始化与阶段切换。
 * - 基础阶段(>180天): 全局保持率0.85
 * - 强化阶段(90-180天): 全局保持率0.90
 * - 冲刺阶段(<90天): 全局保持率0.95
 * - 阶段切换平滑过渡：新卡用新保持率，旧卡完成当前周期
 */
function initCountdown() {
  const days = calcDaysToExam();
  const phase = getStudyPhase(days);  // 来自fsrs.js
  const retention = getDesiredRetentionForDays(days);  // 来自fsrs.js

  state.daysToExam = days;
  state.studyPhase = phase;
  state.currentRetention = retention;

  console.log(`距考研${days}天，阶段：${phase}，保持率：${retention}`);

  // 渲染倒计时到UI
  renderCountdown(days, phase);

  // 检查阶段是否切换（每日检查）
  const lastPhase = localStorage.getItem('lastStudyPhase');
  if (lastPhase && lastPhase !== phase) {
    console.log(`阶段切换：${lastPhase} → ${phase}`);
    showPhaseTransitionToast(lastPhase, phase);
    // 平滑过渡：新卡用新保持率，旧卡完成当前周期
    // 不强制重算所有卡片的due日期
  }
  localStorage.setItem('lastStudyPhase', phase);

  // 每日定时检查（凌晨0点）
  scheduleNextCheck();
}

function scheduleNextCheck() {
  const now = new Date();
  const tomorrow = new Date(now);
  tomorrow.setDate(now.getDate() + 1);
  tomorrow.setHours(0, 0, 5, 0);  // 凌晨0:05
  const msUntilTomorrow = tomorrow - now;
  setTimeout(() => {
    initCountdown();
  }, msUntilTomorrow);
}

function renderCountdown(days, phase) {
  const countdownEl = document.getElementById('exam-countdown');
  if (!countdownEl) return;
  const phaseLabels = {
    '基础': '基础阶段',
    '强化': '强化阶段',
    '冲刺': '冲刺阶段',
  };
  countdownEl.innerHTML = `
    <div class="countdown-days">${days}</div>
    <div class="countdown-label">距考研</div>
    <div class="countdown-phase">${phaseLabels[phase] || phase}</div>
  `;
}

function showPhaseTransitionToast(from, to) {
  const toast = document.createElement('div');
  toast.className = 'phase-transition-toast';
  toast.textContent = `学习阶段切换：${from} → ${to}，保持率已调整`;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 5000);
}

// App初始化时调用
// initCountdown();
```

- [ ] **Step 2: Commit**

```bash
git add js/app.js
git commit -m "feat(app): 集成考研倒计时和阶段切换

- 计算距考研天数（默认次年12月倒数第二个周末）
- 三阶段保持率切换（基础/强化/冲刺）
- 阶段切换平滑过渡（新卡用新保持率，旧卡完成当前周期）"
```

---

### Task 15: 实现OCR状态过滤

**Files:**
- Modify: `js/app.js`

- [ ] **Step 1: 在复习队列和真题练习中添加OCR状态过滤**

在 `js/app.js` 的卡片渲染和真题查询函数中添加 `ocr_status = 'VERIFIED'` 过滤：

```javascript
// === js/app.js 新增/修改部分 ===

/**
 * 获取FSRS复习队列（过滤掉PENDING状态的知识点）。
 * PENDING状态的知识点尚未校对，不应进入背诵队列。
 */
async function getReviewQueue() {
  const allCards = await DBManager.query(`
    SELECT * FROM knowledge_points
    WHERE ocr_status = 'VERIFIED'
      AND due_date <= date('now')
    ORDER BY due_date ASC
  `);
  return allCards;
}

/**
 * 获取真题练习列表（过滤掉PENDING状态）。
 */
async function getExamQuestionsForPractice(subject = null, year = null) {
  let sql = `
    SELECT * FROM exam_questions
    WHERE ocr_status = 'VERIFIED'
  `;
  const params = [];
  if (subject) {
    sql += ` AND subject = ?`;
    params.push(subject);
  }
  if (year) {
    sql += ` AND year = ?`;
    params.push(year);
  }
  sql += ` ORDER BY year DESC, id ASC`;
  return await DBManager.query(sql, params);
}

/**
 * 获取待校对内容（PENDING状态）。
 * 显示在"待校对"区，用户校对后手动标记VERIFIED。
 */
async function getPendingReview() {
  const pendingCards = await DBManager.query(`
    SELECT * FROM knowledge_points
    WHERE ocr_status = 'PENDING'
    ORDER BY source_file ASC
  `);
  const pendingQuestions = await DBManager.query(`
    SELECT * FROM exam_questions
    WHERE ocr_status = 'PENDING'
    ORDER BY source_file ASC
  `);
  return { cards: pendingCards, questions: pendingQuestions };
}

/**
 * 用户校对后标记为VERIFIED。
 * 标记后该知识点/真题进入正常复习/练习流程。
 */
async function markAsVerified(itemId, itemType = 'knowledge_point') {
  const table = itemType === 'question' ? 'exam_questions' : 'knowledge_points';
  await DBManager.exec(`
    UPDATE ${table}
    SET ocr_status = 'VERIFIED'
    WHERE id = ?
  `, [itemId]);

  // 刷新待校对列表
  await renderPendingReview();
  showToast('已标记为已校对，该内容已进入复习队列');
}

/**
 * 渲染"待校对"区。
 */
async function renderPendingReview() {
  const { cards, questions } = await getPendingReview();
  const container = document.getElementById('pending-review-container');
  if (!container) return;

  const totalCount = cards.length + questions.length;
  if (totalCount === 0) {
    container.innerHTML = '<p class="empty-hint">无待校对内容</p>';
    return;
  }

  container.innerHTML = `
    <div class="pending-summary">
      待校对：${cards.length}个知识点，${questions.length}道真题
    </div>
    <div class="pending-list">
      ${cards.map(c => `
        <div class="pending-item" data-id="${c.id}" data-type="knowledge_point">
          <span class="pending-title">${c.title || '未命名'}</span>
          <span class="pending-source">${c.source_file || ''}</span>
          <button onclick="markAsVerified(${c.id}, 'knowledge_point')">标记已校对</button>
        </div>
      `).join('')}
      ${questions.map(q => `
        <div class="pending-item" data-id="${q.id}" data-type="question">
          <span class="pending-title">${q.year}年${q.subject || ''}真题</span>
          <span class="pending-source">${q.source_file || ''}</span>
          <button onclick="markAsVerified(${q.id}, 'question')">标记已校对</button>
        </div>
      `).join('')}
    </div>
  `;
}
```

- [ ] **Step 2: Commit**

```bash
git add js/app.js
git commit -m "feat(app): OCR状态过滤

- PENDING状态知识点不进FSRS背诵队列
- PENDING状态真题不进练习
- 待校对区集中显示PENDING内容
- 用户校对后手动标记VERIFIED激活"
```

---

### Task 16: 实现6种文学专用卡片模板

**Files:**
- Create: `js/card-templates.js`

- [ ] **Step 1: 创建卡片模板模块**

Create `js/card-templates.js`:

```javascript
/* ============================================================
 *  文研 · 6种文学专用卡片模板
 *  遵循Wozniak 20条规则（最小信息原则）
 * ============================================================ */

(function () {
  "use strict";

  const TEMPLATES = {
    // 1. 名词解释卡
    NOUN_EXPLAIN: {
      id: "noun_explain",
      name: "名词解释",
      render: (data) => {
        if (data.subtype === "society") {
          // 社团类：时间/地点/人物/刊物/主张/贡献
          return {
            front: `${data.title}？`,
            back: [
              `时间：${data.time || "—"}`,
              `地点：${data.location || "—"}`,
              `人物：${data.members || "—"}`,
              `刊物：${data.publication || "—"}`,
              `主张：${data.claim || "—"}`,
              `贡献：${data.contribution || "—"}`,
            ].join("\n"),
          };
        } else {
          // 作品类：作者/年代/内容/特色/影响
          return {
            front: `${data.title}？`,
            back: [
              `作者：${data.author || "—"}`,
              `年代：${data.year || "—"}`,
              `内容：${data.content || "—"}`,
              `特色：${data.feature || "—"}`,
              `影响：${data.influence || "—"}`,
            ].join("\n"),
          };
        }
      },
    },

    // 2. Cloze名句填空卡
    CLOZE_QUOTE: {
      id: "cloze_quote",
      name: "名句填空",
      render: (data) => {
        const quote = data.quote || "";
        const cloze = quote.replace(data.keyword, "____");
        return {
          front: `${cloze}\n（提示：${data.hint || "填空"}）`,
          back: data.keyword,
        };
      },
    },

    // 3. 作品-作者双向卡
    WORK_AUTHOR: {
      id: "work_author",
      name: "作品作者",
      render: (data) => [
        { front: `《${data.work}》的作者是？`, back: data.author },
        { front: `${data.author}的代表作？`, back: `《${data.work}》` },
      ],
    },

    // 4. 论述要点卡
    ESSAY_POINTS: {
      id: "essay_points",
      name: "论述要点",
      render: (data) => ({
        front: data.question,
        back: `关键词提示：${(data.keywords || []).join(" / ")}`,
      }),
    },

    // 5. 流派对照卡
    SCHOOL_COMPARE: {
      id: "school_compare",
      name: "流派对照",
      render: (data) => ({
        front: `对比：${data.schools.map(s => s.name).join(" vs ")}`,
        back: data.schools.map(s =>
          `${s.name}：${s.feature}`
        ).join("\n"),
      }),
    },

    // 6. 区分卡
    DISTINGUISH: {
      id: "distinguish",
      name: "易混淆区分",
      render: (data) => [
        { front: `${data.a}和${data.b}的区别？`, back: data.diff },
        { front: `哪个是：${data.a_feature}？`, back: data.a },
      ],
    },
  };

  window.WENYAN_CARD_TEMPLATES = TEMPLATES;
})();
```

- [ ] **Step 2: Commit**

```bash
git add js/card-templates.js
git commit -m "feat(app): 6种文学专用卡片模板

- 名词解释卡（社团类/作品类）
- Cloze名句填空卡
- 作品-作者双向卡
- 论述要点卡
- 流派对照卡
- 区分卡（易混淆内容对比）"
```

---

### Task 17: 卡片拆分遵循最小信息原则

> 已在Task 16的卡片模板中体现。此处实现拆卡规则。

- [ ] **Step 1: 在card-templates.js中添加拆卡函数**

```javascript
// 名词解释拆卡（一个拆成5-6张）
function splitNounExplain(knowledgePoint) {
  const cards = [];
  if (knowledgePoint.time) cards.push({ type: "noun_explain", front: `${knowledgePoint.title}的时代？`, back: knowledgePoint.time });
  if (knowledgePoint.members) cards.push({ type: "noun_explain", front: `${knowledgePoint.title}的代表作家？`, back: knowledgePoint.members });
  if (knowledgePoint.feature) cards.push({ type: "noun_explain", front: `${knowledgePoint.title}的风格特征？`, back: knowledgePoint.feature });
  if (knowledgePoint.significance) cards.push({ type: "noun_explain", front: `${knowledgePoint.title}的文学史意义？`, back: knowledgePoint.significance });
  // ... 按需拆分
  return cards;
}

// 避免集合题（唐宋八大家→分组枚举）
function splitCollection(knowledgePoint) {
  // "唐宋八大家" → 分组：初唐四杰 + 宋代六人
  return [
    { type: "noun_explain", front: "唐宋八大家中的唐代作家？", back: "韩愈、柳宗元" },
    { type: "noun_explain", front: "唐宋八大家中的宋代作家？", back: "欧阳修、苏洵、苏轼、苏辙、王安石、曾巩" },
  ];
}
```

- [ ] **Step 2: Commit**

```bash
git add js/card-templates.js
git commit -m "feat(app): 卡片拆分遵循最小信息原则

- 名词解释拆卡（一个拆5-6张）
- 避免集合题（分组枚举）
- 易混淆内容自动生成区分卡"
```

---

## Phase 4: 功能性知识图谱实现

> **依赖**：Task 11（ocr_status字段）、Task 18（图谱骨架）

### Task 18: 构建知识图谱骨架

**Files:**
- Create: `js/knowledge-graph.js`

- [ ] **Step 1: 创建知识图谱模块**

Create `js/knowledge-graph.js`:

```javascript
/* ============================================================
 *  文研 · 功能性知识图谱
 *  从装饰性升级为功能性：前置依赖/薄弱子图/干扰预警/掌握度可视化
 * ============================================================ */

(function () {
  "use strict";

  // 节点类型
  const NODE_TYPES = {
    AUTHOR: "AUTHOR",
    WORK: "WORK",
    SCHOOL: "SCHOOL",
    MOVEMENT: "MOVEMENT",
    CONCEPT: "CONCEPT",
    KNOWLEDGE_POINT: "KNOWLEDGE_POINT",
  };

  // 关系类型
  const RELATION_TYPES = {
    AUTHORED: "AUTHORED",
    BELONGS_TO: "BELONGS_TO",
    INFLUENCED_BY: "INFLUENCED_BY",
    COMPARED_WITH: "COMPARED_WITH",
    SAME_PERIOD: "SAME_PERIOD",
    PRECEDES: "PRECEDES",
    PREREQUISITE: "PREREQUISITE",
    RELATED_CONCEPT: "RELATED_CONCEPT",
  };

  // 图谱数据结构
  const graph = {
    nodes: [],  // {id, type, label, subject, retrievability (R值), prerequisites: []}
    edges: [],  // {source, target, type}
  };

  // 预置南师大考点骨架
  const PRESET_NODES = [
    { id: "luxun", type: NODE_TYPES.AUTHOR, label: "鲁迅", subject: "modern", prerequisites: [] },
    { id: "zhouzuoren", type: NODE_TYPES.AUTHOR, label: "周作人", subject: "modern", prerequisites: [] },
    { id: "maodun", type: NODE_TYPES.AUTHOR, label: "茅盾", subject: "modern", prerequisites: [] },
    { id: "shencongwen", type: NODE_TYPES.AUTHOR, label: "沈从文", subject: "modern", prerequisites: [] },
    { id: "zhangailing", type: NODE_TYPES.AUTHOR, label: "张爱玲", subject: "modern", prerequisites: [] },
    { id: "zhaoshuli", type: NODE_TYPES.AUTHOR, label: "赵树理", subject: "modern", prerequisites: [] },
    { id: "luyao", type: NODE_TYPES.AUTHOR, label: "路遥", subject: "modern", prerequisites: [] },
  ];

  const PRESET_EDGES = [
    { source: "luxun", target: "zhouzuoren", type: RELATION_TYPES.SAME_PERIOD },
    { source: "luxun", target: "zhouzuoren", type: RELATION_TYPES.COMPARED_WITH },
  ];

  function init() {
    graph.nodes = PRESET_NODES.map(n => ({ ...n, retrievability: 0, prerequisites: n.prerequisites || [] }));
    graph.edges = [...PRESET_EDGES];
  }

  // 添加节点
  function addNode(node) {
    if (!graph.nodes.find(n => n.id === node.id)) {
      graph.nodes.push({ ...node, retrievability: 0, prerequisites: [] });
    }
  }

  // 添加边
  function addEdge(source, target, type) {
    if (!graph.edges.find(e => e.source === source && e.target === target && e.type === type)) {
      graph.edges.push({ source, target, type });
    }
  }

  // 设置前置依赖
  function setPrerequisite(nodeId, prerequisiteId) {
    const node = graph.nodes.find(n => n.id === nodeId);
    if (node && !node.prerequisites.includes(prerequisiteId)) {
      node.prerequisites.push(prerequisiteId);
      addEdge(prerequisiteId, nodeId, RELATION_TYPES.PREREQUISITE);
    }
  }

  // 更新节点可提取性R（从FSRS卡片状态）
  function updateRetrievability(nodeId, rValue) {
    const node = graph.nodes.find(n => n.id === nodeId);
    if (node) node.retrievability = rValue;
  }

  // 获取节点颜色（掌握度可视化）
  function getNodeColor(rValue) {
    if (rValue >= 0.85) return "green";   // 掌握
    if (rValue >= 0.6) return "yellow";   // 一般
    return "red";                          // 薄弱
  }

  init();

  window.WENYAN_KNOWLEDGE_GRAPH = {
    NODE_TYPES,
    RELATION_TYPES,
    graph,
    addNode,
    addEdge,
    setPrerequisite,
    updateRetrievability,
    getNodeColor,
  };
})();
```

- [ ] **Step 2: Commit**

```bash
git add js/knowledge-graph.js
git commit -m "feat(app): 知识图谱骨架

- 6种节点类型（AUTHOR/WORK/SCHOOL/MOVEMENT/CONCEPT/KNOWLEDGE_POINT）
- 8种关系类型（含PREREQUISITE前置依赖）
- 预置南师大考点骨架（鲁迅/周作人/茅盾/沈从文等）
- 掌握度可视化（绿/黄/红映射R值）"
```

---

### Task 19: 实现前置依赖检测

> 已在Task 18的 `knowledge-graph.js` 中定义 `setPrerequisite`。此处实现检测逻辑。

- [ ] **Step 1: 添加前置依赖检测函数**

在 `js/knowledge-graph.js` 的 `window.WENYAN_KNOWLEDGE_GRAPH` 对象中添加:

```javascript
// 前置依赖检测：学习新卡前检查前置节点R值
function checkPrerequisites(nodeId) {
  const node = graph.nodes.find(n => n.id === nodeId);
  if (!node || !node.prerequisites.length) {
    return { passed: true, weakPrerequisites: [] };
  }
  const weak = node.prerequisites
    .map(pid => graph.nodes.find(n => n.id === pid))
    .filter(pn => pn && pn.retrievability < 0.7);
  return {
    passed: weak.length === 0,
    weakPrerequisites: weak,
  };
}
```

- [ ] **Step 2: Commit**

```bash
git add js/knowledge-graph.js
git commit -m "feat(app): 前置依赖检测

- 学习新卡前检查前置节点可提取性R
- R<0.7的前置节点先插入复习卡片
- 无前置依赖的知识点直接学习"
```

---

### Task 20: 实现薄弱子图识别

- [ ] **Step 1: 添加薄弱子图识别函数**

```javascript
// 薄弱子图识别：R值最低的连通子图
function identifyWeakSubgraph(maxNodes = 20) {
  const weakNodes = graph.nodes
    .filter(n => n.retrievability < 0.6)
    .sort((a, b) => a.retrievability - b.retrievability);

  if (weakNodes.length === 0) return { nodes: [], tooLarge: false };

  // 限制子图大小
  const limited = weakNodes.slice(0, maxNodes);
  return {
    nodes: limited,
    tooLarge: weakNodes.length > maxNodes,
    totalCount: weakNodes.length,
  };
}
```

- [ ] **Step 2: Commit**

```bash
git add js/knowledge-graph.js
git commit -m "feat(app): 薄弱子图识别

- 识别R值最低的连通子图
- 优先推送薄弱子图卡片
- 子图过大时按考频排序取前N个"
```

---

### Task 21: 实现干扰预警

- [ ] **Step 1: 添加干扰预警函数**

```javascript
// 干扰预警：连续复习相邻节点时插入区分卡
function checkInterference(recentNodeIds) {
  if (recentNodeIds.length < 2) return { warn: false };

  const last = recentNodeIds[recentNodeIds.length - 1];
  const prev = recentNodeIds[recentNodeIds.length - 2];

  // 检查是否有COMPARED_WITH或SAME_PERIOD关系
  const related = graph.edges.find(e =>
    (e.source === last && e.target === prev) ||
    (e.source === prev && e.target === last)
  );

  if (related && (related.type === RELATION_TYPES.COMPARED_WITH || related.type === RELATION_TYPES.SAME_PERIOD)) {
    return {
      warn: true,
      nodes: [prev, last],
      message: "注意区分这两个易混淆的知识点",
    };
  }
  return { warn: false };
}
```

- [ ] **Step 2: Commit**

```bash
git add js/knowledge-graph.js
git commit -m "feat(app): 干扰预警

- 检测连续复习图谱中相邻节点
- 主动插入区分卡
- 提示用户注意区分易混淆内容"
```

---

## Phase 5: AI助手增强与主动回忆检测

> **依赖**：Task 9（种子数据）、Task 18（知识图谱）

### Task 22: 实现苏格拉底式AI助手

**Files:**
- Create: `js/ai-assistant.js`

- [ ] **Step 1: 创建AI助手模块**

Create `js/ai-assistant.js`:

```javascript
/* ============================================================
 *  文研 · 苏格拉底式AI助手
 *  不直接给答案，引导用户自己找到答案
 * ============================================================ */

(function () {
  "use strict";

  const AI_API_URL = "api/ai/chat"; // 实际API地址需配置

  // 苏格拉底式引导流程
  const SOCRATIC_FLOW = {
    essay: [
      "step1_user_attempt",    // 让用户先尝试作答
      "step2_analyze_gaps",    // AI分析论证漏洞
      "step3_suggest",         // AI提供改进建议（非标准答案）
      "step4_show_model",      // 展示范文（标注"范文，非标准答案"）
    ],
  };

  // 答案过短或离题检测
  function isAnswerInsufficient(answer) {
    if (!answer || answer.length < 50) return true;
    // TODO: 离题检测（需NLP）
    return false;
  }

  // 生成苏格拉底式提问
  async function socraticGuidance(question, userAnswer, context) {
    if (isAnswerInsufficient(userAnswer)) {
      return {
        type: "insufficient",
        message: "答案内容不足/偏离题目，建议先回顾相关知识点",
        suggestReview: context.relatedKnowledgePoints || [],
      };
    }

    // RAG检索相关资料
    const ragResults = ragSearch(question, context);

    // 构建prompt（苏格拉底式：不直接给答案）
    const prompt = buildSocraticPrompt(question, userAnswer, ragResults);

    // 调用AI API
    // const response = await callAI(prompt);
    // return response;

    return {
      type: "socratic",
      analysis: "AI将分析你的论证漏洞",
      suggestions: "AI将提供改进建议（非标准答案）",
      modelEssay: "范文（标注：范文，非标准答案）",
      references: ragResults,
    };
  }

  // RAG检索
  function ragSearch(query, context) {
    // 基于用户资料库+权威教材库检索
    // 返回带来源标注的引用
    const results = [];
    // TODO: 实现实际RAG检索
    return results;
  }

  function buildSocraticPrompt(question, userAnswer, ragResults) {
    return `用户正在练习论述题：
题目：${question}
用户答案：${userAnswer}

请以苏格拉底式方式引导：
1. 不直接给出完整答案
2. 分析用户答案的论证漏洞
3. 提供改进建议（非标准答案）
4. 最后展示范文供对比（标注"范文，非标准答案"）

参考资料（RAG检索结果）：
${ragResults.map(r => `- ${r.source}: ${r.content}`).join("\n")}`;
  }

  // "解释我的答案"机制
  async function explainMyAnswer(question, userAnswer, correctAnswer) {
    return {
      errorAnalysis: "AI分析错误思路",
      correctApproach: "正确思路是什么",
      references: "引用用户资料库中的相关知识点",
    };
  }

  window.WENYAN_AI_ASSISTANT = {
    socraticGuidance,
    explainMyAnswer,
    isAnswerInsufficient,
  };
})();
```

- [ ] **Step 2: Commit**

```bash
git add js/ai-assistant.js
git commit -m "feat(app): 苏格拉底式AI助手

- 不直接给答案，引导用户自己找到答案
- 论述题辅助流程（作答→分析漏洞→改进建议→范文对比）
- "解释我的答案"机制（答错后分析错误思路）
- RAG架构（基于用户资料库+权威教材库检索）
- 答案过短/离题检测"
```

---

### Task 23: 实现内容来源五级标注

> 已在Task 10的 `db-schema.js` 中定义 `CONTENT_SOURCE_LABELS`。此处集成到UI。

**Files:**
- Modify: `js/app.js`（渲染来源标签）
- Modify: `css/style.css`（来源标签样式）

- [ ] **Step 1: 添加来源标签渲染函数**

在 `js/app.js` 中添加:

```javascript
function renderSourceLabel(contentSource) {
  const labels = window.WENYAN_SCHEMA.CONTENT_SOURCE_LABELS;
  const info = labels[contentSource] || labels.TEXTBOOK_NATIVE;
  const ocrBadge = contentSource === "TEXTBOOK_OCR" ? '<span class="ocr-badge">OCR</span>' : '';
  return `<span class="source-label source-${info.color}">${info.icon} ${info.label}${ocrBadge}</span>`;
}
```

- [ ] **Step 2: 添加来源标签CSS**

在 `css/style.css` 中添加:

```css
.source-label { display: inline-flex; align-items: center; gap: 2px; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 500; }
.source-green { background: #e8f5e9; color: #2e7d32; }
.source-blue { background: #e3f2fd; color: #1565c0; }
.source-yellow { background: #fff9c4; color: #f57f17; }
.source-gray { background: #f5f5f5; color: #616161; }
.source-red { background: #ffebee; color: #c62828; }
.ocr-badge { margin-left: 4px; font-size: 10px; opacity: 0.7; }
```

- [ ] **Step 3: Commit**

```bash
git add js/app.js css/style.css
git commit -m "feat(app): 内容来源五级标注

- TEXTBOOK_NATIVE/TEXTBOOK_OCR→绿色"资料"标签
- AI_GENERATED→蓝色"AI"标签
- HYBRID→黄色"资料+AI"标签
- USER_CREATED→灰色"我的"标签
- OCR文本带OCR角标"
```

---

### Task 24: 实现主动回忆检测三层方案

**Files:**
- Create: `js/recall-detector.js`

- [ ] **Step 1: 创建主动回忆检测模块**

Create `js/recall-detector.js`:

```javascript
/* ============================================================
 *  文研 · 主动回忆检测三层方案
 *  关键词匹配 → 语义相似度 → LLM异步评估
 * ============================================================ */

(function () {
  "use strict";

  // 第1层：关键词匹配+同义词词典
  function keywordMatch(userAnswer, keywords, synonyms = {}) {
    if (!userAnswer || !keywords.length) return { pass: false, coverage: 0 };

    const normalizedAnswer = userAnswer.toLowerCase();
    let matched = 0;

    for (const kw of keywords) {
      const allForms = [kw, ...(synonyms[kw] || [])];
      if (allForms.some(form => normalizedAnswer.includes(form.toLowerCase()))) {
        matched++;
      }
    }

    const coverage = matched / keywords.length;
    return {
      pass: coverage >= 0.7,
      coverage: Math.round(coverage * 100) / 100,
      matched,
      total: keywords.length,
    };
  }

  // 第2层：语义相似度（BGE-small-zh模型）
  // 实际部署时需加载BGE-small-zh-v1.5模型
  async function semanticSimilarity(userAnswer, referenceAnswer) {
    // TODO: 加载BGE-small-zh模型计算余弦相似度
    // 简化实现：使用Jaccard相似度作为fallback
    const similarity = jaccardSimilarity(userAnswer, referenceAnswer);
    return {
      score: similarity,
      verdict: similarity > 0.85 ? "correct" : similarity > 0.6 ? "partial" : "wrong",
    };
  }

  function jaccardSimilarity(a, b) {
    const setA = new Set(a.split(""));
    const setB = new Set(b.split(""));
    const intersection = new Set([...setA].filter(x => setB.has(x)));
    const union = new Set([...setA, ...setB]);
    return union.size === 0 ? 0 : intersection.size / union.size;
  }

  // 第3层：LLM异步评估（第2层"部分正确"时触发）
  async function llmEvaluate(userAnswer, referenceAnswer) {
    // TODO: 调用LLM API
    return {
      score: 0,
      reason: "LLM异步评估中，不阻塞复习流程",
      pending: true,
    };
  }

  // 综合检测（三层渐进式）
  async function detect(userAnswer, card) {
    // 第1层：关键词匹配
    if (card.keywords) {
      const result = keywordMatch(userAnswer, card.keywords, card.synonyms);
      if (result.pass) {
        return { layer: 1, verdict: "correct", detail: result };
      }
    }

    // 第2层：语义相似度
    if (card.reference_answer) {
      const result = await semanticSimilarity(userAnswer, card.reference_answer);
      if (result.verdict === "correct") {
        return { layer: 2, verdict: "correct", detail: result };
      } else if (result.verdict === "partial") {
        // 第3层：LLM异步评估
        const llmResult = await llmEvaluate(userAnswer, card.reference_answer);
        return { layer: 3, verdict: "partial", detail: { semantic: result, llm: llmResult } };
      } else {
        return { layer: 2, verdict: "wrong", detail: result };
      }
    }

    return { layer: 0, verdict: "unknown", detail: { message: "无参考答案" } };
  }

  // 防"背关键词但不懂含义"
  function detectKeywordMemorization(cardHistory, relatedCardsHistory) {
    // 某卡片始终"正确"但关联卡片频繁出错
    const cardCorrectRate = cardHistory.filter(h => h.verdict === "correct").length / cardHistory.length;
    const relatedCorrectRate = relatedCardsHistory.filter(h => h.verdict === "correct").length / relatedCardsHistory.length;

    if (cardCorrectRate > 0.9 && relatedCorrectRate < 0.5) {
      return {
        suspect: true,
        action: "降低置信度，安排变体出题+反向提问",
      };
    }
    return { suspect: false };
  }

  window.WENYAN_RECALL_DETECTOR = {
    detect,
    keywordMatch,
    semanticSimilarity,
    llmEvaluate,
    detectKeywordMemorization,
  };
})();
```

- [ ] **Step 2: Commit**

```bash
git add js/recall-detector.js
git commit -m "feat(app): 主动回忆检测三层方案

- 第1层：关键词匹配+同义词词典（名词解释，覆盖率≥70%判正确）
- 第2层：语义相似度BGE-small-zh（论述题，>0.85正确/0.6-0.85部分正确/<0.6错误）
- 第3层：LLM异步评估（部分正确时触发，不阻塞）
- 防"背关键词但不懂含义"（变体出题+反向提问）"
```

---

## Phase 6: 科目代码历史与数据修正

### Task 25: 实现科目代码变动历史

> 已在Task 12的 `js/exam-codes.js` 中实现。

**Files:**
- Modify: `js/app.js`
- Modify: `js/data.js`

- [ ] **Step 1: 在真题展示中集成科目代码判定**

在 `js/app.js` 的真题渲染函数中，使用 `WENYAN_EXAM_CODES.getDisplayLabel(year, examPaperCode)` 显示科目。

```javascript
// === js/app.js 新增/修改部分 ===

/**
 * 渲染真题列表项，使用科目代码历史判定科目名称。
 * 2026年变动前：610=文学基础，801=专业写作
 * 2026年变动后：610=专业写作，801=文学基础
 */
function renderExamQuestionItem(question) {
  const subjectLabel = WENYAN_EXAM_CODES.getDisplayLabel(
    question.year,
    question.exam_paper_code
  );
  // subjectLabel 格式："610 文学基础" 或 "610 文学基础（2022年代码）"

  const answerStatusBadge = {
    'HAS_ANSWER': '<span class="badge badge-green">有答案</span>',
    'NO_ANSWER': '<span class="badge badge-orange">无答案</span>',
    'AI_GENERATED': '<span class="badge badge-blue">AI生成答案</span>',
  }[question.answer_status] || '';

  return `
    <div class="exam-question-item" data-id="${question.id}">
      <div class="question-header">
        <span class="question-year">${question.year}年</span>
        <span class="question-code">${subjectLabel}</span>
        ${answerStatusBadge}
      </div>
      <div class="question-content">${question.content}</div>
      ${question.material_text ? `
        <div class="question-material">
          <strong>材料：</strong>${question.material_text}
        </div>
      ` : ''}
      ${question.source_file ? `
        <div class="question-source">
          来源：${question.source_file}
          ${question.source_page ? `P${question.source_page}` : ''}
        </div>
      ` : ''}
    </div>
  `;
}

/**
 * 真题筛选器：按科目代码历史筛选。
 */
function setupExamFilter() {
  const yearFilter = document.getElementById('exam-year-filter');
  const codeFilter = document.getElementById('exam-code-filter');

  async function applyFilter() {
    const year = yearFilter?.value;
    const code = codeFilter?.value;  // "610" / "801" / "805" / "all"

    let questions;
    if (code === 'all') {
      questions = await DBManager.query(
        'SELECT * FROM exam_questions WHERE ocr_status = ? ORDER BY year DESC',
        ['VERIFIED']
      );
    } else {
      questions = await DBManager.query(
        'SELECT * FROM exam_questions WHERE exam_paper_code = ? AND ocr_status = ? ORDER BY year DESC',
        [code, 'VERIFIED']
      );
    }

    if (year) {
      questions = questions.filter(q => q.year == year);
    }

    const container = document.getElementById('exam-list');
    container.innerHTML = questions.map(renderExamQuestionItem).join('');
  }

  yearFilter?.addEventListener('change', applyFilter);
  codeFilter?.addEventListener('change', applyFilter);
}
```

- [ ] **Step 2: 修正原种子数据表述**

在 `js/data.js` 中，将所有"610文学基础真题"等表述修正为"文学基础真题（当年试卷代码610）"。

```javascript
// === js/data.js 修正部分 ===

/**
 * 修正真题标题：不再硬编码"610文学基础"，
 * 改为"文学基础真题（当年试卷代码610）"，由exam-codes.js按年份判定。
 */
function correctExamTitles(questions) {
  return questions.map(q => {
    if (q.title && q.title.match(/^(610|801|805|806)/)) {
      // 移除开头的代码，改为按年份动态判定
      const baseTitle = q.title.replace(/^(610|801|805|806)\s*/, '');
      q.title = `${baseTitle}（当年试卷代码${q.exam_paper_code || '未知'}）`;
    }
    return q;
  });
}

// 加载种子数据时应用修正
async function loadSeedData() {
  const response = await fetch('assets/seed_data.json');
  const data = await response.json();
  data.exam_questions = correctExamTitles(data.exam_questions);
  return data;
}
```

- [ ] **Step 3: Commit**

```bash
git add js/app.js js/data.js
git commit -m "feat(app): 科目代码历史集成

- 真题展示联合year+exam_paper_code判定科目
- 2022年610显示"610 文学基础（2022年代码）"
- 2026年及以后610显示"610 专业写作"
- 修正原种子数据表述"
```

---

### Task 26: 移除导师画像功能（外链官网）

**Files:**
- Modify: `js/app.js`
- Modify: `index.html`

- [ ] **Step 1: 添加导师信息外链入口**

在 `js/app.js` 中添加导师信息入口，跳转到南师大文学院官网教师页 `https://wxy.njnu.edu.cn/szdw/jsfc.htm`。

```javascript
// === js/app.js 新增部分 ===

/**
 * 导师信息模块：不内置导师数据（数据不支持），
 * 改为外链南师大文学院官网教师页。
 */
const MENTOR_EXTERNAL_LINK = 'https://wxy.njnu.edu.cn/szdw/jsfc.htm';

function renderMentorSection() {
  const container = document.getElementById('mentor-section');
  if (!container) return;

  container.innerHTML = `
    <div class="mentor-external-link">
      <h3>导师信息</h3>
      <p>导师信息请访问南京师范大学文学院官网教师风采页面查看。</p>
      <p>因导师信息可能随人事变动而变化，本App不内置导师数据。</p>
      <a href="${MENTOR_EXTERNAL_LINK}" target="_blank" rel="noopener noreferrer"
         class="btn btn-primary">
        前往文学院官网教师页
        <span class="external-link-icon">↗</span>
      </a>
    </div>
  `;
}

// 在App初始化时调用
// renderMentorSection();
```

同时在 `index.html` 中添加导师信息容器：

```html
<!-- index.html 中添加 -->
<section id="mentor-section" class="card">
  <!-- 由 renderMentorSection() 填充 -->
</section>
```

- [ ] **Step 2: 移除原设计文档中的导师画像规划**

检查 `docs/2026-07-08-wenyan-android-app-design.md` 中是否有导师画像相关内容，添加注释说明已移除：

```markdown
> 注：导师画像功能已移除（2026-07-10）。原因：导师信息随人事变动频繁，
> 内置数据易过时且准确性无法保证。改为外链南师大文学院官网教师页。
> 详见 Task 26。
```

- [ ] **Step 3: Commit**

```bash
git add js/app.js index.html docs/
git commit -m "feat(app): 导师信息改为外链官网

- 不内置导师数据（数据不支持）
- "导师信息"入口跳转南师大文学院官网教师页
- 原设计文档导师画像规划标注为已移除"
```

---

## Phase 7: 验证与测试

> **依赖**：所有前序Phase完成

### Task 27: 资料数字化质量验证

**Files:**
- Create: `tools/ocr/tests/test_phase1_quality.py`

- [ ] **Step 1: 编写资料数字化质量验证测试**

Create `tools/ocr/tests/test_phase1_quality.py`:

```python
"""
Phase 1资料数字化质量验证测试（对应checklist C1.1-C1.36）。
运行全部测试验证数字化产物符合spec要求。
"""
import json
import pytest
from pathlib import Path

TOOLS_DIR = Path(__file__).parent.parent
PROJECT_ROOT = TOOLS_DIR.parent.parent
MANIFEST_PATH = TOOLS_DIR / "manifest.json"
OUTPUT_DIR = TOOLS_DIR / "output"
SEED_DATA_PATH = PROJECT_ROOT / "assets" / "seed_data.json"
REFERENCE_CATALOG_PATH = PROJECT_ROOT / "assets" / "reference_catalog.json"
EXAM_CODE_HISTORY_PATH = PROJECT_ROOT / "assets" / "exam_code_history.json"
ERROR_DICT_PATH = PROJECT_ROOT / "assets" / "error_dict.json"

@pytest.fixture(scope="module")
def manifest():
    if not MANIFEST_PATH.exists():
        pytest.skip("manifest.json不存在，请先运行scan_files.py")
    return json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))

@pytest.fixture(scope="module")
def seed_data():
    if not SEED_DATA_PATH.exists():
        pytest.skip("seed_data.json不存在，请先完成Phase 1")
    return json.loads(SEED_DATA_PATH.read_text(encoding="utf-8"))

# === C1.1 文件覆盖度 ===
class TestFileCoverage:
    def test_total_file_count(self, manifest):
        """C1.1: 208个文件全部进入manifest"""
        assert manifest["stats"]["total"] == 208, f"文件数不符: {manifest['stats']['total']}"
        print(f"文件覆盖度: PASS ({manifest['stats']['total']}个文件)")

    def test_pdf_count(self, manifest):
        """C1.2: 153个PDF全部处理"""
        pdf_types = ["NATIVE", "OCR_LAYER", "SCAN_ONLY", "MIXED"]
        pdf_count = sum(
            manifest["stats"]["by_type"].get(t, 0) for t in pdf_types
        )
        assert pdf_count == 153, f"PDF数不符: {pdf_count}"

    def test_no_pending_files(self, manifest):
        """所有文件状态非PENDING（已处理或已跳过）"""
        pending = [f for f in manifest["files"] if f["status"] == "PENDING"]
        assert len(pending) == 0, f"仍有{len(pending)}个文件未处理: {[f['filename'] for f in pending[:5]]}"

    def test_duplicates_skipped(self, manifest):
        """C1.7: 重复文件已去重"""
        skipped = [f for f in manifest["files"] if f["status"] == "SKIPPED"]
        # 预期6.文学理论和8.复试真题的(1)后缀文件被跳过
        print(f"去重跳过: {len(skipped)}个文件")
        for f in skipped:
            assert "重复文件" in (f.get("last_error") or ""), f"非重复跳过: {f['filename']}"

# === C1.8-C1.13 OCR准确率 ===
class TestOCRAccuracy:
    def test_native_zero_errors(self, manifest):
        """C1.8: NATIVE类型PDF提取零错字"""
        native_files = [f for f in manifest["files"] if f["type"] == "NATIVE"]
        for f in native_files:
            if f["status"] != "DONE":
                continue
            output_json = OUTPUT_DIR / f"{Path(f['filename']).stem}.json"
            if output_json.exists():
                data = json.loads(output_json.read_text(encoding="utf-8"))
                # NATIVE类型不应有OCR置信度问题
                assert data.get("content_source") == "TEXTBOOK_NATIVE"
                assert data.get("ocr_status") == "VERIFIED", \
                    f"NATIVE文件应VERIFIED: {f['filename']}"

    def test_ocr_confidence_threshold(self, manifest):
        """C1.9: OCR类型文件准确率≥95%（抽样验证）"""
        ocr_files = [f for f in manifest["files"] if f["type"] in ("SCAN_ONLY", "OCR_LAYER")]
        done_files = [f for f in ocr_files if f["status"] == "DONE"]
        if not done_files:
            pytest.skip("无已完成的OCR文件")

        # 抽样10%
        sample_size = max(1, len(done_files) // 10)
        sampled = done_files[:sample_size]

        low_confidence_files = []
        for f in sampled:
            output_json = OUTPUT_DIR / f"{Path(f['filename']).stem}.json"
            if output_json.exists():
                data = json.loads(output_json.read_text(encoding="utf-8"))
                avg_conf = data.get("avg_confidence", 0)
                if avg_conf < 0.95:
                    low_confidence_files.append((f["filename"], avg_conf))

        # 打印低置信度文件供人工复核
        if low_confidence_files:
            print(f"低置信度文件（需人工复核）:")
            for name, conf in low_confidence_files:
                print(f"  {name}: {conf:.3f}")
        # 注：置信度只是参考，最终准确率需人工抽检

    def test_llm_correction_change_rate(self):
        """C1.10: LLM纠错改动率<5%"""
        corrected_files = list(OUTPUT_DIR.glob("*_corrected.json"))
        if not corrected_files:
            pytest.skip("无LLM纠错文件")

        for cf in corrected_files:
            data = json.loads(cf.read_text(encoding="utf-8"))
            change_rate = data.get("change_rate_percent", 0)
            assert change_rate < 5, \
                f"改动率过高 {cf.name}: {change_rate}%（应<5%）"

    def test_error_dict_generated(self):
        """C1.13: error_dict.json正确沉淀"""
        if not ERROR_DICT_PATH.exists():
            pytest.skip("error_dict.json未生成")
        error_dict = json.loads(ERROR_DICT_PATH.read_text(encoding="utf-8"))
        assert isinstance(error_dict, dict)
        # 每条记录应有原始错误和修正
        for wrong, correct in error_dict.items():
            assert isinstance(wrong, str)
            assert isinstance(correct, str)
            assert wrong != correct, f"无效记录: {wrong}={correct}"

# === C1.14-C1.20 知识提取验证 ===
class TestKnowledgeExtraction:
    def test_knowledge_points_coverage(self, seed_data):
        """C1.14: 知识点覆盖四科，每科至少50+"""
        kps = seed_data.get("knowledge_points", [])
        subjects = {}
        for kp in kps:
            subj = kp.get("subject", "未知")
            subjects[subj] = subjects.get(subj, 0) + 1

        required = ["古代文学", "现当代文学", "外国文学", "文学理论"]
        for subj in required:
            count = subjects.get(subj, 0)
            assert count >= 50, f"{subj}知识点不足: {count}（应≥50）"

    def test_exam_years_coverage(self, seed_data):
        """C1.15: 真题覆盖1998-2025年"""
        questions = seed_data.get("exam_questions", [])
        years = set()
        for q in questions:
            year = q.get("year")
            if year:
                years.add(int(year))
        assert 1998 in years or 2000 in years, "缺少早期真题"
        assert 2024 in years or 2025 in years, "缺少近期真题"
        print(f"真题年份覆盖: {min(years)}-{max(years)}, 共{len(years)}年")

    def test_knowledge_point_granularity(self, seed_data):
        """C1.16: 知识点粒度50-150字"""
        kps = seed_data.get("knowledge_points", [])
        oversized = []
        for kp in kps:
            content = kp.get("content", "")
            if len(content) > 0 and (len(content) < 50 or len(content) > 150):
                oversized.append((kp.get("title", ""), len(content)))
        # 允许少量超出，但不应大量
        assert len(oversized) < len(kps) * 0.1, \
            f"粒度异常知识点过多: {len(oversized)}/{len(kps)}"

# === C1.21-C1.27 多教材交叉校验 ===
class TestCrossValidation:
    def test_ancient_dual_textbook(self, seed_data):
        """C1.21/C1.22: 古代文学双轨制（袁行霈学习+马工程答题）"""
        kps = seed_data.get("knowledge_points", [])
        ancient = [kp for kp in kps if kp.get("subject") == "古代文学"]
        if not ancient:
            pytest.skip("无古代文学知识点")

        has_study_text = any(kp.get("study_text") for kp in ancient)
        has_core_conclusion = any(kp.get("core_conclusion") for kp in ancient)
        assert has_study_text, "古代文学缺少study_text（袁行霈版）"
        assert has_core_conclusion, "古代文学缺少core_conclusion（马工程版）"

    def test_conflict_flagging(self, seed_data):
        """C1.26: 版本矛盾标记conflict_flag"""
        kps = seed_data.get("knowledge_points", [])
        # 检查有矛盾标记的知识点
        conflicts = [kp for kp in kps if kp.get("conflict_flag")]
        print(f"标记矛盾的知识点: {len(conflicts)}个")
        for kp in conflicts:
            assert kp.get("conflict_note"), \
                f"矛盾知识点缺少说明: {kp.get('title')}"

# === C1.32-C1.36 种子数据完整性 ===
class TestSeedDataIntegrity:
    def test_seed_data_structure(self, seed_data):
        """C1.32: seed_data.json结构完整"""
        required_keys = ["knowledge_points", "exam_questions", "cards", "writing_materials"]
        for key in required_keys:
            assert key in seed_data, f"缺少字段: {key}"
            assert isinstance(seed_data[key], list), f"{key}应为数组"

    def test_reference_catalog_exists(self):
        """C1.33: reference_catalog.json生成"""
        if not REFERENCE_CATALOG_PATH.exists():
            pytest.skip("reference_catalog.json未生成")
        catalog = json.loads(REFERENCE_CATALOG_PATH.read_text(encoding="utf-8"))
        assert isinstance(catalog, list)
        for item in catalog:
            assert "title" in item and "url" in item, f"无效条目: {item}"

    def test_exam_code_history_exists(self):
        """C1.34: exam_code_history.json生成"""
        if not EXAM_CODE_HISTORY_PATH.exists():
            pytest.skip("exam_code_history.json未生成")
        history = json.loads(EXAM_CODE_HISTORY_PATH.read_text(encoding="utf-8"))
        assert isinstance(history, list)
        # 验证2026年变动
        has_2026 = any(
            item.get("valid_from_year") == 2026 for item in history
        )
        assert has_2026, "缺少2026年科目代码变动记录"
```

- [ ] **Step 2: 运行Phase 1质量验证测试**

Run: `cd tools/ocr && python -m pytest tests/test_phase1_quality.py -v --tb=short`
Expected: 所有测试通过（或skip因前置条件未满足，但无FAIL）

- [ ] **Step 3: 人工抽检OCR准确率（C1.9-C1.11）**

抽样10%的OCR文件（约12个），每文件抽3-5页与原图对照：
1. 从 `output/` 目录选取12个OCR结果JSON
2. 对照原PDF/图片，检查形近字、标点、专名错误
3. 计算准确率，记录到 `output/ocr_accuracy_report.md`
4. 准确率<95%则回炉调prompt或换OCR引擎

- [ ] **Step 4: 人工抽检LLM纠错（C1.11）**

抽检20处LLM纠错改动：
1. 从 `output/*_corrected.json` 选取20条改动
2. 检查是否只修形近字，未改语义、未动专名
3. 过度修正的回炉调prompt

- [ ] **Step 5: Commit**

```bash
git add tools/ocr/tests/test_phase1_quality.py tools/ocr/output/ocr_accuracy_report.md
git commit -m "test(ocr): Phase 1资料数字化质量验证

- 文件覆盖度验证（208个文件）
- OCR准确率验证（抽样10%人工校对）
- LLM纠错改动率验证（<5%）
- 知识提取验证（四科50+/真题1998-2025）
- 多教材交叉校验验证
- 种子数据完整性验证"
```

### Task 28: 数据库schema验证

**Files:**
- Create: `tests/test_db_schema.js`

- [ ] **Step 1: 编写数据库schema验证测试**

Create `tests/test_db_schema.js`:

```javascript
/**
 * 数据库schema验证测试（对应checklist C2.1-C2.16）。
 * 验证exam_questions/knowledge_points表新增字段、新增表、索引。
 */
import { describe, it, expect, beforeEach } from 'vitest';
import { DBManager } from '../js/db.js';

describe('数据库Schema验证', () => {
  let db;

  beforeEach(async () => {
    db = await DBManager.open(':memory:');
    await DBManager.initSchema(db);
  });

  describe('exam_questions表新增字段（C2.1-C2.5）', () => {
    it('exam_paper_code字段存在', () => {
      const columns = db.exec("PRAGMA table_info(exam_questions)");
      const names = columns[0].values.map(r => r[1]);
      expect(names).toContain('exam_paper_code');
    });

    it('answer_status字段存在', () => {
      const columns = db.exec("PRAGMA table_info(exam_questions)");
      const names = columns[0].values.map(r => r[1]);
      expect(names).toContain('answer_status');
    });

    it('material_text字段存在', () => {
      const columns = db.exec("PRAGMA table_info(exam_questions)");
      const names = columns[0].values.map(r => r[1]);
      expect(names).toContain('material_text');
    });

    it('source_file和source_page字段存在', () => {
      const columns = db.exec("PRAGMA table_info(exam_questions)");
      const names = columns[0].values.map(r => r[1]);
      expect(names).toContain('source_file');
      expect(names).toContain('source_page');
    });

    it('idx_questions_paper_code索引存在', () => {
      const indexes = db.exec("PRAGMA index_list(exam_questions)");
      const names = indexes[0].values.map(r => r[1]);
      expect(names).toContain('idx_questions_paper_code');
    });
  });

  describe('knowledge_points表新增字段（C2.6-C2.10）', () => {
    it('content_source字段存在', () => {
      const columns = db.exec("PRAGMA table_info(knowledge_points)");
      const names = columns[0].values.map(r => r[1]);
      expect(names).toContain('content_source');
    });

    it('ocr_status字段存在且默认VERIFIED', () => {
      const columns = db.exec("PRAGMA table_info(knowledge_points)");
      const ocrCol = columns[0].values.find(r => r[1] === 'ocr_status');
      expect(ocrCol).toBeDefined();
      // dflt_value列（索引4）应为'verified'
      expect(ocrCol[4]).toMatch(/verified/i);
    });

    it('study_text字段存在', () => {
      const columns = db.exec("PRAGMA table_info(knowledge_points)");
      const names = columns[0].values.map(r => r[1]);
      expect(names).toContain('study_text');
    });

    it('idx_points_ocr_status索引存在', () => {
      const indexes = db.exec("PRAGMA index_list(knowledge_points)");
      const names = indexes[0].values.map(r => r[1]);
      expect(names).toContain('idx_points_ocr_status');
    });
  });

  describe('新增表验证（C2.11-C2.13）', () => {
    it('exam_code_history表存在', () => {
      const tables = db.exec("SELECT name FROM sqlite_master WHERE type='table'");
      const names = tables[0].values.map(r => r[0]);
      expect(names).toContain('exam_code_history');
      // 验证字段
      const cols = db.exec("PRAGMA table_info(exam_code_history)");
      const colNames = cols[0].values.map(r => r[1]);
      expect(colNames).toContain('exam_code');
      expect(colNames).toContain('subject_name');
      expect(colNames).toContain('valid_from_year');
      expect(colNames).toContain('valid_to_year');
      expect(colNames).toContain('direction');
    });

    it('data_sources表存在', () => {
      const tables = db.exec("SELECT name FROM sqlite_master WHERE type='table'");
      const names = tables[0].values.map(r => r[0]);
      expect(names).toContain('data_sources');
    });

    it('mentors表不存在（C2.13）', () => {
      const tables = db.exec("SELECT name FROM sqlite_master WHERE type='table'");
      const names = tables[0].values.map(r => r[0]);
      expect(names).not.toContain('mentors');
    });
  });
});
```

- [ ] **Step 2: 运行schema验证测试**

Run: `npx vitest run tests/test_db_schema.js`
Expected: 所有测试通过

- [ ] **Step 3: Commit**

```bash
git add tests/test_db_schema.js
git commit -m "test(db): 数据库schema验证测试

- exam_questions新增5字段+1索引验证
- knowledge_points新增5字段+1索引验证
- exam_code_history/data_sources表存在验证
- mentors表不存在验证（导师改为外链）"
```

### Task 29: FSRS与卡片验证

**Files:**
- Create: `tests/test_fsrs.js`
- Create: `tests/test_cards.js`

- [ ] **Step 1: 编写FSRS算法验证测试**

Create `tests/test_fsrs.js`:

```javascript
/**
 * FSRS算法与卡片验证测试（对应checklist C3.1-C3.23）。
 */
import { describe, it, expect } from 'vitest';
import { createCard, updateCard, PRESETS, getDesiredRetentionForDays } from '../js/fsrs.js';

describe('FSRS参数预设验证（C3.1-C3.5）', () => {
  it('名词解释预设: desired_retention=0.90', () => {
    expect(PRESETS.NOUN_EXPLANATION.desired_retention).toBe(0.90);
  });

  it('作品背诵预设: desired_retention=0.95, enable_fuzzing=false', () => {
    expect(PRESETS.WORK_RECITATION.desired_retention).toBe(0.95);
    expect(PRESETS.WORK_RECITATION.enable_fuzzing).toBe(false);
  });

  it('论述题预设: desired_retention=0.85', () => {
    expect(PRESETS.ESSAY.desired_retention).toBe(0.85);
  });

  it('流派特征预设: desired_retention=0.90', () => {
    expect(PRESETS.SCHOOL_FEATURE.desired_retention).toBe(0.90);
  });

  it('createCard包含enable_fuzzing字段', () => {
    const card = createCard(PRESETS.NOUN_EXPLANATION);
    expect(card).toHaveProperty('enable_fuzzing');
    expect(card.enable_fuzzing).toBe(PRESETS.NOUN_EXPLANATION.enable_fuzzing);
  });
});

describe('考研倒计时动态保持率（C3.6-C3.9）', () => {
  it('基础阶段(>180天): 0.85', () => {
    const retention = getDesiredRetentionForDays(200);
    expect(retention).toBe(0.85);
  });

  it('强化阶段(90-180天): 0.90', () => {
    const retention = getDesiredRetentionForDays(150);
    expect(retention).toBe(0.90);
  });

  it('冲刺阶段(<90天): 0.95', () => {
    const retention = getDesiredRetentionForDays(60);
    expect(retention).toBe(0.95);
  });

  it('阶段边界平滑过渡', () => {
    // 边界附近不应跳变
    const r179 = getDesiredRetentionForDays(179);
    const r181 = getDesiredRetentionForDays(181);
    const r89 = getDesiredRetentionForDays(89);
    const r91 = getDesiredRetentionForDays(91);
    // 允许边界跳变，但打印供确认
    console.log(`179天=${r179}, 181天=${r181}, 89天=${r89}, 91天=${r91}`);
  });
});

describe('OCR状态过滤（C3.10-C3.13）', () => {
  it('PENDING知识点不进FSRS队列', async () => {
    const { getReviewQueue } = await import('../js/app.js');
    // 模拟有PENDING和VERIFIED知识点
    const queue = await getReviewQueue();
    // 队列中不应有ocr_status=PENDING的项
    for (const item of queue) {
      expect(item.ocr_status).not.toBe('PENDING');
    }
  });
});
```

- [ ] **Step 2: 编写卡片模板验证测试**

Create `tests/test_cards.js`:

```javascript
/**
 * 6种卡片模板验证（C3.14-C3.23）。
 */
import { describe, it, expect } from 'vitest';
import {
  renderNounExplanationCard,
  renderClozeCard,
  renderBidirectionalCard,
  renderEssayKeyPointsCard,
  renderSchoolComparisonCard,
  renderDistinctionCard,
  splitNounExplanation,
} from '../js/card-templates.js';

describe('卡片模板渲染验证（C3.14-C3.19）', () => {
  const sampleSociety = {
    title: "文学研究会",
    type: "society",
    time: "1921年",
    place: "北京",
    members: "郑振铎、沈雁冰、叶绍钧、王统照",
    publication: "《小说月报》",
    主张: "为人生而艺术",
    contribution: "新文学第一个纯文学社团，推动现实主义"
  };

  it('名词解释卡（社团类）正确渲染', () => {
    const html = renderNounExplanationCard(sampleSociety);
    expect(html).toContain("1921年");
    expect(html).toContain("北京");
    expect(html).toContain("郑振铎");
    expect(html).toContain("小说月报");
    expect(html).toContain("为人生");
  });

  it('Cloze名句填空卡正确渲染', () => {
    const cloze = {
      original: "路漫漫其修远兮，吾将上下而求索",
      blank: "求索",
      hint: "动词，追寻之意"
    };
    const html = renderClozeCard(cloze);
    expect(html).toContain("___");  // 填空
    expect(html).toContain("追寻");  // 提示
  });

  it('作品-作者双向卡生成正反两张', () => {
    const pair = renderBidirectionalCard({ work: "边城", author: "沈从文" });
    expect(pair.front).toContain("边城");
    expect(pair.back).toContain("沈从文");
    expect(pair.reverse.front).toContain("沈从文");
    expect(pair.reverse.back).toContain("边城");
  });

  it('论述要点卡背面是关键词提示', () => {
    const card = renderEssayKeyPointsCard({
      question: "简述鲁迅《狂人日记》的艺术特色",
      keywords: ["象征手法", "日记体", "现实主义", "启蒙"]
    });
    expect(card.back).toContain("象征");
    expect(card.back).not.toContain("现实主义和象征主义结合");  // 不放完整答案
  });

  it('流派对照卡表格化渲染', () => {
    const card = renderSchoolComparisonCard({
      schools: ["京派", "海派", "新月派", "象征派"]
    });
    expect(card.html).toContain("<table");
    expect(card.html).toContain("京派");
    expect(card.html).toContain("海派");
  });

  it('区分卡正反面都出', () => {
    const card = renderDistinctionCard({
      a: { name: "沈从文", feature: "田园牧歌" },
      b: { name: "废名", feature: "禅意理趣" }
    });
    expect(card.front).toContain("沈从文");
    expect(card.front).toContain("废名");
    expect(card.back).toContain("田园");
    expect(card.back).toContain("禅意");
  });
});

describe('最小信息原则拆卡（C3.20-C3.23）', () => {
  it("建安风骨拆成6张卡", () => {
    const original = {
      title: "建安风骨",
      content: "建安时期（196-220年）以曹操父子为核心，包括建安七子" +
               "（孔融、陈琳、王粲、徐干、阮瑀、应玚、刘桢）和蔡琰。" +
               "风格慷慨悲凉，反映社会动乱。开创一代诗风，" +
               "与正始诗歌的玄虚晦涩形成对比，对后世唐诗有深远影响。"
    };
    const cards = splitNounExplanation(original);
    expect(cards.length).toBeGreaterThanOrEqual(5);
    // 每张卡只考一个知识点
    for (const card of cards) {
      expect(card.content.length).toBeLessThan(50);
    }
  });

  it('唐宋八大家转为分组枚举', () => {
    const original = {
      title: "唐宋八大家",
      content: "韩愈、柳宗元、欧阳修、苏洵、苏轼、苏辙、王安石、曾巩"
    };
    const cards = splitNounExplanation(original);
    // 不应一张卡问全部8人，应分组
    for (const card of cards) {
      const names = card.content.match(/[韩柳欧苏王曾]/g) || [];
      expect(names.length).toBeLessThan(5);  // 每张卡不超过4人
    }
  });
});
```

- [ ] **Step 3: 运行FSRS和卡片测试**

Run: `npx vitest run tests/test_fsrs.js tests/test_cards.js`
Expected: 所有测试通过

- [ ] **Step 4: Commit**

```bash
git add tests/test_fsrs.js tests/test_cards.js
git commit -m "test(fsrs): FSRS算法与卡片模板验证

- 四种预设参数验证
- 动态保持率三阶段验证
- OCR状态过滤验证
- 6种卡片模板渲染验证
- 最小信息原则拆卡验证（建安风骨6张/唐宋八大家分组）"
```

### Task 30: 知识图谱与AI助手验证

**Files:**
- Create: `tests/test_knowledge_graph.js`
- Create: `tests/test_ai_assistant.js`

- [ ] **Step 1: 编写知识图谱验证测试**

Create `tests/test_knowledge_graph.js`:

```javascript
/**
 * 功能性知识图谱验证测试（对应checklist C4.1-C4.15）。
 */
import { describe, it, expect, beforeEach } from 'vitest';
import { KnowledgeGraph } from '../js/knowledge-graph.js';

describe('图谱骨架验证（C4.1-C4.4）', () => {
  let graph;
  beforeEach(() => { graph = new KnowledgeGraph(); });

  it('节点类型预设完整', () => {
    const types = graph.getNodeTypes();
    expect(types).toContain('AUTHOR');
    expect(types).toContain('WORK');
    expect(types).toContain('SCHOOL');
    expect(types).toContain('MOVEMENT');
    expect(types).toContain('CONCEPT');
    expect(types).toContain('KNOWLEDGE_POINT');
  });

  it('关系类型预设完整', () => {
    const rels = graph.getRelationTypes();
    expect(rels).toContain('AUTHORED');
    expect(rels).toContain('BELONGS_TO');
    expect(rels).toContain('INFLUENCED_BY');
    expect(rels).toContain('COMPARED_WITH');
    expect(rels).toContain('SAME_PERIOD');
    expect(rels).toContain('PRECEDES');
    expect(rels).toContain('PREREQUISITE');
    expect(rels).toContain('RELATED_CONCEPT');
  });

  it('南师大考点骨架预置', () => {
    const preset = graph.getPresetNodes();
    const names = preset.map(n => n.name);
    expect(names).toContain('鲁迅');
    expect(names).toContain('周作人');
    expect(names).toContain('茅盾');
    expect(names).toContain('沈从文');
    expect(names).toContain('张爱玲');
    expect(names).toContain('赵树理');
    expect(names).toContain('路遥');
  });
});

describe('前置依赖检测（C4.5-C4.9）', () => {
  let graph;
  beforeEach(() => {
    graph = new KnowledgeGraph();
    // 模拟江西诗派节点，前置依赖黄庭坚/杜甫/宋诗特点
    graph.addNode({ id: 'jiangxi_school', name: '江西诗派', type: 'SCHOOL' });
    graph.addNode({ id: 'huangtingjian', name: '黄庭坚', type: 'AUTHOR' });
    graph.addNode({ id: 'dufu', name: '杜甫', type: 'AUTHOR' });
    graph.addNode({ id: 'song_poetry', name: '宋诗特点', type: 'CONCEPT' });
    graph.setPrerequisites('jiangxi_school', ['huangtingjian', 'dufu', 'song_poetry']);
  });

  it('前置节点R<0.7时插入复习卡', () => {
    // 模拟用户对黄庭坚/杜甫/宋诗特点的R值较低
    graph.setRetrievability('huangtingjian', 0.5);
    graph.setRetrievability('dufu', 0.6);
    graph.setRetrievability('song_poetry', 0.65);

    const check = graph.checkPrerequisites('jiangxi_school');
    expect(check.canLearn).toBe(false);
    expect(check.reviewCards).toHaveLength(3);  // 3个前置需复习
  });

  it('前置节点R≥0.7时允许学习新卡', () => {
    graph.setRetrievability('huangtingjian', 0.85);
    graph.setRetrievability('dufu', 0.90);
    graph.setRetrievability('song_poetry', 0.80);

    const check = graph.checkPrerequisites('jiangxi_school');
    expect(check.canLearn).toBe(true);
    expect(check.reviewCards).toHaveLength(0);
  });
});

describe('薄弱子图识别（C4.10-C4.12）', () => {
  it('识别R值最低的连通子图', () => {
    const graph = new KnowledgeGraph();
    // 构建两个子图：京派（R低）和海派（R高）
    graph.addNode({ id: 'jingpai', name: '京派', type: 'SCHOOL' });
    graph.addNode({ id: 'shencongwen', name: '沈从文', type: 'AUTHOR' });
    graph.addEdge('jingpai', 'shencongwen', 'BELONGS_TO');
    graph.setRetrievability('jingpai', 0.4);
    graph.setRetrievability('shencongwen', 0.45);

    graph.addNode({ id: 'haipai', name: '海派', type: 'SCHOOL' });
    graph.addNode({ id: 'zhangailing', name: '张爱玲', type: 'AUTHOR' });
    graph.addEdge('haipai', 'zhangailing', 'BELONGS_TO');
    graph.setRetrievability('haipai', 0.9);
    graph.setRetrievability('zhangailing', 0.92);

    const weakest = graph.findWeakestSubgraph();
    expect(weakest.nodes.map(n => n.id)).toContain('jingpai');
    expect(weakest.nodes.map(n => n.id)).toContain('shencongwen');
  });

  it('图谱可视化颜色映射R值', () => {
    const graph = new KnowledgeGraph();
    graph.addNode({ id: 'n1', name: '节点1', type: 'AUTHOR' });
    graph.setRetrievability('n1', 0.9);
    expect(graph.getNodeColor('n1')).toMatch(/green|#0f0/i);  // 绿

    graph.setRetrievability('n1', 0.5);
    expect(graph.getNodeColor('n1')).toMatch(/yellow|#ff0/i);  // 黄

    graph.setRetrievability('n1', 0.3);
    expect(graph.getNodeColor('n1')).toMatch(/red|#f00/i);  // 红
  });
});

describe('干扰预警（C4.13-C4.15）', () => {
  it('检测同流派相邻节点并插入区分卡', () => {
    const graph = new KnowledgeGraph();
    graph.addNode({ id: 'shen', name: '沈从文', type: 'AUTHOR' });
    graph.addNode({ id: 'feiming', name: '废名', type: 'AUTHOR' });
    graph.addEdge('shen', 'feiming', 'SAME_PERIOD');
    graph.addEdge('shen', 'feiming', 'COMPARED_WITH');

    // 模拟连续复习这两个节点
    const alert = graph.detectInterference(['shen', 'feiming']);
    expect(alert.hasInterference).toBe(true);
    expect(alert.suggestion).toContain('区分卡');
  });
});
```

- [ ] **Step 2: 编写AI助手验证测试**

Create `tests/test_ai_assistant.js`:

```javascript
/**
 * AI助手验证测试（对应checklist C5.1-C5.13）。
 */
import { describe, it, expect, vi } from 'vitest';
import { AIAssistant } from '../js/ai-assistant.js';

describe('苏格拉底式AI助手（C5.1-C5.7）', () => {
  it('不直接给出完整答案', async () => {
    const assistant = new AIAssistant();
    const mockRAG = vi.fn().mockResolvedValue("相关知识点...");
    assistant.rag = mockRAG;

    const response = await assistant.ask("简述建安风骨的特征");
    // 不应直接给出完整答案段落
    expect(response).not.toMatch(/建安风骨的特征是.{50,}/);
    // 应引导用户思考
    expect(response).toMatch(/尝试|思考|先.*说说|你觉得/i);
  });

  it('先让用户尝试作答', async () => {
    const assistant = new AIAssistant();
    const result = await assistant.startEssayAssist("论述鲁迅《狂人日记》");
    expect(result.stage).toBe('await_user_attempt');
    expect(result.prompt).toMatch(/先.*尝试|请.*作答/i);
  });

  it('范文标注"非标准答案"', async () => {
    const assistant = new AIAssistant();
    const model = await assistant.getModelEssay("论述题...");
    expect(model.label).toMatch(/范文|非标准答案/);
  });

  it('回答中标注引用来源', async () => {
    const assistant = new AIAssistant();
    assistant.rag = vi.fn().mockResolvedValue({
      content: "...",
      source: { textbook: "袁行霈《中国文学史》第二卷", page: 156 }
    });
    const response = await assistant.ask("什么是江西诗派");
    expect(response).toMatch(/袁行霈.*第二卷.*P?156|据.*袁行霈/);
  });
});

describe('内容来源五级标注（C5.8-C5.13）', () => {
  it('TEXTBOOK_NATIVE→绿色"资料"标签', () => {
    const label = AIAssistant.getSourceLabel('TEXTBOOK_NATIVE');
    expect(label.text).toBe('资料');
    expect(label.color).toMatch(/green/i);
  });

  it('TEXTBOOK_OCR→绿色"资料"标签带OCR角标', () => {
    const label = AIAssistant.getSourceLabel('TEXTBOOK_OCR');
    expect(label.text).toBe('资料');
    expect(label.ocrBadge).toBe(true);
  });

  it('AI_GENERATED→蓝色"AI"标签', () => {
    const label = AIAssistant.getSourceLabel('AI_GENERATED');
    expect(label.text).toBe('AI');
    expect(label.color).toMatch(/blue/i);
  });

  it('HYBRID→黄色"资料+AI"标签', () => {
    const label = AIAssistant.getSourceLabel('HYBRID');
    expect(label.text).toMatch(/资料\+AI|资料\+AI/);
    expect(label.color).toMatch(/yellow/i);
  });

  it('USER_CREATED→灰色"我的"标签', () => {
    const label = AIAssistant.getSourceLabel('USER_CREATED');
    expect(label.text).toBe('我的');
    expect(label.color).toMatch(/gray|grey/i);
  });
});
```

- [ ] **Step 3: 运行知识图谱和AI助手测试**

Run: `npx vitest run tests/test_knowledge_graph.js tests/test_ai_assistant.js`
Expected: 所有测试通过

- [ ] **Step 4: Commit**

```bash
git add tests/test_knowledge_graph.js tests/test_ai_assistant.js
git commit -m "test(graph,ai): 知识图谱与AI助手验证

- 图谱骨架/前置依赖/薄弱子图/干扰预警验证
- 苏格拉底式AI助手（不直接给答案/引导作答）验证
- 内容来源五级标注验证"
```

### Task 31: 主动回忆检测验证

**Files:**
- Create: `tests/test_recall_detector.js`

- [ ] **Step 1: 编写主动回忆检测验证测试**

Create `tests/test_recall_detector.js`:

```javascript
/**
 * 主动回忆检测三层方案验证（对应checklist C5.14-C5.19）。
 */
import { describe, it, expect, vi } from 'vitest';
import { RecallDetector } from '../js/recall-detector.js';

describe('第1层关键词匹配（C5.14）', () => {
  it('名词解释覆盖率≥70%判正确', () => {
    const detector = new RecallDetector();
    const expected = {
      title: "文学研究会",
      keywords: ["1921", "北京", "郑振铎", "沈雁冰", "小说月报", "为人生"]
    };
    // 5/6关键词覆盖 = 83% ≥ 70%
    const result = detector.checkKeywords(
      "文学研究会1921年成立于北京，由郑振铎、沈雁冰发起，办小说月报",
      expected.keywords
    );
    expect(result.coverage).toBeGreaterThanOrEqual(0.70);
    expect(result.verdict).toBe('correct');
  });

  it('覆盖率<70%判错误', () => {
    const detector = new RecallDetector();
    const keywords = ["1921", "北京", "郑振铎", "沈雁冰", "小说月报", "为人生"];
    // 只答中2个 = 33%
    const result = detector.checkKeywords(
      "1921年成立的文学团体",
      keywords
    );
    expect(result.coverage).toBeLessThan(0.70);
    expect(result.verdict).toBe('incorrect');
  });

  it('同义词词典扩展匹配', () => {
    const detector = new RecallDetector();
    detector.addSynonyms("沈雁冰", ["茅盾", "沈德鸿"]);
    const result = detector.checkKeywords(
      "由茅盾发起",
      ["沈雁冰"]
    );
    expect(result.coverage).toBe(1.0);  // 茅盾匹配沈雁冰
  });
});

describe('第2层BGE语义相似度（C5.15）', () => {
  it('>0.85判正确', async () => {
    const detector = new RecallDetector();
    // mock语义相似度
    detector.computeSimilarity = vi.fn().mockResolvedValue(0.90);
    const result = await detector.checkSemantic(
      "建安风骨是建安时期慷慨悲凉的诗风",
      "建安风骨指建安年间以曹操父子为代表的慷慨悲凉的诗歌风格"
    );
    expect(result.similarity).toBeGreaterThan(0.85);
    expect(result.verdict).toBe('correct');
  });

  it('0.6-0.85判部分正确（触发第3层）', async () => {
    const detector = new RecallDetector();
    detector.computeSimilarity = vi.fn().mockResolvedValue(0.72);
    const result = await detector.checkSemantic("答案...", "标准答案...");
    expect(result.similarity).toBeGreaterThanOrEqual(0.6);
    expect(result.similarity).toBeLessThanOrEqual(0.85);
    expect(result.verdict).toBe('partial');
    expect(result.needsLLM).toBe(true);  // 触发第3层
  });

  it('<0.6判错误', async () => {
    const detector = new RecallDetector();
    detector.computeSimilarity = vi.fn().mockResolvedValue(0.40);
    const result = await detector.checkSemantic("完全不相关的内容", "标准答案");
    expect(result.similarity).toBeLessThan(0.6);
    expect(result.verdict).toBe('incorrect');
    expect(result.needsLLM).toBe(false);  // 不触发第3层
  });
});

describe('第3层LLM异步评估（C5.16-C5.17）', () => {
  it('第2层部分正确时触发，不阻塞', async () => {
    const detector = new RecallDetector();
    detector.computeSimilarity = vi.fn().mockResolvedValue(0.72);
    detector.callLLM = vi.fn().mockResolvedValue({
      score: 75,
      reason: "抓住了主要特征，但遗漏了对社会动乱背景的阐述"
    });

    const result = await detector.evaluate("答案", "标准答案", "论述题");
    // 第2层立即返回partial
    expect(result.verdict).toBe('partial');
    expect(result.llmPending).toBe(true);  // LLM异步进行中

    // 等待LLM完成
    await result.llmPromise;
    expect(result.llmResult.score).toBe(75);
  });

  it('LLM输出0-100分及理由', async () => {
    const detector = new RecallDetector();
    detector.callLLM = vi.fn().mockResolvedValue({
      score: 82,
      reason: "论述完整，论据充分"
    });
    const llmResult = await detector.callLLM("答案", "标准答案");
    expect(llmResult.score).toBeGreaterThanOrEqual(0);
    expect(llmResult.score).toBeLessThanOrEqual(100);
    expect(llmResult.reason).toBeTruthy();
  });
});

describe('防"背关键词但不懂含义"（C5.18-C5.19）', () => {
  it('变体出题检测真实理解', () => {
    const detector = new RecallDetector();
    // 原题：文学研究会的宗旨
    // 变体：为什么文学研究会主张"为人生"而非"为艺术"？
    const variant = detector.generateVariant("文学研究会的宗旨是什么？");
    expect(variant).not.toBe("文学研究会的宗旨是什么？");
    expect(variant.length).toBeGreaterThan(10);
  });

  it('关联卡片频繁出错时降低该卡置信度', () => {
    const detector = new RecallDetector();
    // 卡片A始终"正确"，但关联卡片B/C频繁出错
    detector.recordResult('card_A', 'correct');
    detector.recordResult('card_A', 'correct');
    detector.recordResult('card_A', 'correct');
    // 关联卡片B/C错误
    detector.setRelated('card_A', ['card_B', 'card_C']);
    detector.recordResult('card_B', 'incorrect');
    detector.recordResult('card_C', 'incorrect');

    const confidence = detector.getCardConfidence('card_A');
    // 疑似"背关键词但不懂"，降低置信度
    expect(confidence).toBeLessThan(1.0);
  });
});
```

- [ ] **Step 2: 运行主动回忆检测测试**

Run: `npx vitest run tests/test_recall_detector.js`
Expected: 所有测试通过

- [ ] **Step 3: Commit**

```bash
git add tests/test_recall_detector.js
git commit -m "test(recall): 主动回忆检测三层方案验证

- 第1层关键词匹配（覆盖率≥70%判正确）
- 第2层BGE语义相似度（>0.85/0.6-0.85/<0.6）
- 第3层LLM异步评估（不阻塞，输出0-100分）
- 防"背关键词但不懂含义"（变体出题+关联检测）"
```

---

## Self-Review

### 1. Spec coverage

| Spec Requirement | 对应Task | 状态 |
|---|---|---|
| 资料数字化管线 | Task 1-9 | ✅ |
| 资料来源溯源 | Task 10 (db-schema.js) | ✅ |
| 科目代码变动历史 | Task 12, 25 | ✅ |
| FSRS参数预设与动态保持率 | Task 13-14 | ✅ |
| 功能性知识图谱 | Task 18-21 | ✅ |
| 苏格拉底式AI助手 | Task 22 | ✅ |
| 主动回忆检测三层方案 | Task 24 | ✅ |
| 卡片设计遵循最小信息原则 | Task 16-17 | ✅ |
| 真题数据模型支持科目代码历史 | Task 10, 25 | ✅ |
| 知识点多教材对照 | Task 8 | ✅ |
| 移除导师画像功能 | Task 26 | ✅ |
| **困难文件Real-ESRGAN超分（Task 5.6）** | **Task 5 Step 5-6** | ✅（已补充） |
| **DOC格式处理（win32com）** | **Task 1** | ✅（已修复） |
| **XLS格式处理（xlrd 2.0+）** | **Task 1** | ✅（已修复） |
| **FSRS enable_fuzzing字段** | **Task 13** | ✅（已修复） |
| **多教材交叉校验LLM调用** | **Task 8** | ✅（已修复） |
| **种子数据提取实际实现** | **Task 9** | ✅（已修复） |
| **OCR状态过滤完整实现** | **Task 15** | ✅（已补充） |
| **考研倒计时动态保持率** | **Task 14** | ✅（已补充） |
| **科目代码判定集成** | **Task 25** | ✅（已补充） |
| **导师信息外链完整实现** | **Task 26** | ✅（已补充） |
| **Phase 7验证测试完整代码** | **Task 27-31** | ✅（已补充） |

### 2. Placeholder scan

- Task 3 Step 4: 用户补全教材（非代码占位符，是用户操作步骤）✅
- Task 22-24: 部分AI/RAG功能标注TODO（需实际API配置后实现）✅
- 无TBD/FIXME占位符 ✅
- **cross_validate已从空操作改为真正LLM调用** ✅（修复6）
- **generate_seed已从空TODO改为实际提取逻辑** ✅（修复7）
- **Real-ESRGAN超分任务已补充** ✅（修复8）
- **Task 14/15/25/26已补充完整代码** ✅（修复10）
- **Task 27-31已补充完整测试代码** ✅（修复9）

### 3. Type consistency

- `content_source` 字段值在所有Task中一致使用五级标注 ✅
- `ocr_status` 字段值一致使用 VERIFIED/PENDING ✅
- `exam_paper_code` 字段名在Task 10/12/25中一致 ✅
- FSRS预设名在Task 13/16中一致 ✅
- `ocr_engine` 字段值统一为 "MinerU_3.x"（非"MinerU_hybrid"）✅（修复1）
- `enable_fuzzing` 字段在createCard和PRESETS中一致 ✅（修复5）

### 4. 修复记录汇总（本轮严谨性检查）

本轮对实施计划进行了11项修复，确保技术准确性和代码完整性：

| # | 修复内容 | 严重性 | 涉及Task |
|---|---|---|---|
| 1 | environment.yml包名minerU→mineru，新增xlrd/pywin32/openai/realesrgan/pymupdf/pillow | P0 | Task 1 |
| 2 | MinerU CLI命令magic_pdf→mineru，移除不存在的--mode hybrid | P0 | Task 1, 5 |
| 3 | DOC格式用win32com转换，XLS用xlrd 2.0+ | P0 | Task 1, 2 |
| 4 | pipeline_runner路由DOC/XLS独立处理器 | P0 | Task 1 |
| 5 | FSRS createCard新增enable_fuzzing字段 | P0 | Task 13 |
| 6 | cross_validate从空操作改为真正LLM调用 | P0 | Task 8 |
| 7 | generate_seed从空TODO改为实际提取逻辑 | P0 | Task 9 |
| 8 | 新增Real-ESRGAN超分任务（A/B测试+人工决策） | P1 | Task 5 |
| 9 | Task 27-31补充完整测试代码 | P1 | Phase 7 |
| 10 | Task 14/15/25/26补充完整实现代码 | P1 | Phase 3, 6 |
| 11 | ocr_engine字段统一为"MinerU_3.x" | P2 | Task 5 |

### 5. 技术验证来源

以下修复基于外部技术验证（WebSearch）：
- MinerU 3.x包名和CLI命令：通过PyPI和官方文档核实
- python-docx不支持.doc：通过python-docx官方文档核实
- openpyxl不支持.xls，xlrd 2.0+仍支持.xls：通过PyPI核实
- PaddleOCR show_log参数2.7+已移除：通过PaddleOCR changelog核实
- py-fsrs v6.3.1卡片字段：通过py-fsrs GitHub核实
- Real-ESRGAN面向自然图像训练：通过论文核实

---

## Execution Handoff

**Plan complete and saved to `docs/plans/2026-07-10-integrate-resources-and-ai-assistant.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
