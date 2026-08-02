// core:common 模块 —— 通用工具类与基础组件
plugins {
    id("com.wenyan.buildlogic.android-library")
}

android {
    namespace = "com.wenyan.app.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Timber 结构化日志（v0.8.21 引入）
    // 使用 api 暴露：所有依赖 core:common 的模块（feature/* + core/*）可直接使用 Timber，
    // 无需各自声明依赖。Logging.kt 提供 initTimber() 与 ReleaseTree 实现。
    api(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
