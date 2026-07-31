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
        versionCode = 32
        // P1-M1 修正：versionName 与实际版本对齐（原 "0.1.0" 误标三版未更新）
        // v0.9.7：知识卡片功能完善 + 界面审查修复（响应用户反馈"功能还是不够完善"）。
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
        versionName = "0.9.7"

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
            // 本地开发（CI 未设置）允许 fallback，方便开发者无 keystore 时验证 release 编译。
            // CI 环境（CI=true）必须配置正式 keystore，否则 throw GradleException 中止构建。
            val releaseConfig = signingConfigs.getByName("release")
            signingConfig = if (releaseConfig.storeFile != null) {
                releaseConfig
            } else if (System.getenv("CI") == "true") {
                throw GradleException(
                    "Release 签名未配置：CI 环境必须设置 KEYSTORE_PATH / KEYSTORE_PASSWORD / " +
                        "KEY_ALIAS / KEY_PASSWORD 环境变量。debug 签名不允许用于 CI Release 构建。",
                )
            } else {
                signingConfigs.getByName("debug")
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
