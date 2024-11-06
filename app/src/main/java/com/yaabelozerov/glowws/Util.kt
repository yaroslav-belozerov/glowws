package com.yaabelozerov.glowws

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import com.yaabelozerov.glowws.domain.model.Prompt
import okhttp3.internal.immutableListOf

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
    const val MODEL_CHUNK_SIZE = 512
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
