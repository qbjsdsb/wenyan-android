// feature:knowledge 模块 —— 知识点浏览（MVVM：Screen + ViewModel）
plugins {
    id("com.wenyan.buildlogic.android-library")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.wenyan.app.feature.knowledge"


    buildFeatures {
        compose = true
    }

    // 单元测试 JVM 环境不 mock Log，默认抛 RuntimeException。
    // isReturnDefaultValues=true 使 Log 方法返回默认值（0）而非抛异常（与 core/data 一致）。
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
    // v0.9.9 Phase 3：论述题 AI 审题助手（SocraticTutor）
    implementation(project(":core:ai"))
    // v0.9.9 Phase 3：错题回写 + FSRS 调度（Rating 枚举）
    implementation(project(":core:fsrs"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
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

    // Serialization（v0.9.8 论述题板块：解析 angle/notes JSON）
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
