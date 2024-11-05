package com.yaabelozerov.glowws.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.collect.ImmutableMap
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.remote.FeedbackDTO
import com.yaabelozerov.glowws.data.remote.FeedbackService
import com.yaabelozerov.glowws.di.AppModule
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsScreenViewModel
@Inject
constructor(
    private val settingsManager: SettingsManager,
    private val settingsMapper: SettingsMapper,
    private val feedbackService: FeedbackService,
    private val dataStoreManager: AppModule.DataStoreManager
) : ViewModel() {
  private val _state: MutableStateFlow<ImmutableMap<SettingsKeys, SettingDomainModel>> =
      MutableStateFlow(ImmutableMap.of())
  val state = _state.asStateFlow()

  private val _settingsChanged = MutableStateFlow(false)
  val settingsChanged = _settingsChanged.asStateFlow()

  private val default =
      ImmutableMap.copyOf(
          settingsMapper.toDomainModel(settingsMapper.matchSettingsSchema(SettingsList())))

  init {
    viewModelScope.launch { fetchSettings()
      _settingsChanged.update { state.value != default }
    }
  }

  private suspend fun fetchSettings() {
    _state.update {
      ImmutableMap.copyOf(settingsMapper.toDomainModel(settingsManager.fetchSettings()))
    }
  }

  fun modifySetting(key: SettingsKeys, value: String, callback: () -> Unit = {}) {
    viewModelScope.launch {
      settingsManager.modifySetting(key, value)
      fetchSettings()
      _settingsChanged.update { state.value != default }
      callback()
    }
  }

  fun sendFeedback(feedback: FeedbackDTO) {
    viewModelScope.launch { feedbackService.sendFeedback(feedback) }
  }

  fun resetSettings() {
    viewModelScope.launch {
      _settingsChanged.update { false }
      dataStoreManager.setSettings("")
      fetchSettings()
    }
  }
}
