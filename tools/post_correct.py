"""post_correct.py - 文研App OCR校对闭环（四级管线）

读取MinerU输出的middle.json置信度score，分级路由OCR结果：
  - score >= 0.9  → 直接入库（高置信度，无需纠错）
  - 0.7 <= score < 0.9 → 送LLM保守纠错（只修形近字，不改语义，不动专名）
  - score < 0.7   → 进人工校对队列（低置信度，不自动处理）

核心功能：
  1. 解析MinerU的middle.json，提取每页/每块的置信度score
  2. 按置信度分级路由（三级路由策略）
  3. LLM保守纠错（prompt约束：只修形近字/不改语义/不动专名，输出JSON含改动明细）
  4. 保留OCR原文做diff比对，防过度修正（改动率<5%）
  5. 修正沉淀到error_dict.json（项目资产，越做越准）

对应 Task 6.1-6.5（spec.md 第185-190行 OCR校对闭环 Scenario）。

使用方法：
  python post_correct.py --input output/ --output output/corrected/ --error-dict output/error_dict.json

依赖：
  - MinerU输出的middle.json（含置信度score字段）
  - LLM API（用于保守纠错，通过环境变量配置API Key）
  - opencc（繁简转换，可选）
"""

import argparse
import difflib
import json
import os
import re
import sys
from datetime import datetime
from typing import Any


# ===== 常量定义 =====

# 置信度分级阈值（对应spec.md第167行）
CONFIDENCE_HIGH = 0.95     # score >= 0.95 直接入库
CONFIDENCE_MEDIUM = 0.85   # 0.85 <= score < 0.95 送LLM纠错
                          # score < 0.85 进人工校对队列

# LLM纠错改动率上限（对应spec.md第73行，改动率<5%）
MAX_CHANGE_RATE = 0.05

# 常见OCR形近字对照表（用于本地快速修正，不依赖LLM）
# 这些是最常见的中文OCR形近字错误
COMMON_OCR_ERRORS = {
    "己": "已",  # "已"常被误识为"己"
    "巳": "已",  # "已"常被误识为"巳"
    "未": "末",  # "末"常被误识为"未"（视上下文）
    "土": "士",  # "士"常被误识为"土"（视上下文）
    "入": "人",  # "人"常被误识为"入"
    "贝": "见",  # "见"常被误识为"贝"
    "乌": "鸟",  # "鸟"常被误识为"乌"
    "兔": "免",  # "免"常被误识为"兔"
    "折": "拆",  # "拆"常被误识为"折"
    "帅": "师",  # "师"常被误识为"帅"
    "刁": "刀",  # "刀"常被误识为"刁"
    "勺": "匀",  # "匀"常被误识为"勺"
}

# 不应被LLM修改的专名标记（文学领域常见专名）
# 这些是文学考研中的核心专名，LLM纠错时不得修改
PROTECTED_NAMES = [
    "袁行霈", "袁世硕", "马工程", "游国恩", "童庆炳", "聂珍钊",
    "丁帆", "钱理群", "周宪", "郑克鲁", "陈文新",
    "黄庭坚", "杜甫", "李白", "苏轼", "苏辙", "苏洵",
    "鲁迅", "周作人", "茅盾", "沈从文", "张爱玲", "赵树理",
    "路遥", "建安风骨", "江西诗派",
]

# LLM纠错prompt模板（对应SubTask 6.3）
LLM_CORRECTION_PROMPT = """你是一个OCR纠错专家。请对以下OCR识别文本进行保守纠错。

【严格规则】
1. 只修形近字（如"己"→"已"、"贝"→"见"等视觉相似的错字）
2. 不改变语义（不增删内容、不调整语序、不替换同义词）
3. 不修改专名（人名/地名/书名/流派名/术语等保持原样）
4. 不修改标点符号（除非明显是OCR导致的标点错乱）
5. 输出JSON格式，含改动明细

【受保护专名】（绝对不得修改）
{protected_names}

【OCR文本】
{ocr_text}

【输出格式】
{{
  "corrected_text": "纠错后的完整文本",
  "changes": [
    {{
      "original": "原文片段",
      "corrected": "修正片段",
      "reason": "形近字修正",
      "position": "大致位置描述"
    }}
  ],
  "change_count": 改动总数,
  "change_rate": 改动率(0-1之间)
}}

请只输出JSON，不要输出其他内容。"""


# ===== MinerU输出解析 =====

def parse_middle_json(middle_json_path: str) -> list[dict[str, Any]]:
    """解析MinerU的middle.json，提取每页/每块的置信度信息。

    MinerU 3.x的middle.json实际结构（实测核实2026-07-10）：
    {
      "pdf_info": [
        {
          "page_idx": 0,  # 可选，部分版本无此字段
          "preproc_blocks": [
            {
              "type": "text",      # title/text/table/image等
              "score": 0.9288,     # 置信度在block层级
              "bbox": [...],
              "lines": [
                {
                  "bbox": [...],
                  "spans": [        # 多了一层spans嵌套
                    {
                      "type": "text",
                      "content": "识别文本",  # 文本在spans[k].content
                      "score": 1.0           # span级置信度
                    }
                  ]
                }
              ]
            }
          ],
          "discarded_blocks": [...]  # 被丢弃的块（header/footer等）
        }
      ]
    }

    Args:
        middle_json_path: middle.json文件路径

    Returns:
        list: 文本块列表，每个块含text/score/page_idx信息
    """
    with open(middle_json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    blocks = []
    pdf_info = data.get("pdf_info", [])

    for page_idx, page in enumerate(pdf_info):
        page_idx_val = page.get("page_idx", page_idx)
        preproc_blocks = page.get("preproc_blocks", [])

        for block in preproc_blocks:
            block_type = block.get("type", "")
            block_score = block.get("score", 0.0)

            # 跳过非文本类块（image等），但保留title/text/table
            if block_type not in ("text", "title", "table"):
                continue

            lines = block.get("lines", [])
            block_texts = []
            span_scores = []

            for line in lines:
                spans = line.get("spans", [])
                for span in spans:
                    content = span.get("content", "") or span.get("text", "")
                    span_score = span.get("score", block_score)
                    if content:
                        block_texts.append(content)
                        span_scores.append(float(span_score))

            if block_texts:
                text = "\n".join(block_texts)
                # 使用span级置信度均值，若无则用block级置信度
                avg_score = sum(span_scores) / len(span_scores) if span_scores else float(block_score)
                blocks.append({
                    "page_idx": page_idx_val,
                    "text": text,
                    "score": avg_score,
                    "block_type": block_type,
                })

    return blocks


def find_middle_json(output_dir: str, input_filename: str = "") -> str | None:
    """在MinerU输出目录中查找middle.json文件。

    MinerU 3.x的实际输出结构：
      output_dir/<stem>/auto/<stem>_middle.json

    也兼容旧版本的可能结构。

    Args:
        output_dir: MinerU输出根目录
        input_filename: 输入文件名（用于构建stem前缀路径）

    Returns:
        middle.json的完整路径，未找到返回None
    """
    stem = os.path.splitext(input_filename)[0] if input_filename else ""

    # MinerU 3.x实际输出路径（带auto子目录和文件名前缀）
    possible_paths = []
    if stem:
        possible_paths.extend([
            os.path.join(output_dir, stem, "auto", f"{stem}_middle.json"),
            os.path.join(output_dir, stem, f"{stem}_middle.json"),
        ])
    possible_paths.extend([
        os.path.join(output_dir, "middle.json"),
        os.path.join(output_dir, "auto", "middle.json"),
    ])

    for path in possible_paths:
        if os.path.exists(path):
            return path

    # 递归搜索任何包含middle的json文件
    for root, dirs, files in os.walk(output_dir):
        for file in files:
            if "middle" in file and file.endswith(".json"):
                return os.path.join(root, file)
    return None


# ===== 置信度分级路由 =====

def route_by_confidence(blocks: list[dict[str, Any]]) -> dict[str, list[dict]]:
    """按置信度分级路由文本块（对应SubTask 6.2）。

    分级策略：
      - score >= 0.9  → "high"（直接入库）
      - 0.7 <= score < 0.9 → "medium"（送LLM纠错）
      - score < 0.7   → "low"（人工校对队列）

    Args:
        blocks: 文本块列表（含text/score字段）

    Returns:
        dict: 分级结果，键为"high"/"medium"/"low"，值为对应文本块列表
    """
    routed = {"high": [], "medium": [], "low": []}

    for block in blocks:
        score = block.get("score", 0.0)
        if score >= CONFIDENCE_HIGH:
            routed["high"].append(block)
        elif score >= CONFIDENCE_MEDIUM:
            routed["medium"].append(block)
        else:
            routed["low"].append(block)

    return routed


# ===== LLM保守纠错 =====

def call_llm_for_correction(text: str, api_config: dict | None = None) -> dict[str, Any]:
    """调用LLM进行保守纠错（对应SubTask 6.3）。

    使用LLM_CORRECTION_PROMPT模板，约束LLM只修形近字/不改语义/不动专名。
    支持多种API服务商（DeepSeek/通义/智谱/月之暗面/OpenAI兼容）。

    Args:
        text: 待纠错的OCR文本
        api_config: API配置字典（含api_key/api_url/model字段），
                    None则从环境变量读取

    Returns:
        dict: LLM返回的纠错结果（corrected_text/changes/change_count/change_rate）

    Raises:
        RuntimeError: LLM API不可用或返回格式错误
    """
    # 从环境变量或配置读取API信息
    if api_config is None:
        api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
        api_url = os.environ.get("WENYAN_LLM_API_URL", "")
        model = os.environ.get("WENYAN_LLM_MODEL", "deepseek-chat")
    else:
        api_key = api_config.get("api_key", "")
        api_url = api_config.get("api_url", "")
        model = api_config.get("model", "deepseek-chat")

    if not api_key or not api_url:
        # API不可用时，返回原文不做修改（降级处理）
        return {
            "corrected_text": text,
            "changes": [],
            "change_count": 0,
            "change_rate": 0.0,
            "degraded": True,
            "reason": "LLM API未配置，跳过纠错",
        }

    # 构建prompt
    prompt = LLM_CORRECTION_PROMPT.format(
        protected_names="、".join(PROTECTED_NAMES),
        ocr_text=text,
    )

    import requests as _requests

    max_retries = 3
    last_error = None
    for attempt in range(max_retries):
        try:
            response = _requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.1,  # 低温度确保保守纠错
                    "max_tokens": 4096,
                },
                timeout=30,
            )

            # 429 Too Many Requests：等待Retry-After后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", 2 ** attempt))
                last_error = f"429 Too Many Requests"
                if attempt < max_retries - 1:
                    print(f"  LLM限流，等待{retry_after}秒后重试 ({attempt+1}/{max_retries})")
                    import time as _time
                    _time.sleep(retry_after)
                    continue
                else:
                    break

            response.raise_for_status()

            result = response.json()
            content = result["choices"][0]["message"]["content"]

            # 解析LLM返回的JSON
            content = content.strip()
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            correction_result = json.loads(content)
            correction_result["degraded"] = False
            return correction_result

        except Exception as e:
            last_error = str(e)
            if attempt < max_retries - 1:
                wait = 2 ** attempt
                print(f"  LLM重试 {attempt+1}/{max_retries}（等待{wait}秒）: {e}")
                import time as _time
                _time.sleep(wait)
            else:
                break

    # LLM调用失败（重试3次后），降级返回原文
    return {
        "corrected_text": text,
        "changes": [],
        "change_count": 0,
        "change_rate": 0.0,
        "degraded": True,
        "reason": f"LLM调用失败（重试{max_retries}次）: {last_error}",
    }


def calculate_change_rate(original: str, corrected: str) -> float:
    """计算纠错改动率（对应SubTask 6.4，防过度修正）。

    改动率 = 不同字符数 / 原文总字符数

    Args:
        original: OCR原文
        corrected: 纠错后文本

    Returns:
        float: 改动率（0-1之间）
    """
    if not original:
        return 0.0

    # 使用difflib计算差异
    matcher = difflib.SequenceMatcher(None, original, corrected)
    ratio = matcher.ratio()  # 相似度0-1
    change_rate = 1.0 - ratio
    return change_rate


def validate_correction(original: str, corrected: str, changes: list[dict]) -> bool:
    """验证纠错结果是否过度修正（对应SubTask 6.4）。

    验证规则：
      1. 改动率 < 5%（MAX_CHANGE_RATE）
      2. 不修改受保护专名
      3. 不改变文本长度差异过大（±20%以内）

    Args:
        original: OCR原文
        corrected: 纠错后文本
        changes: LLM返回的改动明细列表

    Returns:
        bool: True表示通过验证，False表示过度修正
    """
    # 规则1：改动率检查
    change_rate = calculate_change_rate(original, corrected)
    if change_rate >= MAX_CHANGE_RATE:
        return False

    # 规则2：受保护专名检查
    for name in PROTECTED_NAMES:
        if name in original and name not in corrected:
            # 专名被删除或修改
            return False

    # 规则3：长度差异检查（±20%）
    orig_len = len(original)
    corr_len = len(corrected)
    if orig_len > 0:
        length_ratio = abs(corr_len - orig_len) / orig_len
        if length_ratio > 0.2:
            return False

    return True


def generate_diff(original: str, corrected: str) -> str:
    """生成OCR原文与纠错后文本的diff比对（对应SubTask 6.4）。

    Args:
        original: OCR原文
        corrected: 纠错后文本

    Returns:
        str: diff格式的比对结果
    """
    diff = difflib.unified_diff(
        original.splitlines(keepends=True),
        corrected.splitlines(keepends=True),
        fromfile="ocr_original",
        tofile="corrected",
        lineterm="",
    )
    return "".join(diff)


# ===== 错误词典管理 =====

def load_error_dict(error_dict_path: str) -> dict[str, Any]:
    """加载error_dict.json（OCR错误词典，项目资产）。

    Args:
        error_dict_path: error_dict.json文件路径

    Returns:
        dict: 错误词典字典，结构为{"errors": [{"original": "...", "corrected": "...", "count": N, "first_seen": "..."}]}
    """
    if os.path.exists(error_dict_path):
        with open(error_dict_path, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"errors": [], "total_corrections": 0, "last_updated": None}


def save_error_dict(error_dict: dict[str, Any], error_dict_path: str) -> None:
    """保存error_dict.json（对应SubTask 6.5）。

    Args:
        error_dict: 错误词典字典
        error_dict_path: 输出路径
    """
    error_dict["last_updated"] = datetime.now().isoformat()
    # 原子写入
    tmp_path = error_dict_path + ".tmp"
    with open(tmp_path, "w", encoding="utf-8") as f:
        json.dump(error_dict, f, ensure_ascii=False, indent=2)
    os.replace(tmp_path, error_dict_path)


def update_error_dict(error_dict: dict[str, Any], changes: list[dict]) -> dict[str, Any]:
    """将纠错改动沉淀到error_dict.json（对应SubTask 6.5）。

    每条改动记录：original/corrected/count/first_seen/last_seen
    相同的original→corrected映射累计count。

    Args:
        error_dict: 现有错误词典
        changes: 本次纠错的改动列表

    Returns:
        dict: 更新后的错误词典
    """
    # 构建现有错误的查找表（original→corrected → 索引）
    existing_lookup = {}
    for idx, err in enumerate(error_dict["errors"]):
        key = (err["original"], err["corrected"])
        existing_lookup[key] = idx

    now = datetime.now().isoformat()

    for change in changes:
        original = change.get("original", "")
        corrected = change.get("corrected", "")

        if not original or not corrected or original == corrected:
            continue

        key = (original, corrected)
        if key in existing_lookup:
            # 累加计数
            idx = existing_lookup[key]
            error_dict["errors"][idx]["count"] += 1
            error_dict["errors"][idx]["last_seen"] = now
        else:
            # 新增错误记录
            error_dict["errors"].append({
                "original": original,
                "corrected": corrected,
                "count": 1,
                "first_seen": now,
                "last_seen": now,
                "reason": change.get("reason", "形近字修正"),
            })
            existing_lookup[key] = len(error_dict["errors"]) - 1

    # 更新总数
    error_dict["total_corrections"] = sum(e["count"] for e in error_dict["errors"])

    return error_dict


# ===== 人工校对队列 =====

def generate_manual_review_queue(
    low_confidence_blocks: list[dict],
    output_path: str,
) -> None:
    """生成人工校对队列文件（对应spec.md第167行 score<0.7进人工校对队列）。

    Args:
        low_confidence_blocks: 低置信度文本块列表
        output_path: 人工校对队列JSON输出路径
    """
    review_queue = {
        "total_items": len(low_confidence_blocks),
        "items": [
            {
                "page_idx": block.get("page_idx", 0),
                "text": block.get("text", ""),
                "score": block.get("score", 0.0),
                "status": "pending_review",
            }
            for block in low_confidence_blocks
        ],
        "generated_at": datetime.now().isoformat(),
        "instruction": "请人工校对以下低置信度OCR文本，修正后标记status=reviewed",
    }

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(review_queue, f, ensure_ascii=False, indent=2)


# ===== 主处理流程 =====


def parse_rapidocr_pages(pages: list[dict]) -> list[dict[str, Any]]:
    """将RapidOCR的pages/lines结构转换为post_correct内部blocks格式。

    RapidOCR输出：data.pages[].lines[].{text, score}
    转换为：[{text, score, page_idx, block_type}]
    """
    blocks = []
    for page in pages:
        page_num = page.get("page_num", 0)
        for line in page.get("lines", []):
            blocks.append({
                "text": line.get("text", ""),
                "score": float(line.get("score", 0.0)),
                "page_idx": page_num - 1,
                "block_type": "text",
            })
    return blocks


def _route_blocks_by_confidence(
    file_id: str,
    file_name: str,
    category: str,
    content_source: str,
    ocr_status: str,
    blocks: list[dict[str, Any]],
    output_dir: str,
    error_dict: dict[str, Any],
    api_config: dict | None = None,
) -> dict[str, Any]:
    """对已解析的blocks执行置信度分级路由+LLM纠错+人工校对队列。"""
    # 置信度分级路由
    routed = route_by_confidence(blocks)

    # 高置信度块：直接保留
    high_confidence_texts = [b["text"] for b in routed["high"]]

    # 中置信度块：LLM保守纠错
    corrected_blocks = []
    all_changes = []
    over_corrected_count = 0

    for block in routed["medium"]:
        original_text = block["text"]

        llm_result = call_llm_for_correction(original_text, api_config)
        corrected_text = llm_result.get("corrected_text", original_text)
        changes = llm_result.get("changes", [])

        if validate_correction(original_text, corrected_text, changes):
            corrected_blocks.append({
                "page_idx": block["page_idx"],
                "original": original_text,
                "corrected": corrected_text,
                "score": block["score"],
                "changes": changes,
                "change_rate": calculate_change_rate(original_text, corrected_text),
                "diff": generate_diff(original_text, corrected_text),
                "degraded": llm_result.get("degraded", False),
            })
            all_changes.extend(changes)

            if not llm_result.get("degraded", False):
                error_dict = update_error_dict(error_dict, changes)
        else:
            corrected_blocks.append({
                "page_idx": block["page_idx"],
                "original": original_text,
                "corrected": original_text,
                "score": block["score"],
                "changes": [],
                "change_rate": 0.0,
                "diff": "",
                "degraded": False,
                "over_corrected": True,
                "reason": "LLM纠错过度修正，已回退原文",
            })
            over_corrected_count += 1

    # 低置信度块：人工校对队列
    manual_review_path = os.path.join(output_dir, f"{file_id}_manual_review.json")
    generate_manual_review_queue(routed["low"], manual_review_path)

    # 汇总校对结果
    result = {
        "file_id": file_id,
        "file_name": file_name,
        "category": category,
        "content_source": content_source,
        "ocr_status": ocr_status,
        "status": "completed",
        "total_blocks": len(blocks),
        "high_confidence_blocks": len(routed["high"]),
        "medium_confidence_blocks": len(routed["medium"]),
        "low_confidence_blocks": len(routed["low"]),
        "corrected_blocks": len(corrected_blocks),
        "over_corrected_blocks": over_corrected_count,
        "manual_review_blocks": len(routed["low"]),
        "manual_review_file": manual_review_path if routed["low"] else None,
        "high_confidence_text": "\n".join(high_confidence_texts),
        "corrected_details": corrected_blocks,
        "processed_at": datetime.now().isoformat(),
    }

    result_path = os.path.join(output_dir, f"{file_id}_corrected.json")
    with open(result_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    return result


def process_file(
    file_id: str,
    file_data: dict[str, Any],
    output_dir: str,
    error_dict: dict[str, Any],
    api_config: dict | None = None,
) -> dict[str, Any]:
    """处理单个文件的OCR校对闭环。

    流程：
      1. 查找MinerU输出的middle.json
      2. 解析置信度，分级路由
      3. 高置信度→直接保留
      4. 中置信度→LLM保守纠错→验证→沉淀错误词典
      5. 低置信度→人工校对队列

    Args:
        file_id: 文件ID
        file_data: pipeline_runner输出的文件数据
        output_dir: 校对结果输出目录
        error_dict: 错误词典（会被更新）
        api_config: LLM API配置

    Returns:
        dict: 校对结果摘要
    """
    # 提取文件元信息（传递给下游extract_knowledge.py使用）
    file_name = file_data.get("file_name", file_id)
    category = file_data.get("category", "未分类")
    content_source = file_data.get("content_source", "")
    ocr_status = file_data.get("ocr_status", "")

    # 查找middle.json（仅在MinerU处理的scan_only/mixed PDF中存在）
    mineru_output_dir = file_data.get("data", {}).get("mineru_output_dir", "")
    middle_json_path = None

    if mineru_output_dir and os.path.exists(mineru_output_dir):
        middle_json_path = find_middle_json(mineru_output_dir, file_name)

    if middle_json_path is None:
        # 无middle.json：检查是否为RapidOCR输出（有pages/lines结构）
        rapid_pages = file_data.get("data", {}).get("pages", [])
        if rapid_pages and any(p.get("lines") for p in rapid_pages):
            # RapidOCR格式：从pages[].lines[]提取blocks
            blocks = parse_rapidocr_pages(rapid_pages)
            if not blocks:
                return {
                    "file_id": file_id,
                    "file_name": file_name,
                    "category": category,
                    "content_source": content_source,
                    "ocr_status": ocr_status,
                    "status": "skipped",
                    "reason": "RapidOCR输出中无文本行",
                    "corrected_blocks": 0,
                    "manual_review_blocks": 0,
                }
            # 跳转到置信度分级处理（blocks已就绪）
            return _route_blocks_by_confidence(
                file_id, file_name, category, content_source,
                ocr_status, blocks, output_dir, error_dict, api_config,
            )
        # 无middle.json且无RapidOCR pages（NATIVE/DOCX等无需OCR纠错的文件）
        return {
            "file_id": file_id,
            "file_name": file_name,
            "category": category,
            "content_source": content_source,
            "ocr_status": ocr_status,
            "status": "skipped",
            "reason": "无MinerU输出，无需OCR校对",
            "corrected_blocks": 0,
            "manual_review_blocks": 0,
        }

    # 1. 解析middle.json
    blocks = parse_middle_json(middle_json_path)
    if not blocks:
        return {
            "file_id": file_id,
            "file_name": file_name,
            "category": category,
            "content_source": content_source,
            "ocr_status": ocr_status,
            "status": "skipped",
            "reason": "middle.json中无文本块",
            "corrected_blocks": 0,
            "manual_review_blocks": 0,
        }

    # 2. 置信度分级路由+LLM纠错+人工校对
    return _route_blocks_by_confidence(
        file_id, file_name, category, content_source,
        ocr_status, blocks, output_dir, error_dict, api_config,
    )


def run_correction_pipeline(
    input_dir: str,
    output_dir: str,
    error_dict_path: str,
    api_config: dict | None = None,
) -> None:
    """运行OCR校对闭环管线（对应Task 6全部SubTask）。

    遍历input_dir下所有pipeline_runner输出的JSON文件，对需要OCR校对的文件
    执行四级管线处理。

    Args:
        input_dir: pipeline_runner输出目录（含file_xxx.json）
        output_dir: 校对结果输出目录
        error_dict_path: error_dict.json路径
        api_config: LLM API配置
    """
    os.makedirs(output_dir, exist_ok=True)

    # 加载错误词典
    error_dict = load_error_dict(error_dict_path)
    print(f"已加载错误词典：{error_dict['total_corrections']}条记录")

    # 遍历所有处理结果文件
    json_files = [
        f for f in os.listdir(input_dir)
        if f.endswith(".json") and f.startswith("file_") and "corrected" not in f
    ]
    json_files.sort()

    print(f"发现 {len(json_files)} 个待校对文件")
    print()

    success_count = 0
    skip_count = 0
    fail_count = 0

    for i, json_file in enumerate(json_files):
        file_path = os.path.join(input_dir, json_file)
        file_id = json_file.replace(".json", "")

        try:
            with open(file_path, "r", encoding="utf-8") as f:
                file_data = json.load(f)
        except (json.JSONDecodeError, OSError) as e:
            print(f"警告：跳过损坏的JSON文件 {json_file}: {e}", file=sys.stderr)
            continue

        print(f"[{i + 1}/{len(json_files)}] 校对: {file_data.get('file_name', json_file)}")

        try:
            result = process_file(file_id, file_data, output_dir, error_dict, api_config)

            if result["status"] == "completed":
                success_count += 1
                print(f"  完成: {result['corrected_blocks']}块纠错, "
                      f"{result['manual_review_blocks']}块待人工校对")
            else:
                skip_count += 1
                print(f"  跳过: {result['reason']}")

        except Exception as e:
            fail_count += 1
            print(f"  失败: {e}", file=sys.stderr)

        # 每处理10个文件保存一次错误词典（断点保护）
        if (i + 1) % 10 == 0:
            save_error_dict(error_dict, error_dict_path)

    # 最终保存错误词典
    save_error_dict(error_dict, error_dict_path)

    # 打印汇总
    print()
    print("=" * 50)
    print("OCR校对闭环汇总:")
    print(f"  成功校对: {success_count}")
    print(f"  跳过(无需OCR): {skip_count}")
    print(f"  失败: {fail_count}")
    print(f"  错误词典累计: {error_dict['total_corrections']}条")
    print(f"  错误词典保存至: {error_dict_path}")
    print("=" * 50)


# ===== 命令行入口 =====

def main():
    """命令行入口函数。"""
    parser = argparse.ArgumentParser(
        description="文研App OCR校对闭环（四级管线）。"
                    "读取MinerU输出middle.json，按置信度分级路由，"
                    "LLM保守纠错，沉淀错误词典。",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python post_correct.py --input output/ --output output/corrected/
  python post_correct.py --input output/ --output output/corrected/ --error-dict output/error_dict.json

环境变量（LLM API配置）:
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式）
  WENYAN_LLM_MODEL    - 模型名（默认deepseek-chat）

置信度分级:
  score >= 0.9  → 直接入库（高置信度）
  0.7 <= score < 0.9 → LLM保守纠错（中置信度）
  score < 0.7   → 人工校对队列（低置信度）
        """,
    )
    parser.add_argument(
        "--input",
        default=None,
        help="pipeline_runner输出目录（默认: <脚本目录>/output）",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="校对结果输出目录（默认: <脚本目录>/output/corrected）",
    )
    parser.add_argument(
        "--error-dict",
        default=None,
        help="error_dict.json路径（默认: <脚本目录>/output/error_dict.json）",
    )

    args = parser.parse_args()

    # 确定路径
    script_dir = os.path.dirname(os.path.abspath(__file__))
    input_dir = args.input or os.path.join(script_dir, "output")
    output_dir = args.output or os.path.join(script_dir, "output", "corrected")
    error_dict_path = args.error_dict or os.path.join(script_dir, "output", "error_dict.json")

    # 验证输入目录
    if not os.path.isdir(input_dir):
        print(f"错误：输入目录不存在: {input_dir}", file=sys.stderr)
        print(f"请先运行: python {os.path.join(script_dir, 'pipeline_runner.py')}",
              file=sys.stderr)
        sys.exit(1)

    # 运行校对管线
    run_correction_pipeline(input_dir, output_dir, error_dict_path)


if __name__ == "__main__":
    main()
