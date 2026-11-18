package com.dssrkh.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 角色设定导入：txt / md / docx，自动识别 UTF-8 与 GBK。 */
object TextImport {

    data class Imported(val name: String, val text: String)

    private const val MAX_SOURCE_BYTES = 5 * 1024 * 1024
    private const val MAX_TEXT_CHARS = 200_000

    suspend fun read(context: Context, uri: Uri): Result<Imported> = withContext(Dispatchers.IO) {
        runCatching {
            val name = queryName(context, uri) ?: "导入文本"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("无法读取文件")
            if (bytes.size > MAX_SOURCE_BYTES) error("文件过大（上限 5 MB）")

            val text = if (name.endsWith(".docx", ignoreCase = true) || isZip(bytes)) {
                extractDocx(bytes) ?: error("无法解析这个 docx 文件")
            } else if (name.endsWith(".pdf", ignoreCase = true)) {
                error("暂不支持 PDF，请先转成 txt、md 或 docx")
            } else {
                decode(bytes)
            }.trim()

            if (text.isBlank()) error("文件里没有可导入的文字")
            if (text.length > MAX_TEXT_CHARS) error("文字超过 20 万字，请拆分后再导入")
            Imported(name, text)
        }
    }

    private fun queryName(context: Context, uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment
        return context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
    }

    private fun isZip(bytes: ByteArray): Boolean =
        bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    private fun decode(bytes: ByteArray): String {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: Exception) {
            String(bytes, Charset.forName("GBK"))
        }
    }

    private fun extractDocx(bytes: ByteArray): String? {
        var xml: String? = null
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    xml = zip.readBytes().toString(Charsets.UTF_8)
                    break
                }
                entry = zip.nextEntry
            }
        }
        val source = xml ?: return null
        val paragraphs = source
            .replace("</w:p>", "\n")
            .replace("<w:br/>", "\n")
            .replace("<w:tab/>", "\t")
        val text = Regex("""<w:t[^>]*>(.*?)</w:t>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(paragraphs)
            .joinToString("") { it.groupValues[1] }
        // 保留段落结构
        val withBreaks = StringBuilder()
        var index = 0
        paragraphs.split("\n").forEach { paragraph ->
            val pieces = Regex("""<w:t[^>]*>(.*?)</w:t>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(paragraph)
                .joinToString("") { it.groupValues[1] }
            if (pieces.isNotBlank()) {
                withBreaks.append(pieces).append('\n')
            }
        }
        return (withBreaks.toString().ifBlank { text }).unescapeXml()
    }

    private fun String.unescapeXml(): String = this
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
}
