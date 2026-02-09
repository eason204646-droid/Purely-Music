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
package com.music.purelymusic.model

import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApiService {
    @GET("music/migu")
    suspend fun searchSong(
        @Query("key") key: String = "v3ywJo5vIfAHRz9lIRg",
        @Query("type") type: String = "search",
        @Query("word") word: String
    ): MiguSearchResponse

    @GET("api/music/lrc")
    suspend fun getLrc(
        @Query("key") key: String = "v3ywJo5vIfAHRz9lIRg",
        @Query("mid") mid: String,
        @Query("type") type: String = "wy"
    ): LrcApiResponse
}