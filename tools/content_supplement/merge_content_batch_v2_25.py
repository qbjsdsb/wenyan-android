#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""对 v2.25 丁帆/聂珍钊教材候选卡执行守卫式合并。"""

from __future__ import annotations

import argparse
import copy
import json
import re
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
SEED_PATH = REPO_ROOT / "app/src/main/assets/seed_data.json"
CANDIDATE_PATH = REPO_ROOT / "tools/content_supplement/content_cards_v2_25.json"
REPORT_PATH = REPO_ROOT / "docs/research/content-supplement-v2.25.json"
OCR_ROOT = REPO_ROOT.parent / "tools_unpacked" / "output"

BASE_VERSION = "2.24.0"
TARGET_VERSION = "2.25.0"
BASE_COUNT = 1023
FIRST_NEW_NUMBER = 1024
CARD_COUNT = 78
VALID_SUBJECTS = {"中国现当代文学", "外国文学"}
VALID_FREQ = {"HIGH", "MEDIUM", "LOW", "NEVER"}
AUXILIARY_FIELDS = {"framework_node", "ocr_file", "ocr_physical_pages", "anchor_terms", "source_evidence"}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def dump(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def canonical(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def normalized_title(value: str) -> str:
    return re.sub(r"[\s《》〈〉“”‘’\"'、，,。！？：:；;（）()【】\[\]…—–\-]", "", value).lower()


def strip_auxiliary(card: dict[str, Any]) -> dict[str, Any]:
    return {key: copy.deepcopy(value) for key, value in card.items() if key not in AUXILIARY_FIELDS}


def load_ocr(file_name: str) -> dict[int, str]:
    payload = load(OCR_ROOT / file_name)
    return {int(page["page_num"]): str(page.get("text", "")) for page in payload["data"]["pages"]}


def verify_card_sources(card: dict[str, Any], errors: list[str], index: int) -> None:
    label = f"card[{index}] {card.get('id')}"
    file_name = card.get("ocr_file")
    pages = card.get("ocr_physical_pages")
    anchors = card.get("anchor_terms")
    evidence = card.get("source_evidence")
    if not isinstance(file_name, str) or not (OCR_ROOT / file_name).exists():
        errors.append(f"{label}: OCR 文件不存在: {file_name}")
        return
    if not isinstance(pages, list) or not pages or pages != sorted(set(pages)):
        errors.append(f"{label}: OCR 物理页必须是升序非空列表")
        return
    if not isinstance(anchors, list) or not anchors:
        errors.append(f"{label}: anchor_terms 为空")
        return
    page_text = load_ocr(file_name)
    selected = "\n".join(page_text.get(int(page), "") for page in pages)
    for page in pages:
        if int(page) not in page_text:
            errors.append(f"{label}: OCR 物理页不存在: {page}")
    for anchor in anchors:
        if anchor not in selected:
            errors.append(f"{label}: OCR 锚点未复现: {anchor}")
    if not isinstance(evidence, dict) or evidence.get("file") != file_name:
        errors.append(f"{label}: source_evidence 与 ocr_file 不一致")
    edition = str(evidence.get("edition", "")) if isinstance(evidence, dict) else ""
    if file_name in {"file_131.json", "file_132.json"} and "2013年4月第1版" not in edition:
        errors.append(f"{label}: 丁帆版本证据不准确: {edition}")
    if file_name == "file_090.json" and ("2015年7月第1版" not in edition or "用户提供" not in edition):
        errors.append(f"{label}: 聂珍钊版本证据不准确: {edition}")


def verify_candidate(candidate: dict[str, Any], seed: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if seed.get("metadata", {}).get("version") != BASE_VERSION:
        errors.append(f"当前种子不在基线版本 {BASE_VERSION}: {seed.get('metadata', {}).get('version')!r}")
    if len(seed.get("knowledge_points", [])) != BASE_COUNT:
        errors.append(f"当前种子不在基线数量 {BASE_COUNT}: {len(seed.get('knowledge_points', []))}")
    if errors:
        return errors
    if candidate.get("base_version") != BASE_VERSION:
        errors.append(f"候选基线版本错误: {candidate.get('base_version')!r}")
    if candidate.get("target_version") != TARGET_VERSION:
        errors.append(f"候选目标版本错误: {candidate.get('target_version')!r}")
    if candidate.get("base_count") != BASE_COUNT:
        errors.append(f"候选基线数量错误: {candidate.get('base_count')!r}")
    cards = candidate.get("cards")
    if not isinstance(cards, list) or len(cards) != CARD_COUNT:
        errors.append(f"候选卡数量错误: {len(cards) if isinstance(cards, list) else cards!r}")
        return errors
    expected_ids = [f"kp_{number:05d}" for number in range(FIRST_NEW_NUMBER, FIRST_NEW_NUMBER + CARD_COUNT)]
    if [card.get("id") for card in cards] != expected_ids:
        errors.append("候选 ID 不连续或顺序错误")

    old_points = seed.get("knowledge_points", [])
    old_titles = {str(point.get("title")): point.get("id") for point in old_points}
    old_normalized = {normalized_title(str(point.get("title"))): point.get("id") for point in old_points}
    seen: set[str] = set()
    for index, card in enumerate(cards):
        label = f"card[{index}]"
        if not isinstance(card, dict):
            errors.append(f"{label}: 不是对象")
            continue
        title = str(card.get("title", ""))
        if not title.strip():
            errors.append(f"{label}: 标题为空")
        if title in old_titles:
            errors.append(f"{label}: 标题与旧卡重复: {title}")
        normalized = normalized_title(title)
        if normalized in old_normalized or normalized in seen:
            errors.append(f"{label}: 规范化标题重复: {title}")
        seen.add(normalized)
        if card.get("subject") not in VALID_SUBJECTS:
            errors.append(f"{label}: subject 无效: {card.get('subject')!r}")
        if card.get("difficulty") not in {1, 2, 3, 4, 5}:
            errors.append(f"{label}: difficulty 无效")
        if card.get("exam_frequency") not in VALID_FREQ:
            errors.append(f"{label}: exam_frequency 无效")
        for key in ("summary", "core_conclusion", "study_text", "full_content"):
            if not isinstance(card.get(key), str) or not card[key].strip():
                errors.append(f"{label}: {key} 为空")
        if len(str(card.get("study_text", ""))) < 240:
            errors.append(f"{label}: study_text 过短")
        sources = card.get("textbook_sources")
        if not isinstance(sources, list) or len(sources) != 1 or not isinstance(sources[0], str):
            errors.append(f"{label}: textbook_sources 必须恰有一个字符串来源")
        if card.get("source_count") != len(sources or []):
            errors.append(f"{label}: source_count 与 textbook_sources 不一致")
        if not isinstance(card.get("framework_node"), str) or not card["framework_node"]:
            errors.append(f"{label}: framework_node 缺失")
        verify_card_sources(card, errors, index)
    return errors


def verify_old_unchanged(before: dict[str, Any], after: dict[str, Any], errors: list[str]) -> None:
    before_points = {point["id"]: point for point in before.get("knowledge_points", [])}
    after_points = {point["id"]: point for point in after.get("knowledge_points", [])}
    missing = sorted(set(before_points) - set(after_points))
    if missing:
        errors.append(f"旧知识点 ID 消失: {missing}")
    for point_id, point in before_points.items():
        if point_id in after_points and canonical(point) != canonical(after_points[point_id]):
            errors.append(f"旧知识点字段被修改: {point_id}")
    for key in ("exam_questions", "writing_materials", "subjects"):
        if canonical(before.get(key)) != canonical(after.get(key)):
            errors.append(f"非知识点数据被修改: {key}")


def build_result(seed: dict[str, Any], candidate: dict[str, Any]) -> dict[str, Any]:
    result = copy.deepcopy(seed)
    result["knowledge_points"].extend(strip_auxiliary(card) for card in candidate["cards"])
    metadata = result.setdefault("metadata", {})
    note = "v2.25.0 补充丁帆《中国新文学史》上册 21 条、下册 24 条及聂珍钊《外国文学史》上册 33 条教材 OCR 专题"
    metadata["version"] = TARGET_VERSION
    metadata["generated_at"] = candidate.get("generated_at", metadata.get("generated_at"))
    description = metadata.get("description", "")
    if note not in description:
        metadata["description"] = f"{description} | {note}"
    fixes = metadata.setdefault("fixes", [])
    if note not in fixes:
        fixes.append(note)
    return result


def build_report(candidate: dict[str, Any], before: dict[str, Any], after: dict[str, Any], applied: bool) -> dict[str, Any]:
    cards = candidate["cards"]
    return {
        "batch": candidate["batch"],
        "base_version": BASE_VERSION,
        "target_version": TARGET_VERSION,
        "seed_written": applied,
        "base_knowledge_point_count": len(before.get("knowledge_points", [])),
        "new_knowledge_point_count": len(cards),
        "result_knowledge_point_count": len(after.get("knowledge_points", [])),
        "old_ids_preserved": True,
        "exam_question_count_unchanged": len(before.get("exam_questions", [])) == len(after.get("exam_questions", [])),
        "writing_material_count_unchanged": len(before.get("writing_materials", [])) == len(after.get("writing_materials", [])),
        "counts_by_subject": {
            subject: sum(1 for card in cards if card.get("subject") == subject)
            for subject in sorted({card.get("subject") for card in cards})
        },
        "source_files": [
            {"file_131.json": "丁帆《中国新文学史》上册", "file_132.json": "丁帆《中国新文学史》下册", "file_090.json": "聂珍钊《外国文学史》上册（用户提供 OCR）"}.get(file_name, file_name)
            for file_name in sorted({card["ocr_file"] for card in cards})
        ],
        "cards": [
            {
                "id": card["id"],
                "title": card["title"],
                "subject": card["subject"],
                "framework_node": card["framework_node"],
                "textbook_source": card["textbook_sources"][0],
                "source_evidence": card["source_evidence"],
            }
            for card in cards
        ],
        "known_limits": [
            "本批是按目录和 OCR 锚点复核的独立专题增量，不宣称丁帆上下册已经逐作家、逐作品穷尽。",
            "聂珍钊上册来源是用户提供的 2015 年 7 月第 1 版 OCR；本批不把它伪标为官方 2018 年第二版。",
            "候选卡的 framework_node、OCR 页码和 source_evidence 只用于审核，合并后不进入 App seed。",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=Path, default=SEED_PATH)
    parser.add_argument("--candidates", type=Path, default=CANDIDATE_PATH)
    parser.add_argument("--report", type=Path, default=REPORT_PATH)
    parser.add_argument("--snapshot", type=Path)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--dry-run", action="store_true")
    mode.add_argument("--apply", action="store_true")
    mode.add_argument("--verify-applied", action="store_true")
    args = parser.parse_args()

    seed = load(args.seed)
    candidate = load(args.candidates)
    if args.verify_applied:
        errors: list[str] = []
        if len(seed.get("knowledge_points", [])) != BASE_COUNT + CARD_COUNT:
            errors.append(f"已应用知识点数量错误: {len(seed.get('knowledge_points', []))}")
        by_id = {point.get("id"): point for point in seed.get("knowledge_points", [])}
        for card in candidate.get("cards", []):
            current = by_id.get(card.get("id"))
            if current is None:
                errors.append(f"已应用种子缺少新 ID: {card.get('id')}")
            elif canonical(current) != canonical(strip_auxiliary(card)):
                errors.append(f"新卡写入后字段不一致: {card.get('id')}")
        if args.snapshot:
            verify_old_unchanged(load(args.snapshot), seed, errors)
        if errors:
            print("已应用结果校验失败:\n" + "\n".join(f"- {error}" for error in errors))
            return 1
        print(f"已应用结果校验通过: knowledge_points={len(seed['knowledge_points'])} old_data_unchanged=true")
        return 0

    errors = verify_candidate(candidate, seed)
    result = build_result(seed, candidate) if not errors else None
    if result is not None:
        verify_old_unchanged(seed, result, errors)
        if len(result.get("knowledge_points", [])) != BASE_COUNT + CARD_COUNT:
            errors.append("合并后的知识点数量不符合预期")
    if errors:
        print("写入前校验失败，未修改种子数据:\n" + "\n".join(f"- {error}" for error in errors))
        return 1
    report = build_report(candidate, seed, result, applied=args.apply)
    dump(args.report, report)
    if args.dry_run:
        print(f"写入前校验通过（未写入）: {BASE_COUNT} -> {BASE_COUNT + CARD_COUNT}; report={args.report}")
        return 0
    if args.snapshot:
        if canonical(load(args.snapshot)) != canonical(seed):
            print("--snapshot 与当前 seed 不一致，拒绝写入。")
            return 1
    dump(args.seed, result)
    print(f"已写入 {args.seed}: {BASE_COUNT} -> {BASE_COUNT + CARD_COUNT}; report={args.report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
