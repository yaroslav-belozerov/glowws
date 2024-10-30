package com.yaabelozerov.glowws.data

import android.R.id.message
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yaabelozerov.glowws.data.local.ai.InferenceManager
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelType
import com.yaabelozerov.glowws.data.remote.Content
import com.yaabelozerov.glowws.data.remote.Message
import com.yaabelozerov.glowws.data.remote.OpenRouterRequest
import com.yaabelozerov.glowws.data.remote.OpenRouterResponse
import com.yaabelozerov.glowws.data.remote.OpenRouterService
import com.yaabelozerov.glowws.data.remote.StreamResponse
import com.yaabelozerov.glowws.data.remote.StreamingChoice
import com.yaabelozerov.glowws.data.remote.getResponse
import com.yaabelozerov.glowws.domain.InferenceRepository
import com.yaabelozerov.glowws.queryName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okio.BufferedSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Arrays


class InferenceRepositoryImpl(
    private val localInferenceManager: InferenceManager,
    private val openRouterService: OpenRouterService,
    private val app: Context,
    private val moshi: Moshi
): InferenceRepository {
    private val _source = MutableStateFlow<Triple<Model?, InferenceManagerState, Long>>(Triple(null, InferenceManagerState.IDLE, -1L))
    override val source = _source.asStateFlow()

    private val _response = MutableStateFlow("")
    val ad = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(OpenRouterResponse::class.java)

    override suspend fun generate(prompt: String, onUpdate: (String) -> Unit, pointId: Long, token: String) {
        _source.update { it.copy(second = InferenceManagerState.RESPONDING, third = pointId) }
        when (_source.value.first?.type) {
            ModelType.LOCAL -> localInferenceManager.execute(prompt, onUpdate) {
                _source.update {
                    it.copy(
                        second = InferenceManagerState.ACTIVE, third = -1L
                    )
                }
            }

            ModelType.OPENROUTER -> {
                openRouterService.generate(
                    OpenRouterRequest(
                        messages = listOf(Message(content = listOf(Content(text = prompt)))),
                    ), token
                ).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(p0: Call<ResponseBody>, p1: Response<ResponseBody>) {
                        val reader = p1.body()?.byteStream()?.bufferedReader()
                        if (reader == null) {
                            _source.update { it.copy(second = InferenceManagerState.ACTIVE, third = -1L) }
                            return
                        }
                        val scope = CoroutineScope(Dispatchers.IO)
                        scope.launch {
                            getResponse(
                                reader,
                                ad
                            ) {
                                _source.update {
                                    it.copy(
                                        second = InferenceManagerState.ACTIVE,
                                        third = -1L
                                    )
                                }
                            }.collect { resp ->
                                _response.update { it + resp!!.choices!![0].delta.content!! }
                                onUpdate(_response.value)
                            }
                        }
                    }

                    override fun onFailure(p0: Call<ResponseBody>, p1: Throwable) {
                        Log.e("InferenceRepository", p1.message.toString())
                    }
                })
            }
            null -> TODO()
        }
    }

    override suspend fun loadModel(model: Model, callback: suspend () -> Unit) {
        _source.update { it.copy(model, InferenceManagerState.ACTIVATING) }
        when (model.type) {
            ModelType.LOCAL -> localInferenceManager.activateModel(model.path!!) { _source.update { it.copy(model, InferenceManagerState.ACTIVE) } }
            ModelType.OPENROUTER -> _source.update { it.copy(second =  InferenceManagerState.ACTIVE) }
        }
        callback()
    }

    override suspend fun removeModel(model: Model) {
        _source.update { it.copy(second = InferenceManagerState.REMOVING) }
        when (model.type) {
            ModelType.LOCAL -> localInferenceManager.removeModel(model.name!!) { _source.update { it.copy(null, InferenceManagerState.IDLE, -1L) } }
            ModelType.OPENROUTER -> TODO()
        }
    }

    override suspend fun addLocalModel(uri: Uri, callback: suspend (String) -> Unit) {
        _source.update { it.copy(Model(-1L, ModelType.LOCAL, uri.toStrippedFileName(app), ""), InferenceManagerState.LOADING) }
        localInferenceManager.importModel(uri, callback)
    }

    override fun unloadModel() {
        _source.update { it.copy(null, InferenceManagerState.IDLE, -1L) }
        localInferenceManager.unloadModel()
    }

    fun getData(inputStream: BufferedInputStream, onGet: (String) -> Unit) {
        Thread {
            val buffer =
                ByteArray(1024)
            var bytesRead: Int
            try {
                while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                    val data = String(
                        buffer,
                        0,
                        bytesRead,
                        StandardCharsets.UTF_8
                    ).split("data:".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    Log.i("tramResponseArray", Arrays.stream(data).toArray().contentToString());
                    for (responseString in data) {
                        val tramResponse = responseString.trim { it <= ' ' }
                        if (tramResponse.isNotEmpty()) {
                            if (!tramResponse.equals("[DONE]", ignoreCase = true)) {
                                var openAIChatResponseModel: OpenRouterResponse? = null
                                try {
                                    openAIChatResponseModel = moshi.adapter(OpenRouterResponse::class.java).fromJson(tramResponse)
                                    openAIChatResponseModel?.choices?.let {
                                        if (it[0].delta.content != null
                                        ) {
                                            it[0].delta.content?.let(onGet)
                                        }
                                    } ?: Log.e("getData", "Error parsing JSON response: $tramResponse")
                                } catch (e: JsonDataException) {
                                    e.localizedMessage?.let { Log.e("getData", it) };
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                // Handle any IOException that may occur during the reading process
                e.printStackTrace()
            } finally {
                // Close the InputStream to release system resources
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    // Handle any IOException that may occur during the closing process
                    e.printStackTrace()
                }
            }
        }.start()
    }
}

fun Uri.toStrippedFileName(app: Context): String = queryName(app.contentResolver).split("/")?.last()?.removeSuffix(".bin") ?: ""