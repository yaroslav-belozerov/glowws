package com.yaabelozerov.glowws.data.remote

import com.yaabelozerov.glowws.data.local.room.Model
import retrofit2.Call
import retrofit2.http.GET

interface PreloadModelsService {
    @GET("models")
    fun getModels(): Call<List<Model>>
}