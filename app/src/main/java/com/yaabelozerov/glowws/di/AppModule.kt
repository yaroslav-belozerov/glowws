package com.yaabelozerov.glowws.di

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yaabelozerov.glowws.Const
import com.yaabelozerov.glowws.data.InferenceRepository
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.media.MediaManager
import com.yaabelozerov.glowws.data.local.room.GlowwsDatabase
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.ModelDao
import com.yaabelozerov.glowws.data.remote.FeedbackService
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
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
  fun provideIdeaDatabase(@ApplicationContext app: Context) =
      Room.databaseBuilder(app, GlowwsDatabase::class.java, "glowws.db").build()

  @Singleton @Provides fun provideIdeaDao(db: GlowwsDatabase) = db.ideaDao()

  @Singleton @Provides fun provideModelDao(db: GlowwsDatabase) = db.modelDao()

  @Singleton @Provides fun provideIdeaMapper(dao: IdeaDao) = IdeaMapper(dao)

  @Singleton @Provides fun provideSettingsMapper() = SettingsMapper()

  @Singleton
  @Provides
  fun provideMoshi(): Moshi =
      Moshi.Builder()
          .add(
              SettingsKeys::class.java,
              EnumJsonAdapter.create(SettingsKeys::class.java).withUnknownFallback(null))
          .add(KotlinJsonAdapterFactory())
          .build()

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
  fun provideInferenceRepository(
      @ApplicationContext app: Context,
      dataStoreManager: DataStoreManager,
      modelDao: ModelDao,
  ): InferenceRepository = InferenceRepository(app, modelDao, dataStoreManager)

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

    private val jwtKey = stringPreferencesKey("jwt")
    fun jwt(): Flow<String> = settingsDataStore.data.map { it[jwtKey].orEmpty() }
    suspend fun setJwt(token: String) = settingsDataStore.edit { it[jwtKey] = token }

    private val instanceKey = stringPreferencesKey("instance")
    fun instanceUrl(): Flow<String> = settingsDataStore.data.map { it[instanceKey].orEmpty() }
    suspend fun setInstanceUrl(token: String) = settingsDataStore.edit { it[instanceKey] = token }

    private val loginKey = stringPreferencesKey("login")
    fun login(): Flow<String> = settingsDataStore.data.map { it[loginKey].orEmpty() }
    suspend fun setLogin(login: String) = settingsDataStore.edit { it[loginKey] = login }
  }
}
