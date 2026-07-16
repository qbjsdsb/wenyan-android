package com.wenyan.app.core.data.di

import com.wenyan.app.core.ai.LlmConfigProvider
import com.wenyan.app.core.data.repository.CardRepository
import com.wenyan.app.core.data.repository.CardRepositoryImpl
import com.wenyan.app.core.data.repository.ChatRepository
import com.wenyan.app.core.data.repository.ChatRepositoryImpl
import com.wenyan.app.core.data.repository.ExamRepository
import com.wenyan.app.core.data.repository.ExamRepositoryImpl
import com.wenyan.app.core.data.repository.GraphRepository
import com.wenyan.app.core.data.repository.GraphRepositoryImpl
import com.wenyan.app.core.data.repository.LlmConfigProviderImpl
import com.wenyan.app.core.data.repository.SchedulingRepository
import com.wenyan.app.core.data.repository.SchedulingRepositoryImpl
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
 * [GraphRepository] 为接口，生产实现 [GraphRepositoryImpl] 通过 @Inject constructor 提供，
 * 此处通过 [@Binds][Binds] 将实现绑定到接口。
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
    abstract fun bindGraphRepository(impl: GraphRepositoryImpl): GraphRepository

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
     * 绑定 [SchedulingRepository] 到 [SchedulingRepositoryImpl](NF-PP5 Wave 3.2 提取接口)。
     *
     * SchedulingRepositoryImpl 通过 @Inject constructor 注入 WenyanDatabase +
     * MemoRecordDao + ReviewLogDao + ClockGuard,DAO 由 DatabaseModule 提供,
     * ClockGuard 由本模块 @Provides 提供,ClockGuard 由 DatabaseModule 提供 AppMetaDao。
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
}
