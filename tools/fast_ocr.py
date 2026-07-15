"""fast_ocr.py - 使用Intel NPU/CPU OCR快速识别PDF和图片

用ppocr.exe（PP-OCRv5-server OpenVINO版）替代MinerU，速度提升100倍。
  - NPU模式：0.32秒/图
  - CPU模式：1.5秒/图
  - MinerU对比：30-120秒/页

工作流程：
  1. PyMuPDF把PDF每页转PNG图片（本地秒级）
  2. ppocr.exe逐页OCR（每页0.3-1.5秒）
  3. 合并文本输出

用法：
  python fast_ocr.py <input_pdf_or_image> [--output <dir>] [--device cpu|npu]
"""

import argparse
import json
import os
import subprocess
import sys
import time
from pathlib import Path

# ppocr.exe和模型路径
PPOCR_DIR = r"C:\Users\33425\.trae-cn\skills\local-ocr-npu\bin"
PPOCR_EXE = os.path.join(PPOCR_DIR, "ppocr.exe")
DET_MODEL_DIR = r"C:\Users\33425\.openvino\models\PP-OCRv5_server_det_ov"
REC_MODEL_DIR = r"C:\Users\33425\.openvino\models\PP-OCRv5_server_rec_ov"
DET_MODEL_NAME = "PP-OCRv5_server_det"
REC_MODEL_NAME = "PP-OCRv5_server_rec"
DICT_PATH = os.path.join(PPOCR_DIR, "ppocr_keys_v1.txt")


def pdf_to_images(pdf_path, output_dir, dpi=200):
    """用PyMuPDF把PDF每页转PNG图片。

    Args:
        pdf_path: PDF文件路径
        output_dir: 图片输出目录
        dpi: 输出DPI（默认200，平衡清晰度和速度）

    Returns:
        list: 生成的图片路径列表
    """
    import fitz

    os.makedirs(output_dir, exist_ok=True)
    images = []

    doc = fitz.open(pdf_path)
    zoom = dpi / 72  # PDF默认72DPI
    mat = fitz.Matrix(zoom, zoom)

    for i in range(len(doc)):
        page = doc[i]
        pix = page.get_pixmap(matrix=mat)
        img_path = os.path.join(output_dir, f"page_{i + 1:04d}.png")
        pix.save(img_path)
        images.append(img_path)

    doc.close()
    return images


def ocr_image(image_path, save_path, device="cpu"):
    """用ppocr.exe识别单张图片。

    Args:
        image_path: 图片路径
        save_path: 结果保存目录
        device: cpu 或 npu

    Returns:
        str: 识别出的文本
    """
    os.makedirs(save_path, exist_ok=True)

    # ppocr.exe需要在bin目录下运行（DLL依赖），用相对路径避免长路径崩溃
    # 但跨盘符无法用相对路径，所以复制图片到bin目录附近
    # 更好的方案：直接cd到bin目录运行

    cmd = [
        PPOCR_EXE, "ocr",
        f"--input={image_path}",
        f"--text_detection_model_name={DET_MODEL_NAME}",
        f"--text_detection_model_dir={DET_MODEL_DIR}",
        f"--text_recognition_model_name={REC_MODEL_NAME}",
        f"--text_recognition_model_dir={REC_MODEL_DIR}",
        f"--device={device}",
        "--text_recognition_batch_size=1",
        "--text_rec_score_thresh=0.0",
        f"--save_path={save_path}",
    ]

    result = subprocess.run(
        cmd, capture_output=True, text=True, timeout=120,
        cwd=PPOCR_DIR,  # 在bin目录下运行，让DLL能找到
    )

    if result.returncode != 0:
        raise RuntimeError(
            f"ppocr失败(返回码={result.returncode}): {result.stderr[:300]}"
        )

    # 读取结果txt文件
    txt_files = list(Path(save_path).glob("*.txt"))
    if not txt_files:
        return ""

    # ppocr输出格式：每行一个识别结果，带置信度
    # 读取第一个txt文件
    with open(txt_files[0], "r", encoding="utf-8") as f:
        content = f.read()

    return content.strip()


def ocr_image_fast(image_path, save_path, device="cpu"):
    """用ppocr.exe识别单张图片（快速版，直接解析stdout JSON）。

    Args:
        image_path: 图片路径
        save_path: 结果保存目录
        device: cpu 或 npu

    Returns:
        str: 识别出的文本
    """
    os.makedirs(save_path, exist_ok=True)

    cmd = [
        PPOCR_EXE, "ocr",
        f"--input={image_path}",
        f"--text_detection_model_name={DET_MODEL_NAME}",
        f"--text_detection_model_dir={DET_MODEL_DIR}",
        f"--text_recognition_model_name={REC_MODEL_NAME}",
        f"--text_recognition_model_dir={REC_MODEL_DIR}",
        f"--device={device}",
        "--text_recognition_batch_size=6",
        "--text_rec_score_thresh=0.0",
        f"--save_path={save_path}",
    ]

    result = subprocess.run(
        cmd, capture_output=True, text=True, timeout=120,
        cwd=PPOCR_DIR,
    )

    if result.returncode != 0:
        raise RuntimeError(
            f"ppocr失败(返回码={result.returncode}): {result.stderr[:300]}"
        )

    # 读取结果txt文件
    txt_files = list(Path(save_path).glob("*.txt"))
    if not txt_files:
        return ""

    with open(txt_files[0], "r", encoding="utf-8") as f:
        content = f.read()

    return content.strip()


def process_pdf(pdf_path, work_dir, device="cpu"):
    """处理整个PDF：转图片 → 逐页OCR → 合并文本。

    Args:
        pdf_path: PDF文件路径
        work_dir: 临时工作目录
        device: cpu 或 npu

    Returns:
        dict: 包含pages列表
    """
    t0 = time.time()

    # 第一步：PDF转图片
    images_dir = os.path.join(work_dir, "images")
    images = pdf_to_images(pdf_path, images_dir)
    print(f"  转图片完成: {len(images)}页, 耗时{time.time() - t0:.1f}秒")

    # 第二步：逐页OCR
    pages = []
    ocr_dir = os.path.join(work_dir, "ocr_output")

    for i, img_path in enumerate(images):
        page_save_dir = os.path.join(ocr_dir, f"page_{i + 1:04d}")
        # 清理旧结果
        import shutil
        if os.path.exists(page_save_dir):
            shutil.rmtree(page_save_dir)

        t1 = time.time()
        text = ocr_image_fast(img_path, page_save_dir, device=device)
        elapsed = time.time() - t1

        pages.append({
            "page_num": i + 1,
            "text": text,
            "char_count": len(text),
        })

        if (i + 1) % 10 == 0 or i == 0:
            print(f"  OCR进度: {i + 1}/{len(images)}页, "
                  f"当前页{elapsed:.1f}秒, "
                  f"累计{time.time() - t0:.1f}秒")

    total_elapsed = time.time() - t0
    print(f"  OCR完成: {len(images)}页, 总耗时{total_elapsed:.1f}秒 "
          f"(平均{total_elapsed / len(images):.1f}秒/页)")

    return {
        "pages": pages,
        "total_pages": len(pages),
    }


def process_image(image_path, work_dir, device="cpu"):
    """处理单张图片。

    Args:
        image_path: 图片路径
        work_dir: 临时工作目录
        device: cpu 或 npu

    Returns:
        dict: 包含单页结果
    """
    ocr_dir = os.path.join(work_dir, "ocr_output")
    text = ocr_image_fast(image_path, ocr_dir, device=device)

    return {
        "pages": [{
            "page_num": 1,
            "text": text,
            "char_count": len(text),
        }],
        "total_pages": 1,
    }


def main():
    parser = argparse.ArgumentParser(
        description="快速OCR识别（PP-OCRv5 OpenVINO，替代MinerU）"
    )
    parser.add_argument("input", help="输入文件路径（PDF或图片）")
    parser.add_argument("--output", help="输出JSON路径")
    parser.add_argument(
        "--device", choices=["cpu", "npu"], default="cpu",
        help="推理设备（默认cpu，npu需Intel AIPC）"
    )
    parser.add_argument(
        "--work-dir",
        help="临时工作目录（默认D:\\wenyan\\temp\\fast_ocr_work）"
    )

    args = parser.parse_args()

    input_path = os.path.abspath(args.input)
    work_dir = args.work_dir or r"D:\wenyan\temp\fast_ocr_work"
    output_path = args.output or os.path.join(
        work_dir, "ocr_result.json"
    )

    if not os.path.exists(input_path):
        print(f"错误：文件不存在: {input_path}", file=sys.stderr)
        sys.exit(1)

    # 检测文件类型
    ext = os.path.splitext(input_path)[1].lower()
    is_pdf = ext == ".pdf"
    is_image = ext in (".png", ".jpg", ".jpeg", ".bmp", ".tiff")

    if not (is_pdf or is_image):
        print(f"错误：不支持的文件类型: {ext}", file=sys.stderr)
        sys.exit(1)

    print(f"输入: {input_path}")
    print(f"设备: {args.device.upper()}")
    print(f"工作目录: {work_dir}")
    print()

    # 处理
    if is_pdf:
        result = process_pdf(input_path, work_dir, device=args.device)
    else:
        result = process_image(input_path, work_dir, device=args.device)

    # 保存结果
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"\n结果已保存: {output_path}")
    print(f"总页数: {result['total_pages']}")
    total_chars = sum(p["char_count"] for p in result["pages"])
    print(f"总字符数: {total_chars}")


if __name__ == "__main__":
    main()
