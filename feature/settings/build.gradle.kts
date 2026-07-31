// feature:settings 模块 —— 设置页面（主题模式/AMOLED/调色板风格/关于）
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.wenyan.app.feature.settings"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // P1-M2：library 模块的 BuildConfig 不含 VERSION_NAME（那是 application 模块属性）。
        // 显式注入 buildConfigField 供 SettingsScreen 显示版本号。
        // 注意：发版时需与 app/build.gradle.kts 的 versionName 保持同步。
        // P0-6 修复：与 app/build.gradle.kts 的 versionName=0.9.6 对齐。
        buildConfigField("String", "VERSION_NAME", "\"0.9.6\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // P1-M2：SettingsScreen 版本号读取 BuildConfig.VERSION_NAME，需显式启用 buildConfig
        buildConfig = true
    }

    // 单元测试 JVM 环境不 mock Log，默认抛 RuntimeException。
    // isReturnDefaultValues=true 使 Log 方法返回默认值（0）而非抛异常（与 core/data、feature:knowledge 一致）。
    // StudyProgressRepository.observeProgress 的 catch 分支调用 Log.e，需此配置避免测试崩溃。
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:fsrs"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
