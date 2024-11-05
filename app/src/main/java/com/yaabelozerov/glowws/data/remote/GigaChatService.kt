package com.yaabelozerov.glowws.data.remote

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface GigaChatService {
    @POST("https://ngw.devices.sberbank.ru:9443/api/v2/oauth")
    @FormUrlEncoded
    suspend fun auth(
        @Header("RqUID") rqiu: String,
        @Header("Authorization") token: String,
        @Field("scope") scope: GigaChatScope = GigaChatScope.GIGACHAT_API_PERS
    ): GigaChatAuthResponse

    @POST("https://gigachat.devices.sberbank.ru/api/v1/chat/completions")
    suspend fun generate(
        @Header("Authorization") token: String,
        @Body request: GigaChatMessageRequest
    ): GigaChatMessageResponse
}