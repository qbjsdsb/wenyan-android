"""rapidocr_pipeline.py - 基于 RapidOCR 的快速高精度 OCR 管线

用 RapidOCR (PP-OCRv6 small ONNX) 替代 MinerU 处理 scan_only/mixed PDF 和图片。
精度优先：DPI=200 + small 模型，置信度 avg 0.98。
速度：6秒/页（ONNX Runtime 内部多线程已用满4核）。

核心特性：
  1. 文件分级：跳过 novel_full（名著全文）和 admin（行政文件），只处理核心资料
  2. 置信度分级：≥0.95 直接入库，0.85-0.95 标记 review，<0.85 送 AI 纠错
  3. 断点续传：每处理完一个文件立即更新 manifest.json
  4. 输出格式兼容 pipeline_runner.py（data.pages[].page_num/text/char_count）
  5. 额外保留置信度信息（data.pages[].avg_score/lines[]）

使用方法：
  python rapidocr_pipeline.py                    # 处理所有 pending 文件
  python rapidocr_pipeline.py --resume           # 断点续传
  python rapidocr_pipeline.py --file file_075    # 只处理指定文件
  python rapidocr_pipeline.py --max-files 5      # 只处理5个文件（测试用）
"""
import argparse
import json
import os
import sys
import time
from datetime import datetime

# 配置 D 盘环境（必须在其他导入之前）
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import d_drive_env  # noqa: F401

import fitz  # PyMuPDF
from rapidocr import RapidOCR

# ===== 常量 =====
MANIFEST_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "manifest.json")
OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "output")
TEMP_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "temp")

# OCR 参数
DPI = 200  # 精度优先，DPI=200 比 150 精度更高（尤其脚注小字）

# 置信度分级阈值
SCORE_HIGH = 0.95    # ≥0.95 直接入库
SCORE_MEDIUM = 0.85  # 0.85-0.95 标记 review，<0.85 送 AI 纠错

# 内容来源
SOURCE_OCR = "TEXTBOOK_OCR"
OCR_PENDING = "PENDING"

# 可跳过的文件分类关键词
SKIP_NOVEL_KEYWORDS = [
    '译文名著文库', '作家参考丛书', '克尔恺郭尔', '阿德勒', '洛伦兹',
    '堂吉诃德', '修女', '细雪', '绿野仙踪', '格列佛', '鲁滨逊',
    '简爱', '傲慢', '战争与和平', '安娜', '复活', '罪与罚',
    '卡拉马佐夫', '百年孤独', '霍乱', '追忆', '变形记', '城堡',
    '审判', '局外人', '鼠疫', '铁幕',
]
SKIP_ADMIN_KEYWORDS = [
    '复试', '录取', '招生', '分数线', '名单', '办法', '参考书目', '基本信息',
]


def should_skip(file_name):
    """判断文件是否可跳过（名著全文或行政文件）。"""
    for k in SKIP_NOVEL_KEYWORDS:
        if k in file_name:
            return 'skip_novel'
    for k in SKIP_ADMIN_KEYWORDS:
        if k in file_name:
            return 'skip_admin'
    return None


def classify_priority(file_name):
    """按重要性分级处理顺序（数值越小优先级越高）。

    1 = 核心教材（文学史）
    2 = 笔记/辅导/习题
    3 = 真题
    4 = 文学理论/批评
    5 = 现当代文学研究
    6 = 其他
    """
    if any(k in file_name for k in ['袁行霈', '中国文学史 第', '马工程', '中国古代文学史',
                                     '郑克鲁', '聂珍钊', '外国文学史', '游国恩', '中国新文学史']):
        return 1
    if any(k in file_name for k in ['笔记', '辅导', '习题', '总结', '汇总', '名词解释', '框架']):
        return 2
    if any(k in file_name for k in ['真题', '试题', '答案', '讲解', '2003年', '2004年',
                                     '2005年', '2006年', '2007年', '2008年', '2009年',
                                     '2010年', '2011年', '2012年', '2013年', '2014年',
                                     '2015年', '2016年', '2017年']):
        return 3
    if any(k in file_name for k in ['文学理论', '文论', '批评', '关键词', '文化研究',
                                     '西方文论', '文学批评']):
        return 4
    return 5


def ocr_pdf_with_rapidocr(engine, pdf_path, file_name):
    """用 RapidOCR 处理 scan_only PDF。

    Args:
        engine: RapidOCR 引擎实例
        pdf_path: PDF 文件绝对路径
        file_name: 文件名（用于日志）

    Returns:
        dict: 提取结果，包含 pages 和置信度信息
    """
    doc = fitz.open(pdf_path)
    pages = []
    total_low_score_lines = 0

    for page_idx in range(len(doc)):
        page = doc[page_idx]
        # PDF 转图片（DPI=200）
        mat = fitz.Matrix(DPI / 72, DPI / 72)
        pix = page.get_pixmap(matrix=mat)
        img_path = os.path.join(TEMP_DIR, f"_ocr_page_{page_idx}.png")
        pix.save(img_path)

        # RapidOCR 识别
        result = engine(img_path)

        # 清理临时图片
        try:
            os.remove(img_path)
        except OSError:
            pass

        # 提取文本和置信度（RapidOCR返回的是tuple，需要安全转换）
        txts = list(result.txts) if result.txts else []
        scores = list(result.scores) if result.scores else []
        # 转换为Python float（避免numpy类型问题）
        scores = [float(s) for s in scores]

        lines = []
        for i, (txt, score) in enumerate(zip(txts, scores)):
            lines.append({
                "text": str(txt),
                "score": round(score, 4),
            })

        # 合并页面文本
        page_text = "\n".join(str(t) for t in txts)
        avg_score = sum(scores) / len(scores) if scores else 0.0

        # 统计低置信度行
        low_score_lines = sum(1 for s in scores if s < SCORE_MEDIUM)
        total_low_score_lines += low_score_lines

        pages.append({
            "page_num": page_idx + 1,
            "text": page_text,
            "char_count": len(page_text),
            "avg_score": round(float(avg_score), 4),
            "line_count": len(lines),
            "low_score_lines": low_score_lines,
            "lines": lines if lines else [],
        })

    doc.close()

    # 计算整体置信度
    all_scores = [l["score"] for p in pages for l in p["lines"]]
    overall_avg_score = sum(all_scores) / len(all_scores) if all_scores else 0.0

    # 分级升级：高置信度(≥0.95)直接VERIFIED，低置信度保持PENDING
    ocr_status = "VERIFIED" if overall_avg_score >= SCORE_HIGH else OCR_PENDING

    return {
        "pages": pages,
        "total_pages": len(pages),
        "overall_avg_score": round(overall_avg_score, 4),
        "total_low_score_lines": total_low_score_lines,
        "content_source": SOURCE_OCR,
        "ocr_status": ocr_status,
        "ocr_engine": "RapidOCR-PP-OCRv6-small",
        "ocr_dpi": DPI,
    }


def ocr_image_with_rapidocr(engine, img_path):
    """用 RapidOCR 处理图片。"""
    result = engine(img_path)

    txts = list(result.txts) if result.txts else []
    scores = [float(s) for s in (result.scores or [])]

    lines = []
    for txt, score in zip(txts, scores):
        lines.append({
            "text": str(txt),
            "score": round(score, 4),
        })

    page_text = "\n".join(str(t) for t in txts)
    avg_score = sum(scores) / len(scores) if scores else 0.0
    low_score_lines = sum(1 for s in scores if s < SCORE_MEDIUM)

    ocr_status = "VERIFIED" if avg_score >= SCORE_HIGH else OCR_PENDING

    return {
        "pages": [{
            "page_num": 1,
            "text": page_text,
            "char_count": len(page_text),
            "avg_score": round(float(avg_score), 4),
            "line_count": len(lines),
            "low_score_lines": low_score_lines,
            "lines": lines,
        }],
        "total_pages": 1,
        "overall_avg_score": round(float(avg_score), 4),
        "total_low_score_lines": low_score_lines,
        "content_source": SOURCE_OCR,
        "ocr_status": ocr_status,
        "ocr_engine": "RapidOCR-PP-OCRv6-small",
        "ocr_dpi": DPI,
    }


def process_mixed_pdf(engine, pdf_path):
    """处理 mixed PDF：文本层页用 pdfplumber，扫描页用 RapidOCR。"""
    import pdfplumber

    # 第一步：识别有文本和无文本的页面
    pdfplumber_pages = []
    scan_page_indices = []

    with pdfplumber.open(pdf_path) as pdf:
        for i, page in enumerate(pdf.pages):
            text = (page.extract_text() or "").strip()
            pdfplumber_pages.append(text)
            if len(text) < 10:  # 少于10字符视为扫描页
                scan_page_indices.append(i)

    # 第二步：扫描页用 RapidOCR（保留完整置信度信息）
    mineru_pages = {}  # page_idx -> {text, lines, avg_score, low_score_lines}
    if scan_page_indices:
        doc = fitz.open(pdf_path)
        for page_idx in scan_page_indices:
            page = doc[page_idx]
            mat = fitz.Matrix(DPI / 72, DPI / 72)
            pix = page.get_pixmap(matrix=mat)
            img_path = os.path.join(TEMP_DIR, f"_ocr_mixed_{page_idx}.png")
            pix.save(img_path)

            # 调用ocr_image_with_rapidocr获取完整置信度
            ocr_result = ocr_image_with_rapidocr(engine, img_path)
            page_data = ocr_result["pages"][0] if ocr_result["pages"] else {}
            mineru_pages[page_idx] = page_data

            try:
                os.remove(img_path)
            except OSError:
                pass
        doc.close()

    # 第三步：合并结果
    pages = []
    total_low_score_lines = 0
    all_scores = []
    for i, text in enumerate(pdfplumber_pages):
        if len(text) >= 10:
            pages.append({
                "page_num": i + 1,
                "text": text,
                "char_count": len(text),
                "source": "pdfplumber",
                "avg_score": 1.0,
                "line_count": 0,
                "low_score_lines": 0,
                "lines": [],
            })
            all_scores.append(1.0)
        else:
            ocr_data = mineru_pages.get(i, {})
            ocr_text = ocr_data.get("text", "")
            page_score = ocr_data.get("avg_score", 0.0)
            page_lines = ocr_data.get("lines", [])
            low_sc = ocr_data.get("low_score_lines", 0)
            total_low_score_lines += low_sc
            if page_lines:
                all_scores.extend([l.get("score", 0.0) for l in page_lines])
            pages.append({
                "page_num": i + 1,
                "text": ocr_text,
                "char_count": len(ocr_text),
                "source": "rapidocr",
                "avg_score": page_score,
                "line_count": len(page_lines),
                "low_score_lines": low_sc,
                "lines": page_lines,
            })

    overall_avg = sum(all_scores) / len(all_scores) if all_scores else 0.0
    ocr_status = "VERIFIED" if overall_avg >= SCORE_HIGH else OCR_PENDING

    return {
        "pages": pages,
        "total_pages": len(pages),
        "scan_page_count": len(scan_page_indices),
        "overall_avg_score": round(float(overall_avg), 4),
        "total_low_score_lines": total_low_score_lines,
        "content_source": SOURCE_OCR,
        "ocr_status": ocr_status,
        "ocr_engine": "RapidOCR-PP-OCRv6-small+pdfplumber",
        "ocr_dpi": DPI,
    }


def save_output(file_info, data):
    """保存提取结果到 output 目录。"""
    output_path = os.path.join(OUTPUT_DIR, f"{file_info['id']}.json")
    output = {
        "id": file_info["id"],
        "relative_path": file_info["relative_path"],
        "file_name": file_info["file_name"],
        "file_type": file_info["file_type"],
        "pdf_type": file_info.get("pdf_type"),
        "category": file_info.get("category", ""),
        "content_source": data["content_source"],
        "ocr_status": data["ocr_status"],
        "data": data,
    }
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)
    return output_path


def update_manifest_status(manifest, file_id, status, extra=None):
    """更新 manifest.json 中指定文件的状态（原子写入）。"""
    for f in manifest["files"]:
        if f["id"] == file_id:
            f["status"] = status
            if extra:
                f.update(extra)
            break
    tmp_path = MANIFEST_PATH + ".tmp"
    with open(tmp_path, "w", encoding="utf-8") as fp:
        json.dump(manifest, fp, ensure_ascii=False, indent=2)
    os.replace(tmp_path, MANIFEST_PATH)


def process_with_retry(func, max_retries=3):
    """带指数退避的重试包装器。"""
    last_error = None
    for attempt in range(max_retries):
        try:
            return func(), None
        except Exception as e:
            last_error = str(e)
            if attempt < max_retries - 1:
                wait = 2 ** attempt
                print(f"  重试 {attempt+1}/{max_retries}（等待{wait}秒）: {e}")
                time.sleep(wait)
            else:
                return None, last_error
    return None, last_error


def process_zip_with_rapidocr(engine, zip_path, file_name):
    """用RapidOCR处理zip文件中的PDF。"""
    import zipfile
    import tempfile
    import shutil
    temp_dir = tempfile.mkdtemp(prefix="wenyan_zip_")
    try:
        with zipfile.ZipFile(zip_path, "r") as zf:
            zf.extractall(temp_dir)
        for root, dirs, filenames in os.walk(temp_dir):
            for fn in filenames:
                if fn.lower().endswith(".pdf"):
                    return ocr_pdf_with_rapidocr(engine, os.path.join(root, fn), file_name)
        raise RuntimeError("ZIP中未找到PDF文件")
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)


def main():
    global DPI
    parser = argparse.ArgumentParser(description="RapidOCR 快速高精度 OCR 管线")
    parser.add_argument("--resume", action="store_true", help="断点续传")
    parser.add_argument("--file", type=str, help="只处理指定文件ID（如 file_075）")
    parser.add_argument("--max-files", type=int, help="最多处理多少个文件（测试用）")
    parser.add_argument("--dpi", type=int, default=None, help="OCR DPI（默认200）")
    args = parser.parse_args()

    # 确保目录存在
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    os.makedirs(TEMP_DIR, exist_ok=True)

    # 加载 manifest
    with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
        manifest = json.load(f)

    # 筛选待处理文件
    pending_files = []
    for f in manifest["files"]:
        # 跳过已完成的
        if args.resume and f["status"] == "completed":
            continue
        # 只处理 pending 或 failed
        if f["status"] not in ["pending", "failed"]:
            continue
        # 只处理需要 OCR 的文件类型
        ptype = f.get("pdf_type", "")
        if f["file_type"] == "pdf" and ptype not in ["scan_only", "mixed"]:
            continue
        if f["file_type"] not in ("pdf", "image", "zip"):
            continue
        # 跳过名著和行政文件
        skip = should_skip(f["file_name"])
        if skip:
            # 标记为 skipped
            update_manifest_status(manifest, f["id"], "skipped",
                                   {"skip_reason": skip})
            continue
        pending_files.append(f)

    # 去重：检测带 "(1)" 后缀的重复下载文件（浏览器重复下载产生）
    # 保留不带 "(1)" 的原始文件，标记带 "(1)" 的为 skipped
    duplicates = []
    pending_names = {f["file_name"] for f in pending_files}
    for f in pending_files:
        name = f["file_name"]
        if "(1)." in name:
            base_name = name.replace("(1).", ".")
            if base_name in pending_names:
                update_manifest_status(manifest, f["id"], "skipped",
                                       {"skip_reason": "skip_duplicate",
                                        "duplicate_of": base_name})
                duplicates.append(f)
    if duplicates:
        pending_files = [f for f in pending_files if f not in duplicates]
        print(f"去重：跳过 {len(duplicates)} 个重复下载文件：")
        for f in duplicates:
            print(f"  {f['id']} | {f['file_name']}")

    # 按 --file 过滤
    if args.file:
        pending_files = [f for f in pending_files if f["id"] == args.file]
        if not pending_files:
            print(f"未找到文件: {args.file}")
            return

    # 按优先级排序
    pending_files.sort(key=lambda f: (classify_priority(f["file_name"]), f["id"]))

    # 限制数量
    if args.max_files:
        pending_files = pending_files[:args.max_files]

    if not pending_files:
        print("没有待处理的文件")
        return

    print(f"=== RapidOCR 管线启动 ===")
    print(f"待处理文件: {len(pending_files)} 个")
    print(f"DPI: 200（全部统一，正确率优先）")
    print(f"模型: PP-OCRv6 small")
    print(f"输出目录: {OUTPUT_DIR}")
    print()

    # 初始化 RapidOCR 引擎
    print("初始化 RapidOCR 引擎...")
    t0 = time.time()
    engine = RapidOCR()
    print(f"引擎初始化完成: {time.time() - t0:.1f}秒")
    print()

    # 处理每个文件
    completed = 0
    failed = 0
    total_pages_processed = 0
    total_time = 0

    for i, f in enumerate(pending_files):
        file_id = f["id"]
        file_name = f["file_name"]
        file_type = f["file_type"]
        ptype = f.get("pdf_type", "")
        path = f["absolute_path"]

        # DPI设置：优先使用命令行参数，否则默认200
        if args.dpi is not None:
            DPI = args.dpi
        else:
            DPI = 200

        priority = classify_priority(file_name)
        priority_names = {1: "核心教材", 2: "笔记/辅导", 3: "真题",
                          4: "文学理论", 5: "其他"}
        print(f"[{i+1}/{len(pending_files)}] {file_id}: {file_name[:50]}")
        print(f"  类型: {file_type}/{ptype} | 优先级: P{priority}({priority_names.get(priority, '?')}) | DPI: {DPI}")

        # 检查文件是否存在
        if not os.path.exists(path):
            print(f"  ✗ 文件不存在，跳过")
            update_manifest_status(manifest, file_id, "failed",
                                   {"error": "文件不存在"})
            failed += 1
            continue

        t1 = time.time()

        def _process():
            if file_type == "pdf" and ptype == "scan_only":
                return ocr_pdf_with_rapidocr(engine, path, file_name)
            elif file_type == "pdf" and ptype == "mixed":
                return process_mixed_pdf(engine, path)
            elif file_type == "image":
                return ocr_image_with_rapidocr(engine, path)
            elif file_type == "zip":
                return process_zip_with_rapidocr(engine, path, file_name)
            else:
                raise ValueError(f"不支持的类型: {file_type}/{ptype}")

        data, error = process_with_retry(_process)
        if error:
            elapsed = time.time() - t1
            print(f"  ✗ 失败（重试3次后，{elapsed:.1f}秒）: {error[:80]}")
            update_manifest_status(manifest, file_id, "failed",
                                   {"error": error[:200], "attempts": 3})
            failed += 1
            print()
            continue

        elapsed = time.time() - t1
        n_pages = data["total_pages"]
        avg_score = data.get("overall_avg_score", 0)
        low_lines = data.get("total_low_score_lines", 0)

        # 保存输出
        output_path = save_output(f, data)

        # 更新 manifest
        update_manifest_status(manifest, file_id, "completed", {
            "ocr_engine": "RapidOCR",
            "ocr_score": avg_score,
            "processed_at": datetime.now().isoformat(),
            "result_summary": {
                "output_file": f"{file_id}.json",
                "content_source": data.get("content_source", "UNKNOWN"),
                "ocr_status": data.get("ocr_status", "PENDING"),
            },
        })

        completed += 1
        total_pages_processed += n_pages
        total_time += elapsed

        # 估算剩余时间
        avg_time_per_page = total_time / total_pages_processed if total_pages_processed > 0 else 0
        remaining_files = len(pending_files) - i - 1
        avg_pages_per_file = total_pages_processed / completed if completed > 0 else 0
        remaining_pages = remaining_files * avg_pages_per_file
        eta_seconds = remaining_pages * avg_time_per_page
        eta_hours = eta_seconds / 3600

        print(f"  ✓ {n_pages}页, {elapsed:.1f}秒 ({elapsed/n_pages:.1f}秒/页)")
        print(f"  置信度: avg={avg_score:.4f}, 低分行={low_lines}")
        print(f"  保存: {os.path.basename(output_path)}")
        if remaining_files > 0:
            print(f"  剩余: {remaining_files}文件, 预计{eta_hours:.1f}小时")
        print()

    # 汇总
    print("=" * 50)
    print(f"处理完成")
    print(f"  成功: {completed}")
    print(f"  失败: {failed}")
    print(f"  总页数: {total_pages_processed}")
    print(f"  总耗时: {total_time/3600:.1f}小时")
    if total_pages_processed > 0:
        print(f"  平均速度: {total_time/total_pages_processed:.2f}秒/页")


if __name__ == "__main__":
    main()
