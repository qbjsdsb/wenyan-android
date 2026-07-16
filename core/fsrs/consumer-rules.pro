# consumer-rules.pro：core:fsrs 模块消费者 ProGuard 规则（P1-PG Wave 4 补齐）
#
# core:fsrs 模块包含：FSRS-6 自实现算法（FsrsWrapper）+ 数据类（FlashCard /
# ReviewLog / SchedulingCard）+ 枚举（Rating / State / MemoryTier / ContentType /
# StudyPhase）+ 配置（TierFsrsConfig）。
# 这些类被 SchedulingRepository 通过 @Inject constructor 注入，且枚举值
# 通过 name() 序列化到数据库（review_logs.rating / memo_records.state）。

# ============ FSRS 数据类 ============
# FlashCard / ReviewLog / SchedulingCard 被 FsrsWrapper 内部反射访问。
-keep class com.wenyan.app.core.fsrs.FlashCard { *; }
-keep class com.wenyan.app.core.fsrs.ReviewLog { *; }
-keep class com.wenyan.app.core.fsrs.SchedulingCard { *; }
-keep class com.wenyan.app.core.fsrs.TierFsrsConfig { *; }

# ============ FSRS 枚举 ============
# Rating / State / MemoryTier / ContentType / StudyPhase 的 name() 值
# 被序列化到数据库（如 rating.name() = "AGAIN"/"GOOD"/"EASY"），
# 枚举常量名必须保留，否则反序列化会失败。
-keep enum com.wenyan.app.core.fsrs.Rating { *; }
-keep enum com.wenyan.app.core.fsrs.State { *; }
-keep enum com.wenyan.app.core.fsrs.MemoryTier { *; }
-keep enum com.wenyan.app.core.fsrs.ContentType { *; }
-keep enum com.wenyan.app.core.fsrs.StudyPhase { *; }

# ============ FsrsWrapper ============
# FsrsWrapper 是核心算法类，构造方法被 SchedulingRepositoryImpl 反射调用。
-keep class com.wenyan.app.core.fsrs.FsrsWrapper { *; }

# ============ TIER_CONFIGS 常量 ============
# TIER_CONFIGS 是 top-level val（MemoryTier → TierFsrsConfig 映射），
# 被 SchedulingRepositoryImpl 通过 `TIER_CONFIGS[tier]` 访问。
# Kotlin top-level val 编译为 FileNameKt 类的静态字段，保留 FsrsWrapperKt。
-keep class com.wenyan.app.core.fsrs.FsrsWrapperKt { *; }
-keep class com.wenyan.app.core.fsrs.TierFsrsConfigKt { *; }
