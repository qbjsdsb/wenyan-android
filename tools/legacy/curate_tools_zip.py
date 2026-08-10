#!/usr/bin/env python3
"""Create a safe, metadata-only subset of the historical tools.zip archive.

The archive contains OCR text, exam answers, textbook material, local paths,
and environment artefacts.  This script deliberately emits only provenance
and audit metadata; it never copies OCR bodies, question text, answers, TOC
text, PDFs, installers, caches, or logs.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import zipfile
from collections import Counter
from pathlib import Path
from typing import Any


ARCHIVE_NAME = "tools.zip"
FORMAT_VERSION = "wenyan-tools-legacy-curated-v1"
NOISE_RE = re.compile(r"微信|QQ|淘宝|店铺|公众号|扫描全能王|加Q", re.IGNORECASE)


def read_json(archive: zipfile.ZipFile, member: str) -> Any:
    with archive.open(member) as stream:
        return json.loads(stream.read().decode("utf-8-sig"))


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def counts(items: list[dict[str, Any]], field: str) -> dict[str, int]:
    return dict(sorted(Counter(str(item.get(field) or "MISSING") for item in items).items()))


def archive_metadata(archive_path: Path, manifest: dict[str, Any], archive_sha256: str) -> dict[str, Any]:
    records = manifest.get("files", [])
    completed = [item for item in records if item.get("status") == "completed"]
    skipped = [item for item in records if item.get("status") == "skipped"]
    return {
        "name": ARCHIVE_NAME,
        "sha256": archive_sha256,
        "compressed_bytes": archive_path.stat().st_size,
        "manifest_scanned_at": manifest.get("scanned_at"),
        "manifest_total": manifest.get("total"),
        "manifest_completed": len(completed),
        "manifest_skipped": len(skipped),
        "manifest_categories": counts(records, "category"),
        "manifest_file_types": counts(records, "file_type"),
    }


def build_source_manifest(manifest: dict[str, Any], archive_info: dict[str, Any]) -> dict[str, Any]:
    records = []
    for item in manifest.get("files", []):
        # Skipped records are retained only as aggregate counts.  This avoids
        # publishing a public inventory of files that were never used.
        if item.get("status") != "completed":
            continue
        result_summary = item.get("result_summary") or {}
        records.append(
            {
                "id": item.get("id"),
                "relative_path": item.get("relative_path"),
                "file_name": item.get("file_name"),
                "file_type": item.get("file_type"),
                "category": item.get("category"),
                "is_duplicate": item.get("is_duplicate", False),
                "duplicate_of": item.get("duplicate_of"),
                "hash": item.get("hash"),
                "status": item.get("status"),
                "attempts": item.get("attempts"),
                "pdf_type": item.get("pdf_type"),
                "result_summary": {
                    "content_source": result_summary.get("content_source"),
                    "ocr_status": result_summary.get("ocr_status"),
                },
            }
        )
    records.sort(key=lambda item: item["id"] or "")
    return {
        "format": FORMAT_VERSION,
        "purpose": "历史来源索引；仅用于追溯和审计，不是正式内容源。",
        "archive": archive_info,
        "privacy": {
            "absolute_paths_removed": True,
            "ocr_and_document_bodies_removed": True,
            "skipped_file_names_omitted": True,
        },
        "completed_sources": records,
    }


def build_textbook_structure(
    analysis: list[dict[str, Any]], toc_catalog: dict[str, Any], archive_info: dict[str, Any]
) -> dict[str, Any]:
    records = []
    for item in analysis:
        records.append(
            {
                "source_key": item.get("label"),
                "processed_file": item.get("filename"),
                "pages": item.get("total_pages"),
                "total_chars": item.get("total_chars"),
                "body_chars": item.get("body_chars"),
                "pdf_type": item.get("pdf_type"),
                "mean_ocr_score": item.get("mean_ocr_score"),
                "segmentation": {
                    "granularity": item.get("granularity"),
                    "recommendation": item.get("recommendation"),
                },
                "structure_counts": {
                    "bian": item.get("toc_bian"),
                    "zhang": item.get("toc_zhang"),
                    "jie": item.get("toc_jie"),
                    "body_zhang_matches": item.get("body_zhang_matches"),
                    "body_jie_matches": item.get("body_jie_matches"),
                },
            }
        )
    records.sort(key=lambda item: item["source_key"] or "")

    catalog = []
    for source_key, item in sorted(toc_catalog.items()):
        toc_text = item.get("toc_text") or ""
        catalog.append(
            {
                "source_key": source_key,
                "processed_file": item.get("filename"),
                "copyright_page_count": len(item.get("copyright_pages") or []),
                "toc_line_count": len(toc_text.splitlines()),
                "toc_text_included": False,
            }
        )

    return {
        "format": FORMAT_VERSION,
        "purpose": "教材处理质量与章节切分审计；不发布教材目录或正文。",
        "archive": archive_info,
        "textbooks": records,
        "toc_catalog": catalog,
    }


def build_exam_audit(exam_data: dict[str, Any], archive_info: dict[str, Any]) -> dict[str, Any]:
    questions = exam_data.get("exam_questions", [])
    answer_status = Counter(str(item.get("answer_status") or "MISSING") for item in questions)
    source_status = Counter(str(item.get("content_source") or "MISSING") for item in questions)
    ocr_status = Counter(str(item.get("ocr_status") or "MISSING") for item in questions)
    return {
        "format": FORMAT_VERSION,
        "purpose": "历史真题 OCR 结果的结构审计；不作为正式题库或答案来源。",
        "archive": archive_info,
        "raw_artifact": {
            "name": "output/exam_questions.json",
            "total_questions": exam_data.get("total_questions", len(questions)),
            "year_range": exam_data.get("year_range"),
            "source_file": exam_data.get("file_name"),
        },
        "distribution": {
            "by_year": exam_data.get("year_stats", {}),
            "by_subject": exam_data.get("subject_stats", {}),
            "by_question_type": exam_data.get("type_stats", {}),
            "by_answer_status": dict(sorted(answer_status.items())),
            "by_content_source": dict(sorted(source_status.items())),
            "by_ocr_status": dict(sorted(ocr_status.items())),
        },
        "quality_flags": {
            "missing_score": sum(item.get("score") is None for item in questions),
            "missing_analysis_text": sum(not (item.get("analysis_text") or "").strip() for item in questions),
            "ad_or_watermark_hits": sum(bool(NOISE_RE.search(item.get("analysis_text") or "")) for item in questions),
            "noise_categories": ["联系方式", "商业店铺/广告", "扫描水印"],
        },
        "disposition": "quarantine_until_original_or_user_confirmed_source",
        "text_fields_included": False,
    }


def build_knowledge_audit(knowledge_data: dict[str, Any], archive_info: dict[str, Any]) -> dict[str, Any]:
    points = knowledge_data.get("knowledge_points", [])
    source_counter: Counter[str] = Counter()
    for point in points:
        sources = point.get("textbook_sources") or []
        if not sources:
            source_counter["MISSING"] += 1
        else:
            source_counter.update(str(source) for source in sources)
    return {
        "format": FORMAT_VERSION,
        "purpose": "历史候选知识点的覆盖、冲突和关系审计；不发布知识正文。",
        "archive": archive_info,
        "raw_artifact": "output/cross_validated/cross_validated_knowledge.json",
        "total_unique_knowledge_points": knowledge_data.get("total_unique_knowledge_points", len(points)),
        "multi_source_knowledge_points": knowledge_data.get("multi_source_knowledge_points"),
        "conflict_knowledge_points": knowledge_data.get("conflict_knowledge_points"),
        "subject_stats": knowledge_data.get("subject_stats", {}),
        "source_distribution": dict(sorted(source_counter.items())),
        "relation_count": sum(len(point.get("relations") or []) for point in points),
        "conflict_ids": sorted(
            point.get("id") for point in points if point.get("conflict_flag") and point.get("id")
        ),
        "content_fields_included": False,
    }


def write_json(path: Path, data: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as stream:
        json.dump(data, stream, ensure_ascii=False, indent=2, sort_keys=True)
        stream.write("\n")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("output_dir", type=Path)
    args = parser.parse_args()

    archive_sha256 = sha256_file(args.archive)
    with zipfile.ZipFile(args.archive) as archive:
        manifest = read_json(archive, "manifest.json")
        archive_info = archive_metadata(args.archive, manifest, archive_sha256)
        analysis = read_json(archive, "analysis_final.json")
        toc_catalog = read_json(archive, "toc_extracted.json")
        exam_data = read_json(archive, "output/exam_questions.json")
        knowledge_data = read_json(archive, "output/cross_validated/cross_validated_knowledge.json")

    write_json(args.output_dir / "source-manifest.json", build_source_manifest(manifest, archive_info))
    write_json(
        args.output_dir / "textbook-structure-summary.json",
        build_textbook_structure(analysis, toc_catalog, archive_info),
    )
    write_json(args.output_dir / "exam-question-audit.json", build_exam_audit(exam_data, archive_info))
    write_json(
        args.output_dir / "knowledge-candidate-audit.json",
        build_knowledge_audit(knowledge_data, archive_info),
    )


if __name__ == "__main__":
    main()
