package com.yaabelozerov.glowws.data.local.datastore

import com.yaabelozerov.glowws.data.local.datastore.model.SettingsCategories
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsModel
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsTypes
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.SortType

enum class SettingsKeys {
    SHOW_PROJECT_EMPTY_NAME, SORT, FILTER
}

class SettingsDefaults {
    companion object {
        val DEFAULT = SettingsList(list = listOf(SettingsModel(SettingsKeys.SORT,
            SettingsCategories.USER,
            SettingsTypes.CHOICE,
            SortType.TIMESTAMP_MODIFIED.name,
            choices = SortType.entries.map { it.toString() }), SettingsModel(
            SettingsKeys.FILTER, SettingsCategories.USER, SettingsTypes.MULTIPLE_CHOICE, "", choices = FilterFlag.entries.map { it.toString() }
        ), SettingsModel(
            SettingsKeys.SHOW_PROJECT_EMPTY_NAME,
            SettingsCategories.DISPLAY,
            SettingsTypes.BOOLEAN,
            "true"
        )))
    }
}