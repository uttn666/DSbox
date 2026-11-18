package com.dssrkh.app.domain

import com.dssrkh.app.data.SettingsStore
import com.dssrkh.app.data.api.ChatMessageDto
import com.dssrkh.app.data.api.ChatRequestDto
import com.dssrkh.app.data.api.DeepSeekClient
import com.dssrkh.app.data.api.StreamEvent
import com.dssrkh.app.data.api.ThinkingDto
import com.dssrkh.app.data.api.UsageDto
import com.dssrkh.app.data.db.AppDatabase
import com.dssrkh.app.data.db.CharacterEntity
import com.dssrkh.app.data.db.MessageEntity
import com.dssrkh.app.data.db.SessionEntity
import com.dssrkh.app.data.db.SummaryEntity
import com.dssrkh.app.data.db.UsageRecordEntity
import kotlinx.coroutines.flow.collect

/**
 * 上下文压缩：把历史整理成摘要 + 记忆标签，然后把旧消息标记为 compacted，
 * 之后的请求只带摘要、快照与最近未压缩的原文。
 */
class CompactionService(
    private val db: AppDatabase,
    private val client: DeepSeekClient,
    private val memory: MemoryService,
) {

    suspend fun compact(
        session: SessionEntity,
        character: CharacterEntity,
        model: String,
        thinkingEnabled: Boolean,
        effort: String,
    ): Result<Unit> = runCatching {
        val history = db.messageDao()
            .listBySession(session.id)
            .filter { !it.compacted && it.role != "divider" }
        if (history.isEmpty()) error("没有可整理的内容")

        val previousSummary = db.summaryDao().latest(session.id)?.text
        val messages = PromptBuilder.buildMessages(
            character = character,
            history = history,
            memorySnapshot = session.memorySnapshot,
            summary = previousSummary,
        ) + ChatMessageDto(role = "user", content = INSTRUCTION)

        val request = ChatRequestDto(
            model = model,
            messages = messages,
            thinking = ThinkingDto(if (thinkingEnabled) "enabled" else "disabled"),
            reasoningEffort = if (thinkingEnabled) {
                if (effort == SettingsStore.EFFORT_LOW) SettingsStore.EFFORT_LOW else SettingsStore.EFFORT_HIGH
            } else {
                null
            },
            maxTokens = 16_000,
        )

        val content = StringBuilder()
        var failure: String? = null
        var usage: UsageDto? = null
        client.streamChat(request).collect { event ->
            when (event) {
                is StreamEvent.Content -> content.append(event.text)
                is StreamEvent.Usage -> usage = event.usage
                is StreamEvent.Failed -> failure = event.message
                else -> Unit
            }
        }
        failure?.let { error(it) }

        val raw = content.toString()
        val summary = SUMMARY.find(raw)?.groupValues?.get(1)?.trim()
        if (summary.isNullOrBlank()) error("整理结果里没有摘要，可能被截断")

        val parsed = MemTagParser.parse(raw)
        if (parsed.drafts.isNotEmpty()) {
            memory.persistDrafts(
                characterId = character.id,
                messageId = null,
                drafts = parsed.drafts,
            )
        }

        db.summaryDao().insert(
            SummaryEntity(
                sessionId = session.id,
                epoch = session.compactionEpoch + 1,
                text = summary,
                tokenEstimate = (summary.length / 1.5).toLong(),
            ),
        )
        db.messageDao().markAllCompacted(session.id)
        db.sessionDao().incrementEpoch(session.id)
        db.sessionDao().updateSnapshot(session.id, memory.buildSnapshot(character.id))
        db.sessionDao().updateLastPromptTokens(session.id, 0)
        db.sessionDao().touch(session.id)

        usage?.let {
            val peak = CostCalculator.isPeak(System.currentTimeMillis())
            db.usageDao().insert(
                UsageRecordEntity(
                    sessionId = session.id,
                    characterId = character.id,
                    kind = "compaction",
                    model = model,
                    inputHit = it.promptCacheHitTokens,
                    inputMiss = it.promptCacheMissTokens,
                    output = it.completionTokens,
                    reasoningTokens = it.completionDetails?.reasoningTokens ?: 0,
                    estCostCny = CostCalculator.costCny(
                        model = model,
                        peak = peak,
                        hit = it.promptCacheHitTokens,
                        miss = it.promptCacheMissTokens,
                        output = it.completionTokens,
                    ),
                    peak = peak,
                ),
            )
        }

        db.messageDao().insert(
            MessageEntity(
                sessionId = session.id,
                role = "divider",
                rawText = "",
                displayText = "已整理记忆 · 压缩上下文",
                status = "done",
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    companion object {
        private val SUMMARY = Regex(
            """<summary>(.*?)</summary>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )

        private val INSTRUCTION = """
            现在暂停角色扮演，整理上面的对话。请只输出两部分，不要输出其他内容：
            1. 最多 3 条记忆标签，格式与之前相同：<mem k='...' i='...'>...</mem>，侧重当前剧情、对方的情绪与状态、你们的关系变化、未完成的约定。
            2. 用 <summary> 和 </summary> 包住一段不超过 800 字的会话摘要。摘要包含：剧情进展、关系状态、称呼与语气约定、未完事项、近期情绪基调、设定变更。
        """.trimIndent()
    }
}
