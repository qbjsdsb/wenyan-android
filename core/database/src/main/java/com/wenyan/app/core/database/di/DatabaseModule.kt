package com.wenyan.app.core.database.di

import android.content.Context
import androidx.room.Room
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.dao.AiGradingRecordDao
import com.wenyan.app.core.database.dao.AnswerTemplateDao
import com.wenyan.app.core.database.dao.ApiConfigDao
import com.wenyan.app.core.database.dao.AppMetaDao
import com.wenyan.app.core.database.dao.ChapterDao
import com.wenyan.app.core.database.dao.ChatConversationDao
import com.wenyan.app.core.database.dao.ChatMessageDao
import com.wenyan.app.core.database.dao.DataSourceDao
import com.wenyan.app.core.database.dao.ExamCodeHistoryDao
import com.wenyan.app.core.database.dao.ExamQuestionDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.dao.ReviewLogDao
import com.wenyan.app.core.database.dao.StudyProgressDao
import com.wenyan.app.core.database.dao.SubjectDao
import com.wenyan.app.core.database.dao.TemplateFillDao
import com.wenyan.app.core.database.dao.WritingMaterialDao
import com.wenyan.app.core.database.dao.WritingPatternDao
import com.wenyan.app.core.database.dao.WrongAnswerDao
import com.wenyan.app.core.database.migration.MIGRATION_1_2
import com.wenyan.app.core.database.migration.MIGRATION_2_3
import com.wenyan.app.core.database.migration.MIGRATION_3_4
import com.wenyan.app.core.database.migration.MIGRATION_4_5
import com.wenyan.app.core.database.migration.MIGRATION_5_6
import com.wenyan.app.core.database.migration.MIGRATION_6_7
import com.wenyan.app.core.database.migration.MIGRATION_7_8
import com.wenyan.app.core.database.migration.MIGRATION_8_9
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库 Hilt 模块。
 *
 * 提供：
 * - [WenyanDatabase] 单例（数据库文件 wenyan.db）
 * - 19 个 DAO 的 @Provides 方法
 *
 * 安装到 [SingletonComponent] 以保证全局单例。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供 WenyanDatabase 单例。
     *
     * 使用 ApplicationContext 构建数据库，allowMainThreadQueries 默认不开启，
     * 所有数据库操作须在协程中执行。
     *
     * 迁移策略：
     * - [MIGRATION_1_2]：memo_records 补 elapsed_days/scheduled_days/reps 字段
     * - [MIGRATION_2_3]：回填 reps = review_count（NF-D1 修复，v1→v2 未回填导致老卡片误判为新卡）
     * - [MIGRATION_3_4]：新增 app_meta 表（NF-B / P0-E4 修复，存储 last_known_timestamp_ms 供 ClockGuard 用）
     * - [MIGRATION_4_5]：P1 大型任务合并 schema 变更（NF-PP4 删 history / NF-PP6 合并 chat 表 / NF-PP5 新增 wrong_answers）
     * - [MIGRATION_5_6]：v0.7.6 删除 exam_questions.sample_essay 列（范文冗余字段清理）
     * - [MIGRATION_6_7]：v0.9.3 优化 4 DROP graph_nodes + graph_edges（core 层图谱设施无消费者）
     * - [MIGRATION_7_8]：v0.9.4 wrong_answers 添加 10 个 sched_* FSRS 调度字段 + 索引
     * - [MIGRATION_8_9]：v0.9.22 为存量 v8 用户补建 wrong_answers 两个复合索引
     *   （MIGRATION_7_8 遗漏的 [point_id, source] / [exam_question_id, source]）
     * - fallbackToDestructiveMigrationOnDowngrade：仅版本号降级时重建表（开发期降级测试用）。
     *   P0-D1 修正：原 fallbackToDestructiveMigration() 在升级时也会清空整个数据库，
     *   v0.2.0 已发布用户有真实 FSRS 复习记录，升级时被静默清空是不可接受的。
     *   改为 OnDowngrade 后，未来升级若缺少迁移将抛出 IllegalStateException 而非静默丢数据。
     */
    @Provides
    @Singleton
    fun provideWenyanDatabase(
        @ApplicationContext context: Context,
    ): WenyanDatabase {
        return Room.databaseBuilder(
            context,
            WenyanDatabase::class.java,
            WenyanDatabase.DATABASE_NAME,
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    // ---------------- 各 DAO 的 @Provides ----------------

    @Provides
    fun provideSubjectDao(database: WenyanDatabase): SubjectDao =
        database.subjectDao()

    @Provides
    fun provideChapterDao(database: WenyanDatabase): ChapterDao =
        database.chapterDao()

    @Provides
    fun provideKnowledgePointDao(database: WenyanDatabase): KnowledgePointDao =
        database.knowledgePointDao()

    @Provides
    fun provideExamQuestionDao(database: WenyanDatabase): ExamQuestionDao =
        database.examQuestionDao()

    @Provides
    fun provideMemoRecordDao(database: WenyanDatabase): MemoRecordDao =
        database.memoRecordDao()

    @Provides
    fun provideStudyProgressDao(database: WenyanDatabase): StudyProgressDao =
        database.studyProgressDao()

    @Provides
    fun provideWritingMaterialDao(database: WenyanDatabase): WritingMaterialDao =
        database.writingMaterialDao()

    @Provides
    fun provideApiConfigDao(database: WenyanDatabase): ApiConfigDao =
        database.apiConfigDao()

    @Provides
    fun provideAiGradingRecordDao(database: WenyanDatabase): AiGradingRecordDao =
        database.aiGradingRecordDao()

    @Provides
    fun provideAnswerTemplateDao(database: WenyanDatabase): AnswerTemplateDao =
        database.answerTemplateDao()

    @Provides
    fun provideTemplateFillDao(database: WenyanDatabase): TemplateFillDao =
        database.templateFillDao()

    @Provides
    fun provideWritingPatternDao(database: WenyanDatabase): WritingPatternDao =
        database.writingPatternDao()

    @Provides
    fun provideReviewLogDao(database: WenyanDatabase): ReviewLogDao =
        database.reviewLogDao()

    @Provides
    fun provideExamCodeHistoryDao(database: WenyanDatabase): ExamCodeHistoryDao =
        database.examCodeHistoryDao()

    @Provides
    fun provideDataSourceDao(database: WenyanDatabase): DataSourceDao =
        database.dataSourceDao()

    @Provides
    fun provideAppMetaDao(database: WenyanDatabase): AppMetaDao =
        database.appMetaDao()

    @Provides
    fun provideChatConversationDao(database: WenyanDatabase): ChatConversationDao =
        database.chatConversationDao()

    @Provides
    fun provideChatMessageDao(database: WenyanDatabase): ChatMessageDao =
        database.chatMessageDao()

    @Provides
    fun provideWrongAnswerDao(database: WenyanDatabase): WrongAnswerDao =
        database.wrongAnswerDao()
}
