# PR-01A：只读 seed 审计引擎与确定性 baseline

> 状态：已实施，当前进行本地严格自审；尚未提交或推送
> 前置阶段：PR-00 文档/系统基线
> 执行基线：从当时已合并的最新 main 新开分支
> 当前核对基线：origin/main@2436e6822a6a60bc58cba30176220cb13c91e191
> 本文件用途：给 Luna 和后续 Draft PR 提供唯一的 PR-01A 范围合同

## 1. 工单目标

本工单只建立一个不修改正式 seed 的、可重复运行的内容审计层。它要回答：

1. 当前 seed 的结构是否符合明确 schema；
2. 当前有哪些数量、来源和结构化债务；
3. 既有 ID、显式引用和数据边界是否保持稳定；
4. 新增重复 ID、删除旧 ID、悬空引用、非法结构、AI 草稿混入发布内容或 OCR 广告时，工具是否稳定失败；
5. 同一输入连续运行时，报告是否字节完全一致。

初始存量问题可以进入 baseline，但 baseline 不能把未来回归隐藏掉。审计工具不是 seed 修复器，也不是内容生成器。

## 2. 明确不做

本工单不得：

- 修改、重排、格式化回写或自动修复 app/src/main/assets/seed_data.json；
- 修改 Room、Kotlin、Compose、Gradle、版本号、导航、用户数据或业务行为；
- 接入 .github/workflows；CI 接入属于 PR-01B；
- 引入 OCR、LLM、网络服务或大型 Python 依赖；
- 把 tools.zip 整体解压到仓库，或提交其中的教材、扫描件、缓存、SDK、JDK、Gradle、日志和 Windows 路径；
- 猜测教材版本、页码、学者原话、真题题干、真题分值或官方答案；
- 把 AI_DRAFT 改成 REVIEWED，或用修改 seed 的方式让指标通过；
- 把知识点 relations[].to 这种作品/人物名称误当作知识点 ID 外键；
- 开始 PR-01B、PR-02 或 Today/UI 改造。

## 3. 允许修改范围

实施时原则上只允许修改以下目录和文件：

- tools/content_pipeline/：审计 CLI、schema 加载、确定性报告和 baseline 检查；
- tools/tests/：单元测试、最小 fixture 和恶意 fixture；
- content/schema/ 或等价的内容 schema 目录；
- content/baselines/ 或等价的机器可读 baseline 目录；
- tools/README.md 或必要的审计使用说明；
- 本工单明确需要的文档链接。

不得把临时报告、解压附件、正式 seed 副本或本地绝对路径提交到仓库。测试 fixture 必须是最小化、虚构或从结构中去掉受版权保护正文的样本。

## 4. 当前正式 seed 事实

### 4.1 顶层结构

正式文件是 app/src/main/assets/seed_data.json，当前顶层键为：

~~~text
metadata
subjects
knowledge_points
exam_questions
writing_materials
~~~

运行时 chapters 由 SeedDataLoader 生成，seed 并不直接提供章节数组；schema 不得错误要求顶层必须存在 chapters。

### 4.2 当前数量和摘要

| 指标 | 当前值 | 统计口径 |
| --- | ---: | --- |
| seed version | 2.26.0 | metadata.version |
| subjects | 4 | subjects 数组长度 |
| knowledge points | 1101 | knowledge_points 数组长度 |
| exam questions | 564 | exam_questions 数组长度 |
| ESSAY questions | 142 | question_type == ESSAY |
| writing materials | 909 | writing_materials 数组长度 |
| seed SHA-256 | d6385911bf31fbecaf168d5e882ec0bfc32be32c333fe14a28fc19db2726446 | 整个文件摘要 |

当前 seed 的真题年份范围为 2007—2026，题型包括 ANALYSIS、ESSAY、MULTIPLE_CHOICE、SHORT_ANSWER、TERM_EXPLANATION 和 WRITING。这些是当前事实，不代表未来版本永远不能增加题型；增加题型必须先更新 schema 和测试，而不是静默接受任意字符串。

### 4.3 主要对象字段

知识点重点字段：

~~~text
id, title, summary, core_conclusion, full_content, subject,
tags, difficulty, source_ref, textbook_sources, source_count,
conflict_flag, merged_at, study_text, exam_frequency,
entities, relations
~~~

真题重点字段：

~~~text
id, year, subject, question_type, content, score,
exam_paper_code, answer_framework, angle, notes,
related_point_ids
~~~

写作材料重点字段：

~~~text
id, category, sub_category, content, source, tags, created_at
~~~

### 4.4 来源和关联语义

- 知识点的 source_ref 和 textbook_sources 合并、去空、去重后，再过滤“其他”“未知”“待补”“无”“N/A”等占位标记，才算可识别的教材来源。
- 当前有 201/1101 个知识点具备至少一个非占位教材来源；高频知识点为 39/516。审计必须重新计算。
- knowledge_points[].relations 是实体、作品、人物关系。其 to 通常是名称，不是 knowledge point ID；不能把所有 to 值都按外键检查。
- exam_questions[].related_point_ids 才是当前明确的知识点 ID 引用，必须检查其目标是否存在。
- 当前 seed 中只有 eq_0038、eq_0182、eq_0254 三道题显式提供 related_point_ids；其余关联由运行时 loader 派生，不应被审计器误报为 seed 外键缺失。
- 真题跨科关系不能一刀切禁止。eq_0038 可能是同一综合题中的跨科内容；审计应报告题目、目标 ID 和科目差异，规则由明确 baseline 或 allowlist 决定。
- 正式 seed 当前没有统一的 published、content_status、source_status 字段。字段缺失本身应作为可见的 legacy debt 或 schema 版本事实，不能单独证明“没有 AI 草稿”。

## 5. 审计指标和失败规则

审计报告至少包含下列指标。所有集合和分布必须排序后输出，不能依赖 JSON 原始顺序。

### 5.1 数量和覆盖

- 四类正式数组数量；
- knowledge point 按 subject 数量；
- knowledge point 按 exam_frequency 数量；
- 有非占位教材来源的知识点总数和高频子集；
- 真题按 year、question_type、score 区间和 exam_paper_code 的分布；
- 真题 angle、notes、answer_framework 等结构化字段覆盖；
- 显式 related_point_ids 覆盖和引用总数；
- writing material 按 category、sub_category、source 的分布；
- 正式内容状态和来源状态分布；当前字段不存在时报告 legacy 状态缺失。

数量下降本身不自动等于错误，但任何已发布 ID 被删除必须失败或进入显式、带理由的人工批准流程。不得用 baseline 静默接受删除。

### 5.2 JSON 和 schema

- 输入必须是合法 UTF-8 JSON；
- 顶层键、必填字段、字段类型、枚举、非负分值和 ID 格式必须符合版本化 schema；
- 不接受重复对象键、数组字段被写成字符串、必填文本为空或引用字段类型错误；
- 允许当前历史数据明确记录的可选字段缺失；
- schema 校验失败时不得继续生成“看似完整”的通过报告。

schema 需要把“正式 seed 的 legacy 形状”和未来带审校状态的内容形状区分开。不能为了要求 content_status 而把当前 1101 条历史内容全部判成非法，也不能因此把状态缺失隐藏起来。

### 5.3 ID 和引用

- 每个正式内容集合内 ID 必须唯一；
- 需要同时报告跨集合同名 ID；若 ID 命名空间允许跨集合重复，必须在报告中明确 namespace；
- baseline 保存已存在 ID 的排序清单或稳定摘要；
- 当前输入删除 baseline 中任何旧 ID 时失败；
- 当前输入新增重复 ID 时失败；
- related_point_ids 的每一个值必须命中 knowledge point ID；
- 任何未来新增的明确外键字段都必须加入 schema 和引用规则；
- relations[].to、实体名称、标签和正文关键词不是自动外键，不得按 ID 规则误判。

历史 ID 必须原样保留。已知有 eq_0320b、eq_0399b、eq_0419b、eq_0463b 等带 b 后缀的真题 ID；规则不能用过严正则要求所有 ID 都是无后缀递增数字。

### 5.4 内容状态和来源

审计器需对显式状态字段采用版本化、可解释的读取规则：

- REVIEWED：来源、文本和映射已经人工审校；
- LEGACY_UNVERIFIED：历史内容可学习，但具体出处尚未补齐；
- AI_DRAFT：只能存在于草稿输入或非发布集合；
- REJECTED：不得进入正式发布集合；
- OFFICIAL_ORIGINAL、USER_CONFIRMED、SECONDARY_RECOLLECTION、UNKNOWN：用于真题真实性，不与内容审校状态混用。

出现以下情况必须失败：

- AI_DRAFT 或 REJECTED 出现在 published 或正式 seed；
- 新增 REVIEWED 条目没有有效 source；
- source 只有“其他”等占位词却宣称是具体教材或原卷；
- 真题状态声称 OFFICIAL_ORIGINAL 却没有相应的来源证据字段。

当前 seed 没有这些统一字段时，审计器应报告 legacy status field missing，而不是把每条历史内容凭空升级为 REVIEWED。

### 5.5 OCR 噪声

默认以大小写不敏感方式检测下列广告或水印词及其稳定变体：

~~~text
扫描全能王
微信
淘宝
公众号
店铺
加QQ
加 QQ
咨询微信
~~~

检测范围至少包括题干、答案框架、知识点标题/正文、写作素材文本和来源字段。报告必须包含对象类型、ID、字段和命中的稳定模式；不得把整段受版权保护正文复制到报告。

新增噪声必须失败。当前已知历史噪声可以进入 baseline，但 baseline 只能按对象 ID、字段和模式记录，不得按“总数”吞掉新对象。

### 5.6 跨科异常

真题 subject 与 related_point_ids 目标知识点的 subject 不一致时，报告：

- 题目 ID；
- 目标知识点 ID；
- 两侧科目；
- 是否命中明确 allowlist；
- allowlist 的理由。

新增未批准的跨科异常失败；历史已确认的综合题可以保留为 baseline。不能为了消除报告而删除关联或修改题目科目。

## 6. 确定性报告合同

同一输入、同一 schema、同一 baseline 和同一命令参数必须产生字节完全一致的报告。

具体要求：

- 默认不写 generated_at、随机数、机器路径、进程 ID 或环境差异；
- 若必须显示审计时间，默认关闭，并提供固定 as-of 参数；
- JSON 对象键使用稳定顺序，数组按稳定键排序；
- 文本统一 UTF-8，报告末尾换行规则固定；
- 路径使用输入参数给出的逻辑路径或仓库相对路径，不写临时目录绝对路径；
- 退出码稳定：通过为 0，schema/审计/ratchet 失败为非 0，参数错误为独立非 0；
- 报告内容不嵌入未经必要的教材原文，只输出摘要、字段、ID 和短模式名。

建议的报告结构：

~~~text
{
  "report_schema_version": 1,
  "audit_tool_version": "...",
  "input": {
    "seed_path": "...",
    "seed_sha256": "...",
    "content_version": "2.26.0"
  },
  "counts": {},
  "coverage": {},
  "distributions": {},
  "id_manifest_summary": {},
  "violations": [
    {
      "code": "...",
      "severity": "error|debt|info",
      "collection": "...",
      "id": "...",
      "field": "...",
      "details": {}
    }
  ],
  "result": {
    "passed": false,
    "new_debt": 0,
    "resolved_debt": 0
  }
}
~~~

具体字段可以在实现时细化，但不能破坏排序、无时间戳和不泄露正文这三个合同。

## 7. baseline ratchet 语义

baseline 是“截至某个已审阅 seed 提交，项目已经承认的存量债务清单”，不是通过报告的替代品。

baseline 至少保存：

- seed content version 和 SHA-256；
- 各正式集合的排序 ID manifest 或稳定摘要；
- 当前各类 violation 的唯一身份、对象 ID、字段和规则版本；
- 允许的历史跨科异常及理由；
- 生成参数和 schema 版本，但不保存机器绝对路径。

检查规则：

1. 新 violation identity 不在 baseline 中：失败；
2. 同一 violation identity 的数量或严重程度恶化：失败；
3. baseline 中的 violation 消失：通过，并报告 resolved；
4. 删除旧 ID：失败，不得仅因 baseline 存在而通过；
5. 新增 ID 默认允许，但新增对象若带重复 ID、悬空引用、AI_DRAFT 或 OCR 噪声仍失败；
6. baseline 文件本身非法、引用错误 seed SHA 或版本不匹配：失败；
7. CI 或普通审计命令不得自动修改 baseline；
8. 更新 baseline 必须是单独、人工确认的操作，并在 diff 中说明为什么接受该债务。

baseline 初次建立时要记录真实运行结果。不能把计划中的“约 900 个占位来源”“真题来源覆盖为 0”“论述题可能缺字段”等文字直接写进机器文件，必须由脚本计算后写入。

## 8. 建议命令接口

实现时保持一个清晰、无网络依赖的入口，例如：

~~~bash
python -m tools.content_pipeline.audit_seed \
  --seed app/src/main/assets/seed_data.json \
  --schema content/schema/seed.schema.json \
  --baseline content/baselines/seed-baseline.json \
  --report /tmp/wenyan-seed-audit.json \
  --format json \
  --as-of-year 2026
~~~

建议参数语义：

- --seed：只读输入；
- --schema：明确 schema 版本；
- --baseline：ratchet 检查输入；
- --report：确定性报告输出；
- --as-of-year：固定年份语境，不读取系统当前时间；
- --check：只做校验并以退出码表示结果；
- --write-baseline：显式、人工运行的本地操作，不得在 CI 自动调用。

命令不能默认覆盖 seed、不能隐式更新 baseline、不能依赖网络或当前用户目录。

## 9. 测试 fixture 和测试矩阵

至少需要以下最小 fixture：

| fixture | 预期 |
| --- | --- |
| 合法最小 seed | 能产生稳定报告 |
| 同集合重复 knowledge point ID | 失败，报告重复对象 |
| 同集合重复 exam question ID | 失败 |
| 删除 baseline 中的旧 ID | 失败 |
| related_point_ids 指向不存在 ID | 失败 |
| relations[].to 为作品名称 | 不因名称不是 ID 而误报 |
| 非法 JSON | 失败且不产生通过报告 |
| 顶层或字段类型错误 | schema 失败 |
| AI_DRAFT 进入 published 或正式集合 | 失败 |
| 新增 REVIEWED 但无有效来源 | 失败 |
| OCR 广告词出现在题干或写作材料 | 失败 |
| 已知历史跨科综合题 | 按 allowlist 或 baseline 处理 |
| 未批准新增跨科关系 | 失败 |
| 同一输入运行两次 | 两份报告字节相同 |
| baseline 中债务已消失 | 通过并标记 resolved |
| 新增一个普通合法对象 | ID manifest 正确增长，不重排旧 ID |

Python 侧至少运行：

~~~bash
python -m unittest discover -s tools/tests -p 'test*.py'
~~~

正式 seed 验收还应运行同一命令两次并比较：

~~~bash
python -m tools.content_pipeline.audit_seed ... --report /tmp/audit-1.json
python -m tools.content_pipeline.audit_seed ... --report /tmp/audit-2.json
cmp /tmp/audit-1.json /tmp/audit-2.json
sha256sum app/src/main/assets/seed_data.json
git diff --check
~~~

本工单不修改 Android 源码、Gradle 或 CI，因此不把 Gradle 构建伪装成审计证据。若实现阶段因项目流程额外运行 Android 测试，必须如实记录命令和结果。

## 10. 数据和 ID 风险

- 误把 relations[].to 当外键会制造大量假悬空引用；
- 用连续数字正则会误伤带 b 后缀的历史真题 ID；
- 把当前没有 status 字段误判为“全部已审校”会制造虚假的可信度；
- 把 source == 其他作为有效书名会污染来源覆盖率；
- 将运行时派生关联和 seed 显式关联混为一谈会错误报告引用覆盖；
- 把 baseline 设计为只存总数会隐藏新增债务；
- 把动态时间、绝对路径或 JSON 原始顺序写入报告会破坏确定性；
- 输出完整题干或教材文本会增加不必要的版权暴露；
- 为了通过数量门禁而删除旧 ID 或修改正式 seed，会破坏用户复习记录和可回滚性。

审计器遇到不确定的关系语义时应报告对象和证据，不应一刀切修复。

## 11. PR-01A 验收标准

只有同时满足以下条件，PR-01A 才能进入 Draft PR：

- 审计过程完全只读；
- 两次同输入报告字节完全一致；
- 报告默认不含时间戳和机器绝对路径；
- baseline 记录的是当前存量债务，不能掩盖新增债务；
- 重复 ID、删除旧 ID、悬空引用、非法 JSON/schema、AI_DRAFT 混入发布内容、OCR 广告和新增未批准跨科异常能稳定失败；
- 关系检查不会把名称关系误判为 ID 外键；
- 恶意 fixture 和回归 fixture 均有测试；
- 正式 seed 的内容、文件大小和 SHA-256 与实施前相同；
- 允许修改文件没有越出本工单目录；
- git diff --check 通过；
- 测试命令和未运行命令均真实记录。

## 12. 前置文档阻塞修复记录

在 PR-01A 实施前，曾先完成正式计划文件和独立文档工作树的阻塞修复：

- 新增 docs/plans/WENYAN-MASTER-PLAN.md；
- 新增 docs/plans/PR-01A.md；
- 当时不新增审计 CLI、schema、baseline、fixture 或测试；
- 当时不修改 seed、Room、Kotlin、Compose、Gradle、CI、版本号或用户数据；
- 当时不提交、不推送、不合并、不打 tag、不发布。

上述记录描述的是前置阻塞修复阶段；当前 PR-01A 实现及其验收以本文件第 11 节和实际 diff 为准。
