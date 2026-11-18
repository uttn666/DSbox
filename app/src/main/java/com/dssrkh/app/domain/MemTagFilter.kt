package com.dssrkh.app.domain

/**
 * 把流式正文里的 <mem> 标签过滤掉，供界面实时显示。
 * 规则：完整标签整段吞掉；流式中遇到可能是标签开头的前缀先暂扣；
 * 流结束时仍未闭合的标签整段丢弃（不显示、不入库）。
 */
class MemTagFilter {

    private val pending = StringBuilder()

    fun feed(delta: String): String {
        pending.append(delta)
        return drain(final = false)
    }

    fun finish(): String = drain(final = true)

    private fun drain(final: Boolean): String {
        val s = pending.toString()
        val out = StringBuilder()
        var i = 0

        while (i < s.length) {
            val lt = s.indexOf('<', i)
            if (lt < 0) {
                out.append(s, i, s.length)
                i = s.length
                break
            }
            out.append(s, i, lt)
            val rest = s.substring(lt)

            // 可能是 "<mem" 的前缀：流式中先扣住，结束时丢弃
            if (rest.length < 4 && MEM_PREFIX.startsWith(rest, ignoreCase = true)) {
                if (final) {
                    if (rest.length >= 2) {
                        i = s.length
                    } else {
                        out.append(rest)
                        i = s.length
                    }
                } else {
                    i = lt
                }
                break
            }

            if (!rest.startsWith("<mem", ignoreCase = true) || !isBoundary(rest.getOrNull(4))) {
                out.append('<')
                i = lt + 1
                continue
            }

            val gt = s.indexOf('>', lt)
            if (gt < 0) {
                if (final) {
                    i = s.length
                } else {
                    i = lt
                }
                break
            }

            val head = s.substring(lt, gt + 1)
            if (head.trimEnd().endsWith("/>")) {
                i = gt + 1
                continue
            }

            val close = s.indexOf("</mem>", gt + 1)
            if (close < 0) {
                if (final) {
                    i = s.length
                } else {
                    i = lt
                }
                break
            }
            i = close + "</mem>".length
        }

        pending.setLength(0)
        if (i < s.length) pending.append(s, i, s.length)
        return out.toString()
    }

    private fun isBoundary(c: Char?): Boolean = c == null || c == '>' || c == '/' || c.isWhitespace()

    companion object {
        private const val MEM_PREFIX = "<mem"
    }
}
