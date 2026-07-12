package com.wenyan.app.core.data.di

import android.content.Context
import com.wenyan.app.core.data.crypto.ApiKeyCrypto
import com.wenyan.app.core.data.crypto.ApiKeyCryptoImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AI 数据层 Hilt 模块（阶段4新增）。
 *
 * 提供 AI 相关的 @Provides 方法，与 [DataModule]（abstract class，仅 @Binds）分离。
 *
 * 提供：
 * - [ApiKeyCrypto] → [ApiKeyCryptoImpl] 绑定
 */
@Module
@InstallIn(SingletonComponent::class)
object AiDataModule {

    /**
     * 提供 [ApiKeyCrypto] 单例。
     *
     * 使用 [ApiKeyCryptoImpl]（AndroidKeyStore + AES-256-GCM）。
     */
    @Provides
    @Singleton
    fun provideApiKeyCrypto(
        @ApplicationContext context: Context,
    ): ApiKeyCrypto = ApiKeyCryptoImpl(context)
}
