package com.yaabelozerov.glowws.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.yaabelozerov.glowws.Const
import com.yaabelozerov.glowws.Net
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelDao
import com.yaabelozerov.glowws.data.local.room.ModelType
import com.yaabelozerov.glowws.di.AppModule
import com.yaabelozerov.glowws.domain.model.Prompt
import com.yaabelozerov.glowws.queryName
import io.ktor.client.request.cookie
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import java.io.File
import java.nio.file.Files
import kotlin.onSuccess


sealed class InferenceOp(val resId: Int) {
  data object Idle : InferenceOp(R.string.ai_status_not_active)
  data object Loading : InferenceOp(R.string.ai_status_loading)
  data object Activating : InferenceOp(R.string.ai_status_activating)
  data object Removing : InferenceOp(R.string.ai_status_removing)
  data object Ready : InferenceOp(R.string.ai_status_ready)
  data class Downloading(val progress: Float) : InferenceOp(R.string.downloading)
  data class Responding(val intoPoint: Long) : InferenceOp(R.string.ai_status_responding)
}

/**
 * Returns whether the current [InferenceOp] indicates the system is not busy.
 *
 * @return true when the operation is [InferenceOp.Ready] or [InferenceOp.Idle].
 */
fun InferenceOp.notBusy(): Boolean {
  return this == InferenceOp.Ready || this == InferenceOp.Idle
}

fun InferenceOp.busy(): Boolean = !this.notBusy()

data class InferenceState(
  val models: List<Model> = emptyList(),
  val selected: Model? = null,
  val inference: LlmInference? = null,
  val operation: InferenceOp = InferenceOp.Idle
)

class InferenceRepository(
  private val app: Context,
  private val modelDao: ModelDao,
  dataStoreManager: AppModule.DataStoreManager,
) {
  private val instanceUrl = dataStoreManager.instanceUrl()
  private val jwt = dataStoreManager.jwt()

  private val _state = MutableStateFlow(InferenceState())
  val state = _state.asStateFlow()

  private val _err = MutableStateFlow<Throwable?>(null)

  /**
   * Generates a response using the selected model for a given [prompt] and [contentStrings].
   * Builds a combined message for on-device/downloadable models and streams tokens via [onUpdate].
   *
   * Side effects: updates repository [state] to [InferenceOp.Responding] and resets to [InferenceOp.Ready] on finish.
   *
   * @param prompt prompt type used to format the message.
   * @param contentStrings list of content parts to inject into the prompt template.
   * @param onUpdate callback receiving streamed response parts.
   * @param pointId UI point identifier to bind the response to.
   * @param onErr called if an exception occurs while generating.
   */
  suspend fun generate(
    prompt: Prompt, contentStrings: List<String>, onUpdate: (String) -> Unit, pointId: Long, onErr: () -> Unit, onSuccess: () -> Unit
  ) {
    _state.update { it.copy(operation = InferenceOp.Responding(pointId)) }
    try {
      when (_state.value.selected?.type) {
        ModelType.OnDevice, is ModelType.Downloadable -> {
          val message = when (prompt) {
            Prompt.FillIn -> app.resources.getString(R.string.prompt_local_prepend) + "system: ${
              app.resources.getString(
                prompt.prompt
              )
            }\nuser: ${contentStrings[0]}\nuser: ${contentStrings[1]}\nassistant:"

            Prompt.Rephrase -> app.resources.getString(R.string.prompt_local_prepend) + "system: ${
              app.resources.getString(
                prompt.prompt
              )
            }\nuser: ${contentStrings[0]}\nassistant:"

            Prompt.Summarize, Prompt.Continue -> app.resources.getString(R.string.prompt_local_prepend) + "system: ${
              app.resources.getString(
                prompt.prompt
              )
            }\nuser: ${contentStrings.joinToString("\nuser: ")}\nassistant:"
          }
          execute(message, onUpdate) {
            onSuccess()
            _state.update { it.copy(operation = InferenceOp.Ready) }
          }
        }

        null -> TODO()
      }
    } catch (e: Exception) {
      interrupt(e)
      onErr()
    }
  }

  fun setModels(list: List<Model>) = _state.update { it.copy(models = list) }

  /**
   * Interrupts the current operation: shows a short toast (if [e] provided) and
   * switches operation back to [InferenceOp.Ready].
   *
   * @param e optional exception to display.
   */
  fun interrupt(e: Exception?) {
    e?.let { Toast.makeText(app, it.localizedMessage, Toast.LENGTH_SHORT).show() }
    _state.update { it.copy(operation = InferenceOp.Ready) }
    setStop()
  }

  /**
   * Loads the provided [model] for use:
   * - For [ModelType.OnDevice] attempts to initialize local inference.
   * - For [ModelType.Downloadable] downloads the model if missing and persists metadata.
   *
   * Always calls [callback] at the end of the flow.
   *
   * @param model the model to activate.
   * @param callback invoked when the load flow completes.
   */
  suspend fun loadModel(model: Model, onSuccess: suspend (String) -> Unit = {}) {
    when (model.type) {
      ModelType.OnDevice -> model.path?.let {
        _state.update { it.copy(selected = model, operation = InferenceOp.Activating) }
        tryLoadModel(it).onSuccess {
          _state.update { src -> src.copy(selected = model, operation = InferenceOp.Ready, inference = it) }
          onSuccess(model.path)
        }.onFailure { _err.update { it } }
      }


      is ModelType.Downloadable -> {
        suspend fun performLoad(path: String) {
          _state.update { it.copy(selected = model, operation = InferenceOp.Activating) }

          tryLoadModel(path).onSuccess { inference ->
            _state.update { src ->
              src.copy(selected = model, operation = InferenceOp.Ready, inference = inference)
            }
            onSuccess(path)
          }.onFailure { error ->
            _err.update { error }
          }
        }

        if (model.path != null && File(model.path).exists() && model.type.isDownloaded) {
          performLoad(model.path)
        } else {
          _state.update { it.copy(selected = model, operation = InferenceOp.Downloading(0f)) }

          Net.httpClient.prepareGet("${instanceUrl.first()}/download") {
            parameter("file", model.name)
            cookie("auth", jwt.first())
          }.execute { httpResponse ->
            val channel = httpResponse.bodyAsChannel()
            importModel(model.name, channel, httpResponse.contentLength(), { progress ->
              _state.update { src -> src.copy(operation = InferenceOp.Downloading(progress)) }
            }) { newPath ->
              val updatedModel = model.copy(
                path = newPath,
                type = ModelType.Downloadable(true)
              )
              modelDao.upsertModel(updatedModel)
              _state.update { src ->
                src.copy(selected = updatedModel, operation = InferenceOp.Ready)
              }
              performLoad(updatedModel.path!!)
            }
          }
        }
      }
    }
  }

  /**
   * Removes a previously added [model] from storage. If the removed model is the currently
   * selected one, clears selection and sets operation to [InferenceOp.Idle], otherwise
   * sets operation to last operation before removal.
   *
   * @param model the model metadata to remove.
   */
  suspend fun removeModel(model: Model) {
    val previousOp = _state.value.operation
    _state.update { it.copy(operation = InferenceOp.Removing) }
    when (model.type) {
      ModelType.OnDevice, is ModelType.Downloadable -> model.path?.let {
        removeModel(model.name).onSuccess {
          _state.update {
            if (it.selected != model) {
              it.copy(operation = previousOp)
            } else {
              it.copy(selected = null, operation = InferenceOp.Idle)
            }
          }
        }
      }
    }
  }

  /**
   * Adds a local model file referenced by [uri] into the app's storage and selects it.
   * Emits [InferenceOp.Loading] while copying and calls [callback] with the stored path.
   *
   * @param uri the content Uri to import from.
   * @param callback called with absolute path to the stored model file.
   */
  suspend fun addLocalModel(uri: Uri, callback: suspend (String) -> Unit) {
    _state.update {
      it.copy(
        selected = Model(
          uri.toStrippedFileName(app) + System.currentTimeMillis().toString(),
          ModelType.OnDevice,
          uri.toStrippedFileName(app),
        ), operation = InferenceOp.Loading
      )
    }
    withContext(Dispatchers.IO) {
      try {
        val fileName = uri.queryName(app.contentResolver)
        val dir = File(app.filesDir, "Models")
        dir.mkdir()

        val outFile = File(dir, uri.queryName(app.contentResolver))
        val outStream = outFile.outputStream()
        val sink = outStream.asSink()

        try {
          app.contentResolver.openInputStream(uri)?.use {
            val chan = it.toByteReadChannel()
            var bytesRead = 0L
            while (!chan.exhausted()) {
              bytesRead += chan.readRemaining(Const.File.MODEL_CHUNK_SIZE).transferTo(sink)
              println("FILE PROGRESS: ${bytesRead / 1024 / 1024} GB")
            }
            callback(outFile.absolutePath)
          }
        } catch (e: Exception) {
          e.printStackTrace()
        } finally {
          outStream.close()
        }
        tryLoadModel(File(dir, fileName).absolutePath).onFailure {
          outFile.delete()
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  /**
   * Unselects the current model and clears the in-memory inference engine.
   */
  fun unloadModel() = _state.update { it.copy(selected = null, operation = InferenceOp.Idle, inference = null) }

  /**
   * Attempts to instantiate [LlmInference] from a model file at [path].
   *
   * @param path absolute path to model file.
   * @return [Result] with the created [LlmInference] or a failure.
   */
  private suspend fun tryLoadModel(path: String): Result<LlmInference> = runCatching {
    withContext(Dispatchers.IO) {
      val options = LlmInferenceOptions.builder().setModelPath(path).setMaxTokens(256).setMaxTopK(40).build()
      val inference = LlmInference.createFromOptions(app, options)
      return@withContext inference
    }
  }

  /**
   * Deletes a model file by its [name] in the app's `files/Models` directory.
   *
   * @param name file name of the model to delete.
   * @return [Result] of the deletion operation.
   */
  suspend fun removeModel(name: String): Result<Unit> = runCatching {
    withContext(Dispatchers.IO) {
      Files.delete(app.filesDir.resolve("Models").resolve(name).toPath())
    }
  }

  suspend fun importModel(
    fileName: String, chan: ByteReadChannel, contentLength: Long?, onProgress: (Float) -> Unit, onSuccess: suspend (String) -> Unit
  ) {
    withContext(Dispatchers.IO) {
      runCatching {
        val dir = File(app.filesDir, "Models")
        dir.mkdir()
        val outFile = File(dir, fileName)
        outFile.outputStream().use {
          val byteBufferSize = Const.File.MODEL_CHUNK_SIZE
          var bytesRead = 0L

          while (!chan.exhausted()) {
            bytesRead += chan.readRemaining(byteBufferSize).transferTo(it.asSink())
            contentLength?.toFloat()?.let { len ->
              onProgress(bytesRead.toFloat().div(len))
            }
          }

          onSuccess(outFile.absolutePath)
        }
      }
    }
  }

  private val stop = MutableStateFlow(false)
  val stopSignal = stop.asStateFlow()
  fun setStop() = stop.update { true }

  /**
   * Launches text generation for [prompt] using the loaded inference engine.
   * Streams partial outputs via [onUpdate] and calls [onEnd] when finished.
   *
   * @param prompt full prompt text.
   * @param onUpdate callback for streamed parts of the response.
   * @param onEnd callback invoked when generation completes.
   * @return a [Job] running on [Dispatchers.IO].
   */
  fun execute(prompt: String, onUpdate: (String) -> Unit = {}, onEnd: () -> Unit = {}): Job {
    val scope = CoroutineScope(Dispatchers.IO)
    return scope.launch {
      _state.value.inference?.generateResponseAsync(
        prompt
      ) { part, done ->
        if (done) stop.update { false }
        if (stop.value) return@generateResponseAsync
        onUpdate(part)
        if (done) {
          onEnd()
        }
      }
    }
  }
}

/**
 * Extracts a display-friendly base name from a content [Uri], removing path segments and `.bin` suffix.
 *
 * @param app application context for content resolution.
 * @return stripped file name without extension.
 */
fun Uri.toStrippedFileName(app: Context): String = queryName(app.contentResolver).split("/").last().removeSuffix(".bin")
