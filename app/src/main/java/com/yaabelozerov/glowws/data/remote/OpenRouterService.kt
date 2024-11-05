package com.yaabelozerov.glowws.data.remote

import com.squareup.moshi.JsonAdapter
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

fun getResponse(input: BufferedReader, ad: JsonAdapter<OpenRouterResponse>, onErr: () -> Unit) =
    flow {
      try {
        while (currentCoroutineContext().isActive) {
          val line = input.readLine()
          if (line != null && line.startsWith(Const.Net.OPENROUTER_STREAMING_PREFIX)) {
            try {
              val answerDetailInfo =
                  ad.fromJson(line.substring(Const.Net.OPENROUTER_STREAMING_PREFIX.length).trim())
              emit(answerDetailInfo)
            } catch (e: Exception) {
              e.printStackTrace()
              onErr()
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
