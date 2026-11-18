package com.dssrkh.app.data

import android.util.Base64
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LocalAttachment(
    val kind: String = "image",
    val path: String,
    val mime: String? = "image/jpeg",
    val name: String? = null,
    val fileId: String? = null,
    val purpose: String? = null,
    val bytes: Long? = null,
)

object AttachmentCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(items: List<LocalAttachment>): String =
        json.encodeToString(items)

    fun decode(raw: String?): List<LocalAttachment> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<LocalAttachment>>(raw) }
            .getOrDefault(emptyList())
    }

    fun dataUrl(attachment: LocalAttachment): String? = runCatching {
        val bytes = File(attachment.path).readBytes()
        val mime = attachment.mime ?: "image/jpeg"
        "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()
}
