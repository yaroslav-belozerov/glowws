package com.yaabelozerov.glowws.domain

import android.net.Uri
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.domain.model.Prompt
import kotlinx.coroutines.flow.StateFlow

interface InferenceRepository {
  val source: StateFlow<Triple<Model?, InferenceManagerState, Long>>
    suspend fun generate(prompt: Prompt, contentStrings: List<String>, onUpdate: (String) -> Unit = {}, pointId: Long)
    suspend fun loadModel(model: Model, callback: suspend () -> Unit = {})
    suspend fun removeModel(model: Model, stateAfter: InferenceManagerState = InferenceManagerState.IDLE)
    fun unloadModel()
    suspend fun addLocalModel(uri: Uri, callback: suspend (String) -> Unit)
    fun interrupt(e: Exception? = null)
}
