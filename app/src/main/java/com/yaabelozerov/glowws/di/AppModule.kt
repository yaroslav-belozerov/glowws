package com.yaabelozerov.glowws.di

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import coil.ImageLoader
import coil.imageLoader
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yaabelozerov.glowws.data.local.ai.InferenceManager
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.media.MediaManager
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.IdeaDatabase
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
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
        app,
        IdeaDatabase::class.java,
        "glowws.db"
    ).build()

    @Singleton
    @Provides
    fun provideIdeaDao(db: IdeaDatabase) = db.ideaDao()

    @Singleton
    @Provides
    fun provideIdeaMapper(dao: IdeaDao) = IdeaMapper(dao)

    @Singleton
    @Provides
    fun provideSettingsMapper() = SettingsMapper()

    @Singleton
    @Provides
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(SettingsKeys::class.java, EnumJsonAdapter.create(SettingsKeys::class.java).withUnknownFallback(null))
        .add(KotlinJsonAdapterFactory())
        .build()

    @Singleton
    @Provides
    fun provideSettingsManager(dataStoreManager: DataStoreManager, moshi: Moshi, settingsMapper: SettingsMapper) =
        SettingsManager(dataStoreManager, moshi, settingsMapper)

    @Singleton
    @Provides
    fun provideMediaManager(@ApplicationContext app: Context) = MediaManager(app)

    @Singleton
    @Provides
    fun provideCoilImageLoader(@ApplicationContext app: Context): ImageLoader = app.imageLoader.newBuilder().crossfade(
        true
    ).build()

    @Singleton
    @Provides
    fun provideInferenceManager(
        @ApplicationContext app: Context
    ): InferenceManager = InferenceManager(app)

    private val Context.dataStore by preferencesDataStore("settings")

    @Singleton
    class DataStoreManager @Inject constructor(@ApplicationContext appContext: Context) {
        private val settingsDataStore = appContext.dataStore

        private val settingsKey = stringPreferencesKey("settings")
        fun getSettings(): Flow<String> = settingsDataStore.data.map { it[settingsKey] ?: "" }
        suspend fun setSettings(settings: String) =
            settingsDataStore.edit { it[settingsKey] = settings }

        private val timesOpenedKey = longPreferencesKey("times_opened")
        fun getTimesOpened(): Flow<Long> = settingsDataStore.data.map { it[timesOpenedKey] ?: 0 }
        suspend fun setTimesOpened(timesOpened: Long) =
            settingsDataStore.edit { it[timesOpenedKey] = timesOpened }

        private val currentModelName = stringPreferencesKey("current_model_name")
        fun getCurrentModelName(): Flow<String> =
            settingsDataStore.data.map { it[currentModelName] ?: "" }

        suspend fun setCurrentModelName(name: String) =
            settingsDataStore.edit { it[currentModelName] = name }
    }
}
