package com.yaabelozerov.glowws

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color

sealed class Const {
    object UI {
        val MainColor = Color.hsv(347.0f, 0.79f, 1.0f)
    }
    object String {
        const val JSON_DELIMITER = ", "
    }
    object Net {
        const val MODEL_PRELOAD_BASE_URL = "https://tarakoshka.tech/glowws/"
        const val FEEDBACK_BASE_URL = "https://tarakoshka.tech/api/"
    }
}

fun Uri.queryName(resolver: ContentResolver): String {
    val returnCursor = checkNotNull(
        resolver.query(this, null, null, null, null)
    )
    val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    returnCursor.moveToFirst()
    val name = returnCursor.getString(nameIndex)
    returnCursor.close()
    return name
}

fun String.toReadableKey() =
    replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() }
