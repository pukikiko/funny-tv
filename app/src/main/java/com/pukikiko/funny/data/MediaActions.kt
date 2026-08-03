package com.pukikiko.funny.data

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.pukikiko.funny.api.RetrofitClient
import com.pukikiko.funny.api.UploadResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/** Mirrors the server's upload constraints so bad files fail before the round trip. */
object UploadLimits {
    const val MAX_BYTES = 50L * 1024 * 1024
    val ALLOWED_EXTENSIONS = setOf("mp4", "webm", "ogg")
}

/** Public URL of a video on the configured instance. */
fun videoUrl(instanceUrl: String, filename: String): String {
    val base = if (instanceUrl.endsWith("/")) instanceUrl else "$instanceUrl/"
    return "${base}videos/${Uri.encode(filename)}"
}

fun copyToClipboard(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("funny", url))
}

fun shareLink(context: Context, url: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    val chooser = Intent.createChooser(send, "Share video").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

/**
 * Queues the video with the system download manager. On Q+ it lands in the
 * public Downloads folder; older releases would need WRITE_EXTERNAL_STORAGE for
 * that, so they get the app's external files directory instead and no prompt.
 */
fun downloadVideo(context: Context, url: String, filename: String): String {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(filename)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        manager.enqueue(request)
        "Downloading to Downloads"
    } else {
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename)
        manager.enqueue(request)
        "Downloading to app storage"
    }
}

private fun queryName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        val column = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (column >= 0 && it.moveToFirst()) return it.getString(column) ?: "upload.mp4"
    }
    return uri.lastPathSegment ?: "upload.mp4"
}

/**
 * Streams the picked file into the instance's moderation queue. Returns the
 * message to show the user, whether it succeeded or not.
 */
suspend fun uploadVideo(context: Context, instanceUrl: String, uri: Uri): String = withContext(Dispatchers.IO) {
    val name = queryName(context, uri)
    val extension = name.substringAfterLast('.', "").lowercase()
    if (extension !in UploadLimits.ALLOWED_EXTENSIONS) {
        return@withContext "Only ${UploadLimits.ALLOWED_EXTENSIONS.joinToString(", ")} files are accepted"
    }

    // The multipart body needs a length up front, so spool the content to the
    // cache dir rather than streaming straight off the resolver.
    val temp = File.createTempFile("upload", ".$extension", context.cacheDir)
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: return@withContext "Could not read that file"

        if (temp.length() > UploadLimits.MAX_BYTES) {
            return@withContext "That file is over the 50MB limit"
        }

        val mediaType = context.contentResolver.getType(uri)?.toMediaTypeOrNull()
            ?: "video/$extension".toMediaTypeOrNull()
        val part = MultipartBody.Part.createFormData("video", name, temp.asRequestBody(mediaType))

        val response = RetrofitClient.getApi(instanceUrl).uploadVideo(part)
        if (response.isSuccessful) {
            response.body()?.message ?: "Uploaded, pending moderation"
        } else if (response.code() == 429) {
            "Upload limit reached, try again later"
        } else {
            val body = response.errorBody()?.string()
            val parsed = body?.let {
                runCatching { Gson().fromJson(it, UploadResponse::class.java) }.getOrNull()
            }
            parsed?.error ?: "Upload failed (${response.code()})"
        }
    } catch (e: Exception) {
        "Upload failed: ${e.message ?: "unknown error"}"
    } finally {
        temp.delete()
    }
}
