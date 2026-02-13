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

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.music.purelymusic.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverUri: String?,
    val songIdsJson: String, // 🚩 存储为 JSON 字符串
    val description: String? = null, // 🚩 新增：播放列表描述（版本7添加）
    val createdAt: Long = 0, // 🚩 新增：创建时间（版本7添加）
    val updatedAt: Long = 0 // 🚩 新增：更新时间（版本7添加）
)

// 转换工具：Playlist <-> Entity
fun Playlist.toEntity(): PlaylistEntity {
    return PlaylistEntity(
        id = id,
        name = name,
        coverUri = coverUri,
        songIdsJson = Gson().toJson(songIds),
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun PlaylistEntity.toPlaylist(): Playlist {
    val type = object : TypeToken<List<Long>>() {}.type
    val ids: List<Long> = Gson().fromJson(songIdsJson, type)
    return Playlist(id, name, coverUri, ids, description, createdAt, updatedAt)
}