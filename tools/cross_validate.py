"""cross_validate.py - 文研App多教材交叉校验与对照

将来自不同教材的同一知识点进行交叉校验，合并为多视角对照结构。

核心功能：
  1. 古代文学：袁行霈为主干（study_text），马工程（袁世硕）为答题基准（core_conclusion），
     游国恩为补充视角（multi_perspectives）（SubTask 8.1）
  2. 现当代文学：丁帆为主干，钱理群《三十年》为补充（SubTask 8.2）
  3. 外国文学：聂珍钊为主干，郑克鲁为补充（SubTask 8.3）
  4. 文学理论：童庆炳为主干，周宪标注"非官方拓展"（SubTask 8.4）
  5. 冲突标注：版本间实质性矛盾标记conflict_flag（SubTask 8.5）

对应 Task 8.1-8.5（spec.md MODIFIED Requirements 知识点多教材对照 Scenario）。

双轨制原则（spec.md第504行）：
  - core_conclusion（核心结论/答题基准）：以马工程版（袁世硕）为准，因考试官方指定
  - study_text（学习理解文本）：以袁行霈版为主，因用户指定其更易读
  - multi_perspectives（多视角）：展示游国恩版等不同表述

使用方法：
  python cross_validate.py --input output/knowledge/ --output output/cross_validated/
"""

import argparse
import json
import os
import re
import sys
from datetime import datetime
from typing import Any


# ===== 常量定义 =====

# 教材主干映射（对应SubTask 8.1-8.4）
# 每个科目的主干教材（study_text来源）、答题基准教材（core_conclusion来源）、补充教材
TEXTBOOK_MAPPING = {
    "古代文学": {
        "study_text_main": "袁行霈",          # 学习理解主干（易读）
        "core_conclusion_main": "马工程",      # 答题基准主干（官方指定，袁世硕）
        "supplementary": ["游国恩"],           # 补充视角
        "main_file_keywords": ["袁行霈", "中国文学史"],
        "conclusion_file_keywords": ["马工程", "中国古代文学史"],
        "supplementary_file_keywords": ["游国恩"],
    },
    "现当代文学": {
        "study_text_main": "丁帆",            # 学习理解主干
        "core_conclusion_main": "丁帆",        # 答题基准主干（同主干）
        "supplementary": ["钱理群", "三十年"],
        "main_file_keywords": ["丁帆", "中国新文学史"],
        "conclusion_file_keywords": ["丁帆", "中国新文学史"],
        "supplementary_file_keywords": ["钱理群", "三十年"],
    },
    "外国文学": {
        "study_text_main": "聂珍钊",          # 学习理解主干
        "core_conclusion_main": "聂珍钊",      # 答题基准主干（马工程版本）
        "supplementary": ["郑克鲁"],
        "main_file_keywords": ["聂珍钊", "外国文学史"],
        "conclusion_file_keywords": ["聂珍钊", "外国文学史"],
        "supplementary_file_keywords": ["郑克鲁"],
    },
    "文学理论": {
        "study_text_main": "童庆炳",          # 学习理解主干
        "core_conclusion_main": "童庆炳",      # 答题基准主干
        "supplementary": ["周宪"],
        "main_file_keywords": ["童庆炳", "文学理论教程"],
        "conclusion_file_keywords": ["童庆炳", "文学理论教程"],
        "supplementary_file_keywords": ["周宪", "文学理论导引"],
    },
}

# 周宪标注"非官方拓展"（对应SubTask 8.4）
NON_OFFICIAL_LABEL = "非官方拓展"


# ===== 教材来源识别 =====

def identify_textbook_source(file_name: str, category: str) -> str:
    """根据文件名和分类识别教材来源。

    Args:
        file_name: 文件名
        category: 文件分类（科目）

    Returns:
        str: 教材来源标识（"袁行霈"/"马工程"/"游国恩"/"丁帆"/"聂珍钊"/"郑克鲁"/"童庆炳"/"周宪"/"其他"）
    """
    if category not in TEXTBOOK_MAPPING:
        return "其他"

    mapping = TEXTBOOK_MAPPING[category]

    # 检查主干教材
    for keyword in mapping["main_file_keywords"]:
        if keyword in file_name:
            # 区分袁行霈和马工程（古代文学特殊处理）
            if category == "古代文学":
                if "马工程" in file_name or "中国古代文学史" in file_name:
                    return "马工程"
                if "袁行霈" in file_name or "袁行霈版本" in file_name:
                    return "袁行霈"
                if "游国恩" in file_name:
                    return "游国恩"
            return mapping["study_text_main"]

    # 检查补充教材
    for keyword in mapping["supplementary_file_keywords"]:
        if keyword in file_name:
            return mapping["supplementary"][0] if mapping["supplementary"] else "其他"

    return "其他"


def is_main_textbook(source: str, category: str) -> bool:
    """判断是否为主干教材。

    Args:
        source: 教材来源标识
        category: 科目

    Returns:
        bool: True表示为主干教材
    """
    if category not in TEXTBOOK_MAPPING:
        return False

    mapping = TEXTBOOK_MAPPING[category]
    return source == mapping["study_text_main"]


def is_conclusion_textbook(source: str, category: str) -> bool:
    """判断是否为答题基准教材。

    Args:
        source: 教材来源标识
        category: 科目

    Returns:
        bool: True表示为答题基准教材
    """
    if category not in TEXTBOOK_MAPPING:
        return False

    mapping = TEXTBOOK_MAPPING[category]
    return source == mapping["core_conclusion_main"]


# ===== 知识点聚合 =====

def load_all_knowledge_points(input_dir: str) -> list[dict[str, Any]]:
    """加载所有知识点提取结果。

    Args:
        input_dir: extract_knowledge输出目录

    Returns:
        list: 所有知识点列表（含来源信息）
    """
    all_kps = []

    json_files = [
        f for f in os.listdir(input_dir)
        if f.endswith("_knowledge.json")
    ]

    for json_file in sorted(json_files):
        file_path = os.path.join(input_dir, json_file)
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
        except (json.JSONDecodeError, OSError) as e:
            print(f"警告：跳过损坏的JSON文件 {json_file}: {e}", file=sys.stderr)
            continue

        file_name = data.get("file_name", "")
        category = data.get("category", "未分类")

        # 识别教材来源
        textbook_source = identify_textbook_source(file_name, category)

        for kp in data.get("valid_knowledge_points", []):
            kp["textbook_source"] = textbook_source
            kp["original_file"] = file_name
            all_kps.append(kp)

    return all_kps


def group_knowledge_points_by_title(kps: list[dict[str, Any]]) -> dict[str, list[dict]]:
    """按标题分组知识点（相似标题视为同一知识点）。

    使用标题归一化+difflib.SequenceMatcher相似度>0.8匹配，
    解决"建安风骨"与"建安文学"等近义标题被分到不同组的问题。

    Args:
        kps: 知识点列表

    Returns:
        dict: 按标题分组的知识点字典
    """
    import difflib
    import re

    def normalize_title(title: str) -> str:
        """归一化标题：去除空格/标点/章节编号。"""
        t = re.sub(r'[\s\u3000]+', '', title)
        t = re.sub(r'^[第章节课]\d+[、.．:：]?', '', t)
        t = re.sub(r'[，。！？、：；""''（）()\[\]【】]', '', t)
        return t.strip()

    groups = {}
    normalized_keys = []  # [(normalized_title, original_title)]

    for kp in kps:
        title = kp.get("title", "").strip()
        if not title:
            continue

        norm = normalize_title(title)

        # 精确匹配归一化后的标题
        matched_key = None
        for nk, ok in normalized_keys:
            if nk == norm:
                matched_key = ok
                break

        # 模糊匹配：相似度>0.8
        if matched_key is None:
            best_ratio = 0.0
            best_key = None
            for nk, ok in normalized_keys:
                ratio = difflib.SequenceMatcher(None, norm, nk).ratio()
                if ratio > best_ratio:
                    best_ratio = ratio
                    best_key = ok
            if best_ratio >= 0.8:
                matched_key = best_key

        if matched_key is None:
            # 新建分组
            groups[title] = []
            normalized_keys.append((norm, title))
            groups[title].append(kp)
        else:
            groups[matched_key].append(kp)

    return groups


# ===== 冲突检测 =====

def detect_conflict(kps: list[dict[str, Any]]) -> bool:
    """检测同一知识点的不同教材版本间是否存在实质性矛盾。

    冲突检测策略：
      1. 比较core_conclusion字段的语义相似度（简化版：2-4字n-gram重叠率）
      2. 若重叠率<30%（代码阈值，docstring原写50%已于2026-07-10修正为30%）
         且各版本均有明确表述，标记为冲突

    注意：n-gram重叠率是语义相似度的粗略近似，效果有限（自承简化），
    可能漏标或误标冲突。生产环境建议改用BGE-small-zh语义相似度。

    Args:
        kps: 同一标题的知识点列表（来自不同教材）

    Returns:
        bool: True表示存在冲突
    """
    if len(kps) < 2:
        return False

    # 提取各版本的核心结论
    conclusions = [kp.get("core_conclusion", "") for kp in kps]
    conclusions = [c for c in conclusions if c.strip()]

    if len(conclusions) < 2:
        return False

    # 计算关键词重叠率（简化版，不依赖外部库）
    def extract_keywords(text: str) -> set[str]:
        """提取文本关键词（简化版：2-4字的中文词组）。"""
        # 移除标点和空白
        cleaned = re.sub(r"[^\u4e00-\u9fff]", "", text)
        # 按字滑窗提取2-4字词组
        keywords = set()
        for size in [2, 3, 4]:
            for i in range(len(cleaned) - size + 1):
                keywords.add(cleaned[i:i + size])
        return keywords

    keyword_sets = [extract_keywords(c) for c in conclusions]

    # 计算两两重叠率
    for i in range(len(keyword_sets)):
        for j in range(i + 1, len(keyword_sets)):
            if not keyword_sets[i] or not keyword_sets[j]:
                continue
            overlap = len(keyword_sets[i] & keyword_sets[j])
            union = len(keyword_sets[i] | keyword_sets[j])
            if union > 0:
                overlap_rate = overlap / union
                if overlap_rate < 0.3:
                    return True

    return False


# ===== 合并多视角 =====

def merge_multi_perspective_knowledge_point(
    title: str,
    kps: list[dict[str, Any]],
    kp_id: str = "",
) -> dict[str, Any]:
    """将来自不同教材的同一知识点合并为多视角对照结构。

    合并策略（对应spec.md第507-511行）：
      1. core_conclusion 以答题基准教材（马工程/丁帆/聂珍钊/童庆炳）为准
      2. study_text 以学习理解主干教材（袁行霈/丁帆/聂珍钊/童庆炳）为主
      3. multi_perspectives 收集所有教材版本的不同表述
      4. 冲突标注 conflict_flag

    Args:
        title: 知识点标题
        kps: 同一标题的知识点列表（来自不同教材）
        kp_id: 知识点ID（由调用方分配）

    Returns:
        dict: 合并后的多视角知识点
    """
    if not kps:
        return {}

    # 获取科目
    subject = kps[0].get("subject", "")

    # 按教材来源分组
    by_source = {}
    for kp in kps:
        source = kp.get("textbook_source", "其他")
        if source not in by_source:
            by_source[source] = []
        by_source[source].append(kp)

    # 确定答题基准教材版本
    conclusion_kp = None
    if subject in TEXTBOOK_MAPPING:
        conclusion_source = TEXTBOOK_MAPPING[subject]["core_conclusion_main"]
        if conclusion_source in by_source and by_source[conclusion_source]:
            conclusion_kp = by_source[conclusion_source][0]

    # 如果没有答题基准教材版本，用第一个
    if conclusion_kp is None:
        conclusion_kp = kps[0]

    # 确定学习理解教材版本
    study_kp = None
    if subject in TEXTBOOK_MAPPING:
        study_source = TEXTBOOK_MAPPING[subject]["study_text_main"]
        if study_source in by_source and by_source[study_source]:
            study_kp = by_source[study_source][0]

    if study_kp is None:
        study_kp = conclusion_kp

    # 构建multi_perspectives
    perspectives = []
    for source, source_kps in by_source.items():
        for kp in source_kps:
            perspective = {
                "source": source,
                "core_conclusion": kp.get("core_conclusion", ""),
                "full_content": kp.get("full_content", ""),
                "source_file": kp.get("original_file", ""),
                "is_main": is_main_textbook(source, subject),
                "is_conclusion_base": is_conclusion_textbook(source, subject),
            }

            # 周宪标注"非官方拓展"
            if source == "周宪":
                perspective["label"] = NON_OFFICIAL_LABEL

            perspectives.append(perspective)

    # 冲突检测
    has_conflict = detect_conflict(kps)

    # 合并实体和关系
    all_entities = []
    all_relations = []
    seen_entity_names = set()
    seen_relation_keys = set()

    for kp in kps:
        for entity in kp.get("entities", []):
            name = entity.get("normalized", entity.get("name", ""))
            if name and name not in seen_entity_names:
                all_entities.append(entity)
                seen_entity_names.add(name)

        for relation in kp.get("relations", []):
            rel_key = (relation.get("from", ""), relation.get("relation", ""), relation.get("to", ""))
            if rel_key not in seen_relation_keys:
                all_relations.append(relation)
                seen_relation_keys.add(rel_key)

    # 合并tags
    all_tags = set()
    for kp in kps:
        all_tags.update(kp.get("tags", []))

    # 构建合并后的知识点
    merged = {
        "id": kp_id,
        "title": title,
        "summary": conclusion_kp.get("summary", ""),
        "core_conclusion": conclusion_kp.get("core_conclusion", ""),
        "study_text": study_kp.get("full_content", study_kp.get("core_conclusion", "")),
        "full_content": conclusion_kp.get("full_content", ""),
        "subject": subject,
        "tags": list(all_tags),
        "difficulty": max(kp.get("difficulty", 3) for kp in kps),
        "multi_perspectives": perspectives,
        "conflict_flag": has_conflict,
        "entities": all_entities,
        "relations": all_relations,
        "source_count": len(by_source),
        "textbook_sources": list(by_source.keys()),
        "merged_at": datetime.now().isoformat(),
    }

    return merged


# ===== 主处理流程 =====

def run_cross_validation(
    input_dir: str,
    output_dir: str,
) -> None:
    """运行多教材交叉校验管线（对应Task 8全部SubTask）。

    Args:
        input_dir: extract_knowledge输出目录
        output_dir: 交叉校验结果输出目录
    """
    os.makedirs(output_dir, exist_ok=True)

    # 1. 加载所有知识点
    print("加载知识点...")
    all_kps = load_all_knowledge_points(input_dir)
    print(f"总知识点数: {len(all_kps)}")

    if not all_kps:
        print("警告：未找到知识点，写入空结果", file=sys.stderr)
        result_path = os.path.join(output_dir, "cross_validated_knowledge.json")
        with open(result_path, "w", encoding="utf-8") as f:
            json.dump({"knowledge_points": [], "conflicts": []}, f, ensure_ascii=False, indent=2)
        return

    # 2. 按标题分组
    print("按标题分组...")
    groups = group_knowledge_points_by_title(all_kps)
    print(f"唯一知识点数: {len(groups)}")

    # 3. 多视角合并
    print("多视角合并...")
    merged_kps = []
    conflict_count = 0
    multi_source_count = 0
    kp_id_counter = 1

    for title, kps in groups.items():
        kp_id = f"kp_{kp_id_counter:05d}"
        kp_id_counter += 1
        merged = merge_multi_perspective_knowledge_point(title, kps, kp_id)
        if merged:
            merged_kps.append(merged)
            if merged.get("conflict_flag", False):
                conflict_count += 1
            if merged.get("source_count", 0) > 1:
                multi_source_count += 1

    # 4. 按科目统计
    subject_stats = {}
    for kp in merged_kps:
        subject = kp.get("subject", "未分类")
        subject_stats[subject] = subject_stats.get(subject, 0) + 1

    # 5. 写入结果
    result = {
        "total_unique_knowledge_points": len(merged_kps),
        "multi_source_knowledge_points": multi_source_count,
        "conflict_knowledge_points": conflict_count,
        "subject_stats": subject_stats,
        "knowledge_points": merged_kps,
        "processed_at": datetime.now().isoformat(),
    }

    result_path = os.path.join(output_dir, "cross_validated_knowledge.json")
    with open(result_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    # 打印汇总
    print()
    print("=" * 50)
    print("多教材交叉校验汇总:")
    print(f"  唯一知识点: {len(merged_kps)}")
    print(f"  多来源知识点: {multi_source_count}")
    print(f"  冲突知识点: {conflict_count}")
    print("  科目分布:")
    for subject, count in sorted(subject_stats.items()):
        print(f"    {subject}: {count}")
    print(f"  结果保存至: {result_path}")
    print("=" * 50)


# ===== 命令行入口 =====

def main():
    """命令行入口函数。"""
    parser = argparse.ArgumentParser(
        description="文研App多教材交叉校验与对照。"
                    "将不同教材的同一知识点合并为多视角对照结构。",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python cross_validate.py --input output/knowledge/ --output output/cross_validated/

双轨制原则:
  古代文学: study_text=袁行霈(易读), core_conclusion=马工程(官方指定)
  现当代文学: 主干=丁帆, 补充=钱理群《三十年》
  外国文学: 主干=聂珍钊, 补充=郑克鲁
  文学理论: 主干=童庆炳, 补充=周宪(标注"非官方拓展")
        """,
    )
    parser.add_argument(
        "--input",
        default=None,
        help="extract_knowledge输出目录",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="交叉校验结果输出目录",
    )

    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    input_dir = args.input or os.path.join(script_dir, "output", "knowledge")
    output_dir = args.output or os.path.join(script_dir, "output", "cross_validated")

    if not os.path.isdir(input_dir):
        print(f"错误：输入目录不存在: {input_dir}", file=sys.stderr)
        print(f"请先运行: python {os.path.join(script_dir, 'extract_knowledge.py')}",
              file=sys.stderr)
        sys.exit(1)

    run_cross_validation(input_dir, output_dir)


if __name__ == "__main__":
    main()
