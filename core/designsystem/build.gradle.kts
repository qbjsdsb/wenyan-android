// core:designsystem 模块 —— 设计系统（颜色 / 字体 / 通用组件 / 主题状态）
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.wenyan.app.core.designsystem"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // P1-8 修复：ThemeRepositoryImpl.catch 块调用 android.util.Log.e，
            // JVM 单元测试环境不 mock Log，默认抛 RuntimeException。
            // isReturnDefaultValues=true 使 Log 方法返回默认值（0）而非抛异常。
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // core:common（ContentSource 字符串常量用于内容来源标注组件）
    // P1-7 修复：原 implementation(project(":core:database")) 是死依赖——
    // designsystem 不应反向依赖 database，仅为此前内嵌的 ContentSource object 而设。
    // ContentSource 已迁至 core/common，依赖随之切换。
    implementation(project(":core:common"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Material 3 Adaptive（WindowSizeClass 驱动自适应布局，v0.6 大屏适配）
    api(libs.androidx.compose.material3.adaptive)
    // MaterialKolor 动态色彩生成（KSU 同款）
    api(libs.materialkolor)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Compose UI 测试所需的 ComponentActivity 声明（合并到 debug/test manifest）
    // 不加这个，createComposeRule() 在 Robolectric 下会报
    // "Unable to resolve activity for Intent ... ComponentActivity"
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.core.ktx)

    // Hilt（P1-8 修复：ThemeRepository/ThemeViewModel/ThemeModule 迁入 designsystem）
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // ViewModel（ThemeViewModel 需要）
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // DataStore（ThemeRepositoryImpl 需要）
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // Compose UI 测试（Robolectric + createComposeRule，JVM 跑无需 emulator）
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.tooling)
}
