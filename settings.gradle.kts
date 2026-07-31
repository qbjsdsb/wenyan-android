// 文研App 顶层项目设置，统一管理所有模块
pluginManagement {
    repositories {
        // 全局仓库优先（CI runner 在美/欧，Aliyun 镜像可能不可达）
        gradlePluginPortal()
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // 国内镜像作为 fallback（本地开发加速）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // CI 环境（美/欧 runner）：阿里云镜像可能不可达，官方仓库优先
        // 本地环境（国内）：阿里云镜像优先，加速依赖下载
        val isCI = System.getenv("CI") == "true"
        if (isCI) {
            google()
            mavenCentral()
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        } else {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            google()
            mavenCentral()
        }
        // P0-B8 修正：移除 jitpack.io 死仓库声明（项目无 com.github.* 依赖，FSRS 自实现）。
        // 原声明导致 CI/沙盒环境（jitpack 不可达）依赖解析卡在 TCP 超时。
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
// v0.9.0：feature:graph 已移除（图谱功能弃用，改走知识点树结构 + 关联模块）
include(":feature:knowledge")
include(":feature:quiz")
include(":feature:cards")
include(":feature:aiassistant")
include(":feature:settings")
