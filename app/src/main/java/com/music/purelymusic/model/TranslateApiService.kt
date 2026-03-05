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
package com.music.purelymusic.model

import retrofit2.http.GET
import retrofit2.http.Query

interface TranslateApiService {
    @GET("v5/fanyi")
    suspend fun translateText(
        @Query("key") apiKey: String,
        @Query("text") text: String,
        @Query("from") fromLang: String,
        @Query("to") targetLang: String,
        @Query("type") type: String? = null
    ): TranslateResponse
}

data class TranslateResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: TranslateResultData? = null
) {
    // 获取翻译文本的辅助方法
    fun getTranslatedText(): String? {
        return data?.data?.jieguo
    }
}

data class TranslateResultData(
    val code: Int = 0,
    val from: String = "",
    val to: String = "",
    val data: TranslationContent? = null
)

data class TranslationContent(
    val yuanwen: String = "",
    val jieguo: String = ""
)