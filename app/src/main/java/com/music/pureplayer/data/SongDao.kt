package com.music.PurelyPlayer.data

import androidx.room.*

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY id DESC")
    suspend fun getAllSongs(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("SELECT * FROM songs ORDER BY lastPlayedTime DESC LIMIT 10")
    suspend fun getRecentSongs(): List<SongEntity>
    @Delete
    suspend fun deleteSong(song: SongEntity)
}
