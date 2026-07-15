"""generate_seed_v2.py - 文研App种子数据生成（v5修复版）

v5修复（对比generate_seed.py的6个BUG）：
  1. subjects使用全称（"中国古代文学"等，对齐SeedDataLoader.kt L116）
  2. exam_questions从exam_questions.json读取（由generate_exam_questions.py生成）
  3. exam_questions字段名修正：subject（非subject_id），无answer_status（SeedDataLoader L209从answerFramework推导）
  4. writing_materials字段名修正：category/sub_category/content/source/tags(String)
  5. 卡片模板名修正：CLOZE_QUOTE（非CLOZE_FILL）、WORK_AUTHOR_BIDIRECTIONAL（非WORK_AUTHOR_BIDIR）
  6. 题号拆分由generate_exam_questions.py处理（大题号/小题号层级识别）

使用方法：
  python generate_seed_v2.py --input output/cross_validated/ --exam output/exam_questions.json --output android/app/src/main/assets/

环境变量：
  无需LLM API（纯数据汇总）
"""

import argparse
import json
import os
import re
import sys
from datetime import datetime
from typing import Any


# ===== 常量定义 =====

# 科目全称（对齐SeedDataLoader.kt L116的subjectNameToId映射）
SUBJECTS_FULL = [
    {"id": "subj_01", "name": "中国古代文学", "code": "ancient"},
    {"id": "subj_02", "name": "中国现当代文学", "code": "modern"},
    {"id": "subj_03", "name": "外国文学", "code": "foreign"},
    {"id": "subj_04", "name": "文学理论", "code": "theory"},
]

# 科目简称→全称映射（用于将旧数据中的简称转换为全称）
SUBJECT_NAME_MAP = {
    "古代文学": "中国古代文学",
    "现当代文学": "中国现当代文学",
    "外国文学": "外国文学",
    "文学理论": "文学理论",
}

# 真题年份范围
EXAM_YEAR_MIN = 1998
EXAM_YEAR_MAX = 2025

# 每科最少知识点数
MIN_KPS_PER_SUBJECT = 50

# 6种卡片模板（对齐CardTemplateType.kt枚举名）
CARD_TEMPLATES = [
    "TERM_EXPLANATION",
    "CLOZE_QUOTE",                # v5修复：原CLOZE_FILL
    "WORK_AUTHOR_BIDIRECTIONAL",  # v5修复：原WORK_AUTHOR_BIDIR
    "ESSAY_POINTS",
    "SCHOOL_COMPARISON",
    "DISTINCTION",
]

# 科目代码历史数据（从generate_seed.py复用）
EXAM_CODE_HISTORY = [
    {"exam_code": "610", "subject_name": "文学基础", "valid_from_year": 1998, "valid_to_year": 2025, "direction": "专一", "note": "2025年及以前610=文学基础"},
    {"exam_code": "610", "subject_name": "专业写作", "valid_from_year": 2026, "valid_to_year": None, "direction": "专一", "note": "2026年起610=专业写作（语义翻转）"},
    {"exam_code": "801", "subject_name": "文学基础", "valid_from_year": 2026, "valid_to_year": None, "direction": "专二", "note": "2026年起801=文学基础"},
    {"exam_code": "801", "subject_name": "现当代文学", "valid_from_year": 1998, "valid_to_year": 2025, "direction": "专二", "note": "2025年及以前801=现当代文学"},
    {"exam_code": "805", "subject_name": "古代文学", "valid_from_year": 1998, "valid_to_year": 2025, "direction": "专二", "note": "古代文学方向专二"},
    {"exam_code": "806", "subject_name": "外国文学", "valid_from_year": 1998, "valid_to_year": 2025, "direction": "专二", "note": "外国文学方向专二"},
    {"exam_code": "807", "subject_name": "文学理论", "valid_from_year": 1998, "valid_to_year": 2025, "direction": "专二", "note": "文艺学方向专二"},
    {"exam_code": "F008", "subject_name": "比较文学", "valid_from_year": 1998, "valid_to_year": 2025, "direction": "专二", "note": "比较文学方向专二"},
]

# D级参考资料外链清单
REFERENCE_CATALOG = [
    {"id": "ref_001", "title": "鸿知考研网-南京师范大学文学考研真题", "url": "https://www.hongzedu.com", "type": "EXAM_PAPERS", "description": "1998-2025年南师大文学考研真题电子版", "level": "D"},
    {"id": "ref_002", "title": "南师大文学院官网-教师信息", "url": "https://wxy.njnu.edu.cn/szdw/jsfc.htm", "type": "TEACHER_INFO", "description": "南京师范大学文学院教师风采页面", "level": "D"},
    {"id": "ref_003", "title": "中国研究生招生信息网", "url": "https://yz.chsi.com.cn", "type": "OFFICIAL_INFO", "description": "官方招生目录、复试分数线", "level": "D"},
    {"id": "ref_004", "title": "维基文库-公共领域文学原典", "url": "https://zh.wikisource.org", "type": "ORIGINAL_TEXT", "description": "《文心雕龙》等公共领域原典", "level": "D"},
    {"id": "ref_005", "title": "EOL-文学理论名词解释", "url": "https://www.eol.cn", "type": "KNOWLEDGE_BASE", "description": "文学理论名词解释资料", "level": "D"},
]


# ===== 数据加载 =====

def normalize_subject(subject: str) -> str:
    """将科目名归一化为全称。

    extract_knowledge_v2.py已使用全称，但旧数据可能用简称。
    """
    if subject in SUBJECT_NAME_MAP.values():
        return subject  # 已是全称
    return SUBJECT_NAME_MAP.get(subject, subject)


def load_cross_validated_knowledge(input_dir: str) -> list[dict[str, Any]]:
    """加载交叉校验后的知识点。

    v5修复：将subject字段归一化为全称。
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

    kps = data.get("knowledge_points", [])
    # v5修复：归一化subject为全称
    for kp in kps:
        kp["subject"] = normalize_subject(kp.get("subject", ""))

    return kps


def load_exam_questions(exam_file: str) -> list[dict[str, Any]]:
    """从exam_questions.json加载真题（由generate_exam_questions.py生成）。

    v5修复：
      - 从独立的exam_questions.json读取，而非从原始文件重新拆分
      - 字段名对齐SeedDataLoader.kt的ExamQuestionSeed
      - 不输出answer_status（SeedDataLoader L209从answerFramework推导）
    """
    if not os.path.exists(exam_file):
        print(f"警告：真题文件不存在: {exam_file}", file=sys.stderr)
        return []

    try:
        with open(exam_file, "r", encoding="utf-8") as f:
            data = json.load(f)
    except (json.JSONDecodeError, OSError) as e:
        print(f"警告：真题文件JSON损坏: {e}", file=sys.stderr)
        return []

    raw_questions = data.get("exam_questions", [])
    processed = []

    for q in raw_questions:
        # 归一化subject为全称
        subject = normalize_subject(q.get("subject", ""))

        # v5修复：只输出ExamQuestionSeed需要的字段
        # SeedDataLoader.kt ExamQuestionSeed字段：
        #   id/year/subject/question_type/content/score/exam_paper_code/answer_framework/sample_essay
        processed.append({
            "id": q.get("id", ""),
            "year": q.get("year", 0),
            "subject": subject,
            "question_type": q.get("question_type", "UNKNOWN"),
            "content": q.get("content", ""),
            "score": q.get("score", 0) or 0,
            "exam_paper_code": q.get("exam_paper_code", ""),
            # answerFramework为空字符串时设为null（SeedDataLoader L209据此判断answer_status）
            "answer_framework": q.get("answer_framework") or None,
            "sample_essay": q.get("sample_essay") or None,
        })

    return processed


def load_error_dict(input_dir: str) -> dict[str, Any]:
    """加载OCR错误词典。"""
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
            except (json.JSONDecodeError, OSError):
                continue
    return {"errors": [], "total_corrections": 0, "last_updated": None}


# ===== 卡片生成 =====

def generate_cards_from_knowledge_points(kps: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """从知识点生成卡片（遵循最小信息原则）。

    v5修复：卡片模板名对齐CardTemplateType.kt枚举。
    注意：SeedDataLoader.kt L290 cards字段为List<JsonElement>不被解析，
    卡片由CardRepository动态生成，这里仅生成供参考/调试用。
    """
    cards = []
    card_id_counter = 1

    for kp in kps:
        title = kp.get("title", "")
        core_conclusion = kp.get("core_conclusion", "")
        if not title or not core_conclusion:
            continue

        aspects = extract_card_aspects(kp)
        for aspect in aspects:
            card = {
                "id": f"card_{card_id_counter:05d}",
                "knowledge_point_id": kp.get("id", ""),
                "title": f"{title}-{aspect['question']}",
                "front": aspect["question"],
                "back": aspect["answer"],
                "card_template": aspect["template"],
                "subject": normalize_subject(kp.get("subject", "")),
                "tags": kp.get("tags", []),
                "memory_tier": aspect.get("memory_tier", "TIER_FRAMEWORK"),
                "created_at": datetime.now().isoformat(),
            }
            cards.append(card)
            card_id_counter += 1

    return cards


def extract_card_aspects(kp: dict[str, Any]) -> list[dict[str, str]]:
    """从知识点提取多个卡片切面。

    v5修复：卡片模板名对齐CardTemplateType.kt枚举：
      - CLOZE_FILL → CLOZE_QUOTE
      - WORK_AUTHOR_BIDIR → WORK_AUTHOR_BIDIRECTIONAL
    """
    title = kp.get("title", "")
    core_conclusion = kp.get("core_conclusion", "")
    study_text = kp.get("study_text", "")
    aspects = []

    # 切面1：定义/概述
    aspects.append({
        "question": f"{title}是什么？",
        "answer": core_conclusion,
        "template": "TERM_EXPLANATION",
        "memory_tier": "TIER_FRAMEWORK",
    })

    # 切面2：详细内容
    if study_text and study_text != core_conclusion:
        aspects.append({
            "question": f"{title}的详细内容？",
            "answer": study_text,
            "template": "TERM_EXPLANATION",
            "memory_tier": "TIER_UNDERSTAND",
        })

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

    # 切面4：相关作品（v5修复：WORK_AUTHOR_BIDIRECTIONAL）
    if works:
        work_names = "、".join(e.get("normalized", e.get("name", "")) for e in works[:3])
        aspects.append({
            "question": f"{title}的代表作品？",
            "answer": work_names,
            "template": "WORK_AUTHOR_BIDIRECTIONAL",
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

    # 切面6：多视角对比
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

    # 切面7：Cloze名句填空（v5修复：CLOZE_QUOTE）
    full_content = kp.get("full_content", "")
    if full_content:
        quotes = re.findall(r'[""「」『』《》]([^""「」『』《》]{4,30})[""「」『』《》]', full_content)
        if quotes:
            quote = quotes[0]
            if len(quote) >= 8:
                third = len(quote) // 3
                blank_part = quote[third:third * 2]
                cloze_text = quote[:third] + "____" + quote[third * 2:]
                aspects.append({
                    "question": f"填空：{cloze_text}",
                    "answer": blank_part,
                    "template": "CLOZE_QUOTE",
                    "memory_tier": "TIER_EXACT",
                })

    # 切面8：区分卡
    if len(authors) >= 2:
        author_list = [(e.get("normalized", e.get("name", "")), e.get("name", "")) for e in authors[:3]]
        author_pairs = []
        for i, (norm1, _) in enumerate(author_list):
            for j, (norm2, _) in enumerate(author_list):
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
        school_list = [e.get("normalized", e.get("name", "")) for e in schools[:3]]
        s1, s2 = school_list[0], school_list[1]
        aspects.append({
            "question": f"区分：{s1} vs {s2}",
            "answer": f"{s1}与{s2}是不同的文学流派，需注意区分各自特征。",
            "template": "DISTINCTION",
            "memory_tier": "TIER_FRAMEWORK",
        })

    return aspects[:6]


# ===== 写作素材生成 =====

def generate_writing_materials(kps: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """从知识点生成写作素材。

    v5修复：字段名对齐SeedDataLoader.kt的WritingMaterialSeed：
      - id/category/sub_category/content/source/tags(String)
      原generate_seed.py错误使用title/subject/tags(List)等字段名。
    """
    materials = []
    mat_id = 1

    for kp in kps:
        title = kp.get("title", "")
        core_conclusion = kp.get("core_conclusion", "")
        if not title or not core_conclusion:
            continue

        subject = normalize_subject(kp.get("subject", ""))
        perspectives = kp.get("multi_perspectives", [])
        source = perspectives[0].get("source", "") if perspectives else ""

        # v5修复：字段名对齐WritingMaterialSeed
        materials.append({
            "id": f"wm_{mat_id:04d}",
            "category": subject,              # 科目全称作为category
            "sub_category": title,            # 知识点标题作为sub_category
            "content": core_conclusion,
            "source": source,
            "tags": ",".join(kp.get("tags", [])),  # String类型（非List）
            "created_at": datetime.now().isoformat(),
        })
        mat_id += 1

    return materials


# ===== 图谱数据生成 =====

def generate_graph_data(kps: list[dict[str, Any]]) -> dict[str, list]:
    """从知识点提取图谱节点和边。"""
    nodes = []
    edges = []
    node_id_map = {}
    node_counter = 1
    edge_counter = 1

    for kp in kps:
        title = kp.get("title", "")
        if not title:
            continue

        if title not in node_id_map:
            node_id = f"node_{node_counter:05d}"
            node_id_map[title] = node_id
            nodes.append({
                "id": node_id,
                "label": title,
                "type": "KNOWLEDGE_POINT",
                "subject": normalize_subject(kp.get("subject", "")),
            })
            node_counter += 1

        for entity in kp.get("entities", []):
            name = entity.get("normalized", entity.get("name", ""))
            entity_type = entity.get("type", "CONCEPT")
            if name and name not in node_id_map:
                node_id = f"node_{node_counter:05d}"
                node_id_map[name] = node_id
                nodes.append({
                    "id": node_id,
                    "label": name,
                    "type": entity_type,
                    "subject": normalize_subject(kp.get("subject", "")),
                })
                node_counter += 1

        for relation in kp.get("relations", []):
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
    """验证种子数据完整性。

    v5修复：使用全称验证科目覆盖。
    """
    results = []
    kps = seed_data.get("knowledge_points", [])

    # 验证1：知识点覆盖四科（使用全称）
    subject_counts = {}
    for kp in kps:
        subject = kp.get("subject", "未分类")
        subject_counts[subject] = subject_counts.get(subject, 0) + 1

    for subj in SUBJECTS_FULL:
        count = subject_counts.get(subj["name"], 0)
        passed = count >= MIN_KPS_PER_SUBJECT
        results.append({
            "check_id": "C1.14",
            "name": f"知识点覆盖-{subj['name']}",
            "passed": passed,
            "detail": f"{count}个知识点（要求≥{MIN_KPS_PER_SUBJECT}）",
        })

    # 验证2：真题覆盖年份范围
    exam_questions = seed_data.get("exam_questions", [])
    years = set(eq.get("year") for eq in exam_questions if eq.get("year"))
    year_coverage = len(years)
    year_threshold = int((EXAM_YEAR_MAX - EXAM_YEAR_MIN + 1) * 0.7)
    results.append({
        "check_id": "C1.15",
        "name": "真题覆盖年份",
        "passed": year_coverage >= year_threshold,
        "detail": f"覆盖{year_coverage}个年份" + (f"（{min(years)}-{max(years)}）" if years else ""),
    })

    # 验证3：真题科目匹配（v5新增）
    subject_names = {s["name"] for s in SUBJECTS_FULL}
    unmatched_subjects = set()
    for eq in exam_questions:
        subj = eq.get("subject", "")
        if subj and subj not in subject_names and subj != "COMPREHENSIVE":
            unmatched_subjects.add(subj)
    results.append({
        "check_id": "C1.16",
        "name": "真题科目匹配",
        "passed": len(unmatched_subjects) == 0,
        "detail": f"未匹配科目: {unmatched_subjects}" if unmatched_subjects else "全部匹配",
    })

    # 验证4：卡片模板齐全
    cards = seed_data.get("cards", [])
    used_templates = set(card.get("card_template", "") for card in cards)
    missing_templates = set(CARD_TEMPLATES) - used_templates
    results.append({
        "check_id": "C3.14-C3.19",
        "name": "卡片模板齐全",
        "passed": len(missing_templates) == 0,
        "detail": f"已使用{len(used_templates)}/6种模板" + (f"，缺失: {missing_templates}" if missing_templates else ""),
    })

    # 验证5：种子数据结构完整
    required_keys = ["knowledge_points", "exam_questions", "cards", "writing_materials"]
    missing_keys = [key for key in required_keys if key not in seed_data]
    results.append({
        "check_id": "C1.32",
        "name": "种子数据结构完整",
        "passed": len(missing_keys) == 0,
        "detail": f"结构{'完整' if not missing_keys else '缺失: ' + str(missing_keys)}",
    })

    all_passed = all(r["passed"] for r in results)
    return {
        "all_passed": all_passed,
        "results": results,
        "total_kps": len(kps),
        "total_exam_questions": len(exam_questions),
        "total_cards": len(cards),
        "total_writing_materials": len(seed_data.get("writing_materials", [])),
    }


# ===== 主生成流程 =====

def generate_seed_data(
    knowledge_dir: str,
    exam_file: str,
    output_dir: str,
) -> None:
    """生成全部种子数据文件（v5修复版）。

    Args:
        knowledge_dir: cross_validate输出目录
        exam_file: exam_questions.json路径（由generate_exam_questions.py生成）
        output_dir: 种子数据输出目录
    """
    os.makedirs(output_dir, exist_ok=True)

    print("=" * 60)
    print("文研App种子数据生成（v5修复版）")
    print("=" * 60)
    print()

    # 1. 加载交叉校验后的知识点
    print("[1/5] 加载知识点...")
    knowledge_points = load_cross_validated_knowledge(knowledge_dir)
    print(f"  知识点数: {len(knowledge_points)}")

    # 2. 加载真题（从exam_questions.json）
    print("[2/5] 加载真题...")
    exam_questions = load_exam_questions(exam_file)
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
            "version": "2.0.0",
            "generated_at": datetime.now().isoformat(),
            "description": "文研App种子数据（南师大文学考研）- v5修复版",
            "source": "Phase 1 资料数字化管线输出",
            "fixes": [
                "subjects使用全称",
                "exam_questions从exam_questions.json读取",
                "writing_materials字段名对齐WritingMaterialSeed",
                "卡片模板名对齐CardTemplateType.kt枚举",
            ],
        },
        "subjects": SUBJECTS_FULL,
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
            "metadata": {"version": "2.0.0", "generated_at": datetime.now().isoformat(), "description": "D级参考资料外链清单"},
            "references": REFERENCE_CATALOG,
        }, f, ensure_ascii=False, indent=2)
    print(f"  参考资料目录保存至: {ref_catalog_path}")

    # 写入exam_code_history.json
    exam_code_path = os.path.join(output_dir, "exam_code_history.json")
    with open(exam_code_path, "w", encoding="utf-8") as f:
        json.dump({
            "metadata": {"version": "2.0.0", "generated_at": datetime.now().isoformat(), "description": "科目代码变动历史"},
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
    print("=" * 60)
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
    print()
    if validation["all_passed"]:
        print("所有验证项通过！")
    else:
        failed = [r for r in validation["results"] if not r["passed"]]
        print(f"警告：{len(failed)}项验证未通过（可能是数据量不足，需运行完整管线后重新生成）")
    print("=" * 60)


# ===== 命令行入口 =====

def main():
    parser = argparse.ArgumentParser(description="文研App种子数据生成（v5修复版）")
    parser.add_argument("--input", default=None, help="cross_validate输出目录")
    parser.add_argument("--exam", default=None, help="exam_questions.json路径")
    parser.add_argument("--output", default=None, help="种子数据输出目录")
    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)

    input_dir = args.input or os.path.join(script_dir, "output", "cross_validated")
    exam_file = args.exam or os.path.join(script_dir, "output", "exam_questions.json")
    output_dir = args.output or os.path.join(project_root, "wenyan-android", "app", "src", "main", "assets")

    if not os.path.isdir(input_dir):
        print(f"警告：输入目录不存在: {input_dir}", file=sys.stderr)
    if not os.path.exists(exam_file):
        print(f"警告：真题文件不存在: {exam_file}", file=sys.stderr)

    generate_seed_data(input_dir, exam_file, output_dir)


if __name__ == "__main__":
    main()
