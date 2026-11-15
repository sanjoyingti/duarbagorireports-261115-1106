package com.arin.civicreport.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaStore(private val context: Context) {

    private fun mediaDir(): File =
        File(context.filesDir, "media").also { if (!it.exists()) it.mkdirs() }

    fun newPhotoFile(): File =
        File(mediaDir(), "photo_${System.currentTimeMillis()}.jpg")

    fun newVideoFile(): File =
        File(mediaDir(), "video_${System.currentTimeMillis()}.mp4")

    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copies picked gallery content into the app folder so the URI never expires. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(mediaDir(), "import_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.absolutePath
        }.getOrNull()
    }

    fun deleteFile(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
