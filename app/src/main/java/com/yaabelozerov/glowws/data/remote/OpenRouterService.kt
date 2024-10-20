package com.yaabelozerov.glowws.data.remote

import android.util.JsonToken
import android.util.Log
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import okhttp3.ResponseBody
import org.json.JSONTokener
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Streaming
import java.io.BufferedReader
import java.io.IOException


interface OpenRouterService {
    @Streaming
    @Headers("Accept: application/json")
    @POST("chat/completions")
    fun generate(@Body request: OpenRouterRequest, @Header("Authorization") token: String): Call<ResponseBody>
}


fun getResponse(input: BufferedReader, ad: JsonAdapter<OpenRouterResponse>, onErr: () -> Unit) = flow {
    try {
        while (currentCoroutineContext().isActive) {
            val line = input.readLine()
            if (line != null && line.startsWith("data:")) {
                try {
                    val answerDetailInfo = ad.fromJson(line.substring(5).trim())
                    emit(answerDetailInfo)
                } catch (e: Exception) {
                    onErr()
                }
            }
        }
    } catch (e: IOException) {
        onErr()
    } finally {
        input.close()
    }
}