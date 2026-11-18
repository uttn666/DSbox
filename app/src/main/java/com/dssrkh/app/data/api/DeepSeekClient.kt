package com.dssrkh.app.data.api

import com.dssrkh.app.data.SettingsStore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class DeepSeekClient(private val settings: SettingsStore) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun streamChat(request: ChatRequestDto): Flow<StreamEvent> = callbackFlow {
        val payload = json.encodeToString(ChatRequestDto.serializer(), request)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val httpRequest = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .header("Authorization", "Bearer ${settings.apiKey}")
            .header("Accept", "text/event-stream")
            .post(payload)
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    trySend(StreamEvent.Done)
                    close()
                    return
                }
                val chunk = runCatching {
                    json.decodeFromString(StreamChunkDto.serializer(), data)
                }.getOrNull() ?: return

                val delta = chunk.choices.firstOrNull()?.delta
                delta?.title?.takeIf { it.isNotBlank() }?.let { trySend(StreamEvent.Title(it)) }
                delta?.reasoningContent?.takeIf { it.isNotEmpty() }?.let {
                    trySend(StreamEvent.Reasoning(it))
                }
                delta?.content?.takeIf { it.isNotEmpty() }?.let {
                    trySend(StreamEvent.Content(it))
                }
                if (delta?.annotations != null || delta?.serverToolCalls != null) {
                    trySend(
                        StreamEvent.Blocks(
                            annotationsJson = delta?.annotations?.toString() ?: "[]",
                            toolCallsJson = delta?.serverToolCalls?.toString() ?: "[]",
                        ),
                    )
                }
                chunk.usage?.let { trySend(StreamEvent.Usage(it)) }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val code = response?.code
                val raw = runCatching { response?.body?.string() }.getOrNull()
                val message = raw?.let(::parseErrorMessage)
                    ?: t?.message
                    ?: "网络请求失败"
                trySend(StreamEvent.Failed(message, code))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val source = EventSources.createFactory(http).newEventSource(httpRequest, listener)
        awaitClose { source.cancel() }
    }

    private fun parseErrorMessage(raw: String): String? = runCatching {
        json.decodeFromString(ApiErrorEnvelope.serializer(), raw).error.message.ifBlank { null }
    }.getOrNull()

    companion object {
        const val BASE_URL = "https://api.deepseek.com"
    }
}
