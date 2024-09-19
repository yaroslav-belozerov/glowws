package com.yaabelozerov.glowws.ui.screen.settings

import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsCategories
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val settingsMapper: SettingsMapper
) :
    ViewModel() {
    private val _state: MutableStateFlow<Map<SettingsCategories, List<SettingDomainModel>>> =
        MutableStateFlow(emptyMap())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.update { settingsMapper.toDomainModel(settingsManager.fetchSettings()) } }
    }

    fun modifySetting(key: SettingsKeys, value: String, callback: () -> Unit = {}) {
        viewModelScope.launch {
            settingsManager.modifySetting(key, value)
            _state.update { settingsMapper.toDomainModel(settingsManager.fetchSettings()) }
            callback()
        }
    }
}
