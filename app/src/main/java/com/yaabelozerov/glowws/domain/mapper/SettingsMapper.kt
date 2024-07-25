package com.yaabelozerov.glowws.domain.mapper

import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.fastJoinToString
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
    fun toDomainModel(settings: SettingsList): Map<SettingsCategories, List<SettingDomainModel>> {
        val mp: MutableMap<SettingsCategories, List<SettingDomainModel>> = mutableMapOf()
        settings.list!!.forEach {
            it.category?.let { category ->
                val cat = category.resId
                val icon = category.icon
                val nameRes = it.key?.resId ?: -1
                val dm = when (it.type) {
                    SettingsTypes.BOOLEAN -> BooleanSettingDomainModel(
                        it.key!!,
                        nameRes,
                        it.value == "true"
                    )

                    SettingsTypes.DOUBLE -> DoubleSettingDomainModel(
                        it.key!!,
                        nameRes,
                        it.min!!,
                        it.max!!,
                        it.value!!.toDouble()
                    )

                    SettingsTypes.STRING -> StringSettingDomainModel(
                        it.key!!,
                        nameRes,
                        it.value!!
                    )

                    SettingsTypes.CHOICE -> {
                        ChoiceSettingDomainModel(
                            key = it.key!!,
                            nameRes = nameRes,
                            choices = it.choices!!,
                            localChoicesIds = it.choices!!.map { getLocalChoice(it) },
                            value = it.value!!
                        )
                    }

                    SettingsTypes.MULTIPLE_CHOICE -> MultipleChoiceSettingDomainModel(
                        key = it.key!!,
                        nameRes = nameRes,
                        choices = it.choices!!,
                        localChoicesIds = it.choices!!.map { getLocalChoice(it) },
                        value = it.value!!.split(",").map { it == "true" }
                    )

                    null -> StringSettingDomainModel(
                        it.key!!,
                        nameRes,
                        ""
                    )
                }
                mp[category] = mp[category]?.plus(dm) ?: listOf(dm)
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

    fun getFilter(settings: SettingsList): FilterModel {
        val split = settings.list?.findLast { it.key == SettingsKeys.FILTER }?.value?.split(",")!!
        val mp = mutableMapOf<FilterFlag, Boolean>()
        FilterFlag.entries.forEachIndexed { index, filterFlag ->
            mp[filterFlag] = split[index] == "true"
        }
        return FilterModel(mp)
    }

    fun getLocalChoice(s: String): Int? = try {
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
}
