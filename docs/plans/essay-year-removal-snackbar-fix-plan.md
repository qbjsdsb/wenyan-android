# 论述题删年份 + Snackbar 常驻修复 — 实施计划

> 生成时间：2026-08-02
> 状态：待用户确认后执行（用户要求先调查做计划，不动手）

---

## 一、调查结果

### 问题 1：论述题年份显示位置（共 4 处 UI + 2 处模型）

| 位置 | 文件 | 说明 |
|------|------|------|
| 列表年份筛选栏 | `EssayListScreen.kt:196-218` | "全部年份" + 各年份 FilterChip（LazyRow） |
| 列表卡片年份 chip | `EssayListScreen.kt:317` | `${item.year}年` 主色 chip |
| 详情页标题年份 | `EssayDetailScreen.kt:116` | subtitle `${e.year}年 · ${e.score}分` |
| 详情页信息区年份 | `EssayDetailScreen.kt:178,258,266` | EssayHeaderSection 的 `${year}年` chip |
| 列表模型 year 字段 | `EssayListViewModel.kt:185` | EssayListItem.year |
| 筛选状态 year | `EssayListViewModel.kt:79,85,102,132-134,212` | _selectedYear / availableYears / selectYear |

**数据层不动**：`ExamQuestionEntity.year` 字段、数据库、种子数据全部保留，仅 UI 不显示。

### 问题 2："已加入错题本" Snackbar 常驻

**代码**（`CardsScreen.kt:153-160`）：
```kotlin
LaunchedEffect(successMessage) {
    val msg = successMessage
    if (msg != null) {
        snackbarHostState.showSnackbar(msg)   // ← 挂起直到 Snackbar 消失
        viewModel.clearSuccessMessage()        // ← 消失后才清状态
    }
}
```

**根因分析**：
- `showSnackbar()` 是挂起函数，默认等待 Snackbar 消失（Short ≈ 4s）后才返回
- `clearSuccessMessage()` 在 `showSnackbar()` **之后**才执行 → successMessage 在 Snackbar 显示期间一直非 null
- 若 `showSnackbar` 因 material3 1.5.0-alpha18 的 duration 计时异常而**不返回/不自动消失**，则：
  1. `clearSuccessMessage()` 永远不执行
  2. `successMessage` 永远保持 "已加入错题本"
  3. LaunchedEffect 的 key（successMessage）不变，不会重启清理
  4. **Snackbar 一直显示** —— 与用户描述完全吻合
- `errorMessage`（L145-151）是**同样的模式**，存在相同风险（用户暂未遇到）

---

## 二、实施计划

### 任务一：论述题彻底删除年份（含筛选器）— 用户已确认

**1. `EssayListScreen.kt`**
- 删除 `EssayFilterBar` 中 L196-218 年份筛选 LazyRow（`availableYears` / `selectedYear` / `onYearSelected` 参数一并删除）
- 删除 L317 列表卡片 `${item.year}年` chip（`WenyanInfoChip(text = "${item.year}年", ...)`）
- 删除 L376 / L389 @Preview 的 `year = ...` 参数
- L158 空态判断 `hasFilter` 去掉 `selectedYear != null ||`（保留 subject / onlyWithAngle）

**2. `EssayListViewModel.kt`**
- 删除 `EssayListItem.year` 字段（L185）
- 删除 `EssayListUiState.availableYears`（L212）
- 删除 `_selectedYear` StateFlow、`selectYear()`、`clearFilters()` 中的年份行（L79、L132-134、L145）
- 删除 combine 中 year 参数 + `(year == null || essay.year == year)` 筛选（L79、L82、L85）
- 删除 L102 `availableYears` 提取
- L92 `EssayListItem` 构造去掉 `year = essay.year`

**3. `EssayDetailScreen.kt`**
- L116 subtitle：`if (e.score > 0) "${e.score}分" else null`（去掉年份）
- L178 调用 `EssayHeaderSection` 去掉 `year = essay.year`
- L258 / L266：`EssayHeaderSection` 删除 `year` 参数 + 年份 chip
- L1143 @Preview 调整

**4. `EssayListViewModelTest.kt`**
- 删除/调整年份相关测试：`selectYear_filtersToMatchingYearOnly`、`selectYear_null_clearsYearFilter`、`availableYears_extractedFromAllEssays_descendingDistinct` 等

**5. 检查**：grep 确认无残留 `EssayListItem(year=`、`selectedYear`、`availableYears` 引用

### 任务二：Snackbar 修复（改顺序 + 超时兜底）— 用户已确认

**`CardsScreen.kt` L153-160 改为**：
```kotlin
// v0.9.22 修复：先清状态再显示 + withTimeout 超时兜底。
// 原实现 clearSuccessMessage 在 showSnackbar 之后，若 material3 alpha 的
// duration 计时异常导致 showSnackbar 挂起不返回，successMessage 永远非 null，
// Snackbar 永远显示。修复后：
// 1. 先 clear → successMessage 立即清空，不残留
// 2. withTimeout(5s) 兜底 → 即使 showSnackbar 挂起，5 秒后协程取消、Snackbar 被 dismiss
LaunchedEffect(successMessage) {
    val msg = successMessage
    if (msg != null) {
        viewModel.clearSuccessMessage()
        withTimeout(5_000L) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }
}
```
- 新增 import：`kotlinx.coroutines.withTimeout`、`androidx.compose.material3.SnackbarDuration`
- **顺手一并修** `errorMessage`（L145-151）的同样模式（先 clear 再 show + withTimeout）

### 验证（每项做完必须全绿再提交）

```bash
JAVA_HOME=/opt/jdk17 ./gradlew :feature:knowledge:testDebugUnitTest --no-daemon   # 年份删除
JAVA_HOME=/opt/jdk17 ./gradlew :feature:cards:testDebugUnitTest --no-daemon       # Snackbar
JAVA_HOME=/opt/jdk17 ./gradlew testDebugUnitTest --no-daemon                       # 全量
JAVA_HOME=/opt/jdk17 ./gradlew :app:assembleDebug --no-daemon                      # 编译
```

### 提交

- commit 1：`feat(knowledge): 论述题删除年份显示与年份筛选`
- commit 2：`fix(cards): 加入错题本 Snackbar 常驻修复（先清状态+超时兜底）`

---

## 三、风险与注意事项

| 风险 | 应对 |
|------|------|
| 年份筛选器删除后 `availableYears` 相关测试失败 | 同步更新 `EssayListViewModelTest` |
| Snackbar 修复用 `withTimeout` 取消 `showSnackbar` | material3 取消时会 dismiss Snackbar（finally 清理），已验证该机制 |
| 删除年份影响空态判断（`hasFilter`） | 同步调整 EssayListScreen L158 |
| 年份数据仍保留在数据库 | 无数据迁移，无兼容问题；未来如需恢复年份仅 UI 层面 |
