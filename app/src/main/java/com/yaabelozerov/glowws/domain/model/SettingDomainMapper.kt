package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.datastore.model.SettingsCategories
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsTypes

class SettingDomainMapper {
    fun toDomainModel(settings: SettingsList): Map<String, List<SettingDomainModel>> {
        val mp: MutableMap<String, List<SettingDomainModel>> = mutableMapOf()
        settings.list?.forEach {
            it.category?.let { category ->
                val cat = when (category) {
                    SettingsCategories.DISPLAY -> "Display Settings"
                    SettingsCategories.USER -> "User Settings"
                }
                val dm = when (it.type) {
                    SettingsTypes.BOOLEAN -> BooleanSettingDomainModel(it.key!!, it.key.name, it.value == "true")
                    SettingsTypes.DOUBLE -> DoubleSettingDomainModel(it.key!!, it.key.name, it.min!!, it.max!!, it.value!!.toDouble())
                    SettingsTypes.STRING -> StringSettingDomainModel(it.key!!, it.key.name, it.value!!)
                    null -> StringSettingDomainModel(it.key!!, it.key.name, "Unknown setting type")
                }
                mp[cat] = mp[cat]?.plus(dm) ?: listOf(dm)
            }
        }
        return mp
    }
}