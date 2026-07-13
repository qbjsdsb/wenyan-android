# P1：KnowledgeViewModel 科目筛选 + 科目名显示修复 实施计划

> **状态：✅ 已完成（2026-07-13）**
>
> - commit `d1b9cd5`：DAO JOIN + ViewModel 修复 + 10 测试 + 2 Fake 补全
> - 验证：`assembleDebug` SUCCESSFUL + `testDebugUnitTest` 184 tests 0 failures（基线 174 + 新增 10）
> - 详见 [SESSION_LOG.md](../SESSION_LOG.md) Session 2026-07-13（第四条）
>
> **执行中发现并修复的 2 个计划外问题：**
> 1. Room JOIN POJO 不自动转换 snake_case → camelCase（计划假设错误，加 @ColumnInfo 显式映射修复）
> 2. 2 个 FakeKnowledgePointDao（core/ai + feature/aiassistant）未实现新方法（补全默认实现修复）

> **For agentic workers:** 本计划基于 writing-plans skill 编写。Step 使用 `- [ ]` 复选框跟踪进度。每个 Task 应独立可执行、可验证、可回滚。

**Goal:** 修复 KnowledgeViewModel 的 2 个 bug（filterByCategory 不筛选 + subject 显示 "TEXTBOOK_NATIVE"），让知识点列表的分类标签真正生效、卡片显示真实科目名（"中国古代文学"等）。

**Architecture:** 方案 A — 在 KnowledgePointDao 新增 JOIN 查询方法，返回 `KnowledgePointWithSubject` 关联数据类（KnowledgePointEntity + subjectName）。ReviewRepository 暴露新方法，KnowledgeViewModel 用新数据流替换旧的 `observeVerifiedForReview`，在内存中按 subjectName 匹配 KnowledgeCategory 实现筛选，toUiItem 直接取 subjectName 而非 contentSource。

**Tech Stack:**
- Room 2.7.0（`@Query` JOIN + `data class` 关联结果，参考 ExamRepository 的 `ExamQuestionWithSubject` 模式）
- Kotlin 2.3.10 / Hilt 2.57.1 / Coroutines Flow
- Robolectric 4.13（JVM 单元测试，SDK 34）

---

## 背景调查：当前状态与差距

### 调查项 1：数据模型断层

**事实**：
- `KnowledgePointEntity` **无 subjectId 字段**，只有 `chapterId`（外键到 chapters 表）
- `KnowledgePointEntity.kt:18` 注释明确写道："注意：本表无 subject_id 字段，通过 chapter_id 间接关联科目。"
- 唯一关联路径：`KnowledgePointEntity.chapterId → ChapterEntity.subjectId → SubjectEntity.name`
- **整条通道上没有任何 DAO 查询、@Relation、JOIN 语句实现它**（全代码库 `@Relation` 搜索 0 匹配）

### 调查项 2：三套互不相通的"科目"机制

代码库中存在 3 套互不相通的科目机制，这是 bug 的根因：

| 机制 | 位置 | 用途 | 与 SubjectEntity 关系 |
|------|------|------|----------------------|
| `SubjectEntity`（subjects 表） | core/database | 科目主数据 | 自身 |
| `ExamCodeHistoryEntity` + `ExamCodeResolver` | core/data | 真题按试卷代码判科目 | **无关**（用 exam_code_history 表） |
| `KnowledgeCategory` 枚举 | feature/knowledge | 列表页 FilterChip 标签 | **无关**（空壳筛选） |

**结论**：`SubjectEntity` 存在但从未被知识点/真题模块使用，是"孤儿表"。

### 调查项 3：两个 Bug 的精确根因

**Bug 1（filterByCategory 不筛选）**：
- `KnowledgeViewModel.kt:61-68` 的 `filterByCategory` 方法：
  ```kotlin
  if (category == KnowledgeCategory.ALL) return points
  return points  // 暂时返回全部
  ```
- **根因**：ViewModel 无法获取知识点的科目信息（因调查项 1 的数据模型断层），无法按科目过滤

**Bug 2（subject 显示 "TEXTBOOK_NATIVE"）**：
- `KnowledgeViewModel.kt:70-75` 的 `toUiItem()`：
  ```kotlin
  subject = contentSource ?: "未知"
  ```
- **根因**：`contentSource` 是内容来源标签（TEXTBOOK_NATIVE / TEXTBOOK_OCR / AI_GENERATED），**不是科目名**。因无法获取真实科目名，临时用此字段顶替

### 调查项 4：seed_data.json 的科目名与枚举 label 不匹配

**事实**：
- `seed_data.json` 中 subjects.name 是全名："中国古代文学" / "中国现当代文学" / "外国文学" / "文学理论"
- `KnowledgeCategory` 枚举 label 是简称："古代文学" / "现当代文学" / "外国文学" / "文学理论"
- **4 个中有 2 个不匹配**：古代文学、现当代文学

**影响**：即使接通了科目关联查询，若直接用 `subjectName == category.label` 匹配，古代文学和现当代文学的筛选会失败。需要在 ViewModel 中做映射（contains 匹配或硬编码映射）。

### 调查项 5：SubjectEntity.shortName 是死字段

**事实**：
- `SeedDataLoader.kt:107`：`shortName = seed.name.take(2)` — 截取前 2 字符（"中国古代文学"→"中国"）
- `SubjectEntity.kt:14` 注释声称"如'古文'"，但 `take(2)` 得到"中国"，**实现与注释不符**
- 全代码库无任何代码读取 `shortName`（Grep 验证 0 匹配）

**结论**：`shortName` 是死字段且实现错误。本次不动（YAGNI，不扩大范围），但记录供后续清理。

### 调查项 6：参考实现 — ExamRepository 的关联模式

`ExamRepository.kt:46-56` 已有成熟的"实体 + 关联信息"模式：
```kotlin
fun getExamQuestionsWithSubjectInfo(year: Int): Flow<List<ExamQuestionWithSubject>> {
    return combine(...) { questions, history ->
        questions.map { question ->
            val resolution = resolveQuestionSubject(question, year, history)
            ExamQuestionWithSubject(question, resolution)
        }
    }
}

data class ExamQuestionWithSubject(
    val question: ExamQuestionEntity,
    val subjectResolution: SubjectResolution,
)
```

**借鉴点**：Repository 层定义关联 data class，ViewModel 直接消费。但 ExamRepository 是内存关联（combine + map），本次 P1 用 DAO JOIN 更高效（SQL 层一次查询）。

### 调查项 7：KnowledgeViewModel 无测试

**事实**：
- `feature/knowledge/src/test/` 目录不存在
- 全代码库无 `KnowledgeViewModelTest` 或类似测试文件

**影响**：修复 bug 时应补测试，防止回归。参考 `AiAssistantViewModelTest.kt:370-396` 的 KnowledgePointEntity 工厂方法模式。

---

## 设计决策

### 决策 1：DAO JOIN 而非 @Relation 或 @Embedded

**选择**：`@Query` JOIN + 普通 `data class`

**理由**：
- `@Relation` 会触发 N+1 查询（Room 对每个主实体发一次查询），不适合列表场景
- `@Embedded` 只能嵌入同一表的字段，不能跨表
- `@Query` JOIN 一次查询完成，最高效，且 SQL 语义清晰

**SQL**：
```sql
SELECT kp.*, s.name AS subject_name
FROM knowledge_points kp
INNER JOIN chapters c ON kp.chapter_id = c.id
INNER JOIN subjects s ON c.subject_id = s.id
WHERE kp.ocr_status = 'VERIFIED'
ORDER BY kp.updated_at DESC
```

**注意**：用 INNER JOIN 而非 LEFT JOIN — 如果知识点没有对应科目（数据异常），不显示在列表中比显示"未知科目"更好（强制数据完整性）。

### 决策 2：关联数据类放在 core/database 还是 core/data

**选择**：`KnowledgePointWithSubject` 放在 `core/database`（与 Entity 同包）

**理由**：
- Room `@Query` 返回的 POJO 必须在编译时被 Room 处理器识别，放 core/database 让 DAO 直接返回
- 参考 `ExamQuestionWithSubject` 放在 core/data 是因为它是内存组装的（combine + map），不是 Room 查询结果
- 本次是 DAO 层 JOIN，结果类应在 core/database

### 决策 3：科目名与枚举 label 的映射策略

**选择**：在 KnowledgeViewModel 中用 `subjectName.contains(keyword)` 匹配

**理由**：
- seed_data 用全名（"中国古代文学"），枚举用简称（"古代文学"）
- `contains` 匹配最简单，无需维护映射表
- 映射规则：
  - `ANCIENT` → subjectName.contains("古代")
  - `MODERN` → subjectName.contains("现当代")
  - `FOREIGN` → subjectName.contains("外国")
  - `THEORY` → subjectName.contains("理论")

**备选方案（不选）**：在 `KnowledgeCategory` 枚举加 `keywords: List<String>` 字段 — 过度设计，YAGNI

### 决策 4：ReviewRepository 新增方法 vs 修改现有方法

**选择**：新增 `observeVerifiedWithSubject()` 方法，**不修改** `getAllVerifiedKnowledgePoints()`

**理由**：
- `getAllVerifiedKnowledgePoints()` 被 `KnowledgeViewModel` 使用，可能有其他调用者（未来）
- 新增方法不影响现有 API，可独立测试
- 旧方法标记 `@Deprecated` 会增加改动范围，YAGNI

### 决策 5：测试策略

**选择**：补 `KnowledgeViewModelTest`，覆盖 11 个场景：

**正常路径（5 个）**：ALL/ANCIENT/MODERN/FOREIGN/THEORY 分类各自返回正确结果

**边界场景（4 个）**：
1. 空列表输入 — filterByCategory 对空列表返回空列表，不报错
2. subjectName 不匹配任何 category — 返回空列表（验证 contains 不误匹配）
3. summary 有值时直接用 summary，不截断（验证不误截断人工摘要）
4. summary 为 null 时回退到 coreConclusion.take(100)（验证截断 + 内容正确）

**回归场景（2 个）**：
1. toUiItem 的 subject 字段取 subjectName 而非 contentSource（核心 bug 回归）
2. toUiItem 的 summary 为 null 时回退到 coreConclusion（验证 fallback 逻辑）

**断言库**：JUnit 原生 `org.junit.Assert.*`（项目无 Google Truth 依赖，参考 AiAssistantViewModelTest）

**简化决策**：本次只测 ViewModel 的 `filterByCategory` 和 `toUiItem` 逻辑（纯函数），不测 Flow 管道（Flow 测试需要 Turbine + 复杂 setup，性价比低）。将 `filterByCategory` 和 `toUiItem` 移到 `companion object` 并标记 `internal`，供测试直接调用。

---

## 文件结构

### 修改的文件

| 文件 | 改动 | 风险 |
|------|------|------|
| `core/database/.../dao/KnowledgePointDao.kt` | 新增 `observeVerifiedWithSubject()` @Query JOIN 方法 | 低（新增方法，不改现有） |
| `core/data/.../repository/ReviewRepository.kt` | 新增 `getVerifiedWithSubject()` 方法委托 DAO | 低（新增方法） |
| `feature/knowledge/.../KnowledgeViewModel.kt` | 改用新数据流 + 修复 filterByCategory + 修复 toUiItem + companion object | 中（核心改动） |

### 新建的文件

| 文件 | 用途 |
|------|------|
| `core/database/.../entity/KnowledgePointWithSubject.kt` | Room JOIN 查询结果 POJO |
| `feature/knowledge/src/test/.../KnowledgeViewModelTest.kt` | ViewModel 单元测试 |

### 不修改的文件

- `KnowledgePointEntity.kt` — 不加 subjectId 字段（避免数据库迁移）
- `SubjectEntity.kt` — shortName 死字段不动（YAGNI）
- `seed_data.json` — 科目名保持全名（不改数据）
- `KnowledgePointDetailScreen.kt` — 详情页不显示科目（调查项 5 已确认）
- `ExamRepository.kt` — 真题模块独立机制，不复用（调查项 3 已确认）

---

## Phase 1：DAO 层 — 新增 JOIN 查询

### Task 1: 创建 KnowledgePointWithSubject 关联数据类

**Files:**
- Create: `core/database/src/main/java/com/wenyan/app/core/database/entity/KnowledgePointWithSubject.kt`

**改动说明：**

Room `@Query` JOIN 返回的 POJO。用 `@Embedded` 嵌入 KnowledgePointEntity，外加 `subject_name` 列映射到 `subjectName` 字段。

**代码：**

```kotlin
package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * 知识点 + 科目名 关联数据类（P1 修复）。
 *
 * 由 [com.wenyan.app.core.database.dao.KnowledgePointDao.observeVerifiedWithSubject]
 * 的 JOIN 查询返回，避免 N+1 查询。
 *
 * 关联路径：knowledge_points.chapter_id → chapters.subject_id → subjects.name
 *
 * 注意：subjectName 需显式 @ColumnInfo(name = "subject_name") 映射，
 * Room 对 JOIN 查询的 POJO 不自动转换 snake_case → camelCase（已实测验证）。
 *
 * @property point 知识点实体（@Embedded 展开所有字段）
 * @property subjectName 科目全名（如"中国古代文学"），来自 subjects.name
 */
data class KnowledgePointWithSubject(
    @Embedded val point: KnowledgePointEntity,
    @ColumnInfo(name = "subject_name") val subjectName: String,
)
```

- [ ] **Step 1: 创建 KnowledgePointWithSubject.kt**

### Task 2: KnowledgePointDao 新增 observeVerifiedWithSubject 方法

**Files:**
- Modify: `core/database/src/main/java/com/wenyan/app/core/database/dao/KnowledgePointDao.kt`

**改动说明：**

新增 `@Query` JOIN 方法，SQL 一次查询返回知识点 + 科目名。不修改现有 `observeVerifiedForReview`。

**代码（新增到 KnowledgePointDao 末尾，`searchByKeyword` 之后）：**

```kotlin
    /**
     * 查询所有 VERIFIED 知识点，附带科目名（P1 修复）。
     *
     * 通过 JOIN chapters + subjects 一次查询获取科目名，避免 N+1。
     * 用 INNER JOIN：若知识点无对应科目（数据异常），不显示在列表中
     * （强制数据完整性，比显示"未知科目"更好）。
     *
     * 关联路径：knowledge_points.chapter_id → chapters.subject_id → subjects.id
     *
     * @return 知识点 + 科目名的关联列表，按 updated_at DESC 排序
     */
    @Query(
        "SELECT kp.*, s.name AS subject_name " +
            "FROM knowledge_points kp " +
            "INNER JOIN chapters c ON kp.chapter_id = c.id " +
            "INNER JOIN subjects s ON c.subject_id = s.id " +
            "WHERE kp.ocr_status = 'VERIFIED' " +
            "ORDER BY kp.updated_at DESC",
    )
    fun observeVerifiedWithSubject(): Flow<List<KnowledgePointWithSubject>>
```

- [ ] **Step 1: 在 KnowledgePointDao 末尾新增 observeVerifiedWithSubject 方法**
- [ ] **Step 2: 编译验证** `:core:database:compileDebugKotlin`

---

## Phase 2：Repository 层 — 暴露新方法

### Task 3: ReviewRepository 新增 getVerifiedWithSubject 方法

**Files:**
- Modify: `core/data/src/main/java/com/wenyan/app/core/data/repository/ReviewRepository.kt`

**改动说明：**

新增方法委托 DAO，不修改现有 `getAllVerifiedKnowledgePoints`。同时新增导入 `KnowledgePointWithSubject`。

**代码（新增到 `getAllVerifiedKnowledgePoints` 方法之后）：**

```kotlin
    /**
     * 获取所有已 VERIFIED 的知识点，附带科目名（P1 修复）。
     *
     * 供知识点浏览界面的分类筛选使用（如 [com.wenyan.app.feature.knowledge.KnowledgeViewModel]），
     * 与 [getAllVerifiedKnowledgePoints] 区别：此方法返回科目名，支持按科目过滤。
     */
    fun getVerifiedWithSubject(): Flow<List<KnowledgePointWithSubject>> =
        knowledgePointDao.observeVerifiedWithSubject()
```

**导入新增：**
```kotlin
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
```

- [ ] **Step 1: 在 ReviewRepository 新增 getVerifiedWithSubject 方法 + 导入**
- [ ] **Step 2: 编译验证** `:core:data:compileDebugKotlin`

---

## Phase 3：ViewModel 层 — 修复筛选 + 显示

### Task 4: 修改 KnowledgeViewModel — 使用新数据流 + 修复 filterByCategory + 修复 toUiItem

**Files:**
- Modify: `feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeViewModel.kt`

**改动说明：**

1. `uiState` 的数据源从 `getAllVerifiedKnowledgePoints()` 改为 `getVerifiedWithSubject()`
2. `filterByCategory` 接收 `List<KnowledgePointWithSubject>`，用 `subjectName.contains(keyword)` 匹配
3. `toUiItem` 的 `subject` 字段取 `subjectName` 而非 `contentSource`
4. 移除 `KnowledgePointEntity` 导入（不再直接使用），新增 `KnowledgePointWithSubject` 导入

**完整改动后的文件：**

```kotlin
package com.wenyan.app.feature.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.ReviewRepository
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 知识点模块 ViewModel。
 *
 * 注入 [ReviewRepository] 加载真实知识点数据。
 * 分类筛选通过 [KnowledgePointWithSubject.subjectName] 匹配 [KnowledgeCategory] 实现。
 */
@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(KnowledgeCategory.ALL)
    val selectedCategory: StateFlow<KnowledgeCategory> = _selectedCategory.asStateFlow()

    /**
     * 知识点列表：合并 Repository 数据流与分类筛选流。
     *
     * 使用 [ReviewRepository.getVerifiedWithSubject] 获取知识点 + 科目名，
     * 按 [KnowledgeCategory] 筛选后映射为 UI 项。
     */
    val uiState: StateFlow<KnowledgeUiState> = combine(
        reviewRepository.getVerifiedWithSubject(),
        _selectedCategory,
    ) { pointsWithSubject, category ->
        val filtered = filterByCategory(pointsWithSubject, category)
        KnowledgeUiState(
            isLoading = false,
            knowledgePoints = filtered.map { toUiItem(it) },
            selectedCategory = category,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KnowledgeUiState(isLoading = true),
    )

    // 切换分类标签
    fun selectCategory(category: KnowledgeCategory) {
        _selectedCategory.update { category }
    }

    companion object {
        /**
         * 按科目筛选知识点。
         *
         * 用 [KnowledgeCategory.keyword] 在 [KnowledgePointWithSubject.subjectName] 中做 contains 匹配。
         * seed_data.json 的科目名是全名（"中国古代文学"），枚举 label 是简称（"古代文学"），
         * contains 匹配可兼容两者。
         *
         * 注意：ALL.keyword 为空字符串，任意字符串.contains("") 返回 true，
         * 但为明确语义，ALL 分支显式返回全部。
         */
        internal fun filterByCategory(
            points: List<KnowledgePointWithSubject>,
            category: KnowledgeCategory,
        ): List<KnowledgePointWithSubject> {
            if (category == KnowledgeCategory.ALL) return points
            return points.filter { it.subjectName.contains(category.keyword) }
        }

        /** 将关联数据映射为 UI 项（供测试调用） */
        internal fun toUiItem(pointWithSubject: KnowledgePointWithSubject): KnowledgePointItem =
            KnowledgePointItem(
                id = pointWithSubject.point.id,
                title = pointWithSubject.point.title,
                subject = pointWithSubject.subjectName,
                summary = pointWithSubject.point.summary
                    ?: pointWithSubject.point.coreConclusion.take(100),
            )
    }
}

// 知识点 UI 状态
data class KnowledgeUiState(
    val isLoading: Boolean = false,
    val knowledgePoints: List<KnowledgePointItem> = emptyList(),
    val selectedCategory: KnowledgeCategory = KnowledgeCategory.ALL,
)

// 知识点列表项
data class KnowledgePointItem(
    val id: String,
    val title: String,
    val subject: String,
    val summary: String,
)

// 知识点分类（四科 + 全部）
// keyword 用于 subjectName.contains(keyword) 匹配，兼容 seed_data 全名与枚举简称
enum class KnowledgeCategory(val label: String, val keyword: String) {
    ALL("全部", ""),
    ANCIENT("古代文学", "古代"),
    MODERN("现当代文学", "现当代"),
    FOREIGN("外国文学", "外国"),
    THEORY("文学理论", "理论"),
}
```

**关键变更点：**

1. **数据源**：`getAllVerifiedKnowledgePoints()` (返回 `KnowledgePointEntity`) → `getVerifiedWithSubject()` (返回 `KnowledgePointWithSubject`)
2. **filterByCategory**：从 `return points`（空壳）→ `points.filter { it.subjectName.contains(category.keyword) }`；移到 `companion object` 并改为 `internal` 可见性，供测试直接调用
3. **toUiItem**：从 `subject = contentSource ?: "未知"` → `subject = subjectName`；改为 companion object 静态方法（接收参数而非扩展函数），避免扩展函数在 companion 中的调用歧义
4. **KnowledgeCategory 枚举**：新增 `keyword` 字段（"古代"/"现当代"/"外国"/"理论"），用于 contains 匹配
5. **uiState 调用调整**：`it.toUiItem()` → `toUiItem(it)`（companion object 静态方法调用）

- [ ] **Step 1: 修改 KnowledgeViewModel.kt（完整替换）**
- [ ] **Step 2: 编译验证** `:feature:knowledge:compileDebugKotlin`

---

## Phase 4：测试 — 补 ViewModel 单元测试

### Task 5: 创建 KnowledgeViewModelTest

**Files:**
- Create: `feature/knowledge/src/test/java/com/wenyan/app/feature/knowledge/KnowledgeViewModelTest.kt`

**前置检查：**
- 检查 `feature/knowledge/build.gradle.kts` 是否有测试依赖（junit / kotlinx-coroutines-test / turbine / robolectric）
- 若缺，参考 `feature/aiassistant/build.gradle.kts` 的测试依赖配置

**测试策略：**

不测 Flow 管道（需要 Turbine + 复杂 setup），只测 ViewModel 的核心逻辑。Task 4 已将 `filterByCategory` 和 `toUiItem` 移到 `companion object` 并标记 `internal`，测试可直接调用 `KnowledgeViewModel.filterByCategory(...)` 和 `KnowledgeViewModel.toUiItem(...)`。

**前置检查（必须先执行）：**

```bash
# 检查 feature/knowledge/build.gradle.kts 是否有测试依赖
grep -E "junit|turbine|coroutines-test" feature/knowledge/build.gradle.kts
```

**已核实**（2026-07-13）：feature/knowledge/build.gradle.kts 已有 `junit` + `kotlinx-coroutines-test` + `turbine`（line 57-59），**无需补充依赖**。项目用 JUnit 原生断言（`org.junit.Assert.*`），**不用 Google Truth**（libs.versions.toml 无 truth 依赖）。

**测试代码（用 JUnit 原生断言）：**

```kotlin
package com.wenyan.app.feature.knowledge

import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KnowledgeViewModel 单元测试（P1 修复）。
 *
 * 覆盖：
 * - filterByCategory 按 subjectName.contains(keyword) 筛选
 * - toUiItem 的 subject 取 subjectName 而非 contentSource
 *
 * 用 JUnit 原生断言（项目无 Google Truth 依赖，参考 AiAssistantViewModelTest）。
 */
class KnowledgeViewModelTest {

    @Test
    fun filterByCategory_ALL_returnsAllPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "中国现当代文学"),
            makePointWithSubject("kp3", "外国文学"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.ALL)
        assertEquals(3, result.size)
    }

    @Test
    fun filterByCategory_ANCIENT_returnsOnlyAncientPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "中国现当代文学"),
            makePointWithSubject("kp3", "外国文学"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.ANCIENT)
        assertEquals(1, result.size)
        assertEquals("kp1", result[0].point.id)
    }

    @Test
    fun filterByCategory_MODERN_returnsOnlyModernPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "中国现当代文学"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.MODERN)
        assertEquals(1, result.size)
        assertEquals("kp2", result[0].point.id)
    }

    @Test
    fun filterByCategory_FOREIGN_returnsOnlyForeignPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "外国文学"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.FOREIGN)
        assertEquals(1, result.size)
        assertEquals("kp2", result[0].point.id)
    }

    @Test
    fun filterByCategory_THEORY_returnsOnlyTheoryPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "文学理论"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.THEORY)
        assertEquals(1, result.size)
        assertEquals("kp2", result[0].point.id)
    }

    @Test
    fun toUiItem_subjectTakesSubjectNameNotContentSource() {
        val pointWithSubject = KnowledgePointWithSubject(
            point = makePoint(
                id = "kp1",
                contentSource = "TEXTBOOK_NATIVE",
            ),
            subjectName = "中国古代文学",
        )
        val uiItem = KnowledgeViewModel.toUiItem(pointWithSubject)
        assertEquals("中国古代文学", uiItem.subject)
        assertNotEquals("TEXTBOOK_NATIVE", uiItem.subject)
    }

    @Test
    fun toUiItem_summaryFallsBackToCoreConclusion() {
        val longCoreConclusion = "这是一段很长的核心结论，超过一百字需要被截断。".repeat(5)
        val pointWithSubject = KnowledgePointWithSubject(
            point = makePoint(
                id = "kp1",
                summary = null,
                coreConclusion = longCoreConclusion,
            ),
            subjectName = "中国古代文学",
        )
        val uiItem = KnowledgeViewModel.toUiItem(pointWithSubject)
        assertNotNull(uiItem.summary)
        assertTrue("summary should be at most 100 chars", uiItem.summary.length <= 100)
        assertEquals(longCoreConclusion.take(100), uiItem.summary)
    }

    @Test
    fun toUiItem_summaryNotNullUsesSummaryDirectly() {
        val pointWithSubject = KnowledgePointWithSubject(
            point = makePoint(
                id = "kp1",
                summary = "人工编写的简短摘要",
                coreConclusion = "这是很长的核心结论，不应该被使用".repeat(10),
            ),
            subjectName = "中国古代文学",
        )
        val uiItem = KnowledgeViewModel.toUiItem(pointWithSubject)
        assertEquals("人工编写的简短摘要", uiItem.summary)
    }

    @Test
    fun filterByCategory_emptyList_returnsEmptyList() {
        val points = emptyList<KnowledgePointWithSubject>()
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.ANCIENT)
        assertTrue("empty list should return empty", result.isEmpty())
    }

    @Test
    fun filterByCategory_subjectNameNotMatchingAnyCategory_returnsEmpty() {
        val points = listOf(
            makePointWithSubject("kp1", "未知科目"),
        )
        // 逐个测试非 ALL 分类
        KnowledgeCategory.entries.filter { it != KnowledgeCategory.ALL }.forEach { category ->
            val result = KnowledgeViewModel.filterByCategory(points, category)
            assertTrue(
                "subjectName '未知科目' should not match category $category",
                result.isEmpty(),
            )
        }
    }

    private fun makePointWithSubject(
        id: String,
        subjectName: String,
    ) = KnowledgePointWithSubject(
        point = makePoint(id = id),
        subjectName = subjectName,
    )

    private fun makePoint(
        id: String = "kp1",
        title: String = "测试知识点",
        summary: String? = "测试摘要",
        coreConclusion: String = "测试核心结论",
        contentSource: String? = "TEXTBOOK_NATIVE",
    ) = KnowledgePointEntity(
        id = id,
        chapterId = "ch1",
        title = title,
        summary = summary,
        coreConclusion = coreConclusion,
        fullContent = "",
        multiPerspectives = null,
        relatedIds = null,
        contrastIds = null,
        extensionIds = null,
        examRecords = null,
        examFrequency = "NEVER",
        termTemplate = null,
        tags = null,
        difficulty = 3,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        contentSource = contentSource,
        ocrStatus = "VERIFIED",
        sourceFile = null,
        sourcePage = null,
        studyText = null,
    )
}
```

**注意**：`filterByCategory` 和 `toUiItem` 已在 Task 4 移到 `companion object`（internal 可见性），Task 5 无需再改 ViewModel，直接创建测试文件。

- [ ] **Step 1: 检查 feature/knowledge/build.gradle.kts 测试依赖（按前置检查命令）**
- [ ] **Step 2: 若缺依赖，参考 feature/aiassistant/build.gradle.kts 补充 truth/coroutines-test/turbine**
- [ ] **Step 3: 创建 KnowledgeViewModelTest.kt**
- [ ] **Step 4: 运行测试** `:feature:knowledge:testDebugUnitTest`

---

## Phase 5：全量验证 + Commit

### Task 6: 全量编译 + 测试

- [ ] **Step 1: 全量编译** `assembleDebug`
- [ ] **Step 2: 全量测试** `testDebugUnitTest` — 预期 174 + 11 = 185 tests（原 174 + 新增 11 个 ViewModel 测试）
- [ ] **Step 3: 若测试失败，修复并重新验证**

### Task 7: Commit

```bash
git add core/database/src/main/java/com/wenyan/app/core/database/entity/KnowledgePointWithSubject.kt \
        core/database/src/main/java/com/wenyan/app/core/database/dao/KnowledgePointDao.kt \
        core/data/src/main/java/com/wenyan/app/core/data/repository/ReviewRepository.kt \
        feature/knowledge/src/main/java/com/wenyan/app/feature/knowledge/KnowledgeViewModel.kt \
        feature/knowledge/src/test/java/com/wenyan/app/feature/knowledge/KnowledgeViewModelTest.kt

git commit -m "fix(knowledge): 修复科目筛选不生效 + subject 显示 TEXTBOOK_NATIVE

Bug 1 (filterByCategory 不筛选):
  根因：KnowledgePointEntity 无 subjectId 字段，ViewModel 无法获取科目信息，
  filterByCategory 暂时返回全部。
  修复：KnowledgePointDao 新增 observeVerifiedWithSubject() JOIN 查询
  （knowledge_points JOIN chapters JOIN subjects），返回 KnowledgePointWithSubject。
  ViewModel 用 subjectName.contains(category.keyword) 实现筛选。

Bug 2 (subject 显示 TEXTBOOK_NATIVE):
  根因：toUiItem 的 subject 字段取 contentSource（内容来源标签）而非科目名，
  因数据模型断层临时用此字段顶替。
  修复：toUiItem 改为取 KnowledgePointWithSubject.subjectName（如'中国古代文学'）。

配套改动:
  - KnowledgeCategory 枚举新增 keyword 字段（'古代'/'现当代'/'外国'/'理论'），
    用于 contains 匹配，兼容 seed_data 全名与枚举简称。
  - ReviewRepository 新增 getVerifiedWithSubject() 委托方法。
  - 新增 KnowledgeViewModelTest（11 tests）覆盖筛选 + 显示逻辑：
    5 正常路径（ALL/ANCIENT/MODERN/FOREIGN/THEORY）+ 4 边界场景（空列表/
    不匹配/summary有值不截断/summary为null回退）+ 2 回归场景。
  - filterByCategory + toUiItem 移到 companion object（internal）供测试调用。

为什么：SeedDataLoader 接通后，App 启动会导入 12 个知识点（4 科目 × 3），
但列表页的分类标签点击无效，卡片显示 'TEXTBOOK_NATIVE' 而非'古代文学'。
这是用户可见的功能缺陷，P0 修复后立即可感知。"
```

---

## Phase 6：Push + CI 验证 + 文档

### Task 8: Push + CI 验证

- [ ] **Step 1: git push origin main**
- [ ] **Step 2: 等待 CI（约 20 分钟）**
- [ ] **Step 3: 验证 CI 全绿** `gh run view --repo qbjsdsb/wenyan-android`

### Task 9: 更新文档

**Files:**
- Modify: `docs/00-STATUS.md`（新增 P1 章节 + 更新优先级）
- Modify: `docs/SESSION_LOG.md`（新增 Session 第四条）
- Modify: `AGENTS.md`（第 7/8/9 节更新）
- Modify: `docs/plans/p1-knowledge-viewmap-subject-fix.md`（顶部标记完成）

- [ ] **Step 1: 更新 4 个文档**
- [ ] **Step 2: Commit 文档**
- [ ] **Step 3: Push**

---

## 自检清单

### 计划完整性
- [x] 每个 Task 有明确的文件路径
- [x] 每个 Task 有具体的代码改动（非占位符）
- [x] 每个 Phase 末尾有验证关卡
- [x] Commit message 说明"为什么改"

### 深度调查发现的关键约束（已纳入计划）
- [x] KnowledgePointEntity 无 subjectId 字段，唯一路径 chapterId→subjectId→name（已读 entity 确认）
- [x] 全代码库无 @Relation / KnowledgePointWith*（Grep 验证 0 匹配）
- [x] seed_data.json 科目名是全名，枚举 label 是简称，4 个中 2 个不匹配（已读 seed_data 确认）
- [x] SubjectEntity.shortName 是死字段且 take(2) 实现错误（本次不动，YAGNI）
- [x] ExamRepository 的 ExamQuestionWithSubject 是内存组装，本次用 DAO JOIN 更优（已读 ExamRepository 确认）
- [x] KnowledgeViewModel 无测试（Glob 验证 test/ 目录不存在）
- [x] ReviewRepository 现有 getAllVerifiedKnowledgePoints 不修改（新增方法，向后兼容）
- [x] feature/knowledge/build.gradle.kts 已有 junit + coroutines-test + turbine（line 57-59，无需补依赖）
- [x] 项目用 JUnit 原生断言，不用 Google Truth（libs.versions.toml 无 truth 依赖，参考 AiAssistantViewModelTest）
- [x] feature/cards 和 KnowledgePointDetailScreen 不显示科目（调查项 4/5 确认，无连带改动）
- [x] KnowledgeCategory 枚举 label 只在 KnowledgeScreen.kt:125 被引用（加 keyword 字段不影响调用者）

### 风险评估
- **Phase 1 风险：低** — 新增 DAO 方法 + 新建 data class，不改现有代码。Room 编译期会校验 SQL 语法。
- **Phase 2 风险：低** — Repository 新增委托方法，1 行代码。
- **Phase 3 风险：中** — 修改 ViewModel 是核心改动，但有测试覆盖。KnowledgeCategory 枚举加字段是 breaking change（构造函数签名变化），但全代码库只有 KnowledgeScreen.kt 引用 label，不影响。
- **Phase 4 风险：低** — 新建测试文件，不影响生产代码。companion object 调整是可见性变更，不影响运行时行为。
- **Phase 5 风险：低** — 全量验证 + commit。
- **回滚方案** — 单个 commit，可 `git revert`。

### 预期结果
- 修改 3 个文件 + 新建 2 个文件
- CI 全绿
- 知识点列表的分类标签点击后正确筛选（"古代文学" 只显示古代文学知识点）
- 卡片显示 "中国古代文学" 而非 "TEXTBOOK_NATIVE"
- 新增 11 个测试（基线 174 → 185）：5 正常路径 + 4 边界场景 + 2 回归场景

### 后续验证（需真机/模拟器）
- 启动 App 后进入知识点 Tab
- 点击"古代文学"分类标签，确认只显示 3 个古代文学知识点
- 确认卡片科目字段显示"中国古代文学"等全名
- 切换其他分类标签验证筛选生效

### 已知限制（本次接受）

1. **KnowledgePointEntity 无 subjectId 字段**：通过 JOIN 查询绕过，不改表结构（避免数据库迁移）。后续若性能瓶颈可考虑加 subjectId 冗余字段。

2. **SubjectEntity.shortName 死字段**：本次不动（YAGNI），记录到 SESSION_LOG 供后续清理。`SeedDataLoader.kt:107` 的 `take(2)` 实现也不正确（"中国古代文学"→"中国"而非"古文"），但既然无人读取，不影响。

3. **contains 匹配的脆弱性**：`subjectName.contains("古代")` 在"中国古代文学"上匹配成功，但若未来出现"古代文论"会误匹配。当前 4 科目无歧义，接受。测试 `filterByCategory_subjectNameNotMatchingAnyCategory_returnsEmpty` 覆盖了不匹配场景。

4. **filterByCategory + toUiItem 移到 companion object**：为可测试性的最小妥协。更优方案是提取到独立 mapper 类（如 `KnowledgePointMapper`），但 YAGNI。

5. **INNER JOIN 数据完整性风险**：若知识点的 `chapterId` 指向不存在的 chapter（数据异常），INNER JOIN 会过滤掉该知识点，用户看不到。与旧方案（`observeVerifiedForReview` 单表查询）相比，这是行为变更。**风险评估**：MVP 阶段无用户添加知识点功能，且 SeedDataLoader 导入时已保证外键完整性（chapter 和 knowledge_point 在同一事务中按顺序导入），风险极低。若未来允许用户手动添加知识点，需考虑用 LEFT JOIN + 空科目名降级显示。

6. **架构职责不完美（既有问题，本次不改）**：`getVerifiedWithSubject()` 放在 ReviewRepository（复习仓库）职责不完美——知识点浏览更应在 KnowledgeRepository（知识点仓库）。但当前 `getAllVerifiedKnowledgePoints()` 也在 ReviewRepository，这是既有设计问题。**本次不改的理由**：P1 是修 bug，不是重构；改 ViewModel 依赖（ReviewRepository → KnowledgeRepository）会扩大改动范围，且让 `getAllVerifiedKnowledgePoints()` 变成死代码。记录到 SESSION_LOG 供后续重构。

7. **ReviewRepository.getAllVerifiedKnowledgePoints 将变成事实上的死代码**：KnowledgeViewModel 改用 `getVerifiedWithSubject()` 后，`getAllVerifiedKnowledgePoints()` 不再有调用者（Grep 验证仅 KnowledgeViewModel 引用）。本次不删除（保留 API 向后兼容，未来可能有其他调用者），但记录到 SESSION_LOG。
