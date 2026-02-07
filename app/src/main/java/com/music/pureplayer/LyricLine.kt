//Copyright (c) [2026] [eason204646]
//[purelyplayer] is licensed under Mulan PSL v2.
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
package com.music.PurelyPlayer.model

data class LyricLine(
    val time: Long,
    val content: String
)

/**
 * 将 LRC 格式字符串转换为对象列表
 */
fun parseLrc(lrc: String?): List<LyricLine> {
    if (lrc.isNullOrBlank()) return emptyList()
    val lines = mutableListOf<LyricLine>()
    // 正则匹配 [00:12.34]歌词内容
    val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    lrc.lines().forEach { line ->
        regex.find(line)?.let { match ->
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val ms = match.groupValues[3].toLong()
            val text = match.groupValues[4].trim()

            // 计算毫秒数：分钟*60000 + 秒*1000 + 毫秒
            val time = min * 60000 + sec * 1000 + (if (ms < 100) ms * 10 else ms)
            if (text.isNotEmpty()) lines.add(LyricLine(time, text))
        }
    }
    return lines.sortedBy { it.time }
}