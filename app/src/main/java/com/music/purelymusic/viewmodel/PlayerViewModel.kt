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
import android.app.Application
import android.content.res.Configuration
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.Spatializer
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.music.purelymusic.BuildConfig
import com.music.purelymusic.R
import com.music.purelymusic.data.AppDatabase
import com.music.purelymusic.data.AppFileStore
import com.music.purelymusic.data.MetadataRepository
import com.music.purelymusic.data.toEntity
import com.music.purelymusic.data.toPlaylist
import com.music.purelymusic.data.toSongRefs
import com.music.purelymusic.data.toAlbum
import com.music.purelymusic.model.*
import com.music.purelymusic.utils.LrcParser
import com.music.purelymusic.utils.LyricTranslationParser
import com.music.purelymusic.ui.utils.BlurUtil
import com.music.purelymusic.playback.PlaybackRuntime
import com.music.purelymusic.playback.PlaybackService
import retrofit2.Retrofit
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.charset.Charset
import kotlin.math.sin
import java.util.Locale

@androidx.annotation.OptIn(UnstableApi::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private fun playSongFromList(song: Song) {
        playSong(song, updateInternalList = false)
    }
    fun playPlaylist(playlist: Playlist, isRandom: Boolean) {
        val latestPlaylist = playlists.firstOrNull { it.id == playlist.id } ?: playlist
        val songMap = libraryList.associateBy { it.id.toLong() }
        val songs = latestPlaylist.songIds.mapNotNull { songId -> songMap[songId] }
        if (songs.isEmpty()) return
        currentPlayingList.clear()
        currentPlayingList.addAll(if (isRandom) songs.shuffled() else songs)
        playSongFromList(currentPlayingList[0])
    }

    private suspend fun findOrCreateAlbumId(
        albumName: String?,
        albumArtist: String?,
        fallbackArtist: String,
        coverPath: String?
    ): String? = albumUpdateMutex.withLock {
        if (albumName.isNullOrBlank()) return@withLock null
        val artist = albumArtist?.takeIf { it.isNotBlank() } ?: fallbackArtist
        albumDao.getAlbumByNameAndArtist(albumName, artist)?.let { return@withLock it.id }

        val album = Album(
            id = java.util.UUID.randomUUID().toString(),
            name = albumName,
            artist = artist,
            coverUri = coverPath
        )
        albumDao.insertAlbum(album.toEntity())
        album.id
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 从数据库中删除 (使用你修好的 toEntity 函数)
            playlistDao.deletePlaylist(playlist.toEntity())
            deleteFileIfUnreferenced(playlist.coverUri)

            // 2. 从当前内存列表中移除，这样 UI 才会立刻刷新
            // 假设你的 playlists 是一个 MutableStateList 或者 MutableList
            withContext(Dispatchers.Main) { playlists.remove(playlist) }
        }
    }

    private suspend fun deleteFileIfUnreferenced(path: String?) {
        if (path.isNullOrBlank()) return
        val references = songDao.countPathReferences(path) +
            playlistDao.countCoverReferences(path) +
            albumDao.countCoverReferences(path)
        if (references == 0) fileStore.deleteOwnedFile(path)
    }
    fun saveSong(title: String, artist: String) {
        val mUri = tempMusicUri
        android.util.Log.d("purelymusic", "saveSong 被调用: title=$title, artist=$artist, tempMusicUri=$mUri")
        if (mUri == null) {
            android.util.Log.e("purelymusic", "tempMusicUri 为 null，无法保存歌曲")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val createdPaths = mutableListOf<String>()
            try {
                android.util.Log.d("purelymusic", "开始复制文件")
                // 拷贝文件到私有目录，防止系统清理或权限丢失（不指定扩展名，让 copyFile 自动检测）
                val pMusic = copyFile(mUri, "mus_${System.currentTimeMillis()}")
                pMusic?.let(createdPaths::add)
                
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
                if (tempCoverUri?.toString()?.startsWith("/") == false) pCover?.let(createdPaths::add)
                
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
                val originalLrc = tempLrcUri?.toString()
                if (originalLrc != null && !originalLrc.startsWith("/") && !originalLrc.startsWith("file://")) {
                    pLrc?.let(createdPaths::add)
                }

                android.util.Log.d("purelymusic", "文件复制结果: pMusic=$pMusic, pCover=$pCover, pLrc=$pLrc")

                if (pMusic != null) {
                    // 处理专辑逻辑
                    val albumName = tempAlbumName
                    val albumArtist = tempAlbumArtist

                    database.withTransaction {
                        val albumId = findOrCreateAlbumId(albumName, albumArtist, artist, pCover)
                        val newSong = Song(
                            id = 0,
                            title = title,
                            artist = artist,
                            coverUri = pCover,
                            musicUri = pMusic,
                            lrcPath = pLrc,
                            album = albumName,
                            albumId = albumId
                        )
                        songDao.insertSong(newSong.toEntity())
                    }
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
                        tempAlbumName = null
                        tempAlbumArtist = null
                        refreshData()
                    }
                } else {
                    createdPaths.forEach(fileStore::deleteOwnedFile)
                    val errorMsg = "复制音乐文件失败"
                    android.util.Log.e("purelymusic", errorMsg)
                    withContext(Dispatchers.Main) {
                        saveSongError = errorMsg
                    }
                }
            } catch (e: Exception) {
                createdPaths.forEach(fileStore::deleteOwnedFile)
                val errorMsg = "保存歌曲失败: ${e.message}"
                android.util.Log.e("purelymusic", errorMsg, e)
                withContext(Dispatchers.Main) {
                    saveSongError = errorMsg
                }
            }
        }
    }

    fun clearSaveSongError() {
        saveSongError = null
    }
    fun updatePlaylistSongs(playlistId: String, newSongIds: List<Long>) {
        mutatePlaylist(playlistId) { it.copy(songIds = newSongIds) }
    }

    // 从歌单中删除歌曲
    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        mutatePlaylist(playlistId) { playlist ->
            playlist.copy(songIds = playlist.songIds.filter { it != songId })
        }
    }

    // 添加歌曲到歌单
    fun addSongsToPlaylist(playlistId: String, songIds: List<Long>) {
        mutatePlaylist(playlistId) { playlist ->
            playlist.copy(songIds = (playlist.songIds + songIds).distinct())
        }
    }

    private val playlistUpdateMutex = Mutex()
    private val albumUpdateMutex = Mutex()

    private fun mutatePlaylist(playlistId: String, transform: (Playlist) -> Playlist) {
        viewModelScope.launch {
            playlistUpdateMutex.withLock {
                val index = playlists.indexOfFirst { it.id == playlistId }
                if (index < 0) return@withLock
                val updated = transform(playlists[index]).copy(updatedAt = System.currentTimeMillis())
                try {
                    playlistDao.upsertPlaylist(updated.toEntity(), updated.toSongRefs())
                    playlists[index] = updated
                } catch (error: Exception) {
                    Log.e("Playlist", "更新歌单失败", error)
                    if (currentLanguage == "zh") {
                        saveSongError = "更新歌单失败，请重试"
                    } else {
                        saveSongError = "Could not update playlist. Please retry."
                    }
                }
            }
        }
    }

    /**
     * 从 URI 获取文件扩展名
     */
    private fun getFileExtension(uri: Uri): String {
        // 1) Try display name from ContentResolver
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(nameIndex)
                val dotIndex = name?.lastIndexOf('.') ?: -1
                if (dotIndex >= 0 && dotIndex < name.length - 1) {
                    return name.substring(dotIndex).lowercase()
                }
            }
        }

        // 2) Try MIME type
        val mimeType = context.contentResolver.getType(uri)?.lowercase()
        if (mimeType != null) {
            return when (mimeType) {
                "audio/mpeg", "audio/mp3" -> ".mp3"
                "audio/mp4", "audio/m4a", "audio/x-m4a", "audio/mp4a-latm", "audio/mp4a" -> ".m4a"
                "audio/ogg", "audio/x-ogg" -> ".ogg"
                "audio/flac", "audio/x-flac" -> ".flac"
                "audio/wav", "audio/x-wav" -> ".wav"
                "audio/aac", "audio/x-aac", "audio/aacp" -> ".aac"
                "audio/opus" -> ".opus"
                "audio/ape", "audio/x-ape" -> ".ape"
                "audio/amr" -> ".amr"
                "audio/3gpp" -> ".3gp"
                "audio/3gpp2" -> ".3g2"
                "audio/x-matroska" -> ".mka"
                "audio/x-ms-wma" -> ".wma"
                "image/jpeg", "image/jpg" -> ".jpg"
                "image/png" -> ".png"
                "text/plain", "application/lrc" -> ".lrc"
                else -> ""
            }
        }

        // 3) Fallback to path
        val path = uri.path
        val dotIndex = path?.lastIndexOf('.') ?: -1
        return if (dotIndex >= 0 && dotIndex < (path?.length ?: 0) - 1) {
            "." + path?.substring(dotIndex + 1)?.lowercase()
        } else {
            ""
        }
    }

    private fun guessMimeTypeFromExtension(extension: String): String? {
        return when (extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a", "mp4" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "ape" -> "audio/ape"
            "wma" -> "audio/x-ms-wma"
            "amr" -> "audio/amr"
            "3gp", "3gpp" -> "audio/3gpp"
            "3g2", "3gpp2" -> "audio/3gpp2"
            "mka" -> "audio/x-matroska"
            else -> null
        }
    }

    private fun resolveMimeType(musicPath: String): String? {
        return try {
            if (musicPath.startsWith("content://")) {
                val uri = Uri.parse(musicPath)
                val resolverType = context.contentResolver.getType(uri)
                if (!resolverType.isNullOrBlank()) {
                    return resolverType
                }
                val ext = getFileExtension(uri).trimStart('.')
                if (ext.isNotBlank()) {
                    return guessMimeTypeFromExtension(ext)
                }
                null
            } else {
                val ext = musicPath.substringAfterLast('.', "").lowercase()
                if (ext.isNotBlank()) guessMimeTypeFromExtension(ext) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getDisplayName(uri: Uri): String? {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(nameIndex)
                if (!name.isNullOrBlank()) {
                    return name
                }
            }
        }
        return null
    }

    private suspend fun copyFile(uri: Uri, fileName: String): String? =
        fileStore.copyFromUri(uri, fileName)

    fun savePlaylist(name: String) {
        viewModelScope.launch {
            val finalCoverPath: String? = tempPlaylistCoverUri?.let {
                copyFile(it, "pl_cov_" + System.currentTimeMillis() + ".jpg")
            }
            val newPlaylist = Playlist(
                name = name,
                coverUri = finalCoverPath,
                songIds = selectedSongsForPlaylist.map { it.id.toLong() },
                description = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            playlistDao.upsertPlaylist(newPlaylist.toEntity(), newPlaylist.toSongRefs())
            playlists.add(0, newPlaylist)
            selectedSongsForPlaylist.clear()
            tempPlaylistCoverUri = null
        }
    }


    private val context get() = getApplication<Application>().applicationContext
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val database = AppDatabase.getDatabase(application)
    private val songDao = database.songDao()
    private val playlistDao = database.playlistDao()
    private val albumDao = database.albumDao()
    private val fileStore = AppFileStore(application)
    private val metadataRepository = MetadataRepository(BuildConfig.MUSIC_API_KEY, fileStore)
    private val playbackRuntime = PlaybackRuntime.get(context)
    private var exoPlayer: ExoPlayer? = playbackRuntime.player
    private var transitionPlayer: ExoPlayer? = null
    private var crossfadeJob: Job? = null
    private var isCrossfadeInProgress = false
    private var hasScheduledCrossfadeForCurrentSong = false
    private var suppressNextEndedCallback = false
    private val crossfadeDurationMs: Long
        get() = crossfadeDurationSeconds * 1000L
    var isActuallyPlaying by mutableStateOf(false)
        private set

    private val runtimePlayerChangeListener: (ExoPlayer) -> Unit = { player ->
        exoPlayer?.removeListener(playerListener)
        exoPlayer = player
        player.addListener(playerListener)
        syncPlaybackState()
    }
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            syncPlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncPlaybackState()
            if (playbackState == Player.STATE_READY) {
                this@PlayerViewModel.duration = exoPlayer?.duration ?: 0L
                attachEqualizerToCurrentSession()
            } else if (playbackState == Player.STATE_ENDED) {
                if (suppressNextEndedCallback) {
                    suppressNextEndedCallback = false
                    return
                }
                if (isCrossfadeInProgress) return
                playNextInternal(allowAutoCrossfade = false)
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            tracks.groups.forEach { group ->
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.mediaTrackGroup.length) {
                        val format = group.mediaTrackGroup.getFormat(i)
                        Log.d(
                            "ExoTrack",
                            "audio format: mime=${format.sampleMimeType}, codecs=${format.codecs}, " +
                                "sr=${format.sampleRate}, ch=${format.channelCount}"
                        )
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val songId = mediaItem?.mediaId?.toLongOrNull() ?: return
            val song = currentPlayingList.firstOrNull { it.id == songId }
                ?: libraryList.firstOrNull { it.id == songId }
                ?: return
            if (currentSong?.id != song.id) {
                updateSongState(song)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e("PlayError", "ExoPlayer错误: ${error.errorCodeName}, ${error.message}")
            this@PlayerViewModel.isPlaying = false
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing

        return playbackRuntime.player.also {
            it.addListener(playerListener)
            exoPlayer = it
        }
    }

    private fun createPlayer(): ExoPlayer {
        return playbackRuntime.createPlayer()
    }

    private fun setPlayerVolume(player: ExoPlayer?, left: Float, right: Float) {
        val volume = ((left + right) / 2f).coerceIn(0.0f, 1.0f)
        player?.volume = volume
    }

    private var equalizer: Equalizer? = null
    private var equalizerSessionId: Int? = null
    var equalizerBandLevels by mutableStateOf<List<Short>>(emptyList())
        private set
    var equalizerBandFrequencies by mutableStateOf<List<Pair<Int, Int>>>(emptyList())
        private set
    var equalizerLevelRange by mutableStateOf((-1500).toShort() to 1500.toShort())
        private set

    private fun attachEqualizerToCurrentSession() {
        if (!equalizerEnabled) {
            releaseEqualizer()
            return
        }
        val sessionId = exoPlayer?.audioSessionId ?: return
        if (sessionId == 0 || equalizerSessionId == sessionId) return

        releaseEqualizer()

        runCatching {
            Equalizer(0, sessionId).apply {
                enabled = true
                equalizer = this
                equalizerSessionId = sessionId
                equalizerLevelRange = bandLevelRange[0] to bandLevelRange[1]
                equalizerBandFrequencies = List(numberOfBands.toInt()) { band ->
                    val range = getBandFreqRange(band.toShort())
                    (range[0] / 1000) to (range[1] / 1000)
                }
                val savedBands = com.music.purelymusic.utils.PreferencesManager.getEqualizerBands()
                equalizerBandLevels = List(numberOfBands.toInt()) { band ->
                    val saved = savedBands?.getOrNull(band)
                    if (saved != null) {
                        runCatching { setBandLevel(band.toShort(), saved) }
                        saved
                    } else {
                        getBandLevel(band.toShort())
                    }
                }
            }
        }.onFailure {
            releaseEqualizer()
        }
    }

    fun updateEqualizerBandLevel(bandIndex: Int, level: Short) {
        val effect = equalizer ?: return
        if (bandIndex !in equalizerBandLevels.indices) return
        val clamped = level.coerceIn(equalizerLevelRange.first, equalizerLevelRange.second)
        runCatching {
            effect.setBandLevel(bandIndex.toShort(), clamped)
        }.onSuccess {
            val newLevels = equalizerBandLevels.toMutableList().apply {
                this[bandIndex] = clamped
            }
            equalizerBandLevels = newLevels
            com.music.purelymusic.utils.PreferencesManager.saveEqualizerBands(newLevels)
        }
    }

    fun resetEqualizerBands() {
        val effect = equalizer ?: return
        val zeroLevel = 0.toShort().coerceIn(equalizerLevelRange.first, equalizerLevelRange.second)
        repeat(equalizerBandLevels.size) { bandIndex ->
            runCatching {
                effect.setBandLevel(bandIndex.toShort(), zeroLevel)
            }
        }
        val resetLevels = List(equalizerBandLevels.size) { zeroLevel }
        equalizerBandLevels = resetLevels
        com.music.purelymusic.utils.PreferencesManager.saveEqualizerBands(resetLevels)
    }

    private fun releaseEqualizer() {
        runCatching { equalizer?.release() }
        equalizer = null
        equalizerSessionId = null
        equalizerBandLevels = emptyList()
        equalizerBandFrequencies = emptyList()
        equalizerLevelRange = (-1500).toShort() to 1500.toShort()
    }

    private fun setPlayerVolume(left: Float, right: Float) {
        setPlayerVolume(exoPlayer, left, right)
    }

    private fun syncPlaybackState() {
        val playingNow = exoPlayer?.isPlaying == true || transitionPlayer?.isPlaying == true || isCrossfadeInProgress
        isPlaying = playingNow
        isActuallyPlaying = playingNow
    }

    // Android 12+ Spatializer 支持
    private var spatializer: Spatializer? = null
    private var isSpatializerAvailable = false

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

    // 搜索状态
    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<Song>>(emptyList())
        private set

    //: 收藏列表
    var favoriteSongs by mutableStateOf<List<Song>>(emptyList())
        private set

    //: 睡眠定时器
    var sleepTimerMinutes by mutableIntStateOf(0) // 0 = 关闭
        private set
    var sleepTimerRemainingSeconds by mutableIntStateOf(0)
        private set
    var sleepTimerActive by mutableStateOf(false)
        private set
    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private var searchJob: kotlinx.coroutines.Job? = null //: 搜索防抖

    var recentSongs = mutableStateListOf<Song>()
    var playlists = mutableStateListOf<Playlist>()
    var albums = mutableStateListOf<Album>()
    private var currentPlayingList = mutableStateListOf<Song>()
    var selectedSongsForPlaylist = mutableStateListOf<Song>()
    var showPlaylist by mutableStateOf(false)

    // 播放模式
    enum class PlayMode {
        SEQUENTIAL,  // 顺序播放
        REPEAT_ONE,  // 单曲循环
        SHUFFLE      // 随机播放
    }

    private var _playMode by mutableStateOf(PlayMode.SEQUENTIAL)
    var playMode: PlayMode
        get() = _playMode
        set(value) {
            _playMode = value
            exoPlayer?.let(::configurePlayMode)
        }

    // 环绕音状态
    enum class SurroundMode {
        NONE,           // 无效果
        IMMERSIVE,      // 沉浸立体音（多声道，四面八方）
        THREE_D         // 3D环绕音（圆周运动）
    }

    var surroundMode by mutableStateOf(SurroundMode.NONE)  // 当前环绕音模式
    var isSurroundEnabled by mutableStateOf(false)          // 环绕音是否启用

    // 3D环绕音参数
    var surroundRadius by mutableFloatStateOf(400f)    // 圆周半径
    var surroundSpeed by mutableFloatStateOf(2.0f)     // 运动速度

    // 导入临时状态
    var tempPlaylistCoverUri by mutableStateOf<Uri?>(null)
    var tempMusicUri by mutableStateOf<Uri?>(null)
    var tempCoverUri by mutableStateOf<Uri?>(null)
    var tempLrcUri by mutableStateOf<Uri?>(null)
    var tempAlbumName by mutableStateOf<String?>(null)
    var tempAlbumArtist by mutableStateOf<String?>(null)
    var isProcessingImport by mutableStateOf(false)

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

    // 自动获取所有信息的状态
    var isFetchingAll by mutableStateOf(false)
        private set
    var fetchAllError by mutableStateOf<String?>(null)

    // 批量导入状态
    var isBatchImporting by mutableStateOf(false)
        private set
    var batchImportProgress by mutableIntStateOf(0)
        private set
    var batchImportTotal by mutableIntStateOf(0)
        private set
    var batchImportCurrentSong by mutableStateOf<String?>(null)
        private set
    var batchImportImported by mutableIntStateOf(0)
        private set
    var batchImportSkipped by mutableIntStateOf(0)
        private set
    var batchImportFailed by mutableIntStateOf(0)
        private set
    var batchImportSummary by mutableStateOf<String?>(null)
        private set
    
    // 批量导入暂停状态（需要用户输入歌曲信息）
    var batchImportPaused by mutableStateOf(false)
        private set
    var batchImportPendingUri by mutableStateOf<Uri?>(null)
        private set
    var batchImportPendingFileName by mutableStateOf<String?>(null)
        private set
    var batchImportPendingMusicPath by mutableStateOf<String?>(null)
        private set
    
    // 批量导入待处理的歌曲队列
    private var batchImportQueue = mutableListOf<BatchImportItem>()
    
    data class BatchImportItem(
        val uri: Uri,
        val index: Int,
        val musicPath: String? = null,
        val title: String? = null,
        val artist: String? = null
    )

    // 保存歌曲错误状态
    var saveSongError by mutableStateOf<String?>(null)

    // 翻译相关状态
    var showTranslation by mutableStateOf(false)
    var isTranslating by mutableStateOf(false)
    var canTranslate by mutableStateOf(false)
    var translateError by mutableStateOf<String?>(null)
    var translateLogs by mutableStateOf<String>("")

    // 语言设置状态（带持久化）
    private var _currentLanguage by mutableStateOf("zh")
    var currentLanguage: String
        get() = _currentLanguage
        set(value) {
            _currentLanguage = value
            com.music.purelymusic.utils.PreferencesManager.saveLanguage(value)
        }

    // 语言敏感的文本（替代 stringResource，响应语言切换）
    private fun localizedString(resourceId: Int): String {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(_currentLanguage))
        }
        return context.createConfigurationContext(configuration).getString(resourceId)
    }

    val textUnknownTrack: String get() = localizedString(R.string.unknown_track)
    val textUnknownArtist: String get() = localizedString(R.string.unknown_artist)
    val textPlayMode: String get() = localizedString(R.string.play_mode)
    val textModeSwitch: String get() = localizedString(R.string.mode_switch)
    val textClose: String get() = localizedString(R.string.close)
    val textQueueEmpty: String get() = localizedString(R.string.queue_empty)
    val textNowPlaying: String get() = localizedString(R.string.now_playing)
    val textDelete: String get() = localizedString(R.string.delete)

    // 歌词设置状态（带持久化）
    private var _lyricGlowEnabled by mutableStateOf(true)
    var lyricGlowEnabled: Boolean
        get() = _lyricGlowEnabled
        set(value) {
            _lyricGlowEnabled = value
            com.music.purelymusic.utils.PreferencesManager.saveLyricGlow(value)
        }
    
    private var _lyricFilterEnabled by mutableStateOf(false)
    var lyricFilterEnabled: Boolean
        get() = _lyricFilterEnabled
        set(value) {
            _lyricFilterEnabled = value
            com.music.purelymusic.utils.PreferencesManager.saveLyricFilter(value)
        }
    
    private var _lyricStyle by mutableStateOf("multi") // "multi" or "single"
    var lyricStyle: String
        get() = _lyricStyle
        set(value) {
            _lyricStyle = value
            com.music.purelymusic.utils.PreferencesManager.saveLyricStyle(value)
        }

    // 自动获取源设置状态（带持久化）
    private var _autoFetchSource by mutableStateOf("netease") // "netease" or "mixed"
    var autoFetchSource: String
        get() = _autoFetchSource
        set(value) {
            _autoFetchSource = value
            com.music.purelymusic.utils.PreferencesManager.saveAutoFetchSource(value)
        }

    // 自动从元数据获取封面和歌词开关（带持久化）
    private var _autoFetchMetadata by mutableStateOf(true)
    var autoFetchMetadata: Boolean
        get() = _autoFetchMetadata
        set(value) {
            _autoFetchMetadata = value
            com.music.purelymusic.utils.PreferencesManager.saveAutoFetchMetadata(value)
        }

    // 自动切歌交叉渐入渐出开关（带持久化）
    private var _crossfadeEnabled by mutableStateOf(false)
    var crossfadeEnabled: Boolean
        get() = _crossfadeEnabled
        set(value) {
            _crossfadeEnabled = value
            com.music.purelymusic.utils.PreferencesManager.saveCrossfadeEnabled(value)
        }

    private var _crossfadeDurationSeconds by mutableIntStateOf(3)
    var crossfadeDurationSeconds: Int
        get() = _crossfadeDurationSeconds
        set(value) {
            _crossfadeDurationSeconds = value.coerceIn(1, 10)
            com.music.purelymusic.utils.PreferencesManager.saveCrossfadeDurationSeconds(_crossfadeDurationSeconds)
        }

    private var _equalizerEnabled by mutableStateOf(false)
    var equalizerEnabled: Boolean
        get() = _equalizerEnabled
        set(value) {
            _equalizerEnabled = value
            com.music.purelymusic.utils.PreferencesManager.saveEqualizerEnabled(value)
            if (value) attachEqualizerToCurrentSession() else releaseEqualizer()
        }

    // 翻译API服务
    private val translateService: TranslateApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://api.yaohud.cn/api/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(TranslateApiService::class.java)
    }

    init {
        // 初始化 PreferencesManager
        com.music.purelymusic.utils.PreferencesManager.init(context)
        
        // 加载保存的设置
        _currentLanguage = com.music.purelymusic.utils.PreferencesManager.getLanguage()
        _lyricGlowEnabled = com.music.purelymusic.utils.PreferencesManager.getLyricGlow()
        _lyricFilterEnabled = com.music.purelymusic.utils.PreferencesManager.getLyricFilter()
        _lyricStyle = com.music.purelymusic.utils.PreferencesManager.getLyricStyle()
        _autoFetchSource = com.music.purelymusic.utils.PreferencesManager.getAutoFetchSource()
        _autoFetchMetadata = com.music.purelymusic.utils.PreferencesManager.getAutoFetchMetadata()
        _crossfadeEnabled = com.music.purelymusic.utils.PreferencesManager.getCrossfadeEnabled()
        _crossfadeDurationSeconds = com.music.purelymusic.utils.PreferencesManager.getCrossfadeDurationSeconds()
        _equalizerEnabled = com.music.purelymusic.utils.PreferencesManager.getEqualizerEnabled()

        exoPlayer?.addListener(playerListener)
        playbackRuntime.addPlayerChangeListener(runtimePlayerChangeListener)

        // 初始化 Spatializer (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            try {
                val audioManager = context.getSystemService(Application.AUDIO_SERVICE) as AudioManager
                spatializer = audioManager.spatializer
                isSpatializerAvailable = spatializer != null && spatializer!!.isEnabled
                Log.d("Spatializer", "Spatializer available: $isSpatializerAvailable")
            } catch (e: Exception) {
                Log.e("Spatializer", "Failed to initialize Spatializer: ${e.message}")
                isSpatializerAvailable = false
            }
        }

        observeAlbums()
        refreshData()
        startTimer()
    }

    private fun observeAlbums() {
        viewModelScope.launch {
            albumDao.getAllAlbums().collect { albumEntityList ->
                albums.clear()
                albums.addAll(albumEntityList.map { it.toAlbum() })
            }
        }
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
                    // 检测歌词语言，判断是否可以翻译
                    val lyricTexts = parsed.map { it.content }
                    val isChinese = com.music.purelymusic.utils.LanguageDetector.isLyricsChinese(lyricTexts)
                    canTranslate = !isChinese
                    Log.d("LyricLoad", "语言检测结果: isChinese=$isChinese, canTranslate=$canTranslate")
                    // 重置翻译状态
                    showTranslation = false
                    translateError = null
                }
            } catch (e: Exception) {
                Log.e("LyricLoad", "Failed: ${e.message}", e)
                withContext(Dispatchers.Main) { lyricLines = emptyList() }
            }
        }
    }

    // --- 播放控制逻辑 ---
    private fun buildMediaItem(musicPath: String, song: Song? = currentSong): MediaItem {
        val uri = if (musicPath.startsWith("content://") || musicPath.startsWith("file://")) {
            Uri.parse(musicPath)
        } else {
            Uri.fromFile(File(musicPath))
        }
        val mimeType = resolveMimeType(musicPath)
        val metadata = MediaMetadata.Builder()
            .setTitle(song?.title)
            .setArtist(song?.artist)
            .setAlbumTitle(song?.album)
            .apply {
                song?.coverUri
                    ?.takeIf { File(it).exists() }
                    ?.let { setArtworkUri(Uri.fromFile(File(it))) }
            }
            .build()
        return MediaItem.Builder()
            .setMediaId(song?.id?.toString() ?: musicPath)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .apply {
                if (!mimeType.isNullOrBlank()) {
                    setMimeType(mimeType)
                }
            }
            .build()
    }

    private fun buildQueueMediaItems(): List<MediaItem> = currentPlayingList.mapNotNull { queuedSong ->
        queuedSong.musicUri?.let { buildMediaItem(it, queuedSong) }
    }

    private fun configurePlayMode(player: ExoPlayer) {
        player.repeatMode = when (playMode) {
            PlayMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
            PlayMode.SEQUENTIAL, PlayMode.SHUFFLE -> Player.REPEAT_MODE_ALL
        }
        player.shuffleModeEnabled = playMode == PlayMode.SHUFFLE
    }

    private fun syncPlayerQueueKeepingPosition() {
        val player = exoPlayer ?: return
        val items = buildQueueMediaItems()
        if (items.isEmpty()) return
        val currentId = currentSong?.id?.toString()
        val index = items.indexOfFirst { it.mediaId == currentId }.coerceAtLeast(0)
        val position = player.currentPosition.coerceAtLeast(0L)
        val shouldPlay = player.playWhenReady
        player.setMediaItems(items, index, position)
        configurePlayMode(player)
        player.prepare()
        player.playWhenReady = shouldPlay
    }

    private fun resetCrossfadeState() {
        crossfadeJob?.cancel()
        crossfadeJob = null
        isCrossfadeInProgress = false
        hasScheduledCrossfadeForCurrentSong = false
        suppressNextEndedCallback = false
        transitionPlayer?.release()
        transitionPlayer = null
        exoPlayer?.volume = 1.0f
    }

    private fun getCurrentSongIndex(): Int {
        return currentPlayingList.indexOfFirst { it.id == currentSong?.id }
    }

    private fun getNextSongForPlayback(): Song? {
        if (currentPlayingList.isEmpty()) return null
        if (playMode == PlayMode.REPEAT_ONE) return currentSong
        if (playMode == PlayMode.SHUFFLE) {
            val currentIdx = getCurrentSongIndex()
            if (currentIdx == -1) return currentPlayingList.random()
            val candidates = currentPlayingList.filterIndexed { i, _ -> i != currentIdx }
            return if (candidates.isEmpty()) currentSong else candidates.random()
        }
        val idx = getCurrentSongIndex()
        if (idx == -1) return null
        val nextIdx = (idx + 1) % currentPlayingList.size
        return currentPlayingList.getOrNull(nextIdx)
    }

    private fun beginAutoCrossfadeIfNeeded() {
        val player = exoPlayer ?: return
        if (!crossfadeEnabled || isCrossfadeInProgress || hasScheduledCrossfadeForCurrentSong) return
        if (!player.isPlaying) return

        val currentDuration = player.duration
        val currentPositionMs = player.currentPosition
        if (currentDuration <= crossfadeDurationMs || currentDuration <= 0L) return

        val nextSong = getNextSongForPlayback() ?: return
        if (nextSong.musicUri.isNullOrBlank()) return

        val remaining = currentDuration - currentPositionMs
        if (remaining > crossfadeDurationMs) return

        hasScheduledCrossfadeForCurrentSong = true
        startAutoCrossfade(nextSong)
    }

    private fun startAutoCrossfade(nextSong: Song) {
        val outgoingPlayer = exoPlayer ?: return
        val musicPath = nextSong.musicUri ?: return

        crossfadeJob?.cancel()
        crossfadeJob = viewModelScope.launch {
            try {
                val incomingPlayer = createPlayer().also { transitionPlayer = it }
                val queueItems = buildQueueMediaItems()
                val nextIndex = queueItems.indexOfFirst { it.mediaId == nextSong.id.toString() }
                if (queueItems.isNotEmpty() && nextIndex >= 0) {
                    incomingPlayer.setMediaItems(queueItems, nextIndex, 0L)
                } else {
                    incomingPlayer.setMediaItem(buildMediaItem(musicPath, nextSong))
                }
                configurePlayMode(incomingPlayer)
                incomingPlayer.prepare()
                incomingPlayer.volume = 0f
                incomingPlayer.play()

                isCrossfadeInProgress = true
                suppressNextEndedCallback = true
                this@PlayerViewModel.isPlaying = true
                updateSongState(nextSong)
                duration = incomingPlayer.duration.takeIf { it > 0L } ?: duration

                val steps = 20
                val stepDelay = (crossfadeDurationMs / steps).coerceAtLeast(50L)
                repeat(steps) { index ->
                    val progress = (index + 1) / steps.toFloat()
                    setPlayerVolume(outgoingPlayer, 1f - progress, 1f - progress)
                    setPlayerVolume(incomingPlayer, progress, progress)
                    delay(stepDelay)
                }

                outgoingPlayer.removeListener(playerListener)
                outgoingPlayer.stop()
                outgoingPlayer.release()

                playbackRuntime.replacePlayer(incomingPlayer)
                transitionPlayer = null
                isCrossfadeInProgress = false
                hasScheduledCrossfadeForCurrentSong = false
                suppressNextEndedCallback = false
                currentPosition = incomingPlayer.currentPosition
                duration = incomingPlayer.duration.takeIf { it > 0L } ?: duration
                this@PlayerViewModel.isPlaying = incomingPlayer.isPlaying
                startSurroundEffect()
            } catch (e: Exception) {
                Log.e("Crossfade", "自动交叉渐入渐出失败: ${e.message}", e)
                resetCrossfadeState()
            }
        }
    }

    private fun updateSongState(song: Song) {
        val playedAt = System.currentTimeMillis()
        currentSong = song.copy(
            lastPlayedTime = playedAt,
            playCount = song.playCount + 1
        )
        hasScheduledCrossfadeForCurrentSong = false

        if (!song.lrcPath.isNullOrEmpty()) {
            loadLyrics(song.lrcPath)
        } else {
            lyricLines = emptyList()
        }

        updateBlurBackground(song.coverUri)

        viewModelScope.launch {
            songDao.recordPlayback(song.id, playedAt)
            val recent = songDao.getRecentSongs().map { it.toSong() }
            recentSongs.clear()
            recentSongs.addAll(recent)
        }
    }

    private fun playNextInternal(allowAutoCrossfade: Boolean) {
        if (currentPlayingList.isEmpty()) return

        if (playMode == PlayMode.REPEAT_ONE && currentSong != null) {
            playSong(currentSong!!, false)
            return
        }

        val nextSong = getNextSongForPlayback() ?: return
        if (allowAutoCrossfade && crossfadeEnabled) {
            startAutoCrossfade(nextSong)
            return
        }
        playSong(nextSong, false)
    }

    fun playSong(song: Song, updateInternalList: Boolean = true) {
        resetCrossfadeState()
        if (updateInternalList) {
            currentPlayingList.clear()
            currentPlayingList.addAll(libraryList)
        }

        if (currentSong?.id == song.id && exoPlayer != null) {
            togglePlayPause()
            return
        }

        exoPlayer?.stop()
        updateSongState(song)

        try {
            val musicPath = song.musicUri ?: return
            ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java))
            val player = getOrCreatePlayer()
            val queueItems = buildQueueMediaItems()
            val selectedIndex = queueItems.indexOfFirst { it.mediaId == song.id.toString() }
            if (queueItems.isNotEmpty() && selectedIndex >= 0) {
                player.setMediaItems(queueItems, selectedIndex, 0L)
            } else {
                player.setMediaItem(buildMediaItem(musicPath, song))
            }
            configurePlayMode(player)
            player.prepare()
            player.volume = 1.0f
            player.play()
        } catch (e: Exception) {
            Log.e("PlayError", "播放失败: ${e.message}, 歌曲路径=${song.musicUri}")
        }
    }
    fun removeSongFromPlayingList(song: Song) {
        if (currentPlayingList.isEmpty()) return
        val index = currentPlayingList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            currentPlayingList.removeAt(index)
            // 如果删除的是当前播放的歌曲，播放下一首
            if (currentSong?.id == song.id && currentPlayingList.isNotEmpty()) {
                playSong(currentPlayingList[0], false)
            } else {
                syncPlayerQueueKeepingPosition()
            }
        }
    }

    fun clearPlayingList() {
        val playing = currentSong
        currentPlayingList.clear()
        if (playing != null) currentPlayingList.add(playing)
        syncPlayerQueueKeepingPosition()
    }

    // 跳转到指定歌曲（不删除前面的播放历史）
    fun jumpToSong(song: Song) {
        resetCrossfadeState()
        if (currentPlayingList.isEmpty()) return
        val index = currentPlayingList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            // 直接播放指定歌曲，不修改播放列表
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

    fun deleteAlbum(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            database.withTransaction {
                songDao.clearAlbum(album.id)
                albumDao.deleteAlbum(album.toEntity())
            }
            deleteFileIfUnreferenced(album.coverUri)
            refreshData()
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                stopSurroundEffect()
            } else {
                it.play()
                startSurroundEffect()
            }
            syncPlaybackState()
        }
    }

    fun playNext() {
        resetCrossfadeState()
        playNextInternal(allowAutoCrossfade = false)
    }

    fun playPrevious() {
        resetCrossfadeState()
        if (currentPlayingList.isEmpty()) return
        val idx = currentPlayingList.indexOfFirst { it.id == currentSong?.id }
        if (idx != -1) {
            val prevIdx = if (idx <= 0) currentPlayingList.size - 1 else idx - 1
            playSong(currentPlayingList[prevIdx], false)
        }
    }

    fun seekTo(pos: Float) {
        resetCrossfadeState()
        exoPlayer?.seekTo(pos.toLong())
        currentPosition = pos.toLong()
        // 确保歌词索引立即更新，拖动进度条时自动导航到对应歌词
        // currentLyricIndex 使用 derivedStateOf 会自动根据 currentPosition 重新计算
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (isActive) {
                if (isPlaying) {
                    currentPosition = exoPlayer?.currentPosition ?: 0L
                    beginAutoCrossfadeIfNeeded()
                    syncPlaybackState()
                }
                delay(1000)
            }
        }
    }

// --- 环绕音效 ---
    private var surroundJob: Job? = null
    private var surroundAngle = 0f
    private var sourcePhase = 0f

    // 延迟缓冲区（用于实现ITD和空间混响）
    private val leftDelayBuffer = ArrayDeque<Float>(50).apply { repeat(50) { add(0.5f) } }
    private val rightDelayBuffer = ArrayDeque<Float>(50).apply { repeat(50) { add(0.5f) } }

    private fun startSurroundEffect() {
        stopSurroundEffect()

        // 在 Android 12+ 上检查 Spatializer 状态 (仅对沉浸立体音有效)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 && isSpatializerAvailable && surroundMode == SurroundMode.IMMERSIVE) {
            try {
                val isSpatializerEnabled = spatializer?.isEnabled ?: false
                Log.d("SurroundEffect", "Spatializer available and enabled: $isSpatializerEnabled")
            } catch (e: Exception) {
                Log.e("SurroundEffect", "Failed to check Spatializer: ${e.message}")
            }
        }

        surroundJob = viewModelScope.launch {
            while (isActive && isPlaying) {
                if (isSurroundEnabled) {
                    when (surroundMode) {
                        SurroundMode.IMMERSIVE -> applyImmersiveSurround()
                        SurroundMode.THREE_D -> apply3DSurround()
                        SurroundMode.NONE -> {
                            setPlayerVolume(1.0f, 1.0f)
                            delay(100)
                        }
                    }
                } else {
                    setPlayerVolume(1.0f, 1.0f)
                    delay(100)
                }
            }
        }
    }

private fun stopSurroundEffect() {
        surroundJob?.cancel()
        surroundJob = null

        // 恢复平衡音量
        setPlayerVolume(1.0f, 1.0f)
        // 重置缓冲区
        leftDelayBuffer.clear()
        repeat(50) { leftDelayBuffer.add(0.5f) }
        rightDelayBuffer.clear()
        repeat(50) { rightDelayBuffer.add(0.5f) }
        sourcePhase = 0f
    }

    // 沉浸立体音效（模拟5.1声道环绕）
    private suspend fun applyImmersiveSurround() {
        // 使用动态变化的声源，营造四面八方传来的效果

        // 更新相位，让声源增益动态变化
        sourcePhase = (sourcePhase + 0.08f) % (2 * Math.PI.toFloat())

        // === 模拟5.1声道的动态增益 ===
        // 前置左声道 (FL)：动态变化
        val frontLeft = 0.6f + 0.15f * Math.sin(sourcePhase.toDouble()).toFloat()

        // 前置右声道 (FR)：动态变化，与FL有相位差
        val frontRight = 0.6f + 0.15f * Math.cos(sourcePhase.toDouble()).toFloat()

        // 中置声道 (C)：稳定增益
        val center = 0.4f

        // 后置左环绕 (SL)：较慢的动态变化，模拟从背后传来
        val surroundLeft = 0.35f + 0.2f * Math.sin(sourcePhase.toDouble() + Math.PI / 4).toFloat()

        // 后置右环绕 (SR)：较慢的动态变化，与SL有相位差
        val surroundRight = 0.35f + 0.2f * Math.cos(sourcePhase.toDouble() + Math.PI / 4).toFloat()

        // 低音炮 (LFE)：非常缓慢的脉冲效果
        val lfePulse = (Math.sin(sourcePhase * 0.5) + 1.0).toFloat() * 0.3f

        // === 添加到延迟缓冲区（每次都更新）===
        val currentLeft = frontLeft + center * 0.5f + surroundLeft * 0.3f
        val currentRight = frontRight + center * 0.5f + surroundRight * 0.3f

        leftDelayBuffer.addLast(currentLeft)
        leftDelayBuffer.removeFirst()
        rightDelayBuffer.addLast(currentRight)
        rightDelayBuffer.removeFirst()

        // === 混合5.1声道到立体声输出 ===
        // 左声道混合：FL(强) + C(中) + SL(中) + LFE(弱) + 交叉串扰
        var leftMix = frontLeft * 1.0f           // 前置左，主要
        leftMix += center * 0.5f                 // 中置
        leftMix += leftDelayBuffer[15] * 0.6f   // 后置左环绕（延迟）
        leftMix += lfePulse * 0.2f               // 低音炮

        // 右声道混合：FR(强) + C(中) + SR(中) + LFE(弱) + 交叉串扰
        var rightMix = frontRight * 1.0f          // 前置右，主要
        rightMix += center * 0.5f                 // 中置
        rightMix += rightDelayBuffer[15] * 0.6f  // 后置右环绕（延迟）
        rightMix += lfePulse * 0.2f               // 低音炮

        // === 交叉串扰（模拟空间扩散）===
        // 前置右串扰到左耳
        leftMix += rightDelayBuffer[5] * 0.25f
        // 前置左串扰到右耳
        rightMix += leftDelayBuffer[5] * 0.25f
        // 后置环绕交叉串扰
        leftMix += rightDelayBuffer[20] * 0.15f
        rightMix += leftDelayBuffer[20] * 0.15f

        // === 空间混响（增强空间感）===
        // 早期反射
        val earlyL = (leftDelayBuffer[3] + leftDelayBuffer[8]) / 2f * 0.4f
        val earlyR = (rightDelayBuffer[3] + rightDelayBuffer[8]) / 2f * 0.4f

        // 中期反射
        val midL = (leftDelayBuffer[12] + leftDelayBuffer[18] + leftDelayBuffer[25]) / 3f * 0.3f
        val midR = (rightDelayBuffer[12] + rightDelayBuffer[18] + rightDelayBuffer[25]) / 3f * 0.3f

        // 晚期反射
        val lateL = leftDelayBuffer[35] * 0.15f
        val lateR = rightDelayBuffer[35] * 0.15f

        leftMix += earlyL + midL + lateL
        rightMix += earlyR + midR + lateR

        // === 动态范围控制 ===
        // 确保左右声道有明显差异
        leftMix = leftMix.coerceIn(0.35f, 1.6f)
        rightMix = rightMix.coerceIn(0.35f, 1.6f)

        // 应用到MediaPlayer
        setPlayerVolume(leftMix, rightMix)

        delay(20) // ~50fps
    }

    // 3D环绕音效（圆周运动）
    private suspend fun apply3DSurround() {
        // 参考HMS的动态渲染模式：ROTATION
        surroundAngle = (surroundAngle + surroundSpeed) % 360f
        val angleRad = Math.toRadians(surroundAngle.toDouble())

        // 声源位置：在水平面上做圆周运动（增大运动范围）
        val sourceRadius = (surroundRadius / 100f) * 3.0f
        val sourceX = Math.sin(angleRad).toFloat() * sourceRadius
        val sourceZ = Math.cos(angleRad).toFloat() * sourceRadius

        // 双耳时间差 (ITD)
        val leftEarDist = kotlin.math.sqrt(
            (sourceX - 0.085) * (sourceX - 0.085) + sourceZ * sourceZ
        )
        val rightEarDist = kotlin.math.sqrt(
            (sourceX + 0.085) * (sourceX + 0.085) + sourceZ * sourceZ
        )
        val itdMs = kotlin.math.abs(leftEarDist - rightEarDist) / 343f * 1000
        val itdSamples = (itdMs * 60 / 1000).toInt().coerceIn(0, 49)

        // 双耳强度差 (ILD)（增大衰减系数，增强距离感）
        val leftAttenuation = 1.0f / (1.0f + leftEarDist * 0.5f)
        val rightAttenuation = 1.0f / (1.0f + rightEarDist * 0.5f)

        var leftVol: Double = leftAttenuation
        var rightVol: Double = rightAttenuation

        // 前后方位感（增强前后差异）
        val azimuth = Math.atan2(sourceX.toDouble(), sourceZ.toDouble())
        val frontBackFactor = (Math.cos(azimuth) + 1.0) / 2.0
        val frontBackAttenuation = 0.2f + 0.8f * frontBackFactor.toFloat()

        if (sourceZ < 0) {
            leftVol *= frontBackAttenuation
            rightVol *= frontBackAttenuation
        }

        // 头部阴影效应（增强阴影效果）
        val shadowFactor = (1.0 + Math.cos(azimuth)) / 2.0
        val shadowAttenuation = 0.1f + 0.9f * shadowFactor.toFloat()

        if (sourceX > 0) {
            leftVol *= shadowAttenuation
        } else {
            rightVol *= shadowAttenuation
        }

        // 相位差（增大相位偏移）
        val phaseShift = if (sourceZ < 0) 0.35f else 0.15f
        if (sourceX > 0) {
            leftVol *= (1.0f - phaseShift)
            rightVol *= (1.0f + phaseShift)
        } else {
            leftVol *= (1.0f + phaseShift)
            rightVol *= (1.0f - phaseShift)
        }

        // 应用延迟缓冲
        leftDelayBuffer.addLast(leftVol.toFloat())
        leftDelayBuffer.removeFirst()
        rightDelayBuffer.addLast(rightVol.toFloat())
        rightDelayBuffer.removeFirst()

        // 从延迟缓冲区读取
        var leftVolume = leftDelayBuffer[itdSamples]
        var rightVolume = rightDelayBuffer[itdSamples]

        // 空间混响（增强混响效果）
        val earlyL = (leftDelayBuffer[2] + leftDelayBuffer[5] + leftDelayBuffer[8]) / 3f * 0.8f
        val earlyR = (rightDelayBuffer[2] + rightDelayBuffer[5] + rightDelayBuffer[8]) / 3f * 0.8f
        val midL = (leftDelayBuffer[12] + leftDelayBuffer[18] + leftDelayBuffer[25]) / 3f * 0.6f
        val midR = (rightDelayBuffer[12] + rightDelayBuffer[18] + rightDelayBuffer[25]) / 3f * 0.6f

        leftVolume += earlyL + midL
        rightVolume += earlyR + midR

        // 动态范围控制（扩大变化范围）
        leftVolume = leftVolume.coerceIn(0.2f, 1.8f)
        rightVolume = rightVolume.coerceIn(0.2f, 1.8f)

        // 应用到MediaPlayer
        setPlayerVolume(leftVolume, rightVolume)

        delay(20)
    }

    private fun stop3DSurroundEffect() {
        surroundJob?.cancel()
        surroundJob = null
        setPlayerVolume(1.0f, 1.0f)
        // 重置缓冲区
        leftDelayBuffer.clear()
        repeat(50) { leftDelayBuffer.add(0.5f) }
        rightDelayBuffer.clear()
        repeat(50) { rightDelayBuffer.add(0.5f) }
        sourcePhase = 0f
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

                val activePlayer = exoPlayer
                val activeQueue = activePlayer?.let { player ->
                    (0 until player.mediaItemCount).mapNotNull { index ->
                        val songId = player.getMediaItemAt(index).mediaId.toLongOrNull()
                        convertedSongs.firstOrNull { it.id == songId }
                    }
                }.orEmpty()
                if (activeQueue.isNotEmpty() && activePlayer != null) {
                    currentPlayingList.clear()
                    currentPlayingList.addAll(activeQueue)
                    val activeId = activePlayer.currentMediaItem?.mediaId?.toLongOrNull()
                    convertedSongs.firstOrNull { it.id == activeId }?.let { activeSong ->
                        currentSong = activeSong
                        activeSong.lrcPath?.let(::loadLyrics)
                        updateBlurBackground(activeSong.coverUri)
                        currentPosition = activePlayer.currentPosition
                        duration = activePlayer.duration.coerceAtLeast(0L)
                        syncPlaybackState()
                    }
                }

                val recentFromDb = songDao.getRecentSongs().map { it.toSong() }
                val playlistEntities = playlistDao.getAllPlaylists()

                withContext(Dispatchers.Main) {
                    recentSongs.clear()
                    recentSongs.addAll(recentFromDb)
                    playlists.clear()
                    playlists.addAll(playlistEntities.map { it.toPlaylist() })
                }

                //: 收藏列表必须放在 collect 前（collect 永不返回）
                refreshFavorites()
                if (currentPlayingList.isEmpty()) {
                    currentPlayingList.addAll(libraryList)
                }

            } catch (e: Exception) {
                android.util.Log.e("refreshData", "Failed to refresh data", e)
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            songDao.deleteSong(song.toEntity())
            listOf(song.musicUri, song.coverUri, song.lrcPath)
                .distinct()
                .forEach { deleteFileIfUnreferenced(it) }
            refreshData()
            if (currentSong?.id == song.id) {
                withContext(Dispatchers.Main) {
                    exoPlayer?.stop()
                    currentSong = null
                    isPlaying = false
                }
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
            if (newCoverPath != song.coverUri) deleteFileIfUnreferenced(song.coverUri)
            if (newLrcPath != song.lrcPath) deleteFileIfUnreferenced(song.lrcPath)
            refreshData()

            // 如果正在播放这首歌，更新当前歌曲信息
            if (currentSong?.id == song.id) {
                currentSong = updatedSong
                updateCurrentMediaItemMetadata(updatedSong)
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

    //: 切换收藏状态
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val newFav = !song.isFavorite
            songDao.updateSongFavorite(song.id, if (newFav) 1 else 0)
            // 更新内存中的 libraryList
            libraryList = libraryList.map {
                if (it.id == song.id) it.copy(isFavorite = newFav) else it
            }
            // 如果收藏的是当前歌曲，也更新 currentSong
            if (currentSong?.id == song.id) {
                currentSong = currentSong?.copy(isFavorite = newFav)
            }
            // 如果切换前是收藏列表，刷新收藏
            refreshFavorites()
        }
    }

    //: 刷新收藏列表（suspend，避免嵌套协程）
    private suspend fun refreshFavorites() {
        val favEntities = songDao.getFavoriteSongs()
        favoriteSongs = favEntities.map { it.toSong() }
    }

    //: 搜索歌曲（带300ms防抖，支持仅搜收藏）
    fun performSearch(query: String, onlyFavorites: Boolean = false) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300L)
            if (!isActive) return@launch
            val results = if (onlyFavorites) {
                songDao.searchFavoriteSongs(query)
            } else {
                songDao.searchSongs(query)
            }
            searchResults = results.map { it.toSong() }
        }
    }

    //: 睡眠定时器
    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        sleepTimerMinutes = minutes
        sleepTimerRemainingSeconds = minutes * 60
        sleepTimerActive = true
        sleepTimerJob = viewModelScope.launch {
            while (sleepTimerRemainingSeconds > 0) {
                delay(1000L)
                // 🚩 主动检查协程是否被取消，避免取消后仍执行暂停
                if (!isActive) break
                sleepTimerRemainingSeconds--
                if (sleepTimerRemainingSeconds <= 0) {
                    // 到时：暂停播放
                    if (isActuallyPlaying) {
                        togglePlayPause()
                    }
                    sleepTimerActive = false
                    sleepTimerMinutes = 0
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerActive = false
        sleepTimerMinutes = 0
        sleepTimerRemainingSeconds = 0
    }

    val sleepTimerDisplay: String
        get() {
            if (!sleepTimerActive) return ""
            val mins = sleepTimerRemainingSeconds / 60
            val secs = sleepTimerRemainingSeconds % 60
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
        }

    private fun updateCurrentMediaItemMetadata(song: Song) {
        val player = exoPlayer ?: return
        val musicPath = song.musicUri ?: return
        val index = player.currentMediaItemIndex
        if (index >= 0 && index < player.mediaItemCount) {
            player.replaceMediaItem(index, buildMediaItem(musicPath, song))
        }
    }

    private fun updateBlurBackground(path: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = if (path != null && File(path).exists()) {
                BitmapFactory.decodeFile(path)
            } else {
                // 加载默认封面
                BitmapFactory.decodeResource(context.resources, R.drawable.default_cover)
            }
            val blurred = bitmap?.let { BlurUtil.doBlur(it, 25, 20) }
            withContext(Dispatchers.Main) { blurredBackground = blurred }
        }
    }

    override fun onCleared() {
        super.onCleared()
        resetCrossfadeState()
        stop3DSurroundEffect()
        releaseEqualizer()
        exoPlayer?.removeListener(playerListener)
        playbackRuntime.removePlayerChangeListener(runtimePlayerChangeListener)
    }

    
    // --- 读取音频文件元数据 ---
    /**
     * 从音频文件中读取元数据（歌名和歌手名）
     * @return Pair(歌名, 歌手名)，如果读取失败则返回 Pair(null, null)
     */
    suspend fun readAudioMetadata(uri: Uri): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)

                retriever.release()
                Pair(title, artist)
            } catch (e: Exception) {
                Pair(null, null)
            }
        }
    }

    // --- 批量导入歌曲 ---
    /**
     * 开始批量导入歌曲
     * @param uris 音频文件URI列表
     */
    fun batchImportSongs(uris: List<Uri>) {
        if (uris.isEmpty()) return

        // 初始化队列
        batchImportQueue = uris.mapIndexed { index, uri ->
            BatchImportItem(uri = uri, index = index)
        }.toMutableList()

        isBatchImporting = true
        batchImportTotal = uris.size
        batchImportProgress = 0
        batchImportCurrentSong = null
        batchImportImported = 0
        batchImportSkipped = 0
        batchImportFailed = 0
        batchImportSummary = null
        batchImportPaused = false

        // 开始处理队列
        processBatchImportQueue()
    }

    /**
     * 处理批量导入队列
     */
    private fun processBatchImportQueue() {
        viewModelScope.launch {
            while (batchImportQueue.isNotEmpty() && !batchImportPaused) {
                val item = batchImportQueue.removeAt(0)
                batchImportProgress = batchImportTotal - batchImportQueue.size

                try {
                    // 获取文件名用于显示
                    val fileName = getDisplayName(item.uri)
                        ?: item.uri.path?.substringAfterLast('/')
                        ?: "歌曲 ${item.index + 1}"

                    // 如果关闭了自动获取元数据，直接暂停让用户输入
                    if (!_autoFetchMetadata) {
                        batchImportCurrentSong = fileName
                        batchImportPaused = true
                        batchImportPendingUri = item.uri
                        batchImportPendingFileName = fileName
                        // 将项目放回队列开头
                        batchImportQueue.add(0, item)
                        return@launch
                    }

                    // 读取元数据
                    val (title, artist) = readAudioMetadata(item.uri)

                    // 如果元数据不完整，暂停并等待用户输入
                    if (title.isNullOrBlank() || artist.isNullOrBlank()) {
                        batchImportCurrentSong = fileName
                        batchImportPaused = true
                        batchImportPendingUri = item.uri
                        batchImportPendingFileName = fileName
                        // 将项目放回队列开头
                        batchImportQueue.add(0, item)
                        return@launch
                    }

                    // 显示当前正在处理的歌曲
                    batchImportCurrentSong = title

                    if (songDao.findByTitleAndArtist(title, artist) != null) {
                        batchImportSkipped++
                        continue
                    }

                    // 复制音乐文件到私有目录
                    val pMusic = copyFile(item.uri, "mus_${System.currentTimeMillis()}_${item.index}")

                    if (pMusic != null) {
                        if (songDao.findByMusicUri(pMusic) != null) {
                            batchImportSkipped++
                        } else {
                            processBatchImportSong(title, artist, pMusic, item.index)
                            batchImportImported++
                        }
                    } else {
                        Log.e("BatchImport", "复制音乐文件失败: ${item.uri}")
                        batchImportFailed++
                    }
                } catch (e: Exception) {
                    Log.e("BatchImport", "导入歌曲失败: ${e.message}", e)
                    batchImportFailed++
                }
            }

            // 队列处理完成
            if (batchImportQueue.isEmpty()) {
                refreshData()
                isBatchImporting = false
                batchImportCurrentSong = null
                batchImportPaused = false
                batchImportSummary = "导入完成：成功 $batchImportImported 首，跳过 $batchImportSkipped 首，失败 $batchImportFailed 首"
            }
        }
    }

    /**
     * 处理单首歌曲的导入（带元数据）
     */
    private suspend fun processBatchImportSong(title: String, artist: String, musicPath: String, index: Int) {
        var pCover: String? = null
        var pLrc: String? = null
        var albumName: String? = null
        var albumArtist: String? = null

        // 如果开启了自动获取元数据，则获取封面和歌词
        if (_autoFetchMetadata) {
            // 每首歌间隔1秒，避免API速率限制
            if (index > 0) {
                delay(1000)
            }

            try {
                val (coverPath, lrcPath) = fetchAllFromNetwork(title, artist)
                pCover = coverPath
                pLrc = lrcPath
                albumName = tempAlbumName
                albumArtist = tempAlbumArtist
            } catch (e: Exception) {
                Log.e("BatchImport", "获取歌曲 $title 的封面和歌词失败: ${e.message}")
            }
        }

        try {
            database.withTransaction {
                val albumId = findOrCreateAlbumId(albumName, albumArtist, artist, pCover)
                val newSong = Song(
                    id = 0,
                    title = title,
                    artist = artist,
                    coverUri = pCover,
                    musicUri = musicPath,
                    lrcPath = pLrc,
                    album = albumName,
                    albumId = albumId
                )
                songDao.insertSong(newSong.toEntity())
            }
            Log.d("BatchImport", "成功导入歌曲: $title")
        } catch (error: Exception) {
            listOf(musicPath, pCover, pLrc).forEach(fileStore::deleteOwnedFile)
            throw error
        }
    }

    /**
     * 用户输入歌曲信息后继续批量导入
     * 注意：用户手动输入后，总是尝试从网络获取封面和歌词（不受 autoFetchMetadata 设置影响）
     */
    fun continueBatchImport(title: String, artist: String) {
        val musicPath = batchImportPendingMusicPath
        val uri = batchImportPendingUri
        val index = batchImportTotal - batchImportQueue.size - 1

        viewModelScope.launch {
            if (songDao.findByTitleAndArtist(title, artist) != null) {
                batchImportSkipped++
                batchImportPaused = false
                batchImportPendingUri = null
                batchImportPendingFileName = null
                batchImportPendingMusicPath = null
                if (batchImportQueue.isNotEmpty()) batchImportQueue.removeAt(0)
                processBatchImportQueue()
                return@launch
            }

            var pMusic = musicPath
            
            // 如果没有音乐文件路径，需要先复制文件
            if (pMusic == null && uri != null) {
                pMusic = copyFile(uri, "mus_${System.currentTimeMillis()}")
            }

            if (pMusic != null) {
                var pCover: String? = null
                var pLrc: String? = null
                var albumName: String? = null
                var albumArtist: String? = null

                // 用户手动输入后，总是尝试从网络获取封面和歌词
                try {
                    val (coverPath, lrcPath) = fetchAllFromNetwork(title, artist)
                    pCover = coverPath
                    pLrc = lrcPath
                    albumName = tempAlbumName
                    albumArtist = tempAlbumArtist
                } catch (e: Exception) {
                    Log.e("BatchImport", "获取歌曲 $title 的封面和歌词失败: ${e.message}")
                }

                try {
                    database.withTransaction {
                        val albumId = findOrCreateAlbumId(albumName, albumArtist, artist, pCover)
                        val newSong = Song(
                            id = 0,
                            title = title,
                            artist = artist,
                            coverUri = pCover,
                            musicUri = pMusic,
                            lrcPath = pLrc,
                            album = albumName,
                            albumId = albumId
                        )
                        songDao.insertSong(newSong.toEntity())
                    }
                    batchImportImported++
                    Log.d("BatchImport", "成功导入歌曲: $title")
                } catch (error: Exception) {
                    listOf(pMusic, pCover, pLrc).forEach(fileStore::deleteOwnedFile)
                    batchImportFailed++
                    Log.e("BatchImport", "保存歌曲失败", error)
                }
            } else {
                batchImportFailed++
            }

            // 重置暂停状态
            batchImportPaused = false
            batchImportPendingUri = null
            batchImportPendingFileName = null
            batchImportPendingMusicPath = null

            // 从队列中移除已处理的项目
            if (batchImportQueue.isNotEmpty()) {
                batchImportQueue.removeAt(0)
            }

            // 继续处理队列
            processBatchImportQueue()
        }
    }

    /**
     * 跳过当前歌曲继续批量导入
     */
    fun skipBatchImport() {
        batchImportPaused = false
        batchImportPendingUri = null
        batchImportPendingFileName = null
        batchImportPendingMusicPath = null

        // 从队列中移除跳过的项目
        if (batchImportQueue.isNotEmpty()) {
            batchImportQueue.removeAt(0)
        }

        // 继续处理队列
        processBatchImportQueue()
    }

    /**
     * 取消批量导入
     */
    fun cancelBatchImport() {
        batchImportQueue.clear()
        isBatchImporting = false
        batchImportPaused = false
        batchImportCurrentSong = null
        batchImportPendingUri = null
        batchImportPendingFileName = null
        batchImportPendingMusicPath = null
    }

    fun clearBatchImportSummary() {
        batchImportSummary = null
    }

    // --- 自动获取所有信息（封面+歌词）---
    suspend fun fetchAllFromNetwork(title: String, artist: String): Pair<String?, String?> {
        isFetchingAll = true
        fetchAllError = null
        return try {
            val result = metadataRepository.fetch(title, artist, _autoFetchSource)
            tempAlbumName = result.albumName
            tempAlbumArtist = result.albumArtist
            fetchAllError = result.error
            result.coverPath to result.lyricPath
        } catch (error: Exception) {
            val message = "获取歌曲信息失败：${error.message ?: error.javaClass.simpleName}"
            Log.e("FetchAll", message, error)
            fetchAllError = message
            null to null
        } finally {
            isFetchingAll = false
        }
    }

    // --- 翻译功能 ---
    /**
     * 翻译当前歌词
     */
    fun translateLyrics() {
        viewModelScope.launch {
            try {
                isTranslating = true
                translateError = null
                translateLogs = ""

                if (lyricLines.isEmpty()) {
                    translateError = "没有歌词可翻译"
                    addLog("错误: 没有歌词可翻译")
                    return@launch
                }

                addLog("========== 开始翻译 ==========")
                addLog("原始歌词行数: ${lyricLines.size}")
                Log.d("Translate", "========== 开始翻译 ==========")
                Log.d("Translate", "原始歌词行数: ${lyricLines.size}")

                // 构建歌词文本：每行包含时间戳和内容
                val lyricText = lyricLines.joinToString("\n") { line ->
                    val timeStr = LyricTranslationParser.formatTime(line.time)
                    "[$timeStr]${line.content}"
                }

                addLog("发送翻译请求，歌词长度: ${lyricText.length}")
                Log.d("Translate", "发送翻译请求，歌词长度: ${lyricText.length}")

                // 调用翻译API，目标语言为中文
                val response = translateService.translateText(
                    apiKey = BuildConfig.MUSIC_API_KEY,
                    text = lyricText,
                    fromLang = "auto",
                    targetLang = "zh"
                )

                addLog("API调用成功")
                addLog("========== 翻译响应开始 ==========")
                addLog("响应代码: ${response.code}")
                addLog("响应消息: ${response.msg}")
                Log.d("Translate", "API调用成功")
                Log.d("Translate", "========== 翻译响应开始 ==========")
                Log.d("Translate", "响应代码: ${response.code}")
                Log.d("Translate", "响应消息: ${response.msg}")

                // 检查响应是否成功
                if (response.code != 200 || response.data == null) {
                    translateError = "翻译服务返回错误: ${response.msg} (code: ${response.code})"
                    addLog("❌ 翻译失败: $translateError")
                    Log.e("Translate", "翻译失败: $translateError")
                    return@launch
                }

                // 获取翻译结果
                val translatedText = response.data!!.data?.jieguo
                if (translatedText == null) {
                    translateError = "翻译服务返回格式错误，未找到翻译结果"
                    addLog("❌ 翻译结果为空")
                    Log.e("Translate", "翻译结果为空")
                    return@launch
                }

                // 解码HTML实体
                val decodedText = LyricTranslationParser.decodeHtmlEntities(translatedText)

                addLog("翻译响应: translatedText长度=${decodedText.length}")
                Log.d("Translate", "翻译响应: translatedText长度=${decodedText.length}")

                // 检查翻译文本是否为空
                if (translatedText.isBlank()) {
                    translateError = "翻译服务返回空内容，请稍后重试"
                    addLog("❌ 翻译文本为空")
                    Log.e("Translate", "翻译文本为空")
                    return@launch
                }

                // 解析翻译后的文本
                val translatedLines = LyricTranslationParser.parse(decodedText, lyricLines)

                addLog("解析结果：共${translatedLines.size}行")
                addLog("成功匹配 ${translatedLines.count { !it.isNullOrBlank() }} 行")
                addLog("========== 翻译响应结束 ==========")
                Log.d("Translate", "解析结果：共${translatedLines.size}行")
                Log.d("Translate", "========== 翻译响应结束 ==========")

                // 检查是否有翻译结果
                val hasTranslation = translatedLines.any { !it.isNullOrEmpty() }
                if (!hasTranslation) {
                    translateError = "未能解析出翻译内容，API返回格式可能已改变"
                    addLog("❌ 未能解析出翻译内容")
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    // 更新歌词行，添加翻译
                    val newLyricLines = lyricLines.mapIndexed { index, line ->
                        line.copy(translation = translatedLines.getOrElse(index) { null })
                    }
                    lyricLines = newLyricLines
                    showTranslation = true
                    addLog("✅ 翻译完成！showTranslation=$showTranslation")
                    Log.d("Translate", "翻译完成，showTranslation=$showTranslation")
                }

            } catch (e: retrofit2.HttpException) {
                val errorMsg = "网络请求失败: ${e.code()} - ${e.message()}"
                addLog("❌ $errorMsg")
                Log.e("Translate", errorMsg, e)
                translateError = errorMsg
            } catch (e: java.net.SocketTimeoutException) {
                val errorMsg = "请求超时，请检查网络连接"
                addLog("❌ $errorMsg")
                Log.e("Translate", errorMsg, e)
                translateError = errorMsg
            } catch (e: java.net.UnknownHostException) {
                val errorMsg = "无法连接到翻译服务器，请检查网络"
                addLog("❌ $errorMsg")
                Log.e("Translate", errorMsg, e)
                translateError = errorMsg
            } catch (e: Exception) {
                val errorMsg = "翻译失败: ${e.javaClass.simpleName} - ${e.message}"
                addLog("❌ $errorMsg")
                Log.e("Translate", errorMsg, e)
                translateError = errorMsg
            } finally {
                isTranslating = false
            }
        }
    }

    /** 追加不包含歌词正文或 API 密钥的诊断日志。 */
    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        translateLogs += "[$timestamp] $message\n"
    }

    /**
     * 切换翻译显示状态
     */
    fun toggleTranslation() {
        showTranslation = !showTranslation
    }

}
