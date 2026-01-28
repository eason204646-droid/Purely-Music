package com.music.PurelyPlayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val coverUri: String?,
    val musicUri: String,
    val lastPlayedTime: Long = 0 // 用于记录最近播放时间
)