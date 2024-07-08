package com.yaabelozerov.glowws.domain.mapper

import com.yaabelozerov.glowws.data.local.room.Group
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.domain.model.GroupDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel

class IdeaMapper {
    fun toDomainModel(
        mp: Map<Group, List<Idea>>
    ): Map<GroupDomainModel, List<IdeaDomainModel>> {
        val out: MutableMap<GroupDomainModel, List<IdeaDomainModel>> = mutableMapOf()

        for ((key, value) in mp) {
            out[GroupDomainModel(key.groupId, key.name)] = value.map {
                IdeaDomainModel(
                    it.ideaId, it.groupParentId, it.content
                )
            }
        }
        return out
    }
}