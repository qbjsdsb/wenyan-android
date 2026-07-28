"""derive_exam_frequency.py - 考频派生工具

从 seed_data.json 的真题内容派生知识点的 exam_frequency 字段。

背景：
    generate_seed.py 完全没有考频派生逻辑，导致 98.8% 知识点标记为 NEVER，
    考频 chip（高频/中频/低频）功能形同虚设。本脚本通过关键词匹配 +
    真题年份权重，为每个知识点派生 HIGH/MEDIUM/LOW/NEVER 考频。

派生算法：
    1. 对每道真题，提取题干关键词（去除数字、标点、问号等）
    2. 对每个知识点，用 title + tags + core_conclusion 关键词与真题题干匹配
    3. 匹配成功则该知识点累计一次"命中"
    4. 按命中次数 + 年份近度计算考频分值：
       - 命中次数权重：每次命中 +10 分
       - 年份近度权重：近 5 年（2020-2025）×1.5，近 10 年 ×1.2，更早 ×1.0
    5. 按分值分档：
       - HIGH（高频）：分值 >= 30（约 3 次命中，或 2 次近年命中）
       - MEDIUM（中频）：分值 >= 15（约 1-2 次命中）
       - LOW（低频）：分值 > 0（至少 1 次命中）
       - NEVER（从未考查）：分值 = 0

使用方法：
    python tools/derive_exam_frequency.py [--input PATH] [--dry-run]

输入：app/src/main/assets/seed_data.json
输出：原地更新 exam_frequency 字段（--dry-run 仅预览不写入）

注意：
    本脚本不重新生成 seed_data.json，仅更新 exam_frequency 字段。
    运行后需 bump seed version 触发 App 重新导入。
"""

import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path


# ===== 常量定义 =====

DEFAULT_SEED_PATH = "app/src/main/assets/seed_data.json"

# 考频分档阈值
# 提高阈值：避免 2-3 字通用词误匹配导致大量 HIGH
# HIGH：约 3 次强命中，或 2 次近年强命中 + 多次弱命中
# MEDIUM：约 1-2 次强命中
# LOW：至少 1 次弱命中
HIGH_THRESHOLD = 60
MEDIUM_THRESHOLD = 30
LOW_THRESHOLD = 10

# 年份近度权重
RECENT_YEAR_WINDOW = 5    # 近 5 年视为高频考查期
RECENT_YEAR_WEIGHT = 1.5
MID_YEAR_WINDOW = 10      # 近 10 年
MID_YEAR_WEIGHT = 1.2
OLD_YEAR_WEIGHT = 1.0

# 当前年份（用于计算年份近度）
CURRENT_YEAR = 2025

# 强信号关键词最小长度（中文）：避免"文学"、"世纪"等 2 字通用词
STRONG_KEYWORD_MIN_LENGTH = 3
# 强信号分值（每个强关键词命中）
STRONG_HIT_SCORE = 20
# 弱信号分值（2 字关键词命中，需在 title 中出现）
WEAK_HIT_SCORE = 5

# 题干清洗正则：去除题号、标点、常见连接词
QUESTION_NUMBER_PATTERN = re.compile(r"^\d+[\.\、]\s*")
PUNCTUATION_PATTERN = re.compile(r"[，。；：、？！""''（）《》、\.\,\;\:\?\!\"\'\(\)\[\]\{\}]")
# 停用词：太短或太泛化的词不作为匹配关键词
STOPWORDS = {
    "的", "了", "是", "在", "和", "与", "及", "或", "也", "都", "还", "又",
    "这", "那", "其", "之", "于", "以", "为", "被", "把", "让", "使",
    "什么", "怎么", "为什么", "如何", "试", "试论", "试述", "试析", "简述",
    "论述", "分析", "比较", "说明", "举例", "举例说明", "结合", "谈谈",
    "请", "请简述", "请论述", "请分析", "请说明", "请比较",
    "特点", "意义", "影响", "关系", "区别", "联系", "内容", "形式",
    "下列", "以下", "属于", "关于",
}


def clean_question_content(content: str) -> str:
    """清洗真题题干：去除题号、标点。"""
    if not content:
        return ""
    # 去除题号（如 "1. " "2、 "）
    cleaned = QUESTION_NUMBER_PATTERN.sub("", content.strip())
    # 去除标点（保留中文与字母数字）
    cleaned = PUNCTUATION_PATTERN.sub(" ", cleaned)
    return cleaned.strip()


def extract_keywords(text: str, min_length: int = 2) -> list[str]:
    """从文本提取关键词：分词后过滤停用词与短词。

    简单分词策略：按空格切分（题干清洗后已有空格）+ 滑动 2-3 字提取。
    对于中文文本，用 2-3 字滑动窗口提取候选关键词。
    """
    if not text:
        return []

    keywords = set()

    # 按空格切分（清洗后的题干）
    for token in text.split():
        token = token.strip()
        if not token or len(token) < min_length:
            continue
        if token in STOPWORDS:
            continue
        keywords.add(token)

    # 2-3 字滑动窗口（捕获无空格中文短语）
    no_space = text.replace(" ", "")
    for window_size in (3, 2):
        for i in range(len(no_space) - window_size + 1):
            chunk = no_space[i : i + window_size]
            if chunk in STOPWORDS:
                continue
            # 过滤纯数字或含太多标点的 chunk
            if re.match(r"^[\d\s]+$", chunk):
                continue
            keywords.add(chunk)

    return list(keywords)


def extract_kp_keywords(kp: dict) -> tuple[set[str], set[str]]:
    """从知识点提取匹配关键词：区分强信号与弱信号。

    Returns:
        (strong_keywords, weak_keywords)
        - strong: 长度 >= 3 的关键词（title 子串、tags、entities）
        - weak: 长度 == 2 的关键词（仅 title 子串，不含 core_conclusion）

    设计理由：
        v1 用 2-3 字滑动窗口扫描 core_conclusion，导致"文学"、"世纪"、
        "主义"等通用 2 字词大量误匹配，97.5% 知识点标 HIGH。
        v2 仅用 title/tags/entities 作为关键词源，区分强弱信号，
        避免 core_conclusion 噪声。
    """
    strong = set()
    weak = set()

    def add_chunk(chunk: str):
        chunk = chunk.strip()
        if not chunk or chunk in STOPWORDS:
            return
        if re.match(r"^[\d\s]+$", chunk):
            return
        if len(chunk) >= STRONG_KEYWORD_MIN_LENGTH:
            strong.add(chunk)
        elif len(chunk) == 2:
            weak.add(chunk)

    # 1. title 直接作为关键词（最强信号）
    title = kp.get("title", "")
    if title:
        strong.add(title)
        # title 的 2-3 字子串
        for window_size in (3, 2):
            for i in range(len(title) - window_size + 1):
                add_chunk(title[i : i + window_size])

    # 2. tags 直接作为关键词（强信号）
    for tag in kp.get("tags", []) or []:
        if tag and tag not in STOPWORDS:
            if len(tag) >= STRONG_KEYWORD_MIN_LENGTH:
                strong.add(tag)
            elif len(tag) == 2:
                weak.add(tag)

    # 3. entities 的 name/normalized（作品名、作者名是强信号）
    for entity in kp.get("entities", []) or []:
        name = entity.get("normalized") or entity.get("name", "")
        if name and name not in STOPWORDS:
            strong.add(name)
            # 作品/作者的 2-3 字子串
            for window_size in (3, 2):
                for i in range(len(name) - window_size + 1):
                    add_chunk(name[i : i + window_size])

    # 4. 不再扫描 core_conclusion（v1 误匹配源头）

    return strong, weak


def compute_year_weight(year: int) -> float:
    """计算真题年份近度权重。"""
    if year >= CURRENT_YEAR - RECENT_YEAR_WINDOW:
        return RECENT_YEAR_WEIGHT
    elif year >= CURRENT_YEAR - MID_YEAR_WINDOW:
        return MID_YEAR_WEIGHT
    else:
        return OLD_YEAR_WEIGHT


def derive_frequency(
    knowledge_points: list[dict],
    exam_questions: list[dict],
) -> dict[str, tuple[str, int, list[dict]]]:
    """派生每个知识点的考频。

    Returns:
        dict[kp_id, (frequency_label, score, matched_questions)]
        - frequency_label: HIGH / MEDIUM / LOW / NEVER
        - score: 考频分值（强命中 × STRONG_HIT_SCORE + 弱命中 × WEAK_HIT_SCORE × 年份权重）
        - matched_questions: 命中的真题列表（用于调试与验证）
    """
    # 1. 预处理真题：清洗 + 关键词提取
    processed_questions = []
    for eq in exam_questions:
        cleaned = clean_question_content(eq.get("content", ""))
        q_keywords = set(extract_keywords(cleaned))
        processed_questions.append({
            "id": eq.get("id"),
            "year": eq.get("year"),
            "subject": eq.get("subject"),
            "content": eq.get("content", ""),
            "cleaned": cleaned,
            "keywords": q_keywords,
        })

    # 2. 对每个知识点，与所有同科目真题匹配
    result: dict[str, tuple[str, int, list[dict]]] = {}

    for kp in knowledge_points:
        kp_id = kp.get("id")
        kp_subject = kp.get("subject", "")
        strong_kw, weak_kw = extract_kp_keywords(kp)

        if not strong_kw and not weak_kw:
            result[kp_id] = ("NEVER", 0, [])
            continue

        score = 0
        matched_questions = []

        for pq in processed_questions:
            # 科目过滤：跨科目匹配误判率高，强制同科目
            pq_subject = pq.get("subject", "")
            if kp_subject and pq_subject and kp_subject not in pq_subject and pq_subject not in kp_subject:
                continue

            q_keywords = pq["keywords"]
            if not q_keywords:
                continue

            # 强信号命中（长度 >= 3 的关键词）
            strong_hits = strong_kw & q_keywords
            # 弱信号命中（长度 == 2 的关键词）
            weak_hits = weak_kw & q_keywords

            # 至少 1 个强命中，或 >= 2 个弱命中（避免单字偶合）
            if not strong_hits and len(weak_hits) < 2:
                continue

            year_weight = compute_year_weight(pq.get("year") or CURRENT_YEAR)
            hit_score = (
                len(strong_hits) * STRONG_HIT_SCORE
                + len(weak_hits) * WEAK_HIT_SCORE
            ) * year_weight
            score += hit_score

            matched_questions.append({
                "eq_id": pq["id"],
                "year": pq["year"],
                "content": pq["content"][:80],
                "matched_keywords": sorted(strong_hits | weak_hits)[:5],
                "hit_score": round(hit_score, 1),
            })

        # 3. 分档
        if score >= HIGH_THRESHOLD:
            label = "HIGH"
        elif score >= MEDIUM_THRESHOLD:
            label = "MEDIUM"
        elif score >= LOW_THRESHOLD:
            label = "LOW"
        else:
            label = "NEVER"

        result[kp_id] = (label, round(score), matched_questions)

    return result


def main():
    parser = argparse.ArgumentParser(description="派生知识点考频（HIGH/MEDIUM/LOW/NEVER）")
    parser.add_argument(
        "--input",
        default=DEFAULT_SEED_PATH,
        help=f"seed_data.json 路径（默认: {DEFAULT_SEED_PATH}）",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="仅预览，不写入文件",
    )
    parser.add_argument(
        "--show-matches",
        action="store_true",
        help="显示 HIGH 知识点的匹配真题详情（调试用）",
    )
    args = parser.parse_args()

    seed_path = Path(args.input)
    if not seed_path.exists():
        print(f"错误：seed_data.json 不存在: {seed_path}", file=sys.stderr)
        sys.exit(1)

    # 1. 加载 seed_data.json
    print(f"加载: {seed_path}")
    with open(seed_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    kps = data.get("knowledge_points", [])
    eqs = data.get("exam_questions", [])

    print(f"  知识点数: {len(kps)}")
    print(f"  真题数: {len(eqs)}")

    # 2. 派生考频
    print("\n派生考频...")
    result = derive_frequency(kps, eqs)

    # 3. 统计
    from collections import Counter
    label_dist = Counter(r[0] for r in result.values())
    print("\n考频分布:")
    for label in ["HIGH", "MEDIUM", "LOW", "NEVER"]:
        count = label_dist.get(label, 0)
        pct = 100 * count / len(kps) if kps else 0
        print(f"  {label}: {count}/{len(kps)} ({pct:.1f}%)")

    # 4. 展示 HIGH 样本（调试）
    if args.show_matches:
        high_kps = [(kp_id, r) for kp_id, r in result.items() if r[0] == "HIGH"]
        high_kps.sort(key=lambda x: -x[1][1])  # 按分值降序
        print(f"\n=== HIGH 知识点 Top 10（按分值降序）===")
        kp_map = {kp["id"]: kp for kp in kps}
        for kp_id, (label, score, matches) in high_kps[:10]:
            kp = kp_map.get(kp_id, {})
            print(f"\n[{kp_id}] {kp.get('title', '?')} (score={score}, {len(matches)} 命中)")
            print(f"  subject: {kp.get('subject', '?')}")
            print(f"  tags: {kp.get('tags', [])}")
            for m in matches[:3]:
                print(f"  ← {m['year']} {m['eq_id']}: {m['content']}")
                print(f"    matched: {m['matched_keywords']} (score={m['hit_score']})")

    # 5. 更新或写入
    if args.dry_run:
        print("\n[--dry-run] 未写入文件")
    else:
        # 更新 knowledge_points 的 exam_frequency 字段
        for kp in kps:
            kp_id = kp.get("id")
            if kp_id in result:
                kp["exam_frequency"] = result[kp_id][0]

        # bump seed version（触发 App 重新导入）
        metadata = data.get("metadata", {})
        old_version = metadata.get("version", "0.0.0")
        # 简单版本 bump：patch +1
        parts = old_version.split(".")
        if len(parts) == 3 and parts[0].isdigit():
            parts[2] = str(int(parts[2]) + 1)
            new_version = ".".join(parts)
        else:
            new_version = old_version
        metadata["version"] = new_version
        data["metadata"] = metadata

        with open(seed_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

        print(f"\n已更新: {seed_path}")
        print(f"  seed version: {old_version} → {new_version}")
        print(f"  更新字段: exam_frequency")


if __name__ == "__main__":
    main()
