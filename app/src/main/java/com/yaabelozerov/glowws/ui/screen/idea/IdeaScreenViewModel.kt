package com.yaabelozerov.glowws.ui.screen.idea

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.Net
import com.yaabelozerov.glowws.data.InferenceRepository
import com.yaabelozerov.glowws.data.local.media.MediaManager
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.PointType
import com.yaabelozerov.glowws.di.AppModule
import com.yaabelozerov.glowws.domain.model.PointCreateRequest
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import com.yaabelozerov.glowws.domain.model.PointMainUpdateRequest
import com.yaabelozerov.glowws.domain.model.PointModel
import com.yaabelozerov.glowws.domain.model.PointUpdateRequest
import com.yaabelozerov.glowws.domain.model.Prompt
import com.yaabelozerov.glowws.domain.model.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.call.body
import io.ktor.client.request.cookie
import io.ktor.client.request.forms.FormPart
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.append
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.PartData
import io.ktor.http.headers
import io.ktor.http.headersOf
import io.ktor.http.parameters
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.streams.asInput
import io.ktor.utils.io.streams.inputStream
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray

@HiltViewModel
class IdeaScreenViewModel
@Inject constructor(
  private val dao: IdeaDao,
  @ApplicationContext private val app: Context,
  private val mediaManager: MediaManager,
  private val inferenceRepository: InferenceRepository,
  private val dataStoreManager: AppModule.DataStoreManager
) : ViewModel() {
  private val instanceUrl = dataStoreManager.instanceUrl()
  private val jwt = dataStoreManager.jwt()

  private val _points = MutableStateFlow(emptyList<PointDomainModel>())
  val points = _points.asStateFlow()

  private val _saved = MutableStateFlow(Pair(-1L, 0L))
  private var currentJob: Job? = null

  private val _onPickMedia: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)

  fun setOnPickMedia(onPickMedia: () -> Unit) = _onPickMedia.update { onPickMedia }

  fun refreshPoints(ideaId: Long) {
    if (ideaId == -1L) {
      _points.update { emptyList() }
      return
    }
    viewModelScope.launch {
      Net.get<List<PointModel>>(instanceUrl, "points/$ideaId", jwt.first()).onSuccess { newPts ->
        _points.update { newPts.toDomain(instanceUrl.first()) }
      }.onFailure(Throwable::printStackTrace)
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
        is IdeaScreenEvent.ToggleMain -> modifyPoint(event.id, null, event.isMain)
        is IdeaScreenEvent.ExecutePointNew -> generateResponseNew(event.index, event.prompt, ideaId)
        is IdeaScreenEvent.ExecuteCancel -> {
          currentJob?.cancel()
          inferenceRepository.interrupt(null)
        }
      }
    } catch (e: Exception) {
      onError(e)
    }
  }

  private fun addPointAtIndex(
    pointType: PointType, ideaId: Long, index: Long, content: String = "", textCallback: (Long) -> Unit = {}
  ) {
    viewModelScope.launch {
      when (pointType) {
        PointType.TEXT -> {
          Net.post<PointModel, _>(
            instanceUrl, "points", jwt.first(), PointCreateRequest(
              parentId = ideaId, index = index, content = content, type = pointType.name
            )
          ).onSuccess {
            refreshPoints(ideaId)
            textCallback(it.id)
          }.onFailure(Throwable::printStackTrace)
        }

        PointType.IMAGE -> {
          addImage(
            ideaId = ideaId, index = index
          )
        }
      }
    }
  }

  private fun modifyPoint(
    pointId: Long, content: String? = null, isMain: Boolean, callback: () -> Unit = {}
  ) {
    viewModelScope.launch {
      (content?.let { newContent ->
        Net.put<List<PointModel>, _>(
          instanceUrl, "points/$pointId", jwt.first(), PointUpdateRequest(newContent, isMain)
        )
      } ?: Net.put<List<PointModel>, _>(
        instanceUrl, "points/$pointId/main", jwt.first(), PointMainUpdateRequest(isMain)
      )).onSuccess { newPts ->
          _points.update { newPts.toDomain(instanceUrl.first()) }
          callback()
        }.onFailure(Throwable::printStackTrace)
    }
  }

  private fun removePoint(pointId: Long) {
    viewModelScope.launch {
      Net.delete<List<PointModel>>(instanceUrl, "points/$pointId", jwt.first()).onSuccess { newPts ->
        _points.update { newPts.toDomain(instanceUrl.first()) }
      }.onFailure(Throwable::printStackTrace)
    }
  }

  private fun addImage(ideaId: Long, index: Long) {
    _saved.update { Pair(ideaId, index) }
    _onPickMedia.value?.invoke()
  }

  @OptIn(InternalAPI::class)
  @SuppressLint("Range")
  suspend fun importImage(uri: Uri) {
    mediaManager.importMedia(uri) {
      viewModelScope.launch {
        runCatching {
          var fileNameQueried: String? = null
          app.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
              fileNameQueried = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
          }
          fileNameQueried?.let { fileName ->
            app.contentResolver.openInputStream(uri)?.toByteReadChannel()?.let { input ->
              val token = jwt.first()
              val multipart =
                MultiPartFormDataContent(
                  listOf(
                    PartData.FileItem({ input }, {}, headers {
                      append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                      append(HttpHeaders.ContentType, "image/jpeg")
                    }),
                    PartData.FormItem(fileName, {}, headers {
                      append(HttpHeaders.ContentDisposition, "form-data; name=\"filename\";")
                    }),
                  ))
              Net.httpClient.post("${instanceUrl.first()}/upload") {
                setBody(multipart)
                cookie("auth", token)
              }.body<String>()

            }
          } ?: return@launch
        }.onSuccess {
          Net.post<PointModel, _>(
            instanceUrl, "points", jwt.first(), PointCreateRequest(
              parentId = _saved.value.first, index = _saved.value.second, content = it, type = PointType.IMAGE.name
            )
          ).onSuccess {
            refreshPoints(_saved.value.first)
          }.onFailure(Throwable::printStackTrace)
        }.onFailure(Throwable::printStackTrace)
      }
    }
  }

  private fun generateResponse(s: Prompt, contentStrings: List<String>, pointId: Long) {
    currentJob = viewModelScope.launch {
      Log.i("generateResponse", "Generating ${s.name} with $contentStrings")
//      inferenceRepository.generate(s, contentStrings, onUpdate = { modifyPoint(pointId, it) }, pointId, onErr = {
//        currentJob?.cancel()
//        inferenceRepository.deactivate()
//      })
    }
  }

  private fun generateResponseNew(index: Int, prompt: Prompt, ideaId: Long) {
    addPointAtIndex(PointType.TEXT, ideaId, index.toLong()) { pointId ->
      generateResponse(
        prompt, when (prompt) {
        Prompt.FillIn -> listOf(points.value[index - 1].content, points.value[index + 1].content)
        Prompt.Summarize, Prompt.Continue -> points.value.filterIndexed { i, it -> i < index && it.type == PointType.TEXT }
          .map { it.content }

        else -> error("Unknown prompt type for new point")
      }, pointId)
    }
  }
}
