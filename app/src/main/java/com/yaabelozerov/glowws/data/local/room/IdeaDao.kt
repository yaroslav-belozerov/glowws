package com.yaabelozerov.glowws.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * from idea")
    fun getAll(): Flow<List<Idea>>

    @Insert
    suspend fun createGroup(group: Group): Long

    @Insert
    suspend fun insert(idea: Idea)

    @Query(
        "SELECT * FROM `group`" +
                "JOIN idea ON groupId = groupParentId"
    )
    fun getGroupsWithIdeas(): Flow<Map<Group, List<Idea>>>
}