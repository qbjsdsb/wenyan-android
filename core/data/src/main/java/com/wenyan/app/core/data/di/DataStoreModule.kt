package com.wenyan.app.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DataStore Preferences Hilt 模块。
 *
 * 提供全局唯一的 [DataStore]<[Preferences]> 实例。
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.dataStoreFile("wenyan_preferences.preferences_pb") },
    )
}
