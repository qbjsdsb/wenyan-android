package com.wenyan.app.core.ai.di

import com.wenyan.app.core.ai.AiService
import com.wenyan.app.core.ai.AiServiceImpl
import com.wenyan.app.core.ai.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * core:ai 的 Hilt 模块。
 *
 * 提供：
 * - [AiService] → [AiServiceImpl] 绑定（@Binds）
 * - [OkHttpClient] 单例（@Provides，用于 Retrofit 构造）
 *
 * OkHttpClient 配置：
 * - 30 秒超时（LLM API 响应较慢）
 * - 日志拦截器：
 *   - Debug 构建用 BODY 级别（含 Authorization 头），便于联调
 *   - Release 构建用 NONE 级别，避免 API Key 泄漏到 logcat
 *   - P1-H2 修正：原实现始终 BODY 级别，Release 包中 API Key 会写入 logcat
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiService(impl: AiServiceImpl): AiService

    companion object {

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()
        }
    }
}
