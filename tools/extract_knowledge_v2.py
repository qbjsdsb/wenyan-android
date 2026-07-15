"""extract_knowledge_v2.py - 文研App知识提取与结构化（v2）

v5改进（对比extract_knowledge.py）：
  1. 按页累积切块（TOC检测+尾页过滤+页眉页脚清理），替代按页单块
  2. subject字段使用全称（"中国古代文学"而非"古代文学"）
  3. 输出relative_path字段（供cross_validate识别教材来源）
  4. 排除file_087（郑克鲁作品选，非教材正文）

使用方法：
  python extract_knowledge_v2.py --input output/ --output output/knowledge/

环境变量（LLM API配置）:
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式）
  WENYAN_LLM_MODEL    - 模型名（默认deepseek-chat）
"""

import argparse
import json
import os
import re
import sys
from collections import Counter
from datetime import datetime
from typing import Any

# 复用extract_knowledge.py的可复用函数
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from extract_knowledge import (
    call_llm_for_extraction,
    normalize_entities_in_knowledge_point,
    validate_knowledge_point_schema,
    validate_knowledge_point_granularity,
    route_by_confidence,
    deduplicate_knowledge_points,
    generate_sample_for_review,
    ALIAS_NORMALIZATION,
)
# v5.1新增：复用post_correct的结构性噪声清理（15+条OCR变形正则）
from post_correct import clean_structural_noise


# ===== 常量定义 =====

# v5修复：科目名使用全称（对齐Android SeedDataLoader期望）
SUBJECTS_FULL = ["中国古代文学", "中国现当代文学", "外国文学", "文学理论"]

# 科目简称→全称映射（OCR输出的category用简称，需转换为全称）
SUBJECT_NAME_MAP = {
    "古代文学": "中国古代文学",
    "现当代文学": "中国现当代文学",
    "外国文学": "外国文学",
    "文学理论": "文学理论",
}

# 排除的文件（非教材正文，不提取知识点）
EXCLUDED_FILES = {
    "file_087",  # 郑克鲁作品选（文学作品原文集，非文学史论述）
}

# 切块参数
MAX_CHUNK_SIZE = 2000
MIN_CHUNK_SIZE = 800


# ===== 页面预处理 =====

def detect_toc_pages(pages: list[dict]) -> int:
    """检测目录页范围，返回目录最后一页的页码（0表示无目录）。

    目录页特征（必须同时满足多个）：
      1. 在前30页内
      2. 含"目录"字样 或 含大量"……页码"模式
      3. 整页都是目录条目

    Args:
        pages: 页面列表

    Returns:
        int: 目录最后一页的页码（1-based），0表示无目录
    """
    toc_pages = []
    check_range = min(30, len(pages))

    for i in range(check_range):
        page = pages[i]
        text = page.get("text", "")
        char_count = page.get("char_count", 0)
        lines = [l.strip() for l in text.split("\n") if l.strip()]

        if not lines:
            continue

        has_toc_marker = "目录" in text or "目  录" in text or "目 录" in text

        toc_line_pattern = re.compile(r".{2,40}[……\.\.\.]+\s*\d+\s*$")
        toc_lines = sum(1 for l in lines if toc_line_pattern.match(l))
        toc_line_ratio = toc_lines / len(lines) if lines else 0
        dot_count = text.count("……") + text.count("...")

        is_toc = False
        if has_toc_marker and char_count < 2000:
            is_toc = True
        elif toc_line_ratio >= 0.5 and dot_count >= 5:
            is_toc = True
        elif dot_count >= 10 and char_count < 1500:
            is_toc = True

        if is_toc:
            toc_pages.append(i + 1)

    return max(toc_pages) if toc_pages else 0


def detect_tail_pages(pages: list[dict]) -> int:
    """检测尾页（版权/参考文献/研修书目等），返回尾页起始页码（0-based）。

    尾页特征：含"版权"/"参考文献"/"研修书目"/"文学史年表"/"后记"等关键词。

    Args:
        pages: 页面列表

    Returns:
        int: 尾页起始索引（0-based），len(pages)表示无尾页
    """
    tail_markers = [
        "版权", "参考文献", "研修书目", "文学史年表",
        "后记", "封面", "出版说明", "印刷",
    ]

    # 从后往前扫描最后20页
    check_start = max(0, len(pages) - 20)
    for i in range(len(pages) - 1, check_start - 1, -1):
        text = pages[i].get("text", "")
        if any(marker in text for marker in tail_markers):
            return i

    return len(pages)


def detect_header_footer(pages: list[dict]) -> tuple[set[str], set[str]]:
    """检测跨页重复的页眉页脚。

    Args:
        pages: 页面列表

    Returns:
        tuple: (header_texts, footer_texts)
    """
    first_lines = []
    last_lines = []
    for p in pages:
        text = p.get("text", "").strip()
        if text:
            lines = [l.strip() for l in text.split("\n") if l.strip()]
            if lines:
                first_lines.append(lines[0])
                if len(lines) > 1:
                    last_lines.append(lines[-1])

    first_counter = Counter(first_lines)
    last_counter = Counter(last_lines)

    headers = {t for t, c in first_counter.items() if c >= 5 and len(t) < 30}
    footers = {t for t, c in last_counter.items() if c >= 5 and len(t) < 30}

    return headers, footers


def clean_page_text(text: str, headers: set[str], footers: set[str]) -> str:
    """清理页眉页脚、页码、水印。"""
    lines = text.split("\n")
    cleaned = []
    for line in lines:
        line = line.strip()
        if not line:
            continue
        if line in headers:
            continue
        if line in footers:
            continue
        # 去除纯页码行
        if re.match(r"^\d+$", line):
            continue
        # 去除"标题+页码"模式的页眉（如"从建安风骨到正始之音85"）
        if re.match(r"^.{2,20}\d{1,3}$", line) and len(line) < 25 and "第" not in line[:3]:
            continue
        # 去除水印（快速路径：简单关键词过滤）
        watermarks = ["扫描全能王", "咨询微信", "dxwxky", "Created with", "CamScanner"]
        if any(wm in line for wm in watermarks):
            continue
        cleaned.append(line)
    # v5.1新增：调用post_correct的结构性噪声清理（覆盖15+条OCR变形正则）
    # 包括"台询以"、"笃学文字考研"等OCR变形，以及纯页码行的多行模式
    result = "\n".join(cleaned)
    result = clean_structural_noise(result)
    return result


# ===== 按页累积切块 =====

def chunk_by_page_accumulation(
    pages: list[dict],
    start_page_idx: int,
    end_page_idx: int,
    headers: set[str],
    footers: set[str],
    max_chunk_size: int = MAX_CHUNK_SIZE,
    min_chunk_size: int = MIN_CHUNK_SIZE,
) -> list[dict[str, Any]]:
    """按页累积切块，2000字/块，遇章节标题新起一块。

    Args:
        pages: 页面列表
        start_page_idx: 起始页索引（0-based）
        end_page_idx: 结束页索引（0-based，不含）
        headers: 页眉集合
        footers: 页脚集合
        max_chunk_size: 每块最大字符数
        min_chunk_size: 每块最小字符数

    Returns:
        list: 块列表，每个块含text/start_page/end_page/char_count
    """
    chunks = []
    current_chunk = ""
    current_start_page = start_page_idx + 1
    current_end_page = start_page_idx + 1

    chapter_pattern = re.compile(r"^第[一二三四五六七八九十百]+[章节]\s*.{0,40}", re.MULTILINE)

    for i in range(start_page_idx, end_page_idx):
        page = pages[i]
        raw_text = page.get("text", "")
        text = clean_page_text(raw_text, headers, footers)

        if not text.strip():
            continue

        chapter_matches = list(chapter_pattern.finditer(text))

        # 如果当前块非空且遇到章节标题，且当前块已够大，先保存
        if current_chunk and chapter_matches and len(current_chunk) >= min_chunk_size:
            chunks.append({
                "text": current_chunk,
                "start_page": current_start_page,
                "end_page": current_end_page,
                "char_count": len(current_chunk),
            })
            current_chunk = text
            current_start_page = i + 1
            current_end_page = i + 1
            continue

        # 累积文本
        if current_chunk:
            current_chunk += "\n\n" + text
        else:
            current_chunk = text
            current_start_page = i + 1

        current_end_page = i + 1

        # 超过max_chunk_size时切块
        if len(current_chunk) >= max_chunk_size:
            chunks.append({
                "text": current_chunk,
                "start_page": current_start_page,
                "end_page": current_end_page,
                "char_count": len(current_chunk),
            })
            current_chunk = ""

    # 最后一块
    if current_chunk:
        if len(current_chunk) >= min_chunk_size or not chunks:
            chunks.append({
                "text": current_chunk,
                "start_page": current_start_page,
                "end_page": current_end_page,
                "char_count": len(current_chunk),
            })
        elif chunks:
            chunks[-1]["text"] += "\n\n" + current_chunk
            chunks[-1]["end_page"] = current_end_page
            chunks[-1]["char_count"] = len(chunks[-1]["text"])

    return chunks


# ===== LLM提取prompt（v5：使用全称subject）=====

LLM_EXTRACT_PROMPT_V2 = """你是一个文学考研知识提取专家。请从以下文本中提取结构化知识点。

【提取规则】
1. 每个知识点应为一道考研名词解释/简答题答案级别（50-150字）
2. 严格按JSON Schema输出
3. 标注置信度（0-1），反映知识点提取的准确性和完整性
4. 标注来源引用（source_ref，如"第二卷P156"）
5. 识别实体（作家名/作品名/流派名/术语）并归一化别名
6. 抽取关系（作者-作品/流派-成员/影响-被影响/并称）

【科目分类（必须使用以下精确名称）】
- 中国古代文学：先秦至近代文学
- 中国现当代文学：1917年至今文学
- 外国文学：世界各国文学
- 文学理论：文学理论/批评/美学

【输入文本】
{text}

【来源信息】
- 文件名: {file_name}
- 科目: {category}

【输出JSON Schema】
{{
  "knowledge_points": [
    {{
      "title": "知识点标题",
      "summary": "一句话概括（≤30字）",
      "core_conclusion": "核心结论/答题基准（50-150字）",
      "full_content": "完整内容（可超过150字）",
      "subject": "中国古代文学|中国现当代文学|外国文学|文学理论",
      "tags": ["标签1", "标签2"],
      "difficulty": 1-5,
      "confidence": 0.0-1.0,
      "source_ref": "来源引用",
      "entities": [
        {{"name": "实体名", "type": "AUTHOR|WORK|SCHOOL|CONCEPT", "normalized": "归一化名"}}
      ],
      "relations": [
        {{"from": "实体名", "relation": "AUTHORED|BELONGS_TO|INFLUENCED_BY|...", "to": "实体名"}}
      ]
    }}
  ]
}}

请只输出JSON，不要输出其他内容。"""


def call_llm_for_extraction_v2(
    text: str,
    file_name: str,
    category: str,
    api_config: dict | None = None,
) -> dict[str, Any]:
    """调用LLM提取知识点（v5：使用全称subject的prompt）。"""
    if api_config is None:
        api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
        api_url = os.environ.get("WENYAN_LLM_API_URL", "")
        model = os.environ.get("WENYAN_LLM_MODEL", "deepseek-chat")
    else:
        api_key = api_config.get("api_key", "")
        api_url = api_config.get("api_url", "")
        model = api_config.get("model", "deepseek-chat")

    if not api_key or not api_url:
        return {"knowledge_points": [], "degraded": True}

    # v5修复：category用全称
    category_full = SUBJECT_NAME_MAP.get(category, category)

    prompt = LLM_EXTRACT_PROMPT_V2.format(
        text=text,
        file_name=file_name,
        category=category_full,
    )

    import requests as _requests
    import time as _time

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
                    "temperature": 0.3,
                    "max_tokens": 8192,
                },
                timeout=60,
            )

            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", 2 ** attempt))
                last_error = "429 Too Many Requests"
                if attempt < max_retries - 1:
                    print(f"  LLM限流，等待{retry_after}秒后重试 ({attempt+1}/{max_retries})")
                    _time.sleep(retry_after)
                    continue
                else:
                    break

            response.raise_for_status()

            result = response.json()
            content = result["choices"][0]["message"]["content"]

            content = content.strip()
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            extraction_result = json.loads(content)
            extraction_result["degraded"] = False
            return extraction_result

        except Exception as e:
            last_error = str(e)
            if attempt < max_retries - 1:
                wait = 2 ** attempt
                print(f"  LLM重试 {attempt+1}/{max_retries}（等待{wait}秒）: {e}")
                _time.sleep(wait)
            else:
                break

    return {
        "knowledge_points": [],
        "degraded": True,
        "error": f"LLM调用失败（重试{max_retries}次）: {last_error}",
    }


# ===== 主处理函数 =====

def process_file_v2(
    file_id: str,
    file_data: dict[str, Any],
    output_dir: str,
    api_config: dict | None = None,
) -> dict[str, Any]:
    """处理单个文件的知识提取（v5版本）。

    改进：
      1. 按页累积切块（TOC检测+尾页过滤+页眉页脚清理）
      2. subject字段使用全称
      3. 输出relative_path字段
      4. 排除file_087（作品选）
    """
    # v5修复：排除作品选
    if file_id in EXCLUDED_FILES:
        return {
            "file_id": file_id,
            "status": "skipped",
            "reason": "排除文件（作品选，非教材正文）",
            "knowledge_points_count": 0,
        }

    file_name = file_data.get("file_name", file_id)
    category = file_data.get("category", "未分类")
    relative_path = file_data.get("relative_path", "")

    # v5修复：category转全称
    category_full = SUBJECT_NAME_MAP.get(category, category)

    # 获取页面数据
    data_inner = file_data.get("data", {})
    pages = data_inner.get("pages", [])

    if not pages:
        return {
            "file_id": file_id,
            "status": "skipped",
            "reason": "无页面数据",
            "knowledge_points_count": 0,
        }

    # 1. 检测目录页
    toc_end = detect_toc_pages(pages)
    content_start_idx = toc_end  # 0-based索引

    # 2. 检测尾页
    tail_start_idx = detect_tail_pages(pages)

    # 3. 检测页眉页脚
    headers, footers = detect_header_footer(pages)

    print(f"  {file_id}: {len(pages)}页, 目录1-{toc_end}, 正文{content_start_idx+1}-{tail_start_idx}, 尾页{tail_start_idx+1}-{len(pages)}")
    print(f"  页眉{len(headers)}个, 页脚{len(footers)}个")

    # 4. 按页累积切块
    chunks = chunk_by_page_accumulation(
        pages, content_start_idx, tail_start_idx, headers, footers
    )

    if not chunks:
        return {
            "file_id": file_id,
            "status": "skipped",
            "reason": "文本切块为空",
            "knowledge_points_count": 0,
        }

    print(f"  切块: {len(chunks)}块, 平均{sum(c['char_count'] for c in chunks)//len(chunks)}字/块")

    # 5. 逐块提取知识点
    all_knowledge_points = []
    all_entities = []
    all_relations = []
    schema_fail_count = 0

    for i, chunk in enumerate(chunks):
        print(f"  处理块 {i+1}/{len(chunks)} (p{chunk['start_page']}-{chunk['end_page']}, {chunk['char_count']}字)...", end="", flush=True)

        result = call_llm_for_extraction_v2(
            chunk["text"], file_name, category, api_config
        )

        if result.get("degraded", False):
            error = result.get("error", "未知错误")
            print(f" 降级跳过: {error}")
            continue

        kps = result.get("knowledge_points", [])
        print(f" 提取{len(kps)}个知识点")

        for kp in kps:
            # JSON Schema校验
            schema_valid, schema_reason = validate_knowledge_point_schema(kp)
            if not schema_valid:
                schema_fail_count += 1
                print(f"    警告：Schema校验失败 chunk{i}: {schema_reason}", file=sys.stderr)
                continue

            # 别名归一化
            kp = normalize_entities_in_knowledge_point(kp)

            # v5修复：强制subject为全称
            kp_subject = kp.get("subject", "")
            kp["subject"] = SUBJECT_NAME_MAP.get(kp_subject, kp_subject)

            # 添加来源信息
            kp["source_file"] = file_name
            kp["source_category"] = category
            kp["source_relative_path"] = relative_path
            kp["source_chunk_idx"] = i
            kp["source_pages"] = f"p{chunk['start_page']}-p{chunk['end_page']}"

            # 粒度控制验证
            kp["granularity_valid"] = validate_knowledge_point_granularity(kp)

            # 置信度分级
            kp["confidence_level"] = route_by_confidence(kp)

            all_knowledge_points.append(kp)
            all_entities.extend(kp.get("entities", []))
            all_relations.extend(kp.get("relations", []))

    # 去重
    all_knowledge_points = deduplicate_knowledge_points(all_knowledge_points)

    # 置信度分级统计
    direct_count = sum(1 for kp in all_knowledge_points if kp["confidence_level"] == "direct")
    review_count = sum(1 for kp in all_knowledge_points if kp["confidence_level"] == "review")
    discard_count = sum(1 for kp in all_knowledge_points if kp["confidence_level"] == "discard")

    # 过滤掉丢弃的知识点
    valid_kps = [kp for kp in all_knowledge_points if kp["confidence_level"] != "discard"]

    # 10%抽样校验
    sample_kps = generate_sample_for_review(valid_kps)

    # 写入提取结果
    result = {
        "file_id": file_id,
        "file_name": file_name,
        "category": category,  # 保留简称（与OCR输出一致）
        "relative_path": relative_path,  # v5新增：供cross_validate使用
        "status": "completed",
        "total_chunks": len(chunks),
        "chunk_source": "page_accumulation_v2",
        "total_knowledge_points": len(all_knowledge_points),
        "schema_fail_count": schema_fail_count,
        "direct_count": direct_count,
        "review_count": review_count,
        "discard_count": discard_count,
        "valid_knowledge_points": valid_kps,
        "entities": all_entities,
        "relations": all_relations,
        "sample_for_review": sample_kps,
        "processed_at": datetime.now().isoformat(),
    }

    result_path = os.path.join(output_dir, f"{file_id}_knowledge.json")
    with open(result_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    return result


# ===== 管线入口 =====

def run_extraction_pipeline_v2(
    input_dir: str,
    output_dir: str,
    api_config: dict | None = None,
    file_filter: list[str] | None = None,
) -> None:
    """运行知识提取管线（v5版本）。

    Args:
        input_dir: 输入目录（post_correct输出或pipeline_runner输出）
        output_dir: 输出目录
        api_config: LLM API配置
        file_filter: 指定处理的文件ID列表（None表示处理全部）
    """
    os.makedirs(output_dir, exist_ok=True)

    # 收集所有待处理文件
    json_files = [
        f for f in os.listdir(input_dir)
        if f.startswith("file_") and f.endswith(".json")
        and "corrected" not in f and "knowledge" not in f and "manual_review" not in f
    ]

    if file_filter:
        # 过滤指定文件
        json_files = [
            f for f in json_files
            if os.path.splitext(f)[0] in file_filter
        ]

    print(f"待处理文件: {len(json_files)}个")

    # 检查API配置
    if api_config is None:
        api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
        api_url = os.environ.get("WENYAN_LLM_API_URL", "")
        if not api_key or not api_url:
            print("警告：LLM API未配置（WENYAN_LLM_API_KEY/WENYAN_LLM_API_URL）", file=sys.stderr)
            print("将降级处理，不提取知识点", file=sys.stderr)

    total_kps = 0
    for json_file in sorted(json_files):
        file_id = os.path.splitext(json_file)[0]

        # 跳过已处理的文件
        output_path = os.path.join(output_dir, f"{file_id}_knowledge.json")
        if os.path.exists(output_path):
            print(f"跳过已处理: {file_id}")
            continue

        file_path = os.path.join(input_dir, json_file)
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                file_data = json.load(f)
        except (json.JSONDecodeError, OSError) as e:
            print(f"警告：跳过损坏的JSON文件 {json_file}: {e}", file=sys.stderr)
            continue

        print(f"\n处理 {file_id} ({file_data.get('file_name', 'N/A')})...")

        result = process_file_v2(file_id, file_data, output_dir, api_config)

        kp_count = result.get("total_knowledge_points", 0)
        valid_count = len(result.get("valid_knowledge_points", []))
        total_kps += valid_count
        print(f"  完成: {kp_count}个知识点（有效{valid_count}个）")

    print(f"\n管线完成: 共提取{total_kps}个有效知识点")


def main():
    parser = argparse.ArgumentParser(description="文研App知识提取v2（按页累积切块+全称subject）")
    parser.add_argument("--input", default="output/", help="输入目录")
    parser.add_argument("--output", default="output/knowledge/", help="输出目录")
    parser.add_argument("--files", nargs="*", help="指定处理的文件ID（如file_078 file_079）")
    args = parser.parse_args()

    file_filter = args.files if args.files else None

    run_extraction_pipeline_v2(args.input, args.output, None, file_filter)


if __name__ == "__main__":
    main()
