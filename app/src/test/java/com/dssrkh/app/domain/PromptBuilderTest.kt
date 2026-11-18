package com.dssrkh.app.domain

import com.dssrkh.app.data.db.CharacterEntity
import com.dssrkh.app.data.db.MessageEntity
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private val character = CharacterEntity(
        id = 1,
        name = "林晚",
        personaText = "旧城深夜书店的店主，说话克制。",
    )

    @Test
    fun `system prompt comes first and contains persona`() {
        val messages = PromptBuilder.buildMessages(
            character = character,
            history = listOf(
                MessageEntity(id = 1, sessionId = 1, role = "assistant", rawText = "欢迎光临。", displayText = "欢迎光临。"),
                MessageEntity(id = 2, sessionId = 1, role = "user", rawText = "你好", displayText = "你好"),
            ),
        )

        assertEquals("system", messages.first().role)
        val system = messages.first().content.jsonPrimitive.content
        assertTrue(system.contains("林晚"))
        assertTrue(system.contains("旧城深夜书店"))
        // 开场白被并进 system，历史从第一条 user 开始
        assertTrue(system.contains("欢迎光临"))
        assertEquals(listOf("system", "user"), messages.map { it.role })
    }

    @Test
    fun `assistant history uses raw text with mem tags`() {
        val raw = "晚安。<mem k='event'>他今天很累</mem>"
        val messages = PromptBuilder.buildMessages(
            character = character,
            history = listOf(
                MessageEntity(id = 1, sessionId = 1, role = "user", rawText = "晚安", displayText = "晚安"),
                MessageEntity(id = 2, sessionId = 1, role = "assistant", rawText = raw, displayText = "晚安。"),
            ),
        )
        assertEquals(raw, messages[2].content.jsonPrimitive.content)
    }

    @Test
    fun `skips empty assistant placeholder`() {
        val messages = PromptBuilder.buildMessages(
            character = character,
            history = listOf(
                MessageEntity(id = 1, sessionId = 1, role = "user", rawText = "你好", displayText = "你好"),
                MessageEntity(id = 2, sessionId = 1, role = "assistant", rawText = "", displayText = ""),
            ),
        )
        assertEquals(2, messages.size)
    }

    @Test
    fun `includes memory snapshot when provided`() {
        val messages = PromptBuilder.buildMessages(
            character = character,
            history = emptyList(),
            memorySnapshot = "[m7] 事实·重要4：他怕狗",
        )
        assertTrue(messages.first().content.jsonPrimitive.content.contains("[m7]"))
    }
}
