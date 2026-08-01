# 知识卡片"加入错题本"按钮 — 实施计划

> **文档状态**：v1.0 调研完成，等待用户确认后实施
> **调研范围**：CardsScreen/CardsViewModel/CardItem + WrongAnswerEntity/Repository/Dao/ViewModel + SchedulingRepository
> **涉及文件**：4 个（feature/cards 2 个 + core/data 1 个 + core/database 1 个）

---

## 1. 问题分析

### 1.1 当前行为

| 评分 | 是否记录错题 | 来源 |
|------|------------|------|
| AGAIN（不会） | ✅ 自动记入 | `SOURCE_CARD_AGAIN` |
| HARD（困难） | ❌ 不记 | — |
| GOOD（良好） | ❌ 不记 | — |
| EASY（简单） | ❌ 不记 | — |
| Skip（跳过） | ❌ 不记 | — |

### 1.2 用户需求

用户希望在任何评分之后（或之前），都能手动将当前卡片加入错题本，而不受 AGAIN 评分的限制。典型场景：

- **场景 A**：评了 GOOD 但觉得不扎实，想加入错题本后续重点复习
- **场景 B**：Skip 了一张卡，但觉得内容有价值，想标记为待复习
- **场景 C**：评了 AGAIN（已自动记入），但想确认状态或再次强调

### 1.3 现有基础设施（调研结论）

| 层 | 组件 | 状态 | 说明 |
|----|------|------|------|
| 数据层 | `WrongAnswerEntity` | ✅ 完备 | 含 pointId/examQuestionId/userAnswer/correctAnswer/source/wrongCount 等字段 |
| 数据层 | `WrongAnswerDao` | ✅ 完备 | `findUnresolvedByPointAndSource` 按 pointId+source 去重，`incrementWrongCount` 递增 |
| 仓库层 | `WrongAnswerRepository` | ✅ 完备 | `recordWrongAnswer(pointId, examQuestionId, userAnswer, correctAnswer, source)` 接口，含去重逻辑 |
| 仓库层 | `WrongAnswerRepositoryImpl` | ✅ 完备 | 已有 CARD_AGAIN/QUIZ_WRONG/ESSAY_PRACTICE 三种来源，去重+递增逻辑稳定 |
| ViewModel | `CardsViewModel` | ✅ 已注入 `wrongAnswerRepository` | 可直接调用 `recordWrongAnswer()` |
| UI | `CardsScreen` | ✅ 已有 SnackbarHost | 错误消息可直接复用 |

---

## 2. 设计方案

### 2.1 新增来源常量

在 `WrongAnswerRepository` companion 中新增：

```kotlin
const val SOURCE_CARD_MANUAL = "CARD_MANUAL"
```

**理由**：与 AGAIN 自动记录区分，独立的 source 值使：
- 错题本可显示"手动加入"vs"评分AGAIN"不同来源
- 去重逻辑按 pointId + source=CARD_MANUAL 匹配，互不干扰
- 同一张卡既被 AGAIN 记录又被手动加入时，产生两条独立记录（各有 wrongCount 统计）

### 2.2 UI 按钮位置

**方案**：翻转后，在 RatingButtons 与 Undo/Skip 之间，插入一个全宽 `FilledTonalButton`

```
┌──────────────────────────────┐
│         ProgressSection       │
│          3 / 12 ████░░░░      │
├──────────────────────────────┤
│                              │
│         FlipCard             │
│     (正面问题 / 背面答案)      │
│                              │
├──────────────────────────────┤
│  [AGAIN] [HARD] [GOOD] [EASY]│  ← RatingButtons
├──────────────────────────────┤
│  📑 加入错题本                │  ← NEW: AddToWrongAnswerButton
├──────────────────────────────┤
│  [↩ 撤销]    [⏭ 跳过]       │  ← Undo + Skip
└──────────────────────────────┘
```

**理由**：
- 翻转后用户已看到答案，可判断是否应加入错题本
- 与评分按钮分离（评分是 FSRS 调度，加入错题本是独立需求）
- 全宽按钮视觉上足够醒目，但不会抢评分按钮的 CTA 权重

**翻转前**：同样显示该按钮（用户未看答案时也可主动标记）

```
┌──────────────────────────────┐
│         ProgressSection       │
│          3 / 12 ████░░░░      │
├──────────────────────────────┤
│         FlipCard             │
│        (正面问题，未翻转)       │
├──────────────────────────────┤
│     📑 加入错题本              │  ← NEW: 翻转前也可用
├──────────────────────────────┤
│  [↩ 撤销]    [⏭ 跳过]       │
└──────────────────────────────┘
```

### 2.3 按钮 UI 设计

| 状态 | 图标 | 文案 | 交互 |
|------|------|------|------|
| 默认 | `BookmarkAdd` / `BookmarkBorder` | "加入错题本" | 可点击，调用 `addToWrongAnswerBook()` |
| 已加入（本会话） | `Bookmark` / `CheckCircle` | "已加入错题本" | 禁用，展示已加入状态 |
| 加载中 | — | "加入中..." | 禁用，防重复点击 |
| 无 pointId | — | "无法加入错题本" | 禁用，兜底（理论上不会出现） |

**颜色方案**：
- 默认：`FilledTonalButton` + `secondaryContainer` 色
- 已加入：`FilledTonalButton` 禁用态 + `outline` 色, 透明度降低

### 2.4 数据流

```
用户点击"加入错题本"
  │
  ▼
CardsViewModel.addToWrongAnswerBook()
  │
  ├─ 1. 读取当前卡片 (sessionCards[_currentIndex])
  │     ├─ pointId       → 传参
  │     ├─ correctAnswer → extractCorrectAnswer(card) 复用现有方法
  │     └─ userAnswer    → "（手动加入错题本）"
  │
  ├─ 2. 检查 pointId 是否为空
  │     └─ 空 → showSnackbar("无法加入错题本：知识点关联缺失") + return
  │
  ├─ 3. 检查 pointId 是否已在 manualAddedPointIds 中
  │     └─ 是 → 已加入状态，不重复调用
  │
  ├─ 4. 调用 wrongAnswerRepository.recordWrongAnswer(
  │       pointId = pointId,
  │       examQuestionId = null,
  │       userAnswer = "（手动加入错题本）",
  │       correctAnswer = extractCorrectAnswer(current),
  │       source = SOURCE_CARD_MANUAL,
  │     )
  │     │
  │     ├─ ✅ 成功 → manualAddedPointIds.add(pointId) → UI 更新为"已加入"
  │     └─ ❌ 失败 → _errorMessage 设置 → Snackbar 显示
  │
  └─ 5. sessionReviewedCount 不变（不影响会话统计）
```

### 2.5 状态管理

在 `CardsViewModel` 中新增：

```kotlin
/** 本会话中手动加入错题本的 pointId 集合 */
private val manualAddedPointIds = mutableSetOf<String>()

/** 当前卡片是否已在错题本中（手动加入） */
val isCurrentCardInWrongBook: StateFlow<Boolean> = _uiState
    .map { state ->
        val card = state.currentCard ?: return@map false
        card.pointId.isNotBlank() && card.pointId in manualAddedPointIds
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)
```

**设计决策**：
- 使用 `mutableSetOf<String>()` 仅跟踪本次会话的手动加入记录
- 不查询数据库检查历史记录（避免每次卡片切换都触发 DB 查询）
- 若用户之前已通过 AGAIN 加入过，再次手动加入仍会触发（不同 source，将被视为独立记录）
- `retry()` 时不清空 `manualAddedPointIds`（保持跨轮次的一致性）

### 2.6 边界情况

| 场景 | 行为 |
|------|------|
| pointId 为空 | 显示 Snackbar "无法加入错题本：知识点关联缺失"，不记录 |
| 本会话已手动加入过 | 按钮显示"已加入错题本"禁用态，用户在错题本中可看到 wrongCount 递增 |
| 同一卡多次点击 | 第一次点击后按钮变"已加入"，后续不可点击（防重复） |
| 进程被杀后恢复 | `manualAddedPointIds` 丢失（内存），按钮恢复默认态。用户可再次点击，DB 会正确递增 wrongCount |
| AGAIN 已自动记录 + 手动加入 | 两条独立记录（source 不同 CARD_AGAIN vs CARD_MANUAL），互不干扰 |
| 错题本已存在该 pointId 的记录（已解决） | 视为新记录 upsert（resolvedAt 非空时 findUnresolvedByPointAndSource 返回 null） |
| 快速连续点击 | 状态立即变为"加入中..."（isAddingBookmark），防重入 |

---

## 3. 修改清单

### 3.1 `WrongAnswerRepository.kt` — 新增来源常量

```kotlin
// 在 companion object 中新增
const val SOURCE_CARD_MANUAL = "CARD_MANUAL"
```

### 3.2 `CardsViewModel.kt` — 新增方法 + 状态

新增：
1. `manualAddedPointIds: MutableSet<String>` — 会话级跟踪
2. `isCurrentCardInWrongBook: StateFlow<Boolean>` — UI 状态
3. `_isAddingBookmark: MutableStateFlow<Boolean>` — 防重入锁
4. `fun addToWrongAnswerBook()` — 核心方法

### 3.3 `CardsScreen.kt` — 新增 UI 按钮

1. 新增 `@Composable AddToWrongAnswerButton(...)` — 按钮组件
2. 在 `CardReviewContent` 的翻转后区域插入按钮（RatingButtons 与 Undo/Skip 之间）
3. 在翻转前区域也插入按钮（在"点击卡片查看答案"下方）
4. 收集 `isCurrentCardInWrongBook` + `isAddingBookmark` 状态驱动按钮外观

### 3.4 `WrongAnswerRepository.kt` — 更新文档注释

在 `WrongAnswerRepository` 接口注释中补充 `SOURCE_CARD_MANUAL` 来源说明。

---

## 4. 测试影响

### 4.1 新增测试

| 测试 | 类型 | 说明 |
|------|------|------|
| `addToWrongAnswerBook` 成功记录 | ViewModel 单元测试 | 验证 `recordWrongAnswer` 被调用且参数正确 |
| `addToWrongAnswerBook` 重复调用去重 | ViewModel 单元测试 | 同一卡第二次调用不触发 record |
| `addToWrongAnswerBook` pointId 为空 | ViewModel 单元测试 | 不触发 record，设 errorMessage |
| `addToWrongAnswerBook` 失败 | ViewModel 单元测试 | record 抛异常时设 errorMessage |
| `isCurrentCardInWrongBook` 状态变化 | ViewModel 单元测试 | 加入前 false，加入后 true |
| 无 pointId 卡按钮禁用 | UI 测试 | 不渲染按钮或显示禁用态 |

### 4.2 现有测试不受影响

- `CardsViewModelTest` 现有 21 个测试：不涉及新方法，无需修改
- 现有 `WrongAnswerRepositoryImplTest`：不涉及新 source，无需修改

---

## 5. 实施步骤

### Phase 1: 数据层（1 文件，~5 行）

1. **`WrongAnswerRepository.kt`**：在 `companion object` 中新增 `SOURCE_CARD_MANUAL = "CARD_MANUAL"`

### Phase 2: ViewModel 层（1 文件，~50 行）

2. **`CardsViewModel.kt`**：
   - 新增 `manualAddedPointIds: MutableSet<String>`
   - 新增 `isCurrentCardInWrongBook: StateFlow<Boolean>`
   - 新增 `_isAddingBookmark: MutableStateFlow<Boolean>`
   - 实现 `addToWrongAnswerBook()` 方法

### Phase 3: UI 层（1 文件，~80 行）

3. **`CardsScreen.kt`**：
   - 新增 `AddToWrongAnswerButton` Composable 组件
   - 在 `CardReviewContent` 翻转后区域插入按钮
   - 在翻转前区域也插入按钮
   - 连接 ViewModel 状态

### Phase 4: 测试（~5 个新测试）

4. 在 `CardsViewModelTest` 中新增上述 5 个测试

### Phase 5: 验证

5. 本地验证：`:app:assembleDebug` + `:feature:cards:testDebugUnitTest` 全绿

---

## 6. 风险与注意事项

### 6.1 风险

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| `BookmarkAdd` 图标在 Material Icons 中不存在 | 低 | 降级为 `BookmarkBorder` + `Bookmark` |
| 翻转前按钮与"点击卡片查看答案"文案视觉冲突 | 低 | 按钮放在提示文案下方，用 `Spacing.sm` 分隔 |
| 翻转后按钮与评分按钮的视觉层次冲突 | 低 | 用 `FilledTonalButton`（非 filled），`secondaryContainer` 色，视觉权重低于评分按钮 |

### 6.2 注意事项

- **不修改 sessionReviewedCount/sessionAgainCount**：加错题本是独立操作，不影响会话统计
- **不修改 ratedPointIds**：加错题本不涉及 FSRS 调度
- **不涉及 DB Migration**：仅使用现有表结构和字段，新增 source 常量是纯 Kotlin 常量
- **undo 时不清除 manualAddedPointIds**：undo 是 UI 回退，已加入错题本的记录不回退（符合用户预期）
- **retry 时保留 manualAddedPointIds**：重开一轮后已加入的卡仍标记为"已加入"

---

## 7. 用户确认清单

在实施前，请确认以下决策：

- [ ] **按钮位置**：翻转后，在评分按钮下方、撤销/跳过上方（翻转前也在"点击卡片查看答案"下方）— 是否满意？
- [ ] **来源常量**：使用 `CARD_MANUAL` 与 AGAIN 自动记录区分 — 是否满意？
- [ ] **用户答案文本**："（手动加入错题本）" — 是否有更好的文案？
- [ ] **已加入状态**：本会话内同一卡按钮变为"已加入错题本"禁用态 — 是否满意？
- [ ] **图标选择**：`BookmarkAdd`（或 `BookmarkBorder`）— 是否满意？