package com.wenyan.buildlogic

import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Android Library Convention Plugin（v0.9.26 批三）。
 *
 * 抽取 11 个库模块（core 与 feature 系列）共用的纯配置：
 * - compileSdk / minSdk
 * - compileOptions（Java 17）
 * - testInstrumentationRunner
 * - consumerProguardFiles
 *
 * 注意：只抽"纯配置"，插件应用（kotlin.android / hilt / compose / serialization / ksp）
 * 保留在各模块 build.gradle.kts——插件顺序与 KSP 配置差异大，强抽反而增加出错面。
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")

            extensions.configure<LibraryExtension> {
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
            }
        }
    }
}
