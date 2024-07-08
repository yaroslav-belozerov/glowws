package com.yaabelozerov.glowws.domain.mapper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsCategories
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsTypes
import com.yaabelozerov.glowws.domain.model.BooleanSettingDomainModel
import com.yaabelozerov.glowws.domain.model.DoubleSettingDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.domain.model.StringSettingDomainModel

class SettingsMapper {
    fun toDomainModel(settings: SettingsList): Map<Pair<String, ImageVector>, List<SettingDomainModel>> {
        val mp: MutableMap<Pair<String, ImageVector>, List<SettingDomainModel>> = mutableMapOf()
        settings.list?.forEach {
            it.category?.let { category ->
                val cat = when (category) {
                    SettingsCategories.DISPLAY -> "Display"
                    SettingsCategories.USER -> "User"
                }
                val icon = when (category) {
                    SettingsCategories.DISPLAY -> Icons.Default.Star
                    SettingsCategories.USER -> Icons.Default.Person
                }
                val dm = when (it.type) {
                    SettingsTypes.BOOLEAN -> BooleanSettingDomainModel(
                        it.key!!, it.key.name, it.value == "true"
                    )

                    SettingsTypes.DOUBLE -> DoubleSettingDomainModel(
                        it.key!!, it.key.name, it.min!!, it.max!!, it.value!!.toDouble()
                    )

                    SettingsTypes.STRING -> StringSettingDomainModel(
                        it.key!!, it.key.name, it.value!!
                    )

                    null -> StringSettingDomainModel(it.key!!, it.key.name, "Unknown setting type")
                }
                mp[Pair(cat, icon)] = mp[Pair(cat, icon)]?.plus(dm) ?: listOf(dm)
            }
        }
        return mp
    }
}