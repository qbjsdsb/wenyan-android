package com.wenyan.app.core.designsystem.di

import com.wenyan.app.core.designsystem.theme.ThemeRepository
import com.wenyan.app.core.designsystem.theme.ThemeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 主题仓库 Hilt 模块。
 *
 * 将 [ThemeRepositoryImpl] 绑定到 [ThemeRepository] 接口。
 *
 * P1-8 修复：从 core/data 迁入 core/designsystem，与 ThemeRepository 同模块。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository
}
