package com.dssrkh.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 通过 SAF 选择的文档复制到应用私有目录，等待上传。 */
object FileStore {

    suspend fun saveDocument(context: Context, uri: Uri): LocalAttachment? = withContext(Dispatchers.IO) {
        runCatching {
            val name = queryName(context, uri) ?: "document"
            val mime = context.contentResolver.getType(uri)
            val dir = File(context.filesDir, "attachments").apply { mkdirs() }
            val safeName = name.replace(Regex("""[\\/:*?"<>|]"""), "_")
            val file = File(dir, "${UUID.randomUUID()}_$safeName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null

            LocalAttachment(
                kind = "file",
                path = file.absolutePath,
                mime = mime,
                name = name,
                bytes = file.length(),
            )
        }.getOrNull()
    }

    private fun queryName(context: Context, uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
    }
}
