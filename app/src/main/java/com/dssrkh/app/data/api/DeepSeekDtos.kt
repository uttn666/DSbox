package com.dssrkh.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ChatRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = true,
    @SerialName("stream_options") val streamOptions: StreamOptionsDto? = StreamOptionsDto(),
    val thinking: ThinkingDto? = null,
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null,
    val title: TitleDto? = null,
    val tools: List<ToolDto>? = null,
)

@Serializable
data class ToolDto(val type: String)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: JsonElement,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
) {
    constructor(role: String, content: String) : this(role, JsonPrimitive(content))
}

@Serializable
data class StreamOptionsDto(
    @SerialName("include_usage") val includeUsage: Boolean = true,
)

@Serializable
data class ThinkingDto(val type: String)

@Serializable
data class TitleDto(val enabled: Boolean = true)

@Serializable
data class StreamChunkDto(
    val choices: List<StreamChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
)

@Serializable
data class StreamChoiceDto(
    val delta: DeltaDto = DeltaDto(),
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class DeltaDto(
    val role: String? = null,
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    val title: String? = null,
    val annotations: JsonElement? = null,
    @SerialName("server_tool_calls") val serverToolCalls: JsonElement? = null,
)

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens") val promptTokens: Long = 0,
    @SerialName("completion_tokens") val completionTokens: Long = 0,
    @SerialName("total_tokens") val totalTokens: Long = 0,
    @SerialName("prompt_cache_hit_tokens") val promptCacheHitTokens: Long = 0,
    @SerialName("prompt_cache_miss_tokens") val promptCacheMissTokens: Long = 0,
    @SerialName("completion_tokens_details") val completionDetails: CompletionDetailsDto? = null,
    @SerialName("server_tool_use") val serverToolUse: ServerToolUseDto? = null,
)

@Serializable
data class ServerToolUseDto(
    @SerialName("tool_search") val toolSearch: Int = 0,
    @SerialName("tool_fetch") val toolFetch: Int = 0,
    @SerialName("tool_image_search") val toolImageSearch: Int = 0,
)

@Serializable
data class CompletionDetailsDto(
    @SerialName("reasoning_tokens") val reasoningTokens: Long = 0,
)

@Serializable
data class ApiErrorEnvelope(val error: ApiError)

@Serializable
data class ApiError(
    val message: String = "",
    val type: String? = null,
    val code: String? = null,
)

sealed interface StreamEvent {
    data class Reasoning(val text: String) : StreamEvent
    data class Content(val text: String) : StreamEvent
    data class Title(val text: String) : StreamEvent
    data class Usage(val usage: UsageDto) : StreamEvent
    data class Blocks(val annotationsJson: String, val toolCallsJson: String) : StreamEvent
    data object Done : StreamEvent
    data class Failed(val message: String, val httpCode: Int? = null) : StreamEvent
}
