package com.yaabelozerov.glowws.data.remote

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.yaabelozerov.glowws.Const
import java.io.BufferedReader
import java.io.IOException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OpenRouterService {
  @Streaming
  @Headers("Accept: application/json")
  @POST("chat/completions")
  fun generate(
      @Body request: OpenRouterRequest,
      @Header("Authorization") token: String
  ): Call<ResponseBody>
}

fun getResponse(input: BufferedReader, streamAd: JsonAdapter<OpenRouterResponse>, regularAd: JsonAdapter<List<Content>>, onErr: () -> Unit) =
    flow {
      try {
        while (currentCoroutineContext().isActive) {
          val line = input.readLine()
          if (line != null && line.startsWith(Const.Net.OPENROUTER_STREAMING_PREFIX)) {
            try {
              val answerDetailInfo =
                  streamAd.fromJson(line.substring(Const.Net.OPENROUTER_STREAMING_PREFIX.length).trim())
              emit(answerDetailInfo)
            } catch (e: Exception) {
              if (e is JsonDataException) {
                try {
                  val answerDetailInfo = regularAd.fromJson(line.substring(Const.Net.OPENROUTER_STREAMING_PREFIX.length).trim())
                  emit(OpenRouterResponse(choices = answerDetailInfo?.map { StreamingChoice(delta = Delta(content = it.text, role = null), error = null, finishReason = null) }.orEmpty()))
                } catch (e: Exception) {
                  emit(null)
                  e.printStackTrace()
                  onErr()
                }
              }
            }
          }
        }
      } catch (e: IOException) {
        e.printStackTrace()
        onErr()
      } finally {
        input.close()
      }
    }
