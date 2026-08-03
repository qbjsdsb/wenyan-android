// 文研App 根项目构建文件，配置全局插件版本
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
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
    // 统一 Kotlin JVM target = 17（与各模块 Java 17 对齐）。
    // 避免本机 JDK 版本变化（如 JDK 20）导致 Kotlin 默认 jvmTarget 20 与 Java 17 冲突
    // （CI 使用 temurin JDK 17，本机需在任意 JDK ≥ 17 下均可构建）。
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    configurations.configureEach {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.get()}")
        }
    }
}
