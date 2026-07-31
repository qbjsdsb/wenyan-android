# 论述题板块设计方案

> **状态**: 设计阶段（待用户审批后进入实现）
> **日期**: 2026-07-31
> **依据**: [docs/research/essay-answer-methodology.md](../research/essay-answer-methodology.md)（1.45 万字网络调研，44 个可点击来源）
> **响应用户反馈**: "增加一个论述题的板块，融合在知识点板块里面……每个题最下面都要给依据，给那种交叉验证的链接以及思路"

## 一、背景与目标

### 1.1 为什么做论述题板块

考研现当代文学（050106）的主观题以**论述题**为主，分值高（通常 30 分/题）、占比大。调研结论（[报告第六章](../research/essay-answer-methodology.md)）明确：

- 阅卷老师在 **30–60 秒**内扫完一道论述题，看重的是**论点明确 / 论据充分 / 逻辑严密 / 有个人见解 / 学术视野**，而非知识点堆砌。
- 现有 App 有 **134 道论述题**（占 485 真题的 27.6%），但当前真题页**无题型筛选**、知识点详情页**无关联真题**、论述题**无思路/依据/交叉验证**呈现——考生只能看到 `answer_framework`（答题框架），缺乏"怎么审题、怎么破题、依据在哪、如何交叉验证"的深度学习闭环。

### 1.2 核心目标

| # | 目标 | 对应用户诉求 |
|---|------|------|
| G1 | 论述题板块**融合在知识点板块** | "融合在知识点板块里面" |
| G2 | 每道论述题给出**答题思路**（审题/破题/论证路径） | "如何入手、如何措辞、如何审题、如何覆盖知识点" |
| G3 | 每道论述题给出**依据**（作品原文/学者观点/教材定论） | "每个题最下面都要给依据" |
| G4 | 每道论述题给出**交叉验证链接**（多教材/多学者对照） | "给那种交叉验证的链接" |
| G5 | 接入 **AI 三阶段引导**（分析→建议→范文） | "怎么写好这一道题" |

### 1.3 调研方法论

详见 [docs/research/essay-answer-methodology.md](../research/essay-answer-methodology.md)。该报告调研了 **44 个真实来源**（高校教师、考研阅卷老师、高分上岸学长姐、学术写作规范、考研机构方法论、权威学术平台），每个核心结论均有 2+ 来源交叉验证。本设计方案的所有方法论依据均来自该报告。

## 二、现有基础（项目已具备的能力）

项目结构调研确认，论述题板块**无需新建表、无需大改架构**，现有基础设施高度兼容：

### 2.1 数据层（已就绪）

| 能力 | 现状 | 文件 |
|------|------|------|
| 真题表支持 ESSAY 题型 | ✅ `question_type` 已含 `ESSAY` 枚举，134 道论述题已入库 | [ExamQuestionEntity.kt](../../core/database/src/main/java/com/wenyan/app/core/database/entity/ExamQuestionEntity.kt) |
| 答题框架已填充 | ✅ 485/485 题均有 `answer_framework` | seed_data.json |
| 真题→知识点关联字段 | ⚠️ `related_point_ids` 字段已存在，但 seed 中**全为 null** | [SeedDataLoader.kt:355](../../core/data/src/main/java/com/wenyan/app/core/data/seed/SeedDataLoader.kt) |
| 考查角度字段 | ✅ `angle` 字段已存在，seed 为 null（可存"思路"） | ExamQuestionEntity.kt |
| 备注字段 | ✅ `notes` 字段已存在，seed 为 null（可存"依据"） | ExamQuestionEntity.kt |
| 错题本支持真题来源 | ✅ `wrong_answers.exam_question_id` 外键 | WrongAnswerEntity.kt |
| AI 批改记录表 | ✅ `ai_grading_records` 表已设计（未接入） | AiGradingRecordEntity.kt |
| 答题模板表 | ✅ `answer_templates` 表已支持 ESSAY | AnswerTemplateEntity.kt |

### 2.2 AI 层（已就绪）

| 能力 | 现状 | 文件 |
|------|------|------|
| 三阶段苏格拉底引导 | ✅ `SocraticTutor.guideEssayAnswer(question, userAnswer)` | [SocraticTutor.kt:52](../../core/ai/src/main/java/com/wenyan/app/core/ai/SocraticTutor.kt) |
| 论证漏洞分析 Prompt | ✅ `buildAnalyzePrompt` | PromptTemplates.kt |
| 改进建议 Prompt | ✅ `buildSuggestPrompt` | PromptTemplates.kt |
| 范文生成 Prompt | ✅ `buildSampleEssayPrompt`（500–800 字） | PromptTemplates.kt |
| RAG 引用可溯源 | ✅ `RagEngine.search`（基于 knowledge_points LIKE） | RagEngine.kt |
| Prompt Injection 防护 | ✅ `<USER_INPUT>` 边界标记 | PromptTemplates.kt |

### 2.3 UI 层（部分就绪）

| 能力 | 现状 | 缺口 |
|------|------|------|
| 知识点详情页 | ✅ 6 个区块（标题/摘要/多教材/来源/关联知识点/错题） | ❌ 无"相关论述题"区块 |
| 真题练习页 | ✅ 按年份+科目浏览，支持答题+自评 | ❌ 无题型筛选，无思路/依据展示 |
| AI 助手 | ✅ 对话式 + RAG | ❌ 未与论述题答题流程深度集成 |

## 三、数据模型设计

### 3.1 核心决策：复用 `exam_questions` 表，不新建表

**理由**：
1. `question_type` 已支持 ESSAY，134 道论述题已入库
2. `angle` + `notes` 字段已存在，可承载"思路"和"依据"（无需 Migration）
3. 错题本、AI 批改记录表均通过 `exam_question_id` 外键关联，新建表会破坏外键关系
4. 新建表增加 schema 复杂度，收益低

### 3.2 字段复用方案

| 字段 | 原用途 | 新用途（论述题板块） | 数据格式 |
|------|--------|------|------|
| `related_point_ids` | 真题→知识点关联（seed 全 null） | **补全**：派生真题↔知识点双向关联 | `List<String>`（知识点 ID） |
| `angle` | 考查角度（seed null） | **存"思路"**：审题+破题+论证路径 | JSON 字符串（见 3.3） |
| `notes` | 备注（seed null） | **存"依据与交叉验证"**：依据列表+交叉验证对照 | JSON 字符串（见 3.4） |

### 3.3 `angle` 字段：思路 JSON 结构

```json
{
  "questionType": "比较型",
  "coreKeywords": ["张天翼讽刺小说", "沙汀讽刺小说"],
  "limitKeywords": [],
  "task": "比较异同 + 归因",
  "breakthroughAngles": [
    "①题材选择",
    "②讽刺手法",
    "③人物塑造",
    "④价值立场"
  ],
  "angleRationale": "4 维度覆盖'内容—形式—主题—立场'完整链条",
  "argumentPath": {
    "thesis": "二者同属 30 年代左翼讽刺小说传统，但在题材/手法/立场上呈现差异",
    "points": [
      {"label": "分1（同）", "content": "共同点——都受鲁迅讽刺传统影响，都暴露黑暗"},
      {"label": "分2（异·题材）", "content": "张天翼多写市民/小市民；沙汀多写四川农村基层"},
      {"label": "分3（异·手法）", "content": "张天翼夸张漫画化；沙汀冷峻白描"},
      {"label": "分4（异·立场）", "content": "张天翼更直接批判；沙汀更含蓄揭露"}
    ],
    "conclusion": "差异背后是生活经验与艺术个性不同，共同推进了现代讽刺小说成熟"
  }
}
```

> 模板来自调研报告 [第九章 9.3 节](../research/essay-answer-methodology.md)，对应"思路三层呈现"：审题思路 / 破题角度 / 论证路径。

### 3.4 `notes` 字段：依据与交叉验证 JSON 结构

```json
{
  "evidences": [
    {
      "type": "WORK_TEXT",
      "label": "作品原文",
      "content": "《华威先生》：「他永远是夹着公文包…」",
      "source": "张天翼《华威先生》"
    },
    {
      "type": "SCHOLAR_OPINION",
      "label": "学者观点",
      "content": "钱理群指出 30 年代讽刺小说形成了'暴露与批判'传统",
      "source": "钱理群《中国现代文学三十年》北京大学出版社 1998 年版，第 X 页",
      "url": null
    },
    {
      "type": "TEXTBOOK_CONSENSUS",
      "label": "教材定论",
      "content": "丁帆《新文学史》将张天翼与沙汀并列为左翼讽刺双壁",
      "source": "丁帆《中国新文学史》上册，高等教育出版社 2013 年版，第 120 页",
      "url": null
    }
  ],
  "crossValidation": {
    "textbookComparison": "钱理群《三十年》vs 丁帆《新文学史》对二人异同的表述基本一致，丁帆更强调地域差异",
    "scholarComparison": "王瑶《新文学史稿》vs 严家炎《中国现代小说流派史》对讽刺传统的定位：王瑶重社会批判，严家炎重艺术传承"
  },
  "referenceLinks": [
    {"label": "中国作家网·相关评论", "url": "https://www.chinawriter.com.cn/..."},
    {"label": "社科院文学所·研究论文", "url": "http://literature.cass.cn/..."}
  ]
}
```

> 模板来自调研报告 [第九章 9.1–9.2 节](../research/essay-answer-methodology.md)，对应"依据三类型 + 交叉验证方法"。

### 3.5 数据补全：派生 `related_point_ids`

**问题**：seed 中 `related_point_ids` 全为 null，知识点详情页"相关论述题"无数据。

**方案**：在 `SeedDataLoader` 新增 `computeExamQuestionRelatedPoints()`（参考已有的 `computeRelatedIdsByTags` 模式）：
- 输入：真题 `content` + `answerFramework`，知识点 `title` + `tags` + `summary`
- 匹配逻辑：知识点标题/标签在真题内容或答题框架中出现 → 建立关联
- 取 Top 5（避免过度关联）
- seed 版本 2.13.1 → 2.14.0 触发重新导入

### 3.6 数据补全：填充 `angle` + `notes`

**方案**：分两步

1. **AI 批量生成**（一次性）：对 134 道论述题，用 LLM 基于 `content` + `answerFramework` + 关联知识点生成"思路"和"依据"JSON，写入 seed_data.json
2. **人工校验**：抽样 20 题人工校验，确保依据真实、交叉验证准确、链接可点击

> ⚠️ 关键约束：依据中的学者观点、教材页码**必须真实**，不能 AI 编造。生成时需限定 LLM 只引用已知的 4 部官方教材（童庆炳/袁世硕/丁帆/聂珍钊）+ 学界公认专著（钱理群《三十年》/洪子诚《当代文学史》/夏志清《中国现代小说史》）。链接优先开放获取来源（中国作家网/社科院文学所/中国文艺评论网）。

## 四、功能设计

### 4.1 功能一：知识点详情页嵌入"相关论述题"区块（G1）

**位置**：`KnowledgePointDetailScreen`，在 `RelatedPointsSection`（关联知识点）之后、`WrongAnswersSection`（错题）之前新增 `RelatedEssaysSection`。

**交互**：
- 默认折叠（`ExpandableInfoItem`，v0.9.6 已实现），标题"相关论述题（N）"
- 展开后列出关联的 ESSAY 真题：年份 + 试卷代码 + 题目摘要（前 60 字）+ 分值 chip
- 点击某题 → 跳转论述题详情子路由 `essay_detail/{examQuestionId}`

**数据流**：
```
KnowledgePointDetailViewModel
  └─ KnowledgeRepository.observeRelatedEssays(pointId)  // 新增
       └─ ExamQuestionDao.observeEssayByRelatedPoint(pointId)  // 新增
            └─ SELECT * FROM exam_questions
               WHERE question_type = 'ESSAY'
               AND related_point_ids LIKE '%pointId%'  // 内存过滤更优（134题）
```

### 4.2 功能二：论述题详情页（G2 + G3 + G4 + G5）—— 核心

**新子路由**：`essay_detail/{examQuestionId}`（Push/Pop slide，与 `about` 子路由同模式）

**页面结构**（从上到下）：

| 区块 | 内容 | 数据来源 |
|------|------|------|
| 1. 题目卡 | 年份 + 试卷代码 + 科目 + 分值 + 题目正文 | `ExamQuestionEntity` |
| 2. 审题思路（可折叠） | 题型识别 + 核心词 + 限定词 + 任务 | `angle` JSON → `questionType`/`coreKeywords`/`task` |
| 3. 破题角度（可折叠） | 切入维度 + 选择理由 | `angle` JSON → `breakthroughAngles`/`angleRationale` |
| 4. 论证路径（可折叠） | 总述 + 分论点列表 + 总结 | `angle` JSON → `argumentPath` |
| 5. 答题框架 | 现有 `answer_framework` | `ExamQuestionEntity.answerFramework` |
| 6. 依据列表 | 作品原文 + 学者观点 + 教材定论（每条带来源） | `notes` JSON → `evidences` |
| 7. 交叉验证 | 多教材对照 + 多学者对照 | `notes` JSON → `crossValidation` |
| 8. 参考链接 | 可点击的外部链接 | `notes` JSON → `referenceLinks` |
| 9. 关联知识点 | 跳转知识点详情 | `related_point_ids` |
| 10. AI 引导入口 | "让 AI 帮我分析"按钮 → 跳转 AI 助手（预填上下文） | `SocraticTutor.guideEssayAnswer` |

**默认展开策略**：区块 1（题目）+ 5（答题框架）默认展开；2/3/4（思路）默认折叠，用户按需深入；6/7/8（依据/交叉验证/链接）默认展开（用户核心诉求）。

### 4.3 功能三：真题页题型筛选（增强）

**改动**：`QuizScreen` 顶部新增 `FilterChip` 行：
- 全部 / 名词解释 / 简答 / **论述** / 写作
- 默认"全部"，点击"论述"只看 134 道论述题
- `QuizViewModel` 新增 `selectedQuestionType: StateFlow<String?>` 状态

**点击论述题** → 跳转 `essay_detail/{examQuestionId}`（而非当前的展开答题框架），提供更深度的学习体验。

### 4.4 功能四：论述题答题 + AI 引导闭环（G5）

**流程**：
1. 用户在论述题详情页点击"开始练习"
2. 展开答题区（TextField，5000 字上限，复用 QuizViewModel 逻辑）
3. 用户提交答案
4. 触发 `SocraticTutor.guideEssayAnswer(question, userAnswer)` 三阶段引导：
   - **ANALYZE**：分析论证漏洞（不直接给答案）
   - **SUGGEST**：提供方向性建议
   - **SHOW_SAMPLE**：生成 500–800 字参考范文（标注"范文，非标准答案"）
5. 用户自评（AGAIN/GOOD/EASY）→ 答错记录到错题本（复用 `wrong_answers` 表）
6. AI 批改结果持久化到 `ai_grading_records` 表（Phase 2 接入）

## 五、"依据 + 交叉验证 + 思路"的具体实现（用户核心诉求）

这是用户最强调的部分，单独详述。

### 5.1 三层信息架构（来自调研报告第九章）

```
每道论述题
├── 思路（怎么想）  ← angle 字段
│   ├── 审题思路：题型/核心词/限定词/任务
│   ├── 破题角度：切入维度 + 选择理由
│   └── 论证路径：总述 → 分论点 → 总结
├── 依据（凭什么这么说）  ← notes.evidences
│   ├── 作品原文（引号 + 篇章）
│   ├── 学者观点（学者 + 著作 + 页码）
│   └── 教材定论（教材 + 章节 + 页码）
└── 交叉验证（别人怎么说）  ← notes.crossValidation + referenceLinks
    ├── 多教材对照（共识 + 分歧）
    ├── 多学者对照（不同视角）
    └── 参考链接（可点击的外部资源）
```

### 5.2 UI 呈现规范

- **依据列表**：每条依据用 `GroupedCardItem`，左侧图标区分类型（`MenuBook`=作品 / `Person`=学者 / `School`=教材），右侧来源标签
- **交叉验证**：用对照表（`LazyRow` + 双列卡片），左列教材 A 观点，右列教材 B 观点，底部标注"共识/分歧"
- **参考链接**：` AssistChip` 列表，点击用隐式 Intent 打开浏览器（`Intent.ACTION_VIEW`）
- **思路**：用 `ExpandableInfoItem`（v0.9.6 组件）三层折叠，默认只显示"审题思路"，破题角度和论证路径按需展开

### 5.3 数据质量保障

| 风险 | 对策 |
|------|------|
| AI 生成依据编造页码 | 限定 LLM 只引用 4 部官方教材 + 3 部公认专著，页码留空（"第 X 页"）待人工补 |
| 学者观点张冠李戴 | 抽样 20 题人工校验，重点核对学者归属 |
| 链接失效 | 优先开放获取来源（中国作家网/社科院/中国文艺评论网），避免知网订阅链接 |
| 交叉验证不客观 | 每题至少 2 部教材对照 + 2 位学者对照，标注共识与分歧 |

## 六、实施计划（分 Phase）

### Phase 1：知识点详情页嵌入相关论述题（最小可用）

**目标**：打通知识点↔论述题双向关联，用户从知识点能跳到相关论述题。

| 任务 | 文件 | 说明 |
|------|------|------|
| T1.1 数据补全 | `SeedDataLoader.kt` | 新增 `computeExamQuestionRelatedPoints()`，派生 `related_point_ids` |
| T1.2 seed 升级 | `seed_data.json` | 版本 2.13.1 → 2.14.0，触发重新导入 |
| T1.3 Repository | `KnowledgeRepository.kt` | 新增 `observeRelatedEssays(pointId): Flow<List<ExamQuestionEntity>>` |
| T1.4 DAO | `ExamQuestionDao.kt` | 新增 `observeEssayByRelatedPoint(pointId)`（查 ESSAY + 内存过滤） |
| T1.5 UI | `KnowledgePointDetailScreen.kt` | 新增 `RelatedEssaysSection`（参考 `WrongAnswersSection`） |
| T1.6 ViewModel | `KnowledgePointDetailViewModel.kt` | `UiState` 新增 `relatedEssays: List<ExamQuestionEntity>` |
| T1.7 导航 | `WenyanNavHost.kt` | 新增 `essay_detail/{examQuestionId}` 子路由 |
| T1.8 测试 | `KnowledgePointDetailViewModelTest.kt` | +3 测试（空/有关联/跳转） |

### Phase 2：论述题详情页（思路 + 依据 + 交叉验证）

**目标**：用户核心诉求——每题给思路、依据、交叉验证链接。

| 任务 | 文件 | 说明 |
|------|------|------|
| T2.1 数据生成 | `tools/generate_essay_metadata.py` | LLM 批量生成 134 题的 `angle` + `notes` JSON |
| T2.2 数据校验 | 人工 | 抽样 20 题校验依据真实性、链接可点击 |
| T2.3 seed 写入 | `seed_data.json` | 写入 `angle` + `notes` 字段 |
| T2.4 UI | 新建 `EssayDetailScreen.kt` | 10 区块结构（见 4.2） |
| T2.5 ViewModel | 新建 `EssayDetailViewModel.kt` | 加载真题 + 解析 angle/notes JSON |
| T2.6 组件 | 新建 `EvidenceList.kt` / `CrossValidationCard.kt` / `ThoughtPathSection.kt` | 依据列表/交叉验证卡/思路路径 |
| T2.7 测试 | `EssayDetailViewModelTest.kt` | JSON 解析 + 空数据降级 |

### Phase 3：真题页题型筛选 + 答题闭环

**目标**：真题页可筛论述题，答题后接入 AI 三阶段引导。

| 任务 | 文件 | 说明 |
|------|------|------|
| T3.1 题型筛选 | `QuizScreen.kt` / `QuizViewModel.kt` | 新增 FilterChip + `selectedQuestionType` 状态 |
| T3.2 答题跳转 | `QuizScreen.kt` | 点击论述题 → `essay_detail` 子路由 |
| T3.3 答题区 | `EssayDetailScreen.kt` | "开始练习"展开 TextField + 提交按钮 |
| T3.4 AI 引导 | `EssayDetailViewModel.kt` | 接入 `SocraticTutor.guideEssayAnswer`，三阶段流式展示 |
| T3.5 错题记录 | `WrongAnswerRepository.kt` | 复用 `recordWrongAnswer(examQuestionId, ...)` |
| T3.6 测试 | `QuizViewModelTest.kt` | 题型筛选 + 答题流程 |

### Phase 4（可选）：AI 批改持久化 + 范文库

| 任务 | 说明 |
|------|------|
| T4.1 接入 `ai_grading_records` 表 | 持久化 AI 批改结果 |
| T4.2 范文库 | 接入 `writing_materials`（category=ESSAY） |
| T4.3 写作句式 | 接入 `writing_patterns`（开头/论证/结尾句式） |

## 七、风险与对策

| # | 风险 | 等级 | 对策 |
|---|------|------|------|
| R1 | AI 生成依据编造页码/学者 | 高 | 限定引用范围 + 人工抽样校验 20 题 + 页码留空待补 |
| R2 | `related_point_ids` JSON 数组查询性能 | 低 | 134 题内存过滤 < 5ms，无需建关联表 |
| R3 | 知识点详情页过长（已有 6 区块 +1） | 中 | `RelatedEssaysSection` 默认折叠 + 子路由跳转 |
| R4 | `angle`/`notes` JSON 字段容量 | 低 | 单题 JSON < 2KB，SQLite TEXT 无限制 |
| R5 | 外部链接失效 | 中 | 优先开放获取来源 + 定期巡检（可接入 auto-check） |
| R6 | seed 数据体积膨胀 | 低 | 134 题 × 2KB ≈ 270KB，可接受 |
| R7 | 南师大阅卷特点数据不足 | 中 | 调研报告已标注局限，后续通过校友网络补充 |

## 八、验收标准

### Phase 1 验收
- [ ] 知识点详情页显示"相关论述题（N）"区块，N 准确
- [ ] 点击论述题跳转 `essay_detail` 子路由
- [ ] `related_point_ids` 派生准确（抽样 10 题核对）
- [ ] 全模块 `testDebugUnitTest` 全绿

### Phase 2 验收
- [ ] 论述题详情页展示 10 区块，默认展开策略正确
- [ ] 思路 JSON 正确解析（题型/核心词/论证路径）
- [ ] 依据列表含 3 类依据（作品/学者/教材），来源真实
- [ ] 交叉验证含 2+ 教材对照 + 2+ 学者对照
- [ ] 参考链接可点击跳转浏览器
- [ ] 抽样 20 题人工校验通过

### Phase 3 验收
- [ ] 真题页"论述"FilterChip 筛选准确（134 题）
- [ ] 答题 → AI 三阶段引导流式展示
- [ ] 答错记录到错题本，可在错题本复习
- [ ] AI 引导失败有降级提示

## 九、与现有架构的兼容性

- **不新建表**：复用 `exam_questions` + `wrong_answers` + `ai_grading_records`
- **不新增顶级 Tab**：论述题详情页是子路由（Push/Pop slide）
- **不破坏现有外键**：`wrong_answers.exam_question_id` / `ai_grading_records.exam_question_id` 保持不变
- **不新增依赖**：所有功能用现有技术栈（Compose / Room / Hilt / coroutines）
- **seed 向前兼容**：新增字段（`angle`/`notes` 填充）对旧版本 App 透明（字段已存在，旧版读取为 null 不影响）

## 十、下一步

本设计方案待用户审批后，按 Phase 1 → 2 → 3 顺序实施。建议从 **Phase 1**（知识点详情页嵌入相关论述题）开始，这是最小可用版本，能快速验证"知识点↔论述题"双向关联的体验，为后续深度功能打基础。

---

**相关文档**：
- 调研报告：[docs/research/essay-answer-methodology.md](../research/essay-answer-methodology.md)
- 项目结构调研：见本文件第二章
- 现有知识点详情页：[KnowledgePointDetailScreen.kt](../../feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgePointDetailScreen.kt)
- 真题 Entity：[ExamQuestionEntity.kt](../../core/database/src/main/java/com/wenyan/app/core/database/entity/ExamQuestionEntity.kt)
- AI 引导：[SocraticTutor.kt](../../core/ai/src/main/java/com/wenyan/app/core/ai/SocraticTutor.kt)
