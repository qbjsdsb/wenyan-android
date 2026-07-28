# consumer-rules.pro：core:data 模块消费者 ProGuard 规则（P1-PG Wave 4 补齐）
#
# core:data 模块包含：6 个 SeedDataLoader @Serializable 种子数据类 +
# 仓库实现（SchedulingRepositoryImpl / ExamRepositoryImpl / CardRepositoryImpl 等）+
# GraphSkeleton 预置图结构。

# ============ kotlinx.serialization ============
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class **.$$serializer {
    *;
}

# SeedDataLoader.kt 的 6 个 @Serializable 种子数据类：
# SeedData / SeedMetadata / SubjectSeed / KnowledgePointSeed / ExamQuestionSeed / WritingMaterialSeed
-keep class com.wenyan.app.core.data.seed.** { *; }

# ============ Graph Skeleton ============
# GraphSkeleton 预置图结构（data class，可能被反射读取）
# 注：line 18 的 -keep class com.wenyan.app.core.data.seed.** 已覆盖此类，
# 此处显式声明作为重要类的文档标记（B5.1 修正路径：原误写为 .graph.GraphSkeleton）
-keep class com.wenyan.app.core.data.seed.GraphSkeleton { *; }
-keep class com.wenyan.app.core.data.seed.GraphSkeleton$* { *; }

# ============ Repository 实现 ============
# 仓库实现类通过 Hilt @Inject constructor 注入，Hilt 规则已在 app/proguard-rules.pro 覆盖。
# 此处保留 repository 包通用兜底（防止 R8 误删 @Binds 绑定的 Impl 类）。
-keep class com.wenyan.app.core.data.repository.*Impl { *; }

# ============ Mapper ============
# Mapper 类通过 @Inject constructor 注入，保留无参构造方法。
-keep class com.wenyan.app.core.data.mapper.*Mapper { *; }
