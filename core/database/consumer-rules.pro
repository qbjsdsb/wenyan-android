# consumer-rules.pro：core:database 模块消费者 ProGuard 规则（P1-PG Wave 4 补齐）
#
# core:database 模块包含：Room Database（WenyanDatabase）+ 20 个 @Entity +
# 18 个 @Dao + WenyanTypeConverters。
# Room 编译器生成的代码通过反射访问 Entity / Dao，启用 R8 时必须保留。

# ============ Room @Entity ============
# Room 编译器生成的实现通过反射读写 Entity 字段，必须保留所有字段。
-keep @androidx.room.Entity class * { *; }

# ============ Room @Dao ============
# Dao 接口由 Room 编译器生成实现，接口方法签名必须保留。
-keep @androidx.room.Dao interface * { *; }

# ============ Room @Database ============
# WenyanDatabase 类（@Database）由 Room 编译器生成实现，必须保留。
-keep @androidx.room.Database class *
-keep class * extends androidx.room.RoomDatabase { *; }

# ============ Room @TypeConverter ============
# WenyanTypeConverters 的 @TypeConverter 方法通过反射调用。
-keep @androidx.room.TypeConverter class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# ============ Room 编译器生成的类 ============
# Room 生成的 _Impl 类（WenyanDatabase_Impl / XxxDao_Impl）必须保留。
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class **_Impl { *; }

# ============ 20 个 Entity 类（显式列出，便于排查）============
# AiGradingRecordEntity / AnswerTemplateEntity / ApiConfigEntity / AppMetaEntity
# ChapterEntity / ChatConversationEntity / ChatMessageEntity / DataSourceEntity
# ExamCodeHistoryEntity / ExamQuestionEntity / GraphEdgeEntity / GraphNodeEntity
# KnowledgePointEntity / MemoRecordEntity / ReviewLogEntity / StudyProgressEntity
# SubjectEntity / TemplateFillEntity / WrongAnswerEntity / WritingMaterialEntity
# WritingPatternEntity
# 上述 @Entity 规则已通用覆盖，无需单独声明。
