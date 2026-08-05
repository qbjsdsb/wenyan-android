# 文研App ProGuard 规则（P1-PG Wave 4 补齐）
#
# v0.9.24 起 isMinifyEnabled=true（R8/minify 已启用）且 v0.9.37 起
# isShrinkResources=true（资源压缩）；本文件（app 专用）+ consumer-rules.pro
# （各模块）合并生效。
#
# 参考来源：
# - Hilt: https://dagger.dev/hilt/proguard-guide
# - Compose: https://developer.android.com/jetpack/compose/performance/stability#proguard
# - kotlinx.serialization: https://github.com/Kotlin/kotlinx.serialization#proguard

# ============ 通用属性保留 ============
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod

# ============ Hilt（Application / Activity 入口）============
# @HiltAndroidApp Application 类必须保留（Hilt 代码生成入口）
-keep @dagger.hilt.android.HiltAndroidApp class *
# @AndroidEntryPoint Activity / Fragment / View / Compose 保留
-keep @dagger.hilt.android.AndroidEntryPoint class *
# @HiltViewModel 通用保留（各 feature 模块的 consumer-rules.pro 也会单独声明）
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ============ Compose ============
# Compose Compiler 自带 keep 规则，通常无需手动添加。
# 以下为 Compose runtime 通用保留，避免稳定性推断误删 @Immutable / @Stable 标注类。
-keep @androidx.compose.runtime.Immutable class *
-keep @androidx.compose.runtime.Stable class *

# ============ Kotlin Metadata ============
# Compose / Hilt / Retrofit 反射依赖 Kotlin Metadata，必须保留。
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# ============ Kotlinx Coroutines ============
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============ 反射调用兜底 ============
# 部分库（DataStore / Room）可能通过反射访问字段，保留 synthetic accessor。
-keepclassmembers class * {
    *** Companion;
}
