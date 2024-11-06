package com.yaabelozerov.glowws.ui.screen.idea

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.media.MediaManager
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.Point
import com.yaabelozerov.glowws.data.local.room.PointType
import com.yaabelozerov.glowws.domain.InferenceRepository
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import com.yaabelozerov.glowws.domain.model.Prompt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class IdeaScreenViewModel
@Inject
constructor(
    private val dao: IdeaDao,
    private val mediaManager: MediaManager,
    private val inferenceRepository: InferenceRepository
) : ViewModel() {
  private val _points = MutableStateFlow(emptyList<PointDomainModel>())
  val points = _points.asStateFlow()

  private val _saved = MutableStateFlow(Pair(-1L, 0L))

  private val _onPickMedia: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)

  fun setOnPickMedia(onPickMedia: () -> Unit) = _onPickMedia.update { onPickMedia }

  fun refreshPoints(ideaId: Long) {
    viewModelScope.launch {
      dao.getIdeaPoints(ideaId).flowOn(Dispatchers.IO).distinctUntilChanged().collectLatest { points
        ->
        _points.update {
          points.map { pt -> PointDomainModel(pt.pointId, pt.type, pt.pointContent, pt.isMain) }
        }
      }
    }
  }

  fun onEvent(event: IdeaScreenEvent, ideaId: Long, onBack: () -> Unit, onError: (Exception) -> Unit) {
    try {
      when (event) {
        is IdeaScreenEvent.AddPoint -> addPointAtIndex(event.type, ideaId, event.index)
        is IdeaScreenEvent.ExecutePoint -> generateResponse(event.prompt, listOf(event.content), event.pointId)
        is IdeaScreenEvent.GoBack -> onBack()
        is IdeaScreenEvent.RemovePoint -> removePoint(event.id)
        is IdeaScreenEvent.SavePoint -> modifyPoint(event.id, event.text, event.isMain)
        is IdeaScreenEvent.ExecutePointNew -> generateResponseNew(event.index, event.prompt, ideaId)
      }
    } catch (e: Exception) {
      onError(e)
    }
  }

  private fun addPointAtIndex(
      pointType: PointType,
      ideaId: Long,
      index: Long,
      content: String = "",
      textCallback: (Long) -> Unit = {}
  ) {
    viewModelScope.launch {
      when (pointType) {
        PointType.TEXT -> {
          val id =
              dao.insertPointUpdateIdeaAtIndex(
                  Point(
                      pointId = 0,
                      ideaParentId = ideaId,
                      pointContent = content,
                      index = index,
                      type = pointType,
                      isMain = false))
          textCallback(id)
        }

        PointType.IMAGE -> addImage(ideaId, index)
      }
    }
  }

  private fun modifyPoint(
      pointId: Long,
      content: String? = null,
      isMain: Boolean? = null,
      callback: () -> Unit = {}
  ) {
    viewModelScope.launch {
      val point = dao.getPoint(pointId).first()
      dao.upsertPointUpdateIdea(
          point.copy(pointContent = content ?: point.pointContent, isMain = isMain ?: point.isMain))
      callback()
    }
  }

  private fun removePoint(pointId: Long) {
    viewModelScope.launch {
      val pt = dao.getPoint(pointId).first()
      val ideaId = pt.ideaParentId
      dao.deletePointAndIndex(pointId)
      dao.updateIdeaContentFromPoints(ideaId)
      if (pt.type == PointType.IMAGE) {
        mediaManager.removeMedia(pt.pointContent)
      }
    }
  }

  private fun addImage(ideaId: Long, index: Long) {
    _saved.update { Pair(ideaId, index) }
    _onPickMedia.value?.invoke()
  }

  suspend fun importImage(uri: Uri) {
    mediaManager.importMedia(uri) {
      viewModelScope.launch {
        dao.insertPointUpdateIdeaAtIndex(
            Point(
                pointId = 0,
                ideaParentId = _saved.value.first,
                pointContent = it,
                index = _saved.value.second,
                type = PointType.IMAGE,
                isMain = false))
      }
    }
  }

  private fun generateResponse(s: Prompt, contentStrings: List<String>, pointId: Long) {
    viewModelScope.launch {
      Log.i("generateResponse", "Generating ${s.name} with $contentStrings")
      inferenceRepository.generate(s, contentStrings, onUpdate = { modifyPoint(pointId, it) }, pointId)
    }
  }

  private fun generateResponseNew(index: Int, prompt: Prompt, ideaId: Long) {
    addPointAtIndex(PointType.TEXT, ideaId, index.toLong()) { pointId ->
      generateResponse(prompt, when (prompt) {
        Prompt.FillIn -> listOf(points.value[index-1].content, points.value[index+1].content)
        Prompt.Summarize, Prompt.Continue -> points.value.filterIndexed { i, it -> i < index && it.type == PointType.TEXT }.map { it.content }
        else -> error("Unknown prompt type for new point")
      }, pointId)
    }
  }
}
