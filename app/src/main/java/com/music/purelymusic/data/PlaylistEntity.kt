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
package com.music.purelymusic.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.music.purelymusic.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverUri: String?,
    val description: String? = null, // 🚩 新增：播放列表描述（版本7添加）
    @ColumnInfo(defaultValue = "0") val createdAt: Long = 0,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0
)

// 转换工具：Playlist <-> Entity
fun Playlist.toEntity(): PlaylistEntity {
    return PlaylistEntity(
        id = id,
        name = name,
        coverUri = coverUri,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId",
        entity = PlaylistSongCrossRef::class
    )
    val songRefs: List<PlaylistSongCrossRef>
)

fun Playlist.toSongRefs(): List<PlaylistSongCrossRef> = songIds.mapIndexed { index, songId ->
    PlaylistSongCrossRef(id, songId, index)
}

fun PlaylistWithSongs.toPlaylist(): Playlist = with(playlist) {
    Playlist(
        id = id,
        name = name,
        coverUri = coverUri,
        songIds = songRefs.sortedBy { it.position }.map { it.songId },
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
