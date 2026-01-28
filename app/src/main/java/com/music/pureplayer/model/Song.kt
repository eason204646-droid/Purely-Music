package com.music.PurelyPlayer.model

import com.music.PurelyPlayer.data.SongEntity

// UI 使用的歌曲模型
data class Song(
    val id: Int = 0,
    val title: String,
    val artist: String,
    val coverUri: String?,
    val musicUri: String
)

/**
 * 将数据库实体 (SongEntity) 转换为 UI 模型 (Song)
 */
fun SongEntity.toSong(): Song {
    return Song(
        id = this.id,
        title = this.title,
        artist = this.artist,
        coverUri = this.coverUri,
        musicUri = this.musicUri
    )
}

/**
 * 将 UI 模型 (Song) 转换为数据库实体 (SongEntity)
 */
fun Song.toEntity(lastPlayedTime: Long = 0): SongEntity {
    return SongEntity(
        id = this.id, // 保持 ID 一致，Room 才能更新正确的行
        title = this.title,
        artist = this.artist,
        coverUri = this.coverUri,
        musicUri = this.musicUri,
        lastPlayedTime = lastPlayedTime
    )
}