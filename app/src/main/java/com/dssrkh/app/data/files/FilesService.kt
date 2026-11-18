package com.dssrkh.app.data.files

import com.dssrkh.app.data.SettingsStore
import com.dssrkh.app.data.api.ApiErrorEnvelope
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

/** Files API：上传并解析文档，返回 file_id。 */
class FilesService(private val settings: SettingsStore) {

    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    @Serializable
    data class Uploaded(
        val id: String,
        val filename: String? = null,
        val bytes: Long = 0,
        val purpose: String = "",
    )

    @Serializable
    private data class UploadResponse(
        val id: String = "",
        val filename: String? = null,
        val bytes: Long = 0,
        val purpose: String? = null,
    )

    suspend fun upload(file: File, mime: String?, purpose: String): Result<Uploaded> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("purpose", purpose)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody((mime ?: "application/octet-stream").toMediaType()),
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.deepseek.com/files")
                    .header("Authorization", "Bearer ${settings.apiKey}")
                    .post(body)
                    .build()

                http.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val message = runCatching {
                            json.decodeFromString(ApiErrorEnvelope.serializer(), raw).error.message
                        }.getOrNull()
                        error(message?.takeIf { it.isNotBlank() } ?: "文档上传失败（${response.code}）")
                    }
                    val parsed = json.decodeFromString(UploadResponse.serializer(), raw)
                    if (parsed.id.isBlank()) error("文档上传返回为空")
                    Uploaded(
                        id = parsed.id,
                        filename = parsed.filename,
                        bytes = parsed.bytes,
                        purpose = parsed.purpose ?: purpose,
                    )
                }
            }
        }

    companion object {
        private val PARSED_EXTENSIONS = setOf("pdf", "doc", "docx", "ppt", "pptx")

        fun purposeFor(filename: String): String {
            val ext = filename.substringAfterLast('.', "").lowercase()
            return if (ext in PARSED_EXTENSIONS) "text_recognition" else "user_data"
        }
    }
}
