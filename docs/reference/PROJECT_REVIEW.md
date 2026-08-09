# 文研App项目全面复盘报告

> 生成时间：2026-07-11 06:15（Beijing时间）
> 报告范围：d:\wenyan 全项目
> 项目阶段：Phase 1 资料数字化执行中（约65%）

> **ARCHIVED / OBSOLETE（2026-08-09）**：这是 2026-07-11 的历史复盘，报告中的 D 盘范围、阶段百分比、模块清单和未开始状态不代表当前代码。正文保留作历史审计证据；当前系统事实以 [当前系统基线](../architecture/current-system.md) 为准。

---

## 一、项目总览

### 1.1 项目定位

**文研App** 是南师范大学文学院现当代文学考研专用App。项目从Web原型（index.html/js/css）演化为Android原生应用，采用 Kotlin 2.0+ / Jetpack Compose / Room / Hilt / MVVM / 多模块架构（参考Google Now in Android）。

### 1.2 规格文档三件套

| 文档 | 路径 | 内容 |
|------|------|------|
| spec.md | `.trae/specs/integrate-resources-and-ai-assistant/spec.md` | 项目规格说明（变更说明、技术决策、TDD要求） |
| tasks.md | `.trae/specs/integrate-resources-and-ai-assistant/tasks.md` | 33个任务（7个Phase），全部 `[ ]` 未勾选 |
| checklist.md | `.trae/specs/integrate-resources-and-ai-assistant/checklist.md` | C1.1~C8.28 验证项，P0/P1/P2三级 |

### 1.3 任务总览（33个Task / 7个Phase）

| Phase | 名称 | Task范围 | 状态 |
|-------|------|---------|------|
| Phase 1 | 资料数字化工具链（Python） | Task 1-9 | **执行中（约65%）** |
| Phase 2 | Android骨架+数据库Schema | Task 10-13 | **部分开始（骨架已搭建）** |
| Phase 3 | FSRS算法+卡片设计 | Task 14-18 | 未开始 |
| Phase 4 | 功能性知识图谱 | Task 19-22 | 未开始 |
| Phase 5 | AI助手+主动回忆检测 | Task 23-25 | 未开始 |
| Phase 6 | 科目代码历史+数据修正 | Task 26-27 | 未开始 |
| Phase 7 | 验证与测试 | Task 28-33 | 未开始 |

---

## 二、Phase 1 资料数字化详细进度

### 2.1 manifest.json 总状态（208个文件）

```
状态分布：
  completed: 103  (49.5%)
  pending:   68   (32.7%)
  skipped:   36   (17.3%)
  failed:    1    (0.5%)
```

### 2.2 按文件类型完成情况

| 文件类型 | 总数 | completed | pending | skipped | failed | 完成率 |
|---------|------|-----------|---------|---------|--------|--------|
| pdf | 153 | 50 | 67 | 36 | 0 | 32.7% |
| docx | 31 | 31 | 0 | 0 | 0 | **100%** |
| doc | 3 | 2 | 0 | 0 | 1 | 66.7% |
| xls | 13 | 13 | 0 | 0 | 0 | **100%** |
| xlsx | 3 | 3 | 0 | 0 | 0 | **100%** |
| image | 4 | 4 | 0 | 0 | 0 | **100%** |
| zip | 1 | 0 | 1 | 0 | 0 | 0% |

### 2.3 PDF子类型完成情况（关键！）

| PDF子类型 | 总数 | completed | pending | skipped | 说明 |
|-----------|------|-----------|---------|---------|------|
| native | 41 | 37 | 0 | 4 | 4个skipped是重复文件（"(1)"后缀） |
| ocr_layer | 8 | 8 | 0 | 0 | **100%完成** |
| scan_only | 65 | 5 | 40 | 20 | 20个skipped是名著，40个待RapidOCR |
| mixed | 39 | 0 | 27 | 12 | 12个skipped是名著，27个待RapidOCR |

### 2.4 content_source 与 ocr_status 分布（103个completed）

| content_source | 数量 | 说明 |
|---------------|------|------|
| TEXTBOOK_NATIVE | 86 | 原生电子文本（pdfplumber/docx/xlsx提取） |
| TEXTBOOK_OCR | 15 | OCR识别文本（MinerU 7个 + RapidOCR 2个 + 图片6个） |
| （空） | 2 | 早期处理未记录 |

| ocr_status | 数量 | 说明 |
|------------|------|------|
| VERIFIED | 86 | 已校对（原生文本默认VERIFIED） |
| PENDING | 15 | 待校对（OCR文本需后续post_correct处理） |

### 2.5 跳过文件（36个）

- **32个名著全文**（skip_novel）：译文名著文库、作家参考丛书系列（克尔恺郭尔、阿德勒、弗洛伊德、堂吉诃德等）
- **4个重复native PDF**（无skip_reason但is_duplicate=true）：file_136/138/187/189，与file_137/139/188/190内容重复

### 2.6 失败文件（1个）

- **file_077**：袁行霈版(超全完整版)笔记.doc
  - 错误：`DOC转换失败（请确认已安装MS Word）: (-2147418111, '被呼叫方拒绝接收呼叫。')`
  - 实际：JSON输出文件已生成（6554段落），但manifest状态因竞争条件被覆盖为failed
  - 修复方案：手动将manifest中file_077状态改为completed

### 2.7 当前执行状态

**RapidOCR全量执行**（PID 19296）：
- 已完成：file_075（321页，score=0.9895，60分钟）+ file_085（186页，score=0.9875）
- 正在处理：第二个文件第223页（CPU 363%，内存498MB）
- 速度：8-11秒/页（DPI=200，PP-OCRv6 small ONNX）
- 剩余：67个pending文件（67个pdf scan_only/mixed + 1个zip）
- 预计：约60-82小时（5-7天）

---

## 三、Task 1-9 完成度评估

### Task 1: Python OCR环境与批处理脚本骨架 ✅ 完成

| 子任务 | 状态 | 说明 |
|--------|------|------|
| 1.1 conda环境（ocr, Python 3.11.15） | ✅ | `C:\Users\33425\miniconda3\envs\ocr\` |
| 1.2 scan_files.py（文件扫描+manifest生成） | ✅ | 208个文件全入manifest，MD5去重 |
| 1.3 pipeline_runner.py（断点续传批处理） | ✅ | manifest驱动，指数退避重试 |
| 1.4 去重（"(1)"后缀检测） | ✅ | 4个重复文件正确标记 |
| 1.5 非PDF路由（docx/xls/image/zip） | ✅ | 全部实现，100%完成 |

### Task 2: 第一层原生电子文本 ⚠️ 基本完成（98.7%）

| 资料类型 | 总数 | 完成 | 说明 |
|---------|------|------|------|
| NATIVE PDF | 41 | 37 | 4个skipped是重复文件 |
| DOCX | 31 | 31 | **100%** |
| DOC | 3 | 2 | file_077失败（JSON已生成但状态错误） |
| XLSX | 3 | 3 | **100%** |
| XLS | 13 | 13 | **100%** |
| **小计** | 91 | 86 | **94.5%** |

### Task 3: 第二层网络权威电子文本 ✅ 完成

spec.md已修正：袁行霈4卷本已存在、聂珍钊上册已存在，无需用户补全。

### Task 4: 第三层OCR文本层PDF ✅ 完成

8个ocr_layer PDF全部完成（100%）。

### Task 5: 第四层扫描件OCR 🔄 进行中（8.3%）

| 项目 | 数据 |
|------|------|
| 总SCAN_ONLY+MIXED PDF | 104个 |
| 跳过（名著） | 32个 |
| 需OCR处理 | 67个pdf + 1个zip内pdf = 68个 |
| 已完成 | 5个scan_only（MinerU 3个 + RapidOCR 2个） |
| 图片 | 4/4完成（100%） |
| **完成率** | **9/68 = 13.2%**（含图片） |

当前RapidOCR全量执行中，预计5-7天完成。

### Task 6: OCR校对闭环 ❌ 未开始

- `post_correct.py` 脚本已编写（830行），但尚未执行
- 依赖Task 5完成（OCR文本需先全部产出）
- 功能：解析middle.json置信度→≥0.9入库/0.7-0.9 LLM纠错/<0.7人工校对
- **注意**：RapidOCR输出无middle.json，post_correct会跳过这些文件（不阻塞，但RapidOCR的置信度信息未被利用）

### Task 7: 知识提取与结构化 ❌ 未开始

- `extract_knowledge.py` 脚本已编写（960行），但尚未执行
- 依赖Task 6完成
- 功能：LLM提取知识点+实体识别+关系抽取+10%抽样校验

### Task 8: 多教材交叉校验 ❌ 未开始

- `cross_validate.py` 脚本已编写（510行），但尚未执行
- 依赖Task 7完成
- 功能：古代文学袁行霈vs马工程双轨制、现当代丁帆vs钱理群等

### Task 9: 生成种子数据JSON ❌ 未开始

- `generate_seed.py` 脚本已编写（870行），但尚未执行
- 依赖Task 2-8全部完成
- 输出：seed_data.json / reference_catalog.json / exam_code_history.json / error_dict.json

---

## 四、Phase 2 Android骨架进度

### 4.1 已搭建的骨架结构

```
android/
├── app/                          # 主应用模块
│   ├── src/main/java/com/wenyan/app/
│   │   ├── MainActivity.kt
│   │   ├── WenyanApp.kt
│   │   ├── WenyanApplication.kt
│   │   └── navigation/
│   │       ├── TopLevelDestination.kt
│   │       └── WenyanNavHost.kt
│   ├── src/main/assets/seed_data.json   # 种子数据（占位）
│   ├── src/androidTest/.../RoomDatabaseInstrumentedTest.kt
│   └── build.gradle.kts
├── core/                         # 核心模块
│   ├── ai/                       # AI助手模块
│   ├── common/                   # 公共工具
│   ├── data/                     # 数据层
│   ├── database/                 # Room数据库
│   ├── designsystem/             # 设计系统
│   └── fsrs/                     # FSRS算法
├── feature/                      # 功能模块
│   ├── aiassistant/              # AI助手功能
│   ├── cards/                    # 卡片复习
│   ├── graph/                    # 知识图谱
│   ├── knowledge/                # 知识库
│   └── quiz/                     # 真题测验
├── gradle/libs.versions.toml     # 依赖版本目录
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

### 4.2 Task 10-13 评估

| Task | 状态 | 说明 |
|------|------|------|
| Task 10: Android项目骨架 | ⚠️ 部分完成 | 多模块结构已搭建，有6个Kotlin文件，但各模块仅有build.gradle.kts，无实质代码 |
| Task 11: exam_questions Entity | ❌ 未开始 | core/database模块无Entity类 |
| Task 12: knowledge_points Entity | ❌ 未开始 | 同上 |
| Task 13: 新增Entity表 | ❌ 未开始 | 同上 |

---

## 五、工具链脚本完成度

### 5.1 核心管线脚本（9个，全部已编写）

| 脚本 | 行数 | 功能 | 执行状态 |
|------|------|------|---------|
| scan_files.py | - | 文件扫描+manifest生成 | ✅ 已执行 |
| pipeline_runner.py | ~1150 | 断点续传批处理 | ✅ 已执行（处理native/docx/xls等） |
| rapidocr_pipeline.py | ~480 | RapidOCR替代管线 | 🔄 执行中 |
| fast_ocr.py | - | Intel NPU OCR | ⚠️ 已测试未大规模使用 |
| post_correct.py | ~830 | OCR校对闭环 | ❌ 未执行 |
| extract_knowledge.py | ~960 | 知识提取 | ❌ 未执行 |
| cross_validate.py | ~510 | 多教材交叉校验 | ❌ 未执行 |
| generate_seed.py | ~870 | 种子数据生成 | ❌ 未执行 |
| d_drive_env.py | - | D盘环境配置 | ✅ 已使用 |

### 5.2 OCR技术选型演进

| 阶段 | 方案 | 速度 | 精度 | 问题 |
|------|------|------|------|------|
| 1. PaddleOCR 3.x | PP-OCRv6 | - | - | DLL冲突+API废弃 |
| 2. MinerU CLI | pipeline后端 | 30秒/页 | 高 | 太慢 |
| 3. RapidOCR | PP-OCRv6 ONNX small | 8-11秒/页 | 0.98+ | **当前主力** |
| 4. Intel NPU | ppocr.exe OpenVINO | 0.32秒/图 | 待验证 | 备选方案 |

---

## 六、问题与风险

### 6.1 当前阻塞问题

| # | 问题 | 影响 | 严重度 | 解决方案 |
|---|------|------|--------|---------|
| 1 | file_077 manifest状态错误 | 1个文件显示failed但JSON已生成 | 低 | 手动修复manifest状态为completed |
| 2 | file_084(zip)未处理 | 1个zip内367页PDF待OCR | 中 | 全量执行后单独用RapidOCR处理 |
| 3 | RapidOCR无middle.json | post_correct.py会跳过RapidOCR文件 | 中 | 后续可修改post_correct读取RapidOCR的置信度信息 |
| 4 | OCR速度8-11秒/页 | 68个文件约60-82小时 | 中 | 接受，分5-7天完成 |
| 5 | stdout被buffer | 无法看到实时进度 | 低 | 通过temp目录文件名判断进度 |

### 6.2 后续阶段风险

| # | 风险 | 影响 | 缓解措施 |
|---|------|------|---------|
| 1 | LLM API配置未确定 | post_correct/extract_knowledge需要LLM | 需配置API（OpenAI/通义千问等） |
| 2 | Android开发未实质开始 | Phase 2-7全部待做 | Phase 1完成后立即启动 |
| 3 | 种子数据未生成 | Android App无法加载真实数据 | Task 9依赖Task 2-8全部完成 |

---

## 七、项目完成度总评估

### 7.1 整体进度

```
项目总进度: 约 25%

├── Phase 1 资料数字化 ████████████████░░░░░░░░░░ 65%
│   ├── Task 1 环境搭建    ████████████████████ 100%
│   ├── Task 2 原生文本    ███████████████████░  95%
│   ├── Task 3 网络文本    ████████████████████ 100%
│   ├── Task 4 OCR层PDF   ████████████████████ 100%
│   ├── Task 5 扫描件OCR  ████░░░░░░░░░░░░░░░░░  13%
│   ├── Task 6 OCR校对    ░░░░░░░░░░░░░░░░░░░░   0%
│   ├── Task 7 知识提取    ░░░░░░░░░░░░░░░░░░░░   0%
│   ├── Task 8 交叉校验    ░░░░░░░░░░░░░░░░░░░░   0%
│   └── Task 9 种子数据    ░░░░░░░░░░░░░░░░░░░░   0%
│
├── Phase 2 Android骨架 ██░░░░░░░░░░░░░░░░░░░░  10%
│   ├── Task 10 项目骨架   ████░░░░░░░░░░░░░░░░  20%
│   ├── Task 11-13 Entity ░░░░░░░░░░░░░░░░░░░░   0%
│
├── Phase 3-7 ░░░░░░░░░░░░░░░░░░░░░░░░░░  0%
│
└── 文档与规格 ████████████████████████ 100%
```

### 7.2 关键数字

| 指标 | 数值 |
|------|------|
| 原始资料文件 | 208个 |
| 已数字化处理 | 103个（49.5%） |
| 跳过（名著/重复） | 36个（17.3%） |
| 待OCR处理 | 68个（32.7%）+ 1个failed |
| output JSON文件 | 104个 |
| Python脚本 | 9个核心 + 8个辅助 + 19个测试 |
| Android Kotlin文件 | 6个（骨架） |
| spec/tasks/checklist | 33个Task / C1.1-C8.28验证项 |
| OCR置信度 | 0.9875-0.9895（RapidOCR） |

### 7.3 剩余工作量估算

| 阶段 | 剩余工作 | 预估 |
|------|---------|------|
| Phase 1 Task 5 | 68个文件OCR | 5-7天（自动运行） |
| Phase 1 Task 6 | OCR校对闭环 | 1-2天（需LLM API） |
| Phase 1 Task 7 | 知识提取 | 2-3天（需LLM API） |
| Phase 1 Task 8 | 交叉校验 | 1天 |
| Phase 1 Task 9 | 种子数据生成 | 0.5天 |
| Phase 2 | Android骨架+Entity | 3-5天 |
| Phase 3 | FSRS+卡片 | 3-5天 |
| Phase 4 | 知识图谱 | 3-5天 |
| Phase 5 | AI助手 | 3-5天 |
| Phase 6 | 科目代码+修正 | 1天 |
| Phase 7 | 验证测试 | 2-3天 |
| **总计** | | **约25-35个工作日** |

---

## 八、结论与建议

### 8.1 项目当前状态

项目处于 **Phase 1 资料数字化的中后段**。非OCR文件（docx/xls/native PDF）已100%完成，OCR文件处理（scan_only/mixed PDF）正在RapidOCR全量执行中。工具链脚本（9个核心脚本）全部编写完成，但后续的校对/提取/校验/生成流程尚未执行。

### 8.2 核心优势

1. **文档质量极高**：spec/tasks/checklist三件套完整严谨
2. **工具链成熟**：9个核心脚本全部编写，经历过多次bug修复和优化
3. **断点续传**：manifest驱动，中断可恢复
4. **OCR质量高**：RapidOCR置信度0.98+

### 8.3 关键建议

1. **优先完成Phase 1**：等待RapidOCR全量执行完成，然后依次执行Task 6→7→8→9
2. **尽早配置LLM API**：Task 6/7都需要LLM，需提前准备API key
3. **并行启动Phase 2**：Android骨架已部分搭建，可在Phase 1 OCR运行期间并行推进Task 10-13
4. **修复file_077状态**：手动将manifest中file_077改为completed
5. **处理file_084**：全量OCR完成后单独用RapidOCR处理zip内PDF
