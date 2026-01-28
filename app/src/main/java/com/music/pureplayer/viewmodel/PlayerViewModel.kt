package com.music.PurelyPlayer.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.PurelyPlayer.data.PlaylistEntity
import com.music.PurelyPlayer.data.AppDatabase
import com.music.PurelyPlayer.data.toEntity
import com.music.PurelyPlayer.data.toPlaylist
import com.music.PurelyPlayer.model.Playlist
import com.music.PurelyPlayer.model.Song
import com.music.PurelyPlayer.model.toEntity
import com.music.PurelyPlayer.model.toSong
import com.music.pureplayer.ui.utils.BlurUtil
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val songDao = AppDatabase.getDatabase(application).songDao()
    private val playlistDao = AppDatabase.getDatabase(application).playlistDao()
    private var mediaPlayer: MediaPlayer? = null

    // 🚩 修复 1：显式定义 MediaSessionCompat 类型，解决 Unresolved 报错
    private var mediaSession: MediaSessionCompat? = null

    // --- 🚩 状态变量声明 ---
    var recentSongs = mutableStateListOf<Song>()
        private set
    var libraryList by mutableStateOf<List<Song>>(emptyList())
    var currentSong by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var blurredBackground by mutableStateOf<android.graphics.Bitmap?>(null)
    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var playlists = mutableStateListOf<Playlist>()
        private set
    private var currentPlayingList: List<Song> = emptyList()
    var selectedSongsForPlaylist = mutableStateListOf<Song>()
    var tempPlaylistCoverUri by mutableStateOf<Uri?>(null)
    var tempMusicUri by mutableStateOf<Uri?>(null)
    var tempCoverUri by mutableStateOf<Uri?>(null)

    init {
        // 🚩 修复 2：初始化 MediaSession，确保 Context 传递正确
        mediaSession = MediaSessionCompat(context, "PurelyPlayer").apply {
            isActive = true
        }
        refreshData()
        startTimer()
    }

    /**
     * 更新系统媒体元数据（标题、歌手、封面）
     */
    private fun updateMediaSession(song: Song) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)

        song.coverUri?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                // 🚩 显式调用，解决 Bitmap 类型推断
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            }
        }

        // 🚩 修复 3：调用 setMetadata 解决 Unresolved
        mediaSession?.setMetadata(metadataBuilder.build())
    }

    /**
     * 更新系统播放状态
     */
    private fun updatePlaybackState(playing: Boolean) {
        val state = if (playing) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val stateBuilder = PlaybackStateCompat.Builder()
            .setState(state, currentPosition, 1.0f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )

        // 🚩 修复 4：调用 setPlaybackState 解决 Unresolved
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause() else it.start()
            isPlaying = it.isPlaying
            // 🚩 同步系统状态
            updatePlaybackState(isPlaying)
        }
    }

    // --- 保持你原有逻辑不动 ---

    fun savePlaylist(name: String) {
        viewModelScope.launch {
            val finalCoverPath: String? = tempPlaylistCoverUri?.let {
                copyFile(it, "pl_cov_${System.currentTimeMillis()}.jpg")
            }
            val newPlaylist = Playlist(
                name = name,
                coverUri = finalCoverPath,
                songIds = selectedSongsForPlaylist.map { it.id.toLong() }
            )
            playlistDao.insertPlaylist(newPlaylist.toEntity())
            playlists.add(0, newPlaylist)
            selectedSongsForPlaylist.clear()
            tempPlaylistCoverUri = null
        }
    }

    fun playPlaylist(playlist: Playlist, isRandom: Boolean) {
        val songs = libraryList.filter { song: Song ->
            playlist.songIds.contains(song.id.toLong())
        }
        if (songs.isEmpty()) return
        currentPlayingList = if (isRandom) songs.shuffled() else songs
        playSongFromList(currentPlayingList[0])
    }

    private fun playSongFromList(song: Song) {
        playSong(song, updateInternalList = false)
    }

    fun refreshData() {
        viewModelScope.launch {
            // 1. 加载歌曲逻辑
            val all = songDao.getAllSongs()
            libraryList = all.map { it.toSong() }

            // 2. 🚩 修复：加载歌单逻辑
            try {
                // 获取 Entity 列表
                val entities: List<PlaylistEntity> = playlistDao.getAllPlaylists()

                // 显式指定声明，解决 'it' 无法识别的问题
                val loadedPlaylists = entities.map { entity: PlaylistEntity ->
                    entity.toPlaylist()
                }

                playlists.clear()
                playlists.addAll(loadedPlaylists)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. 最近播放逻辑
            val recentFromDb = songDao.getRecentSongs().map { it.toSong() }
            recentSongs.clear()
            recentSongs.addAll(recentFromDb)

            if (currentPlayingList.isEmpty()) {
                currentPlayingList = libraryList
            }
        }
    }

    fun playSong(song: Song, updateInternalList: Boolean = true) {
        if (updateInternalList) currentPlayingList = libraryList
        if (currentSong?.musicUri == song.musicUri && mediaPlayer != null) {
            togglePlayPause()
            return
        }
        recentSongs.removeAll { it.musicUri == song.musicUri }
        recentSongs.add(0, song)
        if (recentSongs.size > 15) recentSongs.removeLast()

        mediaPlayer?.stop()
        mediaPlayer?.release()
        currentSong = song

        // 🚩 核心逻辑：通知系统媒体信息
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
                    updatePlaybackState(true) // 同步系统
                }
                setOnCompletionListener { playNext() }
            }
            viewModelScope.launch {
                songDao.updateSong(song.toEntity(System.currentTimeMillis()))
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun playNext() {
        if (currentPlayingList.isEmpty()) return
        val idx = currentPlayingList.indexOfFirst { it.musicUri == currentSong?.musicUri }
        if (idx != -1) {
            val nextSong = currentPlayingList[(idx + 1) % currentPlayingList.size]
            playSongFromList(nextSong)
        }
    }

    fun playPrevious() {
        if (currentPlayingList.isEmpty()) return
        val idx = currentPlayingList.indexOfFirst { it.musicUri == currentSong?.musicUri }
        if (idx != -1) {
            val prevIndex = if (idx <= 0) currentPlayingList.size - 1 else idx - 1
            playSongFromList(currentPlayingList[prevIndex])
        }
    }

    private fun updateBlurBackground(path: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (path != null && File(path).exists()) {
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                val bitmap = BitmapFactory.decodeFile(path, options)
                val blurred = bitmap?.let { BlurUtil.doBlur(it, 8, 20) }
                withContext(Dispatchers.Main) { blurredBackground = blurred }
            } else {
                withContext(Dispatchers.Main) { blurredBackground = null }
            }
        }
    }

    fun saveSong(title: String, artist: String) {
        val mUri = tempMusicUri ?: return
        viewModelScope.launch {
            val pMusic = copyFile(mUri, "mus_${System.currentTimeMillis()}.mp3")
            val pCover = tempCoverUri?.let { copyFile(it, "cov_${System.currentTimeMillis()}.jpg") }
            if (pMusic != null) {
                songDao.insertSong(Song(0, title, artist, pCover, pMusic).toEntity())
                tempMusicUri = null
                tempCoverUri = null
                refreshData()
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

    fun seekTo(pos: Float) {
        mediaPlayer?.seekTo(pos.toInt())
        currentPosition = pos.toLong()
        updatePlaybackState(isPlaying) // 同步进度
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                if (isPlaying) currentPosition = mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(1000)
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            songDao.deleteSong(song.toEntity())
            refreshData()
            if (currentSong?.musicUri == song.musicUri) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                currentSong = null
                isPlaying = false
                updatePlaybackState(false)
            }
            recentSongs.removeAll { it.musicUri == song.musicUri }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaSession?.release() // 释放资源
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
                    android.util.Log.e("PurelyPlayer", "数据库更新失败: ${e.message}")
                }
            }
        }
    }
}