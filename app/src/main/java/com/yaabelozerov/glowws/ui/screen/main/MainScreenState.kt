package com.yaabelozerov.glowws.ui.screen.main

import com.yaabelozerov.glowws.domain.model.GroupDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel

data class MainScreenState(
    val ideas: Map<GroupDomainModel, List<IdeaDomainModel>> = emptyMap()
)