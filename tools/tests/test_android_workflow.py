from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/android.yml"


class AndroidWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")

    def test_content_gate_precedes_android_tests_and_build(self) -> None:
        audit = self.workflow.index("- name: Audit seed deterministically")
        unit_tests = self.workflow.index("- name: Run unit tests")
        build = self.workflow.index("- name: Build debug APK")
        self.assertLess(audit, unit_tests)
        self.assertLess(unit_tests, build)

    def test_python_and_deterministic_audit_contract_are_pinned(self) -> None:
        self.assertIn("uses: actions/setup-python@v5", self.workflow)
        self.assertIn("python-version: '3.12'", self.workflow)
        self.assertIn(
            "python -m unittest discover -s tools/tests -p 'test*.py'",
            self.workflow,
        )
        self.assertEqual(
            2,
            self.workflow.count("python -m tools.content_pipeline.audit_seed"),
        )
        self.assertEqual(2, self.workflow.count("--as-of-year 2026 --check"))
        self.assertIn(
            'cmp "$RUNNER_TEMP/wenyan-seed-audit-1.json" '
            '"$RUNNER_TEMP/wenyan-seed-audit-2.json"',
            self.workflow,
        )

    def test_audit_step_cannot_write_or_swallow_failures(self) -> None:
        audit = self.workflow.split("- name: Audit seed deterministically", 1)[1]
        audit = audit.split("- name: Upload seed audit evidence", 1)[0]
        self.assertNotIn("--write-baseline", audit)
        self.assertNotIn("|| true", audit)

    def test_failure_evidence_is_short_lived_and_always_uploaded(self) -> None:
        upload = self.workflow.split("- name: Upload seed audit evidence", 1)[1]
        upload = upload.split("- name: Set up JDK 17", 1)[0]
        self.assertIn("if: always()", upload)
        self.assertIn("name: wenyan-seed-audit", upload)
        self.assertIn("wenyan-seed-audit-*.json", upload)
        self.assertIn("retention-days: 7", upload)

    def test_existing_android_commands_and_apk_artifact_are_preserved(self) -> None:
        self.assertIn(
            "gradle testDebugUnitTest --no-daemon --stacktrace",
            self.workflow,
        )
        self.assertIn("gradle assembleDebug --no-daemon --stacktrace", self.workflow)
        self.assertIn("name: wenyan-debug-apk", self.workflow)


if __name__ == "__main__":
    unittest.main()
