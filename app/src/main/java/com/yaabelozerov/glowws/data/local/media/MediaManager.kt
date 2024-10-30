package com.yaabelozerov.glowws.data.local.media

import android.content.Context
import android.net.Uri
import android.util.Log
import com.yaabelozerov.glowws.queryName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaManager(private val app: Context) {
    suspend fun importMedia(uri: Uri, callback: (String) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = System.currentTimeMillis().toString() + "." +
                    uri.queryName(app.contentResolver).split('.').last()
                val dir = File(app.filesDir, "Media")
                dir.mkdir()
                val inStream = app.contentResolver.openInputStream(uri)

                val outFile = File(dir, fileName)
                val outStream = outFile.outputStream()

                try {
                    val buf = ByteArray(512)
                    var read: Int = inStream!!.read(buf)
                    while (read != -1) {
                        outStream.write(buf)
                        read = inStream.read(buf)
                    }
                    callback(File(dir, fileName).absolutePath)
                } catch (e: Exception) {
                    Log.e("MediaManager importMedia", e.message.toString())
                } finally {
                    inStream?.close()
                    outStream.close()
                }
            } catch (e: Exception) {
                Log.e("MediaManager importMedia", e.message.toString())
            }
        }
    }

    suspend fun removeMedia(path: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists()) return@withContext
                file.delete()
            } catch (e: Exception) {
                Log.e("MediaManager removeMedia", e.message.toString())
            }
        }
    }
}
