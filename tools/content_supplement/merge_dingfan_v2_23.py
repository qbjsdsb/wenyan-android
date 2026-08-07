#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""对丁帆《中国新文学史》补充卡执行来源守卫式合并。

本脚本将 OCR 物理页、教材印刷页和人工整理的知识卡分开处理：
候选 JSON 中的 source_evidence/framework_node/OCR 辅助字段不会进入 App
种子；只有通过 OCR 锚点、版本、标题去重和基线不变性检查后，显式传入
--apply 才会追加知识点。
"""

from __future__ import annotations

import argparse
import copy
import json
import re
import sys
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
SEED_PATH = REPO_ROOT / "app/src/main/assets/seed_data.json"
CANDIDATE_PATH = REPO_ROOT / "tools/content_supplement/dingfan_cards_v2_23.json"
REPORT_PATH = REPO_ROOT / "docs/research/dingfan-supplement-v2.23.json"
DEFAULT_OCR_ROOT = REPO_ROOT.parent / "tools_unpacked" / "output"

BASE_VERSION = "2.22.0"
TARGET_VERSION = "2.23.0"
BASE_COUNT = 993
FIRST_NEW_NUMBER = 994
CARD_COUNT = 20
EXPECTED_SOURCE_EDITION = "2013年4月第1版"
AUXILIARY_FIELDS = {
    "framework_node",
    "ocr_file",
    "ocr_physical_pages",
    "anchor_terms",
    "source_evidence",
}
REQUIRED_CARD_FIELDS = {
    "id",
    "title",
    "summary",
    "core_conclusion",
    "study_text",
    "full_content",
    "subject",
    "tags",
    "difficulty",
    "conflict_flag",
    "entities",
    "relations",
    "source_count",
    "textbook_sources",
    "merged_at",
    "exam_frequency",
    "framework_node",
    "ocr_file",
    "ocr_physical_pages",
    "anchor_terms",
    "source_evidence",
}
VALID_FREQ = {"HIGH", "MEDIUM", "LOW", "NEVER"}


def load_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def dump_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(value, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def canonical(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def normalized_title(value: str) -> str:
    return re.sub(r"[\s《》〈〉“”‘’\"'、，,。！？：:；;（）()【】\[\]…—–\-]", "", value).lower()


def max_kp_number(seed: dict[str, Any]) -> int:
    numbers = []
    for point in seed.get("knowledge_points", []):
        match = re.fullmatch(r"kp_(\d+)", str(point.get("id", "")))
        if match:
            numbers.append(int(match.group(1)))
    return max(numbers, default=0)


def load_ocr_pages(ocr_root: Path, file_name: str, errors: list[str]) -> dict[int, str]:
    path = ocr_root / file_name
    if not path.exists():
        errors.append(f"OCR 文件不存在: {path}")
        return {}
    try:
        payload = load_json(path)
        data = payload["data"]
        if data.get("ocr_status") != "VERIFIED" and payload.get("ocr_status") != "VERIFIED":
            errors.append(f"{file_name}: OCR 状态不是 VERIFIED")
        pages = data["pages"]
    except (OSError, KeyError, TypeError, json.JSONDecodeError) as exc:
        errors.append(f"OCR JSON 无法读取 {path}: {exc}")
        return {}
    result: dict[int, str] = {}
    for page in pages:
        try:
            number = int(page["page_num"])
        except (KeyError, TypeError, ValueError):
            continue
        text = page.get("text", "")
        result[number] = text if isinstance(text, str) else str(text)
    return result


def verify_ocr(card: dict[str, Any], ocr_root: Path, errors: list[str], label: str) -> None:
    file_name = card.get("ocr_file")
    pages = card.get("ocr_physical_pages")
    anchors = card.get("anchor_terms")
    if not isinstance(file_name, str) or not file_name:
        errors.append(f"{label}: ocr_file 缺失")
        return
    if not isinstance(pages, list) or not pages or any(not isinstance(item, int) for item in pages):
        errors.append(f"{label}: ocr_physical_pages 无效")
        return
    if pages != sorted(set(pages)):
        errors.append(f"{label}: OCR 物理页码必须升序且不重复")
    if not isinstance(anchors, list) or not anchors or any(not isinstance(item, str) or not item.strip() for item in anchors):
        errors.append(f"{label}: anchor_terms 无效")
        return
    page_text = load_ocr_pages(ocr_root, file_name, errors)
    missing = [number for number in pages if number not in page_text]
    if missing:
        errors.append(f"{label}: OCR 物理页不存在: {missing}")
    selected = "\n".join(page_text.get(number, "") for number in pages)
    for anchor in anchors:
        if anchor not in selected:
            errors.append(f"{label}: OCR 锚点未复现: {anchor}")


def verify_edition(ocr_root: Path, errors: list[str]) -> None:
    for file_name in ("file_131.json", "file_132.json"):
        path = ocr_root / file_name
        if not path.exists():
            errors.append(f"教材 OCR 文件不存在，无法核对版本: {path}")
            continue
        try:
            payload = load_json(path)
            first_pages = payload["data"]["pages"][:5]
            first_text = "\n".join(page.get("text", "") for page in first_pages)
        except (OSError, KeyError, TypeError, json.JSONDecodeError) as exc:
            errors.append(f"教材版本页读取失败 {path}: {exc}")
            continue
        if EXPECTED_SOURCE_EDITION not in first_text:
            errors.append(f"{file_name}: 版权页未复现版本标记 {EXPECTED_SOURCE_EDITION}")


def verify_cards(candidate: dict[str, Any], seed: dict[str, Any], ocr_root: Path) -> list[str]:
    errors: list[str] = []
    if candidate.get("base_version") != BASE_VERSION:
        errors.append(f"候选基线版本错误: {candidate.get('base_version')!r}")
    if candidate.get("target_version") != TARGET_VERSION:
        errors.append(f"候选目标版本错误: {candidate.get('target_version')!r}")
    if candidate.get("base_count") != BASE_COUNT:
        errors.append(f"候选基线数量错误: {candidate.get('base_count')!r}")
    cards = candidate.get("cards")
    if not isinstance(cards, list) or len(cards) != CARD_COUNT:
        errors.append(f"候选卡数量错误: {len(cards) if isinstance(cards, list) else cards!r}，预期 {CARD_COUNT}")
        return errors
    expected_ids = [f"kp_{number:05d}" for number in range(FIRST_NEW_NUMBER, FIRST_NEW_NUMBER + CARD_COUNT)]
    actual_ids = [card.get("id") if isinstance(card, dict) else None for card in cards]
    if actual_ids != expected_ids:
        errors.append(f"候选 ID 不连续或顺序错误: {actual_ids!r}")

    existing = seed.get("knowledge_points", [])
    existing_titles = {str(point.get("title")): point.get("id") for point in existing}
    existing_normalized = {normalized_title(str(point.get("title"))): point.get("id") for point in existing}
    seen_normalized: dict[str, str] = {}
    for index, card in enumerate(cards):
        label = f"card[{index}]"
        if not isinstance(card, dict):
            errors.append(f"{label}: 不是对象")
            continue
        missing = REQUIRED_CARD_FIELDS - set(card)
        if missing:
            errors.append(f"{label}: 缺少字段 {sorted(missing)}")
        title = card.get("title")
        if not isinstance(title, str) or not title.strip():
            errors.append(f"{label}: 标题为空")
            continue
        if title in existing_titles:
            errors.append(f"{label}: 标题与旧卡重复: {title} ({existing_titles[title]})")
        normalized = normalized_title(title)
        if normalized in existing_normalized:
            errors.append(f"{label}: 规范化标题与旧卡重复: {title} -> {existing_normalized[normalized]}")
        if normalized in seen_normalized:
            errors.append(f"{label}: 规范化标题与本批重复: {title} / {seen_normalized[normalized]}")
        seen_normalized[normalized] = title
        if card.get("subject") != "中国现当代文学":
            errors.append(f"{label}: subject 错误: {card.get('subject')!r}")
        if card.get("exam_frequency") not in VALID_FREQ:
            errors.append(f"{label}: exam_frequency 无效: {card.get('exam_frequency')!r}")
        if card.get("difficulty") not in {1, 2, 3, 4, 5}:
            errors.append(f"{label}: difficulty 无效")
        for text_key in ("summary", "core_conclusion", "study_text", "full_content"):
            if not isinstance(card.get(text_key), str) or not card[text_key].strip():
                errors.append(f"{label}: {text_key} 为空")
        if isinstance(card.get("study_text"), str) and len(card["study_text"]) < 240:
            errors.append(f"{label}: study_text 过短: {title}")
        sources = card.get("textbook_sources")
        if not isinstance(sources, list) or len(sources) != 1 or not isinstance(sources[0], str):
            errors.append(f"{label}: textbook_sources 必须恰有一个来源")
        elif "丁帆" not in sources[0] or "2013年第一版" not in sources[0] or "印刷页" not in sources[0]:
            errors.append(f"{label}: 教材来源未明确丁帆、版本和印刷页码: {sources[0]}")
        if card.get("source_count") != len(sources or []):
            errors.append(f"{label}: source_count 不等于来源数")
        if not isinstance(card.get("framework_node"), str) or not card["framework_node"]:
            errors.append(f"{label}: framework_node 缺失")
        evidence = card.get("source_evidence")
        if not isinstance(evidence, dict) or evidence.get("edition") != EXPECTED_SOURCE_EDITION:
            errors.append(f"{label}: source_evidence 版本不准确")
        verify_ocr(card, ocr_root, errors, label)
    return errors


def strip_auxiliary(card: dict[str, Any]) -> dict[str, Any]:
    return {key: copy.deepcopy(value) for key, value in card.items() if key not in AUXILIARY_FIELDS}


def build_candidate(seed: dict[str, Any], candidate: dict[str, Any]) -> dict[str, Any]:
    result = copy.deepcopy(seed)
    for card in candidate["cards"]:
        result["knowledge_points"].append(strip_auxiliary(card))
    metadata = result.setdefault("metadata", {})
    note = f"v{TARGET_VERSION} 补充丁帆《中国新文学史》上下册 OCR 复核专题：新增 {len(candidate['cards'])} 个现当代文学知识点"
    metadata["version"] = TARGET_VERSION
    metadata["generated_at"] = candidate.get("generated_at", metadata.get("generated_at"))
    description = metadata.get("description", "")
    if note not in description:
        metadata["description"] = f"{description} | {note}"
    fixes = metadata.setdefault("fixes", [])
    if note not in fixes:
        fixes.append(note)
    return result


def verify_old_unchanged(before: dict[str, Any], after: dict[str, Any], errors: list[str]) -> None:
    before_points = {point["id"]: point for point in before.get("knowledge_points", [])}
    after_points = {point["id"]: point for point in after.get("knowledge_points", [])}
    if set(before_points) - set(after_points):
        errors.append(f"旧知识点 ID 消失: {sorted(set(before_points) - set(after_points))}")
    for point_id, point in before_points.items():
        if point_id in after_points and canonical(point) != canonical(after_points[point_id]):
            errors.append(f"旧知识点字段被修改: {point_id}")
    for key in ("exam_questions", "writing_materials", "subjects"):
        if canonical(before.get(key)) != canonical(after.get(key)):
            errors.append(f"非知识点数据被修改: {key}")


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
        "source_edition": EXPECTED_SOURCE_EDITION,
        "source_files": [
            {
                "file_131.json": "file_131.json（丁帆《中国新文学史》上册）",
                "file_132.json": "file_132.json（丁帆《中国新文学史》下册）",
            }.get(file_name, file_name)
            for file_name in sorted({card["ocr_file"] for card in cards})
        ],
        "cards": [
            {
                "id": card["id"],
                "title": card["title"],
                "framework_node": card["framework_node"],
                "textbook_source": card["textbook_sources"][0],
                "source_evidence": card["source_evidence"],
            }
            for card in cards
        ],
        "known_limits": [
            "下册印刷页125—320的自动知识点文件没有覆盖正文，本批优先补入高频和结构性专题，但不声称完成全部逐作家卡。",
            "本批为人工整理的答题卡，仍需与框架映射、种子解析和 Kotlin 直接校验一起验收。",
        ],
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=Path, default=SEED_PATH)
    parser.add_argument("--candidates", type=Path, default=CANDIDATE_PATH)
    parser.add_argument("--report", type=Path, default=REPORT_PATH)
    parser.add_argument("--ocr-root", type=Path, default=DEFAULT_OCR_ROOT)
    parser.add_argument("--snapshot", type=Path)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--dry-run", action="store_true")
    mode.add_argument("--apply", action="store_true")
    mode.add_argument("--verify-applied", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        seed = load_json(args.seed)
        candidate = load_json(args.candidates)
    except (OSError, json.JSONDecodeError) as exc:
        print(f"读取输入失败: {exc}", file=sys.stderr)
        return 2

    if args.verify_applied:
        errors: list[str] = []
        cards = candidate.get("cards", [])
        if seed.get("metadata", {}).get("version") != TARGET_VERSION:
            errors.append(f"已应用种子版本错误: {seed.get('metadata', {}).get('version')!r}")
        if len(seed.get("knowledge_points", [])) != BASE_COUNT + CARD_COUNT:
            errors.append(f"已应用知识点数量错误: {len(seed.get('knowledge_points', []))}")
        if len(cards) != CARD_COUNT:
            errors.append("候选卡数量错误，无法验证已应用结果")
        else:
            by_id = {point.get("id"): point for point in seed.get("knowledge_points", [])}
            for card in cards:
                current = by_id.get(card.get("id"))
                if current is None:
                    errors.append(f"已应用种子缺少新 ID: {card.get('id')}")
                    continue
                expected = strip_auxiliary(card)
                if canonical(current) != canonical(expected):
                    errors.append(f"新卡写入后字段不一致: {card.get('id')}")
        if args.snapshot:
            try:
                before = load_json(args.snapshot)
                verify_old_unchanged(before, seed, errors)
            except (OSError, json.JSONDecodeError) as exc:
                errors.append(f"快照读取失败: {exc}")
        if errors:
            print("已应用结果校验失败:")
            print("\n".join(f"- {error}" for error in errors))
            return 1
        print(f"已应用结果校验通过: version={TARGET_VERSION} knowledge_points={len(seed['knowledge_points'])} old_data_unchanged=true")
        return 0

    errors = []
    if seed.get("metadata", {}).get("version") != BASE_VERSION:
        errors.append(f"当前种子不在基线版本 {BASE_VERSION}: {seed.get('metadata', {}).get('version')!r}")
    if len(seed.get("knowledge_points", [])) != BASE_COUNT:
        errors.append(f"当前种子不在基线数量 {BASE_COUNT}: {len(seed.get('knowledge_points', []))}")
    verify_edition(args.ocr_root, errors)
    errors.extend(verify_cards(candidate, seed, args.ocr_root))
    if errors:
        print("写入前校验失败，未修改种子数据:")
        print("\n".join(f"- {error}" for error in errors))
        return 1

    result = build_candidate(seed, candidate)
    verify_old_unchanged(seed, result, errors)
    if len(result.get("knowledge_points", [])) != BASE_COUNT + CARD_COUNT:
        errors.append("合并后的知识点数量不符合预期")
    if max_kp_number(result) != FIRST_NEW_NUMBER + CARD_COUNT - 1:
        errors.append("合并后的知识点 ID 最大值不符合预期")
    if errors:
        print("合并内存结果校验失败，未修改种子数据:")
        print("\n".join(f"- {error}" for error in errors))
        return 1

    report = build_report(candidate, seed, result, applied=args.apply)
    dump_json(args.report, report)
    if args.dry_run:
        print(f"写入前校验通过（未写入）: {len(seed['knowledge_points'])} -> {len(result['knowledge_points'])}; report={args.report}")
        return 0

    if args.snapshot:
        try:
            snapshot = load_json(args.snapshot)
            if canonical(snapshot) != canonical(seed):
                print("--snapshot 与当前 seed 不一致，拒绝写入。", file=sys.stderr)
                return 1
        except (OSError, json.JSONDecodeError) as exc:
            print(f"快照读取失败，拒绝写入: {exc}", file=sys.stderr)
            return 1
    dump_json(args.seed, result)
    print(f"已写入 {args.seed}: {len(seed['knowledge_points'])} -> {len(result['knowledge_points'])}; report={args.report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
