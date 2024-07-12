package com.yaabelozerov.glowws.domain.mapper

import android.util.Log
import com.yaabelozerov.glowws.data.local.room.Group
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.domain.model.GroupDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Comparator
import java.util.Date
import java.util.Locale
import java.util.SortedMap

class IdeaMapper {
    fun toDomainModel(
        mp: Map<Group, List<Idea>>, comparator: Comparator<Group>
    ): List<GroupDomainModel> {
        val out: MutableList<GroupDomainModel> = mutableListOf()
        val pattern = "yyyy-MM-dd HH:mm:ss"

        for ((key, value) in mp.toSortedMap(comparator)) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = key.timestampCreated
            val created = cal.time
            cal.timeInMillis = key.timestampModified
            val modified = cal.time
            out.add(GroupDomainModel(key.groupId,
                SimpleDateFormat(pattern, Locale.ROOT).format(created),
                SimpleDateFormat(pattern, Locale.ROOT).format(modified),
                key.name,
                value.map {
                    cal.timeInMillis = it.timestampModified
                    IdeaDomainModel(
                        it.ideaId,
                        SimpleDateFormat(pattern, Locale.ROOT).format(cal.time),
                        it.groupParentId,
                        it.content
                    )
                }))
        }
        return out
    }

    fun toDomainModelFlat(
        lst: List<Idea>
    ): List<IdeaDomainModel> {
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val cal = Calendar.getInstance()

        return lst.map {
            cal.timeInMillis = it.timestampModified
            IdeaDomainModel(
                it.ideaId,
                SimpleDateFormat(pattern, Locale.ROOT).format(cal.time),
                it.groupParentId,
                it.content
            )
        }
    }
}