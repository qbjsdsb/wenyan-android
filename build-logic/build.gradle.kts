// build-logic：convention plugins（v0.9.26 批三——抽取模块重复配置）
plugins {
    `kotlin-dsl`
}

dependencies {
    // 与根 gradle/libs.versions.toml 保持一致（AGP 8.6.0 / Kotlin 2.3.10）
    implementation("com.android.tools.build:gradle:8.6.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
}

gradlePlugin {
    plugins {
        create("androidLibrary") {
            id = "com.wenyan.buildlogic.android-library"
            implementationClass = "com.wenyan.buildlogic.AndroidLibraryConventionPlugin"
        }
    }
}
