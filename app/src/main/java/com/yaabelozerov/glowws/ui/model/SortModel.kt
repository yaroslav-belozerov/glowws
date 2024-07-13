package com.yaabelozerov.glowws.ui.model

enum class SortOrder {
    ASCENDING, DESCENDING
}

fun SortOrder.reversed(): SortOrder {
    if (this == SortOrder.ASCENDING) {
        return SortOrder.DESCENDING
    } else {
        return SortOrder.ASCENDING
    }
}

enum class SortType {
    ALPHABETICAL, TIMESTAMP_CREATED, TIMESTAMP_MODIFIED
}

data class SortModel(
    val order: SortOrder,
    val type: SortType
)

enum class FilterFlag {
    IN_GROUP
}

data class FilterModel(
    val flags: Map<FilterFlag, Boolean>
)