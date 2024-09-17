package com.yaabelozerov.glowws.domain.mapper

import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.FilterModel
import com.yaabelozerov.glowws.ui.model.SortModel
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class IdeaMapper {
    fun toDomainModel(
        ideas: List<Idea>,
        filterModel: FilterModel = FilterModel(emptyMap()),
        sortModel: SortModel = SortModel(SortOrder.DESCENDING, SortType.TIMESTAMP_MODIFIED)
    ): List<IdeaDomainModel> {
        val out: MutableList<IdeaDomainModel> = mutableListOf()
        val pattern = "yyyy-MM-dd HH:mm:ss"

        for (idea in ideas.sortedWith(
            when (sortModel.type) {
                SortType.ALPHABETICAL -> compareBy<Idea> { it.ideaContent }.thenBy { it.timestampModified }

                SortType.TIMESTAMP_CREATED -> compareBy<Idea> { it.timestampCreated }.thenBy { it.timestampModified }

                SortType.TIMESTAMP_MODIFIED -> compareBy<Idea> { it.timestampModified }
            }
        )) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = idea.timestampCreated
            val created = cal.time
            cal.timeInMillis = idea.timestampModified
            val modified = cal.time
            out.add(
                IdeaDomainModel(
                    idea.ideaId,
                    SimpleDateFormat(pattern, Locale.ROOT).format(created),
                    SimpleDateFormat(pattern, Locale.ROOT).format(modified),
                    idea.ideaContent
                )
            )
        }
        if (sortModel.order == SortOrder.DESCENDING) out.reverse()
        return out
    }
}
