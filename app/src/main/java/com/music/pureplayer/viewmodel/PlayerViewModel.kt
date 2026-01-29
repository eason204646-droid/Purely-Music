//Copyright (c) [2026] [eason204646]
//[purelyplayer] is licensed under Mulan PSL v2.
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
//
//Your reproduction, use, modification and distribution of the Software shall
//be subject to Mulan PSL v2 (this License) with the following terms and
//conditions:
//
//0. Definition
//
//Software means the program and related documents which are licensed under
//this License and comprise all Contribution(s).
//
//Contribution means the copyrightable work licensed by a particular
//Contributor under this License.
//
//Contributor means the Individual or Legal Entity who licenses its
//copyrightable work under this License.
//
//Legal Entity means the entity making a Contribution and all its
//Affiliates.
//
//Affiliates means entities that control, are controlled by, or are under
//common control with the acting entity under this License, ‘control’ means
//direct or indirect ownership of at least fifty percent (50%) of the voting
//power, capital or other securities of controlled or commonly controlled
//entity.
//
//1. Grant of Copyright License
//
//Subject to the terms and conditions of this License, each Contributor hereby
//grants to you a perpetual, worldwide, royalty-free, non-exclusive,
//irrevocable copyright license to reproduce, use, modify, or distribute its
//Contribution, with modification or not.
//
//2. Grant of Patent License
//
//Subject to the terms and conditions of this License, each Contributor hereby
//grants to you a perpetual, worldwide, royalty-free, non-exclusive,
//irrevocable (except for revocation under this Section) patent license to
//make, have made, use, offer for sale, sell, import or otherwise transfer its
//Contribution, where such patent license is only limited to the patent claims
//owned or controlled by such Contributor now or in future which will be
//necessarily infringed by its Contribution alone, or by combination of the
//Contribution with the Software to which the Contribution was contributed.
//The patent license shall not apply to any modification of the Contribution,
//and any other combination which includes the Contribution. If you or your
//Affiliates directly or indirectly institute patent litigation (including a
//cross claim or counterclaim in a litigation) or other patent enforcement
//activities against any individual or entity by alleging that the Software or
//any Contribution in it infringes patents, then any patent license granted to
//you under this License for the Software shall terminate as of the date such
//litigation or activity is filed or taken.
//
//3. No Trademark License
//
//No trademark license is granted to use the trade names, trademarks, service
//marks, or product names of Contributor, except as required to fulfill notice
//requirements in section 4.
//
//4. Distribution Restriction
//
//You may distribute the Software in any medium with or without modification,
//whether in source or executable forms, provided that you provide recipients
//with a copy of this License and retain copyright, patent, trademark and
//disclaimer statements in the Software.
//
//5. Disclaimer of Warranty and Limitation of Liability
//
//THE SOFTWARE AND CONTRIBUTION IN IT ARE PROVIDED WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED. IN NO EVENT SHALL ANY CONTRIBUTOR OR
//COPYRIGHT HOLDER BE LIABLE TO YOU FOR ANY DAMAGES, INCLUDING, BUT NOT
//LIMITED TO ANY DIRECT, OR INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING
//FROM YOUR USE OR INABILITY TO USE THE SOFTWARE OR THE CONTRIBUTION IN IT, NO
//MATTER HOW IT’S CAUSED OR BASED ON WHICH LEGAL THEORY, EVEN IF ADVISED OF
//THE POSSIBILITY OF SUCH DAMAGES.
//
//6. Language
//
//THIS LICENSE IS WRITTEN IN BOTH CHINESE AND ENGLISH, AND THE CHINESE VERSION
//AND ENGLISH VERSION SHALL HAVE THE SAME LEGAL EFFECT. IN THE CASE OF
//DIVERGENCE BETWEEN THE CHINESE AND ENGLISH VERSIONS, THE CHINESE VERSION
//SHALL PREVAIL.
//
//END OF THE TERMS AND CONDITIONS
//
//How to Apply the Mulan Permissive Software License，Version 2
//(Mulan PSL v2) to Your Software
//
//To apply the Mulan PSL v2 to your work, for easy identification by
//recipients, you are suggested to complete following three steps:
//
//i. Fill in the blanks in following statement, including insert your software
//name, the year of the first publication of your software, and your name
//identified as the copyright owner;
//
//ii. Create a file named "LICENSE" which contains the whole context of this
//License in the first directory of your software package;
//
//iii. Attach the statement to the appropriate annotated syntax at the
//beginning of each source file.
//
//Copyright (c) [Year] [name of copyright holder]
//[Software Name] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
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