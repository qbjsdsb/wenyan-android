import json, re
from collections import Counter

with open('app/src/main/assets/seed_data.json', 'r') as f:
    data = json.load(f)

essays = [e for e in data.get('exam_questions', []) if e.get('question_type') == 'ESSAY']
print(f'=== 论述题总数: {len(essays)} ===\n')

# 1. 分类所有前缀模式
patterns = Counter()
details = []

for e in essays:
    c = e.get('content', '').strip()
    first_line = c.split('\n')[0] if '\n' in c else c
    
    # 数字前缀: "1. xxx"
    m_num = re.match(r'^(\d+)\.\s*(.*)', c)
    # 中文数字前缀: "一、xxx" "二、" 等
    m_cn = re.match(r'^([一二三四五六七八九十]+)[、．]\s*(.*)', c)
    # 试卷标题前缀: "三、论述题" "二、分析题"
    m_paper = re.match(r'^[一二三四五六七八九十]+[、]', c)
    
    if m_num:
        num = m_num.group(1)
        rest = m_num.group(2)
        patterns[f'数字 "{num}."'] += 1
        details.append((e['id'], 'number', num, first_line[:60]))
    elif m_cn:
        cn = m_cn.group(1)
        rest = m_cn.group(2)
        # 区分是试卷标题还是内容前缀
        if re.match(r'^[论分]', rest):
            patterns[f'试卷标题 "{cn}、"'] += 1
            details.append((e['id'], 'paper_title', cn, first_line[:60]))
        else:
            patterns[f'中文数字 "{cn}、"'] += 1
            details.append((e['id'], 'cn_number', cn, first_line[:60]))
    else:
        # 无前缀
        patterns['无前缀（直接开始）'] += 1
        details.append((e['id'], 'none', '', first_line[:60]))

print('=== 前缀模式分布 ===')
for pat, count in patterns.most_common():
    print(f'  {pat}: {count} 题')

print('\n=== 详细分类 ===')
for cat_label, cat_items in [
    ('数字前缀（需清洗）', [d for d in details if d[1] == 'number']),
    ('中文数字前缀（需清洗）', [d for d in details if d[1] == 'cn_number']),
    ('试卷标题（需保留）', [d for d in details if d[1] == 'paper_title']),
    ('无前缀（无需处理）', [d for d in details if d[1] == 'none']),
]:
    print(f'\n--- {cat_label} ({len(cat_items)} 题) ---')
    for eid, typ, val, preview in cat_items[:6]:
        print(f'  {eid}: "{preview}..."')
    if len(cat_items) > 6:
        print(f'  ... 共 {len(cat_items)} 题')

# 2. 多行 content 分析
print('\n\n=== 多行 content 分析 ===')
multiline = [e for e in essays if '\n' in e.get('content', '')]
print(f'多行 content: {len(multiline)} 题')
for e in multiline:
    c = e.get('content', '').strip()
    lines = c.split('\n')
    print(f'\n  {e["id"]}:')
    for i, line in enumerate(lines[:5]):
        print(f'    L{i+1}: "{line[:80]}"')
    if len(lines) > 5:
        print(f'    ... 共 {len(lines)} 行')

# 3. 数字 1-9 的题号分布
print('\n\n=== 数字题号分布 ===')
num_dist = Counter()
for e in essays:
    m = re.match(r'^(\d+)\.\s*', e.get('content', '').strip())
    if m:
        num_dist[int(m.group(1))] += 1
for n in sorted(num_dist.keys()):
    print(f'  题号 "{n}.": {num_dist[n]} 题')

# 4. 清洗后首次字符检查（确保清洗后不会以标点开头）
print('\n\n=== 清洗后首字符检查 ===')
for e in essays:
    c = e.get('content', '').strip()
    m = re.match(r'^\d+\.\s*(.*)', c)
    if m:
        rest = m.group(1).strip()
        if rest and not rest[0].isalpha() and not rest[0].isnumeric():
            print(f'  ⚠️ {e["id"]}: 清洗后首字符="{rest[0]}" -> "{rest[:40]}"')