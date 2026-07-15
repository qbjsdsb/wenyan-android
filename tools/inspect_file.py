#!/usr1/bin/env python
# -*- coding: utf-8 -*-
"""Inspect file_208.json structure"""
import json
import sys

io_encoding = 'utf-8'
sys.stdout.reconfigure(encoding=io_encoding)

path = r"D:\wenyan\tools\output\file_208.json"
with open(path, 'r', encoding='utf-8') as f:
    data = json.load(f)

print("Top-level keys:", list(data.keys()))
print()
print("relative_path:", data.get('relative_path'))
print("file_name:", data.get('file_name'))
print("file_type:", data.get('file_type'))
print()

pages = data.get('data', {}).get('pages', [])
print("Total pages:", len(pages))
print()

if pages:
    p0 = pages[0]
    print("Page[0] keys:", list(p0.keys()))
    print()
    # show first page content sample
    for k, v in p0.items():
        if isinstance(v, str):
            print(f"  {k} (str, len={len(v)}): {v[:200]!r}")
        else:
            print(f"  {k}: {v!r}"[:300])
    print()
    print("--- Page[1] ---")
    if len(pages) > 1:
        p1 = pages[1]
        for k, v in p1.items():
            if isinstance(v, str):
                print(f"  {k} (str, len={len(v)}): {v[:200]!r}")
            else:
                print(f"  {k}: {v!r}"[:300])
