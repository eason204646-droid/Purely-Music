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
        currentPlayingList = if (isRandom) songs.shuffled() else songs
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
        val mUri = tempMusicUri ?: return
        viewModelScope.launch {
            // 拷贝文件到私有目录，防止系统清理或权限丢失
            val pMusic = copyFile(mUri, "mus_${System.currentTimeMillis()}.mp3")
            val pCover = tempCoverUri?.let { copyFile(it, "cov_${System.currentTimeMillis()}.jpg") }
            val pLrc = tempLrcUri?.let { copyFile(it, "lrc_${System.currentTimeMillis()}.lrc") }

            if (pMusic != null) {
                val newSong = Song(
                    id = 0, // Room 会自动生成
                    title = title,
                    artist = artist,
                    coverUri = pCover,
                    musicUri = pMusic,
                    lrcPath = pLrc
                )
                // 存入数据库
                songDao.insertSong(newSong.toEntity())

                // 重置临时状态并刷新
                tempMusicUri = null
                tempCoverUri = null
                tempLrcUri = null
                refreshData()
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
    private var currentPlayingList: List<Song> = emptyList()
    var selectedSongsForPlaylist = mutableStateListOf<Song>()

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
                val bytes = if (pathOrUri.startsWith("/")) {
                    val file = File(pathOrUri)
                    if (file.exists()) file.readBytes() else null
                } else {
                    context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use { it.readBytes() }
                }

                if (bytes == null || bytes.isEmpty()) {
                    withContext(Dispatchers.Main) { lyricLines = emptyList() }
                    return@launch
                }

                // 尝试多种编码防止乱码
                var parsed = LrcParser.parse(String(bytes, Charsets.UTF_8))
                if (parsed.isEmpty()) {
                    parsed = LrcParser.parse(String(bytes, Charset.forName("GBK")))
                }

                withContext(Dispatchers.Main) {
                    lyricLines = parsed
                }
            } catch (e: Exception) {
                Log.e("LyricLoad", "Failed: ${e.message}")
                withContext(Dispatchers.Main) { lyricLines = emptyList() }
            }
        }
    }

    // --- 播放控制逻辑 ---
    fun playSong(song: Song, updateInternalList: Boolean = true) {
        if (updateInternalList) currentPlayingList = libraryList

        if (currentSong?.id == song.id && mediaPlayer != null) {
            togglePlayPause()
            return
        }

        mediaPlayer?.stop()
        mediaPlayer?.release()
        currentSong = song

        // 加载歌词
        if (!song.lrcPath.isNullOrEmpty()) {
            loadLyrics(song.lrcPath)
        } else {
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
            val nextSong = currentPlayingList[(idx + 1) % currentPlayingList.size]
            playSong(nextSong, false)
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
            val all = songDao.getAllSongs()
            libraryList = all.map { it.toSong() }

            val recentFromDb = songDao.getRecentSongs().map { it.toSong() }
            val playlistEntities = playlistDao.getAllPlaylists()

            withContext(Dispatchers.Main) {
                recentSongs.clear()
                recentSongs.addAll(recentFromDb)
                playlists.clear()
                playlists.addAll(playlistEntities.map { it.toPlaylist() })
            }
            if (currentPlayingList.isEmpty()) currentPlayingList = libraryList
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
                if (uri.toString() != song.lrcPath) {
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
}