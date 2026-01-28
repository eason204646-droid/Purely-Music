package com.music.PurelyPlayer.data

import androidx.room.*

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<PlaylistEntity> // 🚩 检查名字和 suspend 关键字

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
}