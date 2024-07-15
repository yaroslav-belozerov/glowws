package com.yaabelozerov.glowws.ui.model

import com.yaabelozerov.glowws.R


enum class SortOrder(val resId: Int) {
    ASCENDING(R.string.s_default_sort_asc), DESCENDING(R.string.s_default_sort_desc)
}

fun SortOrder.reversed(): SortOrder {
    return if (this == SortOrder.ASCENDING) {
        SortOrder.DESCENDING
    } else {
        SortOrder.ASCENDING
    }
}

enum class SortType(val resId: Int) {
    ALPHABETICAL(R.string.s_user_default_sort_alpha), TIMESTAMP_CREATED(R.string.s_user_default_sort_creation), TIMESTAMP_MODIFIED(R.string.s_user_default_sort_modification)
}

data class SortModel(
    val order: SortOrder,
    val type: SortType
)

enum class FilterFlag(val resId: Int) {
    IN_GROUP(R.string.s_default_filter_ingroup)
}

data class FilterModel(
    val flags: Map<FilterFlag, Boolean>
)