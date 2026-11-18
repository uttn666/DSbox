package com.dssrkh.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemTagFilterTest {

    @Test
    fun `removes complete tag`() {
        val filter = MemTagFilter()
        val out = filter.feed("你好<mem k='event'>他今天很累</mem>") + filter.finish()
        assertEquals("你好", out)
    }

    @Test
    fun `holds partial tag across chunks`() {
        val filter = MemTagFilter()
        val a = filter.feed("你好<me")
        val b = filter.feed("m k='event'>记")
        val c = filter.feed("住了</mem>再见")
        val d = filter.finish()
        assertEquals("你好再见", a + b + c + d)
    }

    @Test
    fun `passes fake tags through`() {
        val filter = MemTagFilter()
        val out = filter.feed("这是 <memory> 不是标签") + filter.finish()
        assertEquals("这是 <memory> 不是标签", out)
    }

    @Test
    fun `drops unclosed tag at end`() {
        val filter = MemTagFilter()
        val out = filter.feed("正文<mem k='event'>还没写完") + filter.finish()
        assertEquals("正文", out)
    }

    @Test
    fun `keeps literal less-than`() {
        val filter = MemTagFilter()
        val out = filter.feed("1 < 2") + filter.finish()
        assertEquals("1 < 2", out)
    }

    @Test
    fun `self closing tag`() {
        val filter = MemTagFilter()
        val out = filter.feed("前<mem op='fade' id='m7'/>后") + filter.finish()
        assertEquals("前后", out)
    }

    @Test
    fun `tag split character by character`() {
        val filter = MemTagFilter()
        val input = "开始<mem k='fact' i='4'>他怕狗</mem>结束"
        val sb = StringBuilder()
        input.forEach { sb.append(filter.feed(it.toString())) }
        sb.append(filter.finish())
        assertTrue(sb.toString() == "开始结束")
    }
}
