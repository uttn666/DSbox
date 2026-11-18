package com.dssrkh.app.data.asr

import com.dssrkh.app.data.SettingsStore
import com.dssrkh.app.data.api.ApiErrorEnvelope
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

/** 官方 ASR：POST /v1/tools/asr，multipart 字段名 file。 */
class AsrService(private val settings: SettingsStore) {

    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    @Serializable
    private data class AsrResponse(
        @SerialName("request_id") val requestId: String? = null,
        val text: String = "",
    )

    suspend fun transcribe(file: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("audio/wav".toMediaType()),
                )
                .build()

            val request = Request.Builder()
                .url("https://api.deepseek.com/v1/tools/asr")
                .header("Authorization", "Bearer ${settings.apiKey}")
                .post(body)
                .build()

            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        json.decodeFromString(ApiErrorEnvelope.serializer(), raw).error.message
                    }.getOrNull()
                    error(message?.takeIf { it.isNotBlank() } ?: "语音识别失败（${response.code}）")
                }
                json.decodeFromString(AsrResponse.serializer(), raw).text.trim()
            }
        }
    }
}
