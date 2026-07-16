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
        // v0.1.0=1, v0.2.0=2, v0.3.0=3, v0.4.0=4, v0.5.0=5（启动图标重做 + 第五轮深度审计 P0/P1/P2 21 项修复）
        versionCode = 5
        // P1-M1 修正：versionName 与实际版本对齐（原 "0.1.0" 误标三版未更新）
        versionName = "0.5.0"

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
    implementation(project(":feature:graph"))
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
