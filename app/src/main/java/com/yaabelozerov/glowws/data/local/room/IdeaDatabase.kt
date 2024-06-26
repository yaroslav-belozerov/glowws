package com.yaabelozerov.glowws.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [IdeaEntity::class], version = 1, exportSchema = false)
abstract class IdeaDatabase : RoomDatabase() {
    abstract fun ideaDao(): IdeaDao
}