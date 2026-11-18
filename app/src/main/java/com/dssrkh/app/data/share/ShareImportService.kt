package com.dssrkh.app.data.share

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** 通过官方分享链接读取会话：GET /api/v0/share/content?share_id=... */
class ShareImportService {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    data class ImportedConversation(
        val shareId: String,
        val title: String,
        val modelType: String?,
        val messages: List<ImportedMessage>,
    )

    data class ImportedMessage(
        val role: String,
        val content: String,
        val thinking: String?,
        val fileNames: List<String>,
    )

    @Serializable
    private data class Envelope(
        val code: Int = 0,
        val msg: String = "",
        val data: ShareData? = null,
    )

    @Serializable
    private data class ShareData(
        @SerialName("biz_code") val bizCode: Int = 0,
        @SerialName("biz_msg") val bizMsg: String = "",
        @SerialName("biz_data") val bizData: BizData? = null,
    )

    @Serializable
    private data class BizData(
        val title: String? = null,
        @SerialName("model_type") val modelType: String? = null,
        val messages: List<ShareMessage> = emptyList(),
    )

    @Serializable
    private data class ShareMessage(
        @SerialName("message_id") val messageId: Long = 0,
        @SerialName("parent_id") val parentId: Long? = null,
        val role: String = "",
        val status: String? = null,
        val fragments: List<ShareFragment> = emptyList(),
    )

    @Serializable
    private data class ShareFragment(
        val type: String = "",
        val content: String? = null,
        val files: List<ShareFile> = emptyList(),
    )

    @Serializable
    private data class ShareFile(
        @SerialName("file_name") val fileName: String? = null,
        @SerialName("model_kind") val modelKind: String? = null,
        val status: String? = null,
    )

    fun extractShareId(input: String): String? {
        val text = input.trim()
        val match = SHARE_URL.find(text)
        if (match != null) return match.groupValues[1]
        return text.takeIf { it.matches(Regex("[A-Za-z0-9_-]{8,128}")) }
    }

    suspend fun fetch(input: String): Result<ImportedConversation> = withContext(Dispatchers.IO) {
        runCatching {
            val shareId = extractShareId(input) ?: error("无法识别分享链接")
            val request = Request.Builder()
                .url("$BASE_URL/api/v0/share/content?share_id=$shareId")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Android) DSbox")
                .get()
                .build()

            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("读取分享失败（HTTP ${response.code}）")
                val raw = response.body?.string() ?: error("分享内容为空")
                val envelope = json.decodeFromString(Envelope.serializer(), raw)
                if (envelope.code != 0) {
                    error(envelope.msg.ifBlank { "读取分享失败" })
                }
                val share = envelope.data ?: error("分享数据为空")
                if (share.bizCode != 0) {
                    error(
                        when (share.bizCode) {
                            1 -> "分享不存在或已失效"
                            2 -> "分享已删除或封禁"
                            else -> share.bizMsg.ifBlank { "读取分享失败（${share.bizCode}）" }
                        },
                    )
                }
                val data = share.bizData ?: error("分享内容为空")
                val chain = selectCurrentBranch(data.messages)
                val messages = chain.mapNotNull { message ->
                    val role = message.role.uppercase()
                    if (role != "USER" && role != "ASSISTANT") return@mapNotNull null
                    val request = message.fragments
                        .filter { it.type == "REQUEST" }
                        .mapNotNull { it.content }
                        .joinToString("\n\n")
                    val responseText = message.fragments
                        .filter { it.type == "RESPONSE" }
                        .mapNotNull { it.content }
                        .joinToString("\n\n")
                    val thinking = message.fragments
                        .filter { it.type == "THINK" }
                        .mapNotNull { it.content }
                        .joinToString("\n\n")
                    val files = message.fragments
                        .flatMap { it.files }
                        .mapNotNull { it.fileName }
                    val content = if (role == "USER") request else responseText
                    if (content.isBlank() && thinking.isBlank() && files.isEmpty()) return@mapNotNull null
                    ImportedMessage(
                        role = role,
                        content = content,
                        thinking = thinking.takeIf { it.isNotBlank() },
                        fileNames = files,
                    )
                }
                if (messages.isEmpty()) error("这个分享里没有可导入的消息")
                ImportedConversation(
                    shareId = shareId,
                    title = data.title.orEmpty(),
                    modelType = data.modelType,
                    messages = messages,
                )
            }
        }
    }

    private fun selectCurrentBranch(messages: List<ShareMessage>): List<ShareMessage> {
        if (messages.isEmpty()) return emptyList()
        val byId = messages.associateBy { it.messageId }
        val children = messages.groupBy { it.parentId }
        val leaves = messages.filter { children[it.messageId].isNullOrEmpty() }
        val leaf = leaves.maxByOrNull { it.messageId } ?: messages.maxByOrNull { it.messageId } ?: return emptyList()
        val chain = ArrayDeque<ShareMessage>()
        val visited = mutableSetOf<Long>()
        var current: ShareMessage? = leaf
        while (current != null && visited.add(current.messageId)) {
            chain.addFirst(current)
            current = current.parentId?.let { byId[it] }
        }
        return chain.toList()
    }

    companion object {
        private const val BASE_URL = "https://chat.deepseek.com"
        private val SHARE_URL = Regex("""/share/([A-Za-z0-9_-]+)""")
    }
}
