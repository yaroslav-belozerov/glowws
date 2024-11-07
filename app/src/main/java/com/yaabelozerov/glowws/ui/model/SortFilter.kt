package com.yaabelozerov.glowws.ui.model

import com.yaabelozerov.glowws.R

enum class SortOrder(val resId: Int) {
  ASCENDING(R.string.s_default_sort_asc),
  DESCENDING(R.string.s_default_sort_desc)
}

fun SortOrder.reversed(): SortOrder {
  return if (this == SortOrder.ASCENDING) {
    SortOrder.DESCENDING
  } else {
    SortOrder.ASCENDING
  }
}

enum class SortType(val resId: Int) {
  ALPHABETICAL(R.string.s_user_default_sort_alpha),
  TIMESTAMP_CREATED(R.string.s_user_default_sort_creation),
  TIMESTAMP_MODIFIED(R.string.s_user_default_sort_modification),
  PRIORITY(R.string.sort_priority)
}

data class SortModel(val order: SortOrder, val type: SortType)

sealed class FilterFlag(val resId: Int) {
  data class WithPriority(val priority: List<Int>): FilterFlag(R.string.filter_priority)
}

data class FilterModel(val flags: Map<FilterFlag, Boolean>)
