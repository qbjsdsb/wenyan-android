// 文研App 根项目构建文件，配置全局插件版本
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}

// 强制 kotlin-metadata-jvm 使用 2.3.10 版本
// 原因：Hilt 2.57.1 的 kotlin-metadata-jvm 只支持 Kotlin metadata 2.2.0，
//       但 Kotlin 2.3.10 编译器产生 metadata 2.3.0，导致 Hilt KSP 处理失败。
//       Hilt 2.57 起 kotlin-metadata-jvm 被 unshaded（可覆盖）。
//       2.59+ 需 AGP 9，与 AGP 8.6.0 不兼容，故用 force 覆盖版本。
subprojects {
    configurations.configureEach {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.get()}")
        }
    }
}
