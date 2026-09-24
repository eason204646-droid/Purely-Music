// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.utils

import com.music.purelymusic.model.LrcLine
import java.util.Locale
import kotlin.math.abs

/** 将翻译服务返回的 LRC 或纯文本安全地映射回原歌词时间轴。 */
object LyricTranslationParser {
    // The provider occasionally rearranges the three letters in PMT (for example, PTM or
    // PTC).  The numeric part is the only part that identifies a lyric line reliably.
    private val translationMarker = Regex(
        """\[\[\s*P[A-Z]{2}[_-](\d+)\s*]]""",
        RegexOption.IGNORE_CASE
    )
    private val anyBracketedMarker = Regex("""\[\[[^\]\r\n]{0,64}]]""")

    fun formatTime(milliseconds: Long): String {
        val minutes = milliseconds / 60_000
        val seconds = (milliseconds % 60_000) / 1_000
        val millis = milliseconds % 1_000
        return String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, millis)
    }

    fun parse(
        translatedText: String,
        originalLines: List<LrcLine>,
        toleranceMs: Long = 300L
    ): List<String?> {
        if (originalLines.isEmpty()) return emptyList()
        val decoded = decodeHtmlEntities(translatedText)
        val translatedLines = LrcParser.parseContinuous(decoded)

        if (translatedLines.isEmpty()) {
            val plainLines = decoded.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toList()
            return originalLines.indices.map { index ->
                plainLines.getOrNull(index)?.let(::sanitizeTranslation)
            }
        }

        return originalLines.map { original ->
            translatedLines
                .minByOrNull { abs(it.time - original.time) }
                ?.takeIf { abs(it.time - original.time) <= toleranceMs }
                ?.content
                ?.let(::sanitizeTranslation)
        }
    }

    /**
     * Extract translations from marker-preserving batches. Some providers mutate the marker
     * letters, so map by its stable numeric suffix and never return the marker as lyric text.
     */
    fun parseMarked(text: String): Map<Int, String> {
        val decoded = decodeHtmlEntities(text)
        val matches = translationMarker.findAll(decoded).toList()
        return matches.mapIndexedNotNull { matchIndex, match ->
            val index = match.groupValues[1].toIntOrNull()
                ?: return@mapIndexedNotNull null
            val end = matches.getOrNull(matchIndex + 1)?.range?.first ?: decoded.length
            val value = decoded.substring(match.range.last + 1, end)
            sanitizeTranslation(value)?.let { index to it }
        }.toMap()
    }

    /** True when a batch response still contains a line marker, even a mutated one. */
    fun containsTranslationMarker(text: String): Boolean = translationMarker.containsMatchIn(decodeHtmlEntities(text))

    /**
     * Last-line defence for provider formatting leaks. This is used before every translation is
     * assigned to the UI, so marker text can never be rendered as a lyric translation.
     */
    fun sanitizeTranslation(text: String): String? {
        val cleaned = decodeHtmlEntities(text)
            .replace(translationMarker, " ")
            .replace(anyBracketedMarker, " ")
            .lineSequence()
            .joinToString(" ") { it.trim() }
            .trim()
            .trim('[', ']', '：', ':', '-', '—')
            .trim()
        return cleaned.takeIf(String::isNotBlank)
    }

    fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&quot；", "\"")
            .replace("&apos；", "'")
            .replace("&lt；", "<")
            .replace("&gt；", ">")
            .replace("&amp；", "&")
            .replace("&#039；", "'")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace('【', '[')
            .replace('】', ']')
            .replace('［', '[')
            .replace('］', ']')
            .replace("\\n", "\n")
    }
}
