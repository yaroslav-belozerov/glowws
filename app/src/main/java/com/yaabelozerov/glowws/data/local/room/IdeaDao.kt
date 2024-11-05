package com.yaabelozerov.glowws.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface IdeaDao {
  @Query("SELECT * from `idea` WHERE isArchived = :archived")
  fun getAllIdeas(archived: Boolean = false): Flow<List<Idea>>

  @Query(
      "SELECT * from `idea` WHERE isArchived = :archived AND EXISTS " +
          "(SELECT * FROM point WHERE pointContent LIKE :query AND ideaParentId = ideaId AND type = :pointType)")
  fun getAllIdeasSearch(
      query: String,
      archived: Boolean = false,
      pointType: PointType = PointType.TEXT
  ): Flow<List<Idea>>

  @Query("SELECT * FROM idea WHERE ideaId = :ideaId") fun getIdea(ideaId: Long): Flow<Idea>

  @Insert suspend fun insertIdea(idea: Idea): Long

  suspend fun addIdea(idea: Idea): Long {
    val ideaId = insertIdea(idea)
    return ideaId
  }

  @Query("DELETE FROM `idea` WHERE ideaId = :ideaId") suspend fun deleteIdea(ideaId: Long)

  @Query("SELECT * FROM point WHERE ideaParentId = :ideaId")
  fun getIdeaAttachments(ideaId: Long): Flow<List<Point>>

  @Query("SELECT * FROM point WHERE ideaParentId = :ideaId ORDER BY `index` ASC")
  fun getIdeaPoints(ideaId: Long): Flow<List<Point>>

  @Query("UPDATE idea SET mainPointId = :mainPointId WHERE ideaId = :ideaId")
  suspend fun setIdeaMainPoint(ideaId: Long, mainPointId: Long)

  suspend fun modifyIdeaMainPoint(ideaId: Long, mainPointId: Long) {
    setIdeaMainPoint(ideaId, mainPointId)
    val m = System.currentTimeMillis()
    updateIdeaTimestamp(ideaId, m)
  }

  @Query("SELECT * FROM point WHERE pointId = :pointId") fun getPoint(pointId: Long): Flow<Point>

  @Upsert suspend fun upsertPoint(point: Point): Long

  @Insert suspend fun insertPoint(point: Point): Long

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

  @Query(
      "UPDATE point SET `index` = `index` + 1 WHERE ideaParentId = :ideaId AND `index` >= :threshold")
  suspend fun movePointsIndexesUp(ideaId: Long, threshold: Long)

  @Query(
      "UPDATE point SET `index` = `index` - 1 WHERE ideaParentId = :ideaId AND `index` > :threshold")
  suspend fun movePointsIndexesDown(ideaId: Long, threshold: Long)

  suspend fun updateIdeaContentFromPoints(ideaId: Long) {
    val pts = getIdeaPoints(ideaId).first()
    val pointId: Long =
        pts.firstOrNull { it.isMain }?.pointId
            ?: (pts.firstOrNull { it.pointContent.isNotBlank() }?.pointId ?: -1L)
    modifyIdeaMainPoint(ideaId, pointId)
  }

  @Query("DELETE FROM point WHERE pointId = :pointId") suspend fun deletePoint(pointId: Long)

  suspend fun deletePointAndIndex(pointId: Long) {
    val point = getPoint(pointId).first()
    deletePoint(pointId)
    movePointsIndexesDown(point.ideaParentId, point.index)
  }

  @Query("DELETE FROM point WHERE ideaParentId = :ideaId")
  suspend fun deleteIdeaPoints(ideaId: Long)

  @Query(
      "SELECT ideaId FROM idea WHERE EXISTS (SELECT * FROM point WHERE pointId = :pointId AND ideaParentId = ideaId)")
  suspend fun getIdeaIdFromPointId(pointId: Long): Long

  @Query("UPDATE point SET pointContent = :newText WHERE pointId = :pointId")
  suspend fun setPointContent(pointId: Long, newText: String)

  suspend fun updatePointContent(pointId: Long, newText: String) {
    setPointContent(pointId, newText)
    updateIdeaTimestamp(getIdeaIdFromPointId(pointId), System.currentTimeMillis())
  }

  @Query("UPDATE idea SET timestampModified = :timestamp WHERE ideaId = :ideaId")
  suspend fun updateIdeaTimestamp(ideaId: Long, timestamp: Long)

  suspend fun createIdea(): Long {
    val m = System.currentTimeMillis()
    return insertIdea(Idea(0, 0, false, m, m, -1L))
  }

  suspend fun deleteIdeaAndPoints(ideaId: Long) {
    deleteIdea(ideaId)
    deleteIdeaPoints(ideaId)
  }

  @Query("UPDATE `idea` SET isArchived = 1 WHERE ideaId = :ideaId")
  suspend fun setArchiveIdea(ideaId: Long)

  @Query("UPDATE `idea` SET isArchived = 0 WHERE ideaId = :ideaId AND isArchived = 1")
  suspend fun setNotArchivedIdea(ideaId: Long)

  @Query("UPDATE idea SET priority = :priority WHERE ideaId = :ideaId")
  suspend fun setPriorityIdea(ideaId: Long, priority: Long)
}
