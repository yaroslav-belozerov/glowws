package com.yaabelozerov.glowws.data.local.room

import androidx.room.Dao
import androidx.room.Delete
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

    @Query("DELETE FROM `idea` WHERE ideaId = :ideaId")
    suspend fun deleteIdea(ideaId: Long)

    @Query(
        "SELECT * FROM `group` JOIN idea ON groupId = groupParentId"
    )
    fun getGroupsWithIdeas(): Flow<Map<Group, List<Idea>>>

    @Query(
        "SELECT * FROM point WHERE ideaParentId = :ideaId"
    )
    fun getIdeaPoints(ideaId: Long): Flow<List<Point>>

    @Insert
    suspend fun insertPoint(point: Point)

    suspend fun createIdeaAndGroup(content: String): Long {
        val groupId = createGroup(Group(0, ""))
        return insertIdea(Idea(0, groupId, content))
    }
}