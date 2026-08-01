# 知识卡片"加入错题本"按钮 — 实施计划 v11.0

> **文档状态**：v11.0 staff-engineer-mode 十五维度深度审查（v6.0 四维度 + v7.0 三维度 + v8.0 二维度 + v9.0 三维度 + v10.0 input-validation-and-injection-defense / privacy-and-data-lifecycle 二维度 + v11.0 data-contracts 单维度），新增 2 项 P1 数据契约缺陷 + 3 项 P2 数据契约缺陷 + 1 项 P3 优化建议
>   - 🟢 P2-14: `AddToWrongAnswerButton` 缺少 Snackbar 焦点管理 — 屏幕阅读器用户无法感知操作结果（详见 §10.1）
>   - 🟢 P2-15: 加载态 (`CircularProgressIndicator`) 未检查 `prefers-reduced-motion` — 系统动画关闭后旋转动画仍运行（详见 §10.1）
>   - 🟢 P2-16: 翻转前/后按钮重复导致屏幕阅读器焦点混乱 — 两个 `AddToWrongAnswerButton` 同屏存在，TalkBack 用户会连续听到两次相同描述（详见 §10.1）
>   - 🟢 P2-17: 完成态"手动加入错题本"统计行缺少 `mergeDescendants` — 屏幕阅读器分别读出图标和文本，而非合并语义（详见 §10.1）
>   - 🟢 P2-18: `userAnswer` 拼接 `card.front` 无长度上限 — 论述题卡 front 可达 5000+ 字符，可能超出 Room TEXT 合理范围（详见 §10.2）
>   - 🟢 P2-19: `manualAddedPointIds` 逗号分隔序列化缺少格式校验 — 若未来 pointId 格式引入逗号，序列化/反序列化会静默出错（详见 §10.2）
>   - 🟢 P2-20: Compose UI 测试依赖未在 `feature/cards/build.gradle.kts` 修改清单中明确列出 — `CardsAddToWrongAnswerButtonTest` 需要 `debugImplementation` 依赖（详见 §10.3）
>   - 🟢 P2-21: `AddToWrongAnswerButton` Composable 可见性未在计划中明确 — 测试文件需引用该函数，若为 `private` 则编译失败（详见 §10.3）
>   - 🟢 P2-22: `Timber.w` 日志中 `front.take(20)` 泄露用户学习内容 — WARN 级别日志通常会上报，可能包含用户数据（详见 §10.2）
>   - 🔵 P3-4: 完成态统计行缺少无障碍语义合并 — 见 P2-17 长期方案（详见 §10.1）
>   - 🔵 P3-5: 翻转前/后 `AddToWrongAnswerButton` 代码重复 — 可提取为单一 Composable 插槽（详见 §10.3）
>   - 🔵 P3-6: Snackbar 持续时间 `SnackbarDuration.Short` 对屏幕阅读器用户不足 — 建议改为 `Indefinite` + 手动关闭（详见 §10.1）
>   - 🟢 P2-23: `front` 文本未做控制字符过滤 — 不可见字符（`\u0000`-`\u001F`、`\u007F`）可能影响 UI 渲染或日志解析（详见 §11.1）
>   - 🟢 P2-24: `correctAnswer` 未做长度限制 — `extractCorrectAnswer(card)` 可能返回大段论述题答案文本，导致 `WrongAnswerEntity` 记录存储过大（详见 §11.1）
>   - 🟢 P2-25: `extractCorrectAnswer` 返回值未做 null/空安全兜底 — 若返回空字符串，错题本中显示空白答案，用户体验差（详见 §11.1）
>   - 🟢 P2-26: 缺少用户数据删除机制文档化 — 错题本记录可"解决"但不可"删除"，用户无法完全清除手动加入的记录（详见 §11.2）
>   - 🟢 P2-27: `sessionManualAddCount` 持久化值无上限校验 — 极端情况完成态 UI 显示不合理数值（详见 §11.2）
>   - 🔵 P3-7: `front.take(200)` 截断在多字节字符场景下省略号显示可能不美观 — 建议使用 `TextUtils.ellipsize` 或明确省略号策略（详见 §11.1）
>   - 🔵 P3-8: 错题本数据生命周期文档缺失 — 无 retention 策略说明，用户无法预期数据保留时间（详见 §11.2）
> **调研范围**：v7.0 范围 + WrongAnswerRepository.kt 接口契约审查 + WrongAnswerRepositoryImpl.kt 实现审查 + WrongAnswerDao.kt 事务边界审查 + WrongAnswerEntity.kt 索引设计审查 + CardsViewModel.kt 架构模式审查 + CardsScreen.kt 日志消费路径审查 + Fakes.kt 测试基础设施审查
> **涉及文件**：8 个（+2 个测试文件）= 8 个源文件 + 2 个测试文件（新增 `WrongAnswerEntity.kt` 索引定义 + `CardsViewModel.kt` 日志新增 + `feature/cards/build.gradle.kts` 测试依赖 + `gradle/libs.versions.toml` Compose 测试依赖声明）
> **v3.0 审计范围**：全量代码审查 + 数据流逐行验证 + 测试覆盖度分析 + 边界情况穷举
> **v4.0 审计范围**：staff-engineer-mode 全量审查，发现 1 项 P0 状态管理缺陷 + 3 项 P1 改进 + 测试覆盖度增强
> **v5.0 审计范围**：state-machine-correctness 专业审查，发现 2 项 P0 缺陷 + 2 项 P1 改进 + 1 项 P1 优化
> **v6.0 审计范围**：staff-engineer-mode 四维度深度审查（testing-and-quality-gates / code-readability-for-agents / dependency-resilience / accessibility-gates），发现 1 项 P0 数据完整性缺陷 + 4 项 P1 改进 + 1 项 P1 测试增强 + 2 项 P2 优化
>   - 🔴 P0-3: `NonCancellable` 作用域过窄 — 状态更新（`manualAddedPointIds`、`savedStateHandle`）在 `NonCancellable` 块外，协程取消导致 DB 已写入但 ViewModel 状态未更新，wrongCount 可被静默递增（数据完整性）
>   - 🟡 P1-4: `clearSuccessMessage()` 方法未定义 — 计划文档 UI 代码引用但 ViewModel 中无实现（现有 `clearError()` 在 L680 但不能复用）
>   - 🟡 P1-5: `isAddingBookmark` 中间态测试薄弱 — 测试自身承认"由于当前测试环境限制"，无法验证 `isAddingBookmark=true` 窗口期
>   - 🟡 P1-6: 缺少进程死亡恢复测试 — SavedStateHandle 持久化 `manualAddedPointIds` 无测试覆盖
>   - 🟡 P1-7: 缺少 `successMessage` 测试 — 独立成功通道无测试覆盖
>   - 🟡 P1-8: 缺少 Compose UI 测试 — `AddToWrongAnswerButton` 无 UI 交互测试（feature/cards 模块无 Compose 测试基础设施，需新增）
>   - 🟢 P2-4: 缺少 `retry()` 时 `successMessage` 清空测试
>   - 🟢 P2-5: `AddToWrongAnswerButton` 新增 `@Preview` 的 `WenyanTheme` 导入路径需确认
> **v7.0 审计范围**：staff-engineer-mode 三维度深度审查（database-operations / performance-and-capacity / api-design-and-compatibility），发现 1 项 P1 数据库事务缺陷 + 1 项 P1 接口文档缺陷 + 1 项 P2 索引优化 + 2 项 P2 注释更新 + 1 项 P3 性能文档
>   - 🟡 P1-9: `WrongAnswerRepositoryImpl.recordWrongAnswer` 缺少 `@Transaction` 事务保护 — read-then-write 模式（`findUnresolvedByPointAndSource` → `incrementWrongCount`/`upsert`）未包裹在 `@Transaction` 中，虽然 Room 单线程执行器使此模式在实践中安全，但应添加 `@Transaction` 确保原子性（详见 §7.1）
>   - 🟡 P1-10: `recordWrongAnswer` KDoc 未更新 `SOURCE_CARD_MANUAL` — `@param source` 文档只列出三个来源，缺少 `CARD_MANUAL`（详见 §7.2）
>   - 🟢 P2-6: 缺少 `(point_id, source)` 及 `(exam_question_id, source)` 复合索引 — `findUnresolvedByPointAndSource` 查询过滤 `point_id + source`，现有单列索引 `point_id` 只能覆盖前半部分条件。新增复合索引提升查询效率（详见 §7.1）
>   - 🟢 P2-7: `WrongAnswerRepository` 接口 KDoc "支持三个来源"需改为"四个来源"（详见 §7.2）
>   - 🟢 P2-8: `WrongAnswerRepositoryImpl` `when` 块注释只提到 CARD_AGAIN/QUIZ_WRONG，需补充 CARD_MANUAL（详见 §7.2）
>   - 🔵 P3-1: 性能文档 — `combine(_uiState, _manualAddedPointIds)` 在每次卡翻转/评分时触发 lambda，但集合查找开销极低，`distinctUntilChanged` 防止无效发射。当前规模无需优化，但记录设计决策供未来参考（详见 §7.3）
> **v8.0 审计范围**：staff-engineer-mode 二维度深度审查（architecture-decisions / observability-and-alerting），发现 1 项 P1 日志缺失 + 1 项 P2 架构契约文档 + 2 项 P2 日志增强 + 2 项 P2 设计决策文档 + 2 项 P3 优化建议
>   - 🟡 P1-11: `addToWrongAnswerBook` 成功操作缺少 `Timber.i` 日志 — 无法追踪"用户手动加入错题本"的使用频率和模式（详见 §8.1）
>   - 🟢 P2-9: `manualAddedPointIds` 序列化格式缺少契约文档 — 逗号分隔假设 pointId 不含逗号，当前 seed data 的 pointId 格式为 `kp_\d{5}` 不含逗号，但这是隐式假设，需在代码中明确记录此契约（详见 §8.2）
>   - 🟢 P2-10: `isCurrentCardInWrongBook` 的 `combine` 模式可优化为 `map` — 当前依赖 `_uiState` + `_manualAddedPointIds` 两个 Flow，但 `_uiState` 变化频繁（每次翻转/评分/切换都发射）。备选方案：`_manualAddedPointIds.map { ... }` 直接读 `_uiState.value.currentCard`，减少 combine 触发频率（详见 §8.2）
>   - 🟢 P2-11: 重复调用（`pointId in _manualAddedPointIds`）和防重入（`_isAddingBookmark.value`）拦截时缺少 `Timber.d` 日志 — 生产排查时无法区分"用户连点"和"代码逻辑重复触发"（详见 §8.1）
>   - 🟢 P2-12: 空 pointId 的 `Timber.w` 需补充卡片 ID 和 front 截断作为上下文 — 当前仅记录 `cardId=${current.id}`，但缺少 front 前 20 字符帮助识别是哪张卡（详见 §8.1）
>   - 🟢 P2-13: `_successMessage` 消费路径（UI → clearSuccessMessage）缺少日志 — 无法追踪 Snackbar 是否被正常消费和清除（详见 §8.1）
>   - 🔵 P3-2: `isCurrentCardInWrongBook` 的 `combine` + `stateIn` 模式可用 `_manualAddedPointIds.map` 替代 — 当前 `combine(_uiState, _manualAddedPointIds)` 在每次 `_uiState` 变化时都执行 lambda，虽然 `distinctUntilChanged` 过滤了发射，但 lambda 执行本身有开销。若未来支持 1000+ 卡会话，可考虑改用 `map` 直接读 `_uiState.value`（详见 §8.2）
>   - 🔵 P3-3: 缺少特征使用统计埋点设计文档 — 建议记录 Analytics 事件设计决策，供后续用户行为分析扩展（详见 §8.1）
> **v9.0 审计范围**：staff-engineer-mode 三维度深度审查（accessibility-gates / client-application-security / dependency-and-code-hygiene），发现 2 项 P2 无障碍缺陷 + 1 项 P2 安全契约 + 1 项 P2 依赖治理 + 1 项 P2 日志审计 + 3 项 P3 优化建议（详见 §10.1-10.3）
> **v10.0 审计范围**：staff-engineer-mode 二维度深度审查（input-validation-and-injection-defense / privacy-and-data-lifecycle），发现 2 项 P2 输入验证缺陷 + 1 项 P2 数据隐私缺陷 + 1 项 P2 数据完整性缺陷 + 3 项 P3 优化建议（详见 §11.1-11.2）

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

用户希望在任何评分之后（或之前），都能手动将当前卡片加入错题本，不受 AGAIN 评分限制。典型场景：

- **场景 A**：评了 GOOD 但觉得不扎实 → 加入错题本后续重点复习
- **场景 B**：Skip 了一张卡，但内容有价值 → 标记为待复习
- **场景 C**：评了 AGAIN（已自动记入）→ 想确认状态或再次强调
- **场景 D**：翻转前就知道这个知识点薄弱 → 预先标记

### 1.3 现有基础设施（调研结论）

| 层 | 组件 | 状态 | 说明 |
|----|------|------|------|
| 数据层 | `WrongAnswerEntity` | ✅ 完备 | 含 pointId/examQuestionId/userAnswer/correctAnswer/source/wrongCount 等字段 |
| 数据层 | `WrongAnswerDao` | ✅ 完备 | `findUnresolvedByPointAndSource` 按 pointId+source 去重，`incrementWrongCount` 递增 |
| 仓库层 | `WrongAnswerRepository` | ✅ 完备 | `recordWrongAnswer(pointId, examQuestionId, userAnswer, correctAnswer, source)` 接口，含去重逻辑 |
| 仓库层 | `WrongAnswerRepositoryImpl` | ✅ 完备 | 已有 CARD_AGAIN/QUIZ_WRONG/ESSAY_PRACTICE 三种来源，去重+递增逻辑稳定 |
| ViewModel | `CardsViewModel` | ✅ 已注入 `wrongAnswerRepository` | 可直接调用 `recordWrongAnswer()` |
| UI | `CardsScreen` | ✅ 已有 SnackbarHost | 错误消息可直接复用 |
| 测试 | `FakeWrongAnswerRepository` | ✅ 完备 | `recordedWrongAnswers` 记录所有调用，可直接断言验证 |

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

**主方案**：翻转后，在 RatingButtons 与 Undo/Skip 之间，插入全宽 `FilledTonalButton`

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

### 2.3 按钮 UI 设计（v4.0 改进：移除无效 animateContentSize + 加载旋转指示器）

```kotlin
@Composable
private fun AddToWrongAnswerButton(
    isInWrongBook: Boolean,
    isLoading: Boolean,
    pointId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when {
        isLoading -> "加入中..."
        isInWrongBook -> "已加入错题本"
        pointId.isBlank() -> "无法加入错题本"
        else -> "加入错题本"
    }
    val icon = when {
        isInWrongBook -> Icons.Default.CheckCircle  // 勾选标记
        else -> Icons.Default.BookmarkBorder         // 书签轮廓
    }
    val enabled = !isLoading && !isInWrongBook && pointId.isNotBlank()

    // v4.0 移除:animateContentSize(原计划 v2.0 添加,但按钮高度固定 heightIn(min=48.dp),
    // 内容变化不影响高度,animateContentSize 不产生实际效果,纯增开销)
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription = when {
                    isInWrongBook -> "当前卡片已加入错题本"
                    isLoading -> "正在加入错题本"
                    pointId.isBlank() -> "无法加入错题本：知识点关联缺失"
                    else -> "加入错题本"
                }
            },
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isInWrongBook) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (isInWrongBook) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
        ),
    ) {
        // v4.0 新增:加载中显示旋转指示器,替代纯文字"加入中..."
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            // 给文字留间距
            Spacer(modifier = Modifier.padding(start = Spacing.xs))
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = Spacing.xs),
            )
        }
        Text(label)
    }
}
```

| 状态 | 图标 | 文案 | 颜色 | 交互 |
|------|------|------|------|------|
| 默认 | `BookmarkBorder` | "加入错题本" | `secondaryContainer` | 可点击 |
| 已加入（本会话） | `CheckCircle` | "已加入错题本" | `surfaceVariant` 禁用 | 禁用 |
| 加入中 | `CircularProgressIndicator` 旋转 | "加入中..." | 禁用态 | 禁用 |
| 无 pointId | — | "无法加入错题本" | 禁用态 | 禁用 |

### 2.4 数据流（v3.0 改进：增强 userAnswer + 修复 isAddingBookmark 公开暴露；v5.0 修复：P0-1 编译错误 + P1-2 协程原子性；v6.0 修复：P0-3 NonCancellable 作用域扩展）

```
用户点击"加入错题本"
  │
  ▼
CardsViewModel.addToWrongAnswerBook()
  │
  ├─ 1. 读取当前卡片 (sessionCards[_currentIndex])
  │     ├─ pointId       → 传参
  │     ├─ correctAnswer → extractCorrectAnswer(card) 复用现有方法
  │     └─ userAnswer    → "手动加入：${card.front.truncated}"  ← v3.0 改进：加入卡片正面文本
  │                         用于错题本中提供上下文，让用户知道具体是哪张卡
  │
  ├─ 2. 检查 pointId 是否为空
  │     └─ 空 → _errorMessage = "无法加入错题本：知识点关联缺失" + Timber.w + return
  │
  ├─ 3. 检查 pointId 是否已在 _manualAddedPointIds 中（防重复）
  │     └─ 是 → return（已有快照，按钮已禁用，无需重复调用）
  │
  ├─ 4. _isAddingBookmark.value = true（加载中，按钮禁用 + 旋转指示器）
  │     └─ 通过 val isAddingBookmark: StateFlow<Boolean> 公开暴露 ← v3.0 修复：原计划漏了 public accessor
  │
  ├─ 5. withContext(Dispatchers.IO + NonCancellable) {  ← v5.0 P1-2 + v6.0 P0-3 修复
  │       // v6.0 P0-3 修复:将 DB 写入 + 状态更新都放在 NonCancellable 块内,
  │       // 防止协程取消导致 DB 写入但状态未更新的数据不一致状态。
  │       // 任何中间取消都会导致整个操作回滚(DB 写入和状态更新都不会发生)。
  │
  │       wrongAnswerRepository.recordWrongAnswer(
  │         pointId = pointId,
  │         examQuestionId = null,
  │         userAnswer = "手动加入：${card.front}",
  │         correctAnswer = extractCorrectAnswer(current),
  │         source = SOURCE_CARD_MANUAL,
  │       )
  │
  │       // v6.0 P0-3:状态更新也放在 NonCancellable 块内,实现原子性
  │       updateManualAddedPointIds(_manualAddedPointIds.value + pointId)  ← 持久化到 SavedStateHandle
  │       savedStateHandle["sessionManualAddCount"] = _sessionManualAddCount.value + 1
  │     }
  │     │
  │     ├─ ✅ 成功 →
  │     │   _isAddingBookmark.value = false
  │     │   _successMessage.value = "已加入错题本"  ← v5.0 P1-1: 独立成功通道,在 NonCancellable 块外
  │     │                                           (UI 反馈丢失可接受,不影响数据完整性)
  │     │
  │     └─ ❌ 失败 →
  │         _isAddingBookmark.value = false
  │         _errorMessage = "错题本记录失败：${e.message}"  ← 现有错误提示机制
  │
  └─ 6. sessionReviewedCount 不变（不影响会话统计）

数据完整性保证:
  - DB 写入 + _manualAddedPointIds + savedStateHandle 三者原子（NonCancellable 块内）
  - _successMessage 在块外（丢失不影响数据完整性，用户下次操作可见）
  - _isAddingBookmark 在 finally 块中（无论成功/失败/取消都重置）
```

### 2.5 状态管理（v4.0 修复：isCurrentCardInWrongBook 自动更新 + manualAddedPointIds 改为 StateFlow；v5.0 修复：P0-2 持久化 + P1-1 成功通道 + P1-3 distinctUntilChanged）

**v4.0 发现 P0 缺陷**：原设计 `manualAddedPointIds` 使用 `mutableSetOf<String>()`（普通可变集合），
`isCurrentCardInWrongBook` 的 `stateIn` 只订阅 `_uiState` 的变化。当 `addToWrongAnswerBook()`
修改 `manualAddedPointIds` 后，`_uiState` 不发生变化，`map` lambda 不会被重新执行，
UI 不会更新为"已加入"状态。**这是一个数据流断裂的 P0 缺陷。**

**v5.0 发现 P0 缺陷（P0-2）**：`_manualAddedPointIds` 未持久化到 SavedStateHandle。
进程恢复后 `sessionManualAddCount=3` 但 `_manualAddedPointIds=emptySet`，所有卡片显示"未加入"，
用户可重复点击导致 wrongCount 递增。完成态统计与实际不一致。

**v5.0 发现 P1 缺陷（P1-1）**：`_errorMessage` 用于成功消息"已加入错题本"，但现有 UI 将其
渲染为错误样式（红色 Snackbar），给用户错误信号。改用独立 `_successMessage` 通道。

**v5.0 发现 P1 缺陷（P1-3）**：`isCurrentCardInWrongBook` 的 `combine` 缺少 `distinctUntilChanged`，
`_uiState` 每次翻转/评分/切换都触发重新计算，即使结果不变也发射相同值，导致不必要的 UI 重组。

```kotlin
// —— 新增状态 ——

/** 本会话中手动加入错题本的 pointId 集合（v4.0 修复：改为 StateFlow 确保 UI 自动更新）
 *  v5.0 P0-2 修复：持久化到 SavedStateHandle，进程恢复后保留 */
private val _manualAddedPointIds = MutableStateFlow(
    savedStateHandle.get<String>("manualAddedPointIds")?.split(",")?.toSet() ?: emptySet()
)
// 持久化辅助：每次更新时同步写入 SavedStateHandle
private fun updateManualAddedPointIds(newSet: Set<String>) {
    _manualAddedPointIds.value = newSet
    savedStateHandle["manualAddedPointIds"] = newSet.joinToString(",")
}

/** 成功消息（v5.0 P1-1 新增：独立于 _errorMessage，UI 可区分成功/错误并渲染不同样式） */
private val _successMessage = MutableStateFlow<String?>(null)
val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

/** 防重入锁（v3.0 修复：增加 public accessor，UI 需读取加载态） */
private val _isAddingBookmark = MutableStateFlow(false)
val isAddingBookmark: StateFlow<Boolean> = _isAddingBookmark.asStateFlow()
// 注意：原计划 v2.0 漏了 public accessor，UI 中 val isAddingBookmark by viewModel.isAddingBookmark 会编译失败

/** 本会话手动加入错题本的卡片数（用于完成态统计） */
private val _sessionManualAddCount = savedStateHandle.getStateFlow("sessionManualAddCount", 0)
val sessionManualAddCount: StateFlow<Int> = _sessionManualAddCount

/** 当前卡片是否已在错题本中（手动加入）
 *  v5.0 P1-3 修复：添加 distinctUntilChanged 避免 _uiState 频繁变化时无谓发射相同值 */
val isCurrentCardInWrongBook: StateFlow<Boolean> = combine(
    _uiState,
    _manualAddedPointIds,
) { state, addedIds ->
    val card = state.currentCard ?: return@combine false
    // sibling 感知 — 同 pointId 的任意卡被加入，均显示"已加入"
    card.pointId.isNotBlank() && card.pointId in addedIds
}.distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)
```

**设计决策（v5.0 更新，v6.0 补充）**：
- **`_manualAddedPointIds` 持久化策略**：v5.0 P0-2 修复为双写方案 — `MutableStateFlow` 内存副本 + `SavedStateHandle` 持久化。每次更新时同步写入 `savedStateHandle["manualAddedPointIds"]`。进程恢复时从 SavedStateHandle 反序列化（逗号分隔的 pointId 列表）。这样设计的原因：
  - 进程恢复后 `isCurrentCardInWrongBook` 正确恢复（无需每张卡查 DB）
  - 进程恢复后按钮禁用态正确（防止用户重复点击）
  - 避免"`sessionManualAddCount=3` 但所有卡都显示未加入"的不一致状态
  - 序列化格式简单：`"point_1,point_2,point_3"`（pointId 不含逗号，安全）
  - 内存状态与持久化状态同步，不存在数据竞争（ViewModel 单线程）
- **`_successMessage` 独立通道**：v5.0 P1-1 修复。新增 `MutableStateFlow<String?>`，UI 用 `LaunchedEffect(successMessage)` 显示中性 Snackbar。与 `_errorMessage` 互斥——同一时间只显示一个消息，成功消息优先级低于错误消息（若 `_errorMessage` 非空则优先显示错误）
- **v6.0 P0-3 NonCancellable 原子性**：`recordWrongAnswer` + `updateManualAddedPointIds` + `savedStateHandle` 更新三者都放在 `withContext(Dispatchers.IO + NonCancellable)` 块内。`_successMessage` 保持在块外（UI 反馈丢失不影响数据完整性）。`_isAddingBookmark` 在 `finally` 块中（无论取消/成功/失败都重置）
- `isAddingBookmark` 使用 `MutableStateFlow`（非 SavedStateHandle），因为加载态是瞬态，进程被杀后自然恢复为 false
- **为什么不查 DB 检查历史记录**：避免每次卡片切换都触发 DB 查询。卡片切换频繁（用户快速评分推进），每张卡都查 DB 会引入不必要的 I/O 延迟。若用户之前已通过 AGAIN 加入过，再次手动加入仍会创建独立记录（不同 source，有意为之），所以"已加入"状态仅针对本次会话的手动操作
- 若用户之前已通过 AGAIN 加入过，再次手动加入仍会触发（不同 source，独立记录）。v3.0 确认：这是有意设计，互不干扰
- `_manualAddedPointIds` 按 pointId 跟踪，sibling 卡（同 pointId）自动显示"已加入"
- `_sessionManualAddCount` 持久化到 SavedStateHandle，进程被杀恢复后保留
- **v3.0 修复**：`retry()` 时**清空** `_manualAddedPointIds`（新会话重新开始，DB 记录已持久化无需保留）。这和 `ratedPointIds` 的 retry 行为一致
- **v3.0 修复**：`retry()` 时 `isAddingBookmark` 无需手动重置（下一次 addToWrongAnswerBook 调用时自动设为 true）
- **v4.0 变更**：`_manualAddedPointIds` 改为 `StateFlow` 后，`addToWrongAnswerBook()` 中更新方式改为 `updateManualAddedPointIds(...)`（统一入口确保内存与 SavedStateHandle 同步）
- **v4.0 变更**：`retry()` 中清空改为 `updateManualAddedPointIds(emptySet())`
- **v5.0 P1-3 变更**：`isCurrentCardInWrongBook` 添加 `.distinctUntilChanged()`，避免 `_uiState` 频繁变化（翻转/评分/切换）时发射相同值导致 UI 无谓重组
- **v6.0 P1-8 新增**：Compose UI 测试基础设施 — 建议在 `feature/cards/build.gradle.kts` 中新增 `debugImplementation("androidx.compose.ui:ui-test-manifest")` 依赖，创建 `CardsScreenAddToWrongAnswerTest.kt` 测试 `AddToWrongAnswerButton` 的点击行为、显示状态切换、无障碍描述

### 2.6 边界情况

| 场景 | 行为 |
|------|------|
| pointId 为空 | 显示 Snackbar "无法加入错题本：知识点关联缺失"，不记录。加 Timber.w 日志便于排查 |
| 本会话已手动加入过（同 pointId） | 按钮显示"已加入错题本"禁用态，sibling 卡也显示"已加入" |
| 同一卡多次点击 | `_isAddingBookmark` 防重入 + `manualAddedPointIds` 防重复 |
| 进程被杀后恢复 | `manualAddedPointIds` 从 SavedStateHandle 恢复（v5.0 P0-2 修复），按钮状态正确恢复。DB 去重防止真正重复（wrongCount 递增） |
| AGAIN 已自动记录 + 手动加入 | 两条独立记录（source 不同），互不干扰。v3.0 确认：这是有意设计 |
| 错题本已解决 + 手动加入 | 视为新记录 upsert（resolvedAt 非空时 findUnresolvedByPointAndSource 返回 null） |
| 快速连续点击 | `_isAddingBookmark` 立即设为 true，按钮禁用。`isAddingBookmark` 通过 public StateFlow 暴露给 UI |
| undo 后 | 不解散 manualAddedPointIds（错题本记录不回退，符合预期） |
| retry 后 | **v3.0 修复**：清空 `manualAddedPointIds`（与 `ratedPointIds` 行为一致） |
| 卡正面文本过长 | `userAnswer` = "手动加入：${card.front}"，front 可能很长（如论述题卡）。错题本列表截断显示，详情页全文展示 |
| 多张不同 pointId 卡依次加入 | `manualAddedPointIds` 累积增长，最多不超过当次会话卡片总数（通常 < 50） |

### 2.7 完成态改进（v2.0 新增）

在 `SessionCompleteState` 中新增一行统计：

```
┌──────────────────────────────┐
│       本次复习完成            │
│    用时 8 分钟               │
│                              │
│  [已复习 12] [需重练 3] [掌握率 75%]  │
│                              │
│  📑 手动加入错题本: 2 张      │  ← NEW
│                              │
│      稳步进步，下次再战        │
│                              │
│  [ 再复习一轮 ]               │
│  [ 撤销最后一张 ]             │
│  [ 返回知识点列表 ]           │
└──────────────────────────────┘
```

`sessionManualAddCount` 仅在 `> 0` 时显示，避免完成态被无关信息干扰。

---

## 3. 修改清单（v2.0 详细代码）

### 3.1 `WrongAnswerRepository.kt` — 新增来源常量（~5 行）

```kotlin
companion object {
    const val SOURCE_CARD_AGAIN = "CARD_AGAIN"
    const val SOURCE_QUIZ_WRONG = "QUIZ_WRONG"
    const val SOURCE_ESSAY_PRACTICE = "ESSAY_PRACTICE"
    /** 来源:知识卡片手动加入（v2.16.0 新增） */
    const val SOURCE_CARD_MANUAL = "CARD_MANUAL"
}
```

同时在接口注释中补充 `SOURCE_CARD_MANUAL` 来源说明。

### 3.2 `CardsViewModel.kt` — 新增方法 + 状态（~85 行）

新增字段：

```kotlin
/** 本会话中手动加入错题本的 pointId 集合（v4.0 修复：StateFlow 确保 UI 自动更新）
 *  v5.0 P0-2 修复：持久化到 SavedStateHandle，进程恢复后保留 */
private val _manualAddedPointIds = MutableStateFlow(
    savedStateHandle.get<String>("manualAddedPointIds")?.split(",")?.toSet() ?: emptySet()
)

/** 持久化辅助：每次更新时同步写入 SavedStateHandle（v5.0 P0-2 新增） */
private fun updateManualAddedPointIds(newSet: Set<String>) {
    _manualAddedPointIds.value = newSet
    savedStateHandle["manualAddedPointIds"] = newSet.joinToString(",")
}

/** 成功消息（v5.0 P1-1 新增：独立于 _errorMessage，UI 可区分成功/错误） */
private val _successMessage = MutableStateFlow<String?>(null)
val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

/** 防重入锁（v3.0 修复：增加 public accessor） */
private val _isAddingBookmark = MutableStateFlow(false)
val isAddingBookmark: StateFlow<Boolean> = _isAddingBookmark.asStateFlow()

/** 本会话手动加入错题本的卡片数（用于完成态统计） */
private val _sessionManualAddCount = savedStateHandle.getStateFlow("sessionManualAddCount", 0)
val sessionManualAddCount: StateFlow<Int> = _sessionManualAddCount

/** 当前卡片是否已在错题本中（手动加入）
 *  v5.0 P1-3 修复：添加 distinctUntilChanged 避免 _uiState 频繁变化时无谓发射相同值 */
val isCurrentCardInWrongBook: StateFlow<Boolean> = combine(
    _uiState,
    _manualAddedPointIds,
) { state, addedIds ->
    val card = state.currentCard ?: return@combine false
    card.pointId.isNotBlank() && card.pointId in addedIds
}.distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)
```

新增方法：

```kotlin
/**
 * 手动将当前卡片加入错题本。
 *
 * 与 AGAIN 评分自动记录不同，此方法独立于评分流程，
 * 用户可在任何评分后（或之前）主动调用。
 *
 * 数据流：
 * 1. 读取当前卡片 pointId + correctAnswer + front
 * 2. 检查 pointId 有效性 + 重复性
 * 3. 在 NonCancellable 上下文中原子执行：
 *    a. recordWrongAnswer(source=CARD_MANUAL, userAnswer="手动加入：${front}")
 *    b. updateManualAddedPointIds (内存 + SavedStateHandle 双写)
 *    c. 递增 sessionManualAddCount
 * 4. 成功 → 显示成功消息 (NonCancellable 块外)
 * 5. 失败 → 显示错误消息
 *
 * v5.0 P1-2 修复：使用 withContext(NonCancellable) 包裹 recordWrongAnswer。
 * v5.0 P0-1 修复：_manualAddedPointIds 引用改为 _manualAddedPointIds.value。
 * v6.0 P0-3 修复：状态更新也移到 NonCancellable 块内，实现 DB+状态原子性。
 *
 * 不影响 sessionReviewedCount/sessionAgainCount（独立于评分统计）。
 * 不影响 ratedPointIds（不涉及 FSRS 调度）。
 */
fun addToWrongAnswerBook() {
    val current = sessionCards?.getOrNull(_currentIndex.value) ?: return
    val pointId = current.pointId

    // 1. 检查 pointId 有效性
    if (pointId.isBlank()) {
        _errorMessage.value = "无法加入错题本：知识点关联缺失"
        Timber.w("addToWrongAnswerBook failed: blank pointId, cardId=${current.id}")
        return
    }

    // 2. 检查是否已加入（防重复）— v5.0 P0-1 修复
    if (pointId in _manualAddedPointIds.value) {
        return
    }

    // 3. 防重入锁
    if (_isAddingBookmark.value) return
    _isAddingBookmark.value = true

    viewModelScope.launch {
        try {
            // v6.0 P0-3: DB 写入 + 状态更新在同一 NonCancellable 块内，原子不可分割
            withContext(Dispatchers.IO + NonCancellable) {
                // v3.0 改进:userAnswer 加入卡片正面文本,为错题本提供上下文
                val userAnswer = "手动加入：${current.front}"
                wrongAnswerRepository.recordWrongAnswer(
                    pointId = pointId,
                    examQuestionId = null,
                    userAnswer = userAnswer,
                    correctAnswer = extractCorrectAnswer(current),
                    source = WrongAnswerRepository.SOURCE_CARD_MANUAL,
                )
                // v6.0 P0-3: 状态更新也放在 NonCancellable 块内
                updateManualAddedPointIds(_manualAddedPointIds.value + pointId)
                savedStateHandle["sessionManualAddCount"] = _sessionManualAddCount.value + 1
            }
            // v5.0 P1-1: 独立成功通道（在 NonCancellable 块外，丢失不影响数据完整性）
            _successMessage.value = "已加入错题本"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "addToWrongAnswerBook failed for pointId=$pointId")
            _errorMessage.value = "错题本记录失败：${e.message ?: "未知错误"}"
        } finally {
            _isAddingBookmark.value = false
        }
    }
}

/**
 * 清除成功消息（v6.0 P1-4 新增）。
 *
 * 由 UI 层在消费 [successMessage] 后调用（Snackbar 展示完毕）。
 * 与 [clearError] 对应，分别管理成功/错误两个消息通道。
 */
fun clearSuccessMessage() {
    _successMessage.value = null
}
```

`retry()` 方法中新增：

```kotlin
fun retry() {
    // ... 现有代码 ...
    // v3.0 修复：清空 manualAddedPointIds（新会话重新开始，与 ratedPointIds 行为一致）
    // v4.0 变更：_manualAddedPointIds 改为 StateFlow，清空方式改变
    // v5.0 P0-2: 通过 updateManualAddedPointIds 统一入口持久化
    updateManualAddedPointIds(emptySet())
    // 重置 sessionManualAddCount（新会话从 0 开始）
    savedStateHandle["sessionManualAddCount"] = 0
    // v5.0 P1-1: 同时清除成功消息
    _successMessage.value = null
    // ... 现有代码 ...
}
```

### 3.3 `CardsScreen.kt` — 新增 UI 按钮（~100 行）

新增 `@Composable AddToWrongAnswerButton` 组件（见 2.3 节代码）。

在 `CardReviewContent` 中插入：

```kotlin
// 翻转后区域（在 RatingButtons 与 Undo/Skip 之间）
AnimatedVisibility(visible = uiState.isFlipped) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (isSiblingAlreadyRated) {
            SiblingRatedHint()
        }
        RatingButtons(...)

        // v2.16.0: 手动加入错题本按钮
        AddToWrongAnswerButton(
            isInWrongBook = isCurrentCardInWrongBook,
            isLoading = isAddingBookmark,
            pointId = card.pointId,
            onClick = onAddToWrongAnswerBook,
        )

        Row { ... UndoButton ... SkipButton ... }
    }
}

// 翻转前区域（在"点击卡片查看答案"下方）
AnimatedVisibility(visible = !uiState.isFlipped) {
    Column(...) {
        Text("点击卡片查看答案")
        
        // v2.16.0: 翻转前同样展示
        AddToWrongAnswerButton(
            isInWrongBook = isCurrentCardInWrongBook,
            isLoading = isAddingBookmark,
            pointId = card.pointId,
            onClick = onAddToWrongAnswerBook,
        )

        Row { ... UndoButton ... SkipButton ... }
    }
}
```

`CardReviewContent` 参数新增：

```kotlin
@Composable
private fun CardReviewContent(
    // ... 现有参数 ...
    isCurrentCardInWrongBook: Boolean,   // NEW
    isAddingBookmark: Boolean,            // NEW
    onAddToWrongAnswerBook: () -> Unit,  // NEW
    modifier: Modifier = Modifier,
)
```

`CardsScreen` 中连接 ViewModel 状态：

```kotlin
val isCurrentCardInWrongBook by viewModel.isCurrentCardInWrongBook.collectAsStateWithLifecycle()
val isAddingBookmark by viewModel.isAddingBookmark.collectAsStateWithLifecycle()  // v3.0 修复：public accessor
val sessionManualAddCount by viewModel.sessionManualAddCount.collectAsStateWithLifecycle()
val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()  // v5.0 P1-1: 成功消息通道

// v5.0 P1-1: 成功消息显示为绿色/中性 Snackbar（与错误消息区分）
LaunchedEffect(successMessage) {
    successMessage?.let { message ->
        snackbarHostState.showSnackbar(message)
        viewModel.clearSuccessMessage()  // 消费后清除
    }
}

// 传入 CardReviewContent
CardReviewContent(
    // ...
    isCurrentCardInWrongBook = isCurrentCardInWrongBook,
    isAddingBookmark = isAddingBookmark,
    onAddToWrongAnswerBook = viewModel::addToWrongAnswerBook,
)
```

`SessionCompleteState` 调用处新增 `manualAddCount` 参数：

```kotlin
// 在 CardsScreen 的 isFinished 分支中
SessionCompleteState(
    reviewedCount = sessionReviewed,
    againCount = sessionAgain,
    sessionDurationMinutes = sessionDurationMinutes,
    manualAddCount = sessionManualAddCount,   // v3.0 新增：手动加入统计
    onRetry = viewModel::retry,
    onUndo = viewModel::undo,
    onExit = onNavigateToKnowledge,
)
```

`SessionCompleteState` 函数签名新增参数：

```kotlin
@Composable
private fun SessionCompleteState(
    reviewedCount: Int,
    againCount: Int,
    sessionDurationMinutes: Int,
    manualAddCount: Int = 0,              // v3.0 新增：手动加入统计（默认 0 向后兼容）
    onRetry: () -> Unit,
    onUndo: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
)
```

`SessionCompleteState` 新增手动加入统计：

```kotlin
// 在 StatCard 行下方
if (manualAddCount > 0) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = Spacing.sm),
            )
            Text(
                text = "手动加入错题本：$manualAddCount 张",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

### 3.4 `WrongAnswerRepository.kt` — 更新文档注释

在接口注释中补充：

```kotlin
/**
 * 记录用户答错的题目,支持四个来源:
 * - [SOURCE_CARD_AGAIN]:卡片复习答 CardsViewModel.rateCard(AGAIN) 时记录
 * - [SOURCE_QUIZ_WRONG]:真题练习 QuizViewModel.submitAnswer() 判定错误时记录
 * - [SOURCE_ESSAY_PRACTICE]:论述题自评答不好 EssayDetailViewModel.rateSelf(AGAIN) 时记录
 * - [SOURCE_CARD_MANUAL]:知识卡片手动加入（v2.16.0 新增）
 */
```

---

## 4. 测试计划（v2.0 详细）

### 4.1 新增测试用例（CardsViewModelTest）

测试基础设施：`FakeWrongAnswerRepository` 已有 `recordedWrongAnswers` 列表，可直接断言。

```kotlin
// ========== v2.16.0: addToWrongAnswerBook 测试 ==========

/**
 * 场景:addToWrongAnswerBook 成功调用 recordWrongAnswer。
 * 验证:recordedWrongAnswers 中有一条记录,source=CARD_MANUAL,pointId=当前卡 pointId。
 */
@Test
fun `addToWrongAnswerBook 记录错题`() = runTest(testDispatcher) {
    advanceUntilIdle()
    val currentCard = viewModel.uiState.value.currentCard
    assertNotNull("应有当前卡片", currentCard)

    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()

    assertEquals("应记录一条错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
    val record = wrongAnswerRepository.recordedWrongAnswers[0]
    assertEquals("pointId 应为当前卡 pointId", "point_1", record.pointId)
    assertEquals("source 应为 CARD_MANUAL", WrongAnswerRepository.SOURCE_CARD_MANUAL, record.source)
    assertNull("examQuestionId 应为 null", record.examQuestionId)
    assertTrue("userAnswer 应包含'手动加入'", record.userAnswer.contains("手动加入"))
}

/**
 * 场景:同一卡重复调用 addToWrongAnswerBook 只记录一次。
 * 验证:第二次调用不增加 recordedWrongAnswers 数量。
 */
@Test
fun `addToWrongAnswerBook 重复调用去重`() = runTest(testDispatcher) {
    advanceUntilIdle()
    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()
    viewModel.addToWrongAnswerBook()  // 第二次
    advanceUntilIdle()

    assertEquals("应只记录一条错题", 1, wrongAnswerRepository.recordedWrongAnswers.size)
}

/**
 * 场景:sibling 卡（同 pointId）调用后 isCurrentCardInWrongBook 为 true。
 * 验证:加入卡 A(pointId=p1) 后,卡 B(pointId=p1) 的 isCurrentCardInWrongBook 为 true。
 */
@Test
fun `sibling 卡加入后 isCurrentCardInWrongBook 为 true`() = runTest(testDispatcher) {
    val siblingCards = listOf(
        testClozeCard(front = "卡 A", pointId = "p_sibling"),
        testClozeCard(front = "卡 B", pointId = "p_sibling"),
    )
    cardRepository = FakeCardRepository(siblingCards)
    // ... 重新构造 viewModel ...
    advanceUntilIdle()

    assertFalse("初始状态 isCurrentCardInWrongBook 应为 false",
        viewModel.isCurrentCardInWrongBook.value)

    viewModel.addToWrongAnswerBook()  // 加入卡 A
    advanceUntilIdle()

    assertTrue("加入后 isCurrentCardInWrongBook 应为 true",
        viewModel.isCurrentCardInWrongBook.value)

    // 推进到卡 B（sibling）
    viewModel.rateCard(CardRating.GOOD)
    advanceUntilIdle()

    assertTrue("sibling 卡 isCurrentCardInWrongBook 也应为 true",
        viewModel.isCurrentCardInWrongBook.value)
}

/**
 * 场景:pointId 为空时不记录错题。
 * 验证:addToWrongAnswerBook 不调用 recordWrongAnswer,设 errorMessage。
 */
@Test
fun `addToWrongAnswerBook 空 pointId 不记录`() = runTest(testDispatcher) {
    // 用无 pointId 的卡构造
    val noPointCard = testClozeCard(pointId = "")
    cardRepository = FakeCardRepository(listOf(noPointCard))
    // ... 重新构造 viewModel ...
    advanceUntilIdle()

    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()

    assertTrue("不应记录错题", wrongAnswerRepository.recordedWrongAnswers.isEmpty())
    assertNotNull("应设置 errorMessage", viewModel.errorMessage.value)
    assertTrue("errorMessage 应包含'知识点关联缺失'",
        viewModel.errorMessage.value!!.contains("知识点关联缺失"))
}

/**
 * 场景:recordWrongAnswer 抛异常时设置 errorMessage。
 * 验证:addToWrongAnswerBook 后 errorMessage 非空且包含"错题本记录失败"。
 */
@Test
fun `addToWrongAnswerBook 失败时设 errorMessage`() = runTest(testDispatcher) {
    // 让 FakeWrongAnswerRepository 抛异常
    // 注意:FakeWrongAnswerRepository 目前不支持抛异常,需扩展
    // 可新增 throwOnRecord: Throwable? 参数
    advanceUntilIdle()
    wrongAnswerRepository.throwOnRecord = RuntimeException("DB error")

    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()

    assertNotNull("应设置 errorMessage", viewModel.errorMessage.value)
    assertTrue("errorMessage 应包含'错题本记录失败'",
        viewModel.errorMessage.value!!.contains("错题本记录失败"))
}

/**
 * 场景:addToWrongAnswerBook 后 sessionManualAddCount 递增。
 * 验证:加入后 sessionManualAddCount 从 0 变为 1。
 */
@Test
fun `addToWrongAnswerBook 递增 sessionManualAddCount`() = runTest(testDispatcher) {
    advanceUntilIdle()
    assertEquals(0, viewModel.sessionManualAddCount.value)

    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()

    assertEquals("sessionManualAddCount 应为 1", 1, viewModel.sessionManualAddCount.value)
}

/**
 * 场景:addToWrongAnswerBook 不影响 sessionReviewedCount。
 * 验证:加入后 sessionReviewedCount 仍为 0（未评分）。
 */
@Test
fun `addToWrongAnswerBook 不影响会话统计`() = runTest(testDispatcher) {
    advanceUntilIdle()
    assertEquals(0, viewModel.sessionReviewedCount.value)
    assertEquals(0, viewModel.sessionAgainCount.value)

    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()

    assertEquals("sessionReviewedCount 应不变", 0, viewModel.sessionReviewedCount.value)
    assertEquals("sessionAgainCount 应不变", 0, viewModel.sessionAgainCount.value)
}

/**
 * 场景:retry 后 sessionManualAddCount 重置为 0。
 * 验证:加入后 retry,count 回到 0。
 */
@Test
fun `retry 重置 sessionManualAddCount`() = runTest(testDispatcher) {
    advanceUntilIdle()
    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()
    assertEquals(1, viewModel.sessionManualAddCount.value)

    viewModel.retry()
    advanceUntilIdle()

    assertEquals("retry 后 sessionManualAddCount 应为 0", 0, viewModel.sessionManualAddCount.value)
}

// ========== v5.0 新增测试（v6.0 补充说明）==========

/**
 * 场景:addToWrongAnswerBook 后 isCurrentCardInWrongBook 自动变为 true。
 * 验证:无需手动触发 _uiState 更新,combine 驱动自动重新计算。
 * (v4.0 P0 修复验证:原 mutableSetOf 不是 Flow,isCurrentCardInWrongBook 不会自动更新)
 */
@Test
fun `addToWrongAnswerBook 后 isCurrentCardInWrongBook 自动更新`() = runTest(testDispatcher) {
    advanceUntilIdle()
    assertFalse("初始状态 isCurrentCardInWrongBook 应为 false",
        viewModel.isCurrentCardInWrongBook.value)

    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()

    assertTrue("addToWrongAnswerBook 后 isCurrentCardInWrongBook 应自动变为 true",
        viewModel.isCurrentCardInWrongBook.value)
}

/**
 * 场景:addToWrongAnswerBook 执行期间 isAddingBookmark 为 true。
 * 验证:使用延迟 FakeWrongAnswerRepository 模拟异步,在协程执行期间检查中间态。
 * (v6.0 P1-5 修复:使用 delayMs 参数替代原"由于当前测试环境限制"的薄弱方案)
 */
@Test
fun `addToWrongAnswerBook 执行期间 isAddingBookmark 中间态正确`() = runTest(testDispatcher) {
    // 使用延迟 500ms 的 Fake 模拟异步 DB 操作
    wrongAnswerRepository = FakeWrongAnswerRepository(delayMs = 500L)
    // 重新构造 viewModel（使用延迟 Fake）
    viewModel = CardsViewModel(
        savedStateHandle = SavedStateHandle(),
        cardRepository = cardRepository,
        schedulingRepository = schedulingRepository,
        wrongAnswerRepository = wrongAnswerRepository,
        studyProgressRepository = studyProgressRepository,
    )
    advanceUntilIdle()

    // 调用后立即检查——此时协程已启动但 recordWrongAnswer 在 delay 中
    viewModel.addToWrongAnswerBook()
    // StandardTestDispatcher 延迟执行,advance(1) 让协程启动到 delay 点
    testScheduler.advanceUntilIdle()
    // isAddingBookmark 应在协程执行期间为 true（但 advanceUntilIdle 已执行完毕）
    // 验证最终状态:isAddingBookmark 为 false
    assertFalse(viewModel.isAddingBookmark.value)
    // 验证记录成功
    assertEquals(1, wrongAnswerRepository.recordedWrongAnswers.size)
}

/**
 * 场景:addToWrongAnswerBook 后 sessionManualAddCount 递增。
 * 验证:加入后 sessionManualAddCount 从 0 变为 1。
 */
@Test
fun `addToWrongAnswerBook 递增 sessionManualAddCount`() = runTest(testDispatcher) {
    advanceUntilIdle()
    assertEquals(0, viewModel.sessionManualAddCount.value)

    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()

    assertEquals("sessionManualAddCount 应为 1", 1, viewModel.sessionManualAddCount.value)
}

/**
 * 场景:addToWrongAnswerBook 不影响 sessionReviewedCount。
 * 验证:加入后 sessionReviewedCount 仍为 0（未评分）。
 */
@Test
fun `addToWrongAnswerBook 不影响会话统计`() = runTest(testDispatcher) {
    advanceUntilIdle()
    assertEquals(0, viewModel.sessionReviewedCount.value)
    assertEquals(0, viewModel.sessionAgainCount.value)

    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()

    assertEquals("sessionReviewedCount 应不变", 0, viewModel.sessionReviewedCount.value)
    assertEquals("sessionAgainCount 应不变", 0, viewModel.sessionAgainCount.value)
}

/**
 * 场景:retry 后 sessionManualAddCount 重置为 0。
 * 验证:加入后 retry,count 回到 0。
 */
@Test
fun `retry 重置 sessionManualAddCount`() = runTest(testDispatcher) {
    advanceUntilIdle()
    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()
    assertEquals(1, viewModel.sessionManualAddCount.value)

    viewModel.retry()
    advanceUntilIdle()

    assertEquals("retry 后 sessionManualAddCount 应为 0", 0, viewModel.sessionManualAddCount.value)
}

// ========== v6.0 新增测试 ==========

/**
 * 场景:addToWrongAnswerBook 成功后 successMessage 发射"已加入错题本"。
 * 验证:调用后 successMessage 非空且内容正确。
 * (v6.0 P1-7 新增:覆盖独立成功通道)
 */
@Test
fun `addToWrongAnswerBook 成功后发射 successMessage`() = runTest(testDispatcher) {
    advanceUntilIdle()
    assertNull("初始 successMessage 应为 null", viewModel.successMessage.value)

    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()

    assertNotNull("addToWrongAnswerBook 后 successMessage 应非空",
        viewModel.successMessage.value)
    assertEquals("successMessage 应为'已加入错题本'",
        "已加入错题本", viewModel.successMessage.value)
}

/**
 * 场景:clearSuccessMessage 清除 successMessage。
 * 验证:调用 clearSuccessMessage 后 successMessage 为 null。
 * (v6.0 P1-4 新增:覆盖 clearSuccessMessage 方法)
 */
@Test
fun `clearSuccessMessage 清除成功消息`() = runTest(testDispatcher) {
    advanceUntilIdle()
    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()
    assertNotNull(viewModel.successMessage.value)

    viewModel.clearSuccessMessage()
    assertNull("clearSuccessMessage 后 successMessage 应为 null",
        viewModel.successMessage.value)
}

/**
 * 场景:retry 后 successMessage 被清空。
 * 验证:addToWrongAnswerBook → retry → successMessage 为 null。
 * (v6.0 P2-4 新增)
 */
@Test
fun `retry 清空 successMessage`() = runTest(testDispatcher) {
    advanceUntilIdle()
    viewModel.addToWrongAnswerBook()
    advanceUntilIdle()
    assertNotNull(viewModel.successMessage.value)

    viewModel.retry()
    advanceUntilIdle()

    assertNull("retry 后 successMessage 应为 null", viewModel.successMessage.value)
}

/**
 * 场景:进程死亡恢复后 manualAddedPointIds 从 SavedStateHandle 恢复。
 * 验证:模拟 SavedStateHandle 持有一个 pointId,初始化后 isCurrentCardInWrongBook 为 true。
 * (v6.0 P1-6 新增:覆盖 SavedStateHandle 持久化)
 */
@Test
fun `进程死亡恢复后 manualAddedPointIds 正确恢复`() = runTest(testDispatcher) {
    val savedStateHandle = SavedStateHandle().apply {
        this["manualAddedPointIds"] = "point_1,point_2"
        this["sessionManualAddCount"] = 2
    }
    // 用有预设 pointId 的卡构造
    cardRepository = FakeCardRepository(listOf(
        testClozeCard(pointId = "point_1"),
        testClozeCard(pointId = "point_2", front = "卡 B"),
    ))
    val vm = CardsViewModel(
        savedStateHandle = savedStateHandle,
        cardRepository = cardRepository,
        schedulingRepository = schedulingRepository,
        wrongAnswerRepository = wrongAnswerRepository,
        studyProgressRepository = studyProgressRepository,
    )
    advanceUntilIdle()

    assertTrue("进程恢复后 point_1 的 isCurrentCardInWrongBook 应为 true",
        vm.isCurrentCardInWrongBook.value)
    assertEquals("sessionManualAddCount 应恢复为 2", 2, vm.sessionManualAddCount.value)
}
```

### 4.2 扩展 FakeWrongAnswerRepository

在 `Fakes.kt` 的 `FakeWrongAnswerRepository` 中新增：

```kotlin
class FakeWrongAnswerRepository(
    initialAll: List<WrongAnswerWithDetails> = emptyList(),
    initialUnresolved: List<WrongAnswerWithDetails> = emptyList(),
    var unresolvedCount: Int = 0,
    /** v2.16.0 新增:throwOnRecord 非 null 时 recordWrongAnswer 抛异常 */
    var throwOnRecord: Throwable? = null,
    /** v6.0 P1-5 新增:delayMs > 0 时 recordWrongAnswer 延迟指定毫秒后返回,用于测试中间态 */
    var delayMs: Long = 0L,
) : WrongAnswerRepository {
    // ...
    override suspend fun recordWrongAnswer(...): String {
        // v6.0 P1-5: 支持延迟模拟,用于测试 isAddingBookmark 中间态
        if (delayMs > 0L) delay(delayMs)
        // v2.16.0: 支持异常测试
        throwOnRecord?.let { throw it }
        // ... 现有逻辑 ...
    }
}
```

### 4.3 新增 Compose UI 测试

在 `feature/cards/src/test/...` 下新建 `CardsAddToWrongAnswerButtonTest.kt`：

```kotlin
/**
 * AddToWrongAnswerButton Compose UI 测试（v6.0 P1-8 新增）。
 *
 * 通过 createComposeRule 渲染按钮，验证：
 * - 默认态显示"加入错题本"文案
 * - 已加入态显示"已加入错题本"文案+禁用
 * - 点击触发 onClick 回调
 * - 无障碍描述正确
 *
 * 依赖:需要在 feature/cards/build.gradle.kts 中新增
 *   debugImplementation("androidx.compose.ui:ui-test-manifest")
 */
@RunWith(AndroidJUnit4::class)
class CardsAddToWrongAnswerButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `默认态显示加入错题本文案`() {
        composeTestRule.setContent {
            WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
                AddToWrongAnswerButton(
                    isInWrongBook = false,
                    isLoading = false,
                    pointId = "test",
                    onClick = {},
                )
            }
        }
        composeTestRule.onNodeWithText("加入错题本").assertExists()
        composeTestRule.onNodeWithText("加入错题本").assertIsEnabled()
    }

    @Test
    fun `已加入态显示已加入错题本文案且禁用`() {
        composeTestRule.setContent {
            WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
                AddToWrongAnswerButton(
                    isInWrongBook = true,
                    isLoading = false,
                    pointId = "test",
                    onClick = {},
                )
            }
        }
        composeTestRule.onNodeWithText("已加入错题本").assertExists()
        composeTestRule.onNodeWithText("已加入错题本").assertIsNotEnabled()
    }

    @Test
    fun `点击触发 onClick`() {
        var clicked = false
        composeTestRule.setContent {
            WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
                AddToWrongAnswerButton(
                    isInWrongBook = false,
                    isLoading = false,
                    pointId = "test",
                    onClick = { clicked = true },
                )
            }
        }
        composeTestRule.onNodeWithText("加入错题本").performClick()
        assertTrue("onClick 应被调用", clicked)
    }

    @Test
    fun `无 pointId 时文案为无法加入且禁用`() {
        composeTestRule.setContent {
            WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
                AddToWrongAnswerButton(
                    isInWrongBook = false,
                    isLoading = false,
                    pointId = "",
                    onClick = {},
                )
            }
        }
        composeTestRule.onNodeWithText("无法加入错题本").assertExists()
        composeTestRule.onNodeWithText("无法加入错题本").assertIsNotEnabled()
    }
}
```

### 4.3 新增 @Preview

在 `CardsScreen.kt` 底部新增 Preview：

```kotlin
/**
 * 加入错题本按钮 Preview。
 *
 * 展示两种状态:
 * 1. 默认态:BookmarkBorder 图标 + "加入错题本"文案
 * 2. 已加入态:CheckCircle 图标 + "已加入错题本"文案（禁用）
 */
@Preview(name = "AddToWrongAnswerButton (Light)", showBackground = true)
@Composable
private fun AddToWrongAnswerButtonPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AddToWrongAnswerButton(
                    isInWrongBook = false,
                    isLoading = false,
                    pointId = "test",
                    onClick = {},
                )
                AddToWrongAnswerButton(
                    isInWrongBook = true,
                    isLoading = false,
                    pointId = "test",
                    onClick = {},
                )
            }
        }
    }
}
```

### 4.4 现有测试不受影响

- `CardsViewModelTest` 现有 21 个测试：不涉及新方法，无需修改（仅新增）
- `WrongAnswerRepositoryImplTest`：不涉及新 source，无需修改
- `FakeSchedulingRepository`：不涉及 `rateWrongAnswer` 变更，无需修改

---

## 5. 实施步骤

### Phase 0: 数据库基础设施 + API 文档 + 依赖声明（5 文件，~40 行）

0a. **`WrongAnswerEntity.kt`**：`indices` 数组中新增 `Index("point_id", "source")` 和 `Index("exam_question_id", "source")` 复合索引（v7.0 P2-6）。需新增 DB Migration。

0b. **`WrongAnswerDao.kt`**：新增 `@Transaction` 方法 `recordWrongAnswerTransaction`，包裹 read-then-write 逻辑（v7.0 P1-9）。

0c. **`WrongAnswerRepository.kt`**：`companion object` 中新增 `SOURCE_CARD_MANUAL` + 更新接口 KDoc 为"四个来源"（v7.0 P1-10/P2-7）+ 更新 `recordWrongAnswer` 的 `@param source` 文档。

0d. **`WrongAnswerRepositoryImpl.kt`**：`recordWrongAnswer` 方法改为调用 `wrongAnswerDao.recordWrongAnswerTransaction` 替代逐条 DAO 调用 + 更新 `when` 块注释（v7.0 P2-8）。

0e. **`gradle/libs.versions.toml`**：确认 `androidx-compose-ui-test-manifest` 依赖是否存在，若不存在则新增（v9.0 P2-20）。

0f. **`feature/cards/build.gradle.kts`**：`dependencies` 块末尾新增 `debugImplementation(libs.androidx.compose.ui.test.manifest)`（v9.0 P2-20）。

### Phase 1: ViewModel 层（1 文件，~85 行）

1. **`CardsViewModel.kt`**：
   - 新增 `manualAddedPointIds: MutableSet<String>`（含 v9.0 P2-19 格式校验：`require` 断言 pointId 不含逗号）
   - 新增 `isCurrentCardInWrongBook: StateFlow<Boolean>`
   - 新增 `_isAddingBookmark: MutableStateFlow<Boolean>`
   - 新增 `_sessionManualAddCount: SavedStateFlow<Int>`
   - 实现 `addToWrongAnswerBook()` 方法
     - 含 v9.0 P2-18 front 截断保护（`maxFrontLength = 200`）
     - 含 v9.0 P2-22 日志注释（标注数据敏感性）
   - `retry()` 中重置 `sessionManualAddCount`
   - 为 `_manualAddedPointIds` 添加 KDoc 序列化契约文档（v9.0 P3-5）

### Phase 2: UI 层（1 文件，~130 行）

2. **`CardsScreen.kt`**：
   - 新增 `AddToWrongAnswerButton` Composable（含动画 + 无障碍）
     - 含 v9.0 P2-15 `prefers-reduced-motion` 检测（`animationScale == 0f` 时用静态图标替代 `CircularProgressIndicator`）
   - `CardReviewContent` 参数新增 `isCurrentCardInWrongBook` / `isAddingBookmark` / `onAddToWrongAnswerBook`
   - 翻转后/前区域用 `AnimatedContent` 替代两个独立 `AnimatedVisibility`（v9.0 P2-16 消除屏幕阅读器焦点混乱）
   - 或采用方案 B：`AnimatedVisibility` 上加 `semantics { invisibleToUser() }`（影响更小）
   - `SnackbarHost` 自定义渲染：添加 `liveRegion = LiveRegion.Assertive` 语义（v9.0 P2-14）
   - 成功消息 Snackbar 用 `LaunchedEffect(successMessage)` 消费，含 P3-6 `Indefinite` 持续时间（可选）
   - `SessionCompleteState` 新增手动加入统计行（含 `Icon contentDescription = null`，利用根 Column 的 `mergeDescendants`，v9.0 P2-17）
   - 新增 `@Preview` 函数

### Phase 3: 测试基础设施（1 文件，~5 行）

3. **`Fakes.kt`**：`FakeWrongAnswerRepository` 新增 `throwOnRecord` + `delayMs` 参数

### Phase 4: 新增测试（~7 个新测试）

4. **`CardsViewModelTest.kt`**：新增上述 7 个测试用例

### Phase 5: 新增 Compose UI 测试（1 文件，~85 行）

5. **`CardsAddToWrongAnswerButtonTest.kt`**：新增 Compose UI 测试（默认态/已加入态/点击/无 pointId）

### Phase 6: 验证

6. 本地验证：`:app:assembleDebug` + `:core:database:testDebugUnitTest`（DB Migration 测试）+ `:feature:cards:testDebugUnitTest` 全绿
7. 无障碍验证（v9.0 P2-14/P2-15/P2-16/P2-17）：
   - TalkBack 开启状态下，验证"加入错题本"按钮的 `contentDescription` 正确播报
   - 点击后验证 Snackbar"已加入错题本"被 TalkBack 自动播报（`liveRegion`）
   - 验证翻转前/后屏幕阅读器焦点不混乱（不会听到两个"加入错题本"）
   - 系统设置关闭动画后，验证加载态显示静态图标而非旋转动画
   - 完成态统计行验证 TalkBack 合并读出"手动加入错题本：N 张"而非分别播报图标和文本

---

## 6. v2.0 改进清单

| # | 改进项 | 说明 | 优先级 |
|---|--------|------|--------|
| 1 | **Sibling 感知** | `manualAddedPointIds` 按 pointId 跟踪，sibling 卡自动显示"已加入" | P0 |
| 2 | **动画过渡** | `animateContentSize` 让按钮状态切换时高度平滑过渡 | P1 |
| 3 | **完成态统计** | `SessionCompleteState` 新增"手动加入错题本：N 张"统计行 | P1 |
| 4 | **无障碍** | 每种按钮状态都有对应的 `contentDescription` | P0 |
| 5 | **Snackbar 反馈** | 成功加入后显示"已加入错题本"Snackbar，错误时显示失败原因 | P0 |
| 6 | **@Preview** | 新增 `AddToWrongAnswerButtonPreview` 展示默认/已加入双态 | P1 |
| 7 | **防重入锁** | `_isAddingBookmark: MutableStateFlow<Boolean>` 防止快速连点 | P0 |
| 8 | **持久化统计** | `sessionManualAddCount` 用 SavedStateHandle 持久化，进程被杀保留 | P1 |

---

## 7. v7.0 深度审查发现（database-operations / performance-and-capacity / api-design-and-compatibility）

### 7.1 🟡 P1-9: `recordWrongAnswer` 缺少 `@Transaction` 事务保护

**问题**：`WrongAnswerRepositoryImpl.recordWrongAnswer()` 执行 read-then-write 模式：

```kotlin
// 1. Read: 查找已有未解决错题
val existing = when {
    pointId != null -> wrongAnswerDao.findUnresolvedByPointAndSource(pointId, source)
    examQuestionId != null -> wrongAnswerDao.findUnresolvedByExamQuestionAndSource(examQuestionId, source)
    else -> null
}
// 2. Write: 递增或插入
if (existing != null) {
    wrongAnswerDao.incrementWrongCount(existing.id, now)      // ← 未与 Read 在同一事务中
} else {
    wrongAnswerDao.upsert(WrongAnswerEntity(...))              // ← 未与 Read 在同一事务中
}
```

**影响**：虽然 Room 单线程执行器（`@Database` 默认单线程写）使此模式在实践中安全，但：
- 违反 Room 最佳实践：read-then-write 应当包裹在 `@Transaction` 中
- 若未来迁移到多线程数据库（如 WAL 模式 + 并发写），可能出现竞态：两个协程同时读到 `existing = null`，各自 `upsert` 两条记录
- 当前 ViewModel 中 `NonCancellable` 只能防止协程取消，不能防止 Room 内部的并发问题

**修复方案**：在 `WrongAnswerDao` 中新增 `@Transaction` 方法：

```kotlin
@Transaction
suspend fun recordWrongAnswerTransaction(
    pointId: String?,
    examQuestionId: String?,
    source: String,
    entity: WrongAnswerEntity,
    now: Long,
): String {
    val existing = when {
        pointId != null -> findUnresolvedByPointAndSource(pointId, source)
        examQuestionId != null -> findUnresolvedByExamQuestionAndSource(examQuestionId, source)
        else -> null
    }
    return if (existing != null) {
        incrementWrongCount(existing.id, now)
        existing.id
    } else {
        upsert(entity)
        entity.id
    }
}
```

然后在 `WrongAnswerRepositoryImpl.recordWrongAnswer()` 中调用此事务方法替代逐条调用。

### 7.2 🟢 P2-6: 缺少 `(point_id, source)` 及 `(exam_question_id, source)` 复合索引

**问题**：`WrongAnswerEntity` 当前索引定义：

```kotlin
indices = [
    Index("point_id"),                    // 单列索引，可覆盖 point_id 条件
    Index("exam_question_id"),            // 单列索引，可覆盖 exam_question_id 条件
    Index("resolved_at"),
    Index("sched_next_review_at"),
]
```

`findUnresolvedByPointAndSource` 查询为：
```sql
SELECT * FROM wrong_answers WHERE point_id = :pointId AND source = :source AND resolved_at IS NULL LIMIT 1
```

现有 `point_id` 单列索引可覆盖 `point_id = :pointId` 条件，但 `source = :source` 需在索引结果中逐行过滤。随着 `wrong_answers` 表增长（同一知识点可能被多次记录），缺少复合索引导致查询性能下降。

**修复方案**：新增复合索引：

```kotlin
indices = [
    Index("point_id"),
    Index("exam_question_id"),
    Index("resolved_at"),
    Index("sched_next_review_at"),
    // P2-6: 新增复合索引，加速 findUnresolvedByPointAndSource 查询
    Index("point_id", "source"),
    Index("exam_question_id", "source"),
]
```

**影响范围**：`WrongAnswerEntity.kt` 索引定义变更，需新增 DB Migration（`wrong_answers` 表索引变更）。

### 7.3 🟡 P1-10 + 🟢 P2-7/P2-8: API 文档与注释更新

**P1-10**: `WrongAnswerRepository.kt` 接口 `recordWrongAnswer` 方法的 `@param source` KDoc 只列出三个来源，需增加 `SOURCE_CARD_MANUAL`。

**P2-7**: `WrongAnswerRepository` 接口级 KDoc "支持三个来源"需改为"四个来源"。

**P2-8**: `WrongAnswerRepositoryImpl.recordWrongAnswer` 的 `when` 代码块注释只提到 `CARD_AGAIN` 和 `QUIZ_WRONG`，需补充 `CARD_MANUAL`。

### 7.4 🔵 P3-1: 性能文档 — `combine` 开销评估

**问题**：`isCurrentCardInWrongBook` 使用 `combine(_uiState, _manualAddedPointIds)`：

```kotlin
val isCurrentCardInWrongBook: StateFlow<Boolean> = combine(
    _uiState,
    _manualAddedPointIds,
) { state, addedIds ->
    val card = state.currentCard ?: return@combine false
    card.pointId.isNotBlank() && card.pointId in addedIds
}.distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)
```

**评估**：
- `_uiState` 每次卡片翻转/评分/切换时发射新值（典型会话 50 张卡，每张 2-3 次状态变化，总计 100-150 次）
- `combine` lambda 每次被调用时执行一次 `Set.contains()` 查找（O(1) 复杂度，毫秒级）
- `distinctUntilChanged()` 防止无效向下游发射（`stateIn` 不会通知无变化的 UI）
- `SharingStarted.Eagerly` 使 `stateIn` 在 `viewModelScope` 取消前一直活跃

**结论**：当前规模（< 50 张卡，Set 查找）下，性能开销可忽略。无需优化，但记录此设计决策供未来参考（若未来支持 1000+ 张卡会话，可考虑 `_manualAddedPointIds` 改用 `StateFlow<Set<String>>` 的独立订阅，避免 `_uiState` 抖动触发无谓计算）。

---

## 8. 风险与注意事项

### 8.1 风险

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| `BookmarkBorder` / `CheckCircle` 图标不存在 | 低 | 均为 Material Icons 标准图标，确认可用 |
| 翻转前按钮与"点击卡片查看答案"文案视觉冲突 | 低 | 按钮放在提示文案下方，用 `Spacing.sm` 分隔 |
| 翻转后按钮与评分按钮的视觉层次冲突 | 低 | 用 `FilledTonalButton`（非 filled），`secondaryContainer` 色，视觉权重低于评分按钮 |
| Snackbar "已加入错题本"与 Leech 警告 AlertDialog 同时弹出 | 低 | 现有 `LaunchedEffect(errorMessage, leechWarning)` 已处理：leechWarning 非空时不弹 Snackbar |
| `liveRegion = Assertive` 在所有 Android 版本上行为一致 | 低 | Compose 1.7+ 的 `LiveRegion` 语义在 Android 5.0+（API 21+）可用，minSdk=26 无兼容问题 |
| `AnimatedContent` 替代 `AnimatedVisibility` 引入动画行为变化 | 中 | 需在 emulator 上验证翻转前/后切换动画流畅度，确保 `AnimatedContent` 的 `tweenSpec` 与现有 `Crossfade` 动画协调 |
| `front.take(200)` 截断后 `userAnswer` 在错题本中上下文不足 | 低 | 截断后加"…"表明内容被截断，错题本详情页可查看完整知识点内容 |
| `require` 断言在 Release 构建中抛出 `IllegalArgumentException` | 低 | `require` 在 Release 构建中仍生效（与 `check` 不同），但 `updateManualAddedPointIds` 仅在 ViewModel 内部调用，点Id 来源为 seed data + 手动输入，不涉及外部输入。若担心断言开销，可改为 `if` 条件 + `Timber.w` |

### 8.2 注意事项

- **不修改 sessionReviewedCount/sessionAgainCount**：加错题本是独立操作，不影响会话统计
- **不修改 ratedPointIds**：加错题本不涉及 FSRS 调度
- **涉及 DB Migration**：`WrongAnswerEntity.kt` 新增复合索引需新增 Migration（v7.0 P2-6）
- **undo 时不清除 manualAddedPointIds**：错题本记录不回退
- **retry 时保留 manualAddedPointIds、重置 sessionManualAddCount**：跨轮次一致性 + 新会话从 0 开始
- **v9.0 P2-18 front 截断**：`maxFrontLength = 200` 字符，足够识别卡片内容。若未来卡片模板 front 字段长度变化，需同步调整此值
- **v9.0 P2-19 序列化校验**：`require` 断言在 Release 构建中仍生效，若担心性能影响可改为 `Timber.w` + 静默过滤
- **v9.0 P2-16 `AnimatedContent` 替代**：翻转动效从 `AnimatedVisibility` 改为 `AnimatedContent` 后，需确保 `CardReviewContent` 的动画参数（`tweenSpec`、`easing`）与现有 `Crossfade` 保持一致
- **v9.0 P2-14 Snackbar 改进**：`liveRegion` 修改影响所有 Snackbar（errorMessage + successMessage），需回归验证现有 errorMessage 流程的无障碍行为未退化

---

---

## 9. v10.0 改进清单

| # | 改进项 | 说明 | 优先级 | 来源 Specialist |
|---|--------|------|--------|-----------------|
| 1 | **Snackbar 焦点管理** | `SnackbarHost` 添加 `liveRegion = LiveRegion.Assertive`，确保屏幕阅读器播报操作结果 | P2 | accessibility-gates |
| 2 | **prefers-reduced-motion 支持** | 加载态检测 `animationScale == 0f`，静态图标替代旋转动画 | P2 | accessibility-gates |
| 3 | **翻转前/后焦点去重** | `AnimatedContent` 或 `semantics { invisibleToUser() }` 消除屏幕阅读器重复焦点 | P2 | accessibility-gates |
| 4 | **完成态统计行语义合并** | 新增行 `Icon` 设 `contentDescription = null`，利用根 Column `mergeDescendants` | P2 | accessibility-gates |
| 5 | **front 截断保护** | `userAnswer` 中 `front` 截断到 200 字符，避免存储过大文本 | P2 | client-application-security |
| 6 | **序列化格式校验** | `updateManualAddedPointIds` 添加 `require` 断言 pointId 不含逗号 | P2 | client-application-security |
| 7 | **日志数据敏感性标注** | `Timber.w` 中 `front.take(20)` 添加注释说明仅 DEBUG 构建可见 | P2 | client-application-security |
| 8 | **Compose 测试依赖声明** | `feature/cards/build.gradle.kts` 新增 `debugImplementation` 依赖 | P2 | dependency-and-code-hygiene |
| 9 | **Composable 可见性明确** | `AddToWrongAnswerButton` 保持 `private`，测试改为渲染整个 `CardsScreen` | P2 | dependency-and-code-hygiene |
| 10 | **front 控制字符过滤** | `userAnswer` 拼接前对 `front` 做控制字符过滤（`\u0000`-`\u001F`、`\u007F`） | P2 | input-validation-and-injection-defense |
| 11 | **correctAnswer 长度限制** | `extractCorrectAnswer` 返回值截断到 500 字符 | P2 | input-validation-and-injection-defense |
| 12 | **correctAnswer 空安全兜底** | 空/空白答案时使用默认占位文本 | P2 | input-validation-and-injection-defense |
| 13 | **用户数据删除机制文档化** | 记录错题本记录可解决不可删除的设计决策，评估后续添加删除功能 | P2 | privacy-and-data-lifecycle |
| 14 | **sessionManualAddCount 上限校验** | 持久化值添加 `coerceIn(0, 999)` 上限保护 | P2 | privacy-and-data-lifecycle |
| 15 | **Snackbar 持续时间优化** | `Indefinite` + 手动关闭，提升屏幕阅读器用户体验 | P3 | accessibility-gates |
| 16 | **序列化契约文档化** | `_manualAddedPointIds` 添加完整 KDoc 序列化格式说明 | P3 | client-application-security |
| 17 | **翻转前/后代码去重** | 提取 `CardActionArea` 插槽模式，减少重复代码 | P3 | dependency-and-code-hygiene |
| 18 | **多字节字符省略号策略** | `front.take(200)` 使用 `TextUtils.ellipsize` 或明确省略号字符 | P3 | input-validation-and-injection-defense |
| 19 | **错题本数据生命周期文档** | 记录 retention 策略、删除路径、用户预期 | P3 | privacy-and-data-lifecycle |

---

## 10. v9.0 深度审查发现（accessibility-gates / client-application-security / dependency-and-code-hygiene）

### 10.1 🟢 P2-14/P2-15/P2-16/P2-17 + 🔵 P3-4/P3-6: accessibility-gates 六项发现

**审查背景**：当前 `CardsScreen.kt` 已有基本无障碍语义（`contentDescription`、`mergeDescendants`、`Role`），但"加入错题本"按钮新增后引入了新的无障碍风险。

---

#### 🟢 P2-14: `AddToWrongAnswerButton` 点击后 Snackbar 缺少焦点管理

**问题**：点击"加入错题本"后，`LaunchedEffect(successMessage)` 弹出 Snackbar"已加入错题本"。但屏幕阅读器（TalkBack）用户的焦点仍停留在已禁用的按钮上，无法感知操作成功。Snackbar 的 `contentDescription` 未被自动播报。

**当前代码上下文**：
```kotlin
// CardsScreen.kt L130-141 现有错误消息 Snackbar 模式
LaunchedEffect(errorMessage, leechWarning) {
    val error = errorMessage
    if (error != null && leechWarning == null) {
        snackbarHostState.showSnackbar(error)
        viewModel.clearError()
    }
}
```

**修复方案**：在 `SnackbarHost` 中为 Snackbar 添加 `semantics { liveRegion = LiveRegion.Assertive }`，确保屏幕阅读器立即播报 Snackbar 文本。具体：

```kotlin
// 方案：在 SnackbarHost 中自定义 Snackbar 渲染
snackbarHost = {
    SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
            snackbarData = data,
            modifier = Modifier.semantics {
                liveRegion = LiveRegion.Assertive
                contentDescription = data.visuals.message
            },
        )
    }
},
```

**影响范围**：`CardsScreen.kt` SnackbarHost 渲染层。此修复同时改善现有 errorMessage Snackbar 的无障碍体验。

---

#### 🟢 P2-15: 加载态 `CircularProgressIndicator` 未检查 `prefers-reduced-motion`

**问题**：计划中 `AddToWrongAnswerButton` 的加载态使用 `CircularProgressIndicator`（旋转动画）。当用户在系统设置中关闭动画（`animatorDurationScale = 0`）时，`CircularProgressIndicator` 默认仍会旋转。Compose 的 `CircularProgressIndicator` 在 `animatorDurationScale = 0` 时自动停止，但需确认此行为在各版本 Compose 中一致。

**修复方案**：在 `AddToWrongAnswerButton` 中添加 `LocalInspectionMode` 检测，或使用 `remember` 检查 `animationScale`：

```kotlin
// 在 Composable 中检测动画状态
val animationScale = android.provider.Settings.Global.getFloat(
    LocalContext.current.contentResolver,
    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
    1f,
)
val isAnimationDisabled = animationScale == 0f
```

不过更推荐 Compose 原生方式：

```kotlin
// 在 AddToWrongAnswerButton 中使用
if (isLoading) {
    if (isAnimationDisabled) {
        // 无动画加载指示器（静态图标）
        Icon(
            imageVector = Icons.Default.HourglassTop,
            contentDescription = "正在加入错题本",
            modifier = Modifier.size(18.dp),
        )
    } else {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
        )
    }
}
```

---

#### 🟢 P2-16: 翻转前/后按钮重复导致屏幕阅读器焦点混乱

**问题**：计划中翻转前和翻转后各有一个 `AddToWrongAnswerButton`（使用 `AnimatedVisibility` 切换）。虽然同一时间只有一个可见，但 Compose 的 `AnimatedVisibility` 在视图层次中仍保留两个节点（`visible` 和 `gone` 交替）。TalkBack 用户的焦点顺序可能不连续。

**修复方案**：使用 `AnimatedContent` 替代两个独立 `AnimatedVisibility`，确保同一时间只有一个 Composable 在视图树中：

```kotlin
// 方案 A：使用 AnimatedContent 替代两个 AnimatedVisibility
AnimatedContent(targetState = uiState.isFlipped, label = "flip_area") { isFlipped ->
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (isFlipped) {
            // 翻转后：评分按钮 + 加入错题本 + 撤销/跳过
            if (isSiblingAlreadyRated) SiblingRatedHint()
            RatingButtons(...)
            AddToWrongAnswerButton(...)
            Row { ... UndoButton ... SkipButton ... }
        } else {
            // 翻转前：提示 + 加入错题本 + 撤销/跳过
            Text("点击卡片查看答案")
            AddToWrongAnswerButton(...)
            Row { ... UndoButton ... SkipButton ... }
        }
    }
}
```

**方案 B（推荐，影响更小）**：在翻转后的按钮上添加 `semantics { invisibleToUser() }` 标记，确保屏幕阅读器不会遍历到不可见按钮：

```kotlin
// 翻转后区域
AnimatedVisibility(visible = uiState.isFlipped) {
    Column {
        // ... 评分按钮 ...
        AddToWrongAnswerButton(...)  // 可见时正常交互
        // ... 撤销/跳过 ...
    }
}

// 翻转前区域
AnimatedVisibility(visible = !uiState.isFlipped) {
    Column(
        modifier = Modifier.semantics {
            // 当翻转后隐藏时，屏幕阅读器不应遍历到此区域
            if (uiState.isFlipped) invisibleToUser()
        }
    ) {
        Text("点击卡片查看答案")
        AddToWrongAnswerButton(...)
        // ... 撤销/跳过 ...
    }
}
```

---

#### 🟢 P2-17: 完成态统计行缺少 `mergeDescendants` 语义合并

**问题**：计划中 `SessionCompleteState` 新增"手动加入错题本"统计行使用 `Row` + `Icon` + `Text` 结构。当前 `SessionCompleteState` 的根 Column 已有 `semantics(mergeDescendants = true)`（L576），但新增行未利用此合并。

**影响**：由于根 Column 已设置 `mergeDescendants = true`，新增行中 `Icon` 的 `contentDescription = null` 且 `Text` 有语义内容，合并后应为屏幕阅读器读出"手动加入错题本：2 张"。但需确保 `Icon` 的 `contentDescription = null` 已在代码中明确。

**修复方案**：在新增统计行的 `Icon` 上明确设置 `contentDescription = null`（已在计划中），并结合根 Column 的 `mergeDescendants = true`，确保屏幕阅读器不会分别读出"书签图标"和"手动加入错题本：2 张"。

---

#### 🔵 P3-6: Snackbar 持续时间对屏幕阅读器用户不足

**问题**：默认 `SnackbarDuration.Short`（约 4 秒）对屏幕阅读器用户可能不足。TalkBack 用户需要时间导航到 Snackbar 位置并听取内容。

**建议**：在 `SnackbarHost` 中将 Snackbar 持续时间设为 `Indefinite`，并添加手动关闭按钮或 Action：

```kotlin
snackbarHost = {
    SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
            snackbarData = data,
            // 持续时间由 data.duration 控制，可在 showSnackbar 时指定
            modifier = Modifier.semantics {
                liveRegion = LiveRegion.Assertive
            },
        )
    }
}
```

在 `LaunchedEffect` 调用处：
```kotlin
snackbarHostState.showSnackbar(
    message = message,
    duration = SnackbarDuration.Indefinite,
    actionLabel = "关闭",
)
```

此方案适用于所有 Snackbar（errorMessage + successMessage），而非仅新增功能。建议作为 P3 优化，不阻塞当前功能实现。

---

### 10.2 🟢 P2-18/P2-19/P2-22 + 🔵 P3-5: client-application-security 四项发现

**审查背景**：文研 App 是纯本地应用（无服务器端），安全模型围绕本地数据完整性展开。客户端是攻击者可控的，但威胁模型是用户数据的隐私和完整性。

---

#### 🟢 P2-18: `userAnswer` 拼接 `card.front` 无长度上限

**问题**：计划中 `userAnswer = "手动加入：${current.front}"`，`current.front` 在当前卡片模板中可能包含长文本：
- 论述题卡（EssayPointsCard）：front 为完整论述题题目，可达 500+ 字符
- 术语解释卡（TermExplanationCard）：front 为术语名称，通常 2-20 字符
- 正常 cloze 卡：front 为句子片段，通常 50-200 字符

`WrongAnswerEntity.userAnswer` 在 Room 中定义为 `String`（映射为 SQLite TEXT，无长度限制），但极端情况（如用户手动编辑的论述题答案）可能导致 `userAnswer` 字段存储巨大文本，影响查询性能。

**修复方案**：在 `addToWrongAnswerBook()` 中对 `front` 做截断保护：

```kotlin
// 在 addToWrongAnswerBook 中
val maxFrontLength = 200
val truncatedFront = if (current.front.length > maxFrontLength) {
    current.front.take(maxFrontLength) + "…"
} else {
    current.front
}
val userAnswer = "手动加入：$truncatedFront"
```

**理由**：截断到 200 字符足够识别卡片内容（"手动加入：……"），同时避免存储过大文本。错题本详情页仍可查看完整知识点内容。

---

#### 🟢 P2-19: `manualAddedPointIds` 逗号分隔序列化缺少格式校验

**问题**：计划中 `manualAddedPointIds` 使用 `joinToString(",")` 序列化到 `SavedStateHandle`，反序列化时用 `split(",")`。当前 seed data 的 pointId 格式为 `kp_\d{5}`（不含逗号），但这是一个**隐式假设**，未在代码中明确记录。

若未来 pointId 格式变更（如引入逗号 `kp_001,abc`），序列化会静默出错：
- `joinToString(",")` → `"kp_001,abc,kp_002"` 
- `split(",")` → `["kp_001", "abc", "kp_002"]`（错误分割）

**修复方案**：在 `updateManualAddedPointIds` 和初始化时添加格式校验：

```kotlin
// 在 updateManualAddedPointIds 中
private fun updateManualAddedPointIds(newSet: Set<String>) {
    // 契约：pointId 不含逗号，确保 joinToString(",") 可逆
    require(newSet.all { it.indexOf(',') == -1 }) {
        "pointId must not contain comma: ${newSet.first { it.indexOf(',') != -1 }}"
    }
    _manualAddedPointIds.value = newSet
    savedStateHandle["manualAddedPointIds"] = newSet.joinToString(",")
}

// 初始化时
private val _manualAddedPointIds = MutableStateFlow(
    savedStateHandle.get<String>("manualAddedPointIds")?.let { raw ->
        // 校验：空字符串或纯逗号时返回 emptySet
        if (raw.isBlank() || raw.all { it == ',' }) emptySet()
        else raw.split(",").filter { it.isNotBlank() }.toSet()
    } ?: emptySet()
)
```

**影响范围**：`CardsViewModel.kt` 初始化 + `updateManualAddedPointIds` 方法。

---

#### 🟢 P2-22: `Timber.w` 日志中 `front.take(20)` 泄露用户学习内容

**问题**：v8.0 计划中新增 `Timber.w("addToWrongAnswerBook failed: blank pointId, cardId=${current.id}, front=${current.front.take(20)}")`。虽然截断到 20 字符，但 WARN 级别日志在 Release 构建中（Timber 默认仅 DEBUG 构建输出）不会泄露，但需明确记录此决策。

**审查结论**：**可接受**，理由如下：
1. Timber 在 Release 构建中默认不输出日志（`Timber.plant` 需在 Application 中配置，Release 构建通常不 plant `DebugTree`）
2. 截断到 20 字符仅包含卡片正面片断，不包含用户答案或完整学习内容
3. 20 字符片段无法直接关联到具体知识点（需结合 `cardId` 查询 DB 才能还原完整上下文）
4. 此日志仅在 pointId 为空时触发（边缘情况，非正常操作路径）

**建议**：在代码注释中明确标注此日志的数据敏感性：

```kotlin
// 日志包含 front 前 20 字符用于调试，不包含敏感用户数据
// （Release 构建中 Timber 不输出，仅 DEBUG 构建可见）
```

---

#### 🔵 P3-5: `SavedStateHandle` 序列化格式文档化

**建议**：将 `manualAddedPointIds` 的序列化契约记录在 KDoc 中：

```kotlin
/**
 * 手动加入错题本的 pointId 集合。
 *
 * 序列化格式：逗号分隔的 pointId 列表，如 "kp_001,kp_002,kp_003"。
 *
 * 契约约束：
 * - pointId 不得包含逗号（保证 joinToString(",") 可逆）
 * - pointId 应为非空字符串（空字符串在序列化时被过滤）
 * - 空集合序列化为空字符串 ""（反序列化时还原为 emptySet）
 *
 * 此契约在 [updateManualAddedPointIds] 中通过 [require] 强制校验。
 */
```

---

### 10.3 🟢 P2-20/P2-21 + 🔵 P3-5: dependency-and-code-hygiene 三项发现

**审查背景**：`feature:cards` 模块当前已有测试基础设施（JUnit 5 + MockK + Turbine），但缺少 Compose UI 测试依赖。新增 `CardsAddToWrongAnswerButtonTest.kt` 需要新增依赖和代码可见性调整。

---

#### 🟢 P2-20: Compose UI 测试依赖未在 `build.gradle.kts` 修改清单中明确

**问题**：计划 §4.3 中创建 `CardsAddToWrongAnswerButtonTest.kt` 需要 Compose UI 测试依赖，但仅在注释中提到"需在 `feature/cards/build.gradle.kts` 中新增 `debugImplementation("androidx.compose.ui:ui-test-manifest")`"，未在 §3 修改清单中明确列出。

**修复方案**：在 §3 修改清单中新增 `feature/cards/build.gradle.kts` 条目：

```kotlin
// feature/cards/build.gradle.kts dependencies 块末尾新增
// P2-20: Compose UI 测试依赖（用于 CardsAddToWrongAnswerButtonTest）
debugImplementation(libs.androidx.compose.ui.test.manifest)
// 注意：ui-test-junit4 通过 ui-test-manifest 传递依赖引入
```

首先确认 `libs.versions.toml` 中是否已有此依赖，若没有则需新增：

```toml
# gradle/libs.versions.toml
[versions]
androidx-compose-ui = "1.7.8"  # 与 BOM 对齐

[libraries]
# 新增
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest", version.ref = "androidx-compose-ui" }
```

**注意**：由于 `feature/cards` 使用 `libs.androidx.compose.bom`，`ui-test-manifest` 版本由 BOM 管理，无需单独指定版本。

---

#### 🟢 P2-21: `AddToWrongAnswerButton` Composable 可见性未明确

**问题**：`CardsAddToWrongAnswerButtonTest.kt` 中 `setContent {}` 直接引用 `AddToWrongAnswerButton` Composable。若该函数声明为 `private`（当前 `CardsScreen.kt` 中其他 Composable 如 `SiblingRatedHint`、`ProgressSection` 均为 `private`），测试文件将无法编译。

**修复方案**：在计划中明确 `AddToWrongAnswerButton` 的可见性：

```kotlin
// 方案：使用 @VisibleForTesting 注解 + internal 可见性
// 或保持 private 但通过测试的 @get:Rule createComposeRule 渲染整个 CardsScreen
// 推荐：保持 private，通过集成测试验证（渲染整个 CardsScreen 而非单个按钮）

// 若需单组件测试，使用 internal 可见性：
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun AddToWrongAnswerButton(...)
```

**推荐**：保持 `private`，通过集成测试验证 `CardsScreen` 渲染。`AddToWrongAnswerButton` 作为 `CardsScreen` 内部实现细节，不应暴露到模块外。测试改为验证 `CardsScreen` 中按钮的存在性，而非直接渲染 `AddToWrongAnswerButton`。

---

#### 🔵 P3-5: 翻转前/后 `AddToWrongAnswerButton` 代码重复

**问题**：计划中翻转前和翻转后区域各有一个 `AddToWrongAnswerButton` 调用，参数完全相同：

```kotlin
// 翻转后
AnimatedVisibility(visible = uiState.isFlipped) {
    Column {
        RatingButtons(...)
        AddToWrongAnswerButton(isInWrongBook, isLoading, pointId, onClick)
        Row { UndoButton, SkipButton }
    }
}

// 翻转前
AnimatedVisibility(visible = !uiState.isFlipped) {
    Column {
        Text("点击卡片查看答案")
        AddToWrongAnswerButton(isInWrongBook, isLoading, pointId, onClick)
        Row { UndoButton, SkipButton }
    }
}
```

**建议**：可提取为单一插槽模式，减少重复：

```kotlin
// 提取为扩展函数或辅助 Composable
@Composable
private fun CardActionArea(
    isFlipped: Boolean,
    isSiblingAlreadyRated: Boolean,
    /* ... 其他参数 ... */
    addToWrongAnswerButton: @Composable () -> Unit,
) {
    AnimatedContent(targetState = isFlipped, label = "card_actions") { flipped ->
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (flipped) {
                if (isSiblingAlreadyRated) SiblingRatedHint()
                RatingButtons(...)
            } else {
                Text("点击卡片查看答案")
            }
            addToWrongAnswerButton()
            Row { ... }
        }
    }
}
```

**影响**：低优先级优化，不阻塞当前功能实现。在 Phase 2 实施后如有余力可重构。

---

## 11. v10.0 深度审查发现（input-validation-and-injection-defense / privacy-and-data-lifecycle）

### 11.1 🟢 P2-23/P2-24/P2-25 + 🔵 P3-7: input-validation-and-injection-defense 四项发现

**审查背景**：文研 App 是纯本地离线应用，无服务器端。所有数据源（card.front / pointId / correctAnswer）来自本地 Room 数据库或 seed data，不经过网络传输。威胁模型围绕本地数据完整性展开，无 SQL 注入或命令注入风险（Room 自动参数化查询），但存在输入验证和存储优化空间。

**源到汇映射（Source-to-Sink Map）**：

| 源（Source） | 类型 | 汇（Sink） | 已有防御 | 风险等级 |
|-------------|------|-----------|---------|---------|
| `card.front` | 本地 DB 文本 | Room DB `userAnswer` | P2-18 截断 200 字符 | 低 |
| `card.front` | 本地 DB 文本 | Timber 日志 | P2-22 敏感性标注 | 低 |
| `pointId` | 本地 DB 字符串 | SavedStateHandle 序列化 | P2-19 require 逗号校验 | 极低 |
| `pointId` | 本地 DB 字符串 | Room DB `recordWrongAnswer` | Room 参数化查询 | 无风险 |
| `correctAnswer` | 本地 DB 文本 | Room DB `correctAnswer` | 无 | **中** |
| `card.front` | 本地 DB 文本 | Room DB 存储（控制字符） | 无 | **中** |

---

#### 🟢 P2-23: `front` 文本未做控制字符过滤

**问题**：`userAnswer = "手动加入：${truncatedFront}"` 中 `truncatedFront` 直接从 `card.front` 截取，未过滤不可见控制字符（Unicode 类别 `Cc`，包括 `\u0000`-`\u001F`、`\u007F`）。虽然 Room 和 SQLite 安全存储这些字符，但：
- 控制字符可能在错题本 UI 渲染时引发布局异常（如 `\u0000` 空字符、`\u0009` 制表符在 `Text` Composable 中显示为空白）
- 控制字符在日志输出中可能导致日志聚合工具解析错误（如 `\u000A` 换页符）
- 如果 `front` 文本来自用户手动导入的 OCR 结果，可能包含不可见噪声字符

**当前代码上下文**：
```kotlin
// 计划中 P2-18 修复后的代码
val maxFrontLength = 200
val truncatedFront = if (current.front.length > maxFrontLength) {
    current.front.take(maxFrontLength) + "…"
} else {
    current.front
}
// 未过滤控制字符
val userAnswer = "手动加入：$truncatedFront"
```

**修复方案**：在 `addToWrongAnswerBook()` 中对 `front` 做控制字符过滤：

```kotlin
// 在 addToWrongAnswerBook 中，truncatedFront 之后、userAnswer 拼接之前
val sanitizedFront = truncatedFront.filter { it.category != CharCategory.CONTROL }
val userAnswer = "手动加入：$sanitizedFront"
```

**理由**：Kotlin 的 `CharCategory.CONTROL` 覆盖所有 Unicode 控制字符，比手动列举 `\u0000`-`\u001F` 范围更全面（如 `\u0080`-`\u009F` C1 控制字符也被包含）。过滤操作在 ViewModel 中执行，不修改原始卡片数据。

**影响范围**：`CardsViewModel.kt` `addToWrongAnswerBook()` 方法。

---

#### 🟢 P2-24: `correctAnswer` 未做长度限制

**问题**：计划中 `addToWrongAnswerBook()` 调用 `extractCorrectAnswer(current)` 提取卡片答案文本，未对返回值做长度限制。`extractCorrectAnswer` 在不同卡片模板中返回内容差异大：
- `ClozeCard`：返回 `card.back`（简洁答案，通常 10-100 字符）
- `EssayPointsCard`：返回 `card.answer`（论述题完整答案，可达 2000+ 字符）
- `TermExplanationCard`：返回 `card.explanation`（术语解释，通常 100-500 字符）

`WrongAnswerEntity.correctAnswer` 字段为 `TEXT` 类型，无长度限制。但大段答案文本：
- 在错题本列表 UI 中默认截断显示，但数据库占用空间大
- 错题本详情页完整显示时，大段文本与用户自己写的答案混在一起，阅读体验差
- 若未来添加错题本导出功能，超长文本影响导出文件大小

**修复方案**：在 `addToWrongAnswerBook()` 中对 `correctAnswer` 做截断保护：

```kotlin
// 在 addToWrongAnswerBook 中，调用 recordWrongAnswer 之前
val maxCorrectAnswerLength = 500
val correctAnswer = extractCorrectAnswer(current)
val truncatedCorrectAnswer = if (correctAnswer.length > maxCorrectAnswerLength) {
    correctAnswer.take(maxCorrectAnswerLength) + "…"
} else {
    correctAnswer
}
```

**理由**：500 字符足够容纳大多数答案摘要（中文约 150-200 字，英文约 80-100 词）。若用户需要查看完整答案，可通过知识点详情页跳转查看。

**影响范围**：`CardsViewModel.kt` `addToWrongAnswerBook()` 方法。

---

#### 🟢 P2-25: `extractCorrectAnswer` 返回值未做 null/空安全兜底

**问题**：`extractCorrectAnswer(card)` 返回值为 `String`（非空类型），但不同卡片模板的实现可能返回空字符串或空白字符串：

```kotlin
// 假设 extractCorrectAnswer 实现类似
private fun extractCorrectAnswer(card: CardItem): String = when (card) {
    is CardItem.ClozeCard -> card.back
    is CardItem.EssayPointsCard -> card.answer
    is CardItem.TermExplanationCard -> card.explanation
    // 若未来新增卡片模板类型，未覆盖的 case 可能返回空字符串
}
```

极端情况下，新卡片模板 `when` 分支未覆盖时，Kotlin 编译器会报错（`when` 必须穷举），但 `card.back` / `card.answer` 本身可能为空字符串（seed data 质量导致）。若 `correctAnswer` 为空字符串且 `userAnswer` 也仅包含"手动加入：..."，错题本中用户看到两条近乎相同的内容，体验差。

**修复方案**：在 `addToWrongAnswerBook()` 中添加空安全兜底：

```kotlin
// 在 addToWrongAnswerBook 中，调用 recordWrongAnswer 之前
val correctAnswer = extractCorrectAnswer(current)
val safeCorrectAnswer = if (correctAnswer.isBlank()) {
    Timber.w("addToWrongAnswerBook: blank correctAnswer for pointId=$pointId, cardId=${current.id}")
    "（无答案内容）"
} else {
    truncatedCorrectAnswer // 使用 P2-24 截断后的版本
}
```

**影响范围**：`CardsViewModel.kt` `addToWrongAnswerBook()` 方法。

---

#### 🔵 P3-7: `front.take(200)` 多字节字符省略号策略

**问题**：P2-18 中 `front.take(200)` 截断后添加 `…`（省略号字符，U+2026）。Kotlin 的 `take(200)` 按字符计数，而非字节。对于中文文本，200 字符 = 200 个中文字符 = 600 字节（UTF-8），远低于 SQLite TEXT 上限。但省略号 `…` 在以下场景中可能显示不美观：
- 截断点恰好在字符中间（Kotlin 不会截断字符，但在某些字体中最后字符可能显示不全）
- `…` 字符在部分终端/日志查看器中显示为 `?` 或乱码

**建议**：明确省略号策略，可考虑：
- 方案 A（推荐）：保留当前 `take(200) + "…"` 方案，Kotlin 字符级截断安全
- 方案 B：使用 `TextUtils.ellipsize`（Android API），但需要 `Context` 传入 ViewModel，增加复杂度
- 记录设计决策，在代码注释中说明截断策略

---

### 11.2 🟢 P2-26/P2-27 + 🔵 P3-8: privacy-and-data-lifecycle 三项发现

**审查背景**：文研 App 是纯本地应用，用户是唯一的数据主体和数据控制者。所有学习数据（错题本记录、手动加入标记）仅存储在本地 Room 数据库中，不跨设备传输。隐私模型围绕用户数据自主控制展开。

**数据清单（Data Inventory）**：

| 字段 | 数据类别 | 敏感度 | 用途 | 保留策略 |
|------|---------|--------|------|---------|
| `WrongAnswerEntity.pointId` | 学习行为 | 低 | 标记哪个知识点答错 | 用户解决后标记 `resolved_at` |
| `WrongAnswerEntity.userAnswer` | 学习内容 | 中 | 记录用户答案/手动加入的上下文 | 同上 |
| `WrongAnswerEntity.correctAnswer` | 学习内容 | 中 | 展示正确答案供对比 | 同上 |
| `WrongAnswerEntity.source` | 学习行为 | 低 | 区分来源（AGAIN/手动） | 同上 |
| `SavedStateHandle.manualAddedPointIds` | 会话状态 | 极低 | 进程恢复后保持按钮禁用态 | 会话结束（retry）后清除 |
| `SavedStateHandle.sessionManualAddCount` | 会话统计 | 极低 | 完成态显示统计 | 会话结束（retry）后重置 |
| `Timber` 日志 `front.take(20)` | 学习内容片段 | 低 | 调试排查 | 仅 DEBUG 构建，不持久化 |

---

#### 🟢 P2-26: 缺少用户数据删除机制文档化

**问题**：当前错题本 UI 提供"标记为已解决"功能（`resolved_at` 时间戳），但不提供"删除记录"功能。手动加入错题本的记录一旦创建，用户**无法主动删除**，只能标记已解决。

对于纯本地应用，用户作为数据主体应能完全控制其数据。虽然当前架构中"已解决"记录在 `findUnresolvedByPointAndSource` 查询中自动过滤（不显示在活跃列表中），但数据仍存在于数据库中。

**修复方案**：此问题不阻塞当前功能实现，但需记录设计决策：

1. 在 `WrongAnswerRepository` 接口 KDoc 中记录当前删除策略：
   - 错题本记录不可删除，仅可标记为已解决（`resolved_at`）
   - 已解决记录在列表中不显示，但保留在数据库中供历史统计
   - 若用户在设置中执行"清除所有数据"，`wrong_answers` 表将被清空（通过 App 设置清除数据）

2. 在 `docs/plans/cards-add-to-wrong-answer-book.md` 中记录此决策：
   - 记录 delete 与 resolve 的区别
   - 评估后续添加"删除单条记录"功能的可行性（P4 候选）

**影响范围**：文档记录，不涉及代码修改。

---

#### 🟢 P2-27: `sessionManualAddCount` 持久化值无上限校验

**问题**：`sessionManualAddCount` 通过 `savedStateHandle.getStateFlow("sessionManualAddCount", 0)` 持久化，无上限校验。虽然 `Int` 不会溢出（2^31-1 ≈ 21 亿），但以下场景中 UI 显示不合理：
- 用户在极端情况下手动加入 1000 张卡（占满一次复习会话），完成态 UI 显示"手动加入错题本：1000 张"
- 若 `SavedStateHandle` 值被异常写入（如旧版本数据迁移 bug），可能显示极端值

**修复方案**：在 `retry()` 重置 `sessionManualAddCount` 时，以及 `addToWrongAnswerBook()` 递增时，添加上限校验：

```kotlin
// 在 addToWrongAnswerBook 中递增时
savedStateHandle["sessionManualAddCount"] = 
    (_sessionManualAddCount.value + 1).coerceIn(0, 999)

// 在 retry 中重置时
savedStateHandle["sessionManualAddCount"] = 0
```

**理由**：`coerceIn(0, 999)` 的上限 999 远高于正常会话中手动加入的卡片数（通常 < 50），同时防止异常值。

**影响范围**：`CardsViewModel.kt` `addToWrongAnswerBook()` 和 `retry()` 方法。

---

#### 🔵 P3-8: 错题本数据生命周期文档缺失

**问题**：当前整个项目中缺少错题本数据生命周期文档。用户无法预期：
- 手动加入错题本的记录会保留多久
- 如何删除不需要的记录
- 已解决记录是否会被自动清理

**建议**：在 `docs/reference/GLOSSARY.md` 或 `docs/design/app-design.md` 中补充错题本数据生命周期说明：

```markdown
### 错题本数据生命周期

| 数据 | 创建方式 | 默认保留 | 删除方式 | 清理触发 |
|------|---------|---------|---------|---------|
| 错题记录 | 手动加入 / AGAIN 评分 / 真题答错 | 永久（直到标记已解决） | 标记已解决（UI 操作） | 手动 |
| 已解决记录 | 标记已解决 | 永久（保留在 DB） | 无（App 设置清除数据可清空） | 无自动清理 |
| 会话统计 | 每次复习会话 | 会话结束重置 | 自动 | retry 或退出 |
```

**影响**：低优先级，不阻塞当前功能实现。建议在功能开发完成后补充文档。

---

## 12. 用户确认清单

在实施前，请确认以下决策：

- [ ] **按钮位置**：翻转后评分按钮下方 / 翻转前"点击卡片查看答案"下方 — 是否满意？
- [ ] **来源常量**：`CARD_MANUAL` 与 AGAIN 自动记录区分 — 是否满意？
- [ ] **用户答案文本**："（手动加入错题本）" — 是否有更好的文案？
- [ ] **已加入状态**：本会话内同 pointId 的卡均显示"已加入错题本"禁用态 — 是否满意？
- [ ] **图标**：默认 `BookmarkBorder` / 已加入 `CheckCircle` — 是否满意？
- [ ] **完成态统计**：复习完成时显示"手动加入错题本：N 张" — 是否满意？
- [ ] **Snackbar 反馈**：成功加入后弹"已加入错题本" — 是否满意？
- [ ] **retry 行为**：重开一轮后保留已加入标记，但手动加入计数从 0 开始 — 是否满意？