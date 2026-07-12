// core:fsrs 模块 —— FSRS-Kotlin 算法封装（间隔重复调度）
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.wenyan.app.core.fsrs"
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:common"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // FSRS-6 算法已由 FsrsWrapper.kt 自行实现，无需外部依赖

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
