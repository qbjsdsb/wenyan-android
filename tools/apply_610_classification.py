#!/usr/bin/env python3
"""应用 610 试卷科目重新分类到 seed_data.json（v2.7.0 → v2.8.0）

基于 classify_610.py 的自动分类结果 + 人工复核 14 题。
"""
import json
from pathlib import Path
from collections import Counter
from datetime import datetime

SEED_PATH = Path('/workspace/app/src/main/assets/seed_data.json')
CLASSIFICATION_PATH = Path('/workspace/tools/610_classification.json')

# 人工复核结论（11 道并列题 + 3 道自动分类误判）
MANUAL_OVERRIDES = {
    # === 11 道并列题 ===
    'eq_0063': '文学理论',   # 灵感在文章写作中的作用（文艺学必做题，理论概念）
    'eq_0079': '文学理论',   # 应用文的文本特征（文艺学必做题，理论范畴）
    'eq_0084': '外国文学',   # 黑色幽默（美国后现代主义流派）
    'eq_0101': '中国古代文学', # 骈体文的特征及价值（古代文体）
    'eq_0109': '文学理论',   # 艺术夸张（理论概念，文艺学必做题）
    'eq_0125': '中国古代文学', # 四六文的特征及价值（古代文体=骈体文）
    'eq_0165': '中国古代文学', # 《诗经》（古代文学核心典籍）
    'eq_0297': '中国现当代文学', # 张爱玲小说的艺术特色
    'eq_0348': '文学理论',   # 复调的作用（巴赫金理论，虽结合作品但核心是理论）
    'eq_0353': '文学理论',   # 叙述视角（叙事学理论概念）
    'eq_0362': '中国现当代文学', # 郁达夫与废名（现当代作家）

    # === 3 道自动分类误判修正 ===
    'eq_0078': '文学理论',   # 文学在戏剧影视中的作用（文艺学必做题，误因答案举例鲁迅/老舍）
    'eq_0080': '中国古代文学', # 红楼梦中的诗词曲（虽是文艺学必做题，但内容是古代文学核心作品）
    'eq_0081': '文学理论',   # 文学风格（理论概念，误因答案举例李白/杜甫/苏轼）
}


def main():
    # 加载自动分类结果
    with open(CLASSIFICATION_PATH, 'r', encoding='utf-8') as f:
        results = json.load(f)

    # 构建最终分类映射
    final_map = {}
    for r in results:
        qid = r['id']
        if qid in MANUAL_OVERRIDES:
            final_map[qid] = MANUAL_OVERRIDES[qid]
        elif r['new'] == '需复核':
            print(f'警告: {qid} 仍未分类，保持原值 {r["old"]}')
            final_map[qid] = r['old']
        else:
            final_map[qid] = r['new']

    # 加载 seed_data.json
    with open(SEED_PATH, 'r', encoding='utf-8') as f:
        d = json.load(f)

    # 统计变更
    changes = []
    for q in d.get('exam_questions', []):
        if q.get('exam_paper_code') == '610':
            old = q['subject']
            new = final_map.get(q['id'], old)
            if old != new:
                changes.append((q['id'], q['year'], old, new, q['content'][:60]))
                q['subject'] = new

    # 更新 metadata
    d['metadata']['version'] = '2.8.0'
    d['metadata']['generated_at'] = datetime.now().strftime('%Y-%m-%dT%H:%M:%S.000000')
    d['metadata']['description'] = '文研App种子数据（南师大文学考研）- v11 含610综合卷科目重新分类+真题答案错位修复+合并题拆分+OCR清洗'
    d['metadata']['fixes'].append(
        'v2.8.0 610综合卷127题按内容重新分类subject（原2010-2012全标中国古代文学、2013-2016全标文学理论，'
        '实际含4科：古代33/现当代31/外国25/理论27/复核后14题人工修正）'
    )

    # 保存
    with open(SEED_PATH, 'w', encoding='utf-8') as f:
        json.dump(d, f, ensure_ascii=False, indent=2)

    # 输出统计
    print(f'=== 610 试卷科目重新分类完成 ===')
    print(f'总变更: {len(changes)} 题')
    print()
    changes_by = Counter((o, n) for _, _, o, n, _ in changes)
    for (o, n), c in changes_by.most_common():
        print(f'  {o} → {n}: {c}题')

    print()
    print('=== 变更明细 ===')
    for qid, year, old, new, content in changes:
        print(f'  {qid} {year} [{old}→{new}]: {content}')

    # 验证最终分布
    print()
    print('=== 610 最终科目分布 ===')
    qs_610 = [q for q in d.get('exam_questions', []) if q.get('exam_paper_code') == '610']
    final_dist = Counter(q['subject'] for q in qs_610)
    for s, c in final_dist.most_common():
        print(f'  {s}: {c}')

    # 全局科目分布
    print()
    print('=== 全局科目分布（485题）===')
    all_dist = Counter(q['subject'] for q in d.get('exam_questions', []))
    for s, c in all_dist.most_common():
        print(f'  {s}: {c}')

    print(f'\nseed_data.json 已更新，版本 2.7.0 → 2.8.0')


if __name__ == '__main__':
    main()
