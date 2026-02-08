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

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val coverUri: String?,
    val musicUri: String?,
    val lrcPath: String? = null,// 🚩 新增：歌词文件路径
    val lastPlayedTime: Long = 0, // 用于记录最近播放时间
    val playCount: Int = 0, // 🚩 新增：播放次数（版本5添加）
    val createdTime: Long = 0, // 🚩 新增：歌曲添加时间（版本5添加）
    val isFavorite: Int = 0, // 🚩 新增：收藏状态，0=未收藏，1=已收藏（版本6添加）
    val duration: Long = 0, // 🚩 新增：歌曲时长（毫秒）（版本6添加）
    val album: String? = null // 🚩 新增：专辑名称（版本6添加）
)