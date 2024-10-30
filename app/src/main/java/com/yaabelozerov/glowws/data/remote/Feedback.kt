package com.yaabelozerov.glowws.data.remote


import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class FeedbackDTO(
    val header: String,
    val rating: Long,
    val desc: String,
)

interface FeedbackService {
    @POST("feedback")
    suspend fun sendFeedback(@Body feedback: FeedbackDTO)
}