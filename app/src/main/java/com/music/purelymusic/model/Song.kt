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

import com.music.purelymusic.data.SongEntity

// UI 使用的歌曲模型
data class Song(
    val id: Long,           // 使用 Long 避免溢出，并匹配 currentTimeMillis
    val title: String,
    val artist: String,
    val musicUri: String?,   // 确保叫 musicUri
    val coverUri: String? = null,
    val lrcPath: String? = null,
    val duration: Long = 0L // 🚩 加上这个字段，解决 "No parameter" 报错
)

/**
 * 将数据库实体 (SongEntity) 转换为 UI 模型 (Song)
 */
fun SongEntity.toSong(): Song {
    return Song(
        id = this.id.toLong(),
        title = this.title,
        artist = this.artist,
        coverUri = this.coverUri,
        musicUri = this.musicUri,
        lrcPath = this.lrcPath

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
        lastPlayedTime = lastPlayedTime,
        lrcPath = this.lrcPath
    )
}