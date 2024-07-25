package com.yaabelozerov.glowws.data.local.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.yaabelozerov.glowws.ui.screen.ai.AiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File

enum class InferenceManagerState {
    IDLE, LOADING, ACTIVATING, REMOVING;

    fun isIdle() = this == IDLE
}

fun <T> MutableStateFlow<T?>.reset() = this.update { null }

class InferenceManager(private val app: Context) {
    private val _model: MutableStateFlow<Pair<LlmInference, String>?> = MutableStateFlow(null)
    private val _state: MutableStateFlow<Pair<Boolean, String>> = MutableStateFlow(Pair(true, ""))
    private val _callback: MutableStateFlow<(String) -> Unit> =
        MutableStateFlow { st -> println("empty callback: $st") }

    val status: MutableStateFlow<InferenceManagerState> =
        MutableStateFlow(InferenceManagerState.IDLE)
    val error: MutableStateFlow<Exception?> = MutableStateFlow(null)

    private suspend fun tryLoadModel(path: String): Boolean {
        try {
            withContext(Dispatchers.IO) {
                status.update { InferenceManagerState.ACTIVATING }
                val options =
                    LlmInferenceOptions.builder().setModelPath(path).setMaxTokens(1000).setTopK(40)
                        .setResultListener { part, done ->
                            _state.update { Pair(done, it.second + part) }
                            _callback.value(_state.value.second)
                        }.setTemperature(0.8f).setRandomSeed(101).build()
                val inference = LlmInference.createFromOptions(app, options)
                _model.update { Pair(inference, File(path).name) }
                status.update { InferenceManagerState.IDLE }
            }
            Log.i("InferenceManager", "Model on path $path loaded")
            return true
        } catch (e: Exception) {
            Log.e("InferenceManager", "Error loading model on path: $path")
            error.update { e }
            return false
        }
    }

    suspend fun removeModel(name: String) {
        error.reset()
        coroutineScope {
            status.update { InferenceManagerState.REMOVING }
            try {
                val dir = File(app.filesDir, "Models")
                val file = File(dir, name)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                error.update { e }
            }
            status.update { InferenceManagerState.IDLE }
        }
    }

    suspend fun activateModel(name: String, callback: (String) -> Unit) {
        error.reset()
        status.update { InferenceManagerState.ACTIVATING }
        try {
            val dir = File(app.filesDir, "Models")
            val file = File(dir, name)
            if (tryLoadModel(file.absolutePath)) {
                callback(name)
            }
        } catch (e: Exception) {
            error.update { e }
        }
        status.update { InferenceManagerState.IDLE }
    }

    suspend fun importModel(uri: Uri) {
        error.reset()
        withContext(Dispatchers.IO) {
            status.update { InferenceManagerState.LOADING }
            try {
                val fileName = uri.path!!.split("/").last()
                val dir = File(app.filesDir, "Models")
                dir.mkdir()
                val outFile = File(dir, fileName)
                val outStream = outFile.outputStream()
                val inStream = app.contentResolver.openInputStream(uri)

                try {
                    val buf = ByteArray(16 * 1024)
                    var read: Int = inStream!!.read(buf)
                    while (read != -1) {
                        outStream.write(buf)
                        read = inStream.read(buf)
                    }
                } catch (e: Exception) {
                    error.update { e }
                } finally {
                    inStream?.close()
                    outStream.close()
                    status.update { InferenceManagerState.IDLE }
                }

                if (!tryLoadModel(File(dir, fileName).absolutePath)) {
                    outFile.delete()
                }
            } catch (e: Exception) {
                error.update { e }
            }
        }
    }

    fun refreshModels(): List<AiModel> =
        File(app.filesDir, "Models").listFiles()?.map { AiModel(it.nameWithoutExtension, it.name, it.name == _model.value?.second) }
            ?: emptyList()

    fun setCallback(callback: (String) -> Unit) = _callback.update { callback }

    fun executeInto(prompt: String, callback: (String) -> Unit = {}) {
        setCallback(callback)
        _model.value?.let {
            it.first.generateResponseAsync(prompt)
        }
    }

    fun unloadModel() = _model.update { null }
}