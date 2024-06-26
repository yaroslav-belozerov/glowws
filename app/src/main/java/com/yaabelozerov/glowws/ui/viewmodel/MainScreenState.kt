package com.yaabelozerov.glowws.ui.viewmodel

import com.yaabelozerov.glowws.domain.model.GroupDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class MainScreenState(
    val ideas: Map<GroupDomainModel, List<IdeaDomainModel>> = emptyMap()
)