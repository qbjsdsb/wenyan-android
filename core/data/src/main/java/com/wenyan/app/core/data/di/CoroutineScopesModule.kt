package com.wenyan.app.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 应用级协程作用域（v0.9.37 P0-2）。
 *
 * 供 Repository 内"共享热流"（[kotlinx.coroutines.flow.stateIn]）使用：
 * 多个 UI 流订阅同一上游时只跑一份 Room 查询/定时器，避免重复订阅空耗。
 *
 * - [SupervisorJob]：单协程失败不连带取消 scope 内其他任务
 * - [Dispatchers.Default]：CPU 密集默认调度；Room/IO 操作内部自行切换
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopesModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
