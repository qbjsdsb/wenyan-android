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

# OkHttpClient / Interceptor 内部字段（via builder 反射）
-keep class okhttp3.** { *; }
