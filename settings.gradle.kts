// 文研App 顶层项目设置，统一管理所有模块
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // JitPack 仓库，用于 FSRS-Kotlin 库
    }
}

rootProject.name = "wenyan"

// 应用入口模块
include(":app")

// 核心模块（被各 feature 依赖，提供基础设施）
include(":core:common")
include(":core:database")
include(":core:data")
include(":core:designsystem")
include(":core:fsrs")
include(":core:ai")

// 功能模块（互相独立，可单独编译测试）
include(":feature:knowledge")
include(":feature:quiz")
include(":feature:cards")
include(":feature:graph")
include(":feature:aiassistant")
