package com.music.purelymusic

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.purelymusic.data.AppDatabase
import com.music.purelymusic.data.AlbumEntity
import com.music.purelymusic.data.PlaylistEntity
import com.music.purelymusic.data.PlaylistSongCrossRef
import com.music.purelymusic.data.SongEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun songDeletionCascadesAndPlaylistOrderIsStable() = runBlocking {
        val first = SongEntity(1L, "第一首", "歌手", null, "/music/1.flac")
        val second = SongEntity(2L, "第二首", "歌手", null, "/music/2.flac")
        database.songDao().insertSong(first)
        database.songDao().insertSong(second)
        database.playlistDao().upsertPlaylist(
            PlaylistEntity("playlist", "测试歌单", null),
            listOf(
                PlaylistSongCrossRef("playlist", 2L, 0),
                PlaylistSongCrossRef("playlist", 1L, 1)
            )
        )

        val initial = database.playlistDao().getAllPlaylists().single().songRefs.sortedBy { it.position }
        assertEquals(listOf(2L, 1L), initial.map { it.songId })

        database.songDao().deleteSong(second)
        val remaining = database.playlistDao().getAllPlaylists().single().songRefs
        assertEquals(listOf(1L), remaining.map { it.songId })
    }

    @Test
    fun albumRenameKeepsSongAssociations() = runBlocking {
        val album = AlbumEntity("album-1", "旧名称", "歌手", null)
        database.albumDao().insertAlbum(album)
        database.songDao().insertSong(SongEntity(1L, "第一首", "歌手", null, "/music/1.flac", album = album.name, albumId = album.id))
        database.songDao().insertSong(SongEntity(2L, "旧歌曲", "歌手", null, "/music/2.flac", album = album.name))
        database.songDao().insertSong(SongEntity(3L, "其他专辑", "歌手", null, "/music/3.flac", album = "其他", albumId = "album-2"))

        database.withTransaction {
            database.albumDao().updateAlbum(album.copy(name = "新名称"))
            database.songDao().renameAlbumReferences(album.id, album.name, "新名称")
        }

        assertEquals("新名称", database.albumDao().getAlbumById(album.id)?.name)
        val songs = database.songDao().getAllSongs().associateBy { it.id }
        assertEquals(listOf("新名称", "新名称"), listOf(songs[1L]?.album, songs[2L]?.album))
        assertEquals(listOf(album.id, album.id), listOf(songs[1L]?.albumId, songs[2L]?.albumId))
        assertEquals("其他", songs[3L]?.album)
        assertEquals("album-2", songs[3L]?.albumId)
    }
}
