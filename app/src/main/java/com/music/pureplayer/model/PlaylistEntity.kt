package com.music.PurelyPlayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.music.PurelyPlayer.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverUri: String?,
    val songIdsJson: String // 🚩 存储为 JSON 字符串
)

// 转换工具：Playlist <-> Entity
fun Playlist.toEntity(): PlaylistEntity {
    return PlaylistEntity(
        id = id,
        name = name,
        coverUri = coverUri,
        songIdsJson = Gson().toJson(songIds)
    )
}

fun PlaylistEntity.toPlaylist(): Playlist {
    val type = object : TypeToken<List<Long>>() {}.type
    val ids: List<Long> = Gson().fromJson(songIdsJson, type)
    return Playlist(id, name, coverUri, ids)
}
