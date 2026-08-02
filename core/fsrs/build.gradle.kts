// core:fsrs 模块 —— FSRS-Kotlin 算法封装（间隔重复调度）
plugins {
    id("com.wenyan.buildlogic.android-library")
}

android {
    namespace = "com.wenyan.app.core.fsrs"


}

dependencies {
    implementation(project(":core:common"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // FSRS-6 算法已由 FsrsWrapper.kt 自行实现，无需外部依赖

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
