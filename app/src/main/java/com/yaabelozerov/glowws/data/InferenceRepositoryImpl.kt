package com.yaabelozerov.glowws.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.ai.InferenceManager
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelVariant
import com.yaabelozerov.glowws.data.remote.Content
import com.yaabelozerov.glowws.data.remote.GigaChatMessage
import com.yaabelozerov.glowws.data.remote.GigaChatMessageRequest
import com.yaabelozerov.glowws.data.remote.GigaChatService
import com.yaabelozerov.glowws.data.remote.Message
import com.yaabelozerov.glowws.data.remote.OpenRouterRequest
import com.yaabelozerov.glowws.data.remote.OpenRouterResponse
import com.yaabelozerov.glowws.data.remote.OpenRouterService
import com.yaabelozerov.glowws.data.remote.getResponse
import com.yaabelozerov.glowws.di.AppModule
import com.yaabelozerov.glowws.domain.InferenceRepository
import com.yaabelozerov.glowws.domain.model.Prompt
import com.yaabelozerov.glowws.queryName
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoField

class InferenceRepositoryImpl(
    private val localInferenceManager: InferenceManager,
    private val openRouterService: OpenRouterService,
    private val gigaChatService: GigaChatService,
    private val app: Context,
    private val moshi: Moshi,
    private val dataStoreManager: AppModule.DataStoreManager
) : InferenceRepository {
  private val _source =
      MutableStateFlow<Triple<Model?, InferenceManagerState, Long>>(
          Triple(null, InferenceManagerState.IDLE, -1L))
  override val source = _source.asStateFlow()

  private val _response = MutableStateFlow("")
  val streamingAd: JsonAdapter<OpenRouterResponse> = moshi.adapter(OpenRouterResponse::class.java)
  val regularAd: JsonAdapter<List<Content>> =
      moshi
          .adapter<List<Content>?>(
              Types.newParameterizedType(List::class.java, Content::class.java))
          .lenient()

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
    val token = _source.value.first?.token ?: ""
    _response.update { "" }
    try {
      when (_source.value.first?.type) {
        ModelVariant.ONDEVICE -> {
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

        ModelVariant.OPENROUTER -> {
          val remoteMessages =
              when (prompt) {
                Prompt.FillIn ->
                    listOf(
                        Message(
                            role = "system",
                            content =
                                listOf(Content(text = app.resources.getString(prompt.prompt)))),
                        Message(content = listOf(Content(text = contentStrings[0]))),
                        Message(content = listOf(Content(text = contentStrings[1]))))

                Prompt.Rephrase ->
                    listOf(
                        Message(
                            role = "system",
                            content =
                                listOf(Content(text = app.resources.getString(prompt.prompt))),
                        ),
                        Message(content = listOf(Content(text = contentStrings[0]))))

                Prompt.Summarize,
                Prompt.Continue ->
                    listOf(
                        Message(
                            role = "system",
                            content =
                                listOf(Content(text = app.resources.getString(prompt.prompt))),
                        )) + contentStrings.map { Message(content = listOf(Content(text = it))) }
              }
          openRouterService
              .generate(
                  OpenRouterRequest(
                      messages = remoteMessages, model = _source.value.first!!.path!!),
                  token)
              .enqueue(
                  object : Callback<ResponseBody> {
                    override fun onResponse(p0: Call<ResponseBody>, p1: Response<ResponseBody>) {
                      val reader = p1.body()?.byteStream()?.bufferedReader()
                      if (reader == null) {
                        _source.update {
                          it.copy(second = InferenceManagerState.ACTIVE, third = -1L)
                        }
                        return
                      }
                      val scope = CoroutineScope(Dispatchers.IO)
                      scope.launch {
                        getResponse(reader, streamingAd, regularAd) {
                              _source.update {
                                it.copy(second = InferenceManagerState.ACTIVE, third = -1L)
                              }
                              _response.update { "" }
                            }
                            .collect { resp ->
                              _response.update {
                                it + resp?.choices?.get(0)?.delta?.content.orEmpty()
                              }
                              onUpdate(_response.value)
                            }
                      }
                    }

                    override fun onFailure(p0: Call<ResponseBody>, p1: Throwable) {
                      Log.e("InferenceRepository", p1.message.toString())
                    }
                  })
        }

        ModelVariant.GIGACHAT -> {
          val remoteMessages =
              when (prompt) {
                Prompt.FillIn ->
                    listOf(
                        GigaChatMessage(
                            role = "system", content = app.resources.getString(prompt.prompt)),
                        GigaChatMessage(content = contentStrings[0]),
                        GigaChatMessage(content = contentStrings[1]))

                Prompt.Rephrase ->
                    listOf(
                        GigaChatMessage(
                            role = "system", content = app.resources.getString(prompt.prompt)),
                        GigaChatMessage(content = contentStrings[0]))

                Prompt.Summarize,
                Prompt.Continue ->
                    listOf(
                        GigaChatMessage(
                            role = "system",
                            content = app.resources.getString(prompt.prompt),
                        )) + contentStrings.map { GigaChatMessage(content = it) }
              }
          dataStoreManager.getTempTokenExpiry().collect { expiresAt ->
            if (expiresAt == 0L || Instant.ofEpochMilli(expiresAt) < Instant.now()) {
              val authResp =
                  gigaChatService.auth(rqiu = UUID.randomUUID().toString(), token = "Basic $token")
              dataStoreManager.setTempToken(authResp.accessToken)
              dataStoreManager.setTempTokenExpiry(authResp.expiresAt)
            }
            dataStoreManager.getTempToken().collect { tempToken ->
              val generated =
                  gigaChatService.generate(
                      token = "Bearer $tempToken",
                      request =
                          GigaChatMessageRequest(
                              model =
                                  _source.value.first?.path ?: error("No model path in GigaChat"),
                              messages = remoteMessages))
              _response.update { generated.gigaChatChoices[0].message.content }
              onUpdate(_response.value)
              _source.update { it.copy(second = InferenceManagerState.ACTIVE, third = -1L) }
            }
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

      else -> _source.update { it.copy(second = InferenceManagerState.ACTIVE) }
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
          Model(-1L, ModelVariant.ONDEVICE, uri.toStrippedFileName(app), ""),
          InferenceManagerState.LOADING)
    }
    localInferenceManager.importModel(uri, callback)
  }

  override fun unloadModel() {
    _source.update { it.copy(null, InferenceManagerState.IDLE, -1L) }
    localInferenceManager.unloadModel()
  }
}

fun Uri.toStrippedFileName(app: Context): String =
    queryName(app.contentResolver).split("/").last().removeSuffix(".bin")
