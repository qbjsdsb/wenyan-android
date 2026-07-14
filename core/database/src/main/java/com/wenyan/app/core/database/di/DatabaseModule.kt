package com.wenyan.app.core.database.di

import android.content.Context
import androidx.room.Room
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.dao.AiConversationDao
import com.wenyan.app.core.database.dao.AiGradingRecordDao
import com.wenyan.app.core.database.dao.AnswerTemplateDao
import com.wenyan.app.core.database.dao.ApiConfigDao
import com.wenyan.app.core.database.dao.ChapterDao
import com.wenyan.app.core.database.dao.ChatHistoryDao
import com.wenyan.app.core.database.dao.DataSourceDao
import com.wenyan.app.core.database.dao.ExamCodeHistoryDao
import com.wenyan.app.core.database.dao.ExamQuestionDao
import com.wenyan.app.core.database.dao.GraphEdgeDao
import com.wenyan.app.core.database.dao.GraphNodeDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.dao.ReviewLogDao
import com.wenyan.app.core.database.dao.StudyProgressDao
import com.wenyan.app.core.database.dao.SubjectDao
import com.wenyan.app.core.database.dao.TemplateFillDao
import com.wenyan.app.core.database.dao.WritingMaterialDao
import com.wenyan.app.core.database.dao.WritingPatternDao
import com.wenyan.app.core.database.migration.MIGRATION_1_2
import com.wenyan.app.core.database.migration.MIGRATION_2_3
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
    fun provideChatHistoryDao(database: WenyanDatabase): ChatHistoryDao =
        database.chatHistoryDao()

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
    fun provideGraphNodeDao(database: WenyanDatabase): GraphNodeDao =
        database.graphNodeDao()

    @Provides
    fun provideGraphEdgeDao(database: WenyanDatabase): GraphEdgeDao =
        database.graphEdgeDao()

    @Provides
    fun provideAiConversationDao(database: WenyanDatabase): AiConversationDao =
        database.aiConversationDao()

    @Provides
    fun provideReviewLogDao(database: WenyanDatabase): ReviewLogDao =
        database.reviewLogDao()

    @Provides
    fun provideExamCodeHistoryDao(database: WenyanDatabase): ExamCodeHistoryDao =
        database.examCodeHistoryDao()

    @Provides
    fun provideDataSourceDao(database: WenyanDatabase): DataSourceDao =
        database.dataSourceDao()
}
