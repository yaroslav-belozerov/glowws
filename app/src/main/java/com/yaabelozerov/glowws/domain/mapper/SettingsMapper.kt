package com.yaabelozerov.glowws.domain.mapper

import android.util.Log
import androidx.compose.ui.util.fastJoinToString
import com.yaabelozerov.glowws.Const.String.JSON_DELIMITER
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
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
import com.yaabelozerov.glowws.ui.model.SortModel
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType

class SettingsMapper {
  fun toDomainModel(settings: SettingsList): Map<SettingsKeys, SettingDomainModel> {
    return settings.list
        ?.mapNotNull { it.key?.let { k -> k to asDomainModel(it) } }
        ?.toMap()
        ?.filterValues { it != null }
        ?.mapValues { it.value as SettingDomainModel }
        .orEmpty()
  }

  private fun asDomainModel(k: SettingsModel): SettingDomainModel? {
    return try {
      when (k.key?.type) {
        SettingsTypes.BOOLEAN -> BooleanSettingDomainModel(k.key, k.value == "true")

        SettingsTypes.DOUBLE ->
            if (k.limits != null && k.value != null) {
              DoubleSettingDomainModel(
                  k.key, k.limits[0].toDouble(), k.limits[1].toDouble(), k.value.toDouble())
            } else {
              null
            }

        SettingsTypes.STRING -> k.value?.let { StringSettingDomainModel(k.key, k.value) }

        SettingsTypes.CHOICE ->
            if (k.limits != null && k.value != null) {
              ChoiceSettingDomainModel(
                  key = k.key,
                  choices = k.limits,
                  localChoicesIds = k.limits.map { s -> getLocalChoice(s) },
                  value = k.value)
            } else {
              null
            }

        SettingsTypes.MULTIPLE_CHOICE ->
            if (k.limits != null && k.value != null) {
              MultipleChoiceSettingDomainModel(
                  key = k.key,
                  choices = k.limits,
                  localChoicesIds = k.limits.map { s -> getLocalChoice(s) },
                  value = k.value.split(JSON_DELIMITER).map { s -> s == "true" })
            } else {
              null
            }
        null -> null
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun getSorting(settings: SettingsList): SortModel {
    val order =
        try {
          settings.list
              ?.findLast { it.key == SettingsKeys.SORT_ORDER }
              ?.value
              ?.let { SortOrder.valueOf(it) } ?: SortOrder.ASCENDING
        } catch (_: Exception) {
          SortOrder.ASCENDING
        }
    val type =
        try {
          settings.list
              ?.findLast { it.key == SettingsKeys.SORT_TYPE }
              ?.value
              ?.let { SortType.valueOf(it) } ?: SortType.TIMESTAMP_MODIFIED
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

  private fun getLocalChoice(s: String): Int? =
      try {
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
    for (i in new.filterNotNull()) {
      newList.add(SettingsModel(i, i.default, i.limits))
    }
    return SettingsList(newList)
  }
}
