package com.dssrkh.app.data.tts

import android.content.Context
import com.dssrkh.app.data.SettingsStore
import com.dssrkh.app.data.api.ApiErrorEnvelope
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 官方 TTS：POST /v1/tools/tts，返回 WAV。按（文本 + 音色）哈希缓存到 cacheDir。
 */
class TtsService(
    private val context: Context,
    private val settings: SettingsStore,
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun synthesize(text: String, voiceId: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val key = sha256("$voiceId\n$text")
            val dir = File(context.cacheDir, "tts").apply { mkdirs() }
            val file = File(dir, "$key.wav")
            if (file.exists() && file.length() > 0) return@runCatching file

            val body = buildJsonObject {
                put("text", text)
                put("voice_id", voiceId)
                put("format", "wav")
                put("filter_markdown", true)
            }
            val request = Request.Builder()
                .url("https://api.deepseek.com/v1/tools/tts")
                .header("Authorization", "Bearer ${settings.apiKey}")
                .post(
                    json.encodeToString(JsonObject.serializer(), body)
                        .toRequestBody("application/json; charset=utf-8".toMediaType()),
                )
                .build()

            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val raw = response.body?.string()
                    val message = raw?.let {
                        runCatching {
                            json.decodeFromString(ApiErrorEnvelope.serializer(), it).error.message
                        }.getOrNull()
                    }
                    error(message?.takeIf { it.isNotBlank() } ?: "朗读失败（${response.code}）")
                }
                val bytes = response.body?.bytes() ?: error("朗读响应为空")
                file.writeBytes(bytes)
                file
            }
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
