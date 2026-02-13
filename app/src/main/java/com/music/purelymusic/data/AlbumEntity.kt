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
import com.music.purelymusic.model.Album

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String,
    val coverUri: String?,
    val createdAt: Long = 0
)

// 转换工具：Album <-> Entity
fun Album.toEntity(): AlbumEntity {
    return AlbumEntity(
        id = id,
        name = name,
        artist = artist,
        coverUri = coverUri,
        createdAt = createdAt
    )
}

fun AlbumEntity.toAlbum(): Album {
    return Album(
        id = id,
        name = name,
        artist = artist,
        coverUri = coverUri,
        createdAt = createdAt
    )
}