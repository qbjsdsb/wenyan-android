"""extract_knowledge.py - 文研App知识提取与结构化

从OCR/校对后的文本中提取结构化知识点，生成知识图谱节点和关系。

核心功能：
  1. 按MinerU的content_list.json切块，LLM提取知识点（SubTask 7.1-7.2）
  2. 实体识别（作家名/作品名/流派名/术语）+ 别名归一化（SubTask 7.3）
  3. 关系抽取（作者-作品/流派-成员/影响-被影响/并称）（SubTask 7.4）
  4. 粒度控制（一个知识点=一道考研名词解释/简答题答案级别，50-150字）（SubTask 7.5）
  5. 置信度分级：≥0.9直接入库；0.6-0.9待校；<0.6丢弃（SubTask 7.6）
  6. 10%抽样人工校验（SubTask 7.7）

对应 Task 7.1-7.7（spec.md 知识提取 Scenario）。

使用方法：
  python extract_knowledge.py --input output/corrected/ --output output/knowledge/

依赖：
  - post_correct.py校对后的JSON文件
  - LLM API（通过环境变量配置）
"""

import argparse
import json
import os
import re
import sys
from datetime import datetime
from typing import Any


# ===== 常量定义 =====

# 知识点粒度控制（对应SubTask 7.5）
KNOWLEDGE_POINT_MIN_CHARS = 50
KNOWLEDGE_POINT_MAX_CHARS = 150

# 置信度分级阈值（对应SubTask 7.6）
CONFIDENCE_DIRECT = 0.9   # ≥0.9直接入库
CONFIDENCE_REVIEW = 0.6        # 0.6-0.9待校；<0.6丢弃

# 抽样校验比例（对应SubTask 7.7）
SAMPLE_RATE = 0.1

# 四科分类
SUBJECTS = ["古代文学", "现当代文学", "外国文学", "文学理论"]

# 实体类型
ENTITY_TYPES = {
    "AUTHOR": "作家名",
    "WORK": "作品名",
    "SCHOOL": "流派名",
    "CONCEPT": "术语",
}

# 关系类型（对应设计文档3.2节8种 + spec新增PREREQUISITE）
RELATION_TYPES = {
    "AUTHORED": "作者-作品",
    "BELONGS_TO": "属于-流派",
    "PARTICIPATED_IN": "参与-运动",
    "INFLUENCED_BY": "影响-被影响",
    "COMPARED_WITH": "并称",
    "SAME_PERIOD": "同时期",
    "PRECEDES": "前驱-后继",
    "RELATED_CONCEPT": "相关概念",
    "PREREQUISITE": "前置依赖",
}

# 别名归一化表（对应SubTask 7.3，苏轼=苏东坡=子瞻）
ALIAS_NORMALIZATION = {
    "苏东坡": "苏轼",
    "子瞻": "苏轼",
    "东坡居士": "苏轼",
    "东坡": "苏轼",
    "六一居士": "欧阳修",
    "永叔": "欧阳修",
    "醉翁": "欧阳修",
    "香山居士": "白居易",
    "乐天": "白居易",
    "易安居士": "李清照",
    "稼轩": "辛弃疾",
    "幼安": "辛弃疾",
    "放翁": "陆游",
    "务观": "陆游",
    "山谷道人": "黄庭坚",
    "鲁直": "黄庭坚",
    "涪翁": "黄庭坚",
    "半山": "王安石",
    "介甫": "王安石",
    "临川先生": "王安石",
    "摩诘": "王维",
    "右丞": "王维",
    "太白": "李白",
    "青莲居士": "李白",
    "诗仙": "李白",
    "少陵野老": "杜甫",
    "子美": "杜甫",
    "诗圣": "杜甫",
    "牧之": "杜牧",
    "樊川居士": "杜牧",
    "义山": "李商隐",
    "玉溪生": "李商隐",
    "退之": "韩愈",
    "昌黎先生": "韩愈",
    "子厚": "柳宗元",
    "河东先生": "柳宗元",
    "树人": "鲁迅",
    "豫才": "鲁迅",
    "启明": "周作人",
    "知堂": "周作人",
    "雁冰": "茅盾",
    "沈雁冰": "茅盾",
    "从文": "沈从文",
    "张瑛": "张爱玲",
}

# LLM知识提取prompt模板（对应SubTask 7.2）
LLM_EXTRACT_PROMPT = """你是一个文学考研知识提取专家。请从以下文本中提取结构化知识点。

【提取规则】
1. 每个知识点应为一道考研名词解释/简答题答案级别（50-150字）
2. 严格按JSON Schema输出
3. 标注置信度（0-1），反映知识点提取的准确性和完整性
4. 标注来源引用（source_ref，如"第二卷P156"）
5. 识别实体（作家名/作品名/流派名/术语）并归一化别名
6. 抽取关系（作者-作品/流派-成员/影响-被影响/并称）

【科目分类】
- 古代文学：先秦至近代文学
- 现当代文学：1917年至今文学
- 外国文学：世界各国文学
- 文学理论：文学理论/批评/美学

【输入文本】
{text}

【来源信息】
- 文件名: {file_name}
- 分类: {category}

【输出JSON Schema】
{{
  "knowledge_points": [
    {{
      "title": "知识点标题",
      "summary": "一句话概括（≤30字）",
      "core_conclusion": "核心结论/答题基准（50-150字）",
      "full_content": "完整内容（可超过150字）",
      "subject": "古代文学|现当代文学|外国文学|文学理论",
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


# ===== 文本切块 =====

def load_content_list(content_list_path: str) -> list[dict[str, Any]]:
    """加载MinerU的content_list.json，按块获取文本。

    兼容两种格式：
    - v1扁平格式：[{type, text/table_body, page_idx, ...}, ...]
    - v2按页嵌套格式：[[{type, content:{text/html}, ...}, ...], ...]

    表格HTML提取逻辑与pipeline_runner.py保持一致（td→制表符，tr→换行）。

    Args:
        content_list_path: content_list.json文件路径

    Returns:
        list: 内容块列表，每个块含text/type/page_idx字段
    """
    import html as _html

    with open(content_list_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    # 判断v1（扁平）还是v2（按页嵌套）格式
    is_v2 = bool(data) and isinstance(data[0], list)

    items = []
    if is_v2:
        for page_idx, page_items in enumerate(data):
            for item in page_items:
                if isinstance(item, dict):
                    item.setdefault("page_idx", page_idx)
                    items.append(item)
    else:
        items = data

    blocks = []
    for item in items:
        item_type = item.get("type", "")

        # 跳过非内容块（header/footer/image等）
        if item_type in ("header", "footer", "image"):
            continue

        # 提取文本（兼容v1和v2格式）
        text = ""
        if item_type in ("text", "title"):
            if "content" in item and isinstance(item["content"], dict):
                text = item["content"].get("text", "")
            else:
                text = item.get("text", "")
        elif item_type == "table":
            if "content" in item and isinstance(item["content"], dict):
                table_body = item["content"].get("html", "")
            else:
                table_body = item.get("table_body", "")
            if table_body:
                # 统一表格HTML提取逻辑（与pipeline_runner.py一致）
                t = re.sub(r"<td[^>]*>", "", table_body)
                t = t.replace("</td>", "\t")
                t = re.sub(r"<tr[^>]*>", "", t)
                t = t.replace("</tr>", "\n")
                t = re.sub(r"<[^>]+>", "", t)
                text = _html.unescape(t).strip()

        if text.strip():
            blocks.append({
                "text": text,
                "type": item_type,
                "page_idx": item.get("page_idx", 0),
                "text_level": item.get("text_level", 0),
            })

    return blocks


def find_content_list(mineru_output_dir: str, file_name: str = "") -> str | None:
    """在MinerU输出目录中查找content_list.json文件。

    MinerU 3.x的实际输出结构：
      mineru_output_dir/<stem>/auto/<stem>_content_list.json

    注意：content_list_v2.json是新版格式（含content嵌套），优先使用原版content_list.json。

    Args:
        mineru_output_dir: MinerU输出根目录
        file_name: 输入文件名（用于构建stem前缀路径）

    Returns:
        content_list.json的完整路径，未找到返回None
    """
    stem = os.path.splitext(file_name)[0] if file_name else ""

    possible_paths = []
    if stem:
        possible_paths.extend([
            os.path.join(mineru_output_dir, stem, "auto", f"{stem}_content_list.json"),
            os.path.join(mineru_output_dir, stem, f"{stem}_content_list.json"),
        ])
    possible_paths.extend([
        os.path.join(mineru_output_dir, "content_list.json"),
        os.path.join(mineru_output_dir, "auto", "content_list.json"),
    ])

    for path in possible_paths:
        if os.path.exists(path):
            return path

    # 递归搜索（优先v1，v1不存在时返回v2）
    v1_path = None
    v2_path = None
    for root, dirs, files in os.walk(mineru_output_dir):
        for file in files:
            if "content_list" in file and file.endswith(".json"):
                full_path = os.path.join(root, file)
                if "v2" in file:
                    if v2_path is None:
                        v2_path = full_path
                else:
                    if v1_path is None:
                        v1_path = full_path
    return v1_path if v1_path else v2_path


def chunk_by_content_list(content_blocks: list[dict[str, Any]], max_chunk_size: int = 2000) -> list[str]:
    """按MinerU的content_list块进行切块（优先策略，对应H3修复）。

    切块策略：
      1. 利用MinerU已做好的语义切块（每个item是一个独立语义单元）
      2. 累积相邻块到max_chunk_size时形成一块
      3. 标题（title类型）作为新块的开始

    Args:
        content_blocks: content_list的块列表（由load_content_list返回）
        max_chunk_size: 每块最大字符数

    Returns:
        list: 文本块列表
    """
    if not content_blocks:
        return []

    chunks = []
    current_chunk = ""

    for block in content_blocks:
        block_text = block.get("text", "")
        block_type = block.get("type", "text")

        if not block_text.strip():
            continue

        # 标题作为新块的开始（如果当前块非空）
        if block_type == "title" and current_chunk:
            chunks.append(current_chunk)
            current_chunk = block_text
            continue

        # 累积到max_chunk_size
        if len(current_chunk) + len(block_text) + 2 <= max_chunk_size:
            current_chunk += ("\n\n" if current_chunk else "") + block_text
        else:
            if current_chunk:
                chunks.append(current_chunk)
            # 单块超过max_chunk_size时，用chunk_text进一步切分
            if len(block_text) > max_chunk_size:
                sub_chunks = chunk_text(block_text, max_chunk_size)
                if len(sub_chunks) > 1:
                    chunks.extend(sub_chunks[:-1])
                    current_chunk = sub_chunks[-1]
                else:
                    current_chunk = sub_chunks[0] if sub_chunks else ""
            else:
                current_chunk = block_text

    if current_chunk:
        chunks.append(current_chunk)

    return chunks


def chunk_text(text: str, max_chunk_size: int = 2000) -> list[str]:
    """将长文本按段落切块，每块不超过max_chunk_size字符。

    切块策略：
      1. 按双换行符（段落）分割
      2. 若段落超过max_chunk_size，按句号分割
      3. 累积到max_chunk_size时形成一块

    Args:
        text: 待切块的文本
        max_chunk_size: 每块最大字符数

    Returns:
        list: 文本块列表
    """
    if not text or not text.strip():
        return []

    chunks = []
    paragraphs = text.split("\n\n")

    current_chunk = ""
    for para in paragraphs:
        para = para.strip()
        if not para:
            continue

        if len(current_chunk) + len(para) + 2 <= max_chunk_size:
            current_chunk += ("\n\n" if current_chunk else "") + para
        else:
            if current_chunk:
                chunks.append(current_chunk)

            if len(para) <= max_chunk_size:
                current_chunk = para
            else:
                # 段落过长，按句号分割
                sentences = re.split(r"(。！？；)", para)
                current_chunk = ""
                for i in range(0, len(sentences) - 1, 2):
                    sentence = sentences[i] + (sentences[i + 1] if i + 1 < len(sentences) else "")
                    if len(current_chunk) + len(sentence) <= max_chunk_size:
                        current_chunk += sentence
                    else:
                        if current_chunk:
                            chunks.append(current_chunk)
                        current_chunk = sentence

    if current_chunk:
        chunks.append(current_chunk)

    return chunks


# ===== LLM知识提取 =====

def call_llm_for_extraction(
    text: str,
    file_name: str,
    category: str,
    api_config: dict | None = None,
) -> dict[str, Any]:
    """调用LLM提取知识点（对应SubTask 7.2）。

    Args:
        text: 待提取的文本块
        file_name: 来源文件名
        category: 文件分类
        api_config: API配置

    Returns:
        dict: LLM返回的知识点提取结果
    """
    if api_config is None:
        api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
        api_url = os.environ.get("WENYAN_LLM_API_URL", "")
        model = os.environ.get("WENYAN_LLM_MODEL", "deepseek-chat")
    else:
        api_key = api_config.get("api_key", "")
        api_url = api_config.get("api_url", "")
        model = api_config.get("model", "deepseek-chat")

    if not api_key or not api_url:
        # API不可用，降级返回空结果
        return {"knowledge_points": [], "degraded": True}

    prompt = LLM_EXTRACT_PROMPT.format(
        text=text,
        file_name=file_name,
        category=category,
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

            # 429 Too Many Requests：等待Retry-After后重试
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

            # 移除markdown代码块标记
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


# ===== 实体识别与别名归一化 =====

def normalize_entity_name(name: str) -> str:
    """别名归一化（对应SubTask 7.3）。

    将别名（如"苏东坡"、"子瞻"）归一化为标准名（"苏轼"）。

    Args:
        name: 实体名称（可能是别名）

    Returns:
        str: 归一化后的标准名称
    """
    return ALIAS_NORMALIZATION.get(name, name)


def normalize_entities_in_knowledge_point(kp: dict[str, Any]) -> dict[str, Any]:
    """对知识点中的所有实体进行别名归一化。

    Args:
        kp: 知识点字典

    Returns:
        dict: 归一化后的知识点字典
    """
    entities = kp.get("entities", [])
    for entity in entities:
        if "name" in entity:
            entity["normalized"] = normalize_entity_name(entity["name"])

    relations = kp.get("relations", [])
    for relation in relations:
        if "from" in relation:
            relation["from"] = normalize_entity_name(relation["from"])
        if "to" in relation:
            relation["to"] = normalize_entity_name(relation["to"])

    return kp


# ===== JSON Schema校验（M1修复，2026-07-10）=====

def validate_knowledge_point_schema(kp: dict[str, Any]) -> tuple[bool, str]:
    """校验LLM返回的知识点是否符合JSON Schema。

    校验规则：
      1. 必需字段：title（非空str）、core_conclusion（非空str）、subject（四科之一）
      2. 类型校验：confidence（0-1 float）、difficulty（1-5 int）、tags（list）
      3. 实体校验：每个entity有name(str)和type(在ENTITY_TYPES中)
      4. 关系校验：每个relation有from/relation/to，relation在RELATION_TYPES中

    Args:
        kp: 知识点字典

    Returns:
        tuple: (是否合格, 不合格原因)，合格时原因为空字符串
    """
    # 必需字段校验
    title = kp.get("title", "")
    if not isinstance(title, str) or not title.strip():
        return False, "title缺失或为空"

    core_conclusion = kp.get("core_conclusion", "")
    if not isinstance(core_conclusion, str) or not core_conclusion.strip():
        return False, "core_conclusion缺失或为空"

    subject = kp.get("subject", "")
    if subject not in SUBJECTS:
        return False, f"subject不在四科中: {subject}"

    # 类型校验（可选字段）
    confidence = kp.get("confidence", 1.0)
    if not isinstance(confidence, (int, float)) or not (0.0 <= confidence <= 1.0):
        # 尝试强制转换
        try:
            confidence = float(confidence)
            if not (0.0 <= confidence <= 1.0):
                return False, f"confidence超出0-1范围: {confidence}"
            kp["confidence"] = confidence
        except (ValueError, TypeError):
            return False, f"confidence非数值: {confidence}"

    difficulty = kp.get("difficulty", 3)
    if not isinstance(difficulty, int):
        try:
            difficulty = int(difficulty)
            if not (1 <= difficulty <= 5):
                return False, f"difficulty超出1-5范围: {difficulty}"
            kp["difficulty"] = difficulty
        except (ValueError, TypeError):
            return False, f"difficulty非整数: {difficulty}"

    tags = kp.get("tags", [])
    if not isinstance(tags, list):
        kp["tags"] = []

    # 实体校验
    entities = kp.get("entities", [])
    if not isinstance(entities, list):
        kp["entities"] = []
        entities = []
    valid_entities = []
    for entity in entities:
        if not isinstance(entity, dict):
            continue
        name = entity.get("name", "")
        if not name:
            continue
        entity_type = entity.get("type", "")
        if entity_type not in ENTITY_TYPES:
            entity["type"] = "CONCEPT"  # 降级为通用类型
        valid_entities.append(entity)
    kp["entities"] = valid_entities

    # 关系校验
    relations = kp.get("relations", [])
    if not isinstance(relations, list):
        kp["relations"] = []
        relations = []
    valid_relations = []
    for relation in relations:
        if not isinstance(relation, dict):
            continue
        if not relation.get("from") or not relation.get("to"):
            continue
        rel_type = relation.get("relation", "")
        if rel_type not in RELATION_TYPES:
            relation["relation"] = "RELATED_CONCEPT"  # 降级为通用关系
        valid_relations.append(relation)
    kp["relations"] = valid_relations

    return True, ""


# ===== 粒度控制 =====

def validate_knowledge_point_granularity(kp: dict[str, Any]) -> bool:
    """验证知识点粒度是否符合要求（对应SubTask 7.5）。

    要求：core_conclusion字段50-150字。

    Args:
        kp: 知识点字典

    Returns:
        bool: True表示粒度合格
    """
    core_conclusion = kp.get("core_conclusion", "")
    char_count = len(core_conclusion)

    return KNOWLEDGE_POINT_MIN_CHARS <= char_count <= KNOWLEDGE_POINT_MAX_CHARS


# ===== 置信度分级 =====

def route_by_confidence(kp: dict[str, Any]) -> str:
    """按置信度分级知识点（对应SubTask 7.6）。

    分级策略：
      - confidence >= 0.9 → "direct"（直接入库）
      - 0.6 <= confidence < 0.9 → "review"（待校）
      - confidence < 0.6 → "discard"（丢弃）

    Args:
        kp: 知识点字典

    Returns:
        str: 分级结果（"direct"/"review"/"discard"）
    """
    confidence = kp.get("confidence", 0.0)
    if confidence >= CONFIDENCE_DIRECT:
        return "direct"
    elif confidence >= CONFIDENCE_REVIEW:
        return "review"
    else:
        return "discard"


# ===== 知识点去重 =====

def deduplicate_knowledge_points(kps: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """知识点去重（相同title的知识点保留置信度最高的）。

    Args:
        kps: 知识点列表

    Returns:
        list: 去重后的知识点列表
    """
    seen = {}
    for kp in kps:
        title = kp.get("title", "").strip()
        if not title:
            continue

        if title in seen:
            # 保留置信度更高的
            if kp.get("confidence", 0) > seen[title].get("confidence", 0):
                seen[title] = kp
        else:
            seen[title] = kp

    return list(seen.values())


# ===== 抽样校验 =====

def generate_sample_for_review(
    kps: list[dict[str, Any]],
    sample_rate: float = SAMPLE_RATE,
) -> list[dict[str, Any]]:
    """生成10%抽样校验清单（对应SubTask 7.7）。

    Args:
        kps: 知识点列表
        sample_rate: 抽样比例

    Returns:
        list: 抽样知识点列表（含校验状态字段）
    """
    import random
    random.seed(42)  # 固定种子确保可复现

    sample_size = max(1, int(len(kps) * sample_rate))
    sampled = random.sample(kps, min(sample_size, len(kps)))

    for kp in sampled:
        kp["review_status"] = "pending"
        kp["review_result"] = None

    return sampled


# ===== 主处理流程 =====

def process_file(
    file_id: str,
    file_data: dict[str, Any],
    output_dir: str,
    api_config: dict | None = None,
) -> dict[str, Any]:
    """处理单个文件的知识提取。

    Args:
        file_id: 文件ID
        file_data: 校对后的文件数据（post_correct输出）或pipeline_runner原始输出
        output_dir: 输出目录
        api_config: LLM API配置

    Returns:
        dict: 提取结果摘要
    """
    # 获取文本内容
    file_name = file_data.get("file_name", file_id)
    category = file_data.get("category", "未分类")

    # 合并所有文本（高置信度+纠错后文本）
    texts = []

    # 高置信度文本（post_correct输出在顶层）
    high_text = file_data.get("high_confidence_text", "")
    if not high_text:
        # 兼容data字段位置
        high_text = file_data.get("data", {}).get("high_confidence_text", "")
    if high_text:
        texts.append(high_text)

    # 纠错后文本（post_correct输出在顶层）
    corrected_details = file_data.get("corrected_details", [])
    if not corrected_details:
        # 兼容data字段位置
        corrected_details = file_data.get("data", {}).get("corrected_details", [])
    for detail in corrected_details:
        corrected_text = detail.get("corrected", "")
        if corrected_text:
            texts.append(corrected_text)

    # 原始pages文本（NATIVE/DOCX等无校对结果的文件，来自pipeline_runner输出）
    if not texts:
        data_inner = file_data.get("data", {})
        pages = data_inner.get("pages", [])
        for page in pages:
            page_text = page.get("text", "")
            if page_text:
                texts.append(page_text)

        paragraphs = data_inner.get("paragraphs", [])
        for para in paragraphs:
            if para:
                texts.append(para)

        # 图片OCR结果
        image_text = data_inner.get("text", "")
        if image_text:
            texts.append(image_text)

        # 表格数据
        sheets = data_inner.get("sheets", [])
        for sheet in sheets:
            rows = sheet.get("rows", [])
            for row in rows:
                row_text = " ".join(str(cell) for cell in row if cell)
                if row_text.strip():
                    texts.append(row_text)

        tables = data_inner.get("tables", [])
        for table in tables:
            rows = table.get("rows", [])
            for row in rows:
                if isinstance(row, list):
                    row_text = " ".join(str(cell) for cell in row if cell)
                    if row_text.strip():
                        texts.append(row_text)

    full_text = "\n\n".join(texts)

    if not full_text.strip():
        return {
            "file_id": file_id,
            "status": "skipped",
            "reason": "无文本内容",
            "knowledge_points_count": 0,
        }

    # 文本切块（H3修复，2026-07-10）
    # 优先策略：从MinerU的content_list.json按语义块切块（质量更高）
    # 降级策略：用chunk_text按段落切块（NATIVE/DOCX等无MinerU输出的文件）
    chunks = []
    chunk_source = "paragraph"  # 记录切块来源

    mineru_output_dir = file_data.get("data", {}).get("mineru_output_dir", "")
    if not mineru_output_dir:
        mineru_output_dir = file_data.get("mineru_output_dir", "")

    if mineru_output_dir and os.path.isdir(mineru_output_dir):
        content_list_path = find_content_list(mineru_output_dir, file_name)
        if content_list_path:
            try:
                content_blocks = load_content_list(content_list_path)
                chunks = chunk_by_content_list(content_blocks)
                if chunks:
                    chunk_source = "content_list"
            except (json.JSONDecodeError, OSError) as e:
                print(f"警告：加载content_list失败 {file_id}: {e}，降级用chunk_text", file=sys.stderr)
                chunks = []

    # RapidOCR输出：利用pages[].lines[].text重建段落结构
    if not chunks:
        rapid_pages = file_data.get("data", {}).get("pages", [])
        if rapid_pages and any(p.get("lines") for p in rapid_pages):
            for page in rapid_pages:
                lines = page.get("lines", [])
                if lines:
                    page_text = "\n".join(l.get("text", "") for l in lines)
                    if page_text.strip():
                        chunks.append(page_text)
            if chunks:
                chunk_source = "rapidocr_pages"

    # 降级：用chunk_text按段落切块
    if not chunks:
        chunks = chunk_text(full_text)

    if not chunks:
        return {
            "file_id": file_id,
            "status": "skipped",
            "reason": "文本切块为空",
            "knowledge_points_count": 0,
        }

    # 逐块提取知识点
    all_knowledge_points = []
    all_entities = []
    all_relations = []

    schema_fail_count = 0  # Schema校验失败计数（M1修复）

    for i, chunk in enumerate(chunks):
        result = call_llm_for_extraction(chunk, file_name, category, api_config)

        if result.get("degraded", False):
            continue

        kps = result.get("knowledge_points", [])

        for kp in kps:
            # JSON Schema校验（M1修复，2026-07-10）
            schema_valid, schema_reason = validate_knowledge_point_schema(kp)
            if not schema_valid:
                schema_fail_count += 1
                print(f"警告：知识点Schema校验失败 {file_id} chunk{i}: {schema_reason}", file=sys.stderr)
                continue

            # 别名归一化
            kp = normalize_entities_in_knowledge_point(kp)

            # 添加来源信息
            kp["source_file"] = file_name
            kp["source_category"] = category
            kp["source_chunk_idx"] = i

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
        "category": category,
        "status": "completed",
        "total_chunks": len(chunks),
        "chunk_source": chunk_source,
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


def run_extraction_pipeline(
    input_dir: str,
    output_dir: str,
    api_config: dict | None = None,
) -> None:
    """运行知识提取管线（对应Task 7全部SubTask）。

    Args:
        input_dir: post_correct输出目录（含file_xxx_corrected.json）
        output_dir: 知识提取结果输出目录
        api_config: LLM API配置
    """
    os.makedirs(output_dir, exist_ok=True)

    # 查找待处理文件（校对后的JSON + 原始JSON）
    corrected_files = [
        f for f in os.listdir(input_dir)
        if f.endswith("_corrected.json")
    ]

    # 也处理没有corrected文件的原始pipeline输出
    all_files = [
        f for f in os.listdir(input_dir)
        if f.startswith("file_") and f.endswith(".json")
        and "corrected" not in f and "knowledge" not in f and "manual_review" not in f
    ]

    # 合并：优先用corrected文件，没有则用原始文件
    process_files = []
    for f in all_files:
        file_id = f.replace(".json", "")
        corrected_name = f"{file_id}_corrected.json"
        if corrected_name in corrected_files:
            process_files.append(corrected_name)
        else:
            process_files.append(f)

    process_files = sorted(set(process_files))

    print(f"发现 {len(process_files)} 个待提取文件")
    print()

    total_kps = 0
    total_direct = 0
    total_review = 0
    total_discard = 0

    for i, json_file in enumerate(process_files):
        file_path = os.path.join(input_dir, json_file)
        file_id = json_file.replace("_corrected.json", "").replace(".json", "")

        try:
            with open(file_path, "r", encoding="utf-8") as f:
                file_data = json.load(f)
        except (json.JSONDecodeError, OSError) as e:
            print(f"警告：跳过损坏的JSON文件 {json_file}: {e}", file=sys.stderr)
            continue

        print(f"[{i + 1}/{len(process_files)}] 提取: {file_data.get('file_name', json_file)}")

        try:
            result = process_file(file_id, file_data, output_dir, api_config)

            if result["status"] == "completed":
                total_kps += result["total_knowledge_points"]
                total_direct += result["direct_count"]
                total_review += result["review_count"]
                total_discard += result["discard_count"]
                print(f"  完成: {result['total_knowledge_points']}个知识点 "
                      f"(直接入库{result['direct_count']}/待校{result['review_count']}/丢弃{result['discard_count']})")
            else:
                print(f"  跳过: {result['reason']}")

        except Exception as e:
            print(f"  失败: {e}", file=sys.stderr)

    # 打印汇总
    print()
    print("=" * 50)
    print("知识提取汇总:")
    print(f"  总知识点: {total_kps}")
    print(f"  直接入库: {total_direct}")
    print(f"  待校: {total_review}")
    print(f"  丢弃: {total_discard}")
    print("=" * 50)


# ===== 命令行入口 =====

def main():
    """命令行入口函数。"""
    parser = argparse.ArgumentParser(
        description="文研App知识提取与结构化。"
                    "从OCR/校对后文本提取知识点、实体、关系。",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python extract_knowledge.py --input output/corrected/ --output output/knowledge/
  python extract_knowledge.py --input output/ --output output/knowledge/

环境变量（LLM API配置）:
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址
  WENYAN_LLM_MODEL    - 模型名（默认deepseek-chat）

置信度分级:
  confidence >= 0.9 → 直接入库
  0.6 <= confidence < 0.9 → 待校
  confidence < 0.6 → 丢弃
        """,
    )
    parser.add_argument(
        "--input",
        default=None,
        help="post_correct输出目录或pipeline_runner输出目录",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="知识提取结果输出目录",
    )

    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    input_dir = args.input or os.path.join(script_dir, "output", "corrected")
    output_dir = args.output or os.path.join(script_dir, "output", "knowledge")

    if not os.path.isdir(input_dir):
        print(f"错误：输入目录不存在: {input_dir}", file=sys.stderr)
        sys.exit(1)

    run_extraction_pipeline(input_dir, output_dir)


if __name__ == "__main__":
    main()
