package com.yaabelozerov.glowws.data.local.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.yaabelozerov.glowws.Const
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.queryName
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class InferenceManagerState(val resId: Int) {
  IDLE(R.string.ai_status_not_active),
  LOADING(R.string.ai_status_loading),
  ACTIVATING(R.string.ai_status_activating),
  REMOVING(R.string.ai_status_removing),
  ACTIVE(R.string.ai_status_ready),
  RESPONDING(R.string.ai_status_responding)
}

fun InferenceManagerState.notBusy(): Boolean {
  return this == InferenceManagerState.ACTIVE || this == InferenceManagerState.IDLE
}

fun <T> MutableStateFlow<T?>.reset() = this.update { null }

class InferenceManager @Inject constructor(private val app: Context) {
  private val _model: MutableStateFlow<LlmInference?> = MutableStateFlow(null)
  val model = _model.asStateFlow()

  private val _state: MutableStateFlow<Pair<Boolean, String>> = MutableStateFlow(Pair(true, ""))
  private val _callback: MutableStateFlow<Pair<(String) -> Unit, () -> Unit>> =
      MutableStateFlow(
          Pair(
              first = { st -> println("empty callback: $st") },
              second = { println("empty callback: onEnd") }))

  val error: MutableStateFlow<Exception?> = MutableStateFlow(null)

  private suspend fun tryLoadModel(path: String, onUpdate: suspend (String) -> Unit = {}): Boolean {
    try {
      withContext(Dispatchers.IO) {
        val options =
            LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(512)
                .setTopK(40)
                .setResultListener { part, done ->
                  _state.update { Pair(done, it.second + part) }
                  _callback.value.first(_state.value.second)
                  if (done) {
                    _state.update { it.copy(second = "") }
                    _callback.value.second()
                  }
                }
                .setTemperature(0.8f)
                .setRandomSeed(101)
                .build()
        val inference = LlmInference.createFromOptions(app, options)
        _model.update { inference }
      }
      Log.i("InferenceManager", "Model on path $path loaded")
      onUpdate(path)
      return true
    } catch (e: Exception) {
      Log.e("InferenceManager", "Error loading model on path: $path")
      error.update { e }
      return false
    }
  }

  suspend fun removeModel(name: String, callback: () -> Unit) {
    error.reset()
    withContext(Dispatchers.IO) {
      try {
        Files.delete(app.filesDir.resolve("Models").resolve(name).toPath())
        callback()
      } catch (e: Exception) {
        error.update { e }
      }
    }
  }

  suspend fun activateModel(filepath: String, callback: (String) -> Unit) {
    error.reset()
    withContext(Dispatchers.IO) {
      try {
        if (tryLoadModel(filepath)) {
          callback(filepath)
        }
      } catch (e: Exception) {
        error.update { e }
      }
    }
  }

  suspend fun importModel(uri: Uri, callback: suspend (String) -> Unit = {}) {
    error.reset()
    withContext(Dispatchers.IO) {
      try {
        val fileName = uri.queryName(app.contentResolver)
        val dir = File(app.filesDir, "Models")
        dir.mkdir()
        val inStream = app.contentResolver.openInputStream(uri)

        val outFile = File(dir, uri.queryName(app.contentResolver))
        val outStream = outFile.outputStream()

        try {
          val buf = ByteArray(Const.File.MODEL_CHUNK_SIZE)
          var read: Int = inStream?.read(buf) ?: throw NullPointerException("inStream is null")
          while (read != -1) {
            outStream.write(buf)
            read = inStream.read(buf)
          }
        } catch (e: Exception) {
          e.printStackTrace()
          error.update { e }
        } finally {
          inStream?.close()
          outStream.close()
        }

        if (!tryLoadModel(File(dir, fileName).absolutePath, callback)) {
          outFile.delete()
        }
      } catch (e: Exception) {
        e.printStackTrace()
        error.update { e }
      }
    }
  }

  private fun setCallback(onUpdate: (String) -> Unit, onEnd: () -> Unit) =
      _callback.update { Pair(onUpdate, onEnd) }

  fun execute(prompt: String, onUpdate: (String) -> Unit = {}, onEnd: () -> Unit = {}): Job {
    val scope = CoroutineScope(Dispatchers.IO)
    return scope.launch {
      setCallback(onUpdate, onEnd)
      _model.value?.generateResponseAsync(prompt)
    }
  }

  fun unloadModel() {
    _model.update { null }
  }
}
