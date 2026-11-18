package com.dssrkh.app.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** 解析服务端返回的 annotations / server_tool_calls，供界面展示搜索与引用。 */
object SearchBlocks {

    data class Info(
        val searchCount: Int,
        val citationCount: Int,
        val titles: List<String>,
    )

    fun parse(raw: String?): Info? {
        if (raw.isNullOrBlank()) return null
        val root = runCatching { Json.parseToJsonElement(raw) }.getOrNull() as? JsonObject ?: return null
        val citations = runCatching { root["annotations"]?.jsonArray?.size ?: 0 }.getOrDefault(0)

        var searches = 0
        val titles = mutableListOf<String>()
        runCatching {
            root["server_tool_calls"]?.jsonArray?.forEach { group ->
                val calls = (group as? JsonObject)?.get("calls")?.jsonArray ?: return@forEach
                calls.forEach { call ->
                    val obj = call as? JsonObject ?: return@forEach
                    val type = obj["type"]?.jsonPrimitive?.contentOrNull
                    if (type == "deepseek_web_search" || type == "deepseek_read_url") searches++
                    obj["sources"]?.jsonArray?.forEach { source ->
                        val title = (source as? JsonObject)?.get("title")?.jsonPrimitive?.contentOrNull
                        if (!title.isNullOrBlank() && titles.size < 5) titles.add(title)
                    }
                }
            }
        }
        if (citations == 0 && searches == 0) return null
        return Info(
            searchCount = searches,
            citationCount = citations,
            titles = titles.distinct().take(3),
        )
    }
}
