// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.utils

import com.music.purelymusic.model.LrcLine
import java.util.Locale
import kotlin.math.abs

/** 将翻译服务返回的 LRC 或纯文本安全地映射回原歌词时间轴。 */
object LyricTranslationParser {
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
            return originalLines.indices.map { plainLines.getOrNull(it) }
        }

        return originalLines.map { original ->
            translatedLines
                .minByOrNull { abs(it.time - original.time) }
                ?.takeIf { abs(it.time - original.time) <= toleranceMs }
                ?.content
                ?.takeIf(String::isNotBlank)
        }
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
