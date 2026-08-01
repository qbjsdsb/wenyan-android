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
        versionCode = 44
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
        versionName = "0.9.19"

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
            isMinifyEnabled = false
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
    implementation(libs.androidx.appcompat)
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
}
