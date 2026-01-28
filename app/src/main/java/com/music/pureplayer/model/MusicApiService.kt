package com.music.PurelyPlayer.model

import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApiService {
    @GET("search")
    suspend fun searchSong(
        @Query("keywords") keywords: String,
        @Query("type") type: Int = 1,
        @Query("limit") limit: Int = 1
    ): SearchResponse
}