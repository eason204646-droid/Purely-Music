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

import android.net.Uri

/**
 * 1. App 内部使用的歌曲模型
 * 用于在列表显示、播放器界面展示以及保存本地路径
 */
data class SongItem(
    val title: String,          // 用户输入的歌名
    val artist: String = "未知歌手",
    val coverUri: Uri? = null,  // 用户选择的本地封面
    val id: String,
    val localUri: Uri? = null,  // 歌曲文件路径
    val coverUrl: String = ""   // 网络封面(备用)
)


/**
 * 2. 网易云 API 响应模型 (Retrofit 自动解析用)
 * 结构必须与网易云接口返回的 JSON 嵌套层级完全一致
 */

// 搜索接口最外层
data class SearchResponse(
    val result: SearchResult
)

// 搜索结果层
data class SearchResult(
    val songs: List<CloudSong>
)

// 单首歌曲层
data class CloudSong(
    val id: Long,
    val name: String,
    val al: CloudAlbum,        // 专辑信息 (网易云字段为 al)
    val ar: List<CloudArtist>  // 歌手列表 (网易云字段为 ar)
)

// 专辑信息 (包含封面图)
data class CloudAlbum(
    val id: Long,
    val name: String,
    val picUrl: String         // 【关键】封面图片地址
)

// 歌手信息
data class CloudArtist(
    val name: String
)

/**
 * 3. 歌词响应模型 (如果你以后要实现歌词功能)
 */
data class LyricResponse(
    val lrc: LrcContent
)

data class LrcContent(
    val lyric: String
)
