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
 * 3. 咪咕 API 响应模型
 */

// 咪咕 API 返回的歌曲信息
data class MiguSong(
    val songId: String,
    val songName: String,
    val singer: String,
    val cover: String,
    val album: String?
)

// 咪咕 API 搜索响应
data class MiguSearchResponse(
    val code: Int,
    val msg: String,
    val data: List<MiguSong>
)

/**
 * 4. 自动获取所有信息API响应模型 (https://api.yaohud.cn/api/music/wy)
 */
data class WyApiMusic(
    val lrc: String,
    val lrcurl: String,
    val lrctxt: String? = null
)

data class WyApiData(
    val name: String,
    val songname: String,
    val album: String,
    val songtitle: String,
    val picture: String,
    val url: String,
    val musicurl: String,
    val music: WyApiMusic,
    val lrctxt: String?,
    val lrc: String,
    val catalog: String
)

data class WyApiDebug(
    val cache: String,
    val tips: String
)

data class WyApiResponse(
    val code: Int,
    val msg: String,
    val data: WyApiData,
    val debug: WyApiDebug,
    val exec_time: Double,
    val tips: String,
    val ip: String
)

/**
 * 5. LRC JSON响应模型 (data.music.lrcurl 返回的格式)
 */
data class LrcJsonResponse(
    val code: Int,
    val msg: String,
    val data: LrcJsonData?,
    val exec_time: Double,
    val tips: String,
    val ip: String
)

data class LrcJsonData(
    val lyric: String
)

/**
 * 6. 专辑模型
 */
data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val coverUri: String?,
    val createdAt: Long = System.currentTimeMillis()
)
