package com.dssrkh.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemTagParserTest {

    @Test
    fun `parses add tag and strips it from display`() {
        val raw = "好，我记住了。<mem k='event' i='4' e='疲惫,逞强'>他今天加班很累</mem>"
        val parsed = MemTagParser.parse(raw)

        assertEquals("好，我记住了。", parsed.displayText)
        assertEquals(1, parsed.drafts.size)
        val draft = parsed.drafts.first()
        assertEquals("add", draft.op)
        assertEquals("event", draft.kind)
        assertEquals(4, draft.importance)
        assertEquals("疲惫,逞强", draft.emotionTags)
        assertEquals("他今天加班很累", draft.content)
    }

    @Test
    fun `parses update and fade`() {
        val raw = "好。<mem op='update' id='12' i='5'>他其实怕狗</mem><mem op='fade' id='7'/>"
        val parsed = MemTagParser.parse(raw)

        assertEquals("好。", parsed.displayText)
        assertEquals(2, parsed.drafts.size)
        assertEquals("update", parsed.drafts[0].op)
        assertEquals(12L, parsed.drafts[0].refId)
        assertEquals("fade", parsed.drafts[1].op)
        assertEquals(7L, parsed.drafts[1].refId)
    }

    @Test
    fun `drops invalid kind`() {
        val parsed = MemTagParser.parse("正文<mem k='bogus'>内容</mem>")
        assertEquals("正文", parsed.displayText)
        assertTrue(parsed.drafts.isEmpty())
    }

    @Test
    fun `drops update without id`() {
        val parsed = MemTagParser.parse("正文<mem op='update'>内容</mem>")
        assertTrue(parsed.drafts.isEmpty())
    }

    @Test
    fun `truncates long content to 120 chars`() {
        val longText = "记".repeat(200)
        val parsed = MemTagParser.parse("<mem k='fact'>$longText</mem>")
        assertEquals(120, parsed.drafts.first().content.length)
    }

    @Test
    fun `defaults importance and kind`() {
        val parsed = MemTagParser.parse("<mem>他说喜欢雨天</mem>")
        val draft = parsed.drafts.first()
        assertEquals("fact", draft.kind)
        assertEquals(3, draft.importance)
    }

    @Test
    fun `leaves plain text untouched`() {
        val parsed = MemTagParser.parse("只是普通回复，没有标签")
        assertEquals("只是普通回复，没有标签", parsed.displayText)
        assertTrue(parsed.drafts.isEmpty())
    }
}
