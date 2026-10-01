// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.utils

import com.music.purelymusic.model.LrcLine
import java.util.Locale
import kotlin.math.abs

/** 将翻译服务返回的 LRC 或纯文本安全地映射回原歌词时间轴。 */
object LyricTranslationParser {
    // Providers can change the letters, brackets, spacing or add a slash to a line marker.
    // Recognize the numeric boundary even when the surrounding brackets are incomplete.
    private const val MARKER_PREFIX = """(?<![A-Z0-9])(?:\[\s*)*(?:/\s*)?P\s*[A-Z]\s*[A-Z]\s*[_-]\s*"""
    private val translationMarker = Regex(
        """$MARKER_PREFIX(\d+)(?![\dA-Z_])(?:\s*])*""",
        RegexOption.IGNORE_CASE
    )
    private val markerLikeText = Regex(
        """$MARKER_PREFIX[^\s\[\]]*(?:\s*])*""",
        RegexOption.IGNORE_CASE
    )
    private val anyBracketedMarker = Regex("""\[\[[^\]\r\n]{0,64}]]""")
    private val numericHtmlEntity = Regex("""&#(x[0-9a-f]+|\d+)[;；]""", RegexOption.IGNORE_CASE)

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
            // A merged/truncated response has no reliable positional correspondence.
            if (containsTranslationMarker(decoded) || plainLines.size != originalLines.size) {
                return List(originalLines.size) { null }
            }
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

    /** Accept a tagged batch only when every requested line is identified exactly once. */
    fun parseBatch(text: String, originalLines: Map<Int, LrcLine>): Map<Int, String> {
        if (originalLines.isEmpty()) return emptyMap()
        val decoded = decodeHtmlEntities(text)
        if (containsTranslationMarker(decoded)) {
            val matches = translationMarker.findAll(decoded).toList()
            val indices = matches.map { it.groupValues[1].toIntOrNull() }
            if (matches.size != originalLines.size || indices.toSet() != originalLines.keys) {
                return emptyMap()
            }
            // Unknown markers and unlabelled prefixes can hide a missing/merged lyric line.
            val remainder = decoded.replace(translationMarker, "")
            if (containsTranslationMarker(remainder) ||
                decoded.substring(0, matches.first().range.first).isNotBlank()
            ) {
                return emptyMap()
            }
            val marked = parseMarked(decoded)
            return marked.takeIf { it.keys == originalLines.keys } ?: emptyMap()
        }

        val entries = originalLines.entries.toList()
        val ordered = parse(decoded, entries.map { it.value })
        return entries.mapIndexedNotNull { index, entry ->
            ordered[index]?.let { entry.key to it }
        }.toMap()
    }

    /**
     * Extract translations from marker-preserving batches. Some providers mutate the marker
     * letters, so map by its stable numeric suffix and never return the marker as lyric text.
     */
    fun parseMarked(text: String): Map<Int, String> {
        val decoded = decodeHtmlEntities(text)
        val matches = translationMarker.findAll(decoded).toList()
        val boundaries = markerLikeText.findAll(decoded).map { it.range.first }.toList()
        val candidates = matches.mapIndexedNotNull { matchIndex, match ->
            val index = match.groupValues[1].toIntOrNull()
                ?: return@mapIndexedNotNull null
            val nextMatch = matches.getOrNull(matchIndex + 1)?.range?.first ?: decoded.length
            val end = boundaries.firstOrNull { it > match.range.last }?.coerceAtMost(nextMatch)
                ?: nextMatch
            val value = decoded.substring(match.range.last + 1, end)
            sanitizeTranslation(value)?.let { index to it }
        }
        val duplicateIndices = matches.mapNotNull { it.groupValues[1].toIntOrNull() }
            .groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        return candidates.filterNot { it.first in duplicateIndices }.toMap()
    }

    /** Also detects unparseable marker-like text so it never enables positional matching. */
    fun containsTranslationMarker(text: String): Boolean {
        val decoded = decodeHtmlEntities(text)
        return markerLikeText.containsMatchIn(decoded) || anyBracketedMarker.containsMatchIn(decoded)
    }

    /**
     * Last-line defence for provider formatting leaks. This is used before every translation is
     * assigned to the UI, so marker text can never be rendered as a lyric translation.
     */
    fun sanitizeTranslation(text: String): String? {
        val cleaned = decodeHtmlEntities(text)
            .replace(translationMarker, " ")
            .replace(markerLikeText, " ")
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
            .replace(numericHtmlEntity) { match ->
                val value = match.groupValues[1]
                val codePoint = if (value.startsWith("x", ignoreCase = true)) {
                    value.drop(1).toIntOrNull(16)
                } else {
                    value.toIntOrNull()
                }
                if (codePoint != null && Character.isValidCodePoint(codePoint) &&
                    codePoint !in 0xD800..0xDFFF
                ) {
                    String(Character.toChars(codePoint))
                } else {
                    match.value
                }
            }
            .replace('【', '[')
            .replace('】', ']')
            .replace('［', '[')
            .replace('］', ']')
            .replace('＿', '_')
            .replace('－', '-')
            .replace('／', '/')
            .replace("\\n", "\n")
    }
}
