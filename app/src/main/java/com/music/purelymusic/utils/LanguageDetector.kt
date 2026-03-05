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
//Mulan Permissive Software License，Version 2
//
//January 2020 http://license.coscl.org.cn/MulanPSL2
package com.music.purelymusic.utils

object LanguageDetector {

    /**
     * 检测文本是否为中文（包括简体和繁体）
     * @param text 要检测的文本
     * @return true如果文本包含中文字符，false否则
     */
    fun isChinese(text: String): Boolean {
        if (text.isBlank()) return false
        
        // 简体和繁体中文的Unicode范围
        val chinesePattern = Regex("[\\u4e00-\\u9fff\\u3400-\\u4dbf\\uf900-\\ufaff]")
        return chinesePattern.containsMatchIn(text)
    }

    /**
     * 检测歌词列表是否主要为中文
     * @param lyrics 歌词列表
     * @return true如果超过30%的歌词包含中文字符
     */
    fun isLyricsChinese(lyrics: List<String>): Boolean {
        if (lyrics.isEmpty()) return false
        
        val chineseCount = lyrics.count { line ->
            isChinese(line)
        }
        
        // 如果超过30%的歌词包含中文，则认为是中文歌词
        return chineseCount > lyrics.size * 0.3
    }
}
