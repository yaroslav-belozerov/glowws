package com.yaabelozerov.glowws.data.remote

import com.squareup.moshi.Json

data class GigaChatAuthResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_at") val expiresAt: Long
)

enum class GigaChatScope {
  GIGACHAT_API_PERS
}

data class GigaChatMessageRequest(
    val model: String,
    val messages: List<GigaChatMessage>,
    val stream: Boolean = false,
    @Json(name = "update_interval") val updateInterval: Long = 0,
)

data class GigaChatMessageResponse(
    @Json(name = "choices") val gigaChatChoices: List<GigaChatChoice>,
    val created: Long,
    val model: String,
<<<<<<< Updated upstream
    @Json(name = "object") val objectField: String,
=======
    @Json(name = "object")
    val objectField: String,
>>>>>>> Stashed changes
    val usage: Usage,
)

data class GigaChatChoice(
    val message: GigaChatMessage,
    val index: Long,
    @Json(name = "finish_reason") val finishReason: String,
)

data class Usage(
    @Json(name = "prompt_tokens") val promptTokens: Long,
    @Json(name = "completion_tokens") val completionTokens: Long,
    @Json(name = "total_tokens") val totalTokens: Long,
)

data class GigaChatMessage(
    val role: String = "user",
    val content: String,
)
