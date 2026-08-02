// feature:settings 模块 —— 设置页面（主题模式/AMOLED/调色板风格/关于）
plugins {
    id("com.wenyan.buildlogic.android-library")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.wenyan.app.feature.settings"

    defaultConfig {

        // v0.9.22 清理：删除 VERSION_NAME buildConfigField（死代码）。
        // SettingsScreen/UpdateCheckScreen 已用 context.packageManager.getPackageInfo()
        // 读取真实 versionName（SettingsScreen.kt:95 / UpdateCheckScreen.kt:83），
        // 全仓库无 BuildConfig.VERSION_NAME 引用。此前注释要求与 app 版号同步，
        // 是过时的手动同步点（v0.9.21 曾因漏同步踩坑）。
    }

    buildFeatures {
        compose = true
        // UpdateCheckScreen.kt:161 使用 BuildConfig.DEBUG（区分 Debug/Release 构建），需保留 buildConfig
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

    // OkHttp（软件内更新：APK 下载）
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
