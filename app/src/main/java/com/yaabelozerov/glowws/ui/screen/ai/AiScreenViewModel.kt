package com.yaabelozerov.glowws.ui.screen.ai

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.Net
import com.yaabelozerov.glowws.data.InferenceRepository
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelDao
import com.yaabelozerov.glowws.data.local.room.ModelType
import com.yaabelozerov.glowws.di.AppModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AiScreenViewModel
@Inject
constructor(
  private val inferenceRepository: InferenceRepository,
  private val modelDao: ModelDao,
  private val dataStoreManager: AppModule.DataStoreManager
) : ViewModel() {

  private val _onPickModel: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)

  fun setOnPickModel(onPickModel: () -> Unit) = _onPickModel.update { onPickModel }

  val jwt = dataStoreManager.jwt()
  val instanceUrl = dataStoreManager.instanceUrl()

  val aiStatus = inferenceRepository.state

  init {
    viewModelScope.launch {
      modelDao.getLastActiveModel()?.let { last ->
        inferenceRepository.loadModel(last) {
          modelDao.insertModel(last.copy(isChosen = true))
        }
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
        inferenceRepository.loadModel(
          model =
            Model(
              initialName, ModelType.OnDevice, it.split("/").last().removeSuffix(".bin"), it, true
            )
        )
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
      inferenceRepository.removeModel(model)
      modelDao.deleteModel(model)
      refresh()
    }
  }

  fun refresh() {
    viewModelScope.launch(Dispatchers.IO) {
      Net.get<List<String>>(instanceUrl, "models", jwt.first()).onSuccess { models ->
        models.map {
          Model(
            initialName = it,
            type = ModelType.Downloadable(false),
            name = it,
            path = it,
            isChosen = false
          )
        }.forEach { modelDao.insertModel(it) }
      }.onFailure {
        it.printStackTrace()
      }
      modelDao.getAllModels().collect(inferenceRepository::setModels)
    }
  }

  fun unloadModel() {
    viewModelScope.launch {
      aiStatus.value.selected?.let {
        modelDao.upsertModel(it.copy(isChosen = false))
        inferenceRepository.unloadModel()
        refresh()
      }
    }
  }

  fun editModel(model: Model) {
    viewModelScope.launch {
      modelDao.upsertModel(model)
      if (model.type != ModelType.OnDevice) {
        modelDao.getLastActiveModel()?.let { last ->
          inferenceRepository.loadModel(last) { modelDao.upsertModel(last.copy(isChosen = true)) }
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

