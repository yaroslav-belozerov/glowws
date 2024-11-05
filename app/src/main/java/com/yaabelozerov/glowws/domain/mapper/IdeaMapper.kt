package com.yaabelozerov.glowws.domain.mapper

import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.PointType
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.domain.model.JoinedTimestamp
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import com.yaabelozerov.glowws.ui.model.FilterModel
import com.yaabelozerov.glowws.ui.model.SortModel
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class IdeaMapper @Inject constructor(private val dao: IdeaDao) {
  suspend fun toDomainModel(
      ideas: List<Idea>,
      filterModel: FilterModel = FilterModel(emptyMap()),
      sortModel: SortModel = SortModel(SortOrder.DESCENDING, SortType.TIMESTAMP_MODIFIED)
  ): List<IdeaDomainModel> {
    val out: MutableList<IdeaDomainModel> = mutableListOf()
    val pattern = "HH:mm dd.MM.yyyy"

    for (idea in ideas) {
      val pt = if (idea.mainPointId != -1L) dao.getPoint(idea.mainPointId).first() else null
      val point: PointDomainModel =
          if (pt != null) {
            PointDomainModel(pt.pointId, pt.type, pt.pointContent, pt.isMain)
          } else {
            PointDomainModel(-1, PointType.TEXT, "", false)
          }
      val cal = Calendar.getInstance()
      cal.timeInMillis = idea.timestampCreated
      val created = cal.time
      cal.timeInMillis = idea.timestampModified
      val modified = cal.time
      out.add(
          IdeaDomainModel(
              idea.ideaId,
              idea.priority,
              JoinedTimestamp(
                  idea.timestampCreated, SimpleDateFormat(pattern, Locale.ROOT).format(created)),
              JoinedTimestamp(
                  idea.timestampModified, SimpleDateFormat(pattern, Locale.ROOT).format(modified)),
              point))
    }
    out.sortWith(
        when (sortModel.type) {
          SortType.ALPHABETICAL ->
              compareBy<IdeaDomainModel> { it.mainPoint.content }.thenBy { it.modified.timestamp }

          SortType.TIMESTAMP_CREATED ->
              compareBy<IdeaDomainModel> { it.created.timestamp }.thenBy { it.modified.timestamp }

          SortType.TIMESTAMP_MODIFIED -> compareBy { it.modified.timestamp }
          SortType.PRIORITY -> compareBy { it.priority }
        }.thenBy { it.modified.timestamp })
    if (sortModel.order == SortOrder.DESCENDING) out.reverse()
    return out
  }
}
