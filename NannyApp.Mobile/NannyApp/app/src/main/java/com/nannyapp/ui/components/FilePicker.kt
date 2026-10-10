package com.nannyapp.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Returns a trigger function that opens the system file/image picker; when the
 * person picks something, [onPicked] is called with the raw bytes and a
 * best-effort file name, ready to hand to a multipart upload call (avatar,
 * portfolio documents, etc.). Reading happens off using the content resolver
 * so it works for any storage provider (gallery, Drive, Files app, ...).
 */
@Composable
fun rememberFilePickerLauncher(onPicked: (bytes: ByteArray, fileName: String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onPicked)
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val (bytes, name) = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count == -1) break
                            require(output.size() + count <= 5 * 1024 * 1024) { "Choose an image smaller than 5 MB." }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    } ?: error("Could not open this image.")
                    bytes to (queryDisplayName(context, uri) ?: "upload_${System.currentTimeMillis()}.jpg")
                }
                callback.value(bytes, name)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(context, if (error is IllegalArgumentException) error.message else "Could not read this image. Please try another file.", Toast.LENGTH_LONG).show()
            }
        }
    }

    return { launcher.launch("image/*") }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull()
}
