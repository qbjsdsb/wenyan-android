#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""丁帆《中国新文学史》下册断档第二批的版本化合并入口。"""

from __future__ import annotations

import sys
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import merge_dingfan_v2_23 as guard  # noqa: E402


guard.CANDIDATE_PATH = SCRIPT_DIR / "dingfan_cards_v2_24.json"
guard.REPORT_PATH = guard.REPO_ROOT / "docs/research/dingfan-supplement-v2.24.json"
guard.BASE_VERSION = "2.23.0"
guard.TARGET_VERSION = "2.24.0"
guard.BASE_COUNT = 1013
guard.FIRST_NEW_NUMBER = 1014
guard.CARD_COUNT = 10


if __name__ == "__main__":
    raise SystemExit(guard.main())
