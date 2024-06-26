package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.room.Group
import com.yaabelozerov.glowws.data.local.room.Idea

class IdeaMapper {
    fun toDomainModel(
        mp: Map<Group, List<Idea>>
    ): Map<GroupDomainModel, List<IdeaDomainModel>> {
        val out: MutableMap<GroupDomainModel, List<IdeaDomainModel>> = mutableMapOf()

        for ((key, value) in mp) {
            out[GroupDomainModel(key.groupId, key.name)] = value.map {
                IdeaDomainModel(
                    it.ideaId, it.content
                )
            }
        }
        return out
    }
}