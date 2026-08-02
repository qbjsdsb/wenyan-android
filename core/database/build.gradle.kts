// core:database 模块 —— Room 数据库层（Entity / DAO / Converter）
plugins {
    id("com.wenyan.buildlogic.android-library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.wenyan.app.core.database"


}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:common"))

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Kotlinx Serialization（用于 TypeConverter 的 JSON 转换）
    implementation(libs.kotlinx.serialization.json)

    // Compose Runtime（@Immutable 注解，标记 Entity 为 Compose 稳定类型，零运行时开销）
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
