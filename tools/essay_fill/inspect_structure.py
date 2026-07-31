#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查 seed_data.json 的真实结构"""
import json
import os
from collections import Counter

SEED_PATH = "/workspace/app/src/main/assets/seed_data.json"

with open(SEED_PATH, encoding="utf-8") as f:
    data = json.load(f)

print("=== metadata ===")
print(json.dumps(data["metadata"], ensure_ascii=False, indent=2)[:500])

print("\n=== 知识点样例（第一个）===")
kp0 = data["knowledge_points"][0]
print(json.dumps(kp0, ensure_ascii=False, indent=2)[:800])

print("\n=== 知识点字段统计 ===")
all_fields = set()
for kp in data["knowledge_points"]:
    all_fields.update(kp.keys())
print(f"所有字段: {sorted(all_fields)}")

print("\n=== exam_frequency 值分布 ===")
freq_dist = Counter(kp.get("exam_frequency") for kp in data["knowledge_points"])
print(freq_dist)

print("\n=== textbook_sources 值分布 ===")
ts_dist = Counter()
for kp in data["knowledge_points"]:
    for ts in kp.get("textbook_sources", []):
        ts_dist[ts] += 1
print(ts_dist)

print("\n=== 论述题样例（第一个 ESSAY）===")
essays = [q for q in data["exam_questions"] if q.get("question_type") == "ESSAY"]
print(f"论述题总数: {len(essays)}")
eq0 = essays[0]
print(json.dumps(eq0, ensure_ascii=False, indent=2)[:1500])

print("\n=== 论述题字段统计 ===")
all_eq_fields = set()
for q in essays:
    all_eq_fields.update(q.keys())
print(f"所有字段: {sorted(all_eq_fields)}")

print("\n=== angle 字段类型 ===")
angle_types = Counter(type(q.get("angle")).__name__ for q in essays)
print(angle_types)

print("\n=== notes 字段类型 ===")
notes_types = Counter(type(q.get("notes")).__name__ for q in essays)
print(notes_types)

print("\n=== angle 非空的样例 ===")
for q in essays:
    if q.get("angle"):
        print(f"id={q['id']}, angle type={type(q['angle']).__name__}")
        print(json.dumps(q["angle"], ensure_ascii=False, indent=2)[:500])
        break

print("\n=== notes 非空的样例 ===")
for q in essays:
    if q.get("notes"):
        print(f"id={q['id']}, notes type={type(q['notes']).__name__}")
        print(json.dumps(q["notes"], ensure_ascii=False, indent=2)[:500])
        break

print("\n=== content < 20 字符的论述题 ===")
short_essays = [q for q in essays if len(q.get("content", "")) < 20]
print(f"数量: {len(short_essays)}")
for q in short_essays[:5]:
    print(f"  {q['id']} ({q.get('year')}-{q.get('paper')}): content='{q.get('content')}'")

print("\n=== 所有真题 question_type 分布 ===")
qt_dist = Counter(q.get("question_type") for q in data["exam_questions"])
print(qt_dist)
