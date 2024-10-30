package com.yaabelozerov.glowws.ui.screen.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.data.local.room.ModelDao
import com.yaabelozerov.glowws.data.local.room.ModelType
import com.yaabelozerov.glowws.domain.InferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiScreenViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    @ApplicationContext private val app: Context,
    private val inferenceRepository: InferenceRepository,
    private val modelDao: ModelDao
) : ViewModel() {
    val onPickModel: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)

    private val _models: MutableStateFlow<Map<ModelType, List<Model>>> = MutableStateFlow(emptyMap())
    val models = _models.asStateFlow()

    val aiStatus = inferenceRepository.source

    init {
        viewModelScope.launch {
            modelDao.getLastActiveModel()?.let {
                inferenceRepository.loadModel(it) {
                    modelDao.upsertModel(it.copy(isChosen = true))
                }
            }
            refresh()
        }
    }

    fun openLocalModelPicker() {
        onPickModel.value?.invoke()
    }

    fun importLocalModel(uri: Uri) {
        viewModelScope.launch {
            inferenceRepository.addLocalModel(uri) {
                modelDao.clearChosen()
                val mod = Model(
                    0,
                    ModelType.LOCAL,
                    it.split("/").last().removeSuffix(".bin"),
                    it,
                    null,
                    true
                )
                val id = modelDao.upsertModel(mod)
                inferenceRepository.loadModel(model = mod.copy(id = id))
                refresh()
            }
        }
    }

    fun pickModel(model: Model) {
        viewModelScope.launch {
            inferenceRepository.loadModel(model) {
                modelDao.clearChosen()
                modelDao.upsertModel(model.copy(isChosen = true))
            }
        }
    }

    fun removeModel(model: Model) {
        viewModelScope.launch {
            inferenceRepository.removeModel(model)
            modelDao.deleteModel(model)
            refresh()
        }
    }

    fun importRemoteModels(list: List<Model>) {
        viewModelScope.launch {
            list.forEach {
                modelDao.upsertModel(it)
            }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            modelDao.getAllModels().collect { models ->
                _models.update { models.toTypeMap() }
            }
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            aiStatus.value.first?.let {
                modelDao.upsertModel(it.copy(isChosen = false))
                inferenceRepository.unloadModel()
                settingsManager.setModelId(-1L)
                refresh()
            }
        }
    }

    fun editModel(model: Model) {
        viewModelScope.launch {
            modelDao.upsertModel(model)
            if (model.type != ModelType.LOCAL) {
                modelDao.getLastActiveModel()?.let {
                    inferenceRepository.loadModel(it) {
                        modelDao.upsertModel(it.copy(isChosen = true))
                    }
                }
            }
            refresh()
        }
    }
}

fun List<Model>.toTypeMap(): Map<ModelType, List<Model>> {
    return groupBy { it.type }
}
