package com.yaabelozerov.glowws.ui.screen.ai

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.ai.InferenceManager
import com.yaabelozerov.glowws.di.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiScreenViewModel @Inject constructor(val inferenceManager: InferenceManager, private val settingsManager: SettingsManager): ViewModel() {
    val onPickModel: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)

    fun executeInto(prompt: String, callback: (String) -> Unit) {
        inferenceManager.executeInto(prompt, callback)
    }

    fun importModel() {
        onPickModel.value?.invoke()
    }

    fun refresh() {
        inferenceManager.checkPath()
    }
}