package com.yaabelozerov.glowws.data.local.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class InferenceManager(private val app: Context) {
    private val _model: MutableStateFlow<LlmInference?> = MutableStateFlow(null)
    private val _state: MutableStateFlow<Pair<Boolean, String>> = MutableStateFlow(Pair(true, ""))
    private val _callback: MutableStateFlow<(String) -> Unit> = MutableStateFlow { st -> println("empty callback: $st") }


    private fun tryLoadModel(path: String): Boolean {
        try {
            val options =
                LlmInferenceOptions.builder().setModelPath(path).setMaxTokens(1000).setTopK(40)
                    .setResultListener { part, done ->
                        _state.update { Pair(done, it.second + part) }
                        _callback.value(_state.value.second)
                    }.setTemperature(0.8f).setRandomSeed(101).build()
            val inference = LlmInference.createFromOptions(app, options)
            _model.update { inference }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun importModel(uri: Uri) {
        coroutineScope {
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
            } finally {
                inStream?.close()
                outStream.close()
            }

            if (!tryLoadModel(File(dir, fileName).absolutePath)) {
                outFile.delete()
            }
        }
    }

    fun checkPath() {
        Log.i(
            "InferenceManager", File(app.filesDir, "Models").listFiles()?.map { it.name }.toString()
        )
    }

    fun setCallback(callback: (String) -> Unit) = _callback.update { callback }

    fun executeInto(prompt: String, callback: (String) -> Unit = {}) {
        setCallback(callback)
        _model.value?.let {
            it.generateResponseAsync(prompt)
        }
    }
}