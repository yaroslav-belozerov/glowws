package com.yaabelozerov.glowws.data.remote

import com.squareup.moshi.Json

data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = true
)

data class Message(val role: String = "user", val content: List<Content>)

data class Content(
    val type: String = "text",
    val text: String,
)

data class StreamResponse(val data: StreamingChoice? = null)

data class OpenRouterResponse(val id: String? = null, val choices: List<StreamingChoice>? = null)

data class StreamingChoice(
    @Json(name = "finish_reason") val finishReason: String?,
    val delta: Delta,
    val error: Error?
)

data class Error(val code: Int, val message: String)

data class Delta(val content: String?, val role: String?)
