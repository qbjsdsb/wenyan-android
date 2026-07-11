// core:fsrs 模块 —— FSRS-Kotlin 算法封装（间隔重复调度）
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.wenyan.app.core.fsrs"
    compileSdk = 34

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

    // FSRS-Kotlin 间隔重复算法库（JitPack）
    // 库内部硬编码 enableFuzz=true，封装层 FsrsWrapper 需覆盖实现 enableFuzz=false（精确记忆档要求）
    implementation(libs.fsrs.kotlin)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
