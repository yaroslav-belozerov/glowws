package com.yaabelozerov.glowws.di

import android.content.Context
import androidx.room.Room
import com.yaabelozerov.glowws.data.local.room.IdeaDatabase
import com.yaabelozerov.glowws.domain.model.IdeaMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
}