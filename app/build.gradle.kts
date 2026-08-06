// 文研App 主应用模块构建文件
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.wenyan.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wenyan.app"
        minSdk = 26
        targetSdk = 35
        // P0-M2 修正：versionCode 必须每次发版递增，否则包安装器无法区分版本。
        // v0.1.0=1, v0.2.0=2, v0.3.0=3, v0.4.0=4, v0.5.0=5, v0.6.0=6, v0.7.0=7, v0.7.1=8, v0.7.2=9（修复图谱FK回滚）, v0.7.3=10（481真题答案+卡片镜像+图谱扩充）, v0.7.4=11（481真题范文+卡片内容增强+多维视角解析+考频派生）, v0.7.5=12（610综合卷127题科目重新分类+UI修复+图谱重写）, v0.7.6=13（数据瘦身+图谱时间轴布局）, v0.8.1=14（图谱三模式重构+形状编码+覆盖率100%）, v0.8.2=15（修复图谱闪退：lanes越界+IN子句超限）, v0.8.3=16（全面UI/UX打磨：设计系统修复+输入校验+无障碍+性能优化）, v0.8.4=17（第二轮深度打磨：AMOLED补全+无障碍语义+动画优化+死代码清理+错误反馈）, v0.8.5=18（知识卡片功能深度修复：FSRS调度粒度sibling去重+会话内cards冻结+isFinished状态正确传递到UI+撤销功能+会话统计+评分按钮颜色编码+keyPoints切分修复）, v0.8.11=19（知识卡片功能深度打磨：CardSplitter 6维度限制修复+SiblingRatedHint不隐藏评分按钮+进程恢复统计重置+sibling卡预览误导修复+rateCard异步失败分离+评分按钮颜色对齐Anki+sibling卡字段去冗余+Leech警告跳转修复+无pointId错题记录+会话统计持久化+3处编译错误修复，280 tests全绿）, v0.8.12=20（知识卡片第二轮深度打磨：undo不回退ratedPointIds避免FSRS重复调度+recordStudySession移入if避免部分写入+翻转滚动架构修复+SiblingRatedHint去术语化+Leech警告新增问AI助手按钮+Leech检测改为新增判定+errorMessage优先级+翻转动画对齐WenyanMotion+Leech警告队列化+retry清除错误状态+完成态文案修复+EASY视觉权重降级+SchoolComparison尾部分割线+未翻转也显示UndoButton，327 tests全绿）, v0.8.13=21（stark UI 审计修复：ApiConfig ErrorState 错误状态覆盖+ViewModel retry 触发+GraphScreen 3 处 Surface(onClick) 加 role=Button 语义+AiAssistantScreen RoundedCornerShape(4dp) 改 MaterialTheme.shapes.small+GraphScreen 修复 deprecated MenuBook→AutoMirrored，361 tests 全绿）, v0.8.14=22（P0-3b 紧急修复：catch 位置错误导致 retry() 永久失效+ApiConfigScreen Crossfade 改用 Triple 处理 error 状态，361 tests 全绿）, v0.8.16=23→24（AI 审计+图谱性能优化，443 tests）, v0.8.17=25（staff-engineer-mode 三功能审计：知识点+错题本+知识卡片 retry-after-error Blocker 修复+错误处理一致性+5 项 Must-Fix+9 新测试，455 tests 全绿）, v0.8.18=26（启动图标 v3 印章文重构+Logging.kt 统一日志门面+scripts/setup-env.sh 一键环境+mise.toml 锁定工具链）, v0.9.0=27（知识图谱移除+章节树数据层+关联知识点模块增强+错题本升级为顶级Tab+ProGuard修复，403 tests 全绿）, v0.9.1=28（关联知识点模块不渲染修复：SeedDataLoader 新增 computeRelatedIdsByTags 派生 relatedIds，seed 2.12.0→2.13.0，+8 测试）, v0.9.4=29（错题本接入 FSRS 间隔重复调度：5 层实现 Migration 7→8 + 10 sched_* 字段 + WrongAnswerSchedulingMapper + SchedulingRepository.rateWrongAnswer + DUE 过滤 + 四档评分 UI + ClockGuard 注入 + interval 下界保护，+10 测试，403 tests 全绿）, v0.9.5=30（关于与教程子路由：设置页"关于"分组新增"关于与教程"入口，注册 ROUTE_ABOUT 子路由 Push/Pop slide + launchSingleTop，加载 AboutTutorialScreen 7 章深度教程 430 行：定位/模块/FSRS-6/三档记忆/RAG/使用指南/致谢。Icons.AutoMirrored.Filled.MenuBook 弃用修复。agent-pr-review ✅ Ready to merge，PRR ✅ READY TO RELEASE，RBR ✅ PASS）
        // v0.9.5=30（关于与教程子路由：设置页"关于"分组新增"关于与教程"入口，注册 ROUTE_ABOUT 子路由 Push/Pop slide + launchSingleTop，加载 AboutTutorialScreen 7 章深度教程 430 行：定位/模块/FSRS-6/三档记忆/RAG/使用指南/致谢。Icons.AutoMirrored.Filled.MenuBook 弃用修复。agent-pr-review ✅ Ready to merge，PRR ✅ READY TO RELEASE，RBR ✅ PASS）
        // v0.9.6=31（关于与教程精简重构 + 代码卫生审计：AboutTutorialScreen 7 章→5 节可折叠 + 4 项代码卫生修复。PRR ✅ READY TO RELEASE，RBR ✅ PASS）
        // v0.9.7=32（知识卡片功能完善 + 界面审查修复：B1 sibling去重FSRS调度漏洞 + B2 Leech误报(RELEARNING状态) + B3 无pointId卡日志 + M2 sibling卡打散 + M4 翻转滚动重置 + M5 完成态撤销 + M9 无效cardType日志 + M11 预览闪烁 + M10 @Preview + 2 测试。PRR ✅ READY TO RELEASE，RBR ✅ PASS）
        // v0.9.8=33（论述题板块：知识点详情页"相关论述题"区块 + 论述题详情页 10 区块结构（题目/审题/论证/框架/依据/交叉验证/参考链接/知识盲点/关联知识点）+ JSON 解析优雅降级 + 双向导航 + seed 2.13.1→2.14.0 + 3 道示例题 angle/notes 完整填充 + 131 道派生 relatedPointIds + 47 新测试（Repository/ViewModel/Models）。452 tests 全绿）
        // v0.9.9=34（论述题 AI 审题助手 Phase 3：EssayDetailViewModel 接入 SocraticTutor 三阶段引导（ANALYZE/SUGGEST/SHOW_SAMPLE）+ 自评错题回写（AGAIN→SOURCE_ESSAY_PRACTICE+FSRS调度）+ SocraticTutor 接口提取（便于测试替换）+ FakeSocraticTutor/FakeSchedulingRepository + 10 新测试（AI引导/防重入/clearAiGuides/自评AGAIN/GOOD/异常）+ SOURCE_ESSAY_PRACTICE 常量新增 + WrongAnswerScreen/KnowledgePointDetailScreen UI 标签 + Icons.AutoMirrored.Filled.Send 弃用修复。471 tests 全绿）
        // v0.9.10=35（知识点库完整化 + 全面内容审计：seed 2.15.0→2.16.0 补充论述题 knowledgeGaps 标注缺失的 25 个核心知识点（古代4/现当代8/外国6/文论7，对齐袁行霈/钱理群/朱维之/童庆炳四教材），910→935 知识点。全面审计 935 知识点 + 134 论述题：0 个内容问题（字段完整/ID唯一/subject合法/argumentPath 全有 thesis+≥3论点/722 条 evidence 全有 source）。修复 EssayDetailScreen subtitle score=0 时显示"0分"问题。469 tests 全绿）
        // v0.9.11=36（检查更新功能：设置页"检查更新"入口 + UpdateRepository 5 层架构 + UpdateCheckScreen M3 风格各状态组件 + 导航注册 + Hilt 绑定。480 tests 全绿）
        // v0.9.12=37（修复检查更新 NetworkOnMainThreadException + 新增 api.github.com
        //   访问失败时降级到 github.com 重定向备用方案 + 超时 10s→8s 提速。
        //   本地验证：assembleDebug + testDebugUnitTest 全绿）
        // v0.9.13=38（沉浸式底部导航栏：COMPACT 模式 Box 叠加布局 + NavigationBar 透明 +
        //   BottomGradientScrim 120dp 渐变遮罩 + contentWindowInsets 仅状态栏顶+IME 底。
        //   5 文件修改：WenyanAdaptiveNavigation/WenyanNavigationBar/ExpressiveScaffold +
        //   gradle 依赖。480 tests 全绿）
        // v0.9.14=39（修复底栏遮盖 + 软件内 APK 下载+安装：
        //   - 修复：COMPACT 布局改用 Box+显式 padding，不再依赖 Scaffold contentWindowInsets
        //   - 新增：UpdateViewModel OkHttp 下载 APK + FileProvider 安装
        //   - 新增：AndroidManifest REQUEST_INSTALL_PACKAGES + FileProvider
        //   - 新增：file_paths.xml + OkHttp 依赖
        //   - UI 更新：UpdateCheckScreen 新增 Downloading/DownloadComplete 状态
        //   assembleDebug + 全模块 testDebugUnitTest 全绿）
        // v0.9.15=40（修复子页面底部大块色块：
        //   - 修复：COMPACT 布局 showNavigation=false 时不再添加 surfaceContainer 背景和底部 padding
        //   - 子页面全屏内容，由各自的 ExpressiveScaffold 处理背景和系统 insets
        //   - 编译：assembleDebug + 全模块 testDebugUnitTest 全绿（317 tasks, 0 failures））
        // v0.9.16=41（真题→论述题迁移：底部导航第 2 个 Tab 从"真题"替换为"论述题"，
        //   - TopLevelDestination: ROUTE_QUIZ → ROUTE_ESSAY，Quiz data object → Essay data object
        //   - WenyanNavHost: quizDestination → essayTabDestination，EssayListScreen 作为顶级 Tab
        //   - KnowledgeScreen: 移除 EssayEntryCard + onNavigateToEssays 参数
        //   - EssayListScreen: onBack 改为 nullable（顶级 Tab 模式无返回箭头）
        //   - AboutTutorialScreen: 真题→论述题描述更新 + ErrorOutline 导入修复
        //   - 死代码审查：无残留 ROUTE_QUIZ/quizDestination/onNavigateToEssays/EssayEntryCard/ROUTE_ESSAY_LIST
        //   - 编译：需本地 emulator 验证 assembleDebug + testDebugUnitTest）
        // v0.9.17=42（题号前缀剥离：创建 ExamContentCleaner 集中清洗工具，
        //   - 剥离所有题目内容中的阿拉伯数字前缀（"1. " "2. "）和中文数字前缀（"一、" "二、"），
        //   - 包括试卷标题（"三、论述题" → "论述题"），
        //   - 6 个 UI 展示点统一清洗：EssayListViewModel（列表预览）、EssayDetailScreen（详情正文）、
        //     KnowledgePointDetailScreen（知识点关联预览）、QuizScreen（真题练习）、
        //     WrongAnswerScreen（错题本标题）、EssayDetailViewModel（AI 审题助手输入），
        //   - 不修改 seed_data.json，仅运行时清洗，
        //   - 编译：需本地 emulator 验证 assembleDebug + testDebugUnitTest）
        // v0.9.19：紧凑玻璃导航栏 + 种子加载超时重试
        // v0.9.20=45（KSU 风格滚动感知导航栏：LocalScrollState + WenyanNavigationBar scroll-aware + 5 Screen 提供 LazyListState。versionName "0.9.20"）
        // v0.9.21=46（MD3 规范底栏回归 + inset 双重消费修复：surfaceContainer 实色 + 80dp + secondaryContainer 指示器；
        //   底栏吃手势条 inset、移除顶栏双倍 padding，消除布局空白。versionName "0.9.21"）
        // v0.9.22=47（深度审查改进批 A+B：底栏 double inset 修复 / 主题保存错误提示 /
        //   FSRS stability=0 防御 / MIGRATION_7_8 补复合索引 / recordWrongAnswer 事务+时间源 / 评分防重入。versionName "0.9.22"）
        // v0.9.23=48（论述题删年份 / Snackbar 常驻修复 / AI 审计修复：URL 拼接+竞态+RAG 降级+注入封堵 /
        //   更新日志机制 CHANGELOG。versionName "0.9.23"）
        // v0.9.24=49（批一 AI 体验：流式+停止+多轮上下文+token 统计 / 批二工程质量：R8 混淆+迁移测试+
        //   Tab 闪烁+补索引(DB 9→10)+事务化。versionName "0.9.24"）
        // v0.9.25=50（AI 生成全新启动图标「书堆+文」 / 整体界面审查修复 14 项：AI 停止保留内容+
        //   流式自动滚动+状态栏图标色+更新安装已下载 APK+重试 loading+错误态禁用筛选+
        //   长标题截断+种子色暗色适配+卡片滚动重置+错题本 Snackbar+日期行省略+底栏跨 Tab 重置。
        //   versionName "0.9.25"）
        // v0.9.26=51（新启动图标 v7.4 Google Play Books 风格黑底白书 / 批三：详情页懒加载+
        //   RAG VERIFIED 过滤+AI 成本控制(Retry-After+callTimeout+Semaphore)+i18n 资源化+
        //   convention plugin 抽取。versionName "0.9.26"）
        // v0.9.27=52（启动图标 v7.5 精进：双色页+页脚厚度+排版缩进+主题图标 evenOdd 镂空 /
        //   全面检查 P1-1/2：aiJob 竞态条件清空 + Retry-After 上限 5s /
        //   内容补齐 25 个知识点：真题硬缺口 10+教材缺口 10+台港澳 4+敦煌变文，
        //   seed 2.16.0→2.18.0，935→960。versionName "0.9.27"）
        // v0.9.28=53（P1 hotfix：App 内更新下载失败——降级路径 assets 为空时旧逻辑 fallback
        //   到 release tag 页面 HTML，App 下载网页当 APK 导致"应用文件存在问题"；修复为按
        //   release.yml 命名规则构造真实 APK URL + 下载完整性校验(Content-Length/sha256) +
        //   失败重试。P2：卡片拆分 MIN_STRUCTURED_DIMENSIONS=3 消除 35 个只拆 1 张超长卡。
        //   versionName "0.9.28"）
        // v0.9.29=54（卡片备考系统：每日新卡限额默认 60 可设 + 考频筛选 + 科目筛选 + 考试倒计时；
        //   今日任务横幅 + 设置页配置。versionName "0.9.29"）
        // v0.9.30=55（知识卡片打磨：复习/新卡比例保护 + 今日任务显示优化 / 批次 C UI/UX 14 项：
        //   AI 光标动画/触控目标 48dp/布局稳定/空 item 条件化等 / 仓库卫生部分 /
        //   i18n 全量补全约 130 资源（5 模块 UI 文本，剩余硬编码 0）。versionName "0.9.30"）
        // v0.9.31=56（知识卡片学习科学三改进：横幅按知识点显示 / 新卡 GOOD 进 10 分钟学习步 /
        //   新卡徽章标识 / 整体布局精修（大屏宽度/触控目标/间距呼吸感）/
        //   评分按钮三处重复统一为设计系统公共组件 WenyanRatingButton（Essay 自评补评分色）。
        //   versionName "0.9.31"）
        // v0.9.32=57（AI 界面修复 + 功能完善：输入框上方大面积空白——IME 双重消费修复
        //   （Scaffold contentWindowInsets 含 IME + InputBar imePadding 叠加，由 InputBar 独占 IME）/
        //   键盘 Enter 直接发送 / 空状态学习问题建议一键提问 / i18n 补全 6 处 /
        //   批次 D：validateBaseUrl 强制 https 拒绝明文敞口。versionName "0.9.32"）
        // v0.9.33=58（真题背题专项：知识点页新增"真题背题"入口卡——名词解释/简答
        //   背诵模式（显示答案/会了/不会进错题本走 FSRS），列表页题型+科目+年份三维
        //   筛选，DAO observeByQuestionTypes 多题型 IN 查询稳定排序，数据层排除 ESSAY
        //   避免与论述题 Tab 重复；Snackbar withTimeout 防挂起对齐 v0.9.23/25 模式。
        //   全量 559 测试通过。versionName "0.9.33"）
        // v0.9.34=59（全局横屏适配：知识卡片复习布局横屏改双栏——左卡片区占全部
        //   高度+大部分宽度（突出、大、方便阅读），右侧 200dp 窄操作面板 2×2 评分
        //   网格（按钮更小更省高度）；AdaptiveWindowLayout helper（shouldUseDualPane
        //   判据 maxWidth>maxHeight && maxWidth>=600dp，精确捕获横屏手机不误触平板
        //   竖屏）；列表类 Screen 搜索/筛选栏横屏限宽居中；TodayPlanBanner 横屏
        //   compact 单行释放 ~70dp 给卡片；完成态/背题详情操作栏限宽；
        //   全量 569 测试通过（+10 横屏判定测试）。versionName "0.9.34"）
        // v0.9.35=60（横屏知识卡片协调优化：卡片限宽 480dp 居中 + 操作面板垂直居中 +
        //   横屏协调性回归测试 5 个；全面质量审计修复 18 项：双断点不一致（MEDIUM 窗口
        //   双栏激活）、窄横屏顶栏降级、markDontKnow 连点竞态、新卡排序方向、考试日期
        //   倒计时联动、AI 幽灵回复代次防护、token 预算截断、错题调度清理等；
        //   全量 574 测试通过。versionName "0.9.35"）
        // v0.9.36=61（知识卡片全屏沉浸模式：卡片页顶栏全屏按钮（有卡可复习时显示）进入
        //   全屏页——ImmersiveSystemBars 隐藏状态栏+导航栏（项目首个沉浸式先例，滑动边缘
        //   临时唤出自动隐藏）；无顶栏零 insets Scaffold 内容占满全屏；左上角半透明圆形
        //   浮动退出按钮；共享卡片页 CardsViewModel（hiltViewModel getBackStackEntry）
        //   保持同一复习会话；横屏双栏变体：卡片放宽 560dp + 右操作栏 280dp 单列竖排
        //   评分按钮（用户"一个个竖着排列"偏好）；竖屏放宽最大宽度上限；
        //   Leech 警告/加入错题本/Snackbar 全镜像卡片页；全量 583 测试通过
        //   （+4 全屏横屏布局测试）。versionName "0.9.36"）
        // v0.9.37=62（布局与性能深度优化：P0-1 种子加载版本检查前置（老用户冷启动
        //   不再全量解析 5.3MB JSON）；P0-2 卡片页拆卡缓存（评分后不再全量重拆数千张卡）
        //   + 今日队列 stateIn 共享热流（消除双份订阅）+ 拆卡移出主线程；P0-3 完成态
        //   语义合并修复（3 个按钮恢复 TalkBack 独立操作）；P1-1 shrinkResources 开启
        //   （APK 5.87MB→5.15MB，-12.1%）；P1-2 列表 lean 投影 DAO（列表流不再加载
        //   full_content/study_text 大文本列）；P1-4 论述题详情改 LazyColumn（懒加载）；
        //   P1-6 Retrofit 按 baseUrl 缓存；P1-7 聊天历史保留上限（200 条/会话）；
        //   P1-9 卡片首帧 id 生成移出主线程；P1-10 OkHttp keep 规则收窄；P2 系列 6 项
        //   （设置页边距/停止按钮无障碍/@Immutable 补齐/update{} 原子/友好错误/proguard 注释）；
        //   全量 594 测试通过（+11：种子轻量解析 3 + 完成态无障碍 4 + 缓存键 4）。
        //   versionName "0.9.37"）
        // v0.9.38=63（学习完整性与来源可信度修复：未学习 NEW 记录不再误入到期队列或
        //   学习进度；种子导入版本原子记录并保留 FSRS 状态；来源元数据真实落库，移除
        //   伪 P0 页码与无证据冲突展示；“撤销”更名“回看”并隐藏无效间隔预览；新增
        //   DAO→Repository 集成和回归测试。GitHub Actions 单测、assembleDebug 通过。）
        // v0.9.39=64（知识卡片空队列竞态修复：v0.9.37 stateIn 空初值被 CardsViewModel 冻结为会话
        //   →"横幅有新卡、正文无卡片"；改 shareIn(replay=1) 只重放真实查询 + VM 空列表不冻结会话双保险。
        //   翻转 300ms→420ms emphasized + 正反面操作区 AnimatedContent fade-through 替代双 AnimatedVisibility
        //   消除高度跳动。合并 PR #3，CI 3 次全绿。）
        // v0.9.40=65（四科知识框架整理：现当代、古代、外国、文学理论共 960 个知识点的教材式框架浏览、稳定归类与导入校验；合并 PR #7。）
        versionCode = 65
        // P1-M1 修正：versionName 与实际版本对齐（原 "0.1.0" 误标三版未更新）
        // v0.9.19：紧凑玻璃风格导航栏 + 种子加载 300s+重试机制
        //   数据一致性修复（B1/B2/B3）：
        //     - B1: sibling 去重 FSRS 调度漏洞 — templateType 解析提前,无效 cardType 不污染 ratedPointIds
        //     - B2: Leech 误报修复 — RELEARNING+AGAIN 时 failCount 不变,oldFailCount 反推根据 state 区分
        //     - B3: 无 pointId 卡评分加 Timber 警告日志,便于生产排查
        //   体验优化（M2/M4/M5/M9/M11）：
        //     - M2: sibling 卡打散(interleaveSiblingCards),避免连续 5-6 张同知识点
        //     - M4: 翻转时重置滚动位置,避免背面继承正面滚动状态
        //     - M5: 完成态新增"撤销最后一张"按钮,评错可回退
        //     - M9: 无效 cardTypeStr 加 Timber 警告(原静默失败)
        //     - M11: collectLatest 进入时立即清空预览,避免快速切卡时旧预览闪烁
        //   UI 工程化（M10）：
        //     - CardsScreen 添加 3 个 @Preview(Normal/Empty/Finished),便于 IDE 实时查看
        //   测试（+2）：
        //     - B2 修复 RELEARNING 状态 AGAIN 评分 failCount 不变时不误报 Leech
        //     - B2 对照组 REVIEW 状态 AGAIN 评分 failCount 跨阈值时正确弹 Leech
        //   本地验证：:app:assembleDebug + testDebugUnitTest 全绿
        // v0.9.8：论述题板块（响应用户需求"增加论述题板块融合在知识点板块"）。
        //   深度调研：795 行调研报告 + 44 可点击来源，覆盖南师大命题特征/导师方向/
        //     现当代文学知识网络/文学研究引用规范/六类论述题答题方法论
        //   数据层（Phase 0，已 commit b07da8a）：
        //     - ExamQuestionDao.observeAllEssays()：内存过滤避免 SQL LIKE 误匹配 JSON 子串
        //     - KnowledgeRepository: observeRelatedEssays / observeEssayById / getKnowledgePointsByIds
        //     - SeedDataLoader: computeExamQuestionRelatedPoints（title 权重 2 / tag 权重 1 派生）
        //     - seed 2.13.1→2.14.0 + 3 道示例题（eq_0038/eq_0182/eq_0254）angle/notes 完整填充
        //   UI 层（Phase 1）：
        //     - KnowledgePointDetailScreen: 新增 RelatedEssaysSection（知识点→论述题入口）
        //     - EssayDetailScreen: 10 区块结构（题目/审题/论证/框架/依据/交叉验证/参考链接/知识盲点/关联知识点）
        //     - EssayDetailViewModel: JSON 解析 + 关联知识点聚合（relatedPointIds + evidences.linkedKnowledgePointId 合并去重）
        //     - EssayDetailModels: kotlinx.serialization 解析 angle/notes JSON，优雅降级（解析失败返回 null）
        //     - WenyanNavHost: ROUTE_ESSAY_DETAIL 子路由，Push/Pop slide，双向导航（知识点↔论述题）
        //     - feature:knowledge/build.gradle.kts: 引入 kotlin.serialization 插件 + kotlinx-serialization-json 依赖
        //   测试（+47）：
        //     - KnowledgeRepositoryTest: observeRelatedEssays（4 测试，含 SQL LIKE 误匹配规避）+ observeEssayById（2）+ getKnowledgePointsByIds（4，含去重/顺序/过滤）
        //     - KnowledgePointDetailViewModelTest: relatedEssays 状态（5 测试，含 Flow 自动刷新）
        //     - EssayDetailViewModelTest: 新建（15 测试，含 JSON 优雅降级/关联知识点聚合/retry）
        //     - EssayDetailModelsTest: 新建（16 测试，覆盖 parseEssayAngle/parseEssayNotes 全分支）
        //   本地验证：:app:assembleDebug + 全模块 testDebugUnitTest 全绿（452 tests, 0 failures）
        // v0.9.9：论述题 AI 审题助手 Phase 3（响应用户需求"进入 phase3，严谨仔细，反复检查"）
        //   Phase 3.1-3.3 数据/逻辑/UI 层：
        //     - EssayDetailViewModel 接入 SocraticTutor（三阶段引导 ANALYZE/SUGGEST/SHOW_SAMPLE）
        //     - 自评错题回写（AGAIN → recordWrongAnswer + rateWrongAnswer FSRS 调度）
        //     - SocraticTutor 接口提取（SocraticTutorImpl 生产实现，便于测试替换）
        //     - EssayAiGuideSection UI（答题入口/答题输入/三阶段引导卡片/自评三档按钮）
        //   Phase 3.4 测试（+10）：
        //     - FakeSocraticTutor + FakeSchedulingRepository 新增
        //     - startAnswering/cancelAnswering/updateUserAnswer 截断
        //     - submitAnswerAndGuide 空答案保护/正常三阶段/异常/防重入
        //     - clearAiGuides 清空引导保留答案
        //     - rateSelf AGAIN 写错题本+FSRS调度 / GOOD 不写 / 异常仍设置 selfRating
        //   Phase 3.5 静态审查修复（4 项）：
        //     - SOURCE_ESSAY_PRACTICE 常量新增（原误用 SOURCE_QUIZ_WRONG，语义不精确）
        //     - WrongAnswerScreen/KnowledgePointDetailScreen UI 标签新增"论述题自评"
        //     - Icons.AutoMirrored.Filled.Send 弃用修复（原 Icons.Filled.Send）
        //     - SocraticTutorTest/AiAssistantViewModelTest 接口提取后实例化修正
        //   本地验证：:app:assembleDebug + 全模块 testDebugUnitTest 全绿（471 tests, 0 failures）
        // v0.9.16：真题→论述题迁移（底部导航 Tab 替换 + 代码清理 + AboutTutorialScreen 修复）
        // v0.9.19：紧凑玻璃风格导航栏 + 种子加载 300s+重试机制
        // v0.9.21：MD3 规范底栏回归 + inset 双重消费修复
        // v0.9.22：深度审查改进批 A+B（见 versionCode 注释）
        // v0.9.23：论述题删年份 + Snackbar 修复 + AI 审计修复 + 更新日志机制（见 versionCode 注释）
        // v0.9.24：批一 AI 体验 + 批二工程质量（见 versionCode 注释）
        // v0.9.25：AI 生成全新启动图标 + 整体界面审查修复（见 versionCode 注释）
        // v0.9.26：新启动图标 v7.4 + 批三（见 versionCode 注释）
        // v0.9.27：图标 v7.5 精进 + 全面检查 P1-1/2 + 内容补齐 25 个（见 versionCode 注释）
        // v0.9.28：App 内更新下载修复（见 versionCode 注释）
        // v0.9.29：卡片备考系统（见 versionCode 注释）
        // v0.9.30：卡片打磨 + UI/UX + 仓库卫生 + i18n（见 versionCode 注释）
        // v0.9.31~35：学习科学 / AI 修复 / 真题背题 / 横屏适配 / 审计修复（见 versionCode 注释）
        // v0.9.36：知识卡片全屏沉浸模式（见 versionCode 注释）
        // v0.9.37：布局与性能深度优化（见 versionCode 注释）
        // v0.9.38：学习完整性与来源可信度修复（见 versionCode 注释）
        // v0.9.39：知识卡片空队列竞态修复 + 翻转动画平滑化（见 versionCode 注释）
        // v0.9.40：四科知识框架整理与知识点浏览体验优化（见 versionCode 注释）
        versionName = "0.9.40"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Release 签名配置
    // - CI 环境：从环境变量读取 keystore（KEYSTORE_PATH 指向解码后的 .jks 文件）
    // - 本地环境：若无 keystore 配置，fallback 到 debug 签名（避免本地编译失败）
    //
    // P1-S-1 修正：CI 环境（CI=true）不允许 fallback 到 debug 签名。
    // debug keystore 公开，任何人都能签发相同包名 APK，存在安全风险。
    // CI 必须配置正式 keystore，否则 fail fast。
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")

            if (keystorePath != null && File(keystorePath).exists()) {
                storeFile = File(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                println("✓ Release 签名配置已加载: $keystorePath")
            } else {
                println("⚠ 未找到 release keystore，release 构建将使用 debug 签名（本地开发模式）")
            }
        }
    }

    buildTypes {
        release {
            // v0.9.24：启用 R8 混淆 + 压缩。
            // 此前 isMinifyEnabled=false，release APK 未压缩未混淆（可被反编译）。
            // proguard-rules.pro（app）+ 各模块 consumer-rules.pro 已预置
            // Hilt/Compose/serialization/Retrofit/OkHttp/Room 规则。
            // ⚠️ 需 emulator 实测无崩溃后发布（重点：Room/Hilt/序列化/网络）。
            isMinifyEnabled = true
            // v0.9.37 P1-1：资源压缩——R8 裁剪后无用资源一并移除，APK 进一步减负。
            // shrinkResources 依赖 minify（无引用代码被删后资源才可安全移除）。
            // 误删风险兜底：res/raw/keep.xml 可声明 keep，当前项目无此需要。
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // P1-S-1 修正：CI 环境不允许 fallback 到 debug 签名。
            //
            // 注意：CI 检查放在执行阶段（assembleRelease.doFirst），而非配置阶段。
            // 配置阶段抛异常会导致所有任务（包括 testDebugUnitTest）在 CI 中失败。
            // 这里统一 fallback 到 debug 签名，让配置阶段顺利通过。
            // 执行阶段如果检测到 CI 且无 keystore 再中止。
            val releaseConfig = signingConfigs.getByName("release")
            signingConfig = if (releaseConfig.storeFile != null) {
                releaseConfig
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    // P1-S-1b 修正：执行阶段检查 CI 环境 keystore 配置。
    // 仅在真正运行 assembleRelease 时检查，不影响 testDebugUnitTest / assembleDebug。
    tasks.matching { it.name == "assembleRelease" }.configureEach {
        doFirst {
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile == null && System.getenv("CI") == "true") {
                throw GradleException(
                    "Release 签名未配置：CI 环境必须设置 KEYSTORE_PATH / KEYSTORE_PASSWORD / " +
                        "KEY_ALIAS / KEY_PASSWORD 环境变量。debug 签名不允许用于 CI Release 构建。",
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // v0.9.24：MigrationTestHelper 需要 schema JSON 在 androidTest assets 中可访问。
    // 指向 core:database 的 room.schemaDirectory（含 2/4/5/6/7/8/9/10.json）。
    sourceSets {
        getByName("androidTest").assets.srcDir("$rootDir/core/database/schemas")
    }

    buildFeatures {
        compose = true
        // NF-S1 修复：启用 BuildConfig 生成，供 WenyanApplication 判断 BuildConfig.DEBUG 配置 StrictMode。
        // AGP 8.0+ 默认关闭 buildConfig，需显式开启。
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 项目模块依赖
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:fsrs"))
    implementation(project(":core:ai"))
    implementation(project(":feature:knowledge"))
    implementation(project(":feature:quiz"))
    implementation(project(":feature:cards"))
    implementation(project(":feature:aiassistant"))
    implementation(project(":feature:settings"))

    // AndroidX 基础
    implementation(libs.androidx.core.ktx)
    // v0.9.22 清理：移除 androidx-appcompat（死依赖）。
    // MainActivity 用 ComponentActivity（androidx.activity），主题用 material 的 Theme.Material3，
    // 全仓库无 androidx.appcompat 引用；material 传递依赖已覆盖所需。
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.material)
    // Splash Screen API（消除冷启动白屏，Android 12+ 系统原生支持 + 向后兼容）
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // LeakCanary（仅 debug 构建内存泄漏检测，自动初始化，无需代码改动）
    debugImplementation(libs.leakcanary.android)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // 测试
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // v0.9.24：Room 迁移测试（MigrationTestHelper）
    androidTestImplementation(libs.androidx.room.testing)
}
