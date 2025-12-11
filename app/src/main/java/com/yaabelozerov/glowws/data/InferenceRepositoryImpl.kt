package com.yaabelozerov.glowws.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.yaabelozerov.glowws.Net
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.ai.InferenceManager
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelDao
import com.yaabelozerov.glowws.data.local.room.ModelVariant
import com.yaabelozerov.glowws.di.AppModule
import com.yaabelozerov.glowws.domain.InferenceRepository
import com.yaabelozerov.glowws.domain.model.Prompt
import com.yaabelozerov.glowws.queryName
import io.ktor.client.request.cookie
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.headers
import io.ktor.utils.io.ByteReadChannel
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoField
import kotlin.onSuccess

class InferenceRepositoryImpl(
    private val localInferenceManager: InferenceManager,
    private val app: Context,
    private val dataStoreManager: AppModule.DataStoreManager,
    private val modelDao: ModelDao,
) : InferenceRepository {
  private val _source =
      MutableStateFlow<Triple<Model?, InferenceManagerState, Long>>(
          Triple(null, InferenceManagerState.IDLE, -1L))
  override val source = _source.asStateFlow()

  private val instanceUrl = dataStoreManager.instanceUrl()
  private val jwt = dataStoreManager.jwt()

  private val _response = MutableStateFlow("")

  override fun deactivate() {
    _source.update { it.copy(second = InferenceManagerState.ACTIVE, third = -1L) }
  }

  override suspend fun generate(
      prompt: Prompt,
      contentStrings: List<String>,
      onUpdate: (String) -> Unit,
      pointId: Long,
      onErr: () -> Unit
  ) {
    _source.update { it.copy(second = InferenceManagerState.RESPONDING, third = pointId) }
    _response.update { "" }
    try {
      when (_source.value.first?.type) {
        ModelVariant.ONDEVICE, ModelVariant.DOWNLOADABLE -> {
          val message =
              when (prompt) {
                Prompt.FillIn ->
                    app.resources.getString(R.string.prompt_local_prepend) +
                        "system: ${app.resources.getString(prompt.prompt)}\nuser: ${contentStrings[0]}\nuser: ${contentStrings[1]}\nassistant:"

                Prompt.Rephrase ->
                    app.resources.getString(R.string.prompt_local_prepend) +
                        "system: ${app.resources.getString(prompt.prompt)}\nuser: ${contentStrings[0]}\nassistant:"

                Prompt.Summarize,
                Prompt.Continue ->
                    app.resources.getString(R.string.prompt_local_prepend) +
                        "system: ${app.resources.getString(prompt.prompt)}\nuser: ${contentStrings.joinToString("\nuser: ")}\nassistant:"
              }
          localInferenceManager.execute(message, onUpdate) {
            _source.update { it.copy(second = InferenceManagerState.ACTIVE, third = -1L) }
          }
        }
        null -> TODO()
      }
    } catch (e: Exception) {
      interrupt(e)
      onErr()
    }
  }

  override fun interrupt(e: Exception?) {
    e?.let { Toast.makeText(app, it.localizedMessage, Toast.LENGTH_SHORT).show() }
    _source.update { it.copy(second = InferenceManagerState.ACTIVE, third = -1L) }
  }

  override suspend fun loadModel(model: Model, callback: suspend () -> Unit) {
    _source.update { it.copy(model, InferenceManagerState.ACTIVATING) }
    when (model.type) {
      ModelVariant.ONDEVICE ->
          model.path?.let {
            localInferenceManager.activateModel(model.path) {
              _source.update { src -> src.copy(model, InferenceManagerState.ACTIVE) }
            }
          }

      ModelVariant.DOWNLOADABLE -> {
        if (model.path != null && model.name != null) {
          val file = File(model.path)
          if (!file.exists()) {
            runCatching {
              Net.httpClient.prepareGet("${instanceUrl.first()}/download") {
                parameter("file", model.name)
                cookie("auth", jwt.first())
              }.execute { httpResponse ->
                val channel = httpResponse.bodyAsChannel()
                localInferenceManager.importModel(model.name, channel, httpResponse.contentLength()) {
                  modelDao.upsertModel(model.copy(path = it))
                  _source.update { src -> src.copy(first = model, second = InferenceManagerState.ACTIVE) }
                }
              }
            }.onSuccess {
              println("SUCCESS!!!")
            }.onFailure { it.printStackTrace() }
          }
        }
      }
    }
    callback()
  }

  override suspend fun removeModel(model: Model, stateAfter: InferenceManagerState) {
    _source.update { it.copy(second = InferenceManagerState.REMOVING) }
    when (model.type) {
      ModelVariant.ONDEVICE ->
          model.path?.let {
            localInferenceManager.removeModel(model.path) {
              _source.update {
                if (it.first != model) {
                  it.copy(second = stateAfter)
                } else {
                  it.copy(null, InferenceManagerState.IDLE)
                }
              }
            }
          }

      else -> TODO()
    }
  }

  override suspend fun addLocalModel(uri: Uri, callback: suspend (String) -> Unit) {
    _source.update {
      it.copy(
        first = Model(uri.toStrippedFileName(app) + System.currentTimeMillis().toString(), ModelVariant.ONDEVICE, uri.toStrippedFileName(app), ""),
        second = InferenceManagerState.LOADING
      )
    }
    localInferenceManager.importModel(uri, callback)
  }

  override fun unloadModel() {
    _source.update { it.copy(first = null, second = InferenceManagerState.IDLE, third = -1L) }
    localInferenceManager.unloadModel()
  }
}

fun Uri.toStrippedFileName(app: Context): String =
    queryName(app.contentResolver).split("/").last().removeSuffix(".bin")
