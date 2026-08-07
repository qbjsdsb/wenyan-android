"""generate_exam_questions.py - 文研App真题处理（v5）

v5改进（对比generate_seed.py的load_exam_questions）：
  1. file_208按年份+科目代码双层拆分（每年4套独立科目卷）
  2. 科目代码按年份区分映射（614→805等历史变动）
  3. 2018年解析缺失 → answer_status="NO_ANSWER"
  4. 题型补充"填空""选择""解释题"归类
  5. subject字段使用全称
  6. 综合卷（604/605/610）LLM辅助识别科目

使用方法：
  python generate_exam_questions.py --input output/file_208.json --output output/exam_questions.json

环境变量（LLM API配置）:
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址
  WENYAN_LLM_MODEL    - 模型名
"""

import argparse
import json
import os
import re
import sys
from datetime import datetime
from typing import Any


# ===== 常量定义 =====

# 科目名全称（对齐Android SeedDataLoader期望）
SUBJECT_NAME_MAP = {
    "古代文学": "中国古代文学",
    "现当代文学": "中国现当代文学",
    "外国文学": "外国文学",
    "文学理论": "文学理论",
    "综合": "COMPREHENSIVE",  # 综合卷需LLM辅助识别
}

# 科目代码→科目映射（按年份区分）
def get_subject_by_exam_code(exam_code: str, year: int) -> str:
    """根据科目代码和年份返回科目全称。

    科目代码历史变动：
      604/605: 2007-2009年综合卷
      610: 2010-2022年综合卷（2013-2016年文艺学方向=文学理论）
      614: 2013-2016年古代文学 → 2017年起改为805
      615: 2013-2016年现当代文学 → 2017年起改为806
      616: 2013-2016年外国文学 → 2017-2022年改为807
      805: 2023-2025年外国文学（与此前古代文学方向代码不同）
      807: 2017-2022年外国文学（2013-2016年为文学评论写作=现当代）
    """
    code = exam_code.upper()
    # 综合卷
    if code in ("604", "605"):
        return "COMPREHENSIVE"
    if code == "610":
        # 2013-2016年610文艺学综合基础=文学理论，但需要从试卷标题判断
        # 默认为综合卷，由LLM辅助识别
        return "COMPREHENSIVE"
    # 2023—2025 年外国文学方向改用 805；此前 805 为古代文学方向。
    if code == "805" and 2023 <= year <= 2025:
        return "外国文学"
    # 古代文学
    if code == "614" or (code == "805" and year >= 2017):
        return "中国古代文学"
    # 现当代文学
    if code == "615" or (code == "806" and year >= 2017):
        return "中国现当代文学"
    # 外国文学
    # 616: 2013-2016年外国文学（2016年部分试卷误标为617）
    # 807: 2017年起外国文学（2013-2016年为文学评论写作=现当代）
    if code in ("616", "617") or (code == "807" and year >= 2017):
        return "外国文学"
    # 807在2013-2016年是文学评论写作（现当代方向）
    if code == "807" and year < 2017:
        return "中国现当代文学"

    return "COMPREHENSIVE"  # 默认综合卷


# 题型映射
QUESTION_TYPE_MAP = {
    "名词解释": "TERM_EXPLANATION",
    "解释": "TERM_EXPLANATION",
    "解释题": "TERM_EXPLANATION",
    "简答": "SHORT_ANSWER",
    "简述": "SHORT_ANSWER",
    "简答题": "SHORT_ANSWER",
    "论述": "ESSAY",
    "分析": "ESSAY",
    "论述题": "ESSAY",
    "分析题": "ESSAY",
    "赏析": "ESSAY",  # v5.1新增：赏析题归类为ESSAY
    "评价": "ESSAY",  # v5.1新增
    "评论": "ESSAY",  # v5.1新增
    "阐述": "ESSAY",  # v5.1新增
    "写作": "WRITING",
    "作文": "WRITING",
    "写作题": "WRITING",
    "填空": "SHORT_ANSWER",  # 归类
    "填空题": "SHORT_ANSWER",
    "选择": "SHORT_ANSWER",  # 归类
    "选择题": "SHORT_ANSWER",
}


# ===== 真题拆分 =====

def split_exam_pages(pages: list[dict]) -> list[dict[str, Any]]:
    """按年份+科目代码拆分试题区。

    每个试卷的页范围由其标头页决定：
      - 起始页：该年份+科目代码标头所在页
      - 结束页：下一个标头页-1（或该年最后一页）

    Args:
        pages: 页面列表

    Returns:
        list: 拆分后的试卷列表，每个含year/exam_code/subject/start_page/end_page/text
    """
    # 试题标头正则：20XX年硕士研究生入学考试初试试题
    exam_header_pattern = re.compile(
        r"(20\d{2})年硕士研究生入学考试初试试题"
    )
    # 科目代码+名称正则：610综合基础 / 615中国现当代文学史 等
    code_pattern = re.compile(
        r"(?:科目代码[:：]?\s*)?(6\d{2}|8\d{2}|F\d{3})"
    )

    # 第一步：扫描所有页，记录每个标头页的(year, page_idx, codes_in_page)
    # 每页只记录第一个年份标头（避免同一页多次匹配）
    header_pages = []  # [(year, page_idx, [code1, code2, ...])]
    for i, page in enumerate(pages):
        text = page.get("text", "")
        m = exam_header_pattern.search(text)
        if not m:
            continue
        year = int(m.group(1))
        # 在该页找所有科目代码（同一页可能有多个代码，但通常只有一个）
        codes = []
        for cm in code_pattern.finditer(text):
            code = cm.group(1)
            if code not in codes:
                codes.append(code)
        header_pages.append((year, i, codes))

    if not header_pages:
        return []

    # 第二步：为每个标头页确定页范围（到下一个标头页-1）
    papers = []
    for idx, (year, page_idx, codes) in enumerate(header_pages):
        # 结束页：下一个标头页-1，或最后一页
        end_page = header_pages[idx + 1][1] - 1 if idx + 1 < len(header_pages) else len(pages) - 1

        # 合并该试卷的文本
        paper_text = "\n".join(
            pages[pi].get("text", "")
            for pi in range(page_idx, end_page + 1)
        )

        # 标头页文本（用于识别试卷标题中的"文艺学"等关键词）
        header_text = pages[page_idx].get("text", "")

        # 确定科目代码：优先用该页找到的代码，否则用综合卷默认
        if codes:
            # 取第一个非610的代码作为该卷代码（610通常是综合卷）
            # 如果只有610，就用610
            exam_code = codes[0]
            for c in codes:
                if c != "610":
                    exam_code = c
                    break
        else:
            exam_code = "610"

        subject = get_subject_by_exam_code(exam_code, year)

        # 610特殊处理：2013-2016年"610文艺学综合基础"=文学理论方向
        # 2010-2012年"610综合基础"=综合卷（COMPREHENSIVE）
        if exam_code == "610" and subject == "COMPREHENSIVE":
            if "文艺学" in header_text:
                subject = "文学理论"
            # 2010-2012年610综合基础：所有方向共做题，标记为综合卷
            # LLM未配置时默认为中国古代文学（综合卷需LLM辅助识别每题科目）

        papers.append({
            "year": year,
            "exam_paper_code": exam_code,
            "subject": subject,
            "start_page": page_idx + 1,  # 1-based
            "end_page": end_page + 1,
            "text": paper_text,
        })

    return papers


def split_analysis_pages(pages: list[dict]) -> dict[int, str]:
    """按年份拆分解析区。

    Args:
        pages: 页面列表

    Returns:
        dict: {year: analysis_text}
    """
    analysis_header_pattern = re.compile(
        r"(20\d{2})年硕士研究生入学考试初试试题解析"
    )

    year_pages = []
    for i, page in enumerate(pages):
        text = page.get("text", "")
        for m in analysis_header_pattern.finditer(text):
            year = int(m.group(1))
            year_pages.append((year, i))
            break

    if not year_pages:
        return {}

    # 按年份分组文本
    result = {}
    for idx, (year, page_idx) in enumerate(year_pages):
        next_page = year_pages[idx + 1][1] if idx + 1 < len(year_pages) else len(pages)
        text = "\n".join(
            pages[pi].get("text", "")
            for pi in range(page_idx, next_page)
        )
        result[year] = text

    return result


# ===== 题号拆分 =====

def split_by_question_number(text: str) -> list[dict[str, str]]:
    """按题号拆分试题文本（大题号/小题号层级识别）。

    题号层级：
      大题号：一、 二、 三、 ... （含题型标注，如"一、名词解释（40分）"）
      小题号：1. 2. 3. ... （具体题目，继承大题号的题型）

    拆分策略：
      1. 先找所有大题号（中文数字+顿号/点）
      2. 每个大题号区间内：
         a. 从大题号标题识别题型（如"一、名词解释"→TERM_EXPLANATION）
         b. 按小题号拆分，每道小题继承大题号题型
         c. 如果没有小题号，整个区间作为一道题
      3. 如果没有大题号，直接按小题号拆分

    Args:
        text: 试题文本

    Returns:
        list: 题目列表，每个含question_number/question_text/question_type
    """
    # 大题号正则：中文数字+顿号/点/右括号
    big_q_pattern = re.compile(
        r"^(?:[一二三四五六七八九十]+[、.．)])",
        re.MULTILINE
    )
    # 小题号正则：阿拉伯数字+顿号/点/右括号（排除年份19xx/20xx）
    small_q_pattern = re.compile(
        r"^(?:(?!19\d{2}|20\d{2})\d+[、.．)])",
        re.MULTILINE
    )

    # 找所有大题号
    big_matches = list(big_q_pattern.finditer(text))

    questions = []

    if not big_matches:
        # 没有大题号，直接按小题号拆分
        small_matches = list(small_q_pattern.finditer(text))
        if len(small_matches) < 2:
            return [{
                "question_number": "",
                "question_text": text.strip()[:2000],
                "question_type": detect_question_type(text),
            }]
        for i, m in enumerate(small_matches):
            start = m.start()
            end = small_matches[i + 1].start() if i + 1 < len(small_matches) else len(text)
            q_text = text[start:end].strip()
            if len(q_text) < 5:
                continue
            questions.append({
                "question_number": m.group().strip(),
                "question_text": q_text[:2000],
                "question_type": detect_question_type(q_text),
            })
        return questions

    # 有大题号：按大题号分区
    for bi, bm in enumerate(big_matches):
        big_start = bm.start()
        big_end = big_matches[bi + 1].start() if bi + 1 < len(big_matches) else len(text)
        big_text = text[big_start:big_end]
        big_number = bm.group().strip()

        # 从大题号标题（前100字）识别题型
        big_type = detect_question_type(big_text[:100])

        # 在大题号区间内找小题号
        small_matches = list(small_q_pattern.finditer(big_text))

        if not small_matches:
            # 没有小题号，整个大题号区间作为一道题
            q_text = big_text.strip()
            if len(q_text) < 5:
                continue
            questions.append({
                "question_number": big_number,
                "question_text": q_text[:2000],
                "question_type": big_type,
            })
        else:
            # 有小题号：跳过大题号标题行，按小题号拆分
            for si, sm in enumerate(small_matches):
                s_start = sm.start()
                s_end = small_matches[si + 1].start() if si + 1 < len(small_matches) else len(big_text)
                q_text = big_text[s_start:s_end].strip()
                if len(q_text) < 5:
                    continue
                # 小题号题目继承大题号的题型
                q_type = big_type if big_type != "UNKNOWN" else detect_question_type(q_text)
                questions.append({
                    "question_number": sm.group().strip(),
                    "question_text": q_text[:2000],
                    "question_type": q_type,
                })

    return questions


def detect_question_type(text: str) -> str:
    """识别题型。

    Args:
        text: 题目文本

    Returns:
        str: 题型（TERM_EXPLANATION/SHORT_ANSWER/ESSAY/WRITING/UNKNOWN）
    """
    # 取前100字判断题型
    head = text[:100]

    for keyword, q_type in QUESTION_TYPE_MAP.items():
        if keyword in head:
            return q_type

    # v5.1新增：对含书名号《》的短题目（<60字）推断为名词解释
    # 考研试题中"1. 《离骚》"、"2. 《桃花扇》"等格式通常是名词解释题
    if "《" in head and "》" in head and len(text.strip()) < 60:
        return "TERM_EXPLANATION"

    return "UNKNOWN"


# ===== 综合卷LLM科目识别 =====

def identify_comprehensive_subject(
    question_text: str,
    api_config: dict | None = None,
) -> str:
    """用LLM识别综合卷题目的科目。

    Args:
        question_text: 题目文本
        api_config: API配置

    Returns:
        str: 科目全称（中国古代文学/中国现当代文学/外国文学/文学理论）
    """
    if api_config is None:
        api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
        api_url = os.environ.get("WENYAN_LLM_API_URL", "")
        model = os.environ.get("WENYAN_LLM_MODEL", "deepseek-chat")
    else:
        api_key = api_config.get("api_key", "")
        api_url = api_config.get("api_url", "")
        model = api_config.get("model", "deepseek-chat")

    if not api_key or not api_url:
        return "中国古代文学"  # 默认古代文学

    prompt = f"""请判断以下考研试题属于哪个科目，只回答科目名称（中国古代文学/中国现当代文学/外国文学/文学理论），不要其他内容。

题目：{question_text[:200]}

科目："""

    try:
        import requests as _requests
        response = _requests.post(
            api_url,
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
            json={
                "model": model,
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0.1,
                "max_tokens": 20,
            },
            timeout=30,
        )
        response.raise_for_status()
        result = response.json()
        content = result["choices"][0]["message"]["content"].strip()

        # 匹配返回的科目名
        for subject in ["中国古代文学", "中国现当代文学", "外国文学", "文学理论"]:
            if subject in content:
                return subject

        return "中国古代文学"  # 默认
    except Exception:
        return "中国古代文学"  # 默认


# ===== 主处理函数 =====

def process_exam_file(
    file_path: str,
    output_path: str,
    api_config: dict | None = None,
) -> dict[str, Any]:
    """处理真题文件（file_208）。

    Args:
        file_path: 真题文件路径
        output_path: 输出文件路径
        api_config: LLM API配置

    Returns:
        dict: 处理结果摘要
    """
    with open(file_path, "r", encoding="utf-8") as f:
        file_data = json.load(f)

    pages = file_data["data"]["pages"]
    file_name = file_data.get("file_name", "")
    relative_path = file_data.get("relative_path", "")

    print(f"处理 {file_name}: {len(pages)}页")

    # 1. 拆分试题区（前42页）
    exam_pages = pages[:42]
    exam_papers = split_exam_pages(exam_pages)
    print(f"试题拆分: {len(exam_papers)}套试卷")

    # 2. 拆分解析区（43页之后）
    analysis_pages = pages[42:]
    analysis_by_year = split_analysis_pages(analysis_pages)
    print(f"解析拆分: {len(analysis_by_year)}年")

    # 3. 对每套试卷按题号拆分
    all_questions = []
    question_id_counter = 1

    for paper in exam_papers:
        year = paper["year"]
        exam_code = paper["exam_paper_code"]
        subject = paper["subject"]
        paper_text = paper["text"]

        # 按题号拆分
        questions = split_by_question_number(paper_text)

        # 获取该年解析
        analysis_text = analysis_by_year.get(year, "")

        for q in questions:
            # 综合卷需LLM识别科目
            if subject == "COMPREHENSIVE":
                subject_identified = identify_comprehensive_subject(
                    q["question_text"], api_config
                )
            else:
                subject_identified = subject

            # 判断是否有答案
            has_answer = bool(analysis_text.strip())

            all_questions.append({
                "id": f"eq_{question_id_counter:04d}",
                "year": year,
                "subject": subject_identified,
                "exam_paper_code": exam_code,
                "question_type": q["question_type"],
                "content": q["question_text"],
                "question_number": q["question_number"],
                "score": None,
                "answer_framework": "",  # 后续由LLM生成
                "sample_essay": "",
                "answer_status": "HAS_ANSWER" if has_answer else "NO_ANSWER",
                "analysis_text": analysis_text[:3000] if has_answer else "",  # 解析文本
                "material_text": "",
                "source_file": file_name,
                "source_relative_path": relative_path,
                "source_pages": f"p{paper['start_page']}-p{paper['end_page']}",
                "content_source": "TEXTBOOK_OCR",
                "ocr_status": "VERIFIED",
                "created_at": datetime.now().isoformat(),
            })
            question_id_counter += 1

    print(f"题目总数: {len(all_questions)}")

    # 4. 统计
    year_stats = {}
    subject_stats = {}
    type_stats = {}
    for q in all_questions:
        year_stats[q["year"]] = year_stats.get(q["year"], 0) + 1
        subject_stats[q["subject"]] = subject_stats.get(q["subject"], 0) + 1
        type_stats[q["question_type"]] = type_stats.get(q["question_type"], 0) + 1

    print(f"\n年份分布: {dict(sorted(year_stats.items()))}")
    print(f"科目分布: {subject_stats}")
    print(f"题型分布: {type_stats}")

    # 5. 写入结果
    result = {
        "file_name": file_name,
        "relative_path": relative_path,
        "total_questions": len(all_questions),
        "year_range": f"{min(year_stats.keys())}-{max(year_stats.keys())}",
        "year_stats": year_stats,
        "subject_stats": subject_stats,
        "type_stats": type_stats,
        "exam_questions": all_questions,
        "processed_at": datetime.now().isoformat(),
    }

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"\n写入: {output_path}")
    return result


def main():
    parser = argparse.ArgumentParser(description="文研App真题处理（v5双层拆分）")
    parser.add_argument("--input", default="output/file_208.json", help="真题文件路径")
    parser.add_argument("--output", default="output/exam_questions.json", help="输出文件路径")
    args = parser.parse_args()

    process_exam_file(args.input, args.output)


if __name__ == "__main__":
    main()
