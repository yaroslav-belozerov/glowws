package com.yaabelozerov.glowws.di

import android.content.Context
import android.util.Log
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
import com.yaabelozerov.glowws.data.InferenceRepositoryImpl
import com.yaabelozerov.glowws.data.local.ai.InferenceManager
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.media.MediaManager
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.GlowwsDatabase
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelType
import com.yaabelozerov.glowws.data.remote.OpenRouterService
import com.yaabelozerov.glowws.domain.InferenceRepository
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.util.Network
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideIdeaDatabase(@ApplicationContext app: Context) = Room.databaseBuilder(
        app, GlowwsDatabase::class.java, "glowws.db"
    ).build()

    @Singleton
    @Provides
    fun provideIdeaDao(db: GlowwsDatabase) = db.ideaDao()

    @Singleton
    @Provides
    fun provideModelDao(db: GlowwsDatabase) = db.modelDao()

    @Singleton
    @Provides
    fun provideIdeaMapper(dao: IdeaDao) = IdeaMapper(dao)

    @Singleton
    @Provides
    fun provideSettingsMapper() = SettingsMapper()

    @Singleton
    @Provides
    fun provideMoshi(): Moshi {
        val m = Moshi.Builder().add(
            SettingsKeys::class.java,
            EnumJsonAdapter.create(SettingsKeys::class.java).withUnknownFallback(null)
        ).add(KotlinJsonAdapterFactory()).build()
        Log.i("moshi", m.adapter(Model::class.java).toJson(Model(-1L, ModelType.OPENROUTER, "test", "test", null, true)))
        return m
    }

    @Singleton
    @Provides
    fun provideSettingsManager(
        dataStoreManager: DataStoreManager, moshi: Moshi, settingsMapper: SettingsMapper
    ) = SettingsManager(dataStoreManager, moshi, settingsMapper)

    @Singleton
    @Provides
    fun provideMediaManager(@ApplicationContext app: Context) = MediaManager(app)

    @Singleton
    @Provides
    fun provideCoilImageLoader(@ApplicationContext app: Context): ImageLoader =
        app.imageLoader.newBuilder().crossfade(
            true
        ).build()

    @Singleton
    @Provides
    fun provideInferenceManager(
        @ApplicationContext app: Context, settingsManager: SettingsManager
    ): InferenceManager = InferenceManager(app, settingsManager)

    @Singleton
    @Provides
    fun provideInferenceRepository(
        infm: InferenceManager, ors: OpenRouterService, @ApplicationContext app: Context, moshi: Moshi
    ): InferenceRepository = InferenceRepositoryImpl(infm, ors, app, moshi)

    @Singleton
    @Provides
    fun provideOpenRoutedService(moshi: Moshi): OpenRouterService =
        Retrofit.Builder().baseUrl(Network.BASE_URL).addConverterFactory(MoshiConverterFactory.create(moshi)).build()
            .create(OpenRouterService::class.java)

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

        private val currentModelId = longPreferencesKey("current_model_name")
        fun getCurrentModelId(): Flow<Long> =
            settingsDataStore.data.map { it[currentModelId] ?: -1L }

        suspend fun setCurrentModelId(id: Long) = settingsDataStore.edit { it[currentModelId] = id }
    }
}
