// core:ai 模块 —— AI 助手 RAG 架构封装（LLM API 客户端 / 提示词 / 批改 / 加密）
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.wenyan.app.core.ai"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    // P1-H2 修正：启用 BuildConfig 以区分 Debug/Release 日志级别
    // （AGP 8.0+ 库模块默认不生成 BuildConfig）
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:database"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // 网络请求 Retrofit + OkHttp（调用大模型 API）
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX Security + Keystore（API key 安全存储）
    implementation(libs.androidx.security.crypto)

    // DataStore（API 配置存储）
    implementation(libs.androidx.datastore.preferences)

    // Compose Runtime（@Immutable 注解，标记 AI 数据类为 Compose 稳定类型，零运行时开销）
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
