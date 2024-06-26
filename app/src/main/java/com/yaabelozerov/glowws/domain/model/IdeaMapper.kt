package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.room.Group
import com.yaabelozerov.glowws.data.local.room.Idea

class IdeaMapper {
    fun toDomainModel(
        mp: Map<Group, List<Idea>>, onClick: (Long) -> Unit, onRemove: (Long) -> Unit
    ): Map<GroupDomainModel, List<IdeaDomainModel>> {
        val out: MutableMap<GroupDomainModel, List<IdeaDomainModel>> = mutableMapOf()

        for ((key, value) in mp) {
            out[GroupDomainModel(key.groupId, key.name)] = value.map {
                IdeaDomainModel(it.ideaId,
                    { onClick(it.ideaId) },
                    { onRemove(it.ideaId) },
                    it.content
                )
            }
        }
        return out
    }
}