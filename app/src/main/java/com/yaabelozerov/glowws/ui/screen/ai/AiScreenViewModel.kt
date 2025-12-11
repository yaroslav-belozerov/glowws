package com.yaabelozerov.glowws.ui.screen.ai

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.Net
import com.yaabelozerov.glowws.data.local.ai.InferenceManager
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelDao
import com.yaabelozerov.glowws.data.local.room.ModelType
import com.yaabelozerov.glowws.data.local.room.ModelVariant
import com.yaabelozerov.glowws.di.AppModule
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.domain.InferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AiScreenViewModel
@Inject
constructor(
    private val settingsManager: SettingsManager,
    private val inferenceRepository: InferenceRepository,
    private val inferenceManager: InferenceManager,
    private val modelDao: ModelDao,
    private val dataStoreManager: AppModule.DataStoreManager
) : ViewModel() {

  private val _onPickModel: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)

  fun setOnPickModel(onPickModel: () -> Unit) = _onPickModel.update { onPickModel }

  private val _models: MutableStateFlow<Map<ModelType, List<Model>>> = MutableStateFlow(emptyMap())
  val models = _models.asStateFlow()

  val jwt = dataStoreManager.jwt()
  val instanceUrl = dataStoreManager.instanceUrl()

  val aiStatus = inferenceRepository.source.also { Log.i("source", it.toString()) }

  init {
    viewModelScope.launch {
      modelDao.getLastActiveModel()?.let {
        inferenceRepository.loadModel(it) { modelDao.insertModel(it.copy(isChosen = true)) }
      }
      refresh()
    }
  }

  fun openLocalModelPicker() {
    _onPickModel.value?.invoke()
  }

  fun importLocalModel(uri: Uri) {
    viewModelScope.launch {
      inferenceRepository.addLocalModel(uri) {
        modelDao.clearChosen()
        val initialName = System.currentTimeMillis().toString()
        val mod =
            Model(
                initialName, ModelVariant.ONDEVICE, it.split("/").last().removeSuffix(".bin"), it, true)
        inferenceRepository.loadModel(model = mod.copy(initialName = initialName))
        refresh()
      }
    }
  }

  fun pickModel(model: Model) {
    viewModelScope.launch {
      inferenceRepository.loadModel(model) {
        modelDao.clearChosen()
        modelDao.insertModel(model.copy(isChosen = true))
      }
    }
  }

  fun removeModel(model: Model) {
    viewModelScope.launch {
      if (model == aiStatus.value.first) {
        inferenceRepository.removeModel(model)
      } else {
        inferenceRepository.removeModel(model, aiStatus.value.second)
      }
      modelDao.deleteModel(model)
      refresh()
    }
  }

  private suspend fun loadRemoteModels() {
      Net.get<List<String>>(instanceUrl, "models", jwt.first()).onSuccess { models ->
        models.map { Model(
          initialName = it,
          type = ModelVariant.DOWNLOADABLE,
          name = it,
          path = it,
          isChosen = false
        ) }.forEach { modelDao.insertModel(it) }
      }.onFailure {
        it.printStackTrace()
      }
  }

  fun refresh() {
    viewModelScope.launch(Dispatchers.IO) {
      loadRemoteModels()
      modelDao.getAllModels().collect { models -> _models.update { models.toTypeMap() } }
    }
  }

  fun unloadModel() {
    viewModelScope.launch {
      aiStatus.value.first?.let {
        modelDao.insertModel(it.copy(isChosen = false))
        inferenceRepository.unloadModel()
        refresh()
      }
    }
  }

  fun editModel(model: Model) {
    viewModelScope.launch {
      modelDao.insertModel(model)
      if (model.type != ModelVariant.ONDEVICE) {
        modelDao.getLastActiveModel()?.let {
          inferenceRepository.loadModel(it) { modelDao.insertModel(it.copy(isChosen = true)) }
        }
      }
      refresh()
    }
  }

  fun onEvent(event: AiScreenEvent) {
    when (event) {
      is AiScreenEvent.Add -> openLocalModelPicker()
      is AiScreenEvent.Choose -> pickModel(event.model)
      is AiScreenEvent.Delete -> removeModel(event.model)
      is AiScreenEvent.Edit -> editModel(event.model)
      is AiScreenEvent.Refresh -> refresh()
      is AiScreenEvent.Unload -> unloadModel()
    }
  }
}

fun List<Model>.toTypeMap(): Map<ModelType, List<Model>> {
  val mp = mutableMapOf<ModelType, List<Model>>()
  forEach { model ->
    val key = ModelType.entries.find { it.variants.contains(model.type) }
    if (key != null) mp[key] = listOf(model) + mp[key].orEmpty()
  }
  return mp
}
