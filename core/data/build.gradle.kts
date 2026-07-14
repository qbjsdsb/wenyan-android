// core:data 模块 —— 数据仓库层（Repository 模式），协调 Room / 网络 / 本地资源
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.wenyan.app.core.data"
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

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    api(project(":core:database"))
    api(project(":core:fsrs"))
    api(project(":core:ai"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Lifecycle ViewModel（ThemeViewModel 需要）
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Room KTX（P0-D2 修正：SeedDataLoader 需要 withTransaction 扩展以包裹 7 步种子导入）
    // core:database 仅 implementation room-ktx，未 api 暴露，需在此显式声明
    implementation(libs.androidx.room.ktx)

    // Kotlinx Serialization（用于解析种子数据 JSON）
    implementation(libs.kotlinx.serialization.json)

    // DataStore（记录是否已初始化种子数据）
    // 注意：dataStoreFile 扩展在 androidx.datastore.core 包，由 datastore-core 提供；
    //       Color 在 androidx.compose.ui.graphics。两者原先靠 materialkolor 的 api
    //       传递依赖可见，material3 升级后传递链断裂，必须在此显式声明。
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)

    // Compose（ThemeRepository / ThemeViewModel 的 Color 类型需要）
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.graphics)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
