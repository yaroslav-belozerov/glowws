package com.yaabelozerov.glowws.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Idea::class, Point::class, Model::class], version = 1, exportSchema = false)
@TypeConverters(ModelTypeConverters::class)
abstract class GlowwsDatabase : RoomDatabase() {
  abstract fun ideaDao(): IdeaDao

  abstract fun modelDao(): ModelDao
}
