package com.yaabelozerov.glowws.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

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
