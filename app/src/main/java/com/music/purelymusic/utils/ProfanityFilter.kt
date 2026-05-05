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

/**
 * 脏字过滤工具类
 * 用于过滤歌词中的常见中英文脏字
 */
object ProfanityFilter {
    // 中文敏感词列表
    private val chineseProfanity = setOf(
        "傻逼", "傻b", "煞笔", "煞比", "傻B", "傻×", "傻叉", "装逼", "装B", "装b",
        "妈逼", "妈b", "妈比", "妈B", "操你", "日你", "草你", "狗日的", "狗屁",
        "他妈", "他妈的", "tmd", "TMD", "操蛋", "草蛋", "傻吊", "傻屌", "屌丝",
        "狗逼", "狗B", "狗b", "傻逼", "煞笔", "王八", "王八蛋", "婊子", "婊",
        "废物", "垃圾", "废柴", "蠢货", "白痴", "弱智", "智障", "二逼", "二b",
        "草泥马", "操泥马", "卧槽", "我操", "我草", "艹", "去你妈", "去你大爷",
        "滚蛋", "滚粗", "神经病", "神经", "脑残", "脑残儿", "脑瘫", "脑残粉",
        "贱人", "贱货", "骚货", "骚逼", "骚B", "婊砸", "贱骨头", "死全家",
        "杀千刀", "天杀的", "狗娘养", "娘希匹", "妈了个", "奶奶的", "大爷的"
    )

    // 英文敏感词列表
    private val englishProfanity = setOf(
        "fuck", "shit", "damn", "hell", "bitch", "bastard", "ass", "asshole",
        "whore", "slut", "cunt", "dick", "cock", "pussy", "wtf", "fucking",
        "shitty", "bullshit", "crap", "suck", "sucks", "bitchy", "douche",
        "dumbass", "idiot", "moron", "retard", "jackass", "asshole", "fuckin"
    )

    /**
     * 过滤文本中的脏字
     * @param text 原始文本
     * @return 过滤后的文本，脏字会被替换为星号
     */
    fun filter(text: String): String {
        var result = text

        // 过滤中文脏字
        chineseProfanity.forEach { word ->
            result = result.replace(word, "*".repeat(word.length))
        }

        // 过滤英文脏字（不区分大小写）
        englishProfanity.forEach { word ->
            val regex = Regex(word, RegexOption.IGNORE_CASE)
            result = result.replace(regex) { matchResult ->
                "*".repeat(matchResult.value.length)
            }
        }

        return result
    }

    /**
     * 检查文本是否包含脏字
     * @param text 要检查的文本
     * @return 如果包含脏字返回true，否则返回false
     */
    fun containsProfanity(text: String): Boolean {
        val lowerText = text.lowercase()
        
        // 检查中文脏字
        chineseProfanity.forEach { word ->
            if (text.contains(word)) return true
        }

        // 检查英文脏字
        englishProfanity.forEach { word ->
            if (lowerText.contains(word.lowercase())) return true
        }

        return false
    }
}