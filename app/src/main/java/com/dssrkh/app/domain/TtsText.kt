package com.dssrkh.app.domain

/** 朗读前的文本清理：去掉记忆标签、代码块、Markdown 与链接。 */
object TtsText {

    fun prepare(raw: String): String {
        var text = raw
        text = text.replace(Regex("""<mem\b[^>]*>.*?</mem>""", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("""```.*?```""", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("""https?://\S+"""), "")
        text = text.replace(Regex("""\[(.*?)]\(.*?\)"""), "$1")
        text = text.replace(Regex("""[*_#>`~]"""), "")
        text = text.replace(Regex("""\s+"""), " ")
        return text.trim().take(10_000)
    }
}
