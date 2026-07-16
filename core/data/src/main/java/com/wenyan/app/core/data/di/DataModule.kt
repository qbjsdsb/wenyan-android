package com.wenyan.app.core.data.di

import com.wenyan.app.core.ai.LlmConfigProvider
import com.wenyan.app.core.data.repository.ChatRepository
import com.wenyan.app.core.data.repository.ChatRepositoryImpl
import com.wenyan.app.core.data.repository.GraphRepository
import com.wenyan.app.core.data.repository.GraphRepositoryImpl
import com.wenyan.app.core.data.repository.LlmConfigProviderImpl
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
}
