package com.dssrkh.app.domain

import com.dssrkh.app.data.db.AppDatabase
import com.dssrkh.app.data.db.MemoryEntity
import kotlin.math.pow

/**
 * 记忆的写入、去重、评分与快照生成。所有操作都要求 characterId，
 * 避免跨角色读写。
 */
class MemoryService(private val db: AppDatabase) {

    suspend fun persistDrafts(
        characterId: Long,
        messageId: Long?,
        drafts: List<MemTagParser.MemDraft>,
    ): Int {
        if (characterId == 0L || drafts.isEmpty()) return 0
        var saved = 0
        val dao = db.memoryDao()

        drafts.take(3).forEach { draft ->
            when (draft.op) {
                "add" -> {
                    val active = dao.activeFor(characterId)
                    val duplicate = active.firstOrNull { similarity(it.content, draft.content) >= 0.8 }
                    if (duplicate != null) {
                        dao.update(
                            duplicate.copy(
                                kind = draft.kind,
                                content = draft.content,
                                importance = draft.importance,
                                emotionTags = draft.emotionTags,
                                status = "active",
                                sourceMessageId = messageId,
                            ),
                        )
                    } else {
                        dao.insert(
                            MemoryEntity(
                                characterId = characterId,
                                kind = draft.kind,
                                content = draft.content,
                                importance = draft.importance,
                                emotionTags = draft.emotionTags,
                                sourceMessageId = messageId,
                                createdAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                    saved++
                }
                "update" -> {
                    val target = draft.refId?.let { dao.get(it) }
                    if (target != null && target.characterId == characterId) {
                        dao.update(
                            target.copy(
                                kind = draft.kind,
                                content = draft.content.ifBlank { target.content },
                                importance = draft.importance,
                                emotionTags = draft.emotionTags ?: target.emotionTags,
                                status = "active",
                                sourceMessageId = messageId,
                            ),
                        )
                        saved++
                    }
                }
                "fade" -> {
                    val target = draft.refId?.let { dao.get(it) }
                    if (target != null && target.characterId == characterId) {
                        dao.update(target.copy(status = "faded"))
                        saved++
                    }
                }
            }
        }
        return saved
    }

    /** 生成记忆快照文本，冻结进会话，后续请求原样复用。 */
    suspend fun buildSnapshot(characterId: Long, budgetTokens: Int = 16_000): String {
        val active = db.memoryDao().activeFor(characterId)
        if (active.isEmpty()) return ""
        val now = System.currentTimeMillis()
        val sorted = active.sortedByDescending { score(it, now) }
        val sb = StringBuilder()
        var used = 0
        for (memory in sorted) {
            val line = "[m${memory.id}] ${kindLabel(memory.kind)}·重要${memory.importance}：${memory.content}"
            val estimate = (line.length / 1.5).toInt() + 1
            if (used + estimate > budgetTokens) break
            sb.append(line).append('\n')
            used += estimate
        }
        return sb.toString().trim()
    }

    fun score(memory: MemoryEntity, now: Long): Double {
        val ageDays = ((now - memory.createdAt).coerceAtLeast(0L)) / 86_400_000.0
        val halfLife = when (memory.kind) {
            "user_emotion" -> 3.0
            "event" -> 30.0
            "self_feel" -> 14.0
            "fact" -> 365.0
            "promise" -> Double.MAX_VALUE
            else -> 30.0
        }
        var decay = if (halfLife == Double.MAX_VALUE) 1.0 else 0.5.pow(ageDays / halfLife)
        if (memory.importance >= 4) decay = decay.coerceAtLeast(0.5)
        val emotion = 1.0 + memory.emotionIntensity / 5.0
        val recall = 1.0 + minOf(memory.recallCount, 5) * 0.1
        return memory.importance * emotion * decay * recall
    }

    fun similarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        val ga = bigrams(a)
        val gb = bigrams(b)
        if (ga.isEmpty() || gb.isEmpty()) return if (a == b) 1.0 else 0.0
        val overlap = ga.count { it in gb }
        return 2.0 * overlap / (ga.size + gb.size)
    }

    private fun bigrams(text: String): Set<String> {
        val clean = text.filterNot { it.isWhitespace() }
        if (clean.length < 2) return setOf(clean)
        return (0 until clean.length - 1).mapTo(HashSet()) { clean.substring(it, it + 2) }
    }

    private fun kindLabel(kind: String): String = when (kind) {
        "user_emotion" -> "情绪"
        "event" -> "事件"
        "self_feel" -> "感受"
        "fact" -> "事实"
        "promise" -> "约定"
        else -> kind
    }
}
