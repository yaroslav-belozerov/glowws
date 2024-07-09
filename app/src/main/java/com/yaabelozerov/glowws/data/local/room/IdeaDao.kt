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
    fun getAllIdeas(): Flow<List<Idea>>

    @Insert
    suspend fun createGroup(group: Group): Long

    @Insert
    suspend fun insertIdea(idea: Idea): Long

    @Query("DELETE FROM `idea` WHERE ideaId = :ideaId")
    suspend fun deleteIdea(ideaId: Long)

    @Query(
        "SELECT * FROM `group` JOIN idea ON groupId = groupParentId"
    )
    fun getGroupsWithIdeasAll(): Flow<Map<Group, List<Idea>>>

    @Query(
        "SELECT * FROM `group` JOIN idea ON groupId = groupParentId WHERE isArchived = 0"
    )
    fun getGroupsWithIdeasNotArchived(): Flow<Map<Group, List<Idea>>>

    @Query(
        "SELECT * FROM idea WHERE EXISTS (SELECT * FROM `group` WHERE groupId = groupParentId AND isArchived = 1)"
    )
    fun getArchivedIdeas(): Flow<List<Idea>>

    @Query(
        "SELECT * FROM point WHERE ideaParentId = :ideaId ORDER BY `index` ASC"
    )
    fun getIdeaPoints(ideaId: Long): Flow<List<Point>>

    @Query("UPDATE idea SET content = :content WHERE ideaId = :ideaId")
    suspend fun modifyIdeaContent(ideaId: Long, content: String)

    @Query("UPDATE idea SET groupParentId = :groupId WHERE ideaId = :ideaId")
    suspend fun modifyIdeaGroup(ideaId: Long, groupId: Long)

    @Query("SELECT * FROM point WHERE pointId = :pointId")
    fun getPoint(pointId: Long): Flow<Point>

    @Upsert
    suspend fun upsertPoint(point: Point): Long

    @Insert
    suspend fun insertPoint(point: Point): Long

    suspend fun upsertPointUpdateIdea(point: Point): Long {
        val newPointId = upsertPoint(point)
        updateIdeaContentFromPoints(point.ideaParentId)
        return newPointId
    }

    suspend fun insertPointUpdateIdeaAtIndex(point: Point): Long {
        movePointsIndexesUp(point.ideaParentId, point.index)
        val newPointId = insertPoint(point)
        updateIdeaContentFromPoints(point.ideaParentId)
        return newPointId
    }

    @Query("UPDATE point SET `index` = `index` + 1 WHERE ideaParentId = :ideaId AND `index` >= :threshold")
    suspend fun movePointsIndexesUp(ideaId: Long, threshold: Long)

    @Query("UPDATE point SET `index` = `index` - 1 WHERE ideaParentId = :ideaId AND `index` > :threshold")
    suspend fun movePointsIndexesDown(ideaId: Long, threshold: Long)

    suspend fun updateIdeaContentFromPoints(ideaId: Long) {
        val pts = getIdeaPoints(ideaId).first()
        val content: String =
            pts.firstOrNull { it.isMain }?.content ?: (pts.firstOrNull()?.content ?: "")
        modifyIdeaContent(ideaId, content)
    }

    @Query("DELETE FROM point WHERE pointId = :pointId")
    suspend fun deletePoint(pointId: Long)

    suspend fun deletePointAndIndex(pointId: Long) {
        val point = getPoint(pointId).first()
        deletePoint(pointId)
        movePointsIndexesDown(point.ideaParentId, point.index)
    }

    @Query("DELETE FROM point WHERE ideaParentId = :ideaId")
    suspend fun deleteIdeaPoints(ideaId: Long)

    @Query("UPDATE point SET content = :newText WHERE pointId = :pointId")
    suspend fun updatePointContent(pointId: Long, newText: String)

    suspend fun createIdeaAndGroup(content: String): Long {
        val groupId = createGroup(Group(0, "", isArchived = false))
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
        if (getAllIdeasFromGroup(groupId).isEmpty()) deleteGroupOnly(groupId)
    }

    @Query("DELETE FROM `group` WHERE groupId = :groupId")
    suspend fun deleteGroupOnly(groupId: Long)

    suspend fun deleteGroup(groupId: Long) {
        deleteGroupOnly(groupId)
        deleteGroupIdeas(groupId)
    }

    @Query("DELETE FROM `group` WHERE NOT EXISTS (SELECT 1 FROM idea WHERE groupParentId = groupId)")
    suspend fun cleanProjects()

    @Query("UPDATE `group` SET isArchived = 1 WHERE groupId = :groupId")
    suspend fun archiveGroup(groupId: Long)

    suspend fun archiveStrayIdea(ideaId: Long) {
        val groupId = createGroup(Group(0, "", isArchived = true))
        modifyIdeaGroup(ideaId, groupId)
        cleanProjects()
    }

    @Query("UPDATE `group` SET isArchived = 0 WHERE EXISTS (SELECT * FROM idea WHERE groupParentId = groupId AND ideaId = :ideaId) AND isArchived = 1")
    suspend fun unarchiveIdea(ideaId: Long)

    @Query("UPDATE `group` SET name = :newName WHERE groupId = :groupId")
    suspend fun updateGroupName(groupId: Long, newName: String)

    @Query("DELETE FROM idea WHERE groupParentId = :groupId")
    suspend fun deleteGroupIdeas(groupId: Long)
}