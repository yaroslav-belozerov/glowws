package com.yaabelozerov.glowws.data.local.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.ui.screen.ai.AiModel
import com.yaabelozerov.glowws.util.queryName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File

enum class InferenceManagerState(val resId: Int) {
    IDLE(R.string.ai_status_not_active), LOADING(R.string.ai_status_loading), ACTIVATING(R.string.ai_status_activating), REMOVING(
        R.string.ai_status_removing
    ),
    ACTIVE(R.string.ai_status_ready), RESPONDING(R.string.ai_status_responding);

    fun notBusy() = this == IDLE || this == ACTIVE
}

fun <T> MutableStateFlow<T?>.reset() = this.update { null }

class InferenceManager(private val app: Context) {
    private val _model: MutableStateFlow<LlmInference?> = MutableStateFlow(null)
    val model = _model.asStateFlow()

    private val _state: MutableStateFlow<Pair<Boolean, String>> = MutableStateFlow(Pair(true, ""))
    private val _callback: MutableStateFlow<(String) -> Unit> =
        MutableStateFlow { st -> println("empty callback: $st") }

    val error: MutableStateFlow<Exception?> = MutableStateFlow(null)
    val status: MutableStateFlow<Pair<String?, InferenceManagerState>> =
        MutableStateFlow(Pair(null, InferenceManagerState.IDLE))

    private suspend fun tryLoadModel(path: String, callback: () -> Unit = {}): Boolean {
        try {
            withContext(Dispatchers.IO) {
                status.update {
                    Pair(path.split("/").last(), InferenceManagerState.ACTIVATING)
                }
                val options =
                    LlmInferenceOptions.builder().setModelPath(path).setMaxTokens(1000).setTopK(40)
                        .setResultListener { part, done ->
                            _state.update { Pair(done, it.second + part) }
                            _callback.value(_state.value.second)
                            if (done) {
                                _state.update { it.copy(second = "") }
                                status.update { Pair(it.first, InferenceManagerState.ACTIVE) }
                            }
                        }.setTemperature(0.8f).setRandomSeed(101).build()
                val inference = LlmInference.createFromOptions(app, options)
                _model.update { inference }
                status.update { Pair(path.split("/").last(), InferenceManagerState.ACTIVE) }
            }
            Log.i("InferenceManager", "Model on path $path loaded")
            callback()
            return true
        } catch (e: Exception) {
            Log.e("InferenceManager", "Error loading model on path: $path")
            status.update { Pair(null, InferenceManagerState.IDLE) }
            error.update { e }
            return false
        }
    }

    suspend fun removeModel(name: String) {
        error.reset()
        withContext(Dispatchers.IO) {
            val prevName = if (status.value.first == name) null else status.value.first
            status.update { Pair(name, InferenceManagerState.REMOVING) }
            try {
                val dir = File(app.filesDir, "Models")
                val file = File(dir, name)
                if (file.exists()) {
                    if (status.value.first == name) unloadModel()
                    file.delete()
                }
            } catch (e: Exception) {
                error.update { e }
            }
            status.update { Pair(prevName, InferenceManagerState.IDLE) }
        }
    }

    suspend fun activateModel(name: String, callback: (String) -> Unit) {
        error.reset()
        status.update { Pair(name, InferenceManagerState.LOADING) }
        withContext(Dispatchers.IO) {
            try {
                val dir = File(app.filesDir, "Models")
                val file = File(dir, name)
                if (tryLoadModel(file.absolutePath)) {
                    callback(name)
                }
            } catch (e: Exception) {
                error.update { e }
                status.update { Pair(null, InferenceManagerState.IDLE) }
            }
        }
    }

    suspend fun importModel(uri: Uri, callback: () -> Unit = {}) {
        error.reset()
        withContext(Dispatchers.IO) {
            try {
                val fileName = uri.queryName(app.contentResolver)
                status.update { Pair(fileName, InferenceManagerState.LOADING) }
                val dir = File(app.filesDir, "Models")
                dir.mkdir()
                val inStream = app.contentResolver.openInputStream(uri)

                val outFile = File(dir, uri.queryName(app.contentResolver))
                val outStream = outFile.outputStream()

                try {
                    val buf = ByteArray(16 * 1024)
                    var read: Int = inStream!!.read(buf)
                    while (read != -1) {
                        outStream.write(buf)
                        read = inStream.read(buf)
                    }
                } catch (e: Exception) {
                    error.update { e }
                    status.update { Pair(null, InferenceManagerState.IDLE) }
                } finally {
                    inStream?.close()
                    outStream.close()
                }

                if (!tryLoadModel(File(dir, fileName).absolutePath, callback)) {
                    outFile.delete()
                }
            } catch (e: Exception) {
                error.update { e }
                status.update { Pair(null, InferenceManagerState.IDLE) }
            }
        }
    }

    fun refreshModels(): List<AiModel> = File(app.filesDir, "Models").listFiles()
        ?.map { AiModel(it.nameWithoutExtension, it.name, it.name == status.value.first) }
        ?: emptyList()

    private fun setCallback(callback: (String) -> Unit) = _callback.update { callback }

    suspend fun executeInto(prompt: String, callback: (String) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            status.update { Pair(it.first, InferenceManagerState.RESPONDING) }
            setCallback(callback)
            _model.value?.let {
                it.generateResponseAsync(prompt)
            }
        }
    }

    fun unloadModel() {
        _model.update { null }
        status.update { Pair(null, InferenceManagerState.IDLE) }
    }
}