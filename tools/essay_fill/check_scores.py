#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查 score=0 的论述题，content 里是否包含分值信息"""
import json
import re
from collections import Counter

SEED_PATH = "/workspace/app/src/main/assets/seed_data.json"

with open(SEED_PATH, encoding="utf-8") as f:
    data = json.load(f)

essays = [q for q in data["exam_questions"] if q.get("question_type") == "ESSAY"]
zero_score = [q for q in essays if q.get("score") == 0 or q.get("score") is None]
print(f"score=0 的论述题: {len(zero_score)} / {len(essays)}")

# 检查 content 里是否有分值信息（如"30分""每题30分"等）
score_pattern = re.compile(r'(\d+)\s*分')
has_score_in_content = 0
extracted_scores = Counter()

for q in zero_score:
    content = q.get("content", "")
    matches = score_pattern.findall(content)
    if matches:
        has_score_in_content += 1
        # 取最后一个分值（通常是本题分值）
        extracted_scores[matches[-1]] += 1
    else:
        # 也检查 answer_framework
        af = q.get("answer_framework", "")
        matches_af = score_pattern.findall(af)
        if matches_af:
            has_score_in_content += 1
            extracted_scores[matches_af[-1]] += 1

print(f"content/answer_framework 中含分值信息: {has_score_in_content}")
print(f"可提取的分值分布: {dict(extracted_scores.most_common())}")

# 检查非零分值的论述题
non_zero = [q for q in essays if q.get("score") not in (0, None)]
print(f"\n非零分值论述题: {len(non_zero)}")
for q in non_zero[:10]:
    print(f"  {q['id']} ({q.get('year')}-{q.get('exam_paper_code')}): score={q.get('score')}, content={q.get('content','')[:50]}")

# 看看 score=0 的题目样例
print(f"\nscore=0 题目样例（前 10）:")
for q in zero_score[:10]:
    content = q.get("content", "")
    # 提取分值
    matches = score_pattern.findall(content)
    score_info = f"（content 含分值: {matches}）" if matches else ""
    print(f"  {q['id']} ({q.get('year')}-{q.get('exam_paper_code')}): score={q.get('score')}{score_info}")
    print(f"    content: {content[:80]}")
