package com.dssrkh.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 图片导入：长边压到 2048 px，JPEG 85，存进应用私有目录。 */
object ImageStore {

    suspend fun saveImage(context: Context, uri: Uri): LocalAttachment? = withContext(Dispatchers.IO) {
        runCatching {
            val original = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@withContext null

            val maxSide = 2048
            val longest = maxOf(original.width, original.height)
            val scaled = if (longest > maxSide) {
                val ratio = maxSide.toFloat() / longest
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * ratio).toInt().coerceAtLeast(1),
                    (original.height * ratio).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                original
            }

            val dir = File(context.filesDir, "attachments").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled !== original) scaled.recycle()
            original.recycle()

            LocalAttachment(
                kind = "image",
                path = file.absolutePath,
                mime = "image/jpeg",
                name = file.name,
            )
        }.getOrNull()
    }
}
