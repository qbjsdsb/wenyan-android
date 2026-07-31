#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v2.16.0 全面内容审计脚本（修正版）：
正确处理 angle/notes 为 JSON 字符串的情况。
"""
import json
import os
from collections import Counter, defaultdict

SEED_PATH = "/workspace/app/src/main/assets/seed_data.json"

VALID_SUBJECTS = {"中国古代文学", "中国现当代文学", "外国文学", "文学理论"}
VALID_EXAM_FREQ = {"HIGH", "MEDIUM", "LOW", "NEVER"}
VALID_DIFFICULTY = {1, 2, 3, 4, 5}

def load_data():
    with open(SEED_PATH, encoding="utf-8") as f:
        return json.load(f)

def safe_parse_json(s):
    """安全解析 JSON 字符串，若已是 dict 则直接返回"""
    if s is None:
        return None
    if isinstance(s, dict):
        return s
    if isinstance(s, str):
        try:
            return json.loads(s)
        except json.JSONDecodeError as e:
            return {"_parse_error": str(e)}
    return None

def audit_knowledge_points(data):
    """知识点审计"""
    issues = []
    kps = data.get("knowledge_points", [])
    print(f"\n{'='*70}")
    print(f"知识点审计：{len(kps)} 个")
    print(f"{'='*70}")

    # ID 唯一性
    ids = [kp.get("id") for kp in kps]
    id_counts = Counter(ids)
    dups = {k: v for k, v in id_counts.items() if v > 1}
    if dups:
        issues.append(("KP_ID_DUP", f"知识点 ID 重复: {dups}"))

    # 标题唯一性
    titles = [kp.get("title") for kp in kps]
    title_counts = Counter(titles)
    title_dups = {k: v for k, v in title_counts.items() if v > 1}
    if title_dups:
        issues.append(("KP_TITLE_DUP", f"知识点标题重复: {title_dups}"))

    short_text_count = 0
    invalid_subject_count = 0
    invalid_difficulty_count = 0
    invalid_freq_count = 0
    no_textbook_count = 0
    no_tags_count = 0

    for i, kp in enumerate(kps):
        kp_id = kp.get("id", f"index_{i}")

        study_text = kp.get("study_text", "")
        if len(study_text) < 100:
            short_text_count += 1
            if len(study_text) < 50:
                issues.append(("KP_STUDY_TEXT_TOO_SHORT", f"{kp_id}: study_text 仅 {len(study_text)} 字符"))

        if kp.get("subject") not in VALID_SUBJECTS:
            invalid_subject_count += 1
            issues.append(("KP_INVALID_SUBJECT", f"{kp_id}: subject='{kp.get('subject')}'"))

        diff = kp.get("difficulty")
        if diff not in VALID_DIFFICULTY:
            invalid_difficulty_count += 1
            issues.append(("KP_INVALID_DIFFICULTY", f"{kp_id}: difficulty={diff}"))

        if kp.get("exam_frequency") not in VALID_EXAM_FREQ:
            invalid_freq_count += 1
            issues.append(("KP_INVALID_FREQ", f"{kp_id}: exam_frequency='{kp.get('exam_frequency')}'"))

        if not kp.get("textbook_sources"):
            no_textbook_count += 1

        if not kp.get("tags"):
            no_tags_count += 1

    print(f"study_text < 100 字符: {short_text_count}")
    print(f"subject 无效: {invalid_subject_count}")
    print(f"difficulty 无效: {invalid_difficulty_count}")
    print(f"exam_frequency 无效: {invalid_freq_count}")
    print(f"无 textbook_sources: {no_textbook_count}")
    print(f"无 tags: {no_tags_count}")
    print(f"\n发现 {len(issues)} 个知识点问题")

    return issues, kps

def audit_exam_questions(data, kps):
    """论述题审计（正确处理 JSON 字符串）"""
    issues = []
    kps_by_id = {kp["id"]: kp for kp in kps}
    eqs = [q for q in data.get("exam_questions", []) if q.get("question_type") == "ESSAY"]
    print(f"\n{'='*70}")
    print(f"论述题审计：{len(eqs)} 道")
    print(f"{'='*70}")

    # ID 唯一性
    ids = [q.get("id") for q in eqs]
    id_counts = Counter(ids)
    dups = {k: v for k, v in id_counts.items() if v > 1}
    if dups:
        issues.append(("EQ_ID_DUP", f"论述题 ID 重复: {dups}"))

    no_angle_count = 0
    no_notes_count = 0
    angle_parse_error_count = 0
    notes_parse_error_count = 0
    no_evidences_count = 0
    no_knowledge_gaps_count = 0
    invalid_related_count = 0
    ocr_error_gap_count = 0
    angle_incomplete_count = 0

    for q in eqs:
        qid = q.get("id", "unknown")

        # angle 字段（JSON 字符串）
        angle_raw = q.get("angle")
        if not angle_raw:
            no_angle_count += 1
            issues.append(("EQ_NO_ANGLE", f"{qid}: 无 angle 字段"))
            angle = None
        else:
            angle = safe_parse_json(angle_raw)
            if angle is None or (isinstance(angle, dict) and "_parse_error" in angle):
                angle_parse_error_count += 1
                issues.append(("EQ_ANGLE_PARSE_ERROR", f"{qid}: angle JSON 解析失败 {angle}"))
                angle = None
            elif isinstance(angle, dict):
                # angle 必填子字段
                for sub in ["questionType", "coreKeywords", "task", "breakthroughAngles", "argumentPath"]:
                    if not angle.get(sub):
                        angle_incomplete_count += 1
                        issues.append(("EQ_ANGLE_INCOMPLETE", f"{qid}: angle.{sub} 为空"))

        # notes 字段（JSON 字符串）
        notes_raw = q.get("notes")
        if not notes_raw:
            no_notes_count += 1
            issues.append(("EQ_NO_NOTES", f"{qid}: 无 notes 字段"))
            continue
        notes = safe_parse_json(notes_raw)
        if notes is None or (isinstance(notes, dict) and "_parse_error" in notes):
            notes_parse_error_count += 1
            issues.append(("EQ_NOTES_PARSE_ERROR", f"{qid}: notes JSON 解析失败 {notes}"))
            continue

        # evidences
        evidences = notes.get("evidences", [])
        if not evidences:
            no_evidences_count += 1
            issues.append(("EQ_NO_EVIDENCES", f"{qid}: notes.evidences 为空"))

        # knowledgeGaps
        gaps = notes.get("knowledgeGaps", [])
        if not gaps:
            no_knowledge_gaps_count += 1

        # OCR 错误条目
        for gap in gaps:
            if isinstance(gap, dict):
                author = gap.get("author", "")
                if author == "原题OCR":
                    ocr_error_gap_count += 1
                    issues.append(("EQ_OCR_ERROR_GAP", f"{qid}: knowledgeGaps 含 '原题OCR' 错误条目"))

        # related_point_ids（注意字段名是 related_point_ids 不是 relatedPointIds）
        related_ids = q.get("related_point_ids", [])
        if not related_ids:
            # 也检查 angle 里的
            if angle and isinstance(angle, dict):
                related_ids = angle.get("relatedPointIds", [])
        for rid in related_ids:
            if rid not in kps_by_id:
                invalid_related_count += 1
                issues.append(("EQ_INVALID_RELATED", f"{qid}: related_point_ids 指向不存在的知识点 {rid}"))

    print(f"\n无 angle: {no_angle_count}")
    print(f"angle JSON 解析失败: {angle_parse_error_count}")
    print(f"angle 子字段为空: {angle_incomplete_count}")
    print(f"无 notes: {no_notes_count}")
    print(f"notes JSON 解析失败: {notes_parse_error_count}")
    print(f"无 evidences: {no_evidences_count}")
    print(f"无 knowledgeGaps（空数组）: {no_knowledge_gaps_count}")
    print(f"OCR 错误条目: {ocr_error_gap_count}")
    print(f"无效 related_point_ids: {invalid_related_count}")
    print(f"\n发现 {len(issues)} 个论述题问题")

    return issues, eqs

def audit_cross_references(kps, eqs):
    """交叉引用审计"""
    print(f"\n{'='*70}")
    print(f"交叉引用审计")
    print(f"{'='*70}")

    eq_to_kps = {}
    for q in eqs:
        related = q.get("related_point_ids", [])
        if not related:
            angle = safe_parse_json(q.get("angle"))
            if angle and isinstance(angle, dict):
                related = angle.get("relatedPointIds", [])
        eq_to_kps[q["id"]] = related

    kp_to_eqs = defaultdict(list)
    for eq_id, kp_ids in eq_to_kps.items():
        for kp_id in kp_ids:
            kp_to_eqs[kp_id].append(eq_id)

    referenced_kps = set(kp_to_eqs.keys())
    total_kps = len(kps)
    print(f"\n被论述题引用的知识点: {len(referenced_kps)} / {total_kps} ({100*len(referenced_kps)/total_kps:.1f}%)")

    # 新增 25 个知识点的关联状态
    new_kp_ids = [f"kp_{i:05d}" for i in range(911, 936)]
    print(f"\n新增 25 个知识点的关联状态:")
    linked_count = 0
    for kp_id in new_kp_ids:
        kp = next((k for k in kps if k["id"] == kp_id), None)
        if kp:
            eq_ids = kp_to_eqs.get(kp_id, [])
            if eq_ids:
                linked_count += 1
                print(f"  ✅ {kp_id} ({kp['title'][:30]}): 关联 {len(eq_ids)} 道论述题")
            else:
                print(f"  ⚠ {kp_id} ({kp['title'][:30]}): 未被论述题直接关联")
    print(f"\n新增知识点被论述题关联: {linked_count} / 25")

def audit_short_essays(data):
    """检查短 content 的论述题是否合理"""
    print(f"\n{'='*70}")
    print(f"短 content 论述题检查")
    print(f"{'='*70}")

    eqs = [q for q in data.get("exam_questions", []) if q.get("question_type") == "ESSAY"]
    short_essays = [q for q in eqs if len(q.get("content", "")) < 20]
    print(f"\ncontent < 20 字符的论述题: {len(short_essays)} 道")
    print("（这些可能是分小题形式的论述题，需人工核实是否合理）\n")
    for q in short_essays[:15]:
        print(f"  {q['id']} ({q.get('year')}-{q.get('exam_paper_code', q.get('paper'))}): {q.get('content')}")

def main():
    print(f"加载: {SEED_PATH}")
    data = load_data()
    print(f"seed 版本: {data['metadata']['version']}")
    print(f"知识点总数: {len(data.get('knowledge_points', []))}")
    print(f"真题总数: {len(data.get('exam_questions', []))}")

    all_issues = []

    kp_issues, kps = audit_knowledge_points(data)
    all_issues.extend(kp_issues)

    eq_issues, eqs = audit_exam_questions(data, kps)
    all_issues.extend(eq_issues)

    audit_cross_references(kps, eqs)
    audit_short_essays(data)

    print(f"\n{'='*70}")
    print(f"审计汇总")
    print(f"{'='*70}")
    print(f"\n总问题数: {len(all_issues)}")

    if all_issues:
        print(f"\n问题清单（前 30）:")
        for i, (code, desc) in enumerate(all_issues[:30]):
            print(f"  [{i+1}] {code}: {desc}")
        if len(all_issues) > 30:
            print(f"  ... 还有 {len(all_issues) - 30} 个问题")

    issue_types = Counter(code for code, _ in all_issues)
    print(f"\n问题类型统计:")
    for code, cnt in issue_types.most_common():
        print(f"  {code}: {cnt}")

if __name__ == "__main__":
    main()
