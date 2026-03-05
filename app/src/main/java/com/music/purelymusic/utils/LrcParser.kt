//Copyright (c) [2026] [eason204646]
//[purelymusic] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
//
//Mulan Permissive Software License，Version 2
//
//Mulan Permissive Software License，Version 2 (Mulan PSL v2)
//
//January 2020 http://license.coscl.org.cn/MulanPSL2
package com.music.purelymusic.utils

import com.music.purelymusic.model.LrcLine

object LrcParser {
    private val timePattern = Regex("[\\[\\uFF3B](\\d{1,2})[:\\uFF1A,\\uFF0C](\\d{1,2})(?:[\\.:,\\uFF0E\\uFF0C](\\d{1,3}))?[\\]\\uFF3D]")

    /**
     * 解析标准LRC格式（每行一个时间戳）
     */
    fun parse(lrcText: String): List<LrcLine> {
        if (lrcText.isBlank()) return emptyList()

        val lines = mutableListOf<LrcLine>()
        val matchCount = lrcText.lineSequence().sumOf { line ->
            timePattern.findAll(line).count()
        }
        if (matchCount > 0) {
            (lines as ArrayList).ensureCapacity(matchCount)
        }

        lrcText.lineSequence().forEach { line ->
            val matches = timePattern.findAll(line).toList()
            if (matches.isEmpty()) return@forEach

            val text = line.replace(timePattern, "").trim()
            matches.forEach { match ->
                try {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val milStr = match.groupValues[3]

                    val mil = when (milStr.length) {
                        0 -> 0L
                        1 -> milStr.toLong() * 100
                        2 -> milStr.toLong() * 10
                        3 -> milStr.toLong()
                        else -> milStr.take(3).toLong()
                    }

                    val time = min * 60000 + sec * 1000 + mil
                    lines.add(LrcLine(time, text))
                } catch (e: NumberFormatException) {
                    // 忽略格式错误的歌词行
                }
            }
        }

        // 后处理：标记续行
        // 判断标准：如果两行歌词的时间戳完全相同，则认为是续行
        for (i in lines.indices) {
            if (i > 0) {
                val prevLine = lines[i - 1]
                val currentLine = lines[i]
                // 如果时间戳完全相同，认为是同一句歌词的续行
                if (currentLine.time == prevLine.time) {
                    lines[i] = currentLine.copy(isContinuation = true)
                }
            }
        }

        return lines
    }

    /**
     * 解析连续时间戳格式（如: [00:06.484]文本[00:10.255]文本）
     * 用于解析翻译API返回的连续文本
     */
    fun parseContinuous(text: String): List<LrcLine> {
        if (text.isBlank()) return emptyList()

        val lines = mutableListOf<LrcLine>()
        val matches = timePattern.findAll(text).toList()

        if (matches.isEmpty()) return emptyList()

        // 提取每个时间戳及其后的文本
        for (i in matches.indices) {
            val match = matches[i]
            val timeStr = match.value
            
            try {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val milStr = match.groupValues[3]

                val mil = when (milStr.length) {
                    0 -> 0L
                    1 -> milStr.toLong() * 100
                    2 -> milStr.toLong() * 10
                    3 -> milStr.toLong()
                    else -> milStr.take(3).toLong()
                }

                val time = min * 60000 + sec * 1000 + mil

                // 提取文本：从当前时间戳结束位置到下一个时间戳开始位置
                val startPos = match.range.last + 1
                val endPos = if (i < matches.size - 1) {
                    matches[i + 1].range.first
                } else {
                    text.length
                }

                var content = text.substring(startPos, endPos).trim()
                // 清理文本中的时间戳（防止嵌套）
                content = content.replace(timePattern, "").trim()

                lines.add(LrcLine(time, content))
            } catch (e: NumberFormatException) {
                // 忽略格式错误的时间戳
            }
        }

        return lines
    }
}
