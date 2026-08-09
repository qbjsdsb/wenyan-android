from __future__ import annotations

import copy
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCHEMA = ROOT / "content/schema/seed.schema.json"
FIXTURE = ROOT / "tools/tests/fixtures/minimal_seed.json"
MODULE = "tools.content_pipeline.audit_seed"


class AuditSeedCliTest(unittest.TestCase):
    def setUp(self) -> None:
        self.template = json.loads(FIXTURE.read_text(encoding="utf-8"))
        self.temp_dir = tempfile.TemporaryDirectory()
        self.work = Path(self.temp_dir.name)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def write_seed(self, value: dict, name: str = "seed.json") -> Path:
        path = self.work / name
        path.write_text(
            json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        return path

    def run_audit(
        self,
        seed: Path,
        *,
        baseline: Path | None = None,
        report_name: str = "report.json",
        write_baseline: Path | None = None,
    ) -> tuple[subprocess.CompletedProcess[str], dict]:
        report = self.work / report_name
        command = [
            sys.executable,
            "-m",
            MODULE,
            "--seed",
            str(seed),
            "--schema",
            str(SCHEMA),
            "--report",
            str(report),
            "--as-of-year",
            "2026",
        ]
        if baseline is not None:
            command.extend(["--baseline", str(baseline)])
        if write_baseline is not None:
            command.extend(["--write-baseline", str(write_baseline)])
        completed = subprocess.run(
            command,
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertTrue(report.exists(), completed.stderr or completed.stdout)
        return completed, json.loads(report.read_text(encoding="utf-8"))

    def make_baseline(self, seed: Path) -> Path:
        baseline = self.work / "baseline.json"
        completed, report = self.run_audit(seed, write_baseline=baseline)
        self.assertEqual(0, completed.returncode, report)
        self.assertTrue(baseline.exists())
        return baseline

    def update_baseline_seed_sha(self, baseline: Path, seed: Path) -> None:
        value = json.loads(baseline.read_text(encoding="utf-8"))
        value["seed_sha256"] = hashlib.sha256(seed.read_bytes()).hexdigest()
        baseline.write_text(
            json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )

    @staticmethod
    def codes(report: dict) -> set[str]:
        return {item["code"] for item in report["violations"]}

    def test_valid_fixture_reports_are_byte_identical(self) -> None:
        seed = self.write_seed(copy.deepcopy(self.template))
        first, report_one = self.run_audit(seed, report_name="report-one.json")
        second, report_two = self.run_audit(seed, report_name="report-two.json")
        self.assertEqual(0, first.returncode, first.stdout)
        self.assertEqual(0, second.returncode, second.stdout)
        self.assertEqual(
            (self.work / "report-one.json").read_bytes(),
            (self.work / "report-two.json").read_bytes(),
        )
        self.assertNotIn("generated_at", (self.work / "report-one.json").read_text())
        self.assertEqual(report_one["result"], report_two["result"])

    def test_duplicate_knowledge_point_id_fails(self) -> None:
        value = copy.deepcopy(self.template)
        value["knowledge_points"].append(copy.deepcopy(value["knowledge_points"][0]))
        completed, report = self.run_audit(self.write_seed(value))
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("DUPLICATE_ID", self.codes(report))

    def test_duplicate_exam_question_id_fails(self) -> None:
        value = copy.deepcopy(self.template)
        value["exam_questions"].append(copy.deepcopy(value["exam_questions"][0]))
        completed, report = self.run_audit(self.write_seed(value))
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("DUPLICATE_ID", self.codes(report))

    def test_deleted_baseline_id_fails(self) -> None:
        seed = self.write_seed(copy.deepcopy(self.template))
        baseline = self.make_baseline(seed)
        value = copy.deepcopy(self.template)
        value["knowledge_points"] = []
        changed_seed = self.write_seed(value, "changed.json")
        self.update_baseline_seed_sha(baseline, changed_seed)
        completed, report = self.run_audit(changed_seed, baseline=baseline)
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("DELETED_ID", self.codes(report))

    def test_dangling_related_point_id_fails(self) -> None:
        value = copy.deepcopy(self.template)
        value["exam_questions"][0]["related_point_ids"] = ["kp_99999"]
        completed, report = self.run_audit(self.write_seed(value))
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("DANGLING_RELATED_POINT_ID", self.codes(report))

    def test_relations_to_names_are_not_foreign_keys(self) -> None:
        value = copy.deepcopy(self.template)
        value["knowledge_points"][0]["relations"][0]["to"] = "作品名称而非知识点 ID"
        completed, report = self.run_audit(self.write_seed(value))
        self.assertEqual(0, completed.returncode, report)
        self.assertNotIn("DANGLING_RELATED_POINT_ID", self.codes(report))

    def test_duplicate_json_key_fails_without_pass_report(self) -> None:
        raw = '{"metadata": {}, "metadata": {}}\n'
        seed = self.work / "duplicate.json"
        seed.write_text(raw, encoding="utf-8")
        completed, report = self.run_audit(seed)
        self.assertNotEqual(0, completed.returncode)
        self.assertEqual("SEED_INVALID_JSON", report["violations"][0]["code"])
        self.assertFalse(report["result"]["passed"])

    def test_schema_type_error_fails(self) -> None:
        value = copy.deepcopy(self.template)
        value["exam_questions"][0]["score"] = "5"
        completed, report = self.run_audit(self.write_seed(value))
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("SCHEMA_INVALID", self.codes(report))

    def test_invalid_baseline_fails(self) -> None:
        seed = self.write_seed(copy.deepcopy(self.template))
        baseline = self.work / "invalid-baseline.json"
        baseline.write_text("{}\n", encoding="utf-8")
        completed, report = self.run_audit(seed, baseline=baseline)
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("BASELINE_INVALID", self.codes(report))

    def test_ai_draft_in_formal_collection_fails(self) -> None:
        value = copy.deepcopy(self.template)
        value["knowledge_points"][0]["content_status"] = "AI_DRAFT"
        completed, report = self.run_audit(self.write_seed(value))
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("FORBIDDEN_PUBLISHED_STATUS", self.codes(report))

    def test_reviewed_without_source_fails(self) -> None:
        value = copy.deepcopy(self.template)
        point = value["knowledge_points"][0]
        point["content_status"] = "REVIEWED"
        point["textbook_sources"] = ["其他"]
        completed, report = self.run_audit(self.write_seed(value))
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("REVIEWED_WITHOUT_SOURCE", self.codes(report))

    def test_ocr_watermark_fails(self) -> None:
        value = copy.deepcopy(self.template)
        value["writing_materials"][0]["content"] += " 联系微信咨询"
        completed, report = self.run_audit(self.write_seed(value))
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("OCR_NOISE", self.codes(report))

    def test_historical_ocr_can_be_baselined_but_new_ocr_fails(self) -> None:
        value = copy.deepcopy(self.template)
        value["writing_materials"][0]["content"] += " 历史微信水印"
        seed = self.write_seed(value)
        baseline = self.make_baseline(seed)

        changed = copy.deepcopy(value)
        material = copy.deepcopy(changed["writing_materials"][0])
        material["id"] = "wm_0002"
        material["content"] += " 新增微信水印"
        changed["writing_materials"].append(material)
        changed_seed = self.write_seed(changed, "changed.json")
        self.update_baseline_seed_sha(baseline, changed_seed)
        completed, report = self.run_audit(changed_seed, baseline=baseline)
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("OCR_NOISE", self.codes(report))

    def test_unapproved_cross_subject_relation_fails(self) -> None:
        value = copy.deepcopy(self.template)
        point = copy.deepcopy(value["knowledge_points"][0])
        point["id"] = "kp_00002"
        point["subject"] = "中国现当代文学"
        value["knowledge_points"].append(point)
        value["exam_questions"][0]["related_point_ids"] = ["kp_00002"]
        completed, report = self.run_audit(self.write_seed(value))
        self.assertNotEqual(0, completed.returncode)
        cross = [
            item for item in report["violations"] if item["code"] == "CROSS_SUBJECT_RELATION"
        ]
        self.assertEqual(1, len(cross))
        self.assertFalse(cross[0]["details"]["allowlisted"])

    def test_eq_0038_allowlist_is_exact_pair_only(self) -> None:
        value = copy.deepcopy(self.template)
        value["exam_questions"][0]["id"] = "eq_0038"
        point = copy.deepcopy(value["knowledge_points"][0])
        point["id"] = "kp_00002"
        point["subject"] = "中国现当代文学"
        value["knowledge_points"].append(point)
        value["exam_questions"][0]["related_point_ids"] = ["kp_00002"]

        seed = self.write_seed(value)
        baseline = self.make_baseline(seed)

        changed = copy.deepcopy(value)
        new_point = copy.deepcopy(point)
        new_point["id"] = "kp_00003"
        new_point["subject"] = "外国文学"
        changed["knowledge_points"].append(new_point)
        changed["exam_questions"][0]["related_point_ids"] = [
            "kp_00002",
            "kp_00003",
        ]
        changed_seed = self.write_seed(changed, "changed.json")
        self.update_baseline_seed_sha(baseline, changed_seed)

        completed, report = self.run_audit(changed_seed, baseline=baseline)
        self.assertNotEqual(0, completed.returncode)
        cross = [
            item
            for item in report["violations"]
            if item["code"] == "CROSS_SUBJECT_RELATION"
        ]
        self.assertEqual(
            {
                (item["details"]["target_id"], item["details"]["allowlisted"])
                for item in cross
            },
            {("kp_00002", True), ("kp_00003", False)},
        )

    def test_new_valid_object_is_allowed_after_baseline_sha_update(self) -> None:
        seed = self.write_seed(copy.deepcopy(self.template))
        baseline = self.make_baseline(seed)
        value = copy.deepcopy(self.template)
        point = copy.deepcopy(value["knowledge_points"][0])
        point["id"] = "kp_00002"
        value["knowledge_points"].append(point)
        changed_seed = self.write_seed(value, "changed.json")
        self.update_baseline_seed_sha(baseline, changed_seed)
        completed, report = self.run_audit(changed_seed, baseline=baseline)
        self.assertEqual(0, completed.returncode, report)
        self.assertEqual(0, report["result"]["new_debt"])

    def test_new_missing_source_is_new_debt(self) -> None:
        seed = self.write_seed(copy.deepcopy(self.template))
        baseline = self.make_baseline(seed)
        value = copy.deepcopy(self.template)
        point = copy.deepcopy(value["knowledge_points"][0])
        point["id"] = "kp_00002"
        point["textbook_sources"] = ["其他"]
        value["knowledge_points"].append(point)
        changed_seed = self.write_seed(value, "changed.json")
        self.update_baseline_seed_sha(baseline, changed_seed)
        completed, report = self.run_audit(changed_seed, baseline=baseline)
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("NEW_DEBT", self.codes(report))

    def test_resolved_debt_is_reported(self) -> None:
        debt_value = copy.deepcopy(self.template)
        debt_value["knowledge_points"][0]["textbook_sources"] = ["其他"]
        debt_seed = self.write_seed(debt_value, "debt.json")
        baseline = self.make_baseline(debt_seed)
        resolved_seed = self.write_seed(copy.deepcopy(self.template), "resolved.json")
        self.update_baseline_seed_sha(baseline, resolved_seed)
        completed, report = self.run_audit(resolved_seed, baseline=baseline)
        self.assertEqual(0, completed.returncode, report)
        self.assertGreaterEqual(report["result"]["resolved_debt"], 1)


if __name__ == "__main__":
    unittest.main()
