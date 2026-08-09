"""Read-only, deterministic auditing for ``seed_data.json``.

The command intentionally has no network or third-party dependency.  It reads
the seed, a versioned schema, and (optionally) a ratchet baseline.  It never
rewrites the seed and never updates a baseline implicitly.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any, Iterable


AUDIT_TOOL_VERSION = "1.0.0"
REPORT_SCHEMA_VERSION = 1
BASELINE_SCHEMA_VERSION = 1
SCHEMA_ID = "wenyan-seed-legacy-1"

COLLECTIONS = (
    "subjects",
    "knowledge_points",
    "exam_questions",
    "writing_materials",
)
CONTENT_COLLECTIONS = COLLECTIONS[1:]
SUBJECT_NAMES = (
    "中国古代文学",
    "中国现当代文学",
    "外国文学",
    "文学理论",
)

PLACEHOLDER_SOURCES = {
    "",
    "其他",
    "未知",
    "待补",
    "无",
    "n/a",
    "na",
    "none",
    "unknown",
}

OCR_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    ("scanner_watermark", re.compile(r"扫描全能王", re.IGNORECASE)),
    ("wechat", re.compile(r"微信", re.IGNORECASE)),
    ("taobao", re.compile(r"淘宝", re.IGNORECASE)),
    ("official_account", re.compile(r"公众号", re.IGNORECASE)),
    ("shop", re.compile(r"店铺", re.IGNORECASE)),
    ("add_qq", re.compile(r"加\s*Q{2}", re.IGNORECASE)),
    ("wechat_consult", re.compile(r"咨询\s*微信", re.IGNORECASE)),
)

FORBIDDEN_CONTENT_STATUSES = {"AI_DRAFT", "REJECTED", "DRAFT"}
REVIEWED_STATUS = "REVIEWED"
OFFICIAL_ORIGINAL_STATUS = "OFFICIAL_ORIGINAL"
STATUS_FIELDS = ("content_status", "status", "review_status")
SOURCE_STATUS_FIELDS = ("source_status",)
PUBLICATION_FIELDS = ("published",)

# This is the one historical composite question documented by PR-01A.  The
# exact target IDs are still recorded in the generated baseline, so this map
# does not grant an entire question an unlimited cross-subject exception.
DEFAULT_CROSS_SUBJECT_REASONS = {
    "eq_0038": (
        "2007 年 604 卷的同题三选一综合题，现有 seed 关联保留古代、现当代和外国文学方向。"
    )
}


class DuplicateKeyError(ValueError):
    """Raised when a JSON object contains the same key more than once."""


def _reject_json_constant(value: str) -> None:
    raise ValueError(f"non-standard JSON constant: {value}")


def _object_pairs_no_duplicates(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise DuplicateKeyError(f"duplicate object key: {key}")
        result[key] = value
    return result


def load_json_file(path: Path) -> tuple[Any, str]:
    raw = path.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    text = raw.decode("utf-8")
    value = json.loads(
        text,
        object_pairs_hook=_object_pairs_no_duplicates,
        parse_constant=_reject_json_constant,
    )
    return value, digest


def logical_path(path_arg: str) -> str:
    """Return a stable, non-machine-specific path for the report."""

    path = Path(path_arg)
    if path.is_absolute():
        return path.name
    return path.as_posix()


def json_pointer(root: dict[str, Any], pointer: str) -> Any:
    value: Any = root
    for token in pointer.lstrip("#/").split("/"):
        token = token.replace("~1", "/").replace("~0", "~")
        value = value[token]
    return value


def _type_matches(value: Any, expected: str) -> bool:
    if expected == "object":
        return isinstance(value, dict)
    if expected == "array":
        return isinstance(value, list)
    if expected == "string":
        return isinstance(value, str)
    if expected == "integer":
        return isinstance(value, int) and not isinstance(value, bool)
    if expected == "number":
        return isinstance(value, (int, float)) and not isinstance(value, bool)
    if expected == "boolean":
        return isinstance(value, bool)
    if expected == "null":
        return value is None
    return False


def _path_key(path: str, key: str) -> str:
    return f"{path}.{key}" if path != "$" else f"$.{key}"


def _path_index(path: str, index: int) -> str:
    return f"{path}[{index}]"


def validate_against_schema(
    value: Any,
    schema: dict[str, Any],
    *,
    root_schema: dict[str, Any] | None = None,
    path: str = "$",
) -> list[str]:
    """Validate the small JSON-Schema subset used by the committed schema.

    The repository deliberately avoids a runtime dependency on ``jsonschema``.
    The supported keywords are sufficient for this versioned seed contract and
    are tested through both valid and malformed fixtures.
    """

    root_schema = root_schema or schema
    if "$ref" in schema:
        try:
            target = json_pointer(root_schema, schema["$ref"])
        except (KeyError, TypeError) as exc:
            return [f"{path}: unresolved schema reference {schema['$ref']!r}: {exc}"]
        return validate_against_schema(
            value, target, root_schema=root_schema, path=path
        )

    if "oneOf" in schema:
        branch_errors = [
            validate_against_schema(value, branch, root_schema=root_schema, path=path)
            for branch in schema["oneOf"]
        ]
        if sum(not errors for errors in branch_errors) != 1:
            return [f"{path}: value does not match exactly one schema branch"]
        return []

    if "enum" in schema and value not in schema["enum"]:
        return [f"{path}: value {value!r} is not in the allowed enum"]

    expected_type = schema.get("type")
    if expected_type is not None:
        expected_types = (
            expected_type if isinstance(expected_type, list) else [expected_type]
        )
        if not any(_type_matches(value, item) for item in expected_types):
            return [f"{path}: expected {expected_types}, got {type(value).__name__}"]

    errors: list[str] = []
    if isinstance(value, dict) and "object" in (
        expected_type if isinstance(expected_type, list) else [expected_type]
    ):
        required = schema.get("required", [])
        for key in required:
            if key not in value:
                errors.append(f"{path}: missing required property {key!r}")

        properties = schema.get("properties", {})
        additional = schema.get("additionalProperties", True)
        for key in sorted(value):
            child_path = _path_key(path, key)
            if key in properties:
                errors.extend(
                    validate_against_schema(
                        value[key],
                        properties[key],
                        root_schema=root_schema,
                        path=child_path,
                    )
                )
            elif additional is False:
                errors.append(f"{child_path}: additional property is not allowed")
            elif isinstance(additional, dict):
                errors.extend(
                    validate_against_schema(
                        value[key],
                        additional,
                        root_schema=root_schema,
                        path=child_path,
                    )
                )

    if isinstance(value, list) and "array" in (
        expected_type if isinstance(expected_type, list) else [expected_type]
    ):
        if "minItems" in schema and len(value) < schema["minItems"]:
            errors.append(f"{path}: expected at least {schema['minItems']} items")
        if "items" in schema:
            for index, item in enumerate(value):
                errors.extend(
                    validate_against_schema(
                        item,
                        schema["items"],
                        root_schema=root_schema,
                        path=_path_index(path, index),
                    )
                )

    if isinstance(value, str):
        if "minLength" in schema and len(value) < schema["minLength"]:
            errors.append(f"{path}: string is shorter than {schema['minLength']}")
        if "pattern" in schema and re.fullmatch(schema["pattern"], value) is None:
            errors.append(f"{path}: value does not match required pattern")

    if isinstance(value, (int, float)) and not isinstance(value, bool):
        if "minimum" in schema and value < schema["minimum"]:
            errors.append(f"{path}: value is below minimum {schema['minimum']}")
        if "maximum" in schema and value > schema["maximum"]:
            errors.append(f"{path}: value is above maximum {schema['maximum']}")

    return errors


def normalise_source(value: Any) -> str:
    if not isinstance(value, str):
        return ""
    return value.strip().casefold()


def is_valid_source(value: Any) -> bool:
    return isinstance(value, str) and normalise_source(value) not in PLACEHOLDER_SOURCES


def source_values(record: dict[str, Any], collection: str) -> list[str]:
    values: list[str] = []
    if collection == "knowledge_points":
        candidates = [record.get("source_ref"), record.get("source")]
        textbook_sources = record.get("textbook_sources", [])
        if isinstance(textbook_sources, list):
            candidates.extend(textbook_sources)
    else:
        candidates = [
            record.get("source"),
            record.get("source_ref"),
            record.get("exam_source"),
        ]
    for candidate in candidates:
        if isinstance(candidate, str):
            values.append(candidate)
    return values


def has_valid_source(record: dict[str, Any], collection: str) -> bool:
    return any(is_valid_source(value) for value in source_values(record, collection))


def has_source_evidence(record: dict[str, Any]) -> bool:
    for key in ("source", "source_ref", "exam_source", "source_evidence"):
        value = record.get(key)
        if isinstance(value, str) and is_valid_source(value):
            return True
        if isinstance(value, (dict, list)) and bool(value):
            return True
    return False


def normalise_status(value: Any) -> str:
    if not isinstance(value, str):
        return ""
    return re.sub(r"[\s-]+", "_", value.strip().upper())


def first_status(record: dict[str, Any]) -> tuple[str | None, str]:
    for key in STATUS_FIELDS:
        if key in record:
            return key, normalise_status(record[key])
    return None, ""


def issue_identity(issue: dict[str, Any]) -> str:
    details = issue.get("details") or {}
    parts = [
        str(issue.get("code", "")),
        str(issue.get("collection", "")),
        str(issue.get("id") or ""),
        str(issue.get("field") or ""),
    ]
    for key in ("target_id", "pattern"):
        if key in details:
            parts.append(f"{key}={details[key]}")
    return "|".join(parts)


def _issue(
    code: str,
    severity: str,
    *,
    collection: str = "",
    record_id: str | None = None,
    field: str = "",
    details: dict[str, Any] | None = None,
    baseline_track: bool = True,
) -> dict[str, Any]:
    value = {
        "code": code,
        "severity": severity,
        "collection": collection,
        "id": record_id,
        "field": field,
        "details": details or {},
    }
    if not baseline_track:
        value["baseline_track"] = False
    return value


def _sorted_issues(issues: Iterable[dict[str, Any]]) -> list[dict[str, Any]]:
    return sorted(
        issues,
        key=lambda item: (
            item.get("code", ""),
            item.get("collection", ""),
            item.get("id") or "",
            item.get("field", ""),
            json.dumps(item.get("details", {}), ensure_ascii=False, sort_keys=True),
        ),
    )


def _distribution(values: Iterable[Any]) -> dict[str, int]:
    counts = Counter(str(value) for value in values)
    return {key: counts[key] for key in sorted(counts)}


def _score_bucket(score: int) -> str:
    if score == 0:
        return "0"
    if score <= 10:
        return "1-10"
    if score <= 20:
        return "11-20"
    if score <= 30:
        return "21-30"
    if score <= 50:
        return "31-50"
    return "51+"


def _manifest_digest(ids: list[str]) -> str:
    return hashlib.sha256(("\n".join(ids) + "\n").encode("utf-8")).hexdigest()


def _walk_strings(value: Any, path: str) -> Iterable[tuple[str, str]]:
    if isinstance(value, str):
        yield path, value
    elif isinstance(value, list):
        for index, item in enumerate(value):
            yield from _walk_strings(item, f"{path}[{index}]")
    elif isinstance(value, dict):
        for key in sorted(value):
            yield from _walk_strings(value[key], _path_key(path, key))


def _load_baseline(path: Path) -> tuple[dict[str, Any] | None, str | None]:
    try:
        baseline, _ = load_json_file(path)
    except (OSError, UnicodeError, ValueError) as exc:
        return None, str(exc)
    if not isinstance(baseline, dict):
        return None, "baseline root must be an object"
    required = (
        "baseline_schema_version",
        "schema_id",
        "schema_version",
        "seed_version",
        "seed_sha256",
        "id_manifest",
        "debts",
        "metrics",
        "cross_subject_allowlist",
    )
    missing = [key for key in required if key not in baseline]
    if missing:
        return None, f"baseline missing required properties: {', '.join(missing)}"
    if baseline.get("baseline_schema_version") != BASELINE_SCHEMA_VERSION:
        return None, "unsupported baseline_schema_version"
    for key in ("schema_id", "schema_version", "seed_version", "seed_sha256"):
        if not isinstance(baseline.get(key), str) or not baseline[key]:
            return None, f"baseline {key} must be a non-empty string"
    if re.fullmatch(r"[0-9a-f]{64}", baseline["seed_sha256"]) is None:
        return None, "baseline seed_sha256 must be a lowercase SHA-256 digest"
    if not isinstance(baseline.get("id_manifest"), dict):
        return None, "baseline id_manifest must be an object"
    for collection in COLLECTIONS:
        ids = baseline["id_manifest"].get(collection)
        if not isinstance(ids, list) or any(not isinstance(item, str) for item in ids):
            return None, f"baseline id_manifest.{collection} must be a string array"
        if ids != sorted(set(ids)):
            return None, f"baseline id_manifest.{collection} must be sorted and unique"
    if not isinstance(baseline.get("debts"), list):
        return None, "baseline debts must be an array"
    debt_identities: set[str] = set()
    debt_required = ("identity", "code", "collection", "id", "field", "rule_version")
    for index, entry in enumerate(baseline["debts"]):
        if not isinstance(entry, dict):
            return None, f"baseline debts[{index}] must be an object"
        if any(key not in entry for key in debt_required):
            return None, f"baseline debts[{index}] is missing a required property"
        if not all(isinstance(entry[key], str) for key in ("identity", "code", "collection", "field")):
            return None, f"baseline debts[{index}] has invalid identity fields"
        if entry["collection"] not in CONTENT_COLLECTIONS and entry["collection"] != "":
            return None, f"baseline debts[{index}] has an invalid collection"
        if entry["id"] is not None and not isinstance(entry["id"], str):
            return None, f"baseline debts[{index}].id must be a string or null"
        if entry["rule_version"] != 1:
            return None, f"baseline debts[{index}] has an unsupported rule_version"
        expected_identity = issue_identity(entry)
        if entry["identity"] != expected_identity:
            return None, f"baseline debts[{index}] identity does not match its fields"
        if entry["identity"] in debt_identities:
            return None, f"baseline debts contains duplicate identity {entry['identity']!r}"
        debt_identities.add(entry["identity"])
    if not isinstance(baseline.get("metrics"), dict):
        return None, "baseline metrics must be an object"
    for metric_name, metric in baseline["metrics"].items():
        if not isinstance(metric, dict):
            return None, f"baseline metric {metric_name!r} must be an object"
        if not isinstance(metric.get("value"), (int, float)) or isinstance(
            metric.get("value"), bool
        ):
            return None, f"baseline metric {metric_name!r} has an invalid value"
        if metric.get("ratchet") not in {"min", "max"}:
            return None, f"baseline metric {metric_name!r} has an invalid ratchet"
    if not isinstance(baseline.get("cross_subject_allowlist"), list):
        return None, "baseline cross_subject_allowlist must be an array"
    allowlist_pairs: set[tuple[str, str]] = set()
    for index, entry in enumerate(baseline["cross_subject_allowlist"]):
        if not isinstance(entry, dict):
            return None, f"baseline cross_subject_allowlist[{index}] must be an object"
        if not all(isinstance(entry.get(key), str) and entry[key] for key in ("question_id", "point_id", "reason")):
            return None, f"baseline cross_subject_allowlist[{index}] has invalid fields"
        pair = (entry["question_id"], entry["point_id"])
        if pair in allowlist_pairs:
            return None, f"baseline cross_subject_allowlist contains duplicate pair {pair!r}"
        allowlist_pairs.add(pair)
    return baseline, None


def _baseline_debt_identities(baseline: dict[str, Any]) -> set[str]:
    identities: set[str] = set()
    for entry in baseline.get("debts", []):
        if isinstance(entry, dict) and isinstance(entry.get("identity"), str):
            identities.add(entry["identity"])
        elif isinstance(entry, dict):
            identities.add(issue_identity(entry))
    return identities


def _allowlist_index(baseline: dict[str, Any] | None) -> dict[tuple[str, str], str]:
    if not baseline:
        return {}
    result: dict[tuple[str, str], str] = {}
    for entry in baseline.get("cross_subject_allowlist", []):
        if not isinstance(entry, dict):
            continue
        qid = entry.get("question_id")
        pid = entry.get("point_id")
        reason = entry.get("reason")
        if isinstance(qid, str) and isinstance(pid, str) and isinstance(reason, str):
            result[(qid, pid)] = reason
    return result


def _status_missing_issues(
    collection: str, records: list[dict[str, Any]]
) -> list[dict[str, Any]]:
    if not records:
        return []
    expected = (
        ("content_status", STATUS_FIELDS),
        ("source_status", SOURCE_STATUS_FIELDS),
        ("published", PUBLICATION_FIELDS),
    )
    issues: list[dict[str, Any]] = []
    for label, fields in expected:
        if not any(any(field in record for field in fields) for record in records):
            issues.append(
                _issue(
                    "LEGACY_STATUS_FIELD_MISSING",
                    "debt",
                    collection=collection,
                    field=label,
                    details={"record_count": len(records)},
                )
            )
    return issues


def _build_summary(
    data: dict[str, Any], ids_by_collection: dict[str, list[str]]
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    subjects = data["subjects"]
    knowledge_points = data["knowledge_points"]
    exam_questions = data["exam_questions"]
    writing_materials = data["writing_materials"]

    kp_source_valid = [has_valid_source(item, "knowledge_points") for item in knowledge_points]
    kp_high = [item for item in knowledge_points if item.get("exam_frequency") == "HIGH"]
    kp_high_source_valid = [has_valid_source(item, "knowledge_points") for item in kp_high]
    essay_questions = [item for item in exam_questions if item.get("question_type") == "ESSAY"]
    related_questions = [
        item for item in exam_questions if isinstance(item.get("related_point_ids"), list)
    ]
    scores = [item["score"] for item in exam_questions]

    counts = {
        "subjects": len(subjects),
        "knowledge_points": len(knowledge_points),
        "exam_questions": len(exam_questions),
        "writing_materials": len(writing_materials),
    }
    coverage = {
        "knowledge_points": {
            "source_valid": sum(kp_source_valid),
            "source_missing_or_placeholder": len(kp_source_valid) - sum(kp_source_valid),
            "high_frequency_total": len(kp_high),
            "high_frequency_source_valid": sum(kp_high_source_valid),
            "high_frequency_source_missing_or_placeholder": len(kp_high_source_valid)
            - sum(kp_high_source_valid),
            "by_subject": _distribution(item["subject"] for item in knowledge_points),
            "by_exam_frequency": _distribution(
                item["exam_frequency"] for item in knowledge_points
            ),
        },
        "exam_questions": {
            "answer_framework_present": sum(
                isinstance(item.get("answer_framework"), str)
                and bool(item["answer_framework"].strip())
                for item in exam_questions
            ),
            "angle_present": sum(
                isinstance(item.get("angle"), str) and bool(item["angle"].strip())
                for item in essay_questions
            ),
            "notes_present": sum(
                isinstance(item.get("notes"), str) and bool(item["notes"].strip())
                for item in essay_questions
            ),
            "essay_total": len(essay_questions),
            "explicit_related_question_count": len(related_questions),
            "explicit_related_reference_count": sum(
                len(item["related_point_ids"]) for item in related_questions
            ),
        },
        "writing_materials": {
            "source_valid": sum(has_valid_source(item, "writing_materials") for item in writing_materials),
            "source_missing_or_placeholder": sum(
                not has_valid_source(item, "writing_materials") for item in writing_materials
            ),
        },
        "status_fields": {
            collection: {
                "content_status_present": sum(
                    any(field in item for field in STATUS_FIELDS) for item in data[collection]
                ),
                "source_status_present": sum(
                    any(field in item for field in SOURCE_STATUS_FIELDS)
                    for item in data[collection]
                ),
                "published_present": sum(
                    any(field in item for field in PUBLICATION_FIELDS)
                    for item in data[collection]
                ),
            }
            for collection in CONTENT_COLLECTIONS
        },
    }
    distributions = {
        "exam_questions_by_year": _distribution(item["year"] for item in exam_questions),
        "exam_questions_by_type": _distribution(
            item["question_type"] for item in exam_questions
        ),
        "exam_questions_by_score": _distribution(item["score"] for item in exam_questions),
        "exam_questions_by_score_bucket": _distribution(
            _score_bucket(item["score"]) for item in exam_questions
        ),
        "exam_questions_by_exam_paper_code": _distribution(
            item["exam_paper_code"] for item in exam_questions
        ),
        "writing_materials_by_category": _distribution(
            item["category"] for item in writing_materials
        ),
        "writing_materials_by_sub_category": _distribution(
            item["sub_category"] for item in writing_materials
        ),
        "writing_materials_by_source": _distribution(
            item["source"] for item in writing_materials
        ),
    }
    status_distributions = {
        collection: {
            field: _distribution(
                item[field]
                for item in data[collection]
                if field in item
            )
            for field in (*STATUS_FIELDS, *SOURCE_STATUS_FIELDS, *PUBLICATION_FIELDS)
        }
        for collection in CONTENT_COLLECTIONS
    }
    distributions["status_values"] = status_distributions

    id_manifest_summary = {
        collection: {
            "count": len(ids),
            "sha256": _manifest_digest(ids),
        }
        for collection, ids in ids_by_collection.items()
    }
    metrics = {
        "knowledge_points.source_valid_count": {
            "value": coverage["knowledge_points"]["source_valid"],
            "ratchet": "min",
        },
        "knowledge_points.high_frequency_source_valid_count": {
            "value": coverage["knowledge_points"]["high_frequency_source_valid"],
            "ratchet": "min",
        },
        "exam_questions.answer_framework_present": {
            "value": coverage["exam_questions"]["answer_framework_present"],
            "ratchet": "min",
        },
        "exam_questions.essay_angle_present": {
            "value": coverage["exam_questions"]["angle_present"],
            "ratchet": "min",
        },
        "exam_questions.essay_notes_present": {
            "value": coverage["exam_questions"]["notes_present"],
            "ratchet": "min",
        },
        "writing_materials.source_valid_count": {
            "value": coverage["writing_materials"]["source_valid"],
            "ratchet": "min",
        },
    }
    return counts, coverage | {"status_distributions": status_distributions}, {
        "distributions": distributions,
        "id_manifest_summary": id_manifest_summary,
        "metrics": metrics,
    }


def audit_data(
    data: Any,
    *,
    seed_sha256: str | None,
    seed_path: str,
    schema: dict[str, Any],
    schema_path: str,
    baseline: dict[str, Any] | None = None,
    baseline_path: str | None = None,
    baseline_error: str | None = None,
    as_of_year: int | None = None,
    for_baseline: bool = False,
) -> tuple[dict[str, Any], dict[str, Any]]:
    issues: list[dict[str, Any]] = []
    baseline_known_debts = _baseline_debt_identities(baseline) if baseline else set()
    schema_errors = validate_against_schema(data, schema)
    schema_valid = not schema_errors
    if schema_errors:
        for error in schema_errors:
            issues.append(
                _issue(
                    "SCHEMA_INVALID",
                    "error",
                    field=error.split(":", 1)[0],
                    details={"message": error},
                )
            )

    ids_by_collection: dict[str, list[str]] = {collection: [] for collection in COLLECTIONS}
    cross_subject_candidates: list[dict[str, str]] = []
    if schema_valid and isinstance(data, dict):
        for collection in COLLECTIONS:
            records = data[collection]
            ids = [record["id"] for record in records]
            ids_by_collection[collection] = sorted(ids)
            positions: dict[str, list[int]] = {}
            for index, record_id in enumerate(ids):
                positions.setdefault(record_id, []).append(index)
            for record_id, indexes in sorted(positions.items()):
                if len(indexes) > 1:
                    issues.append(
                        _issue(
                            "DUPLICATE_ID",
                            "error",
                            collection=collection,
                            record_id=record_id,
                            field="id",
                            details={"occurrences": indexes},
                        )
                    )

        seen_ids: dict[str, list[str]] = {}
        for collection, ids in ids_by_collection.items():
            for record_id in ids:
                seen_ids.setdefault(record_id, []).append(collection)
        for record_id, collections in sorted(seen_ids.items()):
            if len(collections) > 1:
                issues.append(
                    _issue(
                        "CROSS_COLLECTION_ID_COLLISION",
                        "error",
                        record_id=record_id,
                        field="id",
                        details={"collections": collections},
                    )
                )

        counts, coverage, summary = _build_summary(data, ids_by_collection)
        kp_ids = set(ids_by_collection["knowledge_points"])
        subject_by_kp = {
            item["id"]: item["subject"] for item in data["knowledge_points"]
        }

        for collection in CONTENT_COLLECTIONS:
            issues.extend(_status_missing_issues(collection, data[collection]))
            for record in data[collection]:
                record_id = record["id"]
                status_keys = (*STATUS_FIELDS, *SOURCE_STATUS_FIELDS)
                for status_field in status_keys:
                    if status_field not in record:
                        continue
                    status = normalise_status(record[status_field])
                    if status in FORBIDDEN_CONTENT_STATUSES:
                        issues.append(
                            _issue(
                                "FORBIDDEN_PUBLISHED_STATUS",
                                "error",
                                collection=collection,
                                record_id=record_id,
                                field=status_field,
                                details={"status": status},
                            )
                        )

                status_field, status = first_status(record)
                if status == REVIEWED_STATUS and not has_valid_source(record, collection):
                    issues.append(
                        _issue(
                            "REVIEWED_WITHOUT_SOURCE",
                            "error",
                            collection=collection,
                            record_id=record_id,
                            field=status_field or "content_status",
                            details={"reason": "REVIEWED requires a non-placeholder source"},
                        )
                    )
                if (
                    collection == "exam_questions"
                    and normalise_status(record.get("source_status"))
                    == OFFICIAL_ORIGINAL_STATUS
                    and not has_source_evidence(record)
                ):
                    issues.append(
                        _issue(
                            "OFFICIAL_ORIGINAL_WITHOUT_EVIDENCE",
                            "error",
                            collection=collection,
                            record_id=record_id,
                            field="source_status",
                            details={"status": OFFICIAL_ORIGINAL_STATUS},
                        )
                    )

        for record in data["knowledge_points"]:
            if not has_valid_source(record, "knowledge_points"):
                issues.append(
                    _issue(
                        "KNOWLEDGE_SOURCE_MISSING",
                        "debt",
                        collection="knowledge_points",
                        record_id=record["id"],
                        field="textbook_sources",
                        details={"reason": "missing or placeholder-only source"},
                    )
                )
            if record["exam_frequency"] == "HIGH" and not has_valid_source(
                record, "knowledge_points"
            ):
                issues.append(
                    _issue(
                        "HIGH_FREQUENCY_SOURCE_MISSING",
                        "debt",
                        collection="knowledge_points",
                        record_id=record["id"],
                        field="textbook_sources",
                        details={"reason": "high-frequency point lacks a non-placeholder source"},
                    )
                )

        for record in data["exam_questions"]:
            if record["score"] == 0:
                issues.append(
                    _issue(
                        "EXAM_SCORE_ZERO",
                        "debt",
                        collection="exam_questions",
                        record_id=record["id"],
                        field="score",
                        details={"reason": "legacy seed score is zero"},
                    )
                )
            if record["question_type"] == "ESSAY":
                for field in ("angle", "notes"):
                    value = record.get(field)
                    if not isinstance(value, str) or not value.strip():
                        issues.append(
                            _issue(
                                f"ESSAY_{field.upper()}_MISSING",
                                "debt",
                                collection="exam_questions",
                                record_id=record["id"],
                                field=field,
                                details={"reason": f"legacy ESSAY record lacks {field}"},
                            )
                        )

            related_ids = record.get("related_point_ids")
            if not isinstance(related_ids, list):
                continue
            for target_id in related_ids:
                if target_id not in kp_ids:
                    issues.append(
                        _issue(
                            "DANGLING_RELATED_POINT_ID",
                            "error",
                            collection="exam_questions",
                            record_id=record["id"],
                            field="related_point_ids",
                            details={"target_id": target_id},
                        )
                    )
                    continue
                question_subject = record["subject"]
                point_subject = subject_by_kp[target_id]
                if question_subject == point_subject:
                    continue
                cross_subject_candidates.append(
                    {
                        "question_id": record["id"],
                        "point_id": target_id,
                        "question_subject": question_subject,
                        "point_subject": point_subject,
                    }
                )

        for record in data["writing_materials"]:
            if not has_valid_source(record, "writing_materials"):
                issues.append(
                    _issue(
                        "WRITING_SOURCE_PLACEHOLDER",
                        "debt",
                        collection="writing_materials",
                        record_id=record["id"],
                        field="source",
                        details={"reason": "missing or placeholder-only source"},
                    )
                )

        for collection in CONTENT_COLLECTIONS:
            for record in data[collection]:
                record_id = record["id"]
                for field_path, text_value in _walk_strings(record, "$record"):
                    for pattern_name, pattern in OCR_PATTERNS:
                        if pattern.search(text_value):
                            noise_issue = _issue(
                                "OCR_NOISE",
                                "debt",
                                collection=collection,
                                record_id=record_id,
                                field=field_path.removeprefix("$record.").removeprefix(
                                    "$record"
                                ),
                                details={"pattern": pattern_name},
                            )
                            # A historical OCR marker may be explicitly
                            # acknowledged in the baseline.  During the
                            # one-time --write-baseline operation it is
                            # allowed to become such a debt; ordinary audits
                            # still fail on any unrecorded occurrence.
                            if not for_baseline and issue_identity(noise_issue) not in baseline_known_debts:
                                noise_issue["severity"] = "error"
                            issues.append(noise_issue)

        allowlist = _allowlist_index(baseline)
        for candidate in cross_subject_candidates:
            key = (candidate["question_id"], candidate["point_id"])
            reason = allowlist.get(key)
            if reason is None:
                reason = DEFAULT_CROSS_SUBJECT_REASONS.get(candidate["question_id"])
                # A default reason is only informational until an exact pair is
                # committed to the baseline allowlist.
            # During the one-time, explicit baseline creation run there is no
            # committed allowlist yet.  The PR-01A contract documents eq_0038
            # as the existing composite question, so its current exact pairs
            # are provisional until make_baseline() records them.  Once a
            # baseline is present, only the exact question/target pair is
            # allowed; a newly added target cannot inherit the exception.
            allowed = key in allowlist or (
                baseline is None
                and candidate["question_id"] in DEFAULT_CROSS_SUBJECT_REASONS
            )
            issues.append(
                _issue(
                    "CROSS_SUBJECT_RELATION",
                    "info" if allowed else "error",
                    collection="exam_questions",
                    record_id=candidate["question_id"],
                    field="related_point_ids",
                    details={
                        "target_id": candidate["point_id"],
                        "question_subject": candidate["question_subject"],
                        "point_subject": candidate["point_subject"],
                        "allowlisted": allowed,
                        "reason": reason or "no approved allowlist reason",
                    },
                    baseline_track=False if allowed else True,
                )
            )
    else:
        counts = {collection: 0 for collection in COLLECTIONS}
        coverage = {}
        summary = {
            "distributions": {},
            "id_manifest_summary": {
                collection: {"count": 0, "sha256": _manifest_digest([])}
                for collection in COLLECTIONS
            },
            "metrics": {},
        }

    if baseline_error:
        issues.append(
            _issue(
                "BASELINE_INVALID",
                "error",
                field="baseline",
                details={"message": baseline_error},
            )
        )

    if baseline is not None:
        if baseline.get("schema_id") != schema.get("$id"):
            issues.append(
                _issue(
                    "BASELINE_SCHEMA_MISMATCH",
                    "error",
                    field="schema_id",
                    details={
                        "expected": schema.get("$id"),
                        "actual": baseline.get("schema_id"),
                    },
                )
            )
        content_version = (
            data.get("metadata", {}).get("version")
            if isinstance(data, dict) and isinstance(data.get("metadata"), dict)
            else None
        )
        if baseline.get("schema_version") != schema.get("schema_version"):
            issues.append(
                _issue(
                    "BASELINE_SCHEMA_VERSION_MISMATCH",
                    "error",
                    field="schema_version",
                    details={
                        "expected": schema.get("schema_version"),
                        "actual": baseline.get("schema_version"),
                    },
                )
            )
        if baseline.get("seed_version") != content_version:
            issues.append(
                _issue(
                    "BASELINE_SEED_VERSION_MISMATCH",
                    "error",
                    field="seed_version",
                    details={"expected": content_version, "actual": baseline.get("seed_version")},
                )
            )
        if seed_sha256 is not None and baseline.get("seed_sha256") != seed_sha256:
            issues.append(
                _issue(
                    "BASELINE_SEED_SHA256_MISMATCH",
                    "error",
                    field="seed_sha256",
                    details={"expected": seed_sha256, "actual": baseline.get("seed_sha256")},
                )
            )
        for collection in COLLECTIONS:
            old_ids = set(baseline.get("id_manifest", {}).get(collection, []))
            current_ids = set(ids_by_collection.get(collection, []))
            for deleted_id in sorted(old_ids - current_ids):
                issues.append(
                    _issue(
                        "DELETED_ID",
                        "error",
                        collection=collection,
                        record_id=deleted_id,
                        field="id",
                        details={"reason": "baseline ID is absent from current seed"},
                    )
                )

        current_metrics = summary["metrics"]
        for metric_name, baseline_metric in sorted(baseline.get("metrics", {}).items()):
            if not isinstance(baseline_metric, dict):
                issues.append(
                    _issue(
                        "BASELINE_INVALID",
                        "error",
                        field=f"metrics.{metric_name}",
                        details={"message": "metric entry must be an object"},
                    )
                )
                continue
            current_metric = current_metrics.get(metric_name)
            if not isinstance(current_metric, dict):
                issues.append(
                    _issue(
                        "RATCHET_METRIC_MISSING",
                        "error",
                        field=metric_name,
                        details={"baseline": baseline_metric},
                    )
                )
                continue
            old_value = baseline_metric.get("value")
            new_value = current_metric.get("value")
            ratchet = baseline_metric.get("ratchet")
            worsened = (
                isinstance(old_value, (int, float))
                and isinstance(new_value, (int, float))
                and (
                    (ratchet == "min" and new_value < old_value)
                    or (ratchet == "max" and new_value > old_value)
                )
            )
            if worsened:
                issues.append(
                    _issue(
                        "RATCHET_METRIC_WORSENED",
                        "error",
                        field=metric_name,
                        details={
                            "baseline_value": old_value,
                            "current_value": new_value,
                            "ratchet": ratchet,
                        },
                    )
                )

        known_debts = _baseline_debt_identities(baseline)
        current_debts = {
            issue_identity(item): item
            for item in issues
            if item.get("severity") == "debt" and item.get("baseline_track", True)
        }
        for identity, item in sorted(current_debts.items()):
            if identity not in known_debts:
                issues.append(
                    _issue(
                        "NEW_DEBT",
                        "error",
                        collection=item.get("collection", ""),
                        record_id=item.get("id"),
                        field=item.get("field", ""),
                        details={"identity": identity, "source_code": item.get("code")},
                    )
                )
        resolved_debt = len(known_debts - set(current_debts))
    else:
        resolved_debt = 0

    current_issues = _sorted_issues(issues)
    error_count = sum(item["severity"] == "error" for item in current_issues)
    debt_count = sum(item["severity"] == "debt" for item in current_issues)
    new_debt = sum(item["code"] == "NEW_DEBT" for item in current_issues)
    result = {
        "passed": error_count == 0,
        "error_count": error_count,
        "debt_count": debt_count,
        "new_debt": new_debt,
        "resolved_debt": resolved_debt,
        "baseline_present": baseline is not None,
    }
    metadata = data.get("metadata", {}) if isinstance(data, dict) else {}
    report = {
        "report_schema_version": REPORT_SCHEMA_VERSION,
        "audit_tool_version": AUDIT_TOOL_VERSION,
        "schema_id": schema.get("$id"),
        "input": {
            "seed_path": seed_path,
            "seed_sha256": seed_sha256,
            "content_version": metadata.get("version") if isinstance(metadata, dict) else None,
            "schema_path": schema_path,
            **({"baseline_path": baseline_path} if baseline_path else {}),
            **({"as_of_year": as_of_year} if as_of_year is not None else {}),
        },
        "counts": counts,
        "coverage": coverage,
        "distributions": summary["distributions"],
        "id_manifest_summary": summary["id_manifest_summary"],
        "ratchet_metrics": summary["metrics"],
        "violations": current_issues,
        "result": result,
    }
    state = {
        "id_manifest": ids_by_collection,
        "cross_subject_candidates": cross_subject_candidates,
        "schema_valid": schema_valid,
        "seed_sha256": seed_sha256,
        "content_version": metadata.get("version") if isinstance(metadata, dict) else None,
    }
    return report, state


def make_baseline(
    report: dict[str, Any], state: dict[str, Any], schema: dict[str, Any]
) -> dict[str, Any]:
    errors = [item for item in report["violations"] if item.get("severity") == "error"]
    if errors:
        raise ValueError("cannot write a baseline while audit errors are present")
    allowlist: list[dict[str, str]] = []
    for candidate in state["cross_subject_candidates"]:
        question_id = candidate["question_id"]
        reason = DEFAULT_CROSS_SUBJECT_REASONS.get(question_id)
        if reason is None:
            raise ValueError(
                f"cross-subject relation {question_id}->{candidate['point_id']} needs an explicit reason"
            )
        allowlist.append(
            {
                "question_id": question_id,
                "point_id": candidate["point_id"],
                "reason": reason,
            }
        )
    allowlist.sort(key=lambda item: (item["question_id"], item["point_id"]))

    debts: list[dict[str, Any]] = []
    for item in report["violations"]:
        if item.get("severity") != "debt" or item.get("baseline_track") is False:
            continue
        debts.append(
            {
                "identity": issue_identity(item),
                "code": item["code"],
                "collection": item.get("collection", ""),
                "id": item.get("id"),
                "field": item.get("field", ""),
                "rule_version": 1,
            }
        )
    debts.sort(key=lambda item: item["identity"])
    return {
        "baseline_schema_version": BASELINE_SCHEMA_VERSION,
        "report_schema_version": REPORT_SCHEMA_VERSION,
        "audit_tool_version": AUDIT_TOOL_VERSION,
        "schema_id": schema.get("$id"),
        "schema_version": schema.get("schema_version"),
        "seed_version": state.get("content_version"),
        "seed_sha256": state.get("seed_sha256"),
        "id_manifest": {
            collection: sorted(ids)
            for collection, ids in state["id_manifest"].items()
        },
        "debts": debts,
        "metrics": report["ratchet_metrics"],
        "cross_subject_allowlist": allowlist,
    }


def _load_schema(path: Path) -> tuple[dict[str, Any] | None, str | None]:
    try:
        schema, _ = load_json_file(path)
    except (OSError, UnicodeError, ValueError) as exc:
        return None, str(exc)
    if not isinstance(schema, dict):
        return None, "schema root must be an object"
    if schema.get("$id") != SCHEMA_ID:
        return None, f"schema $id must be {SCHEMA_ID!r}"
    if schema.get("schema_version") != SCHEMA_ID:
        return None, f"schema_version must be {SCHEMA_ID!r}"
    return schema, None


def _empty_report(
    *,
    seed_path: str,
    seed_sha256: str | None,
    schema_path: str,
    message: str,
    code: str,
    baseline_path: str | None,
) -> dict[str, Any]:
    violation = _issue(code, "error", field="input", details={"message": message})
    return {
        "report_schema_version": REPORT_SCHEMA_VERSION,
        "audit_tool_version": AUDIT_TOOL_VERSION,
        "schema_id": None,
        "input": {
            "seed_path": seed_path,
            "seed_sha256": seed_sha256,
            "content_version": None,
            "schema_path": schema_path,
            **({"baseline_path": baseline_path} if baseline_path else {}),
        },
        "counts": {collection: 0 for collection in COLLECTIONS},
        "coverage": {},
        "distributions": {},
        "id_manifest_summary": {},
        "ratchet_metrics": {},
        "violations": [violation],
        "result": {
            "passed": False,
            "error_count": 1,
            "debt_count": 0,
            "new_debt": 0,
            "resolved_debt": 0,
            "baseline_present": False,
        },
    }


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--seed", required=True, help="read-only seed JSON path")
    parser.add_argument("--schema", required=True, help="versioned schema JSON path")
    parser.add_argument("--baseline", help="ratchet baseline JSON path")
    parser.add_argument("--report", help="deterministic JSON report output path")
    parser.add_argument(
        "--write-baseline",
        metavar="PATH",
        help="explicitly write a new baseline from the current audit",
    )
    parser.add_argument("--as-of-year", type=int, help="fixed year context included in the report")
    parser.add_argument("--check", action="store_true", help="return a failure exit code when audit fails")
    parser.add_argument("--format", choices=("json",), default="json")
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    seed_path = Path(args.seed)
    schema_path = Path(args.schema)
    baseline_path = Path(args.baseline) if args.baseline else None
    report_path = Path(args.report) if args.report else None

    seed_sha256: str | None = None
    try:
        raw_seed = seed_path.read_bytes()
        seed_sha256 = hashlib.sha256(raw_seed).hexdigest()
        seed_data = json.loads(
            raw_seed.decode("utf-8"),
            object_pairs_hook=_object_pairs_no_duplicates,
            parse_constant=_reject_json_constant,
        )
    except (OSError, UnicodeError, ValueError) as exc:
        report = _empty_report(
            seed_path=logical_path(args.seed),
            seed_sha256=seed_sha256,
            schema_path=logical_path(args.schema),
            message=str(exc),
            code="SEED_INVALID_JSON",
            baseline_path=logical_path(args.baseline) if args.baseline else None,
        )
        if report_path:
            write_json(report_path, report)
        print(f"FAIL: {report['violations'][0]['details']['message']}")
        return 1

    schema, schema_error = _load_schema(schema_path)
    if schema_error or schema is None:
        report = _empty_report(
            seed_path=logical_path(args.seed),
            seed_sha256=seed_sha256,
            schema_path=logical_path(args.schema),
            message=schema_error or "schema could not be loaded",
            code="SCHEMA_FILE_INVALID",
            baseline_path=logical_path(args.baseline) if args.baseline else None,
        )
        if report_path:
            write_json(report_path, report)
        print(f"FAIL: {report['violations'][0]['details']['message']}")
        return 1

    baseline: dict[str, Any] | None = None
    baseline_error: str | None = None
    if baseline_path:
        baseline, baseline_error = _load_baseline(baseline_path)

    report, state = audit_data(
        seed_data,
        seed_sha256=seed_sha256,
        seed_path=logical_path(args.seed),
        schema=schema,
        schema_path=logical_path(args.schema),
        baseline=baseline,
        baseline_path=logical_path(args.baseline) if args.baseline else None,
        baseline_error=baseline_error,
        as_of_year=args.as_of_year,
        for_baseline=bool(args.write_baseline),
    )

    if args.write_baseline:
        try:
            baseline_value = make_baseline(report, state, schema)
            write_json(Path(args.write_baseline), baseline_value)
        except (OSError, ValueError) as exc:
            report["violations"] = _sorted_issues(
                [
                    *report["violations"],
                    _issue(
                        "BASELINE_WRITE_FAILED",
                        "error",
                        field="baseline",
                        details={"message": str(exc)},
                    ),
                ]
            )
            report["result"]["passed"] = False
            report["result"]["error_count"] += 1

    if report_path:
        write_json(report_path, report)
    result = report["result"]
    status = "PASS" if result["passed"] else "FAIL"
    print(
        f"{status}: {result['error_count']} error(s), "
        f"{result['debt_count']} known debt(s), "
        f"{result['new_debt']} new debt(s)"
    )
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    sys.exit(main())
