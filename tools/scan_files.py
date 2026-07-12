"""scan_files.py - 文研App资料文件扫描与清单生成工具

扫描 wenyanziliao 文件夹全目录所有文件，生成 manifest.json 清单文件。
manifest.json 驱动后续 pipeline_runner.py 的断点续传批处理。

功能：
  1. 递归扫描资料目录，收集所有文件
  2. 判断文件类型（pdf/docx/doc/xlsx/xls/image/zip）
  3. 对PDF文件用pdfplumber检测类型（native/ocr_layer/scan_only/mixed）
  4. 检测"(1)"后缀重复文件，标记去重（原版优先）
  5. 计算MD5哈希，生成manifest.json

对应 Task 1.2（扫描文件）+ Task 1.4（去重处理）。
"""

import argparse
import hashlib
import json
import os
import re
import sys
from datetime import datetime


# ===== 常量定义 =====

# 文件扩展名到file_type的映射
EXTENSION_MAP = {
    ".pdf": "pdf",
    ".docx": "docx",
    ".doc": "doc",
    ".xlsx": "xlsx",
    ".xls": "xls",
    ".jpg": "image",
    ".jpeg": "image",
    ".png": "image",
    ".bmp": "image",
    ".tiff": "image",
    ".gif": "image",
    ".zip": "zip",
}

# PDF类型中需要OCR的类型
OCR_REQUIRED_PDF_TYPES = {"scan_only", "mixed"}

# "(1)"后缀重复文件的正则匹配模式（半角括号）
DUPLICATE_PATTERN = re.compile(r"^(.+)\(1\)$")


# ===== 辅助函数 =====

def calculate_md5(file_path, chunk_size=8192):
    """计算文件的MD5哈希值。

    Args:
        file_path: 文件绝对路径
        chunk_size: 读取块大小（字节）

    Returns:
        MD5十六进制字符串
    """
    md5 = hashlib.md5()
    try:
        with open(file_path, "rb") as f:
            while True:
                chunk = f.read(chunk_size)
                if not chunk:
                    break
                md5.update(chunk)
        return md5.hexdigest()
    except Exception as e:
        print(f"警告：MD5计算失败 {file_path}: {e}", file=sys.stderr)
        return ""


def get_file_type(file_name):
    """根据文件扩展名判断file_type。

    Args:
        file_name: 文件名（含扩展名）

    Returns:
        file_type字符串（pdf/docx/doc/xlsx/xls/image/zip/unknown）
    """
    _, ext = os.path.splitext(file_name)
    ext_lower = ext.lower()
    return EXTENSION_MAP.get(ext_lower, "unknown")


def extract_category(relative_path):
    """从相对路径推断文件分类（从顶级目录名去除序号前缀）。

    例如："3.古代文学/袁行霈版本/xxx.pdf" → "古代文学"
         "丁帆版本现当代笔记.pdf" → "未分类"

    Args:
        relative_path: 相对于资料根目录的路径

    Returns:
        分类名称字符串
    """
    parts = relative_path.replace("\\", "/").split("/")
    if len(parts) <= 1:
        return "未分类"
    top_dir = parts[0]
    # 去除 "N." 前缀（如 "3.古代文学" → "古代文学"）
    if "." in top_dir:
        prefix, rest = top_dir.split(".", 1)
        if prefix.isdigit():
            return rest
    return top_dir


def detect_pdf_type(pdf_path):
    """用pdfplumber检测PDF类型。

    判断逻辑：
      - 每页平均字符>100 → native（原生电子文本）
      - >0但<100 → ocr_layer（有OCR文本层但可能错字）
      - =0 → scan_only（纯扫描件，无文本层）
      - 混合页（部分页有文本部分页无文本） → mixed

    Args:
        pdf_path: PDF文件绝对路径

    Returns:
        tuple: (pdf_type, needs_ocr)
            pdf_type: "native"|"ocr_layer"|"scan_only"|"mixed"
            needs_ocr: bool，是否需要OCR处理
    """
    try:
        import pdfplumber
    except ImportError:
        print("警告：pdfplumber未安装，PDF类型默认标记为scan_only。"
              "请在conda环境ocr中运行此脚本。", file=sys.stderr)
        return "scan_only", True

    try:
        with pdfplumber.open(pdf_path) as pdf:
            if len(pdf.pages) == 0:
                return "scan_only", True

            page_char_counts = []
            for page in pdf.pages:
                text = page.extract_text() or ""
                page_char_counts.append(len(text.strip()))

            total_chars = sum(page_char_counts)
            total_pages = len(page_char_counts)
            has_text_pages = sum(1 for c in page_char_counts if c > 0)
            no_text_pages = total_pages - has_text_pages

            # 所有页都无文本 → scan_only
            if has_text_pages == 0:
                return "scan_only", True

            # 部分页有文本，部分无 → mixed
            if no_text_pages > 0:
                return "mixed", True

            # 所有页都有文本，按平均字符数判断
            avg_chars = total_chars / total_pages
            if avg_chars > 100:
                return "native", False
            else:
                return "ocr_layer", True

    except Exception as e:
        # PDF损坏或加密，保守标记为scan_only（后续OCR会重试）
        print(f"警告：PDF类型检测失败 {os.path.basename(pdf_path)}: {e}",
              file=sys.stderr)
        return "scan_only", True


def is_duplicate_name(file_stem):
    """检查文件名（不含扩展名）是否带有"(1)"后缀（重复文件标记）。

    Args:
        file_stem: 文件名（不含扩展名）

    Returns:
        如果是重复文件名，返回原版文件名；否则返回None
    """
    match = DUPLICATE_PATTERN.match(file_stem)
    if match:
        return match.group(1)
    return None


# ===== 核心扫描逻辑 =====

def scan_directory(input_dir):
    """递归扫描目录，收集所有文件信息。

    Args:
        input_dir: 要扫描的根目录绝对路径

    Returns:
        list: 文件记录字典列表（未排序、未分配ID）
    """
    files = []
    for root, dirs, filenames in os.walk(input_dir):
        for filename in filenames:
            # 跳过系统隐藏文件
            if filename.startswith("~$") or filename == "Thumbs.db" \
                    or filename == ".DS_Store":
                continue

            abs_path = os.path.join(root, filename)
            rel_path = os.path.relpath(abs_path, input_dir)
            file_type = get_file_type(filename)

            # 跳过unknown类型文件（不支持的格式）
            if file_type == "unknown":
                print(f"跳过不支持的文件类型: {rel_path}", file=sys.stderr)
                continue

            file_stem, _ = os.path.splitext(filename)
            category = extract_category(rel_path)

            record = {
                "relative_path": rel_path.replace("\\", "/"),
                "absolute_path": abs_path,
                "file_name": filename,
                "file_type": file_type,
                "category": category,
                "file_stem": file_stem,  # 临时字段，用于去重，最终输出时移除
            }
            files.append(record)

    return files


def assign_ids_and_deduplicate(files):
    """为文件分配ID并执行去重处理。

    去重逻辑（Task 1.4）：
      - 检测文件名带"(1)"后缀的文件（如"名词解释(1).pdf"）
      - 查找同目录下的原版文件（如"名词解释.pdf"）
      - 原版优先（is_duplicate=false），重复版标记is_duplicate=true
      - 如果原版不存在，则"(1)"版本不标记为重复

    Args:
        files: 文件记录列表

    Returns:
        list: 处理后的文件记录列表（已分配ID、已去重）
    """
    # 按相对路径排序，确保ID分配确定性
    files.sort(key=lambda f: f["relative_path"])

    # 第一步：先为所有文件分配ID
    for idx, f in enumerate(files):
        f["id"] = f"file_{idx + 1:03d}"

    # 第二步：构建 "(目录, 文件名stem) → 文件ID" 的查找表
    # 用于快速查找原版文件
    stem_to_id = {}
    for f in files:
        dir_path = os.path.dirname(f["relative_path"])
        key = (dir_path, f["file_stem"])
        stem_to_id[key] = f["id"]

    # 第三步：执行去重
    for f in files:
        # 检查是否为"(1)"后缀重复文件
        original_stem = is_duplicate_name(f["file_stem"])
        if original_stem is not None:
            dir_path = os.path.dirname(f["relative_path"])
            original_key = (dir_path, original_stem)
            if original_key in stem_to_id:
                f["is_duplicate"] = True
                f["duplicate_of"] = stem_to_id[original_key]
            else:
                # 原版不存在，此文件不视为重复
                f["is_duplicate"] = False
                f["duplicate_of"] = None
        else:
            f["is_duplicate"] = False
            f["duplicate_of"] = None

    return files


def enrich_file_records(files, skip_pdf_detection=False):
    """为文件记录补充完整信息（哈希、PDF类型、OCR需求等）。

    Args:
        files: 文件记录列表
        skip_pdf_detection: 是否跳过PDF类型检测（用于快速扫描模式）

    Returns:
        list: 补充完整信息的文件记录列表
    """
    total = len(files)
    for i, f in enumerate(files):
        print(f"[{i + 1}/{total}] 处理: {f['relative_path']}")

        # 计算MD5哈希
        f["hash"] = calculate_md5(f["absolute_path"])

        # 初始化状态字段
        f["status"] = "pending"
        f["attempts"] = 0

        # PDF类型检测和OCR需求判断
        if f["file_type"] == "pdf":
            if skip_pdf_detection:
                f["pdf_type"] = "scan_only"
                f["needs_ocr"] = True
            else:
                pdf_type, needs_ocr = detect_pdf_type(f["absolute_path"])
                f["pdf_type"] = pdf_type
                f["needs_ocr"] = needs_ocr
            f["needs_super_resolution"] = False
        elif f["file_type"] == "image":
            # 图片用MinerU CLI pipeline后端OCR处理（2026-07-10因PaddleOCR 3.x不兼容改用MinerU）
            f["pdf_type"] = None
            f["needs_ocr"] = True
            f["needs_super_resolution"] = False
        else:
            # docx/doc/xlsx/xls/zip 不需要OCR
            f["pdf_type"] = None
            f["needs_ocr"] = False
            f["needs_super_resolution"] = False

        # 移除临时字段
        del f["file_stem"]

    return files


def generate_manifest(input_dir, output_path, skip_pdf_detection=False):
    """生成manifest.json清单文件。

    Args:
        input_dir: 资料根目录
        output_path: manifest.json输出路径
        skip_pdf_detection: 是否跳过PDF类型检测

    Returns:
        dict: manifest字典
    """
    print(f"开始扫描目录: {input_dir}")

    # 1. 扫描目录
    files = scan_directory(input_dir)
    print(f"发现 {len(files)} 个文件")

    # 2. 分配ID并去重
    files = assign_ids_and_deduplicate(files)

    # 统计去重结果
    dup_count = sum(1 for f in files if f["is_duplicate"])
    if dup_count > 0:
        print(f"检测到 {dup_count} 个重复文件（已标记去重）")

    # 3. 补充完整信息（哈希、PDF类型等）
    files = enrich_file_records(files, skip_pdf_detection)

    # 4. 构建manifest
    manifest = {
        "files": files,
        "total": len(files),
        "scanned_at": datetime.now().isoformat(),
    }

    # 5. 写入JSON文件
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    print(f"\n清单已生成: {output_path}")
    print(f"文件总数: {manifest['total']}")

    # 打印分类统计
    type_stats = {}
    for f in files:
        ft = f["file_type"]
        type_stats[ft] = type_stats.get(ft, 0) + 1
    print("文件类型统计:")
    for ft, count in sorted(type_stats.items()):
        print(f"  {ft}: {count}")

    # 打印PDF类型统计
    pdf_stats = {}
    for f in files:
        if f["file_type"] == "pdf":
            pt = f["pdf_type"]
            pdf_stats[pt] = pdf_stats.get(pt, 0) + 1
    if pdf_stats:
        print("PDF类型统计:")
        for pt, count in sorted(pdf_stats.items()):
            print(f"  {pt}: {count}")

    return manifest


# ===== 命令行入口 =====

def main():
    """命令行入口函数。"""
    parser = argparse.ArgumentParser(
        description="文研App资料文件扫描与清单生成工具。"
                    "扫描wenyanziliao目录，生成manifest.json。",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python scan_files.py
  python scan_files.py --input d:\\wenyan\\wenyanziliao --output d:\\wenyan\\tools\\manifest.json
  python scan_files.py --skip-pdf-detection  # 快速扫描，跳过PDF类型检测
        """,
    )
    parser.add_argument(
        "--input",
        default=r"d:\wenyan\wenyanziliao",
        help="资料根目录路径（默认: d:\\wenyan\\wenyanziliao）",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="manifest.json输出路径（默认: <脚本目录>/manifest.json）",
    )
    parser.add_argument(
        "--skip-pdf-detection",
        action="store_true",
        help="跳过PDF类型检测（快速扫描模式，PDF默认标记为scan_only）",
    )

    args = parser.parse_args()

    # 确定输出路径
    if args.output is None:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        output_path = os.path.join(script_dir, "manifest.json")
    else:
        output_path = args.output

    # 验证输入目录
    if not os.path.isdir(args.input):
        print(f"错误：输入目录不存在: {args.input}", file=sys.stderr)
        sys.exit(1)

    # 生成manifest
    generate_manifest(args.input, output_path, args.skip_pdf_detection)


if __name__ == "__main__":
    main()
