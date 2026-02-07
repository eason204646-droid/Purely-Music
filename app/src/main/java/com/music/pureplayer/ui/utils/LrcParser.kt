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
package com.music.PurelyPlayer.utils

import com.music.PurelyPlayer.model.LrcLine

object LrcParser {
    // 🚩 改进的正则：使用 [.:] 兼容点和冒号，并让内容部分可选
    private val regex = Regex("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\](.*)")

    fun parse(lrcText: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()

        lrcText.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                try {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val milStr = match.groupValues[3]
                    val text = match.groupValues[4].trim()

                    // 🚩 动态转换毫秒：根据位数补全 (2位*10, 1位*100)
                    val mil = when (milStr.length) {
                        1 -> milStr.toLong() * 100
                        2 -> milStr.toLong() * 10
                        else -> milStr.toLong()
                    }

                    val time = min * 60000 + sec * 1000 + mil
                    lines.add(LrcLine(time, text))
                } catch (e: Exception) {
                    // 防止个别行格式崩坏导致整个解析停止
                    e.printStackTrace()
                }
            }
        }
        // 按时间排序
        return lines.sortedBy { it.time }
    }
}