package com.yaabelozerov.glowws.di

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yaabelozerov.glowws.Const
import com.yaabelozerov.glowws.data.InferenceRepositoryImpl
import com.yaabelozerov.glowws.data.local.ai.InferenceManager
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.media.MediaManager
import com.yaabelozerov.glowws.data.local.room.GlowwsDatabase
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelVariant
import com.yaabelozerov.glowws.data.remote.FeedbackService
import com.yaabelozerov.glowws.data.remote.GigaChatService
import com.yaabelozerov.glowws.data.remote.OpenRouterService
import com.yaabelozerov.glowws.domain.InferenceRepository
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
  @Singleton
  @Provides
  fun provideIdeaDatabase(@ApplicationContext app: Context) =
      Room.databaseBuilder(app, GlowwsDatabase::class.java, "glowws.db").build()

  @Singleton @Provides fun provideIdeaDao(db: GlowwsDatabase) = db.ideaDao()

  @Singleton @Provides fun provideModelDao(db: GlowwsDatabase) = db.modelDao()

  @Singleton @Provides fun provideIdeaMapper(dao: IdeaDao) = IdeaMapper(dao)

  @Singleton @Provides fun provideSettingsMapper() = SettingsMapper()

  @Singleton
  @Provides
  fun provideMoshi(): Moshi {
    val m =
        Moshi.Builder()
            .add(
                SettingsKeys::class.java,
                EnumJsonAdapter.create(SettingsKeys::class.java).withUnknownFallback(null))
            .add(KotlinJsonAdapterFactory())
            .build()
    Log.i(
        "moshi",
        m.adapter(Model::class.java)
            .toJson(Model(-1L, ModelVariant.OPENROUTER, "test", "test", null, true)))
    return m
  }

  @Singleton
  @Provides
  fun provideSettingsManager(
      dataStoreManager: DataStoreManager,
      moshi: Moshi,
      settingsMapper: SettingsMapper
  ) = SettingsManager(dataStoreManager, moshi, settingsMapper)

  @Singleton @Provides fun provideMediaManager(@ApplicationContext app: Context) = MediaManager(app)

  @Singleton
  @Provides
  fun provideInferenceManager(
      @ApplicationContext app: Context,
  ): InferenceManager = InferenceManager(app)

  @Singleton
  @Provides
  fun provideInferenceRepository(
      infm: InferenceManager,
      ors: OpenRouterService,
      gcs: GigaChatService,
      @ApplicationContext app: Context,
      dataStoreManager: DataStoreManager
  ): InferenceRepository = InferenceRepositoryImpl(infm, ors, gcs, app, dataStoreManager)

  @Singleton
  @Provides
  fun provideOpenRoutedService(moshi: Moshi): OpenRouterService =
      Retrofit.Builder()
          .baseUrl(ModelVariant.OPENROUTER.baseUrl)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(OpenRouterService::class.java)

  @Singleton
  @Provides
  fun provideGigaChatService(moshi: Moshi): GigaChatService =
      Retrofit.Builder()
          .baseUrl("https://ngw.devices.sberbank.ru:9443/api/v2/oauth/")
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(GigaChatService::class.java)

  @Singleton
  @Provides
  fun provideFeedbackService(moshi: Moshi): FeedbackService =
      Retrofit.Builder()
          .baseUrl(Const.Net.FEEDBACK_BASE_URL)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(FeedbackService::class.java)

  private val Context.dataStore by preferencesDataStore("settings")

  @Singleton
  class DataStoreManager @Inject constructor(@ApplicationContext appContext: Context) {
    private val settingsDataStore = appContext.dataStore

    private val settingsKey = stringPreferencesKey("settings")

    fun getSettings(): Flow<String> = settingsDataStore.data.map { it[settingsKey].orEmpty() }

    suspend fun setSettings(settings: String) =
        settingsDataStore.edit { it[settingsKey] = settings }

    private val timesOpenedKey = longPreferencesKey("times_opened")

    fun getTimesOpened(): Flow<Long> = settingsDataStore.data.map { it[timesOpenedKey] ?: 0 }

    suspend fun setTimesOpened(timesOpened: Long) =
        settingsDataStore.edit { it[timesOpenedKey] = timesOpened }

    private val tempTokenKey = stringPreferencesKey("temp_token")

    fun getTempToken(): Flow<String> = settingsDataStore.data.map { it[tempTokenKey].orEmpty() }

    suspend fun setTempToken(token: String) = settingsDataStore.edit { it[tempTokenKey] = token }

    private val tempTokenExpiryKey = longPreferencesKey("temp_token_expiry")

    fun getTempTokenExpiry(): Flow<Long> =
        settingsDataStore.data.map { it[tempTokenExpiryKey] ?: 0 }

    suspend fun setTempTokenExpiry(expiresAt: Long) =
        settingsDataStore.edit { it[tempTokenExpiryKey] = expiresAt }
  }
}
