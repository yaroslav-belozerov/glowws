package com.yaabelozerov.glowws.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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

    @Query("UPDATE idea SET content = :content WHERE ideaId = :ideaId")
    suspend fun modifyIdeaContent(ideaId: Long, content: String)

    @Query("SELECT * FROM point WHERE pointId = :pointId")
    fun getPoint(pointId: Long): Flow<Point>

    @Upsert
    suspend fun upsertPoint(point: Point): Long

    suspend fun upsertPointUpdateIdea(point: Point): Long {
        val newPointId = upsertPoint(point)
        updateIdeaContentFromPoints(point.ideaParentId)
        return newPointId
    }

    suspend fun updateIdeaContentFromPoints(ideaId: Long) {
        val pts = getIdeaPoints(ideaId).first()
        val content: String =
            pts.firstOrNull { it.isMain }?.content ?: (pts.firstOrNull()?.content ?: "")
        modifyIdeaContent(ideaId, content)
    }

    @Query("DELETE FROM point WHERE pointId = :pointId")
    suspend fun deletePoint(pointId: Long)

    @Query("DELETE FROM point WHERE ideaParentId = :ideaId")
    suspend fun deleteIdeaPoints(ideaId: Long)

    @Query("UPDATE point SET content = :newText WHERE pointId = :pointId")
    suspend fun updatePointContent(pointId: Long, newText: String)

    suspend fun createIdeaAndGroup(content: String): Long {
        val groupId = createGroup(Group(0, ""))
        return insertIdea(Idea(0, groupId, content))
    }

    @Query("SELECT groupParentId FROM idea WHERE ideaId = :ideaId")
    suspend fun getGroupByIdea(ideaId: Long): Long

    @Query("SELECT * FROM idea WHERE groupParentId = :groupId")
    suspend fun getAllIdeasFromGroup(groupId: Long): List<Idea>

    suspend fun deleteIdeaAndPoints(ideaId: Long) {
        val groupId = getGroupByIdea(ideaId)
        deleteIdea(ideaId)
        deleteIdeaPoints(ideaId)
        if (getAllIdeasFromGroup(groupId).isEmpty()) deleteGroup(groupId)
    }

    @Query("DELETE FROM `group` WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Query("UPDATE `group` SET name = :newName WHERE groupId = :groupId")
    suspend fun updateGroupName(groupId: Long, newName: String)
}