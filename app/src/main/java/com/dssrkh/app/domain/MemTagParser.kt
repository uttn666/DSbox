package com.dssrkh.app.domain

/**
 * 解析已完成回复中的记忆标签。数据库存原文，界面用 displayText。
 */
object MemTagParser {

    private val BLOCK = Regex(
        """<mem\b([^>]*?)>(.*?)</mem>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val SELF = Regex("""<mem\b([^>]*?)/>""", RegexOption.IGNORE_CASE)
    private val ATTR = Regex("""(\w+)\s*=\s*(['"])(.*?)\2""")

    data class MemDraft(
        val op: String,
        val refId: Long?,
        val kind: String,
        val content: String,
        val importance: Int,
        val emotionTags: String?,
    )

    data class Parsed(
        val displayText: String,
        val drafts: List<MemDraft>,
    )

    val KINDS = setOf("user_emotion", "event", "self_feel", "fact", "promise")

    fun parse(raw: String): Parsed {
        val drafts = mutableListOf<MemDraft>()
        val ranges = mutableListOf<IntRange>()

        BLOCK.findAll(raw).forEach { m ->
            val attrs = parseAttrs(m.groupValues[1])
            ranges += m.range
            buildDraft(attrs, m.groupValues[2].trim())?.let(drafts::add)
        }
        SELF.findAll(raw).forEach { m ->
            val attrs = parseAttrs(m.groupValues[1])
            ranges += m.range
            buildDraft(attrs, "")?.let(drafts::add)
        }

        val display = removeRanges(raw, ranges).trim()
        return Parsed(display, drafts)
    }

    private fun parseAttrs(text: String): Map<String, String> =
        ATTR.findAll(text).associate { it.groupValues[1].lowercase() to it.groupValues[3] }

    private fun buildDraft(attrs: Map<String, String>, rawContent: String): MemDraft? {
        val op = (attrs["op"] ?: "add").lowercase()
        if (op !in setOf("add", "update", "fade")) return null
        val kind = (attrs["k"] ?: "fact").lowercase()
        if (kind !in KINDS) return null
        val refId = attrs["id"]?.toLongOrNull()
        if ((op == "update" || op == "fade") && refId == null) return null
        val content = rawContent.take(120)
        if (op != "fade" && content.isBlank()) return null
        val importance = (attrs["i"]?.toIntOrNull() ?: 3).coerceIn(1, 5)
        return MemDraft(
            op = op,
            refId = refId,
            kind = kind,
            content = content,
            importance = importance,
            emotionTags = attrs["e"]?.takeIf { it.isNotBlank() },
        )
    }

    private fun removeRanges(text: String, ranges: List<IntRange>): String {
        if (ranges.isEmpty()) return text
        val sorted = ranges.sortedBy { it.first }
        val sb = StringBuilder(text.length)
        var cursor = 0
        for (r in sorted) {
            if (r.first < cursor) continue
            sb.append(text, cursor, r.first)
            cursor = r.last + 1
        }
        if (cursor < text.length) sb.append(text, cursor, text.length)
        return sb.toString()
    }
}
