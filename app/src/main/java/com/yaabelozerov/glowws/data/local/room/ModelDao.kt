package com.yaabelozerov.glowws.data.local.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
  @Query("SELECT * FROM model") fun getAllModels(): Flow<List<Model>>

  @Query("SELECT * FROM model WHERE name = :initialName") fun getModel(initialName: String): Flow<Model>

  @Query("SELECT * FROM model WHERE isChosen = 1") suspend fun getLastActiveModel(): Model?

  @Query("UPDATE model SET isChosen = 0") suspend fun clearChosen()

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertModel(model: Model)

  @Upsert
  suspend fun upsertModel(model: Model)


  @Delete suspend fun deleteModel(model: Model)
}
