import json
from pathlib import Path

manifest = json.loads(Path(r'D:\wenyan\tools\manifest.json').read_text(encoding='utf-8'))
files = manifest.get('files', [])

pending = [f for f in files if f.get('status') not in ('completed', 'skipped')]
skipped = [f for f in files if f.get('status') == 'skipped']
completed = [f for f in files if f.get('status') == 'completed']

print(f"=== Manifest Summary ===")
print(f"Total: {len(files)} | Completed: {len(completed)} | Skipped(duplicate): {len(skipped)} | Pending: {len(pending)}")
print()
print(f"=== Pending Files (need OCR) ===")
for f in sorted(pending, key=lambda x: x['id']):
    print(f"  {f['id']}  [{f.get('category','?')}]  {f['file_name']}")
print()
print(f"=== Skipped (duplicates) ===")
for f in sorted(skipped, key=lambda x: x['id']):
    print(f"  {f['id']}  dup_of={f.get('duplicate_of')}  {f['file_name']}")
