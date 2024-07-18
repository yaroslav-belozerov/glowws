package com.yaabelozerov.glowws.data.local.room

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.yaabelozerov.glowws.ui.model.FilterModel
import com.yaabelozerov.glowws.ui.model.SortModel
import com.yaabelozerov.glowws.ui.model.SortType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Dao
interface IdeaDao {
    @Query("SELECT * from `idea`")
    fun getAllIdeas(): Flow<List<Idea>>

    @Query("SELECT * FROM idea WHERE ideaId = :ideaId")
    fun getIdea(ideaId: Long): Flow<Idea>

    @Insert
    suspend fun createGroup(group: Group): Long

    @Insert
    suspend fun insertIdea(idea: Idea): Long

    suspend fun addIdea(idea: Idea): Long {
        val ideaId = insertIdea(idea)
        updateGroupTimestamp(getGroupByIdea(ideaId), System.currentTimeMillis())
        return ideaId
    }

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
    suspend fun setIdeaContent(ideaId: Long, content: String)

    suspend fun modifyIdeaContent(ideaId: Long, content: String) {
        setIdeaContent(ideaId, content)
        val m = System.currentTimeMillis()
        updateIdeaTimestamp(ideaId, m)
        updateGroupTimestamp(getGroupByIdea(ideaId), m)
    }

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
        val parent = getGroupByIdea(ideaId)
        val siblings = getAllIdeasFromGroup(parent)
        val content: String =
            pts.firstOrNull { it.isMain }?.content ?: (pts.firstOrNull()?.content ?: "")
        if (siblings.size == 1) { updateGroupName(parent, content) }
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

    @Query("SELECT ideaId FROM idea WHERE EXISTS (SELECT * FROM point WHERE pointId = :pointId AND ideaParentId = ideaId)")
    suspend fun getIdeaIdFromPointId(pointId: Long): Long

    @Query("UPDATE point SET content = :newText WHERE pointId = :pointId")
    suspend fun setPointContent(pointId: Long, newText: String)

    suspend fun updatePointContent(pointId: Long, newText: String) {
        setPointContent(pointId, newText)
        updateIdeaTimestamp(getIdeaIdFromPointId(pointId), System.currentTimeMillis())
    }

    @Query("UPDATE idea SET timestampModified = :timestamp WHERE ideaId = :ideaId")
    suspend fun updateIdeaTimestamp(ideaId: Long, timestamp: Long)

    @Query("UPDATE `group` SET timestampModified = :timestamp WHERE groupId = :groupId")
    suspend fun updateGroupTimestamp(groupId: Long, timestamp: Long)

    suspend fun createIdeaAndGroup(content: String): Long {
        val m = System.currentTimeMillis()
        val groupId = createGroup(Group(0, m, m, content, isArchived = false))
        return insertIdea(Idea(0, groupId, m, m, content))
    }

    @Query("SELECT groupParentId FROM idea WHERE ideaId = :ideaId")
    suspend fun getGroupByIdea(ideaId: Long): Long

    @Query("SELECT * FROM idea WHERE groupParentId = :groupId")
    suspend fun getAllIdeasFromGroup(groupId: Long): List<Idea>

    suspend fun deleteIdeaAndPoints(ideaId: Long) {
        val groupId = getGroupByIdea(ideaId)
        deleteIdea(ideaId)
        deleteIdeaPoints(ideaId)
        val all = getAllIdeasFromGroup(groupId)
        if (all.isEmpty()) deleteGroupOnly(groupId)
        else updateGroupTimestamp(groupId, System.currentTimeMillis())
    }

    @Query("DELETE FROM `group` WHERE groupId = :groupId")
    suspend fun deleteGroupOnly(groupId: Long)

    suspend fun deleteGroup(groupId: Long) {
        deleteGroupOnly(groupId)
        deleteGroupIdeas(groupId)
    }

    @Query("DELETE FROM `group` WHERE NOT EXISTS (SELECT * FROM idea WHERE groupParentId = groupId)")
    suspend fun cleanProjects()

    @Query("UPDATE `group` SET isArchived = 1 WHERE groupId = :groupId")
    suspend fun setArchiveGroup(groupId: Long)

    suspend fun archiveGroup(groupId: Long) {
        val m = System.currentTimeMillis()
        getAllIdeasFromGroup(groupId).forEach {
            val newGroup = createGroup(Group(0, m, m, "", isArchived = true))
            modifyIdeaGroup(it.ideaId, newGroup)
        }
        cleanProjects()
    }

    suspend fun archiveStrayIdea(ideaId: Long) {
        val m = System.currentTimeMillis()
        val parent = getGroupByIdea(ideaId)
        updateGroupTimestamp(parent, m)
        val groupId = createGroup(Group(0, m, m, "", isArchived = true))
        modifyIdeaGroup(ideaId, groupId)
        cleanProjects()
    }

    @Query("UPDATE `group` SET isArchived = 0 WHERE groupId = :groupId AND isArchived = 1")
    suspend fun setNotArchivedGroup(groupId: Long)

    suspend fun unarchiveIdea(ideaId: Long) {
        val parent = getGroupByIdea(ideaId)
        setNotArchivedGroup(parent)
        val content = getIdea(ideaId).first().content
        updateGroupName(parent, content)
    }

    @Query("UPDATE `group` SET name = :newName WHERE groupId = :groupId")
    suspend fun setGroupName(groupId: Long, newName: String)

    suspend fun updateGroupName(groupId: Long, newName: String) {
        setGroupName(groupId, newName)
        updateGroupTimestamp(groupId, System.currentTimeMillis())
    }

    @Query("DELETE FROM idea WHERE groupParentId = :groupId")
    suspend fun deleteGroupIdeas(groupId: Long)
}