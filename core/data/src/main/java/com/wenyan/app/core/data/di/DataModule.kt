package com.wenyan.app.core.data.di

import com.wenyan.app.core.ai.LlmConfigProvider
import com.wenyan.app.core.data.repository.CardRepository
import com.wenyan.app.core.data.repository.CardRepositoryImpl
import com.wenyan.app.core.data.repository.CardSettingsRepository
import com.wenyan.app.core.data.repository.CardSettingsRepositoryImpl
import com.wenyan.app.core.data.repository.ChapterRepository
import com.wenyan.app.core.data.repository.ChapterRepositoryImpl
import com.wenyan.app.core.data.repository.ChatRepository
import com.wenyan.app.core.data.repository.ChatRepositoryImpl
import com.wenyan.app.core.data.repository.ClockGuard
import com.wenyan.app.core.data.repository.ClockGuardImpl
import com.wenyan.app.core.data.repository.ExamRepository
import com.wenyan.app.core.data.repository.ExamRepositoryImpl
import com.wenyan.app.core.data.repository.LlmConfigProviderImpl
import com.wenyan.app.core.data.repository.KnowledgeProgressRepository
import com.wenyan.app.core.data.repository.KnowledgeProgressSource
import com.wenyan.app.core.data.repository.PracticeAttemptStore
import com.wenyan.app.core.data.repository.PracticeAttemptStoreImpl
import com.wenyan.app.core.data.repository.SchedulingRepository
import com.wenyan.app.core.data.repository.SchedulingRepositoryImpl
import com.wenyan.app.core.data.repository.UpdateRepository
import com.wenyan.app.core.data.repository.UpdateRepositoryImpl
import com.wenyan.app.core.data.repository.WritingEvidenceRepository
import com.wenyan.app.core.data.repository.WritingEvidenceSource
import com.wenyan.app.core.data.repository.WritingSessionStore
import com.wenyan.app.core.data.repository.WritingSessionStoreImpl
import com.wenyan.app.core.data.writing.SystemWritingClock
import com.wenyan.app.core.data.writing.WritingClock
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据仓库层 Hilt 模块（Task 16）。
 *
 * Repository 统一通过 `@Inject constructor` + `@Singleton` 注入：
 * - [com.wenyan.app.core.data.repository.ReviewRepository]
 * - [com.wenyan.app.core.data.repository.ExamRepository]
 * - [com.wenyan.app.core.data.repository.CardRepository]
 *
 * [LlmConfigProvider] 为接口（core:ai 定义），实现 [LlmConfigProviderImpl] 通过 @Inject constructor 提供，
 * 此处通过 @Binds 绑定。
 *
 * DAO 由 [com.wenyan.app.core.database.di.DatabaseModule] 提供。
 *
 * 本模块使用 abstract class 以支持 @Binds 方法；@Provides 方法在 [AiDataModule] 中。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindWritingEvidenceSource(impl: WritingEvidenceRepository): WritingEvidenceSource

    @Binds
    @Singleton
    abstract fun bindWritingClock(impl: SystemWritingClock): WritingClock

    @Binds
    @Singleton
    abstract fun bindWritingSessionStore(impl: WritingSessionStoreImpl): WritingSessionStore

    @Binds
    @Singleton
    abstract fun bindPracticeAttemptStore(impl: PracticeAttemptStoreImpl): PracticeAttemptStore

    @Binds
    @Singleton
    abstract fun bindKnowledgeProgressSource(impl: KnowledgeProgressRepository): KnowledgeProgressSource

    @Binds
    @Singleton
    abstract fun bindLlmConfigProvider(impl: LlmConfigProviderImpl): LlmConfigProvider

    /**
     * 绑定 [ChatRepository] 到 [ChatRepositoryImpl](NF-PP6 Wave 2.3)。
     *
     * ChatRepositoryImpl 通过 @Inject constructor 注入 ChatConversationDao +
     * ChatMessageDao + DataStore<Preferences>,DAO 由 DatabaseModule 提供,
     * DataStore 由 DataStoreModule 提供。
     */
    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    /** v0.9.29：卡片备考设置仓库绑定。 */
    @Binds
    @Singleton
    abstract fun bindCardSettingsRepository(impl: CardSettingsRepositoryImpl): CardSettingsRepository

    /**
     * 绑定 [WrongAnswerRepository] 到 [WrongAnswerRepositoryImpl](NF-PP5 Wave 2.4)。
     *
     * WrongAnswerRepositoryImpl 通过 @Inject constructor 注入 WrongAnswerDao,
     * DAO 由 DatabaseModule 提供。
     */
    @Binds
    @Singleton
    abstract fun bindWrongAnswerRepository(impl: WrongAnswerRepositoryImpl): WrongAnswerRepository

    /**
     * 绑定 [ClockGuard] 到 [ClockGuardImpl]（v0.9.5 follow-up #1 提取接口）。
     *
     * ClockGuardImpl 通过 @Inject constructor 注入 AppMetaDao,
     * DAO 由 DatabaseModule 提供。提取接口使 ViewModel 层可注入 ClockGuard
     * 且测试可用 FakeClockGuard 替换,无需依赖 Room。
     */
    @Binds
    @Singleton
    abstract fun bindClockGuard(impl: ClockGuardImpl): ClockGuard

    /**
     * 绑定 [SchedulingRepository] 到 [SchedulingRepositoryImpl](NF-PP5 Wave 3.2 提取接口)。
     *
     * SchedulingRepositoryImpl 通过 @Inject constructor 注入 WenyanDatabase +
     * MemoRecordDao + ReviewLogDao + ClockGuard + WrongAnswerDao,DAO 由 DatabaseModule 提供,
     * ClockGuard 由本模块 @Binds 绑定（ClockGuardImpl → ClockGuard 接口）。
     */
    @Binds
    @Singleton
    abstract fun bindSchedulingRepository(impl: SchedulingRepositoryImpl): SchedulingRepository

    /**
     * 绑定 [ExamRepository] 到 [ExamRepositoryImpl](NF-PP5 Wave 3.2 提取接口)。
     *
     * ExamRepositoryImpl 通过 @Inject constructor 注入 ExamQuestionDao +
     * ExamCodeHistoryDao + KnowledgePointDao,DAO 由 DatabaseModule 提供。
     */
    @Binds
    @Singleton
    abstract fun bindExamRepository(impl: ExamRepositoryImpl): ExamRepository

    /**
     * 绑定 [CardRepository] 到 [CardRepositoryImpl](NF-PP5 Wave 3.2 提取接口)。
     *
     * CardRepositoryImpl 通过 @Inject constructor 注入 KnowledgePointDao,
     * DAO 由 DatabaseModule 提供。
     */
    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    /**
     * 绑定 [ChapterRepository] 到 [ChapterRepositoryImpl]（ADR-001 B1.2）。
     *
     * ChapterRepositoryImpl 通过 @Inject constructor 注入 SubjectDao + ChapterDao +
     * KnowledgePointDao，DAO 由 DatabaseModule 提供。
     */
    @Binds
    @Singleton
    abstract fun bindChapterRepository(impl: ChapterRepositoryImpl): ChapterRepository

    /**
     * 绑定 [UpdateRepository] 到 [UpdateRepositoryImpl]（v0.9.11 检查更新功能）。
     *
     * UpdateRepositoryImpl 通过 @Inject constructor 注入（无参），
     * 使用 JDK 内置 HttpURLConnection 调用 GitHub Releases API。
     */
    @Binds
    @Singleton
    abstract fun bindUpdateRepository(impl: UpdateRepositoryImpl): UpdateRepository
}
