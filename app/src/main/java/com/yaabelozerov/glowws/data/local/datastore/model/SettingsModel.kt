package com.yaabelozerov.glowws.data.local.datastore.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys

enum class SettingsCategories {
    DISPLAY, USER
}

enum class SettingsTypes {
    BOOLEAN, DOUBLE, STRING, CHOICE
}

@JsonClass(generateAdapter = true)
data class SettingsModel(
    val key: SettingsKeys? = null,
    val category: SettingsCategories? = null,
    val type: SettingsTypes? = null,
    val value: String? = null,
    val default: String? = null,
    val min: Double? = null,
    val max: Double? = null,
    val choices: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SettingsList(
    @Json(name = "list") val list: List<SettingsModel>? = null
)