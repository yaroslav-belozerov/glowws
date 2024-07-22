package com.yaabelozerov.glowws.data.local.datastore.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys

enum class SettingsCategories(val resId: Int, val icon: ImageVector) {
    DISPLAY(R.string.s_cat_display, Icons.Default.Star), USER(R.string.s_cat_user, Icons.Default.Person)
}

enum class SettingsTypes {
    BOOLEAN, DOUBLE, STRING, CHOICE, MULTIPLE_CHOICE
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
