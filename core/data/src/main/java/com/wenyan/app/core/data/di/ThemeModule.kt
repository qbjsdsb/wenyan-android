package com.wenyan.app.core.data.di

import com.wenyan.app.core.data.repository.ThemeRepository
import com.wenyan.app.core.data.repository.ThemeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 主题仓库 Hilt 模块。
 *
 * 将 [ThemeRepositoryImpl] 绑定到 [ThemeRepository] 接口。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository
}
