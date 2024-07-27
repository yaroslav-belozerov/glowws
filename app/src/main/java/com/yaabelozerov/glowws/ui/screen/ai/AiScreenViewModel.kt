package com.yaabelozerov.glowws.ui.screen.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.ai.InferenceManager
import com.yaabelozerov.glowws.di.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiScreenViewModel @Inject constructor(val inferenceManager: InferenceManager, private val settingsManager: SettingsManager): ViewModel() {
    val onPickModel: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)

    private val _models: MutableStateFlow<List<AiModel>> = MutableStateFlow(emptyList())
    val models = _models.asStateFlow()

    init {
        viewModelScope.launch {
            val savedName = settingsManager.getModelName()
            if (savedName.isNotBlank()) {
                pickModel(savedName)
            }
            refresh()
        }
    }

    fun executeInto(prompt: String, callback: (String) -> Unit) {
        viewModelScope.launch {
            inferenceManager.executeInto(prompt, callback)
        }
    }

    fun importModel() {
        onPickModel.value?.invoke()
    }

    fun pickModel(fileName: String) {
        viewModelScope.launch {
            inferenceManager.activateModel(fileName) {
                _models.update { it.map { md -> AiModel(md.name, md.fileName, md.fileName == fileName) } }
                viewModelScope.launch { settingsManager.setModelName(fileName) }
            }
        }
    }

    fun removeModel(name: String) {
        viewModelScope.launch {
            inferenceManager.removeModel(name)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _models.update { inferenceManager.refreshModels() }
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            inferenceManager.unloadModel()
            settingsManager.setModelName("")
            refresh()
        }
    }
}