package com.yaabelozerov.glowws.domain.mapper

import android.util.Log
import androidx.compose.ui.util.fastJoinToString
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsCategories
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsModel
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
import com.yaabelozerov.glowws.util.JSON_DELIMITER

class SettingsMapper {
    fun toDomainModel(settings: SettingsList): Map<SettingsCategories, List<SettingDomainModel>> {
        val mp: MutableMap<SettingsCategories, List<SettingDomainModel>> = mutableMapOf()
        settings.list!!.forEach {
            it.key?.let { k ->
                val dm = when (k.type) {
                    SettingsTypes.BOOLEAN -> BooleanSettingDomainModel(
                        k, it.value == "true"
                    )

                    SettingsTypes.DOUBLE -> DoubleSettingDomainModel(
                        k, it.limits!![0].toDouble(), it.limits[1].toDouble(), it.value!!.toDouble()
                    )

                    SettingsTypes.STRING -> StringSettingDomainModel(
                        k, it.value!!
                    )

                    SettingsTypes.CHOICE -> ChoiceSettingDomainModel(
                        key = k,
                        choices = it.limits!!,
                        localChoicesIds = it.limits.map { s -> getLocalChoice(s) },
                        value = it.value!!
                    )

                    SettingsTypes.MULTIPLE_CHOICE -> MultipleChoiceSettingDomainModel(key = k,
                        choices = it.limits!!,
                        localChoicesIds = it.limits.map { s -> getLocalChoice(s) },
                        value = it.value!!.split(JSON_DELIMITER).map { s -> s == "true" })
                }
                mp[k.category] = mp[k.category]?.plus(dm) ?: listOf(dm)
            }
        }
        return mp
    }

    fun getSorting(settings: SettingsList): SortModel {
        val order = try {
            SortOrder.valueOf(settings.list?.findLast { it.key == SettingsKeys.SORT_ORDER }?.value!!)
        } catch (_: Exception) {
            SortOrder.ASCENDING
        }
        val type = try {
            SortType.valueOf(settings.list?.findLast { it.key == SettingsKeys.SORT_TYPE }?.value!!)
        } catch (_: Exception) {
            SortType.TIMESTAMP_MODIFIED
        }
        return SortModel(order, type)
    }

//    fun getFilter(settings: SettingsList): FilterModel {
//        val split = settings.list?.findLast { it.key == SettingsKeys.FILTER }?.value?.split(
//            JSON_DELIMITER
//        )!!
//        val mp = mutableMapOf<FilterFlag, Boolean>()
//        FilterFlag.entries.forEachIndexed { index, filterFlag ->
//            mp[filterFlag] = split[index] == "true"
//        }
//        return FilterModel(mp)
//    }

    private fun getLocalChoice(s: String): Int? = try {
        when (s) {
            in SortOrder.entries.fastJoinToString() -> SortOrder.valueOf(s).resId
            in SortType.entries.fastJoinToString() -> SortType.valueOf(s).resId
            in FilterFlag.entries.fastJoinToString() -> FilterFlag.valueOf(s).resId
            else -> null
        }
    } catch (_: Exception) {
        Log.i("SettingsMapper", "Locale not found for key: $s")
        null
    }

    fun matchSettingsSchema(list: SettingsList): SettingsList {
        val newList = list.list?.toMutableList() ?: mutableListOf()
        newList.removeAll { it.key == null }
        val allKeys = SettingsKeys.entries.toSet()
        val hasKeys = newList.map { it.key }.toSet()
        val new = allKeys - hasKeys
        for (i in new) {
            newList.add(SettingsModel(i!!, i.default, i.limits))
        }
        return SettingsList(newList)
    }
}
