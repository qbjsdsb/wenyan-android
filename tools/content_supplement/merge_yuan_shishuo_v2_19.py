#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""严格合并袁世硕第二版第一批补充。

默认只做验证和预览；只有显式传入 --apply 才会写入 seed_data.json。
写入前会检查：基线版本、OCR 页面锚点、标题去重、字段完整性，以及旧 ID
除允许的教材来源字段外是否保持不变。
"""

from __future__ import annotations

import argparse
import copy
import difflib
import json
import re
import sys
from pathlib import Path
from typing import Any


VALID_SUBJECTS = {"中国古代文学", "中国现当代文学", "外国文学", "文学理论"}
VALID_FREQ = {"HIGH", "MEDIUM", "LOW", "NEVER"}
UNKNOWN_SOURCES = {"", "其他", "未知", "待补", "无", "N/A"}
AUXILIARY_FIELDS = {"framework_node", "ocr_file", "ocr_physical_pages", "anchor_terms"}
ALLOWED_SOURCE_CHANGES = {"textbook_sources", "source_count"}
REQUIRED_CARD_FIELDS = {
    "id", "title", "summary", "core_conclusion", "study_text", "subject", "tags",
    "difficulty", "conflict_flag", "entities", "relations", "source_count",
    "textbook_sources", "merged_at", "exam_frequency", "framework_node", "ocr_file",
    "ocr_physical_pages", "anchor_terms",
}


def load_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def dump_json(path: Path, value: Any) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(value, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def canonical(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def normalized_title(value: str) -> str:
    return re.sub(r"[\s《》〈〉“”‘’\"'、，,。！？：:；;（）()【】\[\]…—–\-]", "", value).lower()


def unique(values: list[str]) -> list[str]:
    result: list[str] = []
    for value in values:
        if value not in result:
            result.append(value)
    return result


def read_pages(ocr_root: Path, item: dict[str, Any], errors: list[str], label: str) -> dict[int, str]:
    file_name = item.get("ocr_file")
    if not file_name:
        errors.append(f"{label}: 缺少 ocr_file")
        return {}
    path = ocr_root / file_name
    if not path.exists():
        errors.append(f"{label}: OCR 文件不存在: {path}")
        return {}
    try:
        payload = load_json(path)
        pages = payload["data"]["pages"]
    except (OSError, KeyError, TypeError, json.JSONDecodeError) as exc:
        errors.append(f"{label}: OCR JSON 无法读取: {path} ({exc})")
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


def verify_ocr(item: dict[str, Any], pages: dict[int, str], errors: list[str], label: str) -> None:
    numbers = item.get("ocr_physical_pages")
    anchors = item.get("anchor_terms")
    if not isinstance(numbers, list) or not numbers:
        errors.append(f"{label}: 缺少 ocr_physical_pages")
        return
    if not isinstance(anchors, list) or not anchors:
        errors.append(f"{label}: 缺少 anchor_terms")
        return
    missing_pages = [number for number in numbers if int(number) not in pages]
    if missing_pages:
        errors.append(f"{label}: OCR 物理页不存在: {missing_pages}")
    selected = "\n".join(pages.get(int(number), "") for number in numbers)
    for anchor in anchors:
        if not isinstance(anchor, str) or not anchor.strip():
            errors.append(f"{label}: 存在空锚点")
        elif anchor not in selected:
            errors.append(f"{label}: 锚点未在指定 OCR 页复现: {anchor}")


def load_cards(card_paths: list[Path], errors: list[str]) -> list[dict[str, Any]]:
    cards: list[dict[str, Any]] = []
    for path in card_paths:
        try:
            payload = load_json(path)
        except (OSError, json.JSONDecodeError) as exc:
            errors.append(f"候选文件无法读取: {path} ({exc})")
            continue
        if not isinstance(payload, list):
            errors.append(f"候选文件必须是数组: {path}")
            continue
        for index, card in enumerate(payload):
            if not isinstance(card, dict):
                errors.append(f"{path.name}[{index}] 不是对象")
                continue
            card_copy = copy.deepcopy(card)
            card_copy["_batch_file"] = str(path)
            cards.append(card_copy)
    return cards


def verify_sources(
    manifest: dict[str, Any],
    seed: dict[str, Any],
    ocr_root: Path,
    errors: list[str],
) -> list[dict[str, Any]]:
    supplements = manifest.get("source_supplements", [])
    if not isinstance(supplements, list):
        errors.append("source_supplements 必须是数组")
        return []
    by_id = {kp.get("id"): kp for kp in seed.get("knowledge_points", [])}
    for index, item in enumerate(supplements):
        label = f"source_supplements[{index}]"
        if not isinstance(item, dict):
            errors.append(f"{label}: 不是对象")
            continue
        kp_id = item.get("id")
        target = by_id.get(kp_id)
        if target is None:
            errors.append(f"{label}: 目标 ID 不存在: {kp_id}")
        elif target.get("title") != item.get("expected_title"):
            errors.append(
                f"{label}: 标题守卫失败: {kp_id} 当前={target.get('title')!r} "
                f"预期={item.get('expected_title')!r}"
            )
        source = item.get("source", "")
        if "袁世硕" not in source or "第二版" not in source or "pp." not in source:
            errors.append(f"{label}: 来源缺少教材、版本或印刷页码: {source}")
        pages = read_pages(ocr_root, item, errors, label)
        verify_ocr(item, pages, errors, label)
    return supplements


def verify_cards(
    cards: list[dict[str, Any]],
    seed: dict[str, Any],
    ocr_root: Path,
    errors: list[str],
) -> None:
    existing = seed.get("knowledge_points", [])
    existing_titles = {str(kp.get("title")) for kp in existing}
    existing_normalized = {normalized_title(str(kp.get("title"))): kp.get("id") for kp in existing}
    seen_titles: set[str] = set()
    seen_normalized: set[str] = set()
    for index, card in enumerate(cards):
        label = f"card[{index}]"
        missing = REQUIRED_CARD_FIELDS - set(card)
        if missing:
            errors.append(f"{label}: 缺少字段 {sorted(missing)}")
        if card.get("id") != "AUTO":
            errors.append(f"{label}: id 必须为 AUTO，由合并程序分配稳定新 ID")
        title = card.get("title")
        if not isinstance(title, str) or not title.strip():
            errors.append(f"{label}: 标题为空")
            continue
        if title in existing_titles or title in seen_titles:
            errors.append(f"{label}: 标题重复: {title}")
        seen_titles.add(title)
        normalized = normalized_title(title)
        if normalized in existing_normalized:
            errors.append(f"{label}: 规范化标题与旧卡重复: {title} -> {existing_normalized[normalized]}")
        if normalized in seen_normalized:
            errors.append(f"{label}: 规范化标题与本批重复: {title}")
        seen_normalized.add(normalized)
        if card.get("subject") not in VALID_SUBJECTS:
            errors.append(f"{label}: subject 无效: {card.get('subject')}")
        if card.get("exam_frequency") not in VALID_FREQ:
            errors.append(f"{label}: exam_frequency 无效: {card.get('exam_frequency')}")
        if card.get("difficulty") not in {1, 2, 3, 4, 5}:
            errors.append(f"{label}: difficulty 无效: {card.get('difficulty')}")
        study_text = card.get("study_text", "")
        if not isinstance(study_text, str) or len(study_text) < 100:
            errors.append(f"{label}: study_text 少于 100 字: {title}")
        sources = card.get("textbook_sources")
        if not isinstance(sources, list) or not sources or any(not isinstance(s, str) for s in sources):
            errors.append(f"{label}: textbook_sources 无效: {title}")
        elif any("袁世硕" not in source or "第二版" not in source or "pp." not in source for source in sources):
            errors.append(f"{label}: 来源未明确标出袁世硕第二版和印刷页码: {title}")
        if card.get("source_count") != len(sources or []):
            errors.append(f"{label}: source_count 与 textbook_sources 不一致: {title}")
        if not isinstance(card.get("framework_node"), str) or not card["framework_node"]:
            errors.append(f"{label}: 缺少框架节点: {title}")
        pages = read_pages(ocr_root, card, errors, label)
        verify_ocr(card, pages, errors, label)

    for left in cards:
        for right in cards:
            if left is right:
                continue
            ratio = difflib.SequenceMatcher(None, normalized_title(left["title"]), normalized_title(right["title"])).ratio()
            if ratio >= 0.86:
                errors.append(f"本批存在高度相似标题，请人工确认: {left['title']} / {right['title']} ({ratio:.2f})")


def verify_applied_cards(
    cards: list[dict[str, Any]],
    seed: dict[str, Any],
    manifest: dict[str, Any],
    ocr_root: Path,
    errors: list[str],
) -> None:
    """验证已经写入 seed 的候选卡；OCR 辅助字段仍从候选文件读取。"""
    actual_version = seed.get("metadata", {}).get("version")
    target_version = manifest.get("target_version")
    if actual_version != target_version:
        errors.append(f"已应用结果版本不匹配: seed={actual_version!r}, manifest={target_version!r}")
    base_count = manifest.get("base_count")
    if isinstance(base_count, int) and len(seed.get("knowledge_points", [])) != base_count + len(cards):
        errors.append(
            f"已应用结果数量不匹配: seed={len(seed.get('knowledge_points', []))}, "
            f"预期={base_count + len(cards)}"
        )
    first_new_id = manifest.get("first_new_id")
    match = re.fullmatch(r"kp_(\d+)", str(first_new_id or ""))
    if not match:
        errors.append("manifest 缺少合法 first_new_id，无法验证新 ID 连续性")
        return
    first_number = int(match.group(1))
    by_title = {kp.get("title"): kp for kp in seed.get("knowledge_points", [])}
    for offset, card in enumerate(cards):
        label = f"applied_card[{offset}]"
        title = card.get("title")
        current = by_title.get(title)
        expected_id = f"kp_{first_number + offset:05d}"
        if current is None:
            errors.append(f"{label}: 当前 seed 缺少标题: {title}")
            continue
        if current.get("id") != expected_id:
            errors.append(f"{label}: ID 不符合预期: 当前={current.get('id')}, 预期={expected_id}")
        for key in REQUIRED_CARD_FIELDS - AUXILIARY_FIELDS - {"id"}:
            if key not in card:
                continue
            if canonical(current.get(key)) != canonical(card.get(key)):
                errors.append(f"{label}: 字段写入后不一致: {key} ({title})")
        pages = read_pages(ocr_root, card, errors, label)
        verify_ocr(card, pages, errors, label)


def verify_base(manifest: dict[str, Any], seed: dict[str, Any], errors: list[str]) -> None:
    expected = manifest.get("base_version")
    actual = seed.get("metadata", {}).get("version")
    if actual != expected:
        errors.append(f"基线版本不匹配: seed={actual!r}, manifest={expected!r}")


def build_candidate(
    seed: dict[str, Any],
    manifest: dict[str, Any],
    supplements: list[dict[str, Any]],
    cards: list[dict[str, Any]],
) -> tuple[dict[str, Any], list[dict[str, str]], dict[str, dict[str, Any]]]:
    candidate = copy.deepcopy(seed)
    old_by_id = {kp["id"]: copy.deepcopy(kp) for kp in seed.get("knowledge_points", [])}
    max_number = max(
        (int(match.group(1)) for kp in seed.get("knowledge_points", []) if (match := re.fullmatch(r"kp_(\d+)", kp.get("id", "")))),
        default=0,
    )
    assignments: list[dict[str, str]] = []
    for offset, original in enumerate(cards, start=1):
        card = {key: copy.deepcopy(value) for key, value in original.items() if key not in AUXILIARY_FIELDS and key != "_batch_file"}
        card["id"] = f"kp_{max_number + offset:05d}"
        card.setdefault("full_content", card.get("study_text", ""))
        card["source_count"] = len(card.get("textbook_sources", []))
        candidate["knowledge_points"].append(card)
        assignments.append({"id": card["id"], "title": card["title"], "node": original["framework_node"]})

    source_changes: dict[str, dict[str, Any]] = {}
    by_id = {kp["id"]: kp for kp in candidate["knowledge_points"]}
    for item in supplements:
        target = by_id[item["id"]]
        old_sources = [source for source in target.get("textbook_sources", []) if str(source).strip() not in UNKNOWN_SOURCES]
        new_sources = unique(old_sources + [item["source"]])
        target["textbook_sources"] = new_sources
        target["source_count"] = len(new_sources)
        source_changes[item["id"]] = {
            "title": target["title"],
            "old_sources": old_sources,
            "new_sources": new_sources,
        }

    metadata = candidate.setdefault("metadata", {})
    target_version = manifest["target_version"]
    metadata["version"] = target_version
    metadata["generated_at"] = manifest.get("generated_at", metadata.get("generated_at"))
    note = manifest.get("metadata_note") or (
        f"v{target_version} 袁世硕《中国古代文学史》（第二版）三册补充："
        f"新增 {len(cards)} 个专题并为 {len(supplements)} 个旧卡补回 OCR 页码来源"
    )
    description = metadata.get("description", "")
    if note not in description:
        metadata["description"] = f"{description} | {note}"
    fixes = metadata.setdefault("fixes", [])
    if note not in fixes:
        fixes.append(note)
    return candidate, assignments, source_changes


def verify_old_data_unchanged(
    old: dict[str, Any],
    new: dict[str, Any],
    allowed_source_ids: set[str],
    errors: list[str],
) -> None:
    old_by_id = {kp["id"]: kp for kp in old.get("knowledge_points", [])}
    new_by_id = {kp["id"]: kp for kp in new.get("knowledge_points", [])}
    if set(old_by_id) - set(new_by_id):
        errors.append("合并结果删除了旧知识点 ID")
    for kp_id, old_kp in old_by_id.items():
        new_kp = new_by_id.get(kp_id)
        if new_kp is None:
            continue
        if kp_id in allowed_source_ids:
            old_copy = {key: value for key, value in old_kp.items() if key not in ALLOWED_SOURCE_CHANGES}
            new_copy = {key: value for key, value in new_kp.items() if key not in ALLOWED_SOURCE_CHANGES}
        else:
            old_copy = old_kp
            new_copy = new_kp
        if canonical(old_copy) != canonical(new_copy):
            errors.append(f"旧卡出现未授权字段变化: {kp_id} {old_kp.get('title')}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--cards", type=Path, nargs="+", required=True)
    parser.add_argument("--ocr-root", type=Path, required=True)
    parser.add_argument("--report", type=Path)
    parser.add_argument("--snapshot", type=Path)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--verify-applied", action="store_true")
    args = parser.parse_args()

    seed = load_json(args.seed)
    manifest = load_json(args.manifest)
    errors: list[str] = []
    cards = load_cards(args.cards, errors)
    if args.verify_applied:
        verify_applied_cards(cards, seed, manifest, args.ocr_root, errors)
        supplements = verify_sources(manifest, seed, args.ocr_root, errors)
        report = {
            "batch_id": manifest.get("batch_id"),
            "target_version": manifest.get("target_version"),
            "errors": errors,
            "applied_count": len(seed.get("knowledge_points", [])),
            "verified_card_count": len(cards),
            "source_supplement_count": len(supplements),
        }
        if args.report:
            dump_json(args.report, report)
        print(json.dumps({key: report[key] for key in ("applied_count", "verified_card_count", "source_supplement_count")}, ensure_ascii=False))
        if errors:
            print("写入后验证失败:", file=sys.stderr)
            for error in errors:
                print(f"- {error}", file=sys.stderr)
            return 2
        print("写入后验证通过")
        return 0
    verify_base(manifest, seed, errors)
    supplements = verify_sources(manifest, seed, args.ocr_root, errors)
    verify_cards(cards, seed, args.ocr_root, errors)
    candidate, assignments, source_changes = build_candidate(seed, manifest, supplements, cards)
    verify_old_data_unchanged(seed, candidate, {item["id"] for item in supplements}, errors)

    report = {
        "batch_id": manifest.get("batch_id"),
        "base_version": manifest.get("base_version"),
        "target_version": manifest.get("target_version"),
        "errors": errors,
        "old_count": len(seed.get("knowledge_points", [])),
        "new_count": len(candidate.get("knowledge_points", [])),
        "added_count": len(cards),
        "source_supplement_count": len(supplements),
        "assignments": assignments,
        "source_changes": source_changes,
        "old_ids_preserved": not any("旧卡" in error or "删除了旧" in error for error in errors),
    }
    if args.report:
        dump_json(args.report, report)
    print(json.dumps({key: report[key] for key in ("old_count", "new_count", "added_count", "source_supplement_count", "old_ids_preserved")}, ensure_ascii=False))
    if errors:
        print("验证失败:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 2
    if not args.apply:
        print("验证通过；未写入 seed_data.json（预览模式）")
        return 0
    if args.snapshot:
        args.snapshot.parent.mkdir(parents=True, exist_ok=True)
        args.snapshot.write_text(args.seed.read_text(encoding="utf-8"), encoding="utf-8")
    dump_json(args.seed, candidate)
    print(f"已写入 {args.seed}: {report['old_count']} -> {report['new_count']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
