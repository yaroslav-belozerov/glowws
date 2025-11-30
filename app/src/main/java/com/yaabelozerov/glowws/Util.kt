package com.yaabelozerov.glowws

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.cookie
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

sealed class Const {
  object UI {
    val MainColor = Color.hsv(347.0f, 0.79f, 1.0f)
    const val UNUSED_ICON_ALPHA = 0.4f
    const val MAX_PRIORITY = 3
  }

  object String {
    const val JSON_DELIMITER = ", "
  }

  object Net {
    const val MODEL_PRELOAD_BASE_URL = "https://tarakoshka.tech/glowws/"
    const val OPENROUTER_STREAMING_PREFIX = "data:"
    const val FEEDBACK_BASE_URL = "https://tarakoshka.tech/api/"
  }

  object File {
    const val MODEL_CHUNK_SIZE = 2048
    const val IMAGE_CHUNK_SIZE = 512
  }
}

fun Uri.queryName(resolver: ContentResolver): String {
  val returnCursor = checkNotNull(resolver.query(this, null, null, null, null))
  val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
  returnCursor.moveToFirst()
  val name = returnCursor.getString(nameIndex)
  returnCursor.close()
  return name
}

fun String.toReadableKey() =
    replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() }

object Net {
  val httpClient = HttpClient {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
      })
    }
    defaultRequest {
      headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
      headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
    }
    expectSuccess = true
  }

  suspend inline fun <reified T> get(baseUrlFlow: Flow<String>, path: String, token: String, params: List<Pair<String, String>>? = null) = runCatching {
    httpClient.get("${baseUrlFlow.first()}/$path") {
      params?.forEach { it.run { parameter(first, second) }  }
      cookie("auth", token)
    }.body<T>()
  }

  suspend inline fun <reified T, reified V> put(baseUrlFlow: Flow<String>, path: String, token: String, reqBody: V, params: List<Pair<String, String>>? = null) = runCatching {
    httpClient.put("${baseUrlFlow.first()}/$path") {
      params?.forEach { it.run { parameter(first, second) }  }
      setBody(reqBody)
      cookie("auth", token)
    }.body<T>()
  }

  suspend inline fun <reified T> put(baseUrlFlow: Flow<String>, path: String, token: String, params: List<Pair<String, String>>? = null) = runCatching {
    httpClient.put("${baseUrlFlow.first()}/$path") {
      params?.forEach { it.run { parameter(first, second) }  }
      cookie("auth", token)
    }.body<T>()
  }

  suspend inline fun <reified T> post(baseUrlFlow: Flow<String>, path: String, token: String) = runCatching {
    httpClient.post("${baseUrlFlow.first()}/$path") {
      cookie("auth", token)
    }.body<T>()
  }

  suspend inline fun <reified T, reified V> post(baseUrlFlow: Flow<String>, path: String, token: String, reqBody: V) = runCatching {
    httpClient.post("${baseUrlFlow.first()}/$path") {
      setBody(reqBody)
      cookie("auth", token)
    }.body<T>()
  }

  suspend inline fun <reified T> delete(baseUrlFlow: Flow<String>, path: String, token: String) = runCatching {
    httpClient.delete("${baseUrlFlow.first()}/$path") {
      cookie("auth", token)
    }.body<T>()
  }

  suspend inline fun <reified T, reified V> delete(baseUrlFlow: Flow<String>, path: String, token: String, reqBody: V, params: List<Pair<String, String>>? = null) = runCatching {
    httpClient.delete("${baseUrlFlow.first()}/$path") {
      params?.forEach { it.run { parameter(first, second) }  }
      setBody(reqBody)
      cookie("auth", token)
    }.body<T>()
  }
}
