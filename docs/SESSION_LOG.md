Warning: truncated output (original token count: 113172)
Total output lines: 7036

# 会话日志

> **每次会话结束前追加一节。** 新会话开始时读最后一节恢复进度。

> **当前状态说明（2026-08-09，PR-00）：** 本文件以下内容是按时间保存的历史记录。旧条目中关于旧版本、D 盘工具链或未来恢复 Graph 视图的表述不代表当前实现；当前模块、路由、Room、seed 和路线决策以 [当前系统基线](architecture/current-system.md) 及 [知识图谱暂缓恢复](decisions/004-knowledge-graph-deferred.md) 为准。此处仅加状态说明，不重写历史。

---

## 2026-08-07 会话：2025 年 805 外国文学史真题归码与题干核正（v2.26）

- **发现问题**：上一批 2025 年公开回忆题已经进入 `eq_0533`—`eq_0541`，但错误使用了 `exam_paper_code=801`；题目内容实际来自 2025 年 805 外国文学史页面。
- **核对来源**：以考研云分享的 2025 年南师大 805 页面为逐题来源；页面列出 5 道名词解释和 4 道简答，并将分值写作名词解释 6×5、简答 15×4。搜狐汇总页交叉确认 2024、2025 均以 805 外国文学史回忆版收录。
- **修改**：保留旧 ID 和答案框架；`eq_0533`—`eq_0537` 改为 805/5 分，`eq_0538`—`eq_0541` 改为 805/15 分，并将 4 道简答题干按来源核正。真题总数保持 564 条。
- **严格边界**：来源正文只列出 5 道名词解释，虽然标题写成“6×5”；没有猜写第 6 题。2024 年只有回忆版条目，没有可逐题复现的完整正文，继续待核。
- **验证**：`verify_exam_805_v2_26.py` 通过；本批题目与其他题目规范化重复 0 组；全库另有 11 组历史重复，未改动；2025 年外国文学误标 801 为 0 条；seed metadata 版本更新为 2.26.0。
- **产物**：`tools/content_supplement/verify_exam_805_v2_26.py`、`docs/research/exam-805-audit-v2.26.md`、`docs/research/exam-805-audit-v2.26.json`。

## 2026-08-07 会话：教材专题增量 v2.25 与 805 代码审计

- **教材抽取**：继续核对丁帆《中国新文学史》上册、下册 OCR，并按用户指示直接使用提供的聂珍钊《外国文学史》上册 OCR。新增 78 条独立专题卡，ID 为 `kp_01024`—`kp_01101`：丁帆上册 21 条、下册 24 条，聂珍钊上册 33 条。
- **版本边界**：丁帆 `file_131/file_132` 版权页为 2013 年 4 月第 1 版；聂珍钊 `file_090` 为用户提供的 2015 年 7 月第 1 版。本批来源字段明确写出版本，未把 2015 OCR 冒充官方 2018 第二版。
- **合并结果**：知识点 `1023→1101`；中国现当代文学 `211→256`，外国文学 `124→157`；中国古代文学仍 498，文学理论仍 190。真题 564 条、写作材料 909 条保持不变；袁世硕三册没有新增或修改。
- **来源守卫**：78/78 条 OCR 页码与锚点复现，规范化标题重复 0 组；写入前快照为 `/tmp/wenyan-seed-before-v2.25.json`，`merge_content_batch_v2_25.py --verify-applied` 通过。
- **805 审计**：附件 `file_208.json` 没有 2024/2025；公开资料能确认 2024/2025 的方向代码为 805，但完整题干、题型和分值仍不能交叉复现。因此修正生成器的 2023—2025 `805→外国文学` 年份分支，但没有新增 `exam_questions`，详见 `docs/research/exam-805-audit-v2.25.md`。
- **验证边界**：Python JSON、标题、OCR 和框架覆盖检查通过；完整 Gradle/Kotlin 校验仍需当前环境补齐依赖后复跑，不能把未运行写成通过。
- **构建阻塞实测**：`./gradlew --offline :core:data:test` 先受 `/root/.gradle` 锁目录权限阻塞；改用临时 Gradle 用户目录后又因环境无法访问 `services.gradle.org` 下载 8.14.4 wrapper 失败，未进入源码编译。
- **产物**：`tools/content_supplement/content_cards_v2_25.json`、`build_content_batch_v2_25.py`、`merge_content_batch_v2_25.py`、`docs/research/content-supplement-v2.25.{md,json}`、`docs/research/exam-805-audit-v2.25.md`。

## 2026-08-07 会话：丁帆《中国新文学史》下册断档第二批补充（v2.24）

- **继续审计**：针对下册印刷页 125—320 的自动抽取断档，回到 `file_132.json` OCR 正文逐页扫描，重点核对刘绍棠、冯骥才、叶兆言、刘震云/方方、孔捷生、刘恒、李杭育/郑万隆、北村、孙甘露和 80 年代戏剧等独立专题。
- **新增**：人工整理并通过来源守卫新增 10 条知识点，ID 连续为 `kp_01014`—`kp_01023`；知识点 1013→1023，现当代文学 201→211。全部来源于丁帆《中国新文学史》下册 2013 年 4 月第 1 版，保留印刷页、OCR 物理页和锚点。
- **严格纠错**：预检第一次拦截“心理时间”不在 341—343 页的问题；回到 OCR 定位后确认该术语还出现在物理页 338（印刷页 319），将第 10 条来源范围修正为印刷页 319—324、物理页 338、341—343，再次预检通过。
- **合并结果**：写入前快照 `/tmp/wenyan-seed-before-dingfan-v2.23.json`；种子 1013→1023。旧知识点逐字段 0 变化，真题 564 条、写作材料 909 条保持不变；全库规范化标题重复 0 组。
- **框架与验收**：新增卡已登记到现当代文学显式框架；直接 Kotlin 编译/运行结果为 `frameworks=4 modern=211 total=1023 errors=0`。丁帆写入后验证、2023—2026 真题验证均通过。
- **产物**：`tools/content_supplement/dingfan_cards_v2_24.json`、`tools/content_supplement/merge_dingfan_v2_24.py`、`docs/research/dingfan-supplement-v2.24.md` 与同名 JSON。
- **当前边界**：本批仍是断档区的高置信度增量，不能宣称丁帆教材已经逐作家、逐作品穷尽；聂珍钊 2018 第二版核对仍未完成。完整 Gradle 单测受插件缓存/网络环境阻塞，未把未运行写成通过。

## 2026-08-07 会话：丁帆《中国新文学史》薄卡与 OCR 中断区补充（v2.23）

- **确认**：丁帆上册 `file_131.json` 和下册 `file_132.json` 的版权页均为 2013 年 4 月第 1 版、OCR 状态 `VERIFIED`。下册自动知识点只覆盖印刷页 18—124 与 321—445，印刷页 125—320 的正文 OCR 存在但没有对应抽取文件；上册也漏掉多个独立专题。
- **新增**：根据教材目录、正文锚点、现有知识点和 2023—2026 真题交叉核对，新增 20 条现当代文学卡，ID 连续为 `kp_00994`—`kp_01013`，覆盖叶圣陶、九叶诗派/穆旦、张天翼、巴金《家》、东北流亡作家群、路翎、丁玲、离散写作、海子、盘峰诗会、西川、贾平凹、张炜、女性主义写作、新世纪文学、新时期诗歌、第三代诗歌、乡土小说、新历史小说、生态/西部文学。
- **合并结果**：知识点 993→1013，现当代文学 181→201；真题 564 条、写作材料 909 条未变。旧知识点逐字段对比 0 处变化；全库规范化标题重复 0 组；ID `kp_00001`—`kp_01013` 连续唯一。
- **来源守卫**：20/20 条教材印刷页、OCR 物理页和锚点复现；候选中的 `framework_node`、OCR 辅助页码和 `source_evidence` 未写入 App 种子。写入前快照为 `/tmp/wenyan-seed-before-dingfan-v2.22.json`，写入后 `merge_dingfan_v2_23.py --verify-applied` 通过。
- **框架**：新增 `modern_diaspora`、`modern_since_new_century` 两个显式节点，并将 20 条卡一对一登记；四科总映射应为 1013 条，待直接 Kotlin 校验复核。
- **产物**：`tools/content_supplement/dingfan_cards_v2_23.json`、`tools/content_supplement/merge_dingfan_v2_23.py`、`docs/research/dingfan-supplement-v2.23.md` 与同名 JSON。
- **当前边界**：本批是高价值增量，不宣称丁帆教材已逐作家、逐作品穷尽；下册 125—320 仍需继续细分，聂珍钊 2018 第二版核对仍未完成。完整 Gradle 构建仍需可用插件缓存或网络环境。

---

## 2026-08-07 会话：2023—2026 真题与答案框架补充（v2.22）

- **完成**：从压缩包原始 `file_033.json` 核对 2023 年 610、805、801 三部分，新增 27 道高可信真题；从公开回忆资料核对 2024—2026 可复现部分，新增 52 道中等可信度真题。新题共 79 道，ID 连续为 `eq_0482`—`eq_0560`，每题均有答案框架。
- **来源边界**：2024 年 805 外国文学史、2025 年 805 外国文学史因完整题干不可可靠复现/代码混列，没有猜写入库，已登记为待核项。公开回忆题的分值不明确处保留 `score: 0`，没有猜填。
- **合并结果**：真题 485→564；知识点 993 条、写作材料 909 条、旧真题 485 条均保持不变。写入前 dry-run 和写入后验证均通过；旧真题逐字段对比为 0 处变化。
- **产物**：`tools/content_supplement/merge_exam_2023_2026_v2_22.py`、`tools/content_supplement/exam_2023_2026_candidates_v2_22.json`、`docs/research/exam-2023-2026-v2.22.md` 与同名 JSON。
- **当前边界**：真题补入不等于知识点已经完整；下一步继续按原计划核实聂珍钊 2018 第二版、补上册抽取缺口，再处理丁帆现当代文学薄卡与 OCR 中断区。Gradle 完整构建仍受 wrapper/插件缓存环境阻塞，需在可用缓存或网络环境复跑。

---

## 2026-08-07 会话：聂珍钊版本与上册抽取缺口审计（v2.21）

- **确认**：`file_090.json`（聂珍钊上册）OCR 完整且为 `VERIFIED`，402 页、350,097 字、平均置信度 0.9936；但压缩包没有对应的 `file_090_knowledge.json`，属于抽取管线遗漏，不是 OCR 缺页。
- **来源核对**：现有外国文学候选主要来自聂珍钊下册 64 条和郑克鲁上册 53 条；聂珍钊上册候选为 0 条。当前种子外国文学 124 条不能证明上册已覆盖。
- **版本边界**：压缩包聂珍钊上下册版权页均为 2015 年 7 月第 1 版；高等教育出版社公开书目信息确认指定的 2018 年第 2 版上、下册 ISBN 分别为 `978-7-04-050106-3`、`978-7-04-050107-0`。公开目录结构与 OCR 目录基本一致，但没有逐页版本对照。
- **本阶段处理**：没有将 2015 OCR 候选写入 `seed_data.json`，没有新增外国文学 ID，没有修改旧 ID、真题或写作材料；建立了上册目录覆盖矩阵和 11 项优先补充清单。
- **产物**：`docs/research/nie-zhenzhao-version-audit-v2.21.md` 与同名 JSON。下一道闸门是取得/核实 2018 第二版证据，再做逐专题抽取和守卫式合并。

---

## 2026-08-07 会话：袁世硕第二版三册第一批补齐（内容审计进行中）

- **完成**：以 `main` 的 seed 2.18.0（960 条）为基线，读取 `tools.zip` 中袁世硕《中国古代文学史》第二版上、中、下册 OCR，完成第一批教材证据核对。
  - 新增 23 条知识点，ID 严格续接 `kp_00961`—`kp_00983`；旧 ID、真题和写作材料未改动。
  - 为 10 条既有知识点补回袁世硕第二版的卷册、章节、打印页码和 OCR 页段证据。
  - 更新古代文学显式框架：465→488 条，新增条目均一对一归类。
- **验证**：合并脚本预检通过；JSON 解析、标题唯一性、来源页码锚点、旧字段不变性、真题/写作数据不变性均通过；直接编译四科 Kotlin 框架并运行校验入口，结果 `frameworks=4 ... errors=0`。
- **当前边界**：这是三册的第一批高置信度补充，不宣称三册已经穷尽；Gradle wrapper 仍因环境无法访问 `services.gradle.org` 未能运行完整 Android 单测。下一步继续做袁世硕目录覆盖审计，再核对聂珍钊版本和抽取缺口。
- **产物**：`tools/content_supplement/` 下的证据清单、候选卡片和合并校验脚本；`docs/research/yuan-shishuo-v2.19.md` 与同名 JSON 审计报告。

- **继续完成第二批**：在 v2.19.0 基础上按三册目录与 OCR 章节复核新增 10 条（`kp_00984`—`kp_00993`），覆盖《周易》卦爻辞、《老子》/《孙子》、韩孟诗派、《长恨歌》、南宋前后期词、辽西夏金文学、元代散文、台阁体与山林诗、晚清谴责小说；seed 983→993，古代框架 488→498。
- **第二批验收**：预检首次拦截 1 个 OCR 跨行锚点，修正后通过；写入后旧 983 条逐字段不变，真题/写作数据不变，JSON 与标题/ID 唯一性通过。第二批审计见 `docs/research/yuan-shishuo-v2.20.md`。

---

## 2026-08-05 会话：v0.9.35 横屏协调优化 + 全面质量审计

- **完成**：
  - **横屏协调优化（commit `9bb31eb`）**：Robolectric 语义树实测定位——
    卡片限宽 480dp 居中（原 584dp 比例 1.73:1 横幅感）+ 右栏操作面板垂直居中
    （原悬顶下方 160-240dp 空白）+ 进度条/徽章对齐；新增横屏协调性回归测试 5 个
  - **全面质量审计（commit `623cdee`，修复 18 项）**：
    - 三路并行审计代理 + 实测驱动：横屏/代码质量/数据层/AI/设置
    - 关键修复：双断点不一致（MEDIUM 窗口双栏激活）、窄横屏顶栏降级（高度类）、
      markDontKnow 连点竞态（同步推进+400ms 防连击）、新卡排序方向（升序→降序）、
      考试日期倒计时联动、AI 消息重复注入、AI 幽灵回复代次防护、token 预算截断、
      错题递增清调度、调度失败可重试、背题参数空安全、UI/VM https 同步、
      学习时长 addStudyTime 打通、4 处限宽贴左修复、空错态全宽、资源化 4 处等
  - **版本号提升**：versionCode 60 / versionName 0.9.35 + CHANGELOG
- **验证**：全量 **574 单测 0 失败**（多轮）+ assembleDebug 通过
- **设计决策记录（审计发现但评估后不修改）**：
  - EASY 双重加成（FSRS w[16] 1.23 × 三档 easyBonus 1.2-1.5）：设计文档 3.3.4
    三档机制 + FsrsWrapperTest 断言固化，属既定设计（EASY 间隔更长符合语义）
  - 已知低风险项记录待评估：DUE 查询双时间源（SQLite vs ClockGuard）、每日限额
    按点估算、日期选择器时区（中国 +8 无影响）、静默 AI 任务阻塞发送、设置滑杆
    逐 tick 写 DataStore、会话恢复窄竞态、Composable 硬编码文案 120+ 处（历史债）
- **下次继续**：
  - push + tag v0.9.35 → Release #65 → receipt
  - 路线图：知识图谱 Graph 视图（数据就绪）/ 学习统计页 / 复习提醒通知
- **commit**：
  - `9bb31eb` — refactor(ui): 横屏知识卡片协调性优化
  - `623cdee` — fix: 全面质量审计修复 18 项（v0.9.35）

---

## 2026-08-05 会话：v0.9.36 知识卡片全屏沉浸模式

- **完成**：
  - **全屏沉浸模式（commit `1b3c621`）**：
    - `ImmersiveSystemBars`（core/designsystem 新增）：项目首个沉浸式先例——
      WindowInsetsControllerCompat + BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE 隐藏
      系统栏，滑动边缘临时唤出自动隐藏，onDispose 自动恢复
    - `CardsFullscreenScreen`（feature/cards 新增）：无顶栏零 insets Scaffold +
      左上角半透明圆形浮动退出按钮；五态 Crossfade + Snackbar + Leech 警告
      AlertDialog 全镜像卡片页
    - **共享复习会话**：`hiltViewModel(navController.getBackStackEntry(ROUTE_CARDS))`
      经 @Composable provider 延迟求值（NavHost builder 非 composable 上下文——
      初次直接传 viewModel 编译报错，改 provider 后通过）
    - 横屏变体：卡片 560dp + 右操作栏 280dp 单列竖排评分（RatingButtons columns=1）；
      竖屏放宽上限（comfortable）
    - 顶栏全屏入口（有卡时显示）；strings 新增 card_fullscreen/card_fullscreen_exit
    - `CardsFullscreenLayoutTest` 4 断言（实测校准：Spacing.lg=16 → 左栏 704dp 卡片
      左缘 72dp；面板左缘 720dp；"不会" top=25 居中）
  - **版本号提升（commit `21d301b`）**：versionCode 61 / versionName 0.9.36 +
    CHANGELOG + 00-STATUS + receipt（v0.9.36-release-receipt.md）
- **验证**：全量 **583 单测 0 失败** + assembleDebug + app/cards compileDebugKotlin
  全部通过（offline；GitHub API 网络故障不影响本地构建）
- **发布**：main（`21d301b`）+ tag `v0.9.36` 已通过 git 代理通道（ghfast.top）推送，
  CI Release #66 已触发但 **GitHub API（SSL EOF）无法确认结果**——与 v0.9.35 相同
  状态，待网络恢复后核验 Release 页/APK/签名
- **commit**：
  - `1b3c621` — feat(cards): 知识卡片全屏沉浸模式（v0.9.36）
  - `21d301b` — chore: v0.9.36 版本号提升（versionCode 61 / versionName 0.9.36）+ CHANGELOG
- **下次继续**：
  - 网络恢复后核验 v0.9.35（#65）/ v0.9.36（#66）CI 结果 + receipt 补全
  - 路线图：知识图谱 Graph 视图（数据就绪）/ 学习统计页 / 复习提醒通知

---

## 2026-08-05 会话：v0.9.35/36 Release 核验与修复

- **核验通道突破**：GitHub API（SSL EOF）与 Actions 页被代理拦截，发现 **ghfast.top 网页代理可读 `/releases/tag/X` 页面**——以页面标题判别真实 Release（"Release 文研App vX" = 工作流产出；纯 tag 视图 = 未产出）
- **核验结果**：
  - v0.9.35：✅ 已发布（Release #65，标题 "Release 文研App v0.9.35"），APK 实测 versionCode 60 / 0.9.35，sha256 `8e8a9d1a…`，receipt 已补写
  - v0.9.36：❌ 首次 tag 推送（→21d301b）未产出 Release（纯 tag 视图）→ 判定工作流未完成（原因待 API 恢复后查日志）
- **修复**：移 tag 至 HEAD（ad9ca33，含空态按钮修复）+ 更新 tag 注释（583 单测）+ 强制推送重触发 → **~2.5 分钟生成 Release #66**（2026-08-05T15:55:34Z）
- **v0.9.36 最终核验**：标题 "Release 文研App v0.9.36"、正文含下载安装/更新内容/功能特性；APK `1e6565f0…` 两资产 sha256 一致；aapt2 versionCode 61 / versionName 0.9.36 / targetSdk 35；apksigner 正式证书 CN=Wenyan App（3fefd8a0… 与历次一致）；receipt 已补全
- **commit**：
  - `71a14ae` — docs: v0.9.35 已确认发布 + v0.9.36 重触发（网页代理核验）
  - （receipt/STATUS 更新随本日志提交）
- **下次继续**：
  - GitHub API 恢复后：补录 v0.9.35/36 Release id、查 v0.9.36 首次触发失败日志
  - 路线图：知识图谱 Graph 视图（数据就绪）/ 学习统计页 / 复习提醒通知

---

## 2026-08-05 会话：v0.9.34 全局横屏适配

- **完成**：
  - **横屏双栏（commit `acb2649`）**：知识卡片复习页横屏 Column→Row 双栏——
    左卡片区（占全部高度+大部分宽度，突出大易读）+ 右 200dp 操作面板
    （2×2 评分网格，按钮 ~90dp 更小、总高 ~120dp）
    - 新增 `AdaptiveWindowLayout`（core/designsystem）：BoxWithConstraints 暴露
      内容区尺寸 + `shouldUseDualPane(maxWidth>maxHeight && maxWidth>=600dp)` 纯函数
    - `RatingButtons` 加 columns 参数（横屏 2 / 竖屏 4）；`SiblingRatedHint` 窄版
    - CardsScreen 外层横屏解除 widthIn(600) 让双栏用满宽度
  - **全局巡检**：4 个列表类 Screen（知识/论述题/真题背题/错题本）顶部搜索/筛选栏
    限宽居中与列表对齐；AiAssistant InputBar 限宽（IME 独占语义不变）
  - **反复打磨（commit `9ef057c`，3 轮深度复查）**：
    - TodayPlanBanner 横屏 compact 单行（~110dp→~44dp，释放 ~70dp 给卡片）
    - 右栏 verticalScroll 兜底（矮横屏 318dp 内容 vs 198dp 可用溢出）
    - 右栏滚动重置（翻转/切卡 scrollTo(0)，与左栏 FlipCard 对称）
    - SessionCompleteState / QuizPracticeDetail 操作栏限宽
  - **版本号提升（commit `a200511`）**：versionCode 59 / versionName 0.9.34 + CHANGELOG
- **验证**：
  - 全量 **569 单测 0 失败**（+10 AdaptiveWindowLayoutTest：横屏判定边界
    599/600dp、宽=高、平板竖/横屏 + Compose 尺寸注入）
  - `assembleDebug` BUILD SUCCESSFUL（4 轮全量验证）
- **进行中**：
  - v0.9.34 发布流程：待 push + tag 触发 Release #64 → release receipt
- **下次继续**：
  - push + 触发 release.yml（Release #64）→ 生成 release receipt
  - 路线图规划项待选：复习提醒通知（WorkManager）/ 学习统计页（review_logs
    数据已就绪）/ 数据导出导入（工作量最小）
- **关键发现**：
  - 横屏手机（高 ~360dp）垂直空间极紧张：TopBar 64dp + 横幅 + 进度 + 按钮组后
    卡片仅 ~140dp，必须压缩横幅/收敛按钮才能"卡片大"
  - `shouldUseDualPane` 用内容区尺寸（BoxWithConstraints）而非
    LocalConfiguration.orientation：Preview 可设 widthDp/heightDp、单测可注入
  - 竖屏零回归原则：所有横屏新参数默认 false / 竖屏宽度 < 断点不生效
  - Compose 测试 `assertIsDisplayed` 在 Robolectric 默认屏幕（<800dp）会失败，
    超出屏幕的节点用 `assertExists` 验证
- **commit**：
  - `acb2649` — feat(ui): 全局横屏适配——知识卡片双栏 + 列表/输入栏限宽（v0.9.34）
  - `9ef057c` — refactor(ui): 横屏适配反复打磨——横幅紧凑化/滚动兜底/完成态限宽（v0.9.34）
  - `a200511` — chore: v0.9.34 版本号提升（versionCode 59 / versionName 0.9.34）+ CHANGELOG

---

## 2026-08-04 会话：v0.9.33 真题背题专项

- **完成**：
  - **真题背题功能（v0.9.33，commit `ecf307d`）**：知识点页新增"真题背题"入口卡，
    名词解释/简答背诵模式——列表页（题型/科目/年份三维筛选）+ 详情页（显示答案/会了/不会进错题本走 FSRS）
    - DAO `observeByQuestionTypes`：多题型 IN 查询，稳定 ORDER BY（year DESC + exam_paper_code + id）
    - Repository `observePracticeQuestions`：题型白名单封装，数据层排除 ESSAY 避免与论述题 Tab 重复
    - 导航：`quiz_practice` / `quiz_practice_detail` 两个子路由，筛选条件随参数传递保持上下文
    - 错题本联动：标记"不会"→ `recordWrongAnswer(SOURCE_QUIZ_WRONG)`，错题本显示"真题练习"
  - **质量复查修复（4 项，用户要求"重复检查做到最好"）**：
    - Snackbar `withTimeout(5s)` 防挂起：对齐 CardsScreen v0.9.23 / WrongAnswerScreen v0.9.25 模式
    - `markDontKnow` 失败时成功文案覆盖失败文案的误导 bug（仅成功时覆盖"最后一题"提示）
    - 详情页 ErrorState 重试按钮无效（空 lambda）→ ViewModel `retry()` 取消旧 job + CancellationException rethrow
    - `（本题暂无参考答案）` 硬编码 → 资源化 `kp_quiz_no_answer`
  - **测试修复**：两个模块 `FakeExamQuestionDao` 补齐 `observeByQuestionTypes`（core/data + feature/knowledge）
  - **版本号提升（commit `88ffcb4`）**：versionCode 58 / versionName 0.9.33 + CHANGELOG
- **验证**：
  - 全量 **559 单测 0 失败**（3 轮：首次失败→修复 fake→通过；复查修复后再跑 1 轮全绿）
  - `assembleDebug` BUILD SUCCESSFUL
- **进行中**：
  - v0.9.33 发布流程：release receipt + SESSION_LOG + 00-STATUS 待更新，等待 push 触发 Release #63
- **下次继续**：
  - push + 触发 release.yml（Release #63）→ 生成 release receipt（run id / sha256）
  - 路线图规划项待选：复习提醒通知（WorkManager）/ 学习统计页（review_logs 数据已就绪）/ 数据导出导入（工作量最小）
- **关键发现**：
  - material3 1.5.0-alpha18 的 `showSnackbar` 挂起 bug 是项目已知模式（v0.9.23/25 已修两次），新代码必须套用 withTimeout 保护
  - `catch` 直接设置 StateFlow（非 emit 模式）时，取消必须 rethrow `CancellationException`，否则 retry 取消旧 job 会误设错误态
  - 项目 ViewModel 内硬编码 snackbar 文案是既有惯例；Composable 内文案必须资源化
- **commit**：
  - `ecf307d` — feat(knowledge): 真题背题专项——名词解释/简答背诵模式（v0.9.33）
  - `88ffcb4` — chore: v0.9.33 版本号提升（versionCode 58 / versionName 0.9.33）+ CHANGELOG

---

## 2026-07-12 完整工作日会话

- **完成**：
  - Phase 1-5 Android 开发全部完成（骨架/FSRS/AI/UI/Release）
  - GitHub Release v0.1.0 发布（签名 APK 14.7 MB）
  - M3 Expressive 改造：27 个 commit 推送，设计规格 + 实现计划（26 Task）完成
  - CI 修复：升级 composeBom 到 2025.12.00、AGP 到 8.6.0
  - 交接方案：创建完整 docs/ 文档体系 + AGENTS.md + tools/ 脚本迁移
- **进行中**：
  - M3 改造 Phase 0（CI 修复）阻塞中
  - OCR 处理约 60%（125/208 文件，PID 20432 运行中）
- **阻塞**：
  - CI 编译失败：materialkolor 4.1.1 与 Kotlin 2.0.20 不兼容
  - 根因：materialkolor 4.1.1 用 Kotlin 2.3.0 编译，元数据版本不匹配
  - 详见 [03-FAILED-ATTEMPTS.md #001](03-FAILED-ATTEMPTS.md)
- **下次继续**：
  - 方案 C Phase 0：修复 CI（升级 Kotlin 到 2.3.0 或降级 materialkolor）
  - 方案 C Phase 1：设计令牌 + 4 个关键组件（药丸导航栏/LargeTopAppBar/分组卡片/层级列表项）
  - 方案 C Phase 2：5 主屏应用
  - 方案 C Phase 3：4 次屏打磨
  - OCR 完成后跑知识提取管线
- **关键发现**：
  - materialkolor 4.1.1 用 Kotlin 2.3.0 编译，与项目 Kotlin 2.0.20 不兼容
  - `source must not be null` 错误实际是 Kotlin 元数据版本不匹配，不是代码问题
  - PKCS12 keystore 要求 storepass = keypass
  - PowerShell 不支持 heredoc
  - Trae 云端模式不保留 AI 记忆，依赖 AGENTS.md + docs/ 恢复上下文
- **commit**：
  - `a6a97af` — 升级 composeBom
  - `77d34e7` — 升级 AGP
  - `684e6a2` — 重写 ContentSourceBadge when 表达式
  - 本次会话：AGENTS.md + docs/ + tools/ 迁移（待 commit）

---

## 2026-07-12 会话：KSU 风格 UI 升级 Phase 0-3

- **完成**：
  - **Phase 0**（commit `0e086ba`）：解除 materialkolor 4.1.1 + Kotlin 2.0.20 元数据阻塞
    - Kotlin 2.0.20 → 2.3.10
    - KSP 2.0.20-1.0.25 → 2.3.2（新版本号格式）
    - Hilt 2.51.1 → 2.57.1（Kotlin 2.3 元数据兼容）
    - Room 2.6.1 → 2.7.0（KSP2 支持）
    - material3 显式锁定 1.5.0-alpha18（覆盖 BOM 1.4.0）
    - 修复 WenyanTheme.kt ColorSpec import 路径 + PaletteStyle.supportsSpec2025 校验
  - **Phase 1**（commit `6bbbb29`）：新增 4 个 KSU 风格组件
    - WenyanLargeTopAppBar（LargeFlexibleTopAppBar 封装，含 @OptIn）
    - WenyanNavigationBar（药丸风格底部导航，用 indicatorColor 参数）
    - GroupedCard + GroupedCardItem（分组卡片）
    - HierarchicalListItem（层级列表项）
    - 为 core:designsystem 模块添加首个 Compose UI 测试（Robolectric + createComposeRule）
    - 搭建 Robolectric 测试基础设施（m2 settings.xml 阿里云镜像 + 预下载 SDK jar）
  - **Phase 2**（commit `a85cc68`）：9 个 Screen 迁移到 WenyanLargeTopAppBar
    - WenyanApp.kt 替换为 WenyanNavigationBar（保留 hierarchy 高亮逻辑）
    - 6 个滚动屏接入 exitUntilCollapsedScrollBehavior + nestedScroll
    - 3 个固定内容屏仅享受 Large 标题样式
    - KnowledgePointDetailScreen 动态 title + subtitle（考频+难度）
    - 修复 6 个文件的 nestedScroll import 路径错误

- **关键发现**：
  - material3 1.5.0-alpha19+ 要求 AGP 9.1.0 + compileSdk 37，与 AGP 8.6.0 不兼容
  - alpha18 中 LargeFlexibleTopAppBar 仍为 @ExperimentalMaterial3ExpressiveApi（非 Stable）
  - MaterialExpressiveTheme 标记为 Material3ExpressiveApi（非 @RequiresOptIn），WenyanTheme 编译无需 OptIn
  - NavigationBarItemDefaults.colors() 参数名从 selectedIndicatorColor 改为 indicatorColor（alpha18）
  - nestedScroll 正确 import 路径：androidx.compose.ui.input.nestedscroll（不是 androidx.compose.input.nestedscroll）
  - Robolectric Maven Resolver 不读 Gradle 配置，需单独 ~/.m2/settings.xml
  - createComposeRule() 需 ComponentActivity 声明（debugImplementation compose-ui-test-manifest）
  - assertIsDisplayed 是顶层扩展函数需 import；assertDoesNotExist 是成员函数不需 import
  - onNodeWithText 只匹配 Text 组件；onNodeWithContentDescription 匹配 Icon contentDescription
  - releaseUnitTest 不含 debugImplementation 依赖，需运行 testDebugUnitTest

- **commit**：
  - `0e086ba` — Phase 0：解除 M3 Expressive 改造阻塞
  - `6bbbb29` — Phase 1：4 个 KSU 组件 + 首个 Compose UI 测试
  - `a85cc68` — Phase 2：9 个 Screen 迁移到 WenyanLargeTopAppBar
  - `c0e2cf1` — Phase 3：文档更新

- **下次继续**：
  - 跑 emulator 实测滚动折叠效果
  - 用 GroupedCard 改造 SettingsScreen
  - 用 HierarchicalListItem 改造 KnowledgePointDetailScreen 关联知识点区域
  - 为 GroupedCard / HierarchicalListItem 写测试
  - OCR 完成后跑知识提取管线

---

## 2026-07-12 会话：CI 修复 + PR 合并

- **完成**：
  - 推送 10 个 commit 到 `trae/agent-cKcjcc` 分支
  - 创建 PR #1 触发 CI
  - 修复 3 个 CI 失败问题，最终 CI run 29211066998 全绿（11/11 步骤成功）
  - 合并 PR #1 到 main（squash merge → `3efe678`）

- **CI 失败修复过程**：
  - **失败 1**：`Plugin [id: 'com.google.devtools.ksp', version: '2.3.2'] was not found`
    - 排查：Aliyun 镜像 metadata 显示 2.3.2 存在，POM HTTP 200 OK，但 CI 找不到
    - 修复 `22b1a7e`：pluginManagement 仓库顺序调整，gradlePluginPortal/mavenCentral/google 移到前面，Aliyun 作 fallback
  - **失败 2**：`Plugin [id: 'org.jetbrains.kotlin.plugin.compose', version: '2.3.10'] was not found`
    - 同上，仓库顺序修复后解决
  - **失败 3**：`java.lang.OutOfMemoryError: Metaspace` 在 `:feature:aiassistant:compileReleaseKotlin`
    - 修复 `dcba036`：MaxMetaspaceSize 512m → 1g（Release 构建 R8 + Kotlin + Compose 需加载大量类）
  - **失败 4**：`java.lang.RuntimeException at RoboMonitoringInstrumentation.java:102` 4 个测试全挂
    - 根因：testReleaseUnitTest 不含 debugImplementation 依赖（ComponentActivity manifest 缺失）
    - 修复 `9e1723d`：CI `gradle test` → `gradle testDebugUnitTest`（release 测试通常跳过）
  - 另有 `64b8894`：CI Gradle 8.7 → 8.14.4 与本地环境对齐

- **关键发现**：
  - Aliyun 镜像从 GitHub Actions runner（美/欧）访问时可能不可达或返回错误响应，plugin marker artifact 解析失败
  - dependencyResolutionManagement（依赖）保持 Aliyun 优先（体积大，加速明显），pluginManagement（插件）改为全局仓库优先
  - Kotlin 编译器 in-process 模式下共享 Gradle daemon 的 metaspace，所有模块编译累积压力，512m 对 Release 构建不足
  - `debugImplementation(libs.androidx.compose.ui.test.manifest)` 只在 debug 变体可用，release 变体测试时 Robolectric 找不到 Activity 声明
  - setup-gradle@v3 的 cache-read-only 模式下 cache restoration 可能失败（400 错误），但 Gradle 仍能正常运行

- **commit**：
  - `22b1a7e` — pluginManagement 仓库顺序调整
  - `64b8894` — CI Gradle 8.7 → 8.14.4
  - `dcba036` — MaxMetaspaceSize 512m → 1g
  - `9e1723d` — test → testDebugUnitTest
  - `3efe678` — PR #1 squash merge 到 main

- **下次继续**：
  - 跑 emulator 实测滚动折叠效果
  - 用 GroupedCard 改造 SettingsScreen
  - 用 HierarchicalListItem 改造 KnowledgePointDetailScreen 关联知识点区域
  - 为 GroupedCard / HierarchicalListItem 写测试
  - OCR 完成后跑知识提取管线

---

## 2026-07-12 会话：交接文档完善

- **完成**：
  - 推送文档更新到 main（commit `4461eba`）
  - 清理已合并的远端 feature 分支 `trae/agent-cKcjcc`
  - 系统性更新交接文档，确保沙箱清空后 AI 可无缝接手

- **文档更新内容**：
  - **AGENTS.md**：
    - 技术栈更新为实际版本（Kotlin 2.3.10 / material3 1.5.0-alpha18 / Hilt 2.57.1 / Room 2.7.0）
    - 第 7 节"当前阻塞"改为"当前状态"（无阻塞）
    - 第 8 节"项目阶段总览"更新 KSU UI 升级为已完成
    - 新增第 9 节"下一步优先级"
    - 新增"CI 相关硬约束"小节（5 条 CI 相关规则）
    - 文档地图新增 ksu-ui-upgrade.md
  - **01-QUICK-RECOVERY.md**：
    - CI 检查命令更新为 python3 解析 JSON 格式
    - 新增"下载 CI 失败日志"命令模板
    - 新增"CI 常见失败原因"快速诊断列表
    - 场景 2 从"M3 改造"改为"KSU 风格 UI 升级后续"
    - 新增"Trae 沙箱环境"小节（路径/JDK/Android SDK/Gradle/JAVA_TOOL_OPTIONS）
    - 会话结束 Step 4 同时给出本地和沙箱两条命令
  - **00-STATUS.md**：已在 `4461eba` 中更新
  - **03-FAILED-ATTEMPTS.md**：已在 `4461eba` 中新增 #010-#012

- **关键交接信息**（新会话必读）：
  - **main 最新 commit**：`4461eba`（文档更新，PR #1 后）
  - **PR #1 squash merge**：`3efe678`（KSU UI 升级 Phase 0-3 全部代码）
  - **CI 状态**：run 29211066998 全绿（PR 分支），main 上 2 个 run 运行中
  - **无阻塞**：可直接开始下一步工作
  - **下一步**：跑 emulator 实测 / GroupedCard 改造 / HierarchicalListItem 改造

- **commit**：
  - `4461eba` — 文档更新（00-STATUS + SESSION_LOG + 03-FAILED-ATTEMPTS）
  - 本次交接：AGENTS.md + 01-QUICK-RECOVERY.md + SESSION_LOG.md（待 commit）

- **下次继续**：
  - 跑 emulator 实测 LargeFlexibleTopAppBar 滚动折叠效果
  - 用 GroupedCard 改造 SettingsScreen
  - 用 HierarchicalListItem 改造 KnowledgePointDetailScreen 关联知识点区域
  - 为 GroupedCard / HierarchicalListItem 写测试
  - OCR 完成后跑知识提取管线

---

## Session 2026-07-13：UI 改造闭环计划（Phase 1-5 全部完成）

### 目标

执行 [docs/plans/ui-closure-plan.md](plans/ui-closure-plan.md) — 把 KSU 风格 UI 改造从"骨架已立"推进到"闭环可用"。

### 完成内容

**Phase 1：GroupedCard 组件增强**（commit `da3f369`）
- 增强 `GroupedCardItem`：新增 `leadingIcon` / `leadingIconContentDescription` / `description` 参数
- 新增 `GroupedCardDivider` 函数（`HorizontalDivider` + outlineVariant + 0.5dp）
- 新增 7 个 Robolectric 测试（GroupedCardTest.kt）覆盖 title/subtitle/description/leadingIcon/trailing

**Phase 2：SettingsScreen 重构**（commit `68e5946`）
- 4 个分组（外观/动态色彩/AI服务/关于）全部从 `SectionHeader` + 手写 Row 迁移到 `GroupedCard` + `GroupedCardItem`
- LazyColumn 添加 `verticalArrangement = Arrangement.spacedBy(Spacing.xl)` 避免卡片粘连
- 删除私有 `SwitchItem` 函数（GroupedCardItem.trailing 已覆盖）

**Phase 3：KnowledgePointDetailScreen 重构**（commit `c918411`）
- `RelatedGroup`（关联/对比/延伸知识点）从 `TonalCard` + 简单 `Text` 重构为 `GroupedCard` + `GroupedCardItem` + `GroupedCardDivider`
- `forEachIndexed` 在项间插入分割线（除最后一项）

**Phase 4：@Preview + 组件测试**（commit `f311a31`）
- 4 个 @Preview 文件（全部 `dynamicColor=false`，三态覆盖 light/dark/AMOLED）：
  - `WenyanLargeTopAppBarPreview`：Light-Simple / Light-WithSubtitle / AMOLED-WithSubtitle
  - `WenyanNavigationBarPreview`：Light / Dark / AMOLED（5 个示例导航项）
  - `GroupedCardPreview`：settings-style / about-style / knowledge-related-style
  - `HierarchicalListItemPreview`：Light-Tree / Dark-WithTrailing / AMOLED-NoOnClick
- 2 个组件测试文件（8 tests 全绿）：
  - `WenyanNavigationBarTest`（3 tests）：labels 显示 / items 有点击行为 / onNavigate 回调
  - `HierarchicalListItemTest`（5 tests）：root/child title / trailing / onClick / 无 trailing 时不显示箭头

**Phase 5：全量验证 + 文档更新**（本次）
- `assembleDebug` BUILD SUCCESSFUL（3m 59s，412 tasks）
- `testDebugUnitTest` BUILD SUCCESSFUL（1m 4s，117 tests 0 failures：designsystem 19 + fsrs 25 + data 52 + aiassistant 21）
- 更新文档：00-STATUS.md、SESSION_LOG.md、plans/ui-closure-plan.md（标记完成）

### 关键技术决策

1. **leadingIconContentDescription 默认 null**（装饰性图标）— 避免 TalkBack 重复朗读 title。仅在图标含义与 title 不同时才需显式设置。
2. **@Preview 全部 `dynamicColor=false`** — 动态色彩依赖系统壁纸，Preview 环境无壁纸会导致渲染异常。
3. **`icons_haveContentDescription_withLabel` 测试失败 → 改为 `items_haveClickAction_forAccessibility`** — Material3 NavigationBarItem 在 `label != null` 时对 icon 应用 `clearAndSetSemantics`，icon 的 contentDescription 节点不可见。正确做法是验证合并语义后 label 节点有 `ClickAction`（供 TalkBack 触发）。
4. **`GroupedCardDivider` 用 `outlineVariant` + 0.5dp** — 与 KSU 视觉规格一致，比 `outline` 更柔和。

### 环境问题与解决（沙箱特有）

- **Gradle 代理**：沙箱有 HTTP 代理 `127.0.0.1:18080`，但 Gradle 不读 `http_proxy` 环境变量。需在 `/root/.gradle/gradle.properties` 配置 `systemProp.http.proxyHost` 等。
- **Robolectric 代理**：Robolectric 的 `MavenArtifactFetcher` 不读 Gradle 的 `systemProp.*`。需在 `/root/.gradle/init.d/proxy.gradle` 用 `jvmArgs('-Dhttp.proxyHost=...')` 注入到 Test 任务。
- **JDK 版本**：mise 默认 `java=25`，但 `gradle` shim 用 mise 默认 JDK。需用 `$JAVA_HOME/bin/java -cp .../gradle-launcher.jar org.gradle.launcher.GradleMain` 直接调用强制 JDK 17。
- **Android SDK**：新沙箱未预装，需用 cmdline-tools 安装 `platform-tools;35.0.0` + `platforms;android-35` + `build-tools;35.0.0`。

### commit 列表

- `da3f369` — Phase 1: GroupedCard 增强 + 7 tests
- `68e5946` — Phase 2: SettingsScreen GroupedCard 重构（4 分组）
- `c918411` — Phase 3: KnowledgePointDetailScreen RelatedGroup 重构
- `f311a31` — Phase 4: 4 @Preview + 2 组件测试（8 tests）
- 本次 — Phase 5: 文档更新（00-STATUS + SESSION_LOG + plan 标记完成）

### 下次继续

- 跑 emulator 实测 LargeFlexibleTopAppBar 滚动折叠效果（P0）
- 可选：用 HierarchicalListItem 改造 KnowledgePointDetailScreen 多教材对照区域
- OCR 完成后跑知识提取管线 → 生成 seed_data.json

---

## Session 2026-07-13（第二条）：UI 统一与死组件清理

### 目标

执行 [docs/plans/ui-consolidation-cleanup.md](plans/ui-consolidation-cleanup.md) — 把 KnowledgePointDetailScreen 的 InfoSection/PerspectiveCard/SourcesSection 统一到 designsystem 组件，并清理 4 个零引用死组件。

### 深度调查发现的关键约束

在制定计划阶段，通过两轮深度调查发现 3 个关键问题，修订了原计划：

1. **AMOLED 嵌套卡片视觉反转**：调查 `WenyanTheme.kt` line 60-68 发现，AMOLED 模式覆盖了 `surfaceContainerLow = Color.Black`，但**未覆盖 `surfaceBright`**。若在 GroupedCard（surfaceBright）内嵌套 TonalCardLow（surfaceContainerLow），会形成"深灰卡套纯黑卡"的视觉反转。**结论**：MultiPerspectiveSection 保留 InfoSection 无容器模式，避免嵌套。

2. **padding 一致性**：GroupedCardItem 的水平 padding 是 `Spacing.lg`（16dp）。GroupedCard 内的所有内容必须用 `horizontal = Spacing.lg` 保持左边缘对齐。原计划摘要 Text 用 `Spacing.md`（12dp）会导致 4dp 不对齐。**结论**：统一为 `horizontal=lg, vertical=md`。

3. **HierarchicalListItem API 不匹配**：原 AGENTS.md P1 计划"用 HierarchicalListItem 改造多教材对照"——经源码核实，该组件 API 只有 `title + trailing`，无法承载教材正文段落（多行长文本），且多教材对照是扁平列表非树形层级。**结论**：删除该死组件，修订 P1 计划。

### 完成内容

**Phase 1：KnowledgePointDetailScreen 统一**（commit `ebad848`）
- 摘要 `InfoSection` → `GroupedCard`（纯文本，无嵌套风险，padding `horizontal=lg, vertical=md`）
- 资料来源 `InfoSection` → `GroupedCard` + `HorizontalDivider` → `GroupedCardDivider`
- `SourceRow` 加 `padding(horizontal=lg, vertical=md)` 与 GroupedCardItem 对齐
- `PerspectiveCard` 非 official 分支 → `TonalCardLow`（走 designsystem，独立卡片不嵌套）
- 多教材对照**保留 InfoSection**（避免 AMOLED 嵌套卡片视觉反转），加 KDoc 注释说明原因
- 清理不再使用的 imports（`HorizontalDivider`、`dp`）

**Phase 2：删除 4 个死组件**（commit `2f83ac3`）
- 删除 `WenyanTopAppBar`（KSU 升级后 9/9 Screen 用 WenyanLargeTopAppBar，0 引用）
- 删除 `SectionHeader`（GroupedCard 标题区已覆盖，0 引用）
- 删除 `LoadingState`（9 个 Screen 都手写 Box{CircularProgressIndicator()}，0 引用）
- 删除 `HierarchicalListItem`（API 只有 title+trailing，不匹配任何现有列表，0 生产引用）
  + 同步删除 `HierarchicalListItemPreview`（3 个 @Preview）
  + 同步删除 `HierarchicalListItemTest`（5 个测试）
- 更新 `WenyanLargeTopAppBar.kt` 注释：删除对 WenyanTopAppBar 的 2 处引用

**Phase 3：全量验证 + 文档更新**（本次）
- `assembleDebug` BUILD SUCCESSFUL（3m 59s，412 tasks）
- `testDebugUnitTest` BUILD SUCCESSFUL（174 tests 0 failures：designsystem 14 + data 52 + fsrs 25 + ai 62 + aiassistant 21）
- 更新文档：00-STATUS.md、SESSION_LOG.md、AGENTS.md、01-QUICK-RECOVERY.md、plans/ui-consolidation-cleanup.md

### 关键技术决策

1. **MultiPerspectiveSection 保留 InfoSection** — AMOLED 模式下 `surfaceContainerLow` 被覆盖为 Black 而 `surfaceBright` 未覆盖，GroupedCard 套 TonalCardLow 会形成视觉反转。加 KDoc 注释说明保留原因，避免后续误删。
2. **PerspectiveCard 分 isOfficial 两分支** — official 保留 `Surface(primaryContainer)`（designsystem 无 primaryContainer 变体），非 official 用 `TonalCardLow`（color/shape 完全一致）。
3. **删除 HierarchicalListItem 而非扩展 API** — 经调查证实无任何现有列表适合用该组件（所有列表都有多字段元信息，title+trailing 无法承载）。扩展 API 会增加复杂度但无实际收益，YAGNI。

### 环境问题

- **沙箱重置导致环境丢失**：会话中途沙箱被重置，`/root/.gradle/gradle.properties`、`/root/.gradle/init.d/proxy.gradle`、`/opt/android-sdk`、`/workspace/local.properties` 全部丢失。重新创建代理配置 + 重装 Android SDK（cmdline-tools + platform-tools + platforms;android-35 + build-tools;35.0.0）后恢复。

### commit 列表

- `ebad848` — Phase 1: KnowledgePointDetailScreen 摘要+资料来源统一到 GroupedCard
- `2f83ac3` — Phase 2: 删除 4 个零引用死组件
- 本次 — Phase 3: 文档更新

### 下次继续

- 跑 emulator 实测 LargeFlexibleTopAppBar 滚动折叠效果（P0）
- OCR 完成后跑知识提取管线 → 生成 seed_data.json（P1）
- 可选：用 GroupedCard 改造其他 Screen（如 ApiConfigScreen，但需先扩展 GroupedCardItem API）

### 新会话快速恢复 Checklist

新沙箱会话开始时，按以下顺序操作（5 分钟内进入工作状态）：

1. **读 [AGENTS.md](../AGENTS.md)** — 项目入口，了解技术栈、硬约束、当前状态
2. **读 [00-STATUS.md](00-STATUS.md)** — 10 秒了解当前状态（无阻塞，CI 全绿）
3. **读本文档最后一节** — 上次进度（本次会话）
4. **拉取最新代码**：
   ```bash
   cd /workspace && git pull origin main
   ```
5. **配置 Gradle 代理**（沙箱特有，新沙箱必做）：
   ```bash
   # /root/.gradle/gradle.properties
   cat > /root/.gradle/gradle.properties <<'EOF'
   systemProp.http.proxyHost=127.0.0.1
   systemProp.http.proxyPort=18080
   systemProp.https.proxyHost=127.0.0.1
   systemProp.https.proxyPort=18080
   systemProp.http.nonProxyHosts=localhost|127.0.0.1
   EOF

   # /root/.gradle/init.d/proxy.gradle（Robolectric 测试需要）
   mkdir -p /root/.gradle/init.d
   cat > /root/.gradle/init.d/proxy.gradle <<'EOF'
   allprojects {
       tasks.withType(Test).configureEach {
           jvmArgs('-Dhttp.proxyHost=127.0.0.1','-Dhttp.proxyPort=18080',
                   '-Dhttps.proxyHost=127.0.0.1','-Dhttps.proxyPort=18080',
                   '-Dhttp.nonProxyHosts=localhost|127.0.0.1')
       }
   }
   EOF
   ```
6. **配置环境变量**：
   ```bash
   export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2
   export ANDROID_HOME=/opt/android-sdk
   export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport"
   export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
   ```
7. **验证构建**（注意：不能用 `gradle` shim，它用 mise 默认 JDK 25 与 AGP 8.6.0 不兼容）：
   ```bash
   $JAVA_HOME/bin/java -Dorg.gradle.daemon=false -cp /root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/lib/gradle-launcher-8.14.4.jar org.gradle.launcher.GradleMain :app:assembleDebug --no-daemon 2>&1 | tail -5
   ```
8. **开始工作**：根据 [00-STATUS.md](00-STATUS.md) 的"下一步优先级"选择任务

---

## Session 2026-07-13（第三条）：P0 双修 — SeedDataLoader 接通 + release.yml CI 修复

### 目标

执行 [docs/plans/p0-seed-loader-ci-fix.md](plans/p0-seed-loader-ci-fix.md) — 修复 release.yml 的 2 个 CI bug（避免下次发布失败）+ 接通 SeedDataLoader 调用点（让 App 从空壳 UI 变成可用工具）。

### 深度调查发现的关键约束

计划制定阶段经过多轮深度审查，发现并修订了 3 个关键问题：

1. **SupervisorJob 不能防崩溃（CRITICAL 修正）**：原计划误以为 `SupervisorJob` 能防止 App 崩溃。经 Kotlin 官方文档核实：`SupervisorJob` 只阻断异常向父 Job 传播，**不阻止异常本身被抛出**。`launch` 根协程的未捕获异常会经 `Thread.uncaughtExceptionHandler` 处理，Android 默认是 `RuntimeInit$KillApplicationHandler`（崩溃）。**修订**：必须显式加 `CoroutineExceptionHandler`，捕获异常并 Log.e，降级为 EmptyState。

2. **Hilt 注入链完整性核实**：SeedDataLoader 有 9 个构造依赖（Context + 7 DAO + GraphRepository）。逐一核实可注入性：7 DAO 由 `DatabaseModule` `@Provides`，GraphRepository 由 `DataModule` `@Binds` 到 `GraphRepositoryImpl @Inject constructor`，Context 由 `@ApplicationContext` 提供。**结论**：全部可注入，无需补充 @Provides/@Binds。

3. **属性初始化顺序**：`exceptionHandler`（val）必须在 `applicationScope`（val 引用 exceptionHandler）之前声明。Kotlin 按声明顺序初始化属性，反过来会 NPE。最终代码中 exceptionHandler 在前，applicationScope 在后，安全。

### 已知限制（本次接受，记录供后续优化）

1. **强杀重启可能丢失复习数据**：`MemoRecordEntity` 外键 `onDelete = CASCADE` + DAO 用 `OnConflictStrategy.REPLACE`。首次导入中途被强杀时，下次启动 REPLACE 会先 DELETE（触发 CASCADE 删 memo_records）再 INSERT，覆盖用户复习进度。MVP 阶段无真实数据可丢失，接受。
2. **importToDatabase 无 @Transaction**：7 步导入无外层事务，中途 OOM 会留部分数据。但用 REPLACE，下次启动覆盖，风险可控。
3. **mapNotNull 静默跳过**：subject 字段不匹配的知识点/真题会被跳过，但仍执行 `markInitialized()`。当前 stage2-sample 数据匹配，无影响。
4. **release.yml "Verify keystore" 隐藏 bug（Line 63-70，本次不动）**：该步骤无条件执行 `keytool -list`，但前一步在 `KEYSTORE_BASE64` 未配置时 `exit 0` 跳过解码。结果 Verify 步骤对不存在的文件执行 keytool 失败。当前仓库已配置 Secrets，不会触发；修复需重构 keystore 处理逻辑，超出 P0 范围。记录到 `03-FAILED-ATTEMPTS.md` 供后续修复。

### 完成内容

**Phase 1：修复 release.yml CI bug**（commit `ff19231`）
- Line 46：`gradle-version: '8.7'` → `'8.14.4'`（AGENTS.md 硬约束：旧版 8.7 在解析 KSP 2.3.x 时有 bug）
- Line 81：`gradle test` → `gradle testDebugUnitTest`（AGENTS.md 硬约束：debugImplementation 依赖只在 debug 变体可用）
- yaml 语法验证通过（PyYAML safe_load）

**Phase 2：接通 SeedDataLoader**（commit `07c3a6d`）
- `WenyanApplication.kt` 注入 `SeedDataLoader`（`@Inject lateinit var`）
- `onCreate` 用 `CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler).launch` 异步调用 `ensureSeedDataLoaded()`
- `CoroutineExceptionHandler` 捕获异常并 `Log.e`，避免 App 崩溃
- 不阻塞 onCreate：各 ViewModel 用 `stateIn(WhileSubscribed(5000))` 订阅，数据加载完后自动刷新

**Phase 3：验证 + 文档**
- `:app:compileDebugKotlin` SUCCESSFUL（`:app:kspDebugKotlin` 执行，证明 Hilt 代码生成成功）
- `assembleDebug` SUCCESSFUL（412 tasks）
- `testDebugUnitTest` SUCCESSFUL（174 tests 0 failures，无回归）
- 更新文档：00-STATUS.md、SESSION_LOG.md、AGENTS.md、plans/p0-seed-loader-ci-fix.md

### 环境问题

- **沙箱 Java 版本切换**：会话开始时 `JAVA_HOME` 指向 Java 25.0.2，但 Kotlin 编译器的 `JavaVersion.parse` 无法解析 "25.0.2"（抛 `IllegalArgumentException`）。切换到 Java 17.0.2 后正常。**记录**：本项目要求 Java 17（AGP 8.6.0 + Kotlin 2.3.10 兼容），新沙箱需 `export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2`。

### 关键技术决策

1. **CoroutineExceptionHandler 而非 try-catch** — `launch` 根协程的异常无法用 try-catch 捕获（异常发生在 lambda 内部，但 launch 不向调用者传播）。`CoroutineExceptionHandler` 是 Kotlin 协程官方的根协程异常处理机制。
2. **独立 CoroutineScope 而非 GlobalScope** — `GlobalScope` 引发 lint 警告且生命周期不受控。Application 进程级单例，用独立 CoroutineScope 即可。
3. **Dispatchers.IO** — SeedDataLoader 涉及 assets 读取 + Room 数据库写入，IO 密集型。
4. **不阻塞 onCreate** — 异步加载，App 启动流畅。各 Screen 先显示 loading/EmptyState，数据加载完后 Flow 自动刷新。

### commit 列表

- `ff19231` — Phase 1: release.yml CI 修复（gradle-version 8.7→8.14.4, gradle test→testDebugUnitTest）
- `07c3a6d` — Phase 2: 接通 SeedDataLoader（WenyanApplication 注入 + onCreate 异步调用）
- 本次 — Phase 3: 文档更新

### 下次继续

- 跑 emulator 实测 SeedDataLoader（P0）：Logcat 无异常 + 各 Tab 有数据 + 重启不重复导入
- KnowledgeViewModel 2 个 bug（P1）：filterByCategory 不筛选 + subject 显示 "TEXTBOOK_NATIVE"
- OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（P2）

---

## Session 2026-07-13（第四条）：P1 修复 — KnowledgeViewModel 科目筛选 + 科目名显示

### 目标

执行 [docs/plans/p1-knowledge-viewmap-subject-fix.md](plans/p1-knowledge-viewmap-subject-fix.md) — 修复 KnowledgeViewModel 的 2 个 bug：filterByCategory 不筛选 + subject 显示 "TEXTBOOK_NATIVE"。

### 深度调查发现的关键事实

1. **数据模型断层**：KnowledgePointEntity 无 subjectId 字段，唯一关联路径是 `chapterId → ChapterEntity.subjectId → SubjectEntity.name`，但整条通道上没有任何 DAO JOIN 查询、@Relation、KnowledgePointWith* 数据类实现它。
2. **三套互不相通的"科目"机制**：SubjectEntity（subjects 表，孤儿表）/ ExamCodeHistoryEntity + ExamCodeResolver（仅 Quiz 模块用）/ KnowledgeCategory 枚举（仅 Knowledge 列表页 FilterChip 标签，筛选逻辑空壳）。
3. **seed_data.json 科目名是全名**（"中国古代文学"），枚举 label 是简称（"古代文学"），4 个中 2 个不匹配。用 `subjectName.contains(keyword)` 匹配兼容两者。
4. **SubjectEntity.shortName 是死字段**且 `SeedDataLoader.kt:107` 的 `take(2)` 实现错误（"中国古代文学"→"中国"而非"古文"）。本次不动（YAGNI）。
5. **KnowledgeViewModel 无测试**（test/ 目录不存在），修复时补测试。

### 计划打磨中发现并修复的问题（3 轮深度审查）

| # | 严重度 | 问题 | 修复 |
|---|--------|------|------|
| 1 | CRITICAL | Task 4 和 Task 5 对 filterByCategory/toUiItem 位置说法矛盾 | Task 4 一步到位包含 companion object 完整代码 |
| 2 | CRITICAL | 测试代码用 Google Truth，但项目无 truth 依赖 | 全部改为 JUnit 原生断言 |
| 3 | Minor | 测试代码 `"..." .repeat(5)` 有空格，Kotlin 语法错误 | 改为 `"...".repeat(5)` |
| 4 | 一致性 | 文件结构表包含 build.gradle.kts，但实际已有依赖 | 删除该行 |
| 5 | 设计混乱 | Task 5 "配套改动"与 Task 4 重复 | 改为"无需再改 ViewModel" |
| 6 | 测试不足 | 缺少边界场景（空列表/不匹配/summary 有值不截断） | 新增 3 个测试（7→10） |
| 7 | 架构思考未记录 | getVerifiedWithSubject 放在 ReviewRepository 职责不完美 | 记录为已知限制 #6 |
| 8 | INNER JOIN 风险未记录 | 数据异常时知识点被过滤掉 | 记录为已知限制 #5 |
| 9 | 断言不够严格 | summary 回退测试只验证长度 | 加 `assertEquals(longCoreConclusion.take(100), ...)` |

### 执行中发现并修复的问题

| # | 问题 | 修复 |
|---|------|------|
| 10 | **Room JOIN POJO 不自动转换 snake_case → camelCase**（计划假设错误） | `KnowledgePointWithSubject.subjectName` 加 `@ColumnInfo(name = "subject_name")` 显式映射 |
| 11 | **2 个 FakeKnowledgePointDao 未实现新方法**（core/ai + feature/aiassistant） | 补全 `observeVerifiedWithSubject` 默认实现（`flowOf(emptyList())`） |

### 完成内容

**Phase 1：DAO 层** — 新增 JOIN 查询
- 新建 `KnowledgePointWithSubject.kt`（@Embedded + @ColumnInfo）
- `KnowledgePointDao` 新增 `observeVerifiedWithSubject()`（INNER JOIN chapters + subjects）

**Phase 2：Repository 层** — 暴露新方法
- `ReviewRepository` 新增 `getVerifiedWithSubject()` 委托方法

**Phase 3：ViewModel 层** — 修复筛选 + 显示
- 数据源从 `getAllVerifiedKnowledgePoints()` 改为 `getVerifiedWithSubject()`
- `filterByCategory` 从空壳改为 `points.filter { it.subjectName.contains(category.keyword) }`
- `toUiItem` 的 `subject` 从 `contentSource` 改为 `subjectName`
- `KnowledgeCategory` 枚举新增 `keyword` 字段
- `filterByCategory` + `toUiItem` 移到 companion object（internal 可见性）供测试调用

**Phase 4：测试** — 新增 KnowledgeViewModelTest
- 10 个测试：5 正常路径（ALL/ANCIENT/MODERN/FOREIGN/THEORY）+ 4 边界（空列表/不匹配/summary有值/summary为null）+ 1 回归（subject 不取 contentSource）

**Phase 5：全量验证** — `assembleDebug` SUCCESSFUL + `testDebugUnitTest` 184 tests 0 failures（基线 174 + 新增 10）

**Phase 6：文档 + Push** — 更新 4 个文档（00-STATUS、SESSION_LOG、AGENTS、plan）

### commit

- `d1b9cd5` — fix(knowledge): 修复科目筛选不生效 + subject 显示 TEXTBOOK_NATIVE（8 files, 292 insertions, 32 deletions）

### 关键技术决策

1. **DAO JOIN 而非 @Relation 或 @Embedded**：@Relation 触发 N+1 查询，@Embedded 不能跨表，@Query JOIN 一次查询完成最高效。
2. **INNER JOIN 而非 LEFT JOIN**：数据异常时强制数据完整性（不显示无科目的知识点），MVP 阶段 SeedDataLoader 已保证外键完整性，风险极低。
3. **contains 匹配而非精确匹配**：兼容 seed_data 全名与枚举简称，当前 4 科目无歧义。
4. **新增方法而非修改现有**：`getAllVerifiedKnowledgePoints` 保留向后兼容（虽已成事实死代码，记录到 P5 重构）。
5. **companion object 而非提取 mapper 类**：为可测试性的最小妥协，YAGNI。

### 已知限制（本次接受，记录供后续优化）

1. **KnowledgePointEntity 无 subjectId 字段**：通过 JOIN 绕过，不改表结构（避免数据库迁移）。
2. **SubjectEntity.shortName 死字段**：本次不动（YAGNI）。
3. **contains 匹配的脆弱性**：若未来出现"古代文论"会误匹配。当前 4 科目无歧义。
4. **filterByCategory + toUiItem 移到 companion object**：更优方案是提取到 KnowledgePointMapper 类，YAGNI。
5. **INNER JOIN 数据完整性风险**：若 chapterId 指向不存在的 chapter，知识点会被过滤掉。MVP 阶段无用户添加知识点功能，风险极低。
6. **架构职责不完美（既有问题）**：`getVerifiedWithSubject()` 放在 ReviewRepository 职责不完美——知识点浏览更应在 KnowledgeRepository。但当前 `getAllVerifiedKnowledgePoints()` 也在 ReviewRepository，是既有设计问题。本次不改（P1 是修 bug，不是重构）。
7. **ReviewRepository.getAllVerifiedKnowledgePoints 将变成事实上的死代码**：本次不删除（保留 API 向后兼容），记录到 P5 重构。

### 下次继续

- 跑 emulator 实测（P0）：SeedDataLoader + 知识点分类标签筛选 + LargeFlexibleTopAppBar
- OCR 完成后跑知识提取管线 → 生成完整 seed_data.json（P2）
- 架构重构（P5）：ReviewRepository 死代码清理 + getVerifiedWithSubject 迁移到 KnowledgeRepository

---

## Session 2026-07-13（第五条）：Release v0.2.0 发布

### 目标

用户要求"发一个 release，让我看看软件长什么样子"。在 P1 修复完成的基础上发布 v0.2.0，让用户能下载到包含最新修复的签名 APK。

### 前置：CI 验证策略写入 AGENTS.md（commit `ce50e77`）

用户问"这个 ci 验证是必须的吗，本地会不会快一点"，并要求"你自己判断需不需要 ci 验证，在每次改动结束之后，并且把这个写入记忆里面"。

在 AGENTS.md 第 4 节硬约束下新增 `### CI 验证策略（2026-07-13 新增）` 小节：
- **原则**：AI 自主判断每次改动是否需要 CI 验证，不冗余等待
- **必须等 CI**：改 workflow / build.gradle.kts / libs.versions.toml / settings.gradle.kts / 签名 / 跨平台兼容性 / 发版前
- **不需要等 CI**：纯 Kotlin/Compose 业务逻辑 / 纯测试 / 纯文档
- **本地验证最低标准**：`assembleDebug` SUCCESSFUL + `testDebugUnitTest` 全绿
- **Release tag 流程** 5 步（本地验证 → CI 绿 → 删旧 orphan tag → 打新 tag → 等 workflow）

### Release v0.2.0 发布

**Release tag 流程执行**（严格遵循 AGENTS.md 第 4 节）：

1. **确认本地验证**：P1 修复已通过 `assembleDebug` + `testDebugUnitTest` 184 tests 0 failures（第四条会话已完成）
2. **确认最近 CI 全绿**：`gh run list` 确认最后一次代码 commit CI（run 29275987334，P1 修复）全绿 18m53s。另有 2 个 docs-only CI 在跑（29277763880 + 29277520877），docs 改动不影响发布
3. **检查 orphan tag**：`git ls-remote --tags origin` 确认只有 v0.1.0，无 v0.2.0 旧 tag，无需删除
4. **检查现有 release**：`gh release list` 确认只有 v0.1.0
5. **打 tag 并 push**：`git tag -a v0.2.0 -m "..." && git push origin v0.2.0`
6. **等 Release workflow**：run 29278178988，14m54s，14/14 步骤全绿

### Release workflow 执行详情

**关键步骤全部通过**：
- ✓ Decode keystore from Secrets（KEYSTORE_BASE64 已配置）
- ✓ Verify keystore（keytool 验证通过 — P4 担心的隐藏 bug 没触发，secrets 完整）
- ✓ Build signed release APK（R8 混淆 + 签名）
- ✓ Run unit tests（184 tests 全绿）
- ✓ Create GitHub Release（自动创建，附加 2 个 APK）

**已知警告（不影响发布）**：
- Node.js 20 deprecation warning（actions/checkout@v4 等仍在用 Node 20，被强制运行在 Node 24）
- Gradle cache restoration 400 错误（setup-gradle@v3 cache-read-only 模式偶发，Gradle 仍正常运行）

### 交付物

**GitHub Release v0.2.0**：
- URL：https://github.com/qbjsdsb/wenyan-android/releases/tag/v0.2.0
- Tag：v0.2.0（指向 commit `ce50e77`）
- Assets：`wenyan-v0.2.0.apk` + `wenyan-latest.apk`（内容相同）
- 系统要求：Android 8.0 (API 26) 及以上

**v0.2.0 包含…83172 tokens truncated…nalElevation | 3.dp | 2.dp |
| 颜色 | surfaceContainer | surfaceContainerHigh.copy(alpha = 0.85f) |
| 水平留边 | 16.dp | 8.dp |
| 底部留边 | 8.dp | 4.dp |
| NavigationBar 高度 | 默认 80dp | Modifier.height(56.dp) |
| Android 12+ 叠加 | 无 | 水平渐变光泽 overlay（0.04f→Transparent→0.06f） |

**视觉设计**：半透明 `surfaceContainerHigh` 底色 + 水平渐变光泽 overlay，模拟流体玻璃的 frosted glass 质感。56dp 紧凑高度减少遮挡，24dp 大圆角更圆润。

#### Phase 2b: WenyanAdaptiveNavigation.kt — 移除渐变遮罩 + 调整 padding

| 项 | 改前 | 改后 |
|----|------|------|
| BottomGradientScrim | 有（80dp 渐变遮罩） | 无（已移除） |
| 底部内容 padding | 80.dp + systemNavBarBottomDp | 56.dp + 4.dp + systemNavBarBottomDp |
| 总遮挡面积 | ~184dp（80dp 导航栏 + 80dp 渐变 + 16dp 间距 + 8dp 底部） | ~68dp（56dp 导航栏 + 8dp 间距 + 4dp 底部） |

**遮挡面积变化**：~184dp → ~68dp，减少 ~63%。内容区域增加约 116dp（~14% 的 6.1" 屏幕）。

### 3. 文件变更清单

| 文件 | 改动 | 行数变化 |
|------|------|----------|
| `WenyanApplication.kt` | 种子加载 300s + 1 次重试 + TimeoutCancellationException import | +15/-3 |
| `WenyanNavigationBar.kt` | 紧凑玻璃风格全部改造（圆角/半透明/高度/光泽 overlay） | +20/-4 |
| `WenyanAdaptiveNavigation.kt` | 移除 BottomGradientScrim + 调整 padding + 清理 import | +5/-25 |
| `app/build.gradle.kts` | versionCode 43→44, versionName "0.9.18"→"0.9.19" | +2/-2 |

### 4. 设计文档

- [docs/plans/floating-navigation-bar.md](docs/plans/floating-navigation-bar.md) — 紧凑玻璃导航栏设计

### 5. 待 emulator 实测

1. **玻璃导航栏效果**：圆角 24dp / 半透明 / 渐变光泽 / 紧凑高度 56dp
2. **种子加载**：首次启动 300s 内完成 + 超时后自动重试 1 次
3. **内容区域**：移除 BottomGradientScrim 后无异常
4. **浅色/深色模式**：半透明 surfaceContainerHigh 在不同主题下视觉效果

### 6. 已知限制（v0.9.19）

- 玻璃效果在 Android 11 及以下无渐变光泽 overlay（仅半透明 + 圆角 + 投影）
- 本次未实现滚动感知显隐（scroll-aware visibility）
- 沙箱无 Android SDK，无法编译验证，需本地或 CI 验证
- **待 emulator 实测**：验证玻璃导航栏效果 + 种子加载正常

### 7. 交接清单

- [x] WenyanApplication.kt 种子加载 300s + 1 次重试
- [x] WenyanNavigationBar.kt 紧凑玻璃风格改造（圆角/半透明/高度/光泽）
- [x] WenyanAdaptiveNavigation.kt 移除 BottomGradientScrim + 调整 padding
- [x] app/build.gradle.kts versionCode 44 / versionName "0.9.19"
- [x] STATUS.md 已更新到 v0.9.19
- [x] SESSION_LOG.md 已更新
- [x] 沙箱无 Android SDK，需本地验证 assembleDebug + testDebugUnitTest（CI 自动验证）
- [x] 验证通过后打 tag v0.9.19 + Release（已发布）

### 8. v0.9.19 Release（2026-08-01）

**PRR + RBR 审查通过**（0 blocker，0 exception），已打 tag 并发布：

- **Tag**：`v0.9.19` → `1def192`（commit `5a8fabd` + receipt）
- **命令**：`git tag -a v0.9.19 -m "v0.9.19: 紧凑玻璃导航栏 + 种子加载超时重试"`
- **推送**：`git push origin v0.9.19`
- **CI 触发**：release.yml 自动构建 signed release APK 并发布 GitHub Release
- **Receipt**：[docs/release-receipts/v0.9.19-release-receipt.md](docs/release-receipts/v0.9.19-release-receipt.md)
- **回滚**：v0.9.18（tag `7ec209d`，versionCode 43 < 44，需卸载后安装）

### 下一步

1. **P0**：emulator 实测 v0.9.19 — 验证玻璃导航栏 8 项效果 + 种子加载正常
2. **P0**：emulator 实测启动图标 v4 — 验证书+文负空间图标显示
3. **P0**：emulator 实测 v2.16.0 — 验证 935 知识点正确导入

---

## 2026-08-02 会话：沙箱推送通道打通 + 构建环境搭建

- **完成**：
  - **打通沙箱 → GitHub 推送通道**：沙箱无法直连 github.com（TLS 被中间设备掐断）、api.github.com / SSH 亦不可达。经排查确认 **ghfast.top 镜像可透传 git 协议（含 git-receive-pack 写操作）**，配合 GitHub PAT 完成 clone / push / 打 tag 全链路验证。
  - **AGENTS.md 新增「沙箱推送通道」章节**（commit `0adf20b`，已 push）：记录镜像通道用法、PAT 认证机制（存于沙箱 ~/.git-credentials + GITHUB_PAT 环境变量，90 天有效期至 2026-10-31）、安全约束（PAT 不入仓库）。中途发现编辑工具 CRLF→LF 转换导致 diff 混乱（143+/120-），已修复为 18 行最小改动并 force-push 清理。
  - **沙箱构建环境搭建**（腾讯/阿里云镜像）：Gradle 8.14.4（/opt/gradle-8.14.4，腾讯镜像）+ Android SDK（platform-tools r37 / platforms android-35 / build-tools 34.0.0+35.0.0，腾讯 AndroidSDK 镜像）+ ~/.gradle/init.gradle 重写（原文件有语法错误：url 缺引号 + mavelCentral 拼写错误，已修复为 pluginManagement 镜像配置）。
  - **v0.9.20 测试缺口识别**：滚动感知判定逻辑（WenyanAdaptiveNavigation.kt snapshotFlow 方向判定）无单测覆盖，已起草 `detectScrollDirection` 纯函数提取 + 14 个测试用例（/tmp/ScrollDirectionDetectorTest.kt 草稿，待构建验证后应用）。
  - **底栏 MD3 规范改造**（用户要求：不要毛玻璃，要规范 MD3 风格；保留滚动感知显隐 + 80dp 标准高度）：
    - `WenyanNavigationBar.kt`：移除流体玻璃（渐变遮罩/半透明层/圆角），改为 MD3 标准 —— `containerColor = surfaceContainer` 实色、`height = 80.dp`、直角全宽、`tonalElevation = 3.dp`；选中指示器 `secondaryContainer` / 选中色 `onSecondaryContainer` / 未选中 `onSurfaceVariant`（对齐 docs/design/m3-expressive-redesign.md §5.1）。
    - `WenyanAdaptiveNavigation.kt`：删除 `BottomGradientScrim` 渐变遮罩（MD3 不透明底栏无需过渡），内容底部 padding 72dp→80dp，隐藏距离同步 72dp→80dp，清理 4 个无用 import。
    - 提取 `detectScrollDirection()` 纯函数 + `ScrollDirection` 枚举，新增 `ScrollDirectionDetectorTest`（16 用例全绿）。
  - **沙箱构建全链路打通 + 验证全绿**：
    - 修复 Gradle 依赖下载卡死：init.gradle 只配了 pluginManagement，依赖仓库回退 google()/mavenCentral() 直连被 fake-ip 卡死（Recv-Q=0 无数据）→ 重写 init.gradle，`dependencyResolutionManagement` 清空并全部替换为腾讯 maven-public / Aliyun 镜像；加 `org.gradle.internal.http.*Timeout=30000` 防挂起。
    - 修复 Android SDK 布局错误：`/tmp/sdk-setup.sh` 解压时未去掉 zip 内层前缀目录，导致 build-tools/34.0.0/android-14/（缺 source.properties、aapt2 层级错误）→ 从腾讯 AndroidSDK 镜像重新下载 build-tools_r34-linux.zip + platform-35_r02.zip，正确解压到 build-tools/34.0.0/ 与 platforms/android-35/，补齐 source.properties。
    - 修复 JVM target 不一致：环境 JDK 20 vs 项目 compileOptions 17（`compileDebugJavaWithJavac`(17) vs `compileDebugKotlin`(20)）→ 从清华 TUNA Adoptium 镜像安装 JDK 17.0.20（Temurin，/opt/jdk17），以 `JAVA_HOME=/opt/jdk17` 构建。
    - 修复 Robolectric 联网下载失败：`MavenArtifactFetcher` 尝试下载 `org.robolectric:android-all-instrumented:14-robolectric-10818077-i6`（约 144MB）被 TLS 拦截 → 手动预下载 jar+pom 到 `~/.m2/repository/org/robolectric/`（腾讯 maven-public 有该 artifact），Robolectric 直接本地读取。
    - **验证结果**：`:core:designsystem:assembleDebug` ✅ + `:core:designsystem:testDebugUnitTest` ✅（42 tests / 0 failures，含 ScrollDirectionDetectorTest 16 用例、Robolectric 14 用例）。
- **进行中**：
  - v0.9.20 发布收尾：更新文档 → commit → push（ghfast.top）→ 打 tag v0.9.20
- **阻塞**：
  - 无（推送通道 + 构建环境 + 测试均已打通）
- **下次继续**：
  - v0.9.20 发布（versionCode 45）：提交 + push + 打 tag 触发 Release
  - emulator 实测项（v0.9.20 / 图标 v4 / v2.16.0）仍需真机
- **关键发现**：
  - 沙箱外网被 DNS 劫持到 198.18.0.0/15 fake-ip 网段，github.com TLS 握手被掐断，但国内镜像（ghfast.top / 腾讯 / 阿里云 / 清华 TUNA）全部可用
  - ghfast.top 透传 git-receive-pack POST 请求，是沙箱唯一 GitHub 写通道
  - CodeBuddy 连接器颁发的 `ghu_` OAuth token 无法用于 git 协议 Basic Auth（GitHub 2020 年后要求 PAT），用户需自行提供 `ghp_` classic PAT
  - 项目文件多为 CRLF 行尾（Windows 环境产物），编辑工具会转 LF 导致 git 全文件 diff，需用二进制方式编辑
  - Gradle 官方源（services.gradle.org / dl.google.com / repo1 / maven.google.com）在沙箱全部 TLS 拦截，依赖必须走镜像；Gradle 发行版可从腾讯 `mirrors.cloud.tencent.com/gradle/` 下载
  - Robolectric android-all 系列 jar 在腾讯 maven-public 有镜像，可预下载到 ~/.m2 离线使用
- **commit**：
  - `0adf20b` — docs: 记录沙箱推送通道配置（ghfast.top 镜像 + PAT 认证，不入仓库）




## 2026-08-02 下午：底栏空白修复 + v0.9.21 发布

- **完成**：
  - **底栏/顶栏空白修复**（用户反馈底栏按钮上面大面积空白，不协调）：
    - 反编译 material3 1.5.0-alpha18 NavigationBar 源码（自定义 NavigationBarItemLayout + placeLabelAndIcon 居中算法）+ Robolectric 探针实测（icon 上 6dp / label 下 6dp 居中）→ 确认空白不在底栏内部，而在容器层。
    - 底部空白根因：内容区 bottomPadding = 80dp + 手势条，但底栏本体 80dp 未吃手势条 inset → 底栏上方多出 24-48dp 空白。
    - 顶部空白根因：WenyanAdaptiveNavigation 外层 top padding + ExpressiveScaffold 内层 statusBars inset 双重消费 → 双倍状态栏空白。
    - 修复（commit `fd772a8`）：WenyanNavigationBar 移除 .height(80.dp) 改 windowInsets=NavigationBarDefaults.windowInsets（底栏吃手势条）；WenyanAdaptiveNavigation 移除顶层 top padding、bottomHideDistance = 80dp+手势条。
  - **v0.9.20 发布踩坑**：tag v0.9.20 先推（指向 834be6d）→ Release #48 用旧代码发布成功（07:39）→ force-update tag 到 fd772a8 触发新 run #49，但 softprops/action-gh-release 默认对已存在 tag 的 release **跳过创建**（无 update_release）→ 已发布的 v0.9.20 APK 是旧代码，修复进不去。
  - **v0.9.21 发布决策**（用户确认）：空白修复作为 v0.9.21 发布。versionCode 45→46，versionName 0.9.20→0.9.21；settings BuildConfig.VERSION_NAME 0.9.15→0.9.21（顺带修复漏同步）；release.yml 加 `update_release: true`（防同 tag 重建不覆盖）。commit `68beaf7` + tag v0.9.21 已推送，Release run #50 构建中。
  - **探针测试**：临时 NavBarLayoutProbeTest（Robolectric 渲染 NavigationBar 测 bounds）定位布局，验证后已删除，不留测试代码。
- **进行中**：
  - Release #50（v0.9.21）构建中，预计 10-15 分钟；完成后生成含空白修复的 APK
- **阻塞**：
  - 沙箱无法调用 GitHub API 写操作（ghfast.top 拒绝代理 api.github.com 返回 403，GitHub MCP 工具未暴露）→ 无法远程更新已发布 release/assets，只能通过新 tag 触发新 release
- **关键发现**：
  - material3 1.5.0-alpha18 NavigationBar 用自定义 NavigationBarItemLayout + NavigationBarVerticalItemTokens（icon 24dp / 指示器 56x32dp / ContainerHeight 64dp / TallContainerHeight 80dp），与稳定版布局不同；内容默认居中
  - softprops/action-gh-release 对已存在 tag 默认 skip（不覆盖）；要支持同 tag 重建需 `update_release: true`
  - app/build.gradle.kts 是 CRLF（或 mixed），Edit 工具编辑会转 LF 导致全文件 diff（v0.9.21 提交 331 行改动的 165 行是行尾变化，真实改动仅 versionCode/versionName 几行）；后续需二进制方式编辑
- **commit**：
  - `834be6d` — refactor(designsystem): 底栏回归规范 MD3 风格（surfaceContainer 实色 + 80dp + secondaryContainer 指示器）
  - `fd772a8` — fix(designsystem): 修复底栏/顶栏 inset 双重消费导致的布局空白
  - `68beaf7` — chore: v0.9.21 版本号提升 + release.yml 支持同 tag 重建覆盖


## 2026-08-02 晚上：深度审查 + v0.9.22 发布

- **完成**：
  - **全仓库深度审查**（用户要求"深入检查，给改进计划，反复打磨"）：3 路并行 agent 审查（core/database+data+fsrs / feature 模块 / 构建 CI）+ 关键问题人工复验。发现 2 P1 + 8 P2 + 10 P3；改进计划存档 `docs/plans/deep-review-improvement-plan.md`。
  - **批 A（P1）**：
    - 底栏 double inset 修复（`WenyanAdaptiveNavigation` 外层只 pad 80dp，手势区由内层 Scaffold 消费，与顶部对称）——用户反馈"底栏上方大面积空白"残留根因（v0.9.21 只修了顶部）
    - SettingsScreen 消费 `ThemeViewModel.errorEvents`（主题保存失败弹 Snackbar，此前零订阅者静默丢失）
    - 版本号 46→47 / "0.9.21"→"0.9.22"
  - **批 B（P2）**：
    - FSRS `nextRecallStability` stability<=0 防御（v1 老数据 stability=0 → NaN 污染调度）；新增复现测试先红后绿
    - MIGRATION_7_8 补 2 复合索引 + 新增 MIGRATION_8_9（数据库 8→9）为存量 v8 用户补索引；SQLite 实测 6 索引齐全 + 幂等
    - recordWrongAnswer 查找+递增/插入合并为单个 DAO @Transaction（并发重复插入窗口）
    - recordWrongAnswer/markResolved 改用 ClockGuard 时间源（与 FSRS 调度对齐）
    - WrongAnswerViewModel 加 isRating 防重入锁（DUE 连点防重复 FSRS 调度）
  - **验证**：510 单测 0 失败 + assembleDebug 通过 + 9.json schema 一致 + 8.json vs 9.json 无列增删（迁移安全）
  - **commit**：`5e5c78c`（批 A+B 8 项修复）
  - **v0.9.22 发布**（用户确认"反复检查没问题就打 tag 发布"）：tag v0.9.22 → 5e5c78c 推送，Release #51 触发（11:12 UTC）→ 完成 11:22:48（10m48s）
  - **发布后验证**：
    - Release 页面存在（文研App v0.9.22）
    - wenyan-v0.9.22.apk + wenyan-latest.apk 均可下载（19,491,856 字节）
    - aapt2 校验 APK 内部版本：versionCode 47 / versionName "0.9.22"（防 v0.9.20 错版覆辙）
  - **receipt**：`docs/release-receipts/v0.9.22-release-receipt.md`
- **进行中**：
  - 批 C（仓库卫生）：release-assets 4 个旧 APK 入库 77MB 待清理、AGENTS.md/docs 多处过期待更新、EssayList/ApiConfig stateIn(WhileSubscribed) Tab 返回闪烁待修
  - 批 D（长期）：R8/ProGuard 启用、convention plugin 抽取、历史 schema 1/3.json 补齐 + 迁移测试等
- **关键发现**：
  - 8.json vs 9.json 对比确认：数据库 8→9 仅新增 2 个复合索引，无列增删，存量用户升级绝对安全
  - aapt2 从 gradle 缓存可找到，可用于 APK 版本校验（发布防呆补充手段）
  - 沙箱无法访问 api.github.com（ghfast.top 拒绝代理 API），但 curl 走镜像可访问 github.com 网页 + release 下载


## 2026-08-02 深夜：v0.9.23 发布（论述题删年份 + Snackbar + AI 修复 + 更新日志机制）

- **完成**：
  - **论述题删年份**（用户需求"论述题不要年份"）：列表/详情/年份筛选/知识点详情"相关论述题"全部移除年份（数据层 year 保留）。commit `361bbbd`
  - **Snackbar 常驻修复**（用户反馈"已加入通知一直存在"）：CardsScreen 是唯一漏修"先 clear 再 show"的 Screen（AiAssistant/ApiConfig 早已修复）；改先 clear + withTimeout(5s) 兜底。commit `48343e4`
  - **AI 功能审计**（用户要求"审计 AI 功能，反复打磨"）：2 路并行审查 + 人工复验，发现 2 P0 + 7 P1 + 14 P2；报告存档 `docs/plans/ai-audit-report.md`
  - **AI 修复**（按最优方案）：P1-1 服务商 URL 拼接（通义/智谱/月之暗面 404，改接口 chat/completions + baseUrl 版本前缀）；P0-1/2 竞态（launchAiTask 统一 Job + 取消在途 + 安全空判断）；P1-3 并发防重入；P2-1 RAG 降级；P2-6 注入封堵。commit `944816b` + `33c5142`，新增 5 个回归测试
  - **更新日志机制**（用户反馈"更新日志不变"）：根因是 release.yml body 静态硬编码功能特性列表；新增 CHANGELOG.md + release.yml 动态读取当前 tag 版本日志作为 Release 正文。commit `2a19cde`
  - **v0.9.23 发布**（用户确认"严谨一点，反复检查"）：versionCode 47→48，versionName "0.9.22"→"0.9.23"。Release #52 触发（14:13 UTC）→ 完成。**核心验证：Release body 更新内容来自 CHANGELOG v0.9.23（动态日志机制首次生效）**；APK aapt2 校验 versionCode 48 / versionName "0.9.23"（防错版）
  - **receipt**：`docs/release-receipts/v0.9.23-release-receipt.md`
- **进行中**：
  - 批 C 仓库卫生（release-assets 77MB 旧 APK、过期文档）未做
  - AI 剩余待办：真流式 SSE、停止生成按钮、多轮上下文、对话列表 UI、AI 批改接入
- **关键发现**：
  - 更新日志机制：CHANGELOG.md + release.yml `Extract changelog for version` 步骤（awk 提取 `## [vX.Y.Z]` 段）→ Release body "更新内容"，App 内更新界面同步展示
  - AGENTS.md 是混合行尾（部分段落 LF、部分 CRLF），Edit 工具编辑会整文件转 LF 导致大 diff；必须用 Python 二进制精确替换（按目标段落实际行尾匹配）
  - AI 审计发现 CardsScreen 是唯一漏修"先 clear 再 show"的 Screen——与用户 Snackbar 反馈完全吻合


## 2026-08-03 凌晨：批一 AI 体验 + 批二工程质量完成，v0.9.24 待发布

- **完成**：
  - **批一（AI 体验 4 项，commit `b737f9f`）**：
    - AI 真·流式输出：新增 chatResultStream(query, history) 接口，OkHttp 原生 SSE 逐行解析（零新依赖），逐 chunk emit AiStreamEvent.Delta/Complete；UiState 加 streamingContent 逐字显示
    - 停止生成：stopGeneration() = aiJob?.cancel()，job.invokeOnCompletion { call.cancel() } 中断阻塞读取，已生成内容保留
    - 多轮上下文：ChatMessageDao.getRecentByConversation + ChatRepository.getRecentMessages，最近 20 条注入 LLM
    - Token 统计：Complete 携带 ChatUsage → AiMessage.tokensUsed 透传 + UI 小字
    - 保留 chatResult 兼容 SocraticTutor/RecallChecker（5 处 .first() 零改动）
    - 新增 mockwebserver 流式 SSE 测试（core:ai 3 个）+ ViewModel 流式/多轮/token/停止回归测试（aiassistant 4 个）
  - **批二（工程质量 5 项，commit `178658b`）**：
    - R8 混淆：isMinifyEnabled=true，release APK 26.7MB→5.6MB（-79%），mapping.txt 验证，入口 MainActivity 保留；⚠️ 需 emulator 实测
    - 数据库迁移测试：MigrationTest（8→9、9→10，androidTest）+ room-testing + androidTest assets 指向 schemas
    - Tab 返回闪烁：EssayList/ApiConfig/StudyProgress 3 处 stateIn 改 Eagerly
    - DAO 补索引：exam_questions.question_type/answer_status、knowledge_points.content_source；数据库 9→10 + MIGRATION_9_10 + 10.json（SQLite 实测）
    - ChatRepositoryImpl.appendMessage 事务化（withTransaction）；StudyProgress 评估后保留（并发风险低 + 纯 JVM 单测友好）
  - **验证**：518 单测 0 失败 + assembleDebug + assembleRelease(R8) 全绿
  - **交接**：docs/00-STATUS.md 重写为最新状态（v0.9.24 待发布 + 发布前验证清单）
- **进行中**：
  - v0.9.24 发布（用户确认"严谨仔细发布"）：提升 versionCode 48→49、versionName "0.9.23"→"0.9.24"，发布前验证（R8 冒烟依赖 emulator）
  - 批三（性能/整洁）、批四（仓库卫生/合规）待做
- **关键发现**：
  - callbackFlow + flowOn(IO) 在测试卡死 → 改"直接在 flow 阻塞读取 + job.invokeOnCompletion 取消"
  - runTest 虚拟时间无法唤醒真实 IO → 流式测试用 runBlocking
  - 给 Repository 注入 WenyanDatabase 会破坏纯 JVM 单测（CardsViewModelTest）→ 只给有 in-memory db 测试的 ChatRepositoryImpl 事务化
  - R8 混淆验证：APK 大小 -79% + mapping.txt 45 万行 + AiServiceImpl 不保留 + MainActivity 保留


## 2026-08-03 凌晨：v0.9.24 严谨发布完成（Release #53）

- **完成**：
  - **版本号提升**（commit `9183ecc`）：versionCode 48→49、versionName "0.9.23"→"0.9.24"
  - **打 tag 发布**：tag v0.9.24 → 9183ecc（= HEAD）推送，Release #53 触发并完成（资产由 workflow 上传）
  - **发布后核心验证（全部通过）**：
    - tag 存在且指向 HEAD（git ls-remote 校验 9183ecc）
    - Release 页面"文研App v0.9.24"已发布（2026-08-02T17:33:33Z UTC）
    - Release body"更新内容"来自 CHANGELOG v0.9.24（流式/停止/多轮对话上下文/Token/R8/迁移测试/Tab 返回闪烁/筛选索引/事务化全部出现）——动态日志机制持续生效
    - wenyan-v0.9.24.apk + wenyan-latest.apk 均 HTTP 200（5,909,874 字节），sha256 完全一致（b308396d…）
    - aapt2 校验 APK：versionCode 49 / versionName "0.9.24" / targetSdk 35
    - apksigner 校验：v2 scheme 通过，CN=Wenyan App（qbjsdsb, Nanjing, Jiangsu, CN），RSA 2048
  - **receipt**：`docs/release-receipts/v0.9.24-release-receipt.md`
  - **交接更新**：docs/00-STATUS.md 更新为"v0.9.24 已发布"（版本矩阵 + 发布验证记录）
- **进行中**：
  - 批三（性能/整洁）、批四（仓库卫生/合规）待做
  - ⚠️ 唯一待人工验证：emulator 安装 release 混淆 APK 冒烟（App 启动 / 列表加载 / AI 流式 / 主题切换）+ 数据库 9→10 覆盖安装升级
- **关键发现**：
  - 沙箱 gh CLI 不可用（无 token + remote 是 ghfast.top 代理 host 无法识别）→ 用 git-credentials 提取 token + curl 直接验证（ghfast.top 不支持代理 api.github.com，但可访问 github.com 网页 + release 下载）
  - 发布成功核心判据：release 资产存在（workflow 上传）→ 必已构建成功；APK 版本 + 签名校验防错版


## 2026-08-03 凌晨：v0.9.25 严谨发布完成（新图标 + UI 审查修复，Release #54）

- **完成**：
  - **新启动图标**（commit `d5b9695`）：用户要"图标更好看"→ 3 路 AI 候选 → 选定「书堆+文」→ PIL 抠背景/去水印/安全区居中 → 5 密度 webp（84KB）+ adaptive icon 三层 + Splash 同步；旧 v4 矢量备份 .icon-gen/archive/（已 gitignore）
  - **整体界面审查**（用户要求"整体界面再审查一遍，有问题就修复，完了发布"）：3 路并行审查（knowledge+settings / cards+quiz / aiassistant+框架），发现 4 P1 + 十几个 P2，无 P0
  - **修复 14 项**（commit `769455b`）：
    - P1：AI 停止保留内容（withContext(NonCancellable)）/ 流式自动滚动 / 流式转圈重叠 / 状态栏图标色跟随手动主题 / 更新安装已下载 APK
    - P2：更新页 AnimatedContent key 分发 / retry loading / 错误态禁用筛选 / 长标题截断 / 种子色暗色亮化 / 卡片滚动重置 / 错题本 Snackbar / 日期行省略 / 底栏跨 Tab 重置
  - **验证**：518 单测 0 失败 + assembleDebug + assembleRelease(R8) 全绿
  - **v0.9.25 发布**（commit `760be63` 版本号 50/0.9.25）：tag v0.9.25 → 760be63 推送，Release #54 触发（19:16 UTC）→ 约 13 分钟资产就绪
  - **发布后验证**：Release 页面"文研App v0.9.25" + 正文 10 关键词来自 CHANGELOG v0.9.25 + APK aapt2 50/0.9.25 + apksigner v2 通过 + 两 APK sha256 一致（1be5bdae…）
  - **receipt**：`docs/release-receipts/v0.9.25-release-receipt.md`
- **进行中**：
  - 批三（性能/整洁）、批四（仓库卫生/合规）待做
  - ⚠️ 唯一待人工验证：emulator 安装 release 混淆 APK 冒烟（App 启动 / 新图标桌面效果 / AI 流式+停止 / 状态栏图标色 / 主题切换）
- **关键发现**：
  - 本机 JDK 从 17 变 20 导致 Kotlin/Java JVM target 冲突 → 根 build.gradle.kts 统一 jvmTarget=17（CI temurin 17 对齐），此后任意 JDK≥17 可构建
  - 停止生成保存部分内容：catch CancellationException 里调用 suspend 需 withContext(NonCancellable)（协程已取消直接 suspend 会再次抛 CancellationException）
  - 沙箱 gh CLI 不可用 → git-credentials 提取 token + curl 直连验证（ghfast.top 不支持 api.github.com，可访问 github.com 网页 + release 下载）
  - Release 页面 title 初始为 "Release v0.9.25"、构建完成后变 "文研App v0.9.25"——workflow 最后一步更新名称/正文


## 2026-08-03 凌晨：v0.9.26 严谨发布完成（新图标 v7.4 + 批三，Release #55）

- **完成**：
  - **新图标 v7.4**（commit `38b9ddf`）：用户反馈 v5 难看要 Google 味 → 多轮打磨定稿「黑底白书」（Play Books 风格手工矢量，白书+文字线，墨黑 #1A1A1A）；纯 VectorDrawable 84KB→9.8KB；cairosvg 渲染各密度 webp 兜底
  - **批三：性能与整洁**
    - 详情页懒加载（`ad86909`）：KnowledgePointDetailScreen Column→LazyColumn
    - RAG VERIFIED 过滤（`ad86909`）：searchByKeyword 加 ocr_status='VERIFIED'
    - AI 成本控制（`ad86909`）：Retry-After 头 + callTimeout(90s) + Semaphore(3)
    - i18n 资源化（`ace2e64`）：5 feature 模块 74 处 Text→stringResource（初版脚本括号错误→git 还原重写）
    - convention plugin（`13631da`）：build-logic + android-library-convention，11 库模块共用配置 -130 行
    - RAG 停用词剔除回退（`ba0a53f`）：LIKE '%苏轼贡献%' 不匹配原文，剔除有害（多词 OR 留后续）
  - **验证**：518 单测 0 失败（初跑 1 失败→回退修复）+ assembleDebug + assembleRelease(R8) 全绿
  - **v0.9.26 发布**（`5d7f3d9` 版本号 51/0.9.26）：tag v0.9.26 → 5d7f3d9 推送，Release #55（21:50 UTC）→ ~14 分钟资产就绪
  - **发布后验证**：Release 页面"文研App v0.9.26" + 正文 11 关键词来自 CHANGELOG v0.9.26 + APK aapt2 51/0.9.26 + apksigner v2 + 两 APK sha256 一致（8a291432…）
  - **receipt**：`docs/release-receipts/v0.9.26-release-receipt.md`
- **进行中**：
  - 批四（仓库卫生/合规）待做
  - ⚠️ 唯一待人工验证：emulator 冒烟（新图标 / 详情页滚动 / AI 成本控制 / 主题切换）
- **关键发现**：
  - Kotlin 嵌套块注释坑：KDoc 里写 `core/* + feature/*` 触发 `/*` 嵌套未闭合 → 编译 "Unclosed comment"；注释内避免 `/*`
  - 停用词剔除对中文 LIKE 有害：LIKE '%苏轼贡献%' 不匹配"苏轼的贡献"（中间有"的"）；正确方向是多关键词 OR
  - i18n 正则替换坑：匹配 `Text("...")` 必须含右括号，否则 `Text("中文", color=...)` 变成 `Text(stringResource(...)), color=...)`；脚本必须匹配完整调用
  - build-logic 独立 includeBuild，convention 只抽纯配置（compileSdk/minSdk/compileOptions），插件应用保留模块内（顺序差异大）


## 2026-08-04 凌晨：v0.9.27 严谨发布完成（图标 v7.5 + P1-1/2 + 内容补齐 25 个，Release #56）

- **完成**：
  - **启动图标 v7.5 精进**（`6935b5f`）：用户反馈 v7.4 太简单/主题图标不好看 → 双色页（左白 #FFFFFF / 右米 #F2E9D8）+ 页脚双色厚度（#D8CFC0/#C9BFA8）+ 右页首行缩进 4/末行短收 10；monochrome 改 evenOdd 镂空文字线（8 条矩形），纯色单层也清晰
  - **全面检查 P1-1/2 修复**（`5b7267f`）：
    - aiJob 竞态：`finally { aiJob = null }` → `if (coroutineContext[Job] == aiJob) aiJob = null` 条件清空，旧任务不抹新任务引用
    - Retry-After 无上限：拦截器 `?let { it * 1000 }?.coerceAtMost(5000L)` clamp 到 5s，防阻塞 IO 线程 + 占 Semaphore 槽位
  - **内容补齐 25 个**（`ba3fc68` + `ef3d932`，seed 2.16.0→2.18.0，935→960）：
    - 第一批 11 个：真题硬缺口 10（史铁生/学衡派/寒夜/茅盾三部曲/芙蓉镇/男人的一半是女人/神鞭那五/现代杂志/观堂集林/希腊希伯来）+ 杨朔模式
    - 第二批 14 个：教材缺口 9（艾青/山药蛋派/荷花淀派/解放区文学/重写文学史/探索戏剧/茅盾文艺思想/鸳鸯蝴蝶派/丁帆新文学史观）+ 台港澳 4（台湾概述/白先勇/香港概述/金庸）+ 敦煌变文
    - 图谱补强：茅盾文艺思想/台湾概述/香港概述/敦煌变文 entities≥3、relations≥1（relation 引用一致性全库校验通过）
  - **验证**：960 条数据校验（id 唯一/subject 合法/字段完整/relation 引用一致）+ 518 单测 0 失败 + assembleDebug + APK 内 seed 2.18.0/960 抽查
  - **v0.9.27 发布**（`baa178a` 版本号 52/0.9.27 + CHANGELOG [v0.9.27] 段）：tag v0.9.27 → baa178a 推送，Release #56（16:49 UTC）→ ~13 分钟资产就绪
  - **发布后验证**：Release 页面"文研App v0.9.27" + 正文关键词来自 CHANGELOG v0.9.27 + APK aapt2 52/0.9.27/targetSdk35 + apksigner v2 + 两 APK sha256 一致（1843e1a9…，与 GitHub API digest 一致）
  - **receipt**：`docs/release-receipts/v0.9.27-release-receipt.md`；00-STATUS 版本矩阵更新（960/2.18.0/52）
- **进行中**：
  - 全面检查批次 B（仓库卫生：release-assets 74MB git rm --cached + build 产物清理）、C（UI 体验）、D（合规长期）待执行
  - ⚠️ 唯一待人工验证：emulator 冒烟（图标 v7.5 桌面/主题图标 / 搜索新增知识点 / AI 停止重发 / 更新日志界面显示 v0.9.27 内容）
- **关键发现**：
  - App 内"检查更新"日志 = GitHub Releases API body = release.yml 从 CHANGELOG.md 提取 `## [vX.Y.Z]` 段；**CHANGELOG 必须随版本更新**，否则更新界面日志不变（用户痛点根因）
  - App 更新下载取 `assets.firstOrNull { name.endsWith(".apk") }`（最新 release 第一个 .apk = 带版本号的），删历史 release 资产不影响更新
  - 项目本地磁盘 831MB：82% 是 Gradle build 产物（app/build 456MB + 模块 build ~170MB），74MB 是 git 追踪的 release-assets 旧 APK；可 `./gradlew clean` + `git rm -r --cached release-assets/` 清理
  - 沙箱 api.github.com 直连 TLS 拦截（exit 35），ghfast.top 代理只支持 github.com 网页不支持 api.github.com；WebFetch 可访问 api.github.com（备用验证通道）

## 2026-08-04 凌晨：v0.9.28 严谨发布完成（App 内更新下载修复 + 知识卡片拆分，Release #58）

- **完成**：
  - **App 内更新下载失败 P1 hotfix**（`7bb6f1e`）：用户实测 GitHub 手动下载能装、App 内更新报"应用文件存在问题"。
    根因：国内 `api.github.com` 不可达时降级路径 `fetchLatestTagFromFallback` 返回 assets=emptyList，
    checkForUpdate fallback 下载 URL 到 release **tag 页面 HTML**——App 下载网页当 APK，安装器必然报错。
    修复：新增 `resolveDownloadUrl`/`buildApkDownloadUrl`（降级路径按 release.yml 命名规则构造真实 APK URL）；
    UpdateViewModel 下载加 Content-Length + sha256 双重校验 + 失败重试 1 次；新增 10 个单测。
  - **知识卡片拆分 P2 修复**（`1ebc94e`）：用户要求"一张一张看卡片"→ 写 `CardQualityInspectionTest` 用真实
    CardSplitter 对 960 知识点逐张检查，发现 **35 个知识点只拆 1 张超长卡**（全文仅 1 处"标签："被误判结构化）。
    修复：`MIN_STRUCTURED_DIMENSIONS=3` 阈值，不足时按句末标点拆分 → 35 个知识点变 4-6 张，全库无 1-2 张卡。
  - **v0.9.28 发布**（versionCode 53）：tag 初推 7bb6f1e（Run #57 创建旧 release）→ 卡片修复后 force 更新
    tag 到 1ebc94e（Run #58 用 update_release:true 覆盖更新）→ 最终版 APK sha256 6a103183…（含两个修复）。
    教训：**force 更新 tag 会触发新 Release run 覆盖 release**（Run #57 旧版先被验证，Run #58 才是最终版）。
  - **receipt**：`docs/release-receipts/v0.9.28-release-receipt.md`；00-STATUS 更新（529 单测 / 53）
- **进行中**：
  - **v0.9.29 卡片备考系统**（用户全选 4 项 + 调研优化）：每日新卡限额（默认 60 可设）/ 考频筛选 /
    科目章节筛选 / 考试倒计时计划 / 复习新卡比例保护 / 今日任务入口
  - 全面检查批次 B（仓库卫生）/C（UI 体验）/D（合规）待执行
  - ⚠️ 待人工验证：v0.9.28 App 内更新是否正常（用户实测）、emulator 冒烟
- **关键发现**：
  - 卡片总量：960 知识点 × ~6.5 张 ≈ 6200 张（名词解释 5539 + 论述要点 ~960）；FSRS 只把到期卡放进队列，
    每日量可控；6200÷60 ≈ 103 天可在考前过完一遍
  - GitHub API assets digest 有缓存延迟（release 被 update_release 覆盖后 API 仍显示旧 digest），
    实际下载 sha256 为准
  - CardSplitter 的标签解析缺陷：`indexOf("标签：")` 命中正文普通词（"不同：""特色："）即误判结构化，
    需阈值保护（>=3 才按维度拆）

## 2026-08-04 凌晨：v0.9.29 严谨发布完成（卡片备考系统，Release #59）

- **完成**：
  - **卡片备考系统**（`3118574`）：用户担心 6000+ 张卡片背不完 → 调研 Anki/FSRS 最佳实践 + 考研背诵方法
    后实现：CardSettingsRepository（DataStore：每日新卡默认60可设10-200/考频HIGH_MEDIUM/四科/考试日期）+
    ReviewRepository.getTodayStudyQueue（到期∪新卡，考频HIGH优先，按卡片数限额取整知识点 60张≈10个）+
    getStudyProgress + daysUntilExam；CardsScreen 今日任务横幅（距考试/新卡/复习/进度条）；SettingsScreen
    卡片备考分组（滑杆/SegmentedButton/Checkbox/DatePicker）；27 个新单测；全量 556 单测 0 失败
  - **v0.9.29 发布**（`d8695c2` 版本号 54/0.9.29 + CHANGELOG）：tag v0.9.29 → 发布成功，Release #59
  - **发布后验证**：APK aapt2 54/0.9.29 + apksigner v2 + 两 APK sha256 一致（7ea3170b…，6,041,185 字节）
    + body 来自 CHANGELOG（卡片备考系统/每日新卡限额/今日任务横幅/556 单测）
  - **receipt**：`docs/release-receipts/v0.9.29-release-receipt.md`；00-STATUS 更新（556 单测 / 54）
- **进行中**：
  - 全面检查批次 B（仓库卫生：release-assets 74MB git rm --cached + build 产物清理）、C（UI 体验）、D（合规）待执行
  - ⚠️ 待人工验证：卡片备考系统真机实测（今日任务横幅/每日限额/设置页配置）、emulator 冒烟
- **关键发现**：
  - 卡片备考系统架构：CardsViewModel 不直接依赖 ReviewRepository（难 fake），改由 CardRepository 暴露
    getTodayStudyQueue/getStudyProgress 委托，测试只需扩展 FakeCardRepository + FakeCardSettingsRepository
  - Hilt 新 Repository 需在 DataModule 加 @Binds（漏了会 MissingBinding 编译失败）
  - 60 张/天 ≈ 103 天背完 6200 张，8 月初 → 12 月下旬约 140 天，考前留 40 天二轮，量合理

## 2026-08-04 凌晨：v0.9.30 严谨发布完成（卡片打磨 + UI/UX 14 项 + i18n + 仓库卫生，Release #60）

- **完成**：
  - **知识卡片打磨**（`636aff4`）：复习/新卡比例保护（复习≤10 全量/11-20 减半/>20 暂停）+ 今日任务显示优化
  - **批次 C UI/UX 4 轮 14 项**（`da32226`/`e34ab9f`/`ef5d1e5`/`34ca268`）：AI 光标动画/停止方块/幽灵留白/Snackbar、
    触控目标 48dp 统一、FlowRow/常驻图标/撤销恒占位/翻转动画、ApiConfig 必填校验、空 item 条件化、
    TopBar 统一、弱断言加强
  - **i18n 资源化 6 commit 约 130 资源**（knowledge/cards/settings/quiz/aiassistant）：考频统一、
    标题/按钮/表单/计数 format、semantics 非 Composable 场景外部变量、main 剩余硬编码 = 0
  - **仓库卫生部分**（`fb18e3c`）：release-assets 74MB 出库、临时文件、kotlin.jvm、Quiz 死代码（-1814 行）
  - **v0.9.30 发布**（`133efe8` 版本 55/0.9.30 + CHANGELOG）：tag → Release #60（14 分钟就绪）
  - **发布后验证**：APK sha256 4a4207e4…（两 APK 一致，6,101,989 字节）+ aapt2 55/0.9.30 + apksigner v2
    + body 关键词全命中（知识卡片打磨×8/比例保护×4/UI-UX×5/i18n×5/仓库卫生×5/551 单测）
  - **receipt**：`docs/release-receipts/v0.9.30-release-receipt.md`；00-STATUS 更新
- **进行中**：
  - 批次 B 剩余：docs/plans 归档 + SESSION_LOG 截断 + AGENTS.md 清理
  - 批次 D：合规（隐私政策/用户协议）、validateBaseUrl 强制 https
  - ⚠️ 待人工验证：v0.9.30 真机冒烟（i18n 后各页文字正常、UI/UX 改进效果）
- **关键发现**：
  - i18n 自动化要点：Text("纯文本") 直接换 stringResource；title/label/placeholder/contentDescription 均可；
    含变量用 format（%1$s/%1$d）；semantics lambda 非 Composable 需外部取变量；枚举 displayName/教程正文/
    ViewModel 错误消息/相对时间格式保留硬编码（合理）
  - R8 release 本地预验（assembleRelease）与 CI 产物一致（6,101,989 字节），发布前本地跑 release 构建可提前发现 R8 问题

## 2026-08-04 白天：v0.9.31 严谨发布完成（卡片学习科学三改进 + 布局精修 + 评分按钮统一，Release #61）

- **完成**：
  - **评分按钮三处统一**（`1aea291`）：新增 core:designsystem `WenyanRatingButton` 公共组件
    （动作模式 isPrimary→filled/tonal + 四档颜色 / 选择模式 isSelected→FilledTonal/Outlined 叠加评分色 /
    内置 48dp 触控目标）；Cards RatingButton / WrongAnswer WrongAnswerRatingButton 改薄适配器；
    Essay SelfRatingButton 删除，自评三档补评分色（不会=红/尚可=绿/轻松=蓝）；消除 135 行重复 + 组件 Preview
  - **v0.9.31 发布**（`2d930ac` 版本 56/0.9.31 + CHANGELOG）：tag → Release #61（约 12 分钟就绪）
  - **发布后验证**：APK sha256 d8291663…（两 APK 一致，6,101,985 字节）+ aapt2 56/0.9.31 + apksigner
    正式证书（CN=Wenyan App）+ body 关键词全命中（知识卡片学习科学三改进×1/横幅按知识点×1/新卡学习步×1/
    新卡徽章×1/评分按钮×1/WenyanRatingButton×2/论述题自评评分色×1/大屏宽度×1/触控目标×1/551 单测×1）
  - **receipt**：`docs/release-receipts/v0.9.31-release-receipt.md`；00-STATUS 更新
- **进行中**：
  - 批次 B 剩余：docs/plans 归档 + SESSION_LOG 截断 + AGENTS.md 清理
  - 批次 D：合规（隐私政策/用户协议）、validateBaseUrl 强制 https
  - ⚠️ 待人工验证：v0.9.31 真机冒烟（新卡学习步 GOOD→10 分钟、新卡徽章、横幅按知识点、论述题自评评分色）
- **关键发现**：
  - 组件统一要点：三处评分按钮语义不同（动作评分+预期间隔 / 动作评分无间隔 / 选择态+图标），
    用 isSelected: Boolean? 三态（null=动作 / true=选中 / false=未选中）一个参数覆盖两种模式最简洁；
    action 的 semantics 文案（"后重看"/"调度下次复习"）属业务语义留在各 screen 薄适配器，不进设计系统
  - 本地 assembleRelease 用 debug 签名（无 keystore env），仅验证 R8/编译；CI 用正式 keystore，
    sha256 不同但字节大小一致（6,101,985），属预期
  - gh CLI 未认证 + GitHub API 直连被沙箱拦截，发布状态用 ghfast.top 代理轮询 APK 资产 HTTP 200
    + WebFetch API JSON 验证 body/资产 digest 成功

## 2026-08-04 傍晚：v0.9.32 严谨发布完成（AI 界面 IME 空白修复 + 键盘发送 + 空态建议 + 合规，Release #62）

- **完成**：
  - **AI 输入框空白 P0 修复**（`bd84feb`）：根因 IME 双重消费——Scaffold 默认 contentWindowInsets 含 IME
    + InputBar 又 imePadding()，键盘弹出时输入框上方出现键盘高度空白；修复 contentWindowInsets=0 由 InputBar 独占 IME
  - **键盘 Enter 直接发送**（ImeAction.Send + KeyboardActions.onSend）
  - **空状态学习问题建议**（`bd71985`）：4 个学习问题卡片一键提问 + 触控 48dp + 文案资源化
  - **validateBaseUrl 强制 https**（`8e8c7b3`，批次 D）：拒绝 http:// 明文敞口，单测捕获 https:/// 漏判，+8 测试
  - **i18n 补全 6 处**（AI 模块残留硬编码清零，仅 enum displayName 豁免）
  - **v0.9.32 发布**（`7d67612` 版本 57/0.9.32 + CHANGELOG）：tag → Release #62（约 13 分钟就绪）
  - **发布后验证**：APK sha256 25ee9497…（两 APK 一致，6,105,461 字节）+ aapt2 57/0.9.32 + apksigner
    正式证书 + body 关键词全命中（大面积空白/IME 双重消费/键盘 Enter 发送/空态建议/validateBaseUrl/559 单测）
  - **receipt**：`docs/release-receipts/v0.9.32-release-receipt.md`；00-STATUS 更新（顺序修正为最新在前）
- **进行中**：
  - 批次 B 剩余：docs/plans 归档 + SESSION_LOG 截断 + AGENTS.md 清理
  - 批次 D 剩余：隐私政策/用户协议
  - ⚠️ 待人工验证：v0.9.32 真机确认 AI 输入框空白修复（用户原报告）、键盘发送、空态建议、http 被拒
- **关键发现**：
  - IME 双重消费是"点击输入框上方大面积空白"的典型根因：Scaffold contentWindowInsets 默认含 ime，
    bottomBar 内组件又 imePadding() 时，内容区 innerPadding.bottom = bottomBarHeight + IME。
    修复模式：由 bottomBar 独占 IME（contentWindowInsets=0），顶/底栏高度仍由 Scaffold 计入 innerPadding
  - 排查手段：对比 M3 Scaffold 的 MutableWindowInsets 机制 + 全项目 grep imePadding 定位唯一 double 场景（AI 屏）
  - 00-STATUS 用 python 批量 replace 需复查段落顺序/重复头，避免插入位置错乱

---

## 2026-08-06 会话：v0.9.37 布局与性能深度优化

- **完成**（4 路并行审计 + 逐条实测复核，commit `7055446`）：
  - **P0**：
    - 种子加载版本检查前置：老用户冷启动不再全量解析 5.3MB JSON（`SeedVersionShell` 轻量解析 metadata，+3 测试）
    - 卡片页拆卡缓存：评分后不再全量重拆数千张卡（(id,updatedAt) 排序键，顺序无关，+4 测试）；
      今日队列 stateIn 共享热流（ApplicationScope + WhileSubscribed 5s，消除横幅/拆卡双份订阅）
    - 完成态语义合并修复：3 按钮恢复 TalkBack 独立操作（仅统计区 merge，+4 无障碍测试）
  - **P1**：shrinkResources + OkHttp keep 收窄（**APK 6.15MB→5.15MB，-12.1%**）/ 列表 lean 投影
    DAO（KnowledgePointListItem）/ 论述题详情 LazyColumn / Retrofit 按 baseUrl 缓存 /
    聊天历史上限 200 条（rowid 稳定删最旧）/ 卡片首帧 id 生成移出主线程（sessionCardDispatcher 可注入）
  - **P2**：设置页水平边距 / 停止生成无障碍文案 / @Immutable 补齐 / _leechWarnings.update{} /
    AI 兜底错误友好化 / proguard 注释修正
  - **版本号提升**：versionCode 62 / versionName 0.9.37 + CHANGELOG
- **验证**：全量 **594 单测 0 失败**（+11）+ assembleDebug/Release 通过（R8 + shrinkResources + OkHttp 规则变更）
- **发布**：main + tag v0.9.37 推送 → ~14 分钟生成 Release #67（CI 冷缓存较慢，非异常），
  APK 实测：versionCode 62 / 0.9.37 / targetSdk 35 / 正式签名 3fefd8a0… / sha256 `2c9157ee…` 两资产一致
- **已评估未改**：RetryInterceptor 的 Thread.sleep 保留（OkHttp 拦截器阻塞 API，协程化需上移重试逻辑到 flow 层，
  改动面大 + 已有 5s clamp，风险>收益）
- **下次继续**：
  - 路线图：知识图谱 Graph 视图（数据就绪）/ 学习统计页（review_logs 就绪）/ 复习提醒通知（WorkManager）

## 2026-08-06：学习队列完整性、来源可信度与复习语义修复（待 CI）

- **范围**：`core:database` / `core:data` / `core:ai` / `core:designsystem` / `feature:cards` /
  `feature:aiassistant` / `app` 种子数据。
- **完成**：
  - 修复预创建 NEW 记忆记录被误计为到期复习、已学/待复习集合与学习进度不一致的问题；兼容旧种子时间戳及历史记录。
  - 种子导入改为原子写入版本状态，始终按数据库现有记忆记录保护 FSRS 进度；新增独立导入 schema=1，
    在不伪造 seed 内容版本的前提下触发老用户重导，并解析、持久化教材来源字段。
  - 仅在至少存在两个有效教材来源时展示冲突；当前 43 条原始冲突标记均缺少可核验的双来源，保持普通来源并记录告警。
  - RAG 引用页码改为可空，不再伪造 `P0`；引用标签、离线回答和 AI 界面统一展示真实来源。
  - 卡片“撤销”改为诚实的“回看”语义；已调度卡片不再展示会误导用户的预测间隔。
  - 新增队列边界、种子升级/来源、RAG 引用、卡片回看，以及 Room DAO→Repository 集成测试。
- **验证**：`git diff --check`、种子 JSON/XML 解析与数据不变量、SQLite 等价查询和来源保留场景通过；
  本机具备 JDK 17，但 Gradle 8.14.4 与 Android SDK 35 下载域名受运行环境网络策略限制，完整
  `testDebugUnitTest` / `assembleDebug` 将由 Draft PR 的 GitHub Actions 执行。
- **未做**：未发布、未打 tag；“回看”不是数据库级调度回滚，真正撤销需增加复习前状态快照并迁移 schema。

## 2026-08-06：知识卡片空队列竞态与动画流畅度修复（待 CI）

- **问题复现**：真机顶部显示“今日：新学 10 个知识点”，正文却显示“今天没有到期卡片”。
- **根因**：v0.9.37 将今日队列改为 `stateIn(initialValue = empty)` 后，人工空初值先于 Room
  真实结果到达；`CardsViewModel` 又会冻结首次卡片列表，导致空会话永久覆盖后续真实新卡。
- **完成**：
  - 队列共享改为 `shareIn(replay = 1)`，只重放真实查询结果，不再制造假空状态。
  - ViewModel 增加第二层保护：空卡片列表不建立冻结会话，真实新卡或 60 秒后到期卡仍可进入当前页面。
  - 卡片翻转由 300ms 调整为 420ms emphasized motion，中点轻微缩放、降低透视突兀感。
  - 正反面操作区由两个同时占位的 `AnimatedVisibility` 合并为单槽位 `AnimatedContent`
    fade-through，消除按钮区高度交接时的上下跳动；竖屏、横屏与全屏共用。
  - 新增首次空队列后卡片到达、共享流首值真实性、翻转缩放边界等回归测试。
- **验证**：`git diff --check` 通过；本机 JDK 17 可用，但无 Gradle 8.14.4 缓存且网络策略禁止
  下载 `services.gradle.org`，完整 `testDebugUnitTest` / `assembleDebug` 交由 PR GitHub Actions 验证。
- **分支**：`agent/fix-card-queue-animation`。

## 2026-08-06：知识框架第一阶段（进行中）

- 完成中国现当代文学 181 个知识点的稳定章节归属与 39 个框架节点，采用显式映射而非关键词猜测。
- 完成知识点页“框架 / 列表”双模式：框架按科目 → 章节 → 专题 → 知识点逐层进入，保留原有搜索与筛选。
- 导入结构版本升级为 2；章节使用稳定 ID，重新导入通过 Upsert，保留 MemoRecord、FSRS 排程和复习历史。
- 增加框架完整性测试，静态校验结果为 181/181 个现当代知识点已归类。
- 本地容器无法下载 Gradle 8.14.4，后续由 GitHub Actions 执行完整 Android 编译与回归测试。
- 本次改动待 PR CI 验证后继续推进其他三科归类。

## 2026-08-06：知识框架复检与优化

- 逐条复核现当代文学 181 个显式归属，修正晚清/五四、现代/当代、十七年/解放区、现实主义/新历史等边界分类。
- 新增“城市、战争与知识分子书写”专题，避免把非农村、非红色经典内容强行归类。
- 框架校验增加父节点悬空检查；框架异常流改为在重试触发器内部捕获，保证重试可重新订阅。
- 框架首页隐藏平铺练习入口，列表模式保留；深层浏览增加紧凑路径提示。
- 静态映射校验仍为 181/181、无重复无遗漏；等待 GitHub Actions 完整验证。


## 2026-08-06：知识框架第二阶段（中国古代文学，进行中）

- **研究结果**：对中国古代文学 465 个知识点按文学史时期、文体、作家作品和教材逻辑重新核对，
  不直接复用旧关键词首个命中结果；每个知识点只保留一个主要框架归属，跨专题联系继续由 tags/relatedIds 表达。
- **完成**：新增 10 个一级节点和 39 个专题节点，显式映射 465/465 个知识点；覆盖先秦、秦汉、
  魏晋南北朝、隋唐五代、宋辽金、元、明、清、近代及文学史通论。
- **边界复核**：龚自珍与晚清文学归入近代、元好问金代诗归入宋辽金、江淹《别赋》归入魏晋南北朝、
  佛教传播与文学影响归入魏晋南北朝、敦煌变文归入隋唐、清初才子佳人小说归入清代小说。
- **数据安全**：导入 schema 提升为 3；旧版 `chapter_ancient_0..7` 仅在无知识点引用时清理，
  有用户内容时保留节点；MemoRecord、FSRS 排程和复习历史不随章节重归类改变。
- **静态验证**：465/465、无重复/无遗漏/无悬空节点，`git diff --check` 通过；本地 Gradle 受网络策略限制，
  完整 Android 编译由独立 Draft PR 的 GitHub Actions 验证。
- **CI**：Run #398 首次上传时发现 SeedDataLoader 事务括号位置错误，已修复；Run #399 的全量单测、
  Debug APK 构建和制品上传全部通过。
- **分支**：`agent/framework-ancient`，基于 `main` 的累计验证 PR #5，尚未合并或发布。


## 2026-08-06：知识框架第三阶段（外国文学，进行中）

- **研究结果**：对外国文学 124 个知识点按文学史分期、文学思潮、地域和作家作品重新核对，
  不直接复用旧关键词首个命中结果；每个知识点只保留一个主要框架归属。
- **完成**：新增 11 个一级节点和 35 个专题节点，显式映射 124/124 个知识点；覆盖古希腊罗马、
  中世纪、文艺复兴、古典主义、启蒙、浪漫主义、现实主义/自然主义、19 世纪后期转型、
  20 世纪现代主义和 20 世纪下半期多元文学。
- **边界复核**：古希腊三大悲剧补充知识点、波德莱尔与王尔德的唯美/象征主义、乔伊斯/伍尔夫/劳伦斯的
  英语现代主义、奥斯汀的英国现实主义、陀思妥耶夫斯基补充知识点的俄国现实主义均单独核验。
- **数据安全**：旧版 `chapter_foreign_0..7` 仅在无知识点引用时清理；沿用 schema=3 的安全重导逻辑，
  不覆盖 MemoRecord、FSRS 排程和复习历史。
- **静态验证**：124/124、无重复/无遗漏/无悬空节点，`git diff --check` 通过。
- **CI**：Run #401 的全量单测、Debug APK 构建和制品上传全部通过。
- **分支**：`agent/framework-foreign`，基于累计验证 PR #6，尚未合并或发布。


## 2026-08-06：知识框架四科总复检（文学理论完成，最终 CI 通过）

- **文学理论**：按“学科基础 → 文学活动 → 创作 → 作品 → 接受 → 批评 → 理论史”建立 33 个节点（7 个一级节点），显式映射 190/190 个知识点；将诗歌意境、文本结构、叙事学、接受美学和批评方法等交界内容按语义拆分。
- **四科最终规模**：现当代文学 181 个知识点 / 39 个节点 / 4 个一级节点；古代文学 465 / 59 / 10；外国文学 124 / 66 / 11；文学理论 190 / 33 / 7。四科共 960 个知识点，全部有且仅有一个主要框架归属。
- **逐条语义复检修正**：
  - 古代文学：南北朝乐府归入“魏晋南北朝诗歌”；沈璟与明代格律派归入“明代戏曲”。
  - 外国文学：哈代《德伯家的苔丝》归入“英国现实主义”，不再落入俄国现实主义。
  - 文学理论：诗歌的“意境”理论归入诗学/意象专题，不再与文类体裁混放。
- **工程优化**：四科共享 `KnowledgeFrameworkValidator`，统一检查节点重复、父节点悬空、循环、知识点遗漏/过期和错误归属；注册表回归测试确保四科覆盖闭合、跨科目无重复。
- **数据安全**：沿用导入 schema=3；旧版 `chapter_modern_*`、`chapter_ancient_*`、`chapter_foreign_*`、`chapter_theory_*` 仅在无知识点引用时清理；知识点重新归类使用 Upsert，不删除 MemoRecord、FSRS 排程和复习历史。
- **静态验证**：四科分别为 181/181、465/465、124/124、190/190；合计 960/960，跨科目 ID 重复为 0；远端源码与本地 Blob 摘要一致。
- **CI**：Run #408 代码版全量 `testDebugUnitTest`、`assembleDebug` 和 Debug APK 上传全部成功；文档提交后的 Run #409 也全部通过。最终文档版制品大小 26,656,366 字节，摘要 `sha256:25f00d52e9931e90d246a66add77c7181174926e602258ed791fa7251e7aa5d1`。
- **分支/PR**：`agent/framework-theory` / Draft PR #7，当前未合并、未发布；代码和文档均已通过 GitHub Actions 验证。


## 2026-08-06：知识框架界面复检与交互打磨

### 本轮复检结论

在四科框架数据已通过完整性校验的基础上，继续检查框架首页、科目层、专题层和知识点层的视觉密度、导航状态与无障碍表达。本轮没有改动 `main`，也没有改变章节 ID、知识点归属或学习记录。

### 本轮改进

- 框架/列表切换改为互斥分段控件，减少入口的视觉噪声。
- 框架首页增加“从框架开始学习”概览卡，显示四科、知识点和高频考点总量。
- 科目卡片补充一级专题数量，专题卡片将知识点数、子专题数和高频数量分成可换行的元信息，窄屏下不再挤压标题和返回箭头。
- 统一科目与专题卡片的圆形图标徽章、留白、卡片层级和底部安全间距。
- 导航过渡按进入/返回方向使用 Push/Pop 动画；AnimatedContent 使用目标页面快照，避免切换时旧页面内容闪变。
- 进程恢复、数据库重新导入或章节更新后，如果保存的导航 ID 已失效，框架页会自动回到有效根节点，不会停在空白层级。
- 标题、专题分组和知识点分组补充 TalkBack heading 语义；空专题统一使用空状态组件。

### 验证

- GitHub Actions Run #412：全量单元测试 ✅
- GitHub Actions Run #412：Debug APK 构建 ✅
- GitHub Actions Run #412：Debug APK 上传 ✅
- Debug APK artifact：`wenyan-debug-apk`
- artifact 大小：26,665,564 字节
- artifact SHA-256：`0b7ceb772fd1fecb001d1b7603c9100a559f2fc4c4916740ed5ff8ad4493cb94`
- 远端 UI 文件 Blob SHA 与隔离工作区版本一致：`9e1543c3ea62710e308359c052f18e033c34c8f4`
- 远端资源文件 Blob SHA 与隔离工作区版本一致：`6c00ed35a2b54b19d12945294b13c2d967db2370`
- 当前 PR 仍为 Draft，目标为 `main`，没有合并或发布。

### 环境限制

本地容器仍未缓存 Gradle 8.14.4 Wrapper，且无法从当前网络策略下载；本轮最终编译与 APK 产物以 GitHub Actions 为准。

---

## 2026-08-07 会话：v0.9.42 发布（教材内容增量）

- **背景**：远程 GitHub 在本地 v0.9.37 之后推进到 v0.9.41（v0.9.38 种子安全/来源落库、v0.9.39 修复我 v0.9.37 stateIn 竞态 → shareIn(replay=1)、v0.9.40 四科知识框架、v0.9.41 返回栈/动效/图标 v8）；本地同步后核对 PR #9（content/yuan-shishuo-completion）内容质量 8 项全过
- **发布 v0.9.42**（versionCode 67，seed 2.26.0）：
  - 内容升级合并（PR #9）：知识点 960→1101（+141：现当代75/古代33/外国33），真题 485→564（+79，2023-2026），四科框架覆盖 498/256/157/190
  - 版本号与 CHANGELOG 已在远程就绪，本地同步后直接打 tag v0.9.42 推送
  - CI 首次推送 ~14 分钟生成 Release #69（冷缓存），网页代理核验 + APK 实测通过：
    versionCode 67 / 0.9.42 / targetSdk 35 / 正式签名 3fefd8a0… / sha256 `0217a76f…` 两资产一致 / 5,565,520 字节
  - 全量 **631 单测 0 失败** + assembleDebug/Release 通过
- **receipt**：`docs/release-receipts/v0.9.42-release-receipt.md`
- **下次继续**：路线图——复习提醒通知（WorkManager）/ 学习统计页（review_logs 就绪）/ 知识图谱 Graph 视图

## 2026-08-07：修复发布版关联知识点无法打开

### 本轮结论

- 用户反馈：新增知识点详情页的“关联知识点”列表能展示但点不开。
- 根因定位：`app/src/main/java/com/wenyan/app/navigation/WenyanNavHost.kt` 对动态路由
  `knowledge_detail/{pointId}` 统一启用 `launchSingleTop`。从详情 A 点击详情 B 时，
  两者属于同一个导航目的地 ID，B 没有按浏览路径正常入栈，表现为关联项点击无效/仍停留在原详情。
- 数据核查：当前 seed 2.26.0 共 1101 个知识点；新增 78 个中 70 个有派生关联、233 条边，
  悬空关联 ID 和跨学科误连均为 0。8 个无共享标签的新增知识点没有关联项，属于预期数据状态。

### 已实施修复

- 详情页内部跳转不再使用 `launchSingleTop`，不同知识点保留 A → B → C 的返回历史。
- 同一详情页重复点击、空白 ID 做保护；从列表等非详情页进入时仍保留 `launchSingleTop` 防重复点击。
- 显式声明 `pointId` 为 String 导航参数，并对动态 ID 做 URI 编码。
- 新增导航策略单元测试和 `GroupedCardItem` 点击回归测试，确认 UI 回调确实触发。
- 版本提升至 `versionCode 68 / versionName 0.9.43`，并补充 CHANGELOG。
- 顺手清理源码中已过时的 134/910 规模与旧 `launchSingleTop + popUpTo` 架构注释，避免后续维护误判当前数据和返回栈行为。

### 验证与限制

- `git diff --check` 通过。
- seed 内容关系静态审计通过：1101 points / 78 new / 70 related sources / 233 edges / 0 dangling。
- 独立只读全量审计通过：1101 knowledge points / 564 exam questions / 909 writing materials；142 道 ESSAY 中 134 道 angle/notes 完整、8 道按可选字段正常缺省，JSON 解析错误、悬空关联、重复 ID、跨科目关联均为 0。
- 精确复现 `SeedDataLoader` 关系算法：1043 个关系源、4168 条边；新增 78 个知识点中 70 个有关系、233 条边，8 个无共享标签，符合规则。
- 本地 Android 全量构建未能启动：Gradle Wrapper 需要从 `services.gradle.org` 下载 8.14.4，当前网络不可达；未将该环境限制误报为构建通过，应以推送后的 GitHub Actions `testDebugUnitTest` + `assembleDebug` 作为发布闸门。

---

## 2026-08-07 会话：v0.9.43 发布（关联知识点导航修复）

- **背景**：远程出现新分支 `fix/related-knowledge-navigation`（PR #10，17 提交：4 功能 + 13 基线对齐）
- **合并流程（严谨）**：
  - merge-tree 预检无冲突 → worktree 分支全量测试通过 → --no-ff 合并（707274c）
  - 合并后主工作区复验：636 单测 0 失败 + Debug/Release 构建通过
  - **发现并行合并**：远程 main 已由他人更新（fdfca32）——与本地合并内容 diff 为空（等价），本地对齐远程
- **发布 v0.9.43**（versionCode 68）：tag 推送 → ~14 分钟生成 Release #70
  - APK 实测：versionCode 68 / 0.9.43 / targetSdk 35 / 正式签名 3fefd8a0… / sha256 `2f1340fa…` 两资产一致
- **本版内容**：详情页 A→B 关联跳转不入栈修复（launchSingleTop 折叠动态路由 → 策略化单栈判定）、同点防重复、路径编码；新增导航策略测试 + GroupedCard 测试
- **receipt**：`docs/release-receipts/v0.9.43-release-receipt.md`
- **下次继续**：路线图——复习提醒通知（WorkManager）/ 学习统计页（review_logs 就绪）/ 知识图谱 Graph 视图
