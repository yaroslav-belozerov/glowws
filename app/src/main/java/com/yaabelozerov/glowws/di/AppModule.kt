package com.yaabelozerov.glowws.di

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yaabelozerov.glowws.data.local.room.IdeaDatabase
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideIdeaDatabase(@ApplicationContext app: Context) = Room.databaseBuilder(
        app, IdeaDatabase::class.java, "glowws.db"
    ).build()

    @Singleton
    @Provides
    fun provideIdeaDao(db: IdeaDatabase) = db.ideaDao()

    @Singleton
    @Provides
    fun provideIdeaMapper() = IdeaMapper()

    @Singleton
    @Provides
    fun provideMoshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val Context.dataStore by preferencesDataStore("settings")

    @Singleton
    class DataStoreManager @Inject constructor(@ApplicationContext appContext: Context) {
        private val settingsDataStore = appContext.dataStore

        private val displayKey = stringPreferencesKey("display_settings")
        private val userKey = stringPreferencesKey("user_settings")

        fun getDisplay(): Flow<String> = settingsDataStore.data.map { it[displayKey] ?: "" }

        suspend fun setDisplay(settings: String) = settingsDataStore.edit {
            it[displayKey] = settings
        }

        fun getUser(): Flow<String> = settingsDataStore.data.map { it[userKey] ?: "" }

        suspend fun setUser(settings: String) = settingsDataStore.edit {
            it[userKey] = settings
        }
    }
}