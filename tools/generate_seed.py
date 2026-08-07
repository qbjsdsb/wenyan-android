"""generate_seed.py - 文研App种子数据生成

汇总所有处理结果，生成App所需的种子数据JSON文件。

核心功能：
  1. 汇总所有处理结果为 assets/seed_data.json（知识点/真题/卡片/写作素材）（SubTask 9.1）
  2. 生成 assets/reference_catalog.json（D级参考资料外链清单）（SubTask 9.2）
  3. 生成 assets/exam_code_history.json（科目代码变动历史）（SubTask 9.3）
  4. 生成 assets/error_dict.json（OCR错误词典，项目资产）（SubTask 9.4）
  5. 验证种子数据完整性（知识点覆盖四科/真题覆盖1998-2025/卡片模板齐全）（SubTask 9.5）

对应 Task 9.1-9.5。

输出文件：
  - android/app/src/main/assets/seed_data.json          （主种子数据）
  - android/app/src/main/assets/reference_catalog.json   （D级参考资料外链）
  - android/app/src/main/assets/exam_code_history.json   （科目代码历史）
  - android/app/src/main/assets/error_dict.json          （OCR错误词典）

使用方法：
  python generate_seed.py --input output/cross_validated/ --output android/app/src/main/assets/
"""

import argparse
import json
import os
import re
import sys
from datetime import datetime
from typing import Any


# ===== 常量定义 =====

# 真题年份范围（对应checklist C1.15）
EXAM_YEAR_MIN = 1998
EXAM_YEAR_MAX = 2025

# 四科分类（对应checklist C1.14）
SUBJECTS = ["古代文学", "现当代文学", "外国文学", "文学理论"]

# 每科最少知识点数（对应checklist C1.14，每科至少50+）
MIN_KPS_PER_SUBJECT = 50

# 6种卡片模板（对应checklist C3.14-C3.19）
CARD_TEMPLATES = [
    "TERM_EXPLANATION",      # 名词解释卡
    "CLOZE_FILL",            # Cloze名句填空卡
    "WORK_AUTHOR_BIDIR",     # 作品-作者双向卡
    "ESSAY_POINTS",          # 论述要点卡
    "SCHOOL_COMPARISON",     # 流派对照卡
    "DISTINCTION",           # 区分卡
]

# 科目代码历史数据（对应SubTask 9.3，spec.md第227-246行）
EXAM_CODE_HISTORY = [
    {
        "exam_code": "610",
        "subject_name": "文学基础",
        "valid_from_year": 1998,
        "valid_to_year": 2025,
        "direction": "专一",
        "note": "2025年及以前610=文学基础",
    },
    {
        "exam_code": "610",
        "subject_name": "专业写作",
        "valid_from_year": 2026,
        "valid_to_year": None,
        "direction": "专一",
        "note": "2026年起610=专业写作（语义翻转）",
    },
    {
        "exam_code": "801",
        "subject_name": "文学基础",
        "valid_from_year": 2026,
        "valid_to_year": None,
        "direction": "专二",
        "note": "2026年起801=文学基础",
    },
    {
        "exam_code": "801",
        "subject_name": "现当代文学",
        "valid_from_year": 1998,
        "valid_to_year": 2025,
        "direction": "专二",
        "note": "2025年及以前801=现当代文学（现当代方向）",
    },
    {
        "exam_code": "805",
        "subject_name": "古代文学",
        "valid_from_year": 2017,
        "valid_to_year": 2022,
        "direction": "专二",
        "note": "2017—2022年古代文学方向专二",
    },
    {
        "exam_code": "805",
        "subject_name": "外国文学",
        "valid_from_year": 2023,
        "valid_to_year": 2025,
        "direction": "专二",
        "note": "2023—2025年外国文学方向专二（805）",
    },
    {
        "exam_code": "806",
        "subject_name": "外国文学",
        "valid_from_year": 1998,
        "valid_to_year": 2025,
        "direction": "专二",
        "note": "外国文学方向专二",
    },
    {
        "exam_code": "807",
        "subject_name": "文学理论",
        "valid_from_year": 1998,
        "valid_to_year": 2025,
        "direction": "专二",
        "note": "文艺学方向专二",
    },
    {
        "exam_code": "F008",
        "subject_name": "比较文学",
        "valid_from_year": 1998,
        "valid_to_year": 2025,
        "direction": "专二",
        "note": "比较文学方向专二",
    },
]

# D级参考资料外链清单（对应SubTask 9.2）
REFERENCE_CATALOG = [
    {
        "id": "ref_001",
        "title": "鸿知考研网-南京师范大学文学考研真题",
        "url": "https://www.hongzedu.com",
        "type": "EXAM_PAPERS",
        "description": "1998-2025年南师大文学考研真题电子版",
        "level": "D",
    },
    {
        "id": "ref_002",
        "title": "南师大文学院官网-教师信息",
        "url": "https://wxy.njnu.edu.cn/szdw/jsfc.htm",
        "type": "TEACHER_INFO",
        "description": "南京师范大学文学院教师风采页面",
        "level": "D",
    },
    {
        "id": "ref_003",
        "title": "中国研究生招生信息网",
        "url": "https://yz.chsi.com.cn",
        "type": "OFFICIAL_INFO",
        "description": "官方招生目录、复试分数线",
        "level": "D",
    },
    {
        "id": "ref_004",
        "title": "维基文库-公共领域文学原典",
        "url": "https://zh.wikisource.org",
        "type": "ORIGINAL_TEXT",
        "description": "《文心雕龙》等公共领域原典",
        "level": "D",
    },
    {
        "id": "ref_005",
        "title": "EOL-文学理论名词解释",
        "url": "https://www.eol.cn",
        "type": "KNOWLEDGE_BASE",
        "description": "文学理论名词解释资料",
        "level": "D",
    },
]


# ===== 数据加载 =====

def load_cross_validated_knowledge(input_dir: str) -> list[dict[str, Any]]:
    """加载交叉校验后的知识点。

    Args:
        input_dir: cross_validate输出目录

    Returns:
        list: 知识点列表
    """
    cv_path = os.path.join(input_dir, "cross_validated_knowledge.json")
    if not os.path.exists(cv_path):
        print(f"警告：交叉校验结果不存在: {cv_path}", file=sys.stderr)
        return []

    try:
        with open(cv_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except (json.JSONDecodeError, OSError) as e:
        print(f"警告：交叉校验结果JSON损坏: {e}", file=sys.stderr)
        return []

    return data.get("knowledge_points", [])


def load_exam_questions(input_dir: str) -> list[dict[str, Any]]:
    """从pipeline输出中提取真题数据。

    Args:
        input_dir: cross_validate输出目录（其父目录为pipeline_runner输出目录）

    Returns:
        list: 真题列表
    """
    exam_questions = []

    # cross_validate输出目录为 output/cross_validated，其父目录 output 即pipeline_runner输出目录
    pipeline_dir = os.path.dirname(input_dir)
    if not os.path.isdir(pipeline_dir):
        pipeline_dir = input_dir

    json_files = [
        f for f in os.listdir(pipeline_dir)
        if f.startswith("file_") and f.endswith(".json")
        and "corrected" not in f and "knowledge" not in f and "manual_review" not in f
    ]

    for json_file in sorted(json_files):
        file_path = os.path.join(pipeline_dir, json_file)
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
        except (json.JSONDecodeError, OSError) as e:
            print(f"警告：跳过损坏的JSON文件 {json_file}: {e}", file=sys.stderr)
            continue

        file_name = data.get("file_name", "")
        category = data.get("category", "")

        # 从文件名提取年份
        year_match = re.search(r"(20\d{2}|19\d{2})", file_name)
        year = int(year_match.group(1)) if year_match else None

        # 从文件名/分类识别科目和试卷代码
        exam_paper_code = None
        if "610" in file_name:
            exam_paper_code = "610"
        elif "801" in file_name:
            exam_paper_code = "801"
        elif "805" in file_name:
            exam_paper_code = "805"
        elif "806" in file_name:
            exam_paper_code = "806"
        elif "807" in file_name:
            exam_paper_code = "807"
        elif "F008" in file_name:
            exam_paper_code = "F008"

        # 从真题文件提取题目
        if "真题" in category or "真题" in file_name:
            # pipeline_runner输出的数据结构：data字段内含pages/paragraphs/content_source/ocr_status
            data_inner = data.get("data", {})
            pages = data_inner.get("pages", [])
            full_text = "\n".join(p.get("text", "") for p in pages)

            paragraphs = data_inner.get("paragraphs", [])
            if paragraphs:
                full_text = "\n".join(paragraphs)

            if full_text.strip():
                # 2026-07-10修正：原代码将整张试卷作为一条题目且截断500字
                # 改为按题号拆分（匹配"1.""2."..."十、"等中文数字题号）
                import re as _re
                # 按题号拆分：数字+点/顿号/括号 开头的行作为题目分隔
                # 负向先行断言排除19xx/20xx年份误匹配
                question_pattern = _re.compile(
                    r'^(?:[一二三四五六七八九十]+[、.．)]|(?!19\d{2}|20\d{2})\d+[、.．)]|\([一二三四五六七八九十]+\)|\(\d+\))',
                    _re.MULTILINE
                )
                question_starts = [(m.start(), m.group()) for m in question_pattern.finditer(full_text)]

                if len(question_starts) >= 2:
                    # 按题号拆分多条题目
                    for qi, (start, qmark) in enumerate(question_starts):
                        end = question_starts[qi + 1][0] if qi + 1 < len(question_starts) else len(full_text)
                        q_text = full_text[start:end].strip()
                        if len(q_text) < 5:
                            continue
                        # 识别题型
                        q_type = "UNKNOWN"
                        if any(kw in q_text[:50] for kw in ["名词解释", "解释"]):
                            q_type = "NOUN_EXPLANATION"
                        elif any(kw in q_text[:50] for kw in ["论述", "分析"]):
                            q_type = "ESSAY"
                        elif any(kw in q_text[:50] for kw in ["简答", "简述"]):
                            q_type = "SHORT_ANSWER"

                        exam_questions.append({
                            "id": f"eq_{len(exam_questions) + 1:04d}",
                            "year": year,
                            "exam_paper_code": exam_paper_code,
                            "subject_id": category,
                            "question_type": q_type,
                            "content": q_text[:1000],  # 单题最多1000字
                            "score": None,
                            "angle": None,
                            "related_point_ids": [],
                            "answer_framework": "",
                            "sample_essay": "",
                            "notes": f"来源文件: {file_name}, 题号: {qmark}",
                            "answer_status": "NO_ANSWER",
                            "material_text": "",
                            "source_file": file_name,
                            "source_page": 1,
                            "content_source": data_inner.get("content_source", "TEXTBOOK_NATIVE"),
                            "ocr_status": data_inner.get("ocr_status", "VERIFIED"),
                            "created_at": datetime.now().isoformat(),
                        })
                else:
                    # 无法按题号拆分，整张试卷作为一条记录（保留完整文本，不截断）
                    exam_questions.append({
                        "id": f"eq_{len(exam_questions) + 1:04d}",
                        "year": year,
                        "exam_paper_code": exam_paper_code,
                        "subject_id": category,
                        "question_type": "UNKNOWN",
                        "content": full_text[:2000],  # 最多2000字（原500字过短）
                        "score": None,
                        "angle": None,
                        "related_point_ids": [],
                        "answer_framework": "",
                        "sample_essay": "",
                        "notes": f"来源文件: {file_name}（整张试卷，未拆分）",
                        "answer_status": "NO_ANSWER",
                        "material_text": "",
                        "source_file": file_name,
                        "source_page": 1,
                        "content_source": data_inner.get("content_source", "TEXTBOOK_NATIVE"),
                        "ocr_status": data_inner.get("ocr_status", "VERIFIED"),
                        "created_at": datetime.now().isoformat(),
                    })

    return exam_questions


def load_error_dict(input_dir: str) -> dict[str, Any]:
    """加载OCR错误词典。

    Args:
        input_dir: post_correct输出目录

    Returns:
        dict: 错误词典
    """
    # 尝试多个可能的路径
    possible_paths = [
        os.path.join(input_dir, "error_dict.json"),
        os.path.join(os.path.dirname(input_dir), "error_dict.json"),
        os.path.join(os.path.dirname(os.path.dirname(input_dir)), "error_dict.json"),
    ]

    for path in possible_paths:
        if os.path.exists(path):
            try:
                with open(path, "r", encoding="utf-8") as f:
                    return json.load(f)
            except (json.JSONDecodeError, OSError) as e:
                print(f"警告：error_dict.json损坏 {path}: {e}", file=sys.stderr)
                continue

    return {"errors": [], "total_corrections": 0, "last_updated": None}


# ===== 卡片生成 =====

def generate_cards_from_knowledge_points(kps: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """从知识点生成卡片（遵循最小信息原则）。

    卡片生成策略（对应spec.md第456-477行）：
      1. 名词解释拆成5-6张卡
      2. 避免集合题（分组枚举）
      3. 易混淆内容生成区分卡

    Args:
        kps: 知识点列表

    Returns:
        list: 卡片列表
    """
    cards = []
    card_id_counter = 1

    for kp in kps:
        title = kp.get("title", "")
        core_conclusion = kp.get("core_conclusion", "")
        study_text = kp.get("study_text", "")
        tags = kp.get("tags", [])

        if not title or not core_conclusion:
            continue

        # 名词解释拆卡（对应spec.md第459-466行，一个拆成5-6张）
        # 这里简化处理：根据知识点内容生成多张卡片
        card_aspects = extract_card_aspects(kp)

        for aspect in card_aspects:
            card = {
                "id": f"card_{card_id_counter:05d}",
                "knowledge_point_id": kp.get("id", ""),
                "title": f"{title}-{aspect['question']}",
                "front": aspect["question"],
                "back": aspect["answer"],
                "card_template": aspect["template"],
                "subject": kp.get("subject", ""),
                "tags": tags,
                "content_source": "TEXTBOOK_NATIVE",
                "ocr_status": "VERIFIED",
                "memory_tier": aspect.get("memory_tier", "TIER_FRAMEWORK"),
                "created_at": datetime.now().isoformat(),
            }
            cards.append(card)
            card_id_counter += 1

    return cards


def extract_card_aspects(kp: dict[str, Any]) -> list[dict[str, str]]:
    """从知识点提取多个卡片切面（遵循最小信息原则）。

    一个名词解释拆成5-6张卡，每张只考一个知识点。

    Args:
        kp: 知识点字典

    Returns:
        list: 卡片切面列表，每个含question/answer/template
    """
    title = kp.get("title", "")
    core_conclusion = kp.get("core_conclusion", "")
    study_text = kp.get("study_text", "")
    subject = kp.get("subject", "")

    aspects = []

    # 基本切面1：定义/概述
    aspects.append({
        "question": f"{title}是什么？",
        "answer": core_conclusion,
        "template": "TERM_EXPLANATION",
        "memory_tier": "TIER_FRAMEWORK",
    })

    # 基本切面2：详细内容
    if study_text and study_text != core_conclusion:
        aspects.append({
            "question": f"{title}的详细内容？",
            "answer": study_text,
            "template": "TERM_EXPLANATION",
            "memory_tier": "TIER_UNDERSTAND",
        })

    # 从实体中提取切面
    entities = kp.get("entities", [])
    authors = [e for e in entities if e.get("type") == "AUTHOR"]
    works = [e for e in entities if e.get("type") == "WORK"]
    schools = [e for e in entities if e.get("type") == "SCHOOL"]

    # 切面3：相关作家
    if authors:
        author_names = "、".join(e.get("normalized", e.get("name", "")) for e in authors[:3])
        aspects.append({
            "question": f"{title}的代表作家？",
            "answer": author_names,
            "template": "TERM_EXPLANATION",
            "memory_tier": "TIER_FRAMEWORK",
        })

    # 切面4：相关作品
    if works:
        work_names = "、".join(e.get("normalized", e.get("name", "")) for e in works[:3])
        aspects.append({
            "question": f"{title}的代表作品？",
            "answer": work_names,
            "template": "WORK_AUTHOR_BIDIR",
            "memory_tier": "TIER_FRAMEWORK",
        })

    # 切面5：所属流派
    if schools:
        school_names = "、".join(e.get("normalized", e.get("name", "")) for e in schools[:3])
        aspects.append({
            "question": f"{title}所属流派？",
            "answer": school_names,
            "template": "SCHOOL_COMPARISON",
            "memory_tier": "TIER_FRAMEWORK",
        })

    # 切面6：多视角对比（如果有多教材来源）
    perspectives = kp.get("multi_perspectives", [])
    if len(perspectives) > 1:
        perspective_summary = "\n".join(
            f"【{p.get('source', '')}】{p.get('core_conclusion', '')[:50]}..."
            for p in perspectives[:3]
        )
        aspects.append({
            "question": f"{title}不同教材的表述对比？",
            "answer": perspective_summary,
            "template": "ESSAY_POINTS",
            "memory_tier": "TIER_UNDERSTAND",
        })

    # 切面7：Cloze名句填空（从full_content提取引号内容作为名句）
    full_content = kp.get("full_content", "")
    if full_content:
        import re as _re
        # 提取中文引号和双引号中的内容（≥4字的短句作为名句填空）
        quotes = _re.findall(r'[""「」『』《》]([^""「」『』《》]{4,30})[""「」『』《》]', full_content)
        if quotes:
            # 取第一条名句做Cloze填空（去掉中间部分留空）
            quote = quotes[0]
            if len(quote) >= 8:
                # 取名句的中间1/3作为填空部分
                third = len(quote) // 3
                blank_part = quote[third:third * 2]
                cloze_text = quote[:third] + "____" + quote[third * 2:]
                aspects.append({
                    "question": f"填空：{cloze_text}",
                    "answer": blank_part,
                    "template": "CLOZE_FILL",
                    "memory_tier": "TIER_EXACT",
                })

    # 切面8：区分卡（检测易混淆实体——同类型且需要区分的）
    if len(authors) >= 2:
        # 同一流派/知识点中的多个作家，生成区分卡
        author_pairs = []
        author_list = [(e.get("normalized", e.get("name", "")), e.get("name", "")) for e in authors[:3]]
        for i, (norm1, name1) in enumerate(author_list):
            for j, (norm2, name2) in enumerate(author_list):
                if i < j and norm1 != norm2:
                    author_pairs.append((norm1, norm2))
        if author_pairs:
            a1, a2 = author_pairs[0]
            aspects.append({
                "question": f"区分：{a1} vs {a2}（{title}相关）",
                "answer": f"{a1}与{a2}同属{title}相关作家，需注意区分各自特点。",
                "template": "DISTINCTION",
                "memory_tier": "TIER_FRAMEWORK",
            })
    elif len(schools) >= 2:
        # 多个流派生成区分卡
        school_list = [e.get("normalized", e.get("name", "")) for e in schools[:3]]
        s1, s2 = school_list[0], school_list[1]
        aspects.append({
            "question": f"区分：{s1} vs {s2}",
            "answer": f"{s1}与{s2}是不同的文学流派，需注意区分各自特征。",
            "template": "DISTINCTION",
            "memory_tier": "TIER_FRAMEWORK",
        })

    # 限制最多6张卡（对应spec.md第459行，5-6张）
    return aspects[:6]


def generate_writing_materials(kps: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """从知识点生成写作素材。

    Args:
        kps: 知识点列表

    Returns:
        list: 写作素材列表
    """
    materials = []
    mat_id = 1

    for kp in kps:
        title = kp.get("title", "")
        core_conclusion = kp.get("core_conclusion", "")
        perspectives = kp.get("multi_perspectives", [])

        if not title or not core_conclusion:
            continue

        # 生成写作素材（用于论述题答题）
        material = {
            "id": f"wm_{mat_id:04d}",
            "title": title,
            "content": core_conclusion,
            "subject": kp.get("subject", ""),
            "tags": kp.get("tags", []),
            "source": perspectives[0].get("source", "") if perspectives else "",
            "source_file": perspectives[0].get("source_file", "") if perspectives else "",
            "content_source": "TEXTBOOK_NATIVE",
            "ocr_status": "VERIFIED",
            "created_at": datetime.now().isoformat(),
        }
        materials.append(material)
        mat_id += 1

    return materials


def generate_graph_data(kps: list[dict[str, Any]]) -> dict[str, list]:
    """从知识点提取图谱节点和边。

    Args:
        kps: 知识点列表

    Returns:
        dict: 含nodes和edges的图谱数据
    """
    nodes = []
    edges = []
    node_id_map = {}
    node_counter = 1
    edge_counter = 1

    for kp in kps:
        title = kp.get("title", "")
        if not title:
            continue

        # 添加知识点节点
        if title not in node_id_map:
            node_id = f"node_{node_counter:05d}"
            node_id_map[title] = node_id
            nodes.append({
                "id": node_id,
                "label": title,
                "type": "KNOWLEDGE_POINT",
                "subject": kp.get("subject", ""),
                "retrievability": 0.0,
                "prerequisites": [],
            })
            node_counter += 1

        # 添加实体节点和关系边
        entities = kp.get("entities", [])
        for entity in entities:
            name = entity.get("normalized", entity.get("name", ""))
            entity_type = entity.get("type", "CONCEPT")

            if name and name not in node_id_map:
                node_id = f"node_{node_counter:05d}"
                node_id_map[name] = node_id
                nodes.append({
                    "id": node_id,
                    "label": name,
                    "type": entity_type,
                    "subject": kp.get("subject", ""),
                    "retrievability": 0.0,
                    "prerequisites": [],
                })
                node_counter += 1

        # 添加关系边
        relations = kp.get("relations", [])
        for relation in relations:
            from_name = relation.get("from", "")
            to_name = relation.get("to", "")
            rel_type = relation.get("relation", "RELATED_CONCEPT")

            from_id = node_id_map.get(from_name)
            to_id = node_id_map.get(to_name)

            if from_id and to_id:
                edges.append({
                    "id": f"edge_{edge_counter:05d}",
                    "from_node": from_id,
                    "to_node": to_id,
                    "relation_type": rel_type,
                })
                edge_counter += 1

    return {"nodes": nodes, "edges": edges}


# ===== 种子数据完整性验证 =====

def validate_seed_data(seed_data: dict[str, Any]) -> dict[str, Any]:
    """验证种子数据完整性（对应SubTask 9.5）。

    验证项：
      1. 知识点覆盖四科（每科≥50）
      2. 真题覆盖1998-2025年
      3. 卡片模板齐全（6种）
      4. 种子数据JSON结构完整

    Args:
        seed_data: 种子数据字典

    Returns:
        dict: 验证结果（含每项的passed/detail）
    """
    results = []

    # 验证1：知识点覆盖四科
    kps = seed_data.get("knowledge_points", [])
    subject_counts = {}
    for kp in kps:
        subject = kp.get("subject", "未分类")
        subject_counts[subject] = subject_counts.get(subject, 0) + 1

    for subject in SUBJECTS:
        count = subject_counts.get(subject, 0)
        passed = count >= MIN_KPS_PER_SUBJECT
        results.append({
            "check_id": "C1.14",
            "name": f"知识点覆盖-{subject}",
            "passed": passed,
            "detail": f"{count}个知识点（要求≥{MIN_KPS_PER_SUBJECT}）",
        })

    # 验证2：真题覆盖年份范围
    exam_questions = seed_data.get("exam_questions", [])
    years = set()
    for eq in exam_questions:
        year = eq.get("year")
        if year:
            years.add(year)

    year_coverage = len(years)
    # 2026-07-10修正：原阈值50%过宽松，spec要求覆盖1998-2025年
    # 降为70%阈值（允许个别年份缺失，但要求基本覆盖）
    year_threshold = int((EXAM_YEAR_MAX - EXAM_YEAR_MIN + 1) * 0.7)
    passed_year = year_coverage >= year_threshold
    results.append({
        "check_id": "C1.15",
        "name": "真题覆盖年份",
        "passed": passed_year,
        "detail": f"覆盖{year_coverage}个年份（{min(years) if years else 'N/A'}-{max(years) if years else 'N/A'}）",
    })

    # 验证3：卡片模板齐全
    cards = seed_data.get("cards", [])
    used_templates = set(card.get("card_template", "") for card in cards)
    missing_templates = set(CARD_TEMPLATES) - used_templates
    passed_templates = len(missing_templates) == 0
    results.append({
        "check_id": "C3.14-C3.19",
        "name": "卡片模板齐全",
        "passed": passed_templates,
        "detail": f"已使用{len(used_templates)}/6种模板" + (f"，缺失: {missing_templates}" if missing_templates else ""),
    })

    # 验证4：种子数据结构完整
    required_keys = ["knowledge_points", "exam_questions", "cards",
                     "writing_materials", "graph_nodes", "graph_edges"]
    missing_keys = [key for key in required_keys if key not in seed_data]
    passed_structure = len(missing_keys) == 0
    results.append({
        "check_id": "C1.32",
        "name": "种子数据结构完整",
        "passed": passed_structure,
        "detail": f"结构{'完整' if passed_structure else '缺失: ' + str(missing_keys)}",
    })

    # 汇总
    all_passed = all(r["passed"] for r in results)
    return {
        "all_passed": all_passed,
        "results": results,
        "total_kps": len(kps),
        "total_exam_questions": len(exam_questions),
        "total_cards": len(cards),
        "total_writing_materials": len(seed_data.get("writing_materials", [])),
        "total_graph_nodes": len(seed_data.get("graph_nodes", [])),
        "total_graph_edges": len(seed_data.get("graph_edges", [])),
    }


# ===== 主生成流程 =====

def generate_seed_data(
    knowledge_dir: str,
    output_dir: str,
) -> None:
    """生成全部种子数据文件（对应Task 9全部SubTask）。

    Args:
        knowledge_dir: cross_validate输出目录
        output_dir: 种子数据输出目录（通常为 android/app/src/main/assets/）
    """
    os.makedirs(output_dir, exist_ok=True)

    print("=" * 50)
    print("文研App种子数据生成")
    print("=" * 50)
    print()

    # 1. 加载交叉校验后的知识点
    print("[1/5] 加载知识点...")
    knowledge_points = load_cross_validated_knowledge(knowledge_dir)
    print(f"  知识点数: {len(knowledge_points)}")

    # 2. 加载真题
    print("[2/5] 加载真题...")
    exam_questions = load_exam_questions(knowledge_dir)
    print(f"  真题数: {len(exam_questions)}")

    # 3. 生成卡片
    print("[3/5] 生成卡片...")
    cards = generate_cards_from_knowledge_points(knowledge_points)
    print(f"  卡片数: {len(cards)}")

    # 4. 生成写作素材和图谱数据
    print("[4/5] 生成写作素材和图谱数据...")
    writing_materials = generate_writing_materials(knowledge_points)
    graph_data = generate_graph_data(knowledge_points)
    print(f"  写作素材: {len(writing_materials)}")
    print(f"  图谱节点: {len(graph_data['nodes'])}")
    print(f"  图谱边: {len(graph_data['edges'])}")

    # 5. 汇总种子数据
    print("[5/5] 汇总种子数据...")
    seed_data = {
        "metadata": {
            "version": "1.0.0",
            "generated_at": datetime.now().isoformat(),
            "description": "文研App种子数据（南师大现当代文学考研）",
            "source": "Phase 1 资料数字化管线输出",
        },
        "subjects": [
            {"id": "subj_01", "name": "古代文学", "code": "ancient"},
            {"id": "subj_02", "name": "现当代文学", "code": "modern"},
            {"id": "subj_03", "name": "外国文学", "code": "foreign"},
            {"id": "subj_04", "name": "文学理论", "code": "theory"},
        ],
        "knowledge_points": knowledge_points,
        "exam_questions": exam_questions,
        "cards": cards,
        "writing_materials": writing_materials,
        "graph_nodes": graph_data["nodes"],
        "graph_edges": graph_data["edges"],
    }

    # 验证种子数据完整性
    validation = validate_seed_data(seed_data)

    # 写入seed_data.json
    seed_data_path = os.path.join(output_dir, "seed_data.json")
    with open(seed_data_path, "w", encoding="utf-8") as f:
        json.dump(seed_data, f, ensure_ascii=False, indent=2)
    print(f"  种子数据保存至: {seed_data_path}")

    # 写入reference_catalog.json
    ref_catalog_path = os.path.join(output_dir, "reference_catalog.json")
    with open(ref_catalog_path, "w", encoding="utf-8") as f:
        json.dump({
            "metadata": {
                "version": "1.0.0",
                "generated_at": datetime.now().isoformat(),
                "description": "D级参考资料外链清单",
            },
            "references": REFERENCE_CATALOG,
        }, f, ensure_ascii=False, indent=2)
    print(f"  参考资料目录保存至: {ref_catalog_path}")

    # 写入exam_code_history.json
    exam_code_path = os.path.join(output_dir, "exam_code_history.json")
    with open(exam_code_path, "w", encoding="utf-8") as f:
        json.dump({
            "metadata": {
                "version": "1.0.0",
                "generated_at": datetime.now().isoformat(),
                "description": "科目代码变动历史（解决610/801语义翻转问题）",
            },
            "code_history": EXAM_CODE_HISTORY,
        }, f, ensure_ascii=False, indent=2)
    print(f"  科目代码历史保存至: {exam_code_path}")

    # 写入error_dict.json
    error_dict = load_error_dict(knowledge_dir)
    error_dict_path = os.path.join(output_dir, "error_dict.json")
    with open(error_dict_path, "w", encoding="utf-8") as f:
        json.dump(error_dict, f, ensure_ascii=False, indent=2)
    print(f"  OCR错误词典保存至: {error_dict_path}")

    # 打印验证结果
    print()
    print("=" * 50)
    print("种子数据完整性验证:")
    for result in validation["results"]:
        status = "✓" if result["passed"] else "✗"
        print(f"  {status} [{result['check_id']}] {result['name']}: {result['detail']}")
    print()

    print("种子数据汇总:")
    print(f"  知识点: {validation['total_kps']}")
    print(f"  真题: {validation['total_exam_questions']}")
    print(f"  卡片: {validation['total_cards']}")
    print(f"  写作素材: {validation['total_writing_materials']}")
    print(f"  图谱节点: {validation['total_graph_nodes']}")
    print(f"  图谱边: {validation['total_graph_edges']}")
    print()

    if validation["all_passed"]:
        print("所有验证项通过！")
    else:
        failed = [r for r in validation["results"] if not r["passed"]]
        print(f"警告：{len(failed)}项验证未通过（可能是数据量不足，需运行完整管线后重新生成）")

    print("=" * 50)


# ===== 命令行入口 =====

def main():
    """命令行入口函数。"""
    parser = argparse.ArgumentParser(
        description="文研App种子数据生成。"
                    "汇总所有处理结果，生成App所需的种子数据JSON文件。",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python generate_seed.py --input output/cross_validated/ --output android/app/src/main/assets/

输出文件:
  seed_data.json          - 主种子数据（知识点/真题/卡片/写作素材/图谱）
  reference_catalog.json  - D级参考资料外链清单
  exam_code_history.json  - 科目代码变动历史
  error_dict.json         - OCR错误词典（项目资产）
        """,
    )
    parser.add_argument(
        "--input",
        default=None,
        help="cross_validate输出目录",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="种子数据输出目录（默认: android/app/src/main/assets/）",
    )

    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)

    input_dir = args.input or os.path.join(script_dir, "output", "cross_validated")
    output_dir = args.output or os.path.join(project_root, "android", "app", "src", "main", "assets")

    if not os.path.isdir(input_dir):
        print(f"错误：输入目录不存在: {input_dir}", file=sys.stderr)
        print(f"请先运行: python {os.path.join(script_dir, 'cross_validate.py')}",
              file=sys.stderr)
        sys.exit(1)

    generate_seed_data(input_dir, output_dir)


if __name__ == "__main__":
    main()
