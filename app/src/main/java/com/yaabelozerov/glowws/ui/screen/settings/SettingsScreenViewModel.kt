package com.yaabelozerov.glowws.ui.screen.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.yaabelozerov.glowws.data.local.datastore.SettingsDefaults
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.di.AppModule
import com.yaabelozerov.glowws.domain.model.SettingDomainMapper
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val dataStoreManager: AppModule.DataStoreManager, private val moshi: Moshi
) : ViewModel() {
    private val _state: MutableStateFlow<Map<String, List<SettingDomainModel>>> =
        MutableStateFlow(emptyMap())
    val state = _state.asStateFlow()
    val mapper = SettingDomainMapper()

    fun getSettings(
        defaultDisplay: SettingsList = SettingsDefaults.DISPLAY_SETTINGS,
        defaultUser: SettingsList = SettingsDefaults.USER_SETTINGS
    ) {
        viewModelScope.launch {
            val display =
                dataStoreManager.getDisplay().first().also { Log.i("displaySettings", it) }
            val user = dataStoreManager.getUser().first().also { Log.i("userSettings", it) }
            val ad = moshi.adapter(SettingsList::class.java)
            setSettings(
                if (display.isNotBlank()) {
                    val s = ad.fromJson(display)!!
                    if (defaultDisplay.list!!.map { it.key!! }.toSet() == s.list!!.map { it.key!! }.toSet()) s else defaultDisplay
                } else {
                    defaultDisplay
                }, if (user.isNotBlank()) {
                    val s = ad.fromJson(user)!!
                    if (defaultUser.list!!.map { it.key!! }.toSet() == s.list!!.map { it.key!! }.toSet()) s else defaultUser
                } else {
                    defaultUser
                }
            )
        }
    }

    private fun setSettings(display: SettingsList, user: SettingsList) {
        viewModelScope.launch {
            val ad = moshi.adapter(SettingsList::class.java)
            dataStoreManager.setDisplay(ad.toJson(display))
            dataStoreManager.setUser(ad.toJson(user))
            _state.update {
                mapper.toDomainModel(display) + mapper.toDomainModel(user)
            }
        }
    }

    fun modifySetting(key: SettingsKeys, value: String) {
        viewModelScope.launch {
            val ad = moshi.adapter(SettingsList::class.java)
            val display = ad.fromJson(
                dataStoreManager.getDisplay().first()
            )!!.list!!.map { if (it.key!! == key) it.copy(value = value) else it }
            val user = ad.fromJson(
                dataStoreManager.getUser().first()
            )!!.list!!.map { if (it.key!! == key) it.copy(value = value) else it }

            setSettings(SettingsList(display), SettingsList(user))
        }
    }
}