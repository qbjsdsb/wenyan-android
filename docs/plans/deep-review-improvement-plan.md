# 文研 App 深度检查改进计划（v0.9.22 审查）

> 生成时间：2026-08-02
> 检查范围：全仓库（core/database、core/data、core/fsrs、core/designsystem、feature/*、构建配置、CI/CD、文档）
> 方法：3 路并行深度代码审查 + 关键问题人工复验

## 一、总体评价

**做得好的地方**（已核实）：
- 架构规范：Hilt 依赖注入 + 单向数据流 + `retryTrigger` 重订阅模式统一，11 个 ViewModel 均正确封装可变 StateFlow
- 迁移链完整：v1→v8 全部 7 个迁移在 `DatabaseModule` 注册，无缺口
- Compose 性能意识强：LazyColumn 全部设置 `key` + `contentType` + `animateItem`，catch 均移入 `flatMapLatest` 内部保证 retry 可用
- 无障碍优秀：`mergeDescendants` + `Role.Button` + liveRegion + 语义化 contentDescription
- 发布防呆已加固：tag↔versionName 校验 + `update_release: true`（v0.9.22 已提交）

**本次审查未发现确定性崩溃路径（P0），但存在 2 个 P1 功能缺陷、若干 P2 隐患。**

---

## 二、问题清单（按优先级）

### P1 — 高优先级（影响用户体验/发布就绪）

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| P1-1 | **COMPACT 底部 double inset**：外层 Box `padding(bottom = 80dp + 手势区)` 与内层 Scaffold 默认 `contentWindowInsets`（含底部 systemBars）双重叠加 | `WenyanAdaptiveNavigation.kt:99,150` + 各 Screen | 列表底部多出一个手势区高度的空白——**正是用户之前反复抱怨"底栏上方大面积空白"的残留问题** |
| P1-2 | **ThemeViewModel.errorEvents 无消费者**：主题保存失败时错误被静默丢弃（tryEmit 无订阅者必然失败） | `ThemeViewModel.kt:41-42,80` | 用户改主题失败无任何提示 |
| P1-3 | **版本未提升**：HEAD 为 v0.9.22 清理提交，但 versionCode 仍 46 / versionName "0.9.21" | `app/build.gradle.kts:66,127` | 直接打 v0.9.22 tag 会被新校验 fail-fast，发布前必须先提升 |

### P2 — 中优先级（功能缺陷/隐患）

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| P2-1 | **FSRS `nextRecallStability` s=0 产生 NaN**：`s.pow(-w[9])` 当 s=0 → Infinity → `0*Infinity`=NaN，写回污染后续调度 | `FsrsWrapper.kt:368-377` | 老数据（v1 时代 stability 默认 0）可能触发；LEARNING/HARD/GOOD/EASY 均走此路径 |
| P2-2 | **MIGRATION_7_8 缺两个复合索引**：Entity 定义 6 索引，迁移只建 4 个；新装用户 6 个、升级用户 4 个 | `Migration_7_8.kt` + `WrongAnswerEntity.kt:56-57` | 升级用户 `findUnresolvedBy*AndSource`（每次答错都执行）性能退化；计划文档已要求但未落实 |
| P2-3 | **WrongAnswerViewModel DUE 评分无防重入**：DB 流刷新前连点两个评分按钮会重复 FSRS 调度 | `WrongAnswerViewModel.kt:190-201` | 双击导致重复调度、间隔异常 |
| P2-4 | **recordWrongAnswer 并发重复插入窗口**：查找事务与 upsert 事务分离，并发下可能插入两条重复错题 | `WrongAnswerRepositoryImpl.kt:52-77` | 低概率但存在 |
| P2-5 | **时间源不一致**：`recordWrongAnswer` 用 `System.currentTimeMillis()`，FSRS 调度用 `ClockGuard.effectiveNowMillis()` | `WrongAnswerRepositoryImpl.kt:49,81` | 时钟回拨时错题时间戳与调度时间源不一致 |
| P2-6 | **release-assets 4 个 APK 入库 77MB**：`wenyan-latest.apk` 实为 v0.8.16 产物（与 v0.8.16 同大小），严重过期且有误导性 | `release-assets/` | `.git` 膨胀到 30MB；二进制不应入库 |
| P2-7 | **AGENTS.md/docs 多处过期**：当前状态仍写 v0.9.18/43；混入 OCR 项目遗留约束（Koa 2.x、D:\wenyan、conda 'ocr'） | `AGENTS.md:83-87,188-207` 等 | 严重误导 AI 接手者 |
| P2-8 | **EssayList/ApiConfig ViewModel `stateIn(WhileSubscribed)` Tab 返回闪烁** | `EssayListViewModel.kt:124`、`ApiConfigViewModel.kt:111` | 离开 Tab >5s 返回先闪 loading |

### P3 — 优化建议（可后续分批做）

| # | 问题 | 位置 |
|---|------|------|
| P3-1 | R8/ProGuard 未启用（AGENTS.md 已知待办，需 emulator 实测后开启） | `app/build.gradle.kts:163` |
| P3-2 | 11 个模块 android{} 块 + 依赖块大量重复（~200 行），应抽 convention plugin | 各模块 build.gradle.kts |
| P3-3 | schemas 缺 1.json/3.json + 无 MigrationTestHelper 迁移测试 | `core/database/schemas/` |
| P3-4 | 5 处 DAO 查询列缺索引（question_type/answer_status/content_source/ocr_status） | `ExamQuestionDao` 等 |
| P3-5 | 导航参数未 URL 编码、无深链、子路由常量未统一收敛 | `WenyanNavHost.kt` |
| P3-6 | 详情页 Column+forEach 全量渲染，应改懒加载 | `KnowledgePointDetailScreen` 等 |
| P3-7 | release.yml：校验步骤前移、加 concurrency、加 fail_on_unmatched_files | `release.yml` |
| P3-8 | 全 UI 硬编码字符串（i18n 能力缺失） | 各 Screen |
| P3-9 | 多步写无事务（ChatRepository.appendMessage、StudyProgressRepository.recordStudySession） | core/data |
| P3-10 | `QuizAnswerState()` 每次重组分配、硬编码 dp、SimpleDateFormat 时区、`remember{LocalDate.now()}` 跨天不刷新 | 各 Screen |

---

## 三、建议执行顺序（分批打磨，每批验证后再进下一批）

### 批 A — 发布就绪 + 用户可感知缺陷（推荐立即做）
1. **P1-1** 修复 COMPACT 底部 double inset（内层 Scaffold 统一传 `contentWindowInsets = WindowInsets(0)` 或外层只 pad 80dp）
2. **P1-2** 给 SettingsScreen 增加 `errorEvents` 消费者（Snackbar 提示）
3. **P1-3** 提升 versionCode 47 / versionName "0.9.22"
4. 构建验证（assembleDebug + 全部单测）→ commit → push

### 批 B — 数据正确性加固（推荐第二批）
5. **P2-1** FSRS `nextRecallStability` 加 s<=0 防御（`s.coerceAtLeast(0.1f)` 或返回 initStability）
6. **P2-2** MIGRATION_7_8 补两个复合索引 + 同步 schema（或新增 8→9 迁移）
7. **P2-4** recordWrongAnswer 整体放入一个 DAO @Transaction
8. **P2-5** recordWrongAnswer 改用 ClockGuard 时间源
9. **P2-3** WrongAnswerViewModel 加 isRating 防重入锁
10. 构建验证 → commit → push

### 批 C — 仓库卫生（推荐第三批）
11. **P2-6** 从 git 移除 release-assets 4 个 APK（git rm --cached + .gitignore + 可选 git filter-repo 瘦身）
12. **P2-7** 更新 AGENTS.md 当前状态 + 删除 OCR 遗留约束
13. **P2-8** stateIn 改 Eagerly 或保持 collect 模式
14. 构建验证 → commit → push

### 批 D — 长期优化（可后续按需做）
15. **P3-1** R8 启用（先 emulator 实测）
16. **P3-2** convention plugin 抽取
17. **P3-3** 补历史 schema + 迁移测试
18. **P3-4~10** 各项优化

---

## 四、验证方法（每批做完必须全绿再提交）

```bash
# 1. 全量单测
./gradlew testDebugUnitTest --no-daemon
# 2. 全量编译（含 Room schema export 校验）
./gradlew :app:assembleDebug --no-daemon
# 3. 若涉及 schema：确认 build/intermediates/room/schemas 与源码 schemas 一致
# 4. git status 确认无意外变更 → commit → push
```
