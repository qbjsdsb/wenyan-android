#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Verify the public-recall 2025 805 correction batch.

This batch deliberately does not invent the missing sixth term or any
unpublished 2024 questions.  It only verifies the nine records whose text is
recoverable from the cited public page and whose existing IDs are preserved.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "app/src/main/assets/seed_data.json"

SOURCE_URL = "https://www.kaoyany.top/post/235365.html"

EXPECTED = {
    "eq_0533": {"content": "《罗兰之歌》", "question_type": "TERM_EXPLANATION", "score": 5},
    "eq_0534": {"content": "大学才子派", "question_type": "TERM_EXPLANATION", "score": 5},
    "eq_0535": {"content": "狂飙突进运动", "question_type": "TERM_EXPLANATION", "score": 5},
    "eq_0536": {"content": "波尔金诺之秋", "question_type": "TERM_EXPLANATION", "score": 5},
    "eq_0537": {"content": "约克纳帕塔法世系", "question_type": "TERM_EXPLANATION", "score": 5},
    "eq_0538": {
        "content": "1. 列举几部作品，谈谈你对十八世纪书信体小说的认识。",
        "question_type": "SHORT_ANSWER",
        "score": 15,
    },
    "eq_0539": {
        "content": "2. 雨果在《克伦威尔序言》中提出了哪些浪漫主义原则，结合作品展开分析。",
        "question_type": "SHORT_ANSWER",
        "score": 15,
    },
    "eq_0540": {
        "content": "3. 结合作品简要评析多余人形象系列。",
        "question_type": "SHORT_ANSWER",
        "score": 15,
    },
    "eq_0541": {
        "content": "4. “艺术是表现不是再现”，请结合作品具体分析。",
        "question_type": "SHORT_ANSWER",
        "score": 15,
    },
}


def normalize(value: str) -> str:
    return re.sub(r"[\s《》〈〉“”‘’\"'、，,。！？：:；;（）()【】\[\]…—–\-]", "", value).lower()


def main() -> int:
    with SEED.open(encoding="utf-8") as handle:
        seed = json.load(handle)

    errors: list[str] = []
    exams = seed.get("exam_questions", [])
    by_id = {item.get("id"): item for item in exams}
    if len(by_id) != len(exams):
        errors.append("全库真题 ID 不唯一")

    for exam_id, expected in EXPECTED.items():
        current = by_id.get(exam_id)
        if current is None:
            errors.append(f"缺少 {exam_id}")
            continue
        if current.get("year") != 2025:
            errors.append(f"{exam_id}: year={current.get('year')!r}")
        if current.get("subject") != "外国文学":
            errors.append(f"{exam_id}: subject={current.get('subject')!r}")
        if current.get("exam_paper_code") != "805":
            errors.append(f"{exam_id}: exam_paper_code={current.get('exam_paper_code')!r}")
        for key, value in expected.items():
            if current.get(key) != value:
                errors.append(f"{exam_id}: {key}={current.get(key)!r}, expected={value!r}")
        if len(str(current.get("answer_framework", "")).strip()) < 60:
            errors.append(f"{exam_id}: answer_framework 过短")

    foreign_801 = [
        item.get("id")
        for item in exams
        if item.get("year") == 2025
        and item.get("exam_paper_code") == "801"
        and item.get("subject") == "外国文学"
    ]
    if foreign_801:
        errors.append(f"2025 年仍有外国文学题误标 801: {foreign_801}")

    normalized: dict[str, str] = {}
    historical_duplicate_pairs: list[tuple[str, str]] = []
    for item in exams:
        key = normalize(str(item.get("content", "")))
        if not key:
            errors.append(f"{item.get('id')}: 题干为空")
            continue
        previous = normalized.get(key)
        if previous is not None:
            pair = (previous, str(item.get("id")))
            historical_duplicate_pairs.append(pair)
            if previous in EXPECTED or str(item.get("id")) in EXPECTED:
                errors.append(f"本批题目与既有题目规范化重复: {previous} 与 {item.get('id')}")
        normalized[key] = str(item.get("id"))

    version = seed.get("metadata", {}).get("version")
    if version != "2.26.0":
        errors.append(f"seed metadata.version={version!r}, expected='2.26.0'")

    print(json.dumps({
        "source": SOURCE_URL,
        "total_exam_questions": len(exams),
        "corrected_2025_805_questions": len(EXPECTED),
        "foreign_801_remaining": foreign_801,
        "historical_duplicate_pairs": len(historical_duplicate_pairs),
        "errors": len(errors),
    }, ensure_ascii=False))
    if errors:
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("v2.26 805 校验通过")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
