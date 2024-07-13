package com.yaabelozerov.glowws.domain.mapper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsCategories
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsTypes
import com.yaabelozerov.glowws.domain.model.BooleanSettingDomainModel
import com.yaabelozerov.glowws.domain.model.ChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.DoubleSettingDomainModel
import com.yaabelozerov.glowws.domain.model.MultipleChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.domain.model.StringSettingDomainModel
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.FilterModel
import com.yaabelozerov.glowws.ui.model.SortModel
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType

class SettingsMapper {
    fun toDomainModel(settings: SettingsList): Map<Pair<String, ImageVector>, List<SettingDomainModel>> {
        val mp: MutableMap<Pair<String, ImageVector>, List<SettingDomainModel>> = mutableMapOf()
        settings.list!!.forEach {
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

                    SettingsTypes.CHOICE -> ChoiceSettingDomainModel(
                        it.key!!, it.key.name, it.choices!!, it.value!!
                    )

                    SettingsTypes.MULTIPLE_CHOICE -> MultipleChoiceSettingDomainModel(it.key!!,
                        it.key.name!!,
                        it.choices!!,
                        it.value!!.split(",").map { it == "true" })

                    null -> StringSettingDomainModel(
                        it.key!!, it.key.name, "Unknown setting type"
                    )
                }
                mp[Pair(cat, icon)] = mp[Pair(cat, icon)]?.plus(dm) ?: listOf(dm)
            }
        }
        return mp
    }

    fun getSorting(settings: SettingsList): SortModel {
        val split = settings.list?.findLast { it.key == SettingsKeys.SORT }?.value?.split(",")
        val order = when (split?.first()) {
            SortOrder.ASCENDING.name -> SortOrder.ASCENDING
            SortOrder.DESCENDING.name -> SortOrder.DESCENDING
            else -> SortOrder.ASCENDING
        }
        val type = when (split?.last()) {
            SortType.ALPHABETICAL.name -> SortType.ALPHABETICAL
            SortType.TIMESTAMP_CREATED.name -> SortType.TIMESTAMP_CREATED
            SortType.TIMESTAMP_MODIFIED.name -> SortType.TIMESTAMP_MODIFIED
            else -> SortType.ALPHABETICAL
        }
        return SortModel(order, type)
    }

    fun getFilter(settings: SettingsList): FilterModel {
        val split = settings.list?.findLast { it.key == SettingsKeys.FILTER }?.value?.split(",")!!
        val mp = mutableMapOf<FilterFlag, Boolean>()
        FilterFlag.entries.forEachIndexed { index, filterFlag ->
            mp[filterFlag] = split[index] == "true"
        }
        return FilterModel(mp)
    }
}