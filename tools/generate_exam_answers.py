"""为 seed_data.json"""为 seed_data.json 中无答案的真题批量生成答题框架（"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7."""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 48"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ===================="""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1."""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens":"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout="""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


#"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt:"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EX"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESS"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRIT"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANAL"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRE"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY","""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("W"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(ex"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = ["""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get(""""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f""""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) *"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本："""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"预估时间：~{len(no_answer) * 5 // 60} 分钟")
        return

    # 4. 加载缓存
    cache = {} if args.force else load_cache()
    if cache:
        print(f"加载缓存：{len(cache)} 条已生成答案（断点续跑）")

    # 5. 限制处理数量（测试用）
    to_process = no_answer
    if"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"预估时间：~{len(no_answer) * 5 // 60} 分钟")
        return

    # 4. 加载缓存
    cache = {} if args.force else load_cache()
    if cache:
        print(f"加载缓存：{len(cache)} 条已生成答案（断点续跑）")

    # 5. 限制处理数量（测试用）
    to_process = no_answer
    if args.max_questions > 0:
        to_process"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"预估时间：~{len(no_answer) * 5 // 60} 分钟")
        return

    # 4. 加载缓存
    cache = {} if args.force else load_cache()
    if cache:
        print(f"加载缓存：{len(cache)} 条已生成答案（断点续跑）")

    # 5. 限制处理数量（测试用）
    to_process = no_answer
    if args.max_questions > 0:
        to_process = no_answer[:args.max_questions]
        print"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"预估时间：~{len(no_answer) * 5 // 60} 分钟")
        return

    # 4. 加载缓存
    cache = {} if args.force else load_cache()
    if cache:
        print(f"加载缓存：{len(cache)} 条已生成答案（断点续跑）")

    # 5. 限制处理数量（测试用）
    to_process = no_answer
    if args.max_questions > 0:
        to_process = no_answer[:args.max_questions]
        print(f"【测试模式】只处理前 {args.max_questions} 题")

    # 6. 逐题生成
    total = len(to_process)
    success = 0
    failed = 0
    skipped = 0

    for i, q in enumerate(to_process, 1):
        qid = q["id"]

        # 检查缓存
        if qid in cache and not args.force:
            skipped += 1
            continue

        print(f"\n[{i}/{total}] {qid} | {q.get('year')}年 | {q.get('subject')} | {format_question_type(q.get('question_type', ''))}")
        print(f"  题目："""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"预估时间：~{len(no_answer) * 5 // 60} 分钟")
        return

    # 4. 加载缓存
    cache = {} if args.force else load_cache()
    if cache:
        print(f"加载缓存：{len(cache)} 条已生成答案（断点续跑）")

    # 5. 限制处理数量（测试用）
    to_process = no_answer
    if args.max_questions > 0:
        to_process = no_answer[:args.max_questions]
        print(f"【测试模式】只处理前 {args.max_questions} 题")

    # 6. 逐题生成
    total = len(to_process)
    success = 0
    failed = 0
    skipped = 0

    for i, q in enumerate(to_process, 1):
        qid = q["id"]

        # 检查缓存
        if qid in cache and not args.force:
            skipped += 1
            continue

        print(f"\n[{i}/{total}] {qid} | {q.get('year')}年 | {q.get('subject')} | {format_question_type(q.get('question_type', ''))}")
        print(f"  题目：{q.get('content', '')[:60]}"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"预估时间：~{len(no_answer) * 5 // 60} 分钟")
        return

    # 4. 加载缓存
    cache = {} if args.force else load_cache()
    if cache:
        print(f"加载缓存：{len(cache)} 条已生成答案（断点续跑）")

    # 5. 限制处理数量（测试用）
    to_process = no_answer
    if args.max_questions > 0:
        to_process = no_answer[:args.max_questions]
        print(f"【测试模式】只处理前 {args.max_questions} 题")

    # 6. 逐题生成
    total = len(to_process)
    success = 0
    failed = 0
    skipped = 0

    for i, q in enumerate(to_process, 1):
        qid = q["id"]

        # 检查缓存
        if qid in cache and not args.force:
            skipped += 1
            continue

        print(f"\n[{i}/{total}] {qid} | {q.get('year')}年 | {q.get('subject')} | {format_question_type(q.get('question_type', ''))}")
        print(f"  题目：{q.get('content', '')[:60]}...")

        # 构造 prompt
"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"预估时间：~{len(no_answer) * 5 // 60} 分钟")
        return

    # 4. 加载缓存
    cache = {} if args.force else load_cache()
    if cache:
        print(f"加载缓存：{len(cache)} 条已生成答案（断点续跑）")

    # 5. 限制处理数量（测试用）
    to_process = no_answer
    if args.max_questions > 0:
        to_process = no_answer[:args.max_questions]
        print(f"【测试模式】只处理前 {args.max_questions} 题")

    # 6. 逐题生成
    total = len(to_process)
    success = 0
    failed = 0
    skipped = 0

    for i, q in enumerate(to_process, 1):
        qid = q["id"]

        # 检查缓存
        if qid in cache and not args.force:
            skipped += 1
            continue

        print(f"\n[{i}/{total}] {qid} | {q.get('year')}年 | {q.get('subject')} | {format_question_type(q.get('question_type', ''))}")
        print(f"  题目：{q.get('content', '')[:60]}...")

        # 构造 prompt
        prompt = ANSWER_FRAMEWORK_PROMPT.format(
            year=q.get("year", "未知"),
            subject=q.get("subject", "未知"),
            exam_code=q.get("exam_paper_code", "未知"),
            question_type=format_question_type(q.get("question_type", "未知")),
            score=q.get("score", 0),
            content=q.get("content", ""),
        )

        #"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"预估时间：~{len(no_answer) * 5 // 60} 分钟")
        return

    # 4. 加载缓存
    cache = {} if args.force else load_cache()
    if cache:
        print(f"加载缓存：{len(cache)} 条已生成答案（断点续跑）")

    # 5. 限制处理数量（测试用）
    to_process = no_answer
    if args.max_questions > 0:
        to_process = no_answer[:args.max_questions]
        print(f"【测试模式】只处理前 {args.max_questions} 题")

    # 6. 逐题生成
    total = len(to_process)
    success = 0
    failed = 0
    skipped = 0

    for i, q in enumerate(to_process, 1):
        qid = q["id"]

        # 检查缓存
        if qid in cache and not args.force:
            skipped += 1
            continue

        print(f"\n[{i}/{total}] {qid} | {q.get('year')}年 | {q.get('subject')} | {format_question_type(q.get('question_type', ''))}")
        print(f"  题目：{q.get('content', '')[:60]}...")

        # 构造 prompt
        prompt = ANSWER_FRAMEWORK_PROMPT.format(
            year=q.get("year", "未知"),
            subject=q.get("subject", "未知"),
            exam_code=q.get("exam_paper_code", "未知"),
            question_type=format_question_type(q.get("question_type", "未知")),
            score=q.get("score", 0),
            content=q.get("content", ""),
        )

        # 调用 LLM
        result = call"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"预估时间：~{len(no_answer) * 5 // 60} 分钟")
        return

    # 4. 加载缓存
    cache = {} if args.force else load_cache()
    if cache:
        print(f"加载缓存：{len(cache)} 条已生成答案（断点续跑）")

    # 5. 限制处理数量（测试用）
    to_process = no_answer
    if args.max_questions > 0:
        to_process = no_answer[:args.max_questions]
        print(f"【测试模式】只处理前 {args.max_questions} 题")

    # 6. 逐题生成
    total = len(to_process)
    success = 0
    failed = 0
    skipped = 0

    for i, q in enumerate(to_process, 1):
        qid = q["id"]

        # 检查缓存
        if qid in cache and not args.force:
            skipped += 1
            continue

        print(f"\n[{i}/{total}] {qid} | {q.get('year')}年 | {q.get('subject')} | {format_question_type(q.get('question_type', ''))}")
        print(f"  题目：{q.get('content', '')[:60]}...")

        # 构造 prompt
        prompt = ANSWER_FRAMEWORK_PROMPT.format(
            year=q.get("year", "未知"),
            subject=q.get("subject", "未知"),
            exam_code=q.get("exam_paper_code", "未知"),
            question_type=format_question_type(q.get("question_type", "未知")),
            score=q.get("score", 0),
            content=q.get("content", ""),
        )

        # 调用 LLM
        result = call_llm(prompt, api_key, api_url,"""为 seed_data.json 中无答案的真题批量生成答题框架（answer_framework）。

这是 v0.7.3 P0 修复：补全 481 道真题缺失的参考答案。

设计要点：
- 独立脚本，不依赖 D:\\wenyan 原始 OCR 数据
- 直接读取 app/src/main/assets/seed_data.json，只处理 answer_framework=null 的真题
- 调用 LLM（OpenAI 兼容协议）生成结构化答题框架
- 支持断点续跑：已生成的答案保存到 exam_answers_cache.json，中断后重跑只处理缺失的
- 输出新版 seed_data.json（version 升至 2.3.0），触发 App 端版本感知重新导入

环境变量（与现有管线一致）：
  WENYAN_LLM_API_KEY  - LLM API密钥
  WENYAN_LLM_API_URL  - LLM API地址（OpenAI兼容格式，如 https://api.deepseek.com/v1/chat/completions）
  WENYAN_LLM_MODEL    - 模型名（默认 deepseek-chat）

用法：
  cd D:\\wenyan\\wenyan-android
  conda activate ocr
  python tools/generate_exam_answers.py

  # 或指定参数
  python tools/generate_exam_answers.py --dry-run  # 只统计不调用LLM
  python tools/generate_exam_answers.py --max-questions 10  # 只处理前10题（测试用）

成本估算：
  481题 × ~2000 tokens ≈ 96万 tokens
  DeepSeek: 约 ¥3-5
  处理时间: 约 40 分钟（含限流重试）
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path


# ==================== 配置 ====================

# 输入输出路径（相对于项目根目录）
SCRIPT_DIR = Path(__file__).parent.resolve()
PROJECT_ROOT = SCRIPT_DIR.parent
SEED_DATA_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed_data.json"
CACHE_PATH = SCRIPT_DIR / "exam_answers_cache.json"

# LLM 配置
DEFAULT_MODEL = "deepseek-chat"
MAX_RETRIES = 3
RETRY_BASE_DELAY = 2  # 秒
REQUEST_TIMEOUT = 90  # 秒
RATE_LIMIT_DELAY = 0.5  # 每次请求后等待，避免限流

# 输出版本号
OUTPUT_VERSION = "2.3.0"


# ==================== LLM Prompt ====================

ANSWER_FRAMEWORK_PROMPT = """你是一位南京师范大学文学院现当代文学考研（050106）的阅卷专家。请为以下真题生成结构化答题框架。

【题目信息】
- 年份：{year}年
- 科目：{subject}
- 试卷代码：{exam_code}
- 题型：{question_type}
- 分值：{score}分（0分表示未知，按常规分值估算）
- 题目正文：{content}

【生成要求】
1. 生成答题框架（answer_framework）：用①②③④分条要点，每条 1-2 句，标注预估分值
2. 框架要覆盖踩分点：核心概念定义 + 代表作品/作家 + 艺术特征 + 文学史意义
3. 若是名词解释（TERM_EXPLANATION）：150-250字，定义+特征+代表+意义
4. 若是简答题（SHORT_ANSWER）：300-500字，3-5个要点
5. 若是论述题（ESSAY）：500-800字，4-6个要点，含例证
6. 若是写作题（WRITING）：给出写作思路+结构建议，不写完整范文
7. 标注常见答题误区（1-2条），用【误区】前缀

【输出格式】
严格输出以下 JSON（不要markdown代码块标记）：
{{
  "answer_framework": "①要点1（X分）：...\\n②要点2（X分）：...\\n③要点3（X分）：...\\n【误区】...",
  "sample_essay": null,
  "confidence": 0.8
}}

注意：
- sample_essay 仅在论述题且你有把握时才填，否则填 null
- confidence 0-1，反映你对答案准确度的把握
- 不要编造不存在的作品或作家
- 若题目表述模糊无法判断考点，confidence 设为 0.3 并在框架末尾标注【题目待核实】"""


# ==================== LLM 调用 ====================

def call_llm(prompt: str, api_key: str, api_url: str, model: str) -> dict | None:
    """调用 LLM 生成答案框架，返回解析后的 dict 或 None。"""
    import requests

    for attempt in range(MAX_RETRIES):
        try:
            response = requests.post(
                api_url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.3,
                    "max_tokens": 2048,
                    "response_format": {"type": "json_object"},
                },
                timeout=REQUEST_TIMEOUT,
            )

            # 429 限流：等待后重试
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", RETRY_BASE_DELAY * (2 ** attempt)))
                if attempt < MAX_RETRIES - 1:
                    print(f"    限流，等待 {retry_after} 秒后重试 ({attempt + 1}/{MAX_RETRIES})")
                    time.sleep(retry_after)
                    continue
                else:
                    print(f"    限流重试耗尽，跳过此题")
                    return None

            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"].strip()

            # 移除可能的 markdown 代码块标记
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            print(f"    JSON 解析失败: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY)
                continue
            return None
        except requests.exceptions.Timeout:
            print(f"    请求超时，重试 ({attempt + 1}/{MAX_RETRIES})")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except requests.exceptions.RequestException as e:
            print(f"    网络错误: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BASE_DELAY * (2 ** attempt))
                continue
            return None
        except Exception as e:
            print(f"    未知错误: {e}")
            return None

    return None


# ==================== 缓存管理 ====================

def load_cache() -> dict:
    """加载断点续跑缓存。"""
    if CACHE_PATH.exists():
        try:
            with open(CACHE_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"警告：缓存读取失败，将重新生成: {e}")
    return {}


def save_cache(cache: dict) -> None:
    """保存缓存（断点续跑用）。"""
    try:
        with open(CACHE_PATH, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"警告：缓存保存失败: {e}")


# ==================== 主流程 ====================

def format_question_type(qt: str) -> str:
    """题型中文化。"""
    mapping = {
        "TERM_EXPLANATION": "名词解释",
        "SHORT_ANSWER": "简答题",
        "ESSAY": "论述题",
        "WRITING": "写作题",
        "ANALYSIS": "分析题",
        "APPRECIATION": "赏析题",
    }
    return mapping.get(qt, qt)


def main():
    parser = argparse.ArgumentParser(description="为真题生成答题框架")
    parser.add_argument("--dry-run", action="store_true", help="只统计不调用LLM")
    parser.add_argument("--max-questions", type=int, default=0, help="只处理前N题（0=全部）")
    parser.add_argument("--force", action="store_true", help="忽略缓存重新生成所有答案")
    args = parser.parse_args()

    # 1. 检查环境
    api_key = os.environ.get("WENYAN_LLM_API_KEY", "")
    api_url = os.environ.get("WENYAN_LLM_API_URL", "")
    model = os.environ.get("WENYAN_LLM_MODEL", DEFAULT_MODEL)

    if not args.dry_run:
        if not api_key or not api_url:
            print("错误：LLM API 未配置！")
            print("请设置环境变量：")
            print("  set WENYAN_LLM_API_KEY=你的API密钥")
            print("  set WENYAN_LLM_API_URL=https://api.deepseek.com/v1/chat/completions")
            print("  set WENYAN_LLM_MODEL=deepseek-chat")
            print()
            print("DeepSeek 申请地址：https://platform.deepseek.com/")
            sys.exit(1)
        print(f"LLM 配置：{api_url} | model={model}")
        print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
        print()

    # 2. 加载 seed_data.json
    print(f"读取种子数据：{SEED_DATA_PATH}")
    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data = json.load(f)

    exam_questions = seed_data.get("exam_questions", [])
    print(f"真题总数：{len(exam_questions)}")

    # 3. 统计无答案题目
    no_answer = [q for q in exam_questions if not q.get("answer_framework")]
    has_answer = [q for q in exam_questions if q.get("answer_framework")]
    print(f"已有答案：{len(has_answer)}")
    print(f"待生成答案：{len(no_answer)}")
    print()

    if args.dry_run:
        print("【dry-run 模式】不调用 LLM，仅统计。")
        # 按科目统计
        from collections import Counter
        subj_dist = Counter(q.get("subject") for q in no_answer)
        print("待生成题目按科目分布：")
        for s, c in subj_dist.most_common():
            print(f"  {s}: {c}")
        # 按题型统计
        type_dist = Counter(q.get("question_type") for q in no_answer)
        print("待生成题目按题型分布：")
        for t, c in type_dist.most_common():
            print(f"  {format_question_type(t)}: {c}")
        # 成本估算
        est_tokens = len(no_answer) * 2000
        est_cost = est_tokens / 1_000_000 * 1  # DeepSeek 输入¥1/百万token
        print(f"\n预估成本：~{est_tokens:,} tokens ≈ ¥{est_cost:.1f}")
        print(f"预估时间：~{len(no_answer) * 5 // 60} 分钟")
        return

    # 4. 加载缓存
    cache = {} if args.force else load_cache()
    if cache:
        print(f"加载缓存：{len(cache)} 条已生成答案（断点续跑）")

    # 5. 限制处理数量（测试用）
    to_process = no_answer
    if args.max_questions > 0:
        to_process = no_answer[:args.max_questions]
        print(f"【测试模式】只处理前 {args.max_questions} 题")

    # 6. 逐题生成
    total = len(to_process)
    success = 0
    failed = 0
    skipped = 0

    for i, q in enumerate(to_process, 1):
        qid = q["id"]

        # 检查缓存
        if qid in cache and not args.force:
            skipped += 1
            continue

        print(f"\n[{i}/{total}] {qid} | {q.get('year')}年 | {q.get('subject')} | {format_question_type(q.get('question_type', ''))}")
        print(f"  题目：{q.get('content', '')[:60]}...")

        # 构造 prompt
        prompt = ANSWER_FRAMEWORK_PROMPT.format(
            year=q.get("year", "未知"),
            subject=q.get("subject", "未知"),
            exam_code=q.get("exam_paper_code", "未知"),
            question_type=format_question_type(q.get("question_type", "未知")),
            score=q.get("score", 0),
            content=q.get("content", ""),
        )

        # 调用 LLM
        result = call_llm(prompt, api_key, api_url, model)

        if result and result.get("answer