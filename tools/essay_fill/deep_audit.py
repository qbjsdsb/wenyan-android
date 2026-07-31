#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""深度抽查：学术准确性 + 短题分类核实 + 非标准题型检查"""
import json
from collections import Counter

SEED_PATH = "/workspace/app/src/main/assets/seed_data.json"

with open(SEED_PATH, encoding="utf-8") as f:
    data = json.load(f)

print("="*70)
print("1. 非标准 question_type 检查（非 ESSAY/TERM_EXPLANATION/SHORT_ANSWER）")
print("="*70)
non_standard = [q for q in data["exam_questions"]
                if q.get("question_type") not in ("ESSAY", "TERM_EXPLANATION", "SHORT_ANSWER")]
print(f"数量: {len(non_standard)}")
for q in non_standard:
    print(f"  {q['id']} ({q.get('year')}-{q.get('exam_paper_code')}): type={q.get('question_type')}, content={q.get('content','')[:60]}")

print("\n" + "="*70)
print("2. 40 道短 content 论述题的 answer_framework 长度检查")
print("="*70)
# 短题但 answer_framework 充实 = 合理（分小题形式）
essays = [q for q in data["exam_questions"] if q.get("question_type") == "ESSAY"]
short_essays = [q for q in essays if len(q.get("content", "")) < 20]
print(f"短 content 论述题: {len(short_essays)} 道")
no_framework_count = 0
for q in short_essays:
    af = q.get("answer_framework", "")
    if not af or len(af) < 50:
        no_framework_count += 1
        print(f"  ⚠ {q['id']}: answer_framework 仅 {len(af)} 字符 — content='{q.get('content')}'")
print(f"answer_framework 不足的短题: {no_framework_count} / {len(short_essays)}")
if no_framework_count == 0:
    print("✅ 所有短 content 论述题都有充实的 answer_framework（分小题形式合理）")

print("\n" + "="*70)
print("3. 新增 25 个知识点学术准确性抽查")
print("="*70)
new_kps = [kp for kp in data["knowledge_points"] if kp["id"].startswith("kp_009")]
print(f"新增知识点: {len(new_kps)} 个")
print("\n抽查每个知识点的关键学术要素：")
for kp in new_kps:
    study = kp.get("study_text", "")
    conclusion = kp.get("core_conclusion", "")
    # 检查要素
    has_book = "《" in study  # 书名号
    has_textbook = any("《" in ts for ts in kp.get("textbook_sources", []))
    has_entity = len(kp.get("entities", [])) > 0
    study_len = len(study)
    conclusion_len = len(conclusion)

    # 检查教材引用标注
    textbook_refs = kp.get("textbook_sources", [])
    print(f"\n{kp['id']} {kp['title'][:35]}")
    print(f"  study_text: {study_len} 字符 | core_conclusion: {conclusion_len} 字符")
    print(f"  书名号: {'✅' if has_book else '❌'} | 教材引用: {textbook_refs} | entities: {len(kp.get('entities',[]))} 个")
    print(f"  tags: {kp.get('tags', [])}")
    print(f"  difficulty: {kp.get('difficulty')} | exam_frequency: {kp.get('exam_frequency')}")

    # 检查 study_text 是否有学者/教材作者署名
    scholars = ["袁行霈", "钱理群", "朱维之", "童庆炳", "鲁迅", "王富仁", "汪晖",
                "陈思和", "洪子诚", "夏志清", "陈寅恪", "朱光潜", "王季思",
                "姚斯", "布洛", "康德", "罗兰·巴特", "莱辛", "刘勰", "巴赫金",
                "朱立元", "赵毅衡", "程光炜"]
    found_scholars = [s for s in scholars if s in study]
    if found_scholars:
        print(f"  学者引用: {found_scholars}")
    else:
        print(f"  ⚠ 无已知学者/教材作者署名")

print("\n" + "="*70)
print("4. 论述题 evidences 学术标注抽查（前 10 题）")
print("="*70)
for q in essays[:10]:
    notes = json.loads(q["notes"]) if isinstance(q.get("notes"), str) else q.get("notes", {})
    evidences = notes.get("evidences", [])
    print(f"\n{q['id']} ({q.get('year')}-{q.get('exam_paper_code')}): {len(evidences)} 条 evidence")
    for i, ev in enumerate(evidences[:3]):
        ev_type = ev.get("type", "?")
        ev_source = ev.get("source", "?")
        ev_content = ev.get("content", "")[:60]
        print(f"  [{i+1}] type={ev_type}, source={ev_source}")
        print(f"      content={ev_content}...")

print("\n" + "="*70)
print("5. 论述题 knowledgeGaps 抽查（前 10 题有 gaps 的）")
print("="*70)
gap_count = 0
for q in essays:
    notes = json.loads(q["notes"]) if isinstance(q.get("notes"), str) else q.get("notes", {})
    gaps = notes.get("knowledgeGaps", [])
    if gaps:
        gap_count += 1
        if gap_count <= 10:
            print(f"\n{q['id']}: {len(gaps)} 个 gap")
            for g in gaps[:3]:
                if isinstance(g, dict):
                    print(f"  - author={g.get('author','?')}, topic={g.get('topic', g.get('content','?'))[:50]}")
                else:
                    print(f"  - {str(g)[:60]}")
print(f"\n共有 {gap_count} 道论述题有 knowledgeGaps 标注")

print("\n" + "="*70)
print("6. 年份/卷别分布检查")
print("="*70)
year_dist = Counter(q.get("year") for q in essays)
print(f"论述题年份分布: {dict(sorted(year_dist.items()))}")
paper_dist = Counter(q.get("exam_paper_code") for q in essays)
print(f"论述题卷别分布: {dict(sorted(paper_dist.items(), key=lambda x: str(x[0])))}")
subject_dist = Counter(q.get("subject") for q in essays)
print(f"论述题学科分布: {dict(subject_dist)}")
