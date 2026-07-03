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
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.Spatializer
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.AudioAttributes as ExoAudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.legacy.MediaMetadataCompat
import androidx.media3.session.legacy.MediaSessionCompat
import com.music.purelymusic.BuildConfig
import androidx.media3.session.legacy.PlaybackStateCompat
import com.music.purelymusic.data.AppDatabase
import com.music.purelymusic.data.toEntity
import com.music.purelymusic.data.toPlaylist
import com.music.purelymusic.data.toAlbum
import com.music.purelymusic.model.*
import com.music.purelymusic.utils.LrcParser
import com.music.purelymusic.ui.utils.BlurUtil
import retrofit2.Retrofit
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import kotlin.math.sin

@SuppressLint("RestrictedApi")
@UnstableApi
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
                // 拷贝文件到私有目录，防止系统清理或权限丢失（不指定扩展名，让 copyFile 自动检测）
                val pMusic = copyFile(mUri, "mus_${System.currentTimeMillis()}")
                
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
                    // 处理专辑逻辑
                    val albumName = tempAlbumName
                    val albumArtist = tempAlbumArtist

                    if (!albumName.isNullOrEmpty()) {
                        // 检查专辑是否已存在
                        val existingAlbum = albumDao.getAlbumByName(albumName)
                        if (existingAlbum == null) {
                            // 创建新专辑
                            val albumId = java.util.UUID.randomUUID().toString()
                            val newAlbum = Album(
                                id = albumId,
                                name = albumName,
                                artist = albumArtist ?: artist,
                                coverUri = pCover
                            )
                            android.util.Log.d("purelymusic", "准备创建新专辑: ${newAlbum.name}")
                            val albumEntity = newAlbum.toEntity()
                            albumDao.insertAlbum(albumEntity)
                            android.util.Log.d("purelymusic", "新专辑已创建: ${newAlbum.name}")
                        } else {
                            android.util.Log.d("purelymusic", "专辑已存在: ${albumName}")
                        }
                    }

                    val newSong = Song(
                        id = 0, // Room 会自动生成
                        title = title,
                        artist = artist,
                        coverUri = pCover,
                        musicUri = pMusic,
                        lrcPath = pLrc,
                        album = albumName
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
                        tempAlbumName = null
                        tempAlbumArtist = null
                        refreshData()
                    }
                } else {
                    val errorMsg = "复制音乐文件失败"
                    android.util.Log.e("purelymusic", errorMsg)
                    withContext(Dispatchers.Main) {
                        saveSongError = errorMsg
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "保存歌曲失败: ${e.message}"
                android.util.Log.e("purelymusic", errorMsg, e)
                e.printStackTrace()
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

    private fun copyFile(uri: Uri, fileName: String): String? {
        return try {
            val finalFileName = if (!fileName.contains(".")) {
                val ext = getFileExtension(uri)
                fileName + ext
            } else {
                fileName
            }
            val file = File(context.filesDir, finalFileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) { null }
    }

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
            playlistDao.insertPlaylist(newPlaylist.toEntity())
            playlists.add(0, newPlaylist)
            selectedSongsForPlaylist.clear()
            tempPlaylistCoverUri = null
        }
    }


    private val context = application.applicationContext
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val songDao = AppDatabase.getDatabase(application).songDao()
    private val playlistDao = AppDatabase.getDatabase(application).playlistDao()
    private val albumDao = AppDatabase.getDatabase(application).albumDao()
    private var exoPlayer: ExoPlayer? = null
    private var transitionPlayer: ExoPlayer? = null
    private var crossfadeJob: Job? = null
    private var isCrossfadeInProgress = false
    private var hasScheduledCrossfadeForCurrentSong = false
    private var suppressNextEndedCallback = false
    private val crossfadeDurationMs: Long
        get() = crossfadeDurationSeconds * 1000L
    private var audioFocusGranted = false
    private var audioBecomingNoisyReceiver: AudioBecomingNoisyReceiver? = null

    var isActuallyPlaying by mutableStateOf(false)
        private set
    @SuppressLint("RestrictedApi")
    private var mediaSession: MediaSessionCompat? = null
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            syncPlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncPlaybackState()
            if (playbackState == Player.STATE_READY) {
                this@PlayerViewModel.duration = exoPlayer?.duration ?: 0L
                currentSong?.let { updateMediaSession(it) }
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

        override fun onPlayerError(error: PlaybackException) {
            Log.e("PlayError", "ExoPlayer错误: ${error.errorCodeName}, ${error.message}")
            this@PlayerViewModel.isPlaying = false
            updateNotification(currentSong, false)
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing

        val player = createPlayer()
        player.addListener(playerListener)
        exoPlayer = player
        return player
    }

    private fun createPlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        return ExoPlayer.Builder(context, renderersFactory).build().apply {
            val audioAttributes = ExoAudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttributes, true)
            volume = 1.0f
        }
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
        updatePlaybackState(playingNow)
        updateNotification(currentSong, playingNow)
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

    var playMode by mutableStateOf(PlayMode.SEQUENTIAL)

    // 环绕音状态
    enum class SurroundMode {
        NONE,           // 无效果
        IMMERSIVE,      // 沉浸立体音（多声道，四面八方）
        THREE_D         // 3D环绕音（圆周运动）
    }

    var surroundMode by mutableStateOf(SurroundMode.NONE)  // 当前环绕音模式
    var isSurroundEnabled by mutableStateOf(false)          // 环绕音是否启用

    // 3D环绕音参数
    var surroundRadius by mutableStateOf(400f)         // 圆周半径
    var surroundSpeed by mutableStateOf(2.0f)          // 运动速度

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
    var batchImportProgress by mutableStateOf(0)
        private set
    var batchImportTotal by mutableStateOf(0)
        private set
    var batchImportCurrentSong by mutableStateOf<String?>(null)
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
    val textUnknownTrack: String get() = if (_currentLanguage == "zh") "未知曲目" else "Unknown Track"
    val textUnknownArtist: String get() = if (_currentLanguage == "zh") "未知艺术家" else "Unknown Artist"
    val textPlayMode: String get() = if (_currentLanguage == "zh") "播放模式" else "Play Mode"
    val textModeSwitch: String get() = if (_currentLanguage == "zh") "切换模式" else "Switch Mode"
    val textClose: String get() = if (_currentLanguage == "zh") "关闭" else "Close"
    val textQueueEmpty: String get() = if (_currentLanguage == "zh") "播放列表为空" else "Queue is empty"
    val textNowPlaying: String get() = if (_currentLanguage == "zh") "正在播放" else "Now Playing"
    val textDelete: String get() = if (_currentLanguage == "zh") "删除" else "Delete"

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
        }

    // 翻译API服务
    private val translateService: TranslateApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://api.yaohud.cn/api/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(TranslateApiService::class.java)
    }

    // 通知栏
    private val notificationManager by lazy { context.getSystemService(NotificationManager::class.java) }
    private val notificationChannelId = "purelymusic_playback"

    // --- 音频焦点处理 ---
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                audioFocusGranted = false
                if (isPlaying) togglePlayPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioFocusGranted = false
                exoPlayer?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                exoPlayer?.volume = 0.3f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusGranted = true
                exoPlayer?.volume = 1.0f
            }
        }
    }

    private var audioFocusRequest: AudioFocusRequest? = null

    private fun requestAudioFocus(): Boolean {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
        audioFocusRequest = request
        val result = audioManager.requestAudioFocus(request)
        audioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return audioFocusGranted
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusGranted = false
    }

    // --- 耳机拔出自动暂停 ---
    private class AudioBecomingNoisyReceiver(
        private val onNoisy: () -> Unit
    ) : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                onNoisy()
            }
        }
    }

    private fun registerAudioBecomingNoisyReceiver() {
        if (audioBecomingNoisyReceiver != null) return
        val receiver = AudioBecomingNoisyReceiver {
            if (isPlaying) {
                exoPlayer?.pause()
                syncPlaybackState()
            }
        }
        audioBecomingNoisyReceiver = receiver
        val intentFilter = android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        context.registerReceiver(receiver, intentFilter)
    }

    private fun unregisterAudioBecomingNoisyReceiver() {
        audioBecomingNoisyReceiver?.let {
            runCatching { context.unregisterReceiver(it) }
        }
        audioBecomingNoisyReceiver = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "播放控制",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "控制音乐播放"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(song: Song?, playing: Boolean) {
        if (song == null) {
            NotificationManagerCompat.from(context).cancel(1)
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val prevIntent = android.app.PendingIntent.getActivity(
            context, 1, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = android.app.PendingIntent.getActivity(
            context, 2, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setContentIntent(pendingIntent)
            .setOngoing(playing)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "", prevIntent)
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                "",
                pendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "", nextIntent)
            .build()
        if (playing) {
            try {
                NotificationManagerCompat.from(context).notify(1, notification)
            } catch (e: SecurityException) {
                // 没有通知权限，忽略
            }
        } else {
            NotificationManagerCompat.from(context).cancel(1)
        }
    }

    init {
        // 初始化 PreferencesManager
        com.music.purelymusic.utils.PreferencesManager.init(context)
        createNotificationChannel()
        
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

        // 初始化 Spatializer (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                    // 检测歌词语言，判断是否可以翻译
                    val lyricTexts = parsed.map { it.content }
                    val isChinese = com.music.purelymusic.utils.LanguageDetector.isLyricsChinese(lyricTexts)
                    canTranslate = !isChinese
                    Log.d("LyricLoad", "语言检测结果: isChinese=$isChinese, canTranslate=$canTranslate")
                    Log.d("LyricLoad", "歌词内容示例: ${lyricTexts.take(3)}")
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
    private fun buildMediaItem(musicPath: String): MediaItem {
        val uri = if (musicPath.startsWith("content://") || musicPath.startsWith("file://")) {
            Uri.parse(musicPath)
        } else {
            Uri.fromFile(File(musicPath))
        }
        val mimeType = resolveMimeType(musicPath)
        return MediaItem.Builder()
            .setUri(uri)
            .apply {
                if (!mimeType.isNullOrBlank()) {
                    setMimeType(mimeType)
                }
            }
            .build()
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
                incomingPlayer.setMediaItem(buildMediaItem(musicPath))
                incomingPlayer.prepare()
                incomingPlayer.volume = 0f
                incomingPlayer.play()

                isCrossfadeInProgress = true
                suppressNextEndedCallback = true
                this@PlayerViewModel.isPlaying = true
                updateSongState(nextSong)
                duration = incomingPlayer.duration.takeIf { it > 0L } ?: duration
                updatePlaybackState(true)

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

                incomingPlayer.addListener(playerListener)
                exoPlayer = incomingPlayer
                transitionPlayer = null
                isCrossfadeInProgress = false
                hasScheduledCrossfadeForCurrentSong = false
                suppressNextEndedCallback = false
                currentPosition = incomingPlayer.currentPosition
                duration = incomingPlayer.duration.takeIf { it > 0L } ?: duration
                this@PlayerViewModel.isPlaying = incomingPlayer.isPlaying
                updatePlaybackState(incomingPlayer.isPlaying)
                startSurroundEffect()
            } catch (e: Exception) {
                Log.e("Crossfade", "自动交叉渐入渐出失败: ${e.message}", e)
                resetCrossfadeState()
            }
        }
    }

    private fun updateSongState(song: Song) {
        currentSong = song
        hasScheduledCrossfadeForCurrentSong = false

        if (!song.lrcPath.isNullOrEmpty()) {
            loadLyrics(song.lrcPath)
        } else {
            lyricLines = emptyList()
        }

        updateMediaSession(song)
        updateBlurBackground(song.coverUri)

        viewModelScope.launch {
            songDao.updateSong(song.toEntity(System.currentTimeMillis()))
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

        requestAudioFocus()
        registerAudioBecomingNoisyReceiver()

        exoPlayer?.stop()
        updateSongState(song)

        try {
            val player = getOrCreatePlayer()
            val musicPath = song.musicUri ?: return
            val mediaItem = buildMediaItem(musicPath)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.volume = 1.0f
            player.play()
        } catch (e: Exception) {
            Log.e("PlayError", "播放失败: ${e.message}, 歌曲路径=${song.musicUri}")
        }
        updateNotification(song, true)
    }
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
        viewModelScope.launch {
            albumDao.deleteAlbum(album.toEntity())
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isSpatializerAvailable && surroundMode == SurroundMode.IMMERSIVE) {
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
                    currentPlayingList.clear()
                    currentPlayingList.addAll(libraryList)
                }

                // 获取专辑列表（用独立协程避免阻塞后续逻辑）
                launch(Dispatchers.IO) {
                    albumDao.getAllAlbums().collect { albumEntityList ->
                        withContext(Dispatchers.Main) {
                            albums.clear()
                            albums.addAll(albumEntityList.map { it.toAlbum() })
                        }
                    }
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
                exoPlayer?.stop()
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

    // --- 系统通知栏同步 ---
    private fun updateMediaSession(song: Song) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            // 🚩 核心修复：必须设置时长，系统进度条才能正确显示和响应拖动
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

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
            // 🚩 核心修复：传入 currentPosition 和 playbackSpeed，系统进度条才会显示正确位置
            .setState(state, currentPosition, 1.0f)
            // 🚩 核心修复：设置缓冲位置，确保进度条可以拖动
            .setBufferedPosition(duration)
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
            val blurred = bitmap?.let { BlurUtil.doBlur(it, 25, 20) }
            withContext(Dispatchers.Main) { blurredBackground = blurred }
        }
    }

    override fun onCleared() {
        super.onCleared()
        resetCrossfadeState()
        stop3DSurroundEffect()
        releaseEqualizer()
        abandonAudioFocus()
        unregisterAudioBecomingNoisyReceiver()
        syncPlaybackState()
        exoPlayer?.release()
        mediaSession?.release()
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

                    // 复制音乐文件到私有目录
                    val pMusic = copyFile(item.uri, "mus_${System.currentTimeMillis()}_${item.index}")

                    if (pMusic != null) {
                        processBatchImportSong(title, artist, pMusic, item.index)
                    } else {
                        Log.e("BatchImport", "复制音乐文件失败: ${item.uri}")
                    }
                } catch (e: Exception) {
                    Log.e("BatchImport", "导入歌曲失败: ${e.message}", e)
                }
            }

            // 队列处理完成
            if (batchImportQueue.isEmpty()) {
                refreshData()
                isBatchImporting = false
                batchImportCurrentSong = null
                batchImportPaused = false
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

        // 处理专辑逻辑
        if (!albumName.isNullOrEmpty()) {
            val existingAlbum = albumDao.getAlbumByName(albumName)
            if (existingAlbum == null) {
                val albumId = java.util.UUID.randomUUID().toString()
                val newAlbum = Album(
                    id = albumId,
                    name = albumName,
                    artist = albumArtist ?: artist,
                    coverUri = pCover
                )
                albumDao.insertAlbum(newAlbum.toEntity())
            }
        }

        // 创建歌曲对象并保存
        val newSong = Song(
            id = 0,
            title = title,
            artist = artist,
            coverUri = pCover,
            musicUri = musicPath,
            lrcPath = pLrc,
            album = albumName
        )

        songDao.insertSong(newSong.toEntity())
        Log.d("BatchImport", "成功导入歌曲: ${newSong.title}")
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

                // 处理专辑逻辑
                if (!albumName.isNullOrEmpty()) {
                    val existingAlbum = albumDao.getAlbumByName(albumName)
                    if (existingAlbum == null) {
                        val albumId = java.util.UUID.randomUUID().toString()
                        val newAlbum = Album(
                            id = albumId,
                            name = albumName,
                            artist = albumArtist ?: artist,
                            coverUri = pCover
                        )
                        albumDao.insertAlbum(newAlbum.toEntity())
                    }
                }

                // 创建歌曲对象并保存
                val newSong = Song(
                    id = 0,
                    title = title,
                    artist = artist,
                    coverUri = pCover,
                    musicUri = pMusic,
                    lrcPath = pLrc,
                    album = albumName
                )

                songDao.insertSong(newSong.toEntity())
                Log.d("BatchImport", "成功导入歌曲: ${newSong.title}")
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

    // --- 自动获取所有信息（封面+歌词）---
    suspend fun fetchAllFromNetwork(title: String, artist: String): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isFetchingAll = true
                    fetchAllError = null
                }

                val keywords = "$title $artist"
                Log.d("FetchAll", "开始获取所有信息: keywords=$keywords, source=${_autoFetchSource}")

                // 根据设置选择数据源
                if (_autoFetchSource == "mixed") {
                    // 混合模式：QQ获取封面 + 咪咕获取歌词
                    fetchFromMixedSource(keywords)
                } else {
                    // 网易云模式（默认）
                    fetchFromNetease(keywords)
                }
            } catch (e: Exception) {
                val errorMsg = "获取所有信息失败: ${e.javaClass.simpleName} - ${e.message}"
                Log.e("FetchAll", errorMsg, e)
                withContext(Dispatchers.Main) {
                    fetchAllError = errorMsg
                }
                Pair(null, null)
            } finally {
                withContext(Dispatchers.Main) {
                    isFetchingAll = false
                }
            }
        }
    }

    /**
     * 从网易云获取封面和歌词（原有逻辑）
     */
    private suspend fun fetchFromNetease(keywords: String): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://api.yaohud.cn/api/music/wy?key=${BuildConfig.MUSIC_API_KEY}&msg=${java.net.URLEncoder.encode(keywords, "UTF-8")}&n=1"
                Log.d("FetchAll", "网易云请求URL: $apiUrl")

                val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d("FetchAll", "HTTP响应码: $responseCode")

                if (responseCode == 200) {
                    val rawResponse = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("FetchAll", "原始响应: $rawResponse")

                    val gson = com.google.gson.Gson()
                    val response = gson.fromJson(rawResponse, com.music.purelymusic.model.WyApiResponse::class.java)

                    if (response.code == 200) {
                        val data = response.data
                        Log.d("FetchAll", "解析成功: album=${data.album}, picture=${data.picture}, lrc=${data.lrc}")

                        // 保存专辑信息
                        withContext(Dispatchers.Main) {
                            tempAlbumName = data.album
                            tempAlbumArtist = data.songname
                        }

                        // 处理封面
                        val coverPath = downloadCover(data.picture)

                        // 处理歌词
                        val lrcPath = downloadAndSaveLyricsFromUrl(data.lrc)

                        Pair(coverPath, lrcPath)
                    } else {
                        val errorMsg = "API错误: ${response.msg}"
                        Log.e("FetchAll", errorMsg)
                        withContext(Dispatchers.Main) {
                            fetchAllError = errorMsg
                        }
                        Pair(null, null)
                    }
                } else {
                    val errorMsg = "HTTP错误: $responseCode"
                    Log.e("FetchAll", errorMsg)
                    withContext(Dispatchers.Main) {
                        fetchAllError = errorMsg
                    }
                    Pair(null, null)
                }
            } catch (e: Exception) {
                val errorMsg = "网易云获取失败: ${e.message}"
                Log.e("FetchAll", errorMsg, e)
                withContext(Dispatchers.Main) {
                    fetchAllError = errorMsg
                }
                Pair(null, null)
            }
        }
    }

    /**
     * 混合模式：从QQ获取封面，从咪咕获取歌词
     */
    private suspend fun fetchFromMixedSource(keywords: String): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            var coverPath: String? = null
            var lrcPath: String? = null

            // 并行请求QQ和咪咕API
            try {
                // 1. 从QQ API获取封面
                coverPath = fetchCoverFromQQ(keywords)
            } catch (e: Exception) {
                Log.e("FetchAll", "QQ获取封面失败", e)
            }

            try {
                // 2. 从咪咕API获取歌词
                lrcPath = fetchLyricsFromMigu(keywords)
            } catch (e: Exception) {
                Log.e("FetchAll", "咪咕获取歌词失败", e)
            }

            if (coverPath == null && lrcPath == null) {
                withContext(Dispatchers.Main) {
                    fetchAllError = "混合模式获取失败：封面和歌词均未获取到"
                }
            }

            Pair(coverPath, lrcPath)
        }
    }

    /**
     * 从QQ音乐API获取封面
     */
    private suspend fun fetchCoverFromQQ(keywords: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://api.yaohud.cn/api/music/qq?key=${BuildConfig.MUSIC_API_KEY}&msg=${java.net.URLEncoder.encode(keywords, "UTF-8")}&n=1"
                Log.d("FetchAll", "QQ API请求URL: $apiUrl")

                val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d("FetchAll", "QQ API响应码: $responseCode")

                if (responseCode == 200) {
                    val rawResponse = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("FetchAll", "QQ API响应: $rawResponse")

                    val gson = com.google.gson.Gson()
                    val response = gson.fromJson(rawResponse, com.music.purelymusic.model.QqApiResponse::class.java)

                    if (response.code == 200 && !response.data.picture.isNullOrEmpty()) {
                        val pictureUrl = response.data.picture
                        Log.d("FetchAll", "QQ封面URL: $pictureUrl")

                        // 保存专辑信息
                        withContext(Dispatchers.Main) {
                            tempAlbumArtist = response.data.songname
                        }

                        // 下载封面
                        downloadCover(pictureUrl)
                    } else {
                        Log.e("FetchAll", "QQ API返回错误或无封面: code=${response.code}")
                        null
                    }
                } else {
                    Log.e("FetchAll", "QQ API HTTP错误: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e("FetchAll", "QQ API请求失败", e)
                null
            }
        }
    }

    /**
     * 从咪咕API获取歌词
     */
    private suspend fun fetchLyricsFromMigu(keywords: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://api.yaohud.cn/api/music/migu?key=${BuildConfig.MUSIC_API_KEY}&msg=${java.net.URLEncoder.encode(keywords, "UTF-8")}&n=1"
                Log.d("FetchAll", "咪咕API请求URL: $apiUrl")

                val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d("FetchAll", "咪咕API响应码: $responseCode")

                if (responseCode == 200) {
                    val rawResponse = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("FetchAll", "咪咕API响应: $rawResponse")

                    val gson = com.google.gson.Gson()
                    val response = gson.fromJson(rawResponse, com.music.purelymusic.model.MiguDetailResponse::class.java)

                    if (response.code == 200 && !response.data.lrc_url.isNullOrEmpty()) {
                        val lrcUrl = response.data.lrc_url
                        Log.d("FetchAll", "咪咕歌词URL: $lrcUrl")

                        // 下载歌词内容
                        downloadAndSaveLyricsFromDirectUrl(lrcUrl)
                    } else {
                        Log.e("FetchAll", "咪咕API返回错误或无歌词: code=${response.code}")
                        null
                    }
                } else {
                    Log.e("FetchAll", "咪咕API HTTP错误: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e("FetchAll", "咪咕API请求失败", e)
                null
            }
        }
    }

    /**
     * 下载封面图片
     */
    private fun downloadCover(pictureUrl: String?): String? {
        if (pictureUrl.isNullOrEmpty()) return null

        val secureUrl = if (pictureUrl.startsWith("http://")) {
            "https://${pictureUrl.substring(7)}"
        } else {
            pictureUrl
        }

        return try {
            val coverConnection = java.net.URL(secureUrl).openConnection() as java.net.HttpURLConnection
            coverConnection.requestMethod = "GET"
            coverConnection.connect()

            if (coverConnection.responseCode == 200) {
                val inputStream = coverConnection.inputStream
                val fileName = "cover_${System.currentTimeMillis()}.jpg"
                val file = java.io.File(context.filesDir, fileName)
                FileOutputStream(file).use { output ->
                    inputStream.use { it.copyTo(output) }
                }
                file.absolutePath
            } else {
                Log.e("FetchAll", "下载封面失败，响应码: ${coverConnection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("FetchAll", "下载封面失败", e)
            null
        }
    }

    /**
     * 从歌词URL下载并保存歌词（网易云模式，需要二次请求）
     */
    private fun downloadAndSaveLyricsFromUrl(lrcUrl: String?): String? {
        if (lrcUrl.isNullOrEmpty()) {
            Log.d("FetchAll", "歌词URL为空")
            return null
        }

        return try {
            Log.d("FetchAll", "通过URL获取歌词: $lrcUrl")

            val lrcConnection = java.net.URL(lrcUrl).openConnection() as java.net.HttpURLConnection
            lrcConnection.requestMethod = "GET"
            lrcConnection.connectTimeout = 10000
            lrcConnection.readTimeout = 10000
            lrcConnection.connect()

            if (lrcConnection.responseCode == 200) {
                val rawResponse = lrcConnection.inputStream.bufferedReader().use { it.readText() }
                Log.d("FetchAll", "歌词API响应: ${rawResponse.take(500)}")

                val gson = com.google.gson.Gson()
                val lrcResponse = gson.fromJson(rawResponse, com.music.purelymusic.model.LrcJsonResponse::class.java)

                if (lrcResponse.code == 200 && !lrcResponse.data?.lyric.isNullOrEmpty()) {
                    saveLyricsFile(lrcResponse.data.lyric)
                } else {
                    null
                }
            } else {
                Log.e("FetchAll", "歌词URL请求失败，响应码: ${lrcConnection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("FetchAll", "下载歌词失败", e)
            null
        }
    }

    /**
     * 直接从歌词URL下载并保存歌词（咪咕模式，直接是lrc文件）
     */
    private fun downloadAndSaveLyricsFromDirectUrl(lrcUrl: String): String? {
        return try {
            Log.d("FetchAll", "直接下载歌词文件: $lrcUrl")

            val lrcConnection = java.net.URL(lrcUrl).openConnection() as java.net.HttpURLConnection
            lrcConnection.requestMethod = "GET"
            lrcConnection.connectTimeout = 10000
            lrcConnection.readTimeout = 10000
            lrcConnection.connect()

            if (lrcConnection.responseCode == 200) {
                val lrcContent = lrcConnection.inputStream.bufferedReader().use { it.readText() }
                Log.d("FetchAll", "歌词内容长度: ${lrcContent.length}")
                saveLyricsFile(lrcContent)
            } else {
                Log.e("FetchAll", "下载歌词文件失败，响应码: ${lrcConnection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("FetchAll", "下载歌词文件失败", e)
            null
        }
    }

    /**
     * 保存歌词文件
     */
    private fun saveLyricsFile(lrcContent: String): String? {
        if (lrcContent.isBlank()) return null

        val fileName = "lrc_${System.currentTimeMillis()}.lrc"
        val file = java.io.File(context.filesDir, fileName)
        file.writeText(lrcContent, Charsets.UTF_8)
        return file.absolutePath
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
                    val timeStr = formatTimeToLrc(line.time)
                    "[$timeStr]${line.content}"
                }

                addLog("发送翻译请求，歌词长度: ${lyricText.length}")
                addLog("歌词前100字符: ${lyricText.take(100)}")
                Log.d("Translate", "发送翻译请求，歌词长度: ${lyricText.length}")
                Log.d("Translate", "歌词前100字符: ${lyricText.take(100)}")

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
                addLog("响应data: ${response.data}")
                addLog("完整响应: $response")
                Log.d("Translate", "API调用成功")
                Log.d("Translate", "========== 翻译响应开始 ==========")
                Log.d("Translate", "响应代码: ${response.code}")
                Log.d("Translate", "响应消息: ${response.msg}")
                Log.d("Translate", "响应data: ${response.data}")
                Log.d("Translate", "完整响应: $response")

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
                val decodedText = decodeHtmlEntities(translatedText)

                addLog("翻译响应: translatedText长度=${decodedText.length}")
                addLog("翻译响应前200字符: ${decodedText.take(200)}")
                Log.d("Translate", "翻译响应: translatedText长度=${decodedText.length}")
                Log.d("Translate", "翻译响应前200字符: ${decodedText.take(200)}")

                // 检查翻译文本是否为空
                if (translatedText.isBlank()) {
                    translateError = "翻译服务返回空内容，请稍后重试"
                    addLog("❌ 翻译文本为空")
                    Log.e("Translate", "翻译文本为空")
                    return@launch
                }

                // 解析翻译后的文本
                val translatedLines = parseTranslatedText(decodedText, lyricLines)

                addLog("解析结果：共${translatedLines.size}行")
                translatedLines.forEachIndexed { index, text ->
                    addLog("  第${index}行: ${text?.take(30) ?: "(空)"} (长度=${text?.length ?: 0})")
                }
                addLog("========== 翻译响应结束 ==========")
                Log.d("Translate", "解析结果：共${translatedLines.size}行")
                translatedLines.forEachIndexed { index, text ->
                    Log.d("Translate", "  第${index}行: ${text?.take(30) ?: "(空)"} (长度=${text?.length ?: 0})")
                }
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
                    addLog("更新后的第一行: content=${lyricLines.getOrNull(0)?.content}, translation=${lyricLines.getOrNull(0)?.translation}")
                    addLog("更新后的第二行: content=${lyricLines.getOrNull(1)?.content}, translation=${lyricLines.getOrNull(1)?.translation}")
                    Log.d("Translate", "翻译完成，showTranslation=$showTranslation")
                    Log.d("Translate", "更新后的第一行: content=${lyricLines.getOrNull(0)?.content}, translation=${lyricLines.getOrNull(0)?.translation}")
                    Log.d("Translate", "更新后的第二行: content=${lyricLines.getOrNull(1)?.content}, translation=${lyricLines.getOrNull(1)?.translation}")
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

    /**
     * 将毫秒转换为LRC时间格式 [mm:ss.xxx]
     */
    private fun formatTimeToLrc(ms: Long): String {
        val minutes = ms / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format("%02d:%02d.%03d", minutes, seconds, millis)
    }

    /**
     * 解析翻译后的文本，提取每句翻译
     * 使用 parseContinuous 方法解析包含合并时间戳的翻译文本，然后进行智能匹配
     */
    private fun parseTranslatedText(translatedText: String, originalLines: List<LrcLine>): List<String?> {
        val result: MutableList<String?> = mutableListOf()
        repeat(originalLines.size) { result.add(null) }

        Log.d("Translate", "========== 解析翻译文本开始 ==========")
        addLog("========== 解析翻译文本开始 ==========")
        addLog("翻译文本长度: ${translatedText.length}")
        addLog("翻译文本前500字符: ${translatedText.take(500)}")
        addLog("原始歌词行数: ${originalLines.size}")
        addLog("原始歌词时间戳前3个: ${originalLines.take(3).map { "${formatTimeToLrc(it.time)}(${it.time}ms)" }}")

        // 步骤1: 使用 LrcParser.parseContinuous 解析包含合并时间戳的文本
        // 这个方法可以处理像 [00:10.255]文本[00:17.957]文本 这样的格式
        val normalizedText = convertContinuousToStandardLrc(translatedText)
        val translatedLines = LrcParser.parseContinuous(normalizedText)
        if (translatedLines.isEmpty()) {
            val plainLines = translatedText.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()

            plainLines.forEachIndexed { index, line ->
                val logMsg = "Plain line ${index}: ${line.take(40)}"
                Log.d("Translate", "  $logMsg")
                addLog("  $logMsg")
            }

            for (i in originalLines.indices) {
                result[i] = plainLines.getOrNull(i)
            }
            return result
        }
        Log.d("Translate", "parseContinuous解析结果: 共${translatedLines.size}行")
        addLog("parseContinuous解析结果: 共${translatedLines.size}行")

        // 打印解析后的翻译内容
        translatedLines.forEachIndexed { index, line ->
            val logMsg = "翻译第${index}行: [${formatTimeToLrc(line.time)}] ${line.content.take(40)}"
            Log.d("Translate", "  $logMsg")
            addLog("  $logMsg")
        }

        // 步骤2: 使用双向匹配算法
        // 对于每个原文行，查找最接近的翻译行
        var matchedCount = 0
        var unmatchedCount = 0
        
        for (i in originalLines.indices) {
            val originalTime = originalLines[i].time
            
            // 查找最接近且未使用的时间戳
            var bestMatchIndex = -1
            var minDiff = Long.MAX_VALUE
            val timeTolerance = 300L // 允许300毫秒误差
            
            for (j in translatedLines.indices) {
                val diff = kotlin.math.abs(translatedLines[j].time - originalTime)
                if (diff < minDiff) {
                    minDiff = diff
                    bestMatchIndex = j
                }
            }

            if (bestMatchIndex != -1 && minDiff <= timeTolerance) {
                result[i] = translatedLines[bestMatchIndex].content
                matchedCount++
                val logMsg = "原文行${i} [${formatTimeToLrc(originalTime)}] -> 匹配翻译: ${translatedLines[bestMatchIndex].content.take(40)} (差异: ${minDiff}ms)"
                Log.d("Translate", "$logMsg")
                addLog("$logMsg")
            } else {
                unmatchedCount++
                val logMsg = "原文行${i} [${formatTimeToLrc(originalTime)}] -> 未找到匹配翻译 (原始内容: ${originalLines[i].content.take(30)})"
                Log.d("Translate", "$logMsg")
                addLog("$logMsg")
            }
        }

        Log.d("Translate", "匹配统计: 成功$matchedCount 行, 失败$unmatchedCount 行")
        addLog("匹配统计: 成功$matchedCount 行, 失败$unmatchedCount 行")

        // Fallback: if translation missing, use original text to avoid empty lines
        for (i in result.indices) {
            if (result[i].isNullOrBlank() && originalLines[i].content.isNotBlank()) {
                result[i] = originalLines[i].content
                val logMsg = "Fallback to original line $i"
                Log.d("Translate", logMsg)
                addLog(logMsg)
            }
        }
        
        // 打印调试信息
        Log.d("Translate", "解析结果：")
        addLog("解析结果：")
        result.forEachIndexed { index, text ->
            val logMsg = "  第${index}行: ${text?.take(50) ?: "(空)"} (长度=${text?.length ?: 0})"
            Log.d("Translate", "$logMsg")
            addLog("$logMsg")
        }
        Log.d("Translate", "========== 解析翻译文本结束 ==========")
        addLog("========== 解析翻译文本结束 ==========")

        return result
    }

    /**
     * 将连续时间戳格式的文本转换为标准LRC格式
     * 先标准化所有时间戳格式，然后正确处理换行符
     */
    private fun convertContinuousToStandardLrc(continuousText: String): String {
        var result = continuousText
        
        // 步骤1: 修复错误格式的时间戳
        // 修复 [00:6.484] -> [00:06.484] (秒数补零)
        result = result.replace(Regex("\\[(\\d{2}):(\\d)([.:]\\d{2,3})\\]")) { matchResult ->
            val min = matchResult.groupValues[1]
            val sec = matchResult.groupValues[2].padStart(2, '0')
            val millis = matchResult.groupValues[3]
            "[$min:$sec$millis]"
        }
        
        // 修复 [0:15.367] -> [00:15.367] (分钟补零)
        result = result.replace(Regex("\\[(\\d):(\\d{2})([.:]\\d{2,3})\\]")) { matchResult ->
            val min = matchResult.groupValues[1].padStart(2, '0')
            val sec = matchResult.groupValues[2]
            val millis = matchResult.groupValues[3]
            "[$min:$sec$millis]"
        }
        
        // 修复 [00,49.162] -> [00:49.162] (逗号转冒号)
        result = result.replace(Regex("\\[(\\d{2}),(\\d{2})([.:]\\d{2,3})\\]")) { matchResult ->
            val min = matchResult.groupValues[1]
            val sec = matchResult.groupValues[2]
            val millis = matchResult.groupValues[3]
            "[$min:$sec$millis]"
        }
        
        // 修复 [00:3.884] -> [00:03.884] (秒数补零)
        result = result.replace(Regex("\\[(\\d{2}):(\\d)([.:]\\d{2,3})\\]")) { matchResult ->
            val min = matchResult.groupValues[1]
            val sec = matchResult.groupValues[2].padStart(2, '0')
            val millis = matchResult.groupValues[3]
            "[$min:$sec$millis]"
        }
        
        // 修复 [02:431.23] -> [02:43.123] (秒数错误，截取前两位)
        result = result.replace(Regex("\\[(\\d{2}):(\\d{3})([.:]\\d{2,3})\\]")) { matchResult ->
            val min = matchResult.groupValues[1]
            val sec = matchResult.groupValues[2].take(2)
            val millis = matchResult.groupValues[3]
            "[$min:$sec$millis]"
        }
        
        // 修复 [03:05.65] -> [03:05.065] (毫秒补零)
        result = result.replace(Regex("\\[(\\d{2}):(\\d{2})[.:](\\d{2})\\]")) { matchResult ->
            val min = matchResult.groupValues[1]
            val sec = matchResult.groupValues[2]
            val millis = matchResult.groupValues[3] + "0"
            "[$min:$sec.$millis]"
        }
        
        // 步骤2: 提取每个时间戳及其后的文本，重新格式化
        // 这样可以确保时间戳和文本在同一行，并且每行只有一个时间戳
        val timePattern = Regex("\\[\\d{2}:\\d{2}[.:]\\d{3}\\]")
        val matches = timePattern.findAll(result).toList()
        
        if (matches.isEmpty()) {
            return result
        }
        
        val builder = StringBuilder()
        
        for (i in matches.indices) {
            val match = matches[i]
            val timeStr = match.value
            
            // 提取文本：从当前时间戳结束位置到下一个时间戳开始位置
            val startPos = match.range.last + 1
            val endPos = if (i < matches.size - 1) {
                matches[i + 1].range.first
            } else {
                result.length
            }
            
            var content = result.substring(startPos, endPos).trim()
            
            // 移除内容中的其他时间戳（防止嵌套）
            content = content.replace(timePattern, "").trim()
            
            // 将时间戳和文本放在同一行
            if (content.isNotEmpty()) {
                builder.append(timeStr).append(content).append("\n")
            }
        }
        
        // 步骤3: 移除末尾多余的换行符
        return builder.toString().trimEnd('\n')
    }

    /**
     * 解析LRC时间戳格式 [MM:SS.mmm] 或 [MM:SS:mmm] 为毫秒
     */
    private fun parseLrcTime(timeStr: String): Long {
        try {
            // 移除方括号，提取时间部分
            val timeContent = timeStr.substring(1, timeStr.length - 1)
            val parts = timeContent.split(":")
            
            val minutes = parts[0].toInt()
            val seconds = parts[1].split("[.:]")[0].toInt()
            
            // 提取毫秒部分，支持点号或冒号分隔
            val millisStr = if (parts[1].contains(".")) {
                parts[1].split(".")[1]
            } else if (parts[1].contains(":")) {
                parts[1].split(":")[1]
            } else {
                "0"
            }
            
            // 处理毫秒：2位需要乘以10，3位直接使用
            val millis = when (millisStr.length) {
                2 -> millisStr.toInt() * 10L  // 例如: 48 -> 480
                3 -> millisStr.toLong()       // 例如: 484 -> 484
                else -> millisStr.toLong()    // 其他情况直接使用
            }
            
            return minutes * 60000L + seconds * 1000L + millis
        } catch (e: Exception) {
            Log.e("Translate", "解析时间戳失败: $timeStr, 错误: ${e.message}")
            return 0L
        }
    }

    /**
     * 添加翻译日志
     */
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

    /**
     * 解码HTML实体
     * 将 &amp;quot;、&amp;#039; 等HTML实体转换为正常字符
     * 并将 \\n 转换为真正的换行符
     * 支持中文分号（；）和英文分号（;）两种格式
     */
    private fun decodeHtmlEntities(text: String): String {
        var result = text
        
        // 处理带中文分号的HTML实体（如 &quot；）
        result = result.replace("&quot；", "\"")
        result = result.replace("&apos；", "'")
        result = result.replace("&lt；", "<")
        result = result.replace("&gt；", ">")
        result = result.replace("&amp；", "&")
        result = result.replace("&#039；", "'")
        
        // 处理带英文分号的HTML实体（如 &quot;）
        result = result.replace("&quot;", "\"")
        result = result.replace("&apos;", "'")
        result = result.replace("&lt;", "<")
        result = result.replace("&gt;", ">")
        result = result.replace("&amp;", "&")
        result = result.replace("&#039;", "'")
        
        // 处理不带&前缀但可能被错误编码的情况
        result = result.replace("quot；", "\"")
        result = result.replace("quot;", "\"")
        result = result.replace("apos；", "'")
        result = result.replace("apos;", "'")
        result = result.replace("lt；", "<")
        result = result.replace("lt;", "<")
        result = result.replace("gt；", ">")
        result = result.replace("gt;", ">")
        result = result.replace("amp；", "&")
        result = result.replace("amp;", "&")
        result = result.replace("#039；", "'")
        result = result.replace("#039;", "'")
        
        // 处理中文全角引号和标点
        result = result.replace(""", "\"")
        result = result.replace(""", "\"")
        result = result.replace("，", ",")
        result = result.replace("。", ".")
        result = result.replace("！", "!")
        result = result.replace("？", "?")
        result = result.replace("；", ";")  // 中文分号转英文分号
        
        // 处理中文全角方括号转换为半角方括号（重要！）
        result = result.replace("【", "[")
        result = result.replace("】", "]")
        result = result.replace("［", "[")
        result = result.replace("］", "]")
        
        // 处理中文全角括号
        result = result.replace("（", "(")
        result = result.replace("）", ")")
        
        // 处理换行符
        result = result.replace("\\n", "\n")
        
        return result
    }
}
