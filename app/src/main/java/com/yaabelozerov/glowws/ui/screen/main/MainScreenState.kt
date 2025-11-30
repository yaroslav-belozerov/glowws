package com.yaabelozerov.glowws.ui.screen.main

import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaModel
import com.yaabelozerov.glowws.domain.model.IdeaModelFull
import com.yaabelozerov.glowws.domain.model.archived
import com.yaabelozerov.glowws.domain.model.notArchived
import com.yaabelozerov.glowws.domain.model.toDomain
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.FilterModel
import com.yaabelozerov.glowws.ui.model.SortModel
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import kotlin.reflect.KClass

data class MainScreenState(
  val ideas: List<IdeaDomainModel> = emptyList(),
  val archivedIdeas: List<IdeaDomainModel> = emptyList(),
)

data class MainScreenFilterState(
  val sort: SortModel = SortModel(SortOrder.ASCENDING, SortType.TIMESTAMP_MODIFIED),
  val filter: Map<KClass<FilterFlag>, FilterFlag> = mapOf(FilterFlag::class to FilterFlag.WithPriority(emptyList())),
  val searchQuery: String = ""
)

fun MainScreenFilterState.buildParams(isArchive: Boolean = false): List<Pair<String, String>>? = buildList {
  if (isArchive) return null
  if (searchQuery.isNotBlank()) {
    add("search" to searchQuery)
  }
  add("filter" to filter.map { filter ->
    when (val value = filter.value) {
      is FilterFlag.WithPriority -> value.priority.map { "priority=$it" }
    }
  }.joinToString(","))
  add("sort" to sort.type.name.lowercase())
  add("sort_dir" to sort.order.name.lowercase())
}

fun MainScreenState.setIdeas(list: List<IdeaModelFull>, instanceUrl: String) =
  copy(ideas = list.notArchived().toDomain(instanceUrl), archivedIdeas = list.archived().toDomain(instanceUrl))
