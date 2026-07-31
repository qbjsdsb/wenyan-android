#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""抽样深度检查：论述题 angle/notes 内容质量 + 知识点学术准确性"""
import json

SEED_PATH = "/workspace/app/src/main/assets/seed_data.json"

with open(SEED_PATH, encoding="utf-8") as f:
    data = json.load(f)

essays = [q for q in data["exam_questions"] if q.get("question_type") == "ESSAY"]
kps = data["knowledge_points"]

# 1. 抽查 5 道论述题的完整 angle + notes 内容
print("="*70)
print("1. 论述题 angle + notes 完整内容抽查（5 道）")
print("="*70)

# 选择不同年份/学科的题目
sample_ids = ["eq_0019", "eq_0069", "eq_0075", "eq_0124", "eq_0254"]
for sid in sample_ids:
    q = next((e for e in essays if e["id"] == sid), None)
    if not q:
        continue
    print(f"\n{'─'*70}")
    print(f"题目: {q['id']} ({q.get('year')}-{q.get('exam_paper_code')}) [{q.get('subject')}]")
    print(f"content: {q.get('content', '')[:100]}")
    print(f"score: {q.get('score')}")

    angle = json.loads(q["angle"]) if isinstance(q.get("angle"), str) else q.get("angle", {})
    notes = json.loads(q["notes"]) if isinstance(q.get("notes"), str) else q.get("notes", {})

    print(f"\n【angle】")
    print(f"  questionType: {angle.get('questionType')}")
    print(f"  coreKeywords: {angle.get('coreKeywords')}")
    print(f"  limitKeywords: {angle.get('limitKeywords')}")
    print(f"  task: {angle.get('task')}")
    print(f"  breakthroughAngles: {angle.get('breakthroughAngles')}")
    print(f"  angleRationale: {angle.get('angleRationale', '')[:100]}...")
    arg_path = angle.get('argumentPath', {})
    if isinstance(arg_path, dict):
        print(f"  argumentPath.thesis: {arg_path.get('thesis', '')[:100]}...")
        points = arg_path.get('points', [])
        print(f"  argumentPath.points: {len(points)} 个论点")
        for p in points[:3]:
            print(f"    - {p.get('label', '?')}: {p.get('content', '')[:60]}...")

    print(f"\n【notes】")
    evidences = notes.get('evidences', [])
    print(f"  evidences: {len(evidences)} 条")
    for ev in evidences[:3]:
        print(f"    [{ev.get('type')}] {ev.get('source', '')[:40]}")
        print(f"      {ev.get('content', '')[:80]}...")

    cross_val = notes.get('crossValidation', {})
    if cross_val:
        print(f"  crossValidation: {json.dumps(cross_val, ensure_ascii=False)[:150]}...")

    ref_links = notes.get('referenceLinks', [])
    print(f"  referenceLinks: {len(ref_links)} 条")
    for rl in ref_links[:2]:
        print(f"    - {rl.get('title', rl.get('label', '?'))[:50]}")

    gaps = notes.get('knowledgeGaps', [])
    print(f"  knowledgeGaps: {len(gaps)} 个")
    for g in gaps[:2]:
        print(f"    - {g.get('author', '?')}: {g.get('note', '')[:60]}...")

# 2. 抽查 5 个新增知识点的完整内容
print(f"\n{'='*70}")
print("2. 新增知识点完整内容抽查（5 个）")
print("="*70)

sample_kp_ids = ["kp_00913", "kp_00917", "kp_00923", "kp_00930", "kp_00935"]
for kid in sample_kp_ids:
    kp = next((k for k in kps if k["id"] == kid), None)
    if not kp:
        continue
    print(f"\n{'─'*70}")
    print(f"{kp['id']} [{kp['subject']}] 难度{kp['difficulty']} 考频{kp['exam_frequency']}")
    print(f"标题: {kp['title']}")
    print(f"摘要: {kp['summary']}")
    print(f"core_conclusion: {kp['core_conclusion'][:150]}...")
    print(f"study_text: {kp['study_text'][:200]}...")
    print(f"tags: {kp['tags']}")
    print(f"entities: {kp['entities']}")
    print(f"textbook_sources: {kp['textbook_sources']}")

# 3. 检查论述题 argumentPath 的完整性
print(f"\n{'='*70}")
print("3. 论述题 argumentPath 完整性统计")
print("="*70)

no_thesis = 0
no_points = 0
short_points = 0
for q in essays:
    angle = json.loads(q["angle"]) if isinstance(q.get("angle"), str) else q.get("angle", {})
    arg_path = angle.get("argumentPath", {})
    if isinstance(arg_path, dict):
        if not arg_path.get("thesis"):
            no_thesis += 1
        points = arg_path.get("points", [])
        if not points:
            no_points += 1
        elif len(points) < 3:
            short_points += 1

print(f"无 thesis: {no_thesis} / {len(essays)}")
print(f"无 points: {no_points} / {len(essays)}")
print(f"points < 3 个: {short_points} / {len(essays)}")

# 4. 检查 evidences 的 type 分布
print(f"\n{'='*70}")
print("4. evidences type 分布")
print("="*70)
from collections import Counter
ev_types = Counter()
for q in essays:
    notes = json.loads(q["notes"]) if isinstance(q.get("notes"), str) else q.get("notes", {})
    for ev in notes.get("evidences", []):
        ev_types[ev.get("type", "UNKNOWN")] += 1
print(f"evidence type 分布: {dict(ev_types.most_common())}")

# 5. 检查 evidences 的 source 字段完整性
print(f"\n{'='*70}")
print("5. evidences source 字段完整性")
print("="*70)
no_source = 0
for q in essays:
    notes = json.loads(q["notes"]) if isinstance(q.get("notes"), str) else q.get("notes", {})
    for ev in notes.get("evidences", []):
        if not ev.get("source"):
            no_source += 1
            print(f"  ⚠ {q['id']}: evidence 无 source — type={ev.get('type')}, content={ev.get('content','')[:40]}")
print(f"\n无 source 的 evidence: {no_source}")
