# 文研App 卡片备考系统设计方案（v0.9.29）

> 日期：2026-08-04
> 状态：设计（待实现）
> 需求：用户担心 6000+ 张卡片背不完，要求"每日新卡限额（默认 60 可设）+ 考频筛选 + 科目筛选 + 考试倒计时计划"，并要求调研精进。

## 一、调研结论（Anki/FSRS 最佳实践 + 考研背诵方法）

1. **每日新卡限额是间隔重复软件的标配**（Anki "New cards/day"）——防止新卡堆积导致焦虑，FSRS 自动排期
2. **FSRS 比传统算法减少 20-30% 复习量**（同样记忆保持率）——App 已用 FSRS，天然受益
3. **复习/新卡比例保护**：当日复习量超过上限时应暂停新卡（Anki review limit 规则），避免"欠债"滚雪球
4. **考研背诵方法**：按每天背诵量拆解计划（6200 卡 ÷ 剩余天数 = 每日目标）；真题驱动（考频优先）
5. **最优记忆保持率**（高级可选项）：85% vs 90% retention 复习量差异显著，本期不做，留作扩展

## 二、数据模型（CardSettings）

用 DataStore Preferences（仿 ThemeRepositoryImpl 模式），存 `core/data` 层：

| 字段 | 键 | 类型 | 默认值 | 说明 |
|------|----|------|--------|------|
| dailyNewLimit | card_daily_new_limit | Int | **60** | 每日新卡数（用户可设 10-200） |
| frequencyFilter | card_frequency_filter | String | **HIGH_MEDIUM** | HIGH / HIGH_MEDIUM / ALL |
| subjectFilters | card_subject_filters | Set<String> | 全选 | 古代/现当代/外国/理论 |
| examDate | card_exam_date | Long | 当年 12/24 | 考试日期（毫秒时间戳） |

- 枚举非法值 runCatching 容错降级默认（仿 ThemeRepositoryImpl）
- Flow 加 .catch 降级，避免设置损坏导致崩溃

## 三、数据层改动

### ReviewRepository.getReviewQueue 改造

当前：`observeVerifiedForReview` + `observeDue` → 只返回到期知识点。

改为：返回"今日学习队列" = **到期复习知识点 ∪ 每日新卡知识点**（去重，复习优先）：

```
1. duePoints = observeVerifiedForReview ∩ observeDue（到期复习，现有）
2. newPoints  = VERIFIED ∧ 无 memo_record ∧ 符合考频筛选 ∧ 符合科目筛选
               （考频 HIGH 优先 → MEDIUM → LOW；按 updated_at 排序稳定）
3. newPoints 截断：累计卡片数 ≤ dailyNewLimit（取整到知识点，60 张 ≈ 10 个知识点）
4. result = duePoints + newPoints（复习优先，新卡在后）
```

- 限额按"卡片数"（用户理解的"张"），取整到知识点保证 sibling 完整
- 已学过（有 memo_record）的知识点不再作为新卡
- 考频/科目筛选作用于新卡（复习卡是已学过的，按 FSRS 调度即可，不重复筛选）

### 新增 DAO 查询

KnowledgePointDao 增加（或内存过滤）：
- `observeNewCandidates(subjects, frequencies)`：VERIFIED + 无 memo_record + 科目/考频过滤
  - 实现：`SELECT kp.* FROM knowledge_points kp LEFT JOIN memo_records mr ON kp.id=mr.point_id WHERE kp.ocr_status='VERIFIED' AND mr.point_id IS NULL AND kp.exam_frequency IN (...) AND kp.chapter_id IN (SELECT id FROM chapters WHERE subject_id IN (...))`
- 或复用现有 `observeVerifiedForReview` 内存过滤（数据量 960，内存过滤可接受，避免复杂 SQL）

## 四、UI 改动

### CardsScreen 顶部"今日任务"区（新增）

```
┌─────────────────────────────┐
│ 📅 距考试 142 天             │
│ 今日：新卡 60 · 复习 85      │
│ ████████░░░░░░ 已学 35%     │
│ [开始今日学习]               │
└─────────────────────────────┘
```

- 新卡/复习数：从队列实时计算
- 剩余天数：examDate - today
- 进度：已学知识点（memo_record 去重）/ 总 VERIFIED 知识点
- 点击开始 → 进入现有复习流程

### SettingsScreen"卡片备考"分组（新增）

- 每日新卡数：滑杆 10-200（默认 60），显示当前值
- 考频筛选：三个单选（仅高频 HIGH / 高频+中频 HIGH_MEDIUM / 全部 ALL）
- 科目：四科多选 Checkbox
- 考试日期：日期选择器（默认当年 12/24），保存时间戳

## 五、测试计划

1. CardSettingsRepositoryTest：默认值 / 读写 / 非法值容错（仿 ThemeRepositoryImpl 测试风格）
2. ReviewQueueTest（新增）：新卡限额取整知识点 / 考频优先 / 科目过滤 / 复习优先去重
3. TodayPlanTest（纯函数）：剩余天数 / 进度 / 每日推荐量计算
4. 全量单测 0 失败 + assembleDebug + assembleRelease(R8)

## 六、实施顺序

1. CardSettingsRepository + 测试（Task 85）
2. ReviewRepository/CardRepository 队列改造 + DAO + 测试（Task 86）
3. CardsScreen 今日任务 UI + ViewModel（Task 87）
4. SettingsScreen 配置 UI（Task 88）
5. 单测 + 构建 + CHANGELOG + 发 v0.9.29（Task 89）

## 七、风险与缓解

| 风险 | 缓解 |
|------|------|
| SQL 复杂度（新卡查询） | 优先内存过滤（960 条数据量小），SQL 优化留后续 |
| 新卡"取整知识点"可能超限额少量 | 60 张限额容忍 ±6 张（一个知识点），UI 显示实际值 |
| 设置页 UI 工作量大 | 复用现有 SettingsScreen 分组模式 + 设计系统组件 |
| 与现有 FSRS 调度冲突 | 新卡首次评分仍走 FSRS（AGAIN→1min/GOOD→6d 等），行为不变 |
