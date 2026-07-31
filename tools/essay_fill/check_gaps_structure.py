#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查非标准题型 + 短题框架 + knowledgeGaps 结构"""
import json
from collections import Counter

SEED_PATH = "/workspace/app/src/main/assets/seed_data.json"

with open(SEED_PATH, encoding="utf-8") as f:
    data = json.load(f)

print("="*70)
print("1. 非标准 question_type 检查")
print("="*70)
non_standard = [q for q in data["exam_questions"]
                if q.get("question_type") not in ("ESSAY", "TERM_EXPLANATION", "SHORT_ANSWER")]
print(f"数量: {len(non_standard)}")
for q in non_standard:
    print(f"  {q['id']} ({q.get('year')}-{q.get('exam_paper_code')}): type={q.get('question_type')}")
    print(f"    content: {q.get('content','')[:80]}")

print("\n" + "="*70)
print("2. 短 content 论述题的 answer_framework 检查")
print("="*70)
essays = [q for q in data["exam_questions"] if q.get("question_type") == "ESSAY"]
short_essays = [q for q in essays if len(q.get("content", "")) < 20]
print(f"短 content 论述题: {len(short_essays)} 道")
no_framework = 0
short_framework = 0
for q in short_essays:
    af = q.get("answer_framework", "")
    if not af:
        no_framework += 1
        print(f"  ❌ {q['id']}: 无 answer_framework — content='{q.get('content')}'")
    elif len(af) < 50:
        short_framework += 1
        print(f"  ⚠ {q['id']}: answer_framework 仅 {len(af)} 字符")
print(f"\n无 answer_framework: {no_framework}")
print(f"answer_framework < 50 字符: {short_framework}")
if no_framework == 0 and short_framework == 0:
    print("✅ 所有短 content 论述题都有充实的 answer_framework")

print("\n" + "="*70)
print("3. knowledgeGaps 结构检查")
print("="*70)
# 检查 knowledgeGaps 的字段结构
gap_fields = Counter()
gap_samples = []
for q in essays:
    notes = json.loads(q["notes"]) if isinstance(q.get("notes"), str) else q.get("notes", {})
    gaps = notes.get("knowledgeGaps", [])
    for g in gaps:
        if isinstance(g, dict):
            for k in g.keys():
                gap_fields[k] += 1
            if len(gap_samples) < 5:
                gap_samples.append(g)

print(f"knowledgeGaps 字段统计: {dict(gap_fields)}")
print(f"\nknowledgeGaps 样例（前 5）:")
for i, g in enumerate(gap_samples):
    print(f"  [{i+1}] {json.dumps(g, ensure_ascii=False)}")

print("\n" + "="*70)
print("4. 所有 question_type 分布")
print("="*70)
qt_dist = Counter(q.get("question_type") for q in data["exam_questions"])
for qt, cnt in qt_dist.most_common():
    print(f"  {qt}: {cnt}")

print("\n" + "="*70)
print("5. 论述题年份/卷别/学科分布")
print("="*70)
print(f"年份分布: {dict(sorted(Counter(q.get('year') for q in essays).items()))}")
print(f"卷别分布: {dict(sorted(Counter(q.get('exam_paper_code') for q in essays).items(), key=lambda x: str(x[0])))}")
print(f"学科分布: {dict(Counter(q.get('subject') for q in essays))}")

# 检查 score 分布
print(f"\n论述题分值分布:")
score_dist = Counter(q.get("score") for q in essays)
for s, cnt in sorted(score_dist.items(), key=lambda x: str(x[0])):
    print(f"  {s} 分: {cnt} 道")
