package com.yaabelozerov.glowws.domain.mapper

import com.yaabelozerov.glowws.data.local.room.Group
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.domain.model.GroupDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale

class IdeaMapper {
    fun toDomainModel(
        mp: Map<Group, List<Idea>>
    ): Map<GroupDomainModel, List<IdeaDomainModel>> {
        val out: MutableMap<GroupDomainModel, List<IdeaDomainModel>> = mutableMapOf()
        val pattern = "yyyy-MM-dd HH:mm:ss"

        for ((key, value) in mp) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = key.timestampModified
            out[GroupDomainModel(
                key.groupId, SimpleDateFormat(pattern, Locale.ROOT).format(cal.time), key.name
            )] = value.map {
                cal.timeInMillis = it.timestampModified
                IdeaDomainModel(
                    it.ideaId,
                    SimpleDateFormat(pattern, Locale.ROOT).format(cal.time),
                    it.groupParentId,
                    it.content
                )
            }
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