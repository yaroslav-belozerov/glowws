package com.yaabelozerov.glowws.domain

import android.net.Uri
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.room.Model
import kotlinx.coroutines.flow.StateFlow

interface InferenceRepository {
    val source: StateFlow<Triple<Model?, InferenceManagerState, Long>>

    suspend fun generate(prompt: String, onUpdate: (String) -> Unit = {}, pointId: Long, token: String)
    suspend fun loadModel(model: Model, callback: suspend () -> Unit = {})
    suspend fun removeModel(model: Model)
    fun unloadModel()
    suspend fun addLocalModel(uri: Uri, callback: suspend (String) -> Unit)
}