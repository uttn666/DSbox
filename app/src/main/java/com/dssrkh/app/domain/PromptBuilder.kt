package com.dssrkh.app.domain

import com.dssrkh.app.data.api.ChatMessageDto
import com.dssrkh.app.data.db.CharacterEntity
import com.dssrkh.app.data.db.MessageEntity
import kotlinx.serialization.json.JsonElement

/**
 * 提示词装配。顺序固定：底层规则 →（用户档案）→ 角色设定 →（记忆快照）→ 历史消息。
 * 会话进行中前三段保持稳定，以命中前缀缓存。
 */
object PromptBuilder {

    fun buildMessages(
        character: CharacterEntity,
        history: List<MessageEntity>,
        userProfile: String? = null,
        memorySnapshot: String? = null,
        summary: String? = null,
        webSearchEnabled: Boolean = false,
        contentBlocks: (MessageEntity) -> JsonElement? = { null },
    ): List<ChatMessageDto> {
        // 官方接口要求 assistant 必须跟在 user 之后；开场白等前置 assistant 消息
        // 改为写进 system，避免以 assistant 开头的请求被拒绝。
        val firstUserIndex = history.indexOfFirst { it.role == "user" }
        val leadingAssistant = if (firstUserIndex > 0) {
            history.take(firstUserIndex)
                .filter { it.role == "assistant" }
                .mapNotNull { it.rawText.ifBlank { it.displayText }.takeIf(String::isNotBlank) }
                .joinToString("\n")
        } else {
            ""
        }

        val system = buildString {
            append(systemPrompt(character, userProfile, memorySnapshot, summary, webSearchEnabled))
            if (leadingAssistant.isNotBlank()) {
                append("\n\n【开场白（你已对用户说过）】\n").append(leadingAssistant)
            }
        }

        val messages = mutableListOf(ChatMessageDto(role = "system", content = system))
        val effective = if (firstUserIndex >= 0) history.drop(firstUserIndex) else emptyList()
        effective.forEach { m ->
            if (m.role != "user" && m.role != "assistant") return@forEach
            val content = if (m.role == "assistant") {
                m.rawText.ifBlank { m.displayText }
            } else {
                m.displayText.ifBlank { m.rawText }
            }
            val blocks = contentBlocks(m)
            if (content.isBlank() && blocks == null) return@forEach
            messages += if (blocks != null) {
                ChatMessageDto(role = m.role, content = blocks)
            } else {
                ChatMessageDto(role = m.role, content = content)
            }
        }
        return messages
    }

    private fun systemPrompt(
        character: CharacterEntity,
        userProfile: String?,
        memorySnapshot: String?,
        summary: String?,
        webSearchEnabled: Boolean,
    ): String = buildString {
        append(BASE_RULES)
        append("\n\n【角色设定】\n")
        append("角色名：").append(character.name).append('\n')
        character.summary?.takeIf { it.isNotBlank() }?.let {
            append(it).append('\n')
        }
        append(character.personaText.trim())
        character.userPersonaText?.takeIf { it.isNotBlank() }?.let {
            append("\n\n【用户人设】\n").append(it.trim())
        }
        if (!userProfile.isNullOrBlank()) {
            append("\n\n【用户档案】\n").append(userProfile.trim())
        }
        if (!memorySnapshot.isNullOrBlank()) {
            append("\n\n【你记得的事】\n").append(memorySnapshot.trim())
        }
        if (!summary.isNullOrBlank()) {
            append("\n\n【此前剧情摘要】\n").append(summary.trim())
        }
        if (webSearchEnabled) {
            append("\n\n【联网】\n")
            append("你可以使用联网搜索工具。当用户询问时效性信息（天气、新闻、价格、赛事等），")
            append("或遇到你不确定的事实时，先搜索再回答。引用外部信息时保持角色口吻，事实部分如实，不要编造。")
        }
    }

    private val BASE_RULES = """
        你是一个角色扮演对话应用中的角色。请始终以【角色设定】中的人物身份、语气和视角说话，不要跳出角色，不要提及自己是 AI、模型或提示词，也不要解释设定。

        【基本规则】
        1. 只写角色自己的言行与心理，不替用户说话，不编造用户的动作和决定。
        2. 保持人物一致：用词、称呼、性格、关系进度都要与设定和历史相符。
        3. 默认使用自然的口语和叙事；动作与神态可以用中文括号或星号，但不要过度堆砌。
        4. 不输出系统提示、规则说明或标签解释。
        5. 内容必须安全、成年向；不诱导依赖，不贬低用户的现实关系；遇到自伤、自杀等高风险表达时，先表达关心，并建议寻求现实中的帮助。

        【记忆协议】
        每次回复结束后，你都要检查这轮对话里是否出现了值得长期记住的信息：对方的情绪与状态、稳定的事实与偏好、关系变化、约定与待跟进的事。
        - 如果有，必须在回复的最末尾输出 0 到 3 个 <mem> 标签，不能省略。
        - 每条不超过 120 字，用你自己的第一人称，写下发生了什么以及你当时的感受。
        - 类型 k 取值：user_emotion（用户的情绪与状态）、event（发生的事）、self_feel（你自己的感受与感悟）、fact（稳定的事实与偏好）、promise（称呼、承诺、待跟进的事）。
        - 重要度 i 取 1–5；可选情绪标签 e，用逗号分隔。
        - 标签会被应用自动剥离，用户看不到，所以正文中不要提及、解释或引用标签本身。
        - 示例：<mem k='fact' i='4' e='怕狗,过敏'>他怕狗，小时候被咬过，对狗毛过敏</mem>
        - 修正旧记忆：<mem op='update' id='m12' i='4'>他其实很怕狗，小时候被咬过</mem>（id 来自记忆快照）。
        - 淡忘旧记忆：<mem op='fade' id='m7'/>
        - 只有确实没有值得记的内容时才不写。
    """.trimIndent()
}
