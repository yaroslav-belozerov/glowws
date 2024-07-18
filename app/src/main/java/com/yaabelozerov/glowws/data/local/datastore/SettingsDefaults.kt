package com.yaabelozerov.glowws.data.local.datastore

import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsCategories
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsModel
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsTypes
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType

enum class SettingsKeys(val resId: Int) {
    SHOW_PLACEHOLDERS(R.string.s_display_placeholders), SORT_TYPE(R.string.s_user_default_sort_type), SORT_ORDER(
        R.string.s_user_default_sort_order
    ),
    FILTER(R.string.s_user_default_filter)
}

class SettingsDefaults {
    companion object {
        val DEFAULT = SettingsList(
            list = listOf(
                SettingsModel(SettingsKeys.SORT_TYPE,
                    SettingsCategories.USER,
                    SettingsTypes.CHOICE,
                    SortType.TIMESTAMP_MODIFIED.name,
                    choices = SortType.entries.map { it.name }),
                SettingsModel(
                    SettingsKeys.SORT_ORDER,
                    SettingsCategories.USER,
                    SettingsTypes.CHOICE,
                    SortOrder.ASCENDING.name,
                    choices = SortOrder.entries.map { it.name }
                ),
                SettingsModel(SettingsKeys.FILTER,
                    SettingsCategories.USER,
                    SettingsTypes.MULTIPLE_CHOICE,
                    "",
                    choices = FilterFlag.entries.map { it.name }),
                SettingsModel(
                    SettingsKeys.SHOW_PLACEHOLDERS,
                    SettingsCategories.DISPLAY,
                    SettingsTypes.BOOLEAN,
                    "true"
                )
            )
        )
    }
}