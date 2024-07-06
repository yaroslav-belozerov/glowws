package com.yaabelozerov.glowws.data.local.datastore

import com.yaabelozerov.glowws.data.local.datastore.model.SettingsCategories
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsModel
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsTypes

enum class SettingsKeys {
    SHOW_PROJECT_EMPTY_NAME
}

class SettingsDefaults {
    companion object {
        val DISPLAY_SETTINGS = SettingsList(
            listOf(
                SettingsModel(
                    SettingsKeys.SHOW_PROJECT_EMPTY_NAME, SettingsCategories.DISPLAY, SettingsTypes.BOOLEAN, "true"
                )
            )
        )

        val USER_SETTINGS = SettingsList(
            listOf(
//                SettingsModel("username", SettingsCategories.USER, SettingsTypes.STRING, "user")
            )
        )
    }
}