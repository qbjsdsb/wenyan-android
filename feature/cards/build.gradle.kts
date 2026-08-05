// feature:cards 模块 —— 记忆卡片（MVVM：Screen + ViewModel）
plugins {
    id("com.wenyan.buildlogic.android-library")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.wenyan.app.feature.cards"


    buildFeatures {
        compose = true
    }

    // v0.8.20 P1-2 新增:与 feature/knowledge 一致。
    // 单元测试 JVM 环境不 mock Android 框架类(如 android.database.sqlite.SQLiteException),
    // isReturnDefaultValues=true 使未 mock 的方法返回默认值而非抛 RuntimeException,
    // 允许在测试中实例化 SQLiteException 验证 friendlyErrorMessage 的"本地数据异常"分支。
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            // v0.9.35 横屏审查:Robolectric 需要解析 Android 资源/manifest
            //（含 ui-test-manifest 的 ComponentActivity 声明）才能跑 Compose 截图
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // core:common 提供 friendlyErrorMessage(P1-2 抽取,跨模块复用)
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
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
    // P0 v0.7.2: 测试需要访问 StudyProgressDao/StudyProgressEntity(Fake 实现)
    testImplementation(project(":core:database"))
    // v0.9.18: Compose UI 测试依赖（AddToWrongAnswerButton 测试）
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // v0.9.35 横屏审查:Robolectric 渲染截图（JVM 跑无需 emulator）
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.tooling)
}
