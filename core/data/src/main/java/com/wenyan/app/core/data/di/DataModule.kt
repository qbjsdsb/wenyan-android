package com.wenyan.app.core.data.di

import com.wenyan.app.core.data.repository.GraphRepository
import com.wenyan.app.core.data.repository.GraphRepositoryImpl
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
 * DAO 由 [com.wenyan.app.core.database.di.DatabaseModule] 提供。
 *
 * 本模块使用 abstract class 以支持 @Binds 方法；未来若需 @Provides 方法
 * （如 [com.wenyan.app.core.fsrs.FsrsWrapper]，其构造函数需要三档配置参数），
 * 可在 companion object 中添加。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindGraphRepository(impl: GraphRepositoryImpl): GraphRepository
}
