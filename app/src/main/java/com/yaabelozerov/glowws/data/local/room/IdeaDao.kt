package com.yaabelozerov.glowws.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * from `idea`")
    fun getAll(): Flow<List<Idea>>

    @Insert
    suspend fun createGroup(group: Group): Long

    @Insert
    suspend fun insertIdea(idea: Idea): Long

    @Query(
        "SELECT * FROM `group` JOIN idea ON groupId = groupParentId"
    )
    fun getGroupsWithIdeas(): Flow<Map<Group, List<Idea>>>

    @Query(
        "SELECT * FROM `idea` JOIN point ON ideaId = ideaParentId WHERE ideaId = :ideaId"
    )
    fun getIdeaPoints(ideaId: Long): Flow<List<Point>>

    @Insert
    suspend fun insertPoint(point: Point)
}