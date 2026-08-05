# consumer-rules.pro：core:ai 模块消费者 ProGuard 规则（P1-PG Wave 4 补齐）
#
# core:ai 模块包含：Retrofit 接口（LlmApiService）+ OkHttp 客户端 +
# kotlinx.serialization DTO（LlmDtos 6 个）+ RAG 引擎。
# 启用 R8 时这些规则自动传递给 app 模块。

# ============ kotlinx.serialization ============
# @Serializable 类的 Companion 与 $$serializer 必须保留（反射构造序列化器）
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class **.$$serializer {
    *;
}
-keepattributes *Annotation*, InnerClasses

# LlmDtos.kt 的 6 个 @Serializable DTO（ChatRequest/Response/Choice/Message/Usage/StreamChunk）
-keep class com.wenyan.app.core.ai.network.** { *; }

# RagEngine.kt 的 RagReference（@Serializable，被 ChatMessageMapper 引用）
-keep class com.wenyan.app.core.ai.RagEngine$RagReference { *; }

# ============ Retrofit ============
# Retrofit 接口的方法签名通过反射调用，必须保留。
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep @retrofit2.http.* interface * { *; }
-keepattributes Signature, Exceptions

# LlmApiService 接口（OpenAI 兼容协议 Retrofit 接口）
-keep interface com.wenyan.app.core.ai.network.LlmApiService { *; }

# ============ OkHttp ============
# OkHttp 平台检测通过反射加载 Platform 类，抑制告警即可。
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# v0.9.37 P1-10：移除过宽的 `-keep class okhttp3.** { *; }`。
# 原规则禁用 OkHttp 的 R8 收缩致 dex 膨胀；OkHttp 4.x AAR 自带精准
# consumer rules（Platform 反射所需 keep 已内置），常规 OkHttpClient/
# Interceptor 使用为直接引用，R8 不会误删，无需手动全量 keep。
# 若未来引入 OkHttp 反射扩展点（如自定义 Platform），在此按需补充
# `-keep class okhttp3.internal.platform.XXX { *; }`。
