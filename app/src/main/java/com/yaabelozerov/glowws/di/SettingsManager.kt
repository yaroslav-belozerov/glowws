package com.yaabelozerov.glowws.di

import android.util.Log
import com.squareup.moshi.Moshi
import com.yaabelozerov.glowws.data.local.datastore.SettingsDefaults
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsModel
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.ui.model.SortModel
import kotlinx.coroutines.flow.first

class SettingsManager(
    private val dataStoreManager: AppModule.DataStoreManager, private val moshi: Moshi
) {
    private val ad = moshi.adapter(SettingsList::class.java)

    suspend fun fetchSettings(
        default: SettingsList = SettingsDefaults.DEFAULT
    ): SettingsList {
        val settingsData = dataStoreManager.getSettings().first()

        val settings = if (settingsData.isNotBlank()) {
            val s = ad.fromJson(settingsData)!!
            if (default.list!!.map { it.key!! }.toSet() == s.list!!.map { it.key!! }
                    .toSet()) s else default
        } else default

        setSettings(settings)
        return settings
    }

    private suspend fun setSettings(settings: SettingsList) {
        dataStoreManager.setSettings(ad.toJson(settings))
    }

    suspend fun modifySetting(key: SettingsKeys, value: String) {
        val settings = ad.fromJson(
            dataStoreManager.getSettings().first()
        )!!.list!!.map { if (it.key!! == key) it.copy(value = value) else it }
        setSettings(SettingsList(settings))
    }
}