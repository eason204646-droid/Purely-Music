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
package com.music.purelymusic.viewmodel
import com.music.purelymusic.data.toPlaylist
import android.annotation.SuppressLint
import android.app.Application
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.legacy.MediaMetadataCompat
import androidx.media3.session.legacy.MediaSessionCompat
import androidx.media3.session.legacy.PlaybackStateCompat

import com.music.purelymusic.data.AppDatabase
import com.music.purelymusic.data.toEntity
import com.music.purelymusic.model.* // 🚩 导入 Song, Playlist 及上面的扩展函数
import com.music.purelymusic.utils.LrcParser
import com.music.purelymusic.ui.utils.BlurUtil
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset

@SuppressLint("RestrictedApi")
@UnstableApi
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private fun playSongFromList(song: Song) {
        playSong(song, updateInternalList = false)
    }
    fun playPlaylist(playlist: Playlist, isRandom: Boolean) {
        val songs = libraryList.filter { song: Song ->
            playlist.songIds.contains(song.id.toLong())
        }
        if (songs.isEmpty()) return
        currentPlayingList.clear()
        currentPlayingList.addAll(if (isRandom) songs.shuffled() else songs)
        playSongFromList(currentPlayingList[0])
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            // 1. 从数据库中删除 (使用你修好的 toEntity 函数)
            playlistDao.deletePlaylist(playlist.toEntity())

            // 2. 从当前内存列表中移除，这样 UI 才会立刻刷新
            // 假设你的 playlists 是一个 MutableStateList 或者 MutableList
            playlists.remove(playlist)
        }
    }
    fun saveSong(title: String, artist: String) {
        val mUri = tempMusicUri
        android.util.Log.d("purelymusic", "saveSong 被调用: title=$title, artist=$artist, tempMusicUri=$mUri")
        if (mUri == null) {
            android.util.Log.e("purelymusic", "tempMusicUri 为 null，无法保存歌曲")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("purelymusic", "开始复制文件")
                // 拷贝文件到私有目录，防止系统清理或权限丢失
                val pMusic = copyFile(mUri, "mus_${System.currentTimeMillis()}.mp3")
                
                // 处理封面：如果是本地文件路径，直接使用；如果是 URI，需要复制
                val pCover: String? = tempCoverUri?.let { uri ->
                    val uriString = uri.toString()
                    if (uriString.startsWith("/")) {
                        // 已经是本地文件路径，直接使用
                        uriString
                    } else {
                        // 是 URI，需要复制到本地
                        copyFile(uri, "cov_${System.currentTimeMillis()}.jpg")
                    }
                }
                
                val pLrc = tempLrcUri?.let { uri ->
                    val uriString = uri.toString()
                    if (uriString.startsWith("/")) {
                        // 已经是本地文件路径，直接使用
                        uriString
                    } else if (uriString.startsWith("file://")) {
                        // 是 file:// 格式的 URI，提取路径部分
                        uriString.substring(7)
                    } else {
                        // 是内容选择器的 URI，需要复制到本地
                        copyFile(uri, "lrc_${System.currentTimeMillis()}.lrc")
                    }
                }

                android.util.Log.d("purelymusic", "文件复制结果: pMusic=$pMusic, pCover=$pCover, pLrc=$pLrc")

                if (pMusic != null) {
                    val newSong = Song(
                        id = 0, // Room 会自动生成
                        title = title,
                        artist = artist,
                        coverUri = pCover,
                        musicUri = pMusic,
                        lrcPath = pLrc
                    )
                    android.util.Log.d("purelymusic", "准备插入数据库: $newSong")
                    // 存入数据库
                    songDao.insertSong(newSong.toEntity())
                    android.util.Log.d("purelymusic", "数据库插入成功")

                    // 验证数据是否真的保存了
                    val allSongs = songDao.getAllSongs()
                    android.util.Log.d("purelymusic", "插入后数据库中的歌曲总数: ${allSongs.size}")
                    android.util.Log.d("purelymusic", "最新插入的歌曲: ${allSongs.lastOrNull()}")

                    // 检查数据库文件
                    val dbFile = context.getDatabasePath("am_player_db")
                    android.util.Log.d("purelymusic", "数据库文件大小: ${dbFile.length()} bytes")

                    // 重置临时状态并刷新
                    withContext(Dispatchers.Main) {
                        android.util.Log.d("purelymusic", "重置临时状态并刷新数据")
                        tempMusicUri = null
                        tempCoverUri = null
                        tempLrcUri = null
                        refreshData()
                    }
                } else {
                    android.util.Log.e("purelymusic", "复制音乐文件失败")
                }
            } catch (e: Exception) {
                android.util.Log.e("purelymusic", "保存歌曲失败: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }
    fun updatePlaylistSongs(playlistId: String, newSongIds: List<Long>) {
        viewModelScope.launch {
            // 1. 既然参数已经是 String，直接比较即可，toString() 是为了防止 it.id 可能是其他类型
            val index = playlists.indexOfFirst { it.id.toString() == playlistId }

            if (index != -1) {
                // 2. 更新内存中的列表对象
                val updatedPlaylist = playlists[index].copy(songIds = newSongIds)
                playlists[index] = updatedPlaylist

                // 3. 写入数据库
                // 🚩 注意：请确保你的 PlaylistEntity 里的 id 字段也是 String 类型
                // 如果 Entity 里的 id 是 Long，这里依然会因为 UUID 无法存入而报错
                try {
                    playlistDao.insertPlaylist(updatedPlaylist.toEntity())
                } catch (e: Exception) {
                    android.util.Log.e("purelymusic", "数据库更新失败: ${e.message}")
                }
            }
        }
    }

    // 从歌单中删除歌曲
    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        viewModelScope.launch {
            val index = playlists.indexOfFirst { it.id.toString() == playlistId }
            if (index != -1) {
                val updatedSongIds = playlists[index].songIds.filter { it != songId }
                val updatedPlaylist = playlists[index].copy(songIds = updatedSongIds)
                playlists[index] = updatedPlaylist
                playlistDao.insertPlaylist(updatedPlaylist.toEntity())
            }
        }
    }

    // 添加歌曲到歌单
    fun addSongsToPlaylist(playlistId: String, songIds: List<Long>) {
        viewModelScope.launch {
            val index = playlists.indexOfFirst { it.id.toString() == playlistId }
            if (index != -1) {
                val currentSongIds = playlists[index].songIds.toMutableList()
                songIds.forEach { songId ->
                    if (!currentSongIds.contains(songId)) {
                        currentSongIds.add(songId)
                    }
                }
                val updatedPlaylist = playlists[index].copy(songIds = currentSongIds)
                playlists[index] = updatedPlaylist
                playlistDao.insertPlaylist(updatedPlaylist.toEntity())
            }
        }
    }

    private fun copyFile(uri: Uri, fileName: String): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, fileName)
            val output = FileOutputStream(file)
            input?.copyTo(output)
            input?.close()
            output.close()
            file.absolutePath
        } catch (e: Exception) { null }
    }
    fun savePlaylist(name: String) {
        viewModelScope.launch {
            val finalCoverPath: String? = tempPlaylistCoverUri?.let {
                copyFile(it, "pl_cov_${System.currentTimeMillis()}.jpg")
            }
            val newPlaylist = Playlist(
                name = name,
                coverUri = finalCoverPath,
                songIds = selectedSongsForPlaylist.map { it.id.toLong() },
                description = null, // 默认没有描述
                createdAt = System.currentTimeMillis(), // 创建时间
                updatedAt = System.currentTimeMillis() // 更新时间
            )
            playlistDao.insertPlaylist(newPlaylist.toEntity())
            playlists.add(0, newPlaylist)
            selectedSongsForPlaylist.clear()
            tempPlaylistCoverUri = null
        }
    }


    private val context = application.applicationContext
    private val songDao = AppDatabase.getDatabase(application).songDao()
    private val playlistDao = AppDatabase.getDatabase(application).playlistDao()
    private var mediaPlayer: MediaPlayer? = null
    @SuppressLint("RestrictedApi")
    private var mediaSession: MediaSessionCompat? = null

    // --- UI 状态 ---
    var libraryList by mutableStateOf<List<Song>>(emptyList())
    var currentSong by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var blurredBackground by mutableStateOf<android.graphics.Bitmap?>(null)
    var lyricLines by mutableStateOf(emptyList<LrcLine>())
        private set

    val currentLyricIndex by derivedStateOf {
        val index = lyricLines.indexOfLast { it.time <= currentPosition }
        if (index == -1) 0 else index
    }

    var recentSongs = mutableStateListOf<Song>()
    var playlists = mutableStateListOf<Playlist>()
    private var currentPlayingList = mutableStateListOf<Song>()
    var selectedSongsForPlaylist = mutableStateListOf<Song>()
    var showPlaylist by mutableStateOf(false)

    // 导入临时状态
    var tempPlaylistCoverUri by mutableStateOf<Uri?>(null)
    var tempMusicUri by mutableStateOf<Uri?>(null)
    var tempCoverUri by mutableStateOf<Uri?>(null)
    var tempLrcUri by mutableStateOf<Uri?>(null)

    // 编辑歌曲状态
    var editingSong by mutableStateOf<Song?>(null)
    var editTitle by mutableStateOf("")
    var editArtist by mutableStateOf("")
    var editCoverUri by mutableStateOf<Uri?>(null)
    var editLrcUri by mutableStateOf<Uri?>(null)

    // 添加歌曲到歌单的状态
    var showAddSongDialog by mutableStateOf(false)
    var selectedPlaylistForAdd by mutableStateOf<String?>(null)
    var selectedSongsForAdd by mutableStateOf<Set<Long>>(emptySet())

    // 自动获取封面的状态
    var isFetchingCover by mutableStateOf(false)
        private set
    var fetchCoverError by mutableStateOf<String?>(null)
    var apiResponseData by mutableStateOf<String?>(null)

    // 自动获取LRC的状态
    var isFetchingLrc by mutableStateOf(false)
        private set
    var fetchLrcError by mutableStateOf<String?>(null)

    init {
        // 初始化 MediaSession
        mediaSession = MediaSessionCompat(context, "purelymusic").apply {
            isActive = true
            // 🚩 核心修复：添加回调监听系统指令
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { togglePlayPause() }
                override fun onPause() { togglePlayPause() }
                override fun onSkipToNext() { playNext() }
                override fun onSkipToPrevious() { playPrevious() }
                override fun onSeekTo(pos: Long) { seekTo(pos.toFloat()) } // 支持系统进度条拖动
            })
        }
        refreshData()
        startTimer()
    }

    // --- 歌词加载与解析 ---
    private fun loadLyrics(pathOrUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("LyricLoad", "开始加载歌词: pathOrUri=$pathOrUri")

                val bytes = if (pathOrUri.startsWith("/")) {
                    val file = File(pathOrUri)
                    Log.d("LyricLoad", "文件路径: $pathOrUri, 文件存在: ${file.exists()}, 文件大小: ${file.length()}")
                    if (file.exists()) file.readBytes() else null
                } else {
                    Log.d("LyricLoad", "URI路径: $pathOrUri")
                    context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use { it.readBytes() }
                }

                if (bytes == null || bytes.isEmpty()) {
                    Log.e("LyricLoad", "歌词文件为空或不存在")
                    withContext(Dispatchers.Main) { lyricLines = emptyList() }
                    return@launch
                }

                val rawText = String(bytes, Charsets.UTF_8)
                Log.d("LyricLoad", "歌词内容长度: ${rawText.length}, 前200字符: ${rawText.take(200)}")

                // 尝试多种编码防止乱码
                var parsed = LrcParser.parse(rawText)
                if (parsed.isEmpty()) {
                    Log.d("LyricLoad", "UTF-8解析失败，尝试GBK编码")
                    parsed = LrcParser.parse(String(bytes, Charset.forName("GBK")))
                }

                Log.d("LyricLoad", "解析结果: 共${parsed.size}行歌词")
                parsed.take(3).forEachIndexed { index, line ->
                    Log.d("LyricLoad", "  第${index}行: 时间=${line.time}ms, 文本=${line.content}")
                }

                withContext(Dispatchers.Main) {
                    lyricLines = parsed
                }
            } catch (e: Exception) {
                Log.e("LyricLoad", "Failed: ${e.message}", e)
                withContext(Dispatchers.Main) { lyricLines = emptyList() }
            }
        }
    }

    // --- 播放控制逻辑 ---
    fun playSong(song: Song, updateInternalList: Boolean = true) {
        if (updateInternalList) {
            currentPlayingList.clear()
            currentPlayingList.addAll(libraryList)
        }

        if (currentSong?.id == song.id && mediaPlayer != null) {
            togglePlayPause()
            return
        }

        mediaPlayer?.stop()
        mediaPlayer?.release()
        currentSong = song

        Log.d("PlaySong", "开始播放歌曲: ${song.title}, 歌词路径: ${song.lrcPath}")

        // 加载歌词
        if (!song.lrcPath.isNullOrEmpty()) {
            loadLyrics(song.lrcPath)
        } else {
            Log.d("PlaySong", "歌曲没有歌词路径")
            lyricLines = emptyList()
        }

        updateMediaSession(song)
        updateBlurBackground(song.coverUri)

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(song.musicUri))
                prepareAsync()
                setOnPreparedListener {
                    start()
                    this@PlayerViewModel.isPlaying = true
                    this@PlayerViewModel.duration = it.duration.toLong()
                    updatePlaybackState(true)
                }
                setOnCompletionListener { playNext() }
            }
            // 更新数据库播放时间
            viewModelScope.launch {
                songDao.updateSong(song.toEntity(System.currentTimeMillis()))
            }
        } catch (e: Exception) {
            Log.e("PlayError", "${e.message}")
        }
    }

    // 从播放列表中删除歌曲
    fun removeSongFromPlayingList(song: Song) {
        if (currentPlayingList.isEmpty()) return
        val index = currentPlayingList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            currentPlayingList.removeAt(index)
            // 如果删除的是当前播放的歌曲，播放下一首
            if (currentSong?.id == song.id && currentPlayingList.isNotEmpty()) {
                playSong(currentPlayingList[0], false)
            }
        }
    }

    // 跳转到指定歌曲（删除该歌曲之前的所有歌曲）
    fun jumpToSong(song: Song) {
        if (currentPlayingList.isEmpty()) return
        val index = currentPlayingList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            // 保留从当前歌曲开始的列表
            val newList = currentPlayingList.subList(index, currentPlayingList.size).toList()
            currentPlayingList.clear()
            currentPlayingList.addAll(newList)
            playSong(song, false)
        }
    }

    // 获取当前播放列表（不包括当前播放的歌曲）
    fun getPlayingQueue(): List<Song> {
        if (currentPlayingList.isEmpty() || currentSong == null) {
            return emptyList()
        }
        val currentIndex = currentPlayingList.indexOfFirst { it.id == currentSong?.id }
        return if (currentIndex != -1 && currentIndex + 1 < currentPlayingList.size) {
            currentPlayingList.subList(currentIndex + 1, currentPlayingList.size).toList()
        } else {
            emptyList()
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause() else it.start()
            isPlaying = it.isPlaying
            updatePlaybackState(isPlaying)
        }
    }

    fun playNext() {
        if (currentPlayingList.isEmpty()) return
        val idx = currentPlayingList.indexOfFirst { it.id == currentSong?.id }
        if (idx != -1) {
            val nextIdx = (idx + 1) % currentPlayingList.size
            playSong(currentPlayingList[nextIdx], false)
        }
    }

    fun playPrevious() {
        if (currentPlayingList.isEmpty()) return
        val idx = currentPlayingList.indexOfFirst { it.id == currentSong?.id }
        if (idx != -1) {
            val prevIdx = if (idx <= 0) currentPlayingList.size - 1 else idx - 1
            playSong(currentPlayingList[prevIdx], false)
        }
    }

    fun seekTo(pos: Float) {
        mediaPlayer?.seekTo(pos.toInt())
        currentPosition = pos.toLong()
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                if (isPlaying) {
                    currentPosition = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    // 🚩 核心修复：每秒钟同步一次给系统，确保锁屏进度条在走
                    updatePlaybackState(true)
                }
                delay(1000) // 1秒同步一次即可，减少性能开销
            }
        }
    }

    // --- 数据库操作 ---
    fun refreshData() {
        viewModelScope.launch {
            try {
                // 检查数据库文件是否存在
                val dbFile = context.getDatabasePath("am_player_db")
                android.util.Log.d("refreshData", "数据库文件路径: ${dbFile.absolutePath}")
                android.util.Log.d("refreshData", "数据库文件是否存在: ${dbFile.exists()}")
                android.util.Log.d("refreshData", "数据库文件大小: ${if (dbFile.exists()) dbFile.length() else 0} bytes")

                val all = songDao.getAllSongs()
                android.util.Log.d("refreshData", "Total songs from DB: ${all.size}")
                
                val convertedSongs = all.map { entity ->
                    try {
                        entity.toSong()
                    } catch (e: Exception) {
                        android.util.Log.e("refreshData", "Failed to convert song (id=${entity.id}, title=${entity.title}): ${e.message}")
                        null
                    }
                }.filterNotNull()
                
                android.util.Log.d("refreshData", "Successfully converted songs: ${convertedSongs.size}")
                libraryList = convertedSongs

                val recentFromDb = songDao.getRecentSongs().map { it.toSong() }
                val playlistEntities = playlistDao.getAllPlaylists()

                withContext(Dispatchers.Main) {
                    recentSongs.clear()
                    recentSongs.addAll(recentFromDb)
                    playlists.clear()
                    playlists.addAll(playlistEntities.map { it.toPlaylist() })
                }
                if (currentPlayingList.isEmpty()) {
                    currentPlayingList.clear()
                    currentPlayingList.addAll(libraryList)
                }
            } catch (e: Exception) {
                android.util.Log.e("refreshData", "Failed to refresh data: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            songDao.deleteSong(song.toEntity())
            refreshData()
            if (currentSong?.id == song.id) {
                mediaPlayer?.stop()
                isPlaying = false
            }
        }
    }

    // 开始编辑歌曲
    fun startEditSong(song: Song) {
        editingSong = song
        editTitle = song.title
        editArtist = song.artist
        editCoverUri = song.coverUri?.let { Uri.parse(it) }
        editLrcUri = song.lrcPath?.let { Uri.parse(it) }
    }

    // 保存编辑的歌曲
    fun saveEditedSong() {
        val song = editingSong ?: return
        if (editTitle.isBlank()) return

        viewModelScope.launch {
            // 如果更换了封面或歌词，需要复制新文件
            val newCoverPath = editCoverUri?.let { uri ->
                if (uri.toString() != song.coverUri) {
                    copyFile(uri, "cov_${System.currentTimeMillis()}.jpg")
                } else {
                    song.coverUri
                }
            }

            val newLrcPath = editLrcUri?.let { uri ->
                                val uriString = uri.toString()
                                if (uriString.startsWith("/")) {
                                    // 已经是本地文件路径，直接使用
                                    uriString
                                } else if (uriString.startsWith("file://")) {
                                    // 是 file:// 格式的 URI，提取路径部分
                                    uriString.substring(7)
                                } else if (uriString != song.lrcPath) {
                                    // 是内容选择器的 URI 且路径不同，需要复制到本地
                                    copyFile(uri, "lrc_${System.currentTimeMillis()}.lrc")
                                } else {
                                    song.lrcPath
                                }
                            }
            val updatedSong = song.copy(
                title = editTitle,
                artist = editArtist,
                coverUri = newCoverPath,
                lrcPath = newLrcPath
            )

            songDao.updateSong(updatedSong.toEntity())
            refreshData()

            // 如果正在播放这首歌，更新当前歌曲信息
            if (currentSong?.id == song.id) {
                currentSong = updatedSong
                updateMediaSession(updatedSong)
            }

            // 清理编辑状态
            editingSong = null
            editTitle = ""
            editArtist = ""
            editCoverUri = null
            editLrcUri = null
        }
    }

    // 取消编辑
    fun cancelEditSong() {
        editingSong = null
        editTitle = ""
        editArtist = ""
        editCoverUri = null
        editLrcUri = null
    }

    // --- 系统通知栏同步 ---
    private fun updateMediaSession(song: Song) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)

        song.coverUri?.let { path ->
            if (File(path).exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            }
        }
        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun updatePlaybackState(playing: Boolean) {
        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val stateBuilder = PlaybackStateCompat.Builder()
            // 🚩 核心修复：传入 currentPosition，系统进度条才会显示正确位置
            .setState(state, currentPosition, 1.0f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO // 🚩 核心修复：启用进度条拖动权限
            )
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun updateBlurBackground(path: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = if (path != null && File(path).exists()) {
                BitmapFactory.decodeFile(path)
            } else {
                // 加载默认封面
                val resourceId = context.resources.getIdentifier("default_cover", "drawable", context.packageName)
                BitmapFactory.decodeResource(context.resources, resourceId)
            }
            val blurred = bitmap?.let { BlurUtil.doBlur(it, 8, 20) }
            withContext(Dispatchers.Main) { blurredBackground = blurred }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaSession?.release()
    }

    // --- 网络获取封面 ---
    suspend fun fetchCoverFromNetwork(title: String, artist: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isFetchingCover = true
                    fetchCoverError = null
                }

                val keywords = "$title $artist"
                Log.d("FetchCover", "开始搜索封面: keywords=$keywords")

                // 构建请求 URL - 网易云 API
                val testUrl = "https://api.yaohud.cn/api/music/wyjiexi?key=v3ywJo5vIfAHRz9lIRg&type=so&name=${java.net.URLEncoder.encode(keywords, "UTF-8")}&size=standard"
                Log.d("FetchCover", "请求 URL: $testUrl")

                val connection = java.net.URL(testUrl).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d("FetchCover", "HTTP 响应码: $responseCode")

                if (responseCode == 200) {
                    val rawResponse = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("FetchCover", "原始响应内容: $rawResponse")

                    // 保存原始响应数据用于调试
                    withContext(Dispatchers.Main) {
                        apiResponseData = rawResponse
                    }

                    // 尝试解析 JSON
                    val gson = com.google.gson.Gson()
                    try {
                        val response = gson.fromJson(rawResponse, com.music.purelymusic.model.WangYiResponse::class.java)

                        Log.d("FetchCover", "解析后: code=${response.code}, data=${response.data}, data size=${response.data?.size ?: 0}")

                        if (response.code == 200 && response.data != null && response.data.isNotEmpty()) {
                            val coverUrl = response.data.firstOrNull()?.picUrl
                            Log.d("FetchCover", "获取的封面 URL: $coverUrl")

                            if (!coverUrl.isNullOrEmpty()) {
                                // 下载封面图片到本地
                                try {
                                    val coverConnection = java.net.URL(coverUrl).openConnection() as java.net.HttpURLConnection
                                    coverConnection.requestMethod = "GET"
                                    coverConnection.connect()

                                    if (coverConnection.responseCode == 200) {
                                        val inputStream = coverConnection.inputStream
                                        val fileName = "cover_${System.currentTimeMillis()}.jpg"
                                        val file = java.io.File(context.filesDir, fileName)
                                        val outputStream = java.io.FileOutputStream(file)

                                        inputStream.copyTo(outputStream)
                                        inputStream.close()
                                        outputStream.close()

                                        val localPath = file.absolutePath
                                        Log.d("FetchCover", "封面已下载到本地: $localPath")
                                        localPath
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            fetchCoverError = "下载封面失败: HTTP ${coverConnection.responseCode}"
                                        }
                                        null
                                    }
                                } catch (e: Exception) {
                                    Log.e("FetchCover", "下载封面失败", e)
                                    withContext(Dispatchers.Main) {
                                        fetchCoverError = "下载封面失败: ${e.message}"
                                    }
                                    null
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    fetchCoverError = "未找到封面\n原始响应: ${rawResponse.take(200)}"
                                }
                                null
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                fetchCoverError = "API 错误 (${response.code}): 未找到匹配歌曲\n原始响应: ${rawResponse.take(200)}"
                            }
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("FetchCover", "JSON 解析失败", e)
                        withContext(Dispatchers.Main) {
                            fetchCoverError = "JSON 解析失败: ${e.message}\n原始响应: ${rawResponse.take(200)}"
                        }
                        null
                    }
                } else {
                    val errorMsg = "HTTP 错误: $responseCode"
                    Log.e("FetchCover", errorMsg)
                    withContext(Dispatchers.Main) {
                        fetchCoverError = errorMsg
                    }
                    null
                }
            } catch (e: Exception) {
            val errorMsg = "获取封面失败: ${e.javaClass.simpleName} - ${e.message}"
                Log.e("FetchCover", errorMsg, e)
                withContext(Dispatchers.Main) {
                    fetchCoverError = errorMsg
                }
                e.printStackTrace()
                null
            } finally {
                withContext(Dispatchers.Main) {
                    isFetchingCover = false
                }
            }
        }
    }

    fun clearFetchCoverError() {
        fetchCoverError = null
    }

    fun clearFetchLrcError() {
        fetchLrcError = null
    }

    // --- 自动获取LRC歌词 ---
    suspend fun fetchLrcFromNetwork(title: String, artist: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isFetchingLrc = true
                    fetchLrcError = null
                }

                val keywords = "$title $artist"
                Log.d("FetchLrc", "开始获取LRC: keywords=$keywords")

                // 步骤1: 先调用封面API获取歌曲ID
                val searchUrl = "https://api.yaohud.cn/api/music/wyjiexi?key=v3ywJo5vIfAHRz9lIRg&type=so&name=${java.net.URLEncoder.encode(keywords, "UTF-8")}&size=standard"
                Log.d("FetchLrc", "搜索歌曲URL: $searchUrl")

                val searchConnection = java.net.URL(searchUrl).openConnection() as java.net.HttpURLConnection
                searchConnection.requestMethod = "GET"
                searchConnection.connectTimeout = 10000
                searchConnection.readTimeout = 10000

                val searchResponseCode = searchConnection.responseCode
                if (searchResponseCode != 200) {
                    withContext(Dispatchers.Main) {
                        fetchLrcError = "搜索歌曲失败: HTTP $searchResponseCode"
                    }
                    return@withContext null
                }

                val searchResponse = searchConnection.inputStream.bufferedReader().use { it.readText() }
                Log.d("FetchLrc", "搜索响应: $searchResponse")

                val gson = com.google.gson.Gson()
                val searchResult = gson.fromJson(searchResponse, com.music.purelymusic.model.WangYiResponse::class.java)

                if (searchResult.code != 200 || searchResult.data.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        fetchLrcError = "未找到匹配的歌曲"
                    }
                    return@withContext null
                }

                val songId = searchResult.data.firstOrNull()?.id?.toString()
                if (songId == null) {
                    withContext(Dispatchers.Main) {
                        fetchLrcError = "无法获取歌曲ID"
                    }
                    return@withContext null
                }

                Log.d("FetchLrc", "获取到歌曲ID: $songId")

                // 步骤2: 使用歌曲ID调用LRC API获取歌词
                val lrcUrl = "https://api.yaohud.cn/api/music/lrc?key=v3ywJo5vIfAHRz9lIRg&mid=$songId&type=wy"
                Log.d("FetchLrc", "获取LRC URL: $lrcUrl")

                val lrcConnection = java.net.URL(lrcUrl).openConnection() as java.net.HttpURLConnection
                lrcConnection.requestMethod = "GET"
                lrcConnection.connectTimeout = 10000
                lrcConnection.readTimeout = 10000

                val lrcResponseCode = lrcConnection.responseCode
                if (lrcResponseCode != 200) {
                    withContext(Dispatchers.Main) {
                        fetchLrcError = "获取歌词失败: HTTP $lrcResponseCode"
                    }
                    return@withContext null
                }

                val lrcResponse = lrcConnection.inputStream.bufferedReader().use { it.readText() }
                Log.d("FetchLrc", "LRC响应: $lrcResponse")

                val lrcResult = gson.fromJson(lrcResponse, com.music.purelymusic.model.LrcApiResponse::class.java)

                if (lrcResult.code != 200 || lrcResult.data?.lyric == null) {
                    withContext(Dispatchers.Main) {
                        fetchLrcError = "未找到歌词"
                    }
                    return@withContext null
                }

                val lrcContent = lrcResult.data.lyric
                Log.d("FetchLrc", "获取到LRC内容，长度: ${lrcContent.length}")

                // 保存LRC到本地文件
                val fileName = "lrc_${System.currentTimeMillis()}.lrc"
                val file = java.io.File(context.filesDir, fileName)
                file.writeText(lrcContent, Charsets.UTF_8)

                val localPath = file.absolutePath
                Log.d("FetchLrc", "LRC已保存到本地: $localPath")
                localPath

            } catch (e: Exception) {
                val errorMsg = "获取LRC失败: ${e.javaClass.simpleName} - ${e.message}"
                Log.e("FetchLrc", errorMsg, e)
                withContext(Dispatchers.Main) {
                    fetchLrcError = errorMsg
                }
                null
            } finally {
                withContext(Dispatchers.Main) {
                    isFetchingLrc = false
                }
            }
        }
    }
}