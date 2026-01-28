package com.music.PurelyPlayer.model

import java.util.UUID

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val coverUri: String?, // 存储 copyFile 返回的磁盘路径
    val songIds: List<Long> // 存储歌曲的 ID 列表
)