package com.yaabelozerov.glowws.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * from ideaentity")
    fun getAll(): Flow<List<IdeaEntity>>

    @Insert
    suspend fun insert(ideaEntity: IdeaEntity)
}