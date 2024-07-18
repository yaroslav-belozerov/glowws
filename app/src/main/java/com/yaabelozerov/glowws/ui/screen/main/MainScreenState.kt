package com.yaabelozerov.glowws.ui.screen.main

import com.yaabelozerov.glowws.domain.model.GroupDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.ui.model.FilterModel

import com.yaabelozerov.glowws.ui.model.SortModel
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import java.util.SortedMap

data class MainScreenState(
    val ideas: List<GroupDomainModel> = emptyList(),
    val sort: SortModel = SortModel(SortOrder.ASCENDING, SortType.TIMESTAMP_MODIFIED),
    val filter: FilterModel = FilterModel(emptyMap()),
    val searchQuery: String = ""
)