package com.yaabelozerov.glowws.data.local.datastore

import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsCategories
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsModel
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsTypes
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType

enum class SettingsKeys(
    val category: SettingsCategories,
    val type: SettingsTypes,
    val default: String,
    val limits: List<String>,
    val resId: Int
) {
    SHOW_PLACEHOLDERS(
        SettingsCategories.DISPLAY,
        SettingsTypes.BOOLEAN,
        "true",
        emptyList(),
        R.string.s_display_placeholders
    ),
    SORT_TYPE(
        SettingsCategories.USER,
        SettingsTypes.CHOICE,
        SortType.TIMESTAMP_MODIFIED.name,
        SortType.entries.map { it.name },
        R.string.s_user_default_sort_type
    ),
    SORT_ORDER(
        SettingsCategories.USER,
        SettingsTypes.CHOICE,
        SortOrder.DESCENDING.name,
        SortOrder.entries.map { it.name },
        R.string.s_user_default_sort_order
    ),
    FILTER(
        SettingsCategories.USER,
        SettingsTypes.MULTIPLE_CHOICE,
        "",
        FilterFlag.entries.map { it.name },
        R.string.s_user_default_filter
    )
}